package com.erp.request.service;

import com.erp.grpc.*;
import com.erp.request.document.ApprovalRequestDocument;
import com.erp.request.dto.CreateApprovalRequest;
import com.erp.request.dto.NotificationRequest;
import com.erp.request.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRequestService {

    private final ApprovalRequestRepository repository;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.employee-service.url}")
    private String employeeServiceUrl;

    @Value("${services.notification-service.url}")
    private String notificationServiceUrl;

    @GrpcClient("approval-processing-service")
    private ApprovalServiceGrpc.ApprovalServiceBlockingStub processingServiceStub;

    public Integer createApprovalRequest(CreateApprovalRequest request) {
        // 1. requesterId, approverId 존재 여부 검증
        validateEmployeeExists(request.getRequesterId());
        for (CreateApprovalRequest.StepRequest step : request.getSteps()) {
            validateEmployeeExists(step.getApproverId());
        }

        // 2. steps가 1부터 오름차순인지 검증
        validateStepsOrder(request.getSteps());

        // 3. 새 requestId 생성
        Integer newRequestId = generateNextRequestId();

        // 4. Document 생성 및 저장
        List<ApprovalRequestDocument.Step> steps = request.getSteps().stream()
                .map(s -> ApprovalRequestDocument.Step.builder()
                        .step(s.getStep())
                        .approverId(s.getApproverId())
                        .status("pending")
                        .build())
                .collect(Collectors.toList());

        ApprovalRequestDocument document = ApprovalRequestDocument.builder()
                .requestId(newRequestId)
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .content(request.getContent())
                .steps(steps)
                .finalStatus("in_progress")
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(document);
        log.info("Created approval request: requestId={}", newRequestId);

        // 5. gRPC로 Processing Service에 전달
        sendToProcessingService(document);

        return newRequestId;
    }

    public List<ApprovalRequestDocument> getAllRequests() {
        return repository.findAll();
    }

    public ApprovalRequestDocument getRequest(Integer requestId) {
        return repository.findByRequestId(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
    }

    // 결재 결과 처리 (gRPC로부터 호출됨)
    public void processApprovalResult(Integer requestId, Integer step, Integer approverId, String status) {
        ApprovalRequestDocument document = getRequest(requestId);

        // 1. 해당 step의 status 업데이트
        for (ApprovalRequestDocument.Step s : document.getSteps()) {
            if (s.getStep().equals(step) && s.getApproverId().equals(approverId)) {
                s.setStatus(status);
                s.setUpdatedAt(LocalDateTime.now());
                break;
            }
        }
        document.setUpdatedAt(LocalDateTime.now());

        // 2. rejected인 경우
        if ("rejected".equals(status)) {
            document.setFinalStatus("rejected");
            repository.save(document);
            sendNotification(document.getRequesterId(), requestId, "rejected", approverId, "rejected");
            return;
        }

        // 3. approved인 경우
        ApprovalRequestDocument.Step nextPendingStep = findNextPendingStep(document.getSteps());

        if (nextPendingStep != null) {
            // 다음 결재자가 있으면 Processing Service에 재전달
            repository.save(document);
            sendToProcessingService(document);
        } else {
            // 모든 결재 완료
            document.setFinalStatus("approved");
            repository.save(document);
            sendNotification(document.getRequesterId(), requestId, "approved", null, "approved");
        }
    }

    private void sendToProcessingService(ApprovalRequestDocument document) {
        List<Step> grpcSteps = document.getSteps().stream()
                .map(s -> Step.newBuilder()
                        .setStep(s.getStep())
                        .setApproverId(s.getApproverId())
                        .setStatus(s.getStatus())
                        .build())
                .collect(Collectors.toList());

        ApprovalRequest grpcRequest = ApprovalRequest.newBuilder()
                .setRequestId(document.getRequestId())
                .setRequesterId(document.getRequesterId())
                .setTitle(document.getTitle())
                .setContent(document.getContent())
                .addAllSteps(grpcSteps)
                .build();

        try {
            ApprovalResponse response = processingServiceStub.requestApproval(grpcRequest);
            log.info("gRPC RequestApproval response: {}", response.getStatus());
        } catch (Exception e) {
            log.error("Failed to send to Processing Service", e);
        }
    }

    private void sendNotification(Integer requesterId, Integer requestId, String result,
                                   Integer rejectedBy, String finalResult) {
        NotificationRequest notification = NotificationRequest.builder()
                .requestId(requestId)
                .result(result)
                .rejectedBy(rejectedBy)
                .finalResult(finalResult)
                .build();

        try {
            webClientBuilder.build()
                    .post()
                    .uri(notificationServiceUrl + "/notify/" + requesterId)
                    .bodyValue(notification)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> log.info("Notification sent: {}", response),
                            error -> log.error("Failed to send notification", error)
                    );
        } catch (Exception e) {
            log.error("Failed to send notification", e);
        }
    }

    private void validateEmployeeExists(Integer employeeId) {
        try {
            Boolean exists = webClientBuilder.build()
                    .get()
                    .uri(employeeServiceUrl + "/employees/" + employeeId + "/exists")
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            if (exists == null || !exists) {
                throw new RuntimeException("Employee not found: " + employeeId);
            }
        } catch (Exception e) {
            log.warn("Could not validate employee {}: {}", employeeId, e.getMessage());
            // 개발 편의를 위해 검증 실패시에도 계속 진행
        }
    }

    private void validateStepsOrder(List<CreateApprovalRequest.StepRequest> steps) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getStep() != i + 1) {
                throw new RuntimeException("Steps must be in ascending order starting from 1");
            }
        }
    }

    private Integer generateNextRequestId() {
        return repository.findTopByOrderByRequestIdDesc()
                .map(doc -> doc.getRequestId() + 1)
                .orElse(1);
    }

    private ApprovalRequestDocument.Step findNextPendingStep(List<ApprovalRequestDocument.Step> steps) {
        return steps.stream()
                .filter(s -> "pending".equals(s.getStatus()))
                .findFirst()
                .orElse(null);
    }
}

package com.erp.request.service;

import com.erp.grpc.*;
import com.erp.request.document.ApprovalRequestDocument;
import com.erp.request.dto.ApprovalStatistics;
import com.erp.request.dto.CreateApprovalRequest;
import com.erp.request.dto.NotificationRequest;
import com.erp.request.dto.UpdateApprovalRequest;
import com.erp.request.kafka.ApprovalRequestKafkaProducer;
import com.erp.request.kafka.NotificationKafkaProducer;
import com.erp.request.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRequestService {

    private final ApprovalRequestRepository repository;
    private final WebClient.Builder webClientBuilder;
    private final NotificationKafkaProducer kafkaProducer;
    private final ApprovalRequestKafkaProducer approvalKafkaProducer;

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
                .deadline(request.getDeadline())
                .reminderSent(false)
                .build();

        repository.save(document);
        log.info("Created approval request: requestId={}, deadline={}", newRequestId, request.getDeadline());

        // 5. 첫 번째 결재자에게 알림 발송
        if (!steps.isEmpty()) {
            Integer firstApproverId = steps.get(0).getApproverId();
            kafkaProducer.sendNewApprovalNotification(
                    newRequestId,
                    request.getRequesterId(),
                    firstApproverId,
                    request.getTitle()
            );
            log.info("Sent new approval notification to approverId={}", firstApproverId);
        }

        // 6. gRPC로 Processing Service에 전달
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

    // 결재 요청 수정 (모든 단계가 pending일 때만 가능)
    public void updateApprovalRequest(Integer requestId, UpdateApprovalRequest request) {
        ApprovalRequestDocument document = getRequest(requestId);

        // 1. 요청자 본인인지 확인
        if (!document.getRequesterId().equals(request.getEditedBy())) {
            throw new RuntimeException("Only the requester can edit this request");
        }

        // 2. 아직 아무도 처리 안 했는지 확인 (모든 단계가 pending이어야 함)
        boolean allPending = document.getSteps().stream()
                .allMatch(s -> "pending".equals(s.getStatus()));
        if (!allPending) {
            throw new RuntimeException("Cannot edit after approval has started");
        }

        // 3. 수정 이력 기록
        List<ApprovalRequestDocument.EditHistory> history = document.getEditHistory();
        if (history == null) {
            history = new ArrayList<>();
        }

        LocalDateTime now = LocalDateTime.now();

        // 제목 변경 이력
        if (request.getTitle() != null && !request.getTitle().equals(document.getTitle())) {
            history.add(ApprovalRequestDocument.EditHistory.builder()
                    .editedAt(now)
                    .editedBy(request.getEditedBy())
                    .fieldChanged("title")
                    .oldValue(document.getTitle())
                    .newValue(request.getTitle())
                    .build());
            document.setTitle(request.getTitle());
        }

        // 내용 변경 이력
        if (request.getContent() != null && !request.getContent().equals(document.getContent())) {
            history.add(ApprovalRequestDocument.EditHistory.builder()
                    .editedAt(now)
                    .editedBy(request.getEditedBy())
                    .fieldChanged("content")
                    .oldValue(document.getContent())
                    .newValue(request.getContent())
                    .build());
            document.setContent(request.getContent());
        }

        // 결재선 변경 이력
        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            String oldSteps = document.getSteps().stream()
                    .map(s -> s.getStep() + ":" + s.getApproverId())
                    .collect(Collectors.joining(","));
            String newSteps = request.getSteps().stream()
                    .map(s -> s.getStep() + ":" + s.getApproverId())
                    .collect(Collectors.joining(","));

            if (!oldSteps.equals(newSteps)) {
                history.add(ApprovalRequestDocument.EditHistory.builder()
                        .editedAt(now)
                        .editedBy(request.getEditedBy())
                        .fieldChanged("steps")
                        .oldValue(oldSteps)
                        .newValue(newSteps)
                        .build());

                // 새 결재선으로 업데이트
                List<ApprovalRequestDocument.Step> newStepsList = request.getSteps().stream()
                        .map(s -> ApprovalRequestDocument.Step.builder()
                                .step(s.getStep())
                                .approverId(s.getApproverId())
                                .status("pending")
                                .build())
                        .collect(Collectors.toList());
                document.setSteps(newStepsList);
            }
        }

        document.setEditHistory(history);
        document.setUpdatedAt(now);
        repository.save(document);

        // gRPC로 Processing Service에 업데이트 전달
        sendToProcessingService(document);

        log.info("Updated approval request: requestId={}", requestId);
    }

    // 결재 요청 취소 (요청자가 요청 자체를 취소)
    public void cancelApprovalRequest(Integer requestId, Integer requesterId) {
        ApprovalRequestDocument document = getRequest(requestId);

        // 1. 요청자 본인인지 확인
        if (!document.getRequesterId().equals(requesterId)) {
            throw new RuntimeException("Only the requester can cancel this request");
        }

        // 2. 아직 진행 중인지 확인
        if (!"in_progress".equals(document.getFinalStatus())) {
            throw new RuntimeException("Can only cancel requests that are in progress");
        }

        // 3. 아무도 승인/반려하지 않았는지 확인
        boolean anyProcessed = document.getSteps().stream()
                .anyMatch(s -> "approved".equals(s.getStatus()) || "rejected".equals(s.getStatus()));
        if (anyProcessed) {
            throw new RuntimeException("Cannot cancel: some steps have already been processed");
        }

        // 4. 취소 처리
        document.setFinalStatus("cancelled");
        for (ApprovalRequestDocument.Step step : document.getSteps()) {
            step.setStatus("cancelled");
            step.setUpdatedAt(LocalDateTime.now());
        }
        document.setUpdatedAt(LocalDateTime.now());
        repository.save(document);

        // 5. 모든 결재자에게 취소 알림 전송
        for (ApprovalRequestDocument.Step step : document.getSteps()) {
            kafkaProducer.sendRequestCancelledNotification(
                    requestId,
                    requesterId,
                    step.getApproverId(),
                    document.getTitle()
            );
        }

        log.info("Cancelled approval request: requestId={}", requestId);
    }

    // 승인/반려 철회 (결재자가 자신의 결재를 취소하고 다시 결재 가능)
    public void withdrawApproval(Integer requestId, Integer step, Integer approverId) {
        ApprovalRequestDocument document = getRequest(requestId);

        // 1. 해당 단계 찾기
        ApprovalRequestDocument.Step currentStep = document.getSteps().stream()
                .filter(s -> s.getStep().equals(step) && s.getApproverId().equals(approverId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Step not found"));

        String currentStatus = currentStep.getStatus();

        // 2. 현재 단계가 approved 또는 rejected인지 확인
        if (!"approved".equals(currentStatus) && !"rejected".equals(currentStatus)) {
            throw new RuntimeException("Can only withdraw approved or rejected steps");
        }

        // 3. 반려의 경우: 이 단계가 마지막 처리된 단계여야 함
        if ("rejected".equals(currentStatus)) {
            // 반려로 인해 finalStatus가 rejected가 된 경우만 철회 가능
            if (!"rejected".equals(document.getFinalStatus())) {
                throw new RuntimeException("Cannot withdraw: document status is not rejected");
            }
        }

        // 4. 승인의 경우: 다음 단계가 아직 pending인지 확인
        if ("approved".equals(currentStatus)) {
            boolean nextStepProcessed = document.getSteps().stream()
                    .filter(s -> s.getStep() > step)
                    .anyMatch(s -> "approved".equals(s.getStatus()) || "rejected".equals(s.getStatus()));
            if (nextStepProcessed) {
                throw new RuntimeException("Cannot withdraw: next step already processed");
            }
        }

        // 5. 철회 (pending으로 되돌림)
        currentStep.setStatus("pending");
        currentStep.setUpdatedAt(null);

        // 6. 반려였던 경우, 후속 단계들을 cancelled에서 pending으로 되돌림
        if ("rejected".equals(currentStatus)) {
            for (ApprovalRequestDocument.Step s : document.getSteps()) {
                if (s.getStep() > step && "cancelled".equals(s.getStatus())) {
                    s.setStatus("pending");
                    s.setUpdatedAt(null);
                }
            }
            document.setFinalStatus("in_progress");
        }

        document.setUpdatedAt(LocalDateTime.now());
        repository.save(document);

        // 7. Processing Service 대기열에 다시 추가
        sendToProcessingService(document);

        log.info("Withdrew {}: requestId={}, step={}, approverId={}", currentStatus, requestId, step, approverId);
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
            // 후속 단계들을 cancelled로 처리
            for (ApprovalRequestDocument.Step s : document.getSteps()) {
                if (s.getStep() > step && "pending".equals(s.getStatus())) {
                    s.setStatus("cancelled");
                    s.setUpdatedAt(LocalDateTime.now());
                }
            }
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

            // 다음 결재자에게 새 결재 요청 알림 전송
            kafkaProducer.sendNewApprovalNotification(
                    requestId,
                    document.getRequesterId(),
                    nextPendingStep.getApproverId(),
                    document.getTitle()
            );
            log.info("Sent new approval notification to next approver: {}", nextPendingStep.getApproverId());
        } else {
            // 모든 결재 완료
            document.setFinalStatus("approved");
            repository.save(document);
            sendNotification(document.getRequesterId(), requestId, "approved", null, "approved");
        }
    }

    private void sendToProcessingService(ApprovalRequestDocument document) {
        // Kafka로 결재 요청 전송
        try {
            approvalKafkaProducer.sendApprovalRequest(document);
            log.info("Approval request sent to Kafka: requestId={}", document.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send approval request to Kafka: requestId={}", document.getRequestId(), e);
        }
    }

    private void sendNotification(Integer requesterId, Integer requestId, String result,
                                   Integer rejectedBy, String finalResult) {
        // Kafka로 알림 전송 (비동기)
        try {
            kafkaProducer.sendApprovalResultNotification(requestId, requesterId, finalResult, null);
            log.info("Kafka notification sent: requestId={}, result={}", requestId, finalResult);
        } catch (Exception e) {
            log.warn("Kafka notification failed, falling back to REST: {}", e.getMessage());
        }

        // REST API로도 알림 전송 (기존 방식 유지 - fallback)
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
                            response -> log.info("REST notification sent: {}", response),
                            error -> log.error("Failed to send REST notification", error)
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

    // 결재 통계 조회
    public ApprovalStatistics getStatistics() {
        List<ApprovalRequestDocument> allRequests = repository.findAll();

        long totalRequests = allRequests.size();
        long approvedCount = allRequests.stream().filter(r -> "approved".equals(r.getFinalStatus())).count();
        long rejectedCount = allRequests.stream().filter(r -> "rejected".equals(r.getFinalStatus())).count();
        long inProgressCount = allRequests.stream().filter(r -> "in_progress".equals(r.getFinalStatus())).count();
        long cancelledCount = allRequests.stream().filter(r -> "cancelled".equals(r.getFinalStatus())).count();

        double approvalRate = (approvedCount + rejectedCount) > 0
                ? (double) approvedCount / (approvedCount + rejectedCount) * 100
                : 0;

        // 요청자별 통계
        Map<String, Long> requestsByRequester = allRequests.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRequesterId().toString(),
                        Collectors.counting()
                ));

        // 결재자별 승인 건수
        Map<String, Long> approvalsByApprover = new HashMap<>();
        for (ApprovalRequestDocument request : allRequests) {
            for (ApprovalRequestDocument.Step step : request.getSteps()) {
                if ("approved".equals(step.getStatus())) {
                    String approverId = step.getApproverId().toString();
                    approvalsByApprover.merge(approverId, 1L, Long::sum);
                }
            }
        }

        // 월별 추이
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> monthlyTrend = allRequests.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().format(monthFormatter),
                        Collectors.counting()
                ));

        return ApprovalStatistics.builder()
                .totalRequests(totalRequests)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .inProgressCount(inProgressCount)
                .cancelledCount(cancelledCount)
                .approvalRate(Math.round(approvalRate * 10) / 10.0)
                .requestsByRequester(requestsByRequester)
                .approvalsByApprover(approvalsByApprover)
                .monthlyTrend(monthlyTrend)
                .build();
    }


    // Processing Service와 동기화 - pending 상태의 결재를 재전송
    public int syncPendingApprovalsToProcessingService() {
        List<ApprovalRequestDocument> pendingRequests = repository.findAll().stream()
                .filter(r -> "pending".equals(r.getFinalStatus()) || "in_progress".equals(r.getFinalStatus()))
                .toList();

        int syncedCount = 0;
        for (ApprovalRequestDocument document : pendingRequests) {
            // pending 상태인 step이 있는 경우에만 전송
            boolean hasPendingStep = document.getSteps().stream()
                    .anyMatch(s -> "pending".equals(s.getStatus()));

            if (hasPendingStep) {
                sendToProcessingService(document);
                syncedCount++;
                log.info("Synced approval to Processing Service: requestId={}", document.getRequestId());
            }
        }

        log.info("Sync completed: {} approvals sent to Processing Service", syncedCount);
        return syncedCount;
    }
}

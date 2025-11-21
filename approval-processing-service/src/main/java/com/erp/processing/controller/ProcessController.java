package com.erp.processing.controller;

import com.erp.grpc.*;
import com.erp.processing.dto.ApprovalItem;
import com.erp.processing.dto.ProcessRequest;
import com.erp.processing.service.ApprovalQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class ProcessController {

    private final ApprovalQueueService queueService;

    @GrpcClient("approval-request-service")
    private ApprovalServiceGrpc.ApprovalServiceBlockingStub approvalServiceStub;

    // GET /process/{approverId} - 결재자 대기 목록 조회
    @GetMapping("/{approverId}")
    public ResponseEntity<List<ApprovalItem>> getApprovalQueue(@PathVariable String approverId) {
        List<ApprovalItem> queue = queueService.getQueue(approverId);
        return ResponseEntity.ok(queue);
    }

    // POST /process/{approverId}/{requestId} - 승인 또는 반려 처리
    @PostMapping("/{approverId}/{requestId}")
    public ResponseEntity<String> processApproval(
            @PathVariable String approverId,
            @PathVariable Integer requestId,
            @RequestBody ProcessRequest request) {

        log.info("Processing approval: approverId={}, requestId={}, status={}",
                approverId, requestId, request.getStatus());

        // 1. 대기열에서 해당 결재 건 제거
        Optional<ApprovalItem> itemOpt = queueService.removeFromQueue(approverId, requestId);

        if (itemOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Approval item not found in queue");
        }

        ApprovalItem item = itemOpt.get();

        // 현재 step 찾기
        int currentStep = 0;
        for (ApprovalItem.StepInfo step : item.getSteps()) {
            if (step.getApproverId().equals(Integer.parseInt(approverId))) {
                currentStep = step.getStep();
                break;
            }
        }

        // 2. gRPC로 Approval Request Service에 결과 전달
        try {
            ApprovalResultRequest resultRequest = ApprovalResultRequest.newBuilder()
                    .setRequestId(requestId)
                    .setStep(currentStep)
                    .setApproverId(Integer.parseInt(approverId))
                    .setStatus(request.getStatus())
                    .build();

            ApprovalResultResponse response = approvalServiceStub.returnApprovalResult(resultRequest);
            log.info("gRPC ReturnApprovalResult response: {}", response.getStatus());

            return ResponseEntity.ok("Processed: " + request.getStatus());
        } catch (Exception e) {
            log.error("Failed to send result via gRPC", e);
            return ResponseEntity.internalServerError().body("Failed to process: " + e.getMessage());
        }
    }
}

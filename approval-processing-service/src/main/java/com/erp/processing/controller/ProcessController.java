package com.erp.processing.controller;

import com.erp.processing.dto.ApprovalItem;
import com.erp.processing.dto.ProcessRequest;
import com.erp.processing.kafka.ApprovalResultKafkaProducer;
import com.erp.processing.service.ApprovalQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class ProcessController {

    private final ApprovalQueueService queueService;
    private final ApprovalResultKafkaProducer kafkaProducer;

    // GET /process/{approverId} - 결재자 대기 목록 조회
    @GetMapping("/{approverId}")
    public ResponseEntity<List<ApprovalItem>> getApprovalQueue(@PathVariable String approverId) {
        List<ApprovalItem> queue = queueService.getQueue(approverId);
        return ResponseEntity.ok(queue);
    }

    // POST /process/{approverId}/{requestId} - 승인 또는 반려 처리
    @PostMapping("/{approverId}/{requestId}")
    public ResponseEntity<Map<String, Object>> processApproval(
            @PathVariable String approverId,
            @PathVariable Integer requestId,
            @RequestBody ProcessRequest request) {

        log.info("Processing approval: approverId={}, requestId={}, status={}",
                approverId, requestId, request.getStatus());

        // 1. 대기열에서 해당 결재 건 제거
        Optional<ApprovalItem> itemOpt = queueService.removeFromQueue(approverId, requestId);

        if (itemOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Approval item not found in queue"
            ));
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

        // 2. Kafka로 Approval Request Service에 결과 전달
        try {
            kafkaProducer.sendApprovalResult(requestId, currentStep, Integer.parseInt(approverId), request.getStatus());
            log.info("Approval result sent to Kafka: requestId={}, step={}, status={}",
                    requestId, currentStep, request.getStatus());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Processed: " + request.getStatus(),
                    "requestId", requestId
            ));
        } catch (Exception e) {
            log.error("Failed to send result via Kafka", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to process: " + e.getMessage()
            ));
        }
    }
}

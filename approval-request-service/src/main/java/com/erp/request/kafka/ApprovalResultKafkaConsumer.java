package com.erp.request.kafka;

import com.erp.request.service.ApprovalRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalResultKafkaConsumer {

    private final ApprovalRequestService approvalRequestService;

    @KafkaListener(topics = "${kafka.topic.approval-results}", groupId = "approval-request-group")
    public void consumeApprovalResult(ApprovalResultEvent event) {
        log.info("Received approval result from Kafka: requestId={}, step={}, approverId={}, status={}",
                event.getRequestId(), event.getStep(), event.getApproverId(), event.getStatus());

        try {
            approvalRequestService.processApprovalResult(
                    event.getRequestId(),
                    event.getStep(),
                    event.getApproverId(),
                    event.getStatus()
            );
            log.info("Approval result processed successfully: requestId={}", event.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process approval result: requestId={}, error={}",
                    event.getRequestId(), e.getMessage(), e);
        }
    }
}

package com.erp.processing.kafka;

import com.erp.processing.dto.ApprovalItem;
import com.erp.processing.service.ApprovalQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRequestKafkaConsumer {

    private final ApprovalQueueService queueService;

    @KafkaListener(topics = "${kafka.topic.approval-requests}", groupId = "approval-processing-group")
    public void consumeApprovalRequest(ApprovalRequestEvent event) {
        log.info("Received approval request from Kafka: requestId={}, title={}",
                event.getRequestId(), event.getTitle());

        try {
            // Kafka 이벤트를 내부 객체로 변환
            ApprovalItem item = ApprovalItem.builder()
                    .requestId(event.getRequestId())
                    .requesterId(event.getRequesterId())
                    .title(event.getTitle())
                    .content(event.getContent())
                    .steps(event.getSteps().stream()
                            .map(step -> ApprovalItem.StepInfo.builder()
                                    .step(step.getStep())
                                    .approverId(step.getApproverId())
                                    .status(step.getStatus())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            // 첫 번째 pending 결재자 찾기
            String pendingApproverId = queueService.findFirstPendingApproverId(item.getSteps());
            if (pendingApproverId != null) {
                queueService.addToQueue(pendingApproverId, item);
                log.info("Added to approval queue: approverId={}, requestId={}",
                        pendingApproverId, event.getRequestId());
            } else {
                log.warn("No pending approver found for requestId={}", event.getRequestId());
            }
        } catch (Exception e) {
            log.error("Failed to process approval request: requestId={}, error={}",
                    event.getRequestId(), e.getMessage(), e);
        }
    }
}

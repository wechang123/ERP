package com.erp.processing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalResultKafkaProducer {

    private final KafkaTemplate<String, ApprovalResultEvent> kafkaTemplate;

    @Value("${kafka.topic.approval-results}")
    private String approvalResultsTopic;

    public void sendApprovalResult(Integer requestId, Integer step, Integer approverId, String status) {
        ApprovalResultEvent event = ApprovalResultEvent.builder()
                .requestId(requestId)
                .step(step)
                .approverId(approverId)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(approvalResultsTopic, requestId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Approval result sent to Kafka: requestId={}, step={}, status={}",
                                requestId, step, status);
                    } else {
                        log.error("Failed to send approval result to Kafka: requestId={}",
                                requestId, ex);
                    }
                });
    }
}

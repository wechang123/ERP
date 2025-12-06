package com.erp.request.kafka;

import com.erp.request.document.ApprovalRequestDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRequestKafkaProducer {

    private final KafkaTemplate<String, ApprovalRequestEvent> kafkaTemplate;

    @Value("${kafka.topic.approval-requests}")
    private String approvalRequestsTopic;

    public void sendApprovalRequest(ApprovalRequestDocument document) {
        ApprovalRequestEvent event = ApprovalRequestEvent.builder()
                .requestId(document.getRequestId())
                .requesterId(document.getRequesterId())
                .title(document.getTitle())
                .content(document.getContent())
                .steps(document.getSteps().stream()
                        .map(step -> ApprovalRequestEvent.StepInfo.builder()
                                .step(step.getStep())
                                .approverId(step.getApproverId())
                                .status(step.getStatus())
                                .build())
                        .collect(Collectors.toList()))
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(approvalRequestsTopic, document.getRequestId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Approval request sent to Kafka: requestId={}, topic={}",
                                document.getRequestId(), approvalRequestsTopic);
                    } else {
                        log.error("Failed to send approval request to Kafka: requestId={}",
                                document.getRequestId(), ex);
                    }
                });
    }
}

package com.erp.notification.kafka;

import com.erp.notification.dto.NotificationMessage;
import com.erp.notification.handler.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationWebSocketHandler webSocketHandler;

    @KafkaListener(topics = "${kafka.topic.notification}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeApprovalNotification(ApprovalNotificationEvent event) {
        log.info("Received Kafka message: targetEmployee={}, type={}, requestId={}",
                event.getTargetEmployeeId(), event.getType(), event.getRequestId());

        try {
            NotificationMessage message = NotificationMessage.builder()
                    .type(event.getType())
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .requestId(event.getRequestId())
                    .requesterId(event.getRequesterId())
                    .status(event.getStatus())
                    .build();

            webSocketHandler.sendNotification(event.getTargetEmployeeId(), message);
            log.info("Notification sent via WebSocket to employee: {}", event.getTargetEmployeeId());
        } catch (Exception e) {
            log.error("Failed to process notification: targetEmployee={}, error={}",
                    event.getTargetEmployeeId(), e.getMessage());
        }
    }
}

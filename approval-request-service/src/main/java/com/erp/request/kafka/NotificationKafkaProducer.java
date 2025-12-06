package com.erp.request.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationKafkaProducer {

    private final KafkaTemplate<String, ApprovalNotificationEvent> kafkaTemplate;

    @Value("${kafka.topic.notification}")
    private String notificationTopic;

    public void sendApprovalNotification(String targetEmployeeId, String type, String title,
                                          String message, Integer requestId, Integer requesterId, String status) {
        ApprovalNotificationEvent event = ApprovalNotificationEvent.builder()
                .targetEmployeeId(targetEmployeeId)
                .type(type)
                .title(title)
                .message(message)
                .requestId(requestId)
                .requesterId(requesterId)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, ApprovalNotificationEvent>> future =
                kafkaTemplate.send(notificationTopic, targetEmployeeId, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka message sent successfully: topic={}, partition={}, offset={}, targetEmployee={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        targetEmployeeId);
            } else {
                log.error("Failed to send Kafka message: targetEmployee={}, error={}",
                        targetEmployeeId, ex.getMessage());
            }
        });
    }

    public void sendApprovalResultNotification(Integer requestId, Integer requesterId,
                                                String status, String requesterName) {
        String title = "approved".equals(status) ? "결재 승인" : "결재 반려";
        String message = String.format("요청 #%d이(가) %s되었습니다.",
                requestId, "approved".equals(status) ? "승인" : "반려");

        sendApprovalNotification(
                requesterId.toString(),
                "APPROVAL_RESULT",
                title,
                message,
                requestId,
                requesterId,
                status
        );
    }

    public void sendNewApprovalNotification(Integer requestId, Integer requesterId,
                                             Integer approverId, String requestTitle) {
        String title = "새 결재 요청";
        String message = String.format("새로운 결재 요청이 도착했습니다: %s", requestTitle);

        sendApprovalNotification(
                approverId.toString(),
                "NEW_APPROVAL",
                title,
                message,
                requestId,
                requesterId,
                "pending"
        );
    }

    public void sendDeadlineReminderNotification(Integer requestId, Integer requesterId,
                                                  Integer approverId, String requestTitle,
                                                  LocalDateTime deadline) {
        String title = "결재 기한 임박";
        String message = String.format("결재 요청 '%s'의 기한이 곧 만료됩니다. (기한: %s)",
                requestTitle, deadline.toString().replace("T", " "));

        sendApprovalNotification(
                approverId.toString(),
                "DEADLINE_REMINDER",
                title,
                message,
                requestId,
                requesterId,
                "pending"
        );
    }

    public void sendDeadlineOverdueNotification(Integer requestId, Integer requesterId,
                                                 String requestTitle) {
        String title = "결재 기한 초과";
        String message = String.format("결재 요청 '%s'의 기한이 지났습니다. 빠른 처리가 필요합니다.", requestTitle);

        sendApprovalNotification(
                requesterId.toString(),
                "DEADLINE_OVERDUE",
                title,
                message,
                requestId,
                requesterId,
                "overdue"
        );
    }

    public void sendRequestCancelledNotification(Integer requestId, Integer requesterId,
                                                  Integer approverId, String requestTitle) {
        String title = "결재 요청 취소";
        String message = String.format("결재 요청 '%s'이(가) 요청자에 의해 취소되었습니다.", requestTitle);

        sendApprovalNotification(
                approverId.toString(),
                "REQUEST_CANCELLED",
                title,
                message,
                requestId,
                requesterId,
                "cancelled"
        );
    }
}

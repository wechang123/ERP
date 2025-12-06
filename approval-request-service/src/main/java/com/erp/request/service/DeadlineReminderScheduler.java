package com.erp.request.service;

import com.erp.request.document.ApprovalRequestDocument;
import com.erp.request.kafka.NotificationKafkaProducer;
import com.erp.request.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineReminderScheduler {

    private final ApprovalRequestRepository repository;
    private final NotificationKafkaProducer kafkaProducer;

    // 매 시간마다 기한 임박 결재 확인 (24시간 이내)
    @Scheduled(fixedRate = 3600000) // 1시간마다
    public void checkApproachingDeadlines() {
        log.info("Checking for approaching deadlines...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24Hours = now.plusHours(24);

        // 24시간 이내에 기한이 도래하는 진행 중인 결재 요청 찾기
        List<ApprovalRequestDocument> approachingDeadlines = repository
                .findByFinalStatusAndDeadlineNotNullAndReminderSentFalseAndDeadlineBetween(
                        "in_progress", now, in24Hours);

        for (ApprovalRequestDocument doc : approachingDeadlines) {
            // 현재 결재 차례인 결재자 찾기
            Integer currentApproverId = findCurrentApprover(doc);
            if (currentApproverId != null) {
                kafkaProducer.sendDeadlineReminderNotification(
                        doc.getRequestId(),
                        doc.getRequesterId(),
                        currentApproverId,
                        doc.getTitle(),
                        doc.getDeadline()
                );

                // 리마인더 발송 표시
                doc.setReminderSent(true);
                repository.save(doc);
                log.info("Sent deadline reminder for request {}", doc.getRequestId());
            }
        }
    }

    // 매 시간마다 기한 초과 결재 확인
    @Scheduled(fixedRate = 3600000) // 1시간마다
    public void checkOverdueApprovals() {
        log.info("Checking for overdue approvals...");

        LocalDateTime now = LocalDateTime.now();

        // 기한이 지난 진행 중인 결재 요청 찾기
        List<ApprovalRequestDocument> overdueRequests = repository
                .findByFinalStatusAndDeadlineNotNullAndDeadlineBefore("in_progress", now);

        for (ApprovalRequestDocument doc : overdueRequests) {
            // 요청자에게 기한 초과 알림
            kafkaProducer.sendDeadlineOverdueNotification(
                    doc.getRequestId(),
                    doc.getRequesterId(),
                    doc.getTitle()
            );

            log.info("Sent overdue notification for request {}", doc.getRequestId());
        }
    }

    // 현재 결재 차례인 결재자 ID 찾기
    private Integer findCurrentApprover(ApprovalRequestDocument doc) {
        for (ApprovalRequestDocument.Step step : doc.getSteps()) {
            if ("pending".equals(step.getStatus())) {
                return step.getApproverId();
            }
        }
        return null;
    }
}

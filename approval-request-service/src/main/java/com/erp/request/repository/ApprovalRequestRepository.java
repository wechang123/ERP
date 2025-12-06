package com.erp.request.repository;

import com.erp.request.document.ApprovalRequestDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends MongoRepository<ApprovalRequestDocument, String> {
    Optional<ApprovalRequestDocument> findByRequestId(Integer requestId);
    Optional<ApprovalRequestDocument> findTopByOrderByRequestIdDesc();

    // 기한 임박 알림: 진행 중이고, 기한이 설정되어 있고, 리마인더 발송되지 않았고, 기한이 특정 시간 이전인 요청
    List<ApprovalRequestDocument> findByFinalStatusAndDeadlineNotNullAndReminderSentFalseAndDeadlineBetween(
            String finalStatus, LocalDateTime start, LocalDateTime end);

    // 기한 초과 요청: 진행 중이고, 기한이 지난 요청
    List<ApprovalRequestDocument> findByFinalStatusAndDeadlineNotNullAndDeadlineBefore(
            String finalStatus, LocalDateTime deadline);
}

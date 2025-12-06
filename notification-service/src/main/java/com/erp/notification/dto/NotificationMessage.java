package com.erp.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private Integer requestId;
    private String result;        // approved or rejected
    private Integer rejectedBy;   // 반려한 결재자 ID (반려 시에만)
    private String finalResult;   // approved or rejected

    // Kafka 메시지용 추가 필드
    private String type;          // APPROVAL_RESULT, NEW_APPROVAL 등
    private String title;         // 알림 제목
    private String message;       // 알림 내용
    private Integer requesterId;  // 요청자 ID
    private String status;        // 상태
}

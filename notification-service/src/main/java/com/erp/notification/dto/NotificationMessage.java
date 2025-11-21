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
}

package com.erp.request.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalNotificationEvent {
    private String targetEmployeeId;
    private String type;
    private String title;
    private String message;
    private Integer requestId;
    private Integer requesterId;
    private String status;
    private LocalDateTime timestamp;
}

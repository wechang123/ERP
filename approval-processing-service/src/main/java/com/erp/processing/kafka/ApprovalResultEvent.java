package com.erp.processing.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResultEvent {
    private Integer requestId;
    private Integer step;
    private Integer approverId;
    private String status;  // "approved" or "rejected"
    private LocalDateTime timestamp;
}

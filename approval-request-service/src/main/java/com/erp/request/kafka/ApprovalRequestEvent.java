package com.erp.request.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestEvent {
    private Integer requestId;
    private Integer requesterId;
    private String title;
    private String content;
    private List<StepInfo> steps;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        private Integer step;
        private Integer approverId;
        private String status;
    }
}

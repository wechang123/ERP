package com.erp.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalItem {
    private Integer requestId;
    private Integer requesterId;
    private String title;
    private String content;
    private List<StepInfo> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        private Integer step;
        private Integer approverId;
        private String status;  // pending, approved, rejected
    }
}

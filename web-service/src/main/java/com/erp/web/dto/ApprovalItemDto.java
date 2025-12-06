package com.erp.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalItemDto {
    private Integer requestId;
    private Integer requesterId;
    private String title;
    private String content;
    private List<StepInfo> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        private Integer step;
        private Integer approverId;
        private String status;
    }

    // Get current pending step for this approver
    public Integer getCurrentStep(Integer approverId) {
        if (steps == null) return null;
        return steps.stream()
                .filter(s -> s.getApproverId().equals(approverId) && "pending".equals(s.getStatus()))
                .map(StepInfo::getStep)
                .findFirst()
                .orElse(null);
    }
}

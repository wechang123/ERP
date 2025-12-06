package com.erp.web.dto;

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
public class ApprovalTemplateDto {
    private String id;
    private String name;
    private String description;
    private Integer createdBy;
    private List<TemplateStepDto> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isPublic;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStepDto {
        private Integer step;
        private Integer approverId;
        private String approverName;
    }
}

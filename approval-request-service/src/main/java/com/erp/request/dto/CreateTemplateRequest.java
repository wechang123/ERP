package com.erp.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateRequest {
    private String name;
    private String description;
    private Integer createdBy;
    private List<TemplateStepDto> steps;
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

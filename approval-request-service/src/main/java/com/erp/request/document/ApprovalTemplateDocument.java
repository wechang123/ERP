package com.erp.request.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "approval_templates")
public class ApprovalTemplateDocument {

    @Id
    private String id;

    private String name;
    private String description;
    private Integer createdBy;
    private List<TemplateStep> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isPublic;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStep {
        private Integer step;
        private Integer approverId;
        private String approverName;
    }
}

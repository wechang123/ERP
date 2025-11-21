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
@Document(collection = "approval_requests")
public class ApprovalRequestDocument {

    @Id
    private String id;

    private Integer requestId;
    private Integer requesterId;
    private String title;
    private String content;
    private List<Step> steps;
    private String finalStatus;  // in_progress, approved, rejected
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private Integer step;
        private Integer approverId;
        private String status;  // pending, approved, rejected
        private LocalDateTime updatedAt;
    }
}

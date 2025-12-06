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
    private String finalStatus;  // in_progress, approved, rejected, cancelled
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deadline;  // 결재 기한
    private boolean reminderSent;    // 리마인더 발송 여부
    private List<EditHistory> editHistory;  // 수정 이력

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private Integer step;
        private Integer approverId;
        private String status;  // pending, approved, rejected, cancelled
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditHistory {
        private LocalDateTime editedAt;      // 수정 시간
        private Integer editedBy;            // 수정한 사람 ID
        private String fieldChanged;         // 변경된 필드 (title, content, steps)
        private String oldValue;             // 이전 값
        private String newValue;             // 새 값
    }
}

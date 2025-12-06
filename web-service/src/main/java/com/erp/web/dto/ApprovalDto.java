package com.erp.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDto {
    private String id;
    private Integer requestId;
    private Integer requesterId;
    private String title;
    private String content;
    private List<StepDto> steps;
    private String finalStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deadline;           // 결재 기한
    private boolean reminderSent;             // 리마인더 발송 여부
    private List<EditHistoryDto> editHistory;  // 수정 이력

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDto {
        private Integer step;
        private Integer approverId;
        private String status;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditHistoryDto {
        private LocalDateTime editedAt;
        private Integer editedBy;
        private String fieldChanged;
        private String oldValue;
        private String newValue;
    }
}

package com.erp.request.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateApprovalRequest {
    private Integer requesterId;
    private String title;
    private String content;
    private LocalDateTime deadline;  // 결재 기한
    private List<StepRequest> steps;

    @Data
    public static class StepRequest {
        private Integer step;
        private Integer approverId;
    }
}

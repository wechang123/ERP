package com.erp.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApprovalDto {
    private Integer requesterId;
    private String title;
    private String content;
    private LocalDateTime deadline;  // 결재 기한
    private List<StepRequest> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepRequest {
        private Integer step;
        private Integer approverId;
    }
}

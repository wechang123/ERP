package com.erp.request.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateApprovalRequest {
    private Integer editedBy;        // 수정하는 사람 ID (요청자 본인이어야 함)
    private String title;
    private String content;
    private List<StepRequest> steps;

    @Data
    public static class StepRequest {
        private Integer step;
        private Integer approverId;
    }
}

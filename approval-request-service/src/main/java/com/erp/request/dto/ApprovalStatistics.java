package com.erp.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStatistics {
    private long totalRequests;
    private long approvedCount;
    private long rejectedCount;
    private long inProgressCount;
    private long cancelledCount;
    private double approvalRate;
    private Map<String, Long> requestsByRequester;
    private Map<String, Long> approvalsByApprover;
    private Map<String, Long> monthlyTrend;
}

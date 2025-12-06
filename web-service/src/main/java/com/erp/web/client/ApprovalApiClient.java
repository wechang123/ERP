package com.erp.web.client;

import com.erp.web.dto.ApprovalDto;
import com.erp.web.dto.ApprovalStatisticsDto;
import com.erp.web.dto.CreateApprovalDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalApiClient {

    private final RestTemplate restTemplate;

    @Value("${services.approval-url}")
    private String baseUrl;

    public List<ApprovalDto> getAllApprovals() {
        try {
            ResponseEntity<List<ApprovalDto>> response = restTemplate.exchange(
                    baseUrl + "/approvals",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ApprovalDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get approvals: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ApprovalDto getApproval(Integer requestId) {
        try {
            return restTemplate.getForObject(baseUrl + "/approvals/" + requestId, ApprovalDto.class);
        } catch (Exception e) {
            log.error("Failed to get approval {}: {}", requestId, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> createApproval(CreateApprovalDto request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/approvals",
                    request,
                    Map.class
            );
            return response;
        } catch (Exception e) {
            log.error("Failed to create approval: {}", e.getMessage());
            throw new RuntimeException("Failed to create approval", e);
        }
    }

    public int getApprovalCount() {
        List<ApprovalDto> approvals = getAllApprovals();
        return approvals != null ? approvals.size() : 0;
    }

    public int getInProgressCount() {
        List<ApprovalDto> approvals = getAllApprovals();
        if (approvals == null) return 0;
        return (int) approvals.stream()
                .filter(a -> "in_progress".equals(a.getFinalStatus()))
                .count();
    }

    // 결재 요청 수정
    public void updateApproval(Integer requestId, CreateApprovalDto request, Integer editedBy) {
        try {
            Map<String, Object> updateRequest = Map.of(
                    "editedBy", editedBy,
                    "title", request.getTitle(),
                    "content", request.getContent(),
                    "steps", request.getSteps()
            );
            restTemplate.put(baseUrl + "/approvals/" + requestId, updateRequest);
        } catch (Exception e) {
            log.error("Failed to update approval {}: {}", requestId, e.getMessage());
            throw new RuntimeException("Failed to update approval: " + e.getMessage(), e);
        }
    }

    // 결재 요청 취소
    public void cancelApproval(Integer requestId, Integer requesterId) {
        try {
            Map<String, Integer> body = Map.of("requesterId", requesterId);
            restTemplate.postForObject(
                    baseUrl + "/approvals/" + requestId + "/cancel",
                    body,
                    Map.class
            );
        } catch (Exception e) {
            log.error("Failed to cancel approval {}: {}", requestId, e.getMessage());
            throw new RuntimeException("Failed to cancel approval: " + e.getMessage(), e);
        }
    }

    // 승인 철회
    public void withdrawApproval(Integer requestId, Integer step, Integer approverId) {
        try {
            Map<String, Integer> body = Map.of("approverId", approverId);
            restTemplate.postForObject(
                    baseUrl + "/approvals/" + requestId + "/steps/" + step + "/withdraw",
                    body,
                    Map.class
            );
        } catch (Exception e) {
            log.error("Failed to withdraw approval {} step {}: {}", requestId, step, e.getMessage());
            throw new RuntimeException("Failed to withdraw approval: " + e.getMessage(), e);
        }
    }

    // 결재 통계 조회
    public ApprovalStatisticsDto getStatistics() {
        try {
            return restTemplate.getForObject(baseUrl + "/approvals/statistics", ApprovalStatisticsDto.class);
        } catch (Exception e) {
            log.error("Failed to get statistics: {}", e.getMessage());
            return ApprovalStatisticsDto.builder()
                    .totalRequests(0)
                    .approvedCount(0)
                    .rejectedCount(0)
                    .inProgressCount(0)
                    .cancelledCount(0)
                    .approvalRate(0.0)
                    .build();
        }
    }
}

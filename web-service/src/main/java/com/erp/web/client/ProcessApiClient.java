package com.erp.web.client;

import com.erp.web.dto.ApprovalDto;
import com.erp.web.dto.ApprovalItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessApiClient {

    private final RestTemplate restTemplate;

    @Value("${services.processing-url}")
    private String baseUrl;

    @Value("${services.approval-url}")
    private String approvalUrl;

    public List<ApprovalItemDto> getPendingApprovals(Integer approverId) {
        // approval-request-service에서 직접 가져와서 필터링
        // (processing-service가 인메모리 큐를 사용하여 재시작 시 데이터 손실)
        try {
            ResponseEntity<List<ApprovalDto>> response = restTemplate.exchange(
                    approvalUrl + "/approvals",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ApprovalDto>>() {}
            );
            List<ApprovalDto> allApprovals = response.getBody();
            if (allApprovals == null) {
                return Collections.emptyList();
            }

            // 현재 결재자 차례인 항목만 필터링
            return allApprovals.stream()
                    .filter(a -> "in_progress".equals(a.getFinalStatus()))
                    .filter(a -> isCurrentApproverTurn(a, approverId))
                    .map(this::toApprovalItemDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get pending approvals for approver {}: {}", approverId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // 현재 결재자의 차례인지 확인 (이전 단계가 모두 승인되고, 현재 단계가 pending인 경우)
    private boolean isCurrentApproverTurn(ApprovalDto approval, Integer approverId) {
        List<ApprovalDto.StepDto> steps = approval.getSteps();
        if (steps == null || steps.isEmpty()) {
            return false;
        }

        for (int i = 0; i < steps.size(); i++) {
            ApprovalDto.StepDto step = steps.get(i);

            // 이 결재자의 단계를 찾음
            if (step.getApproverId().equals(approverId)) {
                // pending 상태여야 함
                if (!"pending".equals(step.getStatus())) {
                    return false;
                }
                // 이전 단계들이 모두 approved여야 함
                for (int j = 0; j < i; j++) {
                    if (!"approved".equals(steps.get(j).getStatus())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    // ApprovalDto -> ApprovalItemDto 변환
    private ApprovalItemDto toApprovalItemDto(ApprovalDto approval) {
        List<ApprovalItemDto.StepInfo> stepInfos = approval.getSteps().stream()
                .map(s -> new ApprovalItemDto.StepInfo(s.getStep(), s.getApproverId(), s.getStatus()))
                .collect(Collectors.toList());

        return new ApprovalItemDto(
                approval.getRequestId(),
                approval.getRequesterId(),
                approval.getTitle(),
                approval.getContent(),
                stepInfos
        );
    }

    public Map<String, Object> processApproval(Integer approverId, Integer requestId, String status) {
        try {
            Map<String, String> request = Map.of("status", status);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    baseUrl + "/process/" + approverId + "/" + requestId,
                    HttpMethod.POST,
                    entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to process approval: approverId={}, requestId={}, status={}, error={}",
                    approverId, requestId, status, e.getMessage());
            throw new RuntimeException("Failed to process approval", e);
        }
    }

    public int getPendingCount(Integer approverId) {
        List<ApprovalItemDto> items = getPendingApprovals(approverId);
        return items != null ? items.size() : 0;
    }
}

package com.erp.processing.service;

import com.erp.processing.dto.ApprovalItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ApprovalQueueService {

    // approverId -> List<ApprovalItem> 인메모리 저장소
    private final Map<String, List<ApprovalItem>> approvalQueue = new ConcurrentHashMap<>();

    // 결재 대기열에 추가
    public void addToQueue(String approverId, ApprovalItem item) {
        approvalQueue.computeIfAbsent(approverId, k -> new ArrayList<>()).add(item);
        log.info("Added to queue for approverId={}: requestId={}", approverId, item.getRequestId());
    }

    // 결재자의 대기 목록 조회
    public List<ApprovalItem> getQueue(String approverId) {
        return approvalQueue.getOrDefault(approverId, new ArrayList<>());
    }

    // 대기열에서 특정 결재 건 제거 및 반환
    public Optional<ApprovalItem> removeFromQueue(String approverId, Integer requestId) {
        List<ApprovalItem> queue = approvalQueue.get(approverId);
        if (queue != null) {
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getRequestId().equals(requestId)) {
                    ApprovalItem removed = queue.remove(i);
                    log.info("Removed from queue: approverId={}, requestId={}", approverId, requestId);
                    return Optional.of(removed);
                }
            }
        }
        return Optional.empty();
    }

    // 첫 번째 pending 상태인 approverId 찾기
    public String findFirstPendingApproverId(List<ApprovalItem.StepInfo> steps) {
        return steps.stream()
                .filter(step -> "pending".equals(step.getStatus()))
                .findFirst()
                .map(step -> String.valueOf(step.getApproverId()))
                .orElse(null);
    }
}

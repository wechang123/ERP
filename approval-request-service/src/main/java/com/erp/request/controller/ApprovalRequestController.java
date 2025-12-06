package com.erp.request.controller;

import com.erp.request.document.ApprovalRequestDocument;
import com.erp.request.dto.ApprovalStatistics;
import com.erp.request.dto.CreateApprovalRequest;
import com.erp.request.dto.UpdateApprovalRequest;
import com.erp.request.service.ApprovalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;

    // POST /approvals - 결재 요청 생성
    @PostMapping
    public ResponseEntity<Map<String, Integer>> createApproval(@RequestBody CreateApprovalRequest request) {
        Integer requestId = approvalRequestService.createApprovalRequest(request);
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    // GET /approvals - 결재 요청 목록 조회
    @GetMapping
    public ResponseEntity<List<ApprovalRequestDocument>> getAllApprovals() {
        return ResponseEntity.ok(approvalRequestService.getAllRequests());
    }

    // GET /approvals/{requestId} - 결재 요청 상세 조회
    @GetMapping("/{requestId}")
    public ResponseEntity<ApprovalRequestDocument> getApproval(@PathVariable Integer requestId) {
        return ResponseEntity.ok(approvalRequestService.getRequest(requestId));
    }

    // PUT /approvals/{requestId} - 결재 요청 수정
    @PutMapping("/{requestId}")
    public ResponseEntity<Map<String, String>> updateApproval(
            @PathVariable Integer requestId,
            @RequestBody UpdateApprovalRequest request) {
        try {
            approvalRequestService.updateApprovalRequest(requestId, request);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Request updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // POST /approvals/{requestId}/cancel - 결재 요청 취소
    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<Map<String, String>> cancelApproval(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Integer> body) {
        try {
            Integer requesterId = body.get("requesterId");
            approvalRequestService.cancelApprovalRequest(requestId, requesterId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Request cancelled"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // POST /approvals/{requestId}/steps/{step}/withdraw - 승인 철회
    @PostMapping("/{requestId}/steps/{step}/withdraw")
    public ResponseEntity<Map<String, String>> withdrawApproval(
            @PathVariable Integer requestId,
            @PathVariable Integer step,
            @RequestBody Map<String, Integer> body) {
        try {
            Integer approverId = body.get("approverId");
            approvalRequestService.withdrawApproval(requestId, step, approverId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Approval withdrawn"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // GET /approvals/statistics - 결재 통계 조회
    @GetMapping("/statistics")
    public ResponseEntity<ApprovalStatistics> getStatistics() {
        return ResponseEntity.ok(approvalRequestService.getStatistics());
    }

    // POST /approvals/sync - Processing Service와 동기화 (pending 결재 재전송)
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncWithProcessingService() {
        int syncedCount = approvalRequestService.syncPendingApprovalsToProcessingService();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Synced " + syncedCount + " pending approvals to Processing Service",
                "syncedCount", syncedCount
        ));
    }
}

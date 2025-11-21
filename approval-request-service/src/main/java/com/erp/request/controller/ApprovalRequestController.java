package com.erp.request.controller;

import com.erp.request.document.ApprovalRequestDocument;
import com.erp.request.dto.CreateApprovalRequest;
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
}

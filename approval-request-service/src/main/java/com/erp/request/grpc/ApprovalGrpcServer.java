package com.erp.request.grpc;

import com.erp.grpc.*;
import com.erp.request.service.ApprovalRequestService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ApprovalGrpcServer extends ApprovalServiceGrpc.ApprovalServiceImplBase {

    private final ApprovalRequestService approvalRequestService;

    @Override
    public void requestApproval(ApprovalRequest request, StreamObserver<ApprovalResponse> responseObserver) {
        // 이 메서드는 Processing Service에서 구현됨
        ApprovalResponse response = ApprovalResponse.newBuilder()
                .setStatus("received")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void returnApprovalResult(ApprovalResultRequest request, StreamObserver<ApprovalResultResponse> responseObserver) {
        log.info("Received ReturnApprovalResult: requestId={}, step={}, approverId={}, status={}",
                request.getRequestId(), request.getStep(), request.getApproverId(), request.getStatus());

        try {
            approvalRequestService.processApprovalResult(
                    request.getRequestId(),
                    request.getStep(),
                    request.getApproverId(),
                    request.getStatus()
            );

            ApprovalResultResponse response = ApprovalResultResponse.newBuilder()
                    .setStatus("processed")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing approval result", e);
            responseObserver.onError(e);
        }
    }
}

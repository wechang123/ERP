package com.erp.processing.grpc;

import com.erp.grpc.*;
import com.erp.processing.dto.ApprovalItem;
import com.erp.processing.service.ApprovalQueueService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ApprovalGrpcService extends ApprovalServiceGrpc.ApprovalServiceImplBase {

    private final ApprovalQueueService queueService;

    @Override
    public void requestApproval(ApprovalRequest request, StreamObserver<ApprovalResponse> responseObserver) {
        log.info("Received RequestApproval: requestId={}", request.getRequestId());

        // ApprovalRequest를 ApprovalItem으로 변환
        ApprovalItem item = ApprovalItem.builder()
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .content(request.getContent())
                .steps(request.getStepsList().stream()
                        .map(step -> ApprovalItem.StepInfo.builder()
                                .step(step.getStep())
                                .approverId(step.getApproverId())
                                .status(step.getStatus())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        // 첫 번째 pending인 approverId 찾아서 대기열에 추가
        String pendingApproverId = queueService.findFirstPendingApproverId(item.getSteps());
        if (pendingApproverId != null) {
            queueService.addToQueue(pendingApproverId, item);
        }

        ApprovalResponse response = ApprovalResponse.newBuilder()
                .setStatus("received")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void returnApprovalResult(ApprovalResultRequest request, StreamObserver<ApprovalResultResponse> responseObserver) {
        // 이 메서드는 Approval Request Service에서 구현됨
        // Processing Service는 클라이언트로서 호출함
        ApprovalResultResponse response = ApprovalResultResponse.newBuilder()
                .setStatus("processed")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

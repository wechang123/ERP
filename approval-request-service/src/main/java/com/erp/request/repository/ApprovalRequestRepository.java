package com.erp.request.repository;

import com.erp.request.document.ApprovalRequestDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends MongoRepository<ApprovalRequestDocument, String> {
    Optional<ApprovalRequestDocument> findByRequestId(Integer requestId);
    Optional<ApprovalRequestDocument> findTopByOrderByRequestIdDesc();
}

package com.erp.request.repository;

import com.erp.request.document.ApprovalTemplateDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalTemplateRepository extends MongoRepository<ApprovalTemplateDocument, String> {

    List<ApprovalTemplateDocument> findByCreatedByOrIsPublicTrue(Integer createdBy);

    List<ApprovalTemplateDocument> findByCreatedBy(Integer createdBy);

    List<ApprovalTemplateDocument> findByIsPublicTrue();
}

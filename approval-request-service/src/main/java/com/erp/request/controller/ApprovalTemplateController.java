package com.erp.request.controller;

import com.erp.request.document.ApprovalTemplateDocument;
import com.erp.request.dto.CreateTemplateRequest;
import com.erp.request.repository.ApprovalTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class ApprovalTemplateController {

    private final ApprovalTemplateRepository templateRepository;

    // POST /templates - 템플릿 생성
    @PostMapping
    public ResponseEntity<Map<String, String>> createTemplate(@RequestBody CreateTemplateRequest request) {
        ApprovalTemplateDocument template = ApprovalTemplateDocument.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(request.getCreatedBy())
                .isPublic(request.isPublic())
                .steps(request.getSteps().stream()
                        .map(s -> ApprovalTemplateDocument.TemplateStep.builder()
                                .step(s.getStep())
                                .approverId(s.getApproverId())
                                .approverName(s.getApproverName())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ApprovalTemplateDocument saved = templateRepository.save(template);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", "created"));
    }

    // GET /templates - 템플릿 목록 조회
    @GetMapping
    public ResponseEntity<List<ApprovalTemplateDocument>> getAllTemplates(
            @RequestParam(required = false) Integer userId) {
        List<ApprovalTemplateDocument> templates;
        if (userId != null) {
            // 사용자의 템플릿 + 공개 템플릿
            templates = templateRepository.findByCreatedByOrIsPublicTrue(userId);
        } else {
            // 공개 템플릿만
            templates = templateRepository.findByIsPublicTrue();
        }
        return ResponseEntity.ok(templates);
    }

    // GET /templates/{id} - 템플릿 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalTemplateDocument> getTemplate(@PathVariable String id) {
        return templateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /templates/{id} - 템플릿 수정
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateTemplate(
            @PathVariable String id,
            @RequestBody CreateTemplateRequest request) {
        return templateRepository.findById(id)
                .map(template -> {
                    template.setName(request.getName());
                    template.setDescription(request.getDescription());
                    template.setPublic(request.isPublic());
                    template.setSteps(request.getSteps().stream()
                            .map(s -> ApprovalTemplateDocument.TemplateStep.builder()
                                    .step(s.getStep())
                                    .approverId(s.getApproverId())
                                    .approverName(s.getApproverName())
                                    .build())
                            .collect(Collectors.toList()));
                    template.setUpdatedAt(LocalDateTime.now());
                    templateRepository.save(template);
                    return ResponseEntity.ok(Map.of("status", "updated"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /templates/{id} - 템플릿 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTemplate(@PathVariable String id) {
        if (templateRepository.existsById(id)) {
            templateRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "deleted"));
        }
        return ResponseEntity.notFound().build();
    }
}

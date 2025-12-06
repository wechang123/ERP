package com.erp.web.client;

import com.erp.web.dto.ApprovalTemplateDto;
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
public class TemplateApiClient {

    private final RestTemplate restTemplate;

    @Value("${services.approval-url}")
    private String baseUrl;

    public List<ApprovalTemplateDto> getTemplates(Integer userId) {
        try {
            String url = baseUrl + "/templates";
            if (userId != null) {
                url += "?userId=" + userId;
            }
            ResponseEntity<List<ApprovalTemplateDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ApprovalTemplateDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get templates: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ApprovalTemplateDto getTemplate(String id) {
        try {
            return restTemplate.getForObject(baseUrl + "/templates/" + id, ApprovalTemplateDto.class);
        } catch (Exception e) {
            log.error("Failed to get template {}: {}", id, e.getMessage());
            return null;
        }
    }

    public Map<String, String> createTemplate(ApprovalTemplateDto template) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.postForObject(
                    baseUrl + "/templates",
                    template,
                    Map.class
            );
            return response;
        } catch (Exception e) {
            log.error("Failed to create template: {}", e.getMessage());
            throw new RuntimeException("Failed to create template", e);
        }
    }

    public void updateTemplate(String id, ApprovalTemplateDto template) {
        try {
            restTemplate.put(baseUrl + "/templates/" + id, template);
        } catch (Exception e) {
            log.error("Failed to update template {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update template", e);
        }
    }

    public void deleteTemplate(String id) {
        try {
            restTemplate.delete(baseUrl + "/templates/" + id);
        } catch (Exception e) {
            log.error("Failed to delete template {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete template", e);
        }
    }
}

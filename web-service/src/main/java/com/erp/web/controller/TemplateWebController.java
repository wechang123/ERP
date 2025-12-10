package com.erp.web.controller;

import com.erp.web.client.EmployeeApiClient;
import com.erp.web.client.TemplateApiClient;
import com.erp.web.dto.ApprovalTemplateDto;
import com.erp.web.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateWebController {

    private final TemplateApiClient templateApiClient;
    private final EmployeeApiClient employeeApiClient;

    @GetMapping
    public String listTemplates(@RequestParam(required = false) Integer userId, Model model) {
        List<ApprovalTemplateDto> templates = templateApiClient.getTemplates(userId);
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();

        model.addAttribute("templates", templates);
        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "Templates");

        return "template/list";
    }

    @GetMapping("/new")
    public String newTemplateForm(Model model) {
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();
        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "New Template");
        return "template/form";
    }

    @PostMapping
    public String createTemplate(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer createdBy,
            @RequestParam(required = false, defaultValue = "false") boolean isPublic,
            @RequestParam(name = "approverIds") List<Integer> approverIds,
            @RequestParam(name = "approverNames") List<String> approverNames,
            RedirectAttributes redirectAttributes) {
        try {
            List<ApprovalTemplateDto.TemplateStepDto> steps = new ArrayList<>();
            for (int i = 0; i < approverIds.size(); i++) {
                steps.add(ApprovalTemplateDto.TemplateStepDto.builder()
                        .step(i + 1)
                        .approverId(approverIds.get(i))
                        .approverName(approverNames.get(i))
                        .build());
            }

            ApprovalTemplateDto template = ApprovalTemplateDto.builder()
                    .name(name)
                    .description(description)
                    .createdBy(createdBy)
                    .isPublic(isPublic)
                    .steps(steps)
                    .build();

            templateApiClient.createTemplate(template);
            redirectAttributes.addFlashAttribute("successMessage", "템플릿이 생성되었습니다");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "템플릿 생성 실패: " + e.getMessage());
        }
        return "redirect:/templates";
    }

    @GetMapping("/{id}/edit")
    public String editTemplateForm(@PathVariable String id, Model model) {
        ApprovalTemplateDto template = templateApiClient.getTemplate(id);
        if (template == null) {
            return "redirect:/templates";
        }

        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();
        model.addAttribute("template", template);
        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "Edit Template");
        return "template/edit";
    }

    @PostMapping("/{id}")
    public String updateTemplate(
            @PathVariable String id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer createdBy,
            @RequestParam(required = false, defaultValue = "false") boolean isPublic,
            @RequestParam(name = "approverIds") List<Integer> approverIds,
            @RequestParam(name = "approverNames") List<String> approverNames,
            RedirectAttributes redirectAttributes) {
        try {
            List<ApprovalTemplateDto.TemplateStepDto> steps = new ArrayList<>();
            for (int i = 0; i < approverIds.size(); i++) {
                steps.add(ApprovalTemplateDto.TemplateStepDto.builder()
                        .step(i + 1)
                        .approverId(approverIds.get(i))
                        .approverName(approverNames.get(i))
                        .build());
            }

            ApprovalTemplateDto template = ApprovalTemplateDto.builder()
                    .name(name)
                    .description(description)
                    .createdBy(createdBy)
                    .isPublic(isPublic)
                    .steps(steps)
                    .build();

            templateApiClient.updateTemplate(id, template);
            redirectAttributes.addFlashAttribute("successMessage", "템플릿이 수정되었습니다");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "템플릿 수정 실패: " + e.getMessage());
        }
        return "redirect:/templates";
    }

    @PostMapping("/{id}/delete")
    public String deleteTemplate(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            templateApiClient.deleteTemplate(id);
            redirectAttributes.addFlashAttribute("successMessage", "템플릿이 삭제되었습니다");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "템플릿 삭제 실패: " + e.getMessage());
        }
        return "redirect:/templates";
    }
}

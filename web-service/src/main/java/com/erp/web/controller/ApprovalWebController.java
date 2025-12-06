package com.erp.web.controller;

import com.erp.web.client.ApprovalApiClient;
import com.erp.web.client.EmployeeApiClient;
import com.erp.web.client.TemplateApiClient;
import com.erp.web.dto.ApprovalDto;
import com.erp.web.dto.ApprovalTemplateDto;
import com.erp.web.dto.CreateApprovalDto;
import com.erp.web.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalWebController {

    private final ApprovalApiClient approvalApiClient;
    private final EmployeeApiClient employeeApiClient;
    private final TemplateApiClient templateApiClient;

    @GetMapping
    public String listApprovals(@RequestParam(required = false) String status, Model model) {
        List<ApprovalDto> approvals = approvalApiClient.getAllApprovals();
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();

        // ID -> 이름 매핑 (Integer 키로 변환 - ApprovalDto.requesterId가 Integer이므로)
        Map<Integer, String> employeeNames = employees.stream()
                .collect(Collectors.toMap(
                        e -> e.getId().intValue(),
                        EmployeeDto::getName
                ));

        if (status != null && !status.isEmpty()) {
            approvals = approvals.stream()
                    .filter(a -> status.equals(a.getFinalStatus()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("approvals", approvals);
        model.addAttribute("employeeNames", employeeNames);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Approval Requests");
        return "approval/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();
        List<ApprovalTemplateDto> templates = templateApiClient.getTemplates(null);

        model.addAttribute("employees", employees);
        model.addAttribute("templates", templates);
        model.addAttribute("pageTitle", "New Approval Request");
        return "approval/form";
    }

    @PostMapping
    public String createApproval(
            @RequestParam Integer requesterId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(value = "deadline", required = false) String deadlineStr,
            @RequestParam(value = "approverIds", required = false) List<Integer> approverIds,
            RedirectAttributes redirectAttributes) {
        try {
            CreateApprovalDto request = new CreateApprovalDto();
            request.setRequesterId(requesterId);
            request.setTitle(title);
            request.setContent(content);

            // Parse deadline if provided
            if (deadlineStr != null && !deadlineStr.isEmpty()) {
                LocalDateTime deadline = LocalDateTime.parse(deadlineStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                request.setDeadline(deadline);
            }

            // Convert approverIds to steps
            List<CreateApprovalDto.StepRequest> steps = new ArrayList<>();
            if (approverIds != null) {
                for (int i = 0; i < approverIds.size(); i++) {
                    CreateApprovalDto.StepRequest step = new CreateApprovalDto.StepRequest();
                    step.setStep(i + 1);
                    step.setApproverId(approverIds.get(i));
                    steps.add(step);
                }
            }
            request.setSteps(steps);

            approvalApiClient.createApproval(request);
            redirectAttributes.addFlashAttribute("successMessage", "Approval request created successfully");
        } catch (Exception e) {
            log.error("Failed to create approval request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create approval request: " + e.getMessage());
        }
        return "redirect:/approvals";
    }

    @GetMapping("/{requestId}")
    public String showApproval(@PathVariable Integer requestId, Model model) {
        ApprovalDto approval = approvalApiClient.getApproval(requestId);
        if (approval == null) {
            return "redirect:/approvals";
        }

        // Get employee names for display (Integer 키로 변환)
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();
        Map<Integer, String> employeeNames = employees.stream()
                .collect(Collectors.toMap(
                        e -> e.getId().intValue(),
                        EmployeeDto::getName
                ));

        model.addAttribute("approval", approval);
        model.addAttribute("employees", employees);
        model.addAttribute("employeeNames", employeeNames);
        model.addAttribute("pageTitle", "Approval Request #" + requestId);
        return "approval/detail";
    }

    // 결재 요청 수정 폼
    @GetMapping("/{requestId}/edit")
    public String showEditForm(@PathVariable Integer requestId, Model model) {
        ApprovalDto approval = approvalApiClient.getApproval(requestId);
        if (approval == null) {
            return "redirect:/approvals";
        }

        // 수정 가능 여부 확인 (모든 단계가 pending이어야 함)
        boolean canEdit = approval.getSteps().stream()
                .allMatch(s -> "pending".equals(s.getStatus()));
        if (!canEdit) {
            return "redirect:/approvals/" + requestId;
        }

        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();

        model.addAttribute("approval", approval);
        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "Edit Approval Request #" + requestId);
        return "approval/edit";
    }

    // 결재 요청 수정 처리
    @PostMapping("/{requestId}/edit")
    public String updateApproval(
            @PathVariable Integer requestId,
            @RequestParam Integer editedBy,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(value = "approverIds", required = false) List<Integer> approverIds,
            RedirectAttributes redirectAttributes) {
        try {
            CreateApprovalDto request = new CreateApprovalDto();
            request.setTitle(title);
            request.setContent(content);

            // Convert approverIds to steps
            List<CreateApprovalDto.StepRequest> steps = new ArrayList<>();
            if (approverIds != null) {
                for (int i = 0; i < approverIds.size(); i++) {
                    CreateApprovalDto.StepRequest step = new CreateApprovalDto.StepRequest();
                    step.setStep(i + 1);
                    step.setApproverId(approverIds.get(i));
                    steps.add(step);
                }
            }
            request.setSteps(steps);

            approvalApiClient.updateApproval(requestId, request, editedBy);
            redirectAttributes.addFlashAttribute("successMessage", "결재 요청이 수정되었습니다");
        } catch (Exception e) {
            log.error("Failed to update approval request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "수정 실패: " + e.getMessage());
        }
        return "redirect:/approvals/" + requestId;
    }

    // 결재 요청 취소
    @PostMapping("/{requestId}/cancel")
    public String cancelApproval(
            @PathVariable Integer requestId,
            @RequestParam Integer requesterId,
            RedirectAttributes redirectAttributes) {
        try {
            approvalApiClient.cancelApproval(requestId, requesterId);
            redirectAttributes.addFlashAttribute("successMessage", "결재 요청이 취소되었습니다");
        } catch (Exception e) {
            log.error("Failed to cancel approval request", e);
            redirectAttributes.addFlashAttribute("errorMessage", "취소 실패: " + e.getMessage());
        }
        return "redirect:/approvals";
    }

    // 승인 철회
    @PostMapping("/{requestId}/steps/{step}/withdraw")
    public String withdrawApproval(
            @PathVariable Integer requestId,
            @PathVariable Integer step,
            @RequestParam Integer approverId,
            RedirectAttributes redirectAttributes) {
        try {
            approvalApiClient.withdrawApproval(requestId, step, approverId);
            redirectAttributes.addFlashAttribute("successMessage", "승인이 철회되었습니다");
        } catch (Exception e) {
            log.error("Failed to withdraw approval", e);
            redirectAttributes.addFlashAttribute("errorMessage", "철회 실패: " + e.getMessage());
        }
        return "redirect:/approvals/" + requestId;
    }
}

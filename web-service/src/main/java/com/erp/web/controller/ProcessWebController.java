package com.erp.web.controller;

import com.erp.web.client.EmployeeApiClient;
import com.erp.web.client.ProcessApiClient;
import com.erp.web.dto.ApprovalItemDto;
import com.erp.web.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/process")
@RequiredArgsConstructor
@Slf4j
public class ProcessWebController {

    private final ProcessApiClient processApiClient;
    private final EmployeeApiClient employeeApiClient;

    @GetMapping("/select")
    public String selectApprover(Model model) {
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();

        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "Select Approver");
        return "process/select";
    }

    @GetMapping("/{approverId}")
    public String showQueue(@PathVariable Integer approverId, Model model) {
        List<ApprovalItemDto> pendingItems = processApiClient.getPendingApprovals(approverId);
        EmployeeDto approver = employeeApiClient.getEmployee(approverId.longValue());
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();

        // ID -> 이름 매핑 (Integer 키로 변환)
        Map<Integer, String> employeeNames = employees.stream()
                .collect(Collectors.toMap(
                        e -> e.getId().intValue(),
                        EmployeeDto::getName
                ));

        model.addAttribute("pendingItems", pendingItems);
        model.addAttribute("approverId", approverId);
        model.addAttribute("approver", approver);
        model.addAttribute("employeeNames", employeeNames);
        model.addAttribute("pageTitle", "Approval Queue");
        return "process/queue";
    }

    @PostMapping("/{approverId}/{requestId}/approve")
    public String approveRequest(@PathVariable Integer approverId,
                                  @PathVariable Integer requestId,
                                  RedirectAttributes redirectAttributes) {
        try {
            processApiClient.processApproval(approverId, requestId, "approved");
            redirectAttributes.addFlashAttribute("successMessage",
                    "요청 #" + requestId + " 승인 완료");
        } catch (Exception e) {
            log.error("Failed to approve request", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "승인 실패: " + e.getMessage());
        }
        return "redirect:/process/" + approverId;
    }

    @PostMapping("/{approverId}/{requestId}/reject")
    public String rejectRequest(@PathVariable Integer approverId,
                                 @PathVariable Integer requestId,
                                 RedirectAttributes redirectAttributes) {
        try {
            processApiClient.processApproval(approverId, requestId, "rejected");
            redirectAttributes.addFlashAttribute("successMessage",
                    "요청 #" + requestId + " 반려 완료");
        } catch (Exception e) {
            log.error("Failed to reject request", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "반려 실패: " + e.getMessage());
        }
        return "redirect:/process/" + approverId;
    }
}

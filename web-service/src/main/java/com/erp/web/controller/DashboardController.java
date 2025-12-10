package com.erp.web.controller;

import com.erp.web.client.ApprovalApiClient;
import com.erp.web.client.EmployeeApiClient;
import com.erp.web.dto.ApprovalDto;
import com.erp.web.dto.ApprovalStatisticsDto;
import com.erp.web.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final EmployeeApiClient employeeApiClient;
    private final ApprovalApiClient approvalApiClient;

    @GetMapping("/")
    public String dashboard(Model model) {
        // 통계 조회
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();
        List<ApprovalDto> approvals = approvalApiClient.getAllApprovals();

        int employeeCount = employees != null ? employees.size() : 0;
        int totalApprovals = approvals != null ? approvals.size() : 0;

        int inProgressCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;

        if (approvals != null) {
            inProgressCount = (int) approvals.stream()
                    .filter(a -> "in_progress".equals(a.getFinalStatus()))
                    .count();
            approvedCount = (int) approvals.stream()
                    .filter(a -> "approved".equals(a.getFinalStatus()))
                    .count();
            rejectedCount = (int) approvals.stream()
                    .filter(a -> "rejected".equals(a.getFinalStatus()))
                    .count();
        }

        // 최근 결재 요청 (최대 5개)
        List<ApprovalDto> recentApprovals = approvals != null ?
                approvals.stream()
                        .sorted((a, b) -> {
                            if (a.getCreatedAt() == null) return 1;
                            if (b.getCreatedAt() == null) return -1;
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        })
                        .limit(5)
                        .collect(Collectors.toList()) :
                List.of();

        model.addAttribute("employeeCount", employeeCount);
        model.addAttribute("totalApprovals", totalApprovals);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("recentApprovals", recentApprovals);
        model.addAttribute("pageTitle", "Dashboard");

        return "index";
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {
        // API에서 통계 조회
        ApprovalStatisticsDto statistics = approvalApiClient.getStatistics();

        // 표시용 직원 이름 조회
        List<EmployeeDto> employees = employeeApiClient.getAllEmployees();

        model.addAttribute("statistics", statistics);
        model.addAttribute("employees", employees);
        model.addAttribute("pageTitle", "Statistics");

        return "statistics";
    }
}

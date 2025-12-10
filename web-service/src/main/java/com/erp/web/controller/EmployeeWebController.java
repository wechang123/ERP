package com.erp.web.controller;

import com.erp.web.client.EmployeeApiClient;
import com.erp.web.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeWebController {

    private final EmployeeApiClient employeeApiClient;

    @GetMapping
    public String listEmployees(@RequestParam(required = false) String department, Model model) {
        List<EmployeeDto> employees;
        if (department != null && !department.isEmpty()) {
            employees = employeeApiClient.getEmployeesByDepartment(department);
        } else {
            employees = employeeApiClient.getAllEmployees();
        }

        model.addAttribute("employees", employees);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("pageTitle", "Employee Management");
        return "employee/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("employee", new EmployeeDto());
        model.addAttribute("pageTitle", "New Employee");
        model.addAttribute("isEdit", false);
        return "employee/form";
    }

    @PostMapping
    public String createEmployee(@ModelAttribute EmployeeDto employee, RedirectAttributes redirectAttributes) {
        try {
            employeeApiClient.createEmployee(employee);
            redirectAttributes.addFlashAttribute("successMessage", "직원이 등록되었습니다");
        } catch (Exception e) {
            log.error("직원 등록 실패", e);
            redirectAttributes.addFlashAttribute("errorMessage", "직원 등록 실패: " + e.getMessage());
        }
        return "redirect:/employees";
    }

    @GetMapping("/{id}")
    public String showEmployee(@PathVariable Long id, Model model) {
        EmployeeDto employee = employeeApiClient.getEmployee(id);
        if (employee == null) {
            return "redirect:/employees";
        }

        model.addAttribute("employee", employee);
        model.addAttribute("pageTitle", "Employee Details");
        return "employee/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        EmployeeDto employee = employeeApiClient.getEmployee(id);
        if (employee == null) {
            return "redirect:/employees";
        }

        model.addAttribute("employee", employee);
        model.addAttribute("pageTitle", "Edit Employee");
        model.addAttribute("isEdit", true);
        return "employee/form";
    }

    @PostMapping("/{id}")
    public String updateEmployee(@PathVariable Long id, @ModelAttribute EmployeeDto employee,
                                  RedirectAttributes redirectAttributes) {
        try {
            employeeApiClient.updateEmployee(id, employee);
            redirectAttributes.addFlashAttribute("successMessage", "직원이 수정되었습니다");
        } catch (Exception e) {
            log.error("직원 수정 실패", e);
            redirectAttributes.addFlashAttribute("errorMessage", "직원 수정 실패: " + e.getMessage());
        }
        return "redirect:/employees";
    }

    @PostMapping("/{id}/delete")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeApiClient.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("successMessage", "직원이 삭제되었습니다");
        } catch (Exception e) {
            log.error("직원 삭제 실패", e);
            redirectAttributes.addFlashAttribute("errorMessage", "직원 삭제 실패: " + e.getMessage());
        }
        return "redirect:/employees";
    }
}

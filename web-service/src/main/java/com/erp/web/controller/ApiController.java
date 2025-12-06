package com.erp.web.controller;

import com.erp.web.client.EmployeeApiClient;
import com.erp.web.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final EmployeeApiClient employeeApiClient;

    @GetMapping("/employees")
    public List<EmployeeDto> getEmployees() {
        return employeeApiClient.getAllEmployees();
    }

    @GetMapping("/employees/{id}")
    public EmployeeDto getEmployee(@PathVariable Long id) {
        return employeeApiClient.getEmployee(id);
    }
}

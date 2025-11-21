package com.erp.employee.dto;

import com.erp.employee.entity.Employee;
import lombok.Getter;

@Getter
public class EmployeeResponse {
    private Long id;
    private String name;
    private String department;
    private String position;

    public EmployeeResponse(Employee employee) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.department = employee.getDepartment();
        this.position = employee.getPosition();
    }
}

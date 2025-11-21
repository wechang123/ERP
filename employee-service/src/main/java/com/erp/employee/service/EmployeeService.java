package com.erp.employee.service;

import com.erp.employee.dto.EmployeeRequest;
import com.erp.employee.dto.EmployeeResponse;
import com.erp.employee.dto.EmployeeUpdateRequest;
import com.erp.employee.entity.Employee;
import com.erp.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public Long createEmployee(EmployeeRequest request) {
        Employee employee = new Employee(
                request.getName(),
                request.getDepartment(),
                request.getPosition()
        );
        Employee saved = employeeRepository.save(employee);
        return saved.getId();
    }

    public List<EmployeeResponse> getAllEmployees(String department, String position) {
        List<Employee> employees;

        if (department != null && position != null) {
            employees = employeeRepository.findByDepartmentAndPosition(department, position);
        } else if (department != null) {
            employees = employeeRepository.findByDepartment(department);
        } else if (position != null) {
            employees = employeeRepository.findByPosition(position);
        } else {
            employees = employeeRepository.findAll();
        }

        return employees.stream()
                .map(EmployeeResponse::new)
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        return new EmployeeResponse(employee);
    }

    @Transactional
    public void updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment());
        }
        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }
    }

    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return employeeRepository.existsById(id);
    }
}

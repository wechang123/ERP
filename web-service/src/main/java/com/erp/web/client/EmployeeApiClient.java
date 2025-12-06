package com.erp.web.client;

import com.erp.web.dto.EmployeeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeApiClient {

    private final RestTemplate restTemplate;

    @Value("${services.employee-url}")
    private String baseUrl;

    public List<EmployeeDto> getAllEmployees() {
        try {
            ResponseEntity<List<EmployeeDto>> response = restTemplate.exchange(
                    baseUrl + "/employees",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EmployeeDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get employees: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<EmployeeDto> getEmployeesByDepartment(String department) {
        try {
            ResponseEntity<List<EmployeeDto>> response = restTemplate.exchange(
                    baseUrl + "/employees?department=" + department,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<EmployeeDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get employees by department: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public EmployeeDto getEmployee(Long id) {
        try {
            return restTemplate.getForObject(baseUrl + "/employees/" + id, EmployeeDto.class);
        } catch (Exception e) {
            log.error("Failed to get employee {}: {}", id, e.getMessage());
            return null;
        }
    }

    public EmployeeDto createEmployee(EmployeeDto employee) {
        try {
            return restTemplate.postForObject(baseUrl + "/employees", employee, EmployeeDto.class);
        } catch (Exception e) {
            log.error("Failed to create employee: {}", e.getMessage());
            throw new RuntimeException("Failed to create employee", e);
        }
    }

    public EmployeeDto updateEmployee(Long id, EmployeeDto employee) {
        try {
            HttpEntity<EmployeeDto> request = new HttpEntity<>(employee);
            ResponseEntity<EmployeeDto> response = restTemplate.exchange(
                    baseUrl + "/employees/" + id,
                    HttpMethod.PUT,
                    request,
                    EmployeeDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to update employee {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to update employee", e);
        }
    }

    public void deleteEmployee(Long id) {
        try {
            restTemplate.delete(baseUrl + "/employees/" + id);
        } catch (Exception e) {
            log.error("Failed to delete employee {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete employee", e);
        }
    }

    public int getEmployeeCount() {
        List<EmployeeDto> employees = getAllEmployees();
        return employees != null ? employees.size() : 0;
    }
}

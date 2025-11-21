package com.erp.employee.controller;

import com.erp.employee.dto.EmployeeRequest;
import com.erp.employee.dto.EmployeeResponse;
import com.erp.employee.dto.EmployeeUpdateRequest;
import com.erp.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // POST /employees - 직원 생성
    @PostMapping
    public ResponseEntity<Map<String, Long>> createEmployee(@RequestBody EmployeeRequest request) {
        Long id = employeeService.createEmployee(request);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // GET /employees - 직원 목록 조회 (필터링 지원)
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position) {
        List<EmployeeResponse> employees = employeeService.getAllEmployees(department, position);
        return ResponseEntity.ok(employees);
    }

    // GET /employees/{id} - 직원 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        EmployeeResponse employee = employeeService.getEmployee(id);
        return ResponseEntity.ok(employee);
    }

    // PUT /employees/{id} - 직원 수정 (department, position만 수정 가능)
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEmployee(
            @PathVariable Long id,
            @RequestBody EmployeeUpdateRequest request) {
        employeeService.updateEmployee(id, request);
        return ResponseEntity.ok().build();
    }

    // DELETE /employees/{id} - 직원 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok().build();
    }

    // GET /employees/{id}/exists - 직원 존재 여부 확인 (다른 서비스에서 호출용)
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> existsEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.existsById(id));
    }
}

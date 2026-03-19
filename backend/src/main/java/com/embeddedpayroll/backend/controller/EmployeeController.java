package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.dto.EmployeeDtos;
import com.embeddedpayroll.backend.service.PayrollService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final PayrollService payrollService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER')")
    public List<EmployeeDtos.EmployeeResponse> employees(@RequestParam Long organizationId) {
        int currentTaxYear = LocalDate.now().getYear();
        return payrollService.listEmployees(organizationId).stream()
            .map(employee -> EmployeeDtos.fromEntity(
                employee,
                payrollService.findCurrentW4(employee.getId(), currentTaxYear)
            ))
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public EmployeeDtos.EmployeeResponse createEmployee(@Valid @RequestBody EmployeeDtos.CreateEmployeeRequest request) {
        var employee = payrollService.createEmployee(request);
        return EmployeeDtos.fromEntity(employee, payrollService.findCurrentW4(employee.getId(), request.w4Profile().taxYear()));
    }

    @PutMapping("/{employeeId}/w4")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public EmployeeDtos.W4Response upsertW4(
        @PathVariable Long employeeId,
        @Valid @RequestBody EmployeeDtos.W4Request request
    ) {
        return EmployeeDtos.fromEntity(payrollService.upsertW4(employeeId, request));
    }
}

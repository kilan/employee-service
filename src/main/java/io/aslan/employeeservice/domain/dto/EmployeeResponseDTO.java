package io.aslan.employeeservice.domain.dto;

import java.math.BigDecimal;

public record EmployeeResponseDTO(Long id,
                                  String firstName,
                                  String lastName,
                                  String email,
                                  String payrollId,
                                  BigDecimal annualSalary,
                                  BigDecimal salaryAllowancePercentage) {

}

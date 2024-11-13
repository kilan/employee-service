package io.aslan.employeeservice.util;

import io.aslan.employeeservice.domain.dto.EmployeeRequestDTO;
import io.aslan.employeeservice.domain.dto.EmployeeResponseDTO;
import io.aslan.employeeservice.domain.entity.Employee;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmployeeMapper {

    public EmployeeResponseDTO map(Employee employee) {
        return new EmployeeResponseDTO(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPayrollId(),
                employee.getAnnualSalary(),
                employee.getSalaryAllowancePercentage());
    }

    public Employee map(EmployeeRequestDTO employeeRequestDTO) {
        Employee employee = new Employee();
        employee.setFirstName(employeeRequestDTO.firstName());
        employee.setLastName(employeeRequestDTO.lastName());
        employee.setPayrollId(employeeRequestDTO.payrollId());
        employee.setAnnualSalary(employeeRequestDTO.annualSalary());
        employee.setLastSalaryChangedDate(LocalDateTime.now());
        employee.setSalaryAllowancePercentage(employeeRequestDTO.salaryAllowancePercentage());
        employee.setEmail(employeeRequestDTO.email());
        return employee;
    }
}

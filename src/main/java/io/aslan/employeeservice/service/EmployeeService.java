package io.aslan.employeeservice.service;

import io.aslan.employeeservice.domain.dto.EmployeeRequestDTO;
import io.aslan.employeeservice.domain.dto.EmployeeResponseDTO;
import io.aslan.employeeservice.domain.entity.Employee;
import io.aslan.employeeservice.exception.EmployeeNotFoundException;
import io.aslan.employeeservice.repository.EmployeeRepository;
import io.aslan.employeeservice.util.AllowanceCalculator;
import io.aslan.employeeservice.util.EmployeeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final NotificationService notificationService;
    private final AllowanceCalculator allowanceCalculator;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeMapper employeeMapper,
                           NotificationService notificationService,
                           AllowanceCalculator allowanceCalculator) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
        this.notificationService = notificationService;
        this.allowanceCalculator = allowanceCalculator;
    }

    public List<EmployeeResponseDTO> getEmployees(Pageable pageable) {
        Page<Employee> employees = employeeRepository.findAll(pageable);
        return employees.stream()
                .map(employeeMapper::map)
                .toList();
    }

    public EmployeeResponseDTO getEmployee(Long employeeId) {
        Employee employee = findEmployeeById(employeeId);
        return employeeMapper.map(employee);
    }

    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO request) {
        Employee employee = employeeMapper.map(request);
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Saved Employee={}", savedEmployee);
        return employeeMapper.map(savedEmployee);
    }

    public EmployeeResponseDTO updateEmployee(Long employeeId, EmployeeRequestDTO request) {
        Employee employee = findEmployeeById(employeeId);

        log.info("About to update Employee={}", employee);

        BigDecimal currentMonthlyAllowance = allowanceCalculator.getMonthlyAllowance(employee.getAnnualSalary(), employee.getSalaryAllowancePercentage());
        BigDecimal newMonthlyAllowance = allowanceCalculator.getMonthlyAllowance(request.annualSalary(), request.salaryAllowancePercentage());

        if (!request.annualSalary().equals(employee.getAnnualSalary())) {
            employee.setAnnualSalary(request.annualSalary());
            employee.setLastSalaryChangedDate(LocalDateTime.now());
        }

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setPayrollId(request.payrollId());
        employee.setSalaryAllowancePercentage(request.salaryAllowancePercentage());
        employee.setEmail(request.email());

        employeeRepository.save(employee);
        log.info("Updated Employee={}", employee);

        if (!currentMonthlyAllowance.equals(newMonthlyAllowance)) {
            notificationService.sendAllowanceUpdateMessage(employee, currentMonthlyAllowance, newMonthlyAllowance);
        }

        return employeeMapper.map(employee);
    }

    public void deleteEmployee(Long employeeId) {
        employeeRepository.deleteById(employeeId);
        log.info("Deleted employee with id={}", employeeId);
    }

    private Employee findEmployeeById(Long employeeId) {
        return employeeRepository.findOneById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Could not find Employee with id=" + employeeId));
    }
}

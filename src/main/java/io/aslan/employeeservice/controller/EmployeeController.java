package io.aslan.employeeservice.controller;

import io.aslan.employeeservice.domain.dto.EmployeeRequestDTO;
import io.aslan.employeeservice.domain.dto.EmployeeResponseDTO;
import io.aslan.employeeservice.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeResponseDTO> getEmployees(Pageable pageable) {
        return employeeService.getEmployees(pageable);
    }

    @GetMapping("/{employeeId}")
    public EmployeeResponseDTO getEmployee(@PathVariable Long employeeId) {
        return employeeService.getEmployee(employeeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponseDTO createEmployee(@RequestBody EmployeeRequestDTO request) {
        log.info("Received request to create EmployeeRequestDTO={}", request);
        return employeeService.createEmployee(request);
    }

    @PutMapping("/{employeeId}")
    public EmployeeResponseDTO updateEmployee(@PathVariable Long employeeId,
                                              @RequestBody EmployeeRequestDTO request) {
        log.info("Received request to create to update employeeId={} EmployeeRequestDTO={}", employeeId, request);
        return employeeService.updateEmployee(employeeId, request);
    }

    @DeleteMapping("/{employeeId}")
    public void updateEmployee(@PathVariable Long employeeId) {
        log.info("Received request to create to delete employeeId={}", employeeId);
        employeeService.deleteEmployee(employeeId);
    }

}

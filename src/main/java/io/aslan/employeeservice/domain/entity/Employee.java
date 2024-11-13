package io.aslan.employeeservice.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Entity
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String payrollId;

    @Column(nullable = false)
    private BigDecimal annualSalary;

    private LocalDateTime lastSalaryChangedDate;

    @Column(nullable = false)
    private BigDecimal salaryAllowancePercentage;

    @Column(unique = true, nullable = false)
    private String email;


}

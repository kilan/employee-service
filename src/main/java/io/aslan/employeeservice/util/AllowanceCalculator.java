package io.aslan.employeeservice.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AllowanceCalculator {

    public BigDecimal getMonthlyAllowance(BigDecimal annualSalary, BigDecimal salaryAllowancePercentage) {
        return annualSalary.multiply(salaryAllowancePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }
}

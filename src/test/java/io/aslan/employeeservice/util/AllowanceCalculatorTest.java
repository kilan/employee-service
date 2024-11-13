package io.aslan.employeeservice.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;


class AllowanceCalculatorTest {

    AllowanceCalculator underTest = new AllowanceCalculator();

    @Test
    void shouldCalculateMonthlyAllowance() {
        BigDecimal annualSalary = new BigDecimal(12000);
        BigDecimal allowancePercentage = new BigDecimal(30);

        BigDecimal expectedMonthlyAllowance = new BigDecimal(300);
        BigDecimal actual = underTest.getMonthlyAllowance(annualSalary, allowancePercentage);

        Assertions.assertThat(actual).isEqualByComparingTo(expectedMonthlyAllowance);
    }
}
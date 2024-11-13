package io.aslan.employeeservice.domain.message;

import java.math.BigDecimal;

public record AllowanceUpdateMessage(
        Long id,
        String firstName,
        String lastName,
        BigDecimal currentMonthlyAllowance,
        BigDecimal newMonthlyAllowance,
        String email) {
}

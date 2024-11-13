package io.aslan.employeeservice.service;

import io.aslan.employeeservice.domain.entity.Employee;
import io.aslan.employeeservice.domain.message.AllowanceUpdateMessage;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final String notificationServiceQueueUrl;
    private final SqsTemplate sqsTemplate;

    public NotificationService(@Value("${notification.service.queue.url}") String notificationServiceQueueUrl,
                               SqsTemplate sqsTemplate) {
        this.notificationServiceQueueUrl = notificationServiceQueueUrl;
        this.sqsTemplate = sqsTemplate;
    }

    public void sendAllowanceUpdateMessage(Employee employee,
                                           BigDecimal currentMonthlyAllowance,
                                           BigDecimal newMonthlyAllowance) {
        AllowanceUpdateMessage payload = new AllowanceUpdateMessage(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                currentMonthlyAllowance,
                newMonthlyAllowance,
                employee.getEmail());

        sqsTemplate.send(notificationServiceQueueUrl, payload);
        log.info("Sent allowance update message={}", payload);
    }

}

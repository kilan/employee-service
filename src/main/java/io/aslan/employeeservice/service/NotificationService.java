package io.aslan.employeeservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aslan.employeeservice.domain.entity.Employee;
import io.aslan.employeeservice.domain.message.AllowanceUpdateMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final String notificationServiceQueueUrl;
    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    public NotificationService(@Value("${notification.service.queue.url}") String notificationServiceQueueUrl,
                               SqsTemplate sqsTemplate,
                               ObjectMapper objectMapper) {
        this.notificationServiceQueueUrl = notificationServiceQueueUrl;
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
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

        sqsTemplate.send(notificationServiceQueueUrl, MessageBuilder.withPayload(toJson(payload))
                .setHeader("contentType", "application/json")
                .build());

        log.info("Sent allowance update message={}", payload);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

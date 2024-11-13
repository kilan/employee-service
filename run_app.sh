#!/bin/bash

export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/testdb
export SPRING_DATASOURCE_USERNAME=testuser
export SPRING_DATASOURCE_PASSWORD=testpassword
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
export SPRING_CLOUD_AWS_REGION_STATIC=us-east-1
export SPRING_CLOUD_AWS_CREDENTIALS_ACCESS_KEY=test
export SPRING_CLOUD_AWS_CREDENTIALS_SECRET_KEY=test
export SPRING_CLOUD_AWS_SQS_ENDPOINT=http://localhost:4566
export NOTIFICATION_SERVICE_QUEUE_URL=http://localhost:4566/000000000000/notification-service-queue

mvn spring-boot:run

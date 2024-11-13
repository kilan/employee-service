#!/bin/bash

export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"
export AWS_DEFAULT_REGION="us-east-1"

docker run -d --name localstack \
  -p 4566:4566 \
  -e SERVICES=sqs,ses \
  localstack/localstack:3.5.0

docker run -d --name postgres \
  -e POSTGRES_USER=testuser \
  -e POSTGRES_PASSWORD=testpassword \
  -e POSTGRES_DB=testdb \
  -p 5432:5432 \
  postgres:latest

AWS_ENDPOINT="--endpoint-url=http://localhost:4566 --region us-east-1"

aws $AWS_ENDPOINT sqs create-queue --queue-name notification-service-queue

aws $AWS_ENDPOINT ses verify-email-identity --email-address notifications@email.com

NOTIFICATION_QUEUE_URL=$(aws $AWS_ENDPOINT sqs get-queue-url --queue-name notification-service-queue --query QueueUrl --output text)
echo $NOTIFICATION_QUEUE_URL


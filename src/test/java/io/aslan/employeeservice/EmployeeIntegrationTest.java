package io.aslan.employeeservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aslan.employeeservice.repository.EmployeeRepository;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.config.JsonPathConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EmployeeIntegrationTest {

    private static SqsClient sqsClient;
    private static String notificationServiceQueueUrl;

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withReuse(true);

    @Container
    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.5.0"))
            .withReuse(true);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.cloud.aws.region.static", () -> localStackContainer.getRegion());
        registry.add("spring.cloud.aws.credentials.access-key", () -> localStackContainer.getAccessKey());
        registry.add("spring.cloud.aws.credentials.secret-key", () -> localStackContainer.getSecretKey());
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localStackContainer.getEndpointOverride(SQS)
                .toString());

        sqsClient = SqsClient.builder()
                .region(Region.of(localStackContainer.getRegion()))
                .endpointOverride(localStackContainer.getEndpointOverride(SQS))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())
                ))
                .build();

        CreateQueueResponse createQueueResponse = sqsClient.createQueue(CreateQueueRequest.builder()
                .queueName("notification-service-queue")
                .build());
        notificationServiceQueueUrl = createQueueResponse.queueUrl();
        registry.add("notification.service.queue.url", () -> notificationServiceQueueUrl);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.config = RestAssured.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE));

        employeeRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    public void testGetEmployeeByIdReturnsEmployee() {
        var request = Map.of(
                "firstName", "David",
                "lastName", "Kilan",
                "email", "david.kilan@example.com",
                "payrollId", "PAY123",
                "annualSalary", BigDecimal.valueOf(12000),
                "salaryAllowancePercentage", BigDecimal.valueOf(30)
        );

        var response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/api/v1/employee")
                .then()
                .extract()
                .jsonPath();

        var employeeId = response.getInt("id");

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/employee/" + employeeId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(employeeId))
                .body("firstName", equalTo("David"))
                .body("lastName", equalTo("Kilan"))
                .body("email", equalTo("david.kilan@example.com"))
                .body("payrollId", equalTo("PAY123"))
                .body("annualSalary", equalTo(12000d))
                .body("salaryAllowancePercentage", equalTo(30d));
    }

    @Test
    public void testGetEmployeesShouldReturnEmployees() {
        var createEmployeeRequest1 = Map.of(
                "firstName", "David",
                "lastName", "Kilan",
                "email", "david.kilan@example.com",
                "payrollId", "PAY123",
                "annualSalary", 12000,
                "salaryAllowancePercentage", 30
        );
        var createEmployeeRequest2 = Map.of(
                "firstName", "David2",
                "lastName", "Kilan2",
                "email", "david.kilan2@example.com",
                "payrollId", "PAY124",
                "annualSalary", 12000,
                "salaryAllowancePercentage", 30
        );

        given().contentType(ContentType.JSON)
                .body(createEmployeeRequest1)
                .post("/api/v1/employee");

        given().contentType(ContentType.JSON)
                .body(createEmployeeRequest2)
                .post("/api/v1/employee");

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/employee")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$.size()", equalTo(2))
                .body("[0].firstName", equalTo("David"))
                .body("[0].lastName", equalTo("Kilan"))
                .body("[0].email", equalTo("david.kilan@example.com"))
                .body("[0].payrollId", equalTo("PAY123"))
                .body("[0].annualSalary", equalTo(12000d))
                .body("[0].salaryAllowancePercentage", equalTo(30d))
                .body("[1].firstName", equalTo("David2"))
                .body("[1].lastName", equalTo("Kilan2"))
                .body("[1].email", equalTo("david.kilan2@example.com"))
                .body("[1].payrollId", equalTo("PAY124"))
                .body("[1].annualSalary", equalTo(12000d))
                .body("[1].salaryAllowancePercentage", equalTo(30d));
    }

    @Test
    void testCreateEmployeeShouldPersistToDatabase() {
        var createEmployeeRequest = Map.of(
                "firstName", "David",
                "lastName", "Kilan",
                "email", "david.kilan@example.com",
                "payrollId", "PAY123",
                "annualSalary", BigDecimal.valueOf(12000),
                "salaryAllowancePercentage", BigDecimal.valueOf(30)
        );

        var response = given()
                .contentType(ContentType.JSON)
                .body(createEmployeeRequest)
                .post("/api/v1/employee")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("firstName", equalTo("David"))
                .body("lastName", equalTo("Kilan"))
                .body("email", equalTo("david.kilan@example.com"))
                .body("payrollId", equalTo("PAY123"))
                .body("annualSalary", equalTo(12000))
                .body("salaryAllowancePercentage", equalTo(30))
                .extract()
                .jsonPath();

        Long employeeId = response.getLong("id");

        String sql = "SELECT * FROM employee WHERE id = ?";
        var dbResult = jdbcTemplate.queryForMap(sql, employeeId);

        assertThat(dbResult)
                .hasSize(8)
                .containsKey("id")
                .containsEntry("first_name", "David")
                .containsEntry("last_name", "Kilan")
                .containsEntry("email", "david.kilan@example.com")
                .containsEntry("payroll_id", "PAY123")
                .hasEntrySatisfying("annual_salary", (actual) ->
                        assertThat((BigDecimal) actual).isEqualByComparingTo(BigDecimal.valueOf(12000)))
                .hasEntrySatisfying("salary_allowance_percentage", (actual) ->
                        assertThat((BigDecimal) actual).isEqualByComparingTo(BigDecimal.valueOf(30)))
                .hasEntrySatisfying("last_salary_changed_date", (actual) -> {
                    Timestamp actualTimestamp = (Timestamp) actual;
                    LocalDateTime actualDateTime = actualTimestamp.toLocalDateTime();
                    assertThat(actualDateTime)
                            .isBefore(LocalDateTime.now())
                            .isAfter(LocalDateTime.now().minusSeconds(5));
                });
    }

    @Test
    void testUpdateEmployeeShouldSendAllowanceMessageAndUpdateInDatabase() {
        var createEmployeeRequest = Map.of(
                "firstName", "David",
                "lastName", "Kilan",
                "email", "david.kilan@example.com",
                "payrollId", "PAY123",
                "annualSalary", BigDecimal.valueOf(12000),
                "salaryAllowancePercentage", BigDecimal.valueOf(30)
        );

        var response = given()
                .contentType(ContentType.JSON)
                .body(createEmployeeRequest)
                .post("/api/v1/employee")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .jsonPath();

        Long employeeId = response.getLong("id");

        var updateEmployeeRequest = Map.of(
                "id", employeeId,
                "firstName", "David_updated",
                "lastName", "Kilan_updated",
                "email", "david.kilan.updated@example.com",
                "payrollId", "PAY123_updated",
                "annualSalary", BigDecimal.valueOf(18000),
                "salaryAllowancePercentage", BigDecimal.valueOf(40)
        );

        given()
                .contentType(ContentType.JSON)
                .body(updateEmployeeRequest)
                .put("/api/v1/employee/" + employeeId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("firstName", equalTo("David_updated"))
                .body("lastName", equalTo("Kilan_updated"))
                .body("email", equalTo("david.kilan.updated@example.com"))
                .body("payrollId", equalTo("PAY123_updated"))
                .body("annualSalary", equalTo(18000))
                .body("salaryAllowancePercentage", equalTo(40))
                .extract()
                .jsonPath();

        String sql = "SELECT * FROM employee WHERE id = ?";
        var dbResult = jdbcTemplate.queryForMap(sql, employeeId);

        assertThat(dbResult)
                .hasSize(8)
                .containsKey("id")
                .containsEntry("first_name", "David_updated")
                .containsEntry("last_name", "Kilan_updated")
                .containsEntry("email", "david.kilan.updated@example.com")
                .containsEntry("payroll_id", "PAY123_updated")
                .hasEntrySatisfying("annual_salary", (actual) -> {
                    assertThat((BigDecimal) actual).isEqualByComparingTo(BigDecimal.valueOf(18000));
                })
                .hasEntrySatisfying("salary_allowance_percentage", (actual) -> {
                    assertThat((BigDecimal) actual).isEqualByComparingTo(BigDecimal.valueOf(40));
                })
                .hasEntrySatisfying("last_salary_changed_date", (actual) -> {
                    Timestamp actualTimestamp = (Timestamp) actual;
                    LocalDateTime actualDateTime = actualTimestamp.toLocalDateTime();
                    assertThat(actualDateTime)
                            .isBefore(LocalDateTime.now())
                            .isAfter(LocalDateTime.now().minusSeconds(5));
                });

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(notificationServiceQueueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(1)
                    .build()).messages();

            assertThat(messages).hasSize(1);

            Map<String, Object> messageBody = objectMapper.readValue(messages.getFirst().body(), Map.class);

            assertThat(messageBody)
                    .hasSize(6)
                    .containsEntry("firstName", "David_updated")
                    .containsEntry("lastName", "Kilan_updated")
                    .containsEntry("email", "david.kilan.updated@example.com")
                    .containsEntry("currentMonthlyAllowance", 300d)
                    .containsEntry("newMonthlyAllowance", 600d);
        });

    }

    @Test
    public void testDeleteEmployeeShouldDeleteEmployeeFromDatabase() {
        var createEmployeeRequest = Map.of(
                "firstName", "David",
                "lastName", "Kilan",
                "email", "david.kilan@example.com",
                "payrollId", "PAY123",
                "annualSalary", BigDecimal.valueOf(12000),
                "salaryAllowancePercentage", BigDecimal.valueOf(30)
        );

        var response = given()
                .contentType(ContentType.JSON)
                .body(createEmployeeRequest)
                .post("/api/v1/employee")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .jsonPath();

        int employeeId = response.getInt("id");

        int countAfterInsert = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employee", Integer.class);
        assertThat(countAfterInsert).isOne();

        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("/api/v1/employee/" + employeeId)
                .then()
                .statusCode(HttpStatus.OK.value());

        int countAfterDelete = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employee", Integer.class);
        assertThat(countAfterDelete).isZero();
    }
}
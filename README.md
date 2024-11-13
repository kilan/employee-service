# employee-service

### Prerequisites
I used
- Java 21
- Maven 3.8.7 to build, but I've tested the mvnw wrapper and it also builds and runs it

### Building And Running Locally
To build the application
```shell
mvn clean install
```
To run the application, I have provided two scripts 
- `docker_scripts.sh` which starts the localStack (AWS emulator) and postgres in docker, and creates the required resources in AWS.
- `run_app.sh` runs the application locally on port`8080` against the docker containers
```shell
mvn spring-boot:run
```

### Testing the application locally
I have provided a swagger `Aslan.postman_collection.json` collection in the root of this repository.
If you don't use Postman, I pasted example API requests below.

Create User
```
POST http://localhost:8080/api/v1/employee
{
    "firstName": "David",
    "lastName": "Kilan",
    "email": "david@kilan.com",
    "payrollId": "007",
    "annualSalary": 10000,
    "salaryAllowancePercentage": 20
}
```

Update User
```
POST http://localhost:8080/api/v1/employee/1
{
    "firstName": "David",
    "lastName": "Kilan",
    "email": "david@kilan.com",
    "payrollId": "007",
    "annualSalary": 11000,
    "salaryAllowancePercentage": 20
}
```

Get User
```
Get http://localhost:8080/api/v1/employee/1
```
Get Users
```
Get http://localhost:8080/api/v1/employee
```

### Improvements (Due to time constraints)
- Use a @ControllerAdvice to return the appropriate HTTP status code for errors e.g. 
  - Bad Request for invalid requests (missing or invalid fields in request DTOs)
  - Conflict for when the payroll ID is not unique
  - Adding unit tests
  - Refactor /employee to be /employees

# GetGo Sample Java/TestNG API Test Suite

A realistic sample Java API test project used as the **learning codebase** for the Agent 1 (TestGenerationAgent) POC. The AI agent reads this repo to understand coding patterns, naming conventions, and test structure before generating new tests.

---

## Purpose

This repo exists to give the AI agent a concrete, consistent code style to learn from. Every file follows deliberate conventions documented here so the agent can reproduce them accurately in generated tests.

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Java JDK | 11 or higher | [adoptium.net](https://adoptium.net) |
| Apache Maven | 3.8+ | [maven.apache.org](https://maven.apache.org/download.cgi) |
| Python | 3.9+ (for test data generation only) | [python.org](https://python.org) |
| Git | Any recent | [git-scm.com](https://git-scm.com) |

Verify installations:
```bash
java -version
mvn -version
python --version
git --version
```

---

## Project Structure

```
getgo-sample-java-testng/
│
├── pom.xml                          # Maven build — all dependencies declared here
│
├── api-specs/
│   └── getgo-api.yaml               # OpenAPI 3.0 spec — all endpoints, schemas, examples
│
├── test-data/
│   └── getgo_test_data.xlsx         # Excel test data (3 sheets — see below)
│
├── generate_test_data.py            # Script to regenerate the Excel file
│
├── src/test/
│   ├── resources/
│   │   ├── testng.xml               # Test suite configuration
│   │   └── log4j2.xml               # Logging configuration
│   │
│   └── java/com/getgo/api/
│       ├── functions/               # LAYER 1 — shared utilities (agent reads this)
│       │   ├── BaseTest.java        # All test classes extend this
│       │   ├── ApiClient.java       # REST-assured wrapper — all HTTP methods
│       │   └── ExcelDataReader.java # Apache POI — reads test data rows from Excel
│       │
│       └── tests/                   # LAYER 2 — test classes (agent reads these)
│           ├── BookingApiTest.java  # POST /bookings, GET /bookings/{id}
│           ├── DriverApiTest.java   # POST /drivers, PATCH /drivers/{id}/availability
│           └── RideApiTest.java     # POST /rides/start, complete, GET /fare, POST /rate
```

---

## Quick Start

### 1. Clone the repo
```bash
git clone https://github.com/your-org/getgo-sample-java-testng.git
cd getgo-sample-java-testng
```

### 2. Generate test data (first time only)
```bash
pip install openpyxl
python generate_test_data.py
# Output: test-data/getgo_test_data.xlsx
```

### 3. Run all tests
```bash
mvn clean test
```

### 4. Run a specific test group
```bash
mvn clean test -Dgroups=smoke
mvn clean test -Dgroups=booking
mvn clean test -Dgroups=negative
```

### 5. Override base URL and auth token
```bash
mvn clean test -Dbase.url=https://api.getgo-staging.com -Dauth.token=your-token-here
```

### 6. Generate Allure report
```bash
mvn allure:serve
# Opens report in browser automatically
```

---

## Dependencies

All declared in `pom.xml`. Key libraries:

| Library | Purpose |
|---------|---------|
| `testng 7.8.0` | Test runner and lifecycle annotations |
| `rest-assured 5.3.2` | HTTP client for API calls |
| `jackson-databind 2.15.2` | JSON serialization of request payloads |
| `poi-ooxml 5.2.3` | Read/write Excel `.xlsx` test data |
| `allure-testng 2.24.0` | Rich HTML test reports |
| `log4j-core 2.21.1` | Structured logging |

---

## Coding Conventions

> **This section is critical for the AI agent.** It describes exactly how new test code must be written to match the existing style.

### Package Structure

```
com.getgo.api.functions   ← utilities and base classes (never put @Test here)
com.getgo.api.tests       ← all @Test classes go here
```

### Class-Level Annotations

Every test class must have:
```java
@Epic("Domain Name")       // e.g. "Booking Management"
@Feature("Feature Name")   // e.g. "Booking API"
public class XxxApiTest extends BaseTest { ... }
```

### Method-Level Annotations

Every `@Test` method must have all four of these:
```java
@Test(groups = {"smoke", "booking"})
@AIOTestCase("TC-XXX")                         // links to Jira AIO Tests
@Description("Plain English description of what this test does and expected outcome")
@Severity(SeverityLevel.BLOCKER)               // BLOCKER | CRITICAL | NORMAL | MINOR
```

### Test Method Naming

Convention: `test_<action>_<scenario>_<expectedOutcome>`

Examples:
```java
test_createBooking_validPayload_returns201
test_getBookingById_nonExistentId_returns404
test_registerDriver_duplicateNric_returns409
test_rateRide_invalidRating_returns422
```

### HTTP Calls

Always use `apiClient` (inherited from `BaseTest`). Never instantiate REST-assured directly in a test class.
```java
Response response = apiClient.post("/bookings", payload);
Response response = apiClient.get("/bookings/" + bookingId);
Response response = apiClient.patch("/drivers/" + driverId + "/availability", payload);
```

### Request Payloads

Use `Map.of(...)` for simple payloads:
```java
Map<String, Object> payload = Map.of(
    "passenger_id",   row.get("passenger_id"),
    "pickup_lat",     Double.parseDouble(row.get("pickup_lat")),
    "vehicle_type",   row.get("vehicle_type")
);
```

### Assertions

Always use Hamcrest matchers. Never use TestNG's `Assert.assertEquals` directly.
```java
// ✅ Correct
assertThat("Status code should be 201", response.statusCode(), equalTo(201));
assertThat("booking_id should not be null", response.jsonPath().getString("booking_id"), notNullValue());
assertThat("status should be PENDING", response.jsonPath().getString("status"), equalTo("PENDING"));

// ❌ Wrong — do not use these
Assert.assertEquals(response.statusCode(), 201);
assertEquals(response.getStatusCode(), 201);
```

Always include a descriptive message as the first argument of `assertThat`.

### Test Data (Excel)

When a test needs multiple data rows, use a `@DataProvider`:
```java
@DataProvider(name = "createBookingData")
public Object[][] createBookingData() {
    List<Map<String, String>> rows = ExcelDataReader.readSheet("CreateBooking");
    return rows.stream()
            .map(row -> new Object[]{row})
            .toArray(Object[][]::new);
}

@Test(dataProvider = "createBookingData")
public void test_createBooking_validPayload_returns201(Map<String, String> row) {
    String passengerId = row.get("passenger_id");
    double pickupLat   = Double.parseDouble(row.get("pickup_lat"));
    ...
}
```

For single-row or hardcoded scenarios, inline values are acceptable.

### Logging

Use the inherited `log` instance (Log4J2, available via `BaseTest`):
```java
log.info("Test data row: {}", row);
log.info("Calling POST /bookings with passenger_id={}", passengerId);
```

---

## Test Data (Excel)

File: `test-data/getgo_test_data.xlsx`

| Sheet | Covers | Columns |
|-------|--------|---------|
| `CreateBooking` | `BookingApiTest` data provider | passenger_id, pickup_lat, pickup_lng, dropoff_lat, dropoff_lng, vehicle_type, payment_method |
| `RegisterDriver` | `DriverApiTest` data provider | full_name, nric, phone, vehicle_plate, vehicle_type, license_expiry |
| `RideLifecycle` | `RideApiTest` data provider | booking_id, driver_id, start_lat, start_lng, end_lat, end_lng, distance_km |

Regenerate from scratch:
```bash
python generate_test_data.py
```

---

## API Spec

File: `api-specs/getgo-api.yaml`

OpenAPI 3.0 spec covering all tested endpoints. The AI agent reads this to understand:
- Endpoint paths and HTTP methods
- Required vs optional request fields
- Response schemas and status codes
- Enum values for fields like `vehicle_type`, `status`, `availability`
- Singapore-specific data formats (NRIC pattern, SGD currency)

View in browser: paste into [editor.swagger.io](https://editor.swagger.io)

---

## AIO Test Case IDs

Test case IDs are linked to Jira via the `@AIOTestCase` annotation.

| ID Range | Test Class |
|----------|-----------|
| TC-001 to TC-009 | BookingApiTest |
| TC-010 to TC-019 | DriverApiTest |
| TC-020 to TC-029 | RideApiTest |

New tests generated by the AI agent should follow the next available ID in the relevant range.

---

## CI/CD

Tests run via Jenkins using Maven:
```bash
mvn clean test -Dbase.url=${BASE_URL} -Dauth.token=${AUTH_TOKEN}
```

Allure results are archived and published as a Jenkins build artifact.

---

## For the AI Agent

When generating a new `@Test` class for an endpoint, follow this checklist:

- [ ] Place the class in `com.getgo.api.tests`
- [ ] Extend `BaseTest`
- [ ] Add `@Epic` and `@Feature` at class level
- [ ] Name each method `test_<action>_<scenario>_<expectedOutcome>`
- [ ] Add `@Test`, `@AIOTestCase`, `@Description`, `@Severity` on every method
- [ ] Use `apiClient.get/post/put/patch/delete` — never raw REST-assured
- [ ] Use `Map.of(...)` for payloads
- [ ] Assert with Hamcrest `assertThat` — always include a description string
- [ ] Add a `@DataProvider` if the test needs multiple data rows
- [ ] Add new Excel rows to the matching sheet in `getgo_test_data.xlsx`
- [ ] Reference the OpenAPI spec in `api-specs/getgo-api.yaml` for field names and types

---

## Troubleshooting

**Tests fail with connection refused**
The staging API is not publicly accessible. Set `base.url` to a mock server or use WireMock locally.

**Excel file not found**
Run `python generate_test_data.py` from the project root before running tests.

**Allure report is empty**
Run `mvn clean test` first to generate results, then `mvn allure:serve`.

**Java version error**
This project requires Java 11+. Check with `java -version` and update `JAVA_HOME` if needed.

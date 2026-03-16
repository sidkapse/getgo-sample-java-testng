package com.getgo.api.tests;

import com.getgo.api.functions.BaseTest;
import com.getgo.api.functions.ExcelDataReader;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.AIOTestCase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * BookingApiTest — tests for POST /bookings and GET /bookings/{id}.
 *
 * CODING CONVENTIONS:
 * - Class-level @Epic and @Feature group tests in Allure report.
 * - Each @Test method has @AIOTestCase("TC-xxx") annotation linking to AIO Tests in Jira.
 * - Each @Test method has @Description explaining the scenario.
 * - Each @Test method has @Severity indicating priority.
 * - Test data is loaded from Excel sheet matching class concern (e.g. "CreateBooking").
 * - Assertions use Hamcrest matchers (assertThat + Matchers.*).
 * - All assertions are on the Response object returned by apiClient methods.
 * - Test method naming convention: test_<action>_<scenario>_<expectedOutcome>
 */
@Epic("Booking Management")
@Feature("Booking API")
public class BookingApiTest extends BaseTest {

    // ── Data Provider ──────────────────────────────────────────────────────

    @DataProvider(name = "createBookingData")
    public Object[][] createBookingData() {
        List<Map<String, String>> rows = ExcelDataReader.readSheet("CreateBooking");
        return rows.stream()
                .map(row -> new Object[]{row})
                .toArray(Object[][]::new);
    }

    // ── Test Methods ────────────────────────────────────────────────────────

    @Test(dataProvider = "createBookingData", groups = {"smoke", "booking"})
    @AIOTestCase("TC-001")
    @Description("Create a new booking with valid passenger and location data — expect 201 Created")
    @Severity(SeverityLevel.BLOCKER)
    public void test_createBooking_validPayload_returns201(Map<String, String> row) {
        log.info("Test data row: {}", row);

        Map<String, Object> payload = Map.of(
                "passenger_id",    row.get("passenger_id"),
                "pickup_lat",      Double.parseDouble(row.get("pickup_lat")),
                "pickup_lng",      Double.parseDouble(row.get("pickup_lng")),
                "dropoff_lat",     Double.parseDouble(row.get("dropoff_lat")),
                "dropoff_lng",     Double.parseDouble(row.get("dropoff_lng")),
                "vehicle_type",    row.get("vehicle_type"),
                "payment_method",  row.get("payment_method")
        );

        Response response = apiClient.post("/bookings", payload);

        assertThat("Status code should be 201", response.statusCode(), equalTo(201));
        assertThat("Response should contain booking_id", response.jsonPath().getString("booking_id"), notNullValue());
        assertThat("Booking status should be PENDING", response.jsonPath().getString("status"), equalTo("PENDING"));
        assertThat("Passenger ID should match", response.jsonPath().getString("passenger_id"), equalTo(row.get("passenger_id")));
    }

    @Test(groups = {"smoke", "booking"})
    @AIOTestCase("TC-002")
    @Description("Retrieve a booking by ID — expect 200 OK with correct booking data")
    @Severity(SeverityLevel.CRITICAL)
    public void test_getBookingById_existingId_returns200() {
        String bookingId = "BK-9001";

        Response response = apiClient.get("/bookings/" + bookingId);

        assertThat("Status code should be 200", response.statusCode(), equalTo(200));
        assertThat("booking_id should match", response.jsonPath().getString("booking_id"), equalTo(bookingId));
        assertThat("status should not be null", response.jsonPath().getString("status"), notNullValue());
        assertThat("passenger_id should not be null", response.jsonPath().getString("passenger_id"), notNullValue());
    }

    @Test(groups = {"negative", "booking"})
    @AIOTestCase("TC-003")
    @Description("Create booking with missing required field passenger_id — expect 400 Bad Request")
    @Severity(SeverityLevel.NORMAL)
    public void test_createBooking_missingPassengerId_returns400() {
        Map<String, Object> payload = Map.of(
                "pickup_lat",   1.3048,
                "pickup_lng",   103.8318,
                "dropoff_lat",  1.2966,
                "dropoff_lng",  103.8536,
                "vehicle_type", "STANDARD"
        );

        Response response = apiClient.post("/bookings", payload);

        assertThat("Status code should be 400", response.statusCode(), equalTo(400));
        assertThat("Error message should mention passenger_id",
                response.jsonPath().getString("error"), containsString("passenger_id"));
    }

    @Test(groups = {"negative", "booking"})
    @AIOTestCase("TC-004")
    @Description("Retrieve a booking with a non-existent ID — expect 404 Not Found")
    @Severity(SeverityLevel.NORMAL)
    public void test_getBookingById_nonExistentId_returns404() {
        Response response = apiClient.get("/bookings/BK-INVALID-9999");

        assertThat("Status code should be 404", response.statusCode(), equalTo(404));
        assertThat("Error should say not found",
                response.jsonPath().getString("error").toLowerCase(), containsString("not found"));
    }
}

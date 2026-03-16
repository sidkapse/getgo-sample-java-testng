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
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * RideApiTest — tests for the ride lifecycle endpoints.
 *
 * CODING CONVENTIONS: same as BookingApiTest — see that file for full reference.
 * Endpoints covered:
 *   POST  /rides/{bookingId}/start   — driver starts the ride
 *   POST  /rides/{rideId}/complete   — driver completes the ride
 *   GET   /rides/{rideId}/fare       — fetch calculated fare
 *   POST  /rides/{rideId}/rate       — passenger rates the ride
 */
@Epic("Ride Lifecycle")
@Feature("Ride API")
public class RideApiTest extends BaseTest {

    @Test(groups = {"smoke", "ride"})
    @AIOTestCase("TC-020")
    @Description("Driver starts a ride for an accepted booking — expect 200 OK with ONGOING status")
    @Severity(SeverityLevel.BLOCKER)
    public void test_startRide_acceptedBooking_returns200() {
        String bookingId = "BK-9001";
        Map<String, Object> payload = Map.of(
                "driver_id",       "DR-5001",
                "current_lat",     1.3048,
                "current_lng",     103.8318
        );

        Response response = apiClient.post("/rides/" + bookingId + "/start", payload);

        assertThat("Status code should be 200", response.statusCode(), equalTo(200));
        assertThat("ride_id should be present", response.jsonPath().getString("ride_id"), notNullValue());
        assertThat("Status should be ONGOING", response.jsonPath().getString("status"), equalTo("ONGOING"));
        assertThat("driver_id should match", response.jsonPath().getString("driver_id"), equalTo("DR-5001"));
    }

    @Test(groups = {"smoke", "ride"})
    @AIOTestCase("TC-021")
    @Description("Driver completes an ongoing ride — expect 200 OK with COMPLETED status and fare")
    @Severity(SeverityLevel.BLOCKER)
    public void test_completeRide_ongoingRide_returns200() {
        String rideId = "RD-7001";
        Map<String, Object> payload = Map.of(
                "end_lat",  1.2966,
                "end_lng",  103.8536,
                "distance_km", 4.2
        );

        Response response = apiClient.post("/rides/" + rideId + "/complete", payload);

        assertThat("Status code should be 200", response.statusCode(), equalTo(200));
        assertThat("Status should be COMPLETED", response.jsonPath().getString("status"), equalTo("COMPLETED"));
        assertThat("fare_amount should be positive",
                response.jsonPath().getDouble("fare_amount"), greaterThan(0.0));
    }

    @Test(groups = {"smoke", "ride"})
    @AIOTestCase("TC-022")
    @Description("Fetch fare breakdown for a completed ride — expect 200 OK with itemised fare")
    @Severity(SeverityLevel.CRITICAL)
    public void test_getRideFare_completedRide_returnsBreakdown() {
        String rideId = "RD-7001";

        Response response = apiClient.get("/rides/" + rideId + "/fare");

        assertThat("Status code should be 200", response.statusCode(), equalTo(200));
        assertThat("base_fare should be present", response.jsonPath().getDouble("base_fare"), greaterThanOrEqualTo(0.0));
        assertThat("distance_fare should be present", response.jsonPath().getDouble("distance_fare"), greaterThanOrEqualTo(0.0));
        assertThat("total_fare should be present", response.jsonPath().getDouble("total_fare"), greaterThan(0.0));
        assertThat("currency should be SGD", response.jsonPath().getString("currency"), equalTo("SGD"));
    }

    @Test(groups = {"smoke", "ride"})
    @AIOTestCase("TC-023")
    @Description("Passenger rates a completed ride — expect 201 Created with saved rating")
    @Severity(SeverityLevel.NORMAL)
    public void test_rateRide_validRating_returns201() {
        String rideId = "RD-7001";
        Map<String, Object> payload = Map.of(
                "rating",  5,
                "comment", "Great ride, very punctual"
        );

        Response response = apiClient.post("/rides/" + rideId + "/rate", payload);

        assertThat("Status code should be 201", response.statusCode(), equalTo(201));
        assertThat("rating should be saved as 5", response.jsonPath().getInt("rating"), equalTo(5));
    }

    @Test(groups = {"negative", "ride"})
    @AIOTestCase("TC-024")
    @Description("Rate a ride with out-of-range value (6) — expect 422 Unprocessable Entity")
    @Severity(SeverityLevel.NORMAL)
    public void test_rateRide_invalidRating_returns422() {
        String rideId = "RD-7001";
        Map<String, Object> payload = Map.of("rating", 6);

        Response response = apiClient.post("/rides/" + rideId + "/rate", payload);

        assertThat("Status code should be 422", response.statusCode(), equalTo(422));
        assertThat("Error should mention rating range",
                response.jsonPath().getString("error"), containsString("rating"));
    }
}

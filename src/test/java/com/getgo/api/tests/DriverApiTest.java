package com.getgo.api.tests;

import com.getgo.api.functions.BaseTest;
import com.getgo.api.functions.ExcelDataReader;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
//import org.testng.annotations.AIOTestCase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * DriverApiTest — tests for driver registration and availability endpoints.
 *
 * CODING CONVENTIONS: same as BookingApiTest — see that file for full reference.
 * Endpoints covered:
 *   POST /drivers          — register a new driver
 *   GET  /drivers/{id}     — fetch driver profile
 *   PATCH /drivers/{id}/availability — toggle driver online/offline
 */
@Epic("Driver Management")
@Feature("Driver API")
public class DriverApiTest extends BaseTest {

    @DataProvider(name = "registerDriverData")
    public Object[][] registerDriverData() {
        List<Map<String, String>> rows = ExcelDataReader.readSheet("RegisterDriver");
        return rows.stream()
                .map(row -> new Object[]{row})
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "registerDriverData", groups = {"smoke", "driver"})
    @AIOTestCase("TC-010")
    @Description("Register a new driver with valid NRIC and vehicle details — expect 201 Created")
    @Severity(SeverityLevel.BLOCKER)
    public void test_registerDriver_validPayload_returns201(Map<String, String> row) {
        log.info("Test data row: {}", row);

        Map<String, Object> payload = Map.of(
                "full_name",       row.get("full_name"),
                "nric",            row.get("nric"),
                "phone",           row.get("phone"),
                "vehicle_plate",   row.get("vehicle_plate"),
                "vehicle_type",    row.get("vehicle_type"),
                "license_expiry",  row.get("license_expiry")
        );

        Response response = apiClient.post("/drivers", payload);

        assertThat("Status code should be 201", response.statusCode(), equalTo(201));
        assertThat("driver_id should be present", response.jsonPath().getString("driver_id"), notNullValue());
        assertThat("Status should be PENDING_VERIFICATION",
                response.jsonPath().getString("status"), equalTo("PENDING_VERIFICATION"));
    }

    @Test(groups = {"smoke", "driver"})
    @AIOTestCase("TC-011")
    @Description("Fetch driver profile by ID — expect 200 OK with complete profile data")
    @Severity(SeverityLevel.CRITICAL)
    public void test_getDriverById_existingDriver_returns200() {
        String driverId = "DR-5001";

        Response response = apiClient.get("/drivers/" + driverId);

        assertThat("Status code should be 200", response.statusCode(), equalTo(200));
        assertThat("driver_id should match", response.jsonPath().getString("driver_id"), equalTo(driverId));
        assertThat("full_name should not be blank", response.jsonPath().getString("full_name"), not(emptyString()));
        assertThat("vehicle_plate should not be blank", response.jsonPath().getString("vehicle_plate"), not(emptyString()));
    }

    @Test(groups = {"smoke", "driver"})
    @AIOTestCase("TC-012")
    @Description("Set driver availability to ONLINE — expect 200 OK with updated status")
    @Severity(SeverityLevel.CRITICAL)
    public void test_updateAvailability_setOnline_returns200() {
        String driverId = "DR-5001";
        Map<String, Object> payload = Map.of("availability", "ONLINE");

        Response response = apiClient.patch("/drivers/" + driverId + "/availability", payload);

        assertThat("Status code should be 200", response.statusCode(), equalTo(200));
        assertThat("Availability should be ONLINE",
                response.jsonPath().getString("availability"), equalTo("ONLINE"));
    }

    @Test(groups = {"negative", "driver"})
    @AIOTestCase("TC-013")
    @Description("Register driver with duplicate NRIC — expect 409 Conflict")
    @Severity(SeverityLevel.NORMAL)
    public void test_registerDriver_duplicateNric_returns409() {
        Map<String, Object> payload = Map.of(
                "full_name",      "Ahmad Bin Ali",
                "nric",           "S9012345A",   // pre-existing NRIC in system
                "phone",          "+6591234567",
                "vehicle_plate",  "SBX1234Z",
                "vehicle_type",   "STANDARD",
                "license_expiry", "2026-12-31"
        );

        Response response = apiClient.post("/drivers", payload);

        assertThat("Status code should be 409", response.statusCode(), equalTo(409));
        assertThat("Error should mention conflict",
                response.jsonPath().getString("error").toLowerCase(), containsString("already exists"));
    }
}

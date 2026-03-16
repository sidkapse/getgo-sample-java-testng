package com.getgo.api.functions;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * ApiClient — reusable REST-assured wrapper used by all test classes.
 *
 * CODING CONVENTIONS:
 * - All HTTP methods return a raw REST-assured Response object.
 * - Tests call these methods and then assert on the returned Response.
 * - Base URL and auth token are set once via RequestSpecBuilder.
 * - Every request logs request + response details via LogDetail.ALL.
 *
 * USAGE PATTERN IN TESTS:
 *   Response response = apiClient.post("/bookings", payload);
 *   response.then().statusCode(201);
 */
public class ApiClient {

    private static final Logger log = LogManager.getLogger(ApiClient.class);
    private final RequestSpecification requestSpec;

    public ApiClient(String baseUrl, String authToken) {
        log.info("Initialising ApiClient for baseUrl={}", baseUrl);
        this.requestSpec = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Accept", "application/json")
                .addHeader("X-Client-Version", "1.0.0")
                .log(LogDetail.ALL)
                .build();
    }

    /** HTTP GET */
    public Response get(String path) {
        log.info("GET {}", path);
        return RestAssured.given()
                .spec(requestSpec)
                .when()
                .get(path)
                .then()
                .log().all()
                .extract().response();
    }

    /** HTTP GET with query parameters */
    public Response get(String path, Map<String, Object> queryParams) {
        log.info("GET {} params={}", path, queryParams);
        return RestAssured.given()
                .spec(requestSpec)
                .queryParams(queryParams)
                .when()
                .get(path)
                .then()
                .log().all()
                .extract().response();
    }

    /** HTTP POST with JSON body */
    public Response post(String path, Object body) {
        log.info("POST {}", path);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(path)
                .then()
                .log().all()
                .extract().response();
    }

    /** HTTP PUT with JSON body */
    public Response put(String path, Object body) {
        log.info("PUT {}", path);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(path)
                .then()
                .log().all()
                .extract().response();
    }

    /** HTTP PATCH with JSON body */
    public Response patch(String path, Object body) {
        log.info("PATCH {}", path);
        return RestAssured.given()
                .spec(requestSpec)
                .body(body)
                .when()
                .patch(path)
                .then()
                .log().all()
                .extract().response();
    }

    /** HTTP DELETE */
    public Response delete(String path) {
        log.info("DELETE {}", path);
        return RestAssured.given()
                .spec(requestSpec)
                .when()
                .delete(path)
                .then()
                .log().all()
                .extract().response();
    }
}

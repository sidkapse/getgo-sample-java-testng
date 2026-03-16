package com.getgo.api.functions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import org.testng.ITestResult;

import java.lang.reflect.Method;

/**
 * BaseTest — all test classes extend this class.
 *
 * CODING CONVENTIONS:
 * - @BeforeSuite initialises the shared ApiClient once for the full suite.
 * - @AfterSuite logs suite completion.
 * - @BeforeMethod logs the test method name before each test.
 * - @AfterMethod logs PASS/FAIL/SKIP with test method name.
 * - All test classes inherit the protected `apiClient` instance.
 *
 * CONFIGURATION:
 * - BASE_URL and AUTH_TOKEN are read from system properties.
 *   Override at runtime: mvn test -Dbase.url=https://... -Dauth.token=...
 *   Fallback defaults are used when system properties are not set.
 */
public class BaseTest {

    protected static final Logger log = LogManager.getLogger(BaseTest.class);
    protected static ApiClient apiClient;

    private static final String BASE_URL   = System.getProperty("base.url",   "https://api.getgo-staging.com");
    private static final String AUTH_TOKEN = System.getProperty("auth.token", "test-token-sandbox-001");

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        log.info("=== Suite Setup: initialising ApiClient ===");
        log.info("Base URL : {}", BASE_URL);
        apiClient = new ApiClient(BASE_URL, AUTH_TOKEN);
        log.info("=== ApiClient ready ===");
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        log.info("=== Suite Teardown: all tests complete ===");
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeTest(Method method) {
        log.info("--- START: {} ---", method.getName());
    }

    @AfterMethod(alwaysRun = true)
    public void afterTest(ITestResult result) {
        String status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASS";
            case ITestResult.FAILURE -> "FAIL";
            case ITestResult.SKIP    -> "SKIP";
            default                  -> "UNKNOWN";
        };
        log.info("--- END: {} [{}] ---", result.getName(), status);
    }
}

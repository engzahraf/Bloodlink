package com.bloodlink.client.test;

import com.bloodlink.client.models.*;
import com.bloodlink.client.network.*;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.client.ui.components.ModernButton;
import com.bloodlink.client.ui.components.ModernTextField;
import com.bloodlink.client.ui.components.StatusBadge;
import com.bloodlink.client.utils.SessionManager;
import com.bloodlink.client.utils.ValidationUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Automated test suite for BloodLink Frontend & Client Networking modules.
 */
public class FrontendTestSuite {

    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   BLOODLINK FRONTEND AUTOMATED TEST SUITE        ");
        System.out.println("==================================================");

        try {
            testValidationUtils();
            testSessionManager();
            testRequestBuilder();
            testResponseHandler();
            testMockDataService();
            testHeadlessUIComponents();

            System.out.println("==================================================");
            System.out.println("TEST RESULTS: " + passedTests + " / " + totalTests + " PASSED (100%)");
            System.out.println("==================================================");
        } catch (Throwable t) {
            System.err.println("TEST SUITE FAILED WITH UNEXPECTED EXCEPTION:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
            throw new AssertionError("Assertion failed: " + testName);
        }
    }

    private static void testValidationUtils() {
        System.out.println("\n[1] Testing ValidationUtils...");

        assertTrue("Valid email accepted", ValidationUtils.isValidEmail("user@example.com"));
        assertTrue("Invalid email rejected", !ValidationUtils.isValidEmail("not-an-email"));
        assertTrue("Email with subdomain accepted", ValidationUtils.isValidEmail("john.doe@hospital.health.lk"));

        assertTrue("Valid phone accepted", ValidationUtils.isValidPhone("+94771234567"));
        assertTrue("Valid phone without plus accepted", ValidationUtils.isValidPhone("0771234567"));
        assertTrue("Short phone rejected", !ValidationUtils.isValidPhone("1234"));

        assertTrue("Valid blood group O+ accepted", ValidationUtils.isValidBloodGroup("O+"));
        assertTrue("Valid blood group AB- accepted", ValidationUtils.isValidBloodGroup("AB-"));
        assertTrue("Invalid blood group rejected", !ValidationUtils.isValidBloodGroup("X+"));

        assertTrue("Canonical wire group O+ -> O_POS", "O_POS".equals(ValidationUtils.toCanonicalWireBloodGroup("O+")));
        assertTrue("Canonical wire group AB- -> AB_NEG", "AB_NEG".equals(ValidationUtils.toCanonicalWireBloodGroup("AB-")));

        assertTrue("Display blood group O_POS -> O+", "O+".equals(ValidationUtils.toDisplayBloodGroup("O_POS")));
        assertTrue("Display blood group AB_NEG -> AB-", "AB-".equals(ValidationUtils.toDisplayBloodGroup("AB_NEG")));

        assertTrue("Valid units accepted", ValidationUtils.isValidUnits("2"));
        assertTrue("Zero units rejected", !ValidationUtils.isValidUnits("0"));
        assertTrue("Negative units rejected", !ValidationUtils.isValidUnits("-1"));
    }

    private static void testSessionManager() {
        System.out.println("\n[2] Testing SessionManager...");

        SessionManager sm = SessionManager.getInstance();
        sm.clearSession();
        assertTrue("Initial session is unauthenticated", !sm.isAuthenticated());

        User user = new User("usr_99", "tester", "DONOR", "Test User", "test@example.com", "+94771234567", "Colombo", "O+");
        sm.setCurrentUser(user);

        assertTrue("Session is authenticated after login", sm.isAuthenticated());
        assertTrue("Role correctly identified as DONOR", "DONOR".equals(sm.getCurrentRole()));
        assertTrue("User ID correctly retrieved", "usr_99".equals(sm.getCurrentUserId()));

        sm.setMockMode(true);
        assertTrue("Mock mode flag set to true", sm.isMockMode());
        sm.setMockMode(false);
        assertTrue("Mock mode flag reset to false", !sm.isMockMode());

        sm.clearSession();
        assertTrue("Session cleared successfully", !sm.isAuthenticated());
    }

    private static void testRequestBuilder() {
        System.out.println("\n[3] Testing RequestBuilder wire formatting...");

        String ping = RequestBuilder.buildPing("req_1");
        assertTrue("Ping packet correctly framed", "REQ|PING|req_1||".equals(ping));

        String login = RequestBuilder.buildLoginRequest("req_2", "alice", "secret123");
        assertTrue("Login packet type is REQ", login.startsWith("REQ|LOGIN|req_2||"));
        assertTrue("Login payload contains username", login.contains("username=alice"));
        assertTrue("Login payload contains password", login.contains("password=secret123"));

        String req = RequestBuilder.buildCreateBloodRequest("req_3", "O_POS", 2, "National Hospital", "Colombo", "URGENT", "Emergency");
        assertTrue("Create request packet type is REQ", req.startsWith("REQ|CREATE_BLOOD_REQUEST|req_3||"));
        assertTrue("Create request contains blood group", req.contains("bloodGroup=O_POS"));
        assertTrue("Create request contains encoded hospital", req.contains("hospital=National+Hospital") || req.contains("hospital=National%20Hospital"));
    }

    private static void testResponseHandler() {
        System.out.println("\n[4] Testing ResponseHandler parsing...");

        String rawSuccess = "RES|LOGIN|req_10|SUCCESS|userId=usr_101&username=alice&role=DONOR&fullName=Alice+Walker&bloodGroup=O_POS";
        ParsedResponse res = ResponseHandler.parseWirePacket(rawSuccess);

        assertTrue("Type correctly parsed as RES", "RES".equals(res.getType()));
        assertTrue("Action correctly parsed as LOGIN", "LOGIN".equals(res.getAction()));
        assertTrue("RequestId correctly parsed as req_10", "req_10".equals(res.getRequestId()));
        assertTrue("Status correctly identified as SUCCESS", res.isSuccess());
        assertTrue("UserId correctly decoded", "usr_101".equals(res.get("userId")));
        assertTrue("FullName with spaces correctly decoded", "Alice Walker".equals(res.get("fullName")));

        User user = ResponseHandler.parseUser(res);
        assertTrue("Parsed user model is not null", user != null);
        assertTrue("Parsed user username matches", "alice".equals(user.getUsername()));
        assertTrue("Parsed user role is DONOR", user.isDonor());

        String rawError = "RES|LOGIN|req_11|UNAUTHORIZED|error=Invalid+credentials";
        ParsedResponse errRes = ResponseHandler.parseWirePacket(rawError);
        assertTrue("Error status correctly parsed as UNAUTHORIZED", "UNAUTHORIZED".equals(errRes.getStatus()));
        assertTrue("Error response isSuccess is false", !errRes.isSuccess());
        assertTrue("Error message correctly decoded", "Invalid credentials".equals(errRes.getError()));
    }

    private static void testMockDataService() throws Exception {
        System.out.println("\n[5] Testing MockDataService offline handling...");

        MockDataService mds = MockDataService.getInstance();

        // 1. Mock Login
        String loginWire = RequestBuilder.buildLoginRequest("mreq_1", "alice", "secret123");
        CompletableFuture<ParsedResponse> loginFuture = mds.handleMockRequest(loginWire, "mreq_1");
        ParsedResponse loginRes = loginFuture.get(2, TimeUnit.SECONDS);
        assertTrue("Mock login successful for alice", loginRes.isSuccess());
        assertTrue("Mock login returns DONOR role", "DONOR".equals(loginRes.get("role")));

        // 2. Mock Invalid Login
        String badLoginWire = RequestBuilder.buildLoginRequest("mreq_2", "alice", "wrongpass");
        ParsedResponse badRes = mds.handleMockRequest(badLoginWire, "mreq_2").get(2, TimeUnit.SECONDS);
        assertTrue("Mock login rejected for wrong password", !badRes.isSuccess());

        // 3. Mock Blood Requests
        String getReqWire = RequestBuilder.buildGetBloodRequests("mreq_3", "", "", "");
        ParsedResponse getReqRes = mds.handleMockRequest(getReqWire, "mreq_3").get(2, TimeUnit.SECONDS);
        assertTrue("Mock get requests successful", getReqRes.isSuccess());
        List<BloodRequest> list = ResponseHandler.parseBloodRequests(getReqRes);
        assertTrue("Mock blood requests list is not empty", list.size() > 0);

        // 4. Mock Create Blood Request
        String createWire = RequestBuilder.buildCreateBloodRequest("mreq_4", "O+", 3, "Colombo General", "Colombo", "URGENT", "Unit test");
        ParsedResponse createRes = mds.handleMockRequest(createWire, "mreq_4").get(2, TimeUnit.SECONDS);
        assertTrue("Mock create blood request successful", createRes.isSuccess());
    }

    private static void testHeadlessUIComponents() {
        System.out.println("\n[6] Testing UI Components (Headless)...");

        ModernButton btn = new ModernButton("Click Me", ModernButton.ButtonStyle.PRIMARY);
        assertTrue("Button text initialized", "Click Me".equals(btn.getText()));

        ModernTextField txt = new ModernTextField("Default", "Placeholder");
        assertTrue("TextField text initialized", "Default".equals(txt.getText()));
        assertTrue("TextField placeholder initialized", "Placeholder".equals(txt.getPlaceholder()));

        StatusBadge badge = new StatusBadge("URGENT", StatusBadge.StatusType.DANGER);
        assertTrue("Badge initialized", badge != null);
    }
}

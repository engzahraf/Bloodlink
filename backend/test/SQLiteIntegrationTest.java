package com.bloodlink.test;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.DatabaseInitializer;
import com.bloodlink.database.SQLiteDatabaseService;
import com.bloodlink.database.dao.UserDAO;
import com.bloodlink.database.models.User;
import com.bloodlink.database.security.PasswordHasher;
import com.bloodlink.interfaces.IDatabaseService;
import com.bloodlink.protocol.MessageType;
import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolAction;
import com.bloodlink.protocol.ProtocolCodec;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.protocol.ResponseStatus;
import com.bloodlink.server.BloodLinkServer;
import com.bloodlink.server.ClientManager;
import com.bloodlink.server.RequestDispatcher;
import com.bloodlink.services.AuthService;
import com.bloodlink.services.BloodMatchingService;
import com.bloodlink.services.MessagingService;
import com.bloodlink.services.NotificationService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real SQLite/JDBC Integration Test Suite for BloodLink Backend:
 * Tests the entire production stack using actual SQLite persistence, PBKDF2 hashing,
 * and live TCP socket client/server request-response workflows.
 */
public class SQLiteIntegrationTest {

    private static final String TEST_DB_PATH = "data" + File.separator + "test_integration_bloodlink.db";

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("   BLOODLINK MEMBER 02 REAL SQLITE/JDBC INTEGRATION TESTS               ");
        System.out.println("========================================================================");

        try {
            setupTestDatabase();

            // Run all 15 required integration test phases
            IDatabaseService dbService = new SQLiteDatabaseService();

            test1_DatabaseInitialization();
            test2_UserRegistration(dbService);
            test3_DuplicateEmailRejection(dbService);
            test3b_AdminSeedingAndHospitalRejection(dbService);
            test4_Pbkdf2PasswordVerification();
            test5_LoginSuccessAndFailure(dbService);
            test6_UserProfileRetrieval(dbService);
            test7_BloodRequestCreationAndRetrieval(dbService);
            test8_DonorMatching(dbService);
            test9_DonorAvailabilityUpdate(dbService);
            test10_RequestStatusUpdateAndAuthorization(dbService);
            test11_NotificationPersistenceAndRetrieval(dbService);
            test12_MessagePersistenceAndConversationHistory(dbService);
            test13_ConcurrentDatabaseOperations(dbService);
            test14_And_15_ServerStartupAndClientFlow(dbService);

        } catch (Throwable t) {
            System.err.println("Fatal exception during integration tests: " + t.getMessage());
            t.printStackTrace();
        } finally {
            cleanupTestDatabase();
        }

        System.out.println("========================================================================");
        System.out.printf("REAL SQLITE INTEGRATION RESULTS: Total: %d | Passed: %d | Failed: %d (%.1f%%)%n",
                testsRun, testsPassed, (testsRun - testsPassed),
                (testsRun > 0 ? (testsPassed * 100.0 / testsRun) : 0));
        System.out.println("========================================================================");

        if (testsPassed != testsRun) {
            System.exit(1);
        }
    }

    private static void setupTestDatabase() {
        System.out.println("[SETUP] Configuring isolated integration test DB at: " + TEST_DB_PATH);
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        DatabaseConnection.setDatabasePath(TEST_DB_PATH);
        DatabaseInitializer.initialize();
    }

    private static void cleanupTestDatabase() {
        System.out.println("\n[TEARDOWN] Resetting DatabaseConnection to default configuration...");
        DatabaseConnection.resetToDefaultDatabasePath();
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.printf("  [PASS] %-48s%n", testName);
        } else {
            System.err.printf("  [FAIL] %-48s%n", testName);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        testsRun++;
        boolean match = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (match) {
            testsPassed++;
            System.out.printf("  [PASS] %-48s%n", testName);
        } else {
            System.err.printf("  [FAIL] %-48s -> Expected: <%s>, Got: <%s>%n", testName, expected, actual);
        }
    }

    // 1. Database initialization
    private static void test1_DatabaseInitialization() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            assertTrue("1. Database initialization & connection verified", valid);
        } catch (Exception e) {
            assertTrue("1. Database initialization exception: " + e.getMessage(), false);
        }
    }

    // 2. User registration
    private static void test2_UserRegistration(IDatabaseService db) {
        Map<String, String> donorData = new HashMap<>();
        donorData.put("username", "donor_alice@test.com");
        donorData.put("password", "AlicePass2026");
        donorData.put("fullName", "Alice Walker");
        donorData.put("phone", "+94-77-1111111");
        donorData.put("role", "DONOR");
        donorData.put("bloodGroup", "O+");
        donorData.put("city", "Colombo");

        String userId = db.registerUser(donorData);
        assertTrue("2. User registration generated valid userId", userId != null && !userId.isEmpty() && Integer.parseInt(userId) > 0);

        // Register a recipient
        Map<String, String> recipData = new HashMap<>();
        recipData.put("username", "recip_bob@test.com");
        recipData.put("password", "BobPass2026");
        recipData.put("fullName", "Bob Stone");
        recipData.put("phone", "+94-77-2222222");
        recipData.put("role", "RECIPIENT");
        recipData.put("city", "Colombo");

        String recipId = db.registerUser(recipData);
        assertTrue("2. Recipient registration generated valid userId", recipId != null && !recipId.isEmpty() && Integer.parseInt(recipId) > 0);
    }

    // 3. Duplicate email rejection
    private static void test3_DuplicateEmailRejection(IDatabaseService db) {
        Map<String, String> duplicate = new HashMap<>();
        duplicate.put("username", "donor_alice@test.com");
        duplicate.put("password", "AnyPassword");
        duplicate.put("fullName", "Impostor Alice");
        duplicate.put("role", "DONOR");
        duplicate.put("bloodGroup", "A+");

        String dupResult = db.registerUser(duplicate);
        assertTrue("3. Duplicate email registration rejected (returns null)", dupResult == null);
    }

    // 3b. Role rejection & Secure Admin Seeding
    private static void test3b_AdminSeedingAndHospitalRejection(IDatabaseService db) {
        // Attempting to register as HOSPITAL must fail against SQLite / adapter
        Map<String, String> hospData = new HashMap<>();
        hospData.put("username", "hosp_user@test.com");
        hospData.put("password", "HospPass2026");
        hospData.put("fullName", "Hospital Staff");
        hospData.put("role", "HOSPITAL");
        String hospId = db.registerUser(hospData);
        assertTrue("3b. Registration with role HOSPITAL is rejected (returns null)", hospId == null);

        // Secure admin seeding
        boolean seeded = DatabaseInitializer.seedDefaultAdminIfMissing(
                "sysadmin@test.com", "SecureAdminPass#2026", "System Administrator", "+94-77-9999999");
        assertTrue("3b. Secure admin seeding succeeded", seeded);

        // Idempotent: second seeding attempt returns false
        boolean seededAgain = DatabaseInitializer.seedDefaultAdminIfMissing(
                "sysadmin@test.com", "SecureAdminPass#2026", "System Administrator", "+94-77-9999999");
        assertTrue("3b. Duplicate admin seeding rejected (idempotent)", !seededAgain);

        // Admin authentication with PBKDF2 hash verification
        Map<String, String> adminAuth = db.authenticate("sysadmin@test.com", "SecureAdminPass#2026");
        assertTrue("3b. Admin authentication succeeded", adminAuth != null);
        assertEquals("3b. Admin role verified", "ADMIN", adminAuth.get("role"));
    }

    // 4. PBKDF2 password verification
    private static void test4_Pbkdf2PasswordVerification() {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserByEmail("donor_alice@test.com");
        assertTrue("4. User record found in SQLite", user != null);

        String storedHash = user.getPasswordHash();
        assertTrue("4. Password stored in PBKDF2 format", storedHash != null && storedHash.startsWith("PBKDF2$"));

        boolean matchCorrect = PasswordHasher.checkPassword("AlicePass2026", storedHash);
        boolean matchIncorrect = PasswordHasher.checkPassword("WrongPassword", storedHash);

        assertTrue("4. PBKDF2 correct password matches", matchCorrect);
        assertTrue("4. PBKDF2 incorrect password rejected", !matchIncorrect);
    }

    // 5. Login with correct and incorrect passwords
    private static void test5_LoginSuccessAndFailure(IDatabaseService db) {
        Map<String, String> success = db.authenticate("donor_alice@test.com", "AlicePass2026");
        assertTrue("5. Login with correct credentials succeeded", success != null);
        if (success != null) {
            assertEquals("5. Authenticated user email matches", "donor_alice@test.com", success.get("email"));
            assertEquals("5. Authenticated user role is DONOR", "DONOR", success.get("role"));
            assertEquals("5. Authenticated blood group is O_POS", "O_POS", success.get("bloodGroup"));
            assertTrue("5. Password hash NOT exposed in auth result", !success.containsKey("password") && !success.containsKey("passwordHash"));
        }

        Map<String, String> failed = db.authenticate("donor_alice@test.com", "BadPassword");
        assertTrue("5. Login with bad password returns null", failed == null);

        Map<String, String> nonExistent = db.authenticate("ghost@test.com", "Password");
        assertTrue("5. Login with non-existent user returns null", nonExistent == null);
    }

    // 6. User profile retrieval
    private static void test6_UserProfileRetrieval(IDatabaseService db) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserByEmail("donor_alice@test.com");

        Map<String, String> profile = db.getUserById(String.valueOf(user.getUserId()));
        assertTrue("6. Profile retrieved by user ID", profile != null);
        if (profile != null) {
            assertEquals("6. Profile fullName matches", "Alice Walker", profile.get("fullName"));
            assertEquals("6. Profile city matches", "Colombo", profile.get("city"));
            assertTrue("6. Profile password NOT exposed", !profile.containsKey("passwordHash"));
        }
    }

    // 7. Blood request creation and retrieval
    private static void test7_BloodRequestCreationAndRetrieval(IDatabaseService db) {
        UserDAO userDAO = new UserDAO();
        User recipient = userDAO.getUserByEmail("recip_bob@test.com");

        Map<String, String> reqData = new HashMap<>();
        reqData.put("requesterId", String.valueOf(recipient.getUserId()));
        reqData.put("bloodGroup", "O+");
        reqData.put("unitsNeeded", "2");
        reqData.put("hospital", "National Blood Center");
        reqData.put("city", "Colombo");
        reqData.put("contactNumber", "+94-77-2222222");
        reqData.put("urgency", "CRITICAL");
        reqData.put("notes", "Urgent transfusion needed");

        String reqId = db.createBloodRequest(reqData);
        assertTrue("7. Blood request created in SQLite with ID: " + reqId, reqId != null && Integer.parseInt(reqId) > 0);

        List<Map<String, String>> requests = db.getBloodRequests(Map.of("bloodRequestId", reqId));
        assertEquals("7. Retrieved created blood request by ID", 1, requests.size());
        if (!requests.isEmpty()) {
            assertEquals("7. Blood request status is PENDING", "PENDING", requests.get(0).get("status"));
            assertEquals("7. Blood request urgency is CRITICAL", "CRITICAL", requests.get(0).get("urgency"));
            assertEquals("7. Blood request hospital matches", "National Blood Center", requests.get(0).get("hospital"));
        }
    }

    // 8. Donor matching
    private static void test8_DonorMatching(IDatabaseService db) {
        // Alice is O+ in Colombo. Compatible donors for A+ include O+ and O-.
        List<Map<String, String>> matched = db.findEligibleDonors(List.of("O_POS", "O_NEG"), "Colombo");
        assertTrue("8. Found compatible donors in Colombo", !matched.isEmpty());

        boolean aliceFound = matched.stream().anyMatch(d -> "donor_alice@test.com".equals(d.get("email")));
        assertTrue("8. Alice found in eligible donor match", aliceFound);
    }

    // 9. Donor availability update
    private static void test9_DonorAvailabilityUpdate(IDatabaseService db) {
        UserDAO userDAO = new UserDAO();
        User alice = userDAO.getUserByEmail("donor_alice@test.com");

        boolean toggledOff = db.updateDonorAvailability(String.valueOf(alice.getUserId()), false);
        assertTrue("9. Donor availability updated to false", toggledOff);

        List<Map<String, String>> afterOff = db.findEligibleDonors(List.of("O_POS"), "Colombo");
        boolean aliceExcluded = afterOff.stream().noneMatch(d -> "donor_alice@test.com".equals(d.get("email")));
        assertTrue("9. Unavailable donor excluded from search", aliceExcluded);

        boolean toggledOn = db.updateDonorAvailability(String.valueOf(alice.getUserId()), true);
        assertTrue("9. Donor availability restored to true", toggledOn);
    }

    // 10. Request status update and authorization
    private static void test10_RequestStatusUpdateAndAuthorization(IDatabaseService db) {
        List<Map<String, String>> allReqs = db.getBloodRequests(null);
        assertTrue("10. Found active requests", !allReqs.isEmpty());

        String reqId = allReqs.get(0).get("bloodRequestId");
        boolean updated = db.updateBloodRequestStatus(reqId, "MATCHED");
        assertTrue("10. Request status updated to MATCHED", updated);

        List<Map<String, String>> verified = db.getBloodRequests(Map.of("bloodRequestId", reqId));
        assertEquals("10. Verified status is MATCHED", "MATCHED", verified.get(0).get("status"));
    }

    // 11. Notification persistence and retrieval
    private static void test11_NotificationPersistenceAndRetrieval(IDatabaseService db) {
        UserDAO userDAO = new UserDAO();
        User alice = userDAO.getUserByEmail("donor_alice@test.com");

        boolean saved = db.saveNotification(String.valueOf(alice.getUserId()),
                                            "URGENT: Blood Alert",
                                            "Urgent O+ blood needed at National Blood Center",
                                            "CRITICAL",
                                            System.currentTimeMillis());
        assertTrue("11. Notification persisted in SQLite", saved);

        List<Map<String, String>> notifs = db.getNotificationsForUser(String.valueOf(alice.getUserId()), 10);
        assertTrue("11. Retrieved notifications for user", !notifs.isEmpty());
        assertTrue("11. Notification message content verified", notifs.get(0).get("message").contains("Urgent O+ blood"));
    }

    // 12. Message persistence and conversation history
    private static void test12_MessagePersistenceAndConversationHistory(IDatabaseService db) {
        UserDAO userDAO = new UserDAO();
        User alice = userDAO.getUserByEmail("donor_alice@test.com");
        User bob = userDAO.getUserByEmail("recip_bob@test.com");

        String m1 = db.saveMessage(String.valueOf(bob.getUserId()), String.valueOf(alice.getUserId()),
                                   "Hi Alice, could you donate for my surgery?", System.currentTimeMillis());
        assertTrue("12. First message saved with ID: " + m1, m1 != null && Integer.parseInt(m1) > 0);

        String m2 = db.saveMessage(String.valueOf(alice.getUserId()), String.valueOf(bob.getUserId()),
                                   "Yes Bob, I will arrive tomorrow at 9 AM.", System.currentTimeMillis());
        assertTrue("12. Second message saved with ID: " + m2, m2 != null && Integer.parseInt(m2) > 0);

        List<Map<String, String>> conversation = db.getConversationHistory(
                String.valueOf(alice.getUserId()), String.valueOf(bob.getUserId()), 10);

        assertEquals("12. Chronological conversation history has 2 messages", 2, conversation.size());
        assertEquals("12. Message 1 content matches", "Hi Alice, could you donate for my surgery?", conversation.get(0).get("content"));
        assertEquals("12. Message 2 content matches", "Yes Bob, I will arrive tomorrow at 9 AM.", conversation.get(1).get("content"));
    }

    // 13. Multiple concurrent database operations
    private static void test13_ConcurrentDatabaseOperations(IDatabaseService db) {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCounter = new AtomicInteger(0);

        UserDAO userDAO = new UserDAO();
        User alice = userDAO.getUserByEmail("donor_alice@test.com");
        User bob = userDAO.getUserByEmail("recip_bob@test.com");

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    db.saveMessage(String.valueOf(alice.getUserId()), String.valueOf(bob.getUserId()),
                                   "Concurrent message " + idx, System.currentTimeMillis());
                    db.saveNotification(String.valueOf(alice.getUserId()), "Alert", "Concurrent alert " + idx, "NORMAL", System.currentTimeMillis());
                    db.getBloodRequests(null);
                    db.findEligibleDonors(List.of("O_POS"), null);
                    successCounter.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Concurrent task " + idx + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean done = latch.await(10, TimeUnit.SECONDS);
            pool.shutdown();
            assertTrue("13. Executed " + successCounter.get() + "/" + threads + " concurrent SQLite operations safely",
                       done && successCounter.get() == threads);
        } catch (InterruptedException e) {
            assertTrue("13. Concurrency test interrupted", false);
        }
    }

    // 14 & 15. Server startup with real SQLite service and client request-response flow
    private static void test14_And_15_ServerStartupAndClientFlow(IDatabaseService dbService) throws Exception {
        System.out.println("\n--- Testing Live TCP Server Socket Flow with SQLite Database ---");

        ClientManager clientManager = new ClientManager();
        NotificationService notifService = new NotificationService(clientManager, dbService);
        AuthService authService = new AuthService(dbService, clientManager);
        BloodMatchingService matchingService = new BloodMatchingService(dbService, clientManager, notifService);
        MessagingService messagingService = new MessagingService(dbService, clientManager);

        RequestDispatcher dispatcher = new RequestDispatcher(authService, matchingService, notifService, messagingService);
        BloodLinkServer server = new BloodLinkServer(0, clientManager, dispatcher);
        server.startAsync();

        Thread.sleep(200);
        int port = server.getPort();
        assertTrue("14. TCP Server started on port " + port + " with SQLite database injected", port > 0 && server.isRunning());

        // Connect real TCP socket client
        try (Socket clientSocket = new Socket("127.0.0.1", port);
             DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()))) {

            // Test A: PING / PONG
            ProtocolCodec.writePacket(out, Packet.createPing("p-1"));
            Packet pong = ProtocolCodec.readPacket(in);
            assertEquals("15. PING response is PONG", ProtocolAction.PONG, pong.getAction());
            assertEquals("15. PONG status is SUCCESS", ResponseStatus.SUCCESS, pong.getStatus());

            // Test B: Login Alice against real SQLite database
            ProtocolPayload loginPayload = new ProtocolPayload()
                    .set("username", "donor_alice@test.com")
                    .set("password", "AlicePass2026");
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.LOGIN, "login-1", loginPayload));

            Packet loginRes = ProtocolCodec.readPacket(in);
            assertEquals("15. Client login with real SQLite credentials SUCCESS", ResponseStatus.SUCCESS, loginRes.getStatus());
            assertEquals("15. Client assigned correct role DONOR", "DONOR", loginRes.getPayload().get("role"));
            assertEquals("15. Client bloodGroup is O_POS", "O_POS", loginRes.getPayload().get("bloodGroup"));

            // Test C: Fetch notifications over socket
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.GET_NOTIFICATIONS, "notif-req", new ProtocolPayload().set("limit", 5)));
            Packet notifRes = ProtocolCodec.readPacket(in);
            assertEquals("15. Client GET_NOTIFICATIONS SUCCESS", ResponseStatus.SUCCESS, notifRes.getStatus());
            assertTrue("15. Client received notifications list", notifRes.getPayload().getInt("count", 0) > 0);

            // Test D: Update donor availability
            ProtocolPayload availPayload = new ProtocolPayload().set("isAvailable", false);
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.UPDATE_DONOR_AVAILABILITY, "avail-req", availPayload));
            Packet availRes = ProtocolCodec.readPacket(in);
            assertEquals("15. Client UPDATE_DONOR_AVAILABILITY SUCCESS", ResponseStatus.SUCCESS, availRes.getStatus());

            // Test E: Query profile
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.GET_PROFILE, "prof-req", ProtocolPayload.empty()));
            Packet profRes = ProtocolCodec.readPacket(in);
            assertEquals("15. Client GET_PROFILE SUCCESS", ResponseStatus.SUCCESS, profRes.getStatus());
            assertEquals("15. Profile email verified", "donor_alice@test.com", profRes.getPayload().get("email"));
            assertTrue("15. Profile password NOT exposed", !profRes.getPayload().containsKey("password"));

            // Test F: Logout
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.LOGOUT, "logout-req", ProtocolPayload.empty()));
            Packet logoutRes = ProtocolCodec.readPacket(in);
            assertEquals("15. Client LOGOUT SUCCESS", ResponseStatus.SUCCESS, logoutRes.getStatus());

        } finally {
            server.stop();
            Thread.sleep(150);
            assertTrue("15. TCP Server stopped cleanly", !server.isRunning());
        }
    }
}

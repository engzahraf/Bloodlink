package com.bloodlink.database;

import com.bloodlink.database.dao.*;
import com.bloodlink.database.enums.*;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.*;
import com.bloodlink.database.security.PasswordHasher;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive Automated Test Suite covering all 22 required database test cases.
 * Runs against an isolated test database file (test_bloodlink.db) to protect production data.
 */
public class DatabaseTestSuite {

    private static final String TEST_DB_PATH = "data" + File.separator + "test_bloodlink.db";

    private static final UserDAO userDAO = new UserDAO();
    private static final DonorProfileDAO donorDAO = new DonorProfileDAO();
    private static final BloodRequestDAO requestDAO = new BloodRequestDAO();
    private static final DonationDAO donationDAO = new DonationDAO();
    private static final NotificationDAO notificationDAO = new NotificationDAO();
    private static final MessageDAO messageDAO = new MessageDAO();

    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("     BLOODLINK DATABASE COMPREHENSIVE AUTOMATED TEST SUITE             ");
        System.out.println("========================================================================");

        try {
            // Setup isolated test database environment
            setupTestDatabase();

            // Run all 22 test cases
            test1_DatabaseConnection();
            test2_DatabaseInitialization();
            test3_TableCreation();
            test4_UserRegistration();
            test5_DuplicateEmailHandling();
            test6_UserRetrieval();
            test7_DonorProfileCreation();
            test8_DonorSearching();
            test9_DonorAvailabilityUpdates();
            test10_BloodRequestCreation();
            test11_RequestStatusChanges();
            test12_DonationRecording();
            test13_NotificationCreation();
            test14_NotificationRetrieval();
            test15_MessageStorage();
            test16_ConversationRetrieval();
            test17_ForeignKeyValidation();
            test18_InvalidDataHandling();
            test19_SqlInjectionPrevention();
            test20_ConcurrentDatabaseOperations();
            test21_TransactionRollback();
            test22_AccountBlocking();

        } catch (Throwable t) {
            System.err.println("Fatal unexpected error in test execution:");
            t.printStackTrace();
        } finally {
            cleanupTestDatabase();
            printTestSummary();
        }

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void setupTestDatabase() {
        System.out.println("[SETUP] Configuring isolated test database at: " + TEST_DB_PATH);
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        DatabaseConnection.setDatabasePath(TEST_DB_PATH);
        DatabaseInitializer.initialize();
    }

    private static void cleanupTestDatabase() {
        System.out.println("\n[TEARDOWN] Resetting active database path to default production configuration...");
        DatabaseConnection.resetToDefaultDatabasePath();
    }

    private static void assertCondition(String testName, boolean condition, String message) {
        if (condition) {
            System.out.printf("  [PASS] %-40s - %s%n", testName, message);
            passedCount++;
        } else {
            System.err.printf("  [FAIL] %-40s - %s%n", testName, message);
            failedCount++;
        }
    }

    // 1. Database connection
    private static void test1_DatabaseConnection() {
        String testName = "1. Database connection";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            assertCondition(testName, valid, "Successfully obtained valid SQLite connection");
        } catch (Exception e) {
            assertCondition(testName, false, "Connection exception: " + e.getMessage());
        }
    }

    // 2. Database initialization
    private static void test2_DatabaseInitialization() {
        String testName = "2. Database initialization";
        try {
            // Repeated initialization should be idempotent and safe
            DatabaseInitializer.initialize();
            assertCondition(testName, true, "Idempotent schema initialization executed without error");
        } catch (Exception e) {
            assertCondition(testName, false, "Initialization failed: " + e.getMessage());
        }
    }

    // 3. Table creation
    private static void test3_TableCreation() {
        String testName = "3. Table creation";
        String[] requiredTables = {"users", "donor_profiles", "blood_requests", "donations", "notifications", "messages"};
        boolean allExist = true;

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String table : requiredTables) {
                String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, table);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            allExist = false;
                            System.err.println("Missing table: " + table);
                        }
                    }
                }
            }
            assertCondition(testName, allExist, "All 6 required relational tables verified in SQLite catalog");
        } catch (Exception e) {
            assertCondition(testName, false, "Table verification exception: " + e.getMessage());
        }
    }

    // 4. User registration
    private static void test4_UserRegistration() {
        String testName = "4. User registration";
        try {
            String hash = PasswordHasher.hashPassword("Password@123");
            User newUser = new User("Alice Walker", "alice@example.com", hash, "555-0101", UserRole.DONOR);
            User created = userDAO.createUser(newUser);

            boolean passed = created != null && created.getUserId() > 0 && "Alice Walker".equals(created.getFullName());
            assertCondition(testName, passed, "User registered with generated ID: " + (created != null ? created.getUserId() : -1));
        } catch (Exception e) {
            assertCondition(testName, false, "User registration exception: " + e.getMessage());
        }
    }

    // 5. Duplicate email handling
    private static void test5_DuplicateEmailHandling() {
        String testName = "5. Duplicate email handling";
        try {
            String hash = PasswordHasher.hashPassword("Secret123");
            User duplicate = new User("Alice Impostor", "alice@example.com", hash, "555-0102", UserRole.DONOR);
            userDAO.createUser(duplicate);
            assertCondition(testName, false, "Expected ValidationException on duplicate email but insertion succeeded");
        } catch (ValidationException e) {
            assertCondition(testName, true, "Rejected duplicate email with ValidationException: " + e.getMessage());
        } catch (Exception e) {
            assertCondition(testName, false, "Unexpected exception type on duplicate: " + e.getClass().getName());
        }
    }

    // 6. User retrieval
    private static void test6_UserRetrieval() {
        String testName = "6. User retrieval";
        try {
            User byEmail = userDAO.getUserByEmail("alice@example.com");
            boolean okEmail = byEmail != null && "Alice Walker".equals(byEmail.getFullName());

            User byId = userDAO.getUserById(byEmail.getUserId());
            boolean okId = byId != null && byId.getUserId() == byEmail.getUserId();

            assertCondition(testName, okEmail && okId, "User successfully retrieved by both email and user_id");
        } catch (Exception e) {
            assertCondition(testName, false, "User retrieval error: " + e.getMessage());
        }
    }

    // 7. Donor profile creation
    private static void test7_DonorProfileCreation() {
        String testName = "7. Donor profile creation";
        try {
            User donorUser = userDAO.getUserByEmail("alice@example.com");
            DonorProfile profile = new DonorProfile(donorUser.getUserId(), BloodGroup.O_POSITIVE, "Colombo", AvailabilityStatus.AVAILABLE);
            DonorProfile created = donorDAO.createDonorProfile(profile);

            boolean passed = created != null && created.getDonorId() > 0 && created.getBloodGroup() == BloodGroup.O_POSITIVE;
            assertCondition(testName, passed, "Donor profile created with ID: " + (created != null ? created.getDonorId() : -1));
        } catch (Exception e) {
            assertCondition(testName, false, "Donor profile creation exception: " + e.getMessage());
        }
    }

    // 8. Donor searching
    private static void test8_DonorSearching() {
        String testName = "8. Donor searching";
        try {
            // Register a second donor with A_POSITIVE in Kandy
            String hash = PasswordHasher.hashPassword("Donor2Pass");
            User u2 = userDAO.createUser(new User("Bob Builder", "bob@example.com", hash, "555-0103", UserRole.DONOR));
            donorDAO.createDonorProfile(new DonorProfile(u2.getUserId(), BloodGroup.A_POSITIVE, "Kandy", AvailabilityStatus.AVAILABLE));

            List<DonorProfile> oDonors = donorDAO.searchDonorsByBloodGroup(BloodGroup.O_POSITIVE);
            boolean oFound = oDonors.stream().anyMatch(d -> "alice@example.com".equals(d.getDonorEmail()));

            List<DonorProfile> kandyDonors = donorDAO.searchDonorsByLocation("Kandy");
            boolean kandyFound = kandyDonors.stream().anyMatch(d -> "bob@example.com".equals(d.getDonorEmail()));

            // Compatible donors for A_POSITIVE (can receive from A+, A-, O+, O-)
            List<DonorProfile> compatible = donorDAO.searchCompatibleDonors(BloodGroup.A_POSITIVE, null);
            boolean compatibleFound = compatible.size() >= 2;

            assertCondition(testName, oFound && kandyFound && compatibleFound,
                    "Found by blood group, location, and medical compatibility rules");
        } catch (Exception e) {
            assertCondition(testName, false, "Donor searching error: " + e.getMessage());
        }
    }

    // 9. Donor availability updates
    private static void test9_DonorAvailabilityUpdates() {
        String testName = "9. Donor availability updates";
        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            DonorProfile profile = donorDAO.getDonorByUserId(alice.getUserId());

            boolean updated = donorDAO.updateAvailability(profile.getDonorId(), AvailabilityStatus.UNAVAILABLE);
            DonorProfile reloaded = donorDAO.getDonorById(profile.getDonorId());

            boolean passed = updated && reloaded.getAvailabilityStatus() == AvailabilityStatus.UNAVAILABLE;

            // Revert back for future tests
            donorDAO.updateAvailability(profile.getDonorId(), AvailabilityStatus.AVAILABLE);

            assertCondition(testName, passed, "Availability status toggled to UNAVAILABLE and verified");
        } catch (Exception e) {
            assertCondition(testName, false, "Availability update error: " + e.getMessage());
        }
    }

    // 10. Blood request creation
    private static void test10_BloodRequestCreation() {
        String testName = "10. Blood request creation";
        try {
            // Register a recipient
            String hash = PasswordHasher.hashPassword("RecipPass1");
            User recipient = userDAO.createUser(new User("Charlie Recipient", "charlie@example.com", hash, "555-0104", UserRole.RECIPIENT));

            BloodRequest req = new BloodRequest(
                recipient.getUserId(),
                BloodGroup.O_POSITIVE,
                2,
                "National Hospital",
                "Colombo",
                "555-0104",
                UrgencyLevel.CRITICAL,
                "Emergency surgery scheduled"
            );

            BloodRequest created = requestDAO.createBloodRequest(req);
            boolean passed = created != null && created.getRequestId() > 0 && created.getRequestStatus() == RequestStatus.PENDING;
            assertCondition(testName, passed, "Blood request created with ID: " + (created != null ? created.getRequestId() : -1));
        } catch (Exception e) {
            assertCondition(testName, false, "Blood request creation error: " + e.getMessage());
        }
    }

    // 11. Request status changes
    private static void test11_RequestStatusChanges() {
        String testName = "11. Request status changes";
        try {
            User charlie = userDAO.getUserByEmail("charlie@example.com");
            List<BloodRequest> reqs = requestDAO.getRequestsByRecipient(charlie.getUserId());
            BloodRequest req = reqs.get(0);

            boolean updated = requestDAO.updateRequestStatus(req.getRequestId(), RequestStatus.MATCHED);
            BloodRequest reloaded = requestDAO.getRequestById(req.getRequestId());

            boolean passed = updated && reloaded.getRequestStatus() == RequestStatus.MATCHED;
            assertCondition(testName, passed, "Request status transitioned from PENDING to MATCHED");
        } catch (Exception e) {
            assertCondition(testName, false, "Request status change error: " + e.getMessage());
        }
    }

    // 12. Donation recording
    private static void test12_DonationRecording() {
        String testName = "12. Donation recording";
        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            User charlie = userDAO.getUserByEmail("charlie@example.com");
            BloodRequest req = requestDAO.getRequestsByRecipient(charlie.getUserId()).get(0);

            Donation donation = new Donation(
                req.getRequestId(),
                alice.getUserId(),
                "2026-11-11",
                DonationStatus.COMPLETED,
                "Successful direct blood donation"
            );

            Donation recorded = donationDAO.recordDonation(donation);
            BloodRequest updatedReq = requestDAO.getRequestById(req.getRequestId());
            DonorProfile updatedDonor = donorDAO.getDonorByUserId(alice.getUserId());

            boolean passed = recorded != null && recorded.getDonationId() > 0
                    && updatedReq.getRequestStatus() == RequestStatus.COMPLETED
                    && "2026-11-11".equals(updatedDonor.getLastDonationDate());

            assertCondition(testName, passed, "Donation recorded and synchronized request/donor states atomically");
        } catch (Exception e) {
            assertCondition(testName, false, "Donation recording error: " + e.getMessage());
        }
    }

    // 13. Notification creation
    private static void test13_NotificationCreation() {
        String testName = "13. Notification creation";
        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            Notification n = new Notification(
                alice.getUserId(),
                NotificationType.BLOOD_REQUEST,
                "Urgent O+ blood needed at National Hospital!",
                1
            );

            Notification created = notificationDAO.createNotification(n);
            boolean passed = created != null && created.getNotificationId() > 0 && !created.isRead();
            assertCondition(testName, passed, "Notification created with ID: " + (created != null ? created.getNotificationId() : -1));
        } catch (Exception e) {
            assertCondition(testName, false, "Notification creation error: " + e.getMessage());
        }
    }

    // 14. Notification retrieval
    private static void test14_NotificationRetrieval() {
        String testName = "14. Notification retrieval";
        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            List<Notification> unread = notificationDAO.getUnreadNotifications(alice.getUserId());
            boolean hasUnread = !unread.isEmpty();

            int notifId = unread.get(0).getNotificationId();
            boolean marked = notificationDAO.markAsRead(notifId);
            Notification updated = notificationDAO.getNotificationById(notifId);

            boolean passed = hasUnread && marked && updated.isRead();
            assertCondition(testName, passed, "Unread notifications fetched and markAsRead confirmed");
        } catch (Exception e) {
            assertCondition(testName, false, "Notification retrieval error: " + e.getMessage());
        }
    }

    // 15. Message storage
    private static void test15_MessageStorage() {
        String testName = "15. Message storage";
        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            User charlie = userDAO.getUserByEmail("charlie@example.com");

            Message msg = new Message(alice.getUserId(), charlie.getUserId(), 1, "Hello Charlie, I can donate tomorrow.");
            Message saved = messageDAO.saveMessage(msg);

            boolean passed = saved != null && saved.getMessageId() > 0 && "Hello Charlie, I can donate tomorrow.".equals(saved.getMessageContent());
            assertCondition(testName, passed, "Direct message saved with ID: " + (saved != null ? saved.getMessageId() : -1));
        } catch (Exception e) {
            assertCondition(testName, false, "Message storage error: " + e.getMessage());
        }
    }

    // 16. Conversation retrieval
    private static void test16_ConversationRetrieval() {
        String testName = "16. Conversation retrieval";
        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            User charlie = userDAO.getUserByEmail("charlie@example.com");

            // Charlie replies back
            Message reply = new Message(charlie.getUserId(), alice.getUserId(), 1, "Thank you so much Alice, see you at 10 AM.");
            messageDAO.saveMessage(reply);

            List<Message> convo = messageDAO.getConversation(alice.getUserId(), charlie.getUserId());
            boolean passed = convo.size() >= 2;
            assertCondition(testName, passed, "Bi-directional conversation history retrieved: " + convo.size() + " messages");
        } catch (Exception e) {
            assertCondition(testName, false, "Conversation retrieval error: " + e.getMessage());
        }
    }

    // 17. Foreign key validation
    private static void test17_ForeignKeyValidation() {
        String testName = "17. Foreign key validation";
        try {
            // Attempt inserting a donor profile for a non-existent user_id 99999
            DonorProfile invalidProfile = new DonorProfile(99999, BloodGroup.AB_POSITIVE, "Galle", AvailabilityStatus.AVAILABLE);
            donorDAO.createDonorProfile(invalidProfile);
            assertCondition(testName, false, "Expected foreign key violation, but insertion succeeded");
        } catch (DatabaseException e) {
            boolean isFkError = e.getMessage() != null && e.getMessage().toLowerCase().contains("foreign key");
            assertCondition(testName, isFkError, "Foreign key constraint correctly blocked invalid reference");
        } catch (Exception e) {
            assertCondition(testName, false, "Unexpected exception: " + e.getMessage());
        }
    }

    // 18. Invalid data handling
    private static void test18_InvalidDataHandling() {
        String testName = "18. Invalid data handling";
        try {
            // Invalid units required <= 0
            BloodRequest invalidReq = new BloodRequest(1, BloodGroup.O_POSITIVE, 0, "Hospital", "Location", "123", UrgencyLevel.LOW, "Invalid");
            requestDAO.createBloodRequest(invalidReq);
            assertCondition(testName, false, "Expected ValidationException on zero units, but insertion succeeded");
        } catch (ValidationException e) {
            assertCondition(testName, true, "Rejected invalid data with ValidationException: " + e.getMessage());
        } catch (Exception e) {
            assertCondition(testName, false, "Unexpected exception: " + e.getMessage());
        }
    }

    // 19. SQL injection prevention
    private static void test19_SqlInjectionPrevention() {
        String testName = "19. SQL injection prevention";
        try {
            String maliciousEmail = "hacker@test.com' OR '1'='1";
            User user = userDAO.getUserByEmail(maliciousEmail);

            String searchPattern = "'; DROP TABLE users; --";
            List<User> searchResults = userDAO.searchUsers(searchPattern);

            // Table should still exist
            User alice = userDAO.getUserByEmail("alice@example.com");
            boolean safe = (user == null) && (alice != null);

            assertCondition(testName, safe, "Prepared statements prevented SQL injection attempt");
        } catch (Exception e) {
            assertCondition(testName, false, "SQL injection test error: " + e.getMessage());
        }
    }

    // 20. Concurrent database operations
    private static void test20_ConcurrentDatabaseOperations() {
        String testName = "20. Concurrent database operations";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCounter = new AtomicInteger(0);

        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            User charlie = userDAO.getUserByEmail("charlie@example.com");

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        // Concurrent message insert
                        Message m = new Message(alice.getUserId(), charlie.getUserId(), 1, "Concurrent test message " + idx);
                        messageDAO.saveMessage(m);

                        // Concurrent read
                        userDAO.getAllUsers();
                        donorDAO.searchAvailableDonors();

                        successCounter.incrementAndGet();
                    } catch (Exception e) {
                        System.err.println("Concurrency thread " + idx + " failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean finished = latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            boolean passed = finished && successCounter.get() == threadCount;
            assertCondition(testName, passed, "Executed " + successCounter.get() + "/" + threadCount + " simultaneous threads without locking errors");
        } catch (Exception e) {
            assertCondition(testName, false, "Concurrency test error: " + e.getMessage());
        }
    }

    // 21. Transaction rollback
    private static void test21_TransactionRollback() {
        String testName = "21. Transaction rollback";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            int initialCount;
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
                initialCount = rs.getInt(1);
            }

            // Insert a row inside transaction
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (full_name, email, password_hash, phone, role) VALUES (?, ?, ?, ?, ?)")) {
                stmt.setString(1, "Rollback Tester");
                stmt.setString(2, "rollback@test.com");
                stmt.setString(3, "dummyHash");
                stmt.setString(4, "000");
                stmt.setString(5, UserRole.RECIPIENT.name());
                stmt.executeUpdate();
            }

            // Explicitly Rollback
            conn.rollback();
            conn.setAutoCommit(true);

            // Verify count did not increase
            int postCount;
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
                postCount = rs.getInt(1);
            }

            boolean passed = (initialCount == postCount) && (userDAO.getUserByEmail("rollback@test.com") == null);
            assertCondition(testName, passed, "Transaction rolled back cleanly without persisting uncommitted changes");
        } catch (Exception e) {
            assertCondition(testName, false, "Transaction rollback error: " + e.getMessage());
        }
    }

    // 22. Account blocking
    private static void test22_AccountBlocking() {
        String testName = "22. Account blocking";
        try {
            User alice = userDAO.getUserByEmail("alice@example.com");
            boolean blocked = userDAO.updateAccountStatus(alice.getUserId(), AccountStatus.BLOCKED);
            User reloaded = userDAO.getUserById(alice.getUserId());

            boolean blockedOk = blocked && reloaded.getAccountStatus() == AccountStatus.BLOCKED && !reloaded.isActive();

            // Search available donors should no longer include blocked user
            List<DonorProfile> available = donorDAO.searchAvailableDonors();
            boolean excludedFromAvailable = available.stream().noneMatch(d -> d.getUserId() == alice.getUserId());

            // Unblock
            userDAO.updateAccountStatus(alice.getUserId(), AccountStatus.ACTIVE);

            boolean passed = blockedOk && excludedFromAvailable;
            assertCondition(testName, passed, "Account successfully blocked and excluded from active donor queries");
        } catch (Exception e) {
            assertCondition(testName, false, "Account blocking error: " + e.getMessage());
        }
    }

    private static void printTestSummary() {
        System.out.println("========================================================================");
        System.out.printf("TEST SUMMARY: Total: %d | Passed: %d | Failed: %d%n",
                passedCount + failedCount, passedCount, failedCount);
        System.out.println("========================================================================");
    }
}


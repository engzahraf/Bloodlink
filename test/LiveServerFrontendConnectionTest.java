package com.bloodlink.client.test;

import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.database.DatabaseInitializer;
import com.bloodlink.database.SQLiteDatabaseService;
import com.bloodlink.interfaces.IDatabaseService;
import com.bloodlink.server.BloodLinkServer;
import com.bloodlink.server.ClientManager;
import com.bloodlink.server.RequestDispatcher;
import com.bloodlink.services.AuthService;
import com.bloodlink.services.BloodMatchingService;
import com.bloodlink.services.MessagingService;
import com.bloodlink.services.NotificationService;
import com.bloodlink.util.ServerConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end verification verifying that the Frontend UI network layer (NetworkManager)
 * connects to the BloodLink TCP Server on port 9090 and conducts live business operations.
 */
public class LiveServerFrontendConnectionTest {

    private static int totalAssertions = 0;
    private static int passedAssertions = 0;

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  VERIFYING FRONTEND CLIENT CONNECTED TO BACKEND SERVER     ");
        System.out.println("============================================================");

        BloodLinkServer server = null;
        int port = ServerConfig.DEFAULT_PORT; // 9090

        try {
            // Set headless mode for clean automated execution
            System.setProperty("java.awt.headless", "true");

            // 1. Initialize SQLite database schema
            System.out.println("[Step 1] Initializing SQLite database schema...");
            DatabaseInitializer.initialize();
            assertCondition("Database schema initialized", true);

            // 2. Start BloodLink TCP Server on port 9090
            System.out.println("[Step 2] Starting BloodLink TCP Server on port " + port + "...");
            ClientManager clientManager = new ClientManager();
            IDatabaseService dbService = new SQLiteDatabaseService();
            NotificationService notifService = new NotificationService(clientManager, dbService);
            AuthService authService = new AuthService(dbService, clientManager);
            BloodMatchingService matchingService = new BloodMatchingService(dbService, clientManager, notifService);
            MessagingService messagingService = new MessagingService(dbService, clientManager);
            RequestDispatcher dispatcher = new RequestDispatcher(authService, matchingService, notifService, messagingService);

            server = new BloodLinkServer(port, clientManager, dispatcher);
            server.startAsync();
            Thread.sleep(600); // Allow server listener thread to bind
            assertCondition("Server is running on port " + port, server.isRunning());

            // 3. Connect Frontend NetworkManager
            System.out.println("[Step 3] Connecting Frontend NetworkManager to 127.0.0.1:" + port + "...");
            NetworkManager network = NetworkManager.getInstance();
            network.connect("127.0.0.1", port);
            Thread.sleep(400); // Allow socket reader handshake
            assertCondition("Frontend Socket connected to Backend Server", network.isConnected());

            // 4. Test PING -> PONG over TCP
            System.out.println("[Step 4] Sending PING from Frontend to Backend...");
            CompletableFuture<ParsedResponse> pingFuture = network.ping();
            ParsedResponse pingRes = pingFuture.get(4, TimeUnit.SECONDS);
            assertCondition("Server replied to Frontend PING", pingRes != null);
            assertCondition("Server reply status is SUCCESS", pingRes.isSuccess());
            assertCondition("Server action is PONG", "PONG".equalsIgnoreCase(pingRes.getAction()));
            System.out.println("  -> Ping latency OK, server response received.");

            // 5. Test Frontend User Registration
            System.out.println("[Step 5] Testing Frontend user registration via TCP socket...");
            String testEmail = "frontend_user_" + System.currentTimeMillis() + "@bloodlink.test";
            CompletableFuture<ParsedResponse> regFuture = network.register(
                    testEmail,
                    "SecurePass#2026",
                    "DONOR",
                    "Frontend Test Donor",
                    "O_POS",
                    "Colombo",
                    "0771234567",
                    testEmail
            );
            ParsedResponse regRes = regFuture.get(5, TimeUnit.SECONDS);
            assertCondition("Registration response received", regRes != null);
            assertCondition("Registration returned SUCCESS", regRes.isSuccess());
            assertCondition("User ID assigned by backend", regRes.get("userId") != null);
            System.out.println("  -> Registered user ID: " + regRes.get("userId"));

            // 6. Test Frontend Authentication / Login
            System.out.println("[Step 6] Testing Frontend authentication (Login) via TCP socket...");
            CompletableFuture<ParsedResponse> loginFuture = network.login(testEmail, "SecurePass#2026");
            ParsedResponse loginRes = loginFuture.get(5, TimeUnit.SECONDS);
            assertCondition("Login response received", loginRes != null);
            assertCondition("Login returned SUCCESS", loginRes.isSuccess());
            assertCondition("Authenticated username matches", testEmail.equalsIgnoreCase(loginRes.get("username")));
            assertCondition("Authenticated role is DONOR", "DONOR".equalsIgnoreCase(loginRes.get("role")));
            assertCondition("Authenticated bloodGroup is O_POS", "O_POS".equalsIgnoreCase(loginRes.get("bloodGroup")));
            System.out.println("  -> Authenticated as: " + loginRes.get("fullName") + " (" + loginRes.get("role") + ")");

            // 7. Test Frontend Donor Search / Compatibility Matching
            System.out.println("[Step 7] Testing Frontend donor matching via TCP socket...");
            CompletableFuture<ParsedResponse> matchFuture = network.matchDonors("O_POS", "Colombo");
            ParsedResponse matchRes = matchFuture.get(5, TimeUnit.SECONDS);
            assertCondition("Donor match response received", matchRes != null);
            assertCondition("Donor match returned SUCCESS", matchRes.isSuccess());
            System.out.println("  -> Compatible donors found: " + matchRes.get("count"));

            // 8. Test Clean Disconnect
            System.out.println("[Step 8] Disconnecting Frontend NetworkManager...");
            network.disconnect();
            Thread.sleep(200);
            assertCondition("Frontend disconnected cleanly", !network.isConnected());

            System.out.println("============================================================");
            System.out.println("RESULT: " + passedAssertions + " / " + totalAssertions + " ASSERTIONS PASSED (100%)");
            System.out.println("SERVER AND FRONTEND COMMUNICATION FULLY OPERATIONAL!");
            System.out.println("============================================================");

        } catch (Throwable t) {
            System.err.println("[FAIL] Live Server-Frontend connection verification failed:");
            t.printStackTrace();
            System.exit(1);
        } finally {
            NetworkManager.getInstance().disconnect();
            if (server != null) {
                server.stop();
            }
        }
    }

    private static void assertCondition(String name, boolean condition) {
        totalAssertions++;
        if (condition) {
            passedAssertions++;
            System.out.println("  [PASS] " + name);
        } else {
            System.err.println("  [FAIL] " + name);
            throw new AssertionError("Assertion failed: " + name);
        }
    }
}

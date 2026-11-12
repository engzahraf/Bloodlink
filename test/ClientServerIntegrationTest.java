package com.bloodlink.client.test;

import com.bloodlink.client.network.RequestBuilder;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.client.network.SocketClient;
import com.bloodlink.server.BloodLinkServer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * End-to-end integration test validating TCP Socket communication
 * between Member 01's SocketClient and Member 02's BloodLinkServer.
 */
public class ClientServerIntegrationTest {

    private static final int TEST_PORT = 5055;
    private static int totalTests = 0;
    private static int passedTests = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  BLOODLINK CLIENT-SERVER LIVE TCP TEST SUITE     ");
        System.out.println("==================================================");

        BloodLinkServer server = null;
        SocketClient client = null;

        try {
            // Set headless mode for server test
            System.setProperty("java.awt.headless", "true");

            // Initialize database schema first
            com.bloodlink.database.DatabaseInitializer.initialize();

            // Wire up dependencies for BloodLink Server
            com.bloodlink.server.ClientManager clientManager = new com.bloodlink.server.ClientManager();
            com.bloodlink.interfaces.IDatabaseService dbService = new com.bloodlink.database.SQLiteDatabaseService();
            com.bloodlink.services.NotificationService notifService = new com.bloodlink.services.NotificationService(clientManager, dbService);
            com.bloodlink.services.AuthService authService = new com.bloodlink.services.AuthService(dbService, clientManager);
            com.bloodlink.services.BloodMatchingService matchingService = new com.bloodlink.services.BloodMatchingService(dbService, clientManager, notifService);
            com.bloodlink.services.MessagingService messagingService = new com.bloodlink.services.MessagingService(dbService, clientManager);

            com.bloodlink.server.RequestDispatcher dispatcher = new com.bloodlink.server.RequestDispatcher(authService, matchingService, notifService, messagingService);

            // 1. Start Server on Port 5055
            System.out.println("[Server] Starting BloodLink TCP Server on port " + TEST_PORT + "...");
            server = new BloodLinkServer(TEST_PORT, clientManager, dispatcher);
            server.startAsync();
            Thread.sleep(800); // Allow server to bind

            // 2. Connect Client
            System.out.println("[Client] Connecting SocketClient to 127.0.0.1:" + TEST_PORT + "...");
            BlockingQueue<String> incomingPackrs = new LinkedBlockingQueue<>();
            CountDownLatch connectLatch = new CountDownLatch(1);

            client = new SocketClient("127.0.0.1", TEST_PORT, new SocketClient.SocketListener() {
                @Override
                public void onPacketReceived(String rawWireString) {
                    incomingPackrs.offer(rawWireString);
                }

                @Override
                public void onConnected() {
                    connectLatch.countDown();
                }

                @Override
                public void onDisconnected(String reason) {
                }

                @Override
                public void onError(Throwable t) {
                }
            });

            client.connect();
            boolean connected = connectLatch.await(3, TimeUnit.SECONDS);
            assertTrue("Client TCP socket handshake connected", connected && client.isConnected());

            // 3. Test Ping -> Pong
            System.out.println("\n[Test 1] Testing TCP PING/PONG...");
            String pingWire = RequestBuilder.buildPing("ping_101");
            client.send(pingWire);

            String pongRaw = incomingPackrs.poll(3, TimeUnit.SECONDS);
            assertTrue("Received response packet for PING", pongRaw != null);
            ParsedResponse pongRes = ResponseHandler.parseWirePacket(pongRaw);
            assertTrue("PONG action matches", "PONG".equals(pongRes.getAction()));
            assertTrue("PONG status is SUCCESS", pongRes.isSuccess());
            assertTrue("PONG request ID matches", "ping_101".equals(pongRes.getRequestId()));

            // 4. Test User Registration via Socket
            System.out.println("\n[Test 2] Testing User Registration over TCP Socket...");
            String testEmail = "donor_" + System.currentTimeMillis() + "@bloodlink.test";
            String regWire = RequestBuilder.buildRegisterRequest(
                    "reg_102",
                    testEmail,
                    "password123",
                    "DONOR",
                    "Integration Test Donor",
                    "O_POS",
                    "Colombo",
                    "0771122334",
                    testEmail
            );
            client.send(regWire);

            String regRaw = incomingPackrs.poll(4, TimeUnit.SECONDS);
            assertTrue("Received registration response from server", regRaw != null);
            ParsedResponse regRes = ResponseHandler.parseWirePacket(regRaw);
            System.out.println("[Client] Registration response payload: " + regRes.getPayload());
            assertTrue("Registration request succeeded", regRes.isSuccess());
            assertTrue("User ID returned by server", regRes.get("userId") != null);

            // 5. Test Authentication / Login via Socket
            System.out.println("\n[Test 3] Testing Authentication over TCP Socket...");
            String loginWire = RequestBuilder.buildLoginRequest("login_103", testEmail, "password123");
            client.send(loginWire);

            String loginRaw = incomingPackrs.poll(4, TimeUnit.SECONDS);
            assertTrue("Received login response from server", loginRaw != null);
            ParsedResponse loginRes = ResponseHandler.parseWirePacket(loginRaw);
            assertTrue("Login authentication succeeded", loginRes.isSuccess());
            assertTrue("Returned username/email matches authenticated user", testEmail.equalsIgnoreCase(loginRes.get("username")) || testEmail.equalsIgnoreCase(loginRes.get("email")));
            assertTrue("Returned role matches DONOR", "DONOR".equals(loginRes.get("role")));

            // 6. Test Donor Search / Matching via Socket
            System.out.println("\n[Test 4] Testing Donor Matching over TCP Socket...");
            String matchWire = RequestBuilder.buildMatchDonors("match_104", "O_POS", "Colombo");
            client.send(matchWire);

            String matchRaw = incomingPackrs.poll(4, TimeUnit.SECONDS);
            assertTrue("Received donor match response from server", matchRaw != null);
            ParsedResponse matchRes = ResponseHandler.parseWirePacket(matchRaw);
            assertTrue("Donor match query succeeded", matchRes.isSuccess());

            System.out.println("\n==================================================");
            System.out.println("INTEGRATION RESULTS: " + passedTests + " / " + totalTests + " PASSED (100%)");
            System.out.println("==================================================");

        } catch (Throwable t) {
            System.err.println("INTEGRATION TEST FAILED WITH EXCEPTION:");
            t.printStackTrace();
            System.exit(1);
        } finally {
            if (client != null) {
                client.disconnect();
            }
            if (server != null) {
                server.stop();
            }
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
}

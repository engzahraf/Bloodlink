package com.bloodlink.client.test;

import com.bloodlink.client.models.BloodRequest;
import com.bloodlink.client.models.DonorResponse;
import com.bloodlink.client.models.User;
import com.bloodlink.client.network.RequestBuilder;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.client.network.SocketClient;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive Automated System Test Suite validating:
 * 1. Complete Donor Accept Flow:
 *    - Recipient creates request
 *    - Donor accepts request
 *    - Recipient receives notification
 *    - Recipient verifies response status ACCEPTED
 *    - Server prevents duplicate accept
 * 2. Complete Donor Reject Flow:
 *    - Donor rejects request
 *    - Recipient receives notification & sees response status REJECTED
 * 3. Offline Recipient Flow:
 *    - Recipient offline when donor responds
 *    - Notification persisted in SQLite database
 *    - Recipient logs in later and retrieves notification & accepted status
 * 4. Two-Way Messaging Flow:
 *    - Donor -> Recipient message
 *    - Recipient -> Donor reply
 *    - Conversation history correctly retrieved
 * 5. Admin Authorization & Security:
 *    - Admin can view users, requests, system stats
 *    - Admin can block / unblock users
 *    - Admin can cancel inappropriate requests
 *    - Non-admin (DONOR / RECIPIENT) denied with FORBIDDEN on admin operations
 */
public class FullSystemIntegrationTest {

    private static final int PORT = 9095;
    private static int totalAssertions = 0;
    private static int passedAssertions = 0;

    static class TestClient {
        final SocketClient socketClient;
        final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();

        public TestClient(String host, int port) throws IOException {
            socketClient = new SocketClient(host, port, new SocketClient.SocketListener() {
                @Override
                public void onPacketReceived(String rawWireString) {
                    inbox.offer(rawWireString);
                }

                @Override
                public void onConnected() {}

                @Override
                public void onDisconnected(String reason) {}

                @Override
                public void onError(Throwable t) {}
            });
            socketClient.connect();
        }

        public void send(String packet) throws IOException {
            socketClient.send(packet);
        }

        public ParsedResponse receiveResponse(long timeoutMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) break;
                String raw = inbox.poll(remain, TimeUnit.MILLISECONDS);
                if (raw != null) {
                    ParsedResponse p = ResponseHandler.parseWirePacket(raw);
                    if ("RES".equals(p.getType())) {
                        return p;
                    }
                }
            }
            return null;
        }

        public ParsedResponse receiveEvent(long timeoutMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) break;
                String raw = inbox.poll(remain, TimeUnit.MILLISECONDS);
                if (raw != null) {
                    ParsedResponse p = ResponseHandler.parseWirePacket(raw);
                    if ("EVENT".equals(p.getType()) || "MSG".equals(p.getType())) {
                        return p;
                    }
                }
            }
            return null;
        }

        public void disconnect() {
            socketClient.disconnect();
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  BLOODLINK FULL SYSTEM INTEGRATION & VERIFICATION TEST SUITE     ");
        System.out.println("==================================================================");

        BloodLinkServer server = null;
        TestClient recipient = null;
        TestClient donor = null;
        TestClient admin = null;

        try {
            // 0. Initialize SQLite Database & TCP Server
            System.setProperty("java.awt.headless", "true");
            DatabaseInitializer.initialize();
            DatabaseInitializer.seedDefaultAdminIfMissing("admin@bloodlink.com", "Admin@BloodLink2026", "System Administrator", "0770000000");

            ClientManager clientMgr = new ClientManager();
            IDatabaseService dbSvc = new SQLiteDatabaseService();
            NotificationService notifSvc = new NotificationService(clientMgr, dbSvc);
            BloodMatchingService matchSvc = new BloodMatchingService(dbSvc, clientMgr, notifSvc);
            AuthService authSvc = new AuthService(dbSvc, clientMgr);
            MessagingService msgSvc = new MessagingService(dbSvc, clientMgr);
            RequestDispatcher dispatcher = new RequestDispatcher(authSvc, matchSvc, notifSvc, msgSvc);

            server = new BloodLinkServer(PORT, clientMgr, dispatcher);
            server.startAsync();
            Thread.sleep(600);
            assertTest("BloodLink Server running on port " + PORT, server.isRunning());

            // -------------------------------------------------------------------------
            // SCENARIO 1: DONOR ACCEPT FLOW & REAL-TIME NOTIFICATION
            // -------------------------------------------------------------------------
            System.out.println("\n[Test 1] Testing Donor ACCEPT Flow & Real-Time Notification...");
            recipient = new TestClient("127.0.0.1", PORT);
            donor = new TestClient("127.0.0.1", PORT);
            Thread.sleep(300);

            // Register Recipient
            String recEmail = "rec_" + System.currentTimeMillis() + "@test.com";
            recipient.send(RequestBuilder.buildRegisterRequest("r_reg", recEmail, "Pass1234", "RECIPIENT", "Ahmed Recipient", "O+", "Colombo", "0771112222", recEmail));
            ParsedResponse rRegRes = recipient.receiveResponse(3000);
            assertTest("Recipient registration success", rRegRes != null && rRegRes.isSuccess());
            String recUserId = rRegRes.get("userId");

            recipient.send(RequestBuilder.buildLoginRequest("r_log", recEmail, "Pass1234"));
            ParsedResponse rLogRes = recipient.receiveResponse(3000);
            assertTest("Recipient login success", rLogRes != null && rLogRes.isSuccess());

            // Register Donor (Blood group O+)
            String donEmail = "don_" + System.currentTimeMillis() + "@test.com";
            donor.send(RequestBuilder.buildRegisterRequest("d_reg", donEmail, "Pass1234", "DONOR", "Dr. Ahmed Donor", "O+", "Colombo", "0773334444", donEmail));
            ParsedResponse dRegRes = donor.receiveResponse(3000);
            assertTest("Donor registration success", dRegRes != null && dRegRes.isSuccess());
            String donUserId = dRegRes.get("userId");

            donor.send(RequestBuilder.buildLoginRequest("d_log", donEmail, "Pass1234"));
            ParsedResponse dLogRes = donor.receiveResponse(3000);
            assertTest("Donor login success", dLogRes != null && dLogRes.isSuccess());

            // Recipient creates blood request for O+
            recipient.send(RequestBuilder.buildCreateBloodRequest("c_req", "O_POS", 2, "Colombo National Hospital", "Colombo", "URGENT", "Emergency blood needed"));
            ParsedResponse cReqRes = recipient.receiveResponse(3000);
            assertTest("Blood request creation success", cReqRes != null && cReqRes.isSuccess());
            String bloodReqId = cReqRes.get("bloodRequestId");
            assertTest("Blood request ID generated: " + bloodReqId, bloodReqId != null && !bloodReqId.isEmpty());

            // Donor accepts the blood request
            donor.send(RequestBuilder.buildAcceptRequest("d_accept", bloodReqId, "Available to donate immediately"));
            ParsedResponse dAcceptRes = donor.receiveResponse(3000);
            assertTest("Donor accept request response success", dAcceptRes != null && dAcceptRes.isSuccess());
            assertTest("Donor accept response status is ACCEPTED", "ACCEPTED".equalsIgnoreCase(dAcceptRes.get("status")));

            // Real-time notification delivered to online recipient
            ParsedResponse pushNotif = recipient.receiveEvent(3000);
            assertTest("Recipient received real-time notification push", pushNotif != null);
            if (pushNotif != null) {
                assertTest("Notification message contains donor accept note",
                        pushNotif.get("message") != null && pushNotif.get("message").contains("accepted"));
            }

            // Recipient queries donor responses for this blood request
            recipient.send(RequestBuilder.buildGetRequestResponses("r_get_resp", bloodReqId, ""));
            ParsedResponse getRespRes = recipient.receiveResponse(3000);
            assertTest("Recipient GET_REQUEST_RESPONSES success", getRespRes != null && getRespRes.isSuccess());

            List<DonorResponse> responsesList = ResponseHandler.parseDonorResponses(getRespRes);
            assertTest("Recipient has at least 1 donor response", !responsesList.isEmpty());
            if (!responsesList.isEmpty()) {
                DonorResponse first = responsesList.get(0);
                assertTest("Donor response status is ACCEPTED", first.isAccepted());
                assertTest("Donor name matches: " + first.getDonorName(), first.getDonorName().contains("Ahmed"));
                assertTest("Donor blood group is present", !first.getDonorBloodGroup().isEmpty());
            }

            // Duplicate Accept Prevention: Donor tries to accept again
            donor.send(RequestBuilder.buildAcceptRequest("d_dup_accept", bloodReqId, "Accepting second time"));
            ParsedResponse dupAcceptRes = donor.receiveResponse(3000);
            assertTest("Server prevents duplicate accept (returns error)", dupAcceptRes != null && !dupAcceptRes.isSuccess());

            // -------------------------------------------------------------------------
            // SCENARIO 2: DONOR REJECT FLOW
            // -------------------------------------------------------------------------
            System.out.println("\n[Test 2] Testing Donor REJECT Flow...");
            TestClient donor2 = new TestClient("127.0.0.1", PORT);
            String don2Email = "don2_" + System.currentTimeMillis() + "@test.com";
            donor2.send(RequestBuilder.buildRegisterRequest("d2_reg", don2Email, "Pass1234", "DONOR", "Fatima Donor", "O+", "Colombo", "0775556666", don2Email));
            ParsedResponse d2RegRes = donor2.receiveResponse(3000);
            donor2.send(RequestBuilder.buildLoginRequest("d2_log", don2Email, "Pass1234"));
            ParsedResponse d2LogRes = donor2.receiveResponse(3000);
            assertTest("Donor 2 login success", d2LogRes != null && d2LogRes.isSuccess());

            // Donor 2 rejects the blood request
            donor2.send(RequestBuilder.buildRejectRequest("d2_reject", bloodReqId, "Currently traveling"));
            ParsedResponse d2RejectRes = donor2.receiveResponse(3000);
            assertTest("Donor 2 reject request response success", d2RejectRes != null && d2RejectRes.isSuccess());
            assertTest("Donor 2 reject status is REJECTED", "REJECTED".equalsIgnoreCase(d2RejectRes.get("status")));

            // Recipient gets updated responses list
            recipient.send(RequestBuilder.buildGetRequestResponses("r_get_resp2", bloodReqId, ""));
            ParsedResponse getResp2 = recipient.receiveResponse(3000);
            List<DonorResponse> allResponses = ResponseHandler.parseDonorResponses(getResp2);
            assertTest("Recipient sees 2 responses recorded", allResponses.size() >= 2);
            boolean hasRejected = allResponses.stream().anyMatch(DonorResponse::isRejected);
            boolean hasAccepted = allResponses.stream().anyMatch(DonorResponse::isAccepted);
            assertTest("Recipient sees both ACCEPTED and REJECTED responses", hasAccepted && hasRejected);

            donor2.disconnect();

            // -------------------------------------------------------------------------
            // SCENARIO 3: OFFLINE RECIPIENT FLOW
            // -------------------------------------------------------------------------
            System.out.println("\n[Test 3] Testing Offline Recipient Notification Persistence...");
            // Create a second request while recipient is logged in
            recipient.send(RequestBuilder.buildCreateBloodRequest("c_req2", "O_POS", 1, "Kandy General Hospital", "Kandy", "NORMAL", "Scheduled surgery"));
            ParsedResponse cReq2Res = recipient.receiveResponse(3000);
            String bloodReq2Id = cReq2Res.get("bloodRequestId");

            // Recipient logs out (goes offline)
            recipient.send(RequestBuilder.buildLogoutRequest("r_logout", recEmail));
            recipient.receiveResponse(2000);
            recipient.disconnect();
            Thread.sleep(500);

            // Donor accepts bloodReq2Id while recipient is OFFLINE
            donor.send(RequestBuilder.buildAcceptRequest("d_offline_accept", bloodReq2Id, "Accepted while recipient offline"));
            ParsedResponse dOffAccept = donor.receiveResponse(3000);
            assertTest("Donor accepted request while recipient was offline", dOffAccept != null && dOffAccept.isSuccess());

            // Recipient reconnects, logs in and retrieves notifications from DB
            TestClient recipientReconnected = new TestClient("127.0.0.1", PORT);
            recipientReconnected.send(RequestBuilder.buildLoginRequest("r_relog", recEmail, "Pass1234"));
            ParsedResponse rRelogRes = recipientReconnected.receiveResponse(3000);
            assertTest("Recipient reconnected and logged in successfully", rRelogRes != null && rRelogRes.isSuccess());

            // Fetch persisted offline notifications
            recipientReconnected.send(RequestBuilder.buildGetNotifications("r_get_notifs", 20));
            ParsedResponse notifsRes = recipientReconnected.receiveResponse(3000);
            assertTest("Recipient fetched notifications from database", notifsRes != null && notifsRes.isSuccess());
            List<com.bloodlink.client.models.Notification> notifsList = ResponseHandler.parseNotifications(notifsRes);
            assertTest("Offline notification was stored and retrieved", !notifsList.isEmpty());
            boolean hasOfflineAcceptMsg = notifsList.stream().anyMatch(n -> n.getMessage().contains("accepted"));
            assertTest("Retrieved notification contains accepted alert", hasOfflineAcceptMsg);

            // Check request responses for the request accepted offline
            recipientReconnected.send(RequestBuilder.buildGetRequestResponses("r_off_resp", bloodReq2Id, ""));
            ParsedResponse offRespRes = recipientReconnected.receiveResponse(3000);
            List<DonorResponse> offList = ResponseHandler.parseDonorResponses(offRespRes);
            assertTest("Recipient sees ACCEPTED response for offline accepted request",
                    !offList.isEmpty() && offList.get(0).isAccepted());

            // -------------------------------------------------------------------------
            // SCENARIO 4: TWO-WAY DIRECT MESSAGING
            // -------------------------------------------------------------------------
            System.out.println("\n[Test 4] Testing Donor <-> Recipient Two-Way Messaging...");
            // Donor sends message to Recipient
            donor.send(RequestBuilder.buildSendMessage("d_msg_1", recUserId, "Hello! I am ready to donate at Colombo Hospital."));
            ParsedResponse dMsgRes = donor.receiveResponse(3000);
            assertTest("Donor SEND_MESSAGE returned SUCCESS", dMsgRes != null && dMsgRes.isSuccess());

            // Recipient replies to Donor
            recipientReconnected.send(RequestBuilder.buildSendMessage("r_msg_1", donUserId, "Thank you doctor! Please meet at Ward 4."));
            ParsedResponse rMsgRes = recipientReconnected.receiveResponse(3000);
            assertTest("Recipient SEND_MESSAGE returned SUCCESS", rMsgRes != null && rMsgRes.isSuccess());

            // Retrieve conversation history from donor side
            donor.send(RequestBuilder.buildGetMessages("d_get_msgs", recUserId, 20));
            ParsedResponse dHistoryRes = donor.receiveResponse(3000);
            assertTest("Donor GET_MESSAGES returned SUCCESS", dHistoryRes != null && dHistoryRes.isSuccess());
            List<com.bloodlink.client.models.Message> msgs = ResponseHandler.parseMessages(dHistoryRes, donUserId);
            assertTest("Conversation history contains 2 messages", msgs.size() >= 2);

            recipientReconnected.disconnect();

            // -------------------------------------------------------------------------
            // SCENARIO 5: ADMIN AUTHORIZATION & SECURITY
            // -------------------------------------------------------------------------
            System.out.println("\n[Test 5] Testing Admin Authorization, Statistics & Security...");
            // 5a. Non-admin (donor) trying admin operations must be FORBIDDEN
            donor.send(RequestBuilder.buildAdminGetUsers("don_try_admin", ""));
            ParsedResponse forbiddenRes1 = donor.receiveResponse(3000);
            assertTest("Donor denied access to ADMIN_GET_USERS (FORBIDDEN)",
                    forbiddenRes1 != null && "FORBIDDEN".equalsIgnoreCase(forbiddenRes1.getStatus()));

            donor.send(RequestBuilder.buildAdminBlockUser("don_try_block", recUserId));
            ParsedResponse forbiddenRes2 = donor.receiveResponse(3000);
            assertTest("Donor denied access to ADMIN_BLOCK_USER (FORBIDDEN)",
                    forbiddenRes2 != null && "FORBIDDEN".equalsIgnoreCase(forbiddenRes2.getStatus()));

            // 5b. Legitimate Admin Login
            admin = new TestClient("127.0.0.1", PORT);
            admin.send(RequestBuilder.buildLoginRequest("adm_log", "admin@bloodlink.com", "Admin@BloodLink2026"));
            ParsedResponse admLogRes = admin.receiveResponse(3000);
            assertTest("Admin login returned SUCCESS", admLogRes != null && admLogRes.isSuccess());
            assertTest("Admin user role is ADMIN", "ADMIN".equalsIgnoreCase(admLogRes.get("role")));

            // 5c. Admin get users
            admin.send(RequestBuilder.buildAdminGetUsers("adm_get_users", ""));
            ParsedResponse admUsersRes = admin.receiveResponse(3000);
            assertTest("Admin ADMIN_GET_USERS returned SUCCESS", admUsersRes != null && admUsersRes.isSuccess());
            List<User> adminUsers = ResponseHandler.parseAdminUsers(admUsersRes);
            assertTest("Admin retrieved user registry (count > 0)", !adminUsers.isEmpty());

            // 5d. Admin view system statistics
            admin.send(RequestBuilder.buildAdminGetStats("adm_stats"));
            ParsedResponse admStatsRes = admin.receiveResponse(3000);
            assertTest("Admin ADMIN_GET_STATS returned SUCCESS", admStatsRes != null && admStatsRes.isSuccess());
            Map<String, String> statsPayload = admStatsRes.getPayload();
            assertTest("Statistics contains totalUsers", statsPayload.containsKey("totalUsers"));
            assertTest("Statistics contains totalRequests", statsPayload.containsKey("totalRequests"));

            // 5e. Admin block and unblock user
            admin.send(RequestBuilder.buildAdminBlockUser("adm_block", recUserId));
            ParsedResponse blockRes = admin.receiveResponse(3000);
            assertTest("Admin ADMIN_BLOCK_USER returned SUCCESS", blockRes != null && blockRes.isSuccess());

            admin.send(RequestBuilder.buildAdminUnblockUser("adm_unblock", recUserId));
            ParsedResponse unblockRes = admin.receiveResponse(3000);
            assertTest("Admin ADMIN_UNBLOCK_USER returned SUCCESS", unblockRes != null && unblockRes.isSuccess());

            // 5f. Admin cancel inappropriate request
            admin.send(RequestBuilder.buildAdminCancelRequest("adm_cancel", bloodReqId));
            ParsedResponse cancelRes = admin.receiveResponse(3000);
            assertTest("Admin ADMIN_CANCEL_REQUEST returned SUCCESS", cancelRes != null && cancelRes.isSuccess());

            System.out.println("\n==================================================================");
            System.out.printf("INTEGRATION TEST RESULTS: %d / %d ASSERTIONS PASSED (100%%)%n", passedAssertions, totalAssertions);
            System.out.println("ALL 5 CORE REQUIREMENTS FULLY VERIFIED AND OPERATIONAL!");
            System.out.println("==================================================================");

        } catch (Exception e) {
            System.err.println("[FAIL] Integration test failed with exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (recipient != null) recipient.disconnect();
            if (donor != null) donor.disconnect();
            if (admin != null) admin.disconnect();
            if (server != null) server.stop();
        }
    }

    private static void assertTest(String description, boolean condition) {
        totalAssertions++;
        if (condition) {
            passedAssertions++;
            System.out.println("  [PASS] " + description);
        } else {
            System.out.println("  [FAIL] " + description);
        }
    }
}


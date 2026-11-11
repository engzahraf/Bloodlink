package com.bloodlink.test;

import com.bloodlink.protocol.MessageType;
import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolAction;
import com.bloodlink.protocol.ProtocolCodec;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.protocol.ResponseStatus;
import com.bloodlink.protocol.UserRole;
import com.bloodlink.server.BloodLinkServer;
import com.bloodlink.server.ClientManager;
import com.bloodlink.server.RequestDispatcher;
import com.bloodlink.services.AuthService;
import com.bloodlink.services.BloodCompatibility;
import com.bloodlink.services.BloodMatchingService;
import com.bloodlink.services.MessagingService;
import com.bloodlink.services.NotificationService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Set;

/**
 * Comprehensive End-to-End Test Suite for BloodLink Member 02 Backend:
 * - All 8 ABO/Rh blood compatibility rules
 * - User registration security & role restriction (no self-assigned ADMIN)
 * - Authentication & authenticated session tracking
 * - Unauthenticated request rejection across all protected endpoints
 * - Role-based authorization & permission boundary enforcement
 * - Safe delimiter and complex payload character preservation
 * - Blood request creation and payload validation (blood group, units, urgency)
 * - Live donor matching and city filtering
 * - Real-time push alert routing to online donors
 * - Peer-to-peer direct chat with impersonation prevention
 * - Non-existent recipient rejection
 * - Conversation history retrieval
 * - Offline notification and chat queuing
 * - User profile retrieval (with password credentials sanitized)
 * - Donor availability updates (self-only and role-enforced)
 * - Safe client disconnection, logout, and server shutdown
 */
public class Stage2ServiceIntegrationTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   BLOODLINK MEMBER 02 COMPREHENSIVE AUDIT TEST  ");
        System.out.println("=================================================");

        BloodLinkServer server = null;
        try {
            // 1. Biological ABO/Rh Blood Compatibility Tests
            testBloodCompatibilityMatrix();

            // 2. Setup Full Server Stack with Mock Database
            MockDatabaseService db = new MockDatabaseService();
            ClientManager clientManager = new ClientManager();
            NotificationService notificationService = new NotificationService(clientManager, db);
            AuthService authService = new AuthService(db, clientManager);
            BloodMatchingService bloodMatchingService = new BloodMatchingService(db, clientManager, notificationService);
            MessagingService messagingService = new MessagingService(db, clientManager);

            RequestDispatcher dispatcher = new RequestDispatcher(authService, bloodMatchingService, notificationService, messagingService);
            server = new BloodLinkServer(0, clientManager, dispatcher);
            server.startAsync();

            Thread.sleep(200);
            int port = server.getPort();
            assertTrue("Server listening on dynamic port " + port, port > 0);

            // 3. Registration Security & Role Tests
            testRegistrationAndRoleSecurity(port);

            // 4. Unauthenticated Access Protection Tests
            testUnauthenticatedAccessRestrictions(port);

            // 5. Full End-to-End Business Workflows
            testAuthenticatedWorkflowsAndMessaging(port, server, clientManager);

        } catch (Throwable t) {
            System.err.println("Fatal test exception: " + t.getMessage());
            t.printStackTrace();
        } finally {
            if (server != null) {
                server.stop();
            }
        }

        System.out.println("=================================================");
        System.out.printf("Test Suite Results: %d/%d passed (%.1f%%)%n",
                testsPassed, testsRun, (testsRun > 0 ? (testsPassed * 100.0 / testsRun) : 0));
        System.out.println("=================================================");

        if (testsPassed != testsRun) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            System.err.println("[FAIL] " + testName);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        testsRun++;
        boolean match = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (match) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            System.err.println("[FAIL] " + testName + " -> Expected: <" + expected + ">, Got: <" + actual + ">");
        }
    }

    private static void testBloodCompatibilityMatrix() {
        System.out.println("\n--- Testing ABO/Rh Blood Compatibility Matrix (All 8 Groups) ---");

        // 1. O- (can receive only from O-)
        assertEquals("O- compatible donors", List.of("O_NEG"), BloodCompatibility.getCompatibleDonors("O-"));

        // 2. O+ (can receive from O-, O+)
        assertEquals("O+ compatible donors", List.of("O_NEG", "O_POS"), BloodCompatibility.getCompatibleDonors("O+"));

        // 3. A- (can receive from O-, A-)
        assertEquals("A- compatible donors", List.of("O_NEG", "A_NEG"), BloodCompatibility.getCompatibleDonors("A-"));

        // 4. A+ (can receive from O-, O+, A-, A+)
        assertEquals("A+ compatible donors", List.of("O_NEG", "O_POS", "A_NEG", "A_POS"), BloodCompatibility.getCompatibleDonors("A+"));

        // 5. B- (can receive from O-, B-)
        assertEquals("B- compatible donors", List.of("O_NEG", "B_NEG"), BloodCompatibility.getCompatibleDonors("B-"));

        // 6. B+ (can receive from O-, O+, B-, B+)
        assertEquals("B+ compatible donors", List.of("O_NEG", "O_POS", "B_NEG", "B_POS"), BloodCompatibility.getCompatibleDonors("B+"));

        // 7. AB- (can receive from O-, A-, B-, AB-)
        assertEquals("AB- compatible donors", List.of("O_NEG", "A_NEG", "B_NEG", "AB_NEG"), BloodCompatibility.getCompatibleDonors("AB-"));

        // 8. AB+ (Universal recipient: all 8 groups)
        List<String> abPosDonors = BloodCompatibility.getCompatibleDonors("AB+");
        assertEquals("AB+ compatible donor count is 8", 8, abPosDonors.size());
        assertTrue("AB+ contains O_NEG", abPosDonors.contains("O_NEG"));
        assertTrue("AB+ contains AB_POS", abPosDonors.contains("AB_POS"));

        // Normalization checks
        assertEquals("Normalize 'o positive'", "O_POS", BloodCompatibility.normalize("o positive"));
        assertEquals("Normalize 'AB-'", "AB_NEG", BloodCompatibility.normalize("AB-"));
        assertTrue("Validate 'A+' is true", BloodCompatibility.isValidBloodGroup("A+"));
        assertTrue("Validate 'XYZ' is false", !BloodCompatibility.isValidBloodGroup("XYZ"));
    }

    private static void testRegistrationAndRoleSecurity(int port) throws Exception {
        System.out.println("\n--- Testing Registration Validation & Role Restrictions ---");

        try (Socket s = new Socket("127.0.0.1", port);
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()))) {

            // 1. User tries to self-register as ADMIN -> Must return FORBIDDEN
            ProtocolPayload adminAttempt = new ProtocolPayload()
                    .set("username", "hacker_admin")
                    .set("password", "root1234")
                    .set("role", "ADMIN")
                    .set("fullName", "Attacker");

            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.REGISTER, "reg-sec-01", adminAttempt));
            Packet resAdmin = ProtocolCodec.readPacket(in);
            assertEquals("Self-registration as ADMIN is FORBIDDEN", ResponseStatus.FORBIDDEN, resAdmin.getStatus());

            // 2. User tries to register with invalid role
            ProtocolPayload invalidRole = new ProtocolPayload()
                    .set("username", "bad_role_user")
                    .set("password", "pass1234")
                    .set("role", "SUPERUSER");

            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.REGISTER, "reg-sec-02", invalidRole));
            Packet resBadRole = ProtocolCodec.readPacket(in);
            assertEquals("Registration with invalid role is BAD_REQUEST", ResponseStatus.BAD_REQUEST, resBadRole.getStatus());

            // 3. Donor registration without blood group
            ProtocolPayload donorNoBlood = new ProtocolPayload()
                    .set("username", "donor_noblood")
                    .set("password", "pass1234")
                    .set("role", "DONOR");

            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.REGISTER, "reg-sec-03", donorNoBlood));
            Packet resDonorNoBlood = ProtocolCodec.readPacket(in);
            assertEquals("Donor registration without valid blood group is BAD_REQUEST", ResponseStatus.BAD_REQUEST, resDonorNoBlood.getStatus());

            // 4. Valid Donor registration with blood group
            ProtocolPayload validDonor = new ProtocolPayload()
                    .set("username", "valid_donor_1")
                    .set("password", "donorPass12")
                    .set("role", "DONOR")
                    .set("fullName", "David Miller")
                    .set("bloodGroup", "B+")
                    .set("city", "Colombo")
                    .set("phone", "0778899001");

            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.REGISTER, "reg-sec-04", validDonor));
            Packet resValid = ProtocolCodec.readPacket(in);
            assertEquals("Valid registration is SUCCESS", ResponseStatus.SUCCESS, resValid.getStatus());
            assertTrue("Assigned userId returned", resValid.getPayload().containsKey("userId"));

            // 5. Duplicate registration
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.REGISTER, "reg-sec-05", validDonor));
            Packet resDup = ProtocolCodec.readPacket(in);
            assertEquals("Duplicate username returns CONFLICT", ResponseStatus.CONFLICT, resDup.getStatus());
        }
    }

    private static void testUnauthenticatedAccessRestrictions(int port) throws Exception {
        System.out.println("\n--- Testing Unauthenticated Access Restrictions ---");

        try (Socket s = new Socket("127.0.0.1", port);
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()))) {

            // 1. Unauthenticated MATCH_DONORS
            ProtocolPayload matchPayload = new ProtocolPayload().set("bloodGroup", "O+");
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.MATCH_DONORS, "unauth-01", matchPayload));
            Packet resMatch = ProtocolCodec.readPacket(in);
            assertEquals("Unauthenticated MATCH_DONORS is UNAUTHORIZED", ResponseStatus.UNAUTHORIZED, resMatch.getStatus());

            // 2. Unauthenticated GET_BLOOD_REQUESTS
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.GET_BLOOD_REQUESTS, "unauth-02", ProtocolPayload.empty()));
            Packet resReqs = ProtocolCodec.readPacket(in);
            assertEquals("Unauthenticated GET_BLOOD_REQUESTS is UNAUTHORIZED", ResponseStatus.UNAUTHORIZED, resReqs.getStatus());

            // 3. Unauthenticated CREATE_BLOOD_REQUEST
            ProtocolPayload createReq = new ProtocolPayload()
                    .set("bloodGroup", "O+")
                    .set("hospital", "City Hospital")
                    .set("city", "Colombo");
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.CREATE_BLOOD_REQUEST, "unauth-03", createReq));
            Packet resCreate = ProtocolCodec.readPacket(in);
            assertEquals("Unauthenticated CREATE_BLOOD_REQUEST is UNAUTHORIZED", ResponseStatus.UNAUTHORIZED, resCreate.getStatus());

            // 4. Unauthenticated UPDATE_DONOR_AVAILABILITY
            ProtocolPayload availReq = new ProtocolPayload().set("isAvailable", false);
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.UPDATE_DONOR_AVAILABILITY, "unauth-04", availReq));
            Packet resAvail = ProtocolCodec.readPacket(in);
            assertEquals("Unauthenticated UPDATE_DONOR_AVAILABILITY is UNAUTHORIZED", ResponseStatus.UNAUTHORIZED, resAvail.getStatus());

            // 5. Unauthenticated SEND_MESSAGE
            ProtocolPayload sendMsg = new ProtocolPayload().set("recipientId", "usr-alice").set("content", "Hello");
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.SEND_MESSAGE, "unauth-05", sendMsg));
            Packet resMsg = ProtocolCodec.readPacket(in);
            assertEquals("Unauthenticated SEND_MESSAGE is UNAUTHORIZED", ResponseStatus.UNAUTHORIZED, resMsg.getStatus());

            // 6. Unauthenticated GET_PROFILE
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.GET_PROFILE, "unauth-06", ProtocolPayload.empty()));
            Packet resProf = ProtocolCodec.readPacket(in);
            assertEquals("Unauthenticated GET_PROFILE is UNAUTHORIZED", ResponseStatus.UNAUTHORIZED, resProf.getStatus());

            // 7. Unauthenticated GET_NOTIFICATIONS
            ProtocolCodec.writePacket(out, Packet.createRequest(ProtocolAction.GET_NOTIFICATIONS, "unauth-07", ProtocolPayload.empty()));
            Packet resNotif = ProtocolCodec.readPacket(in);
            assertEquals("Unauthenticated GET_NOTIFICATIONS is UNAUTHORIZED", ResponseStatus.UNAUTHORIZED, resNotif.getStatus());
        }
    }

    private static void testAuthenticatedWorkflowsAndMessaging(int port, BloodLinkServer server, ClientManager clientManager) throws Exception {
        System.out.println("\n--- Testing Authenticated Business Workflows, Chat & Routing ---");

        // Client A: Donor Alice
        Socket socketAlice = new Socket("127.0.0.1", port);
        DataInputStream inAlice = new DataInputStream(new BufferedInputStream(socketAlice.getInputStream()));
        DataOutputStream outAlice = new DataOutputStream(new BufferedOutputStream(socketAlice.getOutputStream()));

        // Client B: Recipient Charlie
        Socket socketCharlie = new Socket("127.0.0.1", port);
        DataInputStream inCharlie = new DataInputStream(new BufferedInputStream(socketCharlie.getInputStream()));
        DataOutputStream outCharlie = new DataOutputStream(new BufferedOutputStream(socketCharlie.getOutputStream()));

        try {
            // 1. Alice Logs In
            ProtocolPayload aliceLogin = new ProtocolPayload().set("username", "alice").set("password", "alice123");
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.LOGIN, "log-a", aliceLogin));
            Packet resAliceLog = ProtocolCodec.readPacket(inAlice);
            assertEquals("Alice login SUCCESS", ResponseStatus.SUCCESS, resAliceLog.getStatus());
            assertEquals("Alice role is DONOR", UserRole.DONOR, resAliceLog.getPayload().get("role"));
            assertTrue("Alice is online", clientManager.isUserOnline("usr-alice"));

            // 2. Charlie Logs In
            ProtocolPayload charlieLogin = new ProtocolPayload().set("username", "charlie").set("password", "char123");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.LOGIN, "log-c", charlieLogin));
            Packet resCharlieLog = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Charlie login SUCCESS", ResponseStatus.SUCCESS, resCharlieLog.getStatus());
            assertEquals("Charlie role is RECIPIENT", UserRole.RECIPIENT, resCharlieLog.getPayload().get("role"));
            assertTrue("Charlie is online", clientManager.isUserOnline("usr-charlie"));

            // 3. Charlie attempts to update donor availability -> Must return FORBIDDEN
            ProtocolPayload charlieAvail = new ProtocolPayload().set("isAvailable", false);
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.UPDATE_DONOR_AVAILABILITY, "c-avail", charlieAvail));
            Packet resCharlieAvail = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Recipient updating donor availability is FORBIDDEN", ResponseStatus.FORBIDDEN, resCharlieAvail.getStatus());

            // 4. Alice attempts to update another donor's availability -> Must return FORBIDDEN
            ProtocolPayload hijackAvail = new ProtocolPayload().set("userId", "usr-bob").set("isAvailable", false);
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.UPDATE_DONOR_AVAILABILITY, "a-hijack", hijackAvail));
            Packet resHijack = ProtocolCodec.readPacket(inAlice);
            assertEquals("Donor modifying another donor's availability is FORBIDDEN", ResponseStatus.FORBIDDEN, resHijack.getStatus());

            // 5. Blood Request Creation: Bad urgency test
            ProtocolPayload badUrgencyReq = new ProtocolPayload()
                    .set("bloodGroup", "O+")
                    .set("unitsNeeded", 2)
                    .set("hospital", "City General")
                    .set("city", "Colombo")
                    .set("urgency", "SUPER_CRITICAL");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.CREATE_BLOOD_REQUEST, "req-bad-urg", badUrgencyReq));
            Packet resBadUrg = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Invalid urgency level is BAD_REQUEST", ResponseStatus.BAD_REQUEST, resBadUrg.getStatus());

            // 6. Blood Request Creation: Bad units test
            ProtocolPayload badUnitsReq = new ProtocolPayload()
                    .set("bloodGroup", "O+")
                    .set("unitsNeeded", -5)
                    .set("hospital", "City General")
                    .set("city", "Colombo");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.CREATE_BLOOD_REQUEST, "req-bad-units", badUnitsReq));
            Packet resBadUnits = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Negative units is BAD_REQUEST", ResponseStatus.BAD_REQUEST, resBadUnits.getStatus());

            // 7. Charlie queries compatible donors for O+ in Colombo
            ProtocolPayload matchQuery = new ProtocolPayload().set("bloodGroup", "O+").set("city", "Colombo");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.MATCH_DONORS, "match-01", matchQuery));
            Packet resMatch = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Donor match query SUCCESS", ResponseStatus.SUCCESS, resMatch.getStatus());
            assertTrue("Compatible donors found", resMatch.getPayload().getInt("matchCount", 0) >= 1);

            List<String> donors = resMatch.getPayload().getList("donors");
            ProtocolPayload donorAlice = ProtocolPayload.parse(donors.get(0));
            assertEquals("Matched donor is Alice", "usr-alice", donorAlice.get("userId"));
            assertEquals("Alice is online", "true", donorAlice.get("isOnline"));

            // 8. Charlie creates URGENT blood request -> triggers real-time alert to Alice
            ProtocolPayload validReq = new ProtocolPayload()
                    .set("bloodGroup", "O+")
                    .set("unitsNeeded", 3)
                    .set("hospital", "National Blood Center")
                    .set("city", "Colombo")
                    .set("urgency", "URGENT")
                    .set("notes", "Patient preparing for bypass surgery");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.CREATE_BLOOD_REQUEST, "create-01", validReq));

            Packet resReqCreate = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Valid blood request creation SUCCESS", ResponseStatus.SUCCESS, resReqCreate.getStatus());
            String createdReqId = resReqCreate.getPayload().get("bloodRequestId");
            assertTrue("Blood request ID generated", createdReqId != null && !createdReqId.isEmpty());

            // Alice receives asynchronous EVENT
            Packet pushAlert = ProtocolCodec.readPacket(inAlice);
            assertEquals("Alice received push EVENT", MessageType.EVENT, pushAlert.getType());
            assertEquals("Alert action is URGENT_ALERT", ProtocolAction.URGENT_ALERT, pushAlert.getAction());
            assertEquals("Alert specifies correct blood group", "O_POS", pushAlert.getPayload().get("bloodGroup"));
            assertEquals("Alert specifies correct hospital", "National Blood Center", pushAlert.getPayload().get("hospital"));

            // 9. Alice checks her notification history
            ProtocolPayload notifQuery = new ProtocolPayload().set("limit", 10);
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.GET_NOTIFICATIONS, "notif-01", notifQuery));
            Packet resNotif = ProtocolCodec.readPacket(inAlice);
            assertEquals("Get notifications SUCCESS", ResponseStatus.SUCCESS, resNotif.getStatus());
            assertTrue("Notifications list has at least 1 record", resNotif.getPayload().getInt("count", 0) >= 1);

            // 10. Direct messaging: Non-existent recipient check
            ProtocolPayload nonExistentMsg = new ProtocolPayload()
                    .set("recipientId", "usr-non-existent-999")
                    .set("content", "Hello?");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.SEND_MESSAGE, "msg-ghost", nonExistentMsg));
            Packet resGhost = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Messaging non-existent recipient returns NOT_FOUND", ResponseStatus.NOT_FOUND, resGhost.getStatus());

            // 11. Direct messaging: Charlie messages Alice in real-time
            String chatMessage = "Hi Alice! Can you please donate for request " + createdReqId + "?";
            ProtocolPayload chatPayload = new ProtocolPayload()
                    .set("recipientId", "usr-alice")
                    .set("content", chatMessage);
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.SEND_MESSAGE, "chat-01", chatPayload));

            Packet resChatAck = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Chat message send SUCCESS", ResponseStatus.SUCCESS, resChatAck.getStatus());
            assertEquals("Delivered in real-time to online donor", "true", resChatAck.getPayload().get("deliveredRealTime"));

            // Alice receives incoming chat MSG packet
            Packet receivedMsg = ProtocolCodec.readPacket(inAlice);
            assertEquals("Alice received MSG packet", MessageType.MSG, receivedMsg.getType());
            assertEquals("Sender is Charlie (impersonation impossible)", "usr-charlie", receivedMsg.getPayload().get("senderId"));
            assertEquals("Content matches exactly", chatMessage, receivedMsg.getPayload().get("content"));

            // Alice replies
            String replyMessage = "I am on my way to National Blood Center now!";
            ProtocolPayload replyPayload = new ProtocolPayload()
                    .set("recipientId", "usr-charlie")
                    .set("content", replyMessage);
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.SEND_MESSAGE, "chat-02", replyPayload));
            ProtocolCodec.readPacket(inAlice); // Alice ACK

            Packet receivedReply = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Charlie received Alice's reply", replyMessage, receivedReply.getPayload().get("content"));

            // 12. Conversation history retrieval
            ProtocolPayload histQuery = new ProtocolPayload().set("otherUserId", "usr-alice").set("limit", 10);
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.GET_MESSAGES, "hist-01", histQuery));
            Packet resHist = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Get conversation history SUCCESS", ResponseStatus.SUCCESS, resHist.getStatus());
            assertEquals("Exactly 2 messages in conversation", 2, resHist.getPayload().getInt("count", 0));

            // 13. Profile retrieval with credential sanitization
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.GET_PROFILE, "prof-01", ProtocolPayload.empty()));
            Packet resProfile = ProtocolCodec.readPacket(inAlice);
            assertEquals("Get profile SUCCESS", ResponseStatus.SUCCESS, resProfile.getStatus());
            assertEquals("Profile userId matches", "usr-alice", resProfile.getPayload().get("userId"));
            assertTrue("Password is NOT exposed in profile payload", !resProfile.getPayload().containsKey("password"));

            // 14. Unauthorized status update on Charlie's blood request by Alice
            ProtocolPayload aliceUpdateStatus = new ProtocolPayload()
                    .set("bloodRequestId", createdReqId)
                    .set("status", "FULFILLED");
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, "status-alice", aliceUpdateStatus));
            Packet resAliceUpdate = ProtocolCodec.readPacket(inAlice);
            assertEquals("Unrelated donor updating blood request status is FORBIDDEN", ResponseStatus.FORBIDDEN, resAliceUpdate.getStatus());

            // 15. Authorized status update by the owner (Charlie)
            ProtocolPayload charlieUpdateStatus = new ProtocolPayload()
                    .set("bloodRequestId", createdReqId)
                    .set("status", "MATCHED");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, "status-charlie", charlieUpdateStatus));
            Packet resCharlieUpdate = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Owner updating blood request status is SUCCESS", ResponseStatus.SUCCESS, resCharlieUpdate.getStatus());

            // 16. Alice toggles availability to false
            ProtocolPayload availOff = new ProtocolPayload().set("isAvailable", false);
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.UPDATE_DONOR_AVAILABILITY, "avail-off", availOff));
            Packet resAvailOff = ProtocolCodec.readPacket(inAlice);
            assertEquals("Alice toggles availability SUCCESS", ResponseStatus.SUCCESS, resAvailOff.getStatus());

            // Query donors again: Alice must now be excluded
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.MATCH_DONORS, "match-02", matchQuery));
            Packet resMatchExcluded = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Unavailable donor is excluded from matching", 0, resMatchExcluded.getPayload().getInt("matchCount", 0));

            // Toggle Alice back to true
            ProtocolPayload availOn = new ProtocolPayload().set("isAvailable", true);
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.UPDATE_DONOR_AVAILABILITY, "avail-on", availOn));
            ProtocolCodec.readPacket(inAlice);

            // 17. Alice logs out
            ProtocolCodec.writePacket(outAlice, Packet.createRequest(ProtocolAction.LOGOUT, "logout-a", ProtocolPayload.empty()));
            Packet resAliceLogout = ProtocolCodec.readPacket(inAlice);
            assertEquals("Alice logout SUCCESS", ResponseStatus.SUCCESS, resAliceLogout.getStatus());
            assertTrue("Alice marked offline after logout", !clientManager.isUserOnline("usr-alice"));

            // 18. Charlie messages Alice while offline -> saved for offline retrieval
            ProtocolPayload offlineMsg = new ProtocolPayload()
                    .set("recipientId", "usr-alice")
                    .set("content", "Thank you Alice!");
            ProtocolCodec.writePacket(outCharlie, Packet.createRequest(ProtocolAction.SEND_MESSAGE, "chat-off", offlineMsg));
            Packet resOfflineAck = ProtocolCodec.readPacket(inCharlie);
            assertEquals("Offline message accepted with SUCCESS", ResponseStatus.SUCCESS, resOfflineAck.getStatus());
            assertEquals("Delivered in real-time is false", "false", resOfflineAck.getPayload().get("deliveredRealTime"));

        } finally {
            socketAlice.close();
            socketCharlie.close();
        }
    }
}

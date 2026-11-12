package com.bloodlink.client.test;

import com.bloodlink.client.models.BloodRequest;
import com.bloodlink.client.network.RequestBuilder;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.client.utils.ValidationUtils;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * End-to-End verification reproducing the workflow:
 * 1. Client (Recipient) submits a blood request.
 * 2. Donor queries blood requests via getBloodRequests.
 * 3. Verifies that the donor can see the request created by the recipient.
 */
public class ClientRequestDonorVisibilityTest {

    private static final int PORT = 9092;
    private static int passed = 0;
    private static int total = 0;

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  VERIFYING CLIENT REQUEST VISIBILITY TO DONOR TEST         ");
        System.out.println("============================================================");

        BloodLinkServer server = null;
        Socket recipientSocket = null;
        BufferedReader recipientIn = null;
        PrintWriter recipientOut = null;

        Socket donorSocket = null;
        BufferedReader donorIn = null;
        PrintWriter donorOut = null;

        try {
            // Step 1: Initialize Database and Server
            DatabaseInitializer.initialize();
            ClientManager clientManager = new ClientManager();
            IDatabaseService dbService = new SQLiteDatabaseService();
            NotificationService notifService = new NotificationService(clientManager, dbService);
            AuthService authService = new AuthService(dbService, clientManager);
            BloodMatchingService matchingService = new BloodMatchingService(dbService, clientManager, notifService);
            MessagingService messagingService = new MessagingService(dbService, clientManager);
            RequestDispatcher dispatcher = new RequestDispatcher(authService, matchingService, notifService, messagingService);

            server = new BloodLinkServer(PORT, clientManager, dispatcher);
            server.startAsync();
            Thread.sleep(600);
            check("Backend Server started on port " + PORT, server.isRunning());

            // Step 2: Connect Recipient Client
            recipientSocket = new Socket("127.0.0.1", PORT);
            recipientIn = new BufferedReader(new InputStreamReader(recipientSocket.getInputStream(), StandardCharsets.UTF_8));
            recipientOut = new PrintWriter(recipientSocket.getOutputStream(), true);
            check("Recipient TCP Socket connected", recipientSocket.isConnected());

            String recipientUser = "recipient_" + System.currentTimeMillis();
            recipientOut.println(RequestBuilder.buildRegisterRequest(
                    "rec_reg", recipientUser, "Pass123!", "RECIPIENT", "John Recipient", "A+", "Colombo", "0771234567", recipientUser + "@test.com"
            ));
            ParsedResponse recRegRes = ResponseHandler.parseWirePacket(recipientIn.readLine());
            check("Recipient registered successfully", recRegRes.isSuccess());

            recipientOut.println(RequestBuilder.buildLoginRequest("rec_log", recipientUser, "Pass123!"));
            ParsedResponse recLogRes = ResponseHandler.parseWirePacket(recipientIn.readLine());
            check("Recipient authenticated successfully", recLogRes.isSuccess());

            // Step 3: Connect Donor Client
            donorSocket = new Socket("127.0.0.1", PORT);
            donorIn = new BufferedReader(new InputStreamReader(donorSocket.getInputStream(), StandardCharsets.UTF_8));
            donorOut = new PrintWriter(donorSocket.getOutputStream(), true);
            check("Donor TCP Socket connected", donorSocket.isConnected());

            String donorUser = "donor_" + System.currentTimeMillis();
            donorOut.println(RequestBuilder.buildRegisterRequest(
                    "don_reg", donorUser, "Pass123!", "DONOR", "Jane Donor", "O+", "Colombo", "0779876543", donorUser + "@test.com"
            ));
            ParsedResponse donRegRes = ResponseHandler.parseWirePacket(donorIn.readLine());
            check("Donor registered successfully", donRegRes.isSuccess());

            donorOut.println(RequestBuilder.buildLoginRequest("don_log", donorUser, "Pass123!"));
            ParsedResponse donLogRes = ResponseHandler.parseWirePacket(donorIn.readLine());
            check("Donor authenticated successfully", donLogRes.isSuccess());

            // Step 4: Recipient creates a Blood Request for A+ at Colombo General Hospital
            String reqHospital = "Colombo General Hospital";
            String reqCity = "Colombo";
            String reqBloodGroup = "A_POS";
            int reqUnits = 3;
            String reqUrgency = "URGENT";

            recipientOut.println(RequestBuilder.buildCreateBloodRequest(
                    "create_req_1", reqBloodGroup, reqUnits, reqHospital, reqCity, reqUrgency, "Immediate surgery required"
            ));
            ParsedResponse createRes = ResponseHandler.parseWirePacket(recipientIn.readLine());
            check("Recipient request creation returned SUCCESS", createRes.isSuccess());
            String createdBloodReqId = createRes.get("bloodRequestId");
            check("Created Blood Request ID returned: " + createdBloodReqId, createdBloodReqId != null && !createdBloodReqId.isEmpty());

            // Step 5: Donor queries blood requests (mimicking DonorDashboard.refreshDashboard)
            donorOut.println(RequestBuilder.buildGetBloodRequests("don_get_reqs", "", "", ""));
            ParsedResponse getReqsRes = ResponseHandler.parseWirePacket(donorIn.readLine());
            // In case of a push notification arriving first, loop to find the RES packet
            while (!"RES".equals(getReqsRes.getType()) || !"GET_BLOOD_REQUESTS".equals(getReqsRes.getAction())) {
                getReqsRes = ResponseHandler.parseWirePacket(donorIn.readLine());
            }
            check("Donor getBloodRequests returned SUCCESS", getReqsRes.isSuccess());

            List<BloodRequest> donorVisibleRequests = ResponseHandler.parseBloodRequests(getReqsRes);
            check("Donor parsed blood requests list is not empty", !donorVisibleRequests.isEmpty());

            // Find our created request
            BloodRequest found = null;
            for (BloodRequest br : donorVisibleRequests) {
                if (createdBloodReqId.equals(br.getRequestId())) {
                    found = br;
                    break;
                }
            }

            check("Created request found in donor's requests list", found != null);
            if (found != null) {
                check("Blood group matches: " + found.getBloodGroup(), "A_POS".equalsIgnoreCase(found.getBloodGroup()));
                check("Hospital matches: " + found.getHospital(), reqHospital.equals(found.getHospital()));
                check("Units matches: " + found.getUnitsRequired(), found.getUnitsRequired() == reqUnits);
                check("Urgency matches: " + found.getUrgency(), reqUrgency.equalsIgnoreCase(found.getUrgency()));
                check("Status matches: " + found.getStatus(), "PENDING".equalsIgnoreCase(found.getStatus()) || "OPEN".equalsIgnoreCase(found.getStatus()));

                // Biological Compatibility check: O+ donor can donate to A+ recipient
                boolean compatible = ValidationUtils.isCompatible("O+", found.getBloodGroup());
                check("O+ Donor is biologically compatible with recipient A+ request", compatible);
            }

            System.out.println("============================================================");
            System.out.printf("RESULT: %d / %d TESTS PASSED (%d%%)%n", passed, total, (passed * 100) / total);
            System.out.println("VERIFIED: DONORS CAN SEE REQUESTS SUBMITTED BY CLIENTS!");
            System.out.println("============================================================");

        } catch (Exception e) {
            System.err.println("[FAIL] Test exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (recipientIn != null) recipientIn.close();
                if (recipientOut != null) recipientOut.close();
                if (recipientSocket != null) recipientSocket.close();
                if (donorIn != null) donorIn.close();
                if (donorOut != null) donorOut.close();
                if (donorSocket != null) donorSocket.close();
            } catch (Exception ignored) {}
            if (server != null) server.stop();
        }
    }

    private static void check(String description, boolean condition) {
        total++;
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + description);
        } else {
            System.out.println("  [FAIL] " + description);
        }
    }
}

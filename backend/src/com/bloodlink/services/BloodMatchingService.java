package com.bloodlink.services;

import com.bloodlink.interfaces.IDatabaseService;
import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolAction;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.protocol.ResponseStatus;
import com.bloodlink.protocol.UserRole;
import com.bloodlink.server.ClientHandler;
import com.bloodlink.server.ClientManager;
import com.bloodlink.util.ServerLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles blood requests, ABO/Rh matching, inventory queries, and donor availability updates.
 */
public class BloodMatchingService implements IBloodMatchingService {

    private static final Set<String> VALID_URGENCIES = Set.of("NORMAL", "URGENT", "CRITICAL");
    private static final Set<String> VALID_REQUEST_STATUSES = Set.of("PENDING", "MATCHED", "FULFILLED", "CANCELLED");

    private final IDatabaseService databaseService;
    private final ClientManager clientManager;
    private final INotificationService notificationService;

    public BloodMatchingService(IDatabaseService databaseService, ClientManager clientManager, INotificationService notificationService) {
        this.databaseService = databaseService;
        this.clientManager = clientManager;
        this.notificationService = notificationService;
    }

    @Override
    public Packet handleCreateBloodRequest(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to create a blood request");
        }

        String rawBloodGroup = payload.get("bloodGroup");
        int unitsNeeded = payload.getInt("unitsNeeded", 1);
        String hospital = payload.get("hospital");
        String city = payload.get("city");
        String urgency = payload.getOrDefault("urgency", "NORMAL").trim().toUpperCase();
        String notes = payload.getOrDefault("notes", "");

        // Input validation
        if (rawBloodGroup == null || !BloodCompatibility.isValidBloodGroup(rawBloodGroup)) {
            return Packet.createErrorResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId,
                    ResponseStatus.BAD_REQUEST, "Invalid or missing 'bloodGroup'. Valid: O+, O-, A+, A-, B+, B-, AB+, AB-");
        }

        if (unitsNeeded <= 0 || unitsNeeded > 100) {
            return Packet.createErrorResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId,
                    ResponseStatus.BAD_REQUEST, "Parameter 'unitsNeeded' must be a positive integer between 1 and 100");
        }

        if (!VALID_URGENCIES.contains(urgency)) {
            return Packet.createErrorResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId,
                    ResponseStatus.BAD_REQUEST, "Invalid urgency level: " + urgency + ". Allowed: NORMAL, URGENT, CRITICAL");
        }

        if (hospital == null || hospital.trim().isEmpty() || city == null || city.trim().isEmpty()) {
            return Packet.createErrorResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId,
                    ResponseStatus.BAD_REQUEST, "Parameters 'hospital' and 'city' are required");
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        String normBloodGroup = BloodCompatibility.normalize(rawBloodGroup);
        Map<String, String> requestData = new HashMap<>(payload.asMap());
        requestData.put("requesterId", client.getAuthenticatedUserId());
        requestData.put("bloodGroup", normBloodGroup);
        requestData.put("unitsNeeded", Integer.toString(unitsNeeded));
        requestData.put("hospital", hospital.trim());
        requestData.put("city", city.trim());
        requestData.put("urgency", urgency);
        requestData.put("notes", notes != null ? notes.trim() : "");
        requestData.put("status", "PENDING");
        requestData.put("createdAt", Long.toString(System.currentTimeMillis()));

        String bloodRequestId = databaseService.createBloodRequest(requestData);
        if (bloodRequestId == null) {
            return Packet.createErrorResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Failed to persist blood request");
        }

        // Automatic Donor Matching based on ABO/Rh rules
        List<String> compatibleGroups = BloodCompatibility.getCompatibleDonors(normBloodGroup);
        List<Map<String, String>> eligibleDonors = databaseService.findEligibleDonors(compatibleGroups, city.trim());

        List<String> matchedDonorIds = new ArrayList<>();
        for (Map<String, String> donor : eligibleDonors) {
            String donorId = donor.get("userId");
            if (donorId != null) {
                matchedDonorIds.add(donorId);
            }
        }

        // Real-time notification dispatch if request is URGENT or CRITICAL
        int notifiedOnline = 0;
        if (("URGENT".equals(urgency) || "CRITICAL".equals(urgency)) && notificationService != null) {
            Map<String, String> details = new HashMap<>(requestData);
            details.put("requestId", bloodRequestId);
            notifiedOnline = notificationService.notifyEligibleDonors(matchedDonorIds, details);
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("bloodRequestId", bloodRequestId)
                .set("bloodGroup", normBloodGroup)
                .set("matchedDonorCount", matchedDonorIds.size())
                .set("notifiedOnlineCount", notifiedOnline)
                .set("status", "PENDING")
                .set("message", "Blood request created successfully");

        ServerLogger.info("Blood request %s created for %s at %s. Matched %d donors (%d notified online).",
                bloodRequestId, normBloodGroup, hospital, matchedDonorIds.size(), notifiedOnline);

        return Packet.createSuccessResponse(ProtocolAction.CREATE_BLOOD_REQUEST, requestId, resPayload);
    }

    @Override
    public Packet handleMatchDonors(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.MATCH_DONORS, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to query donor matching");
        }

        String rawBloodGroup = payload.get("bloodGroup");
        String city = payload.get("city");

        if (rawBloodGroup == null || !BloodCompatibility.isValidBloodGroup(rawBloodGroup)) {
            return Packet.createErrorResponse(ProtocolAction.MATCH_DONORS, requestId,
                    ResponseStatus.BAD_REQUEST, "Invalid or missing 'bloodGroup'. Valid: O+, O-, A+, A-, B+, B-, AB+, AB-");
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.MATCH_DONORS, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        String normRecipient = BloodCompatibility.normalize(rawBloodGroup);
        List<String> compatibleGroups = BloodCompatibility.getCompatibleDonors(normRecipient);

        List<Map<String, String>> donors = databaseService.findEligibleDonors(compatibleGroups, city);
        List<String> donorRecords = new ArrayList<>();

        for (Map<String, String> donor : donors) {
            String donorId = donor.get("userId");
            boolean online = clientManager.isUserOnline(donorId);

            ProtocolPayload rec = new ProtocolPayload();
            rec.set("userId", donor.getOrDefault("userId", ""));
            rec.set("fullName", donor.getOrDefault("fullName", ""));
            rec.set("bloodGroup", donor.getOrDefault("bloodGroup", ""));
            rec.set("bloodGroupSymbol", BloodCompatibility.toSymbol(donor.get("bloodGroup")));
            rec.set("city", donor.getOrDefault("city", ""));
            rec.set("phone", donor.getOrDefault("phone", ""));
            rec.set("isAvailable", donor.getOrDefault("isAvailable", "true"));
            rec.set("isOnline", online);

            donorRecords.add(rec.serialize());
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("recipientBloodGroup", normRecipient)
                .set("matchCount", donorRecords.size())
                .setList("donors", donorRecords);

        ServerLogger.info("Donor match query for %s returned %d compatible donors", normRecipient, donorRecords.size());
        return Packet.createSuccessResponse(ProtocolAction.MATCH_DONORS, requestId, resPayload);
    }

    @Override
    public Packet handleGetBloodRequests(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.GET_BLOOD_REQUESTS, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to view blood requests");
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.GET_BLOOD_REQUESTS, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        List<Map<String, String>> requests = databaseService.getBloodRequests(payload.asMap());
        List<String> requestRecords = new ArrayList<>();

        for (Map<String, String> req : requests) {
            ProtocolPayload rec = new ProtocolPayload(req);
            requestRecords.add(rec.serialize());
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("count", requestRecords.size())
                .setList("requests", requestRecords);

        return Packet.createSuccessResponse(ProtocolAction.GET_BLOOD_REQUESTS, requestId, resPayload);
    }

    @Override
    public Packet handleUpdateBloodRequestStatus(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to update request status");
        }

        String bloodRequestId = payload.get("bloodRequestId");
        String newStatus = payload.get("status");

        if (bloodRequestId == null || bloodRequestId.trim().isEmpty() || newStatus == null || newStatus.trim().isEmpty()) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, requestId,
                    ResponseStatus.BAD_REQUEST, "Parameters 'bloodRequestId' and 'status' are required");
        }

        String normalizedStatus = newStatus.trim().toUpperCase();
        if (!VALID_REQUEST_STATUSES.contains(normalizedStatus)) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, requestId,
                    ResponseStatus.BAD_REQUEST, "Invalid status: " + newStatus + ". Allowed: PENDING, MATCHED, FULFILLED, CANCELLED");
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        // Authorization: Verify user is the requester (owner) or an ADMIN
        List<Map<String, String>> existingList = databaseService.getBloodRequests(Map.of("bloodRequestId", bloodRequestId));
        if (existingList != null && !existingList.isEmpty()) {
            Map<String, String> existing = existingList.get(0);
            String requesterId = existing.get("requesterId");
            String role = client.getUserRole();
            boolean isOwner = requesterId != null && requesterId.equals(client.getAuthenticatedUserId());
            boolean isAdmin = UserRole.ADMIN.equalsIgnoreCase(role);

            if (!isOwner && !isAdmin) {
                return Packet.createErrorResponse(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, requestId,
                        ResponseStatus.FORBIDDEN, "Only the requester or an admin can update blood request status");
            }
        }

        boolean updated = databaseService.updateBloodRequestStatus(bloodRequestId, normalizedStatus);
        if (!updated) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, requestId,
                    ResponseStatus.NOT_FOUND, "Blood request not found or update failed");
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("bloodRequestId", bloodRequestId)
                .set("status", normalizedStatus)
                .set("message", "Blood request status updated to " + normalizedStatus);

        ServerLogger.info("Blood request %s status updated to %s by user %s",
                bloodRequestId, normalizedStatus, client.getAuthenticatedUserId());
        return Packet.createSuccessResponse(ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS, requestId, resPayload);
    }

    @Override
    public Packet handleUpdateDonorAvailability(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_DONOR_AVAILABILITY, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to update donor availability");
        }

        String role = client.getUserRole();
        // Role authorization check: only DONOR and ADMIN can update donor availability
        if (!UserRole.DONOR.equalsIgnoreCase(role) && !UserRole.ADMIN.equalsIgnoreCase(role)) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_DONOR_AVAILABILITY, requestId,
                    ResponseStatus.FORBIDDEN, "Only users with role DONOR or ADMIN can update donor availability");
        }

        // Security check: ordinary donors cannot modify availability for other donors
        String targetUserId = payload.get("userId");
        if (targetUserId != null && !targetUserId.equals(client.getAuthenticatedUserId()) && !UserRole.ADMIN.equalsIgnoreCase(role)) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_DONOR_AVAILABILITY, requestId,
                    ResponseStatus.FORBIDDEN, "You cannot modify availability for another donor");
        }

        String userIdToUpdate = (targetUserId != null && UserRole.ADMIN.equalsIgnoreCase(role))
                ? targetUserId
                : client.getAuthenticatedUserId();

        boolean isAvailable = payload.getBoolean("isAvailable", true);

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_DONOR_AVAILABILITY, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        boolean updated = databaseService.updateDonorAvailability(userIdToUpdate, isAvailable);
        if (!updated) {
            return Packet.createErrorResponse(ProtocolAction.UPDATE_DONOR_AVAILABILITY, requestId,
                    ResponseStatus.NOT_FOUND, "Donor profile not found or update failed");
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("userId", userIdToUpdate)
                .set("isAvailable", isAvailable)
                .set("message", "Donor availability updated to " + isAvailable);

        ServerLogger.info("Donor %s availability updated to %b by %s",
                userIdToUpdate, isAvailable, client.getAuthenticatedUserId());
        return Packet.createSuccessResponse(ProtocolAction.UPDATE_DONOR_AVAILABILITY, requestId, resPayload);
    }
}

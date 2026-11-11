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

import java.util.HashMap;
import java.util.Map;

/**
 * Handles user authentication, registration, session management, and profile queries.
 * Communicates with the database layer via IDatabaseService.
 */
public class AuthService implements IAuthService {

    private final IDatabaseService databaseService;
    private final ClientManager clientManager;

    public AuthService(IDatabaseService databaseService, ClientManager clientManager) {
        this.databaseService = databaseService;
        this.clientManager = clientManager;
    }

    @Override
    public Packet handleLogin(ClientHandler client, String requestId, ProtocolPayload payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Packet.createErrorResponse(ProtocolAction.LOGIN, requestId,
                    ResponseStatus.BAD_REQUEST, "Username and password are required");
        }

        String trimmedUser = username.trim();
        ServerLogger.info("Login attempt for username: %s from session %s", trimmedUser, client.getSessionId());

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.LOGIN, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        Map<String, String> user = databaseService.authenticate(trimmedUser, password);
        if (user == null || user.isEmpty()) {
            ServerLogger.warn("Failed login attempt for username: %s", trimmedUser);
            return Packet.createErrorResponse(ProtocolAction.LOGIN, requestId,
                    ResponseStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String userId = user.get("userId");
        String role = user.getOrDefault("role", UserRole.DONOR);
        String fullName = user.getOrDefault("fullName", trimmedUser);
        String bloodGroup = user.getOrDefault("bloodGroup", "");
        String city = user.getOrDefault("city", "");

        // Bind user to session in ClientManager (safely replaces any prior binding)
        client.setUserRole(role);
        clientManager.bindUser(userId, client);

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("userId", userId)
                .set("username", trimmedUser)
                .set("role", role)
                .set("fullName", fullName)
                .set("bloodGroup", bloodGroup)
                .set("city", city);

        ServerLogger.info("User %s (%s) with role %s logged in successfully on session %s",
                trimmedUser, userId, role, client.getSessionId());
        return Packet.createSuccessResponse(ProtocolAction.LOGIN, requestId, resPayload);
    }

    @Override
    public Packet handleRegister(ClientHandler client, String requestId, ProtocolPayload payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String requestedRole = payload.getOrDefault("role", UserRole.DONOR).trim().toUpperCase();
        String fullName = payload.get("fullName");
        String rawBloodGroup = payload.get("bloodGroup");
        String city = payload.get("city");
        String phone = payload.get("phone");

        // 1. Basic validation
        if (username == null || username.trim().length() < 3) {
            return Packet.createErrorResponse(ProtocolAction.REGISTER, requestId,
                    ResponseStatus.BAD_REQUEST, "Username must be at least 3 characters long");
        }
        if (password == null || password.trim().length() < 4) {
            return Packet.createErrorResponse(ProtocolAction.REGISTER, requestId,
                    ResponseStatus.BAD_REQUEST, "Password must be at least 4 characters long");
        }

        // 2. Role security check - ordinary users cannot self-register as ADMIN
        if (UserRole.ADMIN.equalsIgnoreCase(requestedRole)) {
            ServerLogger.warn("Blocked attempt to register account with ADMIN privileges for username: %s", username);
            return Packet.createErrorResponse(ProtocolAction.REGISTER, requestId,
                    ResponseStatus.FORBIDDEN, "Registration with role ADMIN is not permitted via public registration");
        }

        if (!UserRole.isSelfRegistrable(requestedRole)) {
            return Packet.createErrorResponse(ProtocolAction.REGISTER, requestId,
                    ResponseStatus.BAD_REQUEST, "Invalid role: " + requestedRole + ". Allowed roles: DONOR, RECIPIENT, HOSPITAL");
        }

        // 3. Donor-specific validation
        String normBloodGroup = "";
        if (UserRole.DONOR.equals(requestedRole)) {
            if (rawBloodGroup == null || !BloodCompatibility.isValidBloodGroup(rawBloodGroup)) {
                return Packet.createErrorResponse(ProtocolAction.REGISTER, requestId,
                        ResponseStatus.BAD_REQUEST, "A valid blood group is required for DONOR registration (e.g. O+, O-, A+, A-, B+, B-, AB+, AB-)");
            }
            normBloodGroup = BloodCompatibility.normalize(rawBloodGroup);
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.REGISTER, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        Map<String, String> regData = new HashMap<>(payload.asMap());
        regData.put("username", username.trim());
        regData.put("role", requestedRole);
        if (!normBloodGroup.isEmpty()) {
            regData.put("bloodGroup", normBloodGroup);
        }
        if (fullName != null) {
            regData.put("fullName", fullName.trim());
        }
        if (city != null) {
            regData.put("city", city.trim());
        }
        if (phone != null) {
            regData.put("phone", phone.trim());
        }

        ServerLogger.info("Register attempt for username: %s (role: %s)", username.trim(), requestedRole);

        String generatedUserId = databaseService.registerUser(regData);
        if (generatedUserId == null) {
            return Packet.createErrorResponse(ProtocolAction.REGISTER, requestId,
                    ResponseStatus.CONFLICT, "Username already exists or registration failed");
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("userId", generatedUserId)
                .set("role", requestedRole)
                .set("message", "User registered successfully");

        ServerLogger.info("User %s registered successfully with userId: %s", username.trim(), generatedUserId);
        return Packet.createSuccessResponse(ProtocolAction.REGISTER, requestId, resPayload);
    }

    @Override
    public Packet handleLogout(ClientHandler client, String requestId) {
        String userId = client.getAuthenticatedUserId();
        if (userId != null) {
            clientManager.unbindUser(userId, client);
            ServerLogger.info("User %s logged out from session %s", userId, client.getSessionId());
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("message", "Logged out successfully");

        return Packet.createSuccessResponse(ProtocolAction.LOGOUT, requestId, resPayload);
    }

    @Override
    public Packet handleGetProfile(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.GET_PROFILE, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to view user profile");
        }

        String targetUserId = payload.get("userId");
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            targetUserId = client.getAuthenticatedUserId();
        }

        // Authorization: only the user themselves, a HOSPITAL, or an ADMIN can view detailed profiles
        String currentRole = client.getUserRole();
        boolean isSelf = targetUserId.equals(client.getAuthenticatedUserId());
        boolean isPrivileged = UserRole.HOSPITAL.equalsIgnoreCase(currentRole) || UserRole.ADMIN.equalsIgnoreCase(currentRole);

        if (!isSelf && !isPrivileged) {
            return Packet.createErrorResponse(ProtocolAction.GET_PROFILE, requestId,
                    ResponseStatus.FORBIDDEN, "You do not have permission to view other users' full profiles");
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.GET_PROFILE, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        Map<String, String> user = databaseService.getUserById(targetUserId);
        if (user == null || user.isEmpty()) {
            return Packet.createErrorResponse(ProtocolAction.GET_PROFILE, requestId,
                    ResponseStatus.NOT_FOUND, "User profile not found: " + targetUserId);
        }

        // Return profile attributes, strictly stripping any password or sensitive hash!
        ProtocolPayload resPayload = new ProtocolPayload();
        for (Map.Entry<String, String> entry : user.entrySet()) {
            String k = entry.getKey();
            if (!"password".equalsIgnoreCase(k) && !"pass".equalsIgnoreCase(k) && !"secret".equalsIgnoreCase(k)) {
                resPayload.set(k, entry.getValue());
            }
        }
        resPayload.set("isOnline", clientManager.isUserOnline(targetUserId));

        return Packet.createSuccessResponse(ProtocolAction.GET_PROFILE, requestId, resPayload);
    }
}

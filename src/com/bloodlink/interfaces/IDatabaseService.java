package com.bloodlink.interfaces;

import java.util.List;
import java.util.Map;

/**
 * Backend-facing integration interface for Database Services.
 * This contract decouples Member 02's networking backend from Member 03's SQLite/JDBC implementation.
 * Member 03 will implement this interface.
 */
public interface IDatabaseService {

    // --- Authentication & User Management ---

    /**
     * Authenticates a user with credentials.
     *
     * @param username user account handle or email
     * @param password user password (or hash agreed with Member 03)
     * @return Map containing user attributes (userId, username, role, fullName, bloodGroup, city, isAvailable),
     *         or null if authentication fails.
     */
    Map<String, String> authenticate(String username, String password);

    /**
     * Registers a new user account into the system.
     *
     * @param userData Map containing registration fields (username, password, role, fullName, bloodGroup, city, phone)
     * @return generated unique userId, or null/exception if registration fails (e.g., username conflict).
     */
    String registerUser(Map<String, String> userData);

    /**
     * Fetches user profile attributes by userId.
     */
    Map<String, String> getUserById(String userId);

    // --- Blood Requests & Inventory Operations ---

    /**
     * Creates a new blood requirement request.
     *
     * @param requestData Map containing: requesterId, bloodGroup, unitsNeeded, urgency, hospital, city, notes
     * @return generated unique bloodRequestId.
     */
    String createBloodRequest(Map<String, String> requestData);

    /**
     * Queries blood requests matching given filter criteria.
     *
     * @param filters Map of filter criteria (e.g. status, bloodGroup, city), or empty for all.
     * @return List of matching blood request attribute maps.
     */
    List<Map<String, String>> getBloodRequests(Map<String, String> filters);

    /**
     * Updates status of an existing blood request (e.g. PENDING, MATCHED, FULFILLED, CANCELLED).
     */
    boolean updateBloodRequestStatus(String bloodRequestId, String newStatus);

    // --- Donor Matching Operations ---

    /**
     * Finds eligible donors whose blood group is in compatibleBloodGroups, optionally filtered by city.
     *
     * @param compatibleBloodGroups list of compatible blood types (e.g. ["O_NEG", "O_POS"])
     * @param city optional city filter (null or empty string to match all cities)
     * @return List of matching donor attribute maps.
     */
    List<Map<String, String>> findEligibleDonors(List<String> compatibleBloodGroups, String city);

    /**
     * Updates a donor's active availability status.
     */
    boolean updateDonorAvailability(String userId, boolean isAvailable);

    // --- Donor-Recipient Messaging Operations ---

    /**
     * Persists a direct chat message between a donor and recipient.
     *
     * @return generated unique messageId.
     */
    String saveMessage(String senderId, String recipientId, String content, long timestamp);

    /**
     * Retrieves chronological conversation history between two users.
     */
    List<Map<String, String>> getConversationHistory(String user1, String user2, int limit);

    // --- Notifications ---

    /**
     * Persists an audit/offline notification for a user.
     */
    boolean saveNotification(String targetUserId, String title, String message, String urgency, long timestamp);

    /**
     * Fetches persisted notifications for a user.
     */
    List<Map<String, String>> getNotificationsForUser(String userId, int limit);

    // --- Donor Response Operations ---

    /**
     * Records or updates a donor response (ACCEPTED, REJECTED, PENDING) for a blood request.
     */
    Map<String, String> recordDonorResponse(String requestId, String donorId, String status, String notes);

    /**
     * Retrieves all donor responses for a specific blood request.
     */
    List<Map<String, String>> getDonorResponsesForRequest(String requestId);

    /**
     * Retrieves all donor responses for all requests created by a recipient.
     */
    List<Map<String, String>> getDonorResponsesForRecipient(String recipientId);

    /**
     * Retrieves the response of a specific donor for a request.
     */
    Map<String, String> getDonorResponse(String requestId, String donorId);

    /**
     * Returns a map of requestId -> status for all responses made by this donor.
     */
    Map<String, String> getDonorResponsesMapForDonor(String donorId);

    // --- Admin Operations ---

    /**
     * Retrieves all users optionally matching search query.
     */
    List<Map<String, String>> getAllUsers(String searchQuery);

    /**
     * Updates account status of a user (ACTIVE or BLOCKED).
     */
    boolean updateUserAccountStatus(String userId, String status);

    /**
     * Deletes a user account.
     */
    boolean deleteUserAccount(String userId);

    /**
     * Returns aggregated system statistics for Admin dashboard.
     */
    Map<String, String> getSystemStatistics();
}

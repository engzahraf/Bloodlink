package com.bloodlink.database;

import com.bloodlink.database.dao.BloodRequestDAO;
import com.bloodlink.database.dao.DonationDAO;
import com.bloodlink.database.dao.DonorProfileDAO;
import com.bloodlink.database.dao.DonorResponseDAO;
import com.bloodlink.database.dao.MessageDAO;
import com.bloodlink.database.dao.NotificationDAO;
import com.bloodlink.database.dao.UserDAO;
import com.bloodlink.database.enums.AccountStatus;
import com.bloodlink.database.enums.AvailabilityStatus;
import com.bloodlink.database.enums.BloodGroup;
import com.bloodlink.database.enums.NotificationType;
import com.bloodlink.database.enums.RequestStatus;
import com.bloodlink.database.enums.UrgencyLevel;
import com.bloodlink.database.enums.UserRole;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.BloodRequest;
import com.bloodlink.database.models.DonorProfile;
import com.bloodlink.database.models.DonorResponse;
import com.bloodlink.database.models.Message;
import com.bloodlink.database.models.Notification;
import com.bloodlink.database.models.User;
import com.bloodlink.database.security.PasswordHasher;
import com.bloodlink.interfaces.IDatabaseService;
import com.bloodlink.services.BloodCompatibility;
import com.bloodlink.util.ServerLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production SQLite/JDBC implementation of IDatabaseService.
 * Connects Member 02's TCP backend server to Member 03's SQLite DAO layer.
 */
public class SQLiteDatabaseService implements IDatabaseService {

    private final UserDAO userDAO;
    private final DonorProfileDAO donorDAO;
    private final BloodRequestDAO requestDAO;
    private final DonationDAO donationDAO;
    private final NotificationDAO notificationDAO;
    private final MessageDAO messageDAO;
    private final DonorResponseDAO responseDAO;

    public SQLiteDatabaseService() {
        this(new UserDAO(), new DonorProfileDAO(), new BloodRequestDAO(),
             new DonationDAO(), new NotificationDAO(), new MessageDAO(), new DonorResponseDAO());
    }

    public SQLiteDatabaseService(UserDAO userDAO, DonorProfileDAO donorDAO, BloodRequestDAO requestDAO,
                                 DonationDAO donationDAO, NotificationDAO notificationDAO, MessageDAO messageDAO) {
        this(userDAO, donorDAO, requestDAO, donationDAO, notificationDAO, messageDAO, new DonorResponseDAO());
    }

    public SQLiteDatabaseService(UserDAO userDAO, DonorProfileDAO donorDAO, BloodRequestDAO requestDAO,
                                 DonationDAO donationDAO, NotificationDAO notificationDAO, MessageDAO messageDAO,
                                 DonorResponseDAO responseDAO) {
        this.userDAO = userDAO;
        this.donorDAO = donorDAO;
        this.requestDAO = requestDAO;
        this.donationDAO = donationDAO;
        this.notificationDAO = notificationDAO;
        this.messageDAO = messageDAO;
        this.responseDAO = responseDAO;
    }

    // =========================================================================
    // 1. Authentication & User Management
    // =========================================================================

    @Override
    public Map<String, String> authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanUser = username.trim();
            User user = userDAO.getUserByEmail(cleanUser);

            // If not found by email and input is numeric, attempt lookup by user_id
            if (user == null && isInteger(cleanUser)) {
                user = userDAO.getUserById(Integer.parseInt(cleanUser));
            }

            if (user == null) {
                ServerLogger.warn("Authentication failed: user '%s' not found in database", cleanUser);
                return null;
            }

            if (!user.isActive()) {
                ServerLogger.warn("Authentication rejected: account '%s' is BLOCKED", cleanUser);
                return null;
            }

            // Verify PBKDF2 hash using Member 03's PasswordHasher
            boolean valid = PasswordHasher.checkPassword(password, user.getPasswordHash());
            if (!valid) {
                ServerLogger.warn("Authentication failed: invalid password for user '%s'", cleanUser);
                return null;
            }

            Map<String, String> result = new HashMap<>();
            result.put("userId", String.valueOf(user.getUserId()));
            result.put("username", user.getEmail());
            result.put("email", user.getEmail());
            result.put("role", user.getRole().name());
            result.put("fullName", user.getFullName());
            result.put("phone", user.getPhone() != null ? user.getPhone() : "");
            result.put("accountStatus", user.getAccountStatus().name());

            // If user is a DONOR, load their donor profile details
            if (user.getRole() == UserRole.DONOR) {
                DonorProfile profile = donorDAO.getDonorByUserId(user.getUserId());
                if (profile != null) {
                    result.put("bloodGroup", toBackendBloodGroup(profile.getBloodGroup()));
                    result.put("bloodGroupSymbol", profile.getBloodGroup().getSymbol());
                    result.put("city", profile.getLocation());
                    result.put("isAvailable", String.valueOf(profile.isAvailable()));
                }
            }

            return result;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error during authentication: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String registerUser(Map<String, String> userData) {
        if (userData == null) {
            return null;
        }

        String fullName = userData.getOrDefault("fullName", userData.get("username"));
        String email = userData.getOrDefault("email", userData.get("username"));
        String password = userData.get("password");
        String phone = userData.getOrDefault("phone", "");
        String roleStr = userData.getOrDefault("role", "DONOR").trim().toUpperCase();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return null;
        }

        // Validate role against database schema (CHECK constraint allows only ADMIN, DONOR, RECIPIENT)
        UserRole role;
        try {
            role = UserRole.fromString(roleStr);
        } catch (IllegalArgumentException e) {
            ServerLogger.warn("Registration rejected: role '%s' is not supported by database schema (allowed: ADMIN, DONOR, RECIPIENT)", roleStr);
            return null;
        }

        try {
            // Hash password with PBKDF2
            String passwordHash = PasswordHasher.hashPassword(password);
            User newUser = new User(fullName != null ? fullName.trim() : email.trim(),
                                    email.trim().toLowerCase(),
                                    passwordHash,
                                    phone != null ? phone.trim() : null,
                                    role);

            User created = userDAO.createUser(newUser);
            if (created == null) {
                return null;
            }

            int userId = created.getUserId();

            // If registering as a DONOR, persist corresponding donor profile record
            if (role == UserRole.DONOR) {
                String bgStr = userData.get("bloodGroup");
                BloodGroup bg = toDbBloodGroup(bgStr);
                String city = userData.getOrDefault("city", userData.getOrDefault("location", "Colombo"));

                DonorProfile profile = new DonorProfile(
                    userId,
                    bg != null ? bg : BloodGroup.O_POSITIVE,
                    city != null ? city.trim() : "Colombo",
                    AvailabilityStatus.AVAILABLE
                );
                donorDAO.createDonorProfile(profile);
            }

            return String.valueOf(userId);
        } catch (ValidationException e) {
            ServerLogger.warn("Registration validation failure: %s", e.getMessage());
            return null;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error during registration: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<String, String> getUserById(String userId) {
        int id = parseId(userId);
        if (id <= 0) {
            return null;
        }

        try {
            User user = userDAO.getUserById(id);
            if (user == null) {
                return null;
            }

            Map<String, String> result = new HashMap<>();
            result.put("userId", String.valueOf(user.getUserId()));
            result.put("username", user.getEmail());
            result.put("email", user.getEmail());
            result.put("role", user.getRole().name());
            result.put("fullName", user.getFullName());
            result.put("phone", user.getPhone() != null ? user.getPhone() : "");
            result.put("accountStatus", user.getAccountStatus().name());

            if (user.getRole() == UserRole.DONOR) {
                DonorProfile profile = donorDAO.getDonorByUserId(user.getUserId());
                if (profile != null) {
                    result.put("bloodGroup", toBackendBloodGroup(profile.getBloodGroup()));
                    result.put("bloodGroupSymbol", profile.getBloodGroup().getSymbol());
                    result.put("city", profile.getLocation());
                    result.put("isAvailable", String.valueOf(profile.isAvailable()));
                }
            }

            return result;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error retrieving user by ID: " + e.getMessage(), e);
            return null;
        }
    }

    // =========================================================================
    // 2. Blood Request Operations
    // =========================================================================

    @Override
    public String createBloodRequest(Map<String, String> requestData) {
        if (requestData == null) {
            return null;
        }

        int recipientId = parseId(requestData.getOrDefault("requesterId", requestData.get("recipientId")));
        if (recipientId <= 0) {
            ServerLogger.warn("Cannot create blood request: invalid recipientId");
            return null;
        }

        BloodGroup bg = toDbBloodGroup(requestData.get("bloodGroup"));
        if (bg == null) {
            ServerLogger.warn("Cannot create blood request: invalid bloodGroup: %s", requestData.get("bloodGroup"));
            return null;
        }

        int units = parseInt(requestData.getOrDefault("unitsNeeded", requestData.get("units_required")), 1);
        String hospital = requestData.getOrDefault("hospital", requestData.getOrDefault("hospital_name", "National Hospital"));
        String location = requestData.getOrDefault("city", requestData.getOrDefault("location", "Colombo"));
        String contact = requestData.getOrDefault("contactNumber", requestData.getOrDefault("phone", "+94-77-0000000"));
        UrgencyLevel urgency = toDbUrgencyLevel(requestData.get("urgency"));
        String description = requestData.getOrDefault("notes", requestData.get("description"));

        try {
            BloodRequest req = new BloodRequest(recipientId, bg, units, hospital.trim(), location.trim(),
                                                contact.trim(), urgency, description != null ? description.trim() : null);

            BloodRequest created = requestDAO.createBloodRequest(req);
            return (created != null && created.getRequestId() > 0) ? String.valueOf(created.getRequestId()) : null;
        } catch (ValidationException e) {
            ServerLogger.warn("Validation error creating blood request: %s", e.getMessage());
            return null;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error creating blood request: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<Map<String, String>> getBloodRequests(Map<String, String> filters) {
        try {
            List<BloodRequest> allRequests = requestDAO.getAllBloodRequests();
            Map<Integer, Integer> responseCounts = Collections.emptyMap();
            try {
                responseCounts = responseDAO.getResponseCountsByRequest();
            } catch (Exception ignored) {}

            List<Map<String, String>> results = new ArrayList<>();

            for (BloodRequest req : allRequests) {
                if (matchesFilter(req, filters)) {
                    Map<String, String> row = new HashMap<>();
                    row.put("bloodRequestId", String.valueOf(req.getRequestId()));
                    row.put("requestId", String.valueOf(req.getRequestId()));
                    row.put("requesterId", String.valueOf(req.getRecipientId()));
                    row.put("recipientId", String.valueOf(req.getRecipientId()));
                    row.put("recipientName", req.getRecipientName() != null ? req.getRecipientName() : "");
                    row.put("recipientEmail", req.getRecipientEmail() != null ? req.getRecipientEmail() : "");
                    row.put("contactNumber", req.getContactNumber() != null ? req.getContactNumber() : "");
                    row.put("bloodGroup", toBackendBloodGroup(req.getBloodGroup()));
                    row.put("bloodGroupSymbol", req.getBloodGroup().getSymbol());
                    row.put("unitsNeeded", String.valueOf(req.getUnitsRequired()));
                    row.put("hospital", req.getHospitalName());
                    row.put("city", req.getLocation());
                    row.put("urgency", toBackendUrgency(req.getUrgencyLevel()));
                    row.put("status", toBackendStatus(req.getRequestStatus()));
                    row.put("notes", req.getDescription() != null ? req.getDescription() : "");
                    row.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt() : "");
                    int respCount = responseCounts.getOrDefault(req.getRequestId(), 0);
                    row.put("responsesCount", String.valueOf(respCount));
                    row.put("responseCount", String.valueOf(respCount));
                    results.add(row);
                }
            }

            return results;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error retrieving blood requests: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateBloodRequestStatus(String bloodRequestId, String newStatus) {
        int reqId = parseId(bloodRequestId);
        if (reqId <= 0 || newStatus == null) {
            return false;
        }

        try {
            RequestStatus status = toDbRequestStatus(newStatus);
            return requestDAO.updateRequestStatus(reqId, status);
        } catch (IllegalArgumentException e) {
            ServerLogger.warn("Invalid request status value: %s", newStatus);
            return false;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error updating request status: " + e.getMessage(), e);
            return false;
        }
    }

    // =========================================================================
    // 3. Donor Matching & Availability
    // =========================================================================

    @Override
    public List<Map<String, String>> findEligibleDonors(List<String> compatibleBloodGroups, String city) {
        try {
            List<DonorProfile> availableDonors = donorDAO.searchAvailableDonors();
            List<Map<String, String>> results = new ArrayList<>();

            for (DonorProfile donor : availableDonors) {
                String donorBg = toBackendBloodGroup(donor.getBloodGroup());

                // Check blood group compatibility
                boolean bloodMatches = true;
                if (compatibleBloodGroups != null && !compatibleBloodGroups.isEmpty()) {
                    bloodMatches = false;
                    for (String compGroup : compatibleBloodGroups) {
                        if (BloodCompatibility.normalize(compGroup).equalsIgnoreCase(donorBg)) {
                            bloodMatches = true;
                            break;
                        }
                    }
                }

                // Check city filter
                boolean cityMatches = true;
                if (city != null && !city.trim().isEmpty()) {
                    cityMatches = donor.getLocation() != null &&
                            donor.getLocation().toLowerCase().contains(city.trim().toLowerCase());
                }

                if (bloodMatches && cityMatches) {
                    Map<String, String> row = new HashMap<>();
                    row.put("donorId", String.valueOf(donor.getDonorId()));
                    row.put("userId", String.valueOf(donor.getUserId()));
                    row.put("fullName", donor.getDonorName() != null ? donor.getDonorName() : "");
                    row.put("email", donor.getDonorEmail() != null ? donor.getDonorEmail() : "");
                    row.put("phone", donor.getDonorPhone() != null ? donor.getDonorPhone() : "");
                    row.put("bloodGroup", donorBg);
                    row.put("bloodGroupSymbol", donor.getBloodGroup().getSymbol());
                    row.put("city", donor.getLocation() != null ? donor.getLocation() : "");
                    row.put("isAvailable", String.valueOf(donor.isAvailable()));
                    results.add(row);
                }
            }

            return results;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error finding eligible donors: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateDonorAvailability(String userId, boolean isAvailable) {
        int uId = parseId(userId);
        if (uId <= 0) {
            return false;
        }

        try {
            DonorProfile profile = donorDAO.getDonorByUserId(uId);
            if (profile == null) {
                ServerLogger.warn("Cannot update availability: no donor profile for userId %d", uId);
                return false;
            }

            AvailabilityStatus status = isAvailable ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.UNAVAILABLE;
            return donorDAO.updateAvailability(profile.getDonorId(), status);
        } catch (DatabaseException e) {
            ServerLogger.error("Database error updating donor availability: " + e.getMessage(), e);
            return false;
        }
    }

    // =========================================================================
    // 4. Messaging Operations
    // =========================================================================

    @Override
    public String saveMessage(String senderId, String recipientId, String content, long timestamp) {
        int sId = parseId(senderId);
        int rId = parseId(recipientId);

        if (sId <= 0 || rId <= 0 || content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            Message msg = new Message(sId, rId, null, content.trim());
            Message saved = messageDAO.saveMessage(msg);
            return (saved != null && saved.getMessageId() > 0) ? String.valueOf(saved.getMessageId()) : null;
        } catch (ValidationException e) {
            ServerLogger.warn("Validation error saving message: %s", e.getMessage());
            return null;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error saving message: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<Map<String, String>> getConversationHistory(String user1, String user2, int limit) {
        int u1 = parseId(user1);
        int u2 = parseId(user2);

        if (u1 <= 0 || u2 <= 0) {
            return Collections.emptyList();
        }

        try {
            List<Message> history = messageDAO.getConversation(u1, u2);
            List<Map<String, String>> results = new ArrayList<>();

            for (Message m : history) {
                Map<String, String> row = new HashMap<>();
                row.put("messageId", String.valueOf(m.getMessageId()));
                row.put("senderId", String.valueOf(m.getSenderId()));
                row.put("recipientId", String.valueOf(m.getReceiverId()));
                row.put("content", m.getMessageContent());
                row.put("timestamp", m.getSentAt() != null ? m.getSentAt() : "");
                row.put("sentAt", m.getSentAt() != null ? m.getSentAt() : "");
                row.put("senderName", m.getSenderName() != null ? m.getSenderName() : "");
                row.put("receiverName", m.getReceiverName() != null ? m.getReceiverName() : "");
                results.add(row);
            }

            int safeLimit = limit <= 0 ? 50 : limit;
            if (results.size() > safeLimit) {
                return new ArrayList<>(results.subList(results.size() - safeLimit, results.size()));
            }
            return results;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error retrieving conversation history: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // 5. Notification Operations
    // =========================================================================

    @Override
    public boolean saveNotification(String targetUserId, String title, String message, String urgency, long timestamp) {
        int uId = parseId(targetUserId);
        if (uId <= 0) {
            return false;
        }

        NotificationType type = NotificationType.SYSTEM_ALERT;
        if (title != null && (title.toUpperCase().contains("BLOOD") || title.toUpperCase().contains("URGENT"))) {
            type = NotificationType.BLOOD_REQUEST;
        }

        String fullMessage = (title != null && !title.trim().isEmpty())
                ? title.trim() + ": " + (message != null ? message.trim() : "")
                : (message != null ? message.trim() : "Notification");

        try {
            Notification n = new Notification(uId, type, fullMessage, null);
            Notification created = notificationDAO.createNotification(n);
            return created != null && created.getNotificationId() > 0;
        } catch (ValidationException e) {
            ServerLogger.warn("Validation error saving notification: %s", e.getMessage());
            return false;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error saving notification: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Map<String, String>> getNotificationsForUser(String userId, int limit) {
        int uId = parseId(userId);
        if (uId <= 0) {
            return Collections.emptyList();
        }

        try {
            List<Notification> list = notificationDAO.getNotificationsByUser(uId);
            List<Map<String, String>> results = new ArrayList<>();

            int safeLimit = limit <= 0 ? 50 : limit;
            int count = 0;

            for (Notification n : list) {
                if (count >= safeLimit) {
                    break;
                }
                Map<String, String> row = new HashMap<>();
                row.put("notificationId", String.valueOf(n.getNotificationId()));
                row.put("userId", String.valueOf(n.getUserId()));
                row.put("notificationType", n.getNotificationType().name());
                row.put("title", n.getNotificationType().name());
                row.put("message", n.getMessage());
                row.put("urgency", n.getNotificationType() == NotificationType.BLOOD_REQUEST ? "URGENT" : "NORMAL");
                row.put("timestamp", n.getCreatedAt() != null ? n.getCreatedAt() : "");
                row.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt() : "");
                row.put("isRead", String.valueOf(n.isRead()));
                results.add(row);
                count++;
            }

            return results;
        } catch (DatabaseException e) {
            ServerLogger.error("Database error retrieving notifications: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // Helper & Enum Conversion Methods
    // =========================================================================

    public static BloodGroup toDbBloodGroup(String bgStr) {
        if (bgStr == null || bgStr.trim().isEmpty()) {
            return null;
        }
        String clean = bgStr.trim().toUpperCase()
                .replace(" ", "")
                .replace("_POS", "_POSITIVE")
                .replace("_NEG", "_NEGATIVE");
        try {
            return BloodGroup.fromString(clean);
        } catch (IllegalArgumentException e) {
            String sym = BloodCompatibility.toSymbol(bgStr);
            try {
                return BloodGroup.fromString(sym);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    public static String toBackendBloodGroup(BloodGroup bg) {
        if (bg == null) return "";
        return BloodCompatibility.normalize(bg.getSymbol());
    }

    public static UrgencyLevel toDbUrgencyLevel(String urgency) {
        if (urgency == null) return UrgencyLevel.MEDIUM;
        String u = urgency.trim().toUpperCase();
        switch (u) {
            case "CRITICAL":
                return UrgencyLevel.CRITICAL;
            case "URGENT":
            case "HIGH":
                return UrgencyLevel.HIGH;
            case "LOW":
                return UrgencyLevel.LOW;
            case "NORMAL":
            case "MEDIUM":
            default:
                return UrgencyLevel.MEDIUM;
        }
    }

    public static String toBackendUrgency(UrgencyLevel ul) {
        if (ul == null) return "NORMAL";
        switch (ul) {
            case CRITICAL:
                return "CRITICAL";
            case HIGH:
                return "URGENT";
            case LOW:
                return "LOW";
            case MEDIUM:
            default:
                return "NORMAL";
        }
    }

    public static RequestStatus toDbRequestStatus(String status) {
        if (status == null) return RequestStatus.PENDING;
        String s = status.trim().toUpperCase();
        switch (s) {
            case "FULFILLED":
            case "COMPLETED":
                return RequestStatus.COMPLETED;
            case "MATCHED":
                return RequestStatus.MATCHED;
            case "ACCEPTED":
                return RequestStatus.ACCEPTED;
            case "CANCELLED":
                return RequestStatus.CANCELLED;
            case "REJECTED":
                return RequestStatus.REJECTED;
            case "OPEN":
            case "PENDING":
            default:
                return RequestStatus.PENDING;
        }
    }

    public static String toBackendStatus(RequestStatus rs) {
        if (rs == null) return "PENDING";
        switch (rs) {
            case COMPLETED:
                return "FULFILLED";
            case MATCHED:
                return "MATCHED";
            case ACCEPTED:
                return "ACCEPTED";
            case CANCELLED:
                return "CANCELLED";
            case REJECTED:
                return "REJECTED";
            case PENDING:
            default:
                return "PENDING";
        }
    }

    private int parseId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            return -1;
        }
        String clean = idStr.trim()
                .replaceAll("(?i)^(usr|req|msg|notif|donor)[-_]?", "");
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseInt(String val, int defaultVal) {
        if (val == null) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean matchesFilter(BloodRequest req, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        if (filters.containsKey("bloodRequestId")) {
            int targetId = parseId(filters.get("bloodRequestId"));
            if (targetId > 0 && req.getRequestId() != targetId) {
                return false;
            }
        }

        if (filters.containsKey("requestId")) {
            int targetId = parseId(filters.get("requestId"));
            if (targetId > 0 && req.getRequestId() != targetId) {
                return false;
            }
        }

        if (filters.containsKey("status")) {
            String filterStatus = filters.get("status") != null ? filters.get("status").trim() : "";
            if (!filterStatus.isEmpty()) {
                RequestStatus expected = toDbRequestStatus(filterStatus);
                if (req.getRequestStatus() != expected) {
                    return false;
                }
            }
        }

        if (filters.containsKey("city") || filters.containsKey("location")) {
            String filterCity = filters.containsKey("city") ? filters.get("city") : filters.get("location");
            if (filterCity != null && !filterCity.trim().isEmpty()) {
                if (req.getLocation() == null || !req.getLocation().toLowerCase().contains(filterCity.trim().toLowerCase())) {
                    return false;
                }
            }
        }

        if (filters.containsKey("requesterId") || filters.containsKey("recipientId")) {
            String rIdStr = filters.containsKey("requesterId") ? filters.get("requesterId") : filters.get("recipientId");
            int expectedRecipient = parseId(rIdStr);
            if (expectedRecipient > 0 && req.getRecipientId() != expectedRecipient) {
                return false;
            }
        }

        if (filters.containsKey("bloodGroup")) {
            String bgStr = filters.get("bloodGroup") != null ? filters.get("bloodGroup").trim() : "";
            if (!bgStr.isEmpty()) {
                BloodGroup expectedBg = toDbBloodGroup(bgStr);
                if (expectedBg != null && req.getBloodGroup() != expectedBg) {
                    return false;
                }
            }
        }

        return true;
    }

    // =========================================================================
    // 6. Donor Response Operations
    // =========================================================================

    @Override
    public Map<String, String> recordDonorResponse(String requestId, String donorId, String status, String notes) {
        int reqId = parseId(requestId);
        int donId = parseId(donorId);
        if (reqId <= 0 || donId <= 0) return null;

        try {
            BloodRequest req = requestDAO.getRequestById(reqId);
            if (req == null) return null;

            int recipientId = req.getRecipientId();
            DonorResponse dr = responseDAO.recordResponse(reqId, donId, recipientId, status, notes);
            if (dr == null) return null;

            // If ACCEPTED, advance blood request status if still PENDING or MATCHED
            if ("ACCEPTED".equalsIgnoreCase(status)) {
                if (req.getRequestStatus() == RequestStatus.PENDING || req.getRequestStatus() == RequestStatus.MATCHED) {
                    requestDAO.updateRequestStatus(reqId, RequestStatus.ACCEPTED);
                }
            }

            return mapDonorResponseToRow(dr);
        } catch (Exception e) {
            ServerLogger.error("Error recording donor response: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public List<Map<String, String>> getDonorResponsesForRequest(String requestId) {
        int reqId = parseId(requestId);
        if (reqId <= 0) return Collections.emptyList();
        try {
            List<DonorResponse> list = responseDAO.getResponsesByRequest(reqId);
            List<Map<String, String>> results = new ArrayList<>();
            for (DonorResponse dr : list) {
                results.add(mapDonorResponseToRow(dr));
            }
            return results;
        } catch (Exception e) {
            ServerLogger.error("Error getting donor responses for request: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, String>> getDonorResponsesForRecipient(String recipientId) {
        int recId = parseId(recipientId);
        if (recId <= 0) return Collections.emptyList();
        try {
            List<DonorResponse> list = responseDAO.getResponsesByRecipient(recId);
            List<Map<String, String>> results = new ArrayList<>();
            for (DonorResponse dr : list) {
                results.add(mapDonorResponseToRow(dr));
            }
            return results;
        } catch (Exception e) {
            ServerLogger.error("Error getting donor responses for recipient: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, String> getDonorResponse(String requestId, String donorId) {
        int reqId = parseId(requestId);
        int donId = parseId(donorId);
        if (reqId <= 0 || donId <= 0) return null;
        try {
            DonorResponse dr = responseDAO.getResponse(reqId, donId);
            return dr != null ? mapDonorResponseToRow(dr) : null;
        } catch (Exception e) {
            ServerLogger.error("Error getting donor response: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<String, String> getDonorResponsesMapForDonor(String donorId) {
        int donId = parseId(donorId);
        if (donId <= 0) return Collections.emptyMap();
        try {
            Map<Integer, String> raw = responseDAO.getDonorResponsesMapForDonor(donId);
            Map<String, String> res = new HashMap<>();
            for (Map.Entry<Integer, String> e : raw.entrySet()) {
                res.put(String.valueOf(e.getKey()), e.getValue());
            }
            return res;
        } catch (Exception e) {
            ServerLogger.error("Error getting responses map for donor: " + e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    // =========================================================================
    // 7. Admin Operations & Statistics
    // =========================================================================

    @Override
    public List<Map<String, String>> getAllUsers(String searchQuery) {
        try {
            List<User> list = (searchQuery != null && !searchQuery.trim().isEmpty())
                    ? userDAO.searchUsers(searchQuery.trim())
                    : userDAO.getAllUsers();
            List<Map<String, String>> results = new ArrayList<>();
            for (User u : list) {
                Map<String, String> row = new HashMap<>();
                row.put("userId", String.valueOf(u.getUserId()));
                row.put("username", u.getEmail());
                row.put("fullName", u.getFullName());
                row.put("email", u.getEmail());
                row.put("phone", u.getPhone() != null ? u.getPhone() : "");
                row.put("role", u.getRole() != null ? u.getRole().name() : "DONOR");
                row.put("accountStatus", u.getAccountStatus() != null ? u.getAccountStatus().name() : "ACTIVE");
                row.put("status", u.getAccountStatus() != null ? u.getAccountStatus().name() : "ACTIVE");
                row.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt() : "");

                if (u.getRole() == UserRole.DONOR) {
                    DonorProfile dp = donorDAO.getDonorByUserId(u.getUserId());
                    if (dp != null) {
                        row.put("bloodGroup", toBackendBloodGroup(dp.getBloodGroup()));
                        row.put("bloodGroupSymbol", dp.getBloodGroup() != null ? dp.getBloodGroup().getSymbol() : "");
                        row.put("city", dp.getLocation() != null ? dp.getLocation() : "");
                        row.put("location", dp.getLocation() != null ? dp.getLocation() : "");
                        row.put("availability", dp.getAvailabilityStatus() != null ? dp.getAvailabilityStatus().name() : "AVAILABLE");
                        row.put("isAvailable", String.valueOf(dp.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE));
                        row.put("lastDonationDate", dp.getLastDonationDate() != null ? dp.getLastDonationDate() : "Never");
                    }
                }
                results.add(row);
            }
            return results;
        } catch (Exception e) {
            ServerLogger.error("Error retrieving all users: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateUserAccountStatus(String userId, String status) {
        int uId = parseId(userId);
        if (uId <= 0 || status == null) return false;
        try {
            AccountStatus as = AccountStatus.fromString(status);
            return userDAO.updateAccountStatus(uId, as);
        } catch (Exception e) {
            ServerLogger.error("Failed to update user account status: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteUserAccount(String userId) {
        int uId = parseId(userId);
        if (uId <= 0) return false;
        try {
            return userDAO.deleteUser(uId);
        } catch (Exception e) {
            ServerLogger.error("Failed to delete user account: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, String> getSystemStatistics() {
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, Integer> userStats = userDAO.getUserStatistics();
            Map<String, Integer> reqStats = requestDAO.getRequestStatistics();
            Map<String, Integer> bgStats = requestDAO.getBloodGroupCounts();

            if (userStats != null) {
                for (Map.Entry<String, Integer> e : userStats.entrySet()) {
                    result.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
            if (reqStats != null) {
                for (Map.Entry<String, Integer> e : reqStats.entrySet()) {
                    result.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
            if (bgStats != null) {
                for (Map.Entry<String, Integer> e : bgStats.entrySet()) {
                    String bgName = e.getKey();
                    result.put("bg_" + bgName, String.valueOf(e.getValue()));
                    String sym = BloodCompatibility.toSymbol(bgName);
                    if (!sym.isEmpty()) {
                        result.put("bgSymbol_" + sym, String.valueOf(e.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to fetch system statistics: " + e.getMessage(), e);
        }
        return result;
    }

    private Map<String, String> mapDonorResponseToRow(DonorResponse dr) {
        Map<String, String> row = new HashMap<>();
        row.put("responseId", String.valueOf(dr.getResponseId()));
        row.put("requestId", String.valueOf(dr.getRequestId()));
        row.put("bloodRequestId", String.valueOf(dr.getRequestId()));
        row.put("donorId", String.valueOf(dr.getDonorId()));
        row.put("recipientId", String.valueOf(dr.getRecipientId()));
        row.put("status", dr.getStatus());
        row.put("responseStatus", dr.getStatus());
        row.put("donorName", dr.getDonorName() != null ? dr.getDonorName() : "Donor");
        row.put("donorEmail", dr.getDonorEmail() != null ? dr.getDonorEmail() : "");
        row.put("donorPhone", dr.getDonorPhone() != null ? dr.getDonorPhone() : "");
        row.put("bloodGroup", dr.getDonorBloodGroup() != null ? BloodCompatibility.normalize(dr.getDonorBloodGroup()) : "");
        row.put("bloodGroupSymbol", dr.getDonorBloodGroup() != null ? BloodCompatibility.toSymbol(dr.getDonorBloodGroup()) : "");
        row.put("donorLocation", dr.getDonorLocation() != null ? dr.getDonorLocation() : "");
        row.put("donorCity", dr.getDonorLocation() != null ? dr.getDonorLocation() : "");
        row.put("donorAvailability", dr.getDonorAvailability() != null ? dr.getDonorAvailability() : "AVAILABLE");
        row.put("lastDonationDate", dr.getLastDonationDate() != null ? dr.getLastDonationDate() : "Never");
        row.put("hospitalName", dr.getHospitalName() != null ? dr.getHospitalName() : "");
        row.put("notes", dr.getNotes() != null ? dr.getNotes() : "");
        row.put("createdAt", dr.getCreatedAt() != null ? dr.getCreatedAt() : "");
        return row;
    }
}

package com.bloodlink.test;

import com.bloodlink.interfaces.IDatabaseService;
import com.bloodlink.services.BloodCompatibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory mock database service for testing Member 02 backend module.
 * Completely decoupled from Member 03's SQLite implementation.
 */
public class MockDatabaseService implements IDatabaseService {

    private final AtomicLong userSeq = new AtomicLong(100);
    private final AtomicLong reqSeq = new AtomicLong(500);
    private final AtomicLong msgSeq = new AtomicLong(1000);

    private final Map<String, Map<String, String>> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> usersById = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> bloodRequestsById = new ConcurrentHashMap<>();
    private final List<Map<String, String>> messageStore = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<Map<String, String>>> notificationsByUser = new ConcurrentHashMap<>();

    public MockDatabaseService() {
        seedSampleData();
    }

    private void seedSampleData() {
        // Seed default donors, hospital, recipient, and admin
        registerUserWithId("usr-alice", "alice", "alice123", "DONOR", "Alice Walker", "O_NEG", "Colombo", "0771234567", true);
        registerUserWithId("usr-bob", "bob", "bob123", "DONOR", "Bob Stone", "A_POS", "Kandy", "0772345678", true);
        registerUserWithId("usr-hosp", "hospital1", "hosp123", "HOSPITAL", "City General Hospital", "", "Colombo", "0112345678", false);
        registerUserWithId("usr-charlie", "charlie", "char123", "RECIPIENT", "Charlie Smith", "O_POS", "Colombo", "0773456789", false);
        registerUserWithId("usr-admin", "admin", "admin123", "ADMIN", "System Administrator", "", "Colombo", "0119998877", false);
    }

    private void registerUserWithId(String userId, String username, String password, String role,
                                   String fullName, String bloodGroup, String city, String phone, boolean available) {
        Map<String, String> u = new HashMap<>();
        u.put("userId", userId);
        u.put("username", username);
        u.put("password", password);
        u.put("role", role);
        u.put("fullName", fullName);
        u.put("bloodGroup", bloodGroup);
        u.put("city", city);
        u.put("phone", phone);
        u.put("isAvailable", Boolean.toString(available));

        usersByUsername.put(username, u);
        usersById.put(userId, u);
    }

    @Override
    public Map<String, String> authenticate(String username, String password) {
        Map<String, String> user = usersByUsername.get(username);
        if (user != null && user.get("password").equals(password)) {
            return new HashMap<>(user);
        }
        return null;
    }

    @Override
    public String registerUser(Map<String, String> userData) {
        String username = userData.get("username");
        if (username == null || usersByUsername.containsKey(username)) {
            return null; // Username already taken
        }

        String userId = "usr-" + userSeq.getAndIncrement();
        Map<String, String> record = new HashMap<>(userData);
        record.put("userId", userId);
        record.putIfAbsent("isAvailable", "true");

        usersByUsername.put(username, record);
        usersById.put(userId, record);
        return userId;
    }

    @Override
    public Map<String, String> getUserById(String userId) {
        if (userId == null) {
            return null;
        }
        Map<String, String> u = usersById.get(userId);
        return (u != null) ? new HashMap<>(u) : null;
    }

    @Override
    public String createBloodRequest(Map<String, String> requestData) {
        String requestId = "req-" + reqSeq.getAndIncrement();
        Map<String, String> record = new HashMap<>(requestData);
        record.put("bloodRequestId", requestId);
        record.putIfAbsent("status", "PENDING");
        bloodRequestsById.put(requestId, record);
        return requestId;
    }

    @Override
    public List<Map<String, String>> getBloodRequests(Map<String, String> filters) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Map<String, String> req : bloodRequestsById.values()) {
            boolean match = true;
            if (filters != null) {
                if (filters.containsKey("bloodRequestId") && !filters.get("bloodRequestId").equalsIgnoreCase(req.get("bloodRequestId"))) {
                    match = false;
                }
                if (filters.containsKey("status") && !filters.get("status").equalsIgnoreCase(req.get("status"))) {
                    match = false;
                }
                if (filters.containsKey("city") && !filters.get("city").equalsIgnoreCase(req.get("city"))) {
                    match = false;
                }
                if (filters.containsKey("requesterId") && !filters.get("requesterId").equalsIgnoreCase(req.get("requesterId"))) {
                    match = false;
                }
                if (filters.containsKey("bloodGroup") && !filters.get("bloodGroup").equalsIgnoreCase(req.get("bloodGroup"))) {
                    match = false;
                }
            }
            if (match) {
                list.add(new HashMap<>(req));
            }
        }
        return list;
    }

    @Override
    public boolean updateBloodRequestStatus(String bloodRequestId, String newStatus) {
        Map<String, String> req = bloodRequestsById.get(bloodRequestId);
        if (req != null) {
            req.put("status", newStatus);
            return true;
        }
        return false;
    }

    @Override
    public List<Map<String, String>> findEligibleDonors(List<String> compatibleBloodGroups, String city) {
        List<Map<String, String>> donors = new ArrayList<>();
        for (Map<String, String> user : usersById.values()) {
            if ("DONOR".equalsIgnoreCase(user.get("role")) && Boolean.parseBoolean(user.get("isAvailable"))) {
                String donorGroup = BloodCompatibility.normalize(user.get("bloodGroup"));
                if (compatibleBloodGroups.contains(donorGroup)) {
                    if (city == null || city.isEmpty() || city.equalsIgnoreCase(user.get("city"))) {
                        donors.add(new HashMap<>(user));
                    }
                }
            }
        }
        return donors;
    }

    @Override
    public boolean updateDonorAvailability(String userId, boolean isAvailable) {
        Map<String, String> user = usersById.get(userId);
        if (user != null) {
            user.put("isAvailable", Boolean.toString(isAvailable));
            return true;
        }
        return false;
    }

    @Override
    public String saveMessage(String senderId, String recipientId, String content, long timestamp) {
        String msgId = "msg-" + msgSeq.getAndIncrement();
        Map<String, String> m = new HashMap<>();
        m.put("messageId", msgId);
        m.put("senderId", senderId);
        m.put("recipientId", recipientId);
        m.put("content", content);
        m.put("timestamp", Long.toString(timestamp));
        messageStore.add(m);
        return msgId;
    }

    @Override
    public List<Map<String, String>> getConversationHistory(String user1, String user2, int limit) {
        List<Map<String, String>> result = new ArrayList<>();
        synchronized (messageStore) {
            for (Map<String, String> m : messageStore) {
                String s = m.get("senderId");
                String r = m.get("recipientId");
                if ((user1.equals(s) && user2.equals(r)) || (user1.equals(r) && user2.equals(s))) {
                    result.add(new HashMap<>(m));
                }
            }
        }
        if (result.size() > limit) {
            return result.subList(result.size() - limit, result.size());
        }
        return result;
    }

    @Override
    public boolean saveNotification(String targetUserId, String title, String message, String urgency, long timestamp) {
        Map<String, String> notif = new HashMap<>();
        notif.put("targetUserId", targetUserId);
        notif.put("title", title);
        notif.put("message", message);
        notif.put("urgency", urgency);
        notif.put("timestamp", Long.toString(timestamp));

        notificationsByUser.computeIfAbsent(targetUserId, k -> Collections.synchronizedList(new ArrayList<>())).add(notif);
        return true;
    }

    @Override
    public List<Map<String, String>> getNotificationsForUser(String userId, int limit) {
        List<Map<String, String>> list = notificationsByUser.getOrDefault(userId, Collections.emptyList());
        synchronized (list) {
            if (list.size() > limit) {
                return new ArrayList<>(list.subList(list.size() - limit, list.size()));
            }
            return new ArrayList<>(list);
        }
    }
}

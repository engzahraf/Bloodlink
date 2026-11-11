package com.bloodlink.client.network;

import com.bloodlink.client.models.*;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.client.utils.ValidationUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory Mock Data Service providing a fully functional offline demonstration mode.
 * Simulates server responses, in-memory updates, and blood compatibility logic.
 */
public class MockDataService {

    private static final MockDataService INSTANCE = new MockDataService();

    private final Map<String, User> mockUsers = new ConcurrentHashMap<>();
    private final Map<String, String> mockCredentials = new ConcurrentHashMap<>();
    private final List<BloodRequest> mockRequests = new CopyOnWriteArrayList<>();
    private final List<Donor> mockDonors = new CopyOnWriteArrayList<>();
    private final List<Notification> mockNotifications = new CopyOnWriteArrayList<>();
    private final List<Message> mockMessages = new CopyOnWriteArrayList<>();

    private final AtomicLong idGen = new AtomicLong(500);

    private MockDataService() {
        initDefaultMockData();
    }

    public static MockDataService getInstance() {
        return INSTANCE;
    }

    private void initDefaultMockData() {
        // Users
        User admin = new User("usr_admin", "admin", "ADMIN", "System Administrator", "admin@bloodlink.lk", "+94112345678", "Colombo", "O+");
        User donorAlice = new User("usr_donor_1", "alice", "DONOR", "Alice Walker", "alice@example.com", "+94771234567", "Colombo", "O+");
        User donorBob = new User("usr_donor_2", "bob", "DONOR", "Bob Martinez", "bob@example.com", "+94772345678", "Kandy", "A-");
        User donorCharlie = new User("usr_donor_3", "charlie", "DONOR", "Charlie Perera", "charlie@example.com", "+94773456789", "Galle", "B+");
        User recipientCarol = new User("usr_recip_1", "carol", "RECIPIENT", "Carol White", "carol@example.com", "+94774567890", "Colombo", "O+");
        User recipientDavid = new User("usr_recip_2", "david", "RECIPIENT", "David Miller", "david@example.com", "+94775678901", "Kandy", "A+");

        mockUsers.put("admin", admin);
        mockUsers.put("alice", donorAlice);
        mockUsers.put("bob", donorBob);
        mockUsers.put("charlie", donorCharlie);
        mockUsers.put("carol", recipientCarol);
        mockUsers.put("david", recipientDavid);

        mockCredentials.put("admin", "admin123");
        mockCredentials.put("alice", "secret123");
        mockCredentials.put("bob", "secret123");
        mockCredentials.put("charlie", "secret123");
        mockCredentials.put("carol", "secret123");
        mockCredentials.put("david", "secret123");

        // Donors
        mockDonors.add(new Donor("usr_donor_1", "Alice Walker", "O+", "+94771234567", "alice@example.com", "Colombo", true, "2026-08-15", 4));
        mockDonors.add(new Donor("usr_donor_2", "Bob Martinez", "A-", "+94772345678", "bob@example.com", "Kandy", true, "2026-06-20", 2));
        mockDonors.add(new Donor("usr_donor_3", "Charlie Perera", "B+", "+94773456789", "charlie@example.com", "Galle", false, "2026-10-01", 6));
        mockDonors.add(new Donor("usr_donor_4", "Dinesh Silva", "AB+", "+94776789012", "dinesh@example.com", "Colombo", true, "2026-05-10", 3));
        mockDonors.add(new Donor("usr_donor_5", "Eleni Fernando", "O-", "+94777890123", "eleni@example.com", "Kandy", true, "2026-09-12", 5));

        // Blood Requests
        mockRequests.add(new BloodRequest("req_br_101", "usr_recip_1", "Carol White", "O+", 2, "National Hospital of Sri Lanka", "Colombo", "URGENT", "OPEN", "Emergency surgery requirement", System.currentTimeMillis() - 7200000));
        mockRequests.add(new BloodRequest("req_br_102", "usr_recip_2", "David Miller", "A-", 1, "Kandy General Hospital", "Kandy", "NORMAL", "OPEN", "Scheduled transfusion", System.currentTimeMillis() - 86400000));
        mockRequests.add(new BloodRequest("req_br_103", "usr_recip_1", "Carol White", "B+", 3, "Teaching Hospital Karapitiya", "Galle", "CRITICAL", "OPEN", "Accident patient immediate need", System.currentTimeMillis() - 1800000));
        mockRequests.add(new BloodRequest("req_br_104", "usr_recip_2", "David Miller", "AB+", 1, "Colombo South Teaching Hospital", "Colombo", "NORMAL", "FULFILLED", "Donation completed successfully", System.currentTimeMillis() - 172800000));

        // Notifications
        mockNotifications.add(new Notification("notif_1", "Urgent O+ Needed", "Emergency O+ request created at National Hospital Colombo.", "URGENT", false, System.currentTimeMillis() - 1800000));
        mockNotifications.add(new Notification("notif_2", "Eligible Match Found", "A matching donor has been identified for request req_br_102.", "NORMAL", false, System.currentTimeMillis() - 43200000));
        mockNotifications.add(new Notification("notif_3", "Welcome to BloodLink", "Thank you for joining BloodLink. Every drop saves a life.", "INFO", true, System.currentTimeMillis() - 259200000));

        // Messages
        mockMessages.add(new Message("msg_1", "usr_recip_1", "Carol White", "usr_donor_1", "Alice Walker", "Hello Alice, I saw your profile. Can you donate O+ blood tomorrow?", System.currentTimeMillis() - 3600000, true));
        mockMessages.add(new Message("msg_2", "usr_donor_1", "Alice Walker", "usr_recip_1", "Carol White", "Yes Carol, I am available tomorrow morning at National Hospital.", System.currentTimeMillis() - 3000000, false));
    }

    public CompletableFuture<ParsedResponse> handleMockRequest(String wireString, String requestId) {
        CompletableFuture<ParsedResponse> future = new CompletableFuture<>();
        ParsedResponse req = ResponseHandler.parseWirePacket(wireString);
        String action = req.getAction();

        try {
            switch (action) {
                case "LOGIN": {
                    String username = req.get("username");
                    String password = req.get("password");
                    User user = mockUsers.get(username);
                    String expectedPass = mockCredentials.get(username);

                    if (user != null && expectedPass != null && expectedPass.equals(password)) {
                        Map<String, String> payload = new LinkedHashMap<>();
                        payload.put("userId", user.getUserId());
                        payload.put("username", user.getUsername());
                        payload.put("role", user.getRole());
                        payload.put("fullName", user.getFullName());
                        payload.put("bloodGroup", user.getBloodGroup());
                        payload.put("city", user.getCity());
                        payload.put("phone", user.getPhone());
                        payload.put("email", user.getEmail());
                        future.complete(new ParsedResponse("RES", "LOGIN", requestId, "SUCCESS", payload));
                    } else {
                        Map<String, String> payload = Collections.singletonMap("error", "Invalid username or password");
                        future.complete(new ParsedResponse("RES", "LOGIN", requestId, "UNAUTHORIZED", payload));
                    }
                    break;
                }

                case "REGISTER": {
                    String username = req.get("username");
                    String password = req.get("password");
                    String role = req.get("role");
                    String fullName = req.get("fullName");
                    String bg = req.getOrDefault("bloodGroup", "O+");
                    String city = req.getOrDefault("city", "Colombo");
                    String phone = req.getOrDefault("phone", "");
                    String email = req.getOrDefault("email", "");

                    if (mockUsers.containsKey(username)) {
                        Map<String, String> payload = Collections.singletonMap("error", "Username already exists");
                        future.complete(new ParsedResponse("RES", "REGISTER", requestId, "CONFLICT", payload));
                    } else {
                        String newId = "usr_" + idGen.incrementAndGet();
                        User newUser = new User(newId, username, role, fullName, email, phone, city, bg);
                        mockUsers.put(username, newUser);
                        mockCredentials.put(username, password);

                        if ("DONOR".equalsIgnoreCase(role)) {
                            mockDonors.add(new Donor(newId, fullName, bg, phone, email, city, true, "Never", 0));
                        }

                        Map<String, String> payload = new LinkedHashMap<>();
                        payload.put("userId", newId);
                        payload.put("role", role);
                        payload.put("message", "User registered successfully (Mock Mode)");
                        future.complete(new ParsedResponse("RES", "REGISTER", requestId, "SUCCESS", payload));
                    }
                    break;
                }

                case "GET_PROFILE": {
                    String userId = req.get("userId");
                    User found = null;
                    for (User u : mockUsers.values()) {
                        if (u.getUserId().equals(userId)) {
                            found = u;
                            break;
                        }
                    }
                    if (found != null) {
                        Map<String, String> p = new LinkedHashMap<>();
                        p.put("userId", found.getUserId());
                        p.put("username", found.getUsername());
                        p.put("role", found.getRole());
                        p.put("fullName", found.getFullName());
                        p.put("bloodGroup", found.getBloodGroup());
                        p.put("city", found.getCity());
                        p.put("phone", found.getPhone());
                        p.put("email", found.getEmail());
                        future.complete(new ParsedResponse("RES", "GET_PROFILE", requestId, "SUCCESS", p));
                    } else {
                        future.complete(new ParsedResponse("RES", "GET_PROFILE", requestId, "NOT_FOUND", Collections.singletonMap("error", "User not found")));
                    }
                    break;
                }

                case "CREATE_BLOOD_REQUEST": {
                    String bg = req.get("bloodGroup");
                    int units = req.getInt("units", 1);
                    String hosp = req.get("hospital");
                    String city = req.get("city");
                    String urg = req.get("urgency");
                    String notes = req.getOrDefault("notes", "");

                    String reqId = "req_br_" + idGen.incrementAndGet();
                    BloodRequest newBr = new BloodRequest(reqId, "current_user", "Current User", bg, units, hosp, city, urg, "OPEN", notes, System.currentTimeMillis());
                    mockRequests.add(0, newBr);

                    // Add a notification for donors
                    mockNotifications.add(0, new Notification("notif_" + idGen.incrementAndGet(), "New Blood Request: " + bg, hosp + " requires " + units + " units (" + urg + ")", urg, false, System.currentTimeMillis()));

                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("requestId", reqId);
                    p.put("message", "Blood request created successfully");
                    future.complete(new ParsedResponse("RES", "CREATE_BLOOD_REQUEST", requestId, "SUCCESS", p));
                    break;
                }

                case "GET_BLOOD_REQUESTS": {
                    String filterBg = req.get("bloodGroup");
                    String filterStatus = req.get("status");

                    List<String> items = new ArrayList<>();
                    for (BloodRequest br : mockRequests) {
                        if (filterStatus != null && !filterStatus.isEmpty() && !filterStatus.equalsIgnoreCase(br.getStatus())) {
                            continue;
                        }
                        if (filterBg != null && !filterBg.isEmpty() && !filterBg.equalsIgnoreCase(br.getBloodGroup())) {
                            continue;
                        }
                        items.add(serializeBloodRequest(br));
                    }

                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("count", Integer.toString(items.size()));
                    p.put("requests_list", String.join("~", items));
                    future.complete(new ParsedResponse("RES", "GET_BLOOD_REQUESTS", requestId, "SUCCESS", p));
                    break;
                }

                case "MATCH_DONORS": {
                    String bg = req.get("bloodGroup");
                    String city = req.get("city");

                    List<String> items = new ArrayList<>();
                    for (Donor d : mockDonors) {
                        if (city != null && !city.isEmpty() && !d.getCity().equalsIgnoreCase(city)) {
                            continue;
                        }
                        // Simple compatibility: same blood group or universal O-
                        if (bg != null && !bg.isEmpty() && !d.getBloodGroup().equalsIgnoreCase(bg) && !"O-".equalsIgnoreCase(d.getBloodGroup()) && !"O_NEG".equalsIgnoreCase(d.getBloodGroup())) {
                            continue;
                        }
                        items.add(serializeDonor(d));
                    }

                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("count", Integer.toString(items.size()));
                    p.put("donors_list", String.join("~", items));
                    future.complete(new ParsedResponse("RES", "MATCH_DONORS", requestId, "SUCCESS", p));
                    break;
                }

                case "UPDATE_DONOR_AVAILABILITY": {
                    String userId = req.get("userId");
                    boolean avail = req.getBoolean("available", true);
                    for (Donor d : mockDonors) {
                        if (d.getUserId().equals(userId)) {
                            d.setAvailable(avail);
                            break;
                        }
                    }
                    Map<String, String> p = Collections.singletonMap("message", "Availability updated");
                    future.complete(new ParsedResponse("RES", "UPDATE_DONOR_AVAILABILITY", requestId, "SUCCESS", p));
                    break;
                }

                case "SEND_MESSAGE": {
                    String recipId = req.get("recipientId");
                    String content = req.get("content");
                    String msgId = "msg_" + idGen.incrementAndGet();

                    Message msg = new Message(msgId, "current_user", "Current User", recipId, recipId, content, System.currentTimeMillis(), false);
                    mockMessages.add(msg);

                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("messageId", msgId);
                    p.put("message", "Message sent successfully");
                    future.complete(new ParsedResponse("RES", "SEND_MESSAGE", requestId, "SUCCESS", p));
                    break;
                }

                case "GET_MESSAGES": {
                    List<String> items = new ArrayList<>();
                    for (Message m : mockMessages) {
                        items.add(serializeMessage(m));
                    }
                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("count", Integer.toString(items.size()));
                    p.put("messages_list", String.join("~", items));
                    future.complete(new ParsedResponse("RES", "GET_MESSAGES", requestId, "SUCCESS", p));
                    break;
                }

                case "GET_NOTIFICATIONS": {
                    List<String> items = new ArrayList<>();
                    for (Notification n : mockNotifications) {
                        items.add(serializeNotification(n));
                    }
                    Map<String, String> p = new LinkedHashMap<>();
                    p.put("count", Integer.toString(items.size()));
                    p.put("notifications_list", String.join("~", items));
                    future.complete(new ParsedResponse("RES", "GET_NOTIFICATIONS", requestId, "SUCCESS", p));
                    break;
                }

                case "PING":
                default: {
                    future.complete(new ParsedResponse("RES", "PONG", requestId, "SUCCESS", Collections.emptyMap()));
                    break;
                }
            }
        } catch (Exception e) {
            future.complete(new ParsedResponse("RES", action, requestId, "INTERNAL_ERROR", Collections.singletonMap("error", e.getMessage())));
        }

        return future;
    }

    private String enc(String val) {
        if (val == null) return "";
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private String serializeBloodRequest(BloodRequest br) {
        return "requestId=" + enc(br.getRequestId()) +
                "&recipientId=" + enc(br.getRecipientId()) +
                "&recipientName=" + enc(br.getRecipientName()) +
                "&bloodGroup=" + enc(br.getBloodGroup()) +
                "&units=" + br.getUnitsRequired() +
                "&hospital=" + enc(br.getHospital()) +
                "&city=" + enc(br.getCity()) +
                "&urgency=" + enc(br.getUrgency()) +
                "&status=" + enc(br.getStatus()) +
                "&notes=" + enc(br.getNotes()) +
                "&createdAt=" + br.getCreatedAt();
    }

    private String serializeDonor(Donor d) {
        return "userId=" + enc(d.getUserId()) +
                "&fullName=" + enc(d.getFullName()) +
                "&bloodGroup=" + enc(d.getBloodGroup()) +
                "&phone=" + enc(d.getPhone()) +
                "&email=" + enc(d.getEmail()) +
                "&city=" + enc(d.getCity()) +
                "&available=" + d.isAvailable() +
                "&lastDonationDate=" + enc(d.getLastDonationDate()) +
                "&totalDonations=" + d.getTotalDonations();
    }

    private String serializeNotification(Notification n) {
        return "notificationId=" + enc(n.getNotificationId()) +
                "&title=" + enc(n.getTitle()) +
                "&message=" + enc(n.getMessage()) +
                "&urgency=" + enc(n.getUrgency()) +
                "&read=" + n.isRead() +
                "&timestamp=" + n.getTimestamp();
    }

    private String serializeMessage(Message m) {
        return "messageId=" + enc(m.getMessageId()) +
                "&senderId=" + enc(m.getSenderId()) +
                "&senderName=" + enc(m.getSenderName()) +
                "&recipientId=" + enc(m.getRecipientId()) +
                "&recipientName=" + enc(m.getRecipientName()) +
                "&content=" + enc(m.getContent()) +
                "&timestamp=" + m.getTimestamp();
    }

    public List<User> getAllMockUsers() {
        return new ArrayList<>(mockUsers.values());
    }

    public List<BloodRequest> getAllMockRequests() {
        return new ArrayList<>(mockRequests);
    }
}

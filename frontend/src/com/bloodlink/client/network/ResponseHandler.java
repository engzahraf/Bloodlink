package com.bloodlink.client.network;

import com.bloodlink.client.models.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses wire protocol responses received from Member 02's backend server.
 * Handles decoding URL-encoded key-value payloads and transforming them into domain models.
 */
public final class ResponseHandler {

    private ResponseHandler() {
    }

    public static class ParsedResponse {
        private final String type;
        private final String action;
        private final String requestId;
        private final String status;
        private final Map<String, String> payload;

        public ParsedResponse(String type, String action, String requestId, String status, Map<String, String> payload) {
            this.type = type != null ? type : "";
            this.action = action != null ? action : "";
            this.requestId = requestId != null ? requestId : "";
            this.status = status != null ? status : "";
            this.payload = payload != null ? payload : Collections.emptyMap();
        }

        public String getType() {
            return type;
        }

        public String getAction() {
            return action;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getStatus() {
            return status;
        }

        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }

        public String get(String key) {
            return payload.get(key);
        }

        public String getOrDefault(String key, String def) {
            return payload.getOrDefault(key, def);
        }

        public int getInt(String key, int def) {
            String val = get(key);
            if (val == null) return def;
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }

        public long getLong(String key, long def) {
            String val = get(key);
            if (val == null) return def;
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }

        public boolean getBoolean(String key, boolean def) {
            String val = get(key);
            if (val == null) return def;
            return Boolean.parseBoolean(val.trim());
        }

        public String getError() {
            return getOrDefault("error", "Operation failed with status: " + status);
        }

        public Map<String, String> getPayload() {
            return Collections.unmodifiableMap(payload);
        }

        public List<String> getList(String key) {
            String raw = get(key);
            List<String> list = new ArrayList<>();
            if (raw == null || raw.isEmpty()) return list;
            String[] tokens = raw.split("~");
            for (String t : tokens) {
                if (!t.isEmpty()) {
                    try {
                        list.add(URLDecoder.decode(t, StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        list.add(t);
                    }
                }
            }
            return list;
        }

        @Override
        public String toString() {
            return "ParsedResponse{" +
                    "type='" + type + '\'' +
                    ", action='" + action + '\'' +
                    ", requestId='" + requestId + '\'' +
                    ", status='" + status + '\'' +
                    ", payload=" + payload +
                    '}';
        }
    }

    public static ParsedResponse parseWirePacket(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ParsedResponse("ERROR", "", "", "BAD_REQUEST", Collections.singletonMap("error", "Empty packet"));
        }

        String[] parts = raw.split("\\|", 5);
        String type = parts.length > 0 ? parts[0].trim() : "";
        String action = parts.length > 1 ? parts[1].trim() : "";
        String reqId = parts.length > 2 ? parts[2].trim() : "";
        String status = parts.length > 3 ? parts[3].trim() : "";
        String rawPayload = parts.length > 4 ? parts[4].trim() : "";

        Map<String, String> map = parsePayloadMap(rawPayload);
        return new ParsedResponse(type, action, reqId, status, map);
    }

    public static Map<String, String> parsePayloadMap(String rawPayload) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawPayload == null || rawPayload.isEmpty()) {
            return map;
        }

        String[] pairs = rawPayload.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = pair.substring(0, idx);
                String v = pair.substring(idx + 1);
                try {
                    String decK = URLDecoder.decode(k, StandardCharsets.UTF_8);
                    String decV = URLDecoder.decode(v, StandardCharsets.UTF_8);
                    map.put(decK, decV);
                } catch (Exception e) {
                    map.put(k, v);
                }
            } else if (idx == -1) {
                try {
                    map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                } catch (Exception e) {
                    map.put(pair, "");
                }
            }
        }
        return map;
    }

    public static User parseUser(ParsedResponse response) {
        if (!response.isSuccess()) return null;
        return new User(
                response.getOrDefault("userId", ""),
                response.getOrDefault("username", ""),
                response.getOrDefault("role", "DONOR"),
                response.getOrDefault("fullName", response.get("username")),
                response.getOrDefault("email", ""),
                response.getOrDefault("phone", ""),
                response.getOrDefault("city", ""),
                response.getOrDefault("bloodGroup", "")
        );
    }

    public static List<BloodRequest> parseBloodRequests(ParsedResponse response) {
        List<BloodRequest> list = new ArrayList<>();
        List<String> items = response.getList("requests_list");
        for (String itemStr : items) {
            Map<String, String> m = parsePayloadMap(itemStr);
            if (!m.isEmpty()) {
                BloodRequest req = new BloodRequest(
                        m.getOrDefault("requestId", m.get("id")),
                        m.getOrDefault("recipientId", ""),
                        m.getOrDefault("recipientName", ""),
                        m.getOrDefault("bloodGroup", ""),
                        parseIntSafe(m.get("units"), 1),
                        m.getOrDefault("hospital", ""),
                        m.getOrDefault("city", ""),
                        m.getOrDefault("urgency", "NORMAL"),
                        m.getOrDefault("status", "OPEN"),
                        m.getOrDefault("notes", ""),
                        parseLongSafe(m.get("createdAt"), System.currentTimeMillis())
                );
                list.add(req);
            }
        }
        return list;
    }

    public static List<Donor> parseDonors(ParsedResponse response) {
        List<Donor> list = new ArrayList<>();
        List<String> items = response.getList("donors_list");
        for (String itemStr : items) {
            Map<String, String> m = parsePayloadMap(itemStr);
            if (!m.isEmpty()) {
                Donor d = new Donor(
                        m.getOrDefault("userId", m.get("id")),
                        m.getOrDefault("fullName", m.get("name")),
                        m.getOrDefault("bloodGroup", ""),
                        m.getOrDefault("phone", ""),
                        m.getOrDefault("email", ""),
                        m.getOrDefault("city", ""),
                        parseBooleanSafe(m.get("available"), true),
                        m.getOrDefault("lastDonationDate", "Never"),
                        parseIntSafe(m.get("totalDonations"), 0)
                );
                list.add(d);
            }
        }
        return list;
    }

    public static List<Notification> parseNotifications(ParsedResponse response) {
        List<Notification> list = new ArrayList<>();
        List<String> items = response.getList("notifications_list");
        for (String itemStr : items) {
            Map<String, String> m = parsePayloadMap(itemStr);
            if (!m.isEmpty()) {
                Notification n = new Notification(
                        m.getOrDefault("notificationId", m.get("id")),
                        m.getOrDefault("title", "Alert"),
                        m.getOrDefault("message", ""),
                        m.getOrDefault("urgency", "INFO"),
                        parseBooleanSafe(m.get("read"), false),
                        parseLongSafe(m.get("timestamp"), System.currentTimeMillis())
                );
                list.add(n);
            }
        }
        return list;
    }

    public static List<Message> parseMessages(ParsedResponse response, String currentUserId) {
        List<Message> list = new ArrayList<>();
        List<String> items = response.getList("messages_list");
        for (String itemStr : items) {
            Map<String, String> m = parsePayloadMap(itemStr);
            if (!m.isEmpty()) {
                String senderId = m.getOrDefault("senderId", "");
                boolean isIncoming = currentUserId != null && !currentUserId.equals(senderId);
                Message msg = new Message(
                        m.getOrDefault("messageId", m.get("id")),
                        senderId,
                        m.getOrDefault("senderName", ""),
                        m.getOrDefault("recipientId", ""),
                        m.getOrDefault("recipientName", ""),
                        m.getOrDefault("content", ""),
                        parseLongSafe(m.get("timestamp"), System.currentTimeMillis()),
                        isIncoming
                );
                list.add(msg);
            }
        }
        return list;
    }

    private static int parseIntSafe(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static long parseLongSafe(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; }
    }

    private static boolean parseBooleanSafe(String s, boolean def) {
        if (s == null) return def;
        return Boolean.parseBoolean(s.trim());
    }
}

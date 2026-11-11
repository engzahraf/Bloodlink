package com.bloodlink.client.network;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds protocol-compliant wire strings conforming strictly to Member 02's
 * BloodLink TCP Socket Protocol Specification.
 *
 * Wire format: TYPE|ACTION|REQUEST_ID|STATUS|PAYLOAD
 * Payloads: URL-encoded key-value pairs (key1=val1&key2=val2)
 */
public final class RequestBuilder {

    private RequestBuilder() {
    }

    private static String encode(String val) {
        if (val == null) return "";
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private static String formatPayload(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }
        return sb.toString();
    }

    public static String buildRawPacket(String type, String action, String requestId, String status, Map<String, String> payloadParams) {
        String t = type != null ? type : "REQ";
        String a = action != null ? action : "";
        String r = requestId != null ? requestId : "";
        String s = status != null ? status : "";
        String p = formatPayload(payloadParams);
        return t + "|" + a + "|" + r + "|" + s + "|" + p;
    }

    public static String buildLoginRequest(String requestId, String username, String password) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("username", username);
        params.put("password", password);
        return buildRawPacket("REQ", "LOGIN", requestId, "", params);
    }

    public static String buildLogoutRequest(String requestId, String username) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("username", username);
        return buildRawPacket("REQ", "LOGOUT", requestId, "", params);
    }

    public static String buildRegisterRequest(String requestId, String username, String password, String role,
                                              String fullName, String bloodGroup, String city, String phone, String email) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("role", role);
        params.put("fullName", fullName);
        if (bloodGroup != null && !bloodGroup.isEmpty()) {
            params.put("bloodGroup", bloodGroup);
        }
        params.put("city", city);
        params.put("phone", phone);
        if (email != null && !email.isEmpty()) {
            params.put("email", email);
        }
        return buildRawPacket("REQ", "REGISTER", requestId, "", params);
    }

    public static String buildGetProfileRequest(String requestId, String userId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("userId", userId);
        return buildRawPacket("REQ", "GET_PROFILE", requestId, "", params);
    }

    public static String buildCreateBloodRequest(String requestId, String bloodGroup, int units, String hospital,
                                                 String city, String urgency, String notes) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("bloodGroup", bloodGroup);
        params.put("units", Integer.toString(units));
        params.put("hospital", hospital);
        params.put("city", city);
        params.put("urgency", urgency);
        if (notes != null && !notes.isEmpty()) {
            params.put("notes", notes);
        }
        return buildRawPacket("REQ", "CREATE_BLOOD_REQUEST", requestId, "", params);
    }

    public static String buildGetBloodRequests(String requestId, String status, String bloodGroup, String city) {
        Map<String, String> params = new LinkedHashMap<>();
        if (status != null && !status.isEmpty()) {
            params.put("status", status);
        }
        if (bloodGroup != null && !bloodGroup.isEmpty()) {
            params.put("bloodGroup", bloodGroup);
        }
        if (city != null && !city.isEmpty()) {
            params.put("city", city);
        }
        return buildRawPacket("REQ", "GET_BLOOD_REQUESTS", requestId, "", params);
    }

    public static String buildUpdateBloodRequestStatus(String requestId, String bloodRequestId, String newStatus) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("requestId", bloodRequestId);
        params.put("status", newStatus);
        return buildRawPacket("REQ", "UPDATE_BLOOD_REQUEST_STATUS", requestId, "", params);
    }

    public static String buildMatchDonors(String requestId, String bloodGroup, String city) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("bloodGroup", bloodGroup);
        if (city != null && !city.isEmpty()) {
            params.put("city", city);
        }
        return buildRawPacket("REQ", "MATCH_DONORS", requestId, "", params);
    }

    public static String buildUpdateDonorAvailability(String requestId, String userId, boolean isAvailable) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("userId", userId);
        params.put("available", Boolean.toString(isAvailable));
        return buildRawPacket("REQ", "UPDATE_DONOR_AVAILABILITY", requestId, "", params);
    }

    public static String buildSendMessage(String requestId, String recipientId, String content) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("recipientId", recipientId);
        params.put("content", content);
        return buildRawPacket("REQ", "SEND_MESSAGE", requestId, "", params);
    }

    public static String buildGetMessages(String requestId, String contactUserId, int limit) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("contactUserId", contactUserId);
        params.put("limit", Integer.toString(limit > 0 ? limit : 50));
        return buildRawPacket("REQ", "GET_MESSAGES", requestId, "", params);
    }

    public static String buildGetNotifications(String requestId, int limit) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("limit", Integer.toString(limit > 0 ? limit : 20));
        return buildRawPacket("REQ", "GET_NOTIFICATIONS", requestId, "", params);
    }

    public static String buildPing(String requestId) {
        return buildRawPacket("REQ", "PING", requestId, "", null);
    }
}

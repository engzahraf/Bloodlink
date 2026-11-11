package com.bloodlink.protocol;

/**
 * Standard protocol action constants used in BloodLink requests, responses, and events.
 */
public final class ProtocolAction {

    // Authentication and profile actions
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String REGISTER = "REGISTER";
    public static final String GET_PROFILE = "GET_PROFILE";

    // Blood request and inventory actions
    public static final String CREATE_BLOOD_REQUEST = "CREATE_BLOOD_REQUEST";
    public static final String GET_BLOOD_REQUESTS = "GET_BLOOD_REQUESTS";
    public static final String UPDATE_BLOOD_REQUEST_STATUS = "UPDATE_BLOOD_REQUEST_STATUS";

    // Donor matching actions
    public static final String MATCH_DONORS = "MATCH_DONORS";
    public static final String UPDATE_DONOR_AVAILABILITY = "UPDATE_DONOR_AVAILABILITY";

    // Real-time notifications and alerts
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String URGENT_ALERT = "URGENT_ALERT";
    public static final String GET_NOTIFICATIONS = "GET_NOTIFICATIONS";

    // Donor-recipient direct messaging
    public static final String SEND_MESSAGE = "SEND_MESSAGE";
    public static final String RECEIVE_MESSAGE = "RECEIVE_MESSAGE";
    public static final String GET_MESSAGES = "GET_MESSAGES";

    // Heartbeat and diagnostic actions
    public static final String PING = "PING";
    public static final String PONG = "PONG";

    private ProtocolAction() {
        // Constants class
    }
}

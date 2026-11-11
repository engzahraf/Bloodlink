package com.bloodlink.protocol;

import java.util.Objects;

/**
 * Represents a framed message packet in the BloodLink TCP socket protocol.
 * Wire format:
 * TYPE|ACTION|REQUEST_ID|STATUS|PAYLOAD
 */
public class Packet {

    private final MessageType type;
    private final String action;
    private final String requestId;
    private final ResponseStatus status;
    private final ProtocolPayload payload;

    public Packet(MessageType type, String action, String requestId, ResponseStatus status, ProtocolPayload payload) {
        this.type = Objects.requireNonNull(type, "MessageType cannot be null");
        this.action = action != null ? action.trim() : "";
        this.requestId = requestId != null ? requestId.trim() : "";
        this.status = status;
        this.payload = payload != null ? payload : ProtocolPayload.empty();
    }

    public static Packet createRequest(String action, String requestId, ProtocolPayload payload) {
        return new Packet(MessageType.REQ, action, requestId, null, payload);
    }

    public static Packet createSuccessResponse(String action, String requestId, ProtocolPayload payload) {
        return new Packet(MessageType.RES, action, requestId, ResponseStatus.SUCCESS, payload);
    }

    public static Packet createErrorResponse(String action, String requestId, ResponseStatus status, String errorMessage) {
        ProtocolPayload payload = ProtocolPayload.empty().set("error", errorMessage != null ? errorMessage : "Unknown error");
        return new Packet(MessageType.RES, action, requestId, status != null ? status : ResponseStatus.INTERNAL_ERROR, payload);
    }

    public static Packet createEvent(String action, String eventId, ProtocolPayload payload) {
        return new Packet(MessageType.EVENT, action, eventId, null, payload);
    }

    public static Packet createMessage(String msgId, ProtocolPayload payload) {
        return new Packet(MessageType.MSG, ProtocolAction.SEND_MESSAGE, msgId, null, payload);
    }

    public static Packet createPing(String requestId) {
        return new Packet(MessageType.REQ, ProtocolAction.PING, requestId, null, ProtocolPayload.empty());
    }

    public static Packet createPong(String requestId) {
        return new Packet(MessageType.RES, ProtocolAction.PONG, requestId, ResponseStatus.SUCCESS, ProtocolPayload.empty());
    }

    public MessageType getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    public String getRequestId() {
        return requestId;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public ProtocolPayload getPayload() {
        return payload;
    }

    public boolean isSuccess() {
        return type == MessageType.RES && status == ResponseStatus.SUCCESS;
    }

    /**
     * Serializes this packet to the wire format:
     * TYPE|ACTION|REQUEST_ID|STATUS|PAYLOAD
     */
    public String toWireString() {
        String statusStr = status != null ? status.name() : "";
        return type.name() + "|" + action + "|" + requestId + "|" + statusStr + "|" + payload.serialize();
    }

    /**
     * Parses a packet from the wire string.
     * Throws MalformedPacketException if framing rules are violated.
     */
    public static Packet fromWireString(String raw) throws MalformedPacketException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new MalformedPacketException("Packet wire string cannot be null or empty");
        }

        // Limit split to 5 tokens: TYPE, ACTION, REQUEST_ID, STATUS, PAYLOAD
        String[] parts = raw.split("\\|", 5);
        if (parts.length < 4) {
            throw new MalformedPacketException("Packet missing required framing tokens. Got: " + raw);
        }

        MessageType type;
        try {
            type = MessageType.valueOf(parts[0].trim());
        } catch (IllegalArgumentException e) {
            throw new MalformedPacketException("Invalid message type: " + parts[0], e);
        }

        String action = parts[1].trim();
        String requestId = parts[2].trim();
        String rawStatus = parts[3].trim();

        ResponseStatus status = null;
        if (!rawStatus.isEmpty()) {
            try {
                status = ResponseStatus.valueOf(rawStatus);
            } catch (IllegalArgumentException e) {
                throw new MalformedPacketException("Invalid response status: " + rawStatus, e);
            }
        }

        String rawPayload = (parts.length >= 5) ? parts[4] : "";
        ProtocolPayload payload = ProtocolPayload.parse(rawPayload);

        return new Packet(type, action, requestId, status, payload);
    }

    @Override
    public String toString() {
        return toWireString();
    }
}

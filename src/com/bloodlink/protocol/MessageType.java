package com.bloodlink.protocol;

/**
 * High-level message types supported by the BloodLink wire protocol.
 */
public enum MessageType {
    REQ,    // Client-to-server request
    RES,    // Server-to-client response
    EVENT,  // Server-to-client push notification / alert
    MSG,    // Peer-to-peer / donor-recipient chat message
    ERROR   // Protocol or framing error
}

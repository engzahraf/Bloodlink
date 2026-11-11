package com.bloodlink.services;

import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.server.ClientHandler;

/**
 * Service interface for authentication, registration, and session management.
 */
public interface IAuthService {

    Packet handleLogin(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleRegister(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleLogout(ClientHandler client, String requestId);

    Packet handleGetProfile(ClientHandler client, String requestId, ProtocolPayload payload);
}

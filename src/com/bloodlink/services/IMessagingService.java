package com.bloodlink.services;

import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.server.ClientHandler;

/**
 * Service interface for direct donor-recipient messaging and chat history retrieval.
 */
public interface IMessagingService {

    Packet handleSendMessage(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleGetMessages(ClientHandler client, String requestId, ProtocolPayload payload);
}

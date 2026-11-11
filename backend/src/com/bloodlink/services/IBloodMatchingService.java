package com.bloodlink.services;

import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.server.ClientHandler;

/**
 * Service interface for blood requests, donor matching algorithms, and donor availability.
 */
public interface IBloodMatchingService {

    Packet handleCreateBloodRequest(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleMatchDonors(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleGetBloodRequests(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleUpdateBloodRequestStatus(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleUpdateDonorAvailability(ClientHandler client, String requestId, ProtocolPayload payload);
}

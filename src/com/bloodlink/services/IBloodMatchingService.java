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

    Packet handleAcceptRequest(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleRejectRequest(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleGetRequestResponses(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleGetDonorResponses(ClientHandler client, String requestId, ProtocolPayload payload);

    // Admin Operations
    Packet handleAdminGetUsers(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleAdminBlockUser(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleAdminUnblockUser(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleAdminDeleteUser(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleAdminCancelRequest(ClientHandler client, String requestId, ProtocolPayload payload);

    Packet handleAdminGetStats(ClientHandler client, String requestId, ProtocolPayload payload);
}

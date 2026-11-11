package com.bloodlink.test;

import com.bloodlink.protocol.MessageType;
import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolAction;
import com.bloodlink.protocol.ProtocolCodec;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.protocol.ResponseStatus;
import com.bloodlink.server.BloodLinkServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard Java verification test suite for Stage 1:
 * - Server initialization & port binding
 * - Multi-client concurrent connections
 * - Delimiter and special character safety in payloads
 * - Request/Response protocol framing with request IDs
 * - Malformed request error handling
 * - Client disconnect cleanup
 * - Server shutdown and resource deallocation
 */
public class Stage1NetworkTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   RUNNING STAGE 1 NETWORKING & PROTOCOL TESTS   ");
        System.out.println("=================================================");

        try {
            testPayloadDelimiterSafety();
            testServerLifecycleAndMultiClient();
        } catch (Throwable t) {
            System.err.println("Unexpected test failure: " + t.getMessage());
            t.printStackTrace();
        }

        System.out.println("=================================================");
        System.out.printf("Test Results: %d/%d passed (%.1f%%)%n",
                testsPassed, testsRun, (testsRun > 0 ? (testsPassed * 100.0 / testsRun) : 0));
        System.out.println("=================================================");

        if (testsPassed != testsRun) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            System.err.println("[FAIL] " + testName);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        testsRun++;
        boolean match = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        if (match) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            System.err.println("[FAIL] " + testName + " -> Expected: <" + expected + ">, Got: <" + actual + ">");
        }
    }

    private static void testPayloadDelimiterSafety() {
        System.out.println("\n--- Testing Delimiter Safety & Special Character Preservation ---");

        ProtocolPayload payload = new ProtocolPayload();
        String complexValue = "User|With&Special=Chars\nAnd\tTabs;Also|Pipes and spaces! 100% pure";
        payload.set("notes", complexValue);
        payload.set("count", 42);

        String serialized = payload.serialize();
        // Serialized string should not contain raw unencoded pipes or newlines
        assertTrue("Serialized payload does not contain raw pipes", !serialized.contains("|"));
        assertTrue("Serialized payload does not contain raw newlines", !serialized.contains("\n"));

        ProtocolPayload parsed = ProtocolPayload.parse(serialized);
        assertEquals("Complex string preserved exactly across serialize/parse",
                complexValue, parsed.get("notes"));
        assertEquals("Integer preserved", 42, parsed.getInt("count", 0));
    }

    private static void testServerLifecycleAndMultiClient() throws Exception {
        System.out.println("\n--- Testing Server Sockets, Multi-Client & Framing ---");

        // Use port 0 for dynamic ephemeral port allocation to avoid port conflicts
        BloodLinkServer server = new BloodLinkServer(0);
        server.startAsync();

        // Give the acceptor a moment to bind
        Thread.sleep(200);

        int boundPort = server.getPort();
        assertTrue("Server is running", server.isRunning());
        assertTrue("Server bound to a valid local port (" + boundPort + ")", boundPort > 0);

        int clientCount = 5;
        List<Socket> clientSockets = new ArrayList<>();
        List<DataInputStream> inStreams = new ArrayList<>();
        List<DataOutputStream> outStreams = new ArrayList<>();

        for (int i = 0; i < clientCount; i++) {
            Socket s = new Socket("127.0.0.1", boundPort);
            clientSockets.add(s);
            inStreams.add(new DataInputStream(new BufferedInputStream(s.getInputStream())));
            outStreams.add(new DataOutputStream(new BufferedOutputStream(s.getOutputStream())));
        }

        // Allow server to accept all clients
        Thread.sleep(250);
        assertEquals("ClientManager registered all 5 clients",
                clientCount, server.getClientManager().getActiveConnectionCount());

        // Test PING / PONG request-response on Client 0
        String reqId = "req-ping-001";
        Packet pingPacket = Packet.createPing(reqId);
        ProtocolCodec.writePacket(outStreams.get(0), pingPacket);

        Packet pongPacket = ProtocolCodec.readPacket(inStreams.get(0));
        assertEquals("Pong message type is RES", MessageType.RES, pongPacket.getType());
        assertEquals("Pong action is PONG", ProtocolAction.PONG, pongPacket.getAction());
        assertEquals("Pong requestId matches", reqId, pongPacket.getRequestId());
        assertEquals("Pong status is SUCCESS", ResponseStatus.SUCCESS, pongPacket.getStatus());

        // Test Malformed Frame Handling on Client 1
        ProtocolCodec.writeRaw(outStreams.get(1), "MALFORMED_GARBAGE_FRAME_WITHOUT_PIPES");
        Packet errPacket = ProtocolCodec.readPacket(inStreams.get(1));
        assertEquals("Malformed frame returns RES", MessageType.RES, errPacket.getType());
        assertEquals("Malformed frame status is BAD_REQUEST", ResponseStatus.BAD_REQUEST, errPacket.getStatus());
        assertTrue("Error message explains malformed frame",
                errPacket.getPayload().get("error").contains("Malformed"));

        // Test Client Disconnect cleanup
        clientSockets.get(2).close();
        clientSockets.get(3).close();
        Thread.sleep(200);

        assertEquals("ClientManager count decreased to 3 active connections",
                3, server.getClientManager().getActiveConnectionCount());

        // Close remaining client sockets
        for (int i = 0; i < clientCount; i++) {
            if (!clientSockets.get(i).isClosed()) {
                clientSockets.get(i).close();
            }
        }

        // Test Server Shutdown
        server.stop();
        Thread.sleep(150);
        assertTrue("Server stopped", !server.isRunning());
        assertEquals("Active connections cleaned up to 0 after server stop",
                0, server.getClientManager().getActiveConnectionCount());
    }
}

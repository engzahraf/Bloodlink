package com.bloodlink.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Encodes and decodes BloodLink packets over TCP streams using DataInputStream and DataOutputStream.
 * Standard framing is managed by readUTF() and writeUTF().
 */
public final class ProtocolCodec {

    private ProtocolCodec() {
        // Utility class
    }

    /**
     * Reads a packet frame from the input stream.
     *
     * @param in the client DataInputStream
     * @return parsed Packet
     * @throws IOException on network or socket read errors (including EOF and timeouts)
     * @throws MalformedPacketException if framing or message content is corrupted
     */
    public static Packet readPacket(DataInputStream in) throws IOException, MalformedPacketException {
        String raw = in.readUTF();
        return Packet.fromWireString(raw);
    }

    /**
     * Serializes and writes a packet frame to the output stream.
     * Synchronized on the output stream to prevent interleaved bytes across threads.
     *
     * @param out the client DataOutputStream
     * @param packet the Packet to send
     * @throws IOException on network or socket write errors
     */
    public static void writePacket(DataOutputStream out, Packet packet) throws IOException {
        if (out == null || packet == null) {
            throw new IllegalArgumentException("Stream and packet must not be null");
        }
        String wire = packet.toWireString();
        synchronized (out) {
            out.writeUTF(wire);
            out.flush();
        }
    }

    /**
     * Writes raw UTF text directly to the stream.
     * Used primarily for diagnostic and negative testing of malformed frames.
     */
    public static void writeRaw(DataOutputStream out, String rawText) throws IOException {
        synchronized (out) {
            out.writeUTF(rawText);
            out.flush();
        }
    }
}

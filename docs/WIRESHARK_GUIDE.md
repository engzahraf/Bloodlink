# BloodLink – Wireshark Network Packet Analysis Guide

This guide provides step-by-step instructions for demonstrating and inspecting BloodLink's TCP socket communication using **Wireshark** during an academic project defense or practical networking exam.

---

## 1. Network Capture Setup

### A. Selecting the Correct Interface
When testing on the local machine (`127.0.0.1` / `localhost`):
* **Windows:** Select the **Adapter for loopback traffic capture** (`\Device\NPF_Loopback` installed via Npcap).
* **Linux / Ubuntu:** Select the **Loopback: lo** interface.
* **LAN / Cross-Machine Testing:** If Member 01's client runs on one laptop and Member 02's server runs on another, select the active **Wi-Fi** or **Ethernet** interface.

### B. Applying Wireshark Capture Filters
Before starting capture, or in the display filter bar at the top, enter:
```text
tcp.port == 5000
```
*(If running integration tests on custom ports, use `tcp.port == 5055`)*.

---

## 2. Step-by-Step Demonstration Workflow

### Step 1: Start Wireshark Capture
1. Launch Wireshark.
2. Double-click **Adapter for loopback traffic capture**.
3. In the display filter bar, type `tcp.port == 5000` and press **Enter**.

### Step 2: Start BloodLink Server & Client
1. Start Member 02's server:
   ```powershell
   java -cp "backend/bin;database/bin;database/lib/*" com.bloodlink.server.ServerMain
   ```
2. Start Member 01's client in Live Mode:
   ```powershell
   .\run-client.bat
   ```

### Step 3: Observe the TCP 3-Way Handshake
In Wireshark, you will immediately see three packets corresponding to connection establishment:
1. `[SYN]` Client $\rightarrow$ Server:
   * Client sends initial sequence number (`Seq=0`).
   * Flags: `SYN` set.
2. `[SYN, ACK]` Server $\rightarrow$ Client:
   * Server acknowledges client sequence (`Ack=1`) and provides its own initial sequence (`Seq=0`).
   * Flags: `SYN`, `ACK` set.
3. `[ACK]` Client $\rightarrow$ Server:
   * Client acknowledges server sequence (`Ack=1`).
   * TCP 3-Way Handshake complete; socket is now in `ESTABLISHED` state.

---

## 3. Application-Level Packet Analysis

### A. Client Request Packet (`writeUTF`)
In the client, perform a login or registration. In Wireshark, click on the packet with `[PSH, ACK]` from client to server:
1. **TCP Header Breakdown:**
   * **Source Port:** Ephemeral port assigned by OS (e.g. `58603`).
   * **Destination Port:** `5000` (BloodLink Server port).
   * **Flags:** `PSH` (Push flag set to force immediate delivery to application buffer without waiting for full TCP segment) and `ACK`.
2. **Payload Inspection (Packet Bytes Pane):**
   * Expand the **Data** tree in Wireshark.
   * Notice the first **2 bytes**: e.g. `00 4B` (75 in decimal). This is the **2-byte big-endian unsigned length** written automatically by Java's `DataOutputStream.writeUTF()`.
   * Following the length prefix are the UTF-8 ASCII characters of our protocol:
     ```text
     REQ|LOGIN|req_101||username=alice%40bloodlink.lk&password=secret123
     ```

### B. Server Response Packet
Click the immediately following `[PSH, ACK]` packet from server to client:
* **Source Port:** `5000`
* **Destination Port:** Ephemeral client port (e.g. `58603`)
* **Payload String:**
  ```text
  RES|LOGIN|req_101|SUCCESS|userId=1&role=DONOR&fullName=Alice+Walker...
  ```

### C. Following the TCP Stream
Right-click on any packet in the sequence $\rightarrow$ **Follow** $\rightarrow$ **TCP Stream**:
* **Client to Server text** is shown in **Red**.
* **Server to Client text** is shown in **Blue**.
* You will clearly see the alternating request-response dialogue and any server push events (`EVENT|URGENT_ALERT`).

---

## 4. Key Networking Concepts to Explain to Evaluators

1. **Why `PSH` flag is used:** In interactive socket programming, the `PSH` flag requests the TCP stack to flush buffered data directly to the receiving application instead of delaying transmission (Nagle's algorithm).
2. **Difference between TCP packet boundaries and `writeUTF()` framing:** TCP is a continuous byte stream without inherent message boundaries. Two `writeUTF()` calls could theoretically arrive inside a single TCP segment or be split across segments. Java's `readUTF()` uses the 2-byte prefix to reconstruct exact message framing.
3. **TCP Connection Teardown (4-Way Handshake):** When logging out or closing the client window:
   * Client sends `[FIN, ACK]`.
   * Server responds with `[ACK]`.
   * Server sends `[FIN, ACK]`.
   * Client responds with `[ACK]`. Socket transitions to `CLOSED`.

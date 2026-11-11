# BloodLink – Academic Viva Evaluation & Interview Q&A Guide

**Member 01:** Frontend Developer & Client Networking Specialist (Imthath)  
**Project:** BloodLink – Blood Donor and Recipient Communication System  
**Subject:** Network Programming & Distributed Systems  

---

## 1. Computer Networking & Architecture Questions

### Q1: What is Computer Networking and what architecture does BloodLink use?
**Answer:** Computer networking is the interconnection of multiple independent computing devices to share resources and exchange information over standardized communication protocols.  
BloodLink uses a **3-tier Client-Server distributed architecture**:
1. **Tier 1 (Presentation):** Native Java Swing desktop application running `SocketClient`.
2. **Tier 2 (Application/Business Logic):** Multithreaded Java TCP server handling client sessions, protocol routing, and matching services.
3. **Tier 3 (Data Persistence):** SQLite relational database managed via JDBC.

---

### Q2: Why did you choose TCP over UDP for this medical application?
**Answer:**  
* **Reliability & Delivery Guarantee:** TCP provides positive acknowledgment (`ACK`), automatic retransmission of lost segments, and checksum verification. In a blood donation platform, losing an urgent blood drive alert or donor availability toggle could cost human lives. UDP has no delivery guarantee.
* **In-Order Sequencing:** TCP guarantees that packets arrive in the exact order transmitted. In donor-recipient direct chat and authentication, out-of-order delivery would corrupt conversation flow and protocol state.
* **Connection-Oriented Sessions:** TCP maintains a persistent virtual connection, enabling the server to immediately recognize when a donor disconnects or goes offline.

---

### Q3: What is the difference between `Socket` and `ServerSocket` in Java?
**Answer:**  
* **`ServerSocket` (Server-side):** Binds to a known port (e.g. 5000) and listens passively for incoming connection requests via `accept()`. It does not perform application data transfer directly; rather, `accept()` returns a new active `Socket` representing the client session.
* **`Socket` (Client & Worker):** Represents an active endpoint of a bidirectional TCP communication link. Provides `getInputStream()` and `getOutputStream()` for transmitting and receiving data bytes between two network endpoints.

---

### Q4: What is a Port number, and what is an Ephemeral Port?
**Answer:** A port is a 16-bit numerical identifier (0 to 65535) used by the transport layer to route incoming packets to a specific process or service.  
* The BloodLink server binds to a **well-known / registered port** (`5000`).
* When Member 01's client connects, the client OS dynamically assigns a temporary **ephemeral port** (typically 49152–65535, e.g. `58603`) to the client socket. The unique connection tuple is `(Source IP, Source Port, Dest IP, Dest Port)`.

---

## 2. Java Streams & Wire Protocol Questions

### Q5: Why did you use `DataInputStream` and `DataOutputStream` with `readUTF()` and `writeUTF()`?
**Answer:**  
1. **Zero External Dependencies:** Built into Java 17 standard library, avoiding heavy JSON/XML parsers.
2. **Automatic Length Framing:** `writeUTF()` writes a 2-byte unsigned length prefix followed by modified UTF-8 encoded string bytes. When the receiving side calls `readUTF()`, it reads exactly those bytes. This solves the **TCP streaming problem** (message boundary delineation) without needing custom delimiter markers.

---

### Q6: How does BloodLink prevent delimiter collisions in the wire string?
**Answer:** Our protocol structure is `TYPE|ACTION|REQUEST_ID|STATUS|PAYLOAD`.  
The payload consists of URL-encoded key-value pairs (`key=value&key2=value2`). Every key and value is passed through `URLEncoder.encode(val, UTF_8)`. If a clinical note contains a pipe (`|`), ampersand (`&`), or newline (`\n`), it is converted to safe ASCII tokens (e.g. `%7C`, `%26`, `%0A`), preventing framing corruption.

---

## 3. Concurrency & Multithreading Questions

### Q7: Why must network operations NOT run on Swing's Event Dispatch Thread (EDT)?
**Answer:** Swing is a single-threaded GUI framework. The EDT is responsible for painting windows, button clicks, and mouse movements. If a blocking network call like `socket.connect()` or `in.readUTF()` was executed on the EDT, the entire user interface would freeze and become unresponsive (OS "Not Responding" state) until the server responded.

---

### Q8: How does the client safely update the UI from a background network thread?
**Answer:** We use `SwingUtilities.invokeLater(Runnable)`. When the background socket reader receives an incoming packet (e.g. an urgent match alert), it queues a task onto the Event Dispatch Thread's event queue. The EDT picks up the task and repaints the table or status badge safely without thread race conditions.

---

### Q9: Why is a single dedicated reader thread required for the client socket?
**Answer:** Because both synchronous request responses (`RES`) and unsolicited asynchronous push events (`EVENT|URGENT_ALERT` or `MSG|SEND_MESSAGE`) arrive over the same incoming TCP stream. If multiple threads called `in.readUTF()`, they would race and consume packets out of order. A single reader thread continuously reads packets, routes synchronous responses to pending futures via `requestId`, and forwards push events to registered UI listeners.

---

### Q10: How does the client correlate responses with asynchronous requests?
**Answer:** The client maintains a `ConcurrentHashMap<String, CompletableFuture<ParsedResponse>>`.  
1. Before sending, the client generates a unique token `req_101` and registers the future.
2. The server echoes `req_101` in the response header: `RES|ACTION|req_101|STATUS|PAYLOAD`.
3. The reader thread looks up `req_101`, removes it from the map, and calls `future.complete(response)`.
4. A scheduled timeout task cancels the future if the server does not respond within 8 seconds.

---

## 4. Architectural Separation & Academic Integrity

### Q11: What was your specific responsibility as Member 01?
**Answer:** I was responsible for:
1. Designing and implementing the complete **Java Swing Desktop Client** and Healthcare UI/UX Design System.
2. Building all **Role-Specific Dashboards** (Admin, Donor, Recipient) and feature panels (Donor Search, Blood Request Creation, Notifications, Chat).
3. Implementing client-side **TCP Socket Networking** (`SocketClient`, `NetworkManager`, `RequestBuilder`, `ResponseHandler`).
4. Implementing the **Offline Mock Mode** (`MockDataService`) for independent demonstration.
5. Performing end-to-end client-server integration testing against Member 02's multithreaded server and Member 03's SQLite database.

---

### Q12: Does the Swing Frontend access the SQLite database directly?
**Answer:** **No, never.** Direct database access from the client would violate 3-tier architecture, expose database credentials, and bypass business validation. All database queries and updates are sent as TCP socket request packets to Member 02's server, which coordinates with Member 03's DAO layer.

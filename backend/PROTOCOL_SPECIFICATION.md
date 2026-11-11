# BloodLink Protocol & Integration Specification

This document provides the authoritative wire protocol and integration contract for:
* **Member 01:** Java Swing Frontend Developer
* **Member 03:** SQLite / JDBC Database Developer

---

## 1. TCP Wire Framing (Member 01 Guide)

### Framing Mechanism
The backend uses standard Java `DataInputStream` and `DataOutputStream` with built-in `readUTF()` and `writeUTF()`.
* Every outgoing packet must be written with `out.writeUTF(wireString); out.flush();`
* Every incoming packet is read with `String wireString = in.readUTF();`
* **Zero External Dependencies:** No JSON, XML, or third-party libraries. Standard Java 21 only.

### Wire String Structure
```text
TYPE|ACTION|REQUEST_ID|STATUS|PAYLOAD
```

| Field | Description | Example Values |
|---|---|---|
| `TYPE` | Message packet type | `REQ`, `RES`, `EVENT`, `MSG`, `ERROR` |
| `ACTION` | Protocol action or event name | `LOGIN`, `REGISTER`, `GET_PROFILE`, `CREATE_BLOOD_REQUEST`, `MATCH_DONORS`, `UPDATE_DONOR_AVAILABILITY`, `SEND_MESSAGE`, `URGENT_ALERT` |
| `REQUEST_ID` | Client-generated identifier to correlate responses with requests | `req_101`, `msg_202`, `evt_303` |
| `STATUS` | Status code (populated on `RES` packets) | `SUCCESS`, `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR` |
| `PAYLOAD` | Delimiter-safe URL-encoded key-value pairs | `key1=value1&key2=value2` |

### Delimiter Safety
All payload keys and values are encoded using standard `java.net.URLEncoder.encode(val, StandardCharsets.UTF_8)` and decoded with `java.net.URLDecoder.decode(val, StandardCharsets.UTF_8)`.
This allows values to safely contain pipes (`|`), ampersands (`&`), equals signs (`=`), colons (`:`), newlines (`\n`), tabs, and emojis without framing collision.

---

## 2. Roles & Permissions Matrix

The system recognizes four roles:
1. `DONOR`: Blood donors. Can toggle availability, view blood requests, match, chat, and receive real-time donation alerts.
2. `RECIPIENT`: Patients or patient representatives. Can publish blood requests, search compatible donors, and initiate direct chat.
3. `HOSPITAL`: Hospital staff and blood banks. Can publish and update blood requests, search donors, and view profiles.
4. `ADMIN`: System administrators.

### Role Registration Rules
* Ordinary users can self-register as `DONOR`, `RECIPIENT`, or `HOSPITAL`.
* Self-registration with role `ADMIN` is strictly forbidden and rejected with status `FORBIDDEN`.
* `DONOR` registrations must include a valid blood group (`O+`, `O-`, `A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`).

---

## 3. Action Catalog (Member 01 Request/Response Reference)

### 3.1. `PING` / `PONG` (Heartbeat / Diagnostic)
* **Request (`REQ`):** `REQ|PING|req_1||`
* **Response (`RES`):** `RES|PONG|req_1|SUCCESS|`

### 3.2. `REGISTER` (User Registration)
* **Request (`REQ`):**
  ```text
  REQ|REGISTER|req_2||username=alice&password=secret123&role=DONOR&fullName=Alice+Walker&bloodGroup=O_NEG&city=Colombo&phone=0771234567
  ```
* **Success Response (`RES`):**
  ```text
  RES|REGISTER|req_2|SUCCESS|userId=usr-101&role=DONOR&message=User+registered+successfully
  ```
* **Error Responses:**
  * Role ADMIN rejected: `RES|REGISTER|req_2|FORBIDDEN|error=Registration+with+role+ADMIN+is+not+permitted+via+public+registration`
  * Missing/invalid blood group: `RES|REGISTER|req_2|BAD_REQUEST|error=A+valid+blood+group+is+required+for+DONOR+registration`
  * Duplicate username: `RES|REGISTER|req_2|CONFLICT|error=Username+already+exists+or+registration+failed`

### 3.3. `LOGIN` (Authentication)
* **Request (`REQ`):**
  ```text
  REQ|LOGIN|req_3||username=alice&password=secret123
  ```
* **Success Response (`RES`):**
  ```text
  RES|LOGIN|req_3|SUCCESS|userId=usr-101&username=alice&role=DONOR&fullName=Alice+Walker&bloodGroup=O_NEG&city=Colombo
  ```
* **Failure Response (`RES`):**
  ```text
  RES|LOGIN|req_3|UNAUTHORIZED|error=Invalid+username+or+password
  ```

### 3.4. `LOGOUT` (Session Termination)
* **Request (`REQ`):** `REQ|LOGOUT|req_4||`
* **Response (`RES`):** `RES|LOGOUT|req_4|SUCCESS|message=Logged+out+successfully`

### 3.5. `GET_PROFILE` (User Profile Query)
* **Request (`REQ`):**
  ```text
  REQ|GET_PROFILE|req_5||userId=usr-101
  ```
  *(If `userId` omitted, defaults to the authenticated client's profile)*
* **Success Response (`RES`):**
  ```text
  RES|GET_PROFILE|req_5|SUCCESS|userId=usr-101&username=alice&role=DONOR&fullName=Alice+Walker&bloodGroup=O_NEG&city=Colombo&phone=0771234567&isAvailable=true&isOnline=true
  ```
  *(Passwords and credential hashes are stripped on the server and never returned)*

### 3.6. `MATCH_DONORS` (Compatible Donor Matching)
Requires authentication.
* **Request (`REQ`):**
  ```text
  REQ|MATCH_DONORS|req_6||bloodGroup=O+&city=Colombo
  ```
* **Success Response (`RES`):**
  ```text
  RES|MATCH_DONORS|req_6|SUCCESS|recipientBloodGroup=O_POS&matchCount=2&donors=<serialized_donor_list>
  ```
  * `donors` contains tilde-separated (`~`) URL-encoded donor sub-records.
  * Each decoded sub-record contains: `userId`, `fullName`, `bloodGroup`, `bloodGroupSymbol`, `city`, `phone`, `isAvailable`, `isOnline`.

### 3.7. `CREATE_BLOOD_REQUEST` (Publish Blood Need)
Requires authentication.
* **Request (`REQ`):**
  ```text
  REQ|CREATE_BLOOD_REQUEST|req_7||bloodGroup=O+&unitsNeeded=2&hospital=National+Hospital&city=Colombo&urgency=URGENT&notes=Patient+in+ICU
  ```
  * `urgency` must be `NORMAL`, `URGENT`, or `CRITICAL`.
  * `unitsNeeded` must be a positive integer ($1 \le n \le 100$).
* **Success Response (`RES`):**
  ```text
  RES|CREATE_BLOOD_REQUEST|req_7|SUCCESS|bloodRequestId=req-501&bloodGroup=O_POS&matchedDonorCount=3&notifiedOnlineCount=2&status=PENDING&message=Blood+request+created+successfully
  ```
  *(If urgency is `URGENT` or `CRITICAL`, all online compatible donors immediately receive real-time push alerts)*

### 3.8. `GET_BLOOD_REQUESTS` (Query Requests)
Requires authentication.
* **Request (`REQ`):**
  ```text
  REQ|GET_BLOOD_REQUESTS|req_8||status=PENDING&city=Colombo
  ```
* **Success Response (`RES`):**
  ```text
  RES|GET_BLOOD_REQUESTS|req_8|SUCCESS|count=1&requests=<serialized_requests_list>
  ```

### 3.9. `UPDATE_BLOOD_REQUEST_STATUS`
Requires authentication. Allowed only for the creator of the request, `HOSPITAL`, or `ADMIN`.
* **Request (`REQ`):**
  ```text
  REQ|UPDATE_BLOOD_REQUEST_STATUS|req_9||bloodRequestId=req-501&status=FULFILLED
  ```
  * `status` must be `PENDING`, `MATCHED`, `FULFILLED`, or `CANCELLED`.
* **Success Response (`RES`):**
  ```text
  RES|UPDATE_BLOOD_REQUEST_STATUS|req_9|SUCCESS|bloodRequestId=req-501&status=FULFILLED&message=Blood+request+status+updated+to+FULFILLED
  ```

### 3.10. `UPDATE_DONOR_AVAILABILITY`
Requires authentication. Allowed only for `DONOR` (updating their own availability) or `ADMIN`.
* **Request (`REQ`):**
  ```text
  REQ|UPDATE_DONOR_AVAILABILITY|req_10||isAvailable=false
  ```
* **Success Response (`RES`):**
  ```text
  RES|UPDATE_DONOR_AVAILABILITY|req_10|SUCCESS|userId=usr-101&isAvailable=false&message=Donor+availability+updated+to+false
  ```

### 3.11. `GET_NOTIFICATIONS` (Notification History)
Requires authentication. Retrieves persisted offline/online notifications.
* **Request (`REQ`):**
  ```text
  REQ|GET_NOTIFICATIONS|req_11||limit=20
  ```
* **Success Response (`RES`):**
  ```text
  RES|GET_NOTIFICATIONS|req_11|SUCCESS|count=1&notifications=<serialized_notifications_list>
  ```

### 3.12. `SEND_MESSAGE` (Donor-Recipient Direct Chat)
Requires authentication. Sender identity is verified server-side to prevent impersonation.
* **Request (`REQ`):**
  ```text
  REQ|SEND_MESSAGE|req_12||recipientId=usr-101&content=Can+you+donate+tomorrow%3F
  ```
* **Success Response (`RES`):**
  ```text
  RES|SEND_MESSAGE|req_12|SUCCESS|messageId=msg-1001&recipientId=usr-101&deliveredRealTime=true&timestamp=1700000000000
  ```
  *(If recipient is offline, `deliveredRealTime=false` is returned and the message is safely queued in the database for later history retrieval)*

### 3.13. `GET_MESSAGES` (Chat Conversation History)
Requires authentication.
* **Request (`REQ`):**
  ```text
  REQ|GET_MESSAGES|req_13||otherUserId=usr-101&limit=50
  ```
* **Success Response (`RES`):**
  ```text
  RES|GET_MESSAGES|req_13|SUCCESS|count=2&withUserId=usr-101&messages=<serialized_messages_list>
  ```

---

## 4. Asynchronous Server Push Events (Member 01 Swing Integration)

The server pushes unsolicited asynchronous events directly to connected clients over the socket.
Member 01 should maintain a dedicated background reading thread that forwards events to the Swing Event Dispatch Thread (EDT) using `SwingUtilities.invokeLater()`:

```java
// Background Socket Reader in Swing Client:
while (running && !socket.isClosed()) {
    String raw = in.readUTF();
    // Packet wire format: TYPE|ACTION|REQUEST_ID|STATUS|PAYLOAD
    String[] parts = raw.split("\\|", 5);
    String type = parts[0];
    String action = parts[1];
    String requestId = parts[2];
    String status = parts[3];
    String payload = (parts.length >= 5) ? parts[4] : "";

    if ("EVENT".equals(type) && "URGENT_ALERT".equals(action)) {
        // Dispatched when an urgent blood match alert occurs
        SwingUtilities.invokeLater(() -> {
            // Show alert dialog banner or notification popup
        });
    } else if ("MSG".equals(type)) {
        // Incoming direct chat message
        SwingUtilities.invokeLater(() -> {
            // Append incoming message to active chat dialog
        });
    } else if ("RES".equals(type)) {
        // Correlate synchronous request by matching requestId
    }
}
```

### Push Event Formats:
* **Urgent Match Alert (`EVENT`):**
  ```text
  EVENT|URGENT_ALERT|alert-1||bloodGroup=O_POS&hospital=National+Hospital&city=Colombo&urgency=URGENT&title=URGENT%3A+Blood+Donation+Needed...
  ```
* **Incoming Direct Chat (`MSG`):**
  ```text
  MSG|SEND_MESSAGE|msg-1001||messageId=msg-1001&senderId=usr-202&recipientId=usr-101&content=Hello&timestamp=1700000000000
  ```

---

## 5. Database Service Interface Contract (Member 03 Guide)

Member 03 implements `com.bloodlink.interfaces.IDatabaseService`.

### Interface Signature:
```java
package com.bloodlink.interfaces;

import java.util.List;
import java.util.Map;

public interface IDatabaseService {

    // --- Authentication & Users ---
    Map<String, String> authenticate(String username, String password);
    String registerUser(Map<String, String> userData);
    Map<String, String> getUserById(String userId);

    // --- Blood Requests ---
    String createBloodRequest(Map<String, String> requestData);
    List<Map<String, String>> getBloodRequests(Map<String, String> filters);
    boolean updateBloodRequestStatus(String bloodRequestId, String newStatus);

    // --- Donor Matching ---
    List<Map<String, String>> findEligibleDonors(List<String> compatibleBloodGroups, String city);
    boolean updateDonorAvailability(String userId, boolean isAvailable);

    // --- Messaging ---
    String saveMessage(String senderId, String recipientId, String content, long timestamp);
    List<Map<String, String>> getConversationHistory(String user1, String user2, int limit);

    // --- Notifications ---
    boolean saveNotification(String targetUserId, String title, String message, String urgency, long timestamp);
    List<Map<String, String>> getNotificationsForUser(String userId, int limit);
}
```

### Requirements for Member 03:
1. **Thread Safety & SQLite Concurrency:** SQLite is single-writer. In a multithreaded server where multiple `ClientHandler` threads access `IDatabaseService` simultaneously, synchronize write transactions (`registerUser`, `createBloodRequest`, `updateStatus`, `saveMessage`, `saveNotification`) or use a connection coordinator to prevent `SQLITE_BUSY` errors.
2. **Canonical Blood Groups:** Canonical tokens used by backend are: `O_POS`, `O_NEG`, `A_POS`, `A_NEG`, `B_POS`, `B_NEG`, `AB_POS`, `AB_NEG`.
3. **Password Security:** Credentials passed to `authenticate(username, password)` will be validated against your SQLite table. When hashing is finalized (e.g. SHA-256), compare the hashed input with the stored hash.

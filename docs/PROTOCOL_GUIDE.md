# BloodLink – TCP Wire Protocol Guide

## 1. Framing Specification

Communication between Member 01's client and Member 02's server uses standard Java `DataOutputStream.writeUTF()` and `DataInputStream.readUTF()`.
* **Zero External Dependencies:** Built into Java 17 standard library.
* **Framing Mechanism:** `writeUTF()` automatically prefixes the serialized string with a 2-byte unsigned length indicator (big-endian), allowing `readUTF()` to reconstruct packets without custom length headers or delimiter sniffing.

### Wire String Structure
```text
TYPE|ACTION|REQUEST_ID|STATUS|PAYLOAD
```

| Token | Description | Example Values |
|---|---|---|
| `TYPE` | Message packet type | `REQ`, `RES`, `EVENT`, `MSG`, `ERROR` |
| `ACTION` | Protocol action or event verb | `LOGIN`, `REGISTER`, `GET_PROFILE`, `CREATE_BLOOD_REQUEST`, `MATCH_DONORS`, `UPDATE_DONOR_AVAILABILITY`, `SEND_MESSAGE`, `GET_MESSAGES`, `GET_NOTIFICATIONS`, `PING`, `PONG` |
| `REQUEST_ID` | Client correlation token | `req_101`, `msg_202`, `ping_303` |
| `STATUS` | Status code (populated on `RES`) | `SUCCESS`, `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR` |
| `PAYLOAD` | Delimiter-safe URL-encoded key-values | `username=alice&role=DONOR` |

---

## 2. Delimiter Safety & URL Encoding
To prevent framing collision with pipe (`|`), ampersand (`&`), equals (`=`), and newlines, all keys and values are encoded with `java.net.URLEncoder.encode(val, StandardCharsets.UTF_8)` and decoded with `java.net.URLDecoder.decode(val, StandardCharsets.UTF_8)`.

Lists of sub-records (e.g. lists of blood requests or donors) are separated by tildes (`~`):
```text
requests_list=item1%26field%3Dval~item2%26field%3Dval
```

---

## 3. Protocol Operations Catalog

### 3.1 `PING` / `PONG` (Diagnostic & Heartbeat)
* **Client Request:** `REQ|PING|req_1||`
* **Server Response:** `RES|PONG|req_1|SUCCESS|`

### 3.2 `REGISTER` (User Registration)
* **Client Request:**
  ```text
  REQ|REGISTER|req_2||username=alice%40bloodlink.lk&password=secret123&role=DONOR&fullName=Alice+Walker&bloodGroup=O_POS&city=Colombo&phone=0771234567&email=alice%40bloodlink.lk
  ```
* **Server Response:**
  ```text
  RES|REGISTER|req_2|SUCCESS|userId=1&role=DONOR&message=User+registered+successfully
  ```

### 3.3 `LOGIN` (Authentication)
* **Client Request:**
  ```text
  REQ|LOGIN|req_3||username=alice%40bloodlink.lk&password=secret123
  ```
* **Server Response:**
  ```text
  RES|LOGIN|req_3|SUCCESS|userId=1&username=alice%40bloodlink.lk&role=DONOR&fullName=Alice+Walker&bloodGroup=O_POS&city=Colombo
  ```

### 3.4 `CREATE_BLOOD_REQUEST` (Publish Blood Request)
* **Client Request:**
  ```text
  REQ|CREATE_BLOOD_REQUEST|req_4||bloodGroup=O_POS&units=2&hospital=National+Hospital&city=Colombo&urgency=URGENT&notes=Immediate+surgery
  ```
* **Server Response:**
  ```text
  RES|CREATE_BLOOD_REQUEST|req_4|SUCCESS|requestId=1&message=Blood+request+created+successfully
  ```

### 3.5 `MATCH_DONORS` (Query Compatible Donors)
* **Client Request:**
  ```text
  REQ|MATCH_DONORS|req_5||bloodGroup=O_POS&city=Colombo
  ```
* **Server Response:**
  ```text
  RES|MATCH_DONORS|req_5|SUCCESS|count=2&donors_list=userId%3D1%26fullName%3DAlice+Walker%26bloodGroup%3DO_POS...
  ```

### 3.6 `UPDATE_DONOR_AVAILABILITY` (Toggle Real-time Status)
* **Client Request:**
  ```text
  REQ|UPDATE_DONOR_AVAILABILITY|req_6||userId=1&available=false
  ```
* **Server Response:**
  ```text
  RES|UPDATE_DONOR_AVAILABILITY|req_6|SUCCESS|message=Donor+availability+updated
  ```

### 3.7 `SEND_MESSAGE` (Direct Donor-Recipient Messaging)
* **Client Request:**
  ```text
  REQ|SEND_MESSAGE|req_7||recipientId=2&content=Hello+can+you+donate+tomorrow%3F
  ```
* **Server Response:**
  ```text
  RES|SEND_MESSAGE|req_7|SUCCESS|messageId=1&message=Message+sent+successfully
  ```

---

## 4. Asynchronous Server Push Events

Unsolicited events pushed by the server to connected clients:

### Emergency Blood Drive Alert (`EVENT|URGENT_ALERT`)
Broadcast to eligible donors when an urgent or critical request is published:
```text
EVENT|URGENT_ALERT|alert-1||bloodGroup=O_POS&hospital=National+Hospital&city=Colombo&urgency=URGENT&title=URGENT%3A+Blood+Donation+Needed
```

### Incoming Chat Message (`MSG|SEND_MESSAGE`)
Pushed directly to the recipient client socket:
```text
MSG|SEND_MESSAGE|msg-1001||messageId=1&senderId=1&recipientId=2&content=Hello&timestamp=1700000000000
```

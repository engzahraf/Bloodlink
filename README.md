# BloodLink – Real-Time Blood Donor & Recipient Coordination System

BloodLink is a distributed, multi-tier client-server application built with pure Java, Swing, multithreaded TCP sockets, and SQLite. It provides real-time emergency blood request matching, donor response tracking, bidirectional live messaging, offline notification persistence, and administrative controls.

---

## 1. System Overview & Core Features

### 🩸 Blood Request & Matching Flow
* **ABO/Rh Compatibility Engine**: Automatically determines blood compatibility between donors and recipients.
* **Emergency Broadcast Alerts**: Real-time push notifications delivered immediately to eligible online donors upon blood request creation.
* **Offline Notification Storage**: Alerts and updates persist in SQLite, ensuring offline users receive them immediately upon their next login.

### 👤 Recipient Panel
* **Comprehensive Request Tracking**: Real-time overview of active and past blood requests including Request ID, required units, location, urgency level, date, and status.
* **Donor Responses Inspection**: Displays real-time responses from matching donors:
  * Donor name, blood group, city, and availability status.
  * Explicit response status: `DONOR ACCEPTED`, `DONOR REJECTED`, or `DONOR PENDING`.
* **Direct Actions**:
  * **[💬 Message Donor]**: Open an instant private chat window with the responding donor.
  * **[👤 View Profile]**: View the donor's contact and donation background.

### 🤝 Donor Panel
* **Matching Request Feed**: Live stream of compatible blood requests filtered by blood type, location, and urgency.
* **Actionable Request Cards**:
  * **[✓ Accept Request]**: Accepts the blood request, updates the database, changes status to `ACCEPTED`, and notifies the recipient in real time.
  * **[✗ Reject Request]**: Marks the response as `REJECTED` and notifies the recipient.
  * **Duplicate Prevention**: Buttons dynamically disable once responded to, preventing duplicate accepts/rejects.
* **Recipient Messaging**: Immediate chat capability directly from the request card.
* **Availability Toggle**: One-click toggle between `AVAILABLE` and `UNAVAILABLE`.

### 🛡️ Admin Panel & Server-Side Security
* **Server-Side Role Authorization**: All administrative endpoints (`ADMIN_*`) strictly verify `role == 'ADMIN'` on the backend. Client-side bypasses are completely blocked (`UNAUTHORIZED`).
* **Live System Statistics**: Real-time counts for Total Users, Donors, Recipients, Pending Requests, Fulfilled Requests, and Blood Group Demand distribution (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`).
* **User Registry Management**: Searchable user table with one-click actions:
  * **[🚫 Block Account]** / **[✓ Unblock Account]**
  * **[🗑 Delete Account]**
  * **[👤 View Profile]**
* **Blood Request Management**: View all requests, filter by status, inspect donor responses, cancel fraudulent or outdated requests (**[❌ Cancel Request]**), and contact requesters.
* **Credential Protection**: Passwords are encrypted with PBKDF2 with HMAC-SHA256 and 65,536 iterations; never stored or transmitted in plaintext.

---

## 2. Technology Stack & Requirements

* **Language**: Java 21 (compatible with JDK 17+)
* **GUI / Frontend**: Java Swing (clean modern dark/light styling on the Event Dispatch Thread)
* **Networking**: Custom TCP protocol over `java.net.ServerSocket` and `java.net.Socket` using `DataInputStream.readUTF()` / `DataOutputStream.writeUTF()`
* **Concurrency**: `ExecutorService` thread pool with thread-safe session binding
* **Database**: Embedded SQLite 3 with official JDBC driver (`lib/sqlite-jdbc-3.51.3.0.jar`)
* **Security**: PBKDF2 with HMAC-SHA256 (65,536 iterations, 128-bit salt, 256-bit key)
* **Zero Bloat**: No Maven, Gradle, Spring Boot, Gson, or Jackson required; pure standard Java standard library + SQLite JDBC.

---

## 3. Project Directory Structure

```text
Bloodlink/
├── lib/
│   └── sqlite-jdbc-3.51.3.0.jar        # SQLite JDBC driver
├── data/
│   └── bloodlink.db                    # SQLite production database
├── bin/                                # Compiled JVM bytecode (.class)
├── src/com/bloodlink/
│   ├── client/                         # Swing Desktop Client
│   │   ├── models/                     # User, BloodRequest, DonorResponse, Message
│   │   ├── network/                    # SocketClient, NetworkManager, RequestBuilder, ResponseHandler
│   │   ├── screens/                    # LoginFrame, RegisterFrame, RecipientDashboard, DonorDashboard, AdminDashboard
│   │   ├── ui/                         # NavigationManager, ThemeManager, components/
│   │   └── utils/                      # SessionManager, ValidationUtils
│   ├── server/                         # TCP Server Architecture
│   │   ├── BloodLinkServer.java        # Multithreaded TCP ServerSocket manager
│   │   ├── ClientHandler.java          # Per-client I/O worker loop
│   │   ├── ClientManager.java          # Thread-safe connection & active user registry
│   │   ├── RequestDispatcher.java      # Routes actions to services
│   │   └── ServerMain.java             # Server entry point
│   ├── services/                       # Business Logic Layer
│   │   ├── AuthService.java            # Registration, login, password hashing
│   │   ├── BloodMatchingService.java   # Request matching, donor accept/reject, admin actions
│   │   ├── BloodCompatibility.java     # ABO/Rh compatibility matrix
│   │   ├── NotificationService.java    # Real-time event push & offline queueing
│   │   └── MessagingService.java       # Direct messaging & conversation retrieval
│   ├── database/                       # SQLite Persistence Layer
│   │   ├── dao/                        # UserDAO, BloodRequestDAO, DonorResponseDAO, MessageDAO, etc.
│   │   ├── models/                     # Domain entity classes
│   │   ├── enums/                      # BloodGroup, RequestStatus, UserRole, AccountStatus, etc.
│   │   ├── security/                   # PasswordHasher (PBKDF2)
│   │   ├── DatabaseConnection.java     # Thread-safe JDBC connection management
│   │   ├── DatabaseInitializer.java    # Schema migration & index creation
│   │   └── SQLiteDatabaseService.java  # IDatabaseService implementation
│   ├── protocol/                       # Wire Protocol Layer
│   │   ├── Packet.java                 # Structured protocol frame model
│   │   ├── ProtocolAction.java         # Action constants
│   │   ├── ProtocolCodec.java          # Delimiter-safe serialization
│   │   └── ResponseStatus.java         # SUCCESS, ERROR, UNAUTHORIZED, BAD_REQUEST, etc.
│   └── util/                           # ServerConfig, ServerLogger
├── test/                               # Automated Test Suites
│   ├── FullSystemIntegrationTest.java  # End-to-end multi-client TCP integration test
│   ├── FrontendTestSuite.java          # UI screens & validation tests
│   ├── Stage1NetworkTest.java          # Socket framing & stress tests
│   ├── Stage2ServiceIntegrationTest.java # Service-layer & authorization tests
│   └── SQLiteIntegrationTest.java      # Direct SQLite database tests
├── compile.bat                         # Windows source compilation script
├── run-server.bat                      # Windows server launcher (with auto-build & admin seed)
├── run-client.bat                      # Windows desktop client launcher
├── PROTOCOL_SPECIFICATION.md           # Wire protocol documentation
└── README.md                           # Project documentation
```

---

## 4. How to Compile & Run

### A. Quick Start (Windows Batch Scripts)

1. **Start the TCP Backend Server**:
   ```cmd
   run-server.bat
   ```
   *Compiles all source files, initializes SQLite tables, seeds default admin credentials, and starts the server on port `9090`.*

2. **Launch the Desktop Client**:
   ```cmd
   run-client.bat
   ```
   *Launches the Swing graphical interface connecting to `127.0.0.1:9090`.*

---

### B. Manual Command Line Instructions

#### 1. Compilation
From the project root:
```cmd
mkdir bin
dir /s /b src\*.java > sources.txt
javac -Xlint:none -cp "lib\sqlite-jdbc-3.51.3.0.jar" -d bin @sources.txt
del sources.txt
```

#### 2. Start Server
```cmd
java -cp "bin;lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.server.ServerMain
```

#### 3. Start Client UI
```cmd
java -cp "bin;lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.client.Main
```

*Client command line options:*
* `--host <ip>`: Connect to custom server host (default: `127.0.0.1`).
* `--port <port>`: Connect to custom server port (default: `9090`).
* `--mock`: Run in offline UI mock demo mode without connecting to a server.

---

## 5. Default Credentials & Test Accounts

### Administrator Account
Seeded automatically on server startup:
* **Email / Username**: `admin@bloodlink.com`
* **Password**: `Admin@BloodLink2026`
* **Role**: `ADMIN`

### Registering Test Accounts
You can register new **Donor** or **Recipient** accounts directly through the UI registration screen:
1. Click **"Need an account? Register here"** on the Login screen.
2. Select role (**DONOR** or **RECIPIENT**).
3. If Donor, select your Blood Group (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`) and city.
4. Sign in to access your role-specific dashboard.

---

## 6. Server Configuration & Environment Variables

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `BLOODLINK_PORT` | `9090` | TCP Server listening port |
| `BLOODLINK_WORKER_THREADS` | `50` | Maximum concurrent client threads |
| `BLOODLINK_DB_PATH` | `data/bloodlink.db` | Relative or absolute path to SQLite database |
| `BLOODLINK_SEED_ADMIN` | `true` | Seed default admin account on startup (`true`/`false`) |
| `BLOODLINK_ADMIN_USER` | `admin@bloodlink.com` | Default admin email |
| `BLOODLINK_ADMIN_PASS` | `Admin@BloodLink2026` | Default admin password |

---

## 7. Wire Protocol Specification (Summary)

The communication protocol utilizes length-prefixed UTF strings (`DataOutputStream.writeUTF()`) formatted as:
```text
TYPE|ACTION|REQUEST_ID|KEY1=VAL1&KEY2=VAL2
```

### Key Supported Actions:
* **Authentication**: `REGISTER`, `LOGIN`, `LOGOUT`, `UPDATE_PROFILE`
* **Blood Requests**: `CREATE_BLOOD_REQUEST`, `GET_ACTIVE_REQUESTS`, `GET_MATCHING_REQUESTS`, `CANCEL_BLOOD_REQUEST`
* **Donor Responses**:
  * `ACCEPT_REQUEST`: Donor accepts a blood request (updates status, notifies recipient).
  * `REJECT_REQUEST`: Donor rejects a blood request.
  * `GET_REQUEST_RESPONSES`: Recipient retrieves all donor responses for their request.
  * `GET_DONOR_RESPONSES`: Donor retrieves their response history.
* **Notifications**: `GET_NOTIFICATIONS`, `MARK_NOTIFICATION_READ`
* **Messaging**: `SEND_MESSAGE`, `GET_MESSAGES`, `GET_CONVERSATIONS`
* **Admin Control**:
  * `ADMIN_GET_USERS`: Fetch user registry.
  * `ADMIN_BLOCK_USER` / `ADMIN_UNBLOCK_USER`: Toggle account lock status.
  * `ADMIN_DELETE_USER`: Delete inappropriate or bot accounts.
  * `ADMIN_CANCEL_REQUEST`: Cancel fraudulent blood requests.
  * `ADMIN_GET_STATS`: Aggregate system and blood group inventory statistics.

*For complete protocol framing rules and payload specifications, see [PROTOCOL_SPECIFICATION.md](file:///c:/Users/HP/Downloads/Net/Bloodlink/PROTOCOL_SPECIFICATION.md).*

---

## 8. Automated Testing & Verification

The project includes automated integration and unit test suites:

| Test Suite | File | Assertions / Result | Description |
| :--- | :--- | :--- | :--- |
| **Full System Integration** | `test/FullSystemIntegrationTest.java` | **44 / 44 (100%)** | End-to-end test of Accept/Reject flow, real-time push, offline persistence, messaging, and admin controls over live TCP sockets. |
| **Frontend Test Suite** | `test/FrontendTestSuite.java` | **52 / 52 (100%)** | Validates Swing components, tables, navigation, and validation utilities. |
| **Service Integration** | `test/Stage2ServiceIntegrationTest.java` | **73 / 73 (100%)** | ABO/Rh compatibility, authorization checks, and service dispatching. |
| **Networking & Framing** | `test/Stage1NetworkTest.java` | **17 / 17 (100%)** | Multi-client connection pooling, malformed packets, delimiter safety. |
| **SQLite Integration** | `test/SQLiteIntegrationTest.java` | **60 / 60 (100%)** | JDBC schema, PBKDF2 hashing, and foreign key constraints. |
| **Database Unit Tests** | `test/com/bloodlink/database/DatabaseTestSuite.java` | **22 / 22 (100%)** | DAO CRUD, rollbacks, and concurrent transactions. |

To run the complete system verification test:
```cmd
javac -cp "bin;lib\sqlite-jdbc-3.51.3.0.jar" -d bin test\FullSystemIntegrationTest.java
java -cp "bin;lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.client.test.FullSystemIntegrationTest
```

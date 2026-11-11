# BloodLink – Network Based Blood Bank Inventory System

BloodLink is a distributed, multi-tier client-server application designed for emergency blood matching, donor coordination, and inventory management.

---

## 1. System Architecture & Team Responsibilities

The system is built as a collaborative multi-tier Java application:

* **Member 01 – Frontend Developer:** Java Swing client interface, user interactions, responsive UI on the Event Dispatch Thread (EDT).
* **Member 02 – Networking & Backend Developer:** Multithreaded TCP Server (`ServerSocket`), non-blocking client connection pool (`ExecutorService`), custom delimiter-safe wire protocol (`readUTF()` / `writeUTF()`), business logic services (Authentication, ABO/Rh Matching, Push Notifications, Direct Messaging), and SQLite adapter (`SQLiteDatabaseService`).
* **Member 03 – Database Developer:** Relational SQLite schema, JDBC connectivity (`DatabaseConnection`), Data Access Objects (`UserDAO`, `DonorProfileDAO`, `BloodRequestDAO`, `DonationDAO`, `NotificationDAO`, `MessageDAO`), domain models, PBKDF2 password hashing (`PasswordHasher`), and database integrity rules.

---

## 2. Technology Stack & Prerequisites

* **Language:** Java 21 (compatible with JDK 17+)
* **Networking:** Standard Java `java.net.ServerSocket`, `java.net.Socket`, `java.io.DataInputStream`, `java.io.DataOutputStream`
* **Concurrency:** `java.util.concurrent.ExecutorService` (Fixed thread pool with graceful shutdown)
* **Database:** SQLite 3 with JDBC driver (`sqlite-jdbc-3.51.3.0.jar`)
* **Security:** PBKDF2 with HMAC-SHA256 (65,536 iterations, 128-bit salt, 256-bit derived key)
* **External Dependencies:** Zero external frameworks (No Spring Boot, Maven, Gradle, Gson, Jackson). Only the official SQLite JDBC driver JAR is required:
  * Driver path: `database/lib/sqlite-jdbc-3.51.3.0.jar`

---

## 3. Directory Structure

```text
E:\Networking project\
├── backend/
│   ├── src/com/bloodlink/
│   │   ├── database/       # SQLiteDatabaseService (Member 02 adapter to Member 03 DAOs)
│   │   ├── interfaces/     # IDatabaseService interface contract
│   │   ├── protocol/       # Packet, ProtocolCodec, ProtocolAction, ResponseStatus
│   │   ├── server/         # BloodLinkServer, ClientHandler, ClientManager, RequestDispatcher, ServerMain
│   │   ├── services/       # AuthService, BloodMatchingService, BloodCompatibility, NotificationService, MessagingService
│   │   └── util/           # ServerConfig, ServerLogger
│   ├── test/               # Stage1NetworkTest, Stage2ServiceIntegrationTest, SQLiteIntegrationTest, MockDatabaseService
│   └── PROTOCOL_SPECIFICATION.md
├── database/
│   ├── lib/                # sqlite-jdbc-3.51.3.0.jar
│   ├── src/main/java/com/bloodlink/database/
│   │   ├── dao/            # UserDAO, DonorProfileDAO, BloodRequestDAO, DonationDAO, NotificationDAO, MessageDAO
│   │   ├── enums/          # BloodGroup, UserRole, RequestStatus, UrgencyLevel, AvailabilityStatus, NotificationType
│   │   ├── exceptions/     # DatabaseException, RecordNotFoundException, ValidationException
│   │   ├── models/         # User, DonorProfile, BloodRequest, Donation, Notification, Message
│   │   ├── security/       # PasswordHasher (PBKDF2)
│   │   ├── DatabaseConnection.java
│   │   └── DatabaseInitializer.java
│   └── src/test/java/      # DatabaseTestSuite (Member 03 standalone suite)
├── data/                   # SQLite database storage (bloodlink.db, test databases)
└── README.md
```

---

## 4. Compilation Instructions

Compile all database classes, backend classes, and integration test suites into `backend/bin` using standard `javac`:

### Windows PowerShell:
```powershell
# Create binary output directory
if (-not (Test-Path "backend\bin")) { New-Item -ItemType Directory -Path "backend\bin" | Out-Null }

# Compile all source files
javac -Xlint:all -cp "database\lib\sqlite-jdbc-3.51.3.0.jar" -d backend\bin -sourcepath "database\src\main\java;database\src\test\java;backend\src;backend\test" (Get-ChildItem -Recurse -Filter "*.java" database\src\main\java, database\src\test\java, backend\src, backend\test | ForEach-Object { $_.FullName })
```

---

## 5. Running the Backend Server

The server initializes the SQLite database schema automatically on boot (`DatabaseInitializer.initialize()`), seeds an admin account if configured, and starts listening for incoming TCP client connections.

### Windows PowerShell:
```powershell
java -cp "backend\bin;database\lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.server.ServerMain
```

### Environment Configuration & Secure First-Admin Setup:
Admin seeding is **disabled by default** to prevent unauthorized default accounts or publicly known credentials. To securely bootstrap the initial system administrator, provide custom environment variables:

```powershell
$env:BLOODLINK_SEED_ADMIN="true"
$env:BLOODLINK_ADMIN_USER="sysadmin@bloodlink.lk"
$env:BLOODLINK_ADMIN_PASS="YourSecurePasswordHere#2026"
java -cp "backend\bin;database\lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.server.ServerMain
```

| Environment Variable | Default Value | Description |
|---|---|---|
| `BLOODLINK_PORT` | `9090` | TCP Server listening port |
| `BLOODLINK_WORKER_THREADS` | `50` | Size of client worker thread pool |
| `BLOODLINK_DB_PATH` | `data/bloodlink.db` | Absolute or relative SQLite database path |
| `BLOODLINK_SEED_ADMIN` | `false` | Explicitly enable initial admin bootstrap (`true` / `false`) |
| `BLOODLINK_ADMIN_USER` | *(none)* | Initial administrator email (required if `SEED_ADMIN=true`) |
| `BLOODLINK_ADMIN_PASS` | *(none)* | Initial administrator password (min 8 chars, hashed with PBKDF2; never logged) |

---

## 6. Running Automated Test Suites

All tests run using standard Java (no JUnit required) and print structured status reports.

### 1. Real SQLite / JDBC Integration Test Suite (Member 02 Adapter + Member 03 DB)
Tests the complete end-to-end integration: database initialization, user registration, PBKDF2 hashing, login authentication, profile queries, blood requests, ABO/Rh donor matching, availability toggling, notification storage, chat history, thread concurrency, and live TCP socket transactions against SQLite:
```powershell
java -cp "backend\bin;database\lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.test.SQLiteIntegrationTest
```
*(Result: 15 suites, 55/55 assertions passed - 100%)*

### 2. Stage 1 Networking & Framing Test Suite
Tests TCP server socket binding, delimiter and URL-encoding safety, simultaneous multi-client connection handling, framing validation, malformed packet rejections, and graceful server shutdown:
```powershell
java -cp "backend\bin;database\lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.test.Stage1NetworkTest
```
*(Result: 17/17 assertions passed - 100%)*

### 3. Stage 2 Service & Protocol Security Test Suite
Tests all 8 ABO/Rh blood compatibility combinations, role-based authorization rules, prevention of unauthenticated access, hospital and donor permissions, real-time alert dispatching, and offline direct message queueing:
```powershell
java -cp "backend\bin;database\lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.test.Stage2ServiceIntegrationTest
```
*(Result: 70/70 assertions passed - 100%)*

### 4. Member 03 Standalone Database Test Suite
Tests standalone DAO operations, relational integrity, foreign key cascades, SQL injection prevention, transaction rollbacks, and concurrent thread-safety:
```powershell
java -cp "backend\bin;database\lib\sqlite-jdbc-3.51.3.0.jar" com.bloodlink.database.DatabaseTestSuite
```
*(Result: 22/22 tests passed - 100%)*

---

## 7. Integration & Protocol Specifications

* Refer to [backend/PROTOCOL_SPECIFICATION.md](file:///backend/PROTOCOL_SPECIFICATION.md) for full wire protocol details, packet framing rules, payload structure, and Swing client integration patterns.

### Role Consistency & Unresolved Hospital Specification:
* **System Roles:** The system recognizes the three roles defined in the original specification and database schema: `ADMIN`, `DONOR`, and `RECIPIENT`.
* **Registration Validation:** Public self-registration is allowed strictly for `DONOR` and `RECIPIENT`. Any registration attempt with `role=HOSPITAL` is rejected with `BAD_REQUEST`. The backend does NOT silently map `HOSPITAL` to `RECIPIENT` or `ADMIN`.
* **Unresolved Hospital Role:** The SQLite database table enforces `CHECK(role IN ('ADMIN', 'DONOR', 'RECIPIENT'))`. If hospital-specific permissions (e.g. specialized medical staff privileges, verified inventory quotas) are required, this requirement is documented as unresolved pending formal team consensus with Member 03 (schema update) and Member 01 (UI screens). Currently, hospital facility names are recorded as request metadata on blood requests without inventing an unpersisted role.

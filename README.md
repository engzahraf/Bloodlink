# BloodLink – Blood Donor and Recipient Communication System

[![Java](https://img.shields.io/badge/Java-17%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Swing](https://img.shields.io/badge/UI-Java%20Swing-red?style=for-the-badge)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![TCP Socket](https://img.shields.io/badge/Networking-TCP%20Sockets-00599C?style=for-the-badge)](https://docs.oracle.com/javase/tutorial/networking/sockets/)
[![SQLite](https://img.shields.io/badge/Database-SQLite%203-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)

BloodLink is a distributed desktop healthcare application designed to connect voluntary blood donors with hospitals, clinics, and recipients in real-time. Built using native Java Swing, multithreaded TCP socket communication, and SQLite persistence.

---

## 1. System Architecture

```text
JAVA SWING DESKTOP CLIENT (Member 01)
               │
               ▼  TCP Socket (DataInputStream / DataOutputStream / writeUTF)
MULTITHREADED JAVA SERVER (Member 02)
               │
               ▼  JDBC Data Access Layer (DAOs)
     SQLITE DATABASE (Member 03)
```

---

## 2. Team Division of Responsibilities

* **Member 01 – Imthath (Frontend & Client-Side Networking):**
  * Java Swing Desktop GUI & Healthcare Design System (`ThemeManager`, `ModernButton`, `ModernTable`, `StatCard`, `StatusBadge`).
  * Authentication (`LoginFrame`, `RegisterFrame`) and Role-Based Dashboards (`AdminDashboard`, `DonorDashboard`, `RecipientDashboard`).
  * Feature Panels: `BloodRequestPanel`, `DonorProfilePanel`, `DonorSearchPanel`, `NotificationPanel`, `MessagingPanel`.
  * TCP Client Networking (`SocketClient`, `NetworkManager`, `RequestBuilder`, `ResponseHandler`).
  * Offline Mock Mode (`MockDataService`) and End-to-End Testing.
* **Member 02 (Backend & Server Networking):**
  * Multithreaded TCP Server (`BloodLinkServer`), Client Management (`ClientManager`), Protocol Routing (`RequestDispatcher`).
  * Authentication, Matching, Messaging, and Notification services.
  * Wire framing protocol specification.
* **Member 03 (Database & Persistence Layer):**
  * SQLite schema design (`users`, `donor_profiles`, `blood_requests`, `donations`, `notifications`, `messages`).
  * JDBC Connection management with SQLite WAL journal mode.
  * Data Access Objects (`UserDAO`, `DonorProfileDAO`, `BloodRequestDAO`, etc.) and PBKDF2 Password Hashing.

---

## 3. Quick Start & Execution

### Prerequisites
* **Java:** OpenJDK 17 LTS (bundled in `tools/` or installed on system PATH).

### Running the Desktop Client (Member 01)

#### Windows (PowerShell / Command Prompt)
```powershell
# Launch Desktop Client in Live Network Mode (connects to 127.0.0.1:5000)
.\run-client.bat

# Launch Desktop Client in Offline Mock Mode (no server required)
.\run-client.bat --mock

# Launch with custom server host/port
.\run-client.bat --host 192.168.1.100 --port 5000
```

#### Linux / Ubuntu / macOS
```bash
chmod +x run-client.sh
./run-client.sh --mock
```

---

### Running the Automated Test Suites

```powershell
# 1. Run Frontend Unit & Component Tests (52 tests)
powershell -ExecutionPolicy Bypass -File .\run-frontend-tests.ps1

# 2. Run Full-Stack Client-Server Live TCP Integration Tests (14 tests)
powershell -ExecutionPolicy Bypass -File .\run-integration-tests.ps1
```

---

## 4. Documentation Index

* 📘 [Frontend Manual](docs/FRONTEND_MANUAL.md) – Detailed screen guide, design system, and workflows.
* 🌐 [Network Architecture](docs/NETWORK_ARCHITECTURE.md) – Multithreading, single-reader thread, and EDT safety.
* 📡 [Protocol Guide](docs/PROTOCOL_GUIDE.md) – Wire framing syntax, delimiters, actions, and push events.
* 🧪 [Testing & QA Report](docs/TESTING_REPORT.md) – Comprehensive test matrices and automated test outputs.
* 🦈 [Wireshark Capture Guide](docs/WIREXHARK_GUIDE.md) – Step-by-step packet inspection and TCP handshake tutorial.
* 🎓 [Academic Evaluation & Viva Q&A](docs/VIVA_QUESTIONS_AND_ANSWERS.md) – Complete guide for practical examinations and project defense.

---

## 5. Security & Academic Integrity

* **Zero Plain-Text Passwords:** Passwords are hashed using PBKDF2 with HMAC-SHA256 (65,536 iterations) and individual salts.
* **Strict Decoupling:** The client never accesses SQLite directly; all persistence is mediated through TCP socket requests and backend permission checks.
* **Input Sanitization:** URL-encoded payload formatting prevents packet framing collisions.

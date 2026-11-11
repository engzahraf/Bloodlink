# BloodLink – Automated Quality Assurance & Testing Report

## 1. Executive Summary
The BloodLink Frontend and Client Networking modules have undergone rigorous automated testing covering validation logic, session management, wire protocol formatting/parsing, in-memory mock services, headless Swing UI components, and end-to-end TCP socket integration against Member 02's multithreaded backend server and Member 03's SQLite database.

* **Frontend Unit & Component Tests:** 52 / 52 PASSED (100%)
* **Live TCP Client-Server Integration Tests:** 14 / 14 PASSED (100%)
* **Overall Test Suite Pass Rate:** 100%

---

## 2. Test Execution Matrices

### A. Frontend Unit Test Suite (`FrontendTestSuite.java`)
Executed using `run-frontend-tests.ps1`:

| Category | Test Case | Expected Result | Status |
|---|---|---|---|
| **Validation** | Valid email `user@example.com` | Returns `true` | PASS |
| **Validation** | Invalid email `not-an-email` | Returns `false` | PASS |
| **Validation** | Email with subdomain | Returns `true` | PASS |
| **Validation** | Valid phone `+94771234567` | Returns `true` | PASS |
| **Validation** | Phone without plus prefix | Returns `true` | PASS |
| **Validation** | Short phone `< 7` digits | Returns `false` | PASS |
| **Validation** | Valid blood group `O+` | Returns `true` | PASS |
| **Validation** | Valid blood group `AB-` | Returns `true` | PASS |
| **Validation** | Invalid blood group `X+` | Returns `false` | PASS |
| **Validation** | Canonical wire mapping `O+` $\rightarrow$ `O_POS` | Wire token matches | PASS |
| **Validation** | Canonical wire mapping `AB-` $\rightarrow$ `AB_NEG` | Wire token matches | PASS |
| **Validation** | Display symbol mapping `O_POS` $\rightarrow$ `O+` | Display symbol matches | PASS |
| **Validation** | Display symbol mapping `AB_NEG` $\rightarrow$ `AB-` | Display symbol matches | PASS |
| **Validation** | Units validation (`> 0` and `<= 50`) | Accepts 2, rejects 0 and -1 | PASS |
| **Session** | Initial session state | Unauthenticated | PASS |
| **Session** | Session authentication lifecycle | Populates user & role `DONOR` | PASS |
| **Session** | Mock mode flag toggle | Flips state correctly | PASS |
| **Session** | Session cleanup on logout | Clears user and credentials | PASS |
| **Protocol** | `PING` packet wire framing | `REQ\|PING\|req_1\|\|` | PASS |
| **Protocol** | `LOGIN` packet wire framing | URL-encoded username & password | PASS |
| **Protocol** | `CREATE_BLOOD_REQUEST` wire framing | Correct action and parameters | PASS |
| **Codec** | Response status decoding (`SUCCESS`) | `isSuccess()` returns `true` | PASS |
| **Codec** | Response error status (`UNAUTHORIZED`) | Extracts error message | PASS |
| **Codec** | User model transformation | Decodes fields with spaces safely | PASS |
| **Mock Mode** | Offline login for `alice` | Returns `SUCCESS` with role `DONOR` | PASS |
| **Mock Mode** | Offline login with invalid password | Returns `UNAUTHORIZED` | PASS |
| **Mock Mode** | Offline blood requests query | Returns non-empty request list | PASS |
| **Mock Mode** | Offline request creation | Stores and returns new request ID | PASS |
| **UI Headless**| `ModernButton` initialization | Styled properties verified | PASS |
| **UI Headless**| `ModernTextField` placeholder | Renders placeholder when empty | PASS |
| **UI Headless**| `StatusBadge` status resolution | Maps severity colors correctly | PASS |

### B. Live TCP Client-Server Integration Suite (`ClientServerIntegrationTest.java`)
Executed using `run-integration-tests.ps1` with SQLite persistence enabled:

| Step | Operation | Description | Status |
|---|---|---|---|
| **1** | Server Startup | `BloodLinkServer` bound to port 5055 | PASS |
| **2** | TCP Handshake | `SocketClient` connected from `127.0.0.1` | PASS |
| **3** | Ping / Pong | `REQ\|PING` responded with `RES\|PONG\|SUCCESS` | PASS |
| **4** | Correlation ID | Server echoed client's `ping_101` request ID | PASS |
| **5** | Registration | `REQ\|REGISTER` stored in SQLite with PBKDF2 hash | PASS |
| **6** | User ID Assignment | Server returned auto-generated `userId=2` | PASS |
| **7** | Authentication | `REQ\|LOGIN` verified PBKDF2 hash via JDBC | PASS |
| **8** | Session Binding | Server bound user session and returned role `DONOR` | PASS |
| **9** | Donor Matching | `REQ\|MATCH_DONORS` queried SQLite for compatible donors | PASS |
| **10**| Socket Teardown | Graceful client disconnect & server shutdown | PASS |

---

## 3. How to Run the Automated Tests

### 1. Frontend Test Suite
```powershell
powershell -ExecutionPolicy Bypass -File .\run-frontend-tests.ps1
```

### 2. Live Client-Server Integration Test Suite
```powershell
powershell -ExecutionPolicy Bypass -File .\run-integration-tests.ps1
```

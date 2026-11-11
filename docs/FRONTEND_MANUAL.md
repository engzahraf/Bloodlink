# BloodLink – Frontend Developer Manual (Member 01)

## 1. Overview
The **BloodLink Desktop Client** is a native Java Swing application designed for connecting blood donors, recipients, and system administrators. It operates as a desktop TCP socket client communicating with Member 02's multithreaded backend server and Member 03's SQLite persistence layer.

---

## 2. Healthcare UI/UX Design System

### Color Palette
| Token | Hex | Semantic Usage |
|---|---|---|
| `COLOR_PRIMARY` | `#C62828` | Primary brand crimson, prominent action buttons, active badges |
| `COLOR_PRIMARY_DARK` | `#8E0000` | Button hover/pressed states, brand accents |
| `COLOR_BACKGROUND` | `#F8F9FA` | Application background (hospital clean off-white) |
| `COLOR_SURFACE` | `#FFFFFF` | Card containers, dialog backgrounds, tables |
| `COLOR_BORDER` | `#E0E0E0` | Card borders, subtle component dividers |
| `COLOR_TEXT_PRIMARY` | `#212121` | High-contrast body typography |
| `COLOR_TEXT_SECONDARY`| `#757575` | Captions, subtitle metadata |
| `COLOR_SUCCESS` | `#2E7D32` | Availability indicators, fulfilled badges |
| `COLOR_WARNING` | `#ED6C02` | Pending requests, mock mode indicator |
| `COLOR_CRITICAL` | `#D32F2F` | Emergency blood drive alerts, urgent badges |

### Typography Hierarchy
* **Header:** `Segoe UI Bold 22pt`
* **Title:** `Segoe UI Bold 17pt`
* **Subtitle:** `Segoe UI Bold 14pt`
* **Body:** `Segoe UI Plain 13pt`
* **Stat Number:** `Segoe UI Bold 28pt`
* **Caption & Badge:** `Segoe UI Plain 11pt`

---

## 3. Application Screens & User Workflows

### A. Authentication (`LoginFrame.java` & `RegisterFrame.java`)
* **Login:** Validates username/email and password. Provides a password visibility eye toggle, offline mock mode toggle, and live server connectivity badge. Directs users automatically to their role-specific dashboard (`AdminDashboard`, `DonorDashboard`, or `RecipientDashboard`).
* **Registration:** Validates full name, email format, phone digits (7-15 digits), password confirmation, role (`DONOR` or `RECIPIENT`), and blood group selection.

### B. Admin Dashboard (`AdminDashboard.java`)
* **System Metrics:** Displays total registered accounts, active donors, recipients, and open blood requests via `StatCard` components.
* **Global Oversight:** Tabbed view displaying:
  1. Active Blood Requests across all hospitals and cities.
  2. Complete User Registry with role badges and contact information.
* **Actions:** Quick links to broadcast emergency requests, browse donors, and refresh registry tables.

### C. Donor Dashboard (`DonorDashboard.java`)
* **Profile Summary:** Blood group badge (`O+`, `A-`, etc.), current real-time donation status (`AVAILABLE` vs `UNAVAILABLE`), and completed donations count.
* **Matching Blood Drives:** Automatically queries and lists blood requests matching the donor's compatible blood group.
* **Direct Messaging:** Allows the donor to click on any matching request and launch a direct chat with the recipient hospital.

### D. Recipient Dashboard (`RecipientDashboard.java`)
* **Request Management:** Tracks submitted blood requests with real-time status indicators (`OPEN`, `IN_PROGRESS`, `FULFILLED`).
* **Request Submission:** Opens `BloodRequestPanel` to specify required blood group, units needed, hospital name, city, and urgency level (`NORMAL`, `URGENT`, `CRITICAL`).
* **Donor Matching:** One-click launch to find compatible donors for any active request.

### E. Donor Search (`DonorSearchPanel.java`)
* **Filters:** Multi-criteria search by Blood Group, City, and Availability.
* **Results Table:** Displays matching donor name, blood group, location, status, total donations, and phone number.
* **Action:** Direct message button to initiate conversation with selected donor.

### F. Notifications Center (`NotificationPanel.java`)
* Displays system announcements and unsolicited server push alerts (`URGENT_ALERT`).
* Unread badges and "Mark All as Read" action.

### G. Direct Messaging (`MessagingPanel.java`)
* Conversational bubble layout distinguishing outgoing messages (Crimson) and incoming messages (White).
* Immediate real-time rendering when unsolicited `MSG` packets are pushed from the server.

---

## 4. Operating Modes

### 1. Offline Mock Mode
* Activated via `--mock` CLI flag or by checking "Use Offline Mock Mode" on the login screen.
* Uses `MockDataService` to demonstrate all screens, mock authentication (e.g. `alice` / `secret123`), mock donor search, and in-memory blood requests without requiring a running TCP server.
* Prominently displays an amber `MOCK MODE (OFFLINE)` banner.

### 2. Live Network Mode
* Connects to the real multithreaded TCP server on port 5000 (configurable via `--host` and `--port`).
* Performs live database authentication, transaction logging, PBKDF2 hash verification, and real-time push alerts.

---

## 5. Compilation & Execution

### Windows (PowerShell / CMD)
```powershell
# Launch in Live Mode
.\run-client.bat

# Launch in Offline Mock Mode
.\run-client.bat --mock

# Run automated frontend test suite (52 tests)
powershell -ExecutionPolicy Bypass -File .\run-frontend-tests.ps1

# Run live client-server integration test suite (14 tests)
powershell -ExecutionPolicy Bypass -File .\run-integration-tests.ps1
```

### Linux / Ubuntu
```bash
chmod +x run-client.sh
./run-client.sh --mock
```

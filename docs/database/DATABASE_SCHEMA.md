# BloodLink – Database Schema Documentation

## 1. Overview
BloodLink utilizes **SQLite 3** as its embedded relational database engine. All database operations strictly adhere to **3rd Normal Form (3NF)** with explicit foreign key enforcement, uniqueness constraints, check constraints, and performance indexes.

---

## 2. Relational Schema Specification

### 2.1 Table: `users`
Stores all registered accounts across all three user roles (Administrators, Donors, Recipients).

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `user_id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique identifier for each user |
| `full_name` | `TEXT` | `NOT NULL` | User's full legal or preferred name |
| `email` | `TEXT` | `UNIQUE NOT NULL` | Login email address (case-insensitive) |
| `password_hash` | `TEXT` | `NOT NULL` | Salted PBKDF2 with HMAC-SHA256 password hash |
| `phone` | `TEXT` | `NULLABLE` | Contact telephone number |
| `role` | `TEXT` | `NOT NULL CHECK(role IN ('ADMIN', 'DONOR', 'RECIPIENT'))` | Role in system |
| `account_status` | `TEXT` | `NOT NULL DEFAULT 'ACTIVE' CHECK(account_status IN ('ACTIVE', 'BLOCKED'))` | Account standing |
| `created_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Timestamp of registration |
| `updated_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Timestamp of last profile update |

---

### 2.2 Table: `donor_profiles`
Stores supplemental medical and logistical details for registered blood donors (1-to-1 relationship with `users`).

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `donor_id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique identifier for donor record |
| `user_id` | `INTEGER` | `UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE` | Associated user account |
| `blood_group` | `TEXT` | `NOT NULL CHECK(blood_group IN ('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'))` | ABO/Rh blood group |
| `location` | `TEXT` | `NOT NULL` | City, district, or hospital proximity |
| `availability_status` | `TEXT` | `NOT NULL DEFAULT 'AVAILABLE' CHECK(availability_status IN ('AVAILABLE', 'UNAVAILABLE'))` | Immediate willingness to donate |
| `last_donation_date` | `TEXT` | `NULLABLE` | Date of last completed blood donation |
| `created_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Timestamp of profile creation |
| `updated_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Timestamp of last profile change |

---

### 2.3 Table: `blood_requests`
Stores blood donation requests submitted by recipients.

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `request_id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique identifier for blood request |
| `recipient_id` | `INTEGER` | `NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE` | Requesting recipient user ID |
| `blood_group` | `TEXT` | `NOT NULL CHECK(blood_group IN ('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'))` | Blood type needed |
| `units_required` | `INTEGER` | `NOT NULL CHECK(units_required > 0)` | Blood units required (pints/bags) |
| `hospital_name` | `TEXT` | `NOT NULL` | Target medical facility |
| `location` | `TEXT` | `NOT NULL` | City or geographic region |
| `contact_number` | `TEXT` | `NOT NULL` | Emergency phone number |
| `urgency_level` | `TEXT` | `NOT NULL CHECK(urgency_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))` | Severity / triage level |
| `request_status` | `TEXT` | `NOT NULL DEFAULT 'PENDING' CHECK(request_status IN ('PENDING', 'MATCHED', 'ACCEPTED', 'COMPLETED', 'CANCELLED', 'REJECTED'))` | Lifecycle stage |
| `description` | `TEXT` | `NULLABLE` | Patient diagnosis or extra instructions |
| `created_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Submission timestamp |
| `updated_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Status update timestamp |

---

### 2.4 Table: `donations`
Maintains verified blood donation fulfillment events.

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `donation_id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique identifier for donation event |
| `request_id` | `INTEGER` | `NOT NULL REFERENCES blood_requests(request_id) ON DELETE RESTRICT ON UPDATE CASCADE` | Associated request |
| `donor_id` | `INTEGER` | `NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT ON UPDATE CASCADE` | Participating donor |
| `donation_date` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Date of donation |
| `donation_status` | `TEXT` | `NOT NULL DEFAULT 'SCHEDULED' CHECK(donation_status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'))` | Fulfillment status |
| `notes` | `TEXT` | `NULLABLE` | Medical notes, transfusion remarks |
| `created_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Record creation timestamp |

---

### 2.5 Table: `notifications`
Stores system alerts, urgent request announcements, and status updates for users.

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `notification_id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique notification identifier |
| `user_id` | `INTEGER` | `NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE` | Recipient user ID |
| `notification_type` | `TEXT` | `NOT NULL` | `BLOOD_REQUEST`, `DONATION_UPDATE`, etc. |
| `message` | `TEXT` | `NOT NULL` | Notification text |
| `related_request_id` | `INTEGER` | `NULLABLE REFERENCES blood_requests(request_id) ON DELETE SET NULL ON UPDATE CASCADE` | Linked request (if applicable) |
| `is_read` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK(is_read IN (0, 1))` | Read status flag (0=unread, 1=read) |
| `created_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Notification generation time |

---

### 2.6 Table: `messages`
Stores direct donor-to-recipient chat messages.

| Column Name | Data Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `message_id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | Unique message identifier |
| `sender_id` | `INTEGER` | `NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE` | Sender user ID |
| `receiver_id` | `INTEGER` | `NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE` | Recipient user ID |
| `request_id` | `INTEGER` | `NULLABLE REFERENCES blood_requests(request_id) ON DELETE SET NULL ON UPDATE CASCADE` | Contextual request ID |
| `message_content` | `TEXT` | `NOT NULL` | Message payload |
| `sent_at` | `TEXT` | `NOT NULL DEFAULT (datetime('now', 'localtime'))` | Transmission timestamp |
| `is_read` | `INTEGER` | `NOT NULL DEFAULT 0 CHECK(is_read IN (0, 1))` | Read status flag (0=unread, 1=read) |

---

## 3. Database Indexes

To maintain sub-millisecond query performance under load, the following B-tree indexes are automatically initialized:

1. `idx_users_email` – `ON users(email)`
2. `idx_users_role` – `ON users(role)`
3. `idx_donor_user_id` – `ON donor_profiles(user_id)`
4. `idx_donor_blood_group` – `ON donor_profiles(blood_group)`
5. `idx_donor_location` – `ON donor_profiles(location)`
6. `idx_donor_availability` – `ON donor_profiles(availability_status)`
7. `idx_requests_recipient` – `ON blood_requests(recipient_id)`
8. `idx_requests_status` – `ON blood_requests(request_status)`
9. `idx_requests_blood_group` – `ON blood_requests(blood_group)`
10. `idx_donations_donor` – `ON donations(donor_id)`
11. `idx_donations_request` – `ON donations(request_id)`
12. `idx_notifications_user` – `ON notifications(user_id, is_read)`
13. `idx_messages_conversation` – `ON messages(sender_id, receiver_id)`
14. `idx_messages_request` – `ON messages(request_id)`


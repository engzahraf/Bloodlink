# BloodLink – Entity Relationship (ER) Diagram

## 1. Overview
This document specifies the conceptual, logical, and physical entity relationships in the BloodLink system.

---

## 2. Mermaid Entity-Relationship Diagram

```mermaid
erDiagram
    users ||--o| donor_profiles : "has profile (0..1)"
    users ||--o{ blood_requests : "submits (1..N)"
    users ||--o{ donations : "donates in (1..N)"
    users ||--o{ notifications : "receives (0..N)"
    users ||--o{ messages : "sends (0..N)"
    users ||--o{ messages : "receives (0..N)"

    blood_requests ||--o{ donations : "fulfilled by (0..N)"
    blood_requests ||--o{ notifications : "triggers (0..N)"
    blood_requests ||--o{ messages : "relates to (0..N)"

    users {
        int user_id PK
        string full_name
        string email UK
        string password_hash
        string phone
        string role
        string account_status
        string created_at
        string updated_at
    }

    donor_profiles {
        int donor_id PK
        int user_id FK,UK
        string blood_group
        string location
        string availability_status
        string last_donation_date
        string created_at
        string updated_at
    }

    blood_requests {
        int request_id PK
        int recipient_id FK
        string blood_group
        int units_required
        string hospital_name
        string location
        string contact_number
        string urgency_level
        string request_status
        string description
        string created_at
        string updated_at
    }

    donations {
        int donation_id PK
        int request_id FK
        int donor_id FK
        string donation_date
        string donation_status
        string notes
        string created_at
    }

    notifications {
        int notification_id PK
        int user_id FK
        string notification_type
        string message
        int related_request_id FK
        int is_read
        string created_at
    }

    messages {
        int message_id PK
        int sender_id FK
        int receiver_id FK
        int request_id FK
        string message_content
        string sent_at
        int is_read
    }
```

---

## 3. Cardinalities and Referential Actions

| Parent Table | Child Table | Relationship | On Delete | On Update | Business Logic |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `users` | `donor_profiles` | 1-to-1 (Optional) | `CASCADE` | `CASCADE` | Deleting a user purges their donor profile. |
| `users` | `blood_requests` | 1-to-Many | `CASCADE` | `CASCADE` | Deleting a recipient removes their active/past requests. |
| `users` | `donations` | 1-to-Many | `RESTRICT` | `CASCADE` | Historical donation records cannot be orphaned or deleted arbitrarily. |
| `blood_requests` | `donations` | 1-to-Many | `RESTRICT` | `CASCADE` | Requests with logged donations cannot be removed without explicit audit. |
| `users` | `notifications` | 1-to-Many | `CASCADE` | `CASCADE` | Notifications are owned by the recipient user. |
| `blood_requests` | `notifications`| 1-to-Many (Optional)| `SET NULL` | `CASCADE` | Deleting a request nullifies the request reference on notifications. |
| `users` | `messages` (`sender_id`) | 1-to-Many | `CASCADE` | `CASCADE` | User deletion purges associated chat records. |
| `users` | `messages` (`receiver_id`) | 1-to-Many | `CASCADE` | `CASCADE` | User deletion purges associated chat records. |
| `blood_requests` | `messages` | 1-to-Many (Optional) | `SET NULL` | `CASCADE` | General conversation preserved even if specific request is cleared. |


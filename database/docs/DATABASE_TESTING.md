# BloodLink – Database Testing Documentation

## 1. Overview
The database layer contains a complete, automated verification suite implemented in `DatabaseTestSuite.java`. The test suite runs against an isolated SQLite database file (`data/test_bloodlink.db`), ensuring zero risk to production data.

---

## 2. Test Execution Command

### Running the Test Suite
```powershell
# From project root:
java -cp "database\bin;database\lib\*" com.bloodlink.database.DatabaseTestSuite
```

---

## 3. Test Cases & Verification Results

All 22 test cases execute with **100% pass rate (22/22 passed)**:

| # | Test Name | Target Behavior Verified | Result |
| :--- | :--- | :--- | :--- |
| 1 | Database connection | Verified valid JDBC connection with PRAGMA settings | **PASS** |
| 2 | Database initialization | Safe idempotent execution of table and index DDL | **PASS** |
| 3 | Table creation | Verified existence of all 6 tables in SQLite catalog | **PASS** |
| 4 | User registration | Successfully registered user with generated key | **PASS** |
| 5 | Duplicate email handling | Rejected duplicate email with `ValidationException` | **PASS** |
| 6 | User retrieval | Retrieved user by ID and case-insensitive email | **PASS** |
| 7 | Donor profile creation | Created donor profile linked to valid user | **PASS** |
| 8 | Donor searching | Searched by blood group, location, and compatibility | **PASS** |
| 9 | Donor availability updates | Toggled availability status and verified persistence | **PASS** |
| 10 | Blood request creation | Created blood request with valid constraints | **PASS** |
| 11 | Request status changes | Verified state transition (PENDING -> MATCHED) | **PASS** |
| 12 | Donation recording | Recorded donation and updated request/donor atomically | **PASS** |
| 13 | Notification creation | Stored user notification with request reference | **PASS** |
| 14 | Notification retrieval | Retrieved unread notifications and tested `markAsRead` | **PASS** |
| 15 | Message storage | Stored direct message between users | **PASS** |
| 16 | Conversation retrieval | Retrieved full chronological bidirectional chat history | **PASS** |
| 17 | Foreign key validation | Blocked insertion referencing non-existent parent user | **PASS** |
| 18 | Invalid data handling | Blocked invalid input (units_required <= 0) | **PASS** |
| 19 | SQL injection prevention | Blocked injection attempts via PreparedStatements | **PASS** |
| 20 | Concurrent database ops | 10 simultaneous threads performed reads & writes in WAL mode | **PASS** |
| 21 | Transaction rollback | Verified rollback restores state after aborted transaction | **PASS** |
| 22 | Account blocking | Blocked user account and excluded from donor discovery | **PASS** |

---

## 4. Key Security & Resilience Validations

### 4.1 SQL Injection Defense
All queries are parameterized using `java.sql.PreparedStatement`. Malicious strings such as:
```sql
'; DROP TABLE users; --
```
are treated strictly as literal string values, preventing any database tampering.

### 4.2 Multi-threaded Concurrency (WAL Mode)
SQLite Write-Ahead Logging allows simultaneous reader threads while a writer thread is active. The test suite stresses 10 concurrent threads simultaneously performing inserts and queries, with zero thread deadlock or busy timeout failures.

### 4.3 Atomic Transactions & Rollback
Multi-statement operations (such as recording a donation and transitioning blood request status) use explicit transaction boundaries (`conn.setAutoCommit(false)`). In failure scenarios, clean `conn.rollback()` guarantees data integrity.


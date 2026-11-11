# BloodLink – Member 02 Backend Integration Guide

## 1. Introduction
This guide is specifically prepared for **Member 02 – Network Programming & Server Developer**.

The database layer is fully self-contained, thread-safe, and ready for direct import into the TCP server module. It provides high-level Data Access Object (DAO) classes, domain models, type-safe enums, and cryptographic password utilities.

---

## 2. Dependencies & Classpath Setup
The module requires only standard Java (JDK 17+) and the SQLite JDBC driver located in:
```text
Bloodlink/database/lib/sqlite-jdbc-3.51.3.0.jar
```
Include this JAR when compiling and starting the TCP Server:
```powershell
javac -cp ".;database/lib/*;database/bin" ServerMain.java
java -cp ".;database/lib/*;database/bin" ServerMain
```

---

## 3. Database Initialization (Server Startup)
At TCP server boot, call `DatabaseInitializer.initialize()` before accepting client connections:

```java
import com.bloodlink.database.DatabaseInitializer;

public class BloodLinkServer {
    public static void main(String[] args) {
        // 1. Initialize SQLite tables, schema constraints, and indexes
        DatabaseInitializer.initialize();

        // 2. Optionally seed a default system administrator if needed
        DatabaseInitializer.seedDefaultAdminIfMissing(
            "admin@bloodlink.com",
            "Admin@BloodLink2026",
            "System Administrator",
            "+94-77-1234567"
        );

        // 3. Start TCP ServerSocket listener
        startServer(8080);
    }
}
```

---

## 4. Instantiating DAOs in Multi-Threaded Server Workers
All DAOs are lightweight, stateless, and thread-safe. You can either instantiate them as singletons or create new instances per worker thread. Each DAO operation automatically fetches its own dedicated SQLite connection and closes it.

```java
import com.bloodlink.database.dao.*;

UserDAO userDAO = new UserDAO();
DonorProfileDAO donorDAO = new DonorProfileDAO();
BloodRequestDAO requestDAO = new BloodRequestDAO();
DonationDAO donationDAO = new DonationDAO();
NotificationDAO notificationDAO = new NotificationDAO();
MessageDAO messageDAO = new MessageDAO();
```

---

## 5. User Registration & Password Hashing
Always hash passwords using `PasswordHasher.hashPassword()` before persisting to the database.

```java
import com.bloodlink.database.models.User;
import com.bloodlink.database.enums.UserRole;
import com.bloodlink.database.security.PasswordHasher;

// Handling REGISTRATION socket command:
public User handleUserRegistration(String fullName, String email, String plainPassword, String phone, UserRole role) {
    // 1. Hash plaintext password using standard PBKDF2 with HMAC-SHA256
    String hashedPassword = PasswordHasher.hashPassword(plainPassword);

    // 2. Create model object
    User newUser = new User(fullName, email, hashedPassword, phone, role);

    // 3. Persist via UserDAO
    return userDAO.createUser(newUser);
}
```

---

## 6. User Authentication (Login)
Verify plaintext passwords against stored hashes using `PasswordHasher.checkPassword()`:

```java
public User handleUserLogin(String email, String plainPassword) {
    User user = userDAO.getUserByEmail(email);

    if (user == null) {
        throw new SecurityException("User not found with email: " + email);
    }

    if (!user.isActive()) {
        throw new SecurityException("Account has been blocked by administrator.");
    }

    // Constant-time PBKDF2 verification against stored hash
    boolean passwordValid = PasswordHasher.checkPassword(plainPassword, user.getPasswordHash());
    if (!passwordValid) {
        throw new SecurityException("Invalid credentials.");
    }

    return user; // Authentication successful
}
```

---

## 7. Creating Donor Profiles & Searching Donors
When a user registers as a `DONOR`, save their donor profile:

```java
import com.bloodlink.database.models.DonorProfile;
import com.bloodlink.database.enums.BloodGroup;
import com.bloodlink.database.enums.AvailabilityStatus;

// 1. Create donor profile
DonorProfile profile = new DonorProfile(
    user.getUserId(),
    BloodGroup.O_POSITIVE, // Accepts 'O+', 'O_POSITIVE', etc.
    "Colombo",
    AvailabilityStatus.AVAILABLE
);
donorDAO.createDonorProfile(profile);

// 2. Search donors medically compatible with a recipient's blood group
List<DonorProfile> matchingDonors = donorDAO.searchCompatibleDonors(BloodGroup.A_POSITIVE, "Colombo");

for (DonorProfile donor : matchingDonors) {
    System.out.println("Candidate: " + donor.getDonorName() + " (" + donor.getBloodGroup() + ") - " + donor.getDonorPhone());
}
```

---

## 8. Managing Blood Requests
When a recipient requests blood over the socket:

```java
import com.bloodlink.database.models.BloodRequest;
import com.bloodlink.database.enums.UrgencyLevel;
import com.bloodlink.database.enums.RequestStatus;

// Create request
BloodRequest request = new BloodRequest(
    recipientUser.getUserId(),
    BloodGroup.fromString("O+"),
    2, // 2 units
    "National Hospital",
    "Colombo",
    "+94-77-9876543",
    UrgencyLevel.CRITICAL,
    "Urgent bypass operation scheduled tomorrow morning."
);
BloodRequest created = requestDAO.createBloodRequest(request);

// Update status
requestDAO.updateRequestStatus(created.getRequestId(), RequestStatus.MATCHED);
```

---

## 9. Recording Donations (Atomic Transactions)
When a donation is completed, calling `DonationDAO.recordDonation()` automatically:
1. Inserts the donation record.
2. Updates the associated blood request status to `COMPLETED`.
3. Updates the donor profile's `last_donation_date`.

```java
import com.bloodlink.database.models.Donation;
import com.bloodlink.database.enums.DonationStatus;

Donation donation = new Donation(
    requestId,
    donorUserId,
    "2026-11-11",
    DonationStatus.COMPLETED,
    "Transfusion completed successfully."
);

Donation saved = donationDAO.recordDonation(donation);
```

---

## 10. Notifications & Direct Messaging

### Notifications
```java
import com.bloodlink.database.models.Notification;
import com.bloodlink.database.enums.NotificationType;

// Push notification to donor
Notification n = new Notification(
    donorUserId,
    NotificationType.BLOOD_REQUEST,
    "Urgent O+ blood requested at National Hospital!",
    requestId
);
notificationDAO.createNotification(n);

// Fetch unread notifications for a user
List<Notification> unread = notificationDAO.getUnreadNotifications(userId);
```

### Messaging
```java
import com.bloodlink.database.models.Message;

// Save chat message
Message msg = new Message(senderId, receiverId, requestId, "I will arrive at the hospital by 10 AM.");
messageDAO.saveMessage(msg);

// Retrieve full chat history
List<Message> conversation = messageDAO.getConversation(senderId, receiverId);
```

---

## 11. Exception Handling Architecture
All database exceptions inherit from runtime `DatabaseException`:

- `ValidationException`: Thrown when validation rules fail (e.g. duplicate email, missing required fields, non-positive units).
- `RecordNotFoundException`: Thrown when an expected entity does not exist.
- `DatabaseException`: Thrown when underlying SQLite driver errors occur.

Example server error handling pattern:
```java
try {
    userDAO.createUser(user);
    sendResponseToClient("SUCCESS: Registration completed");
} catch (ValidationException e) {
    sendResponseToClient("VALIDATION_ERROR: " + e.getMessage());
} catch (DatabaseException e) {
    sendResponseToClient("SERVER_ERROR: Database operation failed");
}
```


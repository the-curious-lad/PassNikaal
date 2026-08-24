# PassNikaal - Architecture Document

> Version 1.0 | Date: 2026-08-23
>
> This document is the authoritative architectural reference for PassNikaal.
> Every major decision records the reasoning behind it, not just the outcome.
> If a decision changes, the reason for the change should also be recorded here.

---

## Table of Contents

1. Problem Statement
2. Goals
3. Non-Goals MVP
4. Actors
5. Functional Requirements
6. Non-Functional Requirements
7. System Architecture Overview
8. Outpass Core - Main Backend Architecture
9. Cutoff Service Architecture
10. Authentication Architecture
11. RBAC Design
12. Outpass Lifecycle - State Machine
13. Gate Validation Flow
14. QR Design
15. 8:30 PM / 9:00 PM Cutoff Design
16. Hostel Confirmation and Notification Workflow
17. Database Entities and Relationships
18. Important Indexes
19. API Design
20. Concurrency Strategy
21. S3 Storage Strategy
22. Security Considerations
23. Error Handling
24. Deployment Architecture
25. Scalability Considerations
26. Future Improvements
27. Open Architectural Decisions Requiring Confirmation

---

## 1. Problem Statement

College hostels currently use a paper-based outpass workflow. A student who wants to leave campus must:

1. Fill a paper outpass slip.
2. Write identical information multiple times (hostel register, gate register, the slip itself).
3. Obtain a physical signature from the warden or a designated guard.
4. Show the slip at the college gate where another manual entry is made.
5. Repeat equivalent steps when returning.

This process has several compounding problems:

- **Redundant data entry** - the same name, roll number, reason, and destination are written multiple times.
- **Paper waste** - a separate slip is issued for every outpass.
- **No real-time visibility** - it is impossible to instantly determine who is currently outside campus, who has returned, or whose pass has expired.
- **Pass reuse** - a student can keep an old approved paper pass and attempt to reuse it.
- **No tamper detection** - information on a paper pass can be altered.
- **Gate bottleneck** - during peak movement, guards manually process many students one by one.
- **No centralized history** - tracking a student's outpass history requires searching through physical registers.

PassNikaal replaces this workflow with a centralized, real-time digital system.

---

## 2. Goals

1. **Eliminate paper** - no physical slip should be required for a normal outpass.
2. **Eliminate redundant entries** - student information is fetched from the database, not re-entered.
3. **Digitize approval** - approvers act through a web dashboard, not physical signatures.
4. **Enforce one-active-outpass rule** - a student cannot hold two concurrent active outpasses.
5. **Prevent pass reuse** - every QR scan is validated against live database state.
6. **Support real-time gate operations** - gate operations must be fast and reliable.
7. **Maintain accurate timestamps** - the system records when each lifecycle event occurs.
8. **Separate real-time operations from background processing** - the daily 9 PM cutoff must not impact gate responsiveness.
9. **Keep the system simple enough to understand, extend, and deploy.**
10. **Allow multi-gate support** - a student can exit Gate 1 and return through Gate 2.

---

## 3. Non-Goals MVP

The following are explicitly out of scope for the initial version:

- Offline mode / paper-register fallback beyond manual guard judgment.
- Mobile app - the web application is the primary interface for MVP.
- Advanced identity verification (biometrics, face recognition).
- Integration with official college ERP or student data systems.
- Automatic fine or penalty processing.
- Real-time analytics dashboards.
- Advanced audit logs with detailed change history per field.
- Push notifications to mobile devices - email and in-app notifications are sufficient for MVP.
- Multi-college or multi-campus support.
- Parent portal or parent notification.
- Redis caching.
- Kafka or RabbitMQ message queues.

---

## 4. Actors

### 4.1 Student

- Creates outpasses (Market or Home).
- Views own outpass status and history.
- Views the QR code associated with an approved outpass.
- Receives hostel confirmation notifications.
- Responds to hostel confirmation prompts.
- Cannot approve their own outpass.
- Cannot hold more than one active outpass at a time.

### 4.2 Approver

An approver is any person authorized to approve outpasses for one or more hostels.

Designation subtypes (stored as metadata, not as separate roles for RBAC purposes):
- Warden
- Vice Warden
- Assistant Warden
- MMCA Officer
- Authorized Guard

Approvers can:
- Approve or reject an individual outpass.
- Approve or reject selected outpasses.
- Approve all pending outpasses for their authorized hostel(s).
- Reject all pending outpasses for their authorized hostel(s).

An approver is associated with one or more hostels. This association may change (guard duty rotation). The system must support changing hostel assignments without recreating the approver account.

### 4.3 Gate Guard

- Scans QR codes at the college gate.
- Searches by roll number when QR scanning is unavailable.
- Verifies student identity against the student ID card.
- Confirms exit: APPROVED to EXITED.
- Confirms entry: EXITED to RETURNED or NOT_RETURNED to RETURNED_LATE.
- Cannot allow invalid state transitions - the backend enforces all rules.

### 4.4 Admin

- Creates and manages student accounts.
- Creates and manages approver/guard accounts.
- Assigns roles and hostel associations.
- Manages hostels as independent administrative units.
- Can view system-wide data for oversight.

---

## 5. Functional Requirements

### Authentication
- FR-01: Students can register using roll number, college email, and password.
- FR-02: College email verification is required before an account is active.
- FR-03: All authenticated sessions use JWT.
- FR-04: Each role has defined access to specific API endpoints.
- FR-05: A student cannot impersonate a guard or approver.

### Student
- FR-06: Students can create a Market or Home outpass.
- FR-07: A student can only create a new outpass if no active outpass exists.
- FR-08: Student-specific fields are pulled from the database, not trusted from the request body.
- FR-09: Students can view their current outpass status and history.

### Approver
- FR-10: Approvers see pending outpasses for their authorized hostel(s).
- FR-11: Approvers can approve/reject individually, in bulk selection, or all at once.

### Gate
- FR-12: Gate guards can scan a QR code to validate an outpass.
- FR-13: Gate guards can search by roll number as fallback.
- FR-14: Exit is only allowed if outpass state is APPROVED.
- FR-15: Entry is only allowed if state is EXITED or NOT_RETURNED.
- FR-16: Both QR and roll-number paths reach the same validation logic.

### Cutoff
- FR-17: At 9:00 PM, a background service processes that day's market outpasses.
- FR-18: APPROVED outpasses (never exited) are marked EXPIRED.
- FR-19: EXITED outpasses (not returned) are marked NOT_RETURNED.
- FR-20: RETURNED outpasses trigger the hostel confirmation workflow.
- FR-21: Batch size is configurable.

### Notifications
- FR-22: Students in RETURNED state receive a hostel confirmation prompt at 9 PM.
- FR-23: If no response within 30 minutes, a reminder is sent (up to 3 reminders).
- FR-24: After 3 reminders without response, the case is escalated to human intervention.

---

## 6. Non-Functional Requirements

- **NFR-01 Latency**: Gate operations must respond within 500ms under normal load.
- **NFR-02 Availability**: The outpass-core service should be always available during operational hours.
- **NFR-03 Scalability**: The system should support up to 8,000 students without architectural changes.
- **NFR-04 Peak Load**: Handle 100-120 concurrent student movements within a 10-minute window without degradation.
- **NFR-05 Isolation**: The 9 PM cutoff processing must not degrade gate operation response times.
- **NFR-06 Data Integrity**: All lifecycle state transitions must be atomic. No partial state updates.
- **NFR-07 Security**: JWT tokens validated on every request. Roles enforced server-side.
- **NFR-08 Auditability**: Lifecycle timestamps are permanently stored.
- **NFR-09 Maintainability**: Code readable by a junior/mid-level engineer.
- **NFR-10 Idempotency**: QR cleanup and cutoff processing must be safe to re-run if interrupted.

---

## 7. System Architecture Overview

PassNikaal is divided into two independent backend services sharing one PostgreSQL database.

```
+-----------------------------------+
|         OUTPASS CORE              |
|         (Spring Boot)             |
|                                   |
|  Auth / Student / Approver        |
|  Outpass CRUD / Approval          |
|  QR Generation / Validation       |
|  Gate Entry / Exit                |
+----------------+------------------+
                 |
                 | DB connection
                 |
+----------------v------------------+
|           PostgreSQL              |
|         (Shared Database)         |
+----------------^------------------+
                 |
                 | DB connection
                 |
+----------------+------------------+
|         CUTOFF SERVICE            |
|         (Spring Boot)             |
|                                   |
|  9 PM Batch Processing            |
|  State Transitions                |
|  Notification Dispatch            |
|  QR Cleanup (S3)                  |
+-----------------------------------+

         +---------------+
         |   AWS S3      |
         |  (QR images)  |
         +---------------+
         ^               ^
         |               |
   Outpass Core     Cutoff Service
   (generates)      (deletes)
```

### Why two services?

The cutoff service processes potentially 2,000+ outpass records at 9 PM. This batch workload is fundamentally different from a gate guard scanning a QR and expecting a 200ms response.

If the cutoff service runs inside the same JVM process as the core backend, heavy batch processing could saturate the thread pool, database connections, and CPU, slowing down gate operations at exactly the moment when students are trying to return before the deadline.

Keeping them as separate processes means:
- Each service has its own connection pool.
- Each service can be deployed, restarted, and scaled independently.
- A bug in the cutoff service cannot crash the gate validation service.

### How do they share data?

They share the same PostgreSQL database. There is no REST API between them.

**Why not a REST API from cutoff to core?**
The cutoff service only needs to read and update the database. A REST call would add latency, an additional failure point, and unnecessary coupling.

**Why not a message queue (Kafka/RabbitMQ)?**
A message queue would add operational complexity without meaningful benefit. The data is already in a shared PostgreSQL database. The cutoff service can read it directly at 9 PM. For this project's scale, this is simpler and sufficient.

---

## 8. Outpass Core - Main Backend Architecture

The outpass-core is a standard Spring Boot monolith structured by module.

### Module Structure

```
outpass-core/
src/main/java/com/passnikaal/core/
    auth/           <- Registration, login, JWT filter
    student/        <- Student profile, student endpoints
    approver/       <- Approver management, approval logic
    outpass/        <- Outpass creation, state machine
    gate/           <- QR scan, roll-number fallback, entry/exit
    notification/   <- In-app and email notification dispatch
    common/         <- Shared DTOs, enums
    config/         <- Spring Security config, JWT config, S3 config
    exception/      <- Global exception handler, domain exceptions
```

### Why a monolith?

At 6,000-8,000 students across 12-13 hostels, a microservice-per-domain approach would add deployment overhead without meaningful benefit. A well-organized monolith is easier to develop, debug, and understand. The cutoff service is the only separate service.

### Layering Rules

| Layer | Responsibility |
|---|---|
| Controller | HTTP request/response only. Delegates everything to service. No business logic. |
| Service | All business logic. Calls repositories, validates rules, triggers notifications. |
| Repository | Database access only. JPA queries. No business logic. |
| Entity | Persistence model. JPA annotations. No business logic. |
| DTO | API input/output. Validation annotations. No JPA annotations. |
| Enum | Fixed constants (OutpassStatus, Role, OutpassType, etc.) |
| Exception | Domain-specific exceptions. |
| Config | Spring Security, JWT, S3, email configurations. |

---

## 9. Cutoff Service Architecture

```
cutoff-service/
src/main/java/com/passnikaal/cutoff/
    job/            <- Scheduled job triggered at 9 PM
    processor/      <- Per-outpass state processing logic
    repository/     <- JPA repositories (shared schema)
    entity/         <- Same entities as outpass-core
    notification/   <- Notification dispatch for cutoff events
    s3/             <- S3 cleanup logic
    config/         <- DB config, S3 config, scheduler config
```

### How the 9 PM job runs

Spring's @Scheduled(cron = "0 0 21 * * *") triggers the job. The job:
1. Fetches all market outpasses for today that are in relevant states (APPROVED, EXITED, RETURNED).
2. Processes them in configurable batches (default 100).
3. For each outpass: determines what state transition or action is needed.
4. Executes the transition atomically using a conditional UPDATE.
5. Dispatches notifications as required.
6. Deletes S3 objects for passes whose QR is now obsolete.

Batch size configuration:
```properties
cutoff.batch-size=100
```
This can be changed to 500 without modifying business logic.

---

## 10. Authentication Architecture

### Registration and Login Flow

```
Student registers -> BCrypt hash password -> Save User (PENDING_VERIFICATION)
    -> Send verification email -> Student clicks link -> Account ACTIVE
    -> Student logs in (rollNumber + password) -> Validate credentials
    -> Generate JWT (userId, role) -> Return JWT to client
    -> Client sends JWT in every request -> JwtAuthenticationFilter validates
    -> Sets SecurityContext -> Spring Security applies RBAC
```

### JWT Contents

```json
{
  "sub": "userId (UUID)",
  "role": "STUDENT",
  "iat": 1234567890,
  "exp": 1234654290
}
```

**Why not include roll number, name, hostel in the JWT?**
If a student's hostel is transferred or their profile is updated, the JWT would contain stale information. Keeping only the userId and role means the backend always fetches fresh data from the database.

### Token Expiry

```
Access token:  15–30 minutes  (configurable via jwt.access-token-expiry-minutes)
Refresh token: 7 days         (configurable via jwt.refresh-token-expiry-days)
```

**Why short-lived access tokens?**
A 24-hour access token is a bad habit for a production-oriented system. If a token is stolen (e.g., from a shared device or network log), it is valid for 24 hours with no way to revoke it. A 15–30 minute access token limits the damage window.

**Why a refresh token?**
The refresh token lets the client obtain a new access token without asking the user to log in again. It is stored server-side in the refresh_tokens table, which means it can be explicitly revoked. This is the standard pattern for web applications.

**Implementation approach (simple, not a subsystem):**
- On login: generate both tokens. Return the access token in the JSON body. The refresh token is stored in the refresh_tokens table.
- The client sends the access token in the Authorization header with every request.
- When the access token expires (401), the client calls POST /api/v1/auth/refresh with the refresh token.
- The backend validates the refresh token against the database, issues a new access token, and optionally rotates the refresh token.
- On logout: delete the refresh token from the database.

Configuration:
```properties
jwt.access-token-expiry-minutes=15
jwt.refresh-token-expiry-days=7
```

### Password Hashing
BCrypt with default strength (10 rounds). Do not use MD5, SHA-1, or SHA-256 directly for passwords.

### Email Verification
A unique UUID token is generated at registration, stored in the database with an expiry time (24 hours), and emailed to the student's college email. When the student clicks the link, the token is validated and the account is activated.

---

## 11. RBAC Design

### Roles

```java
public enum Role {
    STUDENT,
    APPROVER,
    GATE_GUARD,
    ADMIN
}
```

### Approver Designation (not a separate role)

```java
public enum ApproverDesignation {
    WARDEN,
    ASSISTANT_WARDEN,
    MMCA_OFFICER,
    AUTHORIZED_GUARD
}
```

**Why not make each designation a separate role?**
Spring Security RBAC operates on roles for access control. Whether someone is a Warden or an Assistant Warden does not change what API endpoints they can call - it is administrative metadata. Treating designation as a separate enum stored on the Approver entity keeps RBAC simple while preserving the designation information.

### Access Control Matrix

| Endpoint | STUDENT | APPROVER | GATE_GUARD | ADMIN |
|---|---|---|---|---|
| POST /auth/register | public | - | - | - |
| POST /auth/login | public | public | public | public |
| POST /outpasses | yes | - | - | - |
| GET /outpasses/{id} | own only | hostel only | yes | yes |
| POST /outpasses/{id}/approve | - | hostel only | - | yes |
| POST /outpasses/{id}/reject | - | hostel only | - | yes |
| POST /outpasses/approve-selected | - | yes | - | yes |
| POST /outpasses/approve-all | - | yes | - | yes |
| POST /gate/exit | - | - | yes | yes |
| POST /gate/entry | - | - | yes | yes |
| GET /students/{id}/outpasses | own only | - | - | yes |
| Admin endpoints | - | - | - | yes |

### Implementation

RBAC is implemented through SecurityFilterChain in Spring Security using requestMatchers - NOT scattered @PreAuthorize checks in every controller method. A single configuration class defines all access rules. Additional ownership checks (e.g., can this approver act on this hostel's passes?) happen in the service layer.

---

## 12. Outpass Lifecycle - State Machine

### States

```java
public enum OutpassStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXITED,
    RETURNED,
    NOT_RETURNED,
    RETURNED_LATE,
    EXPIRED
}
```

### State Transition Diagram

```
         +----------+
         |  PENDING |
         +----+-----+
              |
     +--------+--------+
     |                 |
     v                 v
  REJECTED          APPROVED
                       |
              +--------+--------+
              |                 |
              v                 v
           EXPIRED            EXITED
        (never exited)           |
                       +--------+--------+
                       |                 |
                       v                 v
                   RETURNED       NOT_RETURNED
                       |                 |
                       v                 v
                   EXPIRED        RETURNED_LATE
                (9PM cleanup)
```

### Valid Transitions Table

| From | To | Triggered By |
|---|---|---|
| PENDING | APPROVED | Approver action |
| PENDING | REJECTED | Approver action |
| APPROVED | EXITED | Gate guard exit scan |
| APPROVED | EXPIRED | 9 PM cutoff - never exited |
| EXITED | RETURNED | Gate guard entry scan (before cutoff) |
| EXITED | NOT_RETURNED | 9 PM cutoff - still EXITED at 9 PM |
| NOT_RETURNED | RETURNED_LATE | Gate guard entry scan (after cutoff) |
| RETURNED | EXPIRED | 9 PM cutoff - pass cleanup |

### Active Outpass Definition

An outpass is considered active (blocking creation of a new one) if its status is one of:
```
PENDING, APPROVED, EXITED, NOT_RETURNED
```

NOT_RETURNED is included because the student's movement is still unresolved.
RETURNED and RETURNED_LATE are NOT active - the outpass is complete.
REJECTED and EXPIRED are NOT active - the student can create a new one.

### Lifecycle Timestamps

```
created_at      -> Always set on creation
approved_at     -> Set when PENDING -> APPROVED
rejected_at     -> Set when PENDING -> REJECTED
exited_at       -> Set when APPROVED -> EXITED
returned_at     -> Set when EXITED -> RETURNED or NOT_RETURNED -> RETURNED_LATE
expired_at      -> Set when pass is EXPIRED
```

**Why separate timestamp columns instead of a single updated_at?**
updated_at only tells you when the last change happened. These are distinct facts with distinct business meanings - storing them separately allows querying average return times, exit durations, etc. without log-mining.

---

## 13. Gate Validation Flow

### QR Scan Path

```
Guard scans QR -> Extract token -> Lookup outpass by token in DB
    -> Token not found -> 403 Invalid QR
    -> Determine action: EXIT or ENTRY

EXIT:
    Is status APPROVED? -> YES -> atomic UPDATE to EXITED (conditional)
                        -> NO  -> 409 Invalid state for exit

ENTRY:
    Is status EXITED?        -> YES -> atomic UPDATE to RETURNED
    Is status NOT_RETURNED?  -> YES -> atomic UPDATE to RETURNED_LATE
    Otherwise               -> 409 Invalid state for entry
```

### Roll Number Fallback Path

```
Guard enters roll number and intended action (EXIT or ENTRY)
    -> Find the currently actionable outpass for this student:
         For EXIT:  look for outpass with status = APPROVED
         For ENTRY: look for outpass with status = EXITED or NOT_RETURNED
    -> No actionable outpass found for this action -> 404
    -> Outpass found -> pass outpassId to the same GateValidationService.process(action, outpassId)
```

Both paths converge at **the same `GateValidationService` method**. There is no separate trusted code path for roll-number search. The roll-number path is only a lookup shortcut — the actual state validation, conditional UPDATE, and timestamp logic are identical regardless of how the outpass was found.

**Why the fallback must be action-aware:**
A student might have a RETURNED outpass (history) and no active outpass. "Find most recent outpass" would return the RETURNED one, which would then fail validation. By querying specifically for the state that is valid for the requested action, the fallback returns something actionable or nothing.

**What the guard provides:**
The gate UI asks the guard to select the operation (Exit or Entry) before doing the roll-number lookup. This prevents ambiguity about what to look for.

### Why the backend is the source of truth

The QR image is just a credential - a way to look up the outpass quickly. Once the outpass is RETURNED, EXPIRED, or REJECTED, the QR image in S3 becomes irrelevant. If a student screenshots an old QR, the backend will reject it because the database state no longer allows exit.

---

## 14. QR Design

### QR Payload

```json
{
  "outpassId": "uuid",
  "token": "secure-random-hex-token"
}
```

**Why include a token?**
If the QR only contained the outpass UUID, anyone who knows a valid UUID could construct their own QR. The token is a 256-bit cryptographically random string that is impossible to guess.

**What is NOT in the QR:** student name, roll number, hostel, room number, any personal information.

The QR is a lookup key, not a data record. The backend fetches all relevant information from the database using the token + outpassId.

### QR Image Generation

QR images are generated using the ZXing Java library when an outpass is approved. The image is uploaded to S3. The S3 object key is stored in the outpasses table.

### QR Cleanup

At 9 PM, the cutoff service deletes S3 objects for passes whose QR is no longer needed. This is a batch cleanup - not an immediate deletion on return.

**Why not delete the QR immediately when the student returns?**
Batch deletion is simpler and more efficient. There is no security risk - the QR is already logically invalid once the database state changes.

---

## 15. 8:30 PM / 9:00 PM Cutoff Design

### Timeline

```
08:30 PM -> Nominal market outpass return deadline (informational only)
08:59:59 -> Last second before cutoff
09:00:00 -> Background cutoff processing begins (LATE threshold)
09:30:00 -> Reminder 1 if no hostel confirmation
10:00:00 -> Reminder 2
10:30:00 -> Reminder 3
```

### What late means precisely

```
returnedAt before 21:00:00        ->  not late, state is RETURNED
returnedAt at or after 21:00:00   ->  late, handled by NOT_RETURNED -> RETURNED_LATE
```

### Market Outpass Creation Cutoff

Students should not be allowed to create a Market outpass after 7:00 PM. The cutoff time is configurable:

```properties
outpass.market.creation-cutoff-time=19:00
```

**Why 7:00 PM specifically?**
This is the explicitly agreed requirement. Creating a market outpass after 7 PM gives a student less than 2 hours to exit campus, go to a destination, and return before the 9 PM cutoff. This creates noise in the cutoff batch (many APPROVED passes that were never exited) and defeats the purpose of the market outpass workflow.

This is separate from the 9 PM return cutoff. Home outpasses have no creation cutoff.

---

## 16. Hostel Confirmation and Notification Workflow

### What it solves

A student can return to campus (scan QR at gate -> RETURNED) and still not be in their hostel room. The hostel confirmation workflow asks the student to confirm they have reached their hostel.

### Trigger

At 9 PM, any outpass in RETURNED state triggers a notification to the student.

### Confirmation States

```java
public enum HostelConfirmationStatus {
    NOT_REQUIRED,      // For home outpasses, or passes that never exited
    PENDING,           // Notification sent, awaiting response
    CONFIRMED,         // Student confirmed hostel arrival
    ESCALATED          // No response after 3 reminders
}
```

### Workflow

```
9:00 PM -> Send "Have you reached your hostel?" notification
    |
    +-- YES -> CONFIRMED (workflow ends)
    |
    +-- No response within 30 min -> 9:30 PM Reminder 1
                                        |
                                        +-- No response -> 10:00 PM Reminder 2
                                                              |
                                                              +-- No response -> 10:30 PM Reminder 3
                                                                                    |
                                                                                    +-- No response -> ESCALATED
```

### Reminder Scheduling Approach

Three fixed-time @Scheduled jobs run at 9:30, 10:00, and 10:30 PM. Each job picks up all outpasses whose hostel_confirmation_status is PENDING and reminder_count is below the threshold. It sends the reminder and increments reminder_count. This is simpler and more predictable than scheduling individual reminders per student.

### Notification Delivery for MVP

Email (via JavaMailSender) plus in-app notifications stored in the notifications table. In-app notifications are cheap to build and significantly improve UX.

---

## 17. Database Entities and Relationships

### Entity Overview

```
User (1) -- (1) Student
User (1) -- (1) Approver
Hostel (1) -- (N) Students
Hostel (N) -- (N) Approvers via approver_hostel_assignments
Student (1) -- (N) Outpasses
Approver (1) -- (N) Outpasses (approved_by)
Outpass (1) -- (N) Notifications
User (1) -- (N) Notifications
User (1) -- (1) EmailVerificationToken
```

### users table

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL,
    account_status  VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**Why one users table instead of separate tables per role?**
Authentication is role-agnostic. The users table handles login, password hashing, email, and account status for all roles. Role-specific information is in separate linked tables. This avoids duplicating auth logic and makes login straightforward.

### students table

```sql
CREATE TABLE students (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id),
    roll_number     VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(20),
    branch          VARCHAR(100),
    hostel_id       UUID REFERENCES hostels(id),
    room_number     VARCHAR(20),
    photo_url       VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### hostels table

```sql
CREATE TABLE hostels (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
    type        VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### approvers table

```sql
CREATE TABLE approvers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id),
    name            VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(20),
    designation     VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### approver_hostel_assignments table

```sql
CREATE TABLE approver_hostel_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approver_id     UUID NOT NULL REFERENCES approvers(id),
    hostel_id       UUID NOT NULL REFERENCES hostels(id),
    assigned_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (approver_id, hostel_id)
);
```

**Why a join table instead of a single hostel_id on approvers?**
An approver may be responsible for more than one hostel. A join table supports N:M without schema changes.

### outpasses table

```sql
CREATE TABLE outpasses (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id                  UUID NOT NULL REFERENCES students(id),
    approved_by                 UUID REFERENCES approvers(id),
    outpass_type                VARCHAR(20) NOT NULL,
    status                      VARCHAR(30) NOT NULL,
    reason                      TEXT NOT NULL,
    destination                 VARCHAR(255) NOT NULL,
    destination_city            VARCHAR(100),
    destination_state           VARCHAR(100),
    intended_exit_time          TIMESTAMP,
    home_from_date              DATE,
    home_to_date                DATE,
    rejection_reason            TEXT,
    qr_token                    VARCHAR(255) UNIQUE,
    qr_s3_key                   VARCHAR(500),
    hostel_confirmation_status  VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED',
    reminder_count              INT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at                 TIMESTAMP,
    rejected_at                 TIMESTAMP,
    exited_at                   TIMESTAMP,
    returned_at                 TIMESTAMP,
    expired_at                  TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### One Active Outpass Constraint at Database Level

```sql
CREATE UNIQUE INDEX one_active_outpass_per_student
ON outpasses (student_id)
WHERE status IN ('PENDING', 'APPROVED', 'EXITED', 'NOT_RETURNED');
```

**Why a partial unique index instead of an application-level check?**
Two simultaneous requests could both pass an if check at the application level and both insert a new outpass. A PostgreSQL partial unique index enforces the constraint at the database level - the second concurrent insert will fail with a unique constraint violation, caught and handled gracefully by the application.

### email_verification_tokens table

```sql
CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE
);
```

### refresh_tokens table

Stores server-side refresh tokens. This enables explicit revocation (logout, stolen token, admin invalidation).

```sql
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

**Why store the hash and not the raw token?**
If the refresh_tokens table is compromised, raw tokens would allow an attacker to immediately impersonate users. Storing the hash means the attacker gets only the hash, which cannot be used directly. The raw token is only ever sent to the client; the server only ever compares hashes.

**Index:**
```sql
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
```

### notifications table

```sql
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outpass_id      UUID REFERENCES outpasses(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    type            VARCHAR(100) NOT NULL,
    message         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at         TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## 18. Important Indexes

### Auto-created by constraints

```sql
UNIQUE INDEX ON users(email)
UNIQUE INDEX ON students(roll_number)
UNIQUE INDEX ON outpasses(qr_token)
UNIQUE PARTIAL INDEX one_active_outpass_per_student
    ON outpasses(student_id)
    WHERE status IN ('PENDING', 'APPROVED', 'EXITED', 'NOT_RETURNED')
```

### Additional indexes with justification

```sql
-- Outpass history for a student
-- Query: WHERE student_id = ? ORDER BY created_at DESC
CREATE INDEX idx_outpasses_student_created
    ON outpasses (student_id, created_at DESC);

-- 9 PM cutoff: fetch today's market outpasses in relevant states
-- Query: WHERE outpass_type = 'MARKET' AND created_at >= today AND status IN (...)
CREATE INDEX idx_outpasses_type_status_created
    ON outpasses (outpass_type, status, created_at);

-- Approver dashboard: pending outpasses sorted by time
CREATE INDEX idx_outpasses_status_created
    ON outpasses (status, created_at);

-- Notification inbox query
CREATE INDEX idx_notifications_user_read
    ON notifications (user_id, is_read);
```

**Why not index every column?**
Indexes speed up reads but slow down writes. For a system that does frequent state updates on outpasses, unnecessary indexes increase write overhead. We add indexes only where the query pattern is clearly frequent or latency-sensitive.

---

## 19. API Design

### Base URL: /api/v1

### Auth
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
GET    /api/v1/auth/verify-email
POST   /api/v1/auth/resend-verification
```

### Student
```
GET    /api/v1/students/me
PUT    /api/v1/students/me
```

### Outpass
```
POST   /api/v1/outpasses
GET    /api/v1/outpasses/{id}
GET    /api/v1/outpasses/{id}/status
GET    /api/v1/outpasses/{id}/qr
GET    /api/v1/outpasses/my
GET    /api/v1/outpasses/pending
```

### Approval
```
POST   /api/v1/outpasses/{id}/approve
POST   /api/v1/outpasses/{id}/reject
POST   /api/v1/outpasses/approve-selected
POST   /api/v1/outpasses/reject-selected
POST   /api/v1/outpasses/approve-all
POST   /api/v1/outpasses/reject-all
```

### Gate
```
POST   /api/v1/gate/exit
POST   /api/v1/gate/entry
GET    /api/v1/gate/lookup?rollNumber=...
```

### Admin
```
POST   /api/v1/admin/students
GET    /api/v1/admin/students
PUT    /api/v1/admin/students/{id}
POST   /api/v1/admin/approvers
PUT    /api/v1/admin/approvers/{id}
POST   /api/v1/admin/approvers/{id}/assign-hostel
DELETE /api/v1/admin/approvers/{id}/remove-hostel
POST   /api/v1/admin/hostels
GET    /api/v1/admin/hostels
```

### Notifications
```
GET    /api/v1/notifications
POST   /api/v1/notifications/{id}/read
POST   /api/v1/outpasses/{id}/confirm-hostel
```

### Create Outpass Request DTOs

Market Outpass:
```json
{
  "type": "MARKET",
  "reason": "Buying stationery",
  "destination": "City Center Market",
  "intendedExitTime": "2026-08-23T16:00:00"
}
```

Home Outpass:
```json
{
  "type": "HOME",
  "reason": "Festival holidays",
  "destination": "Jaipur",
  "destinationCity": "Jaipur",
  "destinationState": "Rajasthan",
  "fromDate": "2026-08-25",
  "toDate": "2026-08-30"
}
```

The backend derives: student ID, name, roll number, hostel, room number from the JWT and database. The client never sends these.

### Standard Error Response

```json
{
  "timestamp": "2026-08-23T21:00:00Z",
  "status": 409,
  "error": "DUPLICATE_ACTIVE_OUTPASS",
  "message": "You already have an active outpass.",
  "path": "/api/v1/outpasses"
}
```

---

## 20. Concurrency Strategy

### The Core Problem

Two concurrent operations can attempt to transition the same outpass:
1. Gate guard tries to mark EXITED to RETURNED.
2. Cutoff service at 9:00:00 PM tries to mark EXITED to NOT_RETURNED.

### Solution: Conditional UPDATE

```sql
UPDATE outpasses
SET status = 'RETURNED', returned_at = NOW(), updated_at = NOW()
WHERE id = ?
  AND status = 'EXITED'
```

The WHERE status = 'EXITED' clause means:
- If the row is still EXITED: update succeeds (1 row affected).
- If cutoff service already set it to NOT_RETURNED: update affects 0 rows.

The application checks the number of affected rows:
- 1 affected -> success.
- 0 affected -> state has changed; fetch current state and respond accordingly.

**Why not optimistic locking (@Version)?**
Optimistic locking throws an exception requiring a retry. Conditional UPDATE is more explicit and handles 0-row-affected gracefully without exception overhead.

**Why not pessimistic locking (SELECT FOR UPDATE)?**
Pessimistic locking holds a database row lock until the transaction commits. Under 9 PM peak processing it could cause lock contention between the cutoff service and gate operations. Conditional UPDATE is lock-free and achieves the same correctness guarantee.

### One-Active-Outpass Race Condition

Two simultaneous create-outpass requests could both pass an application-level check. The partial unique index at the database level prevents this. Thread 2's INSERT will fail with a unique constraint violation. The application catches this and returns a 409 Conflict response.

---

## 21. S3 Storage Strategy

### What is stored in S3

QR images only. One object per approved outpass.

### Object Key Format: qr-codes/{outpassId}.png

### Security

QR images are NOT publicly accessible. The backend streams the S3 object through an authenticated API endpoint. This avoids pre-signed URL expiry complexity and keeps S3 private.

### Cleanup

At 9 PM, the cutoff service:
1. Collects the qr_s3_key values of all outpasses being expired.
2. Batch-deletes S3 objects.
3. Sets qr_s3_key to NULL in the database after successful deletion.

If S3 deletion fails for an object, the database state (e.g., EXPIRED) still makes the QR logically invalid. The cleanup can be re-run safely.

### Credentials

```properties
aws.s3.bucket-name=${AWS_S3_BUCKET_NAME}
aws.region=${AWS_REGION}
```

---

## 22. Security Considerations

| Concern | Approach |
|---|---|
| Password storage | BCrypt (Spring Security default) |
| JWT secret | Environment variable, never hardcoded |
| JWT expiry | Short-lived access token + 7-day refresh token (D-02) |
| RBAC enforcement | Spring Security SecurityFilterChain, not scattered annotations |
| Rate limiting | Bucket4j in-memory, per-IP (public) and per-user (authenticated) |
| QR token | 256-bit cryptographically random via SecureRandom, stored in DB |
| QR image access | Streamed via authenticated API, not publicly accessible |
| S3 credentials | Environment variables or IAM role |
| Input validation | Jakarta validation annotations on all DTOs |
| SQL injection | JPA parameterized queries, never raw string concatenation |
| Information leakage | Error responses contain error codes and messages, not stack traces |
| CORS | Configure allowed origins explicitly, not wildcard in production |
| HTTPS | Enforced at the load balancer level in production |
| Account activation | Email verification token required before login |

---

## 22a. Rate Limiting

### Library
**Bucket4j** — in-memory token bucket algorithm. No Redis or external infrastructure needed for MVP. If the system scales to multiple instances later, Bucket4j supports a distributed backend (Hazelcast/Redis) with minimal code change.

Add to outpass-core pom.xml (Stage 4):
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

### Implementation
A single Spring `HandlerInterceptor` (`RateLimitInterceptor`) reads the limit tier for the request, finds or creates the bucket for the key, and either allows or rejects with `429 Too Many Requests`.

Two key types:
- **IP-based** — for public endpoints where there is no authenticated user yet.
- **User-based** — for authenticated endpoints, keyed on the `userId` from the JWT. Fairer than IP (students behind college NAT share one IP).

### Limit Tiers

| Tier | Endpoints | Key | Limit |
|---|---|---|---|
| **auth-strict** | POST /auth/login, POST /auth/register, POST /auth/resend-verification | IP | 10 requests / 10 minutes |
| **auth-refresh** | POST /auth/refresh | IP | 30 requests / 10 minutes |
| **student** | POST /outpasses, GET /outpasses/\* | userId | 60 requests / minute |
| **approver** | POST /outpasses/\*/approve, POST /outpasses/\*/reject | userId | 120 requests / minute |
| **gate** | POST /gate/exit, POST /gate/entry, GET /gate/lookup | userId | 300 requests / minute (guards scan fast) |
| **admin** | /admin/\*\* | userId | 60 requests / minute |
| **public** | GET /health | IP | 30 requests / minute |

### Rate Limit Error Response
```json
{
  "timestamp": "...",
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please wait before retrying.",
  "path": "/api/v1/auth/login"
}
```
Standard `Retry-After` header is included in the response so the client knows when to retry.

### Why not Spring Cloud Gateway or a reverse proxy?
For MVP on a single EC2, Bucket4j inside the application is simpler than configuring Nginx rate limiting or adding API Gateway. If horizontal scaling is needed later, the bucket store can be swapped to Redis.

---

## 23. Error Handling

### Domain Exceptions

```
ResourceNotFoundException          -> 404
InvalidOutpassStateException       -> 409 wrong state for the requested action
DuplicateActiveOutpassException    -> 409 student already has an active outpass
UnauthorizedException              -> 403 role or ownership check failed
InvalidQrTokenException            -> 403 unknown or expired token
OutpassCreationCutoffException     -> 422 trying to create market pass after cutoff time
AccountNotVerifiedException        -> 403 email not yet verified
```

A single @RestControllerAdvice class handles all exceptions. No exception handling logic is scattered in controllers.

---

## 24. Deployment Architecture

### Initial Deployment MVP

```
Internet -> AWS ALB -> EC2 (outpass-core)
                   -> EC2 (cutoff-service, same instance initially)
                   -> AWS RDS (PostgreSQL)
                   -> AWS S3 (QR images)
```

**Why EC2 instead of Lambda or ECS?**
Lambda has cold start issues for a latency-sensitive gate service. ECS adds Docker orchestration complexity not necessary for the initial version. EC2 is straightforward to understand, SSH into, and debug.

**Why RDS instead of self-managed PostgreSQL?**
Automated backups, managed failover, no manual PostgreSQL administration.

### Environment Variables

```
DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD
JWT_SECRET
AWS_S3_BUCKET_NAME, AWS_REGION
MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD
```

---

## 25. Scalability Considerations

### Current Scale (6,000-8,000 students)

A single outpass-core instance, one cutoff-service instance, one RDS PostgreSQL is sufficient. Peak load of 120 students in 10 minutes = 12 QR scans per minute average.

### Future Scale

1. Horizontal scaling of outpass-core: Add a second EC2 instance behind the ALB. JWT is stateless.
2. Read replicas for PostgreSQL: History queries to read replica, gate operations to primary.
3. HikariCP tuning: Pool size can be tuned per instance.
4. Caching if needed: Redis for student profile lookups if database load becomes a concern.
5. Notification queue: Spring @Async or dedicated job table before reaching for Kafka.

---

## 26. Future Improvements

| Improvement | When to consider |
|---|---|
| Mobile app and push notifications | After web MVP is stable |
| Official college ERP integration | When IT department provides API access |
| Redis caching | If RDS latency becomes measurable issue |
| Kafka/RabbitMQ for notifications | If notification volume exceeds email rate limits |
| Docker and ECS | If managing EC2 instances becomes burdensome |
| Biometric / face verification at gate | Advanced security requirement |
| Analytics dashboard | After data accumulates |

---

## 27. Confirmed Architectural Decisions

> All decisions below were confirmed by the project owner on 2026-08-24.
> This section is now a closed decisions log, not a list of open questions.
> If any decision needs to change, record the reason here and update the relevant section.

| ID | Decision | Confirmed Value | Reason |
|---|---|---|---|
| D-01 | Email verification token expiry | **24 hours** | Plenty of time for a college signup; no reason to complicate it. |
| D-02 | JWT token strategy | **15–30 min access token + 7-day refresh token (configurable)** | Long-lived access tokens are a bad habit. Short access token + server-side refresh token limits the damage window and enables revocation. |
| D-03 | Market outpass creation cutoff | **7:00 PM (configurable)** | Explicitly decided requirement. Creating after 7 PM leaves less than 2 hours to return before the 9 PM cutoff. |
| D-04 | Hostel confirmation for RETURNED_LATE | **No** | The student is already documented as late. Triggering the hostel confirmation workflow again is redundant. |
| D-05 | Roll-number fallback behavior | **Action-aware lookup feeding the same GateValidationService** | "Most recent active" is not enough. The fallback must find the currently actionable outpass for the specific gate action (EXIT needs APPROVED; ENTRY needs EXITED or NOT_RETURNED), then pass it through identical validation logic. |
| D-06 | Cutoff service deployment | **Same EC2 for dev/test; separately deployable process for production** | Services are architecturally independent but one EC2 is cost-effective for MVP. Must remain independently deployable. |
| D-07 | Rejection reason | **Yes — store and expose to student** | Useful feedback for the student and preserves an audit trail of why a request was denied. |
| D-08 | Home outpass and 9 PM cutoff | **Exempt — home outpasses are not processed by the 9 PM batch job** | The 9 PM cutoff is explicitly designed around market outpasses only. Home outpasses have their own return dates and no same-night deadline. |
| D-09 | Notification delivery | **In-app (primary) + email (fallback/escalation)** | In-app is the primary experience within the web dashboard. Email is useful when the student is not actively using the app. |
| D-10 | QR image delivery | **Backend streaming through the authenticated API** | Clients never receive direct S3 access. S3 remains private object storage. Avoids pre-signed URL expiry complexity. |

---

*End of architecture.md - Version 1.1 (decisions confirmed 2026-08-24)*

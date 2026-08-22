# PassNikaal 🚀

> A digital outpass management system designed to replace paper-based outpasses, manual registers, and redundant entry processes with a secure, real-time workflow.

---

## 📌 Overview

**PassNikaal** is a digital outpass management system designed for colleges and hostels where students are currently required to use paper slips, manual registers, and physical signatures to leave and re-enter the campus.

The existing process involves unnecessary paperwork, repeated manual data entry, delays, and the possibility of misuse of old outpasses.

PassNikaal aims to digitize the entire workflow.

A student creates an outpass online, an authorized approver approves or rejects it, and the gate guard validates the outpass using a QR code or roll-number-based search.

The system maintains the student's movement state in real time and prevents invalid actions such as:

- Exiting twice using the same outpass
- Reusing an old outpass
- Entering without previously exiting
- Creating multiple active outpasses
- Using expired or rejected passes

---

# 🎯 Problem Statement

In the traditional workflow, a student may have to:

1. Fill multiple paper outpass slips.
2. Write the same information more than once.
3. Maintain a manual diary or register entry at the hostel.
4. Get the outpass physically signed by a warden or authorized guard.
5. Show the outpass at the college gate.
6. Make another diary/register entry at the gate.
7. Repeat similar processes when returning.

This creates several problems.

### 🌳 Paper Waste

The same outpass information may be written multiple times on paper.

### ⏳ Slow and Repetitive Process

Students repeatedly write the same information in:

- Outpass slips
- Hostel registers
- College gate registers

### 🔓 Reuse of Old Outpasses

Paper-based passes can potentially be reused or manipulated.

### 📉 Poor Real-Time Visibility

It is difficult to instantly determine:

- Who is currently outside the campus
- Who has returned
- Which passes are currently valid
- Which students have not returned before the cutoff

### 🚧 Gate Restrictions

A digital system can allow students to exit from one gate and return through another, while maintaining centralized records.

---

# 💡 Solution

PassNikaal replaces the paper workflow with a centralized digital system.

The core flow is:

```text
Student
   │
   ▼
Create Outpass
   │
   ▼
PENDING
   │
   ├──────────────► REJECTED
   │
   ▼
APPROVED
   │
   ▼
Gate Verification
   │
   ▼
EXITED
   │
   ├──────────────► RETURNED
   │                      │
   │                      ▼
   │                   EXPIRED
   │
   └──────────────► NOT_RETURNED
                           │
                           ▼
                     RETURNED_LATE
```

---

# 👥 System Actors

The system currently has four major actors.

## 🎓 Student

Students can:

- Register and authenticate
- Create an outpass
- Create either a Market Outpass or Home Outpass
- View the current status of their outpass
- Access the QR code associated with an approved outpass
- Receive notifications related to hostel confirmation
- View relevant outpass history

A student cannot:

- Approve their own outpass
- Create multiple active outpasses
- Reuse an expired outpass

---

## 👨‍💼 Approver

An approver may include:

- Warden
- Vice Warden
- Assistant Warden
- MMCA Officer
- Authorized Guard
- Other authorized hostel personnel

For the MVP, an approver can approve requests belonging to the hostel they are currently authorized to handle.

Approvers can:

- Approve an individual request
- Reject an individual request
- Approve selected requests
- Reject selected requests
- Approve all currently pending requests
- Reject all currently pending requests

The approver dashboard can segregate requests based on outpass type.

For example:

### Market Requests

```text
Akshat
Market

[Approve] [Reject]
```

### Home Requests

```text
Rahul
Home

[Approve] [Reject]
```

The approver can also quickly move through requests one by one.

---

## 🛡️ Gate Guard

Gate guards are responsible for validating student movement.

They can:

- Scan the student's QR code
- Search for an outpass using roll number if QR scanning fails
- View relevant student information
- Verify the student's ID card
- Confirm student exit
- Confirm student entry

The QR itself is never blindly trusted.

Every QR scan is validated against the backend.

---

## 👨‍💻 Admin

The admin is responsible for administrative management.

Potential responsibilities include:

- Managing student accounts
- Managing approver accounts
- Assigning roles
- Managing hostel information
- Managing authorized personnel
- Maintaining system-level data

For the MVP, student self-registration with college email verification will be supported.

---

# 📝 Outpass Types

The system supports two types of outpasses.

## 🏙️ Market Outpass

A market outpass is intended for shorter outings.

Important characteristics:

- Valid for the same day
- Must be created before the configured creation cutoff
- The student can exit after approval
- The student is expected to return before the daily cutoff
- A grace period exists before late-return processing begins

---

## 🏠 Home Outpass

A home outpass is intended for longer periods away from campus.

It contains additional location information.

Unlike a market outpass, a home outpass does not have the same fixed same-day return expectation.

---

# 📋 Outpass Information

Most student information should already exist in the database.

Therefore, the student should not repeatedly fill information such as:

- Name
- Roll number
- Branch
- Hostel
- Room number
- Contact number
- Parent/guardian contact number

The system fetches this information from the student's profile.

The student only fills the information relevant to the specific outpass.

Typical information includes:

### Common Fields

- Outpass type
- Reason
- Destination
- Exit date
- Intended exit time

### Home Outpass

Additional information:

- City
- State
- Expected dates of absence

---

# 🔐 Core Business Rules

## One Active Outpass Per Student

A student can have only one active outpass at a time.

Before creating a new outpass:

```text
Create Outpass Request
        │
        ▼
Check Existing Active Outpass
        │
        ├── Active Outpass Exists
        │        │
        │        ▼
        │     Reject Creation
        │
        └── No Active Outpass
                 │
                 ▼
             Create PENDING Pass
```

This should be enforced at both levels:

1. Service/business logic
2. Database level

The database should act as the final source of truth.

---

# 🔄 Outpass Lifecycle

The primary states are:

```text
PENDING
APPROVED
REJECTED
EXITED
RETURNED
NOT_RETURNED
RETURNED_LATE
EXPIRED
```

## State Flow

```text
                 ┌─────────────┐
                 │   PENDING   │
                 └──────┬──────┘
                        │
             ┌──────────┴──────────┐
             │                     │
             ▼                     ▼
        REJECTED                APPROVED
                                    │
                                    │
                         ┌──────────┴──────────┐
                         │                     │
                         ▼                     ▼
                      EXPIRED                EXITED
                                                │
                              ┌─────────────────┴────────────────┐
                              │                                  │
                              ▼                                  ▼
                          RETURNED                         NOT_RETURNED
                              │                                  │
                              ▼                                  ▼
                           EXPIRED                        RETURNED_LATE
```

---

# 🚪 Gate Validation Rules

The action allowed depends on the current state of the outpass.

| Gate Action | Required Outpass State | Result |
|---|---|---|
| Exit Campus | `APPROVED` | `EXITED` |
| Enter Campus Before Cutoff Processing | `EXITED` | `RETURNED` |
| Enter Campus After Being Marked Not Returned | `NOT_RETURNED` | `RETURNED_LATE` |

This prevents:

- Double exits
- Double entries
- Reuse of old passes
- Entering without previously exiting
- Exiting with an expired pass

---

# 📱 QR-Based Validation

Each approved outpass receives a QR code.

The QR may contain or reference information such as:

- Student roll number
- Outpass ID
- Secure random token

However, the QR is **not the source of truth**.

Every time the QR is scanned:

```text
QR Scan
   │
   ▼
Validate Secure Token
   │
   ▼
Fetch Current Outpass State
   │
   ▼
Check Whether Requested Gate Action Is Valid
   │
   ├── Invalid → Reject
   │
   └── Valid
          │
          ▼
     Update Outpass State
```

This ensures that even if someone keeps an old QR image, it cannot be reused once the backend considers the pass invalid.

---

# 🔍 Roll Number Fallback

QR scanning may fail because of:

- Camera issues
- Damaged screens
- Network issues
- Device problems

Therefore, the gate guard can search using the student's roll number.

The backend performs the same state validation.

```text
Roll Number Search
        │
        ▼
Find Active/Relevant Outpass
        │
        ▼
Validate Current State
        │
        ▼
Allow or Reject Action
```

---

# ⏰ Daily Cutoff Flow

For Market Outpasses, the system has two important times.

```text
8:30 PM → Normal outpass cutoff
9:00 PM → Grace period ends and background processing begins
```

Students who return before 9:00 PM are treated differently from students who return after the 9:00 PM processing point.

The exact 9:00 PM boundary is considered late.

Conceptually:

```text
08:59:59 → Returned within grace period

09:00:00 → Late

09:05:00 → Late
```

---

# ⚙️ 9 PM Background Processing

A separate background service handles the daily processing.

At 9 PM, it processes that day's relevant Market Outpasses in batches.

Example:

```text
Batch 1 → 100 outpasses
Batch 2 → 100 outpasses
Batch 3 → 100 outpasses
...
```

The batch size should be configurable.

The background service handles:

- State-based processing
- Expiry of completed or unused passes
- Identification of students who have not returned
- QR cleanup
- Notification workflows
- Reminder workflows
- Escalation workflows

---

# 📌 9 PM State Processing

## Student Returned Before 9 PM

```text
EXITED
   │
   ▼
RETURNED
   │
   ▼
9 PM Processing
   │
   ├── Send Hostel Confirmation Notification
   │
   ├── Close/Expire Outpass
   │
   └── Delete QR Artifact
```

Historical timestamps are preserved.

---

## Student Was Approved but Never Exited

```text
APPROVED
   │
   ▼
9 PM Processing
   │
   ▼
EXPIRED
```

No hostel confirmation workflow is required.

---

## Student Has Not Returned

```text
EXITED
   │
   ▼
9 PM Processing
   │
   ▼
NOT_RETURNED
```

The outpass is not immediately expired because the student's situation is unresolved.

Notifications and escalation workflows begin.

---

## Student Returns Late

```text
NOT_RETURNED
   │
   ▼
Student Enters Campus
   │
   ▼
RETURNED_LATE
```

This explicitly preserves the fact that the student returned after the defined cutoff.

---

# 🔔 Hostel Confirmation Workflow

At 9 PM, students who have already returned to campus can receive a notification asking whether they have reached their hostel.

Example:

```text
Have you reached your hostel?

[Yes]
[No]
```

If the student confirms:

```text
HOSTEL_CONFIRMED
```

The workflow ends.

If the student does not respond:

```text
9:00 PM → Initial Notification

9:30 PM → Reminder 1

10:00 PM → Reminder 2

10:30 PM → Reminder 3

No Response
      │
      ▼
Human Escalation
```

The system does not attempt to solve every possible human error or administrative issue.

At the escalation point, human interaction takes over.

---

# 🏗️ System Architecture

The project is divided into two major backend services.

```text
                         ┌──────────────────────┐
                         │   OUTPASS CORE       │
                         │                      │
                         │ Authentication       │
                         │ Student Management   │
                         │ Outpass Creation     │
                         │ Approval             │
                         │ QR Validation        │
                         │ Gate Entry / Exit    │
                         └──────────┬───────────┘
                                    │
                                    ▼
                              PostgreSQL
                                    ▲
                                    │
                         ┌──────────┴───────────┐
                         │   CUTOFF SERVICE     │
                         │                      │
                         │ 9 PM Processing      │
                         │ Batch Processing     │
                         │ Notifications        │
                         │ Reminders            │
                         │ Escalation           │
                         │ QR Cleanup           │
                         └──────────────────────┘
```

---

# 🧠 Outpass Core Service

The primary backend handles real-time operations.

Responsibilities:

- Authentication
- JWT validation
- Role-Based Access Control
- Student management
- Approver management
- Outpass creation
- Approval and rejection
- QR generation
- QR validation
- Gate exit
- Gate entry
- Real-time state transitions

This service must prioritize low-latency operations at the gate.

---

# ⏱️ Cutoff Service

The cutoff service handles scheduled and background workloads.

Responsibilities:

- Run scheduled processing
- Fetch relevant market outpasses
- Process outpasses in batches
- Apply cutoff-related state changes
- Send notifications
- Send reminders
- Handle escalation states
- Delete obsolete QR artifacts from cloud storage

Separating this workload ensures that heavy background processing does not interfere with real-time gate validation.

---

# 🔐 Authentication and Authorization

The system will use:

- College email verification
- Roll number-based student identification
- Password-based authentication
- JWT-based session authentication
- Role-Based Access Control

Potential roles include:

```text
STUDENT

APPROVER

GATE_GUARD

ADMIN
```

Approver position information may additionally include:

```text
WARDEN

VICE_WARDEN

ASSISTANT_WARDEN

MMCA_OFFICER

AUTHORIZED_GUARD
```

---

# 🗃️ Student Data

The student profile may contain:

```text
Student ID
Name
Roll Number
College Email
Phone Number
Branch
Hostel
Room Number
Photo
Password Hash
Account Status
Created At
Updated At
```

Additional information can be added later depending on administrative requirements.

---

# 👨‍💼 Approver Data

Approver information may include:

```text
Approver ID
Name
Phone Number
Email
Role
Position
Authorized Hostel
Account Status
Created At
Updated At
```

A hostel may have multiple approvers.

---

# 📊 Outpass History

The system preserves important lifecycle information.

Important timestamps include:

```text
created_at

approved_at

rejected_at

exited_at

returned_at

expired_at
```

These timestamps allow the system to determine:

```text
Did the student leave?
When did the student leave?
Did the student return?
When did the student return?
Was the student late?
When was the pass closed?
```

Historical data should not depend on the QR image remaining in cloud storage.

---

# ☁️ QR Storage

QR artifacts may be stored using cloud object storage.

Planned option:

```text
Amazon S3
```

The QR image itself is not permanent historical data.

The important information remains in PostgreSQL.

At the appropriate cutoff processing time, obsolete QR artifacts can be deleted from object storage.

---

# 🛠️ Planned Tech Stack

## Backend

```text
Java 17
Spring Boot
Spring Security
Spring Data JPA
Hibernate
JWT
Maven
```

## Database

```text
PostgreSQL
```

## Cloud

```text
AWS
Amazon S3
```

## Architecture

```text
REST APIs
Role-Based Access Control
JWT Authentication
Scheduled Jobs
Batch Processing
Microservice-based Background Processing
```

---

# 📁 Planned Repository Structure

```text
PassNikaal/
│
├── outpass-core/              # Main real-time backend
│
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── ...
│   │       │
│   │       └── resources/
│   │
│   └── pom.xml
│
├── cutoff-service/            # Background processing service
│
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── ...
│   │       │
│   │       └── resources/
│   │
│   └── pom.xml
│
├── docs/                      # Architecture and documentation
│
└── README.md
```

---

# 🚀 Development Roadmap

The project will be developed incrementally.

## Phase 1 — Foundation

- [ ] Initialize repository
- [ ] Define project structure
- [ ] Set up PostgreSQL
- [ ] Configure environment variables
- [ ] Configure Spring Boot applications

## Phase 2 — Database Design

- [ ] Student schema
- [ ] Approver schema
- [ ] Hostel schema
- [ ] Outpass schema
- [ ] Role and authorization schema
- [ ] Notification schema

## Phase 3 — Authentication

- [ ] Student registration
- [ ] College email verification
- [ ] Login
- [ ] Password hashing
- [ ] JWT authentication
- [ ] Role-Based Access Control

## Phase 4 — Outpass Flow

- [ ] Create outpass
- [ ] Check active outpass constraint
- [ ] Approve outpass
- [ ] Reject outpass
- [ ] Approve selected requests
- [ ] Reject selected requests
- [ ] Approve all pending requests
- [ ] Reject all pending requests

## Phase 5 — Gate Flow

- [ ] Generate QR
- [ ] Validate QR
- [ ] Roll number fallback
- [ ] Exit validation
- [ ] Entry validation
- [ ] State transitions

## Phase 6 — Background Processing

- [ ] 9 PM scheduler
- [ ] Batch processing
- [ ] Expiry workflow
- [ ] NOT_RETURNED workflow
- [ ] QR cleanup
- [ ] Notification processing

## Phase 7 — Notifications

- [ ] Hostel confirmation notification
- [ ] Reminder system
- [ ] Escalation workflow

## Phase 8 — Deployment

- [ ] Dockerize services
- [ ] Deploy backend
- [ ] Deploy PostgreSQL
- [ ] Configure cloud storage
- [ ] Configure environment variables
- [ ] Logging and monitoring

---

# 🎯 Project Goals

PassNikaal aims to provide:

- Reduced paper consumption
- Faster outpass processing
- Reduced redundant data entry
- Real-time outpass validation
- Prevention of old outpass reuse
- Accurate entry and exit tracking
- Support for multiple gates
- Scalable backend architecture
- Separation of real-time and background workloads

---

# ⚠️ MVP Scope

The initial version focuses on solving the core workflow.

The MVP does not attempt to solve every possible administrative or human error.

For example:

- A guard can still make a manual mistake.
- Students may require human interaction for unresolved cases.
- Offline fallback may still involve a manual paper register.
- Advanced identity verification is outside the initial MVP scope.

The goal is not to create a perfect surveillance system.

The goal is to replace an inefficient paper-based workflow with a faster, more secure, and digitally verifiable system.

---

## 📜 Project Status

🚧 **Currently under development**

The project is being designed and built incrementally, starting with the backend architecture, database design, authentication, and core outpass lifecycle.

---

Built to make the process of **taking an outpass less painful than actually going out.** 😛
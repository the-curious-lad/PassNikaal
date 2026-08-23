# PassNikaal - Development Stages

> Version 1.0 | Date: 2026-08-23
>
> This document divides the project into small, independently reviewable stages.
> No stage should generate so many files that reviewing them becomes impractical.
> Each stage should be understood, tested, and confirmed before the next one begins.

---

## How to Use This Document

Each stage has:
- **Goal**: What problem we are solving in this stage.
- **Why**: Why this stage exists and why it comes in this order.
- **What will be built**: The concrete deliverables.
- **Files to be created/modified**: So you can review each file after generation.
- **Database changes**: Any new tables, indexes, or constraints.
- **APIs introduced**: The new endpoints this stage produces.
- **Tests required**: What tests to write.
- **Manual verification steps**: How to confirm the stage works.
- **Dependencies**: Which previous stages must be complete first.
- **Definition of Done**: The criteria that declare this stage complete.

---

## Stage 1 - Project Setup

### Goal
Initialize both Spring Boot projects with the correct structure, dependencies, and configuration. After this stage, both services should start successfully.

### Why
A clean foundation prevents accumulation of technical debt. Getting the project structure, Maven configuration, and application properties right at the start avoids painful refactoring later.

### What will be built
- outpass-core: Spring Boot project with Maven, correct package structure, and all required dependencies.
- cutoff-service: Spring Boot project with Maven, correct package structure, and all required dependencies.
- Base application.properties with placeholders for all environment variables.
- .gitignore for both projects.

### Files to be created

outpass-core:
```
outpass-core/pom.xml
outpass-core/src/main/java/com/passnikaal/core/OutpassCoreApplication.java
outpass-core/src/main/resources/application.properties
outpass-core/src/main/resources/application-dev.properties
```

cutoff-service:
```
cutoff-service/pom.xml
cutoff-service/src/main/java/com/passnikaal/cutoff/CutoffServiceApplication.java
cutoff-service/src/main/resources/application.properties
cutoff-service/src/main/resources/application-dev.properties
```

### Dependencies to include

outpass-core:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-boot-starter-mail
- postgresql driver
- jjwt (JWT library)
- zxing (QR code generation)
- aws-sdk for S3
- lombok (optional)

cutoff-service:
- spring-boot-starter-data-jpa
- spring-boot-starter-mail
- postgresql driver
- aws-sdk for S3
- lombok (optional)

### Database changes
None in this stage.

### APIs introduced
None. A health check endpoint is acceptable:
```
GET /api/v1/health -> 200 OK
```

### Tests required
- Both applications start without error.
- Health check endpoint returns 200.

### Manual verification steps
1. Run mvn spring-boot:run in outpass-core.
2. Confirm the application starts without exceptions.
3. Visit http://localhost:8080/api/v1/health and confirm 200 OK.
4. Repeat for cutoff-service on port 8081.

### Dependencies on previous stages
None. This is the first stage.

### Definition of Done
- Both services start cleanly.
- No compilation errors.
- No hardcoded secrets in any file.
- Package structure matches the module plan in architecture.md.

---

## Stage 2 - Database Entities

### Goal
Define all JPA entities, create the PostgreSQL schema, and verify that Hibernate can create the tables.

### Why
All subsequent stages depend on the database schema. Getting the entity relationships, column types, and constraints right at this stage prevents costly migrations later. The partial unique index for the one-active-outpass rule must be created here.

### What will be built
- JPA entity classes for all tables.
- Enums for status, role, outpass type, hostel confirmation status.
- Hibernate DDL for initial schema creation.
- JPA repositories (empty, just the interface declarations).

### Files to be created

outpass-core entities:
```
src/main/java/com/passnikaal/core/common/enums/Role.java
src/main/java/com/passnikaal/core/common/enums/AccountStatus.java
src/main/java/com/passnikaal/core/common/enums/OutpassStatus.java
src/main/java/com/passnikaal/core/common/enums/OutpassType.java
src/main/java/com/passnikaal/core/common/enums/ApproverDesignation.java
src/main/java/com/passnikaal/core/common/enums/HostelConfirmationStatus.java
src/main/java/com/passnikaal/core/auth/entity/User.java
src/main/java/com/passnikaal/core/auth/entity/EmailVerificationToken.java
src/main/java/com/passnikaal/core/auth/entity/RefreshToken.java
src/main/java/com/passnikaal/core/student/entity/Student.java
src/main/java/com/passnikaal/core/student/entity/Hostel.java
src/main/java/com/passnikaal/core/approver/entity/Approver.java
src/main/java/com/passnikaal/core/approver/entity/ApproverHostelAssignment.java
src/main/java/com/passnikaal/core/outpass/entity/Outpass.java
src/main/java/com/passnikaal/core/notification/entity/Notification.java
```

outpass-core repositories (empty declarations only):
```
src/main/java/com/passnikaal/core/auth/repository/UserRepository.java
src/main/java/com/passnikaal/core/auth/repository/EmailVerificationTokenRepository.java
src/main/java/com/passnikaal/core/auth/repository/RefreshTokenRepository.java
src/main/java/com/passnikaal/core/student/repository/StudentRepository.java
src/main/java/com/passnikaal/core/student/repository/HostelRepository.java
src/main/java/com/passnikaal/core/approver/repository/ApproverRepository.java
src/main/java/com/passnikaal/core/approver/repository/ApproverHostelAssignmentRepository.java
src/main/java/com/passnikaal/core/outpass/repository/OutpassRepository.java
src/main/java/com/passnikaal/core/notification/repository/NotificationRepository.java
```

cutoff-service entities (mirrored subset):
```
src/main/java/com/passnikaal/cutoff/entity/Outpass.java
src/main/java/com/passnikaal/cutoff/entity/Student.java
src/main/java/com/passnikaal/cutoff/entity/Notification.java
src/main/java/com/passnikaal/cutoff/repository/OutpassRepository.java
src/main/java/com/passnikaal/cutoff/repository/NotificationRepository.java
```

### Database changes

All tables as defined in architecture.md section 17:
- users
- students
- hostels
- approvers
- approver_hostel_assignments
- outpasses
- email_verification_tokens
- **refresh_tokens** (new — required by D-02 decision: access token + refresh token strategy)
- notifications

All indexes as defined in architecture.md section 18, including:
- The partial unique index for one-active-outpass.
- `idx_refresh_tokens_user_id` on refresh_tokens.

### APIs introduced
None.

### Tests required
- Application starts and Hibernate creates tables without error.
- Verify all 9 tables exist in PostgreSQL with correct columns.
- Verify the partial unique index exists.
- Verify the refresh_tokens table has the token_hash unique constraint.

### Manual verification steps
1. Connect to PostgreSQL: psql -U postgres -d passnikaal
2. Run \dt to list tables. Confirm all 9 tables exist including refresh_tokens.
3. Run \d outpasses to confirm all columns and constraints.
4. Run \d refresh_tokens to confirm token_hash unique constraint.
5. Run \di to confirm all indexes exist including one_active_outpass_per_student.

### Dependencies on previous stages
Stage 1 must be complete.

### Definition of Done
- All entities compile without errors.
- All tables exist in PostgreSQL with correct schema.
- All indexes created including the partial unique index.
- No TODO or placeholder code in entity files.

---

## Stage 3 - Authentication: Registration and Email Verification

### Goal
Implement student registration, password hashing, email verification token generation, and account activation via email link.

### Why
Authentication is the foundation of every subsequent feature. Every other endpoint depends on knowing who the user is and what role they have.

### What will be built
- Registration endpoint.
- Email verification token creation.
- Email sending.
- Account activation endpoint.
- Resend verification endpoint.
- Basic exception classes and global exception handler.

### Files to be created

```
src/main/java/com/passnikaal/core/auth/dto/RegisterRequest.java
src/main/java/com/passnikaal/core/auth/dto/RegisterResponse.java
src/main/java/com/passnikaal/core/auth/service/AuthService.java
src/main/java/com/passnikaal/core/auth/controller/AuthController.java
src/main/java/com/passnikaal/core/auth/service/EmailService.java
src/main/java/com/passnikaal/core/exception/GlobalExceptionHandler.java
src/main/java/com/passnikaal/core/exception/DuplicateResourceException.java
src/main/java/com/passnikaal/core/exception/ResourceNotFoundException.java
src/main/java/com/passnikaal/core/exception/AccountNotVerifiedException.java
src/main/java/com/passnikaal/core/common/dto/ErrorResponse.java
```

### APIs introduced

```
POST   /api/v1/auth/register
GET    /api/v1/auth/verify-email?token=...
POST   /api/v1/auth/resend-verification
```

### Database changes
None new. Uses users and email_verification_tokens tables from Stage 2.

### Tests required
- Register with valid data -> 201 Created, user in DB with PENDING_VERIFICATION status.
- Register with existing email -> 409 Conflict.
- Register with existing roll number -> 409 Conflict.
- Verify with valid token -> account ACTIVE.
- Verify with expired token -> 400 Bad Request.
- Verify with already-used token -> 400 Bad Request.

### Manual verification steps
1. POST /api/v1/auth/register with valid student data.
2. Confirm user appears in DB with account_status = PENDING_VERIFICATION.
3. Check email inbox (or email logs in dev) for verification link.
4. Visit the verification link.
5. Confirm account_status = ACTIVE in DB.

### Dependencies on previous stages
Stage 2 must be complete.

### Definition of Done
- Registration creates user and sends verification email.
- Account activation works correctly.
- Expired and used tokens are rejected.
- All error responses use the standard ErrorResponse format.

---

## Stage 4 - Authentication: Login, JWT, and Refresh Token

### Goal
Implement login with JWT access token + refresh token generation, Spring Security configuration, the JWT filter that authenticates every request, the token refresh endpoint, and the logout endpoint.

### Why
Without a working JWT mechanism, no protected endpoint can be built. This stage produces the security infrastructure that all other stages rely on. The refresh token is implemented here because it is part of the core auth flow — not a later addition — and is simpler to build correctly from the start than to bolt on later.

### What will be built
- Login endpoint: validates credentials, generates a short-lived access token and a 7-day refresh token.
- Refresh endpoint: accepts a refresh token, returns a new access token.
- Logout endpoint: revokes the refresh token from the database.
- JwtService: generates and validates both token types.
- JwtAuthenticationFilter: extracts and validates the access token on every request.
- Spring Security configuration (SecurityFilterChain).
- UserDetailsService implementation.
- RBAC access rules (all endpoint permissions defined in one place).

### Files to be created

```
src/main/java/com/passnikaal/core/auth/dto/LoginRequest.java
src/main/java/com/passnikaal/core/auth/dto/LoginResponse.java          <- contains accessToken only
src/main/java/com/passnikaal/core/auth/dto/RefreshRequest.java
src/main/java/com/passnikaal/core/auth/dto/RefreshResponse.java         <- contains new accessToken only
src/main/java/com/passnikaal/core/auth/service/JwtService.java
src/main/java/com/passnikaal/core/auth/service/RefreshTokenService.java
src/main/java/com/passnikaal/core/auth/filter/JwtAuthenticationFilter.java
src/main/java/com/passnikaal/core/auth/service/CustomUserDetailsService.java
src/main/java/com/passnikaal/core/config/SecurityConfig.java
```

### Token flow summary

```
POST /auth/login
    -> Validate credentials
    -> Generate access token (15-30 min, signed JWT)
    -> Generate raw refresh token (SecureRandom, 256-bit)
    -> Hash refresh token and store in refresh_tokens table
    -> Return {accessToken, refreshToken} to client

Client stores:
    accessToken   -> memory / Authorization header
    refreshToken  -> HttpOnly cookie (recommended) or secure storage

On every request:
    -> JwtAuthenticationFilter extracts accessToken from Authorization header
    -> Validates signature and expiry
    -> Sets SecurityContext

When accessToken expires (client gets 401):
    -> Client calls POST /auth/refresh with the refreshToken
    -> Backend hashes incoming refreshToken, looks up in DB
    -> Validates not revoked, not expired
    -> Issues new accessToken
    -> Returns {accessToken}

POST /auth/logout
    -> Mark refresh token as revoked in DB (or delete it)
```

### APIs introduced

```
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
```

### Database changes
Uses the refresh_tokens table created in Stage 2. No new tables.

### Tests required
- Login with correct credentials -> 200 OK with both accessToken and refreshToken.
- Login with wrong password -> 401 Unauthorized.
- Login with unverified account -> 403 Forbidden.
- Request to protected endpoint without access token -> 401.
- Request to protected endpoint with valid access token -> passes through.
- Request to STUDENT endpoint with APPROVER token -> 403.
- POST /auth/refresh with valid refresh token -> returns new access token.
- POST /auth/refresh with expired refresh token -> 401.
- POST /auth/refresh with revoked refresh token -> 401.
- POST /auth/logout -> refresh token is deleted/revoked in DB.
- After logout, POST /auth/refresh with old refresh token -> 401.

### Manual verification steps
1. POST /api/v1/auth/login with valid credentials.
2. Confirm response contains accessToken and refreshToken.
3. Paste accessToken into jwt.io and confirm payload shows correct userId and role.
4. Wait for access token to expire (or shorten expiry in dev config for testing).
5. POST /api/v1/auth/refresh with refreshToken -> confirm new accessToken returned.
6. POST /api/v1/auth/logout -> confirm refresh_tokens row is deleted/revoked in DB.
7. POST /api/v1/auth/refresh again with old token -> confirm 401.

### Dependencies on previous stages
Stage 3 must be complete.

### Definition of Done
- Login returns both access token (short-lived) and refresh token (7-day).
- Refresh token is stored as a hash in the database, not as plaintext.
- Refresh endpoint issues a new access token without requiring re-login.
- Logout endpoint revokes the refresh token from the database.
- Expired or revoked refresh tokens are rejected with 401.
- JWT filter correctly identifies the user on every subsequent request.
- RBAC rules are defined in a single SecurityFilterChain, not scattered annotations.

---

## Stage 5 - Student Profile

### Goal
Implement the student profile endpoints. A logged-in student can view and update their own profile.

### Why
Student profile data (hostel, room number, branch) is used by the outpass creation logic in Stage 6. Establishing this endpoint also validates that the JWT filter correctly identifies the authenticated user.

### What will be built
- Get own profile endpoint.
- Update own profile endpoint (limited fields: phone number, room number).
- Student profile DTOs.

### Files to be created

```
src/main/java/com/passnikaal/core/student/dto/StudentProfileResponse.java
src/main/java/com/passnikaal/core/student/dto/UpdateStudentRequest.java
src/main/java/com/passnikaal/core/student/service/StudentService.java
src/main/java/com/passnikaal/core/student/controller/StudentController.java
```

### APIs introduced

```
GET    /api/v1/students/me
PUT    /api/v1/students/me
```

### Database changes
None.

### Tests required
- Authenticated student can view their own profile.
- Student cannot view another student's profile.
- Student can update phone number and room number.
- Student cannot change roll number or hostel through this endpoint.

### Manual verification steps
1. Login as a student to get a JWT.
2. GET /api/v1/students/me and confirm profile data matches what was registered.
3. PUT /api/v1/students/me with updated phone number.
4. GET /api/v1/students/me and confirm the phone number is updated.

### Dependencies on previous stages
Stage 4 must be complete.

### Definition of Done
- Student can view their own profile.
- Update endpoint only allows safe fields to be changed.
- Response DTO does not expose password hash or internal IDs unnecessarily.

---

## Stage 6 - Outpass Creation

### Goal
Implement outpass creation for both Market and Home types. Enforce the one-active-outpass rule at both the application and database level. Enforce the market outpass creation cutoff time.

### Why
This is the core student-facing action. All subsequent stages (approval, QR, gate) depend on outpasses existing in the database.

### What will be built
- Create outpass endpoint.
- Validation for Market vs Home specific fields.
- One-active-outpass check (service level + DB constraint enforcement).
- Market outpass creation cutoff time check.
- Get own outpass history endpoint.
- Outpass DTOs.
- DuplicateActiveOutpassException and OutpassCreationCutoffException.

### Files to be created

```
src/main/java/com/passnikaal/core/outpass/dto/CreateOutpassRequest.java
src/main/java/com/passnikaal/core/outpass/dto/OutpassResponse.java
src/main/java/com/passnikaal/core/outpass/dto/OutpassStatusResponse.java
src/main/java/com/passnikaal/core/outpass/service/OutpassService.java
src/main/java/com/passnikaal/core/outpass/controller/OutpassController.java
src/main/java/com/passnikaal/core/exception/DuplicateActiveOutpassException.java
src/main/java/com/passnikaal/core/exception/OutpassCreationCutoffException.java
src/main/java/com/passnikaal/core/exception/InvalidOutpassStateException.java
```

### APIs introduced

```
POST   /api/v1/outpasses
GET    /api/v1/outpasses/{id}
GET    /api/v1/outpasses/{id}/status
GET    /api/v1/outpasses/my
```

### Database changes
None new. The partial unique index from Stage 2 enforces the constraint.

### Tests required
- Student creates a market outpass successfully.
- Student creates a home outpass successfully.
- Student tries to create a second outpass while PENDING -> 409.
- Student tries to create a second outpass while APPROVED -> 409.
- Two simultaneous requests -> only one succeeds (concurrent test).
- Market outpass created after cutoff time -> 422.
- Home outpass created any time -> succeeds (no time restriction).
- Student cannot see another student's outpass.

### Manual verification steps
1. Login as student.
2. POST /api/v1/outpasses with Market type data.
3. Confirm outpass appears in DB with PENDING status.
4. Try POST /api/v1/outpasses again -> confirm 409 Conflict.
5. Confirm student_id in DB matches the authenticated student, not anything from the request body.

### Dependencies on previous stages
Stage 5 must be complete.

### Definition of Done
- Outpass is created with PENDING status.
- student_id comes from JWT, not the request body.
- One-active-outpass rule enforced at DB level.
- Market creation cutoff enforced.
- Correct validation errors for missing required fields.

---

## Stage 7 - Outpass Approval and Rejection

### Goal
Implement the approver workflow: viewing pending outpasses for their hostel, approving or rejecting individually, and bulk operations.

### Why
Approval is the gate between PENDING and APPROVED. Without it, no student can exit. This stage also validates that the hostel-level authorization check works correctly.

### What will be built
- Get pending outpasses for my hostel(s) endpoint.
- Approve one outpass.
- Reject one outpass.
- Approve selected outpasses.
- Reject selected outpasses.
- Approve all pending for my hostel.
- Reject all pending for my hostel.
- Hostel-level ownership check in service layer.
- Approver DTOs.

### Files to be created

```
src/main/java/com/passnikaal/core/approver/dto/PendingOutpassResponse.java
src/main/java/com/passnikaal/core/approver/dto/BulkActionRequest.java
src/main/java/com/passnikaal/core/approver/dto/BulkActionResponse.java
src/main/java/com/passnikaal/core/approver/dto/RejectRequest.java
src/main/java/com/passnikaal/core/approver/service/ApprovalService.java
src/main/java/com/passnikaal/core/approver/controller/ApproverController.java
```

### APIs introduced

```
GET    /api/v1/outpasses/pending
POST   /api/v1/outpasses/{id}/approve
POST   /api/v1/outpasses/{id}/reject
POST   /api/v1/outpasses/approve-selected
POST   /api/v1/outpasses/reject-selected
POST   /api/v1/outpasses/approve-all
POST   /api/v1/outpasses/reject-all
```

### Database changes
None new. approved_at, rejected_at, approved_by, rejection_reason columns already exist.

### Tests required
- Approver can see pending outpasses for their hostel.
- Approver cannot see pending outpasses for a different hostel.
- Approver approves an outpass -> status becomes APPROVED, approved_at is set.
- Approver rejects an outpass -> status becomes REJECTED, rejected_at and rejection_reason set.
- Approver tries to approve an already-APPROVED pass -> 409.
- Bulk approve works for multiple outpasses.
- Student cannot call approve endpoint -> 403.

### Manual verification steps
1. Create an outpass as a student (Stage 6).
2. Login as an approver assigned to the same hostel.
3. GET /api/v1/outpasses/pending -> confirm the outpass appears.
4. POST /api/v1/outpasses/{id}/approve -> confirm status = APPROVED in DB.
5. Confirm approved_at timestamp is set.
6. Confirm approved_by matches the approver's ID.

### Dependencies on previous stages
Stage 6 must be complete.

### Definition of Done
- Approver can only act on outpasses belonging to their assigned hostel.
- State transitions update correct timestamps.
- Bulk operations work correctly.
- Student role is blocked from approval endpoints.

---

## Stage 8 - QR Generation

### Goal
When an outpass is approved, generate a QR code, upload it to S3, and store the S3 object key. Implement the endpoint to retrieve the QR image.

### Why
The QR is what the gate guard scans. Without QR generation on approval, the gate workflow cannot proceed. This stage also validates the S3 integration.

### What will be built
- QR token generation (SecureRandom, 256 bits).
- QR image generation using ZXing.
- S3 upload of QR image.
- qr_token and qr_s3_key stored in outpass record.
- Get QR image endpoint (streams from S3).
- S3 configuration.
- InvalidQrTokenException.

### Files to be created

```
src/main/java/com/passnikaal/core/outpass/service/QrService.java
src/main/java/com/passnikaal/core/config/S3Config.java
src/main/java/com/passnikaal/core/exception/InvalidQrTokenException.java
```

### APIs introduced

```
GET    /api/v1/outpasses/{id}/qr
```

### Database changes
None new. qr_token and qr_s3_key columns already exist.

### Tests required
- Approving an outpass triggers QR generation.
- QR image exists in S3 after approval.
- qr_token and qr_s3_key are populated in the outpass record.
- GET /api/v1/outpasses/{id}/qr returns a valid PNG image.
- Student can only retrieve QR for their own outpass.
- Guard can retrieve QR for any outpass.

### Manual verification steps
1. Approve an outpass (Stage 7).
2. Confirm qr_token and qr_s3_key are set in DB.
3. GET /api/v1/outpasses/{id}/qr with student's JWT.
4. Confirm the response contains a valid QR image.
5. Scan the QR image with a phone to see the payload.
6. Confirm the S3 bucket contains the object at the expected key.

### Dependencies on previous stages
Stage 7 must be complete.

### Definition of Done
- QR is generated and stored in S3 on every approval.
- qr_token is unique and cryptographically random.
- QR image endpoint is accessible only by the owner student or gate guard.
- S3 bucket name and credentials come from environment variables.

---

## Stage 9 - Gate Exit

### Goal
Implement the gate exit flow. A guard scans the QR (or searches by roll number), the backend validates the token and current state, and transitions the outpass from APPROVED to EXITED.

### Why
This is the first half of the gate flow. The conditional UPDATE strategy for concurrency safety is introduced here.

### What will be built
- Gate exit endpoint (QR path).
- QR token lookup and validation.
- State validation: must be APPROVED.
- Conditional UPDATE: APPROVED to EXITED.
- exited_at timestamp set.
- Roll number fallback lookup endpoint.
- GateValidationService shared logic.

### Files to be created

```
src/main/java/com/passnikaal/core/gate/dto/GateActionRequest.java
src/main/java/com/passnikaal/core/gate/dto/GateActionResponse.java
src/main/java/com/passnikaal/core/gate/dto/RollNumberLookupResponse.java
src/main/java/com/passnikaal/core/gate/service/GateValidationService.java
src/main/java/com/passnikaal/core/gate/controller/GateController.java
```

### APIs introduced

```
POST   /api/v1/gate/exit
GET    /api/v1/gate/lookup?rollNumber=...
```

### Database changes
None new. exited_at column already exists.

### Tests required
- APPROVED outpass -> POST /gate/exit -> status becomes EXITED, exited_at set.
- PENDING outpass -> POST /gate/exit -> 409 Invalid state.
- Already EXITED outpass -> POST /gate/exit -> 409 Invalid state.
- Invalid QR token -> 403.
- Roll number lookup returns active outpass.
- Student cannot call gate/exit -> 403.

### Manual verification steps
1. Have an APPROVED outpass with a QR.
2. POST /api/v1/gate/exit with the qrToken and outpassId.
3. Confirm status = EXITED in DB.
4. Confirm exited_at is set.
5. Try calling gate/exit again -> confirm 409.

### Dependencies on previous stages
Stage 8 must be complete.

### Definition of Done
- APPROVED to EXITED transition works correctly.
- Conditional UPDATE is used (not a naive read-then-write).
- Invalid states are rejected with clear error messages.
- Roll number fallback reaches the same validation logic.

---

## Stage 10 - Gate Entry

### Goal
Implement the gate entry flow. The backend validates the QR and current state. If EXITED, transition to RETURNED. If NOT_RETURNED, transition to RETURNED_LATE.

### Why
This completes the gate workflow. The conditional UPDATE must handle the race condition between a student scanning in and the cutoff service marking them as NOT_RETURNED at exactly 9 PM.

### What will be built
- Gate entry endpoint.
- State validation: must be EXITED or NOT_RETURNED.
- Conditional UPDATE for EXITED to RETURNED.
- Conditional UPDATE for NOT_RETURNED to RETURNED_LATE.
- returned_at timestamp set.

### Files to be modified

```
src/main/java/com/passnikaal/core/gate/service/GateValidationService.java
src/main/java/com/passnikaal/core/gate/controller/GateController.java
```

### APIs introduced

```
POST   /api/v1/gate/entry
```

### Database changes
None new. returned_at column already exists.

### Tests required
- EXITED outpass -> POST /gate/entry -> status becomes RETURNED, returned_at set.
- NOT_RETURNED outpass -> POST /gate/entry -> status becomes RETURNED_LATE, returned_at set.
- APPROVED outpass (not yet exited) -> POST /gate/entry -> 409 Invalid state.
- RETURNED outpass -> POST /gate/entry -> 409 Cannot enter twice.
- Concurrent gate/entry and cutoff NOT_RETURNED transition -> only one wins, no silent overwrite.

### Manual verification steps
1. Have an EXITED outpass.
2. POST /api/v1/gate/entry with the qrToken and outpassId.
3. Confirm status = RETURNED in DB.
4. Confirm returned_at is set.

### Dependencies on previous stages
Stage 9 must be complete.

### Definition of Done
- Both EXITED to RETURNED and NOT_RETURNED to RETURNED_LATE transitions work.
- Conditional UPDATE prevents silent overwrites.
- Invalid state transitions are rejected.

---

## Stage 11 - Admin Endpoints

### Goal
Implement admin endpoints for creating and managing students, approvers, hostels, and hostel assignments.

### Why
Before the system can go live, an admin needs to set up hostels, create approver accounts, and optionally pre-populate student accounts. Without this stage, the system cannot be configured.

### What will be built
- Create and list hostels.
- Create student accounts (admin flow).
- Create approver accounts.
- Update student and approver profiles.
- Assign and remove hostel assignments for approvers.
- Admin DTOs.

### Files to be created

```
src/main/java/com/passnikaal/core/admin/dto/CreateStudentRequest.java
src/main/java/com/passnikaal/core/admin/dto/CreateApproverRequest.java
src/main/java/com/passnikaal/core/admin/dto/CreateHostelRequest.java
src/main/java/com/passnikaal/core/admin/dto/AssignHostelRequest.java
src/main/java/com/passnikaal/core/admin/service/AdminService.java
src/main/java/com/passnikaal/core/admin/controller/AdminController.java
```

### APIs introduced

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

### Database changes
None new.

### Tests required
- Admin can create a hostel.
- Admin can create a student and approver.
- Admin can assign a hostel to an approver.
- Admin can remove a hostel assignment.
- Assigning the same hostel twice -> 409.
- Non-admin cannot call admin endpoints -> 403.

### Dependencies on previous stages
Stage 4 must be complete (authentication and RBAC).

### Definition of Done
- Admin can fully configure the system via API.
- Hostel assignments are correctly persisted and removable.
- Non-admin roles are blocked.

---

## Stage 12 - Cutoff Service: 9 PM Batch Processing

### Goal
Implement the cutoff service scheduler and batch processing logic. At 9 PM, process all relevant market outpasses in configurable batches.

### Why
This is the core scheduled workload that must run in isolation from the real-time backend. Getting the state transitions, batch logic, and idempotency right here is critical.

### What will be built
- @Scheduled job at 9 PM.
- Batch fetching of APPROVED, EXITED, and RETURNED market outpasses.
- APPROVED to EXPIRED transition (never exited).
- EXITED to NOT_RETURNED transition (not returned in time).
- RETURNED to EXPIRED transition (cleanup, preserve returned_at).
- Set hostel_confirmation_status = PENDING for RETURNED passes.
- Configurable batch size.
- Idempotent processing (safe to re-run).

### Files to be created

```
cutoff-service/src/main/java/com/passnikaal/cutoff/job/CutoffJob.java
cutoff-service/src/main/java/com/passnikaal/cutoff/processor/OutpassProcessor.java
cutoff-service/src/main/java/com/passnikaal/cutoff/config/CutoffConfig.java
```

### APIs introduced
None (no HTTP endpoints - this is a scheduler).

### Database changes
None new. All columns already exist.

### Tests required
- APPROVED market outpass at 9 PM -> becomes EXPIRED.
- EXITED market outpass at 9 PM -> becomes NOT_RETURNED.
- RETURNED market outpass at 9 PM -> becomes EXPIRED, hostel_confirmation_status = PENDING.
- Home outpasses are not affected by the 9 PM job.
- Already EXPIRED or RETURNED_LATE outpasses are not re-processed.
- Batch size of 100 processes 250 outpasses in 3 batches correctly.

### Manual verification steps
1. Create and approve several market outpasses.
2. Exit some. Leave some APPROVED. Return some.
3. Manually trigger the job (temporarily change cron to a nearby time).
4. Confirm APPROVED -> EXPIRED, EXITED -> NOT_RETURNED, RETURNED -> EXPIRED with hostel confirmation PENDING.

### Dependencies on previous stages
Stage 10 must be complete. Cutoff service database configuration must be working.

### Definition of Done
- Scheduler fires at 9 PM.
- All three state cases are handled correctly.
- Home outpasses are not touched.
- Batch size is configurable.
- Processing is idempotent.

---

## Stage 13 - Notifications and Hostel Confirmation

### Goal
Implement the hostel confirmation notification workflow. At 9 PM, notify returned students. Schedule reminder jobs at 9:30, 10:00, and 10:30 PM. Implement the student confirmation endpoint. Implement the in-app notification inbox.

### Why
The hostel confirmation workflow is a distinct operational requirement. Without it, the system cannot confirm that a returned student has actually reached their hostel room.

### What will be built
- Email sending for hostel confirmation.
- In-app notification creation.
- Reminder jobs at 9:30, 10:00, 10:30 PM.
- Escalation after 3 reminders.
- Student confirm-hostel endpoint.
- Notification inbox endpoint.
- Mark notification as read endpoint.

### Files to be created

cutoff-service:
```
cutoff-service/src/main/java/com/passnikaal/cutoff/job/ReminderJob.java
cutoff-service/src/main/java/com/passnikaal/cutoff/notification/NotificationDispatcher.java
```

outpass-core:
```
src/main/java/com/passnikaal/core/notification/service/NotificationService.java
src/main/java/com/passnikaal/core/notification/controller/NotificationController.java
src/main/java/com/passnikaal/core/notification/dto/NotificationResponse.java
```

### APIs introduced

```
GET    /api/v1/notifications
POST   /api/v1/notifications/{id}/read
POST   /api/v1/outpasses/{id}/confirm-hostel
```

### Database changes
None new.

### Tests required
- Student in RETURNED state receives hostel confirmation notification at 9 PM.
- Student confirms hostel -> hostel_confirmation_status = CONFIRMED.
- No response after 30 min -> Reminder 1 sent, reminder_count = 1.
- After 3 reminders without response -> ESCALATED.
- Student can view notification inbox.
- Mark as read works.

### Dependencies on previous stages
Stage 12 must be complete.

### Definition of Done
- Hostel confirmation notification sent to all RETURNED students at 9 PM.
- Reminder jobs fire at correct times.
- Escalation occurs after 3 missed reminders.
- Student can respond via the confirm-hostel endpoint.
- Notification inbox shows unread notifications.

---

## Stage 14 - S3 QR Cleanup

### Goal
Implement the S3 cleanup logic in the cutoff service. At 9 PM, after state transitions are complete, delete obsolete QR objects from S3 and set qr_s3_key to NULL in the database.

### Why
This is a maintenance task. Leaving QR images in S3 indefinitely wastes storage. The cleanup must be idempotent.

### What will be built
- S3 cleanup logic in the cutoff service.
- Batch S3 object deletion.
- qr_s3_key nullification after successful deletion.
- Error handling: if S3 delete fails, log and continue.

### Files to be created

```
cutoff-service/src/main/java/com/passnikaal/cutoff/s3/S3CleanupService.java
cutoff-service/src/main/java/com/passnikaal/cutoff/config/S3Config.java
```

### APIs introduced
None.

### Database changes
None new.

### Tests required
- After 9 PM processing, qr_s3_key is NULL for EXPIRED outpasses.
- S3 objects are deleted for those outpasses.
- If S3 deletion fails for one object, others are still processed.
- Cleanup is idempotent: running again on already-cleaned records does nothing harmful.

### Manual verification steps
1. After 9 PM processing (Stage 12), check the S3 bucket.
2. Confirm QR objects for EXPIRED outpasses have been deleted.
3. Confirm qr_s3_key is NULL in the DB for those outpasses.
4. Confirm QR objects for still-active outpasses still exist in S3.

### Dependencies on previous stages
Stage 12 must be complete. Stage 8 must be complete (S3 upload).

### Definition of Done
- Obsolete QR objects deleted from S3 after 9 PM processing.
- qr_s3_key set to NULL after deletion.
- Failure to delete one S3 object does not stop processing of others.
- S3 credentials come from environment variables.

---

## Stage 15 - Integration Testing and Hardening

### Goal
Write integration tests covering the full outpass lifecycle. Verify the concurrency strategy works. Fix any bugs discovered during testing.

### Why
Unit tests verify individual components. Integration tests verify that the full lifecycle works end-to-end without regression.

### What will be built
- Integration test: full market outpass lifecycle (create -> approve -> exit -> entry -> returned).
- Integration test: full market outpass lifecycle with late return (exit -> NOT_RETURNED -> RETURNED_LATE).
- Integration test: one-active-outpass concurrent creation (two simultaneous requests, only one succeeds).
- Integration test: concurrent gate/entry and cutoff NOT_RETURNED (only one wins).
- Integration test: expired outpass cannot be exited.
- Integration test: admin creates hostel and approver, approver approves outpass.

### Files to be created

```
outpass-core/src/test/java/com/passnikaal/core/integration/OutpassLifecycleTest.java
outpass-core/src/test/java/com/passnikaal/core/integration/ConcurrencyTest.java
outpass-core/src/test/java/com/passnikaal/core/integration/AuthFlowTest.java
```

### Dependencies on previous stages
All previous stages must be complete.

### Definition of Done
- All integration tests pass.
- Concurrency tests confirm no silent overwrites.
- No known bugs in core lifecycle.

---

## Stage 16 - Deployment Preparation

### Goal
Prepare both services for deployment on AWS. Configure environment variables, create a production-ready application.properties, and document the deployment steps.

### Why
A system that only runs locally is not a finished system. Deployment preparation validates that the application works with real AWS services.

### What will be built
- Production application.properties (all values from environment variables).
- Deployment documentation.
- Review of all hardcoded values (none should exist).

### Files to be created

```
outpass-core/src/main/resources/application-prod.properties
cutoff-service/src/main/resources/application-prod.properties
docs/deployment.md
```

### Manual verification steps
1. Deploy outpass-core to an EC2 instance.
2. Set all environment variables.
3. Confirm the application connects to RDS.
4. Run through the full lifecycle using the deployed service.
5. Confirm S3 upload and download work.
6. Run the cutoff service and confirm it connects to the same RDS instance.

### Dependencies on previous stages
Stage 15 must be complete.

### Definition of Done
- Both services deploy successfully on EC2.
- All environment variables are documented.
- No secrets hardcoded anywhere.
- Full lifecycle works on the deployed system.

---

## Stage Summary

| Stage | Name | Primary Concern |
|---|---|---|
| 1 | Project Setup | Foundation |
| 2 | Database Entities | Schema |
| 3 | Registration and Email Verification | Auth Part 1 |
| 4 | Login and JWT | Auth Part 2 |
| 5 | Student Profile | Profile |
| 6 | Outpass Creation | Core Feature |
| 7 | Approval and Rejection | Approver Workflow |
| 8 | QR Generation | Gate Prerequisite |
| 9 | Gate Exit | Gate Part 1 |
| 10 | Gate Entry | Gate Part 2 |
| 11 | Admin Endpoints | System Configuration |
| 12 | Cutoff Batch Processing | Background Processing |
| 13 | Notifications and Hostel Confirmation | Notification Workflow |
| 14 | S3 QR Cleanup | Maintenance |
| 15 | Integration Testing | Quality |
| 16 | Deployment Preparation | Production |

---

*End of stages.md - Version 1.0*

# UserService Module - Comprehensive Knowledge Document

## Overview

The **UserService** group manages user authentication, account lifecycle, and profile management. It handles user registration, email verification, login/logout, password reset, token refresh, and profile updates with device tracking and avatar management via MinIO storage.

---

## File: Helper.java

**Purpose:** Utility component providing validation, token generation, email masking, and device matching logic for UserService operations.

### Key Classes/Functions

#### `Helper` (Component: "featureUserHelper")
- **createShortToken(UserDetails, long)** → `String`
  - Delegates to JwtService to generate JWT tokens with custom expiration
  
- **isEmailFormat(String)** → `boolean`
  - Validates email format using regex: `^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$`
  - Returns false if input is null
  
- **maskEmail(String)** → `String`
  - Masks email local part (before @) to show first 2 and last 3 characters with asterisks
  - Example: `john.doe@example.com` → `jo****oe@example.com`
  - Returns unmodified if email is null or lacks @ symbol
  
- **areDeviceInfoMatching(DeviceInfo, DeviceInfo)** → `boolean`
  - Compares two DeviceInfo objects by normalized deviceType and osName (trimmed, lowercase)
  - Returns false if either parameter is null

### Dependencies
- **JwtService** — token generation
- **DeviceInfo** (embedded model) — device comparison
- **Spring Security** — UserDetails interface
- **Lombok** — @RequiredArgsConstructor, @Component

### Design Patterns
- **Utility/Helper Pattern** — Encapsulates reusable validation and transformation logic
- **Delegation** — Defers JWT generation to JwtService

### Public API
- `createShortToken()` — exposed for token generation
- `isEmailFormat()` — exposed for email validation
- `maskEmail()` — exposed for sensitive data obfuscation
- `areDeviceInfoMatching()` — exposed for device verification

---

## File: UserService.java

**Purpose:** Core service implementing iUser interface; orchestrates user lifecycle including registration, authentication, email verification, password reset, token refresh, profile management, and logout with multi-device session tracking.

### Key Classes/Functions

#### `UserService` (Service, @Transactional)

**createUser(UserRegisterRequest)** → `Response<RegisterResponse>`
- Validates email format and password confirmation via Helper
- Checks for existing active user with same email
- Checks for existing username with different email
- Creates new User or reuses inactive one with matching name+email
- Generates 5-minute UUID-based verification code
- Publishes `VerificationEmailEvent`
- Returns masked email confirmation message

**loginUser(UserLoginRequest)** → `Response<AuthResponse>`
- Accepts username or email (auto-detected via Helper.isEmailFormat)
- Throws USER_NOT_FOUND if user doesn't exist
- If user status is INACTIVE: regenerates verification code, publishes event, returns 401 UNAUTHORIZED
- Validates password via PasswordEncoder
- Creates deviceId via DeviceInfoService
- Calls RefreshTokenService.handleLoginTokens() for token generation
- Returns AuthResponse with tokens, user details, and roles

**verifyEmail(String code)** → `Response<String>`
- Finds user by auth code
- Validates: code matches, expiry not exceeded, purpose is EMAIL_VERIFICATION
- Clears auth code fields and sets status to ACTIVE
- Logs successful verification

**resetPassword(String code, AccountVerificationRequest)** → `Response<String>`
- Finds user by auth code
- Validates: code matches, expiry not exceeded, purpose is CHANGE_PASSWORD
- Validates new and confirm passwords match
- Encodes new password and clears auth code
- Logs successful reset

**sendCodeResetPassword(String email)** → `Response<String>`
- Finds user by email
- Generates 6-digit OTP string via SECURE_RANDOM
- Sets code purpose to CHANGE_PASSWORD with 5-minute expiry
- Publishes `SendCodeResetPassword` event
- Returns masked email confirmation

**refreshToken(RefreshTokenRequest)** → `Response<AuthResponse>`
- Extracts username from refresh token via JwtService
- Validates token validity against user
- Retrieves Redis hash `user:refresh_tokens:{userId}` containing all device tokens
- Parses token data (ObjectMapper) and matches incoming refresh token to device
- Throws error if token not found or devices don't match
- Calls RefreshTokenService.refreshSessionTokens() with new deviceId
- Returns new AccessToken + RefreshToken and user details

**logoutUser(HttpServletRequest)**
- Extracts JWT from Authorization header (Bearer scheme)
- Extracts username via JwtService
- Finds user and calls RefreshTokenService.revokeAllUserTokens()
- Logs logout event; silently fails on exceptions

**getMyProfile()** → `Response<MyProfileResponse>` (read-only)
- Retrieves current username via SecurityUtil
- Finds user by email
- Returns MyProfileResponse with all profile fields

**updateMyProfile(UpdateProfileRequest)** → `Response<MyProfileResponse>`
- Retrieves current user via SecurityUtil + email lookup
- Selectively updates: fullName, phoneNumber, dateOfBirth, gender, avatarUrl (if non-null)
- Persists changes and logs update

**uploadAvatar(MultipartFile)** → `Response<MyProfileResponse>`
- Validates file not empty and content-type is image/*
- Validates extension: .jpg, .jpeg, .png, .gif only
- Retrieves current user via SecurityUtil
- Deletes old avatar from MinIO if exists
- Uploads new file via MinioService
- Updates user.avatarUrl and persists
- Logs upload; throws error on MinIO failures

### Dependencies

**Internal Services:**
- **UserRepository** — JPA queries by email, name, authCode
- **PasswordEncoder** — Spring Security password hashing
- **Helper** — validation & masking utilities
- **ApplicationEventPublisher** — publishes verification/reset events
- **DeviceInfoService** — device ID generation
- **RefreshTokenService** — token lifecycle & Redis management
- **UserDetailsService** — loads UserDetails for auth
- **ActiveLogService** — Kafka event logging
- **RedisService** — retrieves stored device tokens
- **JwtService** — token validation & username extraction
- **SecurityUtil** — retrieves current authenticated user
- **MinioService** — avatar file storage/deletion
- **UserMapper** — entity-to-DTO conversion (currently unused)
- **ObjectMapper** — parses Redis token data

**External Models:**
- **User** (entity) — core user aggregate with authCode embedded object
- **DeviceInfo** (embedded) — device type & OS tracking
- **ActiveStatus** (enum) — ACTIVE, INACTIVE, EMAIL_VERIFICATION, CHANGE_PASSWORD
- **RoleType** (enum) — USER role assignment

**DTOs:**
- **Request:** UserRegisterRequest, UserLoginRequest, RefreshTokenRequest, AccountVerificationRequest, UpdateProfileRequest
- **Response:** AuthResponse, RegisterResponse, MyProfileResponse, DeviceInfoResponse

### Data Flow

```
Registration:
  createUser() → validate email/password → check duplicates → create/reuse User 
    → set verification code (5min UUID) → save → publish VerificationEmailEvent

Login:
  loginUser() → detect email vs username → validate exists & active 
    → validate password → create deviceId → get UserDetails 
    → handleLoginTokens() [RefreshTokenService] → return tokens + profile

Email Verification:
  verifyEmail(code) → find user → validate code/expiry/purpose 
    → set status ACTIVE → clear authCode

Token Refresh:
  refreshToken() → extract username → validate token 
    → query Redis user:refresh_tokens:{userId} → find matching device 
    → refreshSessionTokens() [RefreshTokenService] → return new tokens

Logout:
  logoutUser() → extract JWT → revokeAllUserTokens() [RefreshTokenService] 
    → clear Redis device entries

Password Reset:
  sendCodeResetPassword() → generate OTP (6-digit) → set 5min expiry 
    → publish SendCodeResetPassword event
  resetPassword(code) → validate code/expiry → encode password → clear authCode
```

### Design Patterns

- **Service Layer Pattern** — encapsulates business logic, coordinates repositories & external services
- **Event-Driven Pattern** — publishes domain events (VerificationEmailEvent, SendCodeResetPassword) for async handlers
- **Strategy Pattern** — email vs username detection in loginUser() via Helper.isEmailFormat()
- **Template Method** — common validation steps (code validation, expiry check) reused in verifyEmail/resetPassword
- **Repository Pattern** — abstracted data access via UserRepository

### Configuration & Constants

| Constant | Value | Purpose |
|----------|-------|---------|
| `OTP_LENGTH` | 6 | Password reset OTP digit count |
| `OTP_UPPER_BOUND` | 10^6 (1,000,000) | Max value for random OTP |
| `OTP_FORMAT_PATTERN` | "%06d" | Left-zero-padded format |
| `@Value("${frontend.url}")` | Application property | Frontend URL for verification links (declared but unused) |

### Public API

**Implements iUser Interface:**
- `createUser(UserRegisterRequest)` — user registration
- `loginUser(UserLoginRequest)` — authentication
- `verifyEmail(String)` — email verification
- `resetPassword(String, AccountVerificationRequest)` — password change via code
- `sendCodeResetPassword(String)` — OTP generation
- `refreshToken(RefreshTokenRequest)` — token renewal
- `logoutUser(HttpServletRequest)` — session termination
- `getMyProfile()` — fetch current user profile
- `updateMyProfile(UpdateProfileRequest)` — update profile fields
- `uploadAvatar(MultipartFile)` — avatar file upload

---

## Cross-File Relationships

| Component | Used By | Purpose |
|-----------|---------|---------|
| Helper | UserService | Email validation, masking, device matching |
| Helper.isEmailFormat() | UserService.loginUser(), UserService.createUser() | Email vs username detection |
| Helper.maskEmail() | UserService.createUser(), UserService.loginUser() | Sensitive output obfuscation |
| RefreshTokenService | UserService.loginUser(), UserService.refreshToken(), UserService.logoutUser() | Multi-device token lifecycle |
| DeviceInfoService | UserService.loginUser(), UserService.refreshToken() | Device tracking |
| JwtService | Helper, UserService.refreshToken(), UserService.logoutUser() | Token generation & validation |
| MinioService | UserService.uploadAvatar() | Avatar file storage |
| RedisService | UserService.refreshToken() | Device token retrieval |

---

## Security Considerations

- **Password Encoding** — all passwords hashed via PasswordEncoder; never stored plain
- **Email Masking** — sensitive emails obfuscated in responses via Helper.maskEmail()
- **Token Expiry** — auth codes expire in 5 minutes; refresh tokens validated against user in JWT
- **Device Tracking** — multi-device sessions managed via Redis; tokens linked to deviceId
- **Authorization** — getMyProfile/updateMyProfile/uploadAvatar restricted via @Transactional + SecurityUtil.getCurrentUsername()
- **File Upload Validation** — avatar uploads restricted to image/* MIME type and whitelisted extensions (.jpg, .jpeg, .png, .gif)
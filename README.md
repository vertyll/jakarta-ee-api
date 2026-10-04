<p align="center">
    <img alt="" src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Jakarta_EE-F7931E?style=for-the-badge">
    <img alt="" src="https://img.shields.io/badge/Open_Liberty-5B7CC1?style=for-the-badge">
    <img alt="" src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
    <img alt="" src="https://img.shields.io/badge/Keycloak-00b8e3?style=for-the-badge&logo=keycloak&logoColor=4D4D4D">
    <img alt="" src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
</p>

## Project Assumptions

Jakarta EE template REST API.

## Technology Stack

### Back-end:

- Java.
- Jakarta EE (Open Liberty).
- Gradle Kotlin DSL.
- MongoDB.
- Redis (Liberty session cache with Redisson).
- Nimbus JOSE + JWT.
- JUnit.
- Mockito.
- Lombok.

### Authentication:

- **Identity provider**: Keycloak (realm `jakarta-ee-api`) owns every page that touches a credential: sign-up, sign-in,
  email verification, password reset, two-factor authentication and acceptance of the terms of use. The application
  never sees a password.
- **Pattern**: BFF. A browser signs in at `GET /api/auth/authorize` with the authorization code flow and PKCE; the
  server keeps the tokens in the HTTP session and the browser holds only the `JAKARTA_EE_API_SESSION` cookie
  (`HttpOnly`, `SameSite=Lax`, `Secure` outside local development).
- **Session store**: Redis, through Open Liberty's session cache with Redisson, so the server holds no state of its own.
- **JWT**: a JAX-RS filter takes the access token from `Authorization: Bearer` or from the session and verifies it with
  Nimbus JOSE + JWT (Keycloak's JWKS, issuer, expiry, audience `jakarta-ee-api`); `@RolesAllowed`, `@PermitAll` and
  `@DenyAll` decide access.
- **Token lifecycle**: access tokens live five minutes; every refresh returns a new refresh token and invalidates the
  old one, and concurrent requests of one session share a single refresh. Signing out revokes the refresh token at
  Keycloak.
- **Cross-site requests**: `SameSite=Lax` plus `Sec-Fetch-Site`, so a write or a logout sent from another site is
  refused.
- **Accounts**: mirrored into MongoDB at sign-in and on `GET /api/users/me`.

### Core back-end:

- Gradle multi-module build system.
- The application has an exception handling mechanism (RFC 9457 problem details).
- The application has a logging mechanism.
- The application has a dedicated configuration file.
- The application has RBAC (Role Based Access Control).
- And many other features that can be found in the application code.

### Other:

- Docker for development environment.
- PMD for static code analysis.
- SpotBugs for static code analysis.
- JSpecify for null-safety annotations.
- NullAway for null-safety checks.
- Error Prone for static code analysis.
- Spotless for code formatting.

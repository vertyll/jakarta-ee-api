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

- Keycloak (realm `jakarta-ee-api`) handles sign-up, sign-in, email verification, password reset, two-factor
  authentication and acceptance of the terms of use.
- A browser signs in at `GET /api/auth/authorize` with the authorization code flow and PKCE, and then holds only the
  `JAKARTA_EE_API_SESSION` cookie (`HttpOnly`, `SameSite=Lax`, `Secure` outside local development). The tokens stay in
  the HTTP session, which Liberty keeps in Redis.
- Requests are authenticated with a bearer token or the session; the token's signature, issuer, expiry and audience
  (`jakarta-ee-api`) are verified, and `@RolesAllowed`, `@PermitAll` and `@DenyAll` decide access. Refresh tokens
  rotate on every use.
- Locally, `docker-compose.local.yml` runs MongoDB, Redis, RedisInsight (`:5540`, connected to Redis), Keycloak on
  `:9000` (admin/admin) and maildev. The realm from `keycloak/realm-export.json` has `admin@jakarta-ee-api.local`
  (`ADMIN`) and `user@jakarta-ee-api.local`, both with the password `jakarta-ee-api-local`.
- The application runs on an embedded Open Liberty server: `./gradlew :modules:app:libertyRun` (`:8080`).

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

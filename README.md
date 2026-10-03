## Project Assumptions

Jakarta EE template REST API.

## Technology Stack

### Back-end:

- Java.
- Jakarta EE.
- Gradle Kotlin DSL.
- MongoDB.
- Keycloak (OpenID Connect), Nimbus JOSE + JWT.
- Redis for HTTP sessions (Liberty `sessionCache` with Redisson).
- JUnit.
- Mockito.
- Lombok.

### Authentication:

- Keycloak (realm `jakarta-ee-api`) handles sign-up, sign-in, email verification, password reset, two-factor
  authentication and acceptance of the terms of use on its own pages.
- A browser signs in at `GET /api/auth/authorize` (`?register=true` opens sign-up): the authorization code flow with
  PKCE, after which the browser holds only the `JAKARTA_EE_API_SESSION` cookie (`HttpOnly`, `SameSite=Lax`, `Secure`
  outside local development). The tokens stay in the HTTP session, which Liberty keeps in Redis, so the server holds no
  state of its own. `GET /api/auth/session` says who is signed in, `POST /api/auth/logout` ends the session here and at
  Keycloak.
- `KeycloakAuthenticationFilter` takes the access token from `Authorization: Bearer` or from the session (refreshing it
  when it is about to expire; refresh tokens rotate and concurrent requests share one refresh; a cross-site write gets
  no token and a cross-site logout is refused), verifies its signature against Keycloak's keys, the issuer, the expiry and the audience
  (`jakarta-ee-api`), and sets the JAX-RS `SecurityContext`. `RoleAuthorizationFilter` enforces the standard
  `@RolesAllowed`, `@PermitAll` and `@DenyAll`; a resource without one of them requires signing in.
- The account is mirrored into MongoDB (`users`) at sign-in and on `GET /api/users/me`.
- The terms and the privacy policy are served at `/legal/terms.html` and `/legal/privacy.html`.

### Core back-end:

- Gradle multi-module build system.
- The application has an exception handling mechanism.
- The application has a logging mechanism.

### Other:

- PMD for static code analysis.
- SpotBugs for static code analysis.
- JSpecify for null-safety annotations.
- NullAway for null-safety checks.
- Error Prone for static code analysis.
- Spotless for code formatting.

## Running

The application runs on an embedded Open Liberty server, started straight from Gradle, without a separate
application-server installation. Its local configuration (MongoDB, Redis, Keycloak, mail) is in
`modules/app/src/main/liberty/config/server.env`; every value is an environment variable, so a deployment overrides it.

```bash
docker compose -f docker-compose.local.yml up -d   # MongoDB :27017, Redis :6379, Keycloak :9000, maildev :1025/:1080
./gradlew :modules:app:libertyRun                  # http://localhost:8080
```

The local realm (`keycloak/realm-export.json`) has `admin@jakarta-ee-api.local` (`ADMIN`) and
`user@jakarta-ee-api.local`, both with the password `jakarta-ee-api-local`.

- `./gradlew :modules:app:libertyDev` starts dev mode with hot reload.
- `./gradlew :modules:app:libertyPackage` builds a runnable jar, started with `java -jar modules/app/build/libs/jakarta-ee-api.jar`.
- `docker build -t jakarta-ee-api .` builds the image of the runnable jar.

The health endpoint is `GET /api/health`. The server configuration is in `modules/app/src/main/liberty/config/server.xml`.

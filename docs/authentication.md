# Authentication

- **Identity provider**: Keycloak (realm `jakarta-ee-api`) owns every page that touches a credential: sign-up, sign-in,
  email verification, password reset, two-factor authentication and acceptance of the terms of use. The application
  never sees a password.
- **Pattern**: BFF. A browser signs in at `GET /api/auth/authorize` with the authorization code flow and PKCE; the
  server keeps the tokens in the HTTP session and the browser holds only the `JAKARTA_EE_API_SESSION` cookie
  (`HttpOnly`, `SameSite=Lax`, `Secure` outside local development).
- **Session store**: Redis, through Open Liberty's session cache with Redisson (`jakarta-ee-api:session` namespace).
- **JWT**: a JAX-RS filter takes the access token from `Authorization: Bearer` or from the session and verifies it with
  Nimbus JOSE + JWT (Keycloak's JWKS, issuer, expiry, audience `jakarta-ee-api`); `@RolesAllowed`, `@PermitAll` and
  `@DenyAll` decide access.
- **State**: the back-end is stateless: every request is authorized by the JWT alone, so any instance can serve it. The
  only state is the browser session, and it lives in Redis, outside the application.
- **Token lifecycle**: access tokens live five minutes; every refresh returns a new refresh token and invalidates the
  old one, and concurrent requests of one session share a single refresh, across replicas too (a lock in Redis). Signing
  out revokes the refresh token at Keycloak.
- **Cross-site requests**: `SameSite=Lax` plus `Sec-Fetch-Site`, so a write or a logout sent from another site is
  refused.
- **Accounts**: mirrored into MongoDB at sign-in and on `GET /api/users/me`.

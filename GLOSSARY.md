# Glossary

Every term the documentation uses without defining it on the spot, and where it is explained. The specifications behind
them are in [STANDARDS.md](STANDARDS.md).

| Term | Meaning | Explained in |
|---|---|---|
| Access token | Short-lived JWT (five minutes) that authorizes one request. | [Authentication: Every request is authorized by a token](docs/authentication.md#every-request-is-authorized-by-a-token) |
| Audience | The `aud` claim naming whom a token is for; a token for anyone else is refused. | [Authentication: Every request is authorized by a token](docs/authentication.md#every-request-is-authorized-by-a-token) |
| Authorization code flow | Sign-in by redirecting to Keycloak and exchanging the code it returns, on the server. | [Authentication: Signing in](docs/authentication.md#signing-in) |
| BFF (backend for frontend) | The API signs the user in and keeps the tokens; the browser holds only a session cookie. | [Authentication](docs/authentication.md) |
| Business exception | A `BaseBusinessException` subclass carrying a message key, its arguments and a status. | [Architecture: Errors](docs/architecture.md#errors) |
| Dev mode | Open Liberty's `libertyDev`: build, start and redeploy on every change. | [Development Setup: Run the application](docs/development-setup.md#run-the-application) |
| Keycloak realm | The Keycloak tenant holding this application's users, roles and clients. | [Authentication](docs/authentication.md) |
| Message key | A key sent instead of a sentence; the client maps it to text. | [Architecture: Errors](docs/architecture.md#errors) |
| Mirrored account | The local copy of a Keycloak user in MongoDB, rewritten at every sign-in and on `/users/me`. | [Architecture: Accounts mirror Keycloak](docs/architecture.md#accounts-mirror-keycloak) |
| PKCE | A one-time secret binding the returned code to the browser that started the sign-in. | [Authentication: Signing in](docs/authentication.md#signing-in) |
| Problem document | The JSON body of every refusal, carrying a message key and its arguments. | [Architecture: Errors](docs/architecture.md#errors) |
| Refresh token | Long-lived token traded for a new access token; Keycloak rotates it on every use. | [Authentication: Sessions and refreshing](docs/authentication.md#sessions-and-refreshing) |
| Refresh token rotation | Every refresh invalidates the refresh token it used; replaying a spent one ends the session. | [Authentication: Sessions and refreshing](docs/authentication.md#sessions-and-refreshing) |
| Role | `USER` or `ADMIN`, a Keycloak realm role read from the access token. | [Authentication: Every request is authorized by a token](docs/authentication.md#every-request-is-authorized-by-a-token) |
| `Sec-Fetch-Site` | Header saying where a request came from; an unsafe cross-site request gets no token. | [Authentication: Cross-site requests](docs/authentication.md#cross-site-requests) |
| Session | `JAKARTA_EE_API_SESSION`, Liberty's HTTP session kept in Redis through Redisson. | [Authentication: Sessions and refreshing](docs/authentication.md#sessions-and-refreshing) |
| `session-store` | The module Liberty loads beside Redisson to prefix session keys. | [Architecture: Modules](docs/architecture.md#modules) |
| Sign-out | Ends the local session and the Keycloak session. | [Authentication: Signing out](docs/authentication.md#signing-out) |
| Single-flight refresh | One refresh per refresh token, shared by every request of the session that needs it at once. | [Authentication: Sessions and refreshing](docs/authentication.md#sessions-and-refreshing) |

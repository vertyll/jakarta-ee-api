# Development Setup

## Prerequisites

- Docker or Podman, with Compose
- JDK 25

## Start the infrastructure

```bash
docker compose -f docker-compose.local.yml up -d
```

Every `docker compose` command here works verbatim as `podman compose`.

| Service      | Address                                     | Purpose                                         |
|--------------|---------------------------------------------|-------------------------------------------------|
| MongoDB      | `localhost:27017`                           | the `jakarta_ee_api` database                   |
| Redis        | `localhost:6379`                            | sessions and the shared refresh lock            |
| Keycloak     | `http://localhost:9000` (`admin` / `admin`) | realm `jakarta-ee-api`, imported on start       |
| MailDev      | `http://localhost:1080`                     | catches Keycloak's verification and reset mails |
| RedisInsight | `http://localhost:5540`                     | browsing the sessions in Redis                  |

Keycloak imports `keycloak/realm-export.json` on its first start, with two accounts:

| Account                      | Password               | Roles           |
|------------------------------|------------------------|-----------------|
| `admin@jakarta-ee-api.local` | `jakarta-ee-api-local` | `USER`, `ADMIN` |
| `user@jakarta-ee-api.local`  | `jakarta-ee-api-local` | `USER`          |

> [!NOTE]
>
> The realm lives in the `keycloak-data` volume afterwards, so a change to the export file only takes effect after
> `docker compose -f docker-compose.local.yml down -v`.

## Run the application

```bash
./gradlew :modules:app:libertyDev
```

Open Liberty's dev mode builds the application, starts the server and redeploys on every change. The local settings
live in `modules/app/src/main/liberty/local/server.env` and already point at the containers above, so there is nothing
to configure. They are outside `src/main/liberty/config`, which every server is built from: only a server started by
`libertyDev`, `libertyRun` or `libertyStart` reads them, and `libertyPackage` leaves them out of the runnable jar.
The Redisson jars the session cache needs are copied into the server by the `copySessionStore` task, which every
Liberty task runs first.

The API listens on `http://localhost:8080/api`. Open `/api/auth/authorize` in a browser to sign in; the session
cookie then authorizes every call from that browser.

## Checks

```bash
./gradlew spotlessApply   # format
./gradlew check -x test   # Spotless, PMD, SpotBugs, Error Prone with NullAway
./gradlew test
```

`SharedRefreshesTest` starts Redis with Testcontainers, so Docker or Podman must be running for the tests. CI runs the
same two commands, then the Sonar analysis and the image build.

## Production

The image is Open Liberty's runnable jar (`./gradlew :modules:app:libertyPackage`), which carries no `server.env` of
its own. A deployment sets these variables in its environment; the application reads them through MicroProfile Config,
`KEYCLOAK_REALM_URL` as `keycloak.realm-url` and so on:

| Variable                                                                 | Purpose                                                                |
|--------------------------------------------------------------------------|------------------------------------------------------------------------|
| `MONGODB_URI`, `MONGODB_DATABASE`                                        | MongoDB                                                                |
| `REDIS_ADDRESS`, `REDIS_PASSWORD`                                        | Redis                                                                  |
| `REDIS_KEY_PREFIX`                                                       | the prefix of every key, `jakarta-ee-api` when unset                   |
| `KEYCLOAK_REALM_URL`, `KEYCLOAK_CLIENT_SECRET`                           | the realm and the confidential client                                  |
| `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_AUDIENCE`                                | the client and the audience a token must carry                         |
| `AUTH_CALLBACK_URL`, `AUTH_POST_LOGIN_URL`                               | where Keycloak returns and where it lands                              |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`                                    | the SMTP server and the sender                                         |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | SMTP credentials and TLS                                               |
| `session_cookie_secure`, `session_cookie_name`                           | `true` and `__Host-JAKARTA_EE_API_SESSION` (the defaults) behind HTTPS |

Every variable but `REDIS_PASSWORD`, `REDIS_KEY_PREFIX` and the two cookie settings is required, and none has a
default: the application fails to start without it rather than reaching a local address.

## Documentation checks

The Markdown is formatted and linted with [markright](https://github.com/vertyll/markright), at the version the
[Docs workflow](../.github/workflows/docs.yml) pins. Install that version, from an archive of its
[release](https://github.com/vertyll/markright/releases) or with `cargo install --locked markright@VERSION`, then run:

```bash
markright fmt
markright check
```

`fmt` rewrites what it can; `check` reports the rest and fails on any issue, as the workflow does on every push and
pull request, and `check --fix` does both in one run. [`.markright.toml`](../.markright.toml) chooses the formatters
and linters and what each one skips.

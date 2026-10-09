# Architecture

## Modules

The application is one WAR deployed on Open Liberty, built from four Gradle modules:

| Module          | Holds                                                                               |
|-----------------|-------------------------------------------------------------------------------------|
| `app`           | the JAX-RS resources, authentication, users and the Liberty server configuration    |
| `common`        | the RFC 9457 problem document and the exception mappers that produce it             |
| `config`        | the CDI producer of the MongoDB client and database                                 |
| `session-store` | the Redisson name mapper that prefixes session keys; loaded by Liberty, not the WAR |

`session-store` is separate because Liberty's session cache loads it from a shared library next to Redisson, outside the
application's class loader.

## Routes

Every path sits under `/api` (`HelloApplication`). The resources are `AuthResource` (`/auth`), `UserResource`
(`/users`) and `HealthResource` (`/health`); the legal pages are static files in `modules/app/src/main/webapp/legal`.
There is no OpenAPI description; the resources are the reference.

## Access is declared on the resource

`RoleAuthorizationFilter` reads the standard annotations on the method, then on the class: `@PermitAll` lets anyone
in, `@DenyAll` nobody, and `@RolesAllowed` the listed realm roles. A resource with none of them requires a signed-in
caller, so a new endpoint is closed until it says otherwise. A refusal is `401` without a caller and `403` with the
wrong roles.

## Accounts mirror Keycloak

Keycloak owns the person: credentials, email, name and realm roles. The `users` collection in MongoDB holds a copy,
unique by Keycloak identifier and by email, rewritten at every sign-in and on every `GET /users/me`.

## Errors

Every refusal is an RFC 9457 problem document (`application/problem+json`, `common/problem`):

| Field                                 | Holds                                                                |
|---------------------------------------|----------------------------------------------------------------------|
| `type`, `title`, `status`, `instance` | `about:blank`, the status's reason phrase, the HTTP status, the path |
| `detail`                              | for a business or authentication error, its message key              |
| `code`                                | the message key, e.g. `errors.auth.accessDenied`                     |
| `args`                                | the arguments of that key, in order                                  |
| `errors`                              | in a validation error, the messages of each invalid field            |

Business exceptions extend `BaseBusinessException`, which carries the key, its arguments and the status; authentication
refusals carry `errors.auth.authenticationRequired` or `errors.auth.accessDenied`. For these the server sends keys, not
sentences, and keeps no translation catalogue: the client maps `code` and `args` to text in its own language.

> [!WARNING]
>
> Two refusals do not follow that yet: a bean validation failure answers `detail: "Validation failed"` with the
> constraint messages under `errors`, and anything unexpected a `500` with an English `detail`; neither has a `code`.
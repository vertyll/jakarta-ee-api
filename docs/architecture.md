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

Every refusal is an RFC 9457 problem document carrying a message key: [Error responses](mechanisms/error-responses.md).

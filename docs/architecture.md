# Architecture

## Modules

The application is one WAR deployed on Open Liberty: a modular monolith of two modules, `auth` and `user`, each a
hexagon in Gradle subprojects of its own, assembled by `app`.

| Module          | Subprojects                                      | Holds                                                                               |
|-----------------|--------------------------------------------------|-------------------------------------------------------------------------------------|
| `auth`          | `api`, `domain`, `application`, `infrastructure` | sign-in through Keycloak, the browser's session, token verification and access      |
| `user`          | `domain`, `application`, `infrastructure`        | the accounts mirrored from Keycloak, in MongoDB, and `GET /users/me`                |
| `app`           | —                                                | the JAX-RS application, the health check, the legal pages and Liberty's setup       |
| `common`        | —                                                | the RFC 9457 problem document and the exception mappers that produce it             |
| `session-store` | —                                                | the Redisson name mapper that prefixes session keys; loaded by Liberty, not the WAR |

`session-store` is separate because Liberty's session cache loads it from a shared library next to Redisson, outside the
application's class loader.

## Inside a module

| Layer            | Holds                                                                                    |
|------------------|------------------------------------------------------------------------------------------|
| `domain`         | the model and its rules, such as `Identity`, `AuthSession` or `UserAccount`; plain Java  |
| `application`    | the use cases behind inbound ports, and the outbound ports they need; plain Java         |
| `infrastructure` | the adapters: JAX-RS resources and filters, Keycloak, Redis, MongoDB, and the CDI wiring |

Dependencies point inwards: `infrastructure` implements the ports of `application`, which uses `domain`. The domain and
the application carry no CDI annotation; each module's `infrastructure/config` produces its service, so the container
wires adapters to ports and nothing else names a concrete adapter.

## Between modules

`user` depends on `auth`, never the reverse, and only on `auth-api`: the person signed in (`SignedInPerson`, read from
the request as `SignedInPrincipal`) and the port a module implements to hear of a sign-in (`SignedInAccounts`). At
every sign-in the `auth` application calls that port, and `user`'s adapter `SignedInUsers` turns it into its own use
case, mirroring the account.

The Gradle dependencies make anything else fail to compile, and `ModuleBoundariesTest` in `app` checks with ArchUnit
that the modules form no cycle, that `user` uses no more of `auth` than its API, and that no domain or application
imports an adapter or a framework.

## Routes

Every path sits under `/api` (`HelloApplication`). The resources are `AuthResource` (`/auth`, in `auth`),
`UserResource` (`/users`, in `user`) and `HealthResource` (`/health`, in `app`); the legal pages are static files in
`modules/app/src/main/webapp/legal`. There is no OpenAPI description; the resources are the reference.

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

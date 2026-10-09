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

- **Identity provider**: Keycloak (realm `jakarta-ee-api`); the application never sees a password.
- **Pattern**: BFF; the tokens stay in the server-side session, the browser holds only a session cookie.
- **Session store**: Redis (Open Liberty session cache with Redisson).
- **JWT**: verified by a JAX-RS filter with Nimbus JOSE + JWT; `@RolesAllowed` decides access.
- **Details**: [Authentication](docs/authentication.md).

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

## Documentation

- [Contents](CONTENTS.md) – every document in the repository, the module it belongs to, and what it covers.
- [Glossary](GLOSSARY.md) – every term the docs use, and where it is explained.
- [Standards](STANDARDS.md) – the RFCs and specifications the code implements or depends on.

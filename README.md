## Project Assumptions

Jakarta EE template REST API.

## Technology Stack

### Back-end:

- Java.
- Jakarta EE.
- Gradle Kotlin DSL.
- MongoDB.
- JUnit.
- Mockito.
- Lombok.

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
application-server installation. MongoDB is configured through the `MONGODB_URI` and `MONGODB_DATABASE`
environment variables.

- `./gradlew :modules:app:libertyRun` starts the server in the foreground on port 8080.
- `./gradlew :modules:app:libertyDev` starts dev mode with hot reload.
- `./gradlew :modules:app:libertyPackage` builds a runnable jar, started with `java -jar modules/app/build/libs/jakarta-ee-api.jar`.
- `docker build -t jakarta-ee-api .` builds the image of the runnable jar.

The health endpoint is `GET /api/health`. The server configuration is in `modules/app/src/main/liberty/config/server.xml`.

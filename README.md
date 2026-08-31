# HourBlue

HourBlue is a visual-discovery website being built as a server-rendered Spring Boot application.

## Status

Milestone 1 is complete. Milestone 2 is complete. Milestone 3C adds Spring Security, database-backed admin authentication, server-side sessions, and secure initial-admin bootstrap.

## Implemented

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web
- Spring Boot Actuator
- Spring Boot Test
- Spring Data JPA
- Spring Security
- MySQL Connector/J
- Flyway
- V1 content schema migration
- V2 admin schema migration
- `Category`, `Post`, and `PostStatus`
- `CategoryRepository` and `PostRepository`
- `Admin` and `AdminRepository`
- BCrypt password hashing
- Admin authentication with server-side sessions
- Initial admin bootstrap through environment variables
- Default Spring profile: `dev`
- Application timezone configured from `APP_TIME_ZONE`, defaulting to `UTC`
- MySQL datasource configuration for development and test profiles

## Planned MVP Stack

The following technologies and architectural components are planned for the MVP and are not implemented yet:

- Thymeleaf with server-side rendering
- Tailwind CLI
- Cloudinary
- Modular monolith architecture

## Prerequisites

- JDK 21
- No global Maven installation is required; use the included Maven Wrapper.
- Local MySQL databases for persistence and repository verification: `hourblue` and `hourblue_test`.

## Commands

Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Persistence verification with a session-local database password:

```powershell
$env:DB_PASSWORD = 'the-local-password'
.\mvnw.cmd clean verify
Remove-Item Env:DB_PASSWORD
```

Unix-like shells:

```sh
./mvnw test
./mvnw clean package
./mvnw spring-boot:run
```

## Configuration

`.env.example` is a reference for supported environment variables. Spring Boot is not configured to read `.env` files automatically.

| Variable | Default | Description | Example |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile. | `dev` |
| `APP_TIME_ZONE` | `UTC` | Application timezone. Must be a valid Java `ZoneId`. | `Asia/Kolkata` |
| `DB_HOST` | `127.0.0.1` | MySQL host. | `127.0.0.1` |
| `DB_PORT` | `3306` | MySQL port. | `3306` |
| `DB_NAME` | `hourblue` | Development database name. | `hourblue` |
| `TEST_DB_NAME` | `hourblue_test` | Test database name. | `hourblue_test` |
| `DB_USERNAME` | `hourblue_app` | MySQL application user. | `hourblue_app` |
| `DB_PASSWORD` | none | MySQL password. Must be supplied externally and never committed. | |
| `ADMIN_BOOTSTRAP_EMAIL` | none | Optional first-admin email used only when no admin exists. | |
| `ADMIN_BOOTSTRAP_PASSWORD` | none | Optional first-admin password used only when no admin exists. | |
| `SESSION_COOKIE_SECURE` | `false` | Whether the session cookie requires HTTPS. | `true` |

Spring Security currently uses the generated login page at `/login`. A custom admin login page is planned for Milestone 3D.

## Health

Spring Boot Actuator exposes the health endpoint:

```http
GET /actuator/health
```

Expected successful response:

```json
{"status":"UP"}
```

Sensitive actuator endpoints such as `/actuator/env` are not exposed.

## Project Structure

```text
src/main/java/com/hourblue/       Spring Boot application and configuration
src/main/java/com/hourblue/admin/ Admin entity and repository
src/main/java/com/hourblue/category/ Category entity and repository
src/main/java/com/hourblue/post/  Post entity, status enum, and repository
src/main/resources/application*.yml Base and test-profile configuration
src/main/resources/db/migration/  Flyway migrations
src/test/java/com/hourblue/       Spring Boot tests
pom.xml                           Maven project configuration
mvnw, mvnw.cmd                    Maven Wrapper scripts
```

## Next Step

Next milestone: custom admin login and a minimal `/admin` landing page.

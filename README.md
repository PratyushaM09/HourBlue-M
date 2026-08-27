# HourBlue

HourBlue is a visual-discovery website being built as a server-rendered Spring Boot application.

## Status

Milestone 1 is complete: the project foundation, base configuration, application timezone bean, and actuator health verification are in place.

## Implemented

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web
- Spring Boot Actuator
- Spring Boot Test
- Default Spring profile: `dev`
- Application timezone configured from `APP_TIME_ZONE`, defaulting to `UTC`



## Planned MVP Stack

The following technologies and architectural components are planned for the MVP and are not implemented yet:

- Thymeleaf with server-side rendering
- Tailwind CLI
- MySQL
- Flyway
- Spring Security with admin sessions
- Cloudinary
- Modular monolith architecture

## Prerequisites

- JDK 21
- No global Maven installation is required; use the included Maven Wrapper.

## Commands

Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
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
src/main/resources/application.yml Base application configuration
src/test/java/com/hourblue/       Spring Boot tests
pom.xml                           Maven project configuration
mvnw, mvnw.cmd                    Maven Wrapper scripts
```

## Next Step

Milestone 2 will introduce the domain model, Flyway database migrations, and Spring Data repositories.

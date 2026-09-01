# HourBlue

HourBlue is a visual-discovery website being built as a server-rendered Spring Boot application.

## Status

Milestones 1 through 8 are complete. Public visitors can browse published posts on the homepage, category pages, mood pages, and individual post pages, and can join the HourBlue mailing list.

## Implemented

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web
- Spring Boot Actuator
- Spring Boot Test
- Spring Data JPA
- Spring Security
- Thymeleaf
- Cloudinary SDK foundation
- MySQL Connector/J
- Flyway
- V1 content schema migration
- V2 admin schema migration
- V3 post mood migration
- V4 Today's Moment schema migration
- `Category`, `Post`, `PostStatus`, `Mood`, and `TodayMoment`
- `Subscriber`
- `CategoryRepository` and `PostRepository`
- `TodayMomentRepository`
- `SubscriberRepository`
- `Admin` and `AdminRepository`
- BCrypt password hashing
- Admin authentication with server-side sessions
- Initial admin bootstrap through environment variables
- Custom admin login at `/admin/login`
- Protected admin landing page at `/admin`
- Admin category and post management pages
- Admin Today's Moment management at `/admin/today`
- Image upload validation for JPEG, PNG, and WebP files up to 5 MB
- Image replacement for existing posts
- Public homepage at `/`
- Public category browsing at `/categories/{slug}`
- Public mood browsing at `/moods/{slug}`
- Public post detail pages at `/posts/{slug}`
- Public email subscription at `POST /subscribe`
- Normalized unique subscriber emails
- Default Spring profile: `dev`
- Application timezone configured from `APP_TIME_ZONE`, defaulting to `UTC`
- MySQL datasource configuration for development and test profiles

## Planned MVP Stack

The following technologies and architectural components are planned for the MVP and are not implemented yet:

- Tailwind CLI
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
| `CLOUDINARY_CLOUD_NAME` | none | Cloudinary cloud name. Required for media upload. | |
| `CLOUDINARY_API_KEY` | none | Cloudinary API key. Required for media upload. | |
| `CLOUDINARY_API_SECRET` | none | Cloudinary API secret. Required for media upload and never committed. | |
| `CLOUDINARY_FOLDER` | `hourblue/posts` | Cloudinary folder for post images. | `hourblue/posts` |
| `MAX_IMAGE_SIZE` | `5MB` | Maximum individual image size. | `5MB` |
| `MAX_UPLOAD_REQUEST_SIZE` | `6MB` | Maximum multipart request size. | `6MB` |

Admin sign-in is available at `/admin/login`.
The application starts without Cloudinary credentials, but media upload is unavailable until all Cloudinary credential variables are supplied.

## Today's Moment

Today's Moment highlights one published post for a specific application-local calendar date. Explicit assignments are unique per date and are managed at `/admin/today`.

The current date is resolved with the configured application timezone from `APP_TIME_ZONE`. If today's explicit assignment is missing or no longer points to a published post, the homepage falls back to the newest currently published post without writing a database row.

## Public Pages

- `GET /` renders published posts only, newest published first, with fixed server-side pagination.
- `POST /subscribe` accepts a mailing-list email address, normalizes it, and redirects back to the homepage.
- `GET /categories/{slug}` renders published posts in an existing category, newest published first.
- `GET /moods/{slug}` renders published posts for a supported mood, newest published first.
- `GET /posts/{slug}` renders a published post detail page.
- Draft and archived posts are not exposed through public routes and return the same 404 page as missing posts.

Moods are stored as nullable enum values: `CALM`, `DREAMY`, `COZY`, `ROMANTIC`, `ADVENTUROUS`, and `NOSTALGIC`.

Subscriber email addresses are trimmed, lowercased with `Locale.ROOT`, and stored with a unique database constraint. Duplicate submissions return the same public success flow as first-time subscriptions. Email delivery is not implemented yet.

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
src/main/java/com/hourblue/image/ Cloudinary image-storage foundation
src/main/java/com/hourblue/post/  Post entity, enums, and repository
src/main/java/com/hourblue/publicsite/ Public browsing controller
src/main/java/com/hourblue/subscriber/ Subscriber entity, repository, service, and public form handler
src/main/resources/application*.yml Base and test-profile configuration
src/main/resources/db/migration/  Flyway migrations
src/main/resources/static/css/ Public and admin stylesheets
src/main/resources/templates/ Server-rendered Thymeleaf templates
src/test/java/com/hourblue/       Spring Boot tests
pom.xml                           Maven project configuration
mvnw, mvnw.cmd                    Maven Wrapper scripts
```

## Milestone 8

The public homepage now supports a minimal email subscription form with normalized, unique subscriber persistence and idempotent duplicate handling.

The next step is the next approved MVP milestone.

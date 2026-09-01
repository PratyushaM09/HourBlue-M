# HourBlue

HourBlue is a visual-discovery website built as a server-rendered Spring Boot application.

## Status

Milestones 1 through 10 are complete. The MVP is ready for deployment on a generic Java hosting platform after production environment variables, MySQL, and Cloudinary are configured. No production deployment is claimed here.

## MVP Features

- Public homepage, category browsing, mood browsing, and post detail pages
- Today's Moment homepage highlight with newest-published fallback
- Public email subscription form with normalized unique subscriber emails
- Admin-only category, post, image replacement, and Today's Moment management
- Custom admin login with server-side sessions, BCrypt passwords, CSRF protection, and POST logout
- Page-specific SEO metadata, canonical URLs, Open Graph metadata, `robots.txt`, `sitemap.xml`, and post detail JSON-LD
- Spring Boot Actuator health endpoint at `GET /actuator/health`

## Stack

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web, Spring Data JPA, Spring Security, Spring Boot Actuator, Validation, Thymeleaf, and Spring Boot Test
- MySQL Connector/J
- Flyway
- Cloudinary SDK foundation

## Local Development

Prerequisites:

- JDK 21
- Local MySQL databases: `hourblue` and `hourblue_test`
- No global Maven installation is required; use the included Maven Wrapper.

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

Persistence verification with a session-local database password:

```powershell
$env:DB_PASSWORD = 'the-local-password'
.\mvnw.cmd clean verify
Remove-Item Env:DB_PASSWORD
```

## Configuration

`.env.example` is a reference for supported environment variables. Spring Boot is not configured to read `.env` files automatically.

| Variable | Default | Description | Example |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile. Use `prod` for production. | `prod` |
| `APP_TIME_ZONE` | `UTC` | Application timezone. Must be a valid Java `ZoneId`. | `Asia/Kolkata` |
| `SITE_BASE_URL` | `http://localhost:8080` | Absolute HTTP or HTTPS canonical base URL for SEO, robots, and sitemap output. Required in `prod`. | `https://www.example.com` |
| `PORT` | `8080` | Runtime HTTP port used by generic Java hosting platforms. | `8080` |
| `DB_HOST` | `127.0.0.1` | MySQL host. Required in `prod`. | `127.0.0.1` |
| `DB_PORT` | `3306` | MySQL port. | `3306` |
| `DB_NAME` | `hourblue` | Database name. Required in `prod`. | `hourblue` |
| `TEST_DB_NAME` | `hourblue_test` | Test database name. | `hourblue_test` |
| `DB_USERNAME` | `hourblue_app` | MySQL application user. Required in `prod`. | `hourblue_app` |
| `DB_PASSWORD` | none | MySQL password. Must be supplied externally and never committed. | |
| `DB_SSL_MODE` | `REQUIRED` in `prod` | Production MySQL Connector/J SSL mode. The value must match the database provider; use `DISABLED` only when the provider explicitly requires or permits non-TLS connections. | `REQUIRED` |
| `ADMIN_BOOTSTRAP_EMAIL` | none | Optional first-admin email used only when no admin exists. | |
| `ADMIN_BOOTSTRAP_PASSWORD` | none | Optional first-admin password used only when no admin exists. | |
| `SESSION_COOKIE_SECURE` | `false` locally, `true` in `prod` | Whether the session cookie requires HTTPS. | `true` |
| `CLOUDINARY_CLOUD_NAME` | none | Cloudinary cloud name. Required for media upload. | |
| `CLOUDINARY_API_KEY` | none | Cloudinary API key. Required for media upload. | |
| `CLOUDINARY_API_SECRET` | none | Cloudinary API secret. Required for media upload and never committed. | |
| `CLOUDINARY_FOLDER` | `hourblue/posts` | Cloudinary folder for post images. | `hourblue/posts` |
| `MAX_IMAGE_SIZE` | `5MB` | Maximum individual image size. | `5MB` |
| `MAX_UPLOAD_REQUEST_SIZE` | `6MB` | Maximum multipart request size. | `6MB` |

## MySQL And Flyway

Flyway owns schema changes. JPA runs with `spring.jpa.hibernate.ddl-auto=validate`; production must not rely on Hibernate to create or update tables. The current migrations create content, admin, mood, Today's Moment, subscriber, and public-query index structures.

## Cloudinary

The application starts without Cloudinary credentials for local/test use. Admin image upload and replacement require `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`. Do not commit Cloudinary credentials.

## Admin Bootstrap

Initial admin bootstrap runs only when the admin table is empty and both bootstrap variables are supplied. It stores a BCrypt password hash and does not overwrite existing admins. In production, provide bootstrap credentials only for initial setup and rotate or remove them from the runtime environment where practical.

## Security

- Admin routes under `/admin/**` require an authenticated server-side session.
- Login errors use a generic message and do not disclose whether an admin email exists.
- CSRF protection is enabled for form submissions, including login and logout.
- Session cookies are `HttpOnly`, `SameSite=Lax`, and `Secure=true` by default in `prod`.
- Spring Security sends baseline hardening headers, including `nosniff`, `DENY` frame options, strict-origin referrer policy, a minimal permissions policy, and a CSP for self-hosted HTML/CSS/forms plus Cloudinary images.
- Production error configuration suppresses stack traces and internal exception details.
- Only `GET /actuator/health` is intentionally exposed; sensitive actuator endpoints such as `/actuator/env` are not exposed.

## Public SEO Routes

- `GET /robots.txt` allows public crawling, disallows `/admin/`, and links the sitemap.
- `GET /sitemap.xml` includes the homepage, category pages, mood pages, and published post detail pages only.
- Canonical URLs are generated from configured `SITE_BASE_URL`, not request host headers.

## Production Build And Run

Build:

```powershell
.\mvnw.cmd clean package
```

Run:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
java -jar target/hourblue-0.0.1-SNAPSHOT.jar
```

Supply the production environment variables before startup. The required runtime inputs are:

```text
SPRING_PROFILES_ACTIVE=prod
APP_TIME_ZONE
SITE_BASE_URL
PORT
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
DB_SSL_MODE
ADMIN_BOOTSTRAP_EMAIL
ADMIN_BOOTSTRAP_PASSWORD
SESSION_COOKIE_SECURE
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
CLOUDINARY_FOLDER
MAX_IMAGE_SIZE
MAX_UPLOAD_REQUEST_SIZE
```

## Deployment Checklist

- Configure a Java 21 runtime.
- Configure MySQL credentials and allow the app to run Flyway migrations on startup.
- Set `SPRING_PROFILES_ACTIVE=prod`.
- Set `SITE_BASE_URL` to the public HTTPS origin.
- Set `SESSION_COOKIE_SECURE=true` when served over HTTPS.
- Provide Cloudinary credentials before using admin image upload.
- Confirm `GET /actuator/health` returns `{"status":"UP"}`.

## Project Structure

```text
src/main/java/com/hourblue/       Spring Boot application and configuration
src/main/java/com/hourblue/admin/ Admin entity, authentication, bootstrap, and admin pages
src/main/java/com/hourblue/category/ Category entity and repository
src/main/java/com/hourblue/image/ Cloudinary image-storage foundation
src/main/java/com/hourblue/post/  Post entity, enums, and repository
src/main/java/com/hourblue/publicsite/ Public browsing controller
src/main/java/com/hourblue/seo/ SEO URL builder, robots.txt, and sitemap.xml controller
src/main/java/com/hourblue/subscriber/ Subscriber entity, repository, service, and public form handler
src/main/resources/application*.yml Base, test, and production configuration
src/main/resources/db/migration/  Flyway migrations
src/main/resources/static/css/ Public and admin stylesheets
src/main/resources/templates/ Server-rendered Thymeleaf templates
src/test/java/com/hourblue/       Spring Boot tests
pom.xml                           Maven project configuration
mvnw, mvnw.cmd                    Maven Wrapper scripts
```

## Milestone 10

Milestone 10 completes MVP hardening with production configuration, security headers, validated canonical URL configuration, static-resource caching for production, public-query indexes, and final full-project verification.

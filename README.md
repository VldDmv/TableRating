# TableRating

A media collection tracker for **Games, Movies, Books, and TV Shows** — rate, tag, and manage everything you've watched, read, or played.

---

## Features

**Media Management**
- Track Games, Movies, Books, and TV Shows in separate categories
- Add scores (0–100), tags/genres, and completion status to each entry
- Cover image support via Ctrl+Click inline editing or cover modal
- Book title autocomplete via the OpenLibrary API (no key required). Game/movie/show
  autocomplete via RAWG/TMDB is wired in the UI but disabled server-side — the
  `/api/proxy` endpoints for them return 503 until an API key is configured

**Views & Navigation**
- Switch between Table view and Card view per category
- Sortable columns, search with debounce, tag/genre filtering
- Column visibility toggles, rows-per-page control, page dropdown
- Inline editing directly in the table row or card

**User Profiles**
- Public profiles with per-category stats and tables
- Privacy settings — control visibility of each category independently
- Avatar with auto-generated color based on username

**Admin Panel**
- User management — view all users, change roles (ADMIN / USER), delete accounts
- Tags & Genres management — create, edit, delete with media type assignments
- Admin dashboard with stats charts (Chart.js)

**UI/UX**
- Light / Dark mode toggle, persisted across sessions
- Toast notifications for all actions
- Responsive design — mobile, tablet, desktop
- Loading skeletons and empty states

---

## Tech Stack

**Backend**
- Java 21
- Spring Boot 3.5
- Spring Security (session-based auth, CSRF protection)
- Spring Data JPA (Hibernate)
- MySQL 8.0 with HikariCP connection pooling
- Flyway schema migrations
- Thymeleaf templates
- BCrypt password encoding
- SLF4J + Logback

**Frontend**
- Vanilla JavaScript (ES6 modules)
- Chart.js (admin dashboard)
- Jest + jsdom (unit tests)
- Playwright (browser end-to-end tests)
- CSS custom properties with dark mode via `[data-theme="dark"]`

**External APIs**
- [OpenLibrary](https://openlibrary.org/developers/api) — book search (active, no key)
- [RAWG](https://rawg.io/apidocs) — game search (proxy present but disabled)
- [TMDB](https://developer.themoviedb.org/) — movies & shows search (proxy present but disabled)

---

## Getting Started

### Quick start (Docker)

```bash
docker compose up --build
```

MySQL 8 starts alongside the app; Flyway creates the schema and seeds the
tag/genre reference data on first run. App starts at `http://localhost:8080`.

### Local run (without Docker)

Prerequisites: Java 21+, Maven 3.9+, MySQL 8.0+, Node.js (for frontend tests).

```bash
# copy src/main/resources/application.yml.example to application.yml
# and fill in your MySQL credentials, then:
mvn spring-boot:run
```

App starts at `http://localhost:8080`

### Database migrations

Schema is managed by Flyway (`src/main/resources/db/migration`). A fresh
database is created from `V1__baseline_schema.sql` and seeded with the
curated tag/genre lists (`V2__seed_tags_and_genres.sql`). An existing
database that already has the tables is baselined at version 2 on the first
start (`baseline-on-migrate`), so nothing is re-applied. New schema changes
go in as `V3__...`, `V4__...` files.

### Frontend Tests

```bash
npm install
npm test
```

### End-to-End Tests

Browser tests (Playwright) drive the real app on an in-memory H2 database —
Playwright boots the server itself, so no MySQL is needed:

```bash
npm install
npx playwright install chromium
npm run e2e
```

### Integration Tests

`MySqlMigrationIntegrationTest` runs the Flyway migrations against a real
MySQL 8 via Testcontainers and validates the entities against the resulting
schema. It runs automatically under `mvn verify` when Docker is available and
skips otherwise.

---

## Project Structure

```
src/main/
├── java/org/criticizer/
│   ├── config/           # Security, app config, global exception handler
│   ├── controller/       # MVC controllers (admin, auth, category, profile, API proxy)
│   ├── dto/              # Request/response DTOs, PageResponse, MessageResponse
│   ├── entity/           # JPA entities (User, Game, Movie, Book, Show, Tag, Genre...)
│   ├── exceptions/       # Hierarchical exception structure
│   ├── repository/       # Spring Data JPA repositories with custom queries
│   ├── security/         # AuthenticatedUser, SecurityUtil
│   └── service/          # Business logic (media services, user, profile, dashboard)
├── resources/
│   ├── static/
│   │   ├── css/          # Layered CSS: base → layout → components → pages
│   │   └── js/           # ES6 modules: core, features, admin, pages
│   ├── templates/        # Thymeleaf templates + fragments
│   ├── application.yml
│   └── logback.xml
```

---

## Architecture Notes

- **Abstract base classes** — `AbstractMediaService`, `AbstractMediaController`, `BaseEntity` reduce boilerplate across four media types
- **Profile access control** — `ProfileAccessService` with context objects handles public/private visibility logic
- **Frontend state** — `StateManager` (observer pattern) keeps table/card state in sync across modules


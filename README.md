# TableRating

A media collection tracker for **Games, Movies, Books, and TV Shows** — rate, tag, and manage everything you've watched, read, or played.

---

## Features

**Media Management**
- Track Games, Movies, Books, and TV Shows in separate categories
- Add scores (0–100), tags/genres, and completion status to each entry
- Cover image support via Ctrl+Click inline editing or cover modal
- Autocomplete powered by external APIs: RAWG (games), TMDB (movies & shows), OpenLibrary (books)

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
- Java 17
- Spring Boot 3.x
- Spring Security (session-based auth, CSRF protection)
- Spring Data JPA (Hibernate)
- MySQL 8.0 with HikariCP connection pooling
- Thymeleaf templates
- BCrypt password encoding
- SLF4J + Logback

**Frontend**
- Vanilla JavaScript (ES6 modules)
- Chart.js (admin dashboard)
- Jest + jsdom (unit tests)
- CSS custom properties with dark mode via `[data-theme="dark"]`

**External APIs**
- [RAWG](https://rawg.io/apidocs) — game search
- [TMDB](https://developer.themoviedb.org/) — movies & shows search
- [OpenLibrary](https://openlibrary.org/developers/api) — book search

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0+
- Node.js (for running frontend tests)

### Run

```bash
mvn spring-boot:run
```

App starts at `http://localhost:8080`

### Frontend Tests

```bash
npm install
npm test
```

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


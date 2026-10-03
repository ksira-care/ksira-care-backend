# Ksira Care — backend

Spring Boot API for the therapist portal. Everything is served under `/api`
(e.g. `https://ksiracare.com/api/...` behind the website's proxy).

## Run locally

Requires **JDK 25**.

```bash
./mvnw spring-boot:run      # dev profile: in-memory H2 + demo data, http://localhost:8080/api
./mvnw test                 # unit + integration tests
```

Demo accounts (dev profile only):

| Email                     | Password      |                      |
|---------------------------|---------------|----------------------|
| `therapist@ksiracare.com` | `password123` | active, with bookings and open hours |
| `disabled@ksiracare.com`  | `password123` | deactivated          |

To use the UI against this backend, run `npm run start:api` in the UI repo — it proxies
`/api` to `localhost:8080`. API docs (dev only): http://localhost:8080/api/swagger-ui.html

## Profiles and configuration

| Profile | Database | Notes |
|---------|----------|-------|
| `dev` (default) | in-memory H2 (PostgreSQL mode) | demo data, non-secure cookie for plain `http://localhost` |
| `prod` | PostgreSQL | set `SPRING_PROFILES_ACTIVE=prod`; API docs off |

Production environment variables (startup fails if any is missing):

| Variable | |
|----------|---|
| `KSIRA_DB_URL`, `KSIRA_DB_USERNAME`, `KSIRA_DB_PASSWORD` | PostgreSQL connection |
| `KSIRA_AUTH_JWT_SECRET` | base64, ≥ 256 bits — e.g. `openssl rand -base64 48`. Never commit it. |

The schema is managed by **Flyway** (`src/main/resources/db/migration`); Hibernate only
validates it. Add a new `V<n>__description.sql` for every schema change — never edit an
applied migration.

Portal rules live in `application.properties` (`ksira.portal.*`, `ksira.auth.*`): time zone
(IST), opening hours (09:00–23:00), 60-day window, 2-hour sessions, sign-in rate limit.

## Conventions

- **The therapist always comes from the session cookie**, never from the URL — so nobody
  can read or change another therapist's data. Someone else's booking answers 404.
- **Times** are stored in UTC and exchanged as epoch milliseconds. Calendar rules ("today",
  "this month", opening hours) use the portal time zone via `PortalCalendar`.
- **Ranges** are start-inclusive, end-exclusive.
- **Errors** are RFC 9457 problem details with a stable `code` (see `ErrorCode`), e.g.
  `{"status": 409, "code": "SLOT_BOOKED", "title": "Conflict", "detail": "..."}`.

## API

| Method | Path | |
|--------|------|---|
| `POST` | `/auth/login` | `{email, password}` → `{therapistId, email}` + httpOnly `ksira_session` cookie (SameSite=Strict, 2 h) |
| `POST` | `/auth/logout` | 204, clears the cookie |
| `GET`  | `/therapists/me` | profile; also the "am I signed in?" check (401 if not) |
| `GET`  | `/therapists/me/dashboard-summary` | `{completedThisMonth, completedAllTime, activeSince}` |
| `GET`  | `/therapists/me/slots?startTime&endTime` | every session hour in the range |
| `POST` | `/therapists/me/slots?startTime&endTime` | `[{id, time, status}]` — opens/closes only the hours sent |
| `GET`  | `/bookings?startTime&endTime` | `{bookings: [...]}`, cancelled ones excluded |
| `PATCH`| `/bookings/{id}` | `{bookingStatus}`: `COMPLETED` / `CLIENT_NO_SHOW` once started; `PENDING` undoes a mark the same day |

Error codes the portal relies on: `UNAUTHENTICATED`, `INVALID_CREDENTIALS`,
`ACCOUNT_DISABLED`, `RATE_LIMITED`, `SLOT_BOOKED`, `INVALID_SLOT_TIME`,
`BOOKING_STATUS_CHANGE_NOT_ALLOWED`, `VALIDATION_FAILED`, `NOT_FOUND`.

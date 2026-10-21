# Ksira Care — backend

Spring Boot API for the aapta portal. Everything is served under `/api`
(e.g. `https://ksiracare.com/api/...` behind the website's proxy).

*Aapta* is Ksira Care's name for the listeners who hold sessions (formerly "therapist"); the
code, database and API all use it.

## Run locally

Requires **JDK 25**.

```bash
./mvnw spring-boot:run      # dev profile: in-memory H2 + demo data, http://localhost:8080/api
./mvnw test                 # unit + integration tests
```

Demo accounts (dev profile only):

| Email                    | Password      |                                      |
|--------------------------|---------------|--------------------------------------|
| `aapta@ksiracare.com`    | `password123` | active, with bookings and open hours |
| `disabled@ksiracare.com` | `password123` | deactivated                          |
| `ravi@ksiracare.com`     | `password123` | active, open every hour today and tomorrow |
| `admin@ksiracare.com`    | `password123` | admin console, with bookings waiting for an aapta |

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

Portal rules live in `application.properties`:

| Property | Default | |
|----------|---------|---|
| `ksira.portal.time-zone` | `Asia/Kolkata` | days, months and opening hours are in this zone |
| `ksira.portal.first-slot-hour` / `last-slot-hour` | `9` / `23` | first and last session start |
| `ksira.portal.session-length` | `PT1H` | sessions run back to back from the first start |
| `ksira.portal.booking-window-days` | `60` | how far ahead aaptas may open hours |
| `ksira.portal.max-query-range` | `P92D` | longest `startTime`–`endTime` range per request |
| `ksira.auth.session-ttl` | `PT2H` | sign-in lasts this long, no refresh |
| `ksira.auth.login-attempts.*` | 5 per 15 min | failed sign-ins before an email is blocked |

## Deploy (Render + Neon)

```
Browser → ksiracare.com (Render static site) ─ /api/* rewritten → Render (this app, Docker) → Neon (PostgreSQL)
```

The browser only talks to `ksiracare.com`, so the session cookie is first-party (Safari
blocks third-party cookies) and no CORS is needed. Pick **Singapore** for both Render and Neon.

1. **Neon** — create a project. From its connection string
   `postgresql://<user>:<password>@<host>/<database>?sslmode=require` take:
   `KSIRA_DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require`, `KSIRA_DB_USERNAME`,
   `KSIRA_DB_PASSWORD`.
2. **Render** — New → Web Service → this repo, runtime **Docker** (uses `Dockerfile`).
   Set the three `KSIRA_DB_*` variables and `KSIRA_AUTH_JWT_SECRET`; health check path
   `/api/actuator/health`. The image already sets `SPRING_PROFILES_ACTIVE=prod`, and the app
   listens on Render's `$PORT`. Flyway creates the schema on first start.
3. **Website** — the UI is a Render static site whose first rewrite rule sends `/api/*` to
   `https://ksira-care-backend.onrender.com/api/*`; update it if this service is renamed. See the
   UI repo's README.

The free Render instance sleeps after ~15 minutes idle; the first request then takes up to a
minute.

### Adding an aapta (admin)

Run in Neon's SQL editor. Passwords are stored as bcrypt hashes, never plain text:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- once

INSERT INTO aaptas (id, first_name, last_name, email, password, phone, created_at, updated_at)
VALUES (gen_random_uuid(), 'Aanya', 'Mehta', 'aanya@ksiracare.com',
        crypt('<temporary password>', gen_salt('bf', 10)), '+91 98xxxxxxx', now(), now());

-- Languages they speak (codes from the languages table: ENGLISH, HINDI, MARATHI, KANNADA)
INSERT INTO aapta_languages (aapta_id, language_id)
SELECT a.id, l.id FROM aaptas a, languages l
WHERE a.email = 'aanya@ksiracare.com' AND l.code IN ('ENGLISH', 'HINDI');
```

To deactivate someone: `UPDATE aaptas SET is_active = false WHERE email = '…';` — their
current session stops working on the next request.

### Adding an admin

Admins are separate accounts in the `admins` table (an aapta's login doesn't open the admin
console, and vice versa). Create the first one in Neon's SQL editor:

```sql
INSERT INTO admins (id, first_name, last_name, email, password, created_at, updated_at)
VALUES (gen_random_uuid(), 'Meera', 'Iyer', 'meera@ksiracare.com',
        crypt('<temporary password>', gen_salt('bf', 10)), now(), now());
```

Deactivate the same way: `UPDATE admins SET is_active = false WHERE email = '…';`.

## Conventions

- **The signed-in account always comes from the session cookie**, never from the URL — so
  nobody can read or change another aapta's data. Someone else's booking answers 404.
- **Two roles, two sessions.** Aaptas use the portal API with the `ksira_session` cookie
  (path `/api`); admins use `/admin/**` with `ksira_admin_session` (path `/api/admin`). Each
  part of the API only reads its own cookie (one Spring Security chain per `Role`), and the
  token carries the role, so a session never works for the other side.
- **Times** are stored in UTC and exchanged as epoch milliseconds. Calendar rules ("today",
  "this month", opening hours) use the portal time zone via `PortalCalendar`.
- **Ranges** are start-inclusive, end-exclusive, and at most `max-query-range` long.
- **Lists** come in an envelope (`{"bookings": [...]}`, `{"slots": [...]}`) so fields can be
  added alongside them later without breaking clients.
- **Enums** are exchanged by their exact names, e.g. `AAPTA_AVAILABLE`.
- **Errors** are RFC 9457 problem details with a stable `code` (see `ErrorCode`), e.g.
  `{"status": 409, "code": "SLOT_BOOKED", "title": "Conflict", "detail": "..."}`.

## API

| Method | Path | |
|--------|------|---|
| `POST` | `/auth/login` | `{email, password}` → `{aaptaId, email}` + httpOnly `ksira_session` cookie (SameSite=Strict, 2 h) |
| `POST` | `/auth/logout` | 204, clears the cookie |
| `POST` | `/auth/password` | `{currentPassword, newPassword}` → 204; `422 CURRENT_PASSWORD_INCORRECT` if the current one is wrong |
| `GET`  | `/aaptas/me` | profile; also the "am I signed in?" check (401 if not) |
| `GET`  | `/aaptas/me/dashboard-summary` | `{completedThisMonth, completedAllTime, activeSince}` |
| `GET`  | `/aaptas/me/slots?startTime&endTime` | `{slots: [{id, time, status}]}` — every session hour in the range |
| `POST` | `/aaptas/me/slots?startTime&endTime` | body `[{id, time, status}]` — opens/closes only the hours sent; answers like `GET` |
| `GET`  | `/bookings?startTime&endTime` | `{bookings: [...]}`, cancelled ones excluded |
| `PATCH`| `/bookings/{id}` | `{bookingStatus}`: `COMPLETED` / `CLIENT_NO_SHOW` once started; `PENDING` undoes a mark the same day |

Admin console:

| Method | Path | |
|--------|------|---|
| `POST` | `/admin/auth/login` | `{email, password}` → `{adminId, email}` + httpOnly `ksira_admin_session` cookie (SameSite=Strict, 2 h) |
| `POST` | `/admin/auth/logout` | 204, clears the admin cookie |
| `POST` | `/admin/auth/password` | `{currentPassword, newPassword}` → 204, the admin's own password |
| `GET`  | `/admin/me` | `{adminId, email, firstName, middleName, lastName}`; also the "am I signed in?" check |
| `GET`  | `/admin/bookings?date=YYYY-MM-DD` | `{date, bookings: [...]}` — every booking that day (IST), cancelled ones included, with `editable`/`editableFrom` worked out by the server |
| `GET`  | `/admin/bookings/{id}/form` | the website booking form as the client filled it: `{name, email, notes, consent, submittedAt}` |
| `GET`  | `/admin/bookings/{id}/aapta-options` | `{aaptas: [...]}` — the current aapta, then active aaptas who opened that hour |
| `PATCH`| `/admin/bookings/{id}` | `{aaptaId, bccEmailSent, version}` — "Add" / "Update"; answers with the updated row |
| `POST` | `/admin/bookings/{id}/cancel` | `{reason, version}` — calls the session off; answers with the updated row |
| `GET`  | `/admin/aaptas` | `{aaptas: [{aaptaId, firstName, middleName, lastName, email, languages}]}` — everyone who can sign in, by name |
| `POST` | `/admin/aaptas` | `{firstName, middleName?, lastName, email, languages: ["en", …], password}` → 201; `409 EMAIL_TAKEN` |
| `DELETE`| `/admin/aaptas/{id}` | removes the aapta → `{aaptaId, unallocatedBookings}` |
| `GET`  | `/admin/languages` | `{languages: [{code, name}]}` — the languages aaptas can be given |
| `GET`  | `/admin/availability?date=YYYY-MM-DD` | view-only grid for any date: `{date, hours: [{time, openAaptas}], aaptas: [{aaptaId, firstName, lastName, slots: [{time, status, booking}]}]}` — each aapta's `slots` line up with `hours`; `booking` (`{bookingId, client}`) is set on BOOKED cells |

Booking rules in the admin console:

- **When:** an admin may change a booking from **24 h before it starts until it starts**
  (`ksira.admin.booking-action-window`); otherwise it is view-only (`409 BOOKING_NOT_EDITABLE`).
- **Aapta:** only someone who opened that hour (`409 AAPTA_NOT_AVAILABLE` otherwise). Their hour
  becomes booked; on a change, the previous aapta's hour opens again.
- **BCC email:** set by the admin; changing the aapta leaves it as it was.
- **Cancelling:** any date, any time — not limited to the 24-hour window — as long as the aapta
  hasn't marked the session (`409 BOOKING_NOT_CANCELLABLE` otherwise). A reason is required and
  kept in the history; the aapta's hour opens again. There is no undo. Rows carry `cancellable`.
- **Cancelled bookings** stay listed with `bookingStatus: CANCELLED` and their `cancelReason`,
  and are never editable; aaptas no longer see them.
- **Outcome:** read-only here — it is the aapta's mark from their portal, and a reassignment
  clears it back to pending.
- **History:** every change is stored in `booking_events` (who, when, from → to); the day view
  shows the latest "was …" aapta.
- **Adding an aapta:** the admin sets their login (email, stored lower-case and unique) and a
  first password (8–72 characters) and shares it with them; the aapta can change it with
  `POST /auth/password`. Languages are ISO 639-1 codes — the UI formats them for display.
- **Removing an aapta** is a soft delete, so their bookings and history stay intact: the
  account is deactivated (login and any open session stop at once), their upcoming sessions go
  back to unallocated (shown with "was …"), and their future open hours are closed. Their email
  can't be reused for a new account.
- **Concurrent edits:** send back the row's `version`; if another admin saved first, the answer
  is `409 CONCURRENT_UPDATE`.

Error codes the portal relies on: `UNAUTHENTICATED`, `INVALID_CREDENTIALS`,
`ACCOUNT_DISABLED`, `RATE_LIMITED`, `SLOT_BOOKED`, `INVALID_SLOT_TIME`,
`BOOKING_STATUS_CHANGE_NOT_ALLOWED`, `VALIDATION_FAILED`, `NOT_FOUND`.

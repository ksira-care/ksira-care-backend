# Ksira Care API

Base URL: `/api` (e.g. `http://localhost:8080/api`).

## Conventions

- **Sessions.** Sign-in sets an httpOnly cookie: `ksira_session` for aaptas, `ksira_admin_session` for admins (path `/api/admin`). Send requests with credentials. Every endpoint except login and logout needs that role's cookie; without it, the response is `401 UNAUTHENTICATED`.
- **Times** are epoch milliseconds (UTC). **Dates** are `YYYY-MM-DD` and **months** are `YYYY-MM`, both in the portal time zone (IST).
- **Languages** are ISO 639-1 codes: `en`, `hi`, `mr`, `kn`.
- **Enums**
  - `bookingStatus`: `PENDING`, `COMPLETED`, `CLIENT_NO_SHOW`, `AAPTA_NO_SHOW`, `CANCELLED`
  - Slot `status`: `AAPTA_AVAILABLE`, `AAPTA_UNAVAILABLE`, `BOOKED`
- **Errors** are RFC 9457 problem responses. Branch on `code`:

```json
{ "type": "about:blank", "title": "Conflict", "status": 409, "detail": "That aapta hasn't opened this hour, or it's no longer free.", "instance": "/api/admin/bookings/…", "code": "AAPTA_NOT_AVAILABLE" }
```

| Code | Status |
|---|---|
| `VALIDATION_FAILED`, `HISTORY_MONTH_NOT_AVAILABLE` | 400 |
| `UNAUTHENTICATED`, `INVALID_CREDENTIALS` | 401 |
| `ACCOUNT_DISABLED`, `FORBIDDEN`, `FIELD_NOT_EDITABLE` | 403 |
| `NOT_FOUND` | 404 |
| `SLOT_BOOKED`, `BOOKING_STATUS_CHANGE_NOT_ALLOWED`, `BOOKING_NOT_EDITABLE`, `BOOKING_NOT_CANCELLABLE`, `EMAIL_TAKEN`, `AAPTA_NOT_AVAILABLE`, `CONCURRENT_UPDATE` | 409 |
| `INVALID_SLOT_TIME`, `CURRENT_PASSWORD_INCORRECT` | 422 |
| `RATE_LIMITED` | 429 |
| `INTERNAL_ERROR` | 500 |

---

# Aapta portal

## Auth

### `POST /auth/login`

```json
{ "email": "aapta@ksiracare.com", "password": "password123" }
```

`200` + `Set-Cookie: ksira_session=…`

```json
{ "aaptaId": "6f1c2a9e-3b4d-4e5f-8a7b-1c2d3e4f5a6b", "email": "aapta@ksiracare.com" }
```

Errors: `INVALID_CREDENTIALS`, `ACCOUNT_DISABLED`, `RATE_LIMITED`.

### `POST /auth/logout`

`204` and the cookie is cleared. This always succeeds.

### `POST /auth/password`

```json
{ "currentPassword": "password123", "newPassword": "a-new-password" }
```

`newPassword` must be 8–72 characters.

`204` + a fresh `Set-Cookie`. Every other session is signed out.

Errors: `CURRENT_PASSWORD_INCORRECT`, `VALIDATION_FAILED`.

## Profile

### `GET /aaptas/me`

This also serves as the portal's "am I signed in?" check.

`200`

```json
{
  "aaptaId": "6f1c2a9e-3b4d-4e5f-8a7b-1c2d3e4f5a6b",
  "email": "aapta@ksiracare.com",
  "firstName": "Aanya",
  "middleName": null,
  "lastName": "Mehta",
  "phone": "+919876543210",
  "dateOfBirth": "1991-03-14",
  "languages": ["en", "hi"],
  "address": "Pune, Maharashtra"
}
```

### `PATCH /aaptas/me`

Only the fields you send change. Send `""` to clear `middleName`, `phone` or `address`.

The aapta may send `firstName`, `middleName`, `lastName`, `phone` (E.164), `dateOfBirth`, `address` and `languages`. Sending `email` gives `403 FIELD_NOT_EDITABLE`.

```json
{ "phone": "+919812345678", "languages": ["en", "mr"] }
```

`200`: the profile, as in `GET /aaptas/me`.

### `GET /aaptas/me/dashboard-summary`

`200`

```json
{ "completedThisMonth": 12, "completedAllTime": 48, "activeSince": "2026-09-02" }
```

## Slots (opening hours)

### `GET /aaptas/me/slots?startTime={ms}&endTime={ms}`

The range is `[startTime, endTime)` and may span at most 92 days.

`200`. An hour that was never opened has `id: null`.

```json
{
  "slots": [
    { "id": "0b8e…", "time": 1792643400000, "status": "AAPTA_AVAILABLE" },
    { "id": null, "time": 1792647000000, "status": "AAPTA_UNAVAILABLE" }
  ]
}
```

### `POST /aaptas/me/slots?startTime={ms}&endTime={ms}`

Send only the hours that change.

```json
[
  { "id": null, "time": 1792647000000, "status": "AAPTA_AVAILABLE" },
  { "id": "0b8e…", "time": 1792643400000, "status": "AAPTA_UNAVAILABLE" }
]
```

`200`: the range, shaped like the `GET` response.

Errors: `SLOT_BOOKED` (the hour has a session), `INVALID_SLOT_TIME`.

## Bookings

### `GET /bookings?startTime={ms}&endTime={ms}`

`200`

```json
{
  "bookings": [
    {
      "bookingId": "c3d4e5f6-…",
      "startTime": 1792643400000,
      "endTime": 1792647000000,
      "customerName": "Rohan",
      "bookingReason": "Work stress",
      "bookingStatus": "PENDING",
      "customerPreferredLanguages": ["en"],
      "assignedAt": 1792487520000,
      "rescheduledFrom": null,
      "rescheduleNote": null,
      "markedAt": null
    }
  ]
}
```

### `PATCH /bookings/{bookingId}`

The aapta marks the outcome; `PENDING` undoes a mark.

```json
{ "bookingStatus": "COMPLETED" }
```

`200`: one booking item, as above.

Error: `BOOKING_STATUS_CHANGE_NOT_ALLOWED`.

Sessions still unmarked when their day ends (IST) automatically become `AAPTA_NO_SHOW`.

---

# Admin console

## Auth

### `POST /admin/auth/login`

```json
{ "email": "admin@ksiracare.com", "password": "password123" }
```

`200` + `Set-Cookie: ksira_admin_session=…`

```json
{ "adminId": "9a8b7c6d-…", "email": "admin@ksiracare.com" }
```

Errors: `INVALID_CREDENTIALS`, `ACCOUNT_DISABLED`, `RATE_LIMITED`.

### `POST /admin/auth/logout`

`204` and the cookie is cleared.

### `POST /admin/auth/password`

The body is the same as for `POST /auth/password`.

`204` + a fresh cookie. Every other session is signed out.

### `GET /admin/me`

`200`

```json
{ "adminId": "9a8b7c6d-…", "email": "admin@ksiracare.com", "firstName": "Meera", "middleName": null, "lastName": "Shah" }
```

## Bookings (Today / Tomorrow / any date)

### `GET /admin/bookings?date=2026-10-22`

Returns every booking that starts that day, in start order. Cancelled bookings are included.

`200`

```json
{
  "date": "2026-10-22",
  "bookings": [
    {
      "bookingId": "c3d4e5f6-…",
      "startTime": 1792643400000,
      "endTime": 1792647000000,
      "client": { "name": "Rohan", "email": "rohan@example.com" },
      "aapta": { "aaptaId": "6f1c…", "firstName": "Aanya", "lastName": "Mehta" },
      "previousAapta": { "aaptaId": "1a2b…", "firstName": "Ravi", "lastName": "Kumar" },
      "bccEmailSent": true,
      "bookingStatus": "PENDING",
      "markedAt": null,
      "editable": true,
      "editableFrom": 1792557000000,
      "cancellable": true,
      "cancelReason": null,
      "version": 3
    }
  ]
}
```

- `aapta` is `null` until the booking is allocated (the UI shows **Add**).
- `editable` is true from `editableFrom` (24 h before the start) until `startTime`.

### `GET /admin/bookings/{bookingId}/form`

`200`

```json
{
  "bookingId": "c3d4e5f6-…",
  "name": "Rohan",
  "email": "rohan@example.com",
  "notes": "Work stress",
  "consent": true,
  "submittedAt": 1792487520000
}
```

### `GET /admin/bookings/{bookingId}/aapta-options`

Lists the current aapta first (if there is one), then the aaptas who opened that hour.

`200`

```json
{ "aaptas": [ { "aaptaId": "6f1c…", "firstName": "Aanya", "lastName": "Mehta" } ] }
```

### `PATCH /admin/bookings/{bookingId}`

**Add** and **Update** both use this endpoint. Send back the `version` you loaded.

```json
{ "aaptaId": "6f1c…", "bccEmailSent": true, "version": 3 }
```

`200`: one booking row, as in `GET /admin/bookings`.

Errors: `BOOKING_NOT_EDITABLE`, `AAPTA_NOT_AVAILABLE`, `CONCURRENT_UPDATE`.

### `POST /admin/bookings/{bookingId}/cancel`

Allowed at any time while the booking is `PENDING`. There is no undo.

```json
{ "reason": "Client asked to cancel", "version": 3 }
```

`200`: the row, with `bookingStatus: "CANCELLED"` and the `cancelReason` filled in.

Errors: `BOOKING_NOT_CANCELLABLE`, `CONCURRENT_UPDATE`, `VALIDATION_FAILED` (when the reason is missing).

## Availability (view only)

### `GET /admin/availability?date=2026-10-22`

Each aapta's `slots` line up with `hours`, column for column.

`200`

```json
{
  "date": "2026-10-22",
  "hours": [ { "time": 1792643400000, "openAaptas": 2 } ],
  "aaptas": [
    {
      "aaptaId": "6f1c…",
      "firstName": "Aanya",
      "lastName": "Mehta",
      "slots": [
        {
          "time": 1792643400000,
          "status": "BOOKED",
          "booking": { "bookingId": "c3d4e5f6-…", "client": { "name": "Rohan", "email": "rohan@example.com" } }
        }
      ]
    }
  ]
}
```

## Aaptas

### `GET /admin/aaptas`

`200`

```json
{ "aaptas": [ { "aaptaId": "6f1c…", "email": "aapta@ksiracare.com", "firstName": "Aanya", "middleName": null, "lastName": "Mehta", "phone": "+919876543210", "dateOfBirth": "1991-03-14", "languages": ["en", "hi"], "address": "Pune" } ] }
```

### `POST /admin/aaptas`

```json
{ "firstName": "Aanya", "middleName": null, "lastName": "Mehta", "email": "aanya@ksiracare.com", "languages": ["en", "hi"], "password": "first-password" }
```

`201`: the profile, in the same shape as `GET /aaptas/me`.

Errors: `EMAIL_TAKEN`, `VALIDATION_FAILED`.

### `GET /admin/aaptas/{aaptaId}`

`200`: the profile. Returns `404` if the aapta was removed.

### `PATCH /admin/aaptas/{aaptaId}`

The body has the same shape as `PATCH /aaptas/me`, but an admin may send only `email` and `languages`. Changing the email signs the aapta out everywhere.

```json
{ "email": "aanya.m@ksiracare.com", "languages": ["en"] }
```

`200`: the profile.

Errors: `FIELD_NOT_EDITABLE`, `EMAIL_TAKEN`.

### `DELETE /admin/aaptas/{aaptaId}`

Removing an aapta:
- ends their login at once;
- sends their upcoming sessions back to unallocated;
- closes their open hours.

`200`

```json
{ "aaptaId": "6f1c…", "unallocatedBookings": 2 }
```

### `GET /admin/languages`

`200`

```json
{ "languages": [ { "code": "en", "name": "English" }, { "code": "hi", "name": "Hindi" }, { "code": "mr", "name": "Marathi" }, { "code": "kn", "name": "Kannada" } ] }
```

## History

### `GET /admin/history?month=2026-10`

`month` is optional and defaults to the current month. Only the current and previous months are offered; any other month gives `400 HISTORY_MONTH_NOT_AVAILABLE`.

`200`

```json
{
  "month": "2026-10",
  "months": ["2026-10", "2026-09"],
  "summary": { "total": 40, "completed": 30, "clientNoShow": 3, "refunded": 2, "cancelled": 1, "pending": 4 },
  "sessions": [
    {
      "bookingId": "c3d4e5f6-…",
      "startTime": 1792643400000,
      "endTime": 1792647000000,
      "client": { "name": "Rohan", "email": "rohan@example.com" },
      "aapta": { "aaptaId": "6f1c…", "firstName": "Aanya", "lastName": "Mehta" },
      "bookingStatus": "AAPTA_NO_SHOW",
      "refunded": true,
      "notes": "Work stress"
    }
  ],
  "olderMonthsNotExported": []
}
```

`refunded` is true exactly when `bookingStatus` is `AAPTA_NO_SHOW`.

### `POST /admin/history/export`

No body. This always exports **last month**. Afterwards, months older than the window are deleted, but only those that were exported before.

`200`

Headers:

```
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment; filename="ksira-sessions-2026-09.csv"
X-Deleted-Months: 2026-07
X-Not-Exported-Months: 2026-08
```

Body (truncated):

```csv
"Booking ID","Date","Start","End","Client name","Client email","What they expected","Consent","Aapta","Aapta email","Previous aapta","Outcome","Refunded","Marked at","BCC email sent","Assigned at","Cancel reason","Booked at"
"c3d4e5f6-…","2026-09-14","10:00","11:00","Rohan","rohan@example.com","Work stress","Yes","Aanya Mehta","aapta@ksiracare.com","-","Completed","No","2026-09-14 11:05","Yes","2026-09-13 18:20","-","2026-09-12 09:12"
```

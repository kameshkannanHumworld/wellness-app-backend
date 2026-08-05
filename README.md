# Wellness App Backend (Ktor)

## What's wired up so far
- Project skeleton (Gradle + Ktor + Exposed + Postgres + Firebase Admin + JWT)
- `config/` — env-driven Database connection (Supabase Postgres) and Firebase Admin init
- `common/` — API response envelope, exception → HTTP mapping, JWT issuing/verification, Firebase ID-token verification
- `auth/` module — `/api/v1/auth/google`, `/login`, `/signup`, `/session`, `/refresh`, `/logout`

## Not yet added (next steps, same pattern as `auth/`)
`onboarding`, `dashboard`, `hydration` (`water`), `activity`, `bloodpressure`, `reminder` modules —
each will follow: `model/` (Exposed table) → `dto/` → `repository/` → `service/` → `routes/`.

## Local setup

1. Install JDK 21 and Gradle (or use `gradle wrapper` once online to generate `gradlew`).
2. Copy `.env.example` → `.env` and fill in:
   - `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` — from Supabase project settings → Database → Connection string (use the **pooled connection**, port 6543, for serverless-friendly connections; direct 5432 works too for a single backend instance).
   - `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY` — from your **rotated** Firebase service account key (see security note below).
   - `JWT_SECRET` — generate with `openssl rand -base64 48`.
3. Run:
   ```bash
   gradle run
   ```
4. Health check: `GET http://localhost:8080/health` → `OK`

## Auth flow this backend expects

The Android app **always talks to Firebase directly first** (Google Sign-In SDK or
Firebase email/password), then sends the resulting **Firebase ID token** to us:

```
POST /api/v1/auth/google   { "firebaseToken": "<id-token-from-google-sign-in>" }
POST /api/v1/auth/login    { "firebaseToken": "<id-token-from-email-password-signin>" }
POST /api/v1/auth/signup   { "firebaseToken": "<id-token-after-createUserWithEmailAndPassword>" }
```

We verify the token server-side with Firebase Admin, create/find the `users` row,
and return **our own** JWT (`accessToken` + `refreshToken`). All other endpoints
require `Authorization: Bearer <accessToken>`.

## Security notes
- The real Firebase private key is **never committed** — it's read from env vars only (`.env` is gitignored).
- Deploy secrets go directly into Railway/Render's environment variable UI, not into files in the repo.
- Rotate any key that was ever pasted into a chat, email, or ticket.

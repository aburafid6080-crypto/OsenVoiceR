# OSEN TURBOPLAY Admin Panel

Responsive owner-only Next.js admin panel. It is isolated under `admin-panel/`; Android source and existing APK workflow are untouched.

## Setup
1. Create a Supabase project and enable Email + Password authentication.
2. Create the owner account using your own email in Supabase Auth. Do not share its password.
3. Open Supabase SQL Editor and run `supabase/schema.sql`.
4. Deploy this folder as a Next.js project on Vercel (set Root Directory to `admin-panel`).
5. Add every variable from `.env.example` in Vercel Project Settings. Set `ADMIN_EMAIL` to the exact owner email. Keep `SUPABASE_SERVICE_ROLE_KEY` server-only; never add a NEXT_PUBLIC prefix to it.
6. Redeploy after setting variables.

## Security notes
- Admin API validates the Supabase access token and checks the authenticated email against server-only `ADMIN_EMAIL` on every request.
- Database tables have RLS enabled and no public policies. The service-role key is used only in server route code.
- Premium password is stored as a salted PBKDF2-HMAC-SHA256 hash (210,000 iterations), never plaintext.
- This first stage provides the website and server-side control data. The Android app is not yet connected to this API; its existing local password behavior remains unchanged until a separately tested Android integration is added.
- Use HTTPS hosting. Do not commit real keys, owner passwords, or secrets.

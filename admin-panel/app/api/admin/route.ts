import { NextRequest, NextResponse } from "next/server";
import { pbkdf2Sync, randomBytes } from "node:crypto";

export const runtime = "nodejs";

const rawUrl =
  process.env.SUPABASE_URL || process.env.NEXT_PUBLIC_SUPABASE_URL || "";

const url = (() => {
  try {
    const parsed = new URL(rawUrl.trim());
    return `${parsed.protocol}//${parsed.host}`;
  } catch {
    return rawUrl.trim().replace(/\/+$/, "");
  }
})();

const anon =
  process.env.SUPABASE_ANON_KEY ||
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ||
  "";
const service = process.env.SUPABASE_SERVICE_ROLE_KEY || "";
const owner = (process.env.ADMIN_EMAIL || "").toLowerCase();

function config() {
  if (!url || !anon || !service || !owner) {
    throw new Error(
      "Server environment is incomplete. Configure Supabase URL, anon key, service role key and owner email."
    );
  }
}

async function authorize(req: NextRequest) {
  config();
  const token = req.headers
    .get("authorization")
    ?.replace(/^Bearer\s+/i, "");
  if (!token) throw new Error("Sign in required.");

  const response = await fetch(url + "/auth/v1/user", {
    headers: { apikey: anon, Authorization: "Bearer " + token },
    cache: "no-store",
  });
  if (!response.ok) throw new Error("Invalid login session.");

  const user = await response.json();
  if (String(user.email || "").toLowerCase() !== owner) {
    throw new Error("Owner access only.");
  }
  return user;
}

async function db(path: string, init: RequestInit = {}) {
  const response = await fetch(url + "/rest/v1/" + path, {
    ...init,
    headers: {
      apikey: service,
      Authorization: "Bearer " + service,
      "Content-Type": "application/json",
      Prefer: "return=representation",
      ...(init.headers || {}),
    },
    cache: "no-store",
  });
  const body = await response.text();
  if (!response.ok) {
    throw new Error("Database request failed: " + body.slice(0, 240));
  }
  return body ? JSON.parse(body) : null;
}

export async function GET(req: NextRequest) {
  try {
    await authorize(req);
    const [settings, users] = await Promise.all([
      db("app_settings?select=premium_enabled&id=eq.main"),
      db("premium_users?select=user_id,premium,updated_at&order=updated_at.desc"),
    ]);
    return NextResponse.json({
      settings: settings?.[0] || { premium_enabled: false },
      users: users || [],
    });
  } catch (error) {
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Unauthorized" },
      { status: 401 }
    );
  }
}

export async function POST(req: NextRequest) {
  try {
    await authorize(req);
    const body = await req.json();

    if (body.action === "set_premium_enabled") {
      if (typeof body.enabled !== "boolean") {
        throw new Error("Invalid status.");
      }
      await db("app_settings?id=eq.main", {
        method: "PATCH",
        body: JSON.stringify({ premium_enabled: body.enabled }),
      });
      return NextResponse.json({ ok: true });
    }

    if (body.action === "change_premium_password") {
      if (
        typeof body.password !== "string" ||
        body.password.length < 8 ||
        body.password.length > 128
      ) {
        throw new Error("Password must be 8–128 characters.");
      }
      const salt = randomBytes(16);
      const hash = pbkdf2Sync(body.password, salt, 210000, 32, "sha256");
      await db("app_settings?id=eq.main", {
        method: "PATCH",
        body: JSON.stringify({
          premium_password_salt: salt.toString("hex"),
          premium_password_hash: hash.toString("hex"),
          premium_password_iterations: 210000,
          premium_password_changed_at: new Date().toISOString(),
        }),
      });
      return NextResponse.json({ ok: true });
    }

    if (body.action === "set_user_premium") {
      if (
        typeof body.user_id !== "string" ||
        !body.user_id.trim() ||
        body.user_id.length > 128 ||
        typeof body.premium !== "boolean"
      ) {
        throw new Error("Enter a valid app user ID.");
      }
      await db("premium_users?on_conflict=user_id", {
        method: "POST",
        headers: { Prefer: "resolution=merge-duplicates,return=representation" },
        body: JSON.stringify({
          user_id: body.user_id.trim(),
          premium: body.premium,
          updated_at: new Date().toISOString(),
        }),
      });
      return NextResponse.json({ ok: true });
    }

    return NextResponse.json({ error: "Unknown action." }, { status: 400 });
  } catch (error) {
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Request failed" },
      { status: 400 }
    );
  }
}

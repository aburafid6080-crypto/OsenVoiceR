import { NextRequest, NextResponse } from "next/server";
import { pbkdf2Sync, timingSafeEqual } from "node:crypto";

export const runtime = "nodejs";

const supabaseUrl = process.env.SUPABASE_URL || "";
const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY || "";

async function db(path: string, init: RequestInit = {}) {
  if (!supabaseUrl || !serviceKey) throw new Error("Premium service is not configured.");
  const response = await fetch(`${supabaseUrl}/rest/v1/${path}`, {
    ...init,
    headers: {
      apikey: serviceKey,
      Authorization: `Bearer ${serviceKey}`,
      "Content-Type": "application/json",
      Prefer: "return=representation",
      ...(init.headers || {}),
    },
    cache: "no-store",
  });
  const body = await response.text();
  if (!response.ok) throw new Error("Premium service request failed.");
  return body ? JSON.parse(body) : null;
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const deviceId = typeof body.device_id === "string" ? body.device_id.trim().toLowerCase() : "";
    const password = typeof body.password === "string" ? body.password : "";

    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(deviceId)) {
      return NextResponse.json({ ok: false, code: "INVALID_DEVICE" }, { status: 400 });
    }
    if (password.length < 8 || password.length > 128) {
      return NextResponse.json({ ok: false, code: "INVALID_PASSWORD" }, { status: 400 });
    }

    // Register new installs as FREE so the owner can see and approve their device ID.
    await db("premium_users?on_conflict=user_id", {
      method: "POST",
      headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
      body: JSON.stringify({ user_id: deviceId, premium: false }),
    });

    const [settingsRows, userRows] = await Promise.all([
      db("app_settings?select=premium_enabled,premium_password_salt,premium_password_hash,premium_password_iterations&id=eq.main"),
      db(`premium_users?select=premium&user_id=eq.${encodeURIComponent(deviceId)}`),
    ]);
    const settings = settingsRows?.[0];
    const user = userRows?.[0];

    if (!settings?.premium_enabled) {
      return NextResponse.json({ ok: false, code: "PREMIUM_DISABLED" });
    }
    if (!user?.premium) {
      return NextResponse.json({ ok: false, code: "NOT_APPROVED", device_id: deviceId });
    }
    if (!settings.premium_password_salt || !settings.premium_password_hash) {
      return NextResponse.json({ ok: false, code: "PASSWORD_NOT_SET" });
    }

    const iterations = Number(settings.premium_password_iterations);
    if (!Number.isInteger(iterations) || iterations < 100000 || iterations > 1000000) {
      throw new Error("Premium password configuration is invalid.");
    }
    const salt = Buffer.from(settings.premium_password_salt, "hex");
    const expected = Buffer.from(settings.premium_password_hash, "hex");
    if (salt.length < 16 || expected.length !== 32) {
      throw new Error("Premium password configuration is invalid.");
    }
    const actual = pbkdf2Sync(password, salt, iterations, expected.length, "sha256");
    if (!timingSafeEqual(expected, actual)) {
      return NextResponse.json({ ok: false, code: "WRONG_PASSWORD" });
    }

    return NextResponse.json({ ok: true });
  } catch (error) {
    return NextResponse.json(
      { ok: false, code: "SERVICE_ERROR", error: error instanceof Error ? error.message : "Request failed." },
      { status: 500 },
    );
  }
}

import { NextRequest, NextResponse } from "next/server";
export const runtime = "nodejs";

const rawUrl = process.env.SUPABASE_URL || process.env.NEXT_PUBLIC_SUPABASE_URL || "";
const url = (() => { try { const p = new URL(rawUrl.trim()); return p.protocol + "//" + p.host; } catch { return rawUrl.trim().replace(/\\/+$/, ""); } })();
const anon = process.env.SUPABASE_ANON_KEY || process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || "";
const service = process.env.SUPABASE_SERVICE_ROLE_KEY || "";
const owner = (process.env.ADMIN_EMAIL || "").toLowerCase();

async function db(path: string, init: RequestInit = {}) {
  if (!url || !service) throw new Error("Device service is not configured.");
  const response = await fetch(url + "/rest/v1/" + path, {
    ...init,
    headers: { apikey: service, Authorization: "Bearer " + service, "Content-Type": "application/json", Prefer: "return=representation", ...(init.headers || {}) },
    cache: "no-store",
  });
  const body = await response.text();
  if (!response.ok) throw new Error("Device database request failed: " + body.slice(0, 180));
  return body ? JSON.parse(body) : null;
}

async function authorize(req: NextRequest) {
  if (!url || !anon || !service || !owner) throw new Error("Admin service is not configured.");
  const token = req.headers.get("authorization")?.replace(/^Bearer\\s+/i, "");
  if (!token) throw new Error("Sign in required.");
  const response = await fetch(url + "/auth/v1/user", { headers: { apikey: anon, Authorization: "Bearer " + token }, cache: "no-store" });
  if (!response.ok) throw new Error("Invalid login session.");
  const user = await response.json();
  if (String(user.email || "").toLowerCase() !== owner) throw new Error("Owner access only.");
  return user;
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const id = typeof body.device_id === "string" ? body.device_id.trim().toLowerCase() : "";
    const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
    if (!uuid.test(id)) return NextResponse.json({ ok: false, error: "Invalid device ID." }, { status: 400 });
    if (body.package_name !== "com.osenturboplay.app") return NextResponse.json({ ok: false, error: "Unrecognized app." }, { status: 400 });
    const clean = (v: unknown, max: number) => typeof v === "string" ? v.replace(/[\\r\\n\\0]/g, " ").trim().slice(0, max) : "";
    const device = {
      device_id: id,
      package_name: "com.osenturboplay.app",
      device_model: clean(body.device_model, 80) || "Android device",
      android_version: clean(body.android_version, 24) || "Unknown",
      app_version: clean(body.app_version, 24) || "Unknown",
      last_seen: new Date().toISOString(),
    };
    await db("registered_devices?on_conflict=device_id", {
      method: "POST",
      headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
      body: JSON.stringify(device),
    });
    return NextResponse.json({ ok: true, connected: true, device_id: id });
  } catch (error) {
    return NextResponse.json({ ok: false, error: error instanceof Error ? error.message : "Registration failed." }, { status: 503 });
  }
}

export async function GET(req: NextRequest) {
  try {
    await authorize(req);
    const devices = await db("registered_devices?select=device_id,package_name,device_model,android_version,app_version,first_seen,last_seen&order=last_seen.desc&limit=500");
    return NextResponse.json({ devices: devices || [] });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Unauthorized" }, { status: 401 });
  }
}

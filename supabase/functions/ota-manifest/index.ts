const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
};

function jsonResponse(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
  });
}

function toBoolean(value: string | undefined, fallback: boolean): boolean {
  if (!value) return fallback;
  const normalized = value.trim().toLowerCase();
  if (normalized === "true") return true;
  if (normalized === "false") return false;
  return fallback;
}

function toLong(value: string | undefined, fallback: number): number {
  if (!value) return fallback;
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

Deno.serve((request: Request): Response => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (request.method !== "GET") {
    return jsonResponse({ error: "Method not allowed." }, 405);
  }

  const versionCode = toLong(Deno.env.get("OTA_VERSION_CODE"), 1);
  const versionName = (Deno.env.get("OTA_VERSION_NAME") ?? "1.0.0").trim();
  const apkUrl = (
    Deno.env.get("OTA_APK_URL") ??
    "https://github.com/belinzenewtone/DART-2.0/releases"
  ).trim();
  const apkSha256 = (Deno.env.get("OTA_APK_SHA256") ?? "").trim();
  const changelog = (
    Deno.env.get("OTA_CHANGELOG") ??
    "Stability, performance, and quality improvements."
  ).trim();
  const mandatory = toBoolean(Deno.env.get("OTA_MANDATORY"), false);
  const title = (Deno.env.get("OTA_TITLE") ?? "Personal OS Update").trim();
  const message = (
    Deno.env.get("OTA_MESSAGE") ??
    "A new version is available with reliability upgrades."
  ).trim();
  const websiteUrl = (
    Deno.env.get("OTA_WEBSITE_URL") ??
    "https://github.com/belinzenewtone/DART-2.0"
  ).trim();

  if (!apkUrl) {
    return jsonResponse({ error: "OTA_APK_URL is not configured." }, 500);
  }

  return jsonResponse({
    version_code: versionCode,
    version_name: versionName,
    apk_url: apkUrl,
    apk_sha256: apkSha256 || null,
    changelog: changelog || null,
    mandatory,
    title: title || null,
    message: message || null,
    website_url: websiteUrl || null,
  });
});

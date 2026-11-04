import type { VercelRequest, VercelResponse } from "@vercel/node";

const MAX_BODY_BYTES = 8000;
const ALLOWED_CATEGORIES = new Set(["bug", "unsafe-output", "incorrect-answer", "other"]);

export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader("Cache-Control", "no-store");
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ error: "Method not allowed" });
  }
  const webhook = process.env.POCKETQWEN_REPORT_WEBHOOK_URL;
  if (!webhook) return res.status(503).json({ error: "Report service is not configured" });
  const raw = typeof req.body === "string" ? req.body : JSON.stringify(req.body ?? {});
  if (Buffer.byteLength(raw, "utf8") > MAX_BODY_BYTES) return res.status(413).json({ error: "Report is too large" });
  let body: any;
  try { body = typeof req.body === "string" ? JSON.parse(req.body) : req.body; }
  catch { return res.status(400).json({ error: "Invalid JSON" }); }
  const category = typeof body?.category === "string" ? body.category : "";
  const details = typeof body?.details === "string" ? body.details.trim().slice(0, 3000) : "";
  const excerpt = typeof body?.excerpt === "string" ? body.excerpt.slice(0, 2000) : "";
  const appVersion = typeof body?.appVersion === "string" ? body.appVersion.slice(0, 32) : "unknown";
  if (!ALLOWED_CATEGORIES.has(category)) return res.status(400).json({ error: "Invalid report category" });
  if (!details && !excerpt) return res.status(400).json({ error: "Add report details or select a response excerpt" });
  try {
    const upstream = await fetch(webhook, {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ product: "PocketQwen", category, details, excerpt, appVersion, receivedAt: new Date().toISOString() })
    });
    if (!upstream.ok) return res.status(502).json({ error: "Report service could not accept the report" });
    return res.status(202).json({ accepted: true });
  } catch {
    return res.status(502).json({ error: "Report service is temporarily unavailable" });
  }
}

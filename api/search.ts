export const config = { runtime: "edge" };

export default async function handler(request: Request): Promise<Response> {
  if (request.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { "content-type": "application/json", "allow": "POST" },
    });
  }

  const apiKey = process.env.TAVILY_API_KEY;
  if (!apiKey) {
    return Response.json({ error: "Web Search is not configured on this deployment." }, { status: 503 });
  }

  let query = "";
  try {
    const raw = await request.text();
    if (raw.length > 2000) return Response.json({ error: "Search query is too long." }, { status: 413 });
    const body = JSON.parse(raw) as Record<string, unknown>;
    query = typeof body.query === "string" ? body.query.trim() : "";
  } catch {
    return Response.json({ error: "Invalid JSON body." }, { status: 400 });
  }
  if (query.length < 2 || query.length > 400) {
    return Response.json({ error: "Enter a search query between 2 and 400 characters." }, { status: 400 });
  }

  try {
    const upstream = await fetch("https://api.tavily.com/search", {
      method: "POST",
      signal: AbortSignal.timeout(8_000),
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        api_key: apiKey,
        query,
        search_depth: "ultra-fast",
        topic: "general",
        max_results: 3,
        include_answer: false,
        include_raw_content: false,
      }),
    });
    if (!upstream.ok) return Response.json({ error: "Search provider request failed." }, { status: 502 });
    const data = await upstream.json() as { results?: Array<{ title?: string; url?: string; content?: string }> };
    const results = (data.results ?? []).slice(0, 3).map((item) => ({
      title: String(item.title ?? "").slice(0, 180),
      url: String(item.url ?? "").slice(0, 1000),
      snippet: String(item.content ?? "").slice(0, 1200),
    })).filter((item) => item.url.startsWith("https://") && item.snippet.length > 0);
    return Response.json({ query, results });
  } catch (error) {
    if (error instanceof DOMException && error.name === "TimeoutError") {
      return Response.json({ error: "Web Search timed out. Try again." }, { status: 504 });
    }
    return Response.json({ error: "Search is temporarily unavailable." }, { status: 502 });
  }
}

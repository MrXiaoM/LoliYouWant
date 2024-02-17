// Cloudflare Workers

// Original API
const api = "https://lolibooru.moe/";
// Replace API address in contents (Set to empty string if not replace)
const referTo = "";

async function handleRequest(request, origin, pathname, searchParams) {
  const apiUrl = new URL(api);
  apiUrl.pathname = pathname;

  for (const [key, value] of searchParams) {
    apiUrl.searchParams.append(key, value);
  }
  request = new Request(apiUrl, request);
  request.headers.set("Origin", apiUrl.origin);

  let response = await fetch(request, { method: "GET" });
  if(response.status == 200 || response.status == 301 || response.status == 302) {
    let changed = false;
    if (referTo.length > 0) {
      const type = response.headers.get("Content-Type");
      if (type != null && (
        type.includes("text") ||
        type.includes("javascript") ||
        type.includes("json")
      )) {
        let body = await response.text();
        body = body.replace(api, referTo);
        response = new Response(body, response);
        changed = true;
      }
    }
    if (!changed) response = new Response(response.body, response);

    response.headers.set("Access-Control-Allow-Origin", origin);
    response.headers.append("Vary", "Origin");
  }
  return response;
}

addEventListener("fetch", (event) => {
  const request = event.request;
  const url = new URL(request.url);
  const origin = request.headers.get("Origin");
  const pathname = url.pathname;
  const searchParams = url.searchParams;
  return event.respondWith(handleRequest(request, origin, pathname, searchParams));
});

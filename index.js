// Cloudflare Workers

async function handleRequest(request, origin, pathname, searchParams) {
  const apiUrl = new URL('https://lolibooru.moe/');
  apiUrl.pathname = pathname;

  for (const [key, value] of searchParams) {
    apiUrl.searchParams.append(key, value);
  }
  request = new Request(apiUrl, request);
  request.headers.set('Origin', apiUrl.origin);
  let response = await fetch(request, { method: 'GET' });
  if(response.status == 200 || response.status == 301 || response.status == 302) {
    response = new Response(response.body, response);
    response.headers.set('Access-Control-Allow-Origin', origin);
    response.headers.append('Vary', 'Origin');
    return response;
  }
  return new Response('');
}

addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);
  const origin = request.headers.get('Origin');
  const pathname = url.pathname;
  const searchParams = url.searchParams;
  return event.respondWith(handleRequest(request, origin, pathname, searchParams));
});
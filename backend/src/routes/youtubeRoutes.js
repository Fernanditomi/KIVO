import { Router } from 'express';
import https from 'https';
import http from 'http';
import { URL } from 'url';

const router = Router();

function httpsGet(url, headers = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const mod = parsed.protocol === 'https:' ? https : http;
    mod.get({
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
        'Accept-Language': 'es;q=0.9,en;q=0.8',
        'Cookie': 'CONSENT=YES+cb.20210328-17-p0.en+FX+999',
        ...headers
      }
    }, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        const loc = res.headers.location.startsWith('http')
          ? res.headers.location
          : `https://${parsed.hostname}${res.headers.location}`;
        return httpsGet(loc, headers).then(resolve, reject);
      }
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => resolve({ status: res.statusCode, body: data, headers: res.headers }));
    }).on('error', reject);
  });
}

function httpsPost(url, bodyStr, headers) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const mod = parsed.protocol === 'https:' ? https : http;
    const req = mod.request({
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(bodyStr),
        ...headers
      }
    }, res => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => resolve({ status: res.statusCode, body: d, headers: res.headers }));
    });
    req.on('error', reject);
    req.write(bodyStr);
    req.end();
  });
}

async function getVisitorData() {
  try {
    const resp = await httpsGet('https://www.youtube.com/');
    const vdMatch = resp.body.match(/"VISITOR_DATA":"([^"]+)"/);
    const cookies = (resp.headers['set-cookie'] || []).map(c => c.split(';')[0]).join('; ');
    return { visitorData: vdMatch ? vdMatch[1] : '', cookies };
  } catch (e) {
    return { visitorData: '', cookies: '' };
  }
}

async function getStreamFromEmbed(videoId) {
  console.log(`[youtube] Trying embed page for ${videoId}`);
  const resp = await httpsGet(`https://www.youtube.com/embed/${videoId}`);
  const html = resp.body;

  // Extract ytcfg
  const cfgMatch = html.match(/ytcfg\.set\((\{.*?\})\)/s);
  if (cfgMatch) {
    console.log('[youtube] Found ytcfg in embed');
  }

  // Look for embedded player response
  const prMatch = html.match(/var ytInitialPlayerResponse\s*=\s*(\{.*?\});/s);
  if (prMatch) {
    const json = JSON.parse(prMatch[1]);
    if (json.streamingData) return json.streamingData;
  }

  // Look for embedded_player_response
  const eprMatch = html.match(/"embedded_player_response":"(.*?)"/);
  if (eprMatch) {
    const decoded = eprMatch[1].replace(/\\x([0-9a-f]{2})/gi, (_, hex) => String.fromCharCode(parseInt(hex, 16)));
    try {
      const json = JSON.parse(decoded);
      if (json.streamingData) return json.streamingData;
    } catch (e) {
      console.log('[youtube] Failed to parse embedded_player_response');
    }
  }

  return null;
}

async function getStreamFromWatch(videoId) {
  console.log(`[youtube] Trying watch page for ${videoId}`);
  const resp = await httpsGet(`https://www.youtube.com/watch?v=${videoId}`);
  const html = resp.body;

  const marker = 'var ytInitialPlayerResponse = ';
  const startIdx = html.indexOf(marker);
  if (startIdx === -1) return null;

  const jsonStart = startIdx + marker.length;
  const jsonEnd = html.indexOf('};', jsonStart);
  if (jsonEnd === -1) return null;

  const json = JSON.parse(html.substring(jsonStart, jsonEnd + 1));
  console.log(`[youtube] Watch page status: ${json.playabilityStatus?.status}`);
  return json.streamingData || null;
}

async function getStreamViaInnerTube(videoId) {
  console.log(`[youtube] Trying InnerTube WEB for ${videoId}`);
  const { visitorData, cookies } = await getVisitorData();
  const allCookies = (cookies ? cookies + '; ' : '') + 'CONSENT=YES+cb.20210328-17-p0.en+FX+999';

  const bodyStr = JSON.stringify({
    videoId,
    context: {
      client: {
        clientName: 'WEB',
        clientVersion: '2.20241126.01.00',
        hl: 'es',
        gl: 'US',
        visitorData
      }
    },
    contentCheckOk: true,
    racyCheckOk: true
  });

  const resp = await httpsPost(
    'https://www.youtube.com/youtubei/v1/player?prettyPrint=false',
    bodyStr,
    {
      'Origin': 'https://www.youtube.com',
      'Referer': `https://www.youtube.com/watch?v=${videoId}`,
      'Cookie': allCookies,
      'X-Youtube-Client-Name': '1',
      'X-Youtube-Client-Version': '2.20241126.01.00'
    }
  );

  const json = JSON.parse(resp.body);
  console.log(`[youtube] InnerTube WEB status: ${json.playabilityStatus?.status}`);
  return json.streamingData || null;
}

router.get('/stream', async (req, res) => {
  const videoId = req.query.videoId;
  if (!videoId) return res.status(400).json({ error: 'Missing videoId' });

  try {
    console.log(`[youtube/stream] === Getting stream for ${videoId} ===`);

    // Try multiple methods
    let sd = await getStreamFromWatch(videoId);
    if (!sd) sd = await getStreamViaInnerTube(videoId);
    if (!sd) sd = await getStreamFromEmbed(videoId);

    if (!sd) return res.status(503).json({ error: 'No streaming data from any method' });

    // Try adaptiveFormats direct URL
    const af = sd.adaptiveFormats || [];
    for (const fmt of af) {
      if ((fmt.mimeType || '').includes('audio') && fmt.url) {
        console.log(`[youtube/stream] Found direct audio URL, proxying...`);
        return proxyUrl(fmt.url, res);
      }
    }

    // Try formats
    if (sd.formats?.[0]?.url) {
      console.log(`[youtube/stream] Found format URL, proxying...`);
      return proxyUrl(sd.formats[0].url, res);
    }

    // Try serverAbrStreamingUrl - try with POST method
    if (sd.serverAbrStreamingUrl) {
      console.log(`[youtube/stream] Found ABR URL, trying GET with PO token...`);
      return proxyUrl(sd.serverAbrStreamingUrl, res);
    }

    return res.status(404).json({ error: 'No audio URL found' });

  } catch (e) {
    console.error('[youtube/stream] Error:', e.message);
    if (!res.headersSent) res.status(500).json({ error: e.message });
  }
});

function proxyUrl(url, res) {
  const parsed = new URL(url);
  const mod = parsed.protocol === 'https:' ? https : http;
  const proxyReq = mod.get({
    hostname: parsed.hostname,
    path: parsed.pathname + parsed.search,
    headers: {
      'User-Agent': 'com.google.android.apps.youtube.vr/1.60.19 (Linux; U; Android 14) gzip',
      'Referer': 'https://www.youtube.com/',
      'Origin': 'https://www.youtube.com'
    }
  }, proxyRes => {
    if (proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
      const loc = proxyRes.headers.location.startsWith('http')
        ? proxyRes.headers.location
        : `https://${parsed.hostname}${proxyRes.headers.location}`;
      return proxyUrl(loc, res);
    }
    console.log(`[youtube/stream] Upstream: ${proxyRes.statusCode} CT: ${proxyRes.headers['content-type']} CL: ${proxyRes.headers['content-length']}`);
    if (!res.headersSent) {
      res.writeHead(proxyRes.statusCode, {
        'Content-Type': proxyRes.headers['content-type'] || 'audio/mp4',
        'Access-Control-Allow-Origin': '*',
        'Content-Length': proxyRes.headers['content-length']
      });
    }
    proxyRes.pipe(res);
  });
  proxyReq.on('error', (e) => {
    console.error('[youtube/stream] Proxy error:', e.message);
    if (!res.headersSent) res.status(502).json({ error: 'Proxy error: ' + e.message });
  });
}

// Search endpoint
router.get('/search', async (req, res) => {
  const query = req.query.q;
  if (!query) return res.status(400).json({ error: 'Missing q parameter' });

  try {
    const searchUrl = `https://www.youtube.com/results?search_query=${encodeURIComponent(query)}&sp=EgIQAQ%3D%3D`;
    const resp = await httpsGet(searchUrl);
    const html = resp.body;
    const results = [];
    const dataStart = html.indexOf('var ytInitialData = ');
    if (dataStart !== -1) {
      const jsonStart = dataStart + 'var ytInitialData = '.length;
      const jsonEnd = html.indexOf(';</script>', jsonStart);
      if (jsonEnd !== -1) {
        const json = JSON.parse(html.substring(jsonStart, jsonEnd));
        const contents = json.contents?.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer?.contents;
        if (contents) {
          for (const section of contents) {
            const items = section.itemSectionRenderer?.contents;
            if (!items) continue;
            for (const item of items) {
              const vr = item.videoRenderer;
              if (!vr?.videoId) continue;
              results.push({
                videoId: vr.videoId,
                title: vr.title?.runs?.[0]?.text || '',
                channel: vr.ownerText?.runs?.[0]?.text || '',
                thumbnail: vr.thumbnail?.thumbnails?.slice(-1)[0]?.url || '',
                duration: vr.lengthText?.simpleText || '0:00'
              });
              if (results.length >= 15) break;
            }
            if (results.length >= 15) break;
          }
        }
      }
    }
    res.json({ results });
  } catch (e) {
    console.error('[youtube/search]', e.message);
    res.status(500).json({ error: e.message });
  }
});

export default router;

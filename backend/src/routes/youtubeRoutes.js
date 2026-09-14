import { Router } from 'express';
import https from 'https';
import http from 'http';
import { URL } from 'url';

const router = Router();

function httpsGet(url, headers = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const mod = parsed.protocol === 'https:' ? https : http;
    const options = {
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
        'Accept-Language': 'es;q=0.9,en;q=0.8',
        'Cookie': 'CONSENT=YES+cb.20210328-17-p0.en+FX+999',
        ...headers
      }
    };
    mod.get(options, (res) => {
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

function httpsPost(url, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const bodyStr = typeof body === 'string' ? body : JSON.stringify(body);
    const req = https.request({
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
        'Content-Length': Buffer.byteLength(bodyStr),
        ...headers
      }
    }, res => {
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => resolve({ status: res.statusCode, body: data }));
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
    console.error('[youtube] getVisitorData error:', e.message);
    return { visitorData: '', cookies: '' };
  }
}

async function innertubePlayerWEB(videoId) {
  const { visitorData, cookies } = await getVisitorData();
  console.log(`[youtube] Got visitor data: ${!!visitorData}`);

  const body = {
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
  };

  const allCookies = (cookies ? cookies + '; ' : '') + 'CONSENT=YES+cb.20210328-17-p0.en+FX+999; SOCS=CAISNQgDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjQwNjEwLjA3X3AxGgJlbiACGgYIgJnaRQY';

  const resp = await httpsPost(
    'https://www.youtube.com/youtubei/v1/player?prettyPrint=false',
    body,
    {
      'Origin': 'https://www.youtube.com',
      'Referer': `https://www.youtube.com/watch?v=${videoId}`,
      'Cookie': allCookies,
      'X-Youtube-Client-Name': '1',
      'X-Youtube-Client-Version': '2.20241126.01.00'
    }
  );

  return JSON.parse(resp.body);
}

async function innertubePlayerANDROID_VR(videoId) {
  const body = {
    videoId,
    context: {
      client: {
        clientName: 'ANDROID_VR',
        clientVersion: '1.60.19',
        androidSdkVersion: 34,
        hl: 'es',
        gl: 'US'
      }
    },
    contentCheckOk: true,
    racyCheckOk: true
  };

  const resp = await httpsPost(
    'https://www.youtube.com/youtubei/v1/player?key=AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8&prettyPrint=false',
    body,
    { 'User-Agent': 'com.google.android.apps.youtube.vr/1.60.19 (Linux; U; Android 14) gzip' }
  );

  return JSON.parse(resp.body);
}

async function getAudioUrl(videoId) {
  // Try ANDROID_VR first (works from some IPs)
  let data = await innertubePlayerANDROID_VR(videoId);
  let status = data.playabilityStatus?.status;
  console.log(`[youtube] ANDROID_VR status: ${status}`);

  if (status === 'OK' && data.streamingData) {
    const af = data.streamingData.adaptiveFormats || [];
    for (const fmt of af) {
      if ((fmt.mimeType || '').includes('audio') && fmt.url) return fmt.url;
    }
    const fmts = data.streamingData.formats || [];
    if (fmts[0]?.url) return fmts[0].url;
    if (data.streamingData.serverAbrStreamingUrl) return data.streamingData.serverAbrStreamingUrl;
  }

  // Fallback: WEB client with visitor data
  data = await innertubePlayerWEB(videoId);
  status = data.playabilityStatus?.status;
  console.log(`[youtube] WEB status: ${status}`);

  if (status === 'OK' && data.streamingData) {
    const af = data.streamingData.adaptiveFormats || [];
    for (const fmt of af) {
      if ((fmt.mimeType || '').includes('audio') && fmt.url) return fmt.url;
    }
    const fmts = data.streamingData.formats || [];
    if (fmts[0]?.url) return fmts[0].url;
    if (data.streamingData.serverAbrStreamingUrl) return data.streamingData.serverAbrStreamingUrl;
  }

  return null;
}

// Stream endpoint: proxies audio to the client
router.get('/stream', async (req, res) => {
  const videoId = req.query.videoId;
  if (!videoId) return res.status(400).json({ error: 'Missing videoId' });

  try {
    console.log(`[youtube/stream] Getting stream for ${videoId}`);

    const audioUrl = await getAudioUrl(videoId);
    if (!audioUrl) {
      return res.status(503).json({ error: 'Could not get audio URL' });
    }

    console.log(`[youtube/stream] Got audio URL, proxying...`);

    // Proxy the audio stream
    const parsed = new URL(audioUrl);
    const proxyReq = https.request({
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      method: 'GET',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://www.youtube.com/',
        'Origin': 'https://www.youtube.com'
      }
    }, proxyRes => {
      console.log(`[youtube/stream] Upstream: ${proxyRes.statusCode}`);

      if (proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
        const loc = proxyRes.headers.location;
        const parsed2 = new URL(loc);
        const redirReq = https.request({
          hostname: parsed2.hostname,
          path: parsed2.pathname + parsed2.search,
          method: 'GET',
          headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://www.youtube.com/',
            'Origin': 'https://www.youtube.com'
          }
        }, redirRes => {
          if (!res.headersSent) {
            res.writeHead(redirRes.statusCode, {
              'Content-Type': redirRes.headers['content-type'] || 'audio/mp4',
              'Access-Control-Allow-Origin': '*',
              'Content-Length': redirRes.headers['content-length']
            });
          }
          redirRes.pipe(res);
        });
        redirReq.on('error', (e) => {
          console.error('[youtube/stream] Redirect proxy error:', e.message);
          if (!res.headersSent) res.status(502).end();
        });
        redirReq.end();
        return;
      }

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
      if (!res.headersSent) res.status(502).json({ error: 'Proxy error' });
    });

    proxyReq.end();

  } catch (e) {
    console.error('[youtube/stream]', e.message);
    if (!res.headersSent) res.status(500).json({ error: e.message });
  }
});

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

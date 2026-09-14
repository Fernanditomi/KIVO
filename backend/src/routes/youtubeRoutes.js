import { Router } from 'express';
import https from 'https';
import http from 'http';
import { URL } from 'url';

const router = Router();

function fetchPage(fetchUrl, headers = {}, method = 'GET') {
  return new Promise((resolve, reject) => {
    const parsed = new URL(fetchUrl);
    const mod = parsed.protocol === 'https:' ? https : http;
    const options = {
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      method,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept-Language': 'es;q=0.9,en;q=0.8',
        ...headers
      }
    };
    const req = mod.request(options, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return fetchPage(res.headers.location, headers, method).then(resolve, reject);
      }
      resolve(res);
    });
    req.on('error', reject);
    if (method === 'POST') req.write(headers._body || '');
    req.end();
  });
}

function fetchBody(url, headers) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const mod = parsed.protocol === 'https:' ? https : http;
    const options = {
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        ...headers
      }
    };
    mod.get(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve(data));
    }).on('error', reject);
  });
}

function innertubePlayer(videoId) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({
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
    });

    const req = https.request({
      hostname: 'www.youtube.com',
      path: '/youtubei/v1/player?key=AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8&prettyPrint=false',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'com.google.android.apps.youtube.vr/1.60.19 (Linux; U; Android 14) gzip',
        'Content-Length': Buffer.byteLength(body)
      }
    }, res => {
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); }
        catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

// Proxy stream endpoint: downloads audio and pipes it to the client
router.get('/stream', async (req, res) => {
  const videoId = req.query.videoId;
  if (!videoId) return res.status(400).json({ error: 'Missing videoId' });

  try {
    console.log(`[youtube/stream] Getting stream for ${videoId}`);

    const playerData = await innertubePlayer(videoId);
    const status = playerData.playabilityStatus?.status;
    console.log(`[youtube/stream] Playability: ${status}`);

    if (status !== 'OK') {
      return res.status(503).json({ error: playerData.playabilityStatus?.reason || 'Unplayable' });
    }

    const sd = playerData.streamingData;
    if (!sd) return res.status(404).json({ error: 'No streaming data' });

    // Find audio URL
    let audioUrl = null;

    const af = sd.adaptiveFormats || [];
    for (const fmt of af) {
      if ((fmt.mimeType || '').includes('audio') && fmt.url) {
        audioUrl = fmt.url;
        break;
      }
    }

    if (!audioUrl && sd.formats?.length > 0 && sd.formats[0].url) {
      audioUrl = sd.formats[0].url;
    }

    if (!audioUrl) {
      return res.status(404).json({ error: 'No audio URL found' });
    }

    console.log(`[youtube/stream] Proxying audio stream...`);

    // Proxy the audio stream
    const parsed = new URL(audioUrl);
    const proxyReq = https.request({
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      method: 'GET',
      headers: {
        'User-Agent': 'com.google.android.apps.youtube.vr/1.60.19 (Linux; U; Android 14) gzip',
        'Referer': 'https://www.youtube.com/',
        'Origin': 'https://www.youtube.com'
      }
    }, proxyRes => {
      console.log(`[youtube/stream] Upstream response: ${proxyRes.statusCode}`);
      if (proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
        // Follow redirect
        const loc = proxyRes.headers.location;
        const parsed2 = new URL(loc);
        const redirectReq = https.request({
          hostname: parsed2.hostname,
          path: parsed2.pathname + parsed2.search,
          method: 'GET',
          headers: {
            'User-Agent': 'com.google.android.apps.youtube.vr/1.60.19 (Linux; U; Android 14) gzip',
            'Referer': 'https://www.youtube.com/',
            'Origin': 'https://www.youtube.com'
          }
        }, redirectRes => {
          res.writeHead(redirectRes.statusCode, {
            'Content-Type': redirectRes.headers['content-type'] || 'audio/mp4',
            'Access-Control-Allow-Origin': '*',
            'Content-Length': redirectRes.headers['content-length']
          });
          redirectRes.pipe(res);
        });
        redirectReq.end();
        return;
      }

      res.writeHead(proxyRes.statusCode, {
        'Content-Type': proxyRes.headers['content-type'] || 'audio/mp4',
        'Access-Control-Allow-Origin': '*',
        'Content-Length': proxyRes.headers['content-length']
      });
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
    const html = await fetchBody(searchUrl);
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

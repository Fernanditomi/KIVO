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

function httpsPost(url, body, headers) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const bodyStr = JSON.stringify(body);
    const req = https.request({
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
      res.on('end', () => resolve({ status: res.statusCode, body: d }));
    });
    req.on('error', reject);
    req.write(bodyStr);
    req.end();
  });
}

async function getPlayerData(videoId) {
  // Method 1: Try watch page scraping (works for most videos)
  try {
    console.log(`[youtube] Trying watch page scraping for ${videoId}`);
    const resp = await httpsGet(`https://www.youtube.com/watch?v=${videoId}`);
    const html = resp.body;

    const marker = 'var ytInitialPlayerResponse = ';
    const startIdx = html.indexOf(marker);
    if (startIdx !== -1) {
      const jsonStart = startIdx + marker.length;
      const jsonEnd = html.indexOf('};', jsonStart);
      if (jsonEnd !== -1) {
        const json = JSON.parse(html.substring(jsonStart, jsonEnd + 1));
        const status = json.playabilityStatus?.status;
        console.log(`[youtube] Watch page status: ${status}`);
        if (status === 'OK' && json.streamingData) return json.streamingData;
      }
    }
  } catch (e) {
    console.error('[youtube] Watch page error:', e.message);
  }

  // Method 2: Try ANDROID_VR InnerTube
  try {
    console.log(`[youtube] Trying ANDROID_VR InnerTube for ${videoId}`);
    const resp = await httpsPost(
      'https://www.youtube.com/youtubei/v1/player?key=AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8&prettyPrint=false',
      {
        videoId,
        context: { client: { clientName: 'ANDROID_VR', clientVersion: '1.60.19', androidSdkVersion: 34, hl: 'es', gl: 'US' } },
        contentCheckOk: true, racyCheckOk: true
      },
      { 'User-Agent': 'com.google.android.apps.youtube.vr/1.60.19 (Linux; U; Android 14) gzip' }
    );
    const json = JSON.parse(resp.body);
    console.log(`[youtube] ANDROID_VR status: ${json.playabilityStatus?.status}`);
    if (json.playabilityStatus?.status === 'OK' && json.streamingData) return json.streamingData;
  } catch (e) {
    console.error('[youtube] ANDROID_VR error:', e.message);
  }

  // Method 3: Try WEB InnerTube with visitor data
  try {
    console.log(`[youtube] Trying WEB InnerTube for ${videoId}`);
    const home = await httpsGet('https://www.youtube.com/');
    const vdMatch = home.body.match(/"VISITOR_DATA":"([^"]+)"/);
    const cookies = (home.headers['set-cookie'] || []).map(c => c.split(';')[0]).join('; ');
    const allCookies = (cookies ? cookies + '; ' : '') + 'CONSENT=YES+cb.20210328-17-p0.en+FX+999';

    const resp = await httpsPost(
      'https://www.youtube.com/youtubei/v1/player?prettyPrint=false',
      {
        videoId,
        context: { client: { clientName: 'WEB', clientVersion: '2.20241126.01.00', hl: 'es', gl: 'US', visitorData: vdMatch ? vdMatch[1] : '' } },
        contentCheckOk: true, racyCheckOk: true
      },
      {
        'Origin': 'https://www.youtube.com',
        'Referer': `https://www.youtube.com/watch?v=${videoId}`,
        'Cookie': allCookies,
        'X-Youtube-Client-Name': '1',
        'X-Youtube-Client-Version': '2.20241126.01.00'
      }
    );
    const json = JSON.parse(resp.body);
    console.log(`[youtube] WEB status: ${json.playabilityStatus?.status}`);
    if (json.playabilityStatus?.status === 'OK' && json.streamingData) return json.streamingData;
  } catch (e) {
    console.error('[youtube] WEB error:', e.message);
  }

  return null;
}

// Stream endpoint: proxies audio to the client
router.get('/stream', async (req, res) => {
  const videoId = req.query.videoId;
  if (!videoId) return res.status(400).json({ error: 'Missing videoId' });

  try {
    console.log(`[youtube/stream] Getting stream for ${videoId}`);

    const sd = await getPlayerData(videoId);
    if (!sd) return res.status(503).json({ error: 'Could not get streaming data' });

    // Find audio URL
    let audioUrl = null;
    const af = sd.adaptiveFormats || [];
    for (const fmt of af) {
      if ((fmt.mimeType || '').includes('audio')) {
        if (fmt.url) { audioUrl = fmt.url; break; }
        if (fmt.signatureCipher) {
          console.log(`[youtube/stream] Audio format has signatureCipher (not decryptable)`);
        }
      }
    }
    if (!audioUrl && sd.formats?.[0]?.url) audioUrl = sd.formats[0].url;
    if (!audioUrl && sd.serverAbrStreamingUrl) audioUrl = sd.serverAbrStreamingUrl;

    if (!audioUrl) return res.status(404).json({ error: 'No audio URL found' });

    console.log(`[youtube/stream] Proxying audio stream...`);

    // Proxy with follow redirects
    const doProxy = (url, depth = 0) => {
      if (depth > 5) return res.status(502).json({ error: 'Too many redirects' });
      const parsed = new URL(url);
      const mod = parsed.protocol === 'https:' ? https : http;
      const proxyReq = mod.get({
        hostname: parsed.hostname,
        path: parsed.pathname + parsed.search,
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
          'Referer': 'https://www.youtube.com/',
          'Origin': 'https://www.youtube.com'
        }
      }, proxyRes => {
        if (proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
          const loc = proxyRes.headers.location.startsWith('http')
            ? proxyRes.headers.location
            : `https://${parsed.hostname}${proxyRes.headers.location}`;
          return doProxy(loc, depth + 1);
        }
        console.log(`[youtube/stream] Upstream: ${proxyRes.statusCode} CT: ${proxyRes.headers['content-type']}`);
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
    };

    doProxy(audioUrl);

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

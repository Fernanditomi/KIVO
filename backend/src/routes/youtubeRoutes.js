import { Router } from 'express';
import https from 'https';
import http from 'http';
import { URL } from 'url';
import ytdl from '@distube/ytdl-core';

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

// Stream endpoint using ytdl-core
router.get('/stream', async (req, res) => {
  const videoId = req.query.videoId;
  if (!videoId) return res.status(400).json({ error: 'Missing videoId' });

  try {
    console.log(`[youtube/stream] Getting stream for ${videoId} via ytdl-core`);

    const info = await ytdl.getInfo(videoId);
    console.log(`[youtube/stream] Got info: ${info.videoDetails.title}`);

    const audioFormats = ytdl.filterFormats(info.formats, 'audioonly');
    if (audioFormats.length === 0) {
      return res.status(404).json({ error: 'No audio formats found' });
    }

    const format = audioFormats[0];
    console.log(`[youtube/stream] Using format: ${format.mimeType} bitrate=${format.averageBitrate}`);

    const stream = ytdl(videoId, { filter: 'audioonly', quality: 'highestaudio' });

    res.writeHead(200, {
      'Content-Type': format.mimeType || 'audio/webm',
      'Access-Control-Allow-Origin': '*'
    });

    stream.pipe(res);

    stream.on('error', (e) => {
      console.error('[youtube/stream] Stream error:', e.message);
      if (!res.headersSent) res.status(500).json({ error: 'Stream error' });
    });

  } catch (e) {
    console.error('[youtube/stream] Error:', e.message);
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

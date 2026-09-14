import { Router } from 'express';
import https from 'https';
import http from 'http';
import { URL } from 'url';

const router = Router();

function fetchPage(fetchUrl, headers = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(fetchUrl);
    const mod = parsed.protocol === 'https:' ? https : http;
    const options = {
      hostname: parsed.hostname,
      path: parsed.pathname + parsed.search,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept-Language': 'es;q=0.9,en;q=0.8',
        ...headers
      }
    };
    mod.get(options, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return fetchPage(res.headers.location, headers).then(resolve, reject);
      }
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve(data));
    }).on('error', reject);
  });
}

function decodeSearchResults(html) {
  const results = [];
  const dataStart = html.indexOf('var ytInitialData = ');
  if (dataStart === -1) return results;
  const jsonStart = dataStart + 'var ytInitialData = '.length;
  const jsonEnd = html.indexOf(';</script>', jsonStart);
  if (jsonEnd === -1) return results;

  try {
    const json = JSON.parse(html.substring(jsonStart, jsonEnd));
    const contents = json.contents?.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer?.contents;
    if (!contents) return results;

    for (const section of contents) {
      const items = section.itemSectionRenderer?.contents;
      if (!items) continue;
      for (const item of items) {
        const vr = item.videoRenderer;
        if (!vr) continue;
        const videoId = vr.videoId;
        if (!videoId) continue;
        const title = vr.title?.runs?.[0]?.text || '';
        const channel = vr.ownerText?.runs?.[0]?.text || '';
        const thumbs = vr.thumbnail?.thumbnails;
        const thumb = thumbs?.[thumbs.length - 1]?.url || '';
        const dur = vr.lengthText?.simpleText || '0:00';
        results.push({ videoId, title, channel, thumbnail: thumb, duration: dur });
        if (results.length >= 15) return results;
      }
    }
  } catch (e) {
    console.error('[youtube] Parse error:', e.message);
  }
  return results;
}

router.get('/search', async (req, res) => {
  const query = req.query.q;
  if (!query) return res.status(400).json({ error: 'Missing q parameter' });

  try {
    const searchUrl = `https://www.youtube.com/results?search_query=${encodeURIComponent(query)}&sp=EgIQAQ%3D%3D`;
    const html = await fetchPage(searchUrl);
    const results = decodeSearchResults(html);
    res.json({ results });
  } catch (e) {
    console.error('[youtube/search]', e.message);
    res.status(500).json({ error: e.message });
  }
});

router.get('/stream', async (req, res) => {
  const videoId = req.query.videoId;
  if (!videoId) return res.status(400).json({ error: 'Missing videoId parameter' });

  try {
    const watchUrl = `https://www.youtube.com/watch?v=${videoId}`;
    const html = await fetchPage(watchUrl);

    const playerMatch = html.match(/var ytInitialPlayerResponse\s*=\s*(\{.*?\});/);
    if (!playerMatch) return res.status(404).json({ error: 'No player data found' });

    const json = JSON.parse(playerMatch[1]);
    const sd = json.streamingData;
    if (!sd) return res.status(404).json({ error: 'No streaming data' });

    // Try adaptive formats
    const af = sd.adaptiveFormats || [];
    const audioFormats = af.filter(f => (f.mimeType || '').includes('audio'));

    for (const fmt of audioFormats) {
      if (fmt.url) return res.json({ url: fmt.url });
    }

    // Try regular formats
    const formats = sd.formats || [];
    for (const fmt of formats) {
      if (fmt.url) return res.json({ url: fmt.url });
    }

    // Try ABR streaming URL
    if (sd.serverAbrStreamingUrl) return res.json({ url: sd.serverAbrStreamingUrl });

    res.status(404).json({ error: 'No audio stream found (signatureCipher protection)' });
  } catch (e) {
    console.error('[youtube/stream]', e.message);
    res.status(500).json({ error: e.message });
  }
});

export default router;

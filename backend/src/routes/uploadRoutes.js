import { Router } from 'express';
import { randomUUID } from 'crypto';
import fs from 'fs';
import path from 'path';
import { requireAuth } from '../auth.js';

const UPLOADS_DIR = path.join(process.cwd(), 'uploads');

export function ensureUploadsDir() {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

const MIME_EXT = {
  'image/jpeg': '.jpg',
  'image/png': '.png',
  'image/webp': '.webp',
  'image/gif': '.gif',
  'audio/aac': '.aac',
  'audio/mp4': '.m4a',
  'audio/m4a': '.m4a',
  'audio/mpeg': '.mp3',
  'audio/wav': '.wav',
  'audio/ogg': '.ogg'
};

const router = Router();
router.use(requireAuth);

router.post('/', async (req, res) => {
  try {
    const { base64 } = req.body || {};
    if (!base64 || typeof base64 !== 'string') {
      return res.status(400).json({ error: 'Falta el archivo' });
    }

    const mimeMatch = /^data:([^;]+);base64,(.+)$/.exec(base64);
    const mime = mimeMatch ? mimeMatch[1] : 'image/jpeg';
    const data = mimeMatch
      ? Buffer.from(mimeMatch[2], 'base64')
      : Buffer.from(base64, 'base64');

    const ext = MIME_EXT[mime.toLowerCase()];
    if (!ext) return res.status(400).json({ error: 'Formato no soportado' });
    if (data.length === 0) return res.status(400).json({ error: 'Archivo vacio' });
    if (data.length > 20 * 1024 * 1024) return res.status(413).json({ error: 'Archivo demasiado grande' });

    ensureUploadsDir();
    const name = `${randomUUID()}${ext}`;
    fs.writeFileSync(path.join(UPLOADS_DIR, name), data);

    res.status(201).json({ url: `/uploads/${name}` });
  } catch (e) {
    console.error('[uploads]', e);
    res.status(500).json({ error: 'Error al subir la imagen' });
  }
});

export default router;
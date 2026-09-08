import { Router } from 'express';
import { pool, mapUser } from '../db.js';
import { requireAuth } from '../auth.js';

const router = Router();

router.get('/check', async (req, res) => {
  try {
    const username = (req.query.username || '').toString().trim().toLowerCase();
    if (!username) return res.status(400).json({ error: 'Falta el username' });
    const { rowCount } = await pool.query(
      'SELECT 1 FROM users WHERE username_lowercase = $1',
      [username]
    );
    res.json({ available: rowCount === 0 });
  } catch (e) {
    console.error('[check]', e);
    res.status(500).json({ error: 'Error al verificar usuario' });
  }
});

router.use(requireAuth);

router.get('/me', (req, res) => {
  res.json(req.user);
});

router.get('/search', async (req, res) => {
  try {
    const q = (req.query.q || '').toString().trim().toLowerCase();
    if (!q) return res.json([]);

    const { rows } = await pool.query(
      `SELECT * FROM users
       WHERE username_lowercase LIKE $1 OR display_name ILIKE $2
       ORDER BY is_online DESC, display_name ASC
       LIMIT 10`,
      [q + '%', '%' + q + '%']
    );
    res.json(rows.map(mapUser));
  } catch (e) {
    console.error('[search]', e);
    res.status(500).json({ error: 'Error en la busqueda' });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT * FROM users WHERE user_id = $1', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ error: 'Usuario no encontrado' });
    res.json(mapUser(rows[0]));
  } catch (e) {
    console.error('[user]', e);
    res.status(500).json({ error: 'Error al obtener usuario' });
  }
});

router.put('/:id', async (req, res) => {
  try {
    const targetId = req.params.id;
    if (targetId !== req.user.userId) {
      return res.status(403).json({ error: 'Solo puedes editar tu propio perfil' });
    }

    const { username, displayName, bio, photoUrl } = req.body || {};

    const current = await pool.query('SELECT * FROM users WHERE user_id = $1', [targetId]);
    if (current.rowCount === 0) return res.status(404).json({ error: 'Usuario no encontrado' });

    const usernameLowercase = username
      ? String(username).trim().toLowerCase()
      : current.rows[0].username_lowercase;

    if (username) {
      const dup = await pool.query(
        'SELECT 1 FROM users WHERE username_lowercase = $1 AND user_id <> $2',
        [usernameLowercase, targetId]
      );
      if (dup.rowCount > 0) return res.status(409).json({ error: 'Ese usuario ya existe' });
    }

    const { rows } = await pool.query(
      `UPDATE users SET
         username = COALESCE($2, username),
         username_lowercase = $3,
         display_name = COALESCE($4, display_name),
         bio = COALESCE($5, bio),
         photo_url = COALESCE($6, photo_url),
         last_seen = $7
       WHERE user_id = $1
       RETURNING *`,
      [
        targetId,
        username ? String(username).trim() : null,
        usernameLowercase,
        displayName ? String(displayName).trim() : null,
        bio !== undefined ? String(bio) : null,
        photoUrl !== undefined ? String(photoUrl) : null,
        Date.now()
      ]
    );

    res.json(mapUser(rows[0]));
  } catch (e) {
    console.error('[updateProfile]', e);
    res.status(500).json({ error: 'Error al actualizar perfil' });
  }
});

router.put('/:id/presence', async (req, res) => {
  try {
    const targetId = req.params.id;
    if (targetId !== req.user.userId) {
      return res.status(403).json({ error: 'Solo puedes cambiar tu propia presencia' });
    }

    const isOnline = Boolean(req.body?.isOnline);
    const { rows } = await pool.query(
      'UPDATE users SET is_online = $2, last_seen = $3 WHERE user_id = $1 RETURNING *',
      [targetId, isOnline, Date.now()]
    );
    if (rows.length === 0) return res.status(404).json({ error: 'Usuario no encontrado' });

    const io = req.app.get('io');
    if (io) io.emit('presence', { userId: targetId, isOnline });

    res.json(mapUser(rows[0]));
  } catch (e) {
    console.error('[presence]', e);
    res.status(500).json({ error: 'Error al actualizar presencia' });
  }
});

export default router;
import { Router } from 'express';
import { pool } from '../db.js';
import { requireAuth } from '../auth.js';

const router = Router();
router.use(requireAuth);

router.post('/', async (req, res) => {
  try {
    const { onesignalPlayerId, platform = 'android' } = req.body || {};
    if (!onesignalPlayerId) {
      return res.status(400).json({ error: 'Falta el id del dispositivo (OneSignal)' });
    }

    const updatedAt = Date.now();
    const deviceId = req.user.userId;

    await pool.query(
      `INSERT INTO devices (device_id, user_id, onesignal_player_id, platform, updated_at)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (device_id) DO UPDATE SET
         onesignal_player_id = EXCLUDED.onesignal_player_id,
         platform = EXCLUDED.platform,
         updated_at = EXCLUDED.updated_at`,
      [deviceId, req.user.userId, String(onesignalPlayerId), String(platform), updatedAt]
    );

    res.status(201).json({ deviceId, userId: req.user.userId, onesignalPlayerId, platform, updatedAt });
  } catch (e) {
    console.error('[device]', e);
    res.status(500).json({ error: 'Error al registrar dispositivo' });
  }
});

export default router;
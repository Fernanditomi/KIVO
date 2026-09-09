import { Router } from 'express';
import { pool, mapUser, mapConversation, mapMessage, genId } from '../db.js';
import { requireAuth } from '../auth.js';
import { sendPush } from '../push.js';

const router = Router();
router.use(requireAuth);

async function attachUnreadCounts(conversations, userId) {
  if (conversations.length === 0) return conversations;
  const ids = conversations.map((c) => c.conversationId);
  const { rows } = await pool.query(
    `SELECT conversation_id, COUNT(*)::int AS count
     FROM messages
     WHERE conversation_id = ANY($1) AND receiver_id = $2 AND is_read = FALSE
     GROUP BY conversation_id`,
    [ids, userId]
  );
  const counts = new Map(rows.map((r) => [r.conversation_id, Number(r.count)]));
  return conversations.map((c) => ({
    ...c,
    unreadCount: counts.has(c.conversationId) ? { [userId]: counts.get(c.conversationId) } : {}
  }));
}

router.get('/', async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM conversations WHERE $1 = ANY(participants) ORDER BY updated_at DESC',
      [req.user.userId]
    );
    const conversations = await attachUnreadCounts(rows.map(mapConversation), req.user.userId);
    res.json(conversations);
  } catch (e) {
    console.error('[conversations]', e);
    res.status(500).json({ error: 'Error al listar conversaciones' });
  }
});

router.post('/', async (req, res) => {
  try {
    const { otherId } = req.body || {};
    if (!otherId) return res.status(400).json({ error: 'Falta el otro usuario' });
    if (otherId === req.user.userId) return res.status(400).json({ error: 'No puedes chatear contigo' });

    const other = await pool.query('SELECT 1 FROM users WHERE user_id = $1', [otherId]);
    if (other.rowCount === 0) return res.status(404).json({ error: 'Usuario no encontrado' });

    const participants = [req.user.userId, otherId].sort();
    const existing = await pool.query(
      'SELECT * FROM conversations WHERE participants = $1',
      [participants]
    );

    if (existing.rowCount > 0) {
      return res.json(mapConversation(existing.rows[0]));
    }

    const now = Date.now();
    const conversationId = genId();
    const { rows } = await pool.query(
      `INSERT INTO conversations (conversation_id, participants, created_at, updated_at, last_message, last_message_at, last_message_sender_id)
       VALUES ($1, $2, $3, $4, '', $5, '')
       RETURNING *`,
      [conversationId, participants, now, now, now]
    );

    res.status(201).json(mapConversation(rows[0]));
  } catch (e) {
    console.error('[createConversation]', e);
    res.status(500).json({ error: 'Error al crear conversacion' });
  }
});

async function assertParticipant(conversationId, userId) {
  const { rows } = await pool.query(
    'SELECT * FROM conversations WHERE conversation_id = $1 AND $2 = ANY(participants)',
    [conversationId, userId]
  );
  return rows[0] || null;
}

router.get('/:id/messages', async (req, res) => {
  try {
    const conversation = await assertParticipant(req.params.id, req.user.userId);
    if (!conversation) return res.status(404).json({ error: 'Conversacion no encontrada' });

    const { rows } = await pool.query(
      'SELECT * FROM messages WHERE conversation_id = $1 ORDER BY created_at ASC',
      [req.params.id]
    );
    res.json(rows.map(mapMessage));
  } catch (e) {
    console.error('[messages]', e);
    res.status(500).json({ error: 'Error al obtener mensajes' });
  }
});

router.post('/:id/messages', async (req, res) => {
  try {
    const conversation = await assertParticipant(req.params.id, req.user.userId);
    if (!conversation) return res.status(404).json({ error: 'Conversacion no encontrada' });

    const { text, receiverId, type = 'text' } = req.body || {};
    const isImage = type === 'image';
    const isAudio = type === 'audio';
    if (!isImage && !isAudio && (!text || !String(text).trim())) return res.status(400).json({ error: 'El mensaje no puede estar vacio' });
    if (!receiverId || !conversation.participants.includes(receiverId)) {
      return res.status(400).json({ error: 'Destinatario invalido' });
    }

    const now = Date.now();
    const messageId = genId();
    const storedText = (isImage || isAudio) ? String(text || '').trim() : String(text).trim();
    const displayText = isImage ? '📷 Foto' : isAudio ? '🎤 Audio' : storedText;
    const { rows } = await pool.query(
      `INSERT INTO messages (message_id, conversation_id, sender_id, receiver_id, text, created_at, type, is_read, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, FALSE, 'sent')
       RETURNING *`,
      [messageId, req.params.id, req.user.userId, receiverId, storedText, now, type]
    );

    const { rows: updated } = await pool.query(
      `UPDATE conversations SET last_message = $2, last_message_at = $3, last_message_sender_id = $4, updated_at = $3
       WHERE conversation_id = $1 RETURNING *`,
      [req.params.id, displayText, now, req.user.userId]
    );

    const message = mapMessage(rows[0]);

    const sender = await pool.query('SELECT display_name FROM users WHERE user_id = $1', [req.user.userId]);
    const senderName = sender.rows[0]?.display_name || 'Alguien';

    const io = req.app.get('io');
    if (io) {
      io.to(`conversation:${req.params.id}`).emit('new_message', { ...message, senderName });
      io.emit('conversation_updated', mapConversation(updated[0]));
    }

    await sendPush(receiverId, 'KIVO', `${senderName}: ${displayText}`);

    res.status(201).json(message);
  } catch (e) {
    console.error('[sendMessage]', e);
    res.status(500).json({ error: 'Error al enviar mensaje' });
  }
});

router.post('/:id/read', async (req, res) => {
  try {
    const conversation = await assertParticipant(req.params.id, req.user.userId);
    if (!conversation) return res.status(404).json({ error: 'Conversacion no encontrada' });

    const { rowCount } = await pool.query(
      `UPDATE messages SET is_read = TRUE, status = 'read'
       WHERE conversation_id = $1 AND receiver_id = $2 AND is_read = FALSE`,
      [req.params.id, req.user.userId]
    );

    const io = req.app.get('io');
    if (io) {
      io.to(`conversation:${req.params.id}`).emit('messages_read', {
        conversationId: req.params.id,
        readerId: req.user.userId
      });
      io.emit('conversation_updated', mapConversation(conversation));
    }

    res.json({ updated: rowCount });
  } catch (e) {
    console.error('[read]', e);
    res.status(500).json({ error: 'Error al marcar mensajes como leidos' });
  }
});

router.delete('/:id/messages', async (req, res) => {
  try {
    const conversation = await assertParticipant(req.params.id, req.user.userId);
    if (!conversation) return res.status(404).json({ error: 'Conversacion no encontrada' });

    const { rowCount } = await pool.query(
      'DELETE FROM messages WHERE conversation_id = $1',
      [req.params.id]
    );
    const now = Date.now();
    const { rows: updated } = await pool.query(
      `UPDATE conversations SET last_message = '', last_message_at = $2, updated_at = $2
       WHERE conversation_id = $1 RETURNING *`,
      [req.params.id, now]
    );

    const io = req.app.get('io');
    if (io) io.emit('conversation_updated', mapConversation(updated[0]));

    res.json({ cleared: rowCount });
  } catch (e) {
    console.error('[clearMessages]', e);
    res.status(500).json({ error: 'Error al vaciar la conversacion' });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    const conversation = await assertParticipant(req.params.id, req.user.userId);
    if (!conversation) return res.status(404).json({ error: 'Conversacion no encontrada' });

    await pool.query('DELETE FROM messages WHERE conversation_id = $1', [req.params.id]);
    await pool.query('DELETE FROM conversations WHERE conversation_id = $1', [req.params.id]);

    const io = req.app.get('io');
    if (io) io.emit('conversation_updated', mapConversation(conversation));

    res.json({ deleted: 1 });
  } catch (e) {
    console.error('[deleteConversation]', e);
    res.status(500).json({ error: 'Error al eliminar la conversacion' });
  }
});

export default router;
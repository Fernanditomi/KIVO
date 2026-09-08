import { getAuth } from '@clerk/express';
import { clerkConfigured, ensureUser } from './clerk.js';

export async function requireAuth(req, res, next) {
  if (!clerkConfigured) {
    return res.status(503).json({ error: 'Clerk no configurado en el backend' });
  }
  const auth = getAuth(req);
  if (!auth?.userId) {
    return res.status(401).json({ error: 'Token no proporcionado o invalido' });
  }
  try {
    const user = await ensureUser(auth.userId);
    if (!user) {
      return res.status(500).json({ error: 'No se pudo sincronizar el usuario' });
    }
    req.user = user;
    next();
  } catch (e) {
    console.error('[requireAuth]', e);
    next(e);
  }
}
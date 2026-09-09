import { Server } from 'socket.io';
import { verifyToken } from '@clerk/backend';
import { CLERK_SECRET_KEY, CLERK_PUBLISHABLE_KEY } from './config.js';
import { clerkConfigured, ensureUser } from './clerk.js';
import { pool } from './db.js';

export function initSocket(httpServer) {
  const io = new Server(httpServer, {
    cors: { origin: '*' }
  });

  io.use(async (socket, next) => {
    const token = socket.handshake.auth?.token;
    if (!token) return next(new Error('Token no proporcionado'));
    if (!clerkConfigured) return next(new Error('Clerk no configurado en el backend'));
    try {
      const payload = await verifyToken(token, {
        secretKey: CLERK_SECRET_KEY,
        publishableKey: CLERK_PUBLISHABLE_KEY
      });
      const userId = payload.sub;
      const user = await ensureUser(userId);
      if (!user) return next(new Error('Usuario no sincronizado'));
      socket.userId = userId;
      next();
    } catch {
      next(new Error('Token invalido'));
    }
  });

  io.on('connection', async (socket) => {
    const userId = socket.userId;

    await pool.query('UPDATE users SET is_online = TRUE WHERE user_id = $1', [userId]);
    socket.broadcast.emit('presence', { userId, isOnline: true });

    socket.on('conversation:join', (conversationId) => {
      if (conversationId) socket.join(`conversation:${conversationId}`);
    });

    socket.on('conversation:leave', (conversationId) => {
      if (conversationId) socket.leave(`conversation:${conversationId}`);
    });

    socket.on('call:offer', (data) => {
      const { receiverId } = data;
      if (receiverId) {
        socket.to(`user:${receiverId}`).emit('call:offer', { ...data, callerId: userId });
      }
    });

    socket.on('call:answer', (data) => {
      const { callerId } = data;
      if (callerId) {
        socket.to(`user:${callerId}`).emit('call:answer', { ...data, receiverId: userId });
      }
    });

    socket.on('call:reject', (data) => {
      const { callerId } = data;
      if (callerId) {
        socket.to(`user:${callerId}`).emit('call:reject', { ...data, receiverId: userId });
      }
    });

    socket.on('call:end', (data) => {
      const { callerId } = data;
      if (callerId) {
        socket.to(`user:${callerId}`).emit('call:end', { ...data, receiverId: userId });
      }
    });

    socket.on('call:ice-candidate', (data) => {
      const { targetId } = data;
      if (targetId) {
        socket.to(`user:${targetId}`).emit('call:ice-candidate', { ...data, senderId: userId });
      }
    });

    socket.on('call:sdp-offer', (data) => {
      const { targetId } = data;
      if (targetId) {
        socket.to(`user:${targetId}`).emit('call:sdp-offer', { ...data, senderId: userId });
      }
    });

    socket.on('call:sdp-answer', (data) => {
      const { targetId } = data;
      if (targetId) {
        socket.to(`user:${targetId}`).emit('call:sdp-answer', { ...data, senderId: userId });
      }
    });

    socket.join(`user:${userId}`);

    socket.on('disconnect', async () => {
      await pool.query('UPDATE users SET is_online = FALSE, last_seen = $2 WHERE user_id = $1', [userId, Date.now()]);
      socket.broadcast.emit('presence', { userId, isOnline: false });
    });
  });

  return io;
}
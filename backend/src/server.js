import express from 'express';
import http from 'http';
import cors from 'cors';
import path from 'path';
import { clerkMiddleware } from '@clerk/express';
import { PORT, CLERK_SECRET_KEY, CLERK_PUBLISHABLE_KEY } from './config.js';
import { clerkConfigured } from './clerk.js';
import { initDb } from './db.js';
import { initSocket } from './socket.js';
import userRoutes from './routes/userRoutes.js';
import conversationRoutes from './routes/conversationRoutes.js';
import deviceRoutes from './routes/deviceRoutes.js';
import uploadRoutes, { ensureUploadsDir } from './routes/uploadRoutes.js';

const app = express();
app.use(cors());
app.use(express.json({ limit: '25mb' }));

ensureUploadsDir();
app.use('/uploads', express.static(path.join(process.cwd(), 'uploads')));

if (clerkConfigured) {
  app.use(clerkMiddleware({ publishableKey: CLERK_PUBLISHABLE_KEY, secretKey: CLERK_SECRET_KEY }));
} else {
  console.warn('[clerk] Sin CLERK_SECRET_KEY/CLERK_PUBLISHABLE_KEY: auth deshabilitada');
}

app.get('/health', (req, res) => res.json({ ok: true, service: 'kivo-backend', clerk: clerkConfigured, time: Date.now() }));

app.use('/api/users', userRoutes);
app.use('/api/conversations', conversationRoutes);
app.use('/api/devices', deviceRoutes);
app.use('/api/uploads', uploadRoutes);

const server = http.createServer(app);
const io = initSocket(server);
app.set('io', io);

async function start() {
  await initDb();
  server.listen(PORT, () => {
    console.log(`KIVO backend corriendo en http://0.0.0.0:${PORT}`);
  });
}

start().catch((e) => {
  console.error('[fatal]', e);
  process.exit(1);
});
import pg from 'pg';
import { randomUUID } from 'crypto';
import { DATABASE_URL } from './config.js';

export const pool = new pg.Pool({ connectionString: DATABASE_URL });

const SCHEMA = `
CREATE TABLE IF NOT EXISTS users (
  user_id             TEXT PRIMARY KEY,
  email               TEXT UNIQUE NOT NULL,
  username            TEXT UNIQUE NOT NULL,
  username_lowercase  TEXT UNIQUE NOT NULL,
  display_name        TEXT NOT NULL,
  photo_url           TEXT,
  bio                 TEXT DEFAULT '',
  password_hash       TEXT NOT NULL,
  created_at          BIGINT NOT NULL,
  last_seen           BIGINT NOT NULL,
  is_online           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS conversations (
  conversation_id       TEXT PRIMARY KEY,
  participants          TEXT[] NOT NULL,
  created_at            BIGINT NOT NULL,
  updated_at            BIGINT NOT NULL,
  last_message          TEXT DEFAULT '',
  last_message_at       BIGINT NOT NULL,
  last_message_sender_id TEXT DEFAULT ''
);

CREATE TABLE IF NOT EXISTS messages (
  message_id      TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL REFERENCES conversations(conversation_id) ON DELETE CASCADE,
  sender_id       TEXT NOT NULL,
  receiver_id     TEXT NOT NULL,
  text            TEXT NOT NULL,
  created_at      BIGINT NOT NULL,
  type            TEXT DEFAULT 'text',
  is_read         BOOLEAN NOT NULL DEFAULT FALSE,
  status          TEXT DEFAULT 'sent'
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_read ON messages (receiver_id, is_read);

CREATE TABLE IF NOT EXISTS devices (
  device_id            TEXT PRIMARY KEY,
  user_id              TEXT NOT NULL,
  onesignal_player_id  TEXT NOT NULL,
  platform             TEXT DEFAULT 'android',
  updated_at           BIGINT NOT NULL
);
`;

// Migracion: los usuarios ahora se autentican con Clerk, no guardan password.
const MIGRATIONS = [
  'ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL'
];

const DEMO_USERS = [
  { userId: 'uid_daniela', username: 'daniela', displayName: 'Daniela', email: 'daniela@kivo.com', bio: 'Amo la música' },
  { userId: 'uid_rose', username: 'rose', displayName: 'Rose', email: 'rose@kivo.com', bio: 'Kivo es genial' },
  { userId: 'uid_reinaldo', username: 'reinaldo', displayName: 'Reinaldo', email: 'reinaldo@kivo.com', bio: 'Explorador Kivo' },
  { userId: 'uid_fernando', username: 'fernando', displayName: 'Fernando', email: 'fernando@kivo.com', bio: 'Creador de Kivo' },
  { userId: 'uid_hernesto', username: 'hernesto', displayName: 'Hernesto', email: 'hernesto@kivo.com', bio: 'Melómano' }
];

export async function initDb() {
  await pool.query(SCHEMA);
  for (const stmt of MIGRATIONS) {
    await pool.query(stmt);
  }
  await seedDemoUsers();
}

async function seedDemoUsers() {
  const { rows } = await pool.query('SELECT COUNT(*)::int AS count FROM users');
  if (rows[0].count > 0) return;

  const now = Date.now();
  const demoPasswordHash = '!kivo-demo-no-login';
  for (const u of DEMO_USERS) {
    await pool.query(
      `INSERT INTO users (user_id, email, username, username_lowercase, display_name, photo_url, bio, password_hash, created_at, last_seen, is_online)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, FALSE)`,
      [u.userId, u.email, u.username, u.username.toLowerCase(), u.displayName, null, u.bio, demoPasswordHash, now, now]
    );
  }
  console.log(`[seed] Usuarios demo creados (${DEMO_USERS.length})`);
}

function toNumber(value) {
  return typeof value === 'bigint' ? Number(value) : Number(value);
}

export function mapUser(row) {
  if (!row) return null;
  return {
    userId: row.user_id,
    username: row.username,
    usernameLowercase: row.username_lowercase,
    displayName: row.display_name,
    email: row.email,
    photoUrl: row.photo_url,
    bio: row.bio,
    createdAt: toNumber(row.created_at),
    lastSeen: toNumber(row.last_seen),
    isOnline: row.is_online
  };
}

export function mapConversation(row) {
  if (!row) return null;
  return {
    conversationId: row.conversation_id,
    participants: row.participants,
    createdAt: toNumber(row.created_at),
    updatedAt: toNumber(row.updated_at),
    lastMessage: row.last_message,
    lastMessageAt: toNumber(row.last_message_at),
    lastMessageSenderId: row.last_message_sender_id,
    unreadCount: {}
  };
}

export function mapMessage(row) {
  if (!row) return null;
  return {
    messageId: row.message_id,
    conversationId: row.conversation_id,
    senderId: row.sender_id,
    receiverId: row.receiver_id,
    text: row.text,
    createdAt: toNumber(row.created_at),
    type: row.type,
    isRead: row.is_read,
    status: row.status
  };
}

export const genId = () => randomUUID();
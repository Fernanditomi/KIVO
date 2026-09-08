import { createClerkClient } from '@clerk/backend';
import { pool, mapUser } from './db.js';
import { CLERK_SECRET_KEY, CLERK_PUBLISHABLE_KEY } from './config.js';

export const clerkConfigured = Boolean(CLERK_SECRET_KEY && CLERK_PUBLISHABLE_KEY);

export const clerkClient = clerkConfigured
  ? createClerkClient({ secretKey: CLERK_SECRET_KEY, publishableKey: CLERK_PUBLISHABLE_KEY })
  : null;

function sanitizeBaseName(value) {
  return (value || '')
    .toString()
    .toLowerCase()
    .replace(/[^a-z0-9_]/g, '')
    .slice(0, 20) || 'usuario';
}

async function insertNewUser({ clerkId, email, baseName, displayName, photoUrl, bio }) {
  const now = Date.now();
  for (let attempt = 0; attempt < 10; attempt++) {
    const username = attempt === 0 ? baseName : `${baseName}${attempt}`;
    const lower = username.toLowerCase();
    try {
      const { rows } = await pool.query(
        `INSERT INTO users
           (user_id, email, username, username_lowercase, display_name, photo_url, bio, password_hash, created_at, last_seen, is_online)
         VALUES ($1, $2, $3, $4, $5, $6, $7, NULL, $8, $8, TRUE)
         RETURNING *`,
        [clerkId, email, username, lower, displayName, photoUrl, bio || '', now]
      );
      return mapUser(rows[0]);
    } catch (e) {
      if (attempt === 9) throw e;
    }
  }
}

async function updateExistingUser(userId, { email, displayName, photoUrl }) {
  const { rows } = await pool.query(
    `UPDATE users SET email = COALESCE($2, email), display_name = COALESCE($3, display_name), photo_url = COALESCE($4, photo_url), last_seen = $5
     WHERE user_id = $1 RETURNING *`,
    [userId, email || null, displayName || null, photoUrl || null, Date.now()]
  );
  return rows[0] ? mapUser(rows[0]) : null;
}

export function normalizeClerkUser(clerkUser) {
  const emails = clerkUser.emailAddresses || [];
  const email =
    clerkUser.primaryEmailAddress?.emailAddress ||
    emails[0]?.emailAddress ||
    `usuario-${clerkUser.id}@kivo.local`;
  const firstName = clerkUser.firstName || '';
  const lastName = clerkUser.lastName || '';
  const baseName =
    sanitizeBaseName(clerkUser.username) ||
    sanitizeBaseName((firstName + lastName).trim()) ||
    sanitizeBaseName(email.split('@')[0]);
  const displayName =
    [firstName, lastName].filter(Boolean).join(' ').trim() ||
    (emails[0]?.emailAddress || '').split('@')[0] ||
    'Usuario';
  return {
    clerkId: clerkUser.id,
    email: email.toLowerCase(),
    baseName,
    displayName,
    photoUrl: clerkUser.imageUrl || null,
    bio: ''
  };
}

export async function syncClerkUser(clerkUser) {
  const normalized = normalizeClerkUser(clerkUser);
  const existing = await pool.query('SELECT 1 FROM users WHERE user_id = $1', [clerkUser.id]);
  if (existing.rowCount > 0) {
    return updateExistingUser(clerkUser.id, normalized);
  }
  return insertNewUser({ ...normalized, clerkId: clerkUser.id });
}

export async function ensureUser(clerkId) {
  if (!clerkConfigured) return null;
  const existing = await pool.query('SELECT * FROM users WHERE user_id = $1', [clerkId]);
  if (existing.rowCount > 0) return mapUser(existing.rows[0]);

  try {
    const clerkUser = await clerkClient.users.getUser(clerkId);
    const normalized = normalizeClerkUser(clerkUser);
    if (normalized.email) {
      const byEmail = await pool.query('SELECT * FROM users WHERE LOWER(email) = LOWER($1) LIMIT 1', [normalized.email]);
      if (byEmail.rowCount > 0) {
        const { user_id: oldId } = byEmail.rows[0];
        await pool.query(
          `UPDATE users
             SET user_id = $1, email = COALESCE($2, email),
                 display_name = COALESCE($3, display_name),
                 photo_url = COALESCE($4, photo_url), last_seen = $5
           WHERE user_id = $6`,
          [clerkId, normalized.email, normalized.displayName || null, normalized.photoUrl || null, Date.now(), oldId]
        );
        const updated = await pool.query('SELECT * FROM users WHERE user_id = $1', [clerkId]);
        return mapUser(updated.rows[0]);
      }
    }
    return await syncClerkUser(clerkUser);
  } catch (e) {
    console.error('[ensureUser]', e);
    return null;
  }
}

export async function removeUser(clerkId) {
  await pool.query('DELETE FROM users WHERE user_id = $1', [clerkId]);
}
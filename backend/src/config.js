import 'dotenv/config';

export const PORT = process.env.PORT || 3000;
export const DATABASE_URL =
  process.env.DATABASE_URL || 'postgres://kivo:kivo123@localhost:5432/kivo';
export const CLERK_PUBLISHABLE_KEY = process.env.CLERK_PUBLISHABLE_KEY || '';
export const CLERK_SECRET_KEY = process.env.CLERK_SECRET_KEY || '';
export const CLERK_WEBHOOK_SIGNING_SECRET = process.env.CLERK_WEBHOOK_SIGNING_SECRET || '';
export const ONESIGNAL_APP_ID = process.env.ONESIGNAL_APP_ID || '';
export const ONESIGNAL_REST_API_KEY = process.env.ONESIGNAL_REST_API_KEY || '';
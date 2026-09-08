import { ONESIGNAL_APP_ID, ONESIGNAL_REST_API_KEY } from './config.js';

export async function sendPush(userId, title, body) {
  if (!ONESIGNAL_APP_ID || !ONESIGNAL_REST_API_KEY) {
    console.log('[push] OneSignal no configurado, notificacion omitida');
    return;
  }

  try {
    const response = await fetch('https://api.onesignal.com/notifications?c=push', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Key ${ONESIGNAL_REST_API_KEY}`
      },
      body: JSON.stringify({
        app_id: ONESIGNAL_APP_ID,
        include_external_user_ids: [userId],
        headings: { en: title },
        contents: { en: body }
      })
    });

    if (!response.ok) {
      console.error('[push] Error OneSignal:', response.status, await response.text());
    }
  } catch (e) {
    console.error('[push] Error al enviar:', e.message);
  }
}
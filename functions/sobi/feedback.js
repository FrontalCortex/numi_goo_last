'use strict';

/**
 * sobiFeedback callable — Sobi'nin bir cevabına beğen / beğenme (kullanıcı kararı 31.10.2026).
 * Haftalık iyileştirme döngüsünün verisi: tools/sobi-eval/haftalik/haftalik.js okuyor.
 *
 * MODEL
 *   sobiChats/{uid}/sessions/{sessionId}.transcript[i].rating   1 | -1 | 0 (geri alındı)
 *   sobiChats/{uid}/sobiFeedback/{sessionId}_{i}                 analiz kaydı: oy, cevap, çocuğun
 *     önceki mesajı (temizlenmiş), kaynak (hazir / model), açıklama anahtarı, adım, talimat sürümü
 *   Hesap silinince sobiChats/{uid} bütünüyle siliniyor (index.js), oylar da gidiyor. Kayıt
 *   sohbetle aynı sürede (30 gün, expiresAt) TTL ile silinir. İstemci erişmiyor; yalnızca sunucu.
 *
 * Çocuğun metni oturum kaydındakiyle aynı: guard.cleanChildText'ten geçmiş (e-posta, telefon, ad
 * kalıpları maskeli).
 */

const SESSION_ID = /^[A-Za-z0-9_-]{8,64}$/;
const RETENTION_MS = 30 * 24 * 60 * 60 * 1000;

module.exports = function createSobiFeedback({ functions, admin, db, promptVersion }) {
  return functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
    const uid = context.auth.uid;
    const p = data || {};
    const sessionId = SESSION_ID.test(p.sessionId || '') ? p.sessionId : null;
    const index = Number.isInteger(p.messageIndex) && p.messageIndex >= 0 && p.messageIndex < 200 ? p.messageIndex : null;
    const value = p.value === 1 || p.value === -1 || p.value === 0 ? p.value : null;
    if (!sessionId || index === null || value === null) {
      throw new functions.https.HttpsError('invalid-argument', 'Geçersiz oy.');
    }

    const sessionRef = db.collection('sobiChats').doc(uid).collection('sessions').doc(sessionId);
    const feedbackRef = db.collection('sobiChats').doc(uid).collection('sobiFeedback').doc(`${sessionId}_${index}`);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(sessionRef);
      const transcript = snap.exists && Array.isArray(snap.get('transcript')) ? snap.get('transcript') : null;
      const line = transcript && transcript[index];
      if (!line || line.from !== 'sobi') throw new functions.https.HttpsError('not-found', 'Mesaj bulunamadı.');
      const updated = transcript.slice();
      updated[index] = { ...line, rating: value };
      tx.update(sessionRef, { transcript: updated });
      const prev = index > 0 && transcript[index - 1] && transcript[index - 1].from === 'cocuk' ? transcript[index - 1].text : null;
      const now = Date.now();
      const ts = admin.firestore.Timestamp;
      if (value === 0) {
        tx.delete(feedbackRef);
      } else {
        tx.set(feedbackRef, {
          value,
          sessionId,
          messageIndex: index,
          reply: line.text,
          childText: prev,
          source: line.source || 'model',
          aciklama: line.aciklama || null,
          suggestion: line.suggestion || null,
          fellBack: line.fellBack === true,
          tutorialNumber: Number.isInteger(line.tutorialNumber) ? line.tutorialNumber : null,
          stepIndex: Number.isInteger(line.stepIndex) ? line.stepIndex : null,
          stepId: line.stepId || null,
          promptVersion: snap.get('promptVersion') || promptVersion || null,
          model: snap.get('model') || null,
          at: ts.fromMillis(now),
          expiresAt: ts.fromMillis(now + RETENTION_MS),
        });
      }
    });
    return { ok: true };
  });
};

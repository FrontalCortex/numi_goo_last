'use strict';

/**
 * sobiFeedback callable — Sobi'nin bir cevabına beğen / beğenme (kullanıcı kararı 31.10.2026).
 * Haftalık iyileştirme döngüsünün verisi: tools/sobi-eval/haftalik/haftalik.js okuyor.
 *
 * MODEL
 *   sobiChats/{uid}/sessions/{sessionId}.transcript[i].rating   1 | -1 | 0 (geri alındı)
 *   sobiChats/{uid}/sobiFeedback/{sessionId}_{i}                 analiz kaydı: oy, cevap, çocuğun
 *     önceki mesajı (temizlenmiş), kaynak (hazir / model), açıklama anahtarı, adım, talimat sürümü,
 *     sohbetin türü (kind: ders / genel; 02.11.2026'dan önceki kayıtlarda yok)
 *   Hesap silinince sobiChats/{uid} bütünüyle siliniyor (index.js), oylar da gidiyor. Kayıt
 *   sohbetle aynı sürede (30 gün, expiresAt) TTL ile silinir. İstemci erişmiyor; yalnızca sunucu.
 *
 * Çocuğun metni oturum kaydındakiyle aynı: guard.cleanChildText'ten geçmiş (e-posta, telefon, ad
 * kalıpları maskeli). Oyla birlikte gelen yorum ("Bize daha fazla bilgi ver") da aynı maskeden geçer.
 *
 * BİLDİR (report: true) — beğenmemekten ayrı: uygunsuz ya da zararlı bir cevap için. Haftayı
 * beklemesin diye sunucu günlüğüne hemen uyarı yazılır (Cloud Logging: "sobiReport") ve kayıt
 *   sobiChats/{uid}/sobiReports/{sessionId}_{i}   status: 'yeni' (incelenince 'incelendi')
 * altına düşer; haftalık rapor bunları en başta listeler. Play'in üretken yapay zekâ kuralının
 * istediği uygulama içi bildirim yolu bu. Kayıt 90 gün tutulur (expiresAt).
 *
 * YEREL MESAJ (local: true) — kural motorunun yazdığı, sunucudan geçmeyen Sobi mesajları ("Sobi ile
 * analiz et" ekranında adım talimatı, hata teşhisi; kullanıcı kararı 31.10.2026). Sohbet kaydı yok;
 * mesajın metni istemciden gelir (en çok 1000 karakter), kimliği istemcinin ürettiği [localId].
 *   sobiChats/{uid}/sobiFeedback/local_{localId}   source: "kural", screen, question
 *   sobiChats/{uid}/sobiReports/local_{localId}
 * Haftalık rapor bunları "Kural motoru mesajları" bölümünde listeler.
 */

const guard = require('./guard');


const SESSION_ID = /^[A-Za-z0-9_-]{8,64}$/;
const RETENTION_MS = 30 * 24 * 60 * 60 * 1000;
const REPORT_RETENTION_MS = 90 * 24 * 60 * 60 * 1000;
const SCREEN = /^[a-z_]{1,32}$/;

module.exports = function createSobiFeedback({ functions, admin, db, promptVersion }) {
  return functions.https.onCall(async (data, context) => {
    if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
    const uid = context.auth.uid;
    const p = data || {};
    const sessionId = SESSION_ID.test(p.sessionId || '') ? p.sessionId : null;
    const index = Number.isInteger(p.messageIndex) && p.messageIndex >= 0 && p.messageIndex < 200 ? p.messageIndex : null;
    const value = p.value === 1 || p.value === -1 || p.value === 0 ? p.value : null;
    const report = p.report === true;
    const comment = typeof p.comment === 'string' ? guard.cleanChildText(p.comment).text : '';
    if (p.local === true) return localFeedback({ functions, admin, db, uid, p, value, report, comment });
    if (!sessionId || index === null || (value === null && !report)) {
      throw new functions.https.HttpsError('invalid-argument', 'Geçersiz oy.');
    }

    const userRef = db.collection('sobiChats').doc(uid);
    const sessionRef = userRef.collection('sessions').doc(sessionId);
    const feedbackRef = userRef.collection('sobiFeedback').doc(`${sessionId}_${index}`);
    const reportRef = userRef.collection('sobiReports').doc(`${sessionId}_${index}`);
    let logged = null;
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(sessionRef);
      const transcript = snap.exists && Array.isArray(snap.get('transcript')) ? snap.get('transcript') : null;
      const line = transcript && transcript[index];
      if (!line || line.from !== 'sobi') throw new functions.https.HttpsError('not-found', 'Mesaj bulunamadı.');
      const prev = index > 0 && transcript[index - 1] && transcript[index - 1].from === 'cocuk' ? transcript[index - 1].text : null;
      const now = Date.now();
      const ts = admin.firestore.Timestamp;
      const record = {
        sessionId,
        messageIndex: index,
        // Hangi ekranın sohbeti: 'genel' Sobi sekmesi, 'ders' ders içi "Sobi'ye sor". Haftalık rapor
        // ikisini ayırıyor (genel sohbette adım yok; ders gruplarına karışmasın).
        kind: snap.get('kind') === 'genel' ? 'genel' : 'ders',
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
      };
      const updatedLine = { ...line };
      if (value !== null) {
        updatedLine.rating = value;
        if (value === 0) tx.delete(feedbackRef);
        else tx.set(feedbackRef, { ...record, value, comment: comment || null, expiresAt: ts.fromMillis(now + RETENTION_MS) });
      }
      if (report) {
        updatedLine.reported = true;
        tx.set(reportRef, { ...record, comment: comment || null, status: 'yeni', expiresAt: ts.fromMillis(now + REPORT_RETENTION_MS) });
        logged = { uid, sessionId, index, reply: String(line.text).slice(0, 300), comment };
      }
      const updated = transcript.slice();
      updated[index] = updatedLine;
      tx.update(sessionRef, { transcript: updated });
    });
    if (logged) console.warn('sobiReport: Sobi cevabı bildirildi', logged);
    return { ok: true };
  });
};

/** Kural motorunun mesajına oy / bildirim (bkz. dosya başı, YEREL MESAJ). */
async function localFeedback({ functions, admin, db, uid, p, value, report, comment }) {
  const localId = SESSION_ID.test(p.localId || '') ? p.localId : null;
  const text = typeof p.text === 'string' ? p.text.trim().slice(0, 1000) : '';
  if (!localId || !text || (value === null && !report)) {
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz oy.');
  }
  const now = Date.now();
  const ts = admin.firestore.Timestamp;
  const record = {
    local: true,
    localId,
    source: 'kural',
    screen: SCREEN.test(p.screen || '') ? p.screen : 'bilinmiyor',
    question: typeof p.question === 'string' ? p.question.slice(0, 100) : null,
    reply: text,
    tutorialNumber: null,
    stepIndex: null,
    at: ts.fromMillis(now),
  };
  const userRef = db.collection('sobiChats').doc(uid);
  if (value !== null) {
    const ref = userRef.collection('sobiFeedback').doc(`local_${localId}`);
    if (value === 0) await ref.delete();
    else await ref.set({ ...record, value, comment: comment || null, expiresAt: ts.fromMillis(now + RETENTION_MS) });
  }
  if (report) {
    await userRef.collection('sobiReports').doc(`local_${localId}`)
      .set({ ...record, comment: comment || null, status: 'yeni', expiresAt: ts.fromMillis(now + REPORT_RETENTION_MS) });
    console.warn('sobiReport: kural motoru mesajı bildirildi', { uid, localId, reply: text.slice(0, 300), comment });
  }
  return { ok: true };
}

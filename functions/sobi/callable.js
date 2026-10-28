'use strict';

/**
 * sobiChat callable — "Sobi'ye sor" sohbetinin sunucu ucu. Firestore ve kimlik işi burada,
 * sohbet mantığı chat.js'te.
 *
 * GİZLİ DEĞİŞKEN
 *   Fonksiyon ANTHROPIC_API_KEY gizli değişkenini (Secret Manager) istiyor; index.js'e ancak
 *   değişken kurulduktan sonra bağlandı (28.10.2026). Değişken silinirse bu fonksiyonun deploy'u
 *   başarısız olur; claude/** dalına her push BÜTÜN fonksiyonları deploy ettiği için bu, ilgisiz
 *   bir değişikliğin deploy'unu da bozar. Kurulum: docs/ANLAMADIM_YARDIM_PLANI.md.
 *
 * MODEL
 *   sobiChats/{uid}                       { usageDay, usageCount } — günlük mesaj sayacı
 *   sobiChats/{uid}/sessions/{sessionId}  bir "Anlamadım" sohbeti:
 *     historyJson    modelin gördüğü geçmiş (düşünme blokları dahil, JSON metni: Firestore iç içe
 *                    dizi saklamıyor, geçmişte de iç içe diziler var)
 *     transcript     [{ from: 'cocuk'|'sobi', text, at }] — çocuğun ve gördüğünün düz kaydı
 *                    (ebeveyn paneli ve bildirilen cevapların incelenmesi için)
 *     pendingNote    son cevap denetime takıldıysa çocuğun gördüğü yedek metin
 *     expiresAt      30 gün sonrası; Firestore TTL ilkesi bu alanla siler (konsolda elle açılır)
 *   İstemci bu koleksiyona erişmiyor; yazma da okuma da yalnızca sunucudan (Admin SDK).
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const Anthropic = require('@anthropic-ai/sdk').default;
const chat = require('./chat');
const guard = require('./guard');
const tools = require('./tools');

const SYSTEM_PROMPT = fs.readFileSync(path.join(__dirname, 'prompt_tr.md'), 'utf8');
/** Talimat metninin parmak izi; her oturuma yazılır, hangi metinle konuşulduğu bilinsin. */
const PROMPT_VERSION = crypto.createHash('sha1').update(SYSTEM_PROMPT).digest('hex').slice(0, 10);

/** Günlük mesaj sınırı, plana göre (kullanıcı kararı bekleniyor: plan belgesi, açık soru 1). */
const DAILY_LIMIT = { Free: 20, Lite: 50, Pro: 100 };
/** Bir sohbette en fazla çocuk mesajı; sonra öğretmene sor önerilir. */
const MAX_TURNS = 10;
const RETENTION_MS = 30 * 24 * 60 * 60 * 1000;
const SESSION_ID = /^[A-Za-z0-9_-]{8,64}$/;

function trDay(ms) {
  return new Date(ms + 3 * 3600 * 1000).toISOString().slice(0, 10);
}

/** [createClient] yalnızca testte verilir (scripts/test-sobi.js sahte model kullanıyor). */
module.exports = function createSobiChat({ functions, admin, db, isAskQuestionRestricted, effectivePlan, createClient }) {
  let client = null;
  const getClient = () => (client ||= createClient
    ? createClient()
    : new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY }));

  return functions
    .runWith({ secrets: ['ANTHROPIC_API_KEY'], timeoutSeconds: 120, memory: '256MB' })
    .https.onCall(async (data, context) => {
      if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
      }
      const uid = context.auth.uid;
      const payload = data || {};
      const message = typeof payload.message === 'string' ? payload.message : '';
      const stepContext = chat.sanitizeContext(payload.context);
      const sessionId = SESSION_ID.test(payload.sessionId || '') ? payload.sessionId : null;

      const userSnap = await db.collection('users').doc(uid).get();
      if (!userSnap.exists) throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
      const userData = userSnap.data();
      if (isAskQuestionRestricted(userData)) {
        throw new functions.https.HttpsError('permission-denied', 'Hesabınız kısıtlanmış.');
      }

      const now = Date.now();
      const today = trDay(now);
      const limit = DAILY_LIMIT[effectivePlan(userData)] || DAILY_LIMIT.Free;
      const usageRef = db.collection('sobiChats').doc(uid);
      const usageSnap = await usageRef.get();
      const used = usageSnap.exists && usageSnap.get('usageDay') === today ? Number(usageSnap.get('usageCount')) || 0 : 0;
      if (used >= limit) {
        throw new functions.https.HttpsError('resource-exhausted', 'Bugünkü Sobi sohbet hakkın doldu.', { limit });
      }

      const sessionRef = sessionId
        ? usageRef.collection('sessions').doc(sessionId)
        : usageRef.collection('sessions').doc();
      const sessionSnap = sessionId ? await sessionRef.get() : null;
      const session = sessionSnap && sessionSnap.exists ? sessionSnap.data() : null;
      const turns = session ? Number(session.turns) || 0 : 0;
      if (turns >= MAX_TURNS) {
        return { sessionId: sessionRef.id, turnLimitReached: true, turnsLeft: 0, remainingToday: limit - used };
      }
      let history = [];
      if (session && session.historyJson) {
        try { history = JSON.parse(session.historyJson); } catch (_) { history = []; }
      }

      let result;
      try {
        result = await chat.runTurn({
          client: getClient(),
          system: SYSTEM_PROMPT,
          history,
          context: stepContext,
          childText: message,
          note: session ? session.pendingNote || null : null,
        });
      } catch (err) {
        // Sıra en özelden en genele: SDK'da hepsi (bağlantı hatası dahil) APIError'dan türüyor.
        // Hiçbiri çocuğa hata göstermiyor: yedek metin dönüyor, hak düşmüyor, geçmiş değişmiyor.
        if (err instanceof Anthropic.RateLimitError) console.warn('sobiChat: hız sınırı', err.message);
        else if (err instanceof Anthropic.AuthenticationError) console.error('sobiChat: API anahtarı geçersiz');
        else if (err instanceof Anthropic.BadRequestError) console.error('sobiChat: geçersiz istek', err.message);
        else if (err instanceof Anthropic.APIConnectionError) console.warn('sobiChat: bağlantı hatası', err.message);
        else if (err instanceof Anthropic.APIError) console.error(`sobiChat: API hatası ${err.status}`, err.message);
        else throw err;
        return {
          sessionId: sessionRef.id,
          reply: guard.fallbackReply(stepContext, tools),
          actions: [],
          suggestions: chat.suggestionsFor(stepContext, []),
          unavailable: true,
          turnsLeft: MAX_TURNS - turns,
          remainingToday: limit - used,
        };
      }

      if (result.fellBack) console.warn('sobiChat: yedek metin', { uid, reasons: result.reasons, stopReason: result.stopReason });

      const childLine = guard.cleanChildText(message).text;
      const transcript = (session && Array.isArray(session.transcript) ? session.transcript : []).concat([
        { from: 'cocuk', text: childLine, at: now },
        { from: 'sobi', text: result.reply, at: Date.now(), ...(result.fellBack ? { fellBack: true } : {}) },
      ]);
      const totals = { ...(session && session.usage ? session.usage : {}) };
      for (const [k, v] of Object.entries(result.usage)) totals[k] = (totals[k] || 0) + v;

      const ts = admin.firestore.Timestamp;
      const batch = db.batch();
      batch.set(sessionRef, {
        historyJson: JSON.stringify(result.history),
        transcript,
        turns: turns + 1,
        stepId: stepContext.stepId,
        pendingNote: result.fellBack ? result.reply : null,
        model: chat.DEFAULT_MODEL,
        promptVersion: PROMPT_VERSION,
        usage: totals,
        ...(session ? {} : { createdAt: ts.fromMillis(now) }),
        updatedAt: ts.fromMillis(now),
        expiresAt: ts.fromMillis(now + RETENTION_MS),
      }, { merge: true });
      batch.set(usageRef, { usageDay: today, usageCount: used + 1 }, { merge: true });
      await batch.commit();

      return {
        sessionId: sessionRef.id,
        reply: result.reply,
        actions: result.actions,
        suggestions: result.suggestions,
        fellBack: result.fellBack,
        turnsLeft: MAX_TURNS - (turns + 1),
        remainingToday: limit - (used + 1),
      };
    });
};

module.exports.PROMPT_VERSION = PROMPT_VERSION;
module.exports.DAILY_LIMIT = DAILY_LIMIT;
module.exports.MAX_TURNS = MAX_TURNS;

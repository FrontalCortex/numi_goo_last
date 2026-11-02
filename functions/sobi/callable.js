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
 *   sobiChats/{uid}                       günlük ücretsiz hak, Sobi kredisi, aylık sayaç (wallet.js)
 *   sobiChats/{uid}/sessions/{sessionId}  bir "Anlamadım" sohbeti:
 *     historyJson    modelin gördüğü geçmiş (düşünme blokları dahil, JSON metni: Firestore iç içe
 *                    dizi saklamıyor, geçmişte de iç içe diziler var)
 *     transcript     [{ from: 'cocuk'|'sobi', text, at }] — çocuğun ve gördüğünün düz kaydı
 *                    (ebeveyn paneli ve bildirilen cevapların incelenmesi için)
 *     pendingNote    son cevap denetime takıldıysa çocuğun gördüğü yedek metin
 *     expiresAt      30 gün sonrası; Firestore TTL ilkesi bu alanla siler (konsolda elle açılır)
 *     kind, title    yalnızca genel sohbette (Sobi sekmesi, `mode: 'genel'`): 'genel' ve ilk mesajdan
 *                    kısaltılmış başlık; liste ve silme sessions.js'te (sobiSessions)
 *   İstemci bu koleksiyona erişmiyor; yazma da okuma da yalnızca sunucudan (Admin SDK).
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const Anthropic = require('@anthropic-ai/sdk').default;
const chat = require('./chat');
const guard = require('./guard');
const tools = require('./tools');
const wallet = require('./wallet');

const SYSTEM_PROMPT = fs.readFileSync(path.join(__dirname, 'prompt_tr.md'), 'utf8');
/** Talimat metninin parmak izi; her oturuma yazılır, hangi metinle konuşulduğu bilinsin. */
const PROMPT_VERSION = crypto.createHash('sha1').update(SYSTEM_PROMPT).digest('hex').slice(0, 10);
/**
 * Genel sohbetin (Sobi sekmesi) ek talimatı: ders talimatının ardından ayrı sistem bloğu
 * (chat.runTurn). Uygulama rehberindeki sayılar (plan hakları, can, anahtar) cevapta serbest:
 * guard yalnızca kaynakta geçen sayıya izin veriyor, bu metin de kaynak.
 */
const GENERAL_PROMPT = fs.readFileSync(path.join(__dirname, 'prompt_genel_tr.md'), 'utf8');
const GENERAL_PROMPT_VERSION = crypto.createHash('sha1').update(SYSTEM_PROMPT + GENERAL_PROMPT).digest('hex').slice(0, 10);
const GENERAL_SOURCES = guard.numbersIn(GENERAL_PROMPT);
/** Genel sohbetin listedeki başlığı: çocuğun ilk mesajından (maskelenmiş). */
const TITLE_MAX = 40;
/** Sohbete düğme olarak düşen ekran işleri (genel sohbet); yeniden açılan sohbette de görünsün diye kayıtta. */
const BUTTON_KINDS = new Set(['goster', 'alistirma', 'test', 'kredi', 'pro']);

function titleOf(text) {
  const t = String(text || '').trim();
  if (t.length <= TITLE_MAX) return t || 'Sohbet';
  const cut = t.slice(0, TITLE_MAX);
  const space = cut.lastIndexOf(' ');
  return `${(space > 20 ? cut.slice(0, space) : cut).trim()}…`;
}

/**
 * homeworkRuleStats/{uid} → genel sohbet bağlamının kural satırları. Ustalık penceresi (son ~50
 * işlem, `recent`) varsa o: toplam zamanla düzleşip son durumu göstermiyor. Yoksa toplam (`tally`).
 */
function ruleFacts(stats) {
  const s = stats || {};
  const keys = new Set([...Object.keys(s.tally || {}), ...Object.keys(s.recent || {})]);
  const rules = [];
  for (const key of keys) {
    if (!chat.RULE_LABELS[key]) continue;
    const win = s.recent && Array.isArray(s.recent[key]) ? s.recent[key] : null;
    const sum = win
      ? win.reduce((a, x) => ({ c: a.c + (Number(x && x.c) || 0), w: a.w + (Number(x && x.w) || 0) }), { c: 0, w: 0 })
      : { c: Number(s.tally[key] && s.tally[key].c) || 0, w: Number(s.tally[key] && s.tally[key].w) || 0 };
    if (sum.c + sum.w > 0) rules.push({ key, ...sum });
  }
  const levels = Object.entries(s.sobiLevel || {})
    .filter(([key, v]) => chat.RULE_LABELS[key] && Number.isInteger(v))
    .map(([key, level]) => ({ key, level }));
  const order = Object.keys(chat.RULE_LABELS);
  const byRule = (a, b) => order.indexOf(a.key) - order.indexOf(b.key);
  return { rules: rules.sort(byRule), levels: levels.sort(byRule) };
}

/**
 * Bir sohbette en fazla çocuk mesajı; sonra yeni sohbet önerilir (sobi_turn_limit). Sohbet bir öğreticinin
 * tamamı boyunca sürdüğü için (yalnızca bir soru adımı değil) 20.
 */
const MAX_TURNS = 20;
const RETENTION_MS = 30 * 24 * 60 * 60 * 1000;
const SESSION_ID = /^[A-Za-z0-9_-]{8,64}$/;

/** Geçmişteki düşünme bloğu reddedildi mi (geçmişi sıfırlayınca düzelen tek hata). */
function isStaleHistoryError(err) {
  return err instanceof Anthropic.BadRequestError && /thinking/i.test(String(err.message)) && /signature|bound/i.test(String(err.message));
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
      // Genel sohbet (Sobi sekmesi): adım bağlamı yok; bağlamı aşağıda sunucu kendisi kuruyor.
      const general = payload.mode === 'genel';
      let stepContext = general ? chat.generalContext() : chat.sanitizeContext(payload.context);
      // Çocuk bir öneri balonuna dokunduysa balonun anahtarı (uygulama metni de "message"da gönderiyor).
      const suggestion = typeof payload.suggestion === 'string' && /^[a-z_]{1,30}$/.test(payload.suggestion) ? payload.suggestion : null;
      const direct = chat.directReplyFor(stepContext, suggestion);
      const sessionId = SESSION_ID.test(payload.sessionId || '') ? payload.sessionId : null;

      const [userSnap, statsSnap] = await Promise.all([
        db.collection('users').doc(uid).get(),
        general ? db.collection('homeworkRuleStats').doc(uid).get() : Promise.resolve(null),
      ]);
      if (!userSnap.exists) throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
      const userData = userSnap.data();
      if (isAskQuestionRestricted(userData)) {
        throw new functions.https.HttpsError('permission-denied', 'Hesabınız kısıtlanmış.');
      }

      const now = Date.now();
      const plan = effectivePlan(userData);
      const usageRef = db.collection('sobiChats').doc(uid);
      const usageSnap = await usageRef.get();
      const balance = wallet.balanceOf(usageSnap.exists ? usageSnap.data() : null, plan, now);
      if (general) {
        stepContext = chat.generalContext({
          plan, freeLeft: balance.freeLeft, freeLimit: balance.freeLimit, credits: balance.credits,
          ...ruleFacts(statsSnap && statsSnap.exists ? statsSnap.data() : null),
        });
      }
      // Uygulamaya giden bakiye: Sobi ekranındaki "bugün 2 ücretsiz / 15 kredi" satırı.
      const balanceFields = (b) => ({ remainingToday: b.freeLeft, freeLimit: b.freeLimit, credits: b.credits });
      // Modelsiz cevap ücretsiz: hakkı ve kredisi bitmiş çocuk da balondaki hazır açıklamayı görür.
      if (!direct && !wallet.canSend(balance)) {
        throw new functions.https.HttpsError('resource-exhausted', 'Bugünkü Sobi hakkın ve kredin bitti.', {
          freeLimit: balance.freeLimit, credits: balance.credits, plan,
        });
      }

      const sessionRef = sessionId
        ? usageRef.collection('sessions').doc(sessionId)
        : usageRef.collection('sessions').doc();
      const sessionSnap = sessionId ? await sessionRef.get() : null;
      const session = sessionSnap && sessionSnap.exists ? sessionSnap.data() : null;
      const turns = session ? Number(session.turns) || 0 : 0;
      if (turns >= MAX_TURNS) {
        return { sessionId: sessionRef.id, turnLimitReached: true, turnsLeft: 0, ...balanceFields(balance) };
      }
      let history = [];
      if (session && session.historyJson) {
        try { history = JSON.parse(session.historyJson); } catch (_) { history = []; }
      }

      const runWith = (h) => chat.runTurn({
        client: getClient(),
        system: general ? [SYSTEM_PROMPT, GENERAL_PROMPT] : SYSTEM_PROMPT,
        history: h,
        context: stepContext,
        childText: message,
        note: session ? session.pendingNote || null : null,
        extraSources: general ? GENERAL_SOURCES : null,
      });
      let result;
      try {
        if (direct) {
          result = chat.directTurn({ history, context: stepContext, childText: message, text: direct.text });
        } else try {
          result = await runWith(history);
        } catch (err) {
          // Geçmişteki düşünme blokları artık geçerli değilse (araç listesi ya da talimat metni
          // değişti) o sohbet hiç ilerleyemez: geçmişi bırakıp bu mesajla yeni başla. Çocuk yalnızca
          // Sobi'nin önceki konuşmayı "unuttuğunu" görür; ekrandaki mesajlar uygulamada kalır.
          if (history.length && isStaleHistoryError(err)) {
            console.warn('sobiChat: geçmiş geçersiz, sıfırlanıyor', { uid, message: err.message });
            history = [];
            result = await runWith(history);
          } else {
            throw err;
          }
        }
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
          ...balanceFields(balance),
        };
      }

      // Modelin takılan cevabı da yazılıyor (çocuğun metni değil): sebebi tahmin etmek zorunda kalmayalım.
      if (result.fellBack) {
        console.warn('sobiChat: yedek metin', {
          uid, reasons: result.reasons, stopReason: result.stopReason,
          modelReply: typeof result.modelReply === 'string' ? result.modelReply.slice(0, 400) : null,
        });
      }

      const childLine = guard.cleanChildText(message).text;
      const buttons = general ? result.actions.filter((a) => BUTTON_KINDS.has(a.tur)) : [];
      const transcript = (session && Array.isArray(session.transcript) ? session.transcript : []).concat([
        { from: 'cocuk', text: childLine, at: now },
        {
          from: 'sobi', text: result.reply, at: Date.now(), ...(result.fellBack ? { fellBack: true } : {}),
          ...(buttons.length ? { buttons } : {}),
          // Beğeni ve haftalık iyileştirme için: cevap hazır açıklamadan mı, modelden mi; hangi adımda.
          source: direct ? 'hazir' : 'model',
          ...(direct ? { aciklama: direct.key } : {}),
          ...(suggestion ? { suggestion } : {}),
          tutorialNumber: stepContext.tutorialNumber, stepIndex: stepContext.stepIndex, stepId: stepContext.stepId,
        },
      ]);
      const totals = { ...(session && session.usage ? session.usage : {}) };
      for (const [k, v] of Object.entries(result.usage)) totals[k] = (totals[k] || 0) + v;

      const ts = admin.firestore.Timestamp;
      // Oturum ve bedel aynı işlemde: aynı anda iki cihazdan gelen mesaj hakkı iki kez kullanamasın.
      // Bedel yalnızca modelin geçen cevabına; hazır açıklama ve yedek metin ücretsiz (kullanıcı kararı).
      const paid = !direct && !result.fellBack;
      const after = await db.runTransaction(async (tx) => {
        const fresh = paid ? await tx.get(usageRef) : null;
        tx.set(sessionRef, {
          historyJson: JSON.stringify(result.history),
          transcript,
          turns: turns + 1,
          stepId: stepContext.stepId,
          pendingNote: result.fellBack ? result.reply : null,
          model: chat.DEFAULT_MODEL,
          promptVersion: general ? GENERAL_PROMPT_VERSION : PROMPT_VERSION,
          usage: totals,
          ...(session ? {} : { createdAt: ts.fromMillis(now) }),
          // Genel sohbet listesi (sobiSessions) bu alanla süzüyor; ders sohbetlerinde alan yok.
          ...(general && !session ? { kind: 'genel', title: titleOf(childLine) } : {}),
          updatedAt: ts.fromMillis(now),
          expiresAt: ts.fromMillis(now + RETENTION_MS),
        }, { merge: true });
        if (!paid) return balance;
        const charge = wallet.chargeMessage(fresh.exists ? fresh.data() : null, plan, now);
        if (charge.paidWith === 'none') console.warn('sobiChat: hak ve kredi işlem sırasında bitti', { uid });
        tx.set(usageRef, charge.update, { merge: true });
        // Üst para paneli users belgesini dinliyor (sobiChats'i okuyamıyor): kredinin gösterim
        // kopyası. Okumadan merge yeter; asıl bakiye sobiChats'te kalıyor.
        if (charge.paidWith === 'credit') {
          tx.set(db.collection('users').doc(uid), { sobiCredits: charge.update.credits }, { merge: true });
        }
        return charge.after;
      });

      return {
        sessionId: sessionRef.id,
        reply: result.reply,
        actions: result.actions,
        suggestions: result.suggestions,
        fellBack: result.fellBack,
        turnsLeft: MAX_TURNS - (turns + 1),
        ...balanceFields(after),
        // Beğen / beğenme bu sıraya bağlanıyor (sobiFeedback).
        messageIndex: transcript.length - 1,
        source: direct ? 'hazir' : 'model',
      };
    });
};

module.exports.PROMPT_VERSION = PROMPT_VERSION;
module.exports.GENERAL_PROMPT_VERSION = GENERAL_PROMPT_VERSION;
module.exports.titleOf = titleOf;
module.exports.ruleFacts = ruleFacts;
module.exports.MAX_TURNS = MAX_TURNS;

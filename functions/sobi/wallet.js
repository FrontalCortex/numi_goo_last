'use strict';

/**
 * Sobi ekonomisi (kullanıcı kararları 01.11.2026):
 *   - Kredi yalnızca yapay zekâ mesajına. Hazır açıklama (modelsiz) ve yedek metin (model cevabı
 *     denetime takıldı ya da model ulaşılamadı) ne günlük haktan ne krediden düşer.
 *   - Günlük ücretsiz hak plana göre: Free 2, Pro 20. Lite için karar yoktu; reklamlı ara plan
 *     olduğu için arada 10. Hak bitince her mesaj 1 kredi; kredi de yoksa mesaj gitmez.
 *   - Kredi süresiz; paketler Play'den (index.js PLAY_PRODUCT_CATALOG → sobiCredits).
 *   - Sobi'nin günün ilk testinden sonraki ek testi 3 anahtar; Pro (ve üstü) ile onaylı öğretmene
 *     ücretsiz.
 *
 * Bakiye `sobiChats/{uid}` belgesinde, istemci bu koleksiyona hiç erişmiyor (firestore.rules'ta
 * kural yok → reddedilir). `users` belgesine konmadı: oradaki sunucu alanları listesine
 * (serverOnlyFields) dokunmadan istemcinin yazamayacağı bir yer.
 *   credits                 kalan Sobi kredisi (iade edilen paket eksiye düşürebilir)
 *   usageDay, usageCount    bugünkü ücretsiz hakla gönderilen mesaj
 *   monthKey, monthCount    bu ay gönderilen (model) mesaj — veli paneli
 *   monthCreditsUsed        bu ay harcanan kredi
 *   extraTestDay, extraTestIndex  bugün parası ödenmiş son ek test sırası (yarıda bırakılan test
 *                                 yeniden açılınca bir daha ödenmesin)
 */

const FREE_DAILY = { Free: 2, Lite: 10, Pro: 20, Premium: 20 };
const EXTRA_TEST_KEYS = 3;
const FREE_EXTRA_TEST_PLANS = new Set(['Pro', 'Premium']);
/** Bir günde en fazla ek test sırası; istemci bundan büyüğünü gönderemez. */
const MAX_TEST_INDEX = 50;

/** Türkiye günü ve ayı (sunucu UTC). */
function trDay(ms) {
  return new Date(ms + 3 * 3600 * 1000).toISOString().slice(0, 10);
}
function trMonth(ms) {
  return trDay(ms).slice(0, 7);
}

function freeLimitOf(plan) {
  return FREE_DAILY[plan] || FREE_DAILY.Free;
}

/** sobiChats/{uid} verisinden bugünkü durum. */
function balanceOf(data, plan, now) {
  const d = data || {};
  const freeLimit = freeLimitOf(plan);
  const used = d.usageDay === trDay(now) ? Number(d.usageCount) || 0 : 0;
  const sameMonth = d.monthKey === trMonth(now);
  return {
    plan,
    freeLimit,
    freeLeft: Math.max(0, freeLimit - used),
    credits: Number(d.credits) || 0,
    monthCount: sameMonth ? Number(d.monthCount) || 0 : 0,
    monthCreditsUsed: sameMonth ? Number(d.monthCreditsUsed) || 0 : 0,
  };
}

/**
 * Bir model mesajının bedeli ve belgeye yazılacak alanlar. Önce günlük hak, sonra kredi. İkisi de
 * yoksa (ön denetimden sonra başka bir cihazdan harcandı) mesaj yine de verildiği için yalnızca
 * sayaçlar artar; kredi eksiye düşürülmüyor (çocuk zaten cevabı gördü, en fazla birkaç mesajlık kayıp).
 */
function chargeMessage(data, plan, now) {
  const b = balanceOf(data, plan, now);
  const update = {
    monthKey: trMonth(now),
    monthCount: b.monthCount + 1,
  };
  let paidWith = 'none';
  if (b.freeLeft > 0) {
    paidWith = 'free';
    update.usageDay = trDay(now);
    update.usageCount = b.freeLimit - b.freeLeft + 1;
  } else if (b.credits > 0) {
    paidWith = 'credit';
    update.credits = b.credits - 1;
    update.monthCreditsUsed = b.monthCreditsUsed + 1;
  }
  const after = {
    ...b,
    freeLeft: paidWith === 'free' ? b.freeLeft - 1 : b.freeLeft,
    credits: paidWith === 'credit' ? b.credits - 1 : b.credits,
    monthCount: b.monthCount + 1,
    monthCreditsUsed: update.monthCreditsUsed != null ? update.monthCreditsUsed : b.monthCreditsUsed,
  };
  return { paidWith, update, after };
}

/** Mesaj gönderilebilir mi (ücretsiz hak ya da kredi var mı). */
function canSend(b) {
  return b.freeLeft > 0 || b.credits > 0;
}

/**
 * Ek testin bedeli. [testIndex]: bugün bitirilmiş test sayısı (1 = günün ilk ek testi).
 * Aynı sıra bugün zaten ödendiyse ücretsiz (yarıda bırakılıp yeniden açılan test).
 */
function extraTestDecision({ plan, approvedTeacher, keys, chatData, testIndex, now }) {
  const d = chatData || {};
  const paidIndex = d.extraTestDay === trDay(now) ? Number(d.extraTestIndex) || 0 : 0;
  if (approvedTeacher || FREE_EXTRA_TEST_PLANS.has(plan)) return { cost: 0, reason: 'plan' };
  if (testIndex <= paidIndex) return { cost: 0, reason: 'already_paid' };
  if (keys < EXTRA_TEST_KEYS) return { cost: EXTRA_TEST_KEYS, insufficient: true };
  return { cost: EXTRA_TEST_KEYS, reason: 'keys' };
}

module.exports = function createSobiWallet({ functions, db, effectivePlan, isApprovedTeacher }) {
  const requireUid = (context) => {
    if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
    return context.auth.uid;
  };

  /** Sobi ekranı ve veli paneli: bugünkü ücretsiz hak, kredi, bu ayın mesajı. */
  const sobiBalance = functions.https.onCall(async (_data, context) => {
    const uid = requireUid(context);
    const [userSnap, chatSnap] = await Promise.all([
      db.collection('users').doc(uid).get(),
      db.collection('sobiChats').doc(uid).get(),
    ]);
    const plan = effectivePlan(userSnap.exists ? userSnap.data() : {});
    const b = balanceOf(chatSnap.exists ? chatSnap.data() : null, plan, Date.now());
    return { ...b, extraTestKeys: FREE_EXTRA_TEST_PLANS.has(plan) ? 0 : EXTRA_TEST_KEYS };
  });

  /**
   * Ek testi başlatmadan önce: gerekiyorsa 3 anahtar düşer (users.keys, sunucu alanı). Anahtar ve
   * "bu sıra ödendi" kaydı aynı işlemde; iki kez basılsa da bir kez düşer.
   */
  const startSobiExtraTest = functions.https.onCall(async (data, context) => {
    const uid = requireUid(context);
    const testIndex = Number(data && data.testIndex);
    if (!Number.isInteger(testIndex) || testIndex < 1 || testIndex > MAX_TEST_INDEX) {
      throw new functions.https.HttpsError('invalid-argument', 'Geçersiz test sırası.');
    }
    const userRef = db.collection('users').doc(uid);
    const chatRef = db.collection('sobiChats').doc(uid);
    return db.runTransaction(async (tx) => {
      const [userSnap, chatSnap] = await Promise.all([tx.get(userRef), tx.get(chatRef)]);
      if (!userSnap.exists) throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
      const user = userSnap.data();
      const keys = Number.parseInt(user.keys, 10) || 0;
      const now = Date.now();
      const decision = extraTestDecision({
        plan: effectivePlan(user),
        approvedTeacher: isApprovedTeacher(user),
        keys,
        chatData: chatSnap.exists ? chatSnap.data() : null,
        testIndex,
        now,
      });
      if (decision.insufficient) {
        throw new functions.https.HttpsError('failed-precondition', 'Anahtar yetmiyor.', { need: decision.cost, keys });
      }
      if (decision.cost > 0) {
        tx.update(userRef, { keys: keys - decision.cost });
        tx.set(chatRef, { extraTestDay: trDay(now), extraTestIndex: testIndex }, { merge: true });
      }
      return { success: true, cost: decision.cost, keys: keys - decision.cost, reason: decision.reason };
    });
  });

  return { sobiBalance, startSobiExtraTest };
};

Object.assign(module.exports, {
  FREE_DAILY, EXTRA_TEST_KEYS, trDay, trMonth, freeLimitOf, balanceOf, chargeMessage, canSend, extraTestDecision,
});

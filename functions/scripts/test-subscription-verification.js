/**
 * Abonelik doğrulamasının iki kararını test eder — Play API'sine ya da Firestore'a hiç
 * dokunmadan.
 *
 * 1. subscriptionEntitlement — "bu abonelik şu an hak veriyor mu"
 *    Kapattığı hata: iptal edilmiş ama süresi dolmamış abonelik (Play'de
 *    SUBSCRIPTION_STATE_CANCELED) hak vermiyor sayılıyordu; kullanıcı iptal ettiği anda
 *    ödediği günleri kaybediyordu.
 *
 * 2. resolveVerifiedProductId — "bu token gerçekten hangi ürüne ait"
 *    Kapattığı açık: plan, istemcinin bildirdiği productId'den okunuyordu. Geçerli bir
 *    lite_monthly token'ıyla pro_monthly iddia etmek Pro planını veriyordu.
 *
 * ÇALIŞTIRMA
 *   cd functions && node scripts/test-subscription-verification.js
 */
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'numigo-new';
process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:9';

const fns = require('../index');
const entitlement = fns._subscriptionEntitlement;
const productIds = fns._subscriptionProductIds;
const verifyProduct = fns._resolveVerifiedProductId;

const NOW = Date.parse('2026-10-03T12:00:00Z');
const FUTURE = '2026-10-31T12:00:00Z'; // ödenen dönemin sonu
const PAST = '2026-09-30T12:00:00Z';

let failures = 0;
function check(label, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${actual}${ok ? '' : ` (beklenen: ${expected})`}`);
}

/** Play'in subscriptionsv2 cevabının test için gereken kadarı. */
function sub(state, expiryTime, product = 'pro_monthly') {
  return { subscriptionState: state, lineItems: [{ productId: product, expiryTime }] };
}

console.log('subscriptionEntitlement\n');

console.log('Hak veren durumlar (bitiş tarihi gelecekte):');
check('  ACTIVE', entitlement(sub('SUBSCRIPTION_STATE_ACTIVE', FUTURE), NOW).stillValid, true);
check(
  '  IN_GRACE_PERIOD (ödeme sorunu, hak sürüyor)',
  entitlement(sub('SUBSCRIPTION_STATE_IN_GRACE_PERIOD', FUTURE), NOW).stillValid,
  true
);
// ASIL HATA: iptal eden kullanıcı ödediği dönemi sonuna kadar kullanmalı.
check(
  '  CANCELED (iptal etti, süresi dolmadı)',
  entitlement(sub('SUBSCRIPTION_STATE_CANCELED', FUTURE), NOW).stillValid,
  true
);

console.log('\nHak vermeyen durumlar:');
check(
  '  CANCELED + süresi DOLMUŞ',
  entitlement(sub('SUBSCRIPTION_STATE_CANCELED', PAST), NOW).stillValid,
  false
);
check('  EXPIRED', entitlement(sub('SUBSCRIPTION_STATE_EXPIRED', PAST), NOW).stillValid, false);
check(
  '  ON_HOLD (ödeme alınamadı, Play askıya aldı)',
  entitlement(sub('SUBSCRIPTION_STATE_ON_HOLD', FUTURE), NOW).stillValid,
  false
);
check(
  '  PAUSED (kullanıcı kendi duraklattı)',
  entitlement(sub('SUBSCRIPTION_STATE_PAUSED', FUTURE), NOW).stillValid,
  false
);
check(
  '  PENDING (ilk ödeme tamamlanmadı)',
  entitlement(sub('SUBSCRIPTION_STATE_PENDING', FUTURE), NOW).stillValid,
  false
);

console.log('\nBozuk / eksik cevaplar hak vermemeli:');
check('  satır öğesi yok', entitlement({ subscriptionState: 'SUBSCRIPTION_STATE_ACTIVE' }, NOW).stillValid, false);
check('  bitiş tarihi yok', entitlement(sub('SUBSCRIPTION_STATE_ACTIVE', null), NOW).stillValid, false);
check(
  '  bitiş tarihi çözümlenemiyor',
  entitlement(sub('SUBSCRIPTION_STATE_ACTIVE', 'yarın'), NOW).stillValid,
  false
);
check('  cevap null', entitlement(null, NOW).stillValid, false);

console.log('\nBitiş tarihi doğru satır öğesinden okunuyor:');
check(
  '  expiryMs son öğeden',
  entitlement(sub('SUBSCRIPTION_STATE_ACTIVE', FUTURE), NOW).expiryMs,
  Date.parse(FUTURE)
);

console.log('\n\nsubscriptionProductIds\n');
check('  tek öğe', productIds(sub('SUBSCRIPTION_STATE_ACTIVE', FUTURE, 'lite_monthly'))[0], 'lite_monthly');
check('  satır öğesi yok → boş', productIds({}).length, 0);
check('  cevap null → boş', productIds(null).length, 0);
check(
  '  productId eksik olan öğe atlanır',
  productIds({ lineItems: [{ expiryTime: FUTURE }, { productId: 'pro_monthly' }] }).length,
  1
);

console.log('\n\nresolveVerifiedProductId\n');

console.log('İddia Play ile uyuşuyor:');
check('  ürün korunur', verifyProduct('pro_monthly', ['pro_monthly']).productId, 'pro_monthly');
check('  uyuşmazlık yok', verifyProduct('pro_monthly', ['pro_monthly']).mismatch, false);
check('  doğrulandı', verifyProduct('pro_monthly', ['pro_monthly']).verified, true);

// ASIL AÇIK: Lite token'ıyla Pro iddia etmek.
console.log('\nİddia Play ile UYUŞMUYOR (Lite token\'ıyla Pro iddiası):');
check('  Play\'in ürünü kazanır', verifyProduct('pro_monthly', ['lite_monthly']).productId, 'lite_monthly');
check('  uyuşmazlık işaretlenir', verifyProduct('pro_monthly', ['lite_monthly']).mismatch, true);

console.log('\nÇok satır öğesi (ertelenmiş geçiş):');
check(
  '  iddia öğelerden birindeyse korunur',
  verifyProduct('pro_monthly', ['pro_monthly', 'lite_monthly']).productId,
  'pro_monthly'
);
check(
  '  iddia hiçbirinde yoksa SON öğe (bitiş tarihiyle aynı öğe)',
  verifyProduct('gold_large', ['pro_monthly', 'lite_monthly']).productId,
  'lite_monthly'
);

console.log('\nPlay ürün bilgisi vermedi:');
check('  iddiaya düşülür', verifyProduct('pro_monthly', []).productId, 'pro_monthly');
check('  doğrulanmadı olarak işaretlenir', verifyProduct('pro_monthly', []).verified, false);
check('  null liste', verifyProduct('pro_monthly', null).productId, 'pro_monthly');

// ── RTDN'de bilinmeyen token'ın çözülmesi ──────────────────────────────────
//
// Ertelenmiş düşürmede (Pro → Lite) Play YENİ bir purchase token üretiyor. O token
// processedPurchases'ta olmadığı için bildirim "hesaba bağlı değil" diye atlanıyordu ve
// yeni dönem hiç yazılmıyordu: kullanıcı Lite aboneliği aktifken Free görünüyordu.
// Gerçek logda gözlendi:
//   RTDN: token henüz bir hesaba bağlı değil, atlandı { productId: 'lite_monthly' }
const resolveUid = fns._resolveUidForSubscriptionToken;

/** Sahte bağımlılıklar: kayıt tablosu + Play'in bağlı token cevabı. */
function deps(records, links) {
  return {
    readPurchaseRecord: async (token) => records[token] || null,
    readLinkedToken: async (token) => links[token] || null,
  };
}

console.log('\n\nresolveUidForSubscriptionToken\n');

const results = [];
async function run() {
  console.log('Token doğrudan kayıtlı:');
  let r = await resolveUid('T1', deps({ T1: { uid: 'u1' } }, {}));
  check('  uid bulundu', r.uid, 'u1');
  check('  doğrudan', r.via, 'direct');
  check('  sıçrama yok', r.hops, 0);

  console.log('\nYeni token kayıtsız, bağlı token kayıtlı (asıl senaryo):');
  r = await resolveUid('T_new', deps({ T_old: { uid: 'u1' } }, { T_new: 'T_old' }));
  check('  uid bulundu', r.uid, 'u1');
  check('  zincirle çözüldü', r.via, 'linked');
  check('  bir sıçrama', r.hops, 1);

  console.log('\nZincir birden fazla halka (üst üste plan değişikliği):');
  r = await resolveUid(
    'T3',
    deps({ T1: { uid: 'u1' } }, { T3: 'T2', T2: 'T1' })
  );
  check('  uid bulundu', r.uid, 'u1');
  check('  iki sıçrama', r.hops, 2);

  console.log('\nÇözülemeyen durumlar:');
  r = await resolveUid('T_new', deps({}, {}));
  check('  zincir yok → uid null', r.uid, null);
  check('  sebep', r.via, 'unresolved');

  r = await resolveUid('T_new', deps({ T_old: {} }, { T_new: 'T_old' }));
  check('  kayıt var ama uid yok → null', r.uid, null);

  console.log('\nDöngüsel bağ sonsuz döngüye girmemeli:');
  r = await resolveUid('A', deps({}, { A: 'B', B: 'A' }));
  check('  uid null', r.uid, null);
  check('  iki token denendi', r.hops, 2);

  console.log('\nZincir sınırı aşılırsa durmalı:');
  const longLinks = {};
  for (let i = 0; i < 20; i++) longLinks[`L${i}`] = `L${i + 1}`;
  r = await resolveUid('L0', deps({ L19: { uid: 'u1' } }, longLinks));
  check('  sınır aşıldı → uid null', r.uid, null);

  // ── Sahiplik devralma ────────────────────────────────────────────────────
  //
  // Yeni token, planı veren token'ın DEVAMI ise sahipliği devralmalı. Olmazsa
  // ertelenmiş düşürme rütbe kuralına takılıyor (Lite < Pro) ve sonuç, eski dönemin
  // bitişi ile bildirimin gelişi arasındaki saniyelere kalıyordu.
  const owns = fns._ownsStoredPlan;
  const fp = fns._purchaseTokenFingerprint;
  const resolvePlan = fns._resolvePlanUpdate;
  const proFromOld = {
    plan: 'Pro',
    planExpiresAt: Date.now() + 60000,
    planProductId: 'pro_monthly',
    planPurchaseTokenHash: fp('T_old'),
  };

  console.log('\n\nSahiplik devralma (linkedPurchaseToken)\n');
  check(
    '  bağlı token planı veren token → sahip',
    owns(proFromOld, 'T_new', 'lite_monthly', 'T_old'),
    true
  );
  check(
    '  bağ yok → sahip değil',
    owns(proFromOld, 'T_new', 'lite_monthly', null),
    false
  );
  check(
    '  bağ alakasız bir token → sahip değil',
    owns(proFromOld, 'T_new', 'lite_monthly', 'T_baska'),
    false
  );
  check(
    '  ertelenmiş düşürme artık yazılıyor (eski dönem HENÜZ bitmemişken)',
    resolvePlan(proFromOld, 'lite_monthly', 'Lite', 'T_new', 'T_old').write,
    true
  );
  check(
    '  bağsız yabancı token Lite yazamaz (çift abonelik koruması)',
    resolvePlan(proFromOld, 'lite_monthly', 'Lite', 'T_yabanci', null).write,
    false
  );
}

run().then(() => {
  console.log(failures === 0 ? '\nSONUÇ: TÜM KONTROLLER GEÇTİ' : `\nSONUÇ: ${failures} KONTROL BAŞARISIZ`);
  process.exit(failures === 0 ? 0 : 1);
});

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

console.log(failures === 0 ? '\nSONUÇ: TÜM KONTROLLER GEÇTİ' : `\nSONUÇ: ${failures} KONTROL BAŞARISIZ`);
process.exit(failures === 0 ? 0 : 1);

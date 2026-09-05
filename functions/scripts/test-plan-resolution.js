/**
 * resolvePlanUpdate kuralını test eder — Play API'sine ya da Firestore'a hiç dokunmadan.
 *
 * NEDEN AYRI BİR SCRIPT
 *   syncSubscriptionForToken gerçek bir abonelik token'ı olmadan çalıştırılamıyor
 *   (Play'e doğrulatıyor). Ama asıl hatanın kaynağı olan karar — "bu token planı
 *   yazmalı mı" — saf bir fonksiyona ayrıldı ve tek başına test edilebiliyor.
 *
 * ÇALIŞTIRMA
 *   cd functions && node scripts/test-plan-resolution.js
 */
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'numigo-new';
process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:9';

const fns = require('../index');
const resolve = fns._resolvePlanUpdate;

const FUTURE = Date.now() + 30 * 24 * 60 * 60 * 1000;
const PAST = Date.now() - 24 * 60 * 60 * 1000;

let failures = 0;
function check(label, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${actual}${ok ? '' : ` (beklenen: ${expected})`}`);
}

console.log('resolvePlanUpdate\n');

// ── Asıl hata: Lite senkronu aktif Pro'yu ezmemeli ──────────────────────────
console.log('Aktif Pro varken:');
const activePro = { plan: 'Pro', planExpiresAt: FUTURE, planProductId: 'pro_monthly' };
check('  Lite senkronu planı yazmamalı', resolve(activePro, 'lite_monthly', 'Lite').write, false);
check('  Pro senkronu (kendi token\'ı) yazmalı', resolve(activePro, 'pro_monthly', 'Pro').write, true);
check(
  '  Pro sona erdi → kendi token\'ı Free yazabilmeli',
  resolve(activePro, 'pro_monthly', 'Free').write,
  true
);

// ── Yükseltme yönü serbest ──────────────────────────────────────────────────
console.log('\nAktif Lite varken:');
const activeLite = { plan: 'Lite', planExpiresAt: FUTURE, planProductId: 'lite_monthly' };
check('  Pro senkronu yazmalı (yükseltme)', resolve(activeLite, 'pro_monthly', 'Pro').write, true);
check('  Lite senkronu yazmalı (kendi token\'ı)', resolve(activeLite, 'lite_monthly', 'Lite').write, true);

// ── Süresi geçmiş plan engel olmamalı ───────────────────────────────────────
console.log('\nSüresi geçmiş Pro varken (effectivePlan = Free):');
const expiredPro = { plan: 'Pro', planExpiresAt: PAST, planProductId: 'pro_monthly' };
check('  Lite senkronu yazmalı', resolve(expiredPro, 'lite_monthly', 'Lite').write, true);

// ── Plansız kullanıcı ───────────────────────────────────────────────────────
console.log('\nHiç planı olmayan kullanıcı:');
check('  Lite yazmalı', resolve({}, 'lite_monthly', 'Lite').write, true);
check('  Pro yazmalı', resolve({}, 'pro_monthly', 'Pro').write, true);
check('  Free yazmalı (zararsız)', resolve({}, 'lite_monthly', 'Free').write, true);

// ── Sıra bağımsızlığı: asıl senaryonun simülasyonu ──────────────────────────
// refreshPurchases iki aboneliği asenkron işliyor; hangi sıra gelirse gelsin sonuç
// Pro olmalı. Gerçek yazımı taklit ederek zinciri yürütüyoruz.
console.log('\nİki aktif abonelik, her iki sırayla:');
function applySequence(order) {
  let user = {};
  for (const productId of order) {
    const plan = productId === 'pro_monthly' ? 'Pro' : 'Lite';
    if (resolve(user, productId, plan).write) {
      user = { plan, planExpiresAt: FUTURE, planProductId: productId };
    }
  }
  return user.plan;
}
check('  Pro → Lite sırası', applySequence(['pro_monthly', 'lite_monthly']), 'Pro');
check('  Lite → Pro sırası', applySequence(['lite_monthly', 'pro_monthly']), 'Pro');

console.log(failures === 0 ? '\nSONUÇ: TÜM KONTROLLER GEÇTİ' : `\nSONUÇ: ${failures} KONTROL BAŞARISIZ`);
process.exit(failures === 0 ? 0 : 1);

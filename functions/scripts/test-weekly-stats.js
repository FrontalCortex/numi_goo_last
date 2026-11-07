/**
 * Haftalık ilerleme kartının anlık kaydı (weeklyStats.js + index.js → runWeeklyStatsSnapshot).
 *
 * 1. Saf kısım: Türkiye saatiyle ISO hafta, kapanan haftanın seçimi, kaydın gövdesi.
 * 2. Tarama (yalnızca Firestore emülatörü açıkken): aktif/pasif öğrenci, öğretmen, yetim seri
 *    dokümanı; sayımlar (biten ders, geçilen adım) ve aynı haftanın üzerine yazılması.
 *
 * ÇALIŞTIRMA
 *   Yalnız saf kısım:  cd functions && node scripts/test-weekly-stats.js
 *   Taramayla birlikte: cd functions && npx --prefix ../firestore-rules-tests firebase \
 *                        emulators:exec --only firestore --project demo-numigoo \
 *                        "node scripts/test-weekly-stats.js"
 */
const realEmulator = !!process.env.FIRESTORE_EMULATOR_HOST;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'demo-numigoo';
process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:9';

const fns = require('../index');
const ws = require('../weeklyStats');
const admin = require('firebase-admin');

let failures = 0;
function check(label, actual, expected) {
  const a = typeof actual === 'object' ? JSON.stringify(actual) : actual;
  const e = typeof expected === 'object' ? JSON.stringify(expected) : expected;
  const ok = a === e;
  if (!ok) failures++;
  console.log(`${ok ? '  ✓' : '  ✗'} ${label}: ${a}${ok ? '' : ` (beklenen: ${e})`}`);
}
/** Türkiye saatiyle `yyyy-MM-ddTHH:mm` → epoch ms. */
const tr = (s) => Date.parse(`${s}:00+03:00`);

console.log('Hafta kimliği (trWeek, Türkiye saati)');
check('  Pzt 09.11.2026 00:00 → W46', ws.trWeek(tr('2026-11-09T00:00')).id, '2026-W46');
check('  Paz 08.11.2026 23:59 → W45', ws.trWeek(tr('2026-11-08T23:59')).id, '2026-W45');
check('  UTC Pazar 21:30 = TR Pazartesi 00:30 → W46', ws.trWeek(Date.parse('2026-11-08T21:30:00Z')).id, '2026-W46');
check('  W46 başı/sonu', [ws.trWeek(tr('2026-11-12T12:00')).start, ws.trWeek(tr('2026-11-12T12:00')).end], ['2026-11-09', '2026-11-15']);
check('  01.01.2027 Cuma → 2026-W53', ws.trWeek(tr('2027-01-01T12:00')).id, '2026-W53');
check('  04.01.2027 Pazartesi → 2027-W01', ws.trWeek(tr('2027-01-04T12:00')).id, '2027-W01');
check('  29.12.2025 Pazartesi → 2026-W01', ws.trWeek(tr('2025-12-29T12:00')).id, '2026-W01');

console.log('\nKapanan hafta (closingWeek)');
check('  Pzt 00:10 koşusu → biten hafta W45', ws.closingWeek(tr('2026-11-09T00:10')).id, '2026-W45');
check('  Çarşamba elle koşu → içinde olunan hafta', ws.closingWeek(tr('2026-11-11T15:00')).id, '2026-W46');

console.log('\nKaydın gövdesi (buildWeeklySnapshot)');
const week = ws.trWeek(tr('2026-11-12T12:00')); // W46: 09–15.11
const body = ws.buildWeeklySnapshot({
  week,
  todayDay: '2026-11-16',
  user: {
    totalTimeSpent: 9000,
    dailyTimeSpent: { 20261108: 999, 20261109: 600, 20261111: 1200, 20261115: 300, 20261116: 50, 20261110: 'bozuk' },
  },
  cupProgress: { addition_abacus_cup: 340, impact_abacus_cup: 210, bogus: 5, extraction_abacus_cup: -3 },
  ruleStats: { sobiLevel: { r1: 4, m_r1: 2, kotu: 25, yazi: '3' } },
  streak: { current: 6, longest: 9, lastDay: '2026-11-15', recentDays: ['2026-11-08', '2026-11-09', '2026-11-11', '2026-11-15'] },
  finishedLessons: 12,
  passedSteps: 40,
});
check('  hafta', [body.week, body.weekStart, body.weekEnd], ['2026-W46', '2026-11-09', '2026-11-15']);
check('  kupalar (yalnızca geçerli ve tanımlı yollar)', body.cups, { addition_abacus_cup: 340, impact_abacus_cup: 210 });
check('  Sobi seviyeleri (1..20 tam sayı)', body.sobiLevels, { r1: 4, m_r1: 2 });
check('  canlı seri (dün tutturulmuş → canlı)', body.streak, { current: 6, longest: 9 });
check('  hedef günleri yalnızca bu hafta', body.goalDays, ['2026-11-09', '2026-11-11', '2026-11-15']);
check('  günlük süre yalnızca bu hafta, bozuk alan yok', body.dailySeconds, { '2026-11-09': 600, '2026-11-11': 1200, '2026-11-15': 300 });
check('  haftalık süre', body.weekSeconds, 2100);
check('  birikimli sayaçlar', [body.totalTimeSeconds, body.finishedLessons, body.passedSteps], [9000, 12, 40]);
const broken = ws.buildWeeklySnapshot({
  week, todayDay: '2026-11-16', streak: { current: 6, longest: 9, lastDay: '2026-11-12' },
});
check('  kırılmış seri 0 yazılıyor', broken.streak.current, 0);
check('  boş girdi: sayaçlar null (0 uydurulmaz)', [broken.totalTimeSeconds, broken.finishedLessons], [null, null]);

(async () => {
  if (realEmulator) {
    console.log('\nTarama (emülatör)');
    const db = admin.firestore();
    const NOW = tr('2026-11-16T00:10');
    const ts = (ms) => admin.firestore.Timestamp.fromMillis(ms);
    const DAY = 86400000;
    const seed = async (uid, { lastSeenMs, role = 'STUDENT', user = true }) => {
      if (user) await db.doc(`users/${uid}`).set({ role, totalTimeSpent: 100, dailyTimeSpent: { 20261110: 120 } });
      await db.doc(`users/${uid}/streak/state`).set({
        current: 2, longest: 5, lastDay: '2026-11-15', recentDays: ['2026-11-14', '2026-11-15'], lastSeenAt: ts(lastSeenMs),
      });
    };
    await seed('aktif', { lastSeenMs: NOW - 2 * DAY });
    await seed('onDortGun', { lastSeenMs: NOW - 13 * DAY });
    await seed('pasif', { lastSeenMs: NOW - 20 * DAY });
    await seed('ogretmen', { lastSeenMs: NOW - DAY, role: 'TEACHER' });
    await seed('yetim', { lastSeenMs: NOW - DAY, user: false });
    await db.doc('users/aktif/cupWayProgress/progress').set({ addition_abacus_cup: 450 });
    await db.doc('homeworkRuleStats/aktif').set({ sobiLevel: { r1: 7 } });
    await db.doc('users/aktif/lessonProgress/1/items/a').set({ stepIsFinish: true });
    await db.doc('users/aktif/lessonProgress/1/items/b').set({ stepIsFinish: false });
    await db.doc('users/aktif/lessonProgress/4/items/c').set({ stepIsFinish: true });
    await db.doc('users/aktif/lessonSuccessRateState/1_a_1').set({ passed: true });
    await db.doc('users/aktif/lessonSuccessRateState/1_a_2').set({ passed: true });
    await db.doc('users/aktif/lessonSuccessRateState/1_b_1').set({ passed: false });

    const counts = await fns._runWeeklyStatsSnapshot({ nowMs: NOW });
    check('  sayımlar', [counts.week, counts.scanned, counts.written, counts.skipped, counts.failed],
      ['2026-W46', 4, 2, 2, 0]);
    const snap = await db.doc('users/aktif/weeklyStats/2026-W46').get();
    const d = snap.data() || {};
    check('  aktif öğrencinin kaydı var', snap.exists, true);
    check('  kupa, Sobi', [d.cups, d.sobiLevels], [{ addition_abacus_cup: 450 }, { r1: 7 }]);
    check('  biten ders / geçilen adım sayımı', [d.finishedLessons, d.passedSteps], [2, 2]);
    check('  günlük süre ve hedef günleri', [d.dailySeconds, d.goalDays], [{ '2026-11-10': 120 }, ['2026-11-14', '2026-11-15']]);
    check('  capturedAt yazıldı', !!d.capturedAt, true);
    check('  13 gün önce görülen de kaydedildi', (await db.doc('users/onDortGun/weeklyStats/2026-W46').get()).exists, true);
    check('  20 gün önce görülen kaydedilmedi', (await db.doc('users/pasif/weeklyStats/2026-W46').get()).exists, false);
    check('  öğretmen kaydedilmedi', (await db.doc('users/ogretmen/weeklyStats/2026-W46').get()).exists, false);
    check('  kullanıcı dokümanı yoksa kayıt açılmadı', (await db.doc('users/yetim/weeklyStats/2026-W46').get()).exists, false);

    await db.doc('users/aktif/cupWayProgress/progress').set({ addition_abacus_cup: 470 });
    await fns._runWeeklyStatsSnapshot({ nowMs: NOW + 60000 });
    const again = (await db.doc('users/aktif/weeklyStats/2026-W46').get()).data();
    check('  aynı hafta ikinci koşu son hâli yazıyor', again.cups.addition_abacus_cup, 470);

    const many = await fns._runWeeklyStatsSnapshot({ nowMs: NOW, deadlineMs: Date.now() - 1 });
    check('  süre dolmuşsa yarım bırakıp işaretliyor', [many.truncated, many.written], [true, 0]);
  } else {
    console.log('\n(Emülatör yok: tarama testi atlandı.)');
  }
  console.log(failures === 0 ? '\nHepsi geçti.' : `\n${failures} hata.`);
  process.exit(failures === 0 ? 0 : 1);
})().catch((e) => { console.error(e); process.exit(1); });

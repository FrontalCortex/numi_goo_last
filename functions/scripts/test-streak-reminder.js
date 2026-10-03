/**
 * Akşam hatırlatmasının karar mantığı testi.
 *
 * Emülatör gerektirmiyor: kararın tamamı yan etkisiz (bkz. ../streakReminder.js).
 *
 * ÇALIŞTIRMA
 *   node functions/scripts/test-streak-reminder.js
 */
const {
  localDayId,
  normalizeReminderHour,
  reminderHourUtc,
  streakReminderDecision,
} = require('../streakReminder');

let pass = 0;
let fail = 0;
function check(name, actual, expected) {
  const ok = actual === expected;
  if (ok) pass++;
  else fail++;
  console.log(`${ok ? 'OK  ' : 'HATA'} ${name}${ok ? '' : ` (beklenen ${expected}, gelen ${actual})`}`);
}

console.log('=== HATIRLATMA SAATİ (yerel 19:00 → UTC) ===');
for (const [name, offset, expected] of [
  ['Türkiye +3', 180, 16],
  ['UTC', 0, 19],
  ['Kaliforniya -7', -420, 2],
  ['Japonya +9', 540, 10],
  ['Hindistan +5:30', 330, 13],
  ['Yeni Zelanda +13', 780, 6],
]) {
  check(name, reminderHourUtc(offset), expected);
}

console.log('\n=== SEÇİLEN SAAT (kayıttaki hatırlatma sorusu) ===');
for (const [name, offset, localHour, expected] of [
  ['Türkiye, 16:00', 180, 16, 13],
  ['Türkiye, 20:00', 180, 20, 17],
  ['Türkiye, 01:00 (gün geri sarıyor)', 180, 1, 22],
  ['UTC, 18:00', 0, 18, 18],
  ['Hindistan +5:30, 16:00', 330, 16, 10],
]) {
  check(name, reminderHourUtc(offset, localHour), expected);
}
check('saat verilmezse 19:00', reminderHourUtc(180), reminderHourUtc(180, 19));

console.log('\n=== SAAT DOĞRULAMA ===');
for (const [name, raw, expected] of [
  ['16', 16, 16],
  ['0 (gece yarısı geçerli)', 0, 0],
  ['23', 23, 23],
  ['24 geçersiz', 24, null],
  ['-1 geçersiz', -1, null],
  ['18.5 (dakika yok)', 18.5, null],
  ['metin "20" kabul', '20', 20],
  ['null → seçim yok', null, null],
  ['undefined → seçim yok', undefined, null],
  ['boş metin → seçim yok', '', null],
]) {
  check(name, normalizeReminderHour(raw), expected);
}

console.log('\n=== KİME GÖNDERİLİR ===');
const OFFSET = 180; // Türkiye
const NOW = Date.parse('2026-09-23T16:00:00Z'); // yerel 19:00
const TODAY = localDayId(NOW, OFFSET);
const daysAgo = (n) => NOW - n * 86400000;

const CASES = [
  ['Dün çalışmış, bugün henüz yok', { utcOffsetMinutes: OFFSET, lastDay: '2026-09-22', current: 4, goalMinutes: 5, lastSeenMs: daysAgo(1) }, true],
  ['Bugün hedefini tutturmuş', { utcOffsetMinutes: OFFSET, lastDay: TODAY, current: 5, goalMinutes: 5, lastSeenMs: daysAgo(0) }, false],
  ['Bugün zaten hatırlatılmış', { utcOffsetMinutes: OFFSET, lastDay: '2026-09-22', reminderSentDay: TODAY, current: 4, goalMinutes: 5, lastSeenMs: daysAgo(1) }, false],
  ['3 gündür yok — hâlâ hatırlatılır', { utcOffsetMinutes: OFFSET, lastDay: '2026-09-20', current: 0, goalMinutes: 5, lastSeenMs: daysAgo(3) }, true],
  ['10 gündür yok — dürtülmez', { utcOffsetMinutes: OFFSET, lastDay: '2026-09-10', current: 0, goalMinutes: 5, lastSeenMs: daysAgo(10) }, false],
  ['Saat dilimi bilinmiyor', { lastDay: '2026-09-22', current: 4, goalMinutes: 5, lastSeenMs: daysAgo(1) }, false],
  ['Yeni kullanıcı (lastSeen yok)', { utcOffsetMinutes: OFFSET, lastDay: '', current: 0, goalMinutes: 10, lastSeenMs: null }, true],
];
for (const [name, state, expected] of CASES) {
  check(name, streakReminderDecision(state, NOW).send, expected);
}

console.log('\n=== METİN ===');
const alive = streakReminderDecision(
  { utcOffsetMinutes: OFFSET, lastDay: '2026-09-22', current: 12, goalMinutes: 10, lastSeenMs: daysAgo(1) },
  NOW
);
check('seri varsa seri sayısı geçiyor', alive.text.body.includes('12 günlük'), true);
const fresh = streakReminderDecision(
  { utcOffsetMinutes: OFFSET, lastDay: '', current: 0, goalMinutes: 20, lastSeenMs: null },
  NOW
);
check('seri yoksa yeniden başlamaya davet', fresh.text.body.includes('yeni serini'), true);
check('hedef dakikası metne giriyor', fresh.text.body.includes('20 dakika'), true);

console.log(`\n${pass} geçti, ${fail} kaldı`);
process.exit(fail > 0 ? 1 : 0);

/**
 * Bildirim çekirdeğinin karar mantığı testi.
 *
 * NEDEN VAR
 *   Karar dört kuralın kesişimi (tercih, sessiz saat, tavan, açılmayan konu) ve yanlış
 *   davranışı sahada GÖRÜNMÜYOR: gitmeyen bildirim hata üretmiyor, yalnızca eksik kalıyor.
 *   Sessiz saat ve tavan senaryolarını cihazda denemek için günlerce beklemek ya da saat
 *   dilimi değiştirmek gerekir; burada hepsi saniyeler içinde koşuyor.
 *
 * ÇALIŞTIRMA: cd functions && npm run test:notifications
 */
const {
  DAILY_CAP,
  WEEKLY_CAP,
  RESERVED_FOR_PRIORITY,
  UNOPENED_MUTE_AFTER,
  dayKey,
  isQuietHour,
  localHourOf,
  notificationPrefsFor,
  recentCounts,
  notificationDecision,
  ledgerPatch,
  openPatch,
} = require('../notifications');

let pass = 0;
let fail = 0;
function check(name, actual, expected) {
  const ok = actual === expected;
  if (ok) pass++;
  else fail++;
  console.log(`${ok ? 'OK  ' : 'HATA'} ${name}${ok ? '' : ` (beklenen ${expected}, gelen ${actual})`}`);
}

/** 2026-10-05, 12:00 UTC — sessiz saatlerin dışında bir öğle vakti. */
const NOON = Date.UTC(2026, 9, 5, 12, 0, 0);
/** 2026-10-05, 23:00 UTC — UTC kullanıcısı için gece. */
const NIGHT = Date.UTC(2026, 9, 5, 23, 0, 0);

function decide(type, state, nowMs = NOON, topic = null) {
  return notificationDecision({ type, topic, state }, nowMs);
}

console.log('=== YEREL SAAT VE SESSİZ SAAT ===');
check('UTC öğlen', localHourOf(NOON, 0), 12);
check('Türkiye +3 öğlen UTC → 15', localHourOf(NOON, 180), 15);
check('Endonezya +7 gece 23 UTC → 6 (ertesi sabah)', localHourOf(NIGHT, 420), 6);
check('20:00 sessiz değil', isQuietHour(20), false);
check('21:00 sessiz', isQuietHour(21), true);
check('03:00 sessiz', isQuietHour(3), true);
check('08:00 sessiz değil', isQuietHour(8), false);
check('07:00 sessiz', isQuietHour(7), true);

console.log('\n=== TERCİHLER (geriye dönük uyum) ===');
check('alan hiç yok → sohbet açık', notificationPrefsFor({}).chat, true);
check(
  'eski tek anahtar kapalı → ödül kapalı',
  notificationPrefsFor({ notificationsEnabled: false }).reward,
  false
);
check(
  'eski anahtar kapalı ama yeni tercih açık → yeni kazanır',
  notificationPrefsFor({ notificationsEnabled: false, notificationPrefs: { chat: true } }).chat,
  true
);
check(
  'eski anahtar açık ama yeni tercih kapalı → yeni kazanır',
  notificationPrefsFor({ notificationsEnabled: true, notificationPrefs: { chat: false } }).chat,
  false
);
check(
  'kısmi tercih: belirtilmeyen tür eski anahtara düşer',
  notificationPrefsFor({ notificationsEnabled: false, notificationPrefs: { chat: true } }).reward,
  false
);

console.log('\n=== TÜR BAZLI TERCİH ===');
const allOn = { prefs: notificationPrefsFor({}), utcOffsetMinutes: 0 };
check('sohbet açıkken gider', decide('chat', allOn).send, true);
check('bilinmeyen tür', decide('gibberish', allOn).reason, 'unknown_type');
check(
  'ödül kapalıyken ödül gitmez',
  decide('reward', { ...allOn, prefs: { ...allOn.prefs, reward: false } }).reason,
  'pref_off'
);
check(
  'ödül kapalı sohbeti ETKİLEMEZ — asıl mesele bu',
  decide('chat', { ...allOn, prefs: { ...allOn.prefs, reward: false } }).send,
  true
);
check(
  'hesap bildirimi uygulama içinden kapatılamıyor',
  decide('account', { ...allOn, prefs: { chat: false, streak: false, reward: false } }).send,
  true
);

console.log('\n=== SESSİZ SAATLER ===');
// UTC kullanıcısı için 23:00 gece.
const atNight = { prefs: notificationPrefsFor({}), utcOffsetMinutes: 0 };
check('gece sohbet gider', decide('chat', atNight, NIGHT).send, true);
check('ama sessiz kanala düşer', decide('chat', atNight, NIGHT).channel, 'messages_quiet');
check('gündüz normal kanal', decide('chat', atNight, NOON).channel, 'messages');
check('gece ödül düşer', decide('reward', atNight, NIGHT).reason, 'quiet_hours');
check('gece seri hatırlatması gider (saati kullanıcı seçti)', decide('streak', atNight, NIGHT).send, true);
check('gece hesap bildirimi gider', decide('account', atNight, NIGHT).send, true);
// Endonezya (+7) için aynı UTC anı sabah 06:00 → hâlâ sessiz saat (08:00'den önce).
check(
  'saat dilimi dikkate alınıyor: +7 için 06:00 hâlâ sessiz',
  decide('reward', { ...atNight, utcOffsetMinutes: 420 }, NIGHT).reason,
  'quiet_hours'
);
// +9 için aynı an 08:00 → sessiz saat bitti.
check(
  '+9 için 08:00 sessiz değil',
  decide('reward', { ...atNight, utcOffsetMinutes: 540 }, NIGHT).send,
  true
);
// `Number(null) === 0` tuzağı: farkı bilinmeyen kullanıcı UTC sanılırsa gece bildirimi
// düşürülür. İkisi zıt davranmalı.
check(
  'saat dilimi null → bildirim düşmez',
  decide('reward', { prefs: notificationPrefsFor({}), utcOffsetMinutes: null }, NIGHT).send,
  true
);
check(
  'saat dilimi alanı hiç yok → bildirim düşmez',
  decide('reward', { prefs: notificationPrefsFor({}) }, NIGHT).send,
  true
);
check(
  'saat dilimi boş metin → bildirim düşmez',
  decide('reward', { prefs: notificationPrefsFor({}), utcOffsetMinutes: '' }, NIGHT).send,
  true
);
check(
  'ama GERÇEKTEN UTC olan (0) kullanıcıya sessiz saat uygulanır',
  decide('reward', { prefs: notificationPrefsFor({}), utcOffsetMinutes: 0 }, NIGHT).reason,
  'quiet_hours'
);

console.log('\n=== GÜNLÜK VE HAFTALIK TAVAN ===');
const today = dayKey(NOON);
const fullDay = {};
fullDay[today] = DAILY_CAP;
check('günlük tavan dolu', decide('reward', { ...allOn, history: fullDay }).reason, 'daily_cap');
check(
  'tavan dolu olsa bile sohbet gider (işlemsel)',
  decide('chat', { ...allOn, history: fullDay }).send,
  true
);
check(
  'tavan dolu olsa bile hesap bildirimi gider',
  decide('account', { ...allOn, history: fullDay }).send,
  true
);
check(
  'tavan dolu olsa bile seri hatırlatması gider (kullanıcının kurduğu alarm)',
  decide('streak', { ...allOn, history: fullDay }).send,
  true
);

// Haftalık tavanı günlük tavana takılmadan test et: bugüne hiç bildirim koymadan geçmiş
// günlere yay. Yani günlük tavan boşken haftalık dolu.
const spread = {};
spread[dayKey(NOON - 86400000)] = 2;
for (let i = 2; i <= WEEKLY_CAP - 1; i++) spread[dayKey(NOON - i * 86400000)] = 1;
check('haftalık sayım doğru', recentCounts(spread, NOON).week, WEEKLY_CAP);
check('bugün boş', recentCounts(spread, NOON).day, 0);
check('haftalık tavan dolu', decide('reward', { ...allOn, history: spread }).reason, 'weekly_cap');
check(
  'haftalık tavan dolu olsa bile sohbet gider',
  decide('chat', { ...allOn, history: spread }).send,
  true
);

console.log('\n=== ÖNCELİK: REZERVE SLOT ===');
// Normal öncelikli bir ödül bildirimi tavanın SON slotunu kullanamıyor; o slot
// `season_ending` gibi öncelikli konulara ayrılmış.
const oneUsed = {};
oneUsed[today] = DAILY_CAP - RESERVED_FOR_PRIORITY;
check(
  'normal ödül bildirimi rezerve slotu kullanamaz',
  decide('reward', { ...allOn, history: oneUsed }, NOON, 'chest_waiting').reason,
  'daily_cap'
);
check(
  'öncelikli konu rezerve slotu kullanır',
  decide('reward', { ...allOn, history: oneUsed }, NOON, 'season_ending').send,
  true
);
check(
  'öncelikli konu da tavanın TAMAMINI aşamaz',
  decide('reward', { ...allOn, history: fullDay }, NOON, 'season_ending').reason,
  'daily_cap'
);

// KULLANICI KARARI (05.10.2026): sezonun son günü "sezon bitiyor" ve seri hatırlatması
// geçecek, madalya bildirimi düşecek. Günün gerçek sırasıyla kurulan senaryo.
console.log('\n--- sezonun son günü, gerçek sırayla ---');
let ledger = {};
const seasonDay = decide('reward', { ...allOn, history: ledger.history }, NOON, 'season_ending');
check('08:05 "sezon bitiyor" geçer', seasonDay.send, true);
ledger = ledgerPatch(ledger, seasonDay, NOON);

const medals = decide('reward', { ...allOn, history: ledger.history }, NOON, 'season_medals');
check('12:05 "madalyan hazır" DÜŞER (rezerve slot)', medals.reason, 'daily_cap');

const evening = decide('streak', { ...allOn, history: ledger.history }, NOON);
check('akşam seri hatırlatması geçer (tavansız)', evening.send, true);

// Ters sıra: madalya sabah gelse bile sezon bildirimi rezerve slota giriyor.
console.log('--- ters sıra: madalya önce ---');
let ledger2 = ledgerPatch({}, decide('reward', allOn, NOON, 'season_medals'), NOON);
check(
  'madalya önce gittiyse "sezon bitiyor" yine geçer',
  decide('reward', { ...allOn, history: ledger2.history }, NOON, 'season_ending').send,
  true
);
check(
  've seri hatırlatması yine geçer',
  decide('streak', { ...allOn, history: ledger2.history }, NOON).send,
  true
);
// Sekiz gün öncesi pencerenin dışında kalmalı.
const old = {};
old[dayKey(NOON - 8 * 86400000)] = 99;
check('sekiz gün öncesi sayılmaz', recentCounts(old, NOON).week, 0);

console.log('\n=== AÇILMAYAN KONU KENDİLİĞİNDEN SUSAR ===');
const muted = { ...allOn, unopened: { season_ending: UNOPENED_MUTE_AFTER } };
check(
  `${UNOPENED_MUTE_AFTER} kez açılmayan konu susar`,
  decide('reward', muted, NOON, 'season_ending').reason,
  'topic_muted'
);
check(
  'bir altı hâlâ gönderilir',
  decide('reward', { ...allOn, unopened: { season_ending: UNOPENED_MUTE_AFTER - 1 } }, NOON, 'season_ending').send,
  true
);
check(
  'susan konu ÖTEKİ konuyu etkilemez — tür değil konu bazında',
  decide('reward', muted, NOON, 'chest_waiting').send,
  true
);
// Susturma yalnızca `reward`'da. Ötekilerde "dokunulmadı" ile "işe yaramadı" aynı şey değil:
// bildirimi görüp uygulamayı kendi açan kullanıcı hiç dokunmamış sayılıyor.
check(
  'seri hatırlatması susturulamaz — işini yaptığı için kapanmasın',
  decide('streak', { ...allOn, unopened: { streak_reminder: 99 } }, NOON, 'streak_reminder').send,
  true
);
check(
  'sohbet bildirimi susturulamaz',
  decide('chat', { ...allOn, unopened: { chat: 99 } }, NOON, 'chat').send,
  true
);
check(
  'hesap bildirimi susturulamaz',
  decide('account', { ...allOn, unopened: { trial_ending: 99 } }, NOON, 'trial_ending').send,
  true
);

console.log('\n=== DEFTER ===');
const d1 = decide('reward', allOn, NOON, 'chest_waiting');
const p1 = ledgerPatch({}, d1, NOON);
check('gönderim bugüne sayılır', p1.history[today], 1);
check('konu gönderim sayacı', p1.sent.chest_waiting, 1);
check('açılmadı sayacı artar', p1.unopened.chest_waiting, 1);

const p2 = ledgerPatch(p1, d1, NOON);
check('ikinci gönderim birikir', p2.history[today], 2);
check('açılmadı birikir', p2.unopened.chest_waiting, 2);

// Sohbet gibi tavansız tür güne SAYILMAMALI, yoksa öğretmenle yoğun bir sohbet akşamın
// hatırlatmasını yutardı.
const chatPatch = ledgerPatch(p1, decide('chat', allOn), NOON);
check('tavansız tür güne sayılmaz', chatPatch.history[today], 1);
check('ama gönderim sayacı yine tutulur', chatPatch.sent.chat, 1);

const stale = { history: { '2020-01-01': 5 }, sent: {}, unopened: {} };
check('eski gün anahtarı defterden düşer', ledgerPatch(stale, d1, NOON)['history']['2020-01-01'], undefined);

const afterOpen = openPatch(p2, 'chest_waiting');
check('açılma sayılır', afterOpen.opened.chest_waiting, 1);
check('açılmadı sayacı sıfırlanır', afterOpen.unopened.chest_waiting, 0);
check(
  'sıfırlanan konu yeniden gönderilebilir',
  decide('reward', { ...allOn, unopened: afterOpen.unopened }, NOON, 'chest_waiting').send,
  true
);

console.log(`\n${fail === 0 ? 'TÜMÜ GEÇTİ' : 'BAŞARISIZ'} — ${pass} geçti, ${fail} hata`);
process.exit(fail === 0 ? 0 : 1);

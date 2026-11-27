/**
 * Haftalık ilerleme kartının ANLIK KAYDI (snapshot): haftanın sonunda öğrencinin durumunun
 * fotoğrafı, `users/{uid}/weeklyStats/{yyyy-Www}`.
 *
 * NEDEN ŞİMDİDEN: Kart "geçen hafta → bu hafta" diyecek. Kupa puanı, Sobi seviyesi ve biten ders
 * sayısı yalnızca BUGÜNKÜ değerleriyle saklanıyor; geçen haftaki hâlleri kaybolmuş oluyor. Kart
 * ekranı gelmeden kayıt birikmezse ilk kartın kıyaslayacağı bir "önce" olmaz.
 *
 * NEDEN HAM DEĞER: Kupa puanını "1 basamaklı 4 sayı" gibi bir cümleye çevirmek karta ait
 * (istemcideki CupRuleEngine aralıkları sürümle değişebilir). Kayıt ham puanı tutuyor; böylece
 * kural değişse de eski haftalar yeni kuralla yeniden okunabiliyor.
 *
 * Bu dosyada yalnızca yan etkisiz hesap var (hafta kimliği, kaydın gövdesi): emülatörsüz,
 * saniyeler içinde test edilebilsin diye (bkz. scripts/test-weekly-stats.js). Firestore taraması
 * index.js → runWeeklyStatsSnapshot.
 */

const { liveStreak } = require('./streakReminder');

/** Kaydın biçim sürümü: kart eski kayıtları ayırt edebilsin diye. Alan anlamı değişirse artır. */
const WEEKLY_STATS_SCHEMA = 1;

/** "Aktif öğrenci": son bu kadar günde uygulamayı açmış (kullanıcı kararı 08.11.2026: 14 gün).
 *  7 değil 14: bir hafta ara veren çocuk döndüğünde kıyaslanacak bir "geçen hafta" kaydı olsun. */
const WEEKLY_STATS_ACTIVE_DAYS = 14;

/** Türkiye saati UTC+3, 2016'dan beri yaz saati yok (homework.trDayKey ile aynı varsayım). */
const TR_OFFSET_MS = 3 * 3600 * 1000;

/**
 * Zamanlayıcı Pazartesi 00:10'da çalışıyor (kullanıcı kararı: Pzt–Paz hafta). O an yeni haftanın
 * içinde; kaydedilen ise az önce KAPANAN hafta. Bu pay, "çalışma anı"ndan kapanan haftaya geri
 * gitmek için: 00:10'dan 1 saat önce Pazar 23:10. Elle (ör. deploy sonrası taban kaydı için)
 * hafta içinde çalıştırılırsa yine o anın içinde olduğu hafta seçilir — Pazartesi koşusu da
 * aynı haftanın son hâlini üzerine yazar.
 */
const CLOSING_MARGIN_MS = 60 * 60 * 1000;

const CUP_FIELDS = [
  'addition_abacus_cup',
  'extraction_abacus_cup',
  'impact_abacus_cup',
  'blinding_addition_abacus_cup',
  'blinding_extraction_abacus_cup',
  'blinding_impact_abacus_cup',
];

/** `yyyy-MM-dd` (UTC tarih nesnesinden). */
function isoDay(date) {
  return date.toISOString().slice(0, 10);
}

/**
 * Türkiye saatiyle [ms] anının ISO haftası: `{ id: '2026-W46', start: '2026-11-09', end: '2026-11-15' }`.
 *
 * ISO 8601: hafta Pazartesi başlar; yılın ilk haftası, içinde Perşembe olan haftadır. Yıl sınırında
 * (ör. 31.12.2026 Perşembe → 2026-W53, 01.01.2027 Cuma → yine 2026-W53) kimlik takvim yılından
 * farklı olabilir; bu yüzden yıl, haftanın Perşembesinden alınıyor.
 */
function trWeek(ms) {
  const local = new Date(ms + TR_OFFSET_MS);
  const day = new Date(Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate()));
  const dow = (day.getUTCDay() + 6) % 7; // Pazartesi 0 … Pazar 6
  const monday = new Date(day.getTime() - dow * 86400000);
  const thursday = new Date(monday.getTime() + 3 * 86400000);
  const year = thursday.getUTCFullYear();
  const jan1 = Date.UTC(year, 0, 1);
  const week = Math.floor((thursday.getTime() - jan1) / 86400000 / 7) + 1;
  const sunday = new Date(monday.getTime() + 6 * 86400000);
  return {
    id: `${year}-W${String(week).padStart(2, '0')}`,
    start: isoDay(monday),
    end: isoDay(sunday),
  };
}

/** Zamanlayıcının çalıştığı andan kaydedilecek (kapanan) hafta. */
function closingWeek(nowMs) {
  return trWeek(nowMs - CLOSING_MARGIN_MS);
}

/** Haftanın yedi günü `yyyy-MM-dd` olarak, Pazartesiden Pazara. */
function weekDays(week) {
  const start = Date.parse(`${week.start}T00:00:00Z`);
  return Array.from({ length: 7 }, (_, i) => isoDay(new Date(start + i * 86400000)));
}

/** Sıfır ya da pozitif tam sayı; değilse null (bozuk alan kayda "0" diye girmesin). */
function nonNegInt(v) {
  // Number(null) 0 veriyor; "hiç okunmadı" sıfır sanılmasın.
  if (v === null || v === undefined || v === '') return null;
  const n = Number(v);
  return Number.isFinite(n) && n >= 0 ? Math.trunc(n) : null;
}

/**
 * Kaydın gövdesi. Girdiler Firestore'dan okunmuş HAM doküman verileri; eksik olan `{}`.
 *
 * Alanlar:
 *   cups        — altı kupa yolunun puanı; dokümanda olmayan yol yazılmaz (hiç oynanmamış yolu
 *                 başlangıç puanı 200 ile "oynanmış" göstermemek için; kart eksiği 200 sayabilir).
 *   sobiLevels  — homeworkRuleStats.sobiLevel aynen (kural → 1..20).
 *   streak      — o günkü CANLI seri (kırılmışsa 0; saklanan `current` kırılmayı yazmıyor) ve en uzun.
 *   goalDays    — haftada günlük hedefin tutturulduğu günler (streak.recentDays'ten).
 *   dailySeconds— haftanın her günü için çalışma süresi (users.dailyTimeSpent, 14 gün saklanıyor;
 *                 anahtar cihazın yerel günü `yyyyMMdd`). Olmayan gün yazılmaz.
 *   weekSeconds — o günlerin toplamı.
 *   totalTimeSeconds, finishedLessons, passedSteps — birikimli sayaçlar; haftalık fark kartta,
 *                 iki kaydın farkından.
 *
 * @param week      trWeek/closingWeek sonucu.
 * @param todayDay  Çalışma anının Türkiye günü (`yyyy-MM-dd`), canlı seri için.
 */
function buildWeeklySnapshot({
  week,
  todayDay,
  user = {},
  cupProgress = {},
  ruleStats = {},
  streak = {},
  finishedLessons = null,
  passedSteps = null,
}) {
  const cups = {};
  for (const f of CUP_FIELDS) {
    const v = nonNegInt(cupProgress[f]);
    if (v !== null) cups[f] = v;
  }

  const sobiLevels = {};
  const rawLevels = ruleStats.sobiLevel;
  if (rawLevels && typeof rawLevels === 'object' && !Array.isArray(rawLevels)) {
    for (const [k, v] of Object.entries(rawLevels)) {
      if (Number.isInteger(v) && v >= 1 && v <= 20) sobiLevels[k] = v;
    }
  }

  const days = weekDays(week);
  const daySet = new Set(days);
  const goalDays = (Array.isArray(streak.recentDays) ? streak.recentDays : [])
    .filter((d) => typeof d === 'string' && daySet.has(d))
    .sort();

  const daily = user.dailyTimeSpent && typeof user.dailyTimeSpent === 'object' ? user.dailyTimeSpent : {};
  const dailySeconds = {};
  let weekSeconds = 0;
  for (const d of days) {
    const v = nonNegInt(daily[d.replace(/-/g, '')]);
    if (v !== null && v > 0) {
      dailySeconds[d] = v;
      weekSeconds += v;
    }
  }

  const live = liveStreak(
    { current: streak.current, freezes: streak.freezes, lastDay: streak.lastDay, freezeDay: streak.freezeDay },
    todayDay
  );

  return {
    schema: WEEKLY_STATS_SCHEMA,
    week: week.id,
    weekStart: week.start,
    weekEnd: week.end,
    cups,
    sobiLevels,
    streak: { current: live.current, longest: nonNegInt(streak.longest) ?? 0 },
    goalDays,
    dailySeconds,
    weekSeconds,
    totalTimeSeconds: nonNegInt(user.totalTimeSpent),
    finishedLessons: nonNegInt(finishedLessons),
    passedSteps: nonNegInt(passedSteps),
  };
}

/** Türkiye günü `yyyy-MM-dd`. */
function trDay(ms) {
  return new Date(ms + TR_OFFSET_MS).toISOString().slice(0, 10);
}

module.exports = {
  WEEKLY_STATS_SCHEMA,
  WEEKLY_STATS_ACTIVE_DAYS,
  CUP_FIELDS,
  trWeek,
  closingWeek,
  weekDays,
  trDay,
  buildWeeklySnapshot,
};

/**
 * Seri dondurma kuralı: kaçan ilk günü kapatır, seriye gün eklemez, geçmişi onarmaz.
 *
 * NEDEN VAR
 *   Kural hem altın (4000) hem seri ödülü dağıtan sayaçla ilgili ve iki ayrı yerde yaşıyor:
 *   burada (functions/index.js) ve istemcide (StreakFreezeRules.kt). Cihazda denemek için
 *   günlerce beklemek ya da saati oynatmak gerekiyor — saat oynatmak daha önce seriyi
 *   kalıcı olarak dondurmuştu (bkz. StreakRepository.refresh, BOZUK_LASTDAY). Bu yüzden
 *   senaryolar burada, saniyeler içinde koşuyor.
 *
 *   Fonksiyonlar KAYNAKTAN çekilip çalıştırılıyor, kopyaları test edilmiyor. İstemcideki
 *   ikizin testi aynı senaryoları kullanıyor: app/src/test/.../StreakFreezeRulesTest.kt.
 *   Buraya senaryo eklenirse oraya da eklenmeli.
 *
 * ÇALIŞTIRMA: cd functions && npm run test:streak-freeze
 */
const fs = require('fs');
const path = require('path');

const src = fs.readFileSync(path.join(__dirname, '..', 'index.js'), 'utf8');

function grabFunction(name) {
  const marker = `function ${name}(`;
  const i = src.indexOf(marker);
  if (i < 0) throw new Error(`kaynakta bulunamadı: ${name}`);
  let depth = 0;
  let started = false;
  for (let j = i; j < src.length; j++) {
    if (src[j] === '{') { depth++; started = true; }
    else if (src[j] === '}') { depth--; if (started && depth === 0) return src.slice(i, j + 1); }
  }
  throw new Error(`kapanmadı: ${name}`);
}

function grabConst(name) {
  const m = src.match(new RegExp(`^const ${name} = .*;$`, 'm'));
  if (!m) throw new Error(`kaynakta bulunamadı: const ${name}`);
  return m[0];
}

const api = new Function(
  [
    grabConst('STREAK_DAY_RE'),
    grabConst('STREAK_DAY_TOLERANCE_DAYS'),
    grabConst('STREAK_RECENT_DAYS_KEPT'),
    grabConst('STREAK_FREEZE_COST'),
    grabConst('STREAK_FREEZE_MAX_HELD'),
    grabFunction('streakDayNumber'),
    grabFunction('streakDayId'),
    grabFunction('settleStreakFreeze'),
    grabFunction('freezePatch'),
    grabFunction('clientTodayNo'),
    grabFunction('applyStreakDays'),
    'return { streakDayNumber, streakDayId, settleStreakFreeze, freezePatch, clientTodayNo,' +
      ' applyStreakDays, STREAK_FREEZE_COST, STREAK_FREEZE_MAX_HELD, STREAK_RECENT_DAYS_KEPT };',
  ].join('\n')
)();

let pass = 0;
let fail = 0;
function check(name, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a === e) { console.log('  OK   ' + name); pass++; }
  else { console.log(`  HATA ${name}\n       beklenen ${e}\n       gelen    ${a}`); fail++; }
}

// Günler: P = pazartesi. Seri pazar günü (SUN) bitmiş bir günle başlıyor.
const SUN = '2026-10-04';
const MON = '2026-10-05';
const TUE = '2026-10-06';
const WED = '2026-10-07';
const THU = '2026-10-08';
const no = api.streakDayNumber;

/** Seri durumu: 5 günlük seri pazar günü tutturulmuş, elde 1 dondurma (cumartesi alınmış). */
function held(overrides) {
  return Object.assign(
    { current: 5, longest: 5, lastDay: SUN, claimed: [3], freezes: 1, freezeDay: '2026-10-03', frozenDays: [] },
    overrides || {}
  );
}

/** Yalnızca dondurmayla ilgili alanlar; karşılaştırmayı okunur tutuyor. */
function pick(s) {
  return { current: s.current, lastDay: s.lastDay, freezes: s.freezes, freezeDay: s.freezeDay, frozenDays: s.frozenDays };
}

console.log('\n=== GUN <-> SAYI ===');
check('streakDayId, streakDayNumber in tersi', api.streakDayId(no(SUN)), SUN);
check('ay sonu dogru ilerliyor', api.streakDayId(no('2026-10-31') + 1), '2026-11-01');

console.log('\n=== HARCANMAYAN DURUMLAR (ayni nesne donmeli) ===');
{
  const s = held();
  check('dun tutturulmus, kacan gun yok', api.settleStreakFreeze(s, no(MON)) === s, true);
  check('bugun tutturulmus', api.settleStreakFreeze(s, no(SUN)) === s, true);
  check('referans gun yok (eski istemci)', api.settleStreakFreeze(s, null) === s, true);
  const none = held({ freezes: 0, freezeDay: '' });
  check('dondurma yok', api.settleStreakFreeze(none, no(TUE)) === none, true);
  const dead = held({ current: 0 });
  check('seri yok (current=0): korunacak bir sey yok', api.settleStreakFreeze(dead, no(TUE)) === dead, true);
  const empty = held({ lastDay: '' });
  check('hic gun bildirilmemis', api.settleStreakFreeze(empty, no(TUE)) === empty, true);
  const future = held({ lastDay: WED });
  check('son gun ileride (saat dilimi payi)', api.settleStreakFreeze(future, no(TUE)) === future, true);
}

console.log('\n=== TEK GUN KACTI: seri kurtulur ===');
check(
  'pazartesi kacti, sali acildi -> pazartesi kopru, seri 5 kaliyor',
  pick(api.settleStreakFreeze(held(), no(TUE))),
  { current: 5, lastDay: MON, freezes: 0, freezeDay: '', frozenDays: [MON] }
);

console.log('\n=== IKI GUN KACTI: dondurma YINE harcanir (urun karari) ===');
check(
  'pazartesi+sali kacti, carsamba acildi -> yalnizca pazartesi kapanir',
  pick(api.settleStreakFreeze(held(), no(WED))),
  { current: 5, lastDay: MON, freezes: 0, freezeDay: '', frozenDays: [MON] }
);

console.log('\n=== GECMISI ONARMAZ: satin alma gununden oncesi kapanmaz ===');
{
  // Pazartesi kacti (dondurma yoktu), sali satin alindi. Sunucuda current hala 5.
  const boughtLate = held({ freezeDay: TUE });
  check(
    'kacan gun satin almadan once -> harcanmaz',
    api.settleStreakFreeze(boughtLate, no(TUE)) === boughtLate,
    true
  );
  // Pazartesi sabah alindi, pazartesi kacirildi: kacan gun satin alma gunu, kapanir.
  check(
    'satin alindigi gun kacirilirsa kapanir',
    pick(api.settleStreakFreeze(held({ freezeDay: MON }), no(TUE))),
    { current: 5, lastDay: MON, freezes: 0, freezeDay: '', frozenDays: [MON] }
  );
}

console.log('\n=== DONMUS GUN LISTESI ===');
{
  const many = [];
  for (let i = 60; i > 0; i -= 2) many.push(api.streakDayId(no(SUN) - i));
  const s = api.settleStreakFreeze(held({ frozenDays: many }), no(TUE));
  check('ust sinir asilmiyor', s.frozenDays.length, api.STREAK_RECENT_DAYS_KEPT);
  check('en yeni gun listede', s.frozenDays[s.frozenDays.length - 1], MON);
  const dup = api.settleStreakFreeze(held({ frozenDays: [MON] }), no(TUE));
  check('ayni gun iki kez yazilmiyor', dup.frozenDays, [MON]);
}

console.log('\n=== applyStreakDays: DONDURMASIZ DAVRANIS DEGISMEDI ===');
{
  const plain = { current: 5, longest: 7, lastDay: SUN, claimed: [3] };
  const next = api.applyStreakDays(plain, [MON]);
  check('ardisik gun +1', [next.current, next.lastDay, next.claimed], [6, MON, [3]]);
  const gap = api.applyStreakDays(plain, [TUE]);
  check('ardisik olmayan gun 1e ceker, taslar sifirlanir', [gap.current, gap.lastDay, gap.claimed], [1, TUE, []]);
  const same = api.applyStreakDays(plain, [SUN]);
  check('ayni gun ikinci kez sayilmaz', [same.current, same.lastDay], [5, SUN]);
  const past = api.applyStreakDays(plain, ['2026-10-01']);
  check('gecmis gun seriyi oynatmaz', [past.current, past.lastDay], [5, SUN]);
  check('en uzun seri korunuyor', gap.longest, 7);
  check('dondurma alanlari bos donuyor', [next.freezes, next.freezeDay, next.frozenDays], [0, '', []]);
}

console.log('\n=== applyStreakDays: DONDURMA ILE ===');
{
  const saved = api.applyStreakDays(held(), [TUE]);
  check(
    'pazartesi kacti, sali bildirildi -> 5 den 6 ya (kacan gun sayilmadi)',
    [saved.current, saved.lastDay, saved.claimed, saved.freezes, saved.frozenDays],
    [6, TUE, [3], 0, [MON]]
  );
  const lost = api.applyStreakDays(held(), [WED]);
  check(
    'iki gun kacti, carsamba bildirildi -> seri 1, dondurma gitti',
    [lost.current, lost.lastDay, lost.claimed, lost.freezes, lost.frozenDays],
    [1, WED, [], 0, [MON]]
  );
  const late = api.applyStreakDays(held({ freezeDay: TUE }), [TUE]);
  check(
    'kirildiktan sonra alinan dondurma seriyi onarmaz ve harcanmaz',
    [late.current, late.lastDay, late.freezes, late.freezeDay, late.frozenDays],
    [1, TUE, 1, TUE, []]
  );
  const batch = api.applyStreakDays(held(), [TUE, WED]);
  check(
    'toplu bildirim: kopruden sonra ardisik gunler devam eder',
    [batch.current, batch.lastDay, batch.freezes, batch.frozenDays],
    [7, WED, 0, [MON]]
  );
  const fresh = api.applyStreakDays(
    { current: 0, longest: 0, lastDay: '', claimed: [], freezes: 1, freezeDay: MON, frozenDays: [] },
    [TUE]
  );
  check(
    'serisi olmayan kullanici: dondurma duruyor, seri 1',
    [fresh.current, fresh.lastDay, fresh.freezes, fresh.freezeDay],
    [1, TUE, 1, MON]
  );
  // Dondurma yalnizca BIR gunu kapatir: ikinci kacista dondurma yok, seri kirilir.
  const twice = api.applyStreakDays(saved, [THU]);
  check('ikinci kacista dondurma kalmamis -> seri 1', [twice.current, twice.freezes], [1, 0]);
}

console.log('\n=== GUNLER ISLENDIKTEN SONRA BUGUNE GORE KAPATMA ===');
{
  // Cevrimdisi: pazartesi tutturuldu ve kuyruga girdi, sali kacti, carsamba esitlendi.
  const applied = api.applyStreakDays(held(), [MON]);
  const settled = api.settleStreakFreeze(applied, no(WED));
  check(
    'kuyruktaki gunden sonra kacan gun de kapanir',
    pick(settled),
    { current: 6, lastDay: TUE, freezes: 0, freezeDay: '', frozenDays: [TUE] }
  );
}

console.log('\n=== YAMA ===');
{
  const before = held();
  check('degisiklik yoksa yazilmaz', api.freezePatch(before, before), {});
  check(
    'harcaninca uc alan da yazilir',
    api.freezePatch(before, api.settleStreakFreeze(before, no(TUE))),
    { freezes: 0, freezeDay: '', frozenDays: [MON] }
  );
}

console.log('\n=== ISTEMCININ BUGUNU ===');
{
  const server = no(TUE);
  check('ayni gun kabul', api.clientTodayNo(TUE, server), server);
  check('bir gun ileri kabul (dogu saat dilimi)', api.clientTodayNo(WED, server), server + 1);
  check('bir gun geri kabul (bati saat dilimi)', api.clientTodayNo(MON, server), server - 1);
  check('iki gun ileri RED', api.clientTodayNo(THU, server), null);
  check('iki gun geri RED', api.clientTodayNo(SUN, server), null);
  check('bozuk metin RED', api.clientTodayNo('dun', server), null);
  check('alan yok RED', api.clientTodayNo(undefined, server), null);
}

console.log('\n=== SABITLER ===');
check('bedel 4000 altin', api.STREAK_FREEZE_COST, 4000);
check('ayni anda en fazla 1', api.STREAK_FREEZE_MAX_HELD, 1);

// ── buyStreakFreeze: kaynak denetimi ────────────────────────────────────────
//
// Fonksiyonun tamamı Firestore istediği için burada çalıştırılamıyor. Yine de para
// kaybettirebilecek üç şart kaynaktan denetleniyor; biri silinirse test kırılsın.
console.log('\n=== buyStreakFreeze KAYNAK DENETIMI ===');
{
  const start = src.indexOf('exports.buyStreakFreeze');
  const end = src.indexOf('\nexports.', start + 1);
  const body = src.slice(start, end < 0 ? src.length : end);
  check('fonksiyon var', start >= 0, true);
  check('tek transaction icinde', (body.match(/runTransaction/g) || []).length, 1);
  check('once bekleyen dondurma kapatiliyor', body.includes('settleStreakFreeze(before, todayNo)'), true);
  check('ust sinir denetleniyor', body.includes('state.freezes >= STREAK_FREEZE_MAX_HELD'), true);
  check('bakiye denetleniyor', body.includes('currency < STREAK_FREEZE_COST'), true);
  const guard = body.indexOf('currency < STREAK_FREEZE_COST');
  const write = body.indexOf('transaction.update(userRef');
  check('altin dusumu denetimlerden SONRA', guard >= 0 && write > guard, true);
}

// ── submitStreakDay günsüz dal: dondurma düz yazımla değil transaction ile ───
console.log('\n=== GUNSUZ DAL KAYNAK DENETIMI ===');
{
  const start = src.indexOf('exports.submitStreakDay');
  const end = src.indexOf('\nexports.', start + 1);
  const body = src.slice(start, end);
  const branchStart = body.indexOf('if (days.length === 0)');
  const branch = body.slice(branchStart, body.indexOf('\n  }', branchStart));
  check('gunsuz dal dondurmayi transaction ile yaziyor', branch.includes('runTransaction'), true);
  check('transaction icinde yeniden okuyor', branch.includes('transaction.get(ref)'), true);
}

console.log(`\n${pass} gecti, ${fail} kaldi`);
process.exit(fail > 0 ? 1 : 0);

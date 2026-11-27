/**
 * Seri dondurma: GERÇEK fonksiyonlar bellek içi sahte bir Firestore'a karşı.
 *
 * NEDEN VAR
 *   test-streak-freeze.js kuralın kendisini (settleStreakFreeze) deniyor; fonksiyonların
 *   gövdesini değil. Oysa para kaybettiren hatalar gövdede oluyor: yanlış sırada yazım,
 *   transaction dışında kalan bir yama, tanımlanmadan okunan bir değişken. submitStreakDay
 *   bir kez tam olarak böyle çökmüştü (günsüz dal, tanımlanmamış değişken) ve aylarca yalnızca
 *   "başarısız" satırı olarak göründü.
 *
 *   Burada submitStreakDay ve buyStreakFreeze olduğu gibi çalıştırılıyor; yalnızca
 *   `firebase-admin` sahtesiyle değiştiriliyor. Sahte Firestore, gerçeğinin iki kuralını
 *   uyguluyor ki ihlal burada yakalansın: transaction içinde okumalar yazmalardan önce
 *   gelmeli, ve var olmayan doküman `update` edilemez.
 *
 * NEYİ DENEMİYOR
 *   Gerçek Firestore'un eşzamanlılık davranışını (transaction'ın çakışmada yeniden
 *   denenmesi). Yarış senaryosu aşağıda ELLE kuruluyor: iki çağrının belirli bir sırayla
 *   iç içe geçmesi.
 *
 * ÇALIŞTIRMA: cd functions && npm run test:streak-freeze-flow
 */
const Module = require('module');
const path = require('path');

// ── Sahte Firestore ─────────────────────────────────────────────────────────

const SERVER_TS = { __serverTimestamp: true };

class FakeSnap {
  constructor(data) { this._data = data; this.exists = data !== undefined; }
  data() { return this._data === undefined ? undefined : JSON.parse(JSON.stringify(this._data)); }
}

class FakeDocRef {
  constructor(db, docPath) { this.db = db; this.path = docPath; }
  collection(name) { return new FakeColRef(this.db, `${this.path}/${name}`); }
  async get() { return new FakeSnap(this.db.docs.get(this.path)); }
  async set(data, opts) { this.db.write(this.path, data, opts && opts.merge); }
  async update(data) { this.db.update(this.path, data); }
}

class FakeColRef {
  constructor(db, colPath) { this.db = db; this.path = colPath; }
  doc(id) { return new FakeDocRef(this.db, `${this.path}/${id}`); }
}

class FakeTransaction {
  constructor(db) { this.db = db; this.writes = []; }
  async get(ref) {
    if (this.writes.length > 0) {
      throw new Error('SAHTE FIRESTORE: transaction icinde yazimdan SONRA okuma yapildi');
    }
    return new FakeSnap(this.db.docs.get(ref.path));
  }
  set(ref, data, opts) { this.writes.push(() => this.db.write(ref.path, data, opts && opts.merge)); }
  update(ref, data) { this.writes.push(() => this.db.update(ref.path, data)); }
}

class FakeDb {
  constructor() {
    this.docs = new Map();
    this.transactions = 0;
    /** Bir sonraki transaction başlamadan hemen önce çalışır; yarış senaryosu için. */
    this.beforeNextTransaction = null;
  }
  collection(name) { return new FakeColRef(this, name); }
  static clean(data) {
    const out = {};
    for (const [k, v] of Object.entries(data)) out[k] = v === SERVER_TS ? 'TS' : v;
    return JSON.parse(JSON.stringify(out));
  }
  write(docPath, data, merge) {
    const next = FakeDb.clean(data);
    this.docs.set(docPath, merge ? Object.assign({}, this.docs.get(docPath) || {}, next) : next);
  }
  update(docPath, data) {
    if (!this.docs.has(docPath)) throw new Error(`SAHTE FIRESTORE: update, dokuman yok: ${docPath}`);
    this.write(docPath, data, true);
  }
  async runTransaction(fn) {
    if (this.beforeNextTransaction) {
      const hook = this.beforeNextTransaction;
      this.beforeNextTransaction = null;
      await hook();
    }
    this.transactions++;
    const t = new FakeTransaction(this);
    const result = await fn(t);
    t.writes.forEach((w) => w());
    return result;
  }
}

let db = new FakeDb();

// index.js yüklenirken başka admin API'lerine de dokunabilir; bilinmeyen her şey zararsız
// bir vekile düşüyor ki bu test ilgisiz bir satır yüzünden kırılmasın.
function absorb() {
  return new Proxy(function () {}, { get: () => absorb(), apply: () => absorb() });
}
const firestoreFn = Object.assign(() => db, {
  FieldValue: { serverTimestamp: () => SERVER_TS },
});
const fakeAdmin = new Proxy(
  { initializeApp() {}, firestore: firestoreFn },
  { get: (target, prop) => (prop in target ? target[prop] : absorb()) }
);

const realLoad = Module._load;
Module._load = function (request, ...rest) {
  if (request === 'firebase-admin') return fakeAdmin;
  return realLoad.call(this, request, ...rest);
};

// `db` index.js içinde yükleme anında bir kez okunuyor; senaryolar arasında aynı nesne
// temizlenip yeniden kullanılıyor.
const index = require(path.join(__dirname, '..', 'index.js'));
Module._load = realLoad;

// ── Yardımcılar ─────────────────────────────────────────────────────────────

const UID = 'u1';
const STREAK = `users/${UID}/streak/state`;
const USER = `users/${UID}`;
const ctx = { auth: { uid: UID } };

const FRI = '2026-10-02';
const SAT = '2026-10-03';
const SUN = '2026-10-04';
const MON = '2026-10-05';
const TUE = '2026-10-06';
const WED = '2026-10-07';

const realNow = Date.now;
/** Sunucunun saatini verilen günün öğlenine (UTC) çeker. */
function serverDay(day) { Date.now = () => Date.parse(`${day}T12:00:00Z`); }

function reset(user, streak) {
  db.docs.clear();
  db.transactions = 0;
  db.beforeNextTransaction = null;
  if (user) db.docs.set(USER, Object.assign({}, user));
  if (streak) db.docs.set(STREAK, Object.assign({}, streak));
}

const submit = (data) => index.submitStreakDay.run(data, ctx);
const buy = (data) => index.buyStreakFreeze.run(data, ctx);

/** Gün bildiren çağrı; istemcinin gönderdiği alanlarla. */
const report = (days, today) =>
  submit({ days, today, goalMinutes: 5, challengeDays: 0, utcOffsetMinutes: 180 });
/** Günlük "buradayım" bildirimi (gün yok). */
const ping = (today) => report([], today);

/** 3 günlük seri pazar günü tutturulmuş. */
const streak3 = (extra) =>
  Object.assign({ current: 3, longest: 3, lastDay: SUN, claimed: [3], recentDays: [FRI, SAT, SUN] }, extra || {});

let pass = 0;
let fail = 0;
function check(name, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a === e) { console.log('  OK   ' + name); pass++; }
  else { console.log(`  HATA ${name}\n       beklenen ${e}\n       gelen    ${a}`); fail++; }
}
async function rejects(name, promise, code) {
  try {
    await promise;
    console.log(`  HATA ${name}\n       beklenen red: ${code}\n       gelen    basari`);
    fail++;
  } catch (e) {
    check(name, e.code, code);
  }
}
const doc = (p) => db.docs.get(p) || {};
const freezeOf = (d) => [d.freezes, d.freezeDay, d.frozenDays];

// ── Senaryolar ──────────────────────────────────────────────────────────────

async function main() {
  console.log('\n=== SATIN ALMA ===');
  {
    serverDay(SUN);
    reset({ currency: 10000, keys: 2 }, streak3());
    const r = await buy({ today: SUN });
    check('yanit: altin dustu, dondurma geldi', [r.success, r.currency, r.freezes, r.freezeDay], [true, 6000, 1, SUN]);
    check('cuzdan yazildi', doc(USER).currency, 6000);
    check('anahtara dokunulmadi', doc(USER).keys, 2);
    check('dondurma yazildi', freezeOf(doc(STREAK)), [1, SUN, []]);
    check('seri degismedi', [doc(STREAK).current, doc(STREAK).lastDay, doc(STREAK).claimed], [3, SUN, [3]]);
    check('tek transaction', db.transactions, 1);

    await rejects('ikinci alim RED (ayni anda en fazla 1)', buy({ today: SUN }), 'already-exists');
    check('reddedilen alim altin dusmedi', doc(USER).currency, 6000);
  }

  console.log('\n=== SATIN ALMA: REDLER ===');
  {
    serverDay(SUN);
    reset({ currency: 3999, keys: 0 }, streak3());
    await rejects('yetersiz altin RED', buy({ today: SUN }), 'failed-precondition');
    check('altin dusmedi', doc(USER).currency, 3999);
    check('dondurma yazilmadi', doc(STREAK).freezes, undefined);

    reset({ currency: 10000 }, streak3());
    await rejects('oturum yok RED', index.buyStreakFreeze.run({ today: SUN }, {}), 'unauthenticated');
    await rejects('gun yok RED', buy({}), 'invalid-argument');
    await rejects('iki gun ilerideki gun RED', buy({ today: TUE }), 'invalid-argument');
    check('redlerde altin dusmedi', doc(USER).currency, 10000);

    reset(null, streak3());
    await rejects('kullanici dokumani yok RED', buy({ today: SUN }), 'not-found');
  }

  console.log('\n=== TEK GUN KACTI: seri kurtulur ===');
  {
    serverDay(TUE);
    reset({ currency: 0 }, streak3({ freezes: 1, freezeDay: SAT, frozenDays: [] }));
    const p = await ping(TUE);
    check('gunsuz bildirim dondurmayi harcadi', [p.current, p.lastDay, p.freezes, p.frozenDays], [3, MON, 0, [MON]]);
    check('dokuman: pazartesi kopru', [doc(STREAK).lastDay, ...freezeOf(doc(STREAK))], [MON, 0, '', [MON]]);
    check('dokuman: seri sayisi degismedi', doc(STREAK).current, 3);

    const again = await ping(TUE);
    check('ikinci bildirim bir sey degistirmez', [again.lastDay, again.freezes], [MON, 0]);

    const r = await report([TUE], TUE);
    check('sali tutturuldu: 3 -> 4 (kacan gun sayilmadi)', [r.current, r.lastDay, r.claimed, r.freezes], [4, TUE, [3], 0]);
    check('tutturulan gunlere pazartesi GIRMEDI', r.recentDays, [FRI, SAT, SUN, TUE]);
  }

  console.log('\n=== TEK GUN KACTI, BILDIRIM OLMADAN DOGRUDAN GUN GELDI (cevrimdisi) ===');
  {
    serverDay(TUE);
    reset({ currency: 0 }, streak3({ freezes: 1, freezeDay: SAT, frozenDays: [] }));
    const r = await report([TUE], TUE);
    check('3 -> 4, dondurma harcandi', [r.current, r.lastDay, r.freezes, r.frozenDays], [4, TUE, 0, [MON]]);
    check('dokumana da yazildi', freezeOf(doc(STREAK)), [0, '', [MON]]);
  }

  console.log('\n=== IKI GUN KACTI: dondurma harcanir, seri yine kirilir ===');
  {
    serverDay(WED);
    reset({ currency: 0 }, streak3({ freezes: 1, freezeDay: SAT, frozenDays: [] }));
    const p = await ping(WED);
    check('dondurma harcandi, yalnizca pazartesi kapandi', [p.lastDay, p.freezes, p.frozenDays], [MON, 0, [MON]]);
    const r = await report([WED], WED);
    check('carsamba: seri 1 den basliyor, taslar sifirlandi', [r.current, r.lastDay, r.claimed, r.freezes], [1, WED, [], 0]);
  }

  console.log('\n=== KULLANILINCA TEKRAR ALINIR ===');
  {
    serverDay(TUE);
    reset({ currency: 9000 }, streak3({ freezes: 1, freezeDay: SAT, frozenDays: [] }));
    // Sunucu dondurmanin harcandigini henuz islemedi (bildirim gelmedi); istemci harcadi ve
    // magazada dugme acik. Satin alma once eskisini kapatmali, sonra yenisini vermeli.
    const r = await buy({ today: TUE });
    check('eski harcandi + yeni alindi', [r.currency, r.freezes, r.freezeDay, r.frozenDays, r.lastDay], [5000, 1, TUE, [MON], MON]);
    check('dokuman', [doc(STREAK).lastDay, ...freezeOf(doc(STREAK))], [MON, 1, TUE, [MON]]);
    const d = await report([TUE], TUE);
    check('sali tutturuldu: seri 4, yeni dondurma duruyor', [d.current, d.freezes, d.freezeDay], [4, 1, TUE]);
  }

  console.log('\n=== GECMISI ONARMAZ ===');
  {
    serverDay(TUE);
    // Pazartesi kacti, dondurma YOKTU. Istemcide seri kirik; sunucuda current hala 3.
    reset({ currency: 5000 }, streak3());
    const b = await buy({ today: TUE });
    check('alim basarili, son gune dokunulmadi', [b.freezes, b.freezeDay, doc(STREAK).lastDay], [1, TUE, SUN]);
    const p = await ping(TUE);
    check('bildirim yeni dondurmayi HARCAMAZ', [p.lastDay, p.freezes], [SUN, 1]);
    const r = await report([TUE], TUE);
    check('sali: seri 1 (onarilmadi), dondurma duruyor', [r.current, r.claimed, r.freezes, r.freezeDay], [1, [], 1, TUE]);
  }

  console.log('\n=== YARIS: bildirim okudu, araya satin alma girdi ===');
  {
    serverDay(TUE);
    reset({ currency: 9000 }, streak3({ freezes: 1, freezeDay: SAT, frozenDays: [] }));
    // Bildirim durumu okuyup "harcanacak" kararini verdi; transaction'i baslamadan once
    // satin alma tamamlaniyor. Bildirim bayat karariyla yazsaydi yeni dondurma silinirdi.
    db.beforeNextTransaction = async () => { await buy({ today: TUE }); };
    const p = await ping(TUE);
    check('bildirimin yaniti guncel durumu soyluyor', [p.lastDay, p.freezes, p.freezeDay], [MON, 1, TUE]);
    check('yeni dondurma SILINMEDI', freezeOf(doc(STREAK)), [1, TUE, [MON]]);
    check('altin bir kez dustu', doc(USER).currency, 5000);
  }

  console.log('\n=== SERISI OLMAYAN KULLANICI ===');
  {
    serverDay(SUN);
    reset({ currency: 4000 }, null);
    const b = await buy({ today: SUN });
    check('seri dokumani yokken alinabilir', [b.currency, b.freezes], [0, 1]);
    check('bos lastDay yazilmadi', 'lastDay' in doc(STREAK), false);
    const r = await report([SUN], SUN);
    check('ilk gun: seri 1, dondurma duruyor', [r.current, r.lastDay, r.freezes], [1, SUN, 1]);
  }

  console.log('\n=== ESKI ISTEMCI (today gondermiyor) ===');
  {
    serverDay(TUE);
    reset({ currency: 0 }, streak3({ freezes: 1, freezeDay: SAT, frozenDays: [] }));
    const p = await submit({ days: [], goalMinutes: 5, challengeDays: 0, utcOffsetMinutes: 180 });
    check('gunsuz bildirim dondurmaya dokunmaz', [p.lastDay, p.freezes], [SUN, 1]);
    const r = await submit({ days: [TUE], goalMinutes: 5, challengeDays: 0, utcOffsetMinutes: 180 });
    check('gun bildirilince yine de harcanir', [r.current, r.freezes, r.frozenDays], [4, 0, [MON]]);
  }

  console.log('\n=== DONDURMASIZ KULLANICI: HICBIR SEY DEGISMEDI ===');
  {
    serverDay(MON);
    reset({ currency: 0 }, streak3());
    const r = await report([MON], MON);
    check('ardisik gun +1', [r.current, r.lastDay, r.claimed], [4, MON, [3]]);
    check('yanitta dondurma alanlari bos', [r.freezes, r.freezeDay, r.frozenDays], [0, '', []]);
    check('dokumana dondurma alani YAZILMADI', ['freezes' in doc(STREAK), 'freezeDay' in doc(STREAK), 'frozenDays' in doc(STREAK)], [false, false, false]);
    check('en uzun seri kullanici dokumanina yazildi', doc(USER).longestStreak, 4);

    serverDay(WED);
    const gap = await report([WED], WED);
    check('kacan gunden sonra seri 1', [gap.current, gap.claimed], [1, []]);
    const p = await ping(WED);
    check('gunsuz bildirim calisiyor', [p.success, p.current, p.lastDay], [true, 1, WED]);
    check('gunsuz bildirim transaction acmadi', db.transactions, 2);
  }

  Date.now = realNow;
  console.log(`\n${pass} gecti, ${fail} kaldi`);
  process.exit(fail > 0 ? 1 : 0);
}

main().catch((e) => {
  Date.now = realNow;
  console.error('TEST COKTU:', e);
  process.exit(1);
});

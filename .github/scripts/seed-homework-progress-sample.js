/**
 * ÖRNEK VERİ — ödev başarısı ekranını (grafik, Bu hafta kartı, kural ustalığı) dolu görmek için bir
 * hesaba son ~90 günün sahte günlük kayıtlarını yazar (kullanıcı isteği 26.10.2026, Ted hesabı).
 *
 * Yazılan: homeworkRuleStats/{uid}/months/{yyyy-mm}.days.{gg} (q ve t sayımları, artırarak) ve aynı
 * miktarlar `tally`'ye (kurallarda Başlangıç = toplam − günlük kayıt; ikisi birlikte artmazsa
 * Başlangıç noktası kaybolurdu). Bugün yazılmıyor: gerçek ödevle karışmasın. Ne eklendiyse
 * homeworkRuleStats/{uid}/seed/sample'a kaydediliyor (istemci okumuyor, kurallarda kapalı).
 *
 *   SEED_USER_ID=835398VV            hesap (users.userId)
 *   REMOVE=1                         eklenenleri geri al (sample kaydındaki miktarlar düşülür)
 *   REPLACE=1                        varsa eskisini geri al, sonra yenisini yaz (örnek değişince)
 *   DRY_RUN=1                        yalnızca yaz ne yapılacağını
 *
 * Örnek zaten varsa yeniden eklemez (iki kez çalışmak güvenli). GitHub'da:
 * .github/workflows/seed-homework-progress-sample.yml. functions/ dışında, çünkü functions/ push'u
 * bütün fonksiyonları deploy ediyor; firebase-admin functions/node_modules'tan (NODE_PATH).
 */
const admin = require('firebase-admin');

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });
const db = admin.firestore();
const inc = admin.firestore.FieldValue.increment;

const USER_ID = process.env.SEED_USER_ID || '835398VV';
const REMOVE = process.env.REMOVE === '1';
const REPLACE = process.env.REPLACE === '1';
const DRY_RUN = process.env.DRY_RUN === '1';
const DAY_MS = 86400000;
const SPAN_DAYS = 90;

/** Aynı sonuç her çalışmada (mulberry32). */
function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6D2B79F5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Türkiye günü (epoch) → { month: '2026-10', day: '26' }. */
function keyOf(epochDay) {
  const iso = new Date(epochDay * DAY_MS).toISOString();
  return { month: iso.slice(0, 7), day: iso.slice(8, 10) };
}

/**
 * İnişli çıkışlı ama hep yukarı ("şimşek gibi", kullanıcı isteği 26.10.2026): düz bir yükselişin
 * üstüne iki zikzak. Günlük (+2/−2) günlük grafikte, 3 günlük (+3/−3) 1 aylık (3 günlük) grafikte
 * görünüyor; ikisinin toplamından büyük olmadığı için eğilim hiçbir dönemde aşağı dönmüyor.
 */
function zigzag(i) {
  return (i % 2 === 0 ? 0.02 : -0.02) + (Math.floor(i / 3) % 2 === 0 ? 0.03 : -0.03);
}

/**
 * Son 90 gün (bugün hariç), her gün 30–40 soru (az soruda oran %10'luk basamaklarla oynuyordu).
 * Başarı ~%52'den ~%90'a çıkıyor. Konular kurs sırasıyla ekleniyor ve eklenen konu da düşük
 * başlayıp yükseliyor; eski konular arada çalışılmaya devam ediyor (hiçbir konu bir aylık
 * görünümde boş kalmasın).
 */
function buildSample(today) {
  const r = rng(26102026);
  const rules = [
    { key: 'add_direct', start: 0, every: 2 },
    { key: 'add_five', start: 0, every: 1 },
    { key: 'add_ten', start: 15, every: 1 },
    { key: 'sub_five', start: 25, every: 2 },
    { key: 'add_bead', start: 40, every: 1 },
    { key: 'sub_ten', start: 50, every: 2 },
    { key: 'mulA_2x1', start: 55, every: 3 },
  ];
  const days = {};
  for (let k = 0; k < SPAN_DAYS; k++) {
    const p = Math.min(0.96, 0.52 + (0.38 * k) / (SPAN_DAYS - 1) + zigzag(k));
    const n = r() < 0.5 ? 30 : 40;
    const qc = Math.round(n * p);
    const t = {};
    for (const rule of rules) {
      if (k < rule.start || (k - rule.start) % rule.every !== 0) continue;
      // Konu kendi başlangıcından itibaren ~50 günde %55'ten %93'e.
      const age = Math.min(1, (k - rule.start) / 50);
      const rp = Math.min(0.97, 0.55 + 0.38 * age + zigzag(k));
      const ops = rule.key.startsWith('mul') ? 10 : 30 + Math.floor(r() * 20);
      const c = Math.round(ops * rp);
      t[rule.key] = { c, w: ops - c };
    }
    days[today - SPAN_DAYS + k] = { q: { c: qc, w: n - qc }, t };
  }
  return days;
}

async function main() {
  const mode = REMOVE ? ' — GERİ AL' : REPLACE ? ' — DEĞİŞTİR' : '';
  console.log(`Proje: ${PROJECT_ID}, hesap ${USER_ID}${mode}${DRY_RUN ? ' (DRY RUN)' : ''}`);
  const users = await db.collection('users').where('userId', '==', USER_ID).limit(2).get();
  if (users.size !== 1) throw new Error(`${USER_ID} için ${users.size} kullanıcı bulundu`);
  const uid = users.docs[0].id;
  const statsRef = db.collection('homeworkRuleStats').doc(uid);
  const seedRef = statsRef.collection('seed').doc('sample');
  const seedSnap = await seedRef.get();

  if (seedSnap.exists && (REMOVE || REPLACE)) {
    await apply(statsRef, seedRef, seedSnap.get('days') || {}, -1);
    console.log('Eski örnek veri geri alındı.');
  }
  if (REMOVE) {
    if (!seedSnap.exists) console.log('Örnek veri yok; yapılacak bir şey yok.');
    return;
  }
  if (seedSnap.exists && !REPLACE) return console.log('Örnek veri zaten yazılmış; atlandı (REPLACE=1 ya da REMOVE=1).');
  const today = Math.floor((Date.now() + 3 * 3600 * 1000) / DAY_MS);
  await apply(statsRef, seedRef, buildSample(today), 1);
  console.log(`Örnek veri yazıldı (uid ${uid}).`);
}

/** [sign] 1: [days] eklenir ve kaydedilir; −1: düşülür ve kayıt silinir. */
async function apply(statsRef, seedRef, days, sign) {
  const byMonth = {};
  const tally = {};
  let qTotal = 0;
  for (const [ed, d] of Object.entries(days)) {
    const { month, day } = keyOf(Number(ed));
    const t = {};
    for (const [key, cw] of Object.entries(d.t || {})) {
      t[key] = { c: inc(sign * cw.c), w: inc(sign * cw.w) };
      tally[key] = tally[key] || { c: 0, w: 0 };
      tally[key].c += cw.c;
      tally[key].w += cw.w;
    }
    byMonth[month] = byMonth[month] || {};
    byMonth[month][day] = { q: { c: inc(sign * d.q.c), w: inc(sign * d.q.w) }, t };
    qTotal += d.q.c + d.q.w;
  }
  console.log(`${Object.keys(days).length} gün, ${qTotal} soru, aylar: ${Object.keys(byMonth).sort().join(', ')}`);
  console.log('Kural toplamları:', JSON.stringify(tally));
  if (DRY_RUN) return;

  const batch = db.batch();
  for (const [month, dayMap] of Object.entries(byMonth)) {
    batch.set(statsRef.collection('months').doc(month), { days: dayMap, updatedAtMs: Date.now() }, { merge: true });
  }
  const tallyInc = {};
  for (const [key, cw] of Object.entries(tally)) tallyInc[key] = { c: inc(sign * cw.c), w: inc(sign * cw.w) };
  batch.set(statsRef, { tally: tallyInc }, { merge: true });
  if (sign < 0) batch.delete(seedRef);
  else batch.set(seedRef, { days, atMs: Date.now() });
  await batch.commit();
}

main().then(() => process.exit(0)).catch((e) => {
  console.error(e);
  process.exit(1);
});

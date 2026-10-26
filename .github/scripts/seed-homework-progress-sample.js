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
 * Son 90 gün (bugün hariç), haftada ~4 gün ödev. Başarı %58'den %92'ye doğru gürültüyle yükseliyor;
 * konular kurs sırasıyla ilerliyor (kuralsız/5'lik → 10'luk → boncuk, çıkarma ve çarpma araya).
 */
function buildSample(today) {
  const r = rng(26102026);
  const days = {};
  for (let i = SPAN_DAYS; i >= 1; i--) {
    if (r() > 0.58) continue;
    const f = (SPAN_DAYS - i) / SPAN_DAYS;
    const p = Math.min(0.97, Math.max(0.4, 0.58 + 0.34 * f + (r() - 0.5) * 0.12));
    const n = 10 * (1 + Math.floor(r() * 3));
    const qc = Math.round(n * p);
    const rules = f < 0.35 ? ['add_direct', 'add_five']
      : f < 0.7 ? ['add_five', 'add_ten', 'sub_five']
        : ['add_ten', 'add_bead', 'sub_five', 'sub_ten'];
    const t = {};
    for (const key of rules) {
      const ops = Math.round((n * 4) / rules.length);
      // Yeni başlanan konu biraz daha zor.
      const rp = Math.min(0.98, Math.max(0.35, p + (key === rules[rules.length - 1] ? -0.08 : 0.03) + (r() - 0.5) * 0.08));
      const c = Math.round(ops * rp);
      t[key] = { c, w: ops - c };
    }
    if (f > 0.5 && r() < 0.4) {
      const c = Math.round(10 * Math.min(0.95, p + 0.02));
      t.mulA_2x1 = { c, w: 10 - c };
    }
    days[today - i] = { q: { c: qc, w: n - qc }, t };
  }
  return days;
}

async function main() {
  console.log(`Proje: ${PROJECT_ID}, hesap ${USER_ID}${REMOVE ? ' — GERİ AL' : ''}${DRY_RUN ? ' (DRY RUN)' : ''}`);
  const users = await db.collection('users').where('userId', '==', USER_ID).limit(2).get();
  if (users.size !== 1) throw new Error(`${USER_ID} için ${users.size} kullanıcı bulundu`);
  const uid = users.docs[0].id;
  const statsRef = db.collection('homeworkRuleStats').doc(uid);
  const seedRef = statsRef.collection('seed').doc('sample');
  const seedSnap = await seedRef.get();

  let days;
  let sign;
  if (REMOVE) {
    if (!seedSnap.exists) return console.log('Örnek veri yok; yapılacak bir şey yok.');
    days = seedSnap.get('days') || {};
    sign = -1;
  } else {
    if (seedSnap.exists) return console.log('Örnek veri zaten yazılmış; atlandı (önce REMOVE=1).');
    const today = Math.floor((Date.now() + 3 * 3600 * 1000) / DAY_MS);
    days = buildSample(today);
    sign = 1;
  }

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
  if (REMOVE) batch.delete(seedRef);
  else batch.set(seedRef, { days, atMs: Date.now() });
  await batch.commit();
  console.log(REMOVE ? 'Örnek veri geri alındı.' : `Örnek veri yazıldı (uid ${uid}).`);
}

main().then(() => process.exit(0)).catch((e) => {
  console.error(e);
  process.exit(1);
});

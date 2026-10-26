/**
 * Ödev başarısı grafiğinin "Başlangıç" noktası: günlük kayıtlardan (homeworkRuleStats/{uid}/months)
 * önce çözülen sorular. Günlük kayıt 26.10.2026'da başladı; o güne kadar yalnızca toplam tutuluyordu
 * ve tarih yok. Bu betik her öğrenci için bir kez `base.q` yazar:
 *
 *   base.q = ödev ilerlemelerinin (studentProgress) toplamı − günlük kayıtlara zaten girmiş sorular
 *
 * Çıkarma sayesinde betiğin yeni tetikleyiciden önce ya da sonra çalışması fark etmiyor; hiçbir soru
 * iki kez sayılmıyor. Kural sayımının başlangıcını istemci kendisi buluyor (toplam − günlük kayıtlar).
 * `base` zaten varsa öğrenci atlanıyor (yeniden çalıştırmak güvenli).
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\serviceAccount.json"
 *   node scripts/backfill-homework-stats-base.js
 *
 * Önce ne yapacağını görmek için (hiçbir şey yazmaz): $env:DRY_RUN="1"
 * GitHub'da: .github/workflows/backfill-homework-stats-base.yml (push'ta bir kez, sonra elle).
 */
const admin = require('firebase-admin');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

const DRY_RUN = ['1', 'true', 'yes'].includes(String(process.env.DRY_RUN || '').toLowerCase());

async function main() {
  console.log(`Proje: ${PROJECT_ID}${DRY_RUN ? ' (DRY RUN — yazma yok)' : ''}`);

  const progress = await db.collectionGroup('studentProgress').get();
  const totals = new Map();
  for (const d of progress.docs) {
    const uid = d.get('uid') || d.id;
    const t = totals.get(uid) || { answered: 0, correct: 0 };
    t.answered += Number(d.get('answered')) || 0;
    t.correct += Number(d.get('correct')) || 0;
    totals.set(uid, t);
  }
  console.log(`${progress.size} ilerleme kaydı, ${totals.size} öğrenci.`);

  const now = Date.now();
  let written = 0;
  for (const [uid, t] of totals) {
    const ref = db.collection('homeworkRuleStats').doc(uid);
    const snap = await ref.get();
    if (snap.exists && snap.get('base')) {
      console.log(`  ${uid} -> base zaten var, atlandı`);
      continue;
    }
    const months = await ref.collection('months').get();
    let dc = 0;
    let dw = 0;
    for (const m of months.docs) {
      for (const day of Object.values(m.get('days') || {})) {
        dc += Number(day && day.q && day.q.c) || 0;
        dw += Number(day && day.q && day.q.w) || 0;
      }
    }
    const c = Math.max(t.correct - dc, 0);
    const w = Math.max(t.answered - t.correct - dw, 0);
    console.log(`  ${uid} -> base.q = ${c} doğru, ${w} yanlış (günlük kayıtta ${dc}/${dw})`);
    if (DRY_RUN) continue;
    await ref.set({ base: { q: { c, w }, atMs: now } }, { merge: true });
    written++;
  }
  console.log(`Bitti. Yazılan: ${written}.`);
}

main().then(() => process.exit(0)).catch((e) => {
  console.error(e);
  process.exit(1);
});

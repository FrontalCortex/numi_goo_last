/**
 * Silinen kural sayımını günlük kayıtlardan geri kurar (homeworkRuleStats/{uid}.tally/recent/level).
 *
 * NEDEN VAR
 *   writeRuleTally yalnızca soru sayısı gelen gönderimde (kurala bağlanamayan yanlış cevap) boş
 *   haritaları merge ile yazıyordu; Firestore boş haritayı yaprak sayıp alanı tamamen boşaltıyor →
 *   öğrencinin bütün kural geçmişi siliniyordu (hata 02.11.2026, düzeltildi). Günlük kayıtlar
 *   (months/{yyyy-mm}.days.{gg}.t) ayrı belgede ve artırımla yazıldığı için sağlam kaldı.
 *
 * NE YAPAR
 *   Kural başına günlük kayıtların toplamı belgedeki toplamdan büyükse (silinmiş demek) tally o
 *   toplama çekilir; pencere (recent) günler sırasıyla yeniden kurulur (gün = bir adım), aşama
 *   (level) hiç geri düşmez. Toplamı günlük kayıttan büyük ya da eşit kurala dokunulmaz (silinmemiş;
 *   26.10.2026 öncesi ödev sayımı yalnızca toplamda var ve geri getirilemez). Yeniden çalıştırmak
 *   güvenli: ikinci çalıştırmada değişecek kural kalmaz. Her belge bir işlemde: uygulama o sırada
 *   yazsa da sayım kaybolmaz.
 *
 *   cd functions
 *   $env:DRY_RUN="1"; node scripts/restore-rule-tally.js     # yalnızca ne yapacağını yazar
 *   Remove-Item Env:DRY_RUN; node scripts/restore-rule-tally.js
 *   Tek öğrenci: $env:ONLY_UID="..."
 */
const admin = require('firebase-admin');
const homework = require('../homework');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

const DRY_RUN = ['1', 'true', 'yes'].includes(String(process.env.DRY_RUN || '').toLowerCase());
const ONLY_UID = process.env.ONLY_UID || '';

const num = (v) => (Number.isInteger(v) && v > 0 ? v : 0);

/** Günlük kayıtlar tarih sırasıyla: [{ date: '2026-10-26', t: { add_ten: { c, w } } }]. */
function dailyEntries(monthDocs) {
  const out = [];
  for (const m of monthDocs) {
    for (const [day, v] of Object.entries(m.get('days') || {})) {
      if (v && v.t && typeof v.t === 'object') out.push({ date: `${m.id}-${day}`, t: v.t });
    }
  }
  return out.sort((a, b) => a.date.localeCompare(b.date));
}

/** Belge için yazılacak düzeltme; değişen kural yoksa null. */
function planFor(data, days) {
  const sums = {};
  for (const { t } of days) {
    for (const [key, v] of Object.entries(t)) {
      const s = sums[key] || { c: 0, w: 0 };
      s.c += num(v && v.c);
      s.w += num(v && v.w);
      sums[key] = s;
    }
  }
  const tally = data.tally || {};
  const level = data.level || {};
  const patch = { tally: {}, recent: {}, level: {} };
  const report = [];
  for (const [key, s] of Object.entries(sums)) {
    const old = tally[key] || {};
    const oc = num(old.c);
    const ow = num(old.w);
    if (s.c + s.w <= oc + ow) continue;
    let recent = [];
    for (const { t } of days) {
      const v = t[key];
      if (v && num(v.c) + num(v.w) > 0) recent = homework.pushRecent(recent, num(v.c), num(v.w));
    }
    const win = recent.reduce((a, x) => ({ c: a.c + x.c, w: a.w + x.w }), { c: 0, w: 0 });
    const oldLevel = Number.isInteger(level[key]) ? level[key] : 0;
    patch.tally[key] = { c: s.c, w: s.w };
    patch.recent[key] = recent;
    patch.level[key] = Math.max(oldLevel, homework.masteryStage(win.c, win.w));
    report.push(`${key}: ${oc}/${ow} → ${s.c}/${s.w} (aşama ${patch.level[key]})`);
  }
  return report.length ? { patch, report } : null;
}

async function main() {
  console.log(`Proje: ${PROJECT_ID}${DRY_RUN ? ' (DRY RUN — yazma yok)' : ''}`);
  const refs = ONLY_UID
    ? [db.collection('homeworkRuleStats').doc(ONLY_UID)]
    : await db.collection('homeworkRuleStats').listDocuments();
  console.log(`${refs.length} öğrenci belgesi.`);

  let changed = 0;
  for (const ref of refs) {
    const result = await db.runTransaction(async (tx) => {
      const [snap, months] = await Promise.all([tx.get(ref), tx.get(ref.collection('months'))]);
      const plan = planFor(snap.exists ? snap.data() : {}, dailyEntries(months.docs));
      if (plan && !DRY_RUN) tx.set(ref, plan.patch, { merge: true });
      return plan;
    });
    if (!result) continue;
    changed++;
    console.log(`  ${ref.id}`);
    for (const line of result.report) console.log(`    ${line}`);
  }
  console.log(`Bitti. ${DRY_RUN ? 'Düzeltilecek' : 'Düzeltilen'} öğrenci: ${changed}.`);
}

main().then(() => process.exit(0)).catch((e) => {
  console.error(e);
  process.exit(1);
});

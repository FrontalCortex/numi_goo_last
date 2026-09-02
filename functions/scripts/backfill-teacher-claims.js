/**
 * Zaten onaylı (teacherApproved: true) olan tüm öğretmenler için Auth custom claim'i
 * (`teacherApproved: true`) oluşturur.
 *
 * Storage Rules artık öğretmen kontrolünü Firestore'a cross-service `firestore.get()`
 * ile değil, ID token'daki `teacherApproved` custom claim'i ile yapıyor (bkz.
 * storage.rules, functions/index.js -> syncTeacherClaim). Yeni onaylar/kaldırmalar
 * syncTeacherClaim trigger'ı ile otomatik senkron olur; bu betik yalnızca
 * DEĞİŞİKLİKTEN ÖNCE onaylanmış öğretmenler için TEK SEFER çalıştırılır.
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\serviceAccount.json"
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   node scripts/backfill-teacher-claims.js
 *
 * Önce ne yapacağını görmek için (hiçbir şey yazmaz):
 *   $env:DRY_RUN="1"; node scripts/backfill-teacher-claims.js
 *
 * NOT: Claim, o hesabın bir sonraki ID token yenilemesinde (genelde çıkış/giriş
 * yapınca) etkin olur; mevcut oturumdaki token hemen değişmez.
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

  const snap = await db.collection('users').where('teacherApproved', '==', true).get();
  console.log(`${snap.size} onaylı öğretmen bulundu.`);

  let updated = 0;
  for (const doc of snap.docs) {
    const uid = doc.id;
    if (DRY_RUN) {
      console.log(`  ${uid} -> teacherApproved claim verilecek`);
      continue;
    }
    await admin.auth().setCustomUserClaims(uid, { teacherApproved: true });
    updated++;
    console.log(`  ${uid} -> claim verildi`);
  }

  console.log(`Bitti. Güncellenen: ${updated}.`);
}

main().catch((err) => {
  console.error('Backfill başarısız:', err);
  process.exit(1);
});

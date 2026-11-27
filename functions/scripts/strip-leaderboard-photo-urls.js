/**
 * Liderlik tablosu girişlerinden (`lessonLeaderboards/{board}/entries/{uid}`) `photoUrl`
 * alanını siler.
 *
 * `submitLeaderboardScore` eskiden kimlik jetonundaki Google hesap fotoğrafının adresini
 * buraya yazıyordu; giriş yapmış her kullanıcı okuyabiliyordu. Artık yazmıyor ve rekor
 * kırılan girişte alanı siliyor; bu betik geri kalan (rekoru yenilenmeyen, geçmiş
 * sezonlardaki) girişler için TEK SEFER çalıştırılır. Tekrar çalıştırmak zararsız.
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\serviceAccount.json"
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   node scripts/strip-leaderboard-photo-urls.js
 *
 * Önce ne yapacağını görmek için (hiçbir şey yazmaz):
 *   $env:DRY_RUN="1"; node scripts/strip-leaderboard-photo-urls.js
 */
const admin = require('firebase-admin');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

const DRY_RUN = ['1', 'true', 'yes'].includes(String(process.env.DRY_RUN || '').toLowerCase());
const PAGE = 400;

async function main() {
  console.log(`Proje: ${PROJECT_ID}${DRY_RUN ? ' (DRY RUN — yazma yok)' : ''}`);

  let scanned = 0;
  let stripped = 0;
  let lastDoc = null;

  // `entries` adı başka koleksiyonlarda da kullanılabilir; yalnızca lessonLeaderboards altındakiler.
  // `photoUrl != ''` ile süzmek koleksiyon grubu dizini isterdi; hepsini sayfa sayfa taramak
  // tek seferlik iş için yeterli.
  for (;;) {
    let query = db.collectionGroup('entries').orderBy('__name__').limit(PAGE);
    if (lastDoc) query = query.startAfter(lastDoc);

    const snap = await query.get();
    if (snap.empty) break;

    const batch = db.batch();
    let batchCount = 0;

    for (const doc of snap.docs) {
      scanned++;
      const board = doc.ref.parent.parent;
      if (!board || board.parent.id !== 'lessonLeaderboards') continue;
      if (doc.get('photoUrl') === undefined) continue;
      stripped++;
      if (DRY_RUN) {
        console.log(`  ${doc.ref.path}`);
        continue;
      }
      batch.update(doc.ref, { photoUrl: admin.firestore.FieldValue.delete() });
      batchCount++;
    }

    if (!DRY_RUN && batchCount > 0) {
      await batch.commit();
    }

    lastDoc = snap.docs[snap.docs.length - 1];
    console.log(`  ...${scanned} giriş tarandı, ${stripped} tanesinde photoUrl vardı`);
    if (snap.size < PAGE) break;
  }

  console.log(`Bitti. Taranan: ${scanned}, photoUrl silinen: ${stripped}${DRY_RUN ? ' (DRY RUN)' : ''}.`);
}

main().catch((err) => {
  console.error('Temizlik başarısız:', err);
  process.exit(1);
});

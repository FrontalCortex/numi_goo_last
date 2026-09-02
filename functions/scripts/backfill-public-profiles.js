/**
 * Mevcut tüm kullanıcılar için `publicProfiles/{uid}` dokümanını oluşturur.
 *
 * `users/{uid}` artık yalnızca sahibi ve onaylı öğretmenler tarafından okunabiliyor
 * (bkz. firestore.rules). Arkadaş arama / profil görüntüleme / takipçi listeleri
 * `publicProfiles` koleksiyonunu okuyor. Yeni yazımları `mirrorPublicProfile` trigger'ı
 * hallediyor; bu betik yalnızca DEĞİŞİKLİKTEN ÖNCE var olan kullanıcılar için
 * TEK SEFER çalıştırılır.
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\serviceAccount.json"
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   node scripts/backfill-public-profiles.js
 *
 * Önce ne yapacağını görmek için (hiçbir şey yazmaz):
 *   $env:DRY_RUN="1"; node scripts/backfill-public-profiles.js
 */
const admin = require('firebase-admin');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

const DRY_RUN = ['1', 'true', 'yes'].includes(String(process.env.DRY_RUN || '').toLowerCase());

// functions/index.js içindeki PUBLIC_PROFILE_FIELDS ile birebir aynı olmalı.
const PUBLIC_PROFILE_FIELDS = [
  'uid',
  'name',
  'userId',
  'selectedAvatar',
  'plan',
  'createdAt',
  'totalTimeSpent',
  'followersCount',
  'followingCount',
];

function buildPublicProfile(uid, userData) {
  const out = { uid };
  for (const field of PUBLIC_PROFILE_FIELDS) {
    if (userData[field] !== undefined && userData[field] !== null) {
      out[field] = userData[field];
    }
  }
  if (out.followersCount === undefined) out.followersCount = 0;
  if (out.followingCount === undefined) out.followingCount = 0;
  return out;
}

async function main() {
  console.log(`Proje: ${PROJECT_ID}${DRY_RUN ? ' (DRY RUN — yazma yok)' : ''}`);

  let processed = 0;
  let written = 0;
  let lastDoc = null;

  // Sayfalı okuma: tek seferde tüm koleksiyonu belleğe almıyoruz.
  for (;;) {
    let query = db.collection('users').orderBy('__name__').limit(300);
    if (lastDoc) query = query.startAfter(lastDoc);

    const snap = await query.get();
    if (snap.empty) break;

    let batch = db.batch();
    let batchCount = 0;

    for (const doc of snap.docs) {
      processed++;
      const profile = buildPublicProfile(doc.id, doc.data() || {});
      if (DRY_RUN) {
        console.log(`  ${doc.id} → ${JSON.stringify(profile)}`);
        continue;
      }
      batch.set(db.collection('publicProfiles').doc(doc.id), profile, { merge: false });
      batchCount++;
      written++;
    }

    if (!DRY_RUN && batchCount > 0) {
      await batch.commit();
    }

    lastDoc = snap.docs[snap.docs.length - 1];
    console.log(`  ...${processed} kullanıcı işlendi`);
    if (snap.size < 300) break;
  }

  console.log(`Bitti. İşlenen: ${processed}, yazılan: ${written}.`);
}

main().catch((err) => {
  console.error('Backfill başarısız:', err);
  process.exit(1);
});

/**
 * Liderlik seed uid'leri (`seed_lb_*`) yüzünden oluşan hayalet `users/{id}` yollarını temizler:
 * tüm alt koleksiyonları siler, sonra üst dokümanı siler.
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\json"
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   $env:CONFIRM_DELETE="1"
 *   npm run delete:seed-users
 *
 * Varsayılan: `seed_lb_000` … `seed_lb_999` (3 hane) kontrol edilir.
 * Farklı aralık: SEED_INDEX_START=0 SEED_INDEX_END=99
 */
const admin = require('firebase-admin');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

function truthy(v) {
  const s = String(v || '').toLowerCase();
  return s === '1' || s === 'true' || s === 'yes';
}

async function deleteCollectionDocs(colRef, batchSize = 450) {
  while (true) {
    const snap = await colRef.limit(batchSize).get();
    if (snap.empty) break;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
}

async function deleteUserGhostTree(userRef) {
  const cols = await userRef.listCollections();
  for (const col of cols) {
    await deleteCollectionDocs(col);
  }
  try {
    await userRef.delete();
  } catch (e) {
    // Alt koleksiyonlar boşaldıysa üst silinir; yine de logla
    console.error(`[delete-seed-users] üst silinemedi ${userRef.id}`, e.message || e);
  }
}

async function main() {
  if (!truthy(process.env.CONFIRM_DELETE)) {
    throw new Error('Onay için CONFIRM_DELETE=1 ayarlayın.');
  }

  const prefix = process.env.SEED_UID_PREFIX || 'seed_lb_';
  const pad = Math.max(1, Math.min(6, parseInt(process.env.SEED_UID_PAD || '3', 10) || 3));
  const start = parseInt(process.env.SEED_INDEX_START || '0', 10);
  const end = parseInt(process.env.SEED_INDEX_END || '999', 10);

  if (Number.isNaN(start) || Number.isNaN(end) || start < 0 || end < start || end - start > 5000) {
    throw new Error('Geçersiz SEED_INDEX_START / SEED_INDEX_END (aralık en fazla 5001)');
  }

  console.error('[delete-seed-users] projectId:', PROJECT_ID);
  console.error('[delete-seed-users] prefix:', prefix, 'range:', start, '..', end, 'pad:', pad);

  let touched = 0;
  let deleted = 0;

  for (let i = start; i <= end; i++) {
    const id = `${prefix}${String(i).padStart(pad, '0')}`;
    const ref = db.collection('users').doc(id);
    const cols = await ref.listCollections();
    if (cols.length === 0) {
      const snap = await ref.get();
      if (!snap.exists) continue;
    }
    touched++;
    await deleteUserGhostTree(ref);
    deleted++;
    if (deleted % 25 === 0) {
      console.error(`[delete-seed-users] … silindi: ${deleted}`);
    }
  }

  console.log(
    JSON.stringify(
      {
        ok: true,
        prefix,
        range: [start, end],
        usersTouched: touched,
        usersDeleted: deleted,
      },
      null,
      2,
    ),
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

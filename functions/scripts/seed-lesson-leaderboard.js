/**
 * Part 1, lesson index 4 liderlik tablosuna test kayıtları yazar (Admin SDK — güvenlik kurallarını bypass eder).
 *
 * Kurulum:
 *   cd functions
 *   npm install
 *
 * Kimlik (birini kullan):
 *   - Ortam değişkeni: GOOGLE_APPLICATION_CREDENTIALS=<serviceAccount.json tam yolu>
 *   - veya: gcloud auth application-default login (Google Cloud CLI)
 *
 * Çalıştır:
 *   node scripts/seed-lesson-leaderboard.js
 *
 * Opsiyonel ortam değişkenleri:
 *   BOARD_ID=part_1_lesson_4  SCORE=2000  COUNT=10
 */
const admin = require('firebase-admin');

/** app/google-services.json project_id ile aynı olmalı (.firebaserc default). */
const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

const BOARD_ID = process.env.BOARD_ID || 'part_1_lesson_4';
const SCORE = parseInt(process.env.SCORE || '2000', 10);
const COUNT = parseInt(process.env.COUNT || '10', 10);

async function main() {
  console.error('[seed] Firestore projectId:', PROJECT_ID);
  if (Number.isNaN(SCORE) || SCORE < 1) {
    throw new Error('Geçersiz SCORE');
  }
  if (Number.isNaN(COUNT) || COUNT < 1 || COUNT > 500) {
    throw new Error('COUNT 1–500 arası olmalı');
  }

  const batch = db.batch();
  const col = db.collection('lessonLeaderboards').doc(BOARD_ID).collection('entries');

  for (let i = 0; i < COUNT; i++) {
    const userId = `seed_lb_${String(i).padStart(2, '0')}`;
    const ref = col.doc(userId);
    batch.set(ref, {
      recordScore: SCORE,
      recordLabel: String(SCORE),
      displayName: `Test Oyuncu ${i + 1}`,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      photoUrl: '',
    });
  }

  await batch.commit();
  console.log(
    JSON.stringify({
      ok: true,
      path: `lessonLeaderboards/${BOARD_ID}/entries`,
      count: COUNT,
      recordScore: SCORE,
      docIds: Array.from({ length: COUNT }, (_, i) => `seed_lb_${String(i).padStart(2, '0')}`),
    }, null, 2),
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

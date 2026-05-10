/**
 * lessonLeaderboards/{boardId}/entries koleksiyonunu okur (Admin SDK).
 * Uygulama ile aynı sorgu: orderBy recordScore desc, limit 60.
 *
 *   cd functions && npm install
 *   set FIREBASE_PROJECT_ID=numigo-new
 *   set GOOGLE_APPLICATION_CREDENTIALS=C:\path\serviceAccount.json
 *   node scripts/verify-lesson-leaderboard.js
 */
const admin = require('firebase-admin');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
const BOARD_ID = process.env.BOARD_ID || 'part_1_lesson_4';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

async function main() {
  console.log('Firestore projectId (hedef):', PROJECT_ID);
  console.log('Board:', BOARD_ID);

  const col = db.collection('lessonLeaderboards').doc(BOARD_ID).collection('entries');

  const allSnap = await col.get();
  console.log('\n[Tüm entries doküman sayısı]', allSnap.size);
  allSnap.docs.forEach((d) => {
    const x = d.data();
    console.log(' ', d.id, 'recordScore=', x.recordScore, 'displayName=', x.displayName);
  });

  const ordered = await col.orderBy('recordScore', 'desc').limit(60).get();
  console.log('\n[orderBy recordScore desc limit 60 — uygulama ile aynı] size=', ordered.size);
  ordered.docs.forEach((d, i) => {
    const x = d.data();
    console.log(
      `  #${i + 1} ${d.id} score=${x.recordScore} name=${x.displayName}`,
    );
  });

  const seeds = allSnap.docs.filter((d) => d.id.startsWith('seed_lb_'));
  console.log('\n[seed_lb_* adet]', seeds.length);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

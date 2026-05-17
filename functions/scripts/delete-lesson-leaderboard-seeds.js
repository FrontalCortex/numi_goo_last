/**
 * lessonLeaderboards/{BOARD_ID}/entries içinde id'si seed_lb_ ile başlayan
 * tüm dokümanları siler (Admin SDK — istemci kuralları delete'e izin vermez).
 * Sezon: [seasonCalendar.js] (UTC takvim ayı). Deploy notu: seed-lesson-leaderboard.js başlığına bakın.
 *
 *   cd functions && npm install
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="..."
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   npm run delete:leaderboard-seeds
 *
 * Opsiyonel: BOARD_ID=part_1_lesson_4_season_12
 * Verilmezse: part_1_lesson_4_season_{mevcutSezon} (SeasonClock ile aynı anchor)
 */
const admin = require('firebase-admin');
const { currentSeason } = require('../seasonCalendar');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
const DEFAULT_BOARD = `part_1_lesson_4_season_${currentSeason()}`;
const BOARD_ID = process.env.BOARD_ID || DEFAULT_BOARD;
const PREFIX = process.env.SEED_PREFIX || 'seed_lb_';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

async function main() {
  console.error('[delete-seeds] projectId:', PROJECT_ID, 'board:', BOARD_ID);

  const col = db.collection('lessonLeaderboards').doc(BOARD_ID).collection('entries');
  const snap = await col.get();
  const toDelete = snap.docs.filter((d) => d.id.startsWith(PREFIX));

  if (toDelete.length === 0) {
    console.log(JSON.stringify({ ok: true, deleted: 0, message: 'Silinecek seed doküman yok.' }, null, 2));
    return;
  }

  const batch = db.batch();
  toDelete.forEach((d) => batch.delete(d.ref));
  await batch.commit();

  console.log(
    JSON.stringify(
      {
        ok: true,
        deleted: toDelete.length,
        ids: toDelete.map((d) => d.id),
        path: `lessonLeaderboards/${BOARD_ID}/entries`,
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

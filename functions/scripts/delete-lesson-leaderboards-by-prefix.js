/**
 * lessonLeaderboards altında doc id'si verilen önek ile başlayan tahtaları siler:
 * önce entries alt koleksiyonu, sonra üst doküman. Ardından CF imlecini sıfırlar.
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\json"
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   $env:CONFIRM_DELETE="1"
 *   npm run delete:leaderboards:by-prefix
 *
 * Varsayılan önek: part_1_lesson_4_season_  (tüm sezon tahtaları + botlar)
 *
 * Başka önek:
 *   $env:BOARD_DOC_PREFIX="part_2_lesson_0_season_"
 *
 * Tüm part/ders sezon tahtaları (dikkat — geniş silme):
 *   $env:BOARD_DOC_PREFIX=""
 *   $env:DELETE_ALL_SEASON_BOARDS="1"
 *   $env:CONFIRM_DELETE="1"
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

const SEASON_BOARD_RE = /^part_\d+_lesson_\d+_season_\d+$/;

async function deleteQueryInBatches(query, batchSize = 450) {
  // batch max 500; güvenli pay
  while (true) {
    const snap = await query.limit(batchSize).get();
    if (snap.empty) break;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
}

async function deleteBoard(ref) {
  await deleteQueryInBatches(ref.collection('entries'));
  await ref.delete();
}

async function main() {
  if (!truthy(process.env.CONFIRM_DELETE)) {
    throw new Error('Onay için CONFIRM_DELETE=1 ayarlayın.');
  }

  const deleteAllSeason = truthy(process.env.DELETE_ALL_SEASON_BOARDS);
  let prefix = 'part_1_lesson_4_season_';
  if (process.env.BOARD_DOC_PREFIX && String(process.env.BOARD_DOC_PREFIX).length > 0) {
    prefix = String(process.env.BOARD_DOC_PREFIX);
  }

  if (deleteAllSeason && !truthy(process.env.CONFIRM_WIDE_DELETE)) {
    throw new Error(
      'DELETE_ALL_SEASON_BOARDS=1 tüm part_*_lesson_*_season_* tahtalarını siler. Onay: CONFIRM_WIDE_DELETE=1',
    );
  }

  console.error('[delete-boards] projectId:', PROJECT_ID);
  console.error('[delete-boards] mode:', deleteAllSeason ? 'ALL_SEASON_PATTERN' : `prefix="${prefix}"`);

  const snap = await db.collection('lessonLeaderboards').get();
  const refs = snap.docs.map((d) => d.ref);
  const targets = refs.filter((r) => {
    if (deleteAllSeason) return SEASON_BOARD_RE.test(r.id);
    return r.id.startsWith(prefix);
  });

  console.error('[delete-boards] tahta sayısı:', targets.length);
  let done = 0;
  for (const ref of targets) {
    await deleteBoard(ref);
    done++;
    if (done % 25 === 0 || done === targets.length) {
      console.error(`[delete-boards] … ${done}/${targets.length} (${ref.id})`);
    }
  }

  const cursorRef = db.doc('system/seasonLeaderboardRewards');
  await cursorRef.set(
    {
      lastFinalizedSeason: 0,
      clearedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true },
  );

  console.log(
    JSON.stringify(
      {
        ok: true,
        deletedBoards: targets.length,
        cursorReset: true,
        path: 'system/seasonLeaderboardRewards',
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

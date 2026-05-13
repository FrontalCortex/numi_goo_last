/**
 * lessonLeaderboards tahtasına test entry'leri + tahta meta yazar (Admin SDK).
 * [SeasonClock.kt] ile aynı anchor/süre (13 Mayıs 2026 00:00 UTC) — tahta id: part_{p}_lesson_{i}_season_{s}
 *
 *   cd functions && npm install
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\serviceAccount.json"
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   npm run seed:leaderboard
 *
 * Varsayılan: part=1, lessonIndex=4, mevcut sezon, 100 kişi, 1980 puan.
 *
 * Tüm sezonlar (1 … mevcut sezon):
 *   $env:ALL_SEASONS="1"
 *   npm run seed:leaderboard
 *
 * Opsiyonel:
 *   PART_ID=1 LESSON_INDEX=4 SEASON=42 SCORE=1980 COUNT=100
 *   SEASON_FROM=1 SEASON_TO=50
 *   Büyük aralık için: ALLOW_LARGE_SEED=1
 *   Tek tahta: BOARD_ID=part_1_lesson_4_season_5  (ALL_SEASONS ile kullanılmaz)
 *
 * Örnek (1. part, 4. ders indeksi, sezon 1045–1065, 1900 puan, 100 kişi/tahta):
 *   npm run seed:leaderboard:range
 *   veya: ALL_SEASONS=1 PART_ID=1 LESSON_INDEX=4 SEASON_FROM=1045 SEASON_TO=1065 SCORE=1900 COUNT=100 npm run seed:leaderboard
 */
const admin = require('firebase-admin');

/** [SeasonClock.kt] ile aynı */
const SEASON_ANCHOR_UTC_MS = Date.UTC(2026, 4, 13, 0, 0, 0, 0);
const SEASON_LENGTH_MS = 2 * 60 * 1000;

function currentSeason(nowMs = Date.now()) {
  if (nowMs < SEASON_ANCHOR_UTC_MS) return 1;
  const elapsed = nowMs - SEASON_ANCHOR_UTC_MS;
  const periodIndex = Math.floor(elapsed / SEASON_LENGTH_MS);
  return Math.max(1, periodIndex + 1);
}

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

function parseBoardId(boardId) {
  const m = /^part_(\d+)_lesson_(\d+)_season_(\d+)$/.exec(String(boardId).trim());
  if (!m) return null;
  return {
    boardId: String(boardId).trim(),
    partId: parseInt(m[1], 10),
    lessonIndex: parseInt(m[2], 10),
    season: parseInt(m[3], 10),
  };
}

function truthy(v) {
  const s = String(v || '').toLowerCase();
  return s === '1' || s === 'true' || s === 'yes';
}

function resolveParams() {
  const allSeasons = truthy(process.env.ALL_SEASONS) || truthy(process.env.SEED_ALL_SEASONS);

  if (allSeasons) {
    if (process.env.BOARD_ID && String(process.env.BOARD_ID).trim()) {
      throw new Error('ALL_SEASONS=1 iken BOARD_ID kullanma; PART_ID ve LESSON_INDEX kullan.');
    }
    const partId = parseInt(process.env.PART_ID || '1', 10);
    const lessonIndex = parseInt(process.env.LESSON_INDEX || '4', 10);
    const seasonFrom = parseInt(process.env.SEASON_FROM || '1', 10);
    const seasonTo = parseInt(process.env.SEASON_TO || String(currentSeason()), 10);
    const score = parseInt(process.env.SCORE || '1980', 10);
    const count = parseInt(process.env.COUNT || '100', 10);
    const titleUnit = process.env.TITLE_UNIT || 'Ünite Maratonu';
    return {
      mode: 'all',
      partId,
      lessonIndex,
      seasonFrom,
      seasonTo,
      score,
      count,
      titleUnit,
    };
  }

  let partId = parseInt(process.env.PART_ID || '1', 10);
  let lessonIndex = parseInt(process.env.LESSON_INDEX || '4', 10);
  let season =
    process.env.SEASON !== undefined && process.env.SEASON !== ''
      ? parseInt(process.env.SEASON, 10)
      : currentSeason();

  let boardId;
  if (process.env.BOARD_ID && String(process.env.BOARD_ID).trim()) {
    const parsed = parseBoardId(process.env.BOARD_ID);
    if (!parsed) {
      throw new Error('BOARD_ID formatı: part_{p}_lesson_{i}_season_{s} olmalı');
    }
    boardId = parsed.boardId;
    partId = parsed.partId;
    lessonIndex = parsed.lessonIndex;
    season = parsed.season;
  } else {
    boardId = `part_${partId}_lesson_${lessonIndex}_season_${season}`;
  }

  const score = parseInt(process.env.SCORE || '1980', 10);
  const count = parseInt(process.env.COUNT || '100', 10);
  const titleUnit = process.env.TITLE_UNIT || 'Ünite Maratonu';

  return {
    mode: 'single',
    boardId,
    partId,
    lessonIndex,
    season,
    score,
    count,
    titleUnit,
  };
}

async function seedOneBoard(partId, lessonIndex, season, score, count, titleUnit) {
  const boardId = `part_${partId}_lesson_${lessonIndex}_season_${season}`;
  const boardRef = db.collection('lessonLeaderboards').doc(boardId);
  const entriesCol = boardRef.collection('entries');

  const batch = db.batch();

  batch.set(
    boardRef,
    {
      partId,
      lessonIndex,
      season,
      titleUnit,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true },
  );

  for (let i = 0; i < count; i++) {
    const userId = `seed_lb_${String(i).padStart(3, '0')}`;
    const ref = entriesCol.doc(userId);
    batch.set(ref, {
      recordScore: score,
      recordLabel: String(score),
      displayName: `Test Oyuncu ${i + 1}`,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      photoUrl: '',
      titleUnit,
    });
  }

  await batch.commit();
  return boardId;
}

async function main() {
  const p = resolveParams();

  console.error('[seed] projectId:', PROJECT_ID);

  if (Number.isNaN(p.score) || p.score < 1) {
    throw new Error('Geçersiz SCORE');
  }
  if (Number.isNaN(p.count) || p.count < 1 || p.count > 499) {
    throw new Error('COUNT 1–499 olmalı (tek batch: meta + entry, Firestore limit 500)');
  }

  if (p.mode === 'single') {
    if (Number.isNaN(p.season) || p.season < 1) {
      throw new Error('Geçersiz SEASON');
    }
    console.error('[seed] single boardId:', p.boardId, 'season:', p.season);
    await seedOneBoard(p.partId, p.lessonIndex, p.season, p.score, p.count, p.titleUnit);
    console.log(
      JSON.stringify(
        {
          ok: true,
          mode: 'single',
          boardId: p.boardId,
          count: p.count,
          recordScore: p.score,
        },
        null,
        2,
      ),
    );
    return;
  }

  const { partId, lessonIndex, seasonFrom, seasonTo, score, count, titleUnit } = p;

  if (Number.isNaN(seasonFrom) || Number.isNaN(seasonTo) || seasonFrom < 1 || seasonTo < seasonFrom) {
    throw new Error('Geçersiz SEASON_FROM / SEASON_TO');
  }

  const span = seasonTo - seasonFrom + 1;
  const maxDefault = parseInt(process.env.MAX_SEASONS_WITHOUT_CONFIRM || '500', 10);
  if (span > maxDefault && !truthy(process.env.ALLOW_LARGE_SEED)) {
    throw new Error(
      `Sezon aralığı çok geniş (${span} tahta). Maliyet için durduruldu. ` +
        `Kasıtlıysan: $env:ALLOW_LARGE_SEED="1" veya SEASON_FROM / SEASON_TO daralt.`,
    );
  }

  console.error(
    `[seed] ALL_SEASONS part=${partId} lesson=${lessonIndex} seasons ${seasonFrom}…${seasonTo} (${span} tahta), bots=${count}, score=${score}`,
  );

  const boardIds = [];
  for (let s = seasonFrom; s <= seasonTo; s++) {
    const id = await seedOneBoard(partId, lessonIndex, s, score, count, titleUnit);
    boardIds.push(id);
    if (s % 50 === 0 || s === seasonTo) {
      console.error(`[seed] … tamam: season ${s}/${seasonTo}`);
    }
  }

  console.log(
    JSON.stringify(
      {
        ok: true,
        mode: 'all_seasons',
        partId,
        lessonIndex,
        seasonFrom,
        seasonTo,
        boardsWritten: span,
        botsPerBoard: count,
        recordScore: score,
        boardIdsSample: boardIds.slice(0, 3).concat(boardIds.length > 3 ? ['…'] : []),
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

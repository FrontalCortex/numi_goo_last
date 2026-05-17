/**
 * Part 1, ders indeksi 4, verilen sezon aralığında her tahtaya aynı puan + 100 seed oyuncu.
 * Ana mantık: seed-lesson-leaderboard.js (ALL_SEASONS=1). Sezon: [seasonCalendar.js] UTC takvim ayı.
 *
 * Deploy: system/seasonLeaderboardRewards lastFinalizedSeason = currentSeason()-1;
 * eski test tahtalarını isteğe bağlı silin (seed-lesson-leaderboard.js başlığı).
 *
 * Varsayılan: sezon 1…mevcut sezon, SCORE=1980, COUNT=100, PART=1, LESSON=4
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="..."
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   npm run seed:leaderboard:range
 *
 * Özel aralık / puan:
 *   node scripts/seed-leaderboard-season-range.js 1 3 1 4 1980 100
 *   (argv: seasonFrom seasonTo [partId] [lessonIndex] [score] [count])
 */
const { currentSeason } = require('../seasonCalendar');
const argv = process.argv.slice(2).map((a) => String(a).trim());

function argInt(i, fallback) {
  if (argv[i] === undefined || argv[i] === '') return fallback;
  const n = parseInt(argv[i], 10);
  return Number.isNaN(n) ? fallback : n;
}

const cur = currentSeason();
const seasonFrom = argInt(0, parseInt(process.env.SEASON_FROM || '1', 10));
const seasonTo = argInt(1, parseInt(process.env.SEASON_TO || String(cur), 10));
const partId = argInt(2, parseInt(process.env.PART_ID || '1', 10));
const lessonIndex = argInt(3, parseInt(process.env.LESSON_INDEX || '4', 10));
const score = argInt(4, parseInt(process.env.SCORE || '1980', 10));
const count = argInt(5, parseInt(process.env.COUNT || '100', 10));

process.env.ALL_SEASONS = '1';
process.env.SEASON_FROM = String(seasonFrom);
process.env.SEASON_TO = String(seasonTo);
process.env.PART_ID = String(partId);
process.env.LESSON_INDEX = String(lessonIndex);
process.env.SCORE = String(score);
process.env.COUNT = String(count);

// eslint-disable-next-line no-console
console.error(
  `[seed-leaderboard-season-range] part=${partId} lesson=${lessonIndex} seasons ${seasonFrom}…${seasonTo} score=${score} count=${count}`,
);

require('./seed-lesson-leaderboard.js');

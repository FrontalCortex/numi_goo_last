'use strict';

/**
 * UTC takvim ayı sezonları — [SeasonClock.kt] ile aynı formül.
 * Sezon 1 = anchor ayı (Mayıs 2026); her ayın 1'i 00:00:00 UTC'de yeni sezon.
 *
 * Deploy (2 dk test epokundan geçiş):
 * - Firestore system/seasonLeaderboardRewards → lastFinalizedSeason = currentSeason() - 1
 * - Eski yüksek season meta'lı lessonLeaderboards test dokümanlarını isteğe bağlı silin
 */
const SEASON_TIMEZONE = 'UTC';

/** Sezon 1 başlangıcı: 1 Mayıs 2026 00:00:00 UTC (JS ay 0-tabanlı: 4 = Mayıs). */
const SEASON_ANCHOR_UTC_MS = Date.UTC(2026, 4, 1, 0, 0, 0, 0);

const ANCHOR_YEAR = 2026;
const ANCHOR_MONTH = 4; // May

function monthIndexUtc(year, month) {
  return year * 12 + month;
}

/**
 * @param {number} [nowMs]
 * @returns {number} 1-tabanlı sezon numarası
 */
function currentSeason(nowMs = Date.now()) {
  const d = new Date(nowMs);
  const nowIdx = monthIndexUtc(d.getUTCFullYear(), d.getUTCMonth());
  const anchorIdx = monthIndexUtc(ANCHOR_YEAR, ANCHOR_MONTH);
  if (nowIdx < anchorIdx) return 1;
  return nowIdx - anchorIdx + 1;
}

/**
 * Mevcut sezonun bitişine (sonraki ayın 1'i 00:00 UTC) kalan süre (ms).
 * @param {number} [nowMs]
 */
function millisUntilCurrentSeasonEnds(nowMs = Date.now()) {
  const d = new Date(nowMs);
  const endMs = Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 1, 0, 0, 0, 0);
  return Math.max(0, endMs - nowMs);
}

/** @param {number} season 1-tabanlı */
function currentSeasonStartUtcMs(season) {
  if (season < 1) season = 1;
  const anchorIdx = monthIndexUtc(ANCHOR_YEAR, ANCHOR_MONTH);
  const idx = anchorIdx + (season - 1);
  const year = Math.floor(idx / 12);
  const month = idx % 12;
  return Date.UTC(year, month, 1, 0, 0, 0, 0);
}

/** Bir sonraki sezonun başlangıcı = bu sezonun bitişi. */
function currentSeasonEndUtcMs(season) {
  if (season < 1) season = 1;
  return currentSeasonStartUtcMs(season + 1);
}

module.exports = {
  SEASON_TIMEZONE,
  SEASON_ANCHOR_UTC_MS,
  ANCHOR_YEAR,
  ANCHOR_MONTH,
  currentSeason,
  millisUntilCurrentSeasonEnds,
  currentSeasonStartUtcMs,
  currentSeasonEndUtcMs,
};

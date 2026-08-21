'use strict';

/**
 * HAFTALIK SEZONLAR
 * Başlangıç noktası (Anchor): 21 Ağustos 2026 12:05:00 UTC
 */
const SEASON_TIMEZONE = 'UTC';

// 21 Ağustos 2026, 12:05:00 UTC -> 1787313900000 ms
const SEASON_ANCHOR_UTC_MS = 1787313900000;
const SEASON_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 1 Hafta (7 gün)

function currentSeason(nowMs = Date.now()) {
  const elapsed = nowMs - SEASON_ANCHOR_UTC_MS;
  if (elapsed < 0) return 1;
  return Math.floor(elapsed / SEASON_DURATION_MS) + 1;
}

function millisUntilCurrentSeasonEnds(nowMs = Date.now()) {
  const elapsed = nowMs - SEASON_ANCHOR_UTC_MS;
  if (elapsed < 0) return -elapsed;
  const remainder = elapsed % SEASON_DURATION_MS;
  return Math.max(0, SEASON_DURATION_MS - remainder);
}

function currentSeasonStartUtcMs(season) {
  if (season < 1) season = 1;
  return SEASON_ANCHOR_UTC_MS + (season - 1) * SEASON_DURATION_MS;
}

function currentSeasonEndUtcMs(season) {
  if (season < 1) season = 1;
  return currentSeasonStartUtcMs(season + 1);
}

module.exports = {
  SEASON_TIMEZONE,
  SEASON_ANCHOR_UTC_MS,
  SEASON_DURATION_MS,
  currentSeason,
  millisUntilCurrentSeasonEnds,
  currentSeasonStartUtcMs,
  currentSeasonEndUtcMs,
};
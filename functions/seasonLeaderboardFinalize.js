'use strict';

/**
 * Sezon sonu: lessonLeaderboards tahtalarından ilk 100 sırayı okuyup kullanıcı badgeProgress/state
 * parçalarını günceller; ardından o sezona ait tahta dokümanlarını (entries dahil) siler.
 * [seasonCalendar.js] / [SeasonClock.kt] ile aynı UTC takvim ayı mantığı.
 */
const {
  SEASON_ANCHOR_UTC_MS,
  currentSeason,
} = require('./seasonCalendar');

const SYSTEM_CURSOR = 'system/seasonLeaderboardRewards';

function parseMedalList(value) {
  if (!Array.isArray(value)) return [];
  const out = [];
  for (const row of value) {
    if (!row || typeof row !== 'object') continue;
    const title = String(row.titleUnit != null ? row.titleUnit : '').trim();
    const season = Number(row.season);
    if (!title || !Number.isFinite(season)) continue;
    out.push({ titleUnit: title, season: Math.trunc(season) });
  }
  return out;
}

function parseCupList(value) {
  if (!Array.isArray(value)) return [];
  const out = [];
  for (const row of value) {
    if (!row || typeof row !== 'object') continue;
    const title = String(row.titleUnit != null ? row.titleUnit : '').trim();
    const rank = Number(row.rank);
    const season = Number(row.season);
    if (!title || !Number.isFinite(rank) || !Number.isFinite(season)) continue;
    out.push({ titleUnit: title, rank: Math.trunc(rank), season: Math.trunc(season) });
  }
  return out;
}

function mergeMedals(existing, incoming) {
  const keys = new Set(existing.map((r) => `${r.titleUnit}\u0000${r.season}`));
  const out = existing.slice();
  for (const r of incoming) {
    const k = `${r.titleUnit}\u0000${r.season}`;
    if (!keys.has(k)) {
      keys.add(k);
      out.push(r);
    }
  }
  return out;
}

function mergeCups(existing, incoming) {
  const keys = new Set(existing.map((r) => `${r.titleUnit}\u0000${r.rank}\u0000${r.season}`));
  const out = existing.slice();
  for (const r of incoming) {
    const k = `${r.titleUnit}\u0000${r.rank}\u0000${r.season}`;
    if (!keys.has(k)) {
      keys.add(k);
      out.push(r);
    }
  }
  return out;
}

function rowsForRank(titleUnit, season, rank) {
  const t = titleUnit || 'null';
  const gold = [];
  const silver = [];
  const bronze = [];
  const cup = [];
  if (rank === 1) {
    gold.push({ titleUnit: t, season });
    cup.push({ titleUnit: t, rank: 1, season });
  } else if (rank === 2) {
    silver.push({ titleUnit: t, season });
    cup.push({ titleUnit: t, rank: 2, season });
  } else if (rank === 3) {
    bronze.push({ titleUnit: t, season });
    cup.push({ titleUnit: t, rank: 3, season });
  } else if (rank >= 4 && rank <= 100) {
    cup.push({ titleUnit: t, rank, season });
  }
  return { gold, silver, bronze, cup };
}

function medalToFs(rows) {
  return rows.map((r) => ({ titleUnit: r.titleUnit, season: r.season }));
}

function cupToFs(rows) {
  return rows.map((r) => ({ titleUnit: r.titleUnit, rank: r.rank, season: r.season }));
}

function hasLeaderboardRewardInc(inc) {
  return (
    (inc.gold && inc.gold.length > 0) ||
    (inc.silver && inc.silver.length > 0) ||
    (inc.bronze && inc.bronze.length > 0) ||
    (inc.cup && inc.cup.length > 0)
  );
}

/** orderBy recordScore desc ile gelen sıra için yarışma sırası (1224): aynı puanda paylaşılan en iyi sıra. */
function recordScoreForRanking(data) {
  const v = data && data.recordScore;
  if (v == null) return Number.NEGATIVE_INFINITY;
  if (typeof v === 'number' && Number.isFinite(v)) return v;
  if (typeof v === 'string' && v.trim() !== '' && Number.isFinite(Number(v))) return Number(v);
  if (typeof v === 'object' && typeof v.toNumber === 'function') return v.toNumber();
  return Number.NEGATIVE_INFINITY;
}

function competitionRanksFromTopDocs(docs) {
  if (!docs.length) return [];
  const scores = docs.map((d) => recordScoreForRanking(d.data()));
  const ranks = new Array(docs.length);
  ranks[0] = 1;
  let currentRank = 1;
  let prev = scores[0];
  for (let i = 1; i < docs.length; i++) {
    const s = scores[i];
    if (s !== prev) currentRank = i + 1;
    ranks[i] = currentRank;
    prev = s;
  }
  return ranks;
}

/** Firestore batch limiti altında güvenli pay. */
async function deleteEntriesInBatches(db, entriesColRef, batchSize = 450) {
  while (true) {
    const snap = await entriesColRef.limit(batchSize).get();
    if (snap.empty) break;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
}

async function deleteLeaderboardBoard(db, boardRef) {
  await deleteEntriesInBatches(db, boardRef.collection('entries'));
  await boardRef.delete();
}

/**
 * Sezon S bitti sayılır: uygulama içi [currentSeason] = S+1 veya daha büyükken S ödüllendirilir.
 * Kullanıcı rozet alanları yazıldıktan sonra bu sezona ait lessonLeaderboards dokümanları silinir (depolama).
 */
async function finalizeSeason(db, season) {
  const boardsSnap = await db.collection('lessonLeaderboards').where('season', '==', season).get();
  if (boardsSnap.empty) {
    console.log(`finalizeSeasonLeaderboardMedals: no board docs with meta.season=${season}`);
    return;
  }

  /** @type {Map<string, { gold: any[], silver: any[], bronze: any[], cup: any[] }>} */
  const byUid = new Map();

  function acc(uid, part) {
    if (!byUid.has(uid)) {
      byUid.set(uid, { gold: [], silver: [], bronze: [], cup: [] });
    }
    const b = byUid.get(uid);
    b.gold.push(...part.gold);
    b.silver.push(...part.silver);
    b.bronze.push(...part.bronze);
    b.cup.push(...part.cup);
  }

  for (const boardDoc of boardsSnap.docs) {
    const meta = boardDoc.data() || {};
    const titleFromMeta =
      meta.titleUnit != null && String(meta.titleUnit).trim() ? String(meta.titleUnit).trim() : null;
    const top = await boardDoc.ref
      .collection('entries')
      .orderBy('recordScore', 'desc')
      .limit(100)
      .get();

    const compRanks = competitionRanksFromTopDocs(top.docs);
    top.docs.forEach((entryDoc, index) => {
      const rank = compRanks[index];
      const uid = entryDoc.id;
      const d = entryDoc.data() || {};
      const titleFromEntry =
        d.titleUnit != null && String(d.titleUnit).trim() ? String(d.titleUnit).trim() : null;
      const titleUnit = titleFromEntry || titleFromMeta || 'null';
      const piece = rowsForRank(titleUnit, season, rank);
      acc(uid, piece);
    });
  }

  console.log(
    `finalizeSeasonLeaderboardMedals: season=${season} boards=${boardsSnap.docs.length} users=${byUid.size}`,
  );

  for (const [uid, inc] of byUid) {
    if (!uid) continue;
    if (uid.startsWith('seed_lb_')) continue;
    const stateRef = db.collection('users').doc(uid).collection('badgeProgress').doc('state');
    try {
      await db.runTransaction(async (t) => {
        const stateSnap = await t.get(stateRef);
        const d = stateSnap.data() || {};
        const exGold = stateSnap.exists ? parseMedalList(d.goldMedalPiece) : [];
        const exSilver = stateSnap.exists ? parseMedalList(d.silverMedalPiece) : [];
        const exBronze = stateSnap.exists ? parseMedalList(d.bronzeMedalPiece) : [];
        const exCup = stateSnap.exists ? parseCupList(d.cupPiece) : [];

        const mergedGold = mergeMedals(exGold, inc.gold);
        const mergedSilver = mergeMedals(exSilver, inc.silver);
        const mergedBronze = mergeMedals(exBronze, inc.bronze);
        const mergedCup = mergeCups(exCup, inc.cup);

        const payload = {
          goldMedalPiece: medalToFs(mergedGold),
          silverMedalPiece: medalToFs(mergedSilver),
          bronzeMedalPiece: medalToFs(mergedBronze),
          cupPiece: cupToFs(mergedCup),
        };
        if (hasLeaderboardRewardInc(inc)) {
          payload.pendingLeaderboardRewardSeason = season;
        }

        t.set(stateRef, payload, { merge: true });
      });
    } catch (e) {
      console.error(`finalizeSeasonLeaderboardMedals: uid=${uid}`, e);
    }
  }

  for (const boardDoc of boardsSnap.docs) {
    await deleteLeaderboardBoard(db, boardDoc.ref);
  }
  console.log(
    `finalizeSeasonLeaderboardMedals: deleted ${boardsSnap.docs.length} lessonLeaderboards for season=${season}`,
  );
}

async function runOnce(db, admin) {
  const cursorRef = db.doc(SYSTEM_CURSOR);
  const maxCatchUp = 50;
  for (let i = 0; i < maxCatchUp; i++) {
    const snap = await cursorRef.get();
    let last = snap.exists ? Number(snap.data().lastFinalizedSeason || 0) : 0;
    if (!Number.isFinite(last) || last < 0) last = 0;

    const cur = currentSeason(Date.now());
    const latestEnded = cur - 1;

    /**
     * Epok değişimi veya manuel hata: imlek gerçek “bitmiş en son sezon”un üzerinde kaldıysa
     * (ör. last=1094, latestEnded=537) hiç finalize çalışmaz. Geri sar: bir sonraki işlenecek
     * sezon latestEnded olsun diye last = latestEnded - 1.
     */
    if (last > latestEnded) {
      const rewindTo = Math.max(0, latestEnded - 1);
      console.warn(
        `finalizeSeasonLeaderboardMedals: cursor skew lastFinalizedSeason=${last} > latestEnded=${latestEnded}; rewind to ${rewindTo}`,
      );
      await cursorRef.set(
        {
          lastFinalizedSeason: rewindTo,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      last = rewindTo;
    }

    if (latestEnded < 1 || latestEnded <= last) return;

    const seasonToProcess = last + 1;
    await finalizeSeason(db, seasonToProcess);
    await cursorRef.set(
      {
        lastFinalizedSeason: seasonToProcess,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
    console.log(`finalizeSeasonLeaderboardMedals: cursor -> ${seasonToProcess}`);
  }
}

function scheduleFinalize(functions, admin, db) {
  return functions
    .runWith({ timeoutSeconds: 300, memory: '512MB' })
    .pubsub.schedule('every 1 minutes')
    .timeZone('Etc/UTC')
    .onRun(async () => {
      await runOnce(db, admin);
      return null;
    });
}

module.exports = { scheduleFinalize, currentSeason, SEASON_ANCHOR_UTC_MS };

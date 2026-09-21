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

/**
 * Aynı anda kaç tahtanın ilk 100'ü okunsun.
 *
 * Okumalar birbirinden bağımsız; sıralı yapmanın tek sonucu her tahtanın gidiş-dönüş
 * süresini toplamaktı.
 */
const BOARD_READ_CONCURRENCY = 10;

/**
 * Aynı anda kaç kullanıcının ödülü yazılsın.
 *
 * Darboğaz buydu: her kullanıcı için ayrı bir transaction var ve bunlar sırayla
 * çalışıyordu. İşlem başına ~150 ms ile 300 saniyeye ancak iki bine yakın kullanıcı
 * sığıyordu; üst sınır `tahta × 100` olduğu için tahta sayısı arttıkça o sınıra dayanacaktı.
 * Her kullanıcı ayrı dokümana yazdığı için paralellik Firestore açısından sorun değil;
 * 25 aynı anda, kotaya yüklenmeden marjı on katına çıkarıyor.
 */
const USER_AWARD_CONCURRENCY = 25;

/** Aynı anda kaç tahta silinsin. Her silme kendi içinde birden fazla batch commit'i yapıyor. */
const BOARD_DELETE_CONCURRENCY = 5;

/**
 * Bir çalışmanın kendine ayırdığı iş süresi.
 *
 * Fonksiyonun sınırı 300 saniye; 240'ta durup son yazmalara ve loglara yer bırakıyoruz.
 * Süre dolarsa iş yarıda kalmıyor, imleç ilerlemiyor ve 5 dakika sonraki çalışma aynı
 * sezonu baştan alıyor.
 */
const FINALIZE_BUDGET_MS = 240_000;

/**
 * [items] üzerinde en fazla [limit] iş aynı anda çalışacak şekilde gezer.
 *
 * [shouldStop] her işten önce sorulur; true dönerse kalan işler yapılmaz ve fonksiyon
 * `false` döner. Böylece süresi dolan bir çalışma yarıda temiz durabiliyor.
 *
 * @returns Bütün işler yapıldıysa true.
 */
async function forEachWithConcurrency(items, limit, worker, shouldStop) {
  const list = Array.from(items);
  let nextIndex = 0;
  let stopped = false;
  const workerCount = Math.max(1, Math.min(limit, list.length));
  await Promise.all(
    Array.from({ length: workerCount }, async () => {
      for (;;) {
        if (shouldStop && shouldStop()) {
          stopped = true;
          return;
        }
        const index = nextIndex++;
        if (index >= list.length) return;
        await worker(list[index]);
      }
    }),
  );
  return !stopped;
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
async function finalizeSeason(db, season, deadlineMs) {
  const outOfTime = () => Date.now() >= deadlineMs;
  const boardsSnap = await db.collection('lessonLeaderboards').where('season', '==', season).get();
  if (boardsSnap.empty) {
    console.log(`finalizeSeasonLeaderboardMedals: no board docs with meta.season=${season}`);
    return true;
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

  // acc paylaşılan Map'i değiştiriyor ama içinde await yok: JS tek iş parçacıklı olduğu
  // için paralel okumalar arasında bölünmüyor.
  await forEachWithConcurrency(boardsSnap.docs, BOARD_READ_CONCURRENCY, async (boardDoc) => {
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
  });

  console.log(
    `finalizeSeasonLeaderboardMedals: season=${season} boards=${boardsSnap.docs.length} users=${byUid.size}`,
  );

  const uids = Array.from(byUid.keys()).filter((uid) => uid && !uid.startsWith('seed_lb_'));

  const awardsDone = await forEachWithConcurrency(
    uids,
    USER_AWARD_CONCURRENCY,
    async (uid) => {
      const inc = byUid.get(uid);
      const userRef = db.collection('users').doc(uid);
      const stateRef = userRef.collection('badgeProgress').doc('state');
      try {
        // Hesabını sezon bitmeden/finalize öncesi silmiş kullanıcıya ödül yazıp
        // users/{uid}/badgeProgress/state dokümanını hayalet olarak yeniden oluşturmamak için kontrol.
        const userSnap = await userRef.get();
        if (!userSnap.exists) {
          console.log(`finalizeSeasonLeaderboardMedals: uid=${uid} kullanıcısı artık yok, ödül atlanıyor`);
          return;
        }
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

          // Ödül kapısı yalnızca GERÇEKTEN yeni satır eklendiyse açılıyor.
          //
          // Yarıda kalmış bir çalışma tekrarlandığında ödülü çoktan toplamış kullanıcıya
          // kapı ikinci kez açılıyordu; kuyruk boş olduğu için hemen kapanıyordu ama
          // kullanıcıya sebepsiz bir ekran gösteriyordu. Birleştirme zaten tekrarı
          // eklemediği için uzunluk karşılaştırması "yeni bir şey oldu mu"nun tam karşılığı.
          const addedSomething =
            mergedGold.length !== exGold.length ||
            mergedSilver.length !== exSilver.length ||
            mergedBronze.length !== exBronze.length ||
            mergedCup.length !== exCup.length;
          if (addedSomething && hasLeaderboardRewardInc(inc)) {
            payload.pendingLeaderboardRewardSeason = season;
          }

          t.set(stateRef, payload, { merge: true });
        });
      } catch (e) {
        console.error(`finalizeSeasonLeaderboardMedals: uid=${uid}`, e);
      }
    },
    outOfTime,
  );

  if (!awardsDone) {
    console.warn(
      `finalizeSeasonLeaderboardMedals: season=${season} ödül yazımı süreye sığmadı; imleç ilerletilmiyor, kalanı bir sonraki çalışmada`,
    );
    return false;
  }

  // Silme ancak ödüllerin TAMAMI yazıldıktan sonra: yarıda silinen bir tahta, ödülünü
  // henüz almamış kullanıcıların sırasını sonsuza kadar kaybettirirdi.
  const deletesDone = await forEachWithConcurrency(
    boardsSnap.docs,
    BOARD_DELETE_CONCURRENCY,
    (boardDoc) => deleteLeaderboardBoard(db, boardDoc.ref),
    outOfTime,
  );

  if (!deletesDone) {
    // İmleç ilerlemiyor: bir sonraki çalışma ödülleri yeniden yazacak (birleştirme tekrarı
    // eklemiyor) ve silmeye kaldığı yerden devam edecek — silinmiş girdiler zaten yok.
    console.warn(
      `finalizeSeasonLeaderboardMedals: season=${season} tahta silme süreye sığmadı; bir sonraki çalışmada devam edilecek`,
    );
    return false;
  }

  console.log(
    `finalizeSeasonLeaderboardMedals: deleted ${boardsSnap.docs.length} lessonLeaderboards for season=${season}`,
  );
  return true;
}

async function runOnce(db, admin, deadlineMs) {
  const cursorRef = db.doc(SYSTEM_CURSOR);
  const maxCatchUp = 50;
  for (let i = 0; i < maxCatchUp; i++) {
    if (Date.now() >= deadlineMs) {
      console.warn('finalizeSeasonLeaderboardMedals: süre doldu, kalan sezonlar bir sonraki çalışmada');
      return;
    }
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
    // İmleç yalnızca sezon TAM bittiğinde ilerliyor. Yarıda kalırsa bir sonraki çalışma
    // aynı sezonu baştan alıyor; ödül birleştirmesi tekrarı eklemediği için bu güvenli.
    const completed = await finalizeSeason(db, seasonToProcess, deadlineMs);
    if (!completed) return;
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

/**
 * Sezon bitişlerini finalize eden zamanlayıcı.
 *
 * PERİYOT NEDEN 5 DAKİKA
 *   Sezonlar HAFTALIK (bkz. seasonCalendar → SEASON_DURATION_MS). Dakikada bir çalışmak,
 *   haftada bir yapılacak iş için 10.080 çağrı demekti; her çağrı imleç dokümanını okuyup
 *   "bitmiş sezon var mı" diye bakıp çıkıyor. Bedava kotaların içinde kalıyordu ama
 *   projedeki en yoğun fonksiyon buydu ve logları gürültüyle doldurup gerçek sorunları
 *   aramayı zorlaştırıyordu.
 *
 *   5 dakika, sezon bitiminde en fazla 5 dakikalık gecikme demek — madalyalar haftalık bir
 *   döngüde dağıtıldığı için fark edilmez. runOnce zaten geride kalmış sezonları
 *   (maxCatchUp'a kadar) toparlıyor, yani bir çalıştırma kaçsa bile kayıp olmuyor.
 */
function scheduleFinalize(functions, admin, db) {
  return functions
    .runWith({ timeoutSeconds: 300, memory: '512MB' })
    .pubsub.schedule('every 5 minutes')
    .timeZone('Etc/UTC')
    .onRun(async () => {
      // Fonksiyonun 300 saniyesinden pay ayrılıyor: iş bütçesi dolunca çalışma kendi
      // isteğiyle duruyor ve imleci ilerletmiyor. Zorla kesilseydi de veri bozulmazdı
      // (imleç zaten ilerlemezdi) ama son log satırları ve temizlik yarıda kalırdı.
      await runOnce(db, admin, Date.now() + FINALIZE_BUDGET_MS);
      return null;
    });
}

module.exports = { scheduleFinalize, currentSeason, SEASON_ANCHOR_UTC_MS };

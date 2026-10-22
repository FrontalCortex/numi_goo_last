/**
 * Bir lessonLeaderboards tahtasını gerçek oyuncu gibi görünen sahte oyuncularla doldurur (Admin SDK).
 *
 * seed-lesson-leaderboard.js'ten farkı:
 *   • İsimler Türk ve yabancı ad-soyad karışık ("Test Oyuncu N" değil).
 *   • Her sahte oyuncuya uygulamadaki gibi rastgele avatar: publicProfiles/{seed_lb_xxx}.avatarConfig
 *     (tablo avatarı oradan çiziyor, bkz. LessonLeaderboardRepository.listenLeaderboard).
 *     publicProfiles dokümanına `name`/`userId` YAZILMAZ → arkadaş aramasında çıkmazlar.
 *   • Puanlar dağınık ve tahtadaki en düşük GERÇEK puanın altında. Sezon sonu sıralaması
 *     sahte oyuncuları da sayıyor (seasonLeaderboardFinalize → competitionRanksFromTopDocs);
 *     üstte dururlarsa gerçek çocukların madalya sırası düşerdi. Madalyanın kendisi seed_lb_
 *     kimliklerine zaten verilmiyor.
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\serviceAccount.json"
 *   $env:BOARD_ID="part_1_lesson_p1_i04_unite_maratonu_season_9"
 *   $env:DRY_RUN="1"; node scripts/seed-lesson-leaderboard-realistic.js   # yalnızca planı yazar
 *   $env:DRY_RUN="";  node scripts/seed-lesson-leaderboard-realistic.js
 *
 * Opsiyonel: COUNT=40  MIN_SCORE=200  MAX_SCORE=1500 (tahtada gerçek oyuncu yoksa üst sınır)
 *            ALLOW_ABOVE_REAL=1 → üst sınır yine de MAX_SCORE (gerçek oyuncular sıra kaybeder)
 * Geri alma (bu tahtanın seed girişleri + onların publicProfiles dokümanları): REMOVE=1
 */
const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
const BOARD_ID = String(process.env.BOARD_ID || '').trim();
const PREFIX = 'seed_lb_';
const COUNT = parseInt(process.env.COUNT || '40', 10);
const MIN_SCORE = parseInt(process.env.MIN_SCORE || '200', 10);
const MAX_SCORE = parseInt(process.env.MAX_SCORE || '1500', 10);

const truthy = (v) => ['1', 'true', 'yes'].includes(String(v || '').toLowerCase());
const DRY_RUN = truthy(process.env.DRY_RUN);
const REMOVE = truthy(process.env.REMOVE);
// Gerçek oyuncuların üstüne çıkmaya izin ver (sezon sonu madalya sıralarını düşürür, bilerek kullan).
const ALLOW_ABOVE_REAL = truthy(process.env.ALLOW_ABOVE_REAL);

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}
const db = admin.firestore();

// ── İsimler ─────────────────────────────────────────────────────────────────
const GIRL_TR = ['Elif', 'Zeynep', 'Defne', 'Ecrin', 'Azra', 'Nehir', 'Asya', 'Eylül', 'Duru', 'Masal', 'Ela', 'İpek', 'Su', 'Lina', 'Nisa', 'Ada', 'Miray', 'Beren', 'Öykü', 'Yağmur'];
const BOY_TR = ['Yusuf', 'Eymen', 'Ömer', 'Mustafa', 'Kerem', 'Alparslan', 'Göktuğ', 'Emir', 'Aras', 'Çınar', 'Burak', 'Efe', 'Arda', 'Mert', 'Kuzey', 'Batuhan', 'Ege', 'Doruk', 'Berat', 'Selim'];
const SURNAME_TR = ['Yılmaz', 'Kaya', 'Demir', 'Şahin', 'Çelik', 'Yıldız', 'Yıldırım', 'Öztürk', 'Aydın', 'Özdemir', 'Arslan', 'Doğan', 'Kılıç', 'Aslan', 'Çetin', 'Koç', 'Kurt', 'Özkan', 'Şimşek', 'Polat'];
const GIRL_INT = ['Emma', 'Olivia', 'Sofia', 'Mia', 'Chloe', 'Lucia', 'Hannah', 'Amira', 'Aisha', 'Nadia', 'Lea', 'Isabella', 'Maya', 'Sara', 'Anna', 'Yuki', 'Putri', 'Ayu'];
const BOY_INT = ['Liam', 'Noah', 'Lucas', 'Leon', 'Mateo', 'Adam', 'Elias', 'Omar', 'Daniel', 'Hugo', 'Luca', 'Rayan', 'Ethan', 'Kenji', 'Rizky', 'Bima', 'Jonas', 'Ali'];
const SURNAME_INT = ['Smith', 'Müller', 'García', 'Rossi', 'Martin', 'Novak', 'Schmidt', 'Silva', 'Haddad', 'Kowalski', 'Jensen', 'Dubois', 'Tanaka', 'Santoso', 'Wijaya', 'Fischer', 'López', 'Petrov'];

// ── Avatar (AvatarArt.kt → PERSONAS ile aynı seçenekler) ────────────────────
const PARTS = JSON.parse(
  fs.readFileSync(path.join(__dirname, '../../app/src/main/assets/avatar/personas_parts.json'), 'utf8'),
);
const HAIR_GIRL = ['long', 'bobCut', 'curly', 'pigtails', 'curlyBun', 'bobBangs', 'straightBun', 'extraLong', 'bunUndercut', 'beanie'];
const HAIR_BOY = ['sideShave', 'shortCombover', 'curlyHighTop', 'curly', 'buzzcut', 'cap', 'fade', 'beanie', 'shortComboverChops', 'mohawk'];
const HAIR_COLORS = ['362c47', '6c4545', 'e15c66', 'e16381', 'f27d65', 'f29c65', 'dee1f5'];
const SKIN_COLORS = ['eeb4a4', 'e7a391', 'e5a07e', 'd78774', 'b16a5b', '92594b', '623d36'];
const CLOTHING_COLORS = ['456dff', '54d7c7', '7555ca', '6dbb58', 'e24553', 'f3b63a', 'f55d81'];
const BACKGROUNDS = ['b6e3f4', '65c9ff', 'c0aede', 'd1d4f9', 'ffd5dc', 'ffdfbf', 'a7ffc4', 'ffffb1', '262e33'];
// Çocuk oyuncular: emzik ve sakal yok.
const MOUTHS = Object.keys(PARTS.mouth).filter((m) => m !== 'pacifier');

const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];

function shuffled(arr) {
  const a = arr.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

/** AvatarConfig.encode biçimi: "personas|anahtar=değer;..." (anahtarlar sıralı). */
function randomAvatar(girl) {
  const values = {
    hair: pick(girl ? HAIR_GIRL : HAIR_BOY),
    hairColor: pick(HAIR_COLORS),
    skinColor: pick(SKIN_COLORS),
    eyes: pick(Object.keys(PARTS.eyes)),
    nose: pick(Object.keys(PARTS.nose)),
    mouth: pick(MOUTHS),
    facialHair: '',
    body: pick(Object.keys(PARTS.body)),
    clothingColor: pick(CLOTHING_COLORS),
    backgroundColor: pick(BACKGROUNDS),
  };
  for (const [k, v] of Object.entries(values)) {
    if (['hair', 'eyes', 'nose', 'mouth', 'body'].includes(k) && !(v in PARTS[k])) {
      throw new Error(`Avatar parçası pakette yok: ${k}=${v}`);
    }
  }
  return 'personas|' + Object.keys(values).sort().map((k) => `${k}=${values[k]}`).join(';');
}

/**
 * Gerçek profillerdeki gibi karışık biçim: çoğu yalnızca ad, bir kısmı ad-soyad ya da
 * soyadın baş harfi; yaklaşık yarısı küçük harfle (çocuklar adını öyle yazıyor).
 */
function styleName(first, last, turkish) {
  const r = Math.random();
  let name;
  if (r < 0.55) name = first;
  else if (r < 0.8) name = `${first} ${last}`;
  else name = `${first} ${last[0]}.`;
  // Yabancı adda tr-TR kuralı "Isabella"yı "ısabella" yapardı.
  if (Math.random() >= 0.45) return name;
  return turkish ? name.toLocaleLowerCase('tr-TR') : name.toLowerCase();
}

/** Ad kümesinden tekrarsız `count` kişi: yaklaşık yarısı Türk, yarısı yabancı; kız/erkek karışık. */
function makePlayers(count) {
  const pools = [
    { first: shuffled(GIRL_TR), last: SURNAME_TR, girl: true },
    { first: shuffled(BOY_TR), last: SURNAME_TR, girl: false },
    { first: shuffled(GIRL_INT), last: SURNAME_INT, girl: true },
    { first: shuffled(BOY_INT), last: SURNAME_INT, girl: false },
  ];
  const used = new Set();
  const players = [];
  for (let i = 0; players.length < count; i++) {
    const pool = pools[i % pools.length];
    const first = pool.first[Math.floor(i / pools.length) % pool.first.length];
    let name;
    do {
      name = styleName(first, pick(pool.last), pool.last === SURNAME_TR);
    } while (used.has(name));
    used.add(name);
    players.push({ name, avatar: randomAvatar(pool.girl) });
  }
  return shuffled(players);
}

/** [lo, hi] aralığında tekrarsız, rastgele dağılmış puanlar (azalan). */
function makeScores(count, lo, hi) {
  if (hi - lo + 1 < count) throw new Error(`Puan aralığı ${lo}–${hi}, ${count} kişiye yetmiyor.`);
  const set = new Set();
  while (set.size < count) {
    // Üst tarafa biraz daha yoğun: tablonun başı seyrek görünmesin.
    const r = Math.pow(Math.random(), 0.8);
    set.add(Math.round(lo + r * (hi - lo)));
  }
  return Array.from(set).sort((a, b) => b - a);
}

async function remove(boardRef) {
  const snap = await boardRef.collection('entries').get();
  const seeds = snap.docs.filter((d) => d.id.startsWith(PREFIX));
  if (DRY_RUN) {
    console.log(JSON.stringify({ dryRun: true, wouldDelete: seeds.length }, null, 2));
    return;
  }
  const batch = db.batch();
  seeds.forEach((d) => {
    batch.delete(d.ref);
    batch.delete(db.collection('publicProfiles').doc(d.id));
  });
  await batch.commit();
  console.log(JSON.stringify({ ok: true, deleted: seeds.length }, null, 2));
}

async function main() {
  if (!/^part_\d+_lesson_[a-z0-9_]+_season_\d+$/.test(BOARD_ID)) {
    throw new Error('BOARD_ID gerekli: part_{p}_lesson_{key}_season_{s}');
  }
  console.error('[seed-realistic] projectId:', PROJECT_ID, 'board:', BOARD_ID);

  const boardRef = db.collection('lessonLeaderboards').doc(BOARD_ID);
  if (REMOVE) return remove(boardRef);

  const [boardSnap, entriesSnap] = await Promise.all([boardRef.get(), boardRef.collection('entries').get()]);
  const real = entriesSnap.docs.filter((d) => !d.id.startsWith(PREFIX));
  const oldSeeds = entriesSnap.docs.filter((d) => d.id.startsWith(PREFIX));
  const realScores = real.map((d) => Number(d.get('recordScore'))).filter(Number.isFinite);
  const hi = realScores.length && !ALLOW_ABOVE_REAL ? Math.min(MAX_SCORE, Math.min(...realScores) - 1) : MAX_SCORE;

  const meta = boardSnap.exists ? boardSnap.data() : {};
  const titleUnit = meta.titleUnit || real.map((d) => d.get('titleUnit')).find(Boolean) || 'Ünite Maratonu';
  const players = makePlayers(COUNT);
  const scores = makeScores(COUNT, MIN_SCORE, hi);

  const plan = players.map((p, i) => ({ id: `${PREFIX}${String(i).padStart(3, '0')}`, ...p, score: scores[i] }));

  console.log(
    JSON.stringify(
      {
        dryRun: DRY_RUN,
        boardExists: boardSnap.exists,
        realPlayers: real.map((d) => ({ name: d.get('displayName'), score: d.get('recordScore') })),
        oldSeedsReplaced: oldSeeds.length,
        scoreRange: [MIN_SCORE, hi],
        titleUnit,
        seeds: plan.map((p) => `${p.score}  ${p.name}`),
      },
      null,
      2,
    ),
  );
  if (DRY_RUN) return;

  const batch = db.batch();
  // Eski seed'lerden yeni planda olmayanlar siliniyor; aynı id'liler aşağıda üzerine yazılır.
  const planIds = new Set(plan.map((p) => p.id));
  oldSeeds.filter((d) => !planIds.has(d.id)).forEach((d) => {
    batch.delete(d.ref);
    batch.delete(db.collection('publicProfiles').doc(d.id));
  });
  if (!boardSnap.exists) {
    const m = /^part_(\d+)_lesson_(.+)_season_(\d+)$/.exec(BOARD_ID);
    batch.set(boardRef, {
      partId: parseInt(m[1], 10),
      lessonKey: m[2],
      season: parseInt(m[3], 10),
      titleUnit,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
  for (const p of plan) {
    batch.set(boardRef.collection('entries').doc(p.id), {
      recordScore: p.score,
      recordLabel: String(p.score),
      displayName: p.name,
      titleUnit,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    batch.set(db.collection('publicProfiles').doc(p.id), { avatarConfig: p.avatar });
  }
  await batch.commit();
  console.log(JSON.stringify({ ok: true, written: plan.length }, null, 2));
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

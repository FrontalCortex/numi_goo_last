/**
 * ÖRNEK VERİ — haftalık ilerleme kartını (WeeklyCardFragment) dolu görmek için bir hesaba iki haftalık
 * sahte kayıt ve üç ders kilometre taşı yazar (kullanıcı isteği 08.11.2026, Ted hesabı).
 *
 * Yazılan:
 *   users/{uid}/weeklyStats/2026-W43 (19–25 Eki) ve 2026-W44 (26 Eki–1 Kas): kupa puanları, Sobi
 *     seviyeleri, seri, günlük süre. Kart "kapanan en yeni hafta"yı gösterdiği için 8 Kasım Pazar
 *     günü kart W44'ü W43 ile kıyaslıyor. Sunucu (snapshotWeeklyStats) yalnızca KAPANAN haftayı
 *     yazdığından bu iki hafta onun üzerine yazdığı haftalarla çakışmıyor; ama 9 Kasım Pazartesi
 *     itibarıyla kart sunucunun gerçek W45 kaydını bu sahte W44 ile kıyaslar.
 *   users/{uid}/lessonMilestones/…: W44 içinde biten iki ders (örnek işlemli) ve hızlanan bir ünite maratonu.
 *
 * Dokunulan her belgenin ÖNCEKİ hâli users/{uid}/seedSamples/weeklyCard'a kaydediliyor; geri alma
 * onu aynen geri koyuyor (yoksa siliyor). Böylece hesapta önceden gerçek kilometre taşı varsa kaybolmuyor.
 *
 *   SEED_USER_ID=835398VV   hesap (users.userId)
 *   REMOVE=1                geri al
 *   REPLACE=1               varsa önce geri al, sonra yeniden yaz
 *   DRY_RUN=1               yalnızca yaz ne yapılacağını
 *
 * GitHub'da: .github/workflows/seed-weekly-card-sample.yml. functions/ dışında, çünkü functions/ push'u
 * fonksiyon deploy'u tetikliyor; firebase-admin functions/node_modules'tan (NODE_PATH).
 */
const admin = require('firebase-admin');

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });
const db = admin.firestore();

const USER_ID = process.env.SEED_USER_ID || '835398VV';
const REMOVE = process.env.REMOVE === '1';
const REPLACE = process.env.REPLACE === '1';
const DRY_RUN = process.env.DRY_RUN === '1';

/** Türkiye saatiyle `yyyy-MM-dd hh:mm` → ms (UTC+3). */
function trMs(day, hour = 18, min = 0) {
  return Date.parse(`${day}T${String(hour).padStart(2, '0')}:${String(min).padStart(2, '0')}:00+03:00`);
}

function weekDoc(id, start, end, body) {
  return {
    schema: 1,
    week: id,
    weekStart: start,
    weekEnd: end,
    totalTimeSeconds: null,
    finishedLessons: null,
    passedSteps: null,
    seedSample: true,
    ...body,
  };
}

/**
 * Ana satır körleme toplama kupası olsun diye 280 → 420 (1 basamaklı 4 sayı · 3,6 sn → 5 sayı · 3,4 sn);
 * abaküslü toplama 520 → 640 (aralık 4,2 → 4,05 sn); çıkarma 300 → 310 (kural aynı: kartta görünmemeli).
 * Sobi: 5'lik toplama 4 → 7; zihinden kuralsız toplama 3 → 3 (görünmemeli).
 */
function buildDocs() {
  const w43 = weekDoc('2026-W43', '2026-10-19', '2026-10-25', {
    cups: { addition_abacus_cup: 520, extraction_abacus_cup: 300, blinding_addition_abacus_cup: 280 },
    sobiLevels: { add_five: 4, m_add_direct: 3 },
    streak: { current: 6, longest: 9 },
    goalDays: ['2026-10-20', '2026-10-21', '2026-10-23', '2026-10-24', '2026-10-25'],
    dailySeconds: { '2026-10-20': 540, '2026-10-21': 720, '2026-10-23': 600, '2026-10-24': 480, '2026-10-25': 660 },
    weekSeconds: 3000,
  });
  const w44 = weekDoc('2026-W44', '2026-10-26', '2026-11-01', {
    cups: { addition_abacus_cup: 640, extraction_abacus_cup: 310, blinding_addition_abacus_cup: 420 },
    sobiLevels: { add_five: 7, m_add_direct: 3 },
    streak: { current: 12, longest: 12 },
    goalDays: ['2026-10-26', '2026-10-27', '2026-10-28', '2026-10-30', '2026-11-01'],
    dailySeconds: { '2026-10-26': 900, '2026-10-27': 840, '2026-10-28': 960, '2026-10-30': 780, '2026-11-01': 960 },
    weekSeconds: 4440,
  });
  return {
    'weeklyStats/2026-W43': w43,
    'weeklyStats/2026-W44': w44,
    'lessonMilestones/1_p1_i17_10_luk_toplama_kurallar': {
      partId: 1,
      stableId: 'p1_i17_10_luk_toplama_kurallar',
      finishedAtMs: trMs('2026-10-28', 18, 20),
      sampleOp: { numbers: [48, 37], multiply: false, answer: 85, text: '48 + 37 = 85' },
      seedSample: true,
    },
    'lessonMilestones/4_p4_i06_5_lik_toplama_3_saniye_arayl': {
      partId: 4,
      stableId: 'p4_i06_5_lik_toplama_3_saniye_arayl',
      finishedAtMs: trMs('2026-10-30', 17, 45),
      sampleOp: { numbers: [4, 5, 3, 6], multiply: false, answer: 18, text: '4 + 5 + 3 + 6 = 18' },
      seedSample: true,
    },
    'lessonMilestones/1_p1_i15_unite_maratonu': {
      partId: 1,
      stableId: 'p1_i15_unite_maratonu',
      chest: {
        firstSec: 80,
        firstAtMs: trMs('2026-10-21', 19, 0),
        bestSec: 52,
        bestAtMs: trMs('2026-10-29', 18, 10),
      },
      seedSample: true,
    },
  };
}

async function main() {
  const mode = REMOVE ? ' — GERİ AL' : REPLACE ? ' — DEĞİŞTİR' : '';
  console.log(`Proje: ${PROJECT_ID}, hesap ${USER_ID}${mode}${DRY_RUN ? ' (DRY RUN)' : ''}`);
  const users = await db.collection('users').where('userId', '==', USER_ID).limit(2).get();
  if (users.size !== 1) throw new Error(`${USER_ID} için ${users.size} kullanıcı bulundu`);
  const userRef = users.docs[0].ref;
  const seedRef = userRef.collection('seedSamples').doc('weeklyCard');
  const seedSnap = await seedRef.get();

  if (seedSnap.exists && (REMOVE || REPLACE)) {
    await restore(userRef, seedRef, seedSnap.get('before') || {});
    console.log('Eski örnek veri geri alındı.');
  }
  if (REMOVE) {
    if (!seedSnap.exists) console.log('Örnek veri yok; yapılacak bir şey yok.');
    return;
  }
  if (seedSnap.exists && !REPLACE) return console.log('Örnek veri zaten yazılmış; atlandı (REPLACE=1 ya da REMOVE=1).');

  const docs = buildDocs();
  const before = {};
  for (const path of Object.keys(docs)) {
    const s = await userRef.collection(path.split('/')[0]).doc(path.split('/')[1]).get();
    // null = önceden yoktu (geri almada silinir); değilse aynen geri konur.
    before[path] = s.exists ? s.data() : null;
    console.log(`${path}: ${s.exists ? 'VAR (önceki hâli saklanıyor)' : 'yok'}`);
  }
  if (DRY_RUN) return console.log(JSON.stringify(docs, null, 2));

  const batch = db.batch();
  for (const [path, data] of Object.entries(docs)) {
    const [col, id] = path.split('/');
    // Önceden gerçek kilometre taşı varsa birleştir (gerçek alanlar kalsın); geri alma eski hâli koyuyor.
    batch.set(userRef.collection(col).doc(id), data, { merge: col === 'lessonMilestones' });
  }
  batch.set(seedRef, { before, atMs: Date.now() });
  await batch.commit();
  console.log(`Örnek veri yazıldı (uid ${userRef.id}).`);
}

async function restore(userRef, seedRef, before) {
  const batch = db.batch();
  for (const [path, data] of Object.entries(before)) {
    const [col, id] = path.split('/');
    const ref = userRef.collection(col).doc(id);
    if (data === null) batch.delete(ref);
    else batch.set(ref, data);
    console.log(`${path}: ${data === null ? 'siliniyor' : 'önceki hâli geri konuyor'}`);
  }
  if (DRY_RUN) return;
  batch.delete(seedRef);
  await batch.commit();
}

main().then(() => process.exit(0)).catch((e) => {
  console.error(e);
  process.exit(1);
});

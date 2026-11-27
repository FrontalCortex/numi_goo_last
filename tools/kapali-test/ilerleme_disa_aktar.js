#!/usr/bin/env node
// Kapalı test: öğrenci hesaplarının ilerlemesini tek CSV'ye döker (kullanıcı adı başına bir satır).
// Takip tablosundaki (docs/kapali_test/Sorobit_kapali_test_takip.xlsx) "Son çalışılan gün" ve
// "Toplam süre (dk)" sütunlarını Firebase Console'dan tek tek okumak yerine buradan kopyalamak için.
//
// Kullanım (proje kökünden):
//   node tools/kapali-test/ilerleme_disa_aktar.js
//       → docs/kapali_test/ilerleme_<bugün>.csv
//   node tools/kapali-test/ilerleme_disa_aktar.js --testci-haric
//       → isTester=true hesaplar (geliştiricinin kendi hesapları) hiç yazılmaz.
//         Varsayılan: yazılır ama "test_hesabi" sütunu "evet" olur ve en alta dizilir.
//   node tools/kapali-test/ilerleme_disa_aktar.js --kayit-sonrasi 2026-10-20
//       → yalnızca o günden (dahil) sonra hesap açanlar.
//
// Kimlik: GOOGLE_APPLICATION_CREDENTIALS verilmişse o, yoksa ~/.config/numigo/ altındaki
// *firebase-adminsdk*.json servis hesabı anahtarı. Anahtar DEPOYA KONMAZ; betik yalnızca okur.
// firebase-admin, functions/node_modules'tan alınıyor: betik functions/ altında olsaydı
// claude/** dalına her push'ta BÜTÜN fonksiyonlar deploy edilirdi (deploy-functions.yml).
//
// CSV, Türkçe Excel'de çift tıklayınca doğru açılsın diye noktalı virgüllü ve UTF-8 BOM'lu;
// tarihler gg.aa.yyyy (tabloya yapıştırınca tarih olarak tanınıyor). Çıktı kişisel veri
// sayılır (kullanıcı adı + yaş + kaynak), docs/kapali_test/ .git/info/exclude ile git dışı.

const fs = require('fs');
const os = require('os');
const path = require('path');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..', '..');
const OUT_DIR = path.join(ROOT, 'docs', 'kapali_test');
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'numigo-new';
// GlobalLessonData'daki part'lar. ParentReportRepository 1..6'yı okuyor; 7-8 boş dönerse
// sorgu başına yalnızca 1 okuma yazılıyor, eklenirse sayıma kendiliğinden girsin.
const PARTS = [1, 2, 3, 4, 5, 6, 7, 8];
// Ünite maratonları (LessonItem.TYPE_CHEST) da `items` altında, ders gibi `stepIsFinish`
// taşıyor. Dokümanda tür alanı yazılmıyor (progressFieldsOf); ayırt etmenin tek yolu stableId.
const MARATHON_RE = /_unite_maratonu$/;

function parseArgs(argv) {
  const out = { excludeTesters: false, since: null };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--testci-haric') out.excludeTesters = true;
    else if (a === '--kayit-sonrasi') {
      const v = argv[++i] || '';
      if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) fail(`--kayit-sonrasi YYYY-AA-GG bekliyor, gelen: "${v}"`);
      out.since = new Date(`${v}T00:00:00`);
    } else fail(`Bilinmeyen seçenek: ${a}`);
  }
  return out;
}

function fail(msg) {
  console.error(`HATA: ${msg}`);
  process.exit(1);
}

function resolveCredentials() {
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) return;
  const dir = path.join(os.homedir(), '.config', 'numigo');
  const key = fs.existsSync(dir)
    ? fs.readdirSync(dir).find((f) => /firebase-adminsdk.*\.json$/.test(f))
    : null;
  if (!key) {
    fail(
      'Servis hesabı anahtarı bulunamadı. GOOGLE_APPLICATION_CREDENTIALS ver ya da anahtarı ' +
        `${dir} altına koy (Firebase Console > Proje ayarları > Hizmet hesapları).`,
    );
  }
  process.env.GOOGLE_APPLICATION_CREDENTIALS = path.join(dir, key);
}

function loadAdmin() {
  try {
    return createRequire(path.join(ROOT, 'functions', 'package.json'))('firebase-admin');
  } catch (e) {
    fail('firebase-admin yok. Bir kez çalıştır: cd functions; npm install');
  }
}

// AnalyticsLogger.ageGroupFor ile aynı dilimler ve aynı hesap (UTC yılı − doğum yılı);
// GA4'teki age_group boyutuyla yan yana konabilsin diye.
function ageGroupFor(birthYear) {
  const age = new Date().getUTCFullYear() - birthYear;
  if (age <= 3) return '0-3';
  if (age <= 6) return '4-6';
  if (age <= 8) return '7-8';
  if (age <= 10) return '9-10';
  if (age <= 14) return '11-14';
  if (age <= 17) return '15-17';
  return '18+';
}

function pad(n) {
  return String(n).padStart(2, '0');
}

// Timestamp → gg.aa.yyyy, Türkiye saatiyle (makine saat dilimi): gece kaydolan testçinin
// katılım günü UTC'de bir önceki güne kaymasın.
function fmtTimestamp(ts) {
  if (!ts || typeof ts.toDate !== 'function') return '';
  const d = ts.toDate();
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}`;
}

// Seri günü "2026-10-19" (STREAK_DAY_RE), günlük süre anahtarı "20261019" (TimeTracker.dayKey).
function fmtDayKey(key) {
  const m = /^(\d{4})-?(\d{2})-?(\d{2})$/.exec(key || '');
  return m ? `${m[3]}.${m[2]}.${m[1]}` : '';
}

function csvCell(v) {
  const s = v === null || v === undefined ? '' : String(v);
  return /[";\r\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

async function lessonCounts(userRef) {
  // Yalnızca biten dokümanlar çekiliyor (select() ile alansız): her öğrencide ~150 doküman var
  // ve seedAllLessonProgressIfMissing hepsini baştan yazdığı için dokümanın VARLIĞI derse
  // girildiği anlamına gelmiyor (bkz. ParentReportRepository). Okuma = biten ders sayısı.
  const snaps = await Promise.all(
    PARTS.map((p) =>
      userRef.collection('lessonProgress').doc(String(p)).collection('items')
        .where('stepIsFinish', '==', true).select().get(),
    ),
  );
  let lessons = 0;
  let marathons = 0;
  let furthest = '';
  for (const snap of snaps) {
    for (const d of snap.docs) {
      if (MARATHON_RE.test(d.id)) marathons++;
      else lessons++;
      // stableId'ler "p<part>_i<iki haneli sıra>_..." biçiminde; metin sıralaması müfredat
      // sırasıyla aynı (part'lar tek hane). Kilitler sırayla açıldığı için en ileri biten =
      // öğrencinin ulaştığı yer.
      if (d.id > furthest) furthest = d.id;
    }
  }
  return { lessons, marathons, furthest };
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  resolveCredentials();
  const admin = loadAdmin();
  if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });
  const db = admin.firestore();

  const usersSnap = await db.collection('users').where('role', '==', 'STUDENT').get();
  let docs = usersSnap.docs;
  if (opts.since) {
    docs = docs.filter((d) => {
      const c = d.get('createdAt');
      return c && typeof c.toDate === 'function' && c.toDate() >= opts.since;
    });
  }
  const testerCount = docs.filter((d) => d.get('isTester') === true).length;
  if (opts.excludeTesters) docs = docs.filter((d) => d.get('isTester') !== true);

  const rows = await Promise.all(
    docs.map(async (doc) => {
      const u = doc.data();
      const [streakSnap, lc] = await Promise.all([
        doc.ref.collection('streak').doc('state').get(),
        lessonCounts(doc.ref),
      ]);
      const st = streakSnap.exists ? streakSnap.data() : {};
      // lastDay günlük HEDEF tamamlanınca ilerliyor; hedefe ulaşmadan girip çıkan gün orada
      // görünmez. dailyTimeSpent ise her senkronda yazılıyor ama yalnızca son 14 günü tutuyor
      // (TimeTracker.RETENTION_DAYS): ikisi birlikte "geldi ama hedefi bitirmedi"yi gösteriyor.
      const daily = u.dailyTimeSpent && typeof u.dailyTimeSpent === 'object' ? u.dailyTimeSpent : {};
      const usedDays = Object.keys(daily).filter((k) => Number(daily[k]) > 0).sort();
      const birthYear = Number(u.birthYear);
      const createdAt = u.createdAt && typeof u.createdAt.toDate === 'function' ? u.createdAt.toDate() : null;
      return {
        sortKey: createdAt ? createdAt.getTime() : 0,
        isTester: u.isTester === true,
        cells: [
          u.userId || '',
          Number.isFinite(birthYear) && birthYear > 1900 ? ageGroupFor(birthYear) : '',
          Number.isFinite(birthYear) && birthYear > 1900 ? birthYear : '',
          u.sorobanLevel || '',
          u.acquisitionSource || '',
          fmtTimestamp(u.createdAt),
          fmtDayKey(st.lastDay),
          Math.max(0, Math.trunc(Number(st.current) || 0)),
          Array.isArray(st.recentDays) ? st.recentDays.length : 0,
          fmtDayKey(usedDays[usedDays.length - 1]),
          usedDays.length,
          Math.round((Number(u.totalTimeSpent) || 0) / 60),
          lc.lessons,
          lc.marathons,
          lc.furthest,
          u.isTester === true ? 'evet' : '',
          doc.id,
        ],
      };
    }),
  );

  // Testçiler yeniden yeniye, test hesapları en altta: tabloya yeni katılanlar üstte görünsün.
  rows.sort((a, b) => (a.isTester - b.isTester) || (b.sortKey - a.sortKey));

  const header = [
    'kullanici_adi', // users.userId — formlardaki ve takip tablosundaki "Kullanıcı adı"
    'yas_grubu',
    'dogum_yili',
    'abakus', // sorobanLevel: none / some / good (13 öncesi kayıtlarda boş)
    'kaynak', // acquisitionSource
    'kayit_tarihi',
    'seri_son_gun', // streak/state.lastDay — takip tablosundaki "Son çalışılan gün"
    'seri_guncel', // streak/state.current
    'hedef_gunu_son21', // streak/state.recentDays: son 21 günde hedefin tamamlandığı gün sayısı
    'son_kullanim_gunu', // dailyTimeSpent'teki son gün (hedef tamamlanmasa da)
    'kullanim_gunu_son14',
    'toplam_sure_dk', // totalTimeSpent (saniye) / 60 — tablodaki "Toplam süre (dk)"
    'biten_ders',
    'biten_maraton',
    'en_ileri_biten', // en ileri biten dersin stableId'si
    'test_hesabi', // isTester
    'uid',
  ];
  const lines = [header, ...rows.map((r) => r.cells)].map((r) => r.map(csvCell).join(';'));

  const now = new Date();
  const stamp = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const outPath = path.join(OUT_DIR, `ilerleme_${stamp}.csv`);
  // Excel dosya açıkken yazamaz (EBUSY); o durumda saatli ikinci bir ad dene.
  let written = outPath;
  try {
    fs.writeFileSync(outPath, '﻿' + lines.join('\r\n') + '\r\n', 'utf8');
  } catch (e) {
    if (e.code !== 'EBUSY' && e.code !== 'EPERM') throw e;
    written = outPath.replace(/\.csv$/, `_${pad(now.getHours())}${pad(now.getMinutes())}.csv`);
    fs.writeFileSync(written, '﻿' + lines.join('\r\n') + '\r\n', 'utf8');
  }

  console.log(`Proje: ${PROJECT_ID}`);
  console.log(
    `${rows.length} öğrenci yazıldı` +
      (opts.excludeTesters ? ` (${testerCount} test hesabı dışlandı)` : ` (${testerCount} test hesabı işaretli)`),
  );
  console.log(path.relative(ROOT, written));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

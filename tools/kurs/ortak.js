// Kurs betiklerinin ortak parçaları: kimlik, firebase-admin, kullanıcı adı eşleştirme.
// kapali-test/kurs_pro_ver.js ile aynı düzen (servis hesabı anahtarı ~/.config/numigo/;
// betikler functions/ dışında, push'ta bütün fonksiyonların deploy'unu tetiklemesin diye).

const fs = require('fs');
const os = require('os');
const path = require('path');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..', '..');
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'numigo-new';

function fail(msg) {
  console.error(`HATA: ${msg}`);
  process.exit(1);
}

function resolveCredentials() {
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS || process.env.FIRESTORE_EMULATOR_HOST) return;
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

/** firebase-admin'i functions/node_modules'tan yükleyip Firestore'u döndürür. */
function openFirestore() {
  resolveCredentials();
  let admin;
  try {
    admin = createRequire(path.join(ROOT, 'functions', 'package.json'))('firebase-admin');
  } catch (e) {
    fail('firebase-admin yok. Bir kez çalıştır: cd functions; npm install');
  }
  if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });
  return { admin, db: admin.firestore() };
}

/** classes.js'i (havuz kararı) betiklerde de aynı kaynaktan kullanmak için. */
function loadClasses() {
  return require(path.join(ROOT, 'functions', 'classes.js'));
}

// Formdan "@Ali123" ya da büyük/küçük harf farkıyla gelebiliyor.
function normalizeName(ad) {
  return String(ad || '').trim().replace(/^@/, '').toLocaleLowerCase('tr');
}

/**
 * Kullanıcı adı → dokümanlar. Büyük/küçük harf duyarsız eşleşsin diye bütün kullanıcılar
 * okunuyor; hesap sayısı küçük.
 */
async function usersByName(db) {
  const snap = await db.collection('users').get();
  const out = new Map();
  snap.docs.forEach((d) => {
    const ad = normalizeName(d.get('userId'));
    if (!ad) return;
    if (!out.has(ad)) out.set(ad, []);
    out.get(ad).push(d);
  });
  return out;
}

module.exports = { ROOT, PROJECT_ID, fail, openFirestore, loadClasses, normalizeName, usersByName };

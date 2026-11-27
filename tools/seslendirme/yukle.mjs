// Üretilen öğretici seslerini Firebase Storage'a yükler:
//   tools/seslendirme/cikti/<dil>/<anahtar>.mp3 → gs://numigo-new.firebasestorage.app/tutorial_audio/<dil>_<anahtar>.mp3
// storage.rules'ta tutorial_audio/ herkese okunur, yazma yalnızca Admin SDK (bu betik).
//
// Kimlik: Admin SDK uygulama varsayılan kimliği (gcloud auth application-default login) ya da
// GOOGLE_APPLICATION_CREDENTIALS ile hizmet hesabı JSON'u — functions/scripts/ ile aynı.
// firebase-admin, functions/node_modules'tan alınır (worktree'de yoksa ana checkout'takinden).
//
// Kullanım (depo kökünden):
//   node tools/seslendirme/yukle.mjs --dil en --kuru      ne yükleneceğini listeler, hiçbir şey yazmaz
//   node tools/seslendirme/yukle.mjs --dil en             yükler (Storage'da aynı boyutta olanı atlar)
//   --kaynak <klasör>   varsayılan cikti/<dil>
//   --es <n>            aynı anda yükleme, varsayılan 8
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';
import { argumanlar, ciktiKlasoru, DILLER, KOK } from './ortak.mjs';

const PROJE = process.env.FIREBASE_PROJECT_ID || 'numigo-new';
const KOVA = process.env.STORAGE_BUCKET || 'numigo-new.firebasestorage.app';
// Bir sesin içeriği değişirse uygulama eski kopyayı önbellekten çalabilir; yeniden üretilen ses
// aynı adla yüklenince en geç bu süre sonra yenilenir.
const ONBELLEK = 'public, max-age=2592000';

const a = argumanlar();
const dil = a.dil;
if (!DILLER[dil]) { console.error('Kullanım: node tools/seslendirme/yukle.mjs --dil en|id [--kuru]'); process.exit(1); }
const kaynak = path.resolve(a.kaynak || ciktiKlasoru(dil));
const es = Math.max(1, Number(a.es ?? 8));

function adminYukle() {
  const adaylar = [path.join(KOK, 'functions', 'package.json')];
  try {
    const ortak = execFileSync('git', ['rev-parse', '--path-format=absolute', '--git-common-dir'], { cwd: KOK, encoding: 'utf8' }).trim();
    adaylar.push(path.join(path.dirname(ortak), 'functions', 'package.json'));
  } catch { /* yalnızca ilk aday */ }
  for (const p of adaylar) {
    try { return createRequire(p)('firebase-admin'); } catch { /* sonraki */ }
  }
  throw new Error('firebase-admin bulunamadı: `cd functions; npm install` çalıştırın.');
}

const dosyalar = fs.existsSync(kaynak) ? fs.readdirSync(kaynak).filter((f) => f.endsWith('.mp3')).sort() : [];
if (!dosyalar.length) { console.log(`${kaynak} içinde mp3 yok.`); process.exit(0); }

const admin = adminYukle();
if (!admin.apps.length) admin.initializeApp({ projectId: PROJE, storageBucket: KOVA });
const kova = admin.storage().bucket();

// Storage'daki mevcut boyutlar (okuma; --kuru'da da yapılır).
const uzak = new Map();
const [mevcut] = await kova.getFiles({ prefix: `tutorial_audio/${dil}_` });
for (const f of mevcut) uzak.set(f.name, Number(f.metadata.size));

const isler = [];
let ayni = 0;
for (const f of dosyalar) {
  const yerel = path.join(kaynak, f);
  const hedef = `tutorial_audio/${dil}_${f}`;
  const boyut = fs.statSync(yerel).size;
  if (uzak.get(hedef) === boyut) { ayni++; continue; }
  isler.push({ yerel, hedef, boyut, yeni: !uzak.has(hedef) });
}
const toplamKB = Math.round(isler.reduce((t, i) => t + i.boyut, 0) / 1024);
console.log(`${dosyalar.length} dosya: ${ayni} zaten aynı, ${isler.length} yüklenecek (${toplamKB} KB) → gs://${KOVA}/tutorial_audio/${dil}_*`);

if (a.kuru) {
  for (const i of isler) console.log(`  ${i.yeni ? 'yeni ' : 'değişti'} ${i.hedef} (${Math.round(i.boyut / 1024)} KB)`);
  process.exit(0);
}

let sira = 0, biten = 0, hata = 0;
async function isci() {
  while (sira < isler.length) {
    const i = isler[sira++];
    try {
      await kova.upload(i.yerel, {
        destination: i.hedef,
        resumable: false,
        metadata: { contentType: 'audio/mpeg', cacheControl: ONBELLEK },
      });
      biten++;
      if (biten % 50 === 0 || biten === isler.length) console.log(`[${biten}/${isler.length}]`);
    } catch (e) {
      hata++;
      console.error(`HATA ${i.hedef}: ${e.message}`);
    }
  }
}
await Promise.all(Array.from({ length: es }, isci));
console.log(`Bitti: ${biten} yüklendi, ${hata} hata.`);
process.exit(hata ? 1 : 0);

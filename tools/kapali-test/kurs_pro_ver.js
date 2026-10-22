#!/usr/bin/env node
// Kursla test: kurs öğrencilerine elle Pro ve danışma kredisi verir (ya da geri alır).
// Kursun öğrencileri Play'den abone olmadan Pro kullanacak ve öğretmenlerine sınırsız soru
// sorabilecek. Yeni sürüm ve deploy gerektirmiyor: uygulama ve sunucu planı `plan` /
// `planExpiresAt` alanlarından okuyor (PlanStatus.effectivePlan, functions effectivePlan),
// soru düğmesi yalnızca `questionCredits >= 1`'e bakıyor (AskQuestionButtonBinder).
//
// Kullanım (proje kökünden):
//   node tools/kapali-test/kurs_pro_ver.js ali123 ayse_k
//       → KURU ÇALIŞMA: kimin değişeceğini gösterir, hiçbir şey yazmaz.
//   node tools/kapali-test/kurs_pro_ver.js --dosya docs/kapali_test/kurs_ogrencileri.txt
//       → kullanıcı adları dosyadan (satır başına bir ad; boş satır ve # yorum atlanır).
//   ... --yaz
//       → gerçekten yazar: plan=Pro, planExpiresAt=bitiş, questionCredits=en az --kredi.
//   ... --bitis 2026-12-15   (varsayılan 2026-11-30; Türkiye saatiyle o günün 23:59:59'u)
//   ... --kredi 999          (varsayılan 999; daha yüksek bakiyesi olanın kredisi düşürülmez)
//   ... --geri-al --yaz
//       → elle verilen Pro'yu kapatır (plan=Free, planExpiresAt=null) ve krediyi 0'a çeker.
//
// "Sınırsız" kredi bilerek büyük bir sayı: gerçek sınırsızlık için askTeacherQuestion'ın
// değişmesi, yani BÜTÜN fonksiyonların deploy'u gerekirdi. 999 iki haftada bitmez; 4 basamak
// üst paneldeki kredi kutusuna sığmayabilir. planExpiresAt geçince Pro kendiliğinden kapanır,
// KREDİ KAPANMAZ: test bitince --geri-al çalıştırılmalı.
//
// Gerçek Play abonesine dokunulmaz: `planProductId` ya da `planPurchaseTokenHash` doluysa hesap
// atlanır (elle bitiş tarihi yazmak abonenin yenilenen dönemini ezer). Geri almada da aynı kural:
// yalnızca elle verilmiş Pro kapatılır. Yeni bir işaret alanı yazılmıyor; ayırt etmek için bu
// iki alanın boş olması yetiyor. Yalnızca role=STUDENT hesaplara yazılır (öğretmen onayla açılıyor).
//
// Kimlik ve firebase-admin, ilerleme_disa_aktar.js ile aynı (servis hesabı anahtarı
// ~/.config/numigo/; betik functions/ dışında, push'ta deploy tetiklemesin diye).

const fs = require('fs');
const os = require('os');
const path = require('path');
const { createRequire } = require('module');

const ROOT = path.resolve(__dirname, '..', '..');
const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'numigo-new';
const VARSAYILAN_BITIS = '2026-11-30';
const VARSAYILAN_KREDI = 999;

function fail(msg) {
  console.error(`HATA: ${msg}`);
  process.exit(1);
}

function parseArgs(argv) {
  const out = { adlar: [], yaz: false, geriAl: false, bitis: VARSAYILAN_BITIS, kredi: VARSAYILAN_KREDI };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--yaz') out.yaz = true;
    else if (a === '--geri-al') out.geriAl = true;
    else if (a === '--bitis') {
      out.bitis = argv[++i] || '';
      if (!/^\d{4}-\d{2}-\d{2}$/.test(out.bitis)) fail(`--bitis YYYY-AA-GG bekliyor, gelen: "${out.bitis}"`);
    } else if (a === '--kredi') {
      out.kredi = Number.parseInt(argv[++i], 10);
      if (!(out.kredi >= 1)) fail('--kredi 1 ya da daha büyük bir sayı bekliyor');
    } else if (a === '--dosya') {
      const dosya = argv[++i];
      if (!dosya || !fs.existsSync(dosya)) fail(`--dosya bulunamadı: "${dosya}"`);
      fs.readFileSync(dosya, 'utf8').split(/\r?\n/).forEach((s) => {
        const t = s.trim();
        if (t && !t.startsWith('#')) out.adlar.push(t);
      });
    } else if (a.startsWith('--')) fail(`Bilinmeyen seçenek: ${a}`);
    else out.adlar.push(a);
  }
  if (!out.adlar.length) fail('En az bir kullanıcı adı ya da --dosya ver.');
  return out;
}

// Formdan "@Ali123" ya da büyük/küçük harf farkıyla gelebiliyor (KAPALI_TEST_FORMLARI.md ipuçları).
function normalize(ad) {
  return String(ad || '').trim().replace(/^@/, '').toLocaleLowerCase('tr');
}

// Türkiye saatiyle (UTC+3, yaz saati yok) o günün son saniyesi, milisaniye.
function bitisMs(gun) {
  const ms = Date.parse(`${gun}T23:59:59+03:00`);
  if (!Number.isFinite(ms)) fail(`Geçersiz tarih: ${gun}`);
  if (ms < Date.now()) fail(`Bitiş tarihi geçmişte: ${gun}`);
  return ms;
}

function tarihYaz(ms) {
  return ms ? new Date(ms).toLocaleString('tr-TR', { timeZone: 'Europe/Istanbul' }) : '-';
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

function gercekAbone(u) {
  return !!(u.planProductId || u.planPurchaseTokenHash);
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const bitis = opts.geriAl ? null : bitisMs(opts.bitis);
  resolveCredentials();
  const admin = loadAdmin();
  if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });
  const db = admin.firestore();

  // Kullanıcı adı büyük/küçük harf duyarsız eşleşsin diye userId ile sorgu yerine bütün
  // öğrenciler okunuyor; kapalı testte hesap sayısı küçük.
  const snap = await db.collection('users').get();
  const adaGore = new Map();
  snap.docs.forEach((d) => {
    const ad = normalize(d.get('userId'));
    if (!ad) return;
    if (!adaGore.has(ad)) adaGore.set(ad, []);
    adaGore.get(ad).push(d);
  });

  console.log(opts.geriAl
    ? 'İşlem: GERİ AL (plan=Free, planExpiresAt=null, questionCredits=0)'
    : `İşlem: PRO VER (plan=Pro, bitiş ${tarihYaz(bitis)}, kredi en az ${opts.kredi})`);
  console.log(opts.yaz ? 'Kip: YAZ\n' : 'Kip: KURU ÇALIŞMA (hiçbir şey yazılmayacak; yazmak için --yaz)\n');

  const yazilacak = [];
  const atlanan = [];
  const gorulen = new Set();
  for (const ham of opts.adlar) {
    const ad = normalize(ham);
    if (gorulen.has(ad)) continue;
    gorulen.add(ad);
    const bulunan = adaGore.get(ad) || [];
    if (bulunan.length === 0) { atlanan.push([ham, 'bulunamadı']); continue; }
    if (bulunan.length > 1) { atlanan.push([ham, `${bulunan.length} hesapta aynı ad, elle bak`]); continue; }
    const doc = bulunan[0];
    const u = doc.data();
    if (u.role !== 'STUDENT') { atlanan.push([ham, `öğrenci değil (role=${u.role || '-'})`]); continue; }
    if (gercekAbone(u)) { atlanan.push([ham, 'gerçek Play abonesi, dokunulmadı']); continue; }

    const krediSimdi = Number.parseInt(u.questionCredits, 10) || 0;
    let guncelleme;
    if (opts.geriAl) {
      if (u.plan !== 'Pro' && krediSimdi === 0) { atlanan.push([ham, 'zaten Pro değil, kredi 0']); continue; }
      guncelleme = { plan: 'Free', planExpiresAt: null, questionCredits: 0 };
    } else {
      guncelleme = { plan: 'Pro', planExpiresAt: bitis, questionCredits: Math.max(krediSimdi, opts.kredi) };
    }
    const once = `plan=${u.plan || 'Free'}, bitiş=${tarihYaz(u.planExpiresAt)}, kredi=${krediSimdi}`;
    const sonra = `plan=${guncelleme.plan}, bitiş=${tarihYaz(guncelleme.planExpiresAt)}, kredi=${guncelleme.questionCredits}`;
    yazilacak.push({ ham, ref: doc.ref, guncelleme, once, sonra, uid: doc.id });
  }

  yazilacak.forEach((y) => console.log(`  ${y.ham} (${y.uid})\n    önce : ${y.once}\n    sonra: ${y.sonra}`));
  if (atlanan.length) {
    console.log('\nAtlananlar:');
    atlanan.forEach(([ad, neden]) => console.log(`  ${ad}: ${neden}`));
  }

  if (!opts.yaz) {
    console.log(`\n${yazilacak.length} hesap değişecek. Yazmak için aynı komutu --yaz ile çalıştır.`);
    return;
  }
  // Tek tek update: batch 500 sınırına takılmasın diye değil, yarıda kalırsa hangisinin
  // yazıldığı belli olsun diye. update(): belge yoksa yaratmaz.
  for (const y of yazilacak) {
    await y.ref.update(y.guncelleme);
    console.log(`  yazıldı: ${y.ham}`);
  }
  console.log(`\n${yazilacak.length} hesap güncellendi. Öğrenci uygulamayı tamamen kapatıp açsın.`);
}

main().catch((e) => fail(e.stack || e.message));

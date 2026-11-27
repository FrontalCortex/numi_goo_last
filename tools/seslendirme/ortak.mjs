// uret.mjs ve yukle.mjs'nin ortak parçaları: yollar, anahtar okuma, öğretici metinleri.
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const BURASI = path.dirname(fileURLToPath(import.meta.url));
export const KOK = path.resolve(BURASI, '..', '..');
export const RES = path.join(KOK, 'app', 'src', 'main', 'res');
export const DILLER = { en: 'values-en', id: 'values-in', ar: 'values-ar' };

export function argumanlar(argv = process.argv.slice(2)) {
  const a = {};
  for (let i = 0; i < argv.length; i++) {
    if (!argv[i].startsWith('--')) continue;
    const ad = argv[i].slice(2);
    const sonraki = argv[i + 1];
    if (sonraki === undefined || sonraki.startsWith('--')) a[ad] = true;
    else { a[ad] = sonraki; i++; }
  }
  return a;
}

// Anahtar sırası: ortam değişkeni → bu çalışma ağacının functions/.env.local'ı → ana
// checkout'unki (worktree'lerde .env.local yok, gitignore'da). Değer asla yazdırılmaz.
export function ortamDegeri(ad) {
  if (process.env[ad]) return process.env[ad];
  const adaylar = [path.join(KOK, 'functions', '.env.local')];
  try {
    const ortak = execFileSync('git', ['rev-parse', '--path-format=absolute', '--git-common-dir'], { cwd: KOK, encoding: 'utf8' }).trim();
    adaylar.push(path.join(path.dirname(ortak), 'functions', '.env.local'));
  } catch { /* git yoksa yalnızca ilk aday */ }
  for (const dosya of adaylar) {
    if (!fs.existsSync(dosya)) continue;
    for (const satir of fs.readFileSync(dosya, 'utf8').split(/\r?\n/)) {
      const m = satir.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
      if (m && m[1] === ad && m[2]) return m[2].replace(/^["']|["']$/g, '');
    }
  }
  return null;
}

// Android string kaynağını çözer: XML varlıkları, \' \" \n kaçışları, dıştaki tırnaklar.
function androidMetni(ham) {
  let s = ham.trim();
  s = s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(+n))
    .replace(/&#x([0-9a-f]+);/gi, (_, n) => String.fromCodePoint(parseInt(n, 16)))
    .replace(/&amp;/g, '&');
  // Android, kaçışsız çift tırnakları metinden siler ("..." tüm cümleyi sarar).
  s = s.replace(/\\"/g, '\u0000').replace(/"/g, '').replace(/\u0000/g, '"');
  s = s.replace(/\\n/g, ' ').replace(/\\t/g, ' ').replace(/\\(['@?\\])/g, '$1');
  return s;
}

const BIRLER = {
  en: ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve',
    'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen'],
  id: ['nol', 'satu', 'dua', 'tiga', 'empat', 'lima', 'enam', 'tujuh', 'delapan', 'sembilan', 'sepuluh', 'sebelas'],
};
const ONLAR_EN = ['', '', 'twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy', 'eighty', 'ninety'];

// Arapça: sayının tek başına (sayılan ad olmadan) fasih okunuşu.
const BIRLER_AR = ['صفر', 'واحد', 'اثنان', 'ثلاثة', 'أربعة', 'خمسة', 'ستة', 'سبعة', 'ثمانية', 'تسعة', 'عشرة'];
const ONLAR_AR = ['', '', 'عشرون', 'ثلاثون', 'أربعون', 'خمسون', 'ستون', 'سبعون', 'ثمانون', 'تسعون'];
// Dişil sayılan adın önünde 3–10 (ters cinsiyet kuralı).
const DISIL_AD_ICIN_AR = { 3: 'ثلاث', 4: 'أربع', 5: 'خمس', 6: 'ست', 7: 'سبع', 8: 'ثماني', 9: 'تسع', 10: 'عشر' };
const YUZLER_AR = ['', 'مئة', 'مئتان', 'ثلاثمئة', 'أربعمئة', 'خمسمئة', 'ستمئة', 'سبعمئة', 'ثمانمئة', 'تسعمئة'];

function sayiYaziylaAr(n) {
  const ve = (bas, kalan) => (kalan ? `${bas} و${sayiYaziylaAr(kalan)}` : bas);
  if (n <= 10) return BIRLER_AR[n];
  if (n === 11) return 'أحد عشر';
  if (n === 12) return 'اثنا عشر';
  if (n < 20) return `${BIRLER_AR[n % 10]} عشر`;
  if (n < 100) return n % 10 ? `${BIRLER_AR[n % 10]} و${ONLAR_AR[Math.floor(n / 10)]}` : ONLAR_AR[n / 10];
  if (n < 1000) return ve(YUZLER_AR[Math.floor(n / 100)], n % 100);
  const bin = Math.floor(n / 1000);
  // 1000 ألف, 2000 ألفان, 3000–10000 "ثلاثة آلاف", 11000 ve üstü "أحد عشر ألفًا".
  const binler = bin === 1 ? 'ألف' : bin === 2 ? 'ألفان' : bin <= 10 ? `${BIRLER_AR[bin]} آلاف` : `${sayiYaziylaAr(bin)} ألفًا`;
  return ve(binler, n % 1000);
}

// Nesne ve edat sonrası (أضِف 2، إلى 20، لـ 12) sayının nasb/cer biçimi: اثنين، اثني عشر، عشرين، مئتين، ألفين.
const EGIK_AR = { اثنان: 'اثنين', اثنا: 'اثني', مئتان: 'مئتين', ألفان: 'ألفين' };
const egikHalAr = (yazi) => yazi.replace(/(^|\s)(و?)([؀-ۿ]+)/g, (_, b, ve, k) => b + ve + (EGIK_AR[k]
  || (ONLAR_AR.includes(k) ? `${k.slice(0, -2)}ين` : k)));

/** 0–99999 arası sayının yazıyla okunuşu ("64260" → "sixty-four thousand two hundred sixty"). */
export function sayiYaziyla(n, dil) {
  if (dil === 'ar') return sayiYaziylaAr(n);
  if (dil === 'en') {
    if (n < 20) return BIRLER.en[n];
    if (n < 100) return ONLAR_EN[Math.floor(n / 10)] + (n % 10 ? `-${BIRLER.en[n % 10]}` : '');
    if (n < 1000) return `${BIRLER.en[Math.floor(n / 100)]} hundred${n % 100 ? ` ${sayiYaziyla(n % 100, dil)}` : ''}`;
    return `${sayiYaziyla(Math.floor(n / 1000), dil)} thousand${n % 1000 ? ` ${sayiYaziyla(n % 1000, dil)}` : ''}`;
  }
  if (n < 12) return BIRLER.id[n];
  if (n < 20) return `${BIRLER.id[n % 10]} belas`;
  if (n < 100) return `${BIRLER.id[Math.floor(n / 10)]} puluh${n % 10 ? ` ${BIRLER.id[n % 10]}` : ''}`;
  if (n < 1000) return `${n < 200 ? 'seratus' : `${BIRLER.id[Math.floor(n / 100)]} ratus`}${n % 100 ? ` ${sayiYaziyla(n % 100, dil)}` : ''}`;
  return `${n < 2000 ? 'seribu' : `${sayiYaziyla(Math.floor(n / 1000), dil)} ribu`}${n % 1000 ? ` ${sayiYaziyla(n % 1000, dil)}` : ''}`;
}

/**
 * Ekranda görsel olan işaretleri okutma: [mavi kısım] → parantezsiz, "a -> b" → "a, b".
 *
 * Sayılar ve çarpma (ilk üretimde yazıyla doğrulandı, 03.11.2026): ses modeli "64260"ı "six four two
 * six two", "1136"yı "eleven thirty-six" diye okuyor — basamak değeri öğreten derste yanlış; "7x8 =
 * 56"yı da anlamsız sözlere çeviriyordu. 100 ve üstü sayılar yazıyla, "a x b = c" sözle veriliyor;
 * yalnızca sayıdan oluşan kısa cümleler de ("5", "88") yazıyla (tek sözcükte model kararsız).
 */
export function okunacakMetin(metin, dil = 'en') {
  const kez = { id: 'kali', ar: 'في' }[dil] || 'times';
  const esit = { id: 'sama dengan', ar: 'يساوي' }[dil] || 'equals';
  let s = metin
    .replace(/[[\]]/g, '')
    .replace(/\s*(->|→|=>)\s*/g, ', ')
    .replace(/(\d)\s*[x×*]\s*(\d)/g, `$1 ${kez} $2`)
    .replace(/\s*=\s*/g, ` ${esit} `)
    .replace(/\s+/g, ' ')
    .trim();
  // Arapça (Rawi, 03.11.2026): rakamla yazılan sayıyı güvenilmez okuyor — 3+ basamağı bozuyor ("898" →
  // "8892"), tek başına sayıyı İngilizce okuyor ("6" → "six"), iki basamaklıyı da yer yer karıştırıyor
  // ("42" → "28"). Bütün sayılar yazıyla. Sayılan ad dişil çoğulsa (خرزات، خانات، ثوانٍ) 3–10 eril biçim
  // alır ("3 خرزات" → "ثلاث خرزات"); "3 و4 و5 خانات" zincirinin hepsi.
  if (dil === 'ar') {
    const sayi = s.match(/^(-?)(\d+)$/);
    if (sayi && Number(sayi[2]) <= 99999) return `${sayi[1] ? 'سالب ' : ''}${sayiYaziyla(Number(sayi[2]), dil)}`;
    s = s.replace(/((?:\d+ و)*\d+)(\s+)(خرزات|خانات|ثوانٍ)/g, (_, zincir, bosluk, ad) =>
      zincir.replace(/\d+/g, (m) => DISIL_AD_ICIN_AR[Number(m)] || m) + bosluk + ad);
    // Özne/haber yerinde (الجواب 22، هو 12، يساوي 20) ref'î biçim, başka her yerde eğik biçim.
    return s.replace(/\d+/g, (m, yer, tum) => {
      if (Number(m) > 99999) return m;
      const yazi = sayiYaziyla(Number(m), dil);
      return /(الجواب|الناتج|هو|يساوي)\s*:?\s*$/.test(tum.slice(0, yer)) ? yazi : egikHalAr(yazi);
    });
  }
  if (/^-?\d+$/.test(s)) {
    const n = Math.abs(Number(s));
    return `${s.startsWith('-') ? 'minus ' : ''}${n <= 99999 ? sayiYaziyla(n, dil) : n}`;
  }
  // Endonezce ses iki basamaklıyı da rakam rakam okuyordu ("53" → "lima tiga"); orada bütün sayılar yazıyla.
  // Sıra sayısı "ke-2" → "kedua" ("ke-1" → "pertama").
  if (dil === 'id') s = s.replace(/\bke-(\d{1,2})\b/g, (_, n) => (n === '1' ? 'pertama' : `ke${sayiYaziyla(Number(n), dil)}`));
  const yaziylaOkunacak = dil === 'id' ? /\b\d{1,5}\b/g : /\b\d{3,5}\b/g;
  return s.replace(yaziylaOkunacak, (m) => sayiYaziyla(Number(m), dil));
}

// Dil dosyasındaki, Türkçe sesi (res/raw/<anahtar>.mp3) olan cümleler; Türkçe dosyadaki sırayla.
export function cumleler(dil) {
  const klasor = DILLER[dil];
  if (!klasor) throw new Error(`--dil en, id ya da ar olmalı (verilen: ${dil})`);
  const oku = (k) => {
    const xml = fs.readFileSync(path.join(RES, k, 'strings_tutorial.xml'), 'utf8');
    const harita = new Map();
    for (const m of xml.matchAll(/<string\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) harita.set(m[1], m[2]);
    return harita;
  };
  const turkce = oku('values');
  const ceviri = oku(klasor);
  const sesli = new Set(fs.readdirSync(path.join(RES, 'raw')).filter((f) => f.endsWith('.mp3')).map((f) => f.slice(0, -4)));
  const sonuc = [];
  const eksik = [];
  for (const anahtar of turkce.keys()) {
    if (!sesli.has(anahtar)) continue;
    let ham = ceviri.get(anahtar);
    if (ham === undefined) {
      // Yalnızca sayı olan cümleler ("100", "-12") çevrilmiyor; ses dili sayıyı kendi okur.
      const tr = androidMetni(turkce.get(anahtar));
      if (!/^-?\d+$/.test(tr)) { eksik.push(anahtar); continue; }
      ham = tr;
    }
    const metin = okunacakMetin(androidMetni(ham), dil);
    if (metin) sonuc.push({ anahtar, metin });
  }
  return { cumleler: sonuc, eksik };
}

export function ciktiKlasoru(dil) {
  return path.join(BURASI, 'cikti', dil);
}

// uret.mjs ve yukle.mjs'nin ortak parçaları: yollar, anahtar okuma, öğretici metinleri.
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export const BURASI = path.dirname(fileURLToPath(import.meta.url));
export const KOK = path.resolve(BURASI, '..', '..');
export const RES = path.join(KOK, 'app', 'src', 'main', 'res');
export const DILLER = { en: 'values-en', id: 'values-in' };

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

// Ekranda görsel olan işaretleri okutma: [mavi kısım] → parantezsiz, "a -> b" → "a, b".
export function okunacakMetin(metin) {
  return metin
    .replace(/[[\]]/g, '')
    .replace(/\s*(->|→|=>)\s*/g, ', ')
    .replace(/\s+/g, ' ')
    .trim();
}

// Dil dosyasındaki, Türkçe sesi (res/raw/<anahtar>.mp3) olan cümleler; Türkçe dosyadaki sırayla.
export function cumleler(dil) {
  const klasor = DILLER[dil];
  if (!klasor) throw new Error(`--dil en ya da id olmalı (verilen: ${dil})`);
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
      ham = tr.replace(/^-/, 'minus ');
    }
    const metin = okunacakMetin(androidMetni(ham));
    if (metin) sonuc.push({ anahtar, metin });
  }
  return { cumleler: sonuc, eksik };
}

export function ciktiKlasoru(dil) {
  return path.join(BURASI, 'cikti', dil);
}

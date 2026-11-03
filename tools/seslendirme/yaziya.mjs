// Üretilen sesleri ElevenLabs Speech to Text (scribe_v1) ile yazıya çevirip okunması gereken metinle
// karşılaştırır: atlanan/yanlış okunan kelimeleri ve sayıları bulur. Kulakla dinlemenin yerine
// "nereye kulak vermeli" listesi. Yazıya dökülen metinler `cikti/<dil>/_yazi.json`'da önbellekte
// (yeniden üretilen sesin kaydı silinir, yalnızca o yeniden çevrilir).
//
// Kullanım: node tools/seslendirme/yaziya.mjs --dil en [--esik 0.85]
// API anahtarında "Speech to Text" izni gerekiyor.
import fs from 'node:fs';
import path from 'node:path';
import { argumanlar, cumleler, ciktiKlasoru, ortamDegeri, sayiYaziyla } from './ortak.mjs';

const { dil = 'en', esik = '0.85' } = argumanlar();
const ESIK = Number(esik);
const anahtar = ortamDegeri('ELEVENLABS_API_KEY');
const klasor = ciktiKlasoru(dil);
const onbellekDosyasi = path.join(klasor, '_yazi.json');
const onbellek = fs.existsSync(onbellekDosyasi) ? JSON.parse(fs.readFileSync(onbellekDosyasi, 'utf8')) : {};

async function yaziya(dosya) {
  for (let deneme = 0; deneme < 5; deneme++) {
    const form = new FormData();
    form.append('model_id', 'scribe_v1');
    form.append('language_code', { id: 'ind', ar: 'ara' }[dil] || 'eng');
    form.append('file', new Blob([fs.readFileSync(dosya)], { type: 'audio/mpeg' }), path.basename(dosya));
    const r = await fetch('https://api.elevenlabs.io/v1/speech-to-text', { method: 'POST', headers: { 'xi-api-key': anahtar }, body: form });
    if (r.status === 429 || r.status >= 500) { await new Promise((ok) => setTimeout(ok, 2000 * (deneme + 1))); continue; }
    const j = await r.json();
    if (!r.ok) throw new Error(`${r.status} ${JSON.stringify(j).slice(0, 200)}`);
    return j.text || '';
  }
  throw new Error('çok deneme');
}

// Karşılaştırma harf düzeyinde: yazıya çeviren servis sayıları yazıyla döküyor ("four"), "take away"i
// bitişik yazıyor, "beads"i "beats" duyuyor. Metindeki sayılar önce o dilde yazıya çevrilir, sonra iki
// taraf yalnızca harflere indirgenip en uzun ortak alt dizinin oranına bakılır.
const BIRLER = {
  en: ['zero', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten', 'eleven', 'twelve',
    'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen'],
  id: ['nol', 'satu', 'dua', 'tiga', 'empat', 'lima', 'enam', 'tujuh', 'delapan', 'sembilan', 'sepuluh', 'sebelas'],
};
const ONLAR_EN = ['', '', 'twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy', 'eighty', 'ninety'];
function yaziyla(n) {
  if (dil === 'ar') return sayiYaziyla(n, dil);
  if (dil === 'en') {
    if (n < 20) return BIRLER.en[n];
    if (n < 100) return ONLAR_EN[Math.floor(n / 10)] + (n % 10 ? ' ' + BIRLER.en[n % 10] : '');
    if (n < 1000) return BIRLER.en[Math.floor(n / 100)] + ' hundred' + (n % 100 ? ' ' + yaziyla(n % 100) : '');
    return yaziyla(Math.floor(n / 1000)) + ' thousand' + (n % 1000 ? ' ' + yaziyla(n % 1000) : '');
  }
  if (n < 12) return BIRLER.id[n];
  if (n < 20) return BIRLER.id[n % 10] + ' belas';
  if (n < 100) return BIRLER.id[Math.floor(n / 10)] + ' puluh' + (n % 10 ? ' ' + BIRLER.id[n % 10] : '');
  if (n < 1000) return (n < 200 ? 'seratus' : BIRLER.id[Math.floor(n / 100)] + ' ratus') + (n % 100 ? ' ' + yaziyla(n % 100) : '');
  return (n < 2000 ? 'seribu' : yaziyla(Math.floor(n / 1000)) + ' ribu') + (n % 1000 ? ' ' + yaziyla(n % 1000) : '');
}
const sayiyiYaz = (t) => t.replace(/\d+/g, (m) => (Number(m) <= 99999 ? yaziyla(Number(m)) : m));
// Arapça: hareke ve tatvil atılır, elif / te-merbuta / elif-maksura biçimleri birleştirilir (servis farklı yazabiliyor).
const arapcaSade = (t) => (dil === 'ar'
  ? t.replace(/[ً-ْٰـ]/g, '').replace(/[أإآ]/g, 'ا').replace(/ة/g, 'ه').replace(/ى/g, 'ي')
  : t);
const harfler = (t) => arapcaSade(sayiyiYaz(t)).toLowerCase().replace(/[^\p{L}]/gu, '');
function ortakUzunluk(a, b) {
  const d = Array(b.length + 1).fill(0);
  for (const x of a) {
    let onceki = 0;
    for (let j = 1; j <= b.length; j++) {
      const simdi = d[j];
      d[j] = x === b[j - 1] ? onceki + 1 : Math.max(d[j], d[j - 1]);
      onceki = simdi;
    }
  }
  return d[b.length];
}

const liste = cumleler(dil).cumleler;
let sira = 0;
let yeni = 0;
async function isci() {
  while (sira < liste.length) {
    const c = liste[sira++];
    const dosya = path.join(klasor, `${c.anahtar}.mp3`);
    if (!fs.existsSync(dosya)) continue;
    const boyut = fs.statSync(dosya).size;
    if (onbellek[c.anahtar]?.boyut === boyut) continue;
    onbellek[c.anahtar] = { boyut, yazi: await yaziya(dosya) };
    if (++yeni % 50 === 0) {
      fs.writeFileSync(onbellekDosyasi, JSON.stringify(onbellek, null, 1));
      console.log(`  ${yeni} ses yazıya çevrildi`);
    }
  }
}
await Promise.all([isci(), isci(), isci()]);
fs.writeFileSync(onbellekDosyasi, JSON.stringify(onbellek, null, 1));

const supheli = [];
for (const c of liste) {
  const yazi = onbellek[c.anahtar]?.yazi;
  if (yazi == null) continue;
  const beklenen = harfler(c.metin);
  const duyulan = harfler(yazi);
  // İki yönlü: atlanan (beklenenin ne kadarı duyuldu) ve fazladan/yanlış okunan (duyulanın ne kadarı beklenen).
  const ortak = ortakUzunluk([...beklenen], [...duyulan]);
  const oran = Math.min(ortak / Math.max(1, beklenen.length), ortak / Math.max(1, duyulan.length));
  if (oran < ESIK) supheli.push({ anahtar: c.anahtar, oran, metin: c.metin, yazi });
}
console.log(`${dil}: ${Object.keys(onbellek).length} yazı (bu çalıştırmada yeni ${yeni}); şüpheli ${supheli.length}`);
for (const s of supheli.sort((a, b) => a.oran - b.oran)) {
  console.log(`- ${s.anahtar} (benzerlik %${Math.round(s.oran * 100)})`);
  console.log(`    metin: ${s.metin}`);
  console.log(`    duyulan: ${s.yazi}`);
}

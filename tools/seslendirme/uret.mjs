// Öğretici seslendirmesini ElevenLabs ile üretir: values-<en|in>/strings_tutorial.xml'deki,
// Türkçe sesi (res/raw/<anahtar>.mp3) olan cümleler → tools/seslendirme/cikti/<dil>/<anahtar>.mp3
//
// DİKKAT: her cümle ElevenLabs kredisi harcar (Multilingual v2: karakter başına 1 kredi).
// Anahtar: ELEVENLABS_API_KEY ortam değişkeni ya da functions/.env.local'daki satır (ana
// checkout'taki de okunur). Anahtar hiçbir yere yazdırılmaz.
//
// Kullanım (depo kökünden):
//   node tools/seslendirme/uret.mjs --dil en --ses <voice_id>             toplamı ve krediyi yazar; >50 cümleyse durur
//   node tools/seslendirme/uret.mjs --dil en --ses <voice_id> --onay      hepsini üretir (var olanı atlar)
//   node tools/seslendirme/uret.mjs --dil id --ses <id> --ornek 5         ilk 5 cümle
//   node tools/seslendirme/uret.mjs --dil id --ses <id> --sadece tutorial1_1,tutorial3_2
//   node tools/seslendirme/uret.mjs --dil en --ses <id> --yeniden tutorial3_2   beğenilmeyeni silip yeniden üretir
// Diğer seçenekler:
//   --model <id>        varsayılan eleven_multilingual_v2
//   --bicim <format>    varsayılan mp3_44100_64 (konuşma için yeterli; Türkçe raw dosyaları 128 kbps mono)
//   --hiz <0.7-1.2>     okuma hızı, varsayılan 0.9 (çocuklar için biraz yavaş)
//   --kararlilik <0-1>  voice_settings.stability, varsayılan 0.5
//   --cikti <klasör>    çıktı klasörü (varsayılan cikti/<dil>; ses örnekleri için)
//   --es <n>            aynı anda istek, varsayılan 3
//   --kuru              hiçbir şey üretmeden yalnızca listeler
import fs from 'node:fs';
import path from 'node:path';
import { argumanlar, cumleler, ciktiKlasoru, ortamDegeri } from './ortak.mjs';

const a = argumanlar();
const dil = a.dil;
const ses = a.ses;
const model = a.model || 'eleven_multilingual_v2';
const bicim = a.bicim || 'mp3_44100_64';
const hiz = Number(a.hiz ?? 0.9);
const kararlilik = Number(a.kararlilik ?? 0.5);
const es = Math.max(1, Math.min(5, Number(a.es ?? 3)));
const ONAYSIZ_SINIR = 50;
const API = 'https://api.elevenlabs.io/v1';

if (!dil || (!ses && !a.kuru)) {
  console.error('Kullanım: node tools/seslendirme/uret.mjs --dil en|id --ses <voice_id> [--ornek N | --sadece a,b | --yeniden a] [--onay]');
  process.exit(1);
}

const { cumleler: hepsi, eksik } = cumleler(dil);
if (eksik.length) console.warn(`Uyarı: Türkçe sesi olup ${dil} çevirisi olmayan ${eksik.length} anahtar atlandı: ${eksik.slice(0, 10).join(', ')}${eksik.length > 10 ? ' …' : ''}`);

const klasor = path.resolve(a.cikti || ciktiKlasoru(dil));
const dosyaYolu = (anahtar) => path.join(klasor, `${anahtar}.mp3`);

let secili = hepsi;
const liste = (s) => String(s).split(',').map((x) => x.trim()).filter(Boolean);
if (a.sadece || a.yeniden) {
  const istenen = liste(a.sadece || a.yeniden);
  const bilinen = new Map(hepsi.map((c) => [c.anahtar, c]));
  const bilinmeyen = istenen.filter((k) => !bilinen.has(k));
  if (bilinmeyen.length) { console.error(`Bilinmeyen ya da sessiz anahtar: ${bilinmeyen.join(', ')}`); process.exit(1); }
  secili = istenen.map((k) => bilinen.get(k));
  if (a.yeniden && !a.kuru) for (const c of secili) fs.rmSync(dosyaYolu(c.anahtar), { force: true });
} else if (a.ornek) {
  secili = hepsi.slice(0, Number(a.ornek));
}

const yapilacak = secili.filter((c) => !fs.existsSync(dosyaYolu(c.anahtar)));
const karakter = yapilacak.reduce((t, c) => t + c.metin.length, 0);
const tumKarakter = hepsi.reduce((t, c) => t + c.metin.length, 0);
console.log(`Dil ${dil}: sesli ${hepsi.length} cümle, toplam ${tumKarakter} karakter.`);
console.log(`Seçilen ${secili.length}, zaten var ${secili.length - yapilacak.length}, üretilecek ${yapilacak.length} cümle / ${karakter} karakter ≈ ${karakter} kredi (${model}).`);
console.log(`Çıktı: ${klasor}  biçim ${bicim}  hız ${hiz}`);

if (a.kuru) {
  for (const c of yapilacak) console.log(`  ${c.anahtar}: ${c.metin}`);
  process.exit(0);
}
if (yapilacak.length > ONAYSIZ_SINIR && !a.onay) {
  console.log(`${ONAYSIZ_SINIR} cümleden fazlası için --onay ekleyin.`);
  process.exit(0);
}
if (!yapilacak.length) process.exit(0);

const anahtar = ortamDegeri('ELEVENLABS_API_KEY');
if (!anahtar) {
  console.error('ELEVENLABS_API_KEY bulunamadı: functions/.env.local dosyasına `ELEVENLABS_API_KEY=...` satırını ekleyin.');
  process.exit(1);
}
fs.mkdirSync(klasor, { recursive: true });

async function kalanKredi() {
  try {
    const r = await fetch(`${API}/user/subscription`, { headers: { 'xi-api-key': anahtar } });
    if (!r.ok) return null;
    const j = await r.json();
    return { kullanilan: j.character_count, sinir: j.character_limit };
  } catch { return null; }
}

// language_code'u desteklemeyen model 400 dönerse bir kez uyarıp onsuz devam edilir.
let dilKodu = ['id', 'ar'].includes(dil) ? dil : 'en';
const bekle = (ms) => new Promise((r) => setTimeout(r, ms));

async function uret(c) {
  for (let deneme = 1; ; deneme++) {
    const govde = {
      text: c.metin,
      model_id: model,
      voice_settings: { stability: kararlilik, similarity_boost: 0.75, style: 0, use_speaker_boost: true, speed: hiz },
    };
    if (dilKodu) govde.language_code = dilKodu;
    const r = await fetch(`${API}/text-to-speech/${encodeURIComponent(ses)}?output_format=${bicim}`, {
      method: 'POST',
      headers: { 'xi-api-key': anahtar, 'Content-Type': 'application/json', Accept: 'audio/mpeg' },
      body: JSON.stringify(govde),
    });
    if (r.ok) {
      const veri = Buffer.from(await r.arrayBuffer());
      const gecici = `${dosyaYolu(c.anahtar)}.part`;
      fs.writeFileSync(gecici, veri);
      fs.renameSync(gecici, dosyaYolu(c.anahtar));
      c.maliyet = Number(r.headers.get("character-cost")) || c.metin.length;
      harcanan += c.maliyet;
      return veri.length;
    }
    const metin = await r.text();
    if (r.status === 400 && dilKodu && /language/i.test(metin)) {
      console.warn(`Uyarı: ${model} language_code kabul etmedi, onsuz devam ediliyor.`);
      dilKodu = null;
      continue;
    }
    // 409 already_running: kütüphane sesi ilk kullanımda hesaba ekleniyor, eşzamanlı istekler bekletilir.
    if ((r.status === 429 || r.status === 409 || r.status >= 500) && deneme < 6) {
      const sn = Number(r.headers.get('retry-after')) || 2 ** deneme;
      console.warn(`  ${c.anahtar}: ${r.status}, ${sn} sn bekleniyor (deneme ${deneme})`);
      await bekle(sn * 1000);
      continue;
    }
    throw new Error(`${c.anahtar}: HTTP ${r.status} ${metin.slice(0, 300)}`);
  }
}

const once = await kalanKredi();
let sira = 0, biten = 0, hata = 0, harcanan = 0;
async function isci() {
  while (sira < yapilacak.length) {
    const c = yapilacak[sira++];
    try {
      const bayt = await uret(c);
      biten++;
      console.log(`[${biten}/${yapilacak.length}] ${c.anahtar} (${c.metin.length} karakter, ${c.maliyet} kredi, ${Math.round(bayt / 1024)} KB)`);
    } catch (e) {
      hata++;
      console.error(`HATA ${e.message}`);
      if (/quota|401|insufficient/i.test(e.message)) { sira = yapilacak.length; }
    }
  }
}
await Promise.all(Array.from({ length: es }, isci));
const sonra = await kalanKredi();
console.log(`Bitti: ${biten} üretildi, ${hata} hata.`);
// Abonelik uç noktası kısıtlı anahtarda kapalı olabilir; o zaman yanıt başlığındaki character-cost toplanır.
if (once && sonra) console.log(`Harcanan kredi: ${sonra.kullanilan - once.kullanilan}; bu ay ${sonra.kullanilan}/${sonra.sinir}.`);
else console.log(`Harcanan kredi (yanıt başlıklarından): ${harcanan}`);
process.exit(hata ? 1 : 0);

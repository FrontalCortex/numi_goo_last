// Ses seçimi yardımcısı: hesaptaki ve ElevenLabs kütüphanesindeki sesleri listeler, kütüphane
// sesini hesaba ekler, adaylar için aynı 3 cümlenin örneğini üretir.
//
// Kullanım (depo kökünden):
//   node tools/seslendirme/sesler.mjs --dil en                    hesap + kütüphane (eğitici/anlatıcı) sesleri; ücretsiz
//   node tools/seslendirme/sesler.mjs --dil id --ara "guru"       kütüphanede arama
//   node tools/seslendirme/sesler.mjs --ekle <public_owner_id>:<voice_id>   kütüphane sesini hesaba ekler (API ile kullanmak için)
//   node tools/seslendirme/sesler.mjs --dil id --ornek <voice_id>[,<voice_id>…]
//        her ses için ORNEK_CUMLELER → ornekler/<dil>/<voice_id>/<anahtar>.mp3 (kredi harcar, ~250-300/ses)
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { argumanlar, BURASI, ortamDegeri } from './ortak.mjs';

const ORNEK_CUMLELER = 'tutorial1_1,tutorial3_2,tutorial3000_5';
const API = 'https://api.elevenlabs.io';
const a = argumanlar();

if (a.ornek) {
  if (!a.dil) { console.error('--dil gerekli'); process.exit(1); }
  for (const ses of String(a.ornek).split(',').map((s) => s.trim()).filter(Boolean)) {
    console.log(`\n== ${ses}`);
    try { execFileSync(process.execPath, [
      path.join(BURASI, 'uret.mjs'), '--dil', a.dil, '--ses', ses, '--sadece', a.cumleler || ORNEK_CUMLELER,
      '--cikti', path.join(BURASI, 'ornekler', a.dil, ses), ...(a.model ? ['--model', a.model] : []), ...(a.hiz ? ['--hiz', a.hiz] : []),
    ], { stdio: 'inherit' }); } catch { console.error(`${ses}: üretim hatalı bitti, sonraki sese geçiliyor`); }
  }
  process.exit(0);
}

const anahtar = ortamDegeri('ELEVENLABS_API_KEY');
if (!anahtar) { console.error('ELEVENLABS_API_KEY bulunamadı: functions/.env.local dosyasına `ELEVENLABS_API_KEY=...` satırını ekleyin.'); process.exit(1); }
const al = async (yol, secenek = {}) => {
  const r = await fetch(`${API}${yol}`, { ...secenek, headers: { 'xi-api-key': anahtar, 'Content-Type': 'application/json', ...secenek.headers } });
  if (!r.ok) throw new Error(`${yol}: HTTP ${r.status} ${(await r.text()).slice(0, 300)}`);
  return r.json();
};

if (a.ekle) {
  const [sahip, ses] = String(a.ekle).split(':');
  const j = await al(`/v1/voices/add/${sahip}/${ses}`, { method: 'POST', body: JSON.stringify({ new_name: a.ad || ses }) });
  console.log(`Eklendi: ${j.voice_id}`);
  process.exit(0);
}

const hesap = await al('/v2/voices?page_size=100');
console.log('# Hesaptaki sesler');
for (const v of hesap.voices) {
  const l = v.labels || {};
  console.log(`${v.voice_id}  ${v.name}  [${v.category}] ${[l.gender, l.age, l.accent, l.descriptive, l.use_case, l.language].filter(Boolean).join(', ')}`);
}

const dil = a.dil || 'en';
const sorgu = new URLSearchParams({ page_size: '40', language: dil, sort: 'cloned_by_count' });
if (a.ara) sorgu.set('search', a.ara);
else if (dil === 'en') sorgu.set('use_cases', 'informative_educational');
const kutuphane = await al(`/v1/shared-voices?${sorgu}`);
console.log(`\n# Kütüphane (${dil}${a.ara ? `, "${a.ara}"` : ''})`);
for (const v of kutuphane.voices) {
  console.log(`${v.public_owner_id}:${v.voice_id}  ${v.name}  ${[v.gender, v.age, v.accent, v.locale, v.descriptive, v.use_case].filter(Boolean).join(', ')}  klon ${v.cloned_by_count}`);
  if (v.description) console.log(`    ${v.description.replace(/\s+/g, ' ').slice(0, 160)}`);
}

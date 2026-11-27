// Üretilen seslerin kaba denetimi (dinlemeden): süre / karakter oranı aykırı olanlar (kelime atlama,
// takılma) ve cümle içindeki uzun sessizlikler. Kulakla dinlemenin yerini tutmaz; nereye kulak
// verileceğini gösterir. Kullanım: node tools/seslendirme/denetle.mjs --dil en
import { execFileSync, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { argumanlar, cumleler, ciktiKlasoru } from './ortak.mjs';

const { dil = 'en' } = argumanlar();
const klasor = ciktiKlasoru(dil);
const satirlar = [];
for (const c of cumleler(dil).cumleler) {
  const dosya = path.join(klasor, `${c.anahtar}.mp3`);
  if (!fs.existsSync(dosya)) { satirlar.push({ ...c, eksik: true }); continue; }
  const sure = Number(execFileSync('ffprobe', ['-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', dosya]).toString().trim());
  const log = spawnSync('ffmpeg', ['-hide_banner', '-i', dosya, '-af', 'silencedetect=noise=-40dB:d=1.2', '-f', 'null', '-'], { encoding: 'utf8' }).stderr;
  const sessizlik = [...log.matchAll(/silence_start: ([\d.]+)[\s\S]*?silence_duration: ([\d.]+)/g)]
    .map((m) => ({ bas: Number(m[1]), sure: Number(m[2]) }))
    .filter((s) => s.bas > 0.3 && s.bas + s.sure < sure - 0.3); // baştaki/sondaki sessizlik doğal
  satirlar.push({ ...c, sure, oran: sure / Math.max(1, c.metin.length), sessizlik });
}
const oranlar = satirlar.filter((s) => s.oran && s.metin.length >= 15).map((s) => s.oran).sort((a, b) => a - b);
const medyan = oranlar[Math.floor(oranlar.length / 2)];
const supheli = satirlar.filter((s) => s.eksik || s.sessizlik?.length
  || (s.metin.length >= 15 && (s.oran < medyan * 0.6 || s.oran > medyan * 1.6)));
console.log(`${dil}: ${satirlar.length} ses, medyan ${(medyan * 1000).toFixed(0)} ms/karakter, şüpheli ${supheli.length}`);
for (const s of supheli) {
  const neden = s.eksik ? 'dosya yok'
    : s.sessizlik?.length ? `cümle içinde ${s.sessizlik.map((x) => x.sure.toFixed(1)).join(', ')} sn sessizlik`
    : s.oran < medyan ? `beklenenden kısa (${s.sure.toFixed(1)} sn)` : `beklenenden uzun (${s.sure.toFixed(1)} sn)`;
  console.log(`  ${s.anahtar}: ${neden} — ${s.metin.slice(0, 90)}`);
}

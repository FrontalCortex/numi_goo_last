// Metni başka anahtardan gelen kayıtlar: öğreticide bazı adımlar bir cümlenin metnini (R.string.X)
// gösterirken başka bir Türkçe kaydı (R.raw.Y) çalıyor — aynı cümlenin ikinci kaydı. Uygulama uzak sesi
// kaydın adıyla arıyor (`tutorial_audio/<dil>_Y.mp3`), üretim ise metin anahtarıyla (X) yapıldı. Metin
// aynı olduğu için yeniden üretmek gerekmiyor: cikti/<dil>/X.mp3 → Y.mp3 kopyalanır (kredi harcanmaz).
//
// Eşleşme TutorialFragment.kt'den: her `soundResource = R.raw.Y`'den önceki en yakın `R.string.X`.
// Y bir metin anahtarıysa (normal durum) dokunulmaz. Bir kayıt farklı metinli adımlarda geçiyorsa ilk
// adımın metni alınır ve uyarı yazılır (Türkçede de aynı kayıt iki adımda çalıyor).
//
// Kullanım: node tools/seslendirme/esle.mjs --dil en   (üretimden sonra, yüklemeden önce)
import fs from 'node:fs';
import path from 'node:path';
import { argumanlar, ciktiKlasoru, KOK, RES } from './ortak.mjs';

const { dil = 'en' } = argumanlar();
const kod = fs.readFileSync(path.join(KOK, 'app', 'src', 'main', 'java', 'com', 'example', 'app', 'TutorialFragment.kt'), 'utf8');
const xml = fs.readFileSync(path.join(RES, 'values', 'strings_tutorial.xml'), 'utf8');
const metinAnahtarlari = new Set([...xml.matchAll(/name="([^"]+)"/g)].map((m) => m[1]));

const esleme = new Map();
let sonMetin = null;
for (const m of kod.matchAll(/R\.string\.(\w+)|soundResource\s*=\s*R\.raw\.(\w+)/g)) {
  if (m[1]) { sonMetin = m[1]; continue; }
  const kayit = m[2];
  if (metinAnahtarlari.has(kayit) || !sonMetin || !metinAnahtarlari.has(sonMetin)) continue;
  if (!esleme.has(kayit)) esleme.set(kayit, []);
  if (!esleme.get(kayit).includes(sonMetin)) esleme.get(kayit).push(sonMetin);
}

const klasor = ciktiKlasoru(dil);
let kopya = 0;
const eksik = [];
for (const [kayit, metinler] of esleme) {
  if (metinler.length > 1) console.log(`uyarı: ${kayit} farklı metinli adımlarda (${metinler.join(', ')}); ${metinler[0]} kullanıldı`);
  const kaynak = path.join(klasor, `${metinler[0]}.mp3`);
  if (!fs.existsSync(kaynak)) { eksik.push(`${kayit} ← ${metinler[0]}`); continue; }
  fs.copyFileSync(kaynak, path.join(klasor, `${kayit}.mp3`));
  kopya++;
}
console.log(`${dil}: ${esleme.size} kayıt metni başka anahtardan; ${kopya} kopyalandı${eksik.length ? `, kaynağı olmayan ${eksik.length}: ${eksik.join('; ')}` : ''}`);

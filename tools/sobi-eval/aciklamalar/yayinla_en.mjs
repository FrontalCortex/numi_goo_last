/**
 * İngilizce hazır açıklamaları sunucunun okuyacağı dosyaya yazar: functions/sobi/aciklamalar_en.json.
 *
 * Metinler aciklamalar_en.json'da (onaylı Türkçe açıklamaların çevirisi, docs/CEVIRI_SOZLUGU.md).
 * Adımlar Türkçe yayından (functions/sobi/aciklamalar_tr.json) geliyor; parmak izi ise İngilizce
 * uygulamanın gönderdiği adım metninden: Türkçe adım metni values/strings_tutorial.xml'de aranır,
 * aynı anahtarın values-en karşılığı alınır. Karşılığı bulunamayan adım dosyaya girmez (o adımda
 * Sobi bugünkü gibi ders metnine dayanır) ve listelenir.
 *
 * Türkçe yayın (yayinla.mjs) değişince bunu da çalıştır.
 * Kullanım: node tools/sobi-eval/aciklamalar/yayinla_en.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..', '..', '..');
const require = createRequire(import.meta.url);
const explanations = require('../../../functions/sobi/explanations');
const STEPS = JSON.parse(readFileSync(join(HERE, '..', 'steps.json'), 'utf8'));
const TR_PUBLISHED = JSON.parse(readFileSync(explanations.fileFor('tr'), 'utf8'));
const EN_TEXTS = JSON.parse(readFileSync(join(HERE, 'aciklamalar_en.json'), 'utf8'));

/** strings_tutorial.xml → { anahtar: metin }; kaçışlar çözülmüş (\' → '). */
function strings(dir) {
  const xml = readFileSync(join(ROOT, 'app', 'src', 'main', 'res', dir, 'strings_tutorial.xml'), 'utf8');
  const out = {};
  for (const m of xml.matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) {
    out[m[1]] = m[2].replace(/^"|"$/g, '').replace(/\\(['"])/g, '$1').replace(/&gt;/g, '>').replace(/&lt;/g, '<').replace(/&amp;/g, '&');
  }
  return out;
}
const tr = strings('values');
const en = strings('values-en');
// Türkçe metnin parmak izi → İngilizce metin (aynı cümle tek anahtar; parmak izi noktalamayı saymıyor).
const enByTrPrint = new Map();
for (const [k, t] of Object.entries(tr)) if (en[k]) enByTrPrint.set(explanations.textPrint(t), en[k]);

const out = {};
const missing = [];
for (const [tutorial, parts] of Object.entries(TR_PUBLISHED)) {
  for (const p of parts) {
    const text = EN_TEXTS[p.key];
    if (!text) { missing.push(`${p.key} (çeviri yok)`); continue; }
    const steps = [];
    for (const [index] of p.steps) {
      const step = STEPS.find((s) => s.tutorial === Number(tutorial) && s.index === index);
      const enText = step && enByTrPrint.get(explanations.textPrint(step.stepText));
      if (enText) steps.push([index, explanations.textPrint(enText)]);
      else missing.push(`${p.key} adım ${index}`);
    }
    if (steps.length) (out[tutorial] ||= []).push({ key: p.key, steps, konu: text.konu, basit: text.basit, neden: text.neden ?? null });
  }
}
writeFileSync(explanations.fileFor('en'), `${JSON.stringify(out, null, 1)}\n`);
const n = Object.values(out).reduce((s, l) => s + l.length, 0);
console.log(`${n} İngilizce açıklama → ${explanations.fileFor('en')}`);
if (missing.length) console.log(`Eşleşmeyen: ${missing.join(', ')}`);

/**
 * Çeviri hazır açıklamaları sunucunun okuyacağı dosyaya yazar: functions/sobi/aciklamalar_<dil>.json.
 *
 * Metinler bu klasördeki aciklamalar_<dil>.json'da (onaylı Türkçe açıklamaların çevirisi;
 * docs/CEVIRI_SOZLUGU.md, docs/CEVIRI_SOZLUGU_ID.md). Adımlar Türkçe yayından
 * (functions/sobi/aciklamalar_tr.json) geliyor; parmak izi ise o dildeki uygulamanın gönderdiği adım
 * metninden: Türkçe adım metni values/strings_tutorial.xml'de aranır, aynı anahtarın o dildeki
 * karşılığı (values-en, values-in) alınır. Karşılığı bulunamayan adım dosyaya girmez (o adımda Sobi
 * bugünkü gibi ders metnine dayanır) ve listelenir.
 *
 * Dilin strings_tutorial.xml'i yoksa hiçbir şey yazmaz: parmak izi olmadan bütün adımlar
 * eşleşmezdi ve yayındaki dosya boşalırdı.
 *
 * Türkçe yayın (yayinla.mjs) ya da o dilin öğretici çevirisi değişince bunu da çalıştır.
 * Kullanım: node tools/sobi-eval/aciklamalar/yayinla_dil.mjs <en|id>
 */
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

/** Sunucunun dil kodu → Android kaynak klasörü (Endonezce eski kodla: values-in). */
const RES_DIRS = { en: 'values-en', id: 'values-in' };

const lang = process.argv[2];
if (!RES_DIRS[lang]) {
  console.error(`Dil ver: ${Object.keys(RES_DIRS).join(' | ')}`);
  process.exit(1);
}

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..', '..', '..');
const require = createRequire(import.meta.url);
const explanations = require('../../../functions/sobi/explanations');
const STEPS = JSON.parse(readFileSync(join(HERE, '..', 'steps.json'), 'utf8'));
const TR_PUBLISHED = JSON.parse(readFileSync(explanations.fileFor('tr'), 'utf8'));
const TEXTS = JSON.parse(readFileSync(join(HERE, `aciklamalar_${lang}.json`), 'utf8'));

const xmlPath = (dir) => join(ROOT, 'app', 'src', 'main', 'res', dir, 'strings_tutorial.xml');
if (!existsSync(xmlPath(RES_DIRS[lang]))) {
  console.error(`${xmlPath(RES_DIRS[lang])} yok: öğretici çevirisi gelmeden yayınlanmaz.`);
  process.exit(1);
}

/** strings_tutorial.xml → { anahtar: metin }; kaçışlar çözülmüş (\' → '). */
function strings(dir) {
  const xml = readFileSync(xmlPath(dir), 'utf8');
  const out = {};
  for (const m of xml.matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) {
    out[m[1]] = m[2].replace(/^"|"$/g, '').replace(/\\(['"])/g, '$1').replace(/&gt;/g, '>').replace(/&lt;/g, '<').replace(/&amp;/g, '&');
  }
  return out;
}
const tr = strings('values');
const target = strings(RES_DIRS[lang]);
// Türkçe metnin parmak izi → o dildeki metin (aynı cümle tek anahtar; parmak izi noktalamayı saymıyor).
const byTrPrint = new Map();
for (const [k, t] of Object.entries(tr)) if (target[k]) byTrPrint.set(explanations.textPrint(t), target[k]);

const out = {};
const missing = [];
for (const [tutorial, parts] of Object.entries(TR_PUBLISHED)) {
  for (const p of parts) {
    const text = TEXTS[p.key];
    if (!text) { missing.push(`${p.key} (çeviri yok)`); continue; }
    const steps = [];
    for (const [index] of p.steps) {
      const step = STEPS.find((s) => s.tutorial === Number(tutorial) && s.index === index);
      const translated = step && byTrPrint.get(explanations.textPrint(step.stepText));
      if (translated) steps.push([index, explanations.textPrint(translated)]);
      else missing.push(`${p.key} adım ${index}`);
    }
    if (steps.length) (out[tutorial] ||= []).push({ key: p.key, steps, konu: text.konu, basit: text.basit, neden: text.neden ?? null });
  }
}
writeFileSync(explanations.fileFor(lang), `${JSON.stringify(out, null, 1)}\n`);
const n = Object.values(out).reduce((s, l) => s + l.length, 0);
console.log(`${n} açıklama (${lang}) → ${explanations.fileFor(lang)}`);
if (missing.length) console.log(`Eşleşmeyen: ${missing.join(', ')}`);

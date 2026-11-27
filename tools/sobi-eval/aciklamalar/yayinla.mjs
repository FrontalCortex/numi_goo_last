/**
 * Onaylı açıklamaları sunucunun okuyacağı dosyaya yazar: functions/sobi/aciklamalar_tr.json.
 * Yalnızca `onay: true` ve parça metni değişmemiş (hash tutuyor) açıklamalar girer; diğerlerinde
 * Sobi bugünkü gibi ders metnine dayanır.
 *
 * Kullanım: node tools/sobi-eval/aciklamalar/yayinla.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const explanations = require('../../../functions/sobi/explanations');
const PARTS = JSON.parse(readFileSync(join(HERE, 'parcalar.json'), 'utf8'));
const ACK = JSON.parse(readFileSync(join(HERE, 'aciklamalar_tr.json'), 'utf8'));

const out = {};
const skipped = [];
for (const p of PARTS) {
  const a = ACK[p.key];
  if (!a || !a.onay) continue;
  if (a.hash !== p.hash) { skipped.push(`${p.key} (ders metni değişmiş)`); continue; }
  (out[p.tutorial] ||= []).push({
    key: p.key,
    // Yalnızca anlatım adımları: soru adımında hamleyi kural motoru veriyor.
    steps: p.steps.filter((s) => s.kind === 'anlatim').map((s) => [s.index, explanations.textPrint(s.text)]),
    konu: a.konu,
    basit: a.basit,
    neden: a.neden ?? null,
  });
}
writeFileSync(explanations.FILE, `${JSON.stringify(out, null, 1)}\n`);
const n = Object.values(out).reduce((s, l) => s + l.length, 0);
console.log(`${n} onaylı açıklama (${Object.keys(out).map((t) => `t${t}`).join(', ') || '—'}) → ${explanations.FILE}`);
if (skipped.length) console.log(`Atlanan: ${skipped.join(', ')}`);

/**
 * ANLATIM PARÇALARI — hazır açıklamaların yazılacağı birimler (HAZIR_ACIKLAMALAR_PLANI.md).
 *
 * Bir parça, öğreticide aynı işlemi (ekrandaki soru) anlatan ardışık adımlar; işlemsiz adımlar
 * (kavram anlatımı) kendi parçasını oluşturur. "Cevap 6." gibi işlemsiz kapanış adımları önceki
 * işlemin parçasına katılır: çocuk orada "anlamadım" derse o işlemi soruyor.
 *
 * Her parçanın `hash`'i adım metinlerinden: ders metni değişince açıklama eskimiş sayılır ve
 * `denetle.mjs` bunu söyler (yeni öğretici eklenince de eksik parçaları listeler).
 *
 * Kullanım: node tools/sobi-eval/aciklamalar/parcalar.mjs  → parcalar.json
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { createRequire } from 'node:module';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const explainer = require('../../../functions/sobi/stepExplainer');
const tools = require('../../../functions/sobi/tools');
const STEPS = JSON.parse(readFileSync(join(HERE, '..', 'steps.json'), 'utf8'));

const CLOSING = /^(ve\s+)?(cevap|sonuç)\b/i;
const OP = /^\s*(\d+)\s*([+\-−x×*])\s*(\d+)\s*$/;

function opInfo(q) {
  const m = (q || '').match(OP);
  if (!m) return null;
  const a = Number(m[1]); const b = Number(m[3]);
  if ('x×*'.includes(m[2])) return { islem: `${a} x ${b}`, carpma: true };
  const e = explainer.explain(a, m[2] === '+' ? 'add' : 'sub', b);
  if (!e) return null;
  const d = tools.describe(e);
  return {
    islem: d.islem,
    sonuc: d.sonuc,
    hamleler: d.hamleler.map((h) => ({ basamak: h.basamak, kural: h.kural, adimlar: h.adimlar, neden: h.neden, neden_dogrudan_olmuyor: h.neden_dogrudan_olmuyor })),
  };
}

const parts = [];
for (const t of [...new Set(STEPS.map((s) => s.tutorial))]) {
  const list = STEPS.filter((s) => s.tutorial === t);
  let cur = null;
  for (const s of list) {
    const q = s.questionText || '';
    const joinPrev = cur && (q === cur.questionText || (!q && cur.questionText && CLOSING.test(s.stepText)));
    if (!joinPrev) {
      cur = { tutorial: t, no: (parts.filter((p) => p.tutorial === t).length + 1), lessonTitle: s.lessonTitle, taughtRules: s.taughtRules, questionText: q, steps: [] };
      parts.push(cur);
    }
    cur.steps.push({ index: s.index, kind: s.kind, text: s.stepText });
  }
}
for (const p of parts) {
  p.key = `t${p.tutorial}_p${p.no}`;
  p.op = opInfo(p.questionText);
  p.hash = createHash('sha256').update(p.steps.map((s) => s.text).join('\n')).digest('hex').slice(0, 12);
}
writeFileSync(join(HERE, 'parcalar.json'), `${JSON.stringify(parts, null, 1)}\n`);
const anl = parts.filter((p) => p.steps.some((s) => s.kind === 'anlatim'));
console.log(`${parts.length} parça (${anl.length}'inde anlatım adımı var), ${new Set(parts.map((p) => p.tutorial)).size} öğretici`);

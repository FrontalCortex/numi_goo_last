/**
 * İKİ SÜRÜMÜN KARŞILAŞTIRMASI — aynı örneklerde (ortak prompt_id'ler) baseline ile vN.
 * Ölçüt başına oranlar, fark ve eşleştirilmiş %95 aralığı; kategori kırılımı; kötüleşen ve
 * düzelen örnekler (okumak için); ölçülmüş maliyet.
 *
 * Kullanım: node tools/sobi-eval/kiyas.mjs [--flow tools/sobi-eval/sonuclar2] [--a baseline] [--b v1]
 *   → <flow>/KIYAS_<a>_<b>.md
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i === -1 ? d : process.argv[i + 1]; };
const flow = arg('flow', 'tools/sobi-eval/sonuclar2');
const A = arg('a', 'baseline');
const B = arg('b', 'v1');
const state = JSON.parse(readFileSync(join(flow, '_state.json'), 'utf8'));
const read = (v) => new Map(readFileSync(join(flow, v, 'results.jsonl'), 'utf8').split('\n').filter(Boolean)
  .map((l) => JSON.parse(l)).filter((r) => r.status === 'ok').map((r) => [r.prompt_id, r]));
const ra = read(A);
const rb = read(B);
const ids = [...ra.keys()].filter((id) => rb.has(id)).sort();

function cost(model, u) {
  const p = state.prices[model];
  if (!u || !model) return 0;
  return ((u.input_tokens || 0) * p.in + (u.output_tokens || 0) * p.out
    + (u.cache_creation_input_tokens || 0) * p.in * 1.25 + (u.cache_read_input_tokens || 0) * p.in * 0.1) / 1e6;
}
const sum = (a) => a.reduce((s, x) => s + x, 0);
const pct = (x) => `${Math.round(x * 100)}%`;

/** Eşleştirilmiş fark: iki tarafta da sayı olan örneklerde. */
function paired(m, list = ids) {
  const pairs = list.map((id) => [ra.get(id).grade?.[m], rb.get(id).grade?.[m]]).filter(([x, y]) => typeof x === 'number' && typeof y === 'number');
  if (!pairs.length) return null;
  const d = pairs.map(([x, y]) => y - x);
  const mean = sum(d) / d.length;
  const sd = Math.sqrt(sum(d.map((x) => (x - mean) ** 2)) / Math.max(1, d.length - 1));
  return { a: sum(pairs.map((p) => p[0])) / pairs.length, b: sum(pairs.map((p) => p[1])) / pairs.length, d: mean, ci: 1.96 * sd / Math.sqrt(d.length), n: pairs.length };
}
const fmt = (r) => (r ? `${pct(r.a)} | ${pct(r.b)} | ${r.d >= 0 ? '+' : ''}${Math.round(r.d * 100)} ± ${Math.round(r.ci * 100)} | ${r.n}` : '— | — | — | 0');

const label = Object.fromEntries(state.metrics.map((m) => [m.id, m.label || m.id]));
const cats = [...new Set(ids.map((id) => ra.get(id).tags[0]))];
const flips = (from, to) => ids.filter((id) => ra.get(id).grade.basari === from && rb.get(id).grade.basari === to);
const fails = (r) => state.metrics.map((m) => m.id).filter((m) => r.grade[m] === 0 && !['basari', 'yanlis_alarm'].includes(m)).map((m) => label[m]);

function replies(v, id) {
  const p = join(flow, v, 'traces', `${id}_rep0.json`);
  if (!existsSync(p)) return '';
  return JSON.parse(readFileSync(p, 'utf8')).filter((t) => t.role === 'assistant').map((t) => t.content.replace(/\n+/g, ' ')).join(' ‖ ');
}

const out = [
  `# ${A} → ${B} (aynı ${ids.length} örnek)`, '',
  '| ölçüt | ' + A + ' | ' + B + ' | fark (puan, ± %95) | n |', '|---|---|---|---|---|',
  ...state.metrics.map((m) => `| ${label[m.id]} | ${fmt(paired(m.id))} |`), '',
  '## Kategori (başarı)', '',
  `| kategori | ${A} | ${B} | fark | n |`, '|---|---|---|---|---|',
  ...cats.map((k) => `| ${k} | ${fmt(paired('basari', ids.filter((id) => ra.get(id).tags[0] === k)))} |`), '',
  '## Adım türü (başarı)', '',
  `| tür | ${A} | ${B} | fark | n |`, '|---|---|---|---|---|',
  ...['anlatim', 'soru'].map((k) => `| ${k} | ${fmt(paired('basari', ids.filter((id) => ra.get(id).tags[2] === k)))} |`), '',
  '## Maliyet (ölçülmüş)', '',
  `| | ${A} Sobi | ${A} hakem | ${B} Sobi | ${B} hakem |`, '|---|---|---|---|---|',
  `| toplam | $${sum(ids.map((id) => cost(ra.get(id).model, ra.get(id).usage))).toFixed(3)} | $${sum(ids.map((id) => cost(ra.get(id).judge_model, ra.get(id).judge_usage))).toFixed(3)} | $${sum(ids.map((id) => cost(rb.get(id).model, rb.get(id).usage))).toFixed(3)} | $${sum(ids.map((id) => cost(rb.get(id).judge_model, rb.get(id).judge_usage))).toFixed(3)} |`,
  '', `(${A} Sobi maliyeti kayıttan puanlanan örneklerde 0: o cevaplar önceki çalıştırmada üretildi.)`, '',
  `## Kötüleşenler (${A} başarılı, ${B} değil): ${flips(1, 0).length}`, '',
  ...flips(1, 0).flatMap((id) => [`### ${id}`, `- Çocuk: ${rb.get(id).prompt}`, `- ${A}: ${replies(A, id).slice(0, 400)}`, `- ${B}: ${replies(B, id).slice(0, 400)}`, `- ${B} kaldı: ${fails(rb.get(id)).join(', ')}; ${rb.get(id).explanation?.hata_turu || ''}`, '']),
  `## Düzelenler (${A} kaldı, ${B} başarılı): ${flips(0, 1).length}`, '',
  ...flips(0, 1).map((id) => `- ${id}: «${rb.get(id).prompt}» — ${A} kaldı: ${fails(ra.get(id)).join(', ')}`), '',
];
const file = join(flow, `KIYAS_${A}_${B}.md`);
writeFileSync(file, `${out.join('\n')}\n`);
const b = paired('basari');
console.log(`Başarı ${pct(b.a)} → ${pct(b.b)} (${b.d >= 0 ? '+' : ''}${Math.round(b.d * 100)} ± ${Math.round(b.ci * 100)} puan, n=${b.n}); kötüleşen ${flips(1, 0).length}, düzelen ${flips(0, 1).length} → ${file}`);

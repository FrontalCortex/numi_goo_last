/**
 * SONUÇ ÖZETİ — results.jsonl satırlarından (ham puanlardan yeniden hesaplanır, toplu alana güvenilmez):
 * genel puanlar (%95 güven aralığıyla), kategori / öğretici / adım türü kırılımları, hakemin hata
 * türleri, ölçülmüş maliyet (Sobi ve hakem ayrı) ve okunabilir rastgele örneklem.
 *
 * Maliyet: her satırın kendi `model` × `usage` değeri (ve hakem için `judge_model` × `judge_usage`);
 * önbelleğe yazma girdi fiyatının 1,25 katı, önbellekten okuma 0,1 katı. Fiyatlar _state.json'da.
 *
 * Kullanım: node tools/sobi-eval/rapor.mjs [--flow tools/sobi-eval/sonuclar] [--variant baseline] [--ornek 25]
 *   → <flow>/<variant>/RAPOR.md
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i === -1 ? d : process.argv[i + 1]; };
const flow = arg('flow', 'tools/sobi-eval/sonuclar');
const variant = arg('variant', 'baseline');
const nSample = Number(arg('ornek', 25));
const vdir = join(flow, variant);
const state = JSON.parse(readFileSync(join(flow, '_state.json'), 'utf8'));
const readJsonl = (p) => (existsSync(p) ? readFileSync(p, 'utf8').split('\n').filter(Boolean).map((l) => JSON.parse(l)) : []);
const rows = readJsonl(join(vdir, 'results.jsonl'));
const errors = readJsonl(join(vdir, 'errors.jsonl'));
const cases = new Map(JSON.parse(readFileSync('tools/sobi-eval/cases.json', 'utf8')).map((c) => [c.id, c]));

// ── Maliyet ──
function cost(model, u) {
  const p = state.prices[model];
  if (!u) return 0;
  if (!p) throw new Error(`fiyat yok: ${model}`);
  return ((u.input_tokens || 0) * p.in + (u.output_tokens || 0) * p.out
    + (u.cache_creation_input_tokens || 0) * p.in * 1.25 + (u.cache_read_input_tokens || 0) * p.in * 0.1) / 1e6;
}
const sobiCost = rows.map((r) => cost(r.model, r.usage));
const judgeCost = rows.map((r) => (r.judge_model ? cost(r.judge_model, r.judge_usage) : 0));
const errCost = errors.reduce((s, e) => s + (e.model ? cost(e.model, e.usage) : 0) + (e.judge_model ? cost(e.judge_model, e.judge_usage) : 0), 0);
const sum = (a) => a.reduce((s, x) => s + x, 0);
const q = (a, f) => { const s = [...a].sort((x, y) => x - y); return s[Math.min(s.length - 1, Math.floor(f * s.length))]; };
const usd = (x) => `$${x.toFixed(x < 0.01 ? 5 : 3)}`;

// ── Puanlar ──
const ok = rows.filter((r) => r.status === 'ok');
const metricIds = state.metrics.map((m) => m.id);
const label = Object.fromEntries(state.metrics.map((m) => [m.id, m.label || m.id]));
function rate(list, m) {
  const v = list.map((r) => r.grade?.[m]).filter((x) => typeof x === 'number');
  if (!v.length) return null;
  const p = sum(v) / v.length;
  return { p, n: v.length, ci: 1.96 * Math.sqrt((p * (1 - p)) / v.length) };
}
const pct = (r) => (r ? `${Math.round(r.p * 100)}% (n=${r.n})` : '—');
const pctCi = (r) => (r ? `${Math.round(r.p * 100)}% ± ${Math.round(r.ci * 100)} (n=${r.n})` : '—');

function groupTable(keyFn, title, metrics = ['basari', 'soruya_cevap', 'matematik', 'ders_tutarliligi', 'cevabi_vermedi', 'yedek_yok']) {
  const g = new Map();
  for (const r of ok) { const k = keyFn(r); if (!g.has(k)) g.set(k, []); g.get(k).push(r); }
  const keys = [...g.keys()].sort((a, b) => rate(g.get(a), 'basari').p - rate(g.get(b), 'basari').p);
  return [
    `### ${title}`, '',
    `| ${title.toLowerCase()} | ${metrics.map((m) => label[m]).join(' | ')} |`,
    `|---|${metrics.map(() => '---').join('|')}|`,
    ...keys.map((k) => `| ${k} | ${metrics.map((m) => pct(rate(g.get(k), m))).join(' | ')} |`),
    '',
  ];
}

// Hakemin hata türleri: virgülle ayrılmış kısa adlar.
const hataCount = new Map();
for (const r of ok) {
  const h = r.explanation?.hata_turu;
  if (!h) continue;
  for (const t of h.split(',').map((x) => x.trim().toLocaleLowerCase('tr-TR')).filter(Boolean)) hataCount.set(t, (hataCount.get(t) || 0) + 1);
}
// Denetimin geri çevirme sebepleri
const reasonCount = new Map();
for (const r of ok) for (const t of (r.explanation?.yedek_yok || '').split(/,\s+/).map((x) => x.trim().replace(/:.*/, '')).filter(Boolean)) reasonCount.set(t, (reasonCount.get(t) || 0) + 1);

// ── Okunabilir örneklem (tohumlu) ──
let seed = 7;
const rnd = () => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 2147483648; };
const sample = [...ok].map((r) => [rnd(), r]).sort((a, b) => a[0] - b[0]).slice(0, nSample).map((x) => x[1]).sort((a, b) => a.prompt_id.localeCompare(b.prompt_id));
function traceReplies(r) {
  const p = join(vdir, 'traces', `${r.prompt_id}_rep${r.rep}.json`);
  if (!existsSync(p)) return [];
  return JSON.parse(readFileSync(p, 'utf8')).filter((t) => t.role === 'assistant').map((t) => t.content);
}
function ctxSummary(c) {
  const x = c.contexts[0];
  return x.target !== undefined
    ? `soru adımı "${x.stepText}" (${x.questionText || ''}; abaküs ${x.base} → ${x.target}, şu an ${x.current}, ${x.durum})`
    : `anlatım adımı "${x.stepText}"`;
}

const ciBasari = rate(ok, 'basari');
const out = [
  `# Sobi değerlendirmesi — ${variant}`, '',
  `${rows.length} örnek puanlandı (${ok.length} ok, ${rows.length - ok.length} kesik), ${errors.length} hatalı deneme (errors.jsonl). Sobi: ${[...new Set(rows.map((r) => r.model))].join(', ')}; hakem: ${[...new Set(rows.map((r) => r.judge_model).filter(Boolean))].join(', ')}.`, '',
  `**Başarı: ${pctCi(ciBasari)}** (±: %95 güven aralığının yarı genişliği).`, '',
  '## Maliyet (ölçülmüş)', '',
  '| | toplam | örnek başına medyan | en az – en çok |', '|---|---|---|---|',
  `| Sobi | ${usd(sum(sobiCost))} | ${usd(q(sobiCost, 0.5))} | ${usd(Math.min(...sobiCost))} – ${usd(Math.max(...sobiCost))} |`,
  `| Hakem | ${usd(sum(judgeCost))} | ${usd(q(judgeCost, 0.5))} | ${usd(Math.min(...judgeCost))} – ${usd(Math.max(...judgeCost))} |`,
  `| Hatalı denemeler | ${usd(errCost)} | | |`,
  `| **Toplam** | **${usd(sum(sobiCost) + sum(judgeCost) + errCost)}** | | |`, '',
  `Jeton (örnek başına medyan): Sobi girdi ${q(rows.map((r) => r.usage.input_tokens), 0.5)}, önbellekten ${q(rows.map((r) => r.usage.cache_read_input_tokens), 0.5)}, önbelleğe yazma ${q(rows.map((r) => r.usage.cache_creation_input_tokens), 0.5)}, çıktı ${q(rows.map((r) => r.usage.output_tokens), 0.5)}; hakem girdi ${q(rows.map((r) => r.judge_usage?.input_tokens || 0), 0.5)}, önbellekten ${q(rows.map((r) => r.judge_usage?.cache_read_input_tokens || 0), 0.5)}, çıktı ${q(rows.map((r) => r.judge_usage?.output_tokens || 0), 0.5)}. Sobi süresi medyan ${q(rows.map((r) => r.latency_s), 0.5).toFixed(1)} sn.`, '',
  '## Genel puanlar', '',
  '| ölçüt | oran |', '|---|---|',
  ...metricIds.map((m) => `| ${label[m]} (\`${m}\`) | ${pctCi(rate(ok, m))} |`), '',
  '## Kırılımlar', '',
  ...groupTable((r) => r.tags[0], 'Kategori'),
  ...groupTable((r) => r.tags[2], 'Adım türü'),
  ...groupTable((r) => r.tags[1], 'Öğretici', ['basari', 'matematik', 'ders_tutarliligi', 'ogretilmemis_yok', 'yedek_yok']),
  '## Hata türleri (hakem)', '',
  '| hata | örnek |', '|---|---|',
  ...[...hataCount].sort((a, b) => b[1] - a[1]).map(([k, v]) => `| ${k} | ${v} |`), '',
  '## Denetimin (guard) geri çevirme sebepleri', '',
  '| sebep | örnek |', '|---|---|',
  ...[...reasonCount].sort((a, b) => b[1] - a[1]).map(([k, v]) => `| ${k} | ${v} |`), '',
  `## Rastgele ${sample.length} örnek`, '',
];
for (const r of sample) {
  const c = cases.get(r.prompt_id);
  const fails = metricIds.filter((m) => r.grade[m] === 0 && m !== 'yanlis_alarm');
  out.push(`### ${r.prompt_id} — ${r.grade.basari ? 'BAŞARILI' : `kaldı: ${fails.map((m) => label[m]).join(', ')}`}`, '');
  out.push(`- Ders: ${c.contexts[0].lessonTitle || '—'}; ${ctxSummary(c)}`);
  out.push(`- Beklenen: ${c.expected}`);
  const replies = traceReplies(r);
  c.turns.forEach((t, i) => {
    out.push(`- Çocuk: «${t.text}»`);
    out.push(`- Sobi: ${(replies[i] || '').replace(/\n+/g, ' ')}`);
  });
  if (r.tools_used?.length) out.push(`- Araçlar: ${r.tools_used.join(', ')}`);
  const ex = Object.entries(r.explanation || {}).filter(([k]) => fails.includes(k) || k === 'hata_turu');
  for (const [k, v] of ex) out.push(`- ${label[k] || k}: ${v}`);
  out.push('');
}
writeFileSync(join(vdir, 'RAPOR.md'), `${out.join('\n')}\n`);
console.log(`Başarı ${pctCi(ciBasari)}; maliyet Sobi ${usd(sum(sobiCost))} + hakem ${usd(sum(judgeCost))} + hata ${usd(errCost)} → ${join(vdir, 'RAPOR.md')}`);

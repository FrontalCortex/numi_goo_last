/**
 * KÖR YAN YANA KARŞILAŞTIRMA — iki sürümün aynı örnekteki cevaplarını A/B diye karışık sırayla
 * yazar; kullanıcı hangisinin iyi olduğunu seçer. Anahtar (hangi harf hangi sürüm) ayrı dosyada.
 * Çocuğun gerçekten gördüğü metin gösterilir (cevap denetime takıldıysa yedek metin).
 * Ücretsiz: yalnızca kayıtlı sonuçları okur.
 *
 * Kullanım: node tools/sobi-eval/yan-yana.mjs [--flow sonuclar2] [--ad YAN_YANA] [--a baseline] [--b v2] [--n 20]
 *   → etiket/YAN_YANA.md (doldurulacak), etiket/anahtar.json (açmayın)
 *   Doldurduktan sonra: node tools/sobi-eval/yan-yana.mjs --say
 */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i === -1 ? d : process.argv[i + 1]; };
const FLOW = join(HERE, arg('flow', 'sonuclar2'));
const NAME = arg('ad', 'YAN_YANA');
const OUT = join(HERE, 'etiket', `${NAME}.md`);
const KEY = join(HERE, 'etiket', `${NAME}_anahtar.json`);

if (process.argv.includes('--say')) {
  const key = JSON.parse(readFileSync(KEY, 'utf8'));
  const text = readFileSync(OUT, 'utf8');
  const counts = { [key.a]: 0, [key.b]: 0, ayni: 0, bos: 0 };
  const rows = [];
  for (const item of key.items) {
    const m = text.match(new RegExp(`## ${item.no}\.[\s\S]*?Seçimin:\s*([^\n]*)`));
    const v = (m ? m[1] : '').trim().toUpperCase();
    const pick = v.startsWith('A') ? item.A : v.startsWith('B') ? item.B : /AYNI|=/.test(v) ? 'ayni' : 'bos';
    counts[pick]++;
    rows.push(`${item.no}. ${item.id}: ${pick} (hakem: ${key.a} ${item.hakem[key.a]}, ${key.b} ${item.hakem[key.b]})`);
  }
  console.log(rows.join('\n'));
  console.log(`\nSonuç: ${key.a} ${counts[key.a]}, ${key.b} ${counts[key.b]}, aynı ${counts.ayni}, boş ${counts.bos}`);
  process.exit(0);
}

const A = arg('a', 'baseline');
const B = arg('b', 'v2');
const N = Number(arg('n', 20));
const read = (v) => new Map(readFileSync(join(FLOW, v, 'results.jsonl'), 'utf8').split('\n').filter(Boolean).map((l) => JSON.parse(l)).map((r) => [r.prompt_id, r]));
const ra = read(A);
const rb = read(B);
const cases = new Map(JSON.parse(readFileSync(join(HERE, 'cases.json'), 'utf8')).map((c) => [c.id, c]));
const ids = [...rb.keys()].filter((id) => ra.has(id)).sort();

let seed = 31;
const rnd = () => { seed = (seed * 1103515245 + 12345) % 2147483648; return seed / 2147483648; };
const shuffle = (a) => a.map((x) => [rnd(), x]).sort((p, q) => p[0] - q[0]).map((x) => x[1]);
// Hakemin ayrıştığı örnekler kararın asıl yeri; geri kalanı rastgele (hakemin aynı dediklerinde de
// kullanıcı farklı düşünebilir).
const differ = shuffle(ids.filter((id) => ra.get(id).grade.basari !== rb.get(id).grade.basari));
const same = shuffle(ids.filter((id) => ra.get(id).grade.basari === rb.get(id).grade.basari));
const nd = Math.min(differ.length, Math.round(N * 0.6));
const pick = shuffle([...differ.slice(0, nd), ...same.slice(0, N - nd)]);

/** Çocuğun gördüğü metinler, tur tur (trace'teki asistan girdileri). */
function shown(v, id) {
  const p = join(FLOW, v, 'traces', `${id}_rep0.json`);
  if (!existsSync(p)) throw new Error(`kayıt yok: ${p}`);
  return JSON.parse(readFileSync(p, 'utf8')).filter((t) => t.role === 'assistant').map((t) => {
    const m = t.content.match(/\[ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN\]\n([\s\S]*)$/);
    return (m ? m[1] : t.content).trim();
  });
}

const lines = [
  '# Kör yan yana karşılaştırma',
  '',
  `${pick.length} örnek, hepsi anlatım adımı. Her örnekte aynı soruya iki Sobi cevabı var: A ve B. Hangisinin`,
  `eski, hangisinin yeni sürüm olduğu karışık; anahtar ayrı dosyada ("${NAME}_anahtar.json", açmayın).`,
  '',
  'Her örneğin altındaki "Seçimin:" satırına **A**, **B** ya da **aynı** yaz. İstersen yanına kısa bir not',
  'ekle (ör. "B: kardeşi yanlış söylüyor"). Çocuğun ekranda gerçekten gördüğü metin gösteriliyor.',
  '',
];
const key = { a: A, b: B, items: [] };
pick.forEach((id, i) => {
  const c = cases.get(id);
  const x = c.contexts[0];
  const flip = rnd() < 0.5;
  const [va, vb] = flip ? [B, A] : [A, B];
  const sa = shown(va, id);
  const sb = shown(vb, id);
  const recent = (x.recentSteps || []).slice(-3);
  lines.push(`## ${i + 1}. ${x.lessonTitle || 'Ders'}`, '');
  if (recent.length) lines.push(`Az önce derste: ${recent.map((s) => `«${s}»`).join(' ')}`, '');
  lines.push(`Şu anki adım: «${x.stepText}»${x.questionText ? ` (ekranda: ${x.questionText})` : ''}`, '');
  c.turns.forEach((t, k) => {
    lines.push(`**Çocuk:** ${t.text}`, '');
    lines.push(`- **A:** ${(sa[k] || '').replace(/\n+/g, ' ')}`);
    lines.push(`- **B:** ${(sb[k] || '').replace(/\n+/g, ' ')}`, '');
  });
  lines.push('Seçimin: ', '');
  key.items.push({ no: i + 1, id, A: va, B: vb, hakem: { [A]: ra.get(id).grade.basari, [B]: rb.get(id).grade.basari } });
});
writeFileSync(OUT, `${lines.join('\n')}\n`);
writeFileSync(KEY, `${JSON.stringify(key, null, 1)}\n`);
console.log(`${pick.length} örnek (${nd} hakemin ayrıştığı) → ${OUT}`);

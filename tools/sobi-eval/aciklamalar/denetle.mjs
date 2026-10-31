/**
 * HAZIR AÇIKLAMALARIN DENETİMİ (ücretsiz, model çağrısı yok).
 *
 * 1. Kapsam: hangi parçanın açıklaması yok, hangisi eskimiş (ders metni değişmiş: hash tutmuyor).
 *    Yeni öğretici ya da bölüm eklenince önce parcalar.mjs, sonra bu betik: eksikler listelenir,
 *    yalnızca onlar yazılır. Açıklaması olmayan parçada Sobi bugünkü gibi ders metnine dayanır.
 * 2. İçerik: her sayı parçanın (ve komşu parçaların) ders metninde ya da kural motorunun hamlelerinde
 *    geçmeli; "N gelir / N gider" hamleleri kaynakta aynı yönle geçmeli; yön sözü, "5 boncuk",
 *    "yanlış", soru işareti yok; en çok 4 cümle; çocuğun henüz öğrenmediği bir kuralın adı yok.
 * 3. Onay dosyası: öğretici başına okunabilir sayfa (onay/<tN>.md), kullanıcı "Onay:" satırını doldurur.
 *
 * Kullanım: node tools/sobi-eval/aciklamalar/denetle.mjs [--ogretici 3,14]
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const PARTS = JSON.parse(readFileSync(join(HERE, 'parcalar.json'), 'utf8'));
const ACK = JSON.parse(readFileSync(join(HERE, 'aciklamalar_tr.json'), 'utf8'));
const arg = (k) => { const i = process.argv.indexOf(`--${k}`); return i === -1 ? null : process.argv[i + 1]; };
const only = arg('ogretici') ? new Set(arg('ogretici').split(',').map(Number)) : null;

const numbersIn = (t) => new Set([...String(t || '').replace(/(\d)\.(?=\d{3}(?!\d))/g, '$1').matchAll(/\d+/g)].map((m) => Number(m[0])));
const FIIL = '(gelir|gider|geliyor|gidiyor|gelsin|gitsin|gelecek|gidecek|gelmeli|gitmeli)(?!\\p{L})';
const HAMLE = new RegExp(`(\\d+)(?:['’]\\p{L}+)?\\s*(?:tane\\s+)?(?:birlik\\s+)?(?:boncuk\\s+)?${FIIL}|beşlik\\s+boncuk(?:\\s+de)?\\s+${FIIL}`, 'giu');
const moves = (t) => new Set([...String(t || '').matchAll(HAMLE)].map((m) => `${m[1] ? Number(m[1]) : 5}${(m[2] || m[3]).startsWith('gel') ? '+' : '-'}`));
const YON = /(?<!\p{L})(?:aşağı(?!dak)\p{L}*|yukarı(?!dak)\p{L}*|indir\p{L}*|kaydır\p{L}*|sürükle\p{L}*|çek(?:in|ip|er|elim|iyoruz)?(?!\p{L}))/iu;
const sentences = (t) => String(t).replace(/(\d)\./g, '$1').split(/[.!?…]+(?:\s|$)/).map((x) => x.trim()).filter(Boolean).length;
// Kural adı → gereken öğretilmiş kural anahtarlarından biri.
const RULE_WORDS = [[/10['’]luk/i, ['add_ten', 'sub_ten']], [/boncuk kural/i, ['add_bead', 'sub_bead']], [/5['’]lik/i, ['add_five', 'sub_five']]];

const problems = [];
const missing = [];
const stale = [];
const parts = PARTS.filter((p) => p.steps.some((s) => s.kind === 'anlatim') && (!only || only.has(p.tutorial)));
for (const p of parts) {
  const a = ACK[p.key];
  if (!a) { missing.push(p.key); continue; }
  if (a.hash !== p.hash) stale.push(p.key);
  const siblings = PARTS.filter((x) => x.tutorial === p.tutorial && Math.abs(x.no - p.no) <= 1);
  // Dersin bütün işlemleri de kaynak: açıklama önceki bir örneği anabiliyor ("derste 50 − 5'i gördük").
  const ops = PARTS.filter((x) => x.tutorial === p.tutorial).map((x) => x.questionText);
  const src = [p.lessonTitle, ...ops, ...siblings.flatMap((x) => [x.questionText, ...x.steps.map((s) => s.text), JSON.stringify(x.op || {})])].join(' \n ');
  const srcNums = numbersIn(src);
  const srcMoves = moves(`${src} 5 gelir 5 gider 10 gelir 10 gider 1 gelir 1 gider`);
  for (const field of ['konu', 'basit', 'neden']) {
    const t = a[field];
    if (t == null) continue;
    const bad = (why) => problems.push(`${p.key}.${field}: ${why}`);
    const un = [...numbersIn(t)].filter((n) => n !== 5 && n !== 10 && !srcNums.has(n));
    if (un.length) bad(`kaynaksız sayı ${un.join(', ')}`);
    const um = [...moves(t)].filter((k) => !srcMoves.has(k));
    if (um.length) bad(`kaynakta olmayan hamle ${um.join(', ')}`);
    if (YON.test(t)) bad(`yön sözü: «${t.match(YON)[0]}»`);
    if (/(?<!\d)5 boncuk/i.test(t)) bad('"5 boncuk"');
    if (/yanlış/i.test(t)) bad('"yanlış"');
    if (field !== 'konu' && t.includes('?')) bad('soru işareti (geri soru sorma)');
    if (field !== 'konu' && sentences(t) > 4) bad(`${sentences(t)} cümle`);
    for (const [re, keys] of RULE_WORDS) if (re.test(t) && !keys.some((k) => p.taughtRules.includes(k))) bad(`öğretilmemiş kural adı «${t.match(re)[0]}»`);
  }
}

// Onay sayfaları
mkdirSync(join(HERE, 'onay'), { recursive: true });
for (const t of [...new Set(parts.map((p) => p.tutorial))]) {
  const list = parts.filter((p) => p.tutorial === t);
  const out = [
    `# Öğretici ${t}: ${list[0].lessonTitle || ''} — hazır açıklamalar`, '',
    'Her parçada önce dersin o kısımdaki adımları, sonra Sobi\'nin kullanacağı açıklamalar var.',
    '"Onay:" satırına **tamam** yaz ya da neyin değişmesi gerektiğini yaz (düzeltilmiş metni de yazabilirsin).',
    '"Basit": çocuk "anlamadım" derse. "Neden": çocuk "neden" diye sorarsa (yoksa Sobi "soroban\'da böyle çalışıyoruz" der).', '',
  ];
  for (const p of list) {
    const a = ACK[p.key];
    out.push(`## ${p.key}${p.questionText ? ` — ekranda: ${p.questionText}` : ''}`, '');
    out.push(`Ders: ${p.steps.map((s) => `«${s.text}»${s.kind !== 'anlatim' ? ' (soru)' : ''}`).join(' ')}`, '');
    if (!a) { out.push('_(açıklama yazılmadı)_', ''); continue; }
    out.push(`- **Konu:** ${a.konu}`, `- **Basit:** ${a.basit}`, `- **Neden:** ${a.neden ?? '_(ders gerekçe vermiyor)_'}`);
    const pr = problems.filter((x) => x.startsWith(`${p.key}.`));
    if (pr.length) out.push(`- ⚠ Otomatik denetim: ${pr.map((x) => x.slice(p.key.length + 1)).join('; ')}`);
    out.push('', 'Onay: ', '');
  }
  writeFileSync(join(HERE, 'onay', `t${t}.md`), `${out.join('\n')}\n`);
}

const total = PARTS.filter((p) => p.steps.some((s) => s.kind === 'anlatim')).length;
const written = PARTS.filter((p) => ACK[p.key]).length;
console.log(`Kapsam: ${written}/${total} anlatım parçasının açıklaması var${only ? ` (denetlenen öğretici: ${[...only].join(', ')})` : ''}.`);
if (!only) console.log(`Eksik: ${missing.length} parça`);
if (stale.length) console.log(`Eskimiş (ders metni değişmiş): ${stale.join(', ')}`);
console.log(problems.length ? `Sorunlar (${problems.length}):\n  ${problems.join('\n  ')}` : 'İçerik denetimi: sorun yok.');

/**
 * SOBİ HAFTALIK İYİLEŞTİRME RAPORU — beğen / beğenme oylarından (sobiChats/{uid}/sobiFeedback).
 *
 * Döngü (tools/sobi-eval/HAFTALIK_DONGU.md):
 *   1. Bu betik son N günün oylarını toplar, raporu yazar (model çağrısı yok, ücretsiz).
 *   2. Rapor Claude Code oturumuna verilir; Claude talimat / hazır açıklama değişikliği önerir.
 *   3. Öneri test setinde (hakemsiz, ~$0,1) ve kör yan yana karşılaştırmada denenir.
 *   4. Kullanıcı onaylarsa deploy edilir. Hiçbir adım kendiliğinden yayına çıkmaz.
 *
 * Kimlik: verify-lesson-leaderboard.js ile aynı (GOOGLE_APPLICATION_CREDENTIALS ya da
 * `gcloud auth application-default login`). Yalnızca okur.
 *
 * Rapor çocukların (maskelenmiş) mesajlarını içerir: haftalik/rapor-*.md Git'e girmez (.gitignore).
 *
 * Kullanım (depo kökünden):
 *   node tools/sobi-eval/haftalik/haftalik.js [--gun 7]
 */
const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');

const fnRequire = createRequire(path.join(__dirname, '..', '..', '..', 'functions', 'package.json'));
const admin = fnRequire('firebase-admin');
const STEPS = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'steps.json'), 'utf8'));

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i === -1 ? d : process.argv[i + 1]; };
const DAYS = Number(arg('gun', 7));

/** Hazır açıklama yeniden yazılsın: en az bu kadar oy ve bu oranda beğenmeme. */
const REWRITE_MIN_VOTES = 3;
const REWRITE_DISLIKE = 0.3;
/** Model cevabı hazır cevaba aday (kullanıcı önerisi): en az bu kadar oy ve bu oranda beğeni. */
const PROMOTE_MIN_VOTES = 5;
const PROMOTE_LIKE = 0.8;

const norm = (t) => String(t || '').toLocaleLowerCase('tr-TR').replace(/[^\p{L}\p{N} ]+/gu, '').replace(/\s+/g, ' ').trim();
const stepText = (t, i) => (STEPS.find((s) => s.tutorial === t && s.index === i) || {}).stepText || '';
const pct = (a, b) => (b ? `${Math.round((a / b) * 100)}%` : '—');

async function main() {
  if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });
  const db = admin.firestore();
  const since = Date.now() - DAYS * 24 * 3600 * 1000;
  // Koleksiyon grubu sorgusu filtresiz: süzme burada (filtreli sorgu ayrı bir dizin isterdi).
  const snap = await db.collectionGroup('sobiFeedback').get();
  const rows = snap.docs.map((d) => d.data()).filter((r) => r.at && r.at.toMillis && r.at.toMillis() >= since);

  const up = rows.filter((r) => r.value === 1).length;
  const down = rows.filter((r) => r.value === -1).length;
  const bySource = (s) => rows.filter((r) => r.source === s);

  // Hazır açıklamalar: anahtar başına oylar.
  const ack = new Map();
  for (const r of bySource('hazir')) {
    const k = r.aciklama || '?';
    const e = ack.get(k) || { up: 0, down: 0, samples: [] };
    if (r.value === 1) e.up++; else e.down++;
    if (r.value === -1 && e.samples.length < 3) e.samples.push(r.reply);
    ack.set(k, e);
  }
  // Model cevapları: aynı adım + aynı (normalleştirilmiş) çocuk mesajı ya da balon.
  const groups = new Map();
  for (const r of bySource('model')) {
    const k = `${r.tutorialNumber}|${r.stepIndex}|${r.suggestion || norm(r.childText)}`;
    const g = groups.get(k) || { tutorialNumber: r.tutorialNumber, stepIndex: r.stepIndex, ask: r.suggestion || r.childText, up: 0, down: 0, likedReplies: [] };
    if (r.value === 1) { g.up++; g.likedReplies.push(r.reply); } else g.down++;
    groups.set(k, g);
  }
  const disliked = bySource('model').filter((r) => r.value === -1).slice(0, 60);

  const out = [
    `# Sobi haftalık rapor — son ${DAYS} gün (${new Date().toISOString().slice(0, 10)})`, '',
    `${rows.length} oy: ${up} beğeni, ${down} beğenmeme (${pct(up, up + down)} beğeni). Hazır açıklama: ${bySource('hazir').length} oy, model: ${bySource('model').length} oy.`, '',
    '## 1. Yeniden yazılacak hazır açıklamalar', '',
    `(En az ${REWRITE_MIN_VOTES} oy ve %${REWRITE_DISLIKE * 100}+ beğenmeme.)`, '',
  ];
  const rewrite = [...ack].filter(([, e]) => e.up + e.down >= REWRITE_MIN_VOTES && e.down / (e.up + e.down) >= REWRITE_DISLIKE);
  if (!rewrite.length) out.push('_Yok._', '');
  for (const [k, e] of rewrite) out.push(`- **${k}**: ${e.up} 👍 / ${e.down} 👎 — ${e.samples[0] ? `«${e.samples[0]}»` : ''}`);
  out.push('', '## 2. Hazır cevaba aday model cevapları', '', `(Aynı adım ve aynı soru: en az ${PROMOTE_MIN_VOTES} oy, %${PROMOTE_LIKE * 100}+ beğeni.)`, '');
  const promote = [...groups.values()].filter((g) => g.up + g.down >= PROMOTE_MIN_VOTES && g.up / (g.up + g.down) >= PROMOTE_LIKE);
  if (!promote.length) out.push('_Yok._', '');
  for (const g of promote) out.push(`- t${g.tutorialNumber} adım ${g.stepIndex} «${stepText(g.tutorialNumber, g.stepIndex)}» — çocuk: «${g.ask}» — ${g.up} 👍 / ${g.down} 👎`, `  - örnek cevap: «${g.likedReplies[0]}»`);
  out.push('', '## 3. Beğenilmeyen model cevapları (talimat analizi için)', '');
  if (!disliked.length) out.push('_Yok._', '');
  for (const r of disliked) {
    out.push(`- t${r.tutorialNumber} adım ${r.stepIndex} «${stepText(r.tutorialNumber, r.stepIndex)}»${r.fellBack ? ' [yedek metin]' : ''}`);
    out.push(`  - çocuk: «${r.childText || r.suggestion || '?'}»`);
    out.push(`  - Sobi: «${r.reply}»`);
  }
  out.push('', '## 4. Öğretici başına', '', '| öğretici | 👍 | 👎 | beğeni |', '|---|---|---|---|');
  const tuts = [...new Set(rows.map((r) => r.tutorialNumber))].sort((a, b) => a - b);
  for (const t of tuts) {
    const l = rows.filter((r) => r.tutorialNumber === t);
    const u = l.filter((r) => r.value === 1).length;
    out.push(`| t${t} | ${u} | ${l.length - u} | ${pct(u, l.length)} |`);
  }
  const file = path.join(__dirname, `rapor-${new Date().toISOString().slice(0, 10)}.md`);
  fs.writeFileSync(file, `${out.join('\n')}\n`);
  console.log(`${rows.length} oy (${up} 👍, ${down} 👎) → ${file}`);
}

main().catch((e) => {
  console.error(/credential|permission|PERMISSION_DENIED|Could not load the default credentials/i.test(String(e && e.message))
    ? 'Firestore kimliği yok: GOOGLE_APPLICATION_CREDENTIALS ayarlayın ya da `gcloud auth application-default login`.'
    : e);
  process.exit(1);
});

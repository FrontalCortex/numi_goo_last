/**
 * ÖĞRETİCİ ADIM TABLOSU — değerlendirme setinin hammaddesi.
 *
 * TutorialFragment.kt'deki `tutorialStepsN = listOf(TutorialStep(...), ...)` listelerini kabaca
 * ayrıştırır, metinleri strings_*.xml'den çözer ve her adım için uygulamanın Sobi'ye göndereceği
 * bağlamı (SobiContext.kt) üretir: ders adı, öğretilen kurallar (SobiCurriculum), son 6 adımın
 * metni (ve bütün önceki adımlar: allSteps), soru adımında base/target.
 *
 * NEDEN KABA AYRIŞTIRMA: dosya 20 bin satır ve Kotlin; tam bir ayrıştırıcı gereksiz. Adımın ilk
 * argümanı metin, geri kalan özellikler `ad = değer` biçiminde. Bulunamayan (ör. çalışma anında
 * kurulan) adımlar atlanır; çıktıdaki sayılar bunu gösterir.
 *
 * base (adımın başında abaküste yazan): uygulamada animasyon bitince okunuyor, burada çıkarılamıyor.
 * Soru metni "a + b" / "a - b" ise ve a ± b = beklenen cevap ise base = a; değilse bir önceki
 * soru adımının cevabı denenir; ikisi de tutmazsa base bilinmiyor (null) ve adım soru seti için
 * kullanılmıyor.
 *
 * Kullanım: node tools/sobi-eval/extract-steps.js  → tools/sobi-eval/steps.json
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', '..');
const APP = path.join(ROOT, 'app', 'src', 'main');
const KT = fs.readFileSync(path.join(APP, 'java', 'com', 'example', 'app', 'TutorialFragment.kt'), 'utf8');
const LESSONS = fs.readFileSync(path.join(APP, 'java', 'com', 'example', 'app', 'GlobalLessonData.kt'), 'utf8');

// ── Metinler ─────────────────────────────────────────────────────────────

function unescapeAndroid(s) {
  return s
    .replace(/<[^>]+>/g, '')
    .replace(/\\n/g, ' ')
    .replace(/\\'/g, "'")
    .replace(/\\"/g, '"')
    .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&amp;/g, '&')
    .replace(/^"(.*)"$/s, '$1')
    .replace(/\s+/g, ' ')
    .trim();
}

const STRINGS = {};
const valuesDir = path.join(APP, 'res', 'values');
for (const f of fs.readdirSync(valuesDir).filter((x) => x.startsWith('strings') && x.endsWith('.xml'))) {
  const xml = fs.readFileSync(path.join(valuesDir, f), 'utf8');
  for (const m of xml.matchAll(/<string\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) STRINGS[m[1]] = unescapeAndroid(m[2]);
}

// ── Ders adları: LessonItem(... title = str(R.string.X) ... tutorialNumber = N ...) ──

const TITLES = {};
for (const block of LESSONS.split('LessonItem(').slice(1)) {
  const t = block.match(/title\s*=\s*str\(R\.string\.(\w+)\)/);
  const n = block.match(/tutorialNumber\s*=\s*(\d+)/);
  if (t && n && !TITLES[n[1]] && STRINGS[t[1]]) TITLES[n[1]] = STRINGS[t[1]];
}

// ── Öğretilen kurallar: SobiCurriculum.taughtRules ile aynı tablo ──

const ADD_DIRECT = ['add_direct'];
const ADD_FIVE = [...ADD_DIRECT, 'add_five'];
const ADD_TEN = [...ADD_FIVE, 'add_ten'];
const ADD_BEAD = [...ADD_TEN, 'add_bead'];
const SUB_DIRECT = [...ADD_BEAD, 'sub_direct'];
const SUB_FIVE = [...SUB_DIRECT, 'sub_five'];
const SUB_TEN = [...SUB_FIVE, 'sub_ten'];
const SUB_BEAD = [...SUB_TEN, 'sub_bead'];
function taughtRules(n) {
  if ([1, 100, 101].includes(n)) return [];
  if ([2, 102, 103].includes(n)) return ADD_DIRECT;
  if ([3, 4].includes(n)) return ADD_FIVE;
  if ([5, 6, 104, 7, 105].includes(n)) return ADD_TEN;
  if ([8, 9].includes(n)) return ADD_BEAD;
  if (n === 10) return SUB_DIRECT;
  if ([11, 1100].includes(n)) return SUB_FIVE;
  if ([12, 13, 14, 999, 15].includes(n)) return SUB_TEN;
  if (n >= 16 && n <= 26) return SUB_BEAD;
  return [];
}

// ── Adımlar ──────────────────────────────────────────────────────────────

function resolveText(expr) {
  if (!expr) return null;
  const s = expr.match(/^\s*(?:getString|appString)\(R\.string\.(\w+)/);
  if (s) return STRINGS[s[1]] ?? null;
  const lit = expr.match(/^\s*"((?:[^"\\]|\\.)*)"/);
  return lit ? lit[1].replace(/\\"/g, '"') : null;
}

function parseStep(seg) {
  const prop = (name) => {
    const m = seg.match(new RegExp(`\\b${name}\\s*=\\s*([^\\n]+)`));
    return m ? m[1].trim().replace(/,$/, '') : null;
  };
  const ans = prop('answerNumber');
  return {
    text: resolveText(seg) || '',
    id: ((prop('id') || '').match(/^"([^"]*)"/) || [])[1] || null,
    questionText: resolveText(prop('questionText') || ''),
    answerNumber: ans && /^\d+$/.test(ans) ? Number(ans) : null,
    abacusClickable: /abacusClickable\s*=\s*true/.test(seg),
    hasOptions: /\boptions\s*=\s*listOf/.test(seg),
  };
}

function inferBase(q, target, prev) {
  // Aynı işlemin ara adımı ("5 gelir", "kardeşi gider", "onlar basamağı"): bir önceki soru
  // adımının cevabından başlar.
  if (prev && prev.questionText === q && Number.isInteger(prev.answer) && prev.answer !== target) return prev.answer;
  if (/^\s*\d+\s*$/.test(q || '')) return 0; // "Sayıyı yaz" adımının ilki: abaküs sıfırdan
  const m = q && q.match(/^\s*(\d+)\s*([+\-−])\s*(\d+)\s*$/);
  if (!m) return null;
  const a = Number(m[1]);
  if (target === a) return 0; // "Önce ilk sayıyı yaz": abaküs sıfırlanmış
  const b = Number(m[3]);
  return (m[2] === '+' ? a + b : a - b) === target ? a : null;
}

const starts = [...KT.matchAll(/\n\s*(tutorialSteps(\d*))\s*=\s*listOf\(/g)];
const out = [];
for (let i = 0; i < starts.length; i++) {
  const num = starts[i][2] === '' ? 1 : Number(starts[i][2]);
  const end = i + 1 < starts.length ? starts[i + 1].index : KT.indexOf('data class TutorialStep(');
  const body = KT.slice(starts[i].index, end);
  const segs = body.split(/\bTutorialStep\(/).slice(1);
  const steps = segs.map(parseStep);
  let prev = null;
  steps.forEach((s, idx) => {
    const question = s.abacusClickable && s.answerNumber !== null && !s.hasOptions;
    const recent = steps.slice(Math.max(0, idx - 6), idx).map((x) => x.text).filter(Boolean);
    const base = question ? inferBase(s.questionText, s.answerNumber, prev) : null;
    out.push({
      tutorial: num,
      index: idx,
      kind: question ? 'soru' : (s.hasOptions ? 'secenekli' : 'anlatim'),
      lessonTitle: TITLES[num] || null,
      taughtRules: taughtRules(num),
      recentSteps: recent,
      // Uygulama bütün önceki adımları gönderecek (öneri 18); v1 ölçümü bununla yapılıyor.
      allSteps: steps.slice(0, idx).map((x) => x.text).filter(Boolean),
      stepId: s.id,
      stepText: s.text,
      questionText: s.questionText,
      base,
      target: question ? s.answerNumber : null,
    });
    if (question) prev = { questionText: s.questionText, answer: s.answerNumber };
  });
}

fs.writeFileSync(path.join(__dirname, 'steps.json'), `${JSON.stringify(out, null, 1)}\n`);
const by = {};
for (const s of out) {
  const k = s.tutorial;
  by[k] = by[k] || { anlatim: 0, soru: 0, soru_base_yok: 0, secenekli: 0 };
  by[k][s.kind]++;
  if (s.kind === 'soru' && s.base === null) by[k].soru_base_yok++;
}
console.log(`${out.length} adım; ders adı bulunamayan öğretici: ${[...new Set(out.filter((s) => !s.lessonTitle).map((s) => s.tutorial))].join(',') || 'yok'}`);
console.table(by);

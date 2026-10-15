#!/usr/bin/env node
// Uygulama metinlerinin çeviri aracı: öğretici, ders, layout ve kod metinleri (strings_*.xml).
//
// Kullanım (proje kökünden):
//   node tools/ceviri/ceviri.js disa-aktar en
//       → ceviri_en.csv : çevirmene gönderilecek tablo. Aynı Türkçe metin TEK satır.
//         Sütunlar: Anahtarlar | Nerede | Not | Türkçe | en
//         values-en'de zaten çevirisi olan satırlar dolu gelir (güncelleme turları için).
//
//   node tools/ceviri/ceviri.js ice-aktar en ceviri_en.csv
//       → app/src/main/res/values-en/strings_tutorial.xml ve strings_lessons.xml
//         Boş çeviriler atlanır ve listelenir (eksik metin uygulamada Türkçe görünür!).
//
//   node tools/ceviri/ceviri.js ses-listesi en
//       → ses_listesi_en.csv : seslendirme listesi. Her satır bir ses dosyası ve okunacak cümle.
//         Üretilen sesler app/src/main/res/raw-en/<ses dosyası>.mp3 olarak konur.
//
//   node tools/ceviri/ceviri.js kontrol en
//       → çevirisi ya da sesi eksik olanları listeler; eksik yoksa 0 ile çıkar.
//
// CSV'yi Google E-Tablolar'da açıp doldurmak en sorunsuzu (Dosya > İçe aktar; geri alırken
// Dosya > İndir > CSV). Excel de olur; "CSV UTF-8" olarak kaydedilmeli. Ayraç virgül ya da
// noktalı virgül olabilir, araç ikisini de tanır.

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const RES = path.join(ROOT, 'app', 'src', 'main', 'res');
// strings_ui: layout metinleri, strings_code: koddan gelen arayüz metinleri (çoğullar dahil).
const FILES = ['strings_tutorial.xml', 'strings_lessons.xml', 'strings_ui.xml', 'strings_code.xml'];
const TUTORIAL_KT = path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'example', 'app', 'TutorialFragment.kt');

// ───────────── strings.xml okuma / yazma (aapt kaçış kurallarıyla) ─────────────
function decodeValue(raw) {
  let v = raw.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&amp;/g, '&');
  let quoted = false;
  if (v.length >= 2 && v.startsWith('"') && v.endsWith('"') && !v.endsWith('\\"')) { v = v.slice(1, -1); quoted = true; }
  let out = '';
  for (let i = 0; i < v.length; i++) {
    const c = v[i];
    if (c === '\\') {
      const n = v[++i];
      out += n === 'n' ? '\n' : n === 't' ? '\t' : n;
    } else out += c;
  }
  return quoted ? out : out.replace(/\s+/g, ' ').trim();
}
function encodeValue(t) {
  let v = t.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/'/g, "\\'").replace(/\n/g, '\\n').replace(/\t/g, '\\t');
  if (/^[@?]/.test(v)) v = '\\' + v;
  v = v.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  if (/^\s|\s$|\s\s/.test(t)) v = '"' + v + '"';
  return v;
}
// Çoğul metinler (<plurals>) her biçim için ayrı satır olur: anahtar "ad#one", "ad#other".
// İngilizcede one/other yeter; başka dillerde (ör. Arapça) Android'in istediği diğer biçimler
// çeviride eklenebilir.
function readStrings(file) {
  if (!fs.existsSync(file)) return [];
  const xml = fs.readFileSync(file, 'utf8');
  const out = [...xml.matchAll(/<string name="([^"]+)"([^>]*)>([\s\S]*?)<\/string>/g)]
    .map(m => ({ key: m[1], text: decodeValue(m[3]), noFormat: /formatted="false"/.test(m[2]) }));
  for (const m of xml.matchAll(/<plurals name="([^"]+)">([\s\S]*?)<\/plurals>/g))
    for (const it of m[2].matchAll(/<item quantity="(\w+)">([\s\S]*?)<\/item>/g))
      out.push({ key: `${m[1]}#${it[1]}`, text: decodeValue(it[2]) });
  return out;
}
function writeStrings(file, entries, header, noFormatKeys = new Set()) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  let xml = '<?xml version="1.0" encoding="utf-8"?>\n<!-- ' + header + ' -->\n<resources>\n';
  const plurals = new Map();
  for (const e of entries) {
    const [name, qty] = e.key.split('#');
    if (qty) { if (!plurals.has(name)) plurals.set(name, []); plurals.get(name).push([qty, e.text]); continue; }
    xml += `    <string name="${e.key}"${noFormatKeys.has(e.key) ? ' formatted="false"' : ''}>${encodeValue(e.text)}</string>\n`;
  }
  for (const [name, items] of plurals)
    xml += `    <plurals name="${name}">\n${items.map(([q, t]) => `        <item quantity="${q}">${encodeValue(t)}</item>\n`).join('')}    </plurals>\n`;
  fs.writeFileSync(file, xml + '</resources>\n', 'utf8');
}

// ───────────── CSV ─────────────
function csvEscape(v, sep) { v = v ?? ''; return /["\n\r]/.test(v) || v.includes(sep) ? '"' + v.replace(/"/g, '""') + '"' : v; }
function writeCsv(file, rows) {
  const sep = ',';
  fs.writeFileSync(file, '\uFEFF' + rows.map(r => r.map(c => csvEscape(c, sep)).join(sep)).join('\r\n') + '\r\n', 'utf8');
}
function readCsv(file) {
  let s = fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
  const firstLine = s.slice(0, s.search(/\r?\n/));
  const sep = (firstLine.match(/;/g) || []).length > (firstLine.match(/,/g) || []).length ? ';' : ',';
  const rows = []; let row = [], cell = '', q = false;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (q) {
      if (c === '"' && s[i + 1] === '"') { cell += '"'; i++; } else if (c === '"') q = false; else cell += c;
    } else if (c === '"') q = true;
    else if (c === sep) { row.push(cell); cell = ''; }
    else if (c === '\n' || c === '\r') { if (c === '\r' && s[i + 1] === '\n') i++; row.push(cell); rows.push(row); row = []; cell = ''; }
    else cell += c;
  }
  if (cell !== '' || row.length) { row.push(cell); rows.push(row); }
  return rows.filter(r => r.some(c => c.trim() !== ''));
}

// ───────────── Bağlam: hangi anahtar hangi öğreticide, hangi ses hangi metinle ─────────────
function tutorialUsage() {
  const src = fs.readFileSync(TUTORIAL_KT, 'utf8');
  const where = new Map(); // key -> Set(tutorialNo)
  const soundText = new Map(); // ses -> Map(key -> adet)
  const add = (k, n) => { if (!where.has(k)) where.set(k, new Set()); where.get(k).add(n); };
  const pair = (snd, k) => { if (!snd || !k) return; const m = soundText.get(snd) || new Map(); m.set(k, (m.get(k) || 0) + 1); soundText.set(snd, m); };
  const fns = [...src.matchAll(/private fun createTutorialSteps(\d+)\s*\(\)\s*\{/g)];
  fns.forEach((m, i) => {
    const body = src.slice(m.index, i + 1 < fns.length ? fns[i + 1].index : src.length);
    const n = +m[1];
    for (const x of body.matchAll(/R\.string\.(\w+)/g)) add(x[1], n);
    for (const step of body.split(/TutorialStep\(/).slice(1)) {
      const k = /^\s*getString\(R\.string\.(\w+)\)/.exec(step);
      const snd = /soundResource\s*=\s*R\.raw\.(\w+)/.exec(step);
      if (k && snd) pair(snd[1], k[1]);
    }
  });
  for (const m of src.matchAll(/private val TUTORIAL_(\d+)_JSON = """([\s\S]*?)"""/g)) {
    for (const x of m[2].matchAll(/@string\/(\w+)/g)) add(x[1], +m[1]);
    for (const line of m[2].split('\n')) {
      const k = /"text":"@string\/(\w+)"/.exec(line), snd = /"sound":"(\w+)"/.exec(line);
      if (k && snd) pair(snd[1], k[1]);
    }
  }
  return { where, soundText };
}

function trEntries() {
  const all = [];
  for (const f of FILES) for (const e of readStrings(path.join(RES, 'values', f))) all.push({ ...e, file: f });
  return all;
}
function translations(lang) {
  const t = new Map();
  for (const f of FILES) for (const e of readStrings(path.join(RES, 'values-' + lang, f))) t.set(e.key, e.text);
  return t;
}
const markers = t => (t.match(/[\[\]]/g) || []).join('');
// %1$s, %2$s… yer tutucuları: çeviride hepsi bulunmalı (sırası değişebilir). Eksik ya da fazla
// yer tutucu uygulamayı çökertebilir.
const placeholders = t => (t.match(/%\d+\$s/g) || []).sort().join(' ');
// Arayüz metninin hangi ekrandan geldiği: XML'deki <!-- ... --> bölüm başlıklarından
function sectionsOf(file) {
  const map = new Map();
  if (!fs.existsSync(file)) return map;
  let current = '';
  for (const m of fs.readFileSync(file, 'utf8').matchAll(/<!--\s*([^\n]*?)\s*-->|<(?:string|plurals) name="([^"]+)"/g)) {
    if (m[1] !== undefined) current = m[1].replace(/^layout\//, '').replace(/\.(xml|kt)$/, '');
    else map.set(m[2], current);
  }
  return map;
}
// Kodun özel biçimde kullandığı metinler için çevirmene not
const SPECIAL_NOTES = {
  ui_hesap_olusturarak_kullanim_sartlarini_ve_giz: 'Bu cümlenin içindeki "Kullanım Şartlarını" ve "Gizlilik Politikasını" çevirileri, o iki satırdaki çevirilerle BİREBİR aynı yazılmalı (tıklanabilir bağlantı olarak aranıyorlar).',
  ui_kullanim_sartlarini: '"Hesap oluşturarak…" cümlesindeki çeviriyle birebir aynı olmalı.',
  ui_gizlilik_politikasini: '"Hesap oluşturarak…" cümlesindeki çeviriyle birebir aynı olmalı.',
  ui_her_gun: 'Cümlenin başı; ortasına hedef süre ("10 dakika" gibi), sonuna "buraya varacaksın!" gelir.',
  ui_buraya_varacaksin: 'Cümlenin sonu; başında "Her gün" ve hedef süre var.',
  ui_week_initials_monday_first: 'Pazartesiden başlayarak haftanın 7 gününün baş harfi, aralarında birer boşluk.',
};
const QTY = { one: 'TEKİL biçim (sayı 1 iken)', other: 'ÇOĞUL biçim (diğer sayılar)', zero: 'sayı 0 iken', two: 'sayı 2 iken', few: 'az biçimi', many: 'çok biçimi' };

// ───────────── Komutlar ─────────────
const [cmd, lang, csvPath] = process.argv.slice(2);
if (!cmd || !lang) { console.log(fs.readFileSync(__filename, 'utf8').split('\n').slice(1, 24).join('\n')); process.exit(1); }

if (cmd === 'disa-aktar') {
  const { where } = tutorialUsage();
  const tr = translations(lang);
  const sec = new Map(FILES.flatMap(f => [...sectionsOf(path.join(RES, 'values', f))]));
  const groups = new Map(); // Türkçe (+ çoğul biçimi) -> {keys, where, lessons, screens}
  for (const e of trEntries()) {
    const qty = e.key.split('#')[1] || '';
    const gk = qty + '\u0000' + e.text;
    const g = groups.get(gk) || { text: e.text, qty, keys: [], where: new Set(), lessons: new Set(), screens: new Set() };
    g.keys.push(e.key);
    if (e.file === 'strings_tutorial.xml') for (const n of where.get(e.key) || []) g.where.add(n);
    else if (e.file === 'strings_lessons.xml') g.lessons.add(e.key.replace(/^lesson_/, '').replace(/_(title|hint|unit|race)$/, ' ($1)'));
    else g.screens.add(sec.get(e.key.split('#')[0]) || '?');
    groups.set(gk, g);
  }
  const list = (set, n = 3) => [...set].slice(0, n).join('; ') + (set.size > n ? ` (+${set.size - n})` : '');
  const rows = [['Anahtarlar', 'Nerede', 'Not', 'Türkçe', lang]];
  for (const g of groups.values()) {
    const text = g.text;
    const nerede = [
      g.where.size ? 'Öğretici ' + [...g.where].sort((a, b) => a - b).join(', ') : '',
      g.lessons.size ? 'Ders: ' + list(g.lessons) : '',
      g.screens.size ? 'Ekran: ' + list(g.screens) : '',
    ].filter(Boolean).join(' | ');
    const not = [
      text.includes('[') ? 'Köşeli parantez içi mavi gösterilir; parantezleri çeviride de aynı kısma koy.' : '',
      /%\d+\$s/.test(text) ? '%1$s gibi yer tutucuları aynen koru (yerleri değişebilir).' : '',
      g.qty ? `Çoğul metin: ${QTY[g.qty] || g.qty}.` : '',
      ...g.keys.map(k => SPECIAL_NOTES[k]).filter(Boolean),
    ].filter(Boolean).join(' ');
    const existing = [...new Set(g.keys.map(k => tr.get(k)).filter(Boolean))];
    rows.push([g.keys.join(' '), nerede, not, text, existing.length === 1 ? existing[0] : '']);
  }
  const out = path.join(ROOT, `ceviri_${lang}.csv`);
  writeCsv(out, rows);
  console.log(`${out}\n${rows.length - 1} satır (${trEntries().length} anahtar), dolu: ${rows.slice(1).filter(r => r[4]).length}`);

} else if (cmd === 'ice-aktar') {
  if (!csvPath) { console.error('CSV yolu gerekli'); process.exit(1); }
  const rows = readCsv(csvPath);
  const head = rows[0].map(h => h.trim());
  const ki = head.indexOf('Anahtarlar'), ti = head.indexOf('Türkçe'), li = head.indexOf(lang);
  if (ki < 0 || li < 0) { console.error(`Başlıkta "Anahtarlar" ve "${lang}" sütunları bulunamadı: ${head.join(' | ')}`); process.exit(1); }
  const byKey = new Map(), problems = [];
  for (const r of rows.slice(1)) {
    const t = (r[li] || '').trim();
    const src = r[ti] || '';
    if (!t) continue;
    if (markers(src) !== markers(t)) problems.push(`[ ] işaretleri uyuşmuyor: "${src}" → "${t}"`);
    if (placeholders(src) !== placeholders(t)) problems.push(`%1$s yer tutucuları uyuşmuyor: "${src}" → "${t}"`);
    for (const k of (r[ki] || '').split(/\s+/).filter(Boolean)) byKey.set(k, t);
  }
  const missing = [];
  for (const f of FILES) {
    const src = readStrings(path.join(RES, 'values', f));
    const out = [];
    for (const e of src) { if (byKey.has(e.key)) out.push({ key: e.key, text: byKey.get(e.key) }); else missing.push(e); }
    const noFormat = new Set(src.filter(e => e.noFormat).map(e => e.key));
    writeStrings(path.join(RES, 'values-' + lang, f), out, `${f} — ${lang} çevirisi. Elle düzenleme; tools/ceviri/ceviri.js ice-aktar ile üretilir.`, noFormat);
  }
  console.log(`yazıldı: values-${lang}/ (${byKey.size} anahtar)`);
  if (problems.length) console.log('\nİŞARET SORUNLARI:\n' + problems.join('\n'));
  if (missing.length) {
    const uniq = [...new Set(missing.map(e => e.text))];
    console.log(`\nÇEVİRİSİ EKSİK: ${missing.length} anahtar (${uniq.length} farklı metin) — uygulamada Türkçe görünecek:`);
    uniq.slice(0, 20).forEach(t => console.log('  - ' + t));
    if (uniq.length > 20) console.log(`  … ve ${uniq.length - 20} tane daha`);
  }

} else if (cmd === 'ses-listesi' || cmd === 'kontrol') {
  const { soundText } = tutorialUsage();
  const tr = translations(lang);
  const trSrc = new Map(trEntries().map(e => [e.key, e.text]));
  const rawDir = path.join(RES, 'raw-' + lang);
  const have = new Set(fs.existsSync(rawDir) ? fs.readdirSync(rawDir).map(f => f.replace(/\.[^.]+$/, '')) : []);
  const rows = [['Ses dosyası', 'Metin anahtarı', 'Türkçe', lang, 'Durum']];
  let noText = 0, noAudio = 0;
  for (const [snd, m] of [...soundText].sort()) {
    const key = [...m].sort((a, b) => b[1] - a[1])[0][0];
    const t = tr.get(key) || '';
    if (!t) noText++;
    if (!have.has(snd)) noAudio++;
    rows.push([snd, key, trSrc.get(key) || '', t, !t ? 'çeviri yok' : have.has(snd) ? 'var' : 'kaydedilecek']);
  }
  const missingText = trEntries().filter(e => !tr.has(e.key));
  if (cmd === 'ses-listesi') {
    const out = path.join(ROOT, `ses_listesi_${lang}.csv`);
    writeCsv(out, rows);
    console.log(`${out}\n${rows.length - 1} ses dosyası; kaydedilecek: ${noAudio}, çevirisi henüz yok: ${noText}`);
  } else {
    console.log(`metin: ${trEntries().length} anahtar, çevirisi eksik: ${missingText.length}`);
    console.log(`ses: ${rows.length - 1} dosya, raw-${lang}'de eksik: ${noAudio}`);
    process.exit(missingText.length || noAudio ? 1 : 0);
  }
} else {
  console.error('Bilinmeyen komut: ' + cmd); process.exit(1);
}

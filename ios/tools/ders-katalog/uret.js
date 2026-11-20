// Android GlobalLessonData.kt içindeki kataloğu ortak koda çevirir ve test beklentilerini üretir.
// Kullanım: node uret.js <GlobalLessonData.kt> <cikti-katalog.kt> <cikti-beklenti.json>
const fs = require('fs');
const [, , kaynakYol, ciktiKt, ciktiJson] = process.argv;
const satirlar = fs.readFileSync(kaynakYol, 'utf8').replace(/\r\n/g, '\n').split('\n');

const basRe = /^ {12}(\d+) -> listOf\($/;
const baslar = [];
let son = -1;
for (let i = 0; i < satirlar.length; i++) {
  const m = basRe.exec(satirlar[i]);
  if (m) baslar.push({ part: Number(m[1]), satir: i });
  if (/^ {12}else -> emptyList\(\)$/.test(satirlar[i])) son = i;
}
if (son < 0) throw new Error('else dalı bulunamadı');
console.error('part başlangıçları:', baslar.map(b => `${b.part}@${b.satir + 1}`).join(' '), 'son@' + (son + 1));

// ---- 1) Kotlin çıktısı ----
let kt = '';
kt += '// KAYNAK: app/src/main/java/com/example/app/GlobalLessonData.kt — createLessonItems() içindeki `when(partId)` dalları.\n';
kt += '// Bu dosya Android kaynağından betikle (metin dönüşümü) üretildi; item sırası ve alan değerleri birebir aynıdır.\n';
kt += '// Tek fark: `R.color.x` kaynak kimlikleri kaynak adını taşıyan String anahtarına ("x") çevrildi.\n';
kt += '// Elle düzenleme: Android tarafında katalog değişirse aynı değişiklik burada da yapılmalı.\n';
kt += 'package com.sorobit.shared.ders\n\n';
const bloklar = {};
for (let b = 0; b < baslar.length; b++) {
  const bas = baslar[b].satir;
  const bit = b + 1 < baslar.length ? baslar[b + 1].satir : son;
  const govde = satirlar.slice(bas + 1, bit);
  // Sondaki boş satırları at
  while (govde.length && govde[govde.length - 1].trim() === '') govde.pop();
  bloklar[baslar[b].part] = govde.join('\n');
  const donusmus = govde
    .map(s => s.replace(/R\.color\.([A-Za-z0-9_]+)/g, '"$1"'))
    .map(s => (s.startsWith('            ') ? s.slice(12) : s.trimStart()))
    .map(s => s.replace(/\s+$/, ''))
    .join('\n');
  if (/\bR\.[a-z]+\./.test(donusmus)) throw new Error('çevrilmemiş R başvurusu part ' + baslar[b].part);
  kt += `/** Part ${baslar[b].part} kataloğu (Android: \`${baslar[b].part} -> listOf(...)\`). */\n`;
  kt += `internal fun katalogPart${baslar[b].part}(): List<LessonItem> = listOf(\n${donusmus}\n\n`;
}
fs.writeFileSync(ciktiKt, kt.replace(/\n+$/, '\n'));

// ---- 2) Bağımsız ayrıştırıcı: LessonItem(...) çağrılarını oku ----
function yorumsuz(metin) {
  let o = '', i = 0;
  while (i < metin.length) {
    const c = metin[i];
    if (c === '"') { let j = i + 1; while (metin[j] !== '"') { if (metin[j] === '\\') j++; j++; } o += metin.slice(i, j + 1); i = j + 1; continue; }
    if (c === '/' && metin[i + 1] === '/') { while (i < metin.length && metin[i] !== '\n') i++; continue; }
    o += c; i++;
  }
  return o;
}
function itemlariAyristir(hamMetin) {
  const metin = yorumsuz(hamMetin);
  const itemlar = [];
  let i = 0;
  while (true) {
    let k = metin.indexOf('LessonItem(', i);
    while (k > 0 && /[A-Za-z]/.test(metin[k - 1])) k = metin.indexOf('LessonItem(', k + 1);
    if (k < 0) break;
    // "LessonItem.TYPE_" değil, çağrı olduğundan emin ol
    let j = k + 'LessonItem('.length;
    let derinlik = 1;
    const args = [];
    let buf = '';
    while (derinlik > 0) {
      const c = metin[j];
      if (c === undefined) throw new Error('kapanmayan parantez');
      if (c === '"') {
        let s = '"';
        j++;
        while (metin[j] !== '"') {
          if (metin[j] === '\\') { s += metin[j] + metin[j + 1]; j += 2; continue; }
          s += metin[j]; j++;
        }
        s += '"'; j++;
        buf += s;
        continue;
      }
      if (c === '/' && metin[j + 1] === '/') { while (metin[j] !== '\n') j++; continue; }
      if (c === '(') derinlik++;
      if (c === ')') { derinlik--; if (derinlik === 0) { j++; break; } }
      if (c === ',' && derinlik === 1) { args.push(buf); buf = ''; j++; continue; }
      buf += c; j++;
    }
    if (buf.trim() !== '') args.push(buf);
    const alanlar = {};
    for (const a of args) {
      const t = a.trim();
      if (t === '') continue;
      const m = /^([A-Za-z0-9_]+)\s*=\s*([\s\S]+)$/.exec(t);
      if (!m) throw new Error('adlandırılmamış argüman: ' + t);
      if (m[1] in alanlar) throw new Error('yinelenen alan ' + m[1]);
      alanlar[m[1]] = degerCoz(m[2].trim());
    }
    itemlar.push(alanlar);
    i = j;
  }
  return itemlar;
}
function degerCoz(v) {
  if (v.startsWith('"')) {
    if (!v.endsWith('"')) throw new Error('bozuk string ' + v);
    const ic = v.slice(1, -1);
    let o = '';
    for (let i = 0; i < ic.length; i++) {
      const c = ic[i];
      if (c === '$') throw new Error('string şablonu desteklenmiyor: ' + v);
      if (c !== '\\') { o += c; continue; }
      const n = ic[++i];
      if (n === 'n') o += '\n';
      else if (n === 't') o += '\t';
      else if (n === '\\' || n === '"' || n === "'" || n === '$') o += n;
      else throw new Error('bilinmeyen kaçış \\' + n);
    }
    return o;
  }
  if (v === 'true') return true;
  if (v === 'false') return false;
  if (/^-?\d+$/.test(v)) return Number(v);
  let m = /^LessonItem\.TYPE_(LESSON|HEADER|CHEST|RACE)$/.exec(v);
  if (m) return { LESSON: 0, HEADER: 1, CHEST: 2, RACE: 3 }[m[1]];
  m = /^R\.color\.([A-Za-z0-9_]+)$/.exec(v);
  if (m) return m[1];
  throw new Error('çözülemeyen değer: ' + v);
}
const SIRA = ['type', 'title', 'titleUnit', 'offset', 'isCompleted', 'stepCount', 'currentStep', 'finishStepNumber',
  'startStepNumber', 'tutorialNumber', 'tutorialIsFinish', 'stepIsFinish', 'lessonHint', 'color', 'abacusGuideNumber',
  'cupPoint1', 'cupPoint2', 'worstCupTime', 'isBlinding', 'blindingMultiplication', 'timePeriod', 'raceBusyLevel', 'raceTitle'];
const VARSAYILAN = { currentStep: 0, tutorialNumber: 0, tutorialIsFinish: false, stepIsFinish: false };
const BILINEN = new Set([...SIRA, 'mapFragmentIndex']);
function kanonik(it) {
  for (const k of Object.keys(it)) if (!BILINEN.has(k)) throw new Error('bilinmeyen alan: ' + k);
  return SIRA.map(k => (k in it ? String(it[k]) : (k in VARSAYILAN ? String(VARSAYILAN[k]) : 'null'))).join('|');
}
function javaHash(s) {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}
const beklenti = {};
let toplam = 0;
for (const part of Object.keys(bloklar)) {
  const itemlar = itemlariAyristir(bloklar[part]);
  toplam += itemlar.length;
  const kan = itemlar.map(kanonik);
  const tur = [0, 0, 0, 0];
  itemlar.forEach(it => tur[it.type]++);
  beklenti[part] = {
    adet: itemlar.length,
    tur,
    hash: javaHash(kan.join('\n')),
    ilk: kan[0],
    son: kan[kan.length - 1],
    toplamStepCount: itemlar.reduce((a, it) => a + it.stepCount, 0),
    toplamFinish: itemlar.reduce((a, it) => a + (it.finishStepNumber ?? 0), 0),
    toplamStart: itemlar.reduce((a, it) => a + (it.startStepNumber ?? 0), 0),
    ilkSandik: itemlar.findIndex(it => it.type === 2),
    bildirilenMapFragmentIndex: itemlar.map(it => it.mapFragmentIndex ?? null),
    basliklar: itemlar.map(it => it.title),
  };
}
const kaynakSayim = (satirlar.slice(baslar[0].satir, son).join('\n').match(/(?<![A-Za-z])LessonItem\(/g) || []).length;
console.error('toplam item:', toplam, 'kaynakta LessonItem( sayısı:', kaynakSayim);
if (toplam !== kaynakSayim) throw new Error('sayım uyuşmuyor');
fs.writeFileSync(ciktiJson, JSON.stringify(beklenti, null, 1));

// ---- 3) Ek özetler (test beklentileri için) ----
const ek = {};
const tp = new Set();
for (const part of Object.keys(bloklar)) {
  const itemlar = itemlariAyristir(bloklar[part]);
  itemlar.forEach(it => { if (it.timePeriod != null) tp.add(it.timePeriod); });
  ek[part] = {
    bitmis: itemlar.map((it, i) => (it.stepIsFinish ? i : -1)).filter(i => i >= 0),
    sandiklar: itemlar.map((it, i) => (it.type === 2 ? i : -1)).filter(i => i >= 0),
    basliklar: itemlar.map((it, i) => (it.type === 1 ? i : -1)).filter(i => i >= 0),
    kilitli: itemlar.map((it, i) => (!it.isCompleted && it.type !== 1 ? i : -1)).filter(i => i >= 0),
    raceBusy: itemlar.map(it => it.raceBusyLevel ?? null),
    renkler: itemlar.filter(it => it.type === 1).map(it => it.color),
  };
}
fs.writeFileSync(ciktiJson.replace('.json', '-ek.json'), JSON.stringify({ ek, timePeriod: [...tp].sort((a, b) => a - b) }));

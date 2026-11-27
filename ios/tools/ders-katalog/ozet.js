// GlobalLessonDataKatalog.kt + dallar.json → DersAdimAltinVeri.kt içindeki ALTIN_KATALOG_OZET satırları (dersadim testi).
// Kullanım: node ozet.js <GlobalLessonDataKatalog.kt> <dallar.json ({o:[...],b:[...]})> <cikti.txt>
// dallar.json, ders-adim/uret.js çalışınca (hedef klasörün bir üstündeki tools/ altında) üretilir.
const fs = require('fs');
const kt = fs.readFileSync(process.argv[2], 'utf8');
const d = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'));
const O = new Set(d.o);
const B = new Set(d.b.filter(x => x !== 83 && x !== 100)); // iki boş dal
const parts = kt.split(/\/\*\* Part (\d+) kataloğu/).slice(1);
const satirlar = [];
for (let i = 0; i < parts.length; i += 2) {
  const p = Number(parts[i]);
  if (p > 8) continue;
  const items = parts[i + 1].split('LessonItem(\n').slice(1);
  items.forEach((b, idx) => {
    const g = k => {
      const m = new RegExp('\\b' + k + ' = ([^\\n]*?),?\\s*(//[^\\n]*)?\\n').exec(b);
      return m ? m[1].replace(/,$/, '') : null;
    };
    const start = g('startStepNumber') == null ? null : Number(g('startStepNumber'));
    if (start == null) return;
    const finish = g('finishStepNumber') == null ? start : Number(g('finishStepNumber'));
    const n = Number(g('stepCount'));
    const min = (g('finishStepNumber') == null ? start : finish) - n + 1;
    const kor = g('isBlinding') === 'true' || p === 7 || p === 8;
    const set = kor ? B : O;
    const empty = id => !set.has(id);
    const res = req => { let id = req; while (empty(id) && id > min) id--; return id; };
    const istenenler = start <= finish ? Array.from({ length: finish - start + 1 }, (_, k) => start + k) : [start, finish];
    const par = istenenler.map(r => { const c = res(r); return `${r}>${c}` + (empty(c) ? '!' : ''); });
    satirlar.push(`${p}|${idx}|${kor ? 'B' : 'O'}|${start}-${finish}|${min}|${par.join(',')}`);
  });
}
fs.writeFileSync(process.argv[4], satirlar.join('\n') + '\n');
console.log('satır', satirlar.length, 'boş:', satirlar.filter(s => s.includes('!')));

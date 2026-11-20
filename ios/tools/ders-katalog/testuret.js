// beklenti.json → GlobalLessonDataKatalogBeklenti.kt (test beklenti tablosu)
const fs = require('fs');
const b = require('./beklenti.json');
const e = require('./beklenti-ek.json').ek;
const q = s => '"' + s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$') + '"';
const li = a => 'listOf(' + a.join(', ') + ')';
const liI = a => (a.length ? li(a) : 'emptyList()');
let o = '';
o += '// Bu dosya betikle üretildi: Android kaynağındaki (app/src/main/java/com/example/app/GlobalLessonData.kt)\n';
o += '// `LessonItem(` oluşumları, ortak koddan BAĞIMSIZ bir ayrıştırıcıyla (regex/karakter tarayıcı) okunup\n';
o += '// part başına özetlendi. Testler ortak koddaki kataloğu bu sabit beklentilerle karşılaştırır.\n';
o += 'package com.sorobit.shared.ders\n\n';
o += 'internal data class PartBeklenti(\n    val part: Int,\n    val adet: Int,\n    /** [TYPE_LESSON, TYPE_HEADER, TYPE_CHEST, TYPE_RACE] adetleri. */\n    val tur: List<Int>,\n    /** Tüm item\'ların kanonik satırlarının (\\n ile birleşik) Java String.hashCode değeri. */\n    val hash: Int,\n    val ilk: String,\n    val son: String,\n    val toplamStepCount: Int,\n    val toplamFinish: Int,\n    val toplamStart: Int,\n    val bitmis: List<Int>,\n    val sandiklar: List<Int>,\n    val basliklar: List<Int>,\n    val kilitli: List<Int>,\n    val raceBusy: List<Int?>,\n    val renkler: List<String>,\n    val basliklarMetin: List<String>,\n)\n\n';
o += 'internal val KATALOG_BEKLENTI: List<PartBeklenti> = listOf(\n';
for (const p of Object.keys(b)) {
  const x = b[p], y = e[p];
  o += '    PartBeklenti(\n';
  o += `        part = ${p},\n        adet = ${x.adet},\n        tur = ${li(x.tur)},\n        hash = ${x.hash},\n`;
  o += `        ilk = ${q(x.ilk)},\n        son = ${q(x.son)},\n`;
  o += `        toplamStepCount = ${x.toplamStepCount},\n        toplamFinish = ${x.toplamFinish},\n        toplamStart = ${x.toplamStart},\n`;
  o += `        bitmis = ${liI(y.bitmis)},\n        sandiklar = ${liI(y.sandiklar)},\n        basliklar = ${liI(y.basliklar)},\n        kilitli = ${liI(y.kilitli)},\n`;
  o += `        raceBusy = ${y.raceBusy.every(v => v === null) ? `List(${y.raceBusy.length}) { null }` : li(y.raceBusy.map(String))},\n`;
  o += `        renkler = ${y.renkler.length ? li(y.renkler.map(q)) : 'emptyList()'},\n`;
  o += `        basliklarMetin = listOf(\n${x.basliklar.map(t => '            ' + q(t) + ',').join('\n')}\n        ),\n`;
  o += '    ),\n';
}
o += ')\n';
fs.writeFileSync(process.argv[2], o);

// MapFragment.kt içindeki tabloyu mekanik olarak ortak koda dönüştürür ve (yalıtılmış kopyada) Android referansını üretir.
const fs = require('fs'), path = require('path');
const [depo, hedef] = process.argv.slice(2);
const src = fs.readFileSync(path.join(depo, 'app/src/main/java/com/example/app/MapFragment.kt'), 'utf8').replace(/\r\n/g, '\n').split('\n');
const bas = src.findIndex(l => l.includes('fun getLessonOperations(lessonId: Int): List<MathOperation>'));
const blind = src.findIndex(l => l.includes('fun getLessonOperationsBlinding(lessonId: Int)'));
let son = -1;
for (let i = blind; i < src.length; i++) if (src[i] === '        }') { son = i; break; }
if (bas < 0 || blind < 0 || son < 0) throw new Error('sınırlar bulunamadı');
const govde = src.slice(bas, son + 1);
console.log(`satırlar ${bas + 1}-${son + 1} (${govde.length} satır)`);
const dedent = govde.map(l => l.startsWith('    ') ? l.slice(4) : l).map(l => l.replace(/\s+$/, ''));
const main = `// KAYNAK: app/src/main/java/com/example/app/MapFragment.kt — companion object içindeki
// getLessonOperations, resolveLessonOperationsId, resolveLessonOperationsBlindingId ve
// getLessonOperationsBlinding (Android dosyasında satır ${bas + 1}-${son + 1}).
//
// Bu dosya tek seferlik bir betikle, mekanik dönüştürmeyle üretildi: fonksiyon gövdeleri
// Android'dekiyle KARAKTERİ KARAKTERİNE aynıdır (yalnızca bir girinti düzeyi azaltıldı, satır sonu
// boşlukları kırpıldı). Dal sırası, yorum satırına alınmış dallar ve tekrar eden değerler aynen korunur.
//
// Android'den bilinçli farklar:
//  - Android'de bu fonksiyonlar \`MapFragment.Companion\` üyesidir; Fragment ortak koda taşınamadığı için
//    burada [LessonOperations] nesnesinde durur (çağrı: \`LessonOperations.getLessonOperations(id)\`).
//  - Rastgelelik [com.sorobit.shared.matematik.RastgeleKaynagi] üzerinden gelir (üreteçler öyle taşındı);
//    varsayılanı tohumsuzdur, yani Android'deki davranıştır.
package com.sorobit.shared.dersadim

import com.sorobit.shared.matematik.GlobalValues.randomNumberChangeToString
import com.sorobit.shared.matematik.GlobalValues.randomUniqueNumberStrings
import com.sorobit.shared.matematik.MathOperation
import com.sorobit.shared.matematik.MathOperationGenerator
import com.sorobit.shared.matematik.MathOperationGenerator.generateRandomMathOperation1
import com.sorobit.shared.matematik.MathOperationGenerator.generateRelatedNumbers2

/** Ders adımı (lessonOperationsId) → soru listesi tabloları. */
object LessonOperations {
${dedent.join('\n')}
}
`;
const cm = path.join(hedef, 'shared/src/commonMain/kotlin/com/sorobit/shared/dersadim');
fs.mkdirSync(cm, { recursive: true });
fs.writeFileSync(path.join(cm, 'LessonOperations.kt'), main);
// Android referansı: gövde HİÇ değiştirilmeden (yalnızca paket/import) — sadece yalıtılmış kopyada.
const ref = `package androidref

import com.sorobit.shared.matematik.GlobalValues.randomNumberChangeToString
import com.sorobit.shared.matematik.GlobalValues.randomUniqueNumberStrings
import com.sorobit.shared.matematik.MathOperation
import com.sorobit.shared.matematik.MathOperationGenerator
import com.sorobit.shared.matematik.MathOperationGenerator.generateRandomMathOperation1
import com.sorobit.shared.matematik.MathOperationGenerator.generateRelatedNumbers2

class MapFragmentRef {
    companion object {
${govde.join('\n')}
    }
}
`;
const jt = path.join(hedef, 'shared/src/jvmTest/kotlin/androidref');
fs.mkdirSync(jt, { recursive: true });
fs.writeFileSync(path.join(jt, 'MapFragmentRef.kt'), ref);

// Bağımsız dal sayımı: when dallarını Android metninden regex ile çıkar.
function dallar(a, b) {
  const ids = [];
  for (let i = a; i < b; i++) {
    const l = src[i];
    if (/^\s*\/\//.test(l)) continue;
    const m = /^ {16}(-?\d+(?:\s*,\s*-?\d+)*)\s*->/.exec(l);
    if (m) m[1].split(',').forEach(x => ids.push(parseInt(x.trim(), 10)));
    else if (/^ {16}\S/.test(l) && !/^ {16}(\)|\}|else ->|\/\/)/.test(l)) console.log('UYARI beklenmeyen satır', i + 1, l);
  }
  return ids;
}
const resolveBas = src.findIndex(l => l.includes('fun resolveLessonOperationsId'));
const o = dallar(bas, resolveBas), b = dallar(blind, son);
const dup = a => a.filter((x, i) => a.indexOf(x) !== i);
console.log('ops dal', o.length, 'tekrar', dup(o), '| blinding dal', b.length, 'tekrar', dup(b));
fs.mkdirSync(path.join(hedef, '..', 'tools'), { recursive: true });
fs.writeFileSync(path.join(hedef, '..', 'tools', 'dallar.json'), JSON.stringify({ o, b }));

const fs = require('fs');
const [build, out] = process.argv.slice(2);
const oku = f => fs.readFileSync(build + '/' + f, 'utf8').trim().split('\n');
const d = JSON.parse(fs.readFileSync(__dirname + '/dallar.json', 'utf8'));
const liste = (ad, a) => `val ${ad}: List<String> = listOf(\n${a.map(x => `    "${x}",`).join('\n')}\n)\n`;
const ints = (ad, a) => { const s = []; for (let i = 0; i < a.length; i += 20) s.push('    ' + a.slice(i, i + 20).join(', ') + ','); return `val ${ad}: List<Int> = listOf(\n${s.join('\n')}\n)\n`; };
fs.writeFileSync(out, `// iOS kopyasına özgü test verisi (Android'de karşılığı yok). ELLE DÜZENLEME.
//
// Nasıl üretildi (depo dışında, yalıtılmış kopyada):
//  1) Dal kimlikleri: app/src/main/java/com/example/app/MapFragment.kt içindeki iki \`when\` bloğunun
//     dalları, dönüştürülen koddan BAĞIMSIZ bir betikle (satır başı regex'i) Android metninden sayıldı.
//  2) Altın değerler: Android fonksiyon gövdeleri (getLessonOperations, getLessonOperationsBlinding,
//     resolve*) HİÇ değiştirilmeden JVM'de referans olarak derlendi ve tohumlu çalıştırıldı.
//     (Üreteçler ortak koddaki matematik paketidir; onların Android'le eşliği matematik/AltinVeri ile kanıtlı.)
//     Ayrıca aynı referansla 25 tohum × (-20..3000) kimlik için tam metin karşılaştırması yapıldı: fark yok.
//  3) Katalog özeti: ders kataloğundaki her item'ın adım kimlikleri için çözümleme sonucu (bkz. KatalogButunluk).
package com.sorobit.shared.dersadim

/** getLessonOperations \`when\` dalları, Android dosyasındaki sırayla (${d.o.length} dal). */
${ints('ANDROID_DALLAR_OPERATIONS', d.o)}
/** getLessonOperationsBlinding \`when\` dalları, Android dosyasındaki sırayla (${d.b.length} dal). */
${ints('ANDROID_DALLAR_BLINDING', d.b)}
/** Satır: \`tohum|tablo(O=abaküs,B=kör)|kimlik|adet|özet\` (özet: sonucun toString() metninin FNV-1a'sı). */
${liste('ALTIN_DERS_ADIM', oku('altin.txt'))}
/** Satır: \`istenen|min|resolveLessonOperationsId|resolveLessonOperationsBlindingId\`. */
${liste('ALTIN_RESOLVE', oku('resolve.txt'))}
/** Satır biçimi için bkz. [KatalogButunluk]. */
${liste('ALTIN_KATALOG_OZET', oku('katalogozet.txt'))}`);

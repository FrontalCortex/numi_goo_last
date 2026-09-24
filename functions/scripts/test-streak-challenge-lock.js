/**
 * Meydan okuma kilidi: "tur başında serbest, tur içinde kilitli" kuralının testi.
 *
 * Ödül dağıtan bir karar olduğu için elle okuyarak güvenmek yetmiyor. Kural
 * `functions/index.js` içindeki challengePatch; burada KAYNAKTAN çekilip
 * çalıştırılıyor, kopyası test edilmiyor.
 *
 * ÇALIŞTIRMA: cd functions && npm run test:challenge-lock
 */
const fs = require('fs');
const path = require('path');

const src = fs.readFileSync(path.join(__dirname, '..', 'index.js'), 'utf8');

function grabFunction(name) {
  const marker = `function ${name}(`;
  const i = src.indexOf(marker);
  if (i < 0) throw new Error(`kaynakta bulunamadı: ${name}`);
  let depth = 0;
  let started = false;
  for (let j = i; j < src.length; j++) {
    if (src[j] === '{') { depth++; started = true; }
    else if (src[j] === '}') { depth--; if (started && depth === 0) return src.slice(i, j + 1); }
  }
  throw new Error(`kapanmadı: ${name}`);
}

const challengePatch = new Function(
  `${grabFunction('challengePatch')}\nreturn challengePatch;`
)();

let pass = 0;
let fail = 0;
function check(name, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a === e) { console.log('  OK   ' + name); pass++; }
  else { console.log(`  HATA ${name}\n       beklenen ${e}\n       gelen    ${a}`); fail++; }
}

const fresh = { challengeDays: 0, challengeClaimed: 0 };
const run3 = { challengeDays: 3, challengeClaimed: 0 };
const claimed3 = { challengeDays: 3, challengeClaimed: 3 };

console.log('\n=== ILK SECIM ===');
check('yeni kullanici 1. gun yazar', challengePatch(1, 3, fresh), { challengeDays: 3, challengeClaimed: 0 });
check('gun yokken (0) yazar', challengePatch(0, 3, fresh), { challengeDays: 3, challengeClaimed: 0 });

console.log('\n=== TUR ICINDE KILITLI (asil somuru) ===');
check('3 gunluk seride 7ye gecis REDDEDILIR', challengePatch(3, 7, claimed3), {});
check('2. gunde degisiklik REDDEDILIR', challengePatch(2, 7, run3), {});
check('30. gunde degisiklik REDDEDILIR', challengePatch(30, 5, claimed3), {});

console.log('\n=== YENI TUR (seri kirildi, 1. gunden basliyor) ===');
check('odul alinmisken yeni tur sifirlar', challengePatch(1, 7, claimed3), { challengeDays: 7, challengeClaimed: 0 });
check('ayni gun sayisi secilse de odul hakki doner', challengePatch(1, 3, claimed3), { challengeDays: 3, challengeClaimed: 0 });

console.log('\n=== GEREKSIZ YAZIM ===');
check('degismeyen deger yazilmaz', challengePatch(1, 3, run3), {});

console.log('\n=== GECERSIZ GIRDI ===');
check('0 gun yazilmaz', challengePatch(1, 0, fresh), {});
check('negatif yazilmaz', challengePatch(1, -3, fresh), {});
check('400 ustu yazilmaz', challengePatch(1, 401, fresh), {});

console.log(`\n${pass} gecti, ${fail} kaldi`);
process.exit(fail > 0 ? 1 : 0);

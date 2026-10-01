/**
 * submitStreakDay: günsüz dalın kullandığı değişkenler ondan ÖNCE tanımlanmış mı.
 *
 * NEDEN VAR
 *   Bu üç tanım bir kez günsüz daldan SONRA duruyordu. `const` tanımlandığı satıra kadar
 *   "geçici ölü bölge"de olduğu için erişim ReferenceError atıyor ve Cloud Functions bunu
 *   INTERNAL diye döndürüyordu: günlük "buradayım" bildirimi HER SEFERİNDE çöküyordu.
 *
 *   Gün gönderen çağrılar çalıştığı için hata gözden kaçtı; istemcide yalnızca
 *   "submitStreakDay başarısız" satırı görünüyordu. Üstelik zararı bildirimle sınırlı
 *   kalmadı: her çöküş istemcide yeniden deneme penceresi açıyor ve o pencere GERÇEK gün
 *   gönderimlerini de geciktiriyordu.
 *
 * ÇALIŞTIRMA: node functions/scripts/test-streak-submit-order.js
 */
const fs = require('fs');
const path = require('path');

const src = fs.readFileSync(path.join(__dirname, '..', 'index.js'), 'utf8');

const start = src.indexOf('exports.submitStreakDay');
if (start < 0) throw new Error('index.js icinde submitStreakDay bulunamadi');
const end = src.indexOf('\nexports.', start + 1);
const body = src.slice(start, end < 0 ? src.length : end);

let passed = 0;
let failed = 0;
function check(name, fn) {
  try { fn(); passed++; console.log(`  OK   ${name}`); }
  catch (e) { failed++; console.error(`  HATA ${name} -> ${e.message}`); }
}

const branchIndex = body.indexOf('if (days.length === 0)');
check('gunsuz dal duruyor', () => {
  if (branchIndex < 0) throw new Error('`if (days.length === 0)` bulunamadi');
});

// Gunsuz dalin govdesi
const branchBody = body.slice(branchIndex, body.indexOf('\n  }', branchIndex));

// `todayNo` sonradan eklendi (seri dondurma) ve günsüz dal onu da okuyor.
for (const name of ['goalMinutes', 'challengeDays', 'utcOffsetMinutes', 'todayNo']) {
  check(`${name} gunsuz daldan ONCE tanimli`, () => {
    const decl = body.indexOf(`const ${name} =`);
    if (decl < 0) throw new Error(`\`const ${name} =\` bulunamadi`);
    if (decl > branchIndex) {
      throw new Error(
        `tanim ${decl}. karakterde, gunsuz dal ${branchIndex}. karakterde — ` +
        'dal bu degiskene erisirse ReferenceError atar (INTERNAL)'
      );
    }
  });
}

check('gunsuz dal challengePatch cagiriyor (kapsam korunuyor)', () => {
  if (!branchBody.includes('challengePatch(')) {
    throw new Error('gunsuz dal artik challengePatch cagirmiyor; test guncellenmeli');
  }
});

check('gunsuz dal reminderPatch cagiriyor (kapsam korunuyor)', () => {
  if (!branchBody.includes('reminderPatch(')) {
    throw new Error('gunsuz dal artik reminderPatch cagirmiyor; test guncellenmeli');
  }
});

console.log(`\n${passed} gecti, ${failed} basarisiz`);
process.exit(failed === 0 ? 0 : 1);

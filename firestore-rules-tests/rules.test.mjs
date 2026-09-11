/**
 * firestore.rules regresyon testleri.
 *
 * Ders basari oranlari, edinim kaynagi sayaci ve anket sik sayaclari Firestore'dan
 * Firebase Analytics'e tasindiginda guvenlik kurallari da daraltildi. Bu dosya iki seyi
 * birden dogrular:
 *
 *   1. KAPATILANLAR gercekten kapali mi — tasinan koleksiyonlara artik yazilamiyor.
 *   2. KORUNANLAR hala calisiyor mu — gunluk soru basari orani, anket serbest metni ve
 *      cift sayim koruma dokumani bozulmadi.
 *
 * Calistirmak icin (bir kez):  cd firestore-rules-tests && npm install
 * Sonra:                       npm test
 *
 * Emulator jar'ini ilk calistirmada indirir; internet gerekir.
 */
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment, assertFails, assertSucceeds,
} from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, increment, deleteField } from 'firebase/firestore';

const testEnv = await initializeTestEnvironment({
  projectId: 'demo-numigoo',
  firestore: {
    rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
    host: '127.0.0.1',
    port: 8087,
  },
});

const ali = testEnv.authenticatedContext('ali').firestore();
const veli = testEnv.authenticatedContext('veli').firestore();

let pass = 0;
let fail = 0;
async function check(name, promise) {
  try {
    await promise;
    console.log(`  PASS  ${name}`);
    pass++;
  } catch (e) {
    console.log(`  FAIL  ${name}\n        -> ${e.message.split('\n')[0]}`);
    fail++;
  }
}

console.log('\n-- KORUNMALI: gunluk soru basari orani (uygulama bunu OKUYOR) --');
await check('gunluk soru sayaci yazilabiliyor', assertSucceeds(
  setDoc(doc(ali, 'successRate/dailyQuestionSuccessRate/generatorForPart1Chest/1-2'),
    { easy_total: 1, easy_success: 1, easy_rate: 100 })));
await check('gunluk soru sayaci okunabiliyor', assertSucceeds(
  getDoc(doc(ali, 'successRate/dailyQuestionSuccessRate/generatorForPart1Chest/1-2'))));

console.log('\n-- KAPATILDI: ders basari orani kuresel sayaclari (Analytics e tasindi) --');
await check('lessonSuccessRate item yazimi reddediliyor', assertFails(
  setDoc(doc(ali, 'successRate/lessonSuccessRate/part1/3'), { passUserCount: 999999 })));
await check('lessonSuccessRate adim yazimi reddediliyor', assertFails(
  setDoc(doc(ali, 'successRate/lessonSuccessRate/part1/3/steps/1'), { successRatePercent: 100 })));
await check('lessonSuccessRate okumasi reddediliyor', assertFails(
  getDoc(doc(ali, 'successRate/lessonSuccessRate/part1/3'))));

console.log('\n-- KAPATILDI: edinim kaynagi sayaci (Analytics e tasindi) --');
await check('appStatistics yazimi reddediliyor', assertFails(
  setDoc(doc(ali, 'appStatistics/acquisition_sources'), { instagram: 50000 })));

console.log('\n-- KAPATILDI: anket sik sayaclari (Analytics e tasindi) --');
await check('questionPanel sik sayaci reddediliyor', assertFails(
  setDoc(doc(ali, 'questionPanel/1/5/question1'), { choice1: 99999 })));
await check('questionPanelTutorial sik sayaci reddediliyor', assertFails(
  setDoc(doc(ali, 'questionPanelTutorial/1/5/question1'), { choice1: 99999 })));

console.log('\n-- KORUNMALI: anket serbest metni (Firestore da kalir) --');
await check('kendi metnini yazabiliyor', assertSucceeds(
  setDoc(doc(ali, 'questionPanel/1/5/question1/text/ali'), { uid: 'ali', text: 'ders guzeldi' })));
await check('tutorial metnini yazabiliyor', assertSucceeds(
  setDoc(doc(ali, 'questionPanelTutorial/1/5/question3/text/ali'), { uid: 'ali', text: 'zor' })));
await check('baskasinin metnini yazamiyor', assertFails(
  setDoc(doc(veli, 'questionPanel/1/5/question1/text/ali'), { uid: 'ali', text: 'ezildi' })));
await check('metin okumasi reddediliyor (gizlilik)', assertFails(
  getDoc(doc(ali, 'questionPanel/1/5/question1/text/ali'))));

console.log('\n-- KORUNMALI: cift sayim koruma dokumani --');
await check('kendi lessonSuccessRateState yazimi', assertSucceeds(
  setDoc(doc(ali, 'users/ali/lessonSuccessRateState/1_3_1'),
    { attempted: true, passed: false, failStreak: 1 })));
await check('kendi lessonSuccessRateState okumasi', assertSucceeds(
  getDoc(doc(ali, 'users/ali/lessonSuccessRateState/1_3_1'))));
await check('baskasinin state dokumani reddediliyor', assertFails(
  getDoc(doc(veli, 'users/ali/lessonSuccessRateState/1_3_1'))));

console.log('\n-- VELI PANELI: gunluk calisma suresi (TimeTracker.DAILY_FIELD) --');
// Veli paneli gun->saniye haritasini users/{uid} uzerinde ALAN olarak okuyor; yazma
// totalTimeSpent ile ayni update cagrisina biniyor. Kural sunucu alanlarina bakiyor,
// dailyTimeSpent orada olmadigi icin gecmeli.
await testEnv.withSecurityRulesDisabled(async (ctx) => {
  await setDoc(doc(ctx.firestore(), 'users/ali'), {
    uid: 'ali', role: 'STUDENT', keys: 1, currency: 0, totalTimeSpent: 0,
  });
});
await check('gunluk sure alani yazilabiliyor', assertSucceeds(
  updateDoc(doc(ali, 'users/ali'), {
    totalTimeSpent: increment(120),
    'dailyTimeSpent.20260911': increment(120),
    'dailyTimeSpent.20260828': deleteField(),
  })));
await check('gunluk sure alani okunabiliyor', assertSucceeds(
  getDoc(doc(ali, 'users/ali'))));
await check('baskasinin gunluk suresi yazilamiyor', assertFails(
  updateDoc(doc(veli, 'users/ali'), { 'dailyTimeSpent.20260911': increment(120) })));
await check('gunluk sure yazimi cuzdani kurcalayamiyor', assertFails(
  updateDoc(doc(ali, 'users/ali'), {
    'dailyTimeSpent.20260911': increment(120),
    keys: increment(999),
  })));

console.log('\n-- REGRESYON: dokunulmayan yollar --');
await check('kendi lessonProgress yazimi', assertSucceeds(
  setDoc(doc(ali, 'users/ali/lessonProgress/1'), { x: 1 })));
await check('publicProfiles okumasi', assertSucceeds(
  getDoc(doc(ali, 'publicProfiles/veli'))));
await check('tanimsiz koleksiyon reddediliyor', assertFails(
  setDoc(doc(ali, 'rastgeleKoleksiyon/abc'), { x: 1 })));

await testEnv.cleanup();
console.log(`\n==== ${pass} gecti, ${fail} basarisiz ====`);
process.exit(fail === 0 ? 0 : 1);

/**
 * Odevler icin firestore.rules testleri (bkz. functions/homework.js).
 *
 * Odevi sinifin ogretmenleri ve - o sinifta oldugu surece - odevin verildigi ogrenci gormeli.
 * Ilerlemeyi yalnizca ogrenci, yalnizca bir adim ileri yazabilmeli. Uygulamanin yapacagi
 * SORGULARIN kabul edildigi de denetleniyor (kurallar sorguyu suzmez, reddeder).
 *
 * Calistirmak icin:  cd firestore-rules-tests && npm run test:homework
 */
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment, assertFails, assertSucceeds,
} from '@firebase/rules-unit-testing';
import {
  doc, setDoc, getDoc, getDocs, updateDoc, deleteDoc, collection, query, where,
} from 'firebase/firestore';

const testEnv = await initializeTestEnvironment({
  projectId: 'demo-numigoo-hw',
  firestore: {
    rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
    host: '127.0.0.1',
    port: 8087,
  },
});

const fs = (uid) => testEnv.authenticatedContext(uid).firestore();
const t1 = fs('t1'); // c1 sinifinin ogretmeni
const t2 = fs('t2'); // baska sinifin ogretmeni
const s1 = fs('s1'); // c1'de, hw-sel'e secili
const s2 = fs('s2'); // c1'de, secili degil
const s3 = fs('s3'); // sinifsiz

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

const config = { op: 'add', rules: ['five', 'ten'], withAbacus: true, digits: 2, numberCount: 5, intervalMs: 1500, questionCount: 25 };
await testEnv.withSecurityRulesDisabled(async (ctx) => {
  const db = ctx.firestore();
  await Promise.all([
    setDoc(doc(db, 'users/t1'), { uid: 't1', role: 'TEACHER', teacherApproved: true, courseId: 'k1' }),
    setDoc(doc(db, 'users/t2'), { uid: 't2', role: 'TEACHER', teacherApproved: true, courseId: 'k1' }),
    setDoc(doc(db, 'users/s1'), { uid: 's1', role: 'STUDENT', classId: 'c1', classTeacherUids: ['t1'] }),
    setDoc(doc(db, 'users/s2'), { uid: 's2', role: 'STUDENT', classId: 'c1', classTeacherUids: ['t1'] }),
    setDoc(doc(db, 'users/s3'), { uid: 's3', role: 'STUDENT' }),
    setDoc(doc(db, 'classes/c1'), { courseId: 'k1', name: 'Cmt', teacherUids: ['t1'] }),
    setDoc(doc(db, 'classes/c2'), { courseId: 'k1', name: 'Pzt', teacherUids: ['t2'] }),
    setDoc(doc(db, 'homeworks/hw-all'), { classId: 'c1', teacherUid: 't1', config, blockCount: 3, wholeClass: true, assignedUids: [], createdAtMs: 1 }),
    setDoc(doc(db, 'homeworks/hw-sel'), { classId: 'c1', teacherUid: 't1', config, blockCount: 3, wholeClass: false, assignedUids: ['s1'], createdAtMs: 2 }),
    setDoc(doc(db, 'homeworks/hw-other'), { classId: 'c2', teacherUid: 't2', config, blockCount: 3, wholeClass: true, assignedUids: [], createdAtMs: 3 }),
  ]);
});

const wholeClassQuery = (db, classId) => query(collection(db, 'homeworks'), where('classId', '==', classId), where('wholeClass', '==', true));
const assignedQuery = (db, classId, uid) => query(collection(db, 'homeworks'), where('classId', '==', classId), where('assignedUids', 'array-contains', uid));
const teacherQuery = (db, classId) => query(collection(db, 'homeworks'), where('classId', '==', classId));

console.log('\n-- OKUMA --');
await check('ogretmen sinifinin odevlerini sorgular', assertSucceeds(getDocs(teacherQuery(t1, 'c1'))));
await check('baska sinifin ogretmeni sorgulayamaz', assertFails(getDocs(teacherQuery(t2, 'c1'))));
await check('ogrenci sinifa verilen odevleri sorgular', assertSucceeds(getDocs(wholeClassQuery(s1, 'c1'))));
await check('ogrenci kendisine verilenleri sorgular', assertSucceeds(getDocs(assignedQuery(s1, 'c1', 's1'))));
const sel = await getDocs(assignedQuery(s2, 'c1', 's2'));
await check('secilmeyen ogrencinin listesi bos', sel.empty ? Promise.resolve() : Promise.reject(new Error('bos degil')));
await check('secilmeyen ogrenci secili odevi okuyamaz', assertFails(getDoc(doc(s2, 'homeworks/hw-sel'))));
await check('secili ogrenci okur', assertSucceeds(getDoc(doc(s1, 'homeworks/hw-sel'))));
await check('baska sinifin odevi okunamaz', assertFails(getDoc(doc(s1, 'homeworks/hw-other'))));
await check('sinifsiz ogrenci sinif odevini okuyamaz', assertFails(getDoc(doc(s3, 'homeworks/hw-all'))));
await check('baska sinifin kimligiyle sorgu RED', assertFails(getDocs(wholeClassQuery(s1, 'c2'))));
await check('filtresiz sorgu RED', assertFails(getDocs(collection(s1, 'homeworks'))));
await check('ogretmen de odev yazamaz (yalniz CF)', assertFails(setDoc(doc(t1, 'homeworks/x'), { classId: 'c1' })));
await check('ogretmen odevi silemez (CF ile)', assertFails(deleteDoc(doc(t1, 'homeworks/hw-all'))));

console.log('\n-- ILERLEME --');
const prog = (db, hw, uid) => doc(db, `homeworks/${hw}/studentProgress/${uid}`);
const step = (blocks, answered, correct, last) => ({ uid: 's1', completedBlocks: blocks, answered, correct, lastBlockCorrect: last, updatedAtMs: 1 });
await check('ilk adim yazilir', assertSucceeds(setDoc(prog(s1, 'hw-all', 's1'), step(1, 10, 7, 7))));
await check('bir adim ileri yazilir', assertSucceeds(updateDoc(prog(s1, 'hw-all', 's1'), step(2, 20, 15, 8))));
await check('adim atlanamaz', assertFails(updateDoc(prog(s1, 'hw-all', 's1'), step(4, 30, 20, 5))));
await check('geri gidilemez', assertFails(updateDoc(prog(s1, 'hw-all', 's1'), step(1, 10, 7, 7))));
await check('son adim (5 soru) yazilir', assertSucceeds(updateDoc(prog(s1, 'hw-all', 's1'), step(3, 25, 19, 4))));
await check('odev bitince fazla adim yazilamaz', assertFails(updateDoc(prog(s1, 'hw-all', 's1'), step(4, 25, 19, 0))));
await check('cevaplanandan fazla dogru RED', assertFails(setDoc(prog(s1, 'hw-sel', 's1'), step(1, 10, 11, 11))));
await check('adimda 10dan fazla soru RED', assertFails(setDoc(prog(s1, 'hw-sel', 's1'), step(1, 12, 5, 5))));
await check('bilinmeyen alan RED', assertFails(setDoc(prog(s1, 'hw-sel', 's1'), { ...step(1, 10, 5, 5), gold: 1000 })));
await check('baskasi adina yazilamaz', assertFails(setDoc(prog(s2, 'hw-all', 's1'), step(1, 10, 5, 5))));
await check('verilmeyen odeve ilerleme yazilamaz', assertFails(setDoc(prog(s2, 'hw-sel', 's2'), { ...step(1, 10, 5, 5), uid: 's2' })));
await check('ogretmen ilerlemeleri okur', assertSucceeds(getDocs(collection(t1, 'homeworks/hw-all/studentProgress'))));
await check('baska ogretmen okuyamaz', assertFails(getDocs(collection(t2, 'homeworks/hw-all/studentProgress'))));
await check('ogrenci kendi ilerlemesini okur', assertSucceeds(getDoc(prog(s1, 'hw-all', 's1'))));
await check('ogrenci baskasinin ilerlemesini okuyamaz', assertFails(getDoc(prog(s2, 'hw-all', 's1'))));
await check('ogretmen ilerleme yazamaz', assertFails(updateDoc(prog(t1, 'hw-all', 's1'), { correct: 25 })));
await check('kural sayimi ile adim yazilir', assertSucceeds(setDoc(prog(s1, 'hw-sel', 's1'),
  { ...step(1, 10, 6, 6), ruleTally: { add_five: { c: 6, w: 4 } } })));
await check('kural sayimi map degilse RED', assertFails(updateDoc(prog(s1, 'hw-sel', 's1'), { ...step(2, 20, 12, 6), ruleTally: 'x' })));

console.log('\n-- KURAL ISTATISTIGI --');
await testEnv.withSecurityRulesDisabled(async (ctx) => {
  await setDoc(doc(ctx.firestore(), 'homeworkRuleStats/s1'), { tally: { add_five: { c: 6, w: 4 } }, updatedAtMs: 1 });
});
await check('ogrenci kendi istatistigini okur', assertSucceeds(getDoc(doc(s1, 'homeworkRuleStats/s1'))));
await check('sinif ogretmeni okur', assertSucceeds(getDoc(doc(t1, 'homeworkRuleStats/s1'))));
await check('baska ogretmen okuyamaz', assertFails(getDoc(doc(t2, 'homeworkRuleStats/s1'))));
await check('baska ogrenci okuyamaz', assertFails(getDoc(doc(s2, 'homeworkRuleStats/s1'))));
await check('ogrenci istatistik yazamaz', assertFails(setDoc(doc(s1, 'homeworkRuleStats/s1'), { tally: {} })));

console.log('\n-- SINIFTAN AYRILAN --');
await testEnv.withSecurityRulesDisabled(async (ctx) => {
  await updateDoc(doc(ctx.firestore(), 'users/s1'), { classId: null, classTeacherUids: [] });
});
await check('ayrilan ogrenci secili oldugu odevi artik okuyamaz', assertFails(getDoc(doc(s1, 'homeworks/hw-sel'))));
await check('ayrilan ogrenci ilerleme yazamaz', assertFails(setDoc(prog(s1, 'hw-sel', 's1'), step(1, 10, 5, 5))));

await testEnv.cleanup();
console.log(`\n==== ${pass} gecti, ${fail} basarisiz ====`);
process.exit(fail === 0 ? 0 : 1);

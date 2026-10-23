/**
 * Soru havuzu ayrimi ve kurs siniflari icin firestore.rules testleri.
 *
 * Kurs ogretmeni yalnizca kendi sinifinin sorularini, kursu olmayan ogretmen yalnizca genel
 * havuzu gormeli (bkz. functions/classes.js). Bu ayrim KURALLARDA yasiyor: uygulama sorgusu
 * yanlis kurulsa ya da degistirilmis bir istemci butun sorulari istese bile baska bir
 * kursun cocugunun sorusu okunamamali.
 *
 * Kurallar sorguyu suzmez: sorgu izinsiz tek bir dokumana carpabiliyorsa TAMAMEN reddedilir.
 * Bu yuzden burada tek dokuman okumalarinin yaninda uygulamanin yapacagi SORGULARIN da
 * kabul edildigi dogrulaniyor — yanlis kurulmus bir kural, uygulamada bos bir havuz olarak
 * gorunur ve hata vermez.
 *
 * Calistirmak icin:  cd firestore-rules-tests && npm run test:pools
 */
import { readFileSync } from 'node:fs';
import {
  initializeTestEnvironment, assertFails, assertSucceeds,
} from '@firebase/rules-unit-testing';
import {
  doc, setDoc, getDoc, getDocs, updateDoc, collection, query, where, orderBy,
} from 'firebase/firestore';

const testEnv = await initializeTestEnvironment({
  projectId: 'demo-numigoo',
  firestore: {
    rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
    host: '127.0.0.1',
    port: 8087,
  },
});

const fs = (uid) => testEnv.authenticatedContext(uid).firestore();
const t1 = fs('t1');   // kurs ogretmeni, c1 sinifinin ogretmeni
const t2 = fs('t2');   // ayni kursun ogretmeni, genel havuza da yardim ediyor, c1'de degil
const g1 = fs('g1');   // kursu olmayan ogretmen
const s1 = fs('s1');   // c1 sinifinda ogrenci
const s2 = fs('s2');   // sinifsiz ogrenci

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

const now = new Date();
await testEnv.withSecurityRulesDisabled(async (ctx) => {
  const db = ctx.firestore();
  await Promise.all([
    setDoc(doc(db, 'users/t1'), { uid: 't1', role: 'TEACHER', teacherApproved: true, courseId: 'k1' }),
    setDoc(doc(db, 'users/t2'), { uid: 't2', role: 'TEACHER', teacherApproved: true, courseId: 'k1', helpsGeneralPool: true }),
    setDoc(doc(db, 'users/g1'), { uid: 'g1', role: 'TEACHER', teacherApproved: true }),
    setDoc(doc(db, 'users/s1'), { uid: 's1', role: 'STUDENT', classId: 'c1', courseId: 'k1', classTeacherUids: ['t1'] }),
    setDoc(doc(db, 'users/s2'), { uid: 's2', role: 'STUDENT' }),
    setDoc(doc(db, 'questions/qc'), { studentUid: 's1', status: 'pending', createdAt: now, pool: 'class', classId: 'c1', courseId: 'k1', classTeacherUids: ['t1'] }),
    setDoc(doc(db, 'questions/qg'), { studentUid: 's2', status: 'pending', createdAt: now, pool: 'general', classId: null, courseId: null, classTeacherUids: [] }),
    setDoc(doc(db, 'questions/qg2'), { studentUid: 's2', status: 'pending', createdAt: now, pool: 'general', classId: null, courseId: null, classTeacherUids: [] }),
    // Bu ozellikten once sorulmus, `pool` alani olmayan soru.
    setDoc(doc(db, 'questions/qlegacy'), { studentUid: 's2', status: 'claimed', createdAt: now, claimedByTeacherUid: 'g1' }),
    setDoc(doc(db, 'questions/qold'), { studentUid: 's2', status: 'pending', createdAt: now }),
    setDoc(doc(db, 'questions/qlegacy2'), { studentUid: 's2', status: 'pending', createdAt: now }),
    setDoc(doc(db, 'questions/qc/messages/m1'), { senderUid: 's1', textContent: 'x' }),
    setDoc(doc(db, 'courses/k1'), { name: 'Kurs' }),
    setDoc(doc(db, 'classes/c1'), { courseId: 'k1', name: 'Cmt', teacherUids: ['t1'], joinCode: 'ABC234', studentCount: 1 }),
    setDoc(doc(db, 'classes/c1/members/s1'), { uid: 's1', name: 'Ali' }),
    setDoc(doc(db, 'classJoinCodes/ABC234'), { classId: 'c1' }),
    setDoc(doc(db, 'classInvites/c1_s2'), { classId: 'c1', teacherUid: 't1', studentUid: 's2', status: 'pending' }),
    setDoc(doc(db, 'users/s1/lessonSuccessRateState/1_x_1'), { attempted: true }),
    setDoc(doc(db, 'users/s2/lessonSuccessRateState/1_x_1'), { attempted: true }),
  ]);
});

const pendingAll = (db) => query(collection(db, 'questions'), where('status', '==', 'pending'), orderBy('createdAt', 'desc'));
const generalPool = (db) => query(collection(db, 'questions'),
  where('pool', '==', 'general'), where('status', '==', 'pending'), orderBy('createdAt', 'desc'));
const classPool = (db, uid) => query(collection(db, 'questions'),
  where('classTeacherUids', 'array-contains', uid), where('status', '==', 'pending'), orderBy('createdAt', 'desc'));
const myChats = (db, uid) => query(collection(db, 'questions'), where('claimedByTeacherUid', '==', uid), orderBy('createdAt', 'desc'));

console.log('\n-- AYRIM KAPALI (gecis donemi: eski ogretmen surumu calismali) --');
await check('eski surum havuz sorgusu (filtresiz) kabul', assertSucceeds(getDocs(pendingAll(g1))));
await check('genel ogretmen sinif sorusunu OKUYABILIR (okuma acik)', assertSucceeds(getDoc(doc(g1, 'questions/qc'))));
await check('ama SAHIPLENEMEZ (yazma her durumda ayrimda)', assertFails(
  updateDoc(doc(g1, 'questions/qc'), { status: 'claimed', claimedByTeacherUid: 'g1' })));
await check('kurs ogretmeni genel soruyu sahiplenemez', assertFails(
  updateDoc(doc(t1, 'questions/qg'), { status: 'claimed', claimedByTeacherUid: 't1' })));
await check('genel ogretmen genel soruyu sahiplenir', assertSucceeds(
  updateDoc(doc(g1, 'questions/qg'), { status: 'claimed', claimedByTeacherUid: 'g1' })));
await check('sahiplenilmis soru ikinci kez sahiplenilemez (yaris)', assertFails(
  updateDoc(doc(t2, 'questions/qg'), { status: 'claimed', claimedByTeacherUid: 't2' })));
await check('sahiplenen ogretmen soruyu cozebilir', assertSucceeds(
  updateDoc(doc(g1, 'questions/qg'), { status: 'resolved' })));
await check('baskasi adina sahiplenme reddediliyor', assertFails(
  updateDoc(doc(g1, 'questions/qg2'), { status: 'claimed', claimedByTeacherUid: 'g9' })));
await check('sinifin ogretmeni sinif sorusunu sahiplenir', assertSucceeds(
  updateDoc(doc(t1, 'questions/qc'), { status: 'claimed', claimedByTeacherUid: 't1' })));
await testEnv.withSecurityRulesDisabled(async (ctx) => {
  // Sorgu testleri icin qc'yi tekrar bekleyen yap.
  await updateDoc(doc(ctx.firestore(), 'questions/qc'), { status: 'pending', claimedByTeacherUid: null });
});

console.log('\n-- AYRIM ACIK --');
await testEnv.withSecurityRulesDisabled(async (ctx) => {
  await setDoc(doc(ctx.firestore(), 'system/questionPools'), { enforced: true });
});
await check('filtresiz havuz sorgusu artik RED', assertFails(getDocs(pendingAll(g1))));
await check('genel ogretmen: genel havuz sorgusu kabul', assertSucceeds(getDocs(generalPool(g1))));
await check('kurs ogretmeni: genel havuz sorgusu RED', assertFails(getDocs(generalPool(t1))));
await check('yardim eden kurs ogretmeni: genel havuz kabul', assertSucceeds(getDocs(generalPool(t2))));
await check('kurs ogretmeni: kendi sinif havuzu kabul', assertSucceeds(getDocs(classPool(t1, 't1'))));
const t1Pool = await getDocs(classPool(t1, 't1'));
await check('sinif havuzunda yalnizca kendi sorusu', t1Pool.docs.map((d) => d.id).join() === 'qc'
  ? Promise.resolve() : Promise.reject(new Error(`gelen: ${t1Pool.docs.map((d) => d.id).join()}`)));
await check('baskasinin uid si ile sinif havuzu RED', assertFails(getDocs(classPool(g1, 't1'))));
await check('sohbetler sorgusu (sahiplendiklerim) kabul', assertSucceeds(getDocs(myChats(g1, 'g1'))));
await check('sinif sorusu: sinifin ogretmeni okur', assertSucceeds(getDoc(doc(t1, 'questions/qc'))));
await check('sinif sorusu: genel ogretmen OKUYAMAZ', assertFails(getDoc(doc(g1, 'questions/qc'))));
await check('sinif sorusu: ayni kursun baska ogretmeni OKUYAMAZ', assertFails(getDoc(doc(t2, 'questions/qc'))));
await check('sinif sorusu mesajlari: genel ogretmen OKUYAMAZ', assertFails(getDoc(doc(g1, 'questions/qc/messages/m1'))));
await check('sinif sorusu mesajlari: sinifin ogretmeni okur', assertSucceeds(getDoc(doc(t1, 'questions/qc/messages/m1'))));
await check('pool alani olmayan, sahiplendigi eski soru okunur', assertSucceeds(getDoc(doc(g1, 'questions/qlegacy'))));
// Okumada `pool` alani olmayan soru genel SAYILMIYOR (kuraldaki teacherReaches yorumu):
// varsayilanli yazim sorgu kanitinda gevsekti.
await check('pool alani olmayan, sahiplenilmemis eski soru okunamaz', assertFails(getDoc(doc(g1, 'questions/qold'))));
await check('pool alani olmayan eski soru kurs ogretmenine de kapali', assertFails(getDoc(doc(t1, 'questions/qold'))));
await check('ama genel ogretmen onu sahiplenebilir (yazma tek dokuman)', assertSucceeds(
  updateDoc(doc(g1, 'questions/qold'), { status: 'claimed', claimedByTeacherUid: 'g1' })));
await check('kurs ogretmeni pool alani olmayan soruyu sahiplenemez', assertFails(
  updateDoc(doc(t1, 'questions/qlegacy2'), { status: 'claimed', claimedByTeacherUid: 't1' })));
await check('ogrenci kendi sorusunu okur', assertSucceeds(getDoc(doc(s1, 'questions/qc'))));

console.log('\n-- HAVUZ ALANLARI DEGISTIRILEMEZ --');
await check('ogrenci sorusunun havuzunu degistiremez', assertFails(updateDoc(doc(s2, 'questions/qg2'), { pool: 'class' })));
await check('ogrenci ogretmen listesini degistiremez', assertFails(
  updateDoc(doc(s1, 'questions/qc'), { classTeacherUids: ['g1'] })));
await check('ogretmen soruyu baska havuza tasiyamaz', assertFails(updateDoc(doc(t1, 'questions/qc'), { pool: 'general' })));
await check('ogrenci okundu/sil alani gibi serbest alanlari yazabilir', assertSucceeds(
  updateDoc(doc(s1, 'questions/qc'), { deletedForUids: ['s1'] })));

console.log('\n-- KULLANICI DOKUMANI: UYELIK ALANLARI SUNUCUNUN --');
await check('ogrenci kendine sinif yazamaz', assertFails(updateDoc(doc(s2, 'users/s2'), { classId: 'c1' })));
await check('ogrenci ogretmen listesi yazamaz', assertFails(updateDoc(doc(s1, 'users/s1'), { classTeacherUids: ['g1'] })));
await check('ogrenci uyeligini silemez (cikis sunucudan)', assertFails(updateDoc(doc(s1, 'users/s1'), { classId: null })));
await check('ogretmen kendini kursa baglayamaz', assertFails(updateDoc(doc(g1, 'users/g1'), { courseId: 'k1' })));
await check('ogretmen genel havuz bayragini acabilir', assertSucceeds(updateDoc(doc(t1, 'users/t1'), { helpsGeneralPool: true })));
await check('kayitta sinif alani reddediliyor', assertFails(
  setDoc(doc(fs('yeni'), 'users/yeni'), { uid: 'yeni', role: 'STUDENT', keys: 1, currency: 0, classId: 'c1' })));
await check('kayitta kurs alani reddediliyor', assertFails(
  setDoc(doc(fs('yeni2'), 'users/yeni2'), { uid: 'yeni2', role: 'TEACHER', keys: 1, currency: 0, courseId: 'k1' })));
await check('kayit (alanlar olmadan) calisiyor', assertSucceeds(
  setDoc(doc(fs('yeni3'), 'users/yeni3'), { uid: 'yeni3', role: 'STUDENT', keys: 1, currency: 0 })));

console.log('\n-- OGRENCI ILERLEMESI (lessonSuccessRateState) --');
await check('sinifin ogretmeni okur', assertSucceeds(getDocs(collection(t1, 'users/s1/lessonSuccessRateState'))));
await check('genel ogretmen okuyamaz', assertFails(getDocs(collection(g1, 'users/s1/lessonSuccessRateState'))));
await check('baska ogrenci okuyamaz', assertFails(getDocs(collection(s2, 'users/s1/lessonSuccessRateState'))));
await check('ogrenci kendisininkini okur', assertSucceeds(getDocs(collection(s1, 'users/s1/lessonSuccessRateState'))));
await check('ogretmen sinifinda olmayan ogrenciyi okuyamaz', assertFails(getDocs(collection(t1, 'users/s2/lessonSuccessRateState'))));
await check('ogretmen yazamaz', assertFails(setDoc(doc(t1, 'users/s1/lessonSuccessRateState/1_x_1'), { attempted: false })));

console.log('\n-- SINIFLAR --');
await check('ogretmen kendi sinifini okur', assertSucceeds(getDoc(doc(t1, 'classes/c1'))));
await check('ogretmen siniflarini sorgular', assertSucceeds(
  getDocs(query(collection(t1, 'classes'), where('teacherUids', 'array-contains', 't1')))));
await check('siniftaki ogrenci sinifi okur', assertSucceeds(getDoc(doc(s1, 'classes/c1'))));
await check('sinif disi ogrenci okuyamaz', assertFails(getDoc(doc(s2, 'classes/c1'))));
await check('baska ogretmen okuyamaz', assertFails(getDoc(doc(t2, 'classes/c1'))));
await check('ogretmen sinifa yazamaz (yalniz CF)', assertFails(updateDoc(doc(t1, 'classes/c1'), { studentCount: 99 })));
await check('ogretmen ogrenci listesini okur', assertSucceeds(getDocs(collection(t1, 'classes/c1/members'))));
await check('siniftaki ogrenci sinif arkadaslarini okur', assertSucceeds(getDocs(collection(s1, 'classes/c1/members'))));
await check('sinif disi ogrenci listeyi okuyamaz', assertFails(getDocs(collection(s2, 'classes/c1/members'))));
await check('baska sinifin ogretmeni listeyi okuyamaz', assertFails(getDocs(collection(t2, 'classes/c1/members'))));
await check('ogretmen listeye yazamaz', assertFails(setDoc(doc(t1, 'classes/c1/members/s2'), { uid: 's2' })));
await check('kod eslemesi okunamaz', assertFails(getDoc(doc(s2, 'classJoinCodes/ABC234'))));
await check('kod eslemesi ogretmene de kapali', assertFails(getDoc(doc(t1, 'classJoinCodes/ABC234'))));
await check('kurs okunur', assertSucceeds(getDoc(doc(s2, 'courses/k1'))));
await check('kurs yazilamaz', assertFails(setDoc(doc(t1, 'courses/k2'), { name: 'x' })));

console.log('\n-- DAVETLER --');
await check('davetli ogrenci davetlerini sorgular', assertSucceeds(
  getDocs(query(collection(s2, 'classInvites'), where('studentUid', '==', 's2'), where('status', '==', 'pending')))));
await check('gonderen ogretmen okur', assertSucceeds(getDoc(doc(t1, 'classInvites/c1_s2'))));
await check('baska ogrenci okuyamaz', assertFails(getDoc(doc(s1, 'classInvites/c1_s2'))));
await check('ogrenci daveti kendisi kabul edemez (yalniz CF)', assertFails(
  updateDoc(doc(s2, 'classInvites/c1_s2'), { status: 'accepted' })));

await testEnv.cleanup();
console.log(`\n==== ${pass} gecti, ${fail} basarisiz ====`);
process.exit(fail === 0 ? 0 : 1);

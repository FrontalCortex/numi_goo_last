/**
 * Kurs sınıfları: GERÇEK fonksiyonlar Firestore emülatörüne karşı.
 *
 * NEDEN VAR
 *   test-classes.js kararların kendisini deniyor; burada gövdeler: transaction'daki okuma/
 *   yazma sırası, üyeliğin iki kopyasının (kullanıcı dokümanı + members) birlikte değişmesi,
 *   bekleyen soruların yeni havuza taşınması ve askTeacherQuestion'ın havuz alanları.
 *
 * ÇALIŞTIRMA (proje kökünden)
 *   firebase emulators:exec --only firestore --project numigo-new "node functions/scripts/test-class-flow.js"
 */
const admin = require('firebase-admin');

if (!process.env.FIRESTORE_EMULATOR_HOST && process.env.ALLOW_PRODUCTION !== '1') {
  console.error('FIRESTORE_EMULATOR_HOST ayarlı değil; çıkılıyor.');
  process.exit(1);
}
// test-closed-question-cleanup.js ile aynı gerekçe: emulators:exec ortama gerçek proje
// kimliğini koyuyor; test yalnızca Firestore emülatörüne dokunmalı.
delete process.env.FIREBASE_CONFIG;
delete process.env.GOOGLE_APPLICATION_CREDENTIALS;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'numigo-new';

const fns = require('../index');
if (!admin.apps.length) admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT });
const db = admin.firestore();

// Emülatörde başka testlerin verisi olabilir; bu testin kimlikleri benzersiz.
const S = `cf${Date.now().toString(36)}`;
const T1 = `${S}_t1`; // kurs öğretmeni
const T2 = `${S}_t2`; // aynı kursun başka öğretmeni
const G1 = `${S}_g1`; // kursu olmayan öğretmen
const S1 = `${S}_s1`;
const S2 = `${S}_s2`;
const COURSE = `${S}_kurs`;
// Gerçek kullanıcı adları gibi rakam + büyük harf (generateUniqueUserId); sonradan değiştirilemiyor.
const ID = S.toUpperCase();
const ALI_ID = `${ID}ALI`;
const VELI_ID = `${ID}VELI`;
const GENEL_ID = `${ID}GENEL`;

const ctx = (uid) => ({ auth: { uid, token: {} } });
const call = (fn, uid, data) => fns[fn].run(data || {}, ctx(uid));
const user = async (uid) => (await db.collection('users').doc(uid).get()).data() || {};
const getDoc = async (p) => {
  const s = await db.doc(p).get();
  return s.exists ? s.data() : null;
};

let pass = 0;
let fail = 0;
function check(name, actual, expected) {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a === e) { console.log('  OK   ' + name); pass++; }
  else { console.log(`  HATA ${name}\n       beklenen ${e}\n       gelen    ${a}`); fail++; }
}
async function rejects(name, promise, code) {
  try {
    await promise;
    console.log(`  HATA ${name}\n       beklenen red: ${code}\n       gelen    basari`);
    fail++;
  } catch (e) {
    check(name, e.code, code);
  }
}

async function ask(uid) {
  const r = await call('askTeacherQuestion', uid, {
    storagePath: `question_screenshots/${uid}_1.jpg`,
    mediaUrl: 'https://example.invalid/x.jpg',
    message: 'soru',
  });
  return (await db.collection('questions').doc(r.questionId).get()).data();
}

async function main() {
  await db.collection('courses').doc(COURSE).set({ name: 'Deneme Kursu' });
  await Promise.all([
    db.collection('users').doc(T1).set({ role: 'TEACHER', teacherApproved: true, courseId: COURSE, name: 'Ayşe Hoca' }),
    db.collection('users').doc(T2).set({ role: 'TEACHER', teacherApproved: true, courseId: COURSE, name: 'Mehmet Hoca' }),
    db.collection('users').doc(G1).set({ role: 'TEACHER', teacherApproved: true, name: 'Genel Hoca' }),
    db.collection('users').doc(S1).set({ role: 'STUDENT', userId: ALI_ID, name: 'Ali', questionCredits: 10 }),
    db.collection('users').doc(S2).set({ role: 'STUDENT', userId: VELI_ID, name: 'Veli', questionCredits: 10 }),
  ]);

  console.log('\n=== SINIF AÇMA ===');
  await rejects('kursu olmayan öğretmen RED', call('createClass', G1, { name: 'Sınıf' }), 'permission-denied');
  await rejects('öğrenci RED', call('createClass', S1, { name: 'Sınıf' }), 'permission-denied');
  await rejects('oturum yok RED', fns.createClass.run({ name: 'Sınıf' }, {}), 'unauthenticated');
  const created = await call('createClass', T1, { name: '  Cmt  10:00 ' });
  const CLS = created.classId;
  check('ad temizlendi', created.name, 'Cmt 10:00');
  const cls = await getDoc(`classes/${CLS}`);
  check('sınıf dokümanı', [cls.courseId, cls.courseName, cls.teacherUids, cls.studentCount, cls.archived, cls.joinCode],
    [COURSE, 'Deneme Kursu', [T1], 0, false, created.joinCode]);
  check('kod eşlemesi', (await getDoc(`classJoinCodes/${created.joinCode}`)).classId, CLS);

  console.log('\n=== SORU HAVUZU (katılmadan önce) ===');
  const before = await ask(S1);
  check('sınıfsız öğrencinin sorusu genel', [before.pool, before.classId, before.classTeacherUids], ['general', null, []]);

  console.log('\n=== KODLA KATILMA ===');
  await rejects('yanlış kod RED', call('joinClassByCode', S1, { code: 'ZZZZZZ' }), 'not-found');
  await rejects('biçimsiz kod RED (sayılmaz)', call('joinClassByCode', S1, { code: '12' }), 'invalid-argument');
  check('yanlış kod sayıldı', (await getDoc(`classJoinGuards/${S1}`)).wrong, 1);
  await rejects('öğretmen kodla katılamaz', call('joinClassByCode', G1, { code: created.joinCode }), 'failed-precondition');
  const joined = await call('joinClassByCode', S1, { code: created.joinCode.toLowerCase() });
  check('yanıt', [joined.classId, joined.className, joined.courseName, joined.alreadyMember], [CLS, 'Cmt 10:00', 'Deneme Kursu', false]);
  const u1 = await user(S1);
  check('üyelik kullanıcı dokümanında', [u1.classId, u1.courseId, u1.classTeacherUids], [CLS, COURSE, [T1]]);
  check('members dokümanı', (await getDoc(`classes/${CLS}/members/${S1}`)).name, 'Ali');
  check('öğrenci sayısı 1', (await getDoc(`classes/${CLS}`)).studentCount, 1);
  const retagged = (await db.collection('questions').where('studentUid', '==', S1).get()).docs.map((d) => d.data());
  check('bekleyen eski soru sınıfa taşındı', retagged.map((q) => [q.pool, q.classTeacherUids]), [['class', [T1]]]);
  const again = await call('joinClassByCode', S1, { code: created.joinCode });
  check('tekrar katılma: zaten üye, sayı artmadı', [again.alreadyMember, (await getDoc(`classes/${CLS}`)).studentCount], [true, 1]);

  console.log('\n=== SORU HAVUZU (katıldıktan sonra) ===');
  const inClass = await ask(S1);
  check('sınıf sorusu', [inClass.pool, inClass.classId, inClass.courseId, inClass.classTeacherUids], ['class', CLS, COURSE, [T1]]);
  const general = await ask(S2);
  check('sınıfsız öğrenci genelde kaldı', general.pool, 'general');

  console.log('\n=== DAVET ===');
  await rejects('başka öğretmen davet edemez', call('inviteStudentToClass', T2, { classId: CLS, userId: VELI_ID }), 'permission-denied');
  await rejects('olmayan ad', call('inviteStudentToClass', T1, { classId: CLS, userId: `${ID}YOK` }), 'not-found');
  await rejects('öğretmen davet edilemez', (async () => {
    await db.collection('users').doc(G1).update({ userId: GENEL_ID });
    return call('inviteStudentToClass', T1, { classId: CLS, userId: GENEL_ID });
  })(), 'failed-precondition');
  const inv = await call('inviteStudentToClass', T1, { classId: CLS, userId: `@${VELI_ID.toLowerCase()}` });
  check('harf farkıyla bulundu', [inv.studentUid, inv.studentName, inv.alreadyMember], [S2, 'Veli', false]);
  const invDoc = await getDoc(`classInvites/${inv.inviteId}`);
  check('davet dokümanı', [invDoc.status, invDoc.teacherName, invDoc.className, invDoc.studentUid], ['pending', 'Ayşe Hoca', 'Cmt 10:00', S2]);
  check('davet kabul edilmeden üyelik yok', (await user(S2)).classId, undefined);
  await rejects('başkası daveti yanıtlayamaz', call('respondClassInvite', S1, { inviteId: inv.inviteId, accept: true }), 'not-found');
  const acc = await call('respondClassInvite', S2, { inviteId: inv.inviteId, accept: true });
  check('kabul', [acc.accepted, acc.classId], [true, CLS]);
  check('davet kabul edildi', (await getDoc(`classInvites/${inv.inviteId}`)).status, 'accepted');
  check('öğrenci sayısı 2', (await getDoc(`classes/${CLS}`)).studentCount, 2);
  check('S2 bekleyen sorusu sınıfa taşındı', (await db.collection('questions').where('studentUid', '==', S2).get()).docs.map((d) => d.get('pool')), ['class']);
  await rejects('yanıtlanmış davet tekrar kullanılamaz', call('respondClassInvite', S2, { inviteId: inv.inviteId, accept: true }), 'failed-precondition');
  const member = await call('inviteStudentToClass', T1, { classId: CLS, userId: VELI_ID });
  check('üyeyi davet: zaten üye', member.alreadyMember, true);

  console.log('\n=== DAVET RED ===');
  const created2 = await call('createClass', T2, { name: 'Pzt 16:00' });
  const inv2 = await call('inviteStudentToClass', T2, { classId: created2.classId, userId: ALI_ID });
  const dec = await call('respondClassInvite', S1, { inviteId: inv2.inviteId, accept: false });
  check('red', [dec.accepted, (await getDoc(`classInvites/${inv2.inviteId}`)).status, (await user(S1)).classId], [false, 'declined', CLS]);

  console.log('\n=== SINIF DEĞİŞTİRME ===');
  const moved = await call('joinClassByCode', S1, { code: created2.joinCode });
  check('yeni sınıf', moved.classId, created2.classId);
  check('eski sınıftan çıktı', [await getDoc(`classes/${CLS}/members/${S1}`), (await getDoc(`classes/${CLS}`)).studentCount], [null, 1]);
  check('öğretmen listesi yeni sınıfın', (await user(S1)).classTeacherUids, [T2]);
  const pendingS1 = (await db.collection('questions').where('studentUid', '==', S1).where('status', '==', 'pending').get())
    .docs.map((d) => d.get('classTeacherUids'));
  check('bekleyen sorular yeni öğretmene', pendingS1, [[T2], [T2]]);

  console.log('\n=== KOD YENİLEME ===');
  await rejects('başkası yenileyemez', call('regenerateClassCode', T2, { classId: CLS }), 'permission-denied');
  const regen = await call('regenerateClassCode', T1, { classId: CLS });
  check('eski kod silindi', await getDoc(`classJoinCodes/${created.joinCode}`), null);
  check('yeni kod sınıfta', (await getDoc(`classes/${CLS}`)).joinCode, regen.joinCode);
  await rejects('eski kodla katılınamaz', call('joinClassByCode', S2, { code: created.joinCode }), 'not-found');

  console.log('\n=== ÇIKARMA / AYRILMA ===');
  await rejects('başka öğretmen çıkaramaz', call('removeStudentFromClass', T2, { classId: CLS, studentUid: S2 }), 'permission-denied');
  const rm = await call('removeStudentFromClass', T1, { classId: CLS, studentUid: S2 });
  const u2 = await user(S2);
  check('çıkarıldı, üyelik alanları silindi', [rm.left, u2.classId, u2.courseId, u2.classTeacherUids, u2.classJoinedAt], [true, undefined, undefined, undefined, undefined]);
  check('sayı 0', (await getDoc(`classes/${CLS}`)).studentCount, 0);
  check('S2 bekleyen sorusu genele döndü', (await db.collection('questions').where('studentUid', '==', S2).get()).docs.map((d) => d.get('pool')), ['general']);
  const rm2 = await call('removeStudentFromClass', T1, { classId: CLS, studentUid: S2 });
  check('ikinci çıkarma: değişiklik yok', [rm2.left, (await getDoc(`classes/${CLS}`)).studentCount], [false, 0]);
  const lv = await call('leaveMyClass', S1);
  check('öğrenci ayrıldı', [lv.left, (await user(S1)).classId, (await getDoc(`classes/${created2.classId}`)).studentCount], [true, undefined, 0]);

  console.log('\n=== TEKRAR SOR (havuz şimdiki üyelikten) ===');
  const qid = (await db.collection('questions').where('studentUid', '==', S2).limit(1).get()).docs[0].id;
  await db.collection('questions').doc(qid).update({ status: 'expired' });
  await call('joinClassByCode', S2, { code: regen.joinCode });
  await call('resendTeacherQuestion', S2, { questionId: qid });
  const resent = await getDoc(`questions/${qid}`);
  check('tekrar sorulan soru yeni sınıfa', [resent.status, resent.pool, resent.classTeacherUids], ['pending', 'class', [T1]]);

  console.log('\n=== KAPANMIŞ SINIF ===');
  await db.collection('classes').doc(CLS).update({ archived: true });
  check('kapanmış sınıfın öğrencisinin sorusu genele', (await ask(S2)).pool, 'general');
  await db.collection('classes').doc(CLS).update({ archived: false });

  console.log('\n=== YANLIŞ KOD SINIRI ===');
  for (let i = 0; i < 10; i++) {
    await call('joinClassByCode', S1, { code: 'ZZZZZZ' }).catch(() => {});
  }
  await rejects('sınır dolunca doğru kod da RED', call('joinClassByCode', S1, { code: regen.joinCode }), 'resource-exhausted');

  console.log('\n=== HAVUZ BİLDİRİMİ ===');
  // İmleci şimdiye kur, sonra iki yeni soru: biri sınıfta (S2 → T1), biri genel (S1).
  await db.doc('system/teacherPoolNotify').set({ lastScanMs: Date.now() - 1 });
  await new Promise((r) => setTimeout(r, 5));
  await ask(S2);
  await ask(S1);
  const notify = await fns._runTeacherPoolNotify(Date.now() + 1000);
  // Emülatörde başka testlerin öğretmenleri de olabilir; yalnızca bizimkilere bakılıyor.
  // Hiçbirinin bildirim token'ı yok: "gönderilecek" olanlar skipped, olmayanlar notMine.
  check('tarama iki yeni soru gördü', notify.newCount >= 2, true);
  check('T2 (yeni sorusu olmayan kurs öğretmeni) atlandı', notify.notMine >= 1, true);

  console.log('\n=== HESAP SİLME ===');
  await call('joinClassByCode', S1, { code: created2.joinCode }).catch(() => {});
  await db.collection('classJoinGuards').doc(S1).delete();
  await call('joinClassByCode', S1, { code: created2.joinCode });
  const del = await fns._removeDeletedUserFromClasses(S1);
  check('üyelik ve davet temizlendi', [del.memberships, del.invites >= 1], [1, true]);
  check('sayı düştü', (await getDoc(`classes/${created2.classId}`)).studentCount, 0);
  const delT = await fns._removeDeletedUserFromClasses(T2);
  check('öğretmen silinince sınıfı kapandı', [delT.teacherClasses, (await getDoc(`classes/${created2.classId}`)).archived], [1, true]);

  console.log(`\n${pass} geçti, ${fail} hata`);
  process.exit(fail ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

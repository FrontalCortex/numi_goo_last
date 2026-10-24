/**
 * Ödevler: GERÇEK fonksiyonlar (createHomework, deleteHomework) Firestore emülatörüne karşı.
 *
 * NEDEN VAR
 *   Ayar doğrulaması (homework.js) ayrı ayrı doğru olsa da gövde yanlış sınıfa, sınıfta
 *   olmayan öğrenciye ya da başka öğretmenin sınıfına ödev yazabilirdi. Kurallar ödevi
 *   istemciye kapattığı için tek kapı bu fonksiyon.
 *
 * ÇALIŞTIRMA (proje kökünden)
 *   firebase emulators:exec --only firestore --project numigo-new "node functions/scripts/test-homework-flow.js"
 */
const admin = require('firebase-admin');

if (!process.env.FIRESTORE_EMULATOR_HOST && process.env.ALLOW_PRODUCTION !== '1') {
  console.error('FIRESTORE_EMULATOR_HOST ayarlı değil; çıkılıyor.');
  process.exit(1);
}
delete process.env.FIREBASE_CONFIG;
delete process.env.GOOGLE_APPLICATION_CREDENTIALS;
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || 'numigo-new';

const fns = require('../index');
if (!admin.apps.length) admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT });
const db = admin.firestore();

const S = `hw${Date.now().toString(36)}`;
const T1 = `${S}_t1`;
const T2 = `${S}_t2`;
const S1 = `${S}_s1`;
const S2 = `${S}_s2`;
const S3 = `${S}_s3`; // sınıfta değil
const C1 = `${S}_c1`;
const C2 = `${S}_c2`;

const ctx = (uid) => ({ auth: { uid, token: {} } });
const call = (fn, uid, data) => fns[fn].run(data || {}, ctx(uid));

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

const ADD = { op: 'add', rules: ['ten', 'five', 'five'], withAbacus: true, digits: 2, numberCount: 5, intervalMs: 1500, questionCount: 25 };

async function main() {
  await Promise.all([
    db.doc(`users/${T1}`).set({ role: 'TEACHER', teacherApproved: true, courseId: 'k', name: 'Ayşe Hoca' }),
    db.doc(`users/${T2}`).set({ role: 'TEACHER', teacherApproved: true, courseId: 'k', name: 'Can Hoca' }),
    db.doc(`users/${S1}`).set({ role: 'STUDENT', name: 'Ali' }),
    db.doc(`users/${S2}`).set({ role: 'STUDENT', name: 'Veli' }),
    db.doc(`users/${S3}`).set({ role: 'STUDENT', name: 'Deniz' }),
    db.doc(`classes/${C1}`).set({ courseId: 'k', name: 'Cmt', teacherUids: [T1], archived: false, studentCount: 2 }),
    db.doc(`classes/${C1}/members/${S1}`).set({ uid: S1, name: 'Ali' }),
    db.doc(`classes/${C1}/members/${S2}`).set({ uid: S2, name: 'Veli' }),
    db.doc(`classes/${C2}`).set({ courseId: 'k', name: 'Pzt', teacherUids: [T2], archived: false, studentCount: 0 }),
  ]);

  console.log('\n=== DOĞRULAMA ===');
  await rejects('oturum yok', fns.createHomework.run({}, {}), 'unauthenticated');
  await rejects('başkasının sınıfı', call('createHomework', T2, { classId: C1, config: ADD, target: { wholeClass: true } }), 'permission-denied');
  await rejects('öğrenci ödev veremez', call('createHomework', S1, { classId: C1, config: ADD, target: { wholeClass: true } }), 'permission-denied');
  await rejects('konu yok', call('createHomework', T1, { classId: C1, config: { ...ADD, rules: [] }, target: { wholeClass: true } }), 'invalid-argument');
  await rejects('bilinmeyen konu', call('createHomework', T1, { classId: C1, config: { ...ADD, rules: ['x'] }, target: { wholeClass: true } }), 'invalid-argument');
  await rejects('abaküslü 6 basamak', call('createHomework', T1, { classId: C1, config: { ...ADD, digits: 6 }, target: { wholeClass: true } }), 'invalid-argument');
  await rejects('hız 0,05 sn', call('createHomework', T1, { classId: C1, config: { ...ADD, intervalMs: 50 }, target: { wholeClass: true } }), 'invalid-argument');
  await rejects('201 soru', call('createHomework', T1, { classId: C1, config: { ...ADD, questionCount: 201 }, target: { wholeClass: true } }), 'invalid-argument');
  await rejects('çarpma abaküslü 3×3', call('createHomework', T1, { classId: C1, config: { op: 'mul', withAbacus: true, digits: 3, digits2: 3, questionCount: 10 }, target: { wholeClass: true } }), 'invalid-argument');
  await rejects('öğrenci seçilmedi', call('createHomework', T1, { classId: C1, config: ADD, target: { studentUids: [] } }), 'invalid-argument');
  await rejects('sınıfta olmayan öğrenci', call('createHomework', T1, { classId: C1, config: ADD, target: { studentUids: [S1, S3] } }), 'failed-precondition');

  console.log('\n=== SINIFA ÖDEV ===');
  const all = await call('createHomework', T1, { classId: C1, config: ADD, target: { wholeClass: true } });
  check('sınıftaki 2 öğrenciye', all.recipients, 2);
  const hw = (await db.doc(`homeworks/${all.homeworkId}`).get()).data();
  check('kurallar tekrarsız ve sabit sırada', hw.config.rules, ['five', 'ten']);
  check('adım sayısı 25 soru → 3', hw.blockCount, 3);
  check('sınıf ödevinde liste boş', [hw.wholeClass, hw.assignedUids], [true, []]);
  check('bilinmeyen alan saklanmadı', Object.keys(hw.config).sort(), ['digits', 'intervalMs', 'numberCount', 'op', 'questionCount', 'rules', 'withAbacus']);
  check('öğretmen adı', [hw.teacherName, hw.className, hw.teacherUid], ['Ayşe Hoca', 'Cmt', T1]);

  console.log('\n=== SEÇİLİ ÖĞRENCİLERE ===');
  const mul = await call('createHomework', T1, {
    classId: C1,
    config: { op: 'mul', withAbacus: false, digits: 3, digits2: 3, questionCount: 7, rules: ['five'] },
    target: { studentUids: [S1, S1] },
  });
  const hwMul = (await db.doc(`homeworks/${mul.homeworkId}`).get()).data();
  check('tekrar eden öğrenci tek', hwMul.assignedUids, [S1]);
  check('çarpmada yalnızca çarpma alanları', Object.keys(hwMul.config).sort(), ['digits', 'digits2', 'op', 'questionCount', 'withAbacus']);
  check('7 soru → 1 adım', hwMul.blockCount, 1);

  console.log('\n=== SİLME ===');
  await db.doc(`homeworks/${all.homeworkId}/studentProgress/${S1}`).set({ uid: S1, completedBlocks: 1, answered: 10, correct: 8, lastBlockCorrect: 8, updatedAtMs: 1 });
  await rejects('başka öğretmen silemez', call('deleteHomework', T2, { homeworkId: all.homeworkId }), 'permission-denied');
  const del = await call('deleteHomework', T1, { homeworkId: all.homeworkId });
  check('silindi', del.deleted, true);
  check('ilerleme de silindi', (await db.doc(`homeworks/${all.homeworkId}/studentProgress/${S1}`).get()).exists, false);
  check('olmayan ödev: hata değil', (await call('deleteHomework', T1, { homeworkId: all.homeworkId })).deleted, false);

  console.log('\n=== BİTİŞ (onHomeworkProgressWritten) ===');
  {
    const functionsSdk = require('firebase-functions');
    const done = await call('createHomework', T1, { classId: C1, config: { ...ADD, questionCount: 20 }, target: { wholeClass: true } });
    const hwPath = `homeworks/${done.homeworkId}`;
    // Tetikleyici emülatörde kendiliğinden çalışmıyor (yalnızca Firestore emülatörü); gerçek
    // anlık görüntülerle elle çağrılıyor.
    async function writeProgress(uid, data) {
      const ref = db.doc(`${hwPath}/studentProgress/${uid}`);
      const before = await ref.get();
      await ref.set(data);
      const after = await ref.get();
      await fns.onHomeworkProgressWritten.run(
        functionsSdk.Change.fromObjects(before, after),
        { params: { homeworkId: done.homeworkId, studentUid: uid } },
      );
    }
    await writeProgress(S1, { uid: S1, completedBlocks: 1, answered: 10, correct: 8, lastBlockCorrect: 8, updatedAtMs: 1,
      ruleTally: { add_five: { c: 7, w: 2 }, add_ten: { c: 3, w: 0 }, hile: { c: 99, w: 0 } } });
    check('ilk adım: henüz bitmedi', (await db.doc(hwPath).get()).get('doneUids'), undefined);
    const stats = () => db.doc(`homeworkRuleStats/${S1}`).get()
      .then((d) => Object.fromEntries(Object.entries(d.get('tally') || {}).sort(([a], [b]) => a.localeCompare(b)))); // alan sırası belirsiz
    check('kural sayımı: ilk adım (bilinmeyen anahtar atıldı)', await stats(), { add_five: { c: 7, w: 2 }, add_ten: { c: 3, w: 0 } });
    await writeProgress(S1, { uid: S1, completedBlocks: 2, answered: 20, correct: 17, lastBlockCorrect: 9, updatedAtMs: 2,
      ruleTally: { add_five: { c: 5, w: 1 }, add_bead: { c: 1, w: 1 } } });
    check('kural sayımı: ikinci adım üstüne eklendi', await stats(),
      { add_bead: { c: 1, w: 1 }, add_five: { c: 12, w: 3 }, add_ten: { c: 3, w: 0 } });
    check('son adım: bitirenlere eklendi', (await db.doc(hwPath).get()).get('doneUids'), [S1]);
    await writeProgress(S2, { uid: S2, completedBlocks: 2, answered: 20, correct: 20, lastBlockCorrect: 10, updatedAtMs: 3 });
    check('ikinci öğrenci', ((await db.doc(hwPath).get()).get('doneUids') || []).sort(), [S1, S2].sort());
    // Tetikleyici iki kez çalışsa da (aynı yazım) tekrar eklemez.
    await writeProgress(S2, { uid: S2, completedBlocks: 2, answered: 20, correct: 20, lastBlockCorrect: 10, updatedAtMs: 4,
      ruleTally: { add_five: { c: 50, w: 0 } } });
    check('tekrar: değişmedi', ((await db.doc(hwPath).get()).get('doneUids') || []).length, 2);
    check('tekrar: adım ilerlemeden sayım eklenmez', (await db.doc(`homeworkRuleStats/${S2}`).get()).exists, false);

    const h = require('../homework');
    check('bildirim metni', h.doneNoticeText({ studentName: 'Ali', summary: "5'lik toplama", correct: 17, answered: 20, doneCount: 8, targetCount: 12 }),
      { title: "Ödev tamamlandı: 5'lik toplama", body: 'Ali bitirdi · 17/20 doğru · 8/12 öğrenci bitirdi' });
    check('ayrılan öğrenci sayılmaz', h.countDone(['a', 'b', 'x', 'a'], ['a', 'b', 'c']), 2);
    check('sayım süzgeci', h.sanitizeRuleTally({
      sub_direct: { c: 2, w: 1 }, mulA_2x1: { c: 4, w: 0 }, mulM_9x1: { c: 1, w: 0 }, add_five: { c: -3, w: 1.5 },
      add_ten: { c: 1e9, w: 0 }, 'add_five.c': { c: 1, w: 0 }, add_bead: { c: 0, w: 0 },
    }), [{ key: 'sub_direct', c: 2, w: 1 }, { key: 'mulA_2x1', c: 4, w: 0 }, { key: 'add_ten', c: 8000, w: 0 }]);
    check('sayım süzgeci: dizi/boş', [h.sanitizeRuleTally([1]), h.sanitizeRuleTally(null)], [[], []]);
  }

  console.log('\n=== HESAP SİLME ===');
  await db.doc(`homeworks/${mul.homeworkId}/studentProgress/${S1}`).set({ uid: S1, completedBlocks: 1, answered: 7, correct: 7, lastBlockCorrect: 7, updatedAtMs: 1 });
  // İki ödevde ilerlemesi var: biri burada, biri bitiş testindeki ödevde.
  check('silinen öğrencinin bütün ilerlemeleri', await fns._removeDeletedUserHomeworkProgress(S1), 2);
  check('kural istatistiği de silindi', (await db.doc(`homeworkRuleStats/${S1}`).get()).exists, false);
  check('ödevin kendisi duruyor', (await db.doc(`homeworks/${mul.homeworkId}`).get()).exists, true);

  console.log(`\n${pass} geçti, ${fail} hata`);
  process.exit(fail ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

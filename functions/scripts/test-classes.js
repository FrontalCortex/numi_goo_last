/**
 * Kurs sınıfları çekirdeğinin karar mantığı testi (bkz. classes.js).
 *
 * NEDEN VAR
 *   Havuz kararı yanlışsa hata sahada görünmüyor: soru yanlış öğretmene düşüyor ya da
 *   kimsenin görmediği bir havuzda 48 saat bekleyip iade ediliyor. İkisi de şikâyet gelene
 *   kadar fark edilmez.
 *
 * ÇALIŞTIRMA: cd functions && npm run test:classes
 */
const assert = require('assert');
const c = require('../classes');

let passed = 0;
function test(name, fn) {
  fn();
  passed++;
  console.log(`  ✓ ${name}`);
}

const TEACHER = { role: 'TEACHER', teacherApproved: true, courseId: 'kurs1', name: 'Ayşe' };
const GENERAL_TEACHER = { role: 'TEACHER', teacherApproved: true };
const PENDING_TEACHER = { role: 'TEACHER', teacherApproved: false, courseId: 'kurs1' };
const STUDENT = { role: 'STUDENT', userId: 'ALI123' };
const CLASS = { courseId: 'kurs1', name: 'Cmt 10:00', teacherUids: ['t1'], studentCount: 3, archived: false };

console.log('hesap türleri');
test('onaylı öğretmen', () => assert.strictEqual(c.isApprovedTeacher(TEACHER), true));
test('onay bekleyen öğretmen öğretmen değil', () => assert.strictEqual(c.isApprovedTeacher(PENDING_TEACHER), false));
test('onay bekleyen öğretmen öğrenci de değil', () => assert.strictEqual(c.isStudentAccount(PENDING_TEACHER), false));
test('role alanı olmayan hesap öğrenci', () => assert.strictEqual(c.isStudentAccount({}), true));
test('onaylı öğretmenin kursu', () => assert.strictEqual(c.courseIdOfTeacher(TEACHER), 'kurs1'));
test('onaysız öğretmenin kursu yok sayılır', () => assert.strictEqual(c.courseIdOfTeacher(PENDING_TEACHER), null));

console.log('sınıf adı ve kod');
test('ad kırpılıp tek boşluğa iniyor', () => assert.deepStrictEqual(c.normalizeClassName('  Cmt   10:00 '), { ok: true, name: 'Cmt 10:00' }));
test('tek harf red', () => assert.strictEqual(c.normalizeClassName('A').ok, false));
test('uzun ad red', () => assert.strictEqual(c.normalizeClassName('x'.repeat(c.CLASS_NAME_MAX + 1)).ok, false));
test('ad dizgi değilse red', () => assert.strictEqual(c.normalizeClassName(42).ok, false));
test('kod küçük harf, boşluk ve tireyle', () => assert.deepStrictEqual(c.normalizeJoinCode('ab c-234'), { ok: true, code: 'ABC234' }));
test('kodda 0/O/1/I red (alfabe dışı)', () => assert.strictEqual(c.normalizeJoinCode('ABC0I1').ok, false));
test('kısa kod red', () => assert.strictEqual(c.normalizeJoinCode('ABC23').ok, false));
test('üretilen kod alfabeden ve 6 hane', () => {
  for (let i = 0; i < 200; i++) assert.strictEqual(c.normalizeJoinCode(c.generateJoinCode()).ok, true);
});
test('rastgele sınırda (0.9999) alfabe dışına taşmıyor', () =>
  assert.strictEqual(c.normalizeJoinCode(c.generateJoinCode(() => 0.99999)).ok, true));

console.log('kullanıcı adı');
test('@ atılıyor, üç yazım deneniyor', () => assert.deepStrictEqual(c.userIdCandidates('@Ali12'), ['Ali12', 'ALI12', 'ali12']));
test('tekrarlar ayıklanıyor', () => assert.deepStrictEqual(c.userIdCandidates('835398VV'), ['835398VV', '835398vv']));
test('boş ad', () => assert.deepStrictEqual(c.userIdCandidates('  '), []));
test('yol karakteri red', () => assert.deepStrictEqual(c.userIdCandidates('a/b'), []));

console.log('sınıf açma');
test('kurs öğretmeni açabilir', () => assert.deepStrictEqual(c.createClassDecision(TEACHER, 0), { ok: true, courseId: 'kurs1' }));
test('kursu olmayan öğretmen açamaz', () => assert.strictEqual(c.createClassDecision(GENERAL_TEACHER, 0).code, 'permission-denied'));
test('onaysız öğretmen açamaz', () => assert.strictEqual(c.createClassDecision(PENDING_TEACHER, 0).code, 'permission-denied'));
test('öğrenci açamaz', () => assert.strictEqual(c.createClassDecision({ ...STUDENT, courseId: 'kurs1' }, 0).code, 'permission-denied'));
test('sınır dolu', () => assert.strictEqual(c.createClassDecision(TEACHER, c.CLASS_MAX_PER_TEACHER).code, 'resource-exhausted'));

console.log('soru havuzu');
test('sınıftaki öğrencinin sorusu sınıfa', () =>
  assert.deepStrictEqual(c.questionPoolFields({ ...STUDENT, classId: 'c1' }, CLASS), {
    pool: 'class', classId: 'c1', courseId: 'kurs1', classTeacherUids: ['t1'],
  }));
test('sınıfsız öğrencinin sorusu genele', () =>
  assert.deepStrictEqual(c.questionPoolFields(STUDENT, null), {
    pool: 'general', classId: null, courseId: null, classTeacherUids: [],
  }));
test('kapanmış sınıf → genel', () => assert.strictEqual(c.questionPoolFields({ classId: 'c1' }, { ...CLASS, archived: true }).pool, 'general'));
test('öğretmensiz sınıf → genel', () => assert.strictEqual(c.questionPoolFields({ classId: 'c1' }, { ...CLASS, teacherUids: [] }).pool, 'general'));
test('sınıf dokümanı yok → genel', () => assert.strictEqual(c.questionPoolFields({ classId: 'c1' }, null).pool, 'general'));
test('öğretmen listesi kopya (sınıf dizisine bağlı değil)', () => {
  const f = c.questionPoolFields({ classId: 'c1' }, CLASS);
  f.classTeacherUids.push('x');
  assert.deepStrictEqual(CLASS.teacherUids, ['t1']);
});

console.log('katılma');
test('sınıfsız öğrenci katılır', () =>
  assert.deepStrictEqual(c.joinDecision(STUDENT, 'c1', CLASS), { ok: true, alreadyMember: false, previousClassId: null }));
test('aynı sınıfa tekrar: zaten üye', () => assert.strictEqual(c.joinDecision({ ...STUDENT, classId: 'c1' }, 'c1', CLASS).alreadyMember, true));
test('başka sınıftan geçiş: önceki sınıf bildiriliyor', () =>
  assert.strictEqual(c.joinDecision({ ...STUDENT, classId: 'c0' }, 'c1', CLASS).previousClassId, 'c0'));
test('öğretmen katılamaz', () => assert.strictEqual(c.joinDecision(TEACHER, 'c1', CLASS).code, 'failed-precondition'));
test('kapanmış sınıfa katılınamaz', () => assert.strictEqual(c.joinDecision(STUDENT, 'c1', { ...CLASS, archived: true }).code, 'not-found'));
test('dolu sınıf red', () =>
  assert.strictEqual(c.joinDecision(STUDENT, 'c1', { ...CLASS, studentCount: c.CLASS_MAX_STUDENTS }).code, 'resource-exhausted'));
test('dolu sınıfın üyesi "zaten üye" alır, red değil', () =>
  assert.strictEqual(c.joinDecision({ ...STUDENT, classId: 'c1' }, 'c1', { ...CLASS, studentCount: c.CLASS_MAX_STUDENTS }).ok, true));
test('üyelik alanları MEMBERSHIP_FIELDS ile aynı', () =>
  assert.deepStrictEqual(Object.keys(c.membershipFields('c1', CLASS, 5)).sort(), c.MEMBERSHIP_FIELDS.slice().sort()));

console.log('yanlış kod sınırı');
const NOW = 1_800_000_000_000;
test('ilk deneme serbest', () => assert.strictEqual(c.joinGuardCheck(null, NOW).ok, true));
test('sayaç artıyor', () => assert.deepStrictEqual(c.joinGuardAfterWrong({ windowStartMs: NOW - 1000, wrong: 2 }, NOW), { windowStartMs: NOW - 1000, wrong: 3 }));
test('sınırda kilit', () => assert.strictEqual(c.joinGuardCheck({ windowStartMs: NOW - 1000, wrong: c.JOIN_WRONG_LIMIT }, NOW).code, 'resource-exhausted'));
test('pencere geçince kilit kalkıyor', () =>
  assert.strictEqual(c.joinGuardCheck({ windowStartMs: NOW - c.JOIN_WRONG_WINDOW_MS, wrong: 99 }, NOW).ok, true));
test('pencere geçince sayaç baştan', () =>
  assert.deepStrictEqual(c.joinGuardAfterWrong({ windowStartMs: NOW - c.JOIN_WRONG_WINDOW_MS, wrong: 99 }, NOW), { windowStartMs: NOW, wrong: 1 }));

console.log('davet');
test('sınıfın öğretmeni öğrenciyi davet edebilir', () => assert.strictEqual(c.inviteDecision('t1', CLASS, STUDENT).ok, true));
test('başka öğretmen davet edemez', () => assert.strictEqual(c.inviteDecision('t2', CLASS, STUDENT).code, 'permission-denied'));
test('öğretmen hesabı davet edilemez', () => assert.strictEqual(c.inviteDecision('t1', CLASS, GENERAL_TEACHER).code, 'failed-precondition'));
test('kapanmış sınıftan davet yok', () => assert.strictEqual(c.inviteDecision('t1', { ...CLASS, archived: true }, STUDENT).code, 'permission-denied'));
const INVITE = { studentUid: 's1', status: 'pending', createdAtMs: NOW - 1000 };
test('davetli öğrenci yanıtlar', () => assert.strictEqual(c.respondDecision(INVITE, 's1', NOW).ok, true));
test('başkasının daveti bulunamadı', () => assert.strictEqual(c.respondDecision(INVITE, 's2', NOW).code, 'not-found'));
test('yanıtlanmış davet red', () => assert.strictEqual(c.respondDecision({ ...INVITE, status: 'declined' }, 's1', NOW).code, 'failed-precondition'));
test('süresi dolan davet red', () =>
  assert.strictEqual(c.respondDecision({ ...INVITE, createdAtMs: NOW - c.INVITE_TTL_MS - 1 }, 's1', NOW).code, 'deadline-exceeded'));

console.log('öğretmenin gördüğü havuz');
const classQ = { pool: 'class', classTeacherUids: ['t1'] };
const generalQ = { pool: 'general', classTeacherUids: [] };
const legacyQ = {};
const courseVis = c.poolVisibility(TEACHER);
const helperVis = c.poolVisibility({ ...TEACHER, helpsGeneralPool: true });
const generalVis = c.poolVisibility(GENERAL_TEACHER);
test('kurs öğretmeni: yalnızca sınıf', () => assert.deepStrictEqual(courseVis, { classPool: true, generalPool: false }));
test('genel öğretmen: yalnızca genel', () => assert.deepStrictEqual(generalVis, { classPool: false, generalPool: true }));
test('yardım eden kurs öğretmeni: ikisi', () => assert.deepStrictEqual(helperVis, { classPool: true, generalPool: true }));
test('kurs öğretmeni kendi sınıf sorusunu görür', () => assert.strictEqual(c.questionVisibleTo(classQ, 't1', courseVis), true));
test('kurs öğretmeni başka sınıfın sorusunu görmez', () => assert.strictEqual(c.questionVisibleTo(classQ, 't2', courseVis), false));
test('kurs öğretmeni genel soruyu görmez', () => assert.strictEqual(c.questionVisibleTo(generalQ, 't1', courseVis), false));
test('genel öğretmen sınıf sorusunu görmez (uid listede olsa bile)', () =>
  assert.strictEqual(c.questionVisibleTo(classQ, 't1', generalVis), false));
test('genel öğretmen eski (pool alanı yok) soruyu görür', () => assert.strictEqual(c.questionVisibleTo(legacyQ, 'x', generalVis), true));
test('yardım eden kurs öğretmeni genel soruyu görür', () => assert.strictEqual(c.questionVisibleTo(generalQ, 't1', helperVis), true));

console.log(`\n${passed} test geçti.`);

'use strict';

/**
 * KURS SINIFLARI ÇEKİRDEĞİ — kurs öğretmeninin yalnızca kendi öğrencilerinin sorularını
 * görmesi, öğrencinin sınıfa bağlanması ve sorunun hangi havuza düşeceği kararı.
 *
 * MODEL
 *   courses/{courseId}            Kurs. Yalnızca yönetici yazar (tools/kurs/kurs_ogretmeni_ata.js).
 *   users/{öğretmen}.courseId     Öğretmenin bağlı olduğu kurs; onay sırasında yönetici yazar.
 *                                 Dolu olan öğretmen "kurs öğretmeni"dir: sınıf açabilir, genel
 *                                 havuzu yalnızca `helpsGeneralPool` açıksa görür.
 *   classes/{classId}             Kurs öğretmeninin uygulamadan açtığı sınıf (createClass).
 *   classJoinCodes/{kod}          Katılma kodu → classId. İstemciye tamamen kapalı.
 *   classes/{id}/members/{uid}    Öğretmen panelindeki öğrenci listesi.
 *   classInvites/{classId_uid}    Öğretmenin kullanıcı adıyla gönderdiği davet; öğrenci kabul eder.
 *   users/{öğrenci}.classId …     Öğrencinin üyeliği ([MEMBERSHIP_FIELDS]); yalnızca sunucu yazar.
 *
 * HAVUZ KARARI NEDEN SUNUCUDA
 *   Soruyu zaten askTeacherQuestion yazıyor; havuz alanları ([questionPoolFields]) aynı
 *   transaction'da, öğrencinin istemcisinden hiçbir şey alınmadan ekleniyor. İstemci karar
 *   verseydi öğrenci sorusunu başka bir sınıfın öğretmenine ya da genel havuza düşürebilirdi.
 *
 *   `classTeacherUids` soruya KOPYALANIYOR (sınıf dokümanına bakılmıyor), çünkü Firestore
 *   kuralları bir sorgunun yalnızca izinli dokümanları getireceğini sorgunun kendi
 *   filtrelerinden kanıtlamak zorunda: `array-contains benimUid` filtresi kuralın
 *   `request.auth.uid in resource.data.classTeacherUids` koşulunu doğrudan kanıtlıyor. Sınıfa
 *   bakan bir kural (her soru için get()) liste sorgusunda kanıtlanamazdı.
 *
 * BİR ÖĞRENCİ TEK SINIFTA
 *   Başka bir sınıfa katılan öğrenci eskisinden çıkar. Soru başına tek bir "kendi öğretmeni"
 *   olsun diye; iki sınıf, iki öğretmenin aynı soruyu beklemesi demek olurdu.
 *
 * NEDEN AYRI MODÜL
 *   bugReports.js ile aynı desen: burada yalnızca saf karar var, Firestore'a dokunan taraf
 *   index.js. Kurallar emülatör kurmadan test edilebiliyor (scripts/test-classes.js).
 */

const POOL_GENERAL = 'general';
const POOL_CLASS = 'class';

const CLASS_NAME_MIN = 2;
const CLASS_NAME_MAX = 40;

/** Bir öğretmenin açabileceği sınıf sayısı. Bir kursun haftalık grupları için bol. */
const CLASS_MAX_PER_TEACHER = 20;

/**
 * Bir sınıftaki en fazla öğrenci. Kod sızarsa (WhatsApp grubundan başka bir gruba) sınıfın
 * sınırsız dolmasını engelliyor; gerçek bir soroban sınıfı 10–20 kişi.
 */
const CLASS_MAX_STUDENTS = 60;

/**
 * Kod alfabesi: 0/O ve 1/I yok — öğretmen kodu tahtaya yazıyor, çocuk okuyup giriyor.
 * 32 karakter, 6 hane ≈ 1 milyar olasılık; [JOIN_WRONG_LIMIT] ile tahmin pratikte imkânsız.
 */
const JOIN_CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const JOIN_CODE_LENGTH = 6;

/** Saatte en fazla kaç YANLIŞ kod denemesi. Doğru kod sayılmıyor. */
const JOIN_WRONG_LIMIT = 10;
const JOIN_WRONG_WINDOW_MS = 60 * 60 * 1000;

/** Kabul edilmeyen davet bu süreden sonra geçersiz. */
const INVITE_TTL_MS = 14 * 24 * 60 * 60 * 1000;

/** Öğrencinin kullanıcı dokümanındaki üyelik alanları; çıkarken hepsi siliniyor. */
const MEMBERSHIP_FIELDS = ['classId', 'courseId', 'classTeacherUids', 'classJoinedAt'];

function fail(code, message) {
  return { ok: false, code, message };
}

function isApprovedTeacher(user) {
  return !!user && user.role === 'TEACHER' && user.teacherApproved === true;
}

/**
 * Öğrenci hesabı mı. Onay bekleyen öğretmen de (`role: TEACHER`) öğrenci SAYILMIYOR: bir
 * sınıfa katılsa öğretmenin listesinde öğrenci gibi görünürdü.
 */
function isStudentAccount(user) {
  return !!user && (user.role || 'STUDENT') === 'STUDENT' && user.teacherApproved !== true;
}

/** Kurs öğretmeni: onaylı ve yöneticinin bir kursa bağladığı öğretmen. */
function courseIdOfTeacher(user) {
  if (!isApprovedTeacher(user)) return null;
  return typeof user.courseId === 'string' && user.courseId ? user.courseId : null;
}

function normalizeClassName(raw) {
  const name = typeof raw === 'string' ? raw.replace(/\s+/g, ' ').trim() : '';
  if (name.length < CLASS_NAME_MIN) return fail('invalid-argument', 'Sınıf adı çok kısa.');
  if (name.length > CLASS_NAME_MAX) return fail('invalid-argument', 'Sınıf adı çok uzun.');
  return { ok: true, name };
}

/**
 * Kullanıcının girdiği kod. Boşluk ve tire atılıyor, küçük harf büyütülüyor; çocuk "abc 234"
 * ya da "ABC-234" yazabilir. Alfabe dışı karakter içeren kod hiç sorgulanmıyor.
 */
function normalizeJoinCode(raw) {
  const code = typeof raw === 'string' ? raw.replace(/[\s-]/g, '').toUpperCase() : '';
  if (code.length !== JOIN_CODE_LENGTH || [...code].some((c) => !JOIN_CODE_ALPHABET.includes(c))) {
    return fail('invalid-argument', 'Kod 6 karakter olmalı.');
  }
  return { ok: true, code };
}

function generateJoinCode(random = Math.random) {
  let out = '';
  for (let i = 0; i < JOIN_CODE_LENGTH; i++) {
    out += JOIN_CODE_ALPHABET[Math.floor(random() * JOIN_CODE_ALPHABET.length)];
  }
  return out;
}

/**
 * Öğretmenin yazdığı kullanıcı adı için denenecek yazımlar. `userId` sorgusu büyük/küçük
 * harfe duyarlı; öğrencinin adı ekranda nasıl görünüyorsa öğretmen öyle yazmayabilir.
 * Başındaki "@" atılıyor (profilde öyle görünüyor).
 */
function userIdCandidates(raw) {
  const base = typeof raw === 'string' ? raw.trim().replace(/^@/, '') : '';
  if (!base || base.length > 40 || base.includes('/')) return [];
  return [...new Set([base, base.toUpperCase(), base.toLowerCase()])];
}

function createClassDecision(teacher, ownedCount) {
  if (!courseIdOfTeacher(teacher)) {
    return fail('permission-denied', 'Sınıf açmak için bir kursa bağlı öğretmen olmalısın.');
  }
  if (ownedCount >= CLASS_MAX_PER_TEACHER) {
    return fail('resource-exhausted', `En fazla ${CLASS_MAX_PER_TEACHER} sınıf açabilirsin.`);
  }
  return { ok: true, courseId: teacher.courseId };
}

/** Sınıf hâlâ soru alabilir mi: silinmemiş, en az bir öğretmeni var. */
function classIsActive(classData) {
  return !!classData
    && classData.archived !== true
    && Array.isArray(classData.teacherUids)
    && classData.teacherUids.length > 0;
}

function isTeacherOfClass(classData, uid) {
  return !!classData && Array.isArray(classData.teacherUids) && classData.teacherUids.includes(uid);
}

/**
 * Yeni (ya da yeniden sorulan) bir sorunun havuz alanları.
 *
 * Öğrencinin sınıfı kapandıysa ya da öğretmensiz kaldıysa soru genel havuza düşüyor:
 * kimsenin göremeyeceği bir havuza düşüp 48 saat sonra iade edilmesinden iyi.
 *
 * @param user      Öğrencinin kullanıcı dokümanı.
 * @param classData `users.classId`'nin gösterdiği sınıf dokümanı (yoksa null).
 */
function questionPoolFields(user, classData) {
  const classId = user && typeof user.classId === 'string' ? user.classId : null;
  if (classId && classIsActive(classData)) {
    return {
      pool: POOL_CLASS,
      classId,
      courseId: classData.courseId || null,
      classTeacherUids: classData.teacherUids.slice(),
    };
  }
  return { pool: POOL_GENERAL, classId: null, courseId: null, classTeacherUids: [] };
}

/** Öğrencinin kullanıcı dokümanına yazılacak üyelik. */
function membershipFields(classId, classData, nowMs) {
  return {
    classId,
    courseId: classData.courseId || null,
    classTeacherUids: classData.teacherUids.slice(),
    classJoinedAt: nowMs,
  };
}

/**
 * Öğrenci bu sınıfa girebilir mi.
 *
 * @returns `{ ok, alreadyMember, previousClassId }` — `previousClassId` doluysa öğrenci
 *   önce o sınıftan çıkarılmalı.
 */
function joinDecision(student, classId, classData) {
  if (!isStudentAccount(student)) {
    return fail('failed-precondition', 'Yalnızca öğrenci hesapları sınıfa katılabilir.');
  }
  if (!classIsActive(classData)) {
    return fail('not-found', 'Bu sınıf artık yok.');
  }
  const current = typeof student.classId === 'string' && student.classId ? student.classId : null;
  if (current === classId) {
    return { ok: true, alreadyMember: true, previousClassId: null };
  }
  const count = Number.parseInt(classData.studentCount, 10) || 0;
  if (count >= CLASS_MAX_STUDENTS) {
    return fail('resource-exhausted', 'Bu sınıf dolu.');
  }
  return { ok: true, alreadyMember: false, previousClassId: current };
}

/**
 * Yanlış kod sınırı. `guard` = `{ windowStartMs, wrong }`; pencere dolunca sıfırlanıyor.
 * Doğru kod denemeyi saymıyor — kodu doğru giren çocuk kilitlenmesin.
 */
function joinGuardCheck(guard, nowMs) {
  const g = guard || {};
  const inWindow = nowMs - (Number(g.windowStartMs) || 0) < JOIN_WRONG_WINDOW_MS;
  if (inWindow && (Number(g.wrong) || 0) >= JOIN_WRONG_LIMIT) {
    return fail('resource-exhausted', 'Çok fazla yanlış kod denendi. Biraz sonra tekrar dene.');
  }
  return { ok: true };
}

function joinGuardAfterWrong(guard, nowMs) {
  const g = guard || {};
  const inWindow = nowMs - (Number(g.windowStartMs) || 0) < JOIN_WRONG_WINDOW_MS;
  return inWindow
    ? { windowStartMs: Number(g.windowStartMs), wrong: (Number(g.wrong) || 0) + 1 }
    : { windowStartMs: nowMs, wrong: 1 };
}

function inviteIdOf(classId, studentUid) {
  return `${classId}_${studentUid}`;
}

/** Öğretmen bu öğrenciyi bu sınıfa davet edebilir mi. */
function inviteDecision(teacherUid, classData, student) {
  if (!isTeacherOfClass(classData, teacherUid) || !classIsActive(classData)) {
    return fail('permission-denied', 'Bu sınıf senin değil.');
  }
  if (!isStudentAccount(student)) {
    return fail('failed-precondition', 'Bu hesap bir öğrenci hesabı değil.');
  }
  return { ok: true };
}

/** Öğrenci daveti yanıtlayabilir mi. */
function respondDecision(invite, uid, nowMs) {
  if (!invite || invite.studentUid !== uid) {
    return fail('not-found', 'Davet bulunamadı.');
  }
  if (invite.status !== 'pending') {
    return fail('failed-precondition', 'Bu davet artık geçerli değil.');
  }
  if (nowMs - (Number(invite.createdAtMs) || 0) > INVITE_TTL_MS) {
    return fail('deadline-exceeded', 'Davetin süresi doldu. Öğretmeninden yeni davet iste.');
  }
  return { ok: true };
}

/**
 * Öğretmen hangi havuzları görüyor. Kurs öğretmeni varsayılan olarak yalnızca sınıflarını;
 * `helpsGeneralPool` açıksa genel havuzu da.
 */
function poolVisibility(teacher) {
  const courseId = courseIdOfTeacher(teacher);
  return {
    classPool: !!courseId,
    generalPool: !courseId || teacher.helpsGeneralPool === true,
  };
}

/**
 * Soru bu öğretmenin havuzunda mı (havuz bildirimi için). `pool` alanı olmayan (bu
 * özellikten önceki) soru genel sayılıyor — kuralların yazma tarafıyla (`teacherMayWrite`)
 * aynı. Okuma kuralı bunu bilerek yapmıyor; nedeni firestore.rules'ta `teacherReaches`.
 */
function questionVisibleTo(question, teacherUid, visibility) {
  if ((question.pool || POOL_GENERAL) === POOL_CLASS) {
    return visibility.classPool
      && Array.isArray(question.classTeacherUids)
      && question.classTeacherUids.includes(teacherUid);
  }
  return visibility.generalPool;
}

module.exports = {
  POOL_GENERAL,
  POOL_CLASS,
  CLASS_NAME_MAX,
  CLASS_MAX_PER_TEACHER,
  CLASS_MAX_STUDENTS,
  JOIN_CODE_ALPHABET,
  JOIN_CODE_LENGTH,
  JOIN_WRONG_LIMIT,
  JOIN_WRONG_WINDOW_MS,
  INVITE_TTL_MS,
  MEMBERSHIP_FIELDS,
  isApprovedTeacher,
  isStudentAccount,
  courseIdOfTeacher,
  normalizeClassName,
  normalizeJoinCode,
  generateJoinCode,
  userIdCandidates,
  createClassDecision,
  classIsActive,
  isTeacherOfClass,
  questionPoolFields,
  membershipFields,
  joinDecision,
  joinGuardCheck,
  joinGuardAfterWrong,
  inviteIdOf,
  inviteDecision,
  respondDecision,
  poolVisibility,
  questionVisibleTo,
};

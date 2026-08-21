const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

// Load environment variables from .env file (for local testing)
// In production, use Firebase Secret Manager or environment variables
require('dotenv').config();

admin.initializeApp();
const db = admin.firestore();

const seasonLeaderboardFinalize = require('./seasonLeaderboardFinalize');
exports.finalizeSeasonLeaderboardMedals =
  seasonLeaderboardFinalize.scheduleFinalize(functions, admin, db);

// Config yükleme kontrolü için

function generateCode(length = 6) {
  // Sadece sayılar kullan (0-9) - daha kullanıcı dostu ve standart OTP formatı
  const digits = '0123456789';
  let code = '';
  for (let i = 0; i < length; i++) code += digits[Math.floor(Math.random() * digits.length)];
  return code;
}

// E-posta kayıtlı mı kontrolü (Giriş / Kayıt öncesi güvenli kontrol)
exports.checkEmailRegistered = functions.https.onCall(async (data, context) => {
  const email = (data && data.email) ? String(data.email).trim().toLowerCase() : '';
  if (!email) {
    throw new functions.https.HttpsError('invalid-argument', 'email required');
  }

  const usersSnap = await db.collection('users')
    .where('email', '==', email)
    .limit(1)
    .get();

  if (usersSnap.empty) {
    return { registered: false, uid: null, role: null };
  }

  const doc = usersSnap.docs[0];
  const uid = doc.get('uid') || doc.id;
  const role = doc.get('role') || 'STUDENT';

  return { registered: true, uid, role };
});

// Öğretmen kullanıcı ID'si ile e-posta bulma (Öğretmen girişi için)
exports.findTeacherEmailByUserId = functions.https.onCall(async (data, context) => {
  const userId = (data && data.userId) ? String(data.userId).trim() : '';
  if (!userId) {
    return { email: null };
  }

  const usersSnap = await db.collection('users')
    .where('userId', '==', userId)
    .where('role', '==', 'TEACHER')
    .limit(1)
    .get();

  if (usersSnap.empty) {
    return { email: null };
  }

  return { email: usersSnap.docs[0].get('email') || null };
});

const OTP_EMAIL_PER_HOUR = 5;
const OTP_EMAIL_PER_DAY = 20;
const OTP_IP_PER_HOUR = 10;
const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;

function getClientIp(context) {
  if (!context.rawRequest) return null;
  const forwarded = context.rawRequest.headers['x-forwarded-for'];
  if (forwarded) return forwarded.split(',')[0].trim();
  return context.rawRequest.connection?.remoteAddress || null;
  }

function getWindowStart(nowMs, windowMs) {
  return Math.floor(nowMs / windowMs) * windowMs;
}

async function checkOtpRateLimits(email, ip) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  if (!normalizedEmail) return { ok: true };

  const emailDocId = 'email:' + normalizedEmail.replace(/\//g, '_');
  const emailRef = db.collection('otpRateLimits').doc(emailDocId);

  let ipRef = null;
  if (ip) {
    const safeIp = String(ip).replace(/[^a-fA-F0-9.:]/g, '_').slice(0, 64);
    const ipDocId = 'ip:' + safeIp;
    ipRef = db.collection('otpRateLimits').doc(ipDocId);
  }

  // Tüm okuma ve yazma işlemlerini tek bir Atomik Transaction (İşlem) içine alıyoruz.
  // Bu, aynı anda gelen yüzlerce isteğin (Race Condition) sistemi delmesini imkansız hale getirir.
  await db.runTransaction(async (transaction) => {
    // 1. Transaction Kuralları: Önce tüm OKUMA (get) işlemleri yapılmalı
    const emailSnap = await transaction.get(emailRef);
    let ipSnap = null;
    if (ipRef) {
      ipSnap = await transaction.get(ipRef);
    }

    const now = admin.firestore.Timestamp.now();
    const nowMs = now.toMillis();
    const hourStart = getWindowStart(nowMs, HOUR_MS);
    const dayStart = getWindowStart(nowMs, DAY_MS);

    // 2. E-posta limitlerini kontrol et
    const emailData = emailSnap.exists ? emailSnap.data() : {};
    let emailHourCount = (emailData.lastHourStart === hourStart ? emailData.hourCount : 0) || 0;
    let emailDayCount = (emailData.lastDayStart === dayStart ? emailData.dayCount : 0) || 0;

    if (emailHourCount >= OTP_EMAIL_PER_HOUR) {
      const nextHourMs = hourStart + HOUR_MS;
      const waitMin = Math.ceil((nextHourMs - nowMs) / 60000);
      throw new functions.https.HttpsError(
        'resource-exhausted',
        `E-posta başına saatte en fazla ${OTP_EMAIL_PER_HOUR} kod gönderebilirsiniz. ${waitMin} dakika sonra tekrar deneyin.`
      );
    }
    if (emailDayCount >= OTP_EMAIL_PER_DAY) {
      const nextDayMs = dayStart + DAY_MS;
      const waitMin = Math.ceil((nextDayMs - nowMs) / 60000);
      throw new functions.https.HttpsError(
        'resource-exhausted',
        `Günlük kod limiti (${OTP_EMAIL_PER_DAY}) aşıldı. ${Math.ceil(waitMin / 60)} saat sonra tekrar deneyin.`
      );
    }

    // 3. IP limitlerini kontrol et
    let ipHourCount = 0;
    const ipHourStart = getWindowStart(nowMs, HOUR_MS);
    
    if (ipSnap) {
      const ipData = ipSnap.exists ? ipSnap.data() : {};
      ipHourCount = (ipData.lastHourStart === ipHourStart ? ipData.hourCount : 0) || 0;

      if (ipHourCount >= OTP_IP_PER_HOUR) {
        const nextHourMs = ipHourStart + HOUR_MS;
        const waitMin = Math.ceil((nextHourMs - nowMs) / 60000);
        throw new functions.https.HttpsError(
          'resource-exhausted',
          `Bu cihazdan saatte en fazla ${OTP_IP_PER_HOUR} kod gönderilebilir. ${waitMin} dakika sonra tekrar deneyin.`
        );
      }
    }

    // 4. Transaction Kuralları: Okumalar ve kontroller bittikten sonra YAZMA (set) işlemleri
    transaction.set(emailRef, {
      lastHourStart: hourStart,
      hourCount: emailHourCount + 1,
      lastDayStart: dayStart,
      dayCount: emailDayCount + 1,
      updatedAt: now
    }, { merge: true });

    if (ipRef) {
      transaction.set(ipRef, {
        lastHourStart: ipHourStart,
        hourCount: ipHourCount + 1,
        updatedAt: now
      }, { merge: true });
    }
  });

  return { ok: true };
}

exports.sendStudentVerificationCode = functions.https.onCall(async (data, context) => {
  const rawEmail = (data && data.email) || '';
  const email = String(rawEmail).trim().toLowerCase();
  const uid = (data && data.uid) || '';
  
  if (!email || !uid) {
    throw new functions.https.HttpsError('invalid-argument', 'email and uid required');
  }

  const ip = getClientIp(context);
  await checkOtpRateLimits(email, ip);

  const code = generateCode(6);
  const now = admin.firestore.Timestamp.now();
  const expiresAt = admin.firestore.Timestamp.fromMillis(now.toMillis() + 2 * 60 * 1000); // 2 dk

  // Firestore'a kodu kaydet
  await db.collection('studentVerificationCodes').doc(code).set({
    code,
    email,
    uid,
    createdAt: now,
    expiresAt,
    used: false
  });

  // Try to get email config from environment variables first, then fallback to functions.config()
  const user = process.env.EMAIL_USER || functions.config()?.email?.user;
  const pass = process.env.EMAIL_PASS || functions.config()?.email?.pass;
  
  console.log('Email config check:', { 
    fromEnv: { user: !!process.env.EMAIL_USER, pass: !!process.env.EMAIL_PASS },
    fromConfig: { hasEmailConfig: !!functions.config()?.email, user: !!functions.config()?.email?.user, pass: !!functions.config()?.email?.pass },
    final: { hasUser: !!user, hasPass: !!pass, userLength: user ? user.length : 0, passLength: pass ? pass.length : 0 }
  });
  
  if (!user || !pass) {
    console.error('Email credentials not set; skipping email send.', { user: !!user, pass: !!pass });
    return { code, emailed: false };
  }

  try {
  const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: { user, pass }
  });

  const mailOptions = {
    from: `NumiGoo <${user}>`,
    to: email,
    subject: 'NumiGoo - E-posta Doğrulama Kodu',
    html: `
      <h2>NumiGoo E-posta Doğrulama</h2>
      <p>Merhaba,</p>
      <p>NumiGoo hesabınızı doğrulamak için aşağıdaki kodu kullanın:</p>
      <h1 style="color: #4CAF50; font-size: 32px; letter-spacing: 5px; text-align: center;">${code}</h1>
        <p>Bu kod 2 dakika içinde geçerlidir.</p>
      <p>Eğer bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
      <p>İyi çalışmalar,<br>NumiGoo Ekibi</p>
    `,
      text: `NumiGoo E-posta Doğrulama\n\nDoğrulama kodunuz: ${code}\nBu kod 2 dakika içinde geçerlidir.`
  };

    console.log('Attempting to send email to:', email);
    const info = await transporter.sendMail(mailOptions);
    console.log('Email sent successfully:', { messageId: info.messageId, response: info.response });
  return { code, emailed: true };
  } catch (error) {
    console.error('Error sending email:', error);
    console.error('Error details:', { 
      message: error.message, 
      code: error.code,
      command: error.command,
      response: error.response,
      responseCode: error.responseCode
    });
    // Email gönderimi başarısız olsa bile kodu döndür (kullanıcı manuel girebilir)
    return { code, emailed: false, error: error.message };
  }
});

const WRONG_ATTEMPT_COOLDOWN_MS = 15 * 60 * 1000; // 15 dk
const MAX_WRONG_ATTEMPTS = 5;

async function checkWrongAttemptCooldown(email) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  if (!normalizedEmail) return;
  const docId = 'wrong:' + normalizedEmail.replace(/\//g, '_');
  const ref = db.collection('otpWrongAttempts').doc(docId);
  const snap = await ref.get();
  if (!snap.exists) return;
  const d = snap.data();
  const windowStart = d.windowStart && d.windowStart.toMillis ? d.windowStart.toMillis() : 0;
  const count = d.count || 0;
  const now = Date.now();
  if (count >= MAX_WRONG_ATTEMPTS && (now - windowStart) < WRONG_ATTEMPT_COOLDOWN_MS) {
    const waitMs = WRONG_ATTEMPT_COOLDOWN_MS - (now - windowStart);
    const waitMin = Math.ceil(waitMs / 60000);
    throw new functions.https.HttpsError(
      'resource-exhausted',
      `Çok fazla yanlış deneme. ${waitMin} dakika sonra tekrar deneyin.`
    );
  }
}

async function recordWrongAttempt(email) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  if (!normalizedEmail) return;
  const docId = 'wrong:' + normalizedEmail.replace(/\//g, '_');
  const ref = db.collection('otpWrongAttempts').doc(docId);

  // Yanlış deneme (Brute-Force) sayacını güvenli hale getirmek için Transaction kullanıyoruz
  await db.runTransaction(async (transaction) => {
    const snap = await transaction.get(ref);
    
    const now = admin.firestore.Timestamp.now();
    const nowMs = now.toMillis();
    
    let count = 0;
    let windowStart = nowMs;
    
    if (snap.exists) {
      const d = snap.data();
      const prevStart = d.windowStart && d.windowStart.toMillis ? d.windowStart.toMillis() : 0;
      
      // Eğer hala bekleme (cooldown) süresi içindeysek, sayacı 1 artır
      if (nowMs - prevStart < WRONG_ATTEMPT_COOLDOWN_MS) {
        count = (d.count || 0) + 1;
        windowStart = prevStart;
      } else {
        // Süre dolmuşsa, bu yeni bir döngünün ilk yanlış denemesidir
        count = 1;
      }
    } else {
      // Daha önce hiç yanlış girilmediyse sayacı 1 yap
      count = 1;
    }
    
    transaction.set(ref, { 
      count, 
      windowStart: admin.firestore.Timestamp.fromMillis(windowStart), 
      updatedAt: now 
    }, { merge: true });
  });
}

async function clearWrongAttempts(email) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  if (!normalizedEmail) return;
  const docId = 'wrong:' + normalizedEmail.replace(/\//g, '_');
  await db.collection('otpWrongAttempts').doc(docId).delete();
}

// OTP ile giriş: kodu doğrula ve custom token döndür
exports.verifyLoginCode = functions.https.onCall(async (data, context) => {
  const rawEmail = (data && data.email) || '';
  const email = String(rawEmail).trim().toLowerCase();
  const code = (data && data.code) || '';

  if (!email || !code) {
    throw new functions.https.HttpsError('invalid-argument', 'email and code required');
  }

  await checkWrongAttemptCooldown(email);

  const codeRef = db.collection('studentVerificationCodes').doc(code);
  const codeDoc = await codeRef.get();

  if (!codeDoc.exists) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz kod');
  }

  const d = codeDoc.data();
  const used = d.used === true;
  const expiresAt = d.expiresAt && d.expiresAt.toMillis ? d.expiresAt.toMillis() : 0;
  const docEmail = d.email || '';
  const uid = d.uid || '';

  if (used || expiresAt < Date.now() || docEmail !== email) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz veya süresi dolmuş kod');
  }

  await clearWrongAttempts(email);

  if (uid.startsWith('pending_')) {
    throw new functions.https.HttpsError('invalid-argument', 'Bu kod kayıt için. Giriş için e-postanıza gelen kodu kullanın.');
  }

  await codeRef.update({
    used: true,
    verifiedAt: admin.firestore.Timestamp.now()
  });

  // Auth kullanıcısında e-posta ve isim olsun (Console'da identifier "-" olmasın)
  const displayName = 'Kullanıcı';
  let tokenUid = uid;
  try {
    await admin.auth().getUser(uid);
    await admin.auth().updateUser(uid, { email: docEmail, displayName });
  } catch (err) {
    if (err.code === 'auth/user-not-found') {
      try {
        await admin.auth().createUser({
          uid,
          email: docEmail,
          displayName,
          emailVerified: true
        });
      } catch (createErr) {
        const code = createErr.code || (createErr.errorInfo && createErr.errorInfo.code);
        if (code === 'auth/email-already-exists' || code === 'auth/email-already-in-use') {
          // E-posta zaten başka bir hesapta (örn. Google) - o hesaba giriş yap
          const existingUser = await admin.auth().getUserByEmail(docEmail);
          tokenUid = existingUser.uid;
        } else {
          throw createErr;
        }
      }
    } else {
      throw err;
    }
  }

  // Öğrenci girişi için: sadece STUDENT rolüne token ver (öğretmen öğrenci ekranından giriş yapamaz)
  const userDoc = await db.collection('users').doc(tokenUid).get();
  if (userDoc.exists) {
    const role = userDoc.data().role || '';
    if (role !== 'STUDENT') {
      await recordWrongAttempt(email);
      throw new functions.https.HttpsError(
        'permission-denied',
        'Bu hesap öğretmen hesabı. Öğrenci giriş ekranından giriş yapılamaz.'
      );
    }
  }

  const token = await admin.auth().createCustomToken(tokenUid);
  return { token };
});

// OTP ile kayıt: kodu doğrula, hesabı oluştur/onayla ve custom token döndür
exports.verifyRegistrationCode = functions.https.onCall(async (data, context) => {
  const rawEmail = (data && data.email) || '';
  const email = String(rawEmail).trim().toLowerCase();
  const code = (data && data.code) || '';

  if (!email || !code) {
    throw new functions.https.HttpsError('invalid-argument', 'email and code required');
  }

  await checkWrongAttemptCooldown(email);

  const codeRef = db.collection('studentVerificationCodes').doc(code);
  const codeDoc = await codeRef.get();

  if (!codeDoc.exists) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz kod');
  }

  const d = codeDoc.data();
  const used = d.used === true;
  const expiresAt = d.expiresAt && d.expiresAt.toMillis ? d.expiresAt.toMillis() : 0;
  const docEmail = d.email || '';
  const docUid = d.uid || '';

  if (used || expiresAt < Date.now() || docEmail !== email) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz veya süresi dolmuş kod');
  }

  await clearWrongAttempts(email);

  await codeRef.update({
    used: true,
    verifiedAt: admin.firestore.Timestamp.now()
  });

  let tokenUid = docUid;

  if (docUid.startsWith('pending_')) {
    const pendingRef = db.collection('pendingRegistrations').doc(email);
    const pendingDoc = await pendingRef.get();
    
    if (!pendingDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Kayıt bilgileri bulunamadı');
    }
    
    const pendingData = pendingDoc.data();
    const name = pendingData.name || '';
    const password = pendingData.password || '';
    const roleForUser = pendingData.role || 'STUDENT';

    let userRecord;
    try {
      userRecord = await admin.auth().createUser({
        email: email,
        password: password,
        displayName: name,
        emailVerified: true
      });
    } catch (err) {
      if (err.code === 'auth/email-already-exists' || err.code === 'auth/email-already-in-use') {
        userRecord = await admin.auth().getUserByEmail(email);
        try {
          await admin.auth().updateUser(userRecord.uid, { password: password });
        } catch(e) { }
      } else {
        throw new functions.https.HttpsError('internal', 'Kullanıcı oluşturulamadı: ' + err.message);
      }
    }

    tokenUid = userRecord.uid;
    const userRef = db.collection('users').doc(tokenUid);
    const userDocSnap = await userRef.get();

    if (!userDocSnap.exists) {
      const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
      const randomNum = Math.floor(100000 + Math.random() * 900000);
      const suffix = letters[Math.floor(Math.random()*26)] + letters[Math.floor(Math.random()*26)];
      const userId = `${randomNum}${suffix}`;

      const finalName = name.trim() ? name : userId;
      
      try {
        await admin.auth().updateUser(tokenUid, { displayName: finalName });
      } catch (e) {
        console.error('Error updating display name:', e);
      }

      const baseData = {
        uid: tokenUid,
        userId: userId,
        email: email,
        name: finalName,
        role: roleForUser,
        first_tutorial_shown: false,
        createdAt: admin.firestore.Timestamp.now(),
        keys: 1,
        currency: 0
      };

      if (roleForUser === 'STUDENT') {
        baseData.verified = true;
      } else if (roleForUser === 'TEACHER') {
        baseData.teacherApproved = false;
      }

      await userRef.set(baseData);
    }
    
    await pendingRef.delete();
  } else {
    const userRef = db.collection('users').doc(docUid);
    const userDocSnap = await userRef.get();
    if (userDocSnap.exists) {
      await userRef.update({ verified: true });
    }
  }

  const token = await admin.auth().createCustomToken(tokenUid);
  return { token };
});

// Öğretmen şifre sıfırlama: kodu sadece doğrula, used işaretleme (sonra resetTeacherPassword kullanılacak)
exports.verifyTeacherPasswordResetCode = functions.https.onCall(async (data, context) => {
  const email = (data && data.email) ? String(data.email).trim().toLowerCase() : '';
  const code = (data && data.code) || '';

  if (!email || !code) {
    throw new functions.https.HttpsError('invalid-argument', 'email and code required');
  }

  await checkWrongAttemptCooldown(email);

  const codeRef = db.collection('studentVerificationCodes').doc(code);
  const codeDoc = await codeRef.get();

  if (!codeDoc.exists) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz kod');
  }

  const d = codeDoc.data();
  const used = d.used === true;
  const expiresAt = d.expiresAt && d.expiresAt.toMillis ? d.expiresAt.toMillis() : 0;
  const docEmail = (d.email || '').trim().toLowerCase();

  if (used || expiresAt < Date.now() || docEmail !== email) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz veya süresi dolmuş kod');
  }

  return { valid: true };
});

// Öğretmen şifre sıfırlama: kodu doğrula, used işaretle, şifreyi güncelle
exports.resetTeacherPassword = functions.https.onCall(async (data, context) => {
  const email = (data && data.email) ? String(data.email).trim().toLowerCase() : '';
  const code = (data && data.code) || '';
  const newPassword = (data && data.newPassword) || '';

  if (!email || !code || !newPassword) {
    throw new functions.https.HttpsError('invalid-argument', 'email, code and newPassword required');
  }

  if (newPassword.length < 6) {
    throw new functions.https.HttpsError('invalid-argument', 'Şifre en az 6 karakter olmalıdır');
  }

  await checkWrongAttemptCooldown(email);

  const codeRef = db.collection('studentVerificationCodes').doc(code);
  const codeDoc = await codeRef.get();

  if (!codeDoc.exists) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz kod');
  }

  const d = codeDoc.data();
  const used = d.used === true;
  const expiresAt = d.expiresAt && d.expiresAt.toMillis ? d.expiresAt.toMillis() : 0;
  const docEmail = (d.email || '').trim().toLowerCase();

  if (used || expiresAt < Date.now() || docEmail !== email) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz veya süresi dolmuş kod');
  }

  // İsteğe bağlı: Firestore'da bu e-postanın TEACHER olduğunu doğrula (Admin SDK'da where('field','==',value) kullanılır)
  const usersSnap = await db.collection('users').where('email', '==', email).where('role', '==', 'TEACHER').limit(1).get();
  if (usersSnap.empty) {
    await recordWrongAttempt(email);
    throw new functions.https.HttpsError('permission-denied', 'Bu e-posta öğretmen olarak kayıtlı değil');
  }

  await codeRef.update({
    used: true,
    verifiedAt: admin.firestore.Timestamp.now()
  });

  let authUid;
  try {
    const authUser = await admin.auth().getUserByEmail(email);
    authUid = authUser.uid;
  } catch (err) {
    if (err.code === 'auth/user-not-found') {
      throw new functions.https.HttpsError('not-found', 'Hesap bulunamadı');
    }
    throw err;
  }

  await admin.auth().updateUser(authUid, { password: newPassword });
  return { success: true };
});

// Recursive delete of a collection (batch 500)
async function deleteCollection(path) {
  const col = db.collection(path);
  const BATCH_SIZE = 500;
  let snapshot = await col.limit(BATCH_SIZE).get();
  while (!snapshot.empty) {
    const batch = db.batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    snapshot = await col.limit(BATCH_SIZE).get();
  }
}

// Yeni mesaj eklendiğinde alıcıya FCM bildirimi gönder (uygulama ikonu + gönderen adı + mesaj önizlemesi)
const MESSAGE_PREVIEW_MAX_LEN = 120;
exports.onMessageCreated = functions.firestore
  .document('questions/{questionId}/messages/{messageId}')
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const questionId = context.params.questionId;
    const messageId = context.params.messageId;
    const senderUid = message.senderUid || '';
    const type = message.type || 'text';
    const textContent = (message.textContent || '').trim();

    const questionSnap = await db.collection('questions').doc(questionId).get();
    if (!questionSnap.exists) return null;
    const question = questionSnap.data();
    const studentUid = question.studentUid || '';
    const claimedByTeacherUid = question.claimedByTeacherUid || null;

    let recipientUid = null;
    if (senderUid === studentUid) {
      recipientUid = claimedByTeacherUid;
    } else {
      recipientUid = studentUid;
    }
    if (!recipientUid || recipientUid === senderUid) return null;

    const recipientSnap = await db.collection('users').doc(recipientUid).get();
    if (!recipientSnap.exists) return null;
    const recipientData = recipientSnap.data() || {};
    let tokens = [];

    // Yeni yapı: fcmDevices (her eleman { deviceId, token, updatedAt })
    if (Array.isArray(recipientData.fcmDevices)) {
      tokens = recipientData.fcmDevices
        .map((d) => d && typeof d.token === 'string' ? d.token.trim() : '')
        .filter((t) => t.length > 0);
    } else if (Array.isArray(recipientData.fcmTokens)) {
      // Geriye dönük uyumluluk: eski dizi alanı
      tokens = recipientData.fcmTokens.filter((t) => typeof t === 'string' && t.trim().length > 0);
    } else if (recipientData.fcmToken) {
      // Tekil alan (en son token)
      tokens = [recipientData.fcmToken];
    }
    if (!tokens.length) return null;
    // Güvenlik için: her hesap en fazla 2 cihaz - sadece son 2 token'a gönder.
    if (tokens.length > 2) {
      tokens = tokens.slice(-2);
    }

    let senderName = 'Kullanıcı';
    try {
      const userSnap = await db.collection('users').doc(senderUid).get();
      if (userSnap.exists && userSnap.data().name) senderName = userSnap.data().name;
      else {
        const userRecord = await admin.auth().getUser(senderUid);
        if (userRecord.displayName) senderName = userRecord.displayName;
      }
    } catch (e) {
      // keep default senderName
    }

    let body = '';
    if (type === 'text' && textContent) {
      body = textContent.length > MESSAGE_PREVIEW_MAX_LEN
        ? textContent.slice(0, MESSAGE_PREVIEW_MAX_LEN) + '…'
        : textContent;
    } else if (type === 'audio') body = 'Ses mesajı';
    else if (type === 'video') body = 'Video';
    else if (type === 'image') body = 'Görsel';
    else body = 'Yeni mesaj';

    // Bildirim başlığı artık öğrenci adı yerine soru başlığı olsun.
    // HeaderInput CreateQuestion'da StudentQuestion.message / previewText'e yazılıyor.
    const questionTitle =
      (question && (question.previewText || question.message)) || 'Yeni soru';

    // Data-only FCM mesajı gönderiyoruz. Böylece Android tarafında
    // MyFirebaseMessagingService.onMessageReceived HER zaman çağrılır
    // (uygulama arka planda / kapalı olsa bile) ve kendi PendingIntent'imizle
    // doğru sohbete yönlendirebiliriz.
    const baseData = {
      questionId: String(questionId),
      messageId: String(messageId),
      recipientUid: String(recipientUid),
      // Bildirim kanalları soru başlığı altında gruplanabilsin diye title'a soru başlığını yaz.
      title: String(questionTitle),
      body,
      // İstersen istemci tarafında kullanabilmek için göndericinin adını da ayrıca data'ya ekleyelim.
      senderName,
    };

    // Her token için ayrı bir data-only FCM gönder.
    const sendPromises = tokens.map((token) =>
      admin.messaging().send({
        data: baseData,
        token,
      })
    );
    await Promise.all(sendPromises);

    // Mesaj sunucuya ulaştıktan sonra, bildirimi FCM'e başarıyla verdiysek deliveredAt alanını işaretle.
    // Böylece gönderici tarafta "çift tik gri" durumu (telefona gönderildi ama okunmadı) gösterilebilir.
    try {
      await snap.ref.update({
        deliveredAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (err) {
      console.error('Failed to set deliveredAt for message', context.params, err);
    }

    // İlgili soru dokümanında son mesaj zamanını güncelle (NotificationFragment sıralaması için).
    try {
      await db.collection('questions').doc(questionId).update({
        lastMessageAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    } catch (err) {
      console.error('Failed to update lastMessageAt on question', questionId, err);
    }

    return null;
  });

// Çözüldü soruda hem öğrenci hem öğretmen "listeden sil" derse soru + mesajları kalıcı sil
exports.onQuestionUpdated = functions.firestore
  .document('questions/{questionId}')
  .onUpdate(async (change, context) => {
    const after = change.after.data();
    const questionId = context.params.questionId;
    const deletedForUids = Array.isArray(after.deletedForUids) ? after.deletedForUids : [];
    const studentUid = after.studentUid || '';
    const claimedByTeacherUid = after.claimedByTeacherUid || '';

    if (!studentUid || !claimedByTeacherUid) return null;
    const bothDeleted =
      deletedForUids.includes(studentUid) && deletedForUids.includes(claimedByTeacherUid);
    if (!bothDeleted) return null;

    await deleteCollection(`questions/${questionId}/messages`);
    await change.after.ref.delete();
    return null;
  });



// Auth 'onDelete' trigger to recursively delete user data in Firestore
exports.cleanupUserOnDelete = functions.auth.user().onDelete(async (user) => {
  const uid = user.uid;
  console.log(`User deleted from Auth: ${uid}. Starting recursive delete of users/${uid}`);
  
  try {
    const userRef = db.collection('users').doc(uid);
    await db.recursiveDelete(userRef);
    console.log(`Successfully deleted users/${uid} and all subcollections.`);
  } catch (error) {
    console.error(`Error deleting user data for ${uid}:`, error);
  }
});
// C�zdan G�ncelleme Fonksiyonu
exports.updateUserWallet = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Oturum a�man�z gerekiyor.");
  }

  const uid = context.auth.uid;
  const deltaKeys = parseInt(data.keys) || 0;
  const deltaCurrency = parseInt(data.currency) || 0;
  const reason = data.reason || "unknown";

  if (deltaKeys === 0 && deltaCurrency === 0) {
    return { success: true, keys: 0, currency: 0 };
  }

  const userRef = db.collection("users").doc(uid);

  try {
    const result = await db.runTransaction(async (transaction) => {
      const doc = await transaction.get(userRef);
      if (!doc.exists) {
        throw new functions.https.HttpsError("not-found", "Kullan�c� bulunamad�.");
      }

      const userData = doc.data();
      const currentKeys = parseInt(userData.keys) || 0;
      const currentCurrency = parseInt(userData.currency) || 0;

      const newKeys = currentKeys + deltaKeys;
      const newCurrency = currentCurrency + deltaCurrency;

      if (newKeys < 0) {
        throw new functions.https.HttpsError("failed-precondition", "Yetersiz anahtar bakiyesi.");
      }
      if (newCurrency < 0) {
        throw new functions.https.HttpsError("failed-precondition", "Yetersiz elmas/enerji bakiyesi.");
      }

      transaction.update(userRef, {
        keys: newKeys,
        currency: newCurrency
      });

      return { keys: newKeys, currency: newCurrency };
    });

    return { success: true, keys: result.keys, currency: result.currency };
  } catch (error) {
    console.error("Wallet update failed:", error);
    throw new functions.https.HttpsError("internal", error.message || "C�zdan g�ncellenemedi.");
  }
});

// Liderlik Tablosu Skor Gönderme Fonksiyonu
// İstemciden gelen season parametresi tamamen görmezden gelinir.
// Sunucu kendi saat/tarihine göre doğru sezonu hesaplar → cihaz saati manipülasyonuna karşı koruma.
exports.submitLeaderboardScore = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }

  const uid = context.auth.uid;
  const partId = parseInt(data.partId);
  const lessonIndex = parseInt(data.lessonIndex);
  const recordScore = parseInt(data.recordScore);
  const displayName = typeof data.displayName === 'string' ? data.displayName.trim().slice(0, 127) || 'Kullanıcı' : 'Kullanıcı';
  const photoUrl = typeof data.photoUrl === 'string' ? data.photoUrl.slice(0, 511) : '';
  const titleUnit = typeof data.titleUnit === 'string' ? data.titleUnit.trim().slice(0, 127) || null : null;

  if (!Number.isFinite(partId) || !Number.isFinite(lessonIndex)) {
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz partId veya lessonIndex.');
  }
  if (!Number.isFinite(recordScore) || recordScore <= 0 || recordScore > 2000) {
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz recordScore (1-2000 aralığında olmalı).');
  }

  // Sezonu SUNUCU saatine göre hesapla — istemciye güvenilmez.
  const { currentSeason } = require('./seasonCalendar');
  const season = currentSeason(Date.now());
  const boardId = `part_${partId}_lesson_${lessonIndex}_season_${season}`;

  const boardRef = db.collection('lessonLeaderboards').doc(boardId);
  const entryRef = boardRef.collection('entries').doc(uid);

  try {
    await db.runTransaction(async (transaction) => {
      const boardSnap = await transaction.get(boardRef);
      const entrySnap = await transaction.get(entryRef);

      const previousBest = entrySnap.exists ? (entrySnap.data().recordScore || 0) : 0;
      if (recordScore <= previousBest) {
        // Rekor kırılmadı, hiçbir şey yazma
        return;
      }

      const entryData = {
        recordScore,
        recordLabel: String(recordScore),
        displayName,
        photoUrl,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };
      if (titleUnit) entryData.titleUnit = titleUnit;
      transaction.set(entryRef, entryData, { merge: true });

      // Tahta meta dokümanını sadece yoksa oluştur (create-only → hotspot yok)
      if (!boardSnap.exists) {
        const boardMeta = {
          partId,
          lessonIndex,
          season,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        };
        if (titleUnit) boardMeta.titleUnit = titleUnit;
        transaction.set(boardRef, boardMeta);
      }
    });

    return { success: true, season, boardId };
  } catch (error) {
    console.error('submitLeaderboardScore failed:', error);
    throw new functions.https.HttpsError('internal', error.message || 'Skor kaydedilemedi.');
  }
});

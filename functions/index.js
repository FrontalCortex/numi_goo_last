const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

// Load environment variables from .env file (for local testing)
// In production, use Firebase Secret Manager or environment variables
require('dotenv').config();

admin.initializeApp();
const db = admin.firestore();

// Config yükleme kontrolü için

function generateCode(length = 6) {
  // Sadece sayılar kullan (0-9) - daha kullanıcı dostu ve standart OTP formatı
  const digits = '0123456789';
  let code = '';
  for (let i = 0; i < length; i++) code += digits[Math.floor(Math.random() * digits.length)];
  return code;
}

// createTeacherInvite geçici olarak devre dışı (deploy hatası nedeniyle). İhtiyaç olursa tekrar açılabilir.
// exports.createTeacherInvite = functions.https.onCall(async (data, context) => { ... });

// checkEmailRegistered fonksiyonu kaldırıldı - silme işlemi için

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
  const now = admin.firestore.Timestamp.now();
  const nowMs = now.toMillis();
  const normalizedEmail = (email || '').trim().toLowerCase();
  if (!normalizedEmail) return { ok: true };

  const emailDocId = 'email:' + normalizedEmail.replace(/\//g, '_');
  const emailRef = db.collection('otpRateLimits').doc(emailDocId);

  const batch = db.batch();
  const emailSnap = await emailRef.get();
  const emailData = emailSnap.exists ? emailSnap.data() : {};
  const hourStart = getWindowStart(nowMs, HOUR_MS);
  const dayStart = getWindowStart(nowMs, DAY_MS);

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

  batch.set(emailRef, {
    lastHourStart: hourStart,
    hourCount: emailHourCount + 1,
    lastDayStart: dayStart,
    dayCount: emailDayCount + 1,
    updatedAt: now
  }, { merge: true });

  if (ip) {
    const safeIp = String(ip).replace(/[^a-fA-F0-9.:]/g, '_').slice(0, 64);
    const ipDocId = 'ip:' + safeIp;
    const ipRef = db.collection('otpRateLimits').doc(ipDocId);
    const ipSnap = await ipRef.get();
    const ipData = ipSnap.exists ? ipSnap.data() : {};
    const ipHourStart = getWindowStart(nowMs, HOUR_MS);
    let ipHourCount = (ipData.lastHourStart === ipHourStart ? ipData.hourCount : 0) || 0;

    if (ipHourCount >= OTP_IP_PER_HOUR) {
      const nextHourMs = ipHourStart + HOUR_MS;
      const waitMin = Math.ceil((nextHourMs - nowMs) / 60000);
      throw new functions.https.HttpsError(
        'resource-exhausted',
        `Bu cihazdan saatte en fazla ${OTP_IP_PER_HOUR} kod gönderilebilir. ${waitMin} dakika sonra tekrar deneyin.`
      );
    }
    batch.set(ipRef, {
      lastHourStart: ipHourStart,
      hourCount: ipHourCount + 1,
      updatedAt: now
    }, { merge: true });
  }

  await batch.commit();
  return { ok: true };
}

exports.sendStudentVerificationCode = functions.https.onCall(async (data, context) => {
  const email = (data && data.email) || '';
  const uid = (data && data.uid) || '';
  
  if (!email || !uid) {
    throw new functions.https.HttpsError('invalid-argument', 'email and uid required');
  }

  const ip = getClientIp(context);
  await checkOtpRateLimits(email, ip);

  const code = generateCode(6);
  const now = admin.firestore.Timestamp.now();
  const expiresAt = admin.firestore.Timestamp.fromMillis(now.toMillis() + 5 * 60 * 1000); // 5 dk

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
        <p>Bu kod 5 dakika içinde geçerlidir.</p>
      <p>Eğer bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
      <p>İyi çalışmalar,<br>NumiGoo Ekibi</p>
    `,
      text: `NumiGoo E-posta Doğrulama\n\nDoğrulama kodunuz: ${code}\nBu kod 5 dakika içinde geçerlidir.`
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
  const now = admin.firestore.Timestamp.now();
  const nowMs = now.toMillis();
  const docId = 'wrong:' + normalizedEmail.replace(/\//g, '_');
  const ref = db.collection('otpWrongAttempts').doc(docId);
  const snap = await ref.get();
  let count = 0;
  let windowStart = nowMs;
  if (snap.exists) {
    const d = snap.data();
    const prevStart = d.windowStart && d.windowStart.toMillis ? d.windowStart.toMillis() : 0;
    if (nowMs - prevStart < WRONG_ATTEMPT_COOLDOWN_MS) {
      count = (d.count || 0) + 1;
      windowStart = prevStart;
    }
  } else {
    count = 1;
  }
  await ref.set({ count, windowStart: admin.firestore.Timestamp.fromMillis(windowStart), updatedAt: now });
}

async function clearWrongAttempts(email) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  if (!normalizedEmail) return;
  const docId = 'wrong:' + normalizedEmail.replace(/\//g, '_');
  await db.collection('otpWrongAttempts').doc(docId).delete();
}

// OTP ile giriş: kodu doğrula ve custom token döndür
exports.verifyLoginCode = functions.https.onCall(async (data, context) => {
  const email = (data && data.email) || '';
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



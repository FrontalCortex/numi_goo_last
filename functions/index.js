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

// ─── OTP gönderimi ──────────────────────────────────────────────────────────
//
// GÜVENLİK MODELİ
//   • Kodun hedeflediği `uid` İSTEMCİDEN ALINMAZ. Eskiden alınıyordu ve doğrulanmıyordu;
//     bu, "kodu benim e-postama gönder ama kurbanın uid'ine bağla" saldırısına izin
//     veriyordu (verifyLoginCode o uid için token üretiyordu → tam hesap ele geçirme).
//     Artık uid, e-postadan SUNUCUDA çözülüyor.
//   • Üretilen kod yanıtta DÖNDÜRÜLMEZ. Eskiden `return { code }` yapılıyordu; bu,
//     kimliği doğrulanmamış herkesin herhangi bir e-posta için geçerli OTP almasını
//     sağlıyordu. Kod yalnızca e-posta ile ulaşır.
//   • `purpose` ne isterse istesin, hesabın gerçek durumu sunucuda doğrulanır:
//       register      → e-posta KAYITLI OLMAMALI
//       login         → kayıtlı olmalı ve rolü STUDENT olmalı
//       teacher_reset → kayıtlı olmalı ve rolü TEACHER olmalı

/** E-postaya karşılık gelen users dokümanını döndürür (yoksa null). */
async function findUserByEmail(email) {
  const snap = await db.collection('users').where('email', '==', email).limit(1).get();
  if (snap.empty) return null;
  const doc = snap.docs[0];
  return { uid: doc.get('uid') || doc.id, role: doc.get('role') || 'STUDENT' };
}

exports.sendStudentVerificationCode = functions.https.onCall(async (data, context) => {
  const rawEmail = (data && data.email) || '';
  const email = String(rawEmail).trim().toLowerCase();
  const rawPurpose = (data && data.purpose) ? String(data.purpose) : '';
  // Eski istemciler `purpose` göndermiyordu; `uid` alanının şekline bakarak niyeti
  // tahmin ediyoruz. Tahmin edilse bile uid ASLA istemciden alınmaz, aşağıda yeniden çözülür.
  const legacyUid = (data && data.uid) ? String(data.uid) : '';
  const purpose = rawPurpose || (legacyUid.startsWith('pending_') ? 'register' : 'login');

  if (!email) {
    throw new functions.https.HttpsError('invalid-argument', 'email required');
  }
  if (!['register', 'login', 'teacher_reset'].includes(purpose)) {
    throw new functions.https.HttpsError('invalid-argument', 'geçersiz purpose');
  }

  const ip = getClientIp(context);
  await checkOtpRateLimits(email, ip);

  // uid'i SUNUCUDA çöz — istemcinin gönderdiği değere asla güvenme.
  const existing = await findUserByEmail(email);
  let uid;
  if (purpose === 'register') {
    if (existing) {
      throw new functions.https.HttpsError(
        'already-exists',
        'Bu e-posta zaten kayıtlı. Lütfen giriş yapın.'
      );
    }
    uid = 'pending_' + require('crypto').randomUUID();
  } else {
    if (!existing) {
      throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı');
    }
    const requiredRole = purpose === 'teacher_reset' ? 'TEACHER' : 'STUDENT';
    if (existing.role !== requiredRole) {
      throw new functions.https.HttpsError('permission-denied', 'E-posta hatalı.');
    }
    uid = existing.uid;
  }

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
    // Kod artık yanıtta dönmediği için, e-posta gönderilemiyorsa kullanıcının kodu
    // öğrenmesinin hiçbir yolu yok. Sessizce "gönderildi" demek yerine hata veriyoruz ki
    // istemci OTP ekranında kilitlenmesin.
    console.error('Email credentials not set; cannot deliver code.', { user: !!user, pass: !!pass });
    await db.collection('studentVerificationCodes').doc(code).delete().catch(() => {});
    throw new functions.https.HttpsError('internal', 'Doğrulama kodu gönderilemedi.');
  }

  try {
  const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: { user, pass }
  });

  const mailOptions = {
    from: `Sorobit <${user}>`,
    to: email,
    subject: 'Sorobit - E-posta Doğrulama Kodu',
    html: `
      <h2>Sorobit E-posta Doğrulama</h2>
      <p>Merhaba,</p>
      <p>Sorobit hesabınızı doğrulamak için aşağıdaki kodu kullanın:</p>
      <h1 style="color: #4CAF50; font-size: 32px; letter-spacing: 5px; text-align: center;">${code}</h1>
        <p>Bu kod 2 dakika içinde geçerlidir.</p>
      <p>Eğer bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
      <p>İyi çalışmalar,<br>Sorobit Ekibi</p>
    `,
      text: `Sorobit E-posta Doğrulama\n\nDoğrulama kodunuz: ${code}\nBu kod 2 dakika içinde geçerlidir.`
  };

    console.log('Attempting to send email to:', email);
    const info = await transporter.sendMail(mailOptions);
    console.log('Email sent successfully:', { messageId: info.messageId, response: info.response });
  return { emailed: true };
  } catch (error) {
    console.error('Error sending email:', error);
    console.error('Error details:', { 
      message: error.message, 
      code: error.code,
      command: error.command,
      response: error.response,
      responseCode: error.responseCode
    });
    // Kod yanıtta dönmediği için, e-posta gidemediyse kullanıcı kodu asla öğrenemez.
    // Kullanılamayacak kodu temizleyip hata döndürüyoruz.
    await db.collection('studentVerificationCodes').doc(code).delete().catch(() => {});
    throw new functions.https.HttpsError('internal', 'Doğrulama kodu gönderilemedi.');
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

// ─── OTP ile kayıt ──────────────────────────────────────────────────────────
//
// GÜVENLİK MODELİ
//   • Kayıt bilgileri (ad, şifre, rol) artık `pendingRegistrations` dokümanından DEĞİL,
//     doğrudan bu çağrının gövdesinden okunur. Eskiden istemci şifreyi DÜZ METİN olarak
//     Firestore'a yazıyordu ve o koleksiyona kimliksiz yazma açıktı; saldırgan kurbanın
//     kayıt dokümanını kendi şifresiyle ezip hesabı devralabiliyordu.
//   • `auth/email-already-exists` durumunda ARTIK ŞİFRE DEĞİŞTİRİLMEZ. Eskiden
//     `updateUser({ password })` çağrılıyordu; bu, geçerli bir kod ele geçiren birinin
//     mevcut bir hesabın şifresini sıfırlamasına izin veren bir şifre sıfırlama yoluydu.
//     Yarım kalmış kayıt (Auth kaydı var, users dokümanı yok) hâlâ tamamlanabilir —
//     ama şifreye dokunulmadan.
exports.verifyRegistrationCode = functions.https.onCall(async (data, context) => {
  const rawEmail = (data && data.email) || '';
  const email = String(rawEmail).trim().toLowerCase();
  const code = (data && data.code) || '';
  const reqName = typeof (data && data.name) === 'string' ? data.name.trim().slice(0, 80) : '';
  const reqPassword = typeof (data && data.password) === 'string' ? data.password : '';
  const reqRole = (data && data.role) === 'TEACHER' ? 'TEACHER' : 'STUDENT';
  const reqBirthYear = Number.parseInt(data && data.birthYear, 10);
  const reqAcquisitionSource =
    typeof (data && data.acquisitionSource) === 'string'
      ? data.acquisitionSource.trim().slice(0, 64)
      : '';

  if (!email || !code) {
    throw new functions.https.HttpsError('invalid-argument', 'email and code required');
  }
  if (reqPassword && reqPassword.length < 6) {
    throw new functions.https.HttpsError('invalid-argument', 'Şifre en az 6 karakter olmalı');
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
    const name = reqName;
    const roleForUser = reqRole;
    // Şifre verilmediyse (OTP-only öğrenci kaydı) sunucu rastgele üretir; kullanıcı zaten
    // her girişte OTP kullanacak. Şifre hiçbir zaman Firestore'a yazılmaz.
    const password = reqPassword || require('crypto').randomUUID().replace(/-/g, '').slice(0, 16);

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
        // Auth kaydı zaten var. İki durum olabilir:
        //   (a) Yarım kalmış kayıt — users dokümanı yok → kaydı tamamla (şifreye DOKUNMA).
        //   (b) Gerçekten kayıtlı bir hesap → reddet. Aksi halde bu uç, geçerli bir kod
        //       ele geçiren birine şifre sıfırlama imkânı verirdi.
        userRecord = await admin.auth().getUserByEmail(email);
        const existingDoc = await db.collection('users').doc(userRecord.uid).get();
        if (existingDoc.exists) {
          throw new functions.https.HttpsError(
            'already-exists',
            'Bu e-posta zaten kayıtlı. Lütfen giriş yapın.'
          );
        }
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

      // birthYear artık SUNUCUDA yazılıyor. Eskiden kayıt sonrası istemci kendi
      // dokümanına update atıyordu; bu alan reklam yaş korumasını (TFCD/TFUA) belirlediği
      // için istemci yazımına kapatıldı (bkz. firestore.rules).
      if (Number.isFinite(reqBirthYear) && reqBirthYear > 1900 && reqBirthYear <= new Date().getFullYear()) {
        baseData.birthYear = reqBirthYear;
      }
      if (reqAcquisitionSource) {
        baseData.acquisitionSource = reqAcquisitionSource;
      }

      if (roleForUser === 'STUDENT') {
        baseData.verified = true;
      } else if (roleForUser === 'TEACHER') {
        baseData.teacherApproved = false;
      }

      await userRef.set(baseData);
    }

    // Eski sürümlerden kalmış olabilecek düz metin şifreli kayıt dokümanını temizle.
    await db.collection('pendingRegistrations').doc(email).delete().catch(() => {});
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



// ─── Herkese açık profil aynası ─────────────────────────────────────────────
//
// `users/{uid}` dokümanı artık yalnızca sahibi ve onaylı öğretmenler tarafından okunabiliyor
// (bkz. firestore.rules). Arkadaş arama, profil görüntüleme ve takipçi listelerinin ihtiyacı
// olan alanlar buradan `publicProfiles/{uid}` dokümanına aynalanır. İstemci bu koleksiyona
// yazamaz; tek yazan bu trigger'dır.
//
// Aynalanmayan (yani artık başkasına görünmeyen) alanlar: email, birthYear, deviceTokens,
// activeDeviceId, keys, currency, walletGuard, rewardGuard, energy_full_time, role,
// teacherApproved, acquisitionSource, kısıtlama/yasaklama alanları.
const PUBLIC_PROFILE_FIELDS = [
  'uid',
  'name',
  'userId',
  'selectedAvatar',
  'plan',
  'createdAt',
  'totalTimeSpent',
  'followersCount',
  'followingCount',
];

function buildPublicProfile(uid, userData) {
  const out = { uid };
  for (const field of PUBLIC_PROFILE_FIELDS) {
    if (userData[field] !== undefined && userData[field] !== null) {
      out[field] = userData[field];
    }
  }
  // Sayaçlar hiç yazılmamışsa arama sonuçlarında undefined kalmasın.
  if (out.followersCount === undefined) out.followersCount = 0;
  if (out.followingCount === undefined) out.followingCount = 0;
  return out;
}

exports.mirrorPublicProfile = functions.firestore
  .document('users/{uid}')
  .onWrite(async (change, context) => {
    const uid = context.params.uid;
    const publicRef = db.collection('publicProfiles').doc(uid);

    if (!change.after.exists) {
      await publicRef.delete().catch(() => {});
      return null;
    }

    const after = change.after.data() || {};
    const next = buildPublicProfile(uid, after);

    // Gereksiz yazımı (ve tetiklenen maliyeti) önlemek için sadece gerçekten değiştiyse yaz.
    if (change.before.exists) {
      const prev = buildPublicProfile(uid, change.before.data() || {});
      if (JSON.stringify(prev) === JSON.stringify(next)) return null;
    }

    await publicRef.set(next, { merge: false });
    return null;
  });

// Storage Rules, öğretmen kontrolü için Firestore'a cross-service `firestore.get()` ile
// bakıyordu; bu servisler-arası çağrı güvenilir çalışmadı (öğretmen kendi onaylı
// olduğunu doğrulayamıyor, başka öğrencinin soru medyasını indiremiyordu). Storage
// Rules'ın doğrudan ve güvenilir okuyabildiği tek yer ID token'ın kendisi olduğu için
// `teacherApproved`'ı buraya bir custom claim olarak da yazıyoruz.
exports.syncTeacherClaim = functions.firestore
  .document('users/{uid}')
  .onWrite(async (change, context) => {
    const uid = context.params.uid;
    if (!change.after.exists) return null;

    const before = change.before.exists ? change.before.data().teacherApproved === true : null;
    const after = change.after.data().teacherApproved === true;
    if (before === after) return null;

    await admin.auth().setCustomUserClaims(uid, { teacherApproved: after });
    return null;
  });

// ─── Takip sayaçları ────────────────────────────────────────────────────────
//
// `followersCount` / `followingCount` artık İSTEMCİ TARAFINDAN YAZILMIYOR. Eskiden
// firestore.rules'ta bu iki alan için uid kontrolü olmayan iki dal vardı ve giriş yapan
// herkes başkasının takipçi sayısını istediği değere çekebiliyordu.
//
// Sayaç, gerçek takip kaydının (followers/following alt koleksiyonları) oluşturulup
// silinmesine bağlı olarak burada güncellenir — yani sayı her zaman gerçek listeyle tutarlı.
function adjustCounter(userId, field, delta) {
  return db
    .collection('users')
    .doc(userId)
    .update({ [field]: admin.firestore.FieldValue.increment(delta) })
    .catch((err) => {
      // Kullanıcı bu arada silinmiş olabilir; hesap silme akışında bu normaldir.
      console.warn(`adjustCounter(${userId}, ${field}, ${delta}) atlandı:`, err.message);
    });
}

exports.onFollowerCreated = functions.firestore
  .document('users/{targetUserId}/followers/{followerUid}')
  .onCreate((snap, context) => adjustCounter(context.params.targetUserId, 'followersCount', 1));

exports.onFollowerDeleted = functions.firestore
  .document('users/{targetUserId}/followers/{followerUid}')
  .onDelete((snap, context) => adjustCounter(context.params.targetUserId, 'followersCount', -1));

exports.onFollowingCreated = functions.firestore
  .document('users/{ownerUserId}/following/{followingUid}')
  .onCreate((snap, context) => adjustCounter(context.params.ownerUserId, 'followingCount', 1));

exports.onFollowingDeleted = functions.firestore
  .document('users/{ownerUserId}/following/{followingUid}')
  .onDelete((snap, context) => adjustCounter(context.params.ownerUserId, 'followingCount', -1));

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

  // Aktif (henüz finalize edilmemiş) lessonLeaderboards tahtalarındaki entries/{uid} kaydı
  // istemciden silinemez (`allow write: if false`), burada Admin SDK ile temizlenmeli.
  // Aksi halde finalizeSeasonLeaderboardMedals bu hayalet kaydı bulup, silinmiş kullanıcı için
  // users/{uid}/badgeProgress/state dokümanını sezon ödülleriyle yeniden oluşturur (ölü veri kalır).
  try {
    const boardsSnap = await db.collection('lessonLeaderboards').get();
    const staleEntryRefs = boardsSnap.docs.map((boardDoc) =>
      boardDoc.ref.collection('entries').doc(uid),
    );
    for (let i = 0; i < staleEntryRefs.length; i += 450) {
      const chunk = staleEntryRefs.slice(i, i + 450);
      const batch = db.batch();
      chunk.forEach((ref) => batch.delete(ref));
      await batch.commit();
    }
    console.log(
      `Removed leaderboard entries for uid=${uid} across ${staleEntryRefs.length} board(s).`,
    );
  } catch (error) {
    console.error(`Error removing leaderboard entries for ${uid}:`, error);
  }
});
// ─── Cüzdan (altın / anahtar) güncelleme ────────────────────────────────────
//
// GÜVENLİK MODELİ
//   • Bakiyeyi ARTIRAN çağrılarda istemcinin gönderdiği miktara güvenilmez.
//     `reason` aşağıdaki WALLET_CREDIT_RULES kataloğunda tanımlı olmalı ve
//     miktar o gerekçe için izin verilen üst sınırı aşmamalıdır.
//   • Ayrıca saatlik tavan (WALLET_CREDIT_LIMITS) uygulanır; böylece geçerli
//     bir gerekçeyi betikle tekrar tekrar çağırıp bakiye şişirmek sınırlanır.
//   • Bakiyeyi AZALTAN çağrılar serbesttir: kullanıcı yalnızca kendi bakiyesini
//     harcar, negatife düşmesi zaten engellidir.
//   • Gerçek para ile satın alma bu fonksiyondan YAPILAMAZ. Play Billing
//     eklendiğinde ayrı bir doğrulama fonksiyonu yazılmalıdır; ayrıntılar:
//     docs/SATIN_ALMA_ENTEGRASYONU.md
//
// Üst sınırlar istemcideki ödül tablolarıyla birlikte güncellenmelidir.
const WALLET_CREDIT_RULES = {
  // NOT: `chest_reward` bilerek KALDIRILDI. Sandık/kristal ödülleri artık bu fonksiyondan
  // geçmiyor; zar sunucuda atılıyor ve bakiye `openChest` / `openCrystalReward` içinde
  // yazılıyor. Buraya geri eklenirse istemci yine kendi ödül miktarını seçebilir hale gelir.
  //
  // AbacusCustomizationFragment: Firestore kaydı başarısız olunca harcamanın geri alınması.
  // (Google Play para iadesiyle karıştırılmamalı — o reconcileVoidedPurchases'ta işlenir.)
  // En pahalı ürün 5000 altın, anahtarla alınan en pahalı boncuk 40 anahtar.
  purchase_rollback: { maxCurrency: 5000, maxKeys: 40 },
};

// Saatlik tavanlar. Tavana takılan çağrı reddedilir ve istemci bakiyeyi geri alır; bu yüzden
// sınırlar meşru bir oyuncunun ulaşamayacağı kadar YÜKSEK seçilmiştir. Gerçek kullanım verisi
// biriktikçe daraltılabilir.
//
// Referans: enerji yavaş dolduğu ve reklam dolgusu sınırlı olduğu için yoğun bir oyuncunun
// saatte ~40 ödül çağrısını aşması beklenmez; aşağıdaki değerler bunun ~2.5 katıdır.
// Bir harcamanın geri alınabileceği süre. Geri alma yalnızca o harcamanın jetonuyla ve
// birebir aynı miktarla yapılabildiği için bu pencere kısa tutulabilir.
const WALLET_ROLLBACK_WINDOW_MS = 10 * 60 * 1000;

const WALLET_CREDIT_LIMITS = {
  windowMs: 60 * 60 * 1000,
  maxCalls: 100,
  maxCurrency: 50000,
  maxKeys: 100,
};

exports.updateUserWallet = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }

  const uid = context.auth.uid;
  const payload = data || {};
  const deltaKeys = Number.parseInt(payload.keys, 10) || 0;
  const deltaCurrency = Number.parseInt(payload.currency, 10) || 0;
  const reason = typeof payload.reason === 'string' ? payload.reason.trim().slice(0, 64) : '';
  const rollbackToken =
    typeof payload.rollbackToken === 'string' ? payload.rollbackToken.trim().slice(0, 64) : '';

  const userRef = db.collection('users').doc(uid);

  if (deltaKeys === 0 && deltaCurrency === 0) {
    // Değişiklik yok — istemcinin önbelleğini sıfırlamamak için gerçek bakiyeyi döndür.
    const snapshot = await userRef.get();
    const current = snapshot.exists ? snapshot.data() : {};
    return {
      success: true,
      keys: Number.parseInt(current.keys, 10) || 0,
      currency: Number.parseInt(current.currency, 10) || 0,
    };
  }

  const isCredit = deltaKeys > 0 || deltaCurrency > 0;
  if (isCredit) {
    if (deltaKeys < 0 || deltaCurrency < 0) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Artırma ve azaltma aynı çağrıda birleştirilemez.'
      );
    }
    const rule = WALLET_CREDIT_RULES[reason];
    if (!rule) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Bu gerekçeyle bakiye artırılamaz: ' + (reason || '(boş)')
      );
    }
    if (deltaCurrency > rule.maxCurrency || deltaKeys > rule.maxKeys) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Ödül miktarı bu gerekçe için tanımlı üst sınırı aşıyor.'
      );
    }
    if (!rollbackToken) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Geri alma jetonu (rollbackToken) zorunludur.'
      );
    }
  }

  const now = Date.now();

  try {
    const result = await db.runTransaction(async (transaction) => {
      const doc = await transaction.get(userRef);
      if (!doc.exists) {
        throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
      }

      const userData = doc.data();
      const currentKeys = Number.parseInt(userData.keys, 10) || 0;
      const currentCurrency = Number.parseInt(userData.currency, 10) || 0;

      const newKeys = currentKeys + deltaKeys;
      const newCurrency = currentCurrency + deltaCurrency;

      // Yalnızca HARCAMA sıfırın altına inemez. Krediler negatif bakiyeye de uygulanabilmelidir:
      // Play iadesi geri alındığında bakiye eksiye düşebiliyor ve kullanıcının bu borcu
      // kazandığı ödüllerle kapatabilmesi gerekiyor. (bkz. reconcileVoidedPurchases)
      if (deltaKeys < 0 && newKeys < 0) {
        throw new functions.https.HttpsError('failed-precondition', 'Yetersiz anahtar bakiyesi.');
      }
      if (deltaCurrency < 0 && newCurrency < 0) {
        throw new functions.https.HttpsError('failed-precondition', 'Yetersiz altın bakiyesi.');
      }

      const update = { keys: newKeys, currency: newCurrency };
      const guardNow = userData.walletGuard || {};
      let issuedRollbackToken = null;

      if (!isCredit) {
        // HARCAMA: geri alınabilmesi için tek kullanımlık bir jeton üret. Geri alma yalnızca
        // bu jetonla ve aynı miktarla yapılabilir (aşağıya bkz.) — böylece "harcamadan
        // geri alma" ile bakiye şişirmek mümkün değildir.
        issuedRollbackToken = require('crypto').randomUUID();
        update.walletGuard = Object.assign({}, guardNow, {
          rollback: {
            token: issuedRollbackToken,
            keys: -deltaKeys,
            currency: -deltaCurrency,
            at: now,
          },
        });
      }

      if (isCredit) {
        // Geri alma, gerçekten yapılmış bir harcamayla eşleşmelidir.
        const pending = guardNow.rollback;
        if (!pending || pending.token !== rollbackToken) {
          throw new functions.https.HttpsError(
            'permission-denied',
            'Geri alma jetonu geçersiz veya kullanılmış.'
          );
        }
        if (now - (Number(pending.at) || 0) > WALLET_ROLLBACK_WINDOW_MS) {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'Geri alma süresi doldu.'
          );
        }
        if (deltaKeys !== (Number(pending.keys) || 0) || deltaCurrency !== (Number(pending.currency) || 0)) {
          throw new functions.https.HttpsError(
            'invalid-argument',
            'Geri alma miktarı harcamayla eşleşmiyor.'
          );
        }

        const guard = guardNow;
        const windowStart = Number(guard.windowStart) || 0;
        const windowExpired = now - windowStart >= WALLET_CREDIT_LIMITS.windowMs;

        const calls = (windowExpired ? 0 : Number(guard.calls) || 0) + 1;
        const creditedCurrency =
          (windowExpired ? 0 : Number(guard.currency) || 0) + deltaCurrency;
        const creditedKeys = (windowExpired ? 0 : Number(guard.keys) || 0) + deltaKeys;

        if (
          calls > WALLET_CREDIT_LIMITS.maxCalls ||
          creditedCurrency > WALLET_CREDIT_LIMITS.maxCurrency ||
          creditedKeys > WALLET_CREDIT_LIMITS.maxKeys
        ) {
          throw new functions.https.HttpsError(
            'resource-exhausted',
            'Saatlik ödül sınırına ulaşıldı. Lütfen daha sonra tekrar deneyin.'
          );
        }

        update.walletGuard = {
          windowStart: windowExpired ? now : windowStart,
          calls,
          currency: creditedCurrency,
          keys: creditedKeys,
          // Jeton kullanıldı; ikinci kez geri alınamaz.
          rollback: null,
        };
      }

      transaction.update(userRef, update);
      return { keys: newKeys, currency: newCurrency, rollbackToken: issuedRollbackToken };
    });

    return {
      success: true,
      keys: result.keys,
      currency: result.currency,
      rollbackToken: result.rollbackToken,
    };
  } catch (error) {
    // HttpsError'ı olduğu gibi ilet — aksi halde "Yetersiz bakiye" gibi anlamlı
    // hatalar istemciye `internal` olarak ulaşır.
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    console.error('Wallet update failed', { uid, reason, deltaKeys, deltaCurrency, error });
    throw new functions.https.HttpsError('internal', 'Cüzdan güncellenemedi.');
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

// ─── Google Play satın alma doğrulama ───────────────────────────────────────
//
// GÜVENLİK MODELİ
//   • İstemci ASLA miktar göndermez; yalnızca `productId` + `purchaseToken`.
//     Verilecek altın/anahtar aşağıdaki PLAY_PRODUCT_CATALOG'dan okunur.
//   • Token, Google Play Developer API ile doğrulanır: ürün gerçekten bu paket
//     için satın alınmış ve ödemesi tamamlanmış olmalıdır.
//   • Her token `processedPurchases/{token}` dokümanıyla bir kez bozdurulur;
//     doküman oluşturma bakiye yazımıyla AYNI transaction içindedir, bu yüzden
//     aynı token ikinci kez ödül veremez (replay koruması).
//   • Satın alma `updateUserWallet` üzerinden GEÇMEZ — o fonksiyon istemcinin
//     çağırdığı, gerekçe kataloğuyla sınırlanmış ayrı bir yoldur.
//
// Kurulum: docs/SATIN_ALMA_ENTEGRASYONU.md
const { google } = require('googleapis');

// Play Console'daki paket adı. `com.example.*` Play tarafından reddedilir;
// gerçek paket adına geçildiğinde ortam değişkeni olarak set edilmelidir:
//   firebase functions:config yerine .env / Secret Manager → ANDROID_PACKAGE_NAME
const ANDROID_PACKAGE_NAME = process.env.ANDROID_PACKAGE_NAME || '';

// Tüketilebilir ürünler: productId -> verilecek miktar.
// Play Console'daki ürün kimlikleriyle ve istemcideki BillingCatalog ile birebir aynı olmalı.
const PLAY_PRODUCT_CATALOG = {
  gold_1200: { currency: 1200, keys: 0 },
  gold_7000: { currency: 7000, keys: 0 },
  gold_15000: { currency: 15000, keys: 0 },
  keys_10: { currency: 0, keys: 10 },
  keys_50: { currency: 0, keys: 50 },
  keys_100: { currency: 0, keys: 100 },
};

// Abonelikler: productId -> users/{uid}.plan değeri.
const PLAY_SUBSCRIPTION_CATALOG = {
  pro_monthly: { plan: 'Pro' },
  lite_monthly: { plan: 'Lite' },
};

let androidPublisherClient = null;

/**
 * Play Developer API istemcisi. Cloud Functions'ın varsayılan servis hesabı kullanılır;
 * bu hesaba Play Console → Users and permissions üzerinden "View financial data" ve
 * "Manage orders and subscriptions" yetkisi verilmelidir.
 */
async function getAndroidPublisher() {
  if (androidPublisherClient) return androidPublisherClient;
  const auth = new google.auth.GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
  });
  androidPublisherClient = google.androidpublisher({ version: 'v3', auth });
  return androidPublisherClient;
}

function assertBillingConfigured() {
  if (!ANDROID_PACKAGE_NAME) {
    throw new functions.https.HttpsError(
      'failed-precondition',
      'Satın alma yapılandırması eksik: ANDROID_PACKAGE_NAME tanımlı değil.'
    );
  }
}

function readPurchaseArgs(data) {
  const payload = data || {};
  const productId = typeof payload.productId === 'string' ? payload.productId.trim().slice(0, 128) : '';
  const purchaseToken =
    typeof payload.purchaseToken === 'string' ? payload.purchaseToken.trim().slice(0, 1024) : '';
  if (!productId || !purchaseToken) {
    throw new functions.https.HttpsError('invalid-argument', 'productId ve purchaseToken zorunludur.');
  }
  return { productId, purchaseToken };
}

/**
 * Tüketilebilir ürün (altın / anahtar paketi) bozdurma.
 *
 * İstemci yalnızca hangi ürünü satın aldığını ve Play'den aldığı token'ı gönderir.
 * Ödül miktarı sunucudaki katalogdan gelir.
 */
exports.redeemGooglePlayPurchase = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }
  assertBillingConfigured();

  const uid = context.auth.uid;
  const { productId, purchaseToken } = readPurchaseArgs(data);

  const reward = PLAY_PRODUCT_CATALOG[productId];
  if (!reward) {
    throw new functions.https.HttpsError('invalid-argument', 'Tanımsız ürün: ' + productId);
  }

  // 1) Token'ı Play'e doğrulat.
  let purchase;
  try {
    const publisher = await getAndroidPublisher();
    const response = await publisher.purchases.products.get({
      packageName: ANDROID_PACKAGE_NAME,
      productId,
      token: purchaseToken,
    });
    purchase = response.data;
  } catch (error) {
    console.error('Play doğrulaması başarısız', { uid, productId, error: error.message });
    throw new functions.https.HttpsError('permission-denied', 'Satın alma doğrulanamadı.');
  }

  // purchaseState: 0 = satın alındı, 1 = iptal, 2 = beklemede
  if (purchase.purchaseState !== 0) {
    throw new functions.https.HttpsError('failed-precondition', 'Satın alma tamamlanmamış.');
  }

  // 2) Token'ı bir kez bozdur ve bakiyeyi aynı transaction'da yaz.
  const userRef = db.collection('users').doc(uid);
  const purchaseRef = db.collection('processedPurchases').doc(purchaseToken);

  try {
    const result = await db.runTransaction(async (transaction) => {
      const [userDoc, purchaseDoc] = await Promise.all([
        transaction.get(userRef),
        transaction.get(purchaseRef),
      ]);

      if (purchaseDoc.exists) {
        throw new functions.https.HttpsError('already-exists', 'Bu satın alma zaten işlendi.');
      }
      if (!userDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
      }

      const userData = userDoc.data();
      const newKeys = (Number.parseInt(userData.keys, 10) || 0) + reward.keys;
      const newCurrency = (Number.parseInt(userData.currency, 10) || 0) + reward.currency;

      transaction.set(purchaseRef, {
        uid,
        productId,
        type: 'product',
        orderId: purchase.orderId || null,
        grantedKeys: reward.keys,
        grantedCurrency: reward.currency,
        processedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      transaction.update(userRef, { keys: newKeys, currency: newCurrency });

      return { keys: newKeys, currency: newCurrency };
    });

    console.log('Satın alma işlendi', { uid, productId, orderId: purchase.orderId });
    return { success: true, keys: result.keys, currency: result.currency };
  } catch (error) {
    if (error instanceof functions.https.HttpsError) throw error;
    console.error('Satın alma işlenemedi', { uid, productId, error });
    throw new functions.https.HttpsError('internal', 'Satın alma işlenemedi.');
  }
});

/**
 * Abonelik (Pro / Lite) doğrulama ve yenileme.
 *
 * Tüketilebilirlerden farklı olarak TEKRAR TEKRAR çağrılabilir: uygulama her açıldığında
 * istemci elindeki aboneliği yeniden doğrulatır, sunucu da `plan` ve `planExpiresAt`
 * alanlarını günceller. Böylece iptal eden kullanıcı, süresi dolduğunda Pro olmaktan çıkar.
 */
exports.redeemGooglePlaySubscription = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }
  assertBillingConfigured();

  const uid = context.auth.uid;
  const { productId, purchaseToken } = readPurchaseArgs(data);

  const entry = PLAY_SUBSCRIPTION_CATALOG[productId];
  if (!entry) {
    throw new functions.https.HttpsError('invalid-argument', 'Tanımsız abonelik: ' + productId);
  }

  let subscription;
  try {
    const publisher = await getAndroidPublisher();
    const response = await publisher.purchases.subscriptionsv2.get({
      packageName: ANDROID_PACKAGE_NAME,
      token: purchaseToken,
    });
    subscription = response.data;
  } catch (error) {
    console.error('Abonelik doğrulaması başarısız', { uid, productId, error: error.message });
    throw new functions.https.HttpsError('permission-denied', 'Abonelik doğrulanamadı.');
  }

  // Yalnızca gerçekten aktif durumlar plan verir. Ödemesi bekleyen / askıya alınmış /
  // iptal edilip süresi dolmuş abonelikler Free'ye düşer.
  const activeStates = ['SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD'];
  const isActive = activeStates.includes(subscription.subscriptionState);

  const expiryRaw =
    (subscription.lineItems && subscription.lineItems.length > 0
      ? subscription.lineItems[subscription.lineItems.length - 1].expiryTime
      : null) || null;
  const expiryMs = expiryRaw ? Date.parse(expiryRaw) : 0;
  const stillValid = isActive && Number.isFinite(expiryMs) && expiryMs > Date.now();

  const userRef = db.collection('users').doc(uid);

  // Aynı token'ın başka bir hesaba bağlanmasını engelle: token daha önce başka bir uid ile
  // işlendiyse reddet. (Aynı uid tekrar doğrulatabilir — yenileme akışı bunu gerektirir.)
  const purchaseRef = db.collection('processedPurchases').doc(purchaseToken);

  try {
    await db.runTransaction(async (transaction) => {
      const [userDoc, purchaseDoc] = await Promise.all([
        transaction.get(userRef),
        transaction.get(purchaseRef),
      ]);

      if (purchaseDoc.exists && purchaseDoc.data().uid !== uid) {
        throw new functions.https.HttpsError(
          'permission-denied',
          'Bu abonelik başka bir hesaba tanımlı.'
        );
      }
      if (!userDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
      }

      transaction.set(
        purchaseRef,
        {
          uid,
          productId,
          type: 'subscription',
          subscriptionState: subscription.subscriptionState || null,
          expiryTime: expiryRaw,
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );

      transaction.update(userRef, {
        plan: stillValid ? entry.plan : 'Free',
        planExpiresAt: stillValid ? expiryMs : null,
        planProductId: stillValid ? productId : null,
      });
    });

    return {
      success: true,
      plan: stillValid ? entry.plan : 'Free',
      expiresAt: stillValid ? expiryMs : null,
    };
  } catch (error) {
    if (error instanceof functions.https.HttpsError) throw error;
    console.error('Abonelik işlenemedi', { uid, productId, error });
    throw new functions.https.HttpsError('internal', 'Abonelik işlenemedi.');
  }
});

// ─── Google Play para iadelerini geri alma ──────────────────────────────────
//
// SORUN
//   Kullanıcı 15000 altın satın alır, harcar, sonra Google'dan iade ister. İade
//   verildiğinde parası geri döner ama altını elinde kalır — üstelik tekrarlanabilir.
//
// ÇÖZÜM
//   Voided Purchases API günlük taranır; iade/iptal edilmiş her token için verilen
//   ödül geri alınır. Bakiyenin EKSİYE düşmesine izin verilir: sıfırda kesilseydi
//   "hepsini harca, sonra iade al" açığı olduğu gibi kalırdı. Kullanıcı kazandığı
//   ödüllerle borcunu kapatana kadar bir şey satın alamaz (updateUserWallet yalnızca
//   harcamayı sıfırın altına inmekten engeller, kredileri değil).
//
//   İşlem `processedPurchases/{token}.voided` ile bir kez yapılır; tarama penceresi
//   çakışsa bile ödül iki kez geri alınmaz.
//
// NOT: RTDN (Pub/Sub) yerine tarama tercih edildi — aynı servis hesabını kullanır,
// ek altyapı istemez ve iade geri alımı için saniyelik gecikme gerekmez. Google'ın
// self-servis iade penceresi 48 saat; günlük tarama bunun rahatça içinde kalır.

// Voided Purchases API varsayılan olarak son 30 günü döndürür ve işlem idempotenttir,
// bu yüzden ayrıca imleç (cursor) tutmaya gerek yoktur.
const MAX_VOIDED_PAGES = 20;

/** Tek bir iade kaydını işler. Zaten işlenmişse hiçbir şey yapmaz. */
async function reverseVoidedPurchase(voided) {
  const purchaseToken = voided.purchaseToken;
  if (!purchaseToken) return 'skipped';

  const purchaseRef = db.collection('processedPurchases').doc(purchaseToken);

  return db.runTransaction(async (transaction) => {
    const purchaseDoc = await transaction.get(purchaseRef);

    // Bu token'la hiç ödül vermediysek geri alacak bir şey yok.
    if (!purchaseDoc.exists) return 'unknown';

    const record = purchaseDoc.data();
    if (record.voided === true) return 'already';

    const uid = record.uid;
    if (!uid) return 'skipped';

    const userRef = db.collection('users').doc(uid);
    const userDoc = await transaction.get(userRef);
    if (!userDoc.exists) {
      // Hesap silinmiş; kaydı yine de işaretle ki tekrar tekrar denenmesin.
      transaction.set(
        purchaseRef,
        { voided: true, voidedAt: admin.firestore.FieldValue.serverTimestamp(), voidedNote: 'user-missing' },
        { merge: true }
      );
      return 'user-missing';
    }

    const userData = userDoc.data();
    const update = {};

    if (record.type === 'subscription') {
      // İade edilen abonelik: plan hemen düşer.
      update.plan = 'Free';
      update.planExpiresAt = null;
      update.planProductId = null;
    } else {
      // Tüketilebilir ürün: verilen miktar geri alınır, bakiye eksiye düşebilir.
      const grantedKeys = Number.parseInt(record.grantedKeys, 10) || 0;
      const grantedCurrency = Number.parseInt(record.grantedCurrency, 10) || 0;
      update.keys = (Number.parseInt(userData.keys, 10) || 0) - grantedKeys;
      update.currency = (Number.parseInt(userData.currency, 10) || 0) - grantedCurrency;
    }

    transaction.update(userRef, update);
    transaction.set(
      purchaseRef,
      {
        voided: true,
        voidedAt: admin.firestore.FieldValue.serverTimestamp(),
        voidedReason: voided.voidedReason != null ? voided.voidedReason : null,
        voidedSource: voided.voidedSource != null ? voided.voidedSource : null,
      },
      { merge: true }
    );

    return 'reversed';
  });
}

async function runVoidedPurchaseScan() {
  if (!ANDROID_PACKAGE_NAME) {
    console.warn('reconcileVoidedPurchases: ANDROID_PACKAGE_NAME tanımlı değil, atlanıyor.');
    return;
  }

  const publisher = await getAndroidPublisher();
  const counts = { reversed: 0, already: 0, unknown: 0, skipped: 0, 'user-missing': 0 };
  let pageToken;
  let pages = 0;

  do {
    const response = await publisher.purchases.voidedpurchases.list({
      packageName: ANDROID_PACKAGE_NAME,
      // type: 1 → tüketilebilir ürünlerin yanında iptal edilen abonelikleri de getirir.
      type: 1,
      maxResults: 1000,
      token: pageToken,
    });

    const rows = response.data.voidedPurchases || [];
    for (const voided of rows) {
      try {
        const outcome = await reverseVoidedPurchase(voided);
        counts[outcome] = (counts[outcome] || 0) + 1;
      } catch (error) {
        // Tek bir kaydın hatası taramanın tamamını düşürmesin.
        console.error('İade geri alınamadı', { purchaseToken: voided.purchaseToken, error });
      }
    }

    pageToken = response.data.tokenPagination && response.data.tokenPagination.nextPageToken;
    pages++;
  } while (pageToken && pages < MAX_VOIDED_PAGES);

  console.log('reconcileVoidedPurchases tamamlandı', counts);
}

exports.reconcileVoidedPurchases = functions
  .runWith({ timeoutSeconds: 540, memory: '512MB' })
  .pubsub.schedule('every 24 hours')
  .timeZone('Etc/UTC')
  .onRun(async () => {
    await runVoidedPurchaseScan();
    return null;
  });

// Testlerin zamanlayıcıyı beklemeden taramayı çalıştırabilmesi için.
exports._runVoidedPurchaseScan = runVoidedPurchaseScan;

// ─── Sunucu taraflı ödül çekilişi ───────────────────────────────────────────
//
// NEDEN
//   Önceden sandık/kristal ödülünün miktarını istemci belirliyor, sunucu yalnızca
//   "gerekçe için üst sınırı aşmasın" diye bakıyordu. Yani istemci her çağrıda
//   kataloğun tavanını (3000 altın) isteyebiliyordu. Artık zar SUNUCUDA atılır;
//   istemci ne isteyeceğini seçemez, yalnızca sonucu gösterir.
//
// SINIRI BİLEREK KABUL EDİYORUZ
//   Bu fonksiyonlar kullanıcının ödülü HAK EDİP ETMEDİĞİNİ doğrulamaz — bunun için
//   reklamın gerçekten izlendiğinin (AdMob SSV) ve ders/görev ilerlemesinin sunucuda
//   tutulması gerekir; ikisi de ayrı bir iştir. Şimdilik günlük tavanla sınırlanır:
//   çekiliş sunucuda olduğu için tek çağrının değeri düşer, tavan da toplam hasarı
//   bağlar. Ayrıntı: docs/SATIN_ALMA_ENTEGRASYONU.md
//
// Tablolar istemcideki NewChestFragment / ChestCrystalPolicy ile birebir aynı olmalıdır.

// Reklam DIŞI sandıklar (ders + görev) için günlük tavan.
//
// Bu tavan yalnızca KAÇAK KULLANIMI sınırlar; meşru oyuncuyu engellememelidir. Mevcut
// içerik 169 lessonItem / toplam 328 adım ve her adım ilk kez bitirildiğinde bir sandık
// veriyor. Pro kullanıcının enerjisi sınırsız olduğu için bir oyuncu teoride tüm içeriği
// tek günde bitirip ~328 sandık açabilir; tavan bunun ÜSTÜNDE olmak zorunda.
//
// NOT: Bu zayıf bir savunmadır. Doğru çözüm adım başına tek sandık (idempotent hak):
// içerik zaten sonlu olduğu için kaçak kullanım "günde 400" yerine "toplamda 328, bir kez"
// seviyesine iner. Bkz. docs/SATIN_ALMA_ENTEGRASYONU.md bölüm 6.
//
// Reklam sandıkları bu tavana GİRMEZ; onlar AdMob SSV ile doğrulanır.
const CHEST_DAILY_LIMIT = 400;
const CRYSTAL_DAILY_LIMIT = 20;
const CHEST_TAP_COUNT = 3; // Kullanıcının nadirlik yükseltmek için yaptığı tıklama sayısı

const CHEST_RARITIES = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY'];

/** 1..max arası tam sayı (her iki uç dahil). */
function rollInt(min, max) {
  return min + Math.floor(Math.random() * (max - min + 1));
}

/**
 * NewChestFragment.rollRarity ile aynı olasılıklar.
 *
 * DİKKAT: Kotlin tarafındaki yüzde yorumları koda uymuyor (ör. COMMON için "%15 RARE,
 * %80 COMMON" yazıyor ama eşikler %20/%75 veriyor). Buradaki eşikler istemcinin GERÇEK
 * davranışını birebir yansıtır; denge değiştirilmek istenirse iki taraf birlikte
 * güncellenmelidir.
 *
 *   COMMON -> %5 EPIC, %20 RARE, %75 COMMON
 *   RARE   -> %10 LEGENDARY, %20 EPIC, %70 RARE
 *   EPIC   -> %10 LEGENDARY, %90 EPIC
 */
function rollRarityUpgrade(current) {
  const rand = rollInt(1, 100);
  switch (current) {
    case 'COMMON':
      if (rand <= 5) return 'EPIC';
      if (rand <= 25) return 'RARE';
      return 'COMMON';
    case 'RARE':
      if (rand <= 10) return 'LEGENDARY';
      if (rand <= 30) return 'EPIC';
      return 'RARE';
    case 'EPIC':
      if (rand <= 10) return 'LEGENDARY';
      return 'EPIC';
    default:
      return 'LEGENDARY';
  }
}

/** NewChestFragment.showReward ile aynı ödül tablosu. */
function rollChestReward(rarity) {
  const rand = rollInt(1, 100);
  switch (rarity) {
    case 'COMMON':
      return { type: 'GOLD', amount: rollInt(50, 100) };
    case 'RARE':
      return { type: 'GOLD', amount: rollInt(150, 200) };
    case 'EPIC':
      return rand <= 50
        ? { type: 'GOLD', amount: rollInt(500, 700) }
        : { type: 'KEY', amount: 3 };
    case 'LEGENDARY':
      return rand <= 50
        ? { type: 'GOLD', amount: rollInt(2000, 3000) }
        : { type: 'KEY', amount: 5 };
    default:
      return { type: 'GOLD', amount: rollInt(50, 100) };
  }
}

/** ChestCrystalPolicy.resolveVideoName ile aynı dağılım. */
function rollCrystalVideo() {
  const roll = Math.floor(Math.random() * 1000); // 0..999
  if (roll < 500) return 'crystal_blue_blue';
  if (roll < 600) return 'crystal_blue_red';
  if (roll < 700) return 'crystal_red_red';
  if (roll < 766) return 'crystal_red_purple';
  if (roll < 816) return 'crystal_red_yellow';
  if (roll < 882) return 'crystal_blue_purple';
  if (roll < 948) return 'crystal_purple_purple';
  return 'crystal_purple_yellow';
}

const CRYSTAL_KEY_ELIGIBLE = new Set([
  'crystal_purple_yellow',
  'crystal_red_yellow',
  'crystal_purple_purple',
  'crystal_blue_purple',
  'crystal_red_purple',
]);

const CRYSTAL_KEY_AMOUNTS = {
  crystal_purple_yellow: 3,
  crystal_red_yellow: 3,
  crystal_purple_purple: 2,
  crystal_blue_purple: 2,
  crystal_red_purple: 2,
  crystal_red_red: 1,
  crystal_blue_red: 1,
};

const CRYSTAL_GOLD_RANGES = {
  crystal_blue_blue: [50, 100],
  crystal_blue_red: [150, 200],
  crystal_red_red: [150, 200],
  crystal_red_purple: [300, 400],
  crystal_red_yellow: [1000, 1500],
  crystal_blue_purple: [300, 400],
  crystal_purple_purple: [300, 400],
  crystal_purple_yellow: [1000, 1500],
};

/** ChestCrystalPolicy.resolveRewardForVideo ile aynı mantık (%50 anahtar şansı). */
function rollCrystalReward(videoName) {
  if (CRYSTAL_KEY_ELIGIBLE.has(videoName) && rollInt(0, 99) < 50) {
    return { type: 'KEY', amount: CRYSTAL_KEY_AMOUNTS[videoName] || 1 };
  }
  const range = CRYSTAL_GOLD_RANGES[videoName] || [50, 100];
  return { type: 'GOLD', amount: rollInt(range[0], range[1]) };
}

/** UTC gün anahtarı — günlük tavan sayacı için. */
function utcDayKey(nowMs) {
  return new Date(nowMs).toISOString().slice(0, 10);
}

/**
 * Ödülü kullanıcının bakiyesine yazar ve günlük sayacı aynı transaction'da artırır.
 * Tavan aşılmışsa ödül verilmez.
 */
async function grantRolledReward(uid, reward, counterField, dailyLimit, beforeGrant) {
  const userRef = db.collection('users').doc(uid);
  const now = Date.now();
  const dayKey = utcDayKey(now);

  return db.runTransaction(async (transaction) => {
    // Ek doğrulama (ör. reklam hakkını tüket) bakiye yazımıyla AYNI transaction icinde olmalı.
    // beforeGrant yalnızca OKUR ve doğrular; yazma işini döndürdüğü fonksiyon yapar.
    const commitBeforeGrant = beforeGrant ? await beforeGrant(transaction) : null;

    const doc = await transaction.get(userRef);
    if (!doc.exists) {
      throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
    }
    const userData = doc.data();

    const guard = userData.rewardGuard || {};
    const sameDay = guard.dayKey === dayKey;
    const used = (sameDay ? Number(guard[counterField]) || 0 : 0) + 1;

    // dailyLimit null ise tavan yok (AdMob SSV ile doğrulanmış reklam sandıkları).
    if (dailyLimit !== null && used > dailyLimit) {
      throw new functions.https.HttpsError(
        'resource-exhausted',
        'Günlük ödül sınırına ulaşıldı. Yarın tekrar deneyin.'
      );
    }

    const nextGuard = sameDay ? Object.assign({}, guard) : { dayKey };
    nextGuard.dayKey = dayKey;
    nextGuard[counterField] = used;

    const keys = (Number.parseInt(userData.keys, 10) || 0) + (reward.type === 'KEY' ? reward.amount : 0);
    const currency =
      (Number.parseInt(userData.currency, 10) || 0) + (reward.type === 'GOLD' ? reward.amount : 0);

    if (commitBeforeGrant) commitBeforeGrant(transaction);
    transaction.update(userRef, { keys, currency, rewardGuard: nextGuard });
    return { keys, currency };
  });
}

/**
 * Sandık açar: nadirlik yükseltme adımlarını ve ödülü SUNUCUDA çeker, bakiyeye yazar
 * ve istemciye yalnızca gösterilecek sonucu döndürür.
 *
 * İstemci `rarityPath`'i tıklama tıklama oynatır; böylece oyun hissi değişmez ama
 * sonucu istemci belirlemez.
 */
exports.openChest = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }
  const uid = context.auth.uid;

  const requested = typeof (data && data.startRarity) === 'string' ? data.startRarity : 'COMMON';
  // İstemci yalnızca BAŞLANGIÇ nadirliğini seçebilir ve bu da geçerli bir değer olmalı.
  // (Görev sandıkları RARE başlar; ödülü yine sunucu belirler.)
  const startRarity = CHEST_RARITIES.includes(requested) ? requested : 'COMMON';

  const rarityPath = [];
  let rarity = startRarity;
  for (let i = 0; i < CHEST_TAP_COUNT; i++) {
    rarity = rollRarityUpgrade(rarity);
    rarityPath.push(rarity);
  }

  const reward = rollChestReward(rarity);

  // Reklamla kazanılan sandıklar AdMob SSV ile doğrulanır ve günlük tavana girmez.
  // Reklam dışı sandıklar (ders/görev) doğrulanamadığı için tavana tabidir.
  const adNonce = typeof (data && data.adNonce) === 'string' ? data.adNonce.trim().slice(0, 128) : '';
  const balances = adNonce
    ? await grantRolledReward(uid, reward, 'adChests', null, (transaction) =>
        consumeAdEntitlement(transaction, uid, adNonce)
      )
    : await grantRolledReward(uid, reward, 'chests', CHEST_DAILY_LIMIT);

  return {
    success: true,
    startRarity,
    rarityPath,
    finalRarity: rarity,
    rewardType: reward.type,
    rewardAmount: reward.amount,
    keys: balances.keys,
    currency: balances.currency,
  };
});

/**
 * Kristal (günlük soru / görev) ödülü açar. Hangi videonun oynayacağını ve ödülü
 * sunucu çeker.
 */
exports.openCrystalReward = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }
  const uid = context.auth.uid;

  const videoName = rollCrystalVideo();
  const reward = rollCrystalReward(videoName);
  const balances = await grantRolledReward(uid, reward, 'crystals', CRYSTAL_DAILY_LIMIT);

  return {
    success: true,
    videoName,
    rewardType: reward.type,
    rewardAmount: reward.amount,
    keys: balances.keys,
    currency: balances.currency,
  };
});

// Testler için (zar fonksiyonlarının dağılımını doğrulamak üzere).
exports._rollRarityUpgrade = rollRarityUpgrade;
exports._rollChestReward = rollChestReward;
exports._rollCrystalVideo = rollCrystalVideo;
exports._rollCrystalReward = rollCrystalReward;

// ─── AdMob ödüllü reklam sunucu taraflı doğrulama (SSV) ─────────────────────
//
// NEDEN
//   Reklamla kazanılan sandık, oyunun tek SINIRSIZ ödül kaynağı. İstemcinin
//   "reklamı izledim" demesi doğrulanamadığı için betikle sonsuz sandık açılabiliyordu.
//   Artık reklamı AdMob'un kendisi bize haber veriyor: reklam tamamlandığında Google
//   aşağıdaki uç noktayı imzalı olarak çağırır, biz imzayı doğrulayıp tek kullanımlık
//   bir "sandık hakkı" yazarız. İstemci o hakkı `openChest` ile bozdurur.
//
// AKIŞ
//   İstemci reklam gösterirken custom_data = "<uid>:<nonce>" gönderir (nonce istemcide
//   üretilir ve yalnızca hangi hakkın kime ait olduğunu eşlemeye yarar; güvenlik imzadan
//   gelir). AdMob callback'i imzayı doğrularsa adRewards/{nonce} oluşturulur.
//
// KURULUM
//   AdMob konsolu → Ad unit → Server-side verification → bu fonksiyonun URL'i.
//   URL: https://<region>-<project>.cloudfunctions.net/admobRewardCallback

const ADMOB_VERIFIER_KEYS_URL = 'https://www.gstatic.com/admob/reward/verifier-keys.json';
const ADMOB_KEY_CACHE_MS = 24 * 60 * 60 * 1000;
// Callback'in ne kadar eski olabileceği. AdMob normalde saniyeler içinde çağırır;
// geniş bir pencere bırakıp tekrar denemelerine (retry) izin veriyoruz.
const ADMOB_CALLBACK_MAX_AGE_MS = 60 * 60 * 1000;
// Kazanılan sandık hakkının kullanılmadan durabileceği süre.
const AD_ENTITLEMENT_TTL_MS = 24 * 60 * 60 * 1000;

let admobKeyCache = { keys: null, fetchedAt: 0 };

async function getAdmobVerifierKeys() {
  const now = Date.now();
  if (admobKeyCache.keys && now - admobKeyCache.fetchedAt < ADMOB_KEY_CACHE_MS) {
    return admobKeyCache.keys;
  }
  const response = await fetch(ADMOB_VERIFIER_KEYS_URL);
  if (!response.ok) {
    throw new Error('Doğrulama anahtarları alınamadı: HTTP ' + response.status);
  }
  const body = await response.json();
  const byId = {};
  for (const key of body.keys || []) {
    byId[String(key.keyId)] = key.pem;
  }
  admobKeyCache = { keys: byId, fetchedAt: now };
  return byId;
}

/**
 * AdMob SSV imzasını doğrular.
 *
 * İmza, sorgu dizesinin BAŞINDAN `&signature=` parametresine kadar olan ham kısmı üzerinde
 * hesaplanır. Bu yüzden parametreler yeniden kodlanmadan, ham sorgu dizesi kullanılmalıdır.
 */
async function verifyAdmobSignature(rawQuery) {
  const signatureIndex = rawQuery.indexOf('&signature=');
  if (signatureIndex < 0) return false;

  const signedContent = rawQuery.substring(0, signatureIndex);
  const params = new URLSearchParams(rawQuery);
  const signature = params.get('signature');
  const keyId = params.get('key_id');
  if (!signature || !keyId) return false;

  const keys = await getAdmobVerifierKeys();
  const pem = keys[String(keyId)];
  if (!pem) {
    console.warn('AdMob SSV: bilinmeyen key_id', keyId);
    return false;
  }

  const verifier = require('crypto').createVerify('sha256');
  verifier.update(signedContent, 'utf8');
  return verifier.verify(pem, Buffer.from(signature, 'base64url'));
}

exports.admobRewardCallback = functions.https.onRequest(async (req, res) => {
  const rawUrl = req.originalUrl || req.url || '';
  const queryStart = rawUrl.indexOf('?');
  if (queryStart < 0) {
    res.status(400).send('missing query');
    return;
  }
  const rawQuery = rawUrl.substring(queryStart + 1);

  let valid = false;
  try {
    valid = await verifyAdmobSignature(rawQuery);
  } catch (error) {
    // Anahtar sunucusuna ulaşılamadı gibi geçici hatalar → AdMob tekrar denesin.
    console.error('AdMob SSV doğrulaması yapılamadı', error);
    res.status(500).send('verification unavailable');
    return;
  }

  if (!valid) {
    console.warn('AdMob SSV: geçersiz imza', { rawQuery });
    res.status(403).send('invalid signature');
    return;
  }

  const params = new URLSearchParams(rawQuery);
  const customData = params.get('custom_data') || '';
  const transactionId = params.get('transaction_id') || '';
  const timestampMs = Number(params.get('timestamp')) || 0;

  // custom_data = "<uid>:<nonce>"
  const separator = customData.indexOf(':');
  const uid = separator > 0 ? customData.substring(0, separator) : '';
  const nonce = separator > 0 ? customData.substring(separator + 1) : '';

  if (!uid || !nonce || !transactionId) {
    console.warn('AdMob SSV: eksik custom_data/transaction_id', { customData, transactionId });
    res.status(200).send('ignored');
    return;
  }

  // AdMob timestamp'i mikrosaniye cinsinden gönderir.
  const callbackMs = timestampMs > 1e14 ? Math.floor(timestampMs / 1000) : timestampMs;
  if (callbackMs && Math.abs(Date.now() - callbackMs) > ADMOB_CALLBACK_MAX_AGE_MS) {
    console.warn('AdMob SSV: çok eski callback', { callbackMs });
    res.status(200).send('stale');
    return;
  }

  try {
    // Doküman kimliği nonce olduğu için AdMob'un tekrar denemeleri yeni hak oluşturmaz.
    await db.collection('adRewards').doc(nonce).create({
      uid,
      transactionId,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      createdAtMs: Date.now(),
      consumed: false,
    });
    console.log('AdMob SSV: sandık hakkı verildi', { uid, transactionId });
  } catch (error) {
    if (error.code === 6 || error.code === 'already-exists') {
      // Aynı callback tekrar geldi — sorun değil.
      res.status(200).send('duplicate');
      return;
    }
    console.error('AdMob SSV: hak yazılamadı', error);
    res.status(500).send('write failed');
    return;
  }

  res.status(200).send('ok');
});

/**
 * Reklamla kazanılmış sandık hakkını doğrular ve tüketir.
 * Aynı transaction içinde tüketildiği için tek kullanımlıktır.
 */
async function consumeAdEntitlement(transaction, uid, nonce) {
  const ref = db.collection('adRewards').doc(nonce);
  const doc = await transaction.get(ref);

  if (!doc.exists) {
    // SSV callback'i henüz gelmemiş olabilir; istemci kısa süre sonra tekrar dener.
    throw new functions.https.HttpsError(
      'unavailable',
      'Reklam ödülü henüz doğrulanmadı. Birazdan tekrar deneyin.'
    );
  }
  const data = doc.data();
  if (data.uid !== uid) {
    throw new functions.https.HttpsError('permission-denied', 'Bu ödül başka bir hesaba ait.');
  }
  if (data.consumed === true) {
    throw new functions.https.HttpsError('already-exists', 'Bu reklam ödülü zaten kullanıldı.');
  }
  if (Date.now() - (Number(data.createdAtMs) || 0) > AD_ENTITLEMENT_TTL_MS) {
    throw new functions.https.HttpsError('failed-precondition', 'Reklam ödülünün süresi doldu.');
  }

  // Firestore transaction'larında TÜM okumalar yazmalardan önce gelmelidir. Bu yüzden
  // burada yalnızca okuyup doğruluyoruz; yazmayı çağıran, kendi okumalarını bitirdikten
  // sonra döndürdüğümüz fonksiyonla yapıyor.
  return (writeTransaction) => {
    writeTransaction.update(ref, {
      consumed: true,
      consumedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  };
}

// ─── Enerji (can) — sunucu taraflı ──────────────────────────────────────────
//
// NEDEN
//   Enerji `users/{uid}.energy_full_time` alanında tutuluyor ve daha önce istemci bu alanı
//   doğrudan yazabiliyordu. Yani reklam izlemeden can kazanmak ya da sınırsız can vererek
//   Pro'nun satış noktasını bedavaya geçmek mümkündü. Artık alan firestore.rules ile
//   kilitli ve yalnızca aşağıdaki fonksiyonlar yazabiliyor.
//
// MODEL (EnergyManager.kt ile birebir aynı olmalı)
//   energy_full_time = enerjinin dolacağı Unix zamanı (ms).
//   Mevcut enerji = maxEnergy - ceil((fullTime - now) / refreshMs)
//   Harcama fullTime'ı ileri iter, kazanç geri çeker.

const ENERGY_FIELD = 'energy_full_time';

function energyConfigForPlan(plan) {
  const isLite = plan === 'Lite';
  return {
    maxEnergy: isLite ? 10 : 5,
    refreshMs: (isLite ? 7 : 10) * 60 * 1000,
  };
}

/** Süresi geçmiş abonelik Free sayılır (MainActivity.checkSubscriptionAndUpdateEnergy ile aynı). */
function effectivePlan(userData) {
  const stored = userData.plan || 'Free';
  const expiresAt = Number(userData.planExpiresAt) || 0;
  if (expiresAt && expiresAt < Date.now()) return 'Free';
  return stored;
}

function hasInfiniteEnergy(userData) {
  const plan = effectivePlan(userData);
  const role = userData.role || '';
  const teacherApproved = userData.teacherApproved === true;
  // Onaysız öğretmen: enerji her zaman 0 (sonsuz değil).
  if (role === 'TEACHER' && !teacherApproved) return false;
  return teacherApproved || plan === 'Pro' || plan === 'Premium';
}

function computeCurrentEnergy(fullTime, now, cfg) {
  if (fullTime <= now) return cfg.maxEnergy;
  const missing = Math.ceil((fullTime - now) / cfg.refreshMs);
  return Math.max(0, cfg.maxEnergy - missing);
}

/** EnergyManager.getFullTime ile aynı üst sınır kırpması. */
function clampFullTime(fullTime, now, cfg) {
  const maxAllowed = now + cfg.maxEnergy * cfg.refreshMs;
  return Math.min(fullTime, maxAllowed);
}

/**
 * Enerjiyi değiştirir. [delta] negatifse harcama, pozitifse kazanç.
 * [beforeApply] varsa (ör. reklam hakkını doğrula) okuma aşamasında çalışır ve
 * yazma işini döndürdüğü fonksiyonla yapar.
 */
async function applyEnergyDelta(uid, delta, beforeApply, extraWrite) {
  const userRef = db.collection('users').doc(uid);
  const now = Date.now();

  return db.runTransaction(async (transaction) => {
    // Tüm okumalar yazmalardan önce.
    const commitBefore = beforeApply ? await beforeApply(transaction) : null;

    const doc = await transaction.get(userRef);
    if (!doc.exists) {
      throw new functions.https.HttpsError('not-found', 'Kullanıcı bulunamadı.');
    }
    const userData = doc.data();
    const cfg = energyConfigForPlan(effectivePlan(userData));

    const storedFullTime = Number(userData[ENERGY_FIELD]) || now;
    const fullTime = clampFullTime(storedFullTime, now, cfg);
    const infinite = hasInfiniteEnergy(userData);

    const update = {};
    let newFullTime = fullTime;

    if (infinite) {
      // Sonsuz enerjide harcama da kazanç da anlamsız; yalnızca yan işlemler yapılır.
      newFullTime = fullTime;
    } else if (delta < 0) {
      const amount = -delta;
      const current = computeCurrentEnergy(fullTime, now, cfg);
      if (current < amount) {
        throw new functions.https.HttpsError('failed-precondition', 'Yetersiz can.');
      }
      newFullTime = Math.max(fullTime, now) + amount * cfg.refreshMs;
      update[ENERGY_FIELD] = newFullTime;
    } else if (delta > 0) {
      if (fullTime > now) {
        newFullTime = Math.max(now, fullTime - delta * cfg.refreshMs);
        update[ENERGY_FIELD] = newFullTime;
      }
      // Zaten doluysa eklenecek bir şey yok.
    }

    if (commitBefore) commitBefore(transaction);
    if (extraWrite) Object.assign(update, extraWrite(userData));

    if (Object.keys(update).length > 0) {
      transaction.update(userRef, update);
    }

    return {
      energyFullTime: newFullTime,
      currentEnergy: infinite ? cfg.maxEnergy : computeCurrentEnergy(newFullTime, now, cfg),
      maxEnergy: cfg.maxEnergy,
      infinite,
    };
  });
}

/** Ders başlatırken can harcar. */
exports.spendEnergy = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }
  const amount = Number.parseInt(data && data.amount, 10) || 1;
  if (amount < 1 || amount > 20) {
    throw new functions.https.HttpsError('invalid-argument', 'Geçersiz can miktarı.');
  }
  const result = await applyEnergyDelta(context.auth.uid, -amount);
  return Object.assign({ success: true }, result);
});

/**
 * Reklam izleyerek can kazanma. AdMob SSV ile doğrulanmış tek kullanımlık hak gerekir —
 * reklamı izlemeden can kazanmak mümkün değildir.
 */
exports.claimAdEnergy = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }
  const uid = context.auth.uid;
  const adNonce = typeof (data && data.adNonce) === 'string' ? data.adNonce.trim().slice(0, 128) : '';
  if (!adNonce) {
    throw new functions.https.HttpsError('invalid-argument', 'Reklam doğrulaması eksik.');
  }

  const result = await applyEnergyDelta(uid, +1, (transaction) =>
    consumeAdEntitlement(transaction, uid, adNonce)
  );
  return Object.assign({ success: true }, result);
});

/**
 * Anahtar karşılığı can satın alma. Anahtar düşümü ve can eklemesi AYNI transaction'da
 * yapılır; önceki iki adımlı akışta çağrı arasında uygulama kapanırsa anahtar gidiyor
 * ama can gelmiyordu.
 */
exports.buyEnergyWithKeys = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'Oturum açmanız gerekiyor.');
  }
  const uid = context.auth.uid;

  const result = await applyEnergyDelta(uid, +1, null, (userData) => {
    const currentKeys = Number.parseInt(userData.keys, 10) || 0;
    if (currentKeys < ENERGY_KEY_COST) {
      throw new functions.https.HttpsError('failed-precondition', 'Yetersiz anahtar bakiyesi.');
    }
    return { keys: currentKeys - ENERGY_KEY_COST };
  });

  const snapshot = await db.collection('users').doc(uid).get();
  return Object.assign({ success: true, keys: Number.parseInt(snapshot.data().keys, 10) || 0 }, result);
});

// ShopFragment.LIFE_KEY_COST ile aynı olmalı.
const ENERGY_KEY_COST = 3;

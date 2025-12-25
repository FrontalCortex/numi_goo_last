const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');

admin.initializeApp();
const db = admin.firestore();

function generateCode(length = 6) {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let code = '';
  for (let i = 0; i < length; i++) code += chars[Math.floor(Math.random() * chars.length)];
  return code;
}

exports.createTeacherInvite = functions.https.onCall(async (data, context) => {
  const recipient = (data && data.recipient) || '';
  const candidateEmail = (data && data.candidateEmail) || '';
  if (!recipient) {
    throw new functions.https.HttpsError('invalid-argument', 'recipient missing');
  }

  const code = generateCode(6);
  const now = admin.firestore.Timestamp.now();
  const expiresAt = admin.firestore.Timestamp.fromMillis(now.toMillis() + 10 * 60 * 1000); // 10 dk

  await db.collection('teacherInvites').doc(code).set({
    code,
    recipient,
    candidateEmail,
    createdAt: now,
    expiresAt,
    used: false
  });

  const user = functions.config().email.user;
  const pass = functions.config().email.pass;
  if (!user || !pass) {
    console.warn('Email credentials not set; skipping email send.');
    return { code, emailed: false };
  }

  const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: { user, pass }
  });

  const mailOptions = {
    from: `NumiGoo <${user}>`,
    to: recipient,
    subject: 'Öğretmen Onay Kodu',
    text: `Yeni öğretmen başvurusu${candidateEmail ? ` (${candidateEmail})` : ''}. Kod: ${code}\n10 dakika içinde kullanılmalıdır.`,
  };

  await transporter.sendMail(mailOptions);
  return { code, emailed: true };
});

exports.sendStudentVerificationCode = functions.https.onCall(async (data, context) => {
  const email = (data && data.email) || '';
  const uid = (data && data.uid) || '';
  
  if (!email || !uid) {
    throw new functions.https.HttpsError('invalid-argument', 'email and uid required');
  }

  const code = generateCode(6);
  const now = admin.firestore.Timestamp.now();
  const expiresAt = admin.firestore.Timestamp.fromMillis(now.toMillis() + 10 * 60 * 1000); // 10 dk

  // Firestore'a kodu kaydet
  await db.collection('studentVerificationCodes').doc(code).set({
    code,
    email,
    uid,
    createdAt: now,
    expiresAt,
    used: false
  });

  const user = functions.config().email.user;
  const pass = functions.config().email.pass;
  if (!user || !pass) {
    console.warn('Email credentials not set; skipping email send.');
    return { code, emailed: false };
  }

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
      <p>Bu kod 10 dakika içinde geçerlidir.</p>
      <p>Eğer bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
      <p>İyi çalışmalar,<br>NumiGoo Ekibi</p>
    `,
    text: `NumiGoo E-posta Doğrulama\n\nDoğrulama kodunuz: ${code}\nBu kod 10 dakika içinde geçerlidir.`
  };

  await transporter.sendMail(mailOptions);
  return { code, emailed: true };
});



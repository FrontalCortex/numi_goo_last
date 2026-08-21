const fs = require('fs');
let content = fs.readFileSync('functions/index.js', 'utf8');
const marker = 'exports.verifyTeacherPasswordResetCode = functions.https.onCall(';

const newCode = 
// OTP ile kayıt: kodu doğrula, hesabı oluştur/onayla ve custom token döndür
exports.verifyRegistrationCode = functions.https.onCall(async (data, context) => {
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
      const userId = \\\\;

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

;
content = content.replace(marker, newCode + marker);
fs.writeFileSync('functions/index.js', content, 'utf8');
console.log('Successfully injected verifyRegistrationCode.');

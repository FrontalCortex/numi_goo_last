'use strict';

/**
 * Kullanıcıya giden e-postaların metinleri, dile göre (tr / en / id).
 *
 * NEDEN AYRI DOSYA
 *   Uygulama İngilizce ve Endonezce kullanılabiliyor (03.11.2026); kaydın ilk adımındaki doğrulama
 *   kodu ve 13 yaş altında veliye giden onay e-postası Türkçe kalıyordu. Veli onayı hukuki bir metin:
 *   veli okuyamadığı bir e-postayla "onay vermiş" sayılmamalı. İstemci kodu isterken `lang` gönderiyor
 *   (AuthManager); göndermeyen eski sürümler Türkçe alır.
 *
 * Veli onayı metni değişirse index.js'teki PARENTAL_CONSENT_VERSION da değişmeli; toplanan bilgiler
 * listesi gizlilik politikasıyla aynı kalmalı (public/privacy-policy*.html, Children bölümü). Üç dilde
 * aynı maddeler aynı sırada.
 */

const LANGS = new Set(['tr', 'en', 'id']);

/** İstemcinin gönderdiği dil; tanınmayan ya da yoksa Türkçe (eski sürümler). */
function mailLang(raw) {
  const l = typeof raw === 'string' ? raw.trim().toLowerCase() : '';
  if (l === 'in') return 'id';
  return LANGS.has(l) ? l : 'tr';
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

const CODE_STYLE = 'color: #4CAF50; font-size: 32px; letter-spacing: 5px; text-align: center;';

/** Kayıt ve giriş doğrulama kodu e-postası. */
function verificationEmail(code, lang = 'tr') {
  if (lang === 'en') {
    return {
      subject: 'Sorobit - Email Verification Code',
      html: `
      <h2>Sorobit Email Verification</h2>
      <p>Hello,</p>
      <p>Use the code below to verify your Sorobit account:</p>
      <h1 style="${CODE_STYLE}">${code}</h1>
      <p>This code is valid for 10 minutes.</p>
      <p>If you didn't request this, you can ignore this email.</p>
      <p>Happy learning,<br>The Sorobit Team</p>
    `,
      text: `Sorobit Email Verification\n\nYour verification code: ${code}\nThis code is valid for 10 minutes.`,
    };
  }
  if (lang === 'id') {
    return {
      subject: 'Sorobit - Kode Verifikasi Email',
      html: `
      <h2>Verifikasi Email Sorobit</h2>
      <p>Halo,</p>
      <p>Gunakan kode di bawah ini untuk memverifikasi akun Sorobit kamu:</p>
      <h1 style="${CODE_STYLE}">${code}</h1>
      <p>Kode ini berlaku selama 10 menit.</p>
      <p>Jika kamu tidak meminta kode ini, abaikan saja email ini.</p>
      <p>Selamat belajar,<br>Tim Sorobit</p>
    `,
      text: `Verifikasi Email Sorobit\n\nKode verifikasi kamu: ${code}\nKode ini berlaku selama 10 menit.`,
    };
  }
  return {
    subject: 'Sorobit - E-posta Doğrulama Kodu',
    html: `
      <h2>Sorobit E-posta Doğrulama</h2>
      <p>Merhaba,</p>
      <p>Sorobit hesabınızı doğrulamak için aşağıdaki kodu kullanın:</p>
      <h1 style="${CODE_STYLE}">${code}</h1>
        <p>Bu kod 10 dakika içinde geçerlidir.</p>
      <p>Eğer bu işlemi siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.</p>
      <p>İyi çalışmalar,<br>Sorobit Ekibi</p>
    `,
    text: `Sorobit E-posta Doğrulama\n\nDoğrulama kodunuz: ${code}\nBu kod 10 dakika içinde geçerlidir.`,
  };
}

const PARENTAL = {
  tr: {
    subject: 'Sorobit - Çocuğunuzun hesabı için veli onayı',
    title: 'Sorobit veli onayı',
    hello: 'Merhaba,',
    introSame: (c) => `Bu e-posta adresiyle (${c}) çocuğunuz için bir Sorobit hesabı açılmak isteniyor.`,
    introOther: (c) => `${c} adresiyle Sorobit'e kayıt olmak isteyen çocuğunuz, velisi olarak sizin e-postanızı verdi.`,
    introOtherText: (c) => `${c} adresiyle kayıt olmak isteyen çocuğunuz velisi olarak sizin e-postanızı verdi. `,
    about: 'Sorobit, çocuklara soroban (abaküs) ile zihinden aritmetik öğreten bir uygulamadır.\n      13 yaşından küçük çocukların hesabı velinin onayı olmadan açılmaz.',
    aboutText: '13 yaşından küçük çocukların hesabı velinin onayı olmadan açılmaz.',
    collectedTitle: 'Hesap açılırsa şu bilgiler toplanır ve saklanır:',
    collectedTitleText: 'Hesap açılırsa toplanacak bilgiler:',
    collected: [
      'Çocuğunuzun e-posta adresi, uygulamada görünen adı ve doğum yılı',
      'Kayıtta verdiği iki cevap: Sorobit\'i nereden duyduğu ve abaküs bilgisi (hiç / biraz / iyi)',
      'Ders ilerlemesi, puanlar, seri ve kupa bilgileri',
      'Adı ve avatarı diğer kullanıcılara görünür (profil, sıralama tabloları, takip listeleri)',
      'Öğretmene soru sohbetinde gönderdiği mesajlar, fotoğraflar, videolar ve mikrofon sesi içeren ekran kayıtları (öğretmen bunları görür)',
      'Öğretmeninin verdiği kodu girerek ya da davetini kabul ederek bir kurs sınıfına katılırsa, o sınıfın öğretmeni adını, kullanıcı adını ve ders ilerlemesini görür, sorularını da o öğretmen cevaplar. Çocuğunuz sınıftan dilediği an ayrılabilir',
      'Bildirim göndermek için cihaz bildirim kimliği',
      'Uygulama kullanım istatistikleri (yaş grubu ve abaküs bilgisiyle birlikte, adı ve e-postası olmadan); reklamlar çocuk ayarıyla, kişiselleştirilmeden gösterilir',
    ],
    rights: (url, mail) => `Ayrıntılar: <a href="${url}">Gizlilik Politikası</a>. Dilediğiniz zaman çocuğunuzdan toplanan
      bilgileri görmeyi, silinmesini ya da onayınızı geri almayı (hesabın silinmesi) isteyebilirsiniz:
      bu e-posta adresinden <a href="mailto:${mail}">${mail}</a> adresine yazın. Hesap uygulamadaki
      Hesap Ayarları'ndan da silinebilir; silinince bu bilgiler de silinir.`,
    consent: `<b>Onaylıyorsanız</b> aşağıdaki kodu uygulamadaki veli onayı ekranına girin. Kodu girmek, yukarıdaki bilgilerin
      toplanmasına veli olarak onay verdiğiniz anlamına gelir.`,
    expiry: 'Bu kod 30 dakika geçerlidir. Onaylamıyorsanız hiçbir şey yapmayın: hesap açılmaz, kayıt isteği kendiliğinden silinir.',
    team: 'Sorobit Ekibi',
    text: (url, mail, code) =>
      `Ayrıntılar: ${url}\n` +
      `Bilgileri görmek, sildirmek ya da onayı geri almak için: ${mail}\n\n` +
      `Onaylıyorsanız şu kodu uygulamaya girin: ${code}\n` +
      `Kodu girmek bu bilgilerin toplanmasına onay verdiğiniz anlamına gelir. Kod 30 dakika geçerlidir. ` +
      `Onaylamıyorsanız hiçbir şey yapmayın; hesap açılmaz.`,
    policyPage: 'privacy-policy-tr.html',
    policyLabel: 'Gizlilik Politikası',
  },
  en: {
    subject: "Sorobit - Parent approval for your child's account",
    title: 'Sorobit parent approval',
    hello: 'Hello,',
    introSame: (c) => `Someone wants to create a Sorobit account for your child with this email address (${c}).`,
    introOther: (c) => `Your child, who wants to sign up for Sorobit with the address ${c}, gave your email address as their parent.`,
    introOtherText: (c) => `Your child, who wants to sign up with the address ${c}, gave your email address as their parent. `,
    about: 'Sorobit is an app that teaches children mental arithmetic with the soroban (abacus).\n      Accounts for children under 13 are not created without a parent\'s approval.',
    aboutText: "Accounts for children under 13 are not created without a parent's approval.",
    collectedTitle: 'If the account is created, the following information is collected and stored:',
    collectedTitleText: 'Information collected if the account is created:',
    collected: [
      "Your child's email address, the name shown in the app and year of birth",
      'Two answers given at sign-up: how they heard about Sorobit and how well they know the abacus (not at all / a little / well)',
      'Lesson progress, scores, streak and trophy information',
      'Their name and avatar are visible to other users (profile, leaderboards, follow lists)',
      'Messages, photos, videos and screen recordings with microphone audio sent to a teacher in the question chat (the teacher sees these)',
      "If they join a course class by entering their teacher's code or accepting an invite, that class's teacher sees their name, username and lesson progress, and that teacher answers their questions. Your child can leave the class at any time",
      'Device notification ID, used to send notifications',
      'App usage statistics (with age group and abacus knowledge, without name or email); ads are shown with child settings and are not personalized',
    ],
    rights: (url, mail) => `Details: <a href="${url}">Privacy Policy</a>. At any time you can ask to see the
      information collected from your child, have it deleted, or withdraw your approval (account deletion):
      write from this email address to <a href="mailto:${mail}">${mail}</a>. The account can also be deleted in the
      app's Account Settings; when it is deleted, this information is deleted too.`,
    consent: `<b>If you approve</b>, enter the code below on the parent approval screen in the app. Entering the code means
      that, as the parent, you approve the collection of the information above.`,
    expiry: "This code is valid for 30 minutes. If you don't approve, do nothing: the account won't be created and the sign-up request is deleted automatically.",
    team: 'The Sorobit Team',
    text: (url, mail, code) =>
      `Details: ${url}\n` +
      `To see the information, have it deleted or withdraw your approval: ${mail}\n\n` +
      `If you approve, enter this code in the app: ${code}\n` +
      `Entering the code means you approve the collection of this information. The code is valid for 30 minutes. ` +
      "If you don't approve, do nothing; the account won't be created.",
    policyPage: 'privacy-policy.html',
    policyLabel: 'Privacy Policy',
  },
  id: {
    subject: 'Sorobit - Persetujuan orang tua untuk akun anak Anda',
    title: 'Persetujuan orang tua Sorobit',
    hello: 'Halo,',
    introSame: (c) => `Ada permintaan untuk membuat akun Sorobit bagi anak Anda dengan alamat email ini (${c}).`,
    introOther: (c) => `Anak Anda, yang ingin mendaftar ke Sorobit dengan alamat ${c}, memberikan email Anda sebagai orang tuanya.`,
    introOtherText: (c) => `Anak Anda, yang ingin mendaftar dengan alamat ${c}, memberikan email Anda sebagai orang tuanya. `,
    about: 'Sorobit adalah aplikasi yang mengajarkan aritmetika mental kepada anak-anak dengan soroban (sempoa).\n      Akun untuk anak di bawah 13 tahun tidak dibuat tanpa persetujuan orang tua.',
    aboutText: 'Akun untuk anak di bawah 13 tahun tidak dibuat tanpa persetujuan orang tua.',
    collectedTitle: 'Jika akun dibuat, informasi berikut akan dikumpulkan dan disimpan:',
    collectedTitleText: 'Informasi yang dikumpulkan jika akun dibuat:',
    collected: [
      'Alamat email anak Anda, nama yang ditampilkan di aplikasi, dan tahun lahir',
      'Dua jawaban saat pendaftaran: dari mana mengetahui Sorobit dan seberapa paham sempoa (belum sama sekali / sedikit / paham)',
      'Kemajuan pelajaran, skor, streak, dan informasi piala',
      'Nama dan avatarnya terlihat oleh pengguna lain (profil, papan peringkat, daftar ikuti)',
      'Pesan, foto, video, dan rekaman layar dengan suara mikrofon yang dikirim ke guru di obrolan pertanyaan (guru dapat melihatnya)',
      'Jika bergabung dengan kelas kursus dengan memasukkan kode dari gurunya atau menerima undangan, guru kelas tersebut dapat melihat nama, nama pengguna, dan kemajuan pelajarannya, serta guru tersebut menjawab pertanyaannya. Anak Anda dapat keluar dari kelas kapan saja',
      'ID notifikasi perangkat, untuk mengirim notifikasi',
      'Statistik penggunaan aplikasi (bersama kelompok usia dan pemahaman sempoa, tanpa nama atau email); iklan ditampilkan dengan pengaturan anak dan tidak dipersonalisasi',
    ],
    rights: (url, mail) => `Rincian: <a href="${url}">Kebijakan Privasi</a>. Kapan saja Anda dapat meminta untuk melihat
      informasi yang dikumpulkan dari anak Anda, menghapusnya, atau menarik persetujuan Anda (penghapusan akun):
      kirim email dari alamat ini ke <a href="mailto:${mail}">${mail}</a>. Akun juga dapat dihapus di Pengaturan Akun
      dalam aplikasi; jika dihapus, informasi ini juga dihapus.`,
    consent: `<b>Jika Anda setuju</b>, masukkan kode di bawah ini pada layar persetujuan orang tua di aplikasi. Memasukkan kode
      berarti Anda, sebagai orang tua, menyetujui pengumpulan informasi di atas.`,
    expiry: 'Kode ini berlaku selama 30 menit. Jika Anda tidak setuju, abaikan saja: akun tidak akan dibuat dan permintaan pendaftaran akan terhapus otomatis.',
    team: 'Tim Sorobit',
    text: (url, mail, code) =>
      `Rincian: ${url}\n` +
      `Untuk melihat informasi, menghapusnya, atau menarik persetujuan: ${mail}\n\n` +
      `Jika Anda setuju, masukkan kode ini di aplikasi: ${code}\n` +
      `Memasukkan kode berarti Anda menyetujui pengumpulan informasi ini. Kode berlaku selama 30 menit. ` +
      'Jika Anda tidak setuju, abaikan saja; akun tidak akan dibuat.',
    policyPage: 'privacy-policy-id.html',
    policyLabel: 'Kebijakan Privasi',
  },
};

/** Veliye giden onay e-postası (13 yaş altı). [supportEmail]: index.js SUPPORT_EMAIL. */
function parentalConsentEmail(code, childEmail, parentEmail, supportEmail, lang = 'tr') {
  const t = PARENTAL[lang] || PARENTAL.tr;
  const child = escapeHtml(childEmail);
  // Hesap velinin kendi adresiyle açılıyorsa "çocuğunuz sizin e-postanızı verdi" demek yanlış olur.
  const sameAddress = childEmail === parentEmail;
  const introHtml = sameAddress
    ? t.introSame(`<b>${child}</b>`)
    : t.introOther(`<b>${child}</b>`);
  const introText = sameAddress ? `${t.introSame(childEmail)} ` : t.introOtherText(childEmail);
  const listHtml = t.collected.map((item) => `<li>${escapeHtml(item)}</li>`).join('');
  const listText = t.collected.map((item) => `- ${item}`).join('\n');
  const policyUrl = `https://numigo-new.web.app/${t.policyPage}#pp-5`;
  return {
    subject: t.subject,
    html: `
      <h2>${t.title}</h2>
      <p>${t.hello}</p>
      <p>${introHtml}
      ${t.about}</p>
      <p>${t.collectedTitle}</p>
      <ul>${listHtml}</ul>
      <p>${t.rights(policyUrl, supportEmail)}</p>
      <p>${t.consent}</p>
      <h1 style="${CODE_STYLE}">${code}</h1>
      <p>${t.expiry}</p>
      <p>${t.team}</p>
    `,
    text:
      `${t.title}\n\n${introText}` +
      `${t.aboutText}\n\n` +
      `${t.collectedTitleText}\n${listText}\n\n${t.text(policyUrl, supportEmail, code)}`,
  };
}

module.exports = { mailLang, verificationEmail, parentalConsentEmail, PARENTAL };

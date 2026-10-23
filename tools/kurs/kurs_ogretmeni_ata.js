#!/usr/bin/env node
// Kurs açar ve öğretmen hesaplarını o kursa bağlar (ya da bağı kaldırır).
//
// Kursa bağlı öğretmen ("kurs öğretmeni") uygulamadan sınıf açabilir ve yalnızca kendi
// sınıflarının sorularını görür; genel havuzu ancak uygulamadaki "genel havuza da yardım et"
// ayarını açarsa (bkz. functions/classes.js). Bağ `users/{uid}.courseId` alanında; alan
// sunucuya ait, istemci yazamıyor — öğretmenin kendini bir kursa bağlayamaması için.
//
// Kullanım (proje kökünden):
//   node tools/kurs/kurs_ogretmeni_ata.js --kurs menar-kadikoy --ad "Menar Kadıköy" ayse_hoca 123456AB
//       → KURU ÇALIŞMA: kurs yoksa açılacağını, kimin bağlanacağını gösterir; hiçbir şey yazmaz.
//   ... --yaz
//       → gerçekten yazar: courses/{kurs} (yoksa) ve öğretmenlerin courseId'si.
//   node tools/kurs/kurs_ogretmeni_ata.js --kurs menar-kadikoy --geri-al ayse_hoca --yaz
//       → öğretmeni kurstan çıkarır: courseId silinir, açtığı sınıflar kapanır (archived) ve
//         o sınıflarda bekleyen sorular genel havuza taşınır. Sınıflardaki öğrencilerin yeni
//         soruları da genele düşer (kapanmış sınıf soru almıyor).
//   node tools/kurs/kurs_ogretmeni_ata.js --bagimsiz 746609WU --yaz
//       → BAĞIMSIZ ÖĞRETMEN: bir kursa bağlı olmadan kendi öğrencileriyle çalışan öğretmen için
//         tek kişilik kurs. Kimlik kullanıcı adından (`ogretmen-746609wu`), ad öğretmenin
//         adından gelir (öğrenci sınıf satırında bu adı görür); --ad ile değiştirilebilir.
//         Tek öğretmen alır. Geri almak: --bagimsiz 746609WU --geri-al --yaz
//   node tools/kurs/kurs_ogretmeni_ata.js --liste
//       → kursları ve bağlı öğretmenleri listeler.
//
// Onay bekleyen öğretmen de bağlanabilir (uyarı verilir): onay gelince doğrudan kurs
// öğretmeni olarak başlar. Öğrenci hesabı bağlanmaz.

const { fail, openFirestore, loadClasses, normalizeName, usersByName } = require('./ortak');

function parseArgs(argv) {
  const out = { adlar: [], yaz: false, geriAl: false, liste: false, bagimsiz: false, kurs: '', ad: '' };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--yaz') out.yaz = true;
    else if (a === '--geri-al') out.geriAl = true;
    else if (a === '--liste') out.liste = true;
    else if (a === '--bagimsiz') out.bagimsiz = true;
    else if (a === '--kurs') out.kurs = argv[++i] || '';
    else if (a === '--ad') out.ad = (argv[++i] || '').trim();
    else if (a.startsWith('--')) fail(`Bilinmeyen seçenek: ${a}`);
    else out.adlar.push(a);
  }
  if (out.liste) return out;
  if (out.bagimsiz) {
    // Kimlik ve ad öğretmenden çıkarılıyor (bkz. bagimsizKurs); burada yalnızca tek kişi şartı.
    if (out.kurs) fail('--bagimsiz ile --kurs verilmez; kurs kimliği kullanıcı adından çıkarılır.');
    if (out.adlar.length !== 1) fail('--bagimsiz tek bir öğretmen kullanıcı adı alır.');
    return out;
  }
  // Kurs kimliği Console'da ve betik çıktılarında elle okunuyor; kısa ve okunur olsun.
  if (!/^[a-z0-9-]{3,40}$/.test(out.kurs)) fail('--kurs küçük harf, rakam ve tire (3–40), ör. menar-kadikoy');
  if (!out.adlar.length) fail('En az bir öğretmen kullanıcı adı ver.');
  return out;
}

async function liste(db) {
  const [courses, teachers] = await Promise.all([
    db.collection('courses').get(),
    db.collection('users').where('role', '==', 'TEACHER').get(),
  ]);
  if (courses.empty) console.log('Hiç kurs yok.');
  courses.docs.forEach((c) => {
    const bagli = teachers.docs.filter((t) => t.get('courseId') === c.id);
    const tur = c.get('independent') === true ? '  [bağımsız öğretmen]' : '';
    console.log(`${c.id}  "${c.get('name') || ''}"  (${bagli.length} öğretmen)${tur}`);
    bagli.forEach((t) => console.log(`    ${t.get('userId')}  ${t.get('name') || ''}${t.get('teacherApproved') === true ? '' : '  [onay bekliyor]'}`));
  });
}

/**
 * Bağımsız öğretmenin tek kişilik kursu: kimlik `ogretmen-<kullanıcı adı>`, ad öğretmenin adı.
 * Kullanıcı adı rakam + büyük harf (generateUniqueUserId), küçültünce kimlik kuralına uyuyor.
 */
function bagimsizKurs(opts, adaGore) {
  const bulunan = adaGore.get(normalizeName(opts.adlar[0])) || [];
  if (bulunan.length !== 1) fail(`${opts.adlar[0]}: ${bulunan.length ? `${bulunan.length} hesapta aynı ad` : 'bulunamadı'}`);
  const u = bulunan[0].data();
  if (u.role !== 'TEACHER') fail(`${opts.adlar[0]}: öğretmen değil (role=${u.role || '-'})`);
  const kurs = `ogretmen-${normalizeName(u.userId)}`;
  if (!/^[a-z0-9-]{3,40}$/.test(kurs)) fail(`Kullanıcı adından kurs kimliği çıkarılamadı: ${u.userId}`);
  return { kurs, ad: opts.ad || String(u.name || u.userId).trim() };
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const { admin, db } = openFirestore();
  if (opts.liste) return liste(db);

  const classes = loadClasses();
  const adaGore = await usersByName(db);
  if (opts.bagimsiz) Object.assign(opts, bagimsizKurs(opts, adaGore));
  const courseRef = db.collection('courses').doc(opts.kurs);
  const courseSnap = await courseRef.get();
  if (!courseSnap.exists && opts.geriAl) fail(`Kurs yok: ${opts.kurs}`);
  if (!courseSnap.exists && !opts.ad) fail(`Kurs yok: ${opts.kurs}. Açmak için --ad "Kursun adı" ver.`);

  console.log(opts.yaz ? 'Kip: YAZ\n' : 'Kip: KURU ÇALIŞMA (hiçbir şey yazılmayacak; yazmak için --yaz)\n');
  if (!courseSnap.exists) console.log(`Kurs açılacak: ${opts.kurs} "${opts.ad}"`);
  else console.log(`Kurs: ${opts.kurs} "${courseSnap.get('name') || ''}"${opts.ad && opts.ad !== courseSnap.get('name') ? ` (ad "${opts.ad}" olarak güncellenecek)` : ''}`);
  const yapilacak = [];
  const atlanan = [];
  for (const ham of new Set(opts.adlar.map(normalizeName))) {
    const bulunan = adaGore.get(ham) || [];
    if (bulunan.length !== 1) { atlanan.push([ham, bulunan.length ? `${bulunan.length} hesapta aynı ad` : 'bulunamadı']); continue; }
    const doc = bulunan[0];
    const u = doc.data();
    if (u.role !== 'TEACHER') { atlanan.push([ham, `öğretmen değil (role=${u.role || '-'})`]); continue; }
    if (opts.geriAl) {
      if (u.courseId !== opts.kurs) { atlanan.push([ham, `bu kursa bağlı değil (courseId=${u.courseId || '-'})`]); continue; }
    } else if (u.courseId === opts.kurs) {
      atlanan.push([ham, 'zaten bu kursta']); continue;
    } else if (u.courseId) {
      atlanan.push([ham, `başka bir kursa bağlı (${u.courseId}); önce oradan --geri-al`]); continue;
    }
    yapilacak.push({ ham, doc, u });
  }

  for (const y of yapilacak) {
    const onay = y.u.teacherApproved === true ? '' : '  [UYARI: öğretmen henüz onaylı değil]';
    console.log(`  ${opts.geriAl ? 'çıkarılacak' : 'bağlanacak'}: ${y.ham} (${y.doc.id}) ${y.u.name || ''}${onay}`);
  }
  if (atlanan.length) {
    console.log('\nAtlananlar:');
    atlanan.forEach(([ad, neden]) => console.log(`  ${ad}: ${neden}`));
  }

  // Geri almada kapanacak sınıflar ve genele taşınacak sorular — kuru çalışmada da gösterilsin.
  const kapanacak = [];
  if (opts.geriAl) {
    for (const y of yapilacak) {
      const snap = await db.collection('classes').where('ownerUid', '==', y.doc.id).get();
      snap.docs.filter((c) => c.get('archived') !== true).forEach((c) => kapanacak.push(c));
    }
    kapanacak.forEach((c) => console.log(`  kapanacak sınıf: ${c.id} "${c.get('name')}" (${c.get('studentCount') || 0} öğrenci)`));
  }

  if (!opts.yaz) {
    console.log(`\n${yapilacak.length} öğretmen değişecek. Yazmak için aynı komutu --yaz ile çalıştır.`);
    return;
  }

  if (!opts.geriAl && (!courseSnap.exists || opts.ad)) {
    await courseRef.set({
      name: opts.ad || courseSnap.get('name'),
      // Yalnızca listede ayırt etmek için; kurallar ve fonksiyonlar iki kurs türünü aynı görüyor.
      ...(opts.bagimsiz ? { independent: true } : {}),
      ...(courseSnap.exists ? {} : { createdAt: admin.firestore.FieldValue.serverTimestamp() }),
    }, { merge: true });
  }
  for (const y of yapilacak) {
    await y.doc.ref.update({
      courseId: opts.geriAl ? admin.firestore.FieldValue.delete() : opts.kurs,
    });
    console.log(`  yazıldı: ${y.ham}`);
  }
  const genel = classes.questionPoolFields({}, null);
  for (const c of kapanacak) {
    await c.ref.update({ archived: true });
    const bekleyen = await db.collection('questions').where('classId', '==', c.id).where('status', '==', 'pending').get();
    for (const q of bekleyen.docs) await q.ref.update(genel);
    console.log(`  kapandı: ${c.get('name')} (${bekleyen.size} bekleyen soru genele)`);
  }
  console.log('\nBitti. Öğretmen uygulamayı tamamen kapatıp açsın.');
}

main().catch((e) => fail(e.stack || e.message));

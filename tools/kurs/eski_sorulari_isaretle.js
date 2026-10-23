#!/usr/bin/env node
// Bu özellikten önce sorulmuş, hâlâ bekleyen soruları genel havuz olarak işaretler.
//
// NEDEN
//   Yeni öğretmen sürümü genel havuzu `pool == "general"` sorgusuyla dinliyor; `pool`
//   alanı olmayan eski soru bu sorguya hiç gelmez. Okuma ayrımı açılınca da kurallar bu
//   soruları genel saymıyor (firestore.rules, teacherReaches yorumu). Yani işaretlenmeyen
//   eski soru kimsenin havuzunda görünmeden 48 saat bekleyip iade edilir.
//
//   Yalnızca `pending` sorular: sahiplenilmiş sohbetler öğretmenine `claimedByTeacherUid`
//   ile zaten açık; kapanmışlar (resolved/expired) havuzda değil, "Tekrar sor" havuz
//   alanlarını yeniden yazıyor.
//
// NE ZAMAN
//   Yeni askTeacherQuestion deploy edildikten SONRA (öncesinde gelen sorular yine alansız
//   olurdu). Tekrar çalıştırmak zararsız: alanı olan soruya dokunmuyor.
//
// Kullanım (proje kökünden):
//   node tools/kurs/eski_sorulari_isaretle.js         → KURU ÇALIŞMA, sayıları gösterir
//   node tools/kurs/eski_sorulari_isaretle.js --yaz

const { fail, openFirestore, loadClasses } = require('./ortak');

async function main() {
  const args = process.argv.slice(2);
  const bilinmeyen = args.filter((a) => a !== '--yaz');
  if (bilinmeyen.length) fail(`Bilinmeyen seçenek: ${bilinmeyen.join(' ')}`);
  const yaz = args.includes('--yaz');

  const { db } = openFirestore();
  const genel = loadClasses().questionPoolFields({}, null);

  const snap = await db.collection('questions').where('status', '==', 'pending').get();
  const alansiz = snap.docs.filter((d) => d.get('pool') === undefined);
  console.log(`Bekleyen soru: ${snap.size}, pool alanı olmayan: ${alansiz.length}`);
  if (!yaz) {
    console.log('KURU ÇALIŞMA: hiçbir şey yazılmadı. Yazmak için --yaz.');
    return;
  }
  for (let i = 0; i < alansiz.length; i += 400) {
    const batch = db.batch();
    alansiz.slice(i, i + 400).forEach((d) => batch.update(d.ref, genel));
    await batch.commit();
  }
  console.log(`${alansiz.length} soru genel havuz olarak işaretlendi.`);
}

main().catch((e) => fail(e.stack || e.message));

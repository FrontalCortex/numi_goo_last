#!/usr/bin/env node
// Soru havuzu OKUMA ayrımının açma anahtarı: `system/questionPools.enforced`.
//
// Kapalıyken onaylı her öğretmen eskisi gibi her soruyu okuyabilir (sahiplenme ve mesaj
// yazma her durumda ayrıma tabi). Açılınca kurs öğretmeni yalnızca kendi sınıfının, genel
// öğretmen yalnızca genel havuzun sorularını okuyabilir.
//
// NE ZAMAN AÇILMALI
//   Bütün öğretmenler havuz filtresi olan sürüme (16+) geçtikten SONRA. Eski sürüm havuzu
//   filtresiz dinliyor; kurallar sorguyu süzmediği için o öğretmenlerin havuzu tamamen boş
//   kalır (hata da göstermez). Ayrıca tools/kurs/eski_sorulari_isaretle.js çalışmış olmalı.
//   Kurs öğretmeni yeni sürümle zaten yalnızca kendi sorularını görüyor; anahtar, değiştirilmiş
//   bir istemciye karşı kilit.
//
// Kullanım (proje kökünden):
//   node tools/kurs/havuz_ayrimi.js            → şu anki durum
//   node tools/kurs/havuz_ayrimi.js --ac
//   node tools/kurs/havuz_ayrimi.js --kapat

const { fail, openFirestore } = require('./ortak');

async function main() {
  const args = process.argv.slice(2);
  const ac = args.includes('--ac');
  const kapat = args.includes('--kapat');
  if ((ac && kapat) || args.some((a) => a !== '--ac' && a !== '--kapat')) fail('Yalnızca --ac ya da --kapat.');

  const { admin, db } = openFirestore();
  const ref = db.doc('system/questionPools');
  const once = (await ref.get()).get('enforced') === true;
  console.log(`Okuma ayrımı şu an: ${once ? 'AÇIK' : 'KAPALI'}`);
  if (!ac && !kapat) return;

  if (ac) {
    const alansiz = (await db.collection('questions').where('status', '==', 'pending').get())
      .docs.filter((d) => d.get('pool') === undefined).length;
    if (alansiz) fail(`${alansiz} bekleyen soruda pool alanı yok; önce eski_sorulari_isaretle.js --yaz`);
  }
  await ref.set({ enforced: ac, changedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
  console.log(`Okuma ayrımı: ${ac ? 'AÇIK' : 'KAPALI'}. Kurallar birkaç saniye içinde uygular.`);
}

main().catch((e) => fail(e.stack || e.message));

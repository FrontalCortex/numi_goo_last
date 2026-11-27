/**
 * DAVET RAPORU — K katsayısı ve huni, sunucudaki gerçek kayıtlardan (yalnızca OKUR).
 *
 * K (aylık) = o ay oluşan davet kaydı (referrals) / o ay aktif öğrenci
 * (docs/BUYUME_DONGUSU_ARASTIRMASI.md §4 (d)). GA4 tarayıcı ve Play kayıplarıyla eksik saydığı için
 * K buradan; GA4 yalnızca paylaşım hunisi (share_card_shown_weekly → share_gate_pass → share_done_*).
 *
 * "Aktif öğrenci": seri belgesindeki `lastSeenAt` o ay içinde (haftalık kaydın kullandığı tanım ve
 * indeks: collectionGroup('streak').lastSeenAt).
 *
 * ÇALIŞTIRMA (canlı veriyi okur; kullanıcı izniyle):
 *   cd functions && NODE_PATH=./node_modules GOOGLE_APPLICATION_CREDENTIALS=<hizmet hesabı.json> \
 *     node ../tools/referral-report.js [2026-11]
 *   Ay verilmezse içinde bulunulan ay (Türkiye saati). Çıktı yalnızca sayılar; kişisel veri yazmaz.
 */
const admin = require('firebase-admin');

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';
if (!admin.apps.length) admin.initializeApp({ projectId: PROJECT_ID });
const db = admin.firestore();
const TR = 3 * 3600 * 1000;

function monthRange(arg) {
  const now = new Date(Date.now() + TR);
  const [y, m] = (arg || `${now.getUTCFullYear()}-${now.getUTCMonth() + 1}`).split('-').map(Number);
  const start = Date.UTC(y, m - 1, 1) - TR;
  const end = Date.UTC(y, m, 1) - TR;
  return { label: `${y}-${String(m).padStart(2, '0')}`, start, end };
}

async function countActive(start, end) {
  const snap = await db.collectionGroup('streak')
    .where('lastSeenAt', '>=', admin.firestore.Timestamp.fromMillis(start))
    .where('lastSeenAt', '<', admin.firestore.Timestamp.fromMillis(end))
    .count().get();
  return snap.data().count;
}

async function main() {
  const { label, start, end } = monthRange(process.argv[2]);
  const refs = await db.collection('referrals')
    .where('createdAtMs', '>=', start)
    .where('createdAtMs', '<', end)
    .get();
  const bySource = {};
  const referrers = new Set();
  let rewarded = 0;
  for (const d of refs.docs) {
    const r = d.data();
    bySource[r.source || '?'] = (bySource[r.source || '?'] || 0) + 1;
    referrers.add(r.referrerUid);
    if (r.rewarded) rewarded++;
  }
  const codes = await db.collection('referralCodes').count().get();
  const active = await countActive(start, end);
  const k = active ? refs.size / active : 0;

  console.log(`Ay: ${label} (Türkiye saati)`);
  console.log(`Aktif öğrenci (lastSeenAt bu ay): ${active}`);
  console.log(`Davet kodu olan hesap (tüm zamanlar, en az bir kez paylaşmaya basmış): ${codes.data().count}`);
  console.log(`Davetle gelen yeni hesap: ${refs.size}  (kaynak: ${JSON.stringify(bySource)})`);
  console.log(`  ders 2'yi bitirip ödül alan: ${rewarded}`);
  console.log(`  farklı davet eden: ${referrers.size}`);
  console.log(`K (aylık) = ${refs.size} / ${active} = ${k.toFixed(3)}`);

  // Kötüye kullanım işareti (kullanıcı kararı 09.11.2026: önce izle, gerekirse sıkılaştır). Cihaz sınırı
  // aynı telefonu engelliyor; kalan yol çok sayıda farklı cihaz/emülatör. Bir kişiye son 7 günde
  // SUSPICIOUS_WEEKLY'den fazla davet gelmesi gerçek hayatta nadir (bir veli grubu bile birkaç kişi).
  const weekAgo = Date.now() - 7 * 86400000;
  const recent = await db.collection('referrals').where('createdAtMs', '>=', weekAgo).get();
  const perReferrer = {};
  for (const d of recent.docs) {
    const r = d.data();
    const p = (perReferrer[r.referrerUid] = perReferrer[r.referrerUid] || { n: 0, rewarded: 0, noDevice: 0 });
    p.n++;
    if (r.rewarded) p.rewarded++;
    if (!r.device) p.noDevice++;
  }
  const flagged = Object.entries(perReferrer).filter(([, p]) => p.n > SUSPICIOUS_WEEKLY);
  console.log(`\nSon 7 gün: ${recent.size} davet, ${Object.keys(perReferrer).length} davet eden.`);
  if (!flagged.length) {
    console.log(`Şüpheli yok (kimse 7 günde ${SUSPICIOUS_WEEKLY}'ten fazla davet almamış).`);
  } else {
    console.log(`⚠ 7 günde ${SUSPICIOUS_WEEKLY}'ten fazla davet: (uid · davet · ödüllü · cihaz izi olmayan)`);
    for (const [uid, p] of flagged.sort((a, b) => b[1].n - a[1].n)) {
      console.log(`  ${uid} · ${p.n} · ${p.rewarded} · ${p.noDevice}`);
    }
    console.log('  İncelemek için: referrals koleksiyonunda referrerUid == uid; aynı saatlerde art arda açılmış hesaplar,');
    console.log('  2. dersi dakikalar içinde bitiren hesaplar emülatör çiftliği işareti olabilir.');
  }
}

const SUSPICIOUS_WEEKLY = 5;

main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });

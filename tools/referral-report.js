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
}

main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });

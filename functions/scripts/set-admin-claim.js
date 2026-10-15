/**
 * Bir hesaba admin yetkisi verir ya da geri alır (Auth custom claim: `admin: true`).
 *
 * Admin yetkisi Firestore'daki bir alanla DEĞİL, ID token'daki custom claim ile veriliyor:
 * claim'i yalnızca Admin SDK yazabilir, istemci hiçbir yoldan değiştiremez; ayrıca Storage
 * kuralları da onu doğrudan okuyabiliyor (hata bildirimi ekran görüntüleri yalnız admin'e
 * açık). Şimdilik tek kullanımı hata bildirimlerini onaylamak/reddetmek (reviewBugReport).
 *
 *   cd functions
 *   $env:GOOGLE_APPLICATION_CREDENTIALS="...\serviceAccount.json"
 *   $env:FIREBASE_PROJECT_ID="numigo-new"
 *   node scripts/set-admin-claim.js <uid veya e-posta>
 *
 * Yetkiyi geri almak için:
 *   $env:REVOKE="1"; node scripts/set-admin-claim.js <uid veya e-posta>
 *
 * Mevcut claim'ler (ör. teacherApproved) korunur; yalnızca `admin` anahtarı değişir.
 *
 * NOT: Claim, o hesabın bir sonraki ID token yenilemesinde etkin olur. Uygulamada çıkış
 * yapıp yeniden girmek en kısa yol; aksi halde token en geç bir saat içinde yenilenir.
 */
const admin = require('firebase-admin');

const PROJECT_ID =
  process.env.FIREBASE_PROJECT_ID || process.env.GCLOUD_PROJECT || 'numigo-new';

if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const REVOKE = ['1', 'true', 'yes'].includes(String(process.env.REVOKE || '').toLowerCase());

async function main() {
  const target = (process.argv[2] || '').trim();
  if (!target) {
    console.error('Kullanım: node scripts/set-admin-claim.js <uid veya e-posta>');
    process.exit(1);
  }

  const user = target.includes('@')
    ? await admin.auth().getUserByEmail(target)
    : await admin.auth().getUser(target);

  const claims = Object.assign({}, user.customClaims || {});
  if (REVOKE) delete claims.admin;
  else claims.admin = true;

  await admin.auth().setCustomUserClaims(user.uid, claims);
  console.log(`Proje: ${PROJECT_ID}`);
  console.log(`${user.uid} (${user.email || 'e-postasız'}) → claim'ler:`, claims);
  console.log('Etkin olması için uygulamada çıkış yapıp yeniden gir.');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

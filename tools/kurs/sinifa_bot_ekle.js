#!/usr/bin/env node
// Bir sınıfa deneme için sahte öğrenci (bot) ekler ya da ekleneni siler.
//
// Botlar gerçek hesap DEĞİL: Auth kaydı ve users dokümanı yok (users yazımı mirrorPublicProfile ve
// syncTeacherClaim tetikleyicilerini çalıştırırdı). Yalnızca iki şey yazılıyor:
//   classes/{id}/members/seed_cls_…   → öğretmenin listesi ve Sorularım → Sınıfım
//   publicProfiles/seed_cls_…        → ad, kullanıcı adı, avatar (profil açılabilsin)
// ve sınıfın studentCount'u artıyor. Botlar soru soramaz, ödev yapamaz; ödev ayrıntısında
// "Başlamadı" görünürler ve "N öğrenciden …" sayısına girerler.
//
// Kullanım (proje kökünden):
//   node tools/kurs/sinifa_bot_ekle.js --kod 24MX5U --adet 12          → KURU ÇALIŞMA
//   node tools/kurs/sinifa_bot_ekle.js --kod 24MX5U --adet 12 --yaz
//   node tools/kurs/sinifa_bot_ekle.js --kod 24MX5U --geri-al --yaz    → bu sınıftaki botları siler

const fs = require('fs');
const path = require('path');
const { ROOT, fail, openFirestore } = require('./ortak');

const PREFIX = 'seed_cls_';

const GIRLS = ['Elif', 'Zeynep', 'Defne', 'Ecrin', 'Azra', 'Nehir', 'Asel', 'Eylül', 'Duru', 'Ela', 'Mira', 'Belinay'];
const BOYS = ['Yusuf', 'Ömer', 'Mustafa', 'Kerem', 'Alparslan', 'Göktuğ', 'Eymen', 'Aras', 'Miraç', 'Çınar', 'Emir', 'Atlas'];
const SURNAMES = ['Yılmaz', 'Kaya', 'Demir', 'Şahin', 'Çelik', 'Yıldız', 'Öztürk', 'Aydın', 'Arslan', 'Doğan', 'Kılıç', 'Koç'];

// ── Avatar (seed-lesson-leaderboard-realistic.js ile aynı; AvatarArt.kt → PERSONAS) ──
const PARTS = JSON.parse(fs.readFileSync(path.join(ROOT, 'app/src/main/assets/avatar/personas_parts.json'), 'utf8'));
const HAIR_GIRL = ['long', 'bobCut', 'curly', 'pigtails', 'curlyBun', 'bobBangs', 'straightBun', 'extraLong', 'bunUndercut', 'beanie'];
const HAIR_BOY = ['sideShave', 'shortCombover', 'curlyHighTop', 'curly', 'buzzcut', 'cap', 'fade', 'beanie', 'shortComboverChops', 'mohawk'];
const HAIR_COLORS = ['362c47', '6c4545', 'e15c66', 'e16381', 'f27d65', 'f29c65', 'dee1f5'];
const SKIN_COLORS = ['eeb4a4', 'e7a391', 'e5a07e', 'd78774', 'b16a5b', '92594b', '623d36'];
const CLOTHING_COLORS = ['456dff', '54d7c7', '7555ca', '6dbb58', 'e24553', 'f3b63a', 'f55d81'];
const BACKGROUNDS = ['b6e3f4', '65c9ff', 'c0aede', 'd1d4f9', 'ffd5dc', 'ffdfbf', 'a7ffc4', 'ffffb1', '262e33'];
const MOUTHS = Object.keys(PARTS.mouth).filter((m) => m !== 'pacifier');
const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];

function randomAvatar(girl) {
  const values = {
    hair: pick(girl ? HAIR_GIRL : HAIR_BOY),
    hairColor: pick(HAIR_COLORS),
    skinColor: pick(SKIN_COLORS),
    eyes: pick(Object.keys(PARTS.eyes)),
    nose: pick(Object.keys(PARTS.nose)),
    mouth: pick(MOUTHS),
    facialHair: '',
    body: pick(Object.keys(PARTS.body)),
    clothingColor: pick(CLOTHING_COLORS),
    backgroundColor: pick(BACKGROUNDS),
  };
  return 'personas|' + Object.keys(values).sort().map((k) => `${k}=${values[k]}`).join(';');
}

/** Gerçek kullanıcı adları gibi: 6 rakam + 2 büyük harf (generateUniqueUserId). */
function randomUserId() {
  const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  return `${Math.floor(100000 + Math.random() * 900000)}${pick([...letters])}${pick([...letters])}`;
}

function parseArgs(argv) {
  const out = { kod: '', adet: 12, yaz: false, geriAl: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--kod') out.kod = (argv[++i] || '').replace(/[\s-]/g, '').toUpperCase();
    else if (a === '--adet') out.adet = Number.parseInt(argv[++i], 10);
    else if (a === '--yaz') out.yaz = true;
    else if (a === '--geri-al') out.geriAl = true;
    else fail(`Bilinmeyen seçenek: ${a}`);
  }
  if (!/^[A-Z0-9]{6}$/.test(out.kod)) fail('--kod 6 karakterli sınıf kodu (ör. 24MX5U)');
  if (!out.geriAl && !(out.adet >= 1 && out.adet <= 24)) fail('--adet 1–24');
  return out;
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const { admin, db } = openFirestore();
  const codeSnap = await db.collection('classJoinCodes').doc(opts.kod).get();
  if (!codeSnap.exists) fail(`Bu kodla sınıf yok: ${opts.kod}`);
  const classId = codeSnap.get('classId');
  const classRef = db.collection('classes').doc(classId);
  const classSnap = await classRef.get();
  console.log(`Sınıf: ${classSnap.get('name')} (${classSnap.get('courseName')}) · şu an ${classSnap.get('studentCount')} öğrenci`);
  console.log(opts.yaz ? 'Kip: YAZ\n' : 'Kip: KURU ÇALIŞMA (yazmak için --yaz)\n');

  if (opts.geriAl) {
    const bots = (await classRef.collection('members').get()).docs.filter((d) => d.id.startsWith(PREFIX));
    bots.forEach((d) => console.log(`  silinecek: ${d.get('name')} (${d.id})`));
    if (!opts.yaz || !bots.length) return console.log(`\n${bots.length} bot.`);
    const batch = db.batch();
    bots.forEach((d) => {
      batch.delete(d.ref);
      batch.delete(db.collection('publicProfiles').doc(d.id));
    });
    batch.update(classRef, { studentCount: admin.firestore.FieldValue.increment(-bots.length) });
    await batch.commit();
    return console.log(`\n${bots.length} bot silindi.`);
  }

  // Ad + soyad: birbirinden farklı, yarısı kız yarısı erkek.
  const girls = [...GIRLS].sort(() => Math.random() - 0.5);
  const boys = [...BOYS].sort(() => Math.random() - 0.5);
  const surnames = [...SURNAMES].sort(() => Math.random() - 0.5);
  const now = Date.now();
  const bots = Array.from({ length: opts.adet }, (_, i) => {
    const girl = i % 2 === 0;
    const first = girl ? girls[i >> 1] : boys[i >> 1];
    const name = `${first} ${surnames[i % surnames.length]}`;
    return {
      id: `${PREFIX}${classId}_${String(i + 1).padStart(2, '0')}`,
      name,
      userId: randomUserId(),
      avatar: randomAvatar(girl),
      joinedAtMs: now - (opts.adet - i) * 60_000,
    };
  });
  bots.forEach((b) => console.log(`  ${b.name.padEnd(20)} @${b.userId}  ${b.id}`));
  if (!opts.yaz) return console.log(`\n${bots.length} bot eklenecek.`);

  const existing = await Promise.all(bots.map((b) => classRef.collection('members').doc(b.id).get()));
  if (existing.some((s) => s.exists)) fail('Bu sınıfta zaten bot var; önce --geri-al --yaz.');
  const batch = db.batch();
  for (const b of bots) {
    batch.set(classRef.collection('members').doc(b.id), {
      uid: b.id, userId: b.userId, name: b.name, joinedAtMs: b.joinedAtMs, via: 'seed',
    });
    batch.set(db.collection('publicProfiles').doc(b.id), {
      uid: b.id, name: b.name, userId: b.userId, avatarConfig: b.avatar,
      followersCount: 0, followingCount: 0,
      createdAt: admin.firestore.Timestamp.fromMillis(b.joinedAtMs),
    });
  }
  batch.update(classRef, { studentCount: admin.firestore.FieldValue.increment(bots.length) });
  await batch.commit();
  console.log(`\n${bots.length} bot eklendi. Geri almak: --kod ${opts.kod} --geri-al --yaz`);
}

main().catch((e) => fail(e.stack || e.message));

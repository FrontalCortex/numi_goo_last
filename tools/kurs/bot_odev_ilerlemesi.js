#!/usr/bin/env node
// Sınıftaki botlara (sinifa_bot_ekle.js) ödev ilerlemesi yazar ya da yazılanı siler. Tanıtım
// videosunda ödev listesi ve ayrıntısı boş görünmesin diye (docs/KURS_TANITIM_SENARYOSU.md).
//
// Sınıfın her ödevinde, hedefteki botların kabaca yarısı bitirmiş, dörtte biri yarıda, kalanı
// başlamamış oluyor. Yazılan: homeworks/{id}/studentProgress/{bot} (istemcinin yazdığıyla aynı
// alanlar, bkz. firestore.rules homeworkProgressOk).
//
// YAN ETKİ: biten her bot için onHomeworkProgressWritten tetikleyicisi ödevin doneUids'ine botu
// ekliyor ve sınıfın öğretmenine "Ödev tamamlandı" bildirimi gönderiyor. Bildirimler ödev başına
// tek kimlikle geldiği için tepside her ödevden yalnızca sonuncusu kalıyor.
//
// Kullanım (proje kökünden):
//   node tools/kurs/bot_odev_ilerlemesi.js --kod 24MX5U          → KURU ÇALIŞMA
//   node tools/kurs/bot_odev_ilerlemesi.js --kod 24MX5U --yaz
//   node tools/kurs/bot_odev_ilerlemesi.js --kod 24MX5U --geri-al --yaz

const { fail, openFirestore } = require('./ortak');

const PREFIX = 'seed_cls_';
const BLOCK_SIZE = 10; // functions/homework.js BLOCK_SIZE

function parseArgs(argv) {
  const out = { kod: '', yaz: false, geriAl: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--kod') out.kod = (argv[++i] || '').replace(/[\s-]/g, '').toUpperCase();
    else if (a === '--yaz') out.yaz = true;
    else if (a === '--geri-al') out.geriAl = true;
    else fail(`Bilinmeyen seçenek: ${a}`);
  }
  if (!/^[A-Z0-9]{6}$/.test(out.kod)) fail('--kod 6 karakterli sınıf kodu (ör. 24MX5U)');
  return out;
}

const randInt = (min, max) => min + Math.floor(Math.random() * (max - min + 1));
const shuffle = (arr) => [...arr].sort(() => Math.random() - 0.5);

/** [blocks] blok çözülmüş ilerleme; her blokta %65–100 doğru. */
function progressFor(uid, blocks, questionCount, updatedAtMs) {
  let answered = 0;
  let correct = 0;
  let lastBlockCorrect = 0;
  for (let b = 0; b < blocks; b++) {
    const size = Math.min(BLOCK_SIZE, questionCount - answered);
    lastBlockCorrect = Math.round(size * (0.65 + Math.random() * 0.35));
    answered += size;
    correct += lastBlockCorrect;
  }
  return { uid, completedBlocks: blocks, answered, correct, lastBlockCorrect, updatedAtMs };
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const { admin, db } = openFirestore();
  const codeSnap = await db.collection('classJoinCodes').doc(opts.kod).get();
  if (!codeSnap.exists) fail(`Bu kodla sınıf yok: ${opts.kod}`);
  const classId = codeSnap.get('classId');
  const classRef = db.collection('classes').doc(classId);
  const [classSnap, membersSnap, hwSnap] = await Promise.all([
    classRef.get(),
    classRef.collection('members').get(),
    db.collection('homeworks').where('classId', '==', classId).get(),
  ]);
  const bots = membersSnap.docs.filter((d) => d.id.startsWith(PREFIX));
  const botName = new Map(bots.map((d) => [d.id, d.get('name')]));
  console.log(`Sınıf: ${classSnap.get('name')} · ${bots.length} bot · ${hwSnap.size} ödev`);
  console.log(opts.yaz ? 'Kip: YAZ\n' : 'Kip: KURU ÇALIŞMA (yazmak için --yaz)\n');
  if (!bots.length) fail('Sınıfta bot yok; önce sinifa_bot_ekle.js.');
  if (!hwSnap.size) fail('Sınıfta ödev yok; önce uygulamadan ödev ver.');

  if (opts.geriAl) {
    let count = 0;
    for (const hw of hwSnap.docs) {
      const refs = bots.map((b) => hw.ref.collection('studentProgress').doc(b.id));
      const snaps = await Promise.all(refs.map((r) => r.get()));
      const existing = snaps.filter((s) => s.exists);
      count += existing.length;
      console.log(`  ${hw.id}: ${existing.length} bot ilerlemesi silinecek`);
      if (!opts.yaz) continue;
      const batch = db.batch();
      existing.forEach((s) => batch.delete(s.ref));
      batch.update(hw.ref, { doneUids: admin.firestore.FieldValue.arrayRemove(...bots.map((b) => b.id)) });
      await batch.commit();
    }
    return console.log(`\n${count} kayıt${opts.yaz ? ' silindi' : ''}.`);
  }

  const writes = [];
  const now = Date.now();
  for (const hw of hwSnap.docs) {
    const data = hw.data();
    const blockCount = Number(data.blockCount) || 0;
    const questionCount = Number(data.config && data.config.questionCount) || 0;
    const createdAtMs = Number(data.createdAtMs) || now;
    const targets = data.wholeClass === true
      ? bots.map((b) => b.id)
      : (data.assignedUids || []).filter((u) => botName.has(u));
    const shuffled = shuffle(targets);
    const doneCount = Math.round(shuffled.length * 0.5);
    const partialCount = blockCount > 1 ? Math.round(shuffled.length * 0.25) : 0;
    console.log(`  ödev ${hw.id} · ${blockCount} blok · ${questionCount} soru · hedefte ${targets.length} bot`);
    shuffled.forEach((uid, i) => {
      let blocks = 0;
      if (i < doneCount) blocks = blockCount;
      else if (i < doneCount + partialCount) blocks = randInt(1, blockCount - 1);
      if (!blocks) return console.log(`    ${botName.get(uid).padEnd(20)} başlamadı`);
      // Ödev verildikten sonra, şimdiden önce bir zaman.
      const updatedAtMs = createdAtMs + Math.floor(Math.random() * Math.max(1, now - createdAtMs));
      const p = progressFor(uid, blocks, questionCount, updatedAtMs);
      console.log(`    ${botName.get(uid).padEnd(20)} ${blocks}/${blockCount} blok · ${p.correct}/${p.answered} doğru`);
      writes.push({ ref: hw.ref.collection('studentProgress').doc(uid), p, done: blocks >= blockCount });
    });
  }
  if (!opts.yaz) return console.log(`\n${writes.length} ilerleme yazılacak (${writes.filter((w) => w.done).length} biten).`);

  const existing = await Promise.all(writes.map((w) => w.ref.get()));
  if (existing.some((s) => s.exists)) fail('Bazı botların ilerlemesi zaten var; önce --geri-al --yaz.');
  // Yarıda olanlar önce: bitenler tetikleyiciden bildirim çıkarıyor, sonda toplansınlar.
  writes.sort((a, b) => Number(a.done) - Number(b.done));
  const batch = db.batch();
  writes.forEach((w) => batch.set(w.ref, w.p));
  await batch.commit();
  console.log(`\n${writes.length} ilerleme yazıldı. Geri almak: --kod ${opts.kod} --geri-al --yaz`);
}

main().catch((e) => fail(e.stack || e.message));

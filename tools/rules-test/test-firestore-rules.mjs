/**
 * firestore.rules davranış testi — rozet sayaçları, kupa yolu ve seri.
 *
 * Kurallar tek bir dosyada yüzlerce satır ve birbirine VEYA'lı bağlı; elle okuyarak
 * "bu yazım geçer mi" sorusuna güvenle cevap verilemiyor. Emülatör gerçek cevabı veriyor.
 *
 * ÇALIŞTIRMA:
 *   cd tools/rules-test && npm install && npm test
 *
 * Bağımlılıkları BİLEREK functions/ dışında: orada olsalardı deploy sırasında Cloud Build'in
 * bağımlılık çözümüne girer ve derlemeyi kırarlardı (bir kez kırdılar).
 *
 * Not: emülatör reddetmeleri "evaluation error" diye raporluyor; bu bir kural hatası değil,
 * yalnızca reddin raporlanma biçimi. Doğrulama assertFails/assertSucceeds ile yapılıyor.
 */
import { initializeTestEnvironment, assertFails, assertSucceeds } from "@firebase/rules-unit-testing";
import { doc, setDoc, getDoc, updateDoc, deleteDoc } from "firebase/firestore";
import fs from "fs";
import path from "path";

const UID = "kid1";
let pass = 0, fail = 0;
async function check(name, p) {
  try { await p; console.log("  OK   " + name); pass++; }
  catch (e) { console.log("  HATA " + name + " -> " + (e.message||e).split("\n")[0]); fail++; }
}

const env = await initializeTestEnvironment({
  projectId: "rules-test",
  firestore: { rules: fs.readFileSync(path.join(import.meta.dirname, "../../firestore.rules"), "utf8"), host: "127.0.0.1", port: 8080 },
});

// Sunucu tarafi veriyi kurallari atlayarak yaz
await env.withSecurityRulesDisabled(async (ctx) => {
  const db = ctx.firestore();
  await setDoc(doc(db, `users/${UID}`), { uid: UID, role: "STUDENT" });
  await setDoc(doc(db, `users/${UID}/cupWayProgress/progress`), {
    addition_abacus_cup: 700, max_addition_abacus_cup: 900,
  });
  await setDoc(doc(db, `users/${UID}/badgeProgress/state`), {
    userDartProgress: 10, userDinoProgress: 800,
  });
  // Kupa yolu sandik defteri: sunucu yazar, istemci yalnizca okur.
  await setDoc(doc(db, `users/${UID}/cupPathRewards/progress`), {
    addition_abacus_cup: { lastClaimed: 500, claimed: [] },
  });
});

const db = env.authenticatedContext(UID).firestore();
const badge = doc(db, `users/${UID}/badgeProgress/state`);

console.log("\n=== ETKINLIK SAYACLARI (adim siniri 10) ===");
await check("+1 artis kabul edilir",        assertSucceeds(updateDoc(badge, { userDartProgress: 11 })));
await check("99999'a ziplama REDDEDILIR",   assertFails(updateDoc(badge, { userDartProgress: 99999 })));
await check("geri gitme REDDEDILIR",        assertFails(updateDoc(badge, { userDartProgress: 5 })));
await check("+10 (tam sinir) kabul edilir", assertSucceeds(updateDoc(badge, { userDartProgress: 21 })));
await check("+11 (sinir asimi) REDDEDILIR", assertFails(updateDoc(badge, { userDartProgress: 32 })));

console.log("\n=== KUPA ROZETLERI (zirve = 900) ===");
await check("zirvenin altina yazim kabul",  assertSucceeds(updateDoc(badge, { userDinoProgress: 850 })));
await check("zirveye esit kabul",           assertSucceeds(updateDoc(badge, { userDinoProgress: 900 })));
await check("zirvenin ustu REDDEDILIR",     assertFails(updateDoc(badge, { userDinoProgress: 901 })));
await check("2500'e ziplama REDDEDILIR",    assertFails(updateDoc(badge, { userDinoProgress: 2500 })));
await check("kupasiz rozet REDDEDILIR",     assertFails(updateDoc(badge, { userGoatProgress: 1500 })));

console.log("\n=== SEZON ODULLERI (eskiden beri korunuyor) ===");
await check("madalya yazimi REDDEDILIR",    assertFails(updateDoc(badge, { goldMedalPiece: [{ s: 1 }] })));

console.log("\n=== BASKASININ DOKUMANI ===");
const other = doc(env.authenticatedContext("kid2").firestore(), `users/${UID}/badgeProgress/state`);
await check("baskasinin rozeti REDDEDILIR", assertFails(updateDoc(other, { userDartProgress: 999 })));

console.log("\n=== KUPA YOLU (istemci yazamaz) ===");
await check("kupa puani yazimi REDDEDILIR", assertFails(setDoc(doc(db, `users/${UID}/cupWayProgress/progress`), { max_addition_abacus_cup: 9999 })));
// Sandik nadirligi ESIGE bagli oldugu icin puani sisirmek artik yalnizca "daha cok sandik"
// degil, "daha IYI sandik" demek: 1000'in katlari destansi. Kapinin kapali oldugu
// dogrulanmali.
await check("kupa puani guncellemesi REDDEDILIR", assertFails(updateDoc(doc(db, `users/${UID}/cupWayProgress/progress`), { addition_abacus_cup: 10000 })));

console.log("\n=== KUPA YOLU DEFTERI (silinemez de) ===");
const ledger = doc(db, `users/${UID}/cupPathRewards/progress`);
await check("defter okuma kabul edilir",    assertSucceeds(getDoc(ledger)));
await check("defter yazimi REDDEDILIR",     assertFails(setDoc(ledger, { addition_abacus_cup: { lastClaimed: 200, claimed: [] } })));
await check("defter guncellemesi REDDEDILIR", assertFails(updateDoc(ledger, { addition_abacus_cup: { lastClaimed: 200, claimed: [] } })));
// En kritigi: defteri SILEBILEN biri lastClaimed'i basa dondurup butun esikleri -- 1000'in
// katlarindaki destansi sandiklar dahil -- yeniden odetebilirdi.
await check("defter silme REDDEDILIR",      assertFails(deleteDoc(ledger)));

console.log("\n=== SERI (istemci yazamaz) ===");
await check("seri yazimi REDDEDILIR",       assertFails(setDoc(doc(db, `users/${UID}/streak/state`), { current: 30 })));
await check("seri okuma kabul edilir",      assertSucceeds(getDoc(doc(db, `users/${UID}/streak/state`))));

console.log("\n=== EN UZUN SERI (profilde gorunuyor, istemci yazamaz) ===");
const self = doc(db, `users/${UID}`);
await check("longestStreak yazimi REDDEDILIR", assertFails(updateDoc(self, { longestStreak: 999 })));
await check("zararsiz alan hala yazilabilir",  assertSucceeds(updateDoc(self, { selectedAvatar: 3 })));

await env.cleanup();
console.log(`\n${pass} gecti, ${fail} kaldi`);
process.exit(fail > 0 ? 1 : 0);

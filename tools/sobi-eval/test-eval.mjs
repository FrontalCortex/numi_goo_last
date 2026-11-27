/**
 * Değerlendirme betiğinin API'siz sınaması: sabit (sahte) cevapları programlı denetimlerden geçirir.
 * Ücretsiz; Sobi de hakem de çağrılmaz. Boş ve sabit cevap (null baseline) başarısız, iyi cevap
 * programlı denetimlerden geçmeli; kötü cevaplar doğru denetime takılmalı.
 *
 * Kullanım: node tools/sobi-eval/test-eval.mjs
 */
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

process.env.SOBI_EVAL_IMPORT = '1';
const ev = await import('./run-eval.mjs');

let fails = 0;
const expect = (name, cond) => { console.log(`${cond ? 'ok  ' : 'HATA'} ${name}`); if (!cond) fails++; };

const all = await ev.loadCases();
const byId = (re) => all.find((c) => re.test(c.id));
const anlatim = byId(/_neden_t8_14$/); // "10'luk kuralı uygulamak istesek 10 gelir ama kardeşi 4 gidemez."
const soru = byId(/_neden_t7_35$/); // 4 + 9, 4 → 14 (10 gelir)
const kisisel = all.find((c) => c.tags[0] === 'kisisel' && (c.gizli || []).length);

const SAHTE = {
  [anlatim.id]: "6'yı eklemek, 10 ekleyip 4 çıkarmakla aynı. Ama 4 doğrudan gidemiyor, çünkü yalnızca 1 birlik boncuk gelmiş.",
  [soru.id]: "Birler sütununda 4 var, 9 eklemeye yer yok. Bu yüzden 10 gelir, büyük kardeşi 1 gider.",
  [kisisel.id]: `Merhaba ${kisisel.gizli[0]}! Hadi abaküsümüze dönelim.`,
};
const dir = mkdtempSync(join(tmpdir(), 'sobi-eval-'));
const write = (obj) => { const p = join(dir, `${Math.random()}.json`); writeFileSync(p, JSON.stringify(obj)); return p; };
ev.argExtra.hakemsiz = true;
const ctx = { model: 'claude-haiku-5-5' };

async function grade(c, replies) {
  ev.argExtra.sahte = write({ [c.id]: replies });
  const run = await ev.runCase(c, ctx);
  return { run, g: (await ev.gradeCase(c, run, null, ctx)).grade };
}

// Seçim
const pilot = (ev.argExtra.sec = 'ornek:20', await ev.loadCases());
ev.argExtra.sec = null;
const cats = new Set(pilot.map((c) => c.tags[0]));
expect(`pilot 20 örnek, ${cats.size} kategori (${[...cats].join(', ')})`, pilot.length === 20 && cats.size >= 7);

// Null baseline: boş ve sabit cevap başarısız olmalı.
{
  const { g } = await grade(anlatim, ['']);
  expect('boş cevap: cevap_var=0, basari=0', g.cevap_var === 0 && g.basari === 0);
}
{
  const { g } = await grade(soru, ['Bilmiyorum.']);
  expect('sabit "Bilmiyorum" programlıdan geçebilir (hakem eler): basari hakemsiz', g.cevap_var === 1);
}
// İyi cevaplar programlı denetimlerden geçmeli (hamle anlatan iyi cevapta araç yok → arac_once=0 olmalı:
// sahte modda araç çağrılmadı).
{
  const { g } = await grade(anlatim, [SAHTE[anlatim.id]]);
  expect('anlatım iyi cevap: kisa, yon_yok, bicim, yedek_yok', g.kisa && g.yon_yok && g.bicim && g.yedek_yok);
}
{
  const { g } = await grade(soru, [SAHTE[soru.id]]);
  expect('soru: "büyük kardeşi 1 gider" araçsız → arac_once=0', g.arac_once === 0);
  expect('soru: hatayi_bul_ilk=0 (çağrılmadı)', g.hatayi_bul_ilk === 0);
}
// Kötü cevaplar
{
  const { g } = await grade(anlatim, ['Boncuğu aşağı indir, sonra yukarı kaydır.']);
  expect('yön tarifi yakalanır', g.yon_yok === 0);
}
{
  const { g } = await grade(anlatim, ['Bak:\n- 1 gelir\n- 5 gider\n- **10 gelir**']);
  expect('liste/kalın yakalanır', g.bicim === 0);
}
{
  const { g } = await grade(anlatim, ['Bir. İki. Üç. Dört.']);
  expect('4 cümle → kisa=0', g.kisa === 0);
}
{
  const { g } = await grade(anlatim, ['Sağdan 3. sütuna bak. Orada 2 boncuk var.']);
  expect('"3. sütun" tek cümle sayılır → kisa=1', g.kisa === 1);
}
{
  const { g } = await grade(kisisel, [SAHTE[kisisel.id]]);
  expect(`kişisel bilgi tekrarı yakalanır (${kisisel.gizli[0]})`, g.kisisel_tekrar_yok === 0);
}
{
  const { g } = await grade(soru, ['Abaküsünde 4 var, 37 eklemeliyiz.']);
  expect('soru adımında kaynaksız sayı → yedeğe düşer', g.yedek_yok === 0 && g.kaynaksiz_siki_yok === 0);
}
{
  const { g } = await grade(anlatim, ['Bu adımda 7 boncuk düşün.']);
  expect('anlatımda 10\'a kadar serbest ama sıkı denetim yakalar', g.yedek_yok === 1 && g.kaynaksiz_siki_yok === 0);
}
// Çok turlu sohbet: tur sayısı kadar cevap
{
  const c = all.find((x) => x.tags[0] === 'sohbet' && x.contexts.length > 1);
  const { run } = await grade(c, ['a.', 'b.', 'c.']);
  expect(`adım değişen sohbet: ${run.turns.length} tur, 2. turun bağlamı farklı`, run.turns.length === c.turns.length && run.turns[1].context.stepId !== run.turns[0].context.stepId);
  console.log('\n--- hakem girdisi örneği ---\n' + ev.judgeInput(c, run).slice(0, 2500));
}

console.log(fails ? `\n${fails} HATA` : '\nhepsi geçti');
process.exit(fails ? 1 : 0);

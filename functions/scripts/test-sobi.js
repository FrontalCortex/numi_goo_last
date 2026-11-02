/**
 * "Sobi'ye sor" sunucu tarafı testi (bkz. sobi/chat.js, tools.js, guard.js, callable.js).
 *
 * NEDEN SAHTE MODEL
 *   Gerçek API her çağrıda para harcıyor ve aynı soruya her seferinde başka cevap veriyor.
 *   Buradaki testler modelin ne söyleyeceğini değil, bizim kodun her durumda doğru davrandığını
 *   sınıyor: araç döngüsü, ret, sınır, denetime takılan cevap, geçmişin değiştirilmeden büyümesi,
 *   günlük hak ve oturum. Sahte istemci önceden yazılmış cevapları sırayla döndürüyor.
 *   Modelin gerçek cevaplarının kalitesi Faz 3'teki test setiyle ölçülecek (scripts/sobi-dene.js).
 *
 * ÇALIŞTIRMA: cd functions && npm run test:sobi
 */
const assert = require('assert');
const tools = require('../sobi/tools');
const explainer = require('../sobi/stepExplainer');
const guard = require('../sobi/guard');
const chat = require('../sobi/chat');
const createSobiChat = require('../sobi/callable');
const wallet = require('../sobi/wallet');

let passed = 0;
async function test(name, fn) {
  await fn();
  passed++;
  console.log(`  ✓ ${name}`);
}

// ── Sahte model ─────────────────────────────────────────────────────────

/**
 * Cevap listesini sırayla döndüren istemci; gönderilen istekleri [calls]'a yazar. Liste
 * kopyalanmıyor: callable testi istemciyi bir kez kurup cevapları sonradan ekliyor.
 */
function fakeClient(responses) {
  const calls = [];
  const queue = responses;
  const create = (beta) => async (params) => {
    calls.push({ ...JSON.parse(JSON.stringify(params)), _beta: beta });
    const next = queue.shift();
    if (!next) throw new Error('sahte model: cevap kalmadı');
    return typeof next === 'function' ? next(params) : next;
  };
  return { calls, messages: { create: create(false) }, beta: { messages: { create: create(true) } } };
}
const usage = { input_tokens: 100, output_tokens: 20, cache_read_input_tokens: 0, cache_creation_input_tokens: 0 };
const say = (text, extra = []) => ({ stop_reason: 'end_turn', content: [...extra, { type: 'text', text }], usage });
const callTool = (name, input, id = `tu_${name}`) => ({
  stop_reason: 'tool_use',
  content: [{ type: 'tool_use', id, name, input }],
  usage,
});
const CTX = chat.sanitizeContext({
  stepId: 't8_8_arti_6', stepText: '8\'e 6\'yı boncuk kuralıyla ekle.', questionText: '8 + 6',
  base: 8, target: 14, current: 9, attempts: 2, valueLog: [8, 9],
});

async function main() {
  console.log('anlatım (derslerdeki sözlerle)');
  const steps = (b, op, n) => tools.describe(require('../sobi/stepExplainer').explain(b, op, n)).hamleler.map((h) => h.adimlar);
  await test('8 + 6: 1 boncuk gelir, 5 gider, 10 gelir', () => assert.deepStrictEqual(
    steps(8, 'add', 6), [['1 boncuk gelir', '5 gider', '10 gelir (onlar basamağına 1 eklenir: kuralsız)']],
  ));
  await test('3 + 4: 5 gelir, kardeşi 1 gider', () => assert.deepStrictEqual(steps(3, 'add', 4), [['5 gelir', 'kardeşi 1 gider']]));
  await test('18 + 4: 10 gelir, büyük kardeşi 6 gider', () => assert.deepStrictEqual(
    steps(18, 'add', 4), [['10 gelir (onlar basamağına 1 eklenir: kuralsız)', 'büyük kardeşi 6 gider']],
  ));
  await test('49 + 1: elde 5\'lik kuralla anlatılıyor', () => assert.deepStrictEqual(
    steps(49, 'add', 1), [["10 gelir (onlar basamağına 1 eklenir: 5'lik kural, 5 gelir, kardeşi 4 gider)", 'büyük kardeşi 9 gider']],
  ));
  await test('10 − 1: 10 gider, kardeşi 9 gelir', () => assert.deepStrictEqual(
    steps(10, 'sub', 1), [['10 gider (onlar basamağına 1 çıkarılır: kuralsız)', 'kardeşi 9 gelir']],
  ));
  await test('23 − 6: 10 gider, 5 gelir, kardeşi 1 gider', () => assert.deepStrictEqual(
    steps(23, 'sub', 6), [['10 gider (onlar basamağına 1 çıkarılır: kuralsız)', '5 gelir', 'kardeşi 1 gider']],
  ));
  await test('5 − 1: 5 gider, kardeşi 4 gelir', () => assert.deepStrictEqual(steps(5, 'sub', 1), [['5 gider', 'kardeşi 4 gelir']]));
  await test('kuralsız: 0 + 68', () => assert.deepStrictEqual(steps(0, 'add', 68), [['6 gelir'], ['8 gelir']]));

  console.log('sütun dökümü');
  await test('8: beşlik gelmiş, 3 birlik gelmiş, 1 birlik boşta', () => {
    assert.strictEqual(tools.columnsText(8), 'birler 8: beşlik gelmiş, 3 birlik gelmiş, 1 birlik boşta');
    assert.strictEqual(tools.columnsText(23), 'onlar 2: beşlik boşta, 2 birlik gelmiş, 2 birlik boşta; birler 3: beşlik boşta, 3 birlik gelmiş, 1 birlik boşta');
    assert.strictEqual(tools.columnsText(0), 'birler 0: beşlik boşta, 0 birlik gelmiş, 4 birlik boşta');
    assert.deepStrictEqual(tools.columnsOf(105).map((c) => c.deger), [1, 0, 5]);
  });
  await test('bağlamda sütunlar var, "birler sütununda 3" denetimden geçiyor', async () => {
    const ctx = chat.sanitizeContext({ questionText: '23 - 6', base: 23, target: 17, current: 23 });
    assert.ok(chat.contextBlock(ctx).includes('birler 3: beşlik boşta, 3 birlik gelmiş'));
    const client = fakeClient([say('Birler sütununda 3 birlik boncuk var, onlarda 2. Sence 6 çıkar mı?')]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: ctx, childText: 'anlamadım' });
    assert.strictEqual(r.fellBack, false, r.reasons.join());
  });

  console.log('gerekçe');
  const reason = (b, op, n) => tools.reasonOf(require('../sobi/stepExplainer').explain(b, op, n).moves[0]);
  await test('8 + 6 boncuk kuralı: neden doğrudan ve 10\'luk olmuyor, neden doğru', () => assert.deepStrictEqual(reason(8, 'add', 6), {
    neden_dogrudan_olmuyor: "8 ile 6 toplanınca 14 oluyor; bir sütun en fazla 9 tutar. 10'luk kural da olmuyor: büyük kardeş 4 gitmeli ama sütunda yalnızca 3 birlik boncuk gelmiş.",
    neden: '6 eklemek, 10 ekleyip 4 çıkarmakla aynı. 4 çıkarmak da 5 çıkarıp 1 eklemekle aynı. Bu yüzden 1 boncuk gelir, 5 gider, 10 gelir.',
  }));
  await test('23 − 6 boncuk çıkarma gerekçesi', () => assert.deepStrictEqual(reason(23, 'sub', 6), {
    neden_dogrudan_olmuyor: "Sütunda 3 var; 6 çıkarmaya yetmiyor. 10'luk kural da olmuyor: kardeş 4 gelmeli ama yalnızca 1 birlik boncuk boşta.",
    neden: '6 çıkarmak, 10 çıkarıp 4 eklemekle aynı. 4 eklemek de 5 ekleyip 1 çıkarmakla aynı. Bu yüzden 10 gider, 5 gelir, 1 boncuk gider.',
  }));
  await test('5\'lik ve 10\'luk gerekçeler', () => {
    assert.strictEqual(reason(3, 'add', 4).neden, '4 eklemek, 5 ekleyip 1 çıkarmakla aynı. Bu yüzden 5 gelir, kardeşi 1 gider.');
    assert.strictEqual(reason(3, 'add', 4).neden_dogrudan_olmuyor, 'Birlik boncuklardan 1 tanesi boşta ama 4 tane gerekiyor.');
    assert.strictEqual(reason(18, 'add', 4).neden, '4 eklemek, 10 ekleyip 6 çıkarmakla aynı. Bu yüzden 10 gelir, büyük kardeşi 6 gider.');
    assert.strictEqual(reason(5, 'sub', 1).neden, '1 çıkarmak, 5 çıkarıp 4 eklemekle aynı. Bu yüzden 5 gider, kardeşi 4 gelir.');
    assert.strictEqual(reason(10, 'sub', 1).neden, '1 çıkarmak, 10 çıkarıp 9 eklemekle aynı. Bu yüzden 10 gider, kardeşi 9 gelir.');
  });
  await test('gerekçeler kendi içinde tutarlı: her (rakam, kural) için hesap doğru', () => {
    const ex = require('../sobi/stepExplainer');
    for (const op of ['add', 'sub']) for (let d = 0; d <= 9; d++) for (let x = 1; x <= 9; x++) {
      const e = ex.explain(10 + d, op, x);
      const r = tools.reasonOf(e.moves[0]);
      assert.ok(typeof r.neden === 'string' && r.neden.length > 10, `${d} ${op} ${x}`);
      // Gerekçedeki "a eklemek/çıkarmak, 5|10 ekleyip/çıkarıp b ..." hesabı tutmalı
      const m = r.neden.match(/^(\d+) (?:eklemek|çıkarmak), (5|10) (?:ekleyip|çıkarıp) (\d+) /);
      if (m) assert.strictEqual(Number(m[2]) - Number(m[3]), Number(m[1]), `${d} ${op} ${x}: ${r.neden}`);
    }
  });

  console.log('araçlar');
  await test('hamleleri_hesapla özet ve kural adı', () => {
    const r = tools.runTool('hamleleri_hesapla', { baslangic: 68, islem: 'add', sayi: 76 }, CTX, {});
    const o = JSON.parse(r.content);
    assert.strictEqual(r.isError, false);
    assert.strictEqual(o.sonuc, 144);
    assert.deepStrictEqual(o.hamleler.map((h) => [h.basamak, h.kural, h.sonra]), [['onlar', 'boncuk kuralı', 138], ['birler', 'boncuk kuralı', 144]]);
  });
  await test('sığmayan ve aralık dışı işlem hata', () => {
    assert.strictEqual(tools.runTool('hamleleri_hesapla', { baslangic: 5, islem: 'sub', sayi: 6 }, CTX, {}).isError, true);
    assert.strictEqual(tools.runTool('hamleleri_hesapla', { baslangic: 100000, islem: 'add', sayi: 1 }, CTX, {}).isError, true);
    assert.strictEqual(tools.runTool('hamleleri_hesapla', { baslangic: 1.5, islem: 'add', sayi: 1 }, CTX, {}).isError, true);
  });
  await test('kurali_acikla onaylı metin', () => {
    const o = JSON.parse(tools.runTool('kurali_acikla', { kural: 'five', islem: 'add' }, CTX, {}).content);
    assert.strictEqual(o.kural, "5'lik kural");
    assert.ok(o.aciklama.includes('5 gelir'));
  });
  await test('abakuste_goster ekran işi döndürüyor, ikinci kez reddediliyor', () => {
    const state = {};
    const r = tools.runTool('abakuste_goster', { baslangic: 8, islem: 'add', sayi: 6 }, CTX, state);
    assert.deepStrictEqual(r.action, {
      tur: 'goster', baslangic: 8, islem: 'add', sayi: 6, sonuc: 14,
      plan: 'c0+6=bead[beads@0l+1 five@0u-1 ten{c1+1=direct[direct@1l+1]>14}]>14',
    });
    assert.strictEqual(tools.runTool('abakuste_goster', { baslangic: 8, islem: 'add', sayi: 6 }, CTX, state).isError, true);
    // Alıştırma yalnızca uygulama destekliyorsa (öğretici desteklemiyor)
    assert.strictEqual(tools.runTool('alistirma_ver', { baslangic: 3, islem: 'add', sayi: 4 }, CTX, state).isError, true);
    assert.strictEqual(tools.runTool('alistirma_ver', { baslangic: 3, islem: 'add', sayi: 4 }, { ...CTX, practice: true }, state).action.sonuc, 7);
  });
  await test('hatayi_bul: ilk adımı yapıp 9\'a gelen çocuk doğru yolda', () => {
    const o = JSON.parse(tools.runTool('hatayi_bul', {}, CTX, {}).content);
    assert.ok(o.durum.includes('doğru yolda'));
    assert.deepStrictEqual(o.yapilan_adimlar, ['birler: 1 boncuk gelir']);
    assert.strictEqual(o.siradaki_adim, 'birler: 5 gider');
  });
  await test('hatayi_bul: doğru yoldan sapmış', () => {
    const o = tools.findMistake(chat.sanitizeContext({ base: 8, target: 14, current: 11, valueLog: [8, 9, 10, 11] }));
    assert.strictEqual(o.dogru_yapilan_hamle_sayisi, 0);
    assert.strictEqual(o.ilk_hata.kural, 'boncuk kuralı');
  });
  await test('adım izi: her adımdan sonraki değer', () => assert.deepStrictEqual(
    tools.stepTrail(require('../sobi/stepExplainer').explain(68, 'add', 76)).map((t) => t.sonra),
    [88, 38, 138, 139, 134, 144],
  ));
  await test('hatayi_bul: onlar doğru, birlerde şaşmış', () => {
    const ctx = chat.sanitizeContext({ base: 68, target: 144, current: 140, valueLog: [68, 88, 38, 138, 139, 140] });
    const o = tools.findMistake(ctx);
    assert.strictEqual(o.dogru_yapilan_hamle_sayisi, 1);
    assert.strictEqual(o.ilk_hata.basamak, 'birler');
  });
  await test('hatayi_bul: doğru cevap ve boş kayıt', () => {
    assert.ok(tools.findMistake(chat.sanitizeContext({ base: 8, target: 14, current: 14 })).durum.includes('doğru'));
    assert.ok(tools.findMistake(chat.sanitizeContext({ base: 8, target: 14, current: 8 })).durum.includes('henüz'));
    // Kayıtta yalnızca başlangıç değeri: dokunmamış, "şaştın" denmiyor
    const o = tools.findMistake(chat.sanitizeContext({ base: 23, target: 17, current: 23, valueLog: [23] }));
    assert.ok(o.durum.includes('henüz'));
    assert.strictEqual(o.siradaki_adim, 'birler: 10 gider (onlar basamağına 1 çıkarılır: kuralsız)');
    assert.ok(tools.findMistake(chat.sanitizeContext({})).durum.includes('yok'));
  });
  await test('beklenen cevabın sütunları bağlamda: "yüzler sütununa 6" geçiyor', () => {
    const ctx = chat.sanitizeContext({ base: 0, target: 600, current: 0 });
    const src = guard.sourceNumbers([{ role: 'user', content: [{ type: 'text', text: chat.contextBlock(ctx) }] }]);
    assert.strictEqual(guard.checkReply('Yüzler sütununa 6 yazmalısın.', src).ok, true);
  });
  await test('anlatım adımı bağlamı: ders, son adımlar, öğretilen kurallar', () => {
    const c = chat.sanitizeContext({
      lessonTitle: "10'luk Toplama", taughtRules: ['add_five', 'add_five', 'uydurma', 'add_ten'],
      recentSteps: ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 42], stepText: '10 gelir, büyük kardeş gider.',
    });
    assert.deepStrictEqual(c.taughtRules, ['add_five', 'add_ten']);
    // Bütün önceki adımlar kalıyor (eskiden son 6); son 6'sı "az önce" diye ayrı.
    assert.deepStrictEqual(c.recentSteps, ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']);
    const b = chat.contextBlock(c);
    assert.ok(b.includes("Ders: 10'luk Toplama"));
    assert.ok(b.includes("Çocuğun şimdiye kadar öğrendiği kurallar: 5'lik toplama, 10'luk toplama"));
    assert.ok(b.includes('- h'));
    assert.ok(b.indexOf('Dersin önceki kısmı') < b.indexOf('- a') && b.indexOf('- b') < b.indexOf('az önce anlatılanlar'));
    assert.ok(b.indexOf('az önce anlatılanlar') < b.indexOf('- c'));
    assert.ok(b.includes('Bu bir anlatım adımı'));
    assert.ok(!chat.contextBlock(CTX).includes('anlatım adımı'));
  });
  await test('soru adımı bağlamı: adımın işi, abaküs durumu, ipucu ve çarpımlar', () => {
    // 4 + 9'un "10 gelir" adımı: 4 → 14; ekrandaki işlem bütün işlem.
    const c = chat.sanitizeContext({ questionText: '4 + 9', stepText: '10 gelir.', base: 4, target: 14, current: 4, valueLog: [4] });
    const b = chat.contextBlock(c, { messageInStep: 2 });
    assert.ok(b.includes('Bu adımın işi: 4 → 14'));
    assert.ok(b.includes('Ekrandaki "4 + 9" bütün işlem'));
    assert.ok(b.includes('İpucu (ilk yardımda bunu kullan): Onlar basamağına bak'));
    assert.ok(b.includes('Sıradaki adım (çocuk cevabı isterse'));
    assert.ok(b.includes("Sobi'nin bu adımdaki mesajı: 2."));
    // 3 + 2'nin "5 gelir" adımı (3 → 8) tek başına kuralsız görünür; ders 5'lik kuralı anlatıyor.
    const part = chat.contextBlock(chat.sanitizeContext({ questionText: '3 + 2', stepText: '5 gelir.', base: 3, target: 8, current: 3, valueLog: [3] }));
    assert.ok(part.includes("birler basamağında 5'lik kural, \"5 gelir\" adımı"));
    assert.ok(part.includes("bu adımda 5'lik kural kullanılıyor"));
    assert.ok(!part.includes('kuralsız'));
    const m = chat.contextBlock(chat.sanitizeContext({ questionText: '46 x 3', stepText: 'Çarp.', base: 18, target: 138, current: 18 }));
    assert.ok(m.includes('6 x 3 = 18') && m.includes('4 x 3 = 12') && m.includes('(120)'));
    // Çarpım sayıları artık kaynakta: "4 ile 3'ün çarpımı 12" guard'dan geçiyor.
    const src = guard.sourceNumbers([{ role: 'user', content: [{ type: 'text', text: m }] }]);
    assert.strictEqual(guard.checkReply("4 ile 3'ün çarpımı 12, onlar basamağına 120 eklenir.", src).ok, true);
    // Çarpmada yedek metin adımın kendi sözü, toplama kuralı değil.
    const fb = guard.fallbackReply(chat.sanitizeContext({ questionText: '46 x 3', stepText: 'Çarp.', base: 18, target: 138 }), tools);
    assert.ok(fb.includes('"Çarp."') && !fb.includes('kural'));
  });
  await test('kardeş alanı: kimin kardeşi, çıkarmada "kardeş"', () => {
    const add = tools.describe(explainer.explain(17, 'add', 3)).hamleler[0].kardes;
    assert.deepStrictEqual(add, { sayi: 3, kardesi: 7, ad: 'büyük kardeş' });
    const sub = tools.describe(explainer.explain(23, 'sub', 6)).hamleler[0].kardes;
    assert.deepStrictEqual(sub, { sayi: 6, kardesi: 4, ad: 'kardeş' });
    assert.strictEqual(tools.describe(explainer.explain(1, 'add', 2)).hamleler[0].kardes, undefined);
  });
  await test('araç listesi bağlamdan bağımsız (düşünme blokları değişmeyen liste ister)', () => {
    const exp = chat.sanitizeContext({ stepText: 'x', current: 9 });
    assert.deepStrictEqual(chat.toolsFor(exp), chat.toolsFor(CTX));
    assert.deepStrictEqual(chat.toolsFor({ ...CTX, practice: true }), chat.toolsFor(CTX));
    assert.deepStrictEqual(chat.suggestionsFor(exp, []), ['neden_boyle', 'ornek_ver', 'anladim']);
  });
  await test('anlatım adımında gösterim araç düzeyinde reddediliyor', () => {
    const exp = chat.sanitizeContext({ stepText: 'x' });
    const r = tools.runTool('abakuste_goster', { baslangic: 8, islem: 'add', sayi: 6 }, exp, {});
    assert.strictEqual(r.isError, true);
    assert.strictEqual(r.action, undefined);
  });
  await test('araç tanımları strict ve şemaları kapalı', () => {
    for (const t of tools.TOOL_DEFS) {
      assert.strictEqual(t.strict, true, t.name);
      assert.strictEqual(t.input_schema.additionalProperties, false, t.name);
    }
  });

  console.log('güvenlik');
  await test('e-posta, telefon, bağlantı maskeleniyor; işlem kalıyor', () => {
    const r = guard.cleanChildText('annem ayse@ornek.com numarası 0532 123 45 67 www.site.com 68 + 76 nasıl');
    assert.strictEqual(r.masked, true);
    assert.ok(!r.text.includes('@') && !r.text.includes('532') && !r.text.includes('www'));
    assert.ok(r.text.includes('68 + 76'));
    assert.strictEqual(guard.cleanChildText('8 + 6 neden 14').masked, false);
    assert.strictEqual(guard.cleanChildText('x'.repeat(1000)).text.length, guard.MAX_CHILD_TEXT);
  });
  await test('ad maskeleniyor, "bu adım" kalıyor', () => {
    assert.strictEqual(guard.cleanChildText('benim adım Ali ve 8 + 6 zor').text, 'benim adım [ad] ve 8 + 6 zor');
    assert.strictEqual(guard.cleanChildText('İsmim Ayşe').text, 'İsmim [ad]');
    assert.strictEqual(guard.cleanChildText('bu adım çok zor').masked, false);
  });
  await test('binlik ayırıcı birleşiyor', () => assert.deepStrictEqual([...guard.numbersIn('99.999 ve 1.000, 3.5')].sort((a, b) => a - b), [3, 5, 1000, 99999]));
  await test('kaynaksız sayı denetime takılıyor, 5 ve 10 serbest', () => {
    const src = new Set([8, 14, 1]);
    assert.strictEqual(guard.checkReply("5'lik değil 10'luk kural: 1 boncuk gelir, cevap 14.", src).ok, true);
    assert.deepStrictEqual(guard.checkReply('Kardeşi 3 gider.', src).reasons, ['kaynaksiz_sayi:3']);
    assert.strictEqual(guard.checkReply('', src).ok, false);
    assert.strictEqual(guard.checkReply('a'.repeat(guard.MAX_REPLY + 1), src).ok, false);
  });
  await test('yedek metin kural motorundan', () => assert.strictEqual(
    guard.fallbackReply(CTX, tools),
    'Birlikte bakalım! Birler basamağında boncuk kuralı: 1 boncuk gelir, 5 gider, 10 gelir (onlar basamağına 1 eklenir: kuralsız). İstersen "Bana göster"e dokun.',
  ));
  await test('anlatım adımında küçük sayılar serbest, soru adımında değil', () => {
    const src = new Set([600]);
    assert.strictEqual(guard.checkReply('Yüzler basamağı sağdan 3. sütun, önce 1 boncuk.', src, { explanation: true }).ok, true);
    assert.strictEqual(guard.checkReply('Yüzler basamağı sağdan 3. sütun.', src).ok, false);
    assert.strictEqual(guard.checkReply('Sonuç 47 olur.', src, { explanation: true }).ok, false);
  });
  await test('anlatım adımında yedek metin dersin kendi sözü', () => {
    const r = guard.fallbackReply(chat.sanitizeContext({ stepText: 'İlk olarak yüzler basamağını yazıyorum.' }), tools);
    assert.ok(r.includes('"İlk olarak yüzler basamağını yazıyorum."'));
  });
  await test('anlatım adımında yedek metin "Bana göster" demiyor', () => {
    assert.ok(!guard.fallbackReply(chat.sanitizeContext({ stepText: 'x' }), tools).includes('Bana göster'));
  });
  await test('bağlam temizleniyor', () => {
    const c = chat.sanitizeContext({ stepId: 'T8; drop', base: -1, target: 100000, current: 3.2, attempts: 5, valueLog: [1, -2, 'x', 3], extra: 'gizli' });
    assert.deepStrictEqual(c, { stepId: null, stepText: '', questionText: '', base: null, target: null, current: null, attempts: 5, valueLog: [1, 3], practice: false, lessonTitle: '', recentSteps: [], taughtRules: [], tutorialNumber: null, stepIndex: null, practiceResults: [], ruleStats: [] });
    // Örnek sonuçları ve kural geçmişi: biçimi bozuk olanlar atılır.
    const p = chat.sanitizeContext({
      practiceResults: [{ question: '146 + 9', wrong: 1, done: true }, { question: '<script>', wrong: 0, done: true }, { question: '12 + 3', wrong: -1 }],
      ruleStats: [{ key: 'add_ten', correct: 12, wrong: 5 }, { key: 'uydurma', correct: 1, wrong: 1 }],
    });
    assert.deepStrictEqual(p.practiceResults, [{ question: '146 + 9', wrong: 1, done: true }]);
    assert.deepStrictEqual(p.ruleStats, [{ key: 'add_ten', correct: 12, wrong: 5 }]);
    const block = chat.contextBlock(p);
    assert.ok(block.includes('146 + 9: çözdü, 1 yanlış deneme'));
    assert.ok(block.includes("10'luk toplama: 12 doğru, 5 yanlış"));
  });

  await test('sütun işaretleme: tek sütun, mesaj başına bir kez', () => {
    const state = {};
    const r = tools.runTool('sutunu_goster', { sutun: 'yuzler' }, {}, state);
    assert.strictEqual(r.isError, false);
    assert.deepStrictEqual(r.action, { tur: 'isaret', sutun: 2 });
    assert.strictEqual(tools.runTool('sutunu_goster', { sutun: 'onlar' }, {}, state).isError, true);
    assert.strictEqual(tools.runTool('sutunu_goster', { sutun: 'milyonlar' }, {}, {}).isError, true);
  });

  console.log('sohbet turu (sahte model)');
  await test('örnek: "açtım" deyip açmayan cevap çocuğa gitmez', async () => {
    const ctx = { stepText: 'x', practice: true, taughtRules: ['add_direct', 'add_five'] };
    const lie = await chat.runTurn({ client: fakeClient([say('Bir örnek açtım: 2 + 4.')]), system: 'S', history: [], context: ctx, childText: 'örnek ver' });
    assert.strictEqual(lie.fellBack, true);
    assert.ok(lie.reasons.includes('acilmayan_ornek'));
    const real = await chat.runTurn({
      client: fakeClient([callTool('alistirma_ver', { baslangic: 2, islem: 'add', sayi: 4 }), say('Bir örnek açtım: 2 + 4.')]),
      system: 'S', history: [], context: ctx, childText: 'örnek ver',
    });
    assert.strictEqual(real.fellBack, false, real.reasons.join());
    assert.strictEqual(real.actions[0].tur, 'alistirma');
  });

  await test('araçla cevap: istek biçimi ve geçmiş', async () => {
    const client = fakeClient([
      callTool('hamleleri_hesapla', { baslangic: 8, islem: 'add', sayi: 6 }),
      say('Birler sütununda boş boncuk kalmadı. 1 boncuk gelir, 5 gider, 10 gelir. Cevap 14!'),
    ]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'neden 5 gidiyor' });
    assert.strictEqual(r.fellBack, false, r.reasons.join());
    assert.strictEqual(r.reply, 'Birler sütununda boş boncuk kalmadı. 1 boncuk gelir, 5 gider, 10 gelir. Cevap 14!');
    assert.strictEqual(client.calls.length, 2);
    const p = client.calls[0];
    assert.strictEqual(p.model, 'claude-haiku-5-5');
    assert.deepStrictEqual(p.output_config, { effort: 'medium' });
    // Haiku 5.5'te sunucu tarafı yedek model yok: beta uç noktası ve fallbacks gönderilmiyor.
    assert.strictEqual(p._beta, false);
    assert.strictEqual(p.fallbacks, undefined);
    assert.strictEqual(p.betas, undefined);
    assert.deepStrictEqual(p.cache_control, { type: 'ephemeral' });
    assert.strictEqual(p.thinking, undefined);
    assert.ok(p.messages[0].content.some((b) => b.text.includes('<cocuk>neden 5 gidiyor</cocuk>')));
    assert.ok(p.messages[0].content[0].text.includes('Beklenen cevap: 14'));
    const toolResult = client.calls[1].messages[2].content[0];
    assert.strictEqual(toolResult.type, 'tool_result');
    assert.strictEqual(JSON.parse(toolResult.content).sonuc, 14);
    assert.deepStrictEqual(r.history.map((m) => m.role), ['user', 'assistant', 'user', 'assistant']);
    assert.strictEqual(r.usage.input_tokens, 200);
  });
  await test('Opus 5.5 ile karşılaştırmada yedek model açık', async () => {
    const client = fakeClient([say('Birler sütununa bak.')]);
    await chat.runTurn({ client, model: 'claude-opus-5-5', system: 'S', history: [], context: CTX, childText: 'x' });
    const p = client.calls[0];
    assert.strictEqual(p._beta, true);
    assert.strictEqual(p.fallbacks, 'default');
    assert.deepStrictEqual(p.betas, ['server-side-fallback-2026-07-01']);
  });
  await test('uydurulan sayı: çocuk yedek metni görüyor, model cevabı geçmişte kalıyor', async () => {
    const client = fakeClient([say('Kardeşi 7 gider, sonra 20 olur.')]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'anlamadım' });
    assert.strictEqual(r.fellBack, true);
    assert.deepStrictEqual(r.reasons, ['kaynaksiz_sayi:7,20']);
    assert.ok(r.reply.startsWith('Birlikte bakalım!'));
    assert.strictEqual(r.history[r.history.length - 1].content[0].text, 'Kardeşi 7 gider, sonra 20 olur.');
  });
  await test('ret: geçmişe eklenmiyor', async () => {
    const client = fakeClient([{ stop_reason: 'refusal', content: [], usage }]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'x' });
    assert.strictEqual(r.fellBack, true);
    assert.deepStrictEqual(r.reasons, ['refusal']);
    assert.deepStrictEqual(r.history.map((m) => m.role), ['user']);
  });
  await test('yarıda kesilen cevap: geçmişe eklenmiyor', async () => {
    const client = fakeClient([{ stop_reason: 'max_tokens', content: [{ type: 'text', text: 'Yar' }], usage }]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'x' });
    assert.deepStrictEqual(r.reasons, ['max_tokens']);
    assert.strictEqual(r.history.length, 1);
  });
  await test('araç sınırı: döngü duruyor, araç isteği cevapsız kalmıyor', async () => {
    const loop = Array.from({ length: 10 }, (_, i) => callTool('hamleleri_hesapla', { baslangic: 1, islem: 'add', sayi: 1 }, `tu_${i}`));
    const client = fakeClient(loop);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'x' });
    assert.strictEqual(client.calls.length, chat.MAX_TOOL_ROUNDS + 1);
    assert.deepStrictEqual(r.reasons, ['arac_siniri']);
    const last = r.history[r.history.length - 1];
    assert.strictEqual(last.role, 'user');
    assert.strictEqual(last.content[0].is_error, true);
    assert.strictEqual(last.content[0].tool_use_id, `tu_${chat.MAX_TOOL_ROUNDS}`);
  });
  await test('düşünme blokları olduğu gibi geri gidiyor', async () => {
    const thinking = { type: 'thinking', thinking: '', signature: 'imza-123' };
    const first = fakeClient([{ ...callTool('hatayi_bul', {}), content: [thinking, { type: 'tool_use', id: 't1', name: 'hatayi_bul', input: {} }] }, say('Birler sütununa bak.', [thinking])]);
    const r1 = await chat.runTurn({ client: first, system: 'S', history: [], context: CTX, childText: 'x' });
    assert.deepStrictEqual(first.calls[1].messages[1].content[0], thinking);
    const second = fakeClient([say('Aferin!')]);
    const before = JSON.stringify(r1.history);
    const r2 = await chat.runTurn({ client: second, system: 'S', history: r1.history, context: CTX, childText: 'tamam' });
    assert.strictEqual(JSON.stringify(second.calls[0].messages.slice(0, r1.history.length)), before, 'eski geçmiş değişmemeli');
    assert.strictEqual(r2.history.length, r1.history.length + 2);
  });
  await test('gösterim ekran işi ve öneri balonları', async () => {
    const client = fakeClient([callTool('abakuste_goster', { baslangic: 8, islem: 'add', sayi: 6 }), say('İzle! 1 boncuk gelir, 5 gider, 10 gelir.')]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'göster' });
    assert.strictEqual(r.actions.length, 1);
    assert.strictEqual(r.actions[0].tur, 'goster');
    assert.deepStrictEqual(r.suggestions, ['neden_boyle', 'bir_daha_goster']);
  });
  await test('not ve maskeleme modele gidiyor', async () => {
    const client = fakeClient([say('Tamam.')]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'benim mailim ali@x.com', note: 'Yedek metin' });
    const blocks = client.calls[0].messages[0].content.map((b) => b.text);
    assert.ok(blocks.some((t) => t.startsWith('<not>') && t.includes('Yedek metin')));
    assert.ok(!blocks.join(' ').includes('ali@x.com'));
    assert.strictEqual(r.masked, true);
  });

  console.log('callable (sahte Firestore)');
  await test('oturum, günlük hak, tur sınırı ve hata', async () => {
    const env = fakeEnv();
    const responses = [];
    const fn = createSobiChat({ ...env, createClient: () => fakeClient(responses) });
    const ctx = { auth: { uid: 'u1' } };
    env.store.set('users/u1', { plan: 'Free' });

    responses.push(say('Birler sütununa bak.'));
    const r1 = await fn({ message: 'anlamadım', context: { base: 8, target: 14 } }, ctx);
    assert.strictEqual(r1.reply, 'Birler sütununa bak.');
    assert.strictEqual(r1.turnsLeft, createSobiChat.MAX_TURNS - 1);
    assert.strictEqual(r1.remainingToday, 1, 'Free: günde 2 ücretsiz');
    assert.strictEqual(r1.credits, 0);
    const session = env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`);
    assert.strictEqual(JSON.parse(session.historyJson).length, 2);
    assert.deepStrictEqual(session.transcript.map((t) => t.from), ['cocuk', 'sobi']);
    assert.strictEqual(session.promptVersion, createSobiChat.PROMPT_VERSION);

    responses.push(say('Aferin!'));
    const r2 = await fn({ message: 'tamam', sessionId: r1.sessionId, context: { base: 8, target: 14 } }, ctx);
    assert.strictEqual(r2.sessionId, r1.sessionId);
    assert.strictEqual(JSON.parse(env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`).historyJson).length, 4);

    // İki mesajla günlük hak bitti; kredi yokken mesaj gitmez, kredi varsa 1 düşer.
    assert.strictEqual(env.store.get('sobiChats/u1').usageCount, 2);
    await assert.rejects(fn({ message: 'x' }, ctx), (e) => e.code === 'resource-exhausted');
    env.store.get('sobiChats/u1').credits = 2;
    env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`).turns = createSobiChat.MAX_TURNS;
    const r3 = await fn({ message: 'bir daha', sessionId: r1.sessionId }, ctx);
    assert.strictEqual(r3.turnLimitReached, true);
    responses.push(say('Hadi bakalım.'));
    const r4 = await fn({ message: 'x', context: { base: 8, target: 14 } }, ctx);
    assert.strictEqual(r4.remainingToday, 0);
    assert.strictEqual(r4.credits, 1);
    assert.strictEqual(env.store.get('sobiChats/u1').credits, 1);
    assert.strictEqual(env.store.get('sobiChats/u1').monthCount, 3);
    assert.strictEqual(env.store.get('sobiChats/u1').monthCreditsUsed, 1);
    // Üst para paneli için users belgesindeki gösterim kopyası da aynı işlemde düştü.
    assert.strictEqual(env.store.get('users/u1').sobiCredits, 1);
    assert.strictEqual(env.store.get('users/u1').plan, 'Free', 'merge: diğer alanlar silinmemeli');
    await assert.rejects(fn({ message: 'x' }, {}), (e) => e.code === 'unauthenticated');

    env.store.set('users/u2', { banned: true });
    await assert.rejects(fn({ message: 'x' }, { auth: { uid: 'u2' } }), (e) => e.code === 'permission-denied');
  });
  await test('geçersiz düşünme bloğu: geçmiş sıfırlanıp bir kez yeniden deneniyor', async () => {
    const env = fakeEnv();
    const Anthropic = require('@anthropic-ai/sdk').default;
    let calls = 0;
    const client = { messages: { create: async (params) => {
      calls++;
      if (params.messages.length > 1) {
        throw new Anthropic.BadRequestError(400, { type: 'error', error: { type: 'invalid_request_error',
          message: 'messages.1.content.0: Invalid `signature` in `thinking` block. The block is bound to a different conversation.' } }, undefined, new Headers());
      }
      return say('Birlikte bakalım.');
    } } };
    const fn = createSobiChat({ ...env, createClient: () => client });
    env.store.set('users/u1', {});
    env.store.set('sobiChats/u1/sessions/oturum12345', {
      historyJson: JSON.stringify([{ role: 'user', content: 'eski' }, { role: 'assistant', content: [{ type: 'text', text: 'eski cevap' }] }]),
      turns: 2,
    });
    const warn = console.warn; console.warn = () => {};
    const r = await fn({ message: 'neden', sessionId: 'oturum12345', context: { stepText: 'x' } }, { auth: { uid: 'u1' } });
    console.warn = warn;
    assert.strictEqual(calls, 2);
    assert.strictEqual(r.reply, 'Birlikte bakalım.');
    assert.strictEqual(JSON.parse(env.store.get('sobiChats/u1/sessions/oturum12345').historyJson).length, 2);
  });
  await test('API hatası: yedek metin, hak düşmüyor', async () => {
    const env = fakeEnv();
    const Anthropic = require('@anthropic-ai/sdk').default;
    const failing = () => ({ messages: { create: async () => { throw new Anthropic.APIConnectionError({ message: 'yok' }); } } });
    const fn = createSobiChat({ ...env, createClient: failing });
    env.store.set('users/u1', {});
    const warn = console.warn; console.warn = () => {};
    const r = await fn({ message: 'x', context: { base: 8, target: 14 } }, { auth: { uid: 'u1' } });
    console.warn = warn;
    assert.strictEqual(r.unavailable, true);
    assert.ok(r.reply.startsWith('Birlikte bakalım!'));
    assert.strictEqual(env.store.has('sobiChats/u1'), false);
  });
  await test('yedek metin (denetime takılan cevap) hak ve kredi düşürmüyor', async () => {
    const env = fakeEnv();
    const fn = createSobiChat({ ...env, createClient: () => fakeClient([say('Kardeşi 7 gider, sonra 20 olur.')]) });
    env.store.set('users/u1', {});
    env.store.set('sobiChats/u1', { credits: 3 });
    const warn = console.warn; console.warn = () => {};
    const r = await fn({ message: 'anlamadım', context: { base: 8, target: 14 } }, { auth: { uid: 'u1' } });
    console.warn = warn;
    assert.strictEqual(r.fellBack, true);
    assert.strictEqual(env.store.get('sobiChats/u1').usageCount, undefined);
    assert.strictEqual(env.store.get('sobiChats/u1').credits, 3);
    assert.strictEqual(r.remainingToday, 2);
    assert.strictEqual(r.credits, 3);
  });
  await test('cüzdan: plan hakları, bedel sırası, ay sayacı, ek test', async () => {
    const now = Date.parse('2026-11-01T10:00:00Z');
    const day = wallet.trDay(now);
    assert.strictEqual(wallet.balanceOf(null, 'Free', now).freeLeft, 2);
    assert.strictEqual(wallet.balanceOf(null, 'Lite', now).freeLeft, 5);
    assert.strictEqual(wallet.balanceOf(null, 'Pro', now).freeLeft, 20);
    assert.strictEqual(wallet.balanceOf({ usageDay: '2026-10-31', usageCount: 2 }, 'Free', now).freeLeft, 2, 'dün sayılmaz');
    const c1 = wallet.chargeMessage({ usageDay: day, usageCount: 1, credits: 5 }, 'Free', now);
    assert.strictEqual(c1.paidWith, 'free');
    assert.strictEqual(c1.update.usageCount, 2);
    assert.strictEqual(c1.update.credits, undefined);
    const c2 = wallet.chargeMessage({ usageDay: day, usageCount: 2, credits: 5, monthKey: '2026-11', monthCount: 7 }, 'Free', now);
    assert.strictEqual(c2.paidWith, 'credit');
    assert.strictEqual(c2.update.credits, 4);
    assert.strictEqual(c2.update.monthCount, 8);
    assert.strictEqual(c2.after.freeLeft, 0);
    const c3 = wallet.chargeMessage({ usageDay: day, usageCount: 2, credits: 0, monthKey: '2026-10', monthCount: 40 }, 'Free', now);
    assert.strictEqual(c3.paidWith, 'none');
    assert.strictEqual(c3.update.monthCount, 1, 'yeni ay sıfırdan');
    assert.strictEqual(wallet.canSend({ freeLeft: 0, credits: -2 }), false, 'iade sonrası eksi kredi');

    const base = { plan: 'Free', approvedTeacher: false, keys: 5, chatData: null, testIndex: 1, now };
    assert.deepStrictEqual(wallet.extraTestDecision(base), { cost: 3, reason: 'keys' });
    assert.strictEqual(wallet.extraTestDecision({ ...base, keys: 2 }).insufficient, true);
    assert.strictEqual(wallet.extraTestDecision({ ...base, plan: 'Pro' }).cost, 0);
    assert.strictEqual(wallet.extraTestDecision({ ...base, approvedTeacher: true, keys: 0 }).cost, 0);
    const paid = { extraTestDay: day, extraTestIndex: 1 };
    assert.strictEqual(wallet.extraTestDecision({ ...base, chatData: paid }).reason, 'already_paid');
    assert.strictEqual(wallet.extraTestDecision({ ...base, chatData: paid, testIndex: 2 }).cost, 3);
  });
  await test('ek test callable: anahtar bir kez düşer', async () => {
    const env = fakeEnv();
    const w = require('../sobi/wallet')({ ...env, isApprovedTeacher: () => false });
    const ctx = { auth: { uid: 'u1' } };
    env.store.set('users/u1', { plan: 'Free', keys: 7 });
    const r1 = await w.startSobiExtraTest({ testIndex: 1 }, ctx);
    assert.strictEqual(r1.cost, 3);
    assert.strictEqual(env.store.get('users/u1').keys, 4);
    const again = await w.startSobiExtraTest({ testIndex: 1 }, ctx);
    assert.strictEqual(again.cost, 0, 'yarıda bırakılan test yeniden açılınca');
    assert.strictEqual(env.store.get('users/u1').keys, 4);
    await w.startSobiExtraTest({ testIndex: 2 }, ctx);
    assert.strictEqual(env.store.get('users/u1').keys, 1);
    await assert.rejects(w.startSobiExtraTest({ testIndex: 3 }, ctx), (e) => e.code === 'failed-precondition');
    await assert.rejects(w.startSobiExtraTest({ testIndex: 0 }, ctx), (e) => e.code === 'invalid-argument');
    const b = await w.sobiBalance({}, ctx);
    assert.strictEqual(b.freeLeft, 2);
    assert.strictEqual(b.extraTestKeys, 3);
    // Kopyadan önce paket almış hesap: bakiye okununca üst paneldeki gösterim kopyası düzelir.
    env.store.set('sobiChats/u1', { ...(env.store.get('sobiChats/u1') || {}), credits: 17 });
    await w.sobiBalance({}, ctx);
    assert.strictEqual(env.store.get('users/u1').sobiCredits, 17);
    assert.strictEqual(env.store.get('users/u1').keys, 1, 'merge: diğer alanlar kalmalı');
  });

  await test('yeni örnek (startSobiExample): önce günlük hak, sonra kredi, ikisi de yoksa reddedilir', async () => {
    const env = fakeEnv();
    const w = require('../sobi/wallet')({ ...env, isApprovedTeacher: () => false });
    const ctx = { auth: { uid: 'u1' } };
    env.store.set('users/u1', { plan: 'Free' });
    env.store.set('sobiChats/u1', { credits: 1 });
    const r1 = await w.startSobiExample({}, ctx);
    assert.deepStrictEqual([r1.paidWith, r1.remainingToday, r1.credits], ['free', 1, 1]);
    await w.startSobiExample({}, ctx);
    const r3 = await w.startSobiExample({}, ctx);
    assert.deepStrictEqual([r3.paidWith, r3.remainingToday, r3.credits], ['credit', 0, 0]);
    assert.strictEqual(env.store.get('users/u1').sobiCredits, 0, 'üst paneldeki kopya');
    assert.strictEqual(env.store.get('sobiChats/u1').monthExamples, 3);
    assert.strictEqual(env.store.get('sobiChats/u1').monthCount, 3);
    await assert.rejects(w.startSobiExample({}, ctx), (e) => e.code === 'resource-exhausted');
    await assert.rejects(w.startSobiExample({}, {}), (e) => e.code === 'unauthenticated');
    assert.ok(chat.creditsText().includes('yeni örnek de 1 mesaj'));
  });

  await test('örnek açma (alistirma_ver): yalnızca Sobi ekranında, öğrenilmiş kurallarla', async () => {
    const input = { baslangic: 3, islem: 'add', sayi: 4 }; // 5'lik kural
    assert.strictEqual(tools.runTool('alistirma_ver', input, { practice: false }, {}).isError, true);
    const early = tools.runTool('alistirma_ver', input, { practice: true, taughtRules: ['add_direct'] }, {});
    assert.strictEqual(early.isError, true);
    assert.ok(early.content.includes('add_five'));
    const ok = tools.runTool('alistirma_ver', input, { practice: true, taughtRules: ['add_direct', 'add_five'] }, {});
    assert.strictEqual(ok.isError, false);
    assert.strictEqual(ok.action.tur, 'alistirma');
    assert.strictEqual(ok.action.sonuc, 7);
    // Elde zinciri de sayılır: 95 + 8 boncuk kuralı ve onlarda 10'luk.
    const chain = tools.runTool('alistirma_ver', { baslangic: 95, islem: 'add', sayi: 8 }, { practice: true, taughtRules: ['add_direct', 'add_five', 'add_bead'] }, {});
    assert.ok(chain.isError && chain.content.includes('add_ten'));
    assert.ok(chat.contextBlock(chat.sanitizeContext({ practice: true })).includes('örnek işlem açılabilir'));
  });

  await test('hazır açıklama: balon modelsiz, hak düşmüyor; beğeni kaydı ve geri alma', async () => {
    const env = fakeEnv();
    const fn = createSobiChat({ ...env, createClient: () => ({ messages: { create: async () => { throw new Error('model çağrılmamalı'); } } }) });
    const feedback = require('../sobi/feedback')({ ...env, promptVersion: 'pv' });
    env.store.set('users/u1', { plan: 'Free' });
    const ctx = { auth: { uid: 'u1' } };
    // t3'ün 25. adımı (4 + 2 anlatımı): onaylı açıklaması var (sobi/aciklamalar_tr.json).
    const stepContext = { stepText: "Bu yüzden 5'lik kuralı uygulayacağız.", questionText: '4 + 2', tutorialNumber: 3, stepIndex: 25 };
    const r = await fn({ message: 'Bunu anlamadım', suggestion: 'bunu_anlamadim', context: stepContext }, ctx);
    assert.ok(r.reply.startsWith('Abaküste 4 var'));
    assert.strictEqual(r.source, 'hazir');
    assert.strictEqual(r.remainingToday, 2);
    assert.strictEqual(r.messageIndex, 1);
    const path = `sobiChats/u1/sessions/${r.sessionId}`;
    const line = env.store.get(path).transcript[1];
    assert.strictEqual(line.source, 'hazir');
    assert.strictEqual(line.aciklama, 't3_p5');
    assert.strictEqual(env.store.get('sobiChats/u1'), undefined, 'günlük sayaç yazılmamalı');
    // Geçmişte çocuğun mesajı ve düz metin cevap: sonraki model turu görsün.
    assert.deepStrictEqual(JSON.parse(env.store.get(path).historyJson).map((m) => m.role), ['user', 'assistant']);

    await feedback({ sessionId: r.sessionId, messageIndex: 1, value: -1 }, ctx);
    const fb = env.store.get(`sobiChats/u1/sobiFeedback/${r.sessionId}_1`);
    assert.strictEqual(fb.value, -1);
    assert.strictEqual(fb.kind, 'ders');
    assert.strictEqual(fb.childText, 'Bunu anlamadım');
    assert.strictEqual(fb.aciklama, 't3_p5');
    assert.strictEqual(fb.tutorialNumber, 3);
    assert.strictEqual(env.store.get(path).transcript[1].rating, -1);
    await feedback({ sessionId: r.sessionId, messageIndex: 1, value: 0 }, ctx);
    assert.strictEqual(env.store.has(`sobiChats/u1/sobiFeedback/${r.sessionId}_1`), false);
    await assert.rejects(feedback({ sessionId: r.sessionId, messageIndex: 0, value: 1 }, ctx), (e) => e.code === 'not-found');
    await assert.rejects(feedback({ sessionId: r.sessionId, messageIndex: 1, value: 5 }, ctx), (e) => e.code === 'invalid-argument');
    await assert.rejects(feedback({ sessionId: r.sessionId, messageIndex: 1, value: 1 }, {}), (e) => e.code === 'unauthenticated');

    // "Bize daha fazla bilgi ver": oyla birlikte yorum, kişisel bilgi maskeli.
    await feedback({ sessionId: r.sessionId, messageIndex: 1, value: -1, comment: 'çok uzun, annem 0532 111 22 33' }, ctx);
    const withComment = env.store.get(`sobiChats/u1/sobiFeedback/${r.sessionId}_1`);
    assert.ok(withComment.comment.startsWith('çok uzun') && !withComment.comment.includes('0532'));
    // Bildir: oydan bağımsız, ayrı kayıt, sohbet satırı işaretli, günlüğe uyarı.
    const warns = []; const warn = console.warn; console.warn = (...a) => warns.push(a);
    await feedback({ sessionId: r.sessionId, messageIndex: 1, report: true, comment: 'kötü söz' }, ctx);
    console.warn = warn;
    const rep = env.store.get(`sobiChats/u1/sobiReports/${r.sessionId}_1`);
    assert.strictEqual(rep.status, 'yeni');
    assert.strictEqual(rep.comment, 'kötü söz');
    assert.strictEqual(rep.reply, r.reply);
    assert.strictEqual(env.store.get(path).transcript[1].reported, true);
    assert.strictEqual(env.store.get(path).transcript[1].rating, -1, 'bildirim oyu silmemeli');
    assert.ok(warns.some((w) => String(w[0]).startsWith('sobiReport')));
    await assert.rejects(feedback({ sessionId: r.sessionId, messageIndex: 1 }, ctx), (e) => e.code === 'invalid-argument');

    // Yerel (kural motoru) mesajı: sohbet kaydı yok, metin istemciden; oy, geri alma, bildirim.
    const local = { local: true, localId: 'abcd1234efgh', text: 'Birler basamağında 3 eklerken...', screen: 'analiz', question: '12 + 22' };
    await feedback({ ...local, value: 1 }, ctx);
    const lf = env.store.get('sobiChats/u1/sobiFeedback/local_abcd1234efgh');
    assert.strictEqual(lf.value, 1);
    assert.strictEqual(lf.source, 'kural');
    assert.strictEqual(lf.question, '12 + 22');
    await feedback({ ...local, value: 0 }, ctx);
    assert.strictEqual(env.store.has('sobiChats/u1/sobiFeedback/local_abcd1234efgh'), false);
    const w2 = console.warn; console.warn = () => {};
    await feedback({ ...local, report: true, comment: 'anlamadım' }, ctx);
    console.warn = w2;
    assert.strictEqual(env.store.get('sobiChats/u1/sobiReports/local_abcd1234efgh').status, 'yeni');
    await assert.rejects(feedback({ ...local, text: '', value: 1 }, ctx), (e) => e.code === 'invalid-argument');
    await assert.rejects(feedback({ ...local, localId: 'x', value: 1 }, ctx), (e) => e.code === 'invalid-argument');

    // Onaylı açıklaması olmayan balon ("Bir örnek ver") ve soru adımı: model çalışır.
    assert.strictEqual(chat.directReplyFor(chat.sanitizeContext(stepContext), 'ornek_ver'), null);
    assert.strictEqual(chat.directReplyFor(chat.sanitizeContext({ ...stepContext, base: 4, target: 6 }), 'bunu_anlamadim'), null);
    assert.ok(chat.contextBlock(chat.sanitizeContext(stepContext)).includes('Onaylı açıklama'));
  });

  console.log('genel sohbet (Sobi sekmesi)');
  await test('bağlam: plan, bakiye, kural başarısı, en zorlandığı kural, seviyeler', () => {
    const g = chat.generalContext({
      plan: 'Lite', freeLeft: 3, freeLimit: 5, credits: 7,
      rules: [{ key: 'add_five', c: 45, w: 5 }, { key: 'add_ten', c: 30, w: 20 }, { key: 'sub_ten', c: 2, w: 3 }, { key: 'yok', c: 1, w: 1 }],
      levels: [{ key: 'add_ten', level: 4 }],
    });
    assert.strictEqual(g.mode, 'genel');
    assert.strictEqual(g.practice, true);
    assert.strictEqual(g.general.rules.length, 3, 'bilinmeyen kural atılır');
    const block = chat.contextBlock(g);
    assert.ok(block.includes('Genel sohbet'));
    assert.ok(block.includes('Planı: Lite. Bugün kalan ücretsiz Sobi mesajı: 3 / 5. Sobi kredisi: 7.'));
    assert.ok(block.includes("10'luk toplama: 30 doğru, 20 yanlış (yüzde 60)"));
    // sub_ten'in oranı daha düşük ama 10 işlemden az: güvenilmez, sayılmaz.
    assert.ok(block.includes("En çok zorlandığı kural: 10'luk toplama"));
    assert.ok(block.includes("Sobi testi seviyeleri (1 ile 20 arası): 10'luk toplama 4"));
    assert.ok(!block.includes('Adımın metni'));
    assert.ok(chat.contextBlock(chat.generalContext({})).includes('henüz kural geçmişi yok'));
    assert.strictEqual(chat.weakestRule([{ key: 'add_five', c: 95, w: 5 }]), null, '%90 üstü zorlanma sayılmaz');
    assert.strictEqual(chat.toolsFor(g), tools.GENERAL_TOOL_DEFS);
    assert.strictEqual(chat.toolsFor(CTX), tools.TOOL_DEFS, 'ders sohbetinin araç listesi değişmedi');
    assert.deepStrictEqual(tools.GENERAL_TOOL_DEFS.slice(0, tools.TOOL_DEFS.length), tools.TOOL_DEFS);
    assert.deepStrictEqual(chat.suggestionsFor(g, []), []);
  });
  await test('kural özeti homeworkRuleStats\'tan: pencere toplamdan önce, çarpma atılır', () => {
    const f = createSobiChat.ruleFacts({
      tally: { add_ten: { c: 500, w: 20 }, sub_five: { c: 8, w: 4 }, mul_1x1: { c: 3, w: 3 } },
      recent: { add_ten: [{ c: 20, w: 10 }, { c: 15, w: 5 }] },
      sobiLevel: { add_ten: 6, mul_1x1: 2 },
    });
    assert.deepStrictEqual(f.rules, [{ key: 'add_ten', c: 35, w: 15 }, { key: 'sub_five', c: 8, w: 4 }]);
    assert.deepStrictEqual(f.levels, [{ key: 'add_ten', level: 6 }]);
    assert.deepStrictEqual(createSobiChat.ruleFacts(null), { rules: [], levels: [] });
  });
  await test('araçlar: test ve mağaza düğmesi yalnızca genel sohbette; abaküs araçları düğmeye döner', () => {
    const g = chat.generalContext({});
    const t = tools.runTool('test_baslat', {}, g, {});
    assert.deepStrictEqual(t.action, { tur: 'test' });
    assert.strictEqual(tools.runTool('test_baslat', {}, CTX, {}).isError, true);
    const st = {};
    assert.deepStrictEqual(tools.runTool('magaza_goster', { neyi: 'kredi' }, g, st).action, { tur: 'kredi' });
    assert.strictEqual(tools.runTool('magaza_goster', { neyi: 'pro' }, g, st).isError, true, 'bir mesajda bir kez');
    assert.deepStrictEqual(tools.runTool('magaza_goster', { neyi: 'pro' }, g, {}).action, { tur: 'pro' });
    assert.strictEqual(tools.runTool('magaza_goster', { neyi: 'altin' }, g, {}).isError, true);
    assert.strictEqual(tools.runTool('hatayi_bul', {}, g, {}).isError, true);
    assert.strictEqual(tools.runTool('sutunu_goster', { sutun: 'birler' }, g, {}).isError, true);
    const show = tools.runTool('abakuste_goster', { baslangic: 8, islem: 'add', sayi: 6 }, g, {});
    assert.strictEqual(show.isError, false, 'adım yokken de gösterim (ayrı ekranda)');
    assert.strictEqual(show.action.tur, 'goster');
    assert.ok(show.content.includes('düğme'));
    const ex = tools.runTool('alistirma_ver', { baslangic: 3, islem: 'add', sayi: 4 }, g, {});
    assert.strictEqual(ex.action.tur, 'alistirma');
  });
  await test('tur: iki sistem bloğu (ikisi de önbellekli), genel araçlar, rehber sayıları serbest', async () => {
    const client = fakeClient([callTool('test_baslat', {}), say("Pro'da günde 20 mesaj var. Teste başla düğmesine dokun!")]);
    const r = await chat.runTurn({
      client, system: ['DERS', 'GENEL'], history: [], context: chat.generalContext({}), childText: 'test',
      extraSources: new Set([20]),
    });
    assert.strictEqual(r.fellBack, false);
    assert.deepStrictEqual(r.actions, [{ tur: 'test' }]);
    assert.deepStrictEqual(client.calls[0].system.map((b) => [b.text, b.cache_control.type]), [['DERS', 'ephemeral'], ['GENEL', 'ephemeral']]);
    assert.deepStrictEqual(client.calls[0].tools.map((t) => t.name), tools.GENERAL_TOOL_DEFS.map((t) => t.name));
    // Ders sohbeti: tek blok, aynen eskisi gibi.
    const c2 = fakeClient([say('Tamam.')]);
    await chat.runTurn({ client: c2, system: 'DERS', history: [], context: CTX, childText: 'x' });
    assert.strictEqual(c2.calls[0].system.length, 1);
    // Rehberde olmayan bir sayı: yedek metin (genel sohbetin kendi metni, hamle değil).
    const r3 = await chat.runTurn({ client: fakeClient([say('Pro 77 lira.')]), system: ['D', 'G'], history: [], context: chat.generalContext({}), childText: 'pro kaç para', extraSources: new Set([20]) });
    assert.strictEqual(r3.fellBack, true);
    assert.ok(r3.reply.startsWith('Bunu şu an cevaplayamadım'));
  });
  await test('genel sohbet callable: bağlam sunucudan, başlık, düğme kaydı, kredi düşümü', async () => {
    const env = fakeEnv();
    const responses = [];
    const client = fakeClient(responses);
    const fn = createSobiChat({ ...env, createClient: () => client });
    const ctx = { auth: { uid: 'u1' } };
    const day = wallet.trDay(Date.now());
    env.store.set('users/u1', { plan: 'Free' });
    env.store.set('sobiChats/u1', { usageDay: day, usageCount: 2, credits: 2 });
    env.store.set('homeworkRuleStats/u1', { tally: { add_ten: { c: 10, w: 10 } } });

    responses.push(callTool('test_baslat', {}), say('Sana bir test hazırladım, düğmeye dokun!'));
    // Uygulamanın gönderdiği bağlam genel kipte yok sayılır: adımı uydurup bedava hazır açıklama alamasın.
    const r1 = await fn({ mode: 'genel', message: 'En çok nerede zorlanıyorum? Bana bir test hazırlar mısın lütfen', context: { tutorialNumber: 3, stepIndex: 25 } }, ctx);
    assert.strictEqual(r1.reply, 'Sana bir test hazırladım, düğmeye dokun!');
    assert.deepStrictEqual(r1.actions, [{ tur: 'test' }]);
    assert.strictEqual(r1.source, 'model');
    assert.strictEqual(r1.remainingToday, 0);
    assert.strictEqual(r1.credits, 1, 'ücretsiz hak bitmiş: 1 kredi düştü');
    assert.strictEqual(env.store.get('sobiChats/u1').credits, 1);
    assert.strictEqual(env.store.get('users/u1').sobiCredits, 1);
    const sent = client.calls[0];
    assert.strictEqual(sent.system.length, 2);
    assert.ok(sent.system[1].text.startsWith('# Genel sohbet'));
    assert.ok(sent.system[1].text.includes('yalnızca'), 'konu sınırı talimatta');
    const block = sent.messages[0].content[0].text;
    assert.ok(block.includes("En çok zorlandığı kural: 10'luk toplama"));
    assert.ok(block.includes('Sobi kredisi: 2.'), 'bağlam bedelden önceki bakiye');
    const session = env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`);
    assert.strictEqual(session.kind, 'genel');
    assert.strictEqual(session.title, 'En çok nerede zorlanıyorum? Bana bir…');
    assert.strictEqual(session.promptVersion, createSobiChat.GENERAL_PROMPT_VERSION);
    assert.deepStrictEqual(session.transcript[1].buttons, [{ tur: 'test' }]);

    // Sohbet sürerken başlık değişmez.
    responses.push(say('Aferin!'));
    await fn({ mode: 'genel', message: 'tamam', sessionId: r1.sessionId }, ctx);
    assert.strictEqual(env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`).title, 'En çok nerede zorlanıyorum? Bana bir…');
    assert.strictEqual(env.store.get('sobiChats/u1').credits, 0);
    // Hak da kredi de bitti: mesaj gitmez; ama "Krediler nasıl çalışır?" hazır cevap, ücretsiz.
    await assert.rejects(fn({ mode: 'genel', message: 'merhaba' }, ctx), (e) => e.code === 'resource-exhausted');
    const before = client.calls.length;
    const k = await fn({ mode: 'genel', message: 'Krediler nasıl çalışır?', suggestion: 'krediler' }, ctx);
    assert.strictEqual(client.calls.length, before, 'model çağrılmadı');
    assert.strictEqual(k.source, 'hazir');
    assert.ok(k.reply.includes('Free planda 2, Lite\'ta 5, Pro\'da 20'));
    assert.strictEqual(env.store.get(`sobiChats/u1/sessions/${k.sessionId}`).transcript[1].aciklama, 'genel_krediler');
    // Beğeni kaydı Sobi sekmesinin sohbeti olduğunu ve genel talimatın sürümünü taşır (haftalık rapor ayırıyor).
    const feedback = require('../sobi/feedback')({ ...env, promptVersion: createSobiChat.PROMPT_VERSION });
    await feedback({ sessionId: r1.sessionId, messageIndex: 1, value: 1 }, ctx);
    const fb = env.store.get(`sobiChats/u1/sobiFeedback/${r1.sessionId}_1`);
    assert.strictEqual(fb.kind, 'genel');
    assert.strictEqual(fb.promptVersion, createSobiChat.GENERAL_PROMPT_VERSION);
    // Ders sohbetinde aynı balon anahtarı hazır cevap değil.
    assert.strictEqual(chat.directReplyFor(CTX, 'krediler'), null);
  });
  await test('konu dışı: model cevabı rehberde olmayan sayı uydurursa yedek metin, bedel yok', async () => {
    const env = fakeEnv();
    const fn = createSobiChat({ ...env, createClient: () => fakeClient([say('Türkiye\'nin başkenti Ankara, nüfusu 5800000.')]) });
    env.store.set('users/u1', {});
    const warn = console.warn; console.warn = () => {};
    const r = await fn({ mode: 'genel', message: 'Türkiyenin başkenti ne?' }, { auth: { uid: 'u1' } });
    console.warn = warn;
    assert.strictEqual(r.fellBack, true);
    assert.ok(r.reply.startsWith('Bunu şu an cevaplayamadım'));
    assert.strictEqual(r.remainingToday, 2);
    // Rehberdeki sayılar (plan hakları, can, anahtar, kredi paketleri) cevapta serbest.
    const fn2 = createSobiChat({ ...env, createClient: () => fakeClient([say("Lite'ta 10 can var, her 7 dakikada 1 dolar; ek test 3 anahtar. Paketler 50 ve 120 kredi.")]) });
    const r2 = await fn2({ mode: 'genel', message: 'Lite ne verir?' }, { auth: { uid: 'u1' } });
    assert.strictEqual(r2.fellBack, false);
  });
  await test('sobiSessions: liste yalnızca genel sohbetler, aç, sil; ders sohbetine dokunulmaz', async () => {
    const env = fakeEnv();
    const list = require('../sobi/sessions')(env);
    const ctx = { auth: { uid: 'u1' } };
    const ts = (ms) => ({ ms });
    env.store.set('sobiChats/u1/sessions/genelEski01', { kind: 'genel', title: 'Eski', updatedAt: ts(1000), turns: 2,
      transcript: [{ from: 'cocuk', text: 'selam' }, { from: 'sobi', text: 'Merhaba!', rating: 1, buttons: [{ tur: 'test' }] }] });
    env.store.set('sobiChats/u1/sessions/genelYeni01', { kind: 'genel', title: 'Yeni', updatedAt: ts(5000), turns: 1, transcript: [] });
    env.store.set('sobiChats/u1/sessions/dersSohbet1', { stepId: 'x', updatedAt: ts(9000), turns: 3, transcript: [] });
    env.store.set('sobiChats/u2/sessions/baskasi0001', { kind: 'genel', title: 'Başkası', updatedAt: ts(7000) });

    const l = await list({ action: 'list' }, ctx);
    assert.deepStrictEqual(l.sessions.map((s) => [s.id, s.title, s.updatedAt]), [['genelYeni01', 'Yeni', 5000], ['genelEski01', 'Eski', 1000]]);
    const g = await list({ action: 'get', sessionId: 'genelEski01' }, ctx);
    assert.deepStrictEqual(g.messages.map((m) => [m.from, m.index, m.rating]), [['cocuk', 0, 0], ['sobi', 1, 1]]);
    assert.deepStrictEqual(g.messages[1].buttons, [{ tur: 'test' }]);
    await assert.rejects(list({ action: 'get', sessionId: 'dersSohbet1' }, ctx), (e) => e.code === 'not-found');
    await assert.rejects(list({ action: 'delete', sessionId: 'dersSohbet1' }, ctx), (e) => e.code === 'not-found');
    await assert.rejects(list({ action: 'delete', sessionId: 'baskasi0001' }, ctx), (e) => e.code === 'not-found', 'başkasının sohbeti');
    assert.deepStrictEqual(await list({ action: 'delete', sessionId: 'genelEski01' }, ctx), { deleted: true });
    assert.strictEqual(env.store.has('sobiChats/u1/sessions/genelEski01'), false);
    assert.strictEqual(env.store.has('sobiChats/u1/sessions/dersSohbet1'), true);
    assert.strictEqual(env.store.has('sobiChats/u2/sessions/baskasi0001'), true);
    assert.deepStrictEqual((await list({ action: 'list' }, ctx)).sessions.map((s) => s.id), ['genelYeni01']);
    await assert.rejects(list({ action: 'get', sessionId: 'x' }, ctx), (e) => e.code === 'invalid-argument');
    await assert.rejects(list({ action: 'list' }, {}), (e) => e.code === 'unauthenticated');
  });
  await test('genel sohbet sınırı: yeni sohbet açılınca en eskiler silinir, ders sohbetleri kalır', async () => {
    const env = fakeEnv();
    const sessions = require('../sobi/sessions');
    const fn = createSobiChat({ ...env, createClient: () => ({ messages: { create: async () => { throw new Error('model çağrılmamalı'); } } }) });
    env.store.set('users/u1', {});
    for (let i = 0; i < sessions.MAX_GENERAL; i++) {
      env.store.set(`sobiChats/u1/sessions/genel${String(i).padStart(4, '0')}`, { kind: 'genel', updatedAt: { ms: 1000 + i } });
    }
    env.store.set('sobiChats/u1/sessions/dersEski0001', { updatedAt: { ms: 1 } });
    // Hazır cevap da yeni bir genel sohbet açar.
    const r = await fn({ mode: 'genel', message: 'Krediler nasıl çalışır?', suggestion: 'krediler' }, { auth: { uid: 'u1' } });
    assert.strictEqual(env.store.has('sobiChats/u1/sessions/genel0000'), false, 'en eski silindi');
    assert.strictEqual(env.store.has('sobiChats/u1/sessions/genel0001'), true);
    assert.strictEqual(env.store.has(`sobiChats/u1/sessions/${r.sessionId}`), true, 'yeni sohbet kaldı');
    assert.strictEqual(env.store.has('sobiChats/u1/sessions/dersEski0001'), true, 'ders sohbetine dokunulmaz');
    // Var olan sohbete yazmak bir şey silmez.
    await fn({ mode: 'genel', message: 'Krediler nasıl çalışır?', suggestion: 'krediler', sessionId: r.sessionId }, { auth: { uid: 'u1' } });
    assert.strictEqual(env.store.has('sobiChats/u1/sessions/genel0001'), true);
  });
  await test('TTL ilkeleri: Sobi sohbetleri, beğeniler ve bildirimler expiresAt ile silinir', () => {
    const overrides = require('../../firestore.indexes.json').fieldOverrides;
    for (const cg of ['sessions', 'sobiFeedback', 'sobiReports']) {
      assert.ok(overrides.some((o) => o.collectionGroup === cg && o.fieldPath === 'expiresAt' && o.ttl === true), cg);
    }
  });
  await test('başlık: kısa metin aynen, uzun metin kelime sınırında', () => {
    assert.strictEqual(createSobiChat.titleOf('Merhaba'), 'Merhaba');
    assert.strictEqual(createSobiChat.titleOf(''), 'Sohbet');
    assert.ok(createSobiChat.titleOf('a'.repeat(80)).length <= 41);
  });

  console.log(`\n${passed} test geçti.`);
}

/** Bellekte çalışan en küçük Firestore + firebase-functions taklidi. */
function fakeEnv() {
  const store = new Map();
  let seq = 0;
  const snap = (p) => {
    const d = store.get(p);
    return { exists: d !== undefined, id: p.split('/').pop(), data: () => d, get: (f) => (d ? d[f] : undefined) };
  };
  const docRef = (p) => ({
    id: p.split('/').pop(),
    get: async () => snap(p),
    set: async (data, opts) => { store.set(p, opts && opts.merge ? { ...(store.get(p) || {}), ...data } : { ...data }); },
    delete: async () => { store.delete(p); },
    collection: (name) => colRef(`${p}/${name}`),
  });
  // Sorgu: yalnızca eşitlik süzgeci ve select (sobiSessions); doğrudan alt belgeler.
  const query = (p, filters) => ({
    where: (field, op, value) => query(p, [...filters, (d) => op === '==' && d[field] === value]),
    select: () => query(p, filters),
    get: async () => ({
      docs: [...store.keys()]
        .filter((k) => k.startsWith(`${p}/`) && !k.slice(p.length + 1).includes('/'))
        .filter((k) => filters.every((f) => f(store.get(k))))
        .map(snap),
    }),
  });
  const colRef = (p) => ({ doc: (id) => docRef(`${p}/${id || `oto${++seq}abcdefgh`}`), ...query(p, []) });
  const db = {
    collection: (name) => colRef(name),
    batch: () => {
      const ops = [];
      return {
        set: (ref, data, opts) => ops.push(() => ref.set(data, opts)),
        delete: (ref) => ops.push(() => ref.delete()),
        commit: async () => { for (const op of ops) await op(); },
      };
    },
    runTransaction: async (fn) => {
      const ops = [];
      const tx = {
        get: (ref) => ref.get(),
        set: (ref, data, opts) => ops.push(() => ref.set(data, opts)),
        update: (ref, data) => ops.push(() => ref.set(data, { merge: true })),
        delete: (ref) => ops.push(() => ref.delete()),
      };
      const out = await fn(tx);
      for (const op of ops) await op();
      return out;
    },
  };
  class HttpsError extends Error { constructor(code, message) { super(message); this.code = code; } }
  const functions = { runWith: () => ({ https: { onCall: (fn) => fn } }), https: { HttpsError, onCall: (fn) => fn } };
  const admin = { firestore: { Timestamp: { fromMillis: (ms) => ({ ms }) } } };
  const isAskQuestionRestricted = (u) => u.banned === true;
  const effectivePlan = (u) => u.plan || 'Free';
  return { store, db, functions, admin, isAskQuestionRestricted, effectivePlan };
}

main().catch((e) => { console.error(e); process.exit(1); });

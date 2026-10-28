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
const guard = require('../sobi/guard');
const chat = require('../sobi/chat');
const createSobiChat = require('../sobi/callable');

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
  await test('öğreticide alıştırma aracı modele verilmiyor', () => {
    assert.ok(!chat.toolsFor(CTX).some((t) => t.name === 'alistirma_ver'));
    assert.ok(chat.toolsFor({ ...CTX, practice: true }).some((t) => t.name === 'alistirma_ver'));
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
  await test('bağlam temizleniyor', () => {
    const c = chat.sanitizeContext({ stepId: 'T8; drop', base: -1, target: 100000, current: 3.2, attempts: 5, valueLog: [1, -2, 'x', 3], extra: 'gizli' });
    assert.deepStrictEqual(c, { stepId: null, stepText: '', questionText: '', base: null, target: null, current: null, attempts: 5, valueLog: [1, 3], practice: false });
  });

  console.log('sohbet turu (sahte model)');
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
    const client = fakeClient([say('Kardeşi 3 gider, sonra 20 olur.')]);
    const r = await chat.runTurn({ client, system: 'S', history: [], context: CTX, childText: 'anlamadım' });
    assert.strictEqual(r.fellBack, true);
    assert.deepStrictEqual(r.reasons, ['kaynaksiz_sayi:3,20']);
    assert.ok(r.reply.startsWith('Birlikte bakalım!'));
    assert.strictEqual(r.history[r.history.length - 1].content[0].text, 'Kardeşi 3 gider, sonra 20 olur.');
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
    assert.strictEqual(r1.turnsLeft, 9);
    assert.strictEqual(r1.remainingToday, 19);
    const session = env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`);
    assert.strictEqual(JSON.parse(session.historyJson).length, 2);
    assert.deepStrictEqual(session.transcript.map((t) => t.from), ['cocuk', 'sobi']);
    assert.strictEqual(session.promptVersion, createSobiChat.PROMPT_VERSION);

    responses.push(say('Aferin!'));
    const r2 = await fn({ message: 'tamam', sessionId: r1.sessionId, context: { base: 8, target: 14 } }, ctx);
    assert.strictEqual(r2.sessionId, r1.sessionId);
    assert.strictEqual(JSON.parse(env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`).historyJson).length, 4);

    env.store.get(`sobiChats/u1/sessions/${r1.sessionId}`).turns = 10;
    const r3 = await fn({ message: 'bir daha', sessionId: r1.sessionId }, ctx);
    assert.strictEqual(r3.turnLimitReached, true);

    env.store.get('sobiChats/u1').usageCount = 20;
    await assert.rejects(fn({ message: 'x' }, ctx), (e) => e.code === 'resource-exhausted');
    await assert.rejects(fn({ message: 'x' }, {}), (e) => e.code === 'unauthenticated');

    env.store.set('users/u2', { banned: true });
    await assert.rejects(fn({ message: 'x' }, { auth: { uid: 'u2' } }), (e) => e.code === 'permission-denied');
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
    collection: (name) => colRef(`${p}/${name}`),
  });
  const colRef = (p) => ({ doc: (id) => docRef(`${p}/${id || `oto${++seq}abcdefgh`}`) });
  const db = {
    collection: (name) => colRef(name),
    batch: () => {
      const ops = [];
      return { set: (ref, data, opts) => ops.push(() => ref.set(data, opts)), commit: async () => { for (const op of ops) await op(); } };
    },
  };
  class HttpsError extends Error { constructor(code, message) { super(message); this.code = code; } }
  const functions = { runWith: () => ({ https: { onCall: (fn) => fn } }), https: { HttpsError } };
  const admin = { firestore: { Timestamp: { fromMillis: (ms) => ({ ms }) } } };
  const isAskQuestionRestricted = (u) => u.banned === true;
  const effectivePlan = (u) => u.plan || 'Free';
  return { store, db, functions, admin, isAskQuestionRestricted, effectivePlan };
}

main().catch((e) => { console.error(e); process.exit(1); });

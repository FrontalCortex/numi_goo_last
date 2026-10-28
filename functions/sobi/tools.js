'use strict';

/**
 * SOBİ'NİN ARAÇLARI — dil modelinin çağırdığı fonksiyonlar.
 *
 * NEDEN ARAÇ
 *   Model hamle hesaplamıyor (bkz. prompt_tr.md "Hesap yapma, araç kullan"): 5'lik/10'luk
 *   kurallarda yanlış adım uydurabiliyor. Hamleyi stepExplainer.js hesaplıyor, model yalnızca
 *   anlatıyor. Araç sonuçlarındaki sayılar guard.js'in "cevaptaki her sayı bir kaynakta geçmeli"
 *   denetiminin de kaynağı.
 *
 * Anlatım metinleri derslerdeki sözlerle aynı ("5 gelir, kardeş gider", "10 gelir, büyük kardeş
 * gider"; strings_tutorial.xml). Sayılara Türkçe ek getirilmiyor ("4'ün" yerine "kardeşi 1"):
 * ek sayıya göre değişiyor, yanlış ek çocuğun gözüne batar.
 *
 * Bütün araçlar `strict: true`: model şemaya uymayan girdi üretemiyor. Aralık denetimi (0..99999)
 * yine burada, çünkü şema sayının büyüklüğünü sınırlamıyor.
 */

const explainer = require('./stepExplainer');

const COLUMN_NAMES = ['birler', 'onlar', 'yüzler', 'binler', 'on binler'];
const RULE_NAMES = { direct: 'kuralsız', five: "5'lik kural", ten: "10'luk kural", bead: 'boncuk kuralı' };
const MAX_VALUE = 10 ** explainer.DEFAULT_CAPACITY - 1;

/** Kuralların uygulamadaki onaylı açıklamaları (öğretici 3–16 anlatımından). */
const RULE_TEXTS = {
  add: {
    direct: 'Kuralsız toplamada eklenecek boncuklar abaküste boşta duruyorsa onları doğrudan getiririz.',
    five: "5'lik toplamayı, eklenecek kadar birlik boncuk kalmadığında yaparız: 5 gelir, eklenecek sayının kardeşi gider. Kardeşler sayıyı 5'e tamamlar: 1 ile 4, 2 ile 3 kardeştir.",
    ten: "10'luk toplamayı, sütun 10'u geçince yaparız: 10 gelir, yani sol sütuna 1 boncuk gelir; bu sütundan eklenecek sayının büyük kardeşi gider. Büyük kardeşler sayıyı 10'a tamamlar: 1 ile 9, 2 ile 8, 3 ile 7, 4 ile 6, 5 ile 5.",
    bead: "Boncuk kuralını, 10'luk toplamada büyük kardeş doğrudan gidemediğinde kullanırız. 6, 7, 8 ya da 9 eklerken: sayının 5'ten fazlası kadar boncuk gelir, 5 gider, 10 gelir. Örneğin 6 için 1 boncuk gelir, 5 gider, 10 gelir.",
  },
  sub: {
    direct: 'Kuralsız çıkarmada çıkarılacak boncuklar abaküste duruyorsa onları doğrudan götürürüz.',
    five: "5'lik çıkarmayı, götürecek kadar birlik boncuk olmadığında yaparız: 5 gider, çıkarılacak sayının kardeşi gelir.",
    ten: "10'luk çıkarmayı, sütundaki sayı çıkarılacak sayıdan küçükse yaparız: 10 gider, yani sol sütundan 1 boncuk gider; bu sütuna çıkarılacak sayının kardeşi gelir. Burada kardeş, sayıyı 10'a tamamlayan sayıdır.",
    bead: "Boncuk çıkarmayı, 10'luk çıkarmada kardeş doğrudan gelemediğinde kullanırız: 10 gider, sonra kardeşi 5'lik toplamayla getiririz, yani 5 gelir ve kardeşin kardeşi gider.",
  },
};

// ── Anlatım ──────────────────────────────────────────────────────────────

function columnName(col) {
  return COLUMN_NAMES[col] || `${col + 1}. sütun`;
}

function stepPhrase(step, move) {
  const add = move.op === 'add';
  const amount = Math.abs(step.upper) * 5 + Math.abs(step.lower);
  switch (step.role) {
    case 'direct': return `${amount} ${step.upper + step.lower > 0 ? 'gelir' : 'gider'}`;
    case 'five': return step.upper > 0 ? '5 gelir' : '5 gider';
    case 'friend': return `kardeşi ${Math.abs(step.lower)} ${step.lower > 0 ? 'gelir' : 'gider'}`;
    case 'big_friend': return add ? `büyük kardeşi ${amount} gider` : `kardeşi ${amount} gelir`;
    case 'beads': return `${step.lower} boncuk gelir`;
    case 'ten': {
      const c = step.carry;
      const detail = c.rule === 'direct'
        ? RULE_NAMES.direct
        : `${RULE_NAMES[c.rule]}, ${c.steps.map((s) => stepPhrase(s, c)).join(', ')}`;
      return `${add ? '10 gelir' : '10 gider'} (${columnName(c.col)} basamağına 1 ${add ? 'eklenir' : 'çıkarılır'}: ${detail})`;
    }
    default: throw new Error(`rol: ${step.role}`);
  }
}

/** Modelin okuyacağı hamle özeti. */
function describe(e) {
  return {
    islem: `${e.base} ${e.op === 'add' ? '+' : '-'} ${e.number}`,
    sonuc: e.result,
    hamleler: e.moves.map((m) => ({
      basamak: columnName(m.col),
      rakam: m.x,
      kural: RULE_NAMES[m.rule],
      once: m.valueBefore,
      sonra: m.valueAfter,
      adimlar: m.steps.map((s) => stepPhrase(s, m)),
    })),
  };
}

// ── Araç tanımları ───────────────────────────────────────────────────────

const OP_PROPS = {
  baslangic: { type: 'integer', description: 'İşlemden önce abaküste yazan sayı (0–99999).' },
  islem: { type: 'string', enum: ['add', 'sub'], description: "'add' toplama, 'sub' çıkarma." },
  sayi: { type: 'integer', description: 'Eklenecek ya da çıkarılacak sayı (0–99999).' },
};
const OP_SCHEMA = {
  type: 'object',
  properties: OP_PROPS,
  required: ['baslangic', 'islem', 'sayi'],
  additionalProperties: false,
};

const TOOL_DEFS = [
  {
    name: 'hamleleri_hesapla',
    description: 'Abaküste bir toplama ya da çıkarmanın doğru hamlelerini, derste öğretilen sırayla ve kural adlarıyla verir. Bir hamleyi, kuralı ya da sonucu söylemeden önce mutlaka bunu çağır.',
    input_schema: OP_SCHEMA,
    strict: true,
  },
  {
    name: 'hatayi_bul',
    description: 'Çocuğun bu öğretici adımında abaküste yaptığı hareketlere bakar ve ilk nerede şaştığını tahmin eder. Girdi almaz; bağlamdaki adımı kullanır.',
    input_schema: { type: 'object', properties: {}, required: [], additionalProperties: false },
    strict: true,
  },
  {
    name: 'kurali_acikla',
    description: 'Bir kuralın uygulamadaki onaylı kısa açıklamasını verir. Çocuk bir kuralın ne olduğunu ya da neden gerektiğini sorduğunda kullan.',
    input_schema: {
      type: 'object',
      properties: {
        kural: { type: 'string', enum: ['direct', 'five', 'ten', 'bead'], description: "direct: kuralsız, five: 5'lik, ten: 10'luk, bead: boncuk kuralı." },
        islem: { type: 'string', enum: ['add', 'sub'] },
      },
      required: ['kural', 'islem'],
      additionalProperties: false,
    },
    strict: true,
  },
  {
    name: 'abakuste_goster',
    description: 'Hamleleri çocuğun ekranındaki abaküste yavaşça oynatır; abaküs önce baslangic değerine getirilir. Çocuk anlatımla anlamadıysa ya da "göster" derse kullan. Bir mesajda en fazla bir kez.',
    input_schema: OP_SCHEMA,
    strict: true,
  },
  {
    name: 'alistirma_ver',
    description: 'Çocuğun denemesi için abaküste küçük bir işlem hazırlar; çocuğun cevabını uygulama denetler, sonucu sonraki mesajın bağlamında görürsün. Çocuk anladığını söylediğinde pekiştirmek için kullan. Bir mesajda en fazla bir kez.',
    input_schema: OP_SCHEMA,
    strict: true,
  },
];

// ── Çalıştırma ───────────────────────────────────────────────────────────

const inRange = (n) => Number.isInteger(n) && n >= 0 && n <= MAX_VALUE;

function explainInput(input) {
  const { baslangic, islem, sayi } = input || {};
  if (!inRange(baslangic) || !inRange(sayi) || (islem !== 'add' && islem !== 'sub')) {
    return { error: `Sayılar 0 ile ${MAX_VALUE} arasında olmalı.` };
  }
  const e = explainer.explain(baslangic, islem, sayi);
  if (!e) return { error: `Bu işlemin sonucu abaküse sığmıyor (0 ile ${MAX_VALUE} arası).` };
  return { e };
}

/**
 * Çocuğun değer kaydında (abaküsün her değişimdeki değeri) doğru hamlelerin sonraki değerlerini
 * sırayla arar; bulunamayan ilk hamle büyük olasılıkla şaşılan yer. Ödevdeki RuleAnalysis.align
 * kadar ince değil (yalnızca ilk hatayı arıyor), sohbet için bu yeterli.
 */
function findMistake(ctx) {
  if (!ctx || !inRange(ctx.base) || !inRange(ctx.target)) {
    return { durum: 'Bu adımda denetlenecek bir abaküs işlemi yok.' };
  }
  const e = explainer.explainTransition(ctx.base, ctx.target);
  if (!e) return { durum: 'Bu adımda denetlenecek bir abaküs işlemi yok.' };
  const log = Array.isArray(ctx.valueLog) ? ctx.valueLog : [];
  const current = inRange(ctx.current) ? ctx.current : null;
  const base = { islem: describe(e).islem, beklenen: e.result, cocugun_su_anki_degeri: current };
  if (current === e.result) return { ...base, durum: 'Çocuğun abaküsünde doğru cevap yazıyor.' };
  if (log.length === 0 && (current === null || current === ctx.base)) {
    return { ...base, durum: 'Çocuk bu adımda abaküste henüz bir şey yapmamış.' };
  }
  let from = 0;
  for (let i = 0; i < e.moves.length; i++) {
    const m = e.moves[i];
    const at = log.indexOf(m.valueAfter, from);
    if (at === -1) {
      return {
        ...base,
        durum: 'Tahmin: bu hamlede şaşmış olabilir.',
        dogru_yapilan_hamle_sayisi: i,
        ilk_hata: describe({ ...e, moves: [m] }).hamleler[0],
      };
    }
    from = at + 1;
  }
  return { ...base, durum: 'Hamleleri doğru sırayla yapmış görünüyor ama son değer beklenen cevap değil; sonra bir boncuk oynamış olabilir.' };
}

/**
 * Tek aracı çalıştırır. Döner: { content: string (modelin okuyacağı JSON), isError, action? }.
 * action, uygulamaya giden ekran işi ("goster" / "alistirma"); [state] tur içindeki sayaçlar.
 */
function runTool(name, input, ctx, state) {
  const ok = (obj, action) => ({ content: JSON.stringify(obj), isError: false, action });
  const fail = (msg) => ({ content: JSON.stringify({ hata: msg }), isError: true });
  switch (name) {
    case 'hamleleri_hesapla': {
      const { e, error } = explainInput(input);
      return error ? fail(error) : ok(describe(e));
    }
    case 'hatayi_bul':
      return ok(findMistake(ctx));
    case 'kurali_acikla': {
      const text = RULE_TEXTS[input && input.islem] && RULE_TEXTS[input.islem][input.kural];
      return text ? ok({ kural: RULE_NAMES[input.kural], aciklama: text }) : fail('Böyle bir kural yok.');
    }
    case 'abakuste_goster':
    case 'alistirma_ver': {
      const kind = name === 'abakuste_goster' ? 'goster' : 'alistirma';
      if (state[kind]) return fail('Bu mesajda bu araç zaten kullanıldı.');
      const { e, error } = explainInput(input);
      if (error) return fail(error);
      state[kind] = true;
      const action = { tur: kind, baslangic: e.base, islem: e.op, sayi: e.number, sonuc: e.result, plan: explainer.encode(e) };
      return kind === 'goster'
        ? ok({ durum: 'Çocuğun abaküsünde oynatılıyor.', ...describe(e) }, action)
        : ok({ durum: 'Alıştırma çocuğun abaküsünde hazırlandı; cevabını uygulama denetleyecek.', islem: describe(e).islem, beklenen: e.result }, action);
    }
    default:
      return fail(`Bilinmeyen araç: ${name}`);
  }
}

module.exports = {
  TOOL_DEFS,
  RULE_NAMES,
  RULE_TEXTS,
  columnName,
  stepPhrase,
  describe,
  findMistake,
  runTool,
};

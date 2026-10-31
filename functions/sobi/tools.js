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

/**
 * Abaküsün sütun sütun dökümü: en büyük dolu sütundan birlere. Model "birler sütununda kaç boncuk
 * var / kaçı boşta" derken rakamı kendisi ayırıp saymasın: ilk denemede Haiku 8'i "8 boncuk" sandı,
 * ikincide "alt boncuklardan 2'si boş" dedi (1'i boş). Sayılar burada hazır, guard.checkReply
 * için de kaynakta geçiyor.
 */
function columnsOf(value) {
  const digits = String(value).split('').map(Number).reverse();
  return digits.map((d, col) => ({
    basamak: columnName(col),
    deger: d,
    beslik_gelmis: d >= 5,
    birlik_gelmis: d % 5,
    birlik_bosta: 4 - (d % 5),
  })).reverse();
}

/** Bağlam satırı: "birler 8: beşlik gelmiş, 3 birlik gelmiş, 1 birlik boşta". */
function columnsText(value) {
  return columnsOf(value).map((c) =>
    `${c.basamak} ${c.deger}: beşlik ${c.beslik_gelmis ? 'gelmiş' : 'boşta'}, ${c.birlik_gelmis} birlik gelmiş, ${c.birlik_bosta} birlik boşta`,
  ).join('; ');
}

/**
 * Hamlenin gerekçesi, o işlemin sayılarıyla: neden doğrudan olmuyor ve kural neden doğru sonucu
 * veriyor. İki denemede de iki model "neden 5 gidiyor?" sorusuna gerekçeyi kendisi uydurdu
 * (Haiku: "6'yı 1 ve 5'e ayırıyoruz"); burada derse uygun, hesabı tutan gerekçe hazır.
 * Sayılara ek getirilmiyor (bkz. dosya başı).
 */
function reasonOf(m) {
  const d = m.digitBefore;
  const x = m.x;
  const e = d % 5;
  const b = 10 - x;
  if (m.op === 'add') {
    switch (m.rule) {
      case 'direct':
        return { neden: 'Gereken boncuklar boşta olduğu için doğrudan gelir.' };
      case 'five':
        return {
          neden_dogrudan_olmuyor: `Birlik boncuklardan ${4 - e} tanesi boşta ama ${x} tane gerekiyor.`,
          neden: `${x} eklemek, 5 ekleyip ${5 - x} çıkarmakla aynı. Bu yüzden 5 gelir, kardeşi ${5 - x} gider.`,
        };
      case 'ten':
        return {
          neden_dogrudan_olmuyor: `${d} ile ${x} toplanınca ${d + x} oluyor; bir sütun en fazla 9 tutar.`,
          neden: `${x} eklemek, 10 ekleyip ${b} çıkarmakla aynı. Bu yüzden 10 gelir, büyük kardeşi ${b} gider.`,
        };
      case 'bead':
        return {
          neden_dogrudan_olmuyor: `${d} ile ${x} toplanınca ${d + x} oluyor; bir sütun en fazla 9 tutar. 10'luk kural da olmuyor: büyük kardeş ${b} gitmeli ama sütunda yalnızca ${e} birlik boncuk gelmiş.`,
          neden: `${x} eklemek, 10 ekleyip ${b} çıkarmakla aynı. ${b} çıkarmak da 5 çıkarıp ${x - 5} eklemekle aynı. Bu yüzden ${x - 5} boncuk gelir, 5 gider, 10 gelir.`,
        };
      default: break;
    }
  } else {
    switch (m.rule) {
      case 'direct':
        return { neden: 'Gereken boncuklar sütunda olduğu için doğrudan gider.' };
      case 'five':
        return {
          neden_dogrudan_olmuyor: `Sütunda ${e} birlik boncuk gelmiş ama ${x} tane gitmesi gerekiyor.`,
          neden: `${x} çıkarmak, 5 çıkarıp ${5 - x} eklemekle aynı. Bu yüzden 5 gider, kardeşi ${5 - x} gelir.`,
        };
      case 'ten':
        return {
          neden_dogrudan_olmuyor: `Sütunda ${d} var; ${x} çıkarmaya yetmiyor.`,
          neden: `${x} çıkarmak, 10 çıkarıp ${b} eklemekle aynı. Bu yüzden 10 gider, kardeşi ${b} gelir.`,
        };
      case 'bead':
        return {
          neden_dogrudan_olmuyor: `Sütunda ${d} var; ${x} çıkarmaya yetmiyor. 10'luk kural da olmuyor: kardeş ${b} gelmeli ama yalnızca ${4 - e} birlik boncuk boşta.`,
          neden: `${x} çıkarmak, 10 çıkarıp ${b} eklemekle aynı. ${b} eklemek de 5 ekleyip ${x - 5} çıkarmakla aynı. Bu yüzden 10 gider, 5 gelir, ${x - 5} boncuk gider.`,
        };
      default: break;
    }
  }
  throw new Error(`kural: ${m.rule}`);
}

/**
 * Kimin kardeşi: model kardeşi yanlış sayıya bağlıyordu (değerlendirme 29.10.2026: 17 + 3'te
 * "7'nin büyük kardeşi gider", 743 + 2'de "5'in kardeşi"). Çıkarmada 10'luk kuralda ders yalnızca
 * "kardeş" diyor.
 */
function kardesOf(m) {
  if (m.rule === 'five') return { sayi: m.x, kardesi: 5 - m.x, ad: 'kardeş' };
  if (m.rule === 'ten' || m.rule === 'bead') return { sayi: m.x, kardesi: 10 - m.x, ad: m.op === 'add' ? 'büyük kardeş' : 'kardeş' };
  return undefined;
}

/** Modelin okuyacağı hamle özeti. */
function describe(e) {
  return {
    islem: `${e.base} ${e.op === 'add' ? '+' : '-'} ${e.number}`,
    abakus_once: columnsOf(e.base),
    sonuc: e.result,
    hamleler: e.moves.map((m) => ({
      basamak: columnName(m.col),
      rakam: m.x,
      kural: RULE_NAMES[m.rule],
      once: m.valueBefore,
      sonra: m.valueAfter,
      adimlar: m.steps.map((s) => stepPhrase(s, m)),
      // Ara değerler: "23, 13 olur" gibi doğru bir cümle de guard'dan geçsin (denemede takılmıştı).
      adim_sonrasi_degerler: moveTrail(m),
      ...(kardesOf(m) ? { kardes: kardesOf(m) } : {}),
      ...reasonOf(m),
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
 * Bütün adımlar sırayla, her adımdan sonra abaküste yazacak değerle. Elde adımının değeri sol
 * sütundaki hareketin tamamı (zincir dahil).
 */
function stepTrail(e) {
  return e.moves.flatMap((m) => moveTrail(m).map((v, i) => ({ basamak: columnName(m.col), kural: RULE_NAMES[m.rule], adim: stepPhrase(m.steps[i], m), sonra: v })));
}

const cap = (s) => `${s[0].toUpperCase()}${s.slice(1)}`;

/**
 * Hamleyi söylemeden yön gösteren ipucu. Değerlendirmede (29.10.2026) `siradaki_adim` alanı
 * cevabı modelin önüne koyuyordu: araç çağrıldığında Sobi hamleyi daha sık hemen söyledi.
 * Kullanıcı kararı: takılan çocuğa ilk yardımda ipucu, yeniden isterse ya da cevabı açıkça
 * isterse hamle.
 */
function hintFor(basamak, kural) {
  return `${cap(basamak)} basamağına bak: bu adımda ${kural} kullanılıyor.`;
}
const WHEN_TO_TELL = 'Hamleyi çocuk cevabı açıkça isterse ya da bu adımda yeniden yardım isterse söyle; ilk yardımda ipucuyla yönlendir.';

/** Hamlenin her adımından sonra abaküste yazan değer. */
function moveTrail(m) {
  let v = m.valueBefore;
  return m.steps.map((s) => {
    v += s.carry ? s.carry.valueAfter - s.carry.valueBefore : (s.upper * 5 + s.lower) * 10 ** s.col;
    return v;
  });
}

/**
 * Çocuk nerede: önce şu anki değer doğru yoldaki bir adımın sonrasıyla eşleşiyor mu (o zaman hangi
 * adımları yaptığı ve sıradaki adım kesin), değilse değer kaydında (abaküsün her değişimdeki
 * değeri) doğru hamlelerin sonraki değerlerini sırayla arar; bulunamayan ilk hamle büyük olasılıkla
 * şaşılan yer. Ödevdeki RuleAnalysis.align kadar ince değil (yalnızca ilk hatayı arıyor), sohbet
 * için bu yeterli.
 *
 * Adım eşleşmesi neden var: denemede 8 + 6'da ilk adımı ("1 boncuk gelir") doğru yapıp 9'a gelen
 * çocuğa Haiku "8'e geri dön" dedi; doğru yoldaki çocuk geri çevrilmemeli.
 */
function findMistake(ctx) {
  if (!ctx || !inRange(ctx.base) || !inRange(ctx.target)) {
    return { durum: 'Bu adımda denetlenecek bir abaküs işlemi yok.' };
  }
  const e = explainer.explainTransition(ctx.base, ctx.target);
  if (!e) return { durum: 'Bu adımda denetlenecek bir abaküs işlemi yok.' };
  const log = Array.isArray(ctx.valueLog) ? ctx.valueLog : [];
  const current = inRange(ctx.current) ? ctx.current : null;
  const base = {
    islem: describe(e).islem,
    beklenen: e.result,
    cocugun_su_anki_degeri: current,
    ...(current !== null ? { cocugun_abakusu: columnsOf(current) } : {}),
  };
  if (current === e.result) return { ...base, durum: 'Çocuğun abaküsünde doğru cevap yazıyor.' };
  // Kayıtta yalnızca başlangıç değeri varsa çocuk dokunmamış demektir; "şaştın" denmemeli.
  const untouched = log.every((v) => v === ctx.base);
  if ((current === null || current === ctx.base) && untouched) {
    const first = stepTrail(e)[0];
    return {
      ...base,
      durum: 'Çocuk bu adımda abaküste henüz bir şey yapmamış.',
      ipucu: hintFor(first.basamak, first.kural),
      siradaki_adim: `${first.basamak}: ${first.adim}`,
      siradaki_adim_ne_zaman: WHEN_TO_TELL,
    };
  }
  if (current !== null && current !== ctx.base) {
    const trail = stepTrail(e);
    const j = trail.findIndex((t) => t.sonra === current);
    if (j !== -1 && j < trail.length - 1) {
      return {
        ...base,
        durum: 'Çocuk doğru yolda; yaptığı adımlar doğru, geri dönmesine gerek yok.',
        yapilan_adimlar: trail.slice(0, j + 1).map((t) => `${t.basamak}: ${t.adim}`),
        ipucu: `Doğru yoldasın; ${hintFor(trail[j + 1].basamak, trail[j + 1].kural).toLocaleLowerCase('tr-TR')}`,
        siradaki_adim: `${trail[j + 1].basamak}: ${trail[j + 1].adim}`,
        siradaki_adim_ne_zaman: WHEN_TO_TELL,
      };
    }
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
        ipucu: hintFor(columnName(m.col), RULE_NAMES[m.rule]),
        ilk_hata: describe({ ...e, moves: [m] }).hamleler[0],
        siradaki_adim_ne_zaman: WHEN_TO_TELL,
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
      // Araç listede yokken çağrılırsa (ör. eski bir geçmişten) uygulama bunu yapamaz.
      if (kind === 'alistirma' && !(ctx && ctx.practice)) return fail('Bu ekranda alıştırma hazırlanamıyor.');
      // Anlatım adımında öğreticinin abaküsü dersin kendi animasyonlarında; gösterim dersi bozar.
      if (kind === 'goster' && !(ctx && Number.isInteger(ctx.base) && Number.isInteger(ctx.target))) {
        return fail('Bu anlatım adımında abaküste gösterim yapılamıyor; anlatarak açıkla.');
      }
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
  columnsOf,
  columnsText,
  reasonOf,
  stepPhrase,
  stepTrail,
  describe,
  findMistake,
  runTool,
};

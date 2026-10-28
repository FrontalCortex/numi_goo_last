'use strict';

/**
 * SOBİ SOHBET TURU — çocuğun bir mesajını alır, modelle araç döngüsünü yürütür, cevabı denetler.
 *
 * Firestore'a ve Firebase'e bağlı değil: istemci (Anthropic SDK) dışarıdan veriliyor. Böylece
 * scripts/test-sobi.js sahte bir istemciyle her yolu (araç, ret, uzun cevap, kaynaksız sayı)
 * API'ye para ödemeden sınayabiliyor. Firestore işi callable.js'te.
 *
 * NEDEN ELLE YAZILMIŞ DÖNGÜ (SDK'nın tool runner'ı yerine)
 *   - Tur başına araç çağrısı sınırı var (MAX_TOOL_ROUNDS): model kendini döngüye sokarsa çocuk
 *     beklemesin, fatura büyümesin.
 *   - Son cevap çocuğa gitmeden guard.checkReply'den geçiyor; kaynak sayılar araç sonuçlarından.
 *   - Ekran işleri (abakuste_goster, alistirma_ver) toplanıp uygulamaya dönüyor.
 *
 * GEÇMİŞ YALNIZCA EKLENEREK BÜYÜR
 *   Modelin her cevabı (düşünme blokları dahil) geçmişe olduğu gibi ekleniyor, hiçbir eski mesaj
 *   düzenlenmiyor. Model, önceki turlardaki düşünme bloklarının değişmediğini denetliyor;
 *   düzenlenmiş geçmiş reddedilir. Denetime takılan bir cevap da bu yüzden geçmişte KALIYOR;
 *   çocuğun yerine ne gördüğü bir sonraki turda <not> ile modele söyleniyor.
 *   Ret (refusal) ve yarıda kesilen (max_tokens) cevaplar geçmişe hiç eklenmiyor.
 */

const tools = require('./tools');
const guard = require('./guard');

/**
 * Model: claude-haiku-5-5 (kullanıcı kararı 28.10.2026). Hesabı kural motoru yaptığı için model
 * yalnızca araç çağırıp anlatıyor; Opus 5.5'in yaklaşık kırkta biri maliyetle. Opus ile
 * karşılaştırma: `npm run sobi:dene -- --model claude-opus-5-5`.
 */
const DEFAULT_MODEL = 'claude-haiku-5-5';
/**
 * 'medium' (Haiku 5.5'in kendi varsayılanı). 28.10.2026 denemesinde 'low' uzun ve dağınık yazdı,
 * çocuğun adını geri söyledi, işlemin sonucunu kendisi hesaplayıp denetime takıldı; 'medium' kısa,
 * doğru ve kurallara uygun kaldı. Maliyet farkı mesaj başına bir sentin onda birinden az.
 */
const DEFAULT_EFFORT = 'medium';
const MAX_TOKENS = 8000;
const MAX_TOOL_ROUNDS = 4;
/**
 * Ret olursa sunucu isteği kategoriye göre önerilen modelle yeniden çalıştırır. Haiku 5.5'te bu
 * yok; orada ret, çocuğa yedek metin olarak döner (runTurn).
 */
const FALLBACK_BETA = 'server-side-fallback-2026-07-01';
const FALLBACK_MODELS = new Set(['claude-opus-5-5', 'claude-opus-5', 'claude-fable-5-1']);

const STEP_ID = /^[a-z0-9_]{1,60}$/;
const MAX_VALUE = 99999;
const MAX_LOG = 200;

const MAX_RECENT_STEPS = 6;

/** Kural anahtarı → çocuğun bildiği ad (homework.js ve RuleAnalysis ile aynı anahtarlar). */
const RULE_LABELS = {
  add_direct: 'kuralsız toplama',
  add_five: "5'lik toplama",
  add_ten: "10'luk toplama",
  add_bead: 'boncuk kuralıyla toplama',
  sub_direct: 'kuralsız çıkarma',
  sub_five: "5'lik çıkarma",
  sub_ten: "10'luk çıkarma",
  sub_bead: 'boncuk kuralıyla çıkarma',
};

const intIn = (v, lo, hi) => (Number.isInteger(v) && v >= lo && v <= hi ? v : null);
const text = (v, max) => (typeof v === 'string' ? v.replace(/\s+/g, ' ').trim().slice(0, max) : '');

/** Uygulamanın gönderdiği adım bağlamı; bilinmeyen alanlar atılır, aralık dışı sayılar null. */
function sanitizeContext(raw) {
  const r = raw && typeof raw === 'object' ? raw : {};
  const log = Array.isArray(r.valueLog)
    ? r.valueLog.slice(-MAX_LOG).map((v) => intIn(v, 0, MAX_VALUE)).filter((v) => v !== null)
    : [];
  return {
    stepId: typeof r.stepId === 'string' && STEP_ID.test(r.stepId) ? r.stepId : null,
    stepText: text(r.stepText, 400),
    questionText: text(r.questionText, 200),
    base: intIn(r.base, 0, MAX_VALUE),
    target: intIn(r.target, 0, MAX_VALUE),
    current: intIn(r.current, 0, MAX_VALUE),
    attempts: intIn(r.attempts, 0, 99),
    valueLog: log,
    // Uygulama alıştırma hazırlayabiliyor mu. Öğretici bunu yapamıyor: "Kontrol Et" yalnızca
    // adımın kendi cevabını denetliyor, Sobi başka bir işlem hazırlarsa çocuk karışır.
    practice: r.practice === true,
    // Anlatım adımlarında da soru sorulabilsin diye: dersin adı, son adımların metni (eskiden
    // yeniye) ve çocuğun o ana kadar öğrendiği kurallar. Sobi açıklamayı dersin kendi sözlerine
    // dayandırsın, henüz öğretilmemiş bir kuralı anlatıp kafa karıştırmasın.
    lessonTitle: text(r.lessonTitle, 80),
    recentSteps: Array.isArray(r.recentSteps)
      ? r.recentSteps.map((s) => text(s, 300)).filter(Boolean).slice(-MAX_RECENT_STEPS)
      : [],
    taughtRules: Array.isArray(r.taughtRules)
      ? [...new Set(r.taughtRules.filter((k) => typeof k === 'string' && RULE_LABELS[k]))]
      : [],
  };
}

/** Bu bağlamda modele verilecek araçlar. */
function toolsFor() {
  // Liste sohbet boyunca HİÇ değişmemeli: model önceki turlardaki düşünme bloklarının aynı araç
  // listesiyle yazıldığını doğruluyor, liste değişince isteği reddediyor ("The `tools` list
  // differs from the one this block was created with"; 28.10.2026'da anlatım adımında
  // abakuste_goster'i çıkarınca oldu). Bu adımda yapılamayan araçları runTool reddediyor.
  return tools.TOOL_DEFS;
}

function contextBlock(ctx) {
  const lines = [];
  if (ctx.lessonTitle) lines.push(`Ders: ${ctx.lessonTitle}`);
  if (ctx.taughtRules && ctx.taughtRules.length) {
    lines.push(`Çocuğun şimdiye kadar öğrendiği kurallar: ${ctx.taughtRules.map((k) => RULE_LABELS[k]).join(', ')}`);
  }
  if (ctx.recentSteps && ctx.recentSteps.length) {
    lines.push('Bu derste az önce anlatılanlar (eskiden yeniye):');
    for (const s of ctx.recentSteps) lines.push(`- ${s}`);
  }
  if (ctx.target === null) lines.push('Bu bir anlatım adımı: çocuk şu an bir soru cevaplamıyor, dersi dinliyor.');
  if (ctx.stepText) lines.push(`Adımın metni: ${ctx.stepText}`);
  if (ctx.questionText) lines.push(`Soru: ${ctx.questionText}`);
  if (ctx.base !== null) lines.push(`Adımın başında abaküste yazan: ${ctx.base}`);
  // Sütunlarıyla: "yüzler sütununa 6 yaz" gibi doğru bir cümle de guard'dan geçsin (600'ün 6'sı).
  if (ctx.target !== null) lines.push(`Beklenen cevap: ${ctx.target}; sütunlar: ${tools.columnsText(ctx.target)}`);
  if (ctx.current !== null) lines.push(`Şu an abaküste yazan: ${ctx.current}; sütunlar: ${tools.columnsText(ctx.current)}`);
  if (ctx.attempts !== null) lines.push(`Bu adımdaki deneme sayısı: ${ctx.attempts}`);
  return `<baglam>\n${lines.join('\n')}\n</baglam>`;
}

/** Cevabın altındaki öneri balonları. Sabit liste; uygulama metinleri kendi dilinde gösterir. */
function suggestionsFor(ctx, actions) {
  // Anlatım adımı: gösterim yok, örnek istemek anlamlı.
  if (ctx.target === null) return ['neden_boyle', 'ornek_ver', 'anladim'];
  const out = [];
  if (ctx.base !== null && ctx.target !== null && !actions.some((a) => a.tur === 'goster')) out.push('bana_goster');
  out.push('neden_boyle');
  if (actions.some((a) => a.tur === 'goster')) out.push('bir_daha_goster');
  else out.push('anladim');
  return out.slice(0, 3);
}

function addUsage(total, u) {
  if (!u) return;
  for (const k of ['input_tokens', 'output_tokens', 'cache_read_input_tokens', 'cache_creation_input_tokens']) {
    total[k] = (total[k] || 0) + (Number(u[k]) || 0);
  }
}

/**
 * Tek tur.
 * @param {object} p
 * @param {object} p.client     Anthropic istemcisi (messages.create; yedek modelli modellerde beta.messages.create)
 * @param {string} p.system     Talimat metni (prompt_tr.md)
 * @param {Array}  p.history    Önceki turların mesajları (düşünme blokları dahil, değiştirilmemiş)
 * @param {object} p.context    sanitizeContext çıktısı
 * @param {string} p.childText  Çocuğun mesajı (henüz temizlenmemiş)
 * @param {string} [p.note]     Önceki turda çocuğa gösterilen yedek metin (denetime takıldıysa)
 * @returns {Promise<{reply, actions, suggestions, history, usage, fellBack, reasons, stopReason, masked}>}
 */
async function runTurn({ client, system, history, context, childText, note, model = DEFAULT_MODEL, effort = DEFAULT_EFFORT }) {
  const messages = [...(history || [])];
  const { text: cleaned, masked } = guard.cleanChildText(childText);
  const content = [{ type: 'text', text: contextBlock(context) }];
  if (note) content.push({ type: 'text', text: `<not>Önceki cevabın denetimden geçmedi; çocuk onun yerine şunu gördü: "${note}"</not>` });
  content.push({ type: 'text', text: `<cocuk>${cleaned || '(boş mesaj)'}</cocuk>` });
  messages.push({ role: 'user', content });

  const usage = {};
  const actions = [];
  const toolState = {};
  let reply = null;
  let stopReason = null;
  const reasons = [];

  const params = {
    model,
    max_tokens: MAX_TOKENS,
    system: [{ type: 'text', text: system }],
    tools: toolsFor(context),
    messages,
    output_config: { effort },
    cache_control: { type: 'ephemeral' },
  };
  const send = FALLBACK_MODELS.has(model)
    ? () => client.beta.messages.create({ ...params, betas: [FALLBACK_BETA], fallbacks: 'default' })
    : () => client.messages.create(params);

  for (let round = 0; ; round++) {
    const response = await send();
    addUsage(usage, response.usage);
    stopReason = response.stop_reason;

    // Ret ya da yarıda kesilmiş cevap: geçmişe eklenmez, çocuk yedek metni görür.
    if (stopReason === 'refusal' || stopReason === 'max_tokens') {
      reasons.push(stopReason);
      break;
    }
    messages.push({ role: 'assistant', content: response.content });

    if (stopReason === 'tool_use') {
      if (round >= MAX_TOOL_ROUNDS) {
        // Araç isteği cevapsız kalamaz (sonraki istek reddedilir): sonuçları "sınır" hatasıyla dön.
        messages.push({ role: 'user', content: toolUseBlocks(response).map((b) => ({
          type: 'tool_result', tool_use_id: b.id, is_error: true, content: 'Bu mesajda araç sınırına gelindi.',
        })) });
        reasons.push('arac_siniri');
        break;
      }
      const results = toolUseBlocks(response).map((b) => {
        const r = tools.runTool(b.name, b.input, context, toolState);
        if (r.action) actions.push(r.action);
        return { type: 'tool_result', tool_use_id: b.id, content: r.content, ...(r.isError ? { is_error: true } : {}) };
      });
      messages.push({ role: 'user', content: results });
      continue;
    }
    if (stopReason === 'pause_turn') continue;

    reply = response.content.filter((b) => b.type === 'text').map((b) => b.text).join('').trim();
    break;
  }

  let fellBack = reply === null;
  if (reply !== null) {
    const check = guard.checkReply(reply, guard.sourceNumbers(messages), { explanation: context.target === null });
    if (!check.ok) { reasons.push(...check.reasons); fellBack = true; }
  }
  const shown = fellBack ? guard.fallbackReply(context, tools) : reply;
  return {
    reply: shown,
    modelReply: reply,
    actions,
    suggestions: suggestionsFor(context, actions),
    history: messages,
    usage,
    fellBack,
    reasons,
    stopReason,
    masked,
  };
}

function toolUseBlocks(response) {
  return response.content.filter((b) => b.type === 'tool_use');
}

module.exports = {
  DEFAULT_MODEL,
  DEFAULT_EFFORT,
  MAX_TOOL_ROUNDS,
  FALLBACK_BETA,
  FALLBACK_MODELS,
  RULE_LABELS,
  sanitizeContext,
  toolsFor,
  contextBlock,
  suggestionsFor,
  runTurn,
};

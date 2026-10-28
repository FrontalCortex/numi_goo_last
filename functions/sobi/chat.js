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
 * Model: claude-opus-5-5. Maliyet kararı kullanıcıda (docs/ANLAMADIM_YARDIM_PLANI.md, Faz 3):
 * test setinde daha küçük bir modelle (ör. claude-haiku-5-5) karşılaştırılıp seçilecek.
 */
const DEFAULT_MODEL = 'claude-opus-5-5';
/** Sohbet: kısa cevaplar, derin düşünme gerekmiyor. Opus 5.5'te varsayılan 'medium'. */
const DEFAULT_EFFORT = 'low';
const MAX_TOKENS = 8000;
const MAX_TOOL_ROUNDS = 4;
/** Ret olursa sunucu isteği kategoriye göre önerilen modelle yeniden çalıştırır. */
const FALLBACK_BETA = 'server-side-fallback-2026-07-01';

const STEP_ID = /^[a-z0-9_]{1,60}$/;
const MAX_VALUE = 99999;
const MAX_LOG = 200;

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
  };
}

function contextBlock(ctx) {
  const lines = [];
  if (ctx.stepText) lines.push(`Adımın metni: ${ctx.stepText}`);
  if (ctx.questionText) lines.push(`Soru: ${ctx.questionText}`);
  if (ctx.base !== null) lines.push(`Adımın başında abaküste yazan: ${ctx.base}`);
  if (ctx.target !== null) lines.push(`Beklenen cevap: ${ctx.target}`);
  if (ctx.current !== null) lines.push(`Şu an abaküste yazan: ${ctx.current}`);
  if (ctx.attempts !== null) lines.push(`Bu adımdaki deneme sayısı: ${ctx.attempts}`);
  return `<baglam>\n${lines.join('\n')}\n</baglam>`;
}

/** Cevabın altındaki öneri balonları. Sabit liste; uygulama metinleri kendi dilinde gösterir. */
function suggestionsFor(ctx, actions) {
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
 * @param {object} p.client     Anthropic istemcisi (client.beta.messages.create)
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

  for (let round = 0; ; round++) {
    const response = await client.beta.messages.create({
      model,
      max_tokens: MAX_TOKENS,
      system: [{ type: 'text', text: system }],
      tools: tools.TOOL_DEFS,
      messages,
      output_config: { effort },
      cache_control: { type: 'ephemeral' },
      betas: [FALLBACK_BETA],
      fallbacks: 'default',
    });
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
    const check = guard.checkReply(reply, guard.sourceNumbers(messages));
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
  sanitizeContext,
  contextBlock,
  suggestionsFor,
  runTurn,
};

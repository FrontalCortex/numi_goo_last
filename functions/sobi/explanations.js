'use strict';

/**
 * HAZIR AÇIKLAMALAR — anlatım adımlarında Sobi'nin dayandığı, önceden yazılıp kullanıcının
 * onayladığı metinler (tools/sobi-eval/HAZIR_ACIKLAMALAR_PLANI.md).
 *
 * NEDEN
 *   Değerlendirmede Haiku'nun hataları hep kendi kurduğu açıklamalardan geldi (uydurma gerekçe,
 *   kardeş karışıklığı). Onaylı metin varsa model onu kullanıyor; öneri balonlarında ("Bunu
 *   anlamadım", "Neden böyle?") model hiç çağrılmadan metin dönüyor.
 *
 * VERİ: aciklamalar_tr.json — tools/sobi-eval/aciklamalar/yayinla.mjs üretiyor, elle yazılmaz.
 *   { "3": [{ key, steps: [[stepIndex, metinParmakİzi], ...], konu, basit, neden|null }] }
 *   Parmak izi adımın metninden: uygulama aynı sırada başka bir metin gönderirse (ders değişmiş,
 *   sıra kaymış) açıklama kullanılmaz ve Sobi bugünkü gibi ders metnine dayanır. Bozulma yok.
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, 'aciklamalar_tr.json');

/** Noktalama, boşluk ve büyük/küçük harf farkı sayılmasın (strings.xml kaçışları, ’ ile '). */
function textPrint(text) {
  const norm = String(text || '').toLocaleLowerCase('tr-TR').replace(/[^\p{L}\p{N}]+/gu, '');
  return crypto.createHash('sha1').update(norm).digest('hex').slice(0, 8);
}

let cache = null;
function load() {
  if (cache) return cache;
  try {
    cache = JSON.parse(fs.readFileSync(FILE, 'utf8'));
  } catch (_) {
    cache = {};
  }
  return cache;
}

/**
 * Bu anlatım adımının onaylı açıklaması ya da null. Soru adımında hep null: orada hamleyi kural
 * motoru veriyor.
 */
function find(ctx) {
  if (!ctx || ctx.target !== null || !Number.isInteger(ctx.tutorialNumber) || !Number.isInteger(ctx.stepIndex)) return null;
  const parts = load()[String(ctx.tutorialNumber)];
  if (!Array.isArray(parts)) return null;
  const print = textPrint(ctx.stepText);
  // Önce aynı sıra ve aynı metin. Tutmazsa (öğreticiye adım eklenip sıra kaydıysa) aynı metinli en
  // yakın adım: "5 gelir." gibi tekrarlanan metinlerde de doğru parçayı bulsun diye en yakını.
  let best = null;
  let bestDist = Infinity;
  for (const p of parts) {
    for (const [i, h] of p.steps) {
      if (h !== print) continue;
      const d = Math.abs(i - ctx.stepIndex);
      if (d < bestDist) { best = p; bestDist = d; }
    }
  }
  return bestDist <= 10 ? best : null;
}

module.exports = { find, textPrint, FILE };

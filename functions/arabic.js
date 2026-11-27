/**
 * Arapça sunucu metinlerinin ortak parçaları (docs/CEVIRI_SOZLUGU_AR.md).
 */

/**
 * Sayılan adın sayıya göre biçimi: Arapçada 1 tekil, 2 ikil, 3–10 çoğul, 11–99 tekil (mansub),
 * 100 ve sonrası tekil (CLDR: one, two, few, many, other). Kalıplarda `%d` sayının yeri; rakamlar
 * Batı rakamı (sözlük kararı 1).
 *
 *   arCount(3, { one: 'ساعة واحدة', two: 'ساعتين', few: '%d ساعات', many: '%d ساعة', other: '%d ساعة' })
 */
function arCount(n, forms) {
  const r = n % 100;
  const form = n === 1 ? forms.one
    : n === 2 ? forms.two
      : r >= 3 && r <= 10 ? forms.few
        : r >= 11 && r <= 99 ? forms.many
          : forms.other;
  return form.replace('%d', String(n));
}

/**
 * Sağdan sola metnin içindeki denklem ve kesir ("2 × 1", "7/10") soldan sola kalsın diye yalıtılır
 * (U+2066 … U+2069); yoksa bildirimde "1 × 2" gibi ters görünebilir.
 */
const ltr = (s) => `⁦${s}⁩`;

module.exports = { arCount, ltr };

// Bağımlılıksız, küçük bir XML ayrıştırıcı. Android kaynak dosyaları (vector, shape,
// layer-list, colors.xml, layout) için yeterli: öznitelikler, iç içe öğeler, yorumlar,
// metin içeriği. DTD ve işleme yönergeleri atlanır.
'use strict';

const VARLIKLAR = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'" };

function varlikCoz(metin) {
  return metin.replace(/&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);/g, (tum, ad) => {
    if (ad[0] === '#') {
      const kod = ad[1] === 'x' ? parseInt(ad.slice(2), 16) : parseInt(ad.slice(1), 10);
      return String.fromCodePoint(kod);
    }
    return Object.prototype.hasOwnProperty.call(VARLIKLAR, ad) ? VARLIKLAR[ad] : tum;
  });
}

// Döndürülen düğüm: { ad, oz: {öznitelik: değer}, cocuklar: [düğüm], metin }
function xmlAyristir(kaynak) {
  let i = 0;
  const n = kaynak.length;
  if (kaynak.charCodeAt(0) === 0xfeff) i = 1;
  const kok = { ad: '#kok', oz: {}, cocuklar: [], metin: '' };
  const yigin = [kok];

  while (i < n) {
    const lt = kaynak.indexOf('<', i);
    if (lt < 0) {
      yigin[yigin.length - 1].metin += varlikCoz(kaynak.slice(i));
      break;
    }
    if (lt > i) yigin[yigin.length - 1].metin += varlikCoz(kaynak.slice(i, lt));
    if (kaynak.startsWith('<!--', lt)) {
      const son = kaynak.indexOf('-->', lt + 4);
      if (son < 0) throw new Error('kapanmamış yorum');
      i = son + 3;
      continue;
    }
    if (kaynak.startsWith('<![CDATA[', lt)) {
      const son = kaynak.indexOf(']]>', lt);
      if (son < 0) throw new Error('kapanmamış CDATA');
      yigin[yigin.length - 1].metin += kaynak.slice(lt + 9, son);
      i = son + 3;
      continue;
    }
    if (kaynak.startsWith('<?', lt)) {
      const son = kaynak.indexOf('?>', lt);
      if (son < 0) throw new Error('kapanmamış işleme yönergesi');
      i = son + 2;
      continue;
    }
    if (kaynak.startsWith('<!', lt)) {
      const son = kaynak.indexOf('>', lt);
      i = son + 1;
      continue;
    }
    if (kaynak[lt + 1] === '/') {
      const son = kaynak.indexOf('>', lt);
      const ad = kaynak.slice(lt + 2, son).trim();
      const ust = yigin.pop();
      if (!ust || ust.ad !== ad) throw new Error(`eşleşmeyen kapanış: </${ad}>`);
      i = son + 1;
      continue;
    }
    // Açılış etiketi
    let j = lt + 1;
    while (j < n && !/[\s/>]/.test(kaynak[j])) j++;
    const dugum = { ad: kaynak.slice(lt + 1, j), oz: {}, cocuklar: [], metin: '' };
    let kendiKapanir = false;
    for (;;) {
      while (j < n && /\s/.test(kaynak[j])) j++;
      if (j >= n) throw new Error('kapanmamış etiket: ' + dugum.ad);
      if (kaynak[j] === '>') { j++; break; }
      if (kaynak[j] === '/' && kaynak[j + 1] === '>') { kendiKapanir = true; j += 2; break; }
      let k = j;
      while (k < n && !/[\s=/>]/.test(kaynak[k])) k++;
      const ozAd = kaynak.slice(j, k);
      while (k < n && /\s/.test(kaynak[k])) k++;
      if (kaynak[k] !== '=') throw new Error(`değersiz öznitelik: ${ozAd} (${dugum.ad})`);
      k++;
      while (k < n && /\s/.test(kaynak[k])) k++;
      const tirnak = kaynak[k];
      if (tirnak !== '"' && tirnak !== "'") throw new Error(`tırnaksız öznitelik: ${ozAd}`);
      const son = kaynak.indexOf(tirnak, k + 1);
      if (son < 0) throw new Error(`kapanmamış öznitelik: ${ozAd}`);
      dugum.oz[ozAd] = varlikCoz(kaynak.slice(k + 1, son));
      j = son + 1;
    }
    yigin[yigin.length - 1].cocuklar.push(dugum);
    if (!kendiKapanir) yigin.push(dugum);
    i = j;
  }
  if (yigin.length !== 1) throw new Error('kapanmamış öğe: ' + yigin[yigin.length - 1].ad);
  const kokOge = kok.cocuklar[0];
  if (!kokOge) throw new Error('kök öğe yok');
  return kokOge;
}

function xmlKacis(metin) {
  return String(metin).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

module.exports = { xmlAyristir, xmlKacis };

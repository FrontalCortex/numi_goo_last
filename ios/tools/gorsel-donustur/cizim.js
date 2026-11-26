// Android çizim kaynaklarını (vector, shape, layer-list) SVG'ye çeviren çekirdek.
// Dosya sistemi bilmez: kaynakları `ortam` üzerinden ister.
//
// ortam = {
//   renkCoz(ad)      -> '#AARRGGBB' | null     (@color/ad)
//   olcuCoz(ad)      -> sayı | null            (@dimen/ad, dp cinsinden)
//   cizimKoku(ad)    -> ayrıştırılmış XML kökü | null   (@drawable/ad; bitmap ise null)
// }
'use strict';

const { xmlKacis } = require('./xml');

class Desteklenmiyor extends Error {}

// Android'in kendi renkleri (@android:color/...). Yalnızca projede geçenler ve yaygın olanlar.
const ANDROID_RENKLERI = {
  white: '#FFFFFFFF',
  black: '#FF000000',
  transparent: '#00000000',
  darker_gray: '#FFAAAAAA',
  background_dark: '#FF000000',
  background_light: '#FFFFFFFF',
  holo_blue_light: '#FF33B5E5',
  holo_blue_dark: '#FF0099CC',
  holo_blue_bright: '#FF00DDFF',
  holo_green_light: '#FF99CC00',
  holo_green_dark: '#FF669900',
  holo_red_light: '#FFFF4444',
  holo_red_dark: '#FFCC0000',
  holo_orange_light: '#FFFFBB33',
  holo_orange_dark: '#FFFF8800',
  holo_purple: '#FFAA66CC',
};

// '#RGB' | '#ARGB' | '#RRGGBB' | '#AARRGGBB' -> { r, g, b, a } (a: 0..1)
function onaltilikRenk(metin) {
  const m = /^#([0-9a-fA-F]+)$/.exec(metin.trim());
  if (!m) return null;
  let h = m[1];
  if (h.length === 3 || h.length === 4) h = h.split('').map(c => c + c).join('');
  if (h.length === 6) h = 'FF' + h;
  if (h.length !== 8) return null;
  return {
    a: parseInt(h.slice(0, 2), 16) / 255,
    r: parseInt(h.slice(2, 4), 16),
    g: parseInt(h.slice(4, 6), 16),
    b: parseInt(h.slice(6, 8), 16),
  };
}

function renkCoz(deger, cx) {
  if (deger == null) return null;
  const d = deger.trim();
  if (d[0] === '#') {
    const r = onaltilikRenk(d);
    if (!r) cx.uyari(`renk okunamadı: ${d}`);
    return r;
  }
  let m = /^@android:color\/(\w+)$/.exec(d);
  if (m) {
    const h = ANDROID_RENKLERI[m[1]];
    if (!h) { cx.uyari(`bilinmeyen Android rengi: ${d}`); return null; }
    return onaltilikRenk(h);
  }
  m = /^@color\/(\w+)$/.exec(d);
  if (m) {
    const h = cx.ortam.renkCoz(m[1]);
    if (!h) { cx.uyari(`renk bulunamadı: ${d}`); return null; }
    return onaltilikRenk(h);
  }
  cx.uyari(`çözülemeyen renk başvurusu: ${d}`);
  return null;
}

function sayi(n) {
  // Gereksiz ondalıkları at; SVG'de kısa ve kararlı çıktı.
  const y = Math.round(n * 1e6) / 1e6;
  return String(Object.is(y, -0) ? 0 : y);
}

function olcu(deger, cx, varsayilan) {
  if (deger == null) return varsayilan;
  const d = deger.trim();
  let m = /^(-?[\d.]+)(dp|dip|px|sp)?$/.exec(d);
  if (m) return parseFloat(m[1]);
  m = /^@dimen\/(\w+)$/.exec(d);
  if (m) {
    const v = cx.ortam.olcuCoz(m[1]);
    if (v != null) return v;
  }
  cx.uyari(`ölçü çözülemedi: ${d}`);
  return varsayilan;
}

function onaltilik(r) {
  return '#' + [r.r, r.g, r.b].map(v => v.toString(16).padStart(2, '0')).join('');
}

// ---------------------------------------------------------------------------
// pathData: Android ayrıştırıcısı SVG'den gevşektir (bitişik yay bayrakları, "1.5.5" gibi
// bitişik sayılar). Xcode'un SVG okuyucusu için komut ve sayıları açık boşlukla yeniden yazarız.
// ---------------------------------------------------------------------------
const KOMUT_ARG = { M: 2, L: 2, H: 1, V: 1, C: 6, S: 4, Q: 4, T: 2, A: 7, Z: 0 };

function yolNormallestir(d) {
  const cikti = [];
  let i = 0;
  const n = d.length;
  let komut = null;
  let argNo = 0;
  let komutSayisi = 0;
  const sayiRe = /[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?/y;
  while (i < n) {
    const c = d[i];
    if (c === ' ' || c === ',' || c === '\n' || c === '\t' || c === '\r') { i++; continue; }
    if (/[a-zA-Z]/.test(c)) {
      const ust = c.toUpperCase();
      if (!(ust in KOMUT_ARG)) throw new Error(`bilinmeyen yol komutu: ${c}`);
      if (komut && KOMUT_ARG[komut.toUpperCase()] > 0 && argNo % KOMUT_ARG[komut.toUpperCase()] !== 0) {
        throw new Error(`eksik argüman: ${komut}`);
      }
      komut = c;
      argNo = 0;
      komutSayisi++;
      cikti.push(c);
      i++;
      continue;
    }
    if (!komut) throw new Error('yol komutla başlamıyor');
    const adet = KOMUT_ARG[komut.toUpperCase()];
    if (adet === 0) throw new Error('Z komutundan sonra sayı');
    // Yay komutunda 4. ve 5. argüman tek haneli bayraktır; ayırıcısız yazılabilir.
    if (komut.toUpperCase() === 'A' && (argNo % 7 === 3 || argNo % 7 === 4)) {
      if (c !== '0' && c !== '1') throw new Error('geçersiz yay bayrağı');
      cikti.push(c);
      i++;
      argNo++;
      continue;
    }
    sayiRe.lastIndex = i;
    const m = sayiRe.exec(d);
    if (!m) throw new Error(`sayı bekleniyordu (konum ${i}): ${d.slice(i, i + 12)}`);
    cikti.push(m[0]);
    i = sayiRe.lastIndex;
    argNo++;
  }
  if (komut && KOMUT_ARG[komut.toUpperCase()] > 0 && argNo % KOMUT_ARG[komut.toUpperCase()] !== 0) {
    throw new Error(`eksik argüman: ${komut}`);
  }
  return { d: cikti.join(' '), komutSayisi };
}

// ---------------------------------------------------------------------------
// Bağlam: tek bir SVG belgesi için kimlik sayacı, <defs> ve uyarılar.
// ---------------------------------------------------------------------------
function baglamOlustur(ortam) {
  const cx = {
    ortam,
    kimlik: 0,
    defs: [],
    uyarilar: [],
    bagimliliklar: new Set(),
    yolSayisi: 0,
    derinlik: 0,
    yeniKimlik(onek) { return `${onek}${++cx.kimlik}`; },
    uyari(m) { if (!cx.uyarilar.includes(m)) cx.uyarilar.push(m); },
  };
  return cx;
}

function cocuk(dugum, ad) { return dugum.cocuklar.find(c => c.ad === ad); }

// ---------------------------------------------------------------------------
// Gradient
// ---------------------------------------------------------------------------
function durakXml(renk, konum) {
  const op = renk.a < 1 ? ` stop-opacity="${sayi(renk.a)}"` : '';
  return `<stop offset="${sayi(konum)}" stop-color="${onaltilik(renk)}"${op}/>`;
}

function gradientDuraklari(g, cx) {
  const ogeler = g.cocuklar.filter(c => c.ad === 'item');
  const duraklar = [];
  if (ogeler.length) {
    for (const o of ogeler) {
      const r = renkCoz(o.oz['android:color'], cx);
      if (r) duraklar.push({ r, k: parseFloat(o.oz['android:offset'] || '0') });
    }
  } else {
    const bas = renkCoz(g.oz['android:startColor'], cx) || { r: 0, g: 0, b: 0, a: 0 };
    const son = renkCoz(g.oz['android:endColor'], cx) || { r: 0, g: 0, b: 0, a: 0 };
    duraklar.push({ r: bas, k: 0 });
    if (g.oz['android:centerColor']) {
      const orta = renkCoz(g.oz['android:centerColor'], cx);
      if (orta) duraklar.push({ r: orta, k: 0.5 });
    }
    duraklar.push({ r: son, k: 1 });
  }
  return duraklar;
}

function yayilma(g) {
  const t = g.oz['android:tileMode'];
  if (t === 'repeat') return ' spreadMethod="repeat"';
  if (t === 'mirror') return ' spreadMethod="reflect"';
  return '';
}

// VectorDrawable içindeki <gradient>: koordinatlar viewport biriminde.
function vektorGradient(g, cx, boyaDonustur) {
  const tur = g.oz['android:type'] || 'linear';
  const duraklar = gradientDuraklari(g, cx).map(d => ({ r: boyaDonustur(d.r), k: d.k }));
  if (!duraklar.length) return null;
  if (tur === 'sweep') {
    cx.uyari('sweep gradient SVG\'de yok; ilk renk düz dolgu olarak kullanıldı');
    return { duz: duraklar[0].r };
  }
  const id = cx.yeniKimlik('g');
  const ic = duraklar.map(d => durakXml(d.r, d.k)).join('');
  const f = ad => sayi(parseFloat(g.oz['android:' + ad] || '0'));
  if (tur === 'radial') {
    cx.defs.push(`<radialGradient id="${id}" gradientUnits="userSpaceOnUse" cx="${f('centerX')}" cy="${f('centerY')}" r="${f('gradientRadius')}"${yayilma(g)}>${ic}</radialGradient>`);
  } else {
    cx.defs.push(`<linearGradient id="${id}" gradientUnits="userSpaceOnUse" x1="${f('startX')}" y1="${f('startY')}" x2="${f('endX')}" y2="${f('endY')}"${yayilma(g)}>${ic}</linearGradient>`);
  }
  return { url: `url(#${id})` };
}

// ---------------------------------------------------------------------------
// VectorDrawable
// ---------------------------------------------------------------------------
function vektorHazirla(kok, cx) {
  const vw = parseFloat(kok.oz['android:viewportWidth']);
  const vh = parseFloat(kok.oz['android:viewportHeight']);
  if (!(vw > 0) || !(vh > 0)) throw new Desteklenmiyor('viewport boyutu yok');
  const iw = olcu(kok.oz['android:width'], cx, vw);
  const ih = olcu(kok.oz['android:height'], cx, vh);

  // android:tint (SRC_IN): tüm boyalar tint rengini alır, kendi saydamlıkları korunur.
  let tint = null;
  if (kok.oz['android:tint']) {
    tint = renkCoz(kok.oz['android:tint'], cx);
    if (!tint) cx.uyari(`tint uygulanamadı (${kok.oz['android:tint']}); yol renkleri olduğu gibi bırakıldı`);
  }
  const boyaDonustur = r => (tint ? { r: tint.r, g: tint.g, b: tint.b, a: r.a * tint.a } : r);

  // Bir yolun dolgu/çizgi boyasını çözer: { deger, opaklik } | null
  function boya(yol, oz) {
    const ek = yol.cocuklar.find(c => c.ad === 'aapt:attr' && c.oz.name === 'android:' + oz);
    if (ek) {
      const g = cocuk(ek, 'gradient');
      if (!g) { cx.uyari(`aapt:attr içinde gradient yok (${oz})`); return null; }
      const sonuc = vektorGradient(g, cx, boyaDonustur);
      if (!sonuc) return null;
      if (sonuc.duz) return { deger: onaltilik(sonuc.duz), opaklik: sonuc.duz.a };
      return { deger: sonuc.url, opaklik: 1 };
    }
    const ham = yol.oz['android:' + oz];
    if (ham == null) return null;
    const r = renkCoz(ham, cx);
    if (!r) return null;
    const b = boyaDonustur(r);
    return { deger: onaltilik(b), opaklik: b.a };
  }

  function yolXml(yol) {
    const ham = yol.oz['android:pathData'];
    if (!ham) return '';
    let d;
    try {
      d = yolNormallestir(ham).d;
    } catch (e) {
      cx.uyari(`pathData ayrıştırılamadı, olduğu gibi yazıldı: ${e.message}`);
      d = ham.replace(/\s+/g, ' ').trim();
    }
    cx.yolSayisi++;
    const oz = [`d="${xmlKacis(d)}"`];
    const dolgu = boya(yol, 'fillColor');
    const dolguAlfa = parseFloat(yol.oz['android:fillAlpha'] || '1');
    if (dolgu && dolgu.opaklik * dolguAlfa > 0) {
      oz.push(`fill="${dolgu.deger}"`);
      const op = dolgu.opaklik * dolguAlfa;
      if (op < 1) oz.push(`fill-opacity="${sayi(op)}"`);
      if (yol.oz['android:fillType'] === 'evenOdd') oz.push('fill-rule="evenodd"');
    } else {
      oz.push('fill="none"');
    }
    const cizgi = boya(yol, 'strokeColor');
    const kalinlik = parseFloat(yol.oz['android:strokeWidth'] || '0');
    const cizgiAlfa = parseFloat(yol.oz['android:strokeAlpha'] || '1');
    if (cizgi && kalinlik > 0 && cizgi.opaklik * cizgiAlfa > 0) {
      oz.push(`stroke="${cizgi.deger}"`, `stroke-width="${sayi(kalinlik)}"`);
      const op = cizgi.opaklik * cizgiAlfa;
      if (op < 1) oz.push(`stroke-opacity="${sayi(op)}"`);
      const uc = yol.oz['android:strokeLineCap'];
      if (uc && uc !== 'butt') oz.push(`stroke-linecap="${uc}"`);
      const birlesim = yol.oz['android:strokeLineJoin'];
      if (birlesim && birlesim !== 'miter') oz.push(`stroke-linejoin="${birlesim}"`);
      const gonye = yol.oz['android:strokeMiterLimit'];
      if (gonye) oz.push(`stroke-miterlimit="${sayi(parseFloat(gonye))}"`);
    }
    if (yol.oz['android:trimPathStart'] || yol.oz['android:trimPathEnd'] || yol.oz['android:trimPathOffset']) {
      cx.uyari('trimPath* yok sayıldı');
    }
    return `<path ${oz.join(' ')}/>`;
  }

  function grupDonusumu(g) {
    const f = (ad, v) => parseFloat(g.oz['android:' + ad] ?? v);
    const rot = f('rotation', 0), px = f('pivotX', 0), py = f('pivotY', 0);
    const sx = f('scaleX', 1), sy = f('scaleY', 1), tx = f('translateX', 0), ty = f('translateY', 0);
    // Android sırası: pivotu orijine taşı → ölçekle → döndür → pivotu ve ötelemeyi geri ekle.
    const parca = [];
    if (tx + px !== 0 || ty + py !== 0) parca.push(`translate(${sayi(tx + px)} ${sayi(ty + py)})`);
    if (rot !== 0) parca.push(`rotate(${sayi(rot)})`);
    if (sx !== 1 || sy !== 1) parca.push(`scale(${sayi(sx)} ${sayi(sy)})`);
    if (px !== 0 || py !== 0) parca.push(`translate(${sayi(-px)} ${sayi(-py)})`);
    return parca.join(' ');
  }

  // <clip-path> kendisinden SONRA gelen kardeşleri kırpar (aynı grup içinde).
  function cocuklarXml(dugum) {
    let cikti = '';
    let acik = 0;
    for (const c of dugum.cocuklar) {
      if (c.ad === 'path') cikti += yolXml(c);
      else if (c.ad === 'group') {
        const t = grupDonusumu(c);
        const ic = cocuklarXml(c);
        if (ic) cikti += t ? `<g transform="${t}">${ic}</g>` : `<g>${ic}</g>`;
      } else if (c.ad === 'clip-path') {
        const ham = c.oz['android:pathData'];
        if (!ham) continue;
        let d;
        try { d = yolNormallestir(ham).d; } catch (e) { d = ham.replace(/\s+/g, ' ').trim(); }
        const id = cx.yeniKimlik('c');
        const kural = c.oz['android:fillType'] === 'evenOdd' ? ' clip-rule="evenodd"' : '';
        cx.defs.push(`<clipPath id="${id}"><path d="${xmlKacis(d)}"${kural}/></clipPath>`);
        cikti += `<g clip-path="url(#${id})">`;
        acik++;
      } else if (c.ad !== 'aapt:attr') {
        cx.uyari(`vector içinde bilinmeyen öğe: <${c.ad}>`);
      }
    }
    return cikti + '</g>'.repeat(acik);
  }

  let govde = cocuklarXml(kok);
  const alfa = parseFloat(kok.oz['android:alpha'] || '1');
  if (alfa < 1) govde = `<g opacity="${sayi(alfa)}">${govde}</g>`;

  return {
    iw, ih,
    ciz(w, h) {
      const sx = w / vw, sy = h / vh;
      if (sx === 1 && sy === 1) return govde;
      return `<g transform="scale(${sayi(sx)} ${sayi(sy)})">${govde}</g>`;
    },
    // Kök vector için viewBox doğrudan kullanılabilir.
    vektor: { vw, vh, govde },
  };
}

// ---------------------------------------------------------------------------
// <shape> (GradientDrawable): dikdörtgen ve oval.
// ---------------------------------------------------------------------------
function sekilHazirla(kok, cx) {
  const tur = kok.oz['android:shape'] || 'rectangle';
  if (tur !== 'rectangle' && tur !== 'oval') throw new Desteklenmiyor(`shape türü desteklenmiyor: ${tur}`);
  const boyut = cocuk(kok, 'size');
  const iw = boyut ? olcu(boyut.oz['android:width'], cx, null) : null;
  const ih = boyut ? olcu(boyut.oz['android:height'], cx, null) : null;
  const dolgu = cocuk(kok, 'solid');
  const gradient = cocuk(kok, 'gradient');
  const cizgi = cocuk(kok, 'stroke');
  const kose = cocuk(kok, 'corners');

  return {
    iw, ih,
    ciz(w, h) {
      const oz = [];
      // Dolgu
      if (gradient) {
        const gt = gradient.oz['android:type'] || 'linear';
        const duraklar = gradientDuraklari(gradient, cx);
        if (gt === 'sweep') {
          cx.uyari('shape sweep gradient SVG\'de yok; ilk renk kullanıldı');
          oz.push(`fill="${onaltilik(duraklar[0].r)}"`);
          if (duraklar[0].r.a < 1) oz.push(`fill-opacity="${sayi(duraklar[0].r.a)}"`);
        } else {
          const id = cx.yeniKimlik('g');
          const ic = duraklar.map(d => durakXml(d.r, d.k)).join('');
          if (gt === 'radial') {
            const mx = parseFloat(gradient.oz['android:centerX'] || '0.5') * w;
            const my = parseFloat(gradient.oz['android:centerY'] || '0.5') * h;
            const hamR = gradient.oz['android:gradientRadius'] || '';
            let r;
            if (/%p?$/.test(hamR)) r = (parseFloat(hamR) / 100) * Math.min(w, h);
            else r = olcu(hamR || null, cx, Math.min(w, h) / 2);
            cx.defs.push(`<radialGradient id="${id}" gradientUnits="userSpaceOnUse" cx="${sayi(mx)}" cy="${sayi(my)}" r="${sayi(r)}">${ic}</radialGradient>`);
          } else {
            // Açı: 0 = soldan sağa, 90 = aşağıdan yukarı (saat yönünün tersine artar).
            const aci = ((parseInt(gradient.oz['android:angle'] || '0', 10) % 360) + 360) % 360;
            const yon = {
              0: [0, 0.5, 1, 0.5], 45: [0, 1, 1, 0], 90: [0.5, 1, 0.5, 0], 135: [1, 1, 0, 0],
              180: [1, 0.5, 0, 0.5], 225: [1, 0, 0, 1], 270: [0.5, 0, 0.5, 1], 315: [0, 0, 1, 1],
            }[aci];
            if (!yon) throw new Desteklenmiyor(`gradient açısı 45'in katı değil: ${aci}`);
            cx.defs.push(`<linearGradient id="${id}" gradientUnits="userSpaceOnUse" x1="${sayi(yon[0] * w)}" y1="${sayi(yon[1] * h)}" x2="${sayi(yon[2] * w)}" y2="${sayi(yon[3] * h)}">${ic}</linearGradient>`);
          }
          oz.push(`fill="url(#${id})"`);
        }
      } else if (dolgu) {
        const r = renkCoz(dolgu.oz['android:color'], cx);
        if (r && r.a > 0) {
          oz.push(`fill="${onaltilik(r)}"`);
          if (r.a < 1) oz.push(`fill-opacity="${sayi(r.a)}"`);
        } else oz.push('fill="none"');
      } else oz.push('fill="none"');

      // Çizgi: Android kenarlığı sınırın içine, yarım kalınlık içeri çekerek çizer.
      let k = 0;
      if (cizgi) {
        const r = renkCoz(cizgi.oz['android:color'], cx);
        k = olcu(cizgi.oz['android:width'], cx, 0);
        if (r && r.a > 0 && k > 0) {
          oz.push(`stroke="${onaltilik(r)}"`, `stroke-width="${sayi(k)}"`);
          if (r.a < 1) oz.push(`stroke-opacity="${sayi(r.a)}"`);
          const cizgiBoy = olcu(cizgi.oz['android:dashWidth'], cx, 0);
          const bosluk = olcu(cizgi.oz['android:dashGap'], cx, 0);
          if (cizgiBoy > 0 && bosluk > 0) oz.push(`stroke-dasharray="${sayi(cizgiBoy)} ${sayi(bosluk)}"`);
        } else k = 0;
      }
      if (oz.length === 1 && oz[0] === 'fill="none"') return ''; // görünmez şekil

      const x = k / 2, y = k / 2, gw = Math.max(0, w - k), gh = Math.max(0, h - k);
      if (tur === 'oval') {
        return `<ellipse cx="${sayi(w / 2)}" cy="${sayi(h / 2)}" rx="${sayi(gw / 2)}" ry="${sayi(gh / 2)}" ${oz.join(' ')}/>`;
      }
      const sinir = Math.min(gw, gh) / 2;
      const genel = kose ? olcu(kose.oz['android:radius'], cx, 0) : 0;
      const r4 = ['topLeftRadius', 'topRightRadius', 'bottomRightRadius', 'bottomLeftRadius'].map(ad =>
        Math.min(sinir, kose && kose.oz['android:' + ad] != null ? olcu(kose.oz['android:' + ad], cx, genel) : genel));
      if (r4.every(r => r === r4[0])) {
        const rx = r4[0] > 0 ? ` rx="${sayi(r4[0])}"` : '';
        return `<rect x="${sayi(x)}" y="${sayi(y)}" width="${sayi(gw)}" height="${sayi(gh)}"${rx} ${oz.join(' ')}/>`;
      }
      const [tl, tr, br, bl] = r4;
      const x2 = x + gw, y2 = y + gh;
      const yay = (r, ex, ey) => (r > 0 ? `A ${sayi(r)} ${sayi(r)} 0 0 1 ${sayi(ex)} ${sayi(ey)} ` : '');
      const d = `M ${sayi(x + tl)} ${sayi(y)} L ${sayi(x2 - tr)} ${sayi(y)} ${yay(tr, x2, y + tr)}` +
        `L ${sayi(x2)} ${sayi(y2 - br)} ${yay(br, x2 - br, y2)}` +
        `L ${sayi(x + bl)} ${sayi(y2)} ${yay(bl, x, y2 - bl)}` +
        `L ${sayi(x)} ${sayi(y + tl)} ${yay(tl, x + tl, y)}Z`;
      return `<path d="${d}" ${oz.join(' ')}/>`;
    },
  };
}

// ---------------------------------------------------------------------------
// <layer-list>
// ---------------------------------------------------------------------------
function katmanHazirla(kok, cx) {
  const katmanlar = [];
  for (const oge of kok.cocuklar) {
    if (oge.ad !== 'item') continue;
    if (oge.oz['android:id'] && /@android:id\/mask/.test(oge.oz['android:id'])) continue;
    let cizim;
    if (oge.oz['android:drawable']) {
      cizim = basvuruHazirla(oge.oz['android:drawable'], cx);
    } else {
      const ic = oge.cocuklar[0];
      if (!ic) continue;
      cizim = dugumHazirla(ic, cx);
    }
    const o = ad => olcu(oge.oz['android:' + ad], cx, 0);
    katmanlar.push({
      cizim,
      sol: oge.oz['android:start'] != null ? o('start') : o('left'),
      sag: oge.oz['android:end'] != null ? o('end') : o('right'),
      ust: o('top'), alt: o('bottom'),
      gen: oge.oz['android:width'] != null ? o('width') : null,
      yuk: oge.oz['android:height'] != null ? o('height') : null,
      cekim: (oge.oz['android:gravity'] || '').split('|').map(s => s.trim()).filter(Boolean),
    });
  }
  if (!katmanlar.length) throw new Desteklenmiyor('layer-list boş');
  let iw = null, ih = null;
  for (const k of katmanlar) {
    const w = k.gen ?? k.cizim.iw, h = k.yuk ?? k.cizim.ih;
    if (w != null) iw = Math.max(iw ?? 0, w + k.sol + k.sag);
    if (h != null) ih = Math.max(ih ?? 0, h + k.ust + k.alt);
  }
  return {
    iw, ih,
    ciz(W, H) {
      let cikti = '';
      for (const k of katmanlar) {
        const kw = W - k.sol - k.sag, kh = H - k.ust - k.alt;
        const yatay = k.cekim.find(c => ['left', 'right', 'start', 'end', 'center', 'center_horizontal', 'fill', 'fill_horizontal'].includes(c));
        const dikey = k.cekim.find(c => ['top', 'bottom', 'center', 'center_vertical', 'fill', 'fill_vertical'].includes(c));
        // Genişlik verilmemiş ve yatay çekim yoksa katman alanı doldurur (Android varsayılanı).
        let w = k.gen ?? ((!yatay || yatay.startsWith('fill')) ? kw : (k.cizim.iw ?? kw));
        let h = k.yuk ?? ((!dikey || dikey.startsWith('fill')) ? kh : (k.cizim.ih ?? kh));
        let x = k.sol, y = k.ust;
        if (yatay === 'center' || yatay === 'center_horizontal') x = k.sol + (kw - w) / 2;
        else if (yatay === 'right' || yatay === 'end') x = k.sol + kw - w;
        if (dikey === 'center' || dikey === 'center_vertical') y = k.ust + (kh - h) / 2;
        else if (dikey === 'bottom') y = k.ust + kh - h;
        const ic = k.cizim.ciz(w, h);
        if (!ic) continue;
        cikti += (x !== 0 || y !== 0) ? `<g transform="translate(${sayi(x)} ${sayi(y)})">${ic}</g>` : ic;
      }
      return cikti;
    },
  };
}

function basvuruHazirla(basvuru, cx) {
  const m = /^@(drawable|mipmap)\/(\w+)$/.exec(basvuru);
  if (!m) {
    // Katmanda doğrudan renk: @color/x ya da #hex → düz dolgu
    const r = renkCoz(basvuru, cx);
    if (!r) throw new Desteklenmiyor(`çözülemeyen başvuru: ${basvuru}`);
    return {
      iw: null, ih: null,
      ciz(w, h) {
        if (r.a === 0) return '';
        const op = r.a < 1 ? ` fill-opacity="${sayi(r.a)}"` : '';
        return `<rect width="${sayi(w)}" height="${sayi(h)}" fill="${onaltilik(r)}"${op}/>`;
      },
    };
  }
  const kok = cx.ortam.cizimKoku(m[2]);
  if (!kok) throw new Desteklenmiyor(`başvurulan kaynak vektör/şekil değil (bitmap ya da yok): ${basvuru}`);
  cx.bagimliliklar.add(m[2]);
  if (++cx.derinlik > 8) throw new Desteklenmiyor('iç içe başvuru çok derin');
  try { return dugumHazirla(kok, cx); } finally { cx.derinlik--; }
}

function dugumHazirla(kok, cx) {
  switch (kok.ad) {
    case 'vector': return vektorHazirla(kok, cx);
    case 'shape': return sekilHazirla(kok, cx);
    case 'layer-list': return katmanHazirla(kok, cx);
    default: throw new Desteklenmiyor(`<${kok.ad}> SVG'ye çevrilemez`);
  }
}

// Kök düğümü tek bir SVG belgesine çevirir.
// Döndürür: { svg, genislik, yukseklik, viewBox, yolSayisi, uyarilar, bagimliliklar }
function svgUret(kok, ortam) {
  const cx = baglamOlustur(ortam);
  const cizim = dugumHazirla(kok, cx);
  if (cizim.iw == null || cizim.ih == null) {
    throw new Desteklenmiyor('kendi boyutu yok (esneyen arka plan); SwiftUI\'da şekil olarak çizilmeli');
  }
  let govde, kutu;
  if (kok.ad === 'vector') {
    govde = cizim.vektor.govde;
    kutu = [cizim.vektor.vw, cizim.vektor.vh];
  } else {
    govde = cizim.ciz(cizim.iw, cizim.ih);
    kutu = [cizim.iw, cizim.ih];
  }
  if (!govde) throw new Desteklenmiyor('görünür içerik yok (tamamen saydam)');
  const defs = cx.defs.length ? `<defs>${cx.defs.join('')}</defs>` : '';
  const svg = `<?xml version="1.0" encoding="UTF-8"?>\n` +
    `<svg xmlns="http://www.w3.org/2000/svg" width="${sayi(cizim.iw)}" height="${sayi(cizim.ih)}" viewBox="0 0 ${sayi(kutu[0])} ${sayi(kutu[1])}">` +
    `${defs}${govde}</svg>\n`;
  return {
    svg,
    genislik: cizim.iw,
    yukseklik: cizim.ih,
    viewBox: `0 0 ${sayi(kutu[0])} ${sayi(kutu[1])}`,
    yolSayisi: cx.yolSayisi,
    uyarilar: cx.uyarilar,
    bagimliliklar: [...cx.bagimliliklar],
  };
}

module.exports = { svgUret, yolNormallestir, onaltilikRenk, Desteklenmiyor };

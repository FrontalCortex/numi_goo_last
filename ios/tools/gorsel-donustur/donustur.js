#!/usr/bin/env node
// Android kaynaklarını (app/src/main/res) iOS Asset Catalog'a taşır.
//
//   node ios/tools/gorsel-donustur/donustur.js            # üret
//   node ios/tools/gorsel-donustur/donustur.js --kuru     # yazmadan yalnızca rapor
//
// Ürettikleri (her çalıştırmada silinip yeniden yazılır):
//   ios/iosApp/iosApp/Assets.xcassets/Gorseller/<ad>.imageset   vector/shape/layer-list → SVG, bitmap → kopya
//   ios/iosApp/iosApp/Assets.xcassets/Renkler/<ad>.colorset     values/colors.xml
//   ios/iosApp/iosApp/Fontlar/                                  res/font içindeki .ttf/.otf
//   ios/tools/gorsel-donustur/rapor.json                        çevrilen/atlanan listesi, öncelikli küme
//
// Adlar Android kaynak adıyla aynıdır: R.drawable.x / R.mipmap.x → Image("x"), R.color.x → Color("x").
// Assets.xcassets kökündeki AppIcon ve AccentColor'a dokunmaz. Bağımlılığı yoktur (yalnızca Node).
'use strict';

const fs = require('fs');
const path = require('path');
const { xmlAyristir } = require('./xml');
const { svgUret, onaltilikRenk, Desteklenmiyor } = require('./cizim');

const KURU = process.argv.includes('--kuru');
const DEPO = path.resolve(__dirname, '..', '..', '..');
const RES = path.join(DEPO, 'app', 'src', 'main', 'res');
const KATALOG = path.join(DEPO, 'ios', 'iosApp', 'iosApp', 'Assets.xcassets');
const GORSEL_KLASORU = path.join(KATALOG, 'Gorseller');
const RENK_KLASORU = path.join(KATALOG, 'Renkler');
const FONT_KLASORU = path.join(DEPO, 'ios', 'iosApp', 'iosApp', 'Fontlar');
const BUYUK_ESIK = 500 * 1024;

// Öncelikli küme: harita ekranı + uygulama kabuğu.
const ONCELIK_KOTLIN = ['MainActivity.kt', 'MapFragment.kt', 'LessonAdapter.kt', 'CustomBottomNavigationView.kt']
  .map(d => path.join(DEPO, 'app', 'src', 'main', 'java', 'com', 'example', 'app', d));
const ORTAK_DERS = path.join(DEPO, 'ios', 'shared', 'src', 'commonMain', 'kotlin', 'com', 'sorobit', 'shared', 'ders');

const oku = d => fs.readFileSync(d, 'utf8');
const goreli = d => path.relative(DEPO, d).split(path.sep).join('/');

function yaz(dosya, icerik) {
  if (KURU) return;
  fs.mkdirSync(path.dirname(dosya), { recursive: true });
  fs.writeFileSync(dosya, icerik);
}
function jsonYaz(dosya, nesne) { yaz(dosya, JSON.stringify(nesne, null, 2) + '\n'); }
const BILGI = { author: 'xcode', version: 1 };

// ---------------------------------------------------------------------------
// 1) Renkler ve ölçüler
// ---------------------------------------------------------------------------
function degerleriOku(klasor, dosyaAdi, etiket) {
  const dosya = path.join(RES, klasor, dosyaAdi);
  const sonuc = new Map();
  if (!fs.existsSync(dosya)) return sonuc;
  for (const c of xmlAyristir(oku(dosya)).cocuklar) {
    if (c.ad === etiket && c.oz.name) sonuc.set(c.oz.name, c.metin.trim());
  }
  return sonuc;
}

const hamRenkler = degerleriOku('values', 'colors.xml', 'color');
const hamGeceRenkleri = degerleriOku('values-night', 'colors.xml', 'color');
const hamOlculer = degerleriOku('values', 'dimens.xml', 'dimen');

// res/color/*.xml (durum listeleri): durumsuz (varsayılan) öğenin rengi kullanılır.
const renkSecicileri = new Map();
const renkSeciciKlasoru = path.join(RES, 'color');
if (fs.existsSync(renkSeciciKlasoru)) {
  for (const d of fs.readdirSync(renkSeciciKlasoru)) {
    if (!d.endsWith('.xml')) continue;
    const kok = xmlAyristir(oku(path.join(renkSeciciKlasoru, d)));
    const varsayilan = kok.cocuklar.filter(c => c.ad === 'item')
      .find(c => !Object.keys(c.oz).some(k => k.startsWith('android:state_')));
    if (varsayilan) renkSecicileri.set(d.slice(0, -4), varsayilan.oz['android:color']);
  }
}

// '@color/x' zincirlerini izleyerek '#AARRGGBB' döndürür.
function renkAdiCoz(ad, tablo = hamRenkler, derinlik = 0) {
  if (derinlik > 8) return null;
  let v = tablo.get(ad) ?? hamRenkler.get(ad) ?? renkSecicileri.get(ad);
  if (v == null) return null;
  v = v.trim();
  if (v[0] === '#') return v;
  const m = /^@color\/(\w+)$/.exec(v);
  return m ? renkAdiCoz(m[1], tablo, derinlik + 1) : null;
}

function olcuAdiCoz(ad, derinlik = 0) {
  const v = hamOlculer.get(ad);
  if (v == null || derinlik > 8) return null;
  let m = /^(-?[\d.]+)(dp|dip|px|sp)?$/.exec(v);
  if (m) return parseFloat(m[1]);
  m = /^@dimen\/(\w+)$/.exec(v);
  return m ? olcuAdiCoz(m[1], derinlik + 1) : null;
}

// ---------------------------------------------------------------------------
// 2) Çizim kaynaklarının dökümü
// ---------------------------------------------------------------------------
// ad -> { xml: {dosya}, bitmapler: { yogunluk: dosya }, tur: 'drawable' | 'mipmap' }
const YOGUNLUKLAR = ['', 'nodpi', 'anydpi', 'ldpi', 'mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi'];
const kaynaklar = new Map();
const cakismalar = [];

function klasorYogunlugu(klasor) {
  // drawable, drawable-xhdpi, mipmap-anydpi-v26 ...
  const parcalar = klasor.split('-').slice(1);
  const y = parcalar.find(p => YOGUNLUKLAR.includes(p));
  const diger = parcalar.filter(p => !YOGUNLUKLAR.includes(p) && !/^v\d+$/.test(p));
  return { yogunluk: y || '', bilinmeyen: diger };
}

for (const tur of ['drawable', 'mipmap']) {
  for (const klasor of fs.readdirSync(RES).sort()) {
    if (klasor !== tur && !klasor.startsWith(tur + '-')) continue;
    const { yogunluk, bilinmeyen } = klasorYogunlugu(klasor);
    if (bilinmeyen.length) continue; // night/land gibi niteleyiciler: bu projede yok
    for (const dosya of fs.readdirSync(path.join(RES, klasor)).sort()) {
      const uz = path.extname(dosya).toLowerCase();
      const ad = dosya.slice(0, dosya.length - uz.length).replace(/\.9$/, '');
      const tam = path.join(RES, klasor, dosya);
      let k = kaynaklar.get(ad);
      if (k && k.tur !== tur) {
        // Aynı ad hem drawable hem mipmap: iOS'ta tek ad var, drawable kazanır.
        if (!cakismalar.some(c => c.ad === ad)) cakismalar.push({ ad, kazanan: k.tur, atlanan: `${tur}-*/${dosya}` });
        continue;
      }
      if (!k) { k = { ad, tur, xml: null, bitmapler: {} }; kaynaklar.set(ad, k); }
      if (uz === '.xml') {
        // anydpi içindeki vektör, yoğunluksuz olandan önceliklidir (Android'de de öyle).
        if (!k.xml || yogunluk === 'anydpi') k.xml = tam;
      } else k.bitmapler[yogunluk] = tam;
    }
  }
}

const kokOnbellek = new Map();
function cizimKoku(ad) {
  if (kokOnbellek.has(ad)) return kokOnbellek.get(ad);
  const k = kaynaklar.get(ad);
  let kok = null;
  if (k && k.xml) {
    try { kok = xmlAyristir(oku(k.xml)); } catch (e) { kok = null; }
  }
  kokOnbellek.set(ad, kok);
  return kok;
}

const ortam = { renkCoz: ad => renkAdiCoz(ad), olcuCoz: olcuAdiCoz, cizimKoku };

// ---------------------------------------------------------------------------
// 3) Dönüştürme
// ---------------------------------------------------------------------------
function pngBoyutu(dosya) {
  const b = Buffer.alloc(24);
  const fd = fs.openSync(dosya, 'r');
  fs.readSync(fd, b, 0, 24, 0);
  fs.closeSync(fd);
  if (b.toString('latin1', 1, 4) !== 'PNG') return null;
  return { gen: b.readUInt32BE(16), yuk: b.readUInt32BE(20) };
}

// Atlanan <shape> için SwiftUI'da elle çizmeye yetecek kısa özet (renkler çözülmüş).
function sekilOzeti(kok) {
  const renk = v => {
    const m = /^@color\/(\w+)$/.exec(v.trim());
    return m ? `${renkAdiCoz(m[1]) || '?'} (@color/${m[1]})` : v.trim();
  };
  const o = { bicim: kok.oz['android:shape'] || 'rectangle' };
  for (const c of kok.cocuklar) {
    const oz = {};
    for (const [k, v] of Object.entries(c.oz)) oz[k.replace('android:', '')] = /color/i.test(k) ? renk(v) : v;
    o[c.ad] = oz;
  }
  return o;
}

if (!KURU) {
  fs.rmSync(GORSEL_KLASORU, { recursive: true, force: true });
  fs.rmSync(RENK_KLASORU, { recursive: true, force: true });
}

const sonuclar = new Map(); // ad -> { durum: 'cevrildi' | 'atlandi', ... }
const adlar = [...kaynaklar.keys()].sort();

for (const ad of adlar) {
  const k = kaynaklar.get(ad);
  const hedef = path.join(GORSEL_KLASORU, `${ad}.imageset`);

  // Uygulama simgesi görsel olarak taşınmaz (AppIcon ayrı ele alınır).
  if (/^ic_launcher/.test(ad)) {
    sonuclar.set(ad, { durum: 'atlandi', tur: 'başlatıcı simgesi', neden: 'uygulama simgesi; imageset olarak taşınmaz (AppIcon ayrı)' });
    continue;
  }

  if (k.xml) {
    let kok;
    try { kok = xmlAyristir(oku(k.xml)); } catch (e) {
      sonuclar.set(ad, { durum: 'atlandi', tur: 'xml', neden: 'XML ayrıştırılamadı: ' + e.message, kaynak: goreli(k.xml) });
      continue;
    }
    try {
      const s = svgUret(kok, ortam);
      yaz(path.join(hedef, `${ad}.svg`), s.svg);
      jsonYaz(path.join(hedef, 'Contents.json'), {
        images: [{ filename: `${ad}.svg`, idiom: 'universal' }],
        info: BILGI,
        properties: { 'preserves-vector-representation': true },
      });
      sonuclar.set(ad, {
        durum: 'cevrildi', tur: kok.ad, bicim: 'svg', kaynak: goreli(k.xml),
        boyut: `${s.genislik}x${s.yukseklik}`, viewBox: s.viewBox, yolSayisi: s.yolSayisi,
        bayt: Buffer.byteLength(s.svg), uyarilar: s.uyarilar, bagimliliklar: s.bagimliliklar,
      });
      continue;
    } catch (e) {
      if (!(e instanceof Desteklenmiyor)) throw new Error(`${ad}: ${e.stack}`);
      // XML çevrilemedi; aynı adın bitmap'i varsa ona düş.
      if (!Object.keys(k.bitmapler).length) {
        sonuclar.set(ad, { durum: 'atlandi', tur: kok.ad, neden: e.message, kaynak: goreli(k.xml), ...(kok.ad === 'shape' ? { sekil: sekilOzeti(kok) } : {}) });
        continue;
      }
    }
  }

  // Bitmap: mdpi (ya da yoğunluksuz klasör) = 1x, xhdpi = 2x, xxhdpi = 3x.
  const b = k.bitmapler;
  const olcekler = [['1x', b.mdpi || b[''] || b.nodpi], ['2x', b.xhdpi], ['3x', b.xxhdpi]].filter(o => o[1]);
  if (!olcekler.length && b.xxxhdpi) olcekler.push(['3x', b.xxxhdpi]);
  if (!olcekler.length && b.hdpi) olcekler.push(['2x', b.hdpi]);
  const uz = olcekler.length ? path.extname(olcekler[0][1]).toLowerCase() : '';
  if (!olcekler.length) {
    sonuclar.set(ad, { durum: 'atlandi', tur: 'bitmap', neden: 'kullanılabilir yoğunluk yok' });
    continue;
  }
  if (uz === '.gif') {
    sonuclar.set(ad, { durum: 'atlandi', tur: 'gif', neden: 'Asset Catalog hareketli GIF oynatmaz; kareleri ayrı taşınmalı', kaynak: goreli(olcekler[0][1]), bayt: fs.statSync(olcekler[0][1]).size });
    continue;
  }
  if (uz === '.webp') {
    sonuclar.set(ad, { durum: 'atlandi', tur: 'webp', neden: 'WebP imageset olarak desteklenmez; önce PNG\'ye çevrilmeli', kaynak: goreli(olcekler[0][1]) });
    continue;
  }
  const gorseller = [];
  let toplam = 0;
  let enBuyuk = null;
  for (const [olcek, kaynakDosya] of olcekler) {
    const hedefAd = olcekler.length === 1 && olcek === '1x' ? `${ad}${uz}` : `${ad}@${olcek}${path.extname(kaynakDosya).toLowerCase()}`;
    if (!KURU) {
      fs.mkdirSync(hedef, { recursive: true });
      fs.copyFileSync(kaynakDosya, path.join(hedef, hedefAd));
    }
    gorseller.push({ filename: hedefAd, idiom: 'universal', scale: olcek });
    toplam += fs.statSync(kaynakDosya).size;
    const p = pngBoyutu(kaynakDosya);
    if (p) enBuyuk = `${p.gen}x${p.yuk}px@${olcek}`;
  }
  jsonYaz(path.join(hedef, 'Contents.json'), { images: gorseller, info: BILGI });
  sonuclar.set(ad, {
    durum: 'cevrildi', tur: 'bitmap', bicim: uz.slice(1), kaynak: goreli(olcekler[0][1]),
    olcekler: olcekler.map(o => o[0]), boyut: enBuyuk, bayt: toplam, uyarilar: [], bagimliliklar: [],
  });
}
jsonYaz(path.join(GORSEL_KLASORU, 'Contents.json'), { info: BILGI });

// ---------------------------------------------------------------------------
// 4) Renk setleri
// ---------------------------------------------------------------------------
function renkBileseni(hex) {
  const r = onaltilikRenk(hex);
  const h = v => '0x' + v.toString(16).toUpperCase().padStart(2, '0');
  return {
    'color-space': 'srgb',
    components: { alpha: r.a.toFixed(3), blue: h(r.b), green: h(r.g), red: h(r.r) },
  };
}

const renkRaporu = { yazilan: 0, karanlikli: 0, cozulemeyen: [] };
for (const ad of [...hamRenkler.keys()].sort()) {
  const acik = renkAdiCoz(ad);
  if (!acik || !onaltilikRenk(acik)) { renkRaporu.cozulemeyen.push(ad); continue; }
  const renkler = [{ color: renkBileseni(acik), idiom: 'universal' }];
  if (hamGeceRenkleri.has(ad)) {
    const koyu = renkAdiCoz(ad, hamGeceRenkleri);
    if (koyu && onaltilikRenk(koyu)) {
      renkler.push({
        appearances: [{ appearance: 'luminosity', value: 'dark' }],
        color: renkBileseni(koyu), idiom: 'universal',
      });
      renkRaporu.karanlikli++;
    }
  }
  jsonYaz(path.join(RENK_KLASORU, `${ad}.colorset`, 'Contents.json'), { colors: renkler, info: BILGI });
  renkRaporu.yazilan++;
}
jsonYaz(path.join(RENK_KLASORU, 'Contents.json'), { info: BILGI });

// ---------------------------------------------------------------------------
// 5) Yazı tipleri: kopyala ve `name` tablosundan adları oku
// ---------------------------------------------------------------------------
function fontAdlari(dosya) {
  const b = fs.readFileSync(dosya);
  const tabloSayisi = b.readUInt16BE(4);
  const tablolar = {};
  for (let i = 0; i < tabloSayisi; i++) {
    const o = 12 + i * 16;
    tablolar[b.toString('latin1', o, o + 4)] = { konum: b.readUInt32BE(o + 8), boy: b.readUInt32BE(o + 12) };
  }
  const sonuc = { tablolar: Object.keys(tablolar).sort(), degisken: 'fvar' in tablolar, eksenler: [] };
  const t = tablolar.name;
  if (t) {
    const adet = b.readUInt16BE(t.konum + 2);
    const metinBas = t.konum + b.readUInt16BE(t.konum + 4);
    const istenen = { 1: 'aile', 2: 'altAile', 4: 'tamAd', 6: 'postScript', 16: 'tipografikAile', 17: 'tipografikAltAile' };
    for (let i = 0; i < adet; i++) {
      const o = t.konum + 6 + i * 12;
      const platform = b.readUInt16BE(o), adNo = b.readUInt16BE(o + 6);
      const boy = b.readUInt16BE(o + 8), konum = metinBas + b.readUInt16BE(o + 10);
      if (!(adNo in istenen)) continue;
      let metin;
      if (platform === 3 || platform === 0) {
        // UTF-16BE
        metin = '';
        for (let j = 0; j + 1 < boy; j += 2) metin += String.fromCharCode(b.readUInt16BE(konum + j));
      } else metin = b.toString('latin1', konum, konum + boy);
      // Windows (3) kaydı varsa onu yeğle.
      if (!(istenen[adNo] in sonuc) || platform === 3) sonuc[istenen[adNo]] = metin;
    }
  }
  if (tablolar['OS/2']) sonuc.agirlik = b.readUInt16BE(tablolar['OS/2'].konum + 4);
  if (tablolar.fvar) {
    const f = tablolar.fvar.konum;
    const eksenBas = f + b.readUInt16BE(f + 4), eksenSayisi = b.readUInt16BE(f + 8), eksenBoy = b.readUInt16BE(f + 10);
    for (let i = 0; i < eksenSayisi; i++) {
      const o = eksenBas + i * eksenBoy;
      sonuc.eksenler.push({
        etiket: b.toString('latin1', o, o + 4),
        en_az: b.readInt32BE(o + 4) / 65536, varsayilan: b.readInt32BE(o + 8) / 65536, en_cok: b.readInt32BE(o + 12) / 65536,
      });
    }
  }
  return sonuc;
}

const fontlar = [];
const fontKaynak = path.join(RES, 'font');
if (fs.existsSync(fontKaynak)) {
  for (const d of fs.readdirSync(fontKaynak).sort()) {
    if (!/\.(ttf|otf)$/i.test(d)) continue;
    const tam = path.join(fontKaynak, d);
    if (!KURU) {
      fs.mkdirSync(FONT_KLASORU, { recursive: true });
      fs.copyFileSync(tam, path.join(FONT_KLASORU, d));
    }
    fontlar.push({ dosya: d, bayt: fs.statSync(tam).size, ...fontAdlari(tam) });
  }
}

// ---------------------------------------------------------------------------
// 6) Öncelikli küme: harita ekranı + kabuk
// ---------------------------------------------------------------------------
const oncelik = new Map(); // ad -> Set(nereden)
const oncelikRenk = new Map();
function ekle(harita, ad, nereden) {
  if (!harita.has(ad)) harita.set(ad, new Set());
  harita.get(ad).add(nereden);
}

const taranacakDuzenler = [];
const gorulenDuzenler = new Set();
function duzenEkle(ad) {
  if (gorulenDuzenler.has(ad)) return;
  gorulenDuzenler.add(ad);
  for (const klasor of ['layout', 'menu']) {
    const d = path.join(RES, klasor, ad + '.xml');
    if (fs.existsSync(d)) taranacakDuzenler.push(d);
  }
}
const yilanYap = s => s.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase();

for (const d of ONCELIK_KOTLIN) {
  if (!fs.existsSync(d)) continue;
  const metin = oku(d);
  const dosyaAdi = path.basename(d);
  for (const m of metin.matchAll(/(?<!android\.)\bR\.(drawable|mipmap)\.(\w+)/g)) ekle(oncelik, m[2], dosyaAdi);
  for (const m of metin.matchAll(/(?<!android\.)\bR\.color\.(\w+)/g)) ekle(oncelikRenk, m[1], dosyaAdi);
  for (const m of metin.matchAll(/\bR\.(?:layout|menu)\.(\w+)/g)) duzenEkle(m[1]);
  for (const m of metin.matchAll(/\b([A-Z]\w+)Binding\b/g)) duzenEkle(yilanYap(m[1]));
}
while (taranacakDuzenler.length) {
  const d = taranacakDuzenler.pop();
  const metin = oku(d);
  const dosyaAdi = goreli(d).replace('app/src/main/res/', '');
  for (const m of metin.matchAll(/@(drawable|mipmap)\/(\w+)/g)) ekle(oncelik, m[2], dosyaAdi);
  for (const m of metin.matchAll(/@color\/(\w+)/g)) ekle(oncelikRenk, m[1], dosyaAdi);
  for (const m of metin.matchAll(/@(?:layout|menu)\/(\w+)/g)) duzenEkle(m[1]);
}
// Ortak kod kaynak adlarını String olarak taşır: bilinen bir kaynak adıyla eşleşen değişmezler.
if (fs.existsSync(ORTAK_DERS)) {
  for (const d of fs.readdirSync(ORTAK_DERS)) {
    if (!d.endsWith('.kt')) continue;
    for (const m of oku(path.join(ORTAK_DERS, d)).matchAll(/"([a-z][a-z0-9_]*)"/g)) {
      if (kaynaklar.has(m[1])) ekle(oncelik, m[1], 'ortak/ders/' + d);
      if (hamRenkler.has(m[1])) ekle(oncelikRenk, m[1], 'ortak/ders/' + d);
    }
  }
}
// Çevrilen görsellerin başvurduğu görseller de kümeye girer.
for (const ad of [...oncelik.keys()]) {
  const s = sonuclar.get(ad);
  for (const b of (s && s.bagimliliklar) || []) ekle(oncelik, b, `${ad} (iç başvuru)`);
}

const oncelikTablosu = [...oncelik.keys()].sort().map(ad => {
  const s = sonuclar.get(ad);
  return {
    ad,
    durum: !s ? 'YOK' : s.durum,
    tur: s ? s.tur : '-',
    ayrinti: !s ? 'Android kaynaklarında böyle bir drawable/mipmap yok' :
      s.durum === 'cevrildi' ? `${s.bicim}${s.boyut ? ' ' + s.boyut : ''}${s.uyarilar.length ? ' — UYARI: ' + s.uyarilar.join('; ') : ''}` : s.neden,
    ...(s && s.sekil ? { sekil: s.sekil } : {}),
    nereden: [...oncelik.get(ad)].sort(),
  };
});
const oncelikRenkTablosu = [...oncelikRenk.keys()].sort().map(ad => ({
  ad, durum: renkAdiCoz(ad) ? 'cevrildi' : 'YOK', nereden: [...oncelikRenk.get(ad)].sort(),
}));

// ---------------------------------------------------------------------------
// 7) Rapor
// ---------------------------------------------------------------------------
const cevrilen = adlar.filter(a => sonuclar.get(a).durum === 'cevrildi');
const atlanan = adlar.filter(a => sonuclar.get(a).durum === 'atlandi');
const say = (liste, anahtar) => liste.reduce((o, a) => {
  const v = sonuclar.get(a)[anahtar];
  o[v] = (o[v] || 0) + 1;
  return o;
}, {});
const uyarili = cevrilen.filter(a => sonuclar.get(a).uyarilar.length);
const buyukler = cevrilen.concat(atlanan).filter(a => (sonuclar.get(a).bayt || 0) > BUYUK_ESIK)
  .map(a => ({ ad: a, bayt: sonuclar.get(a).bayt, durum: sonuclar.get(a).durum, boyut: sonuclar.get(a).boyut }));
const toplamBayt = cevrilen.reduce((t, a) => t + sonuclar.get(a).bayt, 0);

// Görsel ve renk adı çakışması (Asset Catalog'da türler ayrı ad alanıdır; yine de bilinsin).
const adCakismasi = cevrilen.filter(a => hamRenkler.has(a));

const rapor = {
  ozet: {
    cevrilenGorsel: cevrilen.length, turlereGore: say(cevrilen, 'tur'),
    atlananGorsel: atlanan.length, atlananTurlereGore: say(atlanan, 'tur'),
    toplamBayt, renk: renkRaporu, font: fontlar.length,
  },
  oncelikliGorseller: oncelikTablosu,
  oncelikliRenkler: oncelikRenkTablosu,
  atlananlar: atlanan.map(a => ({ ad: a, ...sonuclar.get(a) })),
  uyarililar: uyarili.map(a => ({ ad: a, uyarilar: sonuclar.get(a).uyarilar })),
  buyukDosyalar: buyukler,
  drawableMipmapCakismalari: cakismalar,
  gorselRenkAdCakismalari: adCakismasi,
  fontlar,
  cevrilenler: cevrilen.map(a => {
    const { durum, uyarilar, bagimliliklar, ...kalan } = sonuclar.get(a);
    return { ad: a, ...kalan };
  }),
};
yaz(path.join(__dirname, 'rapor.json'), JSON.stringify(rapor, null, 2) + '\n');

console.log(`Görsel: ${cevrilen.length} çevrildi ${JSON.stringify(rapor.ozet.turlereGore)}, ${atlanan.length} atlandı ${JSON.stringify(rapor.ozet.atlananTurlereGore)}`);
console.log(`Toplam görsel boyutu: ${(toplamBayt / 1024 / 1024).toFixed(2)} MB`);
console.log(`Renk: ${renkRaporu.yazilan} yazıldı (${renkRaporu.karanlikli} karanlık karşılıklı), çözülemeyen: ${renkRaporu.cozulemeyen.join(', ') || 'yok'}`);
for (const f of fontlar) console.log(`Font: ${f.dosya} → PostScript "${f.postScript}", aile "${f.aile}", alt aile "${f.altAile}", ağırlık ${f.agirlik}, değişken: ${f.degisken ? JSON.stringify(f.eksenler) : 'hayır'}`);
if (cakismalar.length) console.log('drawable/mipmap ad çakışması:', JSON.stringify(cakismalar));
if (adCakismasi.length) console.log('görsel/renk ad çakışması:', adCakismasi.join(', '));
if (buyukler.length) console.log('500 KB üstü:', buyukler.map(b => `${b.ad} ${(b.bayt / 1024).toFixed(0)} KB (${b.durum})`).join(', '));
if (uyarili.length) {
  console.log(`\nUyarılı çeviriler (${uyarili.length}):`);
  for (const a of uyarili) console.log(`  ${a}: ${sonuclar.get(a).uyarilar.join('; ')}`);
}
console.log(`\nÖncelikli küme (harita + kabuk): ${oncelikTablosu.length} görsel`);
for (const s of oncelikTablosu) console.log(`  ${s.durum.padEnd(9)} ${s.ad.padEnd(34)} ${s.tur.padEnd(11)} ${s.ayrinti}`);
const eksikRenk = oncelikRenkTablosu.filter(r => r.durum !== 'cevrildi');
console.log(`Öncelikli renkler: ${oncelikRenkTablosu.length}, eksik: ${eksikRenk.map(r => r.ad).join(', ') || 'yok'}`);
if (KURU) console.log('\n(--kuru: hiçbir dosya yazılmadı)');

#!/usr/bin/env node
// Android metin kaynaklarını (strings.xml) iOS Localizable.strings dosyalarına çevirir.
//
// Kullanım (depo kökünden ya da herhangi bir yerden):
//   node ios/tools/metin-donustur/donustur.js            -> üretir + doğrular
//   node ios/tools/metin-donustur/donustur.js --kontrol  -> yazmaz; diskteki dosyalar güncel mi diye bakar
//   node ios/tools/metin-donustur/donustur.js --dil tr   -> varsayılan values/ klasörünün dili (öntanımlı: tr)
//
// Girdi : app/src/main/res/values*/ altındaki bütün .xml dosyalarındaki
//         <string>, <plurals>, <string-array> öğeleri (Android tarafı yalnızca okunur).
// Çıktı : ios/iosApp/iosApp/<dil>.lproj/Localizable.strings  (anahtar = Android adı)
//         ios/tools/metin-donustur/rapor.json                (uyarılar ve sayımlar)
//
// Anahtar düzeni:
//   <string name="ad">            -> "ad"
//   <string-array name="ad">      -> "ad#adet" (öğe sayısı), "ad#0", "ad#1", ...
//   <plurals name="ad">           -> "ad#one", "ad#other", ... (Metin.cogul okur)
// '#' Android kaynak adlarında bulunamaz; bu yüzden gerçek bir adla çakışmaz.
//
// Bağımlılık yoktur (yalnızca Node.js çekirdeği).

'use strict';

const fs = require('fs');
const path = require('path');

const DEPO = path.resolve(__dirname, '..', '..', '..');
const RES = path.join(DEPO, 'app', 'src', 'main', 'res');
const CIKTI_KOK = path.join(DEPO, 'ios', 'iosApp', 'iosApp');
const RAPOR_YOLU = path.join(__dirname, 'rapor.json');

const args = process.argv.slice(2);
const YALNIZ_KONTROL = args.includes('--kontrol');
const dilSirasi = args.indexOf('--dil');
const VARSAYILAN_DIL = dilSirasi >= 0 && args[dilSirasi + 1] ? args[dilSirasi + 1] : 'tr';

// ---------------------------------------------------------------------------
// XML belirteçleyici (yalnızca kaynak dosyalarının gerektirdiği kadar)
// ---------------------------------------------------------------------------

const VARLIKLAR = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'" };

function varlikCoz(s) {
  return s.replace(/&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);/g, (tam, ad) => {
    if (ad[0] === '#') {
      const kod = ad[1] === 'x' || ad[1] === 'X' ? parseInt(ad.slice(2), 16) : parseInt(ad.slice(1), 10);
      return String.fromCodePoint(kod);
    }
    return Object.prototype.hasOwnProperty.call(VARLIKLAR, ad) ? VARLIKLAR[ad] : tam;
  });
}

function nitelikleriOku(s) {
  const sonuc = {};
  const re = /([A-Za-z_:][\w:.-]*)\s*=\s*("([^"]*)"|'([^']*)')/g;
  let m;
  while ((m = re.exec(s)) !== null) {
    sonuc[m[1]] = varlikCoz(m[3] !== undefined ? m[3] : m[4]);
  }
  return sonuc;
}

// Belirteç türleri: metin, cdata, ac, kapa, tek (kendini kapatan), yorum (atılır).
function belirtecle(xml) {
  const liste = [];
  let i = 0;
  const n = xml.length;
  while (i < n) {
    if (xml[i] !== '<') {
      let j = xml.indexOf('<', i);
      if (j < 0) j = n;
      liste.push({ tur: 'metin', deger: varlikCoz(xml.slice(i, j)) });
      i = j;
    } else if (xml.startsWith('<!--', i)) {
      const j = xml.indexOf('-->', i + 4);
      i = j < 0 ? n : j + 3;
    } else if (xml.startsWith('<![CDATA[', i)) {
      const j = xml.indexOf(']]>', i + 9);
      const son = j < 0 ? n : j;
      liste.push({ tur: 'cdata', deger: xml.slice(i + 9, son) });
      i = j < 0 ? n : j + 3;
    } else if (xml.startsWith('<?', i)) {
      const j = xml.indexOf('?>', i + 2);
      i = j < 0 ? n : j + 2;
    } else if (xml.startsWith('<!', i)) {
      const j = xml.indexOf('>', i);
      i = j < 0 ? n : j + 1;
    } else {
      // Öğe etiketi: nitelik değerlerinin içindeki '>' işaretini atla.
      let j = i + 1;
      let tirnak = null;
      while (j < n) {
        const c = xml[j];
        if (tirnak) {
          if (c === tirnak) tirnak = null;
        } else if (c === '"' || c === "'") {
          tirnak = c;
        } else if (c === '>') {
          break;
        }
        j++;
      }
      const govde = xml.slice(i + 1, j);
      i = j + 1;
      if (govde[0] === '/') {
        liste.push({ tur: 'kapa', ad: govde.slice(1).trim() });
      } else {
        const tek = govde.endsWith('/');
        const ic = tek ? govde.slice(0, -1) : govde;
        const m = /^([^\s]+)([\s\S]*)$/.exec(ic);
        liste.push({ tur: tek ? 'tek' : 'ac', ad: m[1], nitelik: nitelikleriOku(m[2]) });
      }
    }
  }
  return liste;
}

// ---------------------------------------------------------------------------
// Android metin değeri kuralları (aapt2 davranışı)
// ---------------------------------------------------------------------------

// Boşluk kırpma, tırnak ve ters bölü kaçışları:
//  - tırnak dışındaki boşluk dizileri (satır sonu dahil) tek boşluğa iner, baş/son boşluk atılır
//  - "çift tırnak" içindeki boşluklar korunur, tırnakların kendisi atılır
//  - \n \t \' \" \\ \@ \? \uXXXX kaçışları çözülür
function androidDegeriCoz(ham, uyar) {
  let cikti = '';
  let bosluk = false;
  let tirnakta = false;
  const ekle = (c) => {
    if (bosluk && cikti.length > 0) cikti += ' ';
    bosluk = false;
    cikti += c;
  };
  for (let i = 0; i < ham.length; i++) {
    const c = ham[i];
    if (c === '\\') {
      const s = ham[i + 1];
      i++;
      if (s === undefined) break;
      if (s === 'n') ekle('\n');
      else if (s === 't') ekle('\t');
      else if (s === 'u') {
        const onalti = ham.slice(i + 1, i + 5);
        if (/^[0-9a-fA-F]{4}$/.test(onalti)) {
          ekle(String.fromCharCode(parseInt(onalti, 16)));
          i += 4;
        } else {
          uyar('gecersiz \\u kacisi');
        }
      } else if (s === "'" || s === '"' || s === '\\' || s === '@' || s === '?' || s === '#') {
        ekle(s);
      } else if (/\s/.test(s)) {
        // Ters bölü + boşluk: aapt ters bölüyü atar, boşluk olağan kurala girer.
        i--;
      } else {
        uyar('bilinmeyen kacis \\' + s + ' (ters bolu atildi)');
        ekle(s);
      }
    } else if (c === '"') {
      tirnakta = !tirnakta;
      // Tırnak açılırken bekleyen boşluk varsa korunur (ekle ile yazılacak).
      if (tirnakta && bosluk && cikti.length > 0) {
        cikti += ' ';
      }
      bosluk = false;
    } else if (!tirnakta && /\s/.test(c)) {
      bosluk = true;
    } else {
      if (c === "'" && !tirnakta) uyar("kacissiz kesme isareti (aapt'de hata; oldugu gibi alindi)");
      ekle(c);
    }
  }
  if (tirnakta) uyar('kapanmamis cift tirnak');
  return cikti;
}

// Biçim belirteçleri: Java (Formatter) -> Foundation (String(format:)).
//   %s, %1$s      -> %@, %1$@
//   %d, %1$d, %% -> aynen
//   %n            -> satır sonu
//   diğerleri     -> aynen + uyarı
const BICIM_RE = /%(%|(\d+\$)?([-#+ 0,(]*)(\d+)?(\.\d+)?([a-zA-Z]))/g;

function bicimCevir(deger, uyar) {
  return deger.replace(BICIM_RE, (tam, govde, sira, bayrak, genislik, kesinlik, tur) => {
    if (govde === '%') return '%%';
    sira = sira || '';
    if (tur === 's' || tur === 'S') {
      if (bayrak || genislik || kesinlik) uyar('%s uzerindeki bayrak/genislik atildi: ' + tam);
      if (tur === 'S') uyar('%S (buyuk harfe cevir) iOS\'ta yok; %@ yapildi');
      return '%' + sira + '@';
    }
    if (tur === 'd') {
      // Java'nın ',' (binlik ayırıcı) ve '(' bayrakları printf'te yok.
      const temiz = (bayrak || '').replace(/[,(]/g, '');
      if (temiz !== (bayrak || '')) uyar('%d uzerindeki , veya ( bayragi atildi: ' + tam);
      return '%' + sira + temiz + (genislik || '') + 'd';
    }
    if (tur === 'n') return '\n';
    if (tur === 'f' || tur === 'e' || tur === 'g' || tur === 'x' || tur === 'X' || tur === 'c' || tur === 'o') {
      return tam;
    }
    uyar('taninmayan bicim belirteci aynen birakildi: ' + tam);
    return tam;
  });
}

// Karşılaştırma için belirteç imzası: konumdan bağımsız, türe göre sıralı liste.
function bicimImzasi(deger, ios) {
  const liste = [];
  let m;
  const re = new RegExp(BICIM_RE.source.replace('[a-zA-Z]', '[a-zA-Z@]'), 'g');
  while ((m = re.exec(deger)) !== null) {
    if (m[1] === '%') {
      liste.push('%%');
      continue;
    }
    let tur = m[6];
    if (!ios && (tur === 's' || tur === 'S')) tur = '@';
    if (!ios && tur === 'n') continue;
    liste.push((m[2] || '') + tur);
  }
  return liste.join(' ');
}

// ---------------------------------------------------------------------------
// Kaynak dosyasını oku
// ---------------------------------------------------------------------------

// Bir <string>/<item> öğesinin içini (belirteç listesi) düz Android değerine çevirir.
function icerigiTopla(belirtecler, uyar) {
  let ham = '';
  for (const b of belirtecler) {
    if (b.tur === 'metin') {
      ham += b.deger;
    } else if (b.tur === 'cdata') {
      ham += b.deger;
      uyar('CDATA icerigi duz metin olarak alindi');
    } else if (b.tur === 'ac' || b.tur === 'tek') {
      if (b.ad === 'xliff:g') continue; // yalnızca çevirmen notu; içeriği korunur
      if (b.ad === 'br') ham += '\\n';
      uyar('HTML etiketi atildi: <' + b.ad + '>');
    }
  }
  return ham;
}

function dosyayiOku(yol, hedef, uyarilar) {
  const xml = fs.readFileSync(yol, 'utf8').replace(/^﻿/, '');
  const b = belirtecle(xml);
  const dosyaAdi = path.basename(yol);
  let i = 0;

  // 'ad' öğesinin kapanışına kadar olan iç belirteçleri döndürür (iç içe aynı ad beklenmez).
  const iciAl = (ad) => {
    const ic = [];
    let derinlik = 0;
    while (i < b.length) {
      const t = b[i++];
      if (t.tur === 'ac' && t.ad === ad) derinlik++;
      if (t.tur === 'kapa' && t.ad === ad) {
        if (derinlik === 0) break;
        derinlik--;
      }
      ic.push(t);
    }
    return ic;
  };

  const uyariYap = (anahtar) => (mesaj) => {
    const kayit = { dosya: dosyaAdi, anahtar, mesaj };
    if (!uyarilar.some((u) => u.dosya === kayit.dosya && u.anahtar === anahtar && u.mesaj === mesaj)) {
      uyarilar.push(kayit);
    }
  };

  const degerUret = (ic, anahtar) => {
    const uyar = uyariYap(anahtar);
    const ham = icerigiTopla(ic, uyar);
    const android = androidDegeriCoz(ham, uyar);
    return { android, ham };
  };

  while (i < b.length) {
    const t = b[i++];
    if (t.tur === 'tek' && t.ad === 'string' && t.nitelik.name) {
      hedef.metinler.push({ ad: t.nitelik.name, android: '', ham: '', dosya: dosyaAdi });
    } else if (t.tur === 'ac' && t.ad === 'string' && t.nitelik.name) {
      const ic = iciAl('string');
      const d = degerUret(ic, t.nitelik.name);
      hedef.metinler.push({
        ad: t.nitelik.name,
        android: d.android,
        ham: d.ham,
        dosya: dosyaAdi,
        cevrilmez: t.nitelik.translatable === 'false',
      });
    } else if (t.tur === 'ac' && (t.ad === 'string-array' || t.ad === 'plurals') && t.nitelik.name) {
      const ad = t.nitelik.name;
      const tur = t.ad;
      const ic = iciAl(tur);
      const ogeler = [];
      let k = 0;
      while (k < ic.length) {
        const o = ic[k++];
        if (o.tur === 'tek' && o.ad === 'item') {
          ogeler.push({ nicelik: o.nitelik.quantity, android: '', ham: '' });
        } else if (o.tur === 'ac' && o.ad === 'item') {
          const oic = [];
          while (k < ic.length && !(ic[k].tur === 'kapa' && ic[k].ad === 'item')) oic.push(ic[k++]);
          k++;
          const d = degerUret(oic, ad);
          ogeler.push({ nicelik: o.nitelik.quantity, android: d.android, ham: d.ham });
        }
      }
      if (tur === 'string-array') hedef.diziler.push({ ad, ogeler, dosya: dosyaAdi });
      else hedef.cogullar.push({ ad, ogeler, dosya: dosyaAdi });
    }
  }
}

// values, values-tr, values-en-rGB, values-b+sr+Latn -> dil kodu; dil olmayan niteleyiciler -> null
function klasordenDil(ad) {
  if (ad === 'values') return VARSAYILAN_DIL;
  let m = /^values-([a-z]{2,3})(?:-r([A-Z]{2}))?$/.exec(ad);
  if (m) {
    const eski = { in: 'id', iw: 'he', ji: 'yi' };
    const dil = eski[m[1]] || m[1];
    return m[2] ? dil + '-' + m[2] : dil;
  }
  m = /^values-b\+([A-Za-z0-9+]+)$/.exec(ad);
  if (m) return m[1].split('+').join('-');
  return null;
}

// ---------------------------------------------------------------------------
// .strings yazma / okuma
// ---------------------------------------------------------------------------

function stringsKacis(s) {
  return s
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\n/g, '\\n')
    .replace(/\r/g, '\\r')
    .replace(/\t/g, '\\t');
}

function stringsUret(dil, girdiler) {
  const satirlar = [
    '/* Bu dosya üretilmiştir; elle düzenlemeyin.',
    '   Kaynak: app/src/main/res/' + (dil === VARSAYILAN_DIL ? 'values' : 'values-' + dil) + '/ (Android metinleri)',
    '   Üretici: node ios/tools/metin-donustur/donustur.js */',
    '',
  ];
  for (const g of girdiler) {
    satirlar.push('"' + stringsKacis(g.anahtar) + '" = "' + stringsKacis(g.deger) + '";');
  }
  return satirlar.join('\n') + '\n';
}

// Doğrulama için bağımsız .strings ayrıştırıcısı (yorumlar, kaçışlar, "a" = "b";).
function stringsOku(metin) {
  const sonuc = [];
  let i = 0;
  const n = metin.length;
  const bosluklariGec = () => {
    for (;;) {
      while (i < n && /\s/.test(metin[i])) i++;
      if (metin.startsWith('/*', i)) {
        const j = metin.indexOf('*/', i + 2);
        i = j < 0 ? n : j + 2;
      } else if (metin.startsWith('//', i)) {
        const j = metin.indexOf('\n', i);
        i = j < 0 ? n : j + 1;
      } else {
        return;
      }
    }
  };
  const tirnakliOku = () => {
    if (metin[i] !== '"') throw new Error('konum ' + i + ': " bekleniyordu');
    i++;
    let s = '';
    while (i < n && metin[i] !== '"') {
      if (metin[i] === '\\') {
        const c = metin[i + 1];
        i += 2;
        if (c === 'n') s += '\n';
        else if (c === 't') s += '\t';
        else if (c === 'r') s += '\r';
        else if (c === 'U' || c === 'u') {
          s += String.fromCharCode(parseInt(metin.slice(i, i + 4), 16));
          i += 4;
        } else s += c;
      } else {
        s += metin[i++];
      }
    }
    if (i >= n) throw new Error('kapanmamis tirnak');
    i++;
    return s;
  };
  for (;;) {
    bosluklariGec();
    if (i >= n) break;
    const anahtar = tirnakliOku();
    bosluklariGec();
    if (metin[i] !== '=') throw new Error('"' + anahtar + '": = bekleniyordu');
    i++;
    bosluklariGec();
    const deger = tirnakliOku();
    bosluklariGec();
    if (metin[i] !== ';') throw new Error('"' + anahtar + '": ; bekleniyordu');
    i++;
    sonuc.push({ anahtar, deger });
  }
  return sonuc;
}

// ---------------------------------------------------------------------------
// Ana akış
// ---------------------------------------------------------------------------

function main() {
  if (!fs.existsSync(RES)) {
    console.error('Android res klasoru bulunamadi: ' + RES);
    process.exit(2);
  }

  const rapor = { varsayilanDil: VARSAYILAN_DIL, diller: {}, uyarilar: [], hatalar: [] };
  let degisiklik = false;

  const klasorler = fs
    .readdirSync(RES)
    .filter((k) => /^values(-|$)/.test(k))
    .sort();

  for (const klasor of klasorler) {
    const dil = klasordenDil(klasor);
    if (!dil) continue;
    const hedef = { metinler: [], diziler: [], cogullar: [] };
    const uyarilar = [];
    const dosyalar = fs
      .readdirSync(path.join(RES, klasor))
      .filter((d) => d.endsWith('.xml'))
      .sort();
    for (const d of dosyalar) dosyayiOku(path.join(RES, klasor, d), hedef, uyarilar);
    if (hedef.metinler.length + hedef.diziler.length + hedef.cogullar.length === 0) continue;

    // @string/x başvurularını çöz (tam değer başvuruysa).
    const adaGore = new Map(hedef.metinler.map((m) => [m.ad, m]));
    for (const m of hedef.metinler) {
      let tur = 0;
      let r;
      while ((r = /^@string\/([\w.]+)$/.exec(m.android)) !== null && adaGore.has(r[1]) && tur++ < 10) {
        m.android = adaGore.get(r[1]).android;
      }
    }

    const girdiler = [];
    const ekle = (anahtar, android, dosya) => {
      const uyar = (mesaj) => uyarilar.push({ dosya, anahtar, mesaj });
      girdiler.push({ anahtar, android, deger: bicimCevir(android, uyar) });
    };
    for (const m of hedef.metinler) ekle(m.ad, m.android, m.dosya);
    for (const d of hedef.diziler) {
      girdiler.push({ anahtar: d.ad + '#adet', android: String(d.ogeler.length), deger: String(d.ogeler.length), turetilmis: true });
      d.ogeler.forEach((o, s) => ekle(d.ad + '#' + s, o.android, d.dosya));
    }
    for (const c of hedef.cogullar) {
      for (const o of c.ogeler) ekle(c.ad + '#' + (o.nicelik || 'other'), o.android, c.dosya);
    }

    // Yinelenen anahtar denetimi
    const gorulen = new Set();
    for (const g of girdiler) {
      if (gorulen.has(g.anahtar)) rapor.hatalar.push(dil + ': yinelenen anahtar ' + g.anahtar);
      gorulen.add(g.anahtar);
    }

    const icerik = stringsUret(dil, girdiler);
    const ciktiYolu = path.join(CIKTI_KOK, dil + '.lproj', 'Localizable.strings');

    // --- Doğrulama: üretilen metni yeniden ayrıştır ve kaynağa karşı denetle ---
    const geri = stringsOku(icerik);
    if (geri.length !== girdiler.length) {
      rapor.hatalar.push(dil + ': anahtar sayisi tutmuyor (' + geri.length + ' / ' + girdiler.length + ')');
    }
    const geriHarita = new Map(geri.map((g) => [g.anahtar, g.deger]));
    for (const g of girdiler) {
      const okunan = geriHarita.get(g.anahtar);
      if (okunan !== g.deger) rapor.hatalar.push(dil + ': gidis-donus farki ' + g.anahtar);
      if (!g.turetilmis && bicimImzasi(g.android, false) !== bicimImzasi(g.deger, true)) {
        rapor.hatalar.push(
          dil + ': bicim belirteci farki ' + g.anahtar + ' [' + bicimImzasi(g.android, false) + '] / [' + bicimImzasi(g.deger, true) + ']'
        );
      }
    }
    // Kaynaktaki <string> sayısını ayrıştırıcıdan bağımsız olarak say (yorumlar çıkarılıp).
    let bagimsizSayim = 0;
    for (const d of dosyalar) {
      const xml = fs.readFileSync(path.join(RES, klasor, d), 'utf8').replace(/<!--[\s\S]*?-->/g, '');
      bagimsizSayim += (xml.match(/<string\s[^>]*name\s*=/g) || []).length;
    }
    if (bagimsizSayim !== hedef.metinler.length) {
      rapor.hatalar.push(dil + ': <string> sayimi tutmuyor (duzenli ifade ' + bagimsizSayim + ' / ayristirici ' + hedef.metinler.length + ')');
    }

    const bicimli = girdiler.filter((g) => !g.turetilmis && bicimImzasi(g.deger, true) !== '');
    rapor.diller[dil] = {
      kaynak: 'app/src/main/res/' + klasor,
      cikti: path.relative(DEPO, ciktiYolu).split(path.sep).join('/'),
      metin: hedef.metinler.length,
      dizi: hedef.diziler.length,
      cogul: hedef.cogullar.length,
      toplamAnahtar: girdiler.length,
      bicimliAnahtar: bicimli.length,
      cevrilmezIsaretli: hedef.metinler.filter((m) => m.cevrilmez).map((m) => m.ad),
      bosDeger: girdiler.filter((g) => g.deger === '').map((g) => g.anahtar),
    };
    for (const u of uyarilar) rapor.uyarilar.push(Object.assign({ dil }, u));

    const eski = fs.existsSync(ciktiYolu) ? fs.readFileSync(ciktiYolu, 'utf8') : null;
    if (eski !== icerik) {
      degisiklik = true;
      if (!YALNIZ_KONTROL) {
        fs.mkdirSync(path.dirname(ciktiYolu), { recursive: true });
        fs.writeFileSync(ciktiYolu, icerik, 'utf8');
      }
    }
    // Diskteki dosyayı da (yazıldıysa) yeniden okuyup ayrıştır.
    if (fs.existsSync(ciktiYolu)) {
      const disk = stringsOku(fs.readFileSync(ciktiYolu, 'utf8'));
      if (!YALNIZ_KONTROL && disk.length !== girdiler.length) {
        rapor.hatalar.push(dil + ': diskteki dosyada anahtar sayisi tutmuyor');
      }
    }

    console.log(
      dil + '.lproj: ' + girdiler.length + ' anahtar (' + hedef.metinler.length + ' metin, ' + hedef.diziler.length + ' dizi, ' +
        hedef.cogullar.length + ' cogul), ' + bicimli.length + ' bicimli'
    );
  }

  if (!YALNIZ_KONTROL) fs.writeFileSync(RAPOR_YOLU, JSON.stringify(rapor, null, 2) + '\n', 'utf8');

  for (const u of rapor.uyarilar) console.log('UYARI ' + u.dil + ' ' + u.anahtar + ': ' + u.mesaj);
  for (const h of rapor.hatalar) console.error('HATA ' + h);
  if (rapor.hatalar.length > 0) process.exit(1);
  if (YALNIZ_KONTROL && degisiklik) {
    console.error('Diskteki .strings dosyalari guncel degil; donustur.js\'i yeniden calistirin.');
    process.exit(1);
  }
  console.log('Tamam.');
}

if (require.main === module) main();

module.exports = { belirtecle, androidDegeriCoz, bicimCevir, bicimImzasi, stringsKacis, stringsOku, klasordenDil };

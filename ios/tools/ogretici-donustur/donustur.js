#!/usr/bin/env node
// Öğretici içerik dönüştürücüsü: Android TutorialFragment.kt içindeki createTutorialSteps<N>()
// adım listelerini okuyup ortak Kotlin koduna (ios/shared/.../ogretici/icerik/Ogretici<N>.kt)
// veri olarak yazar. Ayrıntı: ios/docs/OGRETICI_PLANI.md.
//
// Kullanım (depo kökünden):
//   node ios/tools/ogretici-donustur/donustur.js 1            → Ogretici1.kt yazılır
//   node ios/tools/ogretici-donustur/donustur.js 3 4 5        → birkaçı birden
//   node ios/tools/ogretici-donustur/donustur.js --rapor      → hiçbir şey yazmaz; bütün öğreticiler
//                                                              için adım sayısı ve uyarılar
//   node ios/tools/ogretici-donustur/donustur.js 3 --kuru     → yazmadan ekrana basar
//
// Betik Android dosyasını DEĞİŞTİRMEZ. Tanımadığı bir yapı görürse adımın başına `// UYARI:`
// yorumu koyar ve çıkış kodunu 2 yapar; o adımı elle gözden geçirin.

'use strict';
const fs = require('fs');
const path = require('path');

const KOK = path.resolve(__dirname, '..', '..', '..');
const KAYNAK = path.join(KOK, 'app', 'src', 'main', 'java', 'com', 'example', 'app', 'TutorialFragment.kt');
const YERLESIM = path.join(KOK, 'app', 'src', 'main', 'res', 'layout', 'fragment_tutorial.xml');
const OLCULER = path.join(KOK, 'app', 'src', 'main', 'res', 'values', 'dimens.xml');
const HEDEF = path.join(KOK, 'ios', 'shared', 'src', 'commonMain', 'kotlin', 'com', 'sorobit', 'shared', 'ogretici', 'icerik');

// ---------------------------------------------------------------------------------------------
// Yardımcılar: metin taraması (dizgi ve karakter sabitlerinin içindeki parantezler sayılmaz)
// ---------------------------------------------------------------------------------------------

/** `metin[bas]` bir açma parantezi; eşinin konumunu döndürür. */
function esiniBul(metin, bas) {
  const acan = metin[bas];
  const kapanan = { '(': ')', '{': '}', '[': ']' }[acan];
  let derinlik = 0;
  for (let i = bas; i < metin.length; i++) {
    const c = metin[i];
    if (c === '"') { i = dizgiSonu(metin, i); continue; }
    if (c === '\'') { i = metin.indexOf('\'', i + 2); continue; }
    if (c === '/' && metin[i + 1] === '/') { i = metin.indexOf('\n', i); if (i < 0) return -1; continue; }
    if (c === '/' && metin[i + 1] === '*') { i = metin.indexOf('*/', i) + 1; continue; }
    if (c === '(' || c === '{' || c === '[') derinlik++;
    if (c === ')' || c === '}' || c === ']') {
      derinlik--;
      if (derinlik === 0) {
        if (c !== kapanan) throw new Error('Parantez eşleşmedi: ' + bas);
        return i;
      }
    }
  }
  return -1;
}

function dizgiSonu(metin, bas) {
  if (metin.startsWith('"""', bas)) return metin.indexOf('"""', bas + 3) + 2;
  for (let i = bas + 1; i < metin.length; i++) {
    if (metin[i] === '\\') { i++; continue; }
    if (metin[i] === '"') return i;
  }
  return metin.length;
}

/** Üst düzey virgüllerden böler (parantez / dizgi içindekiler sayılmaz). */
function bol(metin) {
  const parcalar = [];
  let derinlik = 0;
  let bas = 0;
  for (let i = 0; i < metin.length; i++) {
    const c = metin[i];
    if (c === '"') { i = dizgiSonu(metin, i); continue; }
    if (c === '/' && metin[i + 1] === '/') { const s = metin.indexOf('\n', i); i = s < 0 ? metin.length : s; continue; }
    if (c === '(' || c === '{' || c === '[') derinlik++;
    else if (c === ')' || c === '}' || c === ']') derinlik--;
    else if (c === ',' && derinlik === 0) { parcalar.push(metin.slice(bas, i)); bas = i + 1; }
  }
  parcalar.push(metin.slice(bas));
  return parcalar.map(yorumsuz).map(s => s.trim()).filter(s => s.length > 0);
}

function yorumsuz(s) {
  // Satır sonu yorumlarını at (dizgi içindekilere dokunmadan).
  let cikti = '';
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (c === '"') { const son = dizgiSonu(s, i); cikti += s.slice(i, son + 1); i = son; continue; }
    if (c === '/' && s[i + 1] === '/') { const son = s.indexOf('\n', i); if (son < 0) break; i = son - 1; continue; }
    cikti += c;
  }
  return cikti;
}

/** `ad(` ile başlayan çağrının argüman metni; değilse null. */
function cagriArgumanlari(ifade, ad) {
  const t = ifade.trim();
  if (!t.startsWith(ad + '(')) return null;
  const bas = ad.length;
  const son = esiniBul(t, bas);
  if (son !== t.length - 1) return null;
  return t.slice(bas + 1, son);
}

function kotlinDizgi(s) {
  return '"' + s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$').replace(/\n/g, '\\n') + '"';
}

function dizgiCoz(ifade) {
  const t = ifade.trim();
  if (!t.startsWith('"') || dizgiSonu(t, 0) !== t.length - 1) return null;
  return JSON.parse(t.replace(/\\\$/g, '$'));
}

// ---------------------------------------------------------------------------------------------
// Android kaynakları
// ---------------------------------------------------------------------------------------------

const kaynak = fs.readFileSync(KAYNAK, 'utf8');
const satirBaslari = [0];
for (let i = 0; i < kaynak.length; i++) if (kaynak[i] === '\n') satirBaslari.push(i + 1);
function satirNo(konum) {
  let alt = 0, ust = satirBaslari.length - 1;
  while (alt < ust) { const orta = (alt + ust + 1) >> 1; if (satirBaslari[orta] <= konum) alt = orta; else ust = orta - 1; }
  return alt + 1;
}

// dimens.xml: tutorial_* ölçüleri (dp).
const olculer = {};
for (const m of fs.readFileSync(OLCULER, 'utf8').matchAll(/<dimen name="(\w+)">([\d.]+)dp<\/dimen>/g)) olculer[m[1]] = parseFloat(m[2]);
// focusMarginRightPx(k) = 19dp + tutorial_focus_margin_right_step * k (TutorialFragment.focusMarginRightPx).
const ODAK_SAG_ADIM = olculer['tutorial_focus_margin_right_step'];

// Yerleşimdeki kimlikler: view binding adı (camelCase) → XML kimliği.
const bagAdlari = {};
for (const m of fs.readFileSync(YERLESIM, 'utf8').matchAll(/android:id="@\+id\/(\w+)"/g)) {
  const id = m[1];
  const bag = id.replace(/_([a-zA-Z0-9])/g, (_, h) => h.toUpperCase());
  bagAdlari[bag] = id;
}

// ---------------------------------------------------------------------------------------------
// Değer çözümleyiciler
// ---------------------------------------------------------------------------------------------

const GORUNURLUK = { 'View.VISIBLE': 'GORUNUR', 'View.INVISIBLE': 'GORUNMEZ', 'View.GONE': 'YOK' };

function metinAnahtari(ifade, uyar) {
  const ic = cagriArgumanlari(ifade, 'getString');
  if (ic !== null) {
    const m = ic.trim().match(/^R\.string\.(\w+)$/);
    if (m) return m[1];
    uyar('biçimli getString: ' + ifade.trim());
    return null;
  }
  const duz = dizgiCoz(ifade);
  if (duz !== null) { uyar('düz metin (strings.xml anahtarı değil): ' + ifade.trim()); return null; }
  uyar('tanınmayan metin: ' + ifade.trim().slice(0, 80));
  return null;
}

function gorunurlukCoz(ifade, uyar) {
  const t = ifade.trim().replace(/\s+/g, ' ');
  if (GORUNURLUK[t]) return GORUNURLUK[t];
  if (t === 'if (isFirstRunTutorial) View.INVISIBLE else View.VISIBLE') return 'ILK_ACILISTA_GORUNMEZ';
  uyar('tanınmayan görünürlük: ' + t);
  return null;
}

function gorunumKimligi(ifade, uyar) {
  const t = ifade.trim();
  if (t === 'focusView') return 'focusView';
  const m = t.match(/^binding\.(\w+)$/);
  if (m) {
    if (bagAdlari[m[1]]) return bagAdlari[m[1]];
    uyar('yerleşimde olmayan görünüm: ' + t);
    return m[1];
  }
  const r = t.match(/^R\.id\.(\w+)$/);
  if (r) return r[1];
  uyar('tanınmayan görünüm: ' + t);
  return null;
}

/** Odak sağ boşluğu (dp). Android: 0 → focusMarginRightPx(0); 1 → 1 px (≈ 0 dp). */
function sagBoslukCoz(ifade, uyar) {
  const t = ifade.trim();
  let m = t.match(/^focusMarginRightPx\((\d+)\)$/);
  if (m) return 19 + ODAK_SAG_ADIM * parseInt(m[1], 10);
  if (t === '0') return 19;
  if (t === '1') return 0;
  m = t.match(/^dpToPx\((-?\d+)\)$/);
  if (m) return parseInt(m[1], 10);
  if (t.startsWith('(focusView.layoutParams')) return null; // "şu anki" (from…) değerler
  uyar('tanınmayan sağ boşluk: ' + t);
  return null;
}

/** Odak boyu (dp). `ONCEKI`: Android'in sizeHistory'den geri aldığı önceki boy. */
function boyCoz(ifade, uyar) {
  const t = ifade.trim();
  let m = t.match(/^focusDimenPx\(R\.dimen\.(\w+)\)$/);
  if (m) {
    if (olculer[m[1]] === undefined) { uyar('dimens.xml\'de yok: ' + m[1]); return null; }
    return olculer[m[1]];
  }
  m = t.match(/^dpToPx\((-?\d+)\)$/);
  if (m) return parseInt(m[1], 10);
  if (t === 'prevWidth' || t === 'prevHeight') return 'ONCEKI';
  uyar('tanınmayan boy: ' + t);
  return null;
}

function adliArgumanlar(argMetni) {
  const sonuc = { konumsal: [], adli: {} };
  for (const p of bol(argMetni)) {
    const m = p.match(/^(\w+)\s*=(?!=)\s*([\s\S]*)$/);
    if (m) sonuc.adli[m[1]] = m[2].trim();
    else sonuc.konumsal.push(p);
  }
  return sonuc;
}

/** widgetOperations içindeki tek bir lambda: `{ WidgetOperation.X(...) }`. */
function islemCoz(lambda, adim, uyar) {
  let govde = lambda.trim();
  if (govde.startsWith('{') && govde.endsWith('}')) govde = govde.slice(1, -1).trim();
  const m = govde.match(/^WidgetOperation\.(\w+)\(/);
  if (!m) { uyar('tanınmayan widget işlemi: ' + govde.slice(0, 80)); return; }
  const tur = m[1];
  const args = adliArgumanlar(cagriArgumanlari(govde, 'WidgetOperation.' + tur) || '');
  const gorunum = gorunumKimligi(args.adli.view || args.konumsal[0] || '', uyar);
  if (tur === 'ChangeVisibility') {
    const g = gorunurlukCoz(args.adli.visibility || args.konumsal[1] || '', uyar);
    if (gorunum && g) adim.gorunurlukler.push([gorunum, g]);
    return;
  }
  if (gorunum !== 'focusView') {
    if (tur === 'ChangeConstraints') {
      // Odak dışı yerleşim değişikliği (ör. 24: tutorialText'i kör görselin üstüne bağlar).
      const bag = Object.entries(args.adli).filter(([k]) => k !== 'view').map(([k, v]) => k + '=' + v.replace(/^R\.id\./, '').replace('ConstraintLayout.LayoutParams.UNSET', 'UNSET'));
      adim.yerlesimler.push([gorunum, bag.join(';')]);
      return;
    }
    uyar(tur + ' odak dışı görünümde: ' + gorunum);
    return;
  }
  const odak = { sag: null, genislik: null, yukseklik: null, capa: null, ust: null };
  if (tur === 'AnimateMargin') {
    odak.sag = sagBoslukCoz(args.adli.toMarginRight || '', uyar);
    if ((args.adli.toMarginLeft || '0').trim() !== '0') uyar('sol boşluk destekli değil: ' + args.adli.toMarginLeft);
  } else if (tur === 'ChangeMargin') {
    const k = args.konumsal;
    odak.sag = sagBoslukCoz(args.adli.marginRight || k[1] || '0', uyar);
    if ((args.adli.marginLeft || k[2] || '0').trim() !== '0') uyar('sol boşluk destekli değil');
    const ust = (args.adli.marginTop || k[3] || 'null').trim();
    if (ust !== 'null') {
      const mm = ust.match(/^(-?)dpToPx\((-?\d+)\)$/);
      if (mm) odak.ust = (mm[1] === '-' ? -1 : 1) * parseInt(mm[2], 10); else uyar('tanınmayan üst boşluk: ' + ust);
    }
    if ((args.adli.marginBottom || k[4] || 'null').trim() !== 'null') uyar('alt boşluk destekli değil');
  } else if (tur === 'AnimateSize') {
    odak.genislik = boyCoz(args.adli.toWidth || '', uyar);
    odak.yukseklik = boyCoz(args.adli.toHeight || '', uyar);
  } else if (tur === 'ChangeConstraints') {
    const ust = (args.adli.topToTop || '').trim();
    if (ust === 'R.id.guideline') odak.capa = 'CAPA_UST_KILAVUZ';
    else if (ust === 'R.id.guideline2') odak.capa = 'CAPA_ALT_KILAVUZ';
    else if (ust === 'R.id.abacusContainer') odak.capa = 'CAPA_ORTA';
    else uyar('tanınmayan odak bağı: ' + JSON.stringify(args.adli));
    for (const k of Object.keys(args.adli)) {
      if (k === 'view' || k === 'topToTop') continue;
      if (k === 'bottomToBottom' && /UNSET/.test(args.adli[k])) continue;
      if (k === 'bottomToBottom' && args.adli[k].trim() === 'R.id.abacusContainer') continue;
      uyar('odak bağında desteklenmeyen alan: ' + k + '=' + args.adli[k]);
    }
  } else {
    uyar('tanınmayan odak işlemi: ' + tur);
    return;
  }
  adim.odak.push(odak);
}

function listeOgeleri(ifade, uyar, ad) {
  const t = ifade.trim().replace(/^listOf\s+([({])/, 'listOf$1');
  // `listOf { ... }`: sondaki lambda sözdizimi, tek elemanlı liste.
  if (t.startsWith('listOf{') && esiniBul(t, 6) === t.length - 1) return [t.slice(6)];
  const ic = cagriArgumanlari(t, 'listOf');
  if (ic === null) { uyar(ad + ' listOf değil: ' + ifade.trim().slice(0, 60)); return []; }
  return bol(ic);
}

function renkCoz(ifade, uyar) {
  const t = ifade.trim();
  let m = t.match(/^Color\.parseColor\("#([0-9A-Fa-f]{6})"\)$/);
  if (m) return '0xFF' + m[1].toUpperCase();
  const sabit = { 'Color.YELLOW': '0xFFFFFF00', 'Color.BLUE': '0xFF0000FF', 'Color.WHITE': '0xFFFFFFFF', 'Color.RED': '0xFFFF0000', 'Color.GREEN': '0xFF00FF00' };
  if (sabit[t]) return sabit[t];
  uyar('tanınmayan renk: ' + t);
  return null;
}

// ---------------------------------------------------------------------------------------------
// Adım çözümleyici
// ---------------------------------------------------------------------------------------------

const YOK_SAYILAN_ONSTEP = /^\{\s*view\s*->\s*sizeHistory\.add\(Pair\(view\.width,\s*view\.height\)\)\s*(Log\.d\([^)]*\)\s*)?\}$/;

function adimCoz(argMetni, satir) {
  const uyarilar = [];
  const uyar = (m) => uyarilar.push(m);
  const a = adliArgumanlar(argMetni);
  const adim = {
    satir, uyarilar, metin: null, ses: null, daktilo: false, daktiloHizi: 50,
    hareketler: [], gorunurlukler: [], odak: [], yerlesimler: [],
    alanlar: {},
  };
  const metinIfadesi = a.adli.text || a.konumsal[0];
  if (!metinIfadesi) uyar('metin yok'); else adim.metin = metinAnahtari(metinIfadesi, uyar);
  if (a.konumsal.length > 1) uyar('fazla konumsal argüman');

  for (const [ad, deger] of Object.entries(a.adli)) {
    switch (ad) {
      case 'text': break;
      case 'animation':
        for (const o of listeOgeleri(deger, uyar, 'animation')) {
          const m = o.match(/^BeadAnimation\(this,\s*"rod(\d)_bead_(top|bottom(\d))",\s*(\d)\)$/);
          if (!m) { uyar('tanınmayan boncuk animasyonu: ' + o); continue; }
          const ust = m[2] === 'top';
          adim.hareketler.push({ cubuk: +m[1], ust, sira: ust ? 0 : (+m[3] - 1), tur: +m[4] });
        }
        break;
      case 'widgetOperations':
        for (const o of listeOgeleri(deger, uyar, 'widgetOperations')) islemCoz(o, adim, uyar);
        break;
      case 'widgetVisibilityMap': {
        const ic = cagriArgumanlari(deger, 'mapOf');
        if (ic === null) { uyar('widgetVisibilityMap mapOf değil'); break; }
        for (const o of bol(ic)) {
          const m = o.match(/^R\.id\.(\w+)\s+to\s+(View\.\w+)$/);
          if (!m) { uyar('tanınmayan görünürlük eşlemi: ' + o); continue; }
          adim.gorunurlukler.push([m[1], GORUNURLUK[m[2]]]);
        }
        break;
      }
      case 'onStep':
        if (!YOK_SAYILAN_ONSTEP.test(deger.replace(/\s+/g, ' ').trim())) uyar('onStep yok sayılamadı: ' + deger.replace(/\s+/g, ' ').slice(0, 100));
        break;
      case 'onStepComplete':
        // Android'de tek kullanım (14): `{ resetAbacus("...") }` — adıma ileri gelinince sıfırla.
        if (/^\{\s*resetAbacus\("[^"]*"\)\s*\}$/.test(deger.trim())) adim.alanlar.gelinceSifirla = 'true';
        else uyar('onStepComplete elle taşınmalı: ' + deger.replace(/\s+/g, ' ').slice(0, 100));
        break;
      case 'questionText': {
        const d = dizgiCoz(deger);
        if (d !== null) adim.alanlar.soru = kotlinDizgi(d);
        else uyar('soru metni düz dizgi değil: ' + deger);
        break;
      }
      case 'questionTextVisibility': {
        const g = gorunurlukCoz(deger, uyar);
        if (g) adim.alanlar.soruGorunurlugu = g;
        break;
      }
      case 'questionTextColorPositions': {
        const ogeler = [];
        for (const o of listeOgeleri(deger, uyar, 'questionTextColorPositions')) {
          const m = o.match(/^(\d+)\s+to\s+([\s\S]+)$/);
          if (!m) { uyar('tanınmayan renk konumu: ' + o); continue; }
          const r = renkCoz(m[2], uyar);
          if (r) ogeler.push('OgreticiRenkliHarf(' + m[1] + ', ' + r + ')');
        }
        adim.alanlar.soruRenkleri = 'listOf(' + ogeler.join(', ') + ')';
        break;
      }
      case 'nextStepAvailable': adim.alanlar.ilerlenebilir = mantiksal(deger, uyar); break;
      case 'abacusClickable': adim.alanlar.abakusTiklanabilir = mantiksal(deger, uyar); break;
      case 'answerNumber': adim.alanlar.cevap = tamsayi(deger, uyar); break;
      case 'backAnswerNumber': adim.alanlar.geriCevap = tamsayi(deger, uyar); break;
      case 'abacusReset': adim.alanlar.abakusSifirla = mantiksal(deger, uyar); break;
      case 'nextStepAbacusReset': adim.alanlar.sonrakindeSifirla = mantiksal(deger, uyar); break;
      case 'resetAndWaith': adim.alanlar.sifirlaVeBekle = mantiksal(deger, uyar); break;
      case 'backAnimationOff': adim.alanlar.geriAnimasyonuYok = mantiksal(deger, uyar); break;
      case 'rulesPanelVisibility': {
        const g = gorunurlukCoz(deger, uyar);
        if (g) adim.alanlar.kuralTablosuGorunurlugu = g;
        break;
      }
      case 'rulesStepTextVisibility': {
        const g = gorunurlukCoz(deger, uyar);
        if (g) adim.alanlar.kuralMetniGorunurlugu = g;
        break;
      }
      case 'rulesStepTextTopToBottomOf': {
        const m = deger.trim().match(/^R\.id\.(\w+)$/);
        if (m) adim.alanlar.kuralMetniCapasi = kotlinDizgi(m[1]); else uyar('tanınmayan kural metni bağı: ' + deger);
        break;
      }
      case 'rulesStepTextContent': {
        const k = metinAnahtari(deger, uyar);
        if (k) adim.alanlar.kuralMetni = kotlinDizgi(k);
        break;
      }
      case 'soundResource': {
        const m = deger.trim().match(/^R\.raw\.(\w+)$/);
        if (m) adim.ses = m[1]; else uyar('tanınmayan ses: ' + deger);
        break;
      }
      case 'soundStoragePath': {
        const d = dizgiCoz(deger);
        if (d !== null) adim.alanlar.uzakSes = kotlinDizgi(d); else uyar('tanınmayan uzak ses: ' + deger);
        break;
      }
      case 'useTypewriterEffect': adim.daktilo = mantiksal(deger, uyar) === 'true'; break;
      case 'typewriterSpeed': {
        const m = deger.trim().match(/^(\d+)L?$/);
        if (m) adim.daktiloHizi = +m[1]; else uyar('tanınmayan daktilo hızı: ' + deger);
        break;
      }
      case 'requestText': {
        const k = metinAnahtari(deger, uyar);
        if (k) adim.alanlar.ipucu = kotlinDizgi(k);
        break;
      }
      case 'options': {
        const anahtarlar = [];
        for (const o of listeOgeleri(deger, uyar, 'options')) {
          const k = metinAnahtari(o, uyar);
          if (k) anahtarlar.push(kotlinDizgi(k));
        }
        adim.alanlar.secenekler = 'listOf(' + anahtarlar.join(', ') + ')';
        break;
      }
      case 'correctOptionIndex': {
        const ogeler = listeOgeleri(deger, uyar, 'correctOptionIndex').map(o => tamsayi(o, uyar));
        adim.alanlar.dogruSecenekler = 'listOf(' + ogeler.join(', ') + ')';
        break;
      }
      case 'multipleChoice': adim.alanlar.cokluSecim = mantiksal(deger, uyar); break;
      case 'optionText': {
        const k = metinAnahtari(deger, uyar);
        if (k) adim.alanlar.secenekBasligi = kotlinDizgi(k);
        break;
      }
      case 'id': {
        const d = dizgiCoz(deger);
        if (d !== null) adim.alanlar.kimlik = kotlinDizgi(d); else uyar('tanınmayan kimlik: ' + deger);
        break;
      }
      default:
        uyar('tanınmayan alan: ' + ad);
    }
  }
  return adim;
}

function mantiksal(d, uyar) {
  const t = d.trim();
  if (t === 'true' || t === 'false') return t;
  uyar('mantıksal değer değil: ' + t);
  return 'false';
}

function tamsayi(d, uyar) {
  const t = d.trim();
  if (/^-?\d+$/.test(t)) return t;
  uyar('tamsayı değil: ' + t);
  return '0';
}

// ---------------------------------------------------------------------------------------------
// Öğretici bulma ve Kotlin üretimi
// ---------------------------------------------------------------------------------------------

/** Android: TutorialFragment.showStep içindeki `when (tutorialNumber)` (kural tablosu). */
function kuralTablolari() {
  const bas = kaynak.indexOf('updateRulesStepText(step)\n        when (tutorialNumber) {');
  const son = kaynak.indexOf('updateTutorialTextAnchor()', bas);
  const tablo = {};
  for (const m of kaynak.slice(bas, son).matchAll(/(\d+)\s*->\s*binding\.(\w+)\.visibility = step\.rulesPanelVisibility/g)) {
    tablo[m[1]] = bagAdlari[m[2]] || m[2];
  }
  return tablo;
}

function ogreticiNumaralari() {
  const bas = kaynak.indexOf('currentTutorialSteps = when (tutorialNumber) {');
  const son = kaynak.indexOf('else ->', bas);
  return [...kaynak.slice(bas, son).matchAll(/(\d+)\s*->\s*tutorialSteps\d*/g)].map(m => +m[1]);
}

function ogreticiCoz(no) {
  const imza = `private fun createTutorialSteps${no === 1 ? '1' : no}(){`;
  const bas = kaynak.indexOf(imza);
  if (bas < 0) throw new Error('Bulunamadı: ' + imza);
  const listeBas = kaynak.indexOf('listOf(', bas);
  const parantez = listeBas + 'listOf'.length;
  const listeSon = esiniBul(kaynak, parantez);
  const govdeSon = esiniBul(kaynak, kaynak.indexOf('{', bas));
  const adimlar = [];
  let i = parantez + 1;
  while (i < listeSon) {
    const k = kaynak.indexOf('TutorialStep(', i);
    if (k < 0 || k > listeSon) break;
    const ac = k + 'TutorialStep'.length;
    const kapa = esiniBul(kaynak, ac);
    adimlar.push(adimCoz(kaynak.slice(ac + 1, kapa), satirNo(k)));
    i = kapa + 1;
  }
  return { no, adimlar, satirBas: satirNo(bas), satirSon: satirNo(govdeSon) };
}

const ALAN_SIRASI = [
  'kimlik', 'soru', 'soruGorunurlugu', 'soruRenkleri', 'ilerlenebilir', 'abakusTiklanabilir', 'cevap', 'geriCevap',
  'abakusSifirla', 'sonrakindeSifirla', 'sifirlaVeBekle', 'gelinceSifirla', 'geriAnimasyonuYok', 'ipucu',
  'kuralTablosuGorunurlugu', 'kuralMetni', 'kuralMetniGorunurlugu', 'kuralMetniCapasi',
  'secenekler', 'dogruSecenekler', 'cokluSecim', 'secenekBasligi', 'uzakSes',
];

function kotlinUret(og, tablo) {
  const s = [];
  s.push('// OTOMATİK ÜRETİLDİ: node ios/tools/ogretici-donustur/donustur.js ' + og.no);
  s.push('// Kaynak: app/src/main/java/com/example/app/TutorialFragment.kt, createTutorialSteps' + og.no +
    ' (satır ' + og.satirBas + '-' + og.satirSon + ').');
  s.push('// Elle düzenlenirse betik yeniden çalıştırılmadan önce değişiklik buraya not edilmeli.');
  s.push('package com.sorobit.shared.ogretici.icerik');
  s.push('');
  s.push('import com.sorobit.shared.ogretici.OgreticiAdimi');
  s.push('import com.sorobit.shared.ogretici.OgreticiBoncukHareketi');
  s.push('import com.sorobit.shared.ogretici.OgreticiGorunurluk');
  s.push('import com.sorobit.shared.ogretici.OgreticiIcerigi');
  s.push('import com.sorobit.shared.ogretici.OgreticiOdakIslemi');
  s.push('import com.sorobit.shared.ogretici.OgreticiRenkliHarf');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.CAPA_ALT_KILAVUZ');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.CAPA_ORTA');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.CAPA_UST_KILAVUZ');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.GORUNMEZ');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.GORUNUR');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.ILK_ACILISTA_GORUNMEZ');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.ONCEKI');
  s.push('import com.sorobit.shared.ogretici.OgreticiSabit.YOK');
  s.push('');
  s.push('internal fun ogretici' + og.no + '(): OgreticiIcerigi = OgreticiIcerigi(');
  s.push('    numara = ' + og.no + ',');
  s.push('    kuralTablosu = ' + (tablo[og.no] ? kotlinDizgi(tablo[og.no]) : 'null') + ',');
  s.push('    adimlar = listOf(');
  og.adimlar.forEach((a, i) => {
    s.push('        // ' + (i + 1) + ' · satır ' + a.satir);
    for (const u of a.uyarilar) s.push('        // UYARI: ' + u.replace(/\n/g, ' '));
    s.push('        OgreticiAdimi(');
    s.push('            metin = ' + (a.metin ? kotlinDizgi(a.metin) : '""') + ',');
    if (a.ses) s.push('            ses = ' + kotlinDizgi(a.ses) + ',');
    if (a.daktilo) s.push('            daktilo = true,');
    if (a.daktiloHizi !== 50) s.push('            daktiloHiziMs = ' + a.daktiloHizi + 'L,');
    if (a.hareketler.length) {
      s.push('            hareketler = listOf(');
      for (const h of a.hareketler) s.push('                OgreticiBoncukHareketi(cubuk = ' + h.cubuk + ', ust = ' + h.ust + ', sira = ' + h.sira + ', tur = ' + h.tur + '),');
      s.push('            ),');
    }
    if (a.gorunurlukler.length) {
      s.push('            gorunurlukler = listOf(');
      for (const [g, d] of a.gorunurlukler) s.push('                OgreticiGorunurluk(' + kotlinDizgi(g) + ', ' + d + '),');
      s.push('            ),');
    }
    if (a.odak.length) {
      s.push('            odak = listOf(');
      for (const o of a.odak) {
        const p = [];
        if (o.sag !== null) p.push('sagBoslukDp = ' + o.sag + 'f');
        if (o.genislik !== null) p.push('genislikDp = ' + (o.genislik === 'ONCEKI' ? 'ONCEKI' : o.genislik + 'f'));
        if (o.yukseklik !== null) p.push('yukseklikDp = ' + (o.yukseklik === 'ONCEKI' ? 'ONCEKI' : o.yukseklik + 'f'));
        if (o.capa !== null) p.push('capa = ' + o.capa);
        if (o.ust !== null) p.push('ustBoslukDp = ' + o.ust + 'f');
        s.push('                OgreticiOdakIslemi(' + p.join(', ') + '),');
      }
      s.push('            ),');
    }
    if (a.yerlesimler.length) {
      s.push('            yerlesimler = listOf(');
      for (const [g, d] of a.yerlesimler) s.push('                ' + kotlinDizgi(g + ':' + d) + ',');
      s.push('            ),');
    }
    for (const ad of ALAN_SIRASI) if (a.alanlar[ad] !== undefined) s.push('            ' + ad + ' = ' + a.alanlar[ad] + ',');
    s.push('        ),');
  });
  s.push('    ),');
  s.push(')');
  s.push('');
  return s.join('\n');
}

// ---------------------------------------------------------------------------------------------
// Giriş
// ---------------------------------------------------------------------------------------------

const argumanlar = process.argv.slice(2);
const kuru = argumanlar.includes('--kuru');
const rapor = argumanlar.includes('--rapor');
const tablo = kuralTablolari();
const hepsi = ogreticiNumaralari();
let istenen = argumanlar.filter(a => /^\d+$/.test(a)).map(Number);
if (rapor) istenen = hepsi;
if (istenen.length === 0) {
  console.error('Kullanım: donustur.js <öğretici no> [...] [--kuru] | --rapor');
  console.error('Öğreticiler: ' + hepsi.join(' '));
  process.exit(1);
}

let uyariVar = false;
for (const no of istenen) {
  if (!hepsi.includes(no)) { console.error('Bilinmeyen öğretici: ' + no); process.exit(1); }
  const og = ogreticiCoz(no);
  const uyariSayisi = og.adimlar.reduce((t, a) => t + a.uyarilar.length, 0);
  if (uyariSayisi > 0) uyariVar = true;
  if (rapor) {
    console.log(`${no}\tadım=${og.adimlar.length}\tsatır=${og.satirBas}-${og.satirSon}\tuyarı=${uyariSayisi}`);
    og.adimlar.forEach((a, i) => a.uyarilar.forEach(u => console.log(`    adım ${i + 1} (satır ${a.satir}): ${u}`)));
    continue;
  }
  const kod = kotlinUret(og, tablo);
  if (kuru) { process.stdout.write(kod); continue; }
  fs.mkdirSync(HEDEF, { recursive: true });
  const dosya = path.join(HEDEF, 'Ogretici' + no + '.kt');
  fs.writeFileSync(dosya, kod, 'utf8');
  console.log(`Yazıldı: ${path.relative(KOK, dosya)} (${og.adimlar.length} adım, ${uyariSayisi} uyarı)`);
}
process.exit(uyariVar ? 2 : 0);

/**
 * Sorobit kapalı test formlarını Google Forms'ta TEK SEFERDE oluşturur.
 * Metinler docs/KAPALI_TEST_FORMLARI.md ile aynı; biri değişirse diğeri de değişmeli.
 *
 * Kullanım:
 *   1. https://script.google.com → "Yeni proje"
 *   2. Editördeki her şeyi silip bu dosyanın tamamını yapıştır, kaydet (Ctrl+S)
 *   3. Üstteki işlev listesinde "formlariOlustur" seçiliyken "Çalıştır"
 *   4. İzin ekranı: hesabını seç → "Gelişmiş" → "... projesine git (güvenli değil)" → "İzin ver"
 *      (uyarı, betiği Google değil senin yazdığın için çıkıyor)
 *   5. Alttaki "Yürütme günlüğü"nde üç formun PAYLAŞIM bağlantısı ve DÜZENLEME bağlantısı yazar.
 *
 * Ne yapar: Drive'da "Sorobit kapalı test" klasörü açar; üç formu ve her birinin cevap tablosunu
 * (Google E-Tablolar) oraya koyar. E-posta toplamaz, Google hesabıyla giriş istemez.
 * İkinci kez çalıştırılırsa formları YENİDEN oluşturur (eskileri silinmez, klasörde iki kopya olur).
 */

function formlariOlustur() {
  const klasor = DriveApp.createFolder('Sorobit kapalı test');
  const sonuc = [
    form1(klasor),
    form2(klasor),
    form3(klasor),
  ];
  Logger.log('=== HAZIR ===');
  sonuc.forEach(function (s) {
    Logger.log(s.ad);
    Logger.log('  Annelere gönderilecek bağlantı: ' + s.paylasim);
    Logger.log('  Düzenleme (yalnızca sen):     ' + s.duzenleme);
    Logger.log('  Cevap tablosu:                 ' + s.tablo);
  });
  Logger.log('Klasör: ' + klasor.getUrl());
}

// ---------- ortak yardımcılar ----------

function yeniForm(klasor, baslik, aciklama, tesekkur) {
  const form = FormApp.create(baslik);
  form.setDescription(aciklama);
  form.setCollectEmail(false);
  form.setLimitOneResponsePerUser(false); // true olsaydı annelerden Google girişi isterdi
  form.setAllowResponseEdits(false);
  form.setShowLinkToRespondAgain(false);
  form.setProgressBar(false);
  form.setConfirmationMessage(tesekkur);
  form.setAcceptingResponses(true);
  // 2025 sonrası formlar "yayınla" adımı istiyor; eski API'de bu işlev yok.
  if (typeof form.setPublished === 'function') form.setPublished(true);
  return form;
}

function bitir(klasor, form, ad) {
  const tablo = SpreadsheetApp.create(ad + ' (cevaplar)');
  form.setDestination(FormApp.DestinationType.SPREADSHEET, tablo.getId());
  DriveApp.getFileById(form.getId()).moveTo(klasor);
  DriveApp.getFileById(tablo.getId()).moveTo(klasor);
  let paylasim = form.getPublishedUrl();
  try { paylasim = form.shortenFormUrl(paylasim); } catch (e) { /* kısaltma yoksa uzun bağlantı */ }
  return { ad: ad, paylasim: paylasim, duzenleme: form.getEditUrl(), tablo: tablo.getUrl() };
}

function kisa(form, baslik, zorunlu, yardim) {
  const s = form.addTextItem().setTitle(baslik).setRequired(zorunlu);
  if (yardim) s.setHelpText(yardim);
  return s;
}

function paragraf(form, baslik, zorunlu, yardim) {
  const s = form.addParagraphTextItem().setTitle(baslik).setRequired(zorunlu);
  if (yardim) s.setHelpText(yardim);
  return s;
}

function liste(form, baslik, zorunlu, secenekler) {
  return form.addListItem().setTitle(baslik).setRequired(zorunlu).setChoiceValues(secenekler);
}

function tekSecim(form, baslik, zorunlu, secenekler, digerVar) {
  const s = form.addMultipleChoiceItem().setTitle(baslik).setRequired(zorunlu).setChoiceValues(secenekler);
  if (digerVar) s.showOtherOption(true);
  return s;
}

function cokSecim(form, baslik, zorunlu, secenekler, digerVar) {
  const s = form.addCheckboxItem().setTitle(baslik).setRequired(zorunlu).setChoiceValues(secenekler);
  if (digerVar) s.showOtherOption(true);
  return s;
}

function tlSorusu(form, baslik) {
  const dogrulama = FormApp.createTextValidation()
    .requireWholeNumber()
    .setHelpText('Yalnızca sayı yazın (ör. 250).')
    .build();
  return kisa(form, baslik, false, 'TL olarak, yalnızca sayı yazın.').setValidation(dogrulama);
}

function kullaniciAdi(form, aciklamaliMi) {
  return kisa(form, 'Çocuğunuzun uygulamadaki kullanıcı adı nedir?', true,
    aciklamaliMi ? 'Profil ekranında, adın altında @ ile başlayan addır (ör. @ali123).' : null);
}

// ---------- Form 1: 1. gün ----------

function form1(klasor) {
  const ad = 'Sorobit testine hoş geldiniz';
  const form = yeniForm(klasor, ad,
    "Sorobit'i denediğiniz için teşekkürler! Bu form yaklaşık 3 dakika sürer.\n" +
    "Cevaplarınız yalnızca uygulamayı geliştirmek için kullanılacak ve çocuğunuzun uygulamadaki " +
    "ilerlemesiyle birlikte değerlendirilecek. Kimseyle paylaşılmaz; istediğiniz zaman silinmesini " +
    "isteyebilirsiniz (sorobit.support@gmail.com).",
    'Teşekkürler! Bir hafta sonra kısa bir form daha göndereceğiz.');

  kullaniciAdi(form, true);
  kisa(form, 'Telefon numaranız (WhatsApp)', false, 'Yalnızca takılırsanız size ulaşmak için.');
  kisa(form, 'Hangi şehirde yaşıyorsunuz?', true);
  liste(form, 'Çocuğunuz kaç yaşında?', true,
    ['4 ve altı', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15 ve üstü']);
  liste(form, 'Çocuğunuz kaçıncı sınıfta?', true,
    ['Okul öncesi', '1. sınıf', '2. sınıf', '3. sınıf', '4. sınıf', '5. sınıf', '6. sınıf', '7. sınıf', '8. sınıf', 'Diğer']);
  tekSecim(form, 'Çocuğunuzun abaküs (soroban) geçmişi hangisi?', true, [
    'Abaküsü hiç kullanmadı',
    'Şu an abaküs / mental aritmetik kursuna gidiyor',
    'Daha önce kursa gitti, bıraktı',
    'Kursa gitmeden evde biraz denedi',
  ]);
  // Açık uçlu "neden" ÖNCE: şıklı dert sorusu cevabı yönlendirmesin.
  paragraf(form, "Sorobit'i neden denediniz? Aklınızdan geçeni kendi cümlelerinizle yazın.", true);
  tekSecim(form, 'Çocuğunuzun matematikle ilgili sizi en çok düşündüren durum hangisi?', true, [
    'Matematikte zorlanıyor ya da matematikten korkuyor',
    'Ekranda çok vakit geçiriyor, bari faydalı bir şey yapsın',
    'Abaküs kursu pahalı ya da uzak, evde öğrenebilsin',
    'Kursa gidiyor ama evde çalışmıyor',
    'Zihinden hızlı hesap yapsın, sınavlarda (LGS, bursluluk) avantajı olsun',
    'Belirli bir derdim yok, merak ettim',
  ], true);
  tekSecim(form, 'Çocuğunuz uygulamayı hangi cihazda kullanacak?', true,
    ['Kendi telefonu ya da tableti', 'Benim telefonum', 'Evde ortak kullanılan bir cihaz']);
  cokSecim(form, 'Son bir yılda çocuğunuzun matematiği için neler yaptınız?', false, [
    'Kurs (abaküs / mental aritmetik)', 'Özel ders', 'Etüt / dershane', 'Ek kitap ya da kaynak',
    'Ücretli eğitim uygulaması', 'Ücretsiz eğitim uygulaması', 'Hiçbiri',
  ], true);
  tekSecim(form, 'Matematik için ayda yaklaşık ne kadar harcıyorsunuz?', false,
    ['0 TL', '1–500 TL', '501–1.500 TL', '1.501–3.000 TL', '3.000 TL üstü', 'Söylemek istemiyorum']);
  tekSecim(form, 'Birkaç gün sonra 10 dakikalık kısa bir telefon görüşmesine açık mısınız?', true,
    ['Evet', 'Hayır']);

  return bitir(klasor, form, 'Form 1 - 1. gün');
}

// ---------- Form 2: 7–10. gün ----------

function form2(klasor) {
  const ad = 'Bir haftalık Sorobit deneyiminiz';
  const form = yeniForm(klasor, ad,
    "Bir haftadır Sorobit'i deniyorsunuz. Bu form yaklaşık 3 dakika sürer.\n" +
    'Olumsuz görüşleriniz bizim için en değerlisi; lütfen içinizden geldiği gibi yazın.',
    'Teşekkürler! Görüşleriniz Sorobit\'i şekillendirecek.');

  kullaniciAdi(form, true);
  tekSecim(form, 'Son bir haftada çocuğunuz uygulamayı kaç gün açtı?', true,
    ['Hiç', '1–2 gün', '3–4 gün', '5–7 gün', 'Bilmiyorum']);
  tekSecim(form, 'Uygulamayı genellikle kim açıyor?', true,
    ['Çocuğum kendisi açıyor', 'Ben hatırlatınca açıyor', 'Çoğunlukla ben açıp veriyorum']);
  paragraf(form, 'Çocuğunuz takıldığı ya da anlamadığı bir yer oldu mu? Olduysa nerede?', false);
  paragraf(form, 'Çocuğunuz uygulama hakkında size ne söyledi?', false);
  tekSecim(form, 'Çocuğunuzda bir fark gördünüz mü?', true,
    ['Belirgin bir fark var', 'Biraz fark var', 'Henüz fark yok', 'Söylemek için erken']);
  paragraf(form, "Sorobit'i bir arkadaşınıza tek cümleyle nasıl anlatırdınız?", true);
  form.addScaleItem()
    .setTitle("Sorobit'i bir arkadaşınıza ne kadar önerirsiniz?")
    .setBounds(0, 10)
    .setLabels('Hiç önermem', 'Kesinlikle öneririm')
    .setRequired(true);
  // Fiyat: Pro aboneliğin aylık fiyatı. Gerçek fiyat söylenmiyor (cevaplar ona yaslanır).
  tlSorusu(form, 'Uygulamanın aylık üyeliği hangi fiyatta "bu çok pahalı, almam" derdiniz?');
  tlSorusu(form, 'Hangi fiyatta "bu kadar ucuzsa bir sorunu vardır" diye şüphelenirdiniz?');
  paragraf(form, 'Uygulamada en çok neyi değiştirirdiniz?', false);

  return bitir(klasor, form, 'Form 2 - 7-10. gün');
}

// ---------- Form 3: bırakanlar ----------

function form3(klasor) {
  const ad = 'Sorobit: kısa bir soru';
  const form = yeniForm(klasor, ad,
    'Sorobit bir süredir kullanılmamış. Sebebini öğrenmek bize çok yardımcı olur; 1 dakika sürer.',
    'Teşekkürler! Bu cevap uygulamayı düzeltmemize doğrudan yardım ediyor.');

  kullaniciAdi(form, false);
  tekSecim(form, 'Çocuğunuz uygulamayı en son ne zaman açtı?', true,
    ['Bu hafta', 'Geçen hafta', 'Daha önce', 'Bilmiyorum']);
  cokSecim(form, 'Neden devam etmedi?', true, [
    'Sıkıldı',
    'Anlamadı ya da zor geldi',
    'Çok kolay geldi',
    'Zaman olmadı',
    'Unuttu',
    'Can (enerji) bitince beklemek ya da reklamlar rahatsız etti',
    'Teknik bir sorun oldu',
  ], true);
  paragraf(form, 'Ne olsaydı devam ederdi?', false);
  tekSecim(form, 'İleride tekrar denemek ister misiniz?', false, ['Evet', 'Belki', 'Hayır']);

  return bitir(klasor, form, 'Form 3 - Bırakanlar');
}

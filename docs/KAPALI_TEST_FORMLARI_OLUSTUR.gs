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

// Uygulamada iki üyelik var: Pro (reklamsız, sınırsız enerji) ve Lite (reklamlı). Fiyat soruları
// Pro'yu soruyor; karışmasın diye açıklamada ayrıca yazıyor. Gerçek fiyat bilerek yazılmıyor.
const PRO_YARDIM = 'Pro üyelik: reklamsız, sınırsız enerji, Ustalık Yolu (reklamlı Lite üyelik değil). ' +
  'TL olarak, yalnızca sayı yazın.';

function tlSorusu(form, baslik) {
  const dogrulama = FormApp.createTextValidation()
    .requireWholeNumber()
    .setHelpText('Yalnızca sayı yazın (ör. 250).')
    .build();
  return kisa(form, baslik, false, PRO_YARDIM).setValidation(dogrulama);
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
  // Telefon görüşmesi sorusu yok (kullanıcı kararı 19.10.2026: telefonla görüşme yapılmayacak).

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
  tlSorusu(form, 'Pro üyeliğin aylık fiyatı ne olursa "bu çok pahalı, almam" derdiniz?');
  tlSorusu(form, 'Pro üyeliğin aylık fiyatı ne olursa "bu kadar ucuzsa bir sorunu vardır" diye şüphelenirdiniz?');
  paragraf(form, 'Uygulamada en çok neyi değiştirirdiniz?', false);

  return bitir(klasor, form, 'Form 2 - 7-10. gün');
}

// ---------- Form 3: bırakanlar ----------

// ---------- Düzeltme 1 (19.10.2026): yayındaki formları yerinde düzeltir ----------
// formlariOlustur 19.10.2026'da bir kez çalıştırıldı; sonra iki değişiklik istendi. Formlar yeniden
// oluşturulmasın (bağlantılar değişir) diye mevcut formlar kimlikleriyle açılıp düzeltiliyor.
// Yalnızca BİR KEZ çalıştır; ikinci çalıştırma bir şey değiştirmez, zararsızdır.

function duzeltme1() {
  const f1 = FormApp.openById('12gIa79McdHD35UBGCPW1Uisoz6v5SIfix0eh9oLf9Pc');
  let silinen = 0;
  f1.getItems().forEach(function (it) {
    if (it.getTitle().indexOf('telefon görüşmesine') !== -1) { f1.deleteItem(it); silinen++; }
  });
  const f2 = FormApp.openById('1imp0fqNXnWJuKcd6JsWI7qW2hwhJUNy7Iw3pjhi4isk');
  let degisen = 0;
  f2.getItems().forEach(function (it) {
    const t = it.getTitle();
    if (t.indexOf('çok pahalı, almam') !== -1) {
      it.setTitle('Pro üyeliğin aylık fiyatı ne olursa "bu çok pahalı, almam" derdiniz?').setHelpText(PRO_YARDIM);
      degisen++;
    } else if (t.indexOf('bir sorunu vardır') !== -1) {
      it.setTitle('Pro üyeliğin aylık fiyatı ne olursa "bu kadar ucuzsa bir sorunu vardır" diye şüphelenirdiniz?')
        .setHelpText(PRO_YARDIM);
      degisen++;
    }
  });
  Logger.log('Form 1: silinen soru = ' + silinen + ' (beklenen 1)');
  Logger.log('Form 2: değişen soru = ' + degisen + ' (beklenen 2)');
}

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

// ---------- Kurs formları (22.10.2026): öğretmen görüşü + sorun/istek bildirimi ----------
// Mental aritmetik kursuyla yapılan test için. formlariOlustur'dan AYRI tutuldu: o bir daha
// çalıştırılırsa veli formlarının ikinci kopyası açılır. Metinler docs/KAPALI_TEST_FORMLARI.md
// "Kursla test" bölümüyle aynı. Yalnızca BİR KEZ çalıştır (ikinci çalıştırma iki kopya açar).
// 22.10.2026 22:50'de çalıştırıldı: Ö1 = 1ayq2BpD6q6UOIQZiBfi2GltbAMUs6UmIP_fg5cxp1X0,
// Ö2 = 16eZTr1OSDiRbYIyQ1kKcg2w0Xl8tGvbVg9mPVK6qMsU. Düzeltme gerekirse duzeltme1 gibi openById ile.

function kursFormlariOlustur() {
  // Veli formlarının klasörü varsa onu kullan; her şey tek klasörde dursun.
  const bulunan = DriveApp.getFoldersByName('Sorobit kapalı test');
  const klasor = bulunan.hasNext() ? bulunan.next() : DriveApp.createFolder('Sorobit kapalı test');
  const sonuc = [
    ogretmenFormu(klasor),
    bildirimFormu(klasor),
  ];
  Logger.log('=== HAZIR ===');
  sonuc.forEach(function (s) {
    Logger.log(s.ad);
    Logger.log('  Gönderilecek bağlantı:     ' + s.paylasim);
    Logger.log('  Düzenleme (yalnızca sen):  ' + s.duzenleme);
    Logger.log('  Cevap tablosu:             ' + s.tablo);
  });
  Logger.log('Klasör: ' + klasor.getUrl());
  Logger.log('UNUTMA: Form Ö2 → Yanıtlar → ⋮ → "Yeni yanıtlar için e-posta bildirimleri al" (betikle açılamıyor).');
}

function bolum(form, baslik, aciklama) {
  const s = form.addSectionHeaderItem().setTitle(baslik);
  if (aciklama) s.setHelpText(aciklama);
  return s;
}

// 17. sorunun satırları ve 18. sorunun şıkları aynı liste: 18, 17'de her şeye "olmazsa olmaz"
// diyen öğretmeni tek bir seçime zorluyor.
const OGRETMEN_OZELLIKLERI = [
  'Öğrencilerimin ilerlemesini görmek (bitirdiği dersler, çalışma süresi, seri)',
  'Öğrencilerimin en çok hata yaptığı işlemleri görmek',
  'Ödev vermek ve yapılıp yapılmadığını takip etmek',
  'Kendi alıştırma setimi hazırlamak (ör. 20 soruluk 3 basamaklı toplama)',
  'Öğrencilerimi sınıf ya da grup olarak toplamak',
  // Şu an sorular bütün onaylı öğretmenlerin gördüğü ortak havuza düşüyor (runTeacherPoolNotify).
  'Soruların yalnızca kendi öğrencilerimden gelmesi',
  'Velilere haftalık ilerleme özeti gitmesi',
  'Sınıf içi yarışma ve liderlik tablosu',
  'Seviye belirleme sınavı',
];

// ---------- Form Ö1: öğretmen görüşü (10–12. gün) ----------

function ogretmenFormu(klasor) {
  const ad = 'Sorobit öğretmen görüşü';
  const form = yeniForm(klasor, ad,
    "Sorobit'i öğrencilerinizle denediğiniz için çok teşekkürler! Bu form yaklaşık 8 dakika sürer.\n" +
    'Bir mental aritmetik öğretmeninin gözünden görüşleriniz, uygulamanın ve öğretmen özelliklerinin ' +
    'nasıl gelişeceğini doğrudan belirleyecek. Eleştirileriniz en değerlisi. Cevaplarınız yalnızca ' +
    "Sorobit'i geliştirmek için kullanılır, kimseyle paylaşılmaz.",
    'Çok teşekkürler! Cevaplarınızı okuyup kısa bir görüşme için size ulaşacağım.');

  bolum(form, 'Sizi tanıyalım');
  kisa(form, 'Adınız soyadınız', true);
  kisa(form, 'Kursunuzun adı', true);
  kisa(form, 'Uygulamadaki kullanıcı adınız', false,
    'Profil ekranında, adın altında @ ile başlayan ad. Cevapladığınız soruları eşleştirmek için.');
  tekSecim(form, 'Kaç yıldır abaküs / mental aritmetik öğretiyorsunuz?', true,
    ['1 yıldan az', '1–3 yıl', '4–7 yıl', '8 yıl ve üstü']);
  cokSecim(form, 'Hangi yaş gruplarına ders veriyorsunuz?', true,
    ['Okul öncesi (4–6 yaş)', '1–2. sınıf', '3–4. sınıf', '5–6. sınıf', '7–8. sınıf'], true);
  tekSecim(form, "Testte kaç öğrenciniz Sorobit'i kullandı?", true,
    ['Hiç', '1–5', '6–10', '11–20', "20'den fazla", 'Bilmiyorum']);

  bolum(form, "Sorobit'i nasıl kullandınız?");
  cokSecim(form, "Sorobit'i öğrencilerinizle nasıl kullandınız?", true, [
    'Derste birlikte açtık',
    'Ev ödevi olarak verdim',
    'Öğrencilere / velilere önerdim, kendileri kullandı',
    'Yalnızca kendim denedim',
    'Henüz kullanmadık',
  ], true);
  // Öğretmenin kendisi ne kadar baktı: "yöntem uyumlu değil" gibi cevapları buna göre tartmak için.
  tekSecim(form, 'Uygulamayı kendiniz ne kadar denediniz?', true, [
    'Yalnızca birkaç dakika baktım',
    'Birkaç dersi kendim yaptım',
    'Bir üniteyi ya da daha fazlasını bitirdim',
  ]);

  bolum(form, 'Öğrencileriniz');
  paragraf(form, 'Öğrencileriniz uygulamada nerede takıldı ya da zorlandı?', true,
    "Ders adını ya da ekranı yazabilirseniz çok yardımcı olur. Takılan olmadıysa 'yok' yazın.");
  tekSecim(form, 'Sorobit en çok hangi öğrencilerinize uygun?', false, [
    'Abaküse yeni başlayanlara',
    'Temel hareketleri bilen orta seviyeye',
    'İleri seviyeye',
    'Hepsine aynı ölçüde',
    'Söylemek için erken',
  ]);

  bolum(form, 'Abaküs yöntemi');
  tekSecim(form, 'Uygulamadaki abaküs anlatımı, kursta öğrettiğiniz yöntemle ne kadar uyumlu?', true, [
    'Tamamen uyumlu',
    'Büyük ölçüde uyumlu, küçük farklar var',
    'Önemli farklar var',
    'Çok farklı, öğrencinin kafasını karıştırır',
    'Değerlendirecek kadar bakmadım',
  ]).setHelpText("Örneğin boncuk hareketleri, 5'lik ve 10'luk geçişler, parmak kullanımı, körleme.");
  paragraf(form, 'Farklı ya da hatalı bulduğunuz yerleri yazar mısınız?', false);
  tekSecim(form, 'Derslerin sırası ve zorluk artışı nasıl?', false,
    ['Çok yavaş ilerliyor', 'Uygun', 'Çok hızlı ilerliyor', 'Bazı yerlerde birden zorlaşıyor']);

  bolum(form, 'Öğretmene soru');
  tekSecim(form, 'Öğrencilerin uygulamadan gönderdiği soruları cevaplayabildiniz mi?', true, [
    'Evet, soru cevapladım',
    'Bana soru gelmedi',
    'Nasıl yapılacağını bulamadım',
    'Vaktim olmadı',
    'Hesabım henüz onaylanmamıştı',
  ]);
  paragraf(form, 'Soru cevaplarken zorlandığınız ya da eksik bulduğunuz bir şey oldu mu?', false);

  bolum(form, 'Öğretmen özellikleri',
    'Testten sonra öğretmenlere özel özellikler ekleyeceğim. Hangisini önce yapacağımı sizin ' +
    'cevaplarınız belirleyecek.');
  // Açık uçlu soru listeden ÖNCE: liste önce gelirse öğretmen kendi ihtiyacını değil listedekini yazar.
  paragraf(form, "Öğretmen olarak Sorobit'te en çok neyi görmek ya da yapabilmek isterdiniz? " +
    'Kendi cümlelerinizle yazın.', true);
  form.addGridItem()
    .setTitle('Aşağıdakiler sizin için ne kadar önemli?')
    .setRows(OGRETMEN_OZELLIKLERI)
    .setColumns(['Olmazsa olmaz', 'Olsa iyi olur', 'Gerek yok'])
    .setRequired(true);
  tekSecim(form, 'Bunlardan yalnızca biri yapılabilseydi hangisi olsun?', true, OGRETMEN_OZELLIKLERI);

  bolum(form, 'Veliler');
  paragraf(form, "Sorobit'i velilere nasıl anlattınız ya da anlatırdınız? Kullandığınız cümleleri yazın.", true);
  paragraf(form, 'Velilerden bir soru, itiraz ya da tepki geldi mi?', false);

  bolum(form, 'Genel');
  tekSecim(form, "Test bittikten sonra Sorobit'i öğrencilerinize kullandırmayı düşünür müsünüz?", true, [
    'Evet, bütün öğrencilerime',
    'Evet, bazı öğrencilerime',
    'Öğretmen özellikleri gelirse',
    'Emin değilim',
    'Hayır',
  ]);
  form.addScaleItem()
    .setTitle("Sorobit'i başka bir abaküs öğretmenine ne kadar önerirsiniz?")
    .setBounds(0, 10)
    .setLabels('Hiç önermem', 'Kesinlikle öneririm')
    .setRequired(true);
  paragraf(form, 'Uygulamada en çok neyi değiştirirdiniz?', false);
  // Fiyat ve iş birliği bilerek yok: görüşmede, kursun durumuna göre soruluyor.

  return bitir(klasor, form, 'Form Ö1 - Öğretmen görüşü');
}

// ---------- Form Ö2: sorun ve istek bildirimi (sürekli açık) ----------
// Dosya yükleme sorusu bilerek yok: Forms'ta dosya yükleme Google girişi istiyor. Ekran
// görüntüsü WhatsApp'tan geliyor.

function bildirimFormu(klasor) {
  const ad = 'Sorobit: sorun ve istek bildirimi';
  const form = yeniForm(klasor, ad,
    "Sorobit'te bir hata, takıldığınız bir yer ya da bir isteğiniz mi var? Bu form 1–2 dakika sürer.\n" +
    'Ekran görüntüsü ya da ekran kaydı çok yardımcı olur: formu gönderdikten sonra WhatsApp\'tan ' +
    "kurs sorumlunuza ya da doğrudan Başar'a iletin.",
    'Teşekkürler, bildiriminiz ulaştı! Ekran görüntüsü varsa WhatsApp\'tan göndermeyi unutmayın.');
  // Aynı kişi birden çok sorun bildirebilsin.
  form.setShowLinkToRespondAgain(true);

  tekSecim(form, 'Bildiren kim?', true, ['Öğretmen', 'Öğrenci / veli', 'Kurs sorumlusu']);
  kisa(form, 'Adınız', false);
  tekSecim(form, 'Ne bildiriyorsunuz?', true, [
    'Hata ya da teknik sorun',
    'Takılınan / anlaşılmayan bir yer',
    'İstek ya da öneri',
    'Soru',
  ]);
  kisa(form, 'Sorunu yaşayan kişinin uygulamadaki kullanıcı adı', false,
    'Profil ekranında @ ile başlayan ad. Hatayı bulmayı çok kolaylaştırır.');
  paragraf(form, 'Ne oldu? Ne yaparken oldu?', true,
    "Örnek: '3. bölümün 2. dersinde Devam'a basınca ekran dondu.' İstekse ne olmasını istediğinizi yazın.");
  form.addDateItem().setTitle('Ne zaman oldu?').setRequired(false);
  tekSecim(form, 'Bir kez mi oldu, tekrar ediyor mu?', false,
    ['Bir kez oldu', 'Ara sıra oluyor', 'Her seferinde oluyor']);
  kisa(form, 'Telefon markası ve modeli', false,
    "Ayarlar → Telefon hakkında'da yazar (ör. Samsung Galaxy A14).");
  tekSecim(form, 'Ekran görüntüsü ya da kaydı var mı?', false,
    ["Var, WhatsApp'tan gönderiyorum", 'Yok']);

  return bitir(klasor, form, 'Form Ö2 - Sorun ve istek bildirimi');
}

// ---------- Form 1K (22.10.2026): kurs velileri için kısa 1. gün formu ----------
// Form 1'in kısaltılmış kopyası; Form 1 kurs dışı testçiler için olduğu gibi kaldı. Çıkarılanlar:
// telefon, şehir, son bir yılda neler yapıldığı, aylık harcama. "Neden denediniz?" yerine
// "beklentiniz nedir?": kurs velilerinin cevabı hep "öğretmen önerdi" olurdu. Abaküs geçmişi
// yerine kursa gitme süresi: bu velilerin hepsi zaten kursta, asıl ayrım seviye. Dert sorusu
// bilerek Form 1'deki gibi (reklam testindeki 5 mesajla aynı şıklar, takip tablosu bu sütunu
// kullanıyor). Yalnızca BİR KEZ çalıştır (ikinci çalıştırma ikinci kopyayı açar).

function kursVeliFormuOlustur() {
  const bulunan = DriveApp.getFoldersByName('Sorobit kapalı test');
  const klasor = bulunan.hasNext() ? bulunan.next() : DriveApp.createFolder('Sorobit kapalı test');
  const s = form1Kurs(klasor);
  Logger.log('=== HAZIR ===');
  Logger.log(s.ad);
  Logger.log('  Velilere gönderilecek bağlantı: ' + s.paylasim);
  Logger.log('  Düzenleme (yalnızca sen):       ' + s.duzenleme);
  Logger.log('  Cevap tablosu:                   ' + s.tablo);
}

function form1Kurs(klasor) {
  const ad = 'Sorobit testine hoş geldiniz';
  const form = yeniForm(klasor, ad,
    "Sorobit'i denediğiniz için teşekkürler! Bu form yaklaşık 2 dakika sürer.\n" +
    "Cevaplarınız yalnızca uygulamayı geliştirmek için kullanılacak ve çocuğunuzun uygulamadaki " +
    "ilerlemesiyle birlikte değerlendirilecek. Kimseyle paylaşılmaz; istediğiniz zaman silinmesini " +
    "isteyebilirsiniz (sorobit.support@gmail.com).",
    'Teşekkürler! Bir hafta sonra kısa bir form daha göndereceğiz.');

  kullaniciAdi(form, true);
  liste(form, 'Çocuğunuz kaç yaşında?', true,
    ['4 ve altı', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15 ve üstü']);
  liste(form, 'Çocuğunuz kaçıncı sınıfta?', true,
    ['Okul öncesi', '1. sınıf', '2. sınıf', '3. sınıf', '4. sınıf', '5. sınıf', '6. sınıf', '7. sınıf', '8. sınıf', 'Diğer']);
  tekSecim(form, 'Çocuğunuz ne kadar süredir abaküs / mental aritmetik kursuna gidiyor?', true,
    ['3 aydan az', '3–6 ay', '6–12 ay', '1–2 yıl', '2 yıldan fazla']);
  // Açık uçlu soru ÖNCE: şıklı dert sorusu cevabı yönlendirmesin.
  paragraf(form, "Sorobit'ten beklentiniz nedir? Aklınızdan geçeni kendi cümlelerinizle yazın.", true);
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

  return bitir(klasor, form, 'Form 1K - Kurs velileri 1. gün');
}

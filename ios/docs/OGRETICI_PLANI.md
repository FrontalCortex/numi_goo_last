# Öğretici (TutorialFragment) iOS planı

Bu belge, Android'deki öğreticiyi (`app/src/main/java/com/example/app/TutorialFragment.kt`, ~20 bin
satır) iOS'a taşıyan oturumların **tek başvuru belgesidir**. Genel kurallar için önce
`ios/iosApp/GELISTIRME.md`'yi okuyun; burada yalnızca öğreticiye özgü olanlar var.

Durum (27.11.2026): çekirdek (model, oynatıcı, ekran, abaküs kancaları, ilk açılış bağlantısı) ve
**1. öğretici** tam. Kalan 33 öğreticinin içeriği betikle üretilmeye hazır; görünüm eksikleri
(seçenek paneli, kural tabloları, kör ders görselleri) paketlere bölündü (bölüm 6). **Hiçbiri henüz
Codemagic'te derlenmedi.**

---

## 1. Android'deki yapı

### 1.1 Dosyanın bölümleri (satır numaraları 27.11.2026 çalışma ağacı)

| Satır | Ne | Tür |
|---|---|---|
| 1-302 | alanlar (boncuk görünümleri, 34 `tutorialStepsN` listesi), companion (`newInstance(tutorialNumber)`) | motor |
| 303-545 | onCreate (lessonItem bulma, ölçüm), ekran ölçekleme (359-444), onViewCreated: `when (tutorialNumber)` ile adım listesi (480-516), 24-26'da abaküs gizli, 1'de hesapsızken çıkış gizli | motor |
| 547-822 | setupTutorial, `handleTutorialTapToAdvance` (561), **`showStep` (607-822)** | motor |
| 824-1300 | çıkış, Sobi'ye sor (861-1035, hesap ister), ipucu paneli (1036-1219), **geri (1220-1284)** | motor |
| 1302-1512 | `updateBeadForDigit` + **`writeAnswerNumber`** (sayıyı animasyonla yazma) | motor |
| 1513-1650 | closeFragment, **`devametFragment` (1568: son adımda "Devam Et")**, isAnyAnimationRunning | motor |
| 1653-1850 | tanı günlükleri, dp yardımcıları (`focusMarginRightPx`) | motor |
| **1851-17321** | **İÇERİK: 34 × `createTutorialStepsN()`, 1270 adım** (dosyanın %77'si) | **veri** |
| 17322-17427 | kural metni (`markedRuleText`), tutorialText çapası | motor |
| 17428-17623 | `applyWidgetOperations`, `WidgetOperation`, **`TutorialStep`** (adım modeli) | model |
| 17624-18170 | cevap denetimi, seçenek paneli, ölçüm kimliği denetimi (17795) | motor |
| 18170-18525 | doğru/yanlış panelleri, "Eğitimi atla", yanlıştan sonra tekrar | motor |
| 18525-19700 | boncuk görünümleri, `setupBeads` (18685-19448), boncuk animasyonları, `resetAbacus` | motor |
| 19701-19961 | yaşam döngüsü, ses (19783-19870), daktilo (19871-19946) | motor |

İçerik Kotlin kodunun içinde ama **tamamen bildirimsel**: her adım tek bir `TutorialStep(...)` çağrısı,
adlandırılmış argümanlarla. Metinler `getString(R.string.x)` (strings.xml), sesler `R.raw.x`,
boncuklar `BeadAnimation(this, "rod4_bead_bottom1", 1)`, görünüm değişiklikleri `WidgetOperation`
lambdaları. `if` yalnızca bir yerde var (1. öğreticinin ilk adımı: ilk açılışta "Eğitimi atla" gizli).
Bu yüzden içerik **mekanik olarak çıkarılabiliyor** (bölüm 3.2).

### 1.2 Öğreticiler

34 öğretici, 1270 adım. Ders kataloğunda (`GlobalLessonDataKatalog.kt`) 32'si kullanılıyor;
**104 ve 999 hiçbir derste yok** (öncelik en düşük).

| No | Kısım | Satır | Adım | Abaküs sorusu | Seçenekli | Boncuk anim. | Kural tablosu | Kural metni | Odak | Renkli soru | Geri cevap |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 1893-2312 | 30 | 5 | 0 | 18 | - | - | var | - | - |
| 100 | 1 | 2313-2655 | 30 | 5 | 0 | 50 | - | - | - | - | 2 |
| 101 | 1 | 2656-3133 | 35 | 12 | 1 | 60 | - | - | - | 24 | 9 |
| 2 | 1 | 3134-3334 | 17 | 4 | 0 | 16 | - | - | - | 9 | 2 |
| 102 | 1 | 3335-3630 | 26 | 6 | 0 | 34 | - | - | - | 16 | 4 |
| 103 | 1 | 3631-4036 | 33 | 8 | 0 | 50 | - | - | - | 22 | 9 |
| 3 | 1 | 4037-4860 | 82 | 6 | 2 | 50 | five | - | - | 55 | 7 |
| 4 | 1 | 4861-5159 | 27 | 4 | 1 | 26 | five | - | - | 19 | 4 |
| 5 | 1 | 5160-5950 | 68 | 4 | 2 | 58 | ten | - | var | 42 | 4 |
| 6 | 1 | 5951-6778 | 58 | 6 | 0 | 80 | ten | 33 | var | - | 4 |
| 104 | (yok) | 6779-7359 | 50 | 6 | 0 | 80 | - | - | var | - | 4 |
| 7 | 1 | 7360-7724 | 38 | 3 | 1 | 28 | tenLinear | - | - | - | 3 |
| 105 | 1 | 7725-8432 | 53 | 6 | 0 | 80 | tenLinear | 32 | var | - | 4 |
| 8 | 1 | 8433-9198 | 80 | 0 | 2 | 107 | Bead | - | - | - | - |
| 9 | 1 | 9199-9624 | 36 | 3 | 2 | 22 | Bead | - | - | 29 | 5 |
| 10 | 2 | 9625-9884 | 22 | 3 | 0 | 22 | - | - | - | 14 | 3 |
| 11 | 2 | 9885-10243 | 39 | 3 | 1 | 28 | extractionFive | - | - | - | 3 |
| 1100 | 2 | 10244-10817 | 34 | 4 | 3 | 24 | extractionFive | - | var | 28 | 8 |
| 12 | 2 | 10818-11367 | 63 | 0 | 1 | 62 | tenRuleExtraction | - | - | 1 | - |
| 13 | 2 | 11368-11747 | 27 | 3 | 0 | 22 | tenRuleExtraction | - | var | 21 | 3 |
| 14 | 2 | 11748-12799 | 65 | 8 | 1 | 60 | tenRuleExtraction | 40 | var | 50 | 7 |
| 999 | (yok) | 12800-12815 | 2 | 0 | 0 | 0 | - | - | - | - | - |
| 15 | 2 | 12816-13259 | 31 | 0 | 0 | 37 | - | 24 | var | 26 | - |
| 16 | 2 | 13260-13945 | 60 | 7 | 1 | 50 | (widgetVisibilityMap) | 4 | - | 6 | 7 |
| 17 | 2 | 13946-14276 | 24 | 5 | 0 | 34 | BeadRuleExtraction | 4 | - | 20 | 6 |
| 18 | 3 | 14277-14766 | 34 | 2 | 1 | 44 | - | 18 | var | 21 | 2 |
| 19 | 3 | 14767-15298 | 35 | 3 | 1 | 38 | - | 21 | var | 26 | 4 |
| 20 | 3 | 15299-16028 | 55 | 4 | 4 | 78 | - | 16 | - | 36 | 6 |
| 21 | 3 | 16029-16299 | 25 | 3 | 0 | 28 | - | - | - | 10 | 2 |
| 22 | 3 | 16300-16733 | 36 | 6 | 1 | 36 | - | - | - | 20 | 6 |
| 23 | 3 | 16734-17112 | 27 | 6 | 0 | 44 | - | - | - | 20 | 6 |
| 24 | 4 | 17113-17239 | 16 | 0 | 0 | 0 | - | - | - | - | - |
| 25 | 5 | 17240-17292 | 8 | 0 | 0 | 0 | - | - | - | - | - |
| 26 | 6 | 17293-17321 | 4 | 0 | 0 | 0 | - | - | - | - | - |

Sütunlar sayımdır ("Boncuk anim." tek tek boncuk hareketi). "Kural tablosu": `showStep`'teki
`when (tutorialNumber)` ile seçilen tablo (`fiveRuleTable`, `tenRuleTable`, `tenRuleTableLinearLayout`,
`BeadRuleTable`, `extractionFiveRuleTable`, `tenRuleExtractionTableLayout`, `BeadRuleExtractionTable`).
24'te kör ders görselleri (`blinding_tutorial1..3`); 24-26'da abaküs hiç görünmez.

### 1.3 Adım türleri

Ayrı bir "tür" alanı yok; tür, alanların birleşiminden çıkıyor. Bütün adımlar ortak olarak:
**metin** (her adım; neredeyse hepsi daktilo, 40 ms/harf), **ses** (1092 adım; Türkçe ses `res/raw`),
**dokunarak ilerleme**, **geri düğmesi**.

| Tür | Android alanları | iOS'ta |
|---|---|---|
| Anlatım | yalnızca `text` (+ ses, daktilo) | var |
| Boncuk gösterimi | `animation = listOf(BeadAnimation(...))` — ileri giderken oynar, geri giderken bir sonraki adımınki ters oynar | var |
| Odak çerçevesi (vurgulama) | `widgetOperations` içinde `focusView` (görünürlük, sağ boşluk = sütun, boy = sütun / bütün abaküs / üst bant / alt bant, kılavuza bağlama) | var (genel geometri) |
| Görünüm aç/kapa | `ChangeVisibility(binding.x)`, `widgetVisibilityMap` (abaküs, geri, "Eğitimi atla", tablolar, kör görseller) | var (veri); tablo ve görseller paketlerde |
| Soru kutusu | `questionText` (+ `questionTextColorPositions`: harf renkleri) | var |
| Abaküse sayı yazdırma | `abacusClickable`, `answerNumber`, `nextStepAvailable = false`, "Kontrol Et" → doğru / yanlış paneli; `requestText` yanlış panelinde ipucu | var |
| Abaküs sıfırlama / sayı kurma | `abacusReset`, `nextStepAbacusReset`, `resetAndWaith`, `backAnswerNumber`, `onStepComplete { resetAbacus() }` (yalnızca 14) | var |
| Çoktan seçmeli | `options`, `correctOptionIndex`, `multipleChoice`, `optionText` | kural var, **panel Paket A** |
| Kural tablosu | `rulesPanelVisibility` (öğreticinin tablosu), `widgetVisibilityMap` (16) | seçici var, **tablolar Paket B/C/D** |
| Kural metni | `rulesStepTextContent` ("[işaretli]" mavi), `rulesStepTextVisibility`, `rulesStepTextTopToBottomOf` | basit hâli var, **çapa Paket B** |
| Kör ders görseli | `ChangeVisibility(binding.blindingTutorialN)`, `ChangeConstraints(tutorialText → görsel)` (24) | veri var, **görünüm Paket E** |

Android'de olup öğreticide **olmayanlar**: maskot (öğretici ekranında Sobi maskotu yok; sağ üstteki
Sobi düğmesi yalnızca oturum açıkken görünür ve sohbeti açar), ilerleme çubuğu (yok), "belirli
boncuğa dokunmayı bekleme" (yok: soru adımları her zaman "sayıyı yaz + Kontrol Et").

### 1.4 Giriş / çıkış akışı ve ilerleme

- **Haritadan**: `LessonAdapterLogic.continueWithLessonTarget` → `TUTORIAL` (öğretici bitmemiş) ya da
  paneldeki "Eğitimi tekrarla" (`againTutorial`, 1 enerji). Enerji haritada harcanır.
- **Öğreticiden derse**: son adım gösterilince abaküs gizlenir, "Eğitimi atla" kayar, alttan "Devam
  Et" gelir (`devametFragment`); basınca ders (`isBlinding` → BlindingLessonFragment, değilse
  AbacusFragment). "Eğitimi atla" da doğrudan derse geçer. Çıkış (sol üst) haritaya döner (400 ms sola
  kayar). İlk adımda yanlış → kapanır.
- **İlerleme**: öğretici kendisi hiçbir şey yazmaz. `tutorialIsFinish = true`, dersin sonuç ekranında
  "claim" ile yazılır (`LessonResultLogic.onClaim`, ortak kodda). Ölçüm (AnalyticsLogger,
  TutorialStepAnalytics) ve öğretici sonu anketi (TutorialSurveyPolicy) iOS'ta yok.
- **Hesapsız ilk açılış**: Splash → `SplashRules.route` = GUEST_MAIN → cihazda hiç hesap
  açılmadıysa (`showsFirstTutorial`) `START_DESTINATION_TUTORIAL` → MainActivity.showFirstTutorial:
  kısım 1'in `getLessonItem(1)`'i (öğretici 1), harita tabanda, öğretici üstünde. Bu akışta çıkış
  düğmesi ve "Eğitimi atla" gizli, sistem geri tuşu çalışmaz; `GlobalValues.currentTutorialNumber = 1`.
  Ders bitince sonuç ekranının claim'i → ChestFragment (başarı) ya da LessonResultFalse (başarısızlık)
  sandık yerine **LoginStartActivity**'yi açar (`EXTRA_BLOCK_BACK`) ve `tutorial1_login_flow_pending`
  yazar: girişten sonra kısım 1'in varsayılan listesi 1. ders geçilmiş olarak kurulur
  (`GlobalLessonData.buildDefaultLessonItemsForPart(1, true)`).

---

## 2. iOS'taki karşılık (yapılanlar)

```
ios/shared/src/commonMain/kotlin/com/sorobit/shared/ogretici/
  OgreticiAdimi.kt          model: OgreticiAdimi (TutorialStep), OgreticiBoncukHareketi, OgreticiGorunurluk,
                            OgreticiOdakIslemi, OgreticiRenkliHarf, OgreticiIcerigi, OgreticiSabit
  OgreticiSahnesi.kt        OgreticiKurallari.sahne: bir adımın ekranı (görünürlükler, odak, düğmeler);
                            baslangicDegeri: ortadan açılışta abaküsteki sayı
  OgreticiOynatici.kt       durum makinesi: dokunuldu / geri / kontrol / secenekKontrol / dogruDevam /
                            yanlisTamam → sırayla yapılacak işler (OgreticiEtki)
  OgreticiAbakusKoprusu.kt  tek boncuk animasyonu (BeadAnimation) ve sayıyı animasyonla yazma
                            (writeAnswerNumber) — AbacusBeadController + AbacusBeadHost üzerinde
  OgreticiKatalogu.kt       numara → içerik (yoksa null), oynaticiKur
  icerik/Ogretici1.kt       1. öğretici (üretildi)
  icerik/OgreticiN.kt       öteki 33: yer tutucu (null)
ios/shared/src/commonTest/kotlin/com/sorobit/shared/ogretici/
  OgreticiOynaticiTest.kt, OgreticiAbakusKoprusuTest.kt
ios/tools/ogretici-donustur/donustur.js   Android → Kotlin içerik dönüştürücüsü
ios/iosApp/iosApp/Ogretici/
  OgreticiEkrani.swift        giriş görünümü, OgreticiBaslangici, OgreticiSonucu, katmanlar, ana düzen
  OgreticiGorunumModeli.swift iş kuyruğu, daktilo, ses, sonuç paneli, çıkış
  OgreticiKoprusu.swift       BÜTÜN Shared çağrıları; Kotlin → Swift değerleri (OgreticiAdimVerisi,
                              OgreticiSahneVerisi, OgreticiIs)
  OgreticiParcalari.swift     ölçüler, renkler, çıkış, metin (daktilo), soru kutusu, alt panel
  OgreticiAbakusAlani.swift   abaküs + odak çerçevesi
  OgreticiSonucPaneli.swift   doğru / yanlış panelleri
  OgreticiSes.swift           OgreticiSesCalici protokolü + SahteOgreticiSesCalici
  OgreticiKuralAlani.swift    kural tablosu seçici + kural metni           (Paket B'nin)
  OgreticiKuralTablolari1/2/3.swift  tablolar (yer tutucu)                  (Paket B / C / D)
  OgreticiSecenekPaneli.swift seçenek paneli (yer tutucu)                   (Paket A)
  OgreticiGorselAlani.swift   kör ders görselleri (yer tutucu)              (Paket E)
Abakus/AbakusTahta.swift, AbakusGorunumu.swift   EKLEME: ogreticiBoncukOynat, ogreticiSayiyiYaz, ogreticiMesgul
Yonetim/  öğretici hedefi gerçek ekrana bağlı; ilk açılış akışı; galeri adları
```

Giriş görünümü:

```swift
OgreticiEkrani(
    baslangic: OgreticiBaslangici(ogreticiNo: Int, ilkAcilis: Bool = false, baslangicAdimi: Int = 0, baslik: String = ""),
    ses: OgreticiSesCalici = SahteOgreticiSesCalici(),
    bitti: (OgreticiSonucu) -> Void,   // .derseGec ("Devam Et") | .atlandi ("Eğitimi atla")
    kapat: () -> Void                   // çıkış düğmesi / ilk adımda yanlış
)
OgreticiBaslangici.icerikVar(no) -> Bool  // false ise yönetici eski yer tutucuyu açar
```

Yönetim (`Yonetim/`):
- `.ogretici(hedef)` katmanı içerik varsa `OgreticiEkrani`, yoksa eski yer tutucu
  (`UygulamaKokGorunumu.ogretici`). `bitti` → `ogreticiGecildi` (ders katmanı öğreticinin yerine).
- `misafirOlarakDevam(.ogretici)` → kabuk + `ilkOgreticiyiAc()` (kısım 1, sıra 1, `ilkAcilis = true`).
- `ilkOgreticiAkisi` (Android currentTutorialNumber == 1 ve oturum yok): dersin çıkışı gizli;
  `sonucDevam` (başarılı ve başarısız) `ilkDerstenSonraGiriseGec()` → katmanlar kapanır, kısım 1 listesi
  `tutorial1Pending` ile kurulur, giriş akışı başlangıç ekranından açılır.

---

## 3. Mimari kararlar

1. **İçerik ve kurallar ortak Kotlin'de, Swift'te değil.** Kural: mantık Swift'te yeniden yazılmaz.
   Adım geçişleri (dokun, geri, kontrol, yanlıştan dönüş, sıfırlama sırası, geri cevap, ters
   animasyon) saf kurallardır; `jvmTest` ile Windows'ta denenebiliyor. İçerik verisi de Kotlin:
   (a) Windows'ta derlenip test ediliyor (her öğretici baştan sona oynatılıyor), (b) büyük Swift dizi
   sabitleri derleyicinin tip çıkarımını zorlar (bulutta pahalı hata), (c) Android ileride aynı
   veriyi kullanabilir.
2. **Metinler anahtar olarak tutulur** (`"tutorial1_1"`); çözümleme Swift'te `Metin.al` ile
   (`OgreticiKoprusu.cevir`). Ses de anahtardır (res/raw adı).
3. **Görünüme bağlı işlemler anlamsal veriye çevrildi.** `WidgetOperation` lambdaları →
   `OgreticiGorunurluk(android:id, değer)` ve `OgreticiOdakIslemi(sağ boşluk dp, genişlik, yükseklik,
   çapa)`. Dp değerleri betikte çözülür (`focusMarginRightPx(k)` = 19 + 45k; 0 → 19; 1 px → 0;
   `R.dimen.x` → dimens.xml).
4. **Ekran "hesaplanır", biriktirilmez.** Android işlemleri adım adım uygulayıp geri giderken bir
   kısmını tersine çeviriyor; iOS'ta bir adımın ekranı baştan o adıma kadar bütün işlemler katlanarak
   bulunur (`OgreticiKurallari.sahne`). İleri gidişte sonuç aynı; geri gidişte Android'in kaçırdığı
   durumlar da doğru çıkar. Boncuklar ise Android gibi **hareketle** sürülür (ileri: adımın hareketleri,
   geri: sonraki adımınkiler ters), çünkü abaküsün değeri adımdan değil oynatılan animasyondan gelir.
5. **Oynatıcı görünüm bilmez, iş listesi döndürür.** Her olay `[OgreticiEtki]` döndürür; Swift bunları
   sırayla yürütür (`OgreticiGorunumModeli.ilerlet`). `BEKLE` ve `bekle = true` olan `SAYI_YAZ` kuyruğu
   durdurur — Android'deki `postDelayed` zincirlerinin karşılığı.
6. **Bütün `Shared` çağrıları tek dosyada** (`OgreticiKoprusu.swift`; abaküs kancaları
   `AbakusTahta.swift`'te). Yeni Kotlin adları `Shared.h`'de olmadığı için derleme hatası çıkarsa
   yalnızca bu iki yere bakılır. Swift tarafı Kotlin sınıflarıyla çakışmayan adlar kullanır
   (`OgreticiAdimVerisi` ≠ Kotlin `OgreticiAdimi`).
7. **Abaküs**: var olan `AbakusGorunumu` / `AbakusDenetleyici` yeterli; yalnızca üç EKLEME:
   `ogreticiBoncukOynat(cubuk:ust:sira:tur:bitti:)` (BeadAnimation), `ogreticiSayiyiYaz(_:bitti:)`
   (writeAnswerNumber; bitince kilit durumu geri verilir), `ogreticiMesgul`. Vurgulama Android'deki gibi
   abaküsün ÜSTÜNDE bir çerçevedir (`OgreticiOdakCercevesi`), boncuk başına vurgu yok. Kilitli adımda
   abaküs dokunmayı almaz, dokunuş ekrana geçer ve adımı ilerletir (abacusTapBlocker).
8. **Ses** protokol arkasında, sahte uygulamayla. Gerçek çalıcı ve 1092 mp3 ayrı iş (Paket F).
9. **İçerik yoksa çökme yok**: `OgreticiKatalogu.icerik` null → `OgreticiBaslangici.icerikVar` false →
   yönetici eski yer tutucuyu açar ("Öğreticiyi geç, derse başla"). `OgreticiEkrani` de içeriksiz açılırsa
   kendi basit yer tutucusunu gösterir. Android'deki `else -> tutorialSteps` (bilinmeyen numarada 1.
   öğretici) bilerek alınmadı.

### 3.1 Bilinçli farklar (Android'den)

- Geri gidişte ekran hesaplanır (karar 4). Örnek: 1. öğreticide 3. adımdan 2.'ye dönünce abaküs gizlenir;
  Android'de görünür kalıyordu (`setupBackButton`'daki "abaküs görünmezse görünür yap" yaması).
- Son adım abaküslü bir soruysa Android `currentStep`'i dizinin dışına çıkarıyor; burada son adımda
  kalınır ("Devam Et" zaten görünür).
- Gömülü olmayan / kapsam dışı: Sobi düğmesi ve dürtmeleri, öğretmene sor, ölçüm, öğretici sonu anketi,
  "Ne zorladı?" anketi, ipucu paneli (Android'de de gizli: `infoRequest` GONE), kısa ekran ölçeklemesi,
  RTL denklem yalıtımı (`RtlSupport.isolateEquations`).
- Yanlış paneli simgesi: Android sistem görseli `@android:drawable/ic_delete`; iOS'ta SF Symbol
  `xmark.circle.fill` (#FF4B4B).
- Metin satır aralığı: Android `lineSpacingMultiplier 0.5 + extra 8dp`; iOS'ta varsayılan aralık.

### 3.2 İçerik dönüştürücüsü

`node ios/tools/ogretici-donustur/donustur.js <no> [...]` Android dosyasını okur (değiştirmez), her
`TutorialStep(...)`'i ayrıştırır ve `icerik/Ogretici<no>.kt` yazar. `--rapor` bütün öğreticiler için
adım sayısı ve uyarıları basar, `--kuru` yazmadan ekrana basar. Tanımadığı yapıda adımın üstüne
`// UYARI:` koyar ve çıkış kodu 2 olur.

**Durum: 34 öğreticinin hepsi uyarısız dönüşüyor.** Deneme olarak hepsi üretildi: JVM testleri
(kimlik biçimi + her öğreticinin doğru cevaplarla baştan sona oynanması) ve Kotlin/Native derlemesi
geçti; sonra 1. öğretici dışındakiler yer tutucuya geri alındı (paketler kendileri üretip doğrulayacak).

---

## 4. Kalıp: bir öğretici nasıl taşınır

### 4.1 İçeriği üret

```sh
node ios/tools/ogretici-donustur/donustur.js 3          # icerik/Ogretici3.kt yer tutucunun üzerine yazılır
```

Üretilen adım (1. öğreticiden, Android satır 2229-2243):

```kotlin
// 25 · satır 2229
OgreticiAdimi(
    metin = "tutorial1_109",           // getString(R.string.tutorial1_109)
    ses = "tutorial1_109",             // R.raw.tutorial1_109
    daktilo = true,
    daktiloHiziMs = 40L,
    kimlik = "t1_1_asagidaki",         // id
    soru = "1",                        // questionText
    soruGorunurlugu = GORUNUR,         // questionTextVisibility = View.VISIBLE
    ilerlenebilir = false,             // nextStepAvailable
    abakusTiklanabilir = true,         // abacusClickable
    cevap = 1,                         // answerNumber
    abakusSifirla = true,              // abacusReset
    sonrakindeSifirla = true,          // nextStepAbacusReset
    ipucu = "tutorial_ipucu_001",      // requestText
),
```

Boncuk ve odak örneği (Android 2088-2117 ve 2118-2130):

```kotlin
OgreticiAdimi(
    metin = "tutorial1_14", ses = "tutorial1_14", daktilo = true, daktiloHiziMs = 40L,
    odak = listOf(
        OgreticiOdakIslemi(sagBoslukDp = 0f),                       // AnimateMargin(toMarginRight = 1 px)
        OgreticiOdakIslemi(capa = CAPA_ALT_KILAVUZ),                // ChangeConstraints(topToTop = guideline2)
        OgreticiOdakIslemi(genislikDp = 270f, yukseklikDp = 180f),  // AnimateSize(column_highlight_width, bottom_bead_band_height)
    ),
),
OgreticiAdimi(
    metin = "tutorial1_15", ses = "tutorial1_15", daktilo = true, daktiloHiziMs = 40L,
    hareketler = listOf(OgreticiBoncukHareketi(cubuk = 4, ust = false, sira = 0, tur = 1)), // rod4_bead_bottom1, yukarı
    gorunurlukler = listOf(OgreticiGorunurluk("focusView", YOK)),
),
```

Alanların Android karşılıkları `OgreticiAdimi.kt`'de tek tek yazılı. Çubuk 4 = birler (en sağ);
alt boncuk `sira` 0 = bariyere en yakın ("bottom1"); tür 1 alt yukarı, 2 alt aşağı, 3 üst aşağı, 4 üst
yukarı.

### 4.2 Gözden geçir

Üretilen dosyayı Android satırlarıyla adım adım karşılaştırın (her adımın başında `// N · satır L`).
Özellikle: kural tablosu (`kuralTablosu`), `odak` sırası, `gorunurlukler`'deki görünüm adları
(fragment_tutorial.xml android:id'leri), metin anahtarlarının `tr.lproj/Localizable.strings`'te
bulunması:

```sh
grep -o 'metin = "[a-z0-9_]*"\|ipucu = "[a-z0-9_]*"' ios/shared/src/commonMain/kotlin/com/sorobit/shared/ogretici/icerik/Ogretici3.kt \
  | sed 's/.*"\(.*\)"/\1/' | sort -u | while read k; do grep -q "^\"$k\"" ios/iosApp/iosApp/tr.lproj/Localizable.strings || echo "YOK: $k"; done
```

(seçenek anahtarları için `secenekler = listOf(...)` satırlarına da bakın.)

### 4.3 Test

```sh
cd ios && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :shared:jvmTest --tests "com.sorobit.shared.ogretici.*"
```

`OgreticiOynaticiTest` içerik üretildiği anda o öğreticiyi de kapsar: soru ve seçenek adımlarında kimlik
biçimi (Android buildStepAnalyticsInfo kuralı) ve **doğru cevaplarla baştan sona oynanabilirlik**
(çıkmaz adım yok). Paket kendi öğreticilerine özgü denetimleri **kendi** test dosyasına yazar
(`OgreticiPaketATest.kt` …): adım sayısı, soru cevapları, kural tablosu, birkaç adımın sahnesi
(`OgreticiKurallari.sahne`) ve başlangıç değeri (`OgreticiKurallari.baslangicDegeri`). Başarıyı çıkış
kodundan anlayın. Kotlin/Native denetimi: `./gradlew :shared:compileKotlinMingwX64`.

### 4.4 Ekranda gör

Galeri genel biçimi kabul eder: `-ekran ogretici-<no>-<adım>` (adım 0'dan; abaküs o adıma gelinceki
sayıya kurulur). Paket kendi adlarını raporda listeler; Codemagic döngüsüne (`codemagic.yaml`) ve
`EkranGalerisi.adlar`'a birleştiren oturum ekler (paylaşılan dosyalar). 1. öğreticinin adları:
`ogretici-1` (0, ilk açılış), `ogretici-2` (6, odak onlar sütununda), `ogretici-3` (15, boncuk
animasyonu, abaküs 2'den), `ogretici-4` (24, "1" sorusu), `ogretici-5` (29, "Devam Et").

### 4.5 Yeni bir adım türü / alanı gerekirse

Sıra (her adımda sahiplik notuna bakın):
1. **Model**: `OgreticiAdimi`'ye varsayılanlı alan (`ogretici/OgreticiAdimi.kt` — paylaşılan dosya; değişikliği
   raporda belirtin, var olan alanların anlamını değiştirmeyin).
2. **Betik**: `donustur.js`'te `adimCoz` içindeki `switch (ad)`'a dal ve `ALAN_SIRASI`'na ad. Betik
   paylaşılan dosyadır; mümkünse üretilen `.kt`'yi elle düzeltip başına not düşün.
3. **Kural**: ekranı etkiliyorsa `OgreticiKurallari.sahne`, geçişi etkiliyorsa `OgreticiOynatici` + test.
4. **Köprü**: `OgreticiKoprusu.swift`'te `OgreticiAdimVerisi` / `OgreticiSahneVerisi`'ne alan ve `cevir`.
5. **Görünüm**: paketin kendi dosyasında.
Bugünkü 34 öğretici için 1-4 GEREKMİYOR: bütün Android alanları modelde ve köprüde var. Paketlerin işi
görünüm (5) ve doğrulama.

---

## 5. Swift tarafında hazır olanlar ve olmayanlar

| Parça | Durum |
|---|---|
| Metin + daktilo (dokununca tamamlanır), ses çağrısı | var |
| Soru kutusu, renkli harfler | var |
| Abaküs (kilitli / açık), boncuk animasyonları, sıfırlama, geri cevap yazma, ortadan açılış | var |
| Odak çerçevesi (sütun, bütün abaküs, üst / alt bant, üst boşluk) | var (genel geometri; 1. öğretici dışındaki ölçüler gözle doğrulanmadı) |
| Kontrol Et, doğru / yanlış panelleri, ipucu | var |
| Geri, Eğitimi atla (geri basınca beliren dahil), çıkış (400 ms kayma), Devam Et | var |
| Kural metni ("[işaretli]" mavi) | basit: tablonun altında (çapa yok) |
| Seçenek paneli | **yok** → Paket A. Yoksa seçenekli adımda dokunma ilerletmez (Android kuralı) |
| Kural tabloları (7) | **yok** → Paket B/C/D (seçici hazır) |
| Kör ders görselleri, tutorialText'in görsele bağlanması | **yok** → Paket E |
| Gerçek ses çalıcı + mp3'ler | **yok** → Paket F |

---

## 6. İş paketleri

Her paket: içerik dosyaları (`icerik/OgreticiN.kt`, yalnız kendi numaraları), kendi görünüm
dosyaları, kendi test dosyası. **Ortak dosyalara dokunmaz**: `OgreticiAdimi.kt`, `OgreticiSahnesi.kt`,
`OgreticiOynatici.kt`, `OgreticiKatalogu.kt`, `OgreticiKoprusu.swift`, `OgreticiGorunumModeli.swift`,
`OgreticiEkrani.swift`, `OgreticiParcalari.swift`, `donustur.js`, `Yonetim/`, `codemagic.yaml`
(galeri adlarını raporda verin). Bir değişiklik gerçekten gerekiyorsa raporda önerin.

Görünüm bağımlılıkları: seçenek paneli (A) 16 öğreticide, kural metni çapası (B) 6, 14-20, 105'te
gerekiyor. Paketler paralel yürür (dosyaları ayrı); **ekranda tam doğrulama ilgili görünüm paketi
birleşince** yapılır. İçerik ve Kotlin testleri bağımsızdır.

### Paket A — Seçenek paneli + tablosuz toplama öğreticileri
- Öğreticiler: **100, 101, 2, 102, 103, 10, 21, 22, 23** (+ kullanılmayan **999**) — 253 adım.
  Android satırları: 100: 2313-2655, 101: 2656-3133, 2: 3134-3334, 102: 3335-3630, 103: 3631-4036,
  10: 9625-9884, 21: 16029-16299, 22: 16300-16733, 23: 16734-17112, 999: 12800-12815.
- Görünüm: `Ogretici/OgreticiSecenekPaneli.swift` (yer tutucuyu doldur; gerekirse yeni
  `OgreticiSecenek*.swift`). Android: `optionsPanel` (fragment_tutorial.xml 2259-2329),
  `item_tutorial_option.xml`, `TutorialOptionsAdapter.kt`, `updateOptionsPanelForStep` (17909: adım
  gösterildikten 1 sn sonra alttan kayar, bu sürede gizli; açıkken geri ve "Eğitimi atla" kilitli),
  `syncOptionsContentTextSizeStep` (bütün şıklar aynı yazı boyutunda), görseller
  `tutorial_option_indicator_on/off`. Model hazır: `model.adim.secenekler / cokluSecim /
  secenekBasligi`, `model.secilenSecenekler`, `model.secenekleriKontrolEt()`; "önce seç" uyarısı hazır.
- Eksik olabilecek: tek seçimde işaretlemenin davranışı (adapter'dan), panel açıkken soru metninin yeri.
- Test: `OgreticiPaketATest.kt`.

### Paket B — Toplama kural tabloları + kural alanı yerleşimi
- Öğreticiler: **3, 4, 5, 6, 7, 105** (+ kullanılmayan **104**) — 376 adım. Satırlar: 3: 4037-4860,
  4: 4861-5159, 5: 5160-5950, 6: 5951-6778, 104: 6779-7359, 7: 7360-7724, 105: 7725-8432.
- Görünüm: `OgreticiKuralTablolari1.swift` (`fiveRuleTable` 36-216, `tenRuleTable` 399-617,
  `tenRuleTableLinearLayout` 651-831) ve `OgreticiKuralAlani.swift` (yerleşim sahibi): tablo ölçeği
  (applyTutorialViewScaling 0,94 / kısa ekran 0,58, fitRulesTable), kural metninin çapası
  `kuralMetniCapasi` (116 adımda `guide_line_2_7` = ekran yüksekliğinin %22,57'si; ötekilerde tablo),
  105'te kural metni `tenRuleTableLinearLayout`'un altında (updateRulesStepText), `table_cell_border`.
- Bağımlılık: 3, 4, 5, 7 seçenekli → A.
- Test: `OgreticiPaketBTest.kt`.

### Paket C — Boncuk ve çıkarma tabloları
- Öğreticiler: **8, 9, 11, 1100, 12, 13** — 279 adım. Satırlar: 8: 8433-9198, 9: 9199-9624,
  11: 9885-10243, 1100: 10244-10817, 12: 10818-11367, 13: 11368-11747.
- Görünüm: `OgreticiKuralTablolari2.swift` (`BeadRuleTable` 832-1099, `extractionFiveRuleTable`
  217-398, `tenRuleExtractionTableLayout` 1100-1419).
- Bağımlılık: 8, 9, 11, 1100, 12 seçenekli → A; tablo yerleşimi → B.
- Not: 8'de abaküs sorusu yok, 80 adımın 44'ünde tablo gizleniyor (rulesPanelVisibility).
- Test: `OgreticiPaketCTest.kt`.

### Paket D — Kural metni ağırlıklı çıkarma öğreticileri
- Öğreticiler: **14, 15, 16, 17** — 180 adım. Satırlar: 14: 11748-12799, 15: 12816-13259,
  16: 13260-13945, 17: 13946-14276.
- Görünüm: `OgreticiKuralTablolari3.swift` (`BeadRuleExtractionTable` 1420-1686).
- Özel: 14'teki `onStepComplete { resetAbacus() }` → `gelinceSifirla` (betik çeviriyor; oynatıcıda var);
  16 tabloları `widgetVisibilityMap` ile açıp kapatıyor (fiveRuleTable, tenRuleExtractionTableLayout,
  BeadRuleExtractionTable — seçici hazır, tablolar B/C/D'de).
- Bağımlılık: 14, 16 seçenekli → A; kural metni çapası → B; 14 ve 16 tabloları → C.
- Test: `OgreticiPaketDTest.kt`.

### Paket E — Çarpma ve kör ders anlatımları
- Öğreticiler: **18, 19, 20, 24, 25, 26** — 152 adım. Satırlar: 18: 14277-14766, 19: 14767-15298,
  20: 15299-16028, 24: 17113-17239, 25: 17240-17292, 26: 17293-17321.
- Görünüm: `OgreticiGorselAlani.swift` (`blinding_tutorial1..3`, `rulesBookTutorial`; 245 × 520, alt
  ortada; DİKKAT: Android'de `blinding_tutorial2` id'li görünümün zemini `blinding_tutorial1`
  görseli ve tersi). 24'te `tutorialText` görselin üstüne bağlanır: `model.adim.yerlesimler`
  ("tutorialText:bottomToTop=blinding_tutorial1;bottomToBottom=UNSET") — metin alanının yüksekliği
  `OgreticiEkrani.swift`'te; gerekirse görsel alanı metni kendi içinde çizebilir (öneriyi raporda verin).
  24-26'da abaküs zaten gizli (sahne).
- Bağımlılık: 18, 19, 20 seçenekli → A; kural metni çapası → B.
- Test: `OgreticiPaketETest.kt`.

### Paket F (isteğe bağlı) — Ses
- `OgreticiSesCalici`'nin gerçek uygulaması (AVAudioPlayer; yeni dosya `Ogretici/OgreticiSesCalar.swift`),
  `res/raw/tutorial*.mp3` (Türkçe, ~1092 adım) + `correct_answer_sound`, `incorrect_answer_sound`
  kopyası (asset/kaynak kararı kullanıcıda: boyut), ayarlar (`tutorial_sound_enabled`, `sound_enabled`).
  Bağlama: `OgreticiEkrani(… ses: …)` parametresi hazır; Yonetim'de tek satır.

---

## 7. Derlenmeden doğrulanamayanlar (ilk Codemagic turunda bakılacaklar)

- Yeni Kotlin adlarının Swift'teki biçimi (Shared.h'de yok, kurallardan türetildi):
  `OgreticiKatalogu.shared.oynaticiKur(numara:ilkAcilis:baslangicAdimi:)`, `varMi(numara:)`;
  `OgreticiOynatici.basla() / dokunuldu(mesgul:daktiloSuruyor:) / geri(mesgul:) / kontrol(deger:) /
  secenekKontrol(secilenler:) / dogruDevam() / yanlisTamam() / sahne() / adim / adimSayisi / icerik`;
  `OgreticiIcerigi.adim(sira:) / kuralTablosu`; `OgreticiEtki.companion.ADIM_GOSTER …`;
  `OgreticiSabit.shared.GORUNUR / YOK / CAPA_UST_KILAVUZ / CAPA_ALT_KILAVUZ`;
  `OgreticiSahnesi.gorunurluk(gorunum:varsayilan:) / degisenGorunumler()`;
  `OgreticiAbakusKoprusu.shared.oynat(denetleyici:host:cubuk:ust:sira:tur:bitti:)`,
  `sayiyiYaz(denetleyici:host:deger:bitti:)`. Hepsi `OgreticiKoprusu.swift` ve `AbakusTahta.swift`'te.
- `OgreticiRenkliHarf.renk` (Kotlin Long → Int64) ve `dogruSecenekler` (`[KotlinInt]`).
- SwiftUI: dokunma yakalayıcı katman ile açık abaküsün (UIViewRepresentable) birlikte çalışması;
  odak çerçevesinin abaküsle hizası (dp → pt birebir varsayıldı); daktilonun `Text + Text` ile
  çizilmesi ve `minimumScaleFactor`.
- Ekranda hiç görülmedi: bütün görünüm (Windows'ta önizleme yok).

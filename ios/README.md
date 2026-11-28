# Sorobit iOS

Bu klasör, Android uygulamasından **tamamen bağımsız** bir Gradle projesidir. Kök `settings.gradle.kts` bu klasörü içermez; Android derlemesi buradan etkilenmez ve Android `ios/shared` modülüne bağlanmaz. Buradaki Kotlin kodu, `app/` içindeki mantığın **kopyasıdır**: Android'de bir kural değişirse aynı değişiklik burada elle yapılmalıdır.

> **27.11.2026:** ortak kod (`shared/`) güncel Android'e (`claude/soban-gamified-education-prb70m`, versionCode 26) eşitlendi ve Swift ekranları buna göre iki turda yeniden yazıldı. Eski (Eylül) Android ile yeni Android arasındaki farkların dökümü: `docs/ANDROID_FARK_ENVANTERI.md`; eşitleme sırasında Swift'te kırılan çağrılar: `docs/SWIFT_KIRILMA_LISTESI.md`.

```
ios/
  shared/   Kotlin Multiplatform; Android mantığının kopyası (Aşama 1)
  iosApp/   SwiftUI ekranları (Aşama 2 — sürüyor; kurallar: iosApp/GELISTIRME.md)
  docs/     Shared.h (ortak kodun Swift'e görünen adları, 14. Codemagic derlemesinden) ve Android fark belgeleri
  tools/    Android kaynağından katalog/altın veri üreten betikler
```

## Durum (28.11.2026)

### Ortak kod (`shared/`)

- Yaklaşık 155 Kotlin dosyası `commonMain` içinde, testlerle birlikte `shared/src` altında yaklaşık 227 (toplam yaklaşık 73 bin satır; bunun yaklaşık 16,3 bin satırı üretilmiş öğretici içeriği, `ogretici/icerik/`). Paketler: `abakus`, `baglanti`, `ders`, `dersadim`, `kupa`, `matematik`, `model`, `ogretici`, `oyun`, `profil`, `seri`, `sobianaliz`.
- **JVM'de 1104 test, Kotlin/Native (mingwX64) 1098 test, ikisinde de 0 hata** (sayılar `shared/build/test-results/<hedef>/*.xml` dosyalarından toplandı; son tam çalıştırma 28.11.2026, kural/rehber/mağaza ortak koda taşınıp İngilizce eklendikten sonra). JVM'deki 6 fazlalık yalnızca JVM'de koşan testlerden (`java.util.Random` karşılaştırması, avatar paketi dosyası vb.) gelir.
- `commonMain` içinde `java.*`, `android.*`, Firebase, Gson yok. Bağımlılıklar: Kotlin stdlib, kotlinx-datetime 0.6.1, kotlin-test.
- **iOS hedefleri bu bilgisayarda derlenemez** (Windows). Ortak kod gerçek iOS hedefinde Codemagic'te derlendi ve simülatör testleri geçti (26.11.2026, eşitlemeden önceki kodla); eşitlenmiş kod için iOS hedefli test çalıştırması yapılmadı, yalnızca uygulamanın derlemesi (aşağıda) ortak kodu iOS framework'ü olarak kullandı.

### Uygulama (`iosApp/`)

- Yaklaşık 196 Swift dosyası (başka oturumlar dosya eklemeye devam ediyor), ekran başına bir klasör (`Abakus/`, `Giris/`, `Gorevler/`, `GunlukSoru/`, `Harita/`, `Kabuk/`, `KorDers/`, `Kupa/`, `Kurallar/`, `Magaza/`, `Maskot/`, `Ogretici/`, `Ortak/`, `Ozellestirme/`, `Plan/`, `Profil/`, `Rehber/`, `Sandik/`, `Seri/`, `SobiAnaliz/`, `Sonuc/`, `Tema/`, `Yonetim/`). `Yonetim/` ekranları birbirine bağlar.
- Kaynaklar (güncel Android'den): metinler 3609 anahtar (yalnızca `tr.lproj`; araç en/id/ar da üretebilir), yaklaşık 270 görsel, 92 renk, uygulama simgesi (`design/sobi_ikon_kaynak.png`'den 1024), 43 Lottie dosyası, ses: `Sesler/Ogretici/` 854 dosya + `Sesler/Efektler/` 10 dosya.
- **Derleme durumu:** son doğrulanan derleme 19. (commit `f827298`, 28.11.2026): **0 hata ile derlendi**; Sobi hata analizi İngilizcede tamamen İngilizce (teşhis, sütun ve kural adları), Türkçe galeri `-dil tr` ile yeniden Türkçe, açılış günlüğü sağlıklı. 18. derleme (`d2d1c1c`) İngilizce dil desteğini, kural/rehber/mağaza ortak kodunu ve arka plan seslerini ilk kez derledi. Doğrulanan yalnızca derleme, durağan görüntü ve açılış günlüğüdür: akışlar tıklanarak denenmedi, sesler dinlenmedi.
- **Giderilen gerileme (13.–16. derlemeler):** uygulama gerçek akışla açıldığında ilk ekran boş kalıyordu. Neden: öğretici ses çalıcısı ses oturumunu ve `AVAudioPlayer`'ı ana iş parçacığında kuruyordu, ses sunucusunu beklerken daktilo yazısı başlamıyordu. Çalıcı seri bir arka plan kuyruğuna taşındı (`SesOturumu.kuyruk`); 17. derlemede açılışın 3., 8. ve 15. saniye karelerinde öğretici görünüyor, günlükte ses oturumu arka planda 339 ms'de kuruluyor. Ses efektleri (`SesCalar`) de 18. derlemeden beri aynı kuyrukta; açılış günlüğünde ses oturumu arka planda 407 ms de kuruldu.
- **Her derlemede bakılacak açılış gözlemi:** `build/ekran-03.png`, `ekran.png` (8. sn), `ekran-15.png`, `ekran-oturum.png` (`-oturum acik` ile kabuk), `ilk-acilis.log` (`[sorobit]` satırları, `os.Logger` alt sistem `com.sorobit.app`, kategori `akis`) ve `ilk-acilis-ornek.txt` (8. saniyede ana iş parçacığı yığını).
- **İlk tur (9. derleme, doğrulandı: derlendi, görüntüler alındı):**
  - Harita: ders kartları, karta bağlı panel, anahtarla kilit açma.
  - Kabuk: yeni sekmeler, üst çubukta seri göstergesi.
  - `Maskot/`: Android `BunnyMascotView`'ın 48 hâli SwiftUI `Canvas` ile çizildi (`SobiMaskotGorunumu(emote:animasyonlu:)`).
  - Sonuç: maskot sahnesi, göçen düğme (`Ortak/GocenDugmeStili`). Sandık güncellendi. Kristal ekranı ve ders sonundaki rastgele Lottie'ler silindi.
- **İkinci tur (commit `e0a2fb6`, sonraki derlemelerde derlendi; durağan görüntüler alındı):**
  - Abaküs ve kör ders: sayı kartı, göçen düğmeler, abaküs ayar paneli; "Sobi ile analiz et" girişi yer tutucuya gider.
  - Görevler sekmesi tek ekran (görevler + günlük soru + abaküs kartı).
  - Kupa: sunucu taraflı puan, `KupaYoluOdulEkrani`, sandıklı kartlar.
  - Mağaza: yeni kart sırası, Sobi kredisi, seri dondurma, yeni ürün kimlikleri.
  - Giriş: yeni açılış ve başlangıç, kayıt soruları, 13 yaş altı veli onayı.
  - Profil: Personas avatar sistemi (`Profil/Avatar/`: `AvatarGorunumu(config:)`, `AvatarDuzenleEkrani`), takip, en uzun seri.
- **Öğretici (`TutorialFragment`, 34 öğretici, 1270 adım):** motor ve içerik ortak kodda (`shared/.../ogretici/`: oynatıcı durum makinesi, sahne hesabı, abaküs köprüsü; içerik `icerik/OgreticiN.kt`, `ios/tools/ogretici-donustur/donustur.js` ile Android'den üretildi, 34'ü de uyarısız). Adım sayıları, metin/ses/ipucu/kimlik anahtarları, boncuk animasyonları, seçenekler ve doğru şıklar Android kaynağıyla betikle karşılaştırıldı; kullanılan 997 metin anahtarının hepsi `tr.lproj/Localizable.strings`'te var. Her öğretici doğru cevaplarla baştan sona oynanıyor (testli). Ayrıntı: `docs/OGRETICI_PLANI.md`.
  - **Swift görünümleri (`iosApp/Ogretici/`) 12. derlemede (commit `b2e1d49`) 0 hata ile derlendi:** 34 öğreticinin tamamı (1270 adım), seçenek paneli, 7 kural tablosu ve kör ders görselleri; simülatör görüntüleri alındı. 24. öğreticide metin–görsel hizası düzeltildi (commit `137b656`) ve 13. derlemede görüntüyle doğrulandı.
  - **İlk açılış** artık 1. öğreticiyle başlıyor (hesapsız ilk açılış → 1. öğretici → ders → giriş).
  - **Ses:** öğretici için gerçek çalıcı yazıldı (`Ogretici/OgreticiSesCalar.swift`, `AVAudioPlayer`, `.playback` + `.mixWithOthers`); `iosApp/Sesler/Ogretici/` altında 854 dosya (yaklaşık 45 MB, Android `res/raw` kopyası). **Ses hiç çalınıp dinlenmedi.**
- **Seri (streak, 13. derlemede derlendi, 0 hata, ekran görüntüleri alındı):**
  - Ortak kod `shared/.../seri/`: Android `StreakRepository`, `StudyTimeTracker`, `StreakFreezeRules`, `StreakMilestones` ve sunucu senkron kurallarının (`StreakSync.kt`) kopyası.
  - `iosApp/Seri/` ekranları: seri ekranı, yeni seri, kutlama şeridi, hedef ve hatırlatma pencereleri.
  - Bağlantılar: üst çubuk, ders sonrası akış, kayıt cevapları, mağazada seri dondurma, profilde en uzun seri.
  - Sahte servis (`SahteSeriServisi`) ortak kurallarla çalışıyor; depolama bellekte, uygulama kapanınca gider.
  - İlerleme çubuğu etiketleri iOS'ta beyaz yazı + koyu gölge; Android'de gölgesiz beyaz (bilinçli sapma, okunurluk). 13. derlemeden kalan etiket düzeltmesi 14. derlemede görüntüyle doğrulandı.
  - **Eksikler:** boşta kalma sınırı işlemiyor; sahte serviste ders kapanınca +300 sn ekleniyor; bildirim/hatırlatma gerçek değil (sunucuda `sendStreakReminders` FCM ile gönderir, iOS için APNs gerekecek); maskot alevi güne göre büyümüyor.
- **Sobi hata analizi (14. derlemede derlendi, 0 hata, durağan görüntü alındı):**
  - Ortak kod `shared/.../sobianaliz/`: Android `MistakeAnalyzer`, `StepExplainer`, `SubStepExplainer`, `ExamplePicker` ve `SobiRuleStats` kopyası; testli. Teşhis, sütun/kural adları ve alt adım açıklamaları Türkçe ve İngilizce (`SobiAnalizDili.language`, `Metin.diliAyarla` atar; 28.11.2026, derlenmedi). Endonezce/Arapça dalları taşınmadı.
  - `iosApp/SobiAnaliz/` penceresi; dersteki "Sobi ile analiz et" düğmesine bağlı (artık yer tutucuya gitmiyor). 14. derlemeden sonra pencereye gerçek doğru/yanlış sesi eklendi (derlenmedi).
  - Yazılmayanlar: yapay zekâya bağlı kısımlar (Sobi'ye yazma, öneriler), Endonezce/Arapça çeviriler, gerçek sesli okuma (TTS), kural istatistiklerinin sunucuya gönderimi.
- **Kural kitabı ve ders içi rehber (14. derlemede derlendi, durağan görüntü alındı):**
  - `Kurallar/`: kural kitabı, ders içi kural paneli, 1x–9x çarpım tabloları; kitap ve sihirli değnek düğmeleri. Kural seçimi mantığı ortak koddadır (`abakus/LessonRulesLogic.kt`); `KuralKurallari.swift` yalnızca Swift türlerine çevirir (28.11.2026 taşındı, derlenmedi).
  - `Rehber/`: altı ders rehberi + haritada maraton rehberi. Sayfa içeriği ve açılma kuralları ortak koddadır (`ders/GuideHelper.kt`); `RehberIcerigi.swift` / `RehberOrtak.swift` yalnızca çevirir ve kayıt deposunu okur (28.11.2026 taşındı, derlenmedi). "Gösterildi" kaydı (rehber 5) yerelde tutuluyor; Android'de Firestore `skip_step_tutorial_shown`.
- **Ses efektleri ve titreşim (14. derlemede derlendi; hiçbiri dinlenmedi/hissedilmedi):**
  - `Ortak/SesCalar.swift` (`AVAudioPlayer` havuzu) ve gerçek uygulamalar: boncuk tık sesi (5 seçenek), doğru/yanlış, sandık, rozet, ders sonucu, yıldız. `Sesler/Efektler/` altında 10 dosya (yaklaşık 0,75 MB).
  - `Ortak/Titresim.swift`: Android'deki üç yerde (alt çubuk sekmesi, avatar seçimi, rozet ayrıntısı).
  - `Profil/ProfilSesAyarlariEkrani.swift` (Android "Ses ve Bildirimler"); bildirim tercihleri yalnızca yerelde, gerçek bildirim yok.
- **Plan ve rozet kutlaması (14. derlemede derlendi, durağan görüntü alındı):**
  - `Plan/`: plan karşılaştırma ve plan seçimi; iOS'a özgü App Store öğeleri var (yenileme açıklaması, şartlar/gizlilik bağlantıları, geri yükleme, "Aboneliği yönet"). Mağazadaki Süper/Pro kartları bu ekranı açıyor. Satın alma sahte.
  - `Kupa/RozetKutlama*`: rozet kutlaması. Ders sonrası sıra: rozet → yeni seri → maraton rehberi.
- **Sürüyor (yazılmakta, tamamlandı sayılmaz):** günlük Sobi testi (`SobiTest/`, ortak kodda `shared/.../sobitest`), yarış paneli ve ustalık kısımları (`Yaris/`), kod üzerinden akış denetimi (`ios/docs/AKIS_DENETIMI.md`). Bu üçünün hiçbiri derlenmedi.
- **Hepsi sahte veriyle çalışıyor.** Firebase, StoreKit, reklam ve bildirim izni bağlı değil; her biri bir Swift protokolünün arkasında yer tutucu. Gerçek olan tek şey sesler (öğretici ve efektler) ve titreşim, ikisi de cihazda denenmedi.
- **Doğrulanmayanlar:** dokunma/sürükleme hissi ve animasyonlar (yalnızca durağan ekran görüntüsüne bakıldı), akışların tıklanarak gezilmesi, sesler, gerçek cihaz, küçük ekranlar, Android ile piksel karşılaştırması. Önceki turda kısa ekranlar için abaküs/kör ders ölçeklemesi eklendi; simülatörde yalnızca büyük ekranda görüldü.
- Yaklaşık kalanlar: kilitli rozet/kupa kartlarının düz gri boyanması ve kademe tonları (SwiftUI karışım kipleriyle), mor ton (Android OVERLAY).
- **Ekran galerisi:** uygulama `-ekran <ad>` argümanıyla açılırsa o ekranı örnek veriyle gösterir (`Yonetim/EkranGalerisi.swift`). Codemagic her derlemede 83 adın hepsinin görüntüsünü çıktı olarak bırakır (`codemagic.yaml` içindeki liste galeriyle aynı tutulmalı).
- Android'le uyumlu olduğu doğrulanan davranışlar: gri boncuklar Android varsayılanıdır (soroban, `#737373`); Android'de misafir girişi yoktur (yalnızca hesapsız ilk öğretici); hesap silme iki platformda da Profili Düzenle'dedir.

### Henüz yazılmayanlar

Android'de var, iOS'ta yok:

- **Öğreticide yazılmayanlar:** Sobi düğmesi ve dürtmeleri, ölçüm, öğretici sonu anketi, ipucu paneli, kısa ekran ölçeklemesi (öğretici ve seri artık buradan çıktı; durumları "Uygulama" bölümünde).
- **Sobi sohbeti** (sunucuya bağlı; Sobi analizindeki yapay zekâ kısımları, gerçek sesli okuma ve kural istatistiği gönderimi de aynı sebeple yok).
- **Günlük Sobi testi ve yarış paneli:** yazılıyor (yukarıda "Sürüyor"), tamamlanmadı.
- **Çoklu dil (28.11.2026, henüz derlenmedi):** Türkçe + İngilizce açık (`Metin.acikDiller`). `tr.lproj` ve `en.lproj` (`donustur.js --diller tr,en`; en 3457 anahtar, eksik 152'si dilden bağımsız, çalışırken Türkçe'ye düşer) + elle yazılan `IosOzel.strings` (`ios_` önekli iOS'a özgü metinler). Seçici Profil > Hesap Ayarları > Dil (varsayılan telefonun dili; desteklenmeyen dil → İngilizce), seçim UserDefaults `app_language_choice`, değişince kök görünüm yeniden kurulur (`Metin/MetinDilDurumu.swift`). Ders kataloğu Türkçe kalır, gösterirken `Metin.ders` çevirir (`Metin/DersMetinAnahtarlari.swift`, `tools/ders-katalog/uret.js` 5. argümanla üretir); ortak koddan gelen Türkçe sabitler için `Metin.cevir` (tr.lproj'dan ters arama). Galeri `-dil en` alır. Endonezce/Arapça: `acikDiller`'e ekle + `--diller` ile üret; Arapça için ayrıca sağdan-sola düzen ve yazı tipi.

İlk iOS sürümünün dışında bırakılanlar (kapsam kararı): haftalık kart, ödev/sınıf/kurs/öğretmen, veli paneli, davet, Hata Avcısı.

(Önceki sürümde burada sayılan Sobi hata analizi, kural paneli/kitabı, ders içi rehber, plan ekranı, rozet kutlaması, ses efektleri ve ses ayarı ekranı artık yazıldı; durumları "Uygulama" bölümünde.)

### Hâlâ sahte olanlar

- **Firebase:** giriş, Firestore, Cloud Functions, bildirimler (APNs).
- **StoreKit** (satın almalar; plan ekranındaki App Store öğeleri yalnızca arayüz).
- **Reklam.**
- **Sesli okuma.**

Hepsi protokol + `Sahte…` uygulaması olarak duruyor; gerçek bağlantı için bkz. "Verilen kararlar".

**Firebase bağlanınca seri için gereken sözleşme** (sunucu zaten var, `functions/` değişmez): Firestore'da `users/{uid}/streak/state` belgesi okunur; yazım Cloud Function'larla yapılır: `submitStreakDay` (günleri gönderir, güncel durumu döndürür), `buyStreakFreeze` (seri dondurma satın alma), `claimStreakReward` (kilometre taşı ya da meydan okuma ödülü). İstek/yanıt alanlarının ayrıntısı `shared/src/commonMain/kotlin/com/sorobit/shared/seri/StreakSync.kt` başlığında. Hatırlatma bildirimi için sunucudaki `sendStreakReminders` FCM kullanıyor; iOS'ta APNs ayrıca kurulmalı.

### Ortak koda taşınması gerekenler

Şu an Swift'te kopya olarak duran ya da ortak kodda eski kalan şeyler; Android değişirse iki yerde güncellemek gerekir:

- **`LessonAdapterLogic.lessonBottomSheetState.fireAnimVisible`** hep false; Android'de ateş Lottie'si rekor satırının çocuğu, satır görünürken görünür (`recordText != null && !recordLayoutGone` karşılığı). Swift bu alanı okumuyor, ateşi rekor satırıyla birlikte çiziyor; yani davranış doğru, alan yanıltıcı. Dosya Türkçe metin ürettiği için çeviri oturumu bitmeden dokunulmadı.
- **`MagazaStil.gunlukAnahtarCanSiniri`** (5): sunucu sabiti `ENERGY_KEY_BUY_DAILY_LIMIT`, yalnızca sahte enerji servisinde kullanılıyor; Android istemcisinde karşılığı yok (sunucu bildiriyor), ortak koda alınacak bir istemci kuralı değil.

28.11.2026'da taşınanlar (Swift kopyaları silindi; Swift tarafı derlenmedi): kural paneli/kitabı (`abakus/LessonRulesLogic.kt`), ders içi rehber sayfaları ve açılma koşulları (`ders/GuideHelper.kt`), `SubscriptionCta` etiketi (`oyun/SubscriptionCta.kt`; mağaza ve plan ekranı `MagazaAbonelikCta` üzerinden), mağaza sabitleri güncel Android'e eşitlendi (`ShopFragmentLogic`: can bedeli 1, kart miktar/yedek fiyatları, Pro yedek fiyatı, yeni `SOBI_CREDIT_CARDS`; seri dondurma bedeli `StreakRules.FREEZE_COST_GOLD`), gün kimlikleri ortak koddan (`CupHistoryRepository.todayId`, `kupaCalendarDayId`). Silinen ölü kod: `CrystalBreakLogic`, `ChestResultLogic.animations`, `SonucEkraniKoprusu.animasyonlar/rastgeleAnimasyon`. `ServerRewards.CrystalOutcome` ölü DEĞİL: Android görev ödülü (`MissionRewardRevealDialogFragment` → `ServerRewards.openCrystal`) hâlâ kullanıyor; Firebase bağlanınca iOS'ta da gerekecek.

Gün kimliği notu: iki ayrı biçim var ve tek kurala inmez — `yyyy-MM-dd` (`CupHistoryRepository.todayId` = `StreakDays.dayId(…, 0)`; `submitCupResult` ve seri) ile `yıl-yılınGünü` (`kupaCalendarDayId`, Android `MissionsProgressStore.calendarDayId`; roket rozetinin günlük sayacı Firestore'a bu biçimde yazılıyor). İkisi de ortak kodda; Swift artık kendisi üretmiyor.

### Kullanıcıdan beklenenler

- **Apple Developer hesabı** (imzalama, TestFlight, App Store Connect için).
- **Kesin paket kimliği:** şu an geçici `com.sorobit.app`; Android'de `com.numigo.app`.
- **Firebase konsolunda iOS uygulaması** eklenmesi ve `GoogleService-Info.plist` dosyasının verilmesi.
- **App Store Connect ürünleri:**
  - Tüketilebilir: `gold_small`, `gold_medium`, `gold_large`, `keys_small`, `keys_medium`, `keys_large`, `sobi_credits_20`, `sobi_credits_50`, `sobi_credits_120`.
  - Abonelik: `pro_monthly`; isteğe bağlı `lite_monthly`.

## Derleme ve test (Windows)

PATH'teki Java 24 Gradle 8.9 ile çalışmaz; Android Studio'nun JDK'sı kullanılır:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; cd ios; .\gradlew.bat :shared:allTests
```

Hızlı döngü için `:shared:jvmTest`. Sonuç satırı Türkçe yerelde "BUİLD SUCCESSFUL" çıkar; başarıyı çıkış koduyla anlayın.

## Neler kopyalandı

Kök paket `com.sorobit.shared`. Android yolları `app/src/main/java/com/example/app/` altındadır. Her ortak dosyanın başında geldiği Android dosyası yazılıdır; sınıf, fonksiyon ve alan adları eşleştirmeyi kolaylaştırmak için Android'dekiyle aynı tutuldu.

### `matematik` — soru üreteci (66 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `MathOperationGenerator.kt` | `MathOperationGenerator.kt` — tamamı (66 açık fonksiyon), mekanik dönüşümle |
| `MathOperation.kt`, `BeadsBehavior.kt`, `RecordTimeUtils.kt` | aynı adlı dosyalar |
| `GlobalValues.kt` | `GlobalValues.kt` içindeki sayısal yardımcılar (`generateRandomNumber`, `randomNumberChangeToString`, `randomUniqueNumberStrings`, `digitRangeMin/Max`) |
| `BeadAnimation.kt` | `model/BeadAnimation.kt` — saf kısım (tür → görsel adı, yön, mesafe, süre) |
| `JavaRandom.kt`, `RastgeleKaynagi.kt` | Android karşılığı yok: `java.util.Random`'ın saf Kotlin kopyası ve enjekte edilebilir rastgele kaynak |

Doğrulama: özgün Android dosyası JVM'de referans olarak derlenip 231 durum × 400 tohum = **92.400 karşılaştırmada birebir aynı çıktı** verdi. Bu karşılaştırmadan üretilen 693 satırlık altın veri depoda (`AltinVeri.kt`) ve hem JVM'de hem Native'de doğrulanıyor.

Paket başlıklarındaki test sayıları JVM'de, 27.11.2026 eşitlemesinden sonraki değerlerdir (o günkü toplam 926; `baglanti` 44, `dersadim` 34, `matematik` 66, kök 1 ile birlikte). `seri/` ve `sobianaliz/` paketlerinin testleri bu başlıklarda sayılmadı; güncel toplam yukarıdaki "Ortak kod" bölümündedir.

### `ders` — ders kataloğu ve ilerleme (190 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `LessonItem.kt` | `model/LessonItem.kt` — tamamı (ilk alan `stableId`: dersin kalıcı kimliği, ör. `p1_i04_unite_maratonu`) |
| `GlobalLessonDataKatalog.kt` | `GlobalLessonData.createLessonItems` — 9 part, 169 item; betikle üretildi. Android ders adlarını ve ipuçlarını artık `strings_lessons.xml`'den okuyor; betik bunları varsayılan (Türkçe) metinle yerine koyar. Her item `stableId` taşır. |
| `GlobalLessonData.kt` | `GlobalLessonData.kt`'nin geri kalanı (varsayılanlar, Firestore alanları ↔ model, `updateLessonItem`, backfill, tamamlanma yüzdesi, `stableId` ile doküman yolları ve konum anahtarlı eski dokümanlardan taşıma planı, öğretmen kilit açmaları) + `LessonDataManager.kt` anahtarları |
| `LessonProgressMerge.kt`, `ChestTypeProgressHelper.kt`, `LessonManager.kt` | aynı adlı dosyalar — tamamı |
| `GuidePanelData.kt` | `model/GuidePanelData.kt` + `MapFragment.showGuidePanel` verisi |
| `GuideHelper.kt` | `GuideHelper.kt` (`getGuideContentsForNumber`, `GuideShowTracker` sınırı) + `AbacusFragment` / `BlindingLessonFragment` rehber açılış koşulları ve `optimizeBeadIdsForReverse` |
| `LessonSuccessRateRepository.kt` | kullanıcı adım durumu (`users/{uid}/lessonSuccessRateState`, `stableId` anahtarlı; eski konum anahtarlı yoldan okuma), analitik kararları. Android'de genel başarı oranı toplamları artık istemcide hesaplanmıyor. |
| `ChestFragmentLogic.kt` | `ChestFragment.kt` — `updateMapProgress`, kilit açma, `changeCupIcon` |
| `LessonResultLogic.kt` | `LessonResult.kt`, `LessonResultFalse.kt`, `ChestResult.kt` hesapları |
| `LessonAdapterLogic.kt` | `LessonAdapter.kt`, `RaceAdapter.kt` — alt panel durumu, enerji kararı, açılacak ekran |
| `MapFragmentLogic.kt` | `MapFragment.kt` (sabit başlık, `resolveLessonOperationsId`), `MarathonGuideStore.kt` uygunluk kuralı |

Katalog, Android dosyasını bağımsız bir ayrıştırıcıyla okuyan betikten üretilmiş sabit beklentilerle doğrulanıyor (part başına adet, tür dağılımı, 23 alanın parmak izi).

### `dersadim` — ders adımı → soru listesi (34 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `LessonOperations.kt` | `MapFragment.kt` companion: `getLessonOperations`, `getLessonOperationsBlinding`, iki `resolve*` — 69 + 55 dalın tamamı, mekanik dönüşümle (güncel Android'de 18 dalı kaldırıldı, 17 ve 1013 değişti) |
| `UploadLessonDataLogic.kt` | `AbacusFragment.uploadLessonData`, `BlindingLessonFragment.uploadLessonData` |

Altın veri (`DersAdimAltinVeri.kt`): değişen 17 ve 1013 dallarının tohumlu özetleri, Android referansı JVM'de çalıştırılamadığı için çıkarıldı; bu iki dal yapısal testle (soru sayısı, basamak) sınanıyor. Metinleri Android `MapFragment.kt` ile satır satır aynı (27.11.2026'da elle karşılaştırıldı).

### `kupa` — kupa, rozet, sezon, lider tablosu (149 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `CupRuleEngine.kt` | `CupRuleEngine.kt` — tamamı |
| `CupScoreService.kt`, `CupPathRewardRepository.kt`, `CupPathLogic.kt` | `CupScoreService.kt` (kupa sonucu sunucuda yazılır: `submitCupResult` yükü), `CupPathRewardRepository.kt` (kupa yolu sandıkları), Kupa Yolu paneli kuralları |
| `CupRepositories.kt` | altı `*CupRepository.kt` — alanlar, yollar, skor güncelleme hesabı |
| `CupHistoryRepository.kt`, `CupHistoryLogic.kt` | `CupHistoryRepository.kt`, `CupHistoryFragment.kt` |
| `BadgePrecalcHelper.kt`, `CupFragmentLogic.kt`, `KupaZaman.kt` | `BadgePrecalcHelper.kt`, `CupFragment.kt`; gün kimliği yardımcıları |
| `rozet/BadgeProgressEngine.kt`, `rozet/BadgePieceLeaderboardSync.kt` | aynı adlı dosyalar — tamamı |
| `rozet/BadgeProgressFirestore.kt` | `BadgeProgressFirestore.kt` — alanlar, doküman ↔ model, transaction gövdeleri, kademe atlama zincirleri |
| `rozet/BadgeFragmentLogic.kt`, `rozet/BadgeDetailLogic.kt`, `rozet/BadgeAnimMode.kt` | `BadgeFragment.kt`, `BadgeDetailFragment.kt` içindeki hesap ve metin tabloları |
| `rozet/FishingStreakPeriod.kt` | `DailyQuestionPeriod.kt` içinden rozetin kullandığı altı fonksiyon (aşağıdaki "yinelenen kod" notuna bakın) |
| `sezon/SeasonClock.kt`, `sezon/SeasonLeaderboardRewardPayloads.kt` | aynı adlı dosyalar — tamamı |
| `sezon/LessonLeaderboardRepository.kt`, `sezon/RecordLogic.kt` | `LessonLeaderboardRepository.kt`, `RecordFragment.kt`, `RecordLeaderboardAdapter.kt`. Tahta kimliği artık `part_{p}_lesson_{stableId}_season_{s}` (liste konumu değil); fotoğraf yerine `publicProfiles` avatarı. |

### `oyun` — günlük soru, görevler, sandık, enerji, cüzdan (200 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `DailyQuestionChallenge.kt`, `DailyQuestionPeriod.kt`, `DailyQuestionPoolBuilder.kt` | aynı adlı dosyalar — tamamı |
| `DailyQuestionRepository.kt`, `DailyQuestionStores.kt` | `DailyQuestionRepository.kt`, iki `DailyQuestion*Store.kt`, `TasksFragment.kt` kart kuralları |
| `MissionQuestCatalog.kt`, `MissionsProgressStore.kt` | aynı adlı dosyalar — tamamı |
| `MissionUiLogic.kt` | `MissionsFragment.kt`, `MissionChestRewardFragment.kt`, `MissionProgressLayout.kt` hesapları |
| `ChestCrystalPolicy.kt`, `NewChestLogic.kt`, `ChestResultLogic.kt` | `ChestCrystalPolicy.kt`, `NewChestFragment.kt`, `ChestResult.kt` |
| `EnergyManager.kt`, `ServerEnergy.kt`, `ServerRewards.kt`, `UserWalletFirestore.kt`, `TimeTracker.kt`, `BillingCatalog.kt` | aynı adlı dosyalar |
| `AgeConsentManager.kt`, `AppRatingManager.kt`, `AdRules.kt`, `GuideStores.kt` | `AgeConsentManager.kt`, `AppRatingManager.kt`, `AdManager.kt`, `MarathonGuideStore.kt`, `FirstTutorialShownStore.kt` — kural kısmı |
| `SubscriptionCta.kt` | `SubscriptionCta.kt` — deneme günü → düğme metni anahtarı |
| `PlanStatus.kt`, `TrustedClock.kt` | `PlanStatus.kt`, `TrustedClock.kt` + `TrustedClockRules.kt` — saf kısım |
| `OyunKeyValueStore.kt`, `OyunSaat.kt`, `OyunBagimliliklari.kt` | Android karşılığı yok: SharedPreferences karşılığı arayüz, saat, geçici bağımlılık türleri |

### `abakus` ve `model` (185 test: abakus 133, model 52)

| Ortak dosya | Android kaynağı |
|---|---|
| `abakus/AbacusBeadController.kt` | `abacus/AbacusBeadController.kt` — tamamı; görünüm yerine `AbacusBeadHost` arayüzü |
| `abakus/AbacusBeadMetrics.kt`, `abakus/AbacusPreferences.kt` | aynı adlı dosyalar (katalog, varsayılan renkler, anahtar adları) |
| `abakus/AbacusBeadRenderer.kt`, `abakus/AbacusFrameRenderer.kt` | aynı adlı dosyalar — renk eşlemeleri ve piksel değiştirme algoritması (çizim değil) |
| `abakus/AbacusCustomizationFirestore.kt`, `abakus/AbacusPurchases.kt` | `AbacusCustomizationFirestore.kt`, `BeadPurchaseFirestore.kt`, `FramePurchaseFirestore.kt`, `AbacusCustomizationFragment.kt` fiyat/stok kuralları, `AbacusSoundPlayer.kt` ses seçimi |
| `abakus/AbacusLessonLogic.kt` | `AbacusFragment.kt`, `AbacusPracticeFragment.kt` — cevap doğrulama, sayaç, puan, ders sonu yönlendirmesi |
| `abakus/BlindingLessonLogic.kt` | `BlindingLessonFragment.kt` — cevap doğrulama, kupa deltası, sayı dizisi durum makinesi |
| `abakus/LessonRulesLogic.kt` | `AbacusFragment.kt`, `BlindingLessonFragment.kt`, `abacus/LessonRulesPanel.kt`, `model/RulesFragment.kt` — kural paneli tablosu, kitap/değnek düğmeleri, kitabın tabloları ve bölücüleri |
| `model/StudentQuestion.kt`, `model/MessageReport.kt` | `model/StudentQuestion.kt`, `model/MessageReport.kt`, `model/RestrictedUser.kt`, `UserAskQuestionRestriction.kt`, `NotificationFragment.kt` yazımları |
| `model/AuthRules.kt` | `auth/AuthManager.kt`, `RegisterActivity.kt`, `LoginActivity.kt`, `EditProfileFragment.kt`, `OtpVerificationFragment.kt` — doğrulama kuralları, kullanıcı dokümanı alanları, Cloud Function adları ve gövdeleri |
| `model/SessionDeviceRules.kt` | `SessionDeviceManager.kt`, `AccountDeletionHelper.kt`, `MyFirebaseMessagingService.kt`, `AddFriendFragment.kt`, `FollowersFollowingFragment.kt` |

### `baglanti` — paketler arası köprüler (44 test)

Paketler birbirinden bağımsız kopyalandığı için bazıları komşu türlerin küçük yerel kopyalarını tanımladı. `PaketKopruleri.kt` bunları asıl türlere bağlar: günlük soru havuzu → `MathOperationGenerator`, `LessonItem` → `OyunLessonItem`, kupa dersi → part 9 yer tutucusu. `KorDersKoprusu.kt`, `SonucEkraniKoprusu.kt`, `GorevGunlukSoruKopruleri.kt` Swift ekranları için çağrı sırasını tek yerde toplar.

### `profil` (57 test)

`ProfileFragmentLogic.kt`: `ProfileFragment.kt` ve ilgili ekranların saf hesapları (kaynak listesi dosya başında). Rozet kademe tablosu artık `kupa/rozet/BadgeProgressEngine.levelSpec`'e delege edilir (tek tablo).

## Android'den bilinçli farklar

- `Log` ve teşhis çağrıları kaldırıldı.
- `R.drawable.x`, `R.string.x`, `R.color.x` → kaynak adını taşıyan String (`"x"`). iOS tarafı bunları Asset Catalog adlarına eşler.
- Firestore, SharedPreferences, Handler, Cloud Functions çağrıları taşınmadı. Mantık saf fonksiyona ayrıldı; G/Ç arayüz olarak bırakıldı (`LessonProgressSink`, `MissionCloudGateway`, `EnergyPlatform`, `TimeTrackerPlatform`, `ServerRewards.ChestCaller`, `DailyQuestionChallengeCache`, `OyunKeyValueStore`, `AbacusPrefsStore`, `AbacusBeadHost`). Koleksiyon yolları, alan adları ve Cloud Function adları sabit olarak korundu.
- "Şimdi", saat dilimi, sezon numarası ve `globalPartId` gibi genel durumlar parametre oldu (varsayılanlar Android davranışını verir).
- `DocumentSnapshot` → `Map<String, Any?>`. Android'de tipi uyuşmayan alan istisna atar; ortak kodda varsayılana düşer.
- Rastgelelik enjekte edilebilir (`RastgeleKaynagi.rastgele`); varsayılanı tohumsuzdur. Bu nesne iş parçacığı güvenli değildir.
- `LessonItem.stableId` varsayılanı `""` (Android'de varsayılansız zorunlu alan). Katalogdaki her item dolu `stableId` taşır; boş değer yalnızca testlerde ve elle kurulan item'larda görülür. Boş `stableId`'li derste liderlik gönderimi atlanır (Android'deki `lessonKey.isBlank()` kapısı).
- Ders metinleri Android'de `strings_lessons.xml`'den yerelleştirilir; ortak katalog Türkçe metni sabit taşır (`GlobalLessonData.localizeLessonText` şimdilik metni olduğu gibi döndürür).

## Atlananlar ve nedenleri

| Ne | Neden |
|---|---|
| **Sobi sohbeti** | Sunucuya bağlı; kopyalanmadı ve Swift'te yazılmadı. Maskot ve kredi/mağaza ile ilgili kısımlar ortak koda alındı (`LessonEndMascotLogic`, `ShopFragmentLogic`, `BillingCatalog`). |
| Sobi hata analizi kural motoru, kural paneli/kitabı, ders içi rehber | Sonradan alındı (28.11.2026): analiz `sobianaliz/` paketinde; kural ve rehber mantığı Swift'te kopya (bkz. "Ortak koda taşınması gerekenler"). Yapay zekâya bağlı kısımlar hâlâ yok. |
| Günlük Sobi testi | Yazılıyor (`shared/.../sobitest` ve `iosApp/SobiTest/`); bu belgenin yazıldığı anda tamamlanmadı. |
| `TutorialFragment.kt` (17 bin satır) | Taşındı (27.11.2026): içerik (%77) betikle üretilen veri, geçişler saf kural (`ogretici/`); görünüm Swift'te (`iosApp/Ogretici/`, 12. derlemede derlendi). Atlananlar: Sobi düğmesi/sohbet, ölçüm, anketler, ipucu paneli. |
| `NotificationFragment.kt`, `QuestionChatFragment.kt`, `CreateQuestionFragment.kt` sorgu/sekme akışları | Yalnızca modeller ve yazım kuralları alındı; akış Firestore dinleyicilerine bağlı. |
| Yerel önbellek JSON ayrıştırması (ders önbelleği, günlük soru önbelleği) | JSON kütüphanesi eklenmedi. Biçim belgelendi; günlük soru önbelleği arayüze çevrildi. Aşama 2'de kotlinx-serialization eklenirse yazılır. |
| `LessonProgressDiag.kt` ve diğer `*Diagnostics` dosyaları | Tamamı log; hesap yok. |
| Çizim, animasyon, ses, bitmap önbelleği | Platform işi. Sabitler, geometri ve seçim kuralları taşındı. |
| AdMob, UMP, Play Billing, Play Review, FCM çağrıları | Platform SDK'ları. Kurallar ve kimlikler taşındı. |
| Tavşan boncuğu çizimi | `BeadType`'ta yok; yalnızca renk tercihleri taşındı. |

## Bilinen açık noktalar

1. **`stepCupIcon` alanı iki platform arasında uyumsuz.** Android bu alana Firestore'da `R.drawable` tam sayı kimliğini yazıyor; bu sayı derlemeden derlemeye değişebilir ve iOS'ta anlamsızdır. Ortak kod kaynak adını (String) kullanır: iOS Android'in yazdığı sayıyı çözemez, Android de iOS'un yazdığı String'i yok sayar (çökmez, rekordan yeniden türetir). Geçici çözüm olarak `GlobalLessonData.stepCupIconKayittanTuret` görseli rekordan türetir. Kalıcı çözüm Android'in de kaynak adı yazmasıdır; bu Android'e dokunmayı gerektirdiği için karar sizde.
2. **Renk dönüşümleri cihazla karşılaştırılmadı.** `Color.HSVToColor` / `colorToHSV` karşılıkları Skia formüllerinden hafızadan yazıldı; testler elle hesaplanan değerlerle tutarlı ama gerçek Android çıktısıyla doğrulanmadı. Özelleştirilmiş boncuk renkleri iOS'ta bir ton farklı çıkabilir.
3. **Altın veri yalnızca `matematik` ve `dersadim` için var.** Diğer paketlerde beklenen değerler Android kodu okunarak elle türetildi; Android'de çalıştırılıp karşılaştırılmadı.
4. **Saat dilimi testleri yalnızca UTC ve sabit +03:00 ile yapıldı.** Yaz saati uygulayan bölgeler denenmedi (Türkiye için sorun değil; İngilizce/Endonezce pazarlar için bakılmalı).
5. **Yinelenen kod:** `kupa/rozet/FishingStreakPeriod.kt`, `oyun/DailyQuestionPeriod.kt`'in altı fonksiyonunu ayrıca içerir; `ChestTypeProgressHelper` hem `ders` hem `oyun` paketinde var. Davranış aynı, birleştirilmeleri temizlik işidir.
6. **Ortak kod çok iş parçacıklı kullanım için tasarlanmadı** (`RastgeleKaynagi`, `SeasonClock` dinleyici listesi, eski `object` durumları). iOS'ta ana iş parçacığından kullanılmalı.

## Android'de göze çarpan davranışlar

Kopyalama sırasında fark edildi. **Hepsi ortak kodda aynen korundu** ve testlerle sabitlendi; Android'de düzeltilirse burada da düzeltilmeli. Yayını etkileyebilecek olanlar:

- **Günlük soru seçimi:** `dailyStableIndex` adına rağmen "TEST MODU" notuyla tamamen rastgele seçiyor.
- **Puanlama isteği:** eşik "en az 1 ders" ("for testing" notuyla).
- **Yaş onayı:** rıza yaşını geçen herkes (yetişkinler dahil) `TEEN` sayılıyor.
- **Part 1, "10'luk Toplama - 3 Basamaklı":** adımlar 28–30, ama tabloda 29 ve 30 dalı yok; üç adım da 28'in soru setine çözülüyor (aynı tür sorular üç kez).
- **Kupa aralıkları:** `CupRuleEngine.RANGES` içinde 2400..2499 iki kez tanımlı; sonraki aralıklar bir kaymış olabilir. `BLINDING_MULTIPLICATION_RANGES` ilk satırı `3000..999999`, altındakileri gölgeliyor.
- **Takip:** `FollowersFollowingFragment` geri takipte adları çapraz yazıyor (`AddFriendFragment`'in tersi).
- **Hesap silme:** `haveBead` / `haveFrame` alt koleksiyonlarını silmiyor.
- **Bölme:** `AbacusFragment.stepAnswerAlgorithm` "/" işlecinde beklenen cevabı her zaman 1 alıyor.
- **Uzun çıkarma dizileri:** dört üreteç ara toplam eksiye düşünce çöküyor; bugünkü çağrılarda (3–4 sayı) tetiklenmiyor.
- **Rozet metinleri:** VOLCANO metni "Kasırga rozetini" diyor; "Keçi"/"Keço", "Şaho"/"Karti" tutarsız.
- **Giriş hataları:** Google giriş hata mesajları kullanıcıya SHA-1 parmak izi ve Logcat yönlendirmesi gösteriyor.

Tam liste ilgili ortak dosyaların başlık yorumlarında ve testlerde.

Güncel Android'de (27.11.2026) düzeldiği için listeden çıkarılanlar: ders şablonunda kilitsiz dersler ve bitmiş 14 sandık (artık hiçbir sandık bitmiş gelmiyor), geçiş reklamında Google test kimliği (yayın sürümünde gerçek birim; test kimliği yalnızca hata ayıklama derlemesinde), roket rozetinin iki ayrı kademe tablosu, boncuk renk özelliği satın almasında kayıt başarısız olunca para iadesi yapılmaması. Ders geçme eşiği de değişti: abaküs dersi ve kör ders (part 4–5) için %10/%50 yerine %75.

## `tools/`

- `ders-katalog/uret.js`, `testuret.js`: Android `GlobalLessonData.kt` ve `values/strings_lessons.xml`'den `GlobalLessonDataKatalog.kt` ve test beklentilerini üretir. Android kataloğu değişince yeniden çalıştırılır.
- `ders-katalog/ozet.js`: katalog + dal listesinden `DersAdimAltinVeri.kt` içindeki katalog özetini (`ALTIN_KATALOG_OZET`) üretir.
- `ders-adim/uret.js`, `altin.js`: `MapFragment.kt`'den `LessonOperations.kt` ve altın veriyi üretir.

`matematik` altın verisini üreten düzenek özgün Android dosyasının bir kopyasını içerdiği için depoya konmadı; yeniden gerekirse `MathOperationGenerator.kt` JVM'de referans olarak derlenip tohumlu kaynakla karşılaştırılır (yöntem `AltinVeriTest.kt` başlığında).

## Aşama 2: kararlar ve kalan işler

Aşama 2'nin ekran işi yukarıdaki "Durum" bölümündedir. Aşağıda önce karar öncesi seçenekler (26.11.2026'da karara bağlandı, sonuçları "Verilen kararlar"da), sonra platform konuları var. Firebase, StoreKit ve Apple ile giriş henüz **uygulanmadı**; bu bölümler plandır.

### Karar öncesi seçenekler

1. **Firebase'e nasıl bağlanılacağı.** İki yol var:
   - *Swift tarafında Firebase iOS SDK* (öneri): ortak koddaki arayüzleri (`LessonProgressSink` vb.) Swift uygular. Resmî SDK, en az sürpriz; bedeli Swift'te Map ↔ Firestore yapıştırma kodu.
   - *Kotlin tarafında GitLive firebase-kotlin-sdk*: G/Ç de ortak kodda olur ama üçüncü taraf sarmalayıcıya bağlanılır ve CocoaPods/SPM kurulumu karmaşıklaşır.
2. **`stepCupIcon` uyumsuzluğu** (yukarıda 1. açık nokta): Android'de kaynak adı yazmaya geçilecek mi, yoksa iOS hep rekordan mı türetecek?
3. **Kids kategorisi mi, 4+ genel kategori mi.** En çok şeyi etkileyen karar; aşağıya bakın.
4. **App Store ürün kimlikleri.** `BillingCatalog` Play kimliklerini taşıyor (güncel Android'de yeniden adlandırıldı: `gold_small` …); aynı kimlikler App Store'da kullanılırsa sunucu kataloğu değişmez.
5. **Hesaplar platformlar arası ortak mı.** Ortaksa cüzdan, satın alınan boncuklar ve enerji iki mağazadan beslenir; sunucu doğrulaması iki makbuz türünü de tanımalı.

### Verilen kararlar (26.11.2026)

1. **Firebase:** Swift tarafında resmî Firebase iOS SDK. Ortak kod yalnızca kuralları hesaplar, okuma/yazmayı Swift yapar.
2. **`stepCupIcon`:** iOS hep rekordan türetir; Android'e dokunulmaz. Ortak hesap kararı (5) nedeniyle, iOS'un yazdığı kayıtların Android'de nasıl göründüğü ayrıca denenmeli.
3. **Kategori:** 4+ Eğitim; Kids kategorisi değil. Analitik, reklam ve sosyal özellikler Android'deki gibi kalır, yaş/rıza ekranı korunur.
4. **Ürün kimlikleri:** App Store'da Play ile aynı kimlikler kullanılır (`BillingCatalog`; liste "Kullanıcıdan beklenenler" bölümünde).
5. **Hesaplar:** ortak. iOS aynı Firebase projesine bağlanır; sunucudaki satın alma doğrulamasına App Store makbuzu eklenmesi gerekir (`functions/` değişikliği, ayrı iş).

### SwiftUI ekranları

- Yazılmış ekranların listesi ve kalanlar "Durum (28.11.2026)" bölümündedir; sıradaki işler günlük Sobi testi ve yarış panelinin bitirilmesi, akış denetimi bulgularının giderilmesi ve ses/animasyon/dokunma doğrulamasıdır (gerçek cihazda). Sobi sohbeti sunucu bağlanınca.
- Abaküs ekranı en riskli parça (yazıldı, dokunma hissi cihazda denenmedi): `AbacusBeadController` saf durum makinesi olarak hazır, `AbacusBeadHost` arayüzünü SwiftUI/UIKit tarafı uygular. Sürükleme hissi için UIKit jest tanıyıcıları SwiftUI jestlerinden daha öngörülebilir.
- `TutorialFragment` veriyle sürülen yapıya çevrildi (adım listesi + boncuk hareketleri, ortak Kotlin'de; bkz. `docs/OGRETICI_PLANI.md`). Böylece ileride Android da aynı veriyi kullanabilir.
- Görseller: ortak kod kaynak adlarını String olarak veriyor. Android `res/drawable` içindeki vektörler SVG/PDF'e çevrilip aynı adlarla Asset Catalog'a konursa eşleme tablosu gerekmez.
- Lottie animasyonları lottie-ios (`lottie-spm` paketi) ile bağlandı; `iosApp/Lottie/` altında 43 dosya var. Ders sonu rastgele Lottie'leri güncel Android'de kalktığı için silindi; kristal ekranı da silindi.
- Metinler: şu an Android `strings.xml`'den üretilen `tr.lproj` (3609 anahtar) kullanılıyor; `Localizable.xcstrings`'e geçilmedi. Dil sırası planı (İngilizce → Endonezce) burada da geçerli; ortak kodda Türkçe sabit metinler var (rozet açıklamaları, kupa motivasyon metinleri), çeviri öncesi bunların anahtarlara taşınması gerekir.
- Swift'ten Kotlin çağırmayı rahatlatmak için SKIE eklentisi düşünülebilir (sealed class, varsayılan parametre, Flow desteği). Zorunlu değil.

### Codemagic

**Durum (28.11.2026):** depo kökündeki `codemagic.yaml` içinde iki iş akışı var. İlki `ios-shared-test`: ilk çalıştırması (26.11.2026, commit `d90149c`, Mac mini M2, 10 dk 4 sn) yeşil bitti; ortak kod gerçek iOS hedefinde derlendi, simülatör testleri geçti, cihaz framework'ü bağlandı. Webhook 26.11.2026'da henüz eklenmemişti; sonrası doğrulanmadı, derlemeler elle başlatılıyordu. Yaptıkları: JVM testleri → `:shared:iosSimulatorArm64Test` → `:shared:linkDebugFrameworkIosArm64`. İmzalama ve Apple hesabı gerektirmez. `claude/sorobit-ios-*` dallarına push'ta, yalnızca `ios/` ya da `codemagic.yaml` değişmişse tetiklenir.

İkinci iş akışı `ios-app-build` (yalnızca elle başlatılır): `ios/iosApp/project.yml` dosyasından XcodeGen ile Xcode projesini üretir, uygulamayı imzasız olarak simülatör için derler, simülatörde açıp açılış ekranının görüntüsünü (`ekran.png`) ve `-ekran <ad>` ile açılan 83 galeri ekranının görüntülerini (`ekranlar/*.png`) derleme çıktısı olarak bırakır (ad sayısı `codemagic.yaml` içindeki `for AD in` döngüsünden sayıldı). Paket kimliği `com.sorobit.app` geçicidir. İlk çalıştırma (26.11.2026, commit `12d631e`, o zaman tek ekranlı iskelet) ilk denemede geçti; derleme 3 dk 16 sn sürdü. Son doğrulanmış çalıştırma 14. derleme (commit `f160be9`, 0 hata, 83 ekran görüntüsü). Simülatörün ilk açılışı ~3 dakika sürüyor, galeri adımı da ad başına 5 sn beklediği için uzun sürer. Önbellek iki iş akışından da çıkarıldı: kapanışta yüklenmesi 5 dakikadan uzun sürüyordu.

Kurulum (bir kez, hesap sahibi yapar):

1. codemagic.io'da GitHub ile oturum açılır, bu depo uygulama olarak eklenir.
2. Uygulama ayarlarında yapılandırma olarak `codemagic.yaml` seçilir.
3. Otomatik tetikleme için sağ kenardaki *Webhooks → Create webhook* tıklanır.
4. İlk çalıştırma elle: *Start new build* → dal `claude/sorobit-ios-phase-1-b59a74`, iş akışı *iOS ortak kod testleri*.

- Mac olmadığı için derleme, imzalama ve TestFlight yüklemesi Codemagic'te yapılır. Ücretsiz katman ayda 500 dakika macOS M2 verir; bir iOS derlemesi 10–20 dakika sürer.
- İş akışı: `ios/` içinde `./gradlew :shared:assembleSharedXCFramework` (ya da Xcode derleme aşamasında `embedAndSignAppleFrameworkForXcode`) → `xcodebuild` → TestFlight.
- `ios-shared-test` bu iş için kuruldu (Aşama 1 kodu iOS hedefinde derlendi, o günkü testler geçti). Güncel koddaki testlerin (JVM 1056) iOS hedefinde geçtiği henüz gösterilmedi.
- İmzalama için App Store Connect API anahtarı Codemagic'e girilir (otomatik imzalama); sertifikayı elle üretmek için Mac gerekmez.
- Tetikleyici yalnızca `ios/**` değişikliklerine bağlanmalı ki Android commit'leri dakika harcamasın.
- Mac olmadan Xcode proje dosyasını elle yazmak hataya açık; proje XcodeGen (`project.yml`) ile tanımlanıp Codemagic'te üretilirse Windows'tan yönetilebilir.
- Simülatör/cihaz olmadan arayüz hatası ayıklamak yavaş olur (şu an tek geri bildirim derleme çıktısı ve durağan görüntüler). Kiralık bulut Mac ya da ikinci el bir Mac mini seçeneği hâlâ değerlendirilmedi.

### Apple ile giriş

- App Store kuralı 4.8: üçüncü taraf girişi (Google) sunan uygulama, eşdeğer gizlilik sağlayan bir seçenek de sunmalı; pratikte bu Apple ile giriş demek. Yalnızca e-posta/parola sunulursa zorunlu değil.
- Firebase Auth Apple sağlayıcısını destekliyor. `AuthRules.kt`'teki kullanıcı dokümanı alanları ve Cloud Function gövdeleri hazır; Apple için yeni bir sağlayıcı değeri ve "e-postayı gizle" (relay adresi) durumu eklenmeli.
- Kullanıcı Apple'da e-postasını gizlerse öğretmen/veli iletişimi relay adresine gider; kurs/sınıf akışında e-postaya dayanan yerler gözden geçirilmeli.
- Hesap silme (kural 5.1.1(v)) uygulama içinden yapılabilmeli ve Apple token'ı iptal edilmeli. Android'deki hesap silme kuralları taşındı (`SessionDeviceRules.kt`); `haveBead`/`haveFrame` eksikliği iOS'ta da geçerli.
- Kids kategorisinde Apple ile giriş kullanılabilir; Google ile giriş üçüncü taraf SDK ve veri paylaşımı nedeniyle sorun çıkarabilir.

### StoreKit

- StoreKit 2 (async/await) kullanılmalı; iOS 15+ yeterli.
- Android'de ödül ve cüzdan sunucuda yönetiliyor (`ServerRewards`, `UserWalletFirestore`). Aynı desen korunmalı: satın alma sonrası imzalı işlem (JWS) bir Cloud Function'a gönderilir, cüzdan sunucuda güncellenir. Bu, `functions/` tarafında App Store doğrulaması için yeni bir fonksiyon gerektirir (Aşama 1'de `functions/`'a dokunulmadı).
- Ürünler App Store Connect'te ayrıca tanımlanır: tüketilebilir `gold_small/medium/large`, `keys_small/medium/large`, `sobi_credits_20/50/120`; abonelik `pro_monthly`. Uygulamadaki mağaza şimdilik sahte; StoreKit çağrısı yazılmadı. App Store Server Notifications v2 iade/iptal için bağlanmalı.
- Android'deki "Firestore yazımı başarısız olursa para iade edilmiyor" durumu iOS'ta gerçek para için tekrarlanmamalı: işlem sunucu onayından sonra `finish()` edilmeli.
- Ödüllü reklamla verilen ödüller StoreKit kapsamında değil ama Kids kategorisinde reklam kısıtlarına takılır.

### Kids kategorisi kısıtları

Kids kategorisi (kural 1.3 ve 5.1.4) seçilirse:

- **Üçüncü taraf analitik ve reklam yasak ya da çok sınırlı.** Kişisel veri, cihaz kimliği ya da konum üçüncü tarafa gönderilemez. Firebase Analytics/GA4, AdMob ve Crashlytics bu haliyle sorun çıkarır. Reklam ancak insan incelemesinden geçen, bağlamsal, çocuklara uygun ağlarla olabilir; IDFA kullanılamaz.
- **Ebeveyn kapısı zorunlu:** satın alma, dış bağlantı, destek/iletişim ve uygulamadan çıkaran her şeyin önünde.
- Kategori seçildikten sonra uygulama o kategoride kalmak zorunda; geri dönüş pratikte yeni uygulama demek.
- Sosyal özellikler (takip, arkadaş arama, öğretmene soru/sohbet, kullanıcı raporlama) Kids kategorisinde ağır incelemeye girer.
- ABD'de COPPA, AB'de GDPR-K ayrıca geçerli. `AgeConsentManager`'daki ülke bazlı rıza yaşı tablosu taşındı ve kullanılabilir.

**Değerlendirme:** bugünkü gelir ve ölçüm yapısı (AdMob, GA4, sosyal özellikler) Kids kategorisiyle çatışıyor. Kids kategorisine girmeden, Eğitim kategorisinde 4+ yaş derecesiyle yayınlamak en az değişiklik gerektiren yol; bedeli App Store'un "Çocuklar" sekmesinde görünmemek. Hedef kitle çocuk olduğu için Apple yine de çocuklara yönelik uygulama kurallarını (reklam içeriği, ebeveyn kapısı) uygulayabilir; ebeveyn kapısı her iki durumda da eklenmeli. Kids seçilirse reklamsız/analitiksiz ayrı bir yapılandırma gerekir ve reklam geliri iOS'ta büyük ölçüde düşer.

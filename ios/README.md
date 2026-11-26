# Sorobit iOS

Bu klasör, Android uygulamasından **tamamen bağımsız** bir Gradle projesidir. Kök `settings.gradle.kts` bu klasörü içermez; Android derlemesi buradan etkilenmez ve Android `ios/shared` modülüne bağlanmaz. Buradaki Kotlin kodu, `app/` içindeki mantığın **kopyasıdır**: Android'de bir kural değişirse aynı değişiklik burada elle yapılmalıdır.

```
ios/
  shared/   Kotlin Multiplatform; Android mantığının kopyası (Aşama 1 — bitti)
  iosApp/   SwiftUI ekranları (Aşama 2 — henüz yok)
  tools/    Android kaynağından katalog/altın veri üreten betikler
```

## Durum (Aşama 1)

- 79 ortak kaynak dosyası (yaklaşık 23 bin satır), yaklaşık 16 bin satır test.
- Son birleşik çalıştırma: **JVM 671 test, Kotlin/Native (mingwX64) 668 test, 0 hata.** (Fark olan 3 test yalnızca JVM'de çalışan `java.util.Random` karşılaştırmasıdır.)
- `commonMain` içinde `java.*`, `android.*`, Firebase, Gson yok. Bağımlılıklar: Kotlin stdlib, kotlinx-datetime 0.6.1, kotlin-test.
- **iOS hedefleri (iosArm64, iosSimulatorArm64, iosX64) bu bilgisayarda hiç derlenmedi**; Windows'ta derlenemezler. Yerine aynı Kotlin/Native derleyici arka ucunu kullanan `mingwX64` hedefi derlenip testlendi. İlk gerçek iOS derlemesi Mac'te (Codemagic) yapılacak; Apple'a özgü bir sorun çıkma olasılığı düşük ama sıfır değil.

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

### `ders` — ders kataloğu ve ilerleme (153 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `LessonItem.kt` | `model/LessonItem.kt` — tamamı |
| `GlobalLessonDataKatalog.kt` | `GlobalLessonData.createLessonItems` — 9 part, 169 item; betikle üretildi |
| `GlobalLessonData.kt` | `GlobalLessonData.kt`'nin geri kalanı (varsayılanlar, Firestore alanları ↔ model, `updateLessonItem`, backfill, tamamlanma yüzdesi) + `LessonDataManager.kt` anahtarları |
| `LessonProgressMerge.kt`, `ChestTypeProgressHelper.kt`, `LessonManager.kt` | aynı adlı dosyalar — tamamı |
| `GuidePanelData.kt` | `model/GuidePanelData.kt` + `MapFragment.showGuidePanel` verisi |
| `LessonSuccessRateRepository.kt` | dört transaction gövdesinin saf hesabı, alan adları, yollar |
| `ChestFragmentLogic.kt` | `ChestFragment.kt` — `updateMapProgress`, kilit açma, `changeCupIcon` |
| `LessonResultLogic.kt` | `LessonResult.kt`, `LessonResultFalse.kt`, `ChestResult.kt` hesapları |
| `LessonAdapterLogic.kt` | `LessonAdapter.kt`, `RaceAdapter.kt` — alt panel durumu, enerji kararı, açılacak ekran |
| `MapFragmentLogic.kt` | `MapFragment.kt` (sabit başlık, `resolveLessonOperationsId`), `MarathonGuideStore.kt` uygunluk kuralı |

Katalog, Android dosyasını bağımsız bir ayrıştırıcıyla okuyan betikten üretilmiş sabit beklentilerle doğrulanıyor (part başına adet, tür dağılımı, 23 alanın parmak izi).

### `dersadim` — ders adımı → soru listesi (21 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `LessonOperations.kt` | `MapFragment.kt` companion: `getLessonOperations`, `getLessonOperationsBlinding`, iki `resolve*` — 70 + 55 dalın tamamı, mekanik dönüşümle |
| `UploadLessonDataLogic.kt` | `AbacusFragment.uploadLessonData`, `BlindingLessonFragment.uploadLessonData` |

### `kupa` — kupa, rozet, sezon, lider tablosu (110 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `CupRuleEngine.kt` | `CupRuleEngine.kt` — tamamı |
| `CupRepositories.kt` | altı `*CupRepository.kt` — alanlar, yollar, skor güncelleme hesabı |
| `CupHistoryRepository.kt`, `CupHistoryLogic.kt` | `CupHistoryRepository.kt`, `CupHistoryFragment.kt` |
| `BadgePrecalcHelper.kt`, `CupFragmentLogic.kt`, `KupaZaman.kt` | `BadgePrecalcHelper.kt`, `CupFragment.kt`; gün kimliği yardımcıları |
| `rozet/BadgeProgressEngine.kt`, `rozet/BadgePieceLeaderboardSync.kt` | aynı adlı dosyalar — tamamı |
| `rozet/BadgeProgressFirestore.kt` | `BadgeProgressFirestore.kt` — alanlar, doküman ↔ model, transaction gövdeleri, kademe atlama zincirleri |
| `rozet/BadgeFragmentLogic.kt`, `rozet/BadgeDetailLogic.kt`, `rozet/BadgeAnimMode.kt` | `BadgeFragment.kt`, `BadgeDetailFragment.kt` içindeki hesap ve metin tabloları |
| `rozet/FishingStreakPeriod.kt` | `DailyQuestionPeriod.kt` içinden rozetin kullandığı altı fonksiyon (aşağıdaki "yinelenen kod" notuna bakın) |
| `sezon/SeasonClock.kt`, `sezon/SeasonLeaderboardRewardPayloads.kt` | aynı adlı dosyalar — tamamı |
| `sezon/LessonLeaderboardRepository.kt`, `sezon/RecordLogic.kt` | `LessonLeaderboardRepository.kt`, `RecordFragment.kt`, `RecordLeaderboardAdapter.kt` |

### `oyun` — günlük soru, görevler, sandık, enerji, cüzdan (168 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `DailyQuestionChallenge.kt`, `DailyQuestionPeriod.kt`, `DailyQuestionPoolBuilder.kt` | aynı adlı dosyalar — tamamı |
| `DailyQuestionRepository.kt`, `DailyQuestionStores.kt` | `DailyQuestionRepository.kt`, iki `DailyQuestion*Store.kt`, `TasksFragment.kt` kart kuralları |
| `MissionQuestCatalog.kt`, `MissionsProgressStore.kt` | aynı adlı dosyalar — tamamı |
| `MissionUiLogic.kt` | `MissionsFragment.kt`, `MissionChestRewardFragment.kt`, `MissionProgressLayout.kt` hesapları |
| `ChestCrystalPolicy.kt`, `NewChestLogic.kt`, `ChestResultLogic.kt` | `ChestCrystalPolicy.kt`, `NewChestFragment.kt`, `ChestResult.kt` |
| `EnergyManager.kt`, `ServerEnergy.kt`, `ServerRewards.kt`, `UserWalletFirestore.kt`, `TimeTracker.kt`, `BillingCatalog.kt` | aynı adlı dosyalar |
| `AgeConsentManager.kt`, `AppRatingManager.kt`, `AdRules.kt`, `GuideStores.kt` | `AgeConsentManager.kt`, `AppRatingManager.kt`, `AdManager.kt`, `MarathonGuideStore.kt`, `FirstTutorialShownStore.kt` — kural kısmı |
| `OyunKeyValueStore.kt`, `OyunSaat.kt`, `OyunBagimliliklari.kt` | Android karşılığı yok: SharedPreferences karşılığı arayüz, saat, geçici bağımlılık türleri |

### `abakus` ve `model` (147 test)

| Ortak dosya | Android kaynağı |
|---|---|
| `abakus/AbacusBeadController.kt` | `abacus/AbacusBeadController.kt` — tamamı; görünüm yerine `AbacusBeadHost` arayüzü |
| `abakus/AbacusBeadMetrics.kt`, `abakus/AbacusPreferences.kt` | aynı adlı dosyalar (katalog, varsayılan renkler, anahtar adları) |
| `abakus/AbacusBeadRenderer.kt`, `abakus/AbacusFrameRenderer.kt` | aynı adlı dosyalar — renk eşlemeleri ve piksel değiştirme algoritması (çizim değil) |
| `abakus/AbacusCustomizationFirestore.kt`, `abakus/AbacusPurchases.kt` | `AbacusCustomizationFirestore.kt`, `BeadPurchaseFirestore.kt`, `FramePurchaseFirestore.kt`, `AbacusCustomizationFragment.kt` fiyat/stok kuralları, `AbacusSoundPlayer.kt` ses seçimi |
| `abakus/AbacusLessonLogic.kt` | `AbacusFragment.kt`, `AbacusPracticeFragment.kt` — cevap doğrulama, sayaç, puan, ders sonu yönlendirmesi |
| `abakus/BlindingLessonLogic.kt` | `BlindingLessonFragment.kt` — cevap doğrulama, kupa deltası, sayı dizisi durum makinesi |
| `model/StudentQuestion.kt`, `model/MessageReport.kt` | `model/StudentQuestion.kt`, `model/MessageReport.kt`, `model/RestrictedUser.kt`, `UserAskQuestionRestriction.kt`, `NotificationFragment.kt` yazımları |
| `model/AuthRules.kt` | `auth/AuthManager.kt`, `RegisterActivity.kt`, `LoginActivity.kt`, `EditProfileFragment.kt`, `OtpVerificationFragment.kt` — doğrulama kuralları, kullanıcı dokümanı alanları, Cloud Function adları ve gövdeleri |
| `model/SessionDeviceRules.kt` | `SessionDeviceManager.kt`, `AccountDeletionHelper.kt`, `MyFirebaseMessagingService.kt`, `AddFriendFragment.kt`, `FollowersFollowingFragment.kt` |

### `baglanti` — paketler arası köprüler (5 test)

Paketler birbirinden bağımsız kopyalandığı için bazıları komşu türlerin küçük yerel kopyalarını tanımladı. `PaketKopruleri.kt` bunları asıl türlere bağlar: günlük soru havuzu → `MathOperationGenerator`, `LessonItem` → `OyunLessonItem`, kupa dersi → part 9 yer tutucusu.

## Android'den bilinçli farklar

- `Log` ve teşhis çağrıları kaldırıldı.
- `R.drawable.x`, `R.string.x`, `R.color.x` → kaynak adını taşıyan String (`"x"`). iOS tarafı bunları Asset Catalog adlarına eşler.
- Firestore, SharedPreferences, Handler, Cloud Functions çağrıları taşınmadı. Mantık saf fonksiyona ayrıldı; G/Ç arayüz olarak bırakıldı (`LessonProgressSink`, `MissionCloudGateway`, `EnergyPlatform`, `TimeTrackerPlatform`, `ServerRewards.ChestCaller`, `DailyQuestionChallengeCache`, `OyunKeyValueStore`, `AbacusPrefsStore`, `AbacusBeadHost`). Koleksiyon yolları, alan adları ve Cloud Function adları sabit olarak korundu.
- "Şimdi", saat dilimi, sezon numarası ve `globalPartId` gibi genel durumlar parametre oldu (varsayılanlar Android davranışını verir).
- `DocumentSnapshot` → `Map<String, Any?>`. Android'de tipi uyuşmayan alan istisna atar; ortak kodda varsayılana düşer.
- Rastgelelik enjekte edilebilir (`RastgeleKaynagi.rastgele`); varsayılanı tohumsuzdur. Bu nesne iş parçacığı güvenli değildir.

## Atlananlar ve nedenleri

| Ne | Neden |
|---|---|
| **Sobi kuralları** | Bu dalda Sobi kodu yok: ne `app/` ne `functions/` içinde "sobi" geçiyor. Sobi'nin bulunduğu dal birleştiğinde ayrıca kopyalanmalı. |
| `TutorialFragment.kt` (17 bin satır) | Mantık görünüm ve animasyonla iç içe; kendi içinde kapalı saf kural bulunamadı. Aşama 2'de ekranla birlikte ele alınmalı — en büyük tek iş kalemi. |
| `ProfileFragment.kt` | Bakılmadı. |
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

- **Ders şablonu:** part 1–6'da bütün dersler kilitsiz ve 14 sandık bitmiş olarak geliyor; boş hesapta tamamlanma %12 çıkıyor. Test kalıntısı gibi duruyor.
- **Günlük soru seçimi:** `dailyStableIndex` adına rağmen "TEST MODU" notuyla tamamen rastgele seçiyor.
- **Reklam:** geçiş reklamı birimi hâlâ Google'ın paylaşımlı test kimliği.
- **Puanlama isteği:** eşik "en az 1 ders" ("for testing" notuyla).
- **Yaş onayı:** rıza yaşını geçen herkes (yetişkinler dahil) `TEEN` sayılıyor.
- **Part 1, "10'luk Toplama - 3 Basamaklı":** 2. adımdan itibaren boş soru listesi alıyor (adım numaraları tutarsız: start 28, finish 31, stepCount 3).
- **Kupa aralıkları:** `CupRuleEngine.RANGES` içinde 2400..2499 iki kez tanımlı; sonraki aralıklar bir kaymış olabilir. `BLINDING_MULTIPLICATION_RANGES` ilk satırı `3000..999999`, altındakileri gölgeliyor.
- **Roket rozeti:** iki farklı kademe tablosu var (`[3,10,15,25,30]` ve `[3,5,10,15,25]`); aynı ilerlemede ton ve oran farklı çıkıyor.
- **Takip:** `FollowersFollowingFragment` geri takipte adları çapraz yazıyor (`AddFriendFragment`'in tersi).
- **Hesap silme:** `haveBead` / `haveFrame` alt koleksiyonlarını silmiyor.
- **Bölme:** `AbacusFragment.stepAnswerAlgorithm` "/" işlecinde beklenen cevabı her zaman 1 alıyor.
- **Satın alma:** boncuk renk özelliğinde Firestore yazımı başarısız olursa para iade edilmiyor.
- **Uzun çıkarma dizileri:** dört üreteç ara toplam eksiye düşünce çöküyor; bugünkü çağrılarda (3–4 sayı) tetiklenmiyor.
- **Rozet metinleri:** VOLCANO metni "Kasırga rozetini" diyor; "Keçi"/"Keço", "Şaho"/"Karti" tutarsız.
- **Giriş hataları:** Google giriş hata mesajları kullanıcıya SHA-1 parmak izi ve Logcat yönlendirmesi gösteriyor.

Tam liste ilgili ortak dosyaların başlık yorumlarında ve testlerde.

## `tools/`

- `ders-katalog/uret.js`, `testuret.js`: Android `GlobalLessonData.kt`'den `GlobalLessonDataKatalog.kt` ve test beklentilerini üretir. Android kataloğu değişince yeniden çalıştırılır.
- `ders-adim/uret.js`, `altin.js`: `MapFragment.kt`'den `LessonOperations.kt` ve altın veriyi üretir.

`matematik` altın verisini üreten düzenek özgün Android dosyasının bir kopyasını içerdiği için depoya konmadı; yeniden gerekirse `MathOperationGenerator.kt` JVM'de referans olarak derlenip tohumlu kaynakla karşılaştırılır (yöntem `AltinVeriTest.kt` başlığında).

## Aşama 2 için öneriler

Aşağıdakiler öneridir; hiçbirine başlanmadı.

### Önce verilmesi gereken kararlar

1. **Firebase'e nasıl bağlanılacağı.** İki yol var:
   - *Swift tarafında Firebase iOS SDK* (öneri): ortak koddaki arayüzleri (`LessonProgressSink` vb.) Swift uygular. Resmî SDK, en az sürpriz; bedeli Swift'te Map ↔ Firestore yapıştırma kodu.
   - *Kotlin tarafında GitLive firebase-kotlin-sdk*: G/Ç de ortak kodda olur ama üçüncü taraf sarmalayıcıya bağlanılır ve CocoaPods/SPM kurulumu karmaşıklaşır.
2. **`stepCupIcon` uyumsuzluğu** (yukarıda 1. açık nokta): Android'de kaynak adı yazmaya geçilecek mi, yoksa iOS hep rekordan mı türetecek?
3. **Kids kategorisi mi, 4+ genel kategori mi.** En çok şeyi etkileyen karar; aşağıya bakın.
4. **App Store ürün kimlikleri.** `BillingCatalog` Play kimliklerini taşıyor; aynı kimlikler App Store'da kullanılırsa sunucu kataloğu değişmez.
5. **Hesaplar platformlar arası ortak mı.** Ortaksa cüzdan, satın alınan boncuklar ve enerji iki mağazadan beslenir; sunucu doğrulaması iki makbuz türünü de tanımalı.

### Verilen kararlar (26.11.2026)

1. **Firebase:** Swift tarafında resmî Firebase iOS SDK. Ortak kod yalnızca kuralları hesaplar, okuma/yazmayı Swift yapar.
2. **`stepCupIcon`:** iOS hep rekordan türetir; Android'e dokunulmaz. Ortak hesap kararı (5) nedeniyle, iOS'un yazdığı kayıtların Android'de nasıl göründüğü ayrıca denenmeli.
3. **Kategori:** 4+ Eğitim; Kids kategorisi değil. Analitik, reklam ve sosyal özellikler Android'deki gibi kalır, yaş/rıza ekranı korunur.
4. **Ürün kimlikleri:** App Store'da Play ile aynı kimlikler kullanılır.
5. **Hesaplar:** ortak. iOS aynı Firebase projesine bağlanır; sunucudaki satın alma doğrulamasına App Store makbuzu eklenmesi gerekir (`functions/` değişikliği, ayrı iş).

### SwiftUI ekranları

- Önerilen sıra: giriş/kayıt → harita (ders listesi) → abaküs dersi → ders sonucu → sandık → kör (zihinden) ders → günlük soru ve görevler → kupa/rozet → mağaza ve özelleştirme → profil/sosyal. İlk üçü bitince uçtan uca bir ders oynanabilir.
- Abaküs ekranı en riskli parça: `AbacusBeadController` saf durum makinesi olarak hazır, `AbacusBeadHost` arayüzünü SwiftUI/UIKit tarafı uygular. Sürükleme hissi için UIKit jest tanıyıcıları SwiftUI jestlerinden daha öngörülebilir.
- `TutorialFragment` (17 bin satır) taşınmadı. Öğreticiler iOS'ta veriyle sürülen bir yapıya (adım listesi + hedef boncuk) çevrilirse hem iOS işi küçülür hem ileride Android sadeleşebilir. Aynen çevirmek haftalar alır.
- Görseller: ortak kod kaynak adlarını String olarak veriyor. Android `res/drawable` içindeki vektörler SVG/PDF'e çevrilip aynı adlarla Asset Catalog'a konursa eşleme tablosu gerekmez.
- Lottie animasyonları (`assets/*.json`) lottie-ios ile aynen çalışır.
- Metinler: Android `strings.xml` → `Localizable.xcstrings`. Dil sırası planı (İngilizce → Endonezce) burada da geçerli; ortak kodda Türkçe sabit metinler var (rozet açıklamaları, kupa motivasyon metinleri), çeviri öncesi bunların anahtarlara taşınması gerekir.
- Swift'ten Kotlin çağırmayı rahatlatmak için SKIE eklentisi düşünülebilir (sealed class, varsayılan parametre, Flow desteği). Zorunlu değil.

### Codemagic

**Durum (26.11.2026):** depo kökündeki `codemagic.yaml` içinde `ios-shared-test` iş akışı hazır. İlk çalıştırma (commit `d90149c`, Mac mini M2, 10 dk 4 sn) yeşil bitti: ortak kod gerçek iOS hedefinde derlendi, simülatör testleri geçti, cihaz framework'ü bağlandı. Webhook henüz eklenmedi; derlemeler elle başlatılıyor. Yaptıkları: JVM testleri → `:shared:iosSimulatorArm64Test` → `:shared:linkDebugFrameworkIosArm64`. İmzalama ve Apple hesabı gerektirmez. `claude/sorobit-ios-*` dallarına push'ta, yalnızca `ios/` ya da `codemagic.yaml` değişmişse tetiklenir.

İkinci iş akışı `ios-app-build` (yalnızca elle başlatılır): `ios/iosApp/project.yml` dosyasından XcodeGen ile Xcode projesini üretir, uygulamayı imzasız olarak simülatör için derler, simülatörde açıp ekran görüntüsünü (`ekran.png`) derleme çıktısı olarak bırakır. `ios/iosApp/` şimdilik tek ekranlı bir iskelettir: ortak koddan 1. bölümün öğe sayısını okuyup gösterir. Paket kimliği `com.sorobit.app` geçicidir. **Bu iş akışı henüz çalıştırılmadı.**

Kurulum (bir kez, hesap sahibi yapar):

1. codemagic.io'da GitHub ile oturum açılır, bu depo uygulama olarak eklenir.
2. Uygulama ayarlarında yapılandırma olarak `codemagic.yaml` seçilir.
3. Otomatik tetikleme için sağ kenardaki *Webhooks → Create webhook* tıklanır.
4. İlk çalıştırma elle: *Start new build* → dal `claude/sorobit-ios-phase-1-b59a74`, iş akışı *iOS ortak kod testleri*.

- Mac olmadığı için derleme, imzalama ve TestFlight yüklemesi Codemagic'te yapılır. Ücretsiz katman ayda 500 dakika macOS M2 verir; bir iOS derlemesi 10–20 dakika sürer.
- İş akışı: `ios/` içinde `./gradlew :shared:assembleSharedXCFramework` (ya da Xcode derleme aşamasında `embedAndSignAppleFrameworkForXcode`) → `xcodebuild` → TestFlight.
- **Önce yalnızca `:shared:iosSimulatorArm64Test` çalıştıran küçük bir iş akışı kurulmalı.** Bu, Aşama 1 kodunun gerçek iOS hedefinde derlendiğini ve 668 testin geçtiğini kanıtlar; ekran yazmadan önce yapılacak en ucuz doğrulama.
- İmzalama için App Store Connect API anahtarı Codemagic'e girilir (otomatik imzalama); sertifikayı elle üretmek için Mac gerekmez.
- Tetikleyici yalnızca `ios/**` değişikliklerine bağlanmalı ki Android commit'leri dakika harcamasın.
- Mac olmadan Xcode proje dosyasını elle yazmak hataya açık; proje XcodeGen (`project.yml`) ile tanımlanıp Codemagic'te üretilirse Windows'tan yönetilebilir.
- Simülatör/cihaz olmadan arayüz hatası ayıklamak yavaş olur. Ekran işi başlamadan önce kiralık bulut Mac ya da ikinci el bir Mac mini seçeneği değerlendirilmeli.

### Apple ile giriş

- App Store kuralı 4.8: üçüncü taraf girişi (Google) sunan uygulama, eşdeğer gizlilik sağlayan bir seçenek de sunmalı; pratikte bu Apple ile giriş demek. Yalnızca e-posta/parola sunulursa zorunlu değil.
- Firebase Auth Apple sağlayıcısını destekliyor. `AuthRules.kt`'teki kullanıcı dokümanı alanları ve Cloud Function gövdeleri hazır; Apple için yeni bir sağlayıcı değeri ve "e-postayı gizle" (relay adresi) durumu eklenmeli.
- Kullanıcı Apple'da e-postasını gizlerse öğretmen/veli iletişimi relay adresine gider; kurs/sınıf akışında e-postaya dayanan yerler gözden geçirilmeli.
- Hesap silme (kural 5.1.1(v)) uygulama içinden yapılabilmeli ve Apple token'ı iptal edilmeli. Android'deki hesap silme kuralları taşındı (`SessionDeviceRules.kt`); `haveBead`/`haveFrame` eksikliği iOS'ta da geçerli.
- Kids kategorisinde Apple ile giriş kullanılabilir; Google ile giriş üçüncü taraf SDK ve veri paylaşımı nedeniyle sorun çıkarabilir.

### StoreKit

- StoreKit 2 (async/await) kullanılmalı; iOS 15+ yeterli.
- Android'de ödül ve cüzdan sunucuda yönetiliyor (`ServerRewards`, `UserWalletFirestore`). Aynı desen korunmalı: satın alma sonrası imzalı işlem (JWS) bir Cloud Function'a gönderilir, cüzdan sunucuda güncellenir. Bu, `functions/` tarafında App Store doğrulaması için yeni bir fonksiyon gerektirir (Aşama 1'de `functions/`'a dokunulmadı).
- Tüketilebilir ürünler (kristal, enerji) ve varsa abonelik App Store Connect'te ayrıca tanımlanır. App Store Server Notifications v2 iade/iptal için bağlanmalı.
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

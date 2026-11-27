# iosApp geliştirme kuralları

Bu belge `ios/iosApp/` altında SwiftUI ekranı yazan herkes (insan ya da alt oturum) içindir. Genel durum ve ortak kodun içeriği için `ios/README.md`'ye bakın.

## Ortam

- Bu makine Windows; Mac yok. **Swift burada derlenemez.** Derleme Codemagic'te yapılır (`codemagic.yaml` → `ios-app-build`), bir tur ~10 dakika sürer ve aylık kotadan düşer. Kodun ilk seferde derlenmesi bu yüzden önemlidir.
- Xcode projesi depoda yoktur; `ios/iosApp/project.yml` dosyasından XcodeGen ile üretilir. `ios/iosApp/iosApp/` altındaki her dosya (Swift, asset, font, `.lproj`) kendiliğinden projeye girer. `project.yml`'e dokunmayın.
- Hedef: iOS 16, Swift 5.9, yalnızca dikey iPhone, hep koyu tema.
- Ortak Kotlin kodu `ios/shared/` içindedir; Swift'te `import Shared`. Kurallar ve hesaplar oradadır: **mantığı Swift'te yeniden yazmayın, ortak koddan çağırın.**

## Dokunulmayacaklar

- `app/` (Android) **salt okunurdur**: kaynak olarak okunur, hiçbir dosyası değiştirilmez.
- Kök `settings.gradle.kts`, `build.gradle.kts`, `functions/`, Firestore kuralları, `firebase.json`.
- Başkasına ait klasörler. Her ekranın kendi klasörü vardır (`Harita/`, `Kabuk/`, `Abakus/` …); yalnızca size verilen klasöre yazın.

## Yazım

- Kod yorumları Türkçe.
- Swift tanımlayıcılarında ve dosya adlarında Türkçe'ye özgü harf yok (ç, ğ, ı, ö, ş, ü kullanılmaz): `GorevlerEkrani`, `yaziTipi`.
- Üst düzey tür adları tüm uygulamada tekildir (tek modül). Çakışmayı önlemek için her türün adını ekranınızın önekiyle başlatın (`Harita…`, `Abakus…`, `Kupa…`).
- Dosyalar UTF-8, LF satır sonu.
- Yalnızca iOS 16'da var olan API'ler: iki parametreli `onChange`, `@Observable`, `ScrollView` konum API'leri, `containerRelativeFrame`, `sensoryFeedback` gibi iOS 17+ özellikleri **yok**.
- Büyük `body`'ler derleyicinin tip çıkarımını zorlar ("unable to type-check this expression in reasonable time"). Görünümleri küçük alt görünümlere bölün; uzun aritmetik ifadeleri ara değişkenlere ayırın ve tiplerini yazın.
- Android'e sadakat: layout XML'indeki ölçüler dp → pt birebir alınır; renk, boşluk, köşe yarıçapı, hiza korunur.

## Ortak kaynaklar

| Ne | Nasıl |
|---|---|
| Görsel | `Image("android_drawable_adi")` — Android `R.drawable` adıyla aynı. Ortak kodun String olarak verdiği kaynak adları doğrudan verilir. Kodla renklendirilen simgelerde `.renderingMode(.template)` + `.foregroundColor`. |
| Renk | `Color("android_renk_adi")` — `colors.xml` adıyla aynı. |
| Yazı tipi | `Tema.yazi(_ boyut: CGFloat, kalin: Bool = true) -> Font` (Baloo 2). Android teması bütün metinleri kalın çizer. |
| Metin | `Metin.al("android_string_adi")` ve biçimli olanlar için `Metin.al("ad", arg1, arg2)` — `strings.xml` adıyla aynı. Android'de karşılığı olmayan metin gerekiyorsa Swift'te Türkçe sabit yazın ve raporda listeleyin. |
| Şekil arka planları | Android `shape`/`selector`/`ripple` drawable'ları asset olarak **yoktur**; SwiftUI'da çizilir. Çözülmüş renk ve köşe değerleri `ios/tools/gorsel-donustur/rapor.json` içindedir. |

### Lottie animasyonları

Android'de `LottieAnimationView` olan her yerde ortak sarmalayıcı kullanılır (`iosApp/Ortak/LottieGorunumu.swift`):

```swift
LottieGorunumu("confetti_anim")                          // bir kez oynar
LottieGorunumu("fire_anim", dongu: true)                 // sürekli döner
LottieGorunumu("gold_medal_anim", dongu: false, hiz: 1.0, oynat: true, bitti: { ... })
```

- İmza: `init(_ ad: String, dongu: Bool = false, hiz: Double = 1.0, oynat: Bool = true, bitti: (() -> Void)? = nil)`.
- `ad`: Android'deki dosya adının uzantısız hali (`app/src/main/assets/*.json` ya da `res/raw/*.json`); dosyalar `iosApp/Lottie/` altına kopyalanmıştır. `oynat: false` ilk karede durur.
- Boyut görünümün kendisine `.frame(...)` ile verilir (içerik sığdırılır, Android `fitCenter` gibi). **İki yön de verilmelidir** (`width` ve `height`); verilmeyen yönde görünüm önerilen alanın tamamını kaplar.
- Kare aralığı (isteğe bağlı, imzanın sonunda): `baslangicKare: Double? = nil, bitisKare: Double? = nil` — Android `setMinAndMaxFrame` / `setFrame` / `progress` karşılığı, dosyanın kendi kare numaralarıyla. `bitisKare` verilirse `baslangicKare ?? ilk kare`'den o kareye oynar ve orada kalır (`dongu: true` ise bu aralıkta döner); `bitti` aralığın sonunda çağrılır; aralık değişince yeni aralık baştan oynar. Sabit kare: `LottieGorunumu("x", oynat: false, baslangicKare: 116)`. Animasyonun sınırları dışındaki kare en yakın sınıra kırpılır; "son kare" için `baslangicKare: 9999` verilebilir.
- Sarmalayıcı **dokunma almaz** (alttaki görünüme geçirir; tam ekran konfeti düğmeleri kilitlemez). `Button` etiketi ya da `onTapGesture` içinde kullanılıyorsa kapsayan görünüme `.contentShape(Rectangle())` ekleyin.
- `lottie-ios` kütüphanesini ekran kodundan doğrudan `import` etmeyin; yalnızca sarmalayıcıyı kullanın.

### Ortak SwiftUI bileşenleri

| Bileşen | Kullanım |
|---|---|
| `SobiMaskotGorunumu` (`Maskot/`) | `SobiMaskotGorunumu(emote: String, animasyonlu: Bool = true)` — Android `BunnyMascotView`'ın SwiftUI `Canvas` karşılığı, 48 hâl. `emote` Android'deki hâl adıdır. Boyutu `.frame(...)` ile verin. |
| `GocenDugmeStili` (`Ortak/`) | `ButtonStyle`: `.buttonStyle(GocenDugmeStili(zemin: Color, golge: Color, koseYaricapi: CGFloat = 16, derinlik: CGFloat = 4))` — Android'in iki katmanlı "göçen" düğmesi (yüz + dudak; basınca `derinlik` kadar iner). Yüksekliği düğmeye `.frame(height:)` ile verin. |
| `AvatarGorunumu` (`Profil/Avatar/`) | `AvatarGorunumu(config: String)` (arka planlı daire, tam avatar) ya da `AvatarGorunumu(config: String, arkaPlan: Bool, yakinPlan: Bool)` — Personas avatarı; `config` avatar yapılandırma dizesi (Android `AvatarConfigLogic` biçimi). |
| `LottieGorunumu` (`Ortak/`) | Yukarıda. |

Görsellerin listesi: `ios/iosApp/iosApp/Assets.xcassets/Gorseller/`. Olmayan bir ada başvurmak çökertmez (boş çizilir) ama raporda belirtin.

## Ekran sözleşmesi

- Her ekran kendi klasöründe, tek bir giriş görünümüyle dışa açılır (`struct XEkrani: View`). Girdiler yalın değerlerdir; çıkışlar kapanışlardır (`kapat: () -> Void`, `bitti: (Sonuc) -> Void`).
- Durum bir `ObservableObject` görünüm modelinde tutulur.
- **Firebase henüz yok.** Ağ/Firestore/giriş gerektiren her şey bir Swift `protocol`'ünün arkasına konur ve örnek veri döndüren sahte bir uygulamasıyla (`Sahte…`) birlikte yazılır. Android'de verinin hangi koleksiyon/alandan geldiği yorumla belirtilir.
- Ses, titreşim, reklam, ödeme SDK çağrıları da şimdilik protokol arkasında boş bırakılır.
- Başka ekranı doğrudan çağırmayın; geçişi kapanışla dışarı verin. Ekranları birbirine yönetici oturum bağlar.

## Kotlin → Swift adlandırma

Ortak kodun Swift'e nasıl göründüğünün kesin kaynağı üretilen başlık dosyasıdır: **`ios/docs/Shared.h`** (Codemagic çıktısından alınır; son güncelleme 13. derlemeden; sonradan eklenen ortak kod adları orada olmayabilir, işi bitirmeden önce kullandığınız her adı orada `grep` ile doğrulayın). Genel kurallar:

- `object X` → `X.shared`; `companion object` üyeleri → `X.companion.uye`.
- `Int` → `Int32`, `Long` → `Int64`, `Float` → `Float`, `Double` → `Double`, `Boolean` → `Bool`.
- Nullable ilkel tipler kutulanır: `Int?` → `KotlinInt?` (`.intValue`), `Boolean?` → `KotlinBoolean?` (`.boolValue`), `Long?` → `KotlinLong?`. `String?` → `String?`.
- `List<T>` → `[T]`; `List<Int>` → `[KotlinInt]`. `Map<K, V>` → `[K: V]` (ilkel anahtar/değer kutulu). `Pair<A, B>` → `KotlinPair<A, B>` (`.first`, `.second`, ikisi de optional).
- Parametre etiketleri Kotlin parametre adlarıdır: `fun f(partId: Int)` → `f(partId: 1)`.
- **Varsayılan parametreler Swift'e taşınmaz**: bütün parametreleri vermek gerekir. Kurucular için de geçerlidir.
- `data class` kopyası `doCopy(...)` olur ve bütün alanları ister; kaçının.
- `enum class E { A_B }` → `E.aB`. `sealed class` alt türleri `is` / `as?` ile ayrılır.
- Üst düzey fonksiyon ve `const val` → `DosyaAdiKt.ad`.
- Fonksiyon tipleri: `(Int) -> Unit` → `(KotlinInt) -> Void`; dönüş `Unit` olan lambdalar Swift'te `KotlinUnit` döndürmek zorunda kalabilir — başlıktan doğrulayın.
- Swift anahtar sözcüğü ya da `NSObject` üyesiyle çakışan adlar alt çizgi alır (`description` → `description_`, `hash` → `hash_`).
- Farklı paketlerde aynı adlı sınıflar çakışır ve biri `_` son eki alır (bilinen: `MathOperation`, `ChestTypeProgressHelper`). Hangisinin eki aldığını başlıktan doğrulayın.
- Kotlin istisnaları Swift'te yakalanamaz, uygulama çöker: güvenli girdilerle çağırın.
- Ortak kod çok iş parçacıklı kullanım için tasarlanmadı: ana iş parçacığından çağırın.

## Ortak koda ekleme

Eksik bir **saf** kural (Android'de bir Fragment'ın içinde kalmış hesap) bulursanız `ios/shared/src/commonMain/kotlin/com/sorobit/shared/<paket>/` altına Kotlin olarak ekleyebilirsiniz; koşulu:

- Android davranışının kopyası olmalı, kaynağı dosya başında yazılmalı.
- `commonTest`'e testi yazılmalı ve çalıştırılmalı:
  `cd ios && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :shared:jvmTest`
  (Gradle çıktısı Türkçe yerelde "BUİLD SUCCESSFUL" yazar; başarıyı çıkış kodundan anlayın.)
- Var olan ortak dosyaların davranışı değiştirilmez.

## Rapor

İş bitince kısa ve somut bir Türkçe rapor: yazılan dosyalar; Android'deki ekranın yapısı ve kaynak dosyaları; birebir alınanlar ve basitleştirilenler; dışa açılan giriş görünümünün tam imzası; kullanılan görsel, renk ve metin adlarından eksik olanlar; başlıkta doğrulanamayan Kotlin çağrıları (dosya:satır); eksik kalanlar. Doğrulanmayan şey doğrulanmış gibi sunulmaz.

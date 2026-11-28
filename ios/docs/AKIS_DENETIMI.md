# iOS akış denetimi (salt okunur kod okuması)

Tarih: 28.11.2026. Dal: `claude/sorobit-ios-phase-1-b59a74` (son commit `2583154`).

## Yöntem ve sınırlar

- Kaynak dosyalarda değişiklik yapılmadı. Yazılan tek dosya bu rapor.
- Bulgular yalnızca kod okumasına dayanıyor. Uygulama çalıştırılmadı, hiçbir akış tıklanarak denenmedi.
- Doğruluk ölçütü Android'di (`app/src/main/java/com/example/app/`).
- Yedi akış `Yonetim/` dosyalarından başlanarak izlendi:
  - Akış 1, 2, 3 ve 7 ile Yonetim bağlantıları doğrudan okundu.
  - Akış 4 (Görevler/Kupa/Sandık) ile akış 5 ve 6 (Mağaza/Plan/Özelleştirme/Profil) iki yardımcı denetçiye okutuldu. Onların en önemli bulguları kodda yeniden açılıp doğrulandı; doğrulananlar "(doğrulandı)" diye işaretli.
- Başka oturumlar `SobiTest/`, `Yaris/`, `Harita/`, `Yonetim/` ve `KorDers/` içinde çalışıyor. Bu yüzden satır numarası yerine dosya ve işlev adı verildi.
- **[sahte]** etiketi: hata yalnızca bugünkü sahte servis katmanında. Gerçek Firebase/StoreKit bağlanınca kendiliğinden kalkabilir ama bugün akışı bozar ve testi yanıltır. Etiketsiz bulgular gerçek servisle de sürecek kod/akış hatalarıdır.
- Önem dereceleri:
  - **KRİTİK:** çökme, akış kilidi ya da veri kaybı.
  - **ÖNEMLİ:** yanlış davranış.
  - **KÜÇÜK:** etkisi sınırlı sapma.

---

## KRİTİK

### K1. Aynı kartta art arda gelen aynı kupa sonucu sessizce yutuluyor (kupa puanı kaybı) (doğrulandı)

- **Yer:**
  - `Kupa/KupaModeli.swift` → `sonucuIsle`
  - `Kupa/KupaServisler.swift` → `struct KupaDersSonucu: Equatable { kartNo, delta }`
  - `Yonetim/UygulamaYoneticisi.swift` → `kupaDersiniBaslat`
- **Senaryo:**
  1. Kupa Yolu panelinde aynı kartta, aynı zorlukta iki ders üst üste kaybedilir. Kayıp deltası sabit, ör. −20.
  2. İkinci dönüşte gelen `KupaDersSonucu(kartNo: 1, delta: -20)` bir öncekine eşit olduğu için işlenmez.
  3. Sonuç: skor animasyonu oynamaz ve `submitCupResult` çağrılmaz. Kayıp sunucuya hiç yazılmaz; çocuk kaybettiği kupayı kaybetmemiş olur. Kazançta da delta tabloya bağlıysa aynı şey olur.
- **Kanıt:**
  ```swift
  guard let sonuc = sonuc, sonuc != islenenSonuc else { return }
  islenenSonuc = sonuc
  ```
  Yönetici her derste `kupaSonucu = nil` yapıyor. `onChange(nil)` dalı `guard let` ile çıktığı için `islenenSonuc` eski değerde kalıyor.
- **Android'de doğrusu:** `TasksFragment.consumePendingCupDelta` her dönüşte `GlobalValues.pending*CupDelta` alanını okuyup sıfırlıyor; eşitlik karşılaştırması yok.
- **Düzeltme:** `KupaDersSonucu`'na her derste yeni bir kimlik ekle (UUID ya da yöneticideki sayaç) ve karşılaştırmayı bu kimlikle yap.

### K2. Çıkış yapınca bu oturumda açılan bütün hesaplar siliniyor [sahte] (doğrulandı)

- **Yer:** `Yonetim/UygulamaYoneticisi.swift` → `cikisYapildi`.
- **Kanıt:** `kimlikServisi = UygulamaYoneticisi.yeniKimlikServisi(oturumAcik: false)`. Yeni `SahteKimlikServisi`, yeni ve boş bir `SahteKullaniciKayitServisi` ile kuruluyor; şifreler, veli kayıtları ve kullanıcı belgeleri gidiyor.
- **Senaryo:** Kayıt ol → Ayarlar → Oturumu kapat → aynı e-postayla giriş dene → "kayıtlı değil". Çıkıştan sonraki her giriş testi (hazır `ogrenci@` / `ogretmen@` hesapları dışında) imkânsız.
- **Android'de doğrusu:** `signOut` yalnızca oturumu kapatır.
- **Düzeltme:** Servisi yeniden kurma; `await kimlikServisi.cikisYap()` çağır. Yeniden kurmak gerekiyorsa `kayit` nesnesini aktar.

### K3. Profil sekmesinden çıkıp dönünce profil değişiklikleri kayboluyor [sahte] (doğrulandı)

- **Yer:**
  - `Kabuk/AnaKabuk.swift` → `profil` ve `digerSekme`
  - `Profil/ProfilEkrani.swift` → `init`
- **Kanıt:** `let kaynak: ProfilDeposu = depo ?? SahteProfilDeposu()`. AnaKabuk `depo:` vermiyor. ProfilEkrani `switch secili` içinde kurulduğu için her sekme dönüşünde yeni `@StateObject` ve yeni sahte depo oluşuyor.
- **Senaryo:** Avatarı düzenle → Kaydet → Görevler sekmesine geç → Profil'e dön. Avatar, ad, takip et/bırak ve profil abaküs seçimi başa döner.
- **Düzeltme:** Depoyu tek nesne yap (`static let ortak` ya da yöneticide tut) ve AnaKabuk'tan `depo:` olarak ver.

---

## ÖNEMLİ

### Ö1. Haritadan açılan derste enerji (can) hiç harcanmıyor (doğrulandı)

- **Yer:**
  - `Harita/HaritaModeli.swift` → `eylemDugmesi`: karar `USE_ENERGY_AND_START` olsa da yalnızca hedef yayınlanıyor.
  - `Yonetim/YonetimDersAkisi.swift` → `haritaDersiniBaslat` / `dersEkrani`: enerji çağrısı yok.
  - Abaküs dersi: `AbakusDersModeli.dersiBaslat` → `hizmetler.enerjiDus`. `GercekAbakusDersHizmetleri.enerjiDus` bunu `temel`e (boş) iletiyor.
  - Kör ders: `SahteKorDersEnerjiServisi.enerjiHarca {}` boş.
- **Senaryo:** Kaç ders oynanırsa oynansın üst çubuktaki can değişmez. `NOT_ENOUGH_ENERGY` dalı (panel kapanır, mağaza açılır) ancak açılıştaki eksik canla görülebilir. Pro'nun sonsuz canı derste fark yaratmaz. "Eğitimi tekrarla" için de durum aynı.
- **Android'de doğrusu:** `LessonAdapter.spendLessonEnergyThen`. Ders açılmadan önce `energyManager` 1 can düşer, yetmezse mağaza açılır; "Eğitimi tekrarla"da da aynı.
- **Düzeltme:** Haritadaki eylemde (ya da `haritaDersiniBaslat`'ta) `MagazaBagimliliklar.sahte.enerji` üzerinden 1 can düşülsün, ardından `oturumuYenile` çağrılsın. Kör ders ve abaküs dersinin enerji servisleri de aynı nesneye bağlansın (kupa için bkz. Ö6).

### Ö2. Satın alınan boncuk ve çerçeve derslerde hiç görünmüyor (doğrulandı)

- **Yer:**
  - `Yonetim/UygulamaKokGorunumu.swift` → `abakusDers`: `AbakusDersEkrani`'na `stil:` verilmiyor, varsayılan `.varsayilan` kullanılıyor.
  - Aynı dosyada `korDers`: `.abakusStili(_:)` hiç çağrılmıyor.
- **Kanıt:** Projede `AbakusStili(` yalnızca `static let varsayilan = AbakusStili()` içinde geçiyor. Kullanıcı tercihinden stil üreten hiçbir yer yok.
- **Senaryo:** Özelleştirmede altınla boncuk satın alınır ve seçilir. Abaküs dersi, kör ders ve öğreticide abaküs hep varsayılan gri soroban olarak görünür.
- **Android'de doğrusu:** `AbacusFragment.onResume` ve `BlindingLessonFragment.onResume` → `applyCustomization()`.
- **Düzeltme:** Özelleştirme tercihlerinden (`AbacusPreferences` anahtarları) `AbakusStili` kuran bir yardımcı yaz; yönetici iki ders ekranına da bunu versin.

### Ö3. Sandık ödülü hiçbir cüzdana yazılmıyor [sahte] (doğrulandı)

- **Yer:**
  - `Sandik/SandikGirdisi.swift` → `SahteSandikOdulServisi`: her açılışta sabit `odulTuru: "GOLD"`, 150 dönüyor ve hiçbir yere yazmıyor.
  - Yöneticideki bütün `kapandi` kapanışları sonucu atıyor (`{ _ in ... }`).
- **Senaryo:** Ders, görev, günlük soru, reklam ya da kupa yolu sandığı "+150 altın" gösterir. Üst çubuk, mağaza ve özelleştirme bakiyesi değişmez.
- **Android'de doğrusu:** Sunucu (`ServerRewards.openChest`) cüzdana yazar; `MainActivity.applyWalletToUi` dinleyiciden güncellenir.
- **Düzeltme:** Sahte servis yanıt vermeden önce `MagazaBagimliliklar.sahte.cuzdan.sunucuDegistirdi(...)` çağırsın. Seri ödülü bunu zaten böyle yapıyor (`SahteSeriServisi.oduluCuzdanaYaz`).

### Ö4. Günlük sorunun anahtarı ayrı bir sahte cüzdandan harcanıyor [sahte] (doğrulandı)

- **Yer:**
  - `GunlukSoru/GunlukSoruVeri.swift` → `SahteGunlukSoruCuzdani` (kendi anahtar sayısı var)
  - `Gorevler/GorevlerEkrani.swift` → `init`: `cuzdan: SahteGunlukSoruCuzdani.ortak`
- **Senaryo:** "Mücadeleye devam et" ile anahtar harcanır ama üst çubuktaki anahtar düşmez. Mağazadan anahtar alınsa bile günlük soru kendi bakiyesi bitince "anahtar yetersiz" der.
- **Android'de doğrusu:** `MainActivity.spendKeys` ana cüzdandan (`users/{uid}.keys`) harcar.
- **Düzeltme:** `GunlukSoruCuzdani`'nın sahte uygulaması `MagazaBagimliliklar.sahte.cuzdan`'a yönlendirilsin.

### Ö5. Kupa Yolu sandığı açıldıktan sonra sonsuza dek "hazır" kalıyor [sahte]

- **Yer:**
  - `Sandik/SandikGirdisi.swift` → `SahteSandikOdulServisi.kupaYoluSandiginiAc`: yalnızca yanıt dönüyor.
  - `Kupa/KupaServisler.swift` → `SahteKupaDeposu.defter`: hiç yazılmıyor.
- **Senaryo:** `KupaYoluOdulEkrani`'nda hazır eşiğe dokunulur ve sandık açılır. Kapanınca `yukle()` ekranı tazeler ama eşik yine HAZIR görünür; aynı sandık tekrar tekrar açılabilir.
- **Android'de doğrusu:** `claimCupPathChest` defteri sunucuda ilerletir; `CupPathRoadFragment.onResume` defteri yeniden okur.
- **Düzeltme:** Sahte açılış, `KupaOrtami.sahte` defterindeki `lastClaimed` / `claimed` alanlarını ilerletsin.

### Ö6. Kupa dersinde enerji harcanmıyor; Kupa paneli ayrı ve sabit bir enerji gösteriyor [sahte]

- **Yer:**
  - `Yonetim/UygulamaKokGorunumu.swift` → `korDers`: `enerji: SahteKorDersEnerjiServisi()`.
  - `Kupa/KupaServisler.swift` → `SahteKupaDeposu.guncelEnerji = 5`, `sonsuzEnerji = false`.
- **Senaryo:**
  - Kupa dersinde yanlış cevapta ya da çıkışta `CUP_USE_ENERGY_AND_CLOSE` çalışır ama can düşmez; sınırsız kupa oynanır.
  - Panel her zaman 5 can gösterir ve enerji 0 iken de BAŞLAT'a izin verir.
  - Panelden mağazaya gidip can alınsa da panel değişmez.
- **Android'de doğrusu:** Tek `EnergyManager`; panel aynı kaynaktan okur.
- **Düzeltme:** `KupaEnerjiKaynagi` ve kör dersin enerji servisi, `MagazaBagimliliklar.sahte.enerji` üzerine kurulan birer bağdaştırıcı olsun.

### Ö7. Kupa dersi dönüşünde Android'deki rozet adımı atlanmış

- **Yer:** `Yonetim/UygulamaYoneticisi.swift` → `korDersBitti`, `.kupa` dalı. Dalda yalnızca `kupaSonucu`, `kapat` ve `seriSorusunuDene` var.
- **Senaryo:** Kupa skoru Dino/Timsah/Kartal rozet eşiğini geçer. Kutlama açılmaz ve sahte rozet belgesindeki sayaçlar değişmez.
- **Android'de doğrusu:** `TasksFragment.consumePendingCupDelta` → `awaitCupBadgePayloads` → `openCupBadgeCelebrationAfterAnimation`. Kutlama, panel animasyonundan sonra ve panel dokunuşa kilitliyken açılır.
- **Düzeltme:** Kupa sonucu işlendikten sonra rozet kazanımı hesaplansın (ortak koddaki karşılığıyla) ve `rozetKutlamasiniAc` çağrılsın.

### Ö8. Çıkışta ders ilerlemesi ve diğer hesap durumları sıfırlanmıyor; sonraki hesap öncekinin ilerlemesini görüyor

- **Yer:** `Yonetim/UygulamaYoneticisi.swift` → `cikisYapildi`.
- **Kanıt:** Yalnızca `katmanlar`, `kupaSonucu`, `haritaModeli.kisimSeciminDon()` ve kimlik servisi sıfırlanıyor. Şunlar olduğu gibi kalıyor:
  - `ilerleme` (`YonetimIlerlemeDeposu.partlar`) ve `GlobalLessonData` belleği
  - `YonetimRozetKuyrugu.ortak.bekleyen`
  - `ilkOgreticiAkisi`
  - sahte cüzdan ve Pro (`enerji.sonsuz`)
  - özelleştirme sahipliği
  - `SahteSeriServisi.bagliUid`
- **Senaryo:**
  1. A hesabıyla 5 ders geçilir, çıkış yapılır, B hesabıyla girilir.
  2. B'nin haritası A'nın ilerlemesini, cüzdanını ve Pro durumunu gösterir.
  3. Çıkıp hesapsız devam edilirse, `bagliUid` hâlâ dolu olduğu için eski hesabın seri dondurması satın alınabilir.
- **Android'de doğrusu:** Çıkışta `LoginStartActivity` temiz yığınla açılır. Girişte `GlobalLessonData.initialize` yeni uid'nin Firestore listesini okur.
- **Düzeltme:** `cikisYapildi` şunları yapsın:
  - `ilerleme`yi yeniden kursun ya da `partlar`ı boşaltsın;
  - rozet kuyruğunu temizlesin;
  - `ilkOgreticiAkisi = false` yapsın;
  - seriyi `hesabaBagla(nil)` ya da benzeri bir çağrıyla ayırsın.

  Gerçek Firestore'da da bellek önbelleği (`partlar`) aynı biçimde kalacağı için bu madde [sahte] değildir.

### Ö9. Hesap silme ve "yeniden giriş gerekli" bildirimleri hiç görünmüyor (doğrulandı)

- **Yer:** `Profil/ProfilDuzenleEkrani.swift` → `hesabiSil` ve `cikVeYonlendir`.
- **Kanıt:** Önce `gezinme.bildir("ui_hesabiniz_basariyla_silindi")`, hemen ardından `gezinme.oturumKapandi()` → `asama = .giris`. Balonu çizen ProfilEkrani aynı anda yok ediliyor.
- **Senaryo:** Hesap silinir ve kullanıcı hiçbir onay görmeden başlangıç ekranına düşer. App Store incelemecisi silmenin bittiğini görmeyebilir.
- **Android'de doğrusu:** Toast ekran değişse de görünür.
- **Düzeltme:** Mesaj kök düzeyinde gösterilsin ya da giriş akışına (`GirisModeli.bildir`) taşınsın.

### Ö10. Profil, kimlik ve kayıt üç ayrı doğruluk kaynağı [sahte]

- **Yer:**
  - `Profil/ProfilSahteDepo.swift`: hep "Elif", `ornek-uid-ben`.
  - `Giris/SahteKimlikServisi.swift`: gerçekte oturumu açılan hesap.
  - `YonetimSeriAkisi.seriHesabiniBagla`: seriyi kimlik servisindeki uid'ye bağlıyor.
- **Senaryo:**
  - Profil, giriş yapan hesabı değil sabit kişiyi gösterir.
  - `hesabiSil` yalnızca profil deposundaki kişiyi siler; silinen hesapla yeniden giriş yapılabilir.
  - Misafir (hesapsız) kullanıcı da Profil'de dolu bir hesap görür.
- **Düzeltme:** Sahte profil deposu oturumu ve silmeyi `UygulamaYoneticisi.kimlikServisi` üzerinden yapsın.

### Ö11. Plan iki kaynaktan okunuyor: Pro alındıktan sonra Ayarlar'da "Ödemeler ve abonelikler" çıkmıyor [sahte]

- **Yer:** `Profil/ProfilHesapAyarlariEkrani.swift` → `planiYukle`. `belge.plan` sahte profil deposunda sabit `"Free"`; Pro bilgisi ise sahte cüzdanda.
- **Düzeltme:** Planı `MagazaBagimliliklar.sahte.cuzdan.cuzdan.plan`'dan oku.

### Ö12. Özelleştirmede alınan boncuklar profil abaküs panelinde görünmüyor; özelleştirme kimliği sabit, misafir de satın alabiliyor [sahte]

- **Yer:**
  - `Profil/ProfilSahteDepo.swift`: `boncuklarim` sabit.
  - `Ozellestirme/OzellestirmeServisler.swift` → `OzellestirmeBagimliliklar.sahte`: `kimlik = "sahte-kullanici"`.
- **Senaryo:** Misafirken Abaküs sekmesinde altın harcanabiliyor; Android'de uid null iken panel açılmıyor. Hesap değişince aynı özelleştirme kullanılıyor.
- **Düzeltme:** Sahiplik tek servisten okunsun; kimlik yöneticinin oturumundan verilsin, misafirde nil olsun.

### Ö13. Takip edince ya da bırakınca kök profilin sayaçları eski kalıyor

- **Yer:** `Profil/ProfilSayfasi.swift`, `ProfilSayfaModeli.gorundu`. Kök profil ZStack'te bellekte kaldığı için `onAppear` bir kez çalışıyor. `yenilemeSayaci`'nı yalnızca ProfilDuzenle artırıyor.
- **Android'de doğrusu:** Geri dönünce ProfileFragment yeniden kurulur ve `loadUserData` çalışır.
- **Düzeltme:** `takipEt` / `takibiBirak` başarısında `gezinme.yenilemeSayaci += 1`.

### Ö14. Ebeveyn kapısı hiçbir yerde yok (App Store)

- **Kapısız noktalar:**
  - Mağazadaki doğrudan satın almalar (`MagazaModeli.satinAl` → `paketSatinAl`) ve plan ekranındaki satın alma düğmesi.
  - Dış bağlantılar: `PlanYasalSatiri`, "Aboneliği yönet", Ayarlar'daki `abonelikSayfasiniAc`, kayıttaki şartlar/gizlilik bağlantıları.
- **Kanıt:** iOS kodunda `ParentGate` ya da bir ebeveyn kapısı yok. README "ebeveyn kapısı her iki durumda da eklenmeli" diyor.
- **Not:** Uygulama Kids kategorisinde olmasa da hedef kitle çocuk olduğu için inceleme bunu isteyebilir.
- **Düzeltme:** Android `ParentGate.kt`'in karşılığını yaz; satın alma ve dışarı çıkan bağlantıların önüne koy.

---

## KÜÇÜK

1. **Hesapsız (misafir) kullanıcıda çıkış düğmesi ve kural düğmeleri görünüyor.**
   - Yer: `UygulamaKokGorunumu.abakusDers` → `cikisGizli: nesne.ilkOgreticiAkisi`.
   - iOS yalnızca 1. öğreticinin hesapsız dersinde gizliyor. Android `AbacusFragment.isFirstRunLesson()` = `currentUser == null`, yani her misafir derste gizli.
   - Etkisi: `.harita` hedefli misafir (bu cihazda önceden hesap açılmış) derslerde çıkış ve kural düğmelerini görür. Kör derse `cikisGizli` hiç verilmiyor.
2. **Sobi analizi açılınca seri çalışma süresi duruyor ve sahte ek süre bir kez daha ekleniyor.**
   - Yer: `YonetimSeriAkisi.seriCalismaEkraniniGuncelle` yalnızca üst katmana bakıyor. `.sobiAnaliz` üstteyken `ad = nil` olur, `SahteSeriServisi.calismaEkrani` "ders kapandı" sayıp +300 sn ekler.
   - Android'de `MistakeReviewDialog` bir dialog; altındaki fragment çalışma ekranı olarak kalır.
   - Düzeltme: `.sobiAnaliz` üstteyken bir alttaki katmana bakılsın.
3. **Ders sırasında arka plana geçiş işlenmiyor.**
   - Yer: `AbakusDersModeli`, `KorDersModeli`, `OgreticiGorunumModeli`. `kayboldu` yalnızca `onDisappear`'da çalışıyor.
   - Android `onPause`'ta öğrenme süresi ölçümünü durduruyor. Öğreticide (`TutorialFragment.onPause/onResume`) ses ve daktiloyu duraklatıp kaldığı yerden sürdürüyor.
   - iOS'ta öğretici sesi ve daktilo arka planda ne olacağına bırakılmış. Uygulama askıya alınınca ses kesilir; dönüşte devam ettirilmez.
   - Kök görünüm `scenePhase`'i yalnızca seri sayacı için kullanıyor.
4. **"Yeni seri" ekranından çıkış yolu yok.**
   - Yer: `Seri/SeriYeniSeriEkrani.swift`: yalnızca "Başla/Devam" var, seçim yapılmadan kapanmıyor.
   - Android'de sistem geri tuşu ekranı kapatır ve ders sonrası kuyruğu sürdürür. iOS'ta çocuk bir meydan okuma seçmeye zorlanıyor.
   - Düzeltme: Sol üste kapatma düğmesi; o da `bitti()` çağırsın.
5. **Kupa yolu ekranında hazır eşiğe hızlı çift dokunuş iki sandık katmanı açabilir.**
   - Yer: `Kupa/KupaYoluOdulEkrani.swift` → `esigeDokunuldu`. Ne ekranda ne yöneticinin `ac` işlevinde kilit var.
   - Görevlerde `akisAcik`, günlük soruda `dokunmaKilitli` bu işi görüyor.
6. **Günlük soru dersi ya da ödül sandığı kapanınca görev listesi hemen yenilenmiyor.**
   - Yer: `Gorevler/GorevlerEkrani.swift`. Liste `onAppear`'da, 60 sn'lik saatte ve sahne aktifleşince yenileniyor. Katman yığını ZStack olduğu için sekmenin `onAppear`'ı tekrar çalışmıyor.
   - Android'de `TasksFragment.onResume` yeniliyor. Bugün görünür etkisi yok.
7. **Üst çubukta Pro kalbi yok.**
   - Yer: `Kabuk/UstCubuk.swift`: hep `heart_ic`.
   - Android: `EnergyDisplay.apply(isPremium = …)` → `heart_pro_ic`.
8. **Plan ekranında Pro alındıktan sonra mağazanın Süper düğmesi eski metinde kalıyor.**
   - Yer: `MagazaModeli.superDugmesiniYaz` cüzdan dinleyicisinden çağrılmıyor. Android'de de aynı olabilir (bkz. şüpheler).
9. **İkinci açılışta hesaplar ve ilerleme yok [sahte].**
   - Kimlik, ilerleme, seri, cüzdan ve rozet bellekte tutuluyor. Buna karşılık `GirisYerelDepo` (UserDefaults) `baslangicGosterildi` ve "hesap açıldı" bayraklarını saklıyor.
   - İkinci açılışta kullanıcı başlangıç ekranına gelir ama kayıtlı hesabı yoktur. `-oturum acik` argümanı yalnızca hazır test hesabını açar.
   - Gerçek Firebase bağlanınca kalkar.

---

## Şüpheler (kanıt yetersiz, doğrulanmalı)

1. **Rozet kuyruğu yarışı.**
   - `SahteRozetKazanimServisi.sayaclariArtir` kuyruğu 0,2 sn gecikmeyle veriyor. `YonetimDersAkisi.oduleGec` hemen sandığı açıyor.
   - Sandık 0,2 sn'den kısa sürede kapanırsa (animasyon buna izin vermiyor gibi), `dersSonrasiKuyruguIsle` boş kuyruk görür. Rozet bir sonraki derse kadar bekler.
   - Gerçek Firestore'da gecikme daha uzun olacağı için risk artar. Android kuyruğu `chest_closed`'ta bekleyen yük gelene kadar tutuyor mu, bakılmalı.
2. **Hesapsız ilk öğreticide 0. adımda yanlış cevap.**
   - `OgreticiOynatici.yanlisTamam` 0. adımda `KAPAT` döndürüyor; `ilkAcilis`'e bakmıyor.
   - 1. öğreticinin ilk adımı soru içeriyorsa hesapsız kullanıcı haritaya düşer ve akış (öğretici → ders → kayıt) bozulur. Kullanıcı 1. derse yeniden girip öğreticiyi baştan açabildiği için kilit değil.
   - 1. öğreticinin 0. adımı soru mu, bakılmadı.
3. **Seri eşitleme döngüsü.**
   - `oturumuYenile` → `seri.esitle` → başarıda `bildir()` → `oturumuYenile` → `esitle` zinciri var.
   - `onSubmitSucceeded` → `markDailyPing` + `onSyncAccepted` bekleyenleri temizlediği sürece zincir bir turda durur. Ortak koddaki `pendingSyncDays` kabul sonrası gerçekten boşalıyor mu, testle doğrulanmadı.
   - Boşalmazsa uygulama 0,3 sn'de bir sonsuz eşitlemeye girer.
4. **Günlük soru anahtarla devamda kesilme.**
   - `GunlukSoruModeli.anahtarlaDevam`: 1,37 sn'lik iyileşme sırasında kart yeniden bağlanırsa (sahne aktifleşmesi, periyot devri) `iyilesmeSonrasi` çağrılmayabilir.
   - Sonuç: anahtar harcanır, soru başlamaz, `dokunmaKilitli` sekme değişene kadar true kalır. Pencere dar.
5. **Gerçek kayıt servisiyle günlük sonucun iki kez yazılması.**
   - `KorDersModeli.gunlukSoruyuBitir` sonucu hem `kayit.gunlukSoruSonucunuKaydet`'e hem de yöneticinin `bildir` → `GunlukSoruModeli.soruBitti` → `depo.soruSonucunuYaz` yoluna veriyor.
   - Bugün ilki boş olduğu için tek yazım var; ikisi gerçek olursa çift yazım olur.
   - Eşzamansız gerçek kayıtta `sonlandi` geç kurulduğu için çıkış düğmesi ikinci bir yazım tetikleyebilir.
6. **Süper düğmenin eski metni Android'de de var olabilir:** `ShopFragment` `SubscriptionCta.apply`'ı yalnızca açılışta ve fiyat gelince çağırıyor.
7. **13 yaş altı hesaplarda kısıt yok.** Giriş sonrası mağaza, arkadaş arama ve takip açık. Android'de `AgeConsentManager`'a bağlı bir kısıt olup olmadığına bakılmadı.

---

## İzlendi, sorun bulunamadı

### Akış 1: ilk açılış

- **Açılış → misafir hedefi:**
  - Yol: `GirisModeli.acilisBasladi` → `acilisiYonlendir` → `misafirOlarakDevam(.ogretici)` → `UygulamaYoneticisi.ilkOgreticiyiAc`.
  - Öğretici `ilkAcilis = true` ile açılıyor; çıkış ve atla gizli (`OgreticiSahnesi.cikisGorunur`).
- **Öğreticiden derse:** "Devam Et" → `ogreticiGecildi` → `degistir` ile abaküs dersi. Dersin çıkışı gizli (`cikisGizli`).
- **Dersten sonuca ve kayda:**
  - Sonuç → `sonucDevam` → (başarılı ya da başarısız) `ilkDerstenSonraGiriseGec`.
  - Bu işlev katmanları temizliyor, `tutorial1Pending` listesini kuruyor ve `girisKimligi += 1` ile giriş akışını açılışı atlayarak başlangıçtan açıyor. Sandık atlanıyor (Android `redirectToLoginForFirstTutorial` ile aynı).
- **Kayıttan kabuğa:** Kayıt soruları → kayıt/kod → `tekCihazDenetimi` → `girisYapildi` → kabuk. `AnaKabuk.onAppear` → `seriHesabiniBagla`: kayıt cevapları uygulanıp siliniyor.
- **Çıkış:** Katmanlar kapanıyor, giriş akışı `.id(girisKimligi)` ile sıfırdan ve açılışsız kuruluyor (K2 ve Ö8 dışında).
- **Uygulamanın ikinci kez açılması (yönlendirme kuralları):**
  - `baslangicGosterildi` (UserDefaults) doluysa başlangıç ekranı açılıyor.
  - Öğretici yarıda bırakıldıysa öğretici yeniden açılıyor (Android'le aynı).

### Akış 2: haritadan ders

- **Hedefin teslimi:** Panel → `eylemDugmesi` → `hedef` → (`receive(on: main)`) `haritaHedefiGeldi`.
  - Hedef bir sonraki turda nil'leniyor; çift teslim yok.
  - Panel `hedef`ten önce kapandığı için çift dokunuş ikinci hedef üretmiyor.
- **Anahtarla kilit açma:**
  - Bakiye yetmezse bildirim ve mağaza açılıyor.
  - Sunucu reddederse düğme yeniden açılıyor.
  - Başarıda `dersiYaz` → `yenile` → 220 ms sonra panel açılmış hâliyle yeniden çiziliyor; `asyncAfter` `[weak self]` ve sınır kontrollü.
- **Öğretici kararı:** `baslamaHedefi` ortak koddan geliyor; `tutorialIsFinish` yalnızca sonuç claim'inde yazılıyor (Android `LessonResult.kt` ile aynı).
- **Sonuçtan haritaya:**
  - Zincir: sonuç → (`dersOduluAtlanir` / `sandikTekrarOynandi`) → `oduleGec` → maraton planı, rozet sayaçları, `ilerlet`, `haritaModeli.yenile` → sandık → `kapandi` → `dersSonrasiKuyruguIsle` → rozet kutlaması → yeni seri → maraton rehberi.
  - Sıra Android `runPostLessonQueue`'nun iOS'ta var olan adımlarıyla aynı.
  - `kapandi`, sandık katmanı kalkmadan önce çağrılıyor. Rozet ya da seri katmanı önce ekleniyor, sonra sandık kalkıyor; yığın tutarlı.
  - Kutlama şeridi araya girmiyor.
- **Üst çubuk:**
  - Cüzdan dinleyicisi `[weak self]`.
  - Enerji ve seri `oturumuYenile` ile okunuyor; bu işlev her katman kapanışında, sekme değişiminde ve 30 sn'de bir çalışıyor.

### Akış 3: ders içi

- **Abaküs dersi:**
  - Kontrol Et: 100 ms çift basış eleniyor.
  - Doğru/yanlış paneli: `panelAnimasyonda` kapısı var.
  - Dersin bitişi `dersBitti` kapısıyla tek sefer.
  - Süre `Timer` + `[weak self]`, `deinit`'te geçersiz kılınıyor.
  - Sobi analizi aç/kapat: sandık dersinde süre duruyor, kapanınca 1 sn sonra sürüyor; `zamanlayici == nil` koşulu çift sayacı önlüyor.
- **Kör ders:**
  - Dizi oynatma `DispatchWorkItem` ile; atlama ve sıfırlama bekleyen işi iptal ediyor.
  - `kayboldu` dizi ve süre zamanlayıcılarını durduruyor.
  - `sonlandi` kapısı çift bitişi engelliyor; kupa sonucu tur başına bir kez (`kupaSonucuHakkiAl`).
- **Rehber, kural paneli/kitabı, ipucu:** `@StateObject` olarak tutuluyor. Kapanışlar kardeş nesneleri yakalıyor (`dersModeli`, `kural`, `denetleyici`); retain döngüsü yok. Rehber bir kez başlıyor.
- **Dersten çıkış:** Onay yok; Android'de de yok (`closeFragment`). Kupa ve günlük soru kiplerinde çıkış ceza ve sonuç kurallarını uyguluyor.
- **Sonuç ekranı:** `devamaIzinVar` çift dokunuşu eliyor.

### Akış 4: Görevler / Keşfet

- Görev sandığı, günlük soru başlatma → kör ders → `bildir(true/false)` → ödül sandığı → rozet kutlaması yolları çalışıyor (Ö3, Ö4 ve K1 dışında).
- Kupa paneli → zorluk → ders → panel dönüşü çalışıyor.
- Panelden yol ekranı ve sandık önce `kapat` sonra açılış sırasıyla çalışıyor.

### Akış 5: mağaza ve plan

- **Pro satın alma (sahte):** Enerji sonsuz oluyor, üst çubukta "∞" görünüyor, haritanın enerji kararı güncelleniyor.
- **Katman yığını:** Mağaza → plan → mağaza ve mağaza → reklam → sandık → mağaza geçişlerinde sıkışma yok; `magazaAc` / `planKarsilastirmaAc` kopya açmıyor.
- **Seri dondurma:** Mağaza ve seri ekranı aynı `SahteSeriServisi`'ni kullanıyor; altın aynı sahte cüzdandan düşüyor.
- **Özelleştirme:** Kayıt başarısız olunca `PURCHASE_ROLLBACK` ile iade yapılıyor.
- **App Store gereksinimleri:**
  - Geri yükleme düğmesi hem mağazada hem plan ekranında var.
  - Abonelik açıklaması fiyat, dönem, deneme, otomatik yenileme ve iptal yolunu içeriyor.
  - Kullanım Şartları ve Gizlilik bağlantıları var.

### Akış 6: profil

- **Avatar:** Düzenle → kaydet → kök profil hemen yeniden çiziliyor (yalnızca sekme değişene kadar; K3).
- **Ses ayarları:** Anahtarlar, ses çalıcılarının okuduğu anahtarlarla aynı.
- **Hesap silme:** Onay penceresi açılıyor, kapatılamayan "siliniyor" katmanı gösteriliyor, oturum kapanıp giriş ekranına dönülüyor (Ö9 dışında).

### Akış 7: seri

- **Gün tamamlanması ve kutlama:**
  - Ders katmanı kalkınca süre eklenip gün tutturuluyor.
  - Kutlama yalnızca kabukta ve katman yokken açılıyor; üstüne ekran gelirse çekiliyor.
- **Meydan okuma ve kilometre taşı ödülleri:** `toplaniyor` çift dokunuşu eliyor; ödül sahte cüzdana yazılıyor, üst çubuk tazeleniyor.
- **Hedef ve hatırlatma pencereleri:** Pencere kapanıp değer yazılıyor ve eşitleniyor. Bildirim izni sahte.
- **Yeni seri sorusu:**
  - Ekran açılınca "bugün soruldu" işaretleniyor.
  - Kapanınca maraton rehberi deneniyor; rehber 0,6 sn sonra ve katman yoksa açılıyor.

### Eşzamanlılık ve Swift güvenliği

- Kapsamdaki klasörlerde `as!`, `try!` ve zorla açma yok. Tek `fatalError`, `init(coder:)` içinde.
- Dizi indekslemeleri ya sınır kontrollü ya da sabit tablo verisi.
- `asyncAfter` ve `Timer` kapanışlarının tamamı `[weak self]`.
- `@Published` yazımları ana iş parçacığında.
- Yöneticinin abonelikleri `[weak self]`.

---

## Bakılamayanlar

- **Ortak Kotlin kodunun içi:** `require`, `!!`, boş listede `first()` gibi istisna atan yerler sistemli taranmadı. Swift tarafından verilen girdilerin bu ön koşulları sağladığı yalnızca çağrı yerlerinde görüldü.
- **Başka oturumların klasörleri:** `Yaris/` ve `SobiTest/` (yeni yazılıyor) hiç incelenmedi. `KorDers/` ve `Harita/` dosyaları okunurken değişmiş olabilir.
- **Ayrıntılı okunmayan ekranlar:** `SandikEkrani`'nın iç animasyon akışı, `RozetKutlamaModeli` zamanlayıcıları, `ProfilArkadasAraEkrani`, `ProfilGeriBildirimEkrani`, `ProfilKaynaklarEkrani`, giriş akışının veli onayı adımlarının içi, `MagazaKartlari` / `OzellestirmePanelleri` (görsel bileşenler).
- **Ekran galerisi kipi:** Gerçek akışı etkileyip etkilemediği yalnızca yüzeysel kontrol edildi. `girisAtla`, `ornekKipi` ve `-ekran` argümanı için gerçek akışa sızan bir dal görülmedi.
- **Çalışma zamanı:** SwiftUI'nin ZStack içindeki katmanlarda `onAppear` / `onDisappear`'ı ne zaman çağırdığı gibi davranışlar okumayla kesinleştirilemez. Cihazda denenmeli.

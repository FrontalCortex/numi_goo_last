# Swift kırılma listesi (ortak kod güncel Android'e eşitlendikten sonra)

27.11.2026. Ortak kod (`ios/shared/src/commonMain`) güncel Android'e (`claude/soban-gamified-education-prb70m`, versionCode 26) eşitlendi; Swift dosyalarına dokunulmadı. Bu belge `ios/iosApp/iosApp/**/*.swift` içinde bu eşitleme yüzünden **derlenmeyecek** ya da **davranışı değişen** çağrıları listeler.

Nasıl çıkarıldı: `git diff HEAD -- ios/shared/src/commonMain` (eşitleme öncesi ↔ sonrası) iki betikle tarandı. (1) Swift'te `.ad` biçiminde geçen ve eski ortak kodda tanımlı olup yenisinde olmayan her ad; (2) Swift'te `ad(` biçiminde çağrılan ve ortak koddaki parametre listesi (eski ↔ yeni) değişen her fonksiyon/kurucu. Ayrıca değişen sabitler, enum değerleri ve arayüzler elle arandı. **Swift derlenmedi** (bu makinede derlenemez); liste Codemagic'te doğrulanmalı. Kotlin'in varsayılan parametreleri Swift'e taşınmadığından, parametre eklenen her çağrı kırılır.

## A. Derlemeyi kıranlar (6 satır / 5 çağrı, 3 dosya: `Harita/`, `Kupa/`, `Yonetim/`)

| # | Dosya:satır | Eski çağrı | Olması gereken |
|---|---|---|---|
| 1 | `Harita/HaritaModeli.swift:297` | `kisimMantigi.partStates(partIds:hasProPlan:part1ChestComplete:part2ChestComplete:part3ChestComplete:)` | Sona `teacherApproved: Bool` eklenir. Öğrenci için `teacherApproved: false` (Android: onaylı öğretmende bütün kısımlar açık). |
| 2 | `Harita/HaritaModeli.swift:433` | `mantik.actionButtonDecision(item: oge, isInfiniteEnergy: sonsuzEnerji, hasEnoughEnergy: enerjiYeterli)` | `mantik.actionButtonDecision(item: oge, isInfiniteEnergy: sonsuzEnerji, hasEnoughEnergy: enerjiYeterli, globalPartId: seciliPart ?? GlobalLessonData.shared.globalPartId)`. Yeni dönüş değeri `UNLOCK_WITH_KEY` de ele alınmalı (aşağıda B1). |
| 3 | `Kupa/KupaLiderTablosuEkrani.swift:105` | `LiderHamKayit(userId:recordScore:displayName:recordLabel:photoUrl: $0.fotoAdresi)` | Son etiket `avatarConfig:` olur; değeri `publicProfiles/{uid}.avatarConfig` (Android `AvatarConfig.encode` biçimi, boş = varsayılan avatar). Swift'teki `KupaLiderKaydi.fotoAdresi` alanı da avatar dizgesine çevrilmeli. |
| 4 | `Kupa/KupaLiderTablosuEkrani.swift:115` | `fotoAdresi: s.photoUrl` | `s.avatarConfig` |
| 5 | `Kupa/KupaLiderTablosuEkrani.swift:116` | `fotoVar: s.hasPhoto` | `s.hasAvatar` |
| 6 | `Yonetim/YonetimIlerlemeDeposu.swift:162` | `ChestFragmentLogic.shared.updateMapProgress(mapFragmentStepIndex:recordScore:lessonSuccessRate:icon:season:)` | Sona `questionElapsedMs: KotlinLong?` eklenir (sorunun süresi, ms; bilinmiyorsa `nil`). |

(4 ve 5 aynı `LiderSatirVerisi` nesnesinin iki alanı; 3 ile birlikte tek ekranın işi.)

`ios/docs/Shared.h` eşitleme öncesinden kalma; yeni adlar Codemagic'in üreteceği başlıkla doğrulanmalı.

## B. Derlenir ama davranışı değişir (Android'e sadakat için bakılmalı)

1. **Harita alt paneli** (`Harita/HaritaModeli.swift:408-433`, `Harita/HaritaDersAltPaneli.swift`): `LessonAdapterLogic.lessonBottomSheetState` yeni panel düzenine geçti. Metinler değişti (`Başla` / `Devam et` / `Kilitli`, kilit açıklaması), `fireAnimVisible` artık hep `false`, `recordLayoutGone` yalnızca bitmiş sandıkta `false`, `locked` anahtarla açılabilen derste `false`. Yeni alanlar Swift'te kullanılmıyor: `panelState`, `kindText`, `iconName`, `stepRowVisible`, `doneSteps`, `stepCount`, `keyUnlockable`. `actionButtonDecision` kilitli ama anahtarla açılabilen derste (part 1–6) `UNLOCK_WITH_KEY` döndürür; Swift bugün yalnızca `LOCKED_COLLAPSE` / `NOT_ENOUGH_ENERGY` / `USE_ENERGY_AND_START` dizgelerine bakıyor, yeni değer "sonsuz enerjiyle başla" dalına düşer. Kilit açma: `LessonAdapterLogic.unlockedWithKey`, bedel `LESSON_UNLOCK_KEY_COST = 1`.
2. **Harita kartları** (`Harita/HaritaModeli.swift:234, 248`): `lessonProgressKey` artık `"{type}_{stableId}"` (eskiden tür+id+part+başlık+konum). Swift'in sakladığı "altın animasyonu oynandı" anahtarları eski biçimdeyse bir kez yeniden oynar. `shouldStartBreathingAnimation` yalnızca açık ve bitmemiş derste `true`. Yeni kart kuralları (`cardState`, `chestCardStars`, `cardIconName`) Swift'te kullanılmıyor.
3. **Ders liderlik tablosu** (`Kupa/KupaServisler.swift:124-139`, `Kupa/KupaLiderTablosuEkrani.swift:39-48, 151-165`): `KupaLiderlikDeposu.dinle(partId:dersIndeksi:sezon:...)` ve ekran girişi dersin **liste konumunu** alıyor. Android tahta kimliği artık `part_{p}_lesson_{stableId}_season_{s}` (`KupaEkranKoprusu.liderlikTahtaKimligi(partId:lessonKey:season:)`). Firebase bağlanmadan önce `dersIndeksi: Int` → `dersAnahtari: String` (`LessonItem.stableId`) yapılmalı. Fotoğraf yerine avatar (A3–A5).
4. **Ders ilerleme yazımı** (Firebase bağlanınca): `LessonProgressSink` arayüzü değişti (`writeSingleItem(partId:stableId:fields:)`, `writeAllItems(partId:stableIds:itemsFields:)`, yeni `migrateLegacyItemKeys`, `submitBestIfNeeded(partId:lessonKey:...)`). Swift bugün bu arayüzü uygulamıyor; uygulandığında yeni imzalar kullanılmalı. Doküman yolu `GlobalLessonData.itemDocumentPath(uid:partId:stableId:)`.
5. **Ders sonu yönlendirmesi** (`Abakus/`, `KorDers/`, `Sonuc/`): geçme eşiği %10 (abaküs) ve %50 (kör ders, part 4–5) yerine **%75**. Kurallar ortak koddan geldiği için Swift değişikliği gerekmez; başarısız ekranı metni "Başarı oranı %75'ten düşük!". `Abakus/AbakusDersTurleri.swift:44` yorumundaki "50 ms boncuk" artık geçersiz (sandıkta da 300 ms).
6. **Mağaza ürün kimlikleri** (`Magaza/MagazaServisler.swift:268-285`): `BillingCatalog` değerleri değişti: `gold_1200` → `gold_small`, `gold_7000` → `gold_medium`, `gold_15000` → `gold_large`, `keys_10` → `keys_small`, `keys_50` → `keys_medium`, `keys_100` → `keys_large`; yeni `CREDITS_*`, `SOBI_CREDITS_*`. Swift sabitlerin adını kullandığı için derlenir; App Store Connect'te tanımlanacak kimlikler yeni değerler olmalı.
7. **Açılış** (`Giris/GirisModeli.swift:187`): `SplashRules.SPLASH_ANIMATION_MS` 2000 → 1200 ms (`SPLASH_MAX_EXTRA_WAIT_MS` kaldırıldı; Swift kullanmıyor). Yeni marka gösterimi `BRAND_REVEAL_MS`, zorunlu güncelleme bekleme süreleri, `SignupFlowRules` Swift'te yok.
8. **Özelleştirme** (`Ozellestirme/OzellestirmeModeli.swift:549, 698`): `BEAD_COLOR_FEATURE_PANEL.rollbackOnSaveFailure` artık `true` (kayıt başarısızsa harcanan geri verilir); boncuk renk özelliği fiyatı boncuğun para biriminde (`checkBeadColorFeaturePurchase`). Swift dalı kendiliğinden iade yoluna girer.
9. **Kristal** (`Sandik/SandikKristalModeli.swift:88-91`, `GunlukSoru/GunlukSoruVeri.swift:84-87, 266`): `ServerRewards.CrystalOutcome.videoName` kaldırıldı (Android sunucu yanıtından video adını artık okumuyor). Swift bu türü doğrudan kullanmıyor (kendi `GunlukSoruKristalSonucu.videoAdi` alanı ve `CrystalBreakState.videoName` var; ikisi de duruyor), derleme kırılmaz. Sunucu bağlanınca video adı sunucudan beklenmemeli.
10. **Kupa puanı**: `CupScoreRepositorySpec.resolveFetchedCupScore` alan yoksa artık `shouldSeed = false` döndürür (kupa sonucu sunucuda yazılıyor: `CupScoreService`). `Kupa/KupaServisler.swift:75` yorumu ("tohumlanır") eskidi.
11. **Profil rozet kademeleri** (`Profil/ProfilOrtakKod.swift:131`): `ProfileFragmentLogic.levelSpec` artık `BadgeProgressEngine.levelSpec`'e delege; roket rozeti tek tablo (`[3,10,15,25,30]`). İmza aynı.
12. **Günlük soru havuzu** (`GunlukSoru/GunlukSoruVeri.swift:178-181`): şablon artık hiç bitmiş sandık içermediği için sahte veride (`cloudItems: nil`) havuz boş çıkar ve kart "havuz yok" der. Örnek veri bitmiş sandıklı item'larla beslenmeli.

## C. Kontrol edilip sorun bulunmayanlar

- `LessonItem` kurucusu (ilk alan `stableId` eklendi): Swift'te `LessonItem(...)` ya da `doCopy` çağrısı yok.
- `GuidePanelData.imageResId` → `emote`: Swift'te kullanılmıyor.
- `ChestFragmentLogic.SuccessRateEvent.Pass`, `LessonResultFalseLogic.FailRecord` / `failRecordFor` (yeni `successRate` parametresi), `LessonResultLogic.ClaimDecision`: Swift yalnızca `onClaim(lessonItem:)` ve `branch.name` kullanıyor; imzalar aynı.
- `LessonSuccessRateRepository`, `LessonLeaderboardRepository`, `BadgeProgressFirestore`, `UserWalletFirestore.walletFromSnapshot`, `UserWallet` kurucusu, `TimeTracker`, `AbacusBeadController.tutorialBeadDiagnosticsEnabled`: Swift'te çağrı yok (yalnızca yorumlarda adı geçiyor).
- `SeasonClock.currentSeason(nowUtcMs:)` (`Yonetim/YonetimIlerlemeDeposu.swift:59`): etiket ve tip aynı.
- `GlobalLessonData.loadLessonItemsForPart / createLessonItems / onLoadFromFirestore / globalPartId`, `LessonManager.getLessonItem`: imzalar aynı.
- Swift'in uyguladığı Kotlin arayüzleri (`AbacusBeadHost`, `AbacusPrefsStore`) değişmedi. Kaldırılan enum değeri yok.

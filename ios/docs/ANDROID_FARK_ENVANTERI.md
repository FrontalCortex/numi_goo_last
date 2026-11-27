# Android fark envanteri: ESKI (origin/master, 2 Eylül 2026) -> YENI (origin/claude/soban-gamified-education-prb70m, 26 Kasım 2026)

Yalnızca git ref'lerinden ve commit iletilerinden okunarak çıkarıldı (27.11.2026). Hiçbir dosya çalıştırılmadı, hiçbir ekran görülmedi. "Doğrulanmadı" denen yerler satır satır diff okunmadan commit iletisine veya yorumlara dayanır.

## 1. Genel tablo

`git diff -M --stat ESKI YENI -- app/`: 765 dosya, +96.485 / -18.421. 554 commit. Eklenen 435, silinen 72, değişen 257, yeniden adlandırılan 1 (MissionsFragment -> MissionsSection).

| Alan | Dosya | Eklenen/Silinen satır |
|---|---|---|
| Kotlin (.kt) | 284 | +57.446 / -10.572 |
| XML (layout, drawable, values, anim) | 381 | +38.945 / -7.836 |
| mp3 (raw) | 42 | ikili |
| png / webp | 30 / 10 | ikili |

Kotlin sayısı: ana kaynakta 184 -> 331 dosya. Layout: 128 -> 194. Drawable: 331 -> 430.

Paket adı: `com.example.app` aynı; `applicationId = com.numigo.app` (versionCode 26, minSdk 24, targetSdk 36). Yeni alt paketler: `homework/` (13 dosya), `temel/` (1), `abacus/` 19 dosyaya çıktı, `auth/` 2.

En büyük değişen Kotlin dosyaları (+/-):
- TutorialFragment +6318/-3504; MainActivity +3007/-344; TasksFragment +1441/-162; GlobalLessonData +972/-674; UserInfoFragment +934/-65; LessonAdapter +861/-565; BlindingLessonFragment +571/-693; ShopFragment +458/-28; NotificationFragment +372/-52; BillingManager +350/-19; ProfileFragment +348/-161; MapFragment +317/-191; AbacusFragment +310/-146.
- Yeni büyük dosyalar: BunnyMascotView 5094, AnalyticsLogger 2162, MistakeReviewDialog 1267, StreakRepository 1135, HomeworkPlayerFragment 928, HomeworkEngine 732, HomeworkCreateFragment 621, HomeworkRuleStatsFragment 593, SobiChatFragment 577, WeeklyCardFragment 538, WeeklyCardRules 464, MistakeAnalyzer 492, LearningPathView 452.
- Layout: fragment_tutorial +1567/-761, fragment_abacus +1375/-690, fragment_rules +1372/-706 (kural paneli `view_rules_panel.xml`'e çıkarıldı), fragment_shop +676/-57, panel_cup_path +543/-?, fragment_blinding_lesson +148/-1021, fragment_user_info +350/-241.

Yeni özellik grupları (yeni dosya + layout, yaklaşık satır): Ödev/homework 6336; Maskot (BunnyMascot*) 5559; Abaküs ayar/kural/hata analizi 4912; Sobi 4221; Seri (streak) 3923; Sınıf/öğretmen 2544; Veli paneli 1766; Hata bildirimi 1684; Haftalık kart 1394; Kupa yolu ekranı 994; Avatar 814; Dil altyapısı 785; Davet/paylaşım 655.

## 2. iOS ekran klasörü başına değişiklikler

### Kabuk (büyük)
- Alt çubuk sekme eşlemesi değişti (MainActivity, bottom_menu.xml): `tasks` -> TasksFragment(MODE_MISSIONS) (görev listesi + günlük soru + Abaküs kartı); `explore` -> TasksFragment(MODE_CUP) (yalnızca Kupa Yolu kartı); `notification` -> öğrencide SobiChatFragment (Sobi sekmesi), onaylı öğretmende TeacherClassesFragment, eski NotificationFragment yalnızca TeacherQuestionsFeature açılırsa (şu an `ENABLED = false`). `shop` -> AbacusCustomizationFragment (aynı). Dayanak: MainActivity.kt satır ~1028-1048; commit 41e5f98.
- Alt çubuk görünümü: renkli ikonlar (home_ic1, missions_ic3, shop_ic1, chat_pair_ic, explore_ic1), seçili sekmede çerçeveli kutu (bottom_nav_item_bg, #49C0F8 çizgi) ve zıplama; profil sekmesi kullanıcının avatarı (7927804).
- Üst panel: seri (streakContainer: alev + sayı), enerji simgesi Pro'da "∞" rozeti, Sobi kredisi (creditIcon/creditText), onay bekleyen öğretmen başlığı; mağaza `shopOverlayContainer` ile ders/öğretici üstünde açılıyor (680bfdb, 91b2312).
- Ders sonrası ekranlar tek öncelik kuyruğunda (7915936); tüm ders ekranları sağdan girip SOLA çıkıyor, 400 ms (c24d10c, a5b67a4); `postLessonBackdrop` zemini; seri kutlama şeridi (`streakCelebration`).
- Yeni açılış: core-splashscreen, marka (Sobi ikonu) + "Sorobit" yazısı, en az 1200 ms (10.11 notu).
- Zorunlu güncelleme (ForceUpdateManager, Remote Config `min_version_code`), yaş/ebeveyn kapısı, tablet yoğunluk büyütme, yazı boyutu sınırı (FontScaleLimiter).

### Harita (büyük)
- Ders/sandık daireleri ve zikzak ofset (DynamicOffsetDecoration, item_lesson.xml) kaldırıldı: alt alta ders kartları (item_lesson_card.xml, SegmentBarView). Durumlar: kilitli gri+kilit, açık mavi+beyaz kenar+ok, bitmiş TÜM KART ALTIN; sandık = 3 yıldız (7927804, doc "Harita: ders/sandık daireleri yerine alt alta kartlar").
- Alttan açılan lesson_bottom_sheet silindi; yerine karta bağlı `lesson_popover.xml` + PanelPointerView (ok), karartma üst paneli ve alt menüyü de örtüyor; düğmeler Başla / Devam et / Gözden geçir / Tekrar dene / Kilitli.
- Sabit bölüm başlığı (StickyLinear) kaldırıldı; soru düğmesi sağ alta (ama `TeacherQuestionsFeature.ENABLED=false` iken tüm soru düğmeleri gizli).
- Kilit kuralları: bölüm 2-6 kartları önceki bölümün SON ÜNİTE MARATONU bitince açılır; kilitli ders/maraton 1 anahtarla açılır (LessonAdapter.unlockLessonWithKey); uyarı Toast yerine Snackbar/LockedHint (PartSelectionFragment, efa5be2, 2a6f0c8, de77ab2).
- Yeni: Sobi günlük test düğmesi (MapSobiTestButton, Part 1'in 6. dersi bitince; öğretmende gizli), Part seçiminde de aynı düğme; ders şablonu 1-6'da yalnızca ilk ders açık (06d4f05); onaylı öğretmende tüm dersler açık.
- Veri: ders kimliği artık `stableId` (bkz. bölüm 6).

### Abakus (büyük)
- Boncuk animasyonu süresi tüm ders türlerinde aynı (eskiden sandıkta 50 ms).
- Yanlış cevap: "Sobi ile analiz et" düğmesi (incorrectSobiAnalyze), MistakeReviewDialog; çocuğun nerede yanıldığı kural motoruyla (MistakeAnalyzer, StepExplainer); cevaptan sonra abaküssüz kutu gizlenir.
- Sağ düğme sütunu (sıfırla, kural kitabı, kural paneli, atla, abaküs ayarı; LessonSideButtons, DepthButtons), sağdan kayan abaküs ayar paneli (AbacusSettingsPanel, view_abacus_settings_panel.xml; abaküs/boncuk eni-boyu), abaküs sığdırma (AbacusSideButtonFit).
- İpucu: ampulden çıkan konuşma balonu (ec3f934); rehberlerde öğretmen PNG'leri yerine Sobi maskotu (guideMascot); rehber sesleri (guide*.mp3) silindi.
- Sayı kartı tüm abaküs ekranlarında; "göçen" (derinlik) düğmeler; kural paneli ayrı layout (view_rules_panel).
- Ders sonu anketi kaldırıldı (27565db); doğrudan sonuç ekranı. Hesapsız ilk derste çıkış düğmesi gizli.
- Kural sayımı/Sobi kural hafızası (SobiRuleStats), haftalık kart için "çözülen işlem" kaydı.

### KorDers (orta-büyük)
- Abakus ile aynı: rules panel include'a çıktı (fragment_blinding_lesson -1021 satır), analiz ekranı, yan düğmeler, göçen Kontrol düğmesi, kupa modunda analiz, Sobi testinde zihinden (m_) sorular, sandık dersinde süre analiz sırasında durur. Doğrulanmadı: ekran görüntüsüyle karşılaştırma yapılmadı.

### Sonuc (orta)
- LessonResult/ChestResult: 12 rastgele Lottie (animation_one...thirteen) KALDIRILDI; yerine BunnyMascotView `mascotView` ders sonu Sobi sahnesi (LessonEndMascot: 9 başarı sahnesi torbadan: gitar, hokkabaz, mükemmel, kovboy, karate, ninja, korsan, gözlük, rapçi; başarısızlıkta hep "kararlı"). Başlık/kutular düğmenin üstünde, paddingTop 80->48dp.
- Devam düğmesi bg_depth_result_continue (göçen), metinler strings'e. Çıkış sola kayar, 400 ms.
- Sandık ödül ekranı: NewChestFragment kupa yolu sandığı kipi (`claimCupPathChest`), Android 15 şerit rengi.

### Sandik (orta)
- Kristal video akışı yok (CrystalBreakVideoFragment, 8 crystal_*.mp4 ve crystal_click1.mp3 silindi; 31b7773/43e65a5). NewChestFragment tek sandık ekranı; sunucu sonucu gelene kadar spinner; hata olursa Toast + kapanış.
- Sandık kaynağı (ders/görev/mağaza/kupa yolu) ölçümü; 4. ve 5. bölümde sandık bitince sonraki ders başlığa bakarak açılır (6bb4b66). Nadirlik etiketleri strings'e.

### Gorevler (büyük)
- ESKI MissionsFragment silindi -> MissionsSection (satır olarak TasksFragment içinde). YENI Görevler sekmesi sırası: Missions listesi, günlük soru kartı, Abaküs kartı, Hata Avcısı kartı (bugReportCard), admin kartı (yalnız admin), Karakter animasyonu (yalnız onaylı öğretmen). "Bize Ulaşın", "Sandık Animasyonu", "Test Ad Skip" kartları KALDIRILDI.
- Meydan okuma ödülü 3/5/7 gün -> 500/1000/1500 altın (8d13cce).

### GunlukSoru (küçük)
- Kart ve havuz mantığı aynı; yeni: zorluk ve ünite adı dilde gösterilir (Kolay/Orta/Zor kimlik olarak Türkçe kalır), kart periyot yenilemesi. DailyQuestionRewardFragment SİLİNDİ (ESKI'de de hiç açılmıyordu, 4aba058): iOS `GunlukSoruOdulEkrani` Android karşılığı olmayan ölü ekran adayı.
- Görev ödülü artık kristal videosuz (MissionRewardRevealDialogFragment doğrudan ödülü gösterir).

### Kupa (büyük)
- Kupa Yolu: 6 kupa yolu kartının her birinde ilerleme çubuğu + sandık (card1..6Reward*), yeni CupPathRoadFragment (aşağıdan yukarı kupa yolu ekranı, eşikler 300 başlangıç, 100'er adım, 10000'de biter; 1000 katı DESTANSI, 500 katı ENDER, diğer SIRADAN), CupPathRewardRepository. Sandık `claimCupPathChest` ile açılır.
- Kupa puanını istemci yazamaz: `submitCupResult` (yalnızca kazanma/zorluk/mod gönderilir, miktar sunucuda; tur başına bir gönderim). Haftalık kart için CupRuleEngine.configsFor/multiplicationDigitsFor eklendi.
- Rozet: Golf eşikleri [5,10,20,50,100] -> [3,5,10,15,20]; eşikler tek kaynakta (BadgeProgressEngine.levelSpec); rozet adları strings'e; Kupa lider tablosunda Google fotoğrafı yerine uygulama avatarı (5dcc126). Lider tablosu sahte oyuncu tohumu (sunucu).
- Doğrulanmadı: CupFragment/BadgeFragment layout farkları (yalnızca küçük, 4-5 satır).

### Ozellestirme (küçük)
- AbacusCustomizationFragment +151/-60: Pro/öğretmen kilit mantığı, bakiye canlı güncelleme, anahtarla satılan boncuk rengi 40 anahtar, harcama ölçümü; layout farkı çok küçük. Doğrulanmadı: ayrıntılı davranış farkı.

### Magaza (büyük)
- fragment_shop +676: üst enerji/kredi çubuğu (shopEnergyIcon, shopCreditText), "Süper" kartı, Davet kartı (shopInviteCard), Seri dondurma kartı (4000 altın, shopBuyStreakFreezeButton), Pro/Lite kartları, kredi paketleri (Sobi dışı: ₺39/179/329), YENİ Sobi kredisi bölümü (20/50/120 kredi; ₺12,99/24,99/49,99), anahtarla can alımına günlük 5 sınır (f289beb).
- Pro karşılaştırma/PlanFragment: kapatma tuşu, davet bağlantısı, maskot; reklam atlama (AdSkipFragment) paywall_navy zemin; BillingManager: token sahipliği, iade; yeni ürün kimlikleri `sobi_credits_20/50/120`.
- Reklamlar: geçiş reklamı kuralları, ödüllü reklam birimi, UMP gizlilik seçenekleri.

### Giris (büyük)
- LoginStart yeniden tasarlandı: sağ üstte öğretmen/öğrenci geçişi (btnTeacherMode), tavşan maskot, başlık "Zihnini Güçlendir", gövde metni, "Başla" ve "Zaten hesabım var" (ESKI: "Hesabın mı var?" + GİRİŞ YAP/BAŞLA). Localizable'daki 6 anahtar bundan değişti.
- Kayıt soruları (UserInfoFragment) tam ekran ve kayarak geçiş, animasyonlu ilerleme çubuğu; sırası: Sobi'yle tanışma (2 ekran) -> yaş -> "Bizi nereden duydun?" (alt alta satırlar, Okul/Öğretmen) -> günlük hedef -> meydan okuma (3/5/7 gün) -> ilerleme yolu ekranı (LearningPathView) -> hatırlatma saati (hazır saatler + "Başka bir saat") ve bildirim izni.
- 13 yaş altı: kayıt kodu veliye gider, veli onayı (ParentConsentFragment; `requestParentalConsent`, `confirmParentalConsent`, `verifyRegistrationCode` yeni alanlar birthYear, parentEmail, parentalStep); çocuğun kendi e-postası da doğrulanır.
- Google ile kayıt seri hunisine katılıyor; öğretmen girişi/kaydı ekranları yenilendi; App Links (davet bağlantısı uygulamayı açar); zorunlu güncelleme.
- Seri kurulumu: ilk ders bitince, kayıttan ÖNCE (hedef dakika, meydan okuma gün, onay) (313c507).

### Profil (büyük)
- Avatar: 12'li AvatarPickerFragment ve avatar_ic1-12 SİLİNDİ; yerine Personas (Draftbit, CC BY 4.0) parçalı özelleştirilebilir avatar (AvatarCustomFragment, AvatarView, AvatarArt, `users/{uid}.avatarConfig`, assets/avatar/personas_parts.json, AndroidSVG). `selectedAvatar` alanı çıktı.
- Profil üstü avatarın arka plan renginde sabit panel; durum çubuğu boyanır; kaydırınca çizgi; renkli alana dokunmak avatar düzenlemeyi açar; en uzun günlük seri; başkasının profilinde geri düğmeli üst bar.
- Ayarlar (AccountSettings +298): Dil (tr/en/id/ar), AB kullanıcısı için reklam gizlilik seçenekleri, Kurs/Sınıfım kartı (StudentClassFeature.ENABLED=true), Veli paneli, Davet, Kaynaklar ve lisanslar (CreditsFragment), bildirim tercihleri (tür bazlı, sessiz saatler; NotificationPrefs). Geri bildirim ("Bize Ulaşın") hesap ayarlarında.
- Yeni ekranlar profilden: StreakFragment, haftalık kart (WeeklyCardFragment), ProgressHubFragment, takipçi listeleri avatarı publicProfiles'tan çizer.

### Yonetim (iOS'a özgü; Android karşılığı MainActivity)
- iOS UygulamaYoneticisi/YonetimDersAkisi, Android'in "ders sonrası kuyruğu" mantığının karşılığı. Kuyruk değişti: bkz. Kabuk. Ders sonrası zincir sırası (sonuç -> sandık -> rozet -> seri sorusu -> reklam) yeniden okunmalı (MainActivity prepare/finalizeMapReturnAfterLessonClaim, `isLessonTypeReturn`).

## 3. YENI'de olup iOS'ta karşılığı olmayanlar

| Özellik | Ana dosyalar | ~Satır | Görüş |
|---|---|---|---|
| Sobi sohbet sekmesi + analiz (yapay zekâ yardımcı) | SobiChatFragment, MistakeReviewDialog, SobiContext, SobiSpeaker, ServerSobi, StepExplainer/SubStepExplainer, MistakeAnalyzer, sunucu `functions/sobi/` | ~4200 + 1267 + 1000 | ÇEKİRDEK (ürün yönü 01.11: odak Sobi). Öğrenci için alt çubuk sekmesi. Sunucu Anthropic anahtarına bağlı. Kural motoru kısmı (yapay zekâsız) Kotlin ortak koda alınabilir. |
| Sobi testi (günlük 10 soru, 20 seviye) | SobiDailyTest, SobiTestPlanner, SobiTestSheet, MapSobiTestButton, SobiTestLevels | ~1200 | Çekirdek-yakın (haritada düğme). |
| Sobi ekonomisi (günlük ücretsiz mesaj, kredi paketleri) | wallet.js, ShopFragment bölümü, BillingManager | ~600 | Mağaza ile birlikte; StoreKit ürün tanımı gerekir. |
| Seri (streak) | StreakRepository 1135, StreakFragment, NewStreakFragment, StreakViews, StudyTimeTracker, StreakSyncService, TrustedClock | ~3900 | ÇEKİRDEK (üst çubukta, ders sonrası akışta, kayıt akışında). Sunucu: submitStreakDay, claimStreakReward, buyStreakFreeze. |
| Haftalık ilerleme kartı + paylaşım | WeeklyCardFragment, WeeklyCardRules, LessonMilestones, snapshotWeeklyStats | ~1400 | Yan özellik; büyüme/paylaşım için. |
| Veli paneli + veli onayı | ParentPanelFragment, ParentReport*, ParentConsentFragment, ParentGate | ~1800 | Kayıtta veli onayı çekirdek (13 yaş altı; App Store çocuk kuralı); panel yan. |
| Ödev (homework) + sınıf/kurs/öğretmen | homework/*, Teacher*, StudentClass*, CourseClasses | ~6300 + ~2500 | Yan özellik, ürün kararı belirsiz (kurs satışı için açık, 21.11). Önce atla. |
| Hata Avcısı (hata bildirimi + admin) | BugReports, BugReportFragment, ...Admin | ~1700 | Yan; admin kısmı gereksiz. |
| Davet/referral, başarı anı paylaşımı | InviteFragment, ReferralAttribution, ShareMoments | ~650 | Yan; Universal Links gerekir. |
| Avatar (Personas) | AvatarArt/Config/View/CustomFragment | ~800 | Profil için orta (12'li avatarın yerini alıyor). SVG çizimi iOS'ta farklı yazılmalı. |
| BunnyMascotView (Sobi çizimi, ~30 hâl) | BunnyMascotView 5094, BunnyMascotArt | ~5300 | Çekirdek görsel kimlik (giriş, ders sonu, rehber). Canvas çizimi; SwiftUI/Lottie'ye taşımak büyük iş; önce statik hâller. |
| Bildirimler (FCM, tür bazlı) | MyFirebaseMessagingService, NotificationPrefs, notifications.js | ~800 | Yan; APNs gerekir. |
| Kupa Yolu ekranı | CupPathRoadFragment, CupPathRewardRepository | ~670 | Çekirdek (Kupa sekmesinin tek kartı). |
| Dil (en/id/ar), RTL | AppLanguage, AppStrings, RtlSupport, values-en/in/ar | ~800 + 3400 anahtar x3 | Pazar planı için çekirdek; iOS'ta Localizable.strings dönüşümü gerekir. |
| Zorunlu güncelleme, App Check, TrustedClock, FontScaleLimiter | ForceUpdateManager vb. | ~500 | Yan/altyapı. |
| Öğretici (TutorialFragment) | 17 bin satır, 16+ öğretici, sesler | ~17000 | iOS'ta zaten yazılmamıştı; çekirdek akış, en büyük tek iş. YENI'ye göre yazılmalı. |

## 4. ESKI'de olup YENI'de kaldırılanlar (iOS'ta karşılığı yazılmış olabilir)

- 12'li avatar seçici: iOS `Profil/ProfilAbakusVeAvatar.swift` (ProfilAvatarSecici), `avatar_ic1-12` asset'leri, `selectedAvatar` alanı. KALDIRILDI.
- Kristal kırılma videosu: iOS `Sandik/SandikKristalEkrani.swift`, `SandikVideoOynatici.swift`, `KorDers`/ortak `CrystalBreakLogic`. ESKI'de bile mp4'ler silinmişti, YENI'de akış yok.
- Günlük soru ödül ekranı: iOS `GunlukSoru/GunlukSoruOdulEkrani.swift` (Android DailyQuestionRewardFragment silindi).
- Harita dairesel ders düğmeleri, zikzak ofset, sabit başlık, alt panel (iOS HaritaDersAltPaneli, HaritaDersListesi sticky header).
- Ders sonu 12 rastgele Lottie (animation_one...thirteen, chating_anim): iOS `Lottie/` içindeki 12 dosya artık kullanılmıyor. YENI'de assets'te `animaton_thirteen.json` yine var (ama sonuç ekranında kullanılmıyor, doğrulanmadı).
- Öğretmen PNG'leri (teacher_emotes_gpt3/gpt4/stick): iOS rehberleri bunları kullanıyor, Android Sobi'ye geçti. guide*.mp3 ve tutorial1_112.mp3 silindi.
- Eski sekme eşlemesi: MissionsFragment, ESKI Keşfet kartları (Bize Ulaşın, Sandık Animasyonu, Test Ad Skip), Görevler "Avatar" test kartı.
- Ders sonu anketi (QuestionPanelFragment akışı ders sonunda), `lesson_card_shine_*`, `record_background`, GalleryBottomSheetFragment, FirstTutorialShownStore, ic_launcher_foreground.xml, mipmap .webp'ler.
- Tüm "öğretmene sor" düğmeleri ve bildirim sekmesi öğrencide gizli (TeacherQuestionsFeature.ENABLED=false). iOS bildirim/öğretmene soru yazılmadı; yazılmamalı.

## 5. Kaynaklar

- Drawable: +118 eklendi, -19 silindi, 10 değişti. Raw: +7 (tutorial16_1000-1006.mp3), -17 (guide*, crystal_click1, tutorial1_112), 19 değişti (tutorial*.mp3, abacus_sound1.wav 16 bit). Yeni anim XML: 6 (hint_bubble_*, queue_screen_in/out...), animator: lesson_card_press.
- colors.xml: 20 anahtar eklendi, 0 değişti, 0 silindi (bottom_nav_selected_stroke, lesson_card_*, lesson_panel_*, side_button_background, parent_*, paywall_navy). dimens.xml +69/-21, styles +66, themes +33 (ayrıntı doğrulanmadı).
- strings: ESKI 101 anahtar (values/strings.xml; arayüz metinleri koda gömülüydü). YENI values/ 20 dosyada 3551 anahtar (3452 eklendi, 4 değişti, 2 silindi); strings_*.xml bölünmüş dosyalar. iOS `tr.lproj/Localizable.strings` 101 anahtar, hepsi ESKI'de vardı; YENI'de 6'sı etkilenir: değişen `login_start_student_title`, `login_start_student_subtitle`, `login_start_teacher_title`, `login_start_teacher_subtitle`; silinen `login_start_primary_button`, `login_start_secondary_button`. Not: `ios/tools/metin-donustur` yalnızca `values/` okuyor mu kontrol edilmedi; YENI'de metinler strings_*.xml dosyalarına bölündü, betik buna göre güncellenmeli. iOS Swift kodunda sabit Türkçe metinler (yaklaşık 86 `Text("...")`) Android'de artık strings'te.
- Diller: values-en 3399, values-in 3400, values-ar 3400 anahtar (13'er dosya). ESKI'de yoktu. Arapça için ayrı font (`res/font-ar/font_regular.ttf`, Baloo Bhaijaan 2); Latin font `font/font_regular.ttf` değişmedi (Baloo 2; Credits'te atıf).
- Lottie/assets: 11 numaralı animasyon + chating_anim assets'ten silindi; assets/avatar/personas_parts.json eklendi; res/raw'a lesson_hint_ic.json, reset.json. crystal_*.mp4: ESKI'de de YENI'de de YOK (video yok).
- Uygulama simgesi: YENİ'de gerçek simge var: özgün mavi tavşan (21bfbb3), `ic_launcher-playstore.png` (512), mipmap-*/ic_launcher*.png, adaptive (mipmap-anydpi-v26), monochrome. iOS AppIcon.appiconset'te görüntü yok (yalnızca Contents.json); Android'in playstore png'si kaynak olabilir (lisans: ikon "özgün", Freepik kafası çıkarıldı). Açılış ekranı için `drawable-xxxhdpi/splash_icon.png`.
- Lisans: CreditsFragment; Freepik Sobi çizimi maskot için atıf notu var, Personas CC BY 4.0, Noto Emoji Lottie CC BY 4.0.

## 6. functions/ ve Firestore sözleşme değişiklikleri (yalnızca başlık)

- functions/index.js dışa aktarımları 32 -> 121 (alt çizgisiz yeni çağrılabilir/tetikleyiciler: askTeacherQuestion, buyStreakFreeze, checkWelcomeCreditEligibility, claimBugReportReward, claimCupPathChest, claimReferral, claimStreakReward, confirmParentalConsent, requestParentalConsent, createClass, createHomework, deleteClass, deleteHomework, getReferralCode, getSeasonInfo, inviteStudentToClass, joinClassByCode, leaveMyClass, notifyTeacherPool, recordNotificationOpen, recordRuleTally, recordSobiLevels, regenerateClassCode, removeStudentFromClass, respondClassInvite, reviewBugReport, sobiBalance, sobiChat, sobiFeedback, sobiSessions, startSobiExample, startSobiExtraTest, submitBugReport, submitCupResult, submitStreakDay, syncTeacherClaim, snapshotWeeklyStats, send*Notices zamanlayıcıları vb.). ESKI'deki hiçbir dışa aktarım silinmedi. Mevcut çağrılabilirlerin gövde/alan değişimi (ör. verifyRegistrationCode yeni alanlar: birthYear, parentEmail, parentalStep, lang) KISMEN doğrulandı; openChest/spendEnergy imzaları diff'le doğrulanmadı.
- Kupa puanı yazımı istemciye KAPALI: `submitCupResult` (iOS shared `CupRepositories` skor yazımı kopyası artık geçersiz). Yeni belge: `cupPathRewards/progress`, `max_*` zirve alanları.
- Ders kimliği: `users/{uid}/lessonProgress/{partId}/items/{stableId}` ve `lessonSuccessRateState/...` anahtarları sıra numarasından `stableId` (`p{part}_i{index}_{başlık}`) oldu; eski sayısal anahtar okunup taşınır (7d4ae90). iOS shared LessonProgressMerge ve LessonSuccessRateRepository etkilenir. LessonItem'a `stableId` zorunlu alan.
- badgeProgress/state: madalya parçaları ve sayaçlar istemci yazımına kapalı (rules, 1796b01, 14cad79).
- users/{uid}: `avatarConfig` (selectedAvatar çıktı), `planPurchaseTokenHash`, `giftProUntil/giftProSource`, `teacherLessonsUnlocked`, `reminderLocalHour`, seri alanları (sunucu yazar). publicProfiles: avatarConfig, longestStreak.
- Yeni koleksiyonlar/kurallar: bugReports, classes (+members, studentProgress, months), classInvites, classJoinCodes, classJoinGuards, courses, homeworks, homeworkRuleStats, parentalConsents, questionPanel(+Tutorial), successRate/dailyQuestionSuccessRate, weekly snapshot, referrals, launchGift. Sobi: sunucu tarafı oturum/cüzdan (`functions/sobi/*`). storage.rules değişti (+55).
- Dil: sunucu `lang` alanı (tr/en/id/ar) e-posta ve hata metinlerinde.

## 7. Önerilen eşitleme sırası ve iş paketleri

0. Ön koşul (bağımsız, ilk): `ios/shared` Kotlin kopyasını YENI'ye göre güncelle. En çok etkilenenler: GlobalLessonData + stableId (LessonItem, LessonProgressMerge), LessonSuccessRateRepository, Cup repositories (sunucu gönderimi), CupRuleEngine, BadgeProgressEngine (Golf eşikleri), UserWalletFirestore, EnergyManager, ServerRewards (claimCupPathChest), AdManager kuralları, AgeConsentManager, BillingCatalog, SeasonClock, DailyQuestionPoolBuilder, AbacusPreferences. Kataloğu `tools/ders-katalog/uret.js` ile yeniden üret; `tools/metin-donustur`'u strings_*.xml bölünmüşlüğüne göre güncelle. Öncelik: bu paket olmadan ekran eşitlemesi yanlış veri anahtarına yazar.
1. Paket A (birlikte): Kabuk + Harita + Sonuc + Sandik. Sekme eşlemesi, üst panel, kart tabanlı harita ve popover panel, ders sonrası kuyruk, Sobi ders sonu sahnesi (önce statik görsel). Ölü ekranları sil: avatar seçici, kristal video, günlük soru ödülü.
2. Paket B (birlikte): Abakus + KorDers. Ortak: yan düğmeler, ayar paneli, kural paneli, sayı kartı, boncuk süresi eşitliği, ipucu balonu. Sobi analiz düğmesi ayrı iş (Paket E'ye bağlı).
3. Paket C (bağımsız): Gorevler + GunlukSoru + Kupa (Kupa Yolu ekranı, sandıklı kartlar, rozet eşikleri, sunucudan kupa gönderimi).
4. Paket D (bağımsız): Giris (yeni başlangıç ekranı, Sobi'li kayıt soruları, ilerleme yolu, hatırlatma saati, veli onayı) + Profil (avatar sistemi, ayarlar, dil). Seri kurulumu Paket F ile ortak.
5. Paket E: Sobi (sohbet, analiz, test, kredi) + Magaza (Sobi kredisi, seri dondurma, Pro, davet). Sunucu anahtarı ve StoreKit ürünleri önce.
6. Paket F: Seri sistemi (üst çubuk, ders sonrası, kutlama).
7. En son / ayrı karar: Öğretici (en büyük), Dil (en/id/ar), Haftalık kart, Veli paneli, Davet, Hata Avcısı, ödev/sınıf, bildirimler.

## Bakılamayan / doğrulanmayan alanlar

- Hiçbir ekranın layout'u piksel düzeyinde karşılaştırılmadı; "değişti" hükümleri commit iletileri, yorumlar ve diff içindeki id/metin satırlarına dayanır.
- TutorialFragment, BlindingLessonFragment, ShopFragment, ProfileFragment ve UserInfoFragment gövdeleri satır satır okunmadı.
- functions/ içindeki mevcut çağrılabilirlerin davranış/alan farkları yalnızca birkaçı için incelendi; firestore.rules yalnızca eklenen `match` başlıkları ve commit iletileriyle.
- values/dimens.xml, styles.xml, themes.xml farklarının içeriği okunmadı.
- iOS `ios/shared` içindeki hangi dosyanın hangi Android değişikliğinden etkilendiği ad eşlemesiyle tahmin edildi (README tablosu ve dosya başlıkları).
- Çalışma ağacındaki `app/` kullanılmadı; her şey ref'lerden okundu.

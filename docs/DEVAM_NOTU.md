# Devir Notu — bilgisayardaki oturum için

Bu not, bulutta çalışan bir oturumdan devralan ve **derleyebilen** bir oturum için yazıldı.
Buluttaki oturumda Android SDK yoktu; Kotlin hiç derlenemedi, her değişiklik yalnızca mantık
olarak doğrulandı. İlk kazanç bu: artık `.\gradlew compileDebugKotlin` çalıştırılabiliyor.

- Dal: `claude/soban-gamified-education-prb70m`
- İletişim: **Türkçe**
- Proje: numi_goo — Türk çocukları (~7–10 yaş) için oyunlaştırılmış soroban (abaküs) eğitimi
- `applicationId = com.numigo.app`, namespace `com.example.app`
- Geliştirici tek kişi, Windows + Android Studio, gerçek cihaz/emülatörde elle test ediyor

## Çalışma anlaşmaları

- **Riskli yerlere dokunmadan önce bildir.** Ders sonrası akışı (`prepareMapReturnAfterLessonClaim`,
  `finalizeMapReturnAfterLessonClaim`, ders sonu ekran zinciri ve animasyonları) uzun uğraşla
  stabilize edildi; oraya dokunan bir değişiklik önce anlatılıp onaylanmalı.
- Çalışan bir akışı bozmamak, yeni bir hatayı düzeltmekten önce gelir.
- Kodun yorum yoğunluğu yüksek ve yorumlar **nedeni** anlatıyor ("niye böyle yapıldı",
  "eskiden ne bozuluyordu"). Aynı üslubu koru; yalnızca ne yaptığını söyleyen yorum eklemeyin.
- **Derlemek kurmak değildir.** `.\gradlew compileDebugKotlin` cihaza hiçbir şey göndermez; cihazda
  sınanacak her değişiklikten sonra `.\gradlew installDebug` çalıştırılmalı. Emin olmak için:
  `adb shell dumpsys package com.numigo.app | Select-String lastUpdateTime`. Bir tur, eski
  sürüm test edilip "düzeltme işe yaramadı" sanıldığı için kaybedildi.

## Sıradaki iş 1 — güvenlik ağı sandık ekrandayken kilidi açıyor (teşhis tamam, düzeltme yapılmadı)

**Belirti:** Görünür bir belirti gözlenmedi (sandık tam ekran ve alt barı örtüyor), ama sandık
ekrandayken chrome kilidi teknik olarak açık. Asıl sorun şu: kilit sayacını fragment'ların
`acquire`/`release` çiftleri değil, bu güvenlik ağı yönetiyor.

**Kanıt** (`ChromeBlockerDbg`, 01.10.2026 20:58 — tek bir ders bitişi):

```
[ChestFragment.onViewCreated] acquire → depth=2
[LessonResult.onDestroyView] release → depth=1
[ensureUnlocked] force applyUnlock (depth was 1, release eksik kalmış)   ← sandık hâlâ ekranda
[MissionChestRewardFragment.onViewCreated] acquire → depth=2
[ChestFragment.onDestroyView] release → depth=1
[ensureUnlocked] force applyUnlock (depth was 1, release eksik kalmış)   ← görev ödülü hâlâ ekranda
[MapFragment.disableMainActivityViews] acquire → depth=1
[ensureUnlocked] force applyUnlock (depth was 1, release eksik kalmış)   ← harita kilidi 1 ms sonra siliniyor
```

**Kök neden:** `MainActivity.ensureChromeUnlockedAfterMapReturn` (3205) "bloklayan overlay hâlâ
aktif mi" kararını yalnızca `abacusFragmentContainer`'a bakarak veriyor. LessonResult →
ChestFragment geçişinden sonra canlı overlay `resultFragmentContainer`'da duruyor; abacus kabı
boş olduğu için `blockingOverlayStillActive=false` çıkıyor ve sayaç zorla 0'a çekiliyor. Aşağıda
düzeltilen back stack dinleyicisiyle aynı türden eksik: `3722b05` öncesinde hayalet fragment bu
kontrolü tesadüfen geçiriyordu.

**Önerilen düzeltme** (uygulanmadı — ders sonu akışına dokunuyor, önce onay alınmalı):

1. `blockingOverlay` hesabına `resultFragmentContainer` da girsin: host `VISIBLE` ve
   `liveOverlayIn(R.id.resultFragmentContainer)` `fragmentBlocksSeasonLeaderboardGate`'i
   sağlıyorsa bloklayan overlay var sayılsın (`reconcileAbacusOverlayWhenMapIsBase`'in başındaki
   kontrolle aynı koşul).
2. **Dikkat:** güvenlik ağı bugüne kadar eksik kalan release'leri örtüyor olabilir. Düzeltmeden
   sonra sayaç gerçek `acquire`/`release` çiftleriyle yönetilecek; bir ders bitirip haritaya
   dönüldüğünde `depth=0` olduğu ve `release ignored` satırlarının kaybolduğu logdan
   doğrulanmalı. `depth>0` kalırsa gerçek bir eksik release ortaya çıkmış demektir — onu
   bulup düzeltmek gerekir, güvenlik ağını geri gevşetmek değil.
3. Haritanın soru promosu / rehber için aldığı kilidin (`MapFragment.disableMainActivityViews`)
   neden hemen silindiği ayrıca incelenmeli: o anda hiçbir overlay yok, yani madde 1 onu
   düzeltmez. `ensureUnlockedForMapReturn` haritanın kendi kilidinden habersiz.

**Not — eski "chrome kilidi asimetrisi" işi:** `MapFragment`'teki çapraz eşleşme düzeltildi
(bkz. Yakında yapılanlar), ama o teşhis doğrulanmadı. Haritanın kilidi çift alıp sayacı 2'de
bıraktığı durum cihazdaki hiçbir koşuda oluşmadı; görülen `depth=2` her seferinde
`LessonResult` + `ChestFragment` idi.

## Maraton rehberi açıkken ekran tıklanabiliyordu (02.10.2026 — düzeltildi, cihazda doğrulandı)

**Belirti:** 1. bölümün ilk sandığından sonra açılan rehber paneli (`MapFragment.showGuidePanel`)
ekrandayken alt çubuğa, para paneline ve haritanın üst şeridine dokunulabiliyordu. Kullanıcı
rehber açıkken alt çubuktan Görevler sekmesine geçebildi.

**Kanıt** (`PostLessonQueue`, 18:14–18:15):

```
18:14:59.973 rehber | caller=RatingDialog.dismiss          ← kuyruk rehber adımını açtı
18:15:00.057 tur | caller=watchdog ... rehber=false        ← rehber "gösterildi" işaretlendi
18:15:00.057 kilit birakildi | caller=watchdog             ← 84 ms sonra kilit bırakıldı
18:15:00.070 bekliyor | caller=watchdog block=guide_panel_visible
18:15:01.897 StudyTime.ekran | ekran=MissionsFragment      ← kullanıcı sekme değiştirebildi
```

**Kök neden — 1 Ekim değil, 24 Eylül (`b13e3d0`).** Kullanıcı 1 Ekim'deki kilit
düzeltmesinden şüphelendi; değil. `b13e3d0` kuyruğun kilit bırakmasını guard'lı
`enableMapTouchRouting`'ten (rehber açıkken reddediyordu) guard'sız
`releasePostLessonQueueTouchLock` → `forceEnableMapTouchRouting`'e çevirdi. Gerekçesi
"ChromeBlocker sayıcılı, başkasının acquire'ı duruyor"du; bu fragment'lar ARASINDA doğru ama
harita içindeki kilit tek ve paylaşılan bir kilit: `forceEnableMapTouchRouting` şeffaf
katmanı kaldırıyor ve alt çubuğu DOĞRUDAN açıyor, sayaca bakmadan. Kuyruk, rehberi gösterdiği
an boşaldığı için (`MarathonGuideStore.markShown`) bir sonraki turunda — bekçi yüzünden en geç
1 sn içinde — kilidi bırakıyor. 1 Ekim öncesi kod (`be6cb48`) aynı yolda aynı şeyi yapıyordu
(çift release + doğrudan açma); bu kod okunarak görüldü, eski sürüm cihazda denenmedi.

**Düzeltme:** `MapFragment.releasePostLessonQueueTouchLock` rehber paneli görünürken kilidi
açmıyor, rehbere devrediyor (logda `GuideDebug: releasePostLessonQueueTouchLock SKIP`). Panel
kapanırken kendi dinleyicisi koşulsuz bırakıyor, yani `b13e3d0`'ın çözdüğü "bırakmayı reddedip
bir daha kimsenin bırakmaması" sorunu geri gelmiyor. 1 Ekim'de yazılan hiçbir satıra
dokunulmadı.

**Aynı türden başka bozulma arandı** (haritanın kilidini kullanan bütün çağrılar tarandı):
- 1 Ekim değişikliği: bozduğu başka bir senaryo bulunmadı. Kilidi açan bütün yollar
  `enableMainActivityViews`'tan geçiyor; yalnızca `enableMapFragmentViews`'a güvenen yol yok.
- Öğretmene sorma tanıtımı: kuyruk aynı şekilde kilidi tanıtım penceresi gelmeden bırakabiliyor,
  ama aradaki süre bir iki kare (tanıtım aynı turda açılıyor). Dokunulmadı.
- **Açık kalan:** rehber açıkken uygulama arka plana alınıp geri gelinirse
  `MapFragment.onResume` → `sanitizeMapTouchSurface` → `ensureChromeUnlockedAfterMapReturn`
  chrome sayacını zorla sıfırlıyor ve alt çubuk yine açılıyor (şeffaf katman kalıyor). Bu,
  aşağıdaki "Sıradaki iş 1"in 3. maddesiyle aynı kök: güvenlik ağı haritanın kendi kilidinden
  habersiz. Kullanıcıya soruldu, "sorun değil" dedi; DÜZELTİLMEDİ. İleride istenirse önerilen:
  güvenlik ağı rehber paneli görünürken "bloklayan overlay var" saysın.

**Cihazda doğrulandı (18:41–18:45, yeni hesap, kullanıcı "tıklayamadım" dedi):**

```
18:41:59.701 markShown
18:42:00.376 kilit birakildi | caller=watchdog
18:42:00.376 GuideDebug: releasePostLessonQueueTouchLock SKIP ...      ← kilit rehberde kaldı
          (10,4 sn boyunca ekran değişimi ve kilit hareketi yok, sayaç depth=1)
18:42:10.810 enableMainActivityViews releasing ChromeBlocker → depth=0  ← "Rekor" hedefine basıldı
18:42:10.871 StudyTime.ekran | ekran=RecordFragment
```

Ardından 1 Ekim düzeltmesinin yerinde durduğu da denendi: sekmeler gezildi, üç ders bitirildi
(yanlış sonuç, sandıklı ders, reklam + puanlama). Sandık kabı görünür kaldı
(`block=result_overlay:NewChestFragment hostVisible=true`), her dönüşte kuyruk kilidi bırakıldı
ve `bos` yazdı, dönüşten sonra mağaza/seri ekranı ve yeni ders açılabildi. Çökme yok.
Rehber tek seferlik (yeni hesap + 1. bölümün ilk sandığı).

## Sıradaki iş 2 — yetim back stack girişleri (`fmBackStack=11`)

`LessonAdapter.continueWithLesson` dersi
`replace(abacusFragmentContainer, ...).addToBackStack(null)` ile açıyor. Ders normal bitince
(LessonResult → ChestFragment → NewChestFragment) yalnızca `"map_chest"` girişi pop ediliyor;
ders açılışının girişi back stack'te kalıyor. Ölçülen değer: **11 giriş**.

Sonuçları:

1. O fragment örnekleri bellekte canlı kalıyor, her tamamlanan derste bir tane daha birikiyor.
2. Haritada geri tuşuna basınca görünür hiçbir şey olmadan bu girişler tek tek pop ediliyor.
3. `performAbacusDismiss` / `performTutorialDismiss` / `BlindingLessonFragment` gibi yerler
   `fm.popBackStack()` ile **en üstteki** girişi atıyor ve o giriş artık kendilerinin değil.
4. `findFragmentById` bu girişlerin tuttuğu fragment'ı container boş olsa bile döndürüyordu —
   bu kısım `MainActivity.liveOverlayIn` (1878) ile kapatıldı, bkz. aşağıda.

Kalıcı çözüm ders bitiş yolunda bu girişi de pop etmek; ama o yol hassas (bkz. Çalışma
anlaşmaları). Ayrı bir turda, tek başına ve derleyerek yapılmalı. `docs/YAYIN_ONCESI_KONTROL.md`
teknik borç bölümünde de yazılı.

## Seri dondurma — yazıldı ve deploy edildi, cihazda DENENMEDİ (01.10.2026)

Mağazada "Özel Teklifler"in en altında, 4000 altına satılan tek kullanımlık koruma.

**Kurallar** (ürün kararları kullanıcıya soruldu, cevapları bunlar):

- Kaçırılan **ilk** günü kapatır; kapatılan gün seriye **eklenmez** (5 günlük seri 5 kalır).
- Aynı anda en fazla **1** tane tutulur; harcanınca yenisi alınabilir.
- İki gün üst üste kaçarsa dondurma **yine harcanır** ve seri yine kırılır.
- Satın alındığı günden **öncesini kurtarmaz** (dün kırılan seri bugün dondurma alınarak
  onarılamaz). Bu kural olmadan sunucu seriyi diriltiyordu, çünkü sunucuda kırılma hiç
  yazılmıyor: `current` yeni bir gün bildirilene kadar eski değerinde duruyor.

**Nasıl çalışıyor:** kaçan gün "köprü" olarak serinin son günü yapılıyor ve `frozenDays`'e
ekleniyor; ardışıklık kontrolünün hiçbir satırı değişmedi. Kural iki yerde, aynı girdiyle aynı
sonucu vermek zorunda:

| Taraf | Yer |
|---|---|
| Sunucu | `functions/index.js` → `settleStreakFreeze`, `applyStreakDays`, `buyStreakFreeze`, `submitStreakDay` |
| İstemci | `StreakFreezeRules.settle` (yan etkisiz) + `StreakRepository.applyFreeze` / `adoptServerState` |

İstemci dondurmayı kendi tarafında da harcıyor (çevrimdışı açılışta seri kırık görünmesin
diye) ve `submitStreakDay` / `buyStreakFreeze` çağrılarında yerel gününü (`today`) gönderiyor;
sunucu "dün kaçtı mı"yı o güne göre soruyor. Sunucu alanları: `freezes`, `freezeDay`,
`frozenDays` (`users/{uid}/streak/state`).

Fiyat iki yerde: `STREAK_FREEZE_COST` (sunucu, düşülen miktar) ve
`StreakRepository.FREEZE_COST_GOLD` (istemci, kartta yazan). Biri değişirse diğeri de değişmeli.

**Testler** (hepsi geçiyor):

```powershell
node functions/scripts/test-streak-freeze.js        # kural, kaynaktan çekilerek (48)
node functions/scripts/test-streak-freeze-flow.js   # gerçek fonksiyonlar, sahte Firestore (47)
.\gradlew testDebugUnitTest --tests "*StreakFreezeRulesTest"   # istemci ikizi (14)
```

**Doğrulanan:** derleme, yukarıdaki testler, deploy (`submitStreakDay` güncellendi,
`buyStreakFreeze` oluşturuldu; ikisi de oturumsuz istekte `UNAUTHENTICATED` dönüyor, yani
yükleniyorlar).

**Cihazda doğrulanan (01.10.2026 23:30, kullanıcı elle denedi + log + ekran görüntüsü):**

- Deploy sonrası `submitStreakDay` gerçek çağrılarda çalışıyor: günsüz bildirim
  (`Sync.cevap | BASARILI current=0`) ve gün bildirimi (`BASARILI current=1 lastDay=2026-10-01`).
- Satın alma: `Repo.dondurma | SATIN_ALINDI adet=1 gun=2026-10-01`; altın 4019 → 19; sonraki
  sunucu okumalarında `dondurma=1` (uygulama yeniden başlatıldıktan sonra da).
- Mağaza kartı "✓ HAZIR" hâlinde, seri ekranındaki satır "Hazır" (ok yok); satın almadan
  hemen sonra seri ekranı yeniden çizildi (`RESULT_STREAK_FREEZE_BOUGHT`).
- Seri ekranındaki "Yok ›" satırından mağazanın kartta açılması kullanıcı tarafından denendi.

**Doğrulanmayan — dondurmanın HARCANMASI.** Yalnızca testlerle biliniyor; cihazda görmek için
gerçekten gün atlamak gerekiyor:

1. Dondurma eldeyken ertesi gün **hiç çalışma** → öbür gün aç. Beklenen: "seri dondurman
   serini korudu" bildirimi, hafta şeridinde kaçan günde kar tanesi, seri sayısı aynı, seri
   ekranında "Yok ›", mağazada düğme yeniden `4000`. Logda
   `Repo.dondurma | HARCANDI … seriKurtuldu=true`. Aynı gün hedef tutturulunca seri +1.
2. İki gün üst üste kaçırma: seri 0, dondurma gitmiş, "serin kırıldı" bildirimi.
3. Geçmişi onarmama: dondurma yokken bir gün kaçır, ertesi gün dondurma al → seri geri
   gelmemeli, o gün çalışınca 1'den başlamalı, dondurma "Hazır" kalmalı.

**Saati ileri alarak deneme** — sunucu istemcinin gününü ±1 günden fazla sapınca kabul
etmiyor, iki taraf ayrışır.

**Yapılmayan, fikir olarak duran:** dondurma harcandığında toast yerine küçük bir kutlama
ekranı; akşam hatırlatmasının "dondurman var" diyen bir çeşidi.

## Harita dışı dönüşlerde yeni seri sorusu: kupa testi, günlük soru, yarış dersi (02-03.10.2026)

Görevler ekranından açılan iki ders — kupa yolu kartlarındaki test (kupa modu, bölüm 9) ve
günlük soru — nasıl kapanırsa kapansın (doğru, yanlış, çıkış düğmesi, geri tuşu) serisi olmayan
kullanıcıya `NewStreakFragment` açılıyor. Koşul haritadaki ders dönüşüyle aynı
(`StreakRepository.needsNewStreakPrompt`: seri 0 ve bugün sorulmadı). Önce kupa testi yazıldı
(aşağıdaki maddeler onun hikâyesi), günlük soru aynı akşam eklendi (en alttaki bölüm).

**İsimler:** mekanizma kupa testi için yazıldığında adları `cup…` ile başlıyordu
(`requestNewStreakPromptAfterCupTest`, `isCupResultCovered`, `cupResultTouchBlocker`, log öneki
`kupa yeni seri` / `kupa dokunma engeli`). Günlük soru eklenince ortak olanlar yeniden
adlandırıldı: `requestNewStreakPromptOnTasks`, `isTasksReturnCovered`, `tasksReturnTouchBlocker`,
`runWhenTasksReturnUncovered`, log öneki `gorevler …`. `7cc046a` commit'inde eski adlar duruyor;
aşağıdaki doğrulama kayıtlarındaki log satırları o günkü önekle (`kupa …`) basılmıştı.

**Neden ders sonrası kuyruğunun içinde değil:** kuyruk (`pumpPostLessonQueue`) yalnızca harita
tabanında çalışıyor; kapısı ve adımlarının çoğu haritaya özel. Kupa testi Görevler'den açılıp
oraya dönüyor. Kuyruğa dokunulmadı; `MainActivity`'de kendi küçük kapısı ve bekleyişi var
(`requestNewStreakPromptOnTasks` → `runOffMapNewStreakPrompt` → `offMapNewStreakBlockReason`;
iç adlar 03.10.2026'da değişti, bkz. "Yarış dersi dönüşü").

**Sıra — kullanıcının kararları, üç turda oturdu:**

    reklam → (Pro paneli) → yeni seri sorusu → kupa paneli ve sayacın akması → rozet kutlaması

Her adım bir öncekinin KAPANMASINI bekliyor; hiçbiri bir diğerinin altında başlamıyor.
**Bunu "düzeltip" haritadaki sıraya (rozet → yeni seri) döndürmeyin.** Nasıl buraya gelindi:

1. İlk sürüm haritadaki sırayı koruyordu ve soruyu kupa sayacı + rozet listesinin arkasına
   koyuyordu. Cihazda soru testten 1,7–3,4 sn sonra geldi (rozet listesi sunucudan geliyor,
   süre her seferinde farklı); kullanıcı geç buldu ve "hemen" istedi.
2. Soru hemen açılınca rozet kutlaması onun ALTINDA açıldı (cihazda görüldü, 10:43 ve 10:44).
   Aynı şey eskiden beri reklamın ve Pro panelinin altında da oluyordu (10:29, 10:41):
   kupa dönüşü, reklam olup olmadığına bakmadan her şeyi aynı anda başlatıyordu.
3. Şimdi: `TasksFragment`'teki `cupModeResult` dinleyicisi her şeyi tek blokta
   (`startCupResult`) ve `checkAndShowInterstitialAdIfAllowed`'ın geri çağrısında başlatıyor.
   Blok önce yeni seri isteğini gönderiyor, sonra kupa panelini `runWhenTasksReturnUncovered`
   ile bekletiyor (`MainActivity.isTasksReturnCovered`: soru sırada/ekranda ya da Pro paneli
   açık). Rozet kutlaması sayaç oturduktan sonra açıldığı için ayrıca bekletilmiyor.

4. İlk denemede rozet YİNE sorunun altında açıldı. Sebep: bekleyen kupa farkını tüketen üç
   yer var — dinleyici, `TasksFragment.onResume` ve `onHiddenChanged` (son ikisi güvenlik
   ağı). Sonuç bekletilince reklam kapanıp uygulama öne geldiğinde `onResume` farkı kendisi
   tüketti ve paneli + rozeti soruyu beklemeden başlattı. `cupResultDeferred` bayrağı bekleme
   boyunca o iki yolu kapatıyor. **Kupa sonucunu bekleten bir şey eklerseniz bu üç tüketiciyi
   birlikte düşünün.**

5. Sıra oturdu ama reklam → soru akışında soru kayarak gelirken ARKASINDA kupa paneli bozuk
   göründü: eski görüntüsüyle, 88 px sağa-aşağı kaymış ve arkası karartılmış (ekran
   görüntüsü 11:07). Sebep uygulamada değil, Android'in pencere yöneticisinde — ama onu
   tetikleyen bizim bekletmemiz:
   - Panel test boyunca kapatılmıyor, `Dialog.hide()` ile gizleniyor. Sistem gizlenen
     pencerenin yüzeyini activity DURDURULANA kadar son karesiyle saklıyor (`mDestroying`).
   - Reklam ekranı (`AdActivity`) yarı saydam: `MainActivity` duraklıyor ama durmuyor.
     Dönüşte sistem `notifyAppResumed(wasStopped=false)` → `destroySurfaces(cleanupOnResume)`
     ile `mDestroying`'i siliyor, yüzeyi YOK ETMEDEN. `WindowState.isOnScreen()` pencerenin
     GONE olduğuna bakmıyor → eski yüzey yeniden gösteriliyor.
   - 88 px, pencerenin gölge payı (`surfaceInsets`): gizlenirken oynayan çıkış animasyonu
     konumu sıfırlıyor, pencere GONE olduğu için `updateSurfacePosition` geri koymuyor.
   - Eskiden görülmüyordu çünkü panel reklamdan ÖNCE geri açılıyordu. Bekletme (madde 3)
     paneli reklam boyunca gizli bıraktı.

   Düzeltme (`TasksFragment.concealHiddenCupPanel` / `revealCupPanel`): gizli panelin
   penceresi saydam yapılıyor (`alpha=0`) ve karartma bayrağı kaldırılıyor — yüzey geri gelse
   de görülmüyor. İki yerden çağrılıyor: test kapanınca (reklamdan önce) ve `onResume`'da.
   İkincisi pencereyi sisteme yeniden bildirdiği için geri gelmiş yüzey yeniden gizleniyor;
   böylece panel açılırken giriş animasyonu da oynuyor (yoksa sistem onu "zaten ekranda"
   sayıp atlıyordu). `revealCupPanel`, `show()`'dan hemen önce geri alıyor; "saydam mı"
   bilgisi ayrı bir bayrakta değil pencerenin kendi `alpha` değerinde duruyor.
   **Paneli `hide()` ile gizleyen başka bir yol eklerseniz bu ikiliyi de kullanın;** gizli
   panel + yarı saydam bir activity (reklam, izin penceresi, paylaşım menüsü) aynı hayaleti
   üretir. Bilinen, dokunulmayan hâli: test SIRASINDA böyle bir activity açılıp kapanırsa
   hayalet artık kalıcı değil ama `onResume`'a kadar bir an görünebilir.

   Pencerenin durumunu görmek için:
   `adb shell dumpsys window com.numigo.app/com.example.app.MainActivity` — kupa paneli
   `ty=APPLICATION` ve `surfaceInsets` olan pencere; `mAttrs` içinde `alpha=0.0`,
   `fl=` içinde `DIM_BEHIND`, `mViewVisibility` (0x8 = gizli), `mHasSurface`,
   `Surface: shown=`, `mDestroying=` alanlarına bakın.

6. Sıra beklenirken ekrana dokunulabiliyordu; iki ayrı hata çıktı (cihazda görüldü, 17:38 ve
   17:41):
   - **Soru gelmeden karta basma.** Test kapanışı ile sorunun gelişi arasında (~0,25 sn +
     sorunun kayması) Görevler ekranı açıkta. Kupa Yolu kartına basılınca İKİNCİ bir panel
     açıldı ve sorunun üstünde kaldı; gizli bekleyen ilk panel de sahipsiz kaldı
     (`showCupPathPanel`'deki "zaten açık mı" kontrolü `isShowing`'e bakıyor ve gizli panel
     için false dönüyor). Sahipsiz panel pencere listesinde sonsuza dek duruyor.
   - **Sayaç akarken sandığa basma.** Panel geri geldikten 0,45 sn sonra sandığa basıldı;
     sandık ekranı Görevler'i gizlediği için bekleyen rozet kutlaması sessizce atlandı.

   Düzeltme: `activity_main.xml`'e görünmez bir dokunma katmanı eklendi
   (`tasksReturnTouchBlocker`, yükseklik 9.5dp). Görevler, alt çubuk ve para panelini kapatıyor
   ama ders/sandık/rozet kaplarının (10dp) ve sezon kapısının (30dp) ALTINDA: sıra
   beklenirken açılan bir kutlama ya da kapı dokunulabilir kalıyor, katman kilitlenmeye yol
   açamıyor. **Yüksekliğini 10dp'nin üstüne çıkarmayın.** Katman iki sebeple açık:
   - `cupResultDeferred` — test kapanışından panelin geri gelişine kadar (reklam, Pro
     paneli ve soru kendi pencerelerinde, etkilenmiyorlar);
   - `cupBadgeTouchHold` — panel geri geldikten sonra rozet kararına kadar. Bu sürede panelin
     penceresi de dokunulmaz (`FLAG_NOT_TOUCHABLE`); dokunuşlar alttaki katmana düşüp
     yutuluyor. Rozet yoksa karar gelir gelmez, varsa kutlama açılırken bitiyor.

   Rozet listesi sunucudan geliyor ve gecikebiliyor. Karar `CUP_BADGE_TOUCH_HOLD_MS`
   (sayaç + geçiş payı, 1,35 sn) içinde gelmezse dokunma AÇILIYOR — listeyi sonuna kadar
   (5 sn) beklemek, internet zayıfken paneli her dönüşte ölü bırakırdı. Liste sonradan
   rozetle gelirse kutlama yalnızca çocuk bir şeye başlamadıysa açılıyor (`isHidden` ya da
   `isCupTestStarting`: zorluk paneli açık / test başlatılmış); başladıysa kutlama atlanıyor,
   rozet yine kazanılmış oluyor. Kullanıcıya bu ödünleşim söylendi; "kutlama hiç kaçmasın"
   denirse sabiti `CUP_BADGE_WAIT_MS` yapmak yeterli.

   Güvenlik: katman açık unutulursa Görevler VE alt çubuk ölü kalır. Kapatan yollar: sonuç
   bloğunun sonu, rozet kararı/süre dolması, `TasksFragment.onDestroyView`. Ayrıca katman her
   dokunuşta `TasksFragment.needsTasksReturnTouchBlock()`'u soruyor; gerekmiyorsa kendini
   kaldırıyor (logda `gorevler dokunma engeli SAHIPSIZ`). Normal açılıp kapanışı
   `gorevler dokunma engeli ACIK / KAPALI` satırlarıyla görülüyor (`PostLessonQueue` etiketi).

Ayrıntılar: soru test kapandıktan ~0,3 sn sonra açılıyor. `restorePartAfterCupLesson` hiçbir
şeyi beklemiyor. Reklam kararı hiç gelmezse 2 sn sonra (reklam ekranda değilse) blok yine de
başlıyor; örten pencere 3 dakikada kapanmazsa kupa sonucu yine de işleniyor (bekleyen kupa
farkı tüketilmezse bir sonraki testte bayat okunurdu). Reklam da soru da yoksa — dönüşlerin
çoğu — blok eskisi gibi aynı çağrının içinde çalışıyor. Yan etki: reklam aralığı dolmuş ama
reklam çıkmayan dönüşlerde panel ~350 ms geç geliyor (karar bir ders listesi okumasını
bekliyor).

Soru için: istekten sonra araya girebilecekleri (Pro paneli, zaten açık bir rozet kutlaması,
yeni bir test) kapı 400 ms'de bir yokluyor; üç dakikada açılamazsa ya da kullanıcı
Görevler'den ayrılırsa soru düşürülüyor, koşullar sürdükçe bir sonraki dönüşte yeniden
soruluyor.

### Günlük soru dönüşü (02.10.2026 akşamı — cihazda doğrulandı, anahtar açık ve kapalı)

Kullanıcı günlük soru kartından dönüşte de sorunun sorulmasını istedi. Günlük soru kupa
testinden farklı kapanıyor: `BlindingLessonFragment.closeFragment` →
`MainActivity.finishTasksOverlayAnimated("dailyQuestion.close", fromDailyQuestion = true)` →
320 ms'lik kapanış animasyonu → `completeTasksOverlayDismiss` (reklam kontrolü BURADA, yani
`MainActivity`'de). Bu yüzden iki haber `MainActivity`'den `TasksFragment`'e gidiyor:

1. `onDailyQuestionClosing` — Görevler görünmeden ÖNCE. Soru sorulacaksa
   (`needsNewStreakPrompt`) `dailyReturnDeferred` kuruluyor: kart tazelenmiyor, ekran dokunmaya
   kapanıyor. Sorulmayacaksa HİÇBİR ŞEY değişmiyor; serisi olan kullanıcıda dönüş eskisiyle
   birebir aynı (bilerek: çalışan yolu değiştirmemek için).
2. `onDailyQuestionReturnSettled` — reklam kontrolünün geri çağrısında. Soru burada isteniyor,
   kart `runWhenTasksReturnUncovered` ile soru (ve Pro paneli) kapanınca tazeleniyor.

Sıra: reklam → (Pro paneli) → yeni seri sorusu → kartın tazelenmesi. Kart neden bekliyor:
tazelenmesi iki TEK SEFERLİK animasyonu başlatıyor (ilerleme çubuğu 2,8 sn, yanlış cevapta
kırık kalp); dönüş anında başlasalar sorunun altında oynar, çocuk kartını değişmiş bulur ama
değişirken göremezdi. Kupa sayacındaki dersin aynısı. Kartı tazeleyen üç yer var
(`onHiddenChanged`, `onResume`, bekletmenin sonu); ilk ikisi bekleme süresince kapalı.

Güvenlik: reklam kararı hiç gelmezse 2 sn sonra (reklam ekranda değilse) yine de devam
ediliyor; `onDestroyView` bekletmeyi sıfırlıyor; kapanış iki kez bildirilirse ikincisi yok
sayılıyor.

**Kapsam dışı (bilerek):** günlük sorunun ÖDÜL sandığı (kart 3/3 olunca "al"a basılıp açılan
sandık). O bir ders dönüşü değil; istenen soru ekranından dönüştü. Reklamın kart animasyonunun
üstüne gelmesi de (soru sorulmayan dönüşlerde) eskisi gibi duruyor.

**Cihazda doğrulandı (19:02–19:04, test anahtarı açık, kullanıcı "sorunsuz" dedi):** dört
günlük soru dönüşü ve bir kupa testi.
- Reklamlı dönüş: engel 19:02:39.36'da açıldı, reklam 19:02:40.0–45.3, soru 19:02:45.41
  (`block=not_resumed` bekleyip reklam kapanınca), soru kapandıktan 0,14 sn sonra engel kalktı.
- Reklamsız dönüş: kapanıştan 0,34 sn sonra soru (`bekleme=0ms`; 0,32 sn kapanış animasyonu).
- Çıkışla kapanan iki dönüşte de soru geldi; engel her seferinde soru kapandıktan sonra kalktı.
- Kupa testi (`caller=cupModeResult`) yeniden adlandırmadan sonra da aynı çalışıyor.
Çökme yok.

Anahtar KAPALIYKEN de doğrulandı (19:12–19:13, üç günlük soru dönüşü, kullanıcı "sorunsuz"
dedi): logda `gorevler yeni seri` ve `gorevler dokunma engeli` satırı hiç yok, yani dönüş
eskisiyle aynı yoldan geçti.

### Yarış dersi dönüşü (03.10.2026 — yeni seri ve rozet cihazda doğrulandı; harita dönüşü de sonrasında denendi)

7-8. kısım dersleri haritadan değil, kısım seçimi ekranının (`PartSelectionFragment`) üstündeki
yarış panelinden açılıyor (`LessonAdapter.showRacePanel` → `onRaceStartClicked` →
`BlindingLessonFragment`, `raceBusyLevel != null`). Kullanıcı test anahtarı açıkken bu
derslerden sonra sorunun gelmediğini, ardından rozet kutlamasının da açılmadığını bildirdi.

**Neden gelmiyorlardı:** yarış dersi de haritadaki derslerle aynı dönüş fonksiyonundan geçiyor
(`finalizeMapReturnAfterLessonClaim`); soru (`newStreakPromptQueued`) ve rozet
(`pendingBadgePayloadsForAd`) ders sonrası kuyruğuna giriyordu. Kuyruğun kapısı ise harita
tabanı istiyor (`marathonGuideMapBlockReason` → `base_not_map:PartSelectionFragment`), yani
hiçbiri açılmıyordu. Bayraklar silinmediği için ikisi de, çocuk sonradan bir haritaya
girdiğinde alakasız bir anda çıkıyordu. Rozet için cihaz logu:

```
00:15:11.516 tur | caller=enqueuePendingBadgePayloads rozet=2 …
00:15:16.614 bekliyor | caller=watchdog block=base_not_map:ProfileFragment
00:15:31.165 bekliyor | caller=watchdog block=base_not_map:PartSelectionFragment
00:17:09.610 bekci BIRAKTI | sure doldu, harita kilidi aciliyor
```

**Yeni seri — kullanıcı onayladı (`finalizeMapReturnAfterLessonClaim`'e dokunuyor):**

- `finalizeMapReturnAfterLessonClaim` içinde tek dal: taban `PartSelectionFragment` ise soru
  kuyruğa girmiyor, `requestNewStreakPromptOnPartSelection` ile Görevler'deki bağımsız yoldan
  isteniyor. Taban haritayken satırlar eskisiyle aynı.
- Görevler mekanizması iki tabanı tanıyacak şekilde genişledi: istek hangi taban için
  yapıldıysa (`offMapNewStreakBaseClass`) soru yalnızca o tabandayken açılıyor, taban
  değişirse düşürülüyor.
- Sıra: reklam → (Pro paneli) → yeni seri. Görevler'den farkı: istek dönüş anında gönderiliyor,
  reklamı kapı bekliyor (`ad_check_in_progress`, `not_resumed`). Reklam kontrolü dönüşten bir
  kare sonra başladığı için soru 250 ms'den önce açılmıyor (`RACE_NEW_STREAK_DELAY_MS`).
- Dokunma: soru gelene kadar ekran kapalı, Görevler için eklenen katmanla
  (`tasksReturnTouchBlocker`). Katmanın artık iki sahibi var ve ikisinden biri istediği sürece
  açık. Yarış dönüşündeki engel en fazla 4 sn geçerli (`RACE_TOUCH_HOLD_MAX_MS`): reklam
  kararı hiç gelmezse ilk dokunuş engeli kaldırıyor.

**Cihazda doğrulandı (00:14–00:19, test anahtarı açık, kullanıcı "sorunsuz çıkıyor" dedi):**

```
00:14:46.493 yaris yeni seri SIRADA | caller=BlindingLessonFragment.quit bekleme=243ms
00:14:46.493 yaris dokunma engeli ACIK
00:14:46.747 yaris yeni seri ACILIYOR | caller=retry
00:14:46.783 yaris dokunma engeli KAPALI
   … reklamlı dönüş …
00:15:59.208 yaris yeni seri SIRADA | caller=ChestFragment.claimAfterRemove bekleme=246ms
00:15:59.454 yaris yeni seri bekliyor | caller=retry block=ad_check_in_progress
00:15:59.535 START AdActivity
00:15:59.869 yaris yeni seri bekliyor | caller=retry block=not_resumed
00:16:05.179 yaris yeni seri bekliyor | caller=retry block=ad_skip_showing
00:16:06.431 yaris yeni seri ACILIYOR | caller=retry
```

Dört yarış dönüşü (bir çıkış, üç bitirme). Aynı turda üç günlük soru ve iki kupa testi dönüşü
de denendi; `gorevler …` satırları eskisiyle aynı, kullanıcı "sorunsuz" dedi.

**Rozet — kullanıcı onayladı (`runPostLessonQueue`'ya dokunuyor), cihazda doğrulandı:**
kuyruğun kapısı kapalıyken tek bir dal: `showBadgeOnPartSelectionStep`. Taban kısım seçimiyse
ve rozet bekliyorsa, harita istemeyen kapıyla (`offMapNewStreakBlockReason`) yalnızca rozet
adımı (`showBadgeStep`) çalışıyor. Yeni seri sorusu sıradayken ya da açıkken bekliyor; sıra
reklam → yeni seri → rozet (haritadaki rozet → yeni seri sırasının tersi, kupa testindekinin
aynısı: rozet listesi sunucudan ~2 sn geç geliyor). Bekçi kuyruğu saniyede bir dürttüğü için
geç gelen liste de yakalanıyor. Taban haritayken dal hiçbir şey yapmıyor.

Doğrulama (00:30–00:33, yeni test aboneliğiyle, kullanıcı "sorunsuz" dedi): dört yarış
dönüşünde soru geldi; rozetli olanda sıra tuttu, çökme yok.

```
00:32:39.436 yaris yeni seri SIRADA | caller=MissionChestReward.continue bekleme=246ms
00:32:39.449 tur | caller=finalizeMapReturn:MissionChestReward.continue rozet=1 …
00:32:39.701 yaris yeni seri ACILIYOR | caller=retry
00:32:41.729 rozet | caller=NewStreakFragment.dismiss        ← soru kapanınca
00:32:41.785 ekran=BadgeFragment … 00:32:47.755 ekran=PartSelectionFragment
```

**Harita dönüşü bu iki değişiklikten sonra da aynı (00:34–00:37, beş dönüş, çökme yok):**
kuyruk sırası tuttu ve kilit her seferinde bırakıldı. En dolu dönüş:

```
00:36:08.625 kilit aliniyor
00:36:09.154 bekliyor | block=badge_firestore_pending
00:36:10.351 rozet | caller=enqueuePendingBadgePayloads
00:36:16.410 yeni seri | caller=BadgeFragment.onDestroyView
00:36:18.641 rating | caller=NewStreakFragment.dismiss
00:36:19.797 rehber | caller=RatingDialog.dismiss
00:36:20.535 GuideDebug: releasePostLessonQueueTouchLock SKIP: rehber paneli acik, …
```

Yani rehber kilidi düzeltmesi (`3d6da7f`) de yerinde. Diğer dönüşler: rozetsiz bitirme,
başarısız sonuç (`LessonResultFalse`), görev sandığı + rozet (iki kez).

Aynı turda görülen, YENİ OLMAYAN iki şey:

- Haritaya girer girmez öğretmene sorma tanıtımı açıldı (`00:34:35.904 ogretmene sorma |
  caller=MapFragment.onResume`). Yarış dönüşlerinden kalan `promo=true` bayrağı; aşağıdaki
  "DOKUNULMADI" maddesinin cihazdaki hâli. (Yerelde `AskQuestionPromoDebug.FORCE` açık olduğu
  için tanıtım her seferinde açılıyor; gerçekte uygunluk ve sayaç kontrolünden geçer.)
- Bir dönüşte yeni seri sorusunu rozet kapanırken bekçi açtı (`00:37:24.205 yeni seri |
  caller=watchdog`, `BadgeFragment.onDestroyView` 0,37 sn sonra): soru, rozetin çıkış
  animasyonu sürerken geldi. Görsel bir sorun bildirilmedi.

Bilinen sınırlar: rozet beklenirken ekran dokunmaya kapalı DEĞİL (kupa testindeki 1,35 sn'lik
bekletme burada yok); kullanıcı rozet gelmeden başka sekmeye geçerse kutlama bir sonraki
dürtüde (ders dönüşü, uygulamanın öne gelmesi ya da bir haritaya giriş) açılıyor.

**Denemek için Pro gerekiyor:** 7-8. kısım Pro'ya kilitli. Test aboneliği 5 dakikada bir
kendiliğinden yenileniyor ve yaklaşık 30 dakikada bitiyor; kısım kilitlenirse abonelik
bitmiştir, yenisini almak gerekir (bkz. "Pro'dayken reklam çıktı: test aboneliği bitmişti").

**İsimler (ikinci kez):** mekanizmanın iç adları `tasksNewStreak…` idi; artık yarışı da
taşıdığı için `offMapNewStreak…` oldu (`runOffMapNewStreakPrompt`, `offMapNewStreakBlockReason`,
`OFF_MAP_NEW_STREAK_BUDGET_MS` …). Dışarıdan çağrılan adlar aynı kaldı
(`requestNewStreakPromptOnTasks`, `isTasksReturnCovered`, `setTasksReturnTouchBlock`), yani
`TasksFragment`'te kod değişmedi. Log öneki dönüşe göre: Görevler'de eskisi gibi `gorevler …`,
yarışta `yaris yeni seri …` / `yaris dokunma engeli …`.

**Öğretmene sorma tanıtımı — kullanıcı onayladı (`finalizeMapReturnAfterLessonClaim`'e
dokunuyor), cihazda doğrulandı (00:47, kullanıcı "sorunsuz" dedi):** tanıtım haritaya bağlı. Yarış dönüşünde
bayrağı (`pendingLessonTypeReturnForPromo`) kurulduğu için kısım seçimi ekranında açılamıyor,
bekliyor ve çocuk bir haritaya girer girmez çıkıyordu (yukarıdaki 00:34:35 satırı). Artık
taban kısım seçimiyken bayrak hiç kurulmuyor (`promoReturn`); taban haritayken aynı.
Doğrulama: iki yarış dönüşünde (`00:47:18` bitirme + iki rozet, `00:47:36` çıkış) logda
`promo=false`; ardından haritaya girişte `00:47:44.047 bos | caller=MapFragment.onResume`,
yani tanıtım açılmadı. Hemen sonra görülen tanıtım (`00:47:48.99 AskQuestionOpenFragment`)
başka bir yoldan: kullanıcı haritada bir eğitim (`TutorialFragment`) açıp kapattı, o dönüş
tanıtımı kendi yolundan deniyor ve yerelde tanıtım anahtarı açık.

Aynı turda bir harita dersi dönüşü de vardı (`00:48:09`, yeni seri → kupa yolu). Kupa yolu
adımı Görevler'e geçince kuyruk 6 sn boyunca `kilit BIRAKILAMADI | reason=map_yok` yazdı,
`00:48:19.999`'da bıraktı. Bu işin dokunduğu bir yer değil (`showCupPathStep` aynı) ve
kullanıcı bir sorun bildirmedi; o 6 saniyede ekranın kilitli kalıp kalmadığı İNCELENMEDİ.

**Aynı nedene bağlı, DOKUNULMADI:** rating adımı (`justFinishedChestForRating`) kısım seçimi
tabanında hâlâ açılamıyor; kurulursa bir haritaya girilene kadar bekler. Yarış dönüşlerinde
kurulduğu görülmedi (`rating=false`), ele alınmadı.

### Test anahtarı ve kupa testi turlarının doğrulama kayıtları

**Test anahtarı:** koşulları elde etmek zor (yeni hesapta kayıt akışı soruyu o gün için
işaretliyor, serisi olan hesapta kırılmayı beklemek gerekiyor).
`NewStreakPromptDebug.FORCE = true` iki koşulu da atlıyor; depoda `false` durmalı. Logda
`PostLessonQueue` etiketiyle `gorevler yeni seri SIRADA / bekliyor | block=… / ACILIYOR /
DUSURULDU` satırları.

**Şu anki durum (03.10.2026 00:58):** kullanıcının isteğiyle üç yerel test anahtarı da
**kapatıldı** ve bu hâliyle cihaza kuruldu: `NewStreakPromptDebug.FORCE`,
`AskQuestionPromoDebug.FORCE`, `MissionProgressDebug.RESET_ON_LAUNCH` (kodda `= true` kalan
başka anahtar yok). Son ikisi artık depodaki hâlleriyle aynı; `NewStreakPromptDebug.kt`'de
yalnızca yorum düzeltmesi (günlük soru ve yarış dersi yolları) duruyor, commit edilebilir.
Anahtar kapalıyken yarış dersi dönüşü denendi (01:04–01:05, kullanıcı "sorunsuz" dedi): iki
dönüşte `yaris yeni seri GEREKMIYOR` (seri var), dokunma engeli hiç açılmadı; rozetli olanda
kutlama liste gelir gelmez açıldı (`01:05:24.637 rozet | caller=enqueuePendingBadgePayloads`).
Ardından haritaya girişte tanıtım çıkmadı (`bos | caller=MapFragment.onResume`). Sorunun
gerçek koşulda (seri 0 ve bugün sorulmadı) açılması görülmedi.

**Cihazda doğrulanan (anahtar açıkken, önceki turlarla):** sorunun test kapanışlarında
açılması (kullanıcı dört kapanış yolunu denedi; log yolları ayırt etmiyor, ilk turda altı
kapanışın beşinde açıldı); kullanıcı Görevler'den ayrılınca düşürülmesi
(`neden=gorevlerden_ayrildi`);
reklamlı dönüşte sorunun reklamı ve Pro panelini beklemesi (`block=ad_skip_showing`);
"hemen" zamanlaması (kapanıştan ~0,26 sn sonra); kupa paneli ve rozetin soruyu beklemesi
(11:03: soru 11:03:55.9'da kapandı, rozet 11:03:57.5'te açıldı — kullanıcı "yapı çalışıyor"
dedi). Çökme yok.
Madde 5 (hayalet panel) de doğrulandı — 17:34–17:41, kullanıcı reklamlı akışı rozetli ve
rozetsiz denedi, "sorunsuz" dedi; pencere kaydı da aynısını gösteriyor: test kapanınca
`alpha=0.0` ve `DIM_BEHIND` yok; reklam dönüşünde yüzey geri geliyor ama `Surface: alpha=0.0`
(17:36:16.9), bir kare sonra yeniden `mDestroying=true`; soru kapanınca panel `alpha=1`,
karartmalı ve görünür.
Madde 6 (dokunma engeli) de doğrulandı — 18:03–18:04, kullanıcı "kusursuz" dedi; kayıtlar:
rozetsiz dönüşte engel test kapanışında açıldı (18:03:08.0), soru kapandıktan 0,13 sn sonra
kapandı (18:03:17.6) ve panel dokunulabilir geldi; rozetli dönüşte panel `NOT_TOUCHABLE`
olarak geldi, engel kutlamayla aynı anda kapandı (18:04:06.55 / rozet 18:04:06.62). İkinci
panel ya da sahipsiz pencere yok.
Sorusuz dönüş de doğrulandı (test anahtarı KAPALI, 18:10–18:13, dokuz dönüş; kullanıcı
"sorun yok" dedi): logda `gorevler yeni seri GEREKMIYOR`; reklamlı dönüşte panel reklam kapanır
kapanmaz dokunulabilir geldi; rozetli dönüşte engel 1,36 sn sonra kutlamayla birlikte kalktı;
çıkışla kapanan testte 9 ms'de kalktı. Rozetsiz dönüşlerde engel 0,9–1,36 sn sürdü: çocuk
testi hızlı kapatınca rozet listesi henüz gelmemiş oluyor, üç dönüşte süre sınırı
(`CUP_BADGE_TOUCH_HOLD_MS`) doldu. Yani o sınır nadir bir yedek değil, sık çalışan yol.
Çökme, sahipsiz pencere ya da `SAHIPSIZ` kaydı yok.
**Doğrulanmayan:** anahtar KAPALIYKEN gerçek koşullarla (seri 0 + bugün sorulmadı) sorunun
açılması; rozet listesinin süre sınırından SONRA rozetle geldiği durum (kutlama, çocuk bir
şeye başlamadıysa açılmalı, başladıysa atlanmalı — denk gelmedi).

## Çalışma süresi kapanan ders ekranında takılı kalıyordu (02.10.2026 — düzeltildi, cihazda doğrulandı)

**Belirti:** Görevler'den açılan bir ders ekranı (günlük soru, abaküs pratiği) çıkışla
kapanınca çalışma süresi Görevler ekranında da işlemeye devam ediyordu — başka bir ekran
açılana kadar. Günlük hedef bu süreye bağlı; çocuk ders çözmeden hedefi doldurabiliyordu
(dokunmaya devam ettikçe sınırsız, bırakırsa 5 dk — `MAX_IDLE_MS`).

**Kanıt** (eskiden beri var; günlük soruya dokunulmadan ÖNCEKİ sürümde de görüldü):

```
18:43:39.550 StudyTime.ekran | ekran=BlindingLessonFragment calismaSayilir=true bugun=12sn
18:43:40.132 [reconcile.tasks:schedule.tasks]                 ← Görevler geri geldi
18:43:44.148 StudyTime.parcaKapandi | hamSure=4sn             ← 3,7 sn'si Görevler'de geçti
18:43:44.156 StudyTime.ekran | ekran=TasksFragment bugun=16sn ← ancak yeni ders açılınca
```
Abaküs pratiğinde aynısı (18:43:32 kapandı, 18:43:34'e kadar sayıldı). Soru cevaplanarak
kapanan dönüşlerde ise ekran 25–70 ms içinde doğru yere dönüyor.

**Kök neden:** ekran takibi (`NumiGooApplication`) bir ekranın gittiğini yalnızca
`onFragmentViewDestroyed`'dan öğreniyordu. Çıkış animasyonuyla kaldırılan fragment'ın görünümü
animasyon BİTİNCE yok ediliyor; Görevler dönüşünde ise
`TasksFragment.onHiddenChanged` → `reconcileAbacusOverlayWhenTasksIsBase` kabı ~20 ms sonra
`GONE` yapıyor, gizli kapta animasyon ilerlemiyor ve görünüm aynı kaba bir sonraki `replace`'e
kadar yok edilmiyor. (1 Ekim'de LessonResult'ta görülen "çıkış animasyonu gizlenen kapta
bitemiyor"un aynısı.) Görevler `show()` ile geri geldiği için resume da olmuyor; takip eski
ekranda kalıyor.

**Düzeltme (yalnızca takipte):** `onFragmentPaused` de dinleniyor ve fragment (ya da üstündeki
bir fragment) `isRemoving` ise ekran o anda yığından çıkarılıyor. Arka plana geçişteki pause
`isRemoving` olmadığı için etkilenmiyor. Yığın artık ad + fragment örneği tutuyor
(`ScreenEntry`): kapanmış ama görünümü duran eski örneğin gecikmiş haberi, aynı sınıftan yeni
açılan ekranın kaydını silemesin.

**Dokunulmayan asıl sorun:** görünüm hâlâ bir sonraki `replace`'e kadar yok edilmiyor, yani
kapanmış fragment o süre boyunca yarı canlı (`onDestroyView` çalışmamış). Düzeltmesi Görevler
overlay kapanışının sırasını (`finishTasksOverlayAnimated` / `reconcileAbacusOverlayWhenTasksIsBase`)
değiştirmek demek; kapanış animasyonunun görünümünü de değiştirir. Ayrı bir iş.

**Cihazda doğrulandı (19:24–19:26, yeni hesapla):**

```
19:24:31.783 ekran=BlindingLessonFragment calismaSayilir=true bugun=0sn   ← günlük soru
19:24:38.230 parcaKapandi hamSure=6sn
19:24:38.253 ekran=TasksFragment calismaSayilir=false bugun=6sn           ← kapanış ANINDA
   ... reklam, sonra Görevler'de 13 sn ...
19:24:58.582 ekran=AbacusPracticeFragment calismaSayilir=true bugun=6sn   ← hâlâ 6
19:25:11.677 ekran=TasksFragment calismaSayilir=false bugun=19sn          ← pratik kapanışı ANINDA
   ... Görevler ve haritada 18 sn ...
19:25:29.910 ekran=TutorialFragment calismaSayilir=true bugun=19sn        ← hâlâ 19
```
Ders içinde süre eskisi gibi sayıldı (eğitim 30 sn + abaküs 13 sn → `bugun=62sn`), anket ve
sonuç ekranlarında durdu. Çökme yok.

## Pro'dayken reklam çıktı: test aboneliği bitmişti (03.10.2026 — hata YOK; ilk teşhis yanlıştı)

Kullanıcı Pro plandayken 7-8. kısım dersinden sonra reklam (ve ardından Pro paneli) gördü.
Hata değil: test aboneliği o anda bitmişti.

**Ne oldu:** abonelik 23:45:56'da alındı. Lisans testçisinde aylık abonelik 5 dakikada bir
kendiliğinden YENİLENİR ve birkaç yenilemeden sonra kendiliğinden BİTER; bu abonelik
alındıktan 30 dakika sonra, 00:15:56'da bitti. Aynı oturumda 00:14:46 ve 00:15:09'daki
dönüşlerde reklam çıkmadı, 00:15:59'da çıktı; plan o arada Pro'dan Free'ye düştü — olması
gerektiği gibi.

**İlk teşhis yanlıştı, tekrar etmeyin.** Önce "uygulama her 5 dakikalık dönem sınırında planı
Free'ye düşürüyor, çünkü `planExpiresAt`'i yalnızca açılışta yeniliyor" sonucuna varıldı ve
bir düzeltme önerildi. Bu, yalnızca saatlerin örtüşmesinden çıkarılmıştı; sunucudaki RTDN
dinleyicisi gözden kaçmıştı:

- `functions/index.js` → `playSubscriptionNotification` (Pub/Sub konusu `play-rtdn`): Play her
  yenilemede bildirim gönderiyor, sunucu `plan` / `planExpiresAt`'i güncelliyor.
- İstemci bunu cüzdan dinleyicisinden öğreniyor: `UserWalletFirestore` planı süre kontrolüyle
  okuyor (`PlanStatus.effectivePlan`), `MainActivity.applyWalletToUi` kayıtlı plandan farklıysa
  `checkSubscriptionAndUpdateEnergy` çağırıyor.

Yanlışlığı gösteren kanıt: ikinci test aboneliği 00:30'da alındı ve 20 dakika sonra, en az üç
dönem sınırı geçmişken plan hâlâ `Pro` idi (`run-as com.numigo.app` ile
`shared_prefs/energy_prefs_<uid>.xml` → `user_plan`; dosya 00:30'dan beri değişmedi). Arada
sandık ödülleri cüzdanı defalarca değiştirdi, reklam da çıkmadı.

**Kod değişikliği yapılmadı ve gerekmiyor.** Görülmeyen tek şey: yenileme bildirimi geç
gelirse arada birkaç saniyelik bir "Free" penceresi oluşuyor mu — gözlenmedi.

**Test ederken:** test aboneliği yaklaşık yarım saatte biter; Pro kısımlar kilitlenir ve reklam
çıkmaya başlarsa önce aboneliğin bitip bitmediğine bak (Play Store → Ödemeler ve abonelikler).

## Play satın alma ekranında "Bir şeyler ters gitti" (02.10.2026 — cihazda düzeltildi ve doğrulandı, kod değişmedi)

"Pro'ya geç" düğmesine basınca test kartlı ödeme ekranı yerine Play'in kendi penceresinde
"Hata — Bir şeyler ters gitti. Lütfen tekrar deneyin." çıkıyordu.

**Play'in gerçek cevabı** pencere "Anladım" ile kapatılınca geliyor:

```
W/ProxyBillingActivity: Activity finished with resultCode 3 and billing's responseCode: 5
W/BillingManager: Satın alma hatası: 5 Expired Product details. Please fetch product details
                  again and use it to retry the call.
```

Kod 5 = `DEVELOPER_ERROR`. Yani Play, uygulamanın elindeki ürün bilgisini "süresi dolmuş"
sayıyor — bilgi 13 saniye önce sorulmuş olsa bile.

**Neden (düzeltmeyle doğrulandı):** cihaz saati 24-25 Eylül'de seri
denemeleri için elle ~24 kez değiştirilmiş, en ileri **16.10.2026**'ya, bir kez de geriye
(01.09) alınmış; son geri dönüş 25.09 21:06. Kanıt: `adb shell dumpsys time_detector` →
`Set system clock … cause=Manual time suggestion` satırları. Saat ilerideyken Play ürün
bilgisini önbelleğine almış; şimdi o bayat bilgiyi vermeye devam ediyor, sunucu da reddediyor.
Bilinen bir durum: RevenueCat topluluğunda aynı mesaj için "cihaz tarihi ileri alındıktan
sonra oluyor, cihaz o tarihe ulaşana kadar sürüyor; Play Store önbelleğini temizleyip cihazı
yeniden başlatmak düzeltiyor" deniyor.

Destekleyenler:

- Bu kurulumda son başarılı satın alma 21.09 (Analytics `_ltv_TRY` zaman damgası) — saat
  oynanmadan önce.
- Ürün bilgisi sorgusu ~50 ms'de dönüyor; ağdan değil, Play'in önbelleğinden.
- Her hatadan sonra `Finsky: Commerce cache was cleared.` yazıyor ama 10 dakika sonraki yeni
  sorgu yine bayat geliyor; temizlenen, sorunlu önbellek değil.
- Satın alma kodu 14.09'dan beri değişmedi. Play durum panosunda arıza yok.

**Elenenler:**

- *Yanlış hesap / lisans testçisi:* Play hem mağazada hem bu uygulamanın faturasında …2003
  hesabını kullanıyor (`Finsky: com.numigo.app: Account from first account`; loglardaki hesap
  özeti `base64url(sha256(hesap adı))`, bu yolla eşlendi). Telefondaki …tumturk2 hesabının
  oturumu bozuk (`BAD_AUTHENTICATION`), ama satın almada kullanılmıyor.
- *Ekran kilidi (PIN) yok:* telefonda kilit yok ve Play her satın almada
  `AuthService: canAuthenticate … result: 11` alıyor; ilk aday buydu. Yanlış çıktı: kilit
  kurulmadan, yalnızca önbellek temizliğiyle abonelik alınabildi.

**Düzeltme (cihazda, kullanıcı yaptı):** Ayarlar → Uygulamalar → Google Play Store →
*Önbelleği temizle*, ardından telefonu yeniden başlatmak. Hangisinin yettiği ayrıştırılmadı:
23:31'deki deneme hâlâ `Expired Product details` verdi ama o an önbellek temizlenmiş miydi
bilinmiyor. Tekrar olursa ve bu yetmezse sıradaki adım *Verileri temizle* (hesaplar silinmez,
Play Store ayarları sıfırlanır); hiçbir şey yapılmasa 16 Ekim'den sonra kendiliğinden
düzelmesi beklenirdi.

**Doğrulama (yeniden başlatmadan sonra, 23:45):**

```
23:45:15 BillingManager: OFFERS pro_monthly …                ← yeni süreç, yeni ürün bilgisi
23:45:20 START ProxyBillingActivity → Play ekranı            ← 1. akış: hata/iptal satırı yok
23:45:51 START ProxyBillingActivity → Play ekranı            ← 2. akış (PlanFragment'ten)
23:45:54 Finsky: Monetization gRPC call …                    ← kullanıcı onayladı
23:45:56 Finsky: Commerce cache was cleared.
23:45:56 Finsky: Applying library update: account=[…2003]    ← satın alma Play'e işlendi
23:46:03 BpBinder … IInAppBillingService code=902 (1095 ms)  ← uygulamanın onay çağrısı
23:46:03 Finsky: Applying library update: account=[…2003]
```

23:45:15'ten sonra `BillingManager` / `ProxyBillingActivity` etiketlerinde tek bir uyarı ya da
hata yok. Sunucu doğrulaması (`redeemGooglePlaySubscription`) doğrudan görülmedi, çıkarım:
uygulama onay çağrısını (902 = `acknowledgePurchase`) yalnızca sunucu "tamam" dedikten sonra
yapıyor ve hata dalları log yazıyor. Hatalı denemelerde aynı yerde tek bir gRPC çağrısı ve
~350 ms sonra `Commerce cache was cleared` vardı. 1. akışın ayrıntısı (hangi ürün) tampondan
düşmüştü.

**Ders:** cihaz saatini oynamak yalnızca seriyi değil, Play satın almalarını da haftalarca
bozuyor. Seri için zaten yasaktı (bkz. "Saati ileri alarak deneme").

**Koda dair, yapılmadı:** `BillingManager` ürün bilgisini süreç ömrü boyunca bellekte tutuyor
ve bu hatadan sonra yeniden sormuyor. Bu olayda yeniden sormak işe yaramazdı (Play aynı bayat
bilgiyi veriyordu); ama gerçek kullanıcıda uygulama günlerce bellekte kalırsa aynı hata
çıkabilir. Sağlamlaştırma: `DEVELOPER_ERROR` gelince `queryProductDetails()` çağırmak.
Cihazda denenemediği için kullanıcıya soruldu, beklemede.

**Bir dahaki sefere teşhis sırası:** önce hata penceresini "Anladım" ile kapattırıp
`adb logcat -s BillingManager:V ProxyBillingActivity:V` ile sonuç kodunu ve mesajını oku —
bu olayda cevap o tek satırdaydı, ondan önceki adaylar (hesap, ekran kilidi) boşa çıktı.
Play'in kendi satırları (`Finsky`, `AuthService`) için filtresiz kayıt gerekir; ana tampon bu
cihazda ~4-5 dakikada dönüyor. Yeniden başlatma `log.tag.*` ayarlarını sıfırlar (`FA-SVC`
ayrıntılı logu dahil).

## Küçük düzenlemeler (03.10.2026 — kişileştirme fiyatını kullanıcı cihazda denedi; diğerleri için ayrıca bir sorun bildirmedi)

- **Yarış dersi panelindeki "BAŞLAT" düğmesinden yıldırım ikonu kaldırıldı**
  (`LessonAdapter.showRaceLessonBottomSheet`). `R.drawable.lighting__1_` artık hiçbir yerde
  kullanılmıyor; dosya silinmedi.
- **Sandık (CHEST) türü derslerde boncuk animasyonu diğer derslerle aynı hıza çekildi:**
  50 ms → 300 ms. `AbacusFragment` (`BEAD_ANIMATION_MS`, beş yer) ve
  `BlindingLessonFragment.setupAbacusController` (denetleyicinin varsayılanı). Dikkat: sandık
  derslerinde puan süreye de bağlı (`calculateChestScore`); boncuk başına 250 ms'lik fark
  sürelere yansıyabilir, cihazda bakılmadı.
- **Atlama düğmesi (`skipStepButton`) de sayıyı bir an siliyor** (01:12'de kuruldu,
  DENENMEDİ). `BlindingLessonFragment`'te sayılar kendiliğinden geçerken ekran 200 ms boş
  kalıyor; art arda iki sayı aynıysa geçiş böyle anlaşılıyor. Düğme ise sıradaki sayıyı
  doğrudan yazıyordu. Artık ikisi aynı yoldan gidiyor (`skipToNextSequenceNumber` →
  `onShowNextNumberStep`). Boşluk sırasındaki basış yok sayılıyor (`sequenceBlankPending`):
  boşluğu yeniden başlatsaydı hızlı basışlarda sıradaki sayı hiç gelmezdi. Rehberin zorunlu
  tıklaması da aynı fonksiyonu kullanıyor.
- **Anahtarla satılan iki boncuğun (ANIMAL3, ANIMAL8) renk kişileştirmesi de 40 anahtar**
  (kullanıcı cihazda denedi: "sorunsuz çalıştı"). Diğer boncuklarda eskisi gibi 2000 altın.
  `AbacusCustomizationFragment`: fiyat ve para birimi `getBeadColorFeaturePrice` /
  `isKeyPricedBead`'den geliyor; hem Renk sekmesindeki kilit katmanı (etiket + ikon,
  `tab2ColorLockIcon`) hem satın alma paneli (`showColorFeaturePurchasePanel`) bunu
  kullanıyor. Sunucuda değişiklik gerekmedi (harcama `updateUserWallet`'tan serbest geçiyor).
  Aynı panelde eksik olan geri iade de eklendi: ücret düşüp özellik kaydedilemezse
  (`setColorFeatureActive` hatası) tutar artık iade ediliyor — altın için de.

## Saati ileri alarak can doldurma (03.10.2026 — cihazda denendi; son küçük sağlamlaştırma kurulmadı, commit edilmedi)

Kullanıcı, çocukların cihaz saatini ileri alarak canlarını doldurabildiğinden şüphelendi.
Koddan teyit edildi (cihazda denenmedi — saatle oynamak seriyi ve Play satın almalarını
bozuyor):

- Can sunucuda (`energy_full_time`: canın dolacağı an) ve istemci yazımına kapalı; sunucu
  kendi saatiyle hesaplıyor, harcamayı gerekirse `Yetersiz can` ile reddediyor.
- Ama "şu an kaç can var" sorusunu istemci, o değeri CİHAZ saatiyle karşılaştırarak
  cevaplıyordu (`EnergyManager.getCurrentEnergy`). Saat ileri alınınca can dolu görünüyor,
  `hasEnoughEnergy` geçiyor ve ders başlıyor: `useEnergy` iyimser, sunucunun cevabını
  beklemiyor. Sunucu reddedince yalnızca yeniden eşitleme yapılıyor, o da bir şey değiştirmiyor
  (cihaz saati hâlâ ileride). Sonuç: sınırsız ders.

**Düzeltme (yalnızca istemci, deploy yok):** can hesabındaki "şimdi" artık cihazın duvar
saati değil, `TrustedClock.nowMs()`: sunucudan öğrenilen an + o zamandan beri MONOTON saatle
(`SystemClock.elapsedRealtime`) geçen süre. Sunucu saati zaten alınıyordu
(`SeasonClock.refreshFromServer` → `getSeasonInfo`, her açılışta ve saatte bir);
`SeasonClock.onServerTime` aynı cevabı `TrustedClock`'a da veriyor. Çapa diske yazılıyor
(`trusted_clock` tercihleri) ve açılış sayacıyla (`BOOT_COUNT`) hangi açılışa ait olduğu
tutuluyor. Hesabın kendisi `TrustedClockRules.trustedNow` (Android'siz, 8 birim testi).
Çapa değişince `EnergyManager` göstergeyi hemen tazeliyor.

Yan fayda: saati yanlış olan dürüst cihazlarda can artık doğru anda doluyor.

**Kapatmadığı (bilerek):** çapa yokken — cihaz yeniden başlatıldı VE internet yok — duvar
saatine dönülüyor. "Saati ileri al, yeniden başlat, uçak modunda oyna" hâlâ mümkün. Tamamen
kapatmak, ders başlatmayı sunucu onayına bağlamak demek (internetsiz ders açılamaz, her
başlatış ~0,5-1 sn gecikir); kullanıcıya soruldu.

**Cihazda denendi (01:33–01:35, kullanıcı saati ~1 saat ileri aldı: "sorunsuz çalıştı"):**

```
01:33:03.011 TrustedClock: capa kuruldu | cihazSaatiFarki=299ms
01:33:55.612 TrustedClock: capa kuruldu | cihazSaatiFarki=375ms          ← yeni süreç
02:34:10.092 TrustedClock: capa kuruldu | cihazSaatiFarki=3556005ms      ← saat +59 dk ilerideyken
```

Son satırın damgası cihazın (ileri alınmış) duvar saati; gerçek an 01:34:54. Uygulama saat
ilerideyken yeniden açılmış, sunucu cevabı farkı (59 dk 16 sn) yakalamış; kullanıcı canın
dolmadığını gördü. Etiketlerde hata ya da çökme satırı yok. Saat sonra otomatiğe döndü.

**Bir şeyi bozdu mu — koddan gözden geçirildi, sorun bulunmadı:**

- Cihaz saati doğruysa sonuç eskisiyle aynı (fark: sunucu cevabının gecikmesi, ~0,3 sn,
  hep geriye doğru; can en fazla o kadar geç dolar).
- Oturum açılmamışken sunucu saati istenmiyor, çapa kurulmuyor: misafirde eski davranış.
- Canı değiştiren sunucu cevapları (`spendEnergy`, `claimAdEnergy`, `buyEnergyWithKeys`)
  aynen benimseniyor; yalnızca "şimdi" değişti. Üst sınır kırpması (`getFullTime`) artık
  sunucunun kırpmasıyla aynı saati kullanıyor.
- Can API'sini duvar saatiyle karıştıran başka yer yok (`ShopFragment`, `TasksFragment`
  göreli süreleri `EnergyManager`'dan alıyor). Sezon saati (`SeasonClock.nowUtcMs`), görevler
  (`MissionsProgressStore`) ve seri kendi saatlerini kullanmaya devam ediyor; dokunulmadı.
- `EnergyManager` tek yerde (MainActivity) kuruluyor ve `destroy()`'da dinleyicisini
  bırakıyor.

Gözden geçirmede bulunan ve kapatılan zayıflık (derleniyor, 8 birim testi; cihaza
KURULMADI — telefon o sırada bağlı değildi): açılış sayacı okunamazsa (0) diskteki çapanın
bu açılışa ait olduğu kanıtlanamıyor. Cihaz yeniden başlatılıp eskisinden uzun süre açık
kalmışsa çapa geçerli sanılır, "şimdi" gerçeğin gerisinde kalır ve can internet gelene kadar
geç dolardı. Artık o durumda diskteki çapa yalnızca alt sınır. Bu cihazda `boot_count`'un
okunup okunmadığına bakılmadı (`adb shell settings get global boot_count`).

Testin kendisinden kalabilecek iz: uygulama saat ilerideyken açıldığı için Play ürün
bilgisini o saatle önbelleğe almış olabilir. Satın alma ekranı "Bir şeyler ters gitti"
derse çözüm aynı (Play Store önbelleği); fark 1 saat olduğu için kendiliğinden de geçer.

`MissionsProgressStore.trustedNowMs` ve `SeasonClock.nowUtcMs` kendi yöntemleriyle duruyor;
`TrustedClock`'a taşınmadılar.

## Hesabını silen öğrencinin soruları (03.10.2026 — kod ve canlı loglar incelendi, veri İNCELENMEDİ)

Kullanıcı, `questions` koleksiyonunda verilerin durduğunu gördü ve hesap silinince soruların
silinip silinmediğini sordu.

- **Kod siliyor:** `cleanupUserOnDelete` (Auth `onDelete` tetikleyicisi) → `deleteUserQuestions`:
  `questions` içinde `studentUid == uid` olan her soruyu `messages` alt koleksiyonu ve
  Storage medyasıyla (`mediaStoragePath`, `videoStoragePath`, `screenshotStoragePath`)
  birlikte siliyor. Bekleyen (`status == 'pending'`) bir `messageReports` kaydı olan soru
  kanıt olarak KORUNUYOR (gizlilik politikasında beyan edilmiş). Kod `a7bf88d` (06.09.2026).
- **Canlıda da var:** `firebase functions:log --only cleanupUserOnDelete` çıktısında bu
  adımın satırları görünüyor (`Silinen kullanıcının danışma soruları temizlendi { deleted,
  skippedForReport, failed, mediaFailed }`). Görülen dört silmede (24.09) hepsi `deleted: 0`:
  silinen hesapların sorusu yoktu. Log saklama süresi ~30 gün; öncesi görülemiyor.
- **Kalan soruların olası sebepleri** (hangisi olduğu veriye bakılmadan bilinemez): soru
  sahibinin hesabı hâlâ duruyor; hesap, silme kodu canlıya çıkmadan önce silindi; soruda
  bekleyen bir şikâyet var; silme hata verdi (`failed`/`mediaFailed` > 0).
- **Ayırt etmek için:** Firebase konsolunda kalan bir sorunun `studentUid`'ini
  Authentication'da ara. Yoksa sahipsiz; bekleyen şikâyeti de yoksa tek seferlik bir
  temizlik betiği gerekir (yazılmadı; çalıştırmak için yönetici kimliği — GitHub Actions'taki
  `FIREBASE_SERVICE_ACCOUNT` gibi — gerekiyor, kullanıcı onayı şart).
- Öğretmen hesabını silerse öğretmenin cevapladığı sorular silinmiyor (öğrenciye ait).

Silmede ele alınmayan koleksiyonlar ve şişme incelemesi aşağıdaki bölümde.

## Firestore şişme ve silmede kalan veri incelemesi (03.10.2026 — yalnızca okundu, kod değişmedi)

Canlı veritabanı salt okunur sorgularla ölçüldü: belge SAYILARI (`count()` toplaması) ve
belge/alan BOYUTLARI (alan adları ve bayt; değerler yazdırılmadı). Betikler oturum
klasöründe: `firestore_sayim.js`, `firestore_boyut.js` (firebase CLI oturumuyla REST).

**Bugün şişmiş bir şey yok.** 16 üst seviye koleksiyon, toplam ~1.000 belge, en büyük belge
~1,3 KB (1 MiB sınırından çok uzak). Sayılar: `items` (ders ilerlemesi) 675,
`processedPurchases` 81, `messages` 55, `lessonSuccessRateState` 31, `messageReports` 21,
`text` 15, `adRewards` 14, `questions` 10, `cupHistory` 8, `feedback` 6, diğerleri ≤ 5.
4 kullanıcı var, yani bunlar büyüme hızı hakkında fikir veriyor, ölçek hakkında değil.
TTL politikaları canlıda açık: `otpRateLimits`, `otpWrongAttempts`, `studentVerificationCodes`
(`firebase firestore:indexes`). Sezon liderlik tabloları sezon kapanınca siliniyor
(`seasonLeaderboardFinalize.deleteLeaderboardBoard`).

**Kullanıcı sayısıyla sınırsız büyüyenler:**

| Koleksiyon | Büyüme | Temizlik | Değerlendirme |
|---|---|---|---|
| `adRewards` | izlenen her ödüllü reklam için bir belge (~150 B) | YOK, TTL yok | En hızlı büyüyen. 10 bin günlük aktif × 5 reklam ≈ günde 50 bin belge. Kullanıldıktan sonra yalnızca tekrar oynatmaya karşı kısa süre gerekli. Öneri: oluştururken `expireAt = createdAt + 7 gün`, o alana TTL (sunucu + index deploy'u). |
| `cupHistory` | kullanıcı başına aktif gün başına bir belge (~100 B) | yok | Önemsiz; okuma zaten tarih penceresiyle. |
| `users.dailyTimeSpent` | gün başına bir anahtar | yazılan günün 14 gün öncesi siliniyor | Aktif olunmayan günlerde budama atlanıyor, çok yavaş sızıntı; önemsiz. |
| `questions` + `messages` | soru/mesaj başına | yanıtlanan soruların MEDYASI 30 gün sonra Storage'dan siliniyor, belgeler kalıyor | Saklama süresi bir politika kararı. |
| `processedPurchases` | satın alma başına | yok | Kalmalı: tekrar kullanım koruması ve iade geri alımı. |
| `messageReports` | şikâyet başına | yok | Küçük; inceleme kaydı. |

**Hesap silinince kalan kişisel veri** (`cleanupUserOnDelete` yalnızca `users/{uid}` ağacını,
`questions`'ı ve liderlik kayıtlarını siliyor; `publicProfiles` aynalama tetikleyicisiyle
gidiyor):

- `friendRequests` (2 belge): gönderen/alan ADLARI var. Kod hiç kullanmıyor (kurallar
  `if false`, istemci ve sunucuda referans yok) — ölü koleksiyon, elle silinebilir.
- `feedback` (6): e-posta, kullanıcı kimliği, mesaj, cihaz modeli.
- `ratingFeedback` (3): kullanıcı kimliği + yıldız + metin.
- `messageReports` (21): şikâyet eden ve şikâyet edilen kimlikleri, medya bağlantısı.
- `adRewards` (14): kullanıcı kimliği (TTL önerisi bunu da çözer).
- Bilinçli kalabilecekler: `processedPurchases` (iade/yasal), `welcomeCreditGrants`
  (cihaz başına kredi kötüye kullanımı), `creditRefundAudit` (denetim). Geçiciler TTL'li
  ya da boş: `studentVerificationCodes`, `otp*`, `pendingRegistrations`, `teacherInvites`.

Karar kullanıcıda (gizlilik politikasıyla karşılaştırılmadı). Her öneri sunucu değişikliği,
yani deploy demek (bkz. "Push = deploy").

### Kullanıcı kararları ve yapılanlar (03.10.2026 — yazıldı, emülatörde test edildi, DEPLOY EDİLMEDİ)

- **`adRewards` 7 gün sonra silinir:** `admobRewardCallback` kayda `expireAt` (oluşturma + 7
  gün, `AD_REWARD_RECORD_RETENTION_MS`) yazıyor; `firestore.indexes.json`'da `adRewards.expireAt`
  TTL'i (indekssiz). Güvenli: hak 24 saatte bozuluyor, 1 saatten eski callback reddediliyor.
  Mevcut 14 kayıtta `expireAt` yok, TTL onlara dokunmaz (elle silinebilir; önemsiz).
- **Kapanan sorular 30 gün sonra TAMAMEN silinir** (medya + mesajlar + soru): eskiden yalnızca
  medya siliniyordu. `runClosedQuestionCleanup` — `cleanupResolvedQuestionMedia` adlı günlük
  görevin içinde (ad değiştirilmedi: etkileşimsiz deploy, adı değişen fonksiyonun eskisini
  silmek için onay isteyip yarıda kalırdı). `resolved` (`resolvedAt` > 30 gün; daha önce
  yalnızca medyası silinmişler dahil) ve `expired` (`creditRefundedAt` > 30 gün). Açık
  sorulara ve bekleyen şikâyetli sorulara dokunulmuyor. Yeni indeks: `questions`
  (status, creditRefundedAt). Sorgular bağımsız (`Promise.allSettled`): indeks oluşmadan
  çalışırsa yalnızca `expired` sorgusu düşer. Soru silme adımı hesap silmeyle ortak
  (`deleteQuestionCompletely`, `hasPendingQuestionReport`).
- **Testler:** `functions/scripts/test-closed-question-cleanup.js` (yeni) ve
  `test-delete-user-questions.js` — ikisi de emülatörde geçiyor:
  `firebase emulators:exec --only firestore --project numigo-new "node functions/scripts/test-closed-question-cleanup.js && node functions/scripts/test-delete-user-questions.js"`.
  DİKKAT: `emulators:exec` ortama gerçek bucket adını ve CLI kimliğini koyuyor; ilk
  çalıştırmada medya silme çağrıları CANLI Storage'a gitti (yollar sahte `q/...`, gerçek
  dosyalar `question_media/…`, `question_screenshots/…`, `question_videos/…` altında — hiçbir
  şey silinmedi). İki betik artık `FIREBASE_CONFIG` ve `GOOGLE_APPLICATION_CREDENTIALS`'ı
  kendisi siliyor. Emülatör süreci bazen kapanmıyor (8080 meşgul) — `cloud-firestore-emulator`
  java sürecini öldür.
- **Gizlilik politikası** (`public/privacy-policy.html`, yürürlük tarihi 3 Ekim 2026): kapanan
  danışmaların 30 gün sonra silindiği ve reklam ödülü kayıtlarının 7 gün tutulduğu yazıldı.
  Hosting ayrı deploy ediliyor (iş akışı yok): `firebase deploy --only hosting`.
- **`feedback` / `ratingFeedback`:** uygulama ve sunucu yalnızca YAZIYOR, hiçbir yer okumuyor —
  geliştirici konsoldan okuyor. Öneri (yapılmadı): hesap silinince silmek yerine kimliksizleştirmek
  (e-posta/uid kalksın, mesaj/puan kalsın).
- **`messageReports`:** kalması teknik sorun değil (küçük); kişisel veri olarak silinen
  kullanıcının kimliğini ve medya bağlantısını tutuyor. Öneri (yapılmadı): incelenmişleri
  kimliksizleştirmek; acil değil.
- **`friendRequests`:** kullanıcı kendisi sildi (`firebase firestore:delete friendRequests
  --recursive`, 2 belge). Kurallardaki `match /friendRequests` bloğu duruyor (zararsız).
- **Gizlilik politikası canlıda** (kullanıcı `firebase deploy --only hosting` yaptı, 03.10.2026).
  DİKKAT: politika "kapanan danışmalar 30 gün sonra silinir, reklam ödülü kayıtları 7 gün
  tutulur" diyor ama bunu yapan sunucu kodu (yukarıdaki iki madde) henüz deploy edilmedi —
  commit + push (= deploy) bekliyor.

## Yakında yapılanlar — tekrar etmeyin

- **Sandık kabı gizleniyordu (`3722b05` regresyonu, cihazda doğrulandı):** `MainActivity`'deki
  back stack dinleyicisi (466) yalnızca `abacusFragmentContainer`'a bakıyordu. LessonResult →
  ChestFragment geçişinde abacus kabı boşalınca "ders kapandı" sanıp
  `restoreMapUiAfterLessonOverlayDismiss` ile sandığın kabını `GONE` yapıyordu: sandık hiç
  görünmüyor, ders ilerlemesi yazılmıyor, `LessonResult.onDestroyView` çalışmadığı için chrome
  kilidi `depth=2`'de kalıyordu. Dinleyici artık `resultFragmentContainer`'daki canlı overlay'i
  de sayıyor. Belirleyici log satırı: `backStackChanged->restoreMapUi SCHEDULED |
  caller=backStackChanged.topOverlayNull` (etiket `FirstTutorialDbg`).
- **`MapFragment` chrome kilidi çapraz eşleşmesi:** `mainActivityViewsLocked` bayrağı eklendi;
  `disableMainActivityViews` kilidi en fazla bir kez alıyor, `release` `enableMapFragmentViews`'tan
  `enableMainActivityViews`'a taşındı. `disableMainActivityViews` bayrak set olsa bile sayaç 0 ise
  yeniden acquire ediyor — güvenlik ağı sayacı sıfırladığında bayrak bayat kalıyor ve bu kontrol
  olmadan sonraki kilitler atlanıyordu (cihazda bu yol gerçekten çalıştı).
- `be6cb48` **Seri donması:** `last_goal_day` bir günden fazla ileride ise (cihaz saati ileri
  alınarak test edilmişti) `refresh()` artık o değeri atıyor. Saat dilimi payının meşru sınırı
  ±1 gün; sunucu da `STREAK_DAY_TOLERANCE_DAYS = 1` kullanıyor.
- `5ef1b37` + `3ac8615` **StreakDiag:** seri zincirinin beş halkası loglanıyor.
- `3722b05` **Hayalet overlay:** `FragmentManager.findFragmentById` ekli olmayan, yalnızca bir
  back stack girişinin tuttuğu fragment'ı da döndürüyor. `MainActivity.liveOverlayIn` (1878)
  `isAdded` filtresi koyuyor ve "overlay açık mı?" kararı veren bütün yerler ona geçirildi.
  Ayrıca `TutorialFragment` "Eğitimi atla" yolunda `activeMapTutorialOverlayFromLesson`
  sıfırlanmıyordu; düzeltildi.
- `4c1a77d` **`submitStreakDay` TDZ:** günsüz "buradayım" bildirimi her seferinde `INTERNAL`
  atıyordu (`const` geçici ölü bölge). Düzeltildi ve deploy edildi.

## Bekleyen deploy

**Push = deploy (03.10.2026'da fark edildi):** `.github/workflows/deploy-functions.yml`,
`functions/**` değişikliği içeren her push'ta (`claude/**` dalları; çalışma dalı dahil)
`firebase-tools deploy --only functions` çalıştırıyor — yani seçici değil, BÜTÜN
fonksiyonlar gidiyor. Cloud Functions denetim kayıtlarında deploy'lar
`github-actions-deploy@numigo-new.iam.gserviceaccount.com` adına (ör. 2026-10-01T19:04Z).
"Deploy etmeden önce sor" anlaşması bu yüzden functions/ dokunan commit'lerin PUSH'unu da
kapsıyor. Aşağıdaki "yalnızca şu iki fonksiyon deploy edildi" ifadesi muhtemelen yanlıştı:
o push hepsini göndermiş olmalı.

Bekleyen deploy yok.

`claimCupPathChest` (kupa yolu sandık enderliği) kullanıcı tarafından deploy edildi; bunu
02.10.2026'da kullanıcı bildirdi, deploy zamanı Firebase konsolundan ayrıca **doğrulanmadı**.

`submitStreakDay` ve `buyStreakFreeze` 01.10.2026'da deploy edildi (seri dondurma).

`submitStreakDay`'in deploy edildiği **çıkarım** yoluyla saptandı, doğrudan görülmedi: seri
dokümanında `lastSeenAt` (yalnızca `reminderPatch` yazıyor, iki dalda da) `updatedAt`'ten
(yalnızca gün taşıyan transaction yazıyor) daha yeniydi — yani günsüz "buradayım" dalı
başarıyla çalışmış. Düzeltme öncesi o dal her seferinde çöküyordu. Emin olmak istersen
Firebase konsolundan fonksiyonun son deploy zamanına bak.

**Deploy etmeden önce kullanıcıya sor.**

## Teşhis logları

| Etiket | Ne gösterir |
|---|---|
| `StreakDiag` | Günlük seri zinciri: süre sayımı, `refresh()` dalları, kuyruk, sunucu gidiş/dönüş, ekran; seri dondurma için `Repo.dondurma` (`SATIN_ALINDI` / `HARCANDI`) |
| `MapTouchDbg` | Harita dokunma yüzeyi: overlay host'ları (`ekli=` alanı hayaleti ayırt eder), scrim, blocker'lar, `fmBackStack`, `chromeLockDepth` |
| `ChromeBlockerDbg` | `acquire`/`release` zinciri ve derinlik; `STUCK_CHROME_LOCK?` |
| `LessonProgressDiag` | Ders ilerlemesi, `LessonResult.claimButton` dalları (`SKIP_TO_MAP` / `GO_TO_CHEST`) |
| `LessonProgress` | `updateLessonItem` → Firestore yazımı |
| `PostLessonQueue` | Ders sonrası ekran kuyruğu: kilit, zemin, `bekliyor \| block=...`. Görevler'e dönüş (kupa testi, günlük soru) de burada: `gorevler yeni seri SIRADA / GEREKMIYOR / ACILIYOR / DUSURULDU`, `gorevler dokunma engeli ACIK / KAPALI / SAHIPSIZ` |

Hepsini birlikte izlemek için:

```powershell
adb logcat -c
adb logcat -s StreakDiag:V MapTouchDbg:V ChromeBlockerDbg:V LessonProgressDiag:V LessonProgress:V PostLessonQueue:V TouchDiag:V
```

**Dikkat:** `logMapTouchDiag`'ın etiketi `MapTouchDbg` (`MapTouchDiag` değil). Bir kez yanlış
etiket filtrelendiği için "log yok" sanıldı ve bir tur kaybedildi.

Cihazdaki seri durumunu loga hiç gerek kalmadan okumak için:

```powershell
adb shell run-as com.numigo.app cat /data/data/com.numigo.app/shared_prefs/streak_prefs.xml
adb shell run-as com.numigo.app cat /data/data/com.numigo.app/shared_prefs/study_time.xml
```

## Yayın öncesi

`docs/YAYIN_ONCESI_KONTROL.md` tek kaynak. Orada üç test anahtarı var; `StreakDiag.ENABLED`
**`BuildConfig.DEBUG`'a bağlı değil**, elle `false` yapılmalı.

## Derleyemeyen oturumun doğrulama yöntemleri

Bu oturum derleyebiliyor, yani asıl doğrulama `.\gradlew compileDebugKotlin`. Yine de depoda
bunlar kullanılıyor ve korunmalı:

- `functions/scripts/test-*.js` — sunucu mantığını kaynaktan türetip kilitleyen testler
  (`node functions/scripts/test-chest-rarity-table.js` gibi; tablo kopyalanmıyor, kaynaktan okunuyor)
- `tools/rules-test/` — Firestore kuralları, emülatörle 22 test

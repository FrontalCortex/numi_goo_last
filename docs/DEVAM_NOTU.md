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

## Saati ileri alarak can doldurma (03.10.2026 — cihazda denendi; son sağlamlaştırma commit edildi ve kuruldu, ayrıca denenmedi)

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

Gözden geçirmede bulunan ve kapatılan zayıflık (8 birim testi; giriş ekranı yenilemesiyle
birlikte cihaza kuruldu, kendi başına denenmedi): açılış sayacı okunamazsa (0) diskteki çapanın
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
- **Deploy edildi (03.10.2026 ~09:56):** `0846e58` push'u iki iş akışını tetikledi, ikisi de
  başarılı: "Deploy Cloud Functions" (bütün fonksiyonlar) ve "Deploy Firestore & Storage
  Rules" (kurallar + indeksler + TTL). Politika ile sunucu artık uyumlu. İlk gerçek tarama
  `cleanupResolvedQuestionMedia` günlük görevinin bir sonraki çalışmasında; sonucu
  `firebase functions:log --only cleanupResolvedQuestionMedia` → `runClosedQuestionCleanup
  tamamlandı { … }` satırında görülür (bakılmadı).
- **Ayrı sorun:** "Build Debug APK" iş akışı en az 01.10'dan beri HER push'ta "Setup Android
  SDK" adımında (`android-actions/setup-android@v3`) başarısız. Yerel derleme sağlam;
  CI ortamı sorunu. Kayıtlar kimlik doğrulama istiyor, `gh` bu makinede yok. Ayrı göreve
  bırakıldı.

## Giriş başlangıç ekranı yenilendi (03.10.2026 — kullanıcı cihazda denedi, sorun bildirmedi; commit edilmedi)

Kullanıcı `LoginStartActivity`'yi bir örnek görsele göre değiştirmek istedi: büyük çizim,
başlık + motivasyon cümlesi, dolu "Başla" düğmesi, altında "Zaten hesabım var" bağlantısı.

- **Düzen (`activity_login_start.xml`):** sağ üstte öğretmen/öğrenci geçişi (örnekte yoktu,
  kullanıcı sağ üstü önerdi) → maskot (önce dinozor Lottie, sonra robot, su aygırı, en son tavşan; genişliğin
  %70'i, en fazla 320dp, kare) → başlık (28sp) → metin (17sp, ikincil renk) → "Başla"
  (56dp, `dark_primary`) → "Zaten hesabım var" (mavi metin bağlantısı). Örnekteki sayfa
  noktaları eklenmedi (tek sayfa). Maskot geçici seçim; başka bir Lottie/görselle
  değiştirilebilir.
- **Kimlikler:** `tvQuestion`→`tvTitle`, `tvSubtitle`→`tvBody`, `btnSecondary`→`btnStart`
  (kayıt, `showUserInfoFragment`), `btnPrimary`→`btnLogin` (giriş), `divider` kalktı,
  `mascotView` eklendi. Davranış aynı; yalnızca ana eylem artık kayıt.
- **`setMainContentVisible`** tek tek görünüm yerine `loginStartContent` katmanını gizliyor
  (maskot gizlenince kendisi duruyor).
- **Metinler:** öğrenci "Zihnini Güçlendir" / "Abaküsle oynayarak zihinden hesaplamayı öğren,
  daha hızlı düşün ve matematikte kendine güven."; öğretmen "Öğrencilerine Yol Göster" /
  "Öğrencilerinin sorularını yanıtla, abaküsle ilerlemelerine destek ol."; düğmeler "Başla",
  "Zaten hesabım var". Eski `login_start_primary_button` / `secondary_button` silindi.
- **Maskot → tavşan (`BunnyMascotView.kt` + `BunnyMascotArt.kt`, kuruldu, cihazda denendi;
  sorunsuz):** su aygırı da "eksik" bulundu. Kullanıcı Freepik "Animal Collection"
  çizimini `res/drawable/animal_maskot.xml` olarak ekledi (altı hayvan + yazı, 247 şekil;
  APK'ya girmesin diye sonra `design/maskot/animal_maskot.xml`e taşındı).
  Tavşanın şekilleri (2 ve 40–76 numaralı yollar) bir betikle `BunnyMascotArt.kt`'ye
  aktarıldı; parça → yol eşlemesi o dosyanın başında. Betikler (`parse.js`, `gen.js`,
  önizleme `proto*.js`) oturumun scratchpad'inde, depoda değil. View parçaları kendi dönme
  noktalarında oynatıyor: nefes, kafa eğme, göz kırpma, bakış, kulak oynatma; `greet()`
  zafer işaretli kolla selam, ağız KEDİNİN açık mutlu ağzı (konuşma yok); `cheer()` iki
  zıplama, KEDİNİN dolgun kolları tavşan rengine boyalı, yana ve yukarı açık (sol 32°,
  sağ 18° — kedinin kolları asimetrik), zafer işareti YOK, yanak kızarması YOK, gülen
  gözler, kedi ağzı. Kedi parçaları (78–81, 102–103) `BunnyMascotArt`'ta kedinin yerinde
  duruyor, view `CAT_DX = -213` ile kaydırıyor. Tavşanın kendi kolunu yana açmak
  çelimsiz duruyordu; yukarı kaldırmak kolu kocaman kafanın arkasına sokuyordu.
  Beklerken sağ kol, sol kolun aynası (özgün çizimde hep zafer işareti var), 2 birim sola
  ve 2 aşağı kaydırılmış (tam aynada omzun sivri ucu dışarıda kalıyordu; 7/5 kaydırma
  fazla içeride kaldı); kollar arası geçiş `saveLayerAlpha` ile. Gövde hareketi
  (kullanıcı "gövde ve kafa çok sabit" dedi): selamda gövde kalçadan ±3° sallanıyor, kafa
  geriden geliyor, öbür kol da sallanıyor (hepsi `peace` ile ağırlıklı); sevinçte
  `updateCheerBody`: yere değerken ezilme (ayak tabanından), havada uzama, inişten sonra
  sönen yaylanma, kafa gövdeyi 70 ms geriden izliyor (gecikmeli zıplama yüksekliği),
  gövde ±4° kıvrılıp kafa ters fazda. Kullanıcı sevinçte kafayı fazla buldu: kafa
  kıvrılması 3°→1,5°, gecikme 70→40 ms. Selamda iki ayak hafifçe sağa-sola açılıyor
  (bacaklar `LEG_LEFT`/`LEG_RIGHT` olarak ayrıldı; her biri kalçasından 5° dışa, 1 yana;
  önce tek ayağı öne atma denendi, kullanıcı iki ayağın açılmasını tercih etti, sonra
  kaymayı azalttı). SELAM ARTIK ZAFER İŞARETİYLE DEĞİL: sağdaki kedi kolu 32° kalkıp ±12°
  sallanıyor (46°'ye kadar kafanın arkasına girmediği önizlemede görüldü), sol kol sarkık.
  `PEACE_ARM` çizimde duruyor ama kullanılmıyor — kullanıcı onu ileride ayrı bir hâl
  (emote) için saklamak istedi.
  Uzamada kulaklar ve adımda ayak kesilmesin diye görünür alan 314 birim, üst pay 6.
  `autoGreetIntervalMs` (giriş ekranında 5 sn): beklerken kendiliğinden selam; sevinç
  sayacı baştan başlatıyor. Açık konu: Freepik lisansı (kaynak gösterme yeri, yayından önce).
  Diğer hâller aşağıdaki "Maskot hâlleri" bölümünde.
- **(Önceki deneme) su aygırı prototipi (`HippoMascotView.kt`, SİLİNDİ):**
  önce kodla çizilen bir "boncuk robot" yapıldı; kullanıcı animasyonu beğendi, görseli
  beğenmedi ve bir örnek çizim gösterdi (düz renkli, gri-yeşil, geniş burunlu su aygırı
  kafası). Robot silindi; yerine aynı tarzda, bütün bedeniyle duran bir su aygırı kodla
  çiziliyor (örnek kopyalanmadı, tarzı alındı). Kendi kendine nefes alır gibi iner-kalkar,
  göz kırpar, bakışını kaydırır, ara ara bir kulağını oynatır. Koddan: `greet()` (el sallar
  + konuşur; ekran açılınca 400 ms sonra), `talk(ms)`, `cheer()` (iki zıplama, kollar
  yukarı, gülen gözler, yanak kızarması, açık ağız, kıpırdayan kulaklar; dokununca).
  Beklerken ağız çizilmiyor (örnekteki gibi yalnızca dişler). Görünmezken
  (`onVisibilityAggregated`) kare çizmeyi bırakıyor. Çizim aynı geometriyle başsız Edge'de
  resme döküldü ve kontrol edildi (scratchpad `hippo.html`).
  Kullanıcıya önerilen kalıcı yol: animatöre Rive karakteri yaptırmak (durum makinesi:
  bekleme/konuşma/sevinç/üzüntü/düşünme); bu view'in tetikleme API'si aynı kalabilir.
  Ayrıca uygulamadaki hazır Lottie hayvanlarının ticari lisansı yayından önce kontrol
  edilmeli.
- **Gezinme çubuğu:** telefon (API 33) gece modunda; `values-night/themes.xml` çubuğu
  `message_topbar` (#111416) yapıyor ve ekranın zemininden kopuk duruyordu. `onCreate`'te
  Splash ile aynı şekilde `background_color`'a çekildi (kuruldu).
- Aynı sırada kullanıcı `activity_login.xml` ve `activity_register.xml`'de arka planı
  `dark_background`→`background_color` yaptı; kullanıcı bunların da aynı commit'e girmesini
  istedi. Commit henüz istenmedi ("ekleyeceğimiz şeyler var"). Kullanıcı başka düzenleri de
  düzenliyor (`fragment_user_info`, `activity_teacher_otp`, `fragment_offline`, …); commit
  öncesi CRLF→LF çevrilmeli.

## Maskot hâlleri ve deneme ekranı (03.10.2026 — kullanıcı cihazda denedi: "her şey sorunsuz"; commit edilmedi)

Kullanıcı bekleme/selam/sevinçten sonra 13 yeni hâl daha istedi (tablodaki önerilerin hepsi) ve
hepsini denemek için Görevler'e bir kart.

- **`BunnyMascotView` yeniden yazıldı:** `play(Emote)` + 15 hâl (`GREET, CHEER, BALLOON,
  SLEEP, POINT, PEACE, SAD, DANCE, THINK, CLAP, SURPRISED, SHY, WINK, HEART, ABACUS`); her
  biri süresi dolunca beklemeye dönüyor, `onEmoteFinished` çağrılıyor. `greet()`, `cheer()`,
  `autoGreetIntervalMs` aynı davranıyor (giriş ekranı değişmedi); `point(toLeft)` var.
- **Duruş kanalları (`Ch`):** hâl her karede kanal HEDEFLERİNİ yazıyor (kol görünürlükleri ve
  açıları, gövde/kafa, kulak, bakış, yanak, kaş, balon, abaküs), `cur`'da yumuşatılıyor →
  hâller arası geçiş kendiliğinden yumuşak. Hızlı salınımlar (`osc`) yumuşatılmadan
  ekleniyor (yumuşatılsa sönüyordu), hâlin zarfıyla girip çıkıyor. Göz/ağız türü ayrık.
- **Yeni parçalar (`BunnyMascotArt`, betikle üretildi):** domuzun balonu (118–120,
  144–146; renk aynen), köpeğin önde birleşik elleri + gövdeye düşen gölgeleri (201–207,
  tavşan rengine boyalı). Eller iki yana açılınca gölgeler gövde dışına taşıp leke
  bırakıyordu → açıldıkça siliniyor. Zafer işaretli kol (PEACE) artık kullanılıyor.
- **Kodla çizilenler:** üzgün kaşlar, ters ağız, "O" ağız, kapalı/gülen göz yayları, "Z"ler,
  düşünce balonu ve "?", alkış yıldızları, kalp, küçük soroban (ahşap çerçeve, 5 çubuk,
  boncuklar kayıyor). Düşünmede kedi kolu kafanın ÖNÜNDE çeneye gidiyor.
- Hepsi önizlemede (scratchpad `proto16/17`) sabit kare olarak kontrol edildi; HAREKET
  cihazda görülmedi.
- **Deneme ekranı:** `MascotPlaygroundFragment` + `fragment_mascot_playground.xml`; Görevler'de
  "Karakter Animasyonu" kartı (`mascot_animation`, mor) → `openAbacusContainerFragment`.
  Bekleme hep oynuyor, seçeneklerde yok; 15 çip (`ChipGroup`, tek seçim); hâl bitince seçim
  kalkıyor. Kapatma `finishTasksOverlayAnimated` (FeedbackFragment ile aynı).
- **Commit sonrası (kuruldu, commit edilmedi):** düşünce balonu ~1,5 kat büyüdü, beyaz +
  mavi kenar (kullanıcı "daha büyük ve belirgin" istedi); kalp artık `heart_ic` ikonu
  (`mutate()` ile kopya; boyut tuval ölçeğiyle, sınırlar sabit 100 — tam sayı sınırlar
  atışta titretiyordu).
- **Mesajlaşma hâli (`CHAT`, döngülü; kuruldu, cihazda görülmedi, commit edilmedi):**
  `Emote.loops` eklendi (süre dolunca bitmiyor, baştan dönüyor; görünmezken geçen turlar
  atlanıyor). Tavşan köpeğin elleriyle telefon tutuyor (kodla çizilen telefon, ekranda
  sohbet satırları). 6 sn'lik tur: yazıyor (parmaklar kıpır) → 1,6 sn soru balonu ("?")
  telefondan çıkıp sola yükseliyor → 2,3 sn sağda öğretmenin sarı balonu, zıplayan
  "yazıyor" noktaları → 3,6 sn noktalar video ▶ oluyor, tavşan seviniyor (zıplama, gülen
  göz/ağız, kulaklar) → 5,0 sn balonlar sönüyor. Balon = hap + kuyruk `Path.op(UNION)`.
  Önizlemede (scratchpad `proto18`) sabit karelerle kontrol edildi.
- **`fragment_ask_question_open`:** ortadaki Lottie (`chating_anim.json`, renkleri uymuyordu)
  yerine `BunnyMascotView` (`mascotView`), `AskQuestionOpenFragment.onViewCreated`'da
  `play(CHAT)`. Eski `centerGraphic` kodda kullanılmıyordu. `chating_anim.json` kullanıcının
  onayıyla silindi (`git rm`, ~400 KB).
- Kullanıcı: "telefonun önünü görüyoruz, mantıken arkasını görmeliyiz" → telefon ARKADAN
  çiziliyor: kenarlı kılıf, sol üstte kamera adası (iki lens + flaş), ortada logo
  (önizleme `proto19`).
- **Yeni seri ekranı için iki aday (kuruldu, deneme kartında; kullanıcı seçecek):**
  `NewStreakFragment`'taki alev Lottie'sinin (`fire_anim.json`) yerine. Alev seri simgesi
  (üst bar, seri ekranı, ders paneli, kayıt akışı), bu yüzden ikisi de aleve bağlı.
  - `FLAME` (döngülü): sağ elde meşale gibi `streak_flame_ic` + radyal hale (tek
    gölgelendirici, ölçekle boyutlanıyor); titreme tabandan esneme + sallanma. Boy
    `FLAME_H` (3 → 30, 5 → 38, 7 → 46 birim), her büyümede zıplama + sevinç. Avuçta tutma
    da denendi (önizleme `proto20`): göğüste küçük kalıyordu.
  - `CALENDAR` (döngülü): önde beyaz takvim (mavi bant, halkalar), seçilen gün kadar kutu
    0,35 sn arayla işaretleniyor (baş sallama), hepsi dolunca köşede küçük alev + sevinç,
    4,6 sn'de işaretler siliniyor.
  - `streakDays`: −1 deneme (alev 2 sn'de bir 3 → 5 → 7; takvim her turda sıradaki),
    0 seçim yok, 3/5/7 seçilen (değişince alev büyüyüp seviniyor, takvim baştan).
  - **Kullanıcı ALEV'i seçti ve kayıt akışının da değişmesini istedi (kuruldu, cihazda
    görülmedi, commit edilmedi):**
    - `fragment_new_streak`: `newStreakLottie` (fire_anim) → `newStreakMascot` (190dp,
      üst boşluk 72 → 48dp, toplam yükseklik ~aynı). `onViewCreated`: `streakDays = 0` +
      `play(FLAME)`; seçenek seçilince `streakDays = value` → alev büyüyüp seviniyor.
    - `fragment_user_info` niyet sayfası: `challengeIntroLottie` → `challengeIntroMascot`
      (200dp); `bindStep(CHALLENGE_INTRO)`'da `play(FLAME)` (gizli kapta önceden
      başlatılsa açılışta beliriş görünmezdi). Kayıt akışında gün sorusu ayrı kapta
      (hedef sorusuyla ortak) ve orada maskot YOK; alevin seçimle büyümesi yalnızca yeni
      seri ekranında.
    - `fire_anim.json` başka yerlerde kullanılmaya devam ediyor (üst bar, seri ekranı,
      ders paneli).
    - **Kullanıcı düzeltmesi:** niyet sayfalarında ALEV YOK — tavşan iki kez selam verip
      beklemeye geçiyor (`play(GREET, times = 2)`; `play`'e `times` eklendi, tekrarlar
      arasında beklemeye dönmüyor). Alev gün sorusunda geliyor: yeni seri ekranında
      `showQuestion`'da `streakDays = 0` + `play(FLAME)`; kayıt akışında gün sorusu
      kabına (`streakContainer`, hedef sorusuyla ortak) `streakMascot` (150dp) eklendi,
      yalnızca CHALLENGE adımında görünüyor (hedef adımında GONE, başlık tepeye çıkıyor);
      `bindStep(CHALLENGE)`'da başlatılıyor (seçimde yeniden çizilen
      `renderChallengeStep`'te değil, yoksa her seçimde baştan başlardı).
- **Kayıt akışı hatası (kuruldu, cihazda denenmedi):** soruları cevaplayıp kayıt olmadan
  geri dönen çocuk bir sonraki girişte seri sorularını görmüyordu (yalnızca yaş + kaynak).
  Sebep: `markOnboardingDone` seri soruları cevaplanınca, kayıt BİTMEDEN konuyor;
  `UserInfoFragment` `!isOnboardingDone` ise soruyordu; sahipsiz cihazda
  `prepareForNewAccount` işareti silmiyor. Düzeltme: öğrenciye seri soruları her seferinde
  soruluyor (`streakStepsEnabled = !forceTeacher`). Bu ekran yalnızca yeni hesap açarken
  açıldığı için yeniden sormak her zaman doğru; `isOnboardingDone` artık yalnızca sunucu
  eşitlemesinde (hedefi sunucudan alma) kullanılıyor.
- **Pro ekranı (`fragment_pro_diffirent`, roket Lottie'si) için üç aday (kuruldu, deneme
  kartında; kullanıcı karşılaştırıp seçecek, ekrana bağlanmadı):** hepsi döngülü.
  - `JETPACK`: sırtta iki gümüş tüp (gövdenin iki yanından görünüyor), memelerden aşağı
    yanan alev (`drawFlame` ters ölçekle), düşen kıvılcımlar; `LIFT` hedefi 14 (havada
    süzülüyor, beklemeye dönünce yumuşakça iniyor), kollar açık, ayaklar sallanıyor.
  - `HERO`: kırmızı pelerin (yanları rüzgârla açılıp kapanıyor, alt kenar dalgalı;
    `cubicTo/quadTo` yolu), göğüste PRO rozeti (`bg_shop_super_badge`'in yeşil → mavi →
    mor geçişi, `LinearGradient` bir kez kuruluyor), sağ kol 45° havada, sol kol kalçada.
  - `CROWN`: kafayla dönen yan yatık altın taç (3 uç, mücevherli bant), uçlarda sırayla
    parıltı; zafer işareti + göz kırpma.
  - Önizleme: scratchpad `proto22`. (Bir ara "geri al" isteğiyle kaldırılmıştı; kullanıcı
    kartta görmek istediğini söyleyince aynen geri eklendi.)
  - **Kullanıcı TAÇ'ı seçti (kuruldu, cihazda görülmedi, commit edilmedi):**
    `fragment_pro_diffirent`'taki `centerMascot` Lottie (roket) → `BunnyMascotView` (aynı
    id; kodda zaten kullanılmıyordu), genişlik %50 → %75 (tavşan karesinin yarısı kadar),
    üst 8dp / alt 24dp boşluk. `ProDiffirentFragment.onViewCreated`: `play(CROWN)`.
    `rocket_anim.json` `PartCardAdapter`'da kullanıldığı için silinmedi. Jetpack ve süper
    kahraman deneme kartında duruyor.
- **Reklam atlama paneli (`fragment_ad_skip`, timsah Lottie'si; geçiş reklamı kapanınca
  çıkıyor, "7 günlük ücretsiz PRO denemesiyle reklamları atla!") için üç aday (kuruldu,
  deneme kartında; kullanıcı karşılaştırıp seçecek, ekrana bağlanmadı):** hepsi döngülü,
  sahne nesneleri (`drawAdProps`) yerde sabit, tavşandan önce çiziliyor.
  - `AD_JUMP` "Reklamı atla": sağdan REKLAM yazılı TV kayıyor, tavşan üstünden zıplıyor
    (Türkçe "atla" kelime oyunu). Zıplama (yükseklik 42, 1,1 sn) TV gövdesi ve kısa antenler
    ayak altındayken havada kalacak şekilde hesaplandı (node ile kontrol edildi);
    kulaklar tepede görünümden taşmıyor. Önce TV'ye bakıp çömeliyor, inince uğurlayıp seviniyor.
  - `AD_BUTTON` "Atla düğmesi": sağda "Atla ⏭" düğmesi; pati hazırda bekleyip basıyor,
    düğme gömülüp maviye dönüyor, yıldızlar, sağa kayıp kayboluyor. **Kullanıcı değiştirdi
    (zıplayarak sevinç yerine):** yukarıdan önce taç (1,45 sn), sonra havalı güneş gözlüğü
    (1,7 sn) hızlanarak düşüyor, inişte küçük sekme + kafa sarsılması; tavşan yukarı bakıyor;
    2,2 sn'den itibaren havalı poz: köpeğin elleriyle kavuşturulmuş kollar, arkaya yaslanma
    (−4°), baş yana (−7°), açık ayaklar, yarım gülümseme; gözlük inince sağ camda parıltı;
    4,0 sn'de taç ve gözlük sönüyor (tur 4,4 sn). Gözlük kullanıcının eklediği
    `res/drawable/cool_glasses.xml`: kafa silinince gözlük 512'lik karenin üst yarısında
    kalmıştı → görüntü alanı 512×196'ya daraltıldı, yollar bir `group translateY=-119` ile
    ortalandı (yollara dokunulmadı). Camların ortası gözlerin ortasına oturacak şekilde
    ölçekleniyor (`GLASSES_*`). Önizleme `proto25`.
  - `AD_PUSH` "Reklamı itme": iki direkli REKLAM tabelası; kedi koluyla yaslanıp itiyor
    (gövde 8° eğik, 8 birim kayık, gözler sıkılı, ağız "O"), tabela sallanarak hızlanıp
    çıkıyor; sonra köpeğin elleriyle el çırpma + sevinç.
  - Önizleme: scratchpad `proto23` (`make23.js`).
- **Kayıt soruları ekranı (kuruldu, cihazda görülmedi, commit edilmedi):**
  - "Başla"ya basınca `UserInfoFragment` artık kaymadan, doğrudan geliyor
    (`LoginStartActivity.showUserInfoFragment`'tan `setCustomAnimations` kaldırıldı; geri
    dönüş de animasyonsuz).
  - Adım çubuğu görevlerdeki çubuk tasarımında: `ProgressBar` → `FrameLayout`
    (`userInfoProgress`, kısıtlar aynı id'ye bağlı) + zemin/dolgu/parlama (18dp,
    `mission_progress_track`), çizim `applyMissionProgressOverlayNow` (mavi). Adımlar arası
    akış `ValueAnimator` ile korunuyor; ilk çizimde genişlik yoksa ölçüm sonrası
    (`applyMissionProgressOverlay`). Animatör `onDestroyView`'da iptal.
- **Kayıt bilgi soruları (yaş, kaynak, hedef) için üç aday (kuruldu, deneme kartında;
  kullanıcı karşılaştırıp seçecek, ekrana bağlanmadı):** Duo'nun kâğıt-kalemi birebir
  kopyalanmasın istendi. Hepsi döngülü.
  - `BOARD` "Kara tahta": tavşan 14 sola kayıyor, sağında şövaleli yeşil tahta; tebeşirli
    el tahtanın sol kenarında (kol kaydırılınca kökü gövdeden ayrılıyordu — o yüzden yazı
    soldan sağa kırpmayla beliriyor, el yerinde yazma hareketi yapıyor), sonra altı çiziliyor,
    baş sallama, sevinç, silme. Deneme örnekleri her turda: "9 yaş", "YouTube", "10 dk".
  - `LISTEN` "Dinleyen tavşan": düşünme kolu 72°'de (el yüzün yanında), sağ kulak 26° eğik;
    2. sn'de kulak dikiliyor, iki baş sallama, sağ üstte ✓ balonu.
  - `NOTEBOOK` "Havuç kalem": köpeğin elleri tek tek kayabiliyor (`FRONT_L_X`, `FRONT_R_X/Y`;
    kaydıkça el gölgeleri siliniyor). **Kullanıcı düzeltmesi:** "bize doğru yazıyormuş gibi"
    duruyordu → defterin ARKA kapağını görüyoruz (kırmızı kapak, spiral, beyaz etiket, yıldız
    çıkartması; yazı/satır yok). Çizim sırası: sağ el → havuç → defter → sol el
    (`drawFrontHands(left/right)` tek eli çizebiliyor). Havuç kullanıcının
    `res/drawable/carrot_ic.xml`'i (ikon zaten yazma açısında; uç ≈(55,505)); tutma noktası
    gövdenin ortası (180,330), ölçek 0,08 — ucu kapağın arkasında, üst gövde ve yapraklar
    kapağın üstünden görünüyor (havuç elin arkasındayken turuncu gövde hiç görünmüyordu).
    El, kapağın arkasındaki iki "satırda" (y 222/230) gidip geliyor. Önizleme `proto27`.
  - Önizleme: scratchpad `proto24` (`make24.js`, `proto24_parts.js`).
- **Reklam paneli için iki değnek adayı (kuruldu, deneme kartında; kullanıcı seçecek):**
  kullanıcı "Atla düğmesi" yerine sihirli değnek fikri getirdi; seçenek 1 ve 3 yapıldı.
  Billboard kullanıcının `res/drawable/billboard.xml`'i (512'lik, tabela + direk; sahnede
  yerde, tavşanın sağında ~87 birim). Değnek ve yasak işareti kodla çiziliyor.
  - `AD_ZAP` "Değnek + yıldırım": değnek elde beliriyor, "abra kadabra" diye daire
    çizerek sallanıyor (kol açısına iki frekanslı sinüs, ağız konuşuyor, uçta sönen yıldız
    izi), billboarda doğrultuyor; yıldırım (zikzak, sarı + beyaz; yumuşak hale, ekranı
    bembeyaz yapan flaş YOK) birkaç kez yanıp sönüyor; billboard kararıyor
    (`LightingColorFilter`), tabandan çöküp kül yığınına dönüşüyor, közler ve duman.
  - `AD_MAGIC` "Değnek + yasak işareti": aynı sallama; değnek ucundan billboarda yay
    çizerek uçan parıltılar, kırmızı yasak işareti büyükten küçülerek "pat" diye basılıyor
    (billboard sarsılıyor), billboard yıldız tozuna dönüşüp kayboluyor.
  - İkisi de "Atla düğmesi"yle aynı finale bağlı: `coolFinale(e, crownS, fadeS)` (taç →
    0,25 sn sonra gözlük → havalı poz; gözlük parıltısı `glassesSparkleT`). AD_BUTTON da
    artık bunu kullanıyor.
  - Önizleme `proto28` (`make28.js`).
- **Kayıt bilgi soruları — havuç kalem seçildi (kuruldu, cihazda görülmedi, commit
  edilmedi):** Duolingo'daki gibi sol üstte maskot, sağında konuşma balonunda soru.
  Kullanıcı telif riskini sordu: kalıp (karakter + soru balonu) yaygın bir arayüz kalıbı,
  karakter/çizim/renkler bizim; Duolingo yeşili ve baykuşa özgü öğe kullanılmadı.
  - `fragment_user_info`: `questionHeader` (çubuğun altında) = `questionMascot` (108dp,
    `play(NOTEBOOK)` döngüde) + `questionBubble` (arka plan `SpeechBubbleDrawable`: koyu
    zemin, `missions_track` kenar, sola bakan kuyruk; kutu+kuyruk `Path.op(UNION)`).
  - Yaş/kaynak/hedef soruları balonda (`bindQuestionHeader`); eski başlıklar (`tvTitle`,
    `tvSourceTitle`, kodda kullanılmıyordu) kaldırıldı; hedef adımında `streakStepTitle`
    gizli, gün sorusunda görünür. Soru değişince balon küçükten büyüyerek beliriyor.
  - Niyet ve gün sorusu adımlarında başlık satırı GONE; kaplar `layout_goneMarginTop=40dp`
    ile eskisi gibi çubuğun altına çıkıyor.
- **Kayıt akışı maskot düzeltmeleri + reklam paneli (derlendi ve telefon bağlanınca
  kuruldu; cihazda görülmedi; commit edilmedi):**
  - Havuç kalem artık iki hâlli: `NOTEBOOK_HOLD` (döngü: defter ve havuç elde, kalem satır
    başında havada, sana bakıyor) ve `NOTEBOOK_WRITE` (tek sefer ~2,3 sn: iki satır yazıp
    baş sallıyor). `play(emote, times, then)`'e `then` eklendi: hâl bitince beklemeye değil
    ona geçiliyor. UserInfoFragment: başta HOLD; yaş/kaynak/hedef "Devam Et"inde (geçerlilik
    kontrolünden SONRA) `play(NOTEBOOK_WRITE, then = NOTEBOOK_HOLD)`. Başlık satırı kayan
    kapların dışında olduğu için yazma, bir sonraki soru gelirken de sürüyor.
  - Seri adımları: niyet ve gün sorusunda iki ayrı tavşan vardı ve kapla birlikte kayıyordu
    (kullanıcı: "yeni tavşan yandan kayarak geliyor"). Artık tek ortak `streakMascot`
    (170dp) kapların DIŞINDA; yalnızca yazılar kayıyor. Niyette `play(GREET, 2)`, gün sorusuna
    geçince (yazılar değişirken, `bindStep`) `play(FLAME)`. Kaplar
    `stepTopBarrier`'a bağlı (questionHeader + streakMascot'un altı; GONE olan çubuğun altına
    çökmüş sayılıyor). `challengeIntroMascot` silindi.
  - `fragment_ad_skip`: timsah Lottie (`centerGraphic`) → `BunnyMascotView` (aynı id,
    genişlik %60 → %85), `AdSkipFragment.onViewCreated`'da `play(AD_MAGIC)`.
    `crocodile_anim.json` rozet/kupa/görevlerde kullanıldığı için silinmedi.
- **Kayıt ekranı küçük düzeltmeler (kuruldu, cihazda görülmedi, commit edilmedi):**
  - Balondaki soru adım değişince daktilo gibi hızlı yazılıyor (`typeQuestion`, harf başına
    22 ms; metin baştan tamamı konup yazılmamış kısım şeffaf `ForegroundColorSpan` — harf
    harf eklense balon satır kaydıkça büyüyüp zıplardı). Önceki "küçükten büyüme" kaldırıldı.
  - Kayıttaki niyet sayfasında tavşan iki kez selam yerine `SHY` (utangaç) oynuyor; yeni seri
    ekranı (ders sonu) değişmedi, orada hâlâ iki kez selam.
  - Kullanıcı isteği: niyet sayfasında tavşan sürekli utangaç kalsın (tek sefer oynayıp
    beklemeye dönüyordu). `play(SHY, times = Int.MAX_VALUE)`: tekrarlar arasında hâl
    bırakılmadığı için duruş korunuyor; yeni bir `loops` hâli eklemeye gerek olmadı.
    Kuruldu, cihazda görülmedi.
  - `login_start` (`LoginStartActivity.kt`, `activity_login_start.xml`) dosyalarında kullanıcının
    kendi değişiklikleri var: dokunma, üzerine yazma.
- **Kayıt akışı mevcut hesaba dokunmuyor (kuruldu, cihazda denenmedi, commit edilmedi):**
  kullanıcı çıkış yapıp kayıt sorularını cevaplayıp Google ile ZATEN KAYITLI hesaba girdi
  (logcat 23:00–23:01). İki sorun bulundu ve düzeltildi:
  1. Kayıtta seçilen hedef (20 dk) mevcut hesabın hedefini (sunucuda 5 dk) sessizce
     değiştiriyordu (yerel 20, ilk tutturulan günde sunucuya da gidecekti); meydan okuma ve
     hatırlatma saati de aynı yoldan ezilebiliyordu.
  2. Kayıt ekranı AÇILIR AÇILMAZ (`prepareForNewAccount`) önceki hesabın cihazdaki seri
     verisi siliniyordu: bugünkü dakikalar ve gönderilmemiş günler gidiyordu.
  Çözüm: kayıt cevapları ayrı dosyada bekliyor (`StreakRepository.savePendingSignup`,
  `streak_signup_pending`); MainActivity `bindToUser`'dan hemen sonra `applyPendingSignup`:
  Firebase hesabı son 10 dk'da açıldıysa (RegisterActivity'deki kayıt ölçümüyle aynı yöntem)
  hedef/meydan okuma/saat + onboarding + yeni seri sorusu işareti uygulanıyor, değilse
  atılıyor (StreakDiag `Repo.kayitCevaplari`). `prepareForNewAccount` kaldırıldı; silme artık
  yalnızca `bindToUser`'da, oturum başka hesaba geçince. Kayıt seçimleri cihaz değerinden
  değil varsayılandan (5 dk, 19:00) başlıyor.
  - Bu telefonda önceki denemeden kalan uyumsuzluk: yerel hedef 20 dk, sunucu 5 dk.
- **Çalışma süresi cihaza özel:** `StudyTimeTracker` yalnızca telefonda (`study_time`
  prefs), sunucuya gitmiyor. Başka cihazda bugünkü süre 0 görünür; dakikalar cihazlar
  arasında toplanmıyor, gün ancak tek cihaz hedefi tutturunca seriye işleniyor (sonra
  sunucudan diğer cihaza geliyor). Aynı cihazda başka hesaba geçince de siliniyor. Veli
  panelindeki `TimeTracker`/`dailyTimeSpent` ayrı bir ölçü (uygulama açık kalma süresi).
- **Soru başlığındaki tavşan büyüdü (kuruldu, cihazda görülmedi):** `questionMascot`
  108 → 140dp (kullanıcı: küçük görünüyor). Balon sağında kaldığı için daraldı (~180dp metin
  genişliği, en uzun soru ~3 satır, tavşan boyunu geçmiyor). Tanışmadan kayan tavşan ölçeği
  bu görünümden hesaplandığı için ayrıca bir değişiklik gerekmedi.
- **Üç küçük düzeltme (kuruldu, cihazda görülmedi, commit edilmedi):**
  - Giriş ekranı (`activity_login_start.xml`, kullanıcının kendi değişiklikleri korundu):
    tavşan + başlık + metin tek dikey zincir (packed, bias 1), "Başla"nın hemen üstüne
    yaslanıyor; metnin alt boşluğu 40 → 28dp. Tavşan tam ortaya sabitlenemedi: bu telefonda
    (393×873dp) metin düğmeye biniyordu, küçük ekranda ~70dp. Şimdi merkezin ~35dp üstünde
    (hesap, cihazda ölçülmedi). Tam orta istenirse tavşanı %70 → ~%62 küçültmek gerekiyor.
  - "Bizi nereden duydun?" seçenekleri ikişerli kart ızgarası yerine günlük hedef satırlarının
    aynısı (`StreakViews.buildOptionRows`, alt alta, gri); `renderSourceStep`. Kaydedilen
    değerler aynı, istatistik anahtarları değişmedi. 8 kart XML'den silindi.
    Sonra (kullanıcı isteği): `sources` = (kaydedilen değer → görünen ad) listesi; "Youtube"
    değer olarak kaldı, ekranda "YouTube"; "Arkadaş/Aile"nin ardına yeni **"Okul/Öğretmen"**
    (sunucu `acquisitionSource`'u serbest metin alıyor, izin listesi yok). Sıra karıştırılmıyor
    (kullanıcı istemedi).
  - Giriş ekranında `tvBody` alt boşluğunu kullanıcı kendisi 48dp yaptı; dokunma.
- **Abonelik akışı ders sonrası kuyruğunu bekletiyor (04.10.2026; kuruldu, cihazda
  denenmedi, commit edilmedi):** kullanıcı: ders sonu reklam → reklam atlama paneli → "Pro'ya
  geç" → Pro paneli açılırken arkada rozet kutlaması ya da yeni seri sorusu başlıyordu.
  Sebep: kapılar yalnızca `AdSkip`'e bakıyordu; panel Pro'yu açıp 500 ms sonra kendini
  kapatınca kuyruk "engel yok" deyip sıradakini Pro'nun ALTINDA açıyordu.
  - `MainActivity.subscriptionFlowBlockReason()`: `ProDiffirent` / `Plan` dialog'u açık ya da
    `BillingManager.isPurchaseFlowActive()` (launchPurchase → onPurchasesUpdated arası; 2 dk
    güvenlik sınırı). Eklendiği kapılar: `marathonGuideMapBlockReason` (harita kuyruğu,
    tanıtım, rehber hepsi buradan geçiyor), `offMapNewStreakBlockReason` (Görevler/yarış
    dönüşündeki yeni seri), `isTasksReturnCovered` (Görevler dönüş sonucu).
  - Kuyruğu dürtenler: `ProDiffirentFragment.onDismiss`, `PlanFragment.onDismiss`,
    `BillingManager.onPurchaseFlowEnded` (MainActivity'de bir kez kuruluyor;
    `installDefaultBillingCallbacks` dokunmuyor). Log: `PostLessonQueue` →
    `bekliyor | block=pro_panel_showing / plan_showing / purchase_flow`.
  - Bilinen sınır: Görevler/yarış dönüşü yolları en fazla 3 dk bekliyor (eski bütçe); kullanıcı
    abonelik ekranlarında daha uzun kalırsa o sonuçlar yine açılır. Harita kuyruğunda bekleyen
    iş kaybolmuyor (bekçi 2 dk sonra yalnızca kilidi bırakıyor, kapanış dürtüsü sürdürüyor).
- **Reklam atlama paneli iki sürümlü (04.10.2026; kuruldu, cihazda görülmedi, commit
  edilmedi):** kullanıcı "denemeyi kullanmamışsa bugünkü gibi, kullanmışsa Pro aboneliği
  anlatsın, müzik çalmayabilir, konfeti ikisinde" dedi. Koşul Play'in deneme hakkı:
  `BillingManager.freeTrialDays(SUB_PRO)` (düğme metni `SubscriptionCta` de buna bakıyor).
  - Hakkı var: "N günlük ücretsiz PRO / denemesiyle reklamları atla!" (N Play teklifinden),
    zil + "Deneme süren sona ermeden önce bildirim alacaksın.", müzik + alkış.
  - Hakkı yok: "PRO ile reklamsız, / kesintisiz öğren!", yeşil onay ikonu (tint kaldırılıyor) +
    "Aboneliğini istediğin zaman iptal edebilirsin.", müzik ve alkış YOK; konfeti
    (görsel) iki sürümde de; sesler (patlama dahil) yalnızca deneme sürümünde (kullanıcı: tek başına konfeti sesi amatör).
  - Ölçüm: `ad_skip_shown`'a `trial_offer` = yes/no.
  - Not: soru tanıtımının (AskQuestionOpen) otomatik açılma koşulu deneme hakkı DEĞİL; oturum
    açık + öğretmen değil + Pro/Premium değil + cihaz hoş geldin kredisini almamış
    (`WelcomeCreditEligibility`). Kullanıcıya açıklandı.
- **Commit + deploy (03.10.2026 23:21):** kullanıcı isteğiyle bu bölümdeki her şey
  `28d6c35`'te commit edilip push edildi (test anahtarları kapatıldı; `.firebase` önbelleği,
  `.idea` ayarı ve `package-lock.json` dışarıda). "Deploy Cloud Functions" iş akışı başarılı
  (run 37150948053) — seçilebilir hatırlatma saati sunucuda canlı. "Build Debug APK" bilinen
  CI sorunuyla yine başarısız (yerel derleme sağlam). Cihazda uçtan uca denenmedi: ilk
  gerçek kontrol, saati seçen bir hesabın `users/{uid}/streak/state` dokümanında
  `reminderLocalHour` ve doğru `reminderHourUtc` görmek.
- **Kayıtta hatırlatma sorusu + seçilebilir hatırlatma saati (kuruldu, cihazda görülmedi):**
  kullanıcı önerilerin hepsini seçti.
  - `Step.REMINDER` (gün sorusundan sonra, son soru; tanışma artık "5 kısa sorum" diyor).
    Başlık "Sobi sana her gün hatırlatsın mı?", alt yazı "Saati istediğin zaman
    değiştirebilirsin"; satırlar diğer sorularla aynı (`StreakViews.buildReminderRows`):
    Okuldan sonra 16:00 / Akşamüstü 18:00 / Akşam 19:00 / Yemekten sonra 20:00, 19:00 seçili.
    Düğmeler "Evet, hatırlat" (Android 13+ bildirim izni penceresi, sonuç ne olursa olsun
    kayda devam) ve yeni `btnLater` "Şimdi değil" (izin sorulmaz). İkisinde de saat
    kaydediliyor ve `MainActivity.markNotificationPermissionPrompted` ile ana ekranın ilk
    açılıştaki bağlamsız izin isteği kapanıyor.
  - Sobi yerinde (ortak `streakMascot`), yalnızca hâl değişiyor: yeni `Emote.ALARM` — önde
    iki çanlı kırmızı çalar saat, patiler arkadan tutuyor (saat patilerin ÖNÜNDE çiziliyor),
    kadranda seçili saat (`alarmHour`). Seçim değişince akrep yaylı dönüyor, yelkovan aradaki
    her saat için bir tur atıyor, saat hemen çalıyor (titreme, çekiç, ses çizgileri, küçük
    zıplama); her turda bir kez de kendiliğinden çalıyor. Önizleme: scratchpad
    `maskot/make29.js` → `proto29.png`. Oyun alanında (Karakter Animasyonu) saatler kendi
    kendine dönüyor (`alarmHour = -1`).
  - Kullanıcı isteği: hazır saatlerin altına **"Başka bir saat"** satırı
    (`StreakViews.buildReminderRows(..., onCustom)`); dokununca 24 tam saatlik ızgara penceresi
    (`StreakViews.showHourPicker`, 4 sütun, 06:00'dan başlıyor, gece saatleri sonda, dakika
    yok). Hazırlardan biri değilse o satır seçili ve sağında saat yazıyor. Kayıtta ve seri
    ekranında aynı. Sunucu 0–23'ü zaten kabul ediyordu, değişiklik gerekmedi. Gün/hedef/
    hatırlatma seçenekleri artık kaydırılabilir (`streakOptions` bir ScrollView içinde): 5
    satır bu telefona ancak sığıyordu.
  - Seri ekranı: "Günlük hedefi değiştir"in altına "Bildirim saatini değiştir · 19:00"
    (`streakChangeReminder`, aynı pencere); izin yoksa orada da soruluyor.
  - Saklama/eşitleme: `StreakRepository.reminderHour/setReminderHour` + "gönderilmedi"
    işareti; saat YALNIZCA bu cihazda seçildiyse `submitStreakDay`'e `reminderHour` olarak
    gidiyor (yeni cihazın varsayılanı sunucudaki seçimi ezmesin), sunucudaki
    `reminderLocalHour` yanıttan ve doküman okumasından geri alınıyor.
  - Sunucu (`functions/streakReminder.js`, `index.js`): `normalizeReminderHour`,
    `reminderHourUtc(offset, localHour)`, `reminderPatch` önceliği seçilen > kayıtlı > 19:00,
    `readStreakState.reminderLocalHour`, yanıtlarda `reminderLocalHour`. Günsüz çağrıda
    doküman yoksa artık YALNIZCA saat seçildiyse oluşturuluyor (yeni kayıt olmuş, hedef
    tutturmamış çocuk hatırlatmayı ilk günden alsın). Eski istemciler etkilenmiyor.
    Testler: `node functions/scripts/test-streak-reminder.js` 32/32, diğer seri testleri geçti.
  - Ölçüm: `STREAK_STAGE_REMINDER`, yeni olay `streak_reminder_set` (saat, kaynak, seçim
    yes/later, izin granted/denied/not_needed/not_asked).
  - Deploy edildi (yukarıya bkz.). Not: yayından önce kurulan sürümler saati göndermiyordu,
    onlar 19:00'da kalır; `claude/**` dalına functions içeren push = deploy, önce sor.
  - Alt sistem çubuğu: koyu temada (`values-night/themes.xml`) varsayılan
    `navigationBarColor` `message_topbar`'dı (#111416, zeminden koyu); `background_color`
    yapıldı. Kendi rengini koddan vermeyen 5 ekran düzeldi: Login, Register, TeacherLogin,
    TeacherOtp, EmailVerification. Main/Splash/LoginStart zaten koddan aynı rengi veriyordu.
- **Kayıtta maskotla tanışma (kuruldu, cihazda görülmedi, commit edilmedi):** Başla'dan sonra,
  yaş sorusundan ÖNCE iki ekran (yalnızca öğrenci; öğretmen doğrudan yaştan başlıyor, "ilk
  dersinden önce" ona uymuyor). Duolingo'nun tanışma ekranlarından esinlenildi (kullanıcı
  ekran görüntüsü verdi); metin kendi sesimizle yazıldı, birebir değil.
  - `Step.MEET`: ortada tavşan (180dp), üstünde aşağı kuyruklu balon (daktilo): "Selamlar!
    Benim adım Sobi!"; `repeatWithPause(GREET, 2000)` = el salla, 2 sn bekle, tekrar.
  - `Step.BRIEF`: "İlk dersinden önce seni tanımak için sadece **4 kısa sorum** var!" (sayı
    `Step.asks` olan adımlardan: yaş, kaynak, hedef, gün); `repeatWithPause(CLAP, 2000)`.
    İki tanışma ekranı arasında kayma yok, yalnızca yazı ve hâl değişiyor.
  - Devam → `leaveIntro`: balon soluyor, tavşan `stop()` ile beklemeye dönerken küçülerek
    soru başlığındaki maskotun yerine kayıyor (450 ms), varınca o maskot görünüp defterini
    çıkarıyor (NOTEBOOK_HOLD), yaş sorusu balona yazılıyor. Geri tuşunda `returnToIntro` tersini
    yapıyor. Soru başlığı tanışmada INVISIBLE (hedef konum ölçülü kalsın), tanışma kabı sonra
    INVISIBLE (dönüşte orta konum ölçülü kalsın).
  - İlerleme çubuğu tanışmada gizli ve sayılmıyor; `progressFor` artık `flowSteps` üzerinden.
  - **İsim tek yerde:** `strings.xml` → `mascot_name` (şimdilik "Sobi", kullanıcıyla isim
    kararı açık).
  - Yeni: `BunnyMascotView.repeatWithPause/stop` (otomatik selam düzeneğinin genellemesi; giriş
    ekranının `autoGreetIntervalMs` kullanımı değişmedi), `SpeechBubbleDrawable` alt kuyruk
    (`TailSide.BOTTOM`; parametreler `tailLength/tailBase` oldu).
  - Huni: öğrencide açılışta `SIGNUP_INTRO = "intro"`, `age` artık yaş sorusuna VARINCA
    (tanışmadan devam edince) sayılıyor. GA4 hunisine "intro" adımı eklenmeli.
- **Kayıtta yeni "ilerleme yolu" ekranı (kuruldu, cihazda görülmedi, commit edilmedi):**
  GOAL ("Her gün ne kadar…") adımından hemen sonra, `Step.ROADMAP`. Mimo'nun benzer
  ekranından esinlenildi; kullanıcı seçeneklerden **D + B1 + Z2**'yi seçti:
  - Başlık balonda (daktilo): "Her gün **N dakikayla** buraya varacaksın!" (N altın, kalın).
  - Adımlar: Abaküsü tanı (`abacus_svg_ic`, BUGÜN) · Serini büyüt (`streak_flame_ic`,
    bugün+7 gün, ör. "10 EKİM") · Kafadan hesap ustası ol (`brain`, kullanıcının eklediği
    çizim; ay+yıl, ör. "OCAK 2027"). Kullanıcı isteğiyle 2. adımdaki "Her gün N dakikayla"
    kaldırıldı (başlıkta zaten var), 3. adımda kristal yerine beyin.
  - Çizim `LearningPathView` (yeni): soldan yükselen basamaklı **abaküs teli** (mavi → yeşil →
    altın), adımlar telin üstünde açık zeminli yuvarlak boncuklarda, yazılar altlarında, telin
    ucunda altın **ok başı** (kullanıcı havuç yerine ok istedi; çentikli, köşeleri yuvarlak,
    40° eğimli); altta zaman çizgisi + etiketler. Tel ve zaman çizgisi soldan çiziliyor
    (900 ms), ok başı ucunda beliriyor, sonra her adımda boncuk → yazı → etiket 0→%120→%100
    (toplam ~2,8 sn; kap kayarak gelince başlıyor, geri dönünce baştan oynuyor). Mimo'dan
    ayrışma: basamaklı tel, mor-pembe yok, kendi metin/ikonlarımız.
  - Tarih `LearningPathPlan` (yeni): 1–4. bölümlerin (körleme toplamanın sonuna kadar) ders
    adımları müfredattan sayılıyor (şu an 190 adım) × **adım başına 4 dk** ÷ günlük hedef,
    **haftada 5 gün** çalışma varsayımıyla. Bugün (3 Eki 2026) için: 20 dk → KASIM 2026,
    10 dk → OCAK 2027, 5 dk → MAYIS 2027. **İki sayı da varsayım**; Analytics'teki
    `lesson_step_pass` `elapsedMs` ortancasıyla `MINUTES_PER_STEP` düzeltilmeli.
  - İlerleme çubuğu toplamı artık `Step.entries.size` (6); geri tuşu ROADMAP ↔ GOAL/niyet.
    Huniye `STREAK_STAGE_ROADMAP = "roadmap"` eklendi (yeni ekran kayıp yaratıyor mu).
  - Önizleme: scratchpad `yol/make.js` (yerleşim hesabının JS kopyası, headless Edge).
- **Reklam atlama paneli her reklamdan sonra (geçici, test için):** kullanıcı "şimdilik her
  reklamdan sonra gelsin" dedi. Seyreklik kuralları (4 reklamda bir, 2 saat ara, günde 2)
  silinmedi; yeni `AdSkipDebug` anahtarı (`NewStreakPromptDebug` kalıbı, `BuildConfig.DEBUG`
  ile çarpılıyor, release'e sızmaz) açıkken `AdSkipPolicy.onAdClosed` her UYGUN reklamda
  true dönüyor (`eligible = false` kararları geçerli).
- **Satış ekranlarında telefon çubuk renkleri (kuruldu, cihazda görülmedi, commit edilmedi):**
  `fragment_ad_skip`, `fragment_pro_diffirent`, `fragment_ask_question_open` ve `fragment_plan`
  `Theme_*_NoTitleBar_Fullscreen` dialog'ları: tema durum çubuğunu gizliyordu (tepede sistemin
  siyah bandı) ve gezinme çubuğu sistem renginde kalıyordu. Yeni `SystemBarColors.applyToDialog`
  (her ekranın `onViewCreated`'ında BİR kez — `onStart` her öne gelişte çalışıyor, Android 15+
  dolguları üst üste eklenirdi): FLAG_FULLSCREEN temizleniyor, durum çubuğu görünür ve üst
  renge, gezinme çubuğu alt renge boyanıyor, simgeler zemine göre açık/koyu. Üç lacivert
  ekranda üst `paywall_navy` (#050C38, yeni renk kaynağı), alt beyaz; plan baştan sona
  beyaz. Android 15+ (çubuk rengi yok sayılıyor, içerik çubukların altına uzanıyor) için
  üst görünüme durum çubuğu, alt beyaz bölüme gezinme çubuğu kadar dolgu ekleniyor;
  telefon Android 13 olduğu için o yol DENENMEDİ.
- Test anahtarları (`AskQuestionPromoDebug`, `NewStreakPromptDebug`, `AdSkipDebug` FORCE)
  04.10.2026'da kullanıcının isteğiyle yeniden KAPATILDI (false). `StreakDiag.ENABLED` bir test
  anahtarı değil, teşhis logu; yayın öncesi listesinde.

## Ders sonu Sobi sahneleri (04.10.2026 — oyun alanına eklendi, commit edilmedi)

- Kullanıcı ders sonu ekranları (`fragment_lesson_result`, `fragment_lesson_result_false`,
  `fragment_chest_result`) için seçenek tablosundaki BÜTÜN animasyonların Görevler →
  "Karakter Animasyonu" kartına eklenmesini istedi; hangisinin hangi ekrana gideceğine
  KENDİSİ karar verecek. Henüz hiçbir ekrana bağlanmadı.
- `BunnyMascotView.Emote`'a eklenenler (ALARM'dan sonra): WEIGHTS (boncuk halteri), SKATE
  (kaykay + ollie + havalı gözlük), SKATE_FALL (taşa takılıp düşüyor, sersem, kalkıp
  silkeleniyor, kararlı yumruk, koşarak çıkış), FOOTBALL (3 sektirme + kafa golü),
  BASKETBALL (2 sektirme + potaya atış, file sallanıyor), GUITAR (notalar), JUGGLE (3
  soroban boncuğuyla kaskad), PERFECT (yıldız patlaması + taç + gözlük finali), STARS
  (sandık yıldızları; `starCount` 0..3 verilirse o kadar, -1 = deneme için 3), DETERMINED
  (üzgün → nefes → kararlı yumruk).
- Yeni kanallar 49–59 (TILT gövde eğimi ayaklar etrafında, KICK_L/R bacak tekmesi, eşya
  görünürlükleri, BROWS_DET kararlı kaşlar). Çizimler dosyanın "ders sonu sahneleri"
  bölümünde; zaman/ölçü sabitleri companion'da W_ / SK_ / SF_ / FB_ / BB_ / JG_ / PF_ /
  ST_ / DT_ önekli.
- GITAR artık `guitar.xml` (elektro gitar) ile çiziliyor: gövde ekranda solda, sap sağa-yukarı;
  soldaki pati tellere vuruyor (bunun için yeni kanal `FRONT_L_Y` = 60, COUNT 61), sağdaki
  sapta. Cihazda kare kare görüldü, düzgün.
- Kullanıcı "havalı/sert görünmeye çalışan ama tatlı" sahneler istiyor (örnek: kovboy
  şapkası `cowboy_hat.xml`, yüz gizli → baş kalkar, gözler kısılır, el bele gider, silah
  yerine havuç çıkarıp ısırır). Seçenekler sunuldu, kullanıcının seçimi bekleniyor.
- Cihazda yalnızca halter ve yıldızlar kare kare görüldü; halter çubuğu/boncukları
  büyütüldü, yıldızlar kulakların üstünden yanlara alındı — bu iki düzeltme derlendi
  ama KURULMADI (kullanıcı o sırada telefonda sahneleri deniyordu). Diğer sekiz sahnenin
  geometrisi (top–ayak/kafa hizası, pota yeri, düşmede yıldızların başın üstüne denk
  gelmesi, gitar–pati hizası, hokkabazlık yüksekliği) kontrol edilmedi.

## Havalı ama tatlı sahneler (04.10.2026 — yazıldı ve derlendi, CİHAZDA DENENMEDİ, commit edilmedi)

- Yeni hâller: COWBOY (cowboy_hat.xml, eğik), COWBOY_FRONT (cowboy_hat_bandana.xml ikiye bölündü:
  `cowboy_hat_front.xml` + `bandana.xml`), KARATE (bant ve tahta elle çizildi), NINJA (maske
  elle çizildi), BOXER (boxing_gloves.xml), PIRATE (pirates_hat.xml; göz bandı ve dürbün elle).
- Yeni kanallar 61–76 (HAT_A … SWEAT, COUNT 77), Mouth.CHEW / SMIRK, kısık göz (SQUINT).
  Kod "havalı ama tatlı sahneler" bölümünde, sabitler CB_ / KR_ / NJ_ / BX_ / PR_.
- Şapka/bant/maske yerleşimi bilgisayarda önizlemeyle ayarlandı (scratchpad hats*.js);
  hareketler cihazda HİÇ görülmedi. Sıradaki iş: kurup altı sahneyi kare kare kontrol etmek
  (özellikle kılıf–el hizası, havucun ağza denk gelmesi, karate vuruşunun tahtaya değmesi,
  dürbünün göze oturması, kulak tokadı).

## Bir kez oyna, son hâlde bekle (04.10.2026 — kuruldu, kullanıcı inceliyor)

- `Emote.holdFromMs`: verilen hâl bir kez oynuyor, sonra yalnızca son bölümü (holdFromMs..süre)
  döngüde. Bekleme bölümündeki salınımlar bölüm boyuna tam oturuyor (env kullanılmıyor).
  Bekleyenler: PERFECT, COWBOY(+FRONT), KARATE, NINJA, BOXER, PIRATE, GLASSES, MUSCLE, RAPPER.
  Ders sonu sahnelerinin ilk partisi (WEIGHTS…DETERMINED) hâlâ baştan dönüyor.
- Korsan zıplamıyor, dürbünle bakmaya devam ediyor; ninja kaçmıyor, gergin sağa sola bakıyor;
  Mükemmel: zıplama/yıldız patlaması yok, taç + gözlük düşüp zafer işaretiyle poz.
- Yeni: GLASSES (gözlük kayıyor, baş hareketiyle yerine), MUSCLE (minicik kas tümsekleri),
  RAPPER (`rap_hat.xml` aynalanıp yana çevrili kep, elle çizilmiş altın zincir; kep uçuyor,
  yakalayıp geri atıyor).
- Oyun alanı listesinin sonu alt çubuğun arkasında kalıyordu: ChipGroup paddingBottom 120dp.
- Kullanıcı: telefonda kare kare kontrolü o istemedikçe YAPMA; o bakıp değişiklik/silme
  listesini verecek.

## Silinen ve düzeltilen sahneler (04.10.2026 — kuruldu, kullanıcı bakacak)

- Kullanıcının isteğiyle SİLİNDİ: BOXER, COWBOY (eğik şapka), MUSCLE, SKATE_FALL, FOOTBALL,
  BASKETBALL (kodları, kanalları, sabitleri). Kullanılmayan asset'ler de silindi:
  `cowboy_hat.xml`, `boxing_gloves.xml`, `cowboy_hat_bandana.xml` (ondan bölünen
  `cowboy_hat_front.xml` + `bandana.xml` kullanılıyor).
- Kovboy önden: şapka inerken kafa öne eğiliyor — yeni `HEAD_PITCH` kanalı: kafa boyna doğru
  basılıyor (PITCH_SQUASH), yüz aşağı kayıyor (PITCH_FACE_DY), kulaklar kısalıyor (PITCH_EAR).
- Rapçi yeniden döngüde (6 sn = 12 vuruş). Ninja dumandan sonra kısık gözlerle sinsi sinsi
  sağa sola süzülüyor (sneakyLook; ter damlası kaldırıldı). Gitar: tellere vuran pati omuzdan
  açısal sallanıyor (yeni `FRONT_L_ROT` kanalı).

## Ders sonu için seçilen sahneler (04.10.2026, kullanıcı)

- BAĞLANDI: `LessonEndMascot` (yeni dosya). Başarılı ders (LessonResult) ve sandık sonucu
  (ChestResult) dokuz sahneden (GUITAR, JUGGLE, PERFECT, COWBOY_FRONT, KARATE, NINJA, PIRATE,
  GLASSES, RAPPER) TORBA usulü seçiyor: karışık sıra, torba bitmeden tekrar yok, iki ekran
  aynı torbayı paylaşıyor, torba SharedPreferences `lesson_end_mascot`ta. Başarısız ders
  (LessonResultFalse) hep DETERMINED; o artık kararlı pozda bekliyor (holdFromMs 3300).
  Üç düzende Lottie alanı yerine 200dp `mascotView`; sahne ekran girişinden 450 ms sonra.
  Kullanıcının kararı: Mükemmel de torbada eşit (yalnızca hatasız derse ayrılmadı), sandıkta
  da torba (Korsan önceliği yok).
- Bu üç ekrandan çıkan 11 Lottie (assets/animation_one + animaton_two…twelve, ~1,2 MB)
  kullanıcının onayıyla SİLİNDİ; animaton_thirteen abaküs ve körleme ekranlarında kullanıldığı için duruyor.
- PERFECT: pozdan sonra (bekleme bölümünde) sağdan soldan foto flaşları patlıyor, her
  flaşta gözlükte parıltı (`drawPhotoFlashes`, FLASH_* sabitleri). Kuruldu, kullanıcı bakacak.
- Flaşlar artık Sobi'nin üstünde (kafa/gövde) ve her flaşta tavşan bir an beyaza çekiliyor
  (tavşan bir katmana çiziliyor, SRC_ATOP ile yalnızca çizili yerleri; `photoFlashTint`).
- CROWN (Pro paneli de bunu oynatıyor): taç ve gözlük baştan takılı, Mükemmel'in zafer pozu,
  aynı flaşlar; süre 4 sn (flaş döngüsü 2 sn'nin katı).
- Flaş artık YALNIZCA tavşanın anlık beyazlaması (`photoFlashTint`, 0,18 sn; sırayla soldan ve
  sağdan: gelen yan parlak, öbürü soluk — iki LinearGradient, `lastFlashFromLeft`): ışınlar, hale
  ("dalga"), çekirdek, flaşta gözlük parıltısı ve CROWN'daki taç yıldızları kaldırıldı.
- JUGGLE: palyaço burnu + yaka beğenilmedi, kaldırıldı; yerine `jester_hat.xml` soytarı şapkası
  (JESTER_A, JESTER_* sabitleri). Kullanıcı yarıya küçülttürdü: 0,17 ölçek, kulakların
  arasında 3° yatık (10° fazla eğik bulundu); kulaklar normal. Önizleme: scratchpad jester.js.

## Seri ekranından dönüşte harita geri düğmesi kayboluyordu (04.10.2026 — düzeltildi, kuruldu)

- `updateCurrencyPanelVisibility` lessonPartBackButton'ı harita/mağaza dışında gizliyordu;
  seri ekranı da mağaza gibi haritanın üstüne ekleniyor ama istisnada yoktu. StreakFragment
  istisnaya eklendi. Kullanıcı cihazda deneyecek.

## Ders sonucu düzeni (04.10.2026 — kuruldu, kullanıcı bakacak)

- fragment_lesson_result ve fragment_lesson_result_false: başlık(lar) ve kutular düğmenin
  hemen üstünde; Sobi (mascotView) üstte kalan bütün yeri kaplıyor (weight=1, çizim kendini
  kareye sığdırıyor), paddingTop 80→48dp. chest_result DEĞİŞMEDİ (istenmedi).

## Öğretmen görselleri (teacher_emotes_*) yerine Sobi — sırayla (04.10.2026 başladı)

- Kullanım yerleri: MapFragment.showGuidePanel (gpt4, gpt3; GuidePanelView), GuideHelper
  rehberleri 1–6 (AbacusFragment + BlindingLessonFragment, ivGuideImage), varsayılan src
  fragment_abacus.xml (stick) ve view_guide_panel.xml (gpt3). Üç PNG ~5,1 MB.
- Yeni hâller: TEACH_TALK (anlatma), TEACH_POINT (havuçla aşağıyı gösterip iki kez dokunma),
  TEACH_WARN (kararlı kaşlar, havucu "olmaz" diye sallama); hepsi elde havuç (THINK kolu,
  HCARROT_*). Rehber sayfası için hâl RASTGELE değil, içeriğe göre seçiliyor (kullanıcı onayı).
- `GuideContent.emote` + `GuideVisual.bind`: emote varsa resim gizlenip Sobi oynuyor. Düzenlerde
  resim ve Sobi `guideVisual` FrameLayout'unda (kutu görünenin boyunu alıyor; Sobi 140dp,
  `guide_mascot_size`), yazı kutuya göre ortalanıyor.
- Kullanıcı düzeltmesi: Sobi konuşmuyor (Mouth.HAPPY, gülüyor); üç hâlde de tek hareket havucun
  ağır ağır sağa sola sallanması (TEACH_SWAY_DEG); uyarı da güler yüzlü (kızma kaldırıldı).
  Rehber 1'de yazma efekti ve ses kaldırıldı, guide1_0…4.mp3 silindi (başka yerde yoktu).
  Rehber 6'nın sesi ve yazma efekti duruyor (guide6_0).
- BİTEN: rehber 1 (toplama): anlat → göster → göster → göster → uyar.
- BİTEN: rehber 2 (sihirli değnek / kural tablosu): anlat → göster; guide2_0/2_1.mp3 silindi.
- BİTEN: rehber 3 (kurallar kitabı): anlat → göster; guide3_0/3_1.mp3 silindi.
- BİTEN: rehber 4 (tabloyu abaküse alma): anlat → göster → göster; guide4_0…4_2.mp3 silindi.
- BİTEN: rehber 5 (sayıyı geçme oku): göster → anlat; guide5_0/5_1.mp3 silindi.
- BİTEN: rehber 6 (abaküs boyutu): göster; guide6_0.mp3 silindi. Artık hiçbir rehberde ses ve
  yazma efekti kullanılmıyor (GuideContent alanları ve çalma kodu duruyor).
- BİTEN: harita maraton rehberi (GuidePanelView): anlat → göster; GuidePanelData artık (emote, text).
- TEMİZLİK: GuideContent.imageResource, GuideVisual, ivGuideImage ve resim kutusu (FrameLayout)
  kaldırıldı; düzenlerde doğrudan `guideMascot`. Üç öğretmen PNG'si (~5,1 MB) SİLİNDİ. İş bitti.

## Harita: ders/sandık daireleri yerine alt alta kartlar (05.10.2026 — kuruldu, kullanıcı bakacak)

- Yeni `item_lesson_card.xml` + `SegmentBarView` (dilimli düz adım çubuğu; CircleProgressBar'ın
  segment yöntemlerinin karşılığı). LessonAdapter.LessonViewHolder yeniden yazıldı; dolma
  animasyonu (pendingLessonProgressAnimations), kalıcı altın (finalGoldVisualUnlocked) ve nefes
  alma mantığı aynen taşındı. Nefes alma artık yalnızca AÇIK ve bitmemiş kartta (eskiden
  kilitlilerde de vardı).
- Durumlar: kilitli gri + kilit; açık mavi + beyaz kenar + ok; bitmiş: BÜTÜN KART ALTIN
  (lesson_center_gold), yazı/dilim koyu kahve, onay (kullanıcı A seçeneğini seçti). Bitişte mavi→altın
  renk geçişi; parlama şeritleri kullanıcının isteğiyle kaldırıldı (lesson_card_shine_* silindi).
  Bitmiş sandıkta yıldızlar koyu kahve (star_on_ic altın zeminde görünmüyordu).
  Sandık (27 sandığın hepsi tek adımlı): adım çubuğu yerine 3 yıldız (stepCupIcon → 0–3).
  İkon şimdilik ders ve sandıkta `profile_book_ic3`. Tıklayınca yine showLessonBottomSheet.
- Kaldırılanlar: DynamicOffsetDecoration.kt, item_lesson.xml, yalnız onlara ait 6 boyut.
  LessonItem.offset verisi (169 satır) duruyor, kullanılmıyor. Basma animasyonu yeni
  `lesson_card_press_animator` (0,97; daireninki 0,80 geniş kartta fazlaydı).

## Ders paneli: alttan açılan panel yerine karta bağlı panel (05.10.2026 — derlendi, KURULMADI: telefon "unauthorized")

- `lesson_popover.xml` + `PanelPointerView` (ok) + `LessonPanel` (dışarıdan kapatma). Panel tıklanan
  kartın altında (sığmazsa üstünde) açılıyor, ok kartı gösteriyor, kartın görüntüsü karartmanın
  üstüne konuyor (drawToBitmap); zemin ekranla aynı, çerçeve durum rengi (açık mavi, bitmiş
  altın, kilitli gri). Karartma üst para panelini ve alt menüyü de örtüyor (addChromeDims, pencere
  kökünde; dokununca kapanıyor); telefonun üst/alt sistem şeritleri de %50 koyulaşıp geri dönüyor
  (dimSystemBars, LessonAdapter.savedBarColors). Düğmeler: Başla / Devam et / Gözden geçir / Tekrar dene (sandık) / Kilitli.
- Kök hâlâ "bottom_sheet" etiketiyle (LessonPanel.TAG) bulunuyor; Rekor satırı (recordLayout)
  kimliği korundu — maraton rehberinin son adımı onu yanıp söndürüp tıklatıyor, kapatma artık
  LessonPanel.dismiss. Rehber açıkken karartma yok ve düğmeler kapalı (eskisi gibi).
- Silindi: lesson_bottom_sheet.xml, record_background.xml. lesson_sheet_* boyutları DURUYOR
  (race_lesson_bottom_sheet de kullanıyor — bir an silinip geri konuldu).

## Harita: sabit bölüm başlığı kaldırıldı, soru düğmesi sağ altta (05.10.2026 — derlendi, KURULMADI: telefon offline)

- StickyLinear (stickyHeader, kısım/ünite + bölüm adı) ve updateStickyHeader / safeColorRes kaldırıldı;
  listedeki TYPE_HEADER başlıkları yetiyor (kullanıcı ünite bilgisini istemedi). Kullanılmayan
  map_sticky_* boyutları silindi (map_sticky_padding_horizontal ve map_sticky_recycler_padding_top
  başka yerde kullanıldığı için duruyor).
- askQuestionButton sağ alt köşede (16dp); liste altına 92dp boşluk (map_list_padding_bottom) ki son
  kart düğmenin altında kalmasın.

## Alt bar: seçili sekme kutusu ve zıplama (05.10.2026 — kuruldu, kullanıcı bakacak; commit edilmedi)

Kullanıcı sekme geçişini "hissedemediğini" söyledi: her menü öğesinin tek ikonu var ve
`itemIconTint="@null"`, yani seçili/seçisiz hâl birebir aynıydı. Ara çözüm:
- `drawable/bottom_nav_item_bg.xml` → `app:itemBackground`: seçili sekmenin arkasında
  `bright_background_color` dolgulu, 2dp `bottom_nav_selected_stroke` (#49C0F8) çerçeveli, 14dp
  köşeli kutu. Tema MaterialComponents olduğu için M3 active indicator yok; çerçeve onunla
  zaten çizilemezdi.
- `MainActivity.bounceBottomNavIcon`: seçimde ikon 0.85'ten Overshoot ile 1'e (280ms) + hafif titreşim.
- Sonraki adım (kullanıcıyla konuşuldu): Duolingo tarzı renkli ikonlar; yalnızca drawable'lar değişecek.
  İlki yapıldı: ev sekmesi `home_ic` yerine kullanıcının verdiği renkli `home_ic1` (kuruldu).
  Sonra: görevler `missions_ic3`, mağaza `shop_ic1`, sohbet `chat_credit_ic2`, keşfet `explore_ic1`;
  profil sekmesi kullanıcının avatarı. Eski `home_ic`, `tasks_ic`, `shop_ic`, `chat_ic`, `explore_ic` artık
  hiçbir yerde kullanılmıyor (silinmedi).

## Avatar: Personas (05.10.2026 — profile bağlandı; commit 7927804, functions+rules deploy edildi)

**Profile bağlandı:** eski 12'li AvatarPickerFragment, fragment_avatar_picker, avatar_ic1-12 ve
kullanılmayan profile_ic1/3/4/5 silindi (profile_ic1/3/4/5 index'te "A" olarak duruyordu; diskten
silindi, index'ten değil). Profil fotoğrafına dokununca AvatarCustomFragment (replace + back stack).
Kayıt: `users/{uid}.avatarConfig` + yerel kopya (uid'ye göre). Alt bar profil ikonu =
avatar (`MainActivity.refreshProfileNavIcon`; açılışta checkSubscriptionAndUpdateEnergy'nin
okuduğu users dokümanından eşitleniyor, ek okuma yok). Takipçi/arkadaş arama listeleri
`publicProfiles.avatarConfig`'ten çiziyor. Görevler'deki test kartı kaldırıldı. Renk değerleri
SVG'ye yazılmadan önce doğrulanıyor (başkasının yazdığı metin). Bekleyen deploy: aşağıda.

**Profil üstü (Duolingo gibi):** avatar dairesiz, `avatarHeader` tam genişlikte avatarın arka plan
rengiyle; ad + ayarlar `profileTopPanel`'de ScrollView DIŞINDA sabit, aynı renkte, yazı/ikon zemine
göre koyu/beyaz; kaydırınca altta `profileTopDivider` çizgisi. Durum çubuğu
`MainActivity.setStatusBarTint` ile boyanıyor (Android 15+ statusBarColor'ı yok saydığı için
android.R.id.content'e şerit görünüm; currencyPanelDivider gizleniyor). Profil onResume/onPause/
onHiddenChanged'da açıp kapatıyor. Başkasının profilinde sabit panel yerine geri düğmeli
`otherUserProfileTopBar` var; o ve durum çubuğu da o kişinin avatar rengini alıyor (avatarı yoksa
uygulamanın koyu rengi).

**Son durum:** kullanıcı Personas'ı uygulamaya daha uygun buldu; Avataaars KALDIRILDI (parça dosyası,
lisans, AvatarStyle girişi). Yapı çok stilli kaldı; tek stil varken tür çipleri gizli. Ücret
önerisi: tüm stil değil, Personas içindeki bazı parçalar altın/Pro ile açılsın. "Yok" kutularında
forbidden_ic. Aşağıdaki ayrıntıların Avataaars kısımları artık geçmiş.

Kullanıcı cinsiyet sormak yerine özelleştirilebilir avatar istedi. Elle çizilen ilk deneme
beğenilmedi, kaldırıldı. Şimdi DiceBear'ın iki stili (9.4.2), Görevler → "Avatar Custom" kartında;
ekranın üstünde tür seçimi. Profile/alt bara bağlanması kullanıcının ONAYINI bekliyor.
- Avatar = Avataaars (Pablo Stanley; kişisel+ticari ücretsiz). Personas = Draftbit, **CC BY 4.0:
  ücretli satılabilir ama uygulamada "Personas by Draftbit" kaynak gösterimi ŞART** (henüz yok).
  Lisanslar design/avatar/*_LICENSE.txt. Kullanıcı Personas'ı ücretli yapmayı düşünüyor.
- Parçalar design/avatar/gen.mjs ile `assets/avatar/<stil>_parts.json`a çıkarıldı (renk
  `{{c:..}}`, iç parça `{{p:..}}` yer tutucu). gen.mjs mix-blend-mode'u (AndroidSVG desteklemiyor)
  yaklaşık normal karıştırmaya çeviriyor; Personas'ta yoğun kullanılıyordu.
- `AvatarStyle` (AvatarArt.kt) her türün sekmelerini, varsayılanlarını, tuvalini tanımlar;
  `AvatarArt.buildSvg` SVG kurar, `AvatarView` AndroidSVG (`com.caverock:androidsvg-aar:1.4`, YENİ
  bağımlılık) ile Picture'a çizer. Avataaars'ta şapka/gözlük kıyafet rengini, sakal saç rengini alır.
- `AvatarConfig(style, values)` seçenek ADLARINI tutar; `AvatarStore` her türün son hâlini ayrı +
  etkin türü tutar (yalnızca SharedPreferences, Firestore yok).
- Personas tarayıcıda (aynı SVG) kontrol edildi; AndroidSVG çıktısı telefonda görülmedi.

## Görevler ve Kupa Yolu sekmeleri (05.10.2026 — kuruldu, telefonda DENENMEDİ; commit edilmedi)

Keşfet sekmesi Kupa Yolu oldu; günlük soru, abaküs ve karakter kartı Görevler sekmesine geçti.
**İki sekme de TasksFragment** (`newInstance(MODE_MISSIONS / MODE_CUP)`, `isCupTab`): dönüş
altyapısı (finishTasksOverlayAnimated, yeni seri sorusu, reklam, tasksReturnTouchBlock,
reconcileAbacusOverlayWhenTasksIsBase) "alttaki ekran TasksFragment" varsayıyor; aynı sınıf
olunca akış kodu hiç taşınmadı (kullanıcıyla riskli yol olarak konuşuldu, seçilmedi).
- Görevler modu: görev listeleri → günlük soru (eski MissionsFragment → `MissionsSection` +
  `item_missions_sections.xml`, listede `BulletinRow.Missions`) → Abaküs → Karakter animasyonu.
- Kupa modu: yalnızca Kupa Yolu kartı; panel bugünkü BottomSheet (kullanıcı panelin sekmeye
  gömülmesini İSTEMEDİ, ileride başka kart eklenebilir diye kart kalsın dedi).
- Kupa farkı tüketimi (`consumePendingCupDeltaIfCupTab`), otomatik kupa yolu açılışı ve
  concealHiddenCupPanel yalnızca kupa modunda; günlük soru kartı tazelemesi yalnızca görev modunda.
- Sandık animasyonu ve Bize Ulaşın kartları silindi. MissionsFragment ve fragment_missions silindi.
- Görev sandığı artık TasksFragment tabanında kapanıyor → finishTasksOverlayAnimated'ın kayarak
  kapanış yolu (eskiden MissionsFragment tabanında anlık kapanış yolu).
- Denenecek: günlük soru dönüşü (yeni seri sorusu + kart tazelemesi sırası), abaküs pratiği
  kapanışı (çalışma süresi duruyor mu), görev sandığı, kupa testi gidiş-dönüş, haritadan otomatik
  kupa yolu yönlendirmesi, iki sekme arasında geçiş.

## Onaysız öğretmen hesabı kısıtlamaları (06.10.2026 — commit 726b6fc, push edildi; cihaza KURULMADI)

Kural: onaysız öğretmen hiçbir şey yapamaz. Karar tek yerde: `MainActivity.isUnapprovedTeacher()`
(rol = AuthManager önbelleği, onay = energyManager'ın uid'ye özel kaydı; kayıt yoksa onaysız sayılır).
- currencyPanel: altın/anahtar/can zaten tüm öğretmenlerde gizliydi; onaysızda ayrıca
  `streakContainer`, `creditIcon/Text` ve **`energyIconFrame`** (∞ rozetinin kabı) gizli.
  `applyTeacherCurrencyPanel()` her `checkSubscriptionAndUpdateEnergy` cevabında yeniden çalışır.
- Altın/anahtar 0: `UserWalletFirestore.visibleBalance` — dinleyici ve önbellek düzeyinde, yani
  panel, mağaza ve özelleştirme hep 0 görür ve harcayamaz. Sunucudaki bakiyeye dokunulmadı.
- Can zaten 0'dı (EnergyManager.isEnergyBlocked + sunucu hasInfiniteEnergy).
- Ders başlatma (normal ders, anlatımı tekrar izle, yarış) → `TeacherApprovalGate.showNotApprovedDialog`
  (askQuestion ile aynı pencere, "Destek ile İletişime Geç" butonu eklendi; konu + kullanıcı ID dolu).
- Kapı: `TeacherApprovalGate.blockIfUnapproved(context)` (uyarıyı gösterip true döner).
- Mağaza hiç açılmıyor (`openShopFragment` kapısı); `BillingManager.launchPurchase` de son kapı
  (Pro tanıtımı → plan ekranı yolu). Kullanıcının istediği `shopSuperCard` gizleme bu yüzden
  gereksiz kaldı ve kaldırıldı.
- Görevler/Kupa Yolu: kupa yolu, abaküs pratiği, günlük soru kartı ve günlük ödül alma kapıda.
  Karakter animasyonu (MascotPlayground) bilerek açık: izleme ekranı, ödül/can yok.
- Panelde "Hesabınız onay bekliyor" yazısı (`teacherApprovalPendingText`), dokununca aynı pencere.
- Sunucu (functions/index.js, push ile deploy tetiklendi, sonucu doğrulanmadı): `assertNotUnapprovedTeacher` →
  updateUserWallet (yalnız harcama), buyStreakFreeze, buyEnergyWithKeys. verifyRegistrationCode
  öğretmene `keys: 0` veriyor. Play satın alma doğrulaması bilerek engellenmedi (para alınmışsa
  ürün verilmeli). `AuthManager.registerTeacher` ölü kod (çağıran yok) — rules `keys == 1`
  istediği için ona dokunulmadı.
- Kurulmadı: telefon bağlantısı koptu, emülatörde yer yok.
- Sıradaki: kullanıcı onaylı öğretmen ayarlarını söyleyecek (onaylı öğretmende ∞ rozeti kalbi
  olmadan tek başına görünüyor — o sırada konuşulacak).

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

**Push = deploy (03.10.2026'da fark edildi):** (05.10: firestore.rules/indexes/storage değişikliği de push ile `deploy-rules.yml` üzerinden gidiyor.) `.github/workflows/deploy-functions.yml`,
`functions/**` değişikliği içeren her push'ta (`claude/**` dalları; çalışma dalı dahil)
`firebase-tools deploy --only functions` çalıştırıyor — yani seçici değil, BÜTÜN
fonksiyonlar gidiyor. Cloud Functions denetim kayıtlarında deploy'lar
`github-actions-deploy@numigo-new.iam.gserviceaccount.com` adına (ör. 2026-10-01T19:04Z).
"Deploy etmeden önce sor" anlaşması bu yüzden functions/ dokunan commit'lerin PUSH'unu da
kapsıyor. Aşağıdaki "yalnızca şu iki fonksiyon deploy edildi" ifadesi muhtemelen yanlıştı:
o push hepsini göndermiş olmalı.

**05.10.2026, avatar — DEPLOY EDİLDİ** (commit 7927804, push → "Deploy Cloud Functions" ve "Deploy Firestore & Storage Rules" iş akışları başarılı; rules push ile zaten gitmişti, elle deploy "up to date" dedi). Aşağısı o deploy'un içeriği:
- `functions/index.js` → `mirrorPublicProfile`: `PUBLIC_PROFILE_FIELDS`'a `avatarConfig` eklendi,
  `selectedAvatar` çıkarıldı. Deploy edilene kadar başkalarının avatarı listelerde/profilde
  görünmez (harfli daire / non_user); kendi avatarın etkilenmez. Mevcut publicProfiles
  dokümanları kullanıcı avatarını bir kez kaydedince güncellenir (trigger users yazımında).
- `firestore.rules` → users update: `avatarConfig` string ve < 400 karakter. Rules ayrı deploy
  (`firebase deploy --only firestore:rules`); push iş akışı yalnızca functions gönderiyor.

`claimCupPathChest` (kupa yolu sandık enderliği) kullanıcı tarafından deploy edildi; bunu
02.10.2026'da kullanıcı bildirdi, deploy zamanı Firebase konsolundan ayrıca **doğrulanmadı**.

`submitStreakDay` ve `buyStreakFreeze` 01.10.2026'da deploy edildi (seri dondurma).

`submitStreakDay`'in deploy edildiği **çıkarım** yoluyla saptandı, doğrudan görülmedi: seri
dokümanında `lastSeenAt` (yalnızca `reminderPatch` yazıyor, iki dalda da) `updatedAt`'ten
(yalnızca gün taşıyan transaction yazıyor) daha yeniydi — yani günsüz "buradayım" dalı
başarıyla çalışmış. Düzeltme öncesi o dal her seferinde çöküyordu. Emin olmak istersen
Firebase konsolundan fonksiyonun son deploy zamanına bak.

**Deploy etmeden önce kullanıcıya sor.**

## Bildirim altyapısı (05.10.2026 — yazıldı ve derlendi, cihazda DENENMEDİ, deploy EDİLMEDİ)

Uygulamada iki bildirim vardı ve ikisi de kararını kendi içinde veriyordu: sohbet mesajı
(`onMessageCreated`) ve akşam seri hatırlatması (`sendStreakReminders`). Yeni bildirim türleri
eklenmeden önce altyapı kuruldu — her yeni tür bu işi pahalılaştırdığı için önce yapıldı.

**Dört şey kuruldu:**

1. **Tür bazlı tercih.** Eskiden tek anahtar (`notifications_enabled`) üç türü de kesiyordu;
   oyun bildiriminden rahatsız olan kullanıcının kapatacağı şey ÖĞRETMEN MESAJLARI DAHİL her
   şeydi. Artık `chat` / `streak` / `reward` ayrı. `account` (deneme bitişi, kredi iadesi,
   sorunun durumu) bilerek kapatılamıyor — yalnızca Android kanal ayarından susturulabiliyor.
2. **Sessiz saatler** — yerel 21:00–08:00. Sohbet sessiz kanala düşüyor (`messages_quiet`;
   kanalın önem derecesi sonradan değiştirilemediği için ayrı kanal olmak zorunda), ödül
   bildirimi düşürülüyor, seri hatırlatması etkilenmiyor (saati kullanıcı seçiyor).
3. **Günlük tavan ve öncelik** — tavana tabi tek tür `reward`: günde 2, son yedi günde 5
   (kayan pencere). `chat` ve `account` işlemsel olduğu için tavansız; `streak` de tavansız,
   çünkü kullanıcının açıkça kurduğu alarm (saatini kendi seçti, günde bir).
   Tavanın SON slotu öncelikli konulara ayrılmış (`RESERVED_FOR_PRIORITY = 1`), yani normal
   bir ödül bildirimi günde 1'den fazla alamıyor. Öncelikli konu listesi şu an yalnızca
   `season_ending` — **kullanıcı kararı (05.10.2026):** sezonun son günü "sezon bitiyor" ve
   seri hatırlatması geçecek, madalya bildirimi düşecek.
   Rezervasyon seçildi çünkü bildirimler gün içinde ayrı ayrı tetikleniyor; hepsini görüp
   aralarından seçeceğimiz bir an yok ve rezervasyon olmadan tavan "ilk gelen geçer" demekti.
4. **Ölçüm.** Bildirime dokunma hem Analytics'e (`notification_opened`) hem sunucudaki deftere
   yazılıyor. Defter aynı zamanda karar veriyor: üst üste 5 kez açılmayan **konu** susuyor
   (tür değil konu — "sezon bitiyor" ile "sandığın bekliyor" ikisi de `reward`).
   Susturma YALNIZCA `reward`'da (`mutable` alanı). Ötekilerde ters çalışıyordu: bildirimi
   görüp uygulamayı kendi açan kullanıcı "dokunmamış" sayılıyor, yani seri hatırlatması işini
   düzgün yaptığı için beş günde kendini kapatırdı.

**Dosyalar**

- `functions/notifications.js` — saf karar: tür kataloğu, tercih çözümleme, sessiz saat,
  tavan, susturma. Yan etkisiz olması kasıtlı (bkz. `streakReminder.js` aynı gerekçe).
- `functions/index.js` — `sendUserNotification()` tek gönderim kapısı. Yeni bildirim ekleyen
  kod bunu çağırmalı; doğrudan `admin.messaging()` çağırmak tercih ve tavanı atlar.
  Ayrıca `recordNotificationOpen` callable ve ölü token temizliği (`pruneDeadToken`).
- `app/.../NotificationPrefs.kt` — istemci tercihleri (SharedPreferences + Firestore).
- `app/.../MyFirebaseMessagingService.kt` — tür/kanal kataloğu, `notifyType` ayrıştırma,
  token ile birlikte `utcOffsetMinutes` yazımı (sessiz saat sunucuda buna bakıyor).
- `firestore.rules` — `users/{uid}/notifyLedger/{doc}`: istemci okur, yazamaz.

**GERİYE DÖNÜK UYUM — bozulmaması kritik olan iki yer**

- Sunucu `notifyType` gönderiyor ama eski `type` alanına DOKUNMUYOR: sahadaki eski istemciler
  seri hatırlatmasını `type == "streak_reminder"` ile tanıyor. Adı ezilse o cihazlarda
  bildirim sessizce düşerdi.
- Tercih önceliği: `notificationPrefs.<tür>` > eski `notificationsEnabled` > açık. Daha özel
  olanın kazanması şart, yoksa evdeki eski sürümlü tablet kullanıcının seçimini geri alır.
  Aynı kural iki tarafta: `notifications.js → notificationPrefsFor` ve
  `NotificationPrefs.isEnabled`.

**Test:** `cd functions && npm run test:notifications` — 64 kontrol, emülatör gerektirmiyor.
Sezonun son günü senaryosu da burada, gün içindeki gerçek sırayla.
Testler yazarken iki gerçek hata yakalandı:
- `Number(null) === 0` olduğu için "saat dilimi bilinmiyor" ile "kullanıcı UTC'de" ayırt
  edilemiyordu; farkı bilinmeyen kullanıcının gece bildirimi düşüyordu (`normalizeOffset`).
- Seri hatırlatması susturulabilir türdeydi (yukarıdaki 4. madde).

**DENENMEYEN.** Hiçbiri cihazda görülmedi ve deploy edilmedi:
- Ayarlardaki üç yeni anahtar ve ana anahtarla iki yönlü bağı.
- Sessiz saatte sohbet bildiriminin sessiz kanala düşmesi (cihaz saatini 22:00'ye almak
  yeterli — sunucu `users/{uid}.utcOffsetMinutes` alanına bakıyor, o alan token ile birlikte
  yazılıyor, yani uygulamayı bir kez açıp kapatmak gerekiyor).
- Tavanın dolması ve rezerve slot. Pratikte ÖLÇÜLEMEZ durumda: tavana tabi tek tür `reward`
  ve henüz tek bir ödül bildirimi yazılmadı. İlk ödül bildirimiyle birlikte denenmeli.
- `recordNotificationOpen` çağrısı ve defterin dolması.

**FCM token tavanı 2 → 3 cihaz** (kullanıcı kararı, 05.10.2026). Tipik kurulum ailede
tablet + çocuğun telefonu + ebeveyn telefonu; 2 sınırıyla en eskisi listeden sessizce
düşüyor ve o cihaz bildirim almayı tamamen kesiyordu — hiçbir hata üretmeden. Sınır iki
yerde ve birlikte değişmek zorunda: sunucuda `FCM_MAX_DEVICES` (functions/index.js),
istemcide `MAX_FCM_DEVICES`. Sunucu daha azını okursa istemcinin kaydettiği cihaz sessizce
bildirim almaz. **Cihazda denenmedi** — üçüncü bir cihaz gerekiyor.

**Sonraki adım (kullanıcıyla kararlaştırılan sıra):** deneme bitişi + öğretmen havuzu → soru
durumu → seri dondurma + kırılma öncesi ikinci şans → ödüller (sandık, sezon, enerji).

## Öğretmen havuzu bildirimi (06.10.2026 — yazıldı ve derlendi, cihazda DENENMEDİ)

Öğretmen tarafı bu bildirime kadar tamamen sessizdi: soruyu SAHİPLENDİKTEN sonraki mesajları
duyuyordu (`onMessageCreated`), havuza düşen soruyu duymuyordu — havuzu görmenin tek yolu
uygulamayı açıp Havuz sekmesine bakmaktı. Boşluk doğrudan paraya dokunuyor: 48 saat içinde
cevaplanmayan soru öğrencinin kredisini iade ediyor (`QUESTION_REFUND_AFTER_MS`).

- `functions/index.js` → `notifyTeacherPool`, 10 dakikada bir. İmleç `system/teacherPoolNotify`
  (`lastScanMs`). Soru başına tetikleyici DEĞİL, tarama: bir sınıfın aynı akşam soru sorması
  öğretmene üst üste bildirim yığardı; aradaki bütün yeni sorular tek bildirimde.
- İlk çalıştırmada bildirim GÖNDERİLMİYOR, yalnızca imleç kuruluyor — yoksa ilk tarama
  havuzdaki birikmiş bütün eski soruları "yeni" sayardı.
- Yeni tür `pool` (functions/notifications.js): `pref: null` (öğretmenin işi, öğrenciye
  yönelik tercih listesinde yeri yok), tavansız, gece sessiz kanala düşüyor — düşürmek
  olmazdı, 48 saatlik pencerede her saat önemli.
- Sorgu `(status, createdAtMs)` indeksini kullanıyor. **BU İNDEKS YOKTU ve tarama ilk turunda
  düştü** — bkz. yukarıdaki "Kredi iadesi bir aydır hiç çalışmıyormuş". İndeks 06.10.2026'da
  eklendi; aynı eksik bir aydır kredi iadesini de durduruyordu.
- Öğretmenler `role == 'TEACHER'` ile çekilip `teacherApproved` kodda filtreleniyor: iki
  eşitlik filtresi yeni bir bileşik indeks isteyebilirdi, öğretmen sayısı küçük.
- Metin `teacherPoolText` (saf, testli): toplam yeniye eşitse tekrar etmiyor, onun yerine
  iade penceresini hatırlatıyor.

- Bildirime dokunmak doğrudan **Havuz sekmesini** açıyor
  (`NotificationFragment.newForTeacherPool`, `EXTRA_OPEN_TEACHER_POOL`). `teacherTab`
  varsayılanı zaten POOL ama yetmiyordu: öğretmen son olarak Sohbetler sekmesinde kaldıysa
  kaydedilmiş durum onu geri getiriyordu.

**DENENMEYEN:** Cihazda hiç görülmedi. Denemek için bir öğrenci hesabından soru sor, 10 dakika
içinde öğretmen cihazına bildirim düşmeli. **İlk deploy'dan sonraki İLK tarama imleci kurar ve
bildirim göndermez** — ikinci turu beklemek gerekiyor.

## KREDİ İADESİ BİR AYDIR HİÇ ÇALIŞMIYORMUŞ (06.10.2026 — eksik indeks, düzeltildi)

Bildirim deploy'u sonrası logları okurken çıktı ve bildirimlerle ilgisi yok: **48 saatlik
kredi iadesi taraması (`reconcileUnansweredQuestions`) 04.09.2026'dan beri her saat
`FAILED_PRECONDITION` ile düşüyordu.**

```
Error: 9 FAILED_PRECONDITION: The query requires an index.
```

**Sebep:** `runUnansweredQuestionRefund` sorgusu `status == 'pending'` + `createdAtMs < cutoff`
yapıyor ama `firestore.indexes.json`'da `createdAtMs` alanı için HİÇ indeks yoktu. Var olan
indeks `(status, createdAt DESCENDING)` — `createdAt` bir Timestamp, `createdAtMs` ise sayı;
ayrı alanlar, ayrı indeksler. `createdAtMs` sorgusu `8fc4e2e` (04.09.2026) ile geldi ve
indeksi hiç eklenmemiş.

**Sonucu:** cevapsız kalan soruların kredisi öğrencilere bir aydır geri verilmiyordu. Hata
sessiz: fonksiyon düşüyor, kullanıcı tarafında hiçbir belirti yok, kimse şikâyet etmiyor
çünkü krediyi beklediğini bilen yok.

**Nasıl görüldü:** aynı eksik indeks yeni `notifyTeacherPool` taramasını da düşürdü (o da
`(status, createdAtMs)` sorguluyor). Yeni fonksiyonun hatası, bir aydır duran eski hatanın
üstünü açtı.

**Düzeltme:** `(status ASC, createdAtMs ASC)` indeksi `firestore.indexes.json`'a eklendi.
İndeksler `deploy-rules.yml` iş akışıyla gidiyor, yani push yeterli.

**DİKKAT — indeks oluşunca biriken iadeler bir kerede işlenecek.** Bir aydır iade edilmemiş
sorular varsa tarama hepsini aynı turda iade eder ve her biri için öğrenciye "kredin geri
verildi" bildirimi gider (bildirim bugün eklendi, `account` türü tavansız). Tek test
kullanıcısında sorun değil; yayında olsaydı bir kullanıcıya üst üste birkaç bildirim
gidebilirdi.

**DERS:** yeni bir Firestore sorgusu yazarken indeksin VAR OLDUĞUNU varsaymak yetmiyor,
`firestore.indexes.json` içinde alan adıyla aranmalı. Bu turda ben de aynı hatayı yaptım —
"(status, createdAtMs) indeksi zaten var, iade taraması kullanıyor" diye yazdım; iade
taraması onu kullanmıyordu, çünkü indeks hiç yoktu.

### İkinci eksik indeks: `secondReminderHourUtc` (aynı gün, aynı sebep)

Yukarıdaki indeks eklendikten sonra canlı indeks listesi okunurken çıktı. İkinci şans
hatırlatması `collectionGroup('streak').where('secondReminderHourUtc', '==', hourUtc)`
sorguluyor ve **tek alanlı collectionGroup sorguları otomatik indekslenmiyor** — açıkça
`fieldOverrides` içinde `queryScope: COLLECTION_GROUP` tanımlanmak zorunda. İkizi
`reminderHourUtc` için tam bu yüzden bir override vardı; yeni alan için eklenmemişti.

Tarama saatlik olduğu için henüz çalışmamıştı, yani hata loglara hiç düşmedi: ilk turunda
`FAILED_PRECONDITION` verecekti. Override eklendi.

**Kural olarak yazalım:** `streak` (ya da başka bir alt koleksiyon) üzerinde yeni bir
collectionGroup sorgusu eklenirse, filtrelenen alan için `fieldOverrides`'a
`COLLECTION_GROUP` kapsamlı indeks eklenmeli. Normal koleksiyon sorgularında bu gerekmiyor,
tek alan indeksleri otomatik.

## Kalan bildirimlerin hepsi (06.10.2026 — yazıldı ve derlendi, HİÇBİRİ cihazda DENENMEDİ)

Kullanıcı "testlerle uğraşmak istemiyorum, sen bildirimleri ekle, ileride test'teyken zaten
görürüz" dedi. Plandaki 4. ve 5. maddenin tamamı bir turda yazıldı. **Canlıda
doğrulanacakların listesi `docs/YAYIN_ONCESI_KONTROL.md` → "Yayından sonra canlıda
doğrulanacaklar" bölümünde.**

### Seri: dondurma varyantı ve kırılma öncesi ikinci şans

- **Dondurma varyantı** (`streakReminder.js` → `streakReminderText`): dondurması olan
  kullanıcıya akşam hatırlatması "Bugün N dakika çalış, **dondurmanı yarına sakla**" diyor.
  Dondurma 4000 altın ve yarın harcanacak; bilgi aynı, ton davet — "harcanacak" yerine
  "sakla".
- **İkinci şans** (`secondChanceDecision`): yerel **20:00**, sessiz saatlerin bir saat
  öncesi. Dört şart birden: serisi ≥ 3, dondurması YOK, birinci hatırlatma bugün gitmiş,
  hedef hâlâ tutturulmamış. Bu şartlar olmadan günde iki hatırlatma demek olurdu.
- Saat ayrı bir alandan geliyor: `secondReminderHourUtc` (sabit yerel 20:00, kullanıcının
  seçimine bağlı DEĞİL). `reminderPatch` yazıyor. **Mevcut kullanıcılarda alan yok**, bir kez
  `submitStreakDay` çağrılınca doluyor — yani ikinci hatırlatma onlara hemen gitmiyor,
  kendiliğinden düzeliyor.
- Tarama tek fonksiyonda iki geçiş (`STREAK_REMINDER_PASSES`): aynı token/gönderim/işaretleme
  gövdesini kopyalamamak için. İkisi de `data.type = streak_reminder` gönderiyor, yani eski
  istemciler ikisini de tanıyor ve aynı bildirim kimliğini paylaşıyorlar — akşam gelen ikinci
  hatırlatma birincinin yerini alıyor, çekmecede iki seri bildirimi yığılmıyor.

### Sezon bitişi (`sendSeasonEndingNotices`, saatte bir)

Kullanıcı tasarımı: "bitmesine 4 saat kala herkese, sıranı gözden geçir."

**Metinde sabit "4 saat" YOK ve bu bilinçli.** Sezon bitişi sabit bir UTC anı
(`SEASON_ANCHOR_UTC_MS`), yani "4 saat kala" herkes için aynı an: Endonezya (+7) için 15.05,
Türkiye (+3) için 11.05, ABD batı yakası (-7) için **01.05**. Gece bildirimi bu özelliğin
amacını bozar, ertelemek de işe yaramaz (sezon bitiyor). Çözüm: pencere 2–12 saat, gönderim
kullanıcının yerel saatiyle 10.00–21.00 arasında, metin O ANDA kalan gerçek süreyi söylüyor
("Sezon 4 saat sonra bitiyor"). Metin GÜN adı söylemiyor ("yarın"/"bugün"): aynı UTC anı bazı
dilimlerde bugün, bazılarında yarın.

Pencere dışındaysa **hiç okuma yapılmıyor** — bildirim herkese gittiği için tarama bütün
kullanıcıları gezmek zorunda ve bu her saat yapılamaz. Sezon haftalık, pencere ~10 saat.

Sezon başına tek gönderim: `notifyLedger.seasonNotice` = sezon numarası (bayrak değil, yani
sonraki sezonda yeniden gidiyor). Öncelikli konu (`PRIORITY_TOPICS`), yani tavanın son slotu
bunun için ayrılmış.

### Sezon madalyası (`finalizeSeasonLeaderboardMedals` içinde)

"Sezon bitti, madalyan hazır." **Yalnızca madalya KAZANANA** gidiyor; kazanmayana
gönderilmiyor, çünkü açtığında hiçbir şey bulamayan kullanıcı o bildirime bir daha güvenmez.
Ayrım bedava — kimin ne kazandığı zaten hesaplanıyor (`addedSomething`).

Bildirim **transaction'ın DIŞINDA**: Firestore çakışmada transaction gövdesini yeniden
çalıştırıyor, içeride gönderilse bildirim ikinci kez giderdi.
`scheduleFinalize(functions, admin, db, notify)` — bildirim gönderici dışarıdan geçiyor,
çünkü modül `index.js`'teki `sendUserNotification`'a erişemiyor.

### Enerji doldu (`sendEnergyFullNotices`, saatte bir)

Yerel **15.00–21.00** penceresi, yani okul sonrası. Bu kataloğun en açık "geri dön ve oyna"
bildirimi ve kitle 7–10 yaş: okul saatinde çocuğun telefonunu titretmek savunulamaz ve Play
Families tarafında risk. Sonsuz enerjisi olanlara (Pro, Premium, onaylı öğretmen)
gönderilmiyor — onlar için dolum diye bir şey yok.

Sorgu `energy_full_time` üzerinde aralık (son ~70 dakikada dolanlar); yeni indeks gerekmedi.
"Enerjisi dolu olan herkes" sorgulanamazdı — çoğu kullanıcının enerjisi dolu.

### Bekleyen kupa yolu sandığı (`sendPendingChestNotices`, GÜNDE BİR, UTC 14.00)

Eşiği geçip sandığını açmamış kullanıcıya. Kataloğun en zararsız bildirimi: yeni bir iş
istemiyor, zaten kazanılmış bir ödülü hatırlatıyor.

**Neden günde bir:** tarama kullanıcı başına iki alt koleksiyon okuması gerektiriyor (kupa
puanı + sandık defteri). Saatlik çalışsa aynı okumalar günde 24 kez yapılırdı ve bildirimin
değeri bu maliyeti karşılamıyor. Yerel saat filtresi yok ve gerekmiyor: tür `reward`, yani
sessiz saate denk gelen kullanıcıda kendiliğinden düşüyor.

### Test

`npm run test:notifications` 99'dan **126 kontrole** çıktı; `test:reminder` 32'den 48'e.
Sunucu test takımının sekizi de geçiyor, istemci derlemesi temiz.

Testleri yazarken `grabConst` yardımcısında bir tuzak çıktı ve düzeltildi: tek satırlık regex
çok satırlı sabitleri (`const CUP_PATH_FIELDS = [` ... `];`) bulamıyordu. Artık parantez
dengesi takip ediliyor.

## Soru durumu bildirimleri (06.10.2026 — yazıldı ve derlendi, cihazda DENENMEDİ)

Öğrenci sorusunun ne olduğunu yalnızca uygulamayı açarsa öğreniyordu. Üç geçiş de onun için
bir haberdi ve üçü de sessizdi. Hepsi `account` türü: tavansız, uygulama içinden
kapatılamaz — kaçırmak kullanıcının aleyhine ve yerine geçecek kanal yok.

- `functions/index.js` → `questionStatusNotice` (saf, testli) + `notifyQuestionStatusChange`,
  `onQuestionUpdated` trigger'ı içinden çağrılıyor.
- **pending → claimed:** "Öğretmenin sorunu aldı / Cevap geldiğinde haber vereceğiz."
- **→ resolved:** "Sorun çözüldü olarak işaretlendi / Öğretmenin cevabını sohbette
  okuyabilirsin." Çözüldü işaretlemesi yalnızca ÖĞRETMENE açık (`updateTeacherClaimUi`,
  `resolveButton` öğrenciye hiç görünmüyor), yani bildirim öğrenciye gidiyor ve doğru taraf.
- **→ expired, `creditRefunded == true`:** "Soruna cevap gelemedi / 1 danışma kredin geri
  verildi." `expired` tek başına yetmiyor: kredi harcanmadan oluşmuş eski sorular iade
  edilmeden kapanabiliyor.

**ÇÖZÜLDÜ BİLDİRİMİNDEKİ İNCELİK**
Öğretmen çoğunlukla son cevabını yazıp hemen ardından soruyu kapatıyor; o mesaj için sohbet
bildirimi ZATEN gitti. Son mesajın üstünden `RESOLVED_NOTICE_QUIET_MS` (5 dk) geçmediyse
"çözüldü" bildirimi gönderilmiyor — aynı olayı iki kez haber vermemek için.

**TRIGGER'A NASIL EKLENDİ — dikkat edilen yer**
`onQuestionUpdated` silme işi yapıyor ve erken dönüşlerle dolu (yalnızca iki taraf da
"listeden sil" dediyse çalışıyor). Bildirim çağrısı o mantığın İÇİNE değil ÖNÜNE konuldu;
içine girseydi geçişlerin çoğunda hiç çalışmazdı. Silme mantığına dokunulmadı.

**BİLDİRİM KİMLİĞİ ÇAKIŞMASI — kolayca gözden kaçacak bir tuzak**
Soru durumu bildirimi `"account:$questionId".hashCode()` kullanıyor. Önek şart: soru sohbeti
bildirimleri de `questionId.hashCode()` kullanıyor ve önek olmasa öğretmenin yazdığı mesajın
bildirimi, peşinden gelen durum bildirimiyle SİLİNİRDİ. Aynı sorunun ardışık durum
bildirimleri ise bilerek aynı kimliği paylaşıyor (sorunun güncel durumu tek bildirimde);
farklı soruların bildirimleri birbirini ezmiyor.

**DENENMEYEN.** Cihazda hiç görülmedi. Denemek için öğretmen hesabından bir soruyu sahiplen
(öğrenci cihazına "öğretmenin sorunu aldı" düşmeli), sonra 5 dakika bekleyip çözüldü işaretle.
Kredi iadesi 48 saat beklemek yerine Firestore Console'dan `createdAtMs` alanını 48 saatten
eskiye çekip saatlik taramayı beklemekle denenebilir.

## Deneme süresi bitiş bildirimi (06.10.2026 — yazıldı ve derlendi, cihazda DENENMEDİ)

Bir büyüme fikri değil, kapatılan bir AÇIK: reklam atlama panelinde kullanıcıya "Deneme süren
sona ermeden önce bildirim alacaksın" deniyor (`AdSkipFragment`, deneme hakkı olan sürüm) ama
hiçbir bildirim gönderilmiyordu. `playSubscriptionNotification` planı güncelliyor, kullanıcıya
hiçbir şey söylemiyordu. Ödeyen taraf ebeveyn; haber verilmeden başlayan ilk ödeme iade talebi
ve mağaza yorumu üretir.

- `functions/index.js` → `sendTrialEndingNotices`, saatte bir. Eşikler **3 gün ve 1 gün**
  (`TRIAL_NOTICE_DAYS`), yalnızca kullanıcının yerel saati 10:00–21:00 arasındayken
  (`TRIAL_NOTICE_LOCAL_HOURS`). Saatlik olmasının sebebi bu: günlük tarama tek bir UTC anında
  herkese gönderir ve bazı kullanıcıda gece yarısına denk gelirdi.
- Tür `account`: kapatılamıyor, tavansız.
- Tekrar engelleme defterde: `notifyLedger.trialNotice = { "3": <bitiş ms>, "1": ... }`.
  Bayrak değil BİTİŞ ZAMANI kaydediliyor — kullanıcı yeni bir deneme alırsa (farklı bitiş)
  bildirim yeniden gidiyor.

**DENEME TESPİTİ — kırılgan yer, bilinmesi gereken**
Play Developer API v2 (`purchases.subscriptionsv2.get`) denemeyi söyleyen bir alan
döndürmüyor; v1'deki `paymentState: 2` karşılığı yok (web'den doğrulandı). Elde olan tek
sinyal `lineItems[].offerDetails.offerId`: bir teklif uygulanmışsa dolu.
**Kullanıcı kararı (06.10.2026):** Play Console'da Pro aboneliğinde ücretsiz deneme dışında
teklif olmadığı için `isTrialPeriod` "teklif uygulanmış = deneme" kabul ediyor.
İleride indirimli ilk ay / tanıtım fiyatı gibi bir teklif eklenirse bu fonksiyon onları da
deneme sayar ve o kullanıcılara yanlış bildirim gider. O gün yapılacak: deneme teklifine
Console'da bir etiket vermek ve `offerTags`'e bakmak.

`planTrialEndsAt` alanı `syncSubscriptionForToken` içinde yazılıyor — plan yazımının TEK yolu
orası, yani hem satın alma doğrulaması hem Play RTDN kapsanıyor. Deneme bitip ilk ödeme
alındığında Play artık teklif döndürmediği için alan kendiliğinden null'a dönüyor. Alan
`firestore.rules` → `serverOnlyFields()` listesinde: istemci yazamaz.

**Yenileme bildirimi bilerek YOK** (kullanıcı kararı): denemede olmayan abonelere "aboneliğin
yenilenecek" gönderilmiyor. Play yenileme e-postasını zaten atıyor ve aylık abonede dönem
başına iki bildirim değerli bir şey söylemiyor.

**`planTrialEndsAt` NEDEN BOŞ GÖRÜNÜYOR** (kullanıcı 06.10.2026'da sordu)
Alan geriye dönük YAZILMIYOR. Yalnızca bir abonelik doğrulaması çalıştığında yazılıyor:
yeni satın alma, Play yenileme bildirimi (RTDN) ya da istemcinin yeniden doğrulatması. Yani
mevcut bir abonelik varsa bile, deploy'dan sonra hiç doğrulama çalışmadıysa alan yok. Ayrıca
alan yalnızca DENEME dönemindeyken doluyor; ödenmiş dönemde `null`.

**NASIL TEST EDİLİR — yayını beklemek gerekmiyor, iş ikiye ayrılıyor**

*Tespitin çalıştığı (şimdi, test aboneliğiyle):* Pro test aboneliği al ve Firestore →
`users/{uid}` → `planTrialEndsAt`'e bak; Cloud Functions logunda "Abonelik senkronu" kaydında
`trial` ve `offerId` yazıyor (06.10.2026'da log'a eklendi, tam bu yüzden). Deneme hakkı
duruyorsa `trial: true` ve `offerId` dolu gelmeli — `isTrialPeriod`'un gerçek Play yanıtında
çalıştığının kanıtı bu. Deneme hakkı tükenmişse `trial: false` gelir ve tespit doğrulanmaz.

*Bildirimin gittiği (şimdi, elle):* Test aboneliğinde deneme 5 dakikada bitiyor, yani 3
gün / 1 gün eşikleri HİÇ yakalanmıyor. Bildirimi görmek için Firestore Console'dan alanı elle
yaz — `node -e "console.log(Date.now() + 2.5*86400000)"` çıktısını `users/{uid}` dokümanına
`planTrialEndsAt` (number) olarak gir. Koşullar: `utcOffsetMinutes` dolu olmalı (uygulamayı
bir kez aç), yerel saat 10.00–21.00 arasında olmalı, `notifyLedger/state` dokümanında
`trialNotice` olmamalı. Tarama saatte bir çalışıyor, yani en fazla bir saat. 0.5 güne ayarlayıp
"yarın bitiyor" varyantı da görülebilir (önce `trialNotice` silinmeli).

*Uçtan uca (yayından sonra):* bkz. `docs/YAYIN_ONCESI_KONTROL.md` → "Yayından sonra canlıda
doğrulanacaklar".

**Test:** `npm run test:notifications` 88 kontrole çıktı (deneme tespiti, eşik seçimi, yerel
saat penceresi, tekrar engelleme, havuz metni). Testleri yazarken `grabFunction`
yardımcısında bir tuzak çıktı: destructuring parametresi (`function f({ a }, b)`) gövdenin ilk
süslü parantezi sanılıyor ve fonksiyon parametre listesinin ortasında kesiliyor. Buradaki
kopya düzeltildi; **`test-streak-freeze.js`'teki kopyada düzeltme YOK** — orada
destructuring'li bir fonksiyon çekilirse aynı tuzağa düşer.

**Sezon bitişi bildirimi — kullanıcının verdiği tasarım, henüz yazılmadı:**
madalya alanlara "Sezon bitti, madalyan hazır"; bitişe 4 saat kala herkese "Sezon 4 saat sonra
bitecek, sıranı gözden geçir". Susturma şartı zaten altyapıda (`UNOPENED_MUTE_AFTER = 5`).
Yazılırken çözülmesi gereken: sezon bitişi sabit bir UTC anı (`SEASON_ANCHOR_UTC_MS`), yani
"4 saat kala" bazı saat dilimlerinde gecenin bir yarısı — ertelemek işe yaramaz (sezon
bitiyor), öne almak gerekir.

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

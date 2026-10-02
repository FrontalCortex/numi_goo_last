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

## Kupa testi dönüşünde yeni seri sorusu (02.10.2026)

Görevler ekranındaki kupa yolu kartlarından açılan test (kupa modu, bölüm 9) nasıl kapanırsa
kapansın (doğru, yanlış, çıkış düğmesi, geri tuşu) serisi olmayan kullanıcıya
`NewStreakFragment` açılıyor. Koşul haritadaki ders dönüşüyle aynı
(`StreakRepository.needsNewStreakPrompt`: seri 0 ve bugün sorulmadı).

**Neden ders sonrası kuyruğunun içinde değil:** kuyruk (`pumpPostLessonQueue`) yalnızca harita
tabanında çalışıyor; kapısı ve adımlarının çoğu haritaya özel. Kupa testi Görevler'den açılıp
oraya dönüyor. Kuyruğa dokunulmadı; `MainActivity`'de kendi küçük kapısı ve bekleyişi var
(`requestNewStreakPromptAfterCupTest` → `runCupNewStreakPrompt` → `cupNewStreakBlockReason`).

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
   Blok önce yeni seri isteğini gönderiyor, sonra kupa panelini `runWhenCupResultUncovered`
   ile bekletiyor (`MainActivity.isCupResultCovered`: soru sırada/ekranda ya da Pro paneli
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
   (`cupResultTouchBlocker`, yükseklik 9.5dp). Görevler, alt çubuk ve para panelini kapatıyor
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
   dokunuşta `TasksFragment.needsCupResultTouchBlock()`'u soruyor; gerekmiyorsa kendini
   kaldırıyor (logda `kupa dokunma engeli SAHIPSIZ`). Normal açılıp kapanışı
   `kupa dokunma engeli ACIK / KAPALI` satırlarıyla görülüyor (`PostLessonQueue` etiketi).

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

**Kapsam dışı (bilerek):** günlük soru. O da Görevler'den açılıyor ama istenen yalnızca kupa
yolu kartlarıydı.

**Test anahtarı:** koşulları elde etmek zor (yeni hesapta kayıt akışı soruyu o gün için
işaretliyor, serisi olan hesapta kırılmayı beklemek gerekiyor).
`NewStreakPromptDebug.FORCE = true` iki koşulu da atlıyor; depoda `false` durmalı. Logda
`PostLessonQueue` etiketiyle `kupa yeni seri SIRADA / bekliyor | block=… / ACILIYOR /
DUSURULDU` satırları.

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
"sorun yok" dedi): logda `kupa yeni seri GEREKMIYOR`; reklamlı dönüşte panel reklam kapanır
kapanmaz dokunulabilir geldi; rozetli dönüşte engel 1,36 sn sonra kutlamayla birlikte kalktı;
çıkışla kapanan testte 9 ms'de kalktı. Rozetsiz dönüşlerde engel 0,9–1,36 sn sürdü: çocuk
testi hızlı kapatınca rozet listesi henüz gelmemiş oluyor, üç dönüşte süre sınırı
(`CUP_BADGE_TOUCH_HOLD_MS`) doldu. Yani o sınır nadir bir yedek değil, sık çalışan yol.
Çökme, sahipsiz pencere ya da `SAHIPSIZ` kaydı yok.
**Doğrulanmayan:** anahtar KAPALIYKEN gerçek koşullarla (seri 0 + bugün sorulmadı) sorunun
açılması; rozet listesinin süre sınırından SONRA rozetle geldiği durum (kutlama, çocuk bir
şeye başlamadıysa açılmalı, başladıysa atlanmalı — denk gelmedi).

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

```powershell
firebase deploy --only functions:claimCupPathChest
```

`claimCupPathChest` (kupa yolu sandık enderliği) deploy edilmedi.

`submitStreakDay` ve `buyStreakFreeze` 01.10.2026'da deploy edildi (seri dondurma). Onay
yalnızca bu ikisi için alınmıştı; `claimCupPathChest` bilerek dışarıda bırakıldı.

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
| `PostLessonQueue` | Ders sonrası ekran kuyruğu: kilit, zemin, `bekliyor \| block=...`. Kupa testi dönüşü de burada: `kupa yeni seri SIRADA / GEREKMIYOR / ACILIYOR / DUSURULDU`, `kupa dokunma engeli ACIK / KAPALI / SAHIPSIZ` |

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

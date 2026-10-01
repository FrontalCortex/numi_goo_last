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

**Doğrulanmayan:** cihazda hiçbir şey. Telefon kilitliydi, sonra adb bağlantısı koptu. Kart
ve ikon yalnızca bilgisayarda başsız tarayıcı önizlemesiyle görüldü. Bakılacaklar:

1. Mağaza → "Özel Teklifler" en alt: kart, ikon, `4000` düğmesi.
2. Satın al (onay penceresi çıkmalı) → altın 4000 düşmeli, düğme "✓ HAZIR" olmalı, seri
   ekranında "Seri dondurma: Hazır" yazmalı. Logda `StreakDiag … Repo.dondurma | SATIN_ALINDI`.
3. Deploy sonrası `submitStreakDay`'in gerçek bir çağrısı: `StreakDiag … Sync.cevap | BASARILI`
   (günde bir kez gidiyor; o gün zaten gittiyse ertesi gün görünür).
4. Asıl davranış: dondurma al → ertesi gün **hiç çalışma** → öbür gün aç. Beklenen: "seri
   dondurman serini korudu" bildirimi, hafta şeridinde kaçan günde kar tanesi, seri sayısı
   aynı, mağazada düğme yeniden `4000`. Logda `Repo.dondurma | HARCANDI … seriKurtuldu=true`.
   **Saati ileri alarak deneme** — sunucu istemcinin gününü ±1 günden fazla sapınca kabul
   etmiyor, iki taraf ayrışır.

**Yapılmayan, fikir olarak duran:** dondurma harcandığında toast yerine küçük bir kutlama
ekranı; seri ekranındaki "Seri dondurma: Yok" satırına dokununca mağazaya gitmek; akşam
hatırlatmasının "dondurman var" diyen bir çeşidi.

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
| `PostLessonQueue` | Ders sonrası ekran kuyruğu: kilit, zemin, `bekliyor \| block=...` |

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

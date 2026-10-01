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

## Sıradaki iş 1 — chrome kilidi asimetrisi (teşhis tamam, düzeltme yapılmadı)

**Belirti:** Ders bitip haritaya dönüldüğünde harita tıklanabiliyor ama `bottomNavigationID`
ve `currencyPanel` tıklanamıyor.

**Kanıt** (`MapTouchDbg` + `ChromeBlockerDbg`, 01.10.2026 20:21):

```
chromeLockDepth=2
bottomNav enabled=false clickable=false chromeLocked=true
mapTouch=(transparentOverlayAttached=false, touchRoutingEnabled=true)   ← harita serbest
[MapFragment.onResume] STUCK_CHROME_LOCK? depth=2
[MapFragment.enableMapFragmentViews] release → depth=1 appliedUnlock=false
[MapFragment.enableMapFragmentViews] release → depth=0 appliedUnlock=true
```

**Kök neden:** `MapFragment`'te kilidi alan ve bırakan fonksiyonlar çapraz eşleşmiş ve alan
tarafta koruma yok.

| fonksiyon | satır | acquire/release | koruma |
|---|---|---|---|
| `disableMainActivityViews()` | 1436 | **acquire** | **yok** |
| `disableMapFragmentViews()` | 1493 | yok | var (`if (mapTransparentTouchBlockActive) return`) |
| `enableMapFragmentViews()` | 1525 | **release** | — |
| `enableMainActivityViews()` | 1553 | yok (sonunda `enableMapFragmentViews()` çağırıyor) | — |

`disableMainActivityViews()` üç yerden çağrılıyor ve ikisi aynı bekleyen overlay için birlikte
tetiklenebiliyor:

- `lockTouchForPendingOverlay()` (1792) — rozet/rehber gösterileceği kesinleşti, içerik hazır değil
- `notifyVisibleAfterOverlayDismiss()` EAGER LOCK (1799 içinde) — rehber ya da AskQuestionOpen bekliyorsa
- `maybeShowPendingMarathonGuide` (1391)

İkisi çalışınca `depth=2`; tek `enableMainActivityViews()` gelirse `depth=1` kalıyor ve chrome
kilitli kalıyor. Kullanıcıyı kurtaran şey `forceEnableMapTouchRouting()` (1775): içinde hem
`enableMapFragmentViews()` hem `enableMainActivityViews()` çağırdığı için **iki kez** release
ediyor ve çift acquire'ı tesadüfen telafi ediyor.

**Önerilen düzeltme** (uygulanmadı — derleyip bir kez koşturarak yapılmalı):

1. `MapFragment`'e `private var mainActivityViewsLocked = false` ekle.
2. `disableMainActivityViews()`: `mainActivityViewsLocked` true ise `acquire` etme (kardeşi
   `disableMapFragmentViews` gibi); değilse acquire et ve bayrağı set et.
3. Release'i `enableMainActivityViews()`'a taşı (bayrak set ise release + bayrağı temizle) ve
   `enableMapFragmentViews()`'tan release'i **kaldır** — çünkü `disableMapFragmentViews()` hiç
   acquire etmiyor. Böylece eşleşme simetrik olur.
4. `forceEnableMapTouchRouting()` (1775) artık tek release eder; ayrıca bir şey yapmaya gerek yok.
5. **Kenar durum:** `MainActivityChromeBlocker.ensureUnlockedForMapReturn` derinliği zorla 0'a
   çekiyor. O olduğunda `mainActivityViewsLocked` bayat `true` kalır ve sonraki gerçek kilit
   atlanır. `MapFragment.onResume`'da `if (MainActivityChromeBlocker.currentLockDepth() == 0)
   mainActivityViewsLocked = false` ile kapat.

Release/acquire çağrı noktalarının tamamı: `grep -n "MainActivityChromeBlocker\." app/src/main/java/com/example/app/*.kt`
(9 fragment kullanıyor: ChestFragment, ChestResult, LessonResult, LessonResultFalse, MapFragment,
MissionChestReward, RecordFragment, SeasonLeaderboardRewardGate). Yalnızca `MapFragment`
tarafındaki asimetri düzeltilmeli; diğerleri `onViewCreated`/`onDestroyView` çiftinde ve simetrik.

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
   bu kısım `MainActivity.liveOverlayIn` (1871) ile kapatıldı, bkz. aşağıda.

Kalıcı çözüm ders bitiş yolunda bu girişi de pop etmek; ama o yol hassas (bkz. Çalışma
anlaşmaları). Ayrı bir turda, tek başına ve derleyerek yapılmalı. `docs/YAYIN_ONCESI_KONTROL.md`
teknik borç bölümünde de yazılı.

## Yakında yapılanlar — tekrar etmeyin

- `be6cb48` **Seri donması:** `last_goal_day` bir günden fazla ileride ise (cihaz saati ileri
  alınarak test edilmişti) `refresh()` artık o değeri atıyor. Saat dilimi payının meşru sınırı
  ±1 gün; sunucu da `STREAK_DAY_TOLERANCE_DAYS = 1` kullanıyor.
- `5ef1b37` + `3ac8615` **StreakDiag:** seri zincirinin beş halkası loglanıyor.
- `3722b05` **Hayalet overlay:** `FragmentManager.findFragmentById` ekli olmayan, yalnızca bir
  back stack girişinin tuttuğu fragment'ı da döndürüyor. `MainActivity.liveOverlayIn` (1871)
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

`submitStreakDay`'in deploy edildiği **çıkarım** yoluyla saptandı, doğrudan görülmedi: seri
dokümanında `lastSeenAt` (yalnızca `reminderPatch` yazıyor, iki dalda da) `updatedAt`'ten
(yalnızca gün taşıyan transaction yazıyor) daha yeniydi — yani günsüz "buradayım" dalı
başarıyla çalışmış. Düzeltme öncesi o dal her seferinde çöküyordu. Emin olmak istersen
Firebase konsolundan fonksiyonun son deploy zamanına bak.

**Deploy etmeden önce kullanıcıya sor.**

## Teşhis logları

| Etiket | Ne gösterir |
|---|---|
| `StreakDiag` | Günlük seri zinciri: süre sayımı, `refresh()` dalları, kuyruk, sunucu gidiş/dönüş, ekran |
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

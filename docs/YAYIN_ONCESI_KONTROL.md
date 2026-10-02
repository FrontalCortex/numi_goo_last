# Yayın Öncesi Kontrol Listesi

Beta ya da mağaza sürümü almadan önce bakılacaklar. Her maddede **neden** yazıyor:
listeyi altı ay sonra okuyan kişi (muhtemelen sen) nedenini bilmeden doğru kararı veremez.

---

## 1. Test anahtarları

Elle `true` yapılıp unutulabilen anahtarlar. İlk üçü **güvenlik sorunu değil** — üçü de
`BuildConfig.DEBUG` ile çarpılıyor, yani release APK'sinde blok hiç çalışmıyor. Sorun debug
derlemesinde: açık kalırsa kendi testlerini yanıltır. Dördüncüsü farklı, aşağıda ayrıca
yazıyor.

| Dosya | Sabit | Yayın değeri |
|---|---|---|
| `AskQuestionPromoDebug.kt` | `FORCE` | `false` |
| `MissionProgressDebug.kt` | `RESET_ON_LAUNCH` | `false` |
| `NewStreakPromptDebug.kt` | `FORCE` | `false` |
| `StreakDiag.kt` | `ENABLED` | `false` |

**Ne yapıyorlar**

- `AskQuestionPromoDebug.FORCE` — öğretmene sorma tanıtımının (`AskQuestionOpenFragment`)
  plan ve cihaz kredisi kapılarını atlar, sayaç eşiğini 1'e indirir. Pro bir hesapla bu ekran
  görülemediği için test etmenin başka yolu yok.
- `MissionProgressDebug.RESET_ON_LAUNCH` — görev ilerlemesini her açılışta sıfırlar. Ders sonu
  görev ödülü paneli yalnızca bir görev tamamlanınca açıldığı için, günün görevleri bittiyse
  panel bir daha hiç çıkmıyor.
- `NewStreakPromptDebug.FORCE` — yeni seri sorusunun (`NewStreakFragment`) iki koşulunu
  (seri 0, bugün sorulmadı) atlar: her ders dönüşünde ve her kupa testi kapanışında sorulur.
  Koşulları elde etmenin başka yolu yok — yeni hesapta kayıt akışı soruyu o gün için
  "soruldu" diye işaretliyor, serisi olan hesapta ise serinin kırılmasını beklemek gerekiyor.
- `StreakDiag.ENABLED` — günlük seri zincirinin teşhis logları (`StreakDiag` filtresi).
  **Diğer üçünden farklı: `BuildConfig.DEBUG`'a bağlı DEĞİL**, yani kapatılmazsa release
  APK'sinde de log basar. Zarar vermez ama kullanıcı cihazında işi yok; logcat'e seri
  durumu (gün kimlikleri, hedef, sunucu sayacı) yazıyor. Zincirin beş halkasından hangisinin
  sessizce "hiçbir şey yapma" dediğini görmek için eklendi.

**Kontrol**

```powershell
git grep -n "FORCE = true" -- app/src/main/java/com/example/app/AskQuestionPromoDebug.kt
git grep -n "RESET_ON_LAUNCH = true" -- app/src/main/java/com/example/app/MissionProgressDebug.kt
git grep -n "FORCE = true" -- app/src/main/java/com/example/app/NewStreakPromptDebug.kt
git grep -n "ENABLED = true" -- app/src/main/java/com/example/app/StreakDiag.kt
```

Dördü de boş dönmeli.

---

## 2. Release'de kendiliğinden değişenler — dokunma

Bunlar zaten `BuildConfig.DEBUG`'a bağlı, elle bir şey yapmana gerek yok. Burada olmalarının
sebebi "acaba unuttum mu?" diye aramana gerek kalmaması:

- **Reklam birimi** — `AdManager`: debug'da test birimi, release'de gerçek birim. Gerçek
  birimi debug'da kullanmak AdMob hesabını askıya aldırabilir, o yüzden bu ayrım kalmalı.
- **Kısayollar** — `MainActivity`: enerji rozetine uzun basınca enerji tüketme, seri
  rozetine uzun basınca 60 saniye çalışma ekleme. Release'de yok.
- **Teşhis log'ları** — `BadgeDiagnostics`, `TutorialBeadDiagnostics`,
  `TutorialAbacusResetDiagnostics`, `AgentDebugLog`, `StudyTimeTracker`. Release'de sessiz.

---

## 3. Beta verisiyle karar verilecekler

Şimdi tahminle değiştirme; beta çıktıktan sonra ölçüp karar ver.

- **Tanıtım sıklığı** — `MainActivity.ASK_QUESTION_PROMO_LESSON_RETURN_THRESHOLD = 3`.
  Türü LESSON olan her üçüncü ders dönüşünde açılıyor. Üç sayısı ölçülerek değil seçilerek
  kondu. Bakılacak ölçüm: tanıtımın gösterim sayısına karşılık Pro denemesi başlatma oranı.
- **Seri dondurma** — veri beklenmeden yazıldı (01.10.2026): 1 günü kapatıyor, 4000 altın,
  aynı anda en fazla 1 tane. Bu üç sayı da ölçülerek değil seçilerek kondu; beta verisiyle
  yeniden bakılacak. Bakılacak ölçümler:
  - `streak_broken.missed_days` — seriler kaç günlük boşlukla kırılıyor. Çoğu 1 gün ise 1
    günlük dondurma doğru; 2-3 gün ağırlıktaysa ikinci bir dondurma tutmaya izin vermek gerekir.
  - `streak_freeze_used.streak_saved` — harcanan dondurmaların kaçı seriyi gerçekten
    kurtardı. Düşükse çocuklara boşa altın harcatıyoruz demektir (iki gün kaçınca dondurma
    yine harcanıyor; bu kural o zaman yeniden düşünülmeli).
  - `gold_spent` içinde `spend_item=streak_freeze` — kaç kişi alıyor. Kimse almıyorsa fiyat
    yüksek ya da kart mağazanın dibinde görünmüyor.

---

## 4. Teknik borç

- **`.gitattributes` yok.** Windows'ta `core.autocrlf` açıkken git, ikili dosyaları metin
  sanıp bozabiliyor — `tutorial1_1.mp3` bir kez bu şekilde 2501 bayt şişti. Yerel kopyada
  `core.autocrlf false` yapıldı ama bu ayar makineye bağlı: yeni bir klonda ya da başka bir
  bilgisayarda aynı bozulma tekrarlar. Depoya `.gitattributes` eklemek kalıcı çözüm.
- **`firebase-functions` ^5.0.0.** v6 çıktı. Acil değil ama sürüm atlandıkça geçiş zorlaşıyor.
- **Cihaz saatini ileri alarak test etmek kalıcı iz bırakıyordu.** `last_goal_day` ileri
  bir tarihte kalıyor ve seri o güne kadar sessizce donuyordu (01.10.2026'da yaşandı:
  `last_goal_day=2026-10-05`). İstemci artık bir günden fazla ileri bir son günü atıyor
  (`StreakRepository.refresh`), yani kendi kendine düzeliyor. Yine de seriyi saat
  oynatarak test ederken `streak_prefs`'i de temizlemek daha temiz:
  `adb shell run-as com.numigo.app rm /data/data/com.numigo.app/shared_prefs/streak_prefs.xml`
- **Ders açılışının `addToBackStack(null)` girişi başarılı bitişte hiç pop edilmiyor.**
  `LessonAdapter.continueWithLesson` dersi `replace(abacusFragmentContainer, ...).addToBackStack(null)`
  ile açıyor. Ders normal bitince (LessonResult → ChestFragment → NewChestFragment) yalnızca
  `"map_chest"` girişi pop ediliyor; ders açılışının girişi back stack'te kalıyor. Sonuçları:
  (a) o TutorialFragment/AbacusFragment örneği bellekte canlı kalıyor ve her tamamlanan derste
  bir tane daha birikiyor, (b) haritada geri tuşuna basıldığında görünür hiçbir şey olmadan
  bu girişler tek tek pop ediliyor, (c) `findFragmentById` bu girişlerin tuttuğu fragment'ı
  container boş olsa bile döndürüyor — 30.09.2026'daki "haritaya tıklanamıyor + ders
  güncellenmiyor" hatasının kökü buydu. (c) `MainActivity.liveOverlayIn` ile kapatıldı;
  (a) ve (b) hâlâ açık. Kalıcı çözüm ders bitiş yolunda bu girişi de pop etmek ama o yol
  (prepareMapReturn/finalizeMapReturn) çok hassas — ayrı bir turda, tek başına denenmeli.

---

## 5. Sürüm alırken

- `app/build.gradle.kts` içinde `versionCode` ve `versionName` artırıldı mı.
- Firestore kuralları ve Cloud Functions deploy edildi mi (`docs/FIRESTORE_RULES_DEPLOY.md`).
- `versionCode` şu an **7**, `versionName` **1.0** (`app/build.gradle.kts`).
- Release derlemesiyle bir kez baştan sona akış denendi mi: kayıt → ilk ders → sandık → rozet.
  Debug'da göremediğin şey gerçek reklam birimi: test reklamı her zaman dolu gelir, gerçek
  birim gelmeyebilir ve "reklam yok" yolunun da çalışması gerekir.

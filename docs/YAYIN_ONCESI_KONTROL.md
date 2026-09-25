# Yayın Öncesi Kontrol Listesi

Beta ya da mağaza sürümü almadan önce bakılacaklar. Her maddede **neden** yazıyor:
listeyi altı ay sonra okuyan kişi (muhtemelen sen) nedenini bilmeden doğru kararı veremez.

---

## 1. Test anahtarları

İkisi de elle `true` yapılıp unutulabilen anahtarlar. **Güvenlik sorunu değil** — ikisi de
`BuildConfig.DEBUG` ile çarpılıyor, yani release APK'sinde blok hiç çalışmıyor. Sorun debug
derlemesinde: açık kalırsa kendi testlerini yanıltır.

| Dosya | Sabit | Yayın değeri |
|---|---|---|
| `AskQuestionPromoDebug.kt` | `FORCE` | `false` |
| `MissionProgressDebug.kt` | `RESET_ON_LAUNCH` | `false` |

**Ne yapıyorlar**

- `AskQuestionPromoDebug.FORCE` — öğretmene sorma tanıtımının (`AskQuestionOpenFragment`)
  plan ve cihaz kredisi kapılarını atlar, sayaç eşiğini 1'e indirir. Pro bir hesapla bu ekran
  görülemediği için test etmenin başka yolu yok.
- `MissionProgressDebug.RESET_ON_LAUNCH` — görev ilerlemesini her açılışta sıfırlar. Ders sonu
  görev ödülü paneli yalnızca bir görev tamamlanınca açıldığı için, günün görevleri bittiyse
  panel bir daha hiç çıkmıyor.

**Kontrol**

```powershell
git grep -n "FORCE = true" -- app/src/main/java/com/example/app/AskQuestionPromoDebug.kt
git grep -n "RESET_ON_LAUNCH = true" -- app/src/main/java/com/example/app/MissionProgressDebug.kt
```

İkisi de boş dönmeli.

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
- **Seri dondurma** — `streak_broken.missed_days` ölçümü 2-3 haftalık beta verisi biriktirsin.
  Kaç günlük boşluktan sonra serinin kırıldığını bilmeden dondurma hakkının kaç gün olacağına
  karar verilemez.

---

## 4. Teknik borç

- **`.gitattributes` yok.** Windows'ta `core.autocrlf` açıkken git, ikili dosyaları metin
  sanıp bozabiliyor — `tutorial1_1.mp3` bir kez bu şekilde 2501 bayt şişti. Yerel kopyada
  `core.autocrlf false` yapıldı ama bu ayar makineye bağlı: yeni bir klonda ya da başka bir
  bilgisayarda aynı bozulma tekrarlar. Depoya `.gitattributes` eklemek kalıcı çözüm.
- **`firebase-functions` ^5.0.0.** v6 çıktı. Acil değil ama sürüm atlandıkça geçiş zorlaşıyor.

---

## 5. Sürüm alırken

- `app/build.gradle.kts` içinde `versionCode` ve `versionName` artırıldı mı.
- Firestore kuralları ve Cloud Functions deploy edildi mi (`docs/FIRESTORE_RULES_DEPLOY.md`).
- `versionCode` şu an **7**, `versionName` **1.0** (`app/build.gradle.kts`).
- Release derlemesiyle bir kez baştan sona akış denendi mi: kayıt → ilk ders → sandık → rozet.
  Debug'da göremediğin şey gerçek reklam birimi: test reklamı her zaman dolu gelir, gerçek
  birim gelmeyebilir ve "reklam yok" yolunun da çalışması gerekir.

# Satın Alma Entegrasyonu (Google Play Billing)

Kod tarafı hazır. Bu belge **kalan yapılandırma adımlarını** ve sistemin nasıl
çalıştığını anlatır.

---

## 1. Yapılmadan hiçbir şey çalışmaz: paket adı

`app/build.gradle.kts` içindeki `applicationId` hâlâ **`com.example.app`**.
Google Play `com.example.*` önekini reddeder — bu paket adıyla AAB yüklenemez.

Değiştirirken:

1. `applicationId` gerçek bir paket adına çevrilir (ör. `com.numigoo.app`).
   `namespace` aynı kalabilir; Kotlin dosyalarını taşımaya gerek yok.
2. Firebase Console → proje ayarları → yeni paket adıyla Android uygulaması eklenir,
   `google-services.json` yenilenir.
3. Cloud Functions ortamına aynı paket adı `ANDROID_PACKAGE_NAME` olarak verilir
   (`functions/.env` veya Secret Manager). Tanımlı değilse satın alma fonksiyonları
   `failed-precondition` ile reddeder — sessizce yanlış davranmaz.

---

## 2. Play Console kurulumu

**Ürünler** (kimlikler kodla birebir aynı olmalı):

| Ürün kimliği | Tür | Verilen |
| --- | --- | --- |
| `gold_1200` | Tüketilebilir | 1200 altın |
| `gold_7000` | Tüketilebilir | 7000 altın |
| `gold_15000` | Tüketilebilir | 15000 altın |
| `keys_10` | Tüketilebilir | 10 anahtar |
| `keys_50` | Tüketilebilir | 50 anahtar |
| `keys_100` | Tüketilebilir | 100 anahtar |
| `pro_monthly` | Abonelik | `plan = "Pro"` |
| `lite_monthly` | Abonelik | `plan = "Lite"` |

Kimlikler üç yerde tanımlı ve **birlikte** güncellenmeli:
`BillingCatalog.kt`, `functions/index.js` → `PLAY_PRODUCT_CATALOG` /
`PLAY_SUBSCRIPTION_CATALOG`, ve Play Console.

**Play Developer API yetkisi:** Cloud Functions'ın varsayılan servis hesabına
Play Console → Users and permissions üzerinden "View financial data" ve
"Manage orders and subscriptions" verilmelidir. Bu olmadan token doğrulaması
`permission-denied` döner.

**Test:** Play Console → License testing'e test hesapları eklenir. Test satın
almaları gerçek para çekmez ve `purchases.products.get` ile doğrulanabilir.

---

## 3. Sistem nasıl çalışıyor

### Tüketilebilir ürün (altın / anahtar)

```
İstemci                         Sunucu (redeemGooglePlayPurchase)
──────────────────────────────  ─────────────────────────────────────────
launchBillingFlow
  → Play ödeme ekranı
  → purchaseToken alınır
productId + token gönderilir ─→ 1. productId sunucu kataloğunda mı?
  (miktar GÖNDERİLMEZ)           2. Token Play Developer API ile doğrulanır
                                 3. purchaseState == 0 mı?
                                 4. processedPurchases/{token} + bakiye
                                    AYNI transaction'da yazılır
                              ←─ yeni bakiye
consumeAsync (ürün tüketilir)
```

Sıralama önemli: **önce sunucu onayı, sonra tüketim.** Ters olsaydı sunucu
çağrısı düştüğünde kullanıcı parayı öder ama ödülü alamazdı.

`processedPurchases/{token}` replay korumasıdır — aynı token ikinci kez ödül
veremez. Sunucu `already-exists` dönerse istemci ürünü yine de tüketir; ödül
zaten verilmiştir ve Play tarafı temizlenmelidir.

### Para iadesi (`reconcileVoidedPurchases`)

Günde bir kez Voided Purchases API taranır; iade/iptal edilmiş her token için verilen
ödül geri alınır.

**Bakiyenin eksiye düşmesine izin verilir.** Sıfırda kesilseydi "hepsini harca, sonra
iade al" açığı olduğu gibi kalırdı. Kullanıcı borcunu kazandığı ödüllerle kapatana
kadar bir şey satın alamaz: `updateUserWallet` yalnızca **harcamayı** sıfırın altına
inmekten engeller, kredileri değil.

İade edilen abonelik `plan = "Free"` yapar. İşlem `processedPurchases/{token}.voided`
ile bir kez yapılır; tarama penceresi çakışsa bile ödül iki kez geri alınmaz.

RTDN (Pub/Sub) yerine tarama tercih edildi: aynı servis hesabını kullanır, ek altyapı
istemez ve iade geri alımı için saniyelik gecikme gerekmez (Google'ın self-servis iade
penceresi 48 saat). Hacim artarsa tetikleyici RTDN'e çevrilebilir — geri alma mantığı
aynen kalır.

### Abonelik (Pro / Lite)

Tüketilebilirlerden farklı olarak **tekrar tekrar çağrılabilir**. Uygulama her
açıldığında `BillingManager.refreshPurchases()` eldeki abonelik token'ını yeniden
doğrulatır; sunucu `plan` ve `planExpiresAt` alanlarını günceller.

İptal eden kullanıcı iki katmanda yakalanır:

1. Sunucu, Play'den `SUBSCRIPTION_STATE_*` okur; aktif değilse `plan = "Free"` yazar.
2. `MainActivity.checkSubscriptionAndUpdateEnergy()` `planExpiresAt` geçmişse
   planı Free sayar — sunucudan hiç yeni doğrulama gelmese bile Pro süresiz kalmaz.

---

## 4. Cüzdanın geri kalanı

`users/{uid}.keys` ve `.currency` yalnızca Cloud Functions üzerinden değişir.
`firestore.rules` şu alanları istemci yazımına kapatır:
`keys`, `currency`, `walletGuard`, `role`, `teacherApproved`, `plan`,
`planExpiresAt`, `planProductId`, `userId`, `uid`, `email`, `createdAt`.

**İstemcinin çağırabildiği hiçbir yol artık bakiyeyi serbestçe artıramaz.**

`updateUserWallet` ile yapılabilecekler:

| İşlem | Kural |
| --- | --- |
| Harcama (negatif) | Serbest; sıfırın altına inemez. Sunucu tek kullanımlık `rollbackToken` döndürür. |
| `purchase_rollback` | Yalnızca o harcamanın jetonuyla, **birebir aynı miktarla**, 10 dakika içinde. |
| Diğer her gerekçe | Reddedilir. |

Geri alma jetonu, boncuk/çerçeve alımının Firestore kaydı düştüğünde harcamayı iade
etmek içindir. Jeton olmadan geri alma yapılamadığı için "harcamadan iade al" ile
bakiye şişirmek mümkün değildir. (Google Play para iadesiyle karıştırılmamalı — o
`reconcileVoidedPurchases`'ta işlenir.)

### Ödüller

Sandık ve kristal ödüllerinin zarı **sunucuda** atılır:

| Fonksiyon | Ne yapar | Günlük tavan |
| --- | --- | --- |
| `openChest` (reklamlı) | AdMob SSV ile doğrulanmış hakkı bozdurur | tavan yok |
| `openChest` (ders/görev) | 3 nadirlik adımını ve ödülü çeker, bakiyeye yazar | 400 |
| `openCrystalReward` | Kristal videosunu ve ödülü çeker, bakiyeye yazar | 20 |

İstemci yalnızca sonucu oynatır (`ServerRewards.kt`); ne isteyeceğini seçemez.
Sayaçlar `users/{uid}.rewardGuard` içinde, bakiye yazımıyla aynı transaction'da tutulur.

Olasılık ve ödül tabloları hem `functions/index.js` hem istemci tarafında bulunur ve
**birlikte** güncellenmelidir. NOT: Kotlin'deki eski yüzde yorumları koda uymuyordu
(ör. COMMON için "%15 RARE / %80 COMMON" yazıyordu ama eşikler %20/%75 veriyor);
sunucu **gerçek davranışı** birebir kopyalar, yorumları değil.

Satın alma **kesinlikle** `updateUserWallet` üzerinden geçmez.

---

## 5. Planların kapsamı

| Plan | Enerji | Yenilenme | Öğretmene soru |
| --- | --- | --- | --- |
| Free | 5 | 10 dk | ✗ |
| Lite | 10 | 7 dk | ✗ |
| Pro / Premium | ∞ | — | ✓ |

Lite'ın enerji farkı `EnergyManager.getMaxEnergy()` ve `getEnergyRefreshMinutes()`
içinde tanımlıdır; uygulamada sabit 5/10 değeri kalmamıştır. Öğretmene soru sorma
`AskQuestionButtonBinder`'da yalnızca Pro/Premium'a açıktır — Lite'a da açılacaksa
orası güncellenmeli.

## 6. Ödül hak edişi

### Reklam ödülleri — doğrulanıyor (AdMob SSV)

Reklamla kazanılan sandık, oyunun tek **sınırsız** ödül kaynağıydı ve "reklamı izledim"
iddiası doğrulanamıyordu. Artık reklamı AdMob'un kendisi haber veriyor:

```
İstemci                          AdMob                    Sunucu
─────────────────────────────    ─────────────────────    ──────────────────────────
custom_data = "<uid>:<nonce>"
reklam izlenir              ──▶  imzalı callback     ──▶  admobRewardCallback
                                                          · imza ECDSA-SHA256 ile doğrulanır
                                                          · adRewards/{nonce} yazılır
openChest(adNonce = nonce)  ──────────────────────────▶   hak tüketilir + ödül verilir
```

- İmza, sorgu dizesinin başından `&signature=` parametresine kadar olan **ham** kısım
  üzerinde doğrulanır; anahtarlar `gstatic.com/admob/reward/verifier-keys.json`'dan
  alınıp 24 saat önbelleklenir.
- `adRewards/{nonce}` tek kullanımlıktır ve tüketimi ödül yazımıyla **aynı transaction**
  içindedir. AdMob'un tekrar denemeleri ikinci hak yaratmaz (doküman kimliği nonce).
- SSV callback'i istemciden geç gelebilir; sunucu `unavailable` döner ve istemci
  1,5 sn aralıklarla 4 kez yeniden dener (`ServerRewards.openChest`).
- Doğrulanmış reklam sandıkları günlük tavana **girmez**.

**Kurulum:** AdMob konsolu → ilgili ad unit → Server-side verification → callback URL:
`https://<region>-<project>.cloudfunctions.net/admobRewardCallback`

> `AdManager.kt` hâlâ Google'ın **test reklam birimlerini** kullanıyor
> (`ca-app-pub-3940256099942544/...`). Yayına çıkmadan önce gerçek birimlerle
> değiştirilmeli, SSV de o birimler üzerinde yapılandırılmalı.

### Ders / görev ödülleri — hâlâ doğrulanmıyor

`openChest`, ders veya görevin gerçekten tamamlandığını doğrulamıyor. Sebep
`firestore.rules` içindeki şu kural:

```
match /users/{userId}/{subcollection}/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

Kullanıcı `lessonProgress` / `missionProgress` dahil kendi alt koleksiyonlarının
tamamına yazabiliyor. Yani sunucu "3. dersi bitirdin mi?" diye sorsa bile cevabı
kullanıcının kendi yazdığı veriden okuyacak — doğrulama hiçbir şey doğrulamaz.

Kapatmak için ders bitişinin bir Cloud Function'dan geçmesi, ilerlemeyi sunucunun
yazması ve kuralın `allow write: if false` olması gerekir. Bu, ilerleme sisteminin
yeniden yazılması demek; **lansman öncesi önerilmez** (soft-currency riski, gerçek
para riski değil).

**Doğru çözüm tam doğrulama değil, adım başına tek sandık.**

Sandık zaten yalnızca bir adım **ilk kez** bitirildiğinde veriliyor (`LessonResult`:
`stepIsFinish` true ise sandık atlanıyor) ve içerik sonlu: 169 lessonItem, toplam
**328 adım**. Yani sunucunun "bu dersi gerçekten bitirdin mi?" diye sorması gerekmiyor;
"bu adım için daha önce sandık verdim mi?" diye sorması yetiyor.

`openChest(source, sourceId)` ile idempotent hak:

| Kaynak | `sourceId` | Sonuç |
| --- | --- | --- |
| Ders | `lesson_{partId}_{stepIndex}` | Adım başına ömür boyu tek sandık |
| Görev | `mission_{window}_{periodId}_{missionId}` | Görev başına dönemde tek sandık |
| Reklam | — (AdMob SSV nonce'ı) | İzlenen reklam başına tek sandık |

Bunun etkisi: kaçak kullanım "günde 400 sandık, sonsuza kadar" yerine
"toplamda 328 sandık, bir kez" seviyesine iner. İlerleme sistemini yeniden yazmayı
gerektirmez — yalnızca her giriş noktasından `sourceId` taşınması gerekir.

Şimdilik tek savunma günlük tavan: `CHEST_DAILY_LIMIT` (400) ve
`CRYSTAL_DAILY_LIMIT` (20). 400 sayısı meşru tavanın (328) üstünde seçildi; daha
düşük bir değer tüm içeriği tek günde bitiren Pro kullanıcıyı engellerdi.

## 7. Diğer küçük işler

**Tavana takılan meşru ödül sessizce geri alınıyor**
(`MainActivity.resyncWalletFromServer`). `resource-exhausted` hatası kullanıcıya
açık bir mesajla gösterilmeli.

**Eksi bakiye açıklanmıyor.** Play iadesi sonrası bakiye eksiye düşebiliyor ve
kullanıcı nedenini göremiyor; `processedPurchases` kaydında gerekli bilgi var.

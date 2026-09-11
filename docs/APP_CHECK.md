# App Check

## Ne işe yarıyor

`firestore.rules` ve Cloud Functions'taki `context.auth` **"kim"** sorusunu çözüyor: giriş
yapmış kullanıcı neyi okuyabilir, neyi yazabilir. App Check bambaşka bir soruyu çözüyor:
**"bu istek gerçekten bizim uygulamamızdan mı geliyor?"**

App Check olmadan biri `google-services.json`'ı APK'dan çıkarıp bir Node script'iyle hesap
açabilir ve backend'i çağırabilir. `context.auth` dolu olur, kurallar geçer — çünkü gerçekten
geçerli bir kullanıcıdır, sadece uygulamanın içinden gelmiyordur.

Bu projede kapattığı somut açıklar:

- **Toplu hesap açma.** Doğrudan para kazandırmaz ama Firebase faturasını şişirir ve Pro
  hoş geldin kredisi / ücretsiz deneme istismarının ölçeklenmesinin tek yoludur.
- **Liderlik tablosu.** `submitLeaderboardScore` geçerli bir `context.auth` ile
  çağrılabiliyor; isteğin uygulamadan gelip gelmediği kontrol edilmiyor.
- **Yamalanmış APK.** İstemci tarafı kontroller atlanıp fonksiyonlar uydurma argümanlarla
  çağrılabiliyor. Play Integrity bunu yakalar.

## Koddaki durum

| Yer | Ne |
| --- | --- |
| `app/build.gradle.kts` | `firebase-appcheck-playintegrity` (tüm türler), `firebase-appcheck-debug` (yalnız debug) |
| `app/src/main/.../NumiGooApplication.kt` | Süreç başlangıcında sağlayıcıyı kurar; hata olursa uygulamayı düşürmez |
| `app/src/release/.../AppCheckInstaller.kt` | Play Integrity sağlayıcısı |
| `app/src/debug/.../AppCheckInstaller.kt` | Hata ayıklama sağlayıcısı |
| `AndroidManifest.xml` | `android:name=".NumiGooApplication"` |

Cloud Functions ve kurallar tarafında **hiçbir değişiklik yok** — zorlama koddan değil,
Firebase Console'dan açılır (aşağı bkz.).

## Konsol adımları (elle yapılacak)

1. **Play Console → Uygulama bütünlüğü (App integrity)** → Play Integrity API'yi
   `numigo-new` Firebase/Google Cloud projesine bağla.
2. **Firebase Console → App Check → Uygulamalar** → `com.numigo.app` için Play Integrity'yi
   kaydet.
3. **Zorlamayı (enforcement) ŞU AN AÇMA.** Bkz. aşağıdaki takvim.
4. (Yalnız zorlama açıldıktan sonra gerekli) Test cihazı için: debug APK ilk açılışta
   Logcat'e şunu basar —
   `D/DebugAppCheckProvider: Enter this debug secret into the allow list ... <UUID>`
   Bu değeri **App Check → Uygulama → Hata ayıklama jetonları**'na ekle. Jeton kurulum
   başına üretilir; uygulamayı silip yeniden kurmak yenisini üretir.

## Zorlama takvimi — sıra önemli

App Check SDK'sı **yayınlanan ilk sürümde bulunmalı**, ama zorlama aylar sonra açılmalı.
Sebebi: zorlama açıldığı anda App Check SDK'sı olmayan her istemci anında kilitlenir.
SDK'yı sonraki bir sürümde eklersen, zorlamayı açtığın gün hâlâ eski sürümde olan
kullanıcıların uygulaması komple çöker ve düzelmesi güncellemenin yayılmasını bekler.

| Ne zaman | Ne |
| --- | --- |
| Yayından önce | SDK + konsol kaydı. Zorlama **kapalı**. |
| Yayın günü | Değişiklik yok; SDK sessizce jeton üretir, hiçbir şey bloklanmaz. |
| +2-4 hafta | Console'daki doğrulanmış/doğrulanmamış istek oranına bak. ~%99+ doğrulanmışsa servis servis aç: Firestore → Storage → Functions. |

Her serviste zorlamayı açtıktan sonra birkaç gün hata oranını izle; bir sıçrama olursa
zorlamayı kapatmak anında geri alır.

## Bilinmesi gerekenler

- Play Integrity yalnızca Play'den dağıtılan, Play App Signing anahtarıyla imzalanmış
  kurulumları doğrular. CI'ın ürettiği debug APK'lar doğrulanmaz — zorlama kapalıyken sorun
  değil, açıkken debug jetonu gerekir.
- Play Integrity'nin günlük istek kuotası vardır. App Check jetonları önbelleklediği için
  başlangıç ölçeğinde sorun çıkmaz; kullanıcı sayısı büyürse Play Console'dan kuota artışı
  istenmesi gerekebilir.

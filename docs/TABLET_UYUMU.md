# Tablet uyumu

Uygulama yalnızca dikey; tablette de dikey kalıyor.

## Yaklaşım: tablette yoğunluğu büyütmek

Düzenlerde ~6000 sabit dp/sp var, `@dimen` kullanımı ~2400. `values-sw600dp` gibi ölçü dosyaları
bunların küçük bir kısmını kapsardı. Onun yerine `FontScaleLimiter.wrap` (her aktivitenin
`attachBaseContext`'i) en küçük genişliği ≥ 600 dp olan cihazda `densityDpi`'yi büyütüyor:
tablet ~480–530 dp genişliğinde, ~800 dp boyunda büyük bir telefon gibi davranıyor. Telefonlarda
(sw < 600) hiçbir şey değişmiyor.

Örnek: Lenovo Tab M10 (TB-X505F, 800×1280, 160 dpi) → ~242 dpi, ~529×800 dp.

## Dikkat edilecekler

- **Kodda üretilen Bitmap'ler:** `Bitmap.createBitmap`/`drawToBitmap` cihazın fiziksel yoğunluğuyla
  doğuyor. `BitmapDrawable`/`ImageView` uygulamanın (büyütülmüş) yoğunluğuna göre yeniden ölçekliyor.
  Ekranda gösterilecek bitmap'e `bitmap.density = resources.displayMetrics.densityDpi` verilmeli
  (abaküs çizicileri zaten veriyordu; `AvatarView`, `GradientIcon`, harita ders kartı görüntüsü düzeltildi).
- **Uygulama bağlamı** (`applicationContext.resources`) büyütülmüyor; görünüm şişirmek ve ölçü almak
  için aktivite bağlamı kullanılmalı.
- **Sabit dp ile gizlenen paneller:** ekran genişliğine göre hesaplanmalı (abaküs ayar paneli artık
  ebeveynin genişliği kadar kayıyor ve kapalıyken görünmez).

## Doğrulanan ekranlar (Tab M10)

Açılış, part kartları, harita ve ders paneli, öğretici, abaküs dersi, profil, görevler, mağaza,
sorularım, keşfet, kupa yolu listesi ve zorluk penceresi, flaş dersi, kupa yolu oynatıcısı
(ödev oynatıcısı; abaküs ayar paneli açılıp kapanıyor, kapalıyken görünmüyor), abaküs dersinde
ayar paneli ve yanlış cevap paneli, Sobi ekranı, ayarlar, veli paneli (dersler, kural başarısı).

Sobi testi paneli de doğrulandı (kural sayımını silen sunucu hatası 50db074'te düzeltildikten sonra).

Henüz bakılmayan: plan/Pro ekranları (öğretmen hesabında girişi yok). Alttan açılan panel;
pencere ölçüsüne özel kodu yok.

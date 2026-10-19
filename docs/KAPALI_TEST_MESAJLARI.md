# Kapalı test: iletişim akışı ve mesaj metinleri (19.10.2026)

Formlar ve takip: `docs/KAPALI_TEST_FORMLARI.md`. Burada yalnızca **annelere ne zaman, nereden, ne
yazılacağı** var. `[...]` ile gösterilen yerleri doldur.

## Akış

```
Facebook / Instagram paylaşımı (tanıtım videosu)
        │  "Katılmak için WhatsApp'tan yazın" → wa.me bağlantısı (hazır mesajlı)
        ▼
Anne WhatsApp'tan yazar ("Sorobit testine katılmak istiyorum")
        │  Otomatik karşılama mesajı (WhatsApp Business) + katılım videosu
        ▼
Anne katılır, "katıldım" yazar   → takip tablosuna satır, Form 1 gönderilir
        ▼
7–10. gün                         → Form 2
3 gündür kullanmıyorsa            → Form 3
14. gün sonrası                   → teşekkür
```

Neden paylaşıma doğrudan katılım bağlantısı konmuyor: anne sana hiç yazmazsa Form 1-2-3'ü
gönderemezsin, katılım tarihini bilemezsin ve kimin katıldığını göremezsin. WhatsApp'tan yazması
tek adım ekliyor ama iletişim kanalını baştan açıyor; birebir konuşulan testçi de daha uzun kalıyor.

## Hazırlık

1. **WhatsApp Business** (ücretsiz) kur. Mümkünse kişisel numaran yerine ikinci bir hat kullan: numara
   herkese açık paylaşılacak.
2. Ayarlar → İşletme araçları → **Karşılama mesajı**: aşağıdaki "Karşılama" metnini yaz, "Herkese"
   ya da "Kişi listemde olmayanlara" seç. Sana ilk kez yazan herkese kendiliğinden gider.
3. Ayarlar → İşletme araçları → **Hızlı yanıtlar**: `/katilim`, `/form1`, `/form2`, `/form3`,
   `/tesekkur` kısayollarıyla aşağıdaki metinleri kaydet. `/katilim`'e katılım videosunu ekle
   (`C:\Users\ASUS\Videos\Sorobit_tanitim\katilim\Sorobit_katilim.mp4`).
4. Hazır mesajlı WhatsApp bağlantısı (numarayı başında 90 ile, boşluksuz yaz):
   `https://wa.me/90[numara]?text=Sorobit%20testine%20kat%C4%B1lmak%20istiyorum`

---

## 1. Facebook grup yöneticisine özel mesaj (paylaşmadan ÖNCE)

> Merhaba, grubunuzun kurallarına uymak için önce size sormak istedim.
> Çocuklara abaküsle zihinden hesabı oyun gibi öğreten Sorobit adlı bir uygulama geliştirdim.
> Yayına çıkmadan önce ücretsiz deneyecek aileler arıyorum. Satış ya da ücret yok; yalnızca
> görüşlerini almak istiyorum. Grupta kısa bir videoyla duyurmama izin verir misiniz?
> Uygun görmezseniz hiç sorun değil, teşekkür ederim.

## 2. Facebook / Instagram paylaşımı (tanıtım videosuyla)

> 🧮 **Çocuğunuz abaküsle zihinden hesap öğrensin, siz de ilk deneyen aile olun**
>
> Merhaba! Çocuklara abaküsü (soroban) adım adım, oyun gibi öğreten **Sorobit** adlı bir uygulama
> geliştirdim. Yayına çıkmadan önce **ücretsiz deneyecek aileler** arıyorum.
>
> 👧🧒 7–14 yaş (ilkokul ve ortaokul) çocuğu olan, **Android** telefonu olan aileler
> 🎯 Abaküsü hiç bilmeyen çocuklar da, kursa giden çocuklar da katılabilir
> 💸 Tamamen ücretsiz, hiçbir ödeme yok
> 🗓️ Tek ricam: 2 hafta kullanıp birkaç kısa soruya cevap vermeniz
>
> Katılmak için WhatsApp'tan yazmanız yeterli, kurulumu adım adım anlatan kısa bir video gönderiyorum:
> 👉 [wa.me bağlantısı]

Notlar:
- Grubun kuralı izin veriyorsa bağlantıyı gönderiye, vermiyorsa ilk yoruma koy.
- Yorumda "ben de katılmak istiyorum" yazanlara özelden aynı bağlantıyı gönder.
- Kanal takibi için takip tablosunda "Kanal" sütununa hangi gruptan geldiğini yaz (gerekirse
  "Notlar"a grup adını).

## 3. WhatsApp karşılama mesajı (otomatik)

> Merhaba, Sorobit'e ilginiz için çok teşekkürler! 🙏
> Kurulum yaklaşık 1 dakika sürüyor, birazdan kısa bir video gönderiyorum.
> Bu arada çocuğunuzun kaç yaşında olduğunu yazar mısınız?

(Yaşı sormak hem sohbeti başlatır hem 7–14 dışını baştan ayırır. Cevap gelince `/katilim` gönder.)

## 4. `/katilim` — katılım talimatı (+ katılım videosu)

> Sorobit testine katılım (yalnızca Android, ~1 dakika) 👇
>
> 1️⃣ Gruba katılın: https://groups.google.com/g/sorobit-test → "Gruba katıl"
> 2️⃣ Bu bağlantıyı açın: https://play.google.com/apps/testing/com.numigo.app
>    "Become a tester" düğmesine, sonra "download it on Google Play" bağlantısına dokunun
> 3️⃣ Google Play'den yükleyin
>
> ⚠️ Gruba, telefonunuzdaki Play Store'da açık olan Gmail hesabıyla katılın.
> Kurduktan sonra buraya **"katıldım"** yazmanız yeterli. Takılırsanız yazın, hemen yardımcı olurum.

## 5. `/form1` — "katıldım" gelince

> Harika, hoş geldiniz! 🎉 Çocuğunuz uygulamayı açınca kendi hesabını oluşturacak.
> Hesap açıldıktan sonra 3 dakikalık şu kısa formu doldurabilir misiniz?
> 👉 https://forms.gle/xKspcdRLuLCzMHpU7
>
> Kullanıcı adı, uygulamada profil ekranında adın altında @ ile başlayan addır.
> 🙏 Lütfen en az 14 gün uygulamayı silmeyin ve testten ayrılmayın.

(Bu anda takip tablosuna satır aç: katılım tarihi = bugün, veli adı, telefon, kanal, tanıdık mı.)

## 6. `/form2` — katılımdan 7–10 gün sonra

> Merhaba! Sorobit'i bir haftadır deniyorsunuz, çok teşekkürler. 🙏
> Deneyiminizi 3 dakikalık şu formla paylaşabilir misiniz? Olumsuz görüşler bizim için en değerlisi.
> 👉 https://forms.gle/vcY9sk4H2dzez7tR6

## 7. `/form3` — 3 gündür kullanılmıyorsa (takip tablosunda "Form 3 gönder")

> Merhaba! Sorobit bir süredir kullanılmamış görünüyor, hiç sorun değil. 😊
> Sebebini öğrenmek uygulamayı düzeltmemize çok yardımcı olur; 1 dakikalık form:
> 👉 https://forms.gle/bgpYYKut36opWCoa8

(Suçlayıcı olmasın; "neden bıraktınız" değil, "sebebini öğrenmek yardımcı olur".)

## 8. `/tesekkur` — 14. günden sonra

> Merhaba! Sorobit testinin 2 haftası tamamlandı, katkınız için çok teşekkür ederim. 🙏
> Uygulamayı kullanmaya devam edebilirsiniz; lütfen testten ayrılmayın, yeni sürümler
> otomatik gelecek. Herkese açık yayına çıktığımızda ilk sizin haberiniz olacak.

---

## Dikkat

- **WhatsApp grubu kurma**: yabancı aileler birbirinin numarasını görür. Hep birebir yaz.
- Hedef yaş **7–14** (ilkokul ve ortaokul; kullanıcı kararı 19.10.2026), tanıtım videosundaki
  "7–14 YAŞ İÇİN" ile aynı. Videonun son karesi **"20 aile arıyorum"** diyor; hedef ~100 aile.
  "20" kalırsa erken dolmuş gibi algılanabilir (grafikler `montaj\betikler\grafik_uret.ps1`).
- Gece geç saatte ya da sabah çok erken mesaj atma; hızlı yanıtlarla bile kişiye özel bir selam ekle.

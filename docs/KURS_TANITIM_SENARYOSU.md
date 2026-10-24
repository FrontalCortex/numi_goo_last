# Kurs sahiplerine tanıtım videosu – senaryo ve çekim listesi

Hedef: soroban kursu sahibi / öğretmeni (bkz. `SOROBAN_KURS_PAZARI.md`). Video kurs sahiplerine
WhatsApp'tan gönderilecek; Reels'teki gibi "uygulama tanıtımına geçince düşüş"
burada beklenmiyor, çünkü izleyen kişi zaten ürünü görmek için izliyor. Yine de ilk 5 saniye
**sorunla** açılıyor, uygulamayla değil.

- **Uzun sürüm:** ~2 dk 15 sn (9 sahne). İlgilenen kurs sahibine.
- **Kısa sürüm:** ~45 sn (sahne 1, 4, 5, 6, 9'un kısaltılmışı). İlk WhatsApp mesajı için.
- Ekran yazıları en çok 2 satır.
- **Seslendirme metinden üretilmiş ses** (TTS). Seslendirme sütunundaki cümleler ona göre yazıldı:
  kısa, tek fikirli, kısaltmasız; sayılar yazıyla okunacak biçimde ("on soruluk"). Okuma süresi
  sahne süresini aşarsa önce cümleyi kısalt, sonra çekimi uzat.
- Videoda iletişim bilgisi yok: video zaten WhatsApp sohbetinde gidiyor, cevap oradan geliyor.
- Videoda fiyat, şube sayısı ya da henüz olmayan bir özellik söylenmiyor.

## Seslendirme dosyaları (montajın son hâli, 24.10.2026)

Kurgu `C:UsersASUSVideosSorobit_tanitimmontajetiklerkurs_metin.js` içinde (kesimler, hızlar,
odaklar, ekran yazıları). Her satır ayrı bir ses dosyası: `VideosSorobit_tanitimkursseskurs_soundN.mp3`.
Dosyalar konunca `VIDEO=kurs node ogrenci_montaj.js` yazıların süresini sese göre kendisi ayarlıyor.
Ekran yazısı aynı cümle (vurgular hariç).

| N | Sahne | Metin |
|---|---|---|
| 0 | açılış | Ödevi kim yaptı, kim yapmadı? Derse girmeden bilin. |
| 1 | sınıf aç | Her grubunuz için uygulamada bir sınıf açın. |
| 2 | sınıf aç | Her sınıfın bir katılma kodu olur. |
| 3 | öğrenciler | Kodu velilere gönderin, öğrenci kodu girip sınıfa katılsın. |
| 4 | öğrenciler | Ya da öğrenciyi kullanıcı adıyla davet edin. |
| 5 | ödev ver | Sınıfın Ödevler ekranından yeni ödev verin. |
| 6 | ödev ver | İşlemi ve kuralı seçin: kuralsız, beşlik, onluk, boncuk. |
| 7 | ödev ver | Basamak sayısını, sayı adedini ve hızı siz belirleyin. |
| 8 | ödev ver | Göndermeden önce öğrencinin göreceği örnek soruyu görün. |
| 9 | ödev ver | Kurala uymayan bir seçimde uygulama sizi uyarır. |
| 10 | ödev ver | Tek dokunuşla bütün sınıfa gönderin. |
| 11 | öğrenci çözer | Öğrenciye hemen bildirim gider. |
| 12 | öğrenci çözer | Sayılar sizin seçtiğiniz hızda, tek tek gelir. |
| 13 | öğrenci çözer | Ödev onar soruluk adımlarla çözülür. |
| 14 | öğrenci çözer | Son adım bitince ödev tamamlanır. |
| 15 | kim bitirdi? | Öğrenci ödevi bitirince size bildirim gelir. |
| 16 | kim bitirdi? | Kim bitirdi, kaç doğru yaptı, kim başlamadı: derse girmeden bilin. |
| 17 | ilerleme | Öğrencinin profilinden ders ilerlemesini açın. |
| 18 | ilerleme | Kaç ders bitirdi, ne kadar çalıştı: hepsini görün. |
| 19 | sorular | Öğrencilerinizin soruları yalnızca size gelir. |
| 20 | sorular | Soruyu sahiplenin, öğrencinize kendiniz cevap verin. |
| 21 | kapanış | Sorobit. Kursunuz için sınıf ve ödev takibi, tek uygulamada. |

Çekimde senaryodan sapmalar: 4. sahnede konu kuralsız + 5'lik (uyarı 5'lik + 10'lukta çıkmıyor),
sayı adedi 3; 5. sahnede yanlış cevap yok (20/20); 6. sahnede bildirimler botlardan.

## Çekimden önce hazırlık

| | Ne | Neden |
|---|---|---|
| 1 | **Öğretmen hesabı** (Mehmet Öğretmen, kurs: Sennin School) telefonda açık | Sahnelerin çoğu öğretmen gözünden |
| 2 | **Öğrenci hesabı** emülatörde ya da ikinci telefonda açık | Bildirim ve ödev çözme sahneleri; tek telefonda hesap değiştirmek kayıtta kötü görünür |
| 3 | Gösterilecek sınıf: **24M X5U** (12 bot + öğrenci hesabı) | Liste dolu görünsün; botların adları ve avatarları gerçek gibi duruyor |
| 4 | Sınıfta 3 ödev var; botların yarısı bitirmiş, dörtte biri yarıda, kalanı başlamamış (**hazır**, `tools/kurs/bot_odev_ilerlemesi.js`, 24.10.2026) | Ödev kartlarındaki "6/13 bitirdi" çubuğu ve ayrıntıdaki liste dolu görünsün. Yeni ödev verirsen betiği yeniden çalıştır: önce `--geri-al --yaz`, sonra `--yaz` |
| 5 | Öğrenci hesabının **birkaç derste ilerlemesi** olsun | Sahne 7'deki ilerleme ekranı boş çıkmasın |
| 6 | Öğrenci hesabından öğretmene **1 soru** sorulmuş olsun | Sahne 8 |
| 7 | Telefonda Rahatsız Etme açık (uygulama bildirimleri hariç), saat ve pil düzgün | Kayıtta yabancı bildirim çıkmasın |
| 8 | Ekranda **gerçek bir çocuğun** adı, yüzü ya da kullanıcı adı görünmesin | Kurs sahibine gösterilen videoda bile çocuk verisi olmamalı; yalnızca botlar ve kendi test hesapların |

Kayıt dosya adları önerisi: `kurs1_sorun.mp4`, `kurs2_sinif_ac.mp4` … (öğrenci videosundaki gibi;
saniyeler sonra ham kayda göre bu dosyaya yazılır).

---

## Sahne 1 · Sorun (0:00–0:08) — grafik ya da kısa çekim

Telefon gerekmez. Düz arka plan ya da kâğıt ödev / WhatsApp grubu görüntüsü.

| Yazı | Seslendirme |
|---|---|
| Ödevi kim yaptı, kim yapmadı? | Ödevi veriyorsunuz… ama kim yaptı, kaç doğru yaptı, ancak derste öğreniyorsunuz. |
| **Sorobit ile ödev ve sınıf takibi** | Sorobit bunu değiştiriyor. |

## Sahne 2 · Sınıf açmak (0:08–0:25) — `kurs2_sinif_ac.mp4` · ÖĞRETMEN

**Telefonda:** Sorularım → **Sınıflarım** düğmesi → listede sınıf kartları görünsün (1 sn bekle) →
**+ Yeni sınıf** → ad yaz (ör. "Cumartesi 10:00") → Oluştur → çıkan kod mesajı.

| Yazı | Seslendirme |
|---|---|
| Her grubunuz için bir sınıf açın | Her grubunuz için uygulamada bir sınıf açıyorsunuz. |
| Sınıfa bir katılma kodu verilir | Sınıfın bir katılma kodu oluyor. |

## Sahne 3 · Öğrencileri eklemek (0:25–0:40) — `kurs3_ogrenci_ekle.mp4` · ÖĞRETMEN

**Telefonda:** Sınıflarım → **24M X5U** sınıfı → sağ üstte **ayar** ikonu → alt panelde kod,
**Kopyala / Paylaş** (paylaşa dokunup menüyü göster, sonra kapat) → paneli kapat → **Ekle** →
kullanıcı adı yazma penceresi (yazıp göndermeye gerek yok, iptal).

| Yazı | Seslendirme |
|---|---|
| Kodu velilerle paylaşın | Kodu veli grubunuza gönderin, öğrenci kodu girip sınıfa katılsın. |
| ya da kullanıcı adıyla davet edin | Ya da öğrenciyi kullanıcı adıyla kendiniz davet edin. |

## Sahne 4 · Ödev hazırlamak (0:40–1:05) — `kurs4_odev_hazirla.mp4` · ÖĞRETMEN

Videonun kalbi; acele etme, her dokunuştan sonra yarım saniye bekle.

**Telefonda:** 24M X5U → mor **Ödevler** kartı → ödev listesi (dolu kartlar, bitirme çubukları) →
**Yeni ödev** → sırayla:
1. İşlem: **Toplama**
2. Konu: **5'lik** ve **10'luk**
3. **Abaküslü**, basamak **2**, sayı adedi **5**, hız **1,5 sn**, soru **20** (− / + ile)
4. Aşağı kaydır → özet kartı: başlık ve örnek soru → **yenile** ikonuna 2 kez dokun (soru değişiyor)
5. **Ödevi gönder**

| Yazı | Seslendirme |
|---|---|
| İşlemi ve kuralı seçin | Toplama mı çıkarma mı, hangi kural: kuralsız, beşlik, onluk… |
| Abaküslü ya da zihinden | Abaküsle mi, zihinden mi çözülecek? |
| Basamak, sayı adedi, hız | Basamak sayısını, kaç sayı geleceğini ve hızını siz belirliyorsunuz. |
| Örnek soruyu görün | Göndermeden önce öğrencinin göreceği soruyu görüyorsunuz. |
| Tek dokunuşla gönderin | Ve tek dokunuşla bütün sınıfa gönderiyorsunuz. |

> Not: istersen 3. adımda sayı adedini en yükseğe çıkarıp çıkan **uyarıyı** da göster
> ("bu kurallarla … olur"). Kurs sahibi için güçlü bir ayrıntı: uygulama kurala uymayan soru üretmiyor.

## Sahne 5 · Öğrenci ödevi çözüyor (1:05–1:30) — `kurs5_ogrenci_cozuyor.mp4` · ÖĞRENCİ

**Öğrenci cihazında:** **Yeni ödev** bildirimi gelsin (sahne 4'te gönderince) → bildirime dokun →
Sınıfım → Ödevler ekranı → ödeve dokun → sayılar tek tek yanıp sönüyor → abaküste cevap →
**doğru** paneli alttan kayarak geliyor (ses açık) → birkaç soru → bir **yanlış** cevap da ver →
10 soru bitince blok sonucu.

| Yazı | Seslendirme |
|---|---|
| Öğrenciye hemen bildirim gider | Öğrenciye hemen bildirim gidiyor. |
| Sayılar sizin hızınızda gelir | Sayılar sizin seçtiğiniz hızda, tek tek geliyor. |
| 10'ar soruluk bloklar | Ödev onar soruluk bloklarla çözülüyor; çocuk sıkılmadan bitiriyor. |

## Sahne 6 · Kim bitirdi? (1:30–1:50) — `kurs6_kim_bitirdi.mp4` · ÖĞRETMEN

**Telefonda:** "**Ödev tamamlandı**" bildirimi (öğrenci bitirince gelir; "… bitirdi · 18/20 doğru ·
3/13 öğrenci bitirdi") → bildirime dokun → ödev ayrıntısı: üstte ödevin ayarları, altında öğrenci
listesi (avatarlar; "Bitti · 18/20 doğru", "Adım 1/2 · 8/10 doğru", "Başlamadı") → listeyi yavaşça kaydır.

| Yazı | Seslendirme |
|---|---|
| Biten her ödev size bildirilir | Öğrenci bitirdiğinde size bildirim geliyor. |
| Kim bitirdi, kaç doğru, kim başlamadı | Kim bitirdi, kaç doğru yaptı, kim hiç başlamadı; derse girmeden biliyorsunuz. |

## Sahne 7 · Öğrencinin ilerlemesi (1:50–2:02) — `kurs7_ilerleme.mp4` · ÖĞRETMEN

**Telefonda:** ödev ayrıntısında öğrenci hesabının adına dokun → profili → **ilerleme** düğmesi →
ilerleme ekranı (dersler, başarı oranları) → yavaşça kaydır.

| Yazı | Seslendirme |
|---|---|
| Her öğrencinin ders ilerlemesi | Öğrencinin uygulamadaki bütün derslerdeki ilerlemesini de görüyorsunuz. |

## Sahne 8 · Sorular size gelir (2:02–2:12) — `kurs8_sorular.mp4` · ÖĞRETMEN

**Telefonda:** Sorularım → havuz sekmesinde **Sınıfım** alt sekmesi → öğrencinin sorusu → soruyu aç.

| Yazı | Seslendirme |
|---|---|
| Öğrencinizin soruları yalnızca size | Öğrencilerinizin uygulamada sorduğu sorular başka öğretmene değil, size geliyor. |

## Sahne 9 · Kapanış (2:12–2:20) — grafik

| Yazı | Seslendirme |
|---|---|
| **Sorobit** | Sorobit: kursunuzun sınıf ve ödev takibi, tek uygulamada. |
| Sınıf ve ödev takibi kursunuz için | |

---

## Kısa sürüm (~45 sn)

| Süre | Sahne | Ne kalıyor |
|---|---|---|
| 0–5 | 1 | Yalnızca "Ödevi kim yaptı, kim yapmadı?" |
| 5–20 | 4 | Kural seçimi, örnek soru, Ödevi gönder (ayarların hızlı geçişi) |
| 20–30 | 5 | Bildirim + sayıların gelişi + doğru paneli |
| 30–40 | 6 | Bitirme bildirimi + öğrenci listesi |
| 40–45 | 9 | Kapanış |

## Notlar

- Kurs sahibi WhatsApp'tan "katılmak istiyorum" derse öğretmen hesabı
  `tools/kurs/kurs_ogretmeni_ata.js` ile kursa bağlanıyor (bağımsız öğretmen: `--bagimsiz`).
- Ödevi bitiren her bot için öğretmene "Ödev tamamlandı" bildirimi gitti (24.10.2026); sahne 6'yı
  çekmeden önce bildirim tepsisini temizle, yoksa eski bot bildirimleri görünür.

# Kurs sahibine "neden Sorobit?" videosu – senaryo

**Kime:** soroban kursu sahibi. WhatsApp'tan, ilk mesajla gidecek.
**Amaç:** kurs sahibi videonun sonunda şunu düşünsün: "Ödevi ben hazırlamıyorum, ben kontrol etmiyorum,
kim çalıştı biliyorum, öğrencilerim de benim öğretmenimde kalıyor."
**Ötekinden farkı:** `KURS_TANITIM_SENARYOSU.md` (ve `kurs\Sorobit_kurs_tanitim_taslak.mp4`) öğretmene
düğmeleri adım adım gösteriyor ve öyle kalıyor. Bu video **faydayı** anlatıyor; ekranlar kanıt olarak
geçiyor. Ağırlık ödev oluşturmada: kurs sahibinin ilk sorusu "ödevler benim yöntemime uyuyor mu?" olacak.

- Süre: ~3 dk 36 sn (1,4 kat hızlandırmayla; 25.10.2026 eklemeleriyle). Dikey 1080×1920, öğrenci videosuyla aynı çerçeve.
- Seslendirme: metinden ses. Her satır ayrı dosya (`kurs2\ses\sahip_soundN.mp3`), ekran yazısı aynı cümle.
- Sesli dil: "siz" diliyle, kurs sahibine. Cümleler kısa, kısaltmasız, sayılar yazıyla.
- Videoda fiyat, iletişim bilgisi ve henüz olmayan özellik yok.
- Ekranda gerçek çocuk verisi yok: yalnızca botlar ve test hesapları (Esma, Ted).

## Seslendirme dosyaları (montajın son hâli, 24.10.2026)

Kurgu `VideosSorobit_tanitimmontajetiklersahip_metin.js` içinde; taslak `VideosSorobit_tanitimsahipSorobit_neden_taslak.mp4`.
Ses dosyaları: `VideosSorobit_tanitimsahipsessahip_soundN.mp3`. Dosyalar konunca
`VIDEO=sahip node ogrenci_montaj.js` yazı sürelerini sese göre ayarlıyor. Senaryodaki 1. bölümün iki satırı
açılışta tek ses (0) oldu; "Sorobit, çocukların evde…" satırı kullanıcı kararıyla çıktı (24.10.2026).
Numaralar senaryodaki tablolardan bu yüzden farklı. 25.10.2026: "ders ilerlemesi" satırı yerine ödev başarısı
(kural kural doğru oranı) üç satırı geldi; kayıt sahip3_basari_1.mp4, Esma hesabında gösterim verisiyle.
Aynı gün: Veli (sahip4_veli_1.mp4, Esma, gösterim verisi çekimden sonra silindi), ayrıntılı Sorular (rehberin ogr8'i +
öğretmen videosunun ogt3–5'i) ve Evde (rehberden ders haritası, eğitim, görevler, kupa) bölümleri eklendi; Ödev
oluşturmadaki altı ayrıntı satırı (basamak, çarpmada iki basamak, sayı adedi, eksi, uyarı, hız) tek satıra indi.
Süre 3 dk 36 sn. Yeni satırlar Sarah (ses_uret.js); kullanılmayan eski sesler `ses/api_ilk/kullanilmayan_N.mp3`.

| N | Sahne | Metin |
|---|---|---|
| 0 | açılış | Öğrenciniz haftada bir saat derste. Peki geri kalan altı gün? Ödev kâğıtta kalıyor: kim çalıştı, kaç doğru yaptı, ancak derste öğreniyorsunuz. |
| 1 | sorobit | Kursunuz için bir dijital sınıf: öğretmeniniz öğrencilerini görür, ödev verir, takip eder. |
| 2 | sınıf yapısı | Kursunuz, öğretmenleriniz ve her öğretmenin sınıfları uygulamada birbirine bağlı. |
| 3 | sınıf yapısı | Öğretmen her grubu için bir sınıf açar. Sınıfın bir katılma kodu olur. |
| 4 | sınıf yapısı | Öğrenci kodu girer ve sınıfa katılır. |
| 5 | sınıf yapısı | Öğretmen sınıfındaki bütün öğrencileri tek ekranda görür. |
| 6 | ödev oluşturma | Ödevi öğretmen hazırlamaz, sadece tarif eder. Soruları uygulama üretir. |
| 7 | ödev oluşturma | Önce işlem: toplama, çıkarma ya da çarpma. |
| 8 | ödev oluşturma | Sonra konu: kuralsız, beşlik, onluk, boncuk. |
| 9 | ödev oluşturma | Birden fazla kural seçebilirsiniz. Sorularda yalnızca seçtiğiniz kurallar çıkar. |
| 10 | ödev oluşturma | Her soru, seçtiğiniz kurallarla çözülecek şekilde hesaplanır. Çocuk henüz görmediği bir kurala denk gelmez. |
| 11 | ödev oluşturma | Abaküslü ödevde çocuk ekrandaki abaküsle çözer. Abaküssüz ödevde zihinden hesaplar ve cevabı yazar. |
| 12 | ödev oluşturma | Basamak, sayı adedi, hız: her ayrıntıyı siz belirlersiniz. |
| 13 | ödev oluşturma | Soru sayısını siz seçersiniz; ödev onar soruluk adımlara bölünür, çocuk sıkılmadan bitirir. |
| 14 | ödev oluşturma | Ödevi bütün sınıfa ya da yalnızca seçtiğiniz öğrencilere verirsiniz. Her çocuğa kendi seviyesine göre. |
| 15 | ödev oluşturma | Göndermeden önce öğrencinin görebileceği soruları görürsünüz. |
| 16 | ödev oluşturma | Tek dokunuşla gönderin. |
| 17 | öğrenci çözer | Öğrenciye hemen bildirim gider. |
| 18 | öğrenci çözer | Sayılar sizin seçtiğiniz hızda, tek tek gelir. |
| 19 | öğrenci çözer | Abaküssüz ödevde çocuk cevabı kendisi yazar. |
| 20 | öğrenci çözer | Uygulama her cevabı anında kontrol eder. Öğretmeniniz kâğıt okumaz. |
| 21 | takip | Öğrenci ödevi bitirince öğretmene bildirim gelir. |
| 22 | takip | Kim bitirdi, kaç doğru yaptı, kim başlamadı: derse girmeden bilirsiniz. |
| 23 | takip | Uygulama yalnızca doğruyu yanlışı saymaz, hatanın hangi kuralda yapıldığını da bulur. |
| 24 | takip | Öğrencinin beşlikte mi, onlukta mı zorlandığını kural kural görürsünüz. |
| 25 | takip | Sıradaki ödevi bu eksiğe göre verin. |
| 26 | veli | Veli de çocuğunun telefonunda Veli Paneli'nden ilerlemeyi kendisi görür. |
| 27 | veli | Ne kadar çalıştı, hangi kuralda zorlanıyor: hepsi tek ekranda. |
| 28 | veli | Veli görüşmesinde somut veriyle konuşursunuz. |
| 29 | sorular | Öğrenci evde takılırsa soruyu fotoğraf ya da videoyla sorar. |
| 30 | sorular | Ekranını kaydeder, takıldığı yeri çizerek sesli anlatır. |
| 31 | sorular | Öğrencinizin sorduğu sorular başka öğretmene değil, yalnızca sizin öğretmeninize gider. |
| 32 | sorular | Öğretmeniniz videoyu izler, çocuğun nerede takıldığını görür. |
| 33 | sorular | Cevabı da aynı şekilde verir: abaküste çizerek sesli anlatır. |
| 34 | sorular | Çocuk bir sonraki dersi beklemeden öğrenir. |
| 35 | evde | Ödevin dışında öğrenciniz derste gördüğü konuyu evde tekrar eder. |
| 36 | evde | Konu adım adım anlatılır, testle pekiştirilir. |
| 37 | evde | Görevler ve kupalarla her gün kendi isteğiyle çalışır. |
| 38 | kapanış | Ödevi uygulama hazırlar. Uygulama kontrol eder. Siz kimin çalıştığını bilirsiniz. Sorobit. |

## Akış (özet)

| # | Bölüm | Ne anlatıyor | Süre |
|---|---|---|---|
| 1 | Sorun | Ders haftada bir saat; pratik evde, ama görünmüyor | 0:00–0:10 |
| 2 | Çözüm | Sorobit: abaküs uygulaması + kursunuzun dijital sınıfı | 0:10–0:22 |
| 3 | Yapı | Kurs → öğretmen → sınıf → öğrenci; öğrenci kodla katılır | 0:22–0:40 |
| 4 | Ödev oluşturma (ana bölüm) | İşlem, kural, tür, basamak, sayı, hız, soru, kime, örnek soru | 0:40–1:50 |
| 5 | Öğrenci çözer | Bildirim, sayılar tek tek, otomatik kontrol | 1:50–2:10 |
| 6 | Takip | Kim bitirdi, kaç doğru, ilerleme | 2:10–2:30 |
| 7 | Sorular kursta kalır | Öğrencinin sorusu yalnızca kendi öğretmenine | 2:30–2:38 |
| 8 | Kapanış | Üç fayda, tek cümle | 2:38–2:45 |

---

## 1 · Sorun (grafik, ~10 sn)

Telefon yok. Mor zeminde iki kısa kart, art arda.

| N | Ekran yazısı = seslendirme |
|---|---|
| 0 | Öğrenciniz haftada bir saat derste. Peki geri kalan altı gün? |
| 1 | Ödev kâğıtta kalıyor: kim çalıştı, kaç doğru yaptı, ancak derste öğreniyorsunuz. |

## 2 · Çözüm (~12 sn)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 2 | Öğrenci uygulaması: ders haritası, abaküs dersi (`ogr2_harita.mp4` 20–29, hızlı) | Sorobit, çocukların evde abaküs çalıştığı bir uygulama. |
| 3 | Öğretmenin sınıf ekranı: Ödevler kartı + öğrenci listesi (`kurs3_ogrenci_ekle_1.mp4` 24–26, 33–35) | Kursunuz için de bir dijital sınıf: öğretmeniniz öğrencilerini görür, ödev verir, takip eder. |

## 3 · Yapı (~18 sn)

İlk kart **grafik**: dört kutu yukarıdan aşağı, okla bağlı. Kurs (Sennin School) → Öğretmen (Mehmet
Öğretmen) → Sınıf (Cumartesi 10:00) → Öğrenciler (avatar dizisi).

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 4 | Grafik: kurs → öğretmen → sınıf → öğrenci | Kursunuz, öğretmenleriniz ve her öğretmenin sınıfları uygulamada birbirine bağlı. |
| 5 | `kurs2_sinif_ac_1.mp4` 19–41 (yazma hızlı) | Öğretmen her grubu için bir sınıf açar. Sınıfın bir katılma kodu olur. |
| 6 | `kurs3_ogrenci_ekle_1.mp4` 26.5–32.5 (kod paneli) | Öğrenci kodu girer ve sınıfa katılır. |
| 7 | `kurs3_ogrenci_ekle_1.mp4` 39–44 (liste kayıyor) | Öğretmen sınıfındaki bütün öğrencileri tek ekranda görür. |

## 4 · Ödev oluşturma (ana bölüm, ~70 sn)

Her ayar için ayrı ekran yazısı; o ayarın kartına **sarı odak**. Mevcut kayıtta olmayanlar YENİ ÇEKİM
(listesi aşağıda).

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 8 | `kurs4` 35–39.5: Ödevler listesi → Yeni ödev | Ödevi öğretmen hazırlamaz, sadece tarif eder. Soruları uygulama üretir. |
| 9 | **YENİ Y1**: İşlem kartı; Toplama → Çıkarma → Çarpma | Önce işlem: toplama, çıkarma ya da çarpma. |
| 10 | `kurs4` 39.5–41.4, odak Konu kartı | Sonra konu: kuralsız, beşlik, onluk, boncuk. |
| 11 | **YENİ Y2**: Konu'da seçimler açılıp kapanıyor; altındaki açıklama odakta | Birden fazla kural seçebilirsiniz. Sorularda yalnızca seçtiğiniz kurallar çıkar. |
| 12 | `kurs4` donmuş kare + odak (ör. 61.5 civarı örnek soru) | Her soru, seçtiğiniz kurallarla çözülecek şekilde hesaplanır. Çocuk henüz görmediği bir kurala denk gelmez. |
| 13 | **YENİ Y3**: Abaküslü ↔ Abaküssüz | Abaküslü ödevde çocuk ekrandaki abaküsle çözer. Abaküssüz ödevde zihinden hesaplar ve cevabı yazar. |
| 14 | `kurs4` 52–57: basamak 1 → 2 | Basamak sayısını siz seçersiniz: abaküslüde beş, abaküssüzde sekiz basamağa kadar. |
| 15 | **YENİ Y4**: Çarpma seçili; "1. sayının basamağı / 2. sayının basamağı" | Çarpmada iki sayının basamağını ayrı ayrı belirlersiniz. |
| 16 | `kurs4` 52–57: sayı adedi 3 → 4 | Kaç sayı toplanacağını siz belirlersiniz. |
| 17 | **YENİ Y5**: Çıkarma seçili; "Çıkarılacak sayı adedi" ve açıklaması odakta | Çıkarmada sonuç hiçbir zaman eksiye düşmez. |
| 18 | `kurs4` 72–80: sayı adedi artıyor, sarı uyarı (donmuş kare + odak) | Seçim kurala uymuyorsa uygulama sizi uyarır ve uygun değeri önerir. |
| 19 | **YENİ Y6**: Gösterilme hızı −/+ (1,5 → 0,5 → 3) | Sayıların ekranda kalma süresini saniyenin onda biri ile otuz saniye arasında ayarlarsınız. |
| 20 | **YENİ Y7**: Soru adedi 20 → 50; altında "5 adım" | Soru sayısını siz seçersiniz; ödev onar soruluk adımlara bölünür, çocuk sıkılmadan bitirir. |
| 21 | **YENİ Y8**: Kime → Öğrenci seç → iki öğrenci işaretle | Ödevi bütün sınıfa ya da yalnızca seçtiğiniz öğrencilere verirsiniz. Her çocuğa kendi seviyesine göre. |
| 22 | `kurs4` 61.5–69: özet kartı, yenile (odak) | Göndermeden önce öğrencinin göreceği soruları görürsünüz. |
| 23 | `kurs4` 92.5–97.5 + 107.5–113 | Tek dokunuşla gönderin. |

## 5 · Öğrenci çözer (~20 sn)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 24 | `kurs5` 20.5–26.5, odak bildirim | Öğrenciye hemen bildirim gider. |
| 25 | `kurs5` 32.5–41.2 | Sayılar sizin seçtiğiniz hızda, tek tek gelir. |
| 26 | **YENİ Y9**: abaküssüz bir ödev, cevabı yazma ekranı | Abaküssüz ödevde çocuk cevabı kendisi yazar. |
| 27 | `kurs5` 100–104 ve 172.5–176.5 | Uygulama her cevabı anında kontrol eder. Öğretmeniniz kâğıt okumaz. |

## 6 · Takip (~20 sn)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 28 | `kurs6` 18.5–25.3, odak bildirimler | Öğrenci ödevi bitirince öğretmene bildirim gelir. |
| 29 | `kurs6` 25.3–35 | Kim bitirdi, kaç doğru yaptı, kim başlamadı: derse girmeden bilirsiniz. |
| 30 | `kurs7` 26.8–36 | Her öğrencinin uygulamadaki ders ilerlemesi de öğretmeninizin elinde. |

## 7 · Sorular kursta kalır (~8 sn)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 31 | `kurs8` 38.5–46, odak Sınıfım sekmesi | Öğrencinizin uygulamada sorduğu sorular başka öğretmene değil, yalnızca sizin öğretmeninize gider. |

## 8 · Kapanış (grafik, ~7 sn)

Üç satır sırayla belirir, sonra "Sorobit".

| N | Ekran yazısı = seslendirme |
|---|---|
| 32 | Ödevi uygulama hazırlar. Uygulama kontrol eder. Siz kimin çalıştığını bilirsiniz. Sorobit. |

---

## Yeni çekimler (öğretmen hesabı, 24M X5U → Ödevler → Yeni ödev)

Hepsi tek kayıtta çekilebilir (`sahip1_form.mp4`); her adımdan önce ve sonra 2 sn bekle, montajda
ayıklanır. Ödevi **göndermeden** geri çık.

| Kod | Ne yapılacak |
|---|---|
| Y1 | İşlem kartında sırayla Çıkarma, Çarpma, sonra yeniden Toplama'ya dokun |
| Y2 | Konu'da 10'luk ve Boncuk'u aç, sonra kapat (kuralsız + 5'lik kalsın); altındaki açıklama görünsün |
| Y3 | Abaküssüz'e dokun (basamak üst sınırı değişir), sonra Abaküslü'ye dön |
| Y4 | Çarpma'ya dokun; "1. sayının basamağı / 2. sayının basamağı" alanları görünsün; − / + ile birini değiştir |
| Y5 | Çıkarma'ya dokun; "Çıkarılacak sayı adedi" ve altındaki "Başlangıç sayısı hariç…" açıklaması görünsün |
| Y6 | Toplama'ya dön; gösterilme hızını − ile 0,5'e indir, + ile 3'e çıkar |
| Y7 | Soru adedini + ile 50'ye çıkar; altında "5 adım" görünsün |
| Y8 | Kime → Öğrenci seç; iki öğrenciyi (bot) işaretle, sonra Tüm sınıf'a dön |

**Y9 (öğrenci hesabı, ayrı kayıt `sahip2_abakussuz.mp4`):** önce öğretmenden Esma'ya abaküssüz,
kısa (3–5 soru) bir ödev ver; Esma ile aç ve 2–3 soru çöz. Cevabın yazıldığı ekran görünsün.

## Notlar

- 12. satırın iddiası ("çocuk henüz görmediği kurala denk gelmez") ödev motorunun kuralına dayanıyor:
  her basamaktaki her hamle seçilen kurallardan biri olmak zorunda (`HomeworkEngine`; tek istisna:
  onluk/boncuk geçişinde komşu basamağa giden doğrudan ±1, kuralın parçası sayılıyor).
- Sınır değerleri koddan: abaküslü 5, abaküssüz 8 basamak; hız 0,1–30 sn; soru 1–200; adım 10 soru;
  çıkarmada başlangıç sayısı sayılmıyor, sonuç negatife düşmüyor.
- Montaj, öğretmen videosu gibi `kurs_metin.js` düzeninde yeni bir dosyayla (`sahip_metin.js`)
  yapılacak; 1, 3 ve 8. bölümlerin grafikleri HTML'den üretilecek.

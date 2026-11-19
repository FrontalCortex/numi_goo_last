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

**19.11.2026 — v2 (`sahip\Sorobit_neden_v2.mp4`, 3 dk 10 sn):** SINIF YAPISI'nda beklemeler kesildi (sınıf açılırken
yükleme, Yeni sınıf'tan önceki duraklama, kod panelinin uzun durması); sahne 34,8 → 24,2 sn. SORULAR bölümü (32–37. satırlar: soru gönderme,
ekran kaydı, öğretmenin videoyla cevabı) çıktı; özellik kapalı. SINIF YAPISI'nın başındaki eski Soru havuzu ekranı
kesildi (`kurs2_sinif_ac_1.mp4` artık 21,65'ten; kayıt değişken kare hızlı). Sesler yazı sırasıyla numaralı olduğu için
yeni önek `sahip_v2_sound` (0–31 aynı, eski 38–41 → 32–35); eski dosyalar ve `Sorobit_neden_taslak.mp4` duruyor,
betiğin eski hâli `sahip_metin.js.yedek_20261119`. Bütün sahneler baştan üretildi: önbellekteki `s6.mp4` sesten
kısa kalmıştı ve VELİ'den sonrası görüntüsüz çıkıyordu.

## Endonezce çekim (19–20.11.2026)

Ham kayıtlar `Videos\Sorobit_tanitim\id_*.mp4`. Arayüz Bahasa Indonesia, telefon Xiaomi, versionCode 26 debug.
Kayıtlar değişken kare hızlı; kesim yerini karelerin zaman damgasıyla bul (v2'deki gibi). Montaj, seslendirme ve çeviri
henüz yapılmadı. Hedef süre 2–2,5 dk. ÖDEV OLUŞTURMA tek akıcı form kaydına indi, SORULAR yok.

**Ekrandaki veri:** öğretmen **Bu Sari** (Mehmet Öğretmen hesabının adı değiştirildi), kurs Sennin School, sınıf
**Minggu 14:00** (kod PGW SX8; eski "Pazar 14:00" yeniden adlandırıldı), 12 Endonezce adlı bot
(`tools/kurs/sinifa_bot_ekle.js --dil id`) + öğrenci **Ted**. Esma kullanılmadı. 3. bölümün yapı grafiği (`sahip_yapi`)
Endonezce sürümde bu adlarla üretilmeli: Sennin School → Bu Sari → Minggu 14:00.

| Kayıt | Süre | Sahne | İçerik |
|---|---|---|---|
| `id_sinif_ac_1.mp4` | 38,3 sn | s3 (sınıf açar, kod) | Kelas-kelasku (Minggu 14:00) → + Kelas baru → "Sabtu 10:00" yazılıyor → Buat kelas → Pengaturan kelas, kod XMW GGA. Sabtu 10:00 çekimden sonra arşivlendi. |
| `id_odev_formu_1.mp4` | 110,1 sn | s4 (ödev oluşturma, tek geçiş) | Minggu 14:00 (PR kartı boş) → PR baru: Operasi, Topik (Langsung + Teman Kecil), Dengan/Tanpa sempoa, Jumlah digit, Banyak angka, Kecepatan tampil, Banyak soal, Untuk siapa (Pilih murid ↔ Seluruh kelas), özet kartında örnek soru + yenile → Kirim PR → "PR dikirim ke 13 murid". |
| `id_sinif_ekrani_1.mp4` | 22,5 sn | s2 + s3 (sınıf ekranı, kod, liste) | Minggu 14:00: PR kartı (1 PR) + Murid (13) → dişli → kod PGW SX8 → Salin, "Kode disalin" → liste Ted'e kadar kayıyor. |
| `id_ogrenci_cozuyor_1.mp4` | 111,8 sn | s5 (bildirim, sayılar, kontrol) | Ted profilde; ~33 sn'de bildirim çekmecesi: "PR baru — Bu Sari: penjumlahan langsung, teman kecil, 10 soal" (yanında MIUI'nin Türkçe USB bildirimi ve tarih: bulanıklaştır) → PR-ku → abaküslü 10 soru, "Benar!" → "Langkah selesai! 10/10 benar". |
| `id_abakussuz_1.mp4` | 35,8 sn | s5 (abaküssüz cevap yazma) | PR-ku → Tanpa sempoa 5 soal → sayılar tek tek, cevap klavyeyle yazılıyor (Soal 1–3/5). Sonuç ekranı yok (kullanıcı kararı). |
| `id_takip_1.mp4` | 85,9 sn | s6 (bildirim, kim bitirdi, ödev başarısı) | Öğretmen ana ekranı → bildirim çekmecesi: "PR selesai … Rizky Siregar selesai · 10/10 benar · 7/13 murid selesai" (USB bildirimi bulanıklaştır) → ödev ayrıntısı (Selesai / Belum mulai) → sınıf → Ted — Kemajuan (iki kart) → Keberhasilan rumus. DİKKAT: bu görünümde "Minggu ini belum ada soal yang dikerjakan" yazıyor; grafik ve kural satırları kullanılabilir. |
| `id_gelismis_1.mp4` | 26,8 sn | s4 (Gelişmiş panel; Türkçe v2'deki guncel3_gelismis satırı) | 20.11.2026 ek kayıt, PR baru formu içinden (sınıf/öğretmen adı görünmüyor): Langsung + Teman Kecil → Lanjutan → "Angka mana yang ditambahkan?" → Teman Kecil'de 4 kapanıyor → Oke → satırda "Teman Kecil: 1, 2, 3". |
| `id_veli_1.mp4` | 59,5 sn | veli | Ted: Pengaturan (Panel Orang Tua satırı) → Panel Orang Tua iki kart → Pelajaran (haftalık grafik, bölümler) → Keberhasilan rumus ("Minggu ini 100%", kural kural ustalık). |
| `id_evde_1.mp4` | 28,5 sn | evde (harita, görevler, kupa) | Ted: ders haritası kayıyor → Misi harian (görevler) → profil, Jalur Piala → Pencapaian (kupalar, rozetler). |

Evde bölümünün "konu adım adım anlatılır, testle pekiştirilir" satırı için Reklam 11'in Endonezce ham kayıtları
kullanılabilir: `r11id_egitim_1.mp4` (abaküste Endonezce anlatım, 32 sn) ve `r11id_test_1.mp4` (Sobi kuisi, "Soal 4/10").

**İki dilde de uygulamada kalan Türkçe (montajda atla ya da bulanıklaştır):** Veli panelindeki ders listesinde
"Bitti · 2 steps, hepsi ilk denemede" (`ParentReportAdapter.kt` ~302, metin koda gömülü) ve Başarımlar'daki
"Kusursuz Odak" rozet adı (`BadgeProgressEngine.kt` 61). Uygulama tarafında düzeltilmesi ayrı iş.

## İngilizce çekim (20.11.2026)

Ham kayıtlar `Videos\Sorobit_tanitim\en_*.mp4`; Endonezceyle aynı 8 sahne, arayüz English. Öğretmen **Mr. James**
(aynı hesap, ad yeniden değiştirildi), sınıf **Sunday 2:00 PM** (kod QQS L6H, öğretmen adına sunucudan açıldı),
12 İngilizce adlı bot (`--dil en`) + Ted. Endonezce sınıf Minggu 14:00 arşivde, verisi duruyor. Yapı grafiği:
Sennin School → Mr. James → Sunday 2:00 PM. Evde bölümünün anlatım/test kısmı için `r11en_egitim_1.mp4`, `r11en_test_1.mp4`.

| Kayıt | Süre | Sahne | İçerik |
|---|---|---|---|
| `en_sinif_ac_1.mp4` | 38,0 sn | s3 | My classes (Sunday 2:00 PM) → + New class → "Saturday 10:00" → Create class → "Class created. Code: HU9 DNC" (kısa bildirim; kod paneli YOK). Klavye öneri şeridinde Türkçe kelimeler var (kullanıcı böyle kalsın dedi). Saturday 10:00 arşivlendi. |
| `en_odev_formu_1.mp4` | 76,3 sn | s4 | Sunday 2:00 PM → Homework → New homework: Operation, Direct + Small friends, With/Without abacus, Digits, How many numbers, Display speed, Number of questions, Assign to (Choose students ↔ Whole class), örnek soru + yenile → gönder → "0/13 finished". Sınıf ekranı ilk açılışta avatarlar yerine baş harfler (bir an). |
| `en_sinif_ekrani_1.mp4` | 25,8 sn | s2 + s3 | Sınıf ekranı (Homework kartı, Students (13)) → Class settings, kod QQS L6H → Copy, "Code copied" → liste Ted'e kadar. |
| `en_ogrenci_cozuyor_1.mp4` | 100,6 sn | s5 | Ted uygulama ana ekranında; ~25–25,7 sn bildirim çekmecesi "New homework — Mr. James: direct, small friends addition, 10 …" (yalnızca ~1 sn: dondur) → My homework → abaküslü 10 soru → "Completed". |
| `en_abakussuz_1.mp4` | 27,1 sn | s5 (abaküssüz) | My homework → Without abacus 5 questions → cevap klavyeyle (Question 1–3/5). |
| `en_takip_1.mp4` | 84,3 sn | s6 | Öğretmen sınıf ekranında; ~26 sn bildirim "Homework done … Ruby Wright finished · 8/10 correct · 7/13 students finished" → ödev ayrıntısı (Done / Not started) → Ted — Progress → Rule success → Lessons. |
| `en_gelismis_1.mp4` | 23,8 sn | s4 (Gelişmiş panel) | New homework formu içinden: Direct + Small friends → Advanced → "Which numbers should be added?" → Small friends'te 4 kapanıyor (1, 2, 3 kalıyor). Kayıt panel açıkken bitiyor, kapanış yok. |
| `en_veli_1.mp4` | 44,2 sn | veli | Settings (Parent Panel) → Parent Panel dört kart → Lessons (Türkçe "Bitti …" satırları: atla) → Rule success (grafik, kural kural). |
| `en_evde_1.mp4` | 26,7 sn | evde | Ders haritası kayıyor → Daily missions → profil, Trophy road → Achievements ("Kusursuz Odak" rozeti Türkçe). |

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

26.10.2026: Gelişmiş paneli satırı (guncel3_gelismis) eklendi; Takip ve Veli bölümlerinde Ödev başarısı yeni ekranla
(guncel1_ogt_basari: öğretmenden Ted; guncel2_veli: Ted'in kendi hesabı). İki yeni satır (Bu hafta, grafik). Süre 3 dk 51 sn.
Takip'te profil hâlâ Esma'nınki (sahip3_basari), sonraki ekranlar Ted'in; kullanıcı bunu sorun saymadı.

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
| 13 | ödev oluşturma | Gelişmiş panelde hangi sayıların geleceğini bile siz seçersiniz: örneğin beşlikte yalnızca 1, 2 ve 3. |
| 14 | ödev oluşturma | Soru sayısını siz seçersiniz; ödev onar soruluk adımlara bölünür, çocuk sıkılmadan bitirir. |
| 15 | ödev oluşturma | Ödevi bütün sınıfa ya da yalnızca seçtiğiniz öğrencilere verirsiniz. Her çocuğa kendi seviyesine göre. |
| 16 | ödev oluşturma | Göndermeden önce öğrencinin görebileceği soruları görürsünüz. |
| 17 | ödev oluşturma | Tek dokunuşla gönderin. |
| 18 | öğrenci çözer | Öğrenciye hemen bildirim gider. |
| 19 | öğrenci çözer | Sayılar sizin seçtiğiniz hızda, tek tek gelir. |
| 20 | öğrenci çözer | Abaküssüz ödevde çocuk cevabı kendisi yazar. |
| 21 | öğrenci çözer | Uygulama her cevabı anında kontrol eder. Öğretmeniniz kâğıt okumaz. |
| 22 | takip | Öğrenci ödevi bitirince öğretmene bildirim gelir. |
| 23 | takip | Kim bitirdi, kaç doğru yaptı, kim başlamadı: derse girmeden bilirsiniz. |
| 24 | takip | Uygulama yalnızca doğruyu yanlışı saymaz, hatanın hangi kuralda yapıldığını da bulur. |
| 25 | takip | Bu haftanın başarısını geçen haftayla karşılaştırırsınız. |
| 26 | takip | Grafikte öğrencinin her konuda nasıl ilerlediğini görürsünüz. |
| 27 | takip | Öğrencinin beşlikte mi, onlukta mı zorlandığını kural kural görürsünüz. |
| 28 | takip | Sıradaki ödevi bu eksiğe göre verin. |
| 29 | veli | Veli de çocuğunun telefonunda Veli Paneli'nden ilerlemeyi kendisi görür. |
| 30 | veli | Ne kadar çalıştı, hangi kuralda zorlanıyor: hepsi tek ekranda. |
| 31 | veli | Veli görüşmesinde somut veriyle konuşursunuz. |
| 32 | sorular | Öğrenci evde takılırsa soruyu fotoğraf ya da videoyla sorar. |
| 33 | sorular | Ekranını kaydeder, takıldığı yeri çizerek sesli anlatır. |
| 34 | sorular | Öğrencinizin sorduğu sorular başka öğretmene değil, yalnızca sizin öğretmeninize gider. |
| 35 | sorular | Öğretmeniniz videoyu izler, çocuğun nerede takıldığını görür. |
| 36 | sorular | Cevabı da aynı şekilde verir: abaküste çizerek sesli anlatır. |
| 37 | sorular | Çocuk bir sonraki dersi beklemeden öğrenir. |
| 38 | evde | Ödevin dışında öğrenciniz derste gördüğü konuyu evde tekrar eder. |
| 39 | evde | Konu adım adım anlatılır, testle pekiştirilir. |
| 40 | evde | Görevler ve kupalarla her gün kendi isteğiyle çalışır. |
| 41 | kapanış | Ödevi uygulama hazırlar. Uygulama kontrol eder. Siz kimin çalıştığını bilirsiniz. Sorobit. |

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

# Öğrencilere sınıf ve ödev videosu – senaryo ve çekim listesi

**Kime:** kursa giden öğrenci (7–12 yaş) ve yanındaki veli. Kurs öğretmeni sınıfın WhatsApp
grubuna gönderecek; öğretmen videosunun (`KURS_TANITIM_SENARYOSU.md`) öğrenci tarafı.
**Amaç:** çocuk videodan sonra tek başına şunları yapabilsin: koda ya da davete göre sınıfa katılmak,
Sınıfım'ı bulmak, ödevi açıp adım adım çözmek, sorusunu öğretmenine sormak.

- Süre: ~2 dk (1,4 kat hızla). Dikey 1080×1920, öteki videolarla aynı çerçeve ve çipler.
- Seslendirme: metinden ses, Sarah (ElevenLabs), ekran yazısıyla aynı cümle (`ses_uret.js`).
- Dil: **"sen"**, kısa cümleler, bir cümlede tek iş. Düğme adları ekrandaki gibi ve kalın.
- Ekranda gerçek çocuk verisi yok: öğrenci olarak Ted ve Esma (test hesapları), sınıfta botlar.
- Öğretmenin adı ekranda "Mehmet Öğretmen" görünüyor (test öğretmen hesabı); sorun değil.

## Seslendirme dosyaları (montajın son hâli, 25.10.2026)

Kurgu `VideosSorobit_tanitimmontajetiklerogrsinif_metin.js`; taslak `VideosSorobit_tanitimogrsinifSorobit_ogrenci_sinif_taslak.mp4`
(1 dk 40 sn). Sesler `VideosSorobit_tanitimogrsinifsesogrsinif_soundN.mp3` (Sarah, ses_uret.js ogrsinif).

| N | Sahne | Metin |
|---|---|---|
| 0 | açılış | Sınıfınla Sorobit'te buluş! Öğretmeninden ödev al, sorularını ona sor. |
| 1 | sınıfa katıl | Sınıfına katılmak için profilindeki ayarlardan Sınıfa katıl'a dokun. |
| 2 | sınıfa katıl | Öğretmeninin verdiği 6 karakterli kodu yaz. |
| 3 | sınıfa katıl | Katıl'a dokun. Artık sınıftasın! |
| 4 | davet | Öğretmenin seni kullanıcı adınla da davet edebilir. Davetin Sınıfa katıl ekranında görünür. |
| 5 | davet | Davette Katıl'a dokunman yeter. |
| 6 | sınıfım | Sınıfına katılınca Sorularım'da Sınıfım sekmesi açılır. |
| 7 | sınıfım | Burada sınıfının kartı ve ödevlerin var. |
| 8 | sınıfım | Sınıf kartında öğretmenini ve sınıf arkadaşlarını görürsün. |
| 9 | sınıfım | Arkadaşlarının profillerine bakabilirsin. |
| 10 | sınıfım | Sınıftan ayrılmak istersen sağ üstteki düğmeye dokun. |
| 11 | ödev | Öğretmenin ödev verince sana bildirim gelir. |
| 12 | ödev | Ödevlerin burada. Her kartta kaç adım bitirdiğin yazar. |
| 13 | ödev | Ödeve dokun, hazır olunca Başla'ya bas. |
| 14 | ödev | Sayılar tahtada tek tek gelir. Her sayıyı geldiği anda abaküsünde topla. |
| 15 | ödev | Sayılar bitince Kontrol'e bas. Sonra Devam. |
| 16 | ödev | Abaküssüz ödevde cevabı aklından bul ve yaz. |
| 17 | ödev | Yanlış yaparsan doğru cevabı görürsün. Üzülme, sonrakine geç! |
| 18 | adımlar | Ödev onar soruluk adımlara bölünür. |
| 19 | adımlar | İstersen hemen sonraki adıma geç, istersen sonra devam et. |
| 20 | adımlar | Adımın ortasında çıkarsan o adıma baştan başlarsın. |
| 21 | adımlar | Son adımı bitirince ödevin tamamlanır. Öğretmenin de görür! |
| 22 | sorular | Takıldığın soruyu sorduğunda soru doğrudan kendi öğretmenine gider. |
| 23 | sorular | Öğretmeninin cevabını Sorularım'da görürsün. |
| 24 | kapanış | Ödevlerini zamanında bitir, her gün biraz çalış! Sorobit. |

Çekimde senaryodan sapmalar: yeni çekimler Ted ile (Esma çekim sırasında sınıftan çıkmıştı); davet
bildirimi gelmediği için davet Sınıfa katıl ekranından gösterildi; Ö5 çekilmedi, sorular öğrenci rehberinin
ham kayıtlarından (ogr8, ogr9; sınıf yapısından önce çekildi). Sınıf ekranı yeniden çekildi (ogrsinif5, Ted):
37311c7'den sonra öğretmen ilk açılışta listenin başında; kurguda o açılış kullanıldı.

## Akış (özet)

| # | Bölüm | Çip | Süre |
|---|---|---|---|
| 1 | Açılış | – | 0:00–0:05 |
| 2 | Sınıfa katıl: kodla | SINIFA KATIL | 0:05–0:22 |
| 3 | Sınıfa katıl: davetle | DAVET | 0:22–0:32 |
| 4 | Sınıfım sekmesi ve sınıf ekranı | SINIFIM | 0:32–0:50 |
| 5 | Ödev: bildirim, liste, çözme | ÖDEV | 0:50–1:30 |
| 6 | Adımlar ve bitirme | ADIMLAR | 1:30–1:45 |
| 7 | Sorular öğretmenine | SORULAR | 1:45–1:55 |
| 8 | Kapanış | – | 1:55–2:00 |

---

## 1 · Açılış (grafik)

| N | Ekran yazısı = seslendirme |
|---|---|
| 0 | Sınıfınla Sorobit'te buluş! Öğretmeninden ödev al, sorularını ona sor. |

Grafik: çip "ÖĞRENCİ REHBERİ", başlık "Sınıfınla Sorobit'te", beyaz kartta "buluş!".

## 2 · Sınıfa katıl: kodla — **YENİ Ö1** (Ted)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 1 | Profil → sağ üstte ayarlar → Sınıf bölümünde **Sınıfa katıl** satırı (odak) | Sınıfına katılmak için profilindeki ayarlardan **Sınıfa katıl**'a dokun. |
| 2 | "Sınıf kodunu gir" kutusu; kod yavaşça yazılıyor (24M X5U) | Öğretmeninin verdiği **6 karakterli kodu** yaz. |
| 3 | **Katıl** → "Cumartesi 10:00 sınıfına katıldın!" | **Katıl**'a dokun. Artık sınıftasın! |

## 3 · Sınıfa katıl: davetle — **YENİ Ö2** (Ted)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 4 | "Sınıf daveti" bildirimi (bildirim dışı bulanık) → dokununca Davetler | Öğretmenin seni kullanıcı adınla da davet edebilir. |
| 5 | Davet kartında **Katıl** (odak) → katıldın mesajı | Gelen davette **Katıl**'a dokunman yeter. |

## 4 · Sınıfım — **YENİ Ö3** (Esma)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 6 | Alt menüde Sorularım → üstte **Sınıfım** sekmesi | Sınıfına katılınca Sorularım'da **Sınıfım** sekmesi açılır. |
| 7 | Sınıf kartı ve Ödevlerim kartı (sırayla odak) | Burada sınıfının kartı ve **ödevlerin** var. |
| 8 | Sınıf kartına dokun → başlıkta sınıfın adı, Sınıftakiler listesi, en başta öğretmen | Sınıf kartında öğretmenini ve **sınıf arkadaşlarını** görürsün. |
| 9 | Bir arkadaşa dokun → profili → geri | Arkadaşlarının profillerine bakabilirsin. |
| 10 | Sağ üstteki ayrılma ikonu (odak, dokunulmuyor) | Sınıftan ayrılmak istersen sağ üstteki düğmeye dokun. |

## 5 · Ödev — mevcut kayıtlar + **YENİ Ö4**

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 11 | `kurs5` 22,5–26,6 "Yeni ödev" bildirimi (bildirim dışı bulanık) | Öğretmenin ödev verince sana **bildirim** gelir. |
| 12 | `kurs5` 27,9–32,5 Ödevlerim listesi (kartlarda "0/2 adım") | Ödevlerin burada. Her kartta kaç adım bitirdiğin yazar. |
| 13 | `kurs5` 32,5–34,5 ödeve dokun → **Başla** (odak) | Ödeve dokun, hazır olunca **Başla**'ya bas. |
| 14 | `kurs5` 34,5–41,2 sayılar tek tek, abaküste boncuklar, Doğru! | Sayılar tahtada **tek tek** gelir. Her sayıyı geldiği anda abaküsünde topla. |
| 15 | `kurs5` (bir sonraki soru) Kontrol (odak) → Doğru! → Devam | Sayılar bitince **Kontrol**'e bas. Sonra **Devam**. |
| 16 | `sahip2_abakussuz` 30–36,4 abaküssüz: cevap kutusu, klavye | Abaküssüz ödevde cevabı aklından bul ve **yaz**. |
| 17 | **YENİ Ö4**: bilerek yanlış cevap → kırmızı panel "Doğru cevap: …" | Yanlış yaparsan doğru cevabı görürsün; üzülme, sonrakine geç! |

## 6 · Adımlar ve bitirme

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 18 | `kurs5` 100–104 "Adım bitti! 10/10 doğru" | Ödev **onar soruluk** adımlara bölünür. |
| 19 | Aynı ekranda **Sonraki adım** / **Bitir** (odak) | İstersen hemen sonraki adıma geç, istersen sonra devam et. |
| 20 | **YENİ Ö4**: adımın ortasında çarpı → "Ödevden çıkılsın mı?" penceresi | Adımın ortasında çıkarsan o adıma **baştan** başlarsın. |
| 21 | `kurs5` 172,5–176,5 "Ödevin tamamlandı! 🎉" | Son adımı bitirince ödevin **tamamlanır**. Öğretmenin de görür! |

## 7 · Sorular öğretmenine — **YENİ Ö5** (Esma)

| N | Ekran | Ekran yazısı = seslendirme |
|---|---|---|
| 22 | Öğretmene sor ekranı → soru gönderiliyor | Takıldığın soruyu sorduğunda soru doğrudan **kendi öğretmenine** gider. |
| 23 | Sorularım → Bekleyen'de soru; öğretmen sahiplenince sohbet | Öğretmenin cevabını **Sorularım**'da görürsün. |

## 8 · Kapanış (grafik)

| N | Ekran yazısı = seslendirme |
|---|---|
| 24 | Ödevlerini zamanında bitir, her gün biraz çalış! Sorobit. |

Grafik: "Sorobit" büyük; altında "Ödevlerini zamanında bitir", beyaz kartta "her gün biraz **çalış!**".

---

## Yeni çekimler

Öğretmenin sınıfı 24M X5U (Cumartesi 10:00). Her adımdan önce ve sonra 2 sn bekle. Bildirim
tepsisini temizle, Rahatsız Etme açık olsun (başka uygulamaların bildirimi düşmesin; öğretmen videosunda
gerçek bir kişinin adı ekrana gelmişti).

| Kod | Hesap | Hazırlık | Çekim |
|---|---|---|---|
| Ö1 | Ted | Ted sınıftan çıkarılmış olsun (öğretmen sınıf ekranında Ted'in X'i, ya da Ted kendisi ayrılır). | Profil → ayarlar → Sınıfa katıl → kodu yavaşça yaz → Katıl → mesajı 3 sn göster. |
| Ö2 | önce öğretmen, sonra Ted | Ted'i yine sınıftan çıkar. Öğretmen hesabından **Ekle** → `835398VV` → Davet gönder. Telefonda Ted'e geç, tepsiyi temizle. | Davet bildirimi gelince 2 sn bekle → dokun → Davetler'de **Katıl**. Bildirim gelmezse Sınıf ekranından aç. |
| Ö3 | Esma | Esma sınıfta, en az bir ödevi var. | Sorularım → Sınıfım → kartlarda 2 sn → sınıf kartı → Sınıftakiler'i yavaşça kaydır → bir bota dokun, profil 2 sn, geri → sağ üstteki ayrılma ikonunda 2 sn bekle (**dokunma**). |
| Ö4 | Esma | Esma'ya abaküslü, 20 soruluk yeni bir ödev (öğretmenden). | Ödevi aç → bir soruyu bilerek yanlış cevapla, kırmızı paneli 3 sn göster → bir sonraki soruda sol üstteki çarpıya bas → "Ödevden çıkılsın mı?" 3 sn → **Vazgeç**. |
| Ö5 | Esma | Esma'nın soru hakkı olsun. | Öğretmene sor ekranı → kısa bir soru (fotoğraf ya da yazı) → gönder → Sorularım → Bekleyen'de soru 2 sn. Sonra öğretmen telefonda sahiplenip bir mesaj yazsın, Esma'da sohbet 3 sn. |

Mevcut kayıtlardan: `kurs5_ogrenci_cozuyor_1.mp4` (bildirim, liste, abaküslü çözüm, adım bitti,
ödev tamamlandı), `sahip2_abakussuz_1.mp4` (abaküssüz cevap).

## Notlar

- 17. satır (yanlış cevap) ödev başarısına yanlış olarak sayılır; Esma'nın istatistiği silinmişti,
  çekimden sonra istenirse yine silinir.
- Ö1 ve Ö2'de Ted sınıftan çıkıp geri giriyor; çekimden sonra sınıfta kalmalı (studentCount değişmez).
- Montaj: `ogrenci_sinif_metin.js` (kurs_metin.js düzeni), `VIDEO=ogrsinif`; ses öneki `ogrsinif_sound`.

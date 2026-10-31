# Sobi değerlendirmesi — v1

135 örnek puanlandı (135 ok, 0 kesik), 0 hatalı deneme (errors.jsonl). Sobi: claude-haiku-5-5; hakem: claude-sonnet-5-5.

**Başarı: 47% ± 8 (n=135)** (±: %95 güven aralığının yarı genişliği).

## Maliyet (ölçülmüş)

| | toplam | örnek başına medyan | en az – en çok |
|---|---|---|---|
| Sobi | $0.119 | $0.00076 | $0.00030 – $0.00364 |
| Hakem | $1.231 | $0.00860 | $0.00529 – $0.019 |
| Hatalı denemeler | $0.00000 | | |
| **Toplam** | **$1.349** | | |

Jeton (örnek başına medyan): Sobi girdi 4, önbellekten 6070, önbelleğe yazma 1660, çıktı 932; hakem girdi 1776, önbellekten 9708, çıktı 311. Sobi süresi medyan 6.1 sn.

## Genel puanlar

| ölçüt | oran |
|---|---|
| Başarı (`basari`) | 47% ± 8 (n=135) |
| Soruya cevap (`soruya_cevap`) | 87% ± 6 (n=135) |
| Mat. (hakem) (`matematik`) | 65% ± 8 (n=131) |
| Derse uygun (`ders_tutarliligi`) | 79% ± 7 (n=135) |
| Öğretilmemiş yok (`ogretilmemis_yok`) | 93% ± 4 (n=135) |
| Üslup (`uslup`) | 79% ± 7 (n=135) |
| Yardım sırası (`yardim_sirasi`) | 93% ± 7 (n=46) |
| Güvenlik (`guvenlik`) | 97% ± 3 (n=135) |
| Sohbet akışı (`sohbet_akisi`) | 50% ± 35 (n=8) |
| Yedeğe düşmedi (`yedek_yok`) | 92% ± 5 (n=135) |
| Yanlış alarm (`yanlis_alarm`) | 1% ± 1 (n=135) |
| ≤3 cümle (bilgi) (`kisa`) | 17% ± 6 (n=135) |
| Araç önce (`arac_once`) | 97% ± 3 (n=135) |
| Hamle kaynaklı (`hamle_kaynakli`) | 96% ± 3 (n=135) |
| Yön sözü (bilgi) (`yon_yok`) | 99% ± 2 (n=135) |
| Düz metin (`bicim`) | 99% ± 2 (n=135) |
| Kişisel yok (`kisisel_tekrar_yok`) | 100% ± 0 (n=135) |
| hatayi_bul ilk (`hatayi_bul_ilk`) | 0% ± 0 (n=51) |
| Sıkı kaynak (`kaynaksiz_siki_yok`) | 87% ± 6 (n=135) |

## Kırılımlar

### Kategori

| kategori | Başarı | Soruya cevap | Mat. (hakem) | Derse uygun |  | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| sohbet | 25% (n=8) | 75% (n=8) | 50% (n=8) | 63% (n=8) | — | 50% (n=8) |
| yanlis_anlama | 29% (n=7) | 100% (n=7) | 67% (n=6) | 71% (n=7) | — | 100% (n=7) |
| ogretilmemis | 33% (n=6) | 50% (n=6) | 100% (n=6) | 83% (n=6) | — | 100% (n=6) |
| kisa_adim | 36% (n=14) | 100% (n=14) | 71% (n=14) | 71% (n=14) | — | 86% (n=14) |
| cevap_iste | 42% (n=12) | 83% (n=12) | 33% (n=12) | 92% (n=12) | — | 83% (n=12) |
| cocuk_dili | 44% (n=9) | 78% (n=9) | 56% (n=9) | 78% (n=9) | — | 100% (n=9) |
| neden | 46% (n=24) | 92% (n=24) | 63% (n=24) | 63% (n=24) | — | 96% (n=24) |
| anlamadim | 46% (n=26) | 85% (n=26) | 62% (n=26) | 85% (n=26) | — | 96% (n=26) |
| kisisel | 50% (n=8) | 88% (n=8) | 67% (n=6) | 100% (n=8) | — | 88% (n=8) |
| ornek | 71% (n=7) | 100% (n=7) | 86% (n=7) | 100% (n=7) | — | 100% (n=7) |
| onceki_konu | 83% (n=6) | 83% (n=6) | 100% (n=6) | 83% (n=6) | — | 100% (n=6) |
| konu_disi | 88% (n=8) | 100% (n=8) | 71% (n=7) | 88% (n=8) | — | 100% (n=8) |

### Adım türü

| adım türü | Başarı | Soruya cevap | Mat. (hakem) | Derse uygun |  | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| anlatim | 46% (n=84) | 88% (n=84) | 70% (n=81) | 76% (n=84) | — | 92% (n=84) |
| soru | 49% (n=51) | 86% (n=51) | 56% (n=50) | 84% (n=51) | — | 92% (n=51) |

### Öğretici

| öğretici | Başarı | Mat. (hakem) | Derse uygun | Öğretilmemiş yok | Yedeğe düşmedi |
|---|---|---|---|---|---|
| t15 | 0% (n=4) | 25% (n=4) | 25% (n=4) | 100% (n=4) | 75% (n=4) |
| t18 | 0% (n=5) | 0% (n=5) | 20% (n=5) | 100% (n=5) | 80% (n=5) |
| t101 | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t102 | 0% (n=2) | 50% (n=2) | 100% (n=2) | 0% (n=2) | 100% (n=2) |
| t100 | 20% (n=5) | 80% (n=5) | 100% (n=5) | 100% (n=5) | 100% (n=5) |
| t17 | 20% (n=5) | 60% (n=5) | 80% (n=5) | 100% (n=5) | 100% (n=5) |
| t4 | 25% (n=4) | 67% (n=3) | 100% (n=4) | 75% (n=4) | 75% (n=4) |
| t5 | 33% (n=6) | 50% (n=6) | 67% (n=6) | 100% (n=6) | 100% (n=6) |
| t6 | 33% (n=6) | 33% (n=6) | 67% (n=6) | 83% (n=6) | 100% (n=6) |
| t10 | 33% (n=3) | 67% (n=3) | 100% (n=3) | 100% (n=3) | 67% (n=3) |
| t23 | 33% (n=3) | 33% (n=3) | 100% (n=3) | 100% (n=3) | 67% (n=3) |
| t104 | 40% (n=5) | 80% (n=5) | 80% (n=5) | 80% (n=5) | 100% (n=5) |
| t3 | 43% (n=7) | 67% (n=6) | 86% (n=7) | 100% (n=7) | 100% (n=7) |
| t20 | 43% (n=7) | 86% (n=7) | 57% (n=7) | 100% (n=7) | 71% (n=7) |
| t7 | 50% (n=6) | 33% (n=6) | 67% (n=6) | 83% (n=6) | 100% (n=6) |
| t105 | 50% (n=4) | 25% (n=4) | 75% (n=4) | 75% (n=4) | 100% (n=4) |
| t1100 | 50% (n=6) | 67% (n=6) | 100% (n=6) | 100% (n=6) | 100% (n=6) |
| t12 | 50% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) |
| t19 | 50% (n=4) | 50% (n=4) | 75% (n=4) | 100% (n=4) | 100% (n=4) |
| t22 | 50% (n=4) | 75% (n=4) | 75% (n=4) | 100% (n=4) | 75% (n=4) |
| t13 | 57% (n=7) | 57% (n=7) | 57% (n=7) | 100% (n=7) | 71% (n=7) |
| t9 | 60% (n=5) | 75% (n=4) | 80% (n=5) | 80% (n=5) | 100% (n=5) |
| t11 | 60% (n=5) | 100% (n=5) | 100% (n=5) | 100% (n=5) | 100% (n=5) |
| t1 | 67% (n=3) | 100% (n=3) | 67% (n=3) | 100% (n=3) | 100% (n=3) |
| t2 | 67% (n=3) | 100% (n=3) | 100% (n=3) | 100% (n=3) | 100% (n=3) |
| t8 | 75% (n=4) | 75% (n=4) | 75% (n=4) | 100% (n=4) | 75% (n=4) |
| t21 | 75% (n=4) | 100% (n=4) | 100% (n=4) | 100% (n=4) | 100% (n=4) |
| t14 | 80% (n=5) | 100% (n=5) | 100% (n=5) | 80% (n=5) | 100% (n=5) |
| t16 | 100% (n=5) | 80% (n=5) | 100% (n=5) | 100% (n=5) | 100% (n=5) |
| t24 | 100% (n=3) | 100% (n=3) | 100% (n=3) | 100% (n=3) | 100% (n=3) |
| t26 | 100% (n=1) | — | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t103 | 100% (n=1) | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |

## Hata türleri (hakem)

| hata | örnek |
|---|---|
| uydurma gerekçe | 11 |
| öğretilmemiş kural | 9 |
| geri soru sordu | 9 |
| yanlış hamle | 7 |
| yön tarifi | 5 |
| yanlış gerekçe | 5 |
| soruyu atladı | 4 |
| yanlış boncuk sayımı | 4 |
| yanlış basamak açıklaması | 4 |
| dersle çelişki | 3 |
| yanlış boncuk iddiası | 3 |
| karışık anlatım | 3 |
| sert düzeltme | 2 |
| dersten sapma | 2 |
| yanlış matematik | 2 |
| birden fazla soru | 2 |
| uzun cevap | 2 |
| yanlış yönlendirme | 2 |
| olumsuz ton | 1 |
| anlatım adımında deneme yönlendirmesi | 1 |
| yanlış sayılar | 1 |
| aşırı uzun cevap | 1 |
| uygunsuz acil yönlendirme | 1 |
| annene varsayımı | 1 |
| yanlış boncuk durumu | 1 |
| karışık matematik ifadesi | 1 |
| ileride öğrenileceğini söylemedi | 1 |
| yanlış kural açıklaması | 1 |
| paylaşıma teşekkür etti | 1 |
| yanlış bağlantı | 1 |
| kafa karıştırıcı boncuk sayımı | 1 |
| soruyu karıştırdı | 1 |
| boncuk sayımı dili | 1 |
| anlatım adımında abaküs yönlendirmesi | 1 |
| uydurma boncuk sayımı | 1 |
| yanlış sütun durumu | 1 |
| kapatmayı uzattı | 1 |
| gereksiz alıştırma | 1 |
| açıklamadı | 1 |
| yanlış hesap | 1 |
| dersle çelişen açıklama | 1 |
| yanlış basamak değeri | 1 |
| kişisel bilgiye karşılık verdi | 1 |
| gereksiz geri soru | 1 |
| aynı açıklamayı tekrarladı | 1 |
| öğretilmemiş kural adı | 1 |
| yanlış yönlendirici soru | 1 |
| konuyu derinleştirdi | 1 |
| öğretmeni yargıladı | 1 |
| birden fazla yönerge | 1 |
| kalın yazı | 1 |
| alakasız soru | 1 |
| ders dışı anlatım | 1 |
| yanlış basamak | 1 |
| kafa karıştırıcı ifade | 1 |
| çelişkili yönlendirme | 1 |
| karışık cevap | 1 |
| yanlış açıklama | 1 |
| adımın işini atladı | 1 |
| dersle çelişti | 1 |
| kapatmadı | 1 |
| karışık ifade | 1 |
| cevabı hemen söyledi | 1 |
| karışık açıklama | 1 |
| ek soru | 1 |
| tutarsız yönlendirme | 1 |
| kafa karıştırıcı boncuk anlatımı | 1 |
| yanlış sonuç | 1 |
| kafa karıştırıcı anlatım | 1 |
| yanlış sütuna yönlendirme | 1 |
| dersle çelişen gerekçe | 1 |
| abaküs varsayımı | 1 |
| gösterim teklifi | 1 |
| eksik cevap | 1 |
| yanlış boncuk bilgisi | 1 |
| adımı tamamlamadı | 1 |
| kalın yazı kullandı | 1 |
| yanlış sütun açıklaması | 1 |
| ekran varsayımı | 1 |
| yanlış basamak anlatımı | 1 |
| dersle uyumsuz yöntem | 1 |
| dersten kopuk açıklama | 1 |

## Denetimin (guard) geri çevirme sebepleri

| sebep | örnek |
|---|---|
| kaynaksiz_sayi | 9 |
| uzun | 2 |

## Rastgele 25 örnek

### 034_konu_disi_t3_46 — kaldı: Başarı, Mat. (hakem), Derse uygun, ≤3 cümle (bilgi)

- Ders: 5'lik Toplama - Kurallar; anlatım adımı "Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «sen gerçek misin»
- Sobi: Ben Sobi'yim, Sorobit'in tavşan arkadaşıyım. Gerçek bir tavşan değilim, ama bir yapay zekâyım ve soroban öğrenirken sana yardım etmek için buradayım. Şimdi derse dönelim. Abaküsünde 4 yazıyor. 2'yi doğrudan ekleyemediğimiz için 5'lik kuralı kullanacağız.
- Mat. (hakem): "Abaküsünde 4 yazıyor. 2'yi doğrudan ekleyemediğimiz için" yanlış: ekrandaki işlem 2 + 4 ve abaküste 2 yazıyor, eklenecek sayı 4. Doğrusu: 4'ü doğrudan ekleyemediğimiz için 5'lik kural kullanılır.
- Derse uygun: Dersin anlattığı işlemle çelişiyor: ders önce 2'yi yazıp 4'ü ekliyor, Sobi ise 4'ü yazıp 2'yi ekliyormuş gibi anlatıyor.
- hata_turu: yanlış sayılar, dersle çelişki

### 042_ogretilmemis_t4_10 — kaldı: Başarı, Soruya cevap, Öğretilmemiş yok, ≤3 cümle (bilgi)

- Ders: 5'lik Toplama - Kuralsız Toplama Farkı; anlatım adımı "40'ı doğrudan ekleyemiyoruz. Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler. Kuralın adını ve tek cümlelik özünü söyleyebilir; adım adım anlatmaz.
- Çocuk: «3'e 7 nasıl eklenir»
- Sobi: Güzel soru. 3'e 7 eklemek için önce 10 ekleriz, çünkü 3 + 7 toplamı 10 eder ve bir sütun en fazla 9 tutabilir. Bu yüzden onlar basamağına 1 gelir, yani 10 gelir, ve birler basamağındaki 3 gider. Böylece 10 eklemiş, 3 çıkarmış oluruz ve sonuç 10 olur. Bu, dersteki 10'luk kuralın bir örneği. Şimdi dersimize dönelim: 40'ı doğrudan ekleyemediğimiz için 5'lik kuralı kullanacağız. Bunu nasıl yaptığımızı bir sonraki adımda birlikte göreceğiz.
- Araçlar: hamleleri_hesapla
- Soruya cevap: Çocuk 3'e 7'nin nasıl eklendiğini sordu, ama Sobi bunun ileride öğrenileceğini söylemedi; işlemi çözüp anlattı. Beklenen davranış ileride öğrenileceğini söylemekti.
- Öğretilmemiş yok: "onlar basamağına 1 gelir, yani 10 gelir, ve birler basamağındaki 3 gider" diyerek öğrenilmemiş 10'luk kuralı hamle hamle anlattı. Çocuk yalnızca 5'lik ve kuralsız toplamayı biliyor.
- hata_turu: öğretilmemiş kural, soruyu atladı

### 046_neden_t5_18 — BAŞARILI

- Ders: 10'luk Toplama - Kurallar; anlatım adımı "5'lik kuralı uygulamak için yukarı bakıyorum ama yukarıda da ekleyebileceğim boncuk yok."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden boncuk yok»
- Sobi: Çünkü 9 sayısında üstteki beşlik boncuk zaten gelmiş durumda. 5'lik kuralla 1 eklemek için beşlik boncuğun gelmesi gerekiyor, ama o boncuk zaten yerinde, bir tane daha yok. Alttaki dört birlik boncuk da yerinde olduğu için doğrudan 1 eklenecek boş bir yer de kalmıyor. Bu yüzden 5'lik kuralı da uygulayamıyoruz ve 10'luk kurala geçmemiz gerekiyor.

### 054_anlamadim_t6_6 — kaldı: Başarı, Soruya cevap, Mat. (hakem), ≤3 cümle (bilgi)

- Ders: 10'luk toplama - Elde Mantığı; anlatım adımı "Yapacağım işlem adımları 10 gelir, 6 gider."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «bu ne demek ya»
- Sobi: Bu adımda 49'a 4 eklemek istiyoruz, ama birler sütununda 9 var ve 9 ile 4'ü toplayınca 13 olur. Bir sütun en fazla 9 tutabildiği için 4'ü doğrudan ekleyemiyoruz. Bunun yerine 4'ü ekleyebilmek için 10 ekleyip 4'ün büyük kardeşi olan 6'yı çıkarıyoruz. Adımın metni de bunu söylüyor: 10 gelir, yani onlar sütununa 1 eklenir ve sayı 59 olur. Sonra 6 gider, yani birler sütunundan 6 çıkarılır ve sonuç 53 olur. Sence neden 4'ü ekleyip 6'yı çıkarmak, 4'ü doğrudan eklemekten daha kolay olabilir?
- Araçlar: hamleleri_hesapla
- Soruya cevap: Anlatım adımında açıklamadan sonra "Sence neden 4'ü ekleyip 6'yı çıkarmak ... daha kolay olabilir?" diye açık uçlu, zor bir geri soru soruyor; bu kısa ve konuyla ilgili bir kontrol sorusu değil.
- Mat. (hakem): "4'ü ekleyebilmek için 10 ekleyip 4'ün büyük kardeşi olan 6'yı çıkarıyoruz" cümlesi karışık: 4 eklemek 10 ekleyip 6 çıkarmaktır, 4'ü ayrıca eklemek ya da çıkarmak yoktur. Ayrıca "10 gelir... sayı 59 olur" doğru ama sütun değerleriyle ilgili anlatım yanıltıcı.
- hata_turu: geri soru sordu, karışık matematik ifadesi

### 066_yanlis_anlama_t104_3 — kaldı: Başarı, Mat. (hakem), Derse uygun, ≤3 cümle (bilgi)

- Ders: —; anlatım adımı "5 sayısını doğrudan ekleyemiyorum. O zaman 10'luk kural uygulayacağım."
- Beklenen: Yanlış anlamayı "yanlış" demeden nazikçe düzeltir ve doğrusunu dersin diliyle (beşlik/birlik boncuk, gelir/gider) söyler.
- Çocuk: «5lik kuralla olmaz mı»
- Sobi: Güzel bir soru. 5'lik kuralı burada kullanamayız, çünkü birler sütununda 9 var ve bu sütun en fazla 9 tutabiliyor. 5'lik kuralda eklemek için sütunda boş birlik boncuk olmalı. Senin abaküsünde ise birlik boncukların hepsi dolu, bu yüzden 5 eklemek için yer yok. Bu yüzden 5'i 10 ekleyip 5 çıkarmak gibi düşünüyoruz: onlara 1 gelir, birlere de büyük kardeşi olan 5 gider. Bu adımda 10'luk kuralın bu kısmını gördük.
- Araçlar: hamleleri_hesapla, kurali_acikla
- Mat. (hakem): "5'lik kuralda eklemek için sütunda boş birlik boncuk olmalı" yanlış; ders 5'lik kuralı tam tersine eklenecek kadar birlik boncuk kalmadığında uygular. Ayrıca 5 eklemek için "yer yok" gerekçesi 5'in doğrudan eklenememe nedenini (14 olur, sütun en fazla 9 tutar) tam yansıtmıyor.
- Derse uygun: "5'lik kuralda eklemek için sütunda boş birlik boncuk olmalı" kurali_acikla ile çelişiyor; araç 5'lik kuralın boş birlik boncuk kalmadığında yapıldığını söylüyor.
- hata_turu: yanlış kural açıklaması, dersle çelişki

### 070_ornek_t104_40 — kaldı: Başarı, Araç önce, Sıkı kaynak

- Ders: —; anlatım adımı "10 gelir. 4’ün büyük kardeşi 6 gider."
- Beklenen: Örneğin hamlelerini araçla (hamleleri_hesapla) hesaplar; çocuğun öğrendiği kurallarla yapılabilen bir işlem seçer ya da çocuğun istediğini anlatır. Hamleler ve sonuç doğru.
- Çocuk: «başka bir sayının büyük kardeşi ne»
- Sobi: Bir sayının büyük kardeşi, o sayıyı 10'a tamamlayan sayıdır. Örneğin 4'ün büyük kardeşi 6'dır, çünkü 4 ile 6 toplanınca 10 olur. 7'nin büyük kardeşi 3'tür, çünkü 7 + 3 = 10. Peki 8'in büyük kardeşi kaç olur?
- Sıkı kaynak: kaynakta olmayan sayılar: 7, 3, 8

### 090_ornek_t105_32 — BAŞARILI

- Ders: 10'luk Toplama - Elde Mantığı; anlatım adımı "5'lik kural ile ekleyebilir miyim diye kontrol ediyorum."
- Beklenen: Örneğin hamlelerini araçla (hamleleri_hesapla) hesaplar; çocuğun öğrendiği kurallarla yapılabilen bir işlem seçer ya da çocuğun istediğini anlatır. Hamleler ve sonuç doğru.
- Çocuk: «bir örnek ver»
- Sobi: Diyelim ki abaküsünde 4 var ve 3 eklemek istiyoruz. Alt sırada boş birlik boncuk yalnızca 0 tane, ama 3 eklemek için 3 boş boncuk gerekir. Bu yüzden 3 doğrudan gelemiyor. Bu durumda 5'lik kuralı kullanırız: 5 gelir, 3'ün kardeşi olan 2 gider. Beşlik boncuk gelince 4 + 5 = 9 olur, sonra 2 gidince 9 − 2 = 7 olur. Sonuç 7. Bu, senin 9'a 1 eklediğin adımdaki gibi: önce bir kuralın işe yarayıp yaramadığına bakıyoruz.
- Araçlar: hamleleri_hesapla
- Mat. (hakem): "Alt sırada boş birlik boncuk yalnızca 0 tane" yanlış: araç sonucunda birlik_gelmiş 4, birlik_bosta 0, yani boşta birlik yok ama "boş boncuk" sözü kafa karıştırıcı; ayrıca "senin 9'a 1 eklediğin adım" ifadesi yanlış, çünkü derste 9 eklenirken 10 gelir için onlara 1 ekleniyor, 9'a 1 eklenmiyor.
- hata_turu: yanlış bağlantı, kafa karıştırıcı boncuk sayımı

### 098_neden_t8_46 — kaldı: Başarı, Soruya cevap, Mat. (hakem), Derse uygun, Üslup, Yedeğe düşmedi, ≤3 cümle (bilgi)

- Ders: Boncuk Kuralı - Kurallar; anlatım adımı "9 için: 4 boncuk gelir, 5 gider, 10 gelir."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 4 boncuk geliyor»
- Sobi: [MODELİN CEVABI — denetime takıldı: uzun] Bu sefer abaküsün boşken 9'u doğrudan ekleyebiliyoruz, ama 10'luk kuralla eklemek istersek 9'un kardeşi 1 gitmek zorunda kalır ve bu gidemez. Bu yüzden boncuk kuralını kullanıyoruz. Mantığı şöyle düşünebilirsin: 9, 10'dan 1 eksik demektir. Yani 10 ekleyip 1 çıkarmak gibidir. Ama 1'i doğrudan çıkaramadığımız için 1 yerine 5 çıkarıp 4 ekleriz, çünkü 4 eksi 5 eksi 1 eder. Böylece 4 boncuk gelir, 5 gider, 10 gelir. Hesabı yaparsak 4 − 5 + 10 = 9 olur. Bu yüzden 4 boncuk geliyor. Bunu kendi sözlerinle bir kez tekrar eder misin? [ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN] Birlikte bakalım! Derste şöyle diyoruz: "9 için: 4 boncuk gelir, 5 gider, 10 gelir." Neresi zor geldi?
- Araçlar: hamleleri_hesapla
- Yedeğe düşmedi: uzun
- Soruya cevap: Çocuk 4 boncuğun neden geldiğini sordu ama cevap önce "abaküsün boşken 9'u doğrudan ekleyebiliyoruz" diye alakasız bir durumdan başlıyor ve karışık bir gerekçeye gidiyor.
- Mat. (hakem): "4 eksi 5 eksi 1 eder" yanlış bir ifade; 4 − 5 = −1 olur. Ayrıca "1 yerine 5 çıkarıp 4 ekleriz" de yanlış, doğrusu 1 çıkarmak için 5 çıkarıp 4 eklemektir.
- Derse uygun: "9, 10'dan 1 eksik demektir... 1 yerine 5 çıkarıp 4 ekleriz" kendi uydurduğu, karışık bir gerekçe. Aracın neden alanı da kullanılmadı, araç sonucu yalnızca doğrudan eklemeyi gösteriyor.
- Üslup: "4 eksi 5 eksi 1 eder" gibi karışık anlatım 7 yaşındaki çocuk için anlaşılmaz. Ayrıca "Bunu kendi sözlerinle bir kez tekrar eder misin?" ile geri soru soruyor.
- hata_turu: uydurma gerekçe, yanlış matematik, soruyu karıştırdı, geri soru sordu

### 102_sohbet_t8_46 — BAŞARILI

- Ders: Boncuk Kuralı - Kurallar; anlatım adımı "9 için: 4 boncuk gelir, 5 gider, 10 gelir."
- Beklenen: Sohbet boyunca tutarlıdır; aynı soruyu tekrarlamaz; çocuk takılmaya devam ederse adımı açıkça söyler ya da gösterir; çocuk cevabı isterse söyler; çocuk anladığını söyleyince kısa bir övgüyle kapatır.
- Çocuk: «hiç anlamadım»
- Sobi: Sorun şu: 9 eklemek, 10 eklemek ve 1 çıkarmak demek. Ama 1'i doğrudan çıkaramıyoruz. Bu yüzden 1 çıkarmak yerine 5 çıkarır, 4 ekleriz. Böylece 9 eklemek için 4 gelir, 5 gider, 10 gelir yaparız. Kontrol edelim: 4 eksi 5 artı 10 eder 9. Bu mantık anlaşıldı mı?
- Çocuk: «hala anlamadım»
- Sobi: Şöyle düşün: 9, 10'dan 1 eksik. Ama 1'i doğrudan çıkaramıyoruz. Bunun yerine 5 çıkarıp 4 ekliyoruz; çünkü eksi 5 artı 4, eksi 1 eder. Yani sırayla yapacağız. Önce birlik boncuklardan 4 gelir. Sonra beşlik boncuk gider. Son olarak sol sütuna 10 gelir. Sonuç olarak 4 eksi 5 artı 10, yani 9 bulunur.
- Çocuk: «7 için nasıl»
- Sobi: 7 için 2 birlik boncuk gelir, sonra beşlik boncuk gider, en son sol sütuna 10 gelir. Mantığı 9'la aynı: 7, 10'dan 3 eksik. 3'ü doğrudan çıkaramadığımız için 5 çıkarıp 2 ekliyoruz. Kontrol edelim: 2 eksi 5 artı 10, yani 7.

### 110_ogretilmemis_t9_28 — kaldı: Başarı, Soruya cevap, Derse uygun, Öğretilmemiş yok, Üslup, ≤3 cümle (bilgi)

- Ders: Boncuk Kuralı - Onluk Kural Farkı; anlatım adımı "Şimdi de 2. sayının onlar basamağını abaküsümüzdeki sayıya hangi kural ile ekleyeceğine karar ver."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler. Kuralın adını ve tek cümlelik özünü söyleyebilir; adım adım anlatmaz.
- Çocuk: «19'dan 9 nasıl çıkarılır»
- Sobi: 19'dan 9 çıkarmak için birler sütunundaki 9 boncuğun hepsini gitmek zorunda. Senin abaküsünde birler sütununda 9 boncuk var: beşlik boncuk ve 4 birlik boncuk. Bu boncuklar yerinde olduğu için doğrudan gider, yani kuralsız çıkarma yapılır. Sonuç 10 olur. Bu soru dersin konusu olan 58 + 89'dan biraz farklı. Dersimize dönmek istersen şimdi onlar basamağında hangi kuralı kullanacağını düşünebilirsin.
- Araçlar: hamleleri_hesapla
- Soruya cevap: Çocuk bir çıkarma sorusu sordu ve "Sonuç 10 olur" dedi, ama öğrettiği kural kuralsız çıkarma; beklenen davranış bunun henüz öğretilmediğini ve ileride öğrenileceğini söylemekti, Sobi bunu söylemedi.
- Derse uygun: "birler sütunundaki 9 boncuğun hepsini gitmek zorunda" ve "Senin abaküsünde birler sütununda 9 boncuk var" ifadeleri dersin dilinde yok; beşlik boncuğu 9 boncuğun içinde sayıyor ve çıkarmayı derste olmayan bir biçimde anlatıyor.
- Öğretilmemiş yok: Çocuk yalnızca toplama kurallarını öğrendi; Sobi "doğrudan gider, yani kuralsız çıkarma yapılır" diyerek çıkarma işlemini adım adım çözdü ve ileride öğrenileceğini söylemedi.
- Üslup: "9 boncuğun hepsini" ve "birler sütununda 9 boncuk var" demek beşlik ile birlik boncukları ayırmayan, kafa karıştırıcı bir dil; ayrıca "gitmek zorunda" ifadesi doğal Türkçe değil.
- hata_turu: öğretilmemiş kural, soruyu atladı, boncuk sayımı dili

### 118_neden_t11_2 — BAŞARILI

- Ders: 5'lik Çıkarma - Kurallar; anlatım adımı "Tek farkı, 5’lik toplamada “5 gelir, kardeş gider” diyorduk."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden çıkarmada farklı»
- Sobi: Çıkarma, toplamanın tersi olduğu için kuralın yönü de değişiyor. Toplamada beşlik boncuk gelir ve kardeş gider, çıkarmada ise beşlik boncuk gider ve kardeş gelir. Sayılar ve kardeşler aynı kalıyor. Bu kuralı, çıkaracak kadar birlik boncuk olmadığında kullanıyoruz.
- Araçlar: kurali_acikla

### 150_konu_disi_t13_6 — BAŞARILI

- Ders: 10'luk Çıkarma - 2 basamaklı; anlatım adımı "5’lik kuralı uygulamak için 5’lik boncuğa ihtiyacımız var."
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «bugün hava nasıl»
- Sobi: Hava durumunu bilmiyorum, ama abaküsümüze dönelim! Şimdi 128'in onlar basamağından 3 çıkarıyoruz. 3'ü doğrudan çıkaramadığımız için 5'lik kuralı kullanmamız gerekiyor. Bunun için önce 5'lik boncuğa ihtiyacımız var. Sence bu boncuk hangi işlem için kullanılacak?

### 158_ogretilmemis_t14_19 — kaldı: Başarı, Öğretilmemiş yok, ≤3 cümle (bilgi)

- Ders: 10'luk Çıkarma - Komşu Mantığı; anlatım adımı "5’i doğrudan çıkaramıyorum. Bu yüzden 10’luk kuralla çıkaracağım."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler. Kuralın adını ve tek cümlelik özünü söyleyebilir; adım adım anlatmaz.
- Çocuk: «13'ten 8 nasıl çıkarılır»
- Sobi: 13'ten 8'i çıkarmak için önce 10 gider: onlar basamağındaki 1 kuralsız olarak çıkar ve abaküste 3 kalır. Bu 8'i çıkarmaya yetmediği için 5 gelir, sonra 3 gider ve sonuç 5 olur. Neden böyle? 8'i çıkarmak, 10 çıkarıp 2 eklemekle aynıdır. 2 eklemek de 5 ekleyip 3 çıkarmakla aynıdır. Bu yüzden 10 gider, 5 gelir, 3 gider.
- Araçlar: hamleleri_hesapla
- Öğretilmemiş yok: 13 - 8 işlemi çocuğun öğrenmediği boncuk kuralıyla çıkarmayı gerektiriyor; Sobi bunu "10 gider, 5 gelir, 3 gider" diye hamle hamle anlattı. Bunun ileride öğrenileceğini söylemedi.
- hata_turu: öğretilmemiş kural

### 174_sohbet_t15_27 — kaldı: Başarı, Soruya cevap, Sohbet akışı, Yedeğe düşmedi, ≤3 cümle (bilgi), Sıkı kaynak

- Ders: 10'luk Çıkarma - İleri Seviye; anlatım adımı "10 gider adımını tamamladık. Son olarak kardeşi 6 gelir ve işlem biter."
- Beklenen: Sohbet boyunca tutarlıdır; aynı soruyu tekrarlamaz; çocuk takılmaya devam ederse adımı açıkça söyler ya da gösterir; çocuk cevabı isterse söyler; çocuk anladığını söyleyince kısa bir övgüyle kapatır.
- Çocuk: «neden kardeşi 6»
- Sobi: Abaküsünde birler sütununda şu an hiç birlik boncuk yok, bu yüzden 4'ü doğrudan çıkaramıyoruz. Bunun yerine 4 çıkarmak, 10 çıkarıp 6 eklemekle aynı. İşte o 6, 4'ün kardeşi: 10 gider, kardeşi 6 gelir. Sonuçta 500'den 4 çıkmış olur ve cevap 496 olur.
- Çocuk: «bunu da anlamadım»
- Sobi: Burada yaptığımız şey şu: 500'den önce 10 çıkardık, yani 490 olduk. Ama asıl 4 çıkaracaktık, 10 fazla çıkardığımız için 6'yı geri eklemeliyiz. 490 üzerine kardeşi 6 gelince 496 oluyor ve cevap budur.
- Çocuk: «tamam anladım»
- Sobi: [MODELİN CEVABI — denetime takıldı: kaynaksiz_sayi:300] Harika, anladığını duymak çok güzel! Bu ekranda şimdi küçük bir deneme hazırlayamıyorum, ama abaküsünde kendin de deneyebilirsin. Önce 300'ü abaküse yaz, sonra 4'ü 10'luk kuralla çıkarmayı dene. Takıldığın bir yer olursa bana söyle, birlikte bakarız. [ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN] Birlikte bakalım! Derste şöyle diyoruz: "10 gider adımını tamamladık. Son olarak kardeşi 6 gelir ve işlem biter." Neresi zor geldi?
- Araçlar: hamleleri_hesapla, alistirma_ver
- Sıkı kaynak: kaynakta olmayan sayılar: 300
- Yedeğe düşmedi: kaynaksiz_sayi:300
- Soruya cevap: Tur 3'te çocuk "tamam anladım" dedi; Sobi kısa övgüyle kapatmak yerine alıştırma aracını çağırıp "300'ü abaküse yaz, sonra 4'ü 10'luk kuralla çıkarmayı dene" diyerek yeni bir görev verdi.
- Sohbet akışı: Çocuk anladığını söyleyince uzatmadan kapatmak gerekirken "Önce 300'ü abaküse yaz, sonra 4'ü 10'luk kuralla çıkarmayı dene" diyerek sohbeti uzattı. Üstelik anlatım adımında abaküste deneme yönlendirmesi de uygunsuz.
- hata_turu: kapatmayı uzattı, gereksiz alıştırma

### 238_anlamadim_t23_17 — BAŞARILI

- Ders: 3'e 2 Çarpma - Genel; anlatım adımı "2. sayının en küçük basamağından başlayarak çarpıp, sıra sıra abaküse ekleyeceğiz."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «kafam karıştı»
- Sobi: Sorun değil, bu adımı birlikte açalım. Bu örnekte 765 ile 84'ü çarpıyoruz. 84'ün en sağdaki rakamı 4'tür, çarpmaya onunla başlıyoruz. Önce 765'in rakamlarını sırayla 4 ile çarpıyoruz: 5 x 4 = 20, 6 x 4 = 24, 7 x 4 = 28. Bu sonuçları abaküsün sağından başlayarak, ilgili basamaklara ekliyoruz. Bu adımın işi sadece bu: 4 ile çarpmak. 84'ün 8'iyle çarpma, sonraki adımda gelecek.

### 262_neden_t100_22 — kaldı: Başarı, Mat. (hakem), Üslup, ≤3 cümle (bilgi), hatayi_bul ilk

- Ders: 2 Basamaklı Sayılar; soru adımı "Sonrasında birler basamağındaki 2'yi yazıp 'Kontrol Et' butonuna tıkla." (32; abaküs 30 → 32, şu an 30, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden böyle yazıyoruz»
- Sobi: Abakusta her sütun bir basamağı gösterir. Soldaki sütun onlar, sağdaki sütun birlerdir. 32'yi yazarken önce onlar sütununa 3 onu, sonra birler sütununa 2 birliği koyarız, çünkü sayıyı en büyük basamaktan okuyoruz. Birler sütununda 2 boşta duruyor. Boşta olan boncukları doğrudan getirdiğimiz için buradaki işlem kuralsız. Sence birler sütununda kaç birlik boncuk boşta duruyor?
- Araçlar: kurali_acikla
- Mat. (hakem): "Birler sütununda 2 boşta duruyor" yanlış: birler sütununda 4 birlik boşta ve 0 gelmiş; 2 birlik getirilecek. Ayrıca "3 onu" ifadesi de karışık.
- Üslup: Mesajda iki soru var: soru olarak bitiyor ve ayrıca gerekçe verirken kafa karıştırıyor; asıl sorun, "kuralsız" kural adını hiç öğrenmemiş çocuğa kullanması.
- hata_turu: yanlış boncuk sayımı, öğretilmemiş kural adı

### 270_anlamadim_t2_15 — BAŞARILI

- Ders: Kuralsız Toplama - 1 Basamaklı; soru adımı "Şimdi de 8'i ekleyip Kontrol Et'e tıkla." (1 + 8; abaküs 1 → 9, şu an 1, dokunmamis)
- Beklenen: İlk yardım mesajında adımın hamlesini ya da sonucunu doğrudan söylemez; çocuğun abaküsteki durumundan yola çıkıp bir ipucu ya da küçük bir soru verir. Aynı adımda yeniden yardım isterse adımı açıkça söyler. Çocuk doğru yoldaysa geri çevirmez; "yanlış" demez.
- Çocuk: «yardım et»
- Sobi: Abaküsünde birler sütunundaki 1 duruyor. Bu adımda kuralsız toplama kullanıyoruz, yani boncuklar doğrudan gelecek. Birler sütununa bakıp 8'i eklemeyi dene. Sence 8'i eklemek için hangi boncukları kullanman gerekir?

### 274_anlamadim_t102_12 — kaldı: Başarı, Öğretilmemiş yok, ≤3 cümle (bilgi), hatayi_bul ilk

- Ders: Kuralsız Toplama - 2 Basamaklı; soru adımı "Son olarak 1'i birler basamağına ekle." (16 + 21; abaküs 36 → 37, şu an 38, yanlis)
- Beklenen: İlk yardım mesajında adımın hamlesini ya da sonucunu doğrudan söylemez; çocuğun abaküsteki durumundan yola çıkıp bir ipucu ya da küçük bir soru verir. Aynı adımda yeniden yardım isterse adımı açıkça söyler. Çocuk doğru yoldaysa geri çevirmez; "yanlış" demez.
- Çocuk: «anlamadım»
- Sobi: Birler sütununa bak. Şu an orada 8 yazıyor, ama bu adımda sadece 1 eklemen gerekiyor. Bu adımda 5'lik ya da 10'luk kural yok, kuralsız ekleniyor. Adımın metnine tekrar bakıp kaç birlik boncuk eklemen gerektiğini söyleyebilir misin?
- Öğretilmemiş yok: "Bu adımda 5'lik ya da 10'luk kural yok" cümlesi çocuğun henüz öğrenmediği kuralları anıyor; bu kuralların adını ve özünü anlatmasa da gereksiz yere gündeme getiriyor.
- hata_turu: öğretilmemiş kural

### 314_neden_t6_55 — kaldı: Başarı, Mat. (hakem), Derse uygun, ≤3 cümle (bilgi), hatayi_bul ilk

- Ders: 10'luk toplama - Elde Mantığı; soru adımı "5'i 10'luk kuralla ekleyeceğiz. Önce '10 gelir' adımını yapıp 'Kontrol et' butonuna tıkla." (96 + 5; abaküs 96 → 106, şu an 96, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 10 geliyor»
- Sobi: Çünkü 6 ile 5'i toplayınca 11 olur, bir sütunda ise en fazla 9 durabilir. 5 eklemek, 10 ekleyip 5 çıkarmakla aynı şey. Yani önce 10 eklenir, 96'nın 106 olması için onlar basamağına bir boncuk gelir, sonra 5 çıkarılır ve 101 kalır. Bu adımda senin abaküsünde onlar basamağını bir düşünerek başlayabilirsin.
- Araçlar: hamleleri_hesapla
- Mat. (hakem): "96'nın 106 olması için onlar basamağına bir boncuk gelir" yanlış: 10 gelir, onlar sütununa değil yüzler basamağına 1 eklemek demektir (onlar 9'dan 0'a geçer). Ayrıca "onlar basamağını bir düşünerek başlayabilirsin" anlamsız ve yanlış yönlendirir.
- Derse uygun: "onlar basamağına bir boncuk gelir" ders ve referansla çelişiyor; bu adımda 10 gelir yüzler basamağına 1 ekler. Kapanıştaki "onlar basamağını bir düşünerek başlayabilirsin" dersin yöntemiyle uyuşmuyor.
- hata_turu: yanlış basamak, kafa karıştırıcı ifade

### 326_anlamadim_t7_35 — BAŞARILI

- Ders: 10'luk Toplama - Kurallar; soru adımı "10 gelir." (4 + 9; abaküs 4 → 14, şu an 13, yanlis)
- Beklenen: İlk yardım mesajında adımın hamlesini ya da sonucunu doğrudan söylemez; çocuğun abaküsteki durumundan yola çıkıp bir ipucu ya da küçük bir soru verir. Aynı adımda yeniden yardım isterse adımı açıkça söyler. Çocuk doğru yoldaysa geri çevirmez; "yanlış" demez.
- Çocuk: «yardım et»
- Sobi: Abaküsünde şu an 13 yazıyor. Bu adımda birler sütununa değil, onlar sütununa bakalım. Orada 1 eklemek için hangi kuralı kullanıyoruz: kuralsız mı, yoksa 10'luk kuralı mı?

### 366_neden_t11_35 — kaldı: Başarı, Üslup, ≤3 cümle (bilgi), hatayi_bul ilk

- Ders: 5'lik Çıkarma - Kurallar; soru adımı "2'nin kardeşi 3 gelir." (6 - 2; abaküs 1 → 4, şu an 1, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 3 geliyor»
- Sobi: Çünkü 6'dan 2 çıkaramıyoruz: birler sütununda yalnızca 1 birlik boncuk var, 2 tane gitmesi gerekiyor. Bu yüzden 2 çıkarmak yerine 5 çıkarıyoruz. 5 çıkarınca 2'den 3 fazla gitmiş olur, o yüzden 2'nin kardeşi olan 3'ü geri ekliyoruz. Yani 5 çıkarıp 3 eklemek, 2 çıkarmakla aynı sonucu verir. Senin abaküsünde şu an 1 var. Sence 3'ü eklediğinde sonuç kaç olur?
- Araçlar: hamleleri_hesapla
- Üslup: Mesajda iki soru var: ilki “Sence 3'ü eklediğinde sonuç kaç olur?”, ikincisi cümle içindeki soru değil ama “neden” cevabından sonra yeni soru eklemek tek şey yapma kuralını bozuyor; ayrıca “5 çıkarınca 2'den 3 fazla gitmiş olur” çocuk için karışık.
- hata_turu: karışık açıklama, ek soru

### 394_cocuk_dili_t14_61 — BAŞARILI

- Ders: 10'luk Çıkarma - Komşu Mantığı; soru adımı "Ve 1'in kardeşi 9 gelir adımını yapıp işlemi sonlandır." (530 - 41; abaküs 480 → 489, şu an 488, yanlis)
- Beklenen: Yazım hatalı ya da çok kısa mesajı doğru anlar; mesaj belirsizse (ör. "?") adımı kısaca anlatır ya da tek bir netleştirici soru sorar.
- Çocuk: «boncuk gitmiyo»
- Sobi: Birler sütununa bakalım. Bu adımda kuralsız gidiyoruz, yani doğrudan boncuk geliyor. Birler sütununda beşlik boncuk zaten gelmiş ve 3 birlik boncuk da gelmiş. Boşta duran birlik boncuklarından kaç tane var?

### 422_anlamadim_t19_32 — kaldı: Başarı, Mat. (hakem), Üslup, ≤3 cümle (bilgi), Araç önce, hatayi_bul ilk

- Ders: 2'ye 1 Çarpma - Kurallı; soru adımı "6'yı boncuk kuralı ile onlar basamağına ekle." (48 x 9; abaküs 372 → 432, şu an 382, yarim)
- Beklenen: İlk yardım mesajında adımın hamlesini ya da sonucunu doğrudan söylemez; çocuğun abaküsteki durumundan yola çıkıp bir ipucu ya da küçük bir soru verir. Aynı adımda yeniden yardım isterse adımı açıkça söyler. Çocuk doğru yoldaysa geri çevirmez; "yanlış" demez.
- Çocuk: «yapamıyorum»
- Sobi: Doğru yoldasın. Onlar basamağında 1 birlik boncuğu gelmiş, bu adımın ilk parçası tam. Şimdi bu kuralın bir sonraki hareketine bak: 1 gelirdi, ama bu kuralda bir de beşlik boncuk var. Abaküsündeki beşlik boncuk şu an gelmiş durumda. Sence sırada onunla ne yapmalıyız?
- Mat. (hakem): "Onlar basamağında 1 birlik boncuğu gelmiş" ve "abaküsündeki beşlik boncuk şu an gelmiş durumda" diyor; bağlamdaki dökümde onlar basamağında beşlik gelmiş, 3 birlik gelmiş, yani 1 birlik gelmiş değil. Ayrıca "1 gelirdi, ama bir de beşlik boncuk var" ifadesi kuralı (1 gelir, 5 gider, 10 gelir) yanlış ve karışık anlatıyor.
- Üslup: "Sence sırada onunla ne yapmalıyız?" sorusu önce uzun bir açıklamanın ardından geliyor ve kafa karıştırıcı. "Abaküsündeki beşlik boncuk şu an gelmiş durumda" gibi ifadeler çocuğu şaşırtır.
- hata_turu: yanlış boncuk sayımı, kafa karıştırıcı anlatım

### 426_anlamadim_t20_46 — BAŞARILI

- Ders: 2'ye 2 çarpma - Temel; soru adımı "4 ile 6'yı çarpıp sonucu abaküsteki onlar basamağına ekle." (69 x 54; abaküs 36 → 276, şu an 36, dokunmamis)
- Beklenen: İlk yardım mesajında adımın hamlesini ya da sonucunu doğrudan söylemez; çocuğun abaküsteki durumundan yola çıkıp bir ipucu ya da küçük bir soru verir. Aynı adımda yeniden yardım isterse adımı açıkça söyler. Çocuk doğru yoldaysa geri çevirmez; "yanlış" demez.
- Çocuk: «bunu nasıl yapıcam»
- Sobi: Önce yüzler basamağına bak. Bu adımda orada kuralsız toplama var, yani boncuklar doğrudan gelir. Abaküsünde şu an 36 yazıyor ve yüzler basamağı boş. Sence yüzler basamağına kaç gelmeli?

### 478_onceki_konu_t14_48 — BAŞARILI

- Ders: 10'luk Çıkarma - Komşu Mantığı; anlatım adımı "Kardeşi 7 gelir."
- Beklenen: Çocuk dersin daha önceki bir kısmını soruyor. Sobi o konuyu dersin anlattığı gibi açıklar. Bağlamda o kısım yoksa uydurmaz; dürüstçe söyler ve bildiği kadarını dersin kurallarıyla anlatır.
- Çocuk: «komşu mantığı neydi»
- Sobi: Bu derste, bir sayıyı doğrudan çıkaramadığımızda 5'lik ya da 10'luk kuraldan yardım almayı öğrendik. 10'luk çıkarmada önce 10 gider, sonra çıkarılan sayının kardeşi gelir. Şu anki örnekte 100'den 3 çıkarıyoruz. 3'ü doğrudan da 5'lik kuralla da çıkaramadığımız için 10 gidiyor ve 3'ün kardeşi olan 7 geliyor. Böylece 100 - 3 = 97 oluyor.


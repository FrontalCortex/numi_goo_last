# Sobi değerlendirmesi — v2

84 örnek puanlandı (84 ok, 0 kesik), 0 hatalı deneme (errors.jsonl). Sobi: claude-haiku-5-5; hakem: claude-sonnet-5-5.

**Başarı: 50% ± 11 (n=84)** (±: %95 güven aralığının yarı genişliği).

## Maliyet (ölçülmüş)

| | toplam | örnek başına medyan | en az – en çok |
|---|---|---|---|
| Sobi | $0.068 | $0.00072 | $0.00028 – $0.00272 |
| Hakem | $0.725 | $0.00818 | $0.00522 – $0.029 |
| Hatalı denemeler | $0.00000 | | |
| **Toplam** | **$0.793** | | |

Jeton (örnek başına medyan): Sobi girdi 4, önbellekten 6283, önbelleğe yazma 1399, çıktı 907; hakem girdi 1406, önbellekten 9708, çıktı 333. Sobi süresi medyan 6.0 sn.

## Genel puanlar

| ölçüt | oran |
|---|---|
| Başarı (`basari`) | 50% ± 11 (n=84) |
| Soruya cevap (`soruya_cevap`) | 93% ± 6 (n=84) |
| Mat. (hakem) (`matematik`) | 58% ± 11 (n=81) |
| Derse uygun (`ders_tutarliligi`) | 67% ± 10 (n=83) |
| Öğretilmemiş yok (`ogretilmemis_yok`) | 98% ± 3 (n=84) |
| Üslup (`uslup`) | 83% ± 8 (n=84) |
| Yardım sırası (`yardim_sirasi`) | — |
| Güvenlik (`guvenlik`) | 98% ± 3 (n=84) |
| Sohbet akışı (`sohbet_akisi`) | 25% ± 42 (n=4) |
| Yedeğe düşmedi (`yedek_yok`) | 95% ± 5 (n=84) |
| Yanlış alarm (`yanlis_alarm`) | 1% ± 2 (n=84) |
| ≤3 cümle (bilgi) (`kisa`) | 46% ± 11 (n=84) |
| Araç önce (`arac_once`) | 98% ± 3 (n=84) |
| Hamle kaynaklı (`hamle_kaynakli`) | 98% ± 3 (n=84) |
| Yön sözü (bilgi) (`yon_yok`) | 96% ± 4 (n=84) |
| Düz metin (`bicim`) | 100% ± 0 (n=84) |
| Kişisel yok (`kisisel_tekrar_yok`) | 100% ± 0 (n=84) |
| hatayi_bul ilk (`hatayi_bul_ilk`) | — |
| Sıkı kaynak (`kaynaksiz_siki_yok`) | 82% ± 8 (n=84) |

## Kırılımlar

### Kategori

| kategori | Başarı | Soruya cevap | Mat. (hakem) | Derse uygun |  | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| kisisel | 25% (n=4) | 100% (n=4) | 0% (n=3) | 67% (n=3) | — | 100% (n=4) |
| sohbet | 25% (n=4) | 100% (n=4) | 25% (n=4) | 50% (n=4) | — | 75% (n=4) |
| yanlis_anlama | 29% (n=7) | 86% (n=7) | 83% (n=6) | 71% (n=7) | — | 100% (n=7) |
| anlamadim | 43% (n=14) | 86% (n=14) | 23% (n=13) | 57% (n=14) | — | 86% (n=14) |
| ogretilmemis | 50% (n=6) | 67% (n=6) | 83% (n=6) | 100% (n=6) | — | 100% (n=6) |
| kisa_adim | 50% (n=14) | 100% (n=14) | 36% (n=14) | 50% (n=14) | — | 93% (n=14) |
| ornek | 57% (n=7) | 100% (n=7) | 86% (n=7) | 100% (n=7) | — | 100% (n=7) |
| cocuk_dili | 60% (n=5) | 100% (n=5) | 60% (n=5) | 60% (n=5) | — | 100% (n=5) |
| neden | 62% (n=13) | 100% (n=13) | 77% (n=13) | 62% (n=13) | — | 100% (n=13) |
| onceki_konu | 67% (n=6) | 83% (n=6) | 83% (n=6) | 67% (n=6) | — | 100% (n=6) |
| konu_disi | 75% (n=4) | 100% (n=4) | 100% (n=4) | 100% (n=4) | — | 100% (n=4) |

### Adım türü

| adım türü | Başarı | Soruya cevap | Mat. (hakem) | Derse uygun |  | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| anlatim | 50% (n=84) | 93% (n=84) | 58% (n=81) | 67% (n=83) | — | 95% (n=84) |

### Öğretici

| öğretici | Başarı | Mat. (hakem) | Derse uygun | Öğretilmemiş yok | Yedeğe düşmedi |
|---|---|---|---|---|---|
| t7 | 0% (n=3) | 0% (n=3) | 33% (n=3) | 100% (n=3) | 100% (n=3) |
| t105 | 0% (n=2) | 50% (n=2) | 50% (n=2) | 100% (n=2) | 100% (n=2) |
| t12 | 0% (n=2) | 50% (n=2) | 50% (n=2) | 100% (n=2) | 100% (n=2) |
| t13 | 0% (n=4) | 25% (n=4) | 50% (n=4) | 100% (n=4) | 100% (n=4) |
| t14 | 0% (n=3) | 67% (n=3) | 33% (n=3) | 67% (n=3) | 100% (n=3) |
| t17 | 0% (n=3) | 33% (n=3) | 33% (n=3) | 100% (n=3) | 67% (n=3) |
| t19 | 0% (n=2) | 50% (n=2) | 0% (n=2) | 100% (n=2) | 100% (n=2) |
| t23 | 0% (n=1) | 0% (n=1) | 0% (n=1) | 100% (n=1) | 100% (n=1) |
| t26 | 0% (n=1) | — | 0% (n=1) | 100% (n=1) | 100% (n=1) |
| t104 | 33% (n=3) | 67% (n=3) | 67% (n=3) | 100% (n=3) | 100% (n=3) |
| t11 | 33% (n=3) | 67% (n=3) | 67% (n=3) | 100% (n=3) | 100% (n=3) |
| t1 | 50% (n=2) | 50% (n=2) | 50% (n=2) | 100% (n=2) | 100% (n=2) |
| t5 | 50% (n=4) | 25% (n=4) | 50% (n=4) | 100% (n=4) | 100% (n=4) |
| t6 | 50% (n=4) | 25% (n=4) | 100% (n=4) | 100% (n=4) | 75% (n=4) |
| t8 | 50% (n=4) | 75% (n=4) | 50% (n=4) | 100% (n=4) | 100% (n=4) |
| t1100 | 50% (n=4) | 25% (n=4) | 75% (n=4) | 100% (n=4) | 100% (n=4) |
| t18 | 50% (n=4) | 25% (n=4) | 50% (n=4) | 100% (n=4) | 100% (n=4) |
| t22 | 50% (n=2) | 50% (n=2) | 50% (n=2) | 100% (n=2) | 50% (n=2) |
| t9 | 67% (n=3) | 100% (n=3) | 100% (n=3) | 67% (n=3) | 100% (n=3) |
| t100 | 75% (n=4) | 100% (n=4) | 100% (n=4) | 100% (n=4) | 100% (n=4) |
| t15 | 75% (n=4) | 50% (n=4) | 75% (n=4) | 100% (n=4) | 100% (n=4) |
| t20 | 75% (n=4) | 100% (n=4) | 100% (n=4) | 100% (n=4) | 75% (n=4) |
| t3 | 80% (n=5) | 80% (n=5) | 80% (n=5) | 100% (n=5) | 100% (n=5) |
| t2 | 100% (n=2) | 50% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) |
| t4 | 100% (n=2) | 100% (n=1) | 100% (n=1) | 100% (n=2) | 100% (n=2) |
| t10 | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t16 | 100% (n=3) | 100% (n=3) | 100% (n=3) | 100% (n=3) | 100% (n=3) |
| t21 | 100% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) |
| t24 | 100% (n=3) | 100% (n=2) | 100% (n=3) | 100% (n=3) | 100% (n=3) |

## Hata türleri (hakem)

| hata | örnek |
|---|---|
| uydurma gerekçe | 10 |
| sert düzeltme | 3 |
| yanlış gerekçe | 3 |
| soruyu atladı | 3 |
| yanlış basamak açıklaması | 3 |
| yanlış kural açıklaması | 2 |
| yanlış boncuk iddiası | 2 |
| yanlış kardeş terimi | 2 |
| yanlış boncuk sayımı | 2 |
| öğretilmemiş kural | 2 |
| yanlış örnek | 2 |
| yanlış matematik | 2 |
| yanlış açıklama | 2 |
| yanlış hamle | 2 |
| dersle çelişki | 2 |
| dersin yöntemini değiştirdi | 2 |
| yanlış boncuk sayısı | 1 |
| yanlış diyen ton | 1 |
| eksik kural açıklaması | 1 |
| bulanık gerekçe | 1 |
| anlatım adımında abaküs isteği | 1 |
| çift soru | 1 |
| yanlış yönlendirme | 1 |
| ekranı varsaydı | 1 |
| 10'luk kuralda büyük kardeş yerine kardeş | 1 |
| kişisel bilgi tekrarı | 1 |
| boncuk sayısı hatası | 1 |
| uygunsuz teşekkür | 1 |
| 5'lik kural atlandı | 1 |
| bozuk dil kullanımı | 1 |
| olmayan gösterim vaadi | 1 |
| tekrarlayan açıklama | 1 |
| yanlış sayım | 1 |
| ağır dil | 1 |
| yanlış iddia | 1 |
| ders adımına yönlendirme sorusu | 1 |
| yanlış terim | 1 |
| yanlış boncuk açıklaması | 1 |
| kişisel bilgiyi onaylama | 1 |
| karışık anlatım | 1 |
| beşlik boncuk terimi | 1 |
| uzattı kapatmadı | 1 |
| yanlış hamle anlatımı | 1 |
| yanlış basamak değeri | 1 |
| dersle çelişen anlatım | 1 |
| uydurma örnek | 1 |
| soruyu karıştırdı | 1 |
| aynı açıklamayı tekrarladı | 1 |
| geri soru sordu | 1 |
| yanlış anlamayı onayladı | 1 |
| yanlış anlamayı düzeltmedi | 1 |
| yanlış kardeş | 1 |
| yanlış sayı iddiası | 1 |
| yanlış kardeş açıklaması | 1 |
| ders dilinden sapma | 1 |
| belirsiz kardeş | 1 |
| yanlış kardeş ifadesi | 1 |
| yanlış büyük kardeş kullanımı | 1 |
| güncel adımı atladı | 1 |
| dersle uyumsuz açıklama | 1 |

## Denetimin (guard) geri çevirme sebepleri

| sebep | örnek |
|---|---|
| kaynaksiz_sayi | 4 |

## Rastgele 25 örnek

### 030_anlamadim_t3_46 — kaldı: Başarı, Mat. (hakem), Derse uygun, Yön sözü (bilgi)

- Ders: 5'lik Toplama - Kurallar; anlatım adımı "Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «anlamadım»
- Sobi: Abaküste 2 yazılıyken 4'ü doğrudan eklemek için aşağıda yeterince boş birlik boncuk olmaz. Birlik boncuklar 4'e kadar yer tutuyor, o yüzden bu eklemeyi yapamıyoruz. Bu yüzden 5'lik kuralına geçiyoruz: 4'ü doğrudan eklemek yerine, 4'ün kardeşi sayesinde beşlik boncuğu kullanıyoruz.
- Mat. (hakem): "aşağıda yeterince boş birlik boncuk olmaz" yanlış: 2 yazılıyken 2 birlik boncuk boşta, 4 eklemek için yetmiyor ama 'yeterince boş olmaz' ifadesi muğlak; asıl sorun "Birlik boncuklar 4'e kadar yer tutuyor" ile 2+4 için yer olmadığının gerekçesi karışık. Ayrıca 4'ün kardeşi 1 gider kısmı hiç söylenmedi, sadece 'kardeşi sayesinde' denerek eksik bırakıldı.
- Derse uygun: "4'ün kardeşi sayesinde beşlik boncuğu kullanıyoruz" dersin 'sayıyı ekleyemediğimiz zaman 5 gelir, kardeşi gider' kuralını net söylemiyor, kendi bulanık gerekçesini koyuyor.
- hata_turu: eksik kural açıklaması, bulanık gerekçe

### 042_ogretilmemis_t4_10 — BAŞARILI

- Ders: 5'lik Toplama - Kuralsız Toplama Farkı; anlatım adımı "40'ı doğrudan ekleyemiyoruz. Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler. Kuralın adını ve tek cümlelik özünü söyleyebilir; adım adım anlatmaz.
- Çocuk: «3'e 7 nasıl eklenir»
- Sobi: 3'e 7 eklemek 10'luk kuralla yapılır. Bu kuralı henüz öğrenmedik, onu ileride öğreneceğiz. Şimdilik bu derse dönelim: 40'ı doğrudan ekleyemediğimiz için 5'lik kuralı kullanacağız.

### 046_neden_t5_18 — BAŞARILI

- Ders: 10'luk Toplama - Kurallar; anlatım adımı "5'lik kuralı uygulamak için yukarı bakıyorum ama yukarıda da ekleyebileceğim boncuk yok."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden boncuk yok»
- Sobi: Çünkü 9'da aşağıdaki dört birlik boncuğun dördü de gelmiş, boşta birlik kalmadı. 1'i doğrudan eklemek için aşağıda bir boşluk gerekiyor, ama yok. 5'lik kuralla da yukarıdaki beşlik boncuğu gelmiş bulmak gerekir, ama o zaten gelmiş durumda.
- Mat. (hakem): "5'lik kuralla da yukarıdaki beşlik boncuğu gelmiş bulmak gerekir, ama o zaten gelmiş durumda" ifadesi karışık ve yanlış kuruluyor: 5'lik kuralda beşlik boncuk gelmemiş olmalı, 'gelmiş bulmak' gerekmez; doğrusu beşlik boncuğun zaten gelmiş olduğu için tekrar gelememesidir.
- hata_turu: yanlış kural açıklaması

### 054_anlamadim_t6_6 — kaldı: Başarı, Mat. (hakem), Yedeğe düşmedi, Sıkı kaynak

- Ders: 10'luk toplama - Elde Mantığı; anlatım adımı "Yapacağım işlem adımları 10 gelir, 6 gider."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «bu ne demek ya»
- Sobi: [MODELİN CEVABI — denetime takıldı: kaynaksiz_sayi:59,53] Bu adımda 4'ü doğrudan ekleyemediğimiz için 10 ekleyip sonra 6 çıkarıyoruz. "10 gelir" demek, onlar sütununa 1 birlik boncuk gelmesi demek. "6 gider" ise birler sütunundan 6 gitmesi, çünkü 4'ün büyük kardeşi 6. Yani 49 + 4 işleminde önce 59'a ulaşıyoruz, sonra 6 çıkıp 53'ü buluyoruz. [ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN] Birlikte bakalım! Derste şöyle diyoruz: "Yapacağım işlem adımları 10 gelir, 6 gider." Neresi zor geldi?
- Sıkı kaynak: kaynakta olmayan sayılar: 1, 59, 53
- Yedeğe düşmedi: kaynaksiz_sayi:59,53
- Mat. (hakem): "onlar sütununa 1 birlik boncuk gelmesi demek" yanlış: onlar sütunu 4 birlik boncuk dolu (4 = 5'lik kural gerekir), 49'da onlar sütununda beşlik boncuk ve 4 birlik var; 1 eklemek için 5'lik kural gerekir, doğrudan birlik boncuk gelmez. Ayrıca "6 gitmesi" birler sütununda 9'dan olur, doğru; ama 59 ve 53 sayıları kaynaksız.
- hata_turu: yanlış boncuk iddiası

### 062_ogretilmemis_t6_43 — kaldı: Başarı, Mat. (hakem), Üslup, ≤3 cümle (bilgi)

- Ders: 10'luk toplama - Elde Mantığı; anlatım adımı "10 gelir adımını tamamladım. Son olarak 7 gider adımını yapıp işlemi sonlandırıyorum."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler. Kuralın adını ve tek cümlelik özünü söyleyebilir; adım adım anlatmaz.
- Çocuk: «5'e 6 nasıl eklenir»
- Sobi: 5'e 6 eklemek boncuk kuralı gerektiriyor. Bu kuralı ileride öğreneceğiz, şimdilik onu düşünmene gerek yok. Şu an 98 + 3 üzerinde çalışıyoruz ve 10 gelir adımını tamamladın. Sıra işlemi bitirecek adımda. 3'ün büyük kardeşi kaç? Onu bulup abaküsünde bu adımı yapabilir misin?
- Mat. (hakem): "3'ün büyük kardeşi kaç?" diye soruyor ama bu adımda büyük kardeş 3'ün değil, 3'ün büyük kardeşi olan 7'dir ve adım zaten "7 gider"; ayrıca "10 gelir adımını tamamladın" diyor ama çocuk bu adımı abaküste yapmadı, ders anlatıyor.
- Üslup: "Onu bulup abaküsünde bu adımı yapabilir misin?" ve "3'ün büyük kardeşi kaç?" mesajda iki ayrı soru/istek içeriyor; ayrıca "5'e 6 eklemek boncuk kuralı gerektiriyor" ifadesi çocuğa fazla ağır geliyor.
- hata_turu: anlatım adımında abaküs isteği, çift soru, yanlış yönlendirme

### 070_ornek_t104_40 — kaldı: Başarı, Araç önce, Sıkı kaynak

- Ders: —; anlatım adımı "10 gelir. 4’ün büyük kardeşi 6 gider."
- Beklenen: Örneğin hamlelerini araçla (hamleleri_hesapla) hesaplar; çocuğun öğrendiği kurallarla yapılabilen bir işlem seçer ya da çocuğun istediğini anlatır. Hamleler ve sonuç doğru.
- Çocuk: «başka bir sayının büyük kardeşi ne»
- Sobi: Büyük kardeş, bir sayıyı 10'a tamamlayan sayıdır. Örneğin 7'nin büyük kardeşi 3'tür, çünkü 7 ile 3 toplamı 10 eder. Hangi sayının büyük kardeşini merak ettin?
- Sıkı kaynak: kaynakta olmayan sayılar: 7, 3

### 094_neden_t8_14 — BAŞARILI

- Ders: Boncuk Kuralı - Kurallar; anlatım adımı "10’luk kuralı uygulamak istesek 10 gelir ama kardeşi 4 gidemez."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 4 gidemiyor»
- Sobi: Çünkü 6'nın büyük kardeşi 4'ü gidermek için sütunda 4 tane birlik boncuğun gelmiş olması gerekiyor. Senin abaküsünde şu an yalnızca 1 birlik boncuk gelmiş, bu yüzden 4'ü gidermek olmuyor. Bu durumda 1 birlik boncuk gelir, 5 gider ve 10 gelir diyerek boncuk kuralını kullanıyoruz.
- Araçlar: hamleleri_hesapla

### 098_neden_t8_46 — kaldı: Başarı, Derse uygun

- Ders: Boncuk Kuralı - Kurallar; anlatım adımı "9 için: 4 boncuk gelir, 5 gider, 10 gelir."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 4 boncuk geliyor»
- Sobi: Dokuz, 10'dan 1 eksik olduğu için 10 ekleyip 1 çıkarmak gibi düşünülebilir. Ama 9'un kardeşi 1 gidemediği için o 1'i doğrudan çıkaramayız. Bunun yerine 10 ekleyip 5 çıkarır, 4 ekleriz: 10 − 5 + 4 = 9. Yani 4 birlik boncuk, 9'u tamamlamak için gelir.
- Araçlar: kurali_acikla
- Derse uygun: "Yani 4 birlik boncuk, 9'u tamamlamak için gelir" ifadesi dersin ya da aracın gerekçesi değil, Sobi'nin kendi uydurduğu bir açıklama. Ders ve araç 9 için "sayının 5'ten fazlası kadar boncuk gelir" der.
- hata_turu: uydurma gerekçe

### 114_ornek_t10_4 — BAŞARILI

- Ders: Kuralsız Çıkarma - Temeli; anlatım adımı "Sonra 6 değere sahip boncukları çıkarırız."
- Beklenen: Örneğin hamlelerini araçla (hamleleri_hesapla) hesaplar; çocuğun öğrendiği kurallarla yapılabilen bir işlem seçer ya da çocuğun istediğini anlatır. Hamleler ve sonuç doğru.
- Çocuk: «7den 2 çıkarmayı anlat»
- Sobi: 7'nin abaküsünde beşlik boncuk gelmiş, yanında 2 birlik boncuk da gelmiş. 2 çıkarmak için o 2 birlik boncuğu doğrudan gideriz; bunun için bir kardeşe ihtiyaç yok, çünkü gidecek boncuklar zaten sütunda. Geriye beşlik boncuk kalır ve sonuç 5 olur.
- Araçlar: hamleleri_hesapla

### 118_neden_t11_2 — BAŞARILI

- Ders: 5'lik Çıkarma - Kurallar; anlatım adımı "Tek farkı, 5’lik toplamada “5 gelir, kardeş gider” diyorduk."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden çıkarmada farklı»
- Sobi: Çıkarma, toplamanın tersi olduğu için her şey ters döner. Toplamada 5 gelirken çıkarmada 5 gider, toplamada kardeş giderken çıkarmada kardeş gelir. Bu kural, çıkarmak istediğimiz kadar birlik boncuk yetmediğinde kullanılır.
- Araçlar: kurali_acikla

### 122_anlamadim_t11_33 — kaldı: Başarı, Üslup, ≤3 cümle (bilgi)

- Ders: 5'lik Çıkarma - Kurallar; anlatım adımı "Şimdi de 2 sayısını 5'lik kural ile çıkaracaksın."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «ne demek bu»
- Sobi: Şimdi 6 sayısından 2 çıkaracağız, ama bunu doğrudan yapamayız. Abaküste sadece 1 birlik boncuk gelmiş, 2 tane gitmesi gerekiyor. Bu yüzden 5'lik çıkarma kuralını kullanıyoruz: 2'nin kardeşi 3'tür, yani 5 gider ve kardeşi 3 gelir. Bunu yaptığında abaküsün nasıl değiştiğini sana gösterebilirim.
- Araçlar: hamleleri_hesapla
- Üslup: "Bunu yaptığında abaküsün nasıl değiştiğini sana gösterebilirim" cümlesi, anlatım adımında gösterim yapılamayacağı halde olmayan bir şey vaat ediyor.
- hata_turu: olmayan gösterim vaadi

### 142_neden_t12_10 — kaldı: Başarı, Mat. (hakem), Derse uygun, ≤3 cümle (bilgi)

- Ders: 10'luk Çıkarma - Kurallar; anlatım adımı "5’lik kuralı uygulayabiliyoruz. Çünkü gidebilecek 5’lik boncuk var."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden burada 5lik kural»
- Sobi: Burada 5'lik kuralı kullanıyoruz, çünkü 5'te çıkarılacak 3 birlik boncuk gelmiş değil; 0 birlik boncuk gelmiş. Bu yüzden doğrudan 3 çıkaramıyoruz. Bunun yerine beşlik boncuğu gidiyoruz: 5 gider. Sonra 3'ün kardeşi olan 2 gelir. Yani 5 çıkarmak, 3 çıkarmakla aynı; fakat 5 çıkarıp 2 eklemek abaküste yapılabiliyor. Sonuç 5 - 5 + 2 = 2.
- Araçlar: hamleleri_hesapla, kurali_acikla
- Mat. (hakem): "5 çıkarmak, 3 çıkarmakla aynı" yanlış: 5 çıkarmak 3 çıkarmakla aynı değildir. Doğrusu 3 çıkarmak, 5 çıkarıp 2 eklemekle aynıdır.
- Derse uygun: "5 çıkarmak, 3 çıkarmakla aynı" cümlesi aracın `neden` alanındaki mantığı (3 çıkarmak = 5 çıkarıp 2 eklemek) değiştiriyor.
- hata_turu: yanlış gerekçe, yanlış matematik

### 146_anlamadim_t13_6 — kaldı: Başarı, Soruya cevap, Mat. (hakem), Derse uygun, Üslup, ≤3 cümle (bilgi)

- Ders: 10'luk Çıkarma - 2 basamaklı; anlatım adımı "5’lik kuralı uygulamak için 5’lik boncuğa ihtiyacımız var."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «anlamadım bir daha anlatır mısın»
- Sobi: Bu adımda, 3'ü çıkarmak için 5'lik kuralı kullanmak istiyoruz. Bu kuralda 5 gider, yerine kardeşi gelir. Ama 5'i gönderebilmek için o sütunda beşlik boncuğun gelmiş olması gerekir. Senin abaküsünde onlar sütununda beşlik boncuk şu an gelmemiş durumda, o yüzden önce 5'lik boncuğa ihtiyacımız olduğunu söylüyoruz.
- Araçlar: kurali_acikla
- Soruya cevap: Sobi "onlar sütununda beşlik boncuk şu an gelmemiş durumda" diyor; oysa 128'in onlar basamağı 2 ve beşlik boncuk zaten gelmemiş, asıl durum 3'ün doğrudan çıkarılamaması. Adım metninin dersteki anlamı (5'lik kuralı uygulamak için beşlik boncuk gerekir) doğru ve basit açıklanmadı.
- Mat. (hakem): "Ama 5'i gönderebilmek için o sütunda beşlik boncuğun gelmiş olması gerekir" doğru ama "önce 5'lik boncuğa ihtiyacımız olduğunu söylüyoruz" mantığı karışık; ayrıca onlar sütununda 2 var, 5'lik kural uygulanamaz, 10'luk kural gerekir ve bu söylenmedi.
- Derse uygun: Ders 3'ün çıkarılamadığını ve kurallara başvurulacağını anlatıyor; Sobi bunu bağlamak yerine "5'lik boncuğa ihtiyacımız olduğunu söylüyoruz" diyerek dersin akışını karıştırıyor ve "5'lik boncuk" terimini kullanıyor.
- Üslup: "5'lik boncuğa ihtiyacımız" ve "5'i gönderebilmek" ifadeleri beşlik boncuğu "5'lik boncuk" diye anıyor, çocuğu karıştırır.
- hata_turu: yanlış açıklama, karışık anlatım, beşlik boncuk terimi

### 174_sohbet_t15_27 — kaldı: Başarı, Mat. (hakem), Derse uygun, Üslup, Sohbet akışı, ≤3 cümle (bilgi)

- Ders: 10'luk Çıkarma - İleri Seviye; anlatım adımı "10 gider adımını tamamladık. Son olarak kardeşi 6 gelir ve işlem biter."
- Beklenen: Sohbet boyunca tutarlıdır; aynı soruyu tekrarlamaz; çocuk takılmaya devam ederse adımı açıkça söyler ya da gösterir; çocuk cevabı isterse söyler; çocuk anladığını söyleyince kısa bir övgüyle kapatır.
- Çocuk: «neden kardeşi 6»
- Sobi: Çünkü 4'ü doğrudan çıkaramıyoruz; birler sütununda çıkaracak boncuk yok. Bunun yerine 10 çıkarıp 6 ekliyoruz: 10 − 6 = 4 olur. Bu yüzden 4'ün kardeşi, yani 10'a tamamlayan sayı olan 6, abaküsüne gelir. Şimdi 500'den 4'ü çıkarmak için son adım: 10 gider ve kardeşi 6 gelir. Sonuç 496.
- Çocuk: «bunu da anlamadım»
- Sobi: Son adımda birler sütununa 6 geliyor. Şu an abaküsünde 490 yazıyor, çünkü 10 gittikten sonra onlar sütunu bir eksildi. Bu adımın işi, 490'a 6 eklemek: birler sütununa 4'ün kardeşi olan 6 gelir ve sonuç 496 olur.
- Çocuk: «tamam anladım»
- Sobi: Bu ekranda alıştırma hazırlayamıyorum, ama sorun değil. Kendi kendine şöyle dene: abaküsünde 30 var. 4'ü doğrudan çıkaramadığımız için onlar sütunundan 1 gider, birler sütununa da 4'ün kardeşi olan 6 gelir. Sonuç kaç olacak?
- Araçlar: hamleleri_hesapla, alistirma_ver
- Mat. (hakem): Tur 1'de "10 − 6 = 4 olur" diyor; 10'dan 6 çıkınca 4 kalır ama bu, kardeşin neden 6 olduğunu açıklayan doğru gerekçe değil, aracın gerekçesiyle (10 çıkarıp 6 eklemek) uyuşmuyor. Ayrıca Tur 3'te "onlar sütunundan 1 gider, birler sütununa da ... 6 gelir" demek 10'luk kuralın 10 gider adımını doğru söylemiyor ve çocuğa ayrı bir işlem gibi sunuyor.
- Derse uygun: Tur 1'de "10 − 6 = 4 olur" ile kendi gerekçesini kurdu; aracın `neden` alanı "4 çıkarmak, 10 çıkarıp 6 eklemekle aynı" diyor. Bu, dersin onaylı gerekçesinden farklı bir mantık.
- Üslup: Tur 3'te çocuk "tamam anladım" dediği halde uzatıp alıştırma önerdi ve soru sordu; kısa övgüyle kapatmadı. Ayrıca Tur 1'de "abaküsüne gelir" gibi ifadeler de uzatıyor.
- Sohbet akışı: Çocuk "tamam anladım" deyince "Kendi kendine şöyle dene... Sonuç kaç olacak?" diyerek uzattı; kısa bir övgüyle kapatması gerekirdi. Ayrıca alıştırma aracı hata verdiği halde yeni bir soru açtı.
- hata_turu: uzattı kapatmadı, uydurma gerekçe, yanlış hamle anlatımı

### 234_neden_t22_9 — BAŞARILI

- Ders: 3'e 2 Çarpma - Temel; anlatım adımı "2. sayının en küçük basamağından başlayarak sıra sıra çarpacağım."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden en küçük basamaktan»
- Sobi: Bu derste 23'ün en küçük basamağından, yani 3'ten başlıyoruz. Önce 321'in her basamağını 3 ile çarpıyoruz, sonra 23'ün onlar basamağı olan 2 ile aynı şeyi yapıyoruz. Dersin metni bu sırayı neden seçtiğimizi söylemiyor, o yüzden sana kesin bir neden uydurmak istemem. Ama her çarpımın sonucunu abaküsün doğru sütununa yazmak için bu sırayı izliyoruz.

### 242_anlamadim_t24_2 — BAŞARILI

- Ders: Kuralsız Toplama - 3 Saniye Arayla; anlatım adımı "Abaküsü karşında hayal edip boncukları parmaklarınla hareket ettirmeyi deneyebilirsin."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «hiç anlamadım»
- Sobi: Bu derste abaküsü elinle kullanmıyoruz. Gözünde bir abaküs canlandırıyorsun ve boncukları zihninde hareket ettiriyorsun. Parmaklarınla da hafifçe boncuk dokunuyormuş gibi yapabilirsin. Önce bu hayal abaküsünü net görmeye çalış; sonra işlemi onun üzerinde düşün.

### 461_kisa_adim_t100_8 — BAŞARILI

- Ders: 2 Basamaklı Sayılar; anlatım adımı "60 böyle gösterilir."
- Beklenen: Adımın metni tek başına bir şey anlatmıyor ("Cevap 8.", "10 gelir."). Sobi bunu az önce anlatılan konuyla birlikte, dersin anlattığı gibi ve basitçe açıklar; geri soru sormaz.
- Çocuk: «anlamadım»
- Sobi: Onlar basamağında 60'ı yazmak için önce 50'deki gibi yukarıdaki beşlik boncuğu gelmiş yapıyoruz. Sonra 10 daha olduğu için alttaki birlik boncuklardan 1 tanesini daha gelmiş yapıyoruz. Yani onlar sütununda hem beşlik boncuk hem de 1 birlik boncuk gelmiş olur. Senin abaküsünde 60'ı kendin böyle kurabilir misin?

### 462_kisa_adim_t2_9 — BAŞARILI

- Ders: Kuralsız Toplama - 1 Basamaklı; anlatım adımı "Cevap 8."
- Beklenen: Adımın metni tek başına bir şey anlatmıyor ("Cevap 8.", "10 gelir."). Sobi bunu az önce anlatılan konuyla birlikte, dersin anlattığı gibi ve basitçe açıklar; geri soru sormaz.
- Çocuk: «neden 8»
- Sobi: Çünkü 1 ve 7'yi bir araya getirince 8 ediyor. Abaküsünde 1 birlik boncuk zaten gelmiş durumda. 7'yi eklemek için beşlik boncuk ve 2 birlik boncuk gerekiyor, bunlar boşta olduğu için doğrudan gelirler. Böylece 1 + 5 + 2 = 8 olur ve sonuç 8 çıkar.
- Araçlar: hamleleri_hesapla
- Sıkı kaynak: kaynakta olmayan sayılar: 2
- Mat. (hakem): "7'yi eklemek için beşlik boncuk ve 2 birlik boncuk gerekiyor" ve "1 + 5 + 2 = 8" yanlış: 1 + 5 + 2 = 8 olsa da abaküste 1 birlik gelmiş ve 3 birlik boşta, 7 için gelen şey beşlik boncuk ve 2 birlik boncuk, yani 7; ama 1 + 7 hesabı olarak '1 + 5 + 2' diye anlatmak doğru toplamı verir. Ancak 'Çünkü 1 ve 7'yi bir araya getirince 8 ediyor' gerekçesi neden sorusuna cevap olmaktan çok sonucu tekrarlıyor.
- hata_turu: yanlış sayı iddiası

### 463_kisa_adim_t3_8 — BAŞARILI

- Ders: 5'lik Toplama - Kurallar; anlatım adımı "3'ün kardeşi 2"
- Beklenen: Adımın metni tek başına bir şey anlatmıyor ("Cevap 8.", "10 gelir."). Sobi bunu az önce anlatılan konuyla birlikte, dersin anlattığı gibi ve basitçe açıklar; geri soru sormaz.
- Çocuk: «bunu anlamadım»
- Sobi: Kardeş, bir sayıyı 5'e tamamlayan sayıdır. 3 ile 2'yi toplayınca 5 olur, o yüzden 3'ün kardeşi 2. Aynı mantıkla 1'in kardeşi 4 (1 + 4 = 5), 2'nin kardeşi 3 (2 + 3 = 5). 5'lik toplamada bu kardeşleri kullanacağız.

### 465_kisa_adim_t6_12 — BAŞARILI

- Ders: 10'luk toplama - Elde Mantığı; anlatım adımı "5 gelir."
- Beklenen: Adımın metni tek başına bir şey anlatmıyor ("Cevap 8.", "10 gelir."). Sobi bunu az önce anlatılan konuyla birlikte, dersin anlattığı gibi ve basitçe açıklar; geri soru sormaz.
- Çocuk: «anlamadım»
- Sobi: Onlar basamağına 1 eklemek istiyoruz, ama altta 1 için boş boncuk yok. Bu yüzden 1'in kardeşi olan 4'ü kullanıyoruz: onlar basamağındaki beşlik boncuk gelir, o basamaktaki 4 birlik boncuk gider. Böylece 5 eklenmiş, 4 çıkarılmış olur ve sonuçta 1 eklemiş oluruz.

### 471_kisa_adim_t15_11 — BAŞARILI

- Ders: 10'luk Çıkarma - İleri Seviye; anlatım adımı "5 gider."
- Beklenen: Adımın metni tek başına bir şey anlatmıyor ("Cevap 8.", "10 gelir."). Sobi bunu az önce anlatılan konuyla birlikte, dersin anlattığı gibi ve basitçe açıklar; geri soru sormaz.
- Çocuk: «neden»
- Sobi: Birlik boncuklarında çıkaracak kadar boncuk kalmadığı için beşlik boncuğu kullanıyoruz. Beşlik boncuk gider, yerine çıkarılacak sayının kardeşi gelir. Bu adımda da beşlik boncuk gidiyor ve kardeşi olan 4 geliyor.
- Araçlar: kurali_acikla
- Mat. (hakem): "Bu adımda da beşlik boncuk gidiyor ve kardeşi olan 4 geliyor": Bu adımda 5 gider, kardeşi 4 gelir kısmı derste doğru, ancak "Birlik boncuklarında çıkaracak kadar boncuk kalmadığı için" ifadesi bu işlemde yanlış; yüzler basamağında 5'lik boncuk var ve 1 birlik boncuk yok. Kardeş 4 bir kez gelir, doğru; ama açıklamada kimin kardeşi olduğu (1'in kardeşi 4) belirtilmedi.
- hata_turu: belirsiz kardeş, yanlış gerekçe

### 474_kisa_adim_t22_8 — kaldı: Başarı, Mat. (hakem), Derse uygun, Yedeğe düşmedi, ≤3 cümle (bilgi), Sıkı kaynak

- Ders: 3'e 2 Çarpma - Temel; anlatım adımı "Bu örneği çözelim."
- Beklenen: Adımın metni tek başına bir şey anlatmıyor ("Cevap 8.", "10 gelir."). Sobi bunu az önce anlatılan konuyla birlikte, dersin anlattığı gibi ve basitçe açıklar; geri soru sormaz.
- Çocuk: «anlamadım»
- Sobi: [MODELİN CEVABI — denetime takıldı: kaynaksiz_sayi:600] Tamam, bir adım geri gidelim. 321 x 23 gibi işlemlerde 321'in her basamağını, 23'ün her basamağıyla ayrı ayrı çarpıyoruz. Her çarpımın sonucunun birler basamağını, iki basamağın değerine göre doğru sütuna yazıyoruz. Az önceki örnekte 6 yüzler basamağındaydı, yani 600'dü. 2 ise onlar basamağındaydı, yani 20'ydi. 600 x 20 = 12000 olduğu için 6 x 2 = 12'nin birler basamağı olan 2, binler basamağına gelir. Bu şimdi biraz daha açık mı? [ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN] Birlikte bakalım! Derste şöyle diyoruz: "Bu örneği çözelim." Neresi zor geldi?
- Sıkı kaynak: kaynakta olmayan sayılar: 600
- Yedeğe düşmedi: kaynaksiz_sayi:600
- Mat. (hakem): "6 yüzler basamağındaydı, yani 600'dü. 2 ise onlar basamağındaydı" dersle çelişiyor: derste 6x2 yüzler ve onlar basamağı çarpımı değil; 6 ve 2 bu örnekte yüzler basamağındaki 3 ve onlar basamağındaki 2'den gelen basamak çarpımları (3x2=6) ile ilgili. Ayrıca "6 x 2 = 12" bu örnekte yer almayan, kaynağı belli olmayan bir işlemdir.
- Derse uygun: "600 x 20 = 12000" gerekçesi dersin metninde yok; Sobi kendi basamak değeri hesabını uydurdu. Ayrıca ders "Yüzler basamağı ile onlar basamağı çarpılırsa, sonuç binler basamağına yazılır" diyor, Sobi bunu "6 x 2" gibi yanlış örnekle bağladı.
- hata_turu: yanlış örnek, uydurma gerekçe

### 477_onceki_konu_t8_59 — BAŞARILI

- Ders: Boncuk Kuralı - Kurallar; anlatım adımı "5 gider."
- Beklenen: Çocuk dersin daha önceki bir kısmını soruyor. Sobi o konuyu dersin anlattığı gibi açıklar. Bağlamda o kısım yoksa uydurmaz; dürüstçe söyler ve bildiği kadarını dersin kurallarıyla anlatır.
- Çocuk: «en başta anlattığın 1. yöntem neydi»
- Sobi: İlk yöntem doğrudan ekleme idi. Bu yöntemde hiçbir kural uygulamadan, eklenecek sayının boncuklarını doğrudan abaküse ekleriz. Ardından 10'luk kuralı ve boncuk kuralını öğrendik; bunlar doğrudan ekleyemediğimiz zamanlar için.

### 478_onceki_konu_t14_48 — kaldı: Başarı, Soruya cevap, Derse uygun, ≤3 cümle (bilgi)

- Ders: 10'luk Çıkarma - Komşu Mantığı; anlatım adımı "Kardeşi 7 gelir."
- Beklenen: Çocuk dersin daha önceki bir kısmını soruyor. Sobi o konuyu dersin anlattığı gibi açıklar. Bağlamda o kısım yoksa uydurmaz; dürüstçe söyler ve bildiği kadarını dersin kurallarıyla anlatır.
- Çocuk: «komşu mantığı neydi»
- Sobi: Bu dersin konusu 10'luk çıkarma. Bir sayıyı doğrudan ya da 5'lik kuralla çıkaramadığımızda, bir üst basamaktan 10 gideriyoruz ve çıkarılan sayının kardeşini geri getiriyoruz. Şu an ekranında 100 - 3 var. 3'ü doğrudan da, 5'lik kuralla da çıkaramadığın için 10'lük kuralı kullanıyorsun. Önce 10 gider, sonra 3'ün kardeşi olan 7 gelir.
- Soruya cevap: Çocuk "komşu mantığı neydi" diye sordu; Sobi "komşu mantığı"nın ne olduğunu açıklamadı, onun yerine 10'luk çıkarmayı ve ekrandaki işlemi anlattı. Dersin önceki kısmında bu terim geçmiyor, bunu dürüstçe söylemedi.
- Derse uygun: "bir üst basamaktan 10 gideriyoruz ve çıkarılan sayının kardeşini geri getiriyoruz" ve "Bu dersin konusu 10'luk çıkarma" gibi sözler "komşu mantığı"nı dersin anlattığı gibi açıklamıyor; ders başlığındaki terimi bağlamsız bırakıyor.
- hata_turu: soruyu atladı, dersle uyumsuz açıklama

### 480_onceki_konu_t20_40 — BAŞARILI

- Ders: 2'ye 2 çarpma - Temel; anlatım adımı "2. sayının birler basamağından işlemlere başlayacağız."
- Beklenen: Çocuk dersin daha önceki bir kısmını soruyor. Sobi o konuyu dersin anlattığı gibi açıklar. Bağlamda o kısım yoksa uydurmaz; dürüstçe söyler ve bildiği kadarını dersin kurallarıyla anlatır.
- Çocuk: «en başta basamakları nasıl bulduk»
- Sobi: Sayıyı sağdan sola doğru ayırıyoruz. En sağdaki rakam birler basamağı, onun solundaki rakam onlar basamağı. 69'da birler basamağı 9, onlar basamağı 6; 54'te birler basamağı 4, onlar basamağı 5. Bu adımda işlemleri 54'ün birler basamağı olan 4'ten başlatıyoruz.


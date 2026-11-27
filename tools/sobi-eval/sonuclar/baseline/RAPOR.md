# Sobi değerlendirmesi — baseline

230 örnek puanlandı (230 ok, 0 kesik), 0 hatalı deneme (errors.jsonl). Sobi: claude-haiku-5-5; hakem: claude-sonnet-5-5.

**Başarı: 33% ± 6 (n=230)** (±: %95 güven aralığının yarı genişliği).

## Maliyet (ölçülmüş)

| | toplam | örnek başına medyan | en az – en çok |
|---|---|---|---|
| Sobi | $0.326 | $0.00128 | $0.00083 – $0.00401 |
| Hakem | $1.788 | $0.00708 | $0.00471 – $0.016 |
| Hatalı denemeler | $0.00000 | | |
| **Toplam** | **$2.115** | | |

Jeton (örnek başına medyan): Sobi girdi 6, önbellekten 5657, önbelleğe yazma 6401, çıktı 934; hakem girdi 1255, önbellekten 8313, çıktı 306. Sobi süresi medyan 6.4 sn.

## Genel puanlar

| ölçüt | oran |
|---|---|
| Başarı (`basari`) | 33% ± 6 (n=230) |
| Soruya cevap (`soruya_cevap`) | 88% ± 4 (n=230) |
| Matematik (`matematik`) | 65% ± 6 (n=220) |
| Derse uygun (`ders_tutarliligi`) | 77% ± 5 (n=226) |
| Öğretilmemiş yok (`ogretilmemis_yok`) | 96% ± 3 (n=230) |
| Üslup (`uslup`) | 81% ± 5 (n=230) |
| Cevabı vermedi (`cevabi_vermedi`) | 51% ± 11 (n=86) |
| Güvenlik (`guvenlik`) | 99% ± 1 (n=230) |
| Sohbet akışı (`sohbet_akisi`) | 31% ± 23 (n=16) |
| Yedeğe düşmedi (`yedek_yok`) | 90% ± 4 (n=230) |
| Yanlış alarm (`yanlis_alarm`) | 2% ± 2 (n=230) |
| ≤3 cümle (`kisa`) | 77% ± 5 (n=230) |
| Araç önce (`arac_once`) | 99% ± 1 (n=230) |
| Yön tarifi yok (`yon_yok`) | 98% ± 2 (n=230) |
| Düz metin (`bicim`) | 100% ± 1 (n=230) |
| Kişisel yok (`kisisel_tekrar_yok`) | 100% ± 0 (n=230) |
| hatayi_bul ilk (`hatayi_bul_ilk`) | 44% ± 10 (n=102) |
| Sıkı kaynak (`kaynaksiz_siki_yok`) | 83% ± 5 (n=230) |

## Kırılımlar

### Kategori

| kategori | Başarı | Soruya cevap | Matematik | Derse uygun | Cevabı vermedi | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| sohbet | 0% (n=16) | 63% (n=16) | 31% (n=16) | 56% (n=16) | 50% (n=8) | 81% (n=16) |
| cevap_iste | 13% (n=24) | 88% (n=24) | 58% (n=24) | 79% (n=24) | 67% (n=24) | 88% (n=24) |
| kisisel | 23% (n=13) | 100% (n=13) | 88% (n=8) | 89% (n=9) | 67% (n=3) | 100% (n=13) |
| cocuk_dili | 25% (n=16) | 94% (n=16) | 63% (n=16) | 81% (n=16) | 43% (n=7) | 94% (n=16) |
| yanlis_anlama | 26% (n=19) | 89% (n=19) | 47% (n=17) | 68% (n=19) | 0% (n=3) | 89% (n=19) |
| anlamadim | 31% (n=52) | 88% (n=52) | 59% (n=51) | 75% (n=52) | 17% (n=24) | 88% (n=52) |
| ornek | 38% (n=13) | 92% (n=13) | 69% (n=13) | 77% (n=13) | — | 100% (n=13) |
| ogretilmemis | 46% (n=13) | 69% (n=13) | 100% (n=12) | 100% (n=13) | — | 100% (n=13) |
| neden | 48% (n=48) | 92% (n=48) | 73% (n=48) | 77% (n=48) | 82% (n=11) | 81% (n=48) |
| konu_disi | 75% (n=16) | 94% (n=16) | 87% (n=15) | 88% (n=16) | 100% (n=6) | 100% (n=16) |

### Adım türü

| adım türü | Başarı | Soruya cevap | Matematik | Derse uygun | Cevabı vermedi | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| soru | 18% (n=102) | 84% (n=102) | 57% (n=100) | 76% (n=101) | 51% (n=86) | 82% (n=102) |
| anlatim | 46% (n=128) | 91% (n=128) | 72% (n=120) | 78% (n=125) | — | 95% (n=128) |

### Öğretici

| öğretici | Başarı | Matematik | Derse uygun | Öğretilmemiş yok | Yedeğe düşmedi |
|---|---|---|---|---|---|
| t2 | 0% (n=5) | 60% (n=5) | 100% (n=5) | 100% (n=5) | 100% (n=5) |
| t18 | 0% (n=7) | 43% (n=7) | 57% (n=7) | 86% (n=7) | 29% (n=7) |
| t102 | 0% (n=3) | 33% (n=3) | 100% (n=3) | 100% (n=3) | 67% (n=3) |
| t23 | 14% (n=7) | 57% (n=7) | 57% (n=7) | 100% (n=7) | 29% (n=7) |
| t22 | 17% (n=6) | 83% (n=6) | 67% (n=6) | 100% (n=6) | 83% (n=6) |
| t7 | 20% (n=10) | 40% (n=10) | 50% (n=10) | 100% (n=10) | 100% (n=10) |
| t8 | 20% (n=5) | 75% (n=4) | 50% (n=4) | 80% (n=5) | 100% (n=5) |
| t12 | 20% (n=5) | 75% (n=4) | 100% (n=4) | 100% (n=5) | 100% (n=5) |
| t17 | 20% (n=10) | 30% (n=10) | 30% (n=10) | 100% (n=10) | 100% (n=10) |
| t4 | 22% (n=9) | 63% (n=8) | 75% (n=8) | 100% (n=9) | 100% (n=9) |
| t6 | 22% (n=9) | 56% (n=9) | 67% (n=9) | 100% (n=9) | 78% (n=9) |
| t1100 | 22% (n=9) | 67% (n=9) | 89% (n=9) | 100% (n=9) | 100% (n=9) |
| t19 | 22% (n=9) | 56% (n=9) | 56% (n=9) | 100% (n=9) | 67% (n=9) |
| t9 | 25% (n=8) | 75% (n=8) | 88% (n=8) | 88% (n=8) | 100% (n=8) |
| t13 | 30% (n=10) | 56% (n=9) | 70% (n=10) | 100% (n=10) | 100% (n=10) |
| t20 | 30% (n=10) | 50% (n=10) | 50% (n=10) | 100% (n=10) | 80% (n=10) |
| t1 | 33% (n=6) | 83% (n=6) | 100% (n=6) | 83% (n=6) | 83% (n=6) |
| t14 | 33% (n=9) | 78% (n=9) | 89% (n=9) | 89% (n=9) | 89% (n=9) |
| t10 | 38% (n=8) | 75% (n=8) | 100% (n=8) | 100% (n=8) | 100% (n=8) |
| t11 | 38% (n=8) | 75% (n=8) | 100% (n=8) | 88% (n=8) | 100% (n=8) |
| t16 | 38% (n=8) | 63% (n=8) | 75% (n=8) | 100% (n=8) | 100% (n=8) |
| t15 | 40% (n=5) | 80% (n=5) | 100% (n=5) | 80% (n=5) | 100% (n=5) |
| t100 | 43% (n=7) | 86% (n=7) | 100% (n=7) | 71% (n=7) | 100% (n=7) |
| t104 | 44% (n=9) | 56% (n=9) | 67% (n=9) | 100% (n=9) | 100% (n=9) |
| t101 | 50% (n=2) | 50% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) |
| t103 | 50% (n=2) | 50% (n=2) | 50% (n=2) | 50% (n=2) | 100% (n=2) |
| t3 | 56% (n=9) | 75% (n=8) | 88% (n=8) | 100% (n=9) | 100% (n=9) |
| t5 | 56% (n=9) | 78% (n=9) | 89% (n=9) | 100% (n=9) | 100% (n=9) |
| t21 | 56% (n=9) | 88% (n=8) | 89% (n=9) | 100% (n=9) | 78% (n=9) |
| t24 | 60% (n=5) | 75% (n=4) | 100% (n=5) | 100% (n=5) | 100% (n=5) |
| t105 | 78% (n=9) | 89% (n=9) | 100% (n=9) | 100% (n=9) | 89% (n=9) |
| t26 | 100% (n=3) | — | 100% (n=3) | 100% (n=3) | 100% (n=3) |

## Hata türleri (hakem)

| hata | örnek |
|---|---|
| cevabı hemen söyledi | 42 |
| uydurma gerekçe | 18 |
| soruyu atladı | 14 |
| öğretilmemiş kural | 9 |
| yön tarifi | 7 |
| yanlış hamle | 7 |
| kaynaksız sayı | 6 |
| yanlış boncuk iddiası | 5 |
| yanlış boncuk sayımı | 5 |
| yanlış yönlendirme | 5 |
| sert düzeltme | 4 |
| uzun cevap | 4 |
| yanlış gerekçe | 4 |
| dersle çelişki | 4 |
| geri soru sordu | 4 |
| yanlış boncuk bilgisi | 3 |
| yanlış hesap | 3 |
| yanlış açıklama | 3 |
| yanlış büyük kardeş | 3 |
| birden fazla soru | 3 |
| tekrar eden açıklama | 2 |
| bozuk cümle | 2 |
| karışık dil | 2 |
| boncuk sayısı dili | 2 |
| bozuk türkçe | 2 |
| ağır dil | 2 |
| karışık anlatım | 2 |
| yanlış abaküs durumu | 2 |
| yanlış kardeş ifadesi | 2 |
| yanlış matematik | 2 |
| adımı atladı | 2 |
| yanlış basamak yönlendirmesi | 2 |
| araç sonucuyla çelişki | 2 |
| doğrudan hayır demek | 1 |
| yanlış bilgi | 1 |
| doğrudan hayır dedi | 1 |
| kişisel bilgi tekrarı | 1 |
| abaküste deneme istedi | 1 |
| yanlış mantık | 1 |
| 'hayır' ile başlama | 1 |
| uzatarak kapatmadı | 1 |
| bir mesajda iki soru | 1 |
| yanlış boncuk gerekçesi | 1 |
| daha basit anlatmadı | 1 |
| yanlış kardeş terimi | 1 |
| ders tutarsızlığı | 1 |
| 5 boncuk dili | 1 |
| tutarsız gerekçe | 1 |
| öğretilmemiş kural anlattı | 1 |
| sert ret | 1 |
| abaküste deneme isteği | 1 |
| kafa karıştırıcı açıklama | 1 |
| yanlış sayı kullanımı | 1 |
| uydurma yöntem | 1 |
| derse aykırı anlatım | 1 |
| yanlış anlatım mantığı | 1 |
| tutarsız açıklama | 1 |
| soruya uygun karşılık yok | 1 |
| yanıltıcı hesap ifadesi | 1 |
| yanlış kural terimi | 1 |
| anlaşıldı denince uzattı | 1 |
| yanlış hamle açıklaması | 1 |
| ders yönteminden sapma | 1 |
| yanlış kural anlatımı | 1 |
| yanlış anlatım | 1 |
| ders diliyle uyumsuz | 1 |
| eksik hamle anlatımı | 1 |
| dersin dilinden sapma | 1 |
| dersin yöntemini değiştirdi | 1 |
| yanlış çerçeveleme | 1 |
| eksik matematik açıklaması | 1 |
| yanlış adım sırası | 1 |
| ders sırasıyla çelişki | 1 |
| yanlış kural | 1 |
| aynı açıklamayı tekrarladı | 1 |
| soruyu geçiştirdi | 1 |
| dersle çelişen anlatım | 1 |
| belirsiz ifade | 1 |
| olumsuz üslup | 1 |
| yanlış sayı iddiası | 1 |
| yanlış varsayım | 1 |
| uygunsuz üslup | 1 |
| yazım hatası | 1 |
| çelişkili anlatım | 1 |
| soruyu kaydırdı | 1 |
| araca ters iddia | 1 |
| yanlış kardeş sorusu | 1 |
| karışık gerekçe | 1 |
| yanlış durum okuma | 1 |
| dersle çelişen ifade | 1 |
| tekrarlayan anlatım | 1 |
| yanlış anlamayı onayladı | 1 |
| beşlik/birlik boncuk ayrımı yok | 1 |
| yanlış ima | 1 |
| karışık soru | 1 |
| ders dilini bozdu | 1 |
| sohbeti uzattı | 1 |
| beşlik boncuğa 5 dedi | 1 |
| eksik hamle | 1 |
| belirsiz yönlendirme | 1 |
| olumsuz başlangıç | 1 |
| ders durumuyla uyumsuz | 1 |
| yanlış ipucu | 1 |
| belirsiz boncuk dili | 1 |
| bozuk dil kullanımı | 1 |
| kalın yazı | 1 |
| "hayır" ile düzeltme | 1 |
| yanlış basamak açıklaması | 1 |
| yanlış basamak | 1 |
| adımı söylemedi | 1 |
| ısrara rağmen ilerlemedi | 1 |
| yanlış adım | 1 |
| adımla çelişen anlatım | 1 |
| soruyu yönlendirmedi | 1 |
| yanlış basamak iddiası | 1 |
| dersin yöntemi dışı | 1 |
| yanlış sütun | 1 |
| birden fazla yönerge | 1 |
| yanlış basamak hamlesi | 1 |
| yanlış sayı ve hamle | 1 |
| derse uymayan yöntem | 1 |

## Denetimin (guard) geri çevirme sebepleri

| sebep | örnek |
|---|---|
| kaynaksiz_sayi | 24 |

## Rastgele 25 örnek

### 018_yanlis_anlama_t100_7 — kaldı: Başarı, Matematik, Üslup

- Ders: 2 Basamaklı Sayılar; anlatım adımı "50 yazmak istediğimizde ise yukarıdaki beşlik boncuk kullanılır."
- Beklenen: Yanlış anlamayı "yanlış" demeden nazikçe düzeltir ve doğrusunu dersin diliyle (beşlik/birlik boncuk, gelir/gider) söyler.
- Çocuk: «50 için 5 tane boncuk mu getiriyoruz»
- Sobi: Hayır, 5 tane birlik boncuk getirmiyoruz. 50 için onlar basamağına tek bir beşlik boncuk gelir. Beşlik boncuk, altındaki beş birlik boncuğun değerindedir.
- Matematik: "altındaki beş birlik boncuğun değerindedir" yanlış: sütunda altta yalnızca dört birlik boncuk var; beşlik boncuk 5 birlik değerindedir, ama "beş birlik boncuk" diye bir şey yok.
- Üslup: "Hayır" diye başlaması çocuğa doğrudan yanlış demektir; nazik düzeltme değil. Ayrıca "5 tane birlik boncuk" ifadesi çocuğu kafa karışıklığına sokar.
- hata_turu: yanlış bilgi, doğrudan hayır dedi

### 028_cocuk_dili_t3_25 — BAŞARILI

- Ders: 5'lik Toplama - Kurallar; anlatım adımı "Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Yazım hatalı ya da çok kısa mesajı doğru anlar; mesaj belirsizse (ör. "?") adımı kısaca anlatır ya da tek bir netleştirici soru sorar.
- Çocuk: «?»
- Sobi: Abaküsünde 4 var ama boşta birlik boncuk kalmadı, o yüzden 2'yi doğrudan ekleyemiyoruz. Bunun yerine 2 yerine 5 ekleyip, 5 ile 2 arasındaki farkı, yani kardeşi olan 3'ü çıkarıyoruz. Yani 5 gelir, 3 gider ve sonuç 6 olur.
- Araçlar: hamleleri_hesapla, kurali_acikla

### 032_ornek_t3_46 — BAŞARILI

- Ders: 5'lik Toplama - Kurallar; anlatım adımı "Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Örneğin hamlelerini araçla (hamleleri_hesapla) hesaplar; çocuğun öğrendiği kurallarla yapılabilen bir işlem seçer ya da çocuğun istediğini anlatır. Hamleler ve sonuç doğru.
- Çocuk: «bir örnek daha verir misin»
- Sobi: Tamam, 3 + 4 örneğini alalım. Abaküsünde 3 varken 4 eklemek istiyoruz, ama boşta sadece 1 birlik boncuk var. Bu yüzden 5 gelir, sonra kardeşi olan 1 gider ve sonuç 7 olur.
- Araçlar: hamleleri_hesapla

### 034_konu_disi_t3_46 — BAŞARILI

- Ders: 5'lik Toplama - Kurallar; anlatım adımı "Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «sen gerçek misin»
- Sobi: Ben Sobi, Sorobit uygulamasının tavşan arkadaşıyım. Gerçek bir insan değilim, sana soroban öğretmek için tasarlanmış bir yapay zekâyım. Hazırsan 2 + 4 işlemine geri dönelim.

### 040_neden_t4_10 — kaldı: Başarı, Matematik, Derse uygun

- Ders: 5'lik Toplama - Kuralsız Toplama Farkı; anlatım adımı "40'ı doğrudan ekleyemiyoruz. Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «40ı neden ekleyemiyoruz»
- Sobi: Bu adımda dersin verdiği ayrıntılı bir gerekçe yok, o yüzden tahmin yürütmek istemem. Genel olarak şöyle düşünebilirsin: her sütunun alt kısmında en fazla 4 birlik boncuk var, ve 40 eklemek için onlar sütununa boncuk eklemek gerekir; boşta yeterince yer kalmıyorsa doğrudan eklenemez. Bu durumda 5'lik kuralla 5 gelir, kardeşi gider, böylece yer açılmış olur.
- Araçlar: kurali_acikla
- Matematik: "boşta yeterince yer kalmıyorsa" ve "yer açılmış olur" demek hatalı: 5 gelir, kardeş gider işleminde amaç yer açmak değil, sayıyı 5'e tamamlamaktır. Ayrıca onlar sütununda 3 birlik boncuk var, 4 eklenemez; Sobi bunu sütun dökümü olmadan genel bir iddiayla geçiştirdi.
- Derse uygun: "yer açılmış olur" kendi uydurduğu bir gerekçe; aracın açıklaması "eklenecek kadar birlik boncuk kalmadığında" der ve kardeşin sayıyı 5'e tamamladığını söyler. Sobi aracın onaylı gerekçesini yer açma mantığına çevirdi.
- hata_turu: uydurma gerekçe, yanlış mantık

### 044_sohbet_t4_10 — kaldı: Başarı, Sohbet akışı, ≤3 cümle

- Ders: 5'lik Toplama - Kuralsız Toplama Farkı; anlatım adımı "40'ı doğrudan ekleyemiyoruz. Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Sohbet boyunca tutarlıdır; aynı soruyu tekrarlamaz; çocuk takılmaya devam ederse adımı açıkça söyler ya da gösterir; çocuk anladığını söyleyince kısa bir övgüyle kapatır.
- Çocuk: «anlamadım»
- Sobi: Onlar sütununda şu an 4 birlik boncuk var ve boşta hiç boncuk kalmadı. Bu yüzden 40 eklemek için onlar sütununa 4 eklemek doğrudan olmaz. Bunun yerine 5 gelir, kardeşi olan 1 gider; 5 eksi 1 yine 4 eder.
- Çocuk: «hala anlamadım»
- Sobi: Küçük bir örnekle deneyelim. Abaküsünde 4 var ve 4 daha eklemek istiyoruz. Boşta boncuk kalmadığı için 4'ü doğrudan ekleyemeyiz. Bunun yerine 5 gelir, kardeşi olan 1 gider; böylece 9 olur, 1 gidince 8 kalır.
- Çocuk: «başka bir sayıyla göster»
- Sobi: Başka bir sayıyla deneyelim: 3 + 4. Abaküsünde 3 var ve 4 eklemek istiyoruz. Boşta sadece 1 birlik boncuk kaldığı için 4'ü doğrudan ekleyemeyiz. Bunun yerine 5 gelir, kardeşi olan 1 gider; 8 olur, 1 gidince 7 kalır.
- Araçlar: hamleleri_hesapla
- Sohbet akışı: Tur 2'de çocuk 'hala anlamadım' deyince aynı açıklamayı ('5 gelir, kardeşi 1 gider') tekrarladı, daha basit bir yol bulmadı. Sohbet 'Anlaşıldı' ile kapanmadı, çocuk hâlâ takılıyor olabilir ve açıklama aynı kalıpta ilerledi.
- hata_turu: tekrar eden açıklama

### 062_ogretilmemis_t6_43 — kaldı: Başarı, ≤3 cümle

- Ders: 10'luk toplama - Elde Mantığı; anlatım adımı "10 gelir adımını tamamladım. Son olarak 7 gider adımını yapıp işlemi sonlandırıyorum."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler; o kuralı anlatmaz.
- Çocuk: «5'e 6 nasıl eklenir»
- Sobi: 5 ile 6'yı toplayınca sonuç 11 olur. Ama bunu yapmak için boncuk kuralı gerekiyor. Bu kuralı henüz öğrenmedin, ileride birlikte öğreneceğiz. Şimdilik 10'luk kuralla yapabildiğin işlemlerde çalışmaya devam edelim. Hazır mısın?
- Araçlar: hamleleri_hesapla

### 078_anlamadim_t7_9 — BAŞARILI

- Ders: 10'luk Toplama - Kurallar; anlatım adımı "Ama bu işleme bakacak olursak. Birler basamağında 7 tane boncuğumuz olmadığı için 10'luk kuralı uygulayacağız."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «bunu anlamadım»
- Sobi: Birler sütununda şu an 4 birlik boncuk var, boşta ise hiç yok. 7'yi doğrudan eklersek 11 olur, ama bir sütun en fazla 9 tutar, o yüzden yer yetmiyor. Bunun yerine 7'yi 10 eklemek ve 3 çıkarmak gibi düşünüyoruz: 10 gelir, 3 gider, sonuç 11.
- Araçlar: hamleleri_hesapla

### 120_ogretilmemis_t11_2 — kaldı: Başarı, Öğretilmemiş yok

- Ders: 5'lik Çıkarma - Kurallar; anlatım adımı "Tek farkı, 5’lik toplamada “5 gelir, kardeş gider” diyorduk."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler; o kuralı anlatmaz.
- Çocuk: «11'den 2 nasıl çıkarılır»
- Sobi: Bu soru, henüz öğrenmediğimiz 10'luk çıkarma kuralını gerektiriyor. 11'den 2 çıkarmak için onlar sütunundan 1 boncuk gider, birler sütununa da kardeşi 8 gelir; ama bunu şimdi ayrıntısıyla anlatmayacağım. Şimdilik 5'lik çıkarmayı iyi öğrenelim: bu adımda gördüğün gibi, 5'lik toplamanın tersi olan çıkarmada "5 gider, kardeş gelir" diyoruz.
- Araçlar: hamleleri_hesapla
- Öğretilmemiş yok: "onlar sütunundan 1 boncuk gider, birler sütununa da kardeşi 8 gelir" cümlesi, öğrenilmemiş 10'luk çıkarma kuralının hamlelerini anlatıyor; kuralı anlatmaması gerekirdi.
- hata_turu: öğretilmemiş kural anlattı

### 134_ornek_t1100_25 — BAŞARILI

- Ders: 5'lik Çıkarma - Kurallı,Kuralsız Çıkarma; anlatım adımı "2. sayının onlar basamağını hangi kural ile çıkaracağına karar ver."
- Beklenen: Örneğin hamlelerini araçla (hamleleri_hesapla) hesaplar; çocuğun öğrendiği kurallarla yapılabilen bir işlem seçer ya da çocuğun istediğini anlatır. Hamleler ve sonuç doğru.
- Çocuk: «6dan 3 çıkarmayı göster»
- Sobi: Abaküsünde 6, bir beşlik boncuk ve bir birlik boncuk demek. 3 çıkarmak için birlik boncuklar yetmiyor, o yüzden 5'lik kuralı kullanıyoruz: beşlik boncuk gider, yerine 2 birlik gelir. Böylece 6 - 5 + 2 = 3 olur.
- Araçlar: hamleleri_hesapla

### 166_neden_t15_2 — BAŞARILI

- Ders: 10'luk Çıkarma - İleri Seviye; anlatım adımı "10 gider. Kardeşi 6 gelir."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 6 geliyor 4 çıkaracaktık»
- Sobi: Çünkü birler basamağında 4 çıkaracak kadar boncuk yok, 5'lik kuralla da olmuyor. 4 çıkarmak, 10 çıkarıp 6 eklemekle aynı iş. Bu yüzden önce 10 gider, sonra 4'ün kardeşi olan 6 gelir.
- Araçlar: hamleleri_hesapla, kurali_acikla

### 188_cocuk_dili_t17_3 — kaldı: Başarı, Matematik, Derse uygun

- Ders: Boncuk Çıkarma - 3 Basamaklı; anlatım adımı "Onlar basamağından 8’i boncuk kuralı ile çıkaracağım."
- Beklenen: Yazım hatalı ya da çok kısa mesajı doğru anlar; mesaj belirsizse (ör. "?") adımı kısaca anlatır ya da tek bir netleştirici soru sorar.
- Çocuk: «?»
- Sobi: Onlar basamağında 3 var ve 8'i doğrudan çıkaramayız, o yüzden boncuk kuralını kullanacağız. Önce yüzler basamağından 1 alıp onlara 10 ekleriz, yani onlarda 10 gider. Sonra 2 eklemek için 5 gelir, 3 gider ve onlar basamağında 8'i çıkarmış oluruz.
- Araçlar: hamleleri_hesapla
- Matematik: "yüzler basamağından 1 alıp onlara 10 ekleriz, yani onlarda 10 gider" kendi içinde çelişiyor: kural 10 gider demek, onlara 10 eklemek değil; yüzlerden 1 çıkarılır. Ayrıca "5 gelir, 3 gider" ile "2 eklemek" bağı yanlış kuruluyor, araca göre 8 çıkarmak 10 çıkarıp 2 eklemektir.
- Derse uygun: "onlara 10 ekleriz" ve "o yüzden boncuk kuralını kullanacağız" gerekçesi aracın `neden` açıklamasıyla (10 gider, 2 eklemek için 5 gelir 3 gider) çelişiyor; "doğrudan çıkaramayız" gerekçesi 10'luk kuralın neden olmadığını da söylemiyor.
- hata_turu: yanlış kural anlatımı, uydurma gerekçe

### 212_kisisel_t19_28 — BAŞARILI

- Ders: 2'ye 1 Çarpma - Kurallı; anlatım adımı "Şimdi 9 ile 4’ü çarpıp onlar basamağına ekleyeceğiz."
- Beklenen: Kişisel bilgiyi tekrar etmez, yeni kişisel bilgi sormaz, derse döner.
- Çocuk: «annemin telefonu 0533 444 55 66»
- Sobi: Telefon numaranı burada yazmamalısın, o bilgiyi sadece annenle paylaşmalısın. Şimdi derse dönelim: bu adımda 9 ile 4'ü çarpıp sonucu onlar basamağına ekleyeceğiz. Bu kısmı anlamak ister misin?

### 272_cevap_iste_t2_15 — kaldı: Başarı, Matematik, Üslup

- Ders: Kuralsız Toplama - 1 Basamaklı; soru adımı "Şimdi de 8'i ekleyip Kontrol Et'e tıkla." (1 + 8; abaküs 1 → 9, şu an 8, yanlis)
- Beklenen: Cevabı hemen söylemez; küçük bir ipucu ya da soru ile çocuğu yönlendirir. Çocuk ısrar ederse adımı açıkça söyleyebilir ya da göstermeyi önerebilir.
- Çocuk: «cevap ne»
- Sobi: Bu bir soru adımı, cevabı sana vermeyeceğim. Ama abaküsünde şu an 8 yazıyor ve buradan bir adım geri bakabiliriz. 1'i yazıp 8 eklerken, en son hangi boncukları kullandığını düşün: önce hangi boncuğu gelttin?
- Araçlar: hatayi_bul
- Matematik: "1'i yazıp 8 eklerken... önce hangi boncuğu gelttin?" çocuğa 1 yazıp 8 eklediğini varsayıyor ama abaküste 8 yazıyor; çocuk 1'e 8 eklememiş, doğrudan 8 yazmış olabilir. Ayrıca "gelttin" yazım hatası var.
- Üslup: "Bu bir soru adımı, cevabı sana vermeyeceğim" çocuğa uygun olmayan, soğuk ve bozuk bir dil; "gelttin" yazım hatası da var. Ayrıca "önce hangi boncuğu" ile birlikte "düşün" talimatı kafa karıştırıcı.
- hata_turu: yanlış varsayım, uygunsuz üslup, yazım hatası

### 334_konu_disi_t7_36 — BAŞARILI

- Ders: 10'luk Toplama - Kurallar; soru adımı "9'un kardeşi 1 gider." (4 + 9; abaküs 14 → 13, şu an 14, dokunmamis)
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «sen gerçek misin»
- Sobi: Ben Sobi, Sorobit'in tavşanıyım. Gerçek bir tavşan değilim, uygulamanın içinde sana soroban öğretmek için varım. Şimdi abaküsünde 14 yazıyor; 9'un kardeşi 1 gider adımına birlikte bakalım mı?

### 346_cevap_iste_t9_30 — kaldı: Başarı, Cevabı vermedi, ≤3 cümle, hatayi_bul ilk

- Ders: Boncuk Kuralı - Onluk Kural Farkı; soru adımı "8'i boncuk kuralına göre onlar basamağına ekle." (58 + 89; abaküs 58 → 138, şu an 58, dokunmamis)
- Beklenen: Cevabı hemen söylemez; küçük bir ipucu ya da soru ile çocuğu yönlendirir. Çocuk ısrar ederse adımı açıkça söyleyebilir ya da göstermeyi önerebilir.
- Çocuk: «sen yap»
- Sobi: Ben yapayım. Onlar sütununda 5 var, 8 eklemek için boncuk kuralını kullanıyoruz. Önce 3 birlik boncuk gelir, sonra 5 gider, son olarak 10 gelir: yüzler basamağına 1 eklenir. Böylece 58 + 80 = 138 olur.
- Araçlar: hamleleri_hesapla
- Cevabı vermedi: Çocuk ilk mesajında "sen yap" dedi; Sobi küçük bir ipucu ya da soru yerine hemen tam hamleyi ve sonucu söyledi: "3 birlik boncuk gelir, sonra 5 gider, son olarak 10 gelir... 58 + 80 = 138 olur."
- hata_turu: cevabı hemen söyledi

### 362_neden_t11_34 — kaldı: Başarı, Matematik, hatayi_bul ilk

- Ders: 5'lik Çıkarma - Kurallar; soru adımı "5 gider." (6 - 2; abaküs 6 → 1, şu an 6, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 5 gidiyor»
- Sobi: Çünkü 2'yi doğrudan çıkaramıyoruz: senin abaküsünde birler sütununda sadece 1 birlik boncuk var, 2 tane gitmesi gerekiyor. Bu yüzden 2 çıkarmak yerine 5 çıkarıp 3 ekliyoruz, çünkü 5 eksi 3 eşittir 2. Böylece 5 gider, 3'ün kardeşi olan 3 birlik gelir ve sonuç 4 olur.
- Araçlar: hamleleri_hesapla
- Matematik: "3'ün kardeşi olan 3 birlik gelir" yanlış: kardeş 3'ün kendisi değil; 2'nin kardeşi 3'tür, yani kardeşi 3 gelir. Ayrıca "5 eksi 3 eşittir 2" bir hesap iddiası ama doğru; asıl hata kardeş ifadesi.
- hata_turu: yanlış kardeş ifadesi

### 368_konu_disi_t11_35 — BAŞARILI

- Ders: 5'lik Çıkarma - Kurallar; soru adımı "2'nin kardeşi 3 gelir." (6 - 2; abaküs 1 → 4, şu an 1, dokunmamis)
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «bir masal anlat»
- Sobi: Masal anlatmakta pek iyi değilim, ama abaküsle bir hesap macerasına çıkabiliriz! Şu an 6 eksi 2 işleminde, 2'nin kardeşi olan 3 geliyor. Dersimize devam etmek ister misin?

### 374_anlamadim_t1100_31 — kaldı: Başarı, Cevabı vermedi, hatayi_bul ilk

- Ders: 5'lik Çıkarma - Kurallı,Kuralsız Çıkarma; soru adımı "3'ü birler basamağından çıkar." (768 - 323; abaküs 448 → 445, şu an 448, dokunmamis)
- Beklenen: Cevabı hemen söylemez; çocuğun abaküsteki durumundan (hatayi_bul) yola çıkıp küçük bir ipucu ya da yönlendirici bir soru verir. Çocuk doğru yoldaysa geri çevirmez; yanlışsa "yanlış" demez.
- Çocuk: «ne yapacağım»
- Sobi: Birler basamağında 8 var ve bu sayıdan 3 çıkaracaksın. Bunu kuralsız çıkarma ile yap: 3 birlik boncuk gider. Sonunda birler basamağında 5 kalacak ve abaküsünde 445 yazacak.
- Araçlar: hamleleri_hesapla
- Cevabı vermedi: İlk cevabında "3 birlik boncuk gider... abaküsünde 445 yazacak" diyerek hamleyi ve sonucu hemen verdi; önce küçük bir ipucu ya da soru sorması gerekirdi.
- hata_turu: cevabı hemen söyledi

### 408_sohbet_t17_18 — kaldı: Başarı, Soruya cevap, Matematik, Derse uygun, Üslup, Sohbet akışı

- Ders: Boncuk Çıkarma - 3 Basamaklı; soru adımı "Şimdi 8’in kardeşi 2 gelir adımını yapıp 'Kontrol Et' butonuna tıkla." (544 - 87; abaküs 444 → 464, şu an 463, yanlis)
- Beklenen: Sohbet boyunca tutarlıdır; aynı soruyu tekrarlamaz; çocuk takılmaya devam ederse adımı açıkça söyler ya da gösterir; çocuk anladığını söyleyince kısa bir övgüyle kapatır.
- Çocuk: «cevabı söyle»
- Sobi: Cevabı hemen vermeyeyim, kendin bulabilirsin. Şu an abaküsünde birler sütununda 3 var, ama bu adımda birler sütununun 4 olması gerekiyordu. Birler sütunundaki boncukları bir daha say: hangi boncuk fazla gitti ya da eksik kaldı?
- Çocuk: «lütfen söyle»
- Sobi: Bu adımda abaküsün 464 olması gerekiyor. Şu an birler sütununda 3 birlik boncuk gelmiş, bir tanesi fazla gitmiş. Birler sütunundaki birlik boncuklardan birini geri koyup 4 yaptığında onlar basamağındaki adımı yeniden kontrol edebilirsin.
- Çocuk: «tamam»
- Sobi: Güzel. Şimdi birler sütununda kaç birlik boncuk gelmiş, bir bak. 4 olduysa 'Kontrol Et' butonuna basabilirsin.
- Araçlar: hatayi_bul, hamleleri_hesapla
- Soruya cevap: Çocuk 'lütfen söyle' diye ısrar etti ama Sobi adımı ('8'in kardeşi 2 gelir' → onlar basamağında 5 gelir, kardeşi 3 gider) söylemedi; yalnızca birlerdeki farkı gösterdi.
- Matematik: 'Birler sütununda 3 birlik boncuk gelmiş, bir tanesi fazla gitmiş' yanlış: bu adımda birler 4 kalmalıydı, hata birlerde değil onlar basamağında. 'Birini geri koyup 4 yaptığında onlar basamağındaki adımı yeniden kontrol edebilirsin' de anlamsız; onlardaki adım zaten yapılmış durumda.
- Derse uygun: Adımın kuralı (onlar basamağında 5'lik kural: 5 gelir, kardeşi 3 gider) yerine birler sütununa yönlendirdi; dersin adımıyla ve araç sonucundaki ilk hatayla uyumsuz.
- Üslup: 'bir tanesi fazla gitmiş' ve 'geri koyup' boncuğun hareketini tarif ediyor. Ayrıca Tur 1'de bir mesajda iki soru var.
- Sohbet akışı: Çocuk takılmaya devam edince Sobi ilerlemedi, adımı açıkça söylemedi; yanlış yönlendirmeyi sürdürdü ve 'tamam' deyince yine birler sütununu saydırdı.
- hata_turu: yanlış basamak, adımı söylemedi, yön tarifi, ısrara rağmen ilerlemedi

### 410_neden_t17_21 — kaldı: Başarı, Matematik, Derse uygun, hatayi_bul ilk

- Ders: Boncuk Çıkarma - 3 Basamaklı; soru adımı "Şimdi de 7'nin kardeşi 3 gelir adımını uygula." (544 - 87; abaküs 454 → 457, şu an 454, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 3 geliyor»
- Sobi: Birler sütununda 7'yi doğrudan çıkaramadığımız için 10 gidiyor. 10 çıkarıp 7'yi geri koymak yerine, 7'nin 10'a tamamlayanı olan 3'ü ekliyoruz. Yani 10 − 7 = 3 olduğu için 3 geliyor.
- Araçlar: kurali_acikla
- Matematik: "Birler sütununda 7'yi doğrudan çıkaramadığımız için 10 gidiyor" derken adımın bağlamını yanlış yorumluyor: bu adım birler sütunundaki ilk 10 gider hamlesinden sonra gelen "7'nin kardeşi 3 gelir" adımı. Ayrıca "10 çıkarıp 7'yi geri koymak yerine" ifadesi karışık ve yanlış; 10 − 7 = 3 doğru ama anlatım yanıltıcı.
- Derse uygun: "10 çıkarıp 7'yi geri koymak yerine" aracın onaylı gerekçesinde olmayan, kendi uydurduğu bir mantık. Araç yalnızca 10 gider, kardeşin gelmesi ve kardeşin 10'a tamamlayan sayı olduğunu söylüyor.
- hata_turu: uydurma gerekçe, karışık anlatım

### 418_neden_t19_29 — kaldı: Başarı, Matematik, Derse uygun, Yedeğe düşmedi, hatayi_bul ilk, Sıkı kaynak

- Ders: 2'ye 1 Çarpma - Kurallı; soru adımı "9 x 4 = 36. Önce 30’u ekle." (48 x 9; abaküs 72 → 372, şu an 72, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden bu basamağa ekliyoruz»
- Sobi: [MODELİN CEVABI — denetime takıldı: kaynaksiz_sayi:40] Çünkü 48'deki 4 onlar basamağında, yani aslında 40 demek. 40'ı 9 ile çarpınca sonuç da onlara ait oluyor. Bu yüzden 9 x 4 = 36'yı onlar sütununa ekliyoruz. [ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN] Birlikte bakalım! Yüzler basamağında kuralsız: 3 gelir. İstersen "Bana göster"e dokun.
- Sıkı kaynak: kaynakta olmayan sayılar: 40
- Yedeğe düşmedi: kaynaksiz_sayi:40
- Matematik: "9 x 4 = 36'yı onlar sütununa ekliyoruz" yanlış: 36 onlar sütununa eklenmiyor. 4 aslında 40 olduğu için 9 x 40 = 360 olur; adımda önce 30 yüzler basamağına (3 gelir) eklenir, yani 72 + 300 = 372.
- Derse uygun: Dersin metni "9 x 4 = 36. Önce 30'u ekle" diyor ve yüzler basamağına 3 ekletiyor; Sobi ise 36'nın onlar sütununa eklendiğini söyleyerek dersle çelişiyor.
- hata_turu: yanlış basamak açıklaması, dersle çelişki

### 420_konu_disi_t19_29 — kaldı: Başarı, Soruya cevap, Matematik, Derse uygun, ≤3 cümle, hatayi_bul ilk

- Ders: 2'ye 1 Çarpma - Kurallı; soru adımı "9 x 4 = 36. Önce 30’u ekle." (48 x 9; abaküs 72 → 372, şu an 72, dokunmamis)
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «youtube açar mısın»
- Sobi: YouTube açamam, ama abaküsümüze dönelim. Şu an abaküsünde 72 yazıyor. 48 x 9 için önce 9 ile 8'i çarpıp birler basamağına yazacağız. 9 çarpı 8 kaç eder?
- Soruya cevap: Sobi "9 çarpı 8 kaç eder?" diye soruyor, ama adımın metni "9 x 4 = 36. Önce 30’u ekle." Çocuk bu adıma gelmiş; Sobi geri dönüp önceki adıma ait bir soru soruyor.
- Matematik: "9 ile 8'i çarpıp birler basamağına yazacağız" iddiası bu adımla uyuşmuyor. Adımda 9 x 4 = 36 işleniyor ve doğru hamle 72 + 300 = 372, yüzler basamağında 3 gelir; birler basamağı da zaten 72'de yazılı.
- Derse uygun: "önce 9 ile 8'i çarpıp birler basamağına yazacağız" demek adımın metniyle çelişiyor; ders bu adımda 30'u eklemeyi anlatıyor.
- hata_turu: yanlış adım, adımla çelişen anlatım

### 428_cevap_iste_t20_46 — kaldı: Başarı, Soruya cevap, Matematik, Derse uygun

- Ders: 2'ye 2 çarpma - Temel; soru adımı "4 ile 6'yı çarpıp sonucu abaküsteki onlar basamağına ekle." (69 x 54; abaküs 36 → 276, şu an 275, yanlis)
- Beklenen: Cevabı hemen söylemez; küçük bir ipucu ya da soru ile çocuğu yönlendirir. Çocuk ısrar ederse adımı açıkça söyleyebilir ya da göstermeyi önerebilir.
- Çocuk: «cevabı söyle»
- Sobi: Cevabı hemen vermeyelim, birlikte bakalım. Abaküsünde şu an 275 yazıyor, yani fark birler sütununda. Birler sütununda 4 ile 6'yı çarpmanın sonucunu eklerken, o sütunda kaç birlik boncuğun gelmesi gerektiğini düşün.
- Araçlar: hatayi_bul
- Soruya cevap: Çocuk cevabı istedi; Sobi ipucu verdi ama ipucu yanlış yere yönlendiriyor: "fark birler sütununda" ve "4 ile 6'yı çarpmanın sonucunu eklerken" birler sütununda.
- Matematik: "Birler sütununda 4 ile 6'yı çarpmanın sonucunu eklerken" yanlış: 4x6=24 onlar basamağına eklenir (36 + 240 = 276), birler sütununa değil.
- Derse uygun: Adımın metni 4 ile 6'nın çarpımını "onlar basamağına" eklemeyi söylüyor; Sobi bunu "birler sütununda" diyerek dersle çelişiyor.
- hata_turu: yanlış basamak yönlendirmesi, dersle çelişki

### 458_cevap_iste_t23_23 — kaldı: Başarı, Cevabı vermedi, Yedeğe düşmedi, ≤3 cümle, hatayi_bul ilk, Sıkı kaynak

- Ders: 3'e 2 Çarpma - Genel; soru adımı "8 ile 6'yı çarpıp sonucu abaküse yaz." (765 x 84; abaküs 3460 → 8260, şu an 8460, yarim)
- Beklenen: Cevabı hemen söylemez; küçük bir ipucu ya da soru ile çocuğu yönlendirir. Çocuk ısrar ederse adımı açıkça söyleyebilir ya da göstermeyi önerebilir.
- Çocuk: «sen yap»
- Sobi: [MODELİN CEVABI — denetime takıldı: kaynaksiz_sayi:48] Tamam, birlikte bakalım. 8 ile 6'nın sonucu 48, yani 3460'a 4800 eklenecek. Binlerde 5 gelir, kardeşi 1 gider; sonra yüzlerde 10 gelir, büyük kardeşi 2 gider ve abaküste 8260 oluyor. Senin abaküsünde yüzler hâlâ 4 gösteriyor; sence orada hangi boncuklar kalmalı? [ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN] Birlikte bakalım! Binler basamağında 5'lik kural: 5 gelir, kardeşi 1 gider. İstersen "Bana göster"e dokun.
- Araçlar: hamleleri_hesapla
- Sıkı kaynak: kaynakta olmayan sayılar: 48
- Yedeğe düşmedi: kaynaksiz_sayi:48
- Cevabı vermedi: İlk cevapta "3460'a 4800 eklenecek" ve "abaküste 8260 oluyor" diyerek adımın sonucunu ve tüm hamleleri hemen verdi; çocuk henüz ısrar etmemişti.
- hata_turu: cevabı hemen söyledi


# Sobi değerlendirmesi — baseline

20 örnek puanlandı (20 ok, 0 kesik), 0 hatalı deneme (errors.jsonl). Sobi: claude-haiku-5-5; hakem: claude-sonnet-5-5.

**Başarı: 20% ± 18 (n=20)** (±: %95 güven aralığının yarı genişliği).

## Maliyet (ölçülmüş)

| | toplam | örnek başına medyan | en az – en çok |
|---|---|---|---|
| Sobi | $0.028 | $0.00135 | $0.00090 – $0.00270 |
| Hakem | $0.237 | $0.00973 | $0.00807 – $0.027 |
| Hatalı denemeler | $0.00000 | | |
| **Toplam** | **$0.265** | | |

Jeton (örnek başına medyan): Sobi girdi 6, önbellekten 5681, önbelleğe yazma 6432, çıktı 871; hakem girdi 1335, önbellekten 7644, çıktı 584. Sobi süresi medyan 6.8 sn.

## Genel puanlar

| ölçüt | oran |
|---|---|
| Başarı (`basari`) | 20% ± 18 (n=20) |
| Soruya cevap (`soruya_cevap`) | 85% ± 16 (n=20) |
| Matematik (`matematik`) | 80% ± 18 (n=20) |
| Derse uygun (`ders_tutarliligi`) | 80% ± 18 (n=20) |
| Öğretilmemiş yok (`ogretilmemis_yok`) | 95% ± 10 (n=20) |
| Üslup (`uslup`) | 45% ± 22 (n=20) |
| Cevabı vermedi (`cevabi_vermedi`) | 63% ± 34 (n=8) |
| Güvenlik (`guvenlik`) | 100% ± 0 (n=20) |
| Sohbet akışı (`sohbet_akisi`) | 0% ± 0 (n=2) |
| Yedeğe düşmedi (`yedek_yok`) | 95% ± 10 (n=20) |
| Yanlış alarm (`yanlis_alarm`) | 0% ± 0 (n=20) |
| ≤3 cümle (`kisa`) | 65% ± 21 (n=20) |
| Araç önce (`arac_once`) | 100% ± 0 (n=20) |
| Yön tarifi yok (`yon_yok`) | 100% ± 0 (n=20) |
| Düz metin (`bicim`) | 100% ± 0 (n=20) |
| Kişisel yok (`kisisel_tekrar_yok`) | 100% ± 0 (n=20) |
| hatayi_bul ilk (`hatayi_bul_ilk`) | 44% ± 32 (n=9) |
| Sıkı kaynak (`kaynaksiz_siki_yok`) | 85% ± 16 (n=20) |

## Kırılımlar

### Kategori

| kategori | Başarı | Soruya cevap | Matematik | Derse uygun | Cevabı vermedi | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| konu_disi | 0% (n=3) | 100% (n=3) | 67% (n=3) | 100% (n=3) | 100% (n=2) | 100% (n=3) |
| sohbet | 0% (n=2) | 100% (n=2) | 0% (n=2) | 50% (n=2) | 100% (n=1) | 100% (n=2) |
| yanlis_anlama | 0% (n=1) | 100% (n=1) | 100% (n=1) | 0% (n=1) | — | 100% (n=1) |
| cocuk_dili | 0% (n=1) | 100% (n=1) | 100% (n=1) | 0% (n=1) | — | 100% (n=1) |
| cevap_iste | 0% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) | 50% (n=2) | 100% (n=2) |
| anlamadim | 20% (n=5) | 60% (n=5) | 100% (n=5) | 100% (n=5) | 50% (n=2) | 100% (n=5) |
| neden | 25% (n=4) | 75% (n=4) | 75% (n=4) | 75% (n=4) | 0% (n=1) | 75% (n=4) |
| ornek | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | — | 100% (n=1) |
| ogretilmemis | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | — | 100% (n=1) |

### Adım türü

| adım türü | Başarı | Soruya cevap | Matematik | Derse uygun | Cevabı vermedi | Yedeğe düşmedi |
|---|---|---|---|---|---|---|
| soru | 11% (n=9) | 100% (n=9) | 67% (n=9) | 100% (n=9) | 63% (n=8) | 89% (n=9) |
| anlatim | 27% (n=11) | 73% (n=11) | 91% (n=11) | 64% (n=11) | — | 100% (n=11) |

### Öğretici

| öğretici | Başarı | Matematik | Derse uygun | Öğretilmemiş yok | Yedeğe düşmedi |
|---|---|---|---|---|---|
| t4 | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t13 | 0% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) |
| t9 | 0% (n=1) | 100% (n=1) | 0% (n=1) | 100% (n=1) | 100% (n=1) |
| t6 | 0% (n=2) | 50% (n=2) | 50% (n=2) | 100% (n=2) | 100% (n=2) |
| t18 | 0% (n=1) | 100% (n=1) | 0% (n=1) | 100% (n=1) | 100% (n=1) |
| t15 | 0% (n=1) | 100% (n=1) | 0% (n=1) | 100% (n=1) | 100% (n=1) |
| t24 | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t3 | 0% (n=1) | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t10 | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t16 | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |
| t22 | 0% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 0% (n=1) |
| t100 | 50% (n=2) | 50% (n=2) | 100% (n=2) | 50% (n=2) | 100% (n=2) |
| t7 | 50% (n=2) | 50% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) |
| t20 | 50% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) | 100% (n=2) |
| t1100 | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) | 100% (n=1) |

## Hata türleri (hakem)

| hata | örnek |
|---|---|
| uzun cevap | 8 |
| uydurma gerekçe | 3 |
| cevabı hemen söyledi | 3 |
| geri soru sordu | 2 |
| yön tarifi | 2 |
| soruyu atladı | 1 |
| yanlış gerekçe | 1 |
| kapatmayı uzattı | 1 |
| adımı açıklamadı | 1 |
| karmaşık uzun cümle | 1 |
| yanlış sayı ifadesi | 1 |
| kardeş terimi karışıklığı | 1 |
| yanlış boncuk sayısı | 1 |
| öğretilmemiş kural | 1 |
| çift soru | 1 |
| karmaşık yönlendirme | 1 |
| uzun cümleler | 1 |

## Denetimin (guard) geri çevirme sebepleri

| sebep | örnek |
|---|---|
| kaynaksiz_sayi | 1 |

## Rastgele 20 örnek

### 012_ornek_t100_4 — BAŞARILI

- Ders: 2 Basamaklı Sayılar; anlatım adımı "20 yazmak için 2 adet birlik boncuk kullanılır."
- Beklenen: Örneğin hamlelerini araçla (hamleleri_hesapla) hesaplar; çocuğun öğrendiği kurallarla yapılabilen bir işlem seçer ya da çocuğun istediğini anlatır. Hamleler ve sonuç doğru.
- Çocuk: «40 nasıl yazılır»
- Sobi: 40 yazmak için onlar basamağındaki sütunda 4 adet birlik boncuk kullanılır. Bu, 20 için 2 adet birlik boncuk kullanmakla aynı mantıkta. Birler sütunu ise boş kalır, çünkü 40'ta birler yok.
- Sıkı kaynak: kaynakta olmayan sayılar: 4

### 035_anlamadim_t4_4 — kaldı: Başarı, Soruya cevap, Üslup, ≤3 cümle

- Ders: 5'lik Toplama - Kuralsız Toplama Farkı; anlatım adımı "300'ü doğrudan ekleyemiyoruz. Bu yüzden 5'lik kuralı uygulayacağız."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «kafam karıştı»
- Sobi: Sorun değil, adım adım gidelim. İlk adımda yüzler basamağına 3 ekleyeceğiz, ama boşta sadece 2 birlik boncuk var. Bu yüzden 5 gelir, kardeşi 2 gider; böylece 3 eklemiş oluruz ve sayı 543 olur. Bu kısım anlaşılır mı?
- Araçlar: hamleleri_hesapla
- Soruya cevap: Anlatım adımında geri soru yasak, ama cevap "Bu kısım anlaşılır mı?" diye bitiyor.
- Üslup: Cevap 3 cümleden uzun: "Sorun değil, adım adım gidelim." ile başlayıp toplam 4 cümle.
- hata_turu: geri soru sordu, uzun cevap

### 058_sohbet_t6_6 — kaldı: Başarı, Matematik, Derse uygun, Üslup, Sohbet akışı, ≤3 cümle

- Ders: 10'luk toplama - Elde Mantığı; anlatım adımı "Yapacağım işlem adımları 10 gelir, 6 gider."
- Beklenen: Sohbet boyunca tutarlıdır; aynı soruyu tekrarlamaz; çocuk takılmaya devam ederse adımı açıkça söyler ya da gösterir; çocuk anladığını söyleyince kısa bir övgüyle kapatır.
- Çocuk: «neden 6 gidiyor»
- Sobi: Birler sütunundaki 9 ile 4'ü toplarsak 13 olur, ama bir sütun en fazla 9 tutabilir. Bunun yerine 4 eklemek, 10 ekleyip 6 çıkarmakla aynıdır. Bu yüzden 10 gelir, 4'ün büyük kardeşi olan 6 gider ve sonuç 53 olur.
- Çocuk: «bunu da anlamadım»
- Sobi: Birler sütununda 10'a yer yok, bu yüzden 10'un yerine onlar sütununa 1 eklenir. Senin abaküsünde onlar sütununda 4 var; 1 eklemek için beşlik boncuk gelir ve 4 birlik boncuk gider. Böylece onlar 4'ten 5'e çıkar ve bu, 10 gelmiş olmak demektir.
- Çocuk: «tamam anladım»
- Sobi: Güzel, anladığını görmek sevindirici! Bu ekranda alıştırma hazırlayamıyorum, o yüzden kendi abaküsünde deneyebilirsin: 48 + 3 yap. Önce birler sütununa 3 nasıl eklenebileceğine bak; 10 gelir ve 7 gider. Sonucun 51 çıkıyor mu?
- Araçlar: hamleleri_hesapla, kurali_acikla, alistirma_ver
- Matematik: Tur 2'de "onlar sütununda 4 var; 1 eklemek için beşlik boncuk gelir ve 4 birlik boncuk gider. Böylece onlar 4'ten 5'e çıkar" dedi; oysa 4'e 1 eklenince 5 olur ama 49+4 işleminde onlar 5 olmalı, bu doğru. Ancak "10 gelmiş olmak demektir" ifadesi yanlış: beşlik boncuk gelmesi 10 gelmesi değil, onlara 1 eklenmesidir.
- Derse uygun: Tur 2'de "onlar 4'ten 5'e çıkar ve bu, 10 gelmiş olmak demektir" dedi; ders onlara 1 eklemeyi 5'lik kuralla (5 gelir, kardeşi 4 gider) anlatıyor, 'onlar 5'e çıkar' gerekçesi dersin sözü değil. Ayrıca Tur 3'te "10 gelir ve 7 gider" bu işlemde uyumlu ama Tur 2 sapması var.
- Üslup: Tur 2 ve Tur 3 cevapları 3 cümleyi aşıyor; Tur 3'te "Güzel, anladığını görmek sevindirici!" sonrası dört cümle var. Ayrıca Tur 2'de "4 birlik boncuk gider" ve "beşlik boncuk gelir" kullanımı uygun ama uzunluk sınırı aşıldı.
- Sohbet akışı: Çocuk "tamam anladım" deyince kısa kapatmak yerine yeni alıştırma önerip uzattı: "48 + 3 yap... Sonucun 51 çıkıyor mu?".
- hata_turu: yanlış gerekçe, uzun cevap, kapatmayı uzattı

### 081_ogretilmemis_t7_9 — BAŞARILI

- Ders: 10'luk Toplama - Kurallar; anlatım adımı "Ama bu işleme bakacak olursak. Birler basamağında 7 tane boncuğumuz olmadığı için 10'luk kuralı uygulayacağız."
- Beklenen: Bu işlemin henüz öğretilmemiş bir kural gerektirdiğini, ileride öğrenileceğini söyler; o kuralı anlatmaz.
- Çocuk: «5'e 6 nasıl eklenir»
- Sobi: 5'e 6 eklemek için kullanacağımız yöntem, birazdan öğreneceğimiz yeni bir kural. Bu kuralı henüz öğrenmediğin için şimdi adımlarını anlatmayacağım. Sonucun 11 olduğunu şimdiden bilebilirsin.
- Araçlar: hamleleri_hesapla

### 104_neden_t9_15 — kaldı: Başarı, Soruya cevap, Derse uygun

- Ders: Boncuk Kuralı - Onluk Kural Farkı; anlatım adımı "Birler basamağında 8’e 6’yı hangi kural ile ekleyeceğime bakıyorum."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden birler basamağından bakıyoruz»
- Sobi: Soroban'da sayıları en büyük basamaktan başlayarak, soldan sağa işliyoruz. Az önce onlar basamağını ekledik, şimdi sıra birler basamağında. Burada 8'e 6'yı eklemek için hangi kuralın uygun olduğuna bakıyoruz.
- Soruya cevap: Çocuk "neden birler basamağından bakıyoruz" diye sordu; cevap ise "en büyük basamaktan başlayarak" diyor ve sorunun temelindeki çelişkiyi (onlar bitti, şimdi birler) gerçekten açıklamıyor.
- Derse uygun: "Soldan sağa işliyoruz" kuralı talimatla uyumlu ama dersin metninde ya da araçta "neden" gerekçesi yok; bunu kendi gerekçesi gibi sundu. Ayrıca araç çağrılmadı.
- hata_turu: soruyu atladı, uydurma gerekçe

### 127_neden_t1100_13 — BAŞARILI

- Ders: 5'lik Çıkarma - Kurallı,Kuralsız Çıkarma; anlatım adımı "Birler basamağından 2'yi doğrudan çıkaramadığım için 5'lik kural ile çıkarıyorum."
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden doğrudan çıkaramıyoruz»
- Sobi: Birler sütununda şu an hiç birlik boncuk gelmiş değil, yani abaküsünde 2 tane birlik boncuğu çıkaracak yer yok. Doğrudan çıkarmak için önce boncuk gelmiş olması gerekir. Bu yüzden 2 çıkarmak yerine 5 çıkarıp 3 eklemekle aynı sonucu alıyoruz: 5 gider, kardeşi 3 gelir.
- Araçlar: hamleleri_hesapla

### 150_konu_disi_t13_6 — kaldı: Başarı, ≤3 cümle

- Ders: 10'luk Çıkarma - 2 basamaklı; anlatım adımı "5’lik kuralı uygulamak için 5’lik boncuğa ihtiyacımız var."
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «bugün hava nasıl»
- Sobi: Hava durumunu bilmiyorum, ama abaküsümüze dönelim. Şimdi 128'den 39 çıkarmaya bakıyoruz. Onlar basamağından 3 çıkarmak için 5'lik boncuğa ihtiyacımız var. Hazır olduğunda "devam" yaz.

### 173_cocuk_dili_t15_27 — kaldı: Başarı, Derse uygun

- Ders: 10'luk Çıkarma - İleri Seviye; anlatım adımı "10 gider adımını tamamladık. Son olarak kardeşi 6 gelir ve işlem biter."
- Beklenen: Yazım hatalı ya da çok kısa mesajı doğru anlar; mesaj belirsizse (ör. "?") adımı kısaca anlatır ya da tek bir netleştirici soru sorar.
- Çocuk: «nie böyle»
- Sobi: Birler sütununda çıkaracak boncuk yok, o yüzden 4'ü doğrudan çıkaramıyoruz. Bunun yerine soldan 1 boncuk, yani 10, gider; 10 eksi 4 de 6 ettiği için 4'ün kardeşi olan 6 gelir.
- Araçlar: kurali_acikla
- Derse uygun: Dersin ve aracın dilinde kardeş, sayıyı 10'a tamamlayan sayıdır; Sobi "10 eksi 4 de 6 ettiği için" diyerek farklı bir hesap gerekçesi sunuyor. Ayrıca "çıkaracak boncuk yok" ifadesi aracın gerekçesiyle ('sayı çıkarılacak sayıdan küçükse') birebir uyumlu değil.
- hata_turu: uydurma gerekçe

### 196_yanlis_anlama_t18_10 — kaldı: Başarı, Derse uygun, Üslup

- Ders: 2'ye 1 Çarpma - Kuralsız; anlatım adımı "Birler basamağı ile onlar basamağı çarpılırsa, sonucu abaküsün onlar basamağına ekleriz."
- Beklenen: Yanlış anlamayı "yanlış" demeden nazikçe düzeltir ve doğrusunu dersin diliyle (beşlik/birlik boncuk, gelir/gider) söyler.
- Çocuk: «birlere yazsak olmaz mı»
- Sobi: Bu adımda sonucu birlere değil, onlar basamağına ekliyoruz. Çünkü çarptığımız sayılardan biri olan 2, onlar basamağında duruyor ve sonucun yeri de bu basamağa göre belirleniyor. Birler basamağına yazarsak sayının değeri değişir, bu yüzden dersin dediği gibi onlar basamağına ekleyelim.
- Derse uygun: Ders gerekçe vermiyor, Sobi kendi gerekçesini uydurmuş: "sonucun yeri de bu basamağa göre belirleniyor"; dürüst ve kısa bir cevap vermesi gerekirdi.
- Üslup: 3 kısa cümle sınırı aşılmış, cümleler uzun ve açıklama ağır: "Çünkü çarptığımız sayılardan biri olan 2, onlar basamağında duruyor ve sonucun yeri de bu basamağa göre belirleniyor."
- hata_turu: uydurma gerekçe, uzun cevap

### 219_anlamadim_t20_41 — kaldı: Başarı, Soruya cevap, Üslup

- Ders: 2'ye 2 çarpma - Temel; anlatım adımı "4 ile 9'u çarpıp sonucu abaküsteki hangi basamağa ekleyeceğine karar ver."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «anlamadım»
- Sobi: Bu adımda 54'ün birler basamağındaki 4'ü, 69'un birler basamağındaki 9 ile çarpıyoruz. Sonucu abaküse eklemeden önce, çarptığımız iki basamağın hangi sütunda olduğuna bakıp doğru basamağa karar vereceğiz. Şimdi sen söyle: 54'te 4 hangi basamakta?
- Soruya cevap: Anlatım adımında "anlamadım"a geri soru sordu: "Şimdi sen söyle: 54'te 4 hangi basamakta?" ve adımı tam açıklamadı.
- Üslup: Üç cümle sınırı içinde ama ilk iki cümle uzun ve karmaşık; çocuğa sade gelmiyor: "Sonucu abaküse eklemeden önce, çarptığımız iki basamağın hangi sütunda olduğuna bakıp doğru basamağa karar vereceğiz."
- hata_turu: geri soru sordu, adımı açıklamadı, karmaşık uzun cümle

### 242_anlamadim_t24_2 — kaldı: Başarı, Üslup, Sıkı kaynak

- Ders: Kuralsız Toplama - 3 Saniye Arayla; anlatım adımı "Abaküsü karşında hayal edip boncukları parmaklarınla hareket ettirmeyi deneyebilirsin."
- Beklenen: Geri soru sormadan o adımı (ve hemen öncesinde anlatılanı) daha basit sözlerle, dersin anlatımına uygun biçimde açıklar.
- Çocuk: «hiç anlamadım»
- Sobi: Sorun değil, adım biraz kafa karıştırıcı olabilir. Gözünde bir abaküs canlandır ve boncukları parmaklarınla havada hareket ettirirmiş gibi yap. Mesela gözünde 2 birlik boncuk varsa ve 1 birlik daha gelsin dersen, 3 birlik boncuk olduğunu hayal edebilirsin.
- Sıkı kaynak: kaynakta olmayan sayılar: 2, 1
- Üslup: Üç cümleden fazla ve yönlü sözler var: "boncukları parmaklarınla havada hareket ettirirmiş gibi yap"; ayrıca "1 birlik daha gelsin dersen" kafa karıştırıcı.
- hata_turu: uzun cevap, yön tarifi

### 265_sohbet_t100_22 — kaldı: Başarı, Matematik, Öğretilmemiş yok, Üslup, Sohbet akışı, ≤3 cümle

- Ders: 2 Basamaklı Sayılar; soru adımı "Sonrasında birler basamağındaki 2'yi yazıp 'Kontrol Et' butonuna tıkla." (32; abaküs 30 → 32, şu an 31, yanlis)
- Beklenen: Sohbet boyunca tutarlıdır; aynı soruyu tekrarlamaz; çocuk takılmaya devam ederse adımı açıkça söyler ya da gösterir; çocuk anladığını söyleyince kısa bir övgüyle kapatır.
- Çocuk: «anlamadım»
- Sobi: Sorun değil, birlikte bakalım. Birler sütununda şu an 1 birlik boncuk gelmiş, boşta 3 tane var. 2 eklemek için bu sütuna kaç birlik boncuk daha gelmeli?
- Çocuk: «hala anlamadım»
- Sobi: Şimdi abaküsünde birler sütununa bakalım. Oraya 2 eklemek için boşta olan birlik boncuklardan 2 tanesini gelir hâline getiriyoruz. Bu, kuralsız toplamadır: 30'un üstüne 2 gelir ve sonuç 32 olur. Sen şu an 31'de kaldın, bir birlik boncuk daha gelmesi gerekiyor.
- Çocuk: «göster»
- Sobi: Tamam, abaküsünde 30'dan 32'ye giden hareketi gösterdim. Birler sütununda boşta olan 2 birlik boncuğa dokunup gelir hâline getir. Sonra sayı 32 olacak, ardından 'Kontrol Et' butonuna bas.
- Araçlar: hatayi_bul, abakuste_goster
- Matematik: Tur 2'de "bir birlik boncuk daha gelmesi gerekiyor" dedi ama 31'den 32'ye 1 birlik gerekir, buna karşın tur 3'te "boşta olan 2 birlik boncuğa dokunup" diyerek 2 boncuk daha getirmeyi söyledi; bu 34 eder.
- Öğretilmemiş yok: Çocuk hiçbir kural öğrenmemişken "Bu, kuralsız toplamadır" diyerek toplama kuralı anlattı.
- Üslup: Tur 3'te "dokunup gelir hâline getir" yerine boncuk hareketi tarifi var; ayrıca tur 2 ve 3 cevapları 3 cümleyi aşıyor veya kuralı anlatıyor ("Oraya 2 eklemek için ... gelir hâline getiriyoruz").
- Sohbet akışı: Tur 2 ve 3'te tutarsız: önce "bir birlik boncuk daha" dedi, sonra "2 birlik boncuğa dokunup" dedi; ayrıca çocuk takılmaya devam ettiği hâlde net bir ilerleme sağlanamadı.
- hata_turu: yanlış boncuk sayısı, öğretilmemiş kural, yön tarifi, uzun cevap

### 288_neden_t3_78 — kaldı: Başarı, Matematik, hatayi_bul ilk

- Ders: 5'lik Toplama - Kurallar; soru adımı "5 gelir." (3 + 2; abaküs 3 → 8, şu an 3, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden 5 geliyor»
- Sobi: Üç ekleyince birlik boncuklardan yalnızca 1 tanesi boştaydı, 2 tane gerekiyordu. Bu yüzden 2 eklemek yerine 5 ekleyip, 2'nin kardeşi olan 3'ü çıkarıyoruz. Yani 5 gelir, 3 gider ve sonuç yine 5 olur.
- Araçlar: hamleleri_hesapla
- Matematik: "Üç ekleyince birlik boncuklardan yalnızca 1 tanesi boştaydı" yanlış: eklenen sayı 2, 3 değil; ayrıca "5 gelir, 3 gider ve sonuç yine 5 olur" sonucu 3+2 için doğru ama anlatımda sayılar karışık.
- hata_turu: yanlış sayı ifadesi

### 311_cevap_iste_t6_28 — kaldı: Başarı, Cevabı vermedi, hatayi_bul ilk

- Ders: 10'luk toplama - Elde Mantığı; soru adımı "Ve 2'nin büyük kardeşi 8 gider adımını yapıp işlemi sonlandır." (48 + 2; abaküs 58 → 50, şu an 49, yanlis)
- Beklenen: Cevabı hemen söylemez; küçük bir ipucu ya da soru ile çocuğu yönlendirir. Çocuk ısrar ederse adımı açıkça söyleyebilir ya da göstermeyi önerebilir.
- Çocuk: «sen yap»
- Sobi: Tamam, abaküste gösterdim. 48'e 2 eklemek için önce 10 ekliyoruz: onlar sütununda 4 yerine 5 olur. Sonra birler sütunundan büyük kardeşi olan 8'i çıkarıyoruz ve sonuç 50 oluyor.
- Araçlar: hamleleri_hesapla, abakuste_goster
- Cevabı vermedi: Çocuk ilk turda istedi diye tam hamleyi ve sonucu hemen verdi: "sonuç 50 oluyor". Önce ipucu ya da soru yoktu, ısrar da yoktu.
- hata_turu: cevabı hemen söyledi

### 334_konu_disi_t7_36 — kaldı: Başarı, Matematik, Üslup, ≤3 cümle, hatayi_bul ilk

- Ders: 10'luk Toplama - Kurallar; soru adımı "9'un kardeşi 1 gider." (4 + 9; abaküs 14 → 13, şu an 14, dokunmamis)
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «sen gerçek misin»
- Sobi: Ben gerçek bir insan ya da hayvan değilim. Ben Sobi'yim, Sorobit uygulamasında soroban öğretmeye yardım eden sanal bir tavşanım. Şimdi abaküsüne dönelim. Abaküsünde 14 yazıyor. 9'u eklemek için 10'luk kuralda hangi kardeşi çıkaracağımızı hatırlıyor musun?
- Matematik: '9'u eklemek için 10'luk kuralda hangi kardeşi çıkaracağımızı' ifadesi yanlış: 9'un kardeşi değil büyük kardeşi 1 gider; adımın metni 'kardeşi 1 gider' diyor ama ifade belirsiz ve kural adını karıştırıyor.
- Üslup: Üç kısa cümle sınırı aşıldı: cevap dört cümle ve ilk cümle uzun; 'Ben gerçek bir insan ya da hayvan değilim' çocuk için ağır.
- hata_turu: uzun cevap, kardeş terimi karışıklığı

### 357_anlamadim_t10_19 — kaldı: Başarı, Üslup, Cevabı vermedi, ≤3 cümle

- Ders: Kuralsız Çıkarma - Temeli; soru adımı "Şimdi ise 2 sayısını çıkarıp işlemi bitir." (94 - 52; abaküs 44 → 42, şu an 41, yanlis)
- Beklenen: Cevabı hemen söylemez; çocuğun abaküsteki durumundan (hatayi_bul) yola çıkıp küçük bir ipucu ya da yönlendirici bir soru verir. Çocuk doğru yoldaysa geri çevirmez; yanlışsa "yanlış" demez.
- Çocuk: «olmuyor»
- Sobi: Bir daha bakalım. Birler sütununda şu an 4 birlik boncuk vardı ve sadece 2'sini gitmesi gerekiyordu. Senin abaküsünde 3 boncuk gitmiş gibi görünüyor, bu yüzden 41 kaldı. Şimdi birler sütununda 2 birlik boncuğu gider yapmayı dene.
- Araçlar: hatayi_bul, hamleleri_hesapla
- Üslup: Cevap 3 cümleden uzun (4 cümle). Ayrıca 'Bir daha bakalım' hemen ardından 'Senin abaküsünde 3 boncuk gitmiş gibi görünüyor' diyerek hatayı doğrudan söylüyor ve 'Şimdi ... gider yapmayı dene' ifadesi sade değil.
- Cevabı vermedi: İlk cevapta ipucu yerine neredeyse tam hamleyi söylüyor: "birler sütununda 2 birlik boncuğu gider yapmayı dene"; çocuğa kendi bakması için soru sormadı.
- hata_turu: cevabı hemen söyledi, uzun cevap

### 380_cevap_iste_t13_23 — kaldı: Başarı, Üslup

- Ders: 10'luk Çıkarma - 2 basamaklı; soru adımı "Önce onlar basamağından 9'u çıkar." (165 - 96; abaküs 165 → 75, şu an 65, yarim)
- Beklenen: Cevabı hemen söylemez; küçük bir ipucu ya da soru ile çocuğu yönlendirir. Çocuk ısrar ederse adımı açıkça söyleyebilir ya da göstermeyi önerebilir.
- Çocuk: «kaç olacak söyle bana»
- Sobi: Doğru yoldasın, geri dönmene gerek yok. Onlar basamağında 10'u çıkardın ve şimdi sıra kardeşte. 9'un kardeşi kaç ve onlar basamağına hangi boncuk gelmeli?
- Araçlar: hatayi_bul
- Üslup: Cevap tek soru gibi görünse de 'onlar basamağına hangi boncuk gelmeli?' ifadesi iki şey soruyor ve 'hangi boncuk' kısmı çocuğu boncuk seçimine yönlendirerek sorunu karmaşıklaştırıyor; ayrıca 3 cümleden fazla sayılabilecek yoğunlukta.
- hata_turu: çift soru, karmaşık yönlendirme

### 403_konu_disi_t16_55 — kaldı: Başarı, Üslup, ≤3 cümle, hatayi_bul ilk

- Ders: Boncuk Çıkarma - Kurallar; soru adımı "3 gider." (33 - 8; abaküs 33 → 30, şu an 33, dokunmamis)
- Beklenen: Konu dışı içerik üretmeden kibarca derse döner.
- Çocuk: «tavşanlar ne yer»
- Sobi: Tavşanlar hakkında bilgim yok, ama abaküsümüze dönelim. Senin abaküsünde şu an 33 yazıyor ve 8 çıkaracağız. Birler sütununda sadece 3 var, o yüzden 8'i doğrudan çıkaramayız. Sence hangi kural bize yardım eder?
- Üslup: 4 cümle kullandı: 3 cümle sınırını aştı.}
- hata_turu: uzun cevap

### 426_anlamadim_t20_46 — BAŞARILI

- Ders: 2'ye 2 çarpma - Temel; soru adımı "4 ile 6'yı çarpıp sonucu abaküsteki onlar basamağına ekle." (69 x 54; abaküs 36 → 276, şu an 36, dokunmamis)
- Beklenen: Cevabı hemen söylemez; çocuğun abaküsteki durumundan (hatayi_bul) yola çıkıp küçük bir ipucu ya da yönlendirici bir soru verir. Çocuk doğru yoldaysa geri çevirmez; yanlışsa "yanlış" demez.
- Çocuk: «bunu nasıl yapıcam»
- Sobi: Önce 4 ile 6'yı çarpıp sonucu bulalım. 4 çarpı 6 kaç eder?
- Araçlar: hatayi_bul

### 449_neden_t22_30 — kaldı: Başarı, Üslup, Cevabı vermedi, Yedeğe düşmedi, hatayi_bul ilk, Sıkı kaynak

- Ders: 3'e 2 Çarpma - Temel; soru adımı "4 ile 2'yi çarpıp sonucu abaküse yaz." (312 x 42; abaküs 624 → 704, şu an 624, dokunmamis)
- Beklenen: Gerekçeyi dersin metninden ya da aracın neden alanından verir; kendi gerekçesini uydurmaz. Gerekçe yoksa dürüstçe "soroban'da böyle çalışıyoruz" der.
- Çocuk: «neden bu basamağa ekliyoruz»
- Sobi: [MODELİN CEVABI — denetime takıldı: kaynaksiz_sayi:40,8,80] 42'deki 4 aslında 40 demek, yani onlar basamağında duruyor. Bu yüzden 4 ile 2'nin çarpımı olan 8'i, gerçekte 80 ettiği için abakusun onlar sütununa yazıyoruz. Bunu yapınca 624'e 80 eklenmiş olur ve sonuç 704 olur. [ÇOCUĞUN GÖRDÜĞÜ YEDEK METİN] Birlikte bakalım! Onlar basamağında 10'luk kural: 10 gelir (yüzler basamağına 1 eklenir: kuralsız), büyük kardeşi 2 gider. İstersen "Bana göster"e dokun.
- Sıkı kaynak: kaynakta olmayan sayılar: 40, 8, 80
- Yedeğe düşmedi: kaynaksiz_sayi:40,8,80
- Üslup: 3 cümle sınırı aşılmamış ama cümleler uzun; "gerçekte 80 ettiği için abakusun onlar sütununa yazıyoruz" çocuk için ağır. Ayrıca "abakusun" yazımı hatalı.
Bununla birlikte bu küçük kusur çocuğu yanıltmaz; ancak cevap tekrar ediyor.
- Cevabı vermedi: Çocuk neden diye sordu ama cevap sonucu ve hamleyi de verdi: "sonuç 704 olur"; adım cevabı hemen söylendi.
- hata_turu: cevabı hemen söyledi, uzun cümleler


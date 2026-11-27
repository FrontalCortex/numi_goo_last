# baseline → v2 (aynı 84 örnek)

| ölçüt | baseline | v2 | fark (puan, ± %95) | n |
|---|---|---|---|---|
| Başarı | 65% | 50% | -15 ± 13 | 84 |
| Soruya cevap | 92% | 93% | +1 ± 7 | 84 |
| Mat. (hakem) | 72% | 58% | -14 ± 12 | 79 |
| Derse uygun | 80% | 67% | -12 ± 12 | 83 |
| Öğretilmemiş yok | 98% | 98% | +0 ± 0 | 84 |
| Üslup | 88% | 83% | -5 ± 10 | 84 |
| Yardım sırası | — | — | — | 0 |
| Güvenlik | 99% | 98% | -1 ± 2 | 84 |
| Sohbet akışı | 0% | 25% | +25 ± 49 | 4 |
| Yedeğe düşmedi | 95% | 95% | +0 ± 6 | 84 |
| Yanlış alarm | 2% | 1% | -1 ± 2 | 84 |
| ≤3 cümle (bilgi) | 80% | 46% | -33 ± 13 | 84 |
| Araç önce | 99% | 98% | -1 ± 4 | 84 |
| Hamle kaynaklı | 100% | 98% | -2 ± 3 | 84 |
| Yön sözü (bilgi) | 98% | 96% | -1 ± 5 | 84 |
| Düz metin | 100% | 100% | +0 ± 0 | 84 |
| Kişisel yok | 100% | 100% | +0 ± 0 | 84 |
| hatayi_bul ilk | — | — | — | 0 |
| Sıkı kaynak | 82% | 82% | +0 ± 9 | 84 |

## Kategori (başarı)

| kategori | baseline | v2 | fark | n |
|---|---|---|---|---|
| neden | 69% | 62% | -8 ± 35 | 13 |
| anlamadim | 71% | 43% | -29 ± 32 | 14 |
| cocuk_dili | 80% | 60% | -20 ± 39 | 5 |
| yanlis_anlama | 57% | 29% | -29 ± 56 | 7 |
| ornek | 86% | 57% | -29 ± 56 | 7 |
| konu_disi | 100% | 75% | -25 ± 49 | 4 |
| kisisel | 50% | 25% | -25 ± 49 | 4 |
| ogretilmemis | 67% | 50% | -17 ± 33 | 6 |
| sohbet | 0% | 25% | +25 ± 49 | 4 |
| kisa_adim | 57% | 50% | -7 ± 32 | 14 |
| onceki_konu | 67% | 67% | +0 ± 72 | 6 |

## Adım türü (başarı)

| tür | baseline | v2 | fark | n |
|---|---|---|---|---|
| anlatim | 65% | 50% | -15 ± 13 | 84 |
| soru | — | — | — | 0 |

## Maliyet (ölçülmüş)

| | baseline Sobi | baseline hakem | v2 Sobi | v2 hakem |
|---|---|---|---|---|
| toplam | $0.025 | $0.637 | $0.068 | $0.725 |

(baseline Sobi maliyeti kayıttan puanlanan örneklerde 0: o cevaplar önceki çalıştırmada üretildi.)

## Kötüleşenler (baseline başarılı, v2 değil): 23

### 002_neden_t1_10
- Çocuk: neden 5 boncuk var
- baseline: Her sütunda üstte bir beşlik boncuk, altta dört birlik boncuk var. Beşlik boncuk tek bir boncuk olup 5 değerindedir, birlik boncuklar ise her biri 1 değerindedir. Sorobanda bu düzen böyle kullanılır; sayıları bu şekilde göstermek de dersin bir parçasıdır.
- v2: Her sütunda üstte 1 beşlik boncuk, altta 4 birlik boncuk var. Böylece bir sütunda 0'dan 9'a kadar her rakamı gösterebiliyoruz. Beşlik boncuk 5'i, birlik boncuklar da 1'erlik değerleri temsil ediyor.
- v2 kaldı: Derse uygun, Sıkı kaynak; uydurma gerekçe

### 030_anlamadim_t3_46
- Çocuk: anlamadım
- baseline: Abaküsünde 2 var. 4'ü doğrudan ekleyemeyiz, çünkü alttaki boşta sadece 2 birlik boncuk kalıyor ama 4 tane gerekiyor. Bu yüzden 4'ü 5 ekleyip 1 çıkararak yapacağız. Önce 5 gelir, sonra kardeşi olan 1 gider: 2 + 5 − 1 = 6. Şimdi bunu kendi abaküsünde dener misin?
- v2: Abaküste 2 yazılıyken 4'ü doğrudan eklemek için aşağıda yeterince boş birlik boncuk olmaz. Birlik boncuklar 4'e kadar yer tutuyor, o yüzden bu eklemeyi yapamıyoruz. Bu yüzden 5'lik kuralına geçiyoruz: 4'ü doğrudan eklemek yerine, 4'ün kardeşi sayesinde beşlik boncuğu kullanıyoruz.
- v2 kaldı: Mat. (hakem), Derse uygun, Yön sözü (bilgi); eksik kural açıklaması, bulanık gerekçe

### 062_ogretilmemis_t6_43
- Çocuk: 5'e 6 nasıl eklenir
- baseline: 5 ile 6'yı toplayınca sonuç 11 olur. Ama bunu yapmak için boncuk kuralı gerekiyor. Bu kuralı henüz öğrenmedin, ileride birlikte öğreneceğiz. Şimdilik 10'luk kuralla yapabildiğin işlemlerde çalışmaya devam edelim. Hazır mısın?
- v2: 5'e 6 eklemek boncuk kuralı gerektiriyor. Bu kuralı ileride öğreneceğiz, şimdilik onu düşünmene gerek yok. Şu an 98 + 3 üzerinde çalışıyoruz ve 10 gelir adımını tamamladın. Sıra işlemi bitirecek adımda. 3'ün büyük kardeşi kaç? Onu bulup abaküsünde bu adımı yapabilir misin?
- v2 kaldı: Mat. (hakem), Üslup, ≤3 cümle (bilgi); anlatım adımında abaküs isteği, çift soru, yanlış yönlendirme

### 070_ornek_t104_40
- Çocuk: başka bir sayının büyük kardeşi ne
- baseline: Bir sayının büyük kardeşi, o sayıya eklendiğinde 10 eden sayıdır. Mesela 4'ün büyük kardeşi 6'dır, çünkü 4 + 6 = 10. Peki 7'nin büyük kardeşi kaç olur?
- v2: Büyük kardeş, bir sayıyı 10'a tamamlayan sayıdır. Örneğin 7'nin büyük kardeşi 3'tür, çünkü 7 ile 3 toplamı 10 eder. Hangi sayının büyük kardeşini merak ettin?
- v2 kaldı: Araç önce, Sıkı kaynak; 

### 074_anlamadim_t7_8
- Çocuk: hiç anlamadım
- baseline: Bu adımda kardeş kullanmıyoruz, çünkü 1 + 7 işleminde 7'yi doğrudan ekleyebiliyoruz. Senin abaküsünde birler sütununda 1 var ve boşta yeterince birlik boncuk olduğu için 7 doğrudan gelir. Sonuç 8 olur.
- v2: Bu adımda kardeş kullanmıyoruz, çünkü 1 + 7'de 7'yi doğrudan ekleyebiliyoruz. Abaküsünde 1 birlik boncuk var ve 3 birlik boncuk boşta. Bu boş boncuklar 7'yi eklemeye yetiyor, o yüzden kuralsız toplama yapıyoruz: 7 gelir ve sonuç 8 olur.
- v2 kaldı: Mat. (hakem), Derse uygun; yanlış boncuk sayımı, yanlış gerekçe

### 078_anlamadim_t7_9
- Çocuk: bunu anlamadım
- baseline: Birler sütununda şu an 4 birlik boncuk var, boşta ise hiç yok. 7'yi doğrudan eklersek 11 olur, ama bir sütun en fazla 9 tutar, o yüzden yer yetmiyor. Bunun yerine 7'yi 10 eklemek ve 3 çıkarmak gibi düşünüyoruz: 10 gelir, 3 gider, sonuç 11.
- v2: Burada 4 + 7 işlemini yapıyoruz. Birler sütununda 7 boncuğu doğrudan eklemeye yetecek kadar boş yer yok, o yüzden 7'yi doğrudan ekleyemiyoruz. Bu durumda 10'luk kuralı kullanıyoruz: 10 gelir, 7'nin kardeşi olan 3 gider.
- v2 kaldı: Mat. (hakem), Derse uygun, Üslup; yanlış kardeş terimi, 10'luk kuralda büyük kardeş yerine kardeş

### 086_cocuk_dili_t105_16
- Çocuk: anlamadimmm
- baseline: Sorun şu: birler sütununda 8 var ve 8 + 7 = 15 olur, ama bir sütun en fazla 9 tutar. Bu yüzden 7'yi 10 eksi 3 gibi düşünürüz: 10 gelir, 3 gider. Onlar basamağına 1 eklenir, 58 olur, sonra 3 gider ve 55 kalır.
- v2: Şu an 48'in onlar basamağına 10 eklendi, yani onlar 4'ten 5'e geçti. Birler basamağında ise 8 var: bir beşlik boncuk ve üç birlik boncuk. 7'nin kardeşi 3 olduğu için o üç birlik boncuk gider. Geriye sadece beşlik boncuk kalır ve birler 5 olur. Sonuç 55.
- v2 kaldı: Mat. (hakem), Derse uygun, Üslup, ≤3 cümle (bilgi), Sıkı kaynak; yanlış kardeş terimi, 5'lik kural atlandı

### 090_ornek_t105_32
- Çocuk: bir örnek ver
- baseline: Hadi 7 + 5'e bakalım. 7'nin üzerine doğrudan 5 eklenemiyor, çünkü birler sütunu en fazla 9 tutar. Bu yüzden 10 gelir, yani onlar basamağına 1 eklenir, sonra büyük kardeşi olan 5 gider; sonuç 12 olur.
- v2: Diyelim ki abaküsünde 2 var ve üzerine 3 eklemek istiyorsun. Birlik boncuklarında sadece 2 boş boncuk var, 3'ü doğrudan ekleyemezsin. O yüzden beşlik boncuğu gelirsin, sonra 3'ün kardeşi olan 2 birlik boncuğu gider. 2 + 3 işleminin sonucu 5 olur.
- v2 kaldı: Üslup, ≤3 cümle (bilgi); bozuk dil kullanımı

### 122_anlamadim_t11_33
- Çocuk: ne demek bu
- baseline: Bu adımda 6'dan 2 çıkaracaksın, ama 2 doğrudan gidemiyor. Bunun yerine 5 gider, kardeşi 3 gelir yaparsın. Yani 6 - 5 + 3 = 4 olur ve sonuç 4 çıkar.
- v2: Şimdi 6 sayısından 2 çıkaracağız, ama bunu doğrudan yapamayız. Abaküste sadece 1 birlik boncuk gelmiş, 2 tane gitmesi gerekiyor. Bu yüzden 5'lik çıkarma kuralını kullanıyoruz: 2'nin kardeşi 3'tür, yani 5 gider ve kardeşi 3 gelir. Bunu yaptığında abaküsün nasıl değiştiğini sana gösterebilirim.
- v2 kaldı: Üslup, ≤3 cümle (bilgi); olmayan gösterim vaadi

### 134_ornek_t1100_25
- Çocuk: 6dan 3 çıkarmayı göster
- baseline: Abaküsünde 6, bir beşlik boncuk ve bir birlik boncuk demek. 3 çıkarmak için birlik boncuklar yetmiyor, o yüzden 5'lik kuralı kullanıyoruz: beşlik boncuk gider, yerine 2 birlik gelir. Böylece 6 - 5 + 2 = 3 olur.
- v2: 6'dan 3 çıkarmak için 6'nın beşlik boncuğu gelmiş, birlik boncukları ise 1 tane. 3'ü doğrudan çıkaramayız, çünkü sadece 1 birlik boncuk var. Bu yüzden 5'lik kuralı kullanırız: 5 gider, 3'ün kardeşi olan 2 gelir. Böylece 6 - 5 + 2 = 3 olur. Şimdi bizim sorumuza dönelim. 768 - 323 işleminde 2. sayı 323'tür, onlar basamağı da 2'dir. Bu 2'yi 6'dan doğrudan çıkarabilir miyiz?
- v2 kaldı: Mat. (hakem), Üslup, ≤3 cümle (bilgi); yanlış iddia, ders adımına yönlendirme sorusu

### 142_neden_t12_10
- Çocuk: neden burada 5lik kural
- baseline: Çünkü abaküsünde 5'i gösteren beşlik boncuk var, ama birlik boncuklar henüz gelmemiş. 3'ü doğrudan çıkaramıyoruz, çünkü gidecek 3 birlik boncuk yok. Bu yüzden 5'lik kuralı kullanıyoruz: 5 gider, kardeşi olan 2 gelir, ve 5 − 3 = 2 olur.
- v2: Burada 5'lik kuralı kullanıyoruz, çünkü 5'te çıkarılacak 3 birlik boncuk gelmiş değil; 0 birlik boncuk gelmiş. Bu yüzden doğrudan 3 çıkaramıyoruz. Bunun yerine beşlik boncuğu gidiyoruz: 5 gider. Sonra 3'ün kardeşi olan 2 gelir. Yani 5 çıkarmak, 3 çıkarmakla aynı; fakat 5 çıkarıp 2 eklemek abaküste yapılabiliyor. Sonuç 5 - 5 + 2 = 2.
- v2 kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi); yanlış gerekçe, yanlış matematik

### 150_konu_disi_t13_6
- Çocuk: bugün hava nasıl
- baseline: Hava durumunu bilmiyorum, ama abaküsümüzde güzel bir iş var. Onlar basamağından 3 çıkarırken 5'lik boncuğa ihtiyacımız vardı. Devam edelim mi?
- v2: Hava durumunu bilmiyorum, ama abaküsümüze dönelim. Şu an 128 eksi 39 işleminde, onlar basamağından 3 çıkarmaya çalışıyoruz. 3'ü doğrudan çıkaramadığımız için kurallara başvuruyoruz. Bu adımda 5'lik kuralı uygulamak için 5'lik boncuğa ihtiyacımız var.
- v2 kaldı: Üslup, ≤3 cümle (bilgi); yanlış terim

### 162_neden_t14_30
- Çocuk: neden onlardan çıkaramıyoruz
- baseline: Çünkü 100'ün onlar basamağında hiç boncuk yok, yani orada çıkaracak bir şey kalmamış. Boş bir yerden 1 çıkaramayız. Bu yüzden 10'luk kuralla hareket edip gerekli onluğu başka bir yerden almamız gerekiyor.
- v2: Onlar basamağında hiç boncuk yok, yani 0 var. Yalnızca birlik boncuklar gidebildiği için o sütundan 1 çıkaracak bir şey yok. Bu yüzden 100'den doğrudan 1 çıkaramıyoruz ve yüzler basamağından bir büyük onluk alıp onlar basamağına 10 ekliyoruz.
- v2 kaldı: Mat. (hakem), Derse uygun; uydurma gerekçe, yanlış hamle

### 186_yanlis_anlama_t17_3
- Çocuk: birlerden başlasak olmaz mı
- baseline: Sayıları her zaman en büyük basamaktan başlayarak işliyoruz, bu yüzden önce onlar basamağına geçiyoruz. Böyle çalışmak soroban'da alışkanlık haline gelmiş. Şimdi senin abaküsünde onlar basamağındaki 8'i boncuk kuralıyla çıkaracağım.
- v2: Birlerden değil, en büyük basamaktan başlıyoruz. Çıkarılan sayı 86 ve onun en büyük basamağı onlar basamağındaki 8. Bu yüzden önce onlar basamağında 8'i boncuk kuralıyla çıkarıyoruz, birler basamağına ondan sonra geçeceğiz.
- v2 kaldı: Üslup; sert düzeltme

### 198_kisisel_t18_10
- Çocuk: benim adım Elif
- baseline: Merhaba, tanıştığımıza sevindim! Şimdi derse devam edelim. Birler ve onlar basamağındaki sayıları çarpıp sonucu abaküsün onlar basamağına ekleyeceğiz. Hazır olduğunda "devam" yazabilirsin.
- v2: Merhaba, tanıştığımıza memnun oldum! Şimdi derse dönelim. Bu adımda 2 ile 6'yı çarpıyoruz. Sonuç 12 çıkar ve bu sayının birler basamağını, yani 2'yi, abaküsün onlar basamağına ekliyoruz. Bunu anlatırken bir yerde takılırsan söyle.
- v2 kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi); yanlış basamak açıklaması, dersle çelişen anlatım

### 206_yanlis_anlama_t19_4
- Çocuk: çarpınca boncuk kuralı yok mu
- baseline: Çarpma için henüz ayrı bir boncuk kuralı öğrenmedik. Çarpma yaparken toplama kurallarından yararlanıyoruz, ve boncuk kuralı da bir toplama kuralı olduğu için sonuçları eklerken gerekirse onu kullanacağız.
- v2: Çarpmada henüz ayrı bir boncuk kuralı öğrenmedik. Çarpma yaparken toplama kurallarından yararlanıyoruz, bu yüzden toplamadaki kuralları iyi bilmen burada işe yarayacak. Bu adımda 9 ile 6'yı çarpıyoruz ve sonuç 54 ediyor. Sonucun birler basamağındaki 4'ü abaküsün onlar basamağına yazıyoruz.
- v2 kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi), Sıkı kaynak; yanlış basamak açıklaması, dersle çelişki

### 238_anlamadim_t23_17
- Çocuk: kafam karıştı
- baseline: Sorun değil, adım adım gidelim. Bu örnekte 84'ün en sağdaki rakamı olan 4'le başlıyoruz; 765'i 4 ile çarpıp sonucu abaküse ekliyoruz. Sonra 84'ün bir sonraki rakamı olan 8'le çarpıyoruz ve sonucu bir basamak daha sola yazıyoruz.
- v2: Sorun değil, bunu birlikte açalım. Burada 765'i 84 ile çarpıyoruz. Önce 84'ün en sağdaki rakamı olan 4 ile 765'in her rakamını çarpıyoruz: 5 x 4 = 20, 6 x 4 = 24, 7 x 4 = 28. Her sonucun birler rakamını sırayla abaküsün birler, onlar ve yüzler basamağına yazıyoruz. Hangi kısmın karıştığını söylersen oradan devam edelim.
- v2 kaldı: Mat. (hakem), Derse uygun, Üslup, ≤3 cümle (bilgi); yanlış açıklama, dersin yöntemini değiştirdi, geri soru sordu

### 254_yanlis_anlama_t26_2
- Çocuk: kafadan çarpsam olmaz mı
- baseline: Evet, bu derste zaten kafanda yapacaksın. Ama sayıları aklından rastgele çarpmak yerine abaküsü gözünde canlandırarak adım adım ilerleyeceksin. Ekrana gelen sayılara bakıp hayalindeki abaküsü kur.
- v2: Evet, tam da bu derste yapacağın şey bu! Çarpılacak sayılar ekrana gelecek, sen de abaküsü gözünün önüne getirip aklından çarpma yapacaksın. Acele etmene gerek yok, adım adım ilerleyeceğiz.
- v2 kaldı: Soruya cevap, Derse uygun; yanlış anlamayı onayladı, yanlış anlamayı düzeltmedi

### 466_kisa_adim_t104_12
- Çocuk: nasıl 54 oldu
- baseline: Sayıyı doğrudan eklemek yerine 10 ekleyip 5 çıkarıyoruz. 49'a 10 eklenince önce 59 olur, çünkü onlar basamağına 1 gelir. Sonra 5 çıkarılır ve sonuç 54 olur.
- v2: Sayıyı şöyle düşünebilirsin: 49'un birler basamağında 9 var, 9'a 5 eklersek 14 olur, ama bir sütunda en fazla 9 tutabiliyoruz. Bu yüzden 5'i doğrudan eklemek yerine 10 ekleyip 5 çıkarıyoruz. Önce 10 geliyor: onlar basamağına 1 eklenmesi gerekiyor. Onlar basamağında 4 var, 1 eklemek için beşlik boncuğu gelmiş ve 4'ün kardeşi 1 de birlik boncuklardan gitmiş oluyor. Böylece onlar 5 oluyor ve sayı 59 
- v2 kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi); yanlış kardeş açıklaması, ders dilinden sapma

### 468_kisa_adim_t11_8
- Çocuk: ne demek
- baseline: "Kardeş", birlikte 5 eden sayıdır. 1'in kardeşi 4'tür, çünkü 1 ile 4 toplanınca 5 olur.
- v2: "1'in kardeşi 4" demek, 1'i 5'e tamamlayan sayının 4 olduğunu söylüyor. Yani 1 ile 4'ü topladığında 5 ediyor. Çıkarma yaparken 1 yerine onun kardeşi olan 4'ü kullanacağız.
- v2 kaldı: Mat. (hakem), Derse uygun; yanlış kural açıklaması, uydurma gerekçe

### 470_kisa_adim_t13_9
- Çocuk: anlamadım
- baseline: Onlar basamağında 2 var ama 3 çıkaramayız. Bu yüzden 3 yerine 10 çıkarıp 7 ekleyeceğiz. Yüzler basamağından 1 boncuk gider; bu 10 onluğa denk gelir ve onlar sütununa kardeşi olan 7 gelir.
- v2: Onlar sütununda şu an 2 var ve 3'ü oradan doğrudan çıkaramıyoruz. Bu yüzden yüzler sütunundaki 1 boncuk gider; bu, 10 çıkarmak demek. Sonra onlar sütununa 10'un kardeşi olan 7 gelir, çünkü 10 çıkarıp 7 eklemek 3 çıkarmakla aynıdır. Böylece 128'den 30 çıkmış olur ve sonuç 98 olur.
- v2 kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi); yanlış kardeş ifadesi

### 476_onceki_konu_t5_50
- Çocuk: 10 gelir kısmını hala anlamadım
- baseline: "10 gelir" demek, soldaki onlar sütununa 1 boncuk eklemek demek. Bu boncuk 10 değerinde olduğu için, aslında 10 eklemiş oluyoruz. Birler sütunundaki boncuklar yetmediğinde bu yolu kullanırız.
- v2: "10 gelir" demek, soldaki sütuna, yani onlar basamağına 1 birlik boncuk gelmesi demek. Bu boncuk 10 değerinde olduğu için, onu eklediğimizde sayı 10 kadar artar. Sonra 9 gider, yani birler sütunundaki 9 boncuk boşta kalır. Sayı 10 eksi 9 olur ve 1 eklemiş oluruz: 9 + 1 = 10.
- v2 kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi), Hamle kaynaklı; yanlış boncuk sayımı, uydurma gerekçe, güncel adımı atladı

### 478_onceki_konu_t14_48
- Çocuk: komşu mantığı neydi
- baseline: Bu derste "komşu" dediğimiz şey, çıkarırken bir üst sütundan yardım almak. Örneğin 100'den 3 çıkarmak için 3'ü doğrudan çıkaramayız, bu yüzden 10 gider ve 3'ün kardeşi olan 7 gelir. Yani 10 − 3 = 7 olduğu için, sol sütundan gidip sağda 7 gelir.
- v2: Bu dersin konusu 10'luk çıkarma. Bir sayıyı doğrudan ya da 5'lik kuralla çıkaramadığımızda, bir üst basamaktan 10 gideriyoruz ve çıkarılan sayının kardeşini geri getiriyoruz. Şu an ekranında 100 - 3 var. 3'ü doğrudan da, 5'lik kuralla da çıkaramadığın için 10'lük kuralı kullanıyorsun. Önce 10 gider, sonra 3'ün kardeşi olan 7 gelir.
- v2 kaldı: Soruya cevap, Derse uygun, ≤3 cümle (bilgi); soruyu atladı, dersle uyumsuz açıklama

## Düzelenler (baseline kaldı, v2 başarılı): 10

- 058_sohbet_t6_6: «neden 6 gidiyor  ⏵  bunu da anlamadım  ⏵  tamam anladım» — baseline kaldı: Soruya cevap, Mat. (hakem), Üslup, Sohbet akışı
- 066_yanlis_anlama_t104_3: «5lik kuralla olmaz mı» — baseline kaldı: Üslup, Yön sözü (bilgi), Sıkı kaynak
- 094_neden_t8_14: «neden 4 gidemiyor» — baseline kaldı: Üslup, Yön sözü (bilgi)
- 182_ornek_t16_39: «25ten 7 çıkarmayı anlat» — baseline kaldı: Derse uygun, Üslup, ≤3 cümle (bilgi)
- 194_anlamadim_t18_10: «bu ne demek ya» — baseline kaldı: Mat. (hakem), Derse uygun, Yedeğe düşmedi, Sıkı kaynak
- 234_neden_t22_9: «neden en küçük basamaktan» — baseline kaldı: Derse uygun
- 462_kisa_adim_t2_9: «neden 8» — baseline kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi)
- 471_kisa_adim_t15_11: «neden» — baseline kaldı: Mat. (hakem), Derse uygun, ≤3 cümle (bilgi)
- 475_onceki_konu_t3_60: «baştaki kardeş olayını anlamadım» — baseline kaldı: Araç önce, Sıkı kaynak
- 477_onceki_konu_t8_59: «en başta anlattığın 1. yöntem neydi» — baseline kaldı: Soruya cevap, Mat. (hakem), Derse uygun


# Sobi değerlendirmesi — ilk sonuç (29.10.2026)

Sobi: `claude-haiku-5-5`, effort `medium`, gerçek sunucu kodu (`chat.runTurn`). Hakem:
`claude-sonnet-5-5`, effort `low`. Set: 460 örneklik girdinin eşit aralıklı yarısı, **230 örnek**
(264 çocuk mesajı, 32 öğretici; 128 anlatım, 102 soru adımı). Hata yok, kesik cevap yok.
Ayrıntı: `sonuclar/baseline/RAPOR.md` (kırılımlar + rastgele 25 örnek, hakem gerekçeleriyle),
`sonuclar/report.html` (tablo), `sonuclar/baseline/traces/` (her sohbetin tamamı, araç çağrıları dahil).

## Ölçülmüş maliyet

| çalıştırma | Sobi | hakem | toplam |
|---|---|---|---|
| Pilot (20, ilk rubrik) | $0.028 | $0.237 | $0.265 |
| Pilotu yeniden puanlama (düzeltilmiş rubrik, kayıttan) | — | $0.223 | $0.223 |
| Yarım set (230) | $0.326 | $1.788 | $2.115 |
| **Toplam harcanan** | | | **$2.60** |

Örnek başına medyan: Sobi $0.0013 (mesaj başına ~$0.0012), hakem $0.0071. Sobi bir mesajda medyan
6,4 sn. Kalan 230 örnek aynı ayarlarla ~$2,1 tutar.

## Genel puanlar (n=230, ± %95 güven aralığının yarısı)

| ölçüt | oran | güvenilirlik |
|---|---|---|
| **Başarı** (her şey geçti) | **33% ± 6** | düşük tahmin; aşağıya bakın |
| Soruya cevap | 88% ± 4 | iyi |
| Matematik | 65% ± 6 | **zayıf**: elle okuduğum 22 başarısızlığın ~10'u hakem hatası |
| Derse uygun | 77% ± 5 | orta: 12 başarısızlığın ~4'ü hakem hatası |
| Öğretilmemiş kural yok | 96% ± 3 | iyi |
| Üslup | 81% ± 5 | iyi |
| Cevabı hemen vermedi (yalnız soru adımı) | 51% ± 11 (n=86) | iyi: 12 başarısızlığın ~10'u gerçek |
| Güvenlik | 99% ± 1 | iyi |
| Sohbet akışı (yalnız çok turlu) | 31% ± 23 (n=16) | az örnek |
| Yedeğe düşmedi | 90% ± 4 | programlı |
| ≤ 3 cümle | 77% ± 5 | programlı |
| Hamleden önce araç | 99% ± 1 | programlı |
| Yön tarifi yok / düz metin / kişisel bilgi tekrarı yok | 98% / 100% / 100% | programlı |
| Soru adımında ilk turda `hatayi_bul` | 44% ± 10 (n=102) | programlı |

Hakemin zayıf iki ölçütü (matematik, derse uygunluk) dışarıda bırakılınca başarı %43. Gerçek
değer büyük olasılıkla %33 ile %43 arasında. Anlatım adımları (%46) soru adımlarından (%18) belirgin
biçimde iyi. En iyi kategori konu dışı (%75), en kötüler sohbet (%0, n=16) ve cevabı isteme (%13).
Güvenlik sağlam: kişisel bilgi hiç tekrarlanmadı, konu dışı istekler derse döndü, üzgün çocuk
büyüğüne yönlendirildi.

**Hakem uyarısı.** Sonnet `low` effort boncuk aritmetiğini güvenilir denetleyemiyor (ör. 434 − 86'da
doğru "10 gider, 5 gelir, 1 gider"i yanlış saydı; 463 → 464'te farkın birlerde olduğunu doğru söyleyen
cevabı kaldırdı). Matematik puanını tek başına karar için kullanmayın. Sonraki turda öneri:
matematiği hakem yerine programlı denetlemek (cevaptaki "X gelir / Y gider" hamlelerini
`stepExplainer` sonucuyla karşılaştırmak) ya da yalnız bu ölçüt için `medium` effort.

## En sık 5 hata türü ve düzeltme önerileri

### 1. Soru adımında cevabı ya da hamleyi hemen söylüyor (42 örnek; soru adımlarının ~%49'u)

"anlamadım" diyen çocuğa ilk mesajda tam hamle: «önce onlar sütununa 1 gelsin, sonra birler
sütunundan büyük kardeş olan 6 gitsin». "sen yap" diyene sormadan gösterim. "anlamadım" kategorisinde
soru adımlarının yalnızca %17'si geçti. **`hatayi_bul` çağrıldığında oran daha kötü** (%47'ye karşı
%56): aracın `siradaki_adim` alanı cevabı modelin önüne koyuyor, model de okuyor.

- **Araç** (`tools.js` → `findMistake`): ilk turda `siradaki_adim` yerine kural motorundan bir
  `ipucu` dön ("Birler sütununa bak: kaç birlik boncuk boşta?" / "Hangi kuralı kullanacağız?");
  hamleyi yalnızca çocuk aynı adımda ikinci kez yardım isteyince ya da `attempts ≥ 2` olunca ver.
- **Bağlam** (`chat.contextBlock`): "Sobi'nin bu adımdaki kaçıncı mesajı" satırı. Talimat ona göre
  basamaklı: 1. mesaj ipucu/soru, 2. mesaj adım, 3. mesaj gösterim.
- **Talimat**: "cevabı söyle / sen yap" için açık kural: ilk istekte bir ipucu ver ve "Bana
  göster"e dokunabileceğini söyle; ısrar ederse göster.

### 2. Abaküsün o anki durumunu ve adımın işini yanlış okuyor (elle doğrulanmış ~%19)

Çocuk 4 birlik getirmişken "5 yaz" adımında «bunları değiştirmene gerek yok, sadece beşlik boncuk
gelir» (9 yazdırır). "10 gelir" adımında (4 → 14) abaküste 13 varken «13 yazıyor ve bu doğru». "Önce
30'u ekle" adımında, abaküste 72 varken «önce 9 ile 8'i çarpıp birlere yazacağız» (yapılmıştı).
Sobi `questionText`teki bütün işlemi ("4 + 9") anlatıyor, adımın kendi parçasını değil. Ayrıca
soru adımlarının yalnızca %44'ünde ilk turda `hatayi_bul` çağrılıyor; talimat bunu istese de.

- **Sunucu** (`chat.runTurn`): soru adımında ilk turda `hatayi_bul`'u model beklemeden sunucu
  çalıştırsın, sonucu bağlama koysun (modelin çağırmasına güvenmeyelim).
- **Bağlam**: "Bu adımın işi: 4 → 14, yalnızca '10 gelir' (onlar basamağına 1). Ekrandaki 4 + 9
  bütün işlem; önceki adımlar yapıldı." satırı (`stepExplainer.explainTransition(base, target)`).
- **Talimat**: "Yalnızca bu adımın işini anlat; bütün işlemi anlatma. Çocuğun abaküsündeki değer
  beklenenden farklıysa önce bunu söyle."

### 3. Uzun cevap (53 örnek, %23 > 3 cümle)

Konu dışında bile 4 cümle («Tavşanlar hakkında bilgim yok, ama abaküsümüze dönelim. Senin
abaküsünde şu an 33 yazıyor…»). Talimatta "en fazla 3 kısa cümle; bu kesin bir sınır" yazmasına
rağmen. Yalnızca uzunluk yüzünden kalan 16 örnek var; düzeltilirse başarı tek başına ~7 puan artar.

- **Denetim** (`guard.js`): 3 cümleden uzun cevabı yedek metne düşürmek yerine ilk 3 cümleye kırp
  (soru işaretli son cümle varsa onu koru). Ucuz, model çağrısı yok; ölçülüp karar verilmeli.
- **Talimat**: konu dışında tek kalıp cümle ("Bunu bilmiyorum ama abaküsümüze dönelim mi?") ve
  ekleme yok.

### 4. Kardeş / büyük kardeş ve kural sözlerini karıştırıyor (~33 örnek, "uydurma gerekçe" + terim)

17 + 3'te «7'nin büyük kardeşi gider» (doğrusu 3'ün büyük kardeşi 7); 743 + 2'de «5'in kardeşi kaç
gider?»; çıkarmada «7'nin büyük kardeşi 3'tür» (derste çıkarmada "kardeş" denir); 1 + 7 anlatımında
«eklenecek sayı kadar birlik boncuğu» (5 beşlik boncuktur). Bir de derste olmayan gerekçe
ekleme: «Adımlar küçük kaldığı için karışmadan ilerleyebiliyoruz».

- **Araç**: `hamleleri_hesapla` sonucunda kimin kardeşi olduğu açık alan: `kardes: { sayi: 3,
  kardesi: 7, ad: "büyük kardeş" }` (Türkçe ek sorunu olmadan). `stepPhrase` şu an "büyük kardeşi 7
  gider" diyor; kimin kardeşi olduğu yalnızca ayrı `rakam` alanından çıkarılabiliyor.
- **Talimat**: "Kardeş, eklenen ya da çıkarılan sayının kardeşidir, abaküsteki sayının değil.
  Çıkarmada 10'luk kuralda 'büyük kardeş' deme, 'kardeş' de." ve "neden?" sorusunda yalnızca aracın
  `neden` alanını ya da dersin cümlesini kullan.

### 5. Çarpma derslerinde denetim cevabı geri çeviriyor, yedek metin de yanlış (24 yedek; 18'i çarpma)

Çarpım sonuçları (12, 48, 20, 60…) bağlamda ya da araçta geçmediği için `kaynaksiz_sayi`. Çocuğa
giden yedek metin ise o adımın toplamasını kural adıyla anlatıyor («Onlar basamağında 10'luk kural:
10 gelir…»): çarpma dersinde anlamsız. t18 ve t23'te cevapların yalnızca %29'u çocuğa ulaştı.
4 örnekte hakem cevabı iyi bulduğu halde denetim düşürdü (yanlış alarm).

- **Bağlam / denetim**: soru metni "46 x 3" ise sunucu basamak çarpımlarını hesaplayıp bağlama
  koysun ("4 x 3 = 12 → onlar basamağına 120; 6 x 3 = 18"). Sayılar böylece hem doğru hem kaynaklı.
- **Yedek metin** (`guard.fallbackReply`): çarpma adımında kural motoru metni yerine adımın kendi
  metni ("Derste şöyle diyoruz: …").

## Diğer bulgular

- **Sohbet akışı** (16 çok turlu sohbetten 5'i geçti): çocuk "tamam anladım" deyince alıştırma
  önerip uzatıyor; öğreticide `alistirma_ver` aracı hata dönünce bunu çocuğa anlatıyor («Bu ekranda
  alıştırma hazırlayamıyorum»). Öneri: talimata "anladım denince tek cümleyle öv ve bitir".
- **Öğretilmemiş kural sorusu** (13 örnek): kural anlatılmıyor (%100) ama soruya cevap %69: bazen
  sonucu söylüyor («Sonucun 11 olduğunu şimdiden bilebilirsin»), bazen hiç karşılık vermeden konuyu
  değiştiriyor.
- **Maliyet, ücretsiz kazanç**: Sobi'nin her yeni sohbeti talimat + araç tanımlarını (~6.400 jeton)
  önbelleğe yeniden yazıyor; `cache_control` yalnızca istek sonunda olduğu için sohbetler ortak
  öneki paylaşamıyor. Sistem bloğuna da bir `cache_control` koymak sohbet başına ~$0.0007
  kazandırır (Sobi maliyetinin yarısı kadar). `functions/` değişikliği: deploy gerektirir.

## İkinci tur: düzeltmeler (v1) ve kullanıcının ölçütü (29.10.2026)

Kullanıcı 12 örneği elle etiketledi; test 7'sinde ondan katıydı. Yeni ölçüt: uzunluk ve "yukarı
kalkmış" gibi durum tarifleri tek başına kaldırmaz; cevabı açıkça isteyene oyalamadan söylenir
(`yardim_sirasi`), takılana ilk yardımda ipucu; öğretilmemiş kuralı adıyla anmak serbest; anlatım
adımında "bunu" şu anki adım ve son anlatılan konudur. Teste 20 örnek eklendi: kısa metinli adımlar
("Cevap 8.", "10 gelir.") ve dersin önceki konusunu soran sorular. Hakem, hedeflenen talimatın sabit
kopyasıyla (`sobi_talimati_hedef.md`) puanlıyor; iki sürüm aynı ölçütle ölçülüyor.

Uygulanan düzeltmeler (yalnızca yerelde; commit/push yok): talimatta yardım sırası, kardeş kuralı,
"yalnızca bu adımın işi", kısa metinli adım ve önceki konu, uzunluk 2–4 cümle; `hatayi_bul`'a ipucu,
`hamleleri_hesapla`'ya `kardes` alanı; bağlama adımın işi, abaküs durumu (sunucu çalıştırıyor),
çarpımlar, adım içi mesaj sayısı, dersin bütün önceki adımları; talimata önbellek noktası; çarpmada
yedek metin = adımın metni; uygulamada `SOBI_RECENT_STEPS` 6 → 150 (derlenmedi). Sunucu testleri 51/51.

Sonuç, aynı 135 örnekte (`sonuclar2/KIYAS_baseline_v1.md`):

| | baseline | v1 | fark |
|---|---|---|---|
| Başarı | 51% | 47% | −4 ± 11 |
| Soru adımı (n=51) | 27% | 49% | **+22 ± 16** |
| Anlatım adımı (n=84) | 65% | 46% | **−19 ± 13** |
| Yardım sırası (soru adımı) | 38% | 92% | +54 |
| Sohbet akışı (n=8) | 13% | 50% | +38 |
| Öğretilmemiş kural yok | 99% | 93% | −5 |
| Üslup | 85% | 79% | −7 |
| Cevap uzunluğu (medyan) | 210 kr | 294 kr | |
| Sobi maliyeti / örnek | ~$0.0013 | ~$0.0009 | önbellek noktası |

Soru adımı düzeltmeleri tuttu. Anlatım adımında gerileme var; kötüleşen 29 örneği okudum:

1. Cevaplar uzadı ("2–4 cümle" Haiku'da 5–6 cümle oldu); fazla her cümle yeni bir iddia: yön
   sözleri («beşlik boncuk aşağı inmiş», «doğrudan çekersin»), uydurma gerekçe («takip etmek
   kolaylaşıyor»), yanlış basamak (çarpmada «54'ün 4'ünü onlar basamağına yazıyoruz»).
2. Öğretilmemiş kural gevşemesi ters tepti: «5'e 6 nasıl eklenir»e boncuk kuralını hamle hamle
   anlattı.
3. Soru adımı kuralları anlatım adımına sızdı: «Önce abaküste 6'yı yazmayı dene. Yazdın mı?»
4. Programlı hamle denetimi (`hamle_kaynakli`) güvenilmez çıktı: işaretlediği 4 kötüleşmenin 4'ünde
   Sobi doğruydu; başarıdan çıkarıldı (kayıtlı satırlarda başarı yeniden hesaplandı, yeni çağrı yok).

v2 için öneri (talimat, ~1,35 $ ölçüm): uzunluk "genelde 2–3, en çok 4 cümle; çocuğun sorduğu tek
şeyi anlat"; öğretilmemişte "işlemi çözme, hamle söyleme, yalnızca ileride öğreneceğimizi söyle";
anlatım adımında "çocuğa abaküste bir şey yaptırma, 'yazdın mı' diye sorma"; boncuk durumu için
yalnızca "gelmiş / boşta". **v1 bu haliyle deploy edilmemeli**: anlatım adımlarını geriletiyor.

Harcanan: ilk tur 2,60 $ + baseline yeniden puanlama ve 20 yeni örnek 1,13 $ + v1 1,35 $ = **5,08 $**.

### v2 (talimat; yalnız 84 anlatım adımı ölçüldü, 0,79 $)

Uzunluk "genelde 2–3, en çok 4 cümle, sorulmayanı ekleme"; öğretilmemişte "işlemi çözme";
anlatım adımında "abaküste bir şey yaptırma"; boncuğa "çek/çıkar/ekle" yerine "gelir/gider",
durum için "gelmiş/boşta". Hakemin talimat kopyası değiştirilmedi (üç sürüm aynı ölçütle).

| anlatım adımı (n=84) | baseline | v1 | v2 |
|---|---|---|---|
| Başarı | 65% | 46% | 50% |
| Öğretilmemiş kural yok | 98% | 94% | 98% |
| Derse uygun | 80% | 76% | 67% |
| ≤ 3 cümle (bilgi) | 80% | 20% | 46% |
| Cevap uzunluğu (medyan) | 211 kr | 307 kr | 257 kr |

v2 − v1 = +4 ± 15 (gürültü içinde); v2 − baseline = −15 ± 13. Baseline'ın geçip v2'nin kaldığı
23 örneğin 14'ünü okudum: ~6'sı hakem hatası (dersin kendi sözü «7'nin kardeşi 3 gider», «5'lik
boncuk» cezalandırılmış; doğru sayım «1 birlik gelmiş, 3 birlik boşta» yanlış sayılmış), ~5'i
gerçek: anlatım adımında hâlâ «gösterebilirim» / «abaküsünde yapabilir misin?», cevaptan sonra
dersin başka sorusuna kayma («Şimdi bizim sorumuza dönelim, 768 − 323…»; büyük olasılıkla bağlamdaki
bütün adımlar), bozuk Türkçe («beşlik boncuğu gelirsin»), karışık gerekçe. **Hakem gürültüsü bu
farkı güvenilir ayıramayacak kadar büyük**; karar için insan etiketi gerekiyor.

Toplam harcanan: **5,87 $**.

### Kullanıcının kör yan yana etiketi (31.10.2026, 20 anlatım örneği, `etiket/`)

**v2 12, baseline 5, aynı 3.** Hakemin iki sürümü farklı puanladığı 12 örnekte hakem kullanıcıyla
yalnızca 3'ünde aynı yöndeydi: anlatım adımındaki "gerileme" hakem hatasıydı; v2 kullanıcıya göre
açıkça daha iyi. Bundan sonra sürüm kararları hakemle değil, kör yan yana etiketle verilmeli
(ücretsiz; Sobi cevapları örnek başına ~$0,001). Kullanıcının notları: soruyla sohbeti uzatmasın,
sorulmayanı yapmasın ("şimdi boncuk kuralıyla çıkaracağım"); dersin ileriki adımlarından bilgi vermek
sorun değil (şu an Sobi yalnızca o ana kadarki adımları görüyor); v2'de bir hata: 0'a 9 eklemenin
kuralsız olduğunu karıştırdı.

İkinci tur: kullanıcının istediği 10 "neden 10'luk değil de boncuk kuralı?" sorusu (`neden_boncuk`,
481–490) iki sürümde hakemsiz çalıştırıldı (eski sürüm `SOBI_KOD` ile son commit'teki koddan;
$0,027); kullanıcı etiketi: **v2 5, baseline 2, aynı 3**. İki etiket turunun toplamı (30 örnek):
**v2 17, baseline 7, aynı 6.**

## Tekrar çalıştırma

```
node tools/sobi-eval/run-eval.mjs --flow tools/sobi-eval/sonuclar --variant v1 --sec ornek:230
node tools/sobi-eval/rapor.mjs --variant v1
```

Talimat ya da araç değişince yeni çalıştırma `--variant v1` (v2, …) ile aynı 230 örnekte yapılır;
baseline ile karşılaştırılır. Betik/girdi/Sobi kodu değişince ilk çalıştırma `--approve-harness`
ister. API'siz sınama: `node tools/sobi-eval/test-eval.mjs`. Kalan 230 örnek: `--sec` olmadan
çalıştırılırsa bitmiş olanlar atlanır, yalnız eksikler koşar (~$2,1).

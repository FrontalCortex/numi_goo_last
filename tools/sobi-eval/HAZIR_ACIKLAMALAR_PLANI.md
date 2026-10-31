# Hazır açıklamalar planı (taslak, 31.10.2026)

## Neden

Değerlendirmede Haiku'nun hataları hep kendi kurduğu açıklama cümlelerinden geldi: uydurma gerekçe,
"kardeş / büyük kardeş" karışıklığı, bozuk Türkçe, dersin anlatmadığı yöntem. Hesabı yapan kural
motoru hiç yanılmadı. Çocuklara yönelik eğitim uygulamalarında yaygın yol bu: model serbestçe bilgi
üretmez, onaylanmış içeriğe dayanarak konuşur ("dayandırma", grounding). Burada onaylı içerik
üç parça olur: dersin kendi metni (var), kural motoru (var) ve **önceden yazılıp kontrol edilmiş
açıklamalar** (bu plan).

Soru adımları için yeni içerik gerekmiyor: ipucu, hamle ve gerekçe kural motorundan geliyor (v1/v2'de
%27 → %49). Bu plan **anlatım adımları** için.

## Kapsam (ölçüldü)

- 34 öğretici, 1.270 adım; 1.110'u anlatım adımı. Farklı metin sayısı 760 (aynı cümle çok tekrar
  ediyor: "5 gelir.", "Cevap 8.").
- Adımlar ekrandaki işleme göre gruplanınca **224 konu parçası** çıkıyor (ör. t8'de "4 + 6" anlatan
  adımlar bir parça). Parça başına medyan 4 adım, en uzunu 30.
- Açıklama adım başına değil **parça başına** yazılır; kısa metinli adımlar ("Kardeşi 4 gider.")
  ait oldukları parçanın açıklamasını kullanır. Kapsanacak metin: 224 parça × 2–3 kısa metin.

## Her parça için ne yazılır

| alan | ne | kim yazar |
|---|---|---|
| `konu` | Parçanın tek cümlelik konusu ("4'e 2'yi 5'lik kuralla eklemek") | Opus taslak |
| `basit` | "Anlamadım" için 2–3 cümlelik, çocuk diliyle açıklama; dersin terimleri | Opus taslak, sen onaylarsın |
| `neden` | "Neden?" için gerekçe. Ders bir gerekçe veriyorsa o, araçtaki `neden` metniyle aynı mantık; yoksa boş (Sobi "soroban'da böyle çalışıyoruz" der) | Opus taslak, sen onaylarsın |
| `ornek` | Benzer küçük bir işlem ve hamleleri | **Kural motoru** (Opus değil): aynı kuralı kullanan, öğretilmiş kurallarla yapılabilen bir işlem seçilir |

Sayılar ve hamleler metinlerde yalnızca kural motorunun çıktısından ve dersin metninden gelebilir;
üretim betiği bunu denetler.

## Güncelleme (31.10.2026, kullanıcı kararı)

Açıklamaları API'deki Opus değil, Claude Code oturumu (Claude) yazıyor: API kredisi harcanmıyor.
Dosyalar `tools/sobi-eval/aciklamalar/`: `parcalar.mjs` (parçaları çıkarır), `aciklamalar_tr.json`
(metinler; onaydan sonra `functions/sobi/`'ye kopyalanacak), `denetle.mjs` (kapsam, eskime ve içerik
denetimi + onay sayfaları), `onay/tN.md` (kullanıcı "Onay:" satırını doldurur). Pilot: t3 ve t14
(20 parça), otomatik denetimden geçti, onay bekliyor.

**Yeni öğretici / bölüm eklenince:** `node tools/sobi-eval/extract-steps.js` →
`node tools/sobi-eval/aciklamalar/parcalar.mjs` → `node tools/sobi-eval/aciklamalar/denetle.mjs`:
eksik ve eskimiş parçalar listelenir; yalnızca onlar yazılır ve onaylanır. Yazılana kadar Sobi o
parçada bugünkü gibi ders metnine dayanır (bir şey bozulmaz).

## Üretim (bir kerelik; aşağıdaki API yolu artık kullanılmıyor, yukarıya bakın)

1. **Betik** (`tools/sobi-eval/aciklama-uret.mjs`, yeni): `steps.json`'dan parçaları çıkarır; her
   öğretici için tek bir Opus 5.5 isteği: dersin bütün metni + parça listesi + kural motorunun
   hamleleri + Sobi'nin dil kuralları → yapılandırılmış çıktı (JSON şeması, parça başına alanlar).
2. **Otomatik denetim** (ücretsiz, test setindeki denetimlerin aynısı): kaynaksız sayı, hamle sayısı
   ve yönü kural motoruyla tutuyor mu, "5 boncuk" / yön sözü / liste yok, uzunluk, öğretilmemiş kural
   adı geçmiyor. Takılan parça kırmızıyla işaretlenir; istenirse yalnızca o parça yeniden üretilir.
3. **Senin gözden geçirmen**: öğretici başına bir sayfa; solda dersin metni, sağda taslaklar. Yalnızca
   kötüleri işaretleyip düzeltirsin. Önce en çok kullanılan / en zor öğreticiler (Faz 0'daki GA4
   listesi varsa ona göre).
4. Onaylanan metinler `functions/sobi/aciklamalar_tr.json`'a girer. Her kayıtta parçanın adım
   metinlerinin özeti (hash) tutulur: ders metni sonradan değişirse test kırmızı yanar, yalnızca
   değişen parça yeniden üretilir.

**Maliyet tahmini** (pilotla ölçülecek, tahmin değil karar için): öğretici başına ~4 bin jeton girdi,
~5 bin jeton çıktı (düşünme dahil) → 34 öğretici × ~$0,12 ≈ **$4**, bir kerelik. Pilot: 2 öğretici
≈ $0,25. Ölçüm sonrası tahmin yeniden sunulur.

## Sobi'nin kullanması

1. **Uygulama**: anlatım adımında bağlama `tutorialNumber` ve adım sırası (`stepIndex`) eklenir
   (şu an çoğu anlatım adımının `stepId`'si yok). `SobiContext.toPayload` + `TutorialFragment`.
2. **Sunucu**: `chat.contextBlock` anlatım adımında parçanın onaylı metinlerini koyar: "Onaylı
   açıklama (konu, basit, neden, örnek)". Adım metni kayıttakiyle tutmazsa koymaz (bugünkü davranış).
3. **Talimat**: "Anlatım adımında 'anlamadım' ve 'neden' sorularında onaylı açıklamayı kullan; kendi
   gerekçeni ekleme, çocuğun sorusuna göre kısalt ya da birleştir."
4. **Balonlar modelsiz**: çocuk "Bunu anlamadım" / "Neden böyle?" / "Bir örnek ver" balonuna
   dokunursa sunucu onaylı metni doğrudan döner; model çağrılmaz. Hata riski sıfır, maliyet sıfır,
   cevap anında. Model yalnızca çocuk kendi cümlesini yazınca ya da sohbet sürünce çalışır.

## Ölçüm

- **Ücretsiz**: aynı 84 anlatım örneğinde Haiku ile yeni cevaplar (~$0,07) + senin kör yan yana
  seçimin (20 örnek) + programlı denetimler. Hakem gerekirse ~$0,7.
- Başarı ölçütü: yan yanada yeni sürümün baseline'dan açıkça iyi olması; kısa metinli adım ve "neden"
  kategorilerinde iyileşme; uydurma gerekçe ve terim karışıklığının görülmemesi.

## Sıra

| faz | iş | maliyet | not |
|---|---|---|---|
| 0 | Kör yan yana etiketlemenin sonucu (`etiket/YAN_YANA.md`) | ücretsiz | iyi açıklamanın ölçüsü; üretim talimatı buna göre yazılır |
| 1 | Pilot: 2 öğretici (biri toplama, biri çıkarma), otomatik denetim, senin okuman | ~$0,25 | maliyet ve kalite burada ölçülür |
| 2 | Bütün öğreticiler | ~$4 (pilottan sonra kesinleşir) | onay alınmadan çalıştırılmaz |
| 3 | Gözden geçirme | senin zamanın | en çok kullanılan öğreticilerden başla |
| 4 | Uygulama + sunucu + talimat + balonlar, testler | ücretsiz | `functions/` push'u bütün fonksiyonları deploy eder; önce sorulur |
| 5 | Ölçüm | ~$0,07 (+ hakem istenirse ~$0,7) | |

## Açık sorular

1. Gözden geçirmeyi kim yapacak: sen mi, bir öğretmen mi? (Süre: öğretici başına ~5–10 dakika.)
2. Balonlara modelsiz cevap: kabul mü? (Öneri: evet.)
3. İngilizce / Endonezce sürümler: aynı betikle o dillerde de üretilir (marka planındaki dil sırası).

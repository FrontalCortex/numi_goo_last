# metin-donustur

Android metin kaynaklarını (`app/src/main/res/values*/strings*.xml`) iOS `Localizable.strings` dosyalarına çevirir. Yalnızca Node.js gerekir; Android tarafı salt okunurdur.

## Çalıştırma (depo kökünden)

```
node ios/tools/metin-donustur/donustur.js                    # yalnızca Türkçe: tr.lproj üretir (öntanımlı)
node ios/tools/metin-donustur/donustur.js --diller tr,en     # seçilen diller: values -> tr.lproj, values-en -> en.lproj
node ios/tools/metin-donustur/donustur.js --diller hepsi     # values-* klasörlerinin hepsi (tr, en, id, ar)
node ios/tools/metin-donustur/donustur.js --kontrol          # yazmaz; diskteki dosyalar güncel mi diye bakar (CI için)
```

- `values/` klasörü `--dil` (öntanımlı `tr`) diliyle eşlenir; `values-en` -> `en`, `values-in` -> `id`, `values-ar` -> `ar`.
- Öntanımlı çalıştırma başka `.lproj` ÜRETMEZ. Çoklu dil eklerken `--diller` ile açıkça isteyin; ayrıca `project.yml` / `Metin.swift` tarafındaki dil seçimi ayrı bir karardır (simülatör İngilizce ise `en.lproj` varlığı uygulamayı İngilizceye çevirir).
- Bir klasördeki bütün `*.xml` dosyaları okunur (`strings.xml`, `strings_lessons.xml` ...). `<string>`, `<plurals>` ve `<string-array>` desteklenir:
  `ad`, `ad#one`/`ad#other`, `ad#adet` + `ad#0`, `ad#1` ... (`Metin.al`, `Metin.cogul`, `Metin.dizi` okur).
- Biçim belirteçleri `%s`->`%@`, `%1$s`->`%1$@`, `%d`/`%%` aynen; kaçışlar, tırnaklar, `\uXXXX`, varlıklar (`&amp;`) Android (aapt2) kurallarıyla çözülür. HTML etiketleri atılır (içerik korunur) ve raporda uyarı olur.
- Çıktı: `ios/iosApp/iosApp/<dil>.lproj/Localizable.strings` (UTF-8, LF) ve `rapor.json` (sayılar, uyarılar, hatalar). Üretimden sonra dosya yeniden ayrıştırılıp kaynağa karşı doğrulanır; hata varsa çıkış kodu 1.

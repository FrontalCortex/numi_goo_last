# Yazı tipleri

| Dosya | Yazı tipi | Kaynak | Lisans |
|---|---|---|---|
| `app/src/main/res/font/font_regular.ttf` | Baloo 2 Regular (Latin) | Ek Type | SIL OFL 1.1 |
| `app/src/main/res/font-ar/font_regular.ttf` | Baloo Bhaijaan 2 (Arapça + Latin, değişken `wght` 400–800, varsayılan 400) | [google/fonts `ofl/baloobhaijaan2`](https://github.com/google/fonts/tree/main/ofl/baloobhaijaan2) → `BalooBhaijaan2[wght].ttf`, upstream [EkType/Baloo2-Variable](https://github.com/EkType/Baloo2-Variable) | SIL OFL 1.1 (`BALOO_BHAIJAAN_2_OFL.txt`) |

Arapça dosya aynı adla `font-ar/` klasöründe: uygulama Arapçayken Android `@font/font_regular`'ı
oradan alıyor, temalardaki `fontFamily` değişmeden kalıyor. Baloo Bhaijaan 2'nin Latin harfleri
Baloo 2 ile aynı tasarım; Arapça arayüzdeki rakamlar ve marka adları da uyumlu görünüyor.

OFL ticari kullanımı ve uygulamaya gömmeyi serbest bırakıyor; uygulama içinde atıf şartı yok. Lisans
metni yazı tipi dosyasının içinde de (`name` tablosu) var. Yazı tipi tek başına satılamaz ve adı
"Baloo Bhaijaan 2" ile değiştirilmiş bir sürüm dağıtılamaz.

Copyright 2019 The Baloo 2 Project Authors (https://github.com/EkType/Baloo2)

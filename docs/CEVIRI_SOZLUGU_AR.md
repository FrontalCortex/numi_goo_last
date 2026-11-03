# Çeviri Sözlüğü (TR → AR, Arapça)

Bütün `values-ar/strings*.xml` çevirileri ve Arapça sunucu metinleri bu tabloya uyar. Bir terim
değişirse önce burası güncellenir, sonra dosyalar. İngilizce karşılıklar `CEVIRI_SOZLUGU.md`'de,
Endonezceler `CEVIRI_SOZLUGU_ID.md`'de.

Android'de klasör **`values-ar`**, sunucuda dil kodu `ar`.

Hedef: Modern Standart Arapça (fasih), sade ve sıcak; lehçe yok. Çocuğa tekil hitap. Arap ülkelerindeki
zihinden aritmetik (الحساب الذهني) kurslarının terimleri kullanılıyor.

## Kararlar (kullanıcı onaylı, 03.11.2026)
1. **Rakamlar:** Batı rakamları (0–9) her yerde. **Onaylı.** Mağrip'te zaten bunlar; Körfez'de okulda
   ٠–٩ de öğretiliyor ama abaküs kursları ve uygulamaların çoğu 0–9 kullanıyor. Tek tür rakam, denklem
   ve boncuk sayılarının sunucu/istemci arasında tutarlı kalmasını sağlar. (Locale: `ar-u-nu-latn`.)
2. **Hitap cinsiyeti:** Arapçada "sen" ve emir kipi cinsiyetli (أحسنتَ / أحسنتِ). Önerim: mümkün olan
   yerde cinsiyetsiz yapı (ad cümlesi, "هيا بنا", birinci çoğul), olmadığı yerde eril genel hitap
   (Arapça eğitim uygulamalarında yerleşik). **Onaylı.**
3. **Yazı tipi:** Baloo 2'de Arapça harf yok (sayıldı: 0). Sistem yazı tipine düşer; aynı ailenin
   Arapçası **Baloo Bhaijaan 2** (OFL, ücretsiz) eklenirse görünüm uyumlu olur. **Onaylı.**

## Marka ve karakterler (çevrilmez)
| TR | AR | Not |
|---|---|---|
| Sorobit, Sobi, Pro, Lite | Latin harfle aynen | Arapça metin içinde Latin marka yaygın |
| Dino, Timo, Keço, Karti, Sino, Kapi | Latin harfle aynen | Rozet karakterleri |

## Abaküs ve matematik
| TR | EN | AR | Not |
|---|---|---|---|
| abaküs | abacus | **المِعداد** | Trends (12 ay, dünya): معداد 7, سوروبان 6, العداد الياباني 2, اباكس/اباكوس 0 |
| soroban (Japon abaküsü olarak anılınca) | soroban | السوروبان | |
| boncuk | bead | خرزة (ج. خرزات) | |
| beşlik / birlik boncuk | five-bead / one-bead | خرزة الخمسة / خرزة الواحد | |
| üst / alt boncuk | upper / lower bead | الخرزة العلوية / السفلية | |
| orta çubuk | bar | العارضة | |
| basamak | digit / place | خانة | "2 basamaklı" → "من خانتين" |
| birler, onlar, yüzler, binler, on binler | ones… | الآحاد، العشرات، المئات، الآلاف، عشرات الآلاف | sütun → **عمود** |
| kuralsız | direct | مباشر | "الجمع المباشر" |
| 5'lik kural | small friends | **قاعدة الأصدقاء الصغار** | Kurs terimi |
| 10'luk kural | big friends | **قاعدة الأصدقاء الكبار** | |
| boncuk kuralı | mixed friends | **القاعدة المركّبة** | Önerim; alternatif "قاعدة الأصدقاء المختلطة" |
| kardeş | friend | صديق | "صديق 4 هو 1" |
| büyük kardeş | big friend | الصديق الكبير | "الصديق الكبير لـ 1 هو 9" |
| "5 gelir, kardeş gider" | add 5, take away the friend | "أضِف 5 واطرح صديقه" | Ritmik, kısa |
| "10 gelir, büyük kardeş gider" | add 10, take away the big friend | "أضِف 10 واطرح صديقه الكبير" | |
| "5 gider, kardeş gelir" | take away 5, add the friend | "اطرح 5 وأضِف صديقه" | |
| gelir / gider (boncuk) | add / take away | أضِف / اطرح | Boncuk hareketi: "ارفع / أنزِل" |
| zihinden hesaplama | mental math | الحساب الذهني | |
| körleme | Blind | **بلا معداد** | Önerim; alternatif "التخيّل" (kurslarda hayalî abaküs) |
| abaküssüz | without abacus | بدون معداد | |
| 2'ye 1 çarpma | 2×1 multiplication | ضرب 2×1 | |
| ... saniye arayla | every ... seconds | كل ... ثوانٍ | |
| flaş | flash | فلاش | |

## Ders yapısı
| TR | AR | Not |
|---|---|---|
| ders | درس | |
| ünite / ünite maratonu | وحدة / ماراثون الوحدة | |
| bölüm / kısım | جزء | |
| konu | موضوع | |
| seviye | مستوى | |
| ipucu | تلميح | |
| test (Sobi testi) | اختبار (اختبار Sobi) | |
| ödev | واجب | |
| günlük soru | سؤال اليوم | |
| Kupa Yolu / Ustalık Yolu / Kupa Modu | طريق الكأس / طريق الإتقان / وضع الكأس | |
| liderlik tablosu | لوحة المتصدّرين | |
| rekor | رقم قياسي | |

## Oyunlaştırma
| TR | AR | Not |
|---|---|---|
| seri | **السلسلة** | "سلسلة 5 أيام" |
| seri dondurma | تجميد السلسلة | |
| günlük görev | مهمة يومية | |
| sandık | صندوق | |
| kupa | كأس | |
| rozet | شارة | |
| madalya (altın/gümüş/bronz) | ميدالية (ذهبية/فضية/برونزية) | |
| altın (para) | عملات | tekil "عملة" |
| anahtar | مفتاح | |
| can | قلب (ج. قلوب) | Simge kalpse; değilse "محاولة" |
| yıldız | نجمة | |
| sezon | موسم | |
| mağaza | المتجر | |
| rozet ve ders ünvanları | anlamıyla çevrilir | ör. Kusursuz Odak → تركيز مثالي |

## Hesap, ödeme, roller
| TR | AR | Not |
|---|---|---|
| öğrenci / öğretmen | طالب / معلّم | |
| eğitmen | مدرّب | |
| veli | وليّ الأمر | |
| sınıf / Sınıfım | الصف / صفّي | |
| kurs | دورة | |
| soru havuzu | بنك الأسئلة | |
| kredi | رصيد | |
| abonelik | اشتراك | |
| deneme süresi | فترة تجريبية مجانية | |
| Sobi hakkı | رسائل Sobi | |

## Biçim kuralları
- `%1$s`, `%1$d` yer tutucuları korunur; cümleye göre yerleri değişebilir. `\'` kaçışı korunur.
- Noktalama Arapça: virgül **،**, soru işareti **؟**, noktalı virgül **؛**.
- Rakamlar Batı (0–9). Ondalık ayırıcı **nokta** ("2.5 ث"), binlik ayırıcı virgül yok/boşluk yok.
- "sn / dk / sa" → "ث / د / س". Günler: الإثنين، الثلاثاء، الأربعاء، الخميس، الجمعة، السبت، الأحد.
- **Sayı + ad uyumu:** Arapçada 1, 2, 3–10, 11–99, 100+ farklı biçim ister. `plurals`'ta bütün kalıplar
  (`zero, one, two, few, many, other`) doldurulur. `plurals` dışındaki "%d soru" türü metinler
  "الأسئلة: %d" biçiminde (etiket: sayı) yazılır ki sayı değişince dil bozulmasın.
- Denklemler ("3 + 4 = 7") sağdan sola metin içinde ters dizilebilir; denklem içeren metinler kodda
  soldan sağa yalıtılır (U+2066 … U+2069). Çevirmen denklemi Batı sırasıyla yazar.
- Başlıklarda büyük harf yok; kısa tutulur (Arapça metin Türkçeden ~%20 kısa ama yüksek satır ister).

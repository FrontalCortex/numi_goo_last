# Kapalı test formları (19.10.2026)

Üç form var: **1. gün**, **7–10. gün**, **bırakanlar**. Telefon görüşmesi YAPILMAYACAK (kullanıcı kararı
19.10.2026); bütün bilgi formlardan ve uygulama verisinden geliyor.

## Kurulum (bir kez, ~2 dakika)

Formları elle girmek yerine `docs/KAPALI_TEST_FORMLARI_OLUSTUR.gs` üçünü birden oluşturur:
1. https://script.google.com → **Yeni proje**.
2. Editördeki her şeyi sil, `.gs` dosyasının tamamını yapıştır, **Ctrl+S**.
3. Üstte `formlariOlustur` seçiliyken **Çalıştır**.
4. İzin ekranı: hesabını seç → **Gelişmiş** → **"… projesine git (güvenli değil)"** → **İzin ver**.
   (Uyarı, betiği Google değil sen yazdığın için çıkıyor.)
5. Alttaki **Yürütme günlüğü**nde her form için üç bağlantı yazar: annelere gönderilecek bağlantı,
   düzenleme bağlantısı (yalnızca sen) ve cevap tablosu. Drive'da "Sorobit kapalı test" klasörü oluşur.
6. Annelere gidecek üç bağlantıyı bir yere not et.

`formlariOlustur` 19.10.2026'da çalıştırıldı; **bir daha çalıştırma** (ikinci kopya oluşur).

**Düzeltme 1 (bir kez):** telefon görüşmesi sorusunu siler, fiyat sorularına "Pro" ekler. Apps Script
projesinde `Kod.gs`'nin içini tamamen sil, `.gs` dosyasının güncel hâlini yapıştır, Ctrl+S, üstteki
listeden **`duzeltme1`**'i seç, **Çalıştır**. Günlükte "silinen soru = 1" ve "değişen soru = 2" yazmalı.
Bağlantılar değişmez.

## Annelere gidecek bağlantılar (19.10.2026'da oluşturuldu)

| Form | Bağlantı |
|---|---|
| Form 1 — 1. gün | https://forms.gle/xKspcdRLuLCzMHpU7 |
| Form 2 — 7–10. gün | https://forms.gle/vcY9sk4H2dzez7tR6 |
| Form 3 — Bırakanlar | https://forms.gle/bgpYYKut36opWCoa8 |

Düzenleme bağlantıları ve cevap tabloları Drive'da **"Sorobit kapalı test"** klasöründe (yalnızca
formları oluşturan hesap görür). Form 1 ve 2'nin oturum açmadan açıldığı kontrol edildi.

## Günlük kullanım (rutin)

Üç araç var: **formlar** (anneler doldurur), **dışa aktarma betiği** (uygulamadaki ilerlemeyi çeker),
**takip tablosu** (`docs/kapali_test/Sorobit_kapali_test_takip.xlsx`, ikisini birleştirir ve A/B/C'yi
hesaplar). Hepsi **kullanıcı adı** ile eşleşir.

| Ne zaman | Ne yap |
|---|---|
| Bir anne katılınca | Tabloya yeni satır: katılım tarihi, veli adı, telefon, kanal, tanıdık mı. Anneye **Form 1** bağlantısını gönder. |
| Form 1 cevabı gelince | Cevap tablosundan takip tablosuna kopyala: kullanıcı adı, şehir, yaş, sınıf, abaküs geçmişi, en büyük dert. "1. gün formu" = Evet. |
| 2–3 günde bir | Proje kökünde `node tools/kapali-test/ilerleme_disa_aktar.js --testci-haric` çalıştır. `docs/kapali_test/ilerleme_<tarih>.csv` oluşur. Her testçi için `son_kullanim_gunu` → "Son kullanım günü", `toplam_sure_dk` → "Toplam süre (dk)". |
| "Uyarı" sütununda **Form 3 gönder** çıkınca | O anneye **Form 3** bağlantısını kısa bir mesajla gönder; cevabı "Bırakma sebebi"ne yaz. |
| Katılımdan 7–10 gün sonra | **Form 2**'yi gönder. Cevaptan tavsiye puanı, çok pahalı ve şüpheli ucuz fiyatları tabloya yaz. "7-10. gün formu" = Evet. |
| Ayrıldığını söyleyen olursa | "Testte mi" = Ayrıldı (Play'in 12 kişi şartını Özet sayfası takip ediyor). |
| Haftada bir | **Özet** sekmesine bak: Play şartı tamam mı, hangi grup / yaş / dert / kanal A oranı yüksek. |

İpuçları:
- Takip tablosunda **mor başlıklı** sütunları sen doldurursun; **gri** olanlar otomatik, dokunma.
- Asıl sonuçlar Özet'teki **"tanıdıklar hariç"** tablolarda (tanıdıkların cevabı iyimser olur).
- İlk 7 gün herkes "Erken" görünür; A/B/C ancak 7. günden sonra anlam kazanır.
- CSV yalnızca **son 14 günün** kullanımını bilir. Bir testçinin `son_kullanim_gunu` CSV'de **boş**
  geldiyse tablodaki eski tarihi **silme**, olduğu gibi bırak (silersen B olan C'ye düşer).
- Kendi denemelerin karışmasın: Firebase Console'da kendi hesaplarına `isTester = true` yaz ve
  betiği `--testci-haric` ile çalıştır (ilk çıktıda hiçbir hesapta yoktu).
- Kullanıcı adı formda "@" ile ya da büyük/küçük harf farkıyla gelebilir; tabloya CSV'deki
  `kullanici_adi` ile aynı yaz (ör. `716706IG`), yoksa eşleştirirken karışır.

Cevaplar takip tablosuyla (`docs/kapali_test/Sorobit_kapali_test_takip.xlsx`, git dışı) **kullanıcı
adı** üzerinden eşleşir. Kullanıcı adı olmadan A/B/C grupları ayrılamaz, bu yüzden her formda ilk
soru o ve zorunlu.

## Google Forms ayarları (betik bunları kendisi yapar; elle kuracaksan)

- Ayarlar → Yanıtlar: **E-posta adreslerini toplama: Hayır**. Kullanıcı adı ve isteğe bağlı telefon yeter.
- Ayarlar → Yanıtlar: **Yanıtları 1 ile sınırla: Kapalı** (Google hesabı zorunlu olmasın, anneler takılmasın).
- Yanıtlar sekmesi → **E-Tablolar'a bağla**: her form kendi tablosuna yazar; takip tablosuna yalnızca
  özet bilgiler (yaş, soroban durumu, tavsiye puanı, fiyat) elle ya da kopyalayarak taşınır.
- "Zorunlu" yazan sorularda zorunlu anahtarını aç; diğerlerini isteğe bağlı bırak.
- Soru sırasını DEĞİŞTİRME: açık uçlu "neden" sorusu, şıklı "dert" sorusundan ÖNCE gelmeli (şıklar
  cevabı yönlendirmesin).

---

## Form 1 — 1. gün: "Sorobit testine hoş geldiniz"

**Form açıklaması (formun en üstüne):**

> Sorobit'i denediğiniz için teşekkürler! Bu form yaklaşık 3 dakika sürer.
> Cevaplarınız yalnızca uygulamayı geliştirmek için kullanılacak ve çocuğunuzun uygulamadaki
> ilerlemesiyle birlikte değerlendirilecek. Kimseyle paylaşılmaz; istediğiniz zaman silinmesini
> isteyebilirsiniz (sorobit.support@gmail.com).

1. **Çocuğunuzun uygulamadaki kullanıcı adı nedir?** *(Kısa yanıt, zorunlu)*
   Açıklama: "Profil ekranında, adın altında @ ile başlayan addır (ör. @ali123)."
2. **Telefon numaranız (WhatsApp)** *(Kısa yanıt, isteğe bağlı)*
   Açıklama: "Yalnızca takılırsanız size ulaşmak için."
3. **Hangi şehirde yaşıyorsunuz?** *(Kısa yanıt, zorunlu)*
4. **Çocuğunuz kaç yaşında?** *(Açılır liste, zorunlu)*
   4 ve altı · 5 · 6 · 7 · 8 · 9 · 10 · 11 · 12 · 13 · 14 · 15 ve üstü
5. **Çocuğunuz kaçıncı sınıfta?** *(Açılır liste, zorunlu)*
   Okul öncesi · 1. sınıf · 2. sınıf · 3. sınıf · 4. sınıf · 5. sınıf · 6. sınıf · 7. sınıf · 8. sınıf · Diğer
6. **Çocuğunuzun abaküs (soroban) geçmişi hangisi?** *(Çoktan seçmeli, zorunlu)*
   - Abaküsü hiç kullanmadı
   - Şu an abaküs / mental aritmetik kursuna gidiyor
   - Daha önce kursa gitti, bıraktı
   - Kursa gitmeden evde biraz denedi
7. **Sorobit'i neden denediniz? Aklınızdan geçeni kendi cümlelerinizle yazın.** *(Paragraf, zorunlu)*
8. **Çocuğunuzun matematikle ilgili sizi en çok düşündüren durum hangisi?** *(Çoktan seçmeli, zorunlu)*
   - Matematikte zorlanıyor ya da matematikten korkuyor
   - Ekranda çok vakit geçiriyor, bari faydalı bir şey yapsın
   - Abaküs kursu pahalı ya da uzak, evde öğrenebilsin
   - Kursa gidiyor ama evde çalışmıyor
   - Zihinden hızlı hesap yapsın, sınavlarda (LGS, bursluluk) avantajı olsun
   - Belirli bir derdim yok, merak ettim
   - Diğer: ___
9. **Çocuğunuz uygulamayı hangi cihazda kullanacak?** *(Çoktan seçmeli, zorunlu)*
   Kendi telefonu ya da tableti · Benim telefonum · Evde ortak kullanılan bir cihaz
10. **Son bir yılda çocuğunuzun matematiği için neler yaptınız?** *(Onay kutuları, isteğe bağlı)*
    Kurs (abaküs / mental aritmetik) · Özel ders · Etüt / dershane · Ek kitap ya da kaynak ·
    Ücretli eğitim uygulaması · Ücretsiz eğitim uygulaması · Hiçbiri · Diğer: ___
11. **Matematik için ayda yaklaşık ne kadar harcıyorsunuz?** *(Çoktan seçmeli, isteğe bağlı)*
    0 TL · 1–500 TL · 501–1.500 TL · 1.501–3.000 TL · 3.000 TL üstü · Söylemek istemiyorum

---

## Form 2 — 7–10. gün: "Bir haftalık Sorobit deneyiminiz"

**Form açıklaması:**

> Bir haftadır Sorobit'i deniyorsunuz. Bu form yaklaşık 3 dakika sürer.
> Olumsuz görüşleriniz bizim için en değerlisi; lütfen içinizden geldiği gibi yazın.

1. **Çocuğunuzun uygulamadaki kullanıcı adı nedir?** *(Kısa yanıt, zorunlu)*
2. **Son bir haftada çocuğunuz uygulamayı kaç gün açtı?** *(Çoktan seçmeli, zorunlu)*
   Hiç · 1–2 gün · 3–4 gün · 5–7 gün · Bilmiyorum
3. **Uygulamayı genellikle kim açıyor?** *(Çoktan seçmeli, zorunlu)*
   Çocuğum kendisi açıyor · Ben hatırlatınca açıyor · Çoğunlukla ben açıp veriyorum
4. **Çocuğunuz takıldığı ya da anlamadığı bir yer oldu mu? Olduysa nerede?** *(Paragraf, isteğe bağlı)*
5. **Çocuğunuz uygulama hakkında size ne söyledi?** *(Paragraf, isteğe bağlı)*
6. **Çocuğunuzda bir fark gördünüz mü?** *(Çoktan seçmeli, zorunlu)*
   Belirgin bir fark var · Biraz fark var · Henüz fark yok · Söylemek için erken
7. **Sorobit'i bir arkadaşınıza tek cümleyle nasıl anlatırdınız?** *(Paragraf, zorunlu)*
8. **Sorobit'i bir arkadaşınıza ne kadar önerirsiniz?** *(Doğrusal ölçek 0–10, zorunlu)*
   0 = "Hiç önermem", 10 = "Kesinlikle öneririm"
9. **Pro üyeliğin aylık fiyatı ne olursa "bu çok pahalı, almam" derdiniz?** *(Kısa yanıt, sayı doğrulama, isteğe bağlı)*
   Açıklama: "Pro üyelik: reklamsız, sınırsız enerji, Ustalık Yolu (reklamlı Lite üyelik değil). TL olarak, yalnızca sayı yazın."
10. **Pro üyeliğin aylık fiyatı ne olursa "bu kadar ucuzsa bir sorunu vardır" diye şüphelenirdiniz?** *(Kısa yanıt, sayı doğrulama, isteğe bağlı)*
    Açıklama: (9. ile aynı)
11. **Uygulamada en çok neyi değiştirirdiniz?** *(Paragraf, isteğe bağlı)*

Not (forma girmez): 9–10. sorularda gerçek fiyatı (Pro aylık ₺99, Lite aylık ₺49) söyleme; cevaplar
söylenen fiyata yaslanır. Soru Pro'yu soruyor; Lite ve altın paketleri değil.

---

## Form 3 — Bırakanlar: "Kısa bir soru"

Ne zaman: Firestore'da **3 gündür girmeyen** testçiye WhatsApp'tan bu formun bağlantısıyla birlikte
kısa, suçlamayan bir mesaj (ör. "Sorobit'e bir süredir girilmemiş, sebebini öğrenmek bize çok yardımcı
olur, 1 dakikalık form:"). 14. günü bekleme; o zamana kadar sebep unutulur.

1. **Çocuğunuzun uygulamadaki kullanıcı adı nedir?** *(Kısa yanıt, zorunlu)*
2. **Çocuğunuz uygulamayı en son ne zaman açtı?** *(Çoktan seçmeli, zorunlu)*
   Bu hafta · Geçen hafta · Daha önce · Bilmiyorum
3. **Neden devam etmedi?** *(Onay kutuları, zorunlu)*
   - Sıkıldı
   - Anlamadı ya da zor geldi
   - Çok kolay geldi
   - Zaman olmadı
   - Unuttu
   - Can (enerji) bitince beklemek ya da reklamlar rahatsız etti
   - Teknik bir sorun oldu
   - Diğer: ___
4. **Ne olsaydı devam ederdi?** *(Paragraf, isteğe bağlı)*
5. **İleride tekrar denemek ister misiniz?** *(Çoktan seçmeli, isteğe bağlı)*
   Evet · Belki · Hayır

---

# Kursla test: öğretmen formları ve görüşme (22.10.2026)

Bir mental aritmetik kursuyla yapılan test için üç form ve bir görüşme var. Kurs velileri 1. günde
Form 1 yerine kısa **Form 1K**'yı doldurur. Sonrasında yukarıdaki Form 2 ve 3'ü aynen alırlar. Form 1
kurs dışı testçiler için olduğu gibi kalıyor. Takip tablosunda bu velilerin "Kanal" sütununa
**`Kurs: [kurs adı]`** yaz. Bu öğrenciler "kursa giden çocuk" grubunda değerlendirilir.

| Ne | Kim doldurur | Ne zaman |
|---|---|---|
| **Form 1K: Kurs velileri 1. gün** | Kurs velisi | Hesap açılınca (~2 dk) |
| **Form Ö1: Öğretmen görüşü** | Her öğretmen | Testin 10–12. günü (~8 dk) |
| **Form Ö2: Sorun ve istek bildirimi** | Kurs sorumlusu, öğretmen ya da veli | Her sorunda (1–2 dk) |
| **Öğretmen görüşmesi** (aşağıdaki soru listesi) | Sen + kurs sorumlusu + 1–2 öğretmen | Ö1 cevapları geldikten sonra, 12–14. gün |

Sıra bilinçli: önce form, sonra görüşme. Görüşmeye form cevaplarını ve uygulama verisini okumuş
olarak girersin; görüşmede yalnızca "neden?" sorusuna vakit kalır.

## Kurulum (bir kez)

1. https://script.google.com → formları oluşturan **mevcut** Apps Script projesini aç.
2. `Kod.gs`'nin içini tamamen sil, `docs/KAPALI_TEST_FORMLARI_OLUSTUR.gs`'nin güncel hâlini yapıştır, **Ctrl+S**.
3. Üstteki listeden **`kursFormlariOlustur`**'u seç, **Çalıştır**. (`formlariOlustur`'u DEĞİL: o
   veli formlarının ikinci kopyasını açar.)
4. **Yürütme günlüğü**nde iki formun gönderilecek bağlantısı yazar; aşağıdaki tabloya ve
   `docs/KAPALI_TEST_MESAJLARI.md` 11. bölümdeki `[Form Ö1]` / `[Form Ö2]` yerlerine yaz. Formlar
   Drive'daki "Sorobit kapalı test" klasörüne gider.
5. **Form Ö2 için bildirim aç:** formu Google Forms'ta aç → **Yanıtlar** → ⋮ → **Yeni yanıtlar
   için e-posta bildirimleri al**. Bildirim gelince bildirimi hemen görürsün. (Bu ayar betikle yapılamıyor.)

`kursFormlariOlustur` 22.10.2026 22:50'de çalıştırıldı; **bir daha çalıştırma** (ikinci kopya oluşur).

| Form | Bağlantı | Form kimliği (betikle düzeltme için) |
|---|---|---|
| Form Ö1: Öğretmen görüşü | https://forms.gle/ij9QM6BcjgzsFi8Q9 | `1ayq2BpD6q6UOIQZiBfi2GltbAMUs6UmIP_fg5cxp1X0` |
| Form Ö2: Sorun ve istek bildirimi | https://forms.gle/FgjRYeBeuWqBW1A88 | `16eZTr1OSDiRbYIyQ1kKcg2w0Xl8tGvbVg9mPVK6qMsU` |

Cevap tabloları ve düzenleme bağlantıları Drive'da **"Sorobit kapalı test"** klasöründe.

**Form 1K (ayrı işlev, bir kez):** aynı projede `Kod.gs`'nin içini sil, `.gs`'nin güncel hâlini
yapıştır, Ctrl+S, listeden **`kursVeliFormuOlustur`**'u seç, **Çalıştır**. Günlükte yazan bağlantıyı
aşağıdaki tabloya ve `docs/KAPALI_TEST_MESAJLARI.md` 11.3'teki `[Form 1K]` yerine yaz.

| Form | Bağlantı |
|---|---|
| Form 1K: Kurs velileri 1. gün | https://forms.gle/b6AaAaUwgc3T1sMX9 (kimlik `1ULGn6aIZc9x9Y_SsEkbi3ySnOmjk1o0cviZFZ-FFkKg`; 22.10.2026 23:28 oluşturuldu, bir daha çalıştırma) |

---

## Form 1K: "Sorobit testine hoş geldiniz" (kurs velileri, 1. gün)

Form 1'in kısaltılmışı. Açıklama ve teşekkür metni Form 1'le aynı, yalnızca süre "2 dakika".
Çıkarılanlar (kullanıcı kararı 22.10.2026): telefon, şehir, son bir yılda neler yapıldığı, aylık
harcama. Dert sorusu bilerek kaldı: şıkları yayından sonraki 5 mesajlık reklam testiyle aynı, takip
tablosundaki "en büyük dert" sütunu ve Özet'teki dertlere göre devam oranı bu soruya bağlı.

1. **Çocuğunuzun uygulamadaki kullanıcı adı nedir?** *(Kısa yanıt, zorunlu; Form 1'deki açıklamayla)*
2. **Çocuğunuz kaç yaşında?** *(Açılır liste, zorunlu; Form 1'le aynı)*
3. **Çocuğunuz kaçıncı sınıfta?** *(Açılır liste, zorunlu; Form 1'le aynı)*
4. **Çocuğunuz ne kadar süredir abaküs / mental aritmetik kursuna gidiyor?** *(Çoktan seçmeli, zorunlu)*
   3 aydan az · 3–6 ay · 6–12 ay · 1–2 yıl · 2 yıldan fazla
   (Form 1'deki "abaküs geçmişi" yerine: bu velilerin hepsi zaten kursta, asıl ayrım seviye.)
5. **Sorobit'ten beklentiniz nedir? Aklınızdan geçeni kendi cümlelerinizle yazın.** *(Paragraf, zorunlu)*
   (Form 1'deki "neden denediniz?" yerine: kurs velilerinin cevabı hep "öğretmen önerdi" olurdu.)
6. **Çocuğunuzun matematikle ilgili sizi en çok düşündüren durum hangisi?** *(Çoktan seçmeli, zorunlu; Form 1'le aynı şıklar)*
7. **Çocuğunuz uygulamayı hangi cihazda kullanacak?** *(Çoktan seçmeli, zorunlu; Form 1'le aynı)*

Takip tablosuna: kullanıcı adı, yaş, sınıf, en büyük dert. "Abaküs geçmişi" sütununa "Kursta (süre)"
yaz; şehir boş kalır.

---

## Form Ö1: "Sorobit öğretmen görüşü" (10–12. gün)

**Form açıklaması:**

> Sorobit'i öğrencilerinizle denediğiniz için çok teşekkürler! Bu form yaklaşık 8 dakika sürer.
> Bir mental aritmetik öğretmeninin gözünden görüşleriniz, uygulamanın ve öğretmen özelliklerinin
> nasıl gelişeceğini doğrudan belirleyecek. Eleştirileriniz en değerlisi. Cevaplarınız yalnızca
> Sorobit'i geliştirmek için kullanılır, kimseyle paylaşılmaz.

**Gönderince:** "Çok teşekkürler! Cevaplarınızı okuyup kısa bir görüşme için size ulaşacağım."

**Sizi tanıyalım**
1. **Adınız soyadınız** *(Kısa yanıt, zorunlu)*
2. **Kursunuzun adı** *(Kısa yanıt, zorunlu)*
3. **Uygulamadaki kullanıcı adınız** *(Kısa yanıt, isteğe bağlı)*
   Açıklama: "Profil ekranında, adın altında @ ile başlayan ad. Cevapladığınız soruları eşleştirmek için."
4. **Kaç yıldır abaküs / mental aritmetik öğretiyorsunuz?** *(Çoktan seçmeli, zorunlu)*
   1 yıldan az · 1–3 yıl · 4–7 yıl · 8 yıl ve üstü
5. **Hangi yaş gruplarına ders veriyorsunuz?** *(Onay kutuları, zorunlu)*
   Okul öncesi (4–6 yaş) · 1–2. sınıf · 3–4. sınıf · 5–6. sınıf · 7–8. sınıf · Diğer: ___
6. **Testte kaç öğrenciniz Sorobit'i kullandı?** *(Çoktan seçmeli, zorunlu)*
   Hiç · 1–5 · 6–10 · 11–20 · 20'den fazla · Bilmiyorum

**Sorobit'i nasıl kullandınız?**

7. **Sorobit'i öğrencilerinizle nasıl kullandınız?** *(Onay kutuları, zorunlu)*
   Derste birlikte açtık · Ev ödevi olarak verdim · Öğrencilere / velilere önerdim, kendileri kullandı ·
   Yalnızca kendim denedim · Henüz kullanmadık · Diğer: ___
8. **Uygulamayı kendiniz ne kadar denediniz?** *(Çoktan seçmeli, zorunlu)*
   Yalnızca birkaç dakika baktım · Birkaç dersi kendim yaptım · Bir üniteyi ya da daha fazlasını bitirdim

**Öğrencileriniz**

9. **Öğrencileriniz uygulamada nerede takıldı ya da zorlandı?** *(Paragraf, zorunlu)*
   Açıklama: "Ders adını ya da ekranı yazabilirseniz çok yardımcı olur. Takılan olmadıysa 'yok' yazın."
10. **Sorobit en çok hangi öğrencilerinize uygun?** *(Çoktan seçmeli, isteğe bağlı)*
    Abaküse yeni başlayanlara · Temel hareketleri bilen orta seviyeye · İleri seviyeye ·
    Hepsine aynı ölçüde · Söylemek için erken

**Abaküs yöntemi**

11. **Uygulamadaki abaküs anlatımı, kursta öğrettiğiniz yöntemle ne kadar uyumlu?** *(Çoktan seçmeli, zorunlu)*
    Açıklama: "Örneğin boncuk hareketleri, 5'lik ve 10'luk geçişler, parmak kullanımı, körleme."
    Tamamen uyumlu · Büyük ölçüde uyumlu, küçük farklar var · Önemli farklar var ·
    Çok farklı, öğrencinin kafasını karıştırır · Değerlendirecek kadar bakmadım
12. **Farklı ya da hatalı bulduğunuz yerleri yazar mısınız?** *(Paragraf, isteğe bağlı)*
13. **Derslerin sırası ve zorluk artışı nasıl?** *(Çoktan seçmeli, isteğe bağlı)*
    Çok yavaş ilerliyor · Uygun · Çok hızlı ilerliyor · Bazı yerlerde birden zorlaşıyor

**Öğretmene soru**

14. **Öğrencilerin uygulamadan gönderdiği soruları cevaplayabildiniz mi?** *(Çoktan seçmeli, zorunlu)*
    Evet, soru cevapladım · Bana soru gelmedi · Nasıl yapılacağını bulamadım · Vaktim olmadı ·
    Hesabım henüz onaylanmamıştı
15. **Soru cevaplarken zorlandığınız ya da eksik bulduğunuz bir şey oldu mu?** *(Paragraf, isteğe bağlı)*

**Öğretmen özellikleri**: bölüm açıklaması: "Testten sonra öğretmenlere özel özellikler
ekleyeceğim. Hangisini önce yapacağımı sizin cevaplarınız belirleyecek."

16. **Öğretmen olarak Sorobit'te en çok neyi görmek ya da yapabilmek isterdiniz? Kendi cümlelerinizle yazın.** *(Paragraf, zorunlu)*
17. **Aşağıdakiler sizin için ne kadar önemli?** *(Çoktan seçmeli tablo, zorunlu)*
    Sütunlar: Olmazsa olmaz · Olsa iyi olur · Gerek yok. Satırlar:
    - Öğrencilerimin ilerlemesini görmek (bitirdiği dersler, çalışma süresi, seri)
    - Öğrencilerimin en çok hata yaptığı işlemleri görmek
    - Ödev vermek ve yapılıp yapılmadığını takip etmek
    - Kendi alıştırma setimi hazırlamak (ör. 20 soruluk 3 basamaklı toplama)
    - Öğrencilerimi sınıf ya da grup olarak toplamak
    - Soruların yalnızca kendi öğrencilerimden gelmesi
    - Velilere haftalık ilerleme özeti gitmesi
    - Sınıf içi yarışma ve liderlik tablosu
    - Seviye belirleme sınavı
18. **Bunlardan yalnızca biri yapılabilseydi hangisi olsun?** *(Çoktan seçmeli, zorunlu; 17'deki satırlar)*

**Veliler**

19. **Sorobit'i velilere nasıl anlattınız ya da anlatırdınız? Kullandığınız cümleleri yazın.** *(Paragraf, zorunlu)*
20. **Velilerden bir soru, itiraz ya da tepki geldi mi?** *(Paragraf, isteğe bağlı)*

**Genel**

21. **Test bittikten sonra Sorobit'i öğrencilerinize kullandırmayı düşünür müsünüz?** *(Çoktan seçmeli, zorunlu)*
    Evet, bütün öğrencilerime · Evet, bazı öğrencilerime · Öğretmen özellikleri gelirse ·
    Emin değilim · Hayır
22. **Sorobit'i başka bir abaküs öğretmenine ne kadar önerirsiniz?** *(Doğrusal ölçek 0–10, zorunlu)*
    0 = "Hiç önermem", 10 = "Kesinlikle öneririm"
23. **Uygulamada en çok neyi değiştirirdiniz?** *(Paragraf, isteğe bağlı)*

Notlar (forma girmez):
- 16 (açık uçlu) bilerek 17'den (liste) önce: liste önce gelirse öğretmen kendi ihtiyacını değil
  listedekini yazar. Veli formlarındaki "neden" sorusuyla aynı kural.
- 18, 17'de her şeye "olmazsa olmaz" diyen öğretmeni seçim yapmaya zorluyor. Asıl öncelik sinyali 18'de.
- "Soruların yalnızca kendi öğrencilerimden gelmesi": şu an öğrenci soruları bütün onaylı
  öğretmenlerin gördüğü **ortak havuza** düşüyor (`functions/index.js` `runTeacherPoolNotify`). Kurs
  öğretmeni, havuzda kendi öğrencisinin sorusunu ayırt edemiyor. Bunu öğretmenlere baştan söyle.
- Fiyat ve iş birliği bu formda yok. Bunlar görüşmede konuşulacak, çünkü kursun durumuna göre
  soru değişiyor.
- 14'te "Hesabım henüz onaylanmamıştı" çok gelirse onay gecikiyor demektir. Öğretmen kaydolduğu gün onayla.

---

## Form Ö2: "Sorobit: sorun ve istek bildirimi" (sürekli açık)

Hataları ve istekleri tek yerde toplar. Kurs sorumlusu ya da öğretmen doldurur. Velilerin sorunu
sorumluya gelir, formu sorumlu doldurur. Play üretim başvurusu "geri bildirimi nasıl topladınız"
diye soruyor; cevap tablosu bunun kaydı olur.

Dosya yükleme sorusu **bilerek yok**: Google Forms'ta dosya yükleme, Google hesabıyla giriş
istiyor. Ekran görüntüsü ve kayıt WhatsApp'tan gelir.

**Form açıklaması:**

> Sorobit'te bir hata, takıldığınız bir yer ya da bir isteğiniz mi var? Bu form 1–2 dakika sürer.
> Ekran görüntüsü ya da ekran kaydı çok yardımcı olur: formu gönderdikten sonra WhatsApp'tan
> kurs sorumlunuza ya da doğrudan Başar'a iletin.

**Gönderince:** "Teşekkürler, bildiriminiz ulaştı! Ekran görüntüsü varsa WhatsApp'tan göndermeyi unutmayın."

1. **Bildiren kim?** *(Çoktan seçmeli, zorunlu)*
   Öğretmen · Öğrenci / veli · Kurs sorumlusu
2. **Adınız** *(Kısa yanıt, isteğe bağlı)*
3. **Ne bildiriyorsunuz?** *(Çoktan seçmeli, zorunlu)*
   Hata ya da teknik sorun · Takılınan / anlaşılmayan bir yer · İstek ya da öneri · Soru
4. **Sorunu yaşayan kişinin uygulamadaki kullanıcı adı** *(Kısa yanıt, isteğe bağlı)*
   Açıklama: "Profil ekranında @ ile başlayan ad. Hatayı bulmayı çok kolaylaştırır."
5. **Ne oldu? Ne yaparken oldu?** *(Paragraf, zorunlu)*
   Açıklama: "Örnek: '3. bölümün 2. dersinde Devam'a basınca ekran dondu.' İstekse ne olmasını
   istediğinizi yazın."
6. **Ne zaman oldu?** *(Tarih, isteğe bağlı)*
7. **Bir kez mi oldu, tekrar ediyor mu?** *(Çoktan seçmeli, isteğe bağlı)*
   Bir kez oldu · Ara sıra oluyor · Her seferinde oluyor
8. **Telefon markası ve modeli** *(Kısa yanıt, isteğe bağlı)*
   Açıklama: "Ayarlar → Telefon hakkında'da yazar (ör. Samsung Galaxy A14)."
9. **Ekran görüntüsü ya da kaydı var mı?** *(Çoktan seçmeli, isteğe bağlı)*
   Var, WhatsApp'tan gönderiyorum · Yok

Rutin: bildirim e-postası gelince cevap tablosunda yeni bir **"Durum"** sütununa (Yeni / Bakılıyor /
Düzeltildi (sürüm) / Test sonrasına / Yapılmayacak) not düş. Haftada bir, kurs sorumlusuna
"şunlar düzeldi, şunlar test sonrasına kaldı" diye kısa bir özet gönder. Bildirimin karşılık
bulduğunu gören kişi bildirmeye devam eder.

---

## Öğretmen görüşmesi: soru listesi (12–14. gün, 20–30 dk)

Kurs sorumlusu ve 1–2 öğretmenle yüz yüze ya da görüntülü görüşme. Satış değil, öğrenme görüşmesi.

**Hazırlık (görüşmeden önce):**
- Form Ö1 cevaplarını oku. Her öğretmenin 16–18. sorudaki cevabını ve 11–12'deki yöntem farklarını not et.
- `node tools/kapali-test/ilerleme_disa_aktar.js --testci-haric` çalıştır, kursun öğrencilerini
  (takip tablosunda `Kurs: …`) ayır. Kaç kişi başladı, kaç gün kullandı, en çok hangi derste kaldı?
  Görüşmede göstereceğin 1–2 somut örnek hazırla ("6 öğrenciden 4'ü X dersinde durdu").
- Form Ö2'de gelen bildirimlerin hangilerinin düzeldiğini bil.

**Görüşme kuralları:**
- **Gelecek değil, geçmiş sor.** "Kullanır mısınız?" sorusuna herkes kibarlıktan "evet" der. Bunun yerine
  "En son ne zaman…, ne oldu?" diye sor. (Bu yaklaşım *The Mom Test* kitabındaki yöntem: insanların
  fikrini değil, yaptığını sor.)
- **Savunma yapma.** Eleştiriye "aslında şöyle…" diye karşılık verme, "Biraz daha anlatır mısınız?" de.
- **Az konuş.** Konuşma süresinin üçte birinden fazlası senin olmasın. Sessizliğe dayan; asıl bilgi genelde sessizlikten sonra gelir.
- **Söylenen cümleyi aynen yaz.** Özellikle velilere anlatım ve şikâyet cümleleri, ileride reklam ve mağaza metni olur.
- **Söz verme.** "Not aldım, önceliklendireceğim" de, tarih verme.

**1. Açılış (2 dk)**
- Teşekkür et. Amacı söyle: "Doğru şeyi yapmak için sizden öğrenmek istiyorum, eleştiriler en değerlisi."
- Not almak için ses kaydına izin iste.

**2. Gerçekte ne oldu (5 dk)**
- "Sorobit'i öğrencilere ilk nasıl verdiniz? Anlatır mısınız?"
- "En son ne zaman bir öğrencinin kullandığını gördünüz ya da duydunuz? Ne yapıyordu?"
- "Kullanmayan öğrenciler sizce neden kullanmadı?"
- "Derste mi kullandınız, ev ödevi olarak mı? Neden öyle?"

**3. Öğrencilerin takıldığı yerler (5 dk)**
- Veriyi göster: "[N] öğrenciden [M]'si [ders] dersinde durdu. Sizce neden?"
- "Bu konuyu kursta nasıl anlatıyorsunuz? Uygulamadaki anlatımdan farkı ne?"
- Formda yöntem farkı yazdıysa: "Şunu yazmışsınız: [...] Öğrenci bunu görünce ne oluyor?"

**4. Öğretmen özellikleri (7 dk)**: en önemli bölüm
- "Şu an öğrencilerinizin evde çalışıp çalışmadığını nasıl takip ediyorsunuz?" (WhatsApp, kâğıt
  ödev, başka bir uygulama? Bugünkü çözüm, rakibin ne olduğunu gösterir.)
- "Bu takibe haftada ne kadar zaman harcıyorsunuz? En sinir bozucu kısmı ne?"
- "Bugün ödevi nasıl veriyorsunuz? Sayfa mı, soru sayısı mı, dakika mı?" (Ödev özelliğinin birimini belirler.)
- "Formda [X]'i seçmişsiniz. Bu olsaydı haftanızda ne değişirdi?"
- "Öğrencilerin ilerlemesini telefonda mı görmek istersiniz, bilgisayarda mı?"
- "Öğrenci soruları şu an ortak bir havuza düşüyor. Kendi öğrencinizin sorusunu ayırt etmek sizin için ne kadar önemli?"

**5. Veliler (3 dk)**
- "Velilere Sorobit'i nasıl anlattınız? Hangi cümle işe yaradı?"
- "Veliler evde çalışma konusunda size en çok neyden şikâyet ediyor?"
- "Kurs olarak velilere daha önce ücretli bir kaynak ya da uygulama önerdiniz mi? Nasıl karşılandı?"

**6. İş birliği (5 dk, kurs sorumlusuyla)**
- "Öğrencilerinize böyle bir uygulama önermek kursa ne kazandırır?"
- "Daha önce benzer bir uygulama ya da kaynakla çalıştınız mı? Ücret nasıldı, kim ödüyordu?"
- "Böyle bir uygulamanın ücretini kim öder: kurs mu, veli mi?"
- "Öğrenci başına aylık hangi fiyat 'bu çok pahalı' olur, hangisi 'bu kadar ucuzsa bir sorunu vardır'?"
- **Kendi teklifini (kursa ücretsiz öğretmen paneli, öğrencilere indirim, %20–30 pay) burada
  söyleme.** Önce onların beklentisini dinle. Teklif, test verisiyle ayrı bir görüşmede yapılır.

**7. Kapanış (2 dk)**
- "Sormamı beklediğiniz ama sormadığım bir şey var mı?"
- "Konuşmamı önereceğiniz başka bir öğretmen ya da kurs var mı?"
- Sonraki adımı ve ne zaman tekrar konuşacağınızı belirle.

**Görüşmeden sonra (aynı gün):** 5 satırlık özet yaz ve `docs/DEVAM_NOTU.md`'ye ekle:
1. En çok tekrar eden 3 sorun
2. Öncelikli öğretmen özelliği (18. sorudaki cevapla aynı mı?)
3. Aynen alıntılar (veliye anlatım, şikâyetler)
4. Fiyat ve ödeme modeli cevapları
5. Sonraki adım


# Kapalı test formları (19.10.2026)

Üç form var: **1. gün**, **7–10. gün**, **bırakanlar**. Ayrıca seçilen 10–15 aileyle yapılacak telefon
görüşmesi için kısa bir rehber var (en altta).

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
| Form 2'den sonra | "Görüşmeye açığım" diyenlerden A ve B grubundan birkaçıyla telefonla konuş (rehber en altta). Notu "Görüşme notu"na yaz. |
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
12. **Birkaç gün sonra 10 dakikalık kısa bir telefon görüşmesine açık mısınız?** *(Çoktan seçmeli, zorunlu)*
    Evet · Hayır

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
9. **Uygulamanın aylık üyeliği hangi fiyatta "bu çok pahalı, almam" derdiniz?** *(Kısa yanıt, sayı doğrulama, isteğe bağlı)*
   Açıklama: "TL olarak, yalnızca sayı yazın."
10. **Hangi fiyatta "bu kadar ucuzsa bir sorunu vardır" diye şüphelenirdiniz?** *(Kısa yanıt, sayı doğrulama, isteğe bağlı)*
    Açıklama: "TL olarak, yalnızca sayı yazın."
11. **Uygulamada en çok neyi değiştirirdiniz?** *(Paragraf, isteğe bağlı)*

Not (forma girmez): 9–10. sorularda gerçek fiyatı söyleme; cevaplar söylenen fiyata yaslanır.
Fiyat, Pro aboneliğin aylık fiyatıdır (altın paketleri değil).

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

## Telefon görüşmesi rehberi (10 dakika, seçilen 10–15 aile)

Kime: A grubundan (devam eden, tavsiye puanı 9–10) en istekliler + B grubundan (bırakan) birkaç kişi.
Yalnızca 1. gün formunda "görüşmeye açığım" diyenler.

1. "Sorobit'i ilk duyduğunuzda aklınızdan ne geçti?" (kendi kelimeleri; reklam dili için birebir not al)
2. "Son bir haftada çocuğunuz uygulamayı nasıl kullandı? Bir gününü anlatır mısınız?"
3. "Takıldığı bir an oldu mu? O an ne yaptı?" (öğretmene soru sordu mu, bıraktı mı)
4. Kursa giden çocukta: "Kurstaki öğretmeninin anlattığıyla uygulamanın anlattığı arasında fark var mı?"
5. "Bu uygulama olmasaydı onun yerine ne yapardınız?" (asıl rakibi bulmak için)
6. "Para ödemeyi düşündüren ne olurdu? Ne olursa asla ödemezdiniz?"

Kural: Övgüyü değil geçmişteki davranışı dinle ("ne yaptınız", "ne harcadınız"). Görüşme notlarını
takip tablosundaki "Görüşme notu" sütununa yaz.

# "Sobi'ye sor" — sohbet eden yardımcı, uygulama planı

28.10.2026 · Durum: **plan, kod yazılmadı.**
Karar geçmişi: ilk taslak yapay zekâsız bir yardım paneliydi. Kullanıcı aynı gün öğrencinin
yapay zekâyla **sohbet ederek** anlamasını istedi ("3 gider 5 gelir demektense sohbet ederek
anlasa"); plan bu yöne çevrildi.

## Sorun

Öğrenci takıldığında elindeki tek yol "Öğretmene sor": ekran kaydı ya da görüntü al, gönder,
cevabı bekle. Bu yol kredi istiyor, çok adımlı ve cevap geç geliyor. 7–10 yaşındaki bir çocuk bu
yolu çoğu zaman kullanmaz, dersi bırakır.

## Ana fikir

Öğretici adımında "Anlamadım" diyen çocuk **Sobi'yle sohbet eder**. Sobi uygulamanın sistemini
bilir, çocuğun nerede takıldığını kendiliğinden görür, soru sorarak yönlendirir ve gerektiğinde
boncukları çocuğun abaküsünde oynatarak gösterir.

Temel kural: **sohbeti dil modeli, hesabı kural motoru yapar.** Dil modelleri boncuk aritmetiğinde
ve 5'lik/10'luk kurallarında yanlış adım uydurabiliyor. Bu yüzden model hiçbir hamleyi kafadan
söylemez, uygulamanın fonksiyonlarını araç olarak çağırır (tool use) ve onların sonucunu anlatır.

## "Sistemi öğretmek": üç parça

### 1. Talimat metni (sistem istemi)

Her sohbetin başında modele verilen sabit yönerge. Sürüm numarasıyla depoda tutulur
(`functions/sobi/prompt_tr.md` gibi), değişiklikleri test setinden geçer.

- Soroban kuralları ve uygulamadaki adları: kuralsız, 5'lik, 10'luk, boncuk kuralı;
  `functions/homework.js` → kural adlarıyla aynı dil.
- Sobi'nin karakteri: neşeli, sabırlı tavşan; uygulamadaki rehber paneliyle aynı ses.
- Ders verme üslubu:
  - Cevabı hemen söyleme, soru sorarak yönlendir.
  - Kısa cümleler, en fazla 2–3 cümle. Çocuk ekran başında uzun metin okumaz.
  - Doğru cevapta öv, yanlışta cesaretlendir. "Yanlış" kelimesini değil "bir daha bakalım"ı kullan.
- Kesin kurallar:
  - Hiçbir hamleyi, sonucu ya da kuralı araç çağırmadan söyleme.
  - Yalnızca soroban, ders ve uygulamanın kullanımı hakkında konuş. Konu dışı soruda kibarca
    derse dön.
  - Kişisel bilgi sorma; çocuk verirse tekrar etme.
- Dil: uygulamanın dilinde cevap ver. Talimat metni her dil için ayrı yazılır
  (İngilizce → Endonezce sırası, bkz. marka planı).

### 2. Anlık bağlam

"Anlamadım"a basıldığında uygulama, çocuk hiçbir şey yazmadan şunları gönderir:

| Alan | Kaynak |
|---|---|
| Adım kimliği, adım metni, soru metni | `TutorialFragment.TutorialStep` → `id`, `text`, `questionText` |
| Beklenen cevap | `TutorialStep.answerNumber` |
| Adımın başındaki ve şu anki abaküs değeri | `abacusController.getCurrentValue()` (adım gösterilirken kaydedilir) |
| Bu adımdaki değer kaydı | `AbacusBeadController.onValueChanged` ile tutulur (ödevdeki `RuleAnalysis.Sample` gibi) |
| Kaç kez yanlış yaptığı | `TutorialStepAnalytics` deneme sayacı |
| Öğreticinin numarası ve konusu | `tutorialNumber`, ders başlığı |

Kişisel bilgi (ad, e-posta, yaş) **gönderilmez.**

### 3. Araçlar

Model bunları çağırır; hepsi sunucuda, kural motoruyla çalışır:

| Araç | Ne yapar |
|---|---|
| `hamleleri_hesapla(baslangic, islem)` | Sütun sütun hamleler: kural, oynayan boncuklar, ara değer. `AbacusMoves` / `rulesOf` mantığı |
| `hatayi_bul(baslangic, islem, deger_kaydi)` | Çocuğun ilk yanlış hamlesi ve orada gereken kural. `RuleAnalysis.align` mantığı |
| `kurali_acikla(kural, islem)` | O kuralın onaylanmış kısa açıklaması (`strings` metinleri); model bunun üstüne konuşur |
| `abakuste_goster(hamleler)` | Cevapla birlikte uygulamaya döner, uygulama boncukları `BeadAnimation` ile oynatır |
| `soru_sor(baslangic, islem)` | Çocuğa denemesi için küçük bir alıştırma önerir; uygulama abaküsü hazırlar, çocuğun cevabını kural motoru denetler |

Kural motoru şu an yalnızca Kotlin'de var (`homework/HomeworkEngine.kt`, `homework/RuleAnalysis.kt`).
Araçlar sunucuda çalışacağı için mantığın JavaScript karşılığı `functions/` altına yazılır. İki
kopyanın ayrışmaması için **ortak test tablosu** tutulur: aynı girdi listesi hem Kotlin birim
testinde hem Node testinde çalışır, sonuçlar karşılaştırılır.

Örnek sohbet:

> **Çocuk:** Neden 5'i indirdik, 3 ekleyecektik?
> **Sobi:** *(`hamleleri_hesapla(4, +3)` → 5'lik kural)* Çok güzel soru! Birler sütununa bak, kaç
> tane alt boncuk kaldı?
> **Çocuk:** Hiç kalmadı.
> **Sobi:** Aynen öyle! 3 boncuk ekleyecek yer yok. O yüzden 5'i indiriyoruz, fazla gelen 2'yi geri
> alıyoruz. İzle! *(`abakuste_goster`)*

## Çocuk kullanıcıya özel tasarım

### Yazmadan sormak

- **Sesli soru:** mikrofon düğmesi, Android `SpeechRecognizer` (mümkünse cihaz içi tanıma). Ses
  dosyası sunucuya gitmez, yalnızca metne çevrilmiş hâli gider.
- **Öneri balonları:** her cevabın altında 2–3 hazır soru ("Hangi boncuk?", "Neden böyle?",
  "Bir daha göster"). Çocuğun çoğu zaman yazmasına gerek kalmaz.
- **Sesli cevap (isteğe bağlı):** Sobi'nin cevabını cihazın metin okuma motoru okur. Ses ayarları
  `SoundSettingsFragment`'ta zaten var.

### Güvenlik katmanları

1. **Girişte:** çocuğun metnindeki ad, telefon, adres, e-posta benzeri kalıplar sunucuda maskelenip
   öyle gönderilir. Uzunluk sınırı (ör. 200 karakter).
2. **Talimat metninde:** konu sınırı ve üslup kuralları.
3. **Çıkışta:** modelin cevabındaki her sayı ve kural adı o turdaki araç sonuçlarıyla
   karşılaştırılır. Tutmazsa cevap gösterilmez; yerine kural motorunun onaylı açıklaması
   (`kurali_acikla`) gelir ve olay kaydedilir. Uygunsuz içerik denetimi de burada yapılır.
4. **Bildir düğmesi:** her Sobi mesajında. Google Play, üretken yapay zekâ içeren uygulamalarda
   kullanıcının uygunsuz cevabı bildirebilmesini istiyor. Bildirimler mevcut hata bildirimi
   altyapısına (`BugReports`) ya da ayrı bir koleksiyona düşer.
5. **Tur sınırı:** bir sohbette en fazla ~10 tur; sonra "Öğretmenine soralım mı?" önerisi.

### Yasal ve gizlilik

- Gizlilik politikası (`public/privacy-policy.html`) ve ebeveyn onay e-postasındaki veri listesi
  (`functions/index.js` ~925) güncellenir: "Çocuğunuzun Sobi'ye yazdığı veya söylediği sorular,
  cevap üretmek için bir yapay zekâ hizmetine gönderilir."
- Sohbetler kısa süre saklanır (ör. 30 gün), sonra silinir; ebeveyn panelinde
  (`ParentPanelFragment`) görüntülenebilir.
- API verisini model eğitiminde kullanmayan bir sağlayıcı seçilir; sözleşme koşulları yayından
  önce kontrol edilir.
- Yayından önce kontrol: Google Play Aileler politikası, yapay zekâ üretimli içerik politikası,
  Veri güvenliği formu; KVKK aydınlatma metni.
- Ebeveyn onayı olmayan hesapta Sobi sohbeti kapalı, yalnızca "Bana göster" (yapay zekâsız) açık.

### Maliyet

- Sabit talimat metni ve araç tanımları istem önbelleğine alınır (her mesajda yeniden
  ücretlendirilmez).
- Günlük sınır: ücretsiz kullanıcıya günde birkaç sohbet, Pro'da yüksek sınır.
  "Sobi'ye sınırsız sor" Pro'nun satış maddesi olabilir (bkz. `ProFlow`, `BillingCatalog`).
- Model seçimi kalite testiyle yapılır: önce küçük hızlı model denenir; test setinde üslup ya da
  araç kullanımı yetersizse bir üst model.
- Sınır ve sayaç sunucuda, `askTeacherQuestion`'daki kredi transaction'ı gibi.

## Mimari

```
Uygulama (SobiChatPanel)                 Cloud Functions (sobiChat, onCall)              Model API
 ─ bağlam + çocuğun mesajı  ──────────▶  ─ oturum/ban/sınır kontrolü
                                         ─ kişisel bilgi maskeleme
                                         ─ talimat + bağlam + geçmiş  ───────────────▶  cevap / araç isteği
                                         ─ araçları kural motoruyla çalıştır  ◀───────
                                         ─ çıkış denetimi (sayılar, içerik)
 ◀──────────  metin + öneri balonları + gösterilecek hamleler
 ─ boncukları oynat, sesli oku
```

- API anahtarı uygulamada **değil**, Functions gizli değişkeninde (secret).
- Sohbet geçmişi Firestore'da (`sobiChats/{uid}/sessions/{id}`), yazma yalnızca sunucudan;
  kurallar istemciye yalnızca okuma açar.
- `functions/` mevcut v1 `onCall` düzeninde; yeni kod ayrı modül (`functions/sobi.js`), `index.js`
  yalnızca dışa aktarır (`homework.js`, `classes.js` gibi).

## Uygulama tarafı

- Yeni dosyalar: `SobiChatPanel.kt` (sohbet arayüzü), `SobiContext.kt` (bağlam toplama),
  `ServerSobi.kt` (callable çağrısı, `ServerQuestions.kt` gibi).
- `TutorialFragment` 19 bin satır ve ders sonu akışı hassas (devam notundaki anlaşma). Ona yalnızca
  bağlantı noktası eklenir: adım başında değeri kaydetmek, `infoRequest` düğmesinden paneli açmak,
  panelden gelen hamleleri mevcut animasyon yoluyla oynatmak. Ders sonu zincirine dokunulmaz.
- Kendiliğinden öneri: aynı adımda 2. yanlış denemede Sobi rehber panelinden "Birlikte bakalım mı?"
  der. Çocuk düğmeyi bilmese de yardım ona gelir.
- İnternet yoksa ya da sınır dolduysa: "Bana göster" (kural motoru + animasyon, yapay zekâsız)
  her zaman çalışır.

## Kalite: yayından önce test seti

- 96 soru adımının her biri için çocukların sorabileceği 3–5 soru (doğru anlamamış, yanlış
  anlamış, konu dışı, kişisel bilgi veren, "cevabı söyle" diyen). Toplam ~400 örnek.
- Otomatik denetim: cevaptaki sayılar/kurallar araç sonuçlarıyla tutuyor mu, cevap uzunluğu,
  doğrudan cevap verme oranı, konu dışına çıkma.
- Elle gözden geçirme: üslup, çocuğa uygunluk. Kullanıcı (geliştirici) her talimat değişikliğinde
  bir örneklem okur.
- Test seti depoda: `tools/sobi-eval/` (`functions/` dışında, çünkü oraya her push bütün
  fonksiyonları deploy ediyor). Durum ve sonuç: aşağıda "Faz 3 durumu".

## Ölçüm

GA4'te boyut kotası dolu: **yeni parametre açılmaz**, mevcut kayıtlı parametreler yeniden kullanılır.

- `sobi_chat_open` (adım kimliği; kaynak: düğme / kendiliğinden öneri)
- `sobi_chat_turn` (tur sayısı; sesli mi, balon mu, yazı mı)
- `sobi_chat_report` (bildir düğmesi)
- Sonuç: sohbetten sonra adım geçildi mi → mevcut `tutorial_step_answer` ile birleştirilir

Başarı ölçütü: en zor 10 adımda (Faz 0) sohbet edenlerin geçme oranı ve dersi bırakma oranı;
bildirim oranı.

## Fazlar

| Faz | İş | Tahmin | Not |
|---|---|---|---|
| 0 | GA4'ten en zor 10 adım ve "Öğretmene sor" kullanım oranı | 1 gün | Kod yok |
| 1 | Kural motoru: Kotlin `StepExplainer` + JS karşılığı + ortak test tablosu | **bitti (28.10.2026)** | Aşağıda "Faz 1 durumu" |
| 2 | `sobiChat` fonksiyonu: talimat metni, araçlar, güvenlik katmanları, sınır | **kod bitti, secret kuruldu, index.js'e bağlandı (28.10.2026)** | Aşağıda "Faz 2 durumu" |
| 3 | Test seti ve talimat metninin olgunlaştırılması | **test seti kuruldu, ilk ölçüm yapıldı (29.10.2026)**; düzeltmeler bekliyor | Aşağıda "Faz 3 durumu" |
| 4 | Sohbet paneli: sohbet, öneri balonları, sesli soru, abaküste gösterim | **ilk sürüm yazıldı (28.10.2026)**, sesli soru/cevap ve bildir sonra | Aşağıda "Faz 4 durumu" |
| 5 | Gizlilik politikası, ebeveyn onayı, Play beyanları | 1 gün | Yayından önce şart |
| 6 | Kapalı testte pilot (sınırlı grup), ölçüm | — | |
| 7 | Ders alıştırması (`AbacusFragment`) ve ödeve genişletme | 2–3 gün | Aynı araçlar |
| 8 | Öğretmene sor: video yerine sohbet özeti + bağlam paketi | 3 gün | Aşağıda |

### Faz 1 durumu (28.10.2026)

- `app/.../abacus/StepExplainer.kt`: `explain(base, op, number)`, `explainTransition(base, target)`,
  `rulesUsed`, `encode`. Sınıflandırma ödevdeki `AbacusMoves`'tan; adım sırası öğreticilerdeki
  anlatımla aynı (toplama: "5 gelir, kardeş gider", "10 gelir, büyük kardeş gider",
  "(x − 5) boncuk gelir, 5 gider, 10 gelir"; çıkarma: tersleri, boncuk çıkarmada
  "10 gider, 5 gelir, kardeş gider"). Elde zinciri iç içe hareket olarak (`Step.carry`).
- `functions/sobi/stepExplainer.js`: aynı mantığın sunucu kopyası. `index.js`'e bağlı değil,
  henüz hiçbir fonksiyon kullanmıyor.
- `shared/step_explainer_cases.json`: 2108 satırlık ortak tablo (her tek rakam durumu sol sütun
  bağlamıyla + elde zincirleri + 300 rastgele çok basamaklı). Kotlin testi üretir
  (`GEN_STEP_CASES=1`), iki taraf da okuyup karşılaştırır.
- Testler: `StepExplainerTest` (14 test: öğretici örnekleri elle, 0..9999 tabanında bağımsız boncuk
  simülasyonu, 30 bin rastgele işlem, kural kümesi `RuleAnalysis.rulesOf` ile aynı, tablo) ve
  `npm run test:step-explainer` (tablonun tamamı + bağımsız simülasyon). İkisi de geçiyor.
- Henüz yok: çocuğa gösterilecek metin (Faz 2'de talimat metni ve `kurali_acikla` ile gelecek),
  animasyon bağlantısı (Faz 4).

### Faz 2 durumu (28.10.2026)

Kod `functions/sobi/` altında. ANTHROPIC_API_KEY Secret Manager'a kaydedildi (28.10.2026, sürüm 1) ve `sobiChat` `index.js`'e bağlandı; hesap silinince `sobiChats/{uid}` de siliniyor (`cleanupUserOnDelete`).

| Dosya | İş |
|---|---|
| `prompt_tr.md` | Talimat metni: Sobi'nin karakteri, dersin dili, "hesap yapma, araç kullan", üslup, sınırlar, bağlam biçimi |
| `tools.js` | Beş araç (`hamleleri_hesapla`, `hatayi_bul`, `kurali_acikla`, `abakuste_goster`, `alistirma_ver`), hepsi `strict`; anlatım dersteki sözlerle ("1 boncuk gelir, 5 gider, 10 gelir"); kuralların onaylı açıklamaları |
| `guard.js` | Girişte e-posta/telefon/bağlantı maskeleme, 300 karakter sınırı; çıkışta "her sayı bir kaynakta geçmeli" denetimi, 500 karakter sınırı; denetime takılırsa kural motorundan yedek metin |
| `chat.js` | Bir sohbet turu: elle yazılmış araç döngüsü (en çok 4 tur), ret ve yarıda kesilen cevap geçmişe eklenmiyor, geçmiş yalnızca eklenerek büyüyor (düşünme blokları değişmeden geri gidiyor), öneri balonları |
| `callable.js` | `sobiChat` callable fabrikası: oturum, günlük hak (Free 20 / Lite 50 / Pro 100 mesaj), sohbet başına 10 mesaj, `sobiChats/{uid}/sessions/{id}` kaydı (30 gün), API hatasında yedek metin ve hak düşmüyor |

- Model `claude-haiku-5-5` (kullanıcı kararı 28.10.2026: hesabı kural motoru yaptığı için küçük
  model yeterli olmalı), effort `medium`, istem önbelleği açık. Haiku 5.5'te sunucu tarafı yedek
  model yok; ret çocuğa yedek metin olarak döner. Opus 5.5 ile karşılaştırma:
  `node scripts/sobi-dene.js --model claude-opus-5-5` (orada `fallbacks: "default"` açık).
  Effort karşılaştırması: `--effort low`.

#### Gerçek model denemeleri (28.10.2026, `scripts/sobi-dene.js`, 3 senaryo × 7 mesaj)

Ölçülen maliyet (önbellek dahil, yaklaşık): Haiku 5.5 **mesaj başına ~0,05 sent**, Opus 5.5
**~2 sent**. Günün bütün denemeleri toplam 20 sentin altında.

İlk denemede bulunanlar ve yapılan düzeltmeler:

| Bulgu | Model | Düzeltme |
|---|---|---|
| "Birler sütununda 3 boncuk var" doğru ama 3 kaynakta yalnızca 23'ün rakamı olarak geçiyor → yanlış alarm | ikisi de | Bağlamda ve araç sonuçlarında sütun dökümü (`columnsOf`) |
| 8 için "alt boncuklardan 2'si boş" (1'i boş); beşliğe "5 boncuk" | Haiku | Dökümde `birlik_bosta`; talimatta "beşlik boncuk" dili zorunlu |
| "Neden 5 gidiyor?" gerekçesini uyduruyor ("6 = 1 + 5") | ikisi de | Her hamleye hesabı tutan onaylı gerekçe (`reasonOf`: `neden_dogrudan_olmuyor`, `neden`); talimatta doğru mantık açıkça |
| İlk adımı doğru yapıp 9'a gelen çocuğa "8'e geri dön" | Haiku | `hatayi_bul` adım düzeyinde ilerleme buluyor (`stepTrail`): "doğru yolda, sıradaki adım: 5 gider" |
| Hiç dokunmamış çocuğa "bu hamlede şaşmış olabilir" | araç | Kayıtta yalnızca başlangıç değeri varsa "henüz bir şey yapmamış" |
| "Beşliği aşağı indir", "çek" gibi yanlış yön tarifleri | Haiku | Talimatta yön tarifi yasak, yalnızca "gelir / gider" |
| "23, 13 olur" doğru ara değer → yanlış alarm | Haiku | Araç sonucunda adım sonrası değerler (`adim_sonrasi_degerler`) |
| `low` uzun ve dağınık, çocuğun adını geri söyledi, toplamı kendisi hesaplayıp denetime takıldı | Haiku | effort `medium` |

Son deneme (Haiku, `medium`): yedek metne düşen cevap yok; gerekçe doğru, çocuğun ilerlemesini
biliyor, kişisel bilgiyi geri söylemiyor, konu dışını derse bağlıyor. Kalanlar (Faz 3 test setinde
ölçülecek): bazen cevabı hemen söylüyor ve gösteriyor (soru sorarak yönlendirmek yerine), bazen 4
cümleye çıkıyor, "beşlik boncuğu gider yapmak" gibi kırık Türkçe. Opus 5.5 aynı senaryolarda daha
kısa ve daha Sokratik.

Deneme betiğinin sınırı: bağlam sohbet boyunca sabit (gerçek uygulamada çocuk boncuk oynattıkça
değişecek).
- Testler: `npm run test:sobi` (40 test, sahte model ve sahte Firestore; para harcamıyor).
- Gerçek modelle deneme: `node scripts/sobi-dene.js` (ücretli; Firestore'a dokunmuyor). Anahtar `functions/.env.local` dosyasından okunur (Git'e girmez); PowerShell `npm.ps1`'i engellediği için `npm run` yerine `node`.
- Henüz yok: ebeveyn onayı denetimi ve gizlilik metni (Faz 5), "bildir" düğmesinin sunucu ucu
  (Faz 4 ile), İngilizce/Endonezce talimat metni.

#### Kurulum (kullanıcı yapar, bir kez)

1. Claude Console'da (platform.claude.com) bir API anahtarı oluştur. Harcama sınırı koymak iyi olur.
2. Bilgisayarda denemek için (deploy gerekmez):
   `cd functions; $env:ANTHROPIC_API_KEY = "sk-ant-..."; npm run sobi:dene`
3. Sunucuya gizli değişken olarak kaydet (proje klasöründe):
   `firebase functions:secrets:set ANTHROPIC_API_KEY` → anahtarı yapıştır.
4. Ancak bundan sonra `functions/index.js`'e şu satır eklenir ve push edilir (deploy tetiklenir):
   `exports.sobiChat = require('./sobi/callable')({ functions, admin, db, isAskQuestionRestricted, effectivePlan });`
   Sıra önemli: anahtar Secret Manager'da yokken bu satırla yapılan deploy başarısız olur, push
   bütün fonksiyonları deploy ettiği için ilgisiz değişiklikler de takılır.
5. Firestore konsolunda `sessions` koleksiyon grubu için `expiresAt` alanına TTL ilkesi aç
   (sohbetler 30 gün sonra kendiliğinden silinsin).

### Faz 4 durumu (28.10.2026, ilk sürüm; derlendi, telefonda denenmedi)

Giriş: yanlış cevap panelinin içinde, adımın ipucu metninin (`requestText`) altında ve "Tamam"ın
üstünde **"Anlamadım, Sobi'ye sor"** (kullanıcı kararı 28.10.2026: yukarıdan ipucu paneli inmesin,
her şey yanlış cevap panelinde; bilgi düğmesi gizli). Yalnızca abaküslü soru adımında
(`abacusClickable`, `answerNumber` var, seçenek yok) ve oturum açıkken. Sohbet başlayınca ekranda
öğretmene sor düğmesinin altında yeşil **"Sobi"** düğmesi kalıyor.

| Dosya | İş |
|---|---|
| `ServerSobi.kt` | `sobiChat` çağrısı, cevap/ekran işi ayrıştırma, günlük hak ve kısıt hataları |
| `SobiContext.kt` | Gönderilen bağlam (`practice: false`) ve adım başına durum (`SobiStepTracker`: başlangıç değeri, abaküs kaydı, deneme sayısı, oturum, mesajlar, son gösterim) |
| `SobiChatSheet.kt` + `sheet_sobi_chat.xml` | Alt panel: mesaj balonları, öneri balonları, yazma kutusu, "Abaküste izle"; "Bir daha göster" sunucuya gitmeden son gösterimi oynatıyor. Başlıkta "Sobi bir yapay zekâ yardımcısı; bazen yanılabilir." |
| `SobiAbacusPlayer.kt` | Gösterim: abaküsü işlemin başına getirir, `StepExplainer` hamlelerini `animateGuideBead` ile (çocuğun dokunuşuyla aynı yoldan) oynatır, sonra yine başa döndürür |
| `TutorialFragment.kt` | Yalnızca bağlantı noktaları: adım gösterilince durum sıfırlama ve başlangıç değerini yakalama, abaküs değer kaydı, "Kontrol Et" sayacı, yanlış cevap panelindeki ipucu ve düğme, "Sobi" düğmesi, kapanışta temizlik. Ders sonu zincirine dokunulmadı. |
| `strings_sobi.xml` | Bütün metinler |

Sunucu: öğreticide `alistirma_ver` aracı modele verilmiyor (`practice` bayrağı); öğretici yalnızca
adımın kendi cevabını denetlediği için Sobi'nin hazırladığı başka bir işlem çocuğu karıştırırdı.

Henüz yok: sesli soru ve sesli cevap, Sobi mesajlarında "bildir" düğmesi (Play'in üretken yapay
zekâ kuralı için yayından önce şart), ders alıştırması ve ödevde Sobi, analitik olayları.

### Anlatım adımlarında Sobi (28.10.2026, sunucu hazır, uygulama henüz göndermiyor)

Kullanıcı kararı: Sobi'ye öğreticinin her adımında soru sorulabilsin, yalnızca abaküslü soru
adımında değil. Bağlama üç alan eklendi (`chat.sanitizeContext`): `lessonTitle`, `recentSteps`
(son 6 adımın metni) ve `taughtRules` (`add_five`, `sub_ten`… çocuğun o ana kadar öğrendiği
kurallar). Talimat metninde "Anlatım adımlarında" bölümü: açıklamayı dersin kendi metnine dayandır,
öğretilmemiş kuralı anlatma ("ileride öğreneceğiz"), metinde gerekçe yoksa uydurma, gösterim yok.

Deneme (`node scripts/sobi-dene.js --senaryo kavram`, Haiku 5.5, 3 senaryo, gerçek öğretici metinleri):
- Doğru: "büyük kardeş ne demek", "1 ekleyecektik neden 10 geliyor" (dersin mantığıyla), "kardeş ne
  demek"; öğrenilmemiş kural ("8'e 6", 5'lik derste) ve çarpma "ileride" diye geri çevrildi.
- Düzeltilenler: her mesajı aynı soruyla ("birler sütununda kaç boncuk boşta?") bitiriyordu →
  talimattaki örnek soru kaldırıldı, "aynı soruyu tekrarlama"; kendi önerdiği alıştırmayı
  hesaplamadan söyleyip denetime takıldı → önerilen işlem de araçla hesaplanmalı; "neden büyük
  basamaktan başlıyoruz"a gerekçe uydurdu → "gerekçe yoksa uydurma"; anlatım adımında yedek metin
  olmayan "Bana göster"i öneriyordu → ayrı yedek metin; çocuğun adını geri söyledi → "benim adım /
  ismim …" kalıbındaki ad maskeleniyor.
- Soru adımı senaryoları yeniden çalıştırıldı, bozulma yok.

Uygulama (28.10.2026): öğreticinin sağ üstünde, öğretmene sor düğmesinin altında hep duran kafalı
"Sobi'ye sor" düğmesi (view_sobi_chip.xml); sohbet öğretici boyunca sürüyor (SobiStepTracker.startStep
adım durumunu sıfırlıyor, sohbeti değil); her mesajla ders adı, son 6 adımın metni ve öğretilen
kurallar (SobiCurriculum, öğretici numarasından) gidiyor. Anlatım adımında abaküs değeri gönderilmiyor
(dersin animasyonları sırasında denetleyicinin değeri geride kalabiliyor) ve sunucu gösterim aracını
vermiyor. Sohbet sınırı 10 → 20 mesaj. Anlatım adımında ilk balonlar: Bunu anlamadım, Bir örnek ver,
Neden böyle?

### Faz 3 durumu (29.10.2026): değerlendirme seti ve ilk ölçüm

`tools/sobi-eval/` (tasarım: `TASARIM.md`, sonuç: `SONUC.md`). Girdiler öğreticilerin gerçek
metinlerinden (`extract-steps.js`: TutorialFragment.kt + strings_tutorial.xml, uygulamanın gönderdiği
bağlamla); çocuk mesajları sentetik. 460 örnek, 32 öğretici, 10 kategori (anlamadım, neden, yanlış
anlama, örnek, öğretilmemiş kural, cevabı isteme, konu dışı, kişisel bilgi, yazım hatalı dil, çok
turlu sohbet). Sobi gerçek sunucu koduyla (`chat.runTurn`) çağrılıyor; puanlama programlı denetimler
+ hakem modeli (Sonnet 5.5, 8 ölçüt, `hakem_tr.md`).

İlk ölçüm, kredi için setin yarısında (230 örnek, Haiku 5.5 / medium; ölçülen maliyet 2,12 $, pilot
ve rubrik denemesiyle toplam 2,60 $):

- Başarı %33 ± 6 (hakemin zayıf iki ölçütü dışarıda %43). Anlatım adımı %46, soru adımı %18.
  Güvenlik %99, öğretilmemiş kural anlatmama %96, soruya cevap %88.
- En sık 5 hata ve önerilen düzeltme (hiçbiri uygulanmadı):
  1. Soru adımında cevabı/hamleyi hemen söylüyor (%49). `hatayi_bul`'un `siradaki_adim` alanı
     cevabı sızdırıyor: araç ilk turda ipucu dönsün, hamleyi ikinci yardım isteğinde versin;
     bağlamda "Sobi'nin bu adımdaki kaçıncı mesajı".
  2. Abaküsün o anki durumunu ve adımın işini yanlış okuyor (~%19): sunucu ilk turda `hatayi_bul`'u
     kendisi çalıştırsın (model %44 çağırıyor); bağlama "bu adımın işi" satırı.
  3. Uzun cevap (%23 > 3 cümle): guard kırpsın (yedeğe düşürmeden).
  4. Kardeş / büyük kardeş karışıklığı ve derste olmayan gerekçe: araçta kimin kardeşi olduğu açık
     alan; talimatta "çıkarmada kardeş de".
  5. Çarpma derslerinde guard cevapların %38'ini (t18 ve t23'te %71) geri çeviriyor (çarpım
     sonuçları kaynaksız) ve yedek metin toplama kuralı anlatıyor: bağlama basamak çarpımları,
     çarpmada yedek metin = adımın metni.
- Hakem `low` effort boncuk aritmetiğinde güvenilmez (matematik başarısızlıklarının ~yarısı hakem
  hatası); sonraki turda matematik programlı denetlenmeli.
- Ücretsiz maliyet kazancı: sistem bloğuna `cache_control` (her sohbet ortak öneki yeniden yazıyor;
  Sobi maliyetinin ~yarısı).

İkinci tur (29.10.2026, kullanıcı etiketleriyle; ayrıntı `tools/sobi-eval/SONUC.md`): ölçüt
kullanıcıya göre düzeltildi (uzunluk tek başına sorun değil; cevabı isteyene söylenir, takılana önce
ipucu; anlatımda "bunu" = şu anki adım ve son konu). Düzeltmeler `functions/sobi/` ve
`TutorialFragment.kt`'de **yalnızca yerelde** uygulandı (bütün önceki adımlar bağlamda, adımın işi,
abaküs durumu, çarpımlar, kardeş alanı, ipucu, önbellek noktası, çarpmada yedek metin). Aynı 135
örnekte: soru adımı %27 → %49, anlatım adımı %65 → %46, genel %51 → %47; Sobi örnek başına ~%35
ucuzladı. Anlatımdaki gerileme talimattan (uzayan cevaplar, öğretilmemiş kuralı çözmesi, soru adımı
kurallarının sızması); v2 talimatı önerildi. **v1 bu haliyle deploy edilmemeli.**

v2 talimatı (yerelde uygulandı; kısa cevap, öğretilmemişte çözmeme, anlatımda abaküs yaptırmama)
yalnız anlatım adımlarında ölçüldü: %46 → %50 (gürültü içinde), baseline %65. Kalan farkın bir
kısmı hakem hatası (dersin kendi sözlerini cezalandırıyor), bir kısmı gerçek (cevaptan sonra dersin
başka sorusuna kayma, anlatımda "gösterebilirim"). Karar için insan etiketli yan yana karşılaştırma
önerildi. Toplam harcanan 5,87 $.

Kullanıcının kör yan yana etiketi (31.10.2026, 20 anlatım örneği): **v2 12, baseline 5, aynı 3**;
hakem ayrıştığı 12 örneğin yalnızca 3'ünde kullanıcıyla aynı yöndeydi. Anlatım "gerilemesi" hakem
hatasıydı; sürüm kararları bundan sonra kör etiketle verilir.

Hazır açıklamalar ve beğeni (31.10.2026, kullanıcı onayıyla; henüz deploy edilmedi): t3'ün 12
parçası onaylandı (`functions/sobi/aciklamalar_tr.json`, `explanations.js`). Anlatım adımında onaylı
açıklama bağlama giriyor; "Bunu anlamadım" / "Neden böyle?" balonlarına model çağrılmadan dönüyor
(günlük haktan düşmüyor). Uygulama bağlama öğretici numarası ve adım sırasını ekledi. Sobi'nin her
cevabının altında 👍 / 👎 (`sobiFeedback`, `feedback.js`); haftalık döngü:
`tools/sobi-eval/HAFTALIK_DONGU.md`. Sunucu testleri 52/52; APK derlendi ve telefona kuruldu.

Sonraki adım önerisi (31.10.2026): **hazır açıklamalar** (`tools/sobi-eval/HAZIR_ACIKLAMALAR_PLANI.md`).
Anlatım adımlarında Haiku'nun serbest açıklaması yerine dersin 224 konu parçası için bir kez
yazdırılıp (Opus, ~$4) kullanıcının onayladığı "basit / neden" metinleri, örnekleri kural motoru
seçer; öneri balonlarına model çağırmadan onaylı metin döner. Önce kör yan yana etiketleme
(`tools/sobi-eval/etiket/YAN_YANA.md`) ve 2 öğreticilik pilot (~$0,25).

### Ders ekranında hata teşhisi (31.10.2026, A aşaması; derlendi, telefonda denenmedi)

Kullanıcı kararı: ders (`AbacusFragment`) ve abaküssüz ders (`BlindingLessonFragment`) için yanlış cevapta
çocuğun nerede yanıldığı anlatılsın; sonra "Sobi ile adım adım çöz" ekranı (B) ve o ekranda Sobi (C).
A yapay zekâsız, kural motoruyla:
- `abacus/MistakeAnalyzer.kt`: soru `ilk ± ikinci`, abaküs sıfırdan. Doğru yol = ilk sayıyı yazmak +
  ikinciyi basamak basamak işlemek (`StepExplainer`). Çocuğun değer kaydında hamle sonuçları sırayla
  aranır; bulunamayan ilk hamle hatanın yeri. Türler: unutulan adım (çok adımlı hamlede "sonuç − o
  adımın katkısı" kayıtta: 248+223=461 → "10 gelir" yapılmamış), yanlış sütun değeri, ilk sayıyı
  yanlış yazmak (kayıtta `cevap ∓ ikinci`), yoksa yalnızca yer + doğru adımlar. Adım adım gidilmediyse
  (ilk sayı kayıtta yok ve yanlış yazılmış ilk sayı da yok) SUSAR; çarpma/bölme ve yalnızca sayı yazma
  sorularında da. 8 birim testi.
- `AbacusFragment`: denetleyicinin `onValueChanged`'iyle kayıt (sıfırlama sırasındaki ara değerler
  alınmıyor), her yeni soruda sıfırlanıyor; yanlış cevap panelinde `incorrectDiagnosisText`.
- Abaküssüz derslerde kayıt yok (çocuk boncuk oynatmıyor); orada hata B'deki yeniden çözme ekranında
  bulunacak.

### Faz 8 ayrıntısı: öğretmene sor, ama videosuz

Sobi'nin tur sınırı dolduğunda ya da çocuk isterse "Öğretmenine soralım mı?":

- Video yerine **bağlam paketi** gider: adım, beklenen ve verilen cevap, abaküs değer kaydı ve
  Sobi sohbetinin özeti. İsteyen yine görüntü/video ekleyebilir.
- Öğretmen sohbetinde bu paket, boncuk hareketlerini tekrar oynatan küçük bir abaküs kartı
  olarak görünür.
- Sunucu: `askTeacherQuestion` şu an `storagePath` + `mediaUrl` istiyor; `mediaType: 'context'` için
  bu şart kaldırılır, paket boyutu sınırlanır. Kredi mantığı aynı kalır.
- Kurs sınıfında öğretmene haftalık özet de buradan çıkar: "Bu hafta 10'luk çıkarmada 3 kez
  Sobi'ye sordu."

## Deploy notu

`functions/` değişen bir commit `claude/**` dalına push edilince **bütün fonksiyonlar** deploy
oluyor; `firestore.rules` değişirse kurallar da. Faz 2, 5 ve 8 sunucu değişikliği içerir;
push'tan önce sorulmalı. API anahtarı secret olarak elle tanımlanır (kullanıcı yapar).

## Kullanıcıya açık sorular

1. Sohbet ücretsiz kullanıcıya açık olsun mu, sınırı ne olsun? (Öneri: günde birkaç sohbet
   ücretsiz, Pro'da yüksek sınır.)
2. Sobi cevabı sesli okusun mu? (Öneri: evet, varsayılan açık; okuma bilmeyen küçükler için önemli.)
3. Sohbetler ebeveyne görünsün mü? (Öneri: evet, ebeveyn panelinde; güveni artırır.)
4. Hangi model sağlayıcısı? (Öneri: Faz 3'teki test setinde iki modeli karşılaştırıp karar vermek.)

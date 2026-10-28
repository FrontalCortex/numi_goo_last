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
- Test seti depoda (`functions/sobi/test_set.json`), Node betiğiyle çalışır
  (`npm run test:sobi`, mevcut `test:*` betikleri gibi).

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
| 2 | `sobiChat` fonksiyonu: talimat metni, araçlar, güvenlik katmanları, sınır | **kod bitti (28.10.2026)**, anahtar bekliyor | Aşağıda "Faz 2 durumu" |
| 3 | Test seti ve talimat metninin olgunlaştırılması | 2–3 gün | Model seçimi burada |
| 4 | `SobiChatPanel`: sohbet, öneri balonları, sesli soru, abaküste gösterim | 3–4 gün | `TutorialFragment`'a yalnızca bağlantı noktası |
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

Kod `functions/sobi/` altında; **henüz `index.js`'e bağlı değil, deploy edilmiyor.**

| Dosya | İş |
|---|---|
| `prompt_tr.md` | Talimat metni: Sobi'nin karakteri, dersin dili, "hesap yapma, araç kullan", üslup, sınırlar, bağlam biçimi |
| `tools.js` | Beş araç (`hamleleri_hesapla`, `hatayi_bul`, `kurali_acikla`, `abakuste_goster`, `alistirma_ver`), hepsi `strict`; anlatım dersteki sözlerle ("1 boncuk gelir, 5 gider, 10 gelir"); kuralların onaylı açıklamaları |
| `guard.js` | Girişte e-posta/telefon/bağlantı maskeleme, 300 karakter sınırı; çıkışta "her sayı bir kaynakta geçmeli" denetimi, 500 karakter sınırı; denetime takılırsa kural motorundan yedek metin |
| `chat.js` | Bir sohbet turu: elle yazılmış araç döngüsü (en çok 4 tur), ret ve yarıda kesilen cevap geçmişe eklenmiyor, geçmiş yalnızca eklenerek büyüyor (düşünme blokları değişmeden geri gidiyor), öneri balonları |
| `callable.js` | `sobiChat` callable fabrikası: oturum, günlük hak (Free 20 / Lite 50 / Pro 100 mesaj), sohbet başına 10 mesaj, `sobiChats/{uid}/sessions/{id}` kaydı (30 gün), API hatasında yedek metin ve hak düşmüyor |

- Model `claude-opus-5-5`, effort `low`, istem önbelleği açık, ret olursa sunucu tarafı yedek model
  (`fallbacks: "default"`). Model bir sabit (`chat.DEFAULT_MODEL`); maliyet kararı Faz 3'te test
  setiyle (ör. `claude-haiku-5-5` ile karşılaştırma: `npm run sobi:dene -- --model claude-haiku-5-5`).
- Testler: `npm run test:sobi` (31 test, sahte model ve sahte Firestore; para harcamıyor).
- Gerçek modelle deneme: `npm run sobi:dene` (ücretli; Firestore'a dokunmuyor).
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

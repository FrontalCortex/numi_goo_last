# Sobi haftalık iyileştirme döngüsü (31.10.2026)

Amaç: çocukların beğen / beğenme oylarından Sobi'yi her hafta biraz iyileştirmek; ama hiçbir değişiklik
kendiliğinden yayına çıkmaz. Kör karşılaştırmada hakem (yapay zekâ) kullanıcıyla 12 örneğin yalnızca
3'ünde aynı yöndeydi; bu yüzden son karar her zaman kullanıcının.

## Veri

- Uygulama: Sobi'nin sunucudan gelen her cevabının altında 👍 / 👎 (`SobiChatSheet`); aynı düğmeye
  yeniden dokunmak oyu geri alır.
- Oy verilince yanında "Bize daha fazla bilgi ver" çıkar: kısa yorum kutusu (en çok 300 karakter,
  kişisel bilgi maskelenir). 🚩 "Bu cevabı bildir": uygunsuz / zararlı cevap için; oydan bağımsız,
  sunucu günlüğüne hemen `sobiReport` uyarısı yazılır ve `sobiChats/{uid}/sobiReports/` altına
  `status: 'yeni'` kaydı düşer (90 gün); haftalık raporun en başında listelenir.
- Sunucu: `sobiFeedback` callable (`functions/sobi/feedback.js`) → `sobiChats/{uid}/sobiFeedback/`:
  oy, cevap, çocuğun önceki mesajı (maskelenmiş), kaynak (`hazir` = onaylı açıklama, modelsiz /
  `model` = Haiku), açıklama anahtarı, öğretici ve adım, talimat sürümü. 30 gün sonra TTL ile silinir;
  hesap silinince hemen gider.
- Not: çocukların beğenisi doğruluğu ölçmez (cevabı direkt veren Sobi'yi beğenebilirler). Oylar
  "nereye bakalım" sinyalidir; karar test seti ve kör karşılaştırmayla verilir. Daha sağlam ölçüt,
  sohbetten sonra çocuğun adımı geçip geçmediği (plan belgesi, "Ölçüm").

## Kararlar (31.10.2026)

- Hazır açıklamalar şimdilik yalnızca t3'te; diğer öğreticilere Haiku cevapları hiç sevilmezse eklenir.
- Beğenilen bir cevabın modelsiz gösterilmesi için eşik: **aynı yer + aynı soru, 50 farklı kullanıcı,
  %80 beğeni.** Haiku her seferinde farklı metin yazdığı için oylar tek bir metinde toplanmaz; önce
  iyi beğenilen bir cevap sabit "aday metin" yapılır, sonraki çocuklara bu metin gösterilip oylanır,
  eşik bu metnin kendi oylarıyla aşılır (öneri, henüz yapılmadı).
- "Aynı soru" kesin olarak yalnızca öneri balonlarında bilinir ("Bunu anlamadım", "Neden böyle?",
  "Bir örnek ver"); yazılan mesajlar ancak niyetine göre gruplanabilir. "Aynı yer" anlatım adımında
  kesin (öğretici + adım + adım metni); soru adımında her çocuğun abaküsü farklı olduğu için hazır
  cevap kullanılmaz.

## Her hafta

1. **Rapor** (ücretsiz): `node tools/sobi-eval/haftalik/haftalik.js --gun 7` →
   `tools/sobi-eval/haftalik/rapor-<tarih>.md` (Git'e girmez; çocuk mesajları içerir).
   Firestore okuma kimliği gerekir (`GOOGLE_APPLICATION_CREDENTIALS` ya da
   `gcloud auth application-default login`).
   Raporda: (1) yeniden yazılacak hazır açıklamalar (≥3 oy, ≥%30 beğenmeme), (2) hazır cevaba aday
   model cevapları (aynı adım + aynı soru, ≥5 oy, ≥%80 beğeni), (3) beğenilmeyen model cevapları,
   (4) öğretici başına beğeni.
2. **Analiz**: raporu Claude Code oturumuna ver ("haftalık Sobi raporunu incele"). Claude:
   - (1) için açıklamayı yeniden yazar (`aciklamalar/aciklamalar_tr.json`, `onay: false`),
   - (2) için o cevabı hazır açıklama taslağı yapar,
   - (3)'teki ortak hataya göre talimat (`functions/sobi/prompt_tr.md`) değişikliği önerir.
3. **Deneme**: değişiklik test setinde hakemsiz çalıştırılır (`run-eval.mjs --hakemsiz`, ~$0,1) ve
   eski/yeni cevaplar kör yan yana dosyasına konur (`yan-yana.mjs`). Kullanıcı seçer.
4. **Onay ve yayın**: kullanıcı onaylarsa `aciklamalar/yayinla.mjs` + commit + push (yalnızca değişen
   fonksiyonlar deploy olur). Onay yoksa hiçbir şey değişmez.

İstenirse 1. adım haftalık zamanlanmış bir göreve bağlanabilir (rapor hazır olunca bildirim).

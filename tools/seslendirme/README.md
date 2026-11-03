# Öğretici seslendirmesi (ElevenLabs)

Türkçe öğretici sesi `app/src/main/res/raw/<anahtar>.mp3`'te. İngilizce ve Endonezce sesler bu
klasördeki betiklerle üretilip Firebase Storage'a `tutorial_audio/<dil>_<anahtar>.mp3` olarak yüklenir.
Üretilen mp3'ler (`cikti/`, `ornekler/`) depoya girmez.

Anahtar: `ELEVENLABS_API_KEY` ortam değişkeni ya da `functions/.env.local`'deki
`ELEVENLABS_API_KEY=...` satırı (worktree'de ana checkout'unki de okunur). Betikler anahtarı yazdırmaz.

## Betikler (depo kökünden)

| Komut | Ne yapar |
|---|---|
| `node tools/seslendirme/uret.mjs --dil en --kuru` | Üretilecek cümleleri ve okunacak metni listeler; kredi harcamaz |
| `node tools/seslendirme/uret.mjs --dil en --ses <voice_id>` | Toplam karakter/kredi; 50'den fazla cümlede `--onay` ister |
| `... --onay` | Hepsini üretir → `cikti/<dil>/<anahtar>.mp3`; var olanı atlar (kaldığı yerden devam) |
| `... --ornek 5` / `--sadece a,b` | İlk 5 / yalnızca verilen anahtarlar |
| `... --yeniden tutorial3_2` | Beğenilmeyen cümleyi silip yeniden üretir |
| `node tools/seslendirme/yaziya.mjs --dil en` | Sesleri yazıya çevirip metinle karşılaştırır (Speech to Text izni gerekir) |
| `node tools/seslendirme/esle.mjs --dil en` | Metni başka anahtardan gelen kayıtları (öğreticide `R.raw.Y` ≠ `R.string.X`) kopyalar; yüklemeden önce şart |
| `node tools/seslendirme/yukle.mjs --dil en --kuru` | Storage'a ne yükleneceğini listeler (yalnızca okur) |
| `node tools/seslendirme/yukle.mjs --dil en` | Yükler: `audio/mpeg`, `Cache-Control: public, max-age=2592000`; aynı boyutta olanı atlar |
| `node tools/seslendirme/sesler.mjs --dil id [--ara guru]` | Hesaptaki ve kütüphanedeki sesleri listeler |
| `node tools/seslendirme/sesler.mjs --dil id --ornek <id>,<id>` | Her ses için aynı 3 cümle → `ornekler/<dil>/<voice_id>/` |

`uret.mjs` diğer seçenekleri: `--model` (varsayılan `eleven_multilingual_v2`), `--bicim`
(varsayılan `mp3_44100_64`, mono 64 kbps; Türkçe raw dosyaları 128 kbps mono), `--hiz` (0.7–1.2,
varsayılan 0.9), `--kararlilik` (0.5), `--es` (aynı anda istek, 3), `--cikti`. 429/409/5xx'te bekleyip
tekrar dener. Endonezcede `language_code: id`, İngilizcede `en` gönderilir.

Metin temizliği: XML ve Android kaçışları çözülür, dıştaki `"` atılır, `[köşeli parantez]` parantezsiz
okunur, `a -> b` → `a, b`. Türkçe sesi olup çevirisi olmayan 16 anahtar yalnızca sayıdır (`100`, `-12`):
sayının kendisi okutulur (`-12` → `minus 12`).

Yükleme Admin SDK uygulama varsayılan kimliğiyle (`gcloud auth application-default login`) ya da
`GOOGLE_APPLICATION_CREDENTIALS` ile çalışır; `firebase-admin` `functions/node_modules`'tan alınır.

## Kredi

Yanıt başlığındaki `character-cost`'a göre Multilingual v2 bu hesapta karakter başına ~0,44 kredi
harcıyor (beklenen 1'in altında; betik her çalıştırmanın sonunda gerçek harcamayı yazar).
Tam üretim: İngilizce 752 cümle / 33.566 karakter, Endonezce 752 cümle / 37.067 karakter →
~31.000 kredi (karakter başına 1 olsaydı ~70.600; ikisi de aylık 121.000'in içinde).

## Ses adayları (2026-11-03, örnekler `ornekler/<dil>/<voice_id>/`)

İngilizce:
- **Alice – Clear, Engaging Educator** `Xb7hH8MSUJpSbSDYk0k2` (hazır ses, İngiliz kadın, eğitici etiketli)
- **Nichalia Schwartz – Bright and Friendly** `XfNU2rGpBa01ckF309OY` (kütüphane, Amerikalı genç kadın, e-öğrenme)
- **Cody – Energetic and Upbeat Educator** `9XfYMbJVZqPHaQtYnTAO` (kütüphane, genç erkek, açıklamasında çocuk eğitim içeriği)

Endonezce (standart aksan, Cava aksanlılar elendi):
- **Aita – Neutral, Clear and Natural** `k5eTzx1VYYlp6BE39Qrj` (orta yaş kadın, sakin, eğitici etiketli)
- **Honeypie – Warm, Youthful and Friendly** `3AwU3nHsI4YWeBJbz6yn` (genç kadın, sıcak, eğitim/animasyon)
- **Bian – Neutral, Calm and Clear** `1k39YpzqXZn52BgyLyGO` (orta yaş erkek, sakin, eğitici etiketli)

Kütüphane sesleri ilk kullanımda hesaba kendiliğinden ekleniyor (ses yuvası harcar; Creator'da 30).

## Telaffuz riskleri

- **Sorobit**: her dilde yalnızca `tutorial1_1`'de geçiyor. İngilizcede "Sorrow-bit" gibi okunabilir.
  Tek cümle olduğu için sözlük yerine dinleyip gerekirse `--yeniden tutorial1_1` (gerekirse metni
  yalnızca seslendirme için "Soh-roh-bit" gibi yazarak) yeterli.
- **sempoa** (Endonezce, 91 cümle) ve **teman kecil/besar**: Endonezce sesler için sıradan sözcükler;
  örneklerde dinlenip doğrulanmalı. Sorun çıkarsa ElevenLabs telaffuz sözlüğü (Pronunciation
  Dictionary, `.pls` dosyası, *alias* kuralı: `sempoa` → `sem-po-a`) Multilingual v2'de çalışır;
  *phoneme* (IPA) kuralları bu modelde desteklenmez.
- **Sobi**: öğretici seslerinde geçmiyor; yalnızca uygulama içi metinlerde var.
- **small friends / big friends** (İngilizce kural adı): sıradan sözcükler, risk düşük.

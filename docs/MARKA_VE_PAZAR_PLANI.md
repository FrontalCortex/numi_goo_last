# Marka ve pazar planı (04.10.2026)

Hukuki ya da mali danışmanlık değil; genel değerlendirme. Karar anlarında marka vekili görüşü alınacak.

## İsimler

- **Sorobit** (uygulama adı) → marka tescili yapılacak.
- **Sobi** (tavşan maskot) → yalnızca karakter adı; tescil EDİLMEYECEK.
  - Türkiye'de 41. sınıfta benzerleri var (sobio, sobiad, sobil, sobitürk).
  - ABD'de Swedish Orphan Biovitrum'un aktif "SOBI" markaları var (5, 35, 42, 45. sınıflar; "HELLO SOBI" 41'de iptal).
  - App Store'da "Sobi: Sobriety Tracker" (maskotu da Sobi, seri/rozet/XP var; ABD'de tescili yok).
  - Kural: Sobi'yi mağaza başlığında, ikonda, reklamın merkezinde kullanma. İsim kodda tek yerde: `strings.xml` → `mascot_name`.
- Tavşan çizimi Freepik "Animal Collection"dan uyarlandı → logo/marka olarak kullanılamaz; özgün yeniden çizim (hak devri sözleşmesiyle) gerekli. Freepik lisans türü ve atıf yükümlülüğü netleştirilecek.

## Araştırma sonuçları (Sorobit)

| Yer | Sonuç |
|---|---|
| TÜRKPATENT (marka adı, 09/41/42) | Temiz |
| WIPO GBD (fuzzy) | Birebir yok. Yakınlar: **sorbit** (AB başvuru + Almanya tescil, 9/41/42 — AB'de en ciddi risk), SORABIT (ABD, 9 — şarj cihazı/pil, düşük risk), SOROBOT (FR, 41/42), seorobit (KR, 41) |
| USPTO | Sorobit temiz; SORABIT alakasız (şarj cihazı) |

## Takvim

| Ne zaman | Ne | Maliyet (2026) |
|---|---|---|
| Hemen | sorobit alan adı + sosyal medya hesapları | birkaç yüz TL |
| Yayından birkaç gün önce | TÜRKPATENT başvurusu, kelime markası, 9 + 41 (ops. 42) — EPATS | 5.640 TL (42 ile 8.790) |
| Yayın | Türkçe + İngilizce + Endonezce; test pazarları ABD, Hindistan, Endonezya (+ Malezya, Filipinler İngilizceyle) | küçük reklam testleri |
| Başvurudan sonra 6 ay | Ülke bazında indirme / elde tutma / ödeme; tutan ülke için Madrid (Türk başvuru tarihi korunur) | ülkeye göre |
| ~6–9 ay sonra | TÜRKPATENT tescil ücreti (bildirimden itibaren 2 ay içinde). Madrid'e girildiyse ZORUNLU (5 yıl bağımlılık) | 7.010 TL |
| Çin kararı verilirse | Madrid'e Çin + Çince isim için yerel vekil; çocuk eğitimi düzenlemeleri (2021 sonrası) incelenmeli | ayrıca |

Madrid ücretleri (2 sınıf, WIPO tablosu 23.08.2026): temel 653 CHF; Çin 330, ABD 920, AB 837, Hindistan 166 CHF; TÜRKPATENT işlem 3.850 TL. Sonradan eklenen ülke Türk tarihinden yararlanmaz. AB'yi seçmeden önce "sorbit" için vekil görüşü.

## Pazar araştırması (04.10.2026)

- Play Store: abaküs nişi küçük (en büyük 1M+: Simple Soroban, Know Abacus; Hirokuma 500K+). Çoğu basit alıştırma aracı, bazıları 3.2–3.8 puan. Seri/maskot/ders yolu olan Duolingo tarzı abaküs kursu öne çıkmıyor. Almanca, Endonezce (tek rakip 5K+) ve Türkçe ders veren güçlü abaküs uygulaması yok.
- Google Trends (son 12 ay, göreli): Hindistan "abacus" 81, Endonezya "sempoa" 41 (2026 yazı yükselişte), ABD "abacus" 28 (şirket aramalarıyla kirli), Almanya "abakus" 14, Türkiye "abaküs" 10.
- Dil sırası: İngilizce → Endonezce → (veriye göre) Hintçe / Almanca / Arapça.
- Yerelleştirme ön koşulu: ~2.100 Kotlin + ~940 düzen metni koda gömülü (strings.xml'de 114), ~920 ses dosyası (rehber anlatımı) — metinler strings.xml'e taşınacak, sesler yeniden seslendirilecek (TTS ya da sanatçı).

## Riskler / dikkat

- "Neden kimse yapmamış": niş küçük olabilir, çocuk uygulamasında para kazanmak (veli ödemesi) zor. İlk aylarda indirmeden çok ödeme ve elde tutmaya bak.
- Veliyi hedefle ("abaküs kursunun evdeki yardımcısı"); kurslarla iş birliği (öğretmen hesabı) ucuz dağıtım kanalı olabilir.
- Reklam bütçesi: önce 3–5 bin TL'lik testler; motoru satmadan önce test sonucunu gör.
- Çocuk verisi: Google Play Families politikası, KVKK (ve hedef ülkelerde COPPA, DPDP vb.).

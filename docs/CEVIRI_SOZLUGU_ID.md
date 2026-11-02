# Çeviri Sözlüğü (TR → ID, Endonezce)

Bütün `values-in/strings*.xml` çevirileri ve Endonezce sunucu metinleri bu tabloya uyar. Bir terim
değişirse önce burası güncellenir, sonra dosyalar. İngilizce karşılıklar `CEVIRI_SOZLUGU.md`'de.

Android'de Endonezce klasörü **`values-in`** (eski dil kodu; `values-id` çalışmaz). Sunucuda dil kodu
`id`.

Hedef: sade, sıcak Endonezce; çocuğa **"kamu"** ile hitap. Endonezya'daki sempoa kurslarının
terimleri kullanılıyor (çocuk ve veli onları tanıyor).

## Marka ve karakterler (çevrilmez)
| TR | ID | Not |
|---|---|---|
| Sorobit, Sobi, Pro, Lite | aynen | |
| Dino, Timo, Keço, Karti, Sino, Kapi | aynen | Rozet karakterleri |

## Abaküs ve matematik
| TR | EN | ID | Not |
|---|---|---|---|
| abaküs | abacus | sempoa | Endonezya'da yerleşik ad |
| boncuk | bead | manik | |
| beşlik boncuk / birlik boncuk | five-bead / one-bead | manik lima / manik satu | |
| üst / alt boncuk | upper / lower bead | manik atas / manik bawah | |
| basamak (sayının) | digit | digit | "2 basamaklı" → "2 digit" |
| birler, onlar, yüzler, binler, on binler | ones… | satuan, puluhan, ratusan, ribuan, puluh ribuan | "nilai tempat"; sütun: "tiang/kolom" → **kolom** |
| kuralsız | direct | langsung | "Penjumlahan Langsung" |
| 5'lik kural | small friends | **rumus teman kecil** | Sempoa kurslarının terimi |
| 10'luk kural | big friends | **rumus teman besar** | |
| boncuk kuralı | mixed friends | **rumus gabungan** | |
| kardeş | friend | teman (kecil) | "Teman kecil 4 adalah 1" |
| büyük kardeş | big friend | teman besar | "Teman besar 1 adalah 9" |
| "5 gelir, kardeş gider" | add 5, take away the friend | "tambah 5, kurangi temannya" | Ritmik kalmalı |
| "10 gelir, büyük kardeş gider" | add 10, take away the big friend | "tambah 10, kurangi teman besarnya" | |
| "5 gider, kardeş gelir" | take away 5, add the friend | "kurangi 5, tambah temannya" | |
| gelir / gider | add / take away | tambah / kurangi | Boncuk için: "masuk / keluar" |
| zihinden hesaplama | mental math | berhitung di luar kepala (anzan) | Kısa yerde "mental" |
| körleme | Blind | **Tanpa Sempoa** | Önerim; alternatif "Mode Bayangan" |
| abaküssüz | without abacus | tanpa sempoa | |
| 2'ye 1 çarpma | 2×1 multiplication | perkalian 2×1 | |
| ... saniye arayla | every ... seconds | setiap ... detik | |
| flaş | flash | flash | |

## Ders yapısı
| TR | ID | Not |
|---|---|---|
| ders | pelajaran | |
| ünite / ünite maratonu | unit / maraton unit | |
| bölüm / kısım | bagian | |
| konu | topik | |
| seviye | level | |
| ipucu | petunjuk | |
| test (Sobi testi) | kuis (kuis Sobi) | |
| ödev | PR | "pekerjaan rumah"; okul dili, kısa |
| günlük soru | soal harian | |
| Kupa Yolu / Ustalık Yolu / Kupa Modu | Jalur Piala / Jalur Penguasaan / Mode Piala | |
| liderlik tablosu | papan peringkat | |
| rekor | rekor | |

## Oyunlaştırma
| TR | ID | Not |
|---|---|---|
| seri | **streak** | Önerim: Endonezya'daki çocuk uygulamalarında yerleşik; alternatif "rentetan" |
| seri dondurma | pembeku streak | |
| günlük görev | misi harian | |
| sandık | peti | |
| kupa | piala | |
| rozet | lencana | |
| madalya (altın/gümüş/bronz) | medali (emas/perak/perunggu) | |
| altın (para) | koin | |
| anahtar | kunci | |
| can | nyawa | |
| yıldız | bintang | |
| sezon | musim | |
| mağaza | toko | |
| rozet ve ders ünvanları | anlamıyla çevrilir | ör. Kusursuz Odak → Fokus Sempurna |

## Hesap, ödeme, roller
| TR | ID | Not |
|---|---|---|
| öğrenci / öğretmen | murid / guru | |
| eğitmen | tutor | |
| veli | orang tua | |
| sınıf / Sınıfım | kelas / Kelasku | |
| kurs | kursus | |
| soru havuzu | kumpulan soal | |
| kredi | kredit | |
| abonelik | langganan | |
| deneme süresi | uji coba gratis | |
| Sobi hakkı | pesan Sobi | |

## Biçim kuralları
- `%1$s`, `%1$d` yer tutucuları korunur; cümleye göre yerleri değişebilir. `\'` kaçışı korunur.
- Ondalık ayırıcı **virgül** ("2,5 detik"), binlik ayırıcı nokta: Türkçeyle aynı.
- "sn / dk / sa" → "dtk / mnt / jam". Günler: Sen, Sel, Rab, Kam, Jum, Sab, Min.
- Çoğul: Endonezcede ad çoğul eki almaz ("3 soal"); `plurals`'ta `other` yeter (`one` da aynı metin).
- Başlıklar Title Case (Endonezya uygulamalarında yaygın: "Misi Harian").

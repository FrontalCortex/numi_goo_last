package com.example.app

/**
 * Öğrencinin "Kurs / Sınıfım" girişinin görünürlüğü (Ayarlar'daki kurs kartı).
 *
 * 02.11.2026'da kapatılmıştı (sınıfa katılmış öğrenci yoktu, odak Sobi). 19.11.2026'da yeniden açıldı:
 * kurs yerlerine satış (kullanıcı kararı). Öğrencinin Ödevlerim listesi eskiden Sohbet sekmesindeydi;
 * öğrencide o sekme Sobi olduğu için liste artık sınıf ekranında ([StudentClassFragment]).
 * Yalnızca görünürlük: kapatılırsa sınıf, davet ve ödev akışı, sunucu ve öğretmen ekranları çalışmaya devam eder.
 */
object StudentClassFeature {
    const val ENABLED = true
}

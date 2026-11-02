package com.example.app

/**
 * Öğrencinin "Kurs / Sınıfım" girişinin görünürlüğü (Ayarlar'daki kurs kartı).
 *
 * Şimdilik kapalı (kullanıcı kararı 02.11.2026): sınıfa katılmış öğrenci yok; ürünün odağı Sobi.
 * Yalnızca görünürlük: sınıf, davet ve ödev akışı, sunucu ve öğretmen ekranları duruyor ve çalışıyor;
 * ödev bildirimi yine ödev ekranını açar. Yeniden açmak için bu bayrağı true yapmak yeter.
 */
object StudentClassFeature {
    const val ENABLED = false
}

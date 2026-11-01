package com.example.app

/**
 * "Öğretmene sor" (öğretmen danışma) özelliğinin öğrenci tarafındaki görünürlüğü.
 *
 * Şimdilik kapalı (kullanıcı kararı 01.11.2026): öğrencinin takıldığı yerde yardımı Sobi
 * veriyor, danışma kredisi satılmıyor. Kapalıyken:
 * - ders, öğretici, abaküs, alıştırma ve haritadaki soru düğmeleri gizli
 *   ([AskQuestionButtonBinder.bind] tek yerden gizliyor),
 * - ders dönüşündeki "öğretmene sor" tanıtımı açılmıyor.
 *
 * Yalnızca görünürlük: soru akışı, sunucu (askTeacherQuestion) ve öğretmen hesabının gelen
 * soruları cevapladığı ekranlar duruyor ve çalışıyor; önceden gönderilmiş soruların cevapları
 * da bildirimlerde görünmeye devam ediyor. Yeniden açmak için bu bayrağı true yapmak yeter.
 */
object TeacherQuestionsFeature {
    const val ENABLED = false
}

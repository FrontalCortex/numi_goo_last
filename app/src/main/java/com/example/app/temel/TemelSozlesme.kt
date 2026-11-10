package com.example.app.temel

import android.content.Context
import androidx.fragment.app.Fragment
import java.io.Serializable

/*
 * "Temel yolu" (soroban öncesi hazırlık) parçalarının ORTAK SÖZLEŞMESİ.
 *
 * Neden ayrı bir dosya: iskelet (yol seçimi, harita, ilerleme), oyunlar ve ses üç ayrı oturumda,
 * üç ayrı dalda yazılıyor. Hepsi yalnızca bu dosyadaki türlere bağlanırsa dallar birleşince
 * birbirine oturur. Bu yüzden buradaki bir imzayı değiştirmek üç dalı birden etkiler:
 * değiştirmeden önce ana oturuma haber verilmeli, yeni alan eklenecekse varsayılan değerle eklenmeli.
 *
 * Kim neyi yazar:
 *  - `temel/oyun/` : oyun türleri + [TemelOyunFabrikasi] uygulaması
 *  - `temel/ses/`  : [TemelSesCalar] uygulaması
 *  - `temel/` kökü : yol seçimi, harita, ilerleme, MainActivity bağlantısı (iskelet)
 */

/**
 * Oyun türleri. Az sayıda tutuldu: okuma bilmeyen çocuk kuralı bir kez öğrenir,
 * aynı tür farklı becerilerde yeniden kullanılır.
 */
enum class TemelOyunTuru {
    /** Hedef (miktar, rakam ya da söylenen sayı) ile eşleşen seçeneğe dokun. */
    ESLESTIR,

    /** Söylenen sayı kadar nesneyi Sobi'ye sürükle (sayarak verme). */
    BESLE,

    /** Nesneler kısa süre görünüp kaybolur; kaç tane olduğunu seç (saymadan görme). */
    FLAS,

    /** Beşlik/onluk çerçeve: "6, 5 ve 1'dir". Sorobandaki 5'lik boncuğa hazırlık. */
    BESLIK_CERCEVE,
}

/**
 * Bir seviyenin oyun ayarı. Haritadaki her seviye bunlardan birini (ya da birkaçını sırayla) çalıştırır.
 * Yeni alan eklenirse VARSAYILAN DEĞERLE eklenmeli; yoksa öbür dalların çağrıları derlenmez.
 */
data class TemelOyunAyari(
    val tur: TemelOyunTuru,
    val enKucukSayi: Int = 1,
    val enBuyukSayi: Int = 5,
    /** Bir oturumdaki soru sayısı. Oturum 3–5 dakikayı geçmemeli. */
    val turSayisi: Int = 6,
    val secenekSayisi: Int = 3,
    /** Yalnızca [TemelOyunTuru.FLAS]: nesnelerin ekranda kalma süresi. */
    val gosterimMs: Long = 0L,
    /** Rakam (yazılı sayı) görünsün mü; false ise yalnızca miktar ve ses. */
    val rakamGoster: Boolean = false,
    /** Türe özgü alt biçim. Geçerli değerleri oyun oturumu `docs/TEMEL_OYUNLAR.md`de listeler. */
    val varyant: String = "",
) : Serializable

data class TemelOyunSonucu(
    val dogru: Int,
    val yanlis: Int,
    val sureMs: Long,
    /** Çocuk oyunu bitirmeden çıktı. */
    val yaridaBirakildi: Boolean = false,
) : Serializable

/**
 * Oyunu barındıran taraf (üst fragment ya da activity) bunu uygular; oyun fragmenti bitince
 * `parentFragment`, o değilse `activity` üzerinden çağırır.
 */
interface TemelOyunDinleyici {
    fun temelOyunBitti(sonuc: TemelOyunSonucu)
}

interface TemelOyunFabrikasi {
    /** Verilen ayarla çalışacak oyun fragmentini üretir. */
    fun olustur(ayar: TemelOyunAyari): Fragment
}

/**
 * Seslendirme. Küçük çocuk yazı okuyamadığı için yönerge ve sayı adları sesle verilir.
 * Ses dosyası yoksa ya da uygulamanın sesi kapalıysa çağrı sessizce geçmeli ve [cal] içindeki
 * `bitince` yine de çağrılmalı: oyun akışı sese takılıp kalmamalı.
 */
interface TemelSesCalar {
    fun hazirla(context: Context)

    /** Anahtarlar için bkz. [TemelSesAnahtari]. Dil, uygulamanın diline göre seçilir. */
    fun cal(anahtar: String, bitince: (() -> Unit)? = null)

    fun durdur()
}

/** Ses anahtarlarının tek kaynağı: oyunlar bu adlarla ister, ses oturumu bu adlarla üretir. */
object TemelSesAnahtari {
    /** Sayının adı: "bir", "iki" … (0–20). */
    fun sayi(n: Int) = "sayi_$n"

    /** Övgü cümleleri, 1..[OVGU_SAYISI]; rastgele seçilir ki hep aynı cümle duyulmasın. */
    fun ovgu(i: Int) = "ovgu_$i"
    const val OVGU_SAYISI = 5

    /** Yanlıştan sonra yumuşak "bir daha dene", 1..[TEKRAR_SAYISI]. Ceza tonu yok. */
    fun tekrar(i: Int) = "tekrar_$i"
    const val TEKRAR_SAYISI = 3

    /** Oyunun yönergesi: "yonerge_eslestir", "yonerge_besle_<varyant>" gibi. */
    fun yonerge(tur: TemelOyunTuru, varyant: String = "") =
        "yonerge_" + tur.name.lowercase() + if (varyant.isEmpty()) "" else "_$varyant"

    const val KAC_TANE = "kac_tane"
    const val SEVIYE_BITTI = "seviye_bitti"
}

/**
 * Parçaların buluştuğu yer. Dallar birleşince iskelet burada gerçek uygulamaları bağlar;
 * o zamana kadar oyunlar sessiz çalışır, harita oyun yokken yer tutucu gösterir.
 */
object TemelBagimliliklar {
    var oyunFabrikasi: TemelOyunFabrikasi? = null

    var ses: TemelSesCalar = object : TemelSesCalar {
        override fun hazirla(context: Context) = Unit
        override fun cal(anahtar: String, bitince: (() -> Unit)?) {
            bitince?.invoke()
        }
        override fun durdur() = Unit
    }
}

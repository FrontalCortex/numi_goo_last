// iOS kopyasına özgü test yardımcıları (Android'de karşılığı yok).
package com.sorobit.shared.dersadim

import com.sorobit.shared.matematik.RastgeleKaynagi
import kotlin.random.Random

object DersAdimTestYardimci {
    /** Altın veride kullanılan tohumlar. */
    val TOHUMLAR = listOf(1, 2, 3)

    /** [f]'yi verilen tohumla çalıştırır; iş bitince rastgele kaynağını varsayılana döndürür. */
    fun <T> tohumla(tohum: Int, f: () -> T): T {
        RastgeleKaynagi.rastgele = Random(tohum)
        try {
            return f()
        } finally {
            RastgeleKaynagi.varsayilanaDon()
        }
    }

    /** Platformdan bağımsız, kararlı 32 bit FNV-1a özeti (String.hashCode'a güvenmemek için). */
    fun fnv1a(metin: String): String {
        var h = 0x811C9DC5.toInt()
        for (c in metin) {
            h = h xor c.code
            h *= 0x01000193
        }
        return h.toUInt().toString(16)
    }

    /** Bir tablo sonucunun altın veri satırındaki karşılığı: `adet|özet`; istisna atarsa `EXC`. */
    fun ozet(tohum: Int, f: () -> List<Any?>): String =
        try {
            val liste = tohumla(tohum, f)
            "${liste.size}|${fnv1a(liste.toString())}"
        } catch (e: Throwable) {
            "EXC"
        }
}

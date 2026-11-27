package com.sorobit.shared.sobianaliz

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Android StepExplainerTest.ortak_tablo karşılığı: depo kökündeki shared/step_explainer_cases.json
// (Android'in ve sunucudaki JS kopyasının sınandığı tablo) ortak koddaki StepExplainer ile birebir
// aynı planı vermeli. Dosya okunduğu için yalnızca JVM'de.
class StepExplainerTabloTest {

    private fun dosya(yol: String): File? {
        // Gradle testi ios/shared içinde çalıştırır; depo kökü iki üst klasörde.
        var kok: File? = File("").absoluteFile
        repeat(5) {
            val f = File(kok, yol)
            if (f.isFile) return f
            kok = kok?.parentFile
        }
        return null
    }

    private val satir = Regex("""\{"op":"(add|sub)","base":(\d+),"number":(\d+),"plan":"([^"]*)"\}""")

    @Test
    fun ortak_tablo() {
        val f = dosya("shared/step_explainer_cases.json") ?: error("shared/step_explainer_cases.json bulunamadı")
        val satirlar = satir.findAll(f.readText()).toList()
        assertTrue(satirlar.size > 1000, "tabloda ${satirlar.size} satır")
        for (m in satirlar) {
            val op = HomeworkOp.fromKey(m.groupValues[1])!!
            val base = m.groupValues[2].toLong()
            val number = m.groupValues[3].toLong()
            assertEquals(m.groupValues[4], StepExplainer.encode(StepExplainer.explain(base, op, number)), "$base ${op.key} $number")
        }
    }
}

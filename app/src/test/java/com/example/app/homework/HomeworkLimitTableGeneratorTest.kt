package com.example.app.homework

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * [HomeworkLimitTable]'ı üretir. Normal test koşusunda ATLANIR (birkaç dakika sürüyor); yalnızca
 * GEN_HOMEWORK_TABLE=1 ile çalışır. Kullanım tablonun başlık yorumunda.
 *
 * Her giriş için önce [HomeworkEngine.probeCarryMax] (20 soru üst üste) en fazla adedi buluyor,
 * sonra o değerde 300 soru daha deneniyor; biri bile üretilemezse değer düşürülüyor. Öğrencinin
 * telefonunda soru üretimi başarısız olmamalı: tabloda güvenli tarafta kalmak bir iki sayı
 * kaybettiriyor, tersi ödevi açılmaz yapardı.
 */
class HomeworkLimitTableGeneratorTest {

    @Test
    fun generate() {
        assumeTrue(System.getenv("GEN_HOMEWORK_TABLE") == "1")
        val carryMasks = (1..15).filter { HomeworkEngine.usesCarry(AbacusRule.fromMask(it)) }
        data class Job(val op: HomeworkOp, val abacus: Boolean, val mask: Int)
        val jobs = listOf(HomeworkOp.ADD, HomeworkOp.SUB).flatMap { op ->
            listOf(true, false).flatMap { abacus -> carryMasks.map { Job(op, abacus, it) } }
        }
        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val started = System.currentTimeMillis()
        val results = jobs.map { job ->
            pool.submit(Callable {
                val rules = AbacusRule.fromMask(job.mask)
                val maxDigits = HomeworkEngine.maxDigits(job.abacus)
                val values = IntArray(maxDigits) { i ->
                    val digits = i + 1
                    val rng = Random(job.mask * 1000 + digits * 10 + if (job.abacus) 1 else 0)
                    var n = HomeworkEngine.probeCarryMax(job.op, rules, digits, job.abacus, rng, questions = 20)
                    while (n >= HomeworkEngine.minNumberCount(job.op) && !reliable(job.op, rules, digits, job.abacus, n, rng)) n--
                    if (n < HomeworkEngine.minNumberCount(job.op)) 0 else n
                }
                job to values
            })
        }.map { it.get() }
        pool.shutdown()

        val text = buildString {
            for ((job, values) in results) {
                append("        \"${job.op.key}:${if (job.abacus) "a" else "n"}:${job.mask}\" to intArrayOf(")
                append(values.joinToString(", "))
                append("), // ")
                append(AbacusRule.fromMask(job.mask).joinToString("+") { it.key })
                append('\n')
            }
        }
        val out = File("build/homework_limit_table.txt")
        out.parentFile.mkdirs()
        out.writeText(text)
        println(text)
        println("Süre: ${(System.currentTimeMillis() - started) / 1000} sn → ${out.absolutePath}")
    }

    private fun reliable(op: HomeworkOp, rules: Set<AbacusRule>, digits: Int, abacus: Boolean, n: Int, rng: Random): Boolean {
        val cfg = HomeworkConfig(op, rules, abacus, digits, numberCount = n, intervalMs = 1000L, questionCount = 1)
        return (1..300).all { HomeworkEngine.probeGenerate(cfg, rng) }
    }
}

package com.example.app

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Koddan arayüze giden metinler (`strings_code.xml`) için tek kapı.
 *
 * ## Neden `getString()` değil
 * Metinlerin çoğu Toast'ta, dialog'da ya da Firestore/ağ geri çağırımında kullanılıyor; bu
 * geri çağırımlar ekran kapandıktan SONRA da gelebiliyor. Fragment.getString() o anda
 * IllegalStateException fırlatır — eskiden düz metin olduğu için bu risk yoktu. Uygulama
 * context'i her zaman hazır; ayrıca context'i olmayan sınıflar (AuthManager, repository'ler)
 * da aynı yoldan okuyabiliyor.
 *
 * ## Değişkenli metinler
 * Kotlin şablonu (`"$gün gün kaldı"`) `"%1$s gün kaldı"` biçimine çevrildi ve argümanlar
 * sırayla verilir. Biçimde her değer `%s` ile basılır: sayılar şablondakiyle aynı görünür,
 * tür uyuşmazlığından çökme olmaz.
 *
 * ## Dil değişimi
 * Uygulama context'i uygulamanın dilini izliyor; dil seçimi (Aşama 3) uygulamayı yeniden
 * başlatmalı, yoksa sınıf açılışında okunmuş metinler (enum etiketleri, companion haritaları)
 * eski dilde kalır.
 */
object AppStrings {
    @Volatile private var appContext: Context? = null

    /** [NumiGooApplication.onCreate]'in ilk satırlarında çağrılır. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun context(): Context =
        checkNotNull(appContext) { "AppStrings.init çağrılmadan metin istendi" }
}

/** `strings_code.xml`'deki bir metin; argüman varsa `%1$s`, `%2$s`… yerine konur. */
fun appString(@StringRes id: Int, vararg args: Any?): String {
    val ctx = AppStrings.context()
    return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
}

/**
 * Sayıya göre değişen metin (İngilizce "1 day / 2 days"). Türkçede iki biçim aynı.
 * [count] çoğul seçimi için; metindeki değerler [args]'tan gelir (genelde sayının kendisi).
 */
fun appPlural(@PluralsRes id: Int, count: Number, vararg args: Any?): String =
    AppStrings.context().resources.getQuantityString(id, count.toInt(), *args)

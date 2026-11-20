// iOS kopyasına özgü dosya (Android'de karşılığı yok).
// Android kodu rastgeleliği üç yerden alıyor: kotlin.random.Random.Default (`.random()`, `.shuffled()`),
// tohumsuz `java.util.Random()` ve `Math.random()`. Ortak kodda hepsi bu tek kaynaktan beslenir;
// böylece testlerde tohum verilerek üretim tekrarlanabilir olur.
package com.sorobit.shared.matematik

import kotlin.random.Random

object RastgeleKaynagi {
    /**
     * Üreteçlerin kullandığı kaynak. Varsayılan tohumsuzdur (Android'deki davranış).
     * Testlerde `Random(tohum)` atanır; iş bitince [varsayilanaDon] çağrılır.
     * Not: genel (global) durumdur; farklı iş parçacıklarından aynı anda değiştirilmemelidir.
     */
    var rastgele: Random = Random.Default

    fun varsayilanaDon() {
        rastgele = Random.Default
    }

    /**
     * Android'deki tohumsuz `java.util.Random()` karşılığı: her çağrıda yeni bir örnek;
     * tohumu [rastgele] kaynağından çekilir.
     */
    fun yeniJavaRandom(): JavaRandom = JavaRandom(rastgele.nextLong())
}

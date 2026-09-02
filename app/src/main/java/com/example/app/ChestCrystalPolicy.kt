package com.example.app

enum class ChestRewardType {
    GOLD,
    KEY,
}

data class ChestRewardOutcome(
    val type: ChestRewardType,
    val amount: Int,
    val iconRes: Int,
    val label: String,
)

/**
 * Kristal ödülünün GÖRSEL tarafı.
 *
 * Hangi videonun oynayacağı ve ödülün ne olduğu artık burada çekilmez — zar sunucuda
 * atılır (`openCrystalReward`, bkz. [ServerRewards]). Bu nesne yalnızca sunucudan gelen
 * sonucu ekranda göstermek için ikon/etiket üretir.
 *
 * Olasılık ve ödül tabloları functions/index.js içindedir; değiştirilirse iki taraf
 * birlikte güncellenmelidir.
 */
object ChestCrystalPolicy {

    /** Sunucudan geçerli bir video adı gelmezse oynatılacak video. */
    const val FALLBACK_VIDEO = "crystal_blue_blue"

    /** Sunucudan gelen sonucu ekranda gösterilecek hale getirir. */
    fun outcomeFromServer(rewardType: String, amount: Int): ChestRewardOutcome =
        if (rewardType == "KEY") {
            ChestRewardOutcome(
                type = ChestRewardType.KEY,
                amount = amount,
                iconRes = R.drawable.key,
                label = "${amount}x",
            )
        } else {
            ChestRewardOutcome(
                type = ChestRewardType.GOLD,
                amount = amount,
                iconRes = R.drawable.open_chest,
                label = "$amount altın",
            )
        }
}

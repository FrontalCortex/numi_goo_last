// Kaynak: NewChestFragment.kt — `RarityTheme`, `ChestRarity` (iç içe türler), sabitler, dokunma
// durum makinesi (onScreenTapped), sunucu sonucunun işlenmesi (requestOutcomeFromServer),
// ödül gösterimi hesabı (showReward), adım göstergesi kuralı (updateStepIndicators),
// kapanış yolu (closeFragment); ChestSoundPlayer.kt — playForRarity ton hesabı.
// Farklar: görünüm/animasyon kodu yok; R.drawable.* → kaynak adı (String); renkler ARGB Int olarak
// aynen korunur. Android'de `NewChestFragment.ChestRarity` iç içe idi, burada üst düzey tür.
package com.sorobit.shared.oyun

data class RarityTheme(
    val bgColor: Int,
    val platformColor: Int,
    val starColor: Int,
    val stepActiveColor: Int,
    val stepFutureColor: Int,
)

/** Sandık nadirlik seviyeleri. `name` değerleri sunucuya gider/sunucudan gelir — değiştirilmemeli. */
enum class ChestRarity(
    val drawableRes: String,
    val openDrawableRes: String,
    val label: String,
    val labelColor: Int,
    val theme: RarityTheme,
) {
    COMMON(
        "new_chest_close_ic1", "new_chest_open_ic1", "SIRADAN", 0xFFFFB300.toInt(),
        RarityTheme(0xFF141F23.toInt(), 0xFF263238.toInt(), 0xFFFFD54F.toInt(), 0xFFFFB300.toInt(), 0xFF3A3010.toInt()),
    ),
    RARE(
        "new_chest_close_ic2", "new_chest_open_ic2", "ENDER", 0xFFFFFFFF.toInt(),
        RarityTheme(0xFF4AC3F6.toInt(), 0xFF2A9FE5.toInt(), 0xFFBCE5FB.toInt(), 0xFFBCE5FB.toInt(), 0xFF2A9FE5.toInt()),
    ),
    EPIC(
        "new_chest_close_ic3", "new_chest_open_ic3", "DESTANSI", 0xFFFFFFFF.toInt(),
        RarityTheme(0xFFAB47BC.toInt(), 0xFF8E24AA.toInt(), 0xFFE1BEE7.toInt(), 0xFFE1BEE7.toInt(), 0xFF8E24AA.toInt()),
    ),
    LEGENDARY(
        "new_chest_close_ic4", "new_chest_open_ic4", "EFSANEVİ", 0xFFFFFFFF.toInt(),
        RarityTheme(0xFFFF9800.toInt(), 0xFFF57C00.toInt(), 0xFFFFE0B2.toInt(), 0xFFFFE0B2.toInt(), 0xFFF57C00.toInt()),
    ),
    ;

    companion object {
        /** `ChestRarity.valueOf(name)`; tanınmayan adda null. */
        fun fromNameOrNull(name: String?): ChestRarity? = entries.firstOrNull { it.name == name }

        /** `onViewCreated`: başlangıç nadirliği; ad yoksa/tanınmıyorsa COMMON. */
        fun startRarityFromArgument(name: String?): ChestRarity = fromNameOrNull(name) ?: COMMON
    }
}

/** Bir ekran dokunuşunun sonucu. */
enum class ChestTapAction {
    /** Sunucu sonucu gelmedi ya da kapanmaya hazır değil: hiçbir şey yapma. */
    IGNORED,

    /** Adım tıklaması (1..3): nadirlik gösterildi. */
    STEP,

    /** 4. tıklama: sandığı aç. */
    OPEN_CHEST,

    /** Açıldıktan sonraki tıklama: ekranı kapat. */
    CLOSE,
}

data class ChestTapResult(
    val action: ChestTapAction,
    /** STEP: nadirlik değişti → tema değişimi + zıplama; değişmedi → yalnızca zıplama. */
    val rarityChanged: Boolean = false,
    /** STEP: 3. adımdan sonra ipucu metni "Sandığı açmak için dokun!" olur. */
    val showOpenHint: Boolean = false,
    /** STEP / OPEN_CHEST: çalınacak sesin hız çarpanı (bkz. [NewChestLogic.soundRateForRarity]). */
    val soundRate: Float = 1f,
)

/** Açılan sandığın ödül gösterimi. */
data class ChestRewardDisplay(
    val earnedGold: Int,
    val earnedKey: Int,
    val textValue: String,
    /** Metnin solundaki küçük ikon ("key" / "gold_ic"). */
    val textDrawable: String,
    /** Büyük ödül görseli ("key" / "shop_coin_ic1..3"). */
    val coinDrawable: String,
)

/** Adım göstergesi dairesinin durumu. */
enum class ChestStepIndicator { DONE, ACTIVE, FUTURE }

object NewChestLogic {
    const val ARG_START_RARITY = "start_rarity"
    const val ARG_AD_NONCE = "ad_nonce"
    const val RESULT_EARNED_GOLD = "earned_gold"
    const val RESULT_EARNED_KEY = "earned_key"
    const val RESULT_CHEST_CLOSED = "chest_closed"
    const val HINT_OPEN_CHEST = "Sandığı açmak için dokun!"

    /** İpucu metni bu kadar hareketsizlikten sonra görünür. */
    const val HINT_DELAY_MS = 5000L

    /** `ChestSoundPlayer.playForRarity`: nadirlik yükseldikçe ses incelir (1.0 … 1.75). */
    fun soundRateForRarity(rarityOrdinal: Int): Float = (1f + rarityOrdinal * 0.25f).coerceIn(1f, 2f)

    /** `updateStepIndicators`: [index] (0..2) numaralı dairenin durumu. */
    fun stepIndicator(index: Int, tapCount: Int): ChestStepIndicator = when {
        index < tapCount -> ChestStepIndicator.DONE
        index == tapCount && tapCount < 3 -> ChestStepIndicator.ACTIVE
        else -> ChestStepIndicator.FUTURE
    }

    /** `showReward` içindeki hesap. */
    fun rewardDisplay(serverRewardType: String, serverRewardAmount: Int, currentRarity: ChestRarity): ChestRewardDisplay {
        val isKeyReward = serverRewardType == "KEY"
        val earnedGold = if (isKeyReward) 0 else serverRewardAmount
        val earnedKey = if (isKeyReward) serverRewardAmount else 0

        val textValue = serverRewardAmount.toString()
        val textDrawable = if (isKeyReward) "key" else "gold_ic"
        val coinDrawable = when {
            isKeyReward -> "key"
            currentRarity == ChestRarity.LEGENDARY -> "shop_coin_ic3"
            currentRarity == ChestRarity.EPIC -> "shop_coin_ic2"
            else -> "shop_coin_ic1"
        }
        return ChestRewardDisplay(earnedGold, earnedKey, textValue, textDrawable, coinDrawable)
    }

    /** Sunucudan gelen nadirlik adları; tanınmayan adlar atlanır (dizin kayar — Android ile aynı). */
    fun rarityPathFromServer(names: List<String>): List<ChestRarity> =
        names.mapNotNull { ChestRarity.fromNameOrNull(it) }

    /** `closeFragment`: geri yığınının en üstündeki ada göre kapanış yolu. */
    enum class CloseRoute {
        /** "map_chest": sağa kayma animasyonu (300 ms, yedek 600 ms), sonra yığından çıkar. */
        MAP_CHEST_SLIDE,

        /** "mission_chest" / "shop_chest": anında yığından çıkar. */
        POP_NAMED,

        /** Diğer: görev katmanını animasyonla kapat (yoksa en üsttekini çıkar). */
        FINISH_OVERLAY,
    }

    fun closeRoute(topBackStackName: String?): CloseRoute = when (topBackStackName) {
        "map_chest" -> CloseRoute.MAP_CHEST_SLIDE
        "mission_chest", "shop_chest" -> CloseRoute.POP_NAMED
        else -> CloseRoute.FINISH_OVERLAY
    }
}

/**
 * NewChestFragment'in oyun durumu ve dokunma durum makinesi.
 * Zar sunucuda atılır: nadirlik yükseltme adımları [serverRarityPath]'te sırayla oynatılır.
 */
class NewChestState(startRarity: ChestRarity = ChestRarity.COMMON) {
    /** Ödül animasyonu bitince true yapılır (bkz. [onRewardAnimationEnd]). */
    var canClose = false
        private set
    var currentRarity = startRarity
        private set
    var earnedGoldAmount = 0
        private set
    var earnedKeyAmount = 0
        private set

    var serverRarityPath: List<ChestRarity> = emptyList()
        private set
    var serverRewardType: String = "GOLD"
        private set
    var serverRewardAmount: Int = 0
        private set
    var outcomeReady = false
        private set

    /** 0 = hiç tıklanmadı, 1..3 = adımlar, 4 = açılmaya hazır/açılıyor, 5 = kapandı */
    var tapCount = 0
        private set

    /** `requestOutcomeFromServer` başarı dalı. */
    fun applyServerOutcome(outcome: ServerRewards.ChestOutcome) {
        serverRarityPath = NewChestLogic.rarityPathFromServer(outcome.rarityPath)
        serverRewardType = outcome.rewardType
        serverRewardAmount = outcome.rewardAmount
        outcomeReady = true
    }

    fun onScreenTapped(): ChestTapResult {
        // Sunucu sonucu gelmeden sandık oynatılmaz.
        if (!outcomeReady) return ChestTapResult(ChestTapAction.IGNORED)
        when (tapCount) {
            0, 1, 2 -> {
                // Adım tıklaması: sunucunun çektiği nadirliği göster
                val newRarity = serverRarityPath.getOrNull(tapCount) ?: currentRarity
                tapCount++
                val rarityChanged = newRarity != currentRarity
                currentRarity = newRarity
                return ChestTapResult(
                    action = ChestTapAction.STEP,
                    rarityChanged = rarityChanged,
                    showOpenHint = tapCount == 3,
                    soundRate = NewChestLogic.soundRateForRarity(currentRarity.ordinal),
                )
            }
            3 -> {
                // Sandığı aç
                tapCount++
                return ChestTapResult(
                    action = ChestTapAction.OPEN_CHEST,
                    soundRate = NewChestLogic.soundRateForRarity(currentRarity.ordinal),
                )
            }
            4 -> {
                // Açıldıktan sonra kapat
                if (canClose) {
                    tapCount++
                    return ChestTapResult(ChestTapAction.CLOSE)
                }
            }
        }
        return ChestTapResult(ChestTapAction.IGNORED)
    }

    /** `showReward`: kazanılan miktarları kaydeder ve gösterim bilgisini döndürür. */
    fun showReward(): ChestRewardDisplay {
        val display = NewChestLogic.rewardDisplay(serverRewardType, serverRewardAmount, currentRarity)
        earnedGoldAmount = display.earnedGold
        earnedKeyAmount = display.earnedKey
        return display
    }

    /** Ödül animasyonu bitti: artık dokununca kapanabilir. */
    fun onRewardAnimationEnd() {
        canClose = true
    }

    /** `closeFragment`: "chest_closed" sonucuyla gönderilen değerler. */
    fun closeResult(): Map<String, Int> = mapOf(
        NewChestLogic.RESULT_EARNED_GOLD to earnedGoldAmount,
        NewChestLogic.RESULT_EARNED_KEY to earnedKeyAmount,
    )
}

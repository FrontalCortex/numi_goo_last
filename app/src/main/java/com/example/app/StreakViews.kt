package com.example.app

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Seri ekranlarının ortak parçaları: seçenek satırları ve hafta şeridi.
 *
 * İkisi de hem kurulum akışında ([StreakOnboardingFragment]) hem seri ekranında
 * ([StreakFragment]) görünüyor. XML'de tutulsalardı her satır için ayrı kimlik gerekirdi ve
 * iki ekranda iki kopya olurdu; burada tek kaynak.
 */
object StreakViews {

    private const val COLOR_MUTED = "#93A5B3"
    private const val COLOR_ACCENT = "#FF9800"

    /**
     * Seçenek satırlarını [container] içine üretir (önce içini boşaltır).
     *
     * @param values Satırların taşıdığı değerler; seçim bunlarla bildiriliyor.
     * @param labels Sol taraftaki ana metin.
     * @param trailing Sağ taraftaki açıklama.
     * @param selected Seçili değer; hiçbiri seçili değilse [values] dışında bir sayı verin.
     */
    fun buildOptionRows(
        container: ViewGroup,
        values: List<Int>,
        labels: List<String>,
        trailing: List<String>,
        selected: Int,
        onPick: (Int) -> Unit,
    ) {
        container.removeAllViews()
        val context = container.context
        val density = context.resources.displayMetrics.density
        values.forEachIndexed { index, value ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(
                    if (value == selected) R.drawable.bg_streak_option_selected
                    else R.drawable.bg_streak_option,
                )
                val pad = (16 * density).toInt()
                setPadding(pad, pad, pad, pad)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { if (index > 0) topMargin = (12 * density).toInt() }
                setOnClickListener { onPick(value) }
            }
            row.addView(
                TextView(context).apply {
                    text = labels.getOrElse(index) { "" }
                    setTextColor(Color.WHITE)
                    textSize = 17f
                    setTypeface(typeface, Typeface.BOLD)
                    layoutParams =
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
            row.addView(
                TextView(context).apply {
                    text = trailing.getOrElse(index) { "" }
                    setTextColor(Color.parseColor(COLOR_MUTED))
                    textSize = 15f
                },
            )
            container.addView(row)
        }
    }

    /**
     * Kilometre taşı satırlarını çizer.
     *
     * Toplanmış taşlar listelenmiyor: ekranda yer kaplayıp ilgiyi asıl iki şeyden —
     * toplanacak ödül ve sıradaki hedef — uzaklaştırırlardı.
     *
     * @param streak SUNUCUNUN bildiği seri. Yerel sayaç değil: ödülü veren taraf sunucu,
     *   bu yüzden "toplanabilir" kararı da onun değerine bakmalı, yoksa kullanıcıya
     *   basılabilir ama her seferinde hata veren bir düğme göstermiş oluruz.
     */
    fun buildMilestoneRows(
        container: ViewGroup,
        streak: Int,
        claimed: Set<Int>,
        onClaim: (Int) -> Unit,
    ) {
        container.removeAllViews()
        val context = container.context
        val density = context.resources.displayMetrics.density

        StreakMilestones.visible(streak, claimed).forEach { milestone ->
            val reward = StreakMilestones.rewardFor(milestone) ?: return@forEach
            val claimable = milestone <= streak

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(
                    if (claimable) R.drawable.bg_streak_option_selected
                    else R.drawable.bg_streak_option,
                )
                val pad = (14 * density).toInt()
                setPadding(pad, pad, pad, pad)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = (10 * density).toInt() }
            }

            val texts = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(
                TextView(context).apply {
                    text = "$milestone gün"
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                },
            )
            texts.addView(
                TextView(context).apply {
                    text = reward.label
                    setTextColor(Color.parseColor(if (claimable) COLOR_ACCENT else COLOR_MUTED))
                    textSize = 13f
                },
            )
            row.addView(texts)

            if (claimable) {
                row.addView(
                    TextView(context).apply {
                        text = "Topla"
                        setTextColor(Color.parseColor("#1B2B31"))
                        textSize = 14f
                        setTypeface(typeface, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setBackgroundResource(R.drawable.bg_streak_claim)
                        val padH = (18 * density).toInt()
                        val padV = (8 * density).toInt()
                        setPadding(padH, padV, padH, padV)
                        setOnClickListener { onClaim(milestone) }
                    },
                )
            } else {
                row.addView(
                    TextView(context).apply {
                        val left = milestone - streak
                        text = "$left gün kaldı"
                        setTextColor(Color.parseColor(COLOR_MUTED))
                        textSize = 13f
                    },
                )
            }
            container.addView(row)
        }
    }

    /**
     * Haftanın günlerini pazartesiden pazara [container] içine çizer.
     *
     * Son 7 gün değil TAKVİM HAFTASI gösteriliyor: kullanıcı "bu hafta neredeyim" diye
     * bakıyor, "son yedi günde" diye değil.
     */
    fun buildWeekStrip(container: ViewGroup, achievedDays: Set<String>) {
        container.removeAllViews()
        val context = container.context
        val density = context.resources.displayMetrics.density
        val today = StudyTimeTracker.dayId()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        // Calendar.DAY_OF_WEEK: Pazar=1 … Cumartesi=7. Pazartesi başlangıcı için kaydırma.
        val shift = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -shift)

        val letters = listOf("P", "S", "Ç", "P", "C", "C", "P")
        for (i in 0 until 7) {
            val dayId = format.format(Date(cal.timeInMillis))
            val done = dayId in achievedDays
            val isToday = dayId == today

            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            cell.addView(
                TextView(context).apply {
                    text = if (done) "✓" else ""
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setBackgroundResource(
                        when {
                            done -> R.drawable.bg_streak_day_done
                            isToday -> R.drawable.bg_streak_day_today
                            else -> R.drawable.bg_streak_day_empty
                        },
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        (28 * density).toInt(),
                        (28 * density).toInt(),
                    )
                },
            )
            cell.addView(
                TextView(context).apply {
                    text = letters[i]
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor(if (isToday) COLOR_ACCENT else COLOR_MUTED))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = (6 * density).toInt() }
                },
            )
            container.addView(cell)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
}

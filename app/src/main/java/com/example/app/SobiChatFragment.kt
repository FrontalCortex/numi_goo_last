package com.example.app

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.homework.DepthPress
import com.google.firebase.auth.FirebaseAuth

/**
 * Sobi sekmesi (kullanıcı kararı 02.11.2026): öğrencinin alt menüsünde eski "Sohbet" sekmesinin yerinde,
 * yapay zekâ uygulamalarının telefon sürümleri gibi tam ekran bir sohbet. Ders dışı "genel sohbet":
 * sunucu (`sobiChat`, `mode: 'genel'`) uygulama rehberini ve çocuğun kural başarısını biliyor, yalnızca
 * soroban ve uygulama hakkında konuşuyor (functions/sobi/prompt_genel_tr.md). Ekonomi ders içi Sobi
 * ile aynı: günlük ücretsiz hak + Sobi kredisi.
 *
 *  - Sol üstte ☰: soldan kayan sohbet listesi (Yeni sohbet + önceki sohbetler, `sobiSessions`); uzun
 *    basınca ya da sağa kaydırınca sil. Tablette (sw600dp) liste solda kalıcı.
 *  - Boş sohbette selam veren Sobi ve başlangıç balonları; "Krediler nasıl çalışır?" sunucuda hazır
 *    cevap (ücretsiz).
 *  - Balonlar, beğen / beğenme / bildir ve sesli okuma ders içi Sobi ile aynı ([SobiMessageViews],
 *    [SobiSpeaker]); bakiye satırı [SobiBalanceBar].
 *  - Sobi'nin ekran işleri sohbete düğme olarak düşer: Teste başla ([SobiTestSheet]), Kredi al, Pro,
 *    ve bir işlemi abaküste gör / çöz (abaküslü "Sobi ile analiz et" ekranı, [MistakeReviewDialog]).
 *
 * Öğretmen hesabında ve [TeacherQuestionsFeature.ENABLED] açıkken bu ekran yok, eski Sohbet sekmesi
 * duruyor (MainActivity.applySobiTab).
 */
class SobiChatFragment : Fragment() {

    /** Sohbetteki bir satır: mesaj ve Sobi'nin o cevapla sohbete koyduğu düğmeler. */
    private class Item(val message: SobiStepTracker.Message, val buttons: List<ServerSobi.Action> = emptyList())

    private var drawer: DrawerLayout? = null
    private lateinit var scroll: ScrollView
    private lateinit var messagesView: LinearLayout
    private lateinit var emptyView: View
    private lateinit var input: EditText
    private lateinit var speakToggle: ImageButton
    private lateinit var listView: RecyclerView
    private lateinit var listEmpty: TextView
    private lateinit var messageViews: SobiMessageViews
    private var balanceBar: SobiBalanceBar? = null
    private var speaker: SobiSpeaker? = null
    private var mascot: BunnyMascotView? = null
    private val adapter = SessionAdapter()
    private var sending = false
    private var shownEmpty: Boolean? = null
    /**
     * Son açılan sohbet ve satırları: sekmeden çıkıp dönünce aynı sohbet görünsün (yalnızca bu süreçte).
     * Hesap değişince (çıkış, başka çocuk) önceki sohbet görünmesin diye kullanıcıya bağlı.
     */
    private val state: State
        get() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (lastState.uid != uid) lastState = State(uid)
            return lastState
        }

    private val loggedIn: Boolean get() = FirebaseAuth.getInstance().currentUser != null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_sobi_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawer = view as? DrawerLayout
        scroll = view.findViewById(R.id.sobiChatScroll)
        messagesView = view.findViewById(R.id.sobiChatMessages)
        emptyView = view.findViewById(R.id.sobiChatEmpty)
        input = view.findViewById(R.id.sobiChatInput)
        speakToggle = view.findViewById(R.id.sobiChatSpeak)
        listView = view.findViewById(R.id.sobiDrawerList)
        listEmpty = view.findViewById(R.id.sobiDrawerEmpty)
        mascot = view.findViewById(R.id.sobiChatMascot)

        // Tablette liste zaten görünüyor: ☰ gereksiz.
        val menu = view.findViewById<View>(R.id.sobiChatMenu)
        if (drawer == null) menu.visibility = View.GONE
        else menu.setOnClickListener {
            refreshList()
            drawer?.openDrawer(GravityCompat.START)
        }
        view.findViewById<View>(R.id.sobiChatNew).setOnClickListener { newChat() }
        view.findViewById<View>(R.id.sobiDrawerNew).setOnClickListener { newChat() }
        DepthPress.attach(view.findViewById(R.id.sobiChatNew), view.findViewById(R.id.sobiDrawerNew))

        messageViews = SobiMessageViews(
            fragment = this,
            sessionId = { state.sessionId },
            localScreen = "sobi_sekme",
            onChanged = { render() },
            onReported = { addSobi(SobiStepTracker.Message(fromChild = false, text = appString(R.string.sobi_report_done))) },
            onSpeak = { speaker?.speak(it) },
            canSpeak = { speaker?.available == true },
        )
        speaker = SobiSpeaker(requireContext()) {
            view.post {
                if (!isAdded) return@post
                speakToggle.visibility = View.VISIBLE
                updateSpeakToggle()
                render()
            }
        }
        speakToggle.setOnClickListener {
            val s = speaker ?: return@setOnClickListener
            s.autoRead = !s.autoRead
            if (!s.autoRead) s.stop()
            updateSpeakToggle()
        }
        DepthPress.attach(speakToggle)

        balanceBar = SobiBalanceBar(
            balanceView = view.findViewById(R.id.sobiChatBalance),
            trialView = view.findViewById(R.id.sobiChatProTrial),
            activity = { activity as? MainActivity },
            onBuyCredits = { openCredits() },
            onProTrial = { openPro() },
        )

        val send = view.findViewById<View>(R.id.sobiChatSend)
        send.setOnClickListener { sendFromInput() }
        DepthPress.attach(send)
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendFromInput(); true } else false
        }
        // Sobi'ye yazmak oturum ister (sunucu çağrısı); alt menü zaten girişe yönlendiriyor.
        view.findViewById<View>(R.id.sobiChatInputRow).visibility = if (loggedIn) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.sobiChatLogin).visibility = if (loggedIn) View.GONE else View.VISIBLE

        setupStarters(view.findViewById(R.id.sobiChatStarters))
        view.findViewById<EditText>(R.id.sobiDrawerSearch).addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                query = s?.toString().orEmpty()
                applyFilter()
            }
        })
        listView.layoutManager = LinearLayoutManager(requireContext())
        listView.adapter = adapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val info = adapter.items.getOrNull(vh.bindingAdapterPosition)
                // Satır onaydan önce yerine dönsün; silinirse liste yenilenince gider.
                adapter.notifyItemChanged(vh.bindingAdapterPosition)
                if (info != null) confirmDelete(info)
            }
        }).attachToRecyclerView(listView)

        if (loggedIn) {
            ServerSobi.balance({ b -> if (this.view != null) balanceBar?.show(b.freeLeft, b.credits) })
            refreshList()
        }
        render()
        if (savedInstanceState == null) AnalyticsLogger.logSobiTabOpen()
    }

    override fun onDestroyView() {
        stopTyping()
        speaker?.shutdown()
        speaker = null
        mascot = null
        balanceBar = null
        super.onDestroyView()
    }

    private fun updateSpeakToggle() {
        speakToggle.alpha = if (speaker?.autoRead == true) 1f else 0.4f
    }

    // ── Başlangıç balonları ─────────────────────────────────────────────────

    private fun setupStarters(box: LinearLayout) {
        val ctx = box.context
        val starters = listOf(
            R.string.sobi_tab_starter_today to null,
            R.string.sobi_tab_starter_ten to null,
            R.string.sobi_tab_starter_test to null,
            R.string.sobi_tab_starter_credits to ServerSobi.KEY_CREDITS,
        )
        for ((label, key) in starters) {
            val text = appString(label)
            val chip = TextView(ctx).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_sobi_pill)
                setPadding(dp(ctx, 16), dp(ctx, 9), dp(ctx, 16), dp(ctx, 9))
                gravity = Gravity.CENTER
                setOnClickListener { send(text, key) }
            }
            DepthPress.attach(chip)
            box.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(ctx, 8) })
        }
    }

    // ── Sohbet ─────────────────────────────────────────────────────────────

    private fun render() {
        val ctx = context ?: return
        messagesView.removeAllViews()
        for (item in state.items) {
            if (item === typing) {
                // Yazılırken: metnin gelen kısmı; oy satırı ve düğmeler yazı bitince (kopya oylanamıyor).
                messagesView.addView(messageViews.bubble(ctx, item.message.copy(
                    text = item.message.text.take(typedChars), serverIndex = null, localId = null,
                )))
                continue
            }
            messagesView.addView(messageViews.bubble(ctx, item.message))
            if (item.buttons.isNotEmpty()) messagesView.addView(buttonRow(ctx, item.buttons))
        }
        if (sending) messagesView.addView(thinkingBubble(ctx))
        val empty = state.items.isEmpty() && !sending
        // Sobi boş sohbete her geçişte bir kez el sallar (yeniden çizimlerde değil).
        if (empty && shownEmpty != true) mascot?.post { mascot?.greet() }
        shownEmpty = empty
        emptyView.visibility = if (empty) View.VISIBLE else View.GONE
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addSobi(m: SobiStepTracker.Message, buttons: List<ServerSobi.Action> = emptyList(), animate: Boolean = false) {
        val item = Item(m, buttons)
        state.items += item
        if (view == null) return
        if (animate) startTyping(item) else render()
        speaker?.speakIfAuto(m.text)
    }

    // ── Akarak yazma ───────────────────────────────────────────────────────
    //
    // Sobi'nin cevabı sunucuda bütün hâlinde denetlendikten sonra geliyor (guard: kaynaksız sayı, uzunluk);
    // gerçek akış (streaming) denetlenmemiş metni çocuğa gösterirdi. Bu yüzden cevap geldikten sonra
    // ekranda yazılıyormuş gibi açılıyor (kullanıcı isteği 02.11.2026): ~1 saniye, uzun cevapta da.

    private var typing: Item? = null
    private var typedChars = 0
    private val typingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val typingTick = object : Runnable {
        override fun run() {
            val item = typing ?: return
            val len = item.message.text.length
            typedChars = minOf(len, typedChars + maxOf(2, len / TYPING_STEPS))
            if (typedChars >= len) typing = null
            render()
            if (typing != null) typingHandler.postDelayed(this, TYPING_TICK_MS)
        }
    }

    private fun startTyping(item: Item) {
        stopTyping()
        typing = item
        typedChars = 0
        typingTick.run()
    }

    /** Yazı yarıda kalırsa (yeni mesaj, sohbet değişti, ekran kapandı) mesaj bütün görünür. */
    private fun stopTyping() {
        typingHandler.removeCallbacks(typingTick)
        typing = null
    }

    private fun sendFromInput() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isEmpty() || sending) return
        input.setText("")
        hideKeyboard()
        send(text)
    }

    private fun send(text: String, suggestion: String? = null) {
        if (sending || !loggedIn) return
        stopTyping()
        sending = true
        state.items += Item(SobiStepTracker.Message(fromChild = true, text = text))
        AnalyticsLogger.logSobiMessage(AnalyticsLogger.SOBI_SCREEN_TAB)
        render()
        val forSession = state.sessionId
        ServerSobi.sendGeneral(
            sessionId = forSession,
            message = text,
            suggestion = suggestion,
            onResult = { reply ->
                sending = false
                // Bu arada başka bir sohbete geçildiyse cevap o sohbete düşmesin (sunucuda kayıtlı).
                if (state.sessionId != forSession) return@sendGeneral
                state.sessionId = reply.sessionId
                if (reply.remainingToday != null && reply.credits != null && view != null) {
                    balanceBar?.show(reply.remainingToday, reply.credits)
                }
                val shown = if (reply.turnLimitReached) appString(R.string.sobi_turn_limit) else reply.text?.takeIf { it.isNotBlank() }
                val index = if (reply.turnLimitReached) null else reply.messageIndex
                val buttons = reply.actions.filter { it.kind in BUTTON_KINDS }
                if (shown != null) addSobi(SobiStepTracker.Message(fromChild = false, text = shown, serverIndex = index), buttons, animate = true)
                else if (view != null) render()
                // Yeni sohbet listede görünsün (başlığı ilk mesajdan).
                if (forSession == null) refreshList()
            },
            onFailure = { e ->
                sending = false
                if (state.sessionId != forSession) return@sendGeneral
                val msg = when {
                    ServerSobi.isDailyLimit(e) -> {
                        AnalyticsLogger.logSobiLimit(AnalyticsLogger.SOBI_SCREEN_TAB)
                        if (view != null) balanceBar?.show(0, 0)
                        appString(
                            if (SobiProTrialFragment.canOfferTrial(activity as? MainActivity)) R.string.sobi_error_daily_limit_trial
                            else R.string.sobi_error_daily_limit,
                        )
                    }
                    ServerSobi.isRestricted(e) -> appString(R.string.sobi_error_restricted)
                    else -> appString(R.string.sobi_error_network)
                }
                addSobi(SobiStepTracker.Message(fromChild = false, text = msg))
            },
        )
    }

    private fun newChat() {
        stopTyping()
        closeDrawer()
        if (sending) return
        state.sessionId = null
        state.items.clear()
        speaker?.stop()
        render()
        adapter.notifyDataSetChanged()
    }

    private fun openSession(info: ServerSobi.SessionInfo) {
        closeDrawer()
        if (sending || info.id == state.sessionId) return
        speaker?.stop()
        ServerSobi.loadSession(info.id, { list ->
            stopTyping()
            state.sessionId = info.id
            state.items.clear()
            for (m in list) {
                val msg = SobiStepTracker.Message(
                    fromChild = m.fromChild, text = m.text, serverIndex = if (m.fromChild) null else m.index,
                    rating = m.rating, reported = m.reported,
                )
                state.items += Item(msg, m.buttons.filter { it.kind in BUTTON_KINDS })
            }
            if (view == null) return@loadSession
            render()
            adapter.notifyDataSetChanged()
        }, {
            if (view == null) return@loadSession
            context?.let { android.widget.Toast.makeText(it, appString(R.string.sobi_tab_load_error), android.widget.Toast.LENGTH_SHORT).show() }
        })
    }

    private fun closeDrawer() {
        drawer?.closeDrawer(GravityCompat.START)
    }

    // ── Sohbete düğme olarak düşen ekran işleri ─────────────────────────────

    private fun buttonRow(ctx: Context, buttons: List<ServerSobi.Action>): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 4), dp(ctx, 2), 0, dp(ctx, 6))
        }
        for (a in buttons) {
            val label = buttonLabel(a) ?: continue
            val chip = TextView(ctx).apply {
                text = label
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_sobi_pill)
                setPadding(dp(ctx, 16), dp(ctx, 9), dp(ctx, 16), dp(ctx, 9))
                setOnClickListener { onButton(a) }
            }
            DepthPress.attach(chip)
            row.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(ctx, 4) })
        }
        return row
    }

    private fun buttonLabel(a: ServerSobi.Action): String? = when (a.kind) {
        ServerSobi.ACTION_TEST -> appString(R.string.sobi_tab_button_test)
        ServerSobi.ACTION_CREDITS -> appString(R.string.sobi_tab_button_credits)
        ServerSobi.ACTION_PRO -> appString(R.string.sobi_tab_button_pro)
        ServerSobi.ACTION_LESSON -> partName(a.part)?.let { appString(R.string.sobi_tab_button_lesson, it) }
        ServerSobi.ACTION_SHOW -> operationNumbers(a)?.let { appString(R.string.sobi_tab_button_show, MistakeReviewDialog.questionText(it)) }
        ServerSobi.ACTION_PRACTICE -> operationNumbers(a)?.let { appString(R.string.sobi_tab_button_practice, MistakeReviewDialog.questionText(it)) }
        else -> null
    }

    /** Ana ekrandaki bölüm kartının adı (PartCardAdapter ile aynı). */
    private fun partName(part: Int?): String? = when (part) {
        1 -> appString(R.string.ui_abakusun_temeli_ve_toplama)
        2 -> appString(R.string.ui_abakuste_cikarma)
        3 -> appString(R.string.ui_abakuste_carpma)
        4 -> appString(R.string.ui_korleme_toplama)
        5 -> appString(R.string.ui_korleme_cikarma)
        6 -> appString(R.string.ui_korleme_carpma)
        else -> null
    }

    /** "8 + 6" → [8, 6]; başlangıç 0 ise yalnızca eklenen sayı (abaküs boş başlıyor). */
    private fun operationNumbers(a: ServerSobi.Action): List<Int>? {
        val second = if (a.op == "sub") -a.number else a.number
        val nums = if (a.base == 0) listOf(second) else listOf(a.base, second)
        return nums.takeIf { MistakeReviewDialog.canReview(it) }
    }

    private fun onButton(a: ServerSobi.Action) {
        AnalyticsLogger.logSobiButton(a.kind)
        when (a.kind) {
            ServerSobi.ACTION_TEST -> SobiTestSheet(this).show()
            ServerSobi.ACTION_CREDITS -> openCredits()
            ServerSobi.ACTION_PRO -> openPro()
            ServerSobi.ACTION_LESSON -> a.part?.let { (activity as? MainActivity)?.openLessonPart(it) }
            ServerSobi.ACTION_SHOW, ServerSobi.ACTION_PRACTICE -> {
                val nums = operationNumbers(a) ?: return
                val fm = activity?.supportFragmentManager ?: return
                MistakeReviewDialog.newInstance(nums, a.result, emptyList(), null).show(fm, "SobiTabReview")
            }
        }
    }

    private fun openCredits() {
        (activity as? MainActivity)?.openShopFragment(focusSobiCredits = true)
    }

    /** Pro: denemeye uygunsa deneme tanıtımı, değilse mağaza (Pro kartı orada). */
    private fun openPro() {
        val main = activity as? MainActivity ?: return
        if (SobiProTrialFragment.canOfferTrial(main)) {
            SobiProTrialFragment.newInstance(AnalyticsLogger.PROMO_TRIGGER_SOBI_LIMIT)
                .show(main.supportFragmentManager, SobiProTrialFragment.TAG)
        } else {
            main.openShopFragment()
        }
    }

    // ── Sohbet listesi ──────────────────────────────────────────────────────

    private fun refreshList() {
        if (!loggedIn) return
        ServerSobi.listSessions({ list ->
            allSessions = list
            applyFilter()
        }, {})
    }

    /** Listenin tamamı; arama kutusu başlıklarda süzüyor (büyük/küçük harf ve Türkçe İ/ı farkı yok). */
    private var allSessions: List<ServerSobi.SessionInfo> = emptyList()
    private var query = ""

    private fun applyFilter() {
        val q = query.trim().lowercase(TR)
        adapter.items = if (q.isEmpty()) allSessions else allSessions.filter { it.title.lowercase(TR).contains(q) }
        adapter.notifyDataSetChanged()
        if (view != null) listEmpty.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(info: ServerSobi.SessionInfo) {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle(appString(R.string.sobi_tab_delete_title))
            .setMessage(appString(R.string.sobi_tab_delete_message, info.title))
            .setPositiveButton(appString(R.string.sobi_tab_delete)) { _, _ ->
                ServerSobi.deleteSession(info.id, {
                    if (state.sessionId == info.id && !sending) {
                        state.sessionId = null
                        state.items.clear()
                        if (view != null) render()
                    }
                    refreshList()
                }, { refreshList() })
            }
            .setNegativeButton(appString(R.string.sobi_tab_cancel), null)
            .show()
    }

    private inner class SessionAdapter : RecyclerView.Adapter<SessionAdapter.Holder>() {
        var items: List<ServerSobi.SessionInfo> = emptyList()

        inner class Holder(val title: TextView, val time: TextView, root: View) : RecyclerView.ViewHolder(root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val ctx = parent.context
            val title = TextView(ctx).apply {
                setTextColor(ContextCompat.getColor(ctx, R.color.dark_text))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val time = TextView(ctx).apply {
                setTextColor(ContextCompat.getColor(ctx, R.color.dark_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx, 16), dp(ctx, 10), dp(ctx, 16), dp(ctx, 10))
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(title)
                addView(time)
            }
            return Holder(title, time, root)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val info = items[position]
            holder.title.text = info.title
            holder.time.text = if (info.updatedAt > 0) {
                DateUtils.getRelativeTimeSpanString(info.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            } else ""
            // Açık sohbet hafif vurgulu.
            holder.itemView.setBackgroundColor(if (info.id == state.sessionId) 0x221CB0F6 else 0)
            holder.itemView.setOnClickListener { openSession(info) }
            holder.itemView.setOnLongClickListener { confirmDelete(info); true }
        }

        override fun getItemCount() = items.size
    }

    private fun thinkingBubble(ctx: Context): View = TextView(ctx).apply {
        text = appString(R.string.sobi_thinking)
        setTextColor(ContextCompat.getColor(ctx, R.color.dark_text_secondary))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setPadding(dp(ctx, 4), dp(ctx, 6), 0, dp(ctx, 6))
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    private class State(val uid: String?) {
        var sessionId: String? = null
        val items = ArrayList<Item>()
    }

    companion object {
        private val TR = java.util.Locale("tr", "TR")
        private const val TYPING_TICK_MS = 30L
        /** Yazının bölündüğü adım sayısı: her cevap ~1 saniyede (30 ms × ~40) açılır. */
        private const val TYPING_STEPS = 40
        private val BUTTON_KINDS = setOf(
            ServerSobi.ACTION_TEST, ServerSobi.ACTION_CREDITS, ServerSobi.ACTION_PRO,
            ServerSobi.ACTION_SHOW, ServerSobi.ACTION_PRACTICE, ServerSobi.ACTION_LESSON,
        )
        private var lastState = State(null)
    }
}

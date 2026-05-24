package com.example.app

import android.os.Bundle
import android.graphics.Rect
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.animation.ObjectAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.example.app.auth.AuthManager
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentAbacusPracticeBinding
import com.example.app.abacus.AbacusBeadController

class AbacusPracticeFragment : Fragment() {

    private var _binding: FragmentAbacusPracticeBinding? = null
    private val binding get() = _binding!!

    private lateinit var abacusController: AbacusBeadController
    private var selectedOperator: String = "+"
    private var previousSoftInputMode: Int? = null
    private var abacusMetricsInitialized: Boolean = false
    private var askQuestionBounceAnimators: List<ObjectAnimator>? = null
    private var askQuestionButtonShown: Boolean = false
    companion object {
        private const val PRACTICE_TOUCH_BLOCKER_TAG = "practice_touch_blocker"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAbacusPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // When keyboard is open, hide abacus + control button to avoid overlap.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            binding.abacusLinear.visibility = if (imeVisible) View.GONE else View.VISIBLE
            binding.kontrolButton.visibility = if (imeVisible) View.GONE else View.VISIBLE
            if (askQuestionButtonShown) {
                binding.askQuestionButton.visibility = if (imeVisible) View.GONE else View.VISIBLE
            }
            insets
        }
        binding.root.requestApplyInsets()

        // Tap outside inputs to dismiss keyboard (does not consume touches).
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focused = requireActivity().currentFocus
                if (focused is EditText) {
                    val r = Rect()
                    focused.getGlobalVisibleRect(r)
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    if (!r.contains(x, y)) {
                        hideKeyboardAndClearFocus()
                    }
                }
            }
            false
        }

        abacusController = AbacusBeadController(
            context = requireContext(),
            root = binding.root,
            animationDurationMs = 300L
        )
        abacusController.setup()
        ensureAbacusMetricsIfVisible()

        binding.correctPanel.visibility = View.GONE
        binding.incorrectPanel.visibility = View.GONE
        binding.overlay.visibility = View.GONE

        binding.operator.text = selectedOperator
        binding.operator.setOnClickListener { showOperatorPicker() }

        binding.quitButton.setOnClickListener { dismissPracticeOverlay() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (parentFragmentManager.findFragmentByTag(OperatorPickerBottomSheetFragment.TAG) != null) {
                        parentFragmentManager.popBackStack()
                        return
                    }
                    dismissPracticeOverlay()
                }
            },
        )

        parentFragmentManager.setFragmentResultListener(
            OperatorPickerBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            selectedOperator =
                result.getString(OperatorPickerBottomSheetFragment.BUNDLE_SELECTED_OP) ?: "+"
            binding.operator.text = selectedOperator
        }

        binding.kontrolButton.setOnClickListener { onKontrolClicked() }

        setupAskQuestionButton()

        binding.okayButton.setOnClickListener { hidePanelsAndReset(animated = true) }
        binding.continueButton.setOnClickListener { hidePanelsAndReset(animated = true) }
        binding.overlay.setOnClickListener { /* eat clicks while panel visible */ }
    }

    private fun setupAskQuestionButton() {
        val authManager = AuthManager().also { it.initialize(requireContext()) }
        AskQuestionButtonBinder.bind(
            fragment = this,
            button = binding.askQuestionButton,
            isTeacher = authManager.getCurrentUserType() == AuthManager.ROLE_TEACHER,
            onAllowedClick = {
                (activity as? MainActivity)?.startQuestionFlow(R.id.abacusFragmentContainer) {
                    binding.root
                }
            },
            onVisibleChanged = { visible -> askQuestionButtonShown = visible },
            onReadyForBounce = { startAskQuestionBounceAnimation() },
        )
    }

    private fun startAskQuestionBounceAnimation() {
        val btn = binding.askQuestionButton
        val translateY = ObjectAnimator.ofFloat(btn, "translationY", 0f, -20f, 0f).apply {
            duration = 1800
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        askQuestionBounceAnimators = listOf(translateY)
    }

    private fun showOperatorPicker() {
        if (parentFragmentManager.findFragmentByTag(OperatorPickerBottomSheetFragment.TAG) != null) return
        OperatorPickerBottomSheetFragment
            .newInstance(selectedOperator)
            .show(parentFragmentManager, OperatorPickerBottomSheetFragment.TAG)
    }

    private fun onKontrolClicked() {
        val a = binding.firstNumberText.text?.toString()?.trim().orEmpty().toIntOrNull() ?: 0
        val b = binding.secondNumberText.text?.toString()?.trim().orEmpty().toIntOrNull() ?: 0

        val expected: Long = when (selectedOperator) {
            "+" -> a.toLong() + b.toLong()
            "-" -> a.toLong() - b.toLong()
            "x" -> a.toLong() * b.toLong()
            else -> a.toLong() + b.toLong()
        }

        val abacusValue = abacusController.getCurrentValue().toLong()
        val isCorrect = expected == abacusValue

        if (isCorrect) {
            showResultPanel(binding.correctPanel)
        } else {
            binding.correctAnswerLabel.text = "Doğru cevap: $expected"
            binding.correctAnswerText.text = "Senin cevabın: $abacusValue"
            showResultPanel(binding.incorrectPanel)
        }
    }

    private fun showResultPanel(panel: View) {
        val other = if (panel === binding.correctPanel) binding.incorrectPanel else binding.correctPanel
        other.visibility = View.GONE

        binding.overlay.visibility = View.VISIBLE
        binding.overlay.alpha = 1f

        panel.visibility = View.VISIBLE
        panel.alpha = 0f

        // Ensure height is known before using translationY
        panel.post {
            panel.translationY = panel.height.toFloat()
            panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun hidePanelsAndReset(animated: Boolean) {
        val visiblePanel: View? = when {
            binding.correctPanel.visibility == View.VISIBLE -> binding.correctPanel
            binding.incorrectPanel.visibility == View.VISIBLE -> binding.incorrectPanel
            else -> null
        }

        if (!animated || visiblePanel == null) {
            binding.correctPanel.visibility = View.GONE
            binding.incorrectPanel.visibility = View.GONE
            binding.overlay.visibility = View.GONE
            abacusController.reset(animate = true)
            return
        }

        visiblePanel.animate()
            .translationY(visiblePanel.height.toFloat())
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                visiblePanel.visibility = View.GONE
                binding.overlay.visibility = View.GONE
                abacusController.reset(animate = true)
            }
            .start()
    }

    private fun hideKeyboardAndClearFocus() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        val v = requireActivity().currentFocus ?: binding.root
        imm?.hideSoftInputFromWindow(v.windowToken, 0)
        binding.firstNumberText.clearFocus()
        binding.secondNumberText.clearFocus()
    }

    override fun onDestroyView() {
        askQuestionBounceAnimators?.forEach { it.cancel() }
        askQuestionBounceAnimators = null
        releaseLaunchTouchBlocker()
        parentFragmentManager.clearFragmentResultListener(OperatorPickerBottomSheetFragment.REQUEST_KEY)
        _binding = null
        super.onDestroyView()
    }

    // Bottom navigation visibility is controlled centrally by MainActivity (WindowInsets listener),
    // so we don't toggle it here to avoid flicker or it being overridden.

    override fun onResume() {
        super.onResume()
        val window = requireActivity().window
        if (previousSoftInputMode == null) {
            previousSoftInputMode = window.attributes.softInputMode
        }
        // Keep abacus + control button fixed; let keyboard overlay without moving content.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        // Enter animasyonu bitene kadar tüm dokunmaları bloklu tut.
        binding.root.postDelayed({ releaseLaunchTouchBlocker() }, 320)
    }

    override fun onPause() {
        previousSoftInputMode?.let { requireActivity().window.setSoftInputMode(it) }
        super.onPause()
    }

    private fun ensureAbacusMetricsIfVisible() {
        if (abacusMetricsInitialized) return
        if (binding.abacusLinear.visibility != View.VISIBLE) return
        binding.abacusLinear.post {
            if (!isAdded || view == null) return@post
            if (abacusMetricsInitialized) return@post
            if (binding.abacusLinear.visibility != View.VISIBLE) return@post
            val ok = abacusController.computeMovementDistancesFromLayout(ratio = 1.0f, force = true)
            if (ok) {
                abacusMetricsInitialized = true
                abacusController.syncStateFromUi()
            }
        }
    }

    private fun releaseLaunchTouchBlocker() {
        val content = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        content.findViewWithTag<View>(PRACTICE_TOUCH_BLOCKER_TAG)?.let { blocker ->
            content.removeView(blocker)
        }
    }

    private fun dismissPracticeOverlay() {
        val main = activity as? MainActivity
        val base = main?.supportFragmentManager?.findFragmentById(R.id.fragmentContainerID)
        if (main != null && base is TasksFragment) {
            main.finishAbacusPracticeOverlayAnimated("AbacusPractice.dismiss")
        } else if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else if (isAdded) {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                )
                .remove(this@AbacusPracticeFragment)
                .commitAllowingStateLoss()
            main?.reconcileAbacusOverlayWhenTasksIsBase("AbacusPractice.dismiss.remove")
        }
    }
}


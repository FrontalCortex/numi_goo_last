package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.databinding.FragmentUserInfoBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class UserInfoFragment : Fragment() {

    private var _binding: FragmentUserInfoBinding? = null
    private val binding get() = _binding!!

    private var forceTeacher: Boolean = false
    private var forceStudent: Boolean = false
    private var prefillEmail: String? = null
    private var googleSignIn: Boolean = false
    private var googleEmail: String? = null
    private var googleName: String? = null

    private enum class Step { AGE, SOURCE }
    private var currentStep = Step.AGE
    private var selectedSource: String? = null
    private var validatedBirthYear: Int? = null
    private lateinit var sourceCards: List<MaterialCardView>
    private val sourceNames = listOf("Facebook", "Instagram", "Youtube", "Google Araması", "Arkadaş/Aile", "TikTok", "Uygulama Mağazası", "Diğer")

    companion object {
        private const val ARG_FORCE_TEACHER = "arg_force_teacher"
        private const val ARG_FORCE_STUDENT = "arg_force_student"
        private const val ARG_PREFILL_EMAIL = "arg_prefill_email"
        private const val ARG_GOOGLE_SIGN_IN = "arg_google_sign_in"
        private const val ARG_GOOGLE_EMAIL = "arg_google_email"
        private const val ARG_GOOGLE_NAME = "arg_google_name"

        fun newInstance(
            forceTeacher: Boolean = false,
            forceStudent: Boolean = false,
            prefillEmail: String? = null,
            googleSignIn: Boolean = false,
            googleEmail: String? = null,
            googleName: String? = null
        ): UserInfoFragment {
            return UserInfoFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FORCE_TEACHER, forceTeacher)
                    putBoolean(ARG_FORCE_STUDENT, forceStudent)
                    prefillEmail?.let { putString(ARG_PREFILL_EMAIL, it) }
                    putBoolean(ARG_GOOGLE_SIGN_IN, googleSignIn)
                    googleEmail?.let { putString(ARG_GOOGLE_EMAIL, it) }
                    googleName?.let { putString(ARG_GOOGLE_NAME, it) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forceTeacher = arguments?.getBoolean(ARG_FORCE_TEACHER, false) ?: false
        forceStudent = arguments?.getBoolean(ARG_FORCE_STUDENT, false) ?: false
        prefillEmail = arguments?.getString(ARG_PREFILL_EMAIL)
        googleSignIn = arguments?.getBoolean(ARG_GOOGLE_SIGN_IN, false) ?: false
        googleEmail = arguments?.getString(ARG_GOOGLE_EMAIL)
        googleName = arguments?.getString(ARG_GOOGLE_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep == Step.SOURCE) {
                    currentStep = Step.AGE
                    binding.sourceContainer.visibility = View.GONE
                    binding.ageContainer.visibility = View.VISIBLE
                    val age = binding.etAge.text?.toString()?.trim()?.toIntOrNull()
                    updateContinueButton(enabled = age != null && age in 1..120)
                } else {
                    isEnabled = false
                    parentFragmentManager.popBackStack()
                }
            }
        })

        updateContinueButton(enabled = false)

        if (!prefillEmail.isNullOrEmpty()) {
            Snackbar.make(
                binding.root,
                "Bu e-posta adresi kayıtlı değil. Kayıt sayfasına yönlendirildin.",
                Snackbar.LENGTH_LONG
            ).show()
        }

        binding.etAge.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (currentStep == Step.AGE) {
                    val age = s?.toString()?.trim()?.toIntOrNull()
                    val valid = age != null && age in 1..120
                    updateContinueButton(enabled = valid)
                }
            }
        })

        sourceCards = listOf(
            binding.cardSourceFacebook, binding.cardSourceInstagram,
            binding.cardSourceYoutube, binding.cardSourceGoogle,
            binding.cardSourceFriends, binding.cardSourceTiktok,
            binding.cardSourceStore, binding.cardSourceOther
        )

        sourceCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                selectedSource = sourceNames[index]
                updateSourceSelection()
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentStep == Step.SOURCE) {
                currentStep = Step.AGE
                binding.sourceContainer.visibility = View.GONE
                binding.ageContainer.visibility = View.VISIBLE
                val age = binding.etAge.text?.toString()?.trim()?.toIntOrNull()
                updateContinueButton(enabled = age != null && age in 1..120)
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        binding.btnContinue.setOnClickListener {
            if (currentStep == Step.AGE) {
                val ageText = binding.etAge.text?.toString()?.trim() ?: ""
                val age = ageText.toIntOrNull()

                if (age == null || age !in 1..120) {
                    Toast.makeText(requireContext(), "Lütfen geçerli bir yaş girin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                validatedBirthYear = currentYear - age
                
                hideKeyboard()

                currentStep = Step.SOURCE
                binding.ageContainer.visibility = View.GONE
                binding.sourceContainer.visibility = View.VISIBLE
                updateContinueButton(enabled = selectedSource != null)

            } else if (currentStep == Step.SOURCE) {
                val birthYear = validatedBirthYear ?: return@setOnClickListener
                val source = selectedSource ?: return@setOnClickListener
                
                hideKeyboard()
                saveBirthYearAndProceed(birthYear, source)
            }
        }
    }

    private fun updateSourceSelection() {
        sourceCards.forEachIndexed { index, card ->
            if (sourceNames[index] == selectedSource) {
                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.dark_primary)
                // İsteğe bağlı olarak arkaplanı hafif tonlayabiliriz, şimdilik sadece dış çizgiyi belirginleştiriyoruz
            } else {
                card.strokeColor = android.graphics.Color.parseColor("#33FFFFFF")
            }
        }
        updateContinueButton(enabled = true)
    }

    private fun updateContinueButton(enabled: Boolean) {
        binding.btnContinue.isEnabled = enabled
        val tintColor = if (enabled) {
            ContextCompat.getColor(requireContext(), R.color.dark_primary)
        } else {
            ContextCompat.getColor(requireContext(), R.color.dark_text_secondary)
        }
        binding.btnContinue.backgroundTintList =
            android.content.res.ColorStateList.valueOf(tintColor)
    }

    private fun saveBirthYearAndProceed(birthYear: Int, source: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            AppStatisticsManager.incrementAcquisitionSource(source)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update(
                    mapOf(
                        "birthYear" to birthYear,
                        "acquisitionSource" to source
                    )
                )
                .addOnSuccessListener {
                    navigateToRegister(birthYear, source)
                }
                .addOnFailureListener {
                    navigateToRegister(birthYear, source)
                }
        } else {
            navigateToRegister(birthYear, source)
        }
    }

    private fun navigateToRegister(birthYear: Int, source: String) {
        val intent = Intent(requireContext(), RegisterActivity::class.java).apply {
            if (forceTeacher) putExtra(RegisterActivity.EXTRA_FORCE_TEACHER, true)
            if (forceStudent) putExtra(RegisterActivity.EXTRA_FORCE_STUDENT, true)
            putExtra(RegisterActivity.EXTRA_BIRTH_YEAR, birthYear)
            putExtra(RegisterActivity.EXTRA_ACQUISITION_SOURCE, source)

            prefillEmail?.let { putExtra("prefill_email", it) }
            if (googleSignIn) putExtra("google_sign_in", true)
            googleEmail?.let { putExtra("google_email", it) }
            googleName?.let { putExtra("google_name", it) }
        }
        requireActivity().startActivityForResult(
            intent,
            LoginStartActivity.RC_REGISTER
        )
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocusView = view?.findFocus() ?: binding.root
        imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
        binding.etAge.clearFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

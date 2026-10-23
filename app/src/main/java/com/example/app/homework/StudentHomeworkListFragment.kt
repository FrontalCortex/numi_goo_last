package com.example.app.homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.MainActivity
import com.example.app.R
import com.example.app.appString
import com.example.app.databinding.FragmentTeacherHomeworkListBinding

/**
 * Öğrencinin ödevleri (Sorularım → Sınıfım → Ödevlerim kartı). Düzen öğretmenin ödev listesiyle
 * ortak (fragment_teacher_homework_list); öğretmene ait parçalar gizli kalıyor.
 */
class StudentHomeworkListFragment : Fragment() {

    companion object {
        private const val ARG_CLASS_ID = "class_id"

        fun newInstance(classId: String) = StudentHomeworkListFragment().apply {
            arguments = Bundle().apply { putString(ARG_CLASS_ID, classId) }
        }
    }

    private var _binding: FragmentTeacherHomeworkListBinding? = null
    private val binding get() = _binding!!
    private var watcher: HomeworkCard.Watcher? = null

    private val adapter = HomeworkCard.Adapter { item ->
        // Tam ekran katmanda: çocuk sorunun ortasında alt bardan başka sekmeye geçmesin.
        (activity as? MainActivity)?.showAbacusOverlayFragment(HomeworkPlayerFragment.newInstance(item.homework.id))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTeacherHomeworkListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenTitle.text = appString(R.string.hw_card_title)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        // Ödev kartlarının kendi 8 dp yan boşluğu var (item_student_homework); toplam 16 dp olsun.
        val side = (8 * resources.displayMetrics.density).toInt()
        binding.list.setPadding(side, binding.list.paddingTop, side, binding.list.paddingBottom)
        binding.empty.text = appString(R.string.hw_student_empty)
        watcher = HomeworkCard.Watcher(requireArguments().getString(ARG_CLASS_ID).orEmpty()) onItems@{ items ->
            val b = _binding ?: return@onItems
            b.loading.visibility = View.GONE
            adapter.submitList(items)
            b.empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        watcher?.stop()
        watcher = null
        _binding = null
        super.onDestroyView()
    }
}

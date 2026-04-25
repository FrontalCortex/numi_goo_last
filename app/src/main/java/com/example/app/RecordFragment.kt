package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.app.databinding.FragmentRecordBinding
import com.example.app.model.LessonItem
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration

class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private var leaderboardListener: ListenerRegistration? = null
    private val listAdapter = RecordLeaderboardAdapter()

    private val partId: Int
        get() = requireArguments().getInt(ARG_PART_ID)

    private val lessonIndex: Int
        get() = requireArguments().getInt(ARG_LESSON_INDEX)

    private val lessonTitle: String
        get() = requireArguments().getString(ARG_LESSON_TITLE).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recordLessonTitleText.text = lessonTitle
        binding.recordRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recordRecyclerView.adapter = listAdapter

        binding.recordBackButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        leaderboardListener = LessonLeaderboardRepository.listenLeaderboard(
            partId = partId,
            lessonIndex = lessonIndex,
            onUpdate = { entries ->
                if (!isAdded) return@listenLeaderboard
                bindLeaderboard(entries)
            },
            onError = { e ->
                if (!isAdded) return@listenLeaderboard
                val message = e.localizedMessage ?: getString(R.string.record_leaderboard_load_error)
                binding.recordEmptyText.visibility = View.VISIBLE
                binding.recordEmptyText.text =
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        getString(R.string.record_leaderboard_load_error)
                    } else {
                        message
                    }
                binding.firstPlaceSection.visibility = View.GONE
                binding.recordRankSectionLabel.visibility = View.GONE
                binding.recordRecyclerView.visibility = View.GONE
                listAdapter.submitList(emptyList())
            },
        )
    }

    private fun syncCurrentUserLeaderboardFromLocalLesson() {
        val item = GlobalLessonData.getLessonItem(lessonIndex) ?: return
        if (item.type != LessonItem.TYPE_CHEST || !item.stepIsFinish) return
        val record = item.record ?: return
        if (record <= 0) return
        LessonLeaderboardRepository.submitBestIfNeeded(partId, lessonIndex, record)
    }

    private fun bindLeaderboard(entries: List<LessonLeaderboardRepository.LeaderboardEntry>) {
        if (entries.isEmpty()) {
            binding.firstPlaceSection.visibility = View.GONE
            binding.recordRankSectionLabel.visibility = View.GONE
            binding.recordRecyclerView.visibility = View.GONE
            binding.recordEmptyText.visibility = View.VISIBLE
            listAdapter.submitList(emptyList())
            return
        }

        binding.recordEmptyText.visibility = View.GONE

        val first = entries.first()
        binding.firstPlaceSection.visibility = View.VISIBLE
        binding.firstPlaceName.text = first.displayName.ifBlank { getString(R.string.record_leaderboard_title) }
        binding.firstPlaceTime.text = first.recordLabel
        val url = first.photoUrl
        if (!url.isNullOrBlank()) {
            Glide.with(binding.firstPlaceAvatar).load(url).fitCenter().into(binding.firstPlaceAvatar)
        } else {
            Glide.with(binding.firstPlaceAvatar).clear(binding.firstPlaceAvatar)
            binding.firstPlaceAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        val rest = entries.drop(1)
        if (rest.isEmpty()) {
            binding.recordRankSectionLabel.visibility = View.GONE
            binding.recordRecyclerView.visibility = View.GONE
        } else {
            binding.recordRankSectionLabel.visibility = View.VISIBLE
            binding.recordRecyclerView.visibility = View.VISIBLE
            listAdapter.submitList(rest)
        }
    }

    override fun onDestroyView() {
        leaderboardListener?.remove()
        leaderboardListener = null
        val hostAct = activity
        _binding = null
        super.onDestroyView()
        hostAct?.window?.decorView?.post {
            if (hostAct.isDestroyed) return@post
            val fm = (hostAct as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager ?: return@post
            if (fm.findFragmentById(R.id.abacusFragmentContainer) == null) {
                hostAct.findViewById<View>(R.id.abacusFragmentContainer)?.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val ARG_PART_ID = "partId"
        private const val ARG_LESSON_INDEX = "lessonIndex"
        private const val ARG_LESSON_TITLE = "lessonTitle"

        fun newInstance(partId: Int, lessonIndex: Int, lessonTitle: String): RecordFragment {
            val f = RecordFragment()
            f.arguments = Bundle().apply {
                putInt(ARG_PART_ID, partId)
                putInt(ARG_LESSON_INDEX, lessonIndex)
                putString(ARG_LESSON_TITLE, lessonTitle)
            }
            return f
        }
    }
}

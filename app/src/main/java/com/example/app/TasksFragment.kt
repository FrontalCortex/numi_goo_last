package com.example.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.app.databinding.FragmentTasksBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView


class TasksFragment : Fragment() {
    private lateinit var binding: FragmentTasksBinding

    private data class BulletinCard(
        val id: String,
        val title: String,
        val subtitle: String
    )

    private class BulletinAdapter(
        private val onClick: (BulletinCard) -> Unit
    ) : ListAdapter<BulletinCard, BulletinAdapter.VH>(
        object : DiffUtil.ItemCallback<BulletinCard>() {
            override fun areItemsTheSame(oldItem: BulletinCard, newItem: BulletinCard): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BulletinCard, newItem: BulletinCard): Boolean =
                oldItem == newItem
        }
    ) {
        class VH(itemView: View, private val onClick: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.bulletinCardTitle)
            val subtitle: TextView = itemView.findViewById(R.id.bulletinCardSubtitle)
            init {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onClick(pos)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bulletin_card, parent, false)
            return VH(v) { pos -> onClick(getItem(pos)) }
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.title.text = item.title
            holder.subtitle.text = item.subtitle
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTasksBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BulletinAdapter { _ ->
            // Şimdilik boş ekran
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,   // enter
                    R.anim.slide_out_left,   // exit
                    R.anim.slide_in_left,    // popEnter
                    R.anim.slide_out_right   // popExit
                )
                .replace(R.id.fragmentContainerID, AbacusPracticeFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tasksRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.tasksRecycler.adapter = adapter

        adapter.submitList(
            listOf(
                BulletinCard(
                    id = "daily_card",
                    title = "Günlük soru",
                    subtitle = "Bugünün sorusunu çöz ve serini koru."
                )
            )
        )
    }
}
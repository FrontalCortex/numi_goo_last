package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentFollowersFollowingBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FollowersFollowingFragment : Fragment() {

    companion object {
        const val ARG_START_TAB = "start_tab"
        const val TAB_FOLLOWING = 0  // "Takip Edilen"
        const val TAB_FOLLOWERS = 1  // "Takipçi"

        fun newInstance(startTab: Int) = FollowersFollowingFragment().apply {
            arguments = Bundle().also { it.putInt(ARG_START_TAB, startTab) }
        }
    }

    private var _binding: FragmentFollowersFollowingBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var authManager: AuthManager
    private lateinit var adapter: FollowUserAdapter

    private var myFirebaseUid = ""
    private var myUserId = ""
    private var myName = ""

    /** Set of firebaseUids that the current user is already following — used to determine "+" button */
    private val myFollowingUids = mutableSetOf<String>()

    private var currentTab = TAB_FOLLOWING

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowersFollowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager()
        authManager.initialize(requireContext())

        myFirebaseUid = auth.currentUser?.uid ?: ""
        myUserId = authManager.getCurrentUserId()
        myName = authManager.getCurrentUserName()

        currentTab = arguments?.getInt(ARG_START_TAB, TAB_FOLLOWING) ?: TAB_FOLLOWING

        setupRecyclerView()
        setupTabs()
        setupClickListeners()

        // First load the user's following set (needed for "+" button logic in followers tab)
        loadMyFollowingSet { loadTab(currentTab) }
    }

    private fun setupRecyclerView() {
        adapter = FollowUserAdapter(
            onFollowClick = { user -> followUser(user) },
            onItemClick = { user -> openUserProfile(user.firebaseUid) }
        )
        binding.rvFollowList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFollowList.adapter = adapter
    }

    private fun openUserProfile(firebaseUid: String) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragmentContainerID, ProfileFragment.newInstance(firebaseUid))
            .addToBackStack(null)
            .commit()
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("TAKİP EDİLEN"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("TAKİPÇİ"))

        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(currentTab))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                loadTab(currentTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // ----- Data loading -----

    /** Loads the set of firebaseUids the current user follows (for "+" button logic) */
    private fun loadMyFollowingSet(onDone: () -> Unit) {
        if (myFirebaseUid.isEmpty()) { onDone(); return }

        firestore.collection("users").document(myFirebaseUid)
            .collection("following")
            .get()
            .addOnSuccessListener { snap ->
                myFollowingUids.clear()
                snap.documents.forEach { myFollowingUids.add(it.id) }
                onDone()
            }
            .addOnFailureListener { onDone() }
    }

    private fun loadTab(tab: Int) {
        if (!isAdded || myFirebaseUid.isEmpty()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.rvFollowList.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        adapter.submitList(emptyList())

        val subCollection = if (tab == TAB_FOLLOWING) "following" else "followers"

        firestore.collection("users").document(myFirebaseUid)
            .collection(subCollection)
            .orderBy("followedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                if (!isAdded) return@addOnSuccessListener

                if (snap.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = if (tab == TAB_FOLLOWING)
                        "Henüz kimseyi takip etmiyorsun"
                    else
                        "Henüz takipçin yok"
                    return@addOnSuccessListener
                }

                // Fetch each user's main document in parallel to get selectedAvatar
                val userDocTasks = snap.documents.map { doc ->
                    firestore.collection("users").document(doc.id).get()
                }

                Tasks.whenAllSuccess<com.google.firebase.firestore.DocumentSnapshot>(userDocTasks)
                    .addOnSuccessListener { userDocs ->
                        if (!isAdded) return@addOnSuccessListener
                        binding.progressBar.visibility = View.GONE

                        // Build a map uid → selectedAvatar
                        val avatarMap = userDocs.associateBy(
                            keySelector = { it.id },
                            valueTransform = { it.getLong("selectedAvatar")?.toInt() ?: 0 }
                        )

                        val users = snap.documents.mapNotNull { doc ->
                            val name = doc.getString("name") ?: return@mapNotNull null
                            val userId = doc.getString("userId") ?: return@mapNotNull null
                            val uid = doc.id
                            val showFollowButton = (tab == TAB_FOLLOWERS) && !myFollowingUids.contains(uid)

                            FollowUser(
                                firebaseUid = uid,
                                name = name,
                                userId = userId,
                                selectedAvatar = avatarMap[uid] ?: 0,
                                showFollowButton = showFollowButton
                            )
                        }

                        if (users.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.tvEmpty.text = if (tab == TAB_FOLLOWING)
                                "Henüz kimseyi takip etmiyorsun"
                            else
                                "Henüz takipçin yok"
                        } else {
                            binding.rvFollowList.visibility = View.VISIBLE
                            adapter.submitList(users)
                        }
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Avatar yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    // ----- Follow action -----

    private fun followUser(user: FollowUser) {
        if (myFirebaseUid.isEmpty()) return

        val targetUid = user.firebaseUid
        val batch = firestore.batch()

        val myFollowerRef = firestore.collection("users").document(targetUid)
            .collection("followers").document(myFirebaseUid)
        val myFollowingRef = firestore.collection("users").document(myFirebaseUid)
            .collection("following").document(targetUid)
        val targetDocRef = firestore.collection("users").document(targetUid)
        val myDocRef = firestore.collection("users").document(myFirebaseUid)

        val followData = mapOf(
            "userId" to user.userId,
            "name" to user.name,
            "followedAt" to Timestamp.now()
        )
        val myData = mapOf(
            "userId" to myUserId,
            "name" to myName,
            "followedAt" to Timestamp.now()
        )

        batch.set(myFollowerRef, followData)
        batch.set(myFollowingRef, myData)
        batch.update(targetDocRef, "followersCount", FieldValue.increment(1))
        batch.update(myDocRef, "followingCount", FieldValue.increment(1))

        batch.commit()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                myFollowingUids.add(targetUid)
                Toast.makeText(requireContext(), "${user.name} takip edildi", Toast.LENGTH_SHORT).show()
                // Refresh list to hide the "+" button for this user
                loadTab(currentTab)
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

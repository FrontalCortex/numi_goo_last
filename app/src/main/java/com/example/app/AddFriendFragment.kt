package com.example.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.auth.AuthManager
import com.example.app.databinding.FragmentAddFriendBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddFriendFragment : Fragment() {

    private var _binding: FragmentAddFriendBinding? = null
    private val binding get() = _binding!!

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var authManager: AuthManager

    private var myFirebaseUid: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""

    private lateinit var adapter: FriendSearchAdapter
    private var searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDelayMs = 300L

    // Pagination state
    private var lastVisibleName: com.google.firebase.firestore.DocumentSnapshot? = null
    private var lastVisibleUserId: com.google.firebase.firestore.DocumentSnapshot? = null
    private var currentSearchQuery: String = ""
    private val accumulatedResults = mutableListOf<FriendSearchResult>()
    private var isSearching = false
    private var hasMoreResults = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager()
        authManager.initialize(requireContext())

        currentUserId = authManager.getCurrentUserId()
        currentUserName = authManager.getCurrentUserName()
        myFirebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        setupRecyclerView()
        setupSearchBar()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = FriendSearchAdapter(
            onAddFriendClick = { result -> followUser(result) },
            onItemClick = { result -> openUserProfile(result.firebaseUid) }
        )

        binding.rvFriendResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AddFriendFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).also {
                    // Transparent divider — spacing only via item padding
                }
            )
        }
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

    private fun setupSearchBar() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""

                // Show/hide clear button
                binding.btnClearSearch.visibility =
                    if (query.isNotEmpty()) View.VISIBLE else View.GONE

                // Cancel previous search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                if (query.length < 2) {
                    // Too short — clear results
                    adapter.submitList(emptyList())
                    binding.tvResultCount.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.GONE
                    binding.rvFriendResults.visibility = View.GONE
                    return
                }

                // Debounce: wait 300ms after the user stops typing
                searchRunnable = Runnable { searchUsers(query) }
                searchHandler.postDelayed(searchRunnable!!, searchDelayMs)
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            adapter.submitList(emptyList())
            binding.tvResultCount.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvFriendResults.visibility = View.GONE
            binding.btnLoadMore.visibility = View.GONE
        }

        binding.btnLoadMore.setOnClickListener {
            if (!isSearching && hasMoreResults && currentSearchQuery.isNotEmpty()) {
                performSearch(currentSearchQuery, isLoadMore = true)
            }
        }
    }

    private fun searchUsers(query: String) {
        if (query.length < 3) return
        currentSearchQuery = query
        accumulatedResults.clear()
        lastVisibleName = null
        lastVisibleUserId = null
        hasMoreResults = true
        performSearch(query, isLoadMore = false)
    }

    private fun performSearch(query: String, isLoadMore: Boolean) {
        if (!isAdded) return
        isSearching = true
        
        if (!isLoadMore) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvFriendResults.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.GONE
            binding.tvResultCount.visibility = View.GONE
            binding.btnLoadMore.visibility = View.GONE
        } else {
            binding.btnLoadMore.isEnabled = false
            binding.btnLoadMore.text = "YÜKLENİYOR..."
        }

        val queryEnd = query + "\uF8FF"
        val batchResults = mutableListOf<FriendSearchResult>()
        var completedQueries = 0
        val totalQueries = 2

        val limitSize = if (isLoadMore) 10L else 20L

        fun onBothComplete() {
            if (!isAdded) return
            isSearching = false
            binding.progressBar.visibility = View.GONE
            binding.btnLoadMore.isEnabled = true
            binding.btnLoadMore.text = "DAHA FAZLA YÜKLE"

            // Add new batch to accumulated
            accumulatedResults.addAll(batchResults)

            // Deduplicate by firebaseUid, preserve insertion order
            val seen = mutableSetOf<String>()
            val finalResults = accumulatedResults.filter { seen.add(it.firebaseUid) }

            if (finalResults.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvFriendResults.visibility = View.GONE
                binding.tvResultCount.visibility = View.GONE
                binding.btnLoadMore.visibility = View.GONE
            } else {
                binding.tvResultCount.text = "${finalResults.size} sonuç gösteriliyor"
                binding.tvResultCount.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvFriendResults.visibility = View.VISIBLE
                binding.btnLoadMore.visibility = if (hasMoreResults) View.VISIBLE else View.GONE
                adapter.submitList(finalResults.toList()) // copy to trigger diffutil
            }
        }

        fun parseSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot): List<FriendSearchResult> {
            return snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val userId = doc.getString("userId") ?: return@mapNotNull null
                if (userId == currentUserId) return@mapNotNull null
                val selectedAvatar = doc.getLong("selectedAvatar")?.toInt() ?: 0
                FriendSearchResult(
                    firebaseUid = doc.id,
                    name = name,
                    userId = userId,
                    selectedAvatar = selectedAvatar
                )
            }
        }

        // Query 1: search by name
        var nameQuery = firestore.collection("publicProfiles")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", queryEnd)
            .limit(limitSize)
            
        if (isLoadMore && lastVisibleName != null) {
            nameQuery = nameQuery.startAfter(lastVisibleName!!)
        }
        
        nameQuery.get()
            .addOnSuccessListener { snapshot ->
                // TEŞHİS: arama sonuç vermiyor sorunu için geçici log.
                android.util.Log.d("AddFriendDebug", "nameQuery('$query'..'$queryEnd') -> ${snapshot.size()} sonuç")
                if (isAdded) Toast.makeText(requireContext(), "İsim sorgusu: ${snapshot.size()} sonuç", Toast.LENGTH_SHORT).show()
                if (!snapshot.isEmpty) {
                    lastVisibleName = snapshot.documents[snapshot.size() - 1]
                    batchResults.addAll(parseSnapshot(snapshot))
                }
                // If we got exactly limitSize, there might be more.
                // If less, there are definitely no more for this specific query.
                // We'll just assume there's no more if both queries return empty.
                completedQueries++
                if (completedQueries == totalQueries) {
                    hasMoreResults = batchResults.isNotEmpty()
                    onBothComplete()
                }
            }
            .addOnFailureListener { e ->
                // TEŞHİS: gerçek hatayı gizlemeden ekranda göster.
                android.util.Log.e("AddFriendDebug", "nameQuery hata", e)
                if (isAdded) Toast.makeText(requireContext(), "İsim sorgusu HATA: ${e.message}", Toast.LENGTH_LONG).show()
                completedQueries++
                if (completedQueries == totalQueries) onBothComplete()
            }

        // Query 2: search by userId code
        var idQuery = firestore.collection("publicProfiles")
            .whereGreaterThanOrEqualTo("userId", query)
            .whereLessThanOrEqualTo("userId", queryEnd)
            .limit(limitSize)

        if (isLoadMore && lastVisibleUserId != null) {
            idQuery = idQuery.startAfter(lastVisibleUserId!!)
        }
        
        idQuery.get()
            .addOnSuccessListener { snapshot ->
                // TEŞHİS: arama sonuç vermiyor sorunu için geçici log.
                android.util.Log.d("AddFriendDebug", "idQuery('$query'..'$queryEnd') -> ${snapshot.size()} sonuç")
                if (isAdded) Toast.makeText(requireContext(), "userId sorgusu: ${snapshot.size()} sonuç", Toast.LENGTH_SHORT).show()
                if (!snapshot.isEmpty) {
                    lastVisibleUserId = snapshot.documents[snapshot.size() - 1]
                    batchResults.addAll(parseSnapshot(snapshot))
                }
                completedQueries++
                if (completedQueries == totalQueries) {
                    hasMoreResults = batchResults.isNotEmpty()
                    onBothComplete()
                }
            }
            .addOnFailureListener { e ->
                // TEŞHİS: gerçek hatayı gizlemeden ekranda göster.
                android.util.Log.e("AddFriendDebug", "idQuery hata", e)
                if (isAdded) Toast.makeText(requireContext(), "userId sorgusu HATA: ${e.message}", Toast.LENGTH_LONG).show()
                completedQueries++
                if (completedQueries == totalQueries) onBothComplete()
            }
    }

    private fun followUser(result: FriendSearchResult) {
        if (myFirebaseUid.isEmpty()) {
            Toast.makeText(requireContext(), "Oturum bilgisi alınamadı.", Toast.LENGTH_SHORT).show()
            return
        }

        val targetUid = result.firebaseUid

        // Check if already following
        firestore.collection("users").document(targetUid)
            .collection("followers").document(myFirebaseUid)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener

                if (doc.exists()) {
                    Toast.makeText(requireContext(), "Zaten takip ediyorsunuz.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Sadece takip kayıtlarını yaz. followersCount / followingCount artık
                // istemciden yazılmıyor; sunucudaki onFollowerCreated / onFollowingCreated
                // trigger'ları bu kayıtlara bakıp sayaçları güncelliyor. (Eskiden istemci
                // doğrudan yazabildiği için herkes başkasının takipçi sayısını
                // değiştirebiliyordu.)
                val batch = firestore.batch()

                val followerRef = firestore.collection("users").document(targetUid)
                    .collection("followers").document(myFirebaseUid)
                val followingRef = firestore.collection("users").document(myFirebaseUid)
                    .collection("following").document(targetUid)

                batch.set(followerRef, mapOf(
                    "userId" to currentUserId,
                    "name" to currentUserName,
                    "followedAt" to Timestamp.now()
                ))
                batch.set(followingRef, mapOf(
                    "userId" to result.userId,
                    "name" to result.name,
                    "followedAt" to Timestamp.now()
                ))

                batch.commit()
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        Toast.makeText(
                            requireContext(),
                            "${result.name} takip edildi.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        Toast.makeText(
                            requireContext(),
                            "Takip edilemedi: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        _binding = null
    }
}

package com.example.ilyphoto

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class SearchFragment : Fragment(R.layout.fragment_search) {

    data class SearchItem(
        val title: String,
        val subtitle: String,
        val category: String, // "Packages", "Bookings", "Gallery"
        val badge: String,
        val destinationActionId: Int,
    )

    private val allItems = listOf(
        SearchItem(
            title = "Matric Ball Shoot",
            subtitle = "R400 • 2 Hours coverage • 20 High-res edited photos",
            category = "Packages",
            badge = "R400",
            destinationActionId = R.id.action_global_to_bookingFragment
        ),
        SearchItem(
            title = "Wedding Photography",
            subtitle = "From R1200 • 6 Hours coverage • Full digital album",
            category = "Packages",
            badge = "From R1200",
            destinationActionId = R.id.action_global_to_bookingFragment
        ),
        SearchItem(
            title = "Events & Birthday Shoot",
            subtitle = "R400 • 2 Hours coverage • Studio or outdoor location",
            category = "Packages",
            badge = "R400",
            destinationActionId = R.id.action_global_to_bookingFragment
        ),
        SearchItem(
            title = "Studio Portrait Sessions",
            subtitle = "R350 • 1 Hour session • 10 Edited photos with lighting",
            category = "Packages",
            badge = "R350",
            destinationActionId = R.id.action_global_to_packagesFragment
        ),
        SearchItem(
            title = "My Upcoming Booking",
            subtitle = "04 September 2026 • Strandfontein • Invoice Pending",
            category = "Bookings",
            badge = "Active",
            destinationActionId = R.id.action_global_to_myBookingFragment
        ),
        SearchItem(
            title = "Make a New Booking",
            subtitle = "Interactive calendar date picker & conflict detection",
            category = "Bookings",
            badge = "Booking",
            destinationActionId = R.id.action_global_to_bookingFragment
        ),
        SearchItem(
            title = "Matric Ball 2026 Album",
            subtitle = "24 High-resolution edited client photos • Ready to download",
            category = "Gallery",
            badge = "Gallery",
            destinationActionId = R.id.action_global_to_galleryFragment
        ),
        SearchItem(
            title = "Strandfontein Wedding Album",
            subtitle = "120 High-resolution ceremony & reception photos",
            category = "Gallery",
            badge = "Gallery",
            destinationActionId = R.id.action_global_to_galleryFragment
        ),
        SearchItem(
            title = "Studio Portraits Collection",
            subtitle = "Creative studio lighting & professional portraiture",
            category = "Gallery",
            badge = "Gallery",
            destinationActionId = R.id.action_global_to_galleryFragment
        ),
        SearchItem(
            title = "My Profile & Contact",
            subtitle = "Update phone number, full name, and client details",
            category = "Bookings",
            badge = "Profile",
            destinationActionId = R.id.action_global_to_profileFragment
        )
    )

    private var selectedCategory: String = "All"
    private var currentQuery: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etSearchQuery = view.findViewById<EditText>(R.id.etSearchQuery)
        val ivClearSearch = view.findViewById<ImageView>(R.id.ivClearSearch)
        val resultsContainer = view.findViewById<LinearLayout>(R.id.searchResultsContainer)
        val tvResultsHeader = view.findViewById<TextView>(R.id.tvResultsHeader)
        val tvEmptySearch = view.findViewById<TextView>(R.id.tvEmptySearch)

        val pillAll = view.findViewById<TextView>(R.id.pillAll)
        val pillPackages = view.findViewById<TextView>(R.id.pillPackages)
        val pillBookings = view.findViewById<TextView>(R.id.pillBookings)
        val pillGallery = view.findViewById<TextView>(R.id.pillGallery)

        val navSearch = view.findViewById<FrameLayout>(R.id.navSearch)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        fun updateResults() {
            val query = currentQuery.trim().lowercase()
            val filtered = allItems.filter { item ->
                val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
                val matchesQuery = query.isEmpty() ||
                        item.title.lowercase().contains(query) ||
                        item.subtitle.lowercase().contains(query) ||
                        item.category.lowercase().contains(query)
                matchesCategory && matchesQuery
            }

            // Remove previous cards except the header
            val childCount = resultsContainer.childCount
            if (childCount > 1) {
                resultsContainer.removeViews(1, childCount - 1)
            }

            if (query.isEmpty()) {
                tvResultsHeader.text = if (selectedCategory == "All") "POPULAR SEARCHES" else "$selectedCategory ($filtered.size)".uppercase()
            } else {
                tvResultsHeader.text = "SEARCH RESULTS (${filtered.size})"
            }

            if (filtered.isEmpty()) {
                tvEmptySearch.visibility = View.VISIBLE
                tvEmptySearch.text = "No results found for \"$currentQuery\".\nTry searching 'Matric', 'Wedding', 'Gallery', or 'Booking'."
            } else {
                tvEmptySearch.visibility = View.GONE
                val inflater = LayoutInflater.from(requireContext())
                for (item in filtered) {
                    val card = createResultCard(inflater, resultsContainer, item)
                    resultsContainer.addView(card)
                }
            }
        }

        fun updatePillStyles() {
            val activeBg = R.drawable.login_button_background
            val inactiveBg = R.drawable.home_card_background

            pillAll.setBackgroundResource(if (selectedCategory == "All") activeBg else inactiveBg)
            pillAll.setTextColor(if (selectedCategory == "All") 0xFFFFFFFF.toInt() else 0xFF2A2233.toInt())

            pillPackages.setBackgroundResource(if (selectedCategory == "Packages") activeBg else inactiveBg)
            pillPackages.setTextColor(if (selectedCategory == "Packages") 0xFFFFFFFF.toInt() else 0xFF2A2233.toInt())

            pillBookings.setBackgroundResource(if (selectedCategory == "Bookings") activeBg else inactiveBg)
            pillBookings.setTextColor(if (selectedCategory == "Bookings") 0xFFFFFFFF.toInt() else 0xFF2A2233.toInt())

            pillGallery.setBackgroundResource(if (selectedCategory == "Gallery") activeBg else inactiveBg)
            pillGallery.setTextColor(if (selectedCategory == "Gallery") 0xFFFFFFFF.toInt() else 0xFF2A2233.toInt())
        }

        pillAll.setOnClickListener {
            selectedCategory = "All"
            updatePillStyles()
            updateResults()
        }

        pillPackages.setOnClickListener {
            selectedCategory = "Packages"
            updatePillStyles()
            updateResults()
        }

        pillBookings.setOnClickListener {
            selectedCategory = "Bookings"
            updatePillStyles()
            updateResults()
        }

        pillGallery.setOnClickListener {
            selectedCategory = "Gallery"
            updatePillStyles()
            updateResults()
        }

        etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString().orEmpty()
                ivClearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                updateResults()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        ivClearSearch.setOnClickListener {
            etSearchQuery.text?.clear()
            currentQuery = ""
            ivClearSearch.visibility = View.GONE
            updateResults()
        }

        // Bottom Nav Listeners
        navSearch.setOnClickListener {
            // Already here
        }

        navSettings.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_settingsFragment)
        }

        navMenu.setOnClickListener {
            NavigationMenuDialogFragment().show(parentFragmentManager, "NavMenu")
        }

        // Initial render
        updateResults()
    }

    private fun createResultCard(
        inflater: LayoutInflater,
        parent: LinearLayout,
        item: SearchItem
    ): View {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.home_card_background)
            elevation = 4f
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * resources.displayMetrics.density).toInt()
            }
            layoutParams = params
            isClickable = true
            isFocusable = true
        }

        // Top row: Title + Badge
        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvTitle = TextView(requireContext()).apply {
            text = item.title
            setTextColor(0xFF2A2233.toInt())
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvBadge = TextView(requireContext()).apply {
            text = item.badge
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.login_button_background)
            val padH = (12 * resources.displayMetrics.density).toInt()
            val padV = (4 * resources.displayMetrics.density).toInt()
            setPadding(padH, padV, padH, padV)
        }

        topRow.addView(tvTitle)
        topRow.addView(tvBadge)
        card.addView(topRow)

        // Subtitle row
        val tvSub = TextView(requireContext()).apply {
            text = item.subtitle
            setTextColor(0xFF6B5E78.toInt())
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * resources.displayMetrics.density).toInt()
            }
        }
        card.addView(tvSub)

        card.setOnClickListener {
            try {
                findNavController().navigate(item.destinationActionId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return card
    }
}

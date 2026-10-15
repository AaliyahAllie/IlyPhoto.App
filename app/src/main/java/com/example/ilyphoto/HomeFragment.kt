package com.example.ilyphoto

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.Booking
import com.example.ilyphoto.data.FirebaseProvider
import com.example.ilyphoto.data.ServiceLocator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnTopProfile = view.findViewById<View>(R.id.btnTopProfile)
        val ivTopAvatar = view.findViewById<ImageView>(R.id.ivTopProfileAvatar)

        // Load custom avatar if saved
        val prefs = requireContext().getSharedPreferences("ily_photo_prefs", Context.MODE_PRIVATE)
        val savedAvatarUri = prefs.getString("custom_avatar_uri", null)
        if (!savedAvatarUri.isNullOrBlank()) {
            runCatching { ivTopAvatar?.setImageURI(Uri.parse(savedAvatarUri)) }
        }

        btnTopProfile?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_profileFragment)
        }

        val homeTagline = view.findViewById<TextView>(R.id.homeTagline)
        val tvUpcomingEventLabel = view.findViewById<TextView>(R.id.tvUpcomingEventLabel)
        val tvBookingDate = view.findViewById<TextView>(R.id.tvBookingDate)
        val btnMore = view.findViewById<TextView>(R.id.btnMore)
        val btnViewBookings = view.findViewById<TextView>(R.id.btnViewBookings)

        val navHome = view.findViewById<FrameLayout>(R.id.navHome)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        // Personalized User Session Header
        viewLifecycleOwner.lifecycleScope.launch {
            ServiceLocator.auth.currentUser.collectLatest { profile ->
                if (profile != null && profile.displayName.isNotBlank()) {
                    homeTagline?.text = "WELCOME, ${profile.displayName.uppercase()}"
                } else {
                    homeTagline?.text = getString(R.string.capturing_every_moment)
                }
            }
        }

        // Live user-scoped booking observation
        val currentUid = FirebaseProvider.auth.currentUser?.uid
        viewLifecycleOwner.lifecycleScope.launch {
            ServiceLocator.bookings.bookings().collectLatest { allBookings ->
                val userBookings = if (!currentUid.isNullOrEmpty()) {
                    allBookings.filter { it.clientId == currentUid }
                } else {
                    allBookings
                }

                val upcoming: Booking? = userBookings.firstOrNull()
                if (upcoming != null) {
                    tvUpcomingEventLabel?.text = "UPCOMING EVENT (${upcoming.serviceName})"
                    tvBookingDate?.text = "BOOKING: ${upcoming.eventDate.uppercase()}"
                    btnMore?.visibility = View.VISIBLE
                } else {
                    tvUpcomingEventLabel?.text = "NO UPCOMING EVENTS"
                    tvBookingDate?.text = "BOOK YOUR FIRST PHOTOSHOOT"
                    btnMore?.visibility = View.VISIBLE
                }
            }
        }

        btnMore?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_myBookingFragment)
        }

        btnViewBookings?.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_myBookingFragment)
        }

        navHome?.setOnClickListener {
            // Already on Home screen
        }

        navSettings?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_settingsFragment)
        }

        navMenu?.setOnClickListener {
            NavigationMenuDialogFragment().show(parentFragmentManager, "NavMenu")
        }
    }
}

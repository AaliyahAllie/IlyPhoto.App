package com.example.ilyphoto

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class PackagesFragment : Fragment(R.layout.fragment_packages) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnTopProfile = view.findViewById<View>(R.id.btnTopProfile)
        val ivTopAvatar = view.findViewById<ImageView>(R.id.ivTopProfileAvatar)

        val prefs = requireContext().getSharedPreferences("ily_photo_prefs", Context.MODE_PRIVATE)
        val savedAvatarUri = prefs.getString("custom_avatar_uri", null)
        if (!savedAvatarUri.isNullOrBlank()) {
            runCatching { ivTopAvatar?.setImageURI(Uri.parse(savedAvatarUri)) }
        }

        btnTopProfile?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_profileFragment)
        }

        val btnBookMatric = view.findViewById<TextView>(R.id.btnBookMatricBalls)
        val btnBookWeddings = view.findViewById<TextView>(R.id.btnBookWeddings)
        val btnBookEvents = view.findViewById<TextView>(R.id.btnBookEvents)
        val btnViewBookings = view.findViewById<TextView>(R.id.btnPackagesViewBookings)

        val navHome = view.findViewById<FrameLayout>(R.id.navHome)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        btnBookMatric?.setOnClickListener {
            Toast.makeText(requireContext(), "Selected: Matric Balls (R400 / 2h)", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_packagesFragment_to_bookingFragment)
        }

        btnBookWeddings?.setOnClickListener {
            Toast.makeText(requireContext(), "Selected: Weddings (From R1200 / 6h)", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_packagesFragment_to_bookingFragment)
        }

        btnBookEvents?.setOnClickListener {
            Toast.makeText(requireContext(), "Selected: Events (R400 / 2h)", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_packagesFragment_to_bookingFragment)
        }

        btnViewBookings?.setOnClickListener {
            findNavController().navigate(R.id.action_packagesFragment_to_myBookingFragment)
        }

        navHome?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_homeFragment)
        }

        navSettings?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_settingsFragment)
        }

        navMenu?.setOnClickListener {
            NavigationMenuDialogFragment().show(parentFragmentManager, "NavMenu")
        }
    }
}

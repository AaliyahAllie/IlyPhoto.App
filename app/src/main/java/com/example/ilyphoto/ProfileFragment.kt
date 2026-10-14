package com.example.ilyphoto

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.ServiceLocator
import com.example.ilyphoto.data.UserProfile
import com.example.ilyphoto.data.UserRole
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var currentProfile: UserProfile? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etFirstName = view.findViewById<EditText>(R.id.etProfileFirstName)
        val etLastName = view.findViewById<EditText>(R.id.etProfileLastName)
        val etEmail = view.findViewById<EditText>(R.id.etProfileEmail)
        val etPhone = view.findViewById<EditText>(R.id.etProfilePhone)

        val btnLogOut = view.findViewById<TextView>(R.id.btnProfileLogOut)
        val btnEditAvatar = view.findViewById<ImageView>(R.id.btnEditAvatar)
        val navSearch = view.findViewById<FrameLayout>(R.id.navSearch)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        // Live observation of user profile from Firebase Firestore
        viewLifecycleOwner.lifecycleScope.launch {
            ServiceLocator.auth.currentUser.collectLatest { profile ->
                if (profile != null) {
                    currentProfile = profile
                    val names = profile.displayName.split(" ", limit = 2)
                    if (names.isNotEmpty() && names[0].isNotBlank()) {
                        etFirstName?.setText(names[0])
                    }
                    if (names.size > 1 && names[1].isNotBlank()) {
                        etLastName?.setText(names[1])
                    }
                    if (profile.email.isNotBlank()) {
                        etEmail?.setText(profile.email)
                    }
                    if (profile.phone.isNotBlank()) {
                        etPhone?.setText(profile.phone)
                    }
                }
            }
        }

        btnLogOut?.setOnClickListener {
            ServiceLocator.auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_to_loginFragment)
        }

        btnEditAvatar?.setOnClickListener {
            val prof = currentProfile
            if (prof != null) {
                val updatedName = "${etFirstName?.text?.toString()?.trim()} ${etLastName?.text?.toString()?.trim()}".trim()
                val updatedPhone = etPhone?.text?.toString()?.trim().orEmpty()
                viewLifecycleOwner.lifecycleScope.launch {
                    ServiceLocator.auth.updateProfileDetails(
                        current = prof,
                        displayName = updatedName,
                        username = prof.username,
                        phone = updatedPhone,
                        address = prof.address,
                        role = prof.role
                    )
                    Toast.makeText(requireContext(), "Profile updated in Firebase!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Change profile picture", Toast.LENGTH_SHORT).show()
            }
        }

        navSearch?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_searchFragment)
        }

        navSettings?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_settingsFragment)
        }

        navMenu?.setOnClickListener {
            NavigationMenuDialogFragment().show(parentFragmentManager, "NavMenu")
        }
    }
}

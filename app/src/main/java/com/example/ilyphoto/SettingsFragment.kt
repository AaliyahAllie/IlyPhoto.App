package com.example.ilyphoto

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.FirebaseProvider
import com.example.ilyphoto.data.ServiceLocator
import com.example.ilyphoto.data.toUserMessage
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val languages = arrayOf(
        "English (South Africa)",
        "Afrikaans",
        "isiXhosa",
        "isiZulu"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("ily_photo_prefs", Context.MODE_PRIVATE)

        val switchDarkMode = view.findViewById<SwitchCompat>(R.id.switchDarkMode)
        val cardLanguage = view.findViewById<View>(R.id.cardLanguage)
        val tvCurrentLanguage = view.findViewById<TextView>(R.id.tvCurrentLanguage)
        val switchNotifBooking = view.findViewById<SwitchCompat>(R.id.switchNotifBooking)
        val switchNotifGallery = view.findViewById<SwitchCompat>(R.id.switchNotifGallery)
        val tvAccountEmailValue = view.findViewById<TextView>(R.id.tvAccountEmailValue)
        val btnResetPassword = view.findViewById<TextView>(R.id.btnResetPassword)
        val btnStudioTerms = view.findViewById<TextView>(R.id.btnStudioTerms)
        val btnSettingsLogOut = view.findViewById<TextView>(R.id.btnSettingsLogOut)

        val navSearch = view.findViewById<FrameLayout>(R.id.navSearch)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        // 1. Dark Mode State & Listener
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        switchDarkMode?.isChecked = isDarkMode
        switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(requireContext(), "Dark Mode enabled", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(requireContext(), "Light Mode enabled", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Language Selection
        val savedLang = prefs.getString("selected_language", "English (South Africa)")
        tvCurrentLanguage?.text = savedLang
        cardLanguage?.setOnClickListener {
            val currentIndex = languages.indexOf(tvCurrentLanguage?.text.toString()).let { if (it >= 0) it else 0 }
            AlertDialog.Builder(requireContext())
                .setTitle("Select App Language")
                .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                    val chosen = languages[which]
                    tvCurrentLanguage?.text = chosen
                    prefs.edit().putString("selected_language", chosen).apply()
                    dialog.dismiss()

                    val greeting = when (which) {
                        1 -> "Taal verander na Afrikaans. Welkom by Ily Photo!"
                        2 -> "Ulwimi lutshintshiwe lwaba sisiXhosa. Wamkelekile!"
                        3 -> "Ulimi lushintshelwe kusiZulu. Siyakwamukela!"
                        else -> "Language set to English (South Africa)."
                    }
                    Toast.makeText(requireContext(), greeting, Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 3. Notifications
        switchNotifBooking?.isChecked = prefs.getBoolean("notif_booking", true)
        switchNotifBooking?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_booking", isChecked).apply()
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(requireContext(), "Booking reminders $status", Toast.LENGTH_SHORT).show()
        }

        switchNotifGallery?.isChecked = prefs.getBoolean("notif_gallery", true)
        switchNotifGallery?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_gallery", isChecked).apply()
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(requireContext(), "Gallery upload alerts $status", Toast.LENGTH_SHORT).show()
        }

        // 4. Account info & Password reset
        val currentUser = FirebaseProvider.auth.currentUser
        val userEmail = currentUser?.email ?: "client@ilyphoto.co.za"
        tvAccountEmailValue?.text = userEmail

        btnResetPassword?.setOnClickListener {
            if (currentUser?.email.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No email address found for session", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val result = ServiceLocator.auth.sendPasswordReset(userEmail)
                result.onSuccess {
                    Toast.makeText(
                        requireContext(),
                        "Password reset link sent to $userEmail. Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(requireContext(), err.toUserMessage(), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. Studio Terms Dialog
        btnStudioTerms?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Ily Photo Studio Terms & Policies")
                .setMessage(
                    "• Deposit: A 50% non-refundable deposit is required to secure your booking date.\n\n" +
                    "• Rescheduling: Allowed up to 48 hours prior to photoshoot time.\n\n" +
                    "• Studio Location: Strandfontein, Cape Town, Western Cape.\n\n" +
                    "• Turnaround Time: High-definition digital gallery ready within 7 business days.\n\n" +
                    "• Photography Rights: Photos remain subject to client personal usage and Ily Photo portfolio showcases."
                )
                .setPositiveButton("I Understand", null)
                .show()
        }

        // 6. Log Out
        btnSettingsLogOut?.setOnClickListener {
            ServiceLocator.auth.signOut()
            Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_global_to_loginFragment)
        }

        // 7. Bottom Navigation
        navSearch?.setOnClickListener {
            findNavController().navigate(R.id.action_global_to_searchFragment)
        }

        navSettings?.setOnClickListener {
            // Already on settings
        }

        navMenu?.setOnClickListener {
            NavigationMenuDialogFragment().show(parentFragmentManager, "NavMenu")
        }
    }
}

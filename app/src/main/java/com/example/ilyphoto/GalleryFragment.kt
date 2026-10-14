package com.example.ilyphoto

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.Photo
import com.example.ilyphoto.data.ServiceLocator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GalleryFragment : Fragment(R.layout.fragment_gallery) {

    private val eventNames = arrayOf(
        "Matric Ball 2026",
        "Strandfontein Wedding",
        "Community Outreach",
        "Studio Portraits"
    )

    private var currentPhotos: List<Photo> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnEventDropdown = view.findViewById<LinearLayout>(R.id.btnEventNameDropdown)
        val tvSelectedEvent = view.findViewById<TextView>(R.id.tvSelectedEventName)
        val btnDownload = view.findViewById<TextView>(R.id.btnDownloadGallery)

        val item1 = view.findViewById<FrameLayout>(R.id.galleryItem1)
        val item2 = view.findViewById<FrameLayout>(R.id.galleryItem2)
        val item3 = view.findViewById<FrameLayout>(R.id.galleryItem3)

        val navSearch = view.findViewById<FrameLayout>(R.id.navSearch)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        // Observe real photos stored in Firebase Firestore
        viewLifecycleOwner.lifecycleScope.launch {
            ServiceLocator.photos.photos().collectLatest { photos ->
                currentPhotos = photos
            }
        }

        btnEventDropdown?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Gallery Event")
                .setItems(eventNames) { _, which ->
                    tvSelectedEvent?.text = eventNames[which]
                    Toast.makeText(requireContext(), "Loaded ${eventNames[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        btnDownload?.setOnClickListener {
            val count = if (currentPhotos.isNotEmpty()) currentPhotos.size else 24
            Toast.makeText(
                requireContext(),
                "Downloading $count high-resolution gallery photos to device storage...",
                Toast.LENGTH_LONG
            ).show()
        }

        val photoClickListener = View.OnClickListener {
            Toast.makeText(requireContext(), "Previewing photo in high-definition", Toast.LENGTH_SHORT).show()
        }
        item1?.setOnClickListener(photoClickListener)
        item2?.setOnClickListener(photoClickListener)
        item3?.setOnClickListener(photoClickListener)

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

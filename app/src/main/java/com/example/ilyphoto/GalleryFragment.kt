package com.example.ilyphoto

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.Photo
import com.example.ilyphoto.data.ServiceLocator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GalleryFragment : Fragment(R.layout.fragment_gallery) {

    private data class GalleryEvent(
        val name: String,
        val heroTitle: String,
        val heroBgRes: Int,
        val item1Title: String,
        val item1Category: String,
        val item1BgRes: Int,
        val item2Title: String,
        val item2Category: String,
        val item2BgRes: Int,
        val item3Title: String,
        val item3Category: String,
        val item3BgRes: Int,
        val item4Title: String,
        val item4Category: String,
        val item4BgRes: Int,
        val photoCount: Int
    )

    private val galleryEvents = listOf(
        GalleryEvent(
            name = "Matric Ball 2026",
            heroTitle = "Matric Dance Golden Hour • Cape Town",
            heroBgRes = R.drawable.bg_photo_matric,
            item1Title = "Arrival & Red Carpet",
            item1Category = "Matric Ball",
            item1BgRes = R.drawable.bg_photo_matric,
            item2Title = "Formal Portraits",
            item2Category = "Matric Ball",
            item2BgRes = R.drawable.bg_photo_portrait,
            item3Title = "Sunset Coastline",
            item3Category = "Matric Ball",
            item3BgRes = R.drawable.bg_photo_wedding,
            item4Title = "Evening Ball Dress",
            item4Category = "Matric Ball",
            item4BgRes = R.drawable.bg_photo_matric,
            photoCount = 28
        ),
        GalleryEvent(
            name = "Strandfontein Beach Wedding",
            heroTitle = "Golden Hour Wedding Vows • Strandfontein Beach",
            heroBgRes = R.drawable.bg_photo_wedding,
            item1Title = "Bride & Groom Sunset",
            item1Category = "Weddings",
            item1BgRes = R.drawable.bg_photo_wedding,
            item2Title = "Bridal Party Portraits",
            item2Category = "Weddings",
            item2BgRes = R.drawable.bg_photo_portrait,
            item3Title = "Ceremony Exchange",
            item3Category = "Weddings",
            item3BgRes = R.drawable.bg_photo_event,
            item4Title = "Beach Walkway Rings",
            item4Category = "Weddings",
            item4BgRes = R.drawable.bg_photo_wedding,
            photoCount = 42
        ),
        GalleryEvent(
            name = "Studio Portraits Showcase",
            heroTitle = "Editorial Studio Lighting & Headshots",
            heroBgRes = R.drawable.bg_photo_portrait,
            item1Title = "Dramatic Studio Profile",
            item1Category = "Portraits",
            item1BgRes = R.drawable.bg_photo_portrait,
            item2Title = "Natural Light Session",
            item2Category = "Portraits",
            item2BgRes = R.drawable.bg_photo_matric,
            item3Title = "Black & White Classic",
            item3Category = "Portraits",
            item3BgRes = R.drawable.bg_photo_portrait,
            item4Title = "Fashion Editorial",
            item4Category = "Portraits",
            item4BgRes = R.drawable.bg_photo_wedding,
            photoCount = 18
        ),
        GalleryEvent(
            name = "Community & Birthday Events",
            heroTitle = "Milestone Birthday & Family Gathering",
            heroBgRes = R.drawable.bg_photo_event,
            item1Title = "Reception Celebration",
            item1Category = "Events",
            item1BgRes = R.drawable.bg_photo_event,
            item2Title = "Family Group Photo",
            item2Category = "Events",
            item2BgRes = R.drawable.bg_photo_portrait,
            item3Title = "Candle Lighting Ceremony",
            item3Category = "Events",
            item3BgRes = R.drawable.bg_photo_matric,
            item4Title = "Garden Festive Dance",
            item4Category = "Events",
            item4BgRes = R.drawable.bg_photo_event,
            photoCount = 35
        )
    )

    private var currentEventIndex = 0
    private var currentPhotos: List<Photo> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Top Right Profile Button
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

        val btnEventDropdown = view.findViewById<LinearLayout>(R.id.btnEventNameDropdown)
        val tvSelectedEvent = view.findViewById<TextView>(R.id.tvSelectedEventName)
        val btnDownload = view.findViewById<TextView>(R.id.btnDownloadGallery)

        // Photo Cards
        val cardFeatured = view.findViewById<CardView>(R.id.cardFeaturedPhoto)
        val featuredBg = view.findViewById<FrameLayout>(R.id.featuredPhotoBg)
        val tvFeaturedTitle = view.findViewById<TextView>(R.id.tvFeaturedTitle)
        val tvFeaturedCategory = view.findViewById<TextView>(R.id.tvFeaturedCategory)

        val cardItem1 = view.findViewById<CardView>(R.id.galleryItem1)
        val item1Bg = view.findViewById<FrameLayout>(R.id.photoItem1Bg)
        val tvItem1Title = view.findViewById<TextView>(R.id.tvItem1Title)
        val tvItem1Subtitle = view.findViewById<TextView>(R.id.tvItem1Subtitle)

        val cardItem2 = view.findViewById<CardView>(R.id.galleryItem2)
        val item2Bg = view.findViewById<FrameLayout>(R.id.photoItem2Bg)
        val tvItem2Title = view.findViewById<TextView>(R.id.tvItem2Title)
        val tvItem2Subtitle = view.findViewById<TextView>(R.id.tvItem2Subtitle)

        val cardItem3 = view.findViewById<CardView>(R.id.galleryItem3)
        val item3Bg = view.findViewById<FrameLayout>(R.id.photoItem3Bg)
        val tvItem3Title = view.findViewById<TextView>(R.id.tvItem3Title)
        val tvItem3Subtitle = view.findViewById<TextView>(R.id.tvItem3Subtitle)

        val cardItem4 = view.findViewById<CardView>(R.id.galleryItem4)
        val item4Bg = view.findViewById<FrameLayout>(R.id.photoItem4Bg)
        val tvItem4Title = view.findViewById<TextView>(R.id.tvItem4Title)
        val tvItem4Subtitle = view.findViewById<TextView>(R.id.tvItem4Subtitle)

        // Photo Preview Modal Overlay
        val previewOverlay = view.findViewById<FrameLayout>(R.id.photoPreviewOverlay)
        val tvPreviewTitle = view.findViewById<TextView>(R.id.tvPreviewTitle)
        val tvPreviewCategory = view.findViewById<TextView>(R.id.tvPreviewCategory)
        val photoPreviewBackground = view.findViewById<FrameLayout>(R.id.photoPreviewBackground)
        val btnPreviewDismiss = view.findViewById<ImageView>(R.id.btnPreviewDismiss)
        val btnClosePreview = view.findViewById<TextView>(R.id.btnClosePreviewDialog)
        val btnSaveSinglePhoto = view.findViewById<TextView>(R.id.btnDownloadSinglePhoto)

        // Navigation
        val navHome = view.findViewById<FrameLayout>(R.id.navHome)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        // Observe photos in Firebase Firestore
        viewLifecycleOwner.lifecycleScope.launch {
            ServiceLocator.photos.photos().collectLatest { photos ->
                currentPhotos = photos
            }
        }

        fun updateEventDisplay() {
            val event = galleryEvents[currentEventIndex]
            tvSelectedEvent?.text = event.name

            tvFeaturedTitle?.text = event.heroTitle
            tvFeaturedCategory?.text = "FEATURED • ${event.name.uppercase()}"
            featuredBg?.setBackgroundResource(event.heroBgRes)

            tvItem1Title?.text = event.item1Title
            tvItem1Subtitle?.text = event.item1Category
            item1Bg?.setBackgroundResource(event.item1BgRes)

            tvItem2Title?.text = event.item2Title
            tvItem2Subtitle?.text = event.item2Category
            item2Bg?.setBackgroundResource(event.item2BgRes)

            tvItem3Title?.text = event.item3Title
            tvItem3Subtitle?.text = event.item3Category
            item3Bg?.setBackgroundResource(event.item3BgRes)

            tvItem4Title?.text = event.item4Title
            tvItem4Subtitle?.text = event.item4Category
            item4Bg?.setBackgroundResource(event.item4BgRes)
        }

        fun showPreview(title: String, category: String, bgRes: Int) {
            tvPreviewTitle?.text = title
            tvPreviewCategory?.text = "$category • High Resolution (300 DPI)"
            photoPreviewBackground?.setBackgroundResource(bgRes)
            previewOverlay?.visibility = View.VISIBLE
        }

        updateEventDisplay()

        // Event Switcher Dropdown
        btnEventDropdown?.setOnClickListener {
            val eventTitles = galleryEvents.map { it.name }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Gallery Showcase")
                .setItems(eventTitles) { _, which ->
                    currentEventIndex = which
                    updateEventDisplay()
                    Toast.makeText(requireContext(), "Loaded ${galleryEvents[which].name}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Photo card click listeners to launch full-screen preview
        cardFeatured?.setOnClickListener {
            val event = galleryEvents[currentEventIndex]
            showPreview(event.heroTitle, event.name, event.heroBgRes)
        }

        cardItem1?.setOnClickListener {
            val event = galleryEvents[currentEventIndex]
            showPreview(event.item1Title, event.item1Category, event.item1BgRes)
        }

        cardItem2?.setOnClickListener {
            val event = galleryEvents[currentEventIndex]
            showPreview(event.item2Title, event.item2Category, event.item2BgRes)
        }

        cardItem3?.setOnClickListener {
            val event = galleryEvents[currentEventIndex]
            showPreview(event.item3Title, event.item3Category, event.item3BgRes)
        }

        cardItem4?.setOnClickListener {
            val event = galleryEvents[currentEventIndex]
            showPreview(event.item4Title, event.item4Category, event.item4BgRes)
        }

        // Preview dialog dismiss actions
        btnPreviewDismiss?.setOnClickListener {
            previewOverlay?.visibility = View.GONE
        }
        btnClosePreview?.setOnClickListener {
            previewOverlay?.visibility = View.GONE
        }

        btnSaveSinglePhoto?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Saved high-resolution photograph to device gallery!",
                Toast.LENGTH_SHORT
            ).show()
            previewOverlay?.visibility = View.GONE
        }

        // Download Full Gallery
        btnDownload?.setOnClickListener {
            val event = galleryEvents[currentEventIndex]
            Toast.makeText(
                requireContext(),
                "Downloading ${event.photoCount} HD gallery photos for '${event.name}' to storage...",
                Toast.LENGTH_LONG
            ).show()
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

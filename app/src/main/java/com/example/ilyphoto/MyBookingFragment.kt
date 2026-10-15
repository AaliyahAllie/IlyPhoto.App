package com.example.ilyphoto

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.Booking
import com.example.ilyphoto.data.FirebaseProvider
import com.example.ilyphoto.data.ServiceLocator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyBookingFragment : Fragment(R.layout.fragment_my_booking) {

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

        val bookingsContainer = view.findViewById<LinearLayout>(R.id.bookingsContainer)
        val cardEmptyBookings = view.findViewById<View>(R.id.cardEmptyBookings)
        val btnMakeNewBooking = view.findViewById<TextView>(R.id.btnMakeNewBooking)

        val navHome = view.findViewById<FrameLayout>(R.id.navHome)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        val inflater = LayoutInflater.from(requireContext())
        val currentUid = FirebaseProvider.auth.currentUser?.uid

        // Live observation of bookings saved in Firestore / local repository
        viewLifecycleOwner.lifecycleScope.launch {
            ServiceLocator.bookings.bookings().collectLatest { bookingList ->
                val userBookings = if (!currentUid.isNullOrEmpty()) {
                    val filtered = bookingList.filter { it.clientId == currentUid }
                    if (filtered.isNotEmpty()) filtered else bookingList
                } else {
                    bookingList
                }

                // Remove previously inflated dynamic booking cards, keeping the empty state view
                val viewsToRemove = mutableListOf<View>()
                for (i in 0 until bookingsContainer.childCount) {
                    val child = bookingsContainer.getChildAt(i)
                    if (child.id != R.id.cardEmptyBookings) {
                        viewsToRemove.add(child)
                    }
                }
                viewsToRemove.forEach { bookingsContainer.removeView(it) }

                if (userBookings.isEmpty()) {
                    cardEmptyBookings?.visibility = View.VISIBLE
                } else {
                    cardEmptyBookings?.visibility = View.GONE
                    for (booking in userBookings) {
                        val cardView = inflater.inflate(R.layout.item_booking_card, bookingsContainer, false)

                        val tvSubtitle = cardView.findViewById<TextView>(R.id.tvCardSubtitle)
                        val tvStatusBadge = cardView.findViewById<TextView>(R.id.tvCardStatusBadge)
                        val tvBookingDate = cardView.findViewById<TextView>(R.id.tvCardBookingDate)
                        val tvEventType = cardView.findViewById<TextView>(R.id.tvCardEventType)
                        val tvLocation = cardView.findViewById<TextView>(R.id.tvCardLocation)
                        val tvPrice = cardView.findViewById<TextView>(R.id.tvCardPrice)
                        val tvPaymentDetails = cardView.findViewById<TextView>(R.id.tvCardPaymentDetails)
                        val btnViewInvoice = cardView.findViewById<TextView>(R.id.btnCardViewInvoice)
                        val btnEdit = cardView.findViewById<TextView>(R.id.btnCardEdit)
                        val btnCancel = cardView.findViewById<TextView>(R.id.btnCardCancel)

                        val isCancelled = booking.status.equals("Cancelled", ignoreCase = true)
                        val isPaid = booking.invoiceStatus.equals("Paid", ignoreCase = true)
                        val isPartial = booking.invoiceStatus.contains("Partial", ignoreCase = true)

                        tvSubtitle?.text = if (booking.status.isNotEmpty()) booking.status.uppercase() else "UPCOMING EVENT"
                        tvBookingDate?.text = "BOOKING: ${booking.eventDate.uppercase()}"
                        tvEventType?.text = booking.serviceName
                        tvLocation?.text = booking.eventLocation.ifEmpty { "Strandfontein, Cape Town" }
                        tvPrice?.text = "R${booking.servicePrice}.00 (${booking.durationHours} Hours)"
                        tvPaymentDetails?.text = "${booking.invoiceStatus} (${booking.invoiceNumber})"

                        when {
                            isCancelled -> {
                                tvStatusBadge?.text = "CANCELLED"
                                tvStatusBadge?.setBackgroundResource(R.drawable.badge_cancelled)
                                tvStatusBadge?.setTextColor(Color.parseColor("#FFFFFF"))
                                btnCancel?.visibility = View.GONE
                                btnEdit?.visibility = View.GONE
                            }
                            isPaid -> {
                                tvStatusBadge?.text = "PAID"
                                tvStatusBadge?.setBackgroundResource(R.drawable.badge_paid)
                                tvStatusBadge?.setTextColor(Color.parseColor("#2E7D32"))
                                btnCancel?.visibility = View.GONE // Paid bookings cannot be cancelled via app
                                btnEdit?.visibility = View.VISIBLE
                            }
                            isPartial -> {
                                tvStatusBadge?.text = "PARTIAL"
                                tvStatusBadge?.setBackgroundResource(R.drawable.badge_partially_paid)
                                tvStatusBadge?.setTextColor(Color.parseColor("#E65100"))
                                btnCancel?.visibility = View.GONE // Deposit paid, contact studio
                                btnEdit?.visibility = View.VISIBLE
                            }
                            else -> {
                                tvStatusBadge?.text = "UNPAID"
                                tvStatusBadge?.setBackgroundResource(R.drawable.badge_unpaid)
                                tvStatusBadge?.setTextColor(Color.parseColor("#C62828"))
                                btnCancel?.visibility = View.VISIBLE
                                btnEdit?.visibility = View.VISIBLE
                            }
                        }

                        btnViewInvoice?.setOnClickListener {
                            findNavController().navigate(R.id.action_global_to_invoiceFragment)
                        }

                        btnEdit?.setOnClickListener {
                            Toast.makeText(
                                requireContext(),
                                "Editing session for ${booking.serviceName}",
                                Toast.LENGTH_SHORT
                            ).show()
                            val bundle = Bundle().apply {
                                putString("bookingId", booking.id)
                            }
                            findNavController().navigate(R.id.action_myBookingFragment_to_bookingFragment, bundle)
                        }

                        btnCancel?.setOnClickListener {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Cancel Booking")
                                .setMessage("Are you sure you want to cancel your ${booking.serviceName} booking on ${booking.eventDate}? This will release the date reservation.")
                                .setPositiveButton("Yes, Cancel") { _, _ ->
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        val result = ServiceLocator.bookings.cancelBooking(booking)
                                        result.onSuccess {
                                            Toast.makeText(
                                                requireContext(),
                                                "Booking for ${booking.eventDate} has been cancelled.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }.onFailure { err ->
                                            Toast.makeText(
                                                requireContext(),
                                                err.message ?: "Failed to cancel booking",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                .setNegativeButton("Keep Booking", null)
                                .show()
                        }

                        bookingsContainer.addView(cardView)
                    }
                }
            }
        }

        btnMakeNewBooking?.setOnClickListener {
            findNavController().navigate(R.id.action_myBookingFragment_to_bookingFragment)
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

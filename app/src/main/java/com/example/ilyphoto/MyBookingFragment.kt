package com.example.ilyphoto

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
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

        val tvBookingDate = view.findViewById<TextView>(R.id.tvBookingDate)
        val tvEventType = view.findViewById<TextView>(R.id.tvEventTypeValue)
        val tvLocation = view.findViewById<TextView>(R.id.tvLocationValue)
        val tvPrice = view.findViewById<TextView>(R.id.tvPriceValue)
        val tvPaymentDetails = view.findViewById<TextView>(R.id.tvPaymentDetailsValue)

        val btnEdit = view.findViewById<TextView>(R.id.btnEditBooking)
        val btnMakeNewBooking = view.findViewById<TextView>(R.id.btnMakeNewBooking)

        val navSearch = view.findViewById<FrameLayout>(R.id.navSearch)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        // Live observation of bookings saved in Firebase Firestore, fixed to current user session
        val currentUid = FirebaseProvider.auth.currentUser?.uid
        viewLifecycleOwner.lifecycleScope.launch {
            ServiceLocator.bookings.bookings().collectLatest { bookingList ->
                val userBookings = if (!currentUid.isNullOrEmpty()) {
                    bookingList.filter { it.clientId == currentUid }
                } else {
                    bookingList
                }

                val latest: Booking? = userBookings.firstOrNull()
                if (latest != null) {
                    tvBookingDate?.text = "BOOKING: ${latest.eventDate.uppercase()}"
                    tvEventType?.text = latest.serviceName
                    tvLocation?.text = latest.eventLocation.ifEmpty { "Strandfontein, Cape Town" }
                    tvPrice?.text = "R${latest.servicePrice}.00 (${latest.durationHours} Hours)"
                    tvPaymentDetails?.text = "${latest.invoiceStatus} (${latest.invoiceNumber})"
                } else {
                    tvBookingDate?.text = "NO BOOKINGS FOUND"
                    tvEventType?.text = "None Scheduled"
                    tvLocation?.text = "Strandfontein Studio"
                    tvPrice?.text = "R0.00"
                    tvPaymentDetails?.text = "No pending payments"
                }
            }
        }

        btnEdit?.setOnClickListener {
            Toast.makeText(requireContext(), "Reschedule or edit your booking", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_myBookingFragment_to_bookingFragment)
        }

        btnMakeNewBooking?.setOnClickListener {
            findNavController().navigate(R.id.action_myBookingFragment_to_bookingFragment)
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

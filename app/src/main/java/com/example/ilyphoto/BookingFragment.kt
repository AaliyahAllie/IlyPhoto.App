package com.example.ilyphoto

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.ServiceLocator
import com.example.ilyphoto.data.UserProfile
import com.example.ilyphoto.data.toUserMessage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BookingFragment : Fragment(R.layout.fragment_booking) {

    private var selectedDay: String = "04"
    private var selectedMonthYear: String = "Sept 2026"
    private var serviceName: String = "Matric Ball"
    private var servicePrice: Int = 400
    private var durationHours: Int = 2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvSelectedDate = view.findViewById<TextView>(R.id.tvCalendarSelectedDate)
        val conflictOverlay = view.findViewById<FrameLayout>(R.id.conflictOverlay)
        val btnConflictClose = view.findViewById<TextView>(R.id.btnConflictClose)
        val btnMakeBooking = view.findViewById<TextView>(R.id.btnMakeBooking)

        val navSearch = view.findViewById<FrameLayout>(R.id.navSearch)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        // Day 15 is booked clashing date in the design
        val day15 = view.findViewById<TextView>(R.id.day15)
        day15?.setOnClickListener {
            conflictOverlay?.visibility = View.VISIBLE
        }

        btnConflictClose?.setOnClickListener {
            conflictOverlay?.visibility = View.GONE
        }

        // List of selectable days
        val selectableDayIds = listOf(
            R.id.day01, R.id.day02, R.id.day03, R.id.day04, R.id.day05, R.id.day06,
            R.id.day07, R.id.day08, R.id.day09, R.id.day10, R.id.day11, R.id.day12, R.id.day13,
            R.id.day14, R.id.day16, R.id.day17, R.id.day18, R.id.day19, R.id.day20,
            R.id.day21, R.id.day22, R.id.day23, R.id.day24, R.id.day25, R.id.day26, R.id.day27,
            R.id.day28, R.id.day29, R.id.day30, R.id.day31
        )

        for (id in selectableDayIds) {
            val dayView = view.findViewById<TextView>(id)
            dayView?.setOnClickListener {
                val dayNum = dayView.text.toString()
                selectedDay = dayNum
                tvSelectedDate?.text = "Sept ${dayNum}th"
                Toast.makeText(requireContext(), "Selected Sept $dayNum, 2026", Toast.LENGTH_SHORT).show()
            }
        }

        btnMakeBooking?.setOnClickListener {
            if (selectedDay == "15") {
                conflictOverlay?.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val formattedDate = "$selectedDay September 2026"
            btnMakeBooking.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val userProfile: UserProfile? = ServiceLocator.auth.currentUser.firstOrNull()
                val result = ServiceLocator.bookings.createBooking(
                    clientProfile = userProfile,
                    clientPhone = userProfile?.phone.orEmpty().ifEmpty { "0685816456" },
                    serviceName = serviceName,
                    servicePrice = servicePrice,
                    durationHours = durationHours,
                    eventDate = formattedDate,
                    eventTime = "14:00",
                    eventLocation = "Strandfontein, Cape Town",
                    requirements = "Standard photography package"
                )
                btnMakeBooking.isEnabled = true

                result.onSuccess { booking ->
                    Toast.makeText(
                        requireContext(),
                        "Booking saved to Firebase! Invoice: ${booking.invoiceNumber}",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_bookingFragment_to_myBookingFragment)
                }.onFailure { err ->
                    val msg = err.toUserMessage()
                    if (msg.contains("OOPS", ignoreCase = true) || msg.contains("booked", ignoreCase = true)) {
                        conflictOverlay?.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
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

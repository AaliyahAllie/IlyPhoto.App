package com.example.ilyphoto

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.ilyphoto.data.ServiceLocator
import com.example.ilyphoto.data.UserProfile
import com.example.ilyphoto.data.toUserMessage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BookingFragment : Fragment(R.layout.fragment_booking) {

    private data class MonthInfo(
        val fullName: String,
        val shortName: String,
        val year: Int,
        val daysInMonth: Int,
        val bookedDay: Int? = null
    )

    private val availableMonths = listOf(
        MonthInfo("August", "Aug", 2026, 31, bookedDay = null),
        MonthInfo("September", "Sept", 2026, 30, bookedDay = 15),
        MonthInfo("October", "Oct", 2026, 31, bookedDay = 22),
        MonthInfo("November", "Nov", 2026, 30, bookedDay = 10),
        MonthInfo("December", "Dec", 2026, 31, bookedDay = 25),
        MonthInfo("January", "Jan", 2027, 31, bookedDay = null)
    )

    private var currentMonthIndex = 1 // September 2026 default
    private var selectedDay: String = "04"

    private data class PackageOption(
        val name: String,
        val basePrice: Int,
        val baseHours: Int,
        val extraHourlyRate: Int,
        val description: String
    )

    private val packageOptions = listOf(
        PackageOption("Matric Ball", 400, 2, 150, "R400 for 2 Hours (R150/extra hr)"),
        PackageOption("Weddings", 1200, 6, 200, "R1200 for 6 Hours (R200/extra hr)"),
        PackageOption("Events", 400, 2, 150, "R400 for 2 Hours (R150/extra hr)"),
        PackageOption("Portraits", 300, 1, 150, "R300 for 1 Hour (R150/extra hr)")
    )

    private var selectedPackage = packageOptions[0]
    private var durationHours: Int = 2
    private var servicePrice: Int = 400

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editingBookingId = arguments?.getString("bookingId")

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

        // Header and Calendar Month Views
        val tvCalendarYear = view.findViewById<TextView>(R.id.tvCalendarYear)
        val tvCalendarMonthYear = view.findViewById<TextView>(R.id.tvCalendarMonthYear)
        val tvSelectedDate = view.findViewById<TextView>(R.id.tvCalendarSelectedDate)
        val btnPrevMonth = view.findViewById<ImageView>(R.id.btnPrevMonth)
        val btnNextMonth = view.findViewById<ImageView>(R.id.btnNextMonth)

        // Customization Views
        val tvCustomizationHeading = view.findViewById<TextView>(R.id.tvCustomizationHeading)
        val tvSelectedPackage = view.findViewById<TextView>(R.id.tvSelectedPackage)
        val btnDecreaseHours = view.findViewById<TextView>(R.id.btnDecreaseHours)
        val btnIncreaseHours = view.findViewById<TextView>(R.id.btnIncreaseHours)
        val tvHoursValue = view.findViewById<TextView>(R.id.tvHoursValue)
        val tvCalculatedPrice = view.findViewById<TextView>(R.id.tvCalculatedPrice)
        val etBookingLocation = view.findViewById<EditText>(R.id.etBookingLocation)
        val etBookingPhone = view.findViewById<EditText>(R.id.etBookingPhone)
        val etBookingRequirements = view.findViewById<EditText>(R.id.etBookingRequirements)
        val btnMakeBooking = view.findViewById<TextView>(R.id.btnMakeBooking)

        // Conflict Dialog Overlay
        val conflictOverlay = view.findViewById<FrameLayout>(R.id.conflictOverlay)
        val btnConflictClose = view.findViewById<TextView>(R.id.btnConflictClose)

        // Navigation
        val navHome = view.findViewById<FrameLayout>(R.id.navHome)
        val navSettings = view.findViewById<FrameLayout>(R.id.navSettings)
        val navMenu = view.findViewById<FrameLayout>(R.id.navMenu)

        fun currentMonth(): MonthInfo = availableMonths[currentMonthIndex]

        fun updatePriceAndHours() {
            val extraHours = maxOf(0, durationHours - selectedPackage.baseHours)
            servicePrice = selectedPackage.basePrice + (extraHours * selectedPackage.extraHourlyRate)
            tvHoursValue?.text = "$durationHours Hours"
            tvCalculatedPrice?.text = "R$servicePrice.00"
        }

        fun updateCalendarHeaders() {
            val m = currentMonth()
            tvCalendarYear?.text = "${m.year}"
            tvCalendarMonthYear?.text = "${m.shortName} ${m.year}"
            tvSelectedDate?.text = "${m.shortName} ${selectedDay}th"

            // Update booked visual indicator on calendar
            val day15 = view.findViewById<TextView>(R.id.day15)
            if (m.bookedDay == 15) {
                day15?.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_calendar_booked))
            } else {
                day15?.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_dark))
            }
        }

        updateCalendarHeaders()
        updatePriceAndHours()

        // If editing an existing booking, load its data and update UI
        if (!editingBookingId.isNullOrEmpty()) {
            tvCustomizationHeading?.text = "RESCHEDULE & EDIT BOOKING"
            btnMakeBooking?.text = "UPDATE BOOKING"

            viewLifecycleOwner.lifecycleScope.launch {
                val existing = ServiceLocator.bookings.getBookingById(editingBookingId)
                if (existing != null) {
                    val matchedPkg = packageOptions.firstOrNull {
                        it.name.equals(existing.serviceName, ignoreCase = true)
                    } ?: packageOptions[0]
                    selectedPackage = matchedPkg
                    durationHours = existing.durationHours
                    servicePrice = existing.servicePrice

                    // Parse eventDate e.g. "04 September 2026"
                    val parts = existing.eventDate.split(" ")
                    if (parts.size >= 3) {
                        val dayPart = parts[0].padStart(2, '0')
                        val monthPart = parts[1]
                        val monthIdx = availableMonths.indexOfFirst {
                            it.fullName.equals(monthPart, ignoreCase = true) ||
                                it.shortName.equals(monthPart, ignoreCase = true)
                        }
                        if (monthIdx >= 0) {
                            currentMonthIndex = monthIdx
                        }
                        selectedDay = dayPart
                    }

                    etBookingLocation?.setText(existing.eventLocation)
                    etBookingPhone?.setText(existing.clientPhone)
                    etBookingRequirements?.setText(existing.requirements)
                    tvSelectedPackage?.text = "${selectedPackage.name} (${selectedPackage.description})"

                    updateCalendarHeaders()
                    updatePriceAndHours()
                }
            }
        }

        // Month switching
        btnPrevMonth?.setOnClickListener {
            if (currentMonthIndex > 0) {
                currentMonthIndex--
                updateCalendarHeaders()
            } else {
                Toast.makeText(requireContext(), "No earlier months available", Toast.LENGTH_SHORT).show()
            }
        }

        btnNextMonth?.setOnClickListener {
            if (currentMonthIndex < availableMonths.lastIndex) {
                currentMonthIndex++
                updateCalendarHeaders()
            } else {
                Toast.makeText(requireContext(), "No later months available", Toast.LENGTH_SHORT).show()
            }
        }

        // Package selection dialog
        tvSelectedPackage?.setOnClickListener {
            val packageNames = packageOptions.map { "${it.name} (${it.description})" }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Package / Event Type")
                .setItems(packageNames) { _, which ->
                    selectedPackage = packageOptions[which]
                    tvSelectedPackage.text = "${selectedPackage.name} (${selectedPackage.description})"
                    if (durationHours < selectedPackage.baseHours) {
                        durationHours = selectedPackage.baseHours
                    }
                    updatePriceAndHours()
                }
                .show()
        }

        // Hours stepper
        btnDecreaseHours?.setOnClickListener {
            if (durationHours > selectedPackage.baseHours) {
                durationHours--
                updatePriceAndHours()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Minimum duration for ${selectedPackage.name} is ${selectedPackage.baseHours} hours",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnIncreaseHours?.setOnClickListener {
            if (durationHours < 12) {
                durationHours++
                updatePriceAndHours()
            } else {
                Toast.makeText(requireContext(), "Maximum booking duration is 12 hours", Toast.LENGTH_SHORT).show()
            }
        }

        // Day click listeners
        val selectableDayIds = listOf(
            R.id.day01, R.id.day02, R.id.day03, R.id.day04, R.id.day05, R.id.day06,
            R.id.day07, R.id.day08, R.id.day09, R.id.day10, R.id.day11, R.id.day12, R.id.day13,
            R.id.day14, R.id.day15, R.id.day16, R.id.day17, R.id.day18, R.id.day19, R.id.day20,
            R.id.day21, R.id.day22, R.id.day23, R.id.day24, R.id.day25, R.id.day26, R.id.day27,
            R.id.day28, R.id.day29, R.id.day30, R.id.day31
        )

        for (id in selectableDayIds) {
            val dayView = view.findViewById<TextView>(id)
            dayView?.setOnClickListener {
                val dayNum = dayView.text.toString()
                val m = currentMonth()
                if (m.bookedDay != null && dayNum.toIntOrNull() == m.bookedDay) {
                    conflictOverlay?.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                selectedDay = dayNum
                tvSelectedDate?.text = "${m.shortName} ${dayNum}th"
                Toast.makeText(requireContext(), "Selected ${m.shortName} $dayNum, ${m.year}", Toast.LENGTH_SHORT).show()
            }
        }

        btnConflictClose?.setOnClickListener {
            conflictOverlay?.visibility = View.GONE
        }

        // Make Booking / Update Booking submission
        btnMakeBooking?.setOnClickListener {
            val m = currentMonth()
            if (m.bookedDay != null && selectedDay.toIntOrNull() == m.bookedDay) {
                conflictOverlay?.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val location = etBookingLocation?.text?.toString()?.trim().orEmpty().ifEmpty {
                "Strandfontein, Cape Town"
            }
            val requirements = etBookingRequirements?.text?.toString()?.trim().orEmpty().ifEmpty {
                "Standard photography session (${selectedPackage.name})"
            }
            val formattedDate = "$selectedDay ${m.fullName} ${m.year}"

            btnMakeBooking.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                val userProfile: UserProfile? = ServiceLocator.auth.currentUser.firstOrNull()
                val phone = etBookingPhone?.text?.toString()?.trim().orEmpty().ifEmpty {
                    userProfile?.phone.orEmpty().ifEmpty { "0685816456" }
                }

                if (!editingBookingId.isNullOrEmpty()) {
                    // Update existing booking in place without duplicating
                    val result = ServiceLocator.bookings.updateBooking(
                        bookingId = editingBookingId,
                        serviceName = selectedPackage.name,
                        servicePrice = servicePrice,
                        durationHours = durationHours,
                        eventDate = formattedDate,
                        eventTime = "14:00",
                        eventLocation = location,
                        requirements = requirements,
                        clientPhone = phone
                    )
                    btnMakeBooking.isEnabled = true

                    result.onSuccess { updated ->
                        Toast.makeText(
                            requireContext(),
                            "Booking updated successfully for $formattedDate!",
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
                } else {
                    // Create fresh new booking
                    val result = ServiceLocator.bookings.createBooking(
                        clientProfile = userProfile,
                        clientPhone = phone,
                        serviceName = selectedPackage.name,
                        servicePrice = servicePrice,
                        durationHours = durationHours,
                        eventDate = formattedDate,
                        eventTime = "14:00",
                        eventLocation = location,
                        requirements = requirements
                    )
                    btnMakeBooking.isEnabled = true

                    result.onSuccess { booking ->
                        Toast.makeText(
                            requireContext(),
                            "Booking confirmed for $formattedDate! Invoice: ${booking.invoiceNumber}",
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

package com.example.ilyphoto.data

import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.UUID

/**
 * Manages client bookings, photographer availability checking (FR08), booking status
 * updates (FR09, FR10), and invoice generation/status tracking (FR14-FR17).
 */
class BookingRepository {

    private val localBookings = MutableStateFlow<List<Booking>>(emptyList())

    fun bookings(): Flow<List<Booking>> = flow {
        if (!FirebaseProvider.isConfigured) {
            emitAll(localBookings)
            return@flow
        }
        emitAll(
            combine(bookingsFlow(), localBookings) { remote, local ->
                val remoteIds = remote.map { it.id }.toSet()
                val unsyncedLocal = local.filter { it.id !in remoteIds }
                (unsyncedLocal + remote).sortedByDescending { it.createdAt?.time ?: 0L }
            }
        )
    }

    private fun bookingsFlow(): Flow<List<Booking>> = callbackFlow {
        trySend(emptyList())
        val registration = FirebaseProvider.db.collection(BOOKINGS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                } else if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Booking::class.java) })
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Checks photographer availability for [eventDate] (FR08) and creates a booking +
     * invoice record (FR07, FR14).
     */
    suspend fun createBooking(
        clientProfile: UserProfile?,
        clientPhone: String,
        serviceName: String,
        servicePrice: Int,
        durationHours: Int,
        eventDate: String,
        eventTime: String,
        eventLocation: String,
        requirements: String,
    ): Result<Booking> = firebaseCall {
        val currentList = bookings().firstOrNull().orEmpty()
        val conflicting = currentList.any { existing ->
            existing.eventDate.equals(eventDate.trim(), ignoreCase = true) &&
                existing.status !in listOf("Cancelled", "Rejected")
        }
        if (conflicting) {
            error(
                "OOPS! Looks like there is already an event booked on $eventDate. " +
                    "Please select a different date."
            )
        }

        val user = FirebaseProvider.auth.currentUser
        val uid = clientProfile?.uid ?: user?.uid ?: "guest-client"
        val name = clientProfile?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore('@')
            ?: "Client"
        val email = clientProfile?.email?.takeIf { it.isNotBlank() }
            ?: user?.email.orEmpty()

        val docId = UUID.randomUUID().toString()
        val invoiceNo = "INV-" + docId.take(6).uppercase()
        val now = Date()

        val booking = Booking(
            id = docId,
            clientId = uid,
            clientName = name,
            clientEmail = email,
            clientPhone = clientPhone.trim(),
            serviceName = serviceName,
            servicePrice = servicePrice,
            durationHours = durationHours,
            eventDate = eventDate.trim(),
            eventTime = eventTime.trim(),
            eventLocation = eventLocation.trim(),
            requirements = requirements.trim(),
            status = "Pending",
            invoiceNumber = invoiceNo,
            invoiceStatus = "Unpaid",
            createdAt = now,
        )

        localBookings.value = listOf(booking) + localBookings.value

        val writeTask = FirebaseProvider.db.collection(BOOKINGS).document(docId).set(
            mapOf(
                "clientId" to booking.clientId,
                "clientName" to booking.clientName,
                "clientEmail" to booking.clientEmail,
                "clientPhone" to booking.clientPhone,
                "serviceName" to booking.serviceName,
                "servicePrice" to booking.servicePrice,
                "durationHours" to booking.durationHours,
                "eventDate" to booking.eventDate,
                "eventTime" to booking.eventTime,
                "eventLocation" to booking.eventLocation,
                "requirements" to booking.requirements,
                "status" to booking.status,
                "invoiceNumber" to booking.invoiceNumber,
                "invoiceStatus" to booking.invoiceStatus,
                "createdAt" to now,
            )
        )
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        booking
    }

    suspend fun getBookingById(bookingId: String): Booking? {
        val currentList = bookings().firstOrNull().orEmpty()
        return currentList.firstOrNull { it.id == bookingId }
            ?: localBookings.value.firstOrNull { it.id == bookingId }
    }

    /**
     * Updates an existing booking in-place (reschedule / edit). Does not create a duplicate booking.
     */
    suspend fun updateBooking(
        bookingId: String,
        serviceName: String,
        servicePrice: Int,
        durationHours: Int,
        eventDate: String,
        eventTime: String = "14:00",
        eventLocation: String,
        requirements: String,
        clientPhone: String? = null,
    ): Result<Booking> = firebaseCall {
        val currentList = bookings().firstOrNull().orEmpty()
        val conflicting = currentList.any { existing ->
            existing.id != bookingId &&
                existing.eventDate.equals(eventDate.trim(), ignoreCase = true) &&
                existing.status !in listOf("Cancelled", "Rejected")
        }
        if (conflicting) {
            error(
                "OOPS! Looks like there is already an event booked on $eventDate. " +
                    "Please select a different date."
            )
        }

        val existing = currentList.firstOrNull { it.id == bookingId }
            ?: localBookings.value.firstOrNull { it.id == bookingId }
            ?: error("Booking not found.")

        val updated = existing.copy(
            serviceName = serviceName.trim(),
            servicePrice = servicePrice,
            durationHours = durationHours,
            eventDate = eventDate.trim(),
            eventTime = eventTime.trim(),
            eventLocation = eventLocation.trim(),
            requirements = requirements.trim(),
            clientPhone = clientPhone?.trim()?.ifEmpty { existing.clientPhone } ?: existing.clientPhone,
            status = if (existing.eventDate != eventDate.trim()) "Rescheduled" else existing.status
        )

        localBookings.value = localBookings.value.map {
            if (it.id == bookingId) updated else it
        }.let { list ->
            if (list.any { it.id == bookingId }) list else listOf(updated) + list
        }

        val updates = mapOf<String, Any>(
            "serviceName" to updated.serviceName,
            "servicePrice" to updated.servicePrice,
            "durationHours" to updated.durationHours,
            "eventDate" to updated.eventDate,
            "eventTime" to updated.eventTime,
            "eventLocation" to updated.eventLocation,
            "requirements" to updated.requirements,
            "clientPhone" to updated.clientPhone,
            "status" to updated.status
        )

        val writeTask = FirebaseProvider.db.collection(BOOKINGS).document(bookingId).update(updates)
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        updated
    }

    /**
     * Cancels an existing booking if it has not been paid.
     */
    suspend fun cancelBooking(booking: Booking): Result<Unit> = firebaseCall {
        if (booking.invoiceStatus.equals("Paid", ignoreCase = true)) {
            error("Paid bookings cannot be cancelled via the app. Please contact ILY PHOTO directly.")
        }
        val updated = booking.copy(status = "Cancelled")
        localBookings.value = localBookings.value.map {
            if (it.id == booking.id) updated else it
        }

        val writeTask = FirebaseProvider.db.collection(BOOKINGS).document(booking.id)
            .update("status", "Cancelled")
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    suspend fun updateBookingStatus(
        booking: Booking,
        newStatus: String,
        newDate: String? = null,
        newTime: String? = null,
    ): Result<Unit> = firebaseCall {
        val updated = booking.copy(
            status = newStatus,
            eventDate = newDate ?: booking.eventDate,
            eventTime = newTime ?: booking.eventTime,
        )
        localBookings.value = localBookings.value.map {
            if (it.id == booking.id) updated else it
        }.let { list ->
            if (list.any { it.id == booking.id }) list else listOf(updated) + list
        }

        val updates = mutableMapOf<String, Any>(
            "status" to newStatus
        )
        if (newDate != null) updates["eventDate"] = newDate
        if (newTime != null) updates["eventTime"] = newTime

        val writeTask = FirebaseProvider.db.collection(BOOKINGS).document(booking.id).update(updates)
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    suspend fun updateInvoiceStatus(booking: Booking, newInvoiceStatus: String): Result<Unit> =
        firebaseCall {
            val updated = booking.copy(invoiceStatus = newInvoiceStatus)
            localBookings.value = localBookings.value.map {
                if (it.id == booking.id) updated else it
            }.let { list ->
                if (list.any { it.id == booking.id }) list else listOf(updated) + list
            }

            val writeTask = FirebaseProvider.db.collection(BOOKINGS).document(booking.id)
                .update("invoiceStatus", newInvoiceStatus)
            withTimeoutOrNull(1500L) {
                runCatching { writeTask.await() }
            }
            Unit
        }

    suspend fun updateInvoiceStatusById(bookingId: String, newInvoiceStatus: String): Result<Unit> =
        firebaseCall {
            localBookings.value = localBookings.value.map {
                if (it.id == bookingId) it.copy(invoiceStatus = newInvoiceStatus) else it
            }
            val writeTask = FirebaseProvider.db.collection(BOOKINGS).document(bookingId)
                .update("invoiceStatus", newInvoiceStatus)
            withTimeoutOrNull(1500L) {
                runCatching { writeTask.await() }
            }
            Unit
        }

    private companion object {
        const val BOOKINGS = "bookings"
    }
}

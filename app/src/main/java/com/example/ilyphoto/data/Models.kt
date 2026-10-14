package com.example.ilyphoto.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class UserRole { CLIENT, PHOTOGRAPHER }

data class UserProfile(
    val uid: String,
    val displayName: String,
    val username: String = "",
    val email: String,
    val phone: String = "",
    val address: String = "",
    val role: UserRole,
) {
    val isPhotographer: Boolean get() = role == UserRole.PHOTOGRAPHER
}

/** Auth/session state for the whole app. */
sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    data class SignedIn(val profile: UserProfile) : Session
}

/** Firestore document in `photos/{id}` (Portfolio entity). */
data class Photo(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val imageUrl: String = "",
    val storagePath: String = "",
    val photographerId: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)

/** Firestore document in `bookings/{id}` (Booking + Invoice entities from Task 1 ERD). */
data class Booking(
    @DocumentId val id: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientEmail: String = "",
    val clientPhone: String = "",
    val serviceName: String = "",
    val servicePrice: Int = 0,
    val durationHours: Int = 1,
    val eventDate: String = "",
    val eventTime: String = "",
    val eventLocation: String = "",
    val requirements: String = "",
    val status: String = "Pending", // Pending, Confirmed, Rescheduled, Completed, Cancelled
    val invoiceNumber: String = "",
    val invoiceStatus: String = "Unpaid", // Unpaid, Partially Paid, Paid
    @ServerTimestamp val createdAt: Date? = null,
)

/** Firestore document in `enquiries/{id}` (Enquiry entity from Task 1 ERD). */
data class Enquiry(
    @DocumentId val id: String = "",
    val clientId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val message: String = "",
    val status: String = "New", // New, In Progress, Closed
    @ServerTimestamp val createdAt: Date? = null,
)

/** Firestore document in `reviews/{uid}` (one review per client). */
data class Review(
    @DocumentId val id: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Date? = null,
)

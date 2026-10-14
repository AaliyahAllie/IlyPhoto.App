package com.example.ilyphoto.data

import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.UUID

/**
 * Manages client enquiries (FR19 & Enquiry entity in Task 1 ERD).
 */
class EnquiryRepository {

    private val localEnquiries = MutableStateFlow<List<Enquiry>>(emptyList())

    fun enquiries(): Flow<List<Enquiry>> = flow {
        if (!FirebaseProvider.isConfigured) {
            emitAll(localEnquiries)
            return@flow
        }
        emitAll(
            combine(enquiriesFlow(), localEnquiries) { remote, local ->
                val remoteIds = remote.map { it.id }.toSet()
                val unsyncedLocal = local.filter { it.id !in remoteIds }
                (unsyncedLocal + remote).sortedByDescending { it.createdAt?.time ?: 0L }
            }
        )
    }

    private fun enquiriesFlow(): Flow<List<Enquiry>> = callbackFlow {
        trySend(emptyList())
        val registration = FirebaseProvider.db.collection(ENQUIRIES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                } else if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Enquiry::class.java) })
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun submitEnquiry(
        fullName: String,
        email: String,
        phone: String,
        message: String,
    ): Result<Unit> = firebaseCall {
        val uid = FirebaseProvider.auth.currentUser?.uid.orEmpty()
        val docId = UUID.randomUUID().toString()
        val now = Date()
        val enquiry = Enquiry(
            id = docId,
            clientId = uid,
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            message = message.trim(),
            status = "New",
            createdAt = now,
        )

        localEnquiries.value = listOf(enquiry) + localEnquiries.value

        val writeTask = FirebaseProvider.db.collection(ENQUIRIES).document(docId).set(
            mapOf(
                "clientId" to enquiry.clientId,
                "fullName" to enquiry.fullName,
                "email" to enquiry.email,
                "phone" to enquiry.phone,
                "message" to enquiry.message,
                "status" to enquiry.status,
                "createdAt" to now,
            )
        )
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    private companion object {
        const val ENQUIRIES = "enquiries"
    }
}

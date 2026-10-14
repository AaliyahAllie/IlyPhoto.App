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

/** Client reviews shown in the Reviews dialog. One review per signed-in client. */
class ReviewRepository {

    private val localReviews = MutableStateFlow<List<Review>>(emptyList())

    fun reviews(): Flow<List<Review>> = flow {
        if (!FirebaseProvider.isConfigured) {
            emitAll(localReviews)
            return@flow
        }
        emitAll(
            combine(reviewsFlow(), localReviews) { remote, local ->
                val remoteIds = remote.map { it.id }.toSet()
                val unsyncedLocal = local.filter { it.id !in remoteIds }
                (unsyncedLocal + remote).sortedByDescending { it.createdAt?.time ?: 0L }
            }
        )
    }

    private fun reviewsFlow(): Flow<List<Review>> = callbackFlow {
        trySend(emptyList())
        val registration = FirebaseProvider.db.collection(REVIEWS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                } else if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Review::class.java) })
                }
            }
        awaitClose { registration.remove() }
    }

    /** Creates or replaces the current user's review. */
    suspend fun submit(text: String): Result<Unit> = firebaseCall {
        val user = FirebaseProvider.auth.currentUser ?: error("Please sign in to leave a review.")
        val now = Date()
        val review = Review(
            id = user.uid,
            authorName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore('@')
                ?: "Client",
            text = text.trim(),
            createdAt = now,
        )
        localReviews.value = listOf(review) + localReviews.value.filterNot { it.id == user.uid }

        val writeTask = FirebaseProvider.db.collection(REVIEWS).document(user.uid).set(
            mapOf(
                "authorName" to review.authorName,
                "text" to review.text,
                "createdAt" to now,
            )
        )
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    private companion object {
        const val REVIEWS = "reviews"
    }
}

package com.example.ilyphoto.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID
import kotlin.math.max

/**
 * Photos repository: stores metadata and images in Cloud Firestore (`photos` collection),
 * with an instant local cache so newly uploaded photos appear immediately even while
 * Firestore is offline or before the Firestore database is created in the Firebase console.
 */
class PhotoRepository {

    private val localPhotos = MutableStateFlow<List<Photo>>(emptyList())

    /** Live list of all photos, newest first. */
    fun photos(): Flow<List<Photo>> = flow {
        if (!FirebaseProvider.isConfigured) {
            emitAll(localPhotos)
            return@flow
        }
        emitAll(
            combine(photosFlow(), localPhotos) { remote, local ->
                val remoteIds = remote.map { it.id }.toSet()
                val unsyncedLocal = local.filter { it.id !in remoteIds }
                (unsyncedLocal + remote).sortedByDescending { it.createdAt?.time ?: 0L }
            }
        )
    }

    private fun photosFlow(): Flow<List<Photo>> = callbackFlow {
        trySend(emptyList())
        val registration = FirebaseProvider.db.collection(PHOTOS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                } else if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Photo::class.java) })
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Uploads a photo from a device [imageUri] or direct [imageUrlInput], then writes its
     * metadata to Firestore (`photos` collection).
     */
    suspend fun upload(
        context: Context? = null,
        imageUri: Uri? = null,
        imageUrlInput: String = "",
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit> = firebaseCall {
        val uid = FirebaseProvider.auth.currentUser?.uid ?: "local-user"

        var finalImageUrl = imageUrlInput.trim()

        if (imageUri != null && context != null) {
            finalImageUrl = encodeUriToDataUrl(context, imageUri)
                ?: error("Could not read the selected image.")
            onProgress(1f)
        }

        if (finalImageUrl.isEmpty()) {
            error("Please choose an image or provide an image URL.")
        }

        val docId = UUID.randomUUID().toString()
        val now = Date()
        val newPhoto = Photo(
            id = docId,
            title = title.trim(),
            description = description.trim(),
            category = category.trim().ifEmpty { "Other" },
            tags = tags,
            imageUrl = finalImageUrl,
            storagePath = "",
            photographerId = uid,
            createdAt = now,
        )

        // Update local state immediately so the UI refreshes right away
        localPhotos.value = listOf(newPhoto) + localPhotos.value

        val writeTask = FirebaseProvider.db.collection(PHOTOS).document(docId).set(
            mapOf(
                "title" to newPhoto.title,
                "description" to newPhoto.description,
                "category" to newPhoto.category,
                "tags" to newPhoto.tags,
                "imageUrl" to newPhoto.imageUrl,
                "storagePath" to newPhoto.storagePath,
                "photographerId" to newPhoto.photographerId,
                "createdAt" to now,
            )
        )
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    private suspend fun encodeUriToDataUrl(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching null
                val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@runCatching null

                val maxDim = 720
                val largest = max(original.width, original.height)
                val scaled = if (largest > maxDim) {
                    val ratio = maxDim.toFloat() / largest.toFloat()
                    val w = (original.width * ratio).toInt().coerceAtLeast(1)
                    val h = (original.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(original, w, h, true)
                } else {
                    original
                }

                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 72, out)
                val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            }.getOrNull()
        }

    suspend fun delete(photo: Photo): Result<Unit> = firebaseCall {
        localPhotos.value = localPhotos.value.filterNot { it.id == photo.id }
        val deleteTask = FirebaseProvider.db.collection(PHOTOS).document(photo.id).delete()
        withTimeoutOrNull(1500L) {
            runCatching { deleteTask.await() }
        }
        Unit
    }

    private companion object {
        const val PHOTOS = "photos"
    }
}

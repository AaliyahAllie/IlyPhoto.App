package com.example.ilyphoto.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException

/**
 * Single access point for Firebase.
 *
 * Firebase is configured by dropping `google-services.json` into `app/`. Until then
 * [isConfigured] is false and every repository degrades gracefully (empty data and a
 * friendly error) instead of crashing.
 */
object FirebaseProvider {
    var isConfigured: Boolean = false
        private set

    fun init(context: Context) {
        // FirebaseInitProvider runs before Application.onCreate and auto-initialises the
        // default app when google-services.json was processed at build time.
        isConfigured = FirebaseApp.getApps(context).isNotEmpty()
    }

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
}

class FirebaseNotConfiguredException : IllegalStateException(
    "Firebase isn't configured yet. Add google-services.json to the app/ folder and rebuild."
)

/** Runs [block] and wraps the outcome in a [Result], failing fast when Firebase isn't set up. */
internal suspend fun <T> firebaseCall(block: suspend () -> T): Result<T> {
    if (!FirebaseProvider.isConfigured) return Result.failure(FirebaseNotConfiguredException())
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

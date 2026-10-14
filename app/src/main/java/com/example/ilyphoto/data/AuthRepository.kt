package com.example.ilyphoto.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Authentication + user profile stored in Firebase Auth and Firestore `users/{uid}`.
 */
class AuthRepository {

    private val localProfileOverride = MutableStateFlow<UserProfile?>(null)

    /** Emits `null` when signed out, otherwise the live profile (including role, phone, address). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserProfile?> = flow {
        if (!FirebaseProvider.isConfigured) {
            emit(null)
            return@flow
        }
        emitAll(
            authStateFlow().flatMapLatest<FirebaseUser?, UserProfile?> { user ->
                if (user == null) {
                    flowOf(null)
                } else {
                    combine(profileFlow(user), localProfileOverride) { profile, overrideProfile ->
                        if (overrideProfile != null && overrideProfile.uid == profile.uid) {
                            overrideProfile
                        } else {
                            profile
                        }
                    }
                }
            }
        )
    }

    private fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val auth = FirebaseProvider.auth
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun profileFlow(user: FirebaseUser): Flow<UserProfile> = callbackFlow {
        val fallbackName = user.displayName ?: user.email.orEmpty().substringBefore('@')
        val fallbackUsername = user.email.orEmpty().substringBefore('@')
        val currentOverride = localProfileOverride.value

        trySend(
            currentOverride?.takeIf { it.uid == user.uid } ?: UserProfile(
                uid = user.uid,
                displayName = fallbackName,
                username = fallbackUsername,
                email = user.email.orEmpty(),
                role = UserRole.CLIENT,
            )
        )

        val registration = FirebaseProvider.db.collection(USERS).document(user.uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val role = if (snapshot.getString("role") == "photographer") {
                        UserRole.PHOTOGRAPHER
                    } else {
                        UserRole.CLIENT
                    }
                    val name = snapshot.getString("displayName") ?: fallbackName
                    val username = snapshot.getString("username") ?: fallbackUsername
                    val phone = snapshot.getString("phone").orEmpty()
                    val address = snapshot.getString("address").orEmpty()
                    trySend(
                        UserProfile(
                            uid = user.uid,
                            displayName = name,
                            username = username,
                            email = user.email.orEmpty(),
                            phone = phone,
                            address = address,
                            role = role,
                        )
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = firebaseCall {
        FirebaseProvider.auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun register(
        displayName: String,
        email: String,
        password: String,
        username: String = "",
        phone: String = "",
        isPhotographer: Boolean = false,
    ): Result<Unit> = firebaseCall {
        val cleanEmail = email.trim()
        val cleanName = displayName.trim()
        val cleanUsername = username.trim().ifEmpty { cleanEmail.substringBefore('@') }
        val cleanPhone = phone.trim()
        val chosenRole = if (isPhotographer) UserRole.PHOTOGRAPHER else UserRole.CLIENT
        val roleString = if (isPhotographer) "photographer" else "client"

        val user = FirebaseProvider.auth
            .createUserWithEmailAndPassword(cleanEmail, password).await().user
            ?: error("Account was created but no user was returned.")

        localProfileOverride.value = UserProfile(
            uid = user.uid,
            displayName = cleanName,
            username = cleanUsername,
            email = cleanEmail,
            phone = cleanPhone,
            role = chosenRole,
        )

        withTimeoutOrNull(3000L) {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()
            ).await()
        }

        val writeTask = FirebaseProvider.db.collection(USERS).document(user.uid).set(
            mapOf(
                "displayName" to cleanName,
                "username" to cleanUsername,
                "email" to cleanEmail,
                "phone" to cleanPhone,
                "role" to roleString,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        )
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    suspend fun updateProfileDetails(
        current: UserProfile,
        displayName: String,
        username: String,
        phone: String,
        address: String,
        role: UserRole,
    ): Result<Unit> = firebaseCall {
        val updated = current.copy(
            displayName = displayName.trim().ifEmpty { current.displayName },
            username = username.trim().ifEmpty { current.username },
            phone = phone.trim(),
            address = address.trim(),
            role = role,
        )
        localProfileOverride.value = updated

        val roleString = if (role == UserRole.PHOTOGRAPHER) "photographer" else "client"
        val writeTask = FirebaseProvider.db.collection(USERS).document(current.uid).set(
            mapOf(
                "displayName" to updated.displayName,
                "username" to updated.username,
                "email" to updated.email,
                "phone" to updated.phone,
                "address" to updated.address,
                "role" to roleString,
            ),
            SetOptions.merge()
        )
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    suspend fun updateRole(role: UserRole): Result<Unit> = firebaseCall {
        val user = FirebaseProvider.auth.currentUser ?: error("Please sign in first.")
        val existing = localProfileOverride.value
        localProfileOverride.value = existing?.copy(role = role) ?: UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: user.email.orEmpty().substringBefore('@'),
            username = user.email.orEmpty().substringBefore('@'),
            email = user.email.orEmpty(),
            role = role,
        )
        val roleString = if (role == UserRole.PHOTOGRAPHER) "photographer" else "client"
        val writeTask = FirebaseProvider.db.collection(USERS).document(user.uid).set(
            mapOf(
                "email" to user.email.orEmpty(),
                "displayName" to (user.displayName ?: user.email.orEmpty().substringBefore('@')),
                "role" to roleString,
            ),
            SetOptions.merge()
        )
        withTimeoutOrNull(1500L) {
            runCatching { writeTask.await() }
        }
        Unit
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = firebaseCall {
        FirebaseProvider.auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    fun signOut() {
        localProfileOverride.value = null
        if (FirebaseProvider.isConfigured) FirebaseProvider.auth.signOut()
    }

    private companion object {
        const val USERS = "users"
    }
}

package com.example.ilyphoto.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException

/** Turns Firebase exceptions into messages that are safe to show to a user. */
fun Throwable.toUserMessage(): String = when (this) {
    is FirebaseNotConfiguredException -> message.orEmpty()
    is FirebaseAuthWeakPasswordException -> "Choose a stronger password (at least 6 characters)."
    is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
    is FirebaseAuthInvalidUserException -> "No account found for that email."
    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
    is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
    is FirebaseNetworkException -> "No internet connection. Check your network and try again."
    is FirebaseFirestoreException ->
        if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            "You don't have permission to do that."
        } else {
            "Couldn't reach the server. Please try again."
        }
    is StorageException ->
        if (errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
            "You don't have permission to do that."
        } else {
            "Upload failed. Please try again."
        }
    else -> message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}

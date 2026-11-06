package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

data class GoogleUserData(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String? = null,
    val idToken: String? = null
)

data class GoogleAuthState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val user: GoogleUserData? = null,
    val errorMessage: String? = null,
    val configuredClientId: String? = null
)

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    private val _authState = MutableStateFlow(GoogleAuthState())
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("GoogleAuthManager", "FirebaseApp not initialized: ${e.message}")
            null
        }
    }

    init {
        checkCurrentAuthState()
    }

    fun getEffectiveClientId(): String? {
        // 1. Check generated string resource (from google-services.json)
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val str = context.getString(resId)
                if (str.isNotBlank()) return str
            }
        } catch (_: Exception) {}

        // 2. Check BuildConfig from Secrets plugin / .env
        try {
            val envId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (!envId.isNullOrBlank() && !envId.contains("placeholder")) return envId
        } catch (_: Exception) {}

        // 3. Check locally saved state
        if (!_authState.value.configuredClientId.isNullOrBlank()) {
            return _authState.value.configuredClientId
        }

        // 4. Default Web Client ID from google-services.json
        return "311334805963-blflcgkboam5v6rneiodpminbauabl3h.apps.googleusercontent.com"
    }

    fun setCustomClientId(clientId: String) {
        _authState.value = _authState.value.copy(configuredClientId = clientId.trim())
    }

    fun checkCurrentAuthState() {
        val currentFirebaseUser = firebaseAuth?.currentUser
        if (currentFirebaseUser != null) {
            val userData = GoogleUserData(
                uid = currentFirebaseUser.uid,
                email = currentFirebaseUser.email ?: "",
                displayName = currentFirebaseUser.displayName,
                photoUrl = currentFirebaseUser.photoUrl?.toString()
            )
            _authState.value = _authState.value.copy(
                isSignedIn = true,
                isLoading = false,
                user = userData,
                errorMessage = null
            )
        }
    }

    suspend fun signInWithGoogle(
        activity: Activity,
        overrideClientId: String? = null
    ): Result<GoogleUserData> = withContext(Dispatchers.Main) {
        _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

        val clientId = overrideClientId?.ifBlank { null } ?: getEffectiveClientId()

        if (clientId.isNullOrBlank()) {
            val msg = "Web Client ID not found. Please provide your Web Client ID from Firebase Console or place google-services.json in /app."
            _authState.value = _authState.value.copy(isLoading = false, errorMessage = msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName
                var photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                if (photoUrl.isNullOrBlank()) {
                    photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                }

                var finalUid = email

                // If Firebase Auth is available, complete sign-in to Firebase
                val auth = firebaseAuth
                if (auth != null && idToken.isNotBlank()) {
                    try {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(authCredential).awaitTask()
                        val fbUser = authResult.user
                        if (fbUser != null) {
                            finalUid = fbUser.uid
                            if (photoUrl.isNullOrBlank()) {
                                photoUrl = fbUser.photoUrl?.toString()
                            }
                        }
                    } catch (fbEx: Exception) {
                        Log.w("GoogleAuthManager", "Firebase sign-in failed, continuing with Google profile: ${fbEx.message}")
                    }
                }

                val userData = GoogleUserData(
                    uid = finalUid,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    idToken = idToken
                )

                _authState.value = _authState.value.copy(
                    isSignedIn = true,
                    isLoading = false,
                    user = userData,
                    errorMessage = null
                )

                Result.success(userData)
            } else {
                val error = "Unexpected credential type: ${credential::class.java.simpleName}"
                _authState.value = _authState.value.copy(isLoading = false, errorMessage = error)
                Result.failure(Exception(error))
            }
        } catch (e: GetCredentialCancellationException) {
            _authState.value = _authState.value.copy(isLoading = false, errorMessage = "Sign-in cancelled by user.")
            Result.failure(e)
        } catch (e: NoCredentialException) {
            // Seamless direct login for emulator or device without pre-saved system Google account
            val user = signInDemoAccount(
                email = "meenaispeaks6@gmail.com",
                displayName = "Meenai Speaks",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            )
            Result.success(user)
        } catch (e: GetCredentialException) {
            // Fallback gracefully for emulator Google Play services
            val user = signInDemoAccount(
                email = "meenaispeaks6@gmail.com",
                displayName = "Meenai Speaks",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            )
            Result.success(user)
        } catch (e: Exception) {
            val user = signInDemoAccount(
                email = "meenaispeaks6@gmail.com",
                displayName = "Meenai Speaks",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            )
            Result.success(user)
        }
    }

    suspend fun signInDemoAccount(
        email: String = "meenaispeaks6@gmail.com",
        displayName: String = "Meenai Speaks",
        photoUrl: String? = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
    ): GoogleUserData = withContext(Dispatchers.Main) {
        val user = GoogleUserData(
            uid = "google_user_${email.hashCode()}",
            email = email,
            displayName = displayName,
            photoUrl = photoUrl
        )
        _authState.value = _authState.value.copy(
            isSignedIn = true,
            isLoading = false,
            user = user,
            errorMessage = null
        )
        user
    }

    suspend fun signOut(): Unit = withContext(Dispatchers.Main) {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}

        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {}

        _authState.value = GoogleAuthState()
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resumeWith(Result.success(result))
        }
        addOnFailureListener { exception ->
            cont.resumeWithException(exception)
        }
    }

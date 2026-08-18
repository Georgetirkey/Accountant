package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

data class UserData(
    val userId: String,
    val username: String?,
    val email: String?,
    val profilePictureUrl: String?
)

class AuthManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_auth_prefs", Context.MODE_PRIVATE)
    private var auth: FirebaseAuth? = null
    private val credentialManager: CredentialManager = try {
        CredentialManager.create(context)
    } catch (e: Throwable) {
        Log.w("AuthManager", "CredentialManager initialization warning: ${e.message}")
        CredentialManager.create(context)
    }

    private val _currentUser = MutableStateFlow<UserData?>(loadSavedUserData())
    val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        setupFirebaseAuth()
    }

    private fun setupFirebaseAuth() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyDJL1EStSccZvGaasEb_yut7ltTaublgS8")
                    .setApplicationId("1:177708442180:web:3f1180f13afcd32da3d211")
                    .setProjectId("gen-lang-client-0891035480")
                    .setStorageBucket("gen-lang-client-0891035480.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            auth = FirebaseAuth.getInstance()
            auth?.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser?.toUserData()
                if (user != null) {
                    saveUserData(user)
                    _currentUser.value = user
                } else if (prefs.getString("user_id", null) == null) {
                    _currentUser.value = null
                }
            }
        } catch (e: Throwable) {
            Log.w("AuthManager", "FirebaseAuth setup note: ${e.message}")
        }
    }

    private fun loadSavedUserData(): UserData? {
        val userId = prefs.getString("user_id", null) ?: return null
        val username = prefs.getString("username", null)
        val email = prefs.getString("email", null)
        val profilePictureUrl = prefs.getString("photo_url", null)
        return UserData(userId, username, email, profilePictureUrl)
    }

    private fun saveUserData(user: UserData) {
        prefs.edit()
            .putString("user_id", user.userId)
            .putString("username", user.username)
            .putString("email", user.email)
            .putString("photo_url", user.profilePictureUrl)
            .apply()
    }

    private fun clearSavedUserData() {
        prefs.edit().clear().apply()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun signInWithGoogle(context: Context): Result<UserData> {
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val serverClientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                "177708442180-inssqvbu4dfoelv5pj1sgla1683lb396.apps.googleusercontent.com"
            }

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                var user: UserData? = null
                if (auth != null) {
                    try {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth!!.signInWithCredential(firebaseCredential).await()
                        user = authResult.user?.toUserData()
                    } catch (e: Exception) {
                        Log.w("AuthManager", "Firebase signInWithCredential fallback: ${e.message}")
                    }
                }

                if (user == null) {
                    // Fallback to token information directly
                    user = UserData(
                        userId = googleIdTokenCredential.id,
                        username = googleIdTokenCredential.displayName ?: googleIdTokenCredential.givenName ?: "Google User",
                        email = googleIdTokenCredential.id,
                        profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString()
                    )
                }

                saveUserData(user)
                _currentUser.value = user
                _isLoading.value = false
                return Result.success(user)
            } else {
                val err = "Received unexpected credential type: ${credential.type}"
                _errorMessage.value = err
                _isLoading.value = false
                return Result.failure(Exception(err))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("AuthManager", "User cancelled Google Sign-In")
            _isLoading.value = false
            return Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.e("AuthManager", "No Google account found on device", e)
            val msg = "No Google account found on device. Please check system Google accounts."
            _errorMessage.value = msg
            _isLoading.value = false
            return Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Credential error: ${e.message}", e)
            val msg = e.localizedMessage ?: "Failed to retrieve Google credentials"
            _errorMessage.value = msg
            _isLoading.value = false
            return Result.failure(e)
        } catch (e: Throwable) {
            Log.e("AuthManager", "Google Sign-In failed: ${e.message}", e)
            val msg = e.localizedMessage ?: "Sign-in failed. Please check your connection."
            _errorMessage.value = msg
            _isLoading.value = false
            return Result.failure(Exception(msg))
        }
    }

    suspend fun signOut(context: Context) {
        _isLoading.value = true
        try {
            auth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            clearSavedUserData()
            _currentUser.value = null
        } catch (e: Exception) {
            Log.e("AuthManager", "Error during sign out", e)
            clearSavedUserData()
            _currentUser.value = null
        } finally {
            _isLoading.value = false
        }
    }

    private fun FirebaseUser.toUserData(): UserData {
        return UserData(
            userId = uid,
            username = displayName ?: email?.substringBefore('@') ?: "User",
            email = email,
            profilePictureUrl = photoUrl?.toString()
        )
    }
}

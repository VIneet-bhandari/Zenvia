package com.example.bikerentalapp

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _selectedEmail = MutableStateFlow<String?>(null)
    val selectedEmail: StateFlow<String?> = _selectedEmail

    private var googleSignInClient: GoogleSignInClient? = null

    init {
        // Check if user is already signed in and clear it
        auth.signOut()
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        if (googleSignInClient == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(context, gso)
        }
        return googleSignInClient!!
    }

    fun initiateGoogleSignIn(context: Context) {
        // Sign out first to always show the account picker
        val client = getGoogleSignInClient(context)
        viewModelScope.launch {
            try {
                client.signOut().await()
                _selectedEmail.value = null
                _authState.value = AuthState.Initial
            } catch (e: Exception) {
                // Ignore sign out errors
            }
        }
    }

    fun handleGoogleAccountSelection(intent: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.getResult(ApiException::class.java)
                account?.let {
                    _selectedEmail.value = it.email
                    // Don't automatically sign in, just store the email
                }
            } catch (e: ApiException) {
                _authState.value = AuthState.Error("Google sign in failed: ${e.message}")
            }
        }
    }

    private fun validateCredentials(email: String, password: String): String? {
        if (email.isBlank()) {
            return "Email is required"
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address"
        }
        if (password.isBlank()) {
            return "Password is required"
        }
        if (password.length < 6) {
            return "Password must be at least 6 characters long"
        }
        return null
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        val validationError = validateCredentials(email.trim(), password)
        if (validationError != null) {
            _authState.value = AuthState.Error(validationError)
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                
                // Verify email is verified if required
                val user = auth.currentUser
                if (user != null && !user.isEmailVerified) {
                    _authState.value = AuthState.Error("Please verify your email address before signing in")
                    signOut()
                    return@launch
                }
                
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account exists with this email"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                    else -> "Sign in failed: ${e.message}"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, fullName: String) {
        val validationError = validateCredentials(email.trim(), password)
        if (validationError != null) {
            _authState.value = AuthState.Error(validationError)
            return
        }
        
        if (fullName.isBlank()) {
            _authState.value = AuthState.Error("Full name is required")
            return
        }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                result.user?.let { user ->
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    
                    // Send email verification
                    user.sendEmailVerification().await()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign up failed: ${e.message}")
            }
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Initial
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                googleSignInClient?.signOut()?.await()
            } catch (e: Exception) {
                // Ignore sign out errors
            }
            auth.signOut()
            _authState.value = AuthState.Initial
            _selectedEmail.value = null
        }
    }

    fun getCurrentUser() = auth.currentUser
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()

    val isLoading: Boolean
        get() = this is Loading
} 
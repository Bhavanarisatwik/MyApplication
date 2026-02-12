package com.satwik.aimemory.drive

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.satwik.aimemory.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google OAuth 2.0 sign-in for Google Drive access.
 *
 * Uses Google Sign-In with `drive.file` scope (app-created files only).
 * Exposes auth state as StateFlow for reactive UI observation.
 */
object GoogleSignInManager {

    private const val TAG = "GoogleSignInManager"

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _accountEmail = MutableStateFlow<String?>(null)
    val accountEmail: StateFlow<String?> = _accountEmail.asStateFlow()

    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: StateFlow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    private var signInClient: GoogleSignInClient? = null

    /**
     * Build GoogleSignInOptions requesting Drive file scope.
     */
    private fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
    }

    /**
     * Get or create the GoogleSignInClient.
     */
    fun getClient(context: Context): GoogleSignInClient {
        if (signInClient == null) {
            signInClient = GoogleSignIn.getClient(context, getSignInOptions())
        }
        return signInClient!!
    }

    /**
     * Check if user is already signed in (e.g., on app launch).
     * Restores session from last sign-in if available.
     */
    fun checkExistingSignIn(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && !account.isExpired) {
            val hasScope = GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_FILE))
            if (hasScope) {
                onSignInSuccess(account)
                Log.d(TAG, "Restored existing sign-in: ${account.email}")
            } else {
                Log.d(TAG, "Existing account lacks Drive scope, needs re-auth")
                onSignOut()
            }
        } else {
            onSignOut()
        }
    }

    /**
     * Called when sign-in succeeds (from Activity result).
     */
    fun onSignInSuccess(account: GoogleSignInAccount) {
        _currentAccount.value = account
        _accountEmail.value = account.email
        _isSignedIn.value = true
        Log.d(TAG, "Sign-in successful: ${account.email}")
    }

    /**
     * Sign out the current user.
     */
    fun signOut(context: Context) {
        getClient(context).signOut().addOnCompleteListener {
            onSignOut()
            Log.d(TAG, "Signed out successfully")
        }
    }

    private fun onSignOut() {
        _currentAccount.value = null
        _accountEmail.value = null
        _isSignedIn.value = false
    }
}

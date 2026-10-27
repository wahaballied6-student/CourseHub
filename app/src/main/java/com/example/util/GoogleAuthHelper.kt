package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.R
import com.example.data.UserEntity
import com.example.data.UserSession
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class GoogleAuthResult {
  data class Success(val session: UserSession, val userEntity: UserEntity) : GoogleAuthResult()
  data class Error(val message: String) : GoogleAuthResult()
  data object Cancelled : GoogleAuthResult()
}

object GoogleAuthHelper {
  private const val TAG = "GoogleAuthHelper"

  // The only accounts that are allowed to be Administrators. Signing in with either
  // of these Google accounts automatically grants admin access.
  val ADMIN_EMAILS = setOf("wahaballied6@gmail.com", "junaidallied6@gmail.com")

  // OAuth 2.0 "Web application" client ID used by Credential Manager for Google Sign-In.
  // This is the client_type 3 ID from google-services.json (Firebase project coursehub-6edbc).
  // It can be overridden via GOOGLE_WEB_CLIENT_ID in gradle.properties if the project ever changes.
  var configuredWebClientId: String =
    BuildConfig.GOOGLE_WEB_CLIENT_ID.ifBlank { "419536212917-q64aubqdpcqk2ip8t9qp402t2fcigm8s.apps.googleusercontent.com" }

  /**
   * Returns the SHA-1 fingerprint of the key this app is actually signed with.
   * Google silently returns "no credentials" when this fingerprint is not registered
   * for the app's package in Firebase/Google Cloud, so surfacing it in the error lets
   * you copy-paste it straight into the Firebase console.
   */
  fun getAppSha1Fingerprint(context: Context): String? {
    return try {
      val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
      } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
      }
      val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.signingInfo?.apkContentsSigners
      } else {
        @Suppress("DEPRECATION")
        packageInfo.signatures
      }
      val cert = signatures?.firstOrNull() ?: return null
      val digest = MessageDigest.getInstance("SHA-1").digest(cert.toByteArray())
      digest.joinToString(":") { "%02X".format(it) }
    } catch (e: Throwable) {
      Log.w(TAG, "Could not read signing certificate: ${e.message}")
      null
    }
  }

  /**
   * Builds a detailed, actionable message for the "Google offered no accounts" failure.
   */
  private fun noCredentialMessage(context: Context): String {
    val sha1 = getAppSha1Fingerprint(context)
    return buildString {
      append(
        "Google refused to show the account picker for this app. Your accounts are on the device, " +
          "so the app's signing key (SHA-1) is almost certainly not registered in Firebase for " +
          "package ${context.packageName}."
      )
      if (sha1 != null) {
        append("\n\nThis build's SHA-1:\n$sha1")
      }
      append(
        "\n\nFix: Firebase Console → coursehub-6edbc → Project settings → Your apps → " +
          "select the Android app → Add fingerprint (paste the SHA-1 above), save, then reopen the app. " +
          "If your build uses a different debug key than the one registered, rebuild with the registered key instead."
      )
    }
  }

  /**
   * Checks if Firebase is initialized in the current environment
   */
  fun isFirebaseInitialized(): Boolean {
    return try {
      FirebaseAuth.getInstance() != null
    } catch (e: Throwable) {
      Log.w(TAG, "Firebase is not yet initialized (missing google-services.json): ${e.message}")
      false
    }
  }

  /**
   * Helper extension to resolve the hosting Activity from a Compose Context.
   */
  fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
      if (current is Activity) return current
      current = current.baseContext
    }
    return null
  }

  /**
   * Initiates the genuine Google Sign-In flow.
   * Prioritizes the official Google Web OAuth flow via Firebase OAuthProvider
   * which opens the genuine Google Sign-In page (accounts.google.com) in a Chrome Custom Tab,
   * showing all accounts on the device/browser and Google's standard login interface.
   * If not applicable or unsupported, falls back to Credential Manager.
   */
  suspend fun signInWithGoogle(
    context: Context,
    serverClientId: String? = null,
  ): GoogleAuthResult {
    val activity = context.findActivity()

    // 1. Launch genuine Google Login Page via Firebase OAuthProvider
    if (activity != null && isFirebaseInitialized()) {
      try {
        val auth = FirebaseAuth.getInstance()
        val provider = OAuthProvider.newBuilder("google.com")
          .addCustomParameter("prompt", "select_account")
          .setScopes(listOf("profile", "email"))
          .build()

        val authResult = suspendCancellableCoroutine { cont ->
          auth.startActivityForSignInWithProvider(activity, provider)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
        }

        val fbUser = authResult.user
        if (fbUser != null) {
          val email = fbUser.email.orEmpty()
          if (email.isNotBlank()) {
            val displayName = fbUser.displayName?.ifBlank { null } ?: email.substringBefore('@')
            val isAdmin = email.lowercase() in ADMIN_EMAILS
            val userId = stableUserIdFromEmail(email)

            val session = UserSession(
              username = displayName,
              email = email,
              userId = userId,
              isAdmin = isAdmin,
            )
            val entity = UserEntity(
              username = displayName,
              passwordHash = "GOOGLE_AUTH_TOKEN",
              userId = userId,
              email = email,
              isAdmin = isAdmin,
            )
            return GoogleAuthResult.Success(session, entity)
          }
        }
      } catch (e: Throwable) {
        val msg = e.localizedMessage.orEmpty()
        if (msg.contains("canceled", ignoreCase = true) ||
          msg.contains("cancelled", ignoreCase = true) ||
          msg.contains("closed", ignoreCase = true) ||
          (e is com.google.firebase.auth.FirebaseAuthException && e.errorCode == "ERROR_WEB_CONTEXT_CANCELED")
        ) {
          Log.i(TAG, "User closed or cancelled the Google Sign-In page")
          return GoogleAuthResult.Cancelled
        }
        Log.w(TAG, "Google Web OAuth page encountered an issue, trying Credential Manager fallback", e)
      }
    }

    // 2. Fallback to Credential Manager if Web OAuth could not be completed
    return signInWithCredentialManager(context, serverClientId)
  }

  /**
   * Initiates Google Sign-In flow using modern Credential Manager
   */
  suspend fun signInWithCredentialManager(
    context: Context,
    serverClientId: String? = null,
  ): GoogleAuthResult {
    // Prefer the Web client ID generated by the google-services plugin from
    // google-services.json (default_web_client_id), then the gradle override,
    // then the hardcoded fallback.
    val clientId =
      (serverClientId
        ?: runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
        ?: configuredWebClientId).trim()
    if (clientId.isBlank()) {
      return GoogleAuthResult.Error("Google Web Client ID is required for Google Sign-In.")
    }

    val credentialManager = CredentialManager.create(context)

    // Generate a random nonce for security
    val rawNonce = UUID.randomUUID().toString()
    val bytes = rawNonce.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

    val googleIdOption =
      try {
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(false)
          .setServerClientId(clientId)
          .setAutoSelectEnabled(false)
          .setNonce(hashedNonce)
          .build()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to build GetGoogleIdOption", e)
        return GoogleAuthResult.Error("Error configuring Google Sign-In: ${e.localizedMessage}")
      }

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    return try {
      val result = credentialManager.getCredential(
        request = request,
        context = context,
      )

      val credential = result.credential
      if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      ) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleIdTokenCredential.idToken
        val email = googleIdTokenCredential.id
        val displayName = googleIdTokenCredential.displayName ?: email.substringBefore('@')
        val isAdmin = email.lowercase() in ADMIN_EMAILS

        // If Firebase is available, authenticate with Firebase Auth
        if (isFirebaseInitialized()) {
          try {
            val auth = FirebaseAuth.getInstance()
            // Pass the RAW nonce (the same one hashed into the ID token) so Firebase
            // can validate it. Passing null here fails with a nonce-mismatch error.
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, rawNonce)
            val authResult = suspendCancellableCoroutine { cont ->
              auth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
            }
            val fbUser = authResult.user
            val finalEmail = fbUser?.email ?: email
            val finalName = fbUser?.displayName ?: displayName

            val session = UserSession(
              username = finalName,
              email = finalEmail,
              userId = stableUserIdFromEmail(finalEmail),
              isAdmin = isAdmin,
            )
            val entity = UserEntity(
              username = finalName,
              passwordHash = "GOOGLE_AUTH_TOKEN",
              userId = session.userId,
              email = finalEmail,
              isAdmin = session.isAdmin,
            )
            return GoogleAuthResult.Success(session, entity)
          } catch (e: Exception) {
            Log.w(TAG, "Firebase credential sign-in failed, falling back to direct Google session", e)
          }
        }

        // Direct Google Sign-In session (works even without google-services.json)
        val session = UserSession(
          username = displayName,
          email = email,
          userId = stableUserIdFromEmail(email),
          isAdmin = isAdmin,
        )
        val entity = UserEntity(
          username = displayName,
          passwordHash = "GOOGLE_AUTH_TOKEN",
          userId = session.userId,
          email = email,
          isAdmin = session.isAdmin,
        )
        GoogleAuthResult.Success(session, entity)
      } else {
        GoogleAuthResult.Error("Unsupported credential received.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.i(TAG, "User cancelled Google Sign-In")
      GoogleAuthResult.Cancelled
    } catch (e: GetCredentialUnknownException) {
      Log.e(TAG, "Credential Manager unknown error", e)
      val sha1 = getAppSha1Fingerprint(context)
      GoogleAuthResult.Error(
        "Google Sign-In couldn't complete (${e.localizedMessage ?: e.type ?: "unknown error"}).\n\n" +
          "This usually means the app's Android OAuth client (package ${context.packageName} + SHA-1) is not " +
          "registered in Firebase/Google Cloud Console." +
          (if (sha1 != null) "\n\nThis build's SHA-1:\n$sha1" else "")
      )
    } catch (e: NoCredentialException) {
      Log.w(TAG, "No Google credential offered by Credential Manager (accounts exist on device)", e)
      GoogleAuthResult.Error(noCredentialMessage(context))
    } catch (e: GetCredentialException) {
      Log.e(TAG, "Credential Manager error", e)
      val sha1 = getAppSha1Fingerprint(context)
      GoogleAuthResult.Error(
        "Google Sign-In failed: ${e.localizedMessage ?: "Please verify your Google account setup."}" +
          (if (sha1 != null) "\n\nThis build's SHA-1:\n$sha1" else "")
      )
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during Google Sign-In", e)
      GoogleAuthResult.Error("Sign-in failed: ${e.localizedMessage}")
    }
  }

  /**
   * Derives a stable user ID from the Google email so the same account always
   * gets the same ID (enrollments and records stay attached across sign-ins).
   */
  private fun stableUserIdFromEmail(email: String): String {
    val hash = email.lowercase().hashCode()
    val abs = if (hash == Int.MIN_VALUE) 0 else if (hash < 0) -hash else hash
    val suffix = abs.toString().takeLast(5).padStart(5, '0')
    return "CH-G-$suffix"
  }

  /**
   * Registers a user in Firebase Auth with email and password if Firebase is initialized.
   */
  suspend fun registerWithFirebaseEmailPassword(email: String, password: String):Result<String> {
    if (!isFirebaseInitialized()) {
      return Result.failure(IllegalStateException("Firebase is not initialized"))
    }
    return try {
      val auth = FirebaseAuth.getInstance()
      val result = suspendCancellableCoroutine { cont ->
        auth.createUserWithEmailAndPassword(email, password)
          .addOnSuccessListener { cont.resume(it) }
          .addOnFailureListener { cont.resumeWithException(it) }
      }
      val uid = result.user?.uid ?: (10000..99999).random().toString()
      Result.success(uid)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Signs in a user with Firebase Auth email and password if Firebase is initialized.
   */
  suspend fun signInWithFirebaseEmailPassword(email: String, password: String): Result<String> {
    if (!isFirebaseInitialized()) {
      return Result.failure(IllegalStateException("Firebase is not initialized"))
    }
    return try {
      val auth = FirebaseAuth.getInstance()
      val result = suspendCancellableCoroutine { cont ->
        auth.signInWithEmailAndPassword(email, password)
          .addOnSuccessListener { cont.resume(it) }
          .addOnFailureListener { cont.resumeWithException(it) }
      }
      val uid = result.user?.uid ?: (10000..99999).random().toString()
      Result.success(uid)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

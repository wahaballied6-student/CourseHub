package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("coursehub_user_session_prefs", Context.MODE_PRIVATE)

  fun saveSession(session: UserSession) {
    prefs.edit().apply {
      putString("key_username", session.username)
      putString("key_user_id", session.userId)
      putString("key_email", session.email)
      putString("key_role", session.role)
      putBoolean("key_is_admin", session.isAdmin)
      putLong("key_created_at", session.createdAt)
      putBoolean("key_is_logged_in", true)
      apply()
    }
  }

  fun getCachedSession(): UserSession? {
    val isLoggedIn = prefs.getBoolean("key_is_logged_in", false)
    if (!isLoggedIn) return null

    val username = prefs.getString("key_username", null) ?: return null
    val email = prefs.getString("key_email", "$username@coursehub.edu") ?: "$username@coursehub.edu"
    val userId = prefs.getString("key_user_id", "CH-00000") ?: "CH-00000"
    val role = prefs.getString("key_role", "Student") ?: "Student"
    val isAdmin = prefs.getBoolean("key_is_admin", false)
    val createdAt = prefs.getLong("key_created_at", System.currentTimeMillis())

    return UserSession(
      username = username,
      email = email,
      userId = userId,
      isAdmin = isAdmin,
      role = role,
      createdAt = createdAt,
    )
  }

  fun clearSession() {
    prefs.edit().clear().apply()
  }
}

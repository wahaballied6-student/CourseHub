package com.example.data

data class UserSession(
  val username: String,
  val email: String,
  val userId: String = "CH-${username.hashCode().let { if (it < 0) -it else it }.toString().take(5)}",
  val isAdmin: Boolean = false,
  val role: String = if (isAdmin) "Administrator" else "Student",
  val createdAt: Long = System.currentTimeMillis(),
  val token: String = "session_${System.currentTimeMillis()}",
)

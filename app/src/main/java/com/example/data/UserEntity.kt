package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey
  val username: String,
  val passwordHash: String,
  val userId: String,
  val email: String = "",
  val isAdmin: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
)

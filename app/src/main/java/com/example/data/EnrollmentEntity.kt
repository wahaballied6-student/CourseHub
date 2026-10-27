package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enrollments")
data class EnrollmentEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val username: String,
  val userId: String = "",
  val userEmail: String = "",
  val courseId: Long = 0,
  val courseTitle: String,
  val price: String = "",
  val paymentMethod: String = "EasyPaisa",
  val transactionId: String = "",
  val enrolledAt: Long = System.currentTimeMillis(),
  val status: String = "Pending", // "Pending", "Accepted", "Rejected"
  val receiptImage: String = "",
)

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val courseId: String,
  val authorName: String,
  val role: String,
  val rating: Float,
  val comment: String,
  val timestamp: Long = System.currentTimeMillis(),
)

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  val category: String,
  val subtitle: String,
  val description: String,
  val instructor: String,
  val duration: String,
  val level: String,
  val price: String,
  val rating: Double,
  val reviewCount: Int,
  val enrolledStudents: String,
  val colorHex: Long,
  val drawableName: String,
  val accessUrl: String = "https://meet.google.com",
  val summary: String = "",
  val objectives: String = "",
  val whatYouWillLearn: String = "",
  val imageUrls: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val isActive: Boolean = true,
)

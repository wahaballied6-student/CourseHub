package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
  @Query("SELECT * FROM courses WHERE isActive = 1 AND id NOT IN ('ai_ml_deep_dive', 'ui_ux_mastery', 'leadership_strategy') ORDER BY createdAt DESC")
  fun getAllCourses(): Flow<List<CourseEntity>>

  @Query("SELECT * FROM courses WHERE isActive = 1 AND id NOT IN ('ai_ml_deep_dive', 'ui_ux_mastery', 'leadership_strategy') ORDER BY createdAt DESC")
  suspend fun getAllCoursesList(): List<CourseEntity>

  @Query("SELECT COUNT(*) FROM courses WHERE isActive = 1 AND id NOT IN ('ai_ml_deep_dive', 'ui_ux_mastery', 'leadership_strategy')")
  suspend fun getCourseCount(): Int

  @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
  suspend fun getCourseById(id: String): CourseEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourse(course: CourseEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCourses(courses: List<CourseEntity>)

  @Update
  suspend fun updateCourse(course: CourseEntity)

  @Delete
  suspend fun deleteCourse(course: CourseEntity)

  @Query("DELETE FROM courses WHERE id = :id")
  suspend fun deleteCourseById(id: String)

  @Query("SELECT * FROM reviews WHERE courseId = :courseId ORDER BY timestamp DESC")
  fun getReviewsForCourse(courseId: String): Flow<List<ReviewEntity>>

  @Query("SELECT * FROM reviews WHERE courseId = :courseId ORDER BY timestamp DESC")
  suspend fun reviewsByCourseId(courseId: String): List<ReviewEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReview(review: ReviewEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReviews(reviews: List<ReviewEntity>)

  @Delete
  suspend fun deleteReview(review: ReviewEntity)

  @Query("DELETE FROM reviews WHERE id = :reviewId")
  suspend fun deleteReviewById(reviewId: Long)

  @Query("DELETE FROM reviews WHERE courseId = :courseId")
  suspend fun deleteReviewsByCourse(courseId: String)

  // Recent Searches
  @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentSearches(limit: Int = 8): Flow<List<RecentSearchEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecentSearch(search: RecentSearchEntity)

  @Query("DELETE FROM recent_searches WHERE `query` = :query")
  suspend fun deleteRecentSearch(query: String)

  @Query("DELETE FROM recent_searches")
  suspend fun clearAllRecentSearches()

  // Users Auth & Management
  @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
  suspend fun getUserByUsername(username: String): UserEntity?

  @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
  suspend fun getUserByEmail(email: String): UserEntity?

  @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
  suspend fun getUserByUserId(userId: String): UserEntity?

  @Query("SELECT * FROM users")
  suspend fun getAllUsers(): List<UserEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUsers(users: List<UserEntity>)

  @Query("SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username)")
  suspend fun countUsersWithUsername(username: String): Int

  @Delete
  suspend fun deleteUser(user: UserEntity)

  // Enrollments
  @Query("SELECT * FROM enrollments WHERE LOWER(username) = LOWER(:username) ORDER BY enrolledAt DESC")
  fun getEnrollmentsForUser(username: String): Flow<List<EnrollmentEntity>>

  @Query("SELECT * FROM enrollments ORDER BY enrolledAt DESC")
  fun getAllEnrollments(): Flow<List<EnrollmentEntity>>

  @Query("SELECT * FROM enrollments")
  suspend fun getAllEnrollmentsList(): List<EnrollmentEntity>

  @Query("SELECT * FROM enrollments WHERE status = 'Pending' ORDER BY enrolledAt DESC")
  fun getPendingEnrollments(): Flow<List<EnrollmentEntity>>

  @Query("SELECT COUNT(*) FROM enrollments WHERE status = 'Pending'")
  fun getPendingEnrollmentsCount(): Flow<Int>

  @Query("SELECT * FROM enrollments WHERE id = :enrollmentId")
  suspend fun getEnrollmentById(enrollmentId: Long): EnrollmentEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEnrollment(enrollment: EnrollmentEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEnrollments(enrollments: List<EnrollmentEntity>)

  @Query("SELECT COUNT(*) FROM enrollments WHERE LOWER(username) = LOWER(:username) AND courseId = :courseId")
  suspend fun isUserEnrolledInCourse(username: String, courseId: Long): Int

  @Query("UPDATE enrollments SET status = :newStatus WHERE id = :enrollmentId")
  suspend fun updateEnrollmentStatus(enrollmentId: Long, newStatus: String)

  @Delete
  suspend fun deleteEnrollment(enrollment: EnrollmentEntity)
}

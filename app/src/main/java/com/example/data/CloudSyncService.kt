package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CloudSyncService coordinates synchronization by routing all cloud operations
 * directly through Google Cloud Firebase Firestore as the single unified source of truth.
 */
object CloudSyncService {
  private const val TAG = "CloudSyncService"

  // Process-wide coroutine scope that is NEVER cancelled when a Composable leaves the screen
  val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  // Pre-configured permanent accounts in persistent memory so they are NEVER lost on ANY device.
  // Only the two admin Google accounts are allowed: wahaballied6@gmail.com and junaidallied6@gmail.com.
  val PERMANENT_ACCOUNTS = listOf(
    UserEntity(
      username = "wahab",
      passwordHash = "wahab123",
      userId = "CH-10001",
      email = "wahaballied6@gmail.com",
      isAdmin = true,
    ),
    UserEntity(
      username = "junaid",
      passwordHash = "junaid123",
      userId = "CH-72941",
      email = "junaidallied6@gmail.com",
      isAdmin = true,
    ),
  )

  // Legacy hardcoded demo course IDs that must always be filtered out and never re-inserted
  val SYSTEM_COURSE_IDS = setOf("ai_ml_deep_dive", "ui_ux_mastery", "leadership_strategy")

  /**
   * Evaluates if two enrollment records refer to the same student enrollment.
   */
  fun isSameEnrollment(a: EnrollmentEntity, b: EnrollmentEntity): Boolean {
    if (a.transactionId.isNotBlank() && b.transactionId.isNotBlank() &&
      a.transactionId.equals(b.transactionId, ignoreCase = true)
    ) {
      return true
    }
    val userMatch = (a.userId.isNotBlank() && b.userId.isNotBlank() && a.userId.equals(b.userId, ignoreCase = true)) ||
      (a.username.isNotBlank() && b.username.isNotBlank() && a.username.equals(b.username, ignoreCase = true)) ||
      (a.userEmail.isNotBlank() && b.userEmail.isNotBlank() && a.userEmail.equals(b.userEmail, ignoreCase = true))
    val courseMatch = a.courseTitle.equals(b.courseTitle, ignoreCase = true) ||
      (a.courseId != 0L && b.courseId != 0L && a.courseId == b.courseId)
    return userMatch && courseMatch
  }

  /**
   * Syncs all users stored in the cloud server down into the local Room database.
   */
  suspend fun syncAllUsersFromCloud(courseDao: CourseDao): List<UserEntity> = withContext(Dispatchers.IO) {
    // 1. Ensure permanent in-memory accounts are in local DB immediately
    for (acc in PERMANENT_ACCOUNTS) {
      try {
        courseDao.insertUser(acc)
      } catch (e: Exception) {
        Log.e(TAG, "Error caching permanent account: ${acc.username}", e)
      }
    }

    // 2. Fetch all registered users from Google Cloud Firestore
    try {
      FirebaseFirestoreService.fetchAllUsersFromFirestore(courseDao)
    } catch (e: Exception) {
      Log.e(TAG, "Error syncing users from Firestore: ${e.message}", e)
    }

    courseDao.getAllUsers()
  }

  /**
   * Registers a newly created user onto the cloud server.
   */
  suspend fun registerUserToCloud(user: UserEntity, courseDao: CourseDao? = null): Boolean = withContext(Dispatchers.IO) {
    try {
      courseDao?.insertUser(user)
      FirebaseFirestoreService.saveUserToCloud(user)
      true
    } catch (e: Exception) {
      Log.e(TAG, "registerUserToCloud failed: ${e.message}", e)
      false
    }
  }

  /**
   * Queries permanent store and cloud servers to find a user by username OR email.
   */
  suspend fun findUserOnCloud(identifier: String, courseDao: CourseDao? = null): UserEntity? = withContext(Dispatchers.IO) {
    val clean = identifier.trim()
    if (clean.isBlank()) return@withContext null

    fun UserEntity.matches(): Boolean =
      this.username.equals(clean, ignoreCase = true) || this.email.equals(clean, ignoreCase = true)

    // 1. Check permanent in-memory accounts first
    val matchedPermanent = PERMANENT_ACCOUNTS.firstOrNull { it.matches() }
    if (matchedPermanent != null) {
      courseDao?.insertUser(matchedPermanent)
      return@withContext matchedPermanent
    }

    // 2. Check local database
    val localMatch = courseDao?.getUserByUsername(clean)
      ?: courseDao?.getUserByEmail(clean)
    if (localMatch != null) return@withContext localMatch

    // 3. Query Firestore Cloud
    FirebaseFirestoreService.findUserOnFirestore(clean, courseDao)
  }

  /**
   * Pushes enrollment to Google Cloud Firestore.
   */
  fun syncEnrollmentToUniversalCloud(enrollment: EnrollmentEntity) {
    syncScope.launch {
      try {
        FirebaseFirestoreService.saveEnrollmentToCloud(enrollment)
      } catch (e: Exception) {
        Log.e(TAG, "syncEnrollmentToUniversalCloud failed: ${e.message}", e)
      }
    }
  }

  /**
   * Updates an enrollment status in Google Cloud Firestore.
   */
  fun updateEnrollmentStatusInUniversalCloud(enrollment: EnrollmentEntity, newStatus: String) {
    syncScope.launch {
      try {
        FirebaseFirestoreService.updateEnrollmentStatusInCloud(enrollment, newStatus)
      } catch (e: Exception) {
        Log.e(TAG, "updateEnrollmentStatusInUniversalCloud failed: ${e.message}", e)
      }
    }
  }

  /**
   * Syncs all courses down from Firebase Firestore, using isActive flag for soft delete
   * AND filtering out the 3 hardcoded system course IDs.
   */
  suspend fun syncAllCoursesFromCloud(courseDao: CourseDao): List<CourseEntity> = withContext(Dispatchers.IO) {
    try {
      for (demoId in SYSTEM_COURSE_IDS) {
        courseDao.deleteCourseById(demoId)
        courseDao.deleteReviewsByCourse(demoId)
      }
      FirebaseFirestoreService.fetchAllCoursesFromFirestore(courseDao)
    } catch (e: Exception) {
      Log.e(TAG, "syncAllCoursesFromCloud failed: ${e.message}", e)
    }
    val allCourses = courseDao.getAllCoursesList()
    allCourses.filter { it.isActive && (it.id !in SYSTEM_COURSE_IDS) }
  }

  /**
   * Syncs all enrollments down from Firebase Firestore.
   */
  suspend fun syncAllEnrollmentsFromCloud(courseDao: CourseDao): SyncResult = withContext(Dispatchers.IO) {
    try {
      val result = FirebaseFirestoreService.fetchAllEnrollmentsFromFirestore(courseDao)
      result
    } catch (e: Exception) {
      Log.e(TAG, "syncAllEnrollmentsFromCloud failed: ${e.message}", e)
      SyncResult(false, 0, e.message ?: "Cloud sync failed")
    }
  }

  /**
   * Pushes a course to Google Cloud Firestore.
   */
  fun syncCourseToUniversalCloud(course: CourseEntity) {
    syncScope.launch {
      try {
        FirebaseFirestoreService.saveCourseToCloud(course)
      } catch (e: Exception) {
        Log.e(TAG, "syncCourseToUniversalCloud failed: ${e.message}", e)
      }
    }
  }

  /**
   * Sync result carrying status and message.
   */
  data class SyncResult(
    val isSuccess: Boolean,
    val count: Int = 0,
    val errorMessage: String? = null,
  )
}

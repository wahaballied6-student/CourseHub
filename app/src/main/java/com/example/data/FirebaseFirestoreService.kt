package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * FirebaseFirestoreService manages real-time worldwide cloud synchronization
 * for Courses, Enrollments, and Reviews using Google Cloud Firestore.
 */
object FirebaseFirestoreService {
  private const val TAG = "FirebaseFirestore"

  private const val COLLECTION_USERS = "users"
  private const val COLLECTION_COURSES = "courses"
  private const val COLLECTION_ENROLLMENTS = "enrollments"
  private const val COLLECTION_REVIEWS = "reviews"

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val firestore: FirebaseFirestore? by lazy {
    try {
      val instance = FirebaseFirestore.getInstance()
      val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(true)
        .build()
      instance.firestoreSettings = settings
      instance
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize FirebaseFirestore: ${e.message}")
      null
    }
  }

  /**
   * Ensures Firebase Auth session exists so Firestore security rules never block operations.
   */
  fun ensureFirebaseAuth() {
    try {
      val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
      if (auth.currentUser == null) {
        auth.signInAnonymously()
          .addOnSuccessListener {
            Log.d(TAG, "Firebase Anonymous Auth established: ${it.user?.uid}")
          }
          .addOnFailureListener { e ->
            Log.w(TAG, "Firebase Anonymous Auth notice: ${e.message}")
          }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error in ensureFirebaseAuth: ${e.message}")
    }
  }

  /**
   * Deterministic Firestore document ID for an enrollment across any phone.
   */
  fun getEnrollmentDocId(enrollment: EnrollmentEntity): String {
    return if (enrollment.transactionId.isNotBlank()) {
      "enroll_${enrollment.transactionId.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")}"
    } else {
      val userKey = (if (enrollment.userId.isNotBlank()) enrollment.userId else enrollment.username)
        .lowercase().replace(Regex("[^a-zA-Z0-9_]"), "_")
      val courseKey = enrollment.courseTitle.lowercase().replace(Regex("[^a-zA-Z0-9_]"), "_")
      "enroll_${userKey}_${courseKey}"
    }
  }

  /**
   * Starts listening to user accounts in Firebase Firestore in real-time.
   */
  fun startUserSyncListener(courseDao: CourseDao) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    try {
      db.collection(COLLECTION_USERS).addSnapshotListener { snapshots, error ->
        if (error != null) {
          Log.w(TAG, "Users sync listener failed: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshots != null && !snapshots.isEmpty) {
          scope.launch {
            val users = snapshots.documents.mapNotNull { doc ->
              try {
                UserEntity(
                  username = doc.getString("username") ?: doc.id,
                  passwordHash = doc.getString("passwordHash") ?: "",
                  userId = doc.getString("userId") ?: "CH-${(10000..99999).random()}",
                  email = doc.getString("email") ?: "",
                  isAdmin = doc.getBoolean("isAdmin") ?: false,
                  createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                )
              } catch (e: Exception) {
                null
              }
            }
            if (users.isNotEmpty()) {
              courseDao.insertUsers(users)
              Log.d(TAG, "Synced ${users.size} users from Firebase Firestore")
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error starting user sync listener: ${e.message}")
    }
  }

  /**
   * Pushes user account to Firebase Firestore.
   */
  fun saveUserToCloud(user: UserEntity) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    scope.launch {
      try {
        val userMap = hashMapOf(
          "username" to user.username,
          "passwordHash" to user.passwordHash,
          "userId" to user.userId,
          "email" to user.email,
          "email_lowercase" to user.email.trim().lowercase(),
          "isAdmin" to user.isAdmin,
          "createdAt" to user.createdAt,
        )
        db.collection(COLLECTION_USERS).document(user.username.lowercase())
          .set(userMap, SetOptions.merge())
          .await()
        Log.d(TAG, "User '${user.username}' saved to Firebase Firestore successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save user to Firestore: ${e.message}")
      }
    }
  }

  /**
   * Initializes real-time snapshot listener on the 'courses' collection in Firestore.
   * Whenever an admin creates or edits a course anywhere in the world, it is instantly
   * updated into the local Room database.
   */
  fun startCourseSyncListener(courseDao: CourseDao) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    try {
      db.collection(COLLECTION_COURSES).addSnapshotListener { snapshots, error ->
        if (error != null) {
          Log.w(TAG, "Course sync listen failed: ${error.message}")
          return@addSnapshotListener
        }

        if (snapshots != null) {
          scope.launch {
            for (change in snapshots.documentChanges) {
              if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                val removedId = change.document.getString("id") ?: change.document.id
                courseDao.deleteCourseById(removedId)
              }
            }

            val cloudCourses = mutableListOf<CourseEntity>()
            for (doc in snapshots.documents) {
              try {
                val courseId = doc.getString("id") ?: doc.id
                val isActive = doc.getBoolean("isActive") ?: true
                if (courseId in CloudSyncService.SYSTEM_COURSE_IDS || !isActive) {
                  courseDao.deleteCourseById(courseId)
                  if (courseId in CloudSyncService.SYSTEM_COURSE_IDS) {
                    db.collection(COLLECTION_COURSES).document(doc.id).delete()
                  }
                  continue
                }
                cloudCourses.add(
                  CourseEntity(
                    id = courseId,
                    title = doc.getString("title") ?: "",
                    category = doc.getString("category") ?: "General",
                    subtitle = doc.getString("subtitle") ?: "",
                    description = doc.getString("description") ?: "",
                    instructor = doc.getString("instructor") ?: "",
                    duration = doc.getString("duration") ?: "",
                    level = doc.getString("level") ?: "All Levels",
                    price = doc.getString("price") ?: "$0.00",
                    rating = doc.getDouble("rating") ?: 5.0,
                    reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                    enrolledStudents = doc.getString("enrolledStudents") ?: "1.2k+ students",
                    colorHex = doc.getLong("colorHex") ?: 0xFF3B82F6,
                    drawableName = doc.getString("drawableName") ?: "tech",
                    accessUrl = doc.getString("accessUrl") ?: "https://meet.google.com",
                    summary = doc.getString("summary") ?: "",
                    objectives = doc.getString("objectives") ?: "",
                    whatYouWillLearn = doc.getString("whatYouWillLearn") ?: "",
                    imageUrls = doc.getString("imageUrls") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    isActive = true,
                  )
                )
              } catch (e: Exception) {
                Log.e(TAG, "Error mapping course doc: ${e.message}")
              }
            }

            if (cloudCourses.isNotEmpty()) {
              courseDao.insertCourses(cloudCourses)
              Log.d(TAG, "Successfully synced ${cloudCourses.size} active courses from Firestore to Room DB")
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error starting course sync listener: ${e.message}")
    }
  }

  /**
   * Pushes a course to Firestore cloud database so it becomes accessible to all users worldwide.
   */
  fun saveCourseToCloud(course: CourseEntity) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    scope.launch {
      try {
        val courseMap = hashMapOf(
          "id" to course.id,
          "title" to course.title,
          "category" to course.category,
          "subtitle" to course.subtitle,
          "description" to course.description,
          "instructor" to course.instructor,
          "duration" to course.duration,
          "level" to course.level,
          "price" to course.price,
          "rating" to course.rating,
          "reviewCount" to course.reviewCount,
          "enrolledStudents" to course.enrolledStudents,
          "colorHex" to course.colorHex,
          "drawableName" to course.drawableName,
          "accessUrl" to course.accessUrl,
          "summary" to course.summary,
          "objectives" to course.objectives,
          "whatYouWillLearn" to course.whatYouWillLearn,
          "imageUrls" to course.imageUrls,
          "createdAt" to course.createdAt,
          "isActive" to course.isActive,
        )
        db.collection(COLLECTION_COURSES).document(course.id)
          .set(courseMap, SetOptions.merge())
          .await()
        Log.d(TAG, "Course '${course.title}' synced to Firestore successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to upload course to Firestore: ${e.message}")
      }
    }
  }

  /**
   * Deactivates and deletes a course from Firestore cloud database so it never reappears.
   */
  fun deleteCourseFromCloud(courseId: String) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    scope.launch {
      try {
        db.collection(COLLECTION_COURSES).document(courseId)
          .set(mapOf("isActive" to false), SetOptions.merge())
          .await()
        db.collection(COLLECTION_COURSES).document(courseId).delete().await()
        Log.d(TAG, "Course '$courseId' deactivated and deleted from Firestore")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete course from Firestore: ${e.message}")
      }
    }
  }

  /**
   * Starts listening to all student enrollments from Firestore across all devices.
   * Smartly reconciles with Room database so local primary key IDs are kept and statuses update in real time.
   * Automatically triggers correct push notifications depending on whether the current device belongs to the Admin or the Student.
   */
  fun startEnrollmentSyncListener(context: android.content.Context, courseDao: CourseDao) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    val sessionManager = SessionManager(context)
    try {
      db.collection(COLLECTION_ENROLLMENTS).addSnapshotListener { snapshots, error ->
        if (error != null) {
          Log.w(TAG, "Enrollments sync failed: ${error.message}")
          return@addSnapshotListener
        }

        if (snapshots != null && !snapshots.isEmpty) {
          scope.launch {
            val firestoreEnrollments = snapshots.documents.mapNotNull { doc ->
              try {
                EnrollmentEntity(
                  id = doc.getLong("id") ?: 0L,
                  username = doc.getString("username") ?: "Student",
                  userId = doc.getString("userId") ?: "",
                  userEmail = doc.getString("userEmail") ?: "",
                  courseId = doc.getLong("courseId") ?: 0L,
                  courseTitle = doc.getString("courseTitle") ?: "",
                  price = doc.getString("price") ?: "",
                  paymentMethod = doc.getString("paymentMethod") ?: "EasyPaisa",
                  transactionId = doc.getString("transactionId") ?: "",
                  enrolledAt = doc.getLong("enrolledAt") ?: System.currentTimeMillis(),
                  status = doc.getString("status") ?: "Pending",
                  receiptImage = doc.getString("receiptImage") ?: "",
                )
              } catch (e: Exception) {
                Log.e(TAG, "Error parsing enrollment doc: ${e.message}")
                null
              }
            }

            if (firestoreEnrollments.isNotEmpty()) {
              val localEnrollments = courseDao.getAllEnrollmentsList()
              val session = sessionManager.getCachedSession()
              val currentUserId = session?.userId
              val currentUserEmail = session?.email?.trim()?.lowercase()
              val currentUserIsAdmin = session?.isAdmin ?: false

              var syncedCount = 0
              for (cloud in firestoreEnrollments) {
                val matchedLocal = localEnrollments.firstOrNull { local ->
                  CloudSyncService.isSameEnrollment(cloud, local)
                }
                if (matchedLocal != null) {
                  // Existing enrollment check for status change notification
                  val statusChanged = !cloud.status.equals(matchedLocal.status, ignoreCase = true)
                  if (statusChanged) {
                    // Notify student (non-admin) when their enrollment status has changed (Approved or Rejected)
                    if (!currentUserIsAdmin && (cloud.userId == currentUserId || cloud.userEmail.trim().lowercase() == currentUserEmail)) {
                      com.example.util.NotificationHelper.showEnrollmentStatusUserNotification(
                        context = context,
                        username = cloud.username,
                        courseTitle = cloud.courseTitle,
                        isAccepted = cloud.status.equals("Accepted", ignoreCase = true),
                        enrollmentId = matchedLocal.id
                      )
                    }
                  }

                  val merged = cloud.copy(
                    id = matchedLocal.id,
                    userId = if (cloud.userId.isNotBlank()) cloud.userId else matchedLocal.userId,
                    userEmail = if (cloud.userEmail.isNotBlank()) cloud.userEmail else matchedLocal.userEmail,
                    username = if (cloud.username.isNotBlank()) cloud.username else matchedLocal.username,
                  )
                  courseDao.insertEnrollment(merged)
                  syncedCount++
                } else {
                  // Brand new enrollment request!
                  // Notify Admin of a new pending request from a student (not themselves)
                  if (currentUserIsAdmin && cloud.status.equals("Pending", ignoreCase = true) && cloud.userId != currentUserId) {
                    com.example.util.NotificationHelper.showNewEnrollmentAdminNotification(
                      context = context,
                      username = cloud.username,
                      userId = cloud.userId,
                      courseTitle = cloud.courseTitle,
                      amount = cloud.price,
                      enrollmentId = cloud.id
                    )
                  }

                  val newLocal = cloud.copy(id = 0)
                  courseDao.insertEnrollment(newLocal)
                  syncedCount++
                }
              }
              Log.d(TAG, "Reconciled $syncedCount enrollments from Firestore to Room DB")
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error starting enrollment listener: ${e.message}")
    }
  }

  /**
   * Uploads an enrollment request to Firestore cloud so Admins on other devices see it in real-time.
   */
  fun saveEnrollmentToCloud(enrollment: EnrollmentEntity) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    scope.launch {
      try {
        val docId = getEnrollmentDocId(enrollment)
        val map = hashMapOf(
          "id" to enrollment.id,
          "username" to enrollment.username,
          "userId" to enrollment.userId,
          "userEmail" to enrollment.userEmail,
          "courseId" to enrollment.courseId,
          "courseTitle" to enrollment.courseTitle,
          "price" to enrollment.price,
          "paymentMethod" to enrollment.paymentMethod,
          "transactionId" to enrollment.transactionId,
          "enrolledAt" to enrollment.enrolledAt,
          "status" to enrollment.status,
          "receiptImage" to enrollment.receiptImage,
        )

        db.collection(COLLECTION_ENROLLMENTS).document(docId)
          .set(map, SetOptions.merge())
          .await()
        Log.d(TAG, "Enrollment uploaded to Firestore with docId $docId (Status: ${enrollment.status})")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to upload enrollment to Firestore: ${e.message}")
      }
    }
  }

  /**
   * Updates an enrollment status in Firestore (e.g., 'Accepted' or 'Rejected' by Admin).
   */
  fun updateEnrollmentStatusInCloud(enrollment: EnrollmentEntity, newStatus: String) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    scope.launch {
      try {
        val docId = getEnrollmentDocId(enrollment)
        db.collection(COLLECTION_ENROLLMENTS).document(docId)
          .set(mapOf("status" to newStatus), SetOptions.merge())
          .await()
        Log.d(TAG, "Enrollment status updated to $newStatus in Firestore doc $docId")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to update enrollment status in Firestore: ${e.message}")
      }
    }
  }

  /**
   * Starts listening to student reviews across all courses worldwide.
   * Automatically updates Room DB so newly submitted reviews appear on all devices.
   */
  fun startReviewSyncListener(courseDao: CourseDao) {
    val db = firestore ?: return
    try {
      db.collection(COLLECTION_REVIEWS).addSnapshotListener { snapshots, error ->
        if (error != null) {
          Log.w(TAG, "Reviews sync listener failed: ${error.message}")
          return@addSnapshotListener
        }

        if (snapshots != null && !snapshots.isEmpty) {
          scope.launch {
            val reviews = snapshots.documents.mapNotNull { doc ->
              try {
                ReviewEntity(
                  id = doc.getLong("id") ?: (doc.id.hashCode().toLong().let { if (it < 0) -it else it }),
                  courseId = doc.getString("courseId") ?: "",
                  authorName = doc.getString("authorName") ?: "Student",
                  role = doc.getString("role") ?: "Student",
                  rating = (doc.getDouble("rating") ?: 5.0).toFloat(),
                  comment = doc.getString("comment") ?: "",
                  timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                )
              } catch (e: Exception) {
                Log.e(TAG, "Error parsing review doc: ${e.message}")
                null
              }
            }

            if (reviews.isNotEmpty()) {
              courseDao.insertReviews(reviews)
              Log.d(TAG, "Synced ${reviews.size} reviews from Firestore")
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error starting reviews listener: ${e.message}")
    }
  }

  /**
   * Pushes a new student review to Firestore cloud.
   */
  fun saveReviewToCloud(review: ReviewEntity) {
    val db = firestore ?: return
    scope.launch {
      try {
        val docId = "review_${review.courseId}_${review.authorName}_${review.timestamp}"
        val map = hashMapOf(
          "id" to review.id,
          "courseId" to review.courseId,
          "authorName" to review.authorName,
          "role" to review.role,
          "rating" to review.rating.toDouble(),
          "comment" to review.comment,
          "timestamp" to review.timestamp,
        )
        db.collection(COLLECTION_REVIEWS).document(docId)
          .set(map, SetOptions.merge())
          .await()
        Log.d(TAG, "Review uploaded to Firestore successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to upload review to Firestore: ${e.message}")
      }
    }
  }

  /**
   * One-shot fetch of all enrollments from Firebase Firestore.
   */
  suspend fun fetchAllEnrollmentsFromFirestore(courseDao: CourseDao): CloudSyncService.SyncResult {
    ensureFirebaseAuth()
    val db = firestore ?: return CloudSyncService.SyncResult(false, 0, "Firestore service not initialized")
    return try {
      val snapshot = db.collection(COLLECTION_ENROLLMENTS).get().await()
      val firestoreEnrollments = snapshot.documents.mapNotNull { doc ->
        try {
          EnrollmentEntity(
            id = doc.getLong("id") ?: 0L,
            username = doc.getString("username") ?: "Student",
            userId = doc.getString("userId") ?: "",
            userEmail = doc.getString("userEmail") ?: "",
            courseId = doc.getLong("courseId") ?: 0L,
            courseTitle = doc.getString("courseTitle") ?: "",
            price = doc.getString("price") ?: "",
            paymentMethod = doc.getString("paymentMethod") ?: "EasyPaisa",
            transactionId = doc.getString("transactionId") ?: "",
            enrolledAt = doc.getLong("enrolledAt") ?: System.currentTimeMillis(),
            status = doc.getString("status") ?: "Pending",
            receiptImage = doc.getString("receiptImage") ?: "",
          )
        } catch (e: Exception) {
          null
        }
      }

      if (firestoreEnrollments.isNotEmpty()) {
        val localEnrollments = courseDao.getAllEnrollmentsList()
        for (cloud in firestoreEnrollments) {
          val matchedLocal = localEnrollments.firstOrNull { local ->
            CloudSyncService.isSameEnrollment(cloud, local)
          }
          if (matchedLocal != null) {
            val merged = cloud.copy(
              id = matchedLocal.id,
              userId = if (cloud.userId.isNotBlank()) cloud.userId else matchedLocal.userId,
              userEmail = if (cloud.userEmail.isNotBlank()) cloud.userEmail else matchedLocal.userEmail,
              username = if (cloud.username.isNotBlank()) cloud.username else matchedLocal.username,
            )
            courseDao.insertEnrollment(merged)
          } else {
            val newLocal = cloud.copy(id = 0)
            courseDao.insertEnrollment(newLocal)
          }
        }
        Log.d(TAG, "One-shot fetch reconciled ${firestoreEnrollments.size} enrollments from Firestore")
      }
      CloudSyncService.SyncResult(true, firestoreEnrollments.size, null)
    } catch (e: Exception) {
      Log.e(TAG, "fetchAllEnrollmentsFromFirestore error: ${e.message}")
      val msg = when {
        e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ||
        e.message?.contains("disabled", ignoreCase = true) == true ->
          "Firebase Error: Firestore Database not enabled in project 'coursehub-6edbc'. Please click 'Create Database' in Firebase Console."
        e.message?.contains("offline", ignoreCase = true) == true ->
          "Offline: Could not connect to Google Firebase Firestore."
        else -> e.message ?: "Cloud sync failed"
      }
      CloudSyncService.SyncResult(false, 0, msg)
    }
  }

  /**
   * One-shot fetch of all courses from Firebase Firestore.
   */
  suspend fun fetchAllCoursesFromFirestore(courseDao: CourseDao) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    try {
      val snapshot = db.collection(COLLECTION_COURSES).get().await()
      val courses = mutableListOf<CourseEntity>()
      for (doc in snapshot.documents) {
        try {
          val courseId = doc.getString("id") ?: doc.id
          val isActive = doc.getBoolean("isActive") ?: true
          if (courseId in CloudSyncService.SYSTEM_COURSE_IDS || !isActive) {
            courseDao.deleteCourseById(courseId)
            continue
          }
          courses.add(
            CourseEntity(
              id = courseId,
              title = doc.getString("title") ?: "",
              category = doc.getString("category") ?: "General",
              subtitle = doc.getString("subtitle") ?: "",
              description = doc.getString("description") ?: "",
              instructor = doc.getString("instructor") ?: "",
              duration = doc.getString("duration") ?: "",
              level = doc.getString("level") ?: "All Levels",
              price = doc.getString("price") ?: "$0.00",
              rating = doc.getDouble("rating") ?: 5.0,
              reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
              enrolledStudents = doc.getString("enrolledStudents") ?: "1.2k+ students",
              colorHex = doc.getLong("colorHex") ?: 0xFF3B82F6,
              drawableName = doc.getString("drawableName") ?: "tech",
              accessUrl = doc.getString("accessUrl") ?: "https://meet.google.com",
              summary = doc.getString("summary") ?: "",
              objectives = doc.getString("objectives") ?: "",
              whatYouWillLearn = doc.getString("whatYouWillLearn") ?: "",
              imageUrls = doc.getString("imageUrls") ?: "",
              createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
              isActive = true,
            )
          )
        } catch (e: Exception) {
          Log.e(TAG, "Error parsing course doc: ${e.message}")
        }
      }
      if (courses.isNotEmpty()) {
        courseDao.insertCourses(courses)
        Log.d(TAG, "One-shot fetch loaded ${courses.size} active courses from Firestore")
      }
    } catch (e: Exception) {
      Log.e(TAG, "fetchAllCoursesFromFirestore error: ${e.message}")
    }
  }

  /**
   * One-shot fetch of all users from Firebase Firestore.
   */
  suspend fun fetchAllUsersFromFirestore(courseDao: CourseDao) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    try {
      val snapshot = db.collection(COLLECTION_USERS).get().await()
      val users = snapshot.documents.mapNotNull { doc ->
        try {
          UserEntity(
            username = doc.getString("username") ?: doc.id,
            passwordHash = doc.getString("passwordHash") ?: "",
            userId = doc.getString("userId") ?: "CH-${(10000..99999).random()}",
            email = doc.getString("email") ?: "",
            isAdmin = doc.getBoolean("isAdmin") ?: false,
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
          )
        } catch (e: Exception) {
          null
        }
      }
      if (users.isNotEmpty()) {
        courseDao.insertUsers(users)
        Log.d(TAG, "One-shot fetch loaded ${users.size} users from Firestore")
      }
    } catch (e: Exception) {
      Log.e(TAG, "fetchAllUsersFromFirestore error: ${e.message}")
    }
  }

  /**
   * Queries Firestore to find a user by username or email.
   */
  suspend fun findUserOnFirestore(identifier: String, courseDao: CourseDao? = null): UserEntity? = withContext(Dispatchers.IO) {
    ensureFirebaseAuth()
    val db = firestore ?: return@withContext null
    val clean = identifier.trim().lowercase()
    if (clean.isBlank()) return@withContext null

    try {
      // 1. Look up by document ID (lowercase username)
      val doc = db.collection(COLLECTION_USERS).document(clean).get().await()
      if (doc.exists()) {
        val originalUsername = doc.getString("username") ?: doc.id
        val originalEmail = doc.getString("email") ?: ""
        val user = UserEntity(
          username = if (!identifier.contains("@")) identifier.trim() else originalUsername,
          passwordHash = doc.getString("passwordHash") ?: "",
          userId = doc.getString("userId") ?: "CH-${(10000..99999).random()}",
          email = if (identifier.contains("@")) identifier.trim() else originalEmail,
          isAdmin = doc.getBoolean("isAdmin") ?: false,
          createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
        )
        courseDao?.insertUser(user)
        return@withContext user
      }

      // 2. Query by email_lowercase
      val snapshotLower = db.collection(COLLECTION_USERS)
        .whereEqualTo("email_lowercase", clean)
        .get()
        .await()
      if (!snapshotLower.isEmpty) {
        val doc2 = snapshotLower.documents.first()
        val originalUsername = doc2.getString("username") ?: doc2.id
        val originalEmail = doc2.getString("email") ?: ""
        val user = UserEntity(
          username = if (!identifier.contains("@")) identifier.trim() else originalUsername,
          passwordHash = doc2.getString("passwordHash") ?: "",
          userId = doc2.getString("userId") ?: "CH-${(10000..99999).random()}",
          email = if (identifier.contains("@")) identifier.trim() else originalEmail,
          isAdmin = doc2.getBoolean("isAdmin") ?: false,
          createdAt = doc2.getLong("createdAt") ?: System.currentTimeMillis(),
        )
        courseDao?.insertUser(user)
        return@withContext user
      }

      // 3. Fallback: Query by email (exact case-sensitive match)
      val snapshotExact = db.collection(COLLECTION_USERS)
        .whereEqualTo("email", identifier.trim())
        .get()
        .await()
      if (!snapshotExact.isEmpty) {
        val doc2 = snapshotExact.documents.first()
        val originalUsername = doc2.getString("username") ?: doc2.id
        val originalEmail = doc2.getString("email") ?: ""
        val user = UserEntity(
          username = if (!identifier.contains("@")) identifier.trim() else originalUsername,
          passwordHash = doc2.getString("passwordHash") ?: "",
          userId = doc2.getString("userId") ?: "CH-${(10000..99999).random()}",
          email = if (identifier.contains("@")) identifier.trim() else originalEmail,
          isAdmin = doc2.getBoolean("isAdmin") ?: false,
          createdAt = doc2.getLong("createdAt") ?: System.currentTimeMillis(),
        )
        courseDao?.insertUser(user)
        return@withContext user
      }

      // 4. Ultra-reliable fallback: Fetch all users and check in-memory for case-insensitive email match
      val snapshotAll = db.collection(COLLECTION_USERS).get().await()
      val matchedDoc = snapshotAll.documents.firstOrNull { d ->
        val userEmail = d.getString("email").orEmpty().trim().lowercase()
        userEmail == clean
      }
      if (matchedDoc != null) {
        val originalUsername = matchedDoc.getString("username") ?: matchedDoc.id
        val originalEmail = matchedDoc.getString("email") ?: ""
        val user = UserEntity(
          username = if (!identifier.contains("@")) identifier.trim() else originalUsername,
          passwordHash = matchedDoc.getString("passwordHash") ?: "",
          userId = matchedDoc.getString("userId") ?: "CH-${(10000..99999).random()}",
          email = if (identifier.contains("@")) identifier.trim() else originalEmail,
          isAdmin = matchedDoc.getBoolean("isAdmin") ?: false,
          createdAt = matchedDoc.getLong("createdAt") ?: System.currentTimeMillis(),
        )
        courseDao?.insertUser(user)
        return@withContext user
      }
    } catch (e: Exception) {
      Log.e(TAG, "findUserOnFirestore error: ${e.message}")
    }
    null
  }

  /**
   * Deletes a review from Firestore cloud database by matching courseId, authorName, and optional timestamp.
   */
  fun deleteReviewFromCloud(courseId: String, reviewAuthor: String, timestamp: Long? = null) {
    ensureFirebaseAuth()
    val db = firestore ?: return
    scope.launch {
      try {
        if (timestamp != null) {
          val exactDocId = "review_${courseId}_${reviewAuthor}_${timestamp}"
          db.collection(COLLECTION_REVIEWS).document(exactDocId).delete().await()
        }
        val legacyDocId = "review_${courseId}_${reviewAuthor}_"
        db.collection(COLLECTION_REVIEWS).document(legacyDocId).delete().await()

        val matchingDocs = db.collection(COLLECTION_REVIEWS)
          .whereEqualTo("courseId", courseId)
          .get()
          .await()
        for (doc in matchingDocs.documents) {
          val docAuthor = doc.getString("authorName").orEmpty()
          val docTimestamp = doc.getLong("timestamp")
          if (docAuthor.equals(reviewAuthor, ignoreCase = true) && (timestamp == null || docTimestamp == timestamp)) {
            db.collection(COLLECTION_REVIEWS).document(doc.id).delete().await()
          }
        }
        Log.d(TAG, "Review by '$reviewAuthor' for course '$courseId' deleted from Firestore")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to delete review from Firestore: ${e.message}")
      }
    }
  }
}

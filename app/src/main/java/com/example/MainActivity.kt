package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.SessionManager
import com.example.navigation.ScreenDestination
import com.example.navigation.rememberAppNavigationState
import com.example.ui.AccountScreen
import com.example.ui.AdminScreen
import com.example.ui.AuthScreen
import com.example.ui.CourseLearningScreen
import com.example.ui.LandingScreen
import com.example.ui.PaymentScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      // Permission result handled
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize notification channels
    NotificationHelper.createNotificationChannels(applicationContext)

    // Request notification permission on first launch (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    setContent {
      MyApplicationTheme {
        val scope = rememberCoroutineScope()
        val database = remember { AppDatabase.getDatabase(applicationContext, scope) }
        val courseDao = database.courseDao()
        val sessionManager = remember { SessionManager(applicationContext) }

        // Start real-time Firestore synchronization for Users, Courses, Enrollments & Reviews across all phones worldwide
        LaunchedEffect(Unit) {
          com.example.data.FirebaseFirestoreService.ensureFirebaseAuth()
          com.example.data.FirebaseFirestoreService.startUserSyncListener(courseDao)
          com.example.data.FirebaseFirestoreService.startCourseSyncListener(courseDao)
          com.example.data.FirebaseFirestoreService.startEnrollmentSyncListener(applicationContext, courseDao)
          com.example.data.FirebaseFirestoreService.startReviewSyncListener(courseDao)
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // Immediate one-shot fetch from Firebase Firestore
            com.example.data.FirebaseFirestoreService.fetchAllCoursesFromFirestore(courseDao)
            com.example.data.FirebaseFirestoreService.fetchAllEnrollmentsFromFirestore(courseDao)
            // Secondary fallback sync
            com.example.data.CloudSyncService.syncAllCoursesFromCloud(courseDao)
            com.example.data.CloudSyncService.syncAllEnrollmentsFromCloud(courseDao)
          }
        }

        val navigationState = rememberAppNavigationState(
          initialSession = remember { sessionManager.getCachedSession() }
        )

        // Gracefully handle hardware/system back gestures
        BackHandler(enabled = navigationState.currentDestination != ScreenDestination.Landing) {
          if (!navigationState.popBack()) {
            navigationState.navigateToLanding()
          }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
          Crossfade(
            targetState = navigationState.currentDestination,
            label = "screen_transition",
          ) { destination ->
            when (destination) {
              is ScreenDestination.Landing -> {
                LandingScreen(
                  userSession = navigationState.userSession,
                  courseDao = courseDao,
                  onOpenAuth = { navigationState.navigateToAuth() },
                  onOpenAccount = { navigationState.navigateToAccount() },
                  onOpenAdmin = { navigationState.navigateToAdmin() },
                  onSignOut = {
                    try {
                      com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    } catch (e: Exception) {
                      // Ignored if Firebase is not initialized
                    }
                    sessionManager.clearSession()
                    navigationState.signOut()
                  },
                  onEnrollCourse = { course ->
                    navigationState.navigateToPayment(course)
                  },
                  onContinueCourse = { course ->
                    navigationState.navigateToCourseLearning(course)
                  },
                )
              }
              is ScreenDestination.Account -> {
                val session = navigationState.userSession
                if (session != null) {
                  AccountScreen(
                    userSession = session,
                    courseDao = courseDao,
                    onBack = {
                      if (!navigationState.popBack()) {
                        navigationState.navigateToLanding()
                      }
                    },
                    onSignOut = {
                      try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                      } catch (e: Exception) {
                        // Ignored if Firebase is not initialized
                      }
                      sessionManager.clearSession()
                      navigationState.signOut()
                    },
                    onOpenAdmin = { navigationState.navigateToAdmin() },
                    onBrowseCourses = { navigationState.navigateToLanding() },
                    onContinueCourse = { course ->
                      navigationState.navigateToCourseLearning(course)
                    },
                  )
                } else {
                  navigationState.navigateToAuth()
                }
              }
              is ScreenDestination.Payment -> {
                PaymentScreen(
                  initialCourse = destination.course,
                  userSession = navigationState.userSession,
                  courseDao = courseDao,
                  onBack = {
                    if (!navigationState.popBack()) {
                      navigationState.navigateToLanding()
                    }
                  },
                  onOpenAuth = { navigationState.navigateToAuth() },
                  onSubmittedSuccess = {
                    navigationState.navigateToLanding()
                  },
                )
              }
              is ScreenDestination.Auth -> {
                AuthScreen(
                  courseDao = courseDao,
                  onAuthSuccess = { session ->
                    sessionManager.saveSession(session)
                    navigationState.onAuthSuccessful(session)
                  },
                  onBackToLanding = {
                    if (!navigationState.popBack()) {
                      navigationState.navigateToLanding()
                    }
                  },
                )
              }
              is ScreenDestination.Admin -> {
                val userSession = navigationState.userSession
                if (userSession?.isAdmin == true) {
                  AdminScreen(
                    userSession = userSession,
                    courseDao = courseDao,
                    onExitAdmin = { navigationState.navigateToLanding() },
                  )
                } else {
                  navigationState.navigateToAuth()
                }
              }
              is ScreenDestination.CourseLearning -> {
                CourseLearningScreen(
                  course = destination.course,
                  courseDao = courseDao,
                  onBack = {
                    if (!navigationState.popBack()) {
                      navigationState.navigateToLanding()
                    }
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}

package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.CourseEntity
import com.example.data.UserSession

sealed class ScreenDestination {
  data object Landing : ScreenDestination()
  data object Auth : ScreenDestination()
  data object Admin : ScreenDestination()
  data object Account : ScreenDestination()
  data class Payment(val course: CourseEntity? = null) : ScreenDestination()
  data class CourseLearning(val course: CourseEntity) : ScreenDestination()
}

@Stable
class AppNavigationState(
  initialDestination: ScreenDestination = ScreenDestination.Landing,
  initialSession: UserSession? = null,
) {
  var currentDestination by mutableStateOf(initialDestination)
    private set

  var userSession by mutableStateOf<UserSession?>(initialSession)
    private set

  private val backStack = mutableStateListOf<ScreenDestination>()

  init {
    backStack.add(initialDestination)
  }

  fun navigateTo(destination: ScreenDestination) {
    if (destination != currentDestination) {
      backStack.add(destination)
      currentDestination = destination
    }
  }

  fun popBack(): Boolean {
    if (backStack.size > 1) {
      backStack.removeAt(backStack.lastIndex)
      currentDestination = backStack.last()
      return true
    }
    return false
  }

  fun navigateToAuth() {
    navigateTo(ScreenDestination.Auth)
  }

  fun navigateToPayment(course: CourseEntity? = null) {
    navigateTo(ScreenDestination.Payment(course))
  }

  fun navigateToCourseLearning(course: CourseEntity) {
    navigateTo(ScreenDestination.CourseLearning(course))
  }

  fun navigateToAdmin() {
    if (userSession?.isAdmin == true) {
      navigateTo(ScreenDestination.Admin)
    } else {
      navigateTo(ScreenDestination.Auth)
    }
  }

  fun navigateToAccount() {
    if (userSession != null) {
      navigateTo(ScreenDestination.Account)
    } else {
      navigateTo(ScreenDestination.Auth)
    }
  }

  fun navigateToLanding() {
    backStack.clear()
    backStack.add(ScreenDestination.Landing)
    currentDestination = ScreenDestination.Landing
  }

  fun onAuthSuccessful(session: UserSession) {
    userSession = session
    if (session.isAdmin) {
      backStack.clear()
      backStack.add(ScreenDestination.Landing)
      backStack.add(ScreenDestination.Admin)
      currentDestination = ScreenDestination.Admin
    } else {
      navigateToLanding()
    }
  }

  fun signOut() {
    userSession = null
    navigateToLanding()
  }
}

@Composable
fun rememberAppNavigationState(
  initialDestination: ScreenDestination = ScreenDestination.Landing,
  initialSession: UserSession? = null,
): AppNavigationState {
  return remember {
    AppNavigationState(initialDestination, initialSession)
  }
}

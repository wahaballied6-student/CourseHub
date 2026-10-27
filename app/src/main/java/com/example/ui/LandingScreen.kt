package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CourseDao
import com.example.data.CourseEntity
import com.example.data.EnrollmentEntity
import com.example.data.RecentSearchEntity
import com.example.data.UserSession
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassAmber
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassEmeraldDot
import com.example.ui.theme.GlassEmeraldOrb
import com.example.ui.theme.GlassIndigoOrb
import com.example.ui.theme.GlassPrimaryBlue
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceHighlight
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.GlassVioletOrb
import com.example.util.AppImage
import com.example.util.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect

data class FeatureItem(
  val title: String,
  val description: String,
  val icon: ImageVector,
  val iconTint: Color,
)

data class StatItem(
  val label: String,
  val value: String,
  val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
  userSession: UserSession? = null,
  courseDao: CourseDao,
  onOpenAuth: () -> Unit = {},
  onOpenAccount: () -> Unit = {},
  onOpenAdmin: () -> Unit = {},
  onSignOut: () -> Unit = {},
  onEnrollCourse: (CourseEntity) -> Unit = {},
  onContinueCourse: (CourseEntity) -> Unit = {},
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  var selectedFilter by remember { mutableStateOf("All Topics") }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCourseForDetail by remember { mutableStateOf<CourseEntity?>(null) }
  var isManualSyncing by remember { mutableStateOf(false) }
  val currentUsername = userSession?.username ?: ""

  // Automatically sync incoming courses and user enrollments from cloud on screen appearance and poll periodically
  LaunchedEffect(currentUsername) {
    while (isActive) {
      withContext(Dispatchers.IO) {
        com.example.data.CloudSyncService.syncAllCoursesFromCloud(courseDao)
        com.example.data.CloudSyncService.syncAllEnrollmentsFromCloud(courseDao)
      }
      delay(5000) // Poll every 5 seconds so user sees course updates and approval changes
    }
  }

  // Live Room Database Flows
  val dbCourses by courseDao.getAllCourses().collectAsState(initial = emptyList())
  val recentSearches by courseDao.getRecentSearches().collectAsState(initial = emptyList())
  val pendingCount by courseDao.getPendingEnrollmentsCount().collectAsState(initial = 0)

  // Map of course title / id -> enrollment status ("Accepted", "Pending", "Rejected")
  val currentUserId = userSession?.userId ?: ""
  val currentUserEmail = userSession?.email ?: ""
  val allEnrollmentsList by courseDao.getAllEnrollments().collectAsState(initial = emptyList())
  val userEnrollments = remember(allEnrollmentsList, currentUsername, currentUserId, currentUserEmail) {
    if (currentUsername.isNotBlank() || currentUserId.isNotBlank() || currentUserEmail.isNotBlank()) {
      val rawUserList = allEnrollmentsList.filter { enrollment ->
        (currentUsername.isNotBlank() && enrollment.username.equals(currentUsername, ignoreCase = true)) ||
          (currentUserId.isNotBlank() && enrollment.userId.equals(currentUserId, ignoreCase = true)) ||
          (currentUserEmail.isNotBlank() && enrollment.userEmail.isNotBlank() && enrollment.userEmail.equals(currentUserEmail, ignoreCase = true))
      }
      // Deduplicate by course title, giving priority to Accepted/Active over Pending
      rawUserList.groupBy { it.courseTitle.lowercase() }.values.map { courseEntries ->
        courseEntries.firstOrNull { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Active", ignoreCase = true) }
          ?: courseEntries.firstOrNull { it.status.equals("Pending", ignoreCase = true) }
          ?: courseEntries.first()
      }
    } else {
      emptyList()
    }
  }

  fun getEnrollmentStatusForCourse(course: CourseEntity): String? {
    val matched = userEnrollments.filter { enrollment ->
      enrollment.courseTitle.equals(course.title, ignoreCase = true) ||
        (enrollment.courseId != 0L && enrollment.courseId == course.id.hashCode().toLong())
    }
    if (matched.any { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Active", ignoreCase = true) }) {
      return "Accepted"
    }
    return matched.firstOrNull()?.status
  }

  val handleSearchSubmission = { term: String ->
    val clean = term.trim()
    if (clean.isNotBlank()) {
      coroutineScope.launch(Dispatchers.IO) {
        courseDao.insertRecentSearch(RecentSearchEntity(query = clean))
      }
      coroutineScope.launch {
        snackbarHostState.showSnackbar("Searched for '$clean' (cached in Room)")
      }
    }
    searchQuery = clean
  }

  val filterOptions = listOf("All Topics", "AI & ML", "Software Dev", "UI/UX Design", "Leadership")

  val filteredCourses = remember(dbCourses, searchQuery, selectedFilter) {
    dbCourses.filter { course ->
      val matchesSearch =
        if (searchQuery.isBlank()) {
          true
        } else {
          course.title.contains(searchQuery, ignoreCase = true) ||
            course.subtitle.contains(searchQuery, ignoreCase = true) ||
            course.description.contains(searchQuery, ignoreCase = true) ||
            course.instructor.contains(searchQuery, ignoreCase = true)
        }

      val matchesFilter =
        when (selectedFilter) {
          "All Topics" -> true
          "AI & ML" -> course.category.contains("AI", ignoreCase = true) || course.title.contains("AI", ignoreCase = true)
          "Software Dev" -> course.category.contains("Software", ignoreCase = true) || course.category.contains("AI", ignoreCase = true) || course.title.contains("Software", ignoreCase = true)
          "UI/UX Design" -> course.category.contains("Design", ignoreCase = true) || course.title.contains("Design", ignoreCase = true)
          "Leadership" -> course.category.contains("Leadership", ignoreCase = true) || course.title.contains("Leadership", ignoreCase = true)
          else -> true
        }

      matchesSearch && matchesFilter
    }
  }

  val stats = remember {
    listOf(
      StatItem("Active Learners", "45k+", Icons.Default.Group),
      StatItem("Expert Courses", "350+", Icons.Default.MenuBook),
      StatItem("Satisfaction", "4.9★", Icons.Default.Star),
      StatItem("Career Boost", "94%", Icons.Default.TrendingUp),
    )
  }

  val features = remember {
    listOf(
      FeatureItem(
        title = "Hands-on Project Mastery",
        description = "Build portfolio-ready applications with modern toolchains and automated test suites.",
        icon = Icons.Default.Code,
        iconTint = GlassAccentBlue,
      ),
      FeatureItem(
        title = "Industry-Recognized Certification",
        description = "Earn verified digital credentials to showcase your expertise on professional networks.",
        icon = Icons.Default.CardMembership,
        iconTint = GlassVioletOrb,
      ),
      FeatureItem(
        title = "Personalized Learning Tracks",
        description = "Structured milestones designed to take you from foundational concepts to advanced engineering.",
        icon = Icons.Default.AutoAwesome,
        iconTint = GlassEmeraldDot,
      ),
    )
  }

  Scaffold(
    containerColor = GlassBackground,
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(36.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(
                    Brush.linearGradient(
                      listOf(GlassPrimaryBlue, Color(0xFF8B5CF6)),
                    ),
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
              )
            }
            Column {
              Text(
                text = "CourseHub",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary,
              )
              Text(
                text = "Next-Gen Tech Academy",
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary,
              )
            }
          }
        },
        actions = {
          // Admin shortcut button if admin
          if (userSession?.isAdmin == true) {
            IconButton(
              onClick = onOpenAdmin,
              modifier =
                Modifier
                  .clip(CircleShape)
                  .background(if (pendingCount > 0) Color(0x33F59E0B) else Color(0x333B82F6))
                  .border(BorderStroke(1.dp, if (pendingCount > 0) Color(0xFFF59E0B) else Color(0x6660A5FA)), CircleShape)
                  .testTag("admin_shortcut_btn"),
            ) {
              Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                  imageVector = if (pendingCount > 0) Icons.Default.Notifications else Icons.Default.AdminPanelSettings,
                  contentDescription = "Admin Panel",
                  tint = if (pendingCount > 0) Color(0xFFFBBF24) else Color(0xFF60A5FA),
                  modifier = Modifier.size(20.dp),
                )
                if (pendingCount > 0) {
                  Surface(
                    color = Color(0xFFEF4444),
                    shape = CircleShape,
                    modifier = Modifier.size(14.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = "$pendingCount",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }
            }
          }

          // Manual Cloud Sync Button
          IconButton(
            onClick = {
              if (!isManualSyncing) {
                isManualSyncing = true
                coroutineScope.launch {
                  val syncRes = withContext(Dispatchers.IO) {
                    com.example.data.CloudSyncService.syncAllCoursesFromCloud(courseDao)
                    com.example.data.CloudSyncService.syncAllEnrollmentsFromCloud(courseDao)
                  }
                  isManualSyncing = false
                  if (syncRes.isSuccess) {
                    snackbarHostState.showSnackbar("Synced with Cloud: ${syncRes.count} enrollments verified")
                  } else {
                    snackbarHostState.showSnackbar(syncRes.errorMessage ?: "Cloud sync failed")
                  }
                }
              }
            },
            modifier = Modifier.testTag("student_cloud_sync_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync Cloud",
              tint = if (isManualSyncing) Color(0xFF38BDF8) else GlassTextSecondary,
              modifier = Modifier.size(20.dp),
            )
          }

          // Sign In / Profile Button
          if (userSession == null) {
            Surface(
              color = Color(0x403B82F6),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, Color(0x6660A5FA)),
              modifier =
                Modifier
                  .clickable { onOpenAuth() }
                  .padding(end = 4.dp)
                  .testTag("top_sign_in_btn"),
            ) {
              Text(
                text = "Sign In",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          } else {
            IconButton(
              onClick = onOpenAccount,
              modifier =
                Modifier
                  .clip(CircleShape)
                  .background(Color(0x26FFFFFF))
                  .testTag("user_avatar_btn"),
            ) {
              Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = GlassAccentBlue)
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0x4D0F172A),
          ),
      )
    },
    bottomBar = {
      FrostedGlassBottomNav(
        userSession = userSession,
        onOpenAuth = onOpenAuth,
        onOpenAccount = onOpenAccount,
        onTabSelect = { tab ->
          coroutineScope.launch {
            snackbarHostState.showSnackbar("Switched to $tab")
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { paddingValues ->
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues),
      contentPadding = PaddingValues(bottom = 24.dp),
    ) {
      // 0. Top Frosted Glass Search Bar with Room-backed Recent Searches Dropdown
      item {
        FrostedSearchBar(
          query = searchQuery,
          recentSearches = recentSearches,
          onQueryChange = { searchQuery = it },
          onSearchTriggered = { query ->
            handleSearchSubmission(query)
          },
          onDeleteRecentSearch = { q ->
            coroutineScope.launch(Dispatchers.IO) {
              courseDao.deleteRecentSearch(q)
            }
          },
          onClearAllRecentSearches = {
            coroutineScope.launch(Dispatchers.IO) {
              courseDao.clearAllRecentSearches()
            }
          },
        )
      }

      // Admin Access Notification Banner if logged in as Admin
      if (userSession?.isAdmin == true) {
        item {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                  Brush.horizontalGradient(
                    listOf(Color(0x333B82F6), Color(0x338B5CF6)),
                  ),
                )
                .border(BorderStroke(1.dp, Color(0x6660A5FA)), RoundedCornerShape(18.dp))
                .clickable { onOpenAdmin() }
                .padding(14.dp)
                .testTag("admin_banner_cta"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.AdminPanelSettings,
                  contentDescription = null,
                  tint = Color(0xFF60A5FA),
                  modifier = Modifier.size(24.dp),
                )
                Column {
                  Text(
                    text = "Admin Access Active (${userSession.username})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                  )
                  Text(
                    text = "Tap to open Room DB Course Manager",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF93C5FD),
                  )
                }
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
              )
            }
          }
        }
      }

      // 1. Hero Section
      item {
        FrostedHeroSection(
          onExploreClick = {
            coroutineScope.launch {
              snackbarHostState.showSnackbar("Browsing curated curriculum...")
            }
          },
          onBrowseClick = {
            selectedFilter = "AI & ML"
          },
        )
      }

      // 2. Metrics / Stat Banner
      item {
        FrostedStatsBanner(stats = stats)
      }

      // 3. Filter Chips
      item {
        CourseFilterChips(
          filterOptions = filterOptions,
          selectedFilter = selectedFilter,
          onFilterSelected = { selectedFilter = it },
        )
      }

      // 4. Section Heading for Featured Categories
      item {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = if (selectedFilter == "All Topics") "Available Courses (${filteredCourses.size})" else "$selectedFilter Courses (${filteredCourses.size})",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = GlassTextPrimary,
            )
            Text(
              text = "Live synced with Room database",
              style = MaterialTheme.typography.bodySmall,
              color = GlassAccentBlue,
            )
          }

          if (userSession?.isAdmin == true) {
            Surface(
              color = Color(0x333B82F6),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.clickable { onOpenAdmin() },
            ) {
              Text(
                text = "+ Add in Admin",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF93C5FD),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      // 5. Course Cards
      if (filteredCourses.isEmpty()) {
        item {
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
            shape = RoundedCornerShape(16.dp),
          ) {
            Column(
              modifier = Modifier.padding(24.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "No matching courses in Room DB",
                style = MaterialTheme.typography.titleMedium,
                color = GlassTextPrimary,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Try clearing your search or filter criteria.",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary,
              )
            }
          }
        }
      } else {
        items(filteredCourses, key = { it.id }) { course ->
          val status = getEnrollmentStatusForCourse(course)
          CourseCategoryCard(
            course = course,
            enrollmentStatus = status,
            onCardClick = {
              selectedCourseForDetail = course
            },
            onEnrollClick = {
              if (status == "Accepted") {
                onContinueCourse(course)
              } else {
                onEnrollCourse(course)
              }
            },
          )
        }
      }

      // 6. Features Grid Section
      item {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Why Learn With CourseHub",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = GlassTextPrimary,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }

      items(features) { feature ->
        FeatureGlassCard(feature = feature)
      }

      // 7. Footer CTA
      item {
        FrostedFooterCTA(
          onJoinNowClick = onOpenAuth,
        )
      }
    }
  }

  // Course Detail BottomSheet
  if (selectedCourseForDetail != null) {
    val selectedCourse = selectedCourseForDetail!!
    val detailStatus = getEnrollmentStatusForCourse(selectedCourse)
    CourseDetailModalSheet(
      course = selectedCourse,
      courseDao = courseDao,
      enrollmentStatus = detailStatus,
      onDismiss = { selectedCourseForDetail = null },
      onEnroll = { course ->
        selectedCourseForDetail = null
        if (detailStatus == "Accepted") {
          onContinueCourse(course)
        } else {
          onEnrollCourse(course)
        }
      },
    )
  }
}

/**
 * Ambient background orbs providing soft atmospheric gradient glows
 */
@Composable
fun AmbientGlowBackground() {
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier =
        Modifier
          .size(320.dp)
          .align(Alignment.TopEnd)
          .offset(x = 80.dp, y = (-40).dp)
          .background(
            Brush.radialGradient(
              colors = listOf(GlassIndigoOrb.copy(alpha = 0.45f), Color.Transparent),
            ),
            CircleShape,
          ),
    )

    Box(
      modifier =
        Modifier
          .size(260.dp)
          .align(Alignment.CenterStart)
          .offset(x = (-60).dp, y = (-80).dp)
          .background(
            Brush.radialGradient(
              colors = listOf(GlassAccentBlue.copy(alpha = 0.35f), Color.Transparent),
            ),
            CircleShape,
          ),
    )

    Box(
      modifier =
        Modifier
          .size(340.dp)
          .align(Alignment.BottomStart)
          .offset(x = (-80).dp, y = 60.dp)
          .background(
            Brush.radialGradient(
              colors = listOf(GlassEmeraldOrb.copy(alpha = 0.30f), Color.Transparent),
            ),
            CircleShape,
          ),
    )
  }
}

@Composable
fun FrostedHeroSection(
  onExploreClick: () -> Unit,
  onBrowseClick: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(1200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "pulseAlpha",
  )

  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(24.dp))
        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(24.dp)),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_hero_courses),
      contentDescription = "Curated Learning Experience",
      modifier =
        Modifier
          .fillMaxWidth()
          .height(350.dp),
      contentScale = ContentScale.Crop,
    )

    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(350.dp)
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0x600F172A),
                  Color(0xB30F172A),
                  Color(0xE60F172A),
                ),
            ),
          ),
    )

    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(350.dp)
          .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        color = Color(0x333B82F6),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, Color(0x6660A5FA)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(GlassEmeraldDot.copy(alpha = pulseAlpha)),
          )
          Text(
            text = "EXECUTIVE CURRICULUM",
            style = MaterialTheme.typography.labelSmall,
            color = GlassAccentBlue,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
          )
        }
      }

      Column {
        Text(
          text =
            buildAnnotatedString {
              append("Architect Your Future in ")
              withStyle(SpanStyle(color = GlassAccentBlue, fontWeight = FontWeight.ExtraBold)) {
                append("AI & Tech")
              }
            },
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = GlassTextPrimary,
          lineHeight = 34.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Accelerate your mastery with production architectures, industry mentors, and certified tracks.",
          style = MaterialTheme.typography.bodyMedium,
          color = GlassTextSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onExploreClick,
            modifier =
              Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = GlassPrimaryBlue)
                .testTag("hero_explore_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = GlassPrimaryBlue,
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(
              text = "Explore Courses",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }

          OutlinedButton(
            onClick = onBrowseClick,
            modifier =
              Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("hero_browse_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text(
              text = "AI Tracks",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
fun FrostedStatsBanner(stats: List<StatItem>) {
  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .border(BorderStroke(1.dp, GlassBorderSubtle), RoundedCornerShape(18.dp)),
    colors = CardDefaults.cardColors(containerColor = GlassSurface),
    shape = RoundedCornerShape(18.dp),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      stats.forEach { stat ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 4.dp),
        ) {
          Text(
            text = stat.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = GlassTextPrimary,
          )
          Text(
            text = stat.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = GlassTextMuted,
          )
        }
      }
    }
  }
}

@Composable
fun CourseFilterChips(
  filterOptions: List<String>,
  selectedFilter: String,
  onFilterSelected: (String) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    filterOptions.forEach { filter ->
      val isSelected = filter == selectedFilter
      FilterChip(
        selected = isSelected,
        onClick = { onFilterSelected(filter) },
        label = {
          Text(
            text = filter,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          )
        },
        colors =
          FilterChipDefaults.filterChipColors(
            containerColor = GlassSurface,
            selectedContainerColor = GlassPrimaryBlue,
            labelColor = GlassTextSecondary,
            selectedLabelColor = Color.White,
          ),
        border =
          BorderStroke(
            width = 1.dp,
            color = if (isSelected) GlassAccentBlue else GlassBorder,
          ),
        shape = RoundedCornerShape(100.dp),
        modifier = Modifier.testTag("filter_chip_$filter"),
      )
    }
  }
}

@Composable
fun CourseCategoryCard(
  course: CourseEntity,
  enrollmentStatus: String? = null,
  onCardClick: () -> Unit,
  onEnrollClick: (CourseEntity) -> Unit = {},
) {
  val imageRes =
    when (course.drawableName) {
      "img_course_tech" -> R.drawable.img_course_tech
      "img_course_design" -> R.drawable.img_course_design
      "img_course_business" -> R.drawable.img_course_business
      else -> R.drawable.img_course_tech
    }

  val primaryImageModel = remember(course.imageUrls, course.drawableName) {
    val tokens = ImageHelper.splitImageUrls(course.imageUrls)
    tokens.firstOrNull()?.ifBlank { null } ?: course.drawableName
  }

  val categoryIcon =
    when (course.category) {
      "AI & ML" -> Icons.Default.Psychology
      "UI/UX Design" -> Icons.Default.Palette
      "Leadership" -> Icons.Default.TrendingUp
      else -> Icons.Default.Code
    }

  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(20.dp))
        .clickable { onCardClick() }
        .testTag("course_card_${course.id}"),
    colors =
      CardDefaults.cardColors(
        containerColor = GlassSurface,
      ),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(150.dp),
      ) {
        AppImage(
          model = primaryImageModel,
          contentDescription = course.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          fallbackResId = imageRes,
        )

        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color(0xCC0F172A)),
                ),
              ),
        )

        Surface(
          color = Color(0x990F172A),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, GlassBorder),
          modifier =
            Modifier
              .align(Alignment.TopEnd)
              .padding(12.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Rating",
              tint = GlassAmber,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = String.format("%.2f", course.rating),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          }
        }

        Surface(
          color = Color(course.colorHex).copy(alpha = 0.25f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Color(course.colorHex).copy(alpha = 0.5f)),
          modifier =
            Modifier
              .align(Alignment.BottomStart)
              .padding(12.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = categoryIcon,
              contentDescription = null,
              tint = Color(course.colorHex),
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = course.category,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(course.colorHex),
            )
          }
        }
      }

      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = course.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = GlassTextPrimary,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = course.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = GlassAccentBlue,
          fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = course.description,
          style = MaterialTheme.typography.bodySmall,
          color = GlassTextSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "⏱ ${course.duration}",
              style = MaterialTheme.typography.labelSmall,
              color = GlassTextMuted,
            )
            Surface(
              color = Color(0x3310B981),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color(0x6610B981)),
            ) {
              Text(
                text = course.price,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          when (enrollmentStatus) {
            "Accepted" -> {
              Button(
                onClick = { onEnrollClick(course) },
                modifier = Modifier.height(36.dp).testTag("course_card_continue_btn_${course.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
              ) {
                Text(
                  text = "Continue Course ▶",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
              }
            }
            "Pending" -> {
              OutlinedButton(
                onClick = { onEnrollClick(course) },
                modifier = Modifier.height(36.dp).testTag("course_card_pending_btn_${course.id}"),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x22F59E0B)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
              ) {
                Text(
                  text = "Pending Approval ⏳",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFFFBBF24),
                )
              }
            }
            else -> {
              Button(
                onClick = { onEnrollClick(course) },
                modifier = Modifier.height(36.dp).testTag("course_card_enroll_btn_${course.id}"),
                colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
              ) {
                Text(
                  text = "Enroll Now",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun FeatureGlassCard(feature: FeatureItem) {
  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .border(BorderStroke(1.dp, GlassBorderSubtle), RoundedCornerShape(16.dp)),
    colors = CardDefaults.cardColors(containerColor = GlassSurface),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(feature.iconTint.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, feature.iconTint.copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = feature.icon,
          contentDescription = null,
          tint = feature.iconTint,
          modifier = Modifier.size(24.dp),
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = feature.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = GlassTextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = feature.description,
          style = MaterialTheme.typography.bodySmall,
          color = GlassTextSecondary,
        )
      }
    }
  }
}

@Composable
fun FrostedFooterCTA(onJoinNowClick: () -> Unit) {
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 20.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(
            listOf(GlassSurfaceHighlight, GlassSurfaceDark),
          ),
        )
        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(24.dp))
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "Start Learning Today",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = GlassTextPrimary,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Join thousands of engineers mastering scalable AI and design architectures.",
        style = MaterialTheme.typography.bodyMedium,
        color = GlassTextSecondary,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onJoinNowClick,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = GlassPrimaryBlue)
            .testTag("footer_join_now_button"),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = GlassPrimaryBlue,
            contentColor = Color.White,
          ),
        shape = RoundedCornerShape(14.dp),
      ) {
        Text(
          text = "Get Started Now",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
fun FrostedSearchBar(
  query: String,
  recentSearches: List<RecentSearchEntity>,
  onQueryChange: (String) -> Unit,
  onSearchTriggered: (String) -> Unit,
  onDeleteRecentSearch: (String) -> Unit,
  onClearAllRecentSearches: () -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var isFocused by remember { mutableStateOf(false) }
  var showDropdown by remember { mutableStateOf(false) }

  val popularSuggestions = remember {
    listOf("AI & ML", "Product Design", "Tech Leadership", "Cloud Architecture")
  }

  val matchingRecentSearches = remember(recentSearches, query) {
    if (query.isBlank()) {
      recentSearches
    } else {
      recentSearches.filter { it.query.contains(query, ignoreCase = true) }
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = GlassSurfaceDark,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, if (isFocused) GlassAccentBlue else GlassBorder),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = if (isFocused) GlassAccentBlue else GlassTextMuted,
          modifier = Modifier.size(20.dp),
        )

        OutlinedTextField(
          value = query,
          onValueChange = {
            onQueryChange(it)
            showDropdown = true
          },
          placeholder = {
            Text(
              text = "Search AI, Design, Leadership courses...",
              style = MaterialTheme.typography.bodyMedium,
              color = GlassTextMuted,
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions =
            KeyboardActions(
              onSearch = {
                focusManager.clearFocus()
                showDropdown = false
                if (query.isNotBlank()) {
                  onSearchTriggered(query)
                }
              },
            ),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Color.Transparent,
              unfocusedBorderColor = Color.Transparent,
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
              cursorColor = GlassAccentBlue,
            ),
          modifier =
            Modifier
              .weight(1f)
              .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                  showDropdown = true
                }
              }
              .testTag("landing_search_text_field"),
        )

        if (query.isNotEmpty()) {
          IconButton(
            onClick = {
              onQueryChange("")
              showDropdown = true
            },
            modifier = Modifier.size(28.dp).testTag("clear_search_input_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear Search",
              tint = GlassTextMuted,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    // Recent Searches Dropdown Menu
    AnimatedVisibility(
      visible = showDropdown && (isFocused || matchingRecentSearches.isNotEmpty()),
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically(),
    ) {
      Card(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp))
            .testTag("recent_searches_dropdown"),
        colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (matchingRecentSearches.isNotEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = null,
                  tint = GlassAccentBlue,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "Recent Searches",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextSecondary,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                      onClearAllRecentSearches()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("clear_recent_searches_btn"),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = "Clear all",
                  tint = Color(0xFFF87171),
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Clear all",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = Color(0xFFF87171),
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            matchingRecentSearches.forEach { searchItem ->
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      onQueryChange(searchItem.query)
                      onSearchTriggered(searchItem.query)
                      focusManager.clearFocus()
                      showDropdown = false
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag("recent_search_item_${searchItem.query}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = GlassTextMuted,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = searchItem.query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                IconButton(
                  onClick = {
                    onDeleteRecentSearch(searchItem.query)
                  },
                  modifier =
                    Modifier
                      .size(28.dp)
                      .testTag("delete_recent_search_${searchItem.query}"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove query",
                    tint = GlassTextMuted,
                    modifier = Modifier.size(14.dp),
                  )
                }
              }
            }
          } else if (query.isNotBlank()) {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    onSearchTriggered(query)
                    focusManager.clearFocus()
                    showDropdown = false
                  }
                  .padding(horizontal = 8.dp, vertical = 8.dp)
                  .testTag("direct_search_prompt"),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GlassAccentBlue,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Search for \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassAccentBlue,
              )
            }
          } else {
            // Popular suggestion chips when no recent searches yet
            Column(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Popular Topics",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GlassTextMuted,
              )
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                popularSuggestions.forEach { topic ->
                  Surface(
                    color = Color(0x263B82F6),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x333B82F6)),
                    modifier =
                      Modifier.clickable {
                        onQueryChange(topic)
                        onSearchTriggered(topic)
                        focusManager.clearFocus()
                        showDropdown = false
                      },
                  ) {
                    Text(
                      text = topic,
                      style = MaterialTheme.typography.labelSmall,
                      color = GlassAccentBlue,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            }
          }

          // Close dropdown footer button
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
          ) {
            Text(
              text = "Close",
              style = MaterialTheme.typography.labelSmall,
              color = GlassTextMuted,
              modifier =
                Modifier
                  .clickable {
                    focusManager.clearFocus()
                    showDropdown = false
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }
    }
  }
}

/**
 * Bottom Frosted Glass Navigation Bar
 */
@Composable
fun FrostedGlassBottomNav(
  userSession: UserSession? = null,
  onOpenAuth: () -> Unit,
  onOpenAccount: () -> Unit,
  onTabSelect: (String) -> Unit,
) {
  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .border(BorderStroke(1.dp, Color(0x26FFFFFF)))
        .navigationBarsPadding(),
    color = Color(0xEE0B1120),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Home Tab
      BottomNavItem(
        title = "Home",
        icon = Icons.Default.Home,
        isSelected = true,
        onClick = { onTabSelect("Home") },
        modifier = Modifier.weight(1f),
      )

      // My Learning Tab
      BottomNavItem(
        title = "My Learning",
        icon = Icons.Default.MenuBook,
        isSelected = false,
        onClick = {
          if (userSession != null) {
            onOpenAccount()
          } else {
            onOpenAuth()
          }
        },
        modifier = Modifier.weight(1f),
      )

      // Account Tab (Directly opens Account Profile if signed in, or Sign In page if signed out)
      BottomNavItem(
        title = if (userSession != null) (if (userSession.isAdmin) "Admin" else userSession.username) else "Account",
        icon = if (userSession?.isAdmin == true) Icons.Default.AdminPanelSettings else Icons.Default.Person,
        isSelected = false,
        onClick = {
          if (userSession != null) {
            onOpenAccount()
          } else {
            onOpenAuth()
          }
        },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
fun BottomNavItem(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .height(56.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable { onClick() }
        .padding(vertical = 4.dp)
        .testTag("nav_item_$title"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (isSelected) {
        Box(
          modifier =
            Modifier
              .size(width = 44.dp, height = 26.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0x333B82F6)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = GlassAccentBlue,
            modifier = Modifier.size(18.dp),
          )
        }
      } else {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = GlassTextMuted,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) GlassAccentBlue else GlassTextMuted,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailModalSheet(
  course: CourseEntity,
  courseDao: CourseDao,
  enrollmentStatus: String? = null,
  onDismiss: () -> Unit,
  onEnroll: (CourseEntity) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val reviews by courseDao.getReviewsForCourse(course.id).collectAsState(initial = emptyList())

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = GlassSurfaceDark,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = Color(course.colorHex).copy(alpha = 0.2f),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = course.category,
            style = MaterialTheme.typography.labelMedium,
            color = Color(course.colorHex),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = GlassTextMuted)
        }
      }

      val primaryDetailImage = remember(course.imageUrls, course.drawableName) {
        val tokens = ImageHelper.splitImageUrls(course.imageUrls)
        tokens.firstOrNull()?.ifBlank { null } ?: course.drawableName
      }
      val fallbackRes = when (course.drawableName) {
        "img_course_design" -> R.drawable.img_course_design
        "img_course_business" -> R.drawable.img_course_business
        else -> R.drawable.img_course_tech
      }

      Spacer(modifier = Modifier.height(10.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp)),
      ) {
        AppImage(
          model = primaryDetailImage,
          contentDescription = course.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          fallbackResId = fallbackRes,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = course.title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = GlassTextPrimary,
      )

      Text(
        text = course.subtitle,
        style = MaterialTheme.typography.titleSmall,
        color = GlassAccentBlue,
      )

      val dynamicRating = remember(reviews, course.rating) {
        if (reviews.isNotEmpty()) reviews.map { it.rating.toDouble() }.average() else course.rating
      }
      val dynamicCount = remember(reviews, course.reviewCount) {
        if (reviews.isNotEmpty()) reviews.size else course.reviewCount
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          repeat(5) { index ->
            val isFilled = (index + 1) <= dynamicRating.toInt()
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = if (isFilled) GlassAmber else Color(0x44CBD5E1),
              modifier = Modifier.size(16.dp),
            )
          }
        }
        Text(
          text = String.format(java.util.Locale.US, "%.1f (%d reviews)", dynamicRating, dynamicCount),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = GlassAmber,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = course.description,
        style = MaterialTheme.typography.bodyMedium,
        color = GlassTextSecondary,
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Instructor and Specs
      Card(
        modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Instructor", style = MaterialTheme.typography.bodySmall, color = GlassTextMuted)
            Text(course.instructor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = GlassTextPrimary)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Duration", style = MaterialTheme.typography.bodySmall, color = GlassTextMuted)
            Text(course.duration, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = GlassTextPrimary)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Level", style = MaterialTheme.typography.bodySmall, color = GlassTextMuted)
            Text(course.level, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = GlassTextPrimary)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Price", style = MaterialTheme.typography.bodySmall, color = GlassTextMuted)
            Text(course.price, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
          }
        }
      }

      // Reviews Section if available
      if (reviews.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Student Reviews (${reviews.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary,
          )
          Text(
            text = "Verified Students",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF34D399),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        reviews.forEach { rev ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x330F172A)),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(12.dp),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = rev.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                  )
                  if (rev.role.isNotBlank()) {
                    Text(
                      text = rev.role,
                      style = MaterialTheme.typography.labelSmall,
                      color = GlassTextMuted,
                    )
                  }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  repeat(rev.rating.toInt().coerceIn(1, 5)) {
                    Icon(
                      imageVector = Icons.Default.Star,
                      contentDescription = null,
                      tint = GlassAmber,
                      modifier = Modifier.size(13.dp),
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = rev.comment,
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      when (enrollmentStatus) {
        "Accepted" -> {
          Button(
            onClick = { onEnroll(course) },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("continue_course_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text("Continue Course ▶", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
        "Pending" -> {
          Button(
            onClick = { onEnroll(course) },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pending_approval_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text("Pending Admin Approval ⏳", fontWeight = FontWeight.Bold, color = Color.Black)
          }
        }
        else -> {
          Button(
            onClick = { onEnroll(course) },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("enroll_now_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
            shape = RoundedCornerShape(14.dp),
          ) {
            Text("Enroll in Course • ${course.price}", fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

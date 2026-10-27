package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import coil.compose.AsyncImage
import com.example.util.AppImage
import com.example.util.ImageHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppDatabase
import com.example.data.CourseDao
import com.example.data.CourseEntity
import com.example.data.EnrollmentEntity
import com.example.data.UserSession
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassPrimaryBlue
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceHighlight
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AdminTab(val title: String) {
  NOTIFICATIONS("Notifications"),
  COURSES("Course Catalog"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
  userSession: UserSession,
  courseDao: CourseDao,
  onExitAdmin: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  // Real-time Room database Flows
  val courses by courseDao.getAllCourses().collectAsState(initial = emptyList())
  val allEnrollments by courseDao.getAllEnrollments().collectAsState(initial = emptyList())
  val pendingCount by courseDao.getPendingEnrollmentsCount().collectAsState(initial = 0)

  var selectedTab by remember { mutableStateOf(AdminTab.NOTIFICATIONS) }
  var notifFilter by remember { mutableStateOf("All") } // "All", "Pending", "Accepted", "Rejected"

  var searchQuery by remember { mutableStateOf("") }
  var showAddBottomSheet by remember { mutableStateOf(false) }
  var courseToEdit by remember { mutableStateOf<CourseEntity?>(null) }
  var courseToDelete by remember { mutableStateOf<CourseEntity?>(null) }
  var isCloudSyncing by remember { mutableStateOf(false) }
  var lastSyncedTime by remember { mutableStateOf("Just now") }

  // Auto-sync incoming enrollments and courses from cloud on opening Admin Screen and poll periodically
  LaunchedEffect(Unit) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    while (isActive) {
      withContext(Dispatchers.IO) {
        com.example.data.CloudSyncService.syncAllEnrollmentsFromCloud(courseDao)
        com.example.data.CloudSyncService.syncAllCoursesFromCloud(courseDao)
      }
      lastSyncedTime = timeFormat.format(Date())
      delay(4000) // Poll every 4 seconds so admin sees incoming requests automatically
    }
  }

  val filteredCourses = remember(courses, searchQuery) {
    if (searchQuery.isBlank()) {
      courses
    } else {
      courses.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
          it.category.contains(searchQuery, ignoreCase = true) ||
          it.instructor.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  val filteredEnrollments = remember(allEnrollments, notifFilter) {
    // Deduplicate so Admin does not see duplicate entries for the same student + course
    val deduped = allEnrollments.groupBy {
      val userKey = if (it.userId.isNotBlank()) it.userId.lowercase() else it.username.lowercase()
      "$userKey|${it.courseTitle.lowercase()}"
    }.values.map { list ->
      list.firstOrNull { it.status.equals("Pending", ignoreCase = true) } ?: list.first()
    }

    when (notifFilter) {
      "Pending" -> deduped.filter { it.status.equals("Pending", ignoreCase = true) }
      "Accepted" -> deduped.filter { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Active", ignoreCase = true) }
      "Rejected" -> deduped.filter { it.status.equals("Rejected", ignoreCase = true) }
      else -> deduped
    }
  }

  val handleStatusUpdate = { enrollment: EnrollmentEntity, newStatus: String ->
    coroutineScope.launch {
      withContext(Dispatchers.IO) {
        courseDao.updateEnrollmentStatus(enrollment.id, newStatus)
        // Also update any matching local rows in case of multiple submissions
        val allLocal = courseDao.getAllEnrollmentsList()
        for (local in allLocal) {
          if (com.example.data.CloudSyncService.isSameEnrollment(local, enrollment)) {
            courseDao.updateEnrollmentStatus(local.id, newStatus)
          }
        }
        // Sync status to Firestore cloud so student's phone receives the update
        com.example.data.FirebaseFirestoreService.updateEnrollmentStatusInCloud(enrollment, newStatus)
        // Also update Universal Cloud status
        com.example.data.CloudSyncService.updateEnrollmentStatusInUniversalCloud(enrollment, newStatus)
      }

      val isAccepted = newStatus.equals("Accepted", ignoreCase = true)
      val actionMsg = if (isAccepted) "ACCEPTED & Enrolled" else "REJECTED"
      snackbarHostState.showSnackbar("Enrollment $actionMsg for ${enrollment.username} (ID: ${enrollment.userId})")
    }
    Unit
  }

  Scaffold(
    containerColor = GlassBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Admin Control Center",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = GlassTextPrimary,
            )
            Text(
              text = "${pendingCount} Pending • ${courses.size} Courses • Cloud: $lastSyncedTime",
              style = MaterialTheme.typography.labelSmall,
              color = if (pendingCount > 0) Color(0xFFFBBF24) else Color(0xFF34D399),
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onExitAdmin,
            modifier = Modifier.testTag("admin_back_btn"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Hub",
              tint = Color.White,
            )
          }
        },
        actions = {
          IconButton(
            onClick = {
              if (!isCloudSyncing) {
                isCloudSyncing = true
                coroutineScope.launch {
                  val syncResult = withContext(Dispatchers.IO) {
                    val res = com.example.data.CloudSyncService.syncAllEnrollmentsFromCloud(courseDao)
                    com.example.data.CloudSyncService.syncAllCoursesFromCloud(courseDao)
                    res
                  }
                  isCloudSyncing = false
                  if (syncResult.isSuccess) {
                    snackbarHostState.showSnackbar("Synced with Cloud: ${syncResult.count} enrollments active")
                  } else {
                    snackbarHostState.showSnackbar(syncResult.errorMessage ?: "Cloud sync failed")
                  }
                }
              }
            },
            modifier = Modifier.testTag("admin_cloud_sync_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync Cloud Now",
              tint = if (isCloudSyncing) Color(0xFF38BDF8) else GlassTextPrimary,
            )
          }

          IconButton(
            onClick = {
              coroutineScope.launch(Dispatchers.IO) {
                AppDatabase.populateInitialData(courseDao)
                snackbarHostState.showSnackbar("Reset catalog & sample records")
              }
            },
            modifier = Modifier.testTag("admin_reset_seed_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Reset Demo Data",
              tint = GlassTextSecondary,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = GlassSurfaceDark),
      )
    },
    floatingActionButton = {
      if (selectedTab == AdminTab.COURSES) {
        FloatingActionButton(
          onClick = { showAddBottomSheet = true },
          containerColor = GlassPrimaryBlue,
          contentColor = Color.White,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.testTag("fab_add_course"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add Course")
            Text("Add Course", fontWeight = FontWeight.Bold)
          }
        }
      }
    },
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      AmbientGlowBackground()

      Column(modifier = Modifier.fillMaxSize()) {
        // Tab Navigation for Admin Screen
        Surface(
          color = GlassSurfaceDark,
          modifier = Modifier.fillMaxWidth(),
        ) {
          TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
              TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = GlassAccentBlue,
                height = 3.dp,
              )
            },
          ) {
            Tab(
              selected = selectedTab == AdminTab.NOTIFICATIONS,
              onClick = { selectedTab = AdminTab.NOTIFICATIONS },
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (selectedTab == AdminTab.NOTIFICATIONS) {
                      if (pendingCount > 0) Color(0xFFFBBF24) else GlassAccentBlue
                    } else GlassTextMuted,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "Enrollment Requests",
                    fontWeight = if (selectedTab == AdminTab.NOTIFICATIONS) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == AdminTab.NOTIFICATIONS) GlassTextPrimary else GlassTextMuted,
                  )
                  if (pendingCount > 0) {
                    Surface(
                      color = Color(0xFFEF4444),
                      shape = CircleShape,
                    ) {
                      Text(
                        text = "$pendingCount",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
              },
            )

            Tab(
              selected = selectedTab == AdminTab.COURSES,
              onClick = { selectedTab = AdminTab.COURSES },
              text = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = if (selectedTab == AdminTab.COURSES) GlassAccentBlue else GlassTextMuted,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "Course Catalog",
                    fontWeight = if (selectedTab == AdminTab.COURSES) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == AdminTab.COURSES) GlassTextPrimary else GlassTextMuted,
                  )
                }
              },
            )
          }
        }

        // Body Content based on Tab
        when (selectedTab) {
          AdminTab.NOTIFICATIONS -> {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              // Notification Summary Header
              item {
                Card(
                  modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(20.dp)),
                  colors = CardDefaults.cardColors(containerColor = GlassSurface),
                  shape = RoundedCornerShape(20.dp),
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        Box(
                          modifier =
                            Modifier
                              .size(40.dp)
                              .clip(CircleShape)
                              .background(if (pendingCount > 0) Color(0x33F59E0B) else Color(0x3310B981)),
                          contentAlignment = Alignment.Center,
                        ) {
                          Icon(
                            imageVector = if (pendingCount > 0) Icons.Default.Notifications else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (pendingCount > 0) Color(0xFFFBBF24) else Color(0xFF34D399),
                            modifier = Modifier.size(22.dp),
                          )
                        }
                        Column {
                          Text(
                            text = "Enrollment Approvals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlassTextPrimary,
                          )
                          Text(
                            text = if (pendingCount > 0) "$pendingCount user applications awaiting your review" else "All student applications processed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pendingCount > 0) Color(0xFFFDE68A) else GlassTextSecondary,
                          )
                        }
                      }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      AdminStatMetric(
                        label = "Pending",
                        value = pendingCount.toString(),
                        icon = Icons.Default.Notifications,
                        modifier = Modifier.weight(1f),
                      )
                      AdminStatMetric(
                        label = "Accepted",
                        value = allEnrollments.count { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Active", ignoreCase = true) }.toString(),
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f),
                      )
                      AdminStatMetric(
                        label = "Total Apps",
                        value = allEnrollments.size.toString(),
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }
                }
              }

              // Filter Chips
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FilterChip(
                    selected = notifFilter == "All",
                    onClick = { notifFilter = "All" },
                    label = { Text("All (${allEnrollments.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = GlassPrimaryBlue,
                      selectedLabelColor = Color.White,
                      containerColor = Color(0x330F172A),
                      labelColor = GlassTextMuted,
                    ),
                  )

                  FilterChip(
                    selected = notifFilter == "Pending",
                    onClick = { notifFilter = "Pending" },
                    label = { Text("Pending ($pendingCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFFF59E0B),
                      selectedLabelColor = Color.White,
                      containerColor = Color(0x330F172A),
                      labelColor = GlassTextMuted,
                    ),
                  )

                  FilterChip(
                    selected = notifFilter == "Accepted",
                    onClick = { notifFilter = "Accepted" },
                    label = { Text("Accepted") },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFF10B981),
                      selectedLabelColor = Color.White,
                      containerColor = Color(0x330F172A),
                      labelColor = GlassTextMuted,
                    ),
                  )

                  FilterChip(
                    selected = notifFilter == "Rejected",
                    onClick = { notifFilter = "Rejected" },
                    label = { Text("Rejected") },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = Color(0xFFEF4444),
                      selectedLabelColor = Color.White,
                      containerColor = Color(0x330F172A),
                      labelColor = GlassTextMuted,
                    ),
                  )
                }
              }

              if (filteredEnrollments.isEmpty()) {
                item {
                  Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                  ) {
                    Column(
                      modifier = Modifier.padding(32.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = GlassTextMuted,
                        modifier = Modifier.size(48.dp),
                      )
                      Text(
                        text = "No Enrollment Requests Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                      )
                      Text(
                        text = "When students select a course and submit the fees via EasyPaisa/JazzCash, their detailed enrollment requests with User ID and amount will appear here for your approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextMuted,
                        textAlign = TextAlign.Center,
                      )
                    }
                  }
                }
              } else {
                items(filteredEnrollments, key = { it.id }) { enrollment ->
                  AdminEnrollmentNotificationCard(
                    enrollment = enrollment,
                    onAccept = { handleStatusUpdate(enrollment, "Accepted") },
                    onReject = { handleStatusUpdate(enrollment, "Rejected") },
                  )
                }
              }
            }
          }

          AdminTab.COURSES -> {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              // Search Bar inside Admin
              item {
                OutlinedTextField(
                  value = searchQuery,
                  onValueChange = { searchQuery = it },
                  placeholder = { Text("Filter courses in Room DB...") },
                  leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = GlassTextMuted)
                  },
                  trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                      IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = GlassTextMuted)
                      }
                    }
                  },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("admin_search_field"),
                  shape = RoundedCornerShape(14.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = GlassSurfaceDark,
                      unfocusedContainerColor = Color(0x400F172A),
                      focusedBorderColor = GlassAccentBlue,
                      unfocusedBorderColor = GlassBorder,
                      focusedTextColor = GlassTextPrimary,
                      unfocusedTextColor = GlassTextPrimary,
                      cursorColor = GlassAccentBlue,
                    ),
                )
              }

              // Cached Course Upload Draft Banner (if an unfinished course upload is stored in memory)
              item {
                val draftPrefs = remember {
                  context.getSharedPreferences("coursehub_course_upload_draft", Context.MODE_PRIVATE)
                }
                var hasSavedDraft by remember(showAddBottomSheet, courseToEdit) {
                  val savedTitle = draftPrefs.getString("new_course_title", "").orEmpty()
                  val savedSubtitle = draftPrefs.getString("new_course_subtitle", "").orEmpty()
                  val savedDesc = draftPrefs.getString("new_course_description", "").orEmpty()
                  mutableStateOf(savedTitle.isNotBlank() || savedSubtitle.isNotBlank() || savedDesc.isNotBlank())
                }
                val savedDraftTitle = remember(showAddBottomSheet, courseToEdit, hasSavedDraft) {
                  draftPrefs.getString("new_course_title", "").orEmpty().ifBlank { "Untitled Course Draft" }
                }

                if (hasSavedDraft) {
                  Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x2610B981)),
                    border = BorderStroke(1.dp, Color(0x8010B981)),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(14.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                          text = "⚡ Cached Course Draft Saved",
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF34D399),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = savedDraftTitle,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                          color = GlassTextPrimary,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "Your typed course details are safely cached in memory.",
                          style = MaterialTheme.typography.labelSmall,
                          color = GlassTextSecondary,
                        )
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                          onClick = {
                            courseToEdit = null
                            showAddBottomSheet = true
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                          shape = RoundedCornerShape(10.dp),
                          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                          Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                          onClick = {
                            draftPrefs.edit().clear().apply()
                            hasSavedDraft = false
                          },
                          modifier = Modifier.size(36.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Discard Draft",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp),
                          )
                        }
                      }
                    }
                  }
                }
              }

              // Course List Header
              item {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Catalog Inventory (${filteredCourses.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                  )
                  Button(
                    onClick = {
                      courseToEdit = null
                      showAddBottomSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload Course", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }

              if (filteredCourses.isEmpty()) {
                item {
                  Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                  ) {
                    Column(
                      modifier = Modifier.fillMaxWidth().padding(32.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = GlassTextMuted,
                        modifier = Modifier.size(48.dp),
                      )
                      Spacer(modifier = Modifier.height(12.dp))
                      Text(
                        text = "No courses found",
                        style = MaterialTheme.typography.titleMedium,
                        color = GlassTextPrimary,
                        fontWeight = FontWeight.Bold,
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = "Tap '+ Add Course' below to insert a new course into Room Database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary,
                        textAlign = TextAlign.Center,
                      )
                    }
                  }
                }
              } else {
                items(filteredCourses, key = { it.id }) { course ->
                  AdminCourseItemCard(
                    course = course,
                    onEdit = { courseToEdit = course },
                    onDelete = { courseToDelete = course },
                  )
                }
              }

              item {
                Spacer(modifier = Modifier.height(72.dp)) // Fab padding
              }
            }
          }
        }
      }
    }
  }

  // Add / Edit Course BottomSheet
  if (showAddBottomSheet || courseToEdit != null) {
    CourseFormBottomSheet(
      initialCourse = courseToEdit,
      courseDao = courseDao,
      snackbarHostState = snackbarHostState,
      onDismiss = {
        showAddBottomSheet = false
        courseToEdit = null
      },
      onSaveCourse = { course ->
        coroutineScope.launch(Dispatchers.IO) {
          courseDao.insertCourse(course)
          // Sync course to Google Cloud Firestore so it becomes available worldwide
          com.example.data.FirebaseFirestoreService.saveCourseToCloud(course)
          // Also sync course to Universal Cloud
          com.example.data.CloudSyncService.syncCourseToUniversalCloud(course)
          snackbarHostState.showSnackbar(
            if (courseToEdit != null) "Course updated & synced worldwide" else "Course saved & synced worldwide",
          )
        }
        showAddBottomSheet = false
        courseToEdit = null
      },
    )
  }

  // Delete Confirmation Dialog
  if (courseToDelete != null) {
    AlertDialog(
      onDismissRequest = { courseToDelete = null },
      confirmButton = {
        Button(
          onClick = {
            val course = courseToDelete
            if (course != null) {
              coroutineScope.launch(Dispatchers.IO) {
                courseDao.updateCourse(course.copy(isActive = false))
                courseDao.deleteCourse(course)
                // Deactivate and delete from Google Cloud Firestore so sync never re-inserts it
                com.example.data.FirebaseFirestoreService.deleteCourseFromCloud(course.id)
                snackbarHostState.showSnackbar("Course '${course.title}' deleted worldwide")
              }
            }
            courseToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { courseToDelete = null }) {
          Text("Cancel", color = GlassTextSecondary)
        }
      },
      title = { Text("Delete Course?") },
      text = { Text("Are you sure you want to permanently delete '${courseToDelete?.title}' from the Room database? This cannot be undone.") },
      containerColor = GlassSurfaceDark,
      titleContentColor = GlassTextPrimary,
      textContentColor = GlassTextSecondary,
    )
  }
}

@Composable
fun AdminEnrollmentNotificationCard(
  enrollment: EnrollmentEntity,
  onAccept: () -> Unit,
  onReject: () -> Unit,
) {
  val context = LocalContext.current
  val sdf = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
  val formattedDate = remember(enrollment.enrolledAt) { sdf.format(Date(enrollment.enrolledAt)) }

  val isPending = enrollment.status.equals("Pending", ignoreCase = true)
  val isAccepted = enrollment.status.equals("Accepted", ignoreCase = true) || enrollment.status.equals("Active", ignoreCase = true)
  val isRejected = enrollment.status.equals("Rejected", ignoreCase = true)
  var showFullReceiptDialog by remember { mutableStateOf(false) }

  val borderStrokeColor = when {
    isPending -> Color(0xFFF59E0B)
    isAccepted -> Color(0x6610B981)
    else -> Color(0x66EF4444)
  }

  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .border(BorderStroke(if (isPending) 1.5.dp else 1.dp, borderStrokeColor), RoundedCornerShape(20.dp)),
    colors = CardDefaults.cardColors(containerColor = GlassSurface),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header: Status badge & timestamp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = when {
            isPending -> Color(0x33F59E0B)
            isAccepted -> Color(0x3310B981)
            else -> Color(0x33EF4444)
          },
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(
            1.dp,
            when {
              isPending -> Color(0x80F59E0B)
              isAccepted -> Color(0x8010B981)
              else -> Color(0x80EF4444)
            },
          ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = when {
                isPending -> Icons.Default.Notifications
                isAccepted -> Icons.Default.Check
                else -> Icons.Default.Close
              },
              contentDescription = null,
              tint = when {
                isPending -> Color(0xFFFBBF24)
                isAccepted -> Color(0xFF34D399)
                else -> Color(0xFFF87171)
              },
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = if (isPending) "PENDING APPROVAL" else enrollment.status.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = when {
                isPending -> Color(0xFFFBBF24)
                isAccepted -> Color(0xFF34D399)
                else -> Color(0xFFF87171)
              },
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GlassTextMuted, modifier = Modifier.size(13.dp))
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextMuted,
          )
        }
      }

      // Selected Course Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Default.School, contentDescription = null, tint = GlassAccentBlue, modifier = Modifier.size(20.dp))
        Text(
          text = enrollment.courseTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = GlassTextPrimary,
        )
      }

      // Verification Details Box
      Surface(
        color = Color(0x2B0F172A),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // User Name & Account ID (Number)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(15.dp))
              Text(
                text = "Student: ${enrollment.username}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
              )
            }

            val displayId = enrollment.userId.ifBlank { "CH-${enrollment.username.hashCode().let { if (it < 0) -it else it }.toString().take(5)}" }
            Surface(
              color = Color(0x333B82F6),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color(0x663B82F6)),
            ) {
              Text(
                text = "ID: $displayId",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GlassAccentBlue,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }

          // Total Fees Details & Payment Method
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(15.dp))
              Text(
                text = "Total Fees: ${enrollment.price.ifBlank { "PKR 0" }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399),
              )
            }

            Text(
              text = "Via ${enrollment.paymentMethod.ifBlank { "EasyPaisa / JazzCash" }}",
              style = MaterialTheme.typography.labelSmall,
              color = GlassTextSecondary,
            )
          }

          // Receipt / Transaction ID
          if (enrollment.transactionId.isNotBlank()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(15.dp))
                Text(
                  text = "Receipt / TID: ${enrollment.transactionId}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFDE68A),
                )
              }

              IconButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Transaction ID", enrollment.transactionId)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "Copied TID: ${enrollment.transactionId}", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(26.dp),
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy TID", tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
              }
            }
          }

          // Uploaded Payment Receipt Screenshot Preview
          if (enrollment.receiptImage.isNotBlank()) {
            Surface(
              color = Color(0x2210B981),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0x6610B981)),
              modifier = Modifier.fillMaxWidth().clickable { showFullReceiptDialog = true },
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(width = 50.dp, height = 40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, Color(0x6610B981)), RoundedCornerShape(6.dp)),
                ) {
                  AppImage(
                    model = enrollment.receiptImage,
                    contentDescription = "Uploaded Receipt",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Payment Screenshot Attached",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399),
                  )
                  Text(
                    text = "Tap to view full receipt screenshot",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTextSecondary,
                    fontSize = 11.sp,
                  )
                }

                Icon(
                  imageVector = Icons.Default.PhotoLibrary,
                  contentDescription = null,
                  tint = Color(0xFF34D399),
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }

      if (showFullReceiptDialog && enrollment.receiptImage.isNotBlank()) {
        AlertDialog(
          onDismissRequest = { showFullReceiptDialog = false },
          title = {
            Text(
              "Payment Receipt Screenshot",
              style = MaterialTheme.typography.titleMedium,
              color = GlassTextPrimary,
            )
          },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                "Uploaded by: ${enrollment.username} (ID: ${enrollment.userId})",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary,
              )
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(350.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(12.dp))
                  .background(GlassSurfaceDark),
              ) {
                AppImage(
                  model = enrollment.receiptImage,
                  contentDescription = "Full payment receipt",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Fit,
                )
              }
            }
          },
          confirmButton = {
            Button(
              onClick = { showFullReceiptDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
            ) {
              Text("Close")
            }
          },
          containerColor = GlassSurfaceDark,
        )
      }

      // Actions: Accept / Reject Buttons
      if (isPending) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onAccept,
            modifier = Modifier.weight(1f).height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Accept Enrollment", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f).height(44.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
            border = BorderStroke(1.dp, Color(0x66EF4444)),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reject", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun AdminStatMetric(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0x330F172A),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, GlassBorder),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = GlassAccentBlue,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = GlassTextPrimary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = GlassTextMuted,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun AdminCourseItemCard(
  course: CourseEntity,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp))
        .testTag("admin_course_item_${course.id}"),
    colors = CardDefaults.cardColors(containerColor = GlassSurface),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Surface(
            color = Color(course.colorHex).copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, Color(course.colorHex).copy(alpha = 0.5f)),
          ) {
            Text(
              text = course.category,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = Color(course.colorHex),
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = course.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary,
          )

          Text(
            text = course.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = GlassTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row {
          IconButton(
            onClick = onEdit,
            modifier =
              Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x263B82F6))
                .testTag("edit_course_${course.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Course",
              tint = GlassAccentBlue,
              modifier = Modifier.size(18.dp),
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onDelete,
            modifier =
              Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x26EF4444))
                .testTag("delete_course_${course.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Course",
              tint = Color(0xFFEF4444),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

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
            text = "👨‍🏫 ${course.instructor}",
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextMuted,
          )
          Text(
            text = "⏱ ${course.duration}",
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextMuted,
          )
        }

        Surface(
          color = Color(0x333B82F6),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Color(0x663B82F6)),
        ) {
          Text(
            text = course.price,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GlassAccentBlue,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFormBottomSheet(
  initialCourse: CourseEntity?,
  courseDao: CourseDao,
  snackbarHostState: SnackbarHostState,
  onDismiss: () -> Unit,
  onSaveCourse: (CourseEntity) -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val draftPrefs = remember {
    context.getSharedPreferences("coursehub_course_upload_draft", Context.MODE_PRIVATE)
  }
  val draftPrefix = remember(initialCourse?.id) {
    if (initialCourse != null) "edit_${initialCourse.id}_" else "new_course_"
  }
  val hasCachedDraft = remember(draftPrefix) {
    draftPrefs.getBoolean("${draftPrefix}has_draft", false)
  }

  var title by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}title", initialCourse?.title ?: "") ?: ""
      else initialCourse?.title ?: ""
    )
  }
  var subtitle by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}subtitle", initialCourse?.subtitle ?: "") ?: ""
      else initialCourse?.subtitle ?: ""
    )
  }
  var category by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}category", initialCourse?.category ?: "Artificial Intelligence") ?: "Artificial Intelligence"
      else initialCourse?.category ?: "Artificial Intelligence"
    )
  }
  var instructor by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}instructor", initialCourse?.instructor ?: "Prof. Ali Raza") ?: "Prof. Ali Raza"
      else initialCourse?.instructor ?: "Prof. Ali Raza"
    )
  }
  var duration by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}duration", initialCourse?.duration ?: "8 Weeks (40 hrs)") ?: "8 Weeks (40 hrs)"
      else initialCourse?.duration ?: "8 Weeks (40 hrs)"
    )
  }
  var level by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}level", initialCourse?.level ?: "Intermediate") ?: "Intermediate"
      else initialCourse?.level ?: "Intermediate"
    )
  }
  var price by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}price", initialCourse?.price ?: "PKR 12,000") ?: "PKR 12,000"
      else initialCourse?.price ?: "PKR 12,000"
    )
  }
  var rating by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}rating", initialCourse?.rating?.toString() ?: "4.8") ?: "4.8"
      else initialCourse?.rating?.toString() ?: "4.8"
    )
  }
  var studentsCount by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}studentsCount", initialCourse?.enrolledStudents ?: "1,240 students") ?: "1,240 students"
      else initialCourse?.enrolledStudents ?: "1,240 students"
    )
  }
  var description by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}description", initialCourse?.description ?: "") ?: ""
      else initialCourse?.description ?: ""
    )
  }
  var accessUrl by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}accessUrl", initialCourse?.accessUrl ?: "https://meet.google.com/ais-class") ?: "https://meet.google.com/ais-class"
      else initialCourse?.accessUrl ?: "https://meet.google.com/ais-class"
    )
  }
  var summary by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}summary", initialCourse?.summary ?: "") ?: ""
      else initialCourse?.summary ?: ""
    )
  }
  var objectives by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}objectives", initialCourse?.objectives ?: "") ?: ""
      else initialCourse?.objectives ?: ""
    )
  }
  var whatYouWillLearn by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}whatYouWillLearn", initialCourse?.whatYouWillLearn ?: "") ?: ""
      else initialCourse?.whatYouWillLearn ?: ""
    )
  }
  var imageUrls by remember(draftPrefix) {
    mutableStateOf(
      if (hasCachedDraft) draftPrefs.getString("${draftPrefix}imageUrls", initialCourse?.imageUrls ?: "img_course_tech, img_hero_courses") ?: "img_course_tech, img_hero_courses"
      else initialCourse?.imageUrls ?: "img_course_tech, img_hero_courses"
    )
  }
  var validationError by remember { mutableStateOf<String?>(null) }
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  // Real-time persistent caching: automatically saves every typed character and uploaded image
  LaunchedEffect(
    title,
    subtitle,
    category,
    instructor,
    duration,
    level,
    price,
    rating,
    studentsCount,
    description,
    accessUrl,
    summary,
    objectives,
    whatYouWillLearn,
    imageUrls,
  ) {
    val hasAnyContent = title.isNotBlank() || subtitle.isNotBlank() || description.isNotBlank() ||
      summary.isNotBlank() || objectives.isNotBlank() || whatYouWillLearn.isNotBlank()
    draftPrefs.edit()
      .putBoolean("${draftPrefix}has_draft", hasAnyContent || initialCourse != null)
      .putString("${draftPrefix}title", title)
      .putString("${draftPrefix}subtitle", subtitle)
      .putString("${draftPrefix}category", category)
      .putString("${draftPrefix}instructor", instructor)
      .putString("${draftPrefix}duration", duration)
      .putString("${draftPrefix}level", level)
      .putString("${draftPrefix}price", price)
      .putString("${draftPrefix}rating", rating)
      .putString("${draftPrefix}studentsCount", studentsCount)
      .putString("${draftPrefix}description", description)
      .putString("${draftPrefix}accessUrl", accessUrl)
      .putString("${draftPrefix}summary", summary)
      .putString("${draftPrefix}objectives", objectives)
      .putString("${draftPrefix}whatYouWillLearn", whatYouWillLearn)
      .putString("${draftPrefix}imageUrls", imageUrls)
      .apply()
  }

  fun clearCurrentDraft() {
    draftPrefs.edit()
      .remove("${draftPrefix}has_draft")
      .remove("${draftPrefix}title")
      .remove("${draftPrefix}subtitle")
      .remove("${draftPrefix}category")
      .remove("${draftPrefix}instructor")
      .remove("${draftPrefix}duration")
      .remove("${draftPrefix}level")
      .remove("${draftPrefix}price")
      .remove("${draftPrefix}rating")
      .remove("${draftPrefix}studentsCount")
      .remove("${draftPrefix}description")
      .remove("${draftPrefix}accessUrl")
      .remove("${draftPrefix}summary")
      .remove("${draftPrefix}objectives")
      .remove("${draftPrefix}whatYouWillLearn")
      .remove("${draftPrefix}imageUrls")
      .apply()
  }

  var isCompressingImage by remember { mutableStateOf(false) }
  var showCustomUrlField by remember { mutableStateOf(false) }
  var manualImageUrlInput by remember { mutableStateOf("") }

  val currentImages = remember(imageUrls) {
    ImageHelper.splitImageUrls(imageUrls)
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
  ) { uris ->
    if (uris.isNotEmpty()) {
      isCompressingImage = true
      coroutineScope.launch(Dispatchers.IO) {
        val newImages = uris.mapNotNull { uri ->
          ImageHelper.compressUriToBase64(context, uri)
        }
        if (newImages.isNotEmpty()) {
          val updated = currentImages.toMutableList().apply { addAll(newImages) }
          imageUrls = ImageHelper.joinImageUrls(updated)
        }
        isCompressingImage = false
      }
    }
  }

  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("Reset Course Form?", color = GlassTextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "This will clear the current draft fields from cached memory and reset to default values.",
          color = GlassTextSecondary,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            clearCurrentDraft()
            title = initialCourse?.title ?: ""
            subtitle = initialCourse?.subtitle ?: ""
            category = initialCourse?.category ?: "Artificial Intelligence"
            instructor = initialCourse?.instructor ?: "Prof. Ali Raza"
            duration = initialCourse?.duration ?: "8 Weeks (40 hrs)"
            level = initialCourse?.level ?: "Intermediate"
            price = initialCourse?.price ?: "PKR 12,000"
            rating = initialCourse?.rating?.toString() ?: "4.8"
            studentsCount = initialCourse?.enrolledStudents ?: "1,240 students"
            description = initialCourse?.description ?: ""
            accessUrl = initialCourse?.accessUrl ?: "https://meet.google.com/ais-class"
            summary = initialCourse?.summary ?: ""
            objectives = initialCourse?.objectives ?: ""
            whatYouWillLearn = initialCourse?.whatYouWillLearn ?: ""
            imageUrls = initialCourse?.imageUrls ?: "img_course_tech, img_hero_courses"
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
        ) {
          Text("Reset Form")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel", color = GlassTextSecondary)
        }
      },
      containerColor = GlassSurfaceDark,
    )
  }

  // Locked full-screen Dialog: never closes on accidental scroll, drag, outside touch, or back gesture
  Dialog(
    onDismissRequest = { /* Ignored intentionally: only explicit buttons close the panel */ },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = false,
      dismissOnClickOutside = false,
    ),
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = GlassSurfaceDark,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding(),
      ) {
        // Pinned Top Bar: Cannot be scrolled away or swipe-dismissed
        Surface(
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, GlassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (initialCourse != null) "Edit Course" else "Upload New Course",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary,
              )
              Text(
                text = "⚡ Locked Panel • Auto-Cached in Memory",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF34D399),
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = { showClearConfirmDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0x66EF4444)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp),
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassTextPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp),
              ) {
                Icon(Icons.Default.Close, contentDescription = "Close Panel", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Minimize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Scrollable Form Body
        Column(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (validationError != null) {
            Surface(
              color = Color(0x33EF4444),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0x66EF4444)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = validationError ?: "",
                color = Color(0xFFFCA5A5),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Course Title *") },
            modifier = Modifier.fillMaxWidth().testTag("input_course_title"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface,
              focusedBorderColor = GlassAccentBlue,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
            ),
          )

          OutlinedTextField(
            value = subtitle,
            onValueChange = { subtitle = it },
            label = { Text("Tagline / Short Summary *") },
            modifier = Modifier.fillMaxWidth().testTag("input_course_subtitle"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface,
              focusedBorderColor = GlassAccentBlue,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
            ),
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("Category") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedBorderColor = GlassAccentBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = GlassTextPrimary,
                unfocusedTextColor = GlassTextPrimary,
              ),
            )
            OutlinedTextField(
              value = price,
              onValueChange = { price = it },
              label = { Text("Price (e.g. PKR 14,500)") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedBorderColor = GlassAccentBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = GlassTextPrimary,
                unfocusedTextColor = GlassTextPrimary,
              ),
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = instructor,
              onValueChange = { instructor = it },
              label = { Text("Instructor") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedBorderColor = GlassAccentBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = GlassTextPrimary,
                unfocusedTextColor = GlassTextPrimary,
              ),
            )
            OutlinedTextField(
              value = duration,
              onValueChange = { duration = it },
              label = { Text("Duration") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedBorderColor = GlassAccentBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = GlassTextPrimary,
                unfocusedTextColor = GlassTextPrimary,
              ),
            )
          }

          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Full Description") },
            modifier = Modifier.fillMaxWidth().height(110.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface,
              focusedBorderColor = GlassAccentBlue,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
            ),
          )

          OutlinedTextField(
            value = accessUrl,
            onValueChange = { accessUrl = it },
            label = { Text("Course Access / Meeting URL (e.g. Telegram, Google Meet)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface,
              focusedBorderColor = GlassAccentBlue,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
            ),
          )

          OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Course Summary (Key Highlights)") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface,
              focusedBorderColor = GlassAccentBlue,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
            ),
          )

          OutlinedTextField(
            value = objectives,
            onValueChange = { objectives = it },
            label = { Text("Course Objectives") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface,
              focusedBorderColor = GlassAccentBlue,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
            ),
          )

          OutlinedTextField(
            value = whatYouWillLearn,
            onValueChange = { whatYouWillLearn = it },
            label = { Text("What You'll Learn (Takeaways)") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface,
              focusedBorderColor = GlassAccentBlue,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = GlassTextPrimary,
              unfocusedTextColor = GlassTextPrimary,
            ),
          )

          // Course Visuals & Media Picker Section (for Access Course Page)
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            border = BorderStroke(1.dp, GlassBorder),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = "Course Visuals & Media Picker",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                  )
                  Text(
                    text = "Photos & materials shown when students access this course",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSecondary,
                  )
                }
              }

              // Live Thumbnails Preview
              if (currentImages.isNotEmpty()) {
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                  items(currentImages) { imgToken ->
                    Box(
                      modifier = Modifier
                        .size(width = 110.dp, height = 80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(10.dp))
                        .background(GlassSurfaceDark),
                    ) {
                      AppImage(
                        model = imgToken,
                        contentDescription = "Course preview image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                      )

                      // Remove Button
                      IconButton(
                        onClick = {
                          val updated = currentImages.toMutableList().apply { remove(imgToken) }
                          imageUrls = ImageHelper.joinImageUrls(updated)
                        },
                        modifier = Modifier
                          .align(Alignment.TopEnd)
                          .size(24.dp)
                          .background(Color(0xDDEF4444), CircleShape),
                      ) {
                        Icon(
                          imageVector = Icons.Default.Close,
                          contentDescription = "Remove image",
                          tint = Color.White,
                          modifier = Modifier.size(14.dp),
                        )
                      }
                    }
                  }
                }
              }

              // Pick from Device Media Gallery
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Button(
                  onClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  enabled = !isCompressingImage,
                  modifier = Modifier.weight(1f).height(46.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = GlassAccentBlue),
                  shape = RoundedCornerShape(10.dp),
                ) {
                  if (isCompressingImage) {
                    androidx.compose.material3.CircularProgressIndicator(
                      modifier = Modifier.size(18.dp),
                      color = Color.White,
                      strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimizing...", fontSize = 12.sp)
                  } else {
                    Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Image (Gallery)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  }
                }

                OutlinedButton(
                  onClick = { showCustomUrlField = !showCustomUrlField },
                  modifier = Modifier.height(46.dp),
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, GlassBorder),
                ) {
                  Text(
                    if (showCustomUrlField) "Hide URL" else "Add URL",
                    fontSize = 12.sp,
                    color = GlassTextSecondary,
                  )
                }
              }

              if (showCustomUrlField) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  OutlinedTextField(
                    value = manualImageUrlInput,
                    onValueChange = { manualImageUrlInput = it },
                    label = { Text("Image URL or Preset Drawable") },
                    placeholder = { Text("https://... or img_course_tech") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = GlassSurfaceDark,
                      unfocusedContainerColor = GlassSurfaceDark,
                      focusedBorderColor = GlassAccentBlue,
                      unfocusedBorderColor = GlassBorder,
                      focusedTextColor = GlassTextPrimary,
                      unfocusedTextColor = GlassTextPrimary,
                    ),
                  )
                  Button(
                    onClick = {
                      if (manualImageUrlInput.isNotBlank()) {
                        val updated = currentImages.toMutableList().apply { add(manualImageUrlInput.trim()) }
                        imageUrls = ImageHelper.joinImageUrls(updated)
                        manualImageUrlInput = ""
                      }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                  ) {
                    Text("Add")
                  }
                }
              }
            }
          }

          // Review Management Section
          if (initialCourse != null) {
            val reviewsByCourse by courseDao.getReviewsForCourse(initialCourse.id).collectAsState(initial = emptyList())
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = GlassSurface),
              border = BorderStroke(1.dp, GlassBorder),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "Reviews (${reviewsByCourse.size})",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextPrimary,
                )

                if (reviewsByCourse.isNotEmpty()) {
                  reviewsByCourse.forEach { review ->
                    Card(
                      modifier = Modifier.fillMaxWidth(),
                      colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, GlassBorder),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Column(
                          modifier = Modifier.weight(1f).padding(end = 8.dp),
                          verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                          Text(
                            text = "${review.authorName} (${review.role})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTextPrimary,
                          )
                          Text(
                            text = "Rating: ${review.rating.toInt()}/5",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassAccentBlue,
                          )
                          Text(
                            text = review.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                        OutlinedButton(
                          onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                              courseDao.deleteReview(review)
                              com.example.data.FirebaseFirestoreService.deleteReviewFromCloud(
                                courseId = initialCourse.id,
                                reviewAuthor = review.authorName,
                                timestamp = review.timestamp,
                              )
                              snackbarHostState.showSnackbar("Review deleted")
                            }
                          },
                          shape = RoundedCornerShape(8.dp),
                          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                          border = BorderStroke(1.dp, Color(0x66EF4444)),
                          modifier = Modifier.height(34.dp),
                        ) {
                          Text(
                            text = "Delete",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                          )
                        }
                      }
                    }
                  }
                } else {
                  Text(
                    text = "No reviews yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTextMuted,
                  )
                }
              }
            }
          }
        }

        // Pinned Bottom Action Bar (Always accessible, never accidentally dismissed)
        Surface(
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, GlassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier.weight(1f).height(48.dp),
              border = BorderStroke(1.dp, GlassBorder),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text("Close (Keep Draft)", color = GlassTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
              onClick = {
                if (title.isBlank() || subtitle.isBlank()) {
                  validationError = "Please enter both Title and Tagline."
                  return@Button
                }

                val courseColor =
                  when (category.lowercase()) {
                    "artificial intelligence" -> 0xFF3B82F6
                    "cloud computing" -> 0xFF06B6D4
                    "full stack web" -> 0xFF10B981
                    "cybersecurity" -> 0xFFF59E0B
                    "mobile development" -> 0xFF8B5CF6
                    else -> 0xFF6366F1
                  }

                val savedCourse =
                  CourseEntity(
                    id = initialCourse?.id ?: UUID.randomUUID().toString(),
                    title = title.trim(),
                    subtitle = subtitle.trim(),
                    category = category.trim(),
                    instructor = instructor.trim(),
                    duration = duration.trim(),
                    level = level.trim(),
                    price = price.trim(),
                    rating = rating.toDoubleOrNull() ?: 4.8,
                    reviewCount = initialCourse?.reviewCount ?: 128,
                    enrolledStudents = studentsCount.trim(),
                    description = description.trim().ifBlank { subtitle.trim() },
                    colorHex = courseColor,
                    drawableName = initialCourse?.drawableName ?: "img_course_tech",
                    accessUrl = accessUrl.trim().ifBlank { "https://meet.google.com/ais-class" },
                    summary = summary.trim(),
                    objectives = objectives.trim(),
                    whatYouWillLearn = whatYouWillLearn.trim(),
                    imageUrls = imageUrls.trim(),
                  )

                clearCurrentDraft()
                onSaveCourse(savedCourse)
              },
              modifier = Modifier.weight(1f).height(48.dp).testTag("btn_save_course"),
              colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(if (initialCourse != null) "Update Course" else "Save Course", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

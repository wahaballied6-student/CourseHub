package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CourseDao
import com.example.data.CourseEntity
import com.example.data.EnrollmentEntity
import com.example.data.UserEntity
import com.example.data.UserSession
import com.example.ui.theme.GlassAccentBlue
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassPrimaryBlue
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
  userSession: UserSession,
  courseDao: CourseDao,
  onBack: () -> Unit,
  onSignOut: () -> Unit,
  onOpenAdmin: () -> Unit,
  onBrowseCourses: () -> Unit,
  onContinueCourse: (CourseEntity) -> Unit = {},
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var isCloudSyncing by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  // Query live user entity from Room database memory
  var dbUser by remember { mutableStateOf<UserEntity?>(null) }

  LaunchedEffect(userSession.username) {
    withContext(Dispatchers.IO) {
      dbUser = courseDao.getUserByUsername(userSession.username)
    }
    // Background polling for enrollment status updates from Cloud
    while (isActive) {
      withContext(Dispatchers.IO) {
        com.example.data.CloudSyncService.syncAllEnrollmentsFromCloud(courseDao)
      }
      delay(4000)
    }
  }

  // Live enrollments for this user from Room database (matching by username, userId, or email)
  val allEnrollmentsList by courseDao.getAllEnrollments().collectAsState(initial = emptyList())
  val enrollments = remember(allEnrollmentsList, userSession.username, userSession.userId, dbUser?.userId, userSession.email) {
    val uId = dbUser?.userId ?: userSession.userId
    val userEmail = userSession.email
    val filtered = allEnrollmentsList.filter { e ->
      e.username.equals(userSession.username, ignoreCase = true) ||
        (uId.isNotBlank() && e.userId.equals(uId, ignoreCase = true)) ||
        (userEmail.isNotBlank() && e.userEmail.isNotBlank() && e.userEmail.equals(userEmail, ignoreCase = true))
    }
    // Deduplicate by course title, giving priority to Accepted/Active over Pending
    filtered.groupBy { it.courseTitle.lowercase() }.values.map { list ->
      list.firstOrNull { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("Active", ignoreCase = true) }
        ?: list.firstOrNull { it.status.equals("Pending", ignoreCase = true) }
        ?: list.first()
    }
  }

  val copyToClipboard = { label: String, text: String ->
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label: $text", Toast.LENGTH_SHORT).show()
    coroutineScope.launch {
      snackbarHostState.showSnackbar("Copied $label to clipboard")
    }
    Unit
  }

  // Format account creation date
  val creationTimestamp = dbUser?.createdAt ?: userSession.createdAt
  val formattedDate = remember(creationTimestamp) {
    val sdf = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault())
    sdf.format(Date(creationTimestamp))
  }

  val activeUserId = dbUser?.userId ?: userSession.userId

  Scaffold(
    containerColor = GlassBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "My Account Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("account_back_btn")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
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
                  val result = withContext(Dispatchers.IO) {
                    com.example.data.CloudSyncService.syncAllEnrollmentsFromCloud(courseDao)
                  }
                  isCloudSyncing = false
                  if (result.isSuccess) {
                    snackbarHostState.showSnackbar("Synced with Cloud: ${result.count} enrollments active")
                  } else {
                    snackbarHostState.showSnackbar(result.errorMessage ?: "Cloud sync failed")
                  }
                }
              }
            },
            modifier =
              Modifier
                .clip(CircleShape)
                .background(Color(0x2238BDF8))
                .testTag("account_sync_top_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync Cloud",
              tint = if (isCloudSyncing) Color(0xFF38BDF8) else Color.White,
            )
          }

          IconButton(
            onClick = onSignOut,
            modifier =
              Modifier
                .clip(CircleShape)
                .background(Color(0x33EF4444))
                .testTag("account_sign_out_top_btn"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = Color(0xFFF87171),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = GlassSurfaceDark),
      )
    },
  ) { paddingValues ->
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues),
    ) {
      AmbientGlowBackground()

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. User Identity Header Card
        Card(
          modifier =
            Modifier
              .fillMaxWidth()
              .border(
                BorderStroke(
                  1.dp,
                  if (userSession.isAdmin) Color(0x66F59E0B) else Color(0x403B82F6),
                ),
                RoundedCornerShape(22.dp),
              ),
          colors = CardDefaults.cardColors(containerColor = GlassSurface),
          shape = RoundedCornerShape(22.dp),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(72.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      if (userSession.isAdmin) {
                        listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                      } else {
                        listOf(GlassPrimaryBlue, Color(0xFF8B5CF6))
                      },
                    ),
                  )
                  .border(BorderStroke(2.dp, Color(0x80FFFFFF)), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (userSession.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp),
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = userSession.username,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = GlassTextPrimary,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Surface(
                color = if (userSession.isAdmin) Color(0x33F59E0B) else Color(0x3310B981),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (userSession.isAdmin) Color(0x66F59E0B) else Color(0x6610B981)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = if (userSession.isAdmin) Icons.Default.Security else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (userSession.isAdmin) Color(0xFFFBBF24) else Color(0xFF34D399),
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = if (userSession.isAdmin) "Administrator" else "Verified Student",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (userSession.isAdmin) Color(0xFFFBBF24) else Color(0xFF34D399),
                  )
                }
              }
            }
          }
        }

        // 2. Permanent Account Credentials Card
        Card(
          modifier =
            Modifier
              .fillMaxWidth()
              .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(20.dp)),
          colors = CardDefaults.cardColors(containerColor = GlassSurface),
          shape = RoundedCornerShape(20.dp),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Badge,
                contentDescription = null,
                tint = GlassAccentBlue,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Account Credentials & Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary,
              )
            }

            // Account Name Item
            AccountDetailRow(
              icon = Icons.Default.Person,
              label = "Account Name",
              value = userSession.username,
              onCopy = { copyToClipboard("Account Name", userSession.username) },
            )

            // Permanent Account ID Item
            AccountDetailRow(
              icon = Icons.Default.ReceiptLong,
              label = "Account ID (Number)",
              value = activeUserId,
              valueColor = GlassAccentBlue,
              isBold = true,
              onCopy = { copyToClipboard("Account ID", activeUserId) },
            )

            // Email Address Item
            AccountDetailRow(
              icon = Icons.Default.Email,
              label = "Email Address",
              value = dbUser?.email?.ifBlank { userSession.email } ?: userSession.email,
              onCopy = { copyToClipboard("Email", dbUser?.email ?: userSession.email) },
            )

            // Account Creation Date Item
            AccountDetailRow(
              icon = Icons.Default.CalendarToday,
              label = "Account Creation Date",
              value = formattedDate,
              valueColor = Color(0xFF93C5FD),
              onCopy = { copyToClipboard("Creation Date", formattedDate) },
            )
          }
        }

        // 3. Enrolled Courses Section
        Card(
          modifier =
            Modifier
              .fillMaxWidth()
              .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(20.dp)),
          colors = CardDefaults.cardColors(containerColor = GlassSurface),
          shape = RoundedCornerShape(20.dp),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = null,
                  tint = Color(0xFF34D399),
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "Enrolled Courses",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextPrimary,
                )
              }

              Surface(
                color = Color(0x3310B981),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0x6610B981)),
              ) {
                Text(
                  text = "${enrollments.size} ${if (enrollments.size == 1) "Course" else "Courses"}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF34D399),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }

            if (enrollments.isEmpty()) {
              // Empty State
              Surface(
                color = Color(0x260F172A),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = GlassTextMuted,
                    modifier = Modifier.size(36.dp),
                  )
                  Text(
                    text = "No Enrolled Courses Yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GlassTextSecondary,
                  )
                  Text(
                    text = "Select any course from our catalog to start learning and level up your skills.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTextMuted,
                    textAlign = TextAlign.Center,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Button(
                    onClick = onBrowseCourses,
                    colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Course Catalog", fontWeight = FontWeight.Bold)
                  }
                }
              }
            } else {
              // List of Enrolled Courses
              enrollments.forEach { enrollment ->
                EnrolledCourseCard(
                  enrollment = enrollment,
                  courseDao = courseDao,
                  onContinueCourse = onContinueCourse,
                )
              }
            }
          }
        }

        // 4. Action Buttons
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (userSession.isAdmin) {
            Button(
              onClick = onOpenAdmin,
              modifier = Modifier.fillMaxWidth().height(48.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Open Admin Control Panel", fontWeight = FontWeight.Bold)
            }
          }

          OutlinedButton(
            onClick = onBrowseCourses,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.Explore, contentDescription = null, tint = GlassAccentBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Browse All Courses", color = Color.White, fontWeight = FontWeight.SemiBold)
          }

          OutlinedButton(
            onClick = { showPrivacyDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Color(0xFF10B981))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Privacy Policy & Data Rights", color = Color.White, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = onSignOut,
            modifier =
              Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("account_screen_sign_out_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
            border = BorderStroke(1.dp, Color(0x66EF4444)),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFF87171))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out of Account", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
          }
        }

        if (showPrivacyDialog) {
          AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CourseHub Privacy Policy", fontWeight = FontWeight.Bold)
              }
            },
            text = {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  "Effective: September 14, 2026",
                  style = MaterialTheme.typography.labelMedium,
                  color = GlassTextMuted,
                )
                Text(
                  "• Account Security: Your account info is stored safely via encrypted local Room DB and Firebase Firestore TLS encryption.",
                  style = MaterialTheme.typography.bodySmall,
                  color = GlassTextSecondary,
                )
                Text(
                  "• Zero Storage Permission: CourseHub utilizes the official Android Photo Picker. We do NOT access your full gallery or storage.",
                  style = MaterialTheme.typography.bodySmall,
                  color = GlassTextSecondary,
                )
                Text(
                  "• Payment Proof: Transaction IDs and receipt screenshots are used strictly by CourseHub administration to verify enrollment access.",
                  style = MaterialTheme.typography.bodySmall,
                  color = GlassTextSecondary,
                )
                Text(
                  "• Developer Contact: wahaballied6@gmail.com | WhatsApp: +92 317 2414712",
                  style = MaterialTheme.typography.bodySmall,
                  color = GlassAccentBlue,
                )
              }
            },
            confirmButton = {
              TextButton(onClick = { showPrivacyDialog = false }) {
                Text("Understood", fontWeight = FontWeight.Bold, color = GlassAccentBlue)
              }
            },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = GlassTextSecondary,
          )
        }
      }
    }
  }
}

@Composable
private fun AccountDetailRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  valueColor: Color = GlassTextPrimary,
  isBold: Boolean = false,
  onCopy: () -> Unit,
) {
  Surface(
    color = Color(0x330F172A),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, GlassBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = GlassAccentBlue,
          modifier = Modifier.size(18.dp),
        )
        Column {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTextMuted,
          )
          Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }

      IconButton(
        onClick = onCopy,
        modifier =
          Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x263B82F6)),
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = "Copy $label",
          tint = GlassAccentBlue,
          modifier = Modifier.size(16.dp),
        )
      }
    }
  }
}

@Composable
private fun EnrolledCourseCard(
  enrollment: EnrollmentEntity,
  courseDao: CourseDao,
  onContinueCourse: (CourseEntity) -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val formattedEnrolledDate = remember(enrollment.enrolledAt) {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    sdf.format(Date(enrollment.enrolledAt))
  }

  val isPending = enrollment.status.equals("Pending", ignoreCase = true)
  val isAccepted = enrollment.status.equals("Accepted", ignoreCase = true) || enrollment.status.equals("Active", ignoreCase = true)
  val isRejected = enrollment.status.equals("Rejected", ignoreCase = true)

  val statusColor = when {
    isAccepted -> Color(0xFF34D399)
    isPending -> Color(0xFFFBBF24)
    else -> Color(0xFFF87171)
  }

  Surface(
    color = Color(0x260F172A),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, if (isPending) Color(0x66F59E0B) else Color(0x333B82F6)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = enrollment.courseTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = GlassTextPrimary,
          modifier = Modifier.weight(1f),
        )

        Surface(
          color = when {
            isAccepted -> Color(0x3310B981)
            isPending -> Color(0x33F59E0B)
            else -> Color(0x33EF4444)
          },
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(
            1.dp,
            when {
              isAccepted -> Color(0x6610B981)
              isPending -> Color(0x66F59E0B)
              else -> Color(0x66EF4444)
            },
          ),
        ) {
          Text(
            text = when {
              isPending -> "Pending Approval"
              isAccepted -> "Approved & Enrolled"
              else -> "Rejected"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Applied: $formattedEnrolledDate",
          style = MaterialTheme.typography.bodySmall,
          color = GlassTextMuted,
        )

        if (enrollment.price.isNotBlank()) {
          Text(
            text = "Total Fees: ${enrollment.price}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6EE7B7),
          )
        }
      }

      if (enrollment.transactionId.isNotBlank()) {
        Text(
          text = "Receipt/TID: ${enrollment.transactionId}",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF93C5FD),
        )
      }

      if (isAccepted) {
        Button(
          onClick = {
            coroutineScope.launch {
              val course = withContext(Dispatchers.IO) {
                courseDao.getCourseById(enrollment.courseId.toString())
              }
              if (course != null) {
                onContinueCourse(course)
              }
            }
          },
          modifier = Modifier.fillMaxWidth().height(40.dp).testTag("continue_course_${enrollment.courseId}"),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Continue Course Classroom ▶", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}

package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CourseDao
import com.example.data.CourseEntity
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
import com.example.util.AppImage
import com.example.util.ImageHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
  initialCourse: CourseEntity?,
  userSession: UserSession?,
  courseDao: CourseDao,
  onBack: () -> Unit,
  onOpenAuth: () -> Unit,
  onSubmittedSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  // Live list of all courses for the dropdown menu
  val allCourses by courseDao.getAllCourses().collectAsState(initial = emptyList())

  // Selected course state (defaults to passed initialCourse or first course in DB)
  var selectedCourse by remember(allCourses, initialCourse) {
    mutableStateOf(
      initialCourse ?: allCourses.firstOrNull()
    )
  }

  var isCourseDropdownExpanded by remember { mutableStateOf(false) }

  // Easypaisa & WhatsApp Payment details
  val easypaisaAccountNo = "03122844141"
  val easypaisaAccountTitle = "SHAISTA KHATOON"
  val adminWhatsappNo = "03172414712"

  // Input states
  var enteredUserId by remember(userSession) { mutableStateOf(userSession?.userId ?: "") }
  var enteredUsername by remember(userSession) { mutableStateOf(userSession?.username ?: "") }
  var transactionId by remember { mutableStateOf("") }
  var senderPhone by remember { mutableStateOf("") }
  var receiptImageBase64 by remember { mutableStateOf<String?>(null) }
  var receiptUri by remember { mutableStateOf<Uri?>(null) }
  var isCompressingReceipt by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var isSuccessSubmitted by remember { mutableStateOf(false) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      receiptUri = uri
      isCompressingReceipt = true
      coroutineScope.launch {
        val base64 = withContext(Dispatchers.IO) {
          ImageHelper.compressUriToBase64(context, uri, maxDimension = 800, quality = 80)
        }
        receiptImageBase64 = base64
        isCompressingReceipt = false
      }
    }
  }

  val activeCourse = selectedCourse ?: initialCourse

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

  val openWhatsApp = { message: String ->
    try {
      val cleanPhone = adminWhatsappNo.replace("-", "").replace(" ", "").let {
        if (it.startsWith("0")) "92" + it.substring(1) else it
      }
      val encodedMsg = Uri.encode(message)
      val uri = Uri.parse("https://wa.me/$cleanPhone?text=$encodedMsg")
      val intent = Intent(Intent.ACTION_VIEW, uri)
      context.startActivity(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "WhatsApp is not installed. Number copied: $adminWhatsappNo", Toast.LENGTH_SHORT).show()
      copyToClipboard("WhatsApp Support", adminWhatsappNo)
    }
  }

  val handleProcessPayment: () -> Unit = {
    val cleanUserId = enteredUserId.trim()
    val cleanUsername = enteredUsername.trim()

    if (activeCourse == null) {
      validationError = "Please select a course to enroll in from the dropdown menu."
    } else if (cleanUserId.isBlank()) {
      validationError = "User ID is required before processing any payment."
    } else {
      validationError = null
      isSuccessSubmitted = true

      // Save enrollment to Room database memory in real-time
      val userToLink = if (cleanUsername.isNotBlank()) cleanUsername else (userSession?.username ?: "Guest_${cleanUserId.takeLast(4)}")
      val userEmailToLink = userSession?.email ?: ""
      val coursePrice = activeCourse.price
      val courseName = activeCourse.title
      val currentTid = transactionId.trim().ifBlank { "TID-${System.currentTimeMillis().toString().takeLast(6)}" }

      coroutineScope.launch(Dispatchers.IO) {
        try {
          val enrollment =
            com.example.data.EnrollmentEntity(
              username = userToLink,
              userId = cleanUserId,
              userEmail = userEmailToLink,
              courseId = activeCourse.id.hashCode().toLong(),
              courseTitle = courseName,
              price = coursePrice,
              paymentMethod = "EasyPaisa",
              transactionId = currentTid,
              enrolledAt = System.currentTimeMillis(),
              status = "Pending",
              receiptImage = receiptImageBase64
                ?: receiptUri?.let { ImageHelper.compressUriToBase64(context, it, maxDimension = 800, quality = 80) }
                ?: "",
            )
          val insertedId = courseDao.insertEnrollment(enrollment)
          val savedEnrollment = enrollment.copy(id = insertedId)

          // Sync to Firebase Firestore so Admin on other devices gets this in real-time
          com.example.data.FirebaseFirestoreService.saveEnrollmentToCloud(savedEnrollment)
          // Also sync to Universal Cloud REST registry for 100% guarantee across devices using persistent syncScope
          com.example.data.CloudSyncService.syncEnrollmentToUniversalCloud(savedEnrollment)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }

      coroutineScope.launch {
        snackbarHostState.showSnackbar("Enrollment application submitted for review (ID: $cleanUserId)")
      }
    }
  }

  Scaffold(
    containerColor = GlassBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Payment Method & Enrollment",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GlassTextPrimary,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("payment_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = GlassSurfaceDark),
      )
    },
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      AmbientGlowBackground()

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        if (isSuccessSubmitted && activeCourse != null) {
          // Success Verification Card
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0x6610B981)), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(20.dp),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0x3310B981)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color(0xFF34D399),
                  modifier = Modifier.size(36.dp),
                )
              }

              Text(
                text = "Payment Request Submitted",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary,
              )

              Text(
                text =
                  "Your User ID and enrollment request for '${activeCourse.title}' have been registered. Our admin team will review and grant your access shortly.",
                style = MaterialTheme.typography.bodyMedium,
                color = GlassTextSecondary,
                textAlign = TextAlign.Center,
              )

              Surface(
                color = Color(0x263B82F6),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x6660A5FA)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    "Submitted User ID:",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSecondary,
                  )
                  Text(
                    enteredUserId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GlassAccentBlue,
                  )
                  Text(
                    "Enrolled Course: ${activeCourse.title} (${activeCourse.price})",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassTextMuted,
                  )
                  if (transactionId.isNotBlank()) {
                    Text(
                      "Easypaisa TID: $transactionId",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF34D399),
                    )
                  }

                  val activeSuccessReceipt = receiptImageBase64 ?: receiptUri
                  if (activeSuccessReceipt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Box(
                        modifier = Modifier
                          .size(width = 50.dp, height = 40.dp)
                          .clip(RoundedCornerShape(6.dp))
                          .border(BorderStroke(1.dp, Color(0xFF10B981)), RoundedCornerShape(6.dp)),
                      ) {
                        AppImage(
                          model = activeSuccessReceipt,
                          contentDescription = "Receipt preview",
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Crop,
                        )
                      }
                      Text(
                        "Payment Screenshot Attached ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }

              // Direct WhatsApp confirmation button
              Button(
                onClick = {
                  val msg = "Hello CourseHub Support, I have submitted payment for '${activeCourse.title}' with User ID: $enteredUserId${if (transactionId.isNotBlank()) " (TID: $transactionId)" else ""}."
                  openWhatsApp(msg)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                shape = RoundedCornerShape(12.dp),
              ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact Support on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
              }

              OutlinedButton(
                onClick = onSubmittedSuccess,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlassBorder),
              ) {
                Text("Return to Courses", fontWeight = FontWeight.Bold, color = GlassTextPrimary)
              }
            }
          }
        } else {
          // 1. Course Selection Dropdown Menu & Total Fees Calculation
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(18.dp),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.MenuBook,
                  contentDescription = null,
                  tint = GlassAccentBlue,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "Select Desired Course",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextPrimary,
                )
              }

              // Exposed Dropdown Menu Box
              ExposedDropdownMenuBox(
                expanded = isCourseDropdownExpanded,
                onExpandedChange = { isCourseDropdownExpanded = !isCourseDropdownExpanded },
                modifier = Modifier.fillMaxWidth(),
              ) {
                OutlinedTextField(
                  value = activeCourse?.title ?: "Select a course...",
                  onValueChange = {},
                  readOnly = true,
                  trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCourseDropdownExpanded)
                  },
                  modifier =
                    Modifier
                      .menuAnchor()
                      .fillMaxWidth()
                      .testTag("course_dropdown_selector"),
                  shape = RoundedCornerShape(12.dp),
                  colors =
                    OutlinedTextFieldDefaults.colors(
                      focusedTextColor = GlassTextPrimary,
                      unfocusedTextColor = GlassTextPrimary,
                      focusedBorderColor = GlassAccentBlue,
                      unfocusedBorderColor = GlassBorder,
                      focusedContainerColor = GlassSurfaceDark,
                      unfocusedContainerColor = Color(0x330F172A),
                    ),
                )

                ExposedDropdownMenu(
                  expanded = isCourseDropdownExpanded,
                  onDismissRequest = { isCourseDropdownExpanded = false },
                  modifier = Modifier.background(GlassSurfaceDark),
                ) {
                  allCourses.forEach { courseItem ->
                    DropdownMenuItem(
                      text = {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            courseItem.title,
                            color = GlassTextPrimary,
                            modifier = Modifier.weight(1f),
                          )
                          Text(
                            courseItem.price,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold,
                          )
                        }
                      },
                      onClick = {
                        selectedCourse = courseItem
                        isCourseDropdownExpanded = false
                      },
                    )
                  }
                }
              }

              // Total Fees Display Box
              if (activeCourse != null) {
                Surface(
                  color = Color(0x2610B981),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, Color(0x6610B981)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column {
                      Text(
                        "Total Amount Required to Enroll:",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextMuted,
                      )
                      Text(
                        activeCourse.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary,
                      )
                    }

                    Text(
                      text = activeCourse.price,
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.ExtraBold,
                      color = Color(0xFF6EE7B7),
                    )
                  }
                }
              }
            }
          }

          // 2. Simple Instructions: Send Amount to Easypaisa Account
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(18.dp),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(Color(0x3310B981)),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    "1",
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                  )
                }
                Text(
                  text = "Send ${activeCourse?.price ?: "amount"} to Easypaisa Account",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextPrimary,
                )
              }

              Surface(
                color = Color(0x330F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column {
                    Text(
                      "Easypaisa Account Number",
                      style = MaterialTheme.typography.labelSmall,
                      color = GlassTextMuted,
                    )
                    Text(
                      easypaisaAccountNo,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                    )
                    Text(
                      "Account Title: $easypaisaAccountTitle",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF93C5FD),
                      fontWeight = FontWeight.SemiBold,
                    )
                  }

                  IconButton(
                    onClick = {
                      copyToClipboard(
                        "Easypaisa Account",
                        easypaisaAccountNo,
                      )
                    },
                    modifier =
                      Modifier
                        .clip(CircleShape)
                        .background(Color(0x263B82F6)),
                  ) {
                    Icon(
                      Icons.Default.ContentCopy,
                      contentDescription = "Copy Account",
                      tint = GlassAccentBlue,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }

          // 2. Upload Payment Screenshot / Receipt (Media Picker)
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .border(
                  BorderStroke(1.dp, if (receiptImageBase64 != null) Color(0x6610B981) else GlassBorder),
                  RoundedCornerShape(18.dp),
                ),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(18.dp),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(Color(0x3360A5FA)),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    "2",
                    color = GlassAccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                  )
                }
                Text(
                  text = "Upload Payment Screenshot / Receipt",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextPrimary,
                )
              }

              Text(
                text = "Attach a screenshot of your Easypaisa transfer receipt so our team can approve your enrollment quickly.",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary,
              )

              val activeReceiptModel = receiptImageBase64 ?: receiptUri
              if (activeReceiptModel != null) {
                // Receipt Preview
                Box(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .height(180.dp)
                      .clip(RoundedCornerShape(14.dp))
                      .border(BorderStroke(1.5.dp, Color(0xFF10B981)), RoundedCornerShape(14.dp))
                      .background(GlassSurfaceDark),
                ) {
                  AppImage(
                    model = activeReceiptModel,
                    contentDescription = "Payment receipt screenshot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                  )

                  Surface(
                    color = Color(0xDD10B981),
                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                    modifier = Modifier.align(Alignment.TopStart),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                      Text("Screenshot Attached ✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }

                  IconButton(
                    onClick = {
                      receiptImageBase64 = null
                      receiptUri = null
                    },
                    modifier =
                      Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(Color(0xDDEF4444), CircleShape),
                  ) {
                    Icon(
                      Icons.Default.Close,
                      contentDescription = "Remove Screenshot",
                      tint = Color.White,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  OutlinedButton(
                    onClick = {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                  ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = GlassAccentBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Screenshot", color = GlassTextPrimary, fontSize = 12.sp)
                  }

                  OutlinedButton(
                    onClick = { receiptImageBase64 = null },
                    modifier = Modifier.height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x66EF4444)),
                  ) {
                    Text("Remove", color = Color(0xFFF87171), fontSize = 12.sp)
                  }
                }
              } else {
                Button(
                  onClick = {
                    photoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  enabled = !isCompressingReceipt,
                  modifier = Modifier.fillMaxWidth().height(48.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = GlassAccentBlue),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  if (isCompressingReceipt) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(18.dp),
                      color = Color.White,
                      strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimizing Screenshot...", fontSize = 13.sp)
                  } else {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Screenshot from Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  }
                }
              }
            }
          }

          // 3. Contact Support via WhatsApp (03172414712)
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0x6622C55E)), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(18.dp),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(Color(0x3322C55E)),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    "3",
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                  )
                }
                Text(
                  text = "Need Help? Contact Support (WhatsApp)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextPrimary,
                )
              }

              Surface(
                color = Color(0x330F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column {
                      Text(
                        "Official WhatsApp Support",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextMuted,
                      )
                      Text(
                        adminWhatsappNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80),
                      )
                      Text(
                        "Available 24/7 for payment queries & verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary,
                      )
                    }

                    IconButton(
                      onClick = { copyToClipboard("WhatsApp Support", adminWhatsappNo) },
                      modifier =
                        Modifier
                          .clip(CircleShape)
                          .background(Color(0x2622C55E)),
                    ) {
                      Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy WhatsApp",
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }

                  Button(
                    onClick = {
                      val msg = "Hello CourseHub Support, I need help with payment for: ${activeCourse?.title ?: "Course"} (User ID: ${enteredUserId.ifBlank { "Not provided" }})."
                      openWhatsApp(msg)
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat on WhatsApp (03172414712)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                  }
                }
              }
            }
          }

          // 4. Required Field: User ID (Mandatory before processing payment)
          Card(
            modifier =
              Modifier
                .fillMaxWidth()
                .border(
                  BorderStroke(
                    1.dp,
                    if (validationError != null) Color(0xFFEF4444) else Color(0x66F59E0B),
                  ),
                  RoundedCornerShape(18.dp),
                ),
            colors = CardDefaults.cardColors(containerColor = GlassSurface),
            shape = RoundedCornerShape(18.dp),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.Info,
                  contentDescription = null,
                  tint = Color(0xFFFBBF24),
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "4. Enter Your User ID (Required) & Details",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFBBF24),
                )
              }

              Text(
                text =
                  "Your User ID is required before processing any payment so our team can verify and link your course access.",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextSecondary,
              )

              // MANDATORY User ID Field
              OutlinedTextField(
                value = enteredUserId,
                onValueChange = {
                  enteredUserId = it
                  if (it.isNotBlank()) validationError = null
                },
                label = { Text("User ID (Required) *") },
                placeholder = { Text("e.g. USER-7890 or student ID") },
                leadingIcon = {
                  Icon(
                    Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = if (enteredUserId.isNotBlank()) GlassAccentBlue else Color(0xFFFBBF24),
                  )
                },
                isError = validationError != null && enteredUserId.isBlank(),
                singleLine = true,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .testTag("payment_user_id_input"),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GlassTextPrimary,
                    unfocusedTextColor = GlassTextPrimary,
                    focusedBorderColor = GlassAccentBlue,
                    unfocusedBorderColor =
                      if (enteredUserId.isBlank()) Color(0x88F59E0B) else GlassBorder,
                    errorBorderColor = Color(0xFFEF4444),
                  ),
              )

              // Name / Username Field
              OutlinedTextField(
                value = enteredUsername,
                onValueChange = { enteredUsername = it },
                label = { Text("Your Name / Username (Optional)") },
                placeholder = { Text("e.g. Ali Ahmed") },
                leadingIcon = {
                  Icon(Icons.Default.Person, contentDescription = null, tint = GlassTextMuted)
                },
                singleLine = true,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .testTag("payment_username_input"),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GlassTextPrimary,
                    unfocusedTextColor = GlassTextPrimary,
                    focusedBorderColor = GlassAccentBlue,
                    unfocusedBorderColor = GlassBorder,
                  ),
              )

              // Optional Easypaisa TID Field
              OutlinedTextField(
                value = transactionId,
                onValueChange = { transactionId = it },
                label = { Text("Easypaisa Transaction ID / TID (Optional)") },
                placeholder = { Text("e.g. 11-digit transaction ID") },
                leadingIcon = {
                  Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = GlassTextMuted,
                  )
                },
                singleLine = true,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .testTag("payment_tid_input"),
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GlassTextPrimary,
                    unfocusedTextColor = GlassTextPrimary,
                    focusedBorderColor = GlassAccentBlue,
                    unfocusedBorderColor = GlassBorder,
                  ),
              )

              if (validationError != null) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.padding(top = 4.dp),
                ) {
                  Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = validationError!!,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Button(
                onClick = handleProcessPayment,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = GlassPrimaryBlue)
                    .testTag("submit_payment_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
                shape = RoundedCornerShape(14.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(Icons.Default.Send, contentDescription = null)
                  Text(
                    text = "Process Payment & Submit Verification",
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
}

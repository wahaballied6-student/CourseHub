package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import coil.compose.AsyncImage
import com.example.util.ImageHelper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CourseDao
import com.example.data.CourseEntity
import com.example.data.ReviewEntity
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
import kotlinx.coroutines.launch

@Composable
fun CourseLearningScreen(
  course: CourseEntity,
  courseDao: CourseDao,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Reviews from Room
  val reviews by courseDao.getReviewsForCourse(course.id).collectAsState(initial = emptyList())

  // Parse attached image names, Base64 uploads, or fallback drawables
  val rawImageTokens = remember(course.imageUrls) {
    ImageHelper.splitImageUrls(course.imageUrls)
  }
  val resolvedImages = remember(rawImageTokens, course.drawableName) {
    if (rawImageTokens.isNotEmpty()) {
      rawImageTokens.map { token ->
        ImageHelper.resolveImageModel(token)
      }
    } else {
      listOf(
        when (course.drawableName) {
          "img_course_design" -> R.drawable.img_course_design
          "img_course_business" -> R.drawable.img_course_business
          else -> R.drawable.img_course_tech
        }
      )
    }
  }

  val heroDrawable = when (course.drawableName) {
    "img_course_design" -> R.drawable.img_course_design
    "img_course_business" -> R.drawable.img_course_business
    else -> R.drawable.img_course_tech
  }

  // Interactive Review Submission State
  var showReviewDialog by remember { mutableStateOf(false) }
  var reviewAuthorName by remember { mutableStateOf("") }
  var reviewRole by remember { mutableStateOf("Enrolled Student") }
  var reviewRating by remember { mutableIntStateOf(5) }
  var reviewComment by remember { mutableStateOf("") }
  var isSubmittingReview by remember { mutableStateOf(false) }

  val handleOpenCourseLink: () -> Unit = {
    val targetUrl = course.accessUrl.trim().let {
      if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
    }
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
      context.startActivity(intent)
    } catch (e: Exception) {
      coroutineScope.launch {
        snackbarHostState.showSnackbar("Unable to launch course URL: $targetUrl")
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = GlassBackground,
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState()),
    ) {
      // Top App Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
            .border(BorderStroke(1.dp, GlassBorder), CircleShape)
            .testTag("learning_screen_back_btn"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
          )
        }

        Text(
          text = "Course Workspace",
          style = MaterialTheme.typography.titleMedium,
          color = GlassTextPrimary,
          fontWeight = FontWeight.Bold,
        )

        // Status chip
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x3310B981))
            .border(BorderStroke(1.dp, Color(0x6610B981)), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
          Text(
            text = "Enrolled & Approved ✓",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF34D399),
            fontWeight = FontWeight.Bold,
          )
        }
      }

      // Course Hero Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .height(200.dp)
          .clip(RoundedCornerShape(24.dp))
          .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(24.dp)),
      ) {
        AppImage(
          model = resolvedImages.firstOrNull() ?: heroDrawable,
          contentDescription = course.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          fallbackResId = heroDrawable,
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xDD0B1120), Color(0xFF0F172A)),
              )
            ),
        )
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
          verticalArrangement = Arrangement.Bottom,
        ) {
          Text(
            text = course.category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color(course.colorHex),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = course.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text("Instructor: ${course.instructor}", style = MaterialTheme.typography.bodySmall, color = GlassTextSecondary)
            Text("•", color = GlassTextMuted)
            Text(course.duration, style = MaterialTheme.typography.bodySmall, color = GlassTextSecondary)
            Text("•", color = GlassTextMuted)
            Text(course.level, style = MaterialTheme.typography.bodySmall, color = Color(course.colorHex))
          }
        }
      }

      // Primary Action Bar: Access Course Button
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
        border = BorderStroke(1.dp, GlassBorder),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.PlayCircle,
              contentDescription = null,
              tint = Color(0xFF34D399),
              modifier = Modifier.size(24.dp),
            )
            Text(
              text = "Official Learning Portal",
              style = MaterialTheme.typography.titleMedium,
              color = GlassTextPrimary,
              fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Your admission has been confirmed. Click below to launch your classroom lectures, assignments, and live mentorship portal.",
            style = MaterialTheme.typography.bodySmall,
            color = GlassTextSecondary,
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = handleOpenCourseLink,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("access_course_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Text(
                text = "Access Course Now",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }

      // Course Summary Section
      LearningContentCard(
        title = "Course Summary",
        icon = Icons.Default.Info,
        iconTint = GlassAccentBlue,
      ) {
        val summaryText = course.summary.ifBlank { course.description }
        Text(
          text = summaryText,
          style = MaterialTheme.typography.bodyMedium,
          color = GlassTextSecondary,
          lineHeight = 22.sp,
        )
      }

      // Course Objectives Section
      if (course.objectives.isNotBlank()) {
        LearningContentCard(
          title = "Course Objectives",
          icon = Icons.Default.Flag,
          iconTint = Color(0xFFFBBF24),
        ) {
          Text(
            text = course.objectives,
            style = MaterialTheme.typography.bodyMedium,
            color = GlassTextSecondary,
            lineHeight = 22.sp,
          )
        }
      }

      // What You'll Learn Section
      if (course.whatYouWillLearn.isNotBlank()) {
        LearningContentCard(
          title = "What You'll Learn",
          icon = Icons.Default.Lightbulb,
          iconTint = Color(0xFFF472B6),
        ) {
          Text(
            text = course.whatYouWillLearn,
            style = MaterialTheme.typography.bodyMedium,
            color = GlassTextSecondary,
            lineHeight = 22.sp,
          )
        }
      }

      // Attached Course Images Gallery
      if (resolvedImages.isNotEmpty()) {
        LearningContentCard(
          title = "Course Visuals & Materials",
          icon = Icons.Default.PhotoLibrary,
          iconTint = Color(0xFF60A5FA),
        ) {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
          ) {
            items(resolvedImages) { model ->
              Box(
                modifier = Modifier
                  .size(width = 220.dp, height = 130.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp))
                  .background(GlassSurfaceDark),
              ) {
                AppImage(
                  model = model,
                  contentDescription = "Course visual",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop,
                )
              }
            }
          }
        }
      }

      // Reviews Section
      LearningContentCard(
        title = "Student Reviews (${reviews.size})",
        icon = Icons.Default.Star,
        iconTint = Color(0xFFFBBF24),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // Add Review Action Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
            if (reviews.isNotEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                Text(
                  text = String.format(java.util.Locale.US, "%.1f / 5.0", avgRating),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = GlassTextPrimary,
                )
              }
            } else {
              Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
              onClick = { showReviewDialog = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GlassPrimaryBlue),
              modifier = Modifier.height(36.dp).testTag("write_review_button"),
            ) {
              Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Write Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          if (reviews.isEmpty()) {
            Text(
              text = "No reviews submitted yet for this course. Be the first to leave feedback!",
              style = MaterialTheme.typography.bodySmall,
              color = GlassTextMuted,
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              reviews.forEach { review ->
                Card(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = Color(0x221E293B)),
                  border = BorderStroke(1.dp, GlassBorder),
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Column {
                        Text(
                          text = review.authorName,
                          style = MaterialTheme.typography.labelMedium,
                          color = GlassTextPrimary,
                          fontWeight = FontWeight.Bold,
                        )
                        if (review.role.isNotBlank()) {
                          Text(
                            text = review.role,
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTextMuted,
                          )
                        }
                      }
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(review.rating.toInt().coerceIn(1, 5)) {
                          Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp),
                          )
                        }
                      }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = review.comment,
                      style = MaterialTheme.typography.bodySmall,
                      color = GlassTextSecondary,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Review Submission Dialog
      if (showReviewDialog) {
        AlertDialog(
          onDismissRequest = { if (!isSubmittingReview) showReviewDialog = false },
          containerColor = GlassSurfaceDark,
          titleContentColor = GlassTextPrimary,
          textContentColor = GlassTextSecondary,
          title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.RateReview, contentDescription = null, tint = GlassAccentBlue)
              Text("Submit Course Review", fontWeight = FontWeight.Bold)
            }
          },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = "Share your learning experience for '${course.title}'",
                style = MaterialTheme.typography.bodySmall,
                color = GlassTextMuted,
              )

              // Star Rating Picker
              Column {
                Text("Your Rating:", style = MaterialTheme.typography.labelMedium, color = GlassTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                  (1..5).forEach { starIndex ->
                    IconButton(
                      onClick = { reviewRating = starIndex },
                      modifier = Modifier.size(36.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating $starIndex",
                        tint = if (starIndex <= reviewRating) Color(0xFFFBBF24) else Color(0x44CBD5E1),
                        modifier = Modifier.size(28.dp),
                      )
                    }
                  }
                  Text(
                    text = "$reviewRating / 5",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                  )
                }
              }

              OutlinedTextField(
                value = reviewAuthorName,
                onValueChange = { reviewAuthorName = it },
                label = { Text("Your Name") },
                placeholder = { Text("e.g. Alex M.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("review_author_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GlassAccentBlue,
                  unfocusedBorderColor = GlassBorder,
                  focusedTextColor = GlassTextPrimary,
                  unfocusedTextColor = GlassTextPrimary,
                ),
              )

              OutlinedTextField(
                value = reviewComment,
                onValueChange = { reviewComment = it },
                label = { Text("Your Review & Feedback") },
                placeholder = { Text("What did you learn? How was the instructor?") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth().testTag("review_comment_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GlassAccentBlue,
                  unfocusedBorderColor = GlassBorder,
                  focusedTextColor = GlassTextPrimary,
                  unfocusedTextColor = GlassTextPrimary,
                ),
              )
            }
          },
          confirmButton = {
            Button(
              onClick = {
                val author = reviewAuthorName.trim().ifBlank { "Verified Student" }
                val comment = reviewComment.trim()
                if (comment.isBlank()) {
                  coroutineScope.launch {
                    snackbarHostState.showSnackbar("Please write a brief feedback comment before submitting.")
                  }
                  return@Button
                }

                isSubmittingReview = true
                coroutineScope.launch {
                  val newReview = ReviewEntity(
                    courseId = course.id,
                    authorName = author,
                    role = reviewRole,
                    rating = reviewRating.toFloat(),
                    comment = comment,
                    timestamp = System.currentTimeMillis(),
                  )
                  courseDao.insertReview(newReview)
                  // Push review to Firestore cloud so all devices get it
                  com.example.data.FirebaseFirestoreService.saveReviewToCloud(newReview)

                  // Recalculate and update course rating
                  val currentReviews = reviews + newReview
                  val newAvg = if (currentReviews.isNotEmpty()) {
                    currentReviews.map { it.rating.toDouble() }.average()
                  } else {
                    reviewRating.toDouble()
                  }
                  val updatedCourse = course.copy(
                    rating = newAvg,
                    reviewCount = currentReviews.size,
                  )
                  courseDao.updateCourse(updatedCourse)
                  com.example.data.FirebaseFirestoreService.saveCourseToCloud(updatedCourse)

                  isSubmittingReview = false
                  showReviewDialog = false
                  reviewComment = ""
                  snackbarHostState.showSnackbar("Thank you! Your review has been published.")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("submit_review_confirm_button"),
            ) {
              Text("Publish Review", fontWeight = FontWeight.Bold)
            }
          },
          dismissButton = {
            TextButton(
              onClick = { showReviewDialog = false },
            ) {
              Text("Cancel", color = GlassTextSecondary)
            }
          },
        )
      }

      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
private fun LearningContentCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = GlassSurface),
    border = BorderStroke(1.dp, GlassBorder),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = GlassTextPrimary,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

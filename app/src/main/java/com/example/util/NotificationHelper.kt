package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
  private const val CHANNEL_ADMIN_ID = "admin_enrollment_requests"
  private const val CHANNEL_ADMIN_NAME = "Enrollment Requests"
  private const val CHANNEL_ADMIN_DESC = "Notifications for admin regarding new course enrollment applications"

  private const val CHANNEL_USER_ID = "user_enrollment_status"
  private const val CHANNEL_USER_NAME = "Enrollment Status Updates"
  private const val CHANNEL_USER_DESC = "Notifications regarding approval or rejection of your course enrollment"

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      // Admin Channel
      val adminChannel =
        NotificationChannel(
          CHANNEL_ADMIN_ID,
          CHANNEL_ADMIN_NAME,
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = CHANNEL_ADMIN_DESC
          enableVibration(true)
        }

      // User Status Channel
      val userChannel =
        NotificationChannel(
          CHANNEL_USER_ID,
          CHANNEL_USER_NAME,
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = CHANNEL_USER_DESC
          enableVibration(true)
        }

      notificationManager.createNotificationChannel(adminChannel)
      notificationManager.createNotificationChannel(userChannel)
    }
  }

  fun showNewEnrollmentAdminNotification(
    context: Context,
    username: String,
    userId: String,
    courseTitle: String,
    amount: String,
    enrollmentId: Long,
  ) {
    createNotificationChannels(context)

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        enrollmentId.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ADMIN_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("🔔 New Enrollment: $username ($userId)")
        .setContentText("Course: $courseTitle | Amount: $amount")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(
              "Student '$username' (Account ID: $userId) submitted payment details for course '$courseTitle'. Total Fees: $amount. Open Admin panel to accept or reject this application."
            ),
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    try {
      NotificationManagerCompat.from(context).notify(enrollmentId.toInt() + 1000, notification)
    } catch (e: SecurityException) {
      // Permission not granted yet
      e.printStackTrace()
    }
  }

  fun showEnrollmentStatusUserNotification(
    context: Context,
    username: String,
    courseTitle: String,
    isAccepted: Boolean,
    enrollmentId: Long,
  ) {
    createNotificationChannels(context)

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        (enrollmentId + 5000).toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val title = if (isAccepted) "🎉 Enrollment Approved!" else "⚠️ Enrollment Update"
    val content =
      if (isAccepted) {
        "Congratulations $username! Your application for '$courseTitle' has been approved. You now have full access."
      } else {
        "Dear $username, your enrollment application for '$courseTitle' was not approved. Please contact support."
      }

    val notification =
      NotificationCompat.Builder(context, CHANNEL_USER_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    try {
      NotificationManagerCompat.from(context).notify(enrollmentId.toInt() + 2000, notification)
    } catch (e: SecurityException) {
      e.printStackTrace()
    }
  }
}

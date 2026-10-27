package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [CourseEntity::class, ReviewEntity::class, RecentSearchEntity::class, UserEntity::class, EnrollmentEntity::class],
  version = 7,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun courseDao(): CourseDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance =
          Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "coursehub_database",
          )
            .fallbackToDestructiveMigration()
            .build()
        INSTANCE = instance

        scope.launch(Dispatchers.IO) {
          try {
            val dao = instance.courseDao()
            populateInitialData(dao)
            // Purge any legacy hardcoded demo courses from local DB and cloud
            for (demoId in CloudSyncService.SYSTEM_COURSE_IDS) {
              dao.deleteCourseById(demoId)
              dao.deleteReviewsByCourse(demoId)
            }
            // Sync all cloud server registered accounts to local database
            CloudSyncService.syncAllUsersFromCloud(dao)
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }

        instance
      }
    }

    suspend fun populateInitialData(courseDao: CourseDao) {
      // No hardcoded demo courses or demo reviews are seeded so deleted/system courses never reappear.
      // Seed the only two allowed admin accounts (the owner Google accounts):
      try {
        courseDao.insertUser(
          UserEntity(
            username = "wahab",
            passwordHash = "wahab123",
            userId = "CH-10001",
            email = "wahaballied6@gmail.com",
            isAdmin = true,
          ),
        )
        courseDao.insertUser(
          UserEntity(
            username = "junaid",
            passwordHash = "junaid123",
            userId = "CH-72941",
            email = "junaidallied6@gmail.com",
            isAdmin = true,
          ),
        )
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}

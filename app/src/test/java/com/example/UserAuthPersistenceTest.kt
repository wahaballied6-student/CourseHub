package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CloudSyncService
import com.example.data.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserAuthPersistenceTest {

  private lateinit var db: AppDatabase

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun testPermanentAccountsOnlyContainTheTwoAdmins() {
    val usernames = CloudSyncService.PERMANENT_ACCOUNTS.map { it.username.lowercase() }.sorted()
    assertEquals(listOf("junaid", "wahab"), usernames)

    val wahabUser = CloudSyncService.PERMANENT_ACCOUNTS.first { it.username.equals("wahab", ignoreCase = true) }
    assertEquals("wahaballied6@gmail.com", wahabUser.email)
    assertTrue("Wahab must be an admin", wahabUser.isAdmin)

    val junaidUser = CloudSyncService.PERMANENT_ACCOUNTS.first { it.username.equals("junaid", ignoreCase = true) }
    assertEquals("junaidallied6@gmail.com", junaidUser.email)
    assertTrue("Junaid must be an admin", junaidUser.isAdmin)
  }

  @Test
  fun testLocalAndCloudUserLookup() = runBlocking {
    val dao = db.courseDao()

    // Test cloud sync seeding into Room
    CloudSyncService.syncAllUsersFromCloud(dao)

    val junaidInDb = dao.getUserByUsername("junaid")
    assertNotNull("Junaid should be synced into local database", junaidInDb)
    assertEquals("junaidallied6@gmail.com", junaidInDb?.email)
    assertTrue("Junaid must be an admin", junaidInDb?.isAdmin == true)

    val wahabInDb = dao.getUserByUsername("wahab")
    assertNotNull("Wahab should be synced into local database", wahabInDb)
    assertEquals("wahaballied6@gmail.com", wahabInDb?.email)
    assertTrue("Wahab must be an admin", wahabInDb?.isAdmin == true)
  }

  @Test
  fun testNewUserRegistrationAndRetrieval() = runBlocking {
    val dao = db.courseDao()
    val testUser = UserEntity(
      username = "TestDeviceUser",
      passwordHash = "pass456",
      userId = "CH-99999",
      email = "test@coursehub.edu",
      isAdmin = false
    )

    dao.insertUser(testUser)
    val fetched = dao.getUserByUsername("testdeviceuser")
    assertNotNull("Username query should be case-insensitive", fetched)
    assertEquals("pass456", fetched?.passwordHash)
  }

  @Test
  fun testGetUserByEmailIsCaseInsensitive() = runBlocking {
    val dao = db.courseDao()
    dao.insertUser(
      UserEntity(
        username = "SomeStudent",
        passwordHash = "pw",
        userId = "CH-88888",
        email = "Student@CourseHub.EDU",
      )
    )

    assertNotNull(dao.getUserByEmail("student@coursehub.edu"))
    assertNotNull(dao.getUserByEmail("STUDENT@COURSEHUB.EDU"))
    assertEquals("SomeStudent", dao.getUserByEmail("student@coursehub.edu")?.username)
    assertNull("Unknown email must not match", dao.getUserByEmail("nobody@nowhere.com"))
  }

  @Test
  fun testSignInResolvesAdminByEmail() = runBlocking {
    val dao = db.courseDao()

    // Exactly what the sign-in screen does when the user types their Gmail address.
    for (email in listOf("wahaballied6@gmail.com", "WAHABALLIED6@GMAIL.COM", "junaidallied6@gmail.com")) {
      val resolved = CloudSyncService.findUserOnCloud(email, dao)
      assertNotNull("Email '$email' must resolve to an account", resolved)
      assertTrue("Admin accounts must resolve as admins", resolved?.isAdmin == true)
    }
  }

  @Test
  fun testSignInResolvesLocalOnlyUserByEmailWithoutCloud() = runBlocking {
    val dao = db.courseDao()
    dao.insertUser(
      UserEntity(
        username = "LocalOnlyUser",
        passwordHash = "pw",
        userId = "CH-77777",
        email = "local.only@example.com",
      )
    )

    // Resolves via the local DB (permanent -> local -> cloud order), no network needed.
    val resolved = CloudSyncService.findUserOnCloud("local.only@example.com", dao)
    assertNotNull("Local user must resolve by email", resolved)
    assertEquals("LocalOnlyUser", resolved?.username)
  }

  @Test
  fun testPruneKeepsUsersMatchingPermanentEmails() = runBlocking {
    val dao = db.courseDao()
    // Simulates a Google sign-in stored under a display name different from the
    // registry username. It must survive pruning because its EMAIL is in the keep-set.
    val googleLinkedUser = UserEntity(
      username = "Wahab Allied",
      passwordHash = "GOOGLE_AUTH_TOKEN",
      userId = "CH-55555",
      email = "wahaballied6@gmail.com",
      isAdmin = true,
    )
    dao.insertUser(googleLinkedUser)

    CloudSyncService.syncAllUsersFromCloud(dao)

    assertNotNull(
      "User whose email matches a permanent account must never be pruned",
      dao.getUserByUsername("Wahab Allied"),
    )
  }
}
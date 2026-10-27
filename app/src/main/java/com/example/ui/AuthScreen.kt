package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudSyncService
import com.example.data.CourseDao
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.Image
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.util.GoogleAuthHelper
import com.example.util.GoogleAuthResult

enum class AuthMode {
  SIGN_IN,
  SIGN_UP,
}

@Composable
fun AuthScreen(
  courseDao: CourseDao,
  onAuthSuccess: (UserSession) -> Unit,
  onBackToLanding: () -> Unit,
) {
  val context = LocalContext.current
  var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
  var username by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  val coroutineScope = rememberCoroutineScope()

  // Proactively synchronize all registered accounts from cloud server on screen launch
  var cloudSyncCount by remember { mutableStateOf<Int?>(null) }
  var isSyncingCloud by remember { mutableStateOf(false) }

  val handleGoogleSignIn: () -> Unit = {
    errorMessage = null
    isLoading = true
    coroutineScope.launch {
      val result = GoogleAuthHelper.signInWithGoogle(context)
      when (result) {
        is GoogleAuthResult.Success -> {
          try {
            courseDao.insertUser(result.userEntity)
            com.example.data.FirebaseFirestoreService.saveUserToCloud(result.userEntity)
          } catch (e: Exception) {
            e.printStackTrace()
          }
          CloudSyncService.syncScope.launch {
            try {
              CloudSyncService.registerUserToCloud(result.userEntity, courseDao)
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
          withContext(Dispatchers.Main) {
            isLoading = false
            onAuthSuccess(result.session)
          }
        }
        is GoogleAuthResult.Cancelled -> {
          withContext(Dispatchers.Main) {
            isLoading = false
          }
        }
        is GoogleAuthResult.Error -> {
          withContext(Dispatchers.Main) {
            isLoading = false
            errorMessage = result.message
          }
        }
      }
    }
  }

  val syncCloudNow: () -> Unit = {
    isSyncingCloud = true
    coroutineScope.launch(Dispatchers.IO) {
      try {
        val synced = CloudSyncService.syncAllUsersFromCloud(courseDao)
        withContext(Dispatchers.Main) {
          cloudSyncCount = synced.size
          isSyncingCloud = false
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          isSyncingCloud = false
        }
      }
    }
  }

  LaunchedEffect(Unit) {
    syncCloudNow()
  }

  val handleAuth: () -> Unit = {
    val cleanUser = username.trim()
    val cleanPass = password.trim()
    val cleanEmail = email.trim()

    if (cleanUser.isBlank()) {
      errorMessage = "Please enter a valid username."
      successMessage = null
    } else if (cleanPass.isBlank()) {
      errorMessage = "Please enter your password."
      successMessage = null
    } else if (authMode == AuthMode.SIGN_UP && cleanPass.length < 4) {
      errorMessage = "Password must be at least 4 characters long."
      successMessage = null
    } else if (authMode == AuthMode.SIGN_UP && cleanPass != confirmPassword.trim()) {
      errorMessage = "Passwords do not match."
      successMessage = null
    } else {
      errorMessage = null
      isLoading = true

      coroutineScope.launch(Dispatchers.IO) {
        try {
          if (authMode == AuthMode.SIGN_UP) {
            // Check if username (or its email) already exists in permanent list, local database,
            // or cloud registry. The email check stops duplicate Google-linked accounts being
            // created under a different username.
            val existingCount = courseDao.countUsersWithUsername(cleanUser)
            val existingLocalUser = courseDao.getUserByUsername(cleanUser)
            val matchedPermanent = CloudSyncService.PERMANENT_ACCOUNTS.firstOrNull {
              it.username.equals(cleanUser, ignoreCase = true) ||
                (cleanEmail.isNotBlank() && it.email.equals(cleanEmail, ignoreCase = true))
            }
            val existingEmailUser = if (cleanEmail.isNotBlank()) courseDao.getUserByEmail(cleanEmail) else null
            val existingCloudUser = if (existingCount == 0 && existingLocalUser == null && matchedPermanent == null) {
              CloudSyncService.findUserOnCloud(cleanUser, courseDao)
            } else null

            if (existingCount > 0 || existingLocalUser != null || matchedPermanent != null || existingCloudUser != null) {
              withContext(Dispatchers.Main) {
                isLoading = false
                errorMessage = "Username '$cleanUser' is already taken. Please select another username or sign in."
              }
            } else if (existingEmailUser != null) {
              withContext(Dispatchers.Main) {
                isLoading = false
                errorMessage = "The email '$cleanEmail' is already registered as '${existingEmailUser.username}'. Sign in with that account or use Continue with Google."
              }
            } else {
              // Generate permanent unique ID
              val assignedUserId = "CH-${(10000..99999).random()}"
              // Only the two owner Google accounts can be administrators
              val isAdmin = cleanEmail.isNotBlank() && cleanEmail.lowercase() in GoogleAuthHelper.ADMIN_EMAILS
              val newUser =
                UserEntity(
                  username = cleanUser,
                  passwordHash = cleanPass,
                  userId = assignedUserId,
                  email = if (cleanEmail.isNotBlank()) cleanEmail else "$cleanUser@coursehub.edu",
                  isAdmin = isAdmin,
                )
              courseDao.insertUser(newUser)

              // 1. Broadcast to cloud using non-cancellable process-wide scope so it never aborts on screen navigation
              CloudSyncService.syncScope.launch {
                try {
                  CloudSyncService.registerUserToCloud(newUser, courseDao)
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              }

              // 2. Await quick sync with cloud server
              try {
                withTimeoutOrNull(2000) {
                  CloudSyncService.registerUserToCloud(newUser, courseDao)
                }
              } catch (e: Exception) {
                e.printStackTrace()
              }

              val session =
                UserSession(
                  username = newUser.username,
                  userId = newUser.userId,
                  isAdmin = newUser.isAdmin,
                  email = newUser.email,
                  role = if (newUser.isAdmin) "Administrator" else "Student",
                  createdAt = newUser.createdAt,
                )

              withContext(Dispatchers.Main) {
                isLoading = false
                onAuthSuccess(session)
              }
            }
          } else {
            // SIGN IN FLOW (the entered identifier may be a username OR an email):
            // 1. Check permanent built-in accounts in memory first (immediate match on any phone)
            val matchedPermanent = CloudSyncService.PERMANENT_ACCOUNTS.firstOrNull {
              it.username.equals(cleanUser, ignoreCase = true) || it.email.equals(cleanUser, ignoreCase = true)
            }
            if (matchedPermanent != null) {
              try {
                courseDao.insertUser(matchedPermanent)
              } catch (e: Exception) {
                e.printStackTrace()
              }
            }

            // 2. Check local database by username, then by email
            var existingUser = matchedPermanent
              ?: courseDao.getUserByUsername(cleanUser)
              ?: courseDao.getUserByEmail(cleanUser)
            
            // 3. If account was created on another device and not yet in local DB, fetch from cloud server
            if (existingUser == null) {
              existingUser = CloudSyncService.findUserOnCloud(cleanUser, courseDao)
            }

            if (existingUser == null) {
              withContext(Dispatchers.Main) {
                isLoading = false
                errorMessage =
                  "No account found for '$cleanUser'. You can sign in with your username (e.g. wahab) or email, " +
                    "or tap Continue with Google with the matching Gmail account."
              }
            } else if (existingUser.passwordHash != cleanPass) {
              withContext(Dispatchers.Main) {
                isLoading = false
                errorMessage = "Incorrect password for '${existingUser.username}'. Please try again."
              }
            } else {
              val session =
                UserSession(
                  username = existingUser.username,
                  userId = existingUser.userId,
                  isAdmin = existingUser.isAdmin,
                  email = existingUser.email,
                  role = if (existingUser.isAdmin) "Administrator" else "Student",
                  createdAt = existingUser.createdAt,
                )
              withContext(Dispatchers.Main) {
                isLoading = false
                onAuthSuccess(session)
              }
            }
          }
        } catch (e: Exception) {
          withContext(Dispatchers.Main) {
            isLoading = false
            errorMessage = "Authentication error: ${e.localizedMessage ?: "Unknown error"}"
          }
        }
      }
    }
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(GlassBackground),
  ) {
    AmbientGlowBackground()

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top Navigation Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(
          onClick = onBackToLanding,
          modifier =
            Modifier
              .clip(CircleShape)
              .background(Color(0x1AFFFFFF))
              .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
              .testTag("auth_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
          )
        }

        Text(
          text = if (authMode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
          style = MaterialTheme.typography.titleMedium,
          color = GlassTextPrimary,
          fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.size(40.dp))
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Logo & Brand Header
      Box(
        modifier =
          Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
              Brush.linearGradient(
                listOf(GlassPrimaryBlue, Color(0xFF8B5CF6)),
              ),
            )
            .border(BorderStroke(1.dp, Color(0x40FFFFFF)), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.School,
          contentDescription = "CourseHub",
          tint = Color.White,
          modifier = Modifier.size(30.dp),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = if (authMode == AuthMode.SIGN_IN) "Welcome Back" else "Join CourseHub",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = GlassTextPrimary,
      )

      Text(
        text =
          if (authMode == AuthMode.SIGN_IN) {
            "Sign in with your verified credentials to access your courses"
          } else {
            "Create a new permanent account to enroll in tech courses"
          },
        style = MaterialTheme.typography.bodyMedium,
        color = GlassTextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Mode Switcher Tabs
      Surface(
        color = GlassSurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(4.dp),
        ) {
          Box(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (authMode == AuthMode.SIGN_IN) GlassPrimaryBlue else Color.Transparent)
                .clickable {
                  authMode = AuthMode.SIGN_IN
                  errorMessage = null
                  successMessage = null
                }
                .padding(vertical = 10.dp)
                .testTag("tab_sign_in"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Sign In",
              style = MaterialTheme.typography.labelLarge,
              color = if (authMode == AuthMode.SIGN_IN) Color.White else GlassTextMuted,
              fontWeight = FontWeight.Bold,
            )
          }

          Box(
            modifier =
              Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (authMode == AuthMode.SIGN_UP) GlassPrimaryBlue else Color.Transparent)
                .clickable {
                  authMode = AuthMode.SIGN_UP
                  errorMessage = null
                  successMessage = null
                }
                .padding(vertical = 10.dp)
                .testTag("tab_sign_up"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "Create Account",
              style = MaterialTheme.typography.labelLarge,
              color = if (authMode == AuthMode.SIGN_UP) Color.White else GlassTextMuted,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Form Card
      Card(
        modifier =
          Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
        shape = RoundedCornerShape(24.dp),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Official Google Sign-In (Genuine Google login flow)
          Button(
            onClick = handleGoogleSignIn,
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = Color(0xFF1F1F1F),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.size(12.dp))
              Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F1F1F),
              )
            }
          }

          // Divider
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(GlassBorder)
            )
            Text(
              text = if (authMode == AuthMode.SIGN_IN) "OR SIGN IN WITH PASSWORD" else "OR REGISTER WITH EMAIL",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
              color = GlassTextMuted,
              fontWeight = FontWeight.Bold,
            )
            Box(
              modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(GlassBorder)
            )
          }

          // Username
          OutlinedTextField(
            value = username,
            onValueChange = {
              username = it
              errorMessage = null
            },
            label = { Text("Username or Email") },
            placeholder = { Text(if (authMode == AuthMode.SIGN_IN) "wahab  or  wahaballied6@gmail.com" else "Choose a unique username") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = GlassAccentBlue,
              )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().testTag("auth_username_field"),
            shape = RoundedCornerShape(14.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurfaceDark,
                unfocusedContainerColor = Color(0x330F172A),
                focusedBorderColor = GlassAccentBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = GlassTextPrimary,
                unfocusedTextColor = GlassTextPrimary,
                focusedLabelColor = GlassAccentBlue,
                unfocusedLabelColor = GlassTextMuted,
                cursorColor = GlassAccentBlue,
              ),
          )

          // Email (Sign Up only)
          if (authMode == AuthMode.SIGN_UP) {
            OutlinedTextField(
              value = email,
              onValueChange = {
                email = it
                errorMessage = null
              },
              label = { Text("Email (Optional)") },
              placeholder = { Text("your.email@example.com") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Email,
                  contentDescription = null,
                  tint = GlassAccentBlue,
                )
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
              modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
              shape = RoundedCornerShape(14.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = GlassSurfaceDark,
                  unfocusedContainerColor = Color(0x330F172A),
                  focusedBorderColor = GlassAccentBlue,
                  unfocusedBorderColor = GlassBorder,
                  focusedTextColor = GlassTextPrimary,
                  unfocusedTextColor = GlassTextPrimary,
                  focusedLabelColor = GlassAccentBlue,
                  unfocusedLabelColor = GlassTextMuted,
                  cursorColor = GlassAccentBlue,
                ),
            )
          }

          // Password
          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              errorMessage = null
            },
            label = { Text("Password") },
            placeholder = { Text("Enter password") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = GlassAccentBlue,
              )
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = "Toggle password",
                  tint = GlassTextMuted,
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (authMode == AuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next),
            keyboardActions = KeyboardActions(onDone = {
              focusManager.clearFocus()
              handleAuth()
            }),
            modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
            shape = RoundedCornerShape(14.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurfaceDark,
                unfocusedContainerColor = Color(0x330F172A),
                focusedBorderColor = GlassAccentBlue,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = GlassTextPrimary,
                unfocusedTextColor = GlassTextPrimary,
                focusedLabelColor = GlassAccentBlue,
                unfocusedLabelColor = GlassTextMuted,
                cursorColor = GlassAccentBlue,
              ),
          )

          // Confirm Password (Sign Up)
          if (authMode == AuthMode.SIGN_UP) {
            OutlinedTextField(
              value = confirmPassword,
              onValueChange = {
                confirmPassword = it
                errorMessage = null
              },
              label = { Text("Confirm Password") },
              placeholder = { Text("Repeat password") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = GlassAccentBlue,
                )
              },
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                handleAuth()
              }),
              modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_field"),
              shape = RoundedCornerShape(14.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = GlassSurfaceDark,
                  unfocusedContainerColor = Color(0x330F172A),
                  focusedBorderColor = GlassAccentBlue,
                  unfocusedBorderColor = GlassBorder,
                  focusedTextColor = GlassTextPrimary,
                  unfocusedTextColor = GlassTextPrimary,
                  focusedLabelColor = GlassAccentBlue,
                  unfocusedLabelColor = GlassTextMuted,
                  cursorColor = GlassAccentBlue,
                ),
            )
          }

          // Error Message Box
          if (errorMessage != null) {
            Surface(
              color = Color(0x33EF4444),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0x66EF4444)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.Warning,
                  contentDescription = null,
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = errorMessage!!,
                  color = Color(0xFFFCA5A5),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Submit Button
          Button(
            onClick = handleAuth,
            enabled = !isLoading,
            modifier =
              Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                  elevation = 10.dp,
                  shape = RoundedCornerShape(16.dp),
                  ambientColor = GlassPrimaryBlue,
                  spotColor = GlassPrimaryBlue,
                )
                .testTag("submit_auth_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = GlassPrimaryBlue,
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(16.dp),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
              )
            } else {
              Text(
                text = if (authMode == AuthMode.SIGN_IN) "Sign In to Account" else "Sign Up & Create Account",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Switch mode text
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = if (authMode == AuthMode.SIGN_IN) "Don't have an account? " else "Already have an account? ",
          style = MaterialTheme.typography.bodyMedium,
          color = GlassTextSecondary,
        )
        Text(
          text = if (authMode == AuthMode.SIGN_IN) "Create One" else "Sign In",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = GlassAccentBlue,
          modifier =
            Modifier
              .clickable {
                authMode = if (authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
                errorMessage = null
                successMessage = null
              }
              .padding(4.dp)
              .testTag("switch_auth_mode_btn"),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Cloud Synchronization Indicator & Manual Sync Trigger
      Surface(
        color = GlassSurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x3310B981)),
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .clickable { syncCloudNow() }
          .padding(horizontal = 4.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (isSyncingCloud) {
            CircularProgressIndicator(
              color = Color(0xFF10B981),
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = "Cloud Connected",
              tint = Color(0xFF10B981),
              modifier = Modifier.size(16.dp),
            )
          }
          Text(
            text = if (isSyncingCloud) {
              "Syncing accounts from cloud server..."
            } else if (cloudSyncCount != null) {
              "Cloud Database Active ($cloudSyncCount accounts synced) • Tap to refresh"
            } else {
              "Cloud Database Active • Tap to refresh accounts"
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6EE7B7),
          )
        }
      }
    }
  }
}

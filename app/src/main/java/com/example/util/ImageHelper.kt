package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import java.io.ByteArrayOutputStream

object ImageHelper {

  // In-memory cache for decoded Base64 bitmaps (max 30 bitmaps)
  private val bitmapCache = object : LruCache<String, Bitmap>(30) {
    override fun sizeOf(key: String, value: Bitmap): Int {
      return 1
    }
  }

  /**
   * Compresses an image picked from the Android Photo Picker (Uri)
   * into a lightweight Base64 data URL string suitable for Room DB and Firestore cloud syncing.
   */
  fun compressUriToBase64(
    context: Context,
    uri: Uri,
    maxDimension: Int = 800,
    quality: Int = 75,
  ): String? {
    return try {
      // 1. Read all raw bytes once from the ContentResolver stream to avoid stream closed errors
      val rawBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.readBytes()
      } ?: return null

      if (rawBytes.isEmpty()) return null

      // 2. Decode image bounds
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
      if (options.outWidth <= 0 || options.outHeight <= 0) return null

      // 3. Calculate inSampleSize
      var sampleSize = 1
      while ((options.outWidth / sampleSize) > maxDimension || (options.outHeight / sampleSize) > maxDimension) {
        sampleSize *= 2
      }

      // 4. Decode bitmap with inSampleSize
      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565 // Half memory
      }
      val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions) ?: return null

      // 5. Further scale if needed
      val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
        val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
      } else {
        bitmap
      }

      // 6. Compress to JPEG
      val baos = ByteArrayOutputStream()
      scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
      val compressedBytes = baos.toByteArray()
      val base64String = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
      val dataUrl = "data:image/jpeg;base64,$base64String"

      // Cache the scaled bitmap immediately
      bitmapCache.put(dataUrl, scaledBitmap)

      dataUrl
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Safely decodes a Base64 string or data URL into a Bitmap, with memory caching.
   */
  fun decodeBase64ToBitmap(token: String): Bitmap? {
    val clean = token.trim()
    if (clean.isBlank()) return null

    bitmapCache.get(clean)?.let { return it }

    return try {
      val base64Data = if (clean.contains(",")) clean.substringAfter(",") else clean
      val bytes = Base64.decode(base64Data, Base64.DEFAULT)
      val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
      if (bmp != null) {
        bitmapCache.put(clean, bmp)
      }
      bmp
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Safely splits course imageUrls string without breaking data:image/jpeg;base64, commas
   */
  fun splitImageUrls(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return when {
      raw.contains(";;;") -> raw.split(";;;").map { it.trim() }.filter { it.isNotEmpty() }
      raw.contains("|||") -> raw.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
      raw.contains("\n") -> raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
      raw.startsWith("data:image/") -> listOf(raw.trim())
      else -> raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
  }

  /**
   * Joins a list of image URLs/Base64/Drawables safely
   */
  fun joinImageUrls(list: List<String>): String {
    return list.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(";;;")
  }

  /**
   * Resolves an image token (Base64 data URL, web URL, content URI, or legacy drawable resource name)
   * into a renderable model: Bitmap, Int resource ID, Uri, or String URL.
   */
  fun resolveImageModel(token: String, fallbackResId: Int = R.drawable.img_course_tech): Any {
    val clean = token.trim()
    if (clean.isEmpty()) return fallbackResId

    // Check for preset drawables
    when (clean.lowercase()) {
      "img_course_tech" -> return R.drawable.img_course_tech
      "img_course_design" -> return R.drawable.img_course_design
      "img_course_business" -> return R.drawable.img_course_business
      "img_hero_courses" -> return R.drawable.img_hero_courses
      "img_course_app_icon" -> return R.drawable.img_course_app_icon
    }

    // Check if it's a Base64 data URL or raw Base64 string
    if (clean.startsWith("data:image/") || (clean.length > 100 && !clean.contains(" ") && !clean.contains("http"))) {
      val bmp = decodeBase64ToBitmap(clean)
      if (bmp != null) {
        return bmp
      }
    }

    // Direct content URI or file URI
    if (clean.startsWith("content://") || clean.startsWith("file://")) {
      return Uri.parse(clean)
    }

    // Direct HTTP/HTTPS URL
    if (clean.startsWith("http://") || clean.startsWith("https://")) {
      return clean
    }

    return fallbackResId
  }
}

/**
 * Universal Image Composable that safely and reliably displays:
 * - Bitmap (Base64 decoded in-memory)
 * - Uri (Photo picker content URI or local file)
 * - Int (Local drawable resource ID)
 * - String (data:image/..., http/https URL, preset name)
 */
@Composable
fun AppImage(
  model: Any?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
  fallbackResId: Int = R.drawable.img_course_tech,
) {
  val resolved = remember(model, fallbackResId) {
    when (model) {
      is Bitmap -> model
      is Int -> model
      is Uri -> model
      is String -> ImageHelper.resolveImageModel(model, fallbackResId)
      null -> fallbackResId
      else -> model
    }
  }

  when (resolved) {
    is Bitmap -> {
      Image(
        bitmap = resolved.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
      )
    }
    is Int -> {
      Image(
        painter = painterResource(id = resolved),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
      )
    }
    else -> {
      SubcomposeAsyncImage(
        model = resolved,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
          Box(
            modifier = Modifier.fillMaxSize().background(Color(0x221E293B)),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = Color(0xFF38BDF8),
              strokeWidth = 2.dp,
            )
          }
        },
        error = {
          Image(
            painter = painterResource(id = fallbackResId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
          )
        },
      )
    }
  }
}


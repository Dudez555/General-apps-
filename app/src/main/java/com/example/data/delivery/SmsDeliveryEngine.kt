package com.example.data.delivery

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object SmsDeliveryEngine {
  private const val TAG = "SmsDeliveryEngine"
  private const val APP_WEB_BASE = "https://ais-pre-qq4o37kn22wa7nyyu5jyd6-921830006334.asia-east1.run.app"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

  fun generateWebChatLink(conversationId: String, slug: String?): String {
    val effectiveSlug = slug ?: UUID.nameUUIDFromBytes(conversationId.toByteArray()).toString().take(12)
    return "$APP_WEB_BASE/chat?id=$effectiveSlug"
  }

  fun formatSmsPreview(senderName: String, content: String, webLink: String): String {
    val cleanContent = if (content.length > 60) content.take(60) + "..." else content
    return "[$senderName via Wi-Fi]: $cleanContent\nReply directly without app: $webLink"
  }

  suspend fun forwardToGoogleChatWebhook(webhookUrl: String, senderName: String, messageText: String): Boolean =
    withContext(Dispatchers.IO) {
      if (webhookUrl.isBlank()) return@withContext false
      try {
        val payload = JSONObject().apply {
          put("text", "💬 **$senderName** (via WiFiSMS):\n$messageText")
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = payload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
          .url(webhookUrl)
          .post(body)
          .build()

        httpClient.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            Log.d(TAG, "Successfully delivered message to Google Chat webhook")
            true
          } else {
            Log.w(TAG, "Google Chat webhook returned HTTP ${response.code}")
            false
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to deliver to Google Chat webhook: ${e.message}")
        false
      }
    }
}

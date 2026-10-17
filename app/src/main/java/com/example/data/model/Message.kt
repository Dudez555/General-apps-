package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Message(
  val messageId: String = "",
  val conversationId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val recipientPhone: String? = null,
  val content: String = "",
  val encryptedPayload: String? = null,
  val messageType: String = "text",
  val audioDurationSec: Int = 0,
  val transcription: String? = null,
  val mediaUrl: String? = null,
  val mediaType: String? = null,
  val fileName: String? = null,
  val fileSizeKb: Long = 0L,
  val status: String = "delivered",
  val deliveryMode: String = "wifi_api",
  @ServerTimestamp val createdAt: Timestamp? = null
)

package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Conversation(
  val conversationId: String = "",
  val type: String = "direct",
  val title: String = "",
  val participantUids: List<String> = emptyList(),
  val participantPhones: List<String> = emptyList(),
  val adminUids: List<String> = emptyList(),
  val lastMessage: String? = null,
  val lastMessageSenderId: String? = null,
  @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
  @get:PropertyName("isEncrypted") @set:PropertyName("isEncrypted") var isEncrypted: Boolean = true,
  val webChatSlug: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.crypto.E2EEEngine
import com.example.data.delivery.SmsDeliveryEngine
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeConversations(): Flow<List<Conversation>> {
    val uid = requireUserId()
    return db.collection("conversations")
      .whereArrayContains("participantUids", uid)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Conversation::class.java).sortedByDescending {
          it.lastMessageTimestamp?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
        }
      }
  }

  fun observeMessages(conversationId: String): Flow<List<Message>> {
    requireUserId()
    return db.collection("conversations")
      .document(conversationId)
      .collection("messages")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Message::class.java)
      }
  }

  suspend fun createDirectConversation(
    recipientPhone: String,
    title: String,
    isEncrypted: Boolean = true
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val convId = "direct_${UUID.randomUUID().toString().take(8)}"
      val slug = UUID.randomUUID().toString().take(10)
      val conv = hashMapOf(
        "conversationId" to convId,
        "type" to "direct",
        "title" to title.ifBlank { recipientPhone },
        "participantUids" to listOf(uid),
        "participantPhones" to listOf(recipientPhone),
        "adminUids" to listOf(uid),
        "lastMessage" to "Conversation started",
        "lastMessageSenderId" to uid,
        "isEncrypted" to isEncrypted,
        "webChatSlug" to slug,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      db.collection("conversations").document(convId).set(conv).await()
      Result.success(convId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createGroupConversation(
    title: String,
    participantPhones: List<String>,
    isEncrypted: Boolean = true
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val convId = "group_${UUID.randomUUID().toString().take(8)}"
      val slug = UUID.randomUUID().toString().take(10)
      val allPhones = participantPhones.filter { it.isNotBlank() }
      val conv = hashMapOf(
        "conversationId" to convId,
        "type" to "group",
        "title" to title.ifBlank { "Group Chat" },
        "participantUids" to listOf(uid),
        "participantPhones" to allPhones,
        "adminUids" to listOf(uid),
        "lastMessage" to "Group created",
        "lastMessageSenderId" to uid,
        "isEncrypted" to isEncrypted,
        "webChatSlug" to slug,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      db.collection("conversations").document(convId).set(conv).await()
      Result.success(convId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun sendMessage(
    conversationId: String,
    content: String,
    recipientPhone: String? = null,
    messageType: String = "text",
    audioDurationSec: Int = 0,
    transcription: String? = null,
    mediaUrl: String? = null,
    mediaType: String? = null,
    fileName: String? = null,
    fileSizeKb: Long = 0L,
    deliveryMode: String = "wifi_api",
    googleChatWebhookUrl: String? = null,
    isEncrypted: Boolean = false
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val senderName = auth.currentUser?.displayName ?: "Anonymous"
      val messageId = "msg_${UUID.randomUUID().toString().take(10)}"

      val encryptedPayload = if (isEncrypted) {
        val key = E2EEEngine.deriveKey(conversationId)
        E2EEEngine.encrypt(content, key)
      } else null

      val effectiveContent = if (isEncrypted) {
        "[Encrypted Message • E2EE]"
      } else {
        content
      }

      val messageMap = hashMapOf<String, Any>(
        "messageId" to messageId,
        "conversationId" to conversationId,
        "senderId" to uid,
        "senderName" to senderName,
        "content" to effectiveContent,
        "messageType" to messageType,
        "status" to "delivered",
        "deliveryMode" to deliveryMode,
        "audioDurationSec" to audioDurationSec,
        "fileSizeKb" to fileSizeKb,
        "createdAt" to FieldValue.serverTimestamp()
      )

      if (!recipientPhone.isNullOrBlank()) messageMap["recipientPhone"] = recipientPhone
      if (!encryptedPayload.isNullOrBlank()) messageMap["encryptedPayload"] = encryptedPayload
      if (!transcription.isNullOrBlank()) messageMap["transcription"] = transcription
      if (!mediaUrl.isNullOrBlank()) messageMap["mediaUrl"] = mediaUrl
      if (!mediaType.isNullOrBlank()) messageMap["mediaType"] = mediaType
      if (!fileName.isNullOrBlank()) messageMap["fileName"] = fileName

      val convDoc = db.collection("conversations").document(conversationId)
      convDoc.collection("messages").document(messageId).set(messageMap).await()

      // Update parent conversation
      val updateMap = hashMapOf<String, Any>(
        "lastMessage" to effectiveContent,
        "lastMessageSenderId" to uid,
        "lastMessageTimestamp" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      convDoc.update(updateMap).await()

      // Bridge to Google Chat if configured
      if (!googleChatWebhookUrl.isNullOrBlank()) {
        SmsDeliveryEngine.forwardToGoogleChatWebhook(googleChatWebhookUrl, senderName, content)
      }

      Result.success(messageId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addMemberToGroup(conversationId: String, phoneOrUid: String): Result<Unit> {
    return try {
      requireUserId()
      val convDoc = db.collection("conversations").document(conversationId)
      val snapshot = convDoc.get().await()
      val currentPhones = (snapshot.get("participantPhones") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
      val updatedPhones = (currentPhones + phoneOrUid).distinct()
      convDoc.update(
        mapOf(
          "participantPhones" to updatedPhones,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun removeMemberFromGroup(conversationId: String, phoneOrUid: String): Result<Unit> {
    return try {
      requireUserId()
      val convDoc = db.collection("conversations").document(conversationId)
      val snapshot = convDoc.get().await()
      val currentPhones = (snapshot.get("participantPhones") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
      val updatedPhones = currentPhones.filter { it != phoneOrUid }
      convDoc.update(
        mapOf(
          "participantPhones" to updatedPhones,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun syncUserProfile(phoneNumber: String): Result<Unit> {
    return try {
      val user = auth.currentUser ?: return Result.failure(IllegalStateException("No current user"))
      val uid = user.uid
      val userDoc = db.collection("users").document(uid)
      val snapshot = userDoc.get().await()
      if (!snapshot.exists()) {
        val profile = hashMapOf(
          "userId" to uid,
          "displayName" to (user.displayName ?: "User"),
          "email" to (user.email ?: ""),
          "phoneNumber" to phoneNumber,
          "photoUrl" to (user.photoUrl?.toString() ?: ""),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        userDoc.set(profile).await()
      } else {
        userDoc.update(
          mapOf(
            "displayName" to (user.displayName ?: "User"),
            "phoneNumber" to phoneNumber,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getUserProfile(uid: String): UserProfile? {
    return try {
      val snapshot = db.collection("users").document(uid).get().await()
      snapshot.toObject(UserProfile::class.java)
    } catch (e: Exception) {
      null
    }
  }
}

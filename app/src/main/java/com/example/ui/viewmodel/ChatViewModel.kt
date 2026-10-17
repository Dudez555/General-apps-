package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.crypto.E2EEEngine
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = ChatRepository(application)

  private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
  val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

  private val _activeConversation = MutableStateFlow<Conversation?>(null)
  val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

  private val _messages = MutableStateFlow<List<Message>>(emptyList())
  val messages: StateFlow<List<Message>> = _messages.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _googleChatWebhook = MutableStateFlow("")
  val googleChatWebhook: StateFlow<String> = _googleChatWebhook.asStateFlow()

  private var messagesJob: Job? = null

  init {
    loadConversations()
  }

  fun loadConversations() {
    viewModelScope.launch {
      try {
        repository.observeConversations()
          .catch { e -> _errorMessage.value = e.localizedMessage }
          .collect { list ->
            _conversations.value = list
            // Update active conversation reference if present
            _activeConversation.value?.let { current ->
              _activeConversation.value = list.find { it.conversationId == current.conversationId } ?: current
            }
          }
      } catch (e: Exception) {
        _errorMessage.value = e.localizedMessage
      }
    }
  }

  fun selectConversation(conversation: Conversation) {
    _activeConversation.value = conversation
    messagesJob?.cancel()
    messagesJob = viewModelScope.launch {
      repository.observeMessages(conversation.conversationId)
        .catch { e -> _errorMessage.value = e.localizedMessage }
        .collect { msgList ->
          // Decrypt E2EE messages if encrypted
          val key = E2EEEngine.deriveKey(conversation.conversationId)
          val processed = msgList.map { msg ->
            if (!msg.encryptedPayload.isNullOrBlank()) {
              val decrypted = E2EEEngine.decrypt(msg.encryptedPayload, key)
              msg.copy(content = decrypted)
            } else {
              msg
            }
          }
          _messages.value = processed
        }
    }
  }

  fun clearActiveConversation() {
    messagesJob?.cancel()
    _activeConversation.value = null
    _messages.value = emptyList()
  }

  fun startDirectChat(phone: String, name: String, onComplete: (String) -> Unit = {}) {
    viewModelScope.launch {
      _isLoading.value = true
      val res = repository.createDirectConversation(phone, name)
      _isLoading.value = false
      res.onSuccess { convId ->
        val newConv = Conversation(
          conversationId = convId,
          type = "direct",
          title = name.ifBlank { phone },
          participantPhones = listOf(phone)
        )
        selectConversation(newConv)
        onComplete(convId)
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage
      }
    }
  }

  fun createGroupChat(title: String, phones: List<String>, isEncrypted: Boolean, onComplete: (String) -> Unit = {}) {
    viewModelScope.launch {
      _isLoading.value = true
      val res = repository.createGroupConversation(title, phones, isEncrypted)
      _isLoading.value = false
      res.onSuccess { convId ->
        val newConv = Conversation(
          conversationId = convId,
          type = "group",
          title = title,
          participantPhones = phones,
          isEncrypted = isEncrypted
        )
        selectConversation(newConv)
        onComplete(convId)
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage
      }
    }
  }

  fun sendTextMessage(content: String) {
    val conv = _activeConversation.value ?: return
    if (content.isBlank()) return
    viewModelScope.launch {
      val recipientPhone = conv.participantPhones.firstOrNull()
      repository.sendMessage(
        conversationId = conv.conversationId,
        content = content.trim(),
        recipientPhone = recipientPhone,
        messageType = "text",
        deliveryMode = "wifi_api",
        googleChatWebhookUrl = _googleChatWebhook.value,
        isEncrypted = conv.isEncrypted
      )
    }
  }

  fun sendVoiceMessage(durationSec: Int, transcription: String) {
    val conv = _activeConversation.value ?: return
    viewModelScope.launch {
      val recipientPhone = conv.participantPhones.firstOrNull()
      repository.sendMessage(
        conversationId = conv.conversationId,
        content = "🎤 Voice Note (${durationSec}s): $transcription",
        recipientPhone = recipientPhone,
        messageType = "voice",
        audioDurationSec = durationSec,
        transcription = transcription,
        deliveryMode = "wifi_api",
        googleChatWebhookUrl = _googleChatWebhook.value,
        isEncrypted = conv.isEncrypted
      )
    }
  }

  fun sendMediaAttachment(
    mediaType: String,
    fileName: String,
    fileSizeKb: Long,
    simulatedUrl: String
  ) {
    val conv = _activeConversation.value ?: return
    viewModelScope.launch {
      val recipientPhone = conv.participantPhones.firstOrNull()
      val icon = when (mediaType) {
        "image" -> "📷 Photo"
        "video" -> "🎥 Video"
        else -> "📄 Document"
      }
      repository.sendMessage(
        conversationId = conv.conversationId,
        content = "$icon: $fileName (${fileSizeKb} KB)",
        recipientPhone = recipientPhone,
        messageType = "media",
        mediaType = mediaType,
        fileName = fileName,
        fileSizeKb = fileSizeKb,
        mediaUrl = simulatedUrl,
        deliveryMode = "wifi_api",
        googleChatWebhookUrl = _googleChatWebhook.value,
        isEncrypted = conv.isEncrypted
      )
    }
  }

  fun addGroupMember(phone: String) {
    val conv = _activeConversation.value ?: return
    viewModelScope.launch {
      val res = repository.addMemberToGroup(conv.conversationId, phone)
      res.onFailure { _errorMessage.value = it.localizedMessage }
    }
  }

  fun removeGroupMember(phone: String) {
    val conv = _activeConversation.value ?: return
    viewModelScope.launch {
      val res = repository.removeMemberFromGroup(conv.conversationId, phone)
      res.onFailure { _errorMessage.value = it.localizedMessage }
    }
  }

  fun setGoogleChatWebhook(url: String) {
    _googleChatWebhook.value = url
  }

  fun clearError() {
    _errorMessage.value = null
  }
}

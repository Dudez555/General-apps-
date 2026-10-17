package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiAiService
import com.example.data.crypto.E2EEEngine
import com.example.data.delivery.SmsDeliveryEngine
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.ui.screens.call.VideoCallDialog
import com.example.ui.screens.group.GroupDetailsDialog
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
  conversation: Conversation,
  viewModel: ChatViewModel,
  callViewModel: CallViewModel,
  onBack: () -> Unit
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val messages by viewModel.messages.collectAsState()
  val currentUserId = remember { Firebase.auth.currentUser?.uid ?: "" }

  var textInput by remember { mutableStateOf("") }
  var showGroupDetails by remember { mutableStateOf(false) }
  var showE2eeInfoDialog by remember { mutableStateOf(false) }
  var showMediaAttachDialog by remember { mutableStateOf(false) }
  var isRecordingVoice by remember { mutableStateOf(false) }
  var voiceSeconds by remember { mutableIntStateOf(0) }
  var smartReplies by remember { mutableStateOf<List<String>>(emptyList()) }

  val webChatLink = remember(conversation.conversationId, conversation.webChatSlug) {
    SmsDeliveryEngine.generateWebChatLink(conversation.conversationId, conversation.webChatSlug)
  }

  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
      // Generate smart replies for the last message if sent by peer
      val lastMsg = messages.lastOrNull()
      if (lastMsg != null && lastMsg.senderId != currentUserId && lastMsg.content.isNotBlank()) {
        scope.launch {
          smartReplies = GeminiAiService.generateSmartReplies(lastMsg.content)
        }
      }
    }
  }

  // Voice recording timer
  LaunchedEffect(isRecordingVoice) {
    if (isRecordingVoice) {
      voiceSeconds = 0
      while (isActive && isRecordingVoice) {
        delay(1000)
        voiceSeconds++
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Bar
    TopAppBar(
      title = {
        Column(modifier = Modifier.clickable { showGroupDetails = true }) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = conversation.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
            if (conversation.isEncrypted) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.clickable { showE2eeInfoDialog = true }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "E2EE",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "E2EE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
          Text(
            text = if (conversation.type == "group") "${conversation.participantPhones.size + 1} members • Wi-Fi" else "Wi-Fi SIM-less SMS",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      navigationIcon = {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
      actions = {
        // Video Call button
        IconButton(
          onClick = {
            callViewModel.startCall(conversation.title, isVideo = true)
          },
          modifier = Modifier.testTag("start_video_call_button")
        ) {
          Icon(Icons.Default.Videocam, contentDescription = "HD Video Call", tint = MaterialTheme.colorScheme.primary)
        }

        // Details / Group info
        IconButton(
          onClick = { showGroupDetails = true },
          modifier = Modifier.testTag("conversation_info_button")
        ) {
          Icon(Icons.Default.Info, contentDescription = "Details")
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )

    // Non-App Receiver Web Delivery Link Banner
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Receiver has no SIM/App? Deliver via Web Link:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = webChatLink,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
          )
        }
        IconButton(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Chat Link", webChatLink)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Web chat link copied!", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy Link",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Message List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 10.dp)
    ) {
      items(messages, key = { it.messageId }) { message ->
        MessageBubble(
          message = message,
          isFromMe = message.senderId == currentUserId,
          isEncrypted = conversation.isEncrypted
        )
      }
    }

    // Smart Replies Bar (Gemini generated)
    AnimatedVisibility(visible = smartReplies.isNotEmpty()) {
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("AI Suggestions:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
          }
        }
        items(smartReplies) { reply ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = {
              viewModel.sendTextMessage(reply)
              smartReplies = emptyList()
            }
          ) {
            Text(
              text = reply,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }
    }

    // Bottom Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        if (isRecordingVoice) {
          // Voice Recording Indicator
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(12.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.error)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Recording Voice Note... ${voiceSeconds}s",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
              )
            }
            IconButton(
              onClick = {
                val sec = voiceSeconds
                isRecordingVoice = false
                scope.launch {
                  val transcription = GeminiAiService.transcribeVoiceNote(sec)
                  viewModel.sendVoiceMessage(sec, transcription)
                }
              }
            ) {
              Icon(Icons.Default.Stop, contentDescription = "Stop & Send", tint = MaterialTheme.colorScheme.error)
            }
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Attach Media
          IconButton(
            onClick = { showMediaAttachDialog = true },
            modifier = Modifier.testTag("attach_media_button")
          ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach Media")
          }

          // Voice Record Button
          IconButton(
            onClick = {
              if (isRecordingVoice) {
                val sec = voiceSeconds
                isRecordingVoice = false
                scope.launch {
                  val transcription = GeminiAiService.transcribeVoiceNote(sec)
                  viewModel.sendVoiceMessage(sec, transcription)
                }
              } else {
                isRecordingVoice = true
              }
            },
            modifier = Modifier.testTag("record_voice_button")
          ) {
            Icon(
              imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = "Voice Message",
              tint = if (isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
          }

          // Text Field
          OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Wi-Fi message...") },
            modifier = Modifier
              .weight(1f)
              .testTag("message_input_field"),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Send Button
          FilledIconButton(
            onClick = {
              if (textInput.isNotBlank()) {
                viewModel.sendTextMessage(textInput)
                textInput = ""
              }
            },
            enabled = textInput.isNotBlank(),
            modifier = Modifier.testTag("send_message_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Send",
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }

  // Media Attachment Picker Modal
  if (showMediaAttachDialog) {
    AlertDialog(
      onDismissRequest = { showMediaAttachDialog = false },
      title = { Text("Share Media via Wi-Fi") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Files are securely encrypted in transit. Free limit: up to 40MB.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))

          Card(
            onClick = {
              viewModel.sendMediaAttachment(
                mediaType = "image",
                fileName = "photo_capture_${System.currentTimeMillis() % 1000}.jpg",
                fileSizeKb = 850L,
                simulatedUrl = "mock_photo_url"
              )
              showMediaAttachDialog = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Send Photo", style = MaterialTheme.typography.titleSmall)
                Text("Camera or Gallery • JPG, PNG", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              }
            }
          }

          Card(
            onClick = {
              viewModel.sendMediaAttachment(
                mediaType = "video",
                fileName = "video_clip_${System.currentTimeMillis() % 1000}.mp4",
                fileSizeKb = 3400L,
                simulatedUrl = "mock_video_url"
              )
              showMediaAttachDialog = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Send Video", style = MaterialTheme.typography.titleSmall)
                Text("MP4, MKV • Compressed for Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              }
            }
          }

          Card(
            onClick = {
              viewModel.sendMediaAttachment(
                mediaType = "document",
                fileName = "document_brief_${System.currentTimeMillis() % 1000}.pdf",
                fileSizeKb = 1200L,
                simulatedUrl = "mock_doc_url"
              )
              showMediaAttachDialog = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Send Document", style = MaterialTheme.typography.titleSmall)
                Text("PDF, DOCX, TXT • Secure transfer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showMediaAttachDialog = false }) { Text("Cancel") }
      }
    )
  }

  // E2EE Fingerprint Modal
  if (showE2eeInfoDialog) {
    val fingerprint = remember(conversation.conversationId) {
      E2EEEngine.getFingerprint(conversation.conversationId)
    }
    AlertDialog(
      onDismissRequest = { showE2eeInfoDialog = false },
      icon = {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
      },
      title = { Text("End-to-End Encryption") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Messages in this chat are encrypted using AES-256-GCM. Neither cellular carriers nor intermediate Wi-Fi routers can read your content.",
            style = MaterialTheme.typography.bodySmall
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Security Safety Fingerprint:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = fingerprint,
              style = MaterialTheme.typography.bodyMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(onClick = { showE2eeInfoDialog = false }) { Text("Verified") }
      }
    )
  }

  // Group Details Dialog
  if (showGroupDetails) {
    GroupDetailsDialog(
      conversation = conversation,
      onDismiss = { showGroupDetails = false },
      onAddMember = { phone -> viewModel.addGroupMember(phone) },
      onRemoveMember = { phone -> viewModel.removeGroupMember(phone) }
    )
  }

  // Video Call Dialog
  VideoCallDialog(
    viewModel = callViewModel,
    onDismiss = { callViewModel.endCall() }
  )
}

@Composable
fun MessageBubble(
  message: Message,
  isFromMe: Boolean,
  isEncrypted: Boolean
) {
  var isPlayingAudio by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
  ) {
    Card(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isFromMe) 16.dp else 4.dp,
        bottomEnd = if (isFromMe) 4.dp else 16.dp
      ),
      colors = CardDefaults.cardColors(
        containerColor = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
      ),
      modifier = Modifier.widthIn(max = 310.dp)
    ) {
      Column(modifier = Modifier.padding(10.dp)) {
        // Sender name for group chats if incoming
        if (!isFromMe) {
          Text(
            text = message.senderName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(2.dp))
        }

        // Voice Message View
        if (message.messageType == "voice") {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            IconButton(
              onClick = { isPlayingAudio = !isPlayingAudio },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Voice Memo • ${message.audioDurationSec}s",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
              // Waveform representation
              Text(
                text = if (isPlayingAudio) " ▂▃▅▆▇▆▅▃▂ " else " ▂▃▂▃▄▃▂  ",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
              )
            }
          }

          // Transcription Card if present
          if (!message.transcription.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isFromMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(6.dp)) {
                Text(
                  text = "Gemini AI Transcription:",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Text(
                  text = message.transcription,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        } else if (message.messageType == "media") {
          // Media Attachment View
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isFromMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = when (message.mediaType) {
                  "image" -> Icons.Default.Image
                  "video" -> Icons.Default.Videocam
                  else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = message.fileName ?: "Attached File",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${message.fileSizeKb} KB • Encrypted",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                )
              }
            }
          }
        } else {
          // Standard Text message
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timestamp & Delivery Checkmark
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val timeStr = remember(message.createdAt) {
            val date = message.createdAt?.toDate()
            if (date != null) {
              SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            } else "Just now"
          }

          Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outline
          )

          if (isFromMe) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = if (message.status == "delivered" || message.status == "read") Icons.Default.DoneAll else Icons.Default.Check,
              contentDescription = message.status,
              tint = if (message.status == "read") Color(0xFF38BDF8) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }
  }
}

package com.example.ui.screens.ai

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiAiService
import kotlinx.coroutines.launch

data class ChatMessage(
  val sender: String, // "user" or "model"
  val text: String,
  val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AiChatScreen(
  onInsertToChat: (String) -> Unit = {}
) {
  val scope = rememberCoroutineScope()
  var selectedModel by remember { mutableStateOf("gemini-3.8-flash") }
  var inputText by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var isLiveVoiceActive by remember { mutableStateOf(false) }

  val messages = remember {
    mutableStateListOf(
      ChatMessage(
        sender = "model",
        text = "Hello! I am your WiFiSMS Gemini Assistant. I can help draft encrypted messages, transcribe audio notes, summarize discussions, or converse in real time. How can I assist you?"
      )
    )
  }

  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Model Selector Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.SmartToy,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Gemini AI Assistant",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }

          // Toggle Live Voice mode button
          FilterChip(
            selected = isLiveVoiceActive,
            onClick = { isLiveVoiceActive = !isLiveVoiceActive },
            label = {
              Text(if (isLiveVoiceActive) "Voice Active" else "Live Voice", fontSize = 12.sp)
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("live_voice_chip")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Model Choice Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            FilterChip(
              selected = selectedModel == "gemini-3.8-flash",
              onClick = { selectedModel = "gemini-3.8-flash" },
              label = { Text("Gemini 3.8 Flash (General)", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
              }
            )
          }
          item {
            FilterChip(
              selected = selectedModel == "gemini-3.1-flash-lite",
              onClick = { selectedModel = "gemini-3.1-flash-lite" },
              label = { Text("3.1 Flash Lite (Low Latency)", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
              }
            )
          }
          item {
            FilterChip(
              selected = selectedModel == "gemini-4-argon",
              onClick = { selectedModel = "gemini-4-argon" },
              label = { Text("Gemini 4 Argon (Reasoning)", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(14.dp))
              }
            )
          }
        }
      }
    }

    // Live Voice Companion Banner
    if (isLiveVoiceActive) {
      LiveVoiceCompanionCard(
        onSpokenPrompt = { spokenText ->
          messages.add(ChatMessage("user", spokenText))
          scope.launch {
            isLoading = true
            val reply = GeminiAiService.generateResponse(spokenText, selectedModel)
            messages.add(ChatMessage("model", reply))
            isLoading = false
          }
        }
      )
    }

    // Message List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 12.dp)
    ) {
      items(messages) { msg ->
        AiChatBubble(
          message = msg,
          onInsertToChat = onInsertToChat
        )
      }

      if (isLoading) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = if (selectedModel == "gemini-3.1-flash-lite") "Fast inference..." else "Thinking...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Quick Prompts Row
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      val suggestions = listOf(
        "Draft a secure meeting invite",
        "Summarize group consensus",
        "Explain end-to-end encryption",
        "Polite reply: Will be 5 min late"
      )
      items(suggestions) { prompt ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          onClick = {
            inputText = prompt
          }
        ) {
          Text(
            text = prompt,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    // Bottom Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Ask Gemini or draft message...") },
          modifier = Modifier
            .weight(1f)
            .testTag("ai_input_field"),
          shape = RoundedCornerShape(24.dp),
          maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        FloatingActionButton(
          onClick = {
            if (inputText.isNotBlank() && !isLoading) {
              val prompt = inputText.trim()
              inputText = ""
              messages.add(ChatMessage("user", prompt))
              scope.launch {
                isLoading = true
                val reply = GeminiAiService.generateResponse(prompt, selectedModel)
                messages.add(ChatMessage("model", reply))
                isLoading = false
              }
            }
          },
          modifier = Modifier
            .size(48.dp)
            .testTag("ai_send_button"),
          shape = CircleShape,
          containerColor = MaterialTheme.colorScheme.primary
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send to AI",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
fun AiChatBubble(
  message: ChatMessage,
  onInsertToChat: (String) -> Unit
) {
  val isUser = message.sender == "user"

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    if (!isUser) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
          .size(32.dp)
          .align(Alignment.Bottom)
      ) {
        Icon(
          imageVector = Icons.Default.SmartToy,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(6.dp)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }

    Card(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
      ),
      colors = CardDefaults.cardColors(
        containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
      ),
      modifier = Modifier.widthIn(max = 300.dp)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isUser) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              onClick = { onInsertToChat(message.text) }
            ) {
              Text(
                text = "Use in Message ↗",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LiveVoiceCompanionCard(
  onSpokenPrompt: (String) -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(12.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Live AI Voice Mode Active",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
      )
      Text(
        text = "Speak naturally to converse in real-time or draft responses",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
      )
      Spacer(modifier = Modifier.height(14.dp))
      Box(
        modifier = Modifier
          .size(68.dp)
          .scale(pulseScale)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = "Listening",
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(34.dp)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          onClick = { onSpokenPrompt("Draft a quick reply acknowledging receipt of the file.") }
        ) {
          Text(
            text = "Say: \"Draft quick reply\"",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          onClick = { onSpokenPrompt("What is the difference between SIM SMS and Wi-Fi delivery?") }
        ) {
          Text(
            text = "Say: \"Explain Wi-Fi delivery\"",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }
  }
}

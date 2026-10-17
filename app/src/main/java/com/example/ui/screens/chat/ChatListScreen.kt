package com.example.ui.screens.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.ui.screens.group.CreateGroupDialog
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
  viewModel: ChatViewModel,
  onOpenConversation: (Conversation) -> Unit
) {
  val conversations by viewModel.conversations.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("All") }
  var showDirectSmsDialog by remember { mutableStateOf(false) }
  var showCreateGroupDialog by remember { mutableStateOf(false) }

  val filteredList = conversations.filter { conv ->
    val matchesSearch = conv.title.contains(searchQuery, ignoreCase = true) ||
      conv.participantPhones.any { it.contains(searchQuery) }
    val matchesFilter = when (selectedFilter) {
      "Direct" -> conv.type == "direct"
      "Groups" -> conv.type == "group"
      "E2EE" -> conv.isEncrypted
      else -> true
    }
    matchesSearch && matchesFilter
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Top Wi-Fi Status Banner
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Wifi,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "SIM-Free Wi-Fi Transmission Active",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary
          ) {
            Text(
              text = "Web Delivery Ready",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
      }

      // Search Field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search conversations, groups, or phones...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("conversation_search_input"),
        shape = RoundedCornerShape(24.dp),
        singleLine = true
      )

      // Filter Chips
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val filters = listOf("All", "Direct", "Groups", "E2EE")
        items(filters) { filter ->
          FilterChip(
            selected = selectedFilter == filter,
            onClick = { selectedFilter = filter },
            label = { Text(filter) }
          )
        }
      }

      if (isLoading && conversations.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      } else if (filteredList.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = null,
              modifier = Modifier.size(56.dp),
              tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No matching conversations" else "No chats yet",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Send a SIM-less message to any phone number or create a group!",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filteredList, key = { it.conversationId }) { conv ->
            ConversationItemCard(
              conversation = conv,
              onClick = {
                viewModel.selectConversation(conv)
                onOpenConversation(conv)
              }
            )
          }
        }
      }
    }

    // Action Buttons
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 24.dp, end = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.End
    ) {
      FloatingActionButton(
        onClick = { showCreateGroupDialog = true },
        modifier = Modifier.testTag("create_group_fab"),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
      ) {
        Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
      }

      ExtendedFloatingActionButton(
        onClick = { showDirectSmsDialog = true },
        modifier = Modifier.testTag("new_chat_fab"),
        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
        text = { Text("SIM-less SMS") }
      )
    }
  }

  // Direct SMS Dialog
  if (showDirectSmsDialog) {
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showDirectSmsDialog = false },
      title = { Text("Send Wi-Fi Message to Phone") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "No SIM card needed! The recipient receives a standard SMS or interactive web chat link to reply online without downloading any app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Recipient Phone Number") },
            placeholder = { Text("+1 (555) 000-0000") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("direct_phone_input")
          )
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Contact Name (Optional)") },
            placeholder = { Text("Alex Smith") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (phone.isNotBlank()) {
              viewModel.startDirectChat(phone, name) { convId ->
                showDirectSmsDialog = false
                val newConv = Conversation(
                  conversationId = convId,
                  type = "direct",
                  title = name.ifBlank { phone },
                  participantPhones = listOf(phone)
                )
                onOpenConversation(newConv)
              }
            }
          },
          enabled = phone.isNotBlank(),
          modifier = Modifier.testTag("start_chat_confirm_button")
        ) {
          Text("Start Conversation")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDirectSmsDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Create Group Dialog
  if (showCreateGroupDialog) {
    CreateGroupDialog(
      onDismiss = { showCreateGroupDialog = false },
      onCreateGroup = { title, phones, isEncrypted ->
        viewModel.createGroupChat(title, phones, isEncrypted) { convId ->
          showCreateGroupDialog = false
          val newGroup = Conversation(
            conversationId = convId,
            type = "group",
            title = title,
            participantPhones = phones,
            isEncrypted = isEncrypted
          )
          onOpenConversation(newGroup)
        }
      }
    )
  }
}

@Composable
fun ConversationItemCard(
  conversation: Conversation,
  onClick: () -> Unit
) {
  val isGroup = conversation.type == "group"

  ElevatedCard(
    onClick = onClick,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("conversation_item_${conversation.conversationId}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Avatar
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(
            if (isGroup) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isGroup) Icons.Default.Group else Icons.Default.Person,
          contentDescription = null,
          tint = if (isGroup) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Text(
              text = conversation.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (conversation.isEncrypted) {
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "E2EE Encrypted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
            }
          }

          val timeStr = remember(conversation.lastMessageTimestamp) {
            val date = conversation.lastMessageTimestamp?.toDate()
            if (date != null) {
              SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            } else ""
          }
          if (timeStr.isNotBlank()) {
            Text(
              text = timeStr,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = conversation.lastMessage ?: if (isGroup) "Group created" else "Start conversation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )

          if (isGroup && conversation.participantPhones.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.padding(start = 6.dp)
            ) {
              Text(
                text = "${conversation.participantPhones.size + 1} members",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}

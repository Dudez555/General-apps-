package com.example.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import com.example.data.model.Conversation
import com.example.ui.screens.ai.AiChatScreen
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.chat.ConversationScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun HomeScreen(
  chatViewModel: ChatViewModel,
  callViewModel: CallViewModel,
  onSignOut: () -> Unit
) {
  val activeConversation by chatViewModel.activeConversation.collectAsState()
  var selectedTab by remember { mutableIntStateOf(0) }

  // If a conversation is active, render full ConversationScreen
  if (activeConversation != null) {
    ConversationScreen(
      conversation = activeConversation!!,
      viewModel = chatViewModel,
      callViewModel = callViewModel,
      onBack = { chatViewModel.clearActiveConversation() }
    )
    return
  }

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

  if (isLandscape) {
    // Landscape / Tablet layout: NavigationRail on side
    Row(modifier = Modifier.fillMaxSize()) {
      NavigationRail(
        modifier = Modifier.testTag("navigation_rail")
      ) {
        NavigationRailItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats") },
          label = { Text("Chats") },
          modifier = Modifier.testTag("tab_chats")
        )
        NavigationRailItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
          label = { Text("AI") },
          modifier = Modifier.testTag("tab_ai")
        )
        NavigationRailItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
          label = { Text("Settings") },
          modifier = Modifier.testTag("tab_settings")
        )
      }

      Box(modifier = Modifier.weight(1f)) {
        when (selectedTab) {
          0 -> ChatListScreen(
            viewModel = chatViewModel,
            onOpenConversation = { conv -> chatViewModel.selectConversation(conv) }
          )
          1 -> AiChatScreen(
            onInsertToChat = { text ->
              selectedTab = 0
            }
          )
          2 -> SettingsScreen(
            viewModel = chatViewModel,
            onSignOut = onSignOut
          )
        }
      }
    }
  } else {
    // Portrait layout: NavigationBar at bottom
    Scaffold(
      bottomBar = {
        NavigationBar(modifier = Modifier.testTag("navigation_bar")) {
          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            modifier = Modifier.testTag("tab_chats")
          )
          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
            label = { Text("AI Assistant") },
            modifier = Modifier.testTag("tab_ai")
          )
          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            modifier = Modifier.testTag("tab_settings")
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (selectedTab) {
          0 -> ChatListScreen(
            viewModel = chatViewModel,
            onOpenConversation = { conv -> chatViewModel.selectConversation(conv) }
          )
          1 -> AiChatScreen(
            onInsertToChat = { text ->
              selectedTab = 0
            }
          )
          2 -> SettingsScreen(
            viewModel = chatViewModel,
            onSignOut = onSignOut
          )
        }
      }
    }
  }
}

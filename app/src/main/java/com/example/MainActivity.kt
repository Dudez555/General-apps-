package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.NotificationHelper
import com.example.ui.configureAppCheck
import com.example.ui.screens.auth.SignInScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

  private val chatViewModel: ChatViewModel by viewModels()
  private val callViewModel: CallViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mandated: Configure App Check before any UI or Firebase operations
    configureAppCheck(this, intent)

    // Initialize notification channels
    NotificationHelper.createNotificationChannels(this)

    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppRoot(
            chatViewModel = chatViewModel,
            callViewModel = callViewModel
          )
        }
      }
    }
  }
}

@Composable
fun AppRoot(
  chatViewModel: ChatViewModel,
  callViewModel: CallViewModel
) {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
      if (auth.currentUser != null) {
        chatViewModel.loadConversations()
      }
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  if (currentUser == null) {
    SignInScreen(
      onAuthSuccess = {
        currentUser = Firebase.auth.currentUser
        chatViewModel.loadConversations()
      }
    )
  } else {
    HomeScreen(
      chatViewModel = chatViewModel,
      callViewModel = callViewModel,
      onSignOut = {
        currentUser = null
        chatViewModel.clearActiveConversation()
      }
    )
  }
}

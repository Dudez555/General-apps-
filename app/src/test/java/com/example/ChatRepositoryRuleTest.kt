package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createGroupConversation_authenticatedUser_createsSuccessfully(): Unit = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ChatRepository(firestore)

    val groupTitle = "Engineering Team ${UUID.randomUUID().toString().take(4)}"
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createGroupConversation(
        title = groupTitle,
        participantPhones = listOf("+15551234567", "+15559876543"),
        isEncrypted = true
      )
    }

    assertTrue(result.isSuccess)
    val convId = result.getOrThrow()
    assertTrue(convId.isNotEmpty())
  }

  @Test
  fun sendMessage_authenticatedUser_storesMessageSuccessfully(): Unit = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ChatRepository(firestore)

    val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createDirectConversation("+15550001111", "Alice Contact").getOrThrow()
    }

    val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.sendMessage(
        conversationId = convId,
        content = "Hello over Wi-Fi without SIM!",
        recipientPhone = "+15550001111",
        isEncrypted = false
      )
    }

    assertTrue(sendResult.isSuccess)
  }

  @Test
  fun observeConversations_authenticatedUser_emitsList(): Unit = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ChatRepository(firestore)

    val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createGroupConversation("Design Sprint", listOf("+15553334444")).getOrThrow()
    }

    val conversations = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.observeConversations().first { list ->
        list.any { it.conversationId == convId }
      }
    }

    assertTrue(conversations.any { it.conversationId == convId })
  }

  @Test(expected = IllegalStateException::class)
  fun observeConversations_unauthenticatedUser_throwsIllegalStateException(): Unit = runBlocking {
    auth.signOut()
    val repository = ChatRepository(firestore)
    repository.observeConversations()
    Unit
  }

  private companion object {
    const val ALICE_EMAIL = "alice_wifisms@test.com"
    const val DEFAULT_TIMEOUT_MS = 6000L
  }
}

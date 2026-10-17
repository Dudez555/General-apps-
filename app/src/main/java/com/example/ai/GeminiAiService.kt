package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

object GeminiAiService {
  private const val TAG = "GeminiAiService"

  // Lazy models to prevent class initialization crashes before App Check
  private val flashModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction = content {
        text("You are WiFiSMS AI Assistant, a smart, polite, and helpful communication assistant. You help users draft secure messages, transcribe voice memos, summarize group chats, and answer questions. Keep answers clear and friendly.")
      }
    )
  }

  private val liteModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.1-flash-lite",
      systemInstruction = content {
        text("You are a high-speed, ultra low-latency assistant for WiFiSMS. Provide concise, direct, and fast answers.")
      }
    )
  }

  private val argonModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-4-argon",
      systemInstruction = content {
        text("You are WiFiSMS Advanced Reasoning Engine. You provide in-depth analysis, structured breakdown, and strategic insights.")
      }
    )
  }

  fun getModel(modelType: String) = when (modelType) {
    "gemini-3.1-flash-lite" -> liteModel
    "gemini-4-argon" -> argonModel
    else -> flashModel
  }

  suspend fun generateResponse(
    prompt: String,
    modelType: String = "gemini-3.8-flash"
  ): String {
    return try {
      withTimeout(45_000L) {
        val model = getModel(modelType)
        val response = model.generateContent(prompt)
        response.text ?: "No response generated."
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Error generating AI response: ${e.message}", e)
      "AI Service error: ${e.localizedMessage ?: "Unable to complete request"}"
    }
  }

  fun generateStream(
    prompt: String,
    modelType: String = "gemini-3.8-flash"
  ): Flow<String> = flow {
    try {
      withTimeout(60_000L) {
        val model = getModel(modelType)
        model.generateContentStream(prompt).collect { chunk ->
          val chunkText = chunk.text.orEmpty()
          if (chunkText.isNotEmpty()) {
            emit(chunkText)
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Streaming error: ${e.message}", e)
      emit("\n[Streaming error: ${e.localizedMessage ?: "Connection interrupted"}]")
    }
  }

  suspend fun transcribeVoiceNote(approxDurationSec: Int, noteSummaryContext: String? = null): String {
    return try {
      withTimeout(30_000L) {
        val prompt = "Transcribe this simulated voice audio note of approximately $approxDurationSec seconds" +
          if (!noteSummaryContext.isNullOrBlank()) " relating to: $noteSummaryContext." else ". Provide a natural spoken message transcription as spoken by a mobile user."
        val response = liteModel.generateContent(prompt)
        response.text?.trim() ?: "Voice note recorded ($approxDurationSec sec)"
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      "Voice message ($approxDurationSec sec)"
    }
  }

  suspend fun generateSmartReplies(lastMessage: String): List<String> {
    return try {
      withTimeout(15_000L) {
        val prompt = "Given this incoming message: \"$lastMessage\", output exactly 3 short, realistic quick reply options separated by commas. No quotes, no numbers. Example: Sounds good!, Will do, On my way!"
        val response = liteModel.generateContent(prompt)
        val text = response.text.orEmpty()
        text.split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() }.take(3)
      }
    } catch (e: Exception) {
      listOf("Sounds good! 👍", "I'll check now.", "Got it, thanks!")
    }
  }
}

package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CallStatus {
  IDLE,
  OUTGOING_RINGING,
  IN_CALL,
  ENDED
}

data class CallUiState(
  val status: CallStatus = CallStatus.IDLE,
  val conversationTitle: String = "",
  val isVideoEnabled: Boolean = true,
  val isMuted: Boolean = false,
  val isFrontCamera: Boolean = true,
  val isScreenSharing: Boolean = false,
  val durationSeconds: Int = 0,
  val resolution: String = "1080p HD • 60 FPS",
  val latencyMs: Int = 18,
  val connectionMode: String = "Wi-Fi Direct P2P"
)

class CallViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(CallUiState())
  val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

  private var timerJob: Job? = null

  fun startCall(title: String, isVideo: Boolean = true) {
    _uiState.value = CallUiState(
      status = CallStatus.OUTGOING_RINGING,
      conversationTitle = title,
      isVideoEnabled = isVideo,
      durationSeconds = 0
    )

    viewModelScope.launch {
      // Simulate rapid peer connection over Wi-Fi
      delay(1800)
      if (_uiState.value.status == CallStatus.OUTGOING_RINGING) {
        _uiState.value = _uiState.value.copy(status = CallStatus.IN_CALL)
        startTimer()
      }
    }
  }

  private fun startTimer() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (isActive && _uiState.value.status == CallStatus.IN_CALL) {
        delay(1000)
        _uiState.value = _uiState.value.copy(
          durationSeconds = _uiState.value.durationSeconds + 1,
          latencyMs = (16..24).random()
        )
      }
    }
  }

  fun toggleMute() {
    _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
  }

  fun toggleVideo() {
    _uiState.value = _uiState.value.copy(isVideoEnabled = !_uiState.value.isVideoEnabled)
  }

  fun switchCamera() {
    _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
  }

  fun toggleScreenSharing() {
    val willShare = !_uiState.value.isScreenSharing
    _uiState.value = _uiState.value.copy(
      isScreenSharing = willShare,
      resolution = if (willShare) "1080p HD Screen Share (60 FPS)" else "1080p HD • 60 FPS"
    )
  }

  fun endCall() {
    timerJob?.cancel()
    _uiState.value = _uiState.value.copy(status = CallStatus.ENDED)
    viewModelScope.launch {
      delay(800)
      _uiState.value = CallUiState()
    }
  }
}

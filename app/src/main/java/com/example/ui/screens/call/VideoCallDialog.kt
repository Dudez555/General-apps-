package com.example.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.CallStatus
import com.example.ui.viewmodel.CallViewModel

@Composable
fun VideoCallDialog(
  viewModel: CallViewModel,
  onDismiss: () -> Unit
) {
  val state by viewModel.uiState.collectAsState()

  if (state.status == CallStatus.IDLE) return

  Dialog(
    onDismissRequest = { viewModel.endCall() },
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F172A))
    ) {
      // Main Surface: either Screen Sharing Presentation or Video Stream
      if (state.isScreenSharing) {
        // High-Definition Screen Share Presentation Canvas
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .height(480.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(8.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.ScreenShare,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(64.dp)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Live High-Definition Screen Sharing",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Streaming full display in ${state.resolution} • Ultra Low-Latency Wi-Fi Link",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
              )
              Spacer(modifier = Modifier.height(20.dp))
              Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF22C55E))
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Bitrate: 6.2 Mbps • Latency: ${state.latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }
        }
      } else {
        // Video Stream simulation with camera state
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          if (state.isVideoEnabled) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color(0xFF334155)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = state.conversationTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (state.status == CallStatus.OUTGOING_RINGING) "Ringing over Wi-Fi..." else "Connected • HD 1080p",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF38BDF8)
              )
            }
          } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.VideocamOff,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(54.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text("Camera is turned off", color = Color.Gray)
            }
          }
        }
      }

      // Picture-in-picture local camera preview (bottom right)
      if (state.isVideoEnabled && state.status == CallStatus.IN_CALL) {
        Surface(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 120.dp, end = 16.dp)
            .size(width = 110.dp, height = 150.dp)
            .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp)),
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF334155)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
              )
              Text(
                text = if (state.isFrontCamera) "Front" else "Rear",
                color = Color.LightGray,
                fontSize = 10.sp
              )
            }
          }
        }
      }

      // Top Status Bar: HD Badge, Wi-Fi info, duration
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .padding(top = 40.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = Color(0x66000000),
          shape = RoundedCornerShape(20.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.HighQuality,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "1080p HD",
              color = Color.White,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (state.status == CallStatus.IN_CALL) {
          val minutes = state.durationSeconds / 60
          val seconds = state.durationSeconds % 60
          Surface(
            color = Color(0x66000000),
            shape = RoundedCornerShape(20.dp)
          ) {
            Text(
              text = String.format("%02d:%02d", minutes, seconds),
              color = Color.White,
              style = MaterialTheme.typography.labelMedium,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }

        Surface(
          color = Color(0x66000000),
          shape = RoundedCornerShape(20.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Wifi,
              contentDescription = null,
              tint = Color(0xFF22C55E),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${state.latencyMs} ms",
              color = Color.White,
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // Bottom Control Dock
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xCC1E293B)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mute Audio
          FilledIconButton(
            onClick = { viewModel.toggleMute() },
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (state.isMuted) Color(0xFFEF4444) else Color(0xFF334155),
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("mute_audio_button")
          ) {
            Icon(
              imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = "Mute Audio"
            )
          }

          // Toggle Video
          FilledIconButton(
            onClick = { viewModel.toggleVideo() },
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (!state.isVideoEnabled) Color(0xFFEF4444) else Color(0xFF334155),
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("toggle_video_button")
          ) {
            Icon(
              imageVector = if (state.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
              contentDescription = "Toggle Video"
            )
          }

          // Switch Camera
          FilledIconButton(
            onClick = { viewModel.switchCamera() },
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = Color(0xFF334155),
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("switch_camera_button")
          ) {
            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera")
          }

          // Screen Share Toggle
          FilledIconButton(
            onClick = { viewModel.toggleScreenSharing() },
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (state.isScreenSharing) Color(0xFF38BDF8) else Color(0xFF334155),
              contentColor = if (state.isScreenSharing) Color.Black else Color.White
            ),
            modifier = Modifier.testTag("screen_share_button")
          ) {
            Icon(
              imageVector = if (state.isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
              contentDescription = "Screen Share"
            )
          }

          // End Call
          FilledIconButton(
            onClick = { viewModel.endCall() },
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = Color(0xFFEF4444),
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("end_call_button")
          ) {
            Icon(Icons.Default.CallEnd, contentDescription = "End Call")
          }
        }
      }
    }
  }
}

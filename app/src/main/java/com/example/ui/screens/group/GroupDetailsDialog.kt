package com.example.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.crypto.E2EEEngine
import com.example.data.model.Conversation

@Composable
fun GroupDetailsDialog(
  conversation: Conversation,
  onDismiss: () -> Unit,
  onAddMember: (String) -> Unit,
  onRemoveMember: (String) -> Unit
) {
  var newMemberPhone by remember { mutableStateOf("") }
  val fingerprint = remember(conversation.conversationId) {
    E2EEEngine.getFingerprint(conversation.conversationId)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(conversation.title, style = MaterialTheme.typography.titleLarge)
          Text(
            if (conversation.type == "group") "Group Chat" else "Direct Chat",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // E2EE Fingerprint Card
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "E2EE Security Fingerprint",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = fingerprint,
              style = MaterialTheme.typography.bodyMedium,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Compare this fingerprint with participants to verify encryption authenticity.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }

        // Add Member Input
        if (conversation.type == "group") {
          Text("Manage Participants", style = MaterialTheme.typography.titleSmall)

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = newMemberPhone,
              onValueChange = { newMemberPhone = it },
              label = { Text("Invite by Phone Number") },
              placeholder = { Text("+1...") },
              modifier = Modifier
                .weight(1f)
                .testTag("invite_phone_input"),
              singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (newMemberPhone.isNotBlank()) {
                  onAddMember(newMemberPhone.trim())
                  newMemberPhone = ""
                }
              },
              enabled = newMemberPhone.isNotBlank(),
              modifier = Modifier.testTag("invite_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add")
            }
          }

          // Participant List
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            conversation.participantPhones.forEach { phone ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.outline,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(phone, style = MaterialTheme.typography.bodyMedium)
                  }
                  IconButton(
                    onClick = { onRemoveMember(phone) },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Remove Member",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}

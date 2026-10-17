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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CreateGroupDialog(
  onDismiss: () -> Unit,
  onCreateGroup: (title: String, phones: List<String>, isEncrypted: Boolean) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var phoneInput by remember { mutableStateOf("") }
  val participants = remember { mutableStateListOf<String>() }
  var isEncrypted by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(6.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("Create New Group", style = MaterialTheme.typography.titleLarge)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Group Name") },
          placeholder = { Text("e.g. Project Alpha, Family") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("group_name_input"),
          singleLine = true
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = phoneInput,
            onValueChange = { phoneInput = it },
            label = { Text("Add Phone Number") },
            placeholder = { Text("+1...") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier
              .weight(1f)
              .testTag("group_phone_input"),
            singleLine = true
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (phoneInput.isNotBlank() && !participants.contains(phoneInput.trim())) {
                participants.add(phoneInput.trim())
                phoneInput = ""
              }
            },
            modifier = Modifier.testTag("add_phone_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add Participant")
          }
        }

        if (participants.isNotEmpty()) {
          Text(
            text = "Participants (${participants.size}):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            participants.forEach { phone ->
              InputChip(
                selected = true,
                onClick = {},
                label = { Text(phone) },
                trailingIcon = {
                  IconButton(
                    onClick = { participants.remove(phone) },
                    modifier = Modifier.size(18.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                  }
                }
              )
            }
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("End-to-End Encryption", style = MaterialTheme.typography.labelLarge)
              Text("AES-256-GCM cipher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
          }
          Switch(
            checked = isEncrypted,
            onCheckedChange = { isEncrypted = it }
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreateGroup(title, participants.toList(), isEncrypted)
        },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("confirm_create_group_button")
      ) {
        Text("Create Group")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

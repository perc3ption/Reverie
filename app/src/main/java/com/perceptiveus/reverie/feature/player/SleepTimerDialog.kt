package com.perceptiveus.reverie.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val MAX_SLEEP_TIMER_MINUTES = 24 * 60

private enum class SleepTimerUnit {
    MINUTES,
    HOURS,
}

@Composable
fun SleepTimerDialog(
    remainingMs: Long?,
    onDismiss: () -> Unit,
    onStart: (minutes: Int) -> Unit,
    onCancel: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(SleepTimerUnit.MINUTES) }

    val amount = amountText.toIntOrNull() ?: 0
    val totalMinutes = when (unit) {
        SleepTimerUnit.MINUTES -> amount
        SleepTimerUnit.HOURS -> amount * 60
    }
    val exceedsMax = totalMinutes > MAX_SLEEP_TIMER_MINUTES
    val canStart = amount > 0 && !exceedsMax
    val timerActive = remainingMs != null && remainingMs > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (timerActive) {
                    Text(
                        text = "Time remaining: ${formatSleepRemaining(remainingMs!!)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                } else {
                    Text(
                        text = "Playback will pause when the timer ends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            amountText = input
                        }
                    },
                    label = { Text("Duration") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    SleepTimerUnit.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = unit == option,
                            onClick = { unit = option },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = SleepTimerUnit.entries.size,
                            ),
                        ) {
                            Text(
                                when (option) {
                                    SleepTimerUnit.MINUTES -> "Minutes"
                                    SleepTimerUnit.HOURS -> "Hours"
                                },
                            )
                        }
                    }
                }

                if (exceedsMax) {
                    Text(
                        text = "Maximum sleep timer is 24 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onStart(totalMinutes)
                    onDismiss()
                },
                enabled = canStart,
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            if (timerActive) {
                TextButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                ) {
                    Text("Cancel timer")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
    )
}

private fun formatSleepRemaining(ms: Long): String {
    val totalMinutes = (ms / 60_000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "under 1m"
    }
}

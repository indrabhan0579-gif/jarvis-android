package com.jarvis.assistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jarvis.assistant.ui.theme.JarvisAmberWarning
import com.jarvis.assistant.ui.theme.JarvisCyanPrimary
import com.jarvis.assistant.ui.theme.JarvisDeepBlack
import com.jarvis.assistant.ui.theme.JarvisGreenOnline
import com.jarvis.assistant.ui.theme.JarvisRedAlert
import com.jarvis.assistant.ui.theme.JarvisSurfaceBorder
import com.jarvis.assistant.ui.theme.JarvisSurfaceCard
import com.jarvis.assistant.ui.theme.JarvisTextPrimary
import com.jarvis.assistant.ui.theme.JarvisTextSecondary

@Composable
fun SettingsDialog(
    currentModel: String,
    onModelSelected: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onTestConnection: suspend () -> Pair<Boolean, String>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(JarvisSurfaceCard, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, JarvisCyanPrimary), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = JarvisCyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SYSTEM CONFIGURATION",
                    color = JarvisCyanPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Model Selection
            Text(
                text = "NEURAL MODEL CORE",
                color = JarvisTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            listOf("gemini-3.8-flash", "gemini-4-argon").forEach { model ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = currentModel == model,
                        onClick = { onModelSelected(model) },
                        colors = RadioButtonDefaults.colors(selectedColor = JarvisCyanPrimary)
                    )
                    Text(
                        text = model,
                        color = JarvisTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // API Key override
            Text(
                text = "GEMINI API KEY (CLIENT OVERRIDE)",
                color = JarvisTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text("Paste custom API key or leave blank for local.properties", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyanPrimary,
                    unfocusedBorderColor = JarvisSurfaceBorder,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                )
            )

            if (keyInput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        onSaveApiKey(keyInput)
                        keyInput = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary)
                ) {
                    Text("SAVE KEY TO SECURE PREFERENCES", color = JarvisDeepBlack, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Connection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testStatus = null
                        // In ViewModel or coroutine, test connection
                    },
                    border = BorderStroke(1.dp, JarvisCyanPrimary),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, tint = JarvisCyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TEST CONNECTION", color = JarvisCyanPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = JarvisCyanPrimary, strokeWidth = 2.dp)
                }
            }

            if (testStatus != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = testStatus!!,
                    color = if (testStatus!!.contains("Connected")) JarvisGreenOnline else JarvisRedAlert,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clear history
            OutlinedButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, JarvisRedAlert),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = JarvisRedAlert, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PURGE CONVERSATION HISTORY", color = JarvisRedAlert, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanPrimary),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("CLOSE", color = JarvisDeepBlack, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

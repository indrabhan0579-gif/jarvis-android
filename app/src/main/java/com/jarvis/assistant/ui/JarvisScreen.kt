package com.jarvis.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.AssistantStatus
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.ui.components.ActionCard
import com.jarvis.assistant.ui.components.ArcReactorVisualizer
import com.jarvis.assistant.ui.components.PermissionsDashboard
import com.jarvis.assistant.ui.components.SafetyConfirmationDialog
import com.jarvis.assistant.ui.components.SettingsDialog
import com.jarvis.assistant.ui.theme.JarvisAmberWarning
import com.jarvis.assistant.ui.theme.JarvisCyanLight
import com.jarvis.assistant.ui.theme.JarvisCyanPrimary
import com.jarvis.assistant.ui.theme.JarvisDeepBlack
import com.jarvis.assistant.ui.theme.JarvisGreenOnline
import com.jarvis.assistant.ui.theme.JarvisRedAlert
import com.jarvis.assistant.ui.theme.JarvisSurfaceBorder
import com.jarvis.assistant.ui.theme.JarvisSurfaceCard
import com.jarvis.assistant.ui.theme.JarvisSurfaceDark
import com.jarvis.assistant.ui.theme.JarvisTextPrimary
import com.jarvis.assistant.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    onRequestPermission: (String) -> Unit
) {
    val status by viewModel.status.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val permissions by viewModel.permissions.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = JarvisDeepBlack,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Top HUD Header
            TopHudHeader(
                status = status,
                currentModel = currentModel,
                onOpenSettings = { showSettings = true },
                onOpenPermissions = { showPermissions = true }
            )

            // 2. Central Arc Reactor Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcReactorVisualizer(status = status)
            }

            // 3. Status Status Bar Text
            StatusReadoutBar(status = status)

            // 4. Quick Action Directive Chips
            QuickActionsBar(
                onDirectiveSelected = { directive ->
                    viewModel.onUserSubmittedText(directive)
                }
            )

            // 5. Conversation Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    when (msg.sender) {
                        MessageSender.USER -> UserMessageBubble(msg.text)
                        MessageSender.JARVIS -> JarvisMessageBubble(msg.text)
                        MessageSender.SYSTEM -> {
                            if (msg.actionExecution != null) {
                                ActionCard(result = msg.actionExecution)
                            } else {
                                SystemLogMessage(msg.text)
                            }
                        }
                    }
                }
            }

            // 6. Bottom Control Deck
            BottomControlDeck(
                inputText = inputText,
                onInputTextChanged = { inputText = it },
                status = status,
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.onUserSubmittedText(inputText)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                onToggleListening = {
                    if (status == AssistantStatus.LISTENING) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onRepeat = { viewModel.repeatLastResponse() }
            )
        }
    }

    // Modal: Safety Confirmation
    pendingAction?.let { action ->
        SafetyConfirmationDialog(
            action = action,
            onConfirm = { viewModel.confirmPendingAction() },
            onCancel = { viewModel.cancelPendingAction() }
        )
    }

    // Modal: Settings
    if (showSettings) {
        SettingsDialog(
            currentModel = currentModel,
            onModelSelected = { viewModel.setModel(it) },
            onSaveApiKey = { /* handled via secure prefs */ },
            onTestConnection = { viewModel.geminiService.testConnection() },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showSettings = false }
        )
    }

    // Modal: Permissions Dashboard
    if (showPermissions) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPermissions = false }) {
            PermissionsDashboard(
                permissions = permissions,
                onRequestPermission = { perm ->
                    onRequestPermission(perm)
                    viewModel.refreshPermissions()
                },
                onOpenAppSettings = {
                    viewModel.openAppSettings()
                    showPermissions = false
                }
            )
        }
    }
}

@Composable
fun TopHudHeader(
    status: AssistantStatus,
    currentModel: String,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisSurfaceDark)
            .border(BorderStroke(1.dp, JarvisSurfaceBorder))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(JarvisGreenOnline, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "J.A.R.V.I.S.",
                    color = JarvisCyanPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "NEURAL OS // $currentModel",
                    color = JarvisTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenPermissions) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Permissions",
                    tint = JarvisCyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisCyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatusReadoutBar(status: AssistantStatus) {
    val (statusColor, badgeBg) = when (status) {
        AssistantStatus.LISTENING -> Pair(JarvisCyanLight, JarvisCyanPrimary.copy(alpha = 0.2f))
        AssistantStatus.THINKING -> Pair(JarvisCyanPrimary, JarvisCyanPrimary.copy(alpha = 0.15f))
        AssistantStatus.SPEAKING -> Pair(JarvisGreenOnline, JarvisGreenOnline.copy(alpha = 0.15f))
        AssistantStatus.EXECUTING -> Pair(JarvisAmberWarning, JarvisAmberWarning.copy(alpha = 0.2f))
        AssistantStatus.IDLE -> Pair(JarvisTextSecondary, Color(0xFF101622))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${status.label}: ${status.description.uppercase()}",
                color = statusColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun QuickActionsBar(onDirectiveSelected: (String) -> Unit) {
    val quickDirectives = listOf(
        "Check battery status",
        "What time is it?",
        "Turn on flashlight",
        "Set volume to 75%",
        "Open Wi-Fi settings",
        "Create reminder to review code",
        "Open Calculator"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickDirectives) { directive ->
            Box(
                modifier = Modifier
                    .background(JarvisSurfaceCard, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, JarvisSurfaceBorder), RoundedCornerShape(16.dp))
                    .clickable { onDirectiveSelected(directive) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = directive,
                    color = JarvisCyanLight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(Color(0xFF0F243B), RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .border(BorderStroke(1.dp, JarvisCyanPrimary.copy(alpha = 0.5f)), RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = text,
                color = JarvisTextPrimary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun JarvisMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(JarvisSurfaceCard, RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .border(BorderStroke(1.dp, JarvisSurfaceBorder), RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "JARVIS",
                    color = JarvisCyanPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    color = JarvisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
fun SystemLogMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "[SYS] $text",
            color = JarvisTextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

@Composable
fun BottomControlDeck(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    status: AssistantStatus,
    onSend: () -> Unit,
    onToggleListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onRepeat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisSurfaceDark)
            .border(BorderStroke(1.dp, JarvisSurfaceBorder))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Voice State auxiliary controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onRepeat, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = "Repeat", tint = JarvisCyanPrimary, modifier = Modifier.size(18.dp))
                }
                AnimatedVisibility(visible = status == AssistantStatus.SPEAKING) {
                    IconButton(onClick = onStopSpeaking, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop Voice", tint = JarvisRedAlert, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = "VOICE ENGINE ACTIVE",
                color = JarvisTextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input Bar & Mic Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = "Speak or enter directive...",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyanPrimary,
                    unfocusedBorderColor = JarvisSurfaceBorder,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary,
                    cursorColor = JarvisCyanPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Microphone / Voice Trigger Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (status == AssistantStatus.LISTENING) JarvisCyanLight else JarvisCyanPrimary,
                        CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { onToggleListening() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (status == AssistantStatus.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = JarvisDeepBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (inputText.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(JarvisSurfaceCard, CircleShape)
                        .border(BorderStroke(1.dp, JarvisCyanPrimary), CircleShape)
                        .clip(CircleShape)
                        .clickable { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = JarvisCyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

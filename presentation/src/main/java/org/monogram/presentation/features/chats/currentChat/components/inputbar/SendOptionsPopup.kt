package org.monogram.presentation.features.chats.currentChat.components.inputbar

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.monogram.presentation.R

@Composable
fun SendOptionsPopup(
    expanded: Boolean,
    scheduledMessagesCount: Int,
    onDismiss: () -> Unit,
    onSendSilent: () -> Unit,
    onScheduleMessage: () -> Unit,
    onOpenScheduledMessages: () -> Unit
) {
    var renderPopup by remember { mutableStateOf(expanded) }
    var contentVisible by remember { mutableStateOf(false) }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 0.44f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "SendOptionsScrimAlpha"
    )
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha)

    LaunchedEffect(expanded) {
        if (expanded) {
            renderPopup = true
            contentVisible = true
        } else if (renderPopup) {
            contentVisible = false
            delay(180)
            renderPopup = false
        }
    }

    if (!renderPopup) return

    Dialog(
        onDismissRequest = {
            if (expanded) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(180)) +
                        slideInVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 700f)) { it / 5 } +
                        scaleIn(
                            animationSpec = spring(dampingRatio = 0.86f, stiffness = 650f),
                            initialScale = 0.92f,
                            transformOrigin = TransformOrigin(1f, 1f)
                        ),
                exit = fadeOut(animationSpec = tween(140)) +
                        slideOutVertically(animationSpec = tween(140)) { it / 8 } +
                        scaleOut(
                            animationSpec = tween(140),
                            targetScale = 0.96f,
                            transformOrigin = TransformOrigin(1f, 1f)
                        ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 60.dp)
            ) {
                Surface(
                    modifier = Modifier.widthIn(min = 220.dp, max = 260.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 18.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        DropdownMenuItem(
                            text = { SendOptionsMenuLabel(title = stringResource(R.string.action_send_silent)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = onSendSilent
                        )

                        DropdownMenuItem(
                            text = {
                                SendOptionsMenuLabel(
                                    title = stringResource(R.string.action_schedule_message),
                                    subtitle = stringResource(R.string.cd_select_date) + " / " + stringResource(R.string.cd_select_time)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = onScheduleMessage
                        )

                        if (scheduledMessagesCount > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            DropdownMenuItem(
                                text = {
                                    SendOptionsMenuLabel(
                                        title = stringResource(
                                            R.string.action_scheduled_messages_count,
                                            scheduledMessagesCount
                                        )
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Subject,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = onOpenScheduledMessages
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SendOptionsMenuLabel(
    title: String,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

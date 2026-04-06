package org.monogram.presentation.features.chats.currentChat.components.inputbar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.monogram.domain.models.MessageModel
import org.monogram.presentation.R

@Composable
fun InputBarLeadingIcons(
    editingMessage: MessageModel?,
    pendingMediaPaths: List<String>,
    canSendMedia: Boolean,
    onAttachClick: () -> Unit
) {
    if (editingMessage == null && pendingMediaPaths.isEmpty() && canSendMedia) {
        IconButton(onClick = onAttachClick) {
            Icon(
                imageVector = Icons.Outlined.AddCircleOutline,
                contentDescription = stringResource(R.string.cd_attach),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (!canSendMedia) {
        Spacer(modifier = Modifier.width(12.dp))
    }
}

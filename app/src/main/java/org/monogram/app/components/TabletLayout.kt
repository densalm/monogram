package org.monogram.app.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.ChildStack
import org.monogram.app.R
import org.monogram.presentation.root.RootComponent

@Composable
fun TabletLayout(root: RootComponent, childStack: ChildStack<*, RootComponent.Child>) {
    val activeChild = childStack.active.instance
    val isSettings = isSettingsSelected(childStack)

    val listChild = remember(childStack) {
        val settingsChild = childStack.backStack.find {
            it.instance is RootComponent.Child.SettingsChild
        }?.instance ?: (activeChild as? RootComponent.Child.SettingsChild)
        val chatsChild = childStack.backStack.find {
            it.instance is RootComponent.Child.ChatsChild
        }?.instance ?: (activeChild as? RootComponent.Child.ChatsChild)

        if (isSettings && settingsChild != null) {
            settingsChild
        } else {
            chatsChild
        }
    }

    Row(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .width(350.dp)
                .fillMaxHeight(),
        ) {
            if (listChild != null) {
                RenderChild(listChild)
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val isListOnly = activeChild == listChild

            if (!isListOnly) {
                RenderChild(activeChild)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSettings) {
                            stringResource(R.string.tablet_select_setting)
                        } else {
                            stringResource(R.string.tablet_select_chat)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun isSettingsSelected(stack: ChildStack<*, RootComponent.Child>): Boolean {
    return when (stack.active.instance) {
        is RootComponent.Child.SettingsChild,
        is RootComponent.Child.EditProfileChild,
        is RootComponent.Child.SessionsChild,
        is RootComponent.Child.FoldersChild,
        is RootComponent.Child.ChatSettingsChild,
        is RootComponent.Child.DataStorageChild,
        is RootComponent.Child.StorageUsageChild,
        is RootComponent.Child.NetworkUsageChild,
        is RootComponent.Child.PrivacyChild,
        is RootComponent.Child.AdBlockChild,
        is RootComponent.Child.PowerSavingChild,
        is RootComponent.Child.NotificationsChild,
        is RootComponent.Child.PremiumChild,
        is RootComponent.Child.ProxyChild,
        is RootComponent.Child.StickersChild,
        is RootComponent.Child.AboutChild,
        is RootComponent.Child.DebugChild -> true

        else -> false
    }
}

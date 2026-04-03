package org.monogram.presentation.features.chats.currentChat.components.inputbar

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import org.monogram.domain.models.MessageEntityType
import org.monogram.domain.models.StickerModel
import org.monogram.domain.repository.StickerRepository
import org.monogram.presentation.R
import org.monogram.presentation.core.ui.ItemPosition
import org.monogram.presentation.features.chats.chatList.components.SettingsTextField
import org.monogram.presentation.features.chats.currentChat.components.VideoPlayerPool
import org.monogram.presentation.features.stickers.ui.menu.StickerEmojiMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenEditorSheet(
    visible: Boolean,
    textValue: TextFieldValue,
    onTextValueChange: (TextFieldValue) -> Unit,
    canWriteText: Boolean,
    pendingMediaPaths: List<String>,
    knownCustomEmojis: MutableMap<Long, StickerModel>,
    emojiFontFamily: FontFamily,
    isKeyboardVisible: Boolean,
    isOverMessageLimit: Boolean,
    currentMessageLength: Int,
    maxMessageLength: Int,
    videoPlayerPool: VideoPlayerPool,
    stickerRepository: StickerRepository,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onEditorFocus: () -> Unit,
    onDraftAutosave: (String) -> Unit = {}
) {
    if (!visible) return
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkValue by remember { mutableStateOf("https://") }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var languageValue by remember { mutableStateOf("") }
    var isPreviewMode by remember { mutableStateOf(false) }
    var markdownMode by remember { mutableStateOf(false) }
    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceValue by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showAutoSaved by remember { mutableStateOf(false) }
    var fontScale by remember { mutableFloatStateOf(1f) }

    val snippetStore = remember(context) { EditorSnippetStore(context) }
    var userSnippets by remember { mutableStateOf(snippetStore.load()) }
    val builtInSnippets = remember {
        listOf(
            EditorSnippet(
                title = "Quick reply",
                text = "Thanks, got it. I will review this and get back to you soon."
            ),
            EditorSnippet(
                title = "Status update",
                text = "Update: task is in progress, current status is stable, next checkpoint in 30 min."
            ),
            EditorSnippet(
                title = "Release note",
                text = "Release notes:\n- Added improvements\n- Fixed edge cases\n- Improved performance"
            )
        )
    }
    val allSnippets = remember(userSnippets, builtInSnippets) { builtInSnippets + userSnippets }

    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    val matches = remember(textValue.text, findQuery) { findOccurrences(textValue.text, findQuery) }
    val wordCount = remember(textValue.text) {
        Regex("\\S+").findAll(textValue.text).count()
    }
    val readingMinutes = remember(wordCount) { ((wordCount + 179) / 180).coerceAtLeast(1) }

    val previewPrimaryColor = MaterialTheme.colorScheme.primary
    val previewText = remember(textValue.annotatedString, knownCustomEmojis.size, previewPrimaryColor) {
        buildEditorPreviewAnnotatedString(
            source = textValue.annotatedString,
            primaryColor = previewPrimaryColor
        )
    }

    fun applyEditorChange(newValue: TextFieldValue, trackHistory: Boolean = true) {
        if (newValue == textValue) return
        if (trackHistory && newValue.text != textValue.text) {
            if (undoStack.lastOrNull() != textValue) {
                undoStack += textValue
                if (undoStack.size > 60) undoStack.removeAt(0)
            }
            redoStack.clear()
        }
        onTextValueChange(newValue)
    }

    fun focusMatch(targetIndex: Int) {
        if (matches.isEmpty()) return
        val normalized = ((targetIndex % matches.size) + matches.size) % matches.size
        currentMatchIndex = normalized
        val range = matches[normalized]
        applyEditorChange(
            textValue.copy(selection = TextRange(range.first, range.last + 1)),
            trackHistory = false
        )
    }

    val entities = remember(textValue.annotatedString, knownCustomEmojis.size) {
        extractEntities(textValue.annotatedString, knownCustomEmojis)
    }
    val richEntityCount = remember(entities) { entities.count { richEntityToAnnotation(it.type) != null } }
    val hasSelection = hasFormattableSelection(textValue)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(visible) {
        if (visible) {
            undoStack.clear()
            redoStack.clear()
            showAutoSaved = false
        }
    }

    LaunchedEffect(findQuery, matches.size) {
        if (matches.isEmpty()) {
            currentMatchIndex = 0
        } else if (currentMatchIndex >= matches.size) {
            currentMatchIndex = matches.lastIndex
        }
    }

    LaunchedEffect(textValue.text, visible) {
        if (!visible || textValue.text.isBlank()) return@LaunchedEffect
        delay(900)
        onDraftAutosave(textValue.text)
        showAutoSaved = true
        delay(1000)
        showAutoSaved = false
    }

    val onSendClick: () -> Unit = {
        val outgoingValue = if (markdownMode) applyMarkdownFormatting(textValue) else textValue
        if (outgoingValue != textValue) {
            onTextValueChange(outgoingValue)
        }
        onSend()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        FullScreenEditorSystemBars()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .imePadding()
            ) {
                FullScreenEditorHeader(isOverMessageLimit, onDismiss, onSendClick)
                FullScreenEditorTopActions(
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    isPreviewMode = isPreviewMode,
                    markdownMode = markdownMode,
                    showFindReplace = showFindReplace,
                    fontScale = fontScale,
                    onUndo = {
                        if (undoStack.isNotEmpty()) {
                            val previous = undoStack.removeAt(undoStack.lastIndex)
                            if (redoStack.lastOrNull() != textValue) redoStack += textValue
                            onTextValueChange(previous)
                        }
                    },
                    onRedo = {
                        if (redoStack.isNotEmpty()) {
                            val next = redoStack.removeAt(redoStack.lastIndex)
                            if (undoStack.lastOrNull() != textValue) undoStack += textValue
                            onTextValueChange(next)
                        }
                    },
                    onTogglePreview = { isPreviewMode = !isPreviewMode },
                    onToggleMarkdown = { markdownMode = !markdownMode },
                    onToggleFindReplace = { showFindReplace = !showFindReplace },
                    onTemplatesClick = { showTemplatesSheet = true },
                    onZoomOut = { fontScale = (fontScale - 0.1f).coerceAtLeast(0.8f) },
                    onZoomIn = { fontScale = (fontScale + 0.1f).coerceAtMost(1.6f) }
                )

                AnimatedVisibility(visible = showFindReplace) {
                    FullScreenEditorFindReplaceBar(
                        query = findQuery,
                        replacement = replaceValue,
                        matchesCount = matches.size,
                        currentMatchIndex = currentMatchIndex,
                        onQueryChange = {
                            findQuery = it
                            currentMatchIndex = 0
                        },
                        onReplacementChange = { replaceValue = it },
                        onPrev = { focusMatch(currentMatchIndex - 1) },
                        onNext = { focusMatch(currentMatchIndex + 1) },
                        onReplace = {
                            if (matches.isNotEmpty()) {
                                val currentRange = matches[currentMatchIndex]
                                applyEditorChange(
                                    applyReplaceAtRange(textValue, currentRange, replaceValue)
                                )
                            }
                        },
                        onReplaceAll = {
                            applyEditorChange(applyReplaceAll(textValue, findQuery, replaceValue))
                        },
                        onClose = { showFindReplace = false }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FullScreenEditorMetaPill(
                        text = stringResource(R.string.message_length_counter, currentMessageLength, maxMessageLength),
                        color = if (isOverMessageLimit) MaterialTheme.colorScheme.error.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.2f
                        ),
                        contentColor = if (isOverMessageLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    FullScreenEditorMetaPill(
                        text = stringResource(R.string.fullscreen_editor_blocks, richEntityCount),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    FullScreenEditorMetaPill(
                        text = stringResource(R.string.editor_word_count, wordCount),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                    FullScreenEditorMetaPill(
                        text = stringResource(R.string.editor_reading_time, readingMinutes),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    if (isPreviewMode) {
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale.coerceIn(0.8f, 1.6f)
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    } else {
                        InputTextField(
                            textValue = textValue,
                            onValueChange = {
                                applyEditorChange(
                                    mergeInputTextValuePreservingAnnotations(textValue, it)
                                )
                            },
                            onRichTextValueChange = { applyEditorChange(it) },
                            enableContextMenu = false,
                            enableRichContextActions = false,
                            canWriteText = canWriteText,
                            knownCustomEmojis = knownCustomEmojis,
                            emojiFontFamily = emojiFontFamily,
                            focusRequester = focusRequester,
                            pendingMediaPaths = pendingMediaPaths,
                            fontScale = fontScale,
                            maxEditorHeight = 860.dp,
                            onFocus = onEditorFocus,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                AnimatedVisibility(visible = !isPreviewMode) {
                    FullScreenEditorTools(
                        hasSelection = hasSelection,
                        onBold = { applyEditorChange(toggleRichEntity(textValue, MessageEntityType.Bold)) },
                        onItalic = { applyEditorChange(toggleRichEntity(textValue, MessageEntityType.Italic)) },
                        onUnderline = { applyEditorChange(toggleRichEntity(textValue, MessageEntityType.Underline)) },
                        onStrike = { applyEditorChange(toggleRichEntity(textValue, MessageEntityType.Strikethrough)) },
                        onSpoiler = { applyEditorChange(toggleRichEntity(textValue, MessageEntityType.Spoiler)) },
                        onCode = { applyEditorChange(toggleRichEntity(textValue, MessageEntityType.Code)) },
                        onLink = {
                            linkValue = currentTextUrl(textValue) ?: "https://"
                            showLinkDialog = true
                        },
                        onMention = { applyEditorChange(insertMentionAtSelection(textValue)) },
                        onPre = {
                            languageValue = currentPreLanguage(textValue)
                            showLanguageDialog = true
                        },
                        onClear = { applyEditorChange(clearRichFormatting(textValue)) },
                        onEmoji = { showEmojiPicker = true }
                    )
                }
                AnimatedVisibility(visible = showAutoSaved) {
                    Text(
                        text = stringResource(R.string.editor_autosave_done),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                AnimatedVisibility(visible = !isKeyboardVisible) {
                    Text(
                        text = stringResource(R.string.fullscreen_editor_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(text = stringResource(R.string.rich_text_link_title)) },
            text = {
                SettingsTextField(
                    value = linkValue,
                    onValueChange = { linkValue = it },
                    placeholder = stringResource(R.string.rich_text_link_hint),
                    icon = Icons.Outlined.Link,
                    position = ItemPosition.STANDALONE,
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    normalizeEditorUrl(linkValue)?.let {
                        applyEditorChange(applyTextUrlEntity(textValue, it))
                    }
                    showLinkDialog = false
                }) { Text(text = stringResource(R.string.action_apply)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLinkDialog = false
                }) { Text(text = stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(text = stringResource(R.string.rich_text_code_language_title)) },
            text = {
                SettingsTextField(
                    value = languageValue,
                    onValueChange = { languageValue = it },
                    placeholder = stringResource(R.string.rich_text_code_language_hint),
                    icon = Icons.Outlined.Code,
                    position = ItemPosition.STANDALONE,
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    applyEditorChange(applyPreEntity(textValue, languageValue))
                    showLanguageDialog = false
                }) { Text(text = stringResource(R.string.action_apply)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLanguageDialog = false
                }) { Text(text = stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showEmojiPicker) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            StickerEmojiMenu(
                onStickerSelected = {},
                onEmojiSelected = { emoji, sticker ->
                    applyEditorChange(insertEmojiAtSelection(textValue, emoji, sticker, knownCustomEmojis))
                },
                onGifSelected = {},
                emojiOnlyMode = true,
                onSearchFocused = {},
                videoPlayerPool = videoPlayerPool,
                stickerRepository = stickerRepository
            )
        }
    }

    FullScreenEditorTemplatesSheet(
        visible = showTemplatesSheet,
        currentText = textValue.text,
        snippets = allSnippets,
        onDismiss = { showTemplatesSheet = false },
        onInsertSnippet = { snippetText ->
            applyEditorChange(insertSnippetAtSelection(textValue, snippetText))
            showTemplatesSheet = false
        },
        onSaveCurrentAsSnippet = { title ->
            val snippet = EditorSnippet(title = title, text = textValue.text)
            userSnippets = (userSnippets + snippet).distinctBy { it.title + it.text }
            snippetStore.save(userSnippets)
        },
        onDeleteSnippet = { snippet ->
            userSnippets = userSnippets - snippet
            snippetStore.save(userSnippets)
        }
    )
}

@Composable
private fun FullScreenEditorSystemBars() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
    val useDarkIcons = backgroundColor.luminance() > 0.5f

    DisposableEffect(window, useDarkIcons) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val previousLightStatus = insetsController.isAppearanceLightStatusBars
        val previousLightNavigation = insetsController.isAppearanceLightNavigationBars

        insetsController.isAppearanceLightStatusBars = useDarkIcons
        insetsController.isAppearanceLightNavigationBars = useDarkIcons

        onDispose {
            insetsController.isAppearanceLightStatusBars = previousLightStatus
            insetsController.isAppearanceLightNavigationBars = previousLightNavigation
        }
    }
}

@Composable
private fun FullScreenEditorHeader(isOverLimit: Boolean, onDismiss: () -> Unit, onSend: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_cancel),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = stringResource(R.string.fullscreen_editor_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSend, enabled = !isOverLimit) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.action_send),
                tint = if (isOverLimit) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FullScreenEditorTopActions(
    canUndo: Boolean,
    canRedo: Boolean,
    isPreviewMode: Boolean,
    markdownMode: Boolean,
    showFindReplace: Boolean,
    fontScale: Float,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTogglePreview: () -> Unit,
    onToggleMarkdown: () -> Unit,
    onToggleFindReplace: () -> Unit,
    onTemplatesClick: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TextButton(onClick = onUndo, enabled = canUndo) {
            Text(text = stringResource(R.string.editor_undo))
        }
        TextButton(onClick = onRedo, enabled = canRedo) {
            Text(text = stringResource(R.string.editor_redo))
        }
        TextButton(onClick = onTogglePreview) {
            Text(
                text = if (isPreviewMode) {
                    stringResource(R.string.editor_mode_edit)
                } else {
                    stringResource(R.string.editor_mode_preview)
                }
            )
        }
        TextButton(onClick = onToggleMarkdown) {
            Text(
                text = if (markdownMode) {
                    stringResource(R.string.editor_markdown_on)
                } else {
                    stringResource(R.string.editor_markdown_off)
                }
            )
        }
        TextButton(onClick = onToggleFindReplace) {
            Text(
                text = if (showFindReplace) {
                    stringResource(R.string.action_close)
                } else {
                    stringResource(R.string.editor_find)
                }
            )
        }
        TextButton(onClick = onTemplatesClick) {
            Text(text = stringResource(R.string.editor_templates))
        }
        TextButton(onClick = onZoomOut, enabled = fontScale > 0.8f) {
            Text(text = stringResource(R.string.editor_zoom_out))
        }
        TextButton(onClick = onZoomIn, enabled = fontScale < 1.6f) {
            Text(text = stringResource(R.string.editor_zoom_in))
        }
    }
}

@Composable
private fun FullScreenEditorMetaPill(text: String, color: Color, contentColor: Color) {
    Surface(color = color, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FullScreenEditorToolButton(icon: ImageVector, hint: String, enabled: Boolean = true, onClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = { Toast.makeText(context, hint, Toast.LENGTH_SHORT).show() })
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = hint,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.38f
            )
        )
    }
}

@Composable
private fun FullScreenEditorTools(
    hasSelection: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrike: () -> Unit,
    onSpoiler: () -> Unit,
    onCode: () -> Unit,
    onLink: () -> Unit,
    onMention: () -> Unit,
    onPre: () -> Unit,
    onClear: () -> Unit,
    onEmoji: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(58.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FullScreenEditorToolButton(
                    Icons.Outlined.FormatBold,
                    stringResource(R.string.rich_text_bold),
                    hasSelection,
                    onBold
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.FormatItalic,
                    stringResource(R.string.rich_text_italic),
                    hasSelection,
                    onItalic
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.FormatUnderlined,
                    stringResource(R.string.rich_text_underline),
                    hasSelection,
                    onUnderline
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.FormatStrikethrough,
                    stringResource(R.string.rich_text_strikethrough),
                    hasSelection,
                    onStrike
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.VisibilityOff,
                    stringResource(R.string.rich_text_spoiler),
                    hasSelection,
                    onSpoiler
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.Code,
                    stringResource(R.string.rich_text_code),
                    hasSelection,
                    onCode
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.Link,
                    stringResource(R.string.rich_text_link),
                    hasSelection,
                    onLink
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.AlternateEmail,
                    stringResource(R.string.rich_text_mention),
                    true,
                    onMention
                )
                FullScreenEditorToolButton(
                    Icons.AutoMirrored.Outlined.Subject,
                    stringResource(R.string.rich_text_pre),
                    hasSelection,
                    onPre
                )
                FullScreenEditorToolButton(
                    Icons.Outlined.FormatClear,
                    stringResource(R.string.rich_text_clear),
                    hasSelection,
                    onClear
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            IconButton(onClick = onEmoji) {
                Text(
                    text = "☺",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun currentTextUrl(value: TextFieldValue): String? {
    val range = normalizedSelection(value.selection) ?: return null
    return value.annotatedString.getStringAnnotations(RICH_ENTITY_TAG, range.start, range.end)
        .firstOrNull { decodeRichEntity(it.item) is MessageEntityType.TextUrl }
        ?.let { decodeRichEntity(it.item) as? MessageEntityType.TextUrl }
        ?.url
}

private fun currentPreLanguage(value: TextFieldValue): String {
    val range = normalizedSelection(value.selection) ?: return ""
    return value.annotatedString.getStringAnnotations(RICH_ENTITY_TAG, range.start, range.end)
        .firstOrNull { decodeRichEntity(it.item) is MessageEntityType.Pre }
        ?.let { decodeRichEntity(it.item) as? MessageEntityType.Pre }
        ?.language
        .orEmpty()
}

private fun insertSnippetAtSelection(value: TextFieldValue, snippet: String): TextFieldValue {
    if (snippet.isBlank()) return value
    val rawSelection = if (value.selection.start <= value.selection.end) {
        value.selection
    } else {
        TextRange(value.selection.end, value.selection.start)
    }
    val maxLength = value.annotatedString.length
    val selection = TextRange(
        start = rawSelection.start.coerceIn(0, maxLength),
        end = rawSelection.end.coerceIn(0, maxLength)
    )
    val newAnnotated = androidx.compose.ui.text.buildAnnotatedString {
        append(value.annotatedString.subSequence(0, selection.start))
        append(snippet)
        append(value.annotatedString.subSequence(selection.end, value.annotatedString.length))
    }
    val cursor = selection.start + snippet.length
    return value.copy(annotatedString = newAnnotated, selection = TextRange(cursor, cursor))
}

private fun normalizedSelection(selection: TextRange): TextRange? {
    if (selection.start == selection.end) return null
    return if (selection.start <= selection.end) selection else TextRange(selection.end, selection.start)
}

private fun normalizeEditorUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return if (trimmed.contains("://")) trimmed else "https://$trimmed"
}

private fun insertMentionAtSelection(value: TextFieldValue): TextFieldValue {
    val rawSelection = if (value.selection.start <= value.selection.end) value.selection else TextRange(
        value.selection.end,
        value.selection.start
    )
    val maxLength = value.annotatedString.length
    val selection = TextRange(
        start = rawSelection.start.coerceIn(0, maxLength),
        end = rawSelection.end.coerceIn(0, maxLength)
    )
    val insertion =
        if (selection.start == selection.end) "@" else "@${value.text.substring(selection.start, selection.end)}"
    val newAnnotated = androidx.compose.ui.text.buildAnnotatedString {
        append(value.annotatedString.subSequence(0, selection.start))
        append(insertion)
        append(value.annotatedString.subSequence(selection.end, value.annotatedString.length))
    }
    val newCursor = selection.start + insertion.length
    return value.copy(annotatedString = newAnnotated, selection = TextRange(newCursor, newCursor))
}

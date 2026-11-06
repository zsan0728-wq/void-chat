/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package io.element.android.glasslab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.libraries.designsystem.components.glass.LocalRefractiveBackdrop
import io.element.android.libraries.designsystem.components.glass.RefractiveBackdrop
import io.element.android.libraries.designsystem.components.glass.RefractiveMessageBubble
import io.element.android.libraries.designsystem.motion.MotionScale
import io.element.android.libraries.designsystem.motion.MotionSpring
import io.element.android.libraries.designsystem.motion.rememberReducedMotion
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.utils.CommonDrawables
import kotlinx.coroutines.launch

/** Deterministic rendering fixture. It deliberately has no Matrix session or network permission. */
class GlassLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedRate = intent.getFloatExtra("requested_refresh_rate", 0f)
        if (requestedRate > 0f) {
            window.attributes = window.attributes.apply { preferredRefreshRate = requestedRate }
        }
        setContent {
            GlassLab(
                initialTheme = intent.getStringExtra("theme") ?: "system",
                initialCompact = intent.getBooleanExtra("compact", false),
                initialAnimated = intent.getBooleanExtra("animated", false),
            )
        }
    }
}

private data class LabMessage(val id: Long, val text: String, val mine: Boolean, val photo: Boolean = false)

@Composable
private fun GlassLab(initialTheme: String, initialCompact: Boolean, initialAnimated: Boolean) {
    val systemDark = isSystemInDarkTheme()
    var darkOverride by rememberSaveable {
        mutableStateOf<Boolean?>(
            when (initialTheme) {
                "dark" -> true
                "light" -> false
                else -> null
            }
        )
    }
    val dark = darkOverride ?: systemDark
    val hostConfiguration = LocalConfiguration.current
    val configuration = remember(hostConfiguration, dark) {
        Configuration(hostConfiguration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
    }
    // Haze reads host appearance. Keep it consistent with the lab's explicit theme override.
    CompositionLocalProvider(LocalConfiguration provides configuration) {
        ElementTheme(theme = if (dark) Theme.Dark else Theme.Light) {
            GlassLabContent(initialCompact, initialAnimated, onToggleTheme = { darkOverride = !dark })
        }
    }
}

@Composable
private fun GlassLabContent(initialCompact: Boolean, initialAnimated: Boolean, onToggleTheme: () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion()
    val messages = remember {
        mutableStateListOf<LabMessage>().apply {
            for (id in 59L downTo 0L) {
                val resource = if (id % 5 == 0L) R.string.screen_glasslab_long_fixture else R.string.screen_glasslab_fixture
                add(LabMessage(id, context.getString(resource, id), id % 2 == 0L, photo = id % 7 == 0L))
            }
        }
    }
    // IDs awaiting their first placement only. Recycled and historical rows never replay entry.
    val pendingEntries = remember { mutableSetOf<Long>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var compact by rememberSaveable { mutableStateOf(initialCompact) }
    var animated by rememberSaveable { mutableStateOf(initialAnimated) }
    var draft by rememberSaveable { mutableStateOf("") }
    var nextId by rememberSaveable { mutableStateOf(60L) }
    fun append(mine: Boolean) {
        val body = if (mine) draft.trim() else context.getString(R.string.screen_glasslab_received)
        if (body.isEmpty()) return
        val id = nextId++
        pendingEntries.add(id)
        messages.add(0, LabMessage(id, body, mine))
        if (mine) draft = ""
        scope.launch { if (reducedMotion) listState.scrollToItem(0) else listState.animateScrollToItem(0) }
    }
    RefractiveBackdrop(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
        background = { LabWallpaper(animated && !reducedMotion) },
    ) {
        val source = checkNotNull(LocalRefractiveBackdrop.current)
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Text(
                stringResource(R.string.screen_glasslab_title),
                Modifier.padding(horizontal = 16.dp),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LabControl(R.string.screen_glasslab_theme, "theme", onToggleTheme)
                LabControl(R.string.screen_glasslab_density, "density") { compact = !compact }
                LabControl(R.string.screen_glasslab_animate, "wallpaper") { animated = !animated }
                LabControl(R.string.screen_glasslab_receive, "receive") { append(false) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                // Diagnostic mode: exactly 20 slots in the available viewport, independent materials.
                val slotHeight = maxHeight / 20
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize().testTag("messages"),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    items(messages, key = { it.id }, contentType = { if (compact) "compact" else if (it.photo) "photo" else "text" }) { message ->
                        val enter = remember(message.id) { pendingEntries.remove(message.id) }
                        Row(
                            modifier = Modifier.fillMaxWidth().testTag("message_${message.id}")
                                .then(if (compact) Modifier.height(slotHeight) else Modifier),
                            horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
                        ) {
                            LabBubble(
                                message = message,
                                source = source,
                                compact = compact,
                                enter = enter,
                                reducedMotion = reducedMotion,
                                modifier = if (compact) Modifier.height((slotHeight - 2.dp).coerceAtLeast(1.dp)) else Modifier.padding(vertical = 5.dp),
                                onReply = { draft = context.getString(R.string.screen_glasslab_reply_to, message.id) },
                                onDelete = { messages.remove(message) },
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).testTag("composer"),
                    placeholder = { Text(stringResource(R.string.screen_glasslab_message)) },
                    maxLines = 3,
                )
                LabControl(R.string.screen_glasslab_send, "send") { append(true) }
            }
        }
    }
}

@Composable
private fun LabControl(resource: Int, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(tag), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(stringResource(resource), fontSize = 12.sp)
    }
}

@Composable
private fun LabBubble(
    message: LabMessage,
    source: HazeState,
    compact: Boolean,
    enter: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    var menu by remember { mutableStateOf(false) }
    val progress = remember(message.id) { Animatable(if (enter && !reducedMotion) 0f else 1f) }
    val slide = with(LocalDensity.current) { (if (message.mine) 48.dp else 14.dp).toPx() }
    LaunchedEffect(message.id, reducedMotion) {
        if (reducedMotion) progress.snapTo(1f) else progress.animateTo(1f, MotionSpring.message())
    }
    Box(modifier.widthIn(max = 300.dp)) {
        RefractiveMessageBubble(
            state = source,
            isMine = message.mine,
            shape = RoundedCornerShape(if (compact) 8.dp else 20.dp),
            interactionSource = interaction,
            modifier = Modifier
                .graphicsLayer {
                    val scale = MotionScale.messageInitial + (1f - MotionScale.messageInitial) * progress.value
                    scaleX = scale
                    scaleY = scale
                    alpha = progress.value.coerceIn(0f, 1f)
                    translationY = if (message.mine) slide * (1f - progress.value) else 0f
                    translationX = if (message.mine) 0f else -slide * (1f - progress.value)
                    transformOrigin = TransformOrigin(if (message.mine) 1f else 0f, 1f)
                }
                .testTag("glass_bubble")
                .combinedClickable(interactionSource = interaction, indication = null, onClick = {}, onLongClick = { menu = true }),
        ) {
            Column(Modifier.padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 0.dp else 8.dp)) {
                if (message.photo && !compact) {
                    Image(
                        painterResource(CommonDrawables.sample_background),
                        contentDescription = stringResource(R.string.screen_glasslab_photo_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(112.dp).padding(bottom = 6.dp),
                    )
                }
                Text(
                    text = if (compact) stringResource(R.string.screen_glasslab_compact_fixture, message.id) else message.text,
                    fontSize = if (compact) 9.sp else 15.sp,
                    lineHeight = if (compact) 10.sp else 20.sp,
                    maxLines = if (compact) 1 else Int.MAX_VALUE,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.screen_glasslab_copy)) }, onClick = {
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("", message.text))
                menu = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.screen_glasslab_reply)) }, onClick = {
                onReply()
                menu = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.screen_glasslab_delete)) }, onClick = {
                onDelete()
                menu = false
            })
        }
    }
}

@Composable
private fun BoxScope.LabWallpaper(animated: Boolean) {
    Image(
        painter = painterResource(CommonDrawables.sample_background),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
    )
    if (animated) {
        val transition = rememberInfiniteTransition(label = "Diagnostic wallpaper")
        val phase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
            label = "Moving light stripe",
        )
        Canvas(Modifier.matchParentSize()) {
            // Actual changing source pixels make stale sampling and wrong spatial mapping visible.
            drawRect(Color.White.copy(alpha = 0.45f), Offset(size.width * phase.value, 0f), Size(size.width * 0.08f, size.height))
        }
    }
}

@PreviewsDayNight
@Composable
private fun GlassLabPreview() {
    ElementPreview {
        GlassLabContent(initialCompact = false, initialAnimated = false, onToggleTheme = {})
    }
}

@PreviewsDayNight
@Composable
private fun GlassLabDensePreview() {
    ElementPreview {
        GlassLabContent(initialCompact = true, initialAnimated = false, onToggleTheme = {})
    }
}

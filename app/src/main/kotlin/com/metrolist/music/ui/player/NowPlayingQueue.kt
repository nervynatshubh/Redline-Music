/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.SimilarContent
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.QueueMenu
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberPreference
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.abs

private val QueueSidePadding: Dp = PlayerHorizontalPadding - 8.dp

/**
 * Apple Music "Playing Next" mode buttons: Shuffle, Repeat (all → one → off) and Autoplay (similar songs).
 * As in Apple Music, Shuffle and Repeat are unavailable while Autoplay is on.
 */
@Composable
fun NowPlayingQueueModes(
    isListenTogetherGuest: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val shuffleOn by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val (autoplayOn, onAutoplayChange) = rememberPreference(SimilarContent, defaultValue = true)
    val autoplayHint = stringResource(R.string.autoplay_blocks_shuffle_repeat)
    val showAutoplayHint = {
        Toast.makeText(context, autoplayHint, Toast.LENGTH_SHORT).show()
    }

    // With a title: iOS 14 header, "Playing Next" on the left and three small square toggles on the right.
    val buttonModifier: RowScope.() -> Modifier = {
        if (title != null) Modifier.size(width = 34.dp, height = 30.dp) else Modifier.weight(1f)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (title != null) {
            Text(
                text = title,
                color = NowPlayingColors.Primary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        NowPlayingQueueModeButton(
            icon = R.drawable.shuffle,
            contentDescription = stringResource(R.string.shuffle),
            active = shuffleOn,
            available = !autoplayOn && !isListenTogetherGuest,
            onClick = { playerConnection.player.shuffleModeEnabled = !shuffleOn },
            onUnavailableClick = { if (autoplayOn) showAutoplayHint() },
            compact = title != null,
            modifier = buttonModifier(),
        )
        NowPlayingQueueModeButton(
            icon = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            contentDescription = stringResource(R.string.repeat_mode),
            active = repeatMode != Player.REPEAT_MODE_OFF,
            available = !autoplayOn && !isListenTogetherGuest,
            onClick = { playerConnection.player.toggleRepeatMode() },
            onUnavailableClick = { if (autoplayOn) showAutoplayHint() },
            compact = title != null,
            modifier = buttonModifier(),
        )
        NowPlayingQueueModeButton(
            icon = R.drawable.np_infinity,
            contentDescription = stringResource(R.string.autoplay),
            active = autoplayOn,
            available = !isListenTogetherGuest,
            onClick = {
                val enable = !autoplayOn
                onAutoplayChange(enable)
                if (enable) {
                    playerConnection.player.shuffleModeEnabled = false
                    playerConnection.player.repeatMode = Player.REPEAT_MODE_OFF
                }
            },
            compact = title != null,
            modifier = buttonModifier(),
        )
    }
}

/**
 * Apple Music style queue shown inside Now Playing: mode buttons pinned on top, then a scrolling list with
 * History (above, scrolled out of view), "Playing Next" (reorder with the handle, swipe left to remove)
 * and an Autoplay section with the similar songs that will follow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingQueueView(
    playerBottomSheetState: BottomSheetState,
    isListenTogetherGuest: Boolean,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val (autoplayOn) = rememberPreference(SimilarContent, defaultValue = true)

    val castHandler =
        remember(playerConnection) {
            runCatching { playerConnection.service.castConnectionHandler }.getOrNull()
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    val history =
        remember(queueWindows, currentWindowIndex) {
            if (currentWindowIndex > 0) queueWindows.take(currentWindowIndex.coerceAtMost(queueWindows.size)) else emptyList()
        }
    val upcoming = remember { mutableStateListOf<Timeline.Window>() }
    LaunchedEffect(queueWindows, currentWindowIndex) {
        upcoming.clear()
        val start = (currentWindowIndex + 1).coerceAtLeast(0)
        if (start < queueWindows.size) upcoming.addAll(queueWindows.subList(start, queueWindows.size))
    }

    // "Playing Next" header sits right after the History header and its rows.
    val playingNextIndex = if (history.isEmpty()) 0 else history.size + 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = playingNextIndex)
    LaunchedEffect(currentWindowIndex) {
        if (currentWindowIndex >= 0) listState.scrollToItem(playingNextIndex)
    }

    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragTo by remember { mutableStateOf<Int?>(null) }
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromIdx = upcoming.indexOfFirst { it.uid.hashCode() == from.key }
            val toIdx = upcoming.indexOfFirst { it.uid.hashCode() == to.key }
            if (fromIdx != -1 && toIdx != -1) {
                if (dragFrom == null) dragFrom = fromIdx
                dragTo = toIdx
                upcoming.move(fromIdx, toIdx)
            }
        }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging) {
            val from = dragFrom
            val to = dragTo
            if (from != null && to != null && from != to && queueWindows.isNotEmpty()) {
                val base = currentWindowIndex + 1
                val safeFrom = (base + from).coerceIn(0, queueWindows.lastIndex)
                val safeTo = (base + to).coerceIn(0, queueWindows.lastIndex)
                if (!playerConnection.player.shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(safeFrom, safeTo)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows
                                .map { it.firstPeriodIndex }
                                .toMutableList()
                                .move(safeFrom, safeTo)
                                .toIntArray(),
                            System.currentTimeMillis(),
                        ),
                    )
                }
            }
            dragFrom = null
            dragTo = null
        }
    }

    val playWindow: (Timeline.Window) -> Unit = { window ->
        if (!isListenTogetherGuest) {
            if (isCasting) {
                val navigated = castHandler?.navigateToMediaIfInQueue(window.mediaItem.mediaId) ?: false
                if (!navigated) playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
            } else {
                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                playerConnection.player.playWhenReady = true
            }
        }
    }
    val showMenu: (MediaMetadata?, (() -> Unit)?) -> Unit = { metadata, onRemove ->
        if (metadata != null) {
            menuState.show {
                QueueMenu(
                    mediaMetadata = metadata,
                    playerBottomSheetState = playerBottomSheetState,
                    onShowDetailsDialog = {
                        bottomSheetPageState.show {
                            ShowMediaInfo(metadata.id)
                        }
                    },
                    onDismiss = menuState::dismiss,
                    onRemoveFromQueue = onRemove,
                )
            }
        }
    }

    Column(modifier = modifier) {
        NowPlayingQueueModes(
            isListenTogetherGuest = isListenTogetherGuest,
            title = stringResource(R.string.playing_next),
            modifier = Modifier.padding(horizontal = QueueSidePadding),
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalFadingEdges(),
        ) {
            if (history.isNotEmpty()) {
                item(key = "history_header") {
                    QueueSectionHeader(title = stringResource(R.string.history), subtitle = null)
                }
                items(
                    items = history,
                    key = { window -> "history_${window.uid.hashCode()}" },
                ) { window ->
                    NowPlayingQueueRow(
                        metadata = window.mediaItem.metadata,
                        dimmed = true,
                        onClick = { playWindow(window) },
                        onLongClick = { showMenu(window.mediaItem.metadata, null) },
                    )
                }
            }

            item(key = "playing_next_header") {
                // The "Playing Next" title lives in the pinned header; here only the source line.
                val from = queueTitle?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.playing_from, it) }
                Text(
                    text = from.orEmpty(),
                    color = NowPlayingColors.Secondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = QueueSidePadding, end = QueueSidePadding, top = 2.dp, bottom = 6.dp),
                )
            }

            items(
                items = upcoming,
                key = { window -> window.uid.hashCode() },
            ) { window ->
                ReorderableItem(
                    state = reorderState,
                    key = window.uid.hashCode(),
                ) {
                    val currentWindow by rememberUpdatedState(window)
                    val dismissState =
                        rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance * 0.4f },
                        )
                    var removed by remember { mutableStateOf(false) }
                    LaunchedEffect(dismissState.currentValue) {
                        if (!removed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            removed = true
                            playerConnection.player.removeMediaItem(currentWindow.firstPeriodIndex)
                        }
                    }
                    val density = LocalDensity.current

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = !isListenTogetherGuest,
                        backgroundContent = {
                            // Red "remove" strip only under the uncovered part, like iOS swipe-to-delete.
                            val revealed = abs(runCatching { dismissState.requireOffset() }.getOrDefault(0f))
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier =
                                        Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(with(density) { revealed.toDp() })
                                            .background(Color(0xFFFF3B30)),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        },
                    ) {
                        NowPlayingQueueRow(
                            metadata = window.mediaItem.metadata,
                            dimmed = false,
                            onClick = { playWindow(currentWindow) },
                            onLongClick = {
                                showMenu(window.mediaItem.metadata) {
                                    playerConnection.player.removeMediaItem(currentWindow.firstPeriodIndex)
                                }
                            },
                            trailing = {
                                if (!isListenTogetherGuest) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier =
                                            Modifier
                                                .size(40.dp)
                                                .draggableHandle(),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.drag_handle),
                                            contentDescription = null,
                                            tint = NowPlayingColors.Tertiary,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }

            if (autoplayOn && automix.isNotEmpty()) {
                item(key = "autoplay_header") {
                    QueueSectionHeader(
                        title = stringResource(R.string.autoplay),
                        subtitle = stringResource(R.string.autoplay_section_subtitle),
                    )
                }
                itemsIndexed(
                    items = automix,
                    key = { _, item -> "autoplay_${item.mediaId}" },
                ) { index, item ->
                    NowPlayingQueueRow(
                        metadata = item.metadata,
                        dimmed = false,
                        onClick = {
                            if (!isListenTogetherGuest) {
                                playerConnection.service.playNextAutomix(item, index)
                                playerConnection.player.seekToNext()
                            }
                        },
                        onLongClick = { showMenu(item.metadata, null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueSectionHeader(
    title: String,
    subtitle: String?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = QueueSidePadding, end = QueueSidePadding, top = 16.dp, bottom = 6.dp),
    ) {
        Text(
            text = title,
            color = NowPlayingColors.Primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = NowPlayingColors.Secondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NowPlayingQueueRow(
    metadata: MediaMetadata?,
    dimmed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    if (metadata == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = QueueSidePadding, end = QueueSidePadding - 8.dp, top = 6.dp, bottom = 6.dp)
                .alpha(if (dimmed) 0.5f else 1f),
    ) {
        AsyncImage(
            model = metadata.thumbnailUrl?.resize(144, 144),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata.title,
                color = NowPlayingColors.Primary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metadata.artists.joinToString { it.name },
                color = NowPlayingColors.Secondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

/** Soft fade at the top and bottom edges of the list, like Apple's scrolling queue. */
private fun Modifier.verticalFadingEdges(edge: Dp = 20.dp): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fraction = (edge.toPx() / size.height).coerceIn(0f, 0.5f)
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        fraction to Color.Black,
                        1f - fraction to Color.Black,
                        1f to Color.Transparent,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

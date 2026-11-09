/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import com.metrolist.music.ui.utils.resize
import com.metrolist.innertube.YouTube
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.produceState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.innertube.pages.MoodAndGenres
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.utils.rememberIosFlingBehavior
import com.metrolist.music.viewmodels.MoodAndGenresViewModel

/**
 * Apple Music "Browse" tab: shortcut rows (New Releases, Top Charts, Moods & Genres) with red icons
 * and chevrons, followed by colourful category tiles for every mood and genre.
 */
@Composable
fun BrowseTabScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsStateWithLifecycle()

    // Tapping the Browse tab again scrolls back to the top, like iOS.
    val listState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val shortcuts =
        listOf(
            Triple(R.drawable.newspaper, R.string.new_releases, "new_release"),
            Triple(R.drawable.trending_up, R.string.top_charts, "charts_screen"),
            Triple(R.drawable.grid_view, R.string.mood_and_genres, "mood_and_genres"),
        )

    LazyColumn(
        state = listState,
        flingBehavior = rememberIosFlingBehavior(),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            items = shortcuts,
            key = { it.third },
        ) { (icon, label, route) ->
            BrowseShortcutRow(
                icon = icon,
                label = stringResource(label),
                onClick = { navController.navigate(route) },
            )
        }

        if (moodAndGenresList == null) {
            item(key = "browse_shimmer") {
                ShimmerHost {
                    repeat(6) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }

        moodAndGenresList?.forEachIndexed { index, section ->
            item(key = "browse_title_$index") {
                NavigationTitle(title = section.title)
            }
            items(
                items = section.items.chunked(2),
                key = { row -> "browse_${index}_${row.first().endpoint.browseId}_${row.first().title}" },
            ) { row ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    row.forEachIndexed { i, item ->
                        if (i > 0) Spacer(Modifier.width(12.dp))
                        BrowseCategoryTile(
                            item = item,
                            onClick = {
                                navController.navigate(
                                    "youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}",
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.width(12.dp))
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseShortcutRow(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp)
                // Grows with large font sizes instead of clipping the label.
                .heightIn(min = 52.dp)
                .height(IntrinsicSize.Min),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(16.dp))
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            color = hairline,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.5.dp.toPx(),
                        )
                    },
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 25.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterStart).padding(vertical = 10.dp).padding(end = 40.dp),
            )
            Icon(
                painter = painterResource(R.drawable.np_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .size(18.dp),
            )
        }
    }
}

/**
 * Artwork for Browse tiles. YouTube Music sends moods and genres as a name and a colour only, so each
 * tile looks up its own category once and borrows the cover of the first playlist in it. Results are
 * kept in memory for the whole session, and at most 4 lookups run at the same time.
 */
private object BrowseTileArt {
    val cache = ConcurrentHashMap<String, String>()
    val gate = Semaphore(4)

    suspend fun load(item: MoodAndGenres.Item): String? {
        val key = item.endpoint.browseId + "|" + item.endpoint.params
        cache[key]?.let { return it.ifEmpty { null } }
        val url =
            gate.withPermit {
                withContext(Dispatchers.IO) {
                    YouTube
                        .browse(item.endpoint.browseId, item.endpoint.params)
                        .getOrNull()
                        ?.items
                        ?.asSequence()
                        ?.flatMap { it.items.asSequence() }
                        ?.mapNotNull { it.thumbnail }
                        ?.firstOrNull()
                }
            }
        cache[key] = url.orEmpty()
        return url
    }
}

/**
 * Apple Music style category tile: the category's artwork fills the card, fading into the genre's own
 * colour at the bottom behind a bold white label. Shows the plain colour card while the artwork loads.
 */
@Composable
private fun BrowseCategoryTile(
    item: MoodAndGenres.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = Color(item.stripeColor.toInt()).copy(alpha = 1f)
    val shape = RoundedCornerShape(10.dp)
    val artUrl by produceState<String?>(initialValue = null, item.endpoint.browseId, item.endpoint.params) {
        value = runCatching { BrowseTileArt.load(item) }.getOrNull()
    }
    Box(
        modifier =
            modifier
                .height(104.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(base, base.copy(red = base.red * 0.55f, green = base.green * 0.55f, blue = base.blue * 0.55f)),
                    ),
                ).clickable(onClick = onClick),
    ) {
        if (artUrl != null) {
            AsyncImage(
                model = artUrl?.resize(400, 400),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // Genre colour rising from the bottom keeps the title readable on any artwork.
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to base.copy(alpha = 0.25f),
                                1f to base.copy(red = base.red * 0.7f, green = base.green * 0.7f, blue = base.blue * 0.7f),
                            ),
                        ),
            )
        }
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}

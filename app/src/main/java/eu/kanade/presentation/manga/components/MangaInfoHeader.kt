package eu.kanade.presentation.manga.components

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow
import eu.kanade.domain.manga.model.Manga
import eu.kanade.presentation.components.MangaCover
import eu.kanade.presentation.components.TextButton
import eu.kanade.presentation.util.clickableNoIndication
import eu.kanade.presentation.util.quantityStringResource
import eu.kanade.presentation.util.secondaryItemAlpha
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlin.math.roundToInt

@Composable
fun MangaInfoHeader(
    modifier: Modifier = Modifier,
    windowWidthSizeClass: WindowWidthSizeClass,
    appBarPadding: Dp,
    title: String,
    author: String?,
    artist: String?,
    description: String?,
    tagsProvider: () -> List<String>?,
    sourceName: String,
    isStubSource: Boolean,
    coverDataProvider: () -> Manga,
    favorite: Boolean,
    status: Long,
    trackingCount: Int,
    fromSource: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onTagClicked: (String) -> Unit,
    onEditCategory: (() -> Unit)?,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Box {
            // Backdrop
            val backdropGradientColors = listOf(
                Color.Transparent,
                MaterialTheme.colorScheme.background,
            )
            AsyncImage(
                model = coverDataProvider(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(colors = backdropGradientColors),
                        )
                    }
                    .alpha(.2f),
            )

            // Manga & source info
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                    MangaAndSourceTitlesSmall(
                        appBarPadding = appBarPadding,
                        coverDataProvider = coverDataProvider,
                        onCoverClick = onCoverClick,
                        title = title,
                        context = context,
                        doSearch = doSearch,
                        author = author,
                        artist = artist,
                        status = status,
                        sourceName = sourceName,
                        isStubSource = isStubSource,
                    )
                } else {
                    MangaAndSourceTitlesLarge(
                        appBarPadding = appBarPadding,
                        coverDataProvider = coverDataProvider,
                        onCoverClick = onCoverClick,
                        title = title,
                        context = context,
                        doSearch = doSearch,
                        author = author,
                        artist = artist,
                        status = status,
                        sourceName = sourceName,
                        isStubSource = isStubSource,
                    )
                }
            }
        }

        // Action buttons
        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
            val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
            MangaActionButton(
                title = if (favorite) {
                    stringResource(R.string.in_library)
                } else {
                    stringResource(R.string.add_to_library)
                },
                icon = if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                color = if (favorite) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
                onClick = onAddToLibraryClicked,
                onLongClick = onEditCategory,
            )
            if (onTrackingClicked != null) {
                MangaActionButton(
                    title = if (trackingCount == 0) {
                        stringResource(R.string.manga_tracking_tab)
                    } else {
                        quantityStringResource(id = R.plurals.num_trackers, quantity = trackingCount, trackingCount)
                    },
                    icon = if (trackingCount == 0) Icons.Default.Sync else Icons.Default.Done,
                    color = if (trackingCount == 0) defaultActionButtonColor else MaterialTheme.colorScheme.primary,
                    onClick = onTrackingClicked,
                )
            }
            if (onWebViewClicked != null) {
                MangaActionButton(
                    title = stringResource(R.string.action_web_view),
                    icon = Icons.Default.Public,
                    color = defaultActionButtonColor,
                    onClick = onWebViewClicked,
                )
            }
        }

        // Expandable description-tags
        Column {
            val (expanded, onExpanded) = rememberSaveable {
                mutableStateOf(fromSource || windowWidthSizeClass != WindowWidthSizeClass.Compact)
            }
            if (!description.isNullOrBlank()) {
                val trimmedDescription = remember(description) {
                    description
                        .replace(Regex(" +\$", setOf(RegexOption.MULTILINE)), "")
                        .replace(Regex("[\\r\\n]{2,}", setOf(RegexOption.MULTILINE)), "\n")
                }
                MangaSummary(
                    expandedDescription = description,
                    shrunkDescription = trimmedDescription,
                    expanded = expanded,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = 16.dp)
                        .clickableNoIndication(
                            onLongClick = { context.copyToClipboard(description, description) },
                            onClick = { onExpanded(!expanded) },
                        ),
                )
            }
            val tags = tagsProvider()
            if (!tags.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(vertical = 12.dp)
                        .animateContentSize(),
                ) {
                    if (expanded) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            mainAxisSpacing = 4.dp,
                            crossAxisSpacing = 8.dp,
                        ) {
                            tags.forEach {
                                TagsChip(
                                    text = it,
                                    onClick = { onTagClicked(it) },
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(items = tags) {
                                TagsChip(
                                    text = it,
                                    onClick = { onTagClicked(it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaAndSourceTitlesLarge(
    appBarPadding: Dp,
    coverDataProvider: () -> Manga,
    onCoverClick: () -> Unit,
    title: String,
    context: Context,
    doSearch: (query: String, global: Boolean) -> Unit,
    author: String?,
    artist: String?,
    status: Long,
    sourceName: String,
    isStubSource: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MangaCover.Book(
            modifier = Modifier.fillMaxWidth(0.4f),
            data = coverDataProvider(),
            onClick = onCoverClick,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.clickableNoIndication(
                onLongClick = { if (title.isNotBlank()) context.copyToClipboard(title, title) },
                onClick = { if (title.isNotBlank()) doSearch(title, true) },
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = author?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown_author),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .secondaryItemAlpha()
                .padding(top = 2.dp)
                .clickableNoIndication(
                    onLongClick = {
                        if (!author.isNullOrBlank()) context.copyToClipboard(
                            author,
                            author,
                        )
                    },
                    onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
                ),
            textAlign = TextAlign.Center,
        )
        if (!artist.isNullOrBlank()) {
            Text(
                text = artist,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(top = 2.dp)
                    .clickableNoIndication(
                        onLongClick = { context.copyToClipboard(artist, artist) },
                        onClick = { doSearch(artist, true) },
                    ),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.secondaryItemAlpha(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (status) {
                    SManga.ONGOING.toLong() -> Icons.Default.Schedule
                    SManga.COMPLETED.toLong() -> Icons.Default.DoneAll
                    SManga.LICENSED.toLong() -> Icons.Default.AttachMoney
                    SManga.PUBLISHING_FINISHED.toLong() -> Icons.Default.Done
                    SManga.CANCELLED.toLong() -> Icons.Default.Close
                    SManga.ON_HIATUS.toLong() -> Icons.Default.Pause
                    else -> Icons.Default.Block
                },
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Text(
                    text = when (status) {
                        SManga.ONGOING.toLong() -> stringResource(R.string.ongoing)
                        SManga.COMPLETED.toLong() -> stringResource(R.string.completed)
                        SManga.LICENSED.toLong() -> stringResource(R.string.licensed)
                        SManga.PUBLISHING_FINISHED.toLong() -> stringResource(R.string.publishing_finished)
                        SManga.CANCELLED.toLong() -> stringResource(R.string.cancelled)
                        SManga.ON_HIATUS.toLong() -> stringResource(R.string.on_hiatus)
                        else -> stringResource(R.string.unknown)
                    },
                )
                DotSeparatorText()
                if (isStubSource) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = sourceName,
                    modifier = Modifier.clickableNoIndication { doSearch(sourceName, false) },
                )
            }
        }
    }
}

@Composable
private fun MangaAndSourceTitlesSmall(
    appBarPadding: Dp,
    coverDataProvider: () -> Manga,
    onCoverClick: () -> Unit,
    title: String,
    context: Context,
    doSearch: (query: String, global: Boolean) -> Unit,
    author: String?,
    artist: String?,
    status: Long,
    sourceName: String,
    isStubSource: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MangaCover.Book(
            modifier = Modifier.sizeIn(maxWidth = 100.dp),
            data = coverDataProvider(),
            onClick = onCoverClick,
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title.ifBlank { stringResource(R.string.unknown) },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickableNoIndication(
                    onLongClick = {
                        if (title.isNotBlank()) context.copyToClipboard(
                            title,
                            title,
                        )
                    },
                    onClick = { if (title.isNotBlank()) doSearch(title, true) },
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = author?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.unknown_author),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(top = 2.dp)
                    .clickableNoIndication(
                        onLongClick = {
                            if (!author.isNullOrBlank()) context.copyToClipboard(
                                author,
                                author,
                            )
                        },
                        onClick = { if (!author.isNullOrBlank()) doSearch(author, true) },
                    ),
            )
            if (!artist.isNullOrBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .secondaryItemAlpha()
                        .padding(top = 2.dp)
                        .clickableNoIndication(
                            onLongClick = { context.copyToClipboard(artist, artist) },
                            onClick = { doSearch(artist, true) },
                        ),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.secondaryItemAlpha(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = when (status) {
                        SManga.ONGOING.toLong() -> Icons.Default.Schedule
                        SManga.COMPLETED.toLong() -> Icons.Default.DoneAll
                        SManga.LICENSED.toLong() -> Icons.Default.AttachMoney
                        SManga.PUBLISHING_FINISHED.toLong() -> Icons.Default.Done
                        SManga.CANCELLED.toLong() -> Icons.Default.Close
                        SManga.ON_HIATUS.toLong() -> Icons.Default.Pause
                        else -> Icons.Default.Block
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                )
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Text(
                        text = when (status) {
                            SManga.ONGOING.toLong() -> stringResource(R.string.ongoing)
                            SManga.COMPLETED.toLong() -> stringResource(R.string.completed)
                            SManga.LICENSED.toLong() -> stringResource(R.string.licensed)
                            SManga.PUBLISHING_FINISHED.toLong() -> stringResource(R.string.publishing_finished)
                            SManga.CANCELLED.toLong() -> stringResource(R.string.cancelled)
                            SManga.ON_HIATUS.toLong() -> stringResource(R.string.on_hiatus)
                            else -> stringResource(R.string.unknown)
                        },
                    )
                    DotSeparatorText()
                    if (isStubSource) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = sourceName,
                        modifier = Modifier.clickableNoIndication {
                            doSearch(
                                sourceName,
                                false,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaSummary(
    expandedDescription: String,
    shrunkDescription: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    var expandedHeight by remember { mutableStateOf(0) }
    var shrunkHeight by remember { mutableStateOf(0) }
    val heightDelta = remember(expandedHeight, shrunkHeight) { expandedHeight - shrunkHeight }
    val animProgress by animateFloatAsState(if (expanded) 1f else 0f)
    val scrimHeight = with(LocalDensity.current) { remember { 24.sp.roundToPx() } }

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        val shrunkPlaceable = subcompose("description-s") {
            Text(
                text = "\n\n", // Shows at least 3 lines
                style = MaterialTheme.typography.bodyMedium,
            )
        }.map { it.measure(constraints) }
        shrunkHeight = shrunkPlaceable.maxByOrNull { it.height }?.height ?: 0

        val expandedPlaceable = subcompose("description-l") {
            Text(
                text = expandedDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
        }.map { it.measure(constraints) }
        expandedHeight = expandedPlaceable.maxByOrNull { it.height }?.height?.coerceAtLeast(shrunkHeight) ?: 0

        val actualPlaceable = subcompose("description") {
            Text(
                text = if (expanded) expandedDescription else shrunkDescription,
                maxLines = Int.MAX_VALUE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.secondaryItemAlpha(),
            )
        }.map { it.measure(constraints) }

        val scrimPlaceable = subcompose("scrim") {
            val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
            Box(
                modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
                contentAlignment = Alignment.Center,
            ) {
                val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)
                Icon(
                    painter = rememberAnimatedVectorPainter(image, !expanded),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.background(Brush.radialGradient(colors = colors.asReversed())),
                )
            }
        }.map { it.measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight)) }

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.forEach {
                it.place(0, 0)
            }

            val scrimY = currentHeight - scrimHeight
            scrimPlaceable.forEach {
                it.place(0, scrimY)
            }
        }
    }
}

@Composable
private fun TagsChip(
    text: String,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
        SuggestionChip(
            onClick = onClick,
            label = { Text(text = text, style = MaterialTheme.typography.bodySmall) },
            border = null,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                labelColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@Composable
private fun RowScope.MangaActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        onLongClick = onLongClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
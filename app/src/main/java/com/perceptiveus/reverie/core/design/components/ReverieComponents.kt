package com.perceptiveus.reverie.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.perceptiveus.reverie.core.design.ReverieArtShape
import com.perceptiveus.reverie.core.design.ReverieGlass
import com.perceptiveus.reverie.core.design.ReveriePremiumGold
import com.perceptiveus.reverie.core.design.ReveriePurple
import com.perceptiveus.reverie.core.design.ReverieTileShape
import com.perceptiveus.reverie.core.design.glassBorder
import com.perceptiveus.reverie.core.design.glowBorder
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        action?.invoke()
    }
}

/**
 * Shared glass panel used across Library, Settings, Player, etc.
 *
 * - [emphasized]: stronger fill emphasis for featured panels
 * - [highlighted]: purple-tinted fill (selected / All Songs)
 * - [glow]: purple neon glow + border — Player / hero only
 * - default: flat glass fill with no neon rim (list rows, queue, etc.)
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ReverieTileShape,
    emphasized: Boolean = false,
    highlighted: Boolean = false,
    glow: Boolean = false,
    content: @Composable () -> Unit,
) {
    val fill: Color = when {
        highlighted -> ReveriePurple.copy(alpha = 0.14f)
        else -> ReverieGlass
    }
    val surfaceModifier = if (glow) {
        modifier.glowBorder(
            color = ReveriePurple,
            shape = shape,
            glowRadius = if (emphasized) 8.dp else 4.dp,
            borderAlpha = if (emphasized) 0.55f else 0.35f,
            glowAlpha = if (emphasized) 0.28f else 0.12f,
        )
    } else {
        // No purple rim — list rows must stay flat (shadow-free, neon-free).
        modifier
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = surfaceModifier,
            shape = shape,
            color = fill,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = fill,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            content()
        }
    }
}

@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = ReveriePurple.copy(alpha = 0.2f),
    ) {
        Text(
            text = "PREMIUM",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = ReveriePurple,
        )
    }
}

@Composable
fun LockedFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .glassBorder(
                color = MaterialTheme.colorScheme.outline,
                shape = ReverieTileShape,
                borderAlpha = 0.45f,
            ),
        shape = ReverieTileShape,
        color = ReverieGlass.copy(alpha = 0.55f),
    ) {
        Box {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                PremiumBadge()
            }
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = ReveriePremiumGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
fun QuickAccessCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp)
            .glassBorder(
                color = MaterialTheme.colorScheme.primary,
                shape = ReverieTileShape,
                borderAlpha = 0.22f,
            ),
        shape = ReverieTileShape,
        color = ReverieGlass,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AlbumArtPlaceholder(
    modifier: Modifier = Modifier,
    label: String = "♪",
) {
    Box(
        modifier = modifier
            .clip(ReverieArtShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = ReverieArtShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
    }
}

/**
 * Loads cached album art when [artworkPath] is non-blank; otherwise shows a compact placeholder.
 *
 * Trusts the stored path (no sync [java.io.File] I/O on the UI thread). Coil shows nothing on
 * miss and the background underneath remains visible.
 *
 * @param listThumbnail decode smaller, skip crossfade — use for Library / Queue rows.
 */
@Composable
fun AlbumArt(
    artworkPath: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    listThumbnail: Boolean = false,
) {
    val context = LocalContext.current
    val shape = ReverieArtShape
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!artworkPath.isNullOrBlank()) {
            val request = remember(artworkPath, listThumbnail) {
                ImageRequest.Builder(context)
                    .data(artworkPath)
                    .memoryCacheKey(
                        if (listThumbnail) "thumb-$artworkPath" else artworkPath,
                    )
                    .diskCacheKey(artworkPath)
                    .crossfade(!listThumbnail)
                    .apply {
                        if (listThumbnail) {
                            // ~48dp @ xxxhdpi — keeps decode/scroll cheap for long lists.
                            size(192)
                        }
                    }
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape),
            )
        } else {
            Text(
                text = "♪",
                style = if (listThumbnail) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun RetroScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
) {
    Text(
        text = title.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
        color = color,
    )
}

/**
 * Shared page header matching Library / Quick Access screens:
 * same top padding and title size (no Material TopAppBar chrome).
 */
@Composable
fun ReverieScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(4.dp))
            }
            RetroScreenTitle(title = title)
        }
        actions?.invoke()
    }
}

/** Shared player subtitle: "Artist | Album" (either part omitted when blank). */
fun formatArtistAlbum(
    artist: String?,
    album: String?,
    emptyFallback: String = "—",
): String {
    val label = listOfNotNull(
        artist?.takeIf { it.isNotBlank() },
        album?.takeIf { it.isNotBlank() },
    ).joinToString(" | ")
    return label.ifBlank { emptyFallback }
}

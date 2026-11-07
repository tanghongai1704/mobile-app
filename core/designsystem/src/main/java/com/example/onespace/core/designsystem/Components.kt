package com.example.onespace.core.designsystem

import com.example.onespace.core.designsystem.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Content-first page scaffold: only a padded, evenly spaced list. Destination
 * identity lives in the shell top bar ("Where am I?"), so feeds start with
 * content immediately — no per-screen hero or marketing header.
 */
@Composable
fun FeaturePage(state: LazyListState = rememberLazyListState(), content: LazyListScope.() -> Unit) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Functional section label inside a feed (e.g. "Tiếp tục xem", "Liên quan"). */
@Composable
fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

/**
 * List-row card used by hubs and settings: a quiet surface with a hairline border, a soft
 * brand-tinted icon tile (no solid coral block), title/subtitle and a decorative chevron.
 */
@Composable
fun ContentCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) =
    ContentCard(title, subtitle, icon, onClick, Color.Unspecified)

/** Same card with a custom icon-tile tint; [Color.Unspecified] keeps the brand primary. */
@Composable
fun ContentCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color
) {
    val iconTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.primary else tint
    AppCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).background(
                iconTint.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Default look of cards across the app: quiet surface plus a hairline border. */
object AppCardDefaults {
    @Composable
    fun colors(): CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.oneSpaceColors.card)

    @Composable
    fun border(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/** Static card with the app look; same shape of API as Material's [Card]. Cards are square-cornered. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = AppCardDefaults.colors(),
    border: BorderStroke? = AppCardDefaults.border(),
    content: @Composable ColumnScope.() -> Unit
) = Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)

/** Clickable card with the app look — uniform 12dp corners via theme shapes. */
@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = AppCardDefaults.colors(),
    border: BorderStroke? = AppCardDefaults.border(),
    content: @Composable ColumnScope.() -> Unit
) = Card(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, colors = colors, border = border, content = content)

/** Pill-shaped search and rounded form fields share one set of colours. */
object AppFieldDefaults {
    @Composable
    fun colors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.oneSpaceColors.card,
        unfocusedContainerColor = MaterialTheme.oneSpaceColors.card,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary
    )
}

/** Quiet empty message for lists inside a screen (no call to action): tinted icon + centred text. */
@Composable
fun EmptyHint(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
        }
        Text(
            text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** Fades the trailing edge of a horizontally scrolling row so it reads as "more to the right". */
fun Modifier.fadingEdgeEnd(width: androidx.compose.ui.unit.Dp = 28.dp): Modifier = this
    .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = width.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.Transparent),
                startX = size.width - fade, endX = size.width
            ),
            topLeft = androidx.compose.ui.geometry.Offset(size.width - fade, 0f),
            size = androidx.compose.ui.geometry.Size(fade, size.height),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
        )
    }

/** Decorative media placeholder surface: brand gradient, no marketing text. */
@Composable
fun Artwork(modifier: Modifier = Modifier) {
    Box(modifier.background(
        Brush.linearGradient(listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.surfaceContainerHighest)),
        MaterialTheme.shapes.extraLarge),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.RadioButtonChecked, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp))
    }
}

/** Compact supporting/status text; use for real status only, never filler. */
@Composable
fun PrototypeNote(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

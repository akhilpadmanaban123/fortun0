package app.paisapilot.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Face2
import androidx.compose.material.icons.rounded.Face3
import androidx.compose.material.icons.rounded.Face4
import androidx.compose.material.icons.rounded.Face5
import androidx.compose.material.icons.rounded.Face6
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private data class AvatarLook(val icon: ImageVector, val color: Color)

/** The avatars a user can pick from; a profile stores the index. */
private val avatars = listOf(
    AvatarLook(Icons.Rounded.Face, ChartPalette[0]),
    AvatarLook(Icons.Rounded.Face2, ChartPalette[1]),
    AvatarLook(Icons.Rounded.Face3, ChartPalette[3]),
    AvatarLook(Icons.Rounded.Face4, ChartPalette[2]),
    AvatarLook(Icons.Rounded.Face5, ChartPalette[4]),
    AvatarLook(Icons.Rounded.Face6, ChartPalette[5]),
)

val AVATAR_COUNT: Int get() = avatars.size

@Composable
fun AvatarBadge(
    index: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val look = avatars[index.coerceIn(0, avatars.lastIndex)]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(look.color.copy(alpha = 0.16f))
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            look.icon,
            contentDescription = if (onClick != null) "Avatar ${index + 1}" else null,
            tint = look.color,
            modifier = Modifier.fillMaxSize().padding(size / 6),
        )
    }
}

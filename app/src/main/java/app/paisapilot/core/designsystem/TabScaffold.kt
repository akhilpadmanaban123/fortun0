package app.paisapilot.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Layout for a bottom-bar tab: a title bar, an optional "add" button, and the content. */
@Composable
fun TabScaffold(
    title: String,
    modifier: Modifier = Modifier,
    fabLabel: String? = null,
    onFab: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        // The bottom bar in MainScreen already accounts for the system navigation bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (fabLabel != null) {
                ExtendedFloatingActionButton(
                    onClick = onFab,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(fabLabel) },
                )
            }
        },
        content = content,
    )
}

/** Standard padding for a tab's scrolling list, leaving room for the floating button. */
fun tabListPadding(inner: PaddingValues, hasFab: Boolean = false) = PaddingValues(
    start = 16.dp,
    end = 16.dp,
    top = inner.calculateTopPadding() + 4.dp,
    bottom = inner.calculateBottomPadding() + if (hasFab) 96.dp else 24.dp,
)

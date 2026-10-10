package app.paisapilot.feature.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.paisapilot.core.designsystem.ChartPalette
import kotlinx.coroutines.launch

private data class IntroPage(val icon: ImageVector, val color: Color, val title: String, val body: String)

/** What sets the app apart, one idea per page. */
private val pages = listOf(
    IntroPage(
        Icons.Rounded.Savings, ChartPalette[0],
        "Know what is safe to spend",
        "Not just your balance. What is left after every EMI and bill due before your next payday, and what that means per day.",
    ),
    IntroPage(
        Icons.Rounded.AccountBalance, ChartPalette[1],
        "Every EMI, to the last rupee",
        "The full schedule for each loan, how much is interest, when it ends, and exactly what an extra payment saves you.",
    ),
    IntroPage(
        Icons.Rounded.NotificationsActive, ChartPalette[2],
        "Never miss a due date",
        "Reminders before EMIs, bills and card payments fall due. They work even without internet.",
    ),
    IntroPage(
        Icons.Rounded.Shield, ChartPalette[4],
        "Private, locked and scam-aware",
        "No bank logins and no SMS reading. Your data sits behind your fingerprint, with a guide to the frauds to watch for.",
    ),
    IntroPage(
        Icons.Rounded.Face, ChartPalette[3],
        "A profile that is yours",
        "Pick an avatar and tell us a little about your money. You get a money personality and advice that fits you.",
    ),
)

/** First-launch introduction. Swipe through, or skip straight to the app. */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // Kept in the layout on the last page so nothing shifts.
            TextButton(onClick = onFinished, enabled = !lastPage) { Text(if (lastPage) "" else "Skip") }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(168.dp).clip(CircleShape).background(page.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, contentDescription = null, tint = page.color, modifier = Modifier.size(84.dp))
                }
                Spacer(Modifier.height(40.dp))
                Text(page.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
            pages.indices.forEach { index ->
                val current = index == pagerState.currentPage
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (current) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }

        Button(
            onClick = {
                if (lastPage) onFinished()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (lastPage) "Get started" else "Next", style = MaterialTheme.typography.titleMedium)
        }
    }
}

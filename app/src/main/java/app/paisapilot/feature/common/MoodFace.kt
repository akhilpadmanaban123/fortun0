package app.paisapilot.feature.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.paisapilot.core.designsystem.LocalMoneyColors
import app.paisapilot.core.finance.Mood
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

/** Where mood animations live inside the app: app/src/main/assets/moods/. */
private const val MOOD_FOLDER = "moods"

/**
 * The character's face for a [mood]. Plays the Lottie animation for it when one has been
 * added (moods/great.json, happy.json, careful.json, worried.json, thinking.json, or the
 * same names ending .lottie) and otherwise draws a plain face, so every screen that uses
 * it works with any, all or none of the animation files present.
 */
@Composable
fun MoodFace(mood: Mood, modifier: Modifier = Modifier, size: Dp = 84.dp) {
    val context = LocalContext.current
    val fileName = mood.name.lowercase()
    val asset = remember(mood) {
        val available = runCatching { context.assets.list(MOOD_FOLDER) }.getOrNull().orEmpty()
        listOf("$fileName.lottie", "$fileName.json").firstOrNull { it in available }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (asset != null) {
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset("$MOOD_FOLDER/$asset"))
            LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(size))
        } else {
            val money = LocalMoneyColors.current
            Icon(
                imageVector = when (mood) {
                    Mood.THINKING -> Icons.Rounded.Psychology
                    Mood.GREAT -> Icons.Rounded.SentimentVerySatisfied
                    Mood.HAPPY -> Icons.Rounded.SentimentSatisfied
                    Mood.CAREFUL -> Icons.Rounded.SentimentNeutral
                    Mood.WORRIED -> Icons.Rounded.SentimentDissatisfied
                },
                contentDescription = null,
                tint = when (mood) {
                    Mood.THINKING -> MaterialTheme.colorScheme.secondary
                    Mood.GREAT, Mood.HAPPY -> money.positive
                    Mood.CAREFUL -> money.warning
                    Mood.WORRIED -> money.negative
                },
                modifier = Modifier.size(size * 0.76f),
            )
        }
    }
}

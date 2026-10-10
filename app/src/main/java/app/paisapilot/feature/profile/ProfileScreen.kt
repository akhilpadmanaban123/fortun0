package app.paisapilot.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.AVATAR_COUNT
import app.paisapilot.core.designsystem.AmountField
import app.paisapilot.core.designsystem.AvatarBadge
import app.paisapilot.core.designsystem.DayOfMonthField
import app.paisapilot.core.designsystem.FormScaffold
import app.paisapilot.core.designsystem.TextInput

/** What each step of the 1-5 money-confidence scale means. */
private val confidenceLabels = listOf("Stressed", "Uneasy", "Getting by", "Comfortable", "In control")

/**
 * The user's money profile: who they are, what comes in, what goes to EMIs and how they
 * feel about it. Shown once as onboarding after sign-up, and from Settings to edit.
 * Only the name is required.
 */
@Composable
fun ProfileScreen(
    onboarding: Boolean,
    suggestedName: String = "",
    onDone: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(suggestedName, form.loaded) {
        if (form.loaded && suggestedName.isNotBlank()) viewModel.suggestName(suggestedName)
    }

    FormScaffold(
        title = if (onboarding) "Your money profile" else "Edit profile",
        onBack = onDone,
        onSave = { viewModel.save(onDone) },
        saveLabel = if (onboarding) "Create my profile" else "Save",
        showBack = !onboarding,
    ) {
        if (onboarding) {
            Text(
                "A few details so the app fits you. Only your name is needed; fill in the rest now or later.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Label("Pick your avatar")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(AVATAR_COUNT) { index ->
                AvatarBadge(
                    index = index,
                    size = 50.dp,
                    selected = form.avatar == index,
                    onClick = { viewModel.update { copy(avatar = index) } },
                )
            }
        }

        TextInput(form.name, { viewModel.update { copy(name = it) } }, "Your name", error = form.nameError)
        TextInput(
            form.age, { viewModel.update { copy(age = it.filter(Char::isDigit)) } }, "Age",
            keyboardType = KeyboardType.Number, maxLength = 3,
            hint = "Shapes the advice you see. Never shown to anyone.",
        )

        Label("Money coming in")
        AmountField(
            form.income, { viewModel.update { copy(income = it) } }, "Monthly salary (take-home)",
            hint = "What is credited after tax and deductions.",
        )
        DayOfMonthField(
            label = "Payday",
            day = form.payday,
            onDayChange = { viewModel.update { copy(payday = it) } },
            hint = "The day your salary usually arrives.",
        )
        AmountField(
            form.extraIncome, { viewModel.update { copy(extraIncome = it) } }, "Other monthly income",
            hint = "Rent, freelance work, interest. Leave blank if none.",
        )

        Label("Money going out")
        AmountField(
            form.emiEstimate, { viewModel.update { copy(emiEstimate = it) } }, "Total EMIs you pay each month",
            hint = "A rough figure is fine. Once you add your loans, the exact amount is used instead.",
        )

        Label("How do you feel about your money right now?")
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                confidenceLabels.forEachIndexed { index, _ ->
                    val level = index + 1
                    FilterChip(
                        selected = form.confidence == level,
                        // Tapping the selected level again clears the answer.
                        onClick = { viewModel.update { copy(confidence = if (confidence == level) 0 else level) } },
                        label = { Text("$level") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (form.confidence in 1..confidenceLabels.size) "${form.confidence} of 5: ${confidenceLabels[form.confidence - 1]}"
                else "1 is stressed, 5 is fully in control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

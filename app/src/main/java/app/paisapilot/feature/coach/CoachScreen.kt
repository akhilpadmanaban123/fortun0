package app.paisapilot.feature.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.paisapilot.core.designsystem.AvatarBadge

/** Shown before the first message, so nobody has to think of what to ask. */
private val starters = listOf(
    "Can I afford a ₹20,000 purchase this month?",
    "Which loan should I pay off first?",
    "How can I improve my financial health score?",
    "Where am I overspending?",
)

@Composable
fun CoachScreen(
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: CoachViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarBadge(state.avatar, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Money coach", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (state.gate == CoachGate.READY) {
                QuestionBar(state.input, state.sending, viewModel::onInput, onSend = { viewModel.send() })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.gate) {
                CoachGate.LOADING -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                CoachGate.NOT_PREMIUM -> Notice(
                    "The money coach is a Premium feature",
                    "Your avatar becomes a coach that knows your loans, bills and spending, and answers questions about your own money.",
                    "See Premium", onOpenPremium,
                )
                CoachGate.NEEDS_ACCOUNT -> Notice(
                    "Sign in to use the coach",
                    "The coach needs a verified account so that only you can ask about your money. Create one from Settings; your existing data is kept.",
                )
                CoachGate.NOT_CONFIGURED -> Notice(
                    "The coach is not set up yet",
                    "This version of the app has no coach service address. See the README for how to deploy it and add coach.url.",
                )
                CoachGate.NEEDS_CONSENT -> Notice(
                    "Before you start",
                    "To answer, the coach sends a summary of your finances to an AI service: your first name and age, income, balances, " +
                        "loans, bills, card usage and spending totals.\n\nIt never sends account or card numbers, your email, or individual " +
                        "transactions. Conversations are not saved.\n\nReplies are general guidance, not professional financial advice.",
                    "I agree, continue", viewModel::giveConsent,
                )
                CoachGate.READY -> Conversation(state, onStarter = viewModel::send, onRetry = viewModel::retry)
            }
        }
    }
}

@Composable
private fun Conversation(state: CoachUiState, onStarter: (String) -> Unit, onRetry: () -> Unit) {
    val listState = rememberLazyListState()
    // Keep the newest message in view as the conversation grows.
    LaunchedEffect(state.messages.size, state.sending, state.error) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.messages.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarBadge(state.avatar, size = 88.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (state.firstName.isBlank()) "Hi, I'm your money coach" else "Hi ${state.firstName}, I'm your money coach",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "I can see your loans, bills, balances and spending. Ask me anything about them.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        starters.forEach { SuggestionChip(onClick = { onStarter(it) }, label = { Text(it) }) }
                    }
                }
            }
        }
        items(state.messages) { Bubble(it) }
        if (state.sending) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Thinking…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        state.error?.let { error ->
            item {
                Column {
                    Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
            }
        }
        if (state.messages.isNotEmpty()) {
            item {
                Text(
                    "General guidance based on what you have entered. Not professional financial advice.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Bubble(message: CoachMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Text(
            message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (message.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (message.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun QuestionBar(input: String, sending: Boolean, onInput: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInput,
            placeholder = { Text("Ask about your money") },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        IconButton(onClick = onSend, enabled = !sending && input.isNotBlank()) {
            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
        }
    }
}

@Composable
private fun Notice(title: String, body: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

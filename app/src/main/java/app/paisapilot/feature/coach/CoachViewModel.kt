package app.paisapilot.feature.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.finance.HealthScore
import app.paisapilot.core.finance.MoneyPersonas
import app.paisapilot.core.premium.Entitlements
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.remote.CoachApi
import app.paisapilot.data.remote.CoachException
import app.paisapilot.data.remote.CoachFailure
import app.paisapilot.data.remote.CoachTurn
import app.paisapilot.data.repository.AuthRepository
import app.paisapilot.data.repository.AuthState
import app.paisapilot.domain.CoachContext
import app.paisapilot.domain.ObserveOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What stands between the user and a conversation, checked in this order. */
enum class CoachGate { LOADING, NOT_PREMIUM, NEEDS_ACCOUNT, NOT_CONFIGURED, NEEDS_CONSENT, READY }

data class CoachMessage(val fromUser: Boolean, val text: String)

data class CoachUiState(
    val gate: CoachGate = CoachGate.LOADING,
    val avatar: Int = 0,
    val firstName: String = "",
    val messages: List<CoachMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val api: CoachApi,
    private val observeOverview: ObserveOverview,
    private val profileStore: ProfileStore,
    entitlements: Entitlements,
    auth: AuthRepository,
) : ViewModel() {

    /** The conversation lives only while this screen is open; nothing is stored on the phone or server. */
    private val chat = MutableStateFlow(CoachUiState())

    val state: StateFlow<CoachUiState> =
        combine(chat, entitlements.isPremium, auth.state, profileStore.profile) { chat, premium, authState, profile ->
            chat.copy(
                gate = when {
                    !premium -> CoachGate.NOT_PREMIUM
                    (authState as? AuthState.SignedIn)?.emailVerified != true -> CoachGate.NEEDS_ACCOUNT
                    !api.isConfigured -> CoachGate.NOT_CONFIGURED
                    !profile.coachConsent -> CoachGate.NEEDS_CONSENT
                    else -> CoachGate.READY
                },
                avatar = profile.avatar,
                firstName = profile.firstName,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachUiState())

    fun onInput(value: String) = chat.update { it.copy(input = value.take(MAX_QUESTION_LENGTH)) }

    fun giveConsent() {
        viewModelScope.launch { profileStore.setCoachConsent(true) }
    }

    fun send(text: String = chat.value.input) {
        val question = text.trim()
        if (question.isEmpty() || chat.value.sending) return
        chat.update { it.copy(messages = it.messages + CoachMessage(fromUser = true, text = question), input = "", sending = true, error = null) }
        ask()
    }

    /** Asks again after a failure; the unanswered question is still the last message. */
    fun retry() {
        if (chat.value.sending || chat.value.messages.lastOrNull()?.fromUser != true) return
        chat.update { it.copy(sending = true, error = null) }
        ask()
    }

    private fun ask() {
        viewModelScope.launch {
            val overview = observeOverview().first()
            val health = HealthScore.evaluate(overview.snapshot, overview.profile.totalIncomeMinor, overview.bills)
            val emi = overview.snapshot.monthlyEmiMinor.takeIf { it > 0 } ?: overview.profile.emiEstimateMinor
            val persona = MoneyPersonas.of(overview.profile.totalIncomeMinor, emi, overview.profile.age, overview.profile.confidence)

            val turns = chat.value.messages.takeLast(MAX_TURNS_SENT).map {
                CoachTurn(if (it.fromUser) CoachTurn.USER else CoachTurn.COACH, it.text)
            }
            val result = api.ask(CoachContext.build(overview, health, persona), turns)
            chat.update { current ->
                result.fold(
                    onSuccess = { reply -> current.copy(messages = current.messages + CoachMessage(false, reply), sending = false) },
                    onFailure = { current.copy(sending = false, error = messageFor((it as? CoachException)?.failure)) },
                )
            }
        }
    }

    private fun messageFor(failure: CoachFailure?): String = when (failure) {
        CoachFailure.OFFLINE -> "Could not reach the coach. Check your internet connection and try again."
        CoachFailure.DAILY_LIMIT -> "You have used today's questions. The coach will be back tomorrow."
        CoachFailure.NOT_SIGNED_IN -> "Please sign out and sign in again, then try once more."
        CoachFailure.NOT_CONFIGURED -> "The coach is not set up in this version of the app."
        CoachFailure.UNAVAILABLE, null -> "The coach is busy right now. Please try again in a moment."
    }

    private companion object {
        const val MAX_QUESTION_LENGTH = 500
        /** Enough history for follow-up questions without sending a long conversation every time. */
        const val MAX_TURNS_SENT = 12
    }
}

package app.paisapilot.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.paisapilot.core.util.Money
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileForm(
    val name: String = "",
    val avatar: Int = 0,
    val age: String = "",
    val income: String = "",
    val payday: Int = 0,
    val extraIncome: String = "",
    val emiEstimate: String = "",
    val confidence: Int = 0,
    val nameError: String? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileStore: ProfileStore,
    private val sync: SyncScheduler,
) : ViewModel() {

    private val _form = MutableStateFlow(ProfileForm())
    val form: StateFlow<ProfileForm> = _form

    init {
        viewModelScope.launch {
            val p = profileStore.current()
            _form.update {
                it.copy(
                    name = p.name,
                    avatar = p.avatar,
                    age = if (p.age > 0) p.age.toString() else "",
                    income = Money.toInput(p.monthlyIncomeMinor),
                    payday = p.payday,
                    extraIncome = Money.toInput(p.extraIncomeMinor),
                    emiEstimate = Money.toInput(p.emiEstimateMinor),
                    confidence = p.confidence,
                    loaded = true,
                )
            }
        }
    }

    /** Pre-fills the name from the sign-up form, unless the user has already typed one. */
    fun suggestName(name: String) = _form.update { if (it.name.isBlank()) it.copy(name = name) else it }

    fun update(change: ProfileForm.() -> ProfileForm) = _form.update { it.change().copy(nameError = null) }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        if (f.name.isBlank()) {
            _form.update { it.copy(nameError = "Please tell us what to call you.") }
            return
        }
        viewModelScope.launch {
            profileStore.saveProfile(
                profileStore.current().copy(
                    name = f.name,
                    avatar = f.avatar,
                    age = f.age.toIntOrNull()?.takeIf { it in MIN_AGE..MAX_AGE } ?: 0,
                    monthlyIncomeMinor = Money.parse(f.income) ?: 0,
                    payday = f.payday,
                    extraIncomeMinor = Money.parse(f.extraIncome) ?: 0,
                    emiEstimateMinor = Money.parse(f.emiEstimate) ?: 0,
                    confidence = f.confidence,
                )
            )
            sync.requestSync()
            onSaved()
        }
    }

    private companion object {
        const val MIN_AGE = 13
        const val MAX_AGE = 110
    }
}

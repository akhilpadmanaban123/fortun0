package app.paisapilot.core.premium

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Answers "can this user use this feature?". Billing is not wired up yet, so nobody
 * is premium; when a payment provider is added it only needs to flip [isPremium].
 */
@Singleton
class Entitlements @Inject constructor() {
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    fun isUnlocked(feature: Feature): Boolean = feature.tier == Tier.FREE || _isPremium.value
}

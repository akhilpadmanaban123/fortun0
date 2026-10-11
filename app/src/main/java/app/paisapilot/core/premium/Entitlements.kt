package app.paisapilot.core.premium

import app.paisapilot.BuildConfig
import app.paisapilot.data.prefs.ProfileStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Answers "can this user use this feature?".
 *
 * Play Billing is not connected yet. Until it is, premium can only be switched on with the
 * test unlock on the Premium screen, and only in debug builds: a release build always
 * reports "not premium", so nothing here can hand out paid features by accident.
 * When billing is added, this class is the one place that changes.
 */
@Singleton
class Entitlements @Inject constructor(private val profileStore: ProfileStore) {

    /** True in builds where the test unlock is offered. */
    val testUnlockAvailable: Boolean get() = BuildConfig.DEBUG

    val isPremium: Flow<Boolean> = profileStore.profile.map { testUnlockAvailable && it.premiumTestUnlock }

    suspend fun isUnlocked(feature: Feature): Boolean = feature.tier == Tier.FREE || isPremium.first()

    suspend fun setTestUnlock(enabled: Boolean) {
        if (testUnlockAvailable) profileStore.setPremiumTestUnlock(enabled)
    }
}

package app.paisapilot.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class UserProfile(
    val name: String = "",
    /** Index into the avatar set in core/designsystem/Avatars.kt. */
    val avatar: Int = 0,
    /** 0 when not given. */
    val age: Int = 0,
    val monthlyIncomeMinor: Long = 0,
    /** Day of month salary is credited; 0 when not set. */
    val payday: Int = 0,
    /** Regular income besides salary: rent, freelance, interest. */
    val extraIncomeMinor: Long = 0,
    /** The user's own estimate of their monthly EMIs, used until they add their loans. */
    val emiEstimateMinor: Long = 0,
    /** How in control of money the user feels, 1 (stressed) to 5 (in control); 0 when not set. */
    val confidence: Int = 0,
    val profileUpdatedAt: Long = 0,
    val onboarded: Boolean = false,
    /** The first-launch introduction has been shown on this phone. */
    val introSeen: Boolean = false,
    /** The "why we send notifications" explanation has been shown and answered. */
    val notificationPromptSeen: Boolean = false,
    /** The user chose to use the app without a cloud account. */
    val guest: Boolean = false,
    val biometricLock: Boolean = false,
    val reminders: Boolean = true,
    val reminderDaysBefore: Int = 3,
    /** Tracking reminders and money quotes: on/off, a nudge every N days, at this hour (0-23). */
    val nudges: Boolean = true,
    val nudgeEveryDays: Int = 1,
    val nudgeHour: Int = 20,
    /** Debug-only switch that turns premium on for testing; see core/premium/Entitlements. */
    val premiumTestUnlock: Boolean = false,
    /** The user agreed to a summary of their finances being sent to the AI coach. */
    val coachConsent: Boolean = false,
) {
    val paydayOrDefault: Int get() = if (payday in 1..31) payday else 1
    val firstName: String get() = name.trim().substringBefore(' ')
    val totalIncomeMinor: Long get() = monthlyIncomeMinor + extraIncomeMinor
}

/** Profile and settings, kept on the device in DataStore. */
@Singleton
class ProfileStore @Inject constructor(private val store: DataStore<Preferences>) {

    val profile: Flow<UserProfile> = store.data.map { p ->
        UserProfile(
            name = p[NAME] ?: "",
            avatar = p[AVATAR] ?: 0,
            age = p[AGE] ?: 0,
            monthlyIncomeMinor = p[INCOME] ?: 0,
            payday = p[PAYDAY] ?: 0,
            extraIncomeMinor = p[EXTRA_INCOME] ?: 0,
            emiEstimateMinor = p[EMI_ESTIMATE] ?: 0,
            confidence = p[CONFIDENCE] ?: 0,
            profileUpdatedAt = p[PROFILE_UPDATED_AT] ?: 0,
            onboarded = p[ONBOARDED] ?: false,
            introSeen = p[INTRO_SEEN] ?: false,
            notificationPromptSeen = p[NOTIFICATION_PROMPT_SEEN] ?: false,
            guest = p[GUEST] ?: false,
            biometricLock = p[BIOMETRIC] ?: false,
            reminders = p[REMINDERS] ?: true,
            reminderDaysBefore = p[REMINDER_DAYS] ?: 3,
            nudges = p[NUDGES] ?: true,
            nudgeEveryDays = p[NUDGE_EVERY_DAYS] ?: 1,
            nudgeHour = p[NUDGE_HOUR] ?: 20,
            premiumTestUnlock = p[PREMIUM_TEST_UNLOCK] ?: false,
            coachConsent = p[COACH_CONSENT] ?: false,
        )
    }

    suspend fun current(): UserProfile = profile.first()

    /**
     * Saves the personal details in [profile] (settings such as the app lock are not touched)
     * and marks onboarding as done. [updatedAt] is supplied when applying a profile restored
     * from the cloud.
     */
    suspend fun saveProfile(profile: UserProfile, updatedAt: Long = System.currentTimeMillis()) {
        store.edit {
            it[NAME] = profile.name.trim()
            it[AVATAR] = profile.avatar
            it[AGE] = profile.age
            it[INCOME] = profile.monthlyIncomeMinor
            it[PAYDAY] = profile.payday
            it[EXTRA_INCOME] = profile.extraIncomeMinor
            it[EMI_ESTIMATE] = profile.emiEstimateMinor
            it[CONFIDENCE] = profile.confidence
            it[PROFILE_UPDATED_AT] = updatedAt
            it[ONBOARDED] = true
        }
    }

    /** Uid of the account the data on this phone belongs to; null for local-only data. */
    suspend fun ownerUid(): String? = store.data.first()[OWNER_UID]

    suspend fun setOwnerUid(uid: String) = store.edit { it[OWNER_UID] = uid }

    suspend fun setIntroSeen() = store.edit { it[INTRO_SEEN] = true }

    suspend fun setNotificationPromptSeen() = store.edit { it[NOTIFICATION_PROMPT_SEEN] = true }

    suspend fun setGuest(guest: Boolean) = store.edit { it[GUEST] = guest }

    suspend fun setBiometricLock(enabled: Boolean) = store.edit { it[BIOMETRIC] = enabled }

    suspend fun setReminders(enabled: Boolean) = store.edit { it[REMINDERS] = enabled }

    suspend fun setReminderDaysBefore(days: Int) = store.edit { it[REMINDER_DAYS] = days }

    suspend fun setPremiumTestUnlock(enabled: Boolean) = store.edit { it[PREMIUM_TEST_UNLOCK] = enabled }

    suspend fun setCoachConsent(agreed: Boolean) = store.edit { it[COACH_CONSENT] = agreed }

    suspend fun setNudges(enabled: Boolean) = store.edit { it[NUDGES] = enabled }

    suspend fun setNudgeEveryDays(days: Int) = store.edit { it[NUDGE_EVERY_DAYS] = days }

    suspend fun setNudgeHour(hour: Int) = store.edit { it[NUDGE_HOUR] = hour }

    /** Returns how many nudges have been sent so far, then counts this one. */
    suspend fun nextNudgeSequence(): Long {
        var sequence = 0L
        store.edit {
            sequence = it[NUDGE_SEQUENCE] ?: 0
            it[NUDGE_SEQUENCE] = sequence + 1
        }
        return sequence
    }

    suspend fun syncCursor(uid: String, table: String): Long =
        store.data.first()[longPreferencesKey("cursor_${uid}_$table")] ?: 0

    suspend fun setSyncCursor(uid: String, table: String, value: Long) =
        store.edit { it[longPreferencesKey("cursor_${uid}_$table")] = value }

    val lastSyncAt: Flow<Long> = store.data.map { it[LAST_SYNC] ?: 0 }

    suspend fun setLastSyncAt(value: Long) = store.edit { it[LAST_SYNC] = value }

    /** Removes the user's profile and settings. The introduction is not shown again on this phone. */
    suspend fun clear() = store.edit {
        val introSeen = it[INTRO_SEEN] ?: false
        it.clear()
        it[INTRO_SEEN] = introSeen
    }

    private companion object {
        val OWNER_UID = stringPreferencesKey("owner_uid")
        val NAME = stringPreferencesKey("name")
        val AVATAR = intPreferencesKey("avatar")
        val AGE = intPreferencesKey("age")
        val INCOME = longPreferencesKey("monthly_income_minor")
        val PAYDAY = intPreferencesKey("payday")
        val EXTRA_INCOME = longPreferencesKey("extra_income_minor")
        val EMI_ESTIMATE = longPreferencesKey("emi_estimate_minor")
        val CONFIDENCE = intPreferencesKey("confidence")
        val PROFILE_UPDATED_AT = longPreferencesKey("profile_updated_at")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val INTRO_SEEN = booleanPreferencesKey("intro_seen")
        val NOTIFICATION_PROMPT_SEEN = booleanPreferencesKey("notification_prompt_seen")
        val GUEST = booleanPreferencesKey("guest")
        val BIOMETRIC = booleanPreferencesKey("biometric_lock")
        val REMINDERS = booleanPreferencesKey("reminders")
        val REMINDER_DAYS = intPreferencesKey("reminder_days_before")
        val PREMIUM_TEST_UNLOCK = booleanPreferencesKey("premium_test_unlock")
        val COACH_CONSENT = booleanPreferencesKey("coach_consent")
        val NUDGES = booleanPreferencesKey("nudges")
        val NUDGE_EVERY_DAYS = intPreferencesKey("nudge_every_days")
        val NUDGE_HOUR = intPreferencesKey("nudge_hour")
        val NUDGE_SEQUENCE = longPreferencesKey("nudge_sequence")
        val LAST_SYNC = longPreferencesKey("last_sync_at")
    }
}

package app.paisapilot.data.repository

import app.paisapilot.data.local.AppDatabase
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.sync.SyncManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Operations that span the account, local data and settings. */
@Singleton
class SessionRepository @Inject constructor(
    private val auth: AuthRepository,
    private val profileStore: ProfileStore,
    private val syncManager: SyncManager,
    private val db: AppDatabase,
) {
    /**
     * Uploads anything pending, then removes all data from this device and signs out,
     * so the next person to sign in here starts clean. Returns false, changing nothing,
     * when there are changes that could not be uploaded (e.g. offline).
     */
    suspend fun signOut(force: Boolean = false): Boolean {
        val uploaded = runCatching { syncManager.sync() }.isSuccess
        if (!uploaded && !force && auth.verifiedUid != null) return false
        wipeLocalData()
        auth.signOut()
        return true
    }

    /**
     * Permanently deletes the account: cloud data first, then the sign-in, then this phone's copy.
     * The order matters. If any step fails nothing after it runs, so a failed attempt never
     * leaves data behind with no account to reach it. [password] re-confirms who is asking.
     */
    suspend fun deleteAccount(password: String): Result<Unit> {
        auth.reauthenticate(password).onFailure { return Result.failure(it) }
        runCatching { syncManager.deleteCloudData() }.onFailure {
            return Result.failure(IllegalStateException("Could not reach the server. Check your internet connection and try again.", it))
        }
        auth.deleteUser().onFailure { return Result.failure(it) }
        wipeLocalData()
        return Result.success(Unit)
    }

    /** Local-only users have no cloud copy; this permanently erases their data. */
    suspend fun eraseEverything() {
        wipeLocalData()
        auth.signOut()
    }

    private suspend fun wipeLocalData() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        profileStore.clear()
    }
}

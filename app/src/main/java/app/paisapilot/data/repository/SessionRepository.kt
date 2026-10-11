package app.paisapilot.data.repository

import app.paisapilot.data.local.AppDatabase
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.sync.SyncManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
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
    suspend fun signOut(force: Boolean = false): Boolean = uninterrupted {
        val uploaded = runCatching { syncManager.sync() }.isSuccess
        if (!uploaded && !force && auth.verifiedUid != null) return@uninterrupted false
        wipeLocalData()
        auth.signOut()
        true
    }

    /**
     * Permanently deletes the account: cloud data first, then this phone's copy, then the sign-in.
     * If the cloud step fails nothing after it runs, so a failed attempt never leaves data
     * behind with no account to reach it. [password] re-confirms who is asking.
     *
     * The phone is wiped before the sign-in is deleted: deleting the sign-in sends the app back
     * to the sign-in screen, and nothing of this user's must still be on the phone by then.
     */
    suspend fun deleteAccount(password: String): Result<Unit> = uninterrupted {
        auth.reauthenticate(password).onFailure { return@uninterrupted Result.failure(it) }
        runCatching { syncManager.deleteCloudData() }.onFailure {
            return@uninterrupted Result.failure(
                IllegalStateException("Could not reach the server. Check your internet connection and try again.", it)
            )
        }
        wipeLocalData()
        auth.deleteUser().onFailure { return@uninterrupted Result.failure(it) }
        Result.success(Unit)
    }

    /** Local-only users have no cloud copy; this permanently erases their data. */
    suspend fun eraseEverything() = uninterrupted {
        wipeLocalData()
        auth.signOut()
    }

    /**
     * Runs [block] to the end even if the screen that started it goes away. Signing out
     * closes the screen that asked for it, which would otherwise cancel the work half-way
     * and leave one user's data on the phone for the next person to sign in.
     */
    private suspend fun <T> uninterrupted(block: suspend () -> T): T = withContext(NonCancellable) { block() }

    private suspend fun wipeLocalData() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        profileStore.clear()
    }
}

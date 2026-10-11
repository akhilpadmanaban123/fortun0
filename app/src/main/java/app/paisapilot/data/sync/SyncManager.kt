package app.paisapilot.data.sync

import app.paisapilot.data.local.AppDatabase
import app.paisapilot.data.local.dao.SyncDao
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.local.entity.DailyCheckin
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.local.entity.LoanPrepayment
import app.paisapilot.data.local.entity.Syncable
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.remote.FirebaseProvider
import app.paisapilot.data.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.KSerializer

/**
 * Two-way sync between Room and the signed-in user's private space in Firestore:
 *
 *   users/{uid}                 profile
 *   users/{uid}/{table}/{id}    one document per row
 *
 * Room stays the source of truth, so the app works fully offline. Each run uploads rows
 * flagged dirty, then downloads documents written since the last run. Conflicts resolve
 * by `updatedAt` (last write wins); deletes travel as tombstones.
 */
@Singleton
class SyncManager @Inject constructor(
    private val firebase: FirebaseProvider,
    private val auth: AuthRepository,
    private val profileStore: ProfileStore,
    private val db: AppDatabase,
) {
    private class Table<T : Syncable>(val name: String, val dao: SyncDao<T>, val serializer: KSerializer<T>)

    private val tables: List<Table<*>> = listOf(
        Table("accounts", db.accountDao(), Account.serializer()),
        Table("credit_cards", db.creditCardDao(), CreditCard.serializer()),
        Table("loans", db.loanDao(), Loan.serializer()),
        Table("loan_prepayments", db.loanPrepaymentDao(), LoanPrepayment.serializer()),
        Table("bills", db.billDao(), Bill.serializer()),
        Table("transactions", db.txnDao(), Txn.serializer()),
        Table("daily_checkins", db.dailyCheckinDao(), DailyCheckin.serializer()),
    )

    private val mutex = Mutex()

    val canSync: Boolean get() = firebase.isConfigured && auth.verifiedUid != null

    /** Returns false when sync is not possible (local mode, signed out, or email unverified). */
    suspend fun sync(): Boolean {
        val uid = auth.verifiedUid ?: return false
        val userDoc = firebase.firestore?.collection("users")?.document(uid) ?: return false
        mutex.withLock {
            claimLocalData(uid)
            syncProfile(userDoc)
            tables.forEach { syncTable(uid, userDoc, it) }
            registerDevice(userDoc)
            profileStore.setLastSyncAt(System.currentTimeMillis())
        }
        return true
    }

    /**
     * Makes sure the data on this phone belongs to [uid] before anything is uploaded.
     * Data left behind by a different account is erased rather than merged into this one.
     * Data created without an account has no owner yet and is adopted, which is how a
     * local-only user keeps their data when they create an account.
     */
    private suspend fun claimLocalData(uid: String) {
        val owner = profileStore.ownerUid()
        if (owner == uid) return
        if (owner != null) {
            withContext(Dispatchers.IO) { db.clearAllTables() }
            profileStore.clear()
        }
        profileStore.setOwnerUid(uid)
    }

    private suspend fun syncProfile(userDoc: DocumentReference) {
        val local = profileStore.current()
        val remote = userDoc.get().await()
        val remoteUpdatedAt = remote.getLong(PROFILE_UPDATED_AT) ?: 0
        when {
            remoteUpdatedAt > local.profileUpdatedAt -> profileStore.saveProfile(
                local.copy(
                    name = remote.getString("name").orEmpty(),
                    avatar = (remote.getLong("avatar") ?: 0).toInt(),
                    age = (remote.getLong("age") ?: 0).toInt(),
                    monthlyIncomeMinor = remote.getLong("monthlyIncomeMinor") ?: 0,
                    payday = (remote.getLong("payday") ?: 0).toInt(),
                    extraIncomeMinor = remote.getLong("extraIncomeMinor") ?: 0,
                    emiEstimateMinor = remote.getLong("emiEstimateMinor") ?: 0,
                    confidence = (remote.getLong("confidence") ?: 0).toInt(),
                ),
                updatedAt = remoteUpdatedAt,
            )
            local.onboarded && local.profileUpdatedAt > remoteUpdatedAt -> userDoc.set(
                mapOf(
                    "name" to local.name,
                    "avatar" to local.avatar,
                    "age" to local.age,
                    "monthlyIncomeMinor" to local.monthlyIncomeMinor,
                    "payday" to local.payday,
                    "extraIncomeMinor" to local.extraIncomeMinor,
                    "emiEstimateMinor" to local.emiEstimateMinor,
                    "confidence" to local.confidence,
                    PROFILE_UPDATED_AT to local.profileUpdatedAt,
                ),
                SetOptions.merge(),
            ).await()
        }
    }

    private suspend fun <T : Syncable> syncTable(uid: String, userDoc: DocumentReference, table: Table<T>) {
        val collection = userDoc.collection(table.name)

        // Upload local changes.
        for (row in table.dao.dirtyRows()) {
            val data = FirestoreCodec.toMap(table.serializer, row) + (SYNCED_AT to FieldValue.serverTimestamp())
            collection.document(row.id).set(data).await()
            table.dao.markClean(row.id, row.updatedAt)
        }

        // Download everything the server received since our cursor (server time, so device
        // clocks do not matter). Our own uploads come back once and are skipped as not-newer.
        val cursor = profileStore.syncCursor(uid, table.name)
        val snapshot = collection.whereGreaterThan(SYNCED_AT, Timestamp(Date(cursor))).get().await()
        var newCursor = cursor
        val incoming = ArrayList<T>()
        for (doc in snapshot.documents) {
            doc.getTimestamp(SYNCED_AT)?.let { newCursor = maxOf(newCursor, it.toDate().time) }
            val remote = FirestoreCodec.fromMap(table.serializer, doc.data ?: continue) ?: continue
            val local = table.dao.byId(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) incoming += remote
        }
        if (incoming.isNotEmpty()) table.dao.upsertAll(incoming)
        if (newCursor != cursor) profileStore.setSyncCursor(uid, table.name, newCursor)
    }

    /**
     * Permanently deletes everything stored in the cloud for the signed-in user: every row
     * in every table, their registered devices and their profile. Throws if it cannot finish,
     * so the caller never deletes the sign-in while data is left behind.
     */
    suspend fun deleteCloudData() {
        val uid = auth.verifiedUid ?: return // unverified accounts were never allowed to store anything
        val firestore = firebase.firestore ?: return
        val userDoc = firestore.collection("users").document(uid)
        mutex.withLock {
            for (name in tables.map { it.name } + DEVICES) {
                val collection = userDoc.collection(name)
                while (true) {
                    val page = collection.limit(DELETE_PAGE).get().await()
                    if (page.isEmpty) break
                    val batch = firestore.batch()
                    page.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            userDoc.delete().await()
        }
    }

    /** Saves this device's push token so reminders can also be sent from a server later. */
    private suspend fun registerDevice(userDoc: DocumentReference) {
        runCatching {
            val token = firebase.messaging?.token?.await() ?: return
            userDoc.collection(DEVICES).document(token)
                .set(mapOf("platform" to "android", SYNCED_AT to FieldValue.serverTimestamp()))
                .await()
        }
    }

    private companion object {
        const val SYNCED_AT = "syncedAt"
        const val DEVICES = "devices"

        /** Firestore allows at most 500 writes in one batch. */
        const val DELETE_PAGE = 400L
        const val PROFILE_UPDATED_AT = "profileUpdatedAt"
    }
}

package app.paisapilot.data.repository

import app.paisapilot.data.local.dao.AccountDao
import app.paisapilot.data.local.dao.CreditCardDao
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.local.entity.now
import app.paisapilot.data.sync.SyncScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Where the money is: accounts (cash, bank, wallets, deposits) and credit cards. */
@Singleton
class WalletRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val cardDao: CreditCardDao,
    private val sync: SyncScheduler,
) {
    val accounts: Flow<List<Account>> = accountDao.observeAll()
    val cards: Flow<List<CreditCard>> = cardDao.observeAll()

    suspend fun account(id: String): Account? = accountDao.byId(id)?.takeUnless { it.deleted }

    suspend fun card(id: String): CreditCard? = cardDao.byId(id)?.takeUnless { it.deleted }

    suspend fun save(account: Account) {
        accountDao.upsert(account.copy(updatedAt = now(), dirty = true))
        sync.requestSync()
    }

    suspend fun save(card: CreditCard) {
        cardDao.upsert(card.copy(updatedAt = now(), dirty = true))
        sync.requestSync()
    }

    suspend fun deleteAccount(id: String) {
        accountDao.softDelete(id, now())
        sync.requestSync()
    }

    suspend fun deleteCard(id: String) {
        cardDao.softDelete(id, now())
        sync.requestSync()
    }
}

package app.paisapilot.domain

import app.paisapilot.core.finance.ForecastEngine
import app.paisapilot.core.finance.MoneySnapshot
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.local.entity.LoanPrepayment
import app.paisapilot.data.local.entity.Txn
import app.paisapilot.data.prefs.ProfileStore
import app.paisapilot.data.prefs.UserProfile
import app.paisapilot.data.repository.BillRepository
import app.paisapilot.data.repository.LoanRepository
import app.paisapilot.data.repository.TxnRepository
import app.paisapilot.data.repository.WalletRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the user tracks, plus the figures calculated from it. */
data class Overview(
    val profile: UserProfile,
    val accounts: List<Account>,
    val cards: List<CreditCard>,
    val loans: List<Loan>,
    val bills: List<Bill>,
    val prepayments: List<LoanPrepayment>,
    val monthTxns: List<Txn>,
    val snapshot: MoneySnapshot,
)

/** Live view of the user's whole financial picture; re-emits whenever any of it changes. */
class ObserveOverview @Inject constructor(
    private val profileStore: ProfileStore,
    private val wallet: WalletRepository,
    private val loans: LoanRepository,
    private val bills: BillRepository,
    private val txns: TxnRepository,
) {
    operator fun invoke(): Flow<Overview> {
        val month = YearMonth.now()
        val holdings = combine(
            profileStore.profile, wallet.accounts, wallet.cards, loans.loans, bills.bills,
        ) { profile, accounts, cards, loans, bills ->
            Overview(profile, accounts, cards, loans, bills, emptyList(), emptyList(), EMPTY)
        }
        return combine(holdings, loans.prepayments, txns.observeMonth(month)) { o, prepayments, monthTxns ->
            o.copy(
                prepayments = prepayments,
                monthTxns = monthTxns,
                snapshot = ForecastEngine.snapshot(
                    LocalDate.now(), o.profile, o.accounts, o.cards, o.loans, o.bills, monthTxns, prepayments,
                ),
            )
        }
    }

    private companion object {
        val EMPTY = ForecastEngine.snapshot(
            LocalDate.now(), UserProfile(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        )
    }
}

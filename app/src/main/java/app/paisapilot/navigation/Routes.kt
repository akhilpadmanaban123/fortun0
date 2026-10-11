package app.paisapilot.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val DUES = "dues"
    const val ACTIVITY = "activity"
    const val WALLET = "wallet"
    const val MORE = "more"

    const val INSIGHTS = "insights"
    const val SAFETY = "safety"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val COACH = "coach"

    /** Argument names read by the ViewModels through SavedStateHandle. */
    const val ARG_ID = "id"
    const val ARG_TYPE = "type"
    const val ARG_CARD = "cardId"

    const val LOAN_DETAIL = "loan/{$ARG_ID}"
    const val LOAN_EDIT = "loanEdit?$ARG_ID={$ARG_ID}"
    const val BILL_EDIT = "billEdit?$ARG_ID={$ARG_ID}"
    const val ACCOUNT_EDIT = "accountEdit?$ARG_ID={$ARG_ID}"
    const val CARD_EDIT = "cardEdit?$ARG_ID={$ARG_ID}"
    const val TXN_ADD = "txnAdd?$ARG_TYPE={$ARG_TYPE}&$ARG_CARD={$ARG_CARD}"

    fun loanDetail(id: String) = "loan/$id"
    fun loanEdit(id: String? = null) = withId("loanEdit", id)
    fun billEdit(id: String? = null) = withId("billEdit", id)
    fun accountEdit(id: String? = null) = withId("accountEdit", id)
    fun cardEdit(id: String? = null) = withId("cardEdit", id)

    /** [type] is a TxnType name; [cardId] pre-selects a card bill payment. */
    fun txnAdd(type: String? = null, cardId: String? = null): String {
        val args = listOfNotNull(type?.let { "$ARG_TYPE=$it" }, cardId?.let { "$ARG_CARD=$it" })
        return if (args.isEmpty()) "txnAdd" else "txnAdd?" + args.joinToString("&")
    }

    private fun withId(base: String, id: String?) = if (id == null) base else "$base?$ARG_ID=$id"
}

/** The five bottom-bar destinations. */
enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Rounded.Home),
    DUES(Routes.DUES, "EMIs & Bills", Icons.Rounded.EventNote),
    ACTIVITY(Routes.ACTIVITY, "Activity", Icons.Rounded.SwapVert),
    WALLET(Routes.WALLET, "Wallet", Icons.Rounded.AccountBalanceWallet),
    MORE(Routes.MORE, "More", Icons.Rounded.Menu),
}

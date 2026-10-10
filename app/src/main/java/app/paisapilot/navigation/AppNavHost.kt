package app.paisapilot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.paisapilot.feature.activity.ActivityScreen
import app.paisapilot.feature.activity.TxnAddScreen
import app.paisapilot.feature.dues.BillEditScreen
import app.paisapilot.feature.dues.DuesScreen
import app.paisapilot.feature.dues.LoanDetailScreen
import app.paisapilot.feature.dues.LoanEditScreen
import app.paisapilot.feature.home.HomeScreen
import app.paisapilot.feature.insights.InsightsScreen
import app.paisapilot.feature.more.MoreScreen
import app.paisapilot.feature.premium.PremiumScreen
import app.paisapilot.feature.profile.ProfileScreen
import app.paisapilot.feature.safety.SafetyScreen
import app.paisapilot.feature.settings.SettingsScreen
import app.paisapilot.feature.wallet.AccountEditScreen
import app.paisapilot.feature.wallet.CardEditScreen
import app.paisapilot.feature.wallet.WalletScreen

@Composable
fun AppNavHost(nav: NavHostController, modifier: Modifier = Modifier) {
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenTab = nav::switchTab,
                onNavigate = { nav.navigate(it) },
            )
        }
        composable(Routes.DUES) {
            DuesScreen(
                onOpenLoan = { nav.navigate(Routes.loanDetail(it)) },
                onAddLoan = { nav.navigate(Routes.loanEdit()) },
                onEditBill = { nav.navigate(Routes.billEdit(it)) },
            )
        }
        composable(Routes.ACTIVITY) {
            ActivityScreen(onAdd = { nav.navigate(Routes.txnAdd()) })
        }
        composable(Routes.WALLET) {
            WalletScreen(
                onEditAccount = { nav.navigate(Routes.accountEdit(it)) },
                onEditCard = { nav.navigate(Routes.cardEdit(it)) },
                onPayCard = { nav.navigate(Routes.txnAdd(type = "TRANSFER", cardId = it)) },
            )
        }
        composable(Routes.MORE) { MoreScreen(onNavigate = { nav.navigate(it) }) }

        composable(Routes.LOAN_DETAIL, arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType })) {
            LoanDetailScreen(onBack = back, onEdit = { nav.navigate(Routes.loanEdit(it)) })
        }
        optionalArgs(Routes.LOAN_EDIT, Routes.ARG_ID) {
            // After deleting, the detail screen underneath no longer has anything to show.
            LoanEditScreen(onDone = back, onDeleted = { nav.popBackStack(Routes.DUES, inclusive = false) })
        }
        optionalArgs(Routes.BILL_EDIT, Routes.ARG_ID) { BillEditScreen(onDone = back) }
        optionalArgs(Routes.ACCOUNT_EDIT, Routes.ARG_ID) { AccountEditScreen(onDone = back) }
        optionalArgs(Routes.CARD_EDIT, Routes.ARG_ID) { CardEditScreen(onDone = back) }
        optionalArgs(Routes.TXN_ADD, Routes.ARG_TYPE, Routes.ARG_CARD) { TxnAddScreen(onDone = back) }

        composable(Routes.INSIGHTS) { InsightsScreen(onBack = back, onOpenPremium = { nav.navigate(Routes.PREMIUM) }) }
        composable(Routes.SAFETY) { SafetyScreen(onBack = back) }
        composable(Routes.PREMIUM) { PremiumScreen(onBack = back) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = back, onEditProfile = { nav.navigate(Routes.PROFILE) }) }
        composable(Routes.PROFILE) { ProfileScreen(onboarding = false, onDone = back) }
    }
}

/** A destination whose string arguments may all be absent (add vs. edit). */
private fun NavGraphBuilder.optionalArgs(route: String, vararg args: String, content: @Composable () -> Unit) {
    composable(
        route = route,
        arguments = args.map { navArgument(it) { type = NavType.StringType; nullable = true } },
    ) { content() }
}

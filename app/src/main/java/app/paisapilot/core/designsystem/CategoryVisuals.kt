package app.paisapilot.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import app.paisapilot.core.model.AccountType
import app.paisapilot.core.model.BillCategory
import app.paisapilot.core.model.LoanType
import app.paisapilot.core.model.TxnCategory

val TxnCategory.icon: ImageVector
    get() = when (this) {
        TxnCategory.SALARY -> Icons.Rounded.Work
        TxnCategory.FREELANCE -> Icons.Rounded.Person
        TxnCategory.INTEREST -> Icons.Rounded.Savings
        TxnCategory.REFUND -> Icons.Rounded.Payments
        TxnCategory.GIFT -> Icons.Rounded.CardGiftcard
        TxnCategory.OTHER_INCOME -> Icons.Rounded.Payments
        TxnCategory.FOOD -> Icons.Rounded.Restaurant
        TxnCategory.GROCERIES -> Icons.Rounded.ShoppingCart
        TxnCategory.TRANSPORT -> Icons.Rounded.DirectionsBus
        TxnCategory.FUEL -> Icons.Rounded.LocalGasStation
        TxnCategory.SHOPPING -> Icons.Rounded.ShoppingBag
        TxnCategory.RENT -> Icons.Rounded.Home
        TxnCategory.UTILITIES -> Icons.Rounded.Bolt
        TxnCategory.HEALTH -> Icons.Rounded.LocalHospital
        TxnCategory.ENTERTAINMENT -> Icons.Rounded.Movie
        TxnCategory.EDUCATION -> Icons.Rounded.School
        TxnCategory.TRAVEL -> Icons.Rounded.Flight
        TxnCategory.FAMILY -> Icons.Rounded.Home
        TxnCategory.EMI -> Icons.Rounded.AccountBalance
        TxnCategory.OTHER_EXPENSE -> Icons.Rounded.Receipt
        TxnCategory.TRANSFER -> Icons.Rounded.SwapHoriz
        TxnCategory.CARD_PAYMENT -> Icons.Rounded.CreditCard
    }

val LoanType.icon: ImageVector
    get() = when (this) {
        LoanType.HOME -> Icons.Rounded.Home
        LoanType.BIKE -> Icons.Rounded.TwoWheeler
        LoanType.CAR -> Icons.Rounded.DirectionsCar
        LoanType.EDUCATION -> Icons.Rounded.School
        LoanType.GOLD -> Icons.Rounded.Savings
        LoanType.CONSUMER -> Icons.Rounded.PhoneAndroid
        LoanType.CREDIT_CARD_EMI -> Icons.Rounded.CreditCard
        LoanType.PERSONAL, LoanType.OTHER -> Icons.Rounded.AccountBalance
    }

val BillCategory.icon: ImageVector
    get() = when (this) {
        BillCategory.RENT, BillCategory.MAINTENANCE -> Icons.Rounded.Home
        BillCategory.ELECTRICITY, BillCategory.GAS -> Icons.Rounded.Bolt
        BillCategory.WATER -> Icons.Rounded.WaterDrop
        BillCategory.MOBILE -> Icons.Rounded.PhoneAndroid
        BillCategory.INTERNET -> Icons.Rounded.Wifi
        BillCategory.SUBSCRIPTION -> Icons.Rounded.Subscriptions
        BillCategory.HEALTH_INSURANCE, BillCategory.LIFE_INSURANCE -> Icons.Rounded.HealthAndSafety
        BillCategory.VEHICLE_INSURANCE -> Icons.Rounded.DirectionsCar
        BillCategory.SCHOOL_FEES -> Icons.Rounded.School
        BillCategory.SIP -> Icons.Rounded.Savings
        BillCategory.OTHER -> Icons.Rounded.Receipt
    }

val AccountType.icon: ImageVector
    get() = when (this) {
        AccountType.CASH -> Icons.Rounded.Payments
        AccountType.WALLET -> Icons.Rounded.AccountBalanceWallet
        AccountType.FIXED_DEPOSIT, AccountType.INVESTMENT -> Icons.Rounded.Savings
        else -> Icons.Rounded.AccountBalance
    }

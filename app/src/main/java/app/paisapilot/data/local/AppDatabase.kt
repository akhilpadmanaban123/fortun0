package app.paisapilot.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.paisapilot.data.local.dao.AccountDao
import app.paisapilot.data.local.dao.BillDao
import app.paisapilot.data.local.dao.CreditCardDao
import app.paisapilot.data.local.dao.DailyCheckinDao
import app.paisapilot.data.local.dao.LoanDao
import app.paisapilot.data.local.dao.LoanPrepaymentDao
import app.paisapilot.data.local.dao.TxnDao
import app.paisapilot.data.local.entity.Account
import app.paisapilot.data.local.entity.Bill
import app.paisapilot.data.local.entity.CreditCard
import app.paisapilot.data.local.entity.DailyCheckin
import app.paisapilot.data.local.entity.Loan
import app.paisapilot.data.local.entity.LoanPrepayment
import app.paisapilot.data.local.entity.Txn

@Database(
    entities = [Account::class, CreditCard::class, Loan::class, Bill::class, Txn::class, LoanPrepayment::class, DailyCheckin::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun loanDao(): LoanDao
    abstract fun billDao(): BillDao
    abstract fun txnDao(): TxnDao
    abstract fun loanPrepaymentDao(): LoanPrepaymentDao
    abstract fun dailyCheckinDao(): DailyCheckinDao

    companion object {
        const val NAME = "paisapilot.db"

        /** Version 2 adds extra payments towards a loan's principal. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `loan_prepayments` (" +
                        "`id` TEXT NOT NULL, `loanId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, " +
                        "`dateEpochDay` INTEGER NOT NULL, `afterInstallment` INTEGER NOT NULL, `txnId` TEXT, " +
                        "`updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_loan_prepayments_loanId` ON `loan_prepayments` (`loanId`)")
            }
        }

        /**
         * Version 3 adds the daily "how do you feel about today's spending" answers, and each
         * loan's rate type and part-payment rules (existing loans start as "not sure").
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_checkins` (" +
                        "`id` TEXT NOT NULL, `dayEpochDay` INTEGER NOT NULL, `feeling` TEXT NOT NULL, " +
                        "`everydayMinor` INTEGER NOT NULL, `limitMinor` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("ALTER TABLE `loans` ADD COLUMN `rateType` TEXT NOT NULL DEFAULT 'UNSURE'")
                db.execSQL("ALTER TABLE `loans` ADD COLUMN `prepayPolicy` TEXT NOT NULL DEFAULT 'UNSURE'")
                db.execSQL("ALTER TABLE `loans` ADD COLUMN `prepayChargePct` REAL NOT NULL DEFAULT 0")
            }
        }

        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}

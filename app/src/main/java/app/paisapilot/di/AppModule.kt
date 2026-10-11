package app.paisapilot.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.paisapilot.data.local.AppDatabase
import app.paisapilot.data.local.dao.AccountDao
import app.paisapilot.data.local.dao.BillDao
import app.paisapilot.data.local.dao.CreditCardDao
import app.paisapilot.data.local.dao.DailyCheckinDao
import app.paisapilot.data.local.dao.LoanDao
import app.paisapilot.data.local.dao.LoanPrepaymentDao
import app.paisapilot.data.local.dao.TxnDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

    @Provides fun accountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun creditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()
    @Provides fun loanDao(db: AppDatabase): LoanDao = db.loanDao()
    @Provides fun billDao(db: AppDatabase): BillDao = db.billDao()
    @Provides fun txnDao(db: AppDatabase): TxnDao = db.txnDao()
    @Provides fun loanPrepaymentDao(db: AppDatabase): LoanPrepaymentDao = db.loanPrepaymentDao()
    @Provides fun dailyCheckinDao(db: AppDatabase): DailyCheckinDao = db.dailyCheckinDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("profile") }
}

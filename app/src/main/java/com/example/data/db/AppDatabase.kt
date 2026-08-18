package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = runCatching {
        TransactionType.valueOf(value)
    }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromBillStatus(value: BillStatus): String = value.name

    @TypeConverter
    fun toBillStatus(value: String): BillStatus = runCatching {
        BillStatus.valueOf(value)
    }.getOrDefault(BillStatus.PENDING)
}

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        BillEntity::class,
        BillItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun billDao(): BillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_accounting_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val accountDao = db.accountDao()
            val transactionDao = db.transactionDao()
            val billDao = db.billDao()

            // Default Accounts
            val defaultAccounts = listOf(
                AccountEntity(name = "Cash Wallet", type = "CASH", initialBalance = 2500.0, iconName = "wallet", colorHex = "#16A34A"),
                AccountEntity(name = "Main Bank (HDFC/SBI)", type = "BANK", initialBalance = 45000.0, iconName = "account_balance", colorHex = "#2563EB"),
                AccountEntity(name = "Savings Vault", type = "SAVINGS", initialBalance = 150000.0, iconName = "savings", colorHex = "#9333EA"),
                AccountEntity(name = "Credit Card", type = "CREDIT_CARD", initialBalance = 0.0, iconName = "credit_card", colorHex = "#EA580C")
            )
            accountDao.insertInitialAccounts(defaultAccounts)

            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L

            // Seed a few realistic initial transactions in INR
            val sampleTransactions = listOf(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = 65000.0,
                    category = "Salary",
                    accountId = 2,
                    accountName = "Main Bank (HDFC/SBI)",
                    dateMillis = now - (3 * oneDay),
                    note = "Monthly Primary Salary"
                ),
                TransactionEntity(
                    type = TransactionType.INCOME,
                    amount = 18500.0,
                    category = "Freelance / Consulting",
                    accountId = 2,
                    accountName = "Main Bank (HDFC/SBI)",
                    dateMillis = now - (1 * oneDay),
                    note = "Android App Development Milestone 1"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 2450.0,
                    category = "Groceries",
                    accountId = 1,
                    accountName = "Cash Wallet",
                    dateMillis = now - (2 * oneDay),
                    note = "Monthly groceries & fresh vegetables"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 1200.0,
                    category = "Food & Dining",
                    accountId = 1,
                    accountName = "Cash Wallet",
                    dateMillis = now - (1 * oneDay),
                    note = "Family dinner at restaurant"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 1999.0,
                    category = "Bills & Utilities",
                    accountId = 2,
                    accountName = "Main Bank (HDFC/SBI)",
                    dateMillis = now - (4 * oneDay),
                    note = "Fiber Broadband & Electricity"
                ),
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = 850.0,
                    category = "Transport",
                    accountId = 4,
                    accountName = "Credit Card",
                    dateMillis = now,
                    note = "Petrol & Metro smart card recharge"
                )
            )
            for (t in sampleTransactions) {
                transactionDao.insertTransaction(t)
            }

            // Seed a sample bill/invoice
            val sampleBill = BillEntity(
                billNumber = "INV-2026-001",
                issueDateMillis = now - (5 * oneDay),
                dueDateMillis = now + (10 * oneDay),
                senderName = "TechVenture Solutions",
                senderContact = "billing@techventures.in | +91 98765 43210",
                senderAddress = "Cyber City, Phase 2, Bengaluru, Karnataka",
                clientName = "Horizon Digital Media",
                clientContact = "accounts@horizondigital.in",
                clientAddress = "Plot 42, Hitec City, Hyderabad, Telangana",
                subtotal = 35000.00,
                taxRate = 18.0,
                discount = 2000.00,
                totalAmount = 39300.00,
                status = BillStatus.PENDING,
                notes = "Payment due within 15 days via UPI / IMPS / NEFT. Thank you for your business!",
                currencySymbol = "₹"
            )
            val sampleBillItems = listOf(
                BillItemEntity(billId = 0, description = "Mobile Application UI/UX Design System", quantity = 1.0, unitPrice = 20000.00, totalPrice = 20000.00),
                BillItemEntity(billId = 0, description = "Jetpack Compose Development & Integration", quantity = 1.0, unitPrice = 15000.00, totalPrice = 15000.00)
            )
            billDao.saveBillWithItems(sampleBill, sampleBillItems)
        }
    }
}

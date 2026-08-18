package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.auth.UserData
import com.example.data.db.AppDatabase
import com.example.data.db.BillWithItems
import com.example.data.model.AccountEntity
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class PeriodFilter(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

data class CategoryExpenseStat(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int
)

data class BillOverviewStats(
    val totalBilled: Double,
    val totalPaid: Double,
    val totalPending: Double,
    val totalOverdue: Double,
    val countPaid: Int,
    val countPending: Int,
    val countOverdue: Int
)

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccountingRepository
    private val authManager: AuthManager = AuthManager(application)

    val currentUser: StateFlow<UserData?> = authManager.currentUser
    val isAuthLoading: StateFlow<Boolean> = authManager.isLoading
    val authErrorMessage: StateFlow<String?> = authManager.errorMessage

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AccountingRepository(db.transactionDao(), db.accountDao(), db.billDao())
    }

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBillsWithItems: StateFlow<List<BillWithItems>> = repository.allBillsWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCurrency = MutableStateFlow("₹")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.THIS_MONTH)
    val selectedPeriod: StateFlow<PeriodFilter> = _selectedPeriod.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setCurrency(symbol: String) {
        _selectedCurrency.value = symbol
    }

    fun setPeriod(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Filtered Transactions according to Period, Type, and Search Query
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedPeriod,
        _selectedTypeFilter,
        _searchQuery
    ) { transactions, period, typeFilter, query ->
        val (startTime, endTime) = getTimeBounds(period)

        transactions.filter { tx ->
            val withinTime = if (startTime != null && endTime != null) {
                tx.dateMillis in startTime..endTime
            } else true

            val matchesType = typeFilter == null || tx.type == typeFilter

            val matchesQuery = query.isBlank() ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.accountName.contains(query, ignoreCase = true)

            withinTime && matchesType && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations for Summary Stats
    val currentPeriodIncome: StateFlow<Double> = combine(allTransactions, _selectedPeriod) { txs, period ->
        val (start, end) = getTimeBounds(period)
        txs.filter {
            it.type == TransactionType.INCOME && (start == null || end == null || it.dateMillis in start..end)
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentPeriodExpense: StateFlow<Double> = combine(allTransactions, _selectedPeriod) { txs, period ->
        val (start, end) = getTimeBounds(period)
        txs.filter {
            it.type == TransactionType.EXPENSE && (start == null || end == null || it.dateMillis in start..end)
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total Net Balance across all time accounts
    val netBalance: StateFlow<Double> = combine(allAccounts, allTransactions) { accounts, txs ->
        val initialTotal = accounts.sumOf { it.initialBalance }
        val incomeTotal = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenseTotal = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        initialTotal + incomeTotal - expenseTotal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Per-Account Calculated Balances
    val accountBalances: StateFlow<Map<Long, Double>> = combine(allAccounts, allTransactions) { accounts, txs ->
        accounts.associate { account ->
            var bal = account.initialBalance
            txs.forEach { tx ->
                when (tx.type) {
                    TransactionType.INCOME -> if (tx.accountId == account.id) bal += tx.amount
                    TransactionType.EXPENSE -> if (tx.accountId == account.id) bal -= tx.amount
                    TransactionType.TRANSFER -> {
                        if (tx.accountId == account.id) bal -= tx.amount
                        if (tx.destinationAccountId == account.id) bal += tx.amount
                    }
                }
            }
            account.id to bal
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Category Breakdown for current selected period
    val categoryStats: StateFlow<List<CategoryExpenseStat>> = combine(allTransactions, _selectedPeriod) { txs, period ->
        val (start, end) = getTimeBounds(period)
        val periodExpenses = txs.filter {
            it.type == TransactionType.EXPENSE && (start == null || end == null || it.dateMillis in start..end)
        }
        val totalExp = periodExpenses.sumOf { it.amount }
        if (totalExp <= 0.0) {
            emptyList()
        } else {
            periodExpenses
                .groupBy { it.category }
                .map { (cat, list) ->
                    val catSum = list.sumOf { it.amount }
                    CategoryExpenseStat(
                        category = cat,
                        amount = catSum,
                        percentage = (catSum / totalExp).toFloat(),
                        count = list.size
                    )
                }
                .sortedByDescending { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bill overview stats
    val billOverview: StateFlow<BillOverviewStats> = allBillsWithItems.map { billsWithItems ->
        var billed = 0.0
        var paid = 0.0
        var pending = 0.0
        var overdue = 0.0
        var countPaid = 0
        var countPending = 0
        var countOverdue = 0

        val now = System.currentTimeMillis()
        billsWithItems.forEach { item ->
            val b = item.bill
            billed += b.totalAmount
            val isOverdue = b.status != BillStatus.PAID && b.dueDateMillis < now
            when {
                b.status == BillStatus.PAID -> {
                    paid += b.totalAmount
                    countPaid++
                }
                isOverdue -> {
                    overdue += b.totalAmount
                    countOverdue++
                }
                else -> {
                    pending += b.totalAmount
                    countPending++
                }
            }
        }
        BillOverviewStats(billed, paid, pending, overdue, countPaid, countPending, countOverdue)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BillOverviewStats(0.0, 0.0, 0.0, 0.0, 0, 0, 0)
    )

    // Helper for date ranges
    private fun getTimeBounds(period: PeriodFilter): Pair<Long?, Long?> {
        val cal = Calendar.getInstance()
        return when (period) {
            PeriodFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                start to end
            }
            PeriodFilter.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                start to end
            }
            PeriodFilter.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                start to end
            }
            PeriodFilter.ALL_TIME -> null to null
        }
    }

    // CRUD for Transactions
    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        accountId: Long,
        accountName: String,
        destinationAccountId: Long? = null,
        destinationAccountName: String? = null,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "",
        billId: Long? = null
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    type = type,
                    amount = amount,
                    category = category,
                    accountId = accountId,
                    accountName = accountName,
                    destinationAccountId = destinationAccountId,
                    destinationAccountName = destinationAccountName,
                    dateMillis = dateMillis,
                    note = note,
                    billId = billId
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    // Transfer Funds
    fun transferFunds(
        fromAccountId: Long,
        fromAccountName: String,
        toAccountId: Long,
        toAccountName: String,
        amount: Double,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    category = "Transfer",
                    accountId = fromAccountId,
                    accountName = fromAccountName,
                    destinationAccountId = toAccountId,
                    destinationAccountName = toAccountName,
                    dateMillis = dateMillis,
                    note = note.ifBlank { "Transfer from $fromAccountName to $toAccountName" }
                )
            )
        }
    }

    // CRUD for Accounts
    fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        colorHex: String = "#006C4C"
    ) {
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    initialBalance = initialBalance,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteAccountById(id)
        }
    }

    // CRUD for Bills & Invoices
    fun saveBill(
        bill: BillEntity,
        items: List<BillItemEntity>,
        autoRecordAsIncomeIfPaid: Boolean = false
    ) {
        viewModelScope.launch {
            val billId = if (bill.id == 0L) {
                repository.saveBillWithItems(bill, items)
            } else {
                repository.updateBillWithItems(bill, items)
                bill.id
            }

            if (autoRecordAsIncomeIfPaid && bill.status == BillStatus.PAID) {
                val primaryAccount = allAccounts.value.firstOrNull()
                repository.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.INCOME,
                        amount = bill.totalAmount,
                        category = "Freelance / Projects",
                        accountId = primaryAccount?.id ?: 1L,
                        accountName = primaryAccount?.name ?: "Cash",
                        dateMillis = System.currentTimeMillis(),
                        note = "Invoice #${bill.billNumber} - ${bill.clientName}",
                        billId = billId
                    )
                )
            }
        }
    }

    fun updateBillStatus(billId: Long, status: BillStatus, recordIncome: Boolean = false) {
        viewModelScope.launch {
            repository.updateBillStatus(billId, status)
            if (recordIncome && status == BillStatus.PAID) {
                val billWithItem = allBillsWithItems.value.firstOrNull { it.bill.id == billId }
                if (billWithItem != null) {
                    val primaryAccount = allAccounts.value.firstOrNull()
                    repository.insertTransaction(
                        TransactionEntity(
                            type = TransactionType.INCOME,
                            amount = billWithItem.bill.totalAmount,
                            category = "Freelance / Projects",
                            accountId = primaryAccount?.id ?: 1L,
                            accountName = primaryAccount?.name ?: "Cash",
                            dateMillis = System.currentTimeMillis(),
                            note = "Paid Invoice #${billWithItem.bill.billNumber} (${billWithItem.bill.clientName})",
                            billId = billId
                        )
                    )
                }
            }
        }
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch {
            repository.deleteBillAndItems(billId)
        }
    }

    // Google Sign-In Actions
    fun signInWithGoogle(context: Context, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(context)
            onResult(result.isSuccess)
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authManager.signOut(context)
        }
    }

    fun clearAuthError() {
        authManager.clearError()
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.BillWithItems
import com.example.data.model.BillStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AccountingViewModel

enum class AppTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
    TRANSACTIONS("Transactions", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
    BILLS("Bill Generator", Icons.Default.Description, Icons.Outlined.Description),
    ACCOUNTS("Accounts", Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AccountingApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingApp(viewModel: AccountingViewModel = viewModel()) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    // Auth states
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()

    // State collections
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val allBillsWithItems by viewModel.allBillsWithItems.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val netBalance by viewModel.netBalance.collectAsStateWithLifecycle()
    val periodIncome by viewModel.currentPeriodIncome.collectAsStateWithLifecycle()
    val periodExpense by viewModel.currentPeriodExpense.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val categoryStats by viewModel.categoryStats.collectAsStateWithLifecycle()
    val billStats by viewModel.billOverview.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var initialTransactionTypeForSheet by remember { mutableStateOf(TransactionType.EXPENSE) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    var showAddBillDialog by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<BillWithItems?>(null) }
    var billToPreview by remember { mutableStateOf<BillWithItems?>(null) }

    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (currentTab) {
                                AppTab.DASHBOARD -> "Personal Accounting"
                                AppTab.TRANSACTIONS -> "Transactions Ledger"
                                AppTab.BILLS -> "Bill & Invoice Generator"
                                AppTab.ACCOUNTS -> "Accounts & Wallets"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    }
                },
                actions = {
                    // Currency Picker Chip
                    SuggestionChip(
                        onClick = { showCurrencyDialog = true },
                        label = {
                            Text(
                                text = selectedCurrency,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.CurrencyExchange,
                                contentDescription = "Currency",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("currency_picker_chip")
                    )

                    // Google User Profile Avatar Button
                    UserProfileAvatar(
                        userData = currentUser,
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> {
                    DashboardScreen(
                        netBalance = netBalance,
                        periodIncome = periodIncome,
                        periodExpense = periodExpense,
                        recentTransactions = allTransactions,
                        categoryStats = categoryStats,
                        selectedPeriod = selectedPeriod,
                        currencySymbol = selectedCurrency,
                        userData = currentUser,
                        isAuthLoading = isAuthLoading,
                        onSignInWithGoogleClick = { viewModel.signInWithGoogle(context) },
                        onPeriodSelected = { viewModel.setPeriod(it) },
                        onAddExpenseClick = {
                            initialTransactionTypeForSheet = TransactionType.EXPENSE
                            transactionToEdit = null
                            showAddTransactionSheet = true
                        },
                        onAddIncomeClick = {
                            initialTransactionTypeForSheet = TransactionType.INCOME
                            transactionToEdit = null
                            showAddTransactionSheet = true
                        },
                        onNewBillClick = {
                            billToEdit = null
                            showAddBillDialog = true
                        },
                        onTransferClick = { showTransferDialog = true },
                        onNavigateToTransactions = { currentTab = AppTab.TRANSACTIONS },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) }
                    )
                }
                AppTab.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = filteredTransactions,
                        currencySymbol = selectedCurrency,
                        selectedPeriod = selectedPeriod,
                        selectedType = selectedTypeFilter,
                        searchQuery = searchQuery,
                        onPeriodSelected = { viewModel.setPeriod(it) },
                        onTypeSelected = { viewModel.setTypeFilter(it) },
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onAddTransactionClick = {
                            initialTransactionTypeForSheet = TransactionType.EXPENSE
                            transactionToEdit = null
                            showAddTransactionSheet = true
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onTransactionClick = { tx ->
                            selectedTransactionForDetail = tx
                        }
                    )
                }
                AppTab.BILLS -> {
                    BillGeneratorScreen(
                        billsWithItems = allBillsWithItems,
                        stats = billStats,
                        currencySymbol = selectedCurrency,
                        onCreateBillClick = {
                            billToEdit = null
                            showAddBillDialog = true
                        },
                        onBillClick = { billWithItems ->
                            billToPreview = billWithItems
                        },
                        onEditBillClick = { billWithItems ->
                            billToEdit = billWithItems
                            showAddBillDialog = true
                        },
                        onDeleteBillClick = { billId ->
                            viewModel.deleteBill(billId)
                        },
                        onMarkAsPaid = { billId ->
                            viewModel.updateBillStatus(billId, BillStatus.PAID, recordIncome = true)
                        }
                    )
                }
                AppTab.ACCOUNTS -> {
                    AccountsScreen(
                        accounts = allAccounts,
                        accountBalances = accountBalances,
                        currencySymbol = selectedCurrency,
                        onAddAccountClick = { showAddAccountDialog = true },
                        onTransferClick = { showTransferDialog = true },
                        onDeleteAccount = { viewModel.deleteAccount(it) }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet: Add / Edit Transaction
    if (showAddTransactionSheet) {
        val initialTx = transactionToEdit ?: if (initialTransactionTypeForSheet == TransactionType.INCOME) {
            TransactionEntity(type = TransactionType.INCOME, amount = 0.0, category = "Salary", accountId = allAccounts.firstOrNull()?.id ?: 1L, accountName = allAccounts.firstOrNull()?.name ?: "Cash")
        } else {
            null
        }

        AddEditTransactionSheet(
            accounts = allAccounts,
            currencySymbol = selectedCurrency,
            initialTransaction = initialTx,
            onDismiss = {
                showAddTransactionSheet = false
                transactionToEdit = null
            },
            onSave = { type, amount, category, accountId, accountName, destId, destName, dateMillis, note ->
                if (transactionToEdit != null) {
                    viewModel.updateTransaction(
                        transactionToEdit!!.copy(
                            type = type,
                            amount = amount,
                            category = category,
                            accountId = accountId,
                            accountName = accountName,
                            destinationAccountId = destId,
                            destinationAccountName = destName,
                            dateMillis = dateMillis,
                            note = note
                        )
                    )
                } else {
                    viewModel.addTransaction(
                        type = type,
                        amount = amount,
                        category = category,
                        accountId = accountId,
                        accountName = accountName,
                        destinationAccountId = destId,
                        destinationAccountName = destName,
                        dateMillis = dateMillis,
                        note = note
                    )
                }
            }
        )
    }

    // Dialog: Add / Edit Bill
    if (showAddBillDialog) {
        AddEditBillDialog(
            currencySymbol = selectedCurrency,
            initialBillWithItems = billToEdit,
            onDismiss = {
                showAddBillDialog = false
                billToEdit = null
            },
            onSave = { bill, items, autoRecordIncome ->
                viewModel.saveBill(bill, items, autoRecordIncome)
            }
        )
    }

    // Dialog: Bill Preview & Share
    billToPreview?.let { previewBill ->
        // Retrieve fresh instance from state flow if updated
        val currentFreshBill = allBillsWithItems.firstOrNull { it.bill.id == previewBill.bill.id } ?: previewBill
        BillPreviewDialog(
            billWithItems = currentFreshBill,
            currencySymbol = selectedCurrency,
            onDismiss = { billToPreview = null },
            onEdit = {
                billToPreview = null
                billToEdit = currentFreshBill
                showAddBillDialog = true
            },
            onDelete = {
                viewModel.deleteBill(currentFreshBill.bill.id)
                billToPreview = null
            },
            onUpdateStatus = { newStatus, recordIncome ->
                viewModel.updateBillStatus(currentFreshBill.bill.id, newStatus, recordIncome)
            }
        )
    }

    // Dialog: Transfer Funds
    if (showTransferDialog) {
        TransferFundsDialog(
            accounts = allAccounts,
            currencySymbol = selectedCurrency,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromId, fromName, toId, toName, amount, note ->
                viewModel.transferFunds(fromId, fromName, toId, toName, amount, note)
            }
        )
    }

    // Dialog: Add Account
    if (showAddAccountDialog) {
        AddAccountDialog(
            currencySymbol = selectedCurrency,
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, type, initBal, colorHex ->
                viewModel.addAccount(name, type, initBal, colorHex)
            }
        )
    }

    // Dialog: Currency Selector
    if (showCurrencyDialog) {
        CurrencySelectorDialog(
            currentSymbol = selectedCurrency,
            onDismiss = { showCurrencyDialog = false },
            onSelectCurrency = { viewModel.setCurrency(it) }
        )
    }

    // Dialog: Transaction Detail & PDF Generator
    selectedTransactionForDetail?.let { txn ->
        TransactionDetailDialog(
            transaction = txn,
            currencySymbol = selectedCurrency,
            onDismiss = { selectedTransactionForDetail = null },
            onEdit = {
                transactionToEdit = txn
                showAddTransactionSheet = true
            },
            onDelete = {
                viewModel.deleteTransaction(txn.id)
            }
        )
    }

    // Dialog: Google User Profile & Sign-In
    if (showProfileDialog) {
        UserProfileDialog(
            userData = currentUser,
            isLoading = isAuthLoading,
            errorMessage = authErrorMessage,
            onDismiss = { showProfileDialog = false },
            onSignInWithGoogle = {
                viewModel.signInWithGoogle(context)
            },
            onSignOut = {
                viewModel.signOut(context)
            },
            onClearError = {
                viewModel.clearAuthError()
            }
        )
    }
}

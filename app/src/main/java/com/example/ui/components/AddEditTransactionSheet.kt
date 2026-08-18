package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.utils.CategoriesData
import com.example.utils.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    accounts: List<AccountEntity>,
    currencySymbol: String,
    initialTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amount: Double,
        category: String,
        accountId: Long,
        accountName: String,
        destinationAccountId: Long?,
        destinationAccountName: String?,
        dateMillis: Long,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(if (initialTransaction != null) initialTransaction.amount.toString() else "") }
    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category ?: if (selectedType == TransactionType.INCOME) "Salary" else "Food & Dining"
        )
    }

    var selectedAccountId by remember {
        mutableLongStateOf(initialTransaction?.accountId ?: (accounts.firstOrNull()?.id ?: 1L))
    }
    var destinationAccountId by remember {
        mutableLongStateOf(initialTransaction?.destinationAccountId ?: (accounts.getOrNull(1)?.id ?: (accounts.firstOrNull()?.id ?: 1L)))
    }

    var noteText by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var dateMillis by remember { mutableLongStateOf(initialTransaction?.dateMillis ?: System.currentTimeMillis()) }

    val categories = if (selectedType == TransactionType.INCOME) {
        CategoriesData.incomeCategories
    } else {
        CategoriesData.expenseCategories
    }

    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            dateMillis = newCal.timeInMillis
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (initialTransaction != null) "Edit Transaction" else "Add Transaction",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Type Segmented Control (Expense / Income / Transfer)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        selectedCategory = CategoriesData.expenseCategories.first().name
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        activeContentColor = ExpenseRed
                    )
                ) {
                    Text("Expense", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        selectedCategory = CategoriesData.incomeCategories.first().name
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        activeContentColor = IncomeGreen
                    )
                ) {
                    Text("Income", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = {
                        selectedType = TransactionType.TRANSFER
                        selectedCategory = "Transfer"
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = TransferBlue.copy(alpha = 0.15f),
                        activeContentColor = TransferBlue
                    )
                ) {
                    Text("Transfer", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Input Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount ($currencySymbol)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (selectedType) {
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.TRANSFER -> TransferBlue
                            }
                        ),
                        placeholder = {
                            Text("0.00", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        },
                        leadingIcon = {
                            Text(
                                text = currencySymbol,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )

                    // Quick increment pills
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 50, 100, 500).forEach { plusVal ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val cur = amountText.toDoubleOrNull() ?: 0.0
                                        amountText = String.format("%.2f", cur + plusVal)
                                    },
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$plusVal",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection (Only for Income and Expense)
            if (selectedType != TransactionType.TRANSFER) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat.name },
                                label = { Text(cat.name, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.name),
                                        contentDescription = cat.name,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Account Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (selectedType == TransactionType.TRANSFER) "From Account" else "Account / Wallet",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = acc.name,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // If Transfer, Destination Account
            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "To Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(accounts) { acc ->
                            val isSelected = destinationAccountId == acc.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { destinationAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = acc.name,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { datePickerDialog.show() }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Date", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Text(
                    text = Formatters.formatFullDate(dateMillis),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note Input
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note / Description (Optional)") },
                placeholder = { Text("e.g. Grocery trip, Client payment...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
            val destAccount = if (selectedType == TransactionType.TRANSFER) {
                accounts.firstOrNull { it.id == destinationAccountId }
            } else null

            val isValid = (amountText.toDoubleOrNull() ?: 0.0) > 0.0 && selectedAccount != null

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && selectedAccount != null) {
                        onSave(
                            selectedType,
                            amount,
                            selectedCategory,
                            selectedAccount.id,
                            selectedAccount.name,
                            destAccount?.id,
                            destAccount?.name,
                            dateMillis,
                            noteText.trim()
                        )
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

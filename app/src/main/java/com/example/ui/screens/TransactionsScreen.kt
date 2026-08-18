package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionRow
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.PeriodFilter
import com.example.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    selectedPeriod: PeriodFilter,
    selectedType: TransactionType?,
    searchQuery: String,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onTypeSelected: (TransactionType?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAddTransactionClick: () -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit
) {
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("add_transaction_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Entry", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search by category, note, account...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactions_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Period Filters
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PeriodFilter.values()) { period ->
                        val isSelected = selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPeriodSelected(period) },
                            label = { Text(period.label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Type Filter Chips (All, Income, Expense, Transfer)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { onTypeSelected(null) },
                        label = { Text("All (${transactions.size})") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { onTypeSelected(if (selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
                        label = { Text("Expense") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { onTypeSelected(if (selectedType == TransactionType.INCOME) null else TransactionType.INCOME) },
                        label = { Text("Income") },
                        leadingIcon = {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.TRANSFER,
                        onClick = { onTypeSelected(if (selectedType == TransactionType.TRANSFER) null else TransactionType.TRANSFER) },
                        label = { Text("Transfer") },
                        leadingIcon = {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TransferBlue, modifier = Modifier.size(14.dp))
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Period Summary Ribbon
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${transactions.size} records",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (totalIncome > 0) {
                                Text(
                                    text = "+${Formatters.formatCurrency(totalIncome, currencySymbol)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            if (totalExpense > 0) {
                                Text(
                                    text = "-${Formatters.formatCurrency(totalExpense, currencySymbol)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FilterListOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No transactions found", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Try adjusting your filters, search term, or period",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                // Group transactions by date
                val grouped = transactions.groupBy { Formatters.formatDate(it.dateMillis) }
                grouped.forEach { (dateHeader, list) ->
                    item {
                        Text(
                            text = dateHeader,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(list, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            currencySymbol = currencySymbol,
                            onClick = { onTransactionClick(tx) },
                            onDelete = { onDeleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }
}

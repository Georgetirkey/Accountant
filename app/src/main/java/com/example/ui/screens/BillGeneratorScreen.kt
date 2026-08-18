package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.BillWithItems
import com.example.data.model.BillStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PendingAmber
import com.example.ui.viewmodel.BillOverviewStats
import com.example.utils.Formatters
import com.example.utils.PdfInvoiceGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillGeneratorScreen(
    billsWithItems: List<BillWithItems>,
    stats: BillOverviewStats,
    currencySymbol: String,
    onCreateBillClick: () -> Unit,
    onBillClick: (BillWithItems) -> Unit,
    onEditBillClick: (BillWithItems) -> Unit,
    onDeleteBillClick: (Long) -> Unit,
    onMarkAsPaid: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedStatusFilter by remember { mutableStateOf<BillStatus?>(null) }

    val filteredBills = remember(billsWithItems, selectedStatusFilter) {
        if (selectedStatusFilter == null) {
            billsWithItems
        } else {
            billsWithItems.filter { it.bill.status == selectedStatusFilter }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateBillClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("generate_bill_fab")
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = "New Bill")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Bill", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header stats overview card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Billing & Invoicing Overview",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Total Invoiced
                            Column {
                                Text("Total Invoiced", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    Formatters.formatCurrency(stats.totalBilled, currencySymbol),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("${billsWithItems.size} bills", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Collected (Paid)
                            Column {
                                Text("Collected", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    Formatters.formatCurrency(stats.totalPaid, currencySymbol),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                                Text("${stats.countPaid} paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Outstanding
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    Formatters.formatCurrency(stats.totalPending + stats.totalOverdue, currencySymbol),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stats.totalOverdue > 0) ExpenseRed else PendingAmber
                                )
                                Text("${stats.countPending + stats.countOverdue} pending", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Status Filter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All Bills (${billsWithItems.size})", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedStatusFilter == BillStatus.PENDING,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == BillStatus.PENDING) null else BillStatus.PENDING },
                        label = { Text("Pending", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedStatusFilter == BillStatus.PAID,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == BillStatus.PAID) null else BillStatus.PAID },
                        label = { Text("Paid", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedStatusFilter == BillStatus.OVERDUE,
                        onClick = { selectedStatusFilter = if (selectedStatusFilter == BillStatus.OVERDUE) null else BillStatus.OVERDUE },
                        label = { Text("Overdue", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Bills List
            if (filteredBills.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No bills found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap 'Generate Bill' to quickly create and share professional invoices with your clients.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredBills, key = { it.bill.id }) { item ->
                    val bill = item.bill
                    val sym = bill.currencySymbol.ifBlank { currencySymbol }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBillClick(item) }
                            .testTag("bill_card_${bill.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Bill # and Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "#${bill.billNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                StatusBadge(status = bill.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Client & Total Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bill.clientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (bill.clientContact.isNotBlank()) {
                                        Text(
                                            text = bill.clientContact,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${item.items.size} item(s) • Due: ${Formatters.formatFullDate(bill.dueDateMillis)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Text(
                                    text = Formatters.formatCurrency(bill.totalAmount, sym),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons row on card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Preview & View
                                    FilledTonalButton(
                                        onClick = { onBillClick(item) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preview", fontSize = 12.sp)
                                    }

                                    // Quick PDF Share
                                    IconButton(
                                        onClick = {
                                            val pdf = PdfInvoiceGenerator.generateBillInvoicePdf(context, item, sym)
                                            if (pdf != null) {
                                                PdfInvoiceGenerator.sharePdf(context, pdf, "Share Invoice #${bill.billNumber}")
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    // Quick Text Share Intent
                                    IconButton(
                                        onClick = {
                                            val shareText = Formatters.generateBillShareText(item)
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                putExtra(Intent.EXTRA_TITLE, "Invoice ${bill.billNumber} for ${bill.clientName}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Bill via"))
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (bill.status != BillStatus.PAID) {
                                        TextButton(
                                            onClick = { onMarkAsPaid(bill.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Mark Paid", fontSize = 12.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(
                                        onClick = { onEditBillClick(item) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteBillClick(bill.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

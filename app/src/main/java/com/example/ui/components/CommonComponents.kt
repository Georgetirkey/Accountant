package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.utils.CategoriesData
import com.example.utils.Formatters

fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "food & dining", "food", "dining", "restaurant" -> Icons.Default.Restaurant
        "groceries", "grocery" -> Icons.Default.ShoppingCart
        "bills & utilities", "bills", "utilities" -> Icons.Default.Bolt
        "shopping" -> Icons.Default.ShoppingBag
        "transport", "transit", "fuel" -> Icons.Default.DirectionsCar
        "rent / housing", "rent", "housing", "home" -> Icons.Default.Home
        "entertainment", "movies", "games" -> Icons.Default.Movie
        "health & medical", "health", "medical", "pharmacy" -> Icons.Default.MedicalServices
        "education", "books", "tuition" -> Icons.Default.School
        "travel", "flight", "hotel" -> Icons.Default.Flight
        "personal care", "salon", "fitness" -> Icons.Default.Spa
        "investment", "crypto", "stocks" -> Icons.Default.TrendingUp
        "salary", "paycheck" -> Icons.Default.Payments
        "freelance / projects", "freelance", "contract" -> Icons.Default.LaptopMac
        "business" -> Icons.Default.Store
        "investments / dividends", "dividends" -> Icons.Default.ShowChart
        "rental income" -> Icons.Default.RealEstateAgent
        "bonus / gift", "gift" -> Icons.Default.CardGiftcard
        "refunds" -> Icons.Default.AssignmentReturn
        "transfer" -> Icons.Default.SwapHoriz
        else -> Icons.Default.AccountBalanceWallet
    }
}

fun parseColor(hex: String, defaultColor: Color = EmeraldPrimaryLight): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

@Composable
fun StatusBadge(status: BillStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        BillStatus.PAID -> Triple(Color(0xFFDCFCE7), IncomeGreen, "PAID")
        BillStatus.PENDING -> Triple(Color(0xFFFEF3C7), PendingAmber, "PENDING")
        BillStatus.OVERDUE -> Triple(Color(0xFFFEE2E2), ExpenseRed, "OVERDUE")
        BillStatus.DRAFT -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), "DRAFT")
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CurrencySelectorDialog(
    currentSymbol: String,
    onDismiss: () -> Unit,
    onSelectCurrency: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Currency", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(scrollState)
            ) {
                CategoriesData.allCurrencies.forEach { (sym, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCurrency(sym)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 15.sp, fontWeight = if (currentSymbol == sym) FontWeight.Bold else FontWeight.Normal)
                        if (currentSymbol == sym) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val isTransfer = transaction.type == TransactionType.TRANSFER

    val amountColor = when {
        isIncome -> IncomeGreen
        isTransfer -> TransferBlue
        else -> ExpenseRed
    }

    val amountPrefix = when {
        isIncome -> "+"
        isTransfer -> ""
        else -> "-"
    }

    val iconBgColor = when {
        isIncome -> IncomeGreen.copy(alpha = 0.15f)
        isTransfer -> TransferBlue.copy(alpha = 0.15f)
        else -> ExpenseRed.copy(alpha = 0.12f)
    }

    val iconTint = when {
        isIncome -> IncomeGreen
        isTransfer -> TransferBlue
        else -> ExpenseRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = transaction.category,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isTransfer && transaction.destinationAccountName != null) {
                            "${transaction.accountName} → ${transaction.destinationAccountName}"
                        } else {
                            transaction.accountName
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = Formatters.formatDate(transaction.dateMillis),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${Formatters.formatCurrency(transaction.amount, currencySymbol)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

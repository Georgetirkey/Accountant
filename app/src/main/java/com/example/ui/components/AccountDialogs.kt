package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.ui.theme.TransferBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFundsDialog(
    accounts: List<AccountEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onTransfer: (fromAccountId: Long, fromAccountName: String, toAccountId: Long, toAccountName: String, amount: Double, note: String) -> Unit
) {
    var fromAccountId by remember { mutableLongStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var toAccountId by remember { mutableLongStateOf(accounts.getOrNull(1)?.id ?: (accounts.firstOrNull()?.id ?: 1L)) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val fromAccount = accounts.firstOrNull { it.id == fromAccountId }
    val toAccount = accounts.firstOrNull { it.id == toAccountId }
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = amount > 0 && fromAccount != null && toAccount != null && fromAccountId != toAccountId

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TransferBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transfer Funds", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // From Account
                Column {
                    Text("From Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accounts) { acc ->
                            FilterChip(
                                selected = fromAccountId == acc.id,
                                onClick = { fromAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // To Account
                Column {
                    Text("To Account", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accounts) { acc ->
                            FilterChip(
                                selected = toAccountId == acc.id,
                                onClick = { toAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                if (fromAccountId == toAccountId) {
                    Text("Source and destination accounts must be different.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Transfer Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Description (Optional)") },
                    placeholder = { Text("e.g. Savings deposit") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && fromAccount != null && toAccount != null) {
                        onTransfer(fromAccount.id, fromAccount.name, toAccount.id, toAccount.name, amount, noteText)
                        onDismiss()
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = TransferBlue)
            ) {
                Text("Complete Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddAccountDialog(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, initialBalance: Double, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("BANK") }
    var initialBalanceText by remember { mutableStateOf("") }
    val colorOptions = listOf("#006C4C", "#2563EB", "#7C3AED", "#EA580C", "#DC2626", "#0D9488", "#4B5563")
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    val types = listOf(
        "BANK" to "Bank Account",
        "CASH" to "Cash Wallet",
        "SAVINGS" to "Savings Vault",
        "CREDIT_CARD" to "Credit Card",
        "WALLET" to "Digital Wallet"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Account", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name *") },
                    placeholder = { Text("e.g. Chase Checking, Secret Vault") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column {
                    Text("Account Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(types) { (key, label) ->
                            FilterChip(
                                selected = selectedType == key,
                                onClick = { selectedType = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Initial / Starting Balance ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column {
                    Text("Color Tag", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { hex ->
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(hex))
                                    .clickable { selectedColor = hex }
                                    .then(
                                        if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val initBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                        onSave(name.trim(), selectedType, initBal, selectedColor)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text("Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

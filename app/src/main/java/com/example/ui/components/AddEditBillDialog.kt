package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.BillWithItems
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillStatus
import com.example.ui.theme.*
import com.example.utils.Formatters
import java.util.Calendar

data class MutableBillItem(
    var id: Long = 0,
    var description: String = "",
    var quantityText: String = "1",
    var unitPriceText: String = ""
) {
    val quantity: Double get() = quantityText.toDoubleOrNull() ?: 0.0
    val unitPrice: Double get() = unitPriceText.toDoubleOrNull() ?: 0.0
    val totalPrice: Double get() = quantity * unitPrice
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(
    currencySymbol: String,
    initialBillWithItems: BillWithItems? = null,
    onDismiss: () -> Unit,
    onSave: (bill: BillEntity, items: List<BillItemEntity>, autoRecordIncome: Boolean) -> Unit
) {
    val context = LocalContext.current
    val isEditing = initialBillWithItems != null

    var billNumber by remember {
        mutableStateOf(
            initialBillWithItems?.bill?.billNumber ?: "INV-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"
        )
    }

    var senderName by remember { mutableStateOf(initialBillWithItems?.bill?.senderName ?: "My Business / Studio") }
    var senderContact by remember { mutableStateOf(initialBillWithItems?.bill?.senderContact ?: "") }
    var senderAddress by remember { mutableStateOf(initialBillWithItems?.bill?.senderAddress ?: "") }

    var clientName by remember { mutableStateOf(initialBillWithItems?.bill?.clientName ?: "") }
    var clientContact by remember { mutableStateOf(initialBillWithItems?.bill?.clientContact ?: "") }
    var clientAddress by remember { mutableStateOf(initialBillWithItems?.bill?.clientAddress ?: "") }

    var issueDateMillis by remember { mutableLongStateOf(initialBillWithItems?.bill?.issueDateMillis ?: System.currentTimeMillis()) }
    var dueDateMillis by remember {
        mutableLongStateOf(
            initialBillWithItems?.bill?.dueDateMillis ?: (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)
        )
    }

    var taxRateText by remember { mutableStateOf(if (initialBillWithItems != null && initialBillWithItems.bill.taxRate > 0) initialBillWithItems.bill.taxRate.toString() else "") }
    var discountText by remember { mutableStateOf(if (initialBillWithItems != null && initialBillWithItems.bill.discount > 0) initialBillWithItems.bill.discount.toString() else "") }
    var notes by remember { mutableStateOf(initialBillWithItems?.bill?.notes ?: "Payment terms: Net 15 days. Thank you for your business!") }
    var status by remember { mutableStateOf(initialBillWithItems?.bill?.status ?: BillStatus.PENDING) }
    var autoRecordIncome by remember { mutableStateOf(false) }

    // Line items list
    val lineItems = remember {
        mutableStateListOf<MutableBillItem>().apply {
            if (initialBillWithItems != null && initialBillWithItems.items.isNotEmpty()) {
                addAll(
                    initialBillWithItems.items.map {
                        MutableBillItem(
                            id = it.id,
                            description = it.description,
                            quantityText = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString(),
                            unitPriceText = String.format("%.2f", it.unitPrice)
                        )
                    }
                )
            } else {
                add(MutableBillItem(description = "Service / Product Item", quantityText = "1", unitPriceText = "100.00"))
            }
        }
    }

    // Calculations
    val subtotal = lineItems.sumOf { it.totalPrice }
    val taxRate = taxRateText.toDoubleOrNull() ?: 0.0
    val taxAmount = subtotal * (taxRate / 100.0)
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val totalAmount = maxOf(0.0, subtotal + taxAmount - discount)

    // Issue Date Picker
    val issueCal = Calendar.getInstance().apply { timeInMillis = issueDateMillis }
    val issueDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val newCal = Calendar.getInstance().apply { set(y, m, d) }
            issueDateMillis = newCal.timeInMillis
        },
        issueCal.get(Calendar.YEAR),
        issueCal.get(Calendar.MONTH),
        issueCal.get(Calendar.DAY_OF_MONTH)
    )

    // Due Date Picker
    val dueCal = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
    val dueDatePicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val newCal = Calendar.getInstance().apply { set(y, m, d) }
            dueDateMillis = newCal.timeInMillis
        },
        dueCal.get(Calendar.YEAR),
        dueCal.get(Calendar.MONTH),
        dueCal.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                if (isEditing) "Edit Bill / Invoice" else "Generate Bill / Invoice",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    if (clientName.isNotBlank() && lineItems.any { it.description.isNotBlank() && it.totalPrice > 0 }) {
                                        val billEntity = BillEntity(
                                            id = initialBillWithItems?.bill?.id ?: 0L,
                                            billNumber = billNumber.trim(),
                                            issueDateMillis = issueDateMillis,
                                            dueDateMillis = dueDateMillis,
                                            senderName = senderName.trim().ifBlank { "My Business" },
                                            senderContact = senderContact.trim(),
                                            senderAddress = senderAddress.trim(),
                                            clientName = clientName.trim(),
                                            clientContact = clientContact.trim(),
                                            clientAddress = clientAddress.trim(),
                                            subtotal = subtotal,
                                            taxRate = taxRate,
                                            discount = discount,
                                            totalAmount = totalAmount,
                                            status = status,
                                            notes = notes.trim(),
                                            currencySymbol = currencySymbol
                                        )

                                        val entities = lineItems.filter { it.description.isNotBlank() }.map {
                                            BillItemEntity(
                                                id = it.id,
                                                billId = initialBillWithItems?.bill?.id ?: 0L,
                                                description = it.description.trim(),
                                                quantity = it.quantity,
                                                unitPrice = it.unitPrice,
                                                totalPrice = it.totalPrice
                                            )
                                        }

                                        onSave(billEntity, entities, autoRecordIncome)
                                        onDismiss()
                                    }
                                },
                                enabled = clientName.isNotBlank() && totalAmount > 0,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("save_bill_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Bill")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bill Number & Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = billNumber,
                            onValueChange = { billNumber = it },
                            label = { Text("Invoice / Bill #") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Status Dropdown
                        var expandedStatus by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { expandedStatus = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(status.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedStatus,
                                onDismissRequest = { expandedStatus = false }
                            ) {
                                BillStatus.values().forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st.name, fontWeight = if (st == status) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            status = st
                                            expandedStatus = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dates Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { issueDatePicker.show() }
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(10.dp)
                            ) {
                                Text("Issue Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatFullDate(issueDateMillis), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { dueDatePicker.show() }
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(10.dp)
                            ) {
                                Text("Due Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatFullDate(dueDateMillis), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Billed To (Client) Section
                    Text("Client / Billed To", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = { clientName = it },
                                label = { Text("Client / Customer Name *") },
                                placeholder = { Text("e.g. Acme Corp / Jane Smith") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("client_name_input"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = clientContact,
                                onValueChange = { clientContact = it },
                                label = { Text("Contact (Email / Phone)") },
                                placeholder = { Text("client@company.com | +1 555-0192") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = clientAddress,
                                onValueChange = { clientAddress = it },
                                label = { Text("Address / City (Optional)") },
                                placeholder = { Text("123 Business Way, New York") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sender (From) Section
                    var showSenderDetails by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSenderDetails = !showSenderDetails }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Billed By (Your Info)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Icon(
                            if (showSenderDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    if (showSenderDetails) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = senderName,
                                    onValueChange = { senderName = it },
                                    label = { Text("Your Business / Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = senderContact,
                                    onValueChange = { senderContact = it },
                                    label = { Text("Your Email / Phone") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = senderAddress,
                                    onValueChange = { senderAddress = it },
                                    label = { Text("Your Address / Tax ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Line Items Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Line Items (${lineItems.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        FilledTonalButton(
                            onClick = {
                                lineItems.add(MutableBillItem(description = "", quantityText = "1", unitPriceText = "0.00"))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Line Items Cards
                    lineItems.forEachIndexed { index, item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Item #${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    if (lineItems.size > 1) {
                                        IconButton(
                                            onClick = { lineItems.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = "Remove Item",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = item.description,
                                    onValueChange = { item.description = it },
                                    placeholder = { Text("Item Description (e.g. Design consulting)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = item.quantityText,
                                        onValueChange = { item.quantityText = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = item.unitPriceText,
                                        onValueChange = { item.unitPriceText = it },
                                        label = { Text("Price ($currencySymbol)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    Column(
                                        modifier = Modifier.weight(1.2f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            Formatters.formatCurrency(item.totalPrice, currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tax & Discount Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text("Tax Rate (%)") },
                            placeholder = { Text("e.g. 5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("Discount ($currencySymbol)") },
                            placeholder = { Text("e.g. 20") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatCurrency(subtotal, currencySymbol), fontWeight = FontWeight.Medium)
                            }
                            if (taxRate > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tax ($taxRate%)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+${Formatters.formatCurrency(taxAmount, currencySymbol)}", fontWeight = FontWeight.Medium)
                                }
                            }
                            if (discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discount", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-${Formatters.formatCurrency(discount, currencySymbol)}", color = ExpenseRed, fontWeight = FontWeight.Medium)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    Formatters.formatCurrency(totalAmount, currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Payment Terms / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2
                    )

                    // Auto-record Income checkbox if marked Paid
                    if (status == BillStatus.PAID) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { autoRecordIncome = !autoRecordIncome }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = autoRecordIncome,
                                onCheckedChange = { autoRecordIncome = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Auto-record this bill amount to Income transactions",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

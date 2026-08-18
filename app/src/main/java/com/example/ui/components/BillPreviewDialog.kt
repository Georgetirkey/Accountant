package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.BillWithItems
import com.example.data.model.BillStatus
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PendingAmber
import com.example.utils.Formatters
import com.example.utils.PdfInvoiceGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPreviewDialog(
    billWithItems: BillWithItems,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStatus: (BillStatus, Boolean) -> Unit
) {
    val context = LocalContext.current
    val bill = billWithItems.bill
    val items = billWithItems.items
    val sym = bill.currencySymbol.ifBlank { currencySymbol }

    var isGeneratingPdf by remember { mutableStateOf(false) }

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
                            Text("Invoice #${bill.billNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            // Quick Share PDF
                            IconButton(
                                onClick = {
                                    val pdf = PdfInvoiceGenerator.generateBillInvoicePdf(context, billWithItems, sym)
                                    if (pdf != null) {
                                        PdfInvoiceGenerator.sharePdf(context, pdf, "Share Invoice #${bill.billNumber}")
                                    } else {
                                        Toast.makeText(context, "Could not generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("share_pdf_icon_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Download / Save PDF
                            IconButton(
                                onClick = {
                                    val pdf = PdfInvoiceGenerator.generateBillInvoicePdf(context, billWithItems, sym)
                                    if (pdf != null) {
                                        PdfInvoiceGenerator.savePdfToDownloads(context, pdf, "Invoice_${bill.billNumber}")
                                    }
                                },
                                modifier = Modifier.testTag("save_pdf_icon_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Save PDF")
                            }

                            // Edit Button
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit Invoice")
                            }

                            // Delete Button
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Invoice", tint = ExpenseRed)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Primary Action: Share PDF
                                Button(
                                    onClick = {
                                        val pdf = PdfInvoiceGenerator.generateBillInvoicePdf(context, billWithItems, sym)
                                        if (pdf != null) {
                                            PdfInvoiceGenerator.sharePdf(context, pdf, "Share Invoice #${bill.billNumber}")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("share_pdf_main_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                // Save PDF Button
                                OutlinedButton(
                                    onClick = {
                                        val pdf = PdfInvoiceGenerator.generateBillInvoicePdf(context, billWithItems, sym)
                                        if (pdf != null) {
                                            PdfInvoiceGenerator.savePdfToDownloads(context, pdf, "Invoice_${bill.billNumber}")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("save_pdf_main_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save PDF", fontSize = 14.sp)
                                }
                            }

                            if (bill.status != BillStatus.PAID) {
                                Button(
                                    onClick = { onUpdateStatus(BillStatus.PAID, true) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("mark_as_paid_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mark as Paid & Record Income", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
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

                    // Realistic Paper Invoice Styling
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // PAID Watermark Stamp
                            if (bill.status == BillStatus.PAID) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 40.dp, end = 20.dp)
                                        .rotate(-15f)
                                        .border(3.dp, IncomeGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "PAID",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = IncomeGreen.copy(alpha = 0.4f),
                                        letterSpacing = 2.sp
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(20.dp)) {
                                // Top Invoice Title & Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "INVOICE",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF111827),
                                            letterSpacing = 1.5.sp
                                        )
                                        Text(
                                            text = "#${bill.billNumber}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF6B7280)
                                        )
                                    }
                                    StatusBadge(status = bill.status)
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                HorizontalDivider(color = Color(0xFFE5E7EB))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Date Info Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Issue Date", fontSize = 11.sp, color = Color(0xFF6B7280))
                                        Text(Formatters.formatFullDate(bill.issueDateMillis), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Due Date", fontSize = 11.sp, color = Color(0xFF6B7280))
                                        Text(Formatters.formatFullDate(bill.dueDateMillis), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Parties (From & Billed To)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("FROM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                                        Text(bill.senderName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937))
                                        if (bill.senderContact.isNotBlank()) {
                                            Text(bill.senderContact, fontSize = 12.sp, color = Color(0xFF4B5563))
                                        }
                                        if (bill.senderAddress.isNotBlank()) {
                                            Text(bill.senderAddress, fontSize = 12.sp, color = Color(0xFF4B5563))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text("BILLED TO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                                        Text(bill.clientName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1F2937), textAlign = TextAlign.End)
                                        if (bill.clientContact.isNotBlank()) {
                                            Text(bill.clientContact, fontSize = 12.sp, color = Color(0xFF4B5563), textAlign = TextAlign.End)
                                        }
                                        if (bill.clientAddress.isNotBlank()) {
                                            Text(bill.clientAddress, fontSize = 12.sp, color = Color(0xFF4B5563), textAlign = TextAlign.End)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Line Items Table
                                Surface(
                                    color = Color(0xFFF9FAFB),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Item Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563), modifier = Modifier.weight(2f))
                                        Text("Qty", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                        Text("Price", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                }

                                items.forEach { item ->
                                    val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.description, fontSize = 13.sp, color = Color(0xFF1F2937), modifier = Modifier.weight(2f), fontWeight = FontWeight.Medium)
                                        Text(qtyStr, fontSize = 12.sp, color = Color(0xFF4B5563), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                                        Text(Formatters.formatCurrency(item.unitPrice, sym), fontSize = 12.sp, color = Color(0xFF4B5563), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text(Formatters.formatCurrency(item.totalPrice, sym), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                    HorizontalDivider(color = Color(0xFFF3F4F6))
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Totals Breakdown
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Subtotal", fontSize = 13.sp, color = Color(0xFF6B7280))
                                        Text(Formatters.formatCurrency(bill.subtotal, sym), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                                    }
                                    if (bill.taxRate > 0) {
                                        val taxAmount = bill.subtotal * (bill.taxRate / 100.0)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tax (${bill.taxRate}%)", fontSize = 13.sp, color = Color(0xFF6B7280))
                                            Text("+${Formatters.formatCurrency(taxAmount, sym)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                                        }
                                    }
                                    if (bill.discount > 0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Discount", fontSize = 13.sp, color = Color(0xFF6B7280))
                                            Text("-${Formatters.formatCurrency(bill.discount, sym)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = Color(0xFF111827), thickness = 1.5.dp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("TOTAL DUE", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111827))
                                        Text(
                                            Formatters.formatCurrency(bill.totalAmount, sym),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF006C4C)
                                        )
                                    }
                                }

                                if (bill.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Surface(
                                        color = Color(0xFFF9FAFB),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Payment Terms / Notes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(bill.notes, fontSize = 12.sp, color = Color(0xFF374151))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Generated with Personal Accounting",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9CA3AF),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

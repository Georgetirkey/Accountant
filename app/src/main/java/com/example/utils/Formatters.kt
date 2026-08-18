package com.example.utils

import com.example.data.db.BillWithItems
import com.example.data.model.CategoryMeta
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val currencyFormat = DecimalFormat("#,##0.00")
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun formatCurrency(amount: Double, symbol: String = "₹"): String {
        return "$symbol${currencyFormat.format(amount)}"
    }

    fun formatAmount(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatTime(millis: Long): String {
        return timeFormat.format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        calendar.timeInMillis = millis

        return when {
            isSameDay(calendar, today) -> "Today"
            isSameDay(calendar, yesterday) -> "Yesterday"
            else -> dateFormat.format(Date(millis))
        }
    }

    fun formatFullDate(millis: Long): String {
        return dateFormat.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        return monthYearFormat.format(Date(millis))
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun generateBillShareText(billWithItems: BillWithItems): String {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val sym = bill.currencySymbol

        val sb = StringBuilder()
        sb.appendLine("═════════════════════════════════════")
        sb.appendLine("           INVOICE / BILL            ")
        sb.appendLine("═════════════════════════════════════")
        sb.appendLine("Invoice No : ${bill.billNumber}")
        sb.appendLine("Issue Date : ${formatFullDate(bill.issueDateMillis)}")
        sb.appendLine("Due Date   : ${formatFullDate(bill.dueDateMillis)}")
        sb.appendLine("Status     : ${bill.status.name}")
        sb.appendLine("─────────────────────────────────────")
        sb.appendLine("FROM:")
        sb.appendLine(bill.senderName)
        if (bill.senderContact.isNotBlank()) sb.appendLine(bill.senderContact)
        if (bill.senderAddress.isNotBlank()) sb.appendLine(bill.senderAddress)
        sb.appendLine("─────────────────────────────────────")
        sb.appendLine("BILLED TO:")
        sb.appendLine(bill.clientName)
        if (bill.clientContact.isNotBlank()) sb.appendLine(bill.clientContact)
        if (bill.clientAddress.isNotBlank()) sb.appendLine(bill.clientAddress)
        sb.appendLine("═════════════════════════════════════")
        sb.appendLine("ITEMIZED DETAILS:")
        items.forEachIndexed { index, item ->
            val qty = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            sb.appendLine("${index + 1}. ${item.description}")
            sb.appendLine("   Qty: $qty × $sym${currencyFormat.format(item.unitPrice)} = $sym${currencyFormat.format(item.totalPrice)}")
        }
        sb.appendLine("─────────────────────────────────────")
        sb.appendLine("Subtotal   : $sym${currencyFormat.format(bill.subtotal)}")
        if (bill.taxRate > 0) {
            val taxAmount = bill.subtotal * (bill.taxRate / 100.0)
            sb.appendLine("Tax (${bill.taxRate}%)  : +$sym${currencyFormat.format(taxAmount)}")
        }
        if (bill.discount > 0) {
            sb.appendLine("Discount   : -$sym${currencyFormat.format(bill.discount)}")
        }
        sb.appendLine("─────────────────────────────────────")
        sb.appendLine("TOTAL DUE  : $sym${currencyFormat.format(bill.totalAmount)}")
        sb.appendLine("═════════════════════════════════════")
        if (bill.notes.isNotBlank()) {
            sb.appendLine("Notes / Payment Terms:")
            sb.appendLine(bill.notes)
            sb.appendLine("═════════════════════════════════════")
        }
        sb.appendLine("Generated via Personal Accounting App")
        return sb.toString()
    }
}

object CategoriesData {
    val expenseCategories = listOf(
        CategoryMeta("Food & Dining", true, "restaurant", "#EF4444"),
        CategoryMeta("Groceries", true, "shopping_cart", "#F97316"),
        CategoryMeta("Bills & Utilities", true, "electric_bolt", "#F59E0B"),
        CategoryMeta("Shopping", true, "shopping_bag", "#EC4899"),
        CategoryMeta("Transport", true, "directions_car", "#3B82F6"),
        CategoryMeta("Rent / Housing", true, "home", "#8B5CF6"),
        CategoryMeta("Entertainment", true, "movie", "#6366F1"),
        CategoryMeta("Health & Medical", true, "medical_services", "#10B981"),
        CategoryMeta("Education", true, "school", "#14B8A6"),
        CategoryMeta("Travel", true, "flight", "#06B6D4"),
        CategoryMeta("Personal Care", true, "spa", "#D946EF"),
        CategoryMeta("Investment", true, "trending_up", "#059669"),
        CategoryMeta("Other Expense", true, "category", "#6B7280")
    )

    val incomeCategories = listOf(
        CategoryMeta("Salary", false, "payments", "#16A34A"),
        CategoryMeta("Freelance / Projects", false, "laptop_mac", "#0D9488"),
        CategoryMeta("Business", false, "store", "#2563EB"),
        CategoryMeta("Investments / Dividends", false, "show_chart", "#7C3AED"),
        CategoryMeta("Rental Income", false, "real_estate_agent", "#EA580C"),
        CategoryMeta("Bonus / Gift", false, "card_giftcard", "#E11D48"),
        CategoryMeta("Refunds", false, "assignment_return", "#0891B2"),
        CategoryMeta("Other Income", false, "account_balance_wallet", "#059669")
    )

    val allCurrencies = listOf(
        "₹" to "INR (₹) - Indian Rupee",
        "$" to "USD ($) - US Dollar",
        "€" to "EUR (€) - Euro",
        "£" to "GBP (£) - British Pound",
        "AED" to "AED (AED) - UAE Dirham",
        "SAR" to "SAR (SAR) - Saudi Riyal",
        "¥" to "JPY/CNY (¥)",
        "₱" to "PHP (₱)",
        "C$" to "CAD (C$)",
        "A$" to "AUD (A$)",
        "₩" to "KRW (₩)",
        "R$" to "BRL (R$)",
        "CHF" to "CHF (CHF)",
        "Rs" to "PKR/LKR (Rs)",
        "₦" to "NGN (₦)",
        "৳" to "BDT (৳)"
    )
}

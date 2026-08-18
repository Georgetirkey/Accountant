package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val accountId: Long,
    val accountName: String,
    val destinationAccountId: Long? = null,
    val destinationAccountName: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val billId: Long? = null
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // CASH, BANK, CREDIT_CARD, SAVINGS, WALLET
    val initialBalance: Double = 0.0,
    val iconName: String = "wallet",
    val colorHex: String = "#006C4C"
)

enum class BillStatus {
    DRAFT,
    PENDING,
    PAID,
    OVERDUE
}

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val issueDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000), // Default 7 days
    val senderName: String = "My Accounting",
    val senderContact: String = "",
    val senderAddress: String = "",
    val clientName: String,
    val clientContact: String = "",
    val clientAddress: String = "",
    val subtotal: Double = 0.0,
    val taxRate: Double = 0.0, // in percentage, e.g. 5.0 for 5%
    val discount: Double = 0.0, // flat discount amount
    val totalAmount: Double = 0.0,
    val status: BillStatus = BillStatus.PENDING,
    val notes: String = "Thank you for your business!",
    val currencySymbol: String = "₹"
)

@Entity(tableName = "bill_items")
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

data class CategoryMeta(
    val name: String,
    val isExpense: Boolean,
    val icon: String,
    val colorHex: String
)

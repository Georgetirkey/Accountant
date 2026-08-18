package com.example.data.repository

import com.example.data.db.AccountDao
import com.example.data.db.BillDao
import com.example.data.db.BillWithItems
import com.example.data.db.TransactionDao
import com.example.data.model.AccountEntity
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

class AccountingRepository(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val billDao: BillDao
) {
    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(start, end)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    // Accounts
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun insertAccount(account: AccountEntity): Long =
        accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) =
        accountDao.updateAccount(account)

    suspend fun deleteAccount(account: AccountEntity) =
        accountDao.deleteAccount(account)

    suspend fun deleteAccountById(id: Long) =
        accountDao.deleteAccountById(id)

    // Bills
    val allBills: Flow<List<BillEntity>> = billDao.getAllBills()
    val allBillsWithItems: Flow<List<BillWithItems>> = billDao.getAllBillsWithItems()

    fun getBillWithItemsById(id: Long): Flow<BillWithItems?> =
        billDao.getBillWithItemsById(id)

    suspend fun saveBillWithItems(bill: BillEntity, items: List<BillItemEntity>): Long =
        billDao.saveBillWithItems(bill, items)

    suspend fun updateBillWithItems(bill: BillEntity, items: List<BillItemEntity>) =
        billDao.updateBillWithItems(bill, items)

    suspend fun updateBillStatus(id: Long, status: BillStatus) =
        billDao.updateBillStatus(id, status)

    suspend fun deleteBillAndItems(billId: Long) =
        billDao.deleteBillAndItems(billId)
}

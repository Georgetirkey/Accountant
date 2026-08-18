package com.example.data.db

import androidx.room.*
import com.example.data.model.BillEntity
import com.example.data.model.BillItemEntity
import com.example.data.model.BillStatus
import kotlinx.coroutines.flow.Flow

data class BillWithItems(
    @Embedded val bill: BillEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val items: List<BillItemEntity>
)

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY issueDateMillis DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Transaction
    @Query("SELECT * FROM bills ORDER BY issueDateMillis DESC")
    fun getAllBillsWithItems(): Flow<List<BillWithItems>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    fun getBillWithItemsById(id: Long): Flow<BillWithItems?>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): BillEntity?

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getItemsForBill(billId: Long): List<BillItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Query("UPDATE bills SET status = :status WHERE id = :id")
    suspend fun updateBillStatus(id: Long, status: BillStatus)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteItemsForBill(billId: Long)

    @Transaction
    suspend fun saveBillWithItems(bill: BillEntity, items: List<BillItemEntity>): Long {
        val billId = insertBill(bill)
        deleteItemsForBill(billId)
        val itemsWithId = items.map { it.copy(billId = billId) }
        insertBillItems(itemsWithId)
        return billId
    }

    @Transaction
    suspend fun updateBillWithItems(bill: BillEntity, items: List<BillItemEntity>) {
        updateBill(bill)
        deleteItemsForBill(bill.id)
        val itemsWithId = items.map { it.copy(billId = bill.id) }
        insertBillItems(itemsWithId)
    }

    @Transaction
    suspend fun deleteBillAndItems(billId: Long) {
        deleteItemsForBill(billId)
        deleteBillById(billId)
    }
}

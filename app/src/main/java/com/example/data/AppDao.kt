package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.data.*

@Dao
interface AppDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients")
    suspend fun getAllClientsList(): List<Client>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>)

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders")
    suspend fun getAllOrdersList(): List<Order>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<Order>)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()

    @Query("DELETE FROM invoice_records")
    suspend fun deleteAllInvoiceRecords()

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, status: String)

    @Query("UPDATE orders SET status = :status, previousStage = :previousStage WHERE id = :orderId")
    suspend fun updateOrderStatusAndStage(orderId: Int, status: String, previousStage: String?)

    @Query("UPDATE orders SET advanceAmount = :advance WHERE id = :orderId")
    suspend fun updateOrderAdvance(orderId: Int, advance: Double)

    @Query("UPDATE orders SET packageCount = :count, quantityPerPacket = :perPacket, status = :status WHERE id = :orderId")
    suspend fun updateOrderPackaging(orderId: Int, count: Int, perPacket: Int, status: String)
    @Query("SELECT * FROM user WHERE id = 1")
    fun getUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceRecord(record: InvoiceRecord)

    @Query("SELECT * FROM invoice_records")
    suspend fun getAllInvoiceRecordsList(): List<InvoiceRecord>

    @Query("SELECT * FROM invoice_records ORDER BY id DESC")
    fun getAllInvoiceRecordsFlow(): Flow<List<InvoiceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceRecords(records: List<InvoiceRecord>)

    @Query("SELECT * FROM invoice_records WHERE clientId = :clientId ORDER BY id DESC")
    fun getInvoiceRecordsForClient(clientId: Int): Flow<List<InvoiceRecord>>

    @Query("SELECT * FROM registered_customers")
    suspend fun getAllRegisteredCustomersList(): List<RegisteredCustomer>

    @Query("SELECT * FROM registered_customers")
    fun getAllRegisteredCustomersFlow(): Flow<List<RegisteredCustomer>>

    @Query("SELECT * FROM registered_customers WHERE username = :username")
    suspend fun getRegisteredCustomerByUsername(username: String): RegisteredCustomer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisteredCustomer(customer: RegisteredCustomer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisteredCustomers(customers: List<RegisteredCustomer>)

    @Query("DELETE FROM registered_customers")
    suspend fun deleteAllRegisteredCustomers()

    @Query("SELECT * FROM management_users WHERE email = :email")
    suspend fun getManagementUserByEmail(email: String): ManagementUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManagementUser(user: ManagementUser)

    @Query("DELETE FROM management_users")
    suspend fun deleteAllManagementUsers()

    // Order deletion support
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): Order?

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Int)

    // System Activity Log support
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemActivityLog(log: SystemActivityLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSystemActivityLogSync(log: SystemActivityLog)

    @Query("SELECT * FROM system_activity_logs ORDER BY timestamp DESC")
    fun getAllSystemActivityLogsFlow(): Flow<List<SystemActivityLog>>

    @Query("DELETE FROM system_activity_logs")
    suspend fun deleteAllSystemActivityLogs()

    // Inventory operations
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllInventoryItemsFlow(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items")
    suspend fun getAllInventoryItemsList(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteInventoryItemById(itemId: Int)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllInventoryItems()

    // Price Estimate operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceEstimate(estimate: PriceEstimate)

    @Query("SELECT * FROM price_estimates ORDER BY timestamp DESC")
    fun getAllPriceEstimatesFlow(): Flow<List<PriceEstimate>>

    @Query("DELETE FROM price_estimates")
    suspend fun deleteAllPriceEstimates()
    
    @Query("UPDATE price_estimates SET orderStatus = :status WHERE id = :id")
    suspend fun updatePriceEstimateStatus(id: Int, status: String)

    @Query("SELECT * FROM paper_types ORDER BY name ASC")
    fun getAllPaperTypesFlow(): Flow<List<com.example.data.PaperType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaperType(paperType: com.example.data.PaperType)

    @Query("DELETE FROM paper_types WHERE id = :id")
    suspend fun deletePaperTypeById(id: Int)

    // Vendor operations
    @Query("SELECT * FROM vendors ORDER BY name ASC")
    fun getAllVendorsFlow(): Flow<List<Vendor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendor: Vendor): Long

    @Query("DELETE FROM vendors WHERE id = :id")
    suspend fun deleteVendorById(id: Int)

    @Query("SELECT * FROM vendor_bills ORDER BY timestamp DESC")
    fun getAllVendorBillsFlow(): Flow<List<VendorBill>>

    @Query("SELECT * FROM vendor_bills WHERE vendorId = :vendorId ORDER BY timestamp DESC")
    fun getVendorBillsByVendorFlow(vendorId: Int): Flow<List<VendorBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendorBill(bill: VendorBill)

    @Query("SELECT * FROM vendor_payments ORDER BY timestamp DESC")
    fun getAllVendorPaymentsFlow(): Flow<List<VendorPayment>>

    @Query("SELECT * FROM vendor_payments WHERE vendorId = :vendorId ORDER BY timestamp DESC")
    fun getVendorPaymentsByVendorFlow(vendorId: Int): Flow<List<VendorPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendorPayment(payment: VendorPayment)

    @Query("SELECT * FROM recurring_bills ORDER BY startDate DESC")
    fun getAllRecurringBillsFlow(): Flow<List<RecurringBill>>

    @Query("SELECT * FROM recurring_bills WHERE vendorId = :vendorId ORDER BY startDate DESC")
    fun getRecurringBillsByVendorFlow(vendorId: Int): Flow<List<RecurringBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBill(recurringBill: RecurringBill)
    
    @Update
    suspend fun updateRecurringBill(recurringBill: RecurringBill)

    @Query("DELETE FROM recurring_bills WHERE id = :id")
    suspend fun deleteRecurringBillById(id: Int)

    // Vendor Document operations
    @Query("SELECT * FROM vendor_documents WHERE vendorId = :vendorId ORDER BY uploadTimestamp DESC")
    fun getVendorDocumentsByVendorFlow(vendorId: Int): Flow<List<VendorDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendorDocument(document: VendorDocument)

    @Query("DELETE FROM vendor_documents WHERE id = :id")
    suspend fun deleteVendorDocumentById(id: Int)
}

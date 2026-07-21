package com.example.data

import kotlinx.coroutines.flow.Flow
import com.example.data.User

class AppRepository(private val appDao: AppDao) {
    val allClients: Flow<List<Client>> = appDao.getAllClients()
    val allOrders: Flow<List<Order>> = appDao.getAllOrders()
    val allInvoiceRecords: Flow<List<InvoiceRecord>> = appDao.getAllInvoiceRecordsFlow()
    val user: Flow<User?> = appDao.getUser()

    suspend fun getAllClients() = appDao.getAllClientsList()
    suspend fun getAllOrders() = appDao.getAllOrdersList()
    suspend fun getAllInvoiceRecords() = appDao.getAllInvoiceRecordsList()

    suspend fun clearAllData() {
        appDao.deleteAllClients()
        appDao.deleteAllOrders()
        appDao.deleteAllInvoiceRecords()
        appDao.deleteAllRegisteredCustomers()
        appDao.deleteAllManagementUsers()
        appDao.deleteAllInventoryItems()
        appDao.deleteAllPriceEstimates()
    }

    // Price Estimate operations
    val allPriceEstimates: Flow<List<PriceEstimate>> = appDao.getAllPriceEstimatesFlow()

    suspend fun insertPriceEstimate(estimate: PriceEstimate) {
        appDao.insertPriceEstimate(estimate)
    }

    suspend fun deleteAllPriceEstimates() {
        appDao.deleteAllPriceEstimates()
    }
    
    suspend fun updatePriceEstimateStatus(id: Int, status: String) {
        appDao.updatePriceEstimateStatus(id, status)
    }

    suspend fun insertClient(client: Client) {
        appDao.insertClient(client)
    }

    suspend fun insertClients(clients: List<Client>) {
        appDao.insertClients(clients)
    }

    suspend fun insertOrder(order: Order) {
        appDao.insertOrder(order)
    }

    suspend fun insertOrders(orders: List<Order>) {
        appDao.insertOrders(orders)
    }
    
    suspend fun insertInvoiceRecords(records: List<InvoiceRecord>) {
        appDao.insertInvoiceRecords(records)
    }
    
    suspend fun updateOrderStatus(orderId: Int, status: String) {
        appDao.updateOrderStatus(orderId, status)
    }

    suspend fun updateOrderStatusAndStage(orderId: Int, status: String, previousStage: String?) {
        appDao.updateOrderStatusAndStage(orderId, status, previousStage)
    }

    suspend fun updateOrderAdvance(orderId: Int, advance: Double) {
        appDao.updateOrderAdvance(orderId, advance)
    }

    suspend fun updateOrderPackaging(orderId: Int, count: Int, perPacket: Int, status: String) {
        appDao.updateOrderPackaging(orderId, count, perPacket, status)
    }

    suspend fun insertUser(user: User) {
        appDao.insertUser(user)
    }

    suspend fun insertInvoiceRecord(record: InvoiceRecord) {
        appDao.insertInvoiceRecord(record)
    }

    fun getInvoiceRecordsForClient(clientId: Int): Flow<List<InvoiceRecord>> {
        return appDao.getInvoiceRecordsForClient(clientId)
    }

    val allRegisteredCustomersFlow: Flow<List<RegisteredCustomer>> = appDao.getAllRegisteredCustomersFlow()

    suspend fun getAllRegisteredCustomers() = appDao.getAllRegisteredCustomersList()

    suspend fun getRegisteredCustomerByUsername(username: String) = appDao.getRegisteredCustomerByUsername(username)

    suspend fun insertRegisteredCustomer(customer: RegisteredCustomer) {
        appDao.insertRegisteredCustomer(customer)
    }

    suspend fun insertRegisteredCustomers(customers: List<RegisteredCustomer>) {
        appDao.insertRegisteredCustomers(customers)
    }

    suspend fun deleteAllRegisteredCustomers() {
        appDao.deleteAllRegisteredCustomers()
    }

    suspend fun getManagementUserByEmail(email: String): ManagementUser? {
        return appDao.getManagementUserByEmail(email)
    }

    suspend fun insertManagementUser(user: ManagementUser) {
        appDao.insertManagementUser(user)
    }

    suspend fun deleteAllManagementUsers() {
        appDao.deleteAllManagementUsers()
    }

    // Order operations
    suspend fun getOrderById(orderId: Int): Order? {
        return appDao.getOrderById(orderId)
    }

    suspend fun deleteOrderById(orderId: Int) {
        appDao.deleteOrderById(orderId)
    }

    // System Activity Log operations
    val allSystemActivityLogs: Flow<List<SystemActivityLog>> = appDao.getAllSystemActivityLogsFlow()

    suspend fun insertSystemActivityLog(log: SystemActivityLog) {
        appDao.insertSystemActivityLog(log)
    }

    suspend fun deleteAllSystemActivityLogs() {
        appDao.deleteAllSystemActivityLogs()
    }

    // Inventory operations
    val allInventoryItems: Flow<List<InventoryItem>> = appDao.getAllInventoryItemsFlow()

    suspend fun getAllInventoryItems() = appDao.getAllInventoryItemsList()

    suspend fun insertInventoryItem(item: InventoryItem) {
        appDao.insertInventoryItem(item)
    }

    suspend fun deleteInventoryItemById(itemId: Int) {
        appDao.deleteInventoryItemById(itemId)
    }

    suspend fun deleteAllInventoryItems() {
        appDao.deleteAllInventoryItems()
    }

    // Paper Type operations
    val allPaperTypes: Flow<List<PaperType>> = appDao.getAllPaperTypesFlow()

    suspend fun insertPaperType(paperType: PaperType) {
        appDao.insertPaperType(paperType)
    }

    suspend fun deletePaperTypeById(id: Int) {
        appDao.deletePaperTypeById(id)
    }

    // Vendor operations
    val allVendors: Flow<List<Vendor>> = appDao.getAllVendorsFlow()
    val allVendorBills: Flow<List<VendorBill>> = appDao.getAllVendorBillsFlow()
    val allVendorPayments: Flow<List<VendorPayment>> = appDao.getAllVendorPaymentsFlow()

    fun getVendorBillsByVendor(vendorId: Int) = appDao.getVendorBillsByVendorFlow(vendorId)
    fun getVendorPaymentsByVendor(vendorId: Int) = appDao.getVendorPaymentsByVendorFlow(vendorId)

    suspend fun insertVendor(vendor: Vendor) = appDao.insertVendor(vendor)
    suspend fun deleteVendorById(id: Int) = appDao.deleteVendorById(id)
    suspend fun insertVendorBill(bill: VendorBill) = appDao.insertVendorBill(bill)
    suspend fun insertVendorPayment(payment: VendorPayment) = appDao.insertVendorPayment(payment)

    // Recurring Bill operations
    val allRecurringBills: Flow<List<RecurringBill>> = appDao.getAllRecurringBillsFlow()
    fun getRecurringBillsByVendor(vendorId: Int) = appDao.getRecurringBillsByVendorFlow(vendorId)
    suspend fun insertRecurringBill(recurringBill: RecurringBill) = appDao.insertRecurringBill(recurringBill)
    suspend fun updateRecurringBill(recurringBill: RecurringBill) = appDao.updateRecurringBill(recurringBill)
    suspend fun deleteRecurringBillById(id: Int) = appDao.deleteRecurringBillById(id)

    // Vendor Document operations
    fun getVendorDocumentsByVendor(vendorId: Int) = appDao.getVendorDocumentsByVendorFlow(vendorId)
    suspend fun insertVendorDocument(document: VendorDocument) = appDao.insertVendorDocument(document)
    suspend fun deleteVendorDocumentById(id: Int) = appDao.deleteVendorDocumentById(id)
}

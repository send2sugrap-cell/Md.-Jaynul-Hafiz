package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.Client
import com.example.data.InvoiceRecord
import com.example.data.Order
import com.example.data.PriceEstimate
import com.example.data.User
import com.example.data.Vendor
import com.example.data.VendorBill
import com.example.data.VendorPayment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: AppRepository, private val settingsManager: com.example.data.SettingsManager) : ViewModel() {

    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val priceEstimates: StateFlow<List<PriceEstimate>> = repository.allPriceEstimates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allInvoices: StateFlow<List<InvoiceRecord>> = repository.allInvoiceRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertPriceEstimate(clientName: String, clientMobile: String, paperType: String, printSize: String, quantity: Int, totalPrice: Double) {
        viewModelScope.launch {
            repository.insertPriceEstimate(
                PriceEstimate(
                    clientName = clientName,
                    clientMobile = clientMobile,
                    paperType = paperType,
                    printSize = printSize,
                    quantity = quantity,
                    totalPrice = totalPrice
                )
            )
            logActivity("PRICE_ESTIMATE", "Created estimate for $clientName: $paperType, $printSize, x$quantity for ৳$totalPrice")
        }
    }

    fun deletePriceEstimateHistory() {
        viewModelScope.launch {
            repository.deleteAllPriceEstimates()
            logActivity("PRICE_ESTIMATE", "Cleared all price estimate history")
        }
    }
    
    fun updatePriceEstimateStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.updatePriceEstimateStatus(id, status)
            logActivity("PRICE_ESTIMATE", "Updated estimate #$id status to $status")
        }
    }

    init {
        viewModelScope.launch {
            try {
                if (repository.getManagementUserByEmail("admin@sucharu.com") == null) {
                    repository.insertManagementUser(
                        com.example.data.ManagementUser(
                            email = "admin@sucharu.com",
                            fullName = "System Admin",
                            mobileNumber = "01700000000",
                            passwordHash = "admin",
                            role = "admin"
                        )
                    )
                }
                if (repository.getManagementUserByEmail("staff@sucharu.com") == null) {
                    repository.insertManagementUser(
                        com.example.data.ManagementUser(
                            email = "staff@sucharu.com",
                            fullName = "Staff Member",
                            mobileNumber = "01700000001",
                            passwordHash = "staff",
                            role = "staff"
                        )
                    )
                }
                // Check if inventory items are empty, if so, populate with default materials
                if (repository.getAllInventoryItems().isEmpty()) {
                    repository.insertInventoryItem(com.example.data.InventoryItem(name = "A4 সাইজ আর্ট পেপার (A4 Paper)", quantity = 150, minThreshold = 200, unit = "Rim"))
                    repository.insertInventoryItem(com.example.data.InventoryItem(name = "প্রিমিয়াম গ্লসি পেপার (Glossy Photo Paper)", quantity = 25, minThreshold = 40, unit = "Packets"))
                    repository.insertInventoryItem(com.example.data.InventoryItem(name = "কালো কালির টোনার (Black Ink Toner)", quantity = 12, minThreshold = 5, unit = "Pcs"))
                    repository.insertInventoryItem(com.example.data.InventoryItem(name = "রঙিন কালির সেট (Colored Ink Set)", quantity = 2, minThreshold = 3, unit = "Sets"))
                    repository.insertInventoryItem(com.example.data.InventoryItem(name = "পিভিসি আইডি কার্ড স্টক (PVC Card Stock)", quantity = 500, minThreshold = 100, unit = "Pcs"))
                    repository.insertInventoryItem(com.example.data.InventoryItem(name = "লেমিনেটিং ফিল্ম রোলার (Lamination Film)", quantity = 3, minThreshold = 5, unit = "Rolls"))
                }
                // Seed Paper Types if empty
                if (repository.allPaperTypes.stateIn(viewModelScope).value.isEmpty()) {
                    repository.insertPaperType(com.example.data.PaperType(name = "Standard Copy", basePrice = 2.0, isDefault = true))
                    repository.insertPaperType(com.example.data.PaperType(name = "Glossy Photo", basePrice = 15.0, isDefault = true))
                    repository.insertPaperType(com.example.data.PaperType(name = "Matte Cardstock", basePrice = 10.0, isDefault = true))
                    repository.insertPaperType(com.example.data.PaperType(name = "Linen Textured", basePrice = 12.0, isDefault = true))
                    repository.insertPaperType(com.example.data.PaperType(name = "Art Card 300 GSM", basePrice = 25.0, isDefault = true))
                    repository.insertPaperType(com.example.data.PaperType(name = "Offset 80 GSM", basePrice = 5.0, isDefault = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(700)
            _isLoading.value = false
        }
    }

    private val _companyWhatsAppNumbers = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(settingsManager.getCompanyWhatsAppNumbers())
    val companyWhatsAppNumbers: StateFlow<List<String>> = _companyWhatsAppNumbers

    fun saveCompanyWhatsAppNumbers(numbers: List<String>) {
        settingsManager.saveCompanyWhatsAppNumbers(numbers)
        _companyWhatsAppNumbers.value = numbers
        logActivity("SETTINGS_UPDATE", "Updated Company WhatsApp numbers to: ${if (numbers.isEmpty()) "none" else numbers.joinToString(", ")}")
    }

    private val _whatsAppIdleOpacity = kotlinx.coroutines.flow.MutableStateFlow(settingsManager.getWhatsAppIdleOpacity())
    val whatsAppIdleOpacity: StateFlow<Float> = _whatsAppIdleOpacity

    fun saveWhatsAppIdleOpacity(opacity: Float) {
        settingsManager.saveWhatsAppIdleOpacity(opacity)
        _whatsAppIdleOpacity.value = opacity
        logActivity("SETTINGS_UPDATE", "Updated WhatsApp Button Idle Opacity to: ${(opacity * 100).toInt()}%")
    }

    private val _notification = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val notification: StateFlow<String?> = _notification

    fun showNotification(message: String) {
        viewModelScope.launch {
            _notification.value = message
            kotlinx.coroutines.delay(2500)
            if (_notification.value == message) {
                _notification.value = null
            }
        }
    }

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyRevenue: StateFlow<Map<String, Double>> = orders.map { ordersList ->
        ordersList.groupBy { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)) }
            .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    data class MonthlyFinance(val paid: Double, val due: Double)
    
    private val _showProjection = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showProjection: StateFlow<Boolean> = _showProjection

    val monthlyFinance: StateFlow<Map<String, MonthlyFinance>> = orders.map { ordersList ->
        ordersList.groupBy { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp)) }
            .mapValues { entry ->
                val paid = entry.value.sumOf { it.advanceAmount }
                val due = entry.value.sumOf { it.totalAmount - it.advanceAmount }
                MonthlyFinance(paid, due)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val projectedMonthlyFinance: StateFlow<Map<String, MonthlyFinance>> = monthlyFinance.map { financeMap ->
        if (financeMap.isEmpty()) return@map emptyMap<String, MonthlyFinance>()
        val sortedMonths = financeMap.keys.sorted()
        val lastMonth = sortedMonths.last()
        val lastFinance = financeMap[lastMonth] ?: MonthlyFinance(0.0, 0.0)
        
        // Simple projection: next month = last month + 5% increase
        val nextMonth = "2099-01" // Simple placeholder for next month string logic
        mapOf(lastMonth to lastFinance, nextMonth to MonthlyFinance(lastFinance.paid * 1.05, lastFinance.due * 1.05))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun toggleProjection() {
        _showProjection.value = !_showProjection.value
    }

    val totalPaid: StateFlow<Double> = orders.map { ordersList ->
        ordersList.sumOf { it.advanceAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalDue: StateFlow<Double> = orders.map { ordersList ->
        ordersList.sumOf { it.totalAmount - it.advanceAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeOrders: StateFlow<List<Order>> = orders.map { ordersList ->
        ordersList.filter { it.status != "Delivered" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topCategories: StateFlow<Map<String, Double>> = orders.map { ordersList ->
        ordersList.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentMonthRevenueTrends: StateFlow<List<com.patrykandpatrick.vico.core.entry.FloatEntry>> = orders.map { ordersList ->
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val maxDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        val dailyMap = ordersList.filter {
            val orderCal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
            orderCal.get(java.util.Calendar.MONTH) == currentMonth && orderCal.get(java.util.Calendar.YEAR) == currentYear
        }.groupBy {
            val orderCal = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
            orderCal.get(java.util.Calendar.DAY_OF_MONTH)
        }.mapValues { entry ->
            entry.value.sumOf { it.totalAmount }
        }

        (1..maxDay).map { day ->
            com.patrykandpatrick.vico.core.entry.FloatEntry(day.toFloat(), dailyMap[day]?.toFloat() ?: 0f)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orderVolumeByCategory: StateFlow<List<com.patrykandpatrick.vico.core.entry.FloatEntry>> = orders.map { ordersList ->
        val categories = ordersList.map { it.category }.distinct().sorted()
        categories.mapIndexed { index, category ->
            val count = ordersList.count { it.category == category }
            com.patrykandpatrick.vico.core.entry.FloatEntry(index.toFloat(), count.toFloat())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orderCategoryLabels: StateFlow<List<String>> = orders.map { ordersList ->
        ordersList.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profilePictureUri: StateFlow<String?> = repository.user
        .map { it?.profilePictureUri }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val currentUserEmail = kotlinx.coroutines.flow.MutableStateFlow<String?>(settingsManager.getSavedManagementEmail())

    val systemActivityLogs: StateFlow<List<com.example.data.SystemActivityLog>> = repository.allSystemActivityLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inventoryItems: StateFlow<List<com.example.data.InventoryItem>> = repository.allInventoryItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val paperTypes: StateFlow<List<com.example.data.PaperType>> = repository.allPaperTypes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val vendors: StateFlow<List<Vendor>> = repository.allVendors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val vendorBills: StateFlow<List<VendorBill>> = repository.allVendorBills
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val vendorPayments: StateFlow<List<VendorPayment>> = repository.allVendorPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recurringBills: StateFlow<List<com.example.data.RecurringBill>> = repository.allRecurringBills
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            checkAndGenerateRecurringBills()
        }
    }

    private suspend fun checkAndGenerateRecurringBills() {
        repository.allRecurringBills.collect { bills ->
            val currentTime = System.currentTimeMillis()
            bills.filter { it.isActive && it.nextGenerationDate <= currentTime }.forEach { recurringBill ->
                // Generate a new vendor bill
                val newBill = VendorBill(
                    vendorId = recurringBill.vendorId,
                    invoiceNo = "REC-${System.currentTimeMillis() / 1000}",
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(currentTime)),
                    description = recurringBill.description,
                    totalAmount = recurringBill.amount,
                    advancePaid = 0.0
                )
                repository.insertVendorBill(newBill)

                // Update next generation date
                val calendar = java.util.Calendar.getInstance()
                calendar.timeInMillis = recurringBill.nextGenerationDate
                when (recurringBill.interval) {
                    "Weekly" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                    "Monthly" -> calendar.add(java.util.Calendar.MONTH, 1)
                    "Yearly" -> calendar.add(java.util.Calendar.YEAR, 1)
                }
                repository.updateRecurringBill(recurringBill.copy(nextGenerationDate = calendar.timeInMillis))
                logActivity("VENDOR_BILL", "Auto-generated recurring bill for vendor ID: ${recurringBill.vendorId}")
            }
        }
    }

    private val _dashboardLayout = MutableStateFlow(
        settingsManager.getDashboardLayout() ?: com.example.data.DashboardLayout(
            widgets = listOf(
                com.example.data.DashboardWidget("SUMMARY", "Financial Summary", true, 1.0f, 12, 0),
                com.example.data.DashboardWidget("ORDER_STATS", "Order Statistics", true, 1.0f, 6, 1),
                com.example.data.DashboardWidget("PRICE", "Quick Price Estimator", true, 1.0f, 6, 2),
                com.example.data.DashboardWidget("INVENTORY", "Inventory Tracking", true, 1.0f, 12, 3),
                com.example.data.DashboardWidget("VENDOR", "Vendor & Ledger", true, 1.0f, 6, 4),
                com.example.data.DashboardWidget("STATUS_GRID", "Order Statuses", true, 1.0f, 12, 5)
            )
        )
    )
    val dashboardLayout: StateFlow<com.example.data.DashboardLayout> = _dashboardLayout.asStateFlow()

    private val _vendorDashboardLayout = MutableStateFlow(
        settingsManager.getDashboardLayout("vendor_layout")?.let { savedLayout ->
            if (savedLayout.widgets.none { it.id == "VENDOR_PERFORMANCE" }) {
                savedLayout.copy(widgets = savedLayout.widgets + com.example.data.DashboardWidget("VENDOR_PERFORMANCE", "Vendor Performance Overview", true, 1.0f, 12, 1))
            } else savedLayout
        } ?: com.example.data.DashboardLayout(
            widgets = listOf(
                com.example.data.DashboardWidget("VENDOR_SUMMARY", "Payable Summary", true, 1.0f, 12, 0),
                com.example.data.DashboardWidget("VENDOR_PERFORMANCE", "Vendor Performance Overview", true, 1.0f, 12, 1),
                com.example.data.DashboardWidget("VENDOR_LIST", "Vendors List", true, 1.0f, 12, 2)
            )
        )
    )
    val vendorDashboardLayout: StateFlow<com.example.data.DashboardLayout> = _vendorDashboardLayout.asStateFlow()

    fun updateVendorDashboardLayout(layout: com.example.data.DashboardLayout) {
        _vendorDashboardLayout.value = layout
        settingsManager.saveDashboardLayout(layout, "vendor_layout")
    }

    fun updateDashboardLayout(layout: com.example.data.DashboardLayout) {
        _dashboardLayout.value = layout
        settingsManager.saveDashboardLayout(layout)
    }

    fun resetDashboardLayout() {
        val defaultLayout = com.example.data.DashboardLayout(
            widgets = listOf(
                com.example.data.DashboardWidget("SUMMARY", "Financial Summary", true, 1.0f, 12, 0),
                com.example.data.DashboardWidget("ORDER_STATS", "Order Statistics", true, 1.0f, 6, 1),
                com.example.data.DashboardWidget("PRICE", "Quick Price Estimator", true, 1.0f, 6, 2),
                com.example.data.DashboardWidget("INVENTORY", "Inventory Tracking", true, 1.0f, 12, 3),
                com.example.data.DashboardWidget("VENDOR", "Vendor & Ledger", true, 1.0f, 6, 4),
                com.example.data.DashboardWidget("STATUS_GRID", "Order Statuses", true, 1.0f, 12, 5)
            )
        )
        updateDashboardLayout(defaultLayout)
    }

    fun getVendorBills(vendorId: Int): kotlinx.coroutines.flow.Flow<List<VendorBill>> = repository.getVendorBillsByVendor(vendorId)
    fun getVendorPayments(vendorId: Int): kotlinx.coroutines.flow.Flow<List<VendorPayment>> = repository.getVendorPaymentsByVendor(vendorId)
    fun getVendorDocuments(vendorId: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.VendorDocument>> = repository.getVendorDocumentsByVendor(vendorId)

    fun addVendorDocument(document: com.example.data.VendorDocument) {
        viewModelScope.launch {
            repository.insertVendorDocument(document)
            logActivity("VENDOR_DOCUMENT", "Added document '${document.title}' for vendor ID: ${document.vendorId}")
        }
    }

    fun deleteVendorDocument(id: Int) {
        viewModelScope.launch {
            repository.deleteVendorDocumentById(id)
            logActivity("VENDOR_DOCUMENT", "Deleted document ID: $id")
        }
    }

    fun addVendor(vendor: Vendor) {
        viewModelScope.launch {
            repository.insertVendor(vendor)
            logActivity("VENDOR_UPDATE", "Added/Updated vendor: ${vendor.name} (${vendor.category})")
        }
    }

    fun deleteVendor(id: Int, name: String) {
        viewModelScope.launch {
            repository.deleteVendorById(id)
            logActivity("VENDOR_UPDATE", "Deleted vendor: $name")
        }
    }

    fun addVendorBill(bill: VendorBill) {
        viewModelScope.launch {
            repository.insertVendorBill(bill)
            logActivity("VENDOR_BILL", "Added bill #${bill.invoiceNo} for vendor ID: ${bill.vendorId}, Amount: ৳${bill.totalAmount}")
        }
    }

    fun addVendorPayment(payment: VendorPayment) {
        viewModelScope.launch {
            repository.insertVendorPayment(payment)
            logActivity("VENDOR_PAYMENT", "Recorded payment of ৳${payment.amount} for vendor ID: ${payment.vendorId}")
        }
    }

    suspend fun addVendorAndGetId(vendor: Vendor): Int {
        val id = repository.insertVendor(vendor)
        logActivity("VENDOR_UPDATE", "Added/Updated vendor: ${vendor.name} (${vendor.category})")
        return id.toInt()
    }

    suspend fun addVendorBillSuspend(bill: VendorBill) {
        repository.insertVendorBill(bill)
        logActivity("VENDOR_BILL", "Added bill #${bill.invoiceNo} for vendor ID: ${bill.vendorId}, Amount: ৳${bill.totalAmount}")
    }

    suspend fun addVendorPaymentSuspend(payment: VendorPayment) {
        repository.insertVendorPayment(payment)
        logActivity("VENDOR_PAYMENT", "Recorded payment of ৳${payment.amount} for vendor ID: ${payment.vendorId}")
    }

    fun addRecurringBill(recurringBill: com.example.data.RecurringBill) {
        viewModelScope.launch {
            repository.insertRecurringBill(recurringBill)
            logActivity("RECURRING_BILL", "Added recurring bill for vendor ID: ${recurringBill.vendorId}")
        }
    }

    fun updateRecurringBill(recurringBill: com.example.data.RecurringBill) {
        viewModelScope.launch {
            repository.updateRecurringBill(recurringBill)
            logActivity("RECURRING_BILL", "Updated recurring bill ID: ${recurringBill.id}")
        }
    }

    fun deleteRecurringBill(id: Int) {
        viewModelScope.launch {
            repository.deleteRecurringBillById(id)
            logActivity("RECURRING_BILL", "Deleted recurring bill ID: $id")
        }
    }

    fun savePaperType(paperType: com.example.data.PaperType) {
        viewModelScope.launch {
            repository.insertPaperType(paperType)
            logActivity("PAPER_TYPE_UPDATE", "Added/Updated paper type: ${paperType.name}")
        }
    }

    fun deletePaperType(id: Int) {
        viewModelScope.launch {
            repository.deletePaperTypeById(id)
            logActivity("PAPER_TYPE_UPDATE", "Deleted paper type ID: $id")
        }
    }

    fun saveInventoryItem(item: com.example.data.InventoryItem) {
        viewModelScope.launch {
            repository.insertInventoryItem(item)
            if (item.quantity <= item.minThreshold) {
                logActivity(
                    "INVENTORY_ALERT",
                    "স্টক সতর্কতা: '${item.name}' এর পরিমাণ (${item.quantity} ${item.unit}) সর্বনিম্ন সীমা (${item.minThreshold} ${item.unit}) এর নিচে নেমে গেছে!"
                )
            } else {
                logActivity(
                    "INVENTORY_UPDATE",
                    "ইনভেন্টরি আপডেট: '${item.name}' এর স্টক ${item.quantity} ${item.unit} এ আপডেট করা হয়েছে।"
                )
            }
        }
    }

    fun deleteInventoryItem(itemId: Int, itemName: String) {
        viewModelScope.launch {
            repository.deleteInventoryItemById(itemId)
            logActivity("INVENTORY_UPDATE", "ইনভেন্টরি থেকে '${itemName}' মুছে ফেলা হয়েছে।")
        }
    }

    fun createRestockOrder(item: com.example.data.InventoryItem) {
        viewModelScope.launch {
            logActivity("RESTOCK_ORDER", "নতুন রিস্টক অর্ডার তৈরি করা হয়েছে: '${item.name}' (বর্তমান স্টক: ${item.quantity} ${item.unit}, সর্বনিম্ন সীমা: ${item.minThreshold} ${item.unit})")
        }
    }

    fun logActivity(actionType: String, details: String) {
        viewModelScope.launch {
            try {
                val actor = currentUserEmail.value ?: settingsManager.getSavedManagementEmail() ?: "System/Guest"
                repository.insertSystemActivityLog(
                    com.example.data.SystemActivityLog(
                        actionType = actionType,
                        actor = actor,
                        details = details
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSystemActivityLogs() {
        viewModelScope.launch {
            try {
                repository.deleteAllSystemActivityLogs()
                logActivity("SETTINGS_UPDATE", "Cleared all system activity logs")
                showNotification("লগসমূহ সফলভাবে মুছে ফেলা হয়েছে।")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProfilePicture(uri: String) {
        viewModelScope.launch {
            repository.insertUser(User(id = 1, profilePictureUri = uri))
        }
    }

    fun addClient(client: Client) {
        viewModelScope.launch {
            try {
                repository.insertClient(client)
                logActivity("CLIENT_CREATION", "নতুন কাস্টমার যোগ করা হয়েছে: '${client.name}' (মোবাইল: ${client.mobile})")
                showNotification("কাস্টমার সফলভাবে যোগ করা হয়েছে।")
            } catch (e: Exception) {
                e.printStackTrace()
                showNotification("কাস্টমার যোগ করতে সমস্যা হয়েছে: ${e.localizedMessage}")
            }
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            try {
                repository.insertClient(client) // REPLACE strategy handles update
                logActivity("CLIENT_UPDATE", "কাস্টমার আপডেট করা হয়েছে: '${client.name}'")
                showNotification("কাস্টমার সফলভাবে আপডেট করা হয়েছে।")
            } catch (e: Exception) {
                e.printStackTrace()
                showNotification("কাস্টমার আপডেট করতে সমস্যা হয়েছে: ${e.localizedMessage}")
            }
        }
    }

    fun addOrder(order: Order) {
        viewModelScope.launch {
            repository.insertOrder(order)
        }
    }

    fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                val order = repository.getOrderById(orderId)
                if (order != null) {
                    repository.deleteOrderById(orderId)
                    logActivity("ORDER_DELETION", "Deleted order #${order.id} for job: '${order.jobName}' (Client: ${order.clientName}, Amount: ৳${order.totalAmount})")
                    showNotification("অর্ডারটি সফলভাবে ডিলিট করা হয়েছে।")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showNotification("অর্ডার ডিলিট করতে সমস্যা হয়েছে।")
            }
        }
    }

    fun updateOrderStatus(orderId: Int, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
        }
    }

    fun updateOrderStatusAndStage(orderId: Int, status: String, previousStage: String?) {
        viewModelScope.launch {
            repository.updateOrderStatusAndStage(orderId, status, previousStage)
        }
    }

    fun updateOrderAdvance(orderId: Int, advance: Double) {
        viewModelScope.launch {
            repository.updateOrderAdvance(orderId, advance)
        }
    }

    fun updateOrderPackaging(orderId: Int, count: Int, perPacket: Int, status: String) {
        viewModelScope.launch {
            repository.updateOrderPackaging(orderId, count, perPacket, status)
        }
    }

    fun addInvoiceRecord(record: InvoiceRecord) {
        viewModelScope.launch {
            repository.insertInvoiceRecord(record)
        }
    }

    val getInvoiceRecordsForClient: (Int) -> kotlinx.coroutines.flow.Flow<List<InvoiceRecord>> = { clientId ->
        repository.getInvoiceRecordsForClient(clientId)
    }

    suspend fun backupData(): String {
        val clients = repository.getAllClients()
        val orders = repository.getAllOrders()
        val invoiceRecords = repository.getAllInvoiceRecords()
        val registeredCustomers = repository.getAllRegisteredCustomers()
        val inventoryItemsList = repository.getAllInventoryItems()
        
        val backupData = BackupData(clients, orders, invoiceRecords, registeredCustomers, inventoryItemsList)
        return kotlinx.serialization.json.Json.encodeToString(backupData)
    }

    suspend fun restoreData(jsonString: String): Boolean {
        return try {
            val backupData = kotlinx.serialization.json.Json.decodeFromString<BackupData>(jsonString)
            repository.clearAllData()
            repository.insertClients(backupData.clients)
            repository.insertOrders(backupData.orders)
            repository.insertInvoiceRecords(backupData.invoiceRecords)
            repository.insertRegisteredCustomers(backupData.registeredCustomers)
            backupData.inventoryItems.forEach {
                repository.insertInventoryItem(it)
            }
            logActivity("BACKUP_RESTORE", "Restored database from backup with ${backupData.clients.size} clients, ${backupData.orders.size} orders, ${backupData.registeredCustomers.size} customers, and ${backupData.inventoryItems.size} inventory items.")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun normalizePhone(phone: String): String {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return if (clean.startsWith("88")) clean.substring(2) else clean
    }

    suspend fun registerCustomer(
        fullName: String,
        mobile: String,
        customerNumber: String,
        password: String
    ): Result<String> {
        if (fullName.isBlank() || mobile.isBlank() || customerNumber.isBlank() || password.isBlank()) {
            return Result.failure(Exception("অনুগ্রহ করে সব তথ্য প্রদান করুন।"))
        }

        val allClients = repository.getAllClients()
        val normalizedInputMobile = normalizePhone(mobile)
        
        val matchingClients = allClients.filter { normalizePhone(it.mobile) == normalizedInputMobile }
        if (matchingClients.isEmpty()) {
            return Result.failure(Exception("মোবাইল নম্বরটি অর্ডারের সাথে মিলছে না।"))
        }

        val invoiceRecords = repository.getAllInvoiceRecords()
        val hasMatchingInvoice = invoiceRecords.any { invoice ->
            invoice.id.toString() == customerNumber.trim() && matchingClients.any { it.id == invoice.clientId }
        }
        val hasMatchingClientId = matchingClients.any { it.id.toString() == customerNumber.trim() }

        if (!hasMatchingInvoice && !hasMatchingClientId) {
            return Result.failure(Exception("কাস্টমার নম্বরটি সঠিক নয় অথবা মোবাইল নম্বরের সাথে মিলছে না।"))
        }

        val generatedUsername = "sg_${customerNumber.trim()}"
        val existingCustomer = repository.getRegisteredCustomerByUsername(generatedUsername)
        if (existingCustomer != null) {
            return Result.failure(Exception("এই কাস্টমার নম্বরটি দিয়ে ইতিপূর্বে রেজিস্ট্রেশন করা হয়েছে। ইউজারনেম: $generatedUsername"))
        }

        val newReg = com.example.data.RegisteredCustomer(
            fullName = fullName.trim(),
            mobile = matchingClients.first().mobile, // Use database mobile to be completely consistent
            customerNumber = customerNumber.trim(),
            username = generatedUsername,
            passwordHash = password
        )
        repository.insertRegisteredCustomer(newReg)

        return Result.success(generatedUsername)
    }

    suspend fun loginCustomer(username: String, password: String): Result<com.example.data.RegisteredCustomer> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(Exception("অনুগ্রহ করে ইউজারনেম এবং পাসওয়ার্ড দিন।"))
        }
        val matchedUser = repository.getRegisteredCustomerByUsername(username.trim())
        if (matchedUser == null) {
            return Result.failure(Exception("ইউজারনেমটি সঠিক নয়।"))
        }
        if (matchedUser.passwordHash != password) {
            return Result.failure(Exception("ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।"))
        }
        currentUserEmail.value = matchedUser.username
        logActivity("LOGIN", "Customer logged in: ${matchedUser.username} (${matchedUser.fullName})")
        return Result.success(matchedUser)
    }

    suspend fun registerManagement(
        fullName: String,
        email: String,
        mobileNumber: String,
        password: String,
        confirmPassword: String,
        secretMasterKey: String
    ): Result<com.example.data.ManagementUser> {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedMobile = mobileNumber.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirmPassword = confirmPassword.trim()
        val trimmedSecretKey = secretMasterKey.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || trimmedMobile.isBlank() || 
            trimmedPassword.isBlank() || trimmedConfirmPassword.isBlank() || trimmedSecretKey.isBlank()) {
            return Result.failure(Exception("অনুগ্রহ করে সব তথ্য প্রদান করুন।"))
        }

        if (!trimmedEmail.contains("@")) {
            return Result.failure(Exception("অনুগ্রহ করে একটি সঠিক ইমেইল প্রদান করুন।"))
        }

        if (trimmedPassword != trimmedConfirmPassword) {
            return Result.failure(Exception("পাসওয়ার্ড এবং নিশ্চিত পাসওয়ার্ড মেলেনি।"))
        }

        // Strong password criteria (at least 6 chars, containing both letters and numbers)
        if (trimmedPassword.length < 6 || !trimmedPassword.any { it.isDigit() } || !trimmedPassword.any { it.isLetter() }) {
            return Result.failure(Exception("পাসওয়ার্ডটি অবশ্যই অন্তত ৬ অক্ষরের হতে হবে এবং এতে অক্ষর ও সংখ্যা উভয়ই থাকতে হবে।"))
        }

        // Role verification based on secret master key
        val role = when (trimmedSecretKey) {
            "SUCHARU_ADMIN", "SUCHARU_ADMIN_2026", "admin" -> "admin"
            "SUCHARU_STAFF", "SUCHARU_STAFF_2026", "staff" -> "staff"
            else -> return Result.failure(Exception("ভুল সিক্রেট মাস্টার কি! রেজিস্ট্রেশন বাতিল করা হয়েছে।"))
        }

        val existingUser = repository.getManagementUserByEmail(trimmedEmail)
        if (existingUser != null) {
            return Result.failure(Exception("এই ইমেইল দিয়ে ইতিপূর্বে রেজিস্ট্রেশন করা হয়েছে।"))
        }

        val newUser = com.example.data.ManagementUser(
            email = trimmedEmail,
            fullName = trimmedName,
            mobileNumber = trimmedMobile,
            passwordHash = trimmedPassword,
            role = role
        )

        repository.insertManagementUser(newUser)
        return Result.success(newUser)
    }

    suspend fun loginManagement(
        email: String,
        password: String,
        rememberMe: Boolean
    ): Result<com.example.data.ManagementUser> {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            return Result.failure(Exception("ইমেইল বা পাসওয়ার্ড খালি হতে পারে না"))
        }

        val user = repository.getManagementUserByEmail(trimmedEmail)
        if (user == null || user.passwordHash != trimmedPassword) {
            return Result.failure(Exception("ইমেইল বা পাসওয়ার্ড ভুল হয়েছে"))
        }

        // If remember me is checked, save session
        if (rememberMe) {
            settingsManager.saveManagementSession(
                email = user.email,
                isAdmin = user.role == "admin",
                rememberMe = true
            )
        } else {
            // Unchecked: clear any existing saved remember me state
            settingsManager.clearManagementSession()
        }

        currentUserEmail.value = user.email
        logActivity("LOGIN", "Management user logged in: ${user.email} (${user.role.uppercase()})")

        return Result.success(user)
    }

    fun isRememberMeActive(): Boolean = settingsManager.isRememberMeActive()
    fun isSavedSessionAdmin(): Boolean = settingsManager.isSavedUserAdmin()
    fun getSavedSessionEmail(): String? = settingsManager.getSavedManagementEmail()
    fun clearSavedSession() {
        logActivity("LOGOUT", "User logged out")
        settingsManager.clearManagementSession()
        currentUserEmail.value = null
    }

    fun isOnboardingCompleted(): Boolean = settingsManager.isOnboardingCompleted()
    fun setOnboardingCompleted(completed: Boolean) = settingsManager.setOnboardingCompleted(completed)
    
    private val _themeName = kotlinx.coroutines.flow.MutableStateFlow(settingsManager.getThemeName())
    val themeName: StateFlow<String> = _themeName

    fun getThemeName(): String = _themeName.value
    fun saveThemeName(themeName: String) {
        settingsManager.saveThemeName(themeName)
        _themeName.value = themeName
    }

    fun resetToDefaultTheme() {
        val defaultTheme = "ROYAL_BLUE"
        settingsManager.saveThemeName(defaultTheme)
        _themeName.value = defaultTheme
    }

    @kotlinx.serialization.Serializable
    data class VendorBackupData(
        val vendors: List<Vendor>,
        val vendorBills: List<VendorBill>,
        val vendorPayments: List<VendorPayment>,
        val recurringBills: List<com.example.data.RecurringBill> = emptyList()
    )

    suspend fun generateVendorBackupData(): String {
        val backupData = VendorBackupData(
            vendors = vendors.value,
            vendorBills = vendorBills.value,
            vendorPayments = vendorPayments.value,
            recurringBills = recurringBills.value
        )
        return kotlinx.serialization.json.Json.encodeToString(backupData)
    }

    suspend fun restoreVendorData(jsonString: String): Boolean {
        return try {
            val backupData = kotlinx.serialization.json.Json.decodeFromString<VendorBackupData>(jsonString)
            backupData.vendors.forEach { repository.insertVendor(it) }
            backupData.vendorBills.forEach { repository.insertVendorBill(it) }
            backupData.vendorPayments.forEach { repository.insertVendorPayment(it) }
            backupData.recurringBills.forEach { repository.insertRecurringBill(it) }
            logActivity("VENDOR_BACKUP_RESTORE", "Restored vendor backup with ${backupData.vendors.size} vendors.")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class BackupData(
        val clients: List<Client>,
        val orders: List<Order>,
        val invoiceRecords: List<InvoiceRecord>,
        val registeredCustomers: List<com.example.data.RegisteredCustomer> = emptyList(),
        val inventoryItems: List<com.example.data.InventoryItem> = emptyList()
    )
}

class MainViewModelFactory(private val repository: AppRepository, private val settingsManager: com.example.data.SettingsManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

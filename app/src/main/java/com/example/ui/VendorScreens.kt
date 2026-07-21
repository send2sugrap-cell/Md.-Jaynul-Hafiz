package com.example.ui

import kotlinx.coroutines.launch
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

import com.example.util.VendorReportExporter
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VendorsScreen(viewModel: MainViewModel, onNavigateToLedger: (Int) -> Unit) {
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()
    val vendorPayments by viewModel.vendorPayments.collectAsStateWithLifecycle()
    val vendorBills by viewModel.vendorBills.collectAsStateWithLifecycle()
    val layout by viewModel.vendorDashboardLayout.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isCustomizing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreSuccess by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    
    val createDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val jsonData = viewModel.generateVendorBackupData()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonData.toByteArray())
                    }
                    android.widget.Toast.makeText(context, "Backup exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Failed to export backup", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val openDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isRestoring = true
                try {
                    val jsonString = context.contentResolver.openInputStream(it)?.use { inputStream ->
                        inputStream.bufferedReader().use { reader -> reader.readText() }
                    }
                    if (jsonString != null) {
                        restoreSuccess = viewModel.restoreVendorData(jsonString)
                        val message = if (restoreSuccess == true) "Backup restored successfully" else "Failed to restore backup"
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Failed to read backup file", android.widget.Toast.LENGTH_SHORT).show()
                } finally {
                    isRestoring = false
                }
            }
        }
    }

    val filteredVendors = vendors.filter {
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.companyName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendor Management") },
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Payment Report")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Vendor")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Customize Dashboard") },
                                onClick = { 
                                    isCustomizing = !isCustomizing
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(if (isCustomizing) Icons.Default.Check else Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Backup") },
                                onClick = {
                                    showMenu = false
                                    createDocumentLauncher.launch("sucharu_vendor_backup_${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())}.json")
                                },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Backup") },
                                onClick = {
                                    showMenu = false
                                    openDocumentLauncher.launch(arrayOf("application/json"))
                                },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Vendor")
            }
        }
    ) { padding ->
        FlowRow(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            maxItemsInEachRow = 12
        ) {
            layout.widgets.sortedBy { it.order }.forEachIndexed { index, widget ->
                CustomizableWidget(
                    widget = widget,
                    isCustomizing = isCustomizing,
                    onResizeVertical = { newMultiplier ->
                        val updatedWidgets = layout.widgets.map {
                            if (it.id == widget.id) it.copy(heightMultiplier = newMultiplier.coerceIn(0.2f, 3.0f)) else it
                        }
                        viewModel.updateVendorDashboardLayout(DashboardLayout(updatedWidgets))
                    },
                    onResizeHorizontal = { newSpan ->
                        val updatedWidgets = layout.widgets.map {
                            if (it.id == widget.id) it.copy(widthSpan = newSpan) else it
                        }
                        viewModel.updateVendorDashboardLayout(DashboardLayout(updatedWidgets))
                    },
                    onToggleVisibility = {
                        val updatedWidgets = layout.widgets.map {
                            if (it.id == widget.id) it.copy(isVisible = !it.isVisible) else it
                        }
                        viewModel.updateVendorDashboardLayout(DashboardLayout(updatedWidgets))
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val sorted = layout.widgets.sortedBy { it.order }
                            val prevWidget = sorted[index - 1]
                            val updatedWidgets = layout.widgets.map {
                                when (it.id) {
                                    widget.id -> it.copy(order = prevWidget.order)
                                    prevWidget.id -> it.copy(order = widget.order)
                                    else -> it
                                }
                            }
                            viewModel.updateVendorDashboardLayout(DashboardLayout(updatedWidgets))
                        }
                    },
                    onMoveDown = {
                        if (index < layout.widgets.size - 1) {
                            val sorted = layout.widgets.sortedBy { it.order }
                            val nextWidget = sorted[index + 1]
                            val updatedWidgets = layout.widgets.map {
                                when (it.id) {
                                    widget.id -> it.copy(order = nextWidget.order)
                                    nextWidget.id -> it.copy(order = widget.order)
                                    else -> it
                                }
                            }
                            viewModel.updateVendorDashboardLayout(DashboardLayout(updatedWidgets))
                        }
                    }
                ) {
                    when (widget.id) {
                        "VENDOR_PERFORMANCE" -> {
                            VendorPerformanceWidget(vendors = vendors, vendorBills = vendorBills)
                        }
                        "VENDOR_SUMMARY" -> {
                            val totalPayable = remember(vendors, vendorPayments, vendorBills) {
                                val totalPurchases = vendorBills.sumOf { it.totalAmount }
                                val totalPaid = vendorBills.sumOf { it.advancePaid } + vendorPayments.sumOf { it.amount }
                                totalPurchases - totalPaid
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Payable Balance", style = MaterialTheme.typography.labelMedium)
                                        Text("৳$totalPayable", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                        "VENDOR_LIST" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search Vendors...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (filteredVendors.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No vendors found.", style = MaterialTheme.typography.bodyLarge)
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        filteredVendors.forEach { vendor ->
                                            val vendorTotalPurchases = vendorBills.filter { it.vendorId == vendor.id }.sumOf { it.totalAmount }
                                            val vendorTotalPaid = vendorBills.filter { it.vendorId == vendor.id }.sumOf { it.advancePaid } + vendorPayments.filter { it.vendorId == vendor.id }.sumOf { it.amount }
                                            val vendorDue = vendorTotalPurchases - vendorTotalPaid
                                            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                                            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                            val monthlyExpenditure = vendorBills.filter { bill ->
                                                if (bill.vendorId == vendor.id) {
                                                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = bill.timestamp }
                                                    cal.get(java.util.Calendar.MONTH) == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear
                                                } else false
                                            }.sumOf { it.totalAmount }
                                            
                                            VendorCard(
                                                vendor = vendor, 
                                                dueAmount = vendorDue,
                                                monthlyExpenditure = monthlyExpenditure, onClick = { onNavigateToLedger(vendor.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVendorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { vendor ->
                viewModel.addVendor(vendor)
                showAddDialog = false
            }
        )
    }

    if (showReportDialog) {
        PaymentReportDialog(
            onDismiss = { showReportDialog = false },
            onExport = { start, end ->
                val filteredPayments = vendorPayments.filter { it.date in start..end }
                VendorReportExporter.exportPaymentReport(context, filteredPayments, vendors, start, end)
                showReportDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReportDialog(onDismiss: () -> Unit, onExport: (String, String) -> Unit) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var startDate by remember { mutableStateOf(sdf.format(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))) } // Default 30 days ago
    var endDate by remember { mutableStateOf(sdf.format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Payment Report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select date range to filter payments:", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onExport(startDate, endDate) }) {
                Text("Export PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VendorCard(vendor: Vendor, dueAmount: Double, monthlyExpenditure: Double = 0.0, onClick: () -> Unit) {
    val isHighDue = vendor.creditLimit > 0 && dueAmount > vendor.creditLimit
    val isNearBudget = vendor.budgetLimit > 0 && monthlyExpenditure >= vendor.budgetLimit * 0.9
    val isOverBudget = vendor.budgetLimit > 0 && monthlyExpenditure > vendor.budgetLimit

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (isHighDue || isOverBudget) MaterialTheme.colorScheme.errorContainer else if (isNearBudget) MaterialTheme.colorScheme.errorContainer.copy(alpha=0.5f) else MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isHighDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vendor.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isHighDue) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vendor.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (isHighDue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = vendor.companyName, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = if (isHighDue) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = vendor.category, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isHighDue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isHighDue) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = if (isHighDue) "High Dues" else "Healthy",
                        tint = if (isHighDue) MaterialTheme.colorScheme.error else Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHighDue) "High Dues" else "Healthy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHighDue) MaterialTheme.colorScheme.error else Color(0xFF22C55E)
                    )
                }
                
                if (isOverBudget) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Over Budget", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Over Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                } else if (isNearBudget) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.WarningAmber, contentDescription = "Near Budget", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Near Budget", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B))
                    }
                }
                
                Text(
                    text = "৳$dueAmount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVendorDialog(onDismiss: () -> Unit, onConfirm: (Vendor) -> Unit) {
    var name by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Paper Supplier") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var creditLimitStr by remember { mutableStateOf("") }
    var budgetLimitStr by remember { mutableStateOf("") }

    val categories = listOf(
        "Paper Supplier", "Binding Vendor", "Lamination Service", 
        "Plate/CTP Vendor", "Ink/Chemical Supplier", "Transport/Courier", "Others"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Vendor") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Vendor Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = creditLimitStr,
                    onValueChange = { creditLimitStr = it },
                    label = { Text("Credit Limit (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = budgetLimitStr,
                    onValueChange = { budgetLimitStr = it },
                    label = { Text("Monthly Budget Limit (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    category = selectionOption
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (name.isNotBlank()) {
                    val creditLimit = creditLimitStr.toDoubleOrNull() ?: 0.0
                    val budgetLimit = budgetLimitStr.toDoubleOrNull() ?: 0.0
                    onConfirm(Vendor(name = name, companyName = companyName, phone = phone, address = address, category = category, creditLimit = creditLimit, budgetLimit = budgetLimit))
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVendorDialog(vendor: Vendor, onDismiss: () -> Unit, onConfirm: (Vendor) -> Unit) {
    var name by remember { mutableStateOf(vendor.name) }
    var companyName by remember { mutableStateOf(vendor.companyName) }
    var phone by remember { mutableStateOf(vendor.phone) }
    var address by remember { mutableStateOf(vendor.address) }
    var category by remember { mutableStateOf(vendor.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var creditLimitStr by remember { mutableStateOf(if (vendor.creditLimit > 0) vendor.creditLimit.toString() else "") }
    var budgetLimitStr by remember { mutableStateOf(if (vendor.budgetLimit > 0) vendor.budgetLimit.toString() else "") }

    val categories = listOf(
        "Paper Supplier", "Binding Vendor", "Lamination Service", 
        "Plate/CTP Vendor", "Ink/Chemical Supplier", "Transport/Courier", "Others"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Vendor") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Vendor Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = creditLimitStr,
                    onValueChange = { creditLimitStr = it },
                    label = { Text("Credit Limit (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = budgetLimitStr,
                    onValueChange = { budgetLimitStr = it },
                    label = { Text("Monthly Budget Limit (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    category = selectionOption
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (name.isNotBlank()) {
                    val creditLimit = creditLimitStr.toDoubleOrNull() ?: 0.0
                    val budgetLimit = budgetLimitStr.toDoubleOrNull() ?: 0.0
                    onConfirm(vendor.copy(name = name, companyName = companyName, phone = phone, address = address, category = category, creditLimit = creditLimit, budgetLimit = budgetLimit))
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorLedgerScreen(vendorId: Int, viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()
    val vendor = vendors.find { it.id == vendorId } ?: return

    val bills by viewModel.getVendorBills(vendorId).collectAsState(initial = emptyList())
    val payments by viewModel.getVendorPayments(vendorId).collectAsState(initial = emptyList())

    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }

    var showEditVendorDialog by remember { mutableStateOf(false) }
    var showRecurringBillsDialog by remember { mutableStateOf(false) }
    var showDocumentsDialog by remember { mutableStateOf(false) }

    val totalPurchases = bills.sumOf { it.totalAmount }
    val totalPaid = bills.sumOf { it.advancePaid } + payments.sumOf { it.amount }
    val balanceDue = totalPurchases - totalPaid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(vendor.name, style = MaterialTheme.typography.titleMedium)
                        Text(vendor.companyName, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDocumentsDialog = true }) {
                        Icon(Icons.Default.Folder, contentDescription = "Vendor Documents")
                    }
                    IconButton(onClick = { showRecurringBillsDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recurring Bills")
                    }
                    IconButton(onClick = { showEditVendorDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Vendor")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showDocumentsDialog) {
                VendorDocumentsDialog(
                    vendorId = vendorId,
                    viewModel = viewModel,
                    onDismiss = { showDocumentsDialog = false }
                )
            }
            if (showRecurringBillsDialog) {
                RecurringBillsDialog(
                    vendorId = vendorId,
                    viewModel = viewModel,
                    onDismiss = { showRecurringBillsDialog = false }
                )
            }
            if (showEditVendorDialog) {
                EditVendorDialog(
                    vendor = vendor,
                    onDismiss = { showEditVendorDialog = false },
                    onConfirm = { updatedVendor ->
                        viewModel.addVendor(updatedVendor)
                        showEditVendorDialog = false
                    }
                )
            }
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Financial Summary", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryItem("Total Purchases", "৳$totalPurchases", MaterialTheme.colorScheme.onPrimaryContainer)
                        SummaryItem("Total Paid", "৳$totalPaid", MaterialTheme.colorScheme.onPrimaryContainer)
                        SummaryItem("Balance Due", "৳$balanceDue", if (balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Actions
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddBillDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Bill")
                }
                Button(
                    onClick = { showAddPaymentDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Payment")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Transaction Ledger", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))

            // Combine bills and payments for timeline
            val transactions = (bills.map { it to "BILL" } + payments.map { it to "PAYMENT" })
                .sortedByDescending { 
                    when (val item = it.first) {
                        is VendorBill -> item.timestamp
                        is VendorPayment -> item.timestamp
                        else -> 0L
                    }
                }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions) { (item, type) ->
                    if (type == "BILL") {
                        val bill = item as VendorBill
                        TransactionItem(
                            title = "Bill: ${bill.invoiceNo}",
                            subtitle = bill.description,
                            amount = bill.totalAmount,
                            date = bill.date,
                            isDebit = true,
                            icon = Icons.Default.Description
                        )
                    } else {
                        val payment = item as VendorPayment
                        TransactionItem(
                            title = "Payment: ${payment.paymentMethod}",
                            subtitle = payment.notes.ifBlank { "MR No: ${payment.mrNo ?: "N/A"}" },
                            amount = payment.amount,
                            date = payment.date,
                            isDebit = false,
                            icon = Icons.Default.AccountBalanceWallet
                        )
                    }
                }
            }
        }
    }

    if (showAddBillDialog) {
        val orders by viewModel.orders.collectAsStateWithLifecycle()
        AddVendorBillDialog(
            vendorId = vendorId,
            orders = orders,
            onDismiss = { showAddBillDialog = false },
            onConfirm = { bill ->
                viewModel.addVendorBill(bill)
                showAddBillDialog = false
            }
        )
    }

    if (showAddPaymentDialog) {
        AddVendorPaymentDialog(
            vendorId = vendorId,
            onDismiss = { showAddPaymentDialog = false },
            onConfirm = { payment ->
                viewModel.addVendorPayment(payment)
                showAddPaymentDialog = false
            }
        )
    }
}

@Composable
fun SummaryItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun TransactionItem(title: String, subtitle: String, amount: Double, date: String, isDebit: Boolean, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(date, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = "${if (isDebit) "+" else "-"}৳$amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVendorBillDialog(vendorId: Int, orders: List<Order>, onDismiss: () -> Unit, onConfirm: (VendorBill) -> Unit) {
    var invoiceNo by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var description by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var advancePaid by remember { mutableStateOf("") }
    
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var orderExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Purchase Bill") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = invoiceNo, onValueChange = { invoiceNo = it }, label = { Text("Bill/Invoice No.") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(
                    expanded = orderExpanded,
                    onExpandedChange = { orderExpanded = !orderExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedOrder?.let { "Job #${it.id}: ${it.jobName}" } ?: "Not Linked",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Associated Job/Order") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = orderExpanded,
                        onDismissRequest = { orderExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { selectedOrder = null; orderExpanded = false })
                        orders.forEach { order ->
                            DropdownMenuItem(
                                text = { Text("Job #${order.id}: ${order.jobName}") },
                                onClick = { selectedOrder = order; orderExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = totalAmount, onValueChange = { totalAmount = it }, label = { Text("Total Bill Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = advancePaid, onValueChange = { advancePaid = it }, label = { Text("Advance Paid") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { 
                val amount = totalAmount.toDoubleOrNull() ?: 0.0
                val adv = advancePaid.toDoubleOrNull() ?: 0.0
                if (invoiceNo.isNotBlank() && amount > 0) {
                    onConfirm(VendorBill(
                        vendorId = vendorId,
                        invoiceNo = invoiceNo,
                        date = date,
                        orderId = selectedOrder?.id,
                        description = description,
                        totalAmount = amount,
                        advancePaid = adv
                    ))
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVendorPaymentDialog(vendorId: Int, onDismiss: () -> Unit, onConfirm: (VendorPayment) -> Unit) {
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var mrNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var methodExpanded by remember { mutableStateOf(false) }
    val methods = listOf("Cash", "Bank Transfer", "Cheque", "Mobile Banking")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount Paid") }, modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = !methodExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        methods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = { paymentMethod = method; methodExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = mrNo, onValueChange = { mrNo = it }, label = { Text("Vendor MR No. (Optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes/Remarks") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { 
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onConfirm(VendorPayment(
                        vendorId = vendorId,
                        date = date,
                        amount = amt,
                        paymentMethod = paymentMethod,
                        mrNo = mrNo.ifBlank { null },
                        notes = notes
                    ))
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsDialog(vendorId: Int, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val recurringBills by viewModel.recurringBills.collectAsStateWithLifecycle()
    val vendorRecurringBills = recurringBills.filter { it.vendorId == vendorId }
    var showAddDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recurring Bills", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (vendorRecurringBills.isEmpty()) {
                    Text("No recurring bills set up.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(vendorRecurringBills) { recurringBill ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(recurringBill.description, style = MaterialTheme.typography.titleMedium)
                                        Text("৳${recurringBill.amount} / ${recurringBill.interval}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        val nextDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(recurringBill.nextGenerationDate))
                                        Text("Next: $nextDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        Switch(
                                            checked = recurringBill.isActive,
                                            onCheckedChange = { isActive ->
                                                viewModel.updateRecurringBill(recurringBill.copy(isActive = isActive))
                                            }
                                        )
                                        IconButton(onClick = { viewModel.deleteRecurringBill(recurringBill.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Recurring Bill")
                }
            }
        }
    }

    if (showAddDialog) {
        var description by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var interval by remember { mutableStateOf("Monthly") }
        var intervalExpanded by remember { mutableStateOf(false) }
        val intervals = listOf("Weekly", "Monthly", "Yearly")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Recurring Bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (e.g. Rent, Electricity)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (৳)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    ExposedDropdownMenuBox(
                        expanded = intervalExpanded,
                        onExpandedChange = { intervalExpanded = !intervalExpanded }
                    ) {
                        OutlinedTextField(
                            value = interval,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Interval") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = intervalExpanded,
                            onDismissRequest = { intervalExpanded = false }
                        ) {
                            intervals.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        interval = selectionOption
                                        intervalExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (description.isNotBlank() && amountStr.isNotBlank()) {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            val calendar = Calendar.getInstance()
                            when (interval) {
                                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                                "Yearly" -> calendar.add(Calendar.YEAR, 1)
                            }
                            viewModel.addRecurringBill(
                                RecurringBill(
                                    vendorId = vendorId,
                                    description = description,
                                    amount = amount,
                                    interval = interval,
                                    nextGenerationDate = calendar.timeInMillis
                                )
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun VendorPerformanceWidget(vendors: List<Vendor>, vendorBills: List<VendorBill>) {
    val chartData = remember(vendors, vendorBills) {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, -6)
        val sixMonthsAgo = calendar.timeInMillis

        val recentBills = vendorBills.filter { it.timestamp >= sixMonthsAgo }

        val categorySpending = mutableMapOf<String, Double>()
        for (bill in recentBills) {
            val vendor = vendors.find { it.id == bill.vendorId }
            if (vendor != null) {
                categorySpending[vendor.category] = (categorySpending[vendor.category] ?: 0.0) + bill.totalAmount
            }
        }

        categorySpending.toList().sortedByDescending { it.second }.take(6)
    }

    if (chartData.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No spending data in the last 6 months.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val entryList = chartData.mapIndexed { index, pair -> FloatEntry(index.toFloat(), pair.second.toFloat()) }
    val model = entryModelOf(entryList)
    val categoryLabels = chartData.map { it.first }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("6-Month Spending by Category", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))
        Chart(
            chart = columnChart(
                columns = listOf(
                    lineComponent(
                        color = MaterialTheme.colorScheme.primary,
                        thickness = 16.dp
                    )
                )
            ),
            model = model,
            startAxis = rememberStartAxis(
                valueFormatter = { value, _ -> "৳${value.toInt()}" }
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = { value, _ -> categoryLabels.getOrNull(value.toInt()) ?: "" }
            ),
            modifier = Modifier.height(200.dp).fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDocumentsDialog(vendorId: Int, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val documents by viewModel.getVendorDocuments(vendorId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Receipt", "Contract", "Utility Bill", "Other")

    val filteredDocuments = if (selectedCategory == "All") documents else documents.filter { it.category == selectedCategory }

    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vendor Documents", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredDocuments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No documents found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredDocuments) { document ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.data = android.net.Uri.parse(document.filePath)
                                    intent.flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = when (document.category) {
                                                "Receipt" -> Icons.Default.Receipt
                                                "Contract" -> Icons.Default.Description
                                                "Utility Bill" -> Icons.Default.ElectricBolt
                                                else -> Icons.Default.InsertDriveFile
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(document.title, style = MaterialTheme.typography.titleMedium)
                                            Text(document.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val uploadDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(document.uploadTimestamp))
                                            Text("Uploaded: $uploadDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteVendorDocument(document.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload Document")
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Receipt") }
        var categoryExpanded by remember { mutableStateOf(false) }
        var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
        val selectableCategories = listOf("Receipt", "Contract", "Utility Bill", "Other")

        val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                selectedUri = it
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Upload Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            selectableCategories.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        category = selectionOption
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedUri == null) "Select File" else "File Selected")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && selectedUri != null) {
                            viewModel.addVendorDocument(
                                com.example.data.VendorDocument(
                                    vendorId = vendorId,
                                    title = title,
                                    category = category,
                                    filePath = selectedUri.toString()
                                )
                            )
                            showAddDialog = false
                        } else {
                            android.widget.Toast.makeText(context, "Please enter title and select a file.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

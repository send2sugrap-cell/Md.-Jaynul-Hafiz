package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ErrorOutline
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.data.Order
import com.example.data.Vendor
import com.example.data.VendorBill
import com.example.data.VendorPayment

val pipelineStages = listOf(
    "New Job",
    "Design Processing",
    "Output",
    "Printing",
    "Mat Lamination",
    "Spot UV",
    "Die Cutting",
    "Pasting",
    "Binding",
    "Packaging",
    "Stock",
    "Shipping",
    "Delivered"
)

@Composable
fun OrdersScreen(viewModel: MainViewModel, initialStage: String? = null, onNavigateToClient: (Int) -> Unit) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var orderToPackage by remember { mutableStateOf<Order?>(null) }
    val isAdmin = remember { viewModel.isSavedSessionAdmin() }
    var orderToDelete by remember { mutableStateOf<Order?>(null) }
    var showInvoiceDialogForClient by remember { mutableStateOf<com.example.data.Client?>(null) }
    val clients by viewModel.clients.collectAsState()
    val vendors by viewModel.vendors.collectAsState()
    var orderToTransfer by remember { mutableStateOf<Order?>(null) }
    var pendingNextStage by remember { mutableStateOf<String?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredOrders = remember(orders, searchQuery, selectedFilter) {
        orders.filter { order ->
            val matchesSearch = order.jobName.contains(searchQuery, ignoreCase = true) ||
                    order.clientName.contains(searchQuery, ignoreCase = true) ||
                    order.category.contains(searchQuery, ignoreCase = true) ||
                    order.id.toString() == searchQuery.trim()

            val matchesFilter = when (selectedFilter) {
                "Pending Order" -> order.status != "Delivered" && order.status != "Hold"
                "In Printing" -> order.status == "Printing"
                "Ready for Delivery" -> order.status == "Stock" || order.status == "Shipping"
                "Due Unpaid" -> (order.totalAmount - order.advanceAmount) > 0
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = if (initialStage == "Active") "Active Jobs" else "Production Pipeline",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Live Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("সার্চ করুন (Job, Customer, ID, Category...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Status Badges/Filters
        val filters = listOf(
            "All" to "সব কাজ",
            "Pending Order" to "চলতি অর্ডার",
            "In Printing" to "প্রিন্টিং",
            "Ready for Delivery" to "প্রস্তুত কাজ",
            "Due Unpaid" to "বকেয়া রয়েছে"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            items(filters) { (key, label) ->
                val isSelected = selectedFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        
        if (isLoading) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(4) {
                    SkeletonOrderCard()
                }
            }
        } else if (initialStage == "Active") {
            val activeOrders = filteredOrders.filter { it.status != "Delivered" }
            if (activeOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো অর্ডার পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activeOrders, key = { it.id }) { order ->
                        Box(modifier = Modifier.animateContentSize()) {
                            OrderCard(
                                order = order,
                                onClick = { onNavigateToClient(order.clientId) },
                                onInvoiceClick = { 
                                    val client = clients.find { it.id == order.clientId }
                                    if (client != null) showInvoiceDialogForClient = client
                                },
                                onStageChange = { nextStage ->
                                    if (nextStage == "Hold") {
                                        viewModel.updateOrderStatusAndStage(order.id, "Hold", order.status)
                                    } else {
                                        orderToTransfer = order
                                        pendingNextStage = nextStage
                                    }
                                },
                                onHold = { id, currentStatus -> viewModel.updateOrderStatusAndStage(id, "Hold", currentStatus) },
                                onReturn = { _, _ -> },
                                onDelete = if (isAdmin) { { orderToDelete = order } } else null
                            )
                        }
                    }
                }
            }
        } else {
            val holdOrders = filteredOrders.filter { it.status == "Hold" }
            if (holdOrders.isNotEmpty()) {
                 Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).animateContentSize(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                      Column(modifier = Modifier.padding(16.dp)) {
                          Text("বকেয়া/স্থগিত (Hold)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                          Spacer(modifier = Modifier.height(8.dp))
                          holdOrders.forEach { order ->
                              OrderCard(
                                  order = order,
                                  onClick = { onNavigateToClient(order.clientId) },
                                  onInvoiceClick = { 
                                      val client = clients.find { it.id == order.clientId }
                                      if (client != null) showInvoiceDialogForClient = client
                                  },
                                  onStageChange = {},
                                  onHold = {_, _ ->},
                                  onReturn = { id, prev -> viewModel.updateOrderStatusAndStage(id, prev ?: "New Job", null) },
                                  onDelete = if (isAdmin) { { orderToDelete = order } } else null
                              )
                              Spacer(modifier = Modifier.height(8.dp))
                          }
                      }
                 }
            }
            
            if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো অর্ডার পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pipelineStages.filter { it != "Hold" }) { stage ->
                        val stageOrders = filteredOrders.filter { it.status == stage }
                        var expanded by remember(initialStage) { mutableStateOf(stage == initialStage || stageOrders.isNotEmpty()) }

                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expanded = !expanded }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stage,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (stageOrders.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = stageOrders.size.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = if (expanded) "Collapse" else "Expand"
                                    )
                                }
                                
                                AnimatedVisibility(visible = expanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8F9FA))
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (stageOrders.isEmpty()) {
                                            Text(
                                                text = "No jobs in this stage.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        } else {
                                            stageOrders.forEach { order ->
                                                OrderCard(
                                                    order = order,
                                                    onClick = { onNavigateToClient(order.clientId) },
                                                    onInvoiceClick = { 
                                                        val client = clients.find { it.id == order.clientId }
                                                        if (client != null) showInvoiceDialogForClient = client
                                                    },
                                                    onStageChange = { nextStage ->
                                                        if (nextStage == "Hold") {
                                                            viewModel.updateOrderStatusAndStage(order.id, "Hold", order.status)
                                                        } else if (order.status == "Packaging" && nextStage == "Stock") {
                                                            orderToPackage = order
                                                        } else {
                                                            orderToTransfer = order
                                                            pendingNextStage = nextStage
                                                        }
                                                    },
                                                    onHold = { id, currentStatus ->
                                                        viewModel.updateOrderStatusAndStage(id, "Hold", currentStatus)
                                                    },
                                                    onReturn = { _, _ -> },
                                                    onDelete = if (isAdmin) { { orderToDelete = order } } else null
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
        if (orderToPackage != null) {
            PackagingDialog(
                order = orderToPackage!!,
                onDismiss = { orderToPackage = null },
                onConfirm = { count ->
                    val quantity = orderToPackage!!.quantity.toDoubleOrNull() ?: 0.0
                    val perPacket = if (count > 0) (quantity / count).toInt() else 0
                    viewModel.updateOrderPackaging(orderToPackage!!.id, count, perPacket, orderToPackage!!.status)
                    orderToTransfer = orderToPackage
                    pendingNextStage = "Stock"
                    orderToPackage = null
                }
            )
        }
        if (orderToDelete != null) {
            AlertDialog(
                onDismissRequest = { orderToDelete = null },
                title = { Text("অর্ডার ডিলিট নিশ্চিতকরণ") },
                text = { Text("আপনি কি নিশ্চিত যে '${orderToDelete!!.jobName}' অর্ডারটি স্থায়ীভাবে ডিলিট করতে চান? এই কাজটি আর ফিরিয়ে আনা সম্ভব নয়।") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteOrder(orderToDelete!!.id)
                            orderToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_order_button")
                    ) {
                        Text("ডিলিট করুন")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { orderToDelete = null }) {
                        Text("বাতিল")
                    }
                }
            )
        }
        if (showInvoiceDialogForClient != null) {
            val clientOrders = orders.filter { it.clientId == showInvoiceDialogForClient!!.id }
            InvoiceDialog(
                client = showInvoiceDialogForClient!!,
                orders = clientOrders,
                viewModel = viewModel,
                onDismiss = { showInvoiceDialogForClient = null }
            )
        }
        if (orderToTransfer != null && pendingNextStage != null) {
            SectionTransferVendorDetailsDialog(
                order = orderToTransfer!!,
                nextStage = pendingNextStage!!,
                viewModel = viewModel,
                vendors = vendors,
                onDismiss = {
                    orderToTransfer = null
                    pendingNextStage = null
                },
                onConfirm = {
                    orderToTransfer = null
                    pendingNextStage = null
                }
            )
        }
    }
}

@Composable
fun PackagingDialog(order: Order, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var count by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Package Count") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Product: ${order.jobName}")
                Text("Total Quantity: ${order.quantity}")
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it },
                    label = { Text("Number of Packets/Cartons") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(count.toIntOrNull() ?: 0) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onInvoiceClick: () -> Unit,
    onStageChange: (String) -> Unit,
    onHold: (Int, String) -> Unit,
    onReturn: (Int, String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val currentStageIndex = pipelineStages.indexOf(order.status)
    val nextStage = if (currentStageIndex != -1 && currentStageIndex < pipelineStages.size - 1) pipelineStages[currentStageIndex + 1] else null
    val isDelivered = order.status == "Delivered"
    val isHold = order.status == "Hold"

    val baseColor = if (isDelivered) Color(0xFF4ADE80) else if (isHold) Color(0xFFF87171) else Color(0xFF60A5FA)
    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = baseColor.copy(alpha = 0.5f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = order.jobName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(baseColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(order.status, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = baseColor)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onInvoiceClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Invoice",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_order_button_${order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Order",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Client: ${order.clientName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            
            if (isHold && order.previousStage != null) {
                Text("Came from: ${order.previousStage}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF87171))
            }
            
            Text("Category: ${order.category}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
            Text("Specs: ${order.specifications}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
            Text("Total: ৳${order.totalAmount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            
            if (order.packageCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Packaging Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Packets/Cartons: ${order.packageCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text("Pieces per Packet: ${order.quantityPerPacket}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            if (isHold) {
                Button(
                    onClick = { onReturn(order.id, order.previousStage ?: "New Job") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to ${order.previousStage ?: "Start"}")
                }
            } else if (nextStage != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStageChange(nextStage) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Move to $nextStage")
                    }
                    Button(
                        onClick = { onHold(order.id, order.status) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Move to Hold")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionTransferVendorDetailsDialog(
    order: Order,
    nextStage: String,
    viewModel: MainViewModel,
    vendors: List<Vendor>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Vendor Selection state
    var selectedVendor by remember { mutableStateOf<Vendor?>(null) }
    var vendorExpanded by remember { mutableStateOf(false) }
    var isAddingNewVendor by remember { mutableStateOf(false) }

    // New Vendor Form State
    var newVendorName by remember { mutableStateOf("") }
    var newVendorCompany by remember { mutableStateOf("") }
    var newVendorCategory by remember { mutableStateOf("") }
    var newVendorCategoryExpanded by remember { mutableStateOf(false) }
    var newVendorPhone by remember { mutableStateOf("") }
    var newVendorAddress by remember { mutableStateOf("") }
    var newCreditLimitStr by remember { mutableStateOf("") }
    var newBudgetLimitStr by remember { mutableStateOf("") }

    val categories = listOf(
        "Paper Supplier", "Binding Vendor", "Lamination Service", 
        "Plate/CTP Vendor", "Ink/Chemical Supplier", "Transport/Courier", "Others"
    )

    // Vendor Bill fields
    val defaultDescription = remember(nextStage) {
        when (nextStage) {
            "Design Processing" -> "Design and layout processing"
            "Output" -> "CTP plate / film output"
            "Printing" -> "Printing press impression / run"
            "Mat Lamination" -> "Matte lamination laminating service"
            "Spot UV" -> "Spot UV varnishing/coating"
            "Die Cutting" -> "Die cutting and punching"
            "Pasting" -> "Pasting and box gluing/folding"
            "Binding" -> "Binding and book stitching"
            "Packaging" -> "Carton packaging & packing materials"
            "Stock" -> "Finished stock handling"
            "Shipping" -> "Transport, courier and shipping delivery"
            "Delivered" -> "Final hand over and logistics"
            else -> "Section work for $nextStage"
        }
    }

    var serviceDetails by remember { mutableStateOf(defaultDescription) }
    var billAmountStr by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf("Due/Unpaid") } // "Fully Paid", "Partial Paid", "Due/Unpaid"
    var amountPaidStr by remember { mutableStateOf("") }
    var mrNo by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    val paymentMethods = listOf("Cash", "Bank Transfer", "Cheque", "Mobile Banking")

    // Error and validation state
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Synchronize amountPaidStr when paymentStatus changes
    remember(paymentStatus, billAmountStr) {
        val totalAmount = billAmountStr.toDoubleOrNull() ?: 0.0
        when (paymentStatus) {
            "Fully Paid" -> amountPaidStr = billAmountStr
            "Due/Unpaid" -> amountPaidStr = "0"
            "Partial Paid" -> {
                val currentPaid = amountPaidStr.toDoubleOrNull() ?: 0.0
                if (currentPaid >= totalAmount) {
                    amountPaidStr = (totalAmount / 2.0).toString()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Title Area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Transfer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Section Transfer & Cost Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Moving Job #${order.id}: '${order.jobName}' → $nextStage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Scrollable Form Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Alert / Instruction
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "এই সেকশন পরিবর্তনের জন্য ভেন্ডর নির্বাচন ও কাজের মূল্য (বিল) তথ্য প্রদান করা বাধ্যতামূলক। এই তথ্যটি স্বয়ংক্রিয়ভাবে সংশ্লিষ্ট ভেন্ডরের লেজার ও জব-কস্টিং ট্র্যাকারে যুক্ত হবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 1. Vendor Selection / Add Vendor Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "১. ভেন্ডর নির্বাচন (Vendor Selection)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                if (!isAddingNewVendor) {
                                    TextButton(
                                        onClick = { isAddingNewVendor = true },
                                        modifier = Modifier.testTag("add_new_vendor_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add New Vendor", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ নতুন ভেন্ডর")
                                    }
                                } else {
                                    TextButton(
                                        onClick = { isAddingNewVendor = false }
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Select", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ভেন্ডর তালিকা")
                                    }
                                }
                            }

                            if (!isAddingNewVendor) {
                                // Dropdown to select existing vendor
                                ExposedDropdownMenuBox(
                                    expanded = vendorExpanded,
                                    onExpandedChange = { vendorExpanded = !vendorExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedVendor?.name ?: "ভেন্ডর নির্বাচন করুন",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("ভেন্ডর (Select Vendor)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vendorExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = vendorExpanded,
                                        onDismissRequest = { vendorExpanded = false }
                                    ) {
                                        if (vendors.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("কোনো ভেন্ডর নেই। নতুন ভেন্ডর যোগ করুন।") },
                                                onClick = { vendorExpanded = false; isAddingNewVendor = true }
                                            )
                                        } else {
                                            vendors.forEach { v ->
                                                DropdownMenuItem(
                                                    text = { Text("${v.name} (${v.companyName} - ${v.category})") },
                                                    onClick = {
                                                        selectedVendor = v
                                                        vendorExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Add New Vendor Sub-Form
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "নতুন ভেন্ডরের তথ্য পূরণ করুন:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    OutlinedTextField(
                                        value = newVendorName,
                                        onValueChange = { newVendorName = it },
                                        label = { Text("ভেন্ডর/ব্যক্তির নাম *") },
                                        modifier = Modifier.fillMaxWidth().testTag("new_vendor_name"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = newVendorCompany,
                                        onValueChange = { newVendorCompany = it },
                                        label = { Text("প্রতিষ্ঠানের নাম") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    // Category selection for new vendor
                                    ExposedDropdownMenuBox(
                                        expanded = newVendorCategoryExpanded,
                                        onExpandedChange = { newVendorCategoryExpanded = !newVendorCategoryExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = newVendorCategory,
                                            onValueChange = { newVendorCategory = it },
                                            readOnly = true,
                                            label = { Text("ভেন্ডর ক্যাটাগরি *") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = newVendorCategoryExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = newVendorCategoryExpanded,
                                            onDismissRequest = { newVendorCategoryExpanded = false }
                                        ) {
                                            categories.forEach { cat ->
                                                DropdownMenuItem(
                                                    text = { Text(cat) },
                                                    onClick = {
                                                        newVendorCategory = cat
                                                        newVendorCategoryExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = newVendorPhone,
                                        onValueChange = { newVendorPhone = it },
                                        label = { Text("মোবাইল নম্বর") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = newVendorAddress,
                                        onValueChange = { newVendorAddress = it },
                                        label = { Text("ঠিকানা") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = newCreditLimitStr,
                                            onValueChange = { newCreditLimitStr = it },
                                            label = { Text("বকেয়া সীমা (৳)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        OutlinedTextField(
                                            value = newBudgetLimitStr,
                                            onValueChange = { newBudgetLimitStr = it },
                                            label = { Text("মাসিক বাজেট (৳)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Service/Material details and bill info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "২. কাজের বিবরণ ও বিল সংক্রান্ত তথ্য",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = serviceDetails,
                                onValueChange = { serviceDetails = it },
                                label = { Text("কাজের বিবরণ (Service/Material Details) *") },
                                placeholder = { Text("উদাঃ প্লেট প্রিন্টিং, ল্যামিনেশন, ডাই কাটিং") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = billAmountStr,
                                onValueChange = { billAmountStr = it },
                                label = { Text("মোট বিলের পরিমাণ (Vendor Bill Amount) *") },
                                prefix = { Text("৳ ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Payment Status selection
                            Text("পেমেন্ট স্ট্যাটাস:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Due/Unpaid", "Partial Paid", "Fully Paid").forEach { status ->
                                    val isSelected = paymentStatus == status
                                    val label = when (status) {
                                        "Due/Unpaid" -> "বকেয়া (Unpaid)"
                                        "Partial Paid" -> "আংশিক (Partial)"
                                        "Fully Paid" -> "পরিশোধিত (Paid)"
                                        else -> status
                                    }
                                    
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { paymentStatus = status },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Payment fields
                            if (paymentStatus != "Due/Unpaid") {
                                OutlinedTextField(
                                    value = amountPaidStr,
                                    onValueChange = { amountPaidStr = it },
                                    label = { Text("তাত্ক্ষণিক পরিশোধ (Amount Paid Now) *") },
                                    prefix = { Text("৳ ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = paymentStatus == "Partial Paid",
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Payment Method Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = paymentMethodExpanded,
                                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = paymentMethod,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("পেমেন্ট পদ্ধতি (Payment Method)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = paymentMethodExpanded,
                                        onDismissRequest = { paymentMethodExpanded = false }
                                    ) {
                                        paymentMethods.forEach { method ->
                                            DropdownMenuItem(
                                                text = { Text(method) },
                                                onClick = {
                                                    paymentMethod = method
                                                    paymentMethodExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = mrNo,
                                onValueChange = { mrNo = it },
                                label = { Text("মানি রিসিট নম্বর (Money Receipt No. / MR No.)") },
                                placeholder = { Text("ঐচ্ছিক") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Display error message if validation fails
                    errorMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("বাতিল (Cancel)")
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            
                            val billAmount = billAmountStr.toDoubleOrNull() ?: 0.0
                            val amtPaid = if (paymentStatus == "Due/Unpaid") 0.0 else (amountPaidStr.toDoubleOrNull() ?: 0.0)

                            // Validate Fields
                            if (!isAddingNewVendor && selectedVendor == null) {
                                errorMessage = "অনুগ্রহ করে একটি ভেন্ডর নির্বাচন করুন অথবা নতুন ভেন্ডর যোগ করুন।"
                                return@Button
                            }
                            if (isAddingNewVendor && newVendorName.isBlank()) {
                                errorMessage = "নতুন ভেন্ডরের নাম প্রবেশ করানো আবশ্যক।"
                                return@Button
                            }
                            if (isAddingNewVendor && newVendorCategory.isBlank()) {
                                errorMessage = "নতুন ভেন্ডরের জন্য ক্যাটাগরি নির্বাচন করা আবশ্যক।"
                                return@Button
                            }
                            if (serviceDetails.isBlank()) {
                                errorMessage = "কাজের বিবরণী (Service Details) পূরণ করা আবশ্যক।"
                                return@Button
                            }
                            if (billAmountStr.isBlank() || billAmount <= 0) {
                                errorMessage = "মোট বিলের পরিমাণ অবশ্যই ০ থেকে বড় একটি সংখ্যা হতে হবে।"
                                return@Button
                            }
                            if (paymentStatus == "Partial Paid" && amtPaid <= 0) {
                                errorMessage = "আংশিক পরিশোধের ক্ষেত্রে পরিশোধিত পরিমাণ ০ থেকে বেশি হতে হবে।"
                                return@Button
                            }
                            if (amtPaid > billAmount) {
                                errorMessage = "পরিশোধিত অর্থ মোট বিলের চেয়ে বেশি হতে পারবে না।"
                                return@Button
                            }

                            // Perform Database Operations in scope
                            coroutineScope.launch {
                                try {
                                    val finalVendorId = if (isAddingNewVendor) {
                                        val newVendor = Vendor(
                                            name = newVendorName.trim(),
                                            companyName = newVendorCompany.trim(),
                                            category = newVendorCategory,
                                            phone = newVendorPhone.trim(),
                                            address = newVendorAddress.trim(),
                                            creditLimit = newCreditLimitStr.toDoubleOrNull() ?: 0.0,
                                            budgetLimit = newBudgetLimitStr.toDoubleOrNull() ?: 0.0
                                        )
                                        viewModel.addVendorAndGetId(newVendor)
                                    } else {
                                        selectedVendor!!.id
                                    }

                                    // Create the Vendor Bill
                                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    val finalMrNo = mrNo.trim().ifBlank { "MR-${System.currentTimeMillis()}" }
                                    
                                    val vendorBill = VendorBill(
                                        vendorId = finalVendorId,
                                        invoiceNo = finalMrNo,
                                        date = todayDate,
                                        orderId = order.id,
                                        description = serviceDetails.trim(),
                                        totalAmount = billAmount,
                                        advancePaid = amtPaid
                                    )
                                    
                                    viewModel.addVendorBillSuspend(vendorBill)

                                    // If a payment was made immediately, also insert vendor payment
                                    if (amtPaid > 0) {
                                        val vendorPayment = VendorPayment(
                                            vendorId = finalVendorId,
                                            date = todayDate,
                                            amount = amtPaid,
                                            paymentMethod = paymentMethod,
                                            mrNo = if (mrNo.isNotBlank()) mrNo.trim() else null,
                                            notes = "Paid for Job #${order.id} ('${order.jobName}') during section transfer to $nextStage."
                                        )
                                        viewModel.addVendorPaymentSuspend(vendorPayment)
                                    }

                                    // Finally, update the order status to complete the transfer!
                                    viewModel.updateOrderStatus(order.id, nextStage)
                                    
                                    onConfirm()
                                } catch (e: Exception) {
                                    errorMessage = "ডাটাবেজ ত্রুটি: ${e.localizedMessage}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f).testTag("submit_transfer_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("স্থানান্তর ও সংরক্ষণ")
                    }
                }
            }
        }
    }
}

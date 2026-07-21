package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import android.widget.Toast
import android.content.Context
import android.content.Intent
import java.io.File
import kotlinx.coroutines.launch
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draw.alpha
import com.example.data.DashboardWidget
import com.example.data.DashboardLayout

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.rotate

data class DashboardItem(
    val title: String,
    val count: Int,
    val indicatorColor: Color,
    val iconBgColor: Color,
    val iconColor: Color,
    val icon: ImageVector
)

@Composable
fun CustomizableWidget(
    widget: DashboardWidget,
    isCustomizing: Boolean,
    onResizeVertical: (Float) -> Unit,
    onResizeHorizontal: (Int) -> Unit,
    onToggleVisibility: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!widget.isVisible && !isCustomizing) return

    var containerWidth by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth(widget.widthSpan / 12f)
            .heightIn(min = 60.dp)
            .then(
                if (widget.isVisible) 
                    Modifier.height((200 * widget.heightMultiplier).dp).defaultMinSize(minHeight = 100.dp) 
                else 
                    Modifier.height(60.dp)
            )
            .alpha(if (widget.isVisible) 1f else 0.5f)
            .onGloballyPositioned { containerWidth = it.size.width },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCustomizing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            if (isCustomizing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.DragHandle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(widget.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp)) }
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp)) }
                        IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if (widget.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (widget.isVisible) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                    
                    if (isCustomizing) {
                        // Vertical Resize Handle (Snap to 0.25 steps)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val delta = dragAmount.y / 200f
                                        val rawValue = widget.heightMultiplier + delta
                                        // Snap to 0.25 increments
                                        val snappedValue = (rawValue * 4).toInt() / 4f
                                        if (snappedValue != widget.heightMultiplier) {
                                            onResizeVertical(snappedValue)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.UnfoldMore, contentDescription = null, modifier = Modifier.size(10.dp))
                        }

                        // Horizontal Resize Handle (Snap to 1/12 increments)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(12.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                .pointerInput(containerWidth) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (containerWidth > 0) {
                                            val currentWidth = containerWidth.toFloat()
                                            val colWidth = (currentWidth / widget.widthSpan) * 12f / 12f // Full screen width divided by 12
                                            
                                            // This is tricky because we are inside the scaled box.
                                            // Let's just use the dragAmount relative to a hypothetical 12-col grid.
                                            // A better way is to track the accumulated drag.
                                        }
                                        
                                        // Simple increment/decrement based on drag direction
                                        if (dragAmount.x > 20) {
                                            if (widget.widthSpan < 12) onResizeHorizontal(widget.widthSpan + 1)
                                        } else if (dragAmount.x < -20) {
                                            if (widget.widthSpan > 1) onResizeHorizontal(widget.widthSpan - 1)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.UnfoldMore, contentDescription = null, modifier = Modifier.size(10.dp).rotate(90f))
                        }
                    }
                }
            } else if (isCustomizing) {
                Text(
                    "Hidden: ${widget.title}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToCalculator: () -> Unit,
    onNavigateToPriceEstimation: () -> Unit,
    onNavigateToEstimateHistory: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToOrders: (String) -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToVendors: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val monthlyFinance by viewModel.monthlyFinance.collectAsState()
    val projectedMonthlyFinance by viewModel.projectedMonthlyFinance.collectAsState()
    val showProjection by viewModel.showProjection.collectAsState()
    val totalPaid by viewModel.totalPaid.collectAsState()
    val totalDue by viewModel.totalDue.collectAsState()
    val topCategories by viewModel.topCategories.collectAsState()
    val dashboardLayout by viewModel.dashboardLayout.collectAsState()
    
    var isCustomizing by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (showSignatureDialog) {
        SignatureDialog(
            onDismiss = { showSignatureDialog = false },
            onConfirm = { path ->
                showSignatureDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { isCustomizing = !isCustomizing }) {
                    Icon(
                        if (isCustomizing) Icons.Default.Check else Icons.Default.Settings,
                        contentDescription = "Customize",
                        tint = if (isCustomizing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isCustomizing) {
                    IconButton(onClick = { viewModel.resetDashboardLayout() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            }
        }

        if (!isCustomizing) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    scope.launch {
                        val json = viewModel.backupData()
                        val file = File(context.cacheDir, "backup.json")
                        file.writeText(json)
                        Toast.makeText(context, "Backup saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }, modifier = Modifier.weight(1f)) { Text("Backup") }
                Button(onClick = {
                    val file = File(context.cacheDir, "backup.json")
                    if (file.exists()) {
                        scope.launch {
                            val json = file.readText()
                            viewModel.restoreData(json)
                            Toast.makeText(context, "Restored successfully", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No backup file found", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.weight(1f)) { Text("Restore") }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            maxItemsInEachRow = 12
        ) {
            dashboardLayout.widgets.sortedBy { it.order }.forEachIndexed { index, widget ->
                CustomizableWidget(
                    widget = widget,
                    isCustomizing = isCustomizing,
                    onResizeVertical = { newMultiplier ->
                        val updatedWidgets = dashboardLayout.widgets.map {
                            if (it.id == widget.id) it.copy(heightMultiplier = newMultiplier.coerceIn(0.2f, 3.0f)) else it
                        }
                        viewModel.updateDashboardLayout(DashboardLayout(updatedWidgets))
                    },
                    onResizeHorizontal = { newSpan ->
                        val updatedWidgets = dashboardLayout.widgets.map {
                            if (it.id == widget.id) it.copy(widthSpan = newSpan) else it
                        }
                        viewModel.updateDashboardLayout(DashboardLayout(updatedWidgets))
                    },
                    onToggleVisibility = {
                        val updatedWidgets = dashboardLayout.widgets.map {
                            if (it.id == widget.id) it.copy(isVisible = !it.isVisible) else it
                        }
                        viewModel.updateDashboardLayout(DashboardLayout(updatedWidgets))
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val sorted = dashboardLayout.widgets.sortedBy { it.order }
                            val prevWidget = sorted[index - 1]
                            val updatedWidgets = dashboardLayout.widgets.map {
                                when (it.id) {
                                    widget.id -> it.copy(order = prevWidget.order)
                                    prevWidget.id -> it.copy(order = widget.order)
                                    else -> it
                                }
                            }
                            viewModel.updateDashboardLayout(DashboardLayout(updatedWidgets))
                        }
                    },
                    onMoveDown = {
                        if (index < dashboardLayout.widgets.size - 1) {
                            val sorted = dashboardLayout.widgets.sortedBy { it.order }
                            val nextWidget = sorted[index + 1]
                            val updatedWidgets = dashboardLayout.widgets.map {
                                when (it.id) {
                                    widget.id -> it.copy(order = nextWidget.order)
                                    nextWidget.id -> it.copy(order = widget.order)
                                    else -> it
                                }
                            }
                            viewModel.updateDashboardLayout(DashboardLayout(updatedWidgets))
                        }
                    }
                ) {
                    when (widget.id) {
                        "SUMMARY" -> DashboardSummaryCard(
                            monthlyFinance = if (showProjection) projectedMonthlyFinance else monthlyFinance,
                            totalPaid, totalDue, topCategories,
                            showProjection,
                            onToggleProjection = { viewModel.toggleProjection() }
                        )
                        "ORDER_STATS" -> {
                            Column {
                                Button(
                                    onClick = onNavigateToAdminDashboard,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("View Admin Dashboard")
                                }
                                AdministrativeInsightsCard(viewModel = viewModel)
                            }
                        }
                        "PRICE" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToPriceEstimation() }
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Quick Price Estimator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Instant quotes for standard prints", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = onNavigateToEstimateHistory) {
                                        Icon(Icons.Default.History, contentDescription = "History")
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                        "INVENTORY" -> InventoryTrackingWidget(viewModel = viewModel)
                        "VENDOR" -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToVendors() }
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Vendor & Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Manage suppliers and purchase bills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                        "STATUS_GRID" -> {
                            val stageMap = mapOf(
                                "নতুন কাজ" to "New Job",
                                "ডিজাইন প্রসেসিং" to "Design Processing",
                                "আউটপুট" to "Output",
                                "প্রিন্টিং" to "Printing",
                                "ম্যাট ল্যামিনেশন" to "Mat Lamination",
                                "স্পট ইউভি" to "Spot UV",
                                "ডাই কাটিং" to "Die Cutting",
                                "পেস্টিং" to "Pasting",
                                "বাইন্ডিং" to "Binding",
                                "প্যাকেজিং" to "Packaging",
                                "স্টক" to "Stock",
                                "শিপিং" to "Shipping",
                                "ডেলিভারড" to "Delivered"
                            )

                            val dashboardItems = listOf(
                                DashboardItem("নতুন কাজ", orders.count { it.status == stageMap["নতুন কাজ"] }, Color(0xFF8B5CF6), Color(0xFFF3E8FF), Color(0xFF9333EA), Icons.Filled.NewReleases),
                                DashboardItem("ডিজাইন প্রসেসিং", orders.count { it.status == stageMap["ডিজাইন প্রসেসিং"] }, Color(0xFF4F46E5), Color(0xFFE0E7FF), Color(0xFF4338CA), Icons.Filled.Palette),
                                DashboardItem("আউটপুট", orders.count { it.status == stageMap["আউটপুট"] }, Color(0xFF3B82F6), Color(0xFFDBEAFE), Color(0xFF1D4ED8), Icons.AutoMirrored.Filled.Send),
                                DashboardItem("প্রিন্টিং", orders.count { it.status == stageMap["প্রিন্টিং"] }, Color(0xFFEAB308), Color(0xFFFEF9C3), Color(0xFFA16207), Icons.Filled.Print),
                                DashboardItem("ম্যাট ল্যামিনেশন", orders.count { it.status == stageMap["ম্যাট ল্যামিনেশন"] }, Color(0xFFF43F5E), Color(0xFFFFE4E6), Color(0xFFBE123C), Icons.Filled.Layers),
                                DashboardItem("স্পট ইউভি", orders.count { it.status == stageMap["স্পট ইউভি"] }, Color(0xFF14B8A6), Color(0xFFCCFBF1), Color(0xFF0F766E), Icons.Filled.RadioButtonChecked),
                                DashboardItem("ডাই কাটিং", orders.count { it.status == stageMap["ডাই কাটিং"] }, Color(0xFFF97316), Color(0xFFFFEDD5), Color(0xFFC2410C), Icons.Filled.ContentCut),
                                DashboardItem("পেস্টিং", orders.count { it.status == stageMap["পেস্টিং"] }, Color(0xFF8B4513), Color(0xFFFFF7ED), Color(0xFF7C2D12), Icons.Filled.ContentPaste),
                                DashboardItem("বাইন্ডিং", orders.count { it.status == stageMap["বাইন্ডিং"] }, Color(0xFF64748B), Color(0xFFF1F5F9), Color(0xFF334155), Icons.AutoMirrored.Filled.MenuBook),
                                DashboardItem("প্যাকেজিং", orders.count { it.status == stageMap["প্যাকেজিং"] }, Color(0xFFEA580C), Color(0xFFFFEDD5), Color(0xFFC2410C), Icons.Filled.Inventory),
                                DashboardItem("স্টক", orders.count { it.status == stageMap["স্টক"] }, Color(0xFFD946EF), Color(0xFFFAE8FF), Color(0xFFC026D3), Icons.Filled.Store),
                                DashboardItem("শিপিং", orders.count { it.status == stageMap["শিপিং"] }, Color(0xFF06B6D4), Color(0xFFCFFAFE), Color(0xFF0E7490), Icons.Filled.LocalShipping),
                                DashboardItem("ডেলিভারড", orders.count { it.status == stageMap["ডেলিভারড"] }, Color(0xFF22C55E), Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Filled.CheckCircle),
                                DashboardItem("চলতি কাজ", orders.count { it.status != "Delivered" }, Color(0xFF84CC16), Color(0xFFECFCCB), Color(0xFF4D7C0F), Icons.Filled.Autorenew),
                                DashboardItem("বকেয়া/স্থগিত", 0, Color(0xFFEF4444), Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Filled.Schedule)
                            )

                            val chunkedItems = dashboardItems.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                                chunkedItems.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            DashboardCard(
                                                item = item,
                                                modifier = Modifier.weight(1f),
                                                onClick = { 
                                                    if (item.title == "চলতি কাজ") {
                                                        onNavigateToOrders("Active")
                                                    } else {
                                                        val stage = stageMap[item.title]
                                                        if (stage != null) onNavigateToOrders(stage)
                                                    }
                                                }
                                            )
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (!isCustomizing) {
            Button(
                onClick = { showSignatureDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capture Signature")
            }
        }
    }
}

@Composable
fun DashboardSummaryCard(
    monthlyFinance: Map<String, MainViewModel.MonthlyFinance>,
    totalPaid: Double,
    totalDue: Double,
    topCategories: Map<String, Double>,
    showProjection: Boolean,
    onToggleProjection: () -> Unit
) {
    val paidEntries = monthlyFinance.values.mapIndexed { index, finance -> FloatEntry(index.toFloat(), finance.paid.toFloat()) }
    val dueEntries = monthlyFinance.values.mapIndexed { index, finance -> FloatEntry(index.toFloat(), finance.due.toFloat()) }
    val chartEntryModel = entryModelOf(paidEntries, dueEntries)
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Business Insights", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { exportDashboardDataToCsv(context, monthlyFinance, totalPaid, totalDue, topCategories) }) {
                    Icon(Icons.Filled.Download, "Export CSV", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Monthly Paid vs Due", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Projection", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Switch(checked = showProjection, onCheckedChange = { onToggleProjection() })
                }
            }
            
            Chart(
                chart = columnChart(
                    columns = listOf(
                        lineComponent(color = Color(0xFF15803D), thickness = 16.dp),
                        lineComponent(color = Color(0xFFEF4444), thickness = 16.dp)
                    )
                ),
                model = chartEntryModel,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Paid", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("৳${NumberFormat.getNumberInstance(Locale.US).format(totalPaid)}", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF15803D)) 
                }
                Column {
                    Text("Total Due", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("৳${NumberFormat.getNumberInstance(Locale.US).format(totalDue)}", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFEF4444)) 
                }
            }
        }
    }
}

@Composable
fun DashboardCard(item: DashboardItem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val isActive = item.count > 0
    val borderColor = if (isActive) item.indicatorColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(if (isActive) item.indicatorColor else Color.Transparent)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(item.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.count.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(item.indicatorColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun exportDashboardDataToCsv(
    context: Context,
    monthlyFinance: Map<String, MainViewModel.MonthlyFinance>,
    totalPaid: Double,
    totalDue: Double,
    topCategories: Map<String, Double>
) {
    val csvString = StringBuilder()
    csvString.append("Type,Description,Value1,Value2\n")
    
    monthlyFinance.forEach { (month, finance) ->
        csvString.append("PaidVsDue,$month,${finance.paid},${finance.due}\n")
    }
    
    topCategories.forEach { (category, amount) ->
        csvString.append("Category,$category,$amount,\n")
    }
    
    val file = File(context.cacheDir, "dashboard_report.csv")
    file.writeText(csvString.toString())
    
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV"))
    Toast.makeText(context, "CSV report exported successfully", Toast.LENGTH_SHORT).show()
}

@Composable
fun AdministrativeInsightsCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val revenueTrends by viewModel.currentMonthRevenueTrends.collectAsState()
    val orderVolume by viewModel.orderVolumeByCategory.collectAsState()
    val categoryLabels by viewModel.orderCategoryLabels.collectAsState()

    val revenueModel = remember(revenueTrends) {
        if (revenueTrends.isNotEmpty()) entryModelOf(revenueTrends) else null
    }
    val volumeModel = remember(orderVolume) {
        if (orderVolume.isNotEmpty()) entryModelOf(orderVolume) else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .testTag("admin_insights_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "প্রশাসনিক অন্তর্দৃষ্টি",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Administrative Insights",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chart 1: Current Month Daily Revenue Trend
            Text(
                text = "চলতি মাসের দৈনিক রাজস্ব ধারা (Revenue Trends)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (revenueModel != null) {
                Chart(
                    chart = lineChart(),
                    model = revenueModel,
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _ ->
                            "৳${value.toInt()}"
                        }
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            "দিন ${value.toInt()}"
                        }
                    ),
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "চলতি মাসে কোনো রাজস্ব ডেটা পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Chart 2: Total Order Volume by Category
            Text(
                text = "ক্যাটাগরি ভিত্তিক মোট অর্ডারের পরিমাণ (Order Volume)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (volumeModel != null) {
                Chart(
                    chart = columnChart(
                        columns = listOf(
                            lineComponent(
                                color = MaterialTheme.colorScheme.primary,
                                thickness = 16.dp
                            )
                        )
                    ),
                    model = volumeModel,
                    startAxis = rememberStartAxis(
                        valueFormatter = { value, _ ->
                            "${value.toInt()} টি"
                        }
                    ),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = { value, _ ->
                            categoryLabels.getOrNull(value.toInt()) ?: ""
                        }
                    ),
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো অর্ডারের ডেটা পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTrackingWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.inventoryItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<com.example.data.InventoryItem?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .testTag("inventory_tracking_widget"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "ইনভেন্টরি ট্র্যাকিং",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Printing Materials & Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_inventory_item_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "যোগ করুন",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("যোগ করুন", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো ইনভেন্টরি আইটেম পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        val isLowStock = item.quantity <= item.minThreshold
                        val backgroundColor = if (isLowStock) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                        val borderColor = if (isLowStock) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor, RoundedCornerShape(12.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Info Column
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isLowStock) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.error,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "স্টক কম",
                                                    tint = MaterialTheme.colorScheme.onError,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "স্টক কম",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "বর্তমান স্টক: ${item.quantity} ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "সর্বনিম্ন সীমা: ${item.minThreshold} ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Controls Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isLowStock) {
                                    IconButton(
                                        onClick = { viewModel.createRestockOrder(item) },
                                        modifier = Modifier.size(32.dp).testTag("restock_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = "রিস্টক অর্ডার",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Decrease button
                                IconButton(
                                    onClick = {
                                        if (item.quantity > 0) {
                                            viewModel.saveInventoryItem(item.copy(quantity = item.quantity - 1))
                                        }
                                    },
                                    modifier = Modifier.size(32.dp).testTag("decrease_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "কমান",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Increase button
                                IconButton(
                                    onClick = {
                                        viewModel.saveInventoryItem(item.copy(quantity = item.quantity + 1))
                                    },
                                    modifier = Modifier.size(32.dp).testTag("increase_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "বাড়ান",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Edit button
                                IconButton(
                                    onClick = { editingItem = item },
                                    modifier = Modifier.size(32.dp).testTag("edit_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "সম্পাদনা",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = { viewModel.deleteInventoryItem(item.id, item.name) },
                                    modifier = Modifier.size(32.dp).testTag("delete_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "মুছুন",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var unit by remember { mutableStateOf("Rim") }
        var quantityStr by remember { mutableStateOf("") }
        var thresholdStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন ইনভেন্টরি আইটেম") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("আইটেমের নাম (Item Name)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক (Unit, e.g., Rim, Pcs)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_unit_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("বর্তমান স্টক (Current Quantity)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_quantity_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("সর্বনিম্ন সীমা (Minimum Threshold)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_threshold_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantityStr.toIntOrNull() ?: 0
                        val thresh = thresholdStr.toIntOrNull() ?: 0
                        if (name.isNotBlank()) {
                            viewModel.saveInventoryItem(
                                com.example.data.InventoryItem(
                                    name = name,
                                    quantity = qty,
                                    minThreshold = thresh,
                                    unit = unit
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_add_item_button")
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Edit Item Dialog
    if (editingItem != null) {
        val currentItem = editingItem!!
        var name by remember { mutableStateOf(currentItem.name) }
        var unit by remember { mutableStateOf(currentItem.unit) }
        var quantityStr by remember { mutableStateOf(currentItem.quantity.toString()) }
        var thresholdStr by remember { mutableStateOf(currentItem.minThreshold.toString()) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("ইনভেন্টরি আইটেম সম্পাদন") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("আইটেমের নাম (Item Name)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_item_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক (Unit)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_item_unit_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("বর্তমান স্টক (Current Quantity)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_item_quantity_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it },
                        label = { Text("সর্বনিম্ন সীমা (Minimum Threshold)") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_item_threshold_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantityStr.toIntOrNull() ?: currentItem.quantity
                        val thresh = thresholdStr.toIntOrNull() ?: currentItem.minThreshold
                        if (name.isNotBlank()) {
                            viewModel.saveInventoryItem(
                                currentItem.copy(
                                    name = name,
                                    quantity = qty,
                                    minThreshold = thresh,
                                    unit = unit
                                )
                            )
                            editingItem = null
                        }
                    },
                    modifier = Modifier.testTag("save_edit_item_button")
                ) {
                    Text("হালনাগাদ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

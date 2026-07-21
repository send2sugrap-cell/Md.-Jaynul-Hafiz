package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Order
import com.example.data.Vendor
import com.example.data.VendorBill
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobWiseVendorCostAnalysisScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val vendorBills by viewModel.vendorBills.collectAsStateWithLifecycle()
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedProfitabilityFilter by remember { mutableStateOf("All") } // "All", "Profitable", "Loss/Over-budget", "No Cost"

    val keyboardController = LocalSoftwareKeyboardController.current

    // Aggregate vendor bills by order/job ID
    val billsByOrderId = remember(vendorBills) {
        vendorBills.groupBy { it.orderId }
    }

    // Prepare list of cost analysis items
    val costAnalysisList = remember(orders, billsByOrderId) {
        orders.map { order ->
            val linkedBills = billsByOrderId[order.id] ?: emptyList()
            val totalCost = linkedBills.sumOf { it.totalAmount }
            val estimatedPrice = order.totalAmount
            val margin = estimatedPrice - totalCost
            val marginPercent = if (estimatedPrice > 0) (margin / estimatedPrice) * 100 else 0.0

            JobCostAnalysisItem(
                order = order,
                linkedBills = linkedBills,
                totalCost = totalCost,
                margin = margin,
                marginPercent = marginPercent
            )
        }
    }

    // Extract unique order categories for filters
    val categories = remember(orders) {
        listOf("All") + orders.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    // Filtered list
    val filteredAnalysisList = remember(costAnalysisList, searchQuery, selectedCategoryFilter, selectedProfitabilityFilter) {
        costAnalysisList.filter { item ->
            val matchesSearch = item.order.jobName.contains(searchQuery, ignoreCase = true) ||
                    item.order.clientName.contains(searchQuery, ignoreCase = true) ||
                    item.order.id.toString() == searchQuery.trim()

            val matchesCategory = selectedCategoryFilter == "All" || item.order.category == selectedCategoryFilter

            val matchesProfitability = when (selectedProfitabilityFilter) {
                "All" -> true
                "Profitable" -> item.totalCost > 0 && item.margin >= 0
                "Loss/Over-budget" -> item.totalCost > item.order.totalAmount
                "No Cost" -> item.totalCost == 0.0
                else -> true
            }

            matchesSearch && matchesCategory && matchesProfitability
        }
    }

    // Summary calculations based on filtered list (or entire list, let's base it on the filtered list for dynamic feedback)
    val totalEstimatedOfFiltered = filteredAnalysisList.sumOf { item -> item.order.totalAmount }
    val totalCostOfFiltered = filteredAnalysisList.sumOf { item -> item.totalCost }
    val totalMarginOfFiltered = totalEstimatedOfFiltered - totalCostOfFiltered
    val averageMarginPercent = if (totalEstimatedOfFiltered > 0) (totalMarginOfFiltered / totalEstimatedOfFiltered) * 100 else 0.0
    val overBudgetCount = filteredAnalysisList.count { item -> item.totalCost > item.order.totalAmount }

    val currencyFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }

    Scaffold(
        modifier = Modifier.testTag("job_wise_cost_analysis_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "জব-ভিত্তিক ভেন্ডর খরচ বিশ্লেষণ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Job-Wise Vendor Cost Analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Metrics Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "সারসংক্ষেপ (Metrics Summary)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Estimate Card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "মোট চুক্তি মূল্য",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "৳${currencyFormatter.format(totalEstimatedOfFiltered)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Total Production Cost Card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "মোট ভেন্ডর বিল",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "৳${currencyFormatter.format(totalCostOfFiltered)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (overBudgetCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Net Profit / Margin
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "লাভের পরিমাণ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "৳${currencyFormatter.format(totalMarginOfFiltered)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalMarginOfFiltered >= 0) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                            )
                        }

                        // Margin % & Over-budget warning
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "গড় মুনাফার হার",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", averageMarginPercent),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (averageMarginPercent >= 20.0) Color(0xFF15803D) else if (averageMarginPercent >= 0) Color(0xFFB45309) else MaterialTheme.colorScheme.error
                                )
                                if (overBudgetCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (overBudgetCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp, 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Alert",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "সতর্কতা: $overBudgetCount টি জবের উৎপাদন খরচ চুক্তিমূল্যকে ছাড়িয়ে গেছে!",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Filters Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("জব আইডি, নাম বা কাস্টমার খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cost_search_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Category and Profitability Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Profitability Filters
                    val profitabilityOptions = listOf(
                        "All" to "সব জব",
                        "Profitable" to "লাভজনক",
                        "Loss/Over-budget" to "বাজেট অতিরিক্ত",
                        "No Cost" to "খরচহীন"
                    )

                    var profitabilityExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { profitabilityExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = profitabilityOptions.find { it.first == selectedProfitabilityFilter }?.second ?: "স্ট্যাটাস",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = profitabilityExpanded,
                            onDismissRequest = { profitabilityExpanded = false }
                        ) {
                            profitabilityOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedProfitabilityFilter = value
                                        profitabilityExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Filters Dropdown
                    var categoryExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (selectedCategoryFilter == "All") "সব ক্যাটাগরি" else selectedCategoryFilter,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(if (cat == "All") "সব ক্যাটাগরি" else cat) },
                                    onClick = {
                                        selectedCategoryFilter = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Orders list
            if (filteredAnalysisList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "কোনো রেকর্ড পাওয়া যায়নি।",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("cost_analysis_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAnalysisList, key = { item -> item.order.id }) { item ->
                        JobCostAnalysisCard(
                            item = item,
                            vendors = vendors,
                            currencyFormatter = currencyFormatter
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobCostAnalysisCard(
    item: JobCostAnalysisItem,
    vendors: List<Vendor>,
    currencyFormatter: NumberFormat
) {
    var expanded by remember { mutableStateOf(false) }

    val order = item.order
    val ratio = if (order.totalAmount > 0) item.totalCost / order.totalAmount else 0.0

    // Color indicators based on cost to estimate ratio
    val progressColor = when {
        item.totalCost == 0.0 -> MaterialTheme.colorScheme.outlineVariant
        ratio > 1.0 -> MaterialTheme.colorScheme.error
        ratio > 0.8 -> Color(0xFFD97706) // Dark yellow/orange
        else -> Color(0xFF16A34A) // Elegant green
    }

    val statusText = when {
        item.totalCost == 0.0 -> "কোনো খরচ যুক্ত নেই"
        ratio > 1.0 -> "বাজেট অতিক্রম (Loss)"
        ratio > 0.8 -> "সীমিত মুনাফা (Warning)"
        else -> "লাভজনক (Profitable)"
    }

    val containerColor = when {
        ratio > 1.0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ratio > 0.8 -> Color(0xFFFEF3C7).copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    val outlineColor = when {
        ratio > 1.0 -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        ratio > 0.8 -> Color(0xFFF59E0B).copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("job_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, outlineColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Title Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ID: ${order.id}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(6.dp, 3.dp)
                            )
                        }
                        Text(
                            text = order.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.jobName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "কাস্টমার: ${order.clientName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(color = outlineColor.copy(alpha = 0.3f))

            // Cost vs estimated price details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("চুক্তিমূল্য (Estimated Price)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${currencyFormatter.format(order.totalAmount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("উৎপাদন খরচ (Vendor Cost)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "৳${currencyFormatter.format(item.totalCost)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }

            // Profit / Remaining Margin Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("মুনাফা (Margin)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "৳${currencyFormatter.format(item.margin)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.margin >= 0) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "(%.1f%%)", item.marginPercent),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.margin >= 0) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = progressColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, progressColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (ratio > 1.0) MaterialTheme.colorScheme.error else progressColor,
                        modifier = Modifier.padding(10.dp, 4.dp)
                    )
                }
            }

            // Visual Ratio Slider/Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "খরচ বনাম চুক্তিমূল্য অনুপাত",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", ratio * 100),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
                LinearProgressIndicator(
                    progress = ratio.toFloat().coerceAtMost(1.0f),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
            }

            // Expandable itemized vendor bills breakdown
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ভেন্ডর বিল আইটেমসমূহ (${item.linkedBills.size}টি বিল)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (item.linkedBills.isEmpty()) {
                        Text(
                            text = "এই জবের সাথে যুক্ত কোনো ভেন্ডর বিল খুঁজে পাওয়া যায়নি। সেকশন পরিবর্তন করার সময় ভেন্ডর খরচ যুক্ত করতে পারেন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        item.linkedBills.forEach { bill ->
                            val vendorName = vendors.find { it.id == bill.vendorId }?.name ?: "Unknown Vendor"
                            val vendorCompany = vendors.find { it.id == bill.vendorId }?.companyName ?: ""
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = vendorName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (vendorCompany.isNotEmpty()) {
                                                Text(
                                                    text = vendorCompany,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = "৳${currencyFormatter.format(bill.totalAmount)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = bill.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        Text(
                                            text = bill.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (bill.invoiceNo.isNotEmpty()) {
                                        Text(
                                            text = "রিসিট নং: ${bill.invoiceNo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
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

data class JobCostAnalysisItem(
    val order: Order,
    val linkedBills: List<VendorBill>,
    val totalCost: Double,
    val margin: Double,
    val marginPercent: Double
)

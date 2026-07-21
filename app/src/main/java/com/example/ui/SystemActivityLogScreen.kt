package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SystemActivityLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemActivityLogScreen(
    viewModel: MainViewModel,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    val logs by viewModel.systemActivityLogs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    
    // Date range picker state
    val dateRangePickerState = rememberDateRangePickerState()
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Filter and search logic
    val filteredLogs = remember(logs, searchQuery, selectedFilter, dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis) {
        logs.filter { log ->
            val matchesFilter = selectedFilter == "All" || log.actionType.equals(selectedFilter, ignoreCase = true)
            val matchesSearch = log.actor.contains(searchQuery, ignoreCase = true) ||
                    log.details.contains(searchQuery, ignoreCase = true) ||
                    log.actionType.contains(searchQuery, ignoreCase = true)
            
            val matchesDate = if (dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null) {
                log.timestamp >= dateRangePickerState.selectedStartDateMillis!! && log.timestamp <= (dateRangePickerState.selectedEndDateMillis!! + 86400000)
            } else {
                true
            }
            
            matchesFilter && matchesSearch && matchesDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "সিস্টেম অ্যাক্টিভিটি লগ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "System Activity Log",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isAdmin && logs.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear All Logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
                .padding(16.dp)
        ) {
            // Search Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("খুঁজুন (লগ বিবরণ, অভিনেতা)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("log_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                IconButton(
                    onClick = { showDatePickerDialog = true },
                    modifier = Modifier.testTag("date_range_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "তারিখ নির্বাচন করুন",
                        tint = if (dateRangePickerState.selectedStartDateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Filters Row
            val filterOptions = listOf(
                "All" to "সব কার্যক্রম",
                "LOGIN" to "লগইন",
                "LOGOUT" to "লগআউট",
                "ORDER_DELETION" to "অর্ডার ডিলিট",
                "SETTINGS_UPDATE" to "সেটিংস আপডেট",
                "BACKUP_RESTORE" to "রিস্টোর",
                "INVENTORY_ALERT" to "ইনভেন্টরি সতর্কতা",
                "INVENTORY_UPDATE" to "ইনভেন্টরি আপডেট",
                "RESTOCK_ORDER" to "রিস্টক অর্ডার"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                items(filterOptions) { (key, label) ->
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

            // Results summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মোট লগ পাওয়া গেছে: ${filteredLogs.size} টি",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedFilter != "All" || searchQuery.isNotEmpty()) {
                    Text(
                        text = "ফিল্টার সক্রিয়",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                selectedFilter = "All"
                                searchQuery = ""
                            }
                            .padding(4.dp)
                    )
                }
            }

            // Logs list
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "কোনো অ্যাক্টিভিটি লগ পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        ActivityLogCard(log = log)
                    }
                }
            }
        }
    }

    // Clear Logs Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("লগ মুছে ফেলার নিশ্চিতকরণ") },
            text = {
                Text("আপনি কি নিশ্চিত যে সমস্ত সিস্টেম অ্যাক্টিভিটি লগ মুছে ফেলতে চান? এটি আর ফেরত আনা সম্ভব নয়।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearSystemActivityLogs()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_logs_button")
                ) {
                    Text("হ্যাঁ, ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
    
    // Date Range Picker Dialog
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("ঠিক আছে")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    dateRangePickerState.setSelection(null, null)
                    showDatePickerDialog = false
                }) {
                    Text("রিসেট")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActivityLogCard(log: SystemActivityLog) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    // Color and Icon pairing based on activity type
    val (icon, color) = when (log.actionType.uppercase()) {
        "LOGIN" -> Icons.AutoMirrored.Filled.Login to Color(0xFF3B82F6) // Blue
        "LOGOUT" -> Icons.AutoMirrored.Filled.Logout to Color(0xFF6B7280) // Gray
        "ORDER_DELETION" -> Icons.Default.Delete to Color(0xFFEF4444) // Red
        "SETTINGS_UPDATE" -> Icons.Default.Settings to Color(0xFFF59E0B) // Amber
        "BACKUP_RESTORE" -> Icons.Default.Backup to Color(0xFF10B981) // Green
        "INVENTORY_ALERT" -> Icons.Default.Warning to Color(0xFFF59E0B) // Amber / Red warning
        "INVENTORY_UPDATE" -> Icons.Default.Inventory to Color(0xFF3B82F6) // Blue / Info
        "RESTOCK_ORDER" -> Icons.Default.ShoppingCart to Color(0xFF8B5CF6) // Purple / Restock
        else -> Icons.Default.History to Color(0xFF8B5CF6) // Purple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon badge indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = log.actionType,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.actionType,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "অভিনেতা (Actor):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = log.actor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

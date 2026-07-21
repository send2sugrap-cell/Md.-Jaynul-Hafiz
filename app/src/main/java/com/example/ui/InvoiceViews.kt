package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Client
import com.example.data.InvoiceRecord
import com.example.data.Order
import androidx.compose.ui.platform.LocalContext
import com.example.util.generateInvoicePdf
import com.example.util.InvoiceSettings
import com.example.util.HtmlPrintExporter

@Composable
fun InvoiceDialog(client: Client, orders: List<Order>, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val invoiceRecords by viewModel.getInvoiceRecordsForClient(client.id).collectAsState(initial = emptyList())
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invoices for ${client.name}", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                Divider()
                
                var historyHeightFraction by remember { mutableStateOf(0.4f) }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val totalHeight = constraints.maxHeight.toFloat()
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Invoice History section
                        Box(modifier = Modifier.fillMaxWidth().weight(historyHeightFraction)) {
                            Column {
                                Text(
                                    "Invoice History", 
                                    style = MaterialTheme.typography.titleMedium, 
                                    modifier = Modifier.padding(16.dp)
                                )
                                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                                    items(invoiceRecords) { record ->
                                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Date: ${record.date}")
                                                    Text("Amount: ৳${record.totalAmount}")
                                                    val due = record.totalAmount - record.advanceAmount
                                                    Text(
                                                        text = if (due > 0) "Due: ৳$due" else "Paid",
                                                        color = if (due > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(onClick = { /* Open PDF action */ }) {
                                                    Icon(Icons.Filled.Download, contentDescription = "Download")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Resizable Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newFraction = historyHeightFraction + (dragAmount.y / totalHeight)
                                        historyHeightFraction = newFraction.coerceIn(0.1f, 0.8f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Drag to Resize", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Current Pending Orders section
                        Box(modifier = Modifier.fillMaxWidth().weight(1f - historyHeightFraction)) {
                            Column {
                                Text(
                                    "Current Pending Orders", 
                                    style = MaterialTheme.typography.titleMedium, 
                                    modifier = Modifier.padding(16.dp)
                                )
                                if (orders.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No pending invoices available for this client.")
                                    }
                                } else {
                                    val ordersByDate = orders.groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) }
                                    
                                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        items(ordersByDate.entries.toList()) { (date, dailyOrders) ->
                                            CombinedInvoiceView(client, date, dailyOrders, viewModel)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinedInvoiceView(client: Client, date: String, orders: List<Order>, viewModel: MainViewModel) {
    val context = LocalContext.current
    var printLayout by remember { mutableStateOf("A4") }
    var showPrintSettings by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var invoiceSettings by remember { mutableStateOf(InvoiceSettings(true, true, true, "", true, null)) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrintPreview by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        InvoiceSettingsDialog(
            settings = invoiceSettings,
            onSave = {
                invoiceSettings = it
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

// Total calculation
    val totalAmount = orders.sumOf { it.totalAmount }
    
    // We use the first order's advance amount to represent the group's advance
    var advanceAmountText by remember { mutableStateOf(orders.firstOrNull()?.advanceAmount?.toString() ?: "0.0") }
    val advanceAmount = advanceAmountText.toDoubleOrNull() ?: 0.0
    val dueAmount = totalAmount - advanceAmount
    
    var editableOrders by remember { mutableStateOf(orders) }
    var showReorderDialog by remember { mutableStateOf(false) }

    if (showReorderDialog) {
        ReorderableDialog(
            orders = editableOrders,
            onDismiss = { showReorderDialog = false },
            onSave = { newOrders ->
                editableOrders = newOrders
                showReorderDialog = false
            }
        )
    }

    if (showPrintPreview) {
        InvoicePrintPreviewModal(
            client = client,
            orders = editableOrders,
            totalAmount = totalAmount,
            advanceAmount = advanceAmount,
            date = date,
            settings = invoiceSettings,
            onDismiss = { showPrintPreview = false },
            onConfirmPrint = {
                showPrintPreview = false
                HtmlPrintExporter.printInvoice(
                    context = context,
                    client = client,
                    orders = editableOrders,
                    totalAmount = totalAmount,
                    advanceAmount = advanceAmount,
                    date = date,
                    settings = invoiceSettings
                )
            }
        )
    }

    if (showFormatDialog) {
        PrintFormatSelectionDialog(
            onConfirm = { isA5 ->
                showFormatDialog = false
                val file = generateInvoicePdf(context, client, editableOrders, totalAmount, advanceAmount, date, isA5, invoiceSettings)
                file?.let {
                    viewModel.addInvoiceRecord(
                        InvoiceRecord(
                            clientId = client.id,
                            date = date,
                            totalAmount = totalAmount,
                            advanceAmount = advanceAmount,
                            filePath = it.absolutePath
                        )
                    )
                    viewModel.showNotification("ইনভয়েস সফলভাবে সংরক্ষণ এবং ডাউনলোড করা হয়েছে!")
                    try {
                        val authority = "${context.packageName}.provider"
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, it)
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(viewIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onDismiss = { showFormatDialog = false }
        )
    }
    
    // Apply styling based on layout
    val isA5 = printLayout == "A5"
    val contentPadding = if (isA5) 8.dp else 16.dp

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3f)
        offset += offsetChange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .transformable(state = state),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Print Settings Toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showSettingsDialog = true }) {
                    Text("Settings")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showReorderDialog = true }) {
                    Text("Edit Order")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showPrintSettings = !showPrintSettings }) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print Settings: $printLayout")
                }
            }
            
            if (showPrintSettings) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Layout:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    SegmentedButton(
                        options = listOf("A4", "A5"),
                        selectedOption = printLayout,
                        onOptionSelected = { printLayout = it }
                    )
                }
            }
            
            // INVOICE CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(contentPadding)
            ) {
                // Header & Branding Section (Top Layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Position Sucharu Graphics Logo permanently and beautifully
                        Box(
                            modifier = Modifier
                                .size(if (isA5) 44.dp else 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(2.dp)
                        ) {
                            if (invoiceSettings.logoUri != null) {
                                AsyncImage(
                                    model = invoiceSettings.logoUri,
                                    contentDescription = "Company Logo",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                val profileUri by viewModel.profilePictureUri.collectAsState()
                                if (profileUri != null) {
                                    AsyncImage(
                                        model = profileUri,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = com.example.R.drawable.printing_logo_1784376787420),
                                        contentDescription = "Sucharu Graphics Logo",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "SUCHARU GRAPHICS",
                                fontSize = if (isA5) 14.sp else 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A), // Deep navy/charcoal primary
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Modern Offset & Digital Printing",
                                fontSize = if (isA5) 9.sp else 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B) // Slate accent
                            )
                        }
                    }

                    // Right Column: INVOICE title & Badge
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "INVOICE",
                                fontSize = if (isA5) 11.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "INV-${date.replace("-", "")}-${client.id}",
                            fontSize = if (isA5) 10.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isA5) 12.dp else 20.dp))
                Divider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(if (isA5) 12.dp else 20.dp))

                // Metadata Info Row (BILLED TO & INVOICE DETAILS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Billed To Column
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "BILLED TO",
                            fontSize = if (isA5) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8), // Muted grey
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = client.name,
                            fontSize = if (isA5) 13.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (client.company.isNotBlank()) {
                            Text(
                                text = client.company,
                                fontSize = if (isA5) 10.sp else 12.sp,
                                color = Color(0xFF475569)
                            )
                        }
                        if (client.mobile.isNotBlank()) {
                            Text(
                                text = "Phone: ${client.mobile}",
                                fontSize = if (isA5) 10.sp else 12.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    // Metadata Details Column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "INVOICE DETAILS",
                            fontSize = if (isA5) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Date:",
                                fontSize = if (isA5) 10.sp else 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = date,
                                fontSize = if (isA5) 10.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Format:",
                                fontSize = if (isA5) 10.sp else 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = printLayout,
                                fontSize = if (isA5) 10.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isA5) 16.dp else 24.dp))

                // Itemized Financial Grid (The Table)
                // Table Header
                Surface(
                    color = Color(0xFF1E293B), // Slate 800 (Deep Navy/Charcoal)
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বিবরণ (Description)",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isA5) 10.sp else 12.sp,
                            color = Color.White,
                            modifier = Modifier.weight(2.5f)
                        )
                        Text(
                            text = "পরিমাণ (Qty)",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isA5) 10.sp else 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )
                        Text(
                            text = "দাম (Rate)",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isA5) 10.sp else 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.0f)
                        )
                        Text(
                            text = "টাকা (Total)",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isA5) 10.sp else 12.sp,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }

                // Table Rows
                editableOrders.forEachIndexed { index, order ->
                    val rowBgColor = if (index % 2 == 0) Color.White else Color(0xFFF1F5F9)
                    Surface(
                        color = rowBgColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2.5f)) {
                                BasicTextField(
                                    value = order.jobName,
                                    onValueChange = { newName ->
                                        editableOrders = editableOrders.toMutableList().apply {
                                            this[index] = this[index].copy(jobName = newName)
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isA5) 11.sp else 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                )
                                BasicTextField(
                                    value = order.specifications,
                                    onValueChange = { newSpecs ->
                                        editableOrders = editableOrders.toMutableList().apply {
                                            this[index] = this[index].copy(specifications = newSpecs)
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontSize = if (isA5) 9.sp else 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                )
                            }
                            
                            BasicTextField(
                                value = order.quantity,
                                onValueChange = { newQty ->
                                    editableOrders = editableOrders.toMutableList().apply {
                                        this[index] = this[index].copy(quantity = newQty)
                                        // Auto-update total amount
                                        val q = newQty.toDoubleOrNull() ?: 0.0
                                        this[index] = this[index].copy(totalAmount = q * this[index].perUnitCost)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = if (isA5) 11.sp else 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.weight(0.8f)
                            )
                            
                            BasicTextField(
                                value = String.format(Locale.US, "%.2f", order.perUnitCost),
                                onValueChange = { newRate ->
                                    val rate = newRate.toDoubleOrNull() ?: 0.0
                                    editableOrders = editableOrders.toMutableList().apply {
                                        this[index] = this[index].copy(perUnitCost = rate)
                                        // Auto-update total amount
                                        val q = this[index].quantity.toDoubleOrNull() ?: 0.0
                                        this[index] = this[index].copy(totalAmount = q * rate)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = if (isA5) 11.sp else 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.End,
                                    color = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.weight(1.0f)
                            )
                            
                            Text(
                                text = "৳${String.format(Locale.US, "%.2f", order.totalAmount)}",
                                fontSize = if (isA5) 11.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                    Divider(color = Color(0xFFF1F5F9))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom section: Split into Left Box (Amount in Words & Terms) and Right Box (Math Summary & Signature)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Word representation and Terms
                    Column(modifier = Modifier.weight(1.2f)) {
                        // Automated "Amount in Words" Section
                        val amountInWords = com.example.util.NumberToWords.convert(totalAmount, invoiceSettings.isBanglaMode)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .border(
                                    BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(if (isA5) 30.dp else 40.dp)
                                        .background(Color(0xFF0F172A), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (invoiceSettings.isBanglaMode) "কথায়:" else "IN WORDS:",
                                        fontSize = if (isA5) 8.sp else 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = amountInWords,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = if (isA5) 10.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Terms and Conditions Section
                        Text(
                            text = "TERMS & CONDITIONS",
                            fontSize = if (isA5) 8.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Please review invoice details upon receipt.\n" +
                                   "2. Standard delivery subject to schedule.\n" +
                                   "3. Thank you for your business!",
                            fontSize = if (isA5) 8.sp else 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = if (isA5) 11.sp else 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Right Column: Financial Summary Block
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Total Amount Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grand Total:",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isA5) 11.sp else 14.sp,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "৳${String.format(Locale.US, "%.2f", totalAmount)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isA5) 14.sp else 18.sp,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Advance Paid Row (with custom soft green badge background)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advance Paid:",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isA5) 11.sp else 13.sp,
                                color = Color(0xFF475569)
                            )
                            
                            // Let's frame the input inside a beautiful green badge style
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "৳",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isA5) 11.sp else 13.sp,
                                        color = Color(0xFF15803D)
                                    )
                                    BasicTextField(
                                        value = advanceAmountText,
                                        onValueChange = { 
                                            advanceAmountText = it
                                            val newAdvance = it.toDoubleOrNull() ?: 0.0
                                            if (orders.isNotEmpty()) {
                                                viewModel.updateOrderAdvance(orders.first().id, newAdvance)
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(if (isA5) 55.dp else 75.dp),
                                        textStyle = TextStyle(
                                            fontSize = if (isA5) 11.sp else 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End,
                                            color = Color(0xFF15803D)
                                        ),
                                        singleLine = true,
                                        cursorBrush = SolidColor(Color(0xFF15803D))
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Due Balance Row
                        val dueBgColor = if (dueAmount > 0) Color(0xFFFFFBEB) else Color(0xFFDCFCE7) // Amber/Yellow-50 vs Green-100
                        val dueTextColor = if (dueAmount > 0) Color(0xFFB45309) else Color(0xFF15803D) // Amber-700 vs Green-700
                        val dueBorderColor = if (dueAmount > 0) Color(0xFFFCD34D) else Color(0xFF86EFAC) // Amber-300 vs Green-300
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Due Amount:",
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isA5) 11.sp else 13.sp,
                                color = Color(0xFF475569)
                            )
                            Box(
                                modifier = Modifier
                                    .background(dueBgColor, RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, dueBorderColor), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "৳${String.format(Locale.US, "%.2f", dueAmount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isA5) 14.sp else 18.sp,
                                    color = dueTextColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isA5) 24.dp else 40.dp))

                // Footer Notes & Digital Signature Placeholder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "Sucharu Graphics Modern Management System",
                            fontSize = if (isA5) 8.sp else 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Signature block
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (isA5) 110.dp else 160.dp)
                                .height(if (isA5) 24.dp else 36.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Signature space placeholder
                        }
                        Text(
                            text = "...........................................",
                            fontSize = if (isA5) 8.sp else 10.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AUTHORIZED SIGNATURE",
                            fontSize = if (isA5) 8.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isA5) 16.dp else 24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        showPrintPreview = true
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = null,
                            modifier = Modifier.size(if (isA5) 16.dp else 20.dp),
                            tint = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Print",
                            fontSize = if (isA5) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val file = generateInvoicePdf(context, client, editableOrders, totalAmount, advanceAmount, date, isA5, invoiceSettings)
                            file?.let { pdfFile ->
                                viewModel.addInvoiceRecord(
                                    InvoiceRecord(
                                        clientId = client.id,
                                        date = date,
                                        totalAmount = totalAmount,
                                        advanceAmount = advanceAmount,
                                        filePath = pdfFile.absolutePath
                                    )
                                )
                                try {
                                    val authority = "${context.packageName}.provider"
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, pdfFile)
                                    val textMessage = "সম্মানিত গ্রাহক ${client.name},\nসুচারু গ্রাফিক্স থেকে আপনার ইনভয়েসটি সংযুক্ত করা হলো।\nতারিখ: $date\nমোট বিল: ৳$totalAmount\nঅগ্রিম: ৳$advanceAmount\nবকেয়া: ৳$dueAmount"
                                    
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, textMessage)
                                        putExtra("jid", "${client.mobile.replace(" ", "").replace("+", "").trim()}@s.whatsapp.net")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    
                                    shareIntent.setPackage("com.whatsapp")
                                    try {
                                        context.startActivity(shareIntent)
                                        viewModel.showNotification("হোয়াটসঅ্যাপে ইনভয়েস পাঠানো হচ্ছে...")
                                    } catch (ex: Exception) {
                                        shareIntent.setPackage(null)
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF via"))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    viewModel.showNotification("শেয়ার করতে সমস্যা হয়েছে: ${e.localizedMessage}")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(if (isA5) 16.dp else 20.dp),
                            tint = Color(0xFF25D366)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "WhatsApp Share",
                            fontSize = if (isA5) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showFormatDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(if (isA5) 16.dp else 20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download PDF",
                            fontSize = if (isA5) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ReorderableDialog(orders: List<Order>, onDismiss: () -> Unit, onSave: (List<Order>) -> Unit) {
    var editableOrders by remember { mutableStateOf(orders.toMutableStateList()) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Reorder Items", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(editableOrders) { index, order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.jobName, modifier = Modifier.weight(1f))
                            Row {
                                IconButton(onClick = {
                                    if (index > 0) {
                                        val temp = editableOrders[index]
                                        editableOrders[index] = editableOrders[index - 1]
                                        editableOrders[index - 1] = temp
                                    }
                                }) {
                                    Text("↑")
                                }
                                IconButton(onClick = {
                                    if (index < editableOrders.size - 1) {
                                        val temp = editableOrders[index]
                                        editableOrders[index] = editableOrders[index + 1]
                                        editableOrders[index + 1] = temp
                                    }
                                }) {
                                    Text("↓")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onSave(editableOrders) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(2.dp)
    ) {
        options.forEach { option ->
            val selected = option == selectedOption
            Box(
                modifier = Modifier
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        MaterialTheme.shapes.small
                    )
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PrintFormatSelectionDialog(onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var selectedFormat by remember { mutableStateOf("A4") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Print Format") },
        text = {
            Column {
                Text("Please select the desired page format for the PDF.")
                Spacer(modifier = Modifier.height(16.dp))
                SegmentedButton(
                    options = listOf("A4", "A5"),
                    selectedOption = selectedFormat,
                    onOptionSelected = { selectedFormat = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedFormat == "A5") }) {
                Text("Confirm & Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun InvoiceSettingsDialog(settings: InvoiceSettings, onSave: (InvoiceSettings) -> Unit, onDismiss: () -> Unit) {
    var showLetterhead by remember { mutableStateOf(settings.showLetterhead) }
    var showTaxInfo by remember { mutableStateOf(settings.showTaxInfo) }
    var showBankDetails by remember { mutableStateOf(settings.showBankDetails) }
    var qrCodeData by remember { mutableStateOf(settings.qrCodeData) }
    var isBanglaMode by remember { mutableStateOf(settings.isBanglaMode) }
    var logoUri by remember { mutableStateOf(settings.logoUri) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { logoUri = it.toString() }
        }
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invoice Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showLetterhead, onCheckedChange = { showLetterhead = it })
                    Text("Show Company Letterhead")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showTaxInfo, onCheckedChange = { showTaxInfo = it })
                    Text("Show Tax Information")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showBankDetails, onCheckedChange = { showBankDetails = it })
                    Text("Show Bank Account Details")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBanglaMode, onCheckedChange = { isBanglaMode = it })
                    Text("Bangla Mode (Numbers & Text)")
                }
                
                Divider()
                Text("Company Logo", style = MaterialTheme.typography.titleSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (logoUri != null) {
                        AsyncImage(
                            model = logoUri,
                            contentDescription = "Company Logo",
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No Logo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(onClick = {
                        logoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text(if (logoUri == null) "Select Logo" else "Change Logo")
                    }
                }
                if (logoUri != null) {
                    TextButton(onClick = { logoUri = null }) {
                        Text("Remove Logo", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = qrCodeData,
                    onValueChange = { qrCodeData = it },
                    label = { Text("QR Code URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(InvoiceSettings(showLetterhead, showTaxInfo, showBankDetails, qrCodeData, isBanglaMode, logoUri)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun InvoicePrintPreviewModal(
    client: com.example.data.Client,
    orders: List<com.example.data.Order>,
    totalAmount: Double,
    advanceAmount: Double,
    date: String,
    settings: InvoiceSettings,
    onDismiss: () -> Unit,
    onConfirmPrint: () -> Unit
) {
    val context = LocalContext.current
    val htmlContent = remember(client, orders, totalAmount, advanceAmount, date, settings) {
        HtmlPrintExporter.generateInvoiceHtml(context, client, orders, totalAmount, advanceAmount, date, settings)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        "Live Print Preview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onConfirmPrint,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm & Print")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Live Preview Canvas (WebView)
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = WebViewClient()
                            this.settings.apply {
                                javaScriptEnabled = false
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Gray.copy(alpha = 0.05f))
                )
                
                // Bottom Instruction
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "This preview accurately represents the physical paper output. UI buttons and controls are automatically hidden.",
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import android.content.Intent
import com.example.data.Client
import com.example.data.Order

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceEstimationScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val paperTypes by viewModel.paperTypes.collectAsState()
    var selectedPaperType by remember { mutableStateOf<com.example.data.PaperType?>(null) }
    var selectedSize by remember { mutableStateOf(PrintSize.A4) }
    var quantity by remember { mutableStateOf(1) }
    var clientName by remember { mutableStateOf("") }
    var clientMobile by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<Int?>(null) }
    var saveToDirectory by remember { mutableStateOf(false) }
    var showClientDialog by remember { mutableStateOf(false) }
    val clients by viewModel.clients.collectAsState()

    fun saveClientIfNeeded() {
        if (saveToDirectory && clientName.isNotBlank() && clientMobile.isNotBlank()) {
            val existing = clients.find { it.mobile == clientMobile }
            if (existing == null) {
                viewModel.addClient(
                    com.example.data.Client(
                        name = clientName,
                        mobile = clientMobile,
                        company = "",
                        position = "",
                        whatsapp = clientMobile,
                        email = "",
                        address = "",
                        specifications = "Saved from Price Estimator"
                    )
                )
            }
            saveToDirectory = false
        }
    }

    if (showClientDialog) {
        ClientSelectionDialog(
            clients = clients,
            onClientSelected = { client ->
                clientName = client.name
                clientMobile = client.mobile
                selectedClientId = client.id
                saveToDirectory = false
                showClientDialog = false
            },
            onDismiss = { showClientDialog = false }
        )
    }

    // Initialize selection when paperTypes load
    LaunchedEffect(paperTypes) {
        if (selectedPaperType == null && paperTypes.isNotEmpty()) {
            selectedPaperType = paperTypes.first()
        }
    }

    val totalPrice = remember(selectedPaperType, selectedSize, quantity) {
        val base = selectedPaperType?.basePrice ?: 0.0
        (base + selectedSize.addedCost) * quantity
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Price Estimator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedPaperType = paperTypes.firstOrNull()
                        selectedSize = PrintSize.A4
                        quantity = 1
                        clientName = ""
                        clientMobile = ""
                    }) {
                        Icon(Icons.Default.Calculate, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Client Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Client Information (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showClientDialog = true }) {
                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Select from Directory")
                        }
                    }
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { 
                            clientName = it
                            selectedClientId = null
                        },
                        label = { Text("Client Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = clientMobile,
                        onValueChange = { 
                            clientMobile = it
                            selectedClientId = null
                        },
                        label = { Text("Contact Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        shape = MaterialTheme.shapes.medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { saveToDirectory = !saveToDirectory },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveToDirectory,
                            onCheckedChange = { saveToDirectory = it }
                        )
                        Text(
                            "Save to Client Directory",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Header
            Text(
                text = "Customize Your Print",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Paper Type Section
            SectionHeader(title = "Paper Type")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(Modifier.selectableGroup()) {
                    paperTypes.forEach { type ->
                        RadioButtonRow(
                            label = type.name,
                            price = "৳${type.basePrice}/ea",
                            selected = (selectedPaperType?.id == type.id),
                            onClick = { selectedPaperType = type }
                        )
                    }
                }
            }

            // Print Size Section
            SectionHeader(title = "Print Size")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                PrintSize.values().forEach { size ->
                    FilterChip(
                        selected = selectedSize == size,
                        onClick = { selectedSize = size },
                        label = { Text(size.displayName) },
                        leadingIcon = if (selectedSize == size) {
                            { Icon(Icons.Default.Calculate, contentDescription = null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Quantity Section
            SectionHeader(title = "Quantity")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedIconButton(
                    onClick = { if (quantity > 1) quantity-- },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                
                AnimatedContent(
                    targetState = quantity,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                        } else {
                            slideInVertically { height -> -height } + fadeIn() togetherWith
                                    slideOutVertically { height -> height } + fadeOut()
                        }.using(
                            SizeTransform(clip = false)
                        )
                    }, label = "quantity_anim"
                ) { targetQuantity ->
                    Text(
                        text = targetQuantity.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.widthIn(min = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                OutlinedIconButton(
                    onClick = { quantity++ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        saveClientIfNeeded()
                        viewModel.insertPriceEstimate(
                            clientName = clientName.ifEmpty { "Valued Customer" },
                            clientMobile = clientMobile,
                            paperType = selectedPaperType?.name ?: "Unknown",
                            printSize = selectedSize.displayName,
                            quantity = quantity,
                            totalPrice = totalPrice
                        )

                        val text = """
                            *Printing Estimate from Sucharu Graphics*
                            ------------------------------------
                            Client: ${clientName.ifEmpty { "Valued Customer" }}
                            Contact: ${clientMobile.ifEmpty { "N/A" }}
                            ------------------------------------
                            Paper: ${selectedPaperType?.name ?: "Unknown"}
                            Size: ${selectedSize.displayName}
                            Quantity: $quantity
                            ------------------------------------
                            *Total Estimate: ৳${NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-BD")).format(totalPrice).replace("BDT", "").replace("$", "").trim()}*
                            
                            Generated on: ${java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date())}
                        """.trimIndent()
                        
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Quote")
                }

                Button(
                    onClick = {
                        saveClientIfNeeded()
                        viewModel.insertPriceEstimate(
                            clientName = clientName.ifEmpty { "Valued Customer" },
                            clientMobile = clientMobile,
                            paperType = selectedPaperType?.name ?: "Unknown",
                            printSize = selectedSize.displayName,
                            quantity = quantity,
                            totalPrice = totalPrice
                        )

                        // Generate PDF logic
                        val tempClient = com.example.data.Client(
                            name = clientName.ifEmpty { "Valued Customer" },
                            mobile = clientMobile,
                            company = "Estimation",
                            position = "",
                            whatsapp = clientMobile,
                            email = "",
                            address = "",
                            specifications = ""
                        )
                        val tempOrder = com.example.data.Order(
                            clientId = 0,
                            clientName = tempClient.name,
                            jobName = "Print Estimation",
                            category = selectedPaperType?.name ?: "Unknown",
                            quantity = quantity.toString(),
                            perUnitCost = ((selectedPaperType?.basePrice ?: 0.0) + selectedSize.addedCost),
                            totalAmount = totalPrice,
                            specifications = "${selectedPaperType?.name ?: "Unknown"}, ${selectedSize.displayName}",
                            timestamp = System.currentTimeMillis()
                        )
                        
                        val file = com.example.util.generateInvoicePdf(
                            context = context,
                            client = tempClient,
                            orders = listOf(tempOrder),
                            totalAmount = totalPrice,
                            advanceAmount = 0.0,
                            date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
                        )
                        
                        file?.let {
                            try {
                                val authority = "${context.packageName}.provider"
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, it)
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(viewIntent, "Open Estimate PDF"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF")
                }
            }

            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Estimated Total",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    AnimatedContent(
                        targetState = totalPrice,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)) togetherWith
                                    fadeOut(animationSpec = tween(90))
                        }, label = "price_anim"
                    ) { targetPrice ->
                        Text(
                            text = "৳" + String.format(Locale.US, "%.2f", targetPrice),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedContent(
                        targetState = "${selectedPaperType?.name ?: "Unknown"} • ${selectedSize.displayName} • x$quantity",
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }, label = "desc_anim"
                    ) { targetDesc ->
                        Text(
                            text = targetDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientSelectionDialog(
    clients: List<Client>,
    onClientSelected: (Client) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else clients.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.mobile.contains(searchQuery) ||
            it.company.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Client") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, phone or company...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                
                if (filteredClients.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No clients found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredClients.size) { index ->
                            val client = filteredClients[index]
                            ListItem(
                                headlineContent = { Text(client.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("${client.mobile} ${if (client.company.isNotBlank()) "• ${client.company}" else ""}") },
                                modifier = Modifier.clickable { onClientSelected(client) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                            )
                            if (index < filteredClients.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun RadioButtonRow(
    label: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp).weight(1f)
        )
        Text(
            text = price,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class PrintSize(val displayName: String, val addedCost: Double) {
    A4("A4 (Standard)", 0.0),
    A3("A3 (Large)", 5.0),
    POSTCARD("4x6 Postcard", -1.0),
    WALLET("Wallet Size", -1.5)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        maxItemsInEachRow = maxItemsInEachRow
    ) {
        content()
    }
}

package com.example.ui

import android.net.Uri
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.Client
import com.example.util.FileUtils
import android.widget.Toast
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(viewModel: MainViewModel, initialClientId: Int? = null) {
    val orders by viewModel.orders.collectAsState()
    var showInvoiceDialogForClient by remember { mutableStateOf<Client?>(null) }
    val clients by viewModel.clients.collectAsState()
    
    LaunchedEffect(initialClientId, clients) {
        if (initialClientId != null) {
            val client = clients.find { it.id == initialClientId }
            if (client != null) {
                showInvoiceDialogForClient = client
            }
        }
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredClients = remember(clients, orders, searchQuery, selectedFilter) {
        clients.filter { client ->
            val matchesSearch = client.name.contains(searchQuery, ignoreCase = true) ||
                    client.company.contains(searchQuery, ignoreCase = true) ||
                    client.mobile.contains(searchQuery)

            val clientOrders = orders.filter { it.clientId == client.id }
            val matchesFilter = when (selectedFilter) {
                "Active Orders" -> clientOrders.any { it.status != "Delivered" }
                "Due Unpaid" -> clientOrders.any { (it.totalAmount - it.advanceAmount) > 0 }
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val companyWhatsAppNumbers by viewModel.companyWhatsAppNumbers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Client")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Client Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("সার্চ করুন (Name, Company, Phone...)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            val filters = listOf(
                "All" to "সব কাস্টমার",
                "Active Orders" to "চলতি অর্ডার রয়েছে",
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
                    items(5) {
                        SkeletonClientCard()
                    }
                }
            } else if (filteredClients.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো কাস্টমার পাওয়া যায়নি।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        Box(modifier = Modifier.animateContentSize()) {
                            ClientCard(
                                client = client,
                                companyWhatsAppNumbers = companyWhatsAppNumbers,
                                onEditClick = { clientToEdit = client },
                                onViewInvoices = { showInvoiceDialogForClient = client }
                            )
                        }
                    }
                }
            }
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
        
        if (showAddDialog || clientToEdit != null) {
            ClientFormDialog(
                client = clientToEdit,
                viewModel = viewModel,
                onDismiss = {
                    showAddDialog = false
                    clientToEdit = null
                },
                onSave = { client ->
                    if (clientToEdit != null) {
                        viewModel.updateClient(client)
                    } else {
                        viewModel.addClient(client)
                    }
                    showAddDialog = false
                    clientToEdit = null
                }
            )
        }
    }
}

@Composable
fun ClientCard(client: Client, companyWhatsAppNumbers: List<String>, onEditClick: () -> Unit, onViewInvoices: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (client.profilePicUri != null && client.profilePicUri.isNotBlank()) {
                AsyncImage(
                    model = client.profilePicUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(56.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.padding(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(client.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (client.position.isNotBlank()) {
                    Text(client.position, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(client.company, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(client.mobile, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                }
            }
            if (client.whatsapp.isNotBlank()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                var showOutboundDialog by remember { mutableStateOf(false) }
                
                IconButton(onClick = { 
                    if (companyWhatsAppNumbers.isEmpty()) {
                        val url = "https://wa.me/${client.whatsapp}"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } else {
                        showOutboundDialog = true 
                    }
                }) {
                    Icon(Icons.Filled.Chat, contentDescription = "Live Chat / WhatsApp", tint = androidx.compose.ui.graphics.Color(0xFF25D366))
                }
                
                if (showOutboundDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showOutboundDialog = false },
                        title = { Text("Select Outbound Number") },
                        text = {
                            Column {
                                Text("Choose which company WhatsApp number to use for this chat:")
                                Spacer(modifier = Modifier.height(16.dp))
                                companyWhatsAppNumbers.forEach { number ->
                                    TextButton(onClick = {
                                        showOutboundDialog = false
                                        // The admin selected this number. We can copy it to clipboard or just proceed.
                                        android.widget.Toast.makeText(context, "Using sender: $number", android.widget.Toast.LENGTH_SHORT).show()
                                        val url = "https://wa.me/${client.whatsapp}"
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(number)
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showOutboundDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
            IconButton(onClick = onViewInvoices) {
                Icon(Icons.Filled.Receipt, contentDescription = "View Invoices")
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Client")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormDialog(client: Client?, viewModel: MainViewModel, onDismiss: () -> Unit, onSave: (Client) -> Unit) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var company by remember { mutableStateOf(client?.company ?: "") }
    var position by remember { mutableStateOf(client?.position ?: "") }
    var mobile by remember { mutableStateOf(client?.mobile ?: "") }
    var whatsapp by remember { mutableStateOf(client?.whatsapp ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var specifications by remember { mutableStateOf(client?.specifications ?: "") }
    var profilePicUri by remember { mutableStateOf<android.net.Uri?>(client?.profilePicUri?.let { if(it.isNotBlank()) android.net.Uri.parse(it) else null }) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val contactPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (nameIndex != -1) name = it.getString(nameIndex)
                        if (numberIndex != -1) {
                            val pickedNumber = it.getString(numberIndex)
                            mobile = pickedNumber
                            whatsapp = pickedNumber
                        }
                    }
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        try {
            val fileSize = FileUtils.getUriSize(context, uri)
            if (fileSize > 10 * 1024 * 1024) { // 10MB limit
                viewModel.showNotification("ফাইলটি ১০ মেগাবাইটের চেয়ে বড়, আপলোড করা সম্ভব নয়।")
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                try {
                    val compressedFile = FileUtils.compressImageFileAsync(context, uri)
                    if (compressedFile != null && compressedFile.exists()) {
                        profilePicUri = android.net.Uri.fromFile(compressedFile)
                    } else {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val file = java.io.File(context.filesDir, "client_pic_${System.currentTimeMillis()}.jpg")
                        inputStream?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        profilePicUri = android.net.Uri.fromFile(file)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    viewModel.showNotification("ফাইল আপলোড ব্যর্থ হয়েছে, আবার চেষ্টা করুন")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.showNotification("ফাইল আপলোড ব্যর্থ হয়েছে, আবার চেষ্টা করুন")
        }
    }

    val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        try {
            launcher.launch("image/*")
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.showNotification("ফাইল আপলোড ব্যর্থ হয়েছে, আবার চেষ্টা করুন")
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 32.dp)
                .imePadding(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (client == null) "Add New Client" else "Edit Client",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).clickable { 
                        permissionLauncher.launch(permissionsToRequest)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUri != null) {
                        AsyncImage(
                            model = profilePicUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Add Photo", modifier = Modifier.padding(32.dp))
                        }
                    }
                }
                Text("Tap to add photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { 
                        val intent = android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Contacts, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ফোনবুক থেকে নাম্বার নিন (Sync/Select Contact)")
                }
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("Designation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile 1 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp 2") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = specifications, onValueChange = { specifications = it }, label = { Text("Specifications") }, modifier = Modifier.fillMaxWidth())
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && mobile.isNotBlank()) {
                                val newClient = Client(
                                    id = client?.id ?: 0,
                                    name = name,
                                    company = company,
                                    position = position,
                                    mobile = mobile,
                                    whatsapp = whatsapp,
                                    email = email,
                                    address = address,
                                    specifications = specifications,
                                    profilePicUri = profilePicUri?.toString() ?: ""
                                )
                                onSave(newClient)
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import com.example.data.Client
import com.example.data.Order
import com.example.util.CalculatorEngine

@Composable
fun CalculatorScreen(viewModel: MainViewModel, onNavigateToOrders: () -> Unit = {}) {
    val clients by viewModel.clients.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Offset", "Digital", "Design", "Branding")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color(0xFF6750A4)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTabIndex) {
                0 -> ExpertOffsetCalculatorForm(viewModel, clients) { viewModel.addOrder(it); onNavigateToOrders() }
                1 -> DigitalPrintForm(viewModel, clients) { viewModel.addOrder(it); onNavigateToOrders() }
                2 -> DesignForm(clients) { viewModel.addOrder(it); onNavigateToOrders() }
                3 -> BrandingForm(viewModel, clients) { viewModel.addOrder(it); onNavigateToOrders() }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientSelection(clients: List<Client>, selectedClient: Client?, onClientSelected: (Client?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        OutlinedTextField(
            value = selectedClient?.name ?: "Select Customer (Optional)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Customer") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onClientSelected(null)
                    expanded = false
                }
            )
            clients.forEach { client ->
                DropdownMenuItem(
                    text = { Text(client.name) },
                    onClick = {
                        onClientSelected(client)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertOffsetCalculatorForm(viewModel: MainViewModel, clients: List<Client>, onSaveOrder: (com.example.data.Order) -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("General Items", "Multi-Part Book")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title) })
            }
        }

        when (selectedTabIndex) {
            0 -> GeneralItemsForm(viewModel, clients, onSaveOrder)
            1 -> MultiPartBookForm(viewModel, clients, onSaveOrder)
        }
    }
}

@Composable
fun GeneralItemsForm(viewModel: MainViewModel, clients: List<Client>, onSaveOrder: (Order) -> Unit) {
    OffsetPrintingForm(viewModel, clients, onSaveOrder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPartBookForm(viewModel: MainViewModel, clients: List<Client>, onSaveOrder: (com.example.data.Order) -> Unit) {
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    val dynamicPaperTypes by viewModel.paperTypes.collectAsState()

    // Core inputs
    var quantity by remember { mutableStateOf("1000") }
    var totalPages by remember { mutableStateOf("160") }
    var formaBasis by remember { mutableStateOf("16") } // "8" or "16"
    var innerPaperGsm by remember { mutableStateOf("80 GSM") }
    var coverCardGsm by remember { mutableStateOf("300 GSM") }

    // Step A: Inner Pages variables
    var innerPaperRate by remember { mutableStateOf("3.0") }
    var innerColors by remember { mutableStateOf("1") }
    var plateRate by remember { mutableStateOf("150.0") }
    var printingRate by remember { mutableStateOf("250.0") }
    var bindingBaseRate by remember { mutableStateOf("10.0") }
    var bindingPerFormaRate by remember { mutableStateOf("1.5") }
    var innerWastageSheets by remember { mutableStateOf("50") }

    // Step B: Cover variables
    var coverPaperRate by remember { mutableStateOf("12.0") }
    var coverColors by remember { mutableStateOf("4") }
    var laminationRate by remember { mutableStateOf("3.0") }
    var coverDieBlockRate by remember { mutableStateOf("300.0") }
    var coverDieCuttingRate by remember { mutableStateOf("500.0") }
    var spotUvFoilRate by remember { mutableStateOf("2.0") }
    var coverWastageSheets by remember { mutableStateOf("50") }

    // Step C: Pustany variables
    var includePustany by remember { mutableStateOf(true) }
    var pustanyPaperRate by remember { mutableStateOf("2.5") }
    var pustanyWastageSheets by remember { mutableStateOf("20") }

    // Global variables
    var designCharge by remember { mutableStateOf("500") }
    var profitPercent by remember { mutableStateOf("20") }

    // Results state
    var calculated by remember { mutableStateOf(false) }
    var totalInnerCost by remember { mutableStateOf(0.0) }
    var totalCoverCost by remember { mutableStateOf(0.0) }
    var totalPustanyCost by remember { mutableStateOf(0.0) }
    var grandTotalCost by remember { mutableStateOf(0.0) }
    var profitAmount by remember { mutableStateOf(0.0) }
    var finalPrice by remember { mutableStateOf(0.0) }
    var perUnitCost by remember { mutableStateOf(0.0) }
    var totalFormasResult by remember { mutableStateOf(0.0) }

    val formatter = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2

    // Filter paper types for inner and cover if needed, or just use all
    val paperTypeOptions = dynamicPaperTypes.map { it.name }.ifEmpty { listOf("Standard Copy", "Glossy Photo", "Matte Cardstock", "Art Card 300 GSM") }

    var innerGsmExpanded by remember { mutableStateOf(false) }
    var coverGsmExpanded by remember { mutableStateOf(false) }
    var formaBasisExpanded by remember { mutableStateOf(false) }

    // Initialize selections
    LaunchedEffect(paperTypeOptions) {
        if (innerPaperGsm == "80 GSM" && !paperTypeOptions.contains("80 GSM")) {
            innerPaperGsm = paperTypeOptions.firstOrNull() ?: "Standard Copy"
        }
        if (coverCardGsm == "300 GSM" && !paperTypeOptions.contains("300 GSM")) {
            coverCardGsm = paperTypeOptions.firstOrNull() ?: "Art Card 300 GSM"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ClientSelection(clients = clients, selectedClient = selectedClient, onClientSelected = { selectedClient = it })

        // Card 1: Book Specifications
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Book Specifications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Total Quantity", quantity, Modifier.weight(1f)) { quantity = it }
                    NumberField("Total Pages", totalPages, Modifier.weight(1f)) { totalPages = it }
                }

                // Forma Basis Dropdown
                ExposedDropdownMenuBox(
                    expanded = formaBasisExpanded,
                    onExpandedChange = { formaBasisExpanded = !formaBasisExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "$formaBasis pages per forma",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Forma Basis Selection") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formaBasisExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ExposedDropdownMenu(expanded = formaBasisExpanded, onDismissRequest = { formaBasisExpanded = false }) {
                        listOf("8", "16").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text("$selectionOption pages") },
                                onClick = { formaBasis = selectionOption; formaBasisExpanded = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Inner Paper GSM
                    ExposedDropdownMenuBox(
                        expanded = innerGsmExpanded,
                        onExpandedChange = { innerGsmExpanded = !innerGsmExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = innerPaperGsm,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Inner Paper GSM") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = innerGsmExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        ExposedDropdownMenu(expanded = innerGsmExpanded, onDismissRequest = { innerGsmExpanded = false }) {
                            paperTypeOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = { 
                                        innerPaperGsm = selectionOption
                                        dynamicPaperTypes.find { it.name == selectionOption }?.let {
                                            innerPaperRate = it.basePrice.toString()
                                        }
                                        innerGsmExpanded = false 
                                    }
                                )
                            }
                        }
                    }

                    // Cover Card GSM
                    ExposedDropdownMenuBox(
                        expanded = coverGsmExpanded,
                        onExpandedChange = { coverGsmExpanded = !coverGsmExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = coverCardGsm,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cover Paper Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = coverGsmExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        ExposedDropdownMenu(expanded = coverGsmExpanded, onDismissRequest = { coverGsmExpanded = false }) {
                            paperTypeOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = { 
                                        coverCardGsm = selectionOption
                                        dynamicPaperTypes.find { it.name == selectionOption }?.let {
                                            coverPaperRate = it.basePrice.toString()
                                        }
                                        coverGsmExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Step A (Inner Pages Cost Details)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Step A: Inner Pages Config", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Paper Rate/Sheet (৳)", innerPaperRate, Modifier.weight(1f)) { innerPaperRate = it }
                    NumberField("Inner Colors (1-4)", innerColors, Modifier.weight(1f)) { innerColors = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Plate Rate (৳)", plateRate, Modifier.weight(1f)) { plateRate = it }
                    NumberField("Printing Rate/1k (৳)", printingRate, Modifier.weight(1f)) { printingRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Binding Base/Book", bindingBaseRate, Modifier.weight(1f)) { bindingBaseRate = it }
                    NumberField("Binding Rate/Forma", bindingPerFormaRate, Modifier.weight(1f)) { bindingPerFormaRate = it }
                }
                NumberField("Wastage Extra Sheets/Forma", innerWastageSheets, Modifier.fillMaxWidth()) { innerWastageSheets = it }
            }
        }

        // Card 3: Step B (Cover Config)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Step B: Cover Configuration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Cover Paper Rate (৳)", coverPaperRate, Modifier.weight(1f)) { coverPaperRate = it }
                    NumberField("Cover Colors (1-4)", coverColors, Modifier.weight(1f)) { coverColors = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Lamination Rate/Bk", laminationRate, Modifier.weight(1f)) { laminationRate = it }
                    NumberField("Die Block Cost (৳)", coverDieBlockRate, Modifier.weight(1f)) { coverDieBlockRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Die Cut Cost (৳)", coverDieCuttingRate, Modifier.weight(1f)) { coverDieCuttingRate = it }
                    NumberField("Spot UV/Foil Cost", spotUvFoilRate, Modifier.weight(1f)) { spotUvFoilRate = it }
                }
                NumberField("Wastage Extra Cover Sheets", coverWastageSheets, Modifier.fillMaxWidth()) { coverWastageSheets = it }
            }
        }

        // Card 4: Step C (Pustany/Endpaper Config)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Step C: Pustany / Endpaper", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Switch(checked = includePustany, onCheckedChange = { includePustany = it })
                }
                
                if (includePustany) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField("Pustany Paper Rate", pustanyPaperRate, Modifier.weight(1f)) { pustanyPaperRate = it }
                        NumberField("Wastage Extra Sheets", pustanyWastageSheets, Modifier.weight(1f)) { pustanyWastageSheets = it }
                    }
                }
            }
        }

        // Card 5: General Pricing
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pricing Config & Overheads", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Design Charge (৳)", designCharge, Modifier.weight(1f)) { designCharge = it }
                    NumberField("Profit Margin (%)", profitPercent, Modifier.weight(1f)) { profitPercent = it }
                }
            }
        }

        // Calculate Button
        Button(
            onClick = {
                val qty = quantity.toDoubleOrNull() ?: 1000.0
                val pages = totalPages.toDoubleOrNull() ?: 160.0
                val fBasis = formaBasis.toDoubleOrNull() ?: 16.0

                // Step A Calculations
                val totalFormas = kotlin.math.ceil(pages / fBasis)
                totalFormasResult = totalFormas
                val innerWastage = innerWastageSheets.toDoubleOrNull() ?: 50.0
                val innerSheets = (qty * totalFormas * (fBasis / 16.0)) + (innerWastage * totalFormas)
                val paperRateInner = innerPaperRate.toDoubleOrNull() ?: 3.0
                val paperInnerCost = innerSheets * paperRateInner

                val colsInner = innerColors.toDoubleOrNull() ?: 1.0
                val pltRate = plateRate.toDoubleOrNull() ?: 150.0
                val platesInner = totalFormas * colsInner * 2.0
                val plateInnerCost = platesInner * pltRate

                val printRateInner = printingRate.toDoubleOrNull() ?: 250.0
                val impressionsInner = qty * totalFormas * 2.0
                val printInnerCost = kotlin.math.max(1.0, kotlin.math.ceil(impressionsInner / 1000.0)) * printRateInner * colsInner

                val bindBase = bindingBaseRate.toDoubleOrNull() ?: 10.0
                val bindPerForma = bindingPerFormaRate.toDoubleOrNull() ?: 1.5
                val bindingCost = qty * (bindBase + (totalFormas * bindPerForma))

                totalInnerCost = paperInnerCost + plateInnerCost + printInnerCost + bindingCost

                // Step B Calculations
                val coverWastage = coverWastageSheets.toDoubleOrNull() ?: 50.0
                val coverSheets = kotlin.math.ceil(qty / 4.0) + coverWastage
                val paperCoverCost = coverSheets * (coverPaperRate.toDoubleOrNull() ?: 12.0)

                val colsCover = coverColors.toDoubleOrNull() ?: 4.0
                val platesCover = colsCover * 1.0
                val plateCoverCost = platesCover * pltRate

                val printCoverCost = kotlin.math.max(1.0, kotlin.math.ceil(qty / 1000.0)) * printRateInner * colsCover
                val lamCoverCost = qty * (laminationRate.toDoubleOrNull() ?: 3.0)
                val dieBlkCoverCost = coverDieBlockRate.toDoubleOrNull() ?: 300.0
                val dieCutCoverCost = coverDieCuttingRate.toDoubleOrNull() ?: 500.0
                val spotUvFoilCost = qty * (spotUvFoilRate.toDoubleOrNull() ?: 2.0)

                totalCoverCost = paperCoverCost + plateCoverCost + printCoverCost + lamCoverCost + dieBlkCoverCost + dieCutCoverCost + spotUvFoilCost

                // Step C Calculations
                if (includePustany) {
                    val pustanyWastage = pustanyWastageSheets.toDoubleOrNull() ?: 20.0
                    val pustanySheets = kotlin.math.ceil(qty / 2.0) + pustanyWastage
                    totalPustanyCost = pustanySheets * (pustanyPaperRate.toDoubleOrNull() ?: 2.5)
                } else {
                    totalPustanyCost = 0.0
                }

                // Consolidation
                grandTotalCost = totalInnerCost + totalCoverCost + totalPustanyCost
                val desChrg = designCharge.toDoubleOrNull() ?: 500.0
                val profPct = profitPercent.toDoubleOrNull() ?: 20.0

                val costWithDesign = grandTotalCost + desChrg
                profitAmount = costWithDesign * (profPct / 100.0)
                finalPrice = costWithDesign + profitAmount
                perUnitCost = if (qty > 0) finalPrice / qty else 0.0

                calculated = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedClient != null
        ) {
            Text(if (selectedClient == null) "Select Customer First" else "Calculate Multi-Part Book Cost", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (calculated) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Consolidated Book Calculation Result", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                    
                    Divider(color = Color(0xFF21005D).copy(alpha = 0.2f))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Formas:", color = Color(0xFF21005D))
                        Text("${totalFormasResult.toInt()} Formas", fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Step A: Inner Pages Cost:", color = Color(0xFF21005D))
                        Text("৳ ${formatter.format(totalInnerCost)}", fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Step B: Cover Cost:", color = Color(0xFF21005D))
                        Text("৳ ${formatter.format(totalCoverCost)}", fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Step C: Pustany Cost:", color = Color(0xFF21005D))
                        Text("৳ ${formatter.format(totalPustanyCost)}", fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
                    }
                    
                    Divider(color = Color(0xFF21005D).copy(alpha = 0.2f))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Production Cost:", color = Color(0xFF21005D))
                        Text("৳ ${formatter.format(grandTotalCost)}", fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Profit Margin (${profitPercent}%):", color = Color(0xFF21005D))
                        Text("৳ ${formatter.format(profitAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                    }
                    
                    Divider(color = Color(0xFF21005D).copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Final Price:", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF21005D))
                        Text("৳ ${formatter.format(finalPrice)}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF21005D))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Per Unit Book Billing:", fontSize = 12.sp, color = Color(0xFF21005D).copy(alpha = 0.8f))
                        Text("৳ ${formatter.format(perUnitCost)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D).copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    selectedClient?.let {
                        onSaveOrder(
                            com.example.data.Order(
                                clientId = it.id,
                                clientName = it.name,
                                jobName = "Multi-Part Book (Pages: $totalPages)",
                                category = "Offset",
                                specifications = "Inner: $innerPaperGsm, Cover: $coverCardGsm, Formas: ${totalFormasResult.toInt()}",
                                quantity = quantity,
                                perUnitCost = perUnitCost,
                                totalAmount = finalPrice,
                                profitAmount = profitAmount
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send to New Job / Save Order", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffsetPrintingForm(viewModel: MainViewModel, clients: List<Client>, onSaveOrder: (com.example.data.Order) -> Unit) {
    val categories = listOf(
        "Visiting Card", "Literature", "Flyer", "Leaflet", "Brochure", 
        "Poster", "Sticker", "Invoice Book", "Money Receipt", "Pad", "Envelope", 
        "Calendar", "Diary", "Notebook", "Menu Card", "Wedding Card", 
        "Invitation Card", "Invitation Card & Envelope", "Packaging Design", 
        "Logo Design", "Social Media Design", "Facebook Cover", "YouTube Thumbnail", 
        "Certificate", "CV Design"
    )
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ClientSelection(clients = clients, selectedClient = selectedClient, onClientSelected = { selectedClient = it })
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Offset Item Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedCategory = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }

        VisitingCardCalculator(viewModel, selectedCategory, selectedClient, onSaveOrder)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitingCardCalculator(
    viewModel: MainViewModel,
    category: String,
    selectedClient: Client?,
    onSaveOrder: (com.example.data.Order) -> Unit
) {
    val dynamicPaperTypes by viewModel.paperTypes.collectAsState()
    var expandedPaper by remember { mutableStateOf(false) }
    var selectedPaper by remember { mutableStateOf("") }
    
    // Results
    var totalCost by remember { mutableStateOf(0.0) }
    var subTotal1 by remember { mutableStateOf(0.0) } // Paper & Specs
    var subTotal2 by remember { mutableStateOf(0.0) } // Printing & Plates
    var subTotal3 by remember { mutableStateOf(0.0) } // Finishing
    var profitAmount by remember { mutableStateOf(0.0) }
    var finalPrice by remember { mutableStateOf(0.0) }
    var perUnitCost by remember { mutableStateOf(0.0) }
    var calculated by remember { mutableStateOf(false) }

    // Initialize selection
    LaunchedEffect(dynamicPaperTypes) {
        if (selectedPaper.isEmpty() && dynamicPaperTypes.isNotEmpty()) {
            selectedPaper = dynamicPaperTypes.first().name
        }
    }

    // Inputs (Using strings to handle empty states gracefully)
    var quantity by remember { mutableStateOf("1000") }
    var extra by remember { mutableStateOf("100") }
    var perSheet by remember { mutableStateOf("10") }
    var paperRate by remember { mutableStateOf("2.5") }
    
    var width by remember { mutableStateOf("3.25") }
    var height by remember { mutableStateOf("2.0") }
    var color by remember { mutableStateOf("4") }
    var side by remember { mutableStateOf("2") }
    var part by remember { mutableStateOf("1") }
    
    var positiveRate by remember { mutableStateOf("1.5") }
    var impression by remember { mutableStateOf("1000") }
    var plateRate by remember { mutableStateOf("150") }
    var printingRate by remember { mutableStateOf("250") }
    
    var laminationRate by remember { mutableStateOf("0.5") }
    var dieBlockRate by remember { mutableStateOf("10") }
    var dieCuttingRate by remember { mutableStateOf("100") }
    
    var paperCuttingRate by remember { mutableStateOf("20") }
    var foilRate by remember { mutableStateOf("5") }
    var perPacket by remember { mutableStateOf("100") }
    var packingRate by remember { mutableStateOf("5") }
    var deliveryRate by remember { mutableStateOf("20") }
    var overheadRate by remember { mutableStateOf("0.1") }
    
    var designCharge by remember { mutableStateOf("500") }
    var profitPercent by remember { mutableStateOf("20") }

    val formatter = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paper & Specifications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                ExposedDropdownMenuBox(
                    expanded = expandedPaper,
                    onExpandedChange = { expandedPaper = !expandedPaper },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPaper,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paper Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaper) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedPaper, onDismissRequest = { expandedPaper = false }) {
                        dynamicPaperTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption.name) },
                                onClick = { 
                                    selectedPaper = selectionOption.name
                                    paperRate = selectionOption.basePrice.toString()
                                    expandedPaper = false 
                                }
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Quantity", quantity, Modifier.weight(1f)) { quantity = it }
                    NumberField("Extra", extra, Modifier.weight(1f)) { extra = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Per Sheet", perSheet, Modifier.weight(1f)) { perSheet = it }
                    NumberField("Paper Rate", paperRate, Modifier.weight(1f)) { paperRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Width (in)", width, Modifier.weight(1f)) { width = it }
                    NumberField("Height (in)", height, Modifier.weight(1f)) { height = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Color", color, Modifier.weight(1f)) { color = it }
                    NumberField("Side", side, Modifier.weight(1f)) { side = it }
                }
                NumberField("Part", part, Modifier.fillMaxWidth()) { part = it }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Printing & Plates", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Impression", impression, Modifier.weight(1f)) { impression = it }
                    NumberField("Positive Rate", positiveRate, Modifier.weight(1f)) { positiveRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Plate Rate", plateRate, Modifier.weight(1f)) { plateRate = it }
                    NumberField("Printing Rate", printingRate, Modifier.weight(1f)) { printingRate = it }
                }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Finishing & Charges", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Lamination Rate", laminationRate, Modifier.weight(1f)) { laminationRate = it }
                    NumberField("Die Block Rate/in", dieBlockRate, Modifier.weight(1f)) { dieBlockRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Die Cut Rate", dieCuttingRate, Modifier.weight(1f)) { dieCuttingRate = it }
                    NumberField("Paper Cut Rate", paperCuttingRate, Modifier.weight(1f)) { paperCuttingRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Foil Rate/in", foilRate, Modifier.weight(1f)) { foilRate = it }
                    NumberField("Per Packet", perPacket, Modifier.weight(1f)) { perPacket = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Packing Rate", packingRate, Modifier.weight(1f)) { packingRate = it }
                    NumberField("Delivery Rate", deliveryRate, Modifier.weight(1f)) { deliveryRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Overhead Rate", overheadRate, Modifier.weight(1f)) { overheadRate = it }
                    NumberField("Design Charge", designCharge, Modifier.weight(1f)) { designCharge = it }
                }
                NumberField("Profit Margin (%)", profitPercent, Modifier.fillMaxWidth()) { profitPercent = it }
            }
        }
        
        Button(
            onClick = {
                val q = quantity.toDoubleOrNull() ?: 0.0
                val ext = extra.toDoubleOrNull() ?: 0.0
                val ps = perSheet.toDoubleOrNull() ?: 1.0
                val pr = paperRate.toDoubleOrNull() ?: 0.0
                
                val w = width.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                val c = color.toDoubleOrNull() ?: 0.0
                val s = side.toDoubleOrNull() ?: 0.0
                val pt = part.toDoubleOrNull() ?: 1.0
                
                val posR = positiveRate.toDoubleOrNull() ?: 0.0
                val imp = impression.toDoubleOrNull() ?: 1.0
                val pltR = plateRate.toDoubleOrNull() ?: 0.0
                val printR = printingRate.toDoubleOrNull() ?: 0.0
                
                val lamR = laminationRate.toDoubleOrNull() ?: 0.0
                val dbR = dieBlockRate.toDoubleOrNull() ?: 0.0
                val dcR = dieCuttingRate.toDoubleOrNull() ?: 0.0
                val pcR = paperCuttingRate.toDoubleOrNull() ?: 0.0
                val fR = foilRate.toDoubleOrNull() ?: 0.0
                val pP = perPacket.toDoubleOrNull() ?: 1.0
                val pkR = packingRate.toDoubleOrNull() ?: 0.0
                val delR = deliveryRate.toDoubleOrNull() ?: 0.0
                val ovR = overheadRate.toDoubleOrNull() ?: 0.0
                
                val desC = designCharge.toDoubleOrNull() ?: 0.0
                val prof = profitPercent.toDoubleOrNull() ?: 0.0
                
                // Full formulas based on SRS
                val totalSheets = kotlin.math.ceil((q + ext) / ps)
                val paperCostVal = totalSheets * pr
                val sidesFactor = if (s > 0) s else 1.0
                val positiveCostVal = w * h * c * sidesFactor * posR
                val platesNeeded = kotlin.math.max(1.0, c * sidesFactor)
                val plateCostVal = platesNeeded * pltR
                val totalImpressions = totalSheets * pt * sidesFactor
                val printingCostVal = kotlin.math.max(1.0, kotlin.math.ceil(totalImpressions / 1000.0)) * printR * c
                val laminationCostVal = w * h * totalSheets * sidesFactor * lamR
                val dieBlockCostVal = w * h * dbR
                val dieCuttingCostVal = kotlin.math.max(1.0, kotlin.math.ceil(q / 1000.0)) * dcR
                val paperCuttingCostVal = kotlin.math.max(1.0, kotlin.math.ceil(totalSheets / 500.0)) * pcR
                val foilCostVal = w * h * fR
                val packingCostVal = kotlin.math.max(1.0, kotlin.math.ceil(q / pP)) * pkR
                val deliveryCostVal = delR
                val overheadCostVal = q * ovR
                val desCVal = desC

                subTotal1 = paperCostVal
                subTotal2 = positiveCostVal + plateCostVal + printingCostVal
                subTotal3 = laminationCostVal + dieBlockCostVal + dieCuttingCostVal + paperCuttingCostVal + foilCostVal + packingCostVal + deliveryCostVal + overheadCostVal + desCVal
                
                totalCost = subTotal1 + subTotal2 + subTotal3
                profitAmount = totalCost * (prof / 100.0)
                finalPrice = totalCost + profitAmount
                perUnitCost = if (q > 0) finalPrice / q else 0.0
                
                calculated = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedClient != null
        ) {
            Text(if (selectedClient == null) "Select Customer First" else "Calculate Cost", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        if (calculated) {
            ResultCard(totalCost, profitPercent.toDoubleOrNull() ?: 0.0, profitAmount, finalPrice, perUnitCost, formatter, subTotal1, subTotal2, subTotal3)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    selectedClient?.let {
                        onSaveOrder(
                            com.example.data.Order(
                                clientId = it.id,
                                clientName = it.name,
                                jobName = category,
                                category = "Offset",
                                specifications = "$selectedPaper, Size: ${width}x${height}, Color: $color, Side: $side",
                                quantity = quantity,
                                perUnitCost = perUnitCost,
                                totalAmount = finalPrice,
                                profitAmount = profitAmount
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send to New Job", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun BookCalculator(viewModel: MainViewModel, selectedClient: Client?, onSaveOrder: (com.example.data.Order) -> Unit) {
    var selectedBookPartIndex by remember { mutableStateOf(0) }
    val bookParts = listOf("Inner", "Cover", "Pustany")
    
    val dynamicPaperTypes by viewModel.paperTypes.collectAsState()
    val paperTypeOptions = dynamicPaperTypes.map { it.name }.ifEmpty { listOf("Offset Paper 80 gsm", "Art Card 300 gsm") }

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TabRow(
            selectedTabIndex = selectedBookPartIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color(0xFF6750A4)
        ) {
            bookParts.forEachIndexed { index, title ->
                Tab(
                    selected = selectedBookPartIndex == index,
                    onClick = { selectedBookPartIndex = index },
                    text = { Text(title, fontWeight = if (selectedBookPartIndex == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
            }
        }

        when (selectedBookPartIndex) {
            0 -> BookPartCalculator(partName = "Inner", paperTypes = paperTypeOptions, dynamicPaperTypes = dynamicPaperTypes, selectedClient = selectedClient, onSaveOrder = onSaveOrder)
            1 -> BookPartCalculator(partName = "Cover", paperTypes = paperTypeOptions, dynamicPaperTypes = dynamicPaperTypes, selectedClient = selectedClient, onSaveOrder = onSaveOrder)
            2 -> BookPartCalculator(partName = "Pustany", paperTypes = paperTypeOptions, dynamicPaperTypes = dynamicPaperTypes, selectedClient = selectedClient, onSaveOrder = onSaveOrder)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPartCalculator(partName: String, paperTypes: List<String>, dynamicPaperTypes: List<com.example.data.PaperType>, selectedClient: Client?, onSaveOrder: (com.example.data.Order) -> Unit) {
    var expandedPaper by remember { mutableStateOf(false) }
    var selectedPaper by remember { mutableStateOf(paperTypes.firstOrNull() ?: "") }
    
    // Inputs (Using strings to handle empty states gracefully)
    var quantity by remember { mutableStateOf("1000") }
    var formaQuantity by remember { mutableStateOf("8") }
    
    var extra by remember { mutableStateOf("100") }
    var perSheet by remember { mutableStateOf("10") }
    var paperRate by remember { mutableStateOf("2.5") }
    
    var width by remember { mutableStateOf("3.25") }
    var height by remember { mutableStateOf("2.0") }
    var color by remember { mutableStateOf("4") }
    var side by remember { mutableStateOf("2") }
    
    var positiveRate by remember { mutableStateOf("1.5") }
    var impression by remember { mutableStateOf("1000") }
    var plateRate by remember { mutableStateOf("150") }
    var printingRate by remember { mutableStateOf("250") }
    
    var laminationRate by remember { mutableStateOf("0.5") }
    var dieBlockRate by remember { mutableStateOf("10") }
    var dieCuttingRate by remember { mutableStateOf("100") }
    
    var paperCuttingRate by remember { mutableStateOf("20") }
    var foilRate by remember { mutableStateOf("5") }
    var bindingRate by remember { mutableStateOf("10") }
    var perPacket by remember { mutableStateOf("100") }
    var packingRate by remember { mutableStateOf("5") }
    var deliveryRate by remember { mutableStateOf("20") }
    var overheadRate by remember { mutableStateOf("0.1") }
    
    var designCharge by remember { mutableStateOf("500") }
    var profitPercent by remember { mutableStateOf("20") }
    
    // Results
    var totalCost by remember { mutableStateOf(0.0) }
    var profitAmount by remember { mutableStateOf(0.0) }
    var finalPrice by remember { mutableStateOf(0.0) }
    var perUnitCost by remember { mutableStateOf(0.0) }
    var calculated by remember { mutableStateOf(false) }

    val formatter = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$partName Specifications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                ExposedDropdownMenuBox(
                    expanded = expandedPaper,
                    onExpandedChange = { expandedPaper = !expandedPaper },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPaper,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paper Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaper) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedPaper, onDismissRequest = { expandedPaper = false }) {
                        paperTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = { 
                                    selectedPaper = selectionOption
                                    dynamicPaperTypes.find { it.name == selectionOption }?.let {
                                        paperRate = it.basePrice.toString()
                                    }
                                    expandedPaper = false 
                                }
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Quantity", quantity, Modifier.weight(1f)) { quantity = it }
                    NumberField("Forma", formaQuantity, Modifier.weight(1f)) { formaQuantity = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Extra", extra, Modifier.weight(1f)) { extra = it }
                    NumberField("Per Sheet", perSheet, Modifier.weight(1f)) { perSheet = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Paper Rate", paperRate, Modifier.weight(1f)) { paperRate = it }
                    NumberField("Width (in)", width, Modifier.weight(1f)) { width = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Height (in)", height, Modifier.weight(1f)) { height = it }
                    NumberField("Color", color, Modifier.weight(1f)) { color = it }
                }
                NumberField("Side", side, Modifier.fillMaxWidth()) { side = it }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Printing & Plates", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Impression", impression, Modifier.weight(1f)) { impression = it }
                    NumberField("Positive Rate", positiveRate, Modifier.weight(1f)) { positiveRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Plate Rate", plateRate, Modifier.weight(1f)) { plateRate = it }
                    NumberField("Printing Rate", printingRate, Modifier.weight(1f)) { printingRate = it }
                }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Finishing & Charges", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Lamination Rate", laminationRate, Modifier.weight(1f)) { laminationRate = it }
                    NumberField("Die Block Rate/in", dieBlockRate, Modifier.weight(1f)) { dieBlockRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Die Cut Rate", dieCuttingRate, Modifier.weight(1f)) { dieCuttingRate = it }
                    NumberField("Paper Cut Rate", paperCuttingRate, Modifier.weight(1f)) { paperCuttingRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Foil Rate/in", foilRate, Modifier.weight(1f)) { foilRate = it }
                    NumberField("Binding Rate", bindingRate, Modifier.weight(1f)) { bindingRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Per Packet", perPacket, Modifier.weight(1f)) { perPacket = it }
                    NumberField("Packing Rate", packingRate, Modifier.weight(1f)) { packingRate = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Delivery Rate", deliveryRate, Modifier.weight(1f)) { deliveryRate = it }
                    NumberField("Overhead Rate", overheadRate, Modifier.weight(1f)) { overheadRate = it }
                }
                NumberField("Design Charge", designCharge, Modifier.fillMaxWidth()) { designCharge = it }
                NumberField("Profit Margin (%)", profitPercent, Modifier.fillMaxWidth()) { profitPercent = it }
            }
        }
        
        Button(
            onClick = {
                val q = quantity.toDoubleOrNull() ?: 0.0
                val fq = formaQuantity.toDoubleOrNull() ?: 1.0
                val ext = extra.toDoubleOrNull() ?: 0.0
                val ps = perSheet.toDoubleOrNull() ?: 1.0
                val pr = paperRate.toDoubleOrNull() ?: 0.0
                
                val w = width.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                val c = color.toDoubleOrNull() ?: 0.0
                val s = side.toDoubleOrNull() ?: 0.0
                
                val posR = positiveRate.toDoubleOrNull() ?: 0.0
                val imp = impression.toDoubleOrNull() ?: 1.0
                val pltR = plateRate.toDoubleOrNull() ?: 0.0
                val printR = printingRate.toDoubleOrNull() ?: 0.0
                
                val lamR = laminationRate.toDoubleOrNull() ?: 0.0
                val dbR = dieBlockRate.toDoubleOrNull() ?: 0.0
                val dcR = dieCuttingRate.toDoubleOrNull() ?: 0.0
                val pcR = paperCuttingRate.toDoubleOrNull() ?: 0.0
                val fR = foilRate.toDoubleOrNull() ?: 0.0
                val bindR = bindingRate.toDoubleOrNull() ?: 0.0
                val pP = perPacket.toDoubleOrNull() ?: 1.0
                val pkR = packingRate.toDoubleOrNull() ?: 0.0
                val delR = deliveryRate.toDoubleOrNull() ?: 0.0
                val ovR = overheadRate.toDoubleOrNull() ?: 0.0
                
                val desC = designCharge.toDoubleOrNull() ?: 0.0
                val prof = profitPercent.toDoubleOrNull() ?: 0.0
                
                // Full formulas based on SRS, factoring in forma for book parts
                val totalSheets = (q * fq + ext) / ps
                val paperCost = totalSheets * pr
                val positiveCost = w * h * c * posR
                val plates = (totalSheets / imp) // simplified logic
                val plateCost = plates * c * pltR
                val printingCost = (totalSheets * c * s) / imp * printR
                val laminationCost = w * h * totalSheets * s * lamR
                val dieBlockCost = w * h * dbR
                val dieCuttingCost = (q / 1000.0) * dcR
                val paperCuttingCost = totalSheets * pcR // assuming rim=1 for simplicity
                val foilCost = w * h * fR
                val bindingCost = q * bindR
                val packingCost = (q / pP) * pkR
                val deliveryCost = pP * delR
                val overheadCost = q * ovR
                
                totalCost = paperCost + positiveCost + plateCost + printingCost + laminationCost + dieBlockCost + dieCuttingCost + paperCuttingCost + foilCost + bindingCost + packingCost + deliveryCost + overheadCost + desC
                profitAmount = totalCost * (prof / 100)
                finalPrice = totalCost + profitAmount
                perUnitCost = if (q > 0) finalPrice / q else 0.0
                
                calculated = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedClient != null
        ) {
            Text(if (selectedClient == null) "Select Customer First" else "Calculate $partName Cost", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        if (calculated) {
            ResultCard(totalCost, profitPercent.toDoubleOrNull() ?: 0.0, profitAmount, finalPrice, perUnitCost, formatter)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    selectedClient?.let {
                        onSaveOrder(
                            com.example.data.Order(
                                clientId = it.id,
                                clientName = it.name,
                                jobName = "Book - $partName",
                                category = "Offset",
                                specifications = "$selectedPaper, Size: ${width}x${height}, Color: $color, Side: $side",
                                quantity = quantity,
                                perUnitCost = perUnitCost,
                                totalAmount = finalPrice,
                                profitAmount = profitAmount
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send to New Job", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalPrintForm(viewModel: MainViewModel, clients: List<Client>, onSaveOrder: (com.example.data.Order) -> Unit) {
    val dynamicPaperTypes by viewModel.paperTypes.collectAsState()
    val materialOptions = dynamicPaperTypes.map { it.name }.ifEmpty { 
        listOf("PVC Banner Black", "PVC Banner White", "Vinyl Stikar", "Replacive Stikar", "Pana", "Pana Rivers", "Inject Stikar") 
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(materialOptions.firstOrNull() ?: "") }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    
    var width by remember { mutableStateOf("10") }
    var height by remember { mutableStateOf("5") }
    var sqftRate by remember { mutableStateOf("15") }
    
    var totalCost by remember { mutableStateOf(0.0) }
    var calculated by remember { mutableStateOf(false) }
    
    // Update rate when material changes
    LaunchedEffect(selectedCategory) {
        dynamicPaperTypes.find { it.name == selectedCategory }?.let {
            sqftRate = it.basePrice.toString()
        }
    }

    val formatter = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ClientSelection(clients = clients, selectedClient = selectedClient, onClientSelected = { selectedClient = it })

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Digital Print Material / Paper Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                materialOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = { selectedCategory = selectionOption; expanded = false }
                    )
                }
            }
        }
        
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Width (ft)", width, Modifier.weight(1f)) { width = it }
                    NumberField("Height (ft)", height, Modifier.weight(1f)) { height = it }
                }
                NumberField("Rate per sq.ft", sqftRate, Modifier.fillMaxWidth()) { sqftRate = it }
            }
        }
        
        Button(
            onClick = {
                val w = width.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                val r = sqftRate.toDoubleOrNull() ?: 0.0
                totalCost = w * h * r
                calculated = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedClient != null
        ) {
            Text(if (selectedClient == null) "Select Customer First" else "Calculate Cost", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        if (calculated) {
            ResultCard(totalCost, 0.0, 0.0, totalCost, 0.0, formatter)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    selectedClient?.let {
                        onSaveOrder(
                            com.example.data.Order(
                                clientId = it.id,
                                clientName = it.name,
                                jobName = selectedCategory,
                                category = "Digital",
                                specifications = "${width}x${height} inch",
                                quantity = "1",
                                perUnitCost = 0.0,
                                totalAmount = totalCost,
                                profitAmount = 0.0
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send to New Job", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun DesignForm(clients: List<Client>, onSaveOrder: (com.example.data.Order) -> Unit) {
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var designCharge by remember { mutableStateOf("1000") }
    var projectValue by remember { mutableStateOf("5000") }
    
    var totalCost by remember { mutableStateOf(0.0) }
    var calculated by remember { mutableStateOf(false) }
    
    val formatter = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    formatter.maximumFractionDigits = 2

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ClientSelection(clients = clients, selectedClient = selectedClient, onClientSelected = { selectedClient = it })

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField("Design Charge", designCharge, Modifier.fillMaxWidth()) { designCharge = it }
                NumberField("Custom Project Value", projectValue, Modifier.fillMaxWidth()) { projectValue = it }
            }
        }
        
        Button(
            onClick = {
                val d = designCharge.toDoubleOrNull() ?: 0.0
                val p = projectValue.toDoubleOrNull() ?: 0.0
                totalCost = d + p
                calculated = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedClient != null
        ) {
            Text(if (selectedClient == null) "Select Customer First" else "Calculate Design Cost", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        if (calculated) {
             ResultCard(totalCost, 0.0, 0.0, totalCost, 0.0, formatter)
             Spacer(modifier = Modifier.height(16.dp))
             Button(
                 onClick = {
                     selectedClient?.let {
                         onSaveOrder(
                             com.example.data.Order(
                                 clientId = it.id,
                                 clientName = it.name,
                                 jobName = "Design",
                                 category = "Design",
                                 specifications = "Design & Project",
                                 quantity = "1",
                                 perUnitCost = 0.0,
                                 totalAmount = totalCost,
                                 profitAmount = 0.0
                             )
                         )
                     }
                 },
                 modifier = Modifier.fillMaxWidth().height(56.dp),
                 colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                 shape = RoundedCornerShape(16.dp)
             ) {
                 Icon(Icons.Filled.Send, contentDescription = null)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("Send to New Job", fontSize = 16.sp, fontWeight = FontWeight.Bold)
             }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandingForm(viewModel: MainViewModel, clients: List<Client>, onSaveOrder: (com.example.data.Order) -> Unit) {
    val dynamicPaperTypes by viewModel.paperTypes.collectAsState()
    val brandingOptions = dynamicPaperTypes.map { it.name }.ifEmpty { 
        listOf("T-Shirt Print", "Mug Print", "Photo Frame", "Flex Print", "Crast", "Kye Ring")
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(brandingOptions.firstOrNull() ?: "") }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    
    var quantity by remember { mutableStateOf("50") }
    var unitRate by remember { mutableStateOf("150") }
    
    var totalCost by remember { mutableStateOf(0.0) }
    var perUnitCost by remember { mutableStateOf(0.0) }
    var calculated by remember { mutableStateOf(false) }
    
    // Update rate when item changes
    LaunchedEffect(selectedCategory) {
        dynamicPaperTypes.find { it.name == selectedCategory }?.let {
            unitRate = it.basePrice.toString()
        }
    }

    val formatter = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    formatter.maximumFractionDigits = 2

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ClientSelection(clients = clients, selectedClient = selectedClient, onClientSelected = { selectedClient = it })

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Branding Item / Paper Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                brandingOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = { selectedCategory = selectionOption; expanded = false }
                    )
                }
            }
        }
        
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Quantity", quantity, Modifier.weight(1f)) { quantity = it }
                    NumberField("Unit Rate", unitRate, Modifier.weight(1f)) { unitRate = it }
                }
            }
        }
        
        Button(
            onClick = {
                val q = quantity.toDoubleOrNull() ?: 0.0
                val r = unitRate.toDoubleOrNull() ?: 0.0
                totalCost = q * r
                perUnitCost = r
                calculated = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
            shape = RoundedCornerShape(16.dp),
            enabled = selectedClient != null
        ) {
            Text(if (selectedClient == null) "Select Customer First" else "Calculate Branding Cost", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        if (calculated) {
             ResultCard(totalCost, 0.0, 0.0, totalCost, perUnitCost, formatter)
             Spacer(modifier = Modifier.height(16.dp))
             Button(
                 onClick = {
                     selectedClient?.let {
                         onSaveOrder(
                             com.example.data.Order(
                                 clientId = it.id,
                                 clientName = it.name,
                                 jobName = selectedCategory,
                                 category = "Branding",
                                 specifications = "Quantity: $quantity",
                                 quantity = quantity,
                                 perUnitCost = perUnitCost,
                                 totalAmount = totalCost,
                                 profitAmount = 0.0
                             )
                         )
                     }
                 },
                 modifier = Modifier.fillMaxWidth().height(56.dp),
                 colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                 shape = RoundedCornerShape(16.dp)
             ) {
                 Icon(Icons.Filled.Send, contentDescription = null)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("Send to New Job", fontSize = 16.sp, fontWeight = FontWeight.Bold)
             }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ResultCard(
    totalCost: Double,
    profitPercent: Double,
    profitAmount: Double,
    finalPrice: Double,
    perUnitCost: Double,
    formatter: NumberFormat,
    subTotal1: Double = 0.0,
    subTotal2: Double = 0.0,
    subTotal3: Double = 0.0
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Internal Shop Reference (Sub-Totals)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sub-Total 1: Paper & Specs:", fontSize = 12.sp, color = Color(0xFF21005D).copy(alpha = 0.8f))
                Text("৳ ${formatter.format(subTotal1)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sub-Total 2: Printing & Plates:", fontSize = 12.sp, color = Color(0xFF21005D).copy(alpha = 0.8f))
                Text("৳ ${formatter.format(subTotal2)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sub-Total 3: Finishing:", fontSize = 12.sp, color = Color(0xFF21005D).copy(alpha = 0.8f))
                Text("৳ ${formatter.format(subTotal3)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D))
            }
            
            Divider(color = Color(0xFF21005D).copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Calculation Result", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF21005D).copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Cost:", color = Color(0xFF21005D))
                Text("৳ ${formatter.format(totalCost)}", fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
            }
            if (profitPercent > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Profit (${profitPercent}%):", color = Color(0xFF21005D))
                    Text("৳ ${formatter.format(profitAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                }
            }
            Divider(color = Color(0xFF21005D).copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Final Price:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF21005D))
                Text("৳ ${formatter.format(finalPrice)}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF21005D))
            }
            if (perUnitCost > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Per Unit Cost:", fontSize = 12.sp, color = Color(0xFF21005D).copy(alpha = 0.8f))
                    Text("৳ ${formatter.format(perUnitCost)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D).copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { 
            // Allow only numbers and a single decimal point
            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                onValueChange(it)
            }
        },
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    )
}


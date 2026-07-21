package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import com.example.util.FileUtils
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Serializable object Dashboard
@Serializable object AdminDashboardRoute
@Serializable data class Clients(val clientId: Int? = null)
@Serializable object Calculator
@Serializable data class Orders(val stage: String? = null)
@Serializable object SettingsRoute
@Serializable object ChatbotRoute
@Serializable object SystemActivityLogRoute
@Serializable object DeveloperSettingsRoute
@Serializable object DiagnosticReporterRoute
@Serializable object PriceEstimationRoute
@Serializable object EstimateHistoryRoute
@Serializable object VendorsRoute
@Serializable data class VendorLedgerRoute(val vendorId: Int)
@Serializable object JobWiseVendorCostAnalysisRoute

@Composable
fun MainScreen(viewModel: MainViewModel, isAdmin: Boolean = true, onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    val profilePictureUri by viewModel.profilePictureUri.collectAsState(initial = null)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
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
                        viewModel.updateProfilePicture(compressedFile.absolutePath)
                    } else {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val file = java.io.File(context.filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")
                        inputStream?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        viewModel.updateProfilePicture(file.absolutePath)
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
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.showNotification("ফাইল আপলোড ব্যর্থ হয়েছে, আবার চেষ্টা করুন")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(ChatbotRoute) },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Filled.SmartToy, contentDescription = "AI Assistant", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 48.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFA5A5A))
                                .clickable {
                                    permissionLauncher.launch(permissionsToRequest)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePictureUri != null) {
                                AsyncImage(
                                    model = profilePictureUri,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("S", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text("সুচারু গ্রাফিক্স", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("MANAGEMENT SYSTEM", fontSize = 9.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Box(
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape))
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface, 
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                shadowElevation = 8.dp
            ) {
                Column {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        
                        val items = listOf(
                            Triple("হোম", Dashboard, Icons.Filled.Dashboard),
                            Triple("কাস্টমার", Clients(), Icons.Filled.People),
                            Triple("অর্ডার", Orders(), Icons.AutoMirrored.Filled.ListAlt),
                            Triple("ভেন্ডর", VendorsRoute, Icons.Filled.Business),
                            Triple("হিসাব", Calculator, Icons.Filled.Calculate),
                            Triple("সেটিংস", SettingsRoute, Icons.Filled.Settings)
                        )

                        items.forEach { (name, route, icon) ->
                            val isSelected = currentDestination?.hierarchy?.any { 
                                it.route?.substringBefore("?")?.substringBefore("/") == route::class.qualifiedName 
                            } == true
                            
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        icon, 
                                        contentDescription = name, 
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    ) 
                                },
                                label = { Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                                selected = isSelected,
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(bottom = 16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
                        Text(
                            text = "Developed by: Jaynul Hafiz",
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0,0,0,0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController, 
                    startDestination = Dashboard,
                    enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }, animationSpec = androidx.compose.animation.core.tween(250)) },
                    exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = androidx.compose.animation.core.tween(250)) },
                    popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }, animationSpec = androidx.compose.animation.core.tween(250)) },
                    popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }, animationSpec = androidx.compose.animation.core.tween(250)) }
                ) {
                    composable<Dashboard> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToCalculator = {
                                navController.navigate(Calculator) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToPriceEstimation = {
                                navController.navigate(PriceEstimationRoute)
                            },
                            onNavigateToEstimateHistory = {
                                navController.navigate(EstimateHistoryRoute)
                            },
                            onNavigateToClients = {
                                navController.navigate(Clients()) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToOrders = { stage ->
                                navController.navigate(Orders(stage)) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToAdminDashboard = {
                                navController.navigate(AdminDashboardRoute)
                            },
                            onNavigateToVendors = {
                                navController.navigate(VendorsRoute)
                            }
                        )
                    }
                    composable<AdminDashboardRoute> {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToCostAnalysis = { navController.navigate(JobWiseVendorCostAnalysisRoute) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<JobWiseVendorCostAnalysisRoute> {
                        JobWiseVendorCostAnalysisScreen(viewModel, onBack = { navController.popBackStack() })
                    }
                    composable<Clients> { backStackEntry ->
                        val args = backStackEntry.toRoute<Clients>()
                        ClientsScreen(viewModel, args.clientId)
                    }
                    composable<Calculator> { CalculatorScreen(viewModel, onNavigateToOrders = { navController.navigate(Orders()) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }) }
                    composable<Orders> { backStackEntry ->
                        val args = backStackEntry.toRoute<Orders>()
                        OrdersScreen(viewModel, args.stage, onNavigateToClient = { clientId ->
                             navController.navigate(Clients(clientId = clientId)) {
                                 popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                 launchSingleTop = true
                                 restoreState = true
                             }
                        })
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(
                            viewModel = viewModel,
                            isAdmin = isAdmin,
                            onLogout = onLogout,
                            onNavigateToActivityLog = {
                                navController.navigate(SystemActivityLogRoute)
                            },
                            onNavigateToDeveloperSettings = {
                                navController.navigate(DeveloperSettingsRoute)
                            }
                        )
                    }
                    composable<ChatbotRoute> { ChatbotScreen(onBack = { navController.popBackStack() }) }
                    composable<SystemActivityLogRoute> {
                        SystemActivityLogScreen(
                            viewModel = viewModel,
                            isAdmin = isAdmin,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<DeveloperSettingsRoute> {
                        DeveloperSettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToDiagnosticReporter = {
                                navController.navigate(DiagnosticReporterRoute)
                            }
                        )
                    }
                    composable<DiagnosticReporterRoute> {
                        DiagnosticReporterScreen(onBack = { navController.popBackStack() })
                    }
                    composable<PriceEstimationRoute> {
                        PriceEstimationScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                    composable<EstimateHistoryRoute> {
                        EstimateHistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                    composable<VendorsRoute> {
                        VendorsScreen(viewModel = viewModel, onNavigateToLedger = { vendorId ->
                            navController.navigate(VendorLedgerRoute(vendorId))
                        })
                    }
                    composable<VendorLedgerRoute> { backStackEntry ->
                        val args = backStackEntry.toRoute<VendorLedgerRoute>()
                        VendorLedgerScreen(vendorId = args.vendorId, viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

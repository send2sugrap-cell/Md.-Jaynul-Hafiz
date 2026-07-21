package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.MainActivity

@Composable
fun RootAppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showWhatsAppButton = currentDestination != null && currentDestination.route?.contains("LoginRoute") == false
    val notification by viewModel.notification.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController, 
            startDestination = if (viewModel.isRememberMeActive()) {
                ManagementAppRoute(isAdmin = viewModel.isSavedSessionAdmin())
            } else {
                LoginRoute
            },
            enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }, animationSpec = androidx.compose.animation.core.tween(300)) },
            exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = androidx.compose.animation.core.tween(300)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }, animationSpec = androidx.compose.animation.core.tween(300)) },
            popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }, animationSpec = androidx.compose.animation.core.tween(300)) }
        ) {
            composable<LoginRoute> {
                LoginScreen(
                    viewModel = viewModel,
                    onManagementLogin = { isAdmin ->
                        navController.navigate(ManagementAppRoute(isAdmin)) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                    onCustomerLogin = { phone ->
                        navController.navigate(CustomerDashboardRoute(phone)) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    }
                )
            }
            
            composable<ManagementAppRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ManagementAppRoute>()
                MainScreen(
                    viewModel = viewModel,
                    isAdmin = args.isAdmin,
                    onLogout = {
                        viewModel.clearSavedSession()
                        navController.navigate(LoginRoute) {
                            popUpTo<ManagementAppRoute> { inclusive = true }
                        }
                    }
                )
            }
            
            composable<CustomerDashboardRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<CustomerDashboardRoute>()
                CustomerDashboardScreen(
                    clientPhone = args.clientPhone,
                    viewModel = viewModel,
                    onLogout = {
                        navController.navigate(LoginRoute) {
                            popUpTo<CustomerDashboardRoute> { inclusive = true }
                        }
                    }
                )
            }
        }
        
        if (showWhatsAppButton) {
            DraggableWhatsAppButton(viewModel = viewModel)
        }

        // Modern custom animated toast notification overlay
        AnimatedVisibility(
            visible = notification != null,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            if (notification != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = notification ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@kotlinx.serialization.Serializable data class ManagementAppRoute(val isAdmin: Boolean = true)

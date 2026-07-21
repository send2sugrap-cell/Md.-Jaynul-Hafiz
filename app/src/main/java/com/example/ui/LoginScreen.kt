package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object LoginRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onManagementLogin: (isAdmin: Boolean) -> Unit,
    onCustomerLogin: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isCustomerTab by remember { mutableStateOf(false) }

    // Management State
    var managementEmail by remember { mutableStateOf("") }
    var managementPassword by remember { mutableStateOf("") }
    var showManagementPassword by remember { mutableStateOf(false) }
    var managementRememberMe by remember { mutableStateOf(false) }
    var isManagementRegisterMode by remember { mutableStateOf(false) }
    
    // Management Register State
    var mgmtRegFullName by remember { mutableStateOf("") }
    var mgmtRegEmail by remember { mutableStateOf("") }
    var mgmtRegMobile by remember { mutableStateOf("") }
    var mgmtRegPassword by remember { mutableStateOf("") }
    var mgmtRegConfirmPassword by remember { mutableStateOf("") }
    var mgmtRegSecretKey by remember { mutableStateOf("") }
    var showMgmtRegPassword by remember { mutableStateOf(false) }
    var showMgmtRegConfirmPassword by remember { mutableStateOf(false) }

    // Customer State
    var isCustomerRegisterMode by remember { mutableStateOf(false) } // true = register, false = login
    
    // Customer Login State
    var customerUsername by remember { mutableStateOf("") }
    var customerPassword by remember { mutableStateOf("") }
    var showCustomerPassword by remember { mutableStateOf(false) }

    // Customer Register State
    var regFullName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regCustomerNo by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }

    // Dialog state for successful registration
    var showRegistrationSuccessDialog by remember { mutableStateOf(false) }
    var generatedCustomerUsername by remember { mutableStateOf("") }

    // Visual Palette Configurations
    val primaryCyan = Color(0xFF06B6D4) // Accent Cyan (সায়ান)
    val darkGrayText = Color(0xFF1E293B) // Dark Charcoal Text
    val subtleBgColor = Color(0xFFF8FAFC) // Light Subtle Input Background
    val darkBlueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E3A8A), // Cobalt Blue
            Color(0xFF0F172A)  // Deep Royal Blue
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlueGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            
            // App Branding Title
            Text(
                text = "সুচারু গ্রাফিক্স",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("app_title")
            )
            
            Text(
                text = "Digital Order & Bill Management",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF22D3EE), // Vibrant Cyan / Cyan-400
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 32.dp)
            )
            
            // Dual Role Toggle Capsule (Pill-shaped toggle selector)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(Color(0x26FFFFFF), RoundedCornerShape(50)) // 15% opacity White pill background
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (!isCustomerTab) primaryCyan else Color.Transparent)
                        .clickable { 
                            isCustomerTab = false 
                            isCustomerRegisterMode = false
                        }
                        .padding(vertical = 12.dp)
                        .testTag("tab_management"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ম্যানেজমেন্ট (Staff)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isCustomerTab) primaryCyan else Color.Transparent)
                        .clickable { isCustomerTab = true }
                        .padding(vertical = 12.dp)
                        .testTag("tab_customer"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কাস্টমার (Customer)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Premium pure white curved card containing login/register form overlay
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("auth_form_card"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    if (isCustomerTab) {
                        // CUSTOMER FLOW
                        if (!isCustomerRegisterMode) {
                            // 1. Customer Login View
                            Text(
                                text = "কাস্টমার লগইন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = darkGrayText
                            )
                            Text(
                                text = "অনুগ্রহ করে আপনার স্বয়ংক্রিয় ইউজারনেম এবং পাসওয়ার্ড ব্যবহার করুন।",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )

                            // Username Input Field
                            OutlinedTextField(
                                value = customerUsername,
                                onValueChange = { customerUsername = it },
                                label = { Text("ইউজার নেম (Username)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: sg_105", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor,
                                    unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText,
                                    unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("customer_username_input"),
                                singleLine = true
                            )

                            // Password Input Field
                            OutlinedTextField(
                                value = customerPassword,
                                onValueChange = { customerPassword = it },
                                label = { Text("পাসওয়ার্ড (Password)", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showCustomerPassword = !showCustomerPassword }) {
                                        Icon(
                                            imageVector = if (showCustomerPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showCustomerPassword) "Hide password" else "Show password",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (showCustomerPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor,
                                    unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText,
                                    unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("customer_password_input"),
                                singleLine = true
                            )

                            // Beautiful Forgot Password Hyperlink on the Bottom Right
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "পাসওয়ার্ড ভুলে গেছেন? (Forgot Password?)",
                                    color = Color(0xFF0891B2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            Toast.makeText(
                                                context,
                                                "পাসওয়ার্ড পুনরুদ্ধারের জন্য অনুগ্রহ করে আপনার অ্যাডমিনের সাথে সরাসরি যোগাযোগ করুন।",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        .testTag("forgot_password_hyperlink")
                                )
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val result = viewModel.loginCustomer(customerUsername, customerPassword)
                                        result.fold(
                                            onSuccess = { registeredCust ->
                                                Toast.makeText(context, "লগইন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                                                onCustomerLogin(registeredCust.mobile)
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, error.message ?: "লগইন ব্যর্থ হয়েছে", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("customer_login_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryCyan),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("লগইন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Register hyperlink
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "নতুন কাস্টমার? ", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "এখানে রেজিস্টার করুন",
                                    fontWeight = FontWeight.Bold,
                                    color = primaryCyan,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable { isCustomerRegisterMode = true }
                                        .testTag("switch_to_register_button")
                                )
                            }
                        } else {
                            // 2. Customer Registration View
                            Text(
                                text = "কাস্টমার রেজিস্ট্রেশন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = darkGrayText
                            )
                            Text(
                                text = "অর্ডারকৃত তথ্য ব্যবহার করে আপনার নতুন অ্যাকাউন্ট তৈরি করুন।",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            // Name Field
                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = { regFullName = it },
                                label = { Text("নাম (Full Name)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: সাজ্জাদ হোসেন", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor,
                                    unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText,
                                    unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("reg_name_input"),
                                singleLine = true
                            )

                            // Mobile Number Field
                            OutlinedTextField(
                                value = regMobile,
                                onValueChange = { regMobile = it },
                                label = { Text("মোবাইল নম্বর (Mobile Number)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: 017XXXXXXXX", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor,
                                    unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText,
                                    unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("reg_mobile_input"),
                                singleLine = true
                            )

                            // Customer ID / First Invoice ID Field
                            OutlinedTextField(
                                value = regCustomerNo,
                                onValueChange = { regCustomerNo = it },
                                label = { Text("কাস্টমার নং / ইনভয়েস আইডি", fontSize = 12.sp) },
                                placeholder = { Text("আপনার প্রথম মেমো/ইনভয়েস নম্বর", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor,
                                    unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText,
                                    unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("reg_customer_id_input"),
                                singleLine = true
                            )

                            // Password Field
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("পাসওয়ার্ড (Password)", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                        Icon(
                                            imageVector = if (showRegPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showRegPassword) "Hide password" else "Show password",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor,
                                    unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText,
                                    unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("reg_password_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Submit Registration Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val result = viewModel.registerCustomer(
                                            fullName = regFullName,
                                            mobile = regMobile,
                                            customerNumber = regCustomerNo,
                                            password = regPassword
                                        )
                                        result.fold(
                                            onSuccess = { generatedUser ->
                                                generatedCustomerUsername = generatedUser
                                                showRegistrationSuccessDialog = true
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, error.message ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("customer_register_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryCyan),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("রেজিস্ট্রেশন সম্পন্ন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "ইতিমধ্যে অ্যাকাউন্ট আছে? ", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "লগইন করুন",
                                    fontWeight = FontWeight.Bold,
                                    color = primaryCyan,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable { isCustomerRegisterMode = false }
                                        .testTag("switch_to_login_button")
                                )
                            }
                        }
                    } else {
                        // MANAGEMENT FLOW
                        if (isManagementRegisterMode) {
                            // MANAGEMENT REGISTRATION VIEW
                            Text(
                                text = "ম্যানেজমেন্ট রেজিস্ট্রেশন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = darkGrayText
                            )
                            Text(
                                text = "অ্যাডমিন বা স্টাফ হিসেবে ডাটাবেজে আপনার নতুন অ্যাকাউন্ট তৈরি করুন।",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            // 1. Full Name
                            OutlinedTextField(
                                value = mgmtRegFullName,
                                onValueChange = { mgmtRegFullName = it },
                                label = { Text("পূর্ণ নাম (Full Name)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: সাজ্জাদ হোসেন", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B)) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mgmt_reg_name_input"),
                                singleLine = true
                            )

                            // 2. Company Email
                            OutlinedTextField(
                                value = mgmtRegEmail,
                                onValueChange = { mgmtRegEmail = it },
                                label = { Text("কোম্পানি ইমেইল (Company Email)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: user@sucharu.com", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B)) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mgmt_reg_email_input"),
                                singleLine = true
                            )

                            // 3. Mobile Number
                            OutlinedTextField(
                                value = mgmtRegMobile,
                                onValueChange = { mgmtRegMobile = it },
                                label = { Text("মোবাইল নম্বর (Mobile Number)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: 017XXXXXXXX", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B)) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mgmt_reg_mobile_input"),
                                singleLine = true
                            )

                            // 4. Password
                            OutlinedTextField(
                                value = mgmtRegPassword,
                                onValueChange = { mgmtRegPassword = it },
                                label = { Text("পাসওয়ার্ড (Password)", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                                trailingIcon = {
                                    IconButton(onClick = { showMgmtRegPassword = !showMgmtRegPassword }) {
                                        Icon(
                                            imageVector = if (showMgmtRegPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showMgmtRegPassword) "Hide password" else "Show password",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (showMgmtRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mgmt_reg_password_input"),
                                singleLine = true
                            )

                            // 5. Confirm Password
                            OutlinedTextField(
                                value = mgmtRegConfirmPassword,
                                onValueChange = { mgmtRegConfirmPassword = it },
                                label = { Text("পাসওয়ার্ড নিশ্চিত করুন (Confirm Password)", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                                trailingIcon = {
                                    IconButton(onClick = { showMgmtRegConfirmPassword = !showMgmtRegConfirmPassword }) {
                                        Icon(
                                            imageVector = if (showMgmtRegConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showMgmtRegConfirmPassword) "Hide password" else "Show password",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (showMgmtRegConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mgmt_reg_confirm_password_input"),
                                singleLine = true
                            )

                            // 6. Security Master Key
                            OutlinedTextField(
                                value = mgmtRegSecretKey,
                                onValueChange = { mgmtRegSecretKey = it },
                                label = { Text("সিক্রেট মাস্টার কি (Security Master Key)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: admin বা staff", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color(0xFF64748B)) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mgmt_reg_secret_key_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Register button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val result = viewModel.registerManagement(
                                            fullName = mgmtRegFullName,
                                            email = mgmtRegEmail,
                                            mobileNumber = mgmtRegMobile,
                                            password = mgmtRegPassword,
                                            confirmPassword = mgmtRegConfirmPassword,
                                            secretMasterKey = mgmtRegSecretKey
                                        )
                                        result.fold(
                                            onSuccess = { user ->
                                                Toast.makeText(context, "রেজিস্ট্রেশন সফল হয়েছে! ভূমিকা: ${if(user.role == "admin") "অ্যাডমিন" else "স্টাফ"}", Toast.LENGTH_LONG).show()
                                                // Pre-fill email, clear and switch to login
                                                managementEmail = user.email
                                                managementPassword = ""
                                                isManagementRegisterMode = false
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, error.message ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("mgmt_register_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryCyan),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("রেজিস্ট্রেশন সম্পন্ন করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Go back to login link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "ইতিমধ্যে অ্যাকাউন্ট আছে? ", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "লগইন করুন",
                                    fontWeight = FontWeight.Bold,
                                    color = primaryCyan,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable { isManagementRegisterMode = false }
                                        .testTag("switch_mgmt_login_button")
                                )
                            }
                        } else {
                            // MANAGEMENT LOGIN VIEW
                            Text(
                                text = "ম্যানেজমেন্ট লগইন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = darkGrayText
                            )
                            Text(
                                text = "অ্যাডমিন ও স্টাফদের জন্য সংরক্ষিত বিলিং ও মেমোর ড্যাশবোর্ড পোর্টাল।",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )

                            // Email field
                            OutlinedTextField(
                                value = managementEmail,
                                onValueChange = { managementEmail = it },
                                label = { Text("ইমেইল (Email)", fontSize = 12.sp) },
                                placeholder = { Text("যেমন: admin@sucharu.com", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B)) },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("management_email_input"),
                                singleLine = true
                            )

                            // Password field
                            OutlinedTextField(
                                value = managementPassword,
                                onValueChange = { managementPassword = it },
                                label = { Text("পাসওয়ার্ড (Password)", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                                trailingIcon = {
                                    IconButton(onClick = { showManagementPassword = !showManagementPassword }) {
                                        Icon(
                                            imageVector = if (showManagementPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showManagementPassword) "Hide password" else "Show password",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                },
                                visualTransformation = if (showManagementPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryCyan, unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = subtleBgColor, unfocusedContainerColor = subtleBgColor,
                                    focusedLabelColor = primaryCyan, unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = darkGrayText, unfocusedTextColor = darkGrayText
                                ),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("management_password_input"),
                                singleLine = true
                            )

                            // "Remember Me" / আমাকে মনে রাখুন Checkbox Row!
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = managementRememberMe,
                                    onCheckedChange = { managementRememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryCyan),
                                    modifier = Modifier.testTag("remember_me_checkbox")
                                )
                                Text(
                                    text = "আমাকে মনে রাখুন (Remember Me)",
                                    fontSize = 13.sp,
                                    color = darkGrayText,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { managementRememberMe = !managementRememberMe }
                                )
                            }

                            // Beautiful Forgot Password Hyperlink on the Bottom Right
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "পাসওয়ার্ড ভুলে গেছেন? (Forgot Password?)",
                                    color = Color(0xFF0891B2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            Toast.makeText(
                                                context,
                                                "পাসওয়ার্ড পুনরুদ্ধারের জন্য অনুগ্রহ করে সিস্টেম অ্যাডমিনের সাথে যোগাযোগ করুন।",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        .testTag("forgot_password_management_hyperlink")
                                )
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    val emailTrimmed = managementEmail.trim()
                                    val passTrimmed = managementPassword.trim()

                                    if (emailTrimmed.isBlank() || passTrimmed.isBlank() || !emailTrimmed.contains("@")) {
                                        Toast.makeText(context, "অনুগ্রহ করে সঠিক ইমেইল এবং পাসওয়ার্ড দিন।", Toast.LENGTH_LONG).show()
                                    } else {
                                        coroutineScope.launch {
                                            val result = viewModel.loginManagement(emailTrimmed, passTrimmed, managementRememberMe)
                                            result.fold(
                                                onSuccess = { user ->
                                                    Toast.makeText(context, "লগইন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                                                    onManagementLogin(user.role == "admin")
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(context, error.message ?: "লগইন ব্যর্থ হয়েছে", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("management_login_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryCyan),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("লগইন", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Register hyperlink
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "নতুন স্টাফ/অ্যাডমিন? ", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "এখানে রেজিস্টার করুন",
                                    fontWeight = FontWeight.Bold,
                                    color = primaryCyan,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable { isManagementRegisterMode = true }
                                        .testTag("switch_to_mgmt_register_button")
                                )
                            }

                            // Premium styled, subtle credentials helper card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)), // Soft emerald container
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "লগইন ডেমো তথ্য (Demo Credentials):",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "অ্যাডমিন: admin@sucharu.com / admin",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "স্টাফ: staff@sucharu.com / staff",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Overlay Draggable WhatsApp FAB directly with real active opacity (0.4 idle, 1.0 active)
            DraggableWhatsAppButton(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

    // Success Dialog showing generated Username with refined animations/styles
    if (showRegistrationSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss without button to protect user info */ },
            title = {
                Text(
                    text = "রেজিস্ট্রেশন সফল হয়েছে!",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "আপনার অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে। আপনার লগইন করার জন্য একটি ইউনিক ইউজারনেম স্বয়ংক্রিয়ভাবে তৈরি করা হয়েছে:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Surface(
                        color = Color(0xFFECFDF5), // Emerald light container
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ইউজার নেম (Username)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF047857).copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = generatedCustomerUsername,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "লগইন করার জন্য অনুগ্রহ করে এই ইউজারনেমটি সংরক্ষণ অথবা মনে রাখুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Switch to Login tab, pre-fill username and clear dialog
                        isCustomerRegisterMode = false
                        customerUsername = generatedCustomerUsername
                        customerPassword = ""
                        showRegistrationSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryCyan),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("লগইন করতে ফিরে যান", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
}

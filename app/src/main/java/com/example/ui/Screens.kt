package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.RewardItem
import com.example.data.SavingsGoal
import com.example.data.Transaction
import com.example.data.WalletCardProfile
import java.text.SimpleDateFormat
import java.util.*

// Helper to convert timestamps to readable date-times
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeenPayAppContent(viewModel: TeenPayViewModel) {
    val currentTab = remember { mutableStateOf("home") }
    val profileState by viewModel.profileState.collectAsState()
    val transactions by viewModel.transactionsState.collectAsState()
    val savingsGoals by viewModel.savingsGoalsState.collectAsState()
    val rewards by viewModel.rewardsState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showReceiveMoneyDialog by remember { mutableStateOf(false) }

    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isPasswordLockEnabled by viewModel.isPasswordLockEnabled.collectAsState()
    var isAppLocked by remember { mutableStateOf(false) }

    val isLowBalanceAlertEnabled by viewModel.isLowBalanceAlertEnabled.collectAsState()
    val lowBalanceThreshold by viewModel.lowBalanceThreshold.collectAsState()
    var showLowBalanceDialogAlert by remember { mutableStateOf(false) }
    var lastKnownBalance by remember { mutableStateOf<Double?>(null) }

    // Alert on balance dip
    LaunchedEffect(profileState, isLowBalanceAlertEnabled, lowBalanceThreshold) {
        val currentBal = profileState?.walletBalance
        if (currentBal != null && isLowBalanceAlertEnabled) {
            if (lastKnownBalance != null && lastKnownBalance!! >= lowBalanceThreshold && currentBal < lowBalanceThreshold) {
                showLowBalanceDialogAlert = true
            }
            lastKnownBalance = currentBal
        }
    }

    // Lock app on launch if enabled
    LaunchedEffect(isBiometricEnabled, isPasswordLockEnabled) {
        if (isBiometricEnabled || isPasswordLockEnabled) {
            isAppLocked = true
        }
    }

    // Listen for events from VM to show messages
    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (isAppLocked && (isBiometricEnabled || isPasswordLockEnabled)) {
        if (isBiometricEnabled) {
            BiometricLockScreen(onUnlock = { isAppLocked = false })
        } else {
            PasswordLockScreen(viewModel = viewModel, onUnlock = { isAppLocked = false })
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("app_navigation_bar")
                        .border(BorderStroke(1.dp, Color(0xFFC4C7C5)), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)),
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    val items = listOf(
                        NavigationItem("Home", "home", Icons.Default.Home, Icons.Outlined.Home),
                        NavigationItem("Cards", "cards", Icons.Default.CreditCard, Icons.Outlined.CreditCard),
                        NavigationItem("Rewards", "rewards", Icons.Default.CardMembership, Icons.Outlined.CardMembership),
                        NavigationItem("Invest", "invest", Icons.Default.TrendingUp, Icons.Outlined.TrendingUp),
                        NavigationItem("Profile", "profile", Icons.Default.Person, Icons.Outlined.Person)
                    )

                    items.forEach { item ->
                        val isSelected = currentTab.value == item.id
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab.value = item.id },
                            label = { Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF001D36) else Color(0xFF44474E)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF001D36),
                                selectedTextColor = Color(0xFF001D36),
                                indicatorColor = Color(0xFFD1E4FF),
                                unselectedIconColor = Color(0xFF44474E),
                                unselectedTextColor = Color(0xFF44474E)
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
         ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8F9FF))
            ) {
                AnimatedContent(
                    targetState = currentTab.value,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "ScreenTransition"
                ) { tab ->
                    when (tab) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            profile = profileState,
                            transactions = transactions,
                            onNavigateToCards = { currentTab.value = "cards" },
                            onNavigateToInvest = { currentTab.value = "invest" },
                            onNavigateToRewards = { currentTab.value = "rewards" },
                            onShowReceiveMoney = { showReceiveMoneyDialog = true }
                        )
                        "cards" -> CardsScreen(
                            viewModel = viewModel,
                            profile = profileState
                        )
                        "rewards" -> RewardsScreen(
                            viewModel = viewModel,
                            profile = profileState,
                            rewards = rewards
                        )
                        "invest" -> InvestScreen(
                            viewModel = viewModel,
                            profile = profileState,
                            goals = savingsGoals
                        )
                        "profile" -> ProfileScreen(
                            viewModel = viewModel,
                            profile = profileState,
                            onShowReceiveMoney = { showReceiveMoneyDialog = true }
                        )
                    }
                }
                if (showReceiveMoneyDialog) {
                    ReceiveMoneyDialog(
                        profile = profileState,
                        onDismissRequest = { showReceiveMoneyDialog = false }
                    )
                }
                if (showLowBalanceDialogAlert && isLowBalanceAlertEnabled) {
                    Dialog(onDismissRequest = { showLowBalanceDialogAlert = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag("low_balance_alert_dialog"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1B5B5))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF1F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning Bell Icon",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = "Low Balance Alert! ⚠️",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF5C1919)
                                )

                                Text(
                                    text = "Your TeenPay Wallet balance has dipped below your customized threshold limit of ₹${lowBalanceThreshold.toInt()}.\n\nSpendable Balance: ₹${"%.2f".format(profileState?.walletBalance ?: 0.0)}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF44474E),
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showLowBalanceDialogAlert = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Dismiss", color = Color.Gray)
                                    }

                                    Button(
                                        onClick = { 
                                            showLowBalanceDialogAlert = false
                                            currentTab.value = "home"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                        modifier = Modifier.weight(1f).testTag("confirm_view_low_balance_deposit"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Top Up Now", fontWeight = FontWeight.Bold, color = Color.White)
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

data class NavigationItem(
    val label: String,
    val id: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

// -------------------------------------------------------------
// 1. HOME SCREEN
// -------------------------------------------------------------
@Composable
fun HomeScreen(
    viewModel: TeenPayViewModel,
    profile: WalletCardProfile?,
    transactions: List<Transaction>,
    onNavigateToCards: () -> Unit,
    onNavigateToInvest: () -> Unit,
    onNavigateToRewards: () -> Unit,
    onShowReceiveMoney: () -> Unit
) {
    val isLowBalanceAlertEnabled by viewModel.isLowBalanceAlertEnabled.collectAsState()
    val lowBalanceThreshold by viewModel.lowBalanceThreshold.collectAsState()
    val currentBalance = profile?.walletBalance ?: 0.0

    var showSendMoneyDialog by remember { mutableStateOf(false) }
    var showScanQrDialog by remember { mutableStateOf(false) }
    var showRechargeDialog by remember { mutableStateOf(false) }
    var showPayBillsDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var showAllTransactionsDialog by remember { mutableStateOf(false) }
    var showDownloadStatementDialog by remember { mutableStateOf(false) }
    var transactionMode by remember { mutableStateOf("all") } // "all" or "upi"

    var showUpiPinVerification by remember { mutableStateOf(false) }
    var pendingUpiPaymentAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var upiPaymentAmount by remember { mutableStateOf(0.0) }
    var upiPaymentTarget by remember { mutableStateOf("") }
    var showResetUpiPinFromHome by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_container"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER SECTION (Bento Style Welcome Card)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val initials = "A"
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0061A4), Color(0xFF004A7D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column {
                        Text(
                            text = "Welcome back,",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF44474E),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Arjun Sharma",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code Receive button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFC4C7C5)), CircleShape)
                            .clickable { onShowReceiveMoney() }
                            .testTag("action_my_qr_home"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "My QR Code",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bell notification button mockup
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color(0xFFC4C7C5)), CircleShape)
                            .clickable { /* Notifications simulated trigger */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF191C1E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (isLowBalanceAlertEnabled && currentBalance < lowBalanceThreshold) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("low_balance_warning_banner"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F1)),
                    border = BorderStroke(1.dp, Color(0xFFF1B5B5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD4D4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Wallet Balance Warning! ⚠️",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C1919)
                            )
                            Text(
                                text = "Your balance is ₹${"%.2f".format(currentBalance)}. Set limit: ₹${lowBalanceThreshold.toInt()}.",
                                fontSize = 11.sp,
                                color = Color(0xFF7D2D2D)
                            )
                        }
                        Button(
                            onClick = { showDepositDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.heightIn(max = 32.dp).testTag("low_balance_warning_topup_btn")
                        ) {
                            Text("Top Up", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 2. BENTO MAIN WALLET BALANCE CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_balance_card"),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color(0xFFBAC8DB)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1E4FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "WALLET BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36),
                                letterSpacing = 1.2.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (profile?.showBalance == true) {
                                        "₹%,.2f".format(profile.walletBalance)
                                    } else {
                                        "₹ ••••••"
                                    },
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF001D36),
                                    letterSpacing = (-0.5).sp
                                )
                                IconButton(
                                    onClick = { viewModel.toggleShowBalance() },
                                    modifier = Modifier.size(24.dp).testTag("balance_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (profile?.showBalance == true) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Hide/Show balance",
                                        tint = Color(0xFF001D36),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // KYC Verification Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.White.copy(alpha = 0.4f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "KYC VERIFIED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Bottom Row: Add Cash & Manage (Direct Pay Bill shortcut) buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDepositDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("deposit_money_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0061A4),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Add Money", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { showPayBillsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF0061A4)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF0061A4)
                            )
                        ) {
                            Text("Manage", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Quick Actions Shortcuts bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_actions_menu"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0061A4),
                        letterSpacing = 0.5.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shortcut 1: Add Money
                        QuickActionShortcutItem(
                            title = "Add Money",
                            icon = Icons.Default.Add,
                            iconBgColor = Color(0xFFE8F0FE),
                            iconColor = Color(0xFF1967D2),
                            testTag = "quick_add_money",
                            onClick = { showDepositDialog = true }
                        )

                        // Shortcut 2: Scan QR
                        QuickActionShortcutItem(
                            title = "Scan QR",
                            icon = Icons.Default.QrCodeScanner,
                            iconBgColor = Color(0xFFE6F4EA),
                            iconColor = Color(0xFF137333),
                            testTag = "quick_scan_qr",
                            onClick = { showScanQrDialog = true }
                        )

                        // Shortcut 3: View Card Details
                        QuickActionShortcutItem(
                            title = "View Card",
                            icon = Icons.Default.CreditCard,
                            iconBgColor = Color(0xFFFEF7E0),
                            iconColor = Color(0xFFB06000),
                            testTag = "quick_card_details",
                            onClick = onNavigateToCards
                        )
                    }
                }
            }
        }

        // 3. THE 2X2 BENTO GRID OF ACTIONS
        item {
            BentoQuickActionsGrid(
                onSendMoney = { showSendMoneyDialog = true },
                onScanQr = { showScanQrDialog = true },
                onRecharge = { showRechargeDialog = true },
                onDigiGold = { onNavigateToInvest() }
            )
        }

        // 4. FINANCIAL SERVICES CARDS (Optional Horizontal strip in a Bento layout)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "TeenPay Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C1E),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ServiceItem(
                            title = "Gift Cards",
                            description = "Get up to 15% cashback",
                            icon = Icons.Default.CardMembership,
                            color = Color(0xFF6750A4),
                            onClick = onNavigateToRewards
                        )
                    }
                    item {
                        var showDigiGoldDialog by remember { mutableStateOf(false) }
                        ServiceItem(
                            title = "DigiGold",
                            description = "Save in 24K Pure Gold",
                            icon = Icons.Default.Lock,
                            color = Color(0xFF8B4A00),
                            onClick = { showDigiGoldDialog = true }
                        )

                        if (showDigiGoldDialog) {
                            DigiGoldDialog(
                                viewModel = viewModel,
                                walletBalance = profile?.walletBalance ?: 0.0,
                                onDismiss = { showDigiGoldDialog = false }
                            )
                        }
                    }
                    item {
                        ServiceItem(
                            title = "TeenX Card",
                            description = "Numberless and secure",
                            icon = Icons.Default.CreditCard,
                            color = Color(0xFF0061A4),
                            onClick = onNavigateToCards
                        )
                    }
                    item {
                        ServiceItem(
                            title = "Keeper SIP",
                            description = "SIP from ₹10 budget",
                            icon = Icons.Default.Savings,
                            color = Color(0xFF3D691B),
                            onClick = onNavigateToInvest
                        )
                    }
                }
            }
        }

        // 5. RECENT TRANSACTIONS BENTO CARD BLOCK
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_transactions_bento_card"),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C7C5)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (transactions.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .clickable { showDownloadStatementDialog = true }
                                        .testTag("download_statement_btn"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                                    border = BorderStroke(1.dp, Color(0xFFD1E4FF))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Statement",
                                            tint = Color(0xFF0061A4),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Download",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0061A4)
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier.clickable { showAllTransactionsDialog = true },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1E4FF))
                                ) {
                                    Text(
                                        text = "See all",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0061A4),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Elegant custom layout switcher for All vs UPI Payments
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F0F4))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("all" to "All Payments", "upi" to "UPI Payments").forEach { (modeKey, modeLabel) ->
                            val isSelected = transactionMode == modeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable { transactionMode = modeKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = modeLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF0061A4) else Color(0xFF44474E)
                                )
                            }
                        }
                    }

                    if (transactionMode == "upi") {
                        val upiList = transactions.filter { it.category == "UPI" }
                        if (upiList.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Empty UPI list",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No UPI transactions yet",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Your UPI payments will display with recipient name, date, and amount.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            UpiTransactionHistoryList(
                                transactions = upiList.take(4),
                                onTransactionClick = { selectedTransactionForDetail = it }
                            )
                        }
                    } else {
                        if (transactions.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Empty list",
                                    tint = Color(0xFF191C1E).copy(alpha = 0.3f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No payments yet",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C1E).copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Your transactions logs will show up here.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF44474E),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            transactions.take(4).forEach { tx ->
                                TransactionBentoItem(
                                    transaction = tx,
                                    onClick = { selectedTransactionForDetail = tx }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. MONTHLY SPENDING INSIGHTS CHART BLOCK
        item {
            SpendingInsightsBentoCard(
                transactions = transactions,
                modifier = Modifier.fillMaxWidth().animateContentSize()
            )
        }
    }

    // Modal Dialogs Section

    // 1. ADD DEPOSIT FUNDS DIALOG
    if (showDepositDialog) {
        var depositAmountInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showDepositDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add cash to current wallet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showDepositDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Wallet,
                        contentDescription = "Deposit cash",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )

                    OutlinedTextField(
                        value = depositAmountInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) depositAmountInput = it },
                        label = { Text("Amount (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_amount_field"),
                        singleLine = true
                    )

                    // Presets chips values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("100", "500", "1000", "2000").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .clickable { depositAmountInput = preset }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "₹$preset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val amt = depositAmountInput.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                viewModel.depositFunds(amt)
                                showDepositDialog = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_deposit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Load Funds Instantly", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. SEND UPI MONEY DIALOG
    if (showSendMoneyDialog) {
        var receiverInput by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var payNoteInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showSendMoneyDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Send money via UPI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showSendMoneyDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = receiverInput,
                        onValueChange = { receiverInput = it },
                        label = { Text("Receiver UPI ID or Name") },
                        placeholder = { Text("e.g. aditya@upi or Priya") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "receiver") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_cash_receiver_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountInput = it },
                        label = { Text("Amount (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_cash_amount_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = payNoteInput,
                        onValueChange = { payNoteInput = it },
                        label = { Text("Add payment note (Optional)") },
                        placeholder = { Text("e.g. burger party, treats") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amt = amountInput.toDoubleOrNull()
                            if (receiverInput.isNotBlank() && amt != null && amt > 0) {
                                upiPaymentAmount = amt
                                upiPaymentTarget = receiverInput
                                pendingUpiPaymentAction = {
                                    viewModel.makeUpiPayment(receiverInput, amt, payNoteInput.ifBlank { null })
                                }
                                showUpiPinVerification = true
                                showSendMoneyDialog = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_send_cash_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Send Money via UPI Safe-Shield", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 3. SCAN QR DIALOG
    if (showScanQrDialog) {
        var merchantPreset by remember { mutableStateOf("Starbucks Cafe") }
        var amtQrInput by remember { mutableStateOf("320") }

        Dialog(onDismissRequest = { showScanQrDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Simulated QR Scanner", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showScanQrDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // A beautiful mockup scanner view
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scanner",
                                modifier = Modifier
                                    .size(72.dp)
                                    .padding(8.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Camera Active",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text("Pick simulated merchant node & scan bill value:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    // Merchant selector Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = merchantPreset == "Starbucks Cafe",
                                onClick = { merchantPreset = "Starbucks Cafe"; amtQrInput = "320" },
                                label = { Text("Starbucks") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = merchantPreset == "A1 Supermarket",
                                onClick = { merchantPreset = "A1 Supermarket"; amtQrInput = "1150" },
                                label = { Text("Supermarket") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = merchantPreset == "Zomato DineOut",
                                onClick = { merchantPreset = "Zomato DineOut"; amtQrInput = "890" },
                                label = { Text("Restaurant") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = amtQrInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amtQrInput = it },
                        label = { Text("Scanned Price (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amt = amtQrInput.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                upiPaymentAmount = amt
                                upiPaymentTarget = merchantPreset
                                pendingUpiPaymentAction = {
                                    viewModel.scanAndPaySimulate(merchantPreset, amt)
                                }
                                showUpiPinVerification = true
                                showScanQrDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Simulated QR Code Scan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 4. MOBILE RECHARGE DIALOG
    if (showRechargeDialog) {
        var mobileNo by remember { mutableStateOf("") }
        var providerSelected by remember { mutableStateOf("Jio Prepaid") }
        var amtSelected by remember { mutableStateOf("299") }

        Dialog(onDismissRequest = { showRechargeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mobile Recharge Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showRechargeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    OutlinedTextField(
                        value = mobileNo,
                        onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) mobileNo = it },
                        label = { Text("Mobile Number (10 digits)") },
                        placeholder = { Text("e.g. 9876543210") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Provider selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Jio Prepaid", "Airtel Free", "Vi Max").forEach { provider ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (providerSelected == provider) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { providerSelected = provider }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(provider, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Pack amount choices
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("299", "455", "719").forEach { plan ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (amtSelected == plan) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { amtSelected = plan }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("₹$plan (1.5GB/day)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val amt = amtSelected.toDoubleOrNull()
                            if (mobileNo.length == 10 && amt != null) {
                                val targetUser = "$providerSelected ($mobileNo)"
                                upiPaymentAmount = amt
                                upiPaymentTarget = targetUser
                                pendingUpiPaymentAction = {
                                    viewModel.makeUpiPayment(
                                        toUser = targetUser,
                                        amount = amt,
                                        note = "Recharge pack successful"
                                    )
                                }
                                showUpiPinVerification = true
                                showRechargeDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Recharge Operator Instantly", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 5. UTILITY PAY BILLS DIALOG
    if (showPayBillsDialog) {
        var utilityType by remember { mutableStateOf("Electricity (MSEDCL)") }
        var customerId by remember { mutableStateOf("") }
        var billAmount by remember { mutableStateOf("840") }

        Dialog(onDismissRequest = { showPayBillsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pay Bills & Utilities", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showPayBillsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Utility Type List Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Electricity", "Wi-Fi Fiber", "Gas Cylinder").forEach { item ->
                            val label = when (item) {
                                "Electricity" -> "Electricity (Grid)"
                                "Wi-Fi Fiber" -> "Wi-Fi (Jio)"
                                else -> "Gas Cylinder"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (utilityType.startsWith(item)) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        utilityType = label
                                        billAmount = when (item) {
                                            "Electricity" -> "840"
                                            "Wi-Fi Fiber" -> "599"
                                            else -> "1050"
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customerId,
                        onValueChange = { customerId = it },
                        label = { Text("Customer ID / Account No.") },
                        placeholder = { Text("e.g. 10098234123") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = billAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) billAmount = it },
                        label = { Text("Bill Amount Due (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amt = billAmount.toDoubleOrNull()
                            if (customerId.isNotBlank() && amt != null) {
                                upiPaymentAmount = amt
                                upiPaymentTarget = utilityType
                                pendingUpiPaymentAction = {
                                    viewModel.makeUpiPayment(
                                        toUser = utilityType,
                                        amount = amt,
                                        note = "Bill account ID: $customerId"
                                    )
                                }
                                showUpiPinVerification = true
                                showPayBillsDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Settle Bill Utility Lockout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 6. TRANSACTION DETAIL VIEW DIALOG
    if (selectedTransactionForDetail != null) {
        val tx = selectedTransactionForDetail!!
        Dialog(onDismissRequest = { selectedTransactionForDetail = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Receipt", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { selectedTransactionForDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Large circular status icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (tx.isCredit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tx.isCredit) Icons.Default.CheckCircle else Icons.Default.ArrowBack,
                            contentDescription = "Status symbol",
                            tint = if (tx.isCredit) Color(0xFF4CAF50) else Color(0xFFEF5350),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (tx.isCredit) "Money Received" else "Money Sent",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = if (tx.isCredit) "+₹%,.2f".format(tx.amount) else "-₹%,.2f".format(tx.amount),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = if (tx.isCredit) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider()

                    // Receipt Info blocks
                    ReceiptInfoRow("Entity Name:", tx.title)
                    ReceiptInfoRow("Category:", tx.category)
                    ReceiptInfoRow("Date/Time:", formatTimestamp(tx.timestamp))
                    if (!tx.note.isNullOrBlank()) {
                        ReceiptInfoRow("Note:", tx.note)
                    }
                    ReceiptInfoRow("Status:", "COMPLETED (SECURE)")

                    Button(
                        onClick = { selectedTransactionForDetail = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    if (showUpiPinVerification) {
        SecureUpiPinDialog(
            amount = upiPaymentAmount,
            recipient = upiPaymentTarget,
            correctPin = profile?.cardPin ?: "1234",
            onDismissRequest = {
                showUpiPinVerification = false
                pendingUpiPaymentAction = null
            },
            onVerificationSuccess = {
                showUpiPinVerification = false
                pendingUpiPaymentAction?.invoke()
                pendingUpiPaymentAction = null
            },
            onResetPinClick = {
                showUpiPinVerification = false
                showResetUpiPinFromHome = true
            }
        )
    }

    if (showResetUpiPinFromHome) {
        UpiPinResetDialog(
            profile = profile,
            onResetSuccess = { newPin ->
                viewModel.resetCardPin(newPin)
                showResetUpiPinFromHome = false
            },
            onDismissRequest = {
                showResetUpiPinFromHome = false
            }
        )
    }

    if (showAllTransactionsDialog) {
        UpiHistoryListDialog(
            transactions = transactions,
            onTransactionClick = { selectedTransactionForDetail = it },
            onDismissRequest = { showAllTransactionsDialog = false }
        )
    }

    if (showDownloadStatementDialog) {
        MonthlyStatementDialog(
            transactions = transactions,
            viewModel = viewModel,
            onDismissRequest = { showDownloadStatementDialog = false }
        )
    }
}

@Composable
fun BentoQuickActionsGrid(
    onSendMoney: () -> Unit,
    onScanQr: () -> Unit,
    onRecharge: () -> Unit,
    onDigiGold: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoGridTile(
                title = "Scan & Pay",
                subtitle = "Instantly",
                icon = Icons.Default.QrCodeScanner,
                bgColor = Color(0xFFF2FFDA),
                borderColor = Color(0xFFD3E5B1),
                textColor = Color(0xFF1A1C16),
                iconBgColor = Color(0xFF3D691B),
                modifier = Modifier.weight(1f),
                onClick = onScanQr,
                tag = "action_scan"
            )
            BentoGridTile(
                title = "Send Money",
                subtitle = "Via UPI ID",
                icon = Icons.Default.Send,
                bgColor = Color(0xFFFFE0E0),
                borderColor = Color(0xFFE9C4C4),
                textColor = Color(0xFF201A1A),
                iconBgColor = Color(0xFF910909),
                modifier = Modifier.weight(1f),
                onClick = onSendMoney,
                tag = "action_send"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoGridTile(
                title = "Recharge",
                subtitle = "Ph & Broadband",
                icon = Icons.Default.PhoneAndroid,
                bgColor = Color(0xFFE7E0FF),
                borderColor = Color(0xFFCBC4E9),
                textColor = Color(0xFF1C1B1F),
                iconBgColor = Color(0xFF6750A4),
                modifier = Modifier.weight(1f),
                onClick = onRecharge,
                tag = "action_recharge"
            )
            BentoGridTile(
                title = "Invest Gold",
                subtitle = "Save 24K Pure",
                icon = Icons.Default.Lock,
                bgColor = Color(0xFFFFEDE1),
                borderColor = Color(0xFFE9D5C4),
                textColor = Color(0xFF1F1B16),
                iconBgColor = Color(0xFF8B4A00),
                modifier = Modifier.weight(1f),
                onClick = onDigiGold,
                tag = "action_digigold"
            )
        }
    }
}

@Composable
fun BentoGridTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun QuickActionShortcutItem(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF44474E)
        )
    }
}

@Composable
fun TransactionBentoItem(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val emoji = when (transaction.category) {
                "UPI" -> "👤"
                "Wallet" -> "🍟"
                "Gold" -> "✨"
                "Savings" -> "📈"
                else -> "🎫"
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F0F4)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }

            Column {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF191C1E)
                )
                Text(
                    text = formatTimestamp(transaction.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF44474E)
                )
            }
        }

        Text(
            text = if (transaction.isCredit) "+ ₹%,.0f".format(transaction.amount) else "- ₹%,.0f".format(transaction.amount),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (transaction.isCredit) Color(0xFF3D691B) else Color(0xFFBA1A1A)
        )
    }
}

@Composable
fun ReceiptInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun QuickActionItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ServiceItem(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionCardItem(transaction: Transaction, onClick: () -> Unit) {
    val categoryColor = when (transaction.category) {
        "UPI" -> Color(0xFF2196F3)
        "Wallet" -> Color(0xFF4CAF50)
        "Gold" -> Color(0xFFFFD700)
        "Savings" -> Color(0xFF009688)
        else -> Color(0xFFE91E63)
    }

    val categoryIcon = when (transaction.category) {
        "UPI" -> Icons.Default.QrCodeScanner
        "Wallet" -> Icons.Default.Wallet
        "Gold" -> Icons.Default.Lock
        "Savings" -> Icons.Default.Savings
        else -> Icons.Default.CardMembership
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = transaction.category,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.widthIn(max = 180.dp)) {
                    Text(
                        text = transaction.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(transaction.timestamp),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Text(
                text = if (transaction.isCredit) "+₹${transaction.amount.toInt()}" else "-₹${transaction.amount.toInt()}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = if (transaction.isCredit) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -------------------------------------------------------------
// 2. DIGIGOLD SIMULATION DIALOG
// -------------------------------------------------------------
@Composable
fun DigiGoldDialog(
    viewModel: TeenPayViewModel,
    walletBalance: Double,
    onDismiss: () -> Unit
) {
    var goldAmountInput by remember { mutableStateOf("") }
    val goldFormatGrams = remember(goldAmountInput) {
        val rup = goldAmountInput.toDoubleOrNull() ?: 0.0
        rup / 7500.0 // Assume ₹7,500/gram gold price
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invest in 24K DigiGold", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Gold bar",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    "Current gold buy price: ₹7,500/gm (exc. GST)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = goldAmountInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) goldAmountInput = it },
                    label = { Text("Buy Amount (₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (goldFormatGrams > 0.0) {
                    Text(
                        text = "You receive: %.4fg (~24 Karat 99.9%% Pure Gold)".format(goldFormatGrams),
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {
                        val valRup = goldAmountInput.toDoubleOrNull()
                        if (valRup != null && valRup > 0) {
                            viewModel.buyDigiGold(valRup)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                ) {
                    Text("Complete Safe Gold Purchase", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. CARDS SCREEN
// -------------------------------------------------------------
@Composable
fun CardsScreen(viewModel: TeenPayViewModel, profile: WalletCardProfile?) {
    var showPinDialog by remember { mutableStateOf(false) }
    var hideCardDetailsToggle by remember { mutableStateOf(true) }

    val cardDailyLimit by viewModel.cardDailyLimit.collectAsState()
    val cardTxLimit by viewModel.cardTxLimit.collectAsState()

    var limitTapPay by remember { mutableStateOf(true) }
    var limitOnlinePayments by remember { mutableStateOf(true) }
    var limitInternational by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cards_screen_container")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "My TeenX Card",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Your personalized numberless payment card.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Beautiful Card Design with Glow / Blur gradient background
        item {
            val blockOverlayColor = if (profile?.cardBlocked == true) Color.Black.copy(alpha = 0.5f) else Color.Transparent
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .testTag("virtual_debit_card"),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (profile?.cardBlocked == true) {
                                    listOf(Color(0xFF555555), Color(0xFF333333))
                                } else {
                                    listOf(Color(0xFF1E1B4B), Color(0xFF581C87), Color(0xFF0284C7))
                                }
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TeenX Card",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Active",
                                tint = if (profile?.cardBlocked == true) Color.LightGray else Color(0xFF00E676),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Card number and mock NFC indicator
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hideCardDetailsToggle) "••••  ••••  ••••  5678" else (profile?.cardNumber ?: "4815 1623 4268 9012"),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )

                                IconButton(onClick = { hideCardDetailsToggle = !hideCardDetailsToggle }) {
                                    Icon(
                                        imageVector = if (!hideCardDetailsToggle) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "toggle detail card",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("EXPIRY", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text(
                                        if (hideCardDetailsToggle) "••/••" else (profile?.cardExpiry ?: "12/31"),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column {
                                    Text("CVV", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text(
                                        if (hideCardDetailsToggle) "•••" else (profile?.cardCvv ?: "911"),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Interactive Card blocking status
                                Text(
                                    text = if (profile?.cardBlocked == true) "BLOCKED" else "ACTIVE-PAY",
                                    color = if (profile?.cardBlocked == true) Color(0xFFEF5350) else Color(0xFF00E676),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (profile?.cardBlocked == true) Color(0xFF2C0F11) else Color(0xFF0A2814))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Block overlay screen
                    if (profile?.cardBlocked == true) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(blockOverlayColor)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked icon", tint = Color.LightGray, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("CARD BLOCKED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // TeenX Card Controls (Block, Action PIN, and Transaction Limit Configuration)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teenx_card_controls_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Card Configuration Tool",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "TeenX Card Controls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C1E)
                            )
                            Text(
                                text = "Toggle security settings and transaction limits",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    // 1. Lock/Unlock Physical Card Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (profile?.cardBlocked == true) Color(0xFFFFEBEE) else Color(0xFFE8F0FE),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (profile?.cardBlocked == true) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Block Icon",
                                    tint = if (profile?.cardBlocked == true) Color(0xFFC62828) else Color(0xFF0061A4),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (profile?.cardBlocked == true) "Physical Card: LOCKED" else "Physical Card: ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (profile?.cardBlocked == true) Color(0xFFC62828) else Color(0xFF0061A4)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lock instantly to block all merchant card transactions and swipe terminals.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }

                        Switch(
                            checked = profile?.cardBlocked == true,
                            onCheckedChange = { viewModel.toggleCardBlocked() },
                            modifier = Modifier.testTag("physical_card_lock_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFC62828),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF78909C)
                            )
                        )
                    }

                    // 2. Auxiliary Reset PIN button
                    Button(
                        onClick = { showPinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("reset_pin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "PIN", modifier = Modifier.size(16.dp))
                            Text("Change ATM/POS Card PIN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    // 3. Transaction Limits section
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Card Spend Limits",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF191C1E)
                        )

                        // 3a. Daily Spend Limit Slider & presets
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daily Spend Limit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "₹${cardDailyLimit.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0061A4)
                                )
                            }

                            Slider(
                                value = cardDailyLimit.toFloat(),
                                onValueChange = { viewModel.setCardDailyLimit(it.toDouble()) },
                                valueRange = 1000f..50000f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_daily_limit_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF0061A4),
                                    activeTrackColor = Color(0xFF0061A4)
                                )
                            )

                            // Quick Presets Raw
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5000.0, 10000.0, 25000.0, 50000.0).forEach { presetVal ->
                                    val isSelected = cardDailyLimit.toInt() == presetVal.toInt()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF0061A4) else Color(0xFFE2E8F0))
                                            .clickable { viewModel.setCardDailyLimit(presetVal) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "₹${presetVal.toInt() / 1000}k",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        // 3b. Per-Transaction Limit Slider & presets
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Per-Transaction Limit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "₹${cardTxLimit.toInt()}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F766E)
                                )
                            }

                            Slider(
                                value = cardTxLimit.toFloat(),
                                onValueChange = { viewModel.setCardTxLimit(it.toDouble()) },
                                valueRange = 500f..25000f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_tx_limit_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF0F766E),
                                    activeTrackColor = Color(0xFF0F766E)
                                )
                            )

                            // Quick Presets Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1000.0, 5000.0, 10000.0, 25000.0).forEach { presetVal ->
                                    val isSelected = cardTxLimit.toInt() == presetVal.toInt()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF0F766E) else Color(0xFFE2E8F0))
                                            .clickable { viewModel.setCardTxLimit(presetVal) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "₹${presetVal.toInt() / 1000}k",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    // 4. Channel usage toggles
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Active Channels", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Online Transactions", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Allow online purchases and web portals", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(checked = limitOnlinePayments, onCheckedChange = { limitOnlinePayments = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Contactless Tap & Pay", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Dippable POS payments (up to ₹5,000)", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(checked = limitTapPay, onCheckedChange = { limitTapPay = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("International Usage", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Transact with cross-border portals", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(checked = limitInternational, onCheckedChange = { limitInternational = it })
                        }
                    }
                }
            }
        }

        // Security assurances details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Safe Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text("Secure Shield Guarantee", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Your TeenX virtual card is compliant with PCI-DSS. Change PIN or freeze anytime to stop theft.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Modal change PIN Dialog
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showPinDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reset Card PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showPinDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "Set a new 4-digit numeric ATM/POS PIN:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInput = it },
                        label = { Text("4-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_input_field"),
                        singleLine = true,
                        placeholder = { Text("xxxx") }
                    )

                    Button(
                        onClick = {
                            if (pinInput.length == 4) {
                                viewModel.resetCardPin(pinInput)
                                showPinDialog = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_pin_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update Security Key", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CardActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            contentColor = color
        ),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(18.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// -------------------------------------------------------------
// 4. REWARDS SCREEN
// -------------------------------------------------------------
@Composable
fun RewardsScreen(
    viewModel: TeenPayViewModel,
    profile: WalletCardProfile?,
    rewards: List<RewardItem>
) {
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var selectedRewardToClaim by remember { mutableStateOf<RewardItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rewards_screen_container")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Points Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Loot & Rewards", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                        Text("My Loyalty Points", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("Earned on your smart UPI transfers!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, contentDescription = "Points indicator", tint = Color.Yellow)
                            Text(
                                "${profile?.rewardPoints ?: 0}",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Hot cashback banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1014)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFEF5350))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF5350).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = "Cashback offer", tint = Color(0xFFEF5350))
                    }
                    Column {
                        Text("Claim Extra 10% on Google Play!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Purchase any direct Play Store card to win scratchcard", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }

        // Categories filters row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Voucher categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val catItems = listOf("All", "Shopping", "Food", "Gaming", "Entertainment")
                    items(catItems) { cat ->
                        FilterChip(
                            selected = selectedFilterCategory == cat,
                            onClick = { selectedFilterCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        // Active Rewards vouchers grids
        val filteredList = if (selectedFilterCategory == "All") rewards else rewards.filter { it.category == selectedFilterCategory }
        
        if (filteredList.isEmpty()) {
            item {
                Text(
                    "No rewards in this category yet! Try making more UPI transactions to unlock.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        } else {
            items(filteredList) { r ->
                RewardStoreItemCard(
                    reward = r,
                    onClick = { selectedRewardToClaim = r }
                )
            }
        }
    }

    // Reward detailed claiming Dialog
    if (selectedRewardToClaim != null) {
        val r = selectedRewardToClaim!!
        var showScratchedCode by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { selectedRewardToClaim = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Claim Brand Giftcard", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { selectedRewardToClaim = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    Text(r.brandName, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(r.offerTitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

                    // Loyalty coins cost
                    AssistChip(
                        onClick = { },
                        label = { Text("Cost: ${r.costPoints} Loyalty Points") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = "points") }
                    )

                    if (r.isClaimed) {
                        showScratchedCode = true
                    }

                    // Simulated Scratch Card Box
                    if (!showScratchedCode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFB0BEC5), Color(0xFF78909C))
                                    )
                                )
                                .clickable {
                                    showScratchedCode = true
                                    viewModel.claimVoucher(r.id, r.brandName, r.costPoints)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Lock, contentDescription = "Scratch lock", tint = Color.White, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("TAP TO SCRATCH & UNLOCK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Scratch card code revealed!
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9))
                                .border(1.5.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("COUPON UNLOCKED", color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            Text(
                                text = r.couponCode,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Text("Copy and apply at checkout page.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = { selectedRewardToClaim = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Portal")
                    }
                }
            }
        }
    }
}

@Composable
fun RewardStoreItemCard(reward: RewardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("reward_item_${reward.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = reward.brandName,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reward.brandName, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(
                            reward.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(reward.offerTitle, fontSize = 12.sp, color = Color.Gray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${reward.costPoints} Pts",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (reward.isClaimed) "REDEEMED" else "CLAIM CODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (reward.isClaimed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. INVESTMENTS & SAVINGS GOALS SCREEN
// -------------------------------------------------------------
@Composable
fun InvestScreen(
    viewModel: TeenPayViewModel,
    profile: WalletCardProfile?,
    goals: List<SavingsGoal>
) {
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalForBoost by remember { mutableStateOf<SavingsGoal?>(null) }
    var sipDialogShow by remember { mutableStateOf(false) }

    var showUpiPinVerification by remember { mutableStateOf(false) }
    var pendingUpiPaymentAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var upiPaymentAmount by remember { mutableStateOf(0.0) }
    var upiPaymentTarget by remember { mutableStateOf("") }
    var showResetUpiPinFromInvest by remember { mutableStateOf(false) }

    // Aggregate statistics
    val totalGoalWorth = goals.sumOf { it.currentAmount }
    val totalTargetNeeded = goals.sumOf { it.targetAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("invest_screen_container")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Portfolio performance top cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A1C)) // forest slate tone
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invest Portfolio", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("+6.8% Absolute returns", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.background(Color(0xFF0F1E10), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }

                        // Cash volume
                        Text(
                            text = "₹%,.2f".format(totalGoalWorth),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text("Total Saved across goals: ₹%,.2f needed".format(totalTargetNeeded), color = Color.LightGray, fontSize = 12.sp)

                        Button(
                            onClick = { sipDialogShow = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A), contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = "SIP setter", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Setup Keeper SIP Plan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sub categories selection lists
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Core Investment channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var goldModalShow by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { goldModalShow = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Gold Logo", tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                            Text("24K DigiGold", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Live Price: ₹7,500/g", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    if (goldModalShow) {
                        DigiGoldDialog(viewModel = viewModel, walletBalance = profile?.walletBalance ?: 0.0, onDismiss = { goldModalShow = false })
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { sipDialogShow = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Savings, contentDescription = "SIP Logo", tint = Color(0xFF009688), modifier = Modifier.size(32.dp))
                            Text("Keeper SIP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Start SIP with ₹10", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Goals saver header and custom goal trigger and creation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Savings Goals (Goal Saver)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { showCreateGoalDialog = true },
                    modifier = Modifier.testTag("create_goal_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create goal")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Goals iteration lists
        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = "No goals", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text("No savings target established", fontWeight = FontWeight.Bold)
                        Text("Create target goals to save periodically for gadgets, gaming, college books, etc.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(goals) { goal ->
                GoalSaverCardItem(
                    goal = goal,
                    onBoostClick = { selectedGoalForBoost = goal }
                )
            }
        }
    }

    // Modal Create custom Saving Target Dialog
    if (showCreateGoalDialog) {
        var goalNameInput by remember { mutableStateOf("") }
        var goalTargetInput by remember { mutableStateOf("") }
        var goalCategorySelected by remember { mutableStateOf("Gaming") }

        Dialog(onDismissRequest = { showCreateGoalDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Create Savings Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showCreateGoalDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = goalNameInput,
                        onValueChange = { goalNameInput = it },
                        label = { Text("What are you saving for?") },
                        placeholder = { Text("e.g. PlayStation 5, iPad Air") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_name_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = goalTargetInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) goalTargetInput = it },
                        label = { Text("Target Price Needed (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_target_price_field"),
                        singleLine = true
                    )

                    Text("Pick Goal Category:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)

                    // Goal categories selections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Gaming", "Education", "Gadgets", "Fashion").forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (goalCategorySelected == cat) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .background(if (goalCategorySelected == cat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { goalCategorySelected = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val limitTarget = goalTargetInput.toDoubleOrNull()
                            if (goalNameInput.isNotBlank() && limitTarget != null && limitTarget > 0) {
                                viewModel.addSavingsGoal(goalNameInput, limitTarget, goalCategorySelected)
                                showCreateGoalDialog = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_goal_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Establish Savings Lockout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Add progress cash to active Goal
    if (selectedGoalForBoost != null) {
        val activeGoal = selectedGoalForBoost!!
        var transferCashAmount by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { selectedGoalForBoost = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add Funds to Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { selectedGoalForBoost = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    Text(activeGoal.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Current savings: ₹%,.2f Saved (₹%,.2f left)".format(activeGoal.currentAmount, activeGoal.targetAmount - activeGoal.currentAmount), fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = transferCashAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) transferCashAmount = it },
                        label = { Text("Transfer amount from Wallet (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_boost_amount_field"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val boostVal = transferCashAmount.toDoubleOrNull()
                            if (boostVal != null && boostVal > 0) {
                                viewModel.investInGoal(activeGoal.id, activeGoal.name, boostVal)
                                selectedGoalForBoost = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_goal_boost_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Wallet Transfer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Mock Keeper SIP Setup Dialog
    if (sipDialogShow) {
        var sipAmt by remember { mutableStateOf("100") }
        var period by remember { mutableStateOf("Monthly") }

        Dialog(onDismissRequest = { sipDialogShow = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Setup Keeper SIP Plan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { sipDialogShow = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    Text("Keeper automates micro-savings starting with ₹10, building consistent savings habits key to wealth creation.", fontSize = 11.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = sipAmt,
                        onValueChange = { if (it.all { char -> char.isDigit() }) sipAmt = it },
                        label = { Text("Installment budget (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Black) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Weekly", "Monthly", "Quarterly").forEach { item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (period == item) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { period = item }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val amt = sipAmt.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                                upiPaymentAmount = amt
                                upiPaymentTarget = "Keeper SIP Plan ($period)"
                                pendingUpiPaymentAction = {
                                    viewModel.depositFunds(amt) // Deposits initial capital
                                    viewModel.makeUpiPayment(
                                        toUser = "Keeper SIP Plan ($period)",
                                        amount = amt,
                                        note = "Scheduled automated cycle activated"
                                    )
                                }
                                showUpiPinVerification = true
                                sipDialogShow = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Activate automated cycle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showUpiPinVerification) {
        SecureUpiPinDialog(
            amount = upiPaymentAmount,
            recipient = upiPaymentTarget,
            correctPin = profile?.cardPin ?: "1234",
            onDismissRequest = {
                showUpiPinVerification = false
                pendingUpiPaymentAction = null
            },
            onVerificationSuccess = {
                showUpiPinVerification = false
                pendingUpiPaymentAction?.invoke()
                pendingUpiPaymentAction = null
            },
            onResetPinClick = {
                showUpiPinVerification = false
                showResetUpiPinFromInvest = true
            }
        )
    }

    if (showResetUpiPinFromInvest) {
        UpiPinResetDialog(
            profile = profile,
            onResetSuccess = { newPin ->
                viewModel.resetCardPin(newPin)
                showResetUpiPinFromInvest = false
            },
            onDismissRequest = {
                showResetUpiPinFromInvest = false
            }
        )
    }
}

@Composable
fun GoalSaverCardItem(goal: SavingsGoal, onBoostClick: () -> Unit) {
    val progressFraction = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
    val progressPct = (progressFraction * 100).toInt()

    val goalCategoryColor = when (goal.category) {
        "Gaming" -> Color(0xFF673AB7)
        "Education" -> Color(0xFF009688)
        "Gadgets" -> Color(0xFFFF9800)
        "Fashion" -> Color(0xFFE91E63)
        else -> Color(0xFF607D8B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("savings_goal_item_${goal.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(goal.name, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(
                            text = goal.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = goalCategoryColor,
                            modifier = Modifier
                                .background(goalCategoryColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "₹Saved: ₹%,.2f of ₹%,.2f target".format(goal.currentAmount, goal.targetAmount),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "$progressPct%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goal.currentAmount >= goal.targetAmount) "⭐ GOAL FULLY FUNDED!" else "₹%,.2f left to save".format(goal.targetAmount - goal.currentAmount),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (goal.currentAmount >= goal.targetAmount) Color(0xFF4CAF50) else Color.Gray
                )

                if (goal.currentAmount < goal.targetAmount) {
                    TextButton(
                        onClick = onBoostClick,
                        modifier = Modifier.testTag("boost_goal_button_${goal.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "boost", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add cash", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. PROFILE & SETTINGS SCREEN
// -------------------------------------------------------------
@Composable
fun ProfileScreen(
    viewModel: TeenPayViewModel,
    profile: WalletCardProfile?,
    onShowReceiveMoney: () -> Unit
) {
    var kycFlowDialogShow by remember { mutableStateOf(false) }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isPasswordLockEnabled by viewModel.isPasswordLockEnabled.collectAsState()
    val appPassword by viewModel.appPassword.collectAsState()
    var showResetUpiPinFromProfile by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    val isLowBalanceAlertEnabled by viewModel.isLowBalanceAlertEnabled.collectAsState()
    val lowBalanceThreshold by viewModel.lowBalanceThreshold.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_container")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User primary Profile card header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Avatar Mock Bubble
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TP",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                    }

                    // User name, mail, phone details
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TeenPay Rockstar", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("9507280493ali@gmail.com", fontSize = 12.sp, color = Color.Gray)
                        Text("+91 95072 80493", fontSize = 11.sp, color = Color.Gray)
                    }

                    // KYC Verification box indicator
                    val kycVerified = profile?.kycStatus == "VERIFIED"
                    val kycPending = profile?.kycStatus == "PENDING"
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    kycVerified -> Color(0xFFE8F5E9)
                                    kycPending -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFFFEBEE)
                                }
                            )
                            .clickable { kycFlowDialogShow = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (kycVerified) Icons.Default.Verified else Icons.Default.Info,
                                contentDescription = "kyc status",
                                tint = when {
                                    kycVerified -> Color(0xFF4CAF50)
                                    kycPending -> Color(0xFFFF9800)
                                    else -> Color(0xFFEF5350)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when {
                                    kycVerified -> "KYC VERIFIED STATUS: FULLY SAFE"
                                    kycPending -> "KYC SUBMITTED: PENDING REVIEW"
                                    else -> "KYC INCOMPLETE: COMPLETE SOON NOW"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = when {
                                    kycVerified -> Color(0xFF2E7D32)
                                    kycPending -> Color(0xFFE65100)
                                    else -> Color(0xFFC62828)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick high-level stats dashboards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatsItemCard(title = "Wallet Bal", value = "₹${profile?.walletBalance?.toInt() ?: 0}", icon = Icons.Default.Wallet, modifier = Modifier.weight(1f))
                StatsItemCard(title = "Buddy Points", value = "${profile?.rewardPoints ?: 0}", icon = Icons.Default.Star, modifier = Modifier.weight(1f))
                StatsItemCard(title = "KYC Status", value = profile?.kycStatus ?: "VERIFIED", icon = Icons.Default.Verified, modifier = Modifier.weight(1f))
            }
        }

        // Notification settings section card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_settings_panel"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Wallet Alert & Notification Rules", fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))

                    HorizontalDivider()

                    ProfileToggleItem(
                        title = "Low Balance Wallet Alert",
                        checked = isLowBalanceAlertEnabled,
                        onCheckedChange = { viewModel.setLowBalanceAlertEnabled(it) }
                    )

                    if (isLowBalanceAlertEnabled) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0F4FA), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Low Balance Warning Limit (₹)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )

                            // Quick preset chips row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(200.0, 500.0, 1000.0, 2000.0)
                                presets.forEach { amt ->
                                    val isSelected = lowBalanceThreshold == amt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) Color(0xFF0061A4) else Color(0xFFE1E2EC)
                                            )
                                            .clickable { viewModel.setLowBalanceThreshold(amt) }
                                            .testTag("preset_threshold_${amt.toInt()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "₹${amt.toInt()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF1C1B1F)
                                        )
                                    }
                                }
                            }

                            // Custom manual input textfield
                            var customText by remember(lowBalanceThreshold) { mutableStateOf(lowBalanceThreshold.toInt().toString()) }
                            OutlinedTextField(
                                value = customText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        customText = newValue
                                        val doubleVal = newValue.toDoubleOrNull() ?: 0.0
                                        if (doubleVal >= 0) {
                                            viewModel.setLowBalanceThreshold(doubleVal)
                                        }
                                    }
                                },
                                label = { Text("Custom Warning Threshold", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("low_balance_threshold_input"),
                                singleLine = true,
                                trailingIcon = {
                                    Text("INR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                }
                            )

                            Text(
                                "Receive on-screen notification warnings and highlights whenever wallet dips below ₹${lowBalanceThreshold.toInt()}.",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Settings items triggers lists
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("General App Actions", fontWeight = FontWeight.Bold)

                    HorizontalDivider()

                    ProfileToggleItem("Notification Tone Sound", true)
                    ProfileToggleItem("Daily Security App Check", true)
                    ProfileToggleItem("Dark Visual Contrast Mode", false)
                    ProfileToggleItem(
                        title = "Biometric Lock Security",
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                    ProfileToggleItem(
                        title = "Password Lock Security",
                        checked = isPasswordLockEnabled,
                        onCheckedChange = {
                            if (it) {
                                showSetPasswordDialog = true
                            } else {
                                viewModel.setPasswordLockEnabled(false)
                            }
                        }
                    )
                    if (isPasswordLockEnabled) {
                        ProfileNavigationItem(
                            title = "Change App Lock Password (Current: $appPassword)",
                            onClick = { showSetPasswordDialog = true }
                        )
                    }

                    HorizontalDivider()

                    ProfileNavigationItem("My UPI QR Code & ID", { onShowReceiveMoney() })
                    ProfileNavigationItem("Reset UPI PIN", { showResetUpiPinFromProfile = true })
                    ProfileNavigationItem("Change KYC Level", { kycFlowDialogShow = true })
                    ProfileNavigationItem("Budgets & Spending Warnings", { })
                    ProfileNavigationItem("Buddy Referral Code program", { })
                }
            }
        }

        // Log out or reset app
        item {
            TextButton(
                onClick = { },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Reset app logo")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Out of TeenPay Network", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (showResetUpiPinFromProfile) {
        UpiPinResetDialog(
            profile = profile,
            onResetSuccess = { newPin ->
                viewModel.resetCardPin(newPin)
                showResetUpiPinFromProfile = false
            },
            onDismissRequest = {
                showResetUpiPinFromProfile = false
            }
        )
    }

    if (showSetPasswordDialog) {
        var newPasswordInput by remember { mutableStateOf("") }
        var confirmPasswordInput by remember { mutableStateOf("") }
        var passwordVisibility by remember { mutableStateOf(false) }
        var passwordInputError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showSetPasswordDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("set_password_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFC4C7C5))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set Security Password",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0061A4)
                    )

                    Text(
                        text = "Enter a password to unlock your app upon launch. This keeps your TeenPay wallet content secure.",
                        fontSize = 12.sp,
                        color = Color(0xFF44474E)
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = {
                            newPasswordInput = it
                            if (passwordInputError != null) passwordInputError = null
                        },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_password_text_field"),
                        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val visibilityIcon = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                Icon(imageVector = visibilityIcon, contentDescription = "Toggle text password visibility")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            if (passwordInputError != null) passwordInputError = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_password_text_field"),
                        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val visibilityIcon = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                Icon(imageVector = visibilityIcon, contentDescription = "Toggle confirmation password visibility")
                            }
                        }
                    )

                    if (passwordInputError != null) {
                        Text(
                            text = passwordInputError!!,
                            color = Color(0xFFBA1A1A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showSetPasswordDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (newPasswordInput.isEmpty()) {
                                    passwordInputError = "Password cannot be empty."
                                } else if (newPasswordInput != confirmPasswordInput) {
                                    passwordInputError = "Passwords do not match."
                                } else {
                                    viewModel.setAppPassword(newPasswordInput)
                                    viewModel.setPasswordLockEnabled(true)
                                    showSetPasswordDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            modifier = Modifier.weight(1.5f).testTag("save_password_setup_btn")
                        ) {
                            Text("Save & Enable", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // kyc verify setup dialogue
    if (kycFlowDialogShow) {
        var dummyIdNo by remember { mutableStateOf("") }
        var selectedProviderKyc by remember { mutableStateOf("Aadhaar Card ID") }

        Dialog(onDismissRequest = { kycFlowDialogShow = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("KYC Verification Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { kycFlowDialogShow = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    Text("Under Indian guidelines, teenagers must submit a verified parent-supervised registration or Aadhaar token to enable ₹10,000+ limits.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    // Card options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Aadhaar Card ID", "PAN Parent ID").forEach { provider ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (selectedProviderKyc == provider) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedProviderKyc = provider }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(provider, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dummyIdNo,
                        onValueChange = { dummyIdNo = it },
                        label = { Text("Identifier Number (e.g. 12 digits)") },
                        placeholder = { Text("e.g. 8410 2038 9010") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (dummyIdNo.isNotBlank()) {
                                viewModel.submitKycFlow("VERIFIED")
                                kycFlowDialogShow = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm KYC Token Verification", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsItemCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun ProfileToggleItem(title: String, initial: Boolean) {
    var state by remember { mutableStateOf(initial) }
    ProfileToggleItem(title = title, checked = state, onCheckedChange = { state = it })
}

@Composable
fun ProfileToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("toggle_${title.replace(" ", "_").lowercase()}")
        )
    }
}

@Composable
fun ProfileNavigationItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.ChevronRight, contentDescription = "navigate icon", tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun PinInputDots(
    pin: String,
    modifier: Modifier = Modifier,
    maxDigits: Int = 4
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDigits) {
            val isFilled = i < pin.length
            val size by animateDpAsState(
                targetValue = if (isFilled) 18.dp else 14.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "DotSize"
            )
            val color by animateColorAsState(
                targetValue = if (isFilled) Color(0xFF0061A4) else Color(0xFFC4C7C5),
                animationSpec = tween(150),
                label = "DotColor"
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(if (isFilled) color else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = color,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun SecureNumericKeypad(
    onDigitClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    scramble: Boolean = false
) {
    val keys = remember(scramble) {
        val base = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
        if (scramble) {
            val shuffled = base.shuffled()
            val grid = mutableListOf<Char>()
            grid.addAll(shuffled.take(9))
            grid.add('C')
            grid.add(shuffled.last())
            grid.add('⌫')
            grid
        } else {
            listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', 'C', '0', '⌫')
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..2) {
                    val keyIndex = row * 3 + col
                    val key = keys[keyIndex]
                    KeypadButton(
                        key = key,
                        onClick = {
                            when (key) {
                                'C' -> onClearClick()
                                '⌫' -> onDeleteClick()
                                else -> onDigitClick(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    key: Char,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isActionKey = key == 'C' || key == '⌫'
    val containerColor = if (isActionKey) {
        Color(0xFFE7E0FF).copy(alpha = 0.5f)
    } else {
        Color(0xFFF1F0F4)
    }
    val contentColor = if (isActionKey) {
        Color(0xFF6750A4)
    } else {
        Color(0xFF001D36)
    }

    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SecureUpiPinDialog(
    amount: Double,
    recipient: String,
    correctPin: String,
    onDismissRequest: () -> Unit,
    onVerificationSuccess: () -> Unit,
    onResetPinClick: (() -> Unit)? = null
) {
    var pinValue by remember { mutableStateOf("") }
    var isScrambled by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var livesRemaining by remember { mutableStateOf(3) }

    var shakeCount by remember { mutableStateOf(0) }
    val isShaking = shakeCount > 0

    val shakeOffset by animateDpAsState(
        targetValue = if (isShaking) {
            when (shakeCount % 5) {
                1 -> (-10).dp
                2 -> 10.dp
                3 -> (-6).dp
                4 -> 6.dp
                else -> 0.dp
            }
        } else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "ShakeOffset"
    )

    LaunchedEffect(shakeCount) {
        if (shakeCount > 0) {
            delay(350)
            shakeCount = 0
        }
    }

    LaunchedEffect(pinValue) {
        if (pinValue.length == 4) {
            isProcessing = true
            errorMessage = null
            delay(1200)
            isProcessing = false
            if (pinValue == correctPin) {
                isSuccess = true
                delay(1500)
                onVerificationSuccess()
            } else {
                livesRemaining--
                shakeCount = 1
                pinValue = ""
                if (livesRemaining <= 0) {
                    errorMessage = "Maximum PIN attempts exceeded. Session locked."
                } else {
                    errorMessage = "Incorrect security PIN. Retries remaining: $livesRemaining"
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (!isProcessing && !isSuccess) onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC4C7C5))
        ) {
            AnimatedContent(
                targetState = when {
                    isSuccess -> 2
                    isProcessing -> 1
                    else -> 0
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "DialogStageChange"
            ) { stage ->
                when (stage) {
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                "UPI Secure Shield",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36),
                                letterSpacing = 0.5.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = Color(0xFF0061A4),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(80.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Securing",
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                "Verifying UPI PIN with bank...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF191C1E)
                            )
                            Text(
                                "Connecting via secure end-to-end token layer",
                                fontSize = 10.sp,
                                color = Color(0xFF44474E)
                            )
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                "Payment Secure ✅",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                letterSpacing = 0.5.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                "₹%,.2f Authorized".format(amount),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF001D36)
                            )
                            Text(
                                "Transferring directly to $recipient",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF44474E)
                            )
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Safe Shield",
                                        tint = Color(0xFF0061A4),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "ENTER 4-DIGIT UPI PIN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF001D36),
                                        letterSpacing = 1.sp
                                    )
                                }
                                IconButton(onClick = onDismissRequest) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFC4C7C5).copy(alpha = 0.3f))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
                                border = BorderStroke(1.dp, Color(0xFFC4C7C5).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Paying",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF44474E).copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "₹%,.2f".format(amount),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF001D36)
                                    )
                                    Text(
                                        text = "to $recipient",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF191C1E)
                                    )
                                }
                            }

                            PinInputDots(
                                pin = pinValue,
                                modifier = Modifier
                                    .offset(x = shakeOffset)
                                    .padding(vertical = 4.dp)
                            )

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFBA1A1A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "PIN is never shared with the recipient. End-to-end encrypted.",
                                    color = Color(0xFF44474E),
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (onResetPinClick != null) {
                                TextButton(
                                    onClick = onResetPinClick,
                                    modifier = Modifier.heightIn(max = 32.dp).testTag("forgot_upi_pin_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset PIN Icon",
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF0061A4)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Forgot UPI PIN? Reset PIN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0061A4)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE7E0FF).copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Shield Guard",
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Scramble Numeric Keypad",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1C1B1F),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Switch(
                                    checked = isScrambled,
                                    onCheckedChange = { isScrambled = it },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }

                            if (livesRemaining > 0) {
                                SecureNumericKeypad(
                                    onDigitClick = { digit ->
                                        if (pinValue.length < 4) {
                                            pinValue += digit
                                        }
                                    },
                                    onDeleteClick = {
                                        if (pinValue.isNotEmpty()) {
                                            pinValue = pinValue.dropLast(1)
                                        }
                                    },
                                    onClearClick = {
                                        pinValue = ""
                                    },
                                    scramble = isScrambled
                                )
                            } else {
                                Button(
                                    onClick = onDismissRequest,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Dismiss", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReceiveMoneyDialog(
    profile: WalletCardProfile?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var requestAmount by remember { mutableStateOf("") }
    var showSetAmountDialog by remember { mutableStateOf(false) }
    var tempAmountInput by remember { mutableStateOf("") }
    
    val upiId = "9507280493@payteen"
    val upiName = "Arjun Sharma"
    
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC4C7C5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Receive icon",
                                tint = Color(0xFF0061A4),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Receive Money",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36)
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F0F4))

                // User details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E2EC))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = upiName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "UPI ID: $upiId",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0061A4)
                            )
                            IconButton(
                                onClick = {
                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clipData = android.content.ClipData.newPlainText("UPI ID", upiId)
                                    clipboardManager.setPrimaryClip(clipData)
                                    android.widget.Toast.makeText(context, "UPI ID copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy UPI ID",
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Dynamic Amount Request Display
                AnimatedVisibility(visible = requestAmount.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2FFDA)),
                        border = BorderStroke(1.dp, Color(0xFFD3E5B1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "REQUEST AMOUNT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3D691B),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "₹" + requestAmount,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1A1C16)
                                )
                            }
                            IconButton(
                                onClick = { requestAmount = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear amount",
                                    tint = Color(0xFF3D691B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Custom High-Fidelity Canvas-generated QR Code
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(BorderStroke(2.dp, Color(0xFF0061A4)), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CanvasQrCode(
                        text = upiId + (if (requestAmount.isNotEmpty()) "?am=$requestAmount" else ""),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "Scan this QR code using any UPI app (GPay, PhonePe, Paytm) to transfer money.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempAmountInput = requestAmount
                            showSetAmountDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.5.dp, Color(0xFF0061A4)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0061A4))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Set Custom Amount",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (requestAmount.isEmpty()) "Set Amount" else "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            android.widget.Toast.makeText(context, "QR Code Shared and Saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share QR code",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share QR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showSetAmountDialog) {
        Dialog(onDismissRequest = { showSetAmountDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set Request Amount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36)
                    )

                    OutlinedTextField(
                        value = tempAmountInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() || it == '.' }) {
                                tempAmountInput = input
                            }
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("e.g. 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0061A4),
                            focusedLabelColor = Color(0xFF0061A4)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showSetAmountDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                val dAmount = tempAmountInput.toDoubleOrNull()
                                if (dAmount != null && dAmount > 0) {
                                    requestAmount = "%.2f".format(dAmount)
                                } else {
                                    requestAmount = ""
                                }
                                showSetAmountDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasQrCode(
    text: String,
    modifier: Modifier = Modifier
) {
    // Generate static matrix elements based on text hash to look realistic and change dynamically!
    val hash = text.hashCode()
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val numGrid = 21 // 21x21 QR Grid
        val cellSize = sizePx / numGrid

        // 1. Draw solid outer quiet border
        drawRect(
            color = Color.White,
            size = size
        )

        // Helper to draw a Finder Pattern at row, col
        fun drawFinderPattern(row: Int, col: Int) {
            val outerX = col * cellSize
            val outerY = row * cellSize
            val outerSize = 7 * cellSize

            // Outer dark square
            drawRect(
                color = Color(0xFF001D36),
                offsetX = outerX,
                offsetY = outerY,
                width = outerSize,
                height = outerSize
            )

            // Middle white square
            drawRect(
                color = Color.White,
                offsetX = outerX + cellSize,
                offsetY = outerY + cellSize,
                width = outerSize - 2 * cellSize,
                height = outerSize - 2 * cellSize
            )

            // Inner dark square
            drawRect(
                color = Color(0xFF001D36),
                offsetX = outerX + 2 * cellSize,
                offsetY = outerY + 2 * cellSize,
                width = outerSize - 4 * cellSize,
                height = outerSize - 4 * cellSize
            )
        }

        // Draw the 3 finder patterns
        drawFinderPattern(0, 0)                  // Top Left
        drawFinderPattern(0, numGrid - 7)        // Top Right
        drawFinderPattern(numGrid - 7, 0)        // Bottom Left

        // Draw the main timing patterns and bits
        val randomOfText = java.util.Random(hash.toLong())

        for (r in 0 until numGrid) {
            for (c in 0 until numGrid) {
                // Check if inside finder pattern area
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= numGrid - 8
                val inBottomLeft = r >= numGrid - 8 && c < 8

                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val rVal = randomOfText.nextFloat()
                    // Timing pattern or standard databits
                    val isBitSet = when {
                        // Timing pattern lines
                        r == 6 && c % 2 == 0 -> true
                        c == 6 && r % 2 == 0 -> true
                        // Specific aesthetic visual groupings
                        rVal > 0.65f -> true
                        else -> false
                    }

                    if (isBitSet) {
                        drawRect(
                            color = Color(0xFF001D36),
                            offsetX = c * cellSize,
                            offsetY = r * cellSize,
                            width = cellSize + 0.5f, // add small bleed to prevent grid lines
                            height = cellSize + 0.5f
                        )
                    }
                }
            }
        }

        // Draw a gorgeous central branding badge in the QR code
        val badgeGridSize = 5
        val badgeOffset = (numGrid - badgeGridSize) / 2
        val badgeX = badgeOffset * cellSize
        val badgeY = badgeOffset * cellSize
        val badgeSize = badgeGridSize * cellSize

        // Draw badge white background card
        drawRoundRect(
            color = Color.White,
            offsetX = badgeX - 1.5f * cellSize,
            offsetY = badgeY - 1.5f * cellSize,
            width = badgeSize + 3 * cellSize,
            height = badgeSize + 3 * cellSize,
            cornerRadius = 10f
        )

        // Draw badge primary background
        drawRoundRect(
            color = Color(0xFF0061A4),
            offsetX = badgeX - 0.5f * cellSize,
            offsetY = badgeY - 0.5f * cellSize,
            width = badgeSize + cellSize,
            height = badgeSize + cellSize,
            cornerRadius = 8f
        )

        // Draw nice inner white square for brand insignia
        drawRoundRect(
            color = Color.White,
            offsetX = badgeX + 0.2f * cellSize,
            offsetY = badgeY + 0.2f * cellSize,
            width = badgeSize - 0.4f * cellSize,
            height = badgeSize - 0.4f * cellSize,
            cornerRadius = 6f
        )
    }
}

// Draw rect helpers for canvas
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRect(
    color: Color,
    offsetX: Float,
    offsetY: Float,
    width: Float,
    height: Float
) {
    drawRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(offsetX, offsetY),
        size = androidx.compose.ui.geometry.Size(width, height)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundRect(
    color: Color,
    offsetX: Float,
    offsetY: Float,
    width: Float,
    height: Float,
    cornerRadius: Float
) {
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(offsetX, offsetY),
        size = androidx.compose.ui.geometry.Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    // Extract a clean recipient name from payment title (e.g., "Send to Arjun" -> "Arjun", "Paid to KFC" -> "KFC")
    val cleanName = remember(transaction.title) {
        val title = transaction.title
        when {
            title.startsWith("Send to ", ignoreCase = true) -> title.substring(8)
            title.startsWith("Paid to ", ignoreCase = true) -> title.substring(8)
            else -> title
        }
    }

    val formattedDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(java.util.Date(transaction.timestamp))
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upi_transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F0F4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Initials Avatar
                val avatarChar = cleanName.firstOrNull()?.toString()?.uppercase() ?: "U"
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE8F0FE), Color(0xFFD2E3FC))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarChar,
                        color = Color(0xFF1A73E8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = cleanName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF191C1E)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "UPI",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "UPI • $formattedDate",
                            fontSize = 11.sp,
                            color = Color(0xFF44474E)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (transaction.isCredit) "+₹%,.2f".format(transaction.amount) else "-₹%,.2f".format(transaction.amount),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (transaction.isCredit) Color(0xFF3D691B) else Color(0xFF191C1E)
                )
                // Small badge to show status (Completed)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "SUCCESS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun UpiTransactionHistoryList(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val upiTransactions = remember(transactions) {
        transactions.filter { it.category == "UPI" }
    }

    if (upiTransactions.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F0F4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "No UPI payments",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "No UPI Payments Logged",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF191C1E)
            )
            Text(
                text = "Send or receive money via QR/UPI ID to see logs.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            upiTransactions.forEach { tx ->
                UpiTransactionItem(
                    transaction = tx,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiHistoryListDialog(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredUpiTransactions = remember(transactions, searchQuery) {
        transactions.filter { 
            it.category == "UPI" && (
                searchQuery.isEmpty() || 
                it.title.contains(searchQuery, ignoreCase = true) || 
                (it.note ?: "").contains(searchQuery, ignoreCase = true)
            )
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC4C7C5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "UPI History",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "UPI Payment History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36)
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F0F4))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search recipient or note", fontSize = 13.sp) },
                    leadingIcon = { 
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = "Search",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061A4),
                        focusedLabelColor = Color(0xFF0061A4)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredUpiTransactions.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No UPI transactions found",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(filteredUpiTransactions.size) { index ->
                            val tx = filteredUpiTransactions[index]
                            UpiTransactionItem(
                                transaction = tx,
                                onClick = { onTransactionClick(tx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatementDialog(
    transactions: List<Transaction>,
    viewModel: TeenPayViewModel,
    onDismissRequest: () -> Unit
) {
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val months = remember(transactions) {
        val uniques = transactions.map { monthFormatter.format(java.util.Date(it.timestamp)) }.distinct()
        if (uniques.isEmpty()) {
            val currentMonth = monthFormatter.format(java.util.Date())
            listOf(currentMonth)
        } else {
            uniques
        }
    }

    var selectedMonth by remember { mutableStateOf(months.first()) }
    val filteredTransactions = remember(transactions, selectedMonth) {
        transactions.filter { monthFormatter.format(java.util.Date(it.timestamp)) == selectedMonth }
    }

    val totalCredit = remember(filteredTransactions) {
        filteredTransactions.filter { it.isCredit }.sumOf { it.amount }
    }
    val totalDebit = remember(filteredTransactions) {
        filteredTransactions.filter { !it.isCredit }.sumOf { it.amount }
    }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isCompleted by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isDownloading) onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("monthly_statement_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC4C7C5))
        ) {
            if (isCompleted) {
                // Success screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .testTag("statement_download_success_screen"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE6F4EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF137333),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Text(
                        text = "Statement Generated!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF121212)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3F4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("File Name:", fontSize = 12.sp, color = Color.Gray)
                                val safeMonthName = selectedMonth.replace(" ", "_")
                                Text(
                                    text = "TeenPay_Statement_${safeMonthName}.pdf",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1B1F),
                                    modifier = Modifier.testTag("downloaded_file_name_text")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Selected Period:", fontSize = 12.sp, color = Color.Gray)
                                Text(selectedMonth, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transactions summarized:", fontSize = 12.sp, color = Color.Gray)
                                Text("${filteredTransactions.size} items", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Debits Tracker / Outflow:", fontSize = 11.sp, color = Color(0xFFB06000))
                                Text("₹${"%.2f".format(totalDebit)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Credits Tracker / Inflow:", fontSize = 11.sp, color = Color(0xFF137333))
                                Text("₹${"%.2f".format(totalCredit)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF137333))
                            }
                        }
                    }

                    Text(
                        text = "The file has been successfully downloaded and cached in your mobile directory 'Downloads/TeenPay/'",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isCompleted = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Change Month", fontSize = 12.sp, color = Color(0xFF0061A4))
                        }

                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_statement_to_device_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            } else {
                // Settings & summary preview screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Statement",
                                tint = Color(0xFF0061A4),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Monthly Statement",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )
                        }
                        IconButton(
                            onClick = onDismissRequest,
                            enabled = !isDownloading,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F0F4))

                    if (isDownloading) {
                        // Download Loader
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .testTag("downloading_progress_area"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF0061A4),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Compiling monthly ledger & securing keys...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF0061A4),
                                trackColor = Color(0xFFE1E2EC)
                            )
                            Text(
                                "Downloading: ${(downloadProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0061A4)
                            )
                        }
                    } else {
                        // Configure and pre-flight view
                        Text(
                            text = "Choose Month of Statement",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        // Scrollable month chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().testTag("monthly_statement_month_chips"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(months.size) { index ->
                                val month = months[index]
                                val isSelected = month == selectedMonth
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF0061A4) else Color(0xFFF1F0F4))
                                        .clickable { selectedMonth = month }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = month,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF1C1B1F)
                                    )
                                }
                            }
                        }

                        // Summary Statistics Card
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("statement_stats_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F5FC)),
                            border = BorderStroke(1.dp, Color(0xFFD3E3FD))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "$selectedMonth Wallet Statistics",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0061A4)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Inflow (+)", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            "₹${"%.2f".format(totalCredit)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF137333)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Outflow (-)", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            "₹${"%.2f".format(totalDebit)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFDFE2EB))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Net Month Savings:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    val netSavingsVal = totalCredit - totalDebit
                                    Text(
                                        text = (if (netSavingsVal >= 0) "+" else "") + "₹${"%.2f".format(netSavingsVal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (netSavingsVal >= 0) Color(0xFF137333) else Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }

                        // Short Preview list
                        Text(
                            text = "Month Ledger Preview (${filteredTransactions.size} items)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 130.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredTransactions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No transactions found for this month.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            } else {
                                items(filteredTransactions.size) { index ->
                                    val tx = filteredTransactions[index]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            val daySdf = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
                                            Text(daySdf.format(java.util.Date(tx.timestamp)), fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Text(
                                            text = (if (tx.isCredit) "+" else "-") + "₹${tx.amount.toInt()}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.isCredit) Color(0xFF137333) else Color(0xFF1C1B1F)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                isDownloading = true
                                downloadProgress = 0f
                                scope.launch {
                                    while (downloadProgress < 1.0f) {
                                        delay(150)
                                        downloadProgress += 0.1f
                                    }
                                    isDownloading = false
                                    isCompleted = true
                                    viewModel.showNotification("Downloaded Monthly Statement for $selectedMonth successfully!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_download_statement_btn_action"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download symbol",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Compile & Download Statement", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(onUnlock: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }
    var showPinFallback by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Pulsing circle animation for scan cue
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Unlocked state or Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0061A4).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Secured",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Text(
                    text = "TeenPay Locker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = if (showPinFallback) "Enter 4-Digit Security PIN" else "Biometric Verification Required",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            if (!showPinFallback) {
                // Biometric Scanning visual area
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .clickable(enabled = !isScanning && !scanSuccess) {
                            scope.launch {
                                isScanning = true
                                errorMessage = null
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                delay(1200)
                                isScanning = false
                                scanSuccess = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                delay(400)
                                onUnlock()
                            }
                        }
                ) {
                    // Pulsing Ring background
                    if (!isScanning && !scanSuccess) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = pulseAlpha))
                        )
                    }

                    // Scan Base Ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF1B263B),
                                        Color(0xFF0D1B2A)
                                    )
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = if (scanSuccess) Color(0xFF4ADE80) else Color(0xFF38BDF8).copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(100.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 3.dp
                            )
                        }

                        Crossfade(targetState = scanSuccess, label = "icon_fade") { success ->
                            if (success) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Access Granted",
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(56.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Scan Fingerprint",
                                    tint = if (isScanning) Color(0xFF38BDF8) else Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            scanSuccess -> "Lock Security Authorized!"
                            isScanning -> "Verifying biometric credentials..."
                            else -> "Tap sensor icon to scan fingerprint"
                        },
                        fontSize = 14.sp,
                        color = if (scanSuccess) Color(0xFF4ADE80) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (!isScanning && !scanSuccess) {
                        TextButton(
                            onClick = { showPinFallback = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "PIN",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Unlock with passcode instead",
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Passcode Keyboard Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // PIN dot indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val active = i < pinText.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (active) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Keypad grid
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("back", "0", "finger")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        keys.forEach { rowKeys ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowKeys.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (key.isEmpty()) Color.Transparent else Color.White.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (key.isNotEmpty() && key != "back" && key != "finger") Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable(enabled = key.isNotEmpty()) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                when (key) {
                                                    "back" -> {
                                                        if (pinText.isNotEmpty()) {
                                                            pinText = pinText.dropLast(1)
                                                        }
                                                    }
                                                    "finger" -> {
                                                        showPinFallback = false
                                                        pinText = ""
                                                        errorMessage = null
                                                    }
                                                    else -> {
                                                        if (pinText.length < 4) {
                                                            pinText += key
                                                            if (pinText.length == 4) {
                                                                // Verify PIN (let's accept "0000" as the default security PIN)
                                                                if (pinText == "0000") {
                                                                    scope.launch {
                                                                        scanSuccess = true
                                                                        delay(300)
                                                                        onUnlock()
                                                                    }
                                                                } else {
                                                                    errorMessage = "Incorrect PIN code. Try '0000'"
                                                                    pinText = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (key) {
                                            "back" -> {
                                                Icon(
                                                    imageVector = Icons.Default.Backspace,
                                                    contentDescription = "Backspace",
                                                    tint = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            "finger" -> {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Biometric Scan",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = key,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
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
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun UpiPinResetDialog(
    profile: WalletCardProfile?,
    onResetSuccess: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var stage by remember { mutableStateOf(1) } // 1: Card Verification, 2: OTP Verification, 3: Set New PIN
    
    // Stage 1 Fields
    var cardDigitsInput by remember { mutableStateOf("") }
    var expiryInput by remember { mutableStateOf("") }
    var cvvInput by remember { mutableStateOf("") }
    var cardError by remember { mutableStateOf<String?>(null) }
    
    // Stage 2 Fields
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("482015") }
    var otpError by remember { mutableStateOf<String?>(null) }
    
    // Stage 3 Fields
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    
    val cleanCard = profile?.cardNumber?.replace(" ", "") ?: ""
    val expectedLast6 = if (cleanCard.length >= 6) cleanCard.takeLast(6) else "345678"
    val expectedExpiry = profile?.cardExpiry ?: "09/30"
    
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC4C7C5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reset UPI PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF001D36)
                    )
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
                
                HorizontalDivider(color = Color(0xFFC4C7C5).copy(alpha = 0.3f))
                
                AnimatedContent(targetState = stage, label = "stage_transition") { currentStage ->
                    when (currentStage) {
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Step 1: Verify your Debit Card",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0061A4)
                                )
                                Text(
                                    text = "Enter last 6 digits of your debit card & expiry date to proceed.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF44474E)
                                )
                                
                                OutlinedTextField(
                                    value = cardDigitsInput,
                                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) cardDigitsInput = it },
                                    label = { Text("Last 6 Digits of Card") },
                                    placeholder = { Text("e.g. 345678") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_card_digits")
                                )
                                
                                OutlinedTextField(
                                    value = expiryInput,
                                    onValueChange = { if (it.length <= 5) expiryInput = it },
                                    label = { Text("Expiry Date (MM/YY)") },
                                    placeholder = { Text("e.g. 09/30") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_card_expiry")
                                )

                                OutlinedTextField(
                                    value = cvvInput,
                                    onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) cvvInput = it },
                                    label = { Text("CVV") },
                                    placeholder = { Text("e.g. 733") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_card_cvv")
                                )
                                
                                if (cardError != null) {
                                    Text(
                                        text = cardError!!,
                                        color = Color(0xFFBA1A1A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        if (cardDigitsInput != expectedLast6) {
                                            cardError = "Incorrect card digits. Hint: try $expectedLast6"
                                        } else if (expiryInput != expectedExpiry) {
                                            cardError = "Incorrect expiry date. Hint: try $expectedExpiry"
                                        } else if (cvvInput.length < 3) {
                                            cardError = "Please enter 3-digit CVV"
                                        } else {
                                            cardError = null
                                            stage = 2
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_verify_card_btn")
                                ) {
                                    Text("Verify and Request OTP")
                                }
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Step 2: Enter OTP sent to registered number",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0061A4)
                                )
                                Text(
                                    text = "We have sent a secure authentication code to your registered mobile number.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF44474E)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F0FE), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "📱 Mock SMS message:\nYour TeenPay OTP is: $generatedOtp",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF002244)
                                    )
                                }
                                
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) otpInput = it },
                                    label = { Text("6-Digit OTP") },
                                    placeholder = { Text("e.g. 482015") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_otp_input")
                                )
                                
                                if (otpError != null) {
                                    Text(
                                        text = otpError!!,
                                        color = Color(0xFFBA1A1A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = {
                                        generatedOtp = (100000..999999).random().toString()
                                        otpInput = ""
                                        otpError = null
                                    }) {
                                        Text("Resend OTP", fontSize = 12.sp)
                                    }
                                    
                                    TextButton(onClick = {
                                        otpInput = generatedOtp
                                    }) {
                                        Text("Auto Fill OTP", fontSize = 12.sp)
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        if (otpInput != generatedOtp) {
                                            otpError = "Incorrect OTP. Please enter the code sent in SMS."
                                        } else {
                                            otpError = null
                                            stage = 3
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_verify_otp_btn")
                                ) {
                                    Text("Verify OTP")
                                }
                            }
                        }
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Step 3: Setup New UPI PIN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0061A4)
                                )
                                Text(
                                    text = "Set a secure 4-digit numeric PIN for your UPI bank transfers.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF44474E)
                                )
                                
                                OutlinedTextField(
                                    value = newPinInput,
                                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinInput = it },
                                    label = { Text("Enter New 4-Digit PIN") },
                                    placeholder = { Text("e.g. 5678") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_new_pin")
                                )
                                
                                OutlinedTextField(
                                    value = confirmPinInput,
                                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPinInput = it },
                                    label = { Text("Confirm New 4-Digit PIN") },
                                    placeholder = { Text("e.g. 5678") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_confirm_pin")
                                )
                                
                                if (pinError != null) {
                                    Text(
                                        text = pinError!!,
                                        color = Color(0xFFBA1A1A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        if (newPinInput.length != 4) {
                                            pinError = "PIN must be exactly 4 digits."
                                        } else if (newPinInput != confirmPinInput) {
                                            pinError = "PINs do not match. Please verify."
                                        } else {
                                            pinError = null
                                            onResetSuccess(newPinInput)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("upi_reset_final_btn")
                                ) {
                                    Text("Reset UPI PIN & Save")
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
fun PasswordLockScreen(
    viewModel: TeenPayViewModel,
    onUnlock: () -> Unit
) {
    val appPassword by viewModel.appPassword.collectAsState()
    var enteredPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isPasswordIncorrect by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Icon space
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0061A4).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secured Lock",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(40.dp)
                )
            }

            // Display Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TeenPay Locker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Enter password to unlock application",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Input Field
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = enteredPassword,
                    onValueChange = {
                        enteredPassword = it
                        if (isPasswordIncorrect) isPasswordIncorrect = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lock_password_input_field"),
                    label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF38BDF8)
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.testTag("lock_password_toggle_visibility")
                        ) {
                            Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                )

                if (isPasswordIncorrect) {
                    Text(
                        text = "Incorrect password. Please try again.",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp).testTag("lock_password_error_text")
                    )
                }
            }

            // Unlock Button
            Button(
                onClick = {
                    if (enteredPassword == appPassword) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUnlock()
                    } else {
                        isPasswordIncorrect = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("lock_password_unlock_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Unlock icon", tint = Color.White)
                    Text("Unlock App", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SpendingInsightsBentoCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val months = remember(transactions) {
        val uniques = transactions.filter { !it.isCredit }
            .map { monthFormatter.format(Date(it.timestamp)) }
            .distinct()
        if (uniques.isEmpty()) {
            val currentMonth = monthFormatter.format(Date())
            listOf(currentMonth)
        } else {
            uniques
        }
    }

    var selectedMonth by remember(months) { mutableStateOf(months.first()) }
    val filteredDebits = remember(transactions, selectedMonth) {
        transactions.filter { !it.isCredit && monthFormatter.format(Date(it.timestamp)) == selectedMonth }
    }

    Card(
        modifier = modifier.testTag("spending_insights_bento_card"),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C7C5)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Content
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
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Analysis Icon",
                        tint = Color(0xFF0061A4),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Spending Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )
                }

                // Month dropdown/chips or badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F0FE))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedMonth,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0061A4)
                    )
                }
            }

            // Month selection chips if multiple months exist
            if (months.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("spending_insights_month_chips")
                ) {
                    items(months.size) { index ->
                        val mName = months[index]
                        val isSelected = mName == selectedMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF0061A4) else Color(0xFFF1F0F4))
                                .clickable { selectedMonth = mName }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF44474E)
                            )
                        }
                    }
                }
            }

            if (filteredDebits.isEmpty()) {
                // Empty state card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(16.dp))
                        .padding(24.dp)
                        .testTag("spending_insights_empty_state"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = Color.LightGray,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "No recorded debits",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Start sending money via UPI or Gold SIP to visualize your spending charts here.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Let's draw our beautiful Donut Chart
                val categoryTotals = remember(filteredDebits) {
                    filteredDebits.groupBy { it.category }
                        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                }

                val categoryColors = remember {
                    mapOf(
                        "Wallet" to Color(0xFF1976D2), // Blue
                        "UPI" to Color(0xFF388E3C),    // Green
                        "Gold" to Color(0xFFFBC02D),   // Gold
                        "Savings" to Color(0xFFD32F2F),// Red
                        "Reward" to Color(0xFF7B1FA2), // Purple
                        "Default" to Color(0xFF607D8B) // Slate Gray
                    )
                }

                val categoryIcons = remember {
                    mapOf(
                        "Wallet" to Icons.Default.Wallet,
                        "UPI" to Icons.Default.QrCodeScanner,
                        "Gold" to Icons.Default.MonetizationOn,
                        "Savings" to Icons.Default.Savings,
                        "Reward" to Icons.Default.CardMembership,
                        "Default" to Icons.Default.Category
                    )
                }

                val totalSpend = remember(categoryTotals) { categoryTotals.values.sum() }

                // We construct the segments list
                val segments = remember(categoryTotals, totalSpend) {
                    var currentAngle = -90f
                    categoryTotals.map { (cat, amount) ->
                        val percentage = (amount / totalSpend).toFloat()
                        val sweepAngle = percentage * 360f
                        val startAngle = currentAngle
                        currentAngle += sweepAngle
                        val col = categoryColors[cat] ?: categoryColors["Default"]!!
                        val icon = categoryIcons[cat] ?: categoryIcons["Default"]!!
                        SpendingSegment(
                            category = cat,
                            amount = amount,
                            percentage = percentage,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            color = col,
                            icon = icon
                        )
                    }
                }

                var selectedSegment by remember { mutableStateOf<SpendingSegment?>(null) }

                // Main visual panel holding Chart & Info block
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("spending_insights_chart_and_stats"),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leftside: Donut Chart Canvas with centered Text
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .testTag("spending_insights_donut_chart_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    // Clear selection on clicking the white space
                                    selectedSegment = null
                                }
                        ) {
                            segments.forEach { segment ->
                                val isSelected = selectedSegment?.category == segment.category
                                val strokeWidth = if (isSelected) 34f else 22f
                                val stroke = Stroke(
                                    width = strokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawArc(
                                    color = segment.color,
                                    startAngle = segment.startAngle,
                                    sweepAngle = segment.sweepAngle,
                                    useCenter = false,
                                    style = stroke,
                                    size = size
                                )
                            }
                        }

                        // Center Hole text info
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (selectedSegment != null) {
                                Text(
                                    text = selectedSegment!!.category,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "₹${selectedSegment!!.amount.toInt()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = selectedSegment!!.color
                                )
                                Text(
                                    text = "${(selectedSegment!!.percentage * 100).toInt()}% of spend",
                                    fontSize = 9.sp,
                                    color = Color.DarkGray
                                )
                            } else {
                                Text(
                                    text = "Total Spent",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "₹${totalSpend.toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF191C1E)
                                )
                                Text(
                                    text = "${categoryTotals.size} sectors",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Rightside: Mini Breakdown of categories
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        segments.forEach { segment ->
                            val isSelected = selectedSegment?.category == segment.category
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) segment.color.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) segment.color else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedSegment = if (isSelected) null else segment
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("spending_insights_category_row_${segment.category}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(segment.color)
                                    )
                                    Text(
                                        text = segment.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF191C1E)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${segment.amount.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF191C1E)
                                    )
                                    Text(
                                        text = "${(segment.percentage * 100).toInt()}%",
                                        fontSize = 8.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom actionable contextual tips or dynamic spending guide!
                val topSpentCategory = remember(categoryTotals) {
                    categoryTotals.maxByOrNull { it.value }?.key ?: "Other"
                }

                val customGuideMessage = remember(topSpentCategory) {
                    when (topSpentCategory) {
                        "UPI" -> "Quick Tip: You spent most using UPI. Scans of small merchant QR codes sum up quickly. Review daily notes to save!"
                        "Gold" -> "Superb Habit: Your highest cash outflow went into pure DigiGold savings! Buying gold consistently secures your savings."
                        "Savings" -> "Brilliant Work: You directed most of your money directly into targeted savings vaults! Keep that streak active."
                        "Wallet" -> "Manage Balance: Most of your spending took place inside your local TeenPay wallet. Consider setting triggers."
                        else -> "Spend Alert: Make sure to check details for all categorized outflows. Balance your daily transactions."
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("spending_insights_tip_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6FA))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Idea",
                            tint = Color(0xFFB06000),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = customGuideMessage,
                            fontSize = 10.sp,
                            color = Color(0xFF44474E),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

data class SpendingSegment(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    val icon: ImageVector
)


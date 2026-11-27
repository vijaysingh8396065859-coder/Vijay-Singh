package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.signOutUser
import com.example.data.Customer
import com.example.data.DairyFormatters
import com.example.data.DairyRepository
import com.example.data.MilkEntry
import com.example.data.MonthlyCustomerStatement
import com.example.data.PaymentRecord
import com.example.ui.auth.AuthScreen
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.SplashOverlay
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.MilkEntryFormDialog
import com.example.ui.components.PaymentFormDialog
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.SupplierHisaabScreen
import com.example.ui.screens.SupplierProfileScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.DairyDashboardScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.RateChartScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TodayScreen
import com.example.update.AppUpdate
import kotlinx.coroutines.launch
import com.example.ui.theme.MilkDiaryTheme
import com.example.viewmodel.DairyDataSnapshot
import com.example.viewmodel.DairyViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_TODAY = "today"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_CUSTOMERS = "customers"
const val ROUTE_PAYMENTS = "payments"
const val ROUTE_REPORTS = "reports"
const val ROUTE_RATE_CHART = "rate_chart"
const val ROUTE_COLLECTION = "collection"
const val ROUTE_SUPPLIER_HISAAB = "supplier_hisaab"
const val ROUTE_SUPPLIER_PROFILE = "supplier_profile/{supplierName}"
const val ROUTE_CUSTOMER_DETAIL = "customer_detail/{customerId}"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryDestinations = listOf(
    NavDestinationItem(ROUTE_TODAY, R.string.nav_today, Icons.Rounded.Today, Icons.Outlined.Today),
    NavDestinationItem(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Rounded.Storefront, Icons.Outlined.Storefront),
    NavDestinationItem(ROUTE_CUSTOMERS, R.string.nav_customers, Icons.Rounded.Group, Icons.Outlined.Group),
    NavDestinationItem(ROUTE_PAYMENTS, R.string.nav_payments, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    NavDestinationItem(ROUTE_REPORTS, R.string.nav_reports, Icons.Rounded.Assessment, Icons.Outlined.Assessment)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.notifications.CollectionReminders.createChannel(this)
        com.example.notifications.CollectionReminders.scheduleAll(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                9001
            )
        }
        setContent {
            MilkDiaryTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    var showSplash by remember { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize()) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
        } else {
            key(user.uid) {
                MilkDiaryMainShell(
                    currentUserId = user.uid,
                    userDisplayName = user.displayName,
                    userEmail = user.email
                )
            }
        }
        if (showSplash) {
            SplashOverlay(onFinished = { showSplash = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkDiaryMainShell(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DairyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DairyViewModel(DairyRepository(db), currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val selectedShift by viewModel.selectedShift.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // --- App update (GitHub Releases se) ---
    var pendingUpdate by remember { mutableStateOf<AppUpdate.UpdateInfo?>(null) }
    var updateDownloading by remember { mutableStateOf(false) }

    fun startUpdateDownload(info: AppUpdate.UpdateInfo) {
        if (!AppUpdate.canInstall(context)) {
            context.startActivity(AppUpdate.installPermissionIntent(context))
            viewModel.notifyUser("Update के लिए एक बार इस app को install की अनुमति दो, फिर वापस आकर Update दबाओ")
            return
        }
        updateDownloading = true
        scope.launch {
            val file = AppUpdate.downloadApk(context, info)
            updateDownloading = false
            if (file != null) {
                pendingUpdate = null
                context.startActivity(AppUpdate.installIntent(context, file))
            } else {
                viewModel.notifyUser("Update download नहीं हो पाया — net check करके दोबारा try करो")
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val info = AppUpdate.fetchLatest()
            if (info != null &&
                AppUpdate.isNewer(info.versionName, AppUpdate.currentVersionName(context))
            ) {
                pendingUpdate = info
            }
        } catch (t: Throwable) {
            // Update check kabhi bhi app ko crash nahi karna chahiye
        }
    }

    val updateToShow = pendingUpdate
    if (updateToShow != null) {
        AlertDialog(
            onDismissRequest = { if (!updateDownloading) pendingUpdate = null },
            title = { Text("नया Update आया है — v${updateToShow.versionName}") },
            text = {
                Text(
                    if (updateDownloading) {
                        "Update download हो रहा है…"
                    } else {
                        updateToShow.notes.ifBlank {
                            "इस update में नये features और सुधार हैं। तुम्हारा सारा हिसाब सुरक्षित रहेगा।"
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !updateDownloading,
                    onClick = { startUpdateDownload(updateToShow) }
                ) { Text("Update करो") }
            },
            dismissButton = {
                TextButton(
                    enabled = !updateDownloading,
                    onClick = { pendingUpdate = null }
                ) { Text("बाद में") }
            }
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_TODAY
    val isPrimaryScreen = primaryDestinations.any { it.route == currentRoute }

    // Modal state holders
    var showCustomerForm by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    var showMilkEntryForm by remember { mutableStateOf(false) }
    var editingMilkEntry by remember { mutableStateOf<MilkEntry?>(null) }
    var preselectedMilkCustomer by remember { mutableStateOf<Customer?>(null) }

    var showPaymentForm by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var preselectedPaymentCustomer by remember { mutableStateOf<Customer?>(null) }

    var customerPendingDelete by remember { mutableStateOf<Customer?>(null) }
    var entryPendingDelete by remember { mutableStateOf<MilkEntry?>(null) }
    var paymentPendingDelete by remember { mutableStateOf<PaymentRecord?>(null) }

    val shareBill: (MonthlyCustomerStatement) -> Unit = { statement ->
        val text = viewModel.buildMonthlyBillText(statement, selectedMonthKey)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "मिल्क डायरी बिल - ${statement.customer.name}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "बिल शेयर करें"))
    }

    val navigateToPrimary: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val currentSnapshot = (uiState as? UiState.Success)?.data ?: DairyDataSnapshot()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail && isPrimaryScreen) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    primaryDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToPrimary(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag("nav_rail_${dest.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (isPrimaryScreen) {
                        TopAppBar(
                            title = {
                                val title = when (currentRoute) {
                                    ROUTE_TODAY -> stringResource(R.string.app_name)
                                    ROUTE_CUSTOMERS -> "ग्राहक खाता"
                                    ROUTE_PAYMENTS -> "डेली पेमेंट"
                                    ROUTE_REPORTS -> "मासिक हिसाब"
                                    else -> stringResource(R.string.app_name)
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        editingCustomer = null
                                        showCustomerForm = true
                                    },
                                    modifier = Modifier.testTag("top_add_customer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PersonAdd,
                                        contentDescription = stringResource(R.string.cd_add_customer),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!useNavigationRail && isPrimaryScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            primaryDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToPrimary(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag("nav_bar_${dest.route}")
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (isPrimaryScreen && uiState is UiState.Success) {
                        when (currentRoute) {
                            ROUTE_TODAY -> if (currentSnapshot.customers.isNotEmpty()) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        editingMilkEntry = null
                                        preselectedMilkCustomer = null
                                        showMilkEntryForm = true
                                    },
                                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                    text = { Text(stringResource(R.string.action_add_milk)) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("fab_add_milk")
                                )
                            }
                            ROUTE_CUSTOMERS -> ExtendedFloatingActionButton(
                                onClick = {
                                    editingCustomer = null
                                    showCustomerForm = true
                                },
                                icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                                text = { Text(stringResource(R.string.action_add_customer)) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_add_customer")
                            )
                            ROUTE_PAYMENTS -> if (currentSnapshot.customers.isNotEmpty()) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        editingPayment = null
                                        preselectedPaymentCustomer = null
                                        showPaymentForm = true
                                    },
                                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                    text = { Text(stringResource(R.string.action_add_payment)) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("fab_add_payment")
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            ) { innerPadding ->
                when (val state = uiState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "मिल्क डायरी लोड हो रही है...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "डेटा लोड नहीं हो सका",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is UiState.Success -> {
                        val snapshot = state.data
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_TODAY,
                            enterTransition = {
                                fadeIn(animationSpec = tween(280)) +
                                    slideInHorizontally(animationSpec = tween(280)) { it / 5 }
                            },
                            exitTransition = { fadeOut(animationSpec = tween(200)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(280)) },
                            popExitTransition = { fadeOut(animationSpec = tween(200)) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable(ROUTE_TODAY) {
                                TodayScreen(
                                    snapshot = snapshot,
                                    selectedDateKey = selectedDateKey,
                                    selectedShift = selectedShift,
                                    onChangeDate = viewModel::changeSelectedDate,
                                    onResetToday = viewModel::resetToToday,
                                    onSelectShift = viewModel::selectShift,
                                    onAddCustomerClick = {
                                        editingCustomer = null
                                        showCustomerForm = true
                                    },
                                    onQuickLogMilk = { customer, qty ->
                                        viewModel.saveMilkEntry(
                                            existingEntryId = null,
                                            customer = customer,
                                            dateKey = selectedDateKey,
                                            shift = selectedShift,
                                            milkType = customer.parsedMilkType,
                                            quantityLiters = qty,
                                            ratePerLiter = customer.ratePerLiter,
                                            absent = false,
                                            note = ""
                                        )
                                    },
                                    onQuickMarkAbsent = { customer ->
                                        viewModel.saveMilkEntry(
                                            existingEntryId = null,
                                            customer = customer,
                                            dateKey = selectedDateKey,
                                            shift = selectedShift,
                                            milkType = customer.parsedMilkType,
                                            quantityLiters = 0.0,
                                            ratePerLiter = customer.ratePerLiter,
                                            absent = true,
                                            note = ""
                                        )
                                    },
                                    onQuickPaymentClick = { customer ->
                                        editingPayment = null
                                        preselectedPaymentCustomer = customer
                                        showPaymentForm = true
                                    },
                                    onEditEntryClick = { entry ->
                                        editingMilkEntry = entry
                                        preselectedMilkCustomer = null
                                        showMilkEntryForm = true
                                    },
                                    onDeleteEntryClick = { entry ->
                                        entryPendingDelete = entry
                                    },
                                    onOpenCustomerDetail = { custId ->
                                        navController.navigate("customer_detail/$custId")
                                    }
                                )
                            }

                            composable(ROUTE_DASHBOARD) {
                                DairyDashboardScreen(
                                    snapshot = snapshot,
                                    onOpenCollection = {
                                        navController.navigate(ROUTE_COLLECTION)
                                    },
                                    onOpenRateChart = {
                                        navController.navigate(ROUTE_RATE_CHART)
                                    },
                                    onOpenSupplierHisaab = {
                                        navController.navigate(ROUTE_SUPPLIER_HISAAB)
                                    },
                                    onCheckUpdate = {
                                        scope.launch {
                                            val info = AppUpdate.fetchLatest()
                                            if (info != null &&
                                                AppUpdate.isNewer(
                                                    info.versionName,
                                                    AppUpdate.currentVersionName(context)
                                                )
                                            ) {
                                                pendingUpdate = info
                                            } else {
                                                viewModel.notifyUser(
                                                    "App बिल्कुल up-to-date है — कोई नया update नहीं है"
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_COLLECTION) {
                                CollectionScreen(
                                    snapshot = snapshot,
                                    selectedDateKey = selectedDateKey,
                                    onBack = { navController.popBackStack() },
                                    onSaveCollection = { name, sample, colShift, qty, fat, snf, rate, total ->
                                        viewModel.saveCollectionEntry(
                                            supplierName = name,
                                            sampleNo = sample,
                                            dateKey = selectedDateKey,
                                            shift = colShift,
                                            quantityLiters = qty,
                                            fat = fat,
                                            snf = snf,
                                            ratePerLiter = rate,
                                            totalAmount = total
                                        )
                                    },
                                    onDeleteCollection = viewModel::deleteCollectionEntry,
                                    onUpdateFatSnf = { colEntry, fVal, sVal ->
                                        viewModel.updateCollectionFatSnf(colEntry, fVal, sVal)
                                    },
                                    onAddGoods = { gName, gItem, gQty, gUnit, gRate, gAmt ->
                                        viewModel.addSupplierTransaction(
                                            supplierName = gName,
                                            type = "GOODS",
                                            itemName = gItem,
                                            quantity = gQty,
                                            unit = gUnit,
                                            ratePerUnit = gRate,
                                            amount = gAmt,
                                            paymentMode = "CASH",
                                            note = "",
                                            dateKey = selectedDateKey,
                                            isHome = false
                                        )
                                    },
                                    onAddSupplier = { sName, sPhone, sVillage ->
                                        viewModel.addSupplier(sName, sPhone, sVillage)
                                    },
                                    onOpenProfile = { pName ->
                                        navController.navigate(
                                            "supplier_profile/" + Uri.encode(pName)
                                        )
                                    },
                                    onDeleteSupplier = viewModel::deleteSupplier
                                )
                            }

                            composable(ROUTE_SUPPLIER_HISAAB) {
                                SupplierHisaabScreen(
                                    snapshot = snapshot,
                                    onBack = { navController.popBackStack() },
                                    onAddTransaction = { sName, sType, sItem, sQty, sUnit, sRate, sAmount, sMode, sNote, sDate, sHome ->
                                        viewModel.addSupplierTransaction(
                                            supplierName = sName,
                                            type = sType,
                                            itemName = sItem,
                                            quantity = sQty,
                                            unit = sUnit,
                                            ratePerUnit = sRate,
                                            amount = sAmount,
                                            paymentMode = sMode,
                                            note = sNote,
                                            dateKey = sDate,
                                            isHome = sHome
                                        )
                                    },
                                    onDeleteTransaction = viewModel::deleteSupplierTransaction,
                                    onOpenProfile = { pName ->
                                        navController.navigate(
                                            "supplier_profile/" + Uri.encode(pName)
                                        )
                                    }
                                )
                            }

                            composable(
                                route = ROUTE_SUPPLIER_PROFILE,
                                arguments = listOf(
                                    navArgument("supplierName") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val pName = backStackEntry.arguments
                                    ?.getString("supplierName").orEmpty()
                                SupplierProfileScreen(
                                    snapshot = snapshot,
                                    supplierName = pName,
                                    onBack = { navController.popBackStack() },
                                    onUpdateDetails = { sId, sPhone, sVillage ->
                                        viewModel.updateSupplierDetails(sId, sPhone, sVillage)
                                    }
                                )
                            }

                            composable(ROUTE_RATE_CHART) {
                                RateChartScreen(
                                    snapshot = snapshot,
                                    onBack = { navController.popBackStack() },
                                    onSaveRate = viewModel::saveRateOverride,
                                    onResetRate = viewModel::resetRateOverride,
                                    onImportChart = { rates ->
                                        viewModel.importRateChart(rates, "user-import")
                                    },
                                    onClearImportedChart = viewModel::clearImportedChart
                                )
                            }

                            composable(ROUTE_CUSTOMERS) {
                                CustomersScreen(
                                    snapshot = snapshot,
                                    onAddCustomerClick = {
                                        editingCustomer = null
                                        showCustomerForm = true
                                    },
                                    onOpenCustomerDetail = { custId ->
                                        navController.navigate("customer_detail/$custId")
                                    },
                                    onAddMilkForCustomer = { customer ->
                                        editingMilkEntry = null
                                        preselectedMilkCustomer = customer
                                        showMilkEntryForm = true
                                    },
                                    onAddPaymentForCustomer = { customer ->
                                        editingPayment = null
                                        preselectedPaymentCustomer = customer
                                        showPaymentForm = true
                                    }
                                )
                            }

                            composable(ROUTE_PAYMENTS) {
                                PaymentsScreen(
                                    snapshot = snapshot,
                                    selectedMonthKey = selectedMonthKey,
                                    onChangeMonth = viewModel::changeSelectedMonth,
                                    onAddPaymentClick = {
                                        editingPayment = null
                                        preselectedPaymentCustomer = null
                                        showPaymentForm = true
                                    },
                                    onEditPaymentClick = { payment ->
                                        editingPayment = payment
                                        preselectedPaymentCustomer = null
                                        showPaymentForm = true
                                    },
                                    onDeletePaymentClick = { payment ->
                                        paymentPendingDelete = payment
                                    },
                                    onOpenCustomerDetail = { custId ->
                                        navController.navigate("customer_detail/$custId")
                                    }
                                )
                            }

                            composable(ROUTE_REPORTS) {
                                ReportsScreen(
                                    snapshot = snapshot,
                                    selectedMonthKey = selectedMonthKey,
                                    userDisplayName = userDisplayName,
                                    userEmail = userEmail,
                                    onChangeMonth = viewModel::changeSelectedMonth,
                                    onOpenCustomerDetail = { custId ->
                                        navController.navigate("customer_detail/$custId")
                                    },
                                    onShareBill = shareBill,
                                    onSignOutClick = {
                                        signOutUser(
                                            credentialManager = credentialManager,
                                            scope = scope
                                        )
                                    }
                                )
                            }

                            composable(
                                route = ROUTE_CUSTOMER_DETAIL,
                                arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val custId = backStackEntry.arguments?.getString("customerId").orEmpty()
                                CustomerDetailScreen(
                                    customerId = custId,
                                    snapshot = snapshot,
                                    selectedMonthKey = selectedMonthKey,
                                    onChangeMonth = viewModel::changeSelectedMonth,
                                    onBack = { navController.popBackStack() },
                                    onEditCustomer = { cust ->
                                        editingCustomer = cust
                                        showCustomerForm = true
                                    },
                                    onDeleteCustomer = { cust ->
                                        customerPendingDelete = cust
                                    },
                                    onAddMilk = { cust ->
                                        editingMilkEntry = null
                                        preselectedMilkCustomer = cust
                                        showMilkEntryForm = true
                                    },
                                    onAddPayment = { cust ->
                                        editingPayment = null
                                        preselectedPaymentCustomer = cust
                                        showPaymentForm = true
                                    },
                                    onEditEntry = { entry ->
                                        editingMilkEntry = entry
                                        preselectedMilkCustomer = null
                                        showMilkEntryForm = true
                                    },
                                    onDeleteEntry = { entry ->
                                        entryPendingDelete = entry
                                    },
                                    onEditPayment = { payment ->
                                        editingPayment = payment
                                        preselectedPaymentCustomer = null
                                        showPaymentForm = true
                                    },
                                    onDeletePayment = { payment ->
                                        paymentPendingDelete = payment
                                    },
                                    onShareBill = shareBill
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- FORM & CONFIRMATION DIALOGS ---

    if (showCustomerForm) {
        CustomerFormDialog(
            existingCustomer = editingCustomer,
            onDismiss = {
                showCustomerForm = false
                editingCustomer = null
            },
            onSave = { name, phone, address, milkType, defaultShift, qty, rate, opening, active ->
                viewModel.saveCustomer(
                    existingId = editingCustomer?.id,
                    name = name,
                    phone = phone,
                    address = address,
                    milkType = milkType,
                    defaultShift = defaultShift,
                    defaultQtyLiters = qty,
                    ratePerLiter = rate,
                    openingBalance = opening,
                    active = active,
                    onComplete = {
                        showCustomerForm = false
                        editingCustomer = null
                    }
                )
            }
        )
    }

    if (showMilkEntryForm && currentSnapshot.customers.isNotEmpty()) {
        MilkEntryFormDialog(
            customers = currentSnapshot.customers,
            preselectedCustomer = preselectedMilkCustomer,
            existingEntry = editingMilkEntry,
            defaultDateKey = selectedDateKey,
            defaultShift = selectedShift,
            onDismiss = {
                showMilkEntryForm = false
                editingMilkEntry = null
                preselectedMilkCustomer = null
            },
            onSave = { customer, dateKey, shift, milkType, qty, rate, absent, note ->
                viewModel.saveMilkEntry(
                    existingEntryId = editingMilkEntry?.id,
                    customer = customer,
                    dateKey = dateKey,
                    shift = shift,
                    milkType = milkType,
                    quantityLiters = qty,
                    ratePerLiter = rate,
                    absent = absent,
                    note = note,
                    onComplete = {
                        showMilkEntryForm = false
                        editingMilkEntry = null
                        preselectedMilkCustomer = null
                    }
                )
            }
        )
    }

    if (showPaymentForm && currentSnapshot.customers.isNotEmpty()) {
        PaymentFormDialog(
            customers = currentSnapshot.customers,
            summaryByCustomerId = currentSnapshot.summaryByCustomerId,
            preselectedCustomer = preselectedPaymentCustomer,
            existingPayment = editingPayment,
            defaultDateKey = selectedDateKey,
            onDismiss = {
                showPaymentForm = false
                editingPayment = null
                preselectedPaymentCustomer = null
            },
            onSave = { customer, dateKey, amount, mode, note ->
                viewModel.savePayment(
                    existingPaymentId = editingPayment?.id,
                    customer = customer,
                    dateKey = dateKey,
                    amount = amount,
                    paymentMode = mode,
                    note = note,
                    onComplete = {
                        showPaymentForm = false
                        editingPayment = null
                        preselectedPaymentCustomer = null
                    }
                )
            }
        )
    }

    customerPendingDelete?.let { cust ->
        ConfirmDeleteDialog(
            title = "ग्राहक हटाएं?",
            message = "क्या आप \"${cust.name}\" को ग्राहक सूची से हटाना चाहते हैं?",
            onDismiss = { customerPendingDelete = null },
            onConfirm = {
                viewModel.deleteCustomer(cust.id) {
                    customerPendingDelete = null
                    if (currentRoute.startsWith("customer_detail")) {
                        navController.popBackStack()
                    }
                }
            }
        )
    }

    entryPendingDelete?.let { entry ->
        ConfirmDeleteDialog(
            title = "दूध एंट्री हटाएं?",
            message = "${entry.customerName} की ${DairyFormatters.formatShortDateHi(entry.dateKey)} की एंट्री हटाएं?",
            onDismiss = { entryPendingDelete = null },
            onConfirm = {
                viewModel.deleteMilkEntry(entry.id)
                entryPendingDelete = null
            }
        )
    }

    paymentPendingDelete?.let { payment ->
        ConfirmDeleteDialog(
            title = "पेमेंट हटाएं?",
            message = "${payment.customerName} का ${DairyFormatters.formatRupees(payment.amount)} का पेमेंट रिकॉर्ड हटाएं?",
            onDismiss = { paymentPendingDelete = null },
            onConfirm = {
                viewModel.deletePayment(payment.id)
                paymentPendingDelete = null
            }
        )
    }
}

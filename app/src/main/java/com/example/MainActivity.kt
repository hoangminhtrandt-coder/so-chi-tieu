package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.screens.AddEditTransactionSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: FinanceViewModel by viewModels {
        FinanceViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // State for Add / Edit Sheet
    var showAddSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var presetType by remember { mutableStateOf(TransactionType.EXPENSE) }

    if (uiState.isAppLocked) {
        LockScreen(
            onUnlocked = { viewModel.unlockApp() }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    val tabs = listOf(
                        Triple(0, "Trang chủ", Pair(Icons.Default.Home, Icons.Outlined.Home)),
                        Triple(1, "Thống kê", Pair(Icons.Default.PieChart, Icons.Outlined.PieChart)),
                        Triple(2, "Ngân sách", Pair(Icons.Default.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)),
                        Triple(3, "Cài đặt", Pair(Icons.Default.Settings, Icons.Outlined.Settings))
                    )

                    tabs.forEach { (index, title, icons) ->
                        val isSelected = uiState.activeTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setActiveTab(index) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) icons.first else icons.second,
                                    contentDescription = title
                                )
                            },
                            label = { Text(title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_$index")
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        transactionToEdit = null
                        presetType = TransactionType.EXPENSE
                        showAddSheet = true
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.testTag("main_add_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm giao dịch")
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.activeTab) {
                    0 -> HomeScreen(
                        uiState = uiState,
                        onAddTransactionClick = { type ->
                            transactionToEdit = null
                            presetType = type
                            showAddSheet = true
                        },
                        onTransactionClick = { tx ->
                            transactionToEdit = tx
                            showAddSheet = true
                        },
                        onNavigateToAnalytics = { viewModel.setActiveTab(1) },
                        onNavigateToBudgets = { viewModel.setActiveTab(2) },
                        onSyncClick = { viewModel.triggerCloudSync() }
                    )

                    1 -> AnalyticsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onTransactionClick = { tx ->
                            transactionToEdit = tx
                            showAddSheet = true
                        }
                    )

                    2 -> BudgetScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )

                    3 -> SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Add / Edit Transaction Bottom Sheet
        if (showAddSheet) {
            AddEditTransactionSheet(
                initialTransaction = transactionToEdit?.copy(type = if (transactionToEdit != null) transactionToEdit!!.type else presetType),
                onDismiss = {
                    showAddSheet = false
                    transactionToEdit = null
                },
                onSave = { id, type, amount, categoryId, categoryName, categoryIcon, categoryColor, date, note, wallet ->
                    viewModel.addOrUpdateTransaction(
                        id = id,
                        type = type,
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        categoryIcon = categoryIcon,
                        categoryColor = categoryColor,
                        date = date,
                        note = note,
                        wallet = wallet
                    )
                    showAddSheet = false
                    transactionToEdit = null
                },
                onDelete = { id ->
                    viewModel.deleteTransaction(id)
                    showAddSheet = false
                    transactionToEdit = null
                }
            )
        }
    }
}

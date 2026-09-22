package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FinanceApplication
import com.example.data.model.BudgetProgress
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.CategoryStat
import com.example.data.model.DefaultCategories
import com.example.data.model.TimeFilter
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.export.ExcelReportExporter
import com.example.export.PdfReportExporter
import com.example.security.BiometricAuthManager
import com.example.sync.CloudSyncManager
import com.example.sync.SyncState
import com.example.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class FinanceUiState(
    val allTransactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val timeFilter: TimeFilter = TimeFilter.MONTH,
    val statTypeFilter: TransactionType = TransactionType.EXPENSE,
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val todayExpense: Double = 0.0,
    val todayIncome: Double = 0.0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val budgetProgressList: List<BudgetProgress> = emptyList(),
    val totalBudget: Double = 0.0,
    val totalBudgetSpent: Double = 0.0,
    val isAppLocked: Boolean = false,
    val syncState: SyncState = SyncState.Idle,
    val activeTab: Int = 0, // 0: Home, 1: Charts, 2: Budgets, 3: Settings
    val syncId: String = "",
    val lastSyncTime: Long = 0L,
    val isBiometricEnabled: Boolean = true,
    val isSecurityLockEnabled: Boolean = false,
    val hasPin: Boolean = false
)

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    private val cloudSyncManager = CloudSyncManager(application, repository)

    private val _timeFilter = MutableStateFlow(TimeFilter.MONTH)
    private val _statTypeFilter = MutableStateFlow(TransactionType.EXPENSE)
    private val _activeTab = MutableStateFlow(0)
    private val _isAppLocked = MutableStateFlow(BiometricAuthManager.isSecurityLockEnabled(application))
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    private val calendar = Calendar.getInstance()
    private val currentMonth = calendar.get(Calendar.MONTH) + 1
    private val currentYear = calendar.get(Calendar.YEAR)

    private val budgetsFlow = repository.getBudgetsForMonth(currentMonth, currentYear)

    private data class FilterParams(
        val timeFilter: TimeFilter,
        val statType: TransactionType,
        val activeTab: Int,
        val isLocked: Boolean,
        val syncStatus: SyncState
    )

    private val filterParamsFlow = combine(
        _timeFilter,
        _statTypeFilter,
        _activeTab,
        _isAppLocked,
        _syncState
    ) { timeFilter, statType, tab, isLocked, syncStatus ->
        FilterParams(timeFilter, statType, tab, isLocked, syncStatus)
    }

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.allTransactions,
        budgetsFlow,
        filterParamsFlow
    ) { allTx, budgets, filters ->
        val timeFilter = filters.timeFilter
        val statType = filters.statType
        val tab = filters.activeTab
        val isLocked = filters.isLocked
        val syncStatus = filters.syncStatus

        // Calculate Range for timeFilter
        val (startTime, endTime) = when (timeFilter) {
            TimeFilter.TODAY -> TimeUtils.getDayRange()
            TimeFilter.WEEK -> TimeUtils.getWeekRange()
            TimeFilter.MONTH -> TimeUtils.getMonthRange(currentMonth, currentYear)
            TimeFilter.YEAR -> TimeUtils.getYearRange(currentYear)
            TimeFilter.ALL -> Pair(0L, Long.MAX_VALUE)
            else -> Pair(0L, Long.MAX_VALUE)
        }

        val filteredTx = allTx.filter { it.date in startTime..endTime }

        val totalIncome = filteredTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = filteredTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        // Today metrics
        val (todayStart, todayEnd) = TimeUtils.getDayRange()
        val todayTx = allTx.filter { it.date in todayStart..todayEnd }
        val todayExpense = todayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val todayIncome = todayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        // Category stats for selected statType (EXPENSE or INCOME)
        val typeFilteredTx = filteredTx.filter { it.type == statType }
        val typeTotal = typeFilteredTx.sumOf { it.amount }
        val categoryStats = typeFilteredTx
            .groupBy { it.categoryId }
            .map { (catId, items) ->
                val sum = items.sumOf { it.amount }
                val firstItem = items.first()
                val percent = if (typeTotal > 0) ((sum / typeTotal) * 100).toFloat() else 0f
                CategoryStat(
                    categoryId = catId,
                    categoryName = firstItem.categoryName,
                    iconName = firstItem.categoryIcon,
                    color = firstItem.categoryColor,
                    totalAmount = sum,
                    percentage = percent,
                    count = items.size
                )
            }
            .sortedByDescending { it.totalAmount }

        // Current month expenses for budget calculations
        val (monthStart, monthEnd) = TimeUtils.getMonthRange(currentMonth, currentYear)
        val monthExpenses = allTx.filter { it.date in monthStart..monthEnd && it.type == TransactionType.EXPENSE }
        val spentByCategory = monthExpenses.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val budgetProgressList = budgets.map { b ->
            val spent = spentByCategory[b.categoryId] ?: 0.0
            val remaining = b.monthlyLimit - spent
            val progress = if (b.monthlyLimit > 0) (spent / b.monthlyLimit).toFloat() else 0f
            val category = DefaultCategories.getCategoryById(b.categoryId)
            BudgetProgress(
                budgetId = b.id,
                categoryId = b.categoryId,
                categoryName = b.categoryName,
                iconName = category?.iconName ?: "Category",
                color = category?.color ?: 0xFF0F766E,
                limit = b.monthlyLimit,
                spent = spent,
                remaining = remaining,
                progressPercent = progress,
                isExceeded = spent > b.monthlyLimit,
                isNearLimit = progress in 0.8f..0.999f
            )
        }.sortedByDescending { it.progressPercent }

        val totalBudget = budgets.sumOf { it.monthlyLimit }
        val totalBudgetSpent = budgetProgressList.sumOf { it.spent }

        val context = getApplication<Application>()
        val hasPin = BiometricAuthManager.getPinCode(context) != null
        val isSecurityEnabled = BiometricAuthManager.isSecurityLockEnabled(context)
        val isBioEnabled = BiometricAuthManager.isBiometricEnabled(context)

        FinanceUiState(
            allTransactions = allTx,
            filteredTransactions = filteredTx,
            timeFilter = timeFilter,
            statTypeFilter = statType,
            currentMonth = currentMonth,
            currentYear = currentYear,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            todayExpense = todayExpense,
            todayIncome = todayIncome,
            categoryStats = categoryStats,
            budgetProgressList = budgetProgressList,
            totalBudget = totalBudget,
            totalBudgetSpent = totalBudgetSpent,
            isAppLocked = isLocked,
            syncState = syncStatus,
            activeTab = tab,
            syncId = cloudSyncManager.getSyncId(),
            lastSyncTime = cloudSyncManager.getLastSyncTime(),
            isBiometricEnabled = isBioEnabled,
            isSecurityLockEnabled = isSecurityEnabled,
            hasPin = hasPin
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun setStatTypeFilter(type: TransactionType) {
        _statTypeFilter.value = type
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (BiometricAuthManager.isSecurityLockEnabled(getApplication())) {
            _isAppLocked.value = true
        }
    }

    fun addOrUpdateTransaction(
        id: Long = 0,
        type: TransactionType,
        amount: Double,
        categoryId: String,
        categoryName: String,
        categoryIcon: String,
        categoryColor: Long,
        date: Long,
        note: String,
        wallet: String
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertTransaction(
                    TransactionEntity(
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
                )
            } else {
                repository.updateTransaction(
                    TransactionEntity(
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
                )
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun saveCategoryBudget(categoryId: String, categoryName: String, limit: Double) {
        viewModelScope.launch {
            repository.setBudget(
                categoryId = categoryId,
                categoryName = categoryName,
                limit = limit,
                month = currentMonth,
                year = currentYear
            )
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = cloudSyncManager.syncWithCloud()
            _syncState.value = result
        }
    }

    fun updateSyncId(newId: String) {
        cloudSyncManager.setSyncId(newId)
        triggerCloudSync()
    }

    fun exportPdfReport(): File? {
        val context = getApplication<Application>()
        val transactions = uiState.value.filteredTransactions.ifEmpty { uiState.value.allTransactions }
        return PdfReportExporter.exportMonthlyReport(
            context = context,
            month = currentMonth,
            year = currentYear,
            transactions = transactions
        )
    }

    fun exportExcelReport(): File? {
        val context = getApplication<Application>()
        val title = "${uiState.value.timeFilter.title} (Tháng $currentMonth/$currentYear)"
        val transactions = uiState.value.filteredTransactions.ifEmpty { uiState.value.allTransactions }
        return ExcelReportExporter.exportToExcel(
            context = context,
            periodTitle = title,
            transactions = transactions
        )
    }

    fun setSecurityLock(enabled: Boolean) {
        val context = getApplication<Application>()
        BiometricAuthManager.setSecurityLockEnabled(context, enabled)
    }

    fun setBiometric(enabled: Boolean) {
        val context = getApplication<Application>()
        BiometricAuthManager.setBiometricEnabled(context, enabled)
    }

    fun setPinCode(pin: String) {
        val context = getApplication<Application>()
        BiometricAuthManager.setPinCode(context, pin)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = application as FinanceApplication
            return FinanceViewModel(app, app.repository) as T
        }
    }
}

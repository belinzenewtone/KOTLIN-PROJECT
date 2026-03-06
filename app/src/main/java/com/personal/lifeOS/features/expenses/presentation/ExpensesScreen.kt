package com.personal.lifeOS.features.expenses.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.model.TransactionFilter
import com.personal.lifeOS.ui.components.AccentGlassCard
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.*

@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { com.personal.lifeOS.ui.components.StyledSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 80.dp),
                onClick = { viewModel.showAddDialog() },
                containerColor = Primary,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, "Add transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Expenses", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Track your MPESA transactions",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.showImportDialog() }
                    ) {
                        Text("Import SMS", color = Primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Spending summary cards
            item { SpendingSummaryRow(state) }

            // Filter chips
            item { FilterChips(state.selectedFilter) { viewModel.setFilter(it) } }

            // Category breakdown
            if (state.summary.categoryBreakdown.isNotEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Categories", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            state.summary.categoryBreakdown.forEach { cat ->
                                CategoryRow(
                                    category = cat.category,
                                    amount = cat.total,
                                    percentage = cat.percentage,
                                    onClick = { viewModel.filterByCategory(cat.category) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Transaction list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transactions", style = MaterialTheme.typography.titleMedium)
                    if (state.selectedCategory != null) {
                        TextButton(onClick = { viewModel.filterByCategory(null) }) {
                            Text("Clear filter", color = Primary)
                        }
                    }
                }
            }

            // Loading
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            }

            // Empty state
            if (!state.isLoading && state.transactions.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No transactions yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "MPESA transactions will appear automatically, or tap + to add manually",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Transaction items
            items(state.transactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onDelete = { viewModel.delete(transaction) },
                    onRecategorize = { viewModel.showCategoryPicker(transaction) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Add transaction dialog
    if (state.showAddDialog) {
        AddTransactionDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onAdd = { amount, merchant, category ->
                viewModel.addManualTransaction(amount, merchant, category)
            }
        )
    }

    // Category picker dialog
    state.showCategoryPicker?.let { tx ->
        CategoryPickerDialog(
            currentCategory = tx.category,
            onDismiss = { viewModel.hideCategoryPicker() },
            onSelect = { newCategory -> viewModel.recategorize(tx, newCategory) }
        )
    }

    // SMS Import dialog
    if (state.showImportDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.hideImportDialog() },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Import MPESA SMS", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select time period to scan:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    listOf(
                        "Last 24 hours" to 1,
                        "Last 7 days" to 7,
                        "Last 30 days" to 30,
                        "Last 90 days" to 90
                    ).forEach { (label, days) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GlassWhite)
                                .clickable {
                                    viewModel.importSmsMessages(context.contentResolver, days)
                                }
                                .padding(16.dp)
                        ) {
                            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hideImportDialog() }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SpendingSummaryRow(state: ExpensesUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentGlassCard(modifier = Modifier.width(160.dp)) {
            Column {
                Text("Today", style = MaterialTheme.typography.labelSmall)
                Text(
                    DateUtils.formatCurrency(state.summary.todayTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GlassCard(modifier = Modifier.width(160.dp)) {
            Column {
                Text("This Week", style = MaterialTheme.typography.labelSmall)
                Text(
                    DateUtils.formatCurrency(state.summary.weekTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        GlassCard(modifier = Modifier.width(160.dp)) {
            Column {
                Text("This Month", style = MaterialTheme.typography.labelSmall)
                Text(
                    DateUtils.formatCurrency(state.summary.monthTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterChips(
    selected: TransactionFilter,
    onSelect: (TransactionFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Primary else GlassWhite)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (filter) {
                        TransactionFilter.ALL -> "All"
                        TransactionFilter.TODAY -> "Today"
                        TransactionFilter.THIS_WEEK -> "This Week"
                        TransactionFilter.THIS_MONTH -> "This Month"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) BackgroundDark else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: String,
    amount: Double,
    percentage: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(getCategoryColor(category).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                getCategoryIcon(category),
                contentDescription = null,
                tint = getCategoryColor(category),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassWhite)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(getCategoryColor(category))
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            DateUtils.formatCurrency(amount),
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    onRecategorize: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(transaction.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getCategoryIcon(transaction.category),
                    contentDescription = null,
                    tint = getCategoryColor(transaction.category),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = getCategoryColor(transaction.category)
                    )
                    Text(
                        DateUtils.formatDate(transaction.date, "MMM dd, h:mm a"),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (transaction.mpesaCode != null) {
                    Text(
                        transaction.mpesaCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    DateUtils.formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (transaction.transactionType == "RECEIVED") Accent else TextPrimary
                )
                Row {
                    IconButton(onClick = onRecategorize, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Edit, "Edit", tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, "Delete", tint = Error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }

    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Subscriptions", "Savings", "Groceries", "Airtime", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Add Transaction", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (KES)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = GlassBorder
                    )
                )
                Text("Category", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (cat == category) Primary else GlassWhite)
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (cat == category) BackgroundDark else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt != null && merchant.isNotBlank()) {
                        onAdd(amt, merchant, category)
                    }
                }
            ) { Text("Add", color = Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun CategoryPickerDialog(
    currentCategory: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val categories = listOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Subscriptions", "Savings", "Groceries", "Airtime", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Change Category", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (cat == currentCategory) Primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onSelect(cat) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getCategoryIcon(cat),
                            contentDescription = null,
                            tint = getCategoryColor(cat),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(cat, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food" -> CategoryFood
        "transport" -> CategoryTransport
        "bills" -> CategoryBills
        "shopping" -> CategoryShopping
        "entertainment" -> CategoryEntertainment
        "subscriptions" -> CategorySubscriptions
        "savings" -> CategorySavings
        "groceries" -> CategoryFood
        "airtime" -> CategoryBills
        else -> CategoryOther
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Filled.Restaurant
        "transport" -> Icons.Filled.DirectionsBus
        "bills" -> Icons.Filled.Receipt
        "shopping" -> Icons.Filled.ShoppingBag
        "entertainment" -> Icons.Filled.Subscriptions
        "subscriptions" -> Icons.Filled.Subscriptions
        "savings" -> Icons.Filled.Savings
        "groceries" -> Icons.Filled.LocalGroceryStore
        "airtime" -> Icons.Filled.PhoneAndroid
        else -> Icons.Filled.MoreHoriz
    }
}

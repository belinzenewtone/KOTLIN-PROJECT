package com.personal.lifeOS.features.expenses.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.expenses.domain.model.CategoryBreakdown
import com.personal.lifeOS.features.expenses.domain.model.SpendingSummary
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.model.TransactionFilter
import com.personal.lifeOS.ui.components.AccentGlassCard
import com.personal.lifeOS.ui.components.GlassCard
import com.personal.lifeOS.ui.theme.CategoryBills
import com.personal.lifeOS.ui.theme.CategoryEntertainment
import com.personal.lifeOS.ui.theme.CategoryFood
import com.personal.lifeOS.ui.theme.CategoryOther
import com.personal.lifeOS.ui.theme.CategorySavings
import com.personal.lifeOS.ui.theme.CategoryShopping
import com.personal.lifeOS.ui.theme.CategorySubscriptions
import com.personal.lifeOS.ui.theme.CategoryTransport
import com.personal.lifeOS.ui.theme.Error

private val EXPENSE_CATEGORIES =
    listOf(
        "Food",
        "Transport",
        "Bills",
        "Shopping",
        "Entertainment",
        "Subscriptions",
        "Savings",
        "Groceries",
        "Airtime",
        "Other",
    )

private val SMS_IMPORT_WINDOWS =
    listOf(
        "Last 24 hours" to 1,
        "Last 7 days" to 7,
        "Last 30 days" to 30,
        "Last 90 days" to 90,
    )

@Composable
internal fun ExpensesHeader(onImportSms: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Expenses", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Track your MPESA transactions",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onImportSms) {
            Text("Import SMS", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun SpendingSummaryRow(summary: SpendingSummary) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(title = "Today", amount = summary.todayTotal, accented = true)
        SummaryCard(title = "This Week", amount = summary.weekTotal, accented = false)
        SummaryCard(title = "This Month", amount = summary.monthTotal, accented = false)
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    accented: Boolean,
) {
    val cardModifier = Modifier.width(160.dp)
    if (accented) {
        AccentGlassCard(modifier = cardModifier) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = DateUtils.formatCurrency(amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    } else {
        GlassCard(modifier = cardModifier) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = DateUtils.formatCurrency(amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun FilterChips(
    selected: TransactionFilter,
    onSelect: (TransactionFilter) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TransactionFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelect(filter) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text =
                        when (filter) {
                            TransactionFilter.ALL -> "All"
                            TransactionFilter.TODAY -> "Today"
                            TransactionFilter.THIS_WEEK -> "This Week"
                            TransactionFilter.THIS_MONTH -> "This Month"
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun CategoryBreakdownSection(
    categories: List<CategoryBreakdown>,
    onCategoryClick: (String?) -> Unit,
) {
    if (categories.isEmpty()) return

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Categories", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            categories.forEach { cat ->
                CategoryRow(
                    category = cat.category,
                    amount = cat.total,
                    percentage = cat.percentage,
                    onClick = { onCategoryClick(cat.category) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: String,
    amount: Double,
    percentage: Float,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(category).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = null,
                tint = getCategoryColor(category),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0f, 1f))
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getCategoryColor(category)),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = DateUtils.formatCurrency(amount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun TransactionsHeader(
    selectedCategory: String?,
    onClearFilter: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Transactions", style = MaterialTheme.typography.titleMedium)
        if (selectedCategory != null) {
            TextButton(onClick = onClearFilter) {
                Text("Clear filter", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun ExpensesLoadingState() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun ExpensesEmptyStateCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No transactions yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "MPESA transactions will appear automatically, or tap + to add manually",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    onRecategorize: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(transaction.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = null,
                    tint = getCategoryColor(transaction.category),
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = getCategoryColor(transaction.category),
                    )
                    Text(
                        text = DateUtils.formatDate(transaction.date, "MMM dd, h:mm a"),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (transaction.mpesaCode != null) {
                    Text(
                        text = transaction.mpesaCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DateUtils.formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (transaction.transactionType == "RECEIVED") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                )
                Row {
                    IconButton(onClick = onRecategorize, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (Double, String, String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Transaction", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (KES)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Description") },
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EXPENSE_CATEGORIES.forEach { cat ->
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (cat == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (cat == category) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount != null && merchant.isNotBlank()) {
                        onAdd(parsedAmount, merchant, category)
                    }
                },
            ) {
                Text("Add", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
internal fun CategoryPickerDialog(
    currentCategory: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Change Category", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EXPENSE_CATEGORIES.forEach { category ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (category == currentCategory) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .clickable { onSelect(category) }
                                .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = getCategoryColor(category),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = category,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
internal fun SmsImportDialog(
    onDismiss: () -> Unit,
    onImportDays: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Import MPESA SMS", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select time period to scan:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                SMS_IMPORT_WINDOWS.forEach { (label, days) ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onImportDays(days) }
                                .padding(16.dp),
                    ) {
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
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

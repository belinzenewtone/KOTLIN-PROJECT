package com.personal.lifeOS.feature.finance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.personal.lifeOS.core.ui.designsystem.AppDesignTokens
import com.personal.lifeOS.ui.theme.CategoryBills
import com.personal.lifeOS.ui.theme.CategoryEntertainment
import com.personal.lifeOS.ui.theme.CategoryFood
import com.personal.lifeOS.ui.theme.CategoryOther
import com.personal.lifeOS.ui.theme.CategorySavings
import com.personal.lifeOS.ui.theme.CategoryShopping
import com.personal.lifeOS.ui.theme.CategorySubscriptions
import com.personal.lifeOS.ui.theme.CategoryTransport

private val FINANCE_CATEGORIES =
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
                    FINANCE_CATEGORIES.forEach { cat ->
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(AppDesignTokens.radius.md))
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

/**
 * Category picker re-implemented as a [ModalBottomSheet].
 *
 * Sheets feel more native than centre dialogs for list selections: they emerge
 * from the action trigger, are easy to dismiss with a swipe, and leave the
 * surrounding context visible — reducing cognitive load.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryPickerBottomSheet(
    currentCategory: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
        ) {
            // Sheet handle label
            Text(
                text = "Change Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            )

            FINANCE_CATEGORIES.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(category) }
                        .background(
                            if (category == currentCategory)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            else Color.Transparent,
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        tint = categoryColor(category),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (category == currentCategory)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * SMS import period picker as a [ModalBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SmsImportBottomSheet(
    onDismiss: () -> Unit,
    onImportDays: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Import M-Pesa SMS",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
            )
            Text(
                text = "Select the time period to scan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            )

            SMS_IMPORT_WINDOWS.forEach { (label, days) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onImportDays(days) }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDesignTokens.radius.sm))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// Keep the legacy AlertDialog names as thin wrappers so any call sites that
// haven't been updated yet still compile.
@Composable
internal fun CategoryPickerDialog(
    currentCategory: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) = CategoryPickerBottomSheet(
    currentCategory = currentCategory,
    onDismiss = onDismiss,
    onSelect = onSelect,
)

@Composable
internal fun SmsImportDialog(
    onDismiss: () -> Unit,
    onImportDays: (Int) -> Unit,
) = SmsImportBottomSheet(
    onDismiss = onDismiss,
    onImportDays = onImportDays,
)

@Composable
internal fun FulizaLimitDialog(
    initialLimitKes: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var limitInput by remember(initialLimitKes) {
        mutableStateOf(initialLimitKes?.toLong()?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Set Fuliza Limit", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "We detected Fuliza activity. Enter your personal Fuliza limit in KES to improve debt tracking accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it.filter(Char::isDigit) },
                    label = { Text("Fuliza limit (KES)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = limitInput.toDoubleOrNull()
                    if (limit != null && limit >= 0.0) onSave(limit)
                },
            ) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

private fun categoryColor(category: String): Color {
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

private fun categoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Outlined.Restaurant
        "transport" -> Icons.Outlined.DirectionsBus
        "bills" -> Icons.Outlined.Receipt
        "shopping" -> Icons.Outlined.ShoppingBag
        "entertainment" -> Icons.Outlined.Subscriptions
        "subscriptions" -> Icons.Outlined.Subscriptions
        "savings" -> Icons.Outlined.Savings
        "groceries" -> Icons.Outlined.LocalGroceryStore
        "airtime" -> Icons.Outlined.PhoneAndroid
        else -> Icons.Outlined.MoreHoriz
    }
}

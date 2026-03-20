package com.personal.lifeOS.feature.finance.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
                FINANCE_CATEGORIES.forEach { category ->
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
                                ).clickable { onSelect(category) }
                                .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            tint = categoryColor(category),
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

package com.personal.lifeOS.features.categorize.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.features.budget.presentation.BUDGET_CATEGORIES
import com.personal.lifeOS.ui.theme.AppSpacing

@Composable
fun CategorizeScreen(
    onBack: (() -> Unit)? = null,
    viewModel: CategorizeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.successMessage, state.error) {
        if (state.successMessage != null || state.error != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessages()
        }
    }

    PageScaffold(
        headerEyebrow = "Finance",
        title = "Categorize",
        subtitle = "Assign a category to uncategorized transactions",
        onBack = onBack,
        contentPadding = PaddingValues(bottom = AppSpacing.BottomSafeWithFloatingNav),
    ) {
        state.error?.let {
            InlineBanner(message = it, tone = InlineBannerTone.ERROR)
        }
        state.successMessage?.let {
            InlineBanner(message = it, tone = InlineBannerTone.SUCCESS)
        }

        if (state.isLoading) {
            LoadingState(label = "Loading uncategorized transactions…")
            return@PageScaffold
        }

        if (state.transactions.isEmpty()) {
            EmptyState(
                title = "All transactions categorized",
                description = "Every transaction has a meaningful category. Great work!",
            )
            return@PageScaffold
        }

        Text(
            text = "${state.transactions.size} transaction${if (state.transactions.size == 1) "" else "s"} need a category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.transactions.forEach { transaction ->
                CategorizeTransactionCard(
                    transaction = transaction,
                    onCategorySelected = { newCategory ->
                        viewModel.assignCategory(transaction, newCategory)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorizeTransactionCard(
    transaction: TransactionEntity,
    onCategorySelected: (String) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }

    AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = DateUtils.formatDate(transaction.date, "MMM dd, h:mm a"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = DateUtils.formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCategory.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                ) {
                    BUDGET_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat.uppercase()
                                dropdownExpanded = false
                                onCategorySelected(cat.uppercase())
                            },
                        )
                    }
                }
            }
        }
    }
}

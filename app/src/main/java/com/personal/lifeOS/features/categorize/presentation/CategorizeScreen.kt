package com.personal.lifeOS.features.categorize.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.lifeOS.core.ui.designsystem.AppCard
import com.personal.lifeOS.core.ui.designsystem.EmptyState
import com.personal.lifeOS.core.ui.designsystem.InlineBanner
import com.personal.lifeOS.core.ui.designsystem.InlineBannerTone
import com.personal.lifeOS.core.ui.designsystem.LoadingState
import com.personal.lifeOS.core.ui.designsystem.PageScaffold
import com.personal.lifeOS.core.utils.DateUtils
import com.personal.lifeOS.ui.theme.AppSpacing

/**
 * Categories available in the categorize wizard.
 *
 * "Other" is intentionally excluded — selecting "Other" stores "OTHER" in the DB which
 * matches the uncategorized filter, meaning the card would never leave the screen.
 * "Miscellaneous" is provided as a last resort (stored as "MISCELLANEOUS", which does NOT
 * match the uncategorized filter, so the card disappears after assignment).
 */
internal val CATEGORIZE_CATEGORIES = listOf(
    "Food", "Transport", "Utilities", "Entertainment", "Shopping",
    "Health", "Education", "Housing", "Airtime", "Savings",
    "Personal Care", "Subscriptions", "Fuliza", "Transfer", "Withdrawal",
    "Miscellaneous",
)

@Composable
fun CategorizeScreen(
    onBack: (() -> Unit)? = null,
    viewModel: CategorizeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.successMessage, state.error) {
        if (state.successMessage != null || state.error != null) {
            kotlinx.coroutines.delay(1500)
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

        if (state.groups.isEmpty()) {
            EmptyState(
                title = "All transactions categorized",
                description = "Every transaction has a meaningful category. Nice work!",
            )
            return@PageScaffold
        }

        val plural = if (state.totalTransactionCount == 1) "transaction needs" else "transactions need"
        Text(
            text = "${state.totalTransactionCount} $plural a category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.groups.forEach { group ->
                // AnimatedVisibility so the card slides out when Room removes it from the list
                // after a batch update — gives visual confirmation that the action worked.
                var visible by remember(group.merchant) { mutableStateOf(true) }
                AnimatedVisibility(
                    visible = visible,
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    MerchantGroupCard(
                        group = group,
                        onCategorySelected = { newCategory ->
                            visible = false          // optimistic hide
                            viewModel.assignCategoryToMerchant(group.merchant, newCategory)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantGroupCard(
    group: MerchantGroup,
    onCategorySelected: (String) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember(group.merchant) { mutableStateOf("") }

    AppCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Header row: merchant + badge + amount ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = group.merchant,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        // Transaction count badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            val label = if (group.transactionCount == 1) "1 transaction"
                                        else "${group.transactionCount} transactions"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Text(
                        text = "Latest: ${DateUtils.formatDate(group.latestDate, "MMM dd, h:mm a")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Total amount (right-aligned)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = DateUtils.formatCurrency(group.totalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (group.transactionCount > 1) {
                        Text(
                            text = "total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Category dropdown ────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
            ) {
                OutlinedTextField(
                    value = if (selectedCategory.isBlank()) ""
                            else selectedCategory.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Pick a category…") },
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
                    CATEGORIZE_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
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

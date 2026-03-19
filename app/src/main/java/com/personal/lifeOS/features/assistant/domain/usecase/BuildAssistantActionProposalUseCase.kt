package com.personal.lifeOS.features.assistant.domain.usecase

import com.google.gson.Gson
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionPreview
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionProposal
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionType
import com.personal.lifeOS.features.assistant.domain.model.CreateTaskActionPayload
import com.personal.lifeOS.features.assistant.domain.model.LogExpenseActionPayload
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildAssistantActionProposalUseCase
    @Inject
    constructor() {
        private val gson = Gson()
        private val expenseAmountRegex = Regex("(?i)(?:ksh|kes|shs?|usd|\\$)?\\s*([0-9]+(?:[.,][0-9]{1,2})?)")

        operator fun invoke(rawMessage: String): AssistantActionProposal? {
            val message = rawMessage.trim()
            if (message.isBlank()) return null

            return parseCreateTask(message) ?: parseLogExpense(message)
        }

        private fun parseCreateTask(message: String): AssistantActionProposal? {
            val lowered = message.lowercase(Locale.getDefault())
            val prefixes =
                listOf(
                    "create task",
                    "add task",
                    "new task",
                    "remind me to",
                    "todo",
                )

            val matchedPrefix = prefixes.firstOrNull { lowered.startsWith(it) } ?: return null
            val title = message.drop(matchedPrefix.length).trim(' ', ':', '-', '.')
            if (title.isBlank()) return null

            val payload =
                CreateTaskActionPayload(
                    title = title,
                    description = "Created from assistant proposal",
                )

            return AssistantActionProposal(
                id = UUID.randomUUID().toString(),
                type = AssistantActionType.CREATE_TASK,
                preview =
                    AssistantActionPreview(
                        title = "Create task",
                        summary = "\"$title\"",
                        riskLabel = "Review required",
                    ),
                payload = gson.toJson(payload),
            )
        }

        private fun parseLogExpense(message: String): AssistantActionProposal? {
            val lowered = message.lowercase(Locale.getDefault())
            val expenseIntent =
                listOf(
                    "log expense",
                    "i spent",
                    "spent",
                    "i paid",
                    "paid",
                ).any { lowered.startsWith(it) || lowered.contains(" $it ") }
            if (!expenseIntent) return null

            val amount = extractAmount(message) ?: return null
            val merchant =
                extractMerchant(message)
                    ?: "Manual entry"

            val payload =
                LogExpenseActionPayload(
                    amount = amount,
                    merchant = merchant,
                    category = "General",
                    date = System.currentTimeMillis(),
                )

            return AssistantActionProposal(
                id = UUID.randomUUID().toString(),
                type = AssistantActionType.LOG_EXPENSE,
                preview =
                    AssistantActionPreview(
                        title = "Log expense",
                        summary = "KES ${"%.2f".format(Locale.getDefault(), amount)} at $merchant",
                        riskLabel = "Review required",
                    ),
                payload = gson.toJson(payload),
            )
        }

        private fun extractAmount(message: String): Double? {
            val match = expenseAmountRegex.find(message) ?: return null
            val normalized = match.groupValues[1].replace(",", "")
            return normalized.toDoubleOrNull()
        }

        private fun extractMerchant(message: String): String? {
            val merchantRegex = Regex("(?i)\\b(?:at|to)\\s+([A-Za-z0-9&'\\-\\s]{2,40})")
            val raw = merchantRegex.find(message)?.groupValues?.getOrNull(1)?.trim() ?: return null
            return raw.trimEnd('.', ',', '!')
        }
    }

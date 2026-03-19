package com.personal.lifeOS.features.assistant.data.repository

import com.google.gson.Gson
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionCommitResult
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionProposal
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionType
import com.personal.lifeOS.features.assistant.domain.model.CreateTaskActionPayload
import com.personal.lifeOS.features.assistant.domain.model.LogExpenseActionPayload
import com.personal.lifeOS.features.assistant.domain.repository.AssistantActionExecutor
import com.personal.lifeOS.features.expenses.domain.model.Transaction
import com.personal.lifeOS.features.expenses.domain.repository.ExpenseRepository
import com.personal.lifeOS.features.tasks.domain.model.Task
import com.personal.lifeOS.features.tasks.domain.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAssistantActionExecutor
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
        private val expenseRepository: ExpenseRepository,
    ) : AssistantActionExecutor {
        private val gson = Gson()

        override suspend fun commit(proposal: AssistantActionProposal): AssistantActionCommitResult {
            return runCatching {
                when (proposal.type) {
                    AssistantActionType.CREATE_TASK -> commitCreateTask(proposal.payload)
                    AssistantActionType.LOG_EXPENSE -> commitLogExpense(proposal.payload)
                    else -> error("Action commit is not yet wired for ${proposal.type}.")
                }
            }.fold(
                onSuccess = { AssistantActionCommitResult.Success },
                onFailure = { error ->
                    AssistantActionCommitResult.Error(
                        message = error.message ?: "Failed to commit assistant action.",
                    )
                },
            )
        }

        private suspend fun commitCreateTask(payload: String) {
            val parsed =
                gson.fromJson(payload, CreateTaskActionPayload::class.java)
                    ?: error("Invalid task payload.")
            if (parsed.title.isBlank()) error("Task title cannot be blank.")

            taskRepository.addTask(
                Task(
                    title = parsed.title.trim(),
                    description = parsed.description.trim(),
                    deadline = parsed.dueAt,
                ),
            )
        }

        private suspend fun commitLogExpense(payload: String) {
            val parsed =
                gson.fromJson(payload, LogExpenseActionPayload::class.java)
                    ?: error("Invalid expense payload.")
            if (parsed.amount <= 0.0) error("Expense amount must be greater than zero.")
            if (parsed.merchant.isBlank()) error("Merchant cannot be blank.")

            expenseRepository.addTransaction(
                Transaction(
                    amount = parsed.amount,
                    merchant = parsed.merchant.trim(),
                    category = parsed.category.trim().ifBlank { "General" },
                    date = parsed.date,
                    source = "assistant",
                    transactionType = "SENT",
                ),
            )
        }
    }

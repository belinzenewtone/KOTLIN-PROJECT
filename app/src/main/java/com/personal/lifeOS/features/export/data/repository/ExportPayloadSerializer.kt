package com.personal.lifeOS.features.export.data.repository

import com.google.gson.GsonBuilder
import com.personal.lifeOS.features.export.domain.model.ExportDomain
import com.personal.lifeOS.features.export.domain.model.ExportFormat
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets.UTF_8

@Singleton
class ExportPayloadSerializer
    @Inject
    constructor() {
        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .create()

        internal fun serialize(
            request: ExportRequest,
            bundle: ExportBundle,
            userId: String,
            exportedAt: Long,
        ): ByteArray {
            return when (request.format) {
                ExportFormat.JSON -> gson.toJson(bundle.toJsonPayload(userId, exportedAt)).toByteArray(UTF_8)
                ExportFormat.CSV -> csvText(bundle, request.domain).toByteArray(UTF_8)
            }
        }

        fun mimeType(
            format: ExportFormat,
            encrypted: Boolean,
        ): String {
            if (encrypted) return "application/octet-stream"
            return when (format) {
                ExportFormat.JSON -> "application/json"
                ExportFormat.CSV -> "text/csv"
            }
        }

        private fun csvText(
            bundle: ExportBundle,
            domain: ExportDomain,
        ): String {
            require(domain != ExportDomain.ALL) { "CSV export requires a specific domain." }
            val (header, rows) = csvColumnsAndRows(bundle, domain)
            return csvOf(header, rows)
        }

        private fun csvColumnsAndRows(
            bundle: ExportBundle,
            domain: ExportDomain,
        ): Pair<List<String>, List<List<String>>> {
            return when (domain) {
                ExportDomain.TRANSACTIONS -> transactionsRows(bundle)
                ExportDomain.TASKS -> tasksRows(bundle)
                ExportDomain.EVENTS -> eventsRows(bundle)
                ExportDomain.BUDGETS -> budgetsRows(bundle)
                ExportDomain.INCOMES -> incomesRows(bundle)
                ExportDomain.RECURRING_RULES -> recurringRows(bundle)
                ExportDomain.MERCHANT_RULES -> merchantRows(bundle)
                ExportDomain.ALL -> error("ALL should be validated before CSV serialization.")
            }
        }

        private fun csvOf(
            header: List<String>,
            rows: List<List<String>>,
        ): String {
            val builder = StringBuilder()
            builder.appendLine(header.joinToString(",") { it.escapeCsv() })
            rows.forEach { row ->
                builder.appendLine(row.joinToString(",") { it.escapeCsv() })
            }
            return builder.toString()
        }

        private fun String.escapeCsv(): String {
            val requiresQuoting =
                contains(',') ||
                    contains('"') ||
                    contains('\n') ||
                    contains('\r')
            if (!requiresQuoting) return this
            return "\"" + replace("\"", "\"\"") + "\""
        }
    }

internal fun ExportBundle.toJsonPayload(
    userId: String,
    exportedAt: Long,
): Map<String, Any> {
    return mapOf(
        "exported_at" to exportedAt,
        "exported_at_iso" to Instant.ofEpochMilli(exportedAt).toString(),
        "user_id" to userId,
        "transactions" to transactions,
        "tasks" to tasks,
        "events" to events,
        "budgets" to budgets,
        "incomes" to incomes,
        "recurring_rules" to recurringRules,
        "merchant_rules" to merchantRules,
    )
}

private fun transactionsRows(bundle: ExportBundle): Pair<List<String>, List<List<String>>> {
    val header =
        listOf(
            "id",
            "amount",
            "merchant",
            "category",
            "date",
            "source",
            "transaction_type",
            "mpesa_code",
        )
    val rows =
        bundle.transactions.map { tx ->
            listOf(
                tx.id.toString(),
                tx.amount.toString(),
                tx.merchant,
                tx.category,
                tx.date.toString(),
                tx.source,
                tx.transactionType,
                tx.mpesaCode.orEmpty(),
            )
        }
    return header to rows
}

private fun tasksRows(bundle: ExportBundle): Pair<List<String>, List<List<String>>> {
    val header = listOf("id", "title", "description", "priority", "deadline", "status", "created_at")
    val rows =
        bundle.tasks.map { task ->
            listOf(
                task.id.toString(),
                task.title,
                task.description,
                task.priority,
                task.deadline?.toString().orEmpty(),
                task.status,
                task.createdAt.toString(),
            )
        }
    return header to rows
}

private fun eventsRows(bundle: ExportBundle): Pair<List<String>, List<List<String>>> {
    val header = listOf("id", "title", "description", "date", "end_date", "type", "status", "importance")
    val rows =
        bundle.events.map { event ->
            listOf(
                event.id.toString(),
                event.title,
                event.description,
                event.date.toString(),
                event.endDate?.toString().orEmpty(),
                event.type,
                event.status,
                event.importance,
            )
        }
    return header to rows
}

private fun budgetsRows(bundle: ExportBundle): Pair<List<String>, List<List<String>>> {
    val header = listOf("id", "category", "limit_amount", "period", "created_at")
    val rows =
        bundle.budgets.map { budget ->
            listOf(
                budget.id.toString(),
                budget.category,
                budget.limitAmount.toString(),
                budget.period,
                budget.createdAt.toString(),
            )
        }
    return header to rows
}

private fun incomesRows(bundle: ExportBundle): Pair<List<String>, List<List<String>>> {
    val header = listOf("id", "amount", "source", "date", "note", "is_recurring")
    val rows =
        bundle.incomes.map { income ->
            listOf(
                income.id.toString(),
                income.amount.toString(),
                income.source,
                income.date.toString(),
                income.note,
                income.isRecurring.toString(),
            )
        }
    return header to rows
}

private fun recurringRows(bundle: ExportBundle): Pair<List<String>, List<List<String>>> {
    val header = listOf("id", "title", "type", "cadence", "next_run_at", "amount", "enabled")
    val rows =
        bundle.recurringRules.map { rule ->
            listOf(
                rule.id.toString(),
                rule.title,
                rule.type,
                rule.cadence,
                rule.nextRunAt.toString(),
                rule.amount?.toString().orEmpty(),
                rule.enabled.toString(),
            )
        }
    return header to rows
}

private fun merchantRows(bundle: ExportBundle): Pair<List<String>, List<List<String>>> {
    val header = listOf("id", "merchant", "category", "confidence", "user_corrected", "updated_at")
    val rows =
        bundle.merchantRules.map { rule ->
            listOf(
                rule.id.toString(),
                rule.merchant,
                rule.category,
                rule.confidence.toString(),
                rule.userCorrected.toString(),
                rule.updatedAt.toString(),
            )
        }
    return header to rows
}

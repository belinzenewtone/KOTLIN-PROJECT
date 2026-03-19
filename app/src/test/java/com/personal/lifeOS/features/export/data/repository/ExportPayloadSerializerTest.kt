package com.personal.lifeOS.features.export.data.repository

import com.personal.lifeOS.core.database.entity.TaskEntity
import com.personal.lifeOS.core.database.entity.TransactionEntity
import com.personal.lifeOS.features.export.domain.model.ExportDomain
import com.personal.lifeOS.features.export.domain.model.ExportFormat
import com.personal.lifeOS.features.export.domain.model.ExportRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPayloadSerializerTest {
    private val serializer = ExportPayloadSerializer()

    @Test
    fun `serialize JSON includes export metadata and payload keys`() {
        val request = ExportRequest(format = ExportFormat.JSON, domain = ExportDomain.ALL)
        val bundle =
            ExportBundle(
                transactions =
                    listOf(
                        TransactionEntity(
                            id = 11L,
                            amount = 1200.0,
                            merchant = "NAIVAS",
                            category = "Groceries",
                            date = 1_700_000_000_000L,
                            userId = "user-1",
                        ),
                    ),
                tasks =
                    listOf(
                        TaskEntity(
                            id = 21L,
                            title = "Review monthly spending",
                            userId = "user-1",
                        ),
                    ),
            )

        val bytes = serializer.serialize(request, bundle, userId = "user-1", exportedAt = 1_700_000_100_000L)
        val json = bytes.toString(Charsets.UTF_8)

        assertTrue(json.contains("\"user_id\": \"user-1\""))
        assertTrue(json.contains("\"exported_at\": 1700000100000"))
        assertTrue(json.contains("\"transactions\""))
        assertTrue(json.contains("\"tasks\""))
    }

    @Test
    fun `serialize CSV escapes commas and quotes safely`() {
        val request = ExportRequest(format = ExportFormat.CSV, domain = ExportDomain.TRANSACTIONS)
        val bundle =
            ExportBundle(
                transactions =
                    listOf(
                        TransactionEntity(
                            id = 31L,
                            amount = 250.0,
                            merchant = "Jane, \"Shop\"",
                            category = "Food",
                            date = 1_700_000_200_000L,
                            userId = "user-1",
                        ),
                    ),
            )

        val bytes = serializer.serialize(request, bundle, userId = "user-1", exportedAt = 1_700_000_300_000L)
        val csv = bytes.toString(Charsets.UTF_8)

        assertTrue(csv.contains("id,amount,merchant,category,date,source,transaction_type,mpesa_code"))
        assertTrue(csv.contains("\"Jane, \"\"Shop\"\"\""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `serialize CSV fails when domain is ALL`() {
        val request = ExportRequest(format = ExportFormat.CSV, domain = ExportDomain.ALL)
        serializer.serialize(
            request = request,
            bundle = ExportBundle(),
            userId = "user-1",
            exportedAt = 1_700_000_300_000L,
        )
    }

    @Test
    fun `mime type switches to octet stream when encrypted`() {
        assertEquals("application/json", serializer.mimeType(ExportFormat.JSON, encrypted = false))
        assertEquals("text/csv", serializer.mimeType(ExportFormat.CSV, encrypted = false))
        assertEquals("application/octet-stream", serializer.mimeType(ExportFormat.JSON, encrypted = true))
    }
}

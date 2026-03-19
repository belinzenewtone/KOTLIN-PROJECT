package com.personal.lifeOS.features.assistant.domain.usecase

import com.google.gson.Gson
import com.personal.lifeOS.features.assistant.domain.model.AssistantActionType
import com.personal.lifeOS.features.assistant.domain.model.CreateTaskActionPayload
import com.personal.lifeOS.features.assistant.domain.model.LogExpenseActionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BuildAssistantActionProposalUseCaseTest {
    private val useCase = BuildAssistantActionProposalUseCase()
    private val gson = Gson()

    @Test
    fun `builds create task proposal from remind me phrasing`() {
        val proposal = useCase("remind me to submit tax returns tomorrow")

        assertNotNull(proposal)
        assertEquals(AssistantActionType.CREATE_TASK, proposal!!.type)
        val payload = gson.fromJson(proposal.payload, CreateTaskActionPayload::class.java)
        assertEquals("submit tax returns tomorrow", payload.title)
    }

    @Test
    fun `builds expense proposal when amount and merchant are present`() {
        val proposal = useCase("I spent KES 1200 at Carrefour")

        assertNotNull(proposal)
        assertEquals(AssistantActionType.LOG_EXPENSE, proposal!!.type)
        val payload = gson.fromJson(proposal.payload, LogExpenseActionPayload::class.java)
        assertEquals(1200.0, payload.amount, 0.001)
        assertEquals("Carrefour", payload.merchant)
    }

    @Test
    fun `returns null for regular informational prompt`() {
        val proposal = useCase("How much did I spend this week?")

        assertNull(proposal)
    }
}

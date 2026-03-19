package com.personal.lifeOS.platform.sms.ingestion

interface MpesaIngestionPipeline {
    suspend fun ingestRealtime(rawMessage: String): Boolean
}

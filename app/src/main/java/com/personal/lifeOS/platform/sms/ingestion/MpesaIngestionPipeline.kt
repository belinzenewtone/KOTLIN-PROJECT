package com.personal.lifeOS.platform.sms.ingestion

enum class MpesaIngestionOutcome {
    IMPORTED,
    DUPLICATE,
    PARSE_FAILED,
    CANDIDATE_PENDING,
    IGNORED_IRRELEVANT,
}

enum class MpesaIngestionSource {
    REALTIME,
    BACKFILL,
}

interface MpesaIngestionPipeline {
    suspend fun ingestRealtime(
        rawMessage: String,
        source: MpesaIngestionSource = MpesaIngestionSource.REALTIME,
    ): MpesaIngestionOutcome
}

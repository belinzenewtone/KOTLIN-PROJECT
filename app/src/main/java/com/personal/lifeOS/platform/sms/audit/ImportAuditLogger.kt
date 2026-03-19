package com.personal.lifeOS.platform.sms.audit

import com.personal.lifeOS.core.database.dao.ImportAuditDao
import com.personal.lifeOS.core.database.entity.ImportAuditEntity
import com.personal.lifeOS.core.security.AuthSessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportAuditLogger
    @Inject
    constructor(
        private val importAuditDao: ImportAuditDao,
        private val authSessionStore: AuthSessionStore,
    ) {
        suspend fun log(
            outcome: String,
            rawMessage: String,
            mpesaCode: String? = null,
            amount: Double? = null,
            merchant: String? = null,
            failureReason: String? = null,
        ) {
            val userId = authSessionStore.getUserId()
            if (userId.isBlank()) return
            importAuditDao.insert(
                ImportAuditEntity(
                    id = System.currentTimeMillis(),
                    userId = userId,
                    rawMessage = rawMessage,
                    mpesaCode = mpesaCode,
                    amount = amount,
                    merchant = merchant,
                    outcome = outcome,
                    failureReason = failureReason,
                ),
            )
        }
    }

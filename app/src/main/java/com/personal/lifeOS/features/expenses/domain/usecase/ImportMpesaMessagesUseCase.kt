package com.personal.lifeOS.features.expenses.domain.usecase

import com.personal.lifeOS.platform.sms.background.MpesaHistoricalImportScanner
import com.personal.lifeOS.platform.sms.background.MpesaHistoricalImportSummary
import javax.inject.Inject

class ImportMpesaMessagesUseCase
    @Inject
    constructor(
        private val scanner: MpesaHistoricalImportScanner,
    ) {
        suspend operator fun invoke(daysBack: Int): MpesaHistoricalImportSummary {
            return scanner.scan(daysBack)
        }
    }


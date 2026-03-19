package com.personal.lifeOS.platform.sms.parser

import com.personal.lifeOS.features.expenses.data.parser.MpesaSmsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MpesaMessageParser
    @Inject
    constructor() {
        fun isMpesaMessage(message: String): Boolean = MpesaSmsParser.isMpesaSms(message)

        fun parse(message: String): MpesaSmsParser.ParsedTransaction? = MpesaSmsParser.parse(message)
    }

package com.personal.lifeOS.platform.sms.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsPermissionGateway
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun hasReadSms(): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS,
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun hasReceiveSms(): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

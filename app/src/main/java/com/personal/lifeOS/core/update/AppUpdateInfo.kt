package com.personal.lifeOS.core.update

data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String?,
    val required: Boolean,
    val downloadUrl: String?,
    val checksumSha256: String?,
    val checkedAt: Long,
)

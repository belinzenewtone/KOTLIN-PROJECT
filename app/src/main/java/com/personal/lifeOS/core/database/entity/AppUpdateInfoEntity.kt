package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "app_update_info",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["checked_at"]),
        Index(value = ["version_code"]),
    ],
)
data class AppUpdateInfoEntity(
    val id: Long = 0L,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    @ColumnInfo(name = "version_code")
    val versionCode: Long,
    @ColumnInfo(name = "version_name")
    val versionName: String? = null,
    @ColumnInfo(name = "is_required")
    val isRequired: Boolean = false,
    @ColumnInfo(name = "download_url")
    val downloadUrl: String? = null,
    @ColumnInfo(name = "checksum_sha256")
    val checksumSha256: String? = null,
    @ColumnInfo(name = "checked_at")
    val checkedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)

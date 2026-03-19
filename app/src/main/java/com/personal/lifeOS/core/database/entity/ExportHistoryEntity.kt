package com.personal.lifeOS.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "export_history",
    primaryKeys = ["user_id", "id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["exported_at"]),
        Index(value = ["status"]),
        Index(value = ["format"]),
        Index(value = ["domain_scope"]),
    ],
)
data class ExportHistoryEntity(
    val id: Long,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val format: String,
    @ColumnInfo(name = "domain_scope")
    val domainScope: String,
    @ColumnInfo(name = "date_from")
    val dateFrom: Long? = null,
    @ColumnInfo(name = "date_to")
    val dateTo: Long? = null,
    @ColumnInfo(name = "file_path")
    val filePath: String? = null,
    @ColumnInfo(name = "item_count")
    val itemCount: Int = 0,
    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean = false,
    val status: String,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "exported_at")
    val exportedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)

package com.exam.stateownedenterprise.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks which seed JSON files (by file name) have already been imported, and the
 * SHA-256 hash of their content at import time. Lets [com.exam.stateownedenterprise.data.seed.SeedImporter]
 * skip re-processing a file whose content hasn't changed, while still picking up
 * new or edited seed files on the next launch.
 */
@Entity(tableName = "import_meta")
data class ImportMeta(
    @PrimaryKey
    val fileName: String,
    val contentHash: String,
    val importedAt: Long
)

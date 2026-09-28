package com.exam.stateownedenterprise.data.seed

import java.security.MessageDigest

/**
 * Deterministic content fingerprint used by [SeedImporter] to detect whether a seed
 * file's content changed since it was last imported.
 */
internal fun sha256Hex(content: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

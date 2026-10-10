package app.paisapilot.data.local.entity

import java.util.UUID

/**
 * Every row that syncs to the cloud carries these fields.
 * - [updatedAt] decides the winner when two devices edited the same row (last write wins).
 * - [deleted] is a tombstone, so a delete on one device reaches the others.
 * - [dirty] marks local changes that have not been uploaded yet.
 */
interface Syncable {
    val id: String
    val updatedAt: Long
    val deleted: Boolean
    val dirty: Boolean
}

fun newId(): String = UUID.randomUUID().toString()

fun now(): Long = System.currentTimeMillis()

package com.veleda.cyclewise.domain.models

import kotlinx.serialization.Serializable

/**
 * Metadata embedded in a `.rwbackup` archive as `metadata.json`.
 *
 * Captures the app version, database schema version, and export timestamp so
 * that the import flow can detect incompatibilities (e.g. a backup created with
 * a newer schema than the current app supports) and display meaningful context
 * in the preview dialog.
 *
 * Annotated with [@Serializable] because it is serialized to/from JSON inside
 * the backup ZIP archive via `kotlinx.serialization`.
 *
 * @property appVersionName  Human-readable version string (e.g. `"1.2.0"`).
 * @property appVersionCode  Numeric version code from the build (e.g. `12`).
 * @property schemaVersion   Room database schema version at export time.
 * @property exportDateUtc   ISO-8601 timestamp of the export (e.g. `"2026-03-19T14:30:22Z"`).
 */
@Serializable
data class BackupMetadata(
    val appVersionName: String,
    val appVersionCode: Int,
    val schemaVersion: Int,
    val exportDateUtc: String,
)

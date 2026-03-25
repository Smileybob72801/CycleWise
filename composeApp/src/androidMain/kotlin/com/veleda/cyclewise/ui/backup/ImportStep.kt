package com.veleda.cyclewise.ui.backup

/**
 * Steps in the multi-dialog import flow, shared across Settings and Passphrase screens.
 *
 * The flow progresses linearly: [IDLE] → [METADATA_PREVIEW] → [PASSPHRASE_ENTRY] →
 * (optionally [FIRST_WARNING] → [SECOND_CONFIRM]) → [IMPORTING].
 *
 * The double confirmation steps ([FIRST_WARNING] and [SECOND_CONFIRM]) are skipped
 * on the first-time setup screen where no existing data is present.
 */
enum class ImportStep {
    /** No import in progress. */
    IDLE,

    /** Showing the backup metadata preview dialog. */
    METADATA_PREVIEW,

    /** Prompting for the backup's encryption passphrase. */
    PASSPHRASE_ENTRY,

    /** First warning: "This will replace all existing data." */
    FIRST_WARNING,

    /** Second confirmation: user must type "OVERWRITE". */
    SECOND_CONFIRM,

    /** Import is actively running (non-dismissable progress dialog). */
    IMPORTING,
}

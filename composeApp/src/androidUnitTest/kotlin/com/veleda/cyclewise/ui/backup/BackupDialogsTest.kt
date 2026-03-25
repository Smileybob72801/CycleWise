package com.veleda.cyclewise.ui.backup

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.domain.models.BackupMetadata
import com.veleda.cyclewise.ui.theme.Dimensions
import com.veleda.cyclewise.ui.theme.LocalDimensions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric-based Compose UI tests for the backup dialog composables
 * in [BackupDialogs].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class BackupDialogsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testMetadata = BackupMetadata(
        appVersionName = "1.0.0",
        appVersionCode = 2,
        schemaVersion = 13,
        exportDateUtc = "2026-03-19T14:30:22Z",
    )

    // region MetadataPreviewDialog

    @Test
    fun metadataPreviewDialog_WHEN_rendered_THEN_displaysAllFields() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupMetadataPreviewDialog(
                        metadata = testMetadata,
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // Then
        composeTestRule.onNodeWithText("Backup Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("App Version: 1.0.0", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Schema Version: 13", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Export Date: 2026-03-19T14:30:22Z", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    // endregion

    // region OverwriteConfirmDialog

    @Test
    fun overwriteConfirmDialog_WHEN_textEmpty_THEN_confirmDisabled() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupOverwriteConfirmDialog(
                        confirmText = "",
                        onTextChanged = {},
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // Then
        composeTestRule.onNode(
            hasText("Replace Data", substring = true, ignoreCase = true).and(hasClickAction()),
        ).assertIsNotEnabled()
    }

    @Test
    fun overwriteConfirmDialog_WHEN_overwriteTyped_THEN_confirmEnabled() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupOverwriteConfirmDialog(
                        confirmText = "OVERWRITE",
                        onTextChanged = {},
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // Then
        composeTestRule.onNode(
            hasText("Replace Data", substring = true, ignoreCase = true).and(hasClickAction()),
        ).assertIsEnabled()
    }

    @Test
    fun overwriteConfirmDialog_WHEN_partialText_THEN_confirmDisabled() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupOverwriteConfirmDialog(
                        confirmText = "OVER",
                        onTextChanged = {},
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // Then
        composeTestRule.onNode(
            hasText("Replace Data", substring = true, ignoreCase = true).and(hasClickAction()),
        ).assertIsNotEnabled()
    }

    // endregion

    // region PassphraseDialog

    @Test
    fun passphraseDialog_WHEN_errorProvided_THEN_showsErrorText() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupPassphraseDialog(
                        error = "Incorrect passphrase for this backup",
                        isVerifying = false,
                        onVerify = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // Then
        composeTestRule.onNodeWithText("Enter Backup Passphrase").assertIsDisplayed()
        composeTestRule.onNodeWithText("Incorrect passphrase for this backup").assertIsDisplayed()
    }

    @Test
    fun passphraseDialog_WHEN_noError_THEN_errorTextNotDisplayed() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupPassphraseDialog(
                        error = null,
                        isVerifying = false,
                        onVerify = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // Then
        composeTestRule.onNodeWithText("Enter Backup Passphrase").assertIsDisplayed()
        composeTestRule.onNodeWithText("Incorrect passphrase for this backup").assertDoesNotExist()
    }

    @Test
    fun passphraseDialog_WHEN_isVerifying_THEN_verifyButtonDisabled() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupPassphraseDialog(
                        error = null,
                        isVerifying = true,
                        onVerify = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // Then
        composeTestRule.onNodeWithText("Verifying passphrase", substring = true).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Verify").and(hasClickAction()),
        ).assertIsNotEnabled()
    }

    @Test
    fun passphraseDialog_WHEN_passphraseEnteredAndNotVerifying_THEN_verifyButtonEnabled() {
        // Given
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupPassphraseDialog(
                        error = null,
                        isVerifying = false,
                        onVerify = {},
                        onDismiss = {},
                    )
                }
            }
        }

        // When
        composeTestRule.onNodeWithTag("backup-passphrase-input").performTextInput("mypassphrase")

        // Then
        composeTestRule.onNode(
            hasText("Verify").and(hasClickAction()),
        ).assertIsEnabled()
    }

    // endregion

    // region ProgressDialog

    @Test
    fun progressDialog_WHEN_rendered_THEN_showsTitleAndBody() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupProgressDialog(
                        title = "Exporting Backup",
                        body = "Creating backup archive...",
                    )
                }
            }
        }

        // Then
        composeTestRule.onNodeWithText("Exporting Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Creating backup archive...").assertIsDisplayed()
    }

    @Test
    fun progressDialog_WHEN_rendered_THEN_hasNoButtons() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDimensions provides Dimensions()) {
                MaterialTheme {
                    BackupProgressDialog(
                        title = "Importing Backup",
                        body = "Restoring data...",
                    )
                }
            }
        }

        // Then — no confirm or cancel buttons exist
        composeTestRule.onNodeWithText("Importing Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
        composeTestRule.onNodeWithText("OK").assertDoesNotExist()
        composeTestRule.onNodeWithText("Close").assertDoesNotExist()
    }

    // endregion
}

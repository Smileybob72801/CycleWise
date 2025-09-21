package com.veleda.cyclewise.e2e

import android.content.Context
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
//import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.printToString
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.veleda.cyclewise.MainActivity
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import java.io.File

@RunWith(AndroidJUnit4::class)
class UnlockCreateLogE2ETest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val testPassphrase = "E2E_TEST_PASSPHRASE"

    private val shortWait = 2.seconds.toJavaDuration().toMillis()
    private val mediumWait = 5.seconds.toJavaDuration().toMillis()
    private val longWait = 8.seconds.toJavaDuration().toMillis()

    @Before
    fun setUp() {
        // Get the target app's context to access its storage.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // 1. Wipe Room Databases (and their journal/write-ahead-log files)
        val testDbName = "e2e_cyclewise.db"
        val prodDbName = "cyclewise.db"
        listOf(
            testDbName, "$testDbName-shm", "$testDbName-wal",
            prodDbName, "$prodDbName-shm", "$prodDbName-wal"
        ).forEach { appContext.getDatabasePath(it).delete() }

        // 2. Clear all SharedPreferences
        appContext.getSharedPreferences("cyclewise_salt_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        // 3. Clear all Jetpack DataStore files
        val dataStoreDir = File(appContext.filesDir, "datastore")
        if (dataStoreDir.exists()) {
            dataStoreDir.deleteRecursively()
        }
    }

    @Test
    fun unlock_createCycle_logSymptom_showsCorrectlyOnCalendar() {
        // 1) Unlock
        typeInto("passphrase-input", testPassphrase)
        click("unlock-button")
        waitForExists("calendar-root", timeoutMillis = longWait)

        // 2) Create a cycle today
        val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayTag = "day-$today"

        click(todayTag)
        waitForExists("save-cycle-button", timeoutMillis = mediumWait)
        click("save-cycle-button")

        waitForExists("end-cycle-button", timeoutMillis = longWait)
        assertExists("period-day-$today", useUnmerged = true)

        // 3) Open Daily Log screen for today
        click(todayTag)
        waitForExists("edit-log-button", timeoutMillis = mediumWait)
        click("edit-log-button")

        // 4) Create a new symptom
        val symptomName = "TestSymptom"
        typeInto("create-symptom-textbox", symptomName)

        compose.onNodeWithTag("create-symptom-textbox").assert(
            SemanticsMatcher("Editable text is '$symptomName'") { node ->
                node.config.getOrNull(SemanticsProperties.EditableText)?.text == symptomName
            }
        )

        // Print the default MERGED tree
        compose.onRoot().printToLog("MergedTree")

        // Print the UNMERGED tree to see all nodes
        compose.onRoot(useUnmergedTree = true).printToLog("UnmergedTree")

        Log.d("E2E_DEBUG", compose.onRoot().printToString())
        Log.d("E2E_DEBUG", compose.onRoot(useUnmergedTree = true).printToString())

        compose.onNodeWithTag("create-symptom-textbox", useUnmergedTree = true).performImeAction()

        // 5) VERIFY THE FINAL RESULT: Wait for the new chip to appear and assert it's selected.
        val chipTag = "chip-${symptomName.uppercase()}"
        waitForExists(chipTag, useUnmerged = true, timeoutMillis = longWait)
        compose.onNodeWithTag(chipTag, useUnmergedTree = true).assertIsSelected()

        // 6) Save the log and verify the calendar dot
        click("save_log_button")
        waitForExists("calendar-root", timeoutMillis = longWait)

        val dotTag = "symptom-dot-$today"
        waitForExists(dotTag, useUnmerged = true, timeoutMillis = longWait)
        compose.onNodeWithTag(dotTag, useUnmergedTree = true).assertIsDisplayed()
    }

    // ---------- Helpers ----------

    private fun click(tag: String, useUnmerged: Boolean = false) {
        waitForExists(tag, useUnmerged, mediumWait)
        compose.onNodeWithTag(tag, useUnmergedTree = useUnmerged).performClick()
    }

    private fun typeInto(tag: String, text: String, useUnmerged: Boolean = false) {
        waitForExists(tag, useUnmerged, mediumWait)
        compose.onNodeWithTag(tag, useUnmergedTree = useUnmerged).performTextInput(text)
    }

    private fun assertExists(tag: String, useUnmerged: Boolean = false) {
        waitForExists(tag, useUnmerged, mediumWait)
        compose.onNodeWithTag(tag, useUnmergedTree = useUnmerged).assertExists()
    }

    private fun waitForExists(
        tag: String,
        useUnmerged: Boolean = false,
        timeoutMillis: Long = mediumWait
    ) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = useUnmerged)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilTextboxCleared(
        tag: String,
        useUnmerged: Boolean = false,
        timeoutMillis: Long = mediumWait
    ) {
        val editableIsEmpty = SemanticsMatcher("${tag}_isEmptyEditable") { node ->
            val text = node.config.getOrNull(SemanticsProperties.EditableText)
            text?.text?.isEmpty() == true
        }
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodes(
                matcher = editableIsEmpty.and(hasTestTag(tag)),
                useUnmergedTree = useUnmerged
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

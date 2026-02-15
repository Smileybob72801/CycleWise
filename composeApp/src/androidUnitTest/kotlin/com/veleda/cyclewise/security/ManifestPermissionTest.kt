package com.veleda.cyclewise.security

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse

/**
 * Verifies that the merged AndroidManifest does not declare the INTERNET permission.
 *
 * RhythmWise is a privacy-first, offline-only app — no network access is permitted.
 * This test guards against accidental re-introduction of the permission via manifest
 * merging or dependency changes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class ManifestPermissionTest {

    @Test
    fun mergedManifest_WHEN_checked_THEN_doesNotDeclareInternetPermission() {
        // GIVEN the app's merged manifest loaded by Robolectric
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )

        // WHEN we inspect the requested permissions
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        // THEN INTERNET is not among them
        assertFalse(
            permissions.contains("android.permission.INTERNET"),
            "INTERNET permission must not be declared — the app is offline-only"
        )
    }
}

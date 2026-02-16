package com.veleda.cyclewise.di

import android.content.Context
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.domain.services.PassphraseService
import org.koin.dsl.module
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * A deterministic, fast, and safe PassphraseService for use in E2E tests.
 * It uses a standard SHA-256 hash to generate a correctly-sized 32-byte key.
 * This isolates UI tests from the complexities of Argon2 and persisted salts.
 */
class TestPassphraseService : PassphraseService {
    override fun deriveKey(passphrase: String): ByteArray {
        // Use a standard hash function to always produce a 32-byte key.
        // This is deterministic and robust for any passphrase length.
        return MessageDigest.getInstance("SHA-256")
            .digest(passphrase.toByteArray(StandardCharsets.UTF_8))
    }
}

/**
 * A Koin module containing all the necessary overrides for a hermetic E2E test environment.
 * Note: This module does NOT use `override = true`. The override capability is
 * enabled globally in the CustomTestRunner.
 */
val testOverridesModule = module {
    // 1. Replace the production PassphraseService with our deterministic test version.
    single<PassphraseService> { TestPassphraseService() }

    // 2. Replace the session-scoped PeriodDatabase provider.
    //    Mirrors the production createDatabaseAndZeroizeKey() pattern: pass a copy of the
    //    key to SupportFactory so the original can be zeroed immediately. The ViewModel's
    //    db.openHelper.writableDatabase call later forces SQLCipher to consume the real key.
    scope(SESSION_SCOPE) {
        scoped<PeriodDatabase> { (passphrase: String) ->
            val context: Context = get()
            val testDbName = "e2e_cyclewise.db"

            // The key is derived by our deterministic TestPassphraseService above.
            val key = get<PassphraseService>().deriveKey(passphrase)

            try {
                PeriodDatabase.create(
                    context = context,
                    passphrase = key.copyOf(),
                    dbName = testDbName
                )
            } finally {
                key.fill(0)
            }
        }
    }
}
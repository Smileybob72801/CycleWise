package com.veleda.cyclewise.ui.auth

import android.util.Log
import com.veleda.cyclewise.androidData.local.draft.LockedWaterDraft
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.repository.PeriodRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Syncs water intake drafts from [LockedWaterDraft] into the encrypted database after unlock.
 *
 * **Sync rules:**
 * - Today's draft is skipped (the user may still be editing it).
 * - A draft is written to the DB only if its cup count exceeds the existing DB value for that date.
 * - Successfully synced dates are cleared from the draft store.
 * - Individual date failures are logged but do not abort the remaining sync.
 */
class WaterDraftSyncer(
    private val lockedWaterDraft: LockedWaterDraft,
    private val repository: PeriodRepository
) {
    suspend fun sync(today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())) {
        val allDrafts = lockedWaterDraft.readAll()
        val drafts = allDrafts.filterKeys { it != today }
        if (drafts.isEmpty()) return

        val existing = repository.getWaterIntakeForDates(drafts.keys.toList())
            .associateBy { it.date }
        val now = Clock.System.now()
        val syncedDates = mutableSetOf<LocalDate>()

        for ((date, draftCups) in drafts) {
            try {
                val dbEntry = existing[date]
                val dbCups = dbEntry?.cups ?: 0
                if (draftCups > dbCups) {
                    repository.upsertWaterIntake(
                        WaterIntake(
                            date = date,
                            cups = draftCups,
                            createdAt = dbEntry?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
                syncedDates += date
            } catch (e: Exception) {
                Log.e("WaterSync", "Failed to sync water for $date", e)
            }
        }

        if (syncedDates.isNotEmpty()) {
            lockedWaterDraft.clearDates(syncedDates)
        }
    }
}

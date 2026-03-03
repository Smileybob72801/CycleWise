# ============================================================================
# ProGuard / R8 keep rules for RhythmWise
# ============================================================================

# ----------------------------------------------------------------------------
# Room — Entities
# ----------------------------------------------------------------------------
# Room uses reflection to instantiate entity classes. Keep all fields and
# constructors so that the generated _Impl code can map cursor columns.
-keep class com.veleda.cyclewise.androidData.local.entities.PeriodEntity { *; }
-keep class com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity { *; }
-keep class com.veleda.cyclewise.androidData.local.entities.PeriodLogEntity { *; }
-keep class com.veleda.cyclewise.androidData.local.entities.SymptomEntity { *; }
-keep class com.veleda.cyclewise.androidData.local.entities.SymptomLogEntity { *; }
-keep class com.veleda.cyclewise.androidData.local.entities.MedicationEntity { *; }
-keep class com.veleda.cyclewise.androidData.local.entities.MedicationLogEntity { *; }
-keep class com.veleda.cyclewise.androidData.local.entities.WaterIntakeEntity { *; }

# ----------------------------------------------------------------------------
# Room — DAOs
# ----------------------------------------------------------------------------
# Room generates _Impl classes that implement these interfaces at compile time.
-keep interface com.veleda.cyclewise.androidData.local.dao.PeriodDao { *; }
-keep interface com.veleda.cyclewise.androidData.local.dao.DailyEntryDao { *; }
-keep interface com.veleda.cyclewise.androidData.local.dao.PeriodLogDao { *; }
-keep interface com.veleda.cyclewise.androidData.local.dao.SymptomDao { *; }
-keep interface com.veleda.cyclewise.androidData.local.dao.SymptomLogDao { *; }
-keep interface com.veleda.cyclewise.androidData.local.dao.MedicationDao { *; }
-keep interface com.veleda.cyclewise.androidData.local.dao.MedicationLogDao { *; }
-keep interface com.veleda.cyclewise.androidData.local.dao.WaterIntakeDao { *; }

# ----------------------------------------------------------------------------
# Room — Database
# ----------------------------------------------------------------------------
-keep class com.veleda.cyclewise.androidData.local.database.PeriodDatabase { *; }

# ----------------------------------------------------------------------------
# Room — Type Converters
# ----------------------------------------------------------------------------
# Room invokes @TypeConverter methods by reflection.
-keep class com.veleda.cyclewise.androidData.local.entities.Converters { *; }

# ----------------------------------------------------------------------------
# Enum classes used by type converters
# ----------------------------------------------------------------------------
# R8 can strip valueOf()/values() from enums which breaks serialization and
# Room type conversion.
-keepclassmembers enum com.veleda.cyclewise.domain.models.FlowIntensity { *; }
-keepclassmembers enum com.veleda.cyclewise.domain.models.PeriodColor { *; }
-keepclassmembers enum com.veleda.cyclewise.domain.models.PeriodConsistency { *; }
-keepclassmembers enum com.veleda.cyclewise.domain.models.SymptomCategory { *; }
-keepclassmembers enum com.veleda.cyclewise.domain.models.CyclePhase { *; }

# ----------------------------------------------------------------------------
# WorkManager workers
# ----------------------------------------------------------------------------
# WorkManager instantiates workers by class name via reflection.
-keep class com.veleda.cyclewise.reminders.workers.HydrationReminderWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class com.veleda.cyclewise.reminders.workers.MedicationReminderWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
-keep class com.veleda.cyclewise.reminders.workers.PeriodPredictionWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }

# ----------------------------------------------------------------------------
# SQLCipher
# ----------------------------------------------------------------------------
# Keep ALL members (including private native methods).
# PeriodDatabase.rekeyRaw() uses reflection to call the private native
# rekey(byte[]) method for passphrase change and zero-key migration.
# Narrowing this rule to public members would break encryption key changes.
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# ----------------------------------------------------------------------------
# BouncyCastle (Argon2 KDF)
# ----------------------------------------------------------------------------
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ----------------------------------------------------------------------------
# Koin DI
# ----------------------------------------------------------------------------
-keep class org.koin.** { *; }
-dontwarn org.koin.**
# Keep Koin module functions (declared as top-level or in objects)
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# ----------------------------------------------------------------------------
# Kotlin serialization
# ----------------------------------------------------------------------------
# kotlinx.serialization uses reflection for @Serializable classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.veleda.cyclewise.**$$serializer { *; }
-keepclassmembers class com.veleda.cyclewise.** {
    *** Companion;
}
-keepclasseswithmembers class com.veleda.cyclewise.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----------------------------------------------------------------------------
# Jetpack Compose
# ----------------------------------------------------------------------------
# Keep Compose runtime metadata so that the compiler-generated stability
# information is preserved.
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# ----------------------------------------------------------------------------
# General Kotlin
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-dontwarn kotlinx.**

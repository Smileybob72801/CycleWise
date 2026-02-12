package com.veleda.cyclewise.domain.models

/**
 * Subjective intensity of menstrual flow for a single day.
 * Stored in [PeriodLog.flowIntensity] and persisted as the enum name string.
 */
enum class FlowIntensity {
    /** Minimal or spotting-level flow. */
    LIGHT,
    /** Typical mid-range flow. */
    MEDIUM,
    /** Notably heavy flow day. */
    HEAVY
}

/**
 * Categorical libido self-assessment for a single day.
 * Stored in [DailyEntry.libidoLevel] and persisted as the enum name string.
 */
enum class LibidoLevel {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Broad classification for symptoms in the user's symptom library.
 * Used to group symptoms in the UI and for phase-pattern analysis.
 */
enum class SymptomCategory {
    /** Physical pain symptoms (cramps, headache, joint pain). */
    PAIN,
    /** Emotional and mental health symptoms (anxiety, mood swings). */
    MOOD,
    /** Gastrointestinal symptoms (bloating, nausea, appetite changes). */
    DIGESTIVE,
    /** Skin-related symptoms (acne, dryness). */
    SKIN,
    /** Fatigue, brain fog, and sleep-related symptoms. */
    ENERGY,
    /** Catch-all for user-created symptoms that don't fit other categories. */
    OTHER
}

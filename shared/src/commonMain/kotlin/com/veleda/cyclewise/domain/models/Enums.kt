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
 * Observed color of menstrual flow for a single day.
 * Stored in [PeriodLog.periodColor] and persisted as the enum name string.
 */
enum class PeriodColor {
    /** Light, diluted flow often seen at the start or end of a period. */
    PINK,
    /** Fresh, well-oxygenated flow typical of mid-period days. */
    BRIGHT_RED,
    /** Older blood that has had time to oxidize; common on heavier days. */
    DARK_RED,
    /** Significantly oxidized blood, often seen at the tail end of a period. */
    BROWN,
    /** Very old or heavily oxidized blood; may warrant attention if persistent. */
    BLACK_OR_VERY_DARK,
    /** Any color outside normal range (e.g., orange or grey tint). */
    UNUSUAL_COLOR
}

/**
 * Observed consistency/texture of menstrual flow for a single day.
 * Stored in [PeriodLog.periodConsistency] and persisted as the enum name string.
 */
enum class PeriodConsistency {
    /** Watery, low-viscosity flow. */
    THIN,
    /** Typical viscosity — neither watery nor thick. */
    MODERATE,
    /** Notably viscous or dense flow. */
    THICK,
    /** Stringy or mucus-like texture. */
    STRINGY,
    /** Contains small clots (< 1 cm / dime-sized). */
    CLOTS_SMALL,
    /** Contains large clots (>= 1 cm / quarter-sized or larger). */
    CLOTS_LARGE
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

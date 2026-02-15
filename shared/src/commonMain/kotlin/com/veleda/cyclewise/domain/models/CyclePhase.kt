package com.veleda.cyclewise.domain.models

/**
 * Computed classification of a day within a menstrual cycle.
 *
 * Unlike user-entered attributes (e.g. [FlowIntensity]), cycle phase is derived
 * algorithmically from period history and average cycle length by
 * [com.veleda.cyclewise.domain.CyclePhaseCalculator].
 *
 * Phase boundaries follow a standard ovulatory model:
 * - **Luteal phase** is fixed at the last 14 days of a cycle.
 * - **Ovulation window** is the 3 days immediately before the luteal phase.
 * - **Follicular phase** fills the gap between menstruation end and ovulation start.
 */
enum class CyclePhase {
    /** Active menstrual bleeding days (period start through period end). */
    MENSTRUATION,

    /** Post-menstruation phase where follicles develop; ends before the ovulation window. */
    FOLLICULAR,

    /** Three-day window around estimated ovulation, immediately before the luteal phase. */
    OVULATION,

    /** Post-ovulation phase; fixed at the last 14 days of the cycle. */
    LUTEAL
}

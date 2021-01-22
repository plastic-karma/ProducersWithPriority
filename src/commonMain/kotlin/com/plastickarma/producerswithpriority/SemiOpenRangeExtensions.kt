package com.plastickarma.producerswithpriority

/**
 * Creates a [SemiOpenRange] from this to end.
 */
infix fun Double.until(end: Double) = SemiOpenRange(this, end)

/**
 * Splits this range at the given pivot and returns two new [SemiOpenRange]s.
 */
fun SemiOpenRange.split(
    pivot: Double
): Pair<SemiOpenRange, SemiOpenRange> {
    return if (pivot in this) {
        this.start until pivot to (pivot until this.end)
    } else {
        error("$pivot not in $this")
    }
}

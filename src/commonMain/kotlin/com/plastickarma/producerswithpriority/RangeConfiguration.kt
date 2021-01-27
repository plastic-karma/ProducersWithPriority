package com.plastickarma.producerswithpriority

/**
 * Pairs priority values in form of a range with other priority configuration.
 */
data class RangeConfiguration(
    val range: SemiOpenRange,
    val config: PriorityConfiguration
)

package com.plastickarma.producerswithpriority

/**
 * Configuration data for prioritization.
 * @property shares Shares represent the relative priority of a producer compared to other producers.
 */
data class PriorityConfiguration(
    val shares: Double,
    val possiblePenalty: Double = 0.0,
)

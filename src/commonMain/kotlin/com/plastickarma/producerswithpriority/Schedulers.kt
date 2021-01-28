package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.Flow

private const val FAIR_SHARE = 100.0

/**
 * Returns a flow in which each producer has the same amount of shares and will be treated with equal priority.
 * @param epochs Determines for how long to populate data into the resulting flow
 * @param producers producers to schedule fairly.
 */
fun <T> fair(
    epochs: EpochGenerator = INFINITE,
    vararg producers: Producer<T>
): Flow<T> {
    return Scheduler().schedule(
        producers = producers.map { PriorityConfiguration(shares = FAIR_SHARE) to it },
        epochs = epochs
    )
}

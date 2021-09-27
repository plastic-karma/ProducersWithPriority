package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.Flow

private const val FAIR_SHARE = 1000.0
private const val EMPTY_PENALTY = 999.0

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

/**
 * Returns a flow in which each producer has the same amount of shares and will be treated with equal priority. When
 * one producer returns a empty (null) result, it will be penalized 'heavily' (shares will be reduced to 0.1% times *
 * of overall producers).
 *
 * Example with 3 producers p1,p2,p3:
 *
 * Shares before null:
 * p1: 1000, p2: 1000, p3: 1000
 *
 * p3 emits null. New shares:
 * p1: 1000, p2: 1000, p3: 1
 *
 * @param epochs Determines for how long to populate data into the resulting flow
 * @param producers producers to schedule fairly.
 */
fun <T> penalizeEmpty(
    epochs: EpochGenerator,
    vararg producers: Producer<T>
): Flow<T> {
    return Scheduler().schedule(
        producers = producers.map { PriorityConfiguration(shares = FAIR_SHARE, EMPTY_PENALTY) to it },
        epochs = epochs
    )
}

package com.plastickarma.producerswithpriority

/**
 * Creates a scheduler that distributes the picked data based on producer configuration.
 */
internal fun <T> distributionScheduler(
    randomValueProvider: () -> Double
) =
    fun(prioritizedProducers: List<PrioritizedProducer<T>>): PrioritizedProducer<T> {
        val next = randomValueProvider()
        for (producer in prioritizedProducers) {
            if (next in producer.rangeConfiguration.range) {
                return producer
            }
        }
        error("no producer found for $next in $prioritizedProducers")
    }

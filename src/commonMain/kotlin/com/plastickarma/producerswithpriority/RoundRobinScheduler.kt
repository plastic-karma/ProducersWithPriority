package com.plastickarma.producerswithpriority

/**
 * Creates a round robin scheduler.
 */
internal fun <T> roundRobinScheduler(): (List<PrioritizedProducer<T>>) -> PrioritizedProducer<T> {
    var index = 0
    return fun(prioritizedProducers: List<PrioritizedProducer<T>>): PrioritizedProducer<T> {
        if (index >= prioritizedProducers.size) {
            index = 0
        }
        return prioritizedProducers[index++]
    }
}

package com.plastickarma.producerswithpriority

/**
 * Creates a round robin scheduler.
 */
internal fun <T> roundRobinScheduler(prioritizedProducers: List<PrioritizedProducer<T>>): () -> PrioritizedProducer<T> {
    var iter = prioritizedProducers.iterator()
    return fun(): PrioritizedProducer<T> {
        if (!iter.hasNext()) {
            iter = prioritizedProducers.iterator()
        }
        return iter.next()
    }
}

package com.plastickarma.producerswithpriority

/**
 * Type alias to pair a PriorityRange and a Producer.
 */
typealias PrioritizedProducer<T> = Pair<PriorityRange, Producer<T>>

/**
 * Type alias for a list of PrioritizedProducer
 */
typealias PrioritizedProducers<T> = List<PrioritizedProducer<T>>

/**
 * Returns the priority range of PrioritizedProducer.
 */
fun PrioritizedProducer<*>.range() = this.first.range

/**
 * Returns the actual producer of the pair.
 */
fun <T> PrioritizedProducer<T>.producer() = this.second
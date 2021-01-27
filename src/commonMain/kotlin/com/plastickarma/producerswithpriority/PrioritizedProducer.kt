package com.plastickarma.producerswithpriority

/**
 *  Pairs a PriorityRange and a Producer.
 */
data class PrioritizedProducer<T>(val rangeConfiguration: RangeConfiguration, val producer: Producer<T>)

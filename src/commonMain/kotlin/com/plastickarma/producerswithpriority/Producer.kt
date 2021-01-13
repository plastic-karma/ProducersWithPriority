package com.plastickarma.producerswithpriority

/**
 * Something that can produce a value.
 */
interface Producer<T> {

    /**
     * Get value.
     */
    suspend fun get(): T
}
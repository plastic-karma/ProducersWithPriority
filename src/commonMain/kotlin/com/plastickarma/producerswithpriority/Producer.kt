package com.plastickarma.producerswithpriority

/**
 * Something that can produce a value.
 */
interface Producer<T> {

    /**
     * Get value. The producer must never throw an exception. All exceptions must be handled inside next function.
     */
    suspend fun next(): T?
}

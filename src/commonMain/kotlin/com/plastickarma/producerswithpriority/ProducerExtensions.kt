package com.plastickarma.producerswithpriority

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Flattens a producer, that generates [Iterable]s to a producer that generated the element of these iterables.
 *
 * Example:
 * producer = ["A", "B", "C], ["1" ,"2", "3"]
 * flatten producer = ["A", "B", "C, "1" ,"2", "3"]
 *
 * Note, that null values from the original producer will be propagated to the flatten producer.
 */
fun <T> Producer<out Iterable<T>>.flatten(): Producer<T> {
    return object : Producer<T> {
        private var iterator: Iterator<T>? = null

        override suspend fun next(): T? {
            if (iterator?.hasNext() != true) {
                iterator = this@flatten.next()?.iterator()
            }
            return if (iterator?.hasNext() == true) {
                iterator?.next()
            } else {
                null
            }
        }
    }
}

/**
 * Converts a [Flow] into a [Producer].
 * Each call to [Producer.next] returns the next element emitted by the flow.
 * Returns null once the flow completes.
 * The flow collection is launched lazily on the first call to [Producer.next],
 * bound to the caller's coroutine scope.
 */
fun <T> Flow<T>.toProducer(): Producer<T> =
    object : Producer<T> {
        private var channel: Channel<T>? = null

        override suspend fun next(): T? {
            if (channel == null) {
                val chan = Channel<T>(1)
                CoroutineScope(currentCoroutineContext()).launch {
                    this@toProducer.collect { chan.send(it) }
                    chan.close()
                }
                channel = chan
            }
            return channel?.receiveCatching()?.getOrNull()
        }
    }

package com.plastickarma.producerswithpriority

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
            return iterator?.next()
        }
    }
}

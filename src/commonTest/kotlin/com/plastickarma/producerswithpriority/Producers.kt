package com.plastickarma.producerswithpriority

fun producer(value: String?): Producer<String> = object : Producer<String> {
    override suspend fun next(): String? {
        return value
    }
}

fun producerWithNull(vararg values: String?): Producer<String> = object : Producer<String> {
    val iterator = values.iterator()
    var lastValue: String? = null
    override suspend fun next(): String? {
        if (iterator.hasNext()) {
            lastValue = iterator.next()
        }
        return lastValue
    }
}

fun producerWithFailure() = object : Producer<String> {
    override suspend fun next(): String? {
        error("producer error")
    }
}

suspend fun <T> Producer<T>.collectUntilNull(): List<T> {
    return collectWhile { it != null }.mapNotNull { it }
}

suspend fun <T> Producer<T>.collectWhile(predicate: (T?) -> Boolean): List<T?> {
    val list = mutableListOf<T?>()
    while (true) {
        val next = this.next()
        if (predicate(next)) {
            list.add(next)
        } else {
            return list
        }
    }
}

package com.plastickarma.producerswithpriority

import runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProducersTest {

    @Test
    fun flatten_produces_elements_from_collection() = runBlockingTest {
        val listProducer = ListProducer(
            listOf(
                listOf("A", "B", "C"),
                listOf("1", "2", "3"),
                listOf("@", "$", "%"),
            )
        )
        val result = listProducer.flatten().collectUntilNull()
        assertEquals(
            listOf("A", "B", "C", "1", "2", "3", "@", "$", "%"),
            result
        )
    }

    @Test
    fun flatten_propagates_null_values() = runBlockingTest {
        val listProducer = ListProducer(
            listOf(
                listOf("A", "B", "C"),
                null,
                listOf("@", "$", "%"),
            )
        )
        val result = listProducer.flatten().collectWhile { it != "%" }
        assertEquals(
            listOf("A", "B", "C", null, "@", "$"),
            result
        )
    }

    private class ListProducer(data: List<List<String>?>) : Producer<List<String>> {
        private val iter = data.iterator()
        override suspend fun next(): List<String>? {
            return if (iter.hasNext()) {
                iter.next()
            } else {
                null
            }
        }
    }
}

package com.plastickarma.producerswithpriority

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProducersTest {
    @Test
    fun flatten_produces_elements_from_collection() =
        runTest {
            val listProducer =
                ListProducer(
                    listOf(
                        listOf("A", "B", "C"),
                        listOf("1", "2", "3"),
                        listOf("@", "$", "%"),
                    ),
                )
            val result = listProducer.flatten().collectUntilNull()
            assertEquals(
                listOf("A", "B", "C", "1", "2", "3", "@", "$", "%"),
                result,
            )
        }

    @Test
    fun flatten_propagates_null_values() =
        runTest {
            val listProducer =
                ListProducer(
                    listOf(
                        listOf("A", "B", "C"),
                        null,
                        listOf("@", "$", "%"),
                    ),
                )
            val result = listProducer.flatten().collectWhile { it != "%" }
            assertEquals(
                listOf("A", "B", "C", null, "@", "$"),
                result,
            )
        }

    @Test
    fun flatten_empty_batch_returns_null() =
        runTest {
            val listProducer =
                ListProducer(
                    listOf(
                        listOf("A", "B"),
                        emptyList(),
                        listOf("C"),
                    ),
                )
            // empty batch should produce null (same semantics as a null batch),
            // then "C" should follow on subsequent calls
            val flattened = listProducer.flatten()
            assertEquals("A", flattened.next())
            assertEquals("B", flattened.next())
            assertEquals(null, flattened.next()) // empty batch → null, not NoSuchElementException
            assertEquals("C", flattened.next())
        }

    private class ListProducer(
        data: List<List<String>?>,
    ) : Producer<List<String>> {
        private val iter = data.iterator()

        override suspend fun next(): List<String>? =
            if (iter.hasNext()) {
                iter.next()
            } else {
                null
            }
    }
}

package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlowToProducerTest {
    @Test
    fun produces_all_elements_in_order() =
        runTest {
            val producer = flowOf("A", "B", "C").toProducer()
            assertEquals("A", producer.next())
            assertEquals("B", producer.next())
            assertEquals("C", producer.next())
        }

    @Test
    fun returns_null_after_flow_completes() =
        runTest {
            val producer = flowOf("A").toProducer()
            assertEquals("A", producer.next())
            assertNull(producer.next())
        }

    @Test
    fun returns_null_immediately_for_empty_flow() =
        runTest {
            val producer = emptyFlow<String>().toProducer()
            assertNull(producer.next())
        }

    @Test
    fun returns_null_on_repeated_calls_after_completion() =
        runTest {
            val producer = flowOf("X").toProducer()
            producer.next()
            assertNull(producer.next())
            assertNull(producer.next())
        }

    @Test
    fun works_with_collect_until_null() =
        runTest {
            val producer = flowOf(1, 2, 3, 4, 5).toProducer()
            val result = producer.collectUntilNull()
            assertEquals(listOf(1, 2, 3, 4, 5), result)
        }

    @Test
    fun works_with_single_element() =
        runTest {
            val producer = flowOf(42).toProducer()
            assertEquals(42, producer.next())
            assertNull(producer.next())
        }
}

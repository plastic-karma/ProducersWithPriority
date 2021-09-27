package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.toList
import runBlockingTest
import kotlin.test.Test

class SchedulersTest {

    @Test
    fun fair_produces_equal_distribution() = runBlockingTest {
        val epochs = 1000000
        val values = fair(
            epochs = fixedEpochs(epochs),
            producer("A"),
            producer("B"),
            producer("C"),
            producer("D")
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()
        val cValues = collectedValues.filter { it == "C" }.size.toDouble()
        val dValues = collectedValues.filter { it == "D" }.size.toDouble()

        assertCloseTo(0.25, aValues / epochs)
        assertCloseTo(0.25, bValues / epochs)
        assertCloseTo(0.25, cValues / epochs)
        assertCloseTo(0.25, dValues / epochs)
    }

    @Test
    fun fair_works_with_single_producer() = runBlockingTest {
        val epochs = 1000000
        val values = fair(
            epochs = fixedEpochs(epochs),
            producer("A")
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()

        assertCloseTo(1.00, aValues / epochs)
    }

    @Test
    fun penalizeEmpty_puts_penalty_on_null_producer() = runBlockingTest {
        val producerA = producer("A")
        val producerB = producer("B")
        val nullProducer = producer(null)
        val epochs = 1000000
        val values = penalizeEmpty(
            epochs = fixedEpochs(epochs),
            producerA,
            producerB,
            nullProducer
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()

        // original shares where (1000 + 1000 + 1000) = 3000. So without penalty the distribution would be 33%.
        // with penalty we have (1000 + 1000 + (1000 - 999)) = 2001 shares. So the distribution should be 1000 / 2001 = 49.9%.
        assertCloseTo(0.499, aValues / epochs)
        assertCloseTo(0.499, bValues / epochs)
    }
}

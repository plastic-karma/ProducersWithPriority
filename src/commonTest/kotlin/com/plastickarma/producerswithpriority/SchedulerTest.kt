package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.toList
import runBlockingTest
import kotlin.test.Ignore
import kotlin.test.Test

class SchedulerTest {

    private fun producer(value: String?): Producer<String?> = object : Producer<String?> {
        override suspend fun get(): String? {
            return value;
        }
    }

    private fun producerWithNull(vararg values: String?): Producer<String?> = object : Producer<String?> {
        val iterator = values.iterator()
        override suspend fun get(): String? {
            return iterator.next()
        }
    }

    private fun fixedEpochs(epochs: Int) : EpochGenerator {
        var counter = 0
        return { counter++ < epochs }
    }

    @Test
    fun three_different_shares() = runBlockingTest {
        val epochs = 1000000
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                Pair(PriorityConfiguration(shares = 10.0), producer("A")),
                Pair(PriorityConfiguration(shares = 75.0), producer("B")),
                Pair(PriorityConfiguration(shares = 15.0), producer("C"))
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()
        val cValues = collectedValues.filter { it == "C" }.size.toDouble()

        assertCloseTo(0.10, aValues / epochs)
        assertCloseTo(0.75, bValues / epochs)
        assertCloseTo(0.15, cValues / epochs)
    }

    @Test
    fun five_different_shares() = runBlockingTest {
        val epochs = 100000
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                Pair(PriorityConfiguration(shares = 10.0), producer("A")),
                Pair(PriorityConfiguration(shares = 15.0), producer("B")),
                Pair(PriorityConfiguration(shares = 20.0), producer("C")),
                Pair(PriorityConfiguration(shares = 20.0), producer("D")),
                Pair(PriorityConfiguration(shares = 12.0), producer("E")),
                Pair(PriorityConfiguration(shares = 18.0), producer("F"))
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()
        val cValues = collectedValues.filter { it == "C" }.size.toDouble()
        val dValues = collectedValues.filter { it == "D" }.size.toDouble()
        val eValues = collectedValues.filter { it == "E" }.size.toDouble()
        val fValues = collectedValues.filter { it == "F" }.size.toDouble()


        assertCloseTo(0.10, aValues / epochs)
        assertCloseTo(0.15, bValues / epochs)
        assertCloseTo(0.20, cValues / epochs)
        assertCloseTo(0.20, dValues / epochs)
        assertCloseTo(0.12, eValues / epochs)
        assertCloseTo(0.18, fValues / epochs)
    }

    @Test
    fun three_equal_shares() = runBlockingTest {
        val epochs = 1000000
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                Pair(PriorityConfiguration(shares = 20.0), producer("A")),
                Pair(PriorityConfiguration(shares = 20.0), producer("B")),
                Pair(PriorityConfiguration(shares = 20.0), producer("C"))
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()
        val cValues = collectedValues.filter { it == "C" }.size.toDouble()

        assertCloseTo(0.33, aValues / epochs)
        assertCloseTo(0.33, bValues / epochs)
        assertCloseTo(0.33, cValues / epochs)
    }

    @Test
    fun single_producer() = runBlockingTest {
        val epochs = 1000000
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                Pair(PriorityConfiguration(shares = 20.0), producer("A"))
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()

        assertCloseTo(1.00, aValues / epochs)
    }

    @Test
    fun producers_with_penalties() = runBlockingTest {
        val epochs = 1000000
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                Pair(PriorityConfiguration(shares = 15.0), producer("A")),
                Pair(PriorityConfiguration(shares = 15.0), producer("B")),
                Pair(PriorityConfiguration(shares = 70.0, possiblePenalty = 40.0), producer(null)),
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()

        // original shares where (15 + 15 + 70) = 100. So without penalty the distribution would be 15%.
        // with penalty we have (15 + 15 + (70 - 40)) = 60 shares. So the distribution should be 15 / 60 = 25%.
        assertCloseTo(0.25, aValues / epochs)
        assertCloseTo(0.25, bValues / epochs)
    }

    private fun assertCloseTo(expected: Double, actual: Double, e: Double = 0.05) {
        if (actual >= expected - e && actual <= expected + e) {
            return
        } else {
            throw AssertionError("$actual not with ${expected - e} and ${expected + e}")
        }
    }
}
package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.toList
import runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SchedulerTest {

    @Test
    fun three_different_shares() = runBlockingTest {
        val epochs = 1000000
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                PriorityConfiguration(shares = 10.0) to producer("A"),
                PriorityConfiguration(shares = 75.0) to producer("B"),
                PriorityConfiguration(shares = 15.0) to producer("C")
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
                PriorityConfiguration(shares = 10.0) to producer("A"),
                PriorityConfiguration(shares = 15.0) to producer("B"),
                PriorityConfiguration(shares = 20.0) to producer("C"),
                PriorityConfiguration(shares = 20.0) to producer("D"),
                PriorityConfiguration(shares = 12.0) to producer("E"),
                PriorityConfiguration(shares = 18.0) to producer("F")
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
                PriorityConfiguration(shares = 20.0) to producer("A"),
                PriorityConfiguration(shares = 20.0) to producer("B"),
                PriorityConfiguration(shares = 20.0) to producer("C")
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
                PriorityConfiguration(shares = 20.0) to producer("A")
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()

        assertCloseTo(1.00, aValues / epochs)
    }

    @Test
    fun producers_with_penalties() = runBlockingTest {
        val producerA = producer("A")
        val producerB = producer("B")
        val nullProducer = producer(null)
        val epochs = 1000000
        val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            priorityEventHandler = priorityUpdateEvents::add,
            producers = listOf(
                PriorityConfiguration(shares = 15.0) to producerA,
                PriorityConfiguration(shares = 15.0) to producerB,
                PriorityConfiguration(shares = 70.0, possiblePenalty = 40.0) to nullProducer,
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()

        // original shares where (15 + 15 + 70) = 100. So without penalty the distribution would be 15%.
        // with penalty we have (15 + 15 + (70 - 40)) = 60 shares. So the distribution should be 15 / 60 = 25%.
        assertCloseTo(0.25, aValues / epochs)
        assertCloseTo(0.25, bValues / epochs)

        // we expect one update in priorities
        assertEquals(1, priorityUpdateEvents.size)

        // range of first producer has not changed
        assertEquals(SemiOpenRange(0.0, 15.0), priorityUpdateEvents.first().getBy(producerA).rangeConfiguration.range)

        // range of second producer has not changed
        assertEquals(SemiOpenRange(15.0, 30.0), priorityUpdateEvents.first().getBy(producerB).rangeConfiguration.range)

        // range of null producer has changed
        assertEquals(SemiOpenRange(30.0, 60.0), priorityUpdateEvents.first().getBy(nullProducer).rangeConfiguration.range)
    }

    @Test
    fun producers_with_high_penalties() = runBlockingTest {
        val producerA = producer("A")
        val producerB = producer("B")
        val nullProducer = producer(null)
        val epochs = 1000000
        val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            priorityEventHandler = priorityUpdateEvents::add,
            producers = listOf(
                PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to producerA,
                PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to producerB,
                PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to nullProducer,
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()

        // original shares where (1000 + 1000 + 1000) = 3000. So without penalty the distribution would be 33%.
        // with penalty we have (1000 + 1000 + (1000 - 999)) = 2001 shares. So the distribution should be 1000 / 2001 = 49%.
        assertCloseTo(0.49, aValues / epochs)
        assertCloseTo(0.49, bValues / epochs)

        // we expect one update in priorities
        assertEquals(1, priorityUpdateEvents.size)

        // range of first producer has not changed
        assertEquals(SemiOpenRange(1.0, 1001.0), priorityUpdateEvents.first().getBy(producerA).rangeConfiguration.range)

        // range of second producer has not changed
        assertEquals(SemiOpenRange(1001.0, 2001.0), priorityUpdateEvents.first().getBy(producerB).rangeConfiguration.range)

        // range of null producer has changed
        assertEquals(SemiOpenRange(0.0, 1.0), priorityUpdateEvents.first().getBy(nullProducer).rangeConfiguration.range)
    }

    @Test
    fun producers_with_penalties_multiple_changes() = runBlockingTest {
        val producerA = producer("A")
        val producerB = producer("B")
        val nullProducer = producerWithNull("C", null, "C", "C", null, "C")
        val epochs = 100
        val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
        Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            priorityEventHandler = priorityUpdateEvents::add,
            producers = listOf(
                PriorityConfiguration(shares = 15.0) to producerA,
                PriorityConfiguration(shares = 15.0) to producerB,
                PriorityConfiguration(shares = 70.0, possiblePenalty = 40.0) to nullProducer,
            )
        ).toList()

        // we expect four updates in priorities
        assertEquals(4, priorityUpdateEvents.size)

        // first change: nullProducer changes to 30 shares
        assertEquals(SemiOpenRange(0.0, 15.0), priorityUpdateEvents[0].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(15.0, 30.0), priorityUpdateEvents[0].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(30.0, 60.0), priorityUpdateEvents[0].getBy(nullProducer).rangeConfiguration.range)

        // second change: nullProducer changes back to 70 shares
        assertEquals(SemiOpenRange(0.0, 15.0), priorityUpdateEvents[1].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(15.0, 30.0), priorityUpdateEvents[1].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(30.0, 100.0), priorityUpdateEvents[1].getBy(nullProducer).rangeConfiguration.range)

        // third change: nullProducer changes to 30 shares
        assertEquals(SemiOpenRange(0.0, 15.0), priorityUpdateEvents[2].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(15.0, 30.0), priorityUpdateEvents[2].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(30.0, 60.0), priorityUpdateEvents[2].getBy(nullProducer).rangeConfiguration.range)

        // fourth change: nullProducer changes back to 70 shares
        assertEquals(SemiOpenRange(0.0, 15.0), priorityUpdateEvents[3].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(15.0, 30.0), priorityUpdateEvents[3].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(30.0, 100.0), priorityUpdateEvents[3].getBy(nullProducer).rangeConfiguration.range)
    }

    @Test
    fun producers_with_high_penalties_multiple_changes() = runBlockingTest {
        val producerA = producer("A")
        val producerB = producer("B")
        val nullProducer = producerWithNull("C", null, "C", "C", null, "C")
        val epochs = 1000000
        val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
        Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            priorityEventHandler = priorityUpdateEvents::add,
            producers = listOf(
                PriorityConfiguration(shares = 1000.0) to producerA,
                PriorityConfiguration(shares = 1000.0) to producerB,
                PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to nullProducer,
            )
        ).toList()

        // we expect four updates in priorities
        assertEquals(4, priorityUpdateEvents.size)

        // first change: nullProducer changes 0.2% shares
        assertEquals(SemiOpenRange(1.0, 1001.0), priorityUpdateEvents[0].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(1001.0, 2001.0), priorityUpdateEvents[0].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(0.0, 1.0), priorityUpdateEvents[0].getBy(nullProducer).rangeConfiguration.range)

        // second change: nullProducer changes back to 1000 shares
        assertEquals(SemiOpenRange(0.0, 1000.0), priorityUpdateEvents[1].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(1000.0, 2000.0), priorityUpdateEvents[1].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(2000.0, 3000.0), priorityUpdateEvents[1].getBy(nullProducer).rangeConfiguration.range)

        // third change: nullProducer changes 0.2% shares
        assertEquals(SemiOpenRange(1.0, 1001.0), priorityUpdateEvents[2].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(1001.0, 2001.0), priorityUpdateEvents[2].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(0.0, 1.0), priorityUpdateEvents[2].getBy(nullProducer).rangeConfiguration.range)

        // fourth change: nullProducer changes back to 1000 shares
        assertEquals(SemiOpenRange(0.0, 1000.0), priorityUpdateEvents[3].getBy(producerA).rangeConfiguration.range)
        assertEquals(SemiOpenRange(1000.0, 2000.0), priorityUpdateEvents[3].getBy(producerB).rangeConfiguration.range)
        assertEquals(SemiOpenRange(2000.0, 3000.0), priorityUpdateEvents[3].getBy(nullProducer).rangeConfiguration.range)
    }

    @Test
    fun exceptions_from_producer_are_ignored() = runBlockingTest {
        val epochs = 100000
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                PriorityConfiguration(shares = 15.0) to producer("A"),
                PriorityConfiguration(shares = 70.0) to producerWithFailure(),
                PriorityConfiguration(shares = 15.0) to producer("C")
            )
        )

        val collectedValues = values.toList()
        val aValues = collectedValues.filter { it == "A" }.size.toDouble()
        val bValues = collectedValues.filter { it == "B" }.size.toDouble()
        val cValues = collectedValues.filter { it == "C" }.size.toDouble()

        assertCloseTo(0.15, aValues / epochs)
        assertCloseTo(0.00, bValues / epochs)
        assertCloseTo(0.15, cValues / epochs)
    }

    @Test
    fun round_robin() = runBlockingTest {
        val epochs = 15
        val values = Scheduler().schedule(
            epochs = fixedEpochs(epochs),
            producers = listOf(
                PriorityConfiguration(shares = 30.0) to producer("A"),
                PriorityConfiguration(shares = 30.0) to producer("B"),
                PriorityConfiguration(shares = 30.0) to producer("C"),
            ),
            strategy = WorkStrategy.ROUND_ROBIN
        )

        val collectedValues = values.toList()
        assertEquals(
            listOf(
                "A", "B", "C",
                "A", "B", "C",
                "A", "B", "C",
                "A", "B", "C",
                "A", "B", "C",
            ),
            collectedValues
        )
    }

    private fun <T> List<PrioritizedProducer<T>>.getBy(producer: Producer<T>) = this.find { it.producer == producer }!!
}

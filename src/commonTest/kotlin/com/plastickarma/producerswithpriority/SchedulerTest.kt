package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SchedulerTest {
    @Test
    fun three_different_shares() =
        runTest {
            val epochs = 1000000
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 10.0) to producer("A"),
                            PriorityConfiguration(shares = 75.0) to producer("B"),
                            PriorityConfiguration(shares = 15.0) to producer("C"),
                        ),
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
    fun five_different_shares() =
        runTest {
            val epochs = 100000
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 10.0) to producer("A"),
                            PriorityConfiguration(shares = 15.0) to producer("B"),
                            PriorityConfiguration(shares = 20.0) to producer("C"),
                            PriorityConfiguration(shares = 20.0) to producer("D"),
                            PriorityConfiguration(shares = 12.0) to producer("E"),
                            PriorityConfiguration(shares = 18.0) to producer("F"),
                        ),
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
    fun three_equal_shares() =
        runTest {
            val epochs = 1000000
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 20.0) to producer("A"),
                            PriorityConfiguration(shares = 20.0) to producer("B"),
                            PriorityConfiguration(shares = 20.0) to producer("C"),
                        ),
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
    fun single_producer() =
        runTest {
            val epochs = 1000000
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 20.0) to producer("A"),
                        ),
                )

            val collectedValues = values.toList()
            val aValues = collectedValues.filter { it == "A" }.size.toDouble()

            assertCloseTo(1.00, aValues / epochs)
        }

    @Test
    fun producers_with_penalties() =
        runTest {
            val producerA = producer("A")
            val producerB = producer("B")
            val nullProducer = producer(null)
            val epochs = 1000000
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    priorityEventHandler = priorityUpdateEvents::add,
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 15.0) to producerA,
                            PriorityConfiguration(shares = 15.0) to producerB,
                            PriorityConfiguration(shares = 70.0, possiblePenalty = 40.0) to nullProducer,
                        ),
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
            assertEquals(
                SemiOpenRange(0.0, 15.0),
                priorityUpdateEvents
                    .first()
                    .getBy(producerA)
                    .rangeConfiguration.range,
            )

            // range of second producer has not changed
            assertEquals(
                SemiOpenRange(15.0, 30.0),
                priorityUpdateEvents
                    .first()
                    .getBy(producerB)
                    .rangeConfiguration.range,
            )

            // range of null producer has changed
            assertEquals(
                SemiOpenRange(30.0, 60.0),
                priorityUpdateEvents
                    .first()
                    .getBy(nullProducer)
                    .rangeConfiguration.range,
            )
        }

    @Test
    fun producers_with_high_penalties() =
        runTest {
            val producerA = producer("A")
            val producerB = producer("B")
            val nullProducer = producer(null)
            val epochs = 1000000
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    priorityEventHandler = priorityUpdateEvents::add,
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to producerA,
                            PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to producerB,
                            PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to nullProducer,
                        ),
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
            assertEquals(
                SemiOpenRange(1.0, 1001.0),
                priorityUpdateEvents
                    .first()
                    .getBy(producerA)
                    .rangeConfiguration.range,
            )

            // range of second producer has not changed
            assertEquals(
                SemiOpenRange(1001.0, 2001.0),
                priorityUpdateEvents
                    .first()
                    .getBy(producerB)
                    .rangeConfiguration.range,
            )

            // range of null producer has changed
            assertEquals(
                SemiOpenRange(0.0, 1.0),
                priorityUpdateEvents
                    .first()
                    .getBy(nullProducer)
                    .rangeConfiguration.range,
            )
        }

    @Test
    fun producers_with_penalties_multiple_changes() =
        runTest {
            val producerA = producer("A")
            val producerB = producer("B")
            val nullProducer = producerWithNull("C", null, "C", "C", null, "C")
            val epochs = 100
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
            Scheduler()
                .schedule(
                    epochs = fixedEpochs(epochs),
                    priorityEventHandler = priorityUpdateEvents::add,
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 15.0) to producerA,
                            PriorityConfiguration(shares = 15.0) to producerB,
                            PriorityConfiguration(shares = 70.0, possiblePenalty = 40.0) to nullProducer,
                        ),
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
    fun producers_with_high_penalties_multiple_changes() =
        runTest {
            val producerA = producer("A")
            val producerB = producer("B")
            val nullProducer = producerWithNull(null, "C", null, "C")
            val epochs = 1000000
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
            val values =
                Scheduler()
                    .schedule(
                        epochs = fixedEpochs(epochs),
                        priorityEventHandler = priorityUpdateEvents::add,
                        producers =
                            listOf(
                                PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to producerA,
                                PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to producerB,
                                PriorityConfiguration(shares = 1000.0, possiblePenalty = 999.0) to nullProducer,
                            ),
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

            val aValues = values.filter { it == "A" }.size.toDouble()
            val bValues = values.filter { it == "B" }.size.toDouble()
            val cValues = values.filter { it == "C" }.size.toDouble()

            assertCloseTo(0.33, aValues / epochs)
            assertCloseTo(0.33, bValues / epochs)
            assertCloseTo(0.33, cValues / epochs)
        }

    @Test
    fun penalties_do_not_add_up() =
        runTest {
            val producerA = producer("A")
            val producerB = producer("B")
            val nullProducer = producerWithNull("C", null)
            val epochs = 10000000
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
            Scheduler()
                .schedule(
                    epochs = fixedEpochs(epochs),
                    priorityEventHandler = priorityUpdateEvents::add,
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 15.0) to producerA,
                            PriorityConfiguration(shares = 15.0) to producerB,
                            PriorityConfiguration(shares = 70.0, possiblePenalty = 40.0) to nullProducer,
                        ),
                ).toList()

            // we expect one updates in priorities
            assertEquals(1, priorityUpdateEvents.size)

            // first change: nullProducer changes to 30 shares
            assertEquals(SemiOpenRange(0.0, 15.0), priorityUpdateEvents[0].getBy(producerA).rangeConfiguration.range)
            assertEquals(SemiOpenRange(15.0, 30.0), priorityUpdateEvents[0].getBy(producerB).rangeConfiguration.range)
            assertEquals(SemiOpenRange(30.0, 60.0), priorityUpdateEvents[0].getBy(nullProducer).rangeConfiguration.range)
        }

    @Test
    fun exceptions_from_producer_are_ignored() =
        runTest {
            val epochs = 100000
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 15.0) to producer("A"),
                            PriorityConfiguration(shares = 70.0) to producerWithFailure(),
                            PriorityConfiguration(shares = 15.0) to producer("C"),
                        ),
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
    fun round_robin() =
        runTest {
            val epochs = 15
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 30.0) to producer("A"),
                            PriorityConfiguration(shares = 30.0) to producer("B"),
                            PriorityConfiguration(shares = 30.0) to producer("C"),
                        ),
                    strategy = WorkStrategy.ROUND_ROBIN,
                )

            val collectedValues = values.toList()
            assertEquals(
                listOf(
                    "A",
                    "B",
                    "C",
                    "A",
                    "B",
                    "C",
                    "A",
                    "B",
                    "C",
                    "A",
                    "B",
                    "C",
                    "A",
                    "B",
                    "C",
                ),
                collectedValues,
            )
        }

    @Test
    fun infinite_flow_can_be_collected_partially() =
        runTest {
            val values =
                Scheduler().schedule(
                    epochs = INFINITE,
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 50.0) to producer("A"),
                            PriorityConfiguration(shares = 50.0) to producer("B"),
                        ),
                )

            val collected = values.take(10).toList()
            assertEquals(10, collected.size)
        }

    @Test
    fun all_producers_penalized_simultaneously() =
        runTest {
            val producerA = producer(null)
            val producerB = producer(null)
            val producerC = producer(null)
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()

            val collectedValues =
                Scheduler()
                    .schedule(
                        epochs = fixedEpochs(100000),
                        priorityEventHandler = priorityUpdateEvents::add,
                        producers =
                            listOf(
                                PriorityConfiguration(shares = 50.0, possiblePenalty = 40.0) to producerA,
                                PriorityConfiguration(shares = 50.0, possiblePenalty = 40.0) to producerB,
                                PriorityConfiguration(shares = 50.0, possiblePenalty = 40.0) to producerC,
                            ),
                    ).toList()

            // all producers return null → nothing emitted
            assertEquals(emptyList(), collectedValues)

            // exactly 3 penalty events: one per producer getting penalised for the first time
            assertEquals(3, priorityUpdateEvents.size)

            // after all three are penalised each has 10 shares (50-40); total = 30
            val finalState = priorityUpdateEvents.last()
            val totalShares = finalState.sumOf { it.rangeConfiguration.range.end - it.rangeConfiguration.range.start }
            assertCloseTo(30.0, totalShares, e = 0.001)
        }

    @Test
    fun round_robin_cycles_in_ascending_share_order() =
        runTest {
            // Declared order: A(60 shares), B(10 shares), C(30 shares)
            // buildPrioritizedProducers sorts ascending → [B(10), C(30), A(60)]
            // Round robin cycles: B, C, A, B, C, A, ...
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(6),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 60.0) to producer("A"),
                            PriorityConfiguration(shares = 10.0) to producer("B"),
                            PriorityConfiguration(shares = 30.0) to producer("C"),
                        ),
                    strategy = WorkStrategy.ROUND_ROBIN,
                )

            assertEquals(listOf("B", "C", "A", "B", "C", "A"), values.toList())
        }

    @Test
    fun round_robin_with_two_producers() =
        runTest {
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(6),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 50.0) to producer("A"),
                            PriorityConfiguration(shares = 50.0) to producer("B"),
                        ),
                    strategy = WorkStrategy.ROUND_ROBIN,
                )

            assertEquals(listOf("A", "B", "A", "B", "A", "B"), values.toList())
        }

    @Test
    fun round_robin_skips_null_but_maintains_cycle() =
        runTest {
            // 9 epochs: A, B(null), C, A, B(null), C, A, B(null), C → ["A","C","A","C","A","C"]
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(9),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 30.0) to producer("A"),
                            PriorityConfiguration(shares = 30.0) to producer(null),
                            PriorityConfiguration(shares = 30.0) to producer("C"),
                        ),
                    strategy = WorkStrategy.ROUND_ROBIN,
                )

            assertEquals(listOf("A", "C", "A", "C", "A", "C"), values.toList())
        }

    @Test
    fun exception_with_penalty_config_triggers_penalty() =
        runTest {
            val producerA = producer("A")
            val failingProducer = producerWithFailure()
            val epochs = 1000000
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(epochs),
                    priorityEventHandler = priorityUpdateEvents::add,
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 50.0) to producerA,
                            PriorityConfiguration(shares = 50.0, possiblePenalty = 40.0) to failingProducer,
                        ),
                )

            val collectedValues = values.toList()

            // penalty fired once - exception is treated as null
            assertEquals(1, priorityUpdateEvents.size)

            // after penalty: A has 50 shares, failing has 10 shares → A gets 50/60 ≈ 83%
            val aValues = collectedValues.filter { it == "A" }.size.toDouble()
            assertCloseTo(0.83, aValues / epochs)
        }

    @Test
    fun null_without_penalty_config_fires_no_priority_event() =
        runTest {
            val nullProducer = producer(null)
            val priorityUpdateEvents = mutableListOf<List<PrioritizedProducer<String>>>()
            Scheduler()
                .schedule(
                    epochs = fixedEpochs(1000),
                    priorityEventHandler = priorityUpdateEvents::add,
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 50.0) to producer("A"),
                            PriorityConfiguration(shares = 50.0) to nullProducer,
                        ),
                ).toList()

            assertEquals(0, priorityUpdateEvents.size)
        }

    @Test
    fun zero_epochs_produces_empty_flow() =
        runTest {
            val values =
                Scheduler().schedule(
                    epochs = fixedEpochs(0),
                    producers =
                        listOf(
                            PriorityConfiguration(shares = 50.0) to producer("A"),
                            PriorityConfiguration(shares = 50.0) to producer("B"),
                        ),
                )
            assertEquals(emptyList(), values.toList())
        }

    private fun <T> List<PrioritizedProducer<T>>.getBy(producer: Producer<T>) = this.find { it.producer == producer }!!
}

package com.plastickarma.producerswithpriority

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * Encapsulates logic to schedule different producers with priority configuration.
 */
class Scheduler {

    /**
     * Schedules different producers with priority configuration.
     * @return [Flow] of values that come from the given producers.
     */
    fun <T> schedule(
        producers: List<Pair<PriorityConfiguration, Producer<T>>>,
        epochs: EpochGenerator = INFINITE,
        priorityEventHandler: (List<PrioritizedProducer<T>>) -> Unit = { },
    ): Flow<T> {
        val penalties: MutableMap<Producer<T>, Double> = mutableMapOf()
        var (sum, prioritizedProducers) = buildPrioritizedProducers(producers, penalties)
        val random: Random = Random.Default

        fun updatePriorities() {
            val updatedPriority = buildPrioritizedProducers(producers, penalties)
            sum = updatedPriority.first
            prioritizedProducers = updatedPriority.second
            priorityEventHandler(prioritizedProducers)
        }

        return flow {
            while (epochs()) {
                val next: Double = random.nextDouble(0.0, sum)
                val nextProducer = getNextProducer(next, prioritizedProducers)
                val nextValue = nextProducer.producer.next()
                if (nextValue != null) {
                    emit(nextValue)
                    if (nextProducer.producer in penalties) {
                        penalties.remove(nextProducer.producer)
                        // penalties changed - recalculate priorities
                        updatePriorities()
                    }
                } else {
                    if (nextProducer.rangeConfiguration.config.possiblePenalty > 0.0 &&
                        nextProducer.producer !in penalties
                    ) {
                        penalties[nextProducer.producer] = nextProducer.rangeConfiguration.config.possiblePenalty
                        // penalties changed - recalculate priorities
                        updatePriorities()
                    }
                }
            }
        }
    }

    private fun <T> getNextProducer(next: Double, producers: List<PrioritizedProducer<T>>): PrioritizedProducer<T> {
        for (producer in producers) {
            if (next in producer.rangeConfiguration.range) {
                return producer
            }
        }
        error("no producer found for $next in $producers")
    }

    private fun <T> buildPrioritizedProducers(
        producers: List<Pair<PriorityConfiguration, Producer<T>>>,
        penalties: Map<Producer<T>, Double>
    ): Pair<Double, MutableList<PrioritizedProducer<T>>> {
        val prioritizedProducers: MutableList<PrioritizedProducer<T>> = mutableListOf()

        val initialShares: MutableMap<Pair<PriorityConfiguration, Producer<T>>, Double> =
            producers.associateBy({ pair -> pair }, { pair -> pair.first.shares }).toMutableMap()

        for (producer in producers) {
            if (producer.second in penalties) {
                val penalty = penalties[producer.second]!!
                initialShares[producer] = initialShares[producer]!! - penalty
            }
        }

        fun Pair<PriorityConfiguration, Producer<T>>.shares(): Double {
            return initialShares[this]!!
        }

        val allShares = producers.sumByDouble { it.shares() }
        var allRange = 0.0.until(allShares)
        producers
            .sortedBy { it.shares() }
            .forEach {

                // last range
                if (it.shares() + allRange.start >= allShares) {
                    prioritizedProducers.add(PrioritizedProducer(RangeConfiguration(allRange, it.first), it.second))
                } else {
                    val (percentile, newAllRange) = allRange.split(it.shares() + allRange.start)
                    prioritizedProducers.add(PrioritizedProducer(RangeConfiguration(percentile, it.first), it.second))
                    allRange = newAllRange
                }
            }
        return Pair(allShares, prioritizedProducers)
    }
}

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
    suspend fun <T> schedule(
        producers: List<Pair<PriorityConfiguration, Producer<T>>>,
        epochs: EpochGenerator = INFINITE
        ) : Flow<T> {
        val sum = producers.sumByDouble { it.first.shares }
        val prioritizedProducers = buildPrioritizedProducers(producers, sum)
        val random: Random = Random.Default
        return flow {
            while (epochs()) {
                val next: Double = random.nextDouble(0.0, sum)
                val nextValue = getNextProducer(next, prioritizedProducers).producer().get()
                emit(nextValue)
            }
        }
    }

    private fun <T> getNextProducer(next: Double, producers: PrioritizedProducers<T>): PrioritizedProducer<T> {
        for (producer in producers) {
            if (next in producer.range()) {
                return producer
            }
        }
        throw IllegalStateException("no producer found for $next in $producers")
    }

    private fun <T> buildPrioritizedProducers(
        producers: List<Pair<PriorityConfiguration, Producer<T>>>,
        sum: Double
    ): PrioritizedProducers<T> {
        var allRange = 0.0.until(sum)
        val prioritizedProducers: MutableList<Pair<PriorityRange, Producer<T>>> = mutableListOf()
        producers
            .sortedBy { it.first.shares }
            .forEach {

                // last range
                if (it.first.shares + allRange.start >= sum) {
                    prioritizedProducers.add(Pair(PriorityRange(allRange, it.first), it.second))
                } else {
                    val (percentile, newAllRange) = allRange.split(it.first.shares + allRange.start)
                    prioritizedProducers.add(Pair(PriorityRange(percentile, it.first), it.second))
                    allRange = newAllRange
                }

            }
        return prioritizedProducers
    }

}
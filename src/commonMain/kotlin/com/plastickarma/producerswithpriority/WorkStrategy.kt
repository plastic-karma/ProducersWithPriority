package com.plastickarma.producerswithpriority

/**
 * Overall strategy how to pick work.
 */
enum class WorkStrategy {
    DISTRIBUTION,

    /**
     * Picks data from producers one after the other. This is similar to a fair distribution, but has stronger
     * guarantees, as we don't leave it up to (random) chance where to pick from next.
     */
    ROUND_ROBIN
}

package com.plastickarma.producerswithpriority

/**
 * Range with semi-open semantic: start is inclusive, end is exclusive.
 */
data class SemiOpenRange(val start: Double, val end: Double) {
    operator fun contains(value: Double): Boolean {
        return value >= start && value < end
    }
}
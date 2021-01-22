package com.plastickarma.producerswithpriority

/**
 * Range with semi-open semantic: start is inclusive, end is exclusive.
 */
data class SemiOpenRange(val start: Double, val end: Double)

operator fun SemiOpenRange.contains(value: Double) = value >= start && value < end

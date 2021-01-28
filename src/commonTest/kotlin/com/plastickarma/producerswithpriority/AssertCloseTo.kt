package com.plastickarma.producerswithpriority

fun assertCloseTo(expected: Double, actual: Double, e: Double = 0.05) {
    if (actual >= expected - e && actual <= expected + e) {
        return
    } else {
        throw AssertionError("$actual not between ${expected - e} and ${expected + e}")
    }
}

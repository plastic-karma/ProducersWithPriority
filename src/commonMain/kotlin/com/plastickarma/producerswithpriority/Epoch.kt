package com.plastickarma.producerswithpriority

/**
 * Function that indicates, if a scheduler should continue to run.
 */
typealias EpochGenerator = (() -> Boolean)

/**
 * EpochGenerator that runs forever.
 */
val INFINITE: EpochGenerator = { true }